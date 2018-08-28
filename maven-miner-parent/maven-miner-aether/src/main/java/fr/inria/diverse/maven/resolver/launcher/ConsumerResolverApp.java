package fr.inria.diverse.maven.resolver.launcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.sonatype.aether.util.artifact.DefaultArtifact;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
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
import fr.inria.diverse.maven.resolver.tasks.Neo4jGraphDependencyVisitorTask;

public class ConsumerResolverApp {
	/**
	 * RabbitMQ fields
	 */
	private static ConnectionFactory factory;
	
	private static Connection connection;
	
	private static Channel channel;
	
    private static final String ARTIFACT_QUEUE_NAME = "artifactsQueue";

	
	/**
	 * The resolver application logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(BatchResolverApp.class);
	
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
		options.addOption("db", "database", true, "Path to store the neo4j database. Mandatory!");
		options.addOption("r", "resolve-jars", false, "Actioning jars resolution and classes count. Not activated by default!");
		options.addOption(new Option("q", "queue", true, "Hostname and port of the RabbitMQ broker. Note, URI comes in the form hostname:port"));

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
		   if(cmd.hasOption("db")) {
			    String path = cmd.getOptionValue("db");
			    dbwrapper = new Neo4jGraphDBWrapperServer(path);
				Neo4jGraphDependencyVisitorTask neo4jGraphBuilder = new Neo4jGraphDependencyVisitorTask();
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
			   factory = new ConnectionFactory();
			   factory.setHost("localhost");
			   connection = factory.newConnection();
			   channel = connection.createChannel();
			   channel.queueDeclare(ARTIFACT_QUEUE_NAME, true, false, false, null);
		   } else {
			   LOGGER.error("Missing the hostname and port of rabbitMQ");
		   }
		   } catch (ParseException e) {
			   LOGGER.error("Failed to parse comand line properties", e);
			   help();
		   } catch (TimeoutException e) {
			   LOGGER.error("Error while trying to connect to the RabbitMQ server");
			   e.printStackTrace();
		   }

		   
		   try {
			  channel.basicConsume(ARTIFACT_QUEUE_NAME, false, new DefaultConsumer(channel) {
				  @Override
			      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
			          throws IOException {
					  try {
						  String artifactCoordinate = new String(body, "UTF-8");
						  DefaultArtifact artifact = new DefaultArtifact(artifactCoordinate);
			              processor.process(artifact);
					  } finally {
						  channel.basicAck(envelope.getDeliveryTag(), false); 
					  }
			      }
			  });
            } catch (IOException ioe) {
            	LOGGER.error("Couldn't find arifact {}", coordinatesPath );
             	ioe.printStackTrace();
            } catch (Exception e) {
            	LOGGER.error("unhandled error");
            	e.printStackTrace();
            }
		  	  finally {
//                myVisitor.getTaskSet().forEach(task -> {task.shutdown();});
//                processor.report();
            }    
	}
	
	private static void help() {

		  HelpFormatter formater = new HelpFormatter();

		  formater.printHelp("Maven-miner", options);

		  System.exit(0);

	}
}
