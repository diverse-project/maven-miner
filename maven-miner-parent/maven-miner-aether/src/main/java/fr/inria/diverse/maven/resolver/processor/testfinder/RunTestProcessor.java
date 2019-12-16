package fr.inria.diverse.maven.resolver.processor.testfinder;

import fr.inria.diverse.maven.resolver.processor.CollectArtifactProcessor;
import org.apache.commons.io.FileUtils;
import org.sonatype.aether.artifact.Artifact;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class RunTestProcessor extends CollectArtifactProcessor {

	@Override
	public Artifact process(Artifact artifact) {
		return null;
	}

	static File unresolvedArtifact = new File("test-unresolved");
	static File results = new File("test-results.json");
	static File gaUrl = new File("ga-url.json");

	public static void main(String[] args) throws Exception {
		if(args.length != 2 && args.length != 1) {
			System.out.println("Usage: jar file (githubToken)");
			System.out.println("\tFile contains 1 g:a:pom:v per line");
			return;
		}
		RunTestProcessor processor = new RunTestProcessor();
		FileUtils.write(results, "[" + "\n",false);
		FileUtils.write(unresolvedArtifact, "Start\n", false);

		//File coordinatesPath = new File("/home/nharrand/Documents/maven-miner/maven-miner-parent/maven-miner-aether/org.json-clients-artifacts");
		File coordinatesPath = new File(args[0]);
		if(!coordinatesPath.exists()) {
			System.err.println("File " + args[0] + " does not exists.");
			return;
		}
		if(args.length == 2) {
			GithubAPIClient.init(args[1]);
		} else {
			GithubAPIClient.init(null);
		}

		try (BufferedReader resultsReader = new BufferedReader(new FileReader(coordinatesPath))) {
			String artifactCoordinate;
			int lineCounter = 0;
			int skippedCounter = 0;
			while ((artifactCoordinate = resultsReader.readLine()) != null) {
				if (artifactCoordinate.startsWith("#")) continue;
				++lineCounter;
				try {
					GroupArtifact ga = new GroupArtifact(artifactCoordinate);
					ga.resolve();
					if (ga.status == GroupArtifact.STATUS.COMMIT_RESOLVED) {
						FileUtils.write(results, ga.toJSON() + ",\n", true);
						FileUtils.write(gaUrl, ga.artifactCoordinates + "," + ga.repoUrl + "\n", true);
					} else {
						FileUtils.write(unresolvedArtifact, artifactCoordinate + ": " + ga.status + "\n", true);
					}
					LOGGER.debug("Resolving artifact {} number {} finished", artifactCoordinate, skippedCounter + lineCounter);
				} catch (Exception ee) {
					ee.printStackTrace();
					if(ee instanceof GithubQuotaException) throw new GithubQuotaException();
					FileUtils.write(unresolvedArtifact, artifactCoordinate + ": Unknown\n", true);
				}
			}
		}

		FileUtils.write(results, "]" + "\n",true);


		//processor.process(s1);
		System.out.println("Done");
	}


}
