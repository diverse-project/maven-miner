package fr.inria.diverse.maven.resolver;

import fr.inria.diverse.maven.resolver.processor.testfinder.MavenHelper;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MetaResolver {
	protected static final Logger LOGGER = LoggerFactory.getLogger(MetaResolver.class);
	protected static final RepositorySystem system = Booter.newRepositorySystem();
	protected static final RepositorySystemSession session = Booter.newRepositorySystemSession(system);

	static private Map<String, RemoteRepository> repositories = new HashMap<>();
	static private RemoteRepository defaultRepository = Booter.newCentralRepository();
	static private String mvnRepositoriyComUrl = "https://mvnrepository.com/artifact/";

	static private String getRepostories(Artifact artifact) {
		String url = mvnRepositoriyComUrl + artifact.getGroupId() + "/" + artifact.getArtifactId() + "/" + artifact.getVersion();
		String repoUrl = null;
		Document doc = null;
		try {
			doc = Jsoup.connect(url).get();

			Elements search = doc.getElementsContainingText(": this artifact it located at");
			if(search.size() == 0) return null;
			for(Node node : search.last().childNodes()) {
				if(!node.toString().contains("repository (")) continue;
				repoUrl = node.toString().split("\\(")[1].split("\\)")[0];
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return repoUrl;
	}

	static public File resolveJar(ArtifactRequest request) {
		File jarFile = null;
		request.setRepositories(Arrays.asList(defaultRepository));
		ArtifactResult artifactResult = null;
		try {

			artifactResult = system.resolveArtifact(session, request);
			jarFile= artifactResult.getArtifact().getFile();

		} catch (ArtifactResolutionException
				| SecurityException
				| NullPointerException e) {
			LOGGER.warn("Unable to read artifact from maven.central {}", request.getArtifact());
		}
		if(jarFile != null) return jarFile;

		String alternativeRepo = getRepostories(request.getArtifact());
		if(alternativeRepo == null) return null;
		RemoteRepository repo = repositories.computeIfAbsent(alternativeRepo, r -> new RemoteRepository(alternativeRepo, "default", alternativeRepo));
		request.setRepositories(Arrays.asList(repo));
		try {
			artifactResult = system.resolveArtifact(session, request);
			jarFile= artifactResult.getArtifact().getFile();

		} catch (ArtifactResolutionException
				| SecurityException
				| NullPointerException e) {
			LOGGER.error("Unable to read artifact {}", request.getArtifact());
		}
		return jarFile;
	}

	static public File resolvePom(Artifact artifact) {
		Artifact artifact2 = new DefaultArtifact(
				artifact.getGroupId() + ":" +
						artifact.getArtifactId() + ":pom:" +
						artifact.getVersion()
		);

		ArtifactRequest request = new ArtifactRequest();
		request.setArtifact(artifact2);
		File pomFile = null;
		request.setRepositories(Arrays.asList(defaultRepository));
		ArtifactResult artifactResult = null;
		try {

			artifactResult = system.resolveArtifact(session, request);
			pomFile = artifactResult.getArtifact().getFile();

		} catch (ArtifactResolutionException
				| SecurityException
				| NullPointerException e) {
			LOGGER.warn("Unable to read artifact from maven.central {}", request.getArtifact());
		}
		if(pomFile != null) return pomFile;

		String alternativeRepo = getRepostories(request.getArtifact());
		if(alternativeRepo == null) return null;
		RemoteRepository repo = repositories.computeIfAbsent(alternativeRepo, r -> new RemoteRepository(alternativeRepo, "default", alternativeRepo));
		request.setRepositories(Arrays.asList(repo));
		try {
			artifactResult = system.resolveArtifact(session, request);
			pomFile = artifactResult.getArtifact().getFile();

		} catch (ArtifactResolutionException
				| SecurityException
				| NullPointerException e) {
			LOGGER.error("Unable to read artifact {}", request.getArtifact());
		}
		return pomFile;
	}

	public static String deriveLicense(Artifact artifact) {
		String license = "";
		try {
			File pom = resolvePom(artifact);

			MavenXpp3Reader pomReader = new MavenXpp3Reader();
			try (FileReader reader = new FileReader(pom)) {
				Model model = pomReader.read(reader);
				for (License l :model.getLicenses()) {
					license += (l.getName() == null ? "" : l.getName()) + "|" +
							(l.getUrl() == null ? "" : l.getUrl()) + "|";
				}
				return license;

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			}
		} catch (Exception e) {}
		return "";
	}

	public static String deriveParent(Artifact artifact) {
		String parent;
		try {
			File pom = resolvePom(artifact);

			MavenXpp3Reader pomReader = new MavenXpp3Reader();
			try (FileReader reader = new FileReader(pom)) {
				Model model = pomReader.read(reader);
				Parent parentO = model.getParent();
				parent = parentO.getGroupId() + ":" + parentO.getArtifactId() + ":" + parentO.getVersion();
				return parent;

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			}
		} catch (Exception e) {}
		return null;
	}

	public static String deriveRepo(Artifact artifact) {
		try {
			File pom = resolvePom(artifact);

			MavenXpp3Reader pomReader = new MavenXpp3Reader();
			try (FileReader reader = new FileReader(pom)) {
				Model model = pomReader.read(reader);

				Scm scm = model.getScm();
				if (scm != null && scm.getUrl() != null) {
					String url = scm.getUrl();
					return url;
				} else {
					Parent parent = model.getParent();
					if (parent != null) {
						DefaultArtifact parentArtifact = new DefaultArtifact(parent.toString());
						//this.isModule = true;
						//this.module += "/" + model.getArtifactId();
						return deriveRepo(parentArtifact);
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			}
		} catch (Exception e) {}
		return "";
	}
}
