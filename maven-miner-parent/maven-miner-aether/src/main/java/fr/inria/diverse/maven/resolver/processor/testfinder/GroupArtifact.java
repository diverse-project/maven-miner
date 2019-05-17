package fr.inria.diverse.maven.resolver.processor.testfinder;

import fr.inria.diverse.maven.resolver.Booter;
import fr.inria.diverse.maven.resolver.MetaResolver;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.json.simple.parser.ParseException;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GroupArtifact {
	public String entryCoordinate;
	public String artifactCoordinates;
	public File pom;
	public String module = ".";
	public boolean isModule = false;
	public String rawUrl;
	public String repoUrl;
	public String repoName;
	public Map<String, String> versionCommit;
	public STATUS status = STATUS.UNINITIALIZED;
	static final RemoteRepository repo = Booter.newCentralRepository();

	public enum STATUS {UNINITIALIZED, INITIALIZED, POM_RESOLVED, URL_RESOLVED, COMMIT_RESOLVED}



	public GroupArtifact(String entryCoordinate) {
		this.entryCoordinate = entryCoordinate;
		String[] parts = entryCoordinate.split(":");
		this.artifactCoordinates = parts[0] + ":" + parts[1];
		this.status = STATUS.INITIALIZED;
	}

	public void resolve() throws GithubQuotaException, InterruptedException {
		DefaultArtifact artifact = new DefaultArtifact(entryCoordinate);
		pom = getPom(artifact);
		if(pom == null) return;
		status = STATUS.POM_RESOLVED;
		rawUrl = getGithubRepo(pom);
		if(rawUrl == null) return;
		status = STATUS.URL_RESOLVED;
		repoName = getRepoNameFromURL(rawUrl);
		repoUrl = "https://github.com/" + repoName;
		getVersionCommit();
		if(versionCommit == null) return;
		status = STATUS.COMMIT_RESOLVED;
	}

	public static String getRepoNameFromURL(String repoUrl) {
		try {
			//http://github.com/name/repo.git
			if (repoUrl.contains("github.com")) {
				String[] parts = repoUrl.split("github\\.com");
				String repoName = null;
				if(parts[1].startsWith("/")) {
					parts = parts[1].split("/");
					repoName = parts[1] + "/" + parts[2].replace(".git", "");
				} else {
					parts = parts[1].split("/");
					repoName = parts[0].replace(":", "") + "/" + parts[1].replace(".git", "");
				}
				return repoName;
			}
		} catch (Exception e) {

		}
		System.err.println("repoUrl: " + repoUrl);
		return  null;
	}

	public void getVersionCommit() throws GithubQuotaException, InterruptedException {
		String githubRepo = null;
		try {
			versionCommit = GithubAPIClient.getCommits(repoName);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		System.out.println("Repo: " + githubRepo);
	}

	public File getPom(Artifact artifact) {
		ArtifactRequest artifactRequest = new ArtifactRequest();

		artifactRequest.setArtifact(artifact);

		artifactRequest.addRepository(repo);
		try {
			return MetaResolver.resolveJar(artifactRequest);
		} catch (SecurityException | NullPointerException e) {

		}
		return null;
	}



	public String getGithubRepo(File pom) {
		MavenXpp3Reader pomReader = new MavenXpp3Reader();
		try (FileReader reader = new FileReader(pom)) {
			Model model = pomReader.read(reader);
			Scm scm = model.getScm();
			if(scm != null && scm.getUrl() != null) {
				String url = scm.getUrl();
				if (url.contains("github.com")) {
					return MavenHelper.extractVariable(model, url);
				}
			} else {
				Parent parent = model.getParent();
				if(parent != null) {
					DefaultArtifact parentArtifact = new DefaultArtifact(parent.toString());
					this.isModule = true;
					this.module += "/" + model.getArtifactId();
					File parentPom = getPom(parentArtifact);
					return getGithubRepo(parentPom);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String toJSON() {
		if(status == STATUS.COMMIT_RESOLVED) {
			String jsonRes = "{ \"ga\": \"" + artifactCoordinates + "\", " +
					"\"repo\": \"" + repoUrl + "\", " +
					"\"module\": \"" + module + "\", " +
					"\"version\": [" +
					versionCommit.entrySet().stream().map(
							e -> "{\"version\": \"" + e.getKey() + "\", \"commit\": \"" + e.getValue() + "\"}"
					).collect(Collectors.joining(","))
					+ "]}";
			return jsonRes;
		} else {
			return status.toString();
		}
	}
}
