package fr.inria.diverse.maven.resolver;

import org.junit.Ignore;
import org.junit.Test;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import static org.junit.Assert.*;

public class MetaResolverTest {

	@Ignore
	@Test
	public void deriveRepoTag() {
		String artifactName = "au.com.dius:pact-jvm-consumer_2.11:3.5.15";
		String repo = MetaResolver.deriveRepo(new DefaultArtifact(artifactName));
		assertFalse(repo.equals(""));
		String license = MetaResolver.deriveLicense(new DefaultArtifact(artifactName));
		assertFalse(license.equals(""));
	}
}