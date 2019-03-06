package fr.inria.diverse.maven.resolver.launcher;

import fr.inria.diverse.maven.resolver.db.sql.MariaDBWrapper;
import fr.inria.diverse.maven.resolver.processor.AbstractArtifactProcessor;
import fr.inria.diverse.maven.resolver.processor.LibApiProcessor;
import fr.inria.diverse.maven.resolver.processor.LibProcessor;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;

public class LibApiResolverApp {

		/**
		 * The resolver application logger
		 */
		private static final Logger LOGGER = LoggerFactory.getLogger(BatchResolverApp.class);
		/**
		 * Cli options
		 */
		private static final Options options = new Options();

		@SuppressWarnings("null")
		public static void main(String[] args) throws IOException, SQLException {


			//initialize arguments
			String coordinatesPath = "./libraries";

			options.addOption("h", "help", false, "Show help");

			options.addOption("f", "file", true, "File containing list of gav");
			CommandLineParser parser = new DefaultParser();

			CommandLine cmd = null;
			AbstractArtifactProcessor processor = new LibApiProcessor(new MariaDBWrapper().getConnection());
			try {
				cmd = parser.parse(options, args);

				if (cmd.hasOption("h")) {
					help();
				}

				if (cmd.hasOption("f")) {
					coordinatesPath = cmd.getOptionValue("f");
				}
			} catch (ParseException e) {
				LOGGER.error("Failed to parse command line properties", e);
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
				processor.report();
			}
		}

		private static void help() {

			HelpFormatter formater = new HelpFormatter();

			formater.printHelp("Maven-miner", options);

			System.exit(0);

		}
}
