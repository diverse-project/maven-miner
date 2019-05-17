package fr.inria.diverse.maven.resolver.processor.amqtasks;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import fr.inria.diverse.maven.resolver.launcher.ChannelShutdownListener;
import fr.inria.diverse.maven.resolver.launcher.ConnectionShutdownListener;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

public abstract class AbstractTask implements Task {
	private static ConnectionFactory factory;

	private static Connection connection;

	private static JSONParser parser = new JSONParser();

	private String IN_Q;
	private String OUT_Q;
	private String ERR_Q;

	private Channel channel;

	public void init(Properties config) {
		try {
			IN_Q = config.getProperty("IN_Q");
			OUT_Q = config.getProperty("OUT_Q");
			ERR_Q = config.getProperty("ERR_Q");
			String username = config.getProperty("Q_user");
			String password = config.getProperty("Q_password");
			String host = config.getProperty("Q_host");
			String port = config.getProperty("Q_port");

			factory = new ConnectionFactory();

			factory.setHost(host);
			factory.setPort(Integer.valueOf(port));
			factory.setUsername(username);
			factory.setPassword(password);
			factory.setNetworkRecoveryInterval(10000);
			factory.setAutomaticRecoveryEnabled(true);
			connection = factory.newConnection();
			connection.addShutdownListener(new ConnectionShutdownListener());
			channel = connection.createChannel();
			channel.basicQos(1);
			channel.addShutdownListener(new ChannelShutdownListener(connection));
			Map<String, Object> lazyArg = new HashMap<String, Object>();
			lazyArg.put("x-queue-mode", "lazy");
			channel.queueDeclare(IN_Q, true, false, false, lazyArg);
			channel.queueDeclare(OUT_Q, true, false, false, lazyArg);
			channel.queueDeclare(ERR_Q, true, false, false, lazyArg);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
	}

	public void run() {

		try {
			channel.basicConsume(IN_Q, false, new DefaultConsumer(channel) {
				@Override
				public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
						throws IOException {
					try {
						channel.basicAck(envelope.getDeliveryTag(), false);
						String inData = new String(body, "UTF-8");
						JSONObject in = (JSONObject) parser.parse(inData);

						Map.Entry<JSONObject, JSONObject> result = process(in);

						if(result.getKey() != null) {
							JSONObject out = result.getKey();
							channel.basicPublish("", OUT_Q, null, out.toJSONString().getBytes("UTF-8"));
						}

						if(result.getValue() != null) {
							JSONObject err = result.getValue();
							channel.basicPublish("", ERR_Q, null, err.toJSONString().getBytes("UTF-8"));
						}
					} catch (Exception e) {
						//LOGGER.error("Handle delivery Error {}", e.getMessage());
					} finally {
						//channel.basicAck(envelope.getDeliveryTag(), false);
					}
				}

				@Override
				public void  handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
				}
			});
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			//healthChecker.shutdownNow();
		}
	}

	public void stop() {

	}


	public abstract Map.Entry<JSONObject, JSONObject> process(JSONObject data);


}
