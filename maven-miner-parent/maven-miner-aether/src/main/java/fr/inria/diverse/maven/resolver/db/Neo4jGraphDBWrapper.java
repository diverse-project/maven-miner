package fr.inria.diverse.maven.resolver.db;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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
import org.sonatype.aether.version.InvalidVersionSpecificationException;

import fr.inria.diverse.maven.resolver.model.Edge.Scope;
import fr.inria.diverse.maven.resolver.processor.MultiTaskDependencyVisitor;
import fr.inria.diverse.maven.resolver.model.JarCounter;
import fr.inria.diverse.maven.resolver.tasks.Neo4jGraphDependencyVisitorTask;
import fr.inria.diverse.maven.resolver.util.MavenResolverUtil;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.sonatype.aether.util.version.GenericVersionScheme;
import org.sonatype.aether.version.Version;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class Neo4jGraphDBWrapper {
	/**
	 * Me: Obviously a logger... 
	 * Him (N-H): Tnx for the Info Bro! You rock!
	 */
	private static Logger LOGGER = LoggerFactory.getLogger(MultiTaskDependencyVisitor.class);
	/**
	 * An index of already resolved artifacts in the form of Neo4j {@link Node} 
	 */
	protected Map<String,Node> nodesIndex = new HashMap<String,Node>();
	/**
	 * An index of already resolved dependency relationships
	 */
	protected ListMultimap<String, String> edgesIndex = ArrayListMultimap.create();
	/**
	 * A String to Label map to cache already created labels
	 */
	protected Map<String,Label> labelsIndex = new HashMap<String,Label>();
	/**
	 * The path to the database directory
	 */
	protected final String graphDirectory;
	/*
	 * Neo4j {@link GraphDatabaseService}
	 */
	protected final GraphDatabaseService graphDB;
	
	/**
	 * Constructor
	 * @param graphDirectory
	 * @reurns {@link Neo4jGraphDependencyVisitorTask}
	 */
	public Neo4jGraphDBWrapper(@NotEmpty String graphDirectory) {
		this.graphDirectory = graphDirectory;
		graphDB = initDB();	
	}
	/**
	 * Default constructor
	 * @return {@link Neo4jGraphDependencyVisitorTask}
	 */
	public Neo4jGraphDBWrapper() {
		File tmpFile = FileUtils.getTempDirectory();
		this.graphDirectory = tmpFile.getAbsolutePath();
		graphDB = initDB();
	}
	
	/**
	 * Initiating the database with default option config.
	 * Using Neo4j default configuration
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
	 * Creating per Label Index on Artifact coordinates per label
	 */
	public void createIndexes() {
		try ( Transaction tx = graphDB.beginTx() )
		{			
			LOGGER.info("Creating per Label Index on Artifact coordinates");
		    Schema schema = graphDB.schema();
		    graphDB.getAllLabels().forEach(label ->  {
		    	try {
		    		schema.indexFor(label)
		    			  .on(Properties.COORDINATES)
			              .create();
				} catch (Exception e) {
					LOGGER.info("Index {} already exists", label);
				}
		    	
		    });		    
		    tx.success();
			LOGGER.info("Index creation finished");
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw e;
		}
	}

	/**
	 * Returns of the Label in the index and creates a new one if not 
	 * @param groupId {@link String}
	 * @return label {@link Label}
	 */
	@NotNull(message = "The return label should not be null")
	private Label getOrCreateLabel(String groupId) {
		
		if (labelsIndex.containsKey(groupId)) 
			return labelsIndex.get(groupId);
		Label label = Label.label(groupId);
		return label;
	}
	/**
	 * Returns {@link Node} given a {@link Artifact}
	 * If the node is not in the database, it is created and returned
	 * @param dependency
	 * @return {@link Node} result
	 */
	@NotNull (message = "The returned node should not be null")
	public Node getNodeFromArtifactCoordinate(@NotNull Artifact artifact) {
		
		String depKey = MavenResolverUtil.artifactToCoordinate(artifact);
		//Artifact artifact = dependency.getArtifact();
		Node result;
		//Label label;
		try ( Transaction tx = graphDB.beginTx()) {
			Label artifactLabel = getOrCreateLabel(artifact.getGroupId());
			result = graphDB.findNode(artifactLabel, Properties.COORDINATES, depKey);
			
			if (result == null) {
			
				LOGGER.debug("adding artifact: "+depKey);
				result = graphDB.createNode(artifactLabel);
				nodesIndex.put(depKey, result);
				
				// setting the artifact metadata properties
				result.setProperty(Properties.COORDINATES, depKey);
				result.setProperty(Properties.GROUP, artifact.getGroupId());
				result.setProperty(Properties.CLASSIFIER, artifact.getClassifier());
				result.setProperty(Properties.VERSION, artifact.getVersion());
				result.setProperty(Properties.PACKAGING, MavenResolverUtil.derivePackaging(artifact).toString());
				result.setProperty(Properties.ARTIFACT, artifact.getArtifactId());
			}
			tx.success();
		}
		return  result;
	}
	/**
	 * Registering the DB shutdown hook
	 */
	public void registerShutdownHook() {
	    Runtime.getRuntime().addShutdownHook( new Thread(() -> {graphDB.shutdown();}));
	}
	/**
	 * Creates An outgoing relationship of type {@link DependencyRelation#DEPENDS_ON} from @param sourceArtifact to @param target artifact  
	 * @param sourceArtifact
	 * @param targetArtifact
	 * @param scope
	 */
 	public void addDependencyToGraphDB( @NotNull Artifact sourceArtifact, @NotNull Artifact targetArtifact, @NotNull Scope scope) {	
 		Node source = getNodeFromArtifactCoordinate(sourceArtifact);
		Node target = getNodeFromArtifactCoordinate(targetArtifact);
		
		try ( Transaction tx = graphDB.beginTx() ) { 
//			String sourceCoord = (String) source.getProperty(Properties.COORDINATES);
//			String targetCoor = (String) target.getProperty(Properties.COORDINATES);
			//If the edge already exists do nothing
			//if (edgesIndex.containsEntry(sourceCoord, targetCoor)) return;
			//Otherwise create one ...
			@NotNull(message = "should always return a valid nonNull relationship")
			Relationship relation = source.createRelationshipTo(target, DependencyRelation.DEPENDS_ON);
			
			relation.setProperty(Properties.SCOPE, scope.toString());

			tx.success();
			//... and update the index
			// edgesIndex.put(sourceCoord, targetCoor);
		}	
	}
 	
	/**
	 * create the precedence relationship between nodes
	 */
	public void createPrecedenceShip() {
		LOGGER.info("Creating plugins version's evolution ");

		try (Transaction tx = graphDB.beginTx()) {
			graphDB.getAllLabelsInUse().forEach(label ->
			{	
				@NotNull(message = "All the existing labels should have at least one node")
				List<Node> sortedNodes = graphDB.findNodes(label).stream().sorted(new Comparator<Node>() {

					@Override
					public int compare(Node n1, Node n2) {
						//String p1 = n1.getProperty
						String p1 = (String) n1.getProperty(Properties.ARTIFACT);
						String p2 = (String) n2.getProperty(Properties.ARTIFACT);
						Version v1 = null;
						Version v2 = null;
						if (p1.compareTo(p2) != 0) return p1.compareTo(p2);
						final GenericVersionScheme versionScheme = new GenericVersionScheme();
						try {
							v1 = versionScheme.parseVersion((String)n1.getProperty(Properties.VERSION));
							v2 = versionScheme.parseVersion((String)n2.getProperty(Properties.VERSION));
						} catch (InvalidVersionSpecificationException e) {
							LOGGER.error(e.getMessage());
							e.printStackTrace();
						}
					    return v1.compareTo(v2); 		
					}
				}).collect(Collectors.toList());//end find nodes
				
				for (int i =0; i< sortedNodes.size() - 2; i++) {
					Node firstNode = sortedNodes.get(i);
					Node secondNode = sortedNodes.get(i+1);
					
					if (isNextRelease(firstNode, secondNode)) {
						firstNode.createRelationshipTo(secondNode, DependencyRelation.NEXT);
					}
				}
			});//end foreach		
			tx.success();
		} catch (Exception e) {
			LOGGER.error(e.getLocalizedMessage());
			e.printStackTrace();
			throw e;
		}
		
	}
	/**
	 * @param firstNode
	 * @return {@link Boolean#TRUE} is  
	 */
	private boolean isNextRelease(Node firstNode, Node secondNode) {
		
		@NotNull String artifact1 = (String) firstNode.getProperty(Properties.ARTIFACT);
		@NotNull String artifact2 = (String) secondNode.getProperty(Properties.ARTIFACT);
		return artifact1.equals(artifact2);
	}
	

	/**
	 * shutDown the graph database
	 */
	public void shutdown() {
		graphDB.shutdown();	
	}
	public void updateDependencyCounts(String coordinates, JarCounter jarCounter) {
		
		String [] elements = MavenResolverUtil.coordinatesToElements(coordinates);
		@NotNull String groupeID = elements[0];
		
		try (Transaction tx = graphDB.beginTx()) {
			Label label = getOrCreateLabel(groupeID);
			Node node = graphDB.findNode(label, Properties.COORDINATES, coordinates);
			
			node.setProperty(Properties.ANNOTATION_COUNT, jarCounter.getAnnotations());
			node.setProperty(Properties.ENUM_COUNT, jarCounter.getEnums());
			node.setProperty(Properties.CLASS_COUNT, jarCounter.getClasses());
			node.setProperty(Properties.INTERFACE_COUNT, jarCounter.getInterfaces());
			node.setProperty(Properties.METHOD_COUNT, jarCounter.getMethods());	
			
			tx.success();
		}
		
	}
	/**
	 * Updating the Number of classes property of a given artifact
	 * @param  coordinates {@link String}
	 * @param classCount {@link Integer}
	 */
	public void updateClassCount(String coordinates, int classCount) {
		String [] elements = MavenResolverUtil.coordinatesToElements(coordinates);
		@NotNull String groupeID = elements[0];
		
		try (Transaction tx = graphDB.beginTx()) {
			Label label = getOrCreateLabel(groupeID);
			Node node = graphDB.findNode(label, Properties.COORDINATES, coordinates);
			node.setProperty(Properties.CLASS_COUNT, classCount);
			tx.success();
		}	
	}
	
	
	/**
	 * An enum type implementing Neo4j's RelationshipTypes 
	 * @author Amine BENELALLAM
 	 *
	 */
	private static enum DependencyRelation implements RelationshipType {
		DEPENDS_ON,
		NEXT;
	}
	
	
	/**
	 * An internal class containing node properties used for persistence
	 * @author Amine BENELALLAM
	 *
	 */
	private class Properties {
		
		private static final String COORDINATES = "coordinates";
		private static final String GROUP = "groupID";
		private static final String VERSION = "version";
		private static final String PACKAGING = "packaging";
		private static final String CLASSIFIER = "classifier";
		private static final String ARTIFACT = "artifact";
		private static final String SCOPE = "scope";
		
		private static final String CLASS_COUNT = "class_count";
		private static final String ENUM_COUNT = "enum_count";
		private static final String INTERFACE_COUNT = "interface_count";
		private static final String ANNOTATION_COUNT = "annotation_count";
		private static final String METHOD_COUNT = "method_count";
		
		
		
	}
	

	
}
