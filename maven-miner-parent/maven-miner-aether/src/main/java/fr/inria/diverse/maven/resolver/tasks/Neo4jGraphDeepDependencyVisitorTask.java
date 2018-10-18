package fr.inria.diverse.maven.resolver.tasks;

import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.DependencyNode;

import fr.inria.diverse.maven.model.Edge.Scope;
import fr.inria.diverse.maven.util.MavenMinerUtil;

public class Neo4jGraphDeepDependencyVisitorTask extends  Neo4jGraphDependencyVisitorTask {
	
	/**
	 * Default constructor
	 */
	public Neo4jGraphDeepDependencyVisitorTask () {
		super();
	}
	/**
	 * @see Neo4jGraphDeepDependencyVisitorTask#enter(DependencyNode)
	 */
	@Override
	public void enter(DependencyNode node) {
		depth++;
        if (depth == 1) {
        	root = node.getDependency().getArtifact();  	
            dbWrapper.createNodeFromArtifactCoordinate( node.getDependency().getArtifact());
            stack.push(root); 
        }
        // get the nodes on the second level (the direct dependencies), 
        //and add these with the first node to the graph
        else {
        	root = stack.peek();
        	Artifact secondLevelNode = node.getDependency().getArtifact();
        	dbWrapper.createNodeFromArtifactCoordinate(secondLevelNode);
        	// add dependency to graph
            Scope scope = MavenMinerUtil.deriveScope(node.getDependency());      
            dbWrapper.addDependency(root, secondLevelNode, scope);
            stack.push(secondLevelNode);
        }
	}

	/**
	 * @see Neo4jGraphDeepDependencyVisitorTask#leave(DependencyNode)
	 */
	@Override
	public void leave(DependencyNode node) {
		depth--;
		stack.pop();
	}

}
