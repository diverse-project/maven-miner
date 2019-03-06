package fr.inria.diverse.maven.resolver.launcher;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import fr.inria.diverse.maven.resolver.db.sql.MariaDBWrapper;
import fr.inria.diverse.maven.resolver.processor.AbstractArtifactProcessor;
import fr.inria.diverse.maven.resolver.processor.ClassScanCounter;
import fr.inria.diverse.maven.resolver.processor.DependencyUsageProcessor;
import fr.inria.diverse.maven.resolver.processor.MultiTaskDependencyVisitor;
import fr.inria.diverse.maven.resolver.tasks.DependencyGraphPrettyPrinterTask;
import fr.inria.diverse.maven.resolver.tasks.DependencyVisitorTask;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class UsageResolverApp {

	/**
	 * RabbitMQ fields
	 */
	private static ConnectionFactory factory;

	private static Connection connection;

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

	private static InsertBuffer insertBuffer;


	public static void main(String[] args) throws IOException, SQLException {

		//initialize arguments
		String coordinatesPath = "src/main/resources/allUniqueArtifactsOnly-mini-100";

		options.addOption("h", "help", false, "Show help");
		options.addOption("d", "db-properties", true, "Path to database properties file. Mandatory");
		options.addOption("q", "queue", true, "Hostname and port of the RabbitMQ broker. Note, URI comes in the form hostname:port");
		options.addOption("u", "user", true, "User and password of the rabbitMQ. Note, it comes in the form user:password. By default user:user");
		options.addOption("s", "batch-size", true, "Number of usage to write per db commit");


		CommandLineParser parser = new DefaultParser();
		formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

		CommandLine cmd = null;
		boolean fillQueue = false;
		try {
			cmd = parser.parse(options, args);
			if (cmd.hasOption("h")) {
				help();
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
				factory.setNetworkRecoveryInterval(10000);
				factory.setAutomaticRecoveryEnabled(true);
				connection = factory.newConnection();
				connection.addShutdownListener(new ConnectionShutdownListener());
				usageChannel = connection.createChannel();
				usageChannel.basicQos(1);
				usageChannel.addShutdownListener(new ChannelShutdownListener(connection));
				Map<String, Object> lazyArg = new HashMap<String, Object>();
				lazyArg.put("x-queue-mode", "lazy");
				usageChannel.queueDeclare(USAGE_QUEUE_NAME, true, false, false, lazyArg);

				dbwrapper.getConnection().setAutoCommit(false);
				int batchSize = 1000;
				if(cmd.hasOption("s")) {
					batchSize = Integer.parseInt(cmd.getOptionValue("s"));
				}
				insertBuffer = new InsertBuffer(batchSize, dbwrapper.getConnection());
			} else {
				LOGGER.error("Missing the hostname and port of rabbitMQ");
			}
		} catch (ParseException e) {
			LOGGER.error("Failed to parse comand line properties {}", e.getMessage());
			help();
		} catch (TimeoutException e) {
			LOGGER.error("Error while trying to connect to the RabbitMQ server {}",e.getMessage());
			e.printStackTrace();
		}
		try {
			usageChannel.basicConsume(USAGE_QUEUE_NAME, false, new DefaultConsumer(usageChannel) {
				@Override
				public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
						throws IOException {
					try {
						usageChannel.basicAck(envelope.getDeliveryTag(), false);
						String insert = new String(body, "UTF-8");

						//System.out.println(insert);
						insertBuffer.add(insert);

					} catch (Exception e) {
						LOGGER.error("Handle delivery Error {}", e.getMessage());
					} finally {
						//channel.basicAck(envelope.getDeliveryTag(), false);
					}
				}

				@Override
				public void  handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
					try {
						System.out.println("Received shutdown signal");
						insertBuffer.pushToDB();
						System.out.println("Done pushing remaining data");
					} catch (SQLException e) {
						LOGGER.error("Handle delivery Error {}", e.getMessage());
						e.printStackTrace();
					}
					System.out.println("Done (" + insertBuffer.inserts.size() + " remaining)");
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
	}
	/**
	 * Help formatter. Displays how to launch the application
	 */
	private static void help() {

		HelpFormatter formater = new HelpFormatter();

		formater.printHelp("Maven-miner", options);

		System.exit(0);

	}

	static String insertQuery = "INSERT INTO api_usage (clientid, apimemberid, nb, diversity)\n" + //START TRANSACTION;
			"VALUES\n";

	static File failedToPush = new File("failedToPush.error");
	static File timeLog = new File("usageWritter.time");
	static private SimpleDateFormat formatter;

	static class InsertBuffer {
		int maxInsert;
		List<String> inserts;
		java.sql.Connection db;
		public InsertBuffer(int maxInsert, java.sql.Connection db) {
			this.maxInsert = maxInsert;
			this.inserts = new ArrayList<>(maxInsert);
			this.db = db;
		}

		public void add(String insert) throws IOException {
			inserts.add(insert);
			if(inserts.size() >= maxInsert) {
				try {
					pushToDB();
				} catch (SQLException e) {
					FileUtils.write(failedToPush, String.join("\n", inserts) + "\n",true);
					inserts.clear();
				}
			}
		}
		public void pushToDB() throws SQLException {
			try {
				FileUtils.write(timeLog, formatter.format(new Date()) + " | Start DB push\n", true);
			} catch (IOException e) {
				e.printStackTrace();
			}
			String query = insertQuery + String.join(",", inserts) + ";";//COMMIT;
			PreparedStatement statement = db.prepareStatement(query);

			//PreparedStatement statement = db.prepareStatement(lockBegin + query + ";" + lockEnd);
			statement.execute();
			statement.close();
			db.commit();
			inserts.clear();
			try {
				FileUtils.write(timeLog, formatter.format(new Date()) + " | Done DB push\n", true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
