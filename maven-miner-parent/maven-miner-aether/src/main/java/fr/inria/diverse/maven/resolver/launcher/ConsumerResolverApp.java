package fr.inria.diverse.maven.resolver.launcher;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

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

import fr.inria.diverse.maven.resolver.db.Neo4jGraphDBWrapper;
import fr.inria.diverse.maven.resolver.db.Neo4jGraphDBWrapperServer;
import fr.inria.diverse.maven.resolver.processor.AbstractArtifactProcessor;
import fr.inria.diverse.maven.resolver.processor.ClassScanCounter;
import fr.inria.diverse.maven.resolver.processor.CollectAndResolveArtifactProcessor;
import fr.inria.diverse.maven.resolver.processor.CollectArtifactProcessor;
import fr.inria.diverse.maven.resolver.processor.MultiTaskDependencyVisitor;
import fr.inria.diverse.maven.resolver.tasks.DependencyGraphPrettyPrinterTask;
import fr.inria.diverse.maven.resolver.tasks.DependencyVisitorTask;
import fr.inria.diverse.maven.resolver.tasks.Neo4jGraphDeepDependencyVisitorTask;
import fr.inria.diverse.maven.resolver.tasks.Neo4jGraphDependencyVisitorTask;

public class ConsumerResolverApp {


	/**
	 * RabbitMQ fields
	 */
	private static ConnectionFactory factory;
	
	private static Connection connection;
	
	private static Channel channel;
	
    private static final String ARTIFACT_QUEUE_NAME = "artifactsQueue";

    private static final String DEFAULT_USERNAME = "user";
	
	/**
	 * The resolver application logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerResolverApp.class);
	
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
	private static Neo4jGraphDBWrapper dbwrapper;
   /**
    * Cli options
    */
    private static final Options options = new Options();
    
	@SuppressWarnings("null")
	public static void main(String[] args) throws IOException {
		
		//initialize arguments
		String coordinatesPath = "src/main/resources/allUniqueArtifactsOnly-mini-100";
		
		options.addOption("h", "help", false, "Show help");
		options.addOption("p", "pretty-printer", true, "Path to the output file stream. Optional");
		options.addOption("db", "database", true, "Hostname of the neo4j server. <host:port>. Required");
		options.addOption("r", "resolve-jars", false, "Actioning jars resolution and classes count. Not activated by default!");
		options.addOption("q", "queue", true, "Hostname and port of the RabbitMQ broker. Note, URI comes in the form hostname:port");
		options.addOption("d", "deep", false, "Actioning deep dependency resolution. Not activated by default");
		options.addOption("u", "user", true, "User and password of the rabbitMQ. Note, it comes in the form user:password. By default user:user");
		
		CommandLineParser parser = new DefaultParser();
		 
		  CommandLine cmd = null;
		   try {
		   cmd = parser.parse(options, args);
		   if (cmd.hasOption("h")) {
		    help();
		   }
		   if(cmd.hasOption("p")) {
				DependencyVisitorTask prettyPrinter = new DependencyGraphPrettyPrinterTask();
				myVisitor.addTask(prettyPrinter);
		   }
		   Neo4jGraphDependencyVisitorTask neo4jGraphBuilder = null;
		   if(cmd.hasOption("d")) {
			   neo4jGraphBuilder = new Neo4jGraphDeepDependencyVisitorTask();
		   } else {
			   neo4jGraphBuilder = new Neo4jGraphDependencyVisitorTask();
		   }
		   if(cmd.hasOption("db")) {
			    String path = cmd.getOptionValue("db");
			    dbwrapper = new Neo4jGraphDBWrapperServer(path);
				//Neo4jGraphDependencyVisitorTask neo4jGraphBuilder = new Neo4jGraphDependencyVisitorTask();
				neo4jGraphBuilder.setDbWrapper(dbwrapper);
				myCounter.setDbwrapper(dbwrapper);
				myVisitor.addTask(neo4jGraphBuilder);
		   } else {
		    LOGGER.error( "Missing db options");
		    help();
		   }
		   if(cmd.hasOption("r")) {
			   processor = new CollectAndResolveArtifactProcessor(myVisitor, myCounter);
		   } else {
			   processor = new CollectArtifactProcessor(myVisitor);
		   }
		   if (cmd.hasOption("q")) {
			   String [] values = cmd.getOptionValue("q").split(":");
	    	   //check the presence of the port number 
	    	   if (values.length!=2) {
				   LOGGER.error("Couldnt handle hostname \"{}\"",cmd.getOptionValue("q"));
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
						  LOGGER.error("Handle deleviery Error {}", e.getMessage());
					  }  finally {
						  //channel.basicAck(envelope.getDeliveryTag(), false); 
					  }
			      }
			  });
            } catch (IOException ioe) {
            	LOGGER.error("Couldn't find arifact {}", coordinatesPath );
             	ioe.printStackTrace();
            } catch (Exception e) {
            	LOGGER.error("unhandled error {}",e.getMessage());
            	e.printStackTrace();
            }
		  	  finally {
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

}
