package fr.inria.diverse.maven.resolver.launcher;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import fr.inria.diverse.maven.resolver.db.sql.MariaDBWrapper;
import fr.inria.diverse.maven.resolver.processor.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import fr.inria.diverse.maven.resolver.tasks.DependencyGraphPrettyPrinterTask;
import fr.inria.diverse.maven.resolver.tasks.DependencyVisitorTask;

public class ClientResolverApp {

	/**
	 * RabbitMQ fields
	 */
	private static ConnectionFactory factory;

	private static Connection connection;

	private static Channel channel;

	private static final String ARTIFACT_QUEUE_NAME = "clientsQueue";

	private static final String USAGE_QUEUE_NAME = "usagesQueue";
	private static Channel usageChannel;

	private static final String DEFAULT_USERNAME = "user";

	/**
	 * The resolver application logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ClientResolverApp.class);

	/**
	 * A multiTask visitor. To add additional visit behaviour/task @see {@link MultiTaskDependencyVisitor}
	 */
	private static MultiTaskDependencyVisitor myVisitor = new MultiTaskDependencyVisitor();
	/**
	 *
	 */
	private static ClassScanCounter myCounter = new ClassScanCounter();
	/*
	 * Artifact processor
	 */
	private static AbstractArtifactProcessor processor;


	/**
	 * Neo4j Graph DBwrapper.
	 * Contains common operations to store Maven dependencies and counts
	 */
	private static MariaDBWrapper dbwrapper;
	/**
	 * Cli options
	 */
	private static final Options options = new Options();

	@SuppressWarnings("null")
	public static void main(String[] args) throws IOException, SQLException {

		//initialize arguments
		String coordinatesPath = "src/main/resources/allUniqueArtifactsOnly-mini-100";

		options.addOption("h", "help", false, "Show help");
		options.addOption("f", "fill-queue", false, "Populate queue with client coordinates.");
		options.addOption("p", "pretty-printer", true, "Path to the output file stream. Optional");
		options.addOption("d", "db-properties", true, "Path to database properties file. Mandatory");
		options.addOption("q", "queue", true, "Hostname and port of the RabbitMQ broker. Note, URI comes in the form hostname:port");
		options.addOption("u", "user", true, "User and password of the rabbitMQ. Note, it comes in the form user:password. By default user:user");

		options.addOption("b", "debug", true, "Debug a specific artifact");
		options.addOption("w", "delegate-writing", false, "Publish results on queue instead of on the DB");

		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = null;
		boolean fillQueue = false;
		try {
			cmd = parser.parse(options, args);
			if (cmd.hasOption("h")) {
				help();
			}
			if(cmd.hasOption("p")) {
				DependencyVisitorTask prettyPrinter = new DependencyGraphPrettyPrinterTask();
				myVisitor.addTask(prettyPrinter);
			}
			if(cmd.hasOption("f")) {
				fillQueue = true;
			}
			if(cmd.hasOption("d")) {
				dbwrapper = new MariaDBWrapper(new File(cmd.getOptionValue("d")));

			} else {
				LOGGER.error("Missing the properties for MariaDB");
			}
			if (cmd.hasOption("q")) {
				String [] values = cmd.getOptionValue("q").split(":");
				//check the presence of the port number
				if (values.length!=2) {
					LOGGER.error("Could not handle hostname \"{}\"",cmd.getOptionValue("q"));
					help();
				}

				factory = new ConnectionFactory();

				factory.setHost(values[0]);
				factory.setPort(Integer.valueOf(values[1]));
				String username = DEFAULT_USERNAME;
				String password = DEFAULT_USERNAME;
				if (cmd.hasOption("u")) {
					String [] credentials = cmd.getOptionValue("u").split(":");
					if (credentials.length!=2) {
						LOGGER.error("Malformated RabbitMQ credentials \"{}\". It should rather be in the form user:pass",cmd.getOptionValue("u"));
						help();
					}
					username = credentials[0];
					password = credentials[1];
				}
				factory.setUsername(username);
				factory.setPassword(password);
				factory.setNetworkRecoveryInterval(1000);
				factory.setAutomaticRecoveryEnabled(true);
				connection = factory.newConnection();
				connection.addShutdownListener(new ConnectionShutdownListener());
				channel = connection.createChannel();
				channel.basicQos(1);
				channel.addShutdownListener(new ChannelShutdownListener(connection));
				Map<String, Object> lazyArg = new HashMap<String, Object>();
				lazyArg.put("x-queue-mode", "lazy");
				channel.queueDeclare(ARTIFACT_QUEUE_NAME, true, false, false, lazyArg);

				if(cmd.hasOption("w")) {
					initQueue();
					processor = new DependencyUsageProcessor(dbwrapper, usageChannel);
				} else {
					processor = new DependencyUsageProcessor(dbwrapper);
				}
			} else if (!cmd.hasOption("b")) {
				LOGGER.error("Missing the hostname and port of rabbitMQ");
			}
		} catch (ParseException e) {
			LOGGER.error("Failed to parse comand line properties {}", e.getMessage());
			help();
		} catch (TimeoutException e) {
			LOGGER.error("Error while trying to connect to the RabbitMQ server {}",e.getMessage());
			e.printStackTrace();
		}

		if(fillQueue) {
			populateQueue();
		} else {
			if(!cmd.hasOption("b")) {
				try {
					channel.basicConsume(ARTIFACT_QUEUE_NAME, false, new DefaultConsumer(channel) {
						@Override
						public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
								throws IOException {
							try {
								channel.basicAck(envelope.getDeliveryTag(), false);
								String artifactCoordinate = new String(body, "UTF-8");
								DefaultArtifact artifact = new DefaultArtifact(artifactCoordinate);

								processor.process(artifact);
							} catch (Exception e) {
								LOGGER.error("Handle delivery Error {}", e.getMessage());
							} finally {
								//channel.basicAck(envelope.getDeliveryTag(), false);
							}
						}
					});
				} catch (IOException ioe) {
					LOGGER.error("Couldn't find arifact {}", coordinatesPath);
					ioe.printStackTrace();
				} catch (Exception e) {
					LOGGER.error("unhandled error {}", e.getMessage());
					e.printStackTrace();
				} finally {
					//healthChecker.shutdownNow();
				}
			} else {
				try {
					DefaultArtifact artifact = new DefaultArtifact(cmd.getOptionValue("b"));
					processor = new DependencyUsageProcessor(dbwrapper, true);
					processor.process(artifact);
					channel.close();
				} catch (Exception e) {
					LOGGER.error("Handle delivery Error {}", e.getMessage());
				}
			}
		}
	}
	/**
	 * Help formatter. Displays how to launch the application
	 */
	private static void help() {

		HelpFormatter formater = new HelpFormatter();

		formater.printHelp("Maven-miner", options);

		System.exit(0);

	}

	static String getClientsCoordinates = "SELECT coordinates FROM client;";
	static String getUnresolvedClientsCoordinates = "SELECT c.coordinates FROM client as c WHERE c.id NOT IN (SELECT DISTINCT(clientid) FROM api_usage);";

	private static void populateQueue() throws IOException, SQLException {

		//PreparedStatement getClients = dbwrapper.getConnection().prepareStatement(getClientsCoordinates);
		PreparedStatement getClients = dbwrapper.getConnection().prepareStatement(getUnresolvedClientsCoordinates);
		ResultSet resultSet = getClients.executeQuery();
		getClients.close();

		Map<String, Object> lazyArg = new HashMap<>();
		lazyArg.put("x-queue-mode", "lazy");
		channel.queueDeclareNoWait(ARTIFACT_QUEUE_NAME, true, false, false, lazyArg);
		channel.queuePurge(ARTIFACT_QUEUE_NAME);

		while(resultSet.next()) {
			String message = resultSet.getString("coordinates");
			channel.basicPublish("", ARTIFACT_QUEUE_NAME, null, message.getBytes("UTF-8"));
		}
		try {
			Thread.sleep(60000);
			channel.close();
		} catch (TimeoutException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		connection.close();
	}

	private static void initQueue() throws IOException {
		usageChannel = connection.createChannel();
		usageChannel.basicQos(1);
		usageChannel.addShutdownListener(new ChannelShutdownListener(connection));
		Map<String, Object> lazyArg = new HashMap<String, Object>();
		lazyArg.put("x-queue-mode", "lazy");
		usageChannel.queueDeclare(USAGE_QUEUE_NAME, true, false, false, lazyArg);
	}

	private static void closeQueue() {
		try {
			usageChannel.close();
		} catch (IOException | TimeoutException e) {
			e.printStackTrace();
		}
	}
}
