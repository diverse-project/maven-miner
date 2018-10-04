package fr.inria.diverse.maven.resolver.launcher;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.sonatype.aether.util.artifact.DefaultArtifact;

import fr.inria.diverse.maven.resolver.db.Neo4jGraphDBWrapper;
import fr.inria.diverse.maven.resolver.db.Neo4jGraphDBWrapperEmbedded;
import fr.inria.diverse.maven.resolver.processor.AbstractArtifactProcessor;
import fr.inria.diverse.maven.resolver.processor.ClassScanCounter;
import fr.inria.diverse.maven.resolver.processor.CollectAndResolveArtifactProcessor;
import fr.inria.diverse.maven.resolver.processor.CollectArtifactProcessor;
import fr.inria.diverse.maven.resolver.processor.MultiTaskDependencyVisitor;
import fr.inria.diverse.maven.resolver.tasks.DependencyGraphPrettyPrinterTask;
import fr.inria.diverse.maven.resolver.tasks.DependencyVisitorTask;
import fr.inria.diverse.maven.resolver.tasks.Neo4jGraphDeepDependencyVisitorTask;
import fr.inria.diverse.maven.resolver.tasks.Neo4jGraphDependencyVisitorTask;


public class BatchResolverApp {

	/**
	 * The resolver application logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(BatchResolverApp.class);
	
    /**
     * A multiTask visitor. To add additional visit behavior/task @see {@link MultiTaskDependencyVisitor}  
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
		options.addOption("f", "file", true, "Path to artiacts coordinates list file. Note, artifacts are per line");
		options.addOption("p", "pretty-printer", true, "Path to the output file stream. Optional");
		options.addOption("db", "database", true, "Path to store the neo4j database. Mandatory!");
		options.addOption("r", "resolve-jars", false, "Actioning jars resolution and classes count. Not activated by default!");
		options.addOption("d", "deep", false, "Actioning deep dependency resolution. Not activated by default");
		 CommandLineParser parser = new DefaultParser();
		 
		  CommandLine cmd = null;
		  try {
		   cmd = parser.parse(options, args);
		 
		   if (cmd.hasOption("h")) {
		    help();
		   }
		   
		   if (cmd.hasOption("f")) {
			   coordinatesPath = cmd.getOptionValue("f");
		   } 
		   
		   if(cmd.hasOption("p")) {
				DependencyVisitorTask prettyPrinter = new DependencyGraphPrettyPrinterTask();
				myVisitor.addTask(prettyPrinter);
		   }
		   
		   Neo4jGraphDependencyVisitorTask neo4jGraphBuilder = null;
		   if(cmd.hasOption("d")) {
			   neo4jGraphBuilder = new Neo4jGraphDeepDependencyVisitorTask();
		   } else {
			   new Neo4jGraphDependencyVisitorTask();
		   }
		   if(cmd.hasOption("db")) {
			    String path = cmd.getOptionValue("db");
			    dbwrapper = new Neo4jGraphDBWrapperEmbedded(path);			
				neo4jGraphBuilder.setDbWrapper(dbwrapper);
				myCounter.setDbwrapper(dbwrapper);
				myVisitor.addTask(neo4jGraphBuilder);
		   } else {
		    LOGGER.error( "Missing db option");
		    help();
		   }
		   
		   if(cmd.hasOption("r")) {
			   processor = new CollectAndResolveArtifactProcessor(myVisitor, myCounter);
		   } else {
			   processor = new CollectArtifactProcessor(myVisitor);
		   }
		   
		  } catch (ParseException e) {
		   LOGGER.error("Failed to parse comand line properties", e);
		   help();
		  }
		  //open database 
		  BufferedReader resultsReader = null;          
		  try {
        	resultsReader = new BufferedReader(new FileReader(coordinatesPath));
            String artifactCoordinate;
            int lineCounter = 0;
            int skippedCounter = 0;
		    while ((artifactCoordinate = resultsReader.readLine()) != null) {
		            try {
		            	if (artifactCoordinate.startsWith("#")) continue;
		            	++lineCounter;
		                DefaultArtifact artifact = new DefaultArtifact(artifactCoordinate);
		                processor.process(artifact);
		             } catch (Exception ee) {
		            	--lineCounter;
		            	++skippedCounter;
		             	LOGGER.error("Could not resolve artifact: {} ",artifactCoordinate);
		             	ee.printStackTrace();
		             }
		            	LOGGER.debug("Resolving artifact {} number {} finished", artifactCoordinate, skippedCounter+lineCounter);
		    	}
		   		            
            } catch (IOException ioe) {
            	LOGGER.error("Couldn't find file: " + coordinatesPath );
             	ioe.printStackTrace();
             	resultsReader.close();
            } finally {
            	resultsReader.close();
                myVisitor.getTaskSet().forEach(task -> {task.shutdown();});
                processor.report();
            } 
	}
	
	private static void help() {

		  HelpFormatter formater = new HelpFormatter();

		  formater.printHelp("Maven-miner", options);

		  System.exit(0);

	}
}
