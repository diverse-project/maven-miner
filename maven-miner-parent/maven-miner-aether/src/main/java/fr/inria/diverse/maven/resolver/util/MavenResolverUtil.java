package fr.inria.diverse.maven.resolver.util;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.Dependency;

import fr.inria.diverse.maven.resolver.model.Vertex;
import fr.inria.diverse.maven.resolver.model.Edge.Scope;
import fr.inria.diverse.maven.resolver.model.Vertex.Packaging;

public  class MavenResolverUtil {

	
	public static Vertex getVertexFromArtifactCoordinate(Dependency dependency) {
        String groupId = dependency.getArtifact().getGroupId();
        String artifactId = dependency.getArtifact().getArtifactId();
        String version = dependency.getArtifact().getVersion();
        Packaging packaging = derivePackaging(dependency.getArtifact());
        String classifier = dependency.getArtifact().getClassifier();
        Vertex artifactVertex = new Vertex(groupId, artifactId, version, classifier, packaging);

        //LOGGER.info(" from split -> " + artifactVertex.toString());
        return artifactVertex;
    }
	public static Scope deriveScope(Dependency dependency) {
	        return Scope.parseFromString(dependency.getScope());
	}
	public static Packaging derivePackaging(Artifact artifact) {
        return Packaging.parseFromString(artifact.getExtension());
    }
	public static Node getNodeFromArtifactCoordinate(GraphDatabaseService graphDB, Dependency dependency) {
		
		return null;
	}
	public static void addDependencyToGraphDB(GraphDatabaseService graphDB, Node root, Node secondLevelNode,
			Scope scope) {
		// TODO Auto-generated method stub
		
	}
	public static String dependencyToCoordinate(Dependency dependency) {
		Artifact artifact = dependency.getArtifact();
		return artifact.getGroupId()
				.concat(":")
				.concat(artifact.getArtifactId())
				.concat(":")
				.concat(artifact.getVersion());
	}
}
