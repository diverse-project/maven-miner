package fr.inria.diverse.maven.resolver.launcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.artifact.Gav;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.CollectResult;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.JavaScopes;

import fr.inria.diverse.maven.resolver.Booter;
import fr.inria.diverse.maven.resolver.CentralIndex;
import fr.inria.diverse.maven.resolver.MultiTaskDependencyVisitor;
import fr.inria.diverse.maven.resolver.tasks.DependencyGraphPrettyPrinterTask;
import fr.inria.diverse.maven.resolver.tasks.DependencyVisitorTask;
import fr.inria.diverse.maven.resolver.tasks.Neo4jGraphDependencyVisitorTask;


public class ResolverApp{

	private static final Logger LOGGER = LoggerFactory.getLogger(ResolverApp.class);

    private static final RepositorySystem system = Booter.newRepositorySystem();
    private static final RemoteRepository repo = Booter.newCentralRepository();
    private static final RepositorySystemSession session = Booter.newRepositorySystemSession(system);
   
    private static MultiTaskDependencyVisitor myVisitor = new MultiTaskDependencyVisitor();
   
    private static final Options options = new Options();

	private static final String SEPARATOR_ARTIFACTS = ":";
    

	
    
    public static void resolveDependencyforArtifact(Artifact artifact) throws DependencyCollectionException {

        LOGGER.info("Resolving artifact: " + artifact);
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, JavaScopes.COMPILE)); 
        collectRequest.addRepository(repo);

        CollectResult collectResult = system.collectDependencies(session, collectRequest);
       // DependencyGraph localDependencies = new DependencyGraph();
        collectResult.getRoot().accept(myVisitor);

        //return localDependencies;
    }
	@SuppressWarnings("null")
	public static void main(String[] args) throws IOException {
		
		//initialize arguments
		String coordinatesPath = "src/main/resources/allUniqueArtifactsOnly-mini-100";
//		boolean skipBuild = true;
		options.addOption("h", "help", false, "Show help");
		options.addOption("f", "file", true, "Path to artiacts coordinates list file. Note, artifacts are per line");
		options.addOption("p", "pretty-printer", true, "Path to the output file stream. Optional");
		options.addOption("db", "database", true, "Path to store the neo4j database. Mandatory!");
//		options.addOption("b", "build-artifacts", false, "building the artifacts file at the location specified by the option -f [--file]. If not specified, allArtifacts is used by default as a name");
		
		 CommandLineParser parser = new DefaultParser();
		 
		  CommandLine cmd = null;
		  try {
		   cmd = parser.parse(options, args);
		 
		   if (cmd.hasOption("h")) {
		    help();
		   }
//		   if (cmd.hasOption("b")) {
//			   skipBuild = false;
//			   coordinatesPath = "results/allArtifacts";
//		   }
		   if (cmd.hasOption("f")) {
			   coordinatesPath = cmd.getOptionValue("f");
		   } 
		   
		   if(cmd.hasOption("p")) {
				DependencyVisitorTask prettyPrinter = new DependencyGraphPrettyPrinterTask();
				myVisitor.addTask(prettyPrinter);
		   } 
		   
		   if(cmd.hasOption("db")) {
			   String path = cmd.getOptionValue("db");
				DependencyVisitorTask neo4jGraphBuilder = new Neo4jGraphDependencyVisitorTask(path);
				myVisitor.addTask(neo4jGraphBuilder);
		   } else {
		    LOGGER.error( "Missing db option");
		    help();
		   }
		 
		  } catch (ParseException e) {
		   LOGGER.error("Failed to parse comand line properties", e);
		   help();
		  }
		 BufferedReader resultsReader = null;
        try {
//        	if (!skipBuild) {
//        		writeAllArtifactInfo(coordinatesPath);
//        	}
        	resultsReader = new BufferedReader(new FileReader(coordinatesPath));
            String artifactCoordinate;
            int lineCounter = 0;
            int skippedCounter = 0;
		    while ((artifactCoordinate = resultsReader.readLine()) != null) {
		            try {
		            	if (artifactCoordinate.startsWith("#")) continue;
		            	++lineCounter;
		                DefaultArtifact artifact = new DefaultArtifact(artifactCoordinate);
		                resolveDependencyforArtifact(artifact);
		             } catch (Exception ee) {
		            	--lineCounter;
		            	++skippedCounter;
		             	LOGGER.error("Could not resolve artifact: {} ",artifactCoordinate);
		             	ee.printStackTrace();
		             }
		            }
		            
		            LOGGER.info(" {} artifacts have been resolved", lineCounter); 
		            LOGGER.info(" {} artifacts have been skipped", skippedCounter); 
		            
            } catch (IOException ioe) {
            	LOGGER.error("Couldn't find file: " + coordinatesPath );
             	ioe.printStackTrace();
             	resultsReader.close();
            } {
            	//visitors.shutdown
            }    
        resultsReader.close();
        myVisitor.getTasksList().forEach(task -> {task.shutdown();});
	}

	private static void help() {

		  HelpFormatter formater = new HelpFormatter();

		  formater.printHelp("Maven-miner", options);

		  System.exit(0);

	}

	public static void writeAllArtifactInfo(String fileName) {
        try {
            CentralIndex centralIndex = new CentralIndex();
            centralIndex.buildCentralIndex();
            int totalArtifactsNumber = centralIndex.allArtifactSize();
            int subListsNumber = 100000;
            int count = 0, error = 0;
            Set<ArtifactInfo> artifactInfoSet;
            BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
            Gav gav;
            String groupId;
            String artifactId;
            String version;
            for (int index = 0; index < subListsNumber; index++) {
                artifactInfoSet = centralIndex.partialArtifactInfo(
                        (int) (totalArtifactsNumber * index / subListsNumber),
                        Math.min((int) (totalArtifactsNumber * (index + 1) / subListsNumber),
                                (int) totalArtifactsNumber));
                //Log.debug("Storing artifacts from index {} to {}", (totalArtifactsNumber * index) / subListsNumber, Math.min((totalArtifactsNumber * (index + 1)) / subListsNumber, totalArtifactsNumber));
                String text = "";
                for (ArtifactInfo ai : artifactInfoSet) {
                    try {
                        gav = ai.calculateGav();
                        groupId = gav.getGroupId();
                        artifactId = gav.getArtifactId();
                        version = gav.getVersion();
                        text += groupId + SEPARATOR_ARTIFACTS
                                + artifactId + SEPARATOR_ARTIFACTS
                                + version+System.getProperty("line.separator");
                        count++;
                    } catch (Exception ex) {
                        error++;
                    }
                }
                bw.write(text);
                bw.flush();
            }
            bw.close();
            LOGGER.info("count: {}, error: {}", count, error);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
