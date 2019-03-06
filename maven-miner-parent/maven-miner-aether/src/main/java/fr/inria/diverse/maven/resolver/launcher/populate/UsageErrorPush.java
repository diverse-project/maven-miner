package fr.inria.diverse.maven.resolver.launcher.populate;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import fr.inria.diverse.maven.resolver.launcher.ChannelShutdownListener;
import fr.inria.diverse.maven.resolver.launcher.ConnectionShutdownListener;
import fr.inria.diverse.maven.resolver.launcher.UsageResolverApp;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class UsageErrorPush {

	private static ConnectionFactory factory;

	private static Connection connection;

	private static String USAGE_QUEUE_NAME = "usagesQueue";
	private static Channel usageChannel;

	private static final String DEFAULT_USERNAME = "user";

	private static final Options options = new Options();


	public static void main(String[] args) throws IOException, SQLException {

		options.addOption("q", "queue", true, "Hostname and port of the RabbitMQ broker. Note, URI comes in the form hostname:port");
		options.addOption("n", "queue-name", true, "Name of the queue into which dump messages");
		options.addOption("u", "user", true, "User and password of the rabbitMQ. Note, it comes in the form user:password. By default user:user");
		options.addOption("f", "file", true, "List of message to push separated by \\n");


		CommandLineParser parser = new DefaultParser();

		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);
			if (cmd.hasOption("q")) {
				String [] values = cmd.getOptionValue("q").split(":");
				cmd = parser.parse(options, args);
				factory = new ConnectionFactory();

				factory.setHost(values[0]);
				factory.setPort(Integer.valueOf(values[1]));
				String username = DEFAULT_USERNAME;
				String password = DEFAULT_USERNAME;
				if (cmd.hasOption("u")) {
					String [] credentials = cmd.getOptionValue("u").split(":");
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

				if (cmd.hasOption("n")) {
					USAGE_QUEUE_NAME = cmd.getOptionValue("n");
				}


				usageChannel.queueDeclare(USAGE_QUEUE_NAME, true, false, false, lazyArg);

				if (cmd.hasOption("f")) {
					File toPush = new File(cmd.getOptionValue("f"));

					try (BufferedReader br = new BufferedReader(new FileReader(toPush))) {
						for (String line; (line = br.readLine()) != null; ) {
							usageChannel.basicPublish("", USAGE_QUEUE_NAME, null, line.getBytes("UTF-8"));
						}
					}

					try {
						usageChannel.waitForConfirms();
						usageChannel.close();
					} catch (TimeoutException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if(connection.isOpen())
						connection.close();
				}


			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
	}
}
