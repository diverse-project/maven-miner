package fr.inria.diverse.maven.resolver.processor.testfinder;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.Assert.*;

public class MavenHelperTest {

	@Test
	public void extractVariable() throws IOException, XmlPullParserException {
		File pom = new File("/home/nharrand/Documents/maven-miner/maven-miner-parent/target/local-repo/net/adamcin/granite/granite-client-packman/0.8.1/granite-client-packman-0.8.1.pom");
		MavenXpp3Reader pomReader = new MavenXpp3Reader();
		FileReader reader = new FileReader(pom);
		Model model = pomReader.read(reader);
		String url = model.getScm().getUrl();
		String var = MavenHelper.extractVariable(model, url);
		assertEquals(var, "https://github.com/adamcin/granite-client-packman");
		String var2 = MavenHelper.extractVariable(model, var);
		assertEquals(var, var2);
	}
}