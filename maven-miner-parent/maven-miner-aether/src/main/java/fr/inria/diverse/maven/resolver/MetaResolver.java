package fr.inria.diverse.maven.resolver;

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

import java.io.File;
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
}
