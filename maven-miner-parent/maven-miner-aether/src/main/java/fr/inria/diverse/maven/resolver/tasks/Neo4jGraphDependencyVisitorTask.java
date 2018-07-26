package fr.inria.diverse.maven.resolver.tasks;

import java.io.File;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import fr.inria.diverse.maven.resolver.MultiTaskDependencyVisitor;
import fr.inria.diverse.maven.resolver.model.Edge.Scope;
import fr.inria.diverse.maven.resolver.util.MavenResolverUtil;

public class Neo4jGraphDependencyVisitorTask implements DependencyVisitorTask {

	private static Logger LOGGER = LoggerFactory.getLogger(MultiTaskDependencyVisitor.class);
	
	private final String graphDirectory;
	private final GraphDatabaseService graphDB;
	private Stack<Node> stack = new Stack<Node>();
	
	private Node root;
	
	private int depth = 0;
	
	private Map<String,Node> nodesIndex = new ConcurrentHashMap<String,Node>();
	private ListMultimap<String, String> edgesIndex = ArrayListMultimap.create();

	
	
	public Neo4jGraphDependencyVisitorTask(String graphDirectory) {
		this.graphDirectory = graphDirectory;
		graphDB = initDB();
		
	}
	
	public Neo4jGraphDependencyVisitorTask() {
		File tmpFile = FileUtils.getTempDirectory();
		this.graphDirectory = tmpFile.getAbsolutePath();
		graphDB = initDB();
	}
	
	private GraphDatabaseService initDB() {
		GraphDatabaseService db =new GraphDatabaseFactory()
	    .newEmbeddedDatabaseBuilder(FileUtils.getFile(graphDirectory))
	    .setConfig( GraphDatabaseSettings.pagecache_memory, "512M" )
	    .setConfig( GraphDatabaseSettings.string_block_size, "60" )
	    .setConfig( GraphDatabaseSettings.array_block_size, "300" )
	    .newGraphDatabase();
		return db;
	}
	
	@Override
	public void enter(DependencyNode node) {
		depth++;
        if (depth == 1) {      	
            root = getNodeFromArtifactCoordinate( node.getDependency());
            stack.push(root); 
        }
        // get the nodes on the second level (the direct dependencies), and add these with the first node to the graph
        else {
        	root = stack.peek();
            Node secondLevelNode = getNodeFromArtifactCoordinate(node.getDependency());

            Scope scope = MavenResolverUtil.deriveScope(node.getDependency());      
            addDependencyToGraphDB(root, secondLevelNode, scope);
            stack.push(secondLevelNode);
        }

	}

	private void addDependencyToGraphDB( Node source, Node target, Scope scope) {
				
		try ( Transaction tx = graphDB.beginTx() ) { 
			String sourceCoord = (String) source.getProperty(Properties.COORDINATES);
			String targetCoor = (String) target.getProperty(Properties.COORDINATES);
			//If the edge already exists do nothing
			if (edgesIndex.containsEntry(sourceCoord, targetCoor)) return;
			//Otherwise create one ...
			Relationship relation = source.createRelationshipTo(target, DependencyRelation.DEPENDS_ON);
			relation.setProperty(Properties.SCOPE, scope.toString());

			tx.success();
			//... and update the index
			edgesIndex.put(sourceCoord, targetCoor);
		}
		
	}

	private Node getNodeFromArtifactCoordinate( Dependency dependency) {
		
		String depKey = MavenResolverUtil.dependencyToCoordinate(dependency);
		Artifact artifact = dependency.getArtifact();
		Node result;
		if (nodesIndex.containsKey(depKey))
			return nodesIndex.get(depKey);
		try ( Transaction tx = graphDB.beginTx() ) { 
			LOGGER.info("adding artifact: "+depKey);
			result = graphDB.createNode();
			nodesIndex.put(depKey, result);
			
			result.setProperty(Properties.COORDINATES, depKey);
			result.setProperty(Properties.GROUP, artifact.getGroupId());
			result.setProperty(Properties.CLASSIFIER, artifact.getClassifier());
			result.setProperty(Properties.VERSION, artifact.getVersion());
			result.setProperty(Properties.PACKAGING, MavenResolverUtil.derivePackaging(artifact).toString());
			
			tx.success();
		}
		return  result;
	}

	@Override
	public void leave(DependencyNode node) {
		depth--;
		stack.pop();
	}

	private static void registerShutdownHook( final GraphDatabaseService graphDb ) {
	    Runtime.getRuntime().addShutdownHook( new Thread(() -> {graphDb.shutdown();}));
	}
	
	@Override
	public void init() {
		registerShutdownHook(this.graphDB);
	}
	
	@Override
	public void shutdown() {
		graphDB.shutdown();
	}
	
	/**
	 * 
	 * @author Amine BENELALLAM
 	 *
	 */
	private static enum DependencyRelation implements RelationshipType {
		DEPENDS_ON
	}
	
	private class Properties {
		private static final String COORDINATES = "coordinates";
		private static final String GROUP = "groupID";
		private static final String VERSION = "version";
		private static final String PACKAGING = "packaging";
		private static final String CLASSIFIER = "classifier";
		private static final String SCOPE = "scope";
		
	}
}
