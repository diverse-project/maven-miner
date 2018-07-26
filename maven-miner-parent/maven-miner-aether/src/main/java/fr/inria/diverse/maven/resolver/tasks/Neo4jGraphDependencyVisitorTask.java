package fr.inria.diverse.maven.resolver.tasks;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;

import fr.inria.diverse.maven.resolver.MultiTaskDependencyVisitor;
import fr.inria.diverse.maven.resolver.model.Edge.Scope;
import fr.inria.diverse.maven.resolver.util.MavenResolverUtil;

public class Neo4jGraphDependencyVisitorTask extends  AbstractGraphBuilderVisitorTask {
	/**
	 * Me: Obviously a logger... 
	 * Him (N-H): Tnx for the Info Bro! You rock!
	 */
	private static Logger LOGGER = LoggerFactory.getLogger(MultiTaskDependencyVisitor.class);
	/**
	 * The path to the database directory
	 */
	private final String graphDirectory;
	/*
	 * Neo4j {@link GraphDatabaseService}
	 */
	private final GraphDatabaseService graphDB;
	

	/**
	 * Constructor
	 * @param graphDirectory
	 * @reurns {@link Neo4jGraphDependencyVisitorTask}
	 */
	public Neo4jGraphDependencyVisitorTask(String graphDirectory) {
		this.graphDirectory = graphDirectory;
		graphDB = initDB();	
	}
	/**
	 * Default constructor
	 * @return {@link Neo4jGraphDependencyVisitorTask}
	 */
	public Neo4jGraphDependencyVisitorTask() {
		File tmpFile = FileUtils.getTempDirectory();
		this.graphDirectory = tmpFile.getAbsolutePath();
		graphDB = initDB();
	}
	/**
	 * Initiating the database with default option config.
	 * 
	 * @return {@link GraphDatabaseService}
	 */
	private GraphDatabaseService initDB() {
		GraphDatabaseService db =new GraphDatabaseFactory()
	    .newEmbeddedDatabaseBuilder(FileUtils.getFile(graphDirectory))
	    .setConfig( GraphDatabaseSettings.pagecache_memory, "512M" )
	    .setConfig( GraphDatabaseSettings.string_block_size, "60" )
	    .setConfig( GraphDatabaseSettings.array_block_size, "300" )
	    .newGraphDatabase();
		return db;
	}
	/**
	 * @see {@link Neo4jGraphDependencyVisitorTask#enter(DependencyNode)}
	 *
	 */
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
			Label artifactLabel = getOrCreateLabel(artifact.getGroupId());
			result = graphDB.createNode(artifactLabel);
			nodesIndex.put(depKey, result);
			
			result.setProperty(Properties.COORDINATES, depKey);
			result.setProperty(Properties.GROUP, artifact.getGroupId());
			result.setProperty(Properties.CLASSIFIER, artifact.getClassifier());
			result.setProperty(Properties.VERSION, artifact.getVersion());
			result.setProperty(Properties.PACKAGING, MavenResolverUtil.derivePackaging(artifact).toString());
			result.setProperty(Properties.ARTIFACT, artifact.getArtifactId());
			
			tx.success();
		}
		return  result;
	}
	/**
	 * Cheks the existence of the Label on the index and creates a new one if not 
	 * @param groupId {@link String}
	 * @return label {@link Label}
	 */
	private Label getOrCreateLabel(String groupId) {
		if (labelsIndex.containsKey(groupId)) 
			return labelsIndex.get(groupId);
		Label label = Label.label(groupId);
		return label;
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
		
		//TODO creating tasksEvolution
		
		//creating indexes by labels on artifacts coordinates.
		// note labels reflects groupID
		try ( Transaction tx = graphDB.beginTx() )
		{
			LOGGER.info("Creating per Label Index on Artifact coordinates");
		    Schema schema = graphDB.schema();
		    graphDB.getAllLabels().forEach(label ->  {
		    	schema.indexFor(label)
			          .on(Properties.COORDINATES)
			          .create();
		    });		    
		    tx.success();
			LOGGER.info("Index creation finished");

		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw e;
		}
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
		private static final String ARTIFACT = "artifact";
		private static final String SCOPE = "scope";
		
	}
}
