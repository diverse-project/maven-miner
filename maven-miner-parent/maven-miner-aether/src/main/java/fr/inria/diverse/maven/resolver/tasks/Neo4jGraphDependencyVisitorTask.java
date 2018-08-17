package fr.inria.diverse.maven.resolver.tasks;

import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.DependencyNode;

import fr.inria.diverse.maven.resolver.db.Neo4jGraphDBWrapper;
import fr.inria.diverse.maven.resolver.model.Edge.Scope;
import fr.inria.diverse.maven.resolver.util.MavenResolverUtil;

public class Neo4jGraphDependencyVisitorTask extends  AbstractGraphBuilderVisitorTask {


	/**
	 * A wrapper to perform common operations to store maven dependencies
	 */
	private Neo4jGraphDBWrapper dbWrapper;
	
	/**
	 * 
	 * @return dbWrapper {@link Neo4jGraphDBWrapper}
	 */
	public Neo4jGraphDBWrapper getDbWrapper() {
		return dbWrapper;
	}
	/**
	 * 
	 * @param dbWrapper {@link Neo4jGraphDBWrapper}
	 */
	public void setDbWrapper(Neo4jGraphDBWrapper dbWrapper) {
		this.dbWrapper = dbWrapper;
	}

	/**
	 * @see {@link Neo4jGraphDependencyVisitorTask#enter(DependencyNode)}
	 *
	 */
	@Override
	public void enter(DependencyNode node) {
		depth++;
        if (depth == 1) {
        	root = node.getDependency().getArtifact();
        	
            dbWrapper.getNodeFromArtifactCoordinate( node.getDependency().getArtifact());
            stack.push(root); 
        }
        // get the nodes on the second level (the direct dependencies), 
        //and add these with the first node to the graph
        else {
        	root = stack.peek();
        	Artifact secondLevelNode = node.getDependency().getArtifact();
        	dbWrapper.getNodeFromArtifactCoordinate(secondLevelNode);

            Scope scope = MavenResolverUtil.deriveScope(node.getDependency());      
            dbWrapper.addDependencyToGraphDB(root, secondLevelNode, scope);
            stack.push(secondLevelNode);
        }
	}

	/**
	 * @see Neo4jGraphDependencyVisitorTask#leave(DependencyNode)
	 */
	@Override
	public void leave(DependencyNode node) {
		depth--;
		stack.pop();
	}

	/**
	 * @see Neo4jGraphDependencyVisitorTask#init()
	 */
	@Override
	public void init() {
		dbWrapper.registerShutdownHook();
	}
	/**
	 * @see Neo4jGraphDependencyVisitorTask#shutdown()
	 */
	@Override
	public void shutdown() {	
		//creating release precedence
		dbWrapper.createPrecedenceShip();
		
		//creating indexes
		dbWrapper.createIndexes();
		
		//Shutting down database
		//dbWrapper.shutdown();
	}
	





}
