package fr.inria.diverse.maven.resolver.model;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import fr.inria.diverse.maven.resolver.model.Edge.Scope;


public class DependencyGraph {

	private Set<Vertex> vertices = new HashSet<Vertex>();
	private Set<Edge> edges = new HashSet<Edge>();
	public Set<Vertex> getVertices() {
		return vertices;
	}

	
	public void setVertices(@Nonnull Set<Vertex> vertices) {
		this.vertices = vertices;
	}
	
	public boolean addVertex(@Nonnull Vertex vertex) {
		return vertices.add(vertex);
	}

	public Set<Edge> getEdges() {
		return edges;
	}

	public void setEdges(@Nonnull Set<Edge> edges) {
		this.edges = edges;
	}

	public void addDependency(@NotNull Vertex firstLevelArtifactVertex, @NotNull Vertex secondLevelArtifactVerteX, @NotNull Scope scope) {
		vertices.add(firstLevelArtifactVertex);
		vertices.add(secondLevelArtifactVerteX);
		edges.add(new Edge(firstLevelArtifactVertex.getId(), secondLevelArtifactVerteX.getId(), scope));
	}
	
}
