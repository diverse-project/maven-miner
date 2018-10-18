package fr.inria.diverse.maven.util;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.Dependency;

import fr.inria.diverse.maven.model.Edge;
import fr.inria.diverse.maven.model.Vertex;
import fr.inria.diverse.maven.model.Edge.Scope;
import fr.inria.diverse.maven.model.Vertex.Packaging;

public  class MavenMinerUtil {
	
	
	protected static final SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss z");
	
	public static boolean versionIsGreater (String v1, String v2) {
		
		return false;
	}
	
	/**
	 * Creates a Vertex Object out of a {@link Dependency}
	 * @param dependency
	 * @return {@link Vertex} vertex
	 */
	public static Vertex getVertexFromArtifactCoordinate(@NotNull Dependency dependency) {
        String groupId = dependency.getArtifact().getGroupId();
        String artifactId = dependency.getArtifact().getArtifactId();
        String version = dependency.getArtifact().getVersion();
        Packaging packaging = derivePackaging(dependency.getArtifact());
        String classifier = dependency.getArtifact().getClassifier();
        Vertex artifactVertex = new Vertex(groupId, artifactId, version, classifier, packaging);

        //LOGGER.info(" from split -> " + artifactVertex.toString());
        return artifactVertex;
    }
	/**
	 * 
	 * @param dependency
	 * @return {@link Edge.Scope}
	 */
	public static Scope deriveScope(@NotNull Dependency dependency) {
	        return Scope.parseFromString(dependency.getScope());
	}
	/**
	 * Returns a {@link Packaging} enum type of a giving {@link Artifact}
	 * @param artifact
	 * @return {@link Packaging} 
	 */
	public static Packaging derivePackaging(@NotNull Artifact artifact) {
        return Packaging.parseFromString(artifact.getExtension());
    }
	/**
	 * Creates artifacts coordinates out of an {@link Artifact} object
	 * @param dependency
	 * @return {@link String} coordinates
	 */
	public static String dependencyToCoordinate(@NotNull Dependency dependency) {
		return artifactToCoordinate(dependency.getArtifact());
	}
	/**
	 * Creates artifacts coordinates out of an {@link Artifact} object
	 * @param artifact
	 * @return {@link String} coordinates
	 */
	public static String artifactToCoordinate(@NotNull Artifact artifact) {
		return artifact.getGroupId()
				.concat(":")
				.concat(artifact.getArtifactId())
				.concat(":")
				//.concat(artifact.getExtension()!=null ? artifact.getExtension()+":" : "")
				.concat(artifact.getVersion());
	}
	/**
	 * a dummy method 
	 * @return null
	 */
	public static URL dummyURL() {
		return null;
	}
	/**
	 * Splits a coordinates into 
	 * @param coordinates
	 * @return {@link String}[]
	 */
	//@Length(min=2, max=2, message = "The returned array should contain exactly three elements" )
	public static String[] coordinatesToElements(@Pattern (regexp = ".*?:.*?:.*?", message = "Artifact coordinates' is not well-formed") String coordinates) {
		return coordinates.split(":");
	}
	
	public static ZonedDateTime toZonedTime(String modified) throws ParseException {		
		ZonedDateTime result = null;
		try {
			result = sdf.parse(modified).toInstant().atZone(ZoneId.systemDefault());
		} catch (ParseException e) {
			e.printStackTrace();
			throw e;
		}
		return result;
	}
	
	public static ZonedDateTime fromZonedTime(String time) {
		ZonedDateTime result = null;
		result = ZonedDateTime.parse(time);
		return result;
	}

	
}
