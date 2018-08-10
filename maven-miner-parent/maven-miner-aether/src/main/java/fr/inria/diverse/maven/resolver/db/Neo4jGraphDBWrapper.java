package fr.inria.diverse.maven.resolver.db;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.graphdb.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.version.InvalidVersionSpecificationException;

import fr.inria.diverse.maven.resolver.model.Edge.Scope;
import fr.inria.diverse.maven.resolver.model.ExceptionCounter;
import fr.inria.diverse.maven.resolver.model.ExceptionCounter.ExceptionType;
import fr.inria.diverse.maven.resolver.processor.MultiTaskDependencyVisitor;
import fr.inria.diverse.maven.resolver.model.JarCounter;
import fr.inria.diverse.maven.resolver.model.JarCounter.JarEntryType;
import fr.inria.diverse.maven.resolver.tasks.Neo4jGraphDependencyVisitorTask;
import fr.inria.diverse.maven.resolver.util.MavenResolverUtil;

import java.util.Arrays;
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
	 * TODO replace with neo4j indexes
	 */
	protected Map<String,Node> nodesIndex = new HashMap<String,Node>();
	/**
	 * An index of already resolved dependency relationships
	 * TODO replace with Neo4j Relationship index -
	 * 
	 */
	protected RelationshipIndex edgesIndex;
	//protected ListMultimap<String, String> edgesIndex = ArrayListMultimap.create();
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
	 * Exception nodes index
	 */
	protected final Label exceptionLabel = Label.label(Properties.EXCEPTION_LABEL);
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
		try(Transaction tx = db.beginTx()){
			edgesIndex = db.index().forRelationships(DependencyRelation.DEPENDS_ON.name());	
		}
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
	 * @param key {@link String}
	 * @return label {@link Label}
	 */
	@NotNull(message = "The return label should not be null")
	private Label getOrCreateLabel(String key) {
		
		if (labelsIndex.containsKey(key)) 
			return labelsIndex.get(key);
		Label label = Label.label(key);
		//add constraints on label 
		graphDB.schema().constraintFor(label)
						.assertPropertyIsUnique(Properties.COORDINATES);
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
	 * Creates An outgoing relationship of type {@link DependencyRelation#DEPENDS_ON} from @param sourceArtifact to @param targetArtifact  
	 * @param sourceArtifact {@link Artifact}
	 * @param targetArtifact {@link Artifact}
	 * @param scope {@link Scope}
	 */
 	public void addDependencyToGraphDB( @NotNull Artifact sourceArtifact, @NotNull Artifact targetArtifact, @NotNull Scope scope) {	
 		Node source = getNodeFromArtifactCoordinate(sourceArtifact);
		Node target = getNodeFromArtifactCoordinate(targetArtifact);
		
		try ( Transaction tx = graphDB.beginTx() ) { 

			if(!edgesIndex.get(Properties.SCOPE, scope.toString(), source, target).hasNext()) {
				@NotNull(message = "should always return a valid nonNull relationship")
				Relationship relation = source.createRelationshipTo(target, DependencyRelation.DEPENDS_ON);
				relation.setProperty(Properties.SCOPE, scope.toString());
				edgesIndex.add(relation, Properties.SCOPE, scope.toString());
			}
			tx.success();

		}	
	}
 	
	/**
	 * create the precedence relationship between nodes
	 */
	public void createPrecedenceShip() {
		LOGGER.info("Creating plugins version's evolution ");

		try (Transaction tx = graphDB.beginTx()) {
			 graphDB.getAllLabelsInUse().stream()
					.filter(label -> ! label.name().equals(Properties.EXCEPTION_LABEL))							
					.forEach(label ->
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
	/**
	 * updates the jar entries counters of a given artifact {@link Artifact}
	 * @param artifact {@link Artifact}
	 * @param jarCounter {@link JarCounter}
	 */
	public void updateDependencyCounts(Artifact artifact, JarCounter jarCounter) {
		
		try (Transaction tx = graphDB.beginTx()) {
			// retrieving the artifact's node
			Node node = getNodeFromArtifactCoordinate(artifact);
			// updating jar entries count
			Arrays.asList(JarEntryType.values())
			      .forEach(type -> node.setProperty(type.getName(), jarCounter.getValueForType(type)));
			
			tx.success();
		}
		
	}
	/**
	 * 
	 * @param coordinates
	 * @param jarCounter
	 */
	public void updateDependencyCounts(Artifact artifact, JarCounter jarCounter, ExceptionCounter exCounter) {
		
		
		try (Transaction tx = graphDB.beginTx()) {
			// retrieving the artifact's node
			Node node = getNodeFromArtifactCoordinate(artifact);
			// updating jar entries count
			Arrays.asList(JarEntryType.values())
				  .forEach(type -> node.setProperty(type.getName(), jarCounter.getValueForType(type)));
			
			//Updating exceptions count
			Arrays.asList(ExceptionType.values())
				  .forEach(type -> {
					  int value = exCounter.getValueForType(type); 
					  if (value != 0) {
						 createExceptionRelationship(node, type, value);
					  }
				  });
			tx.success();
		}
		
	}
	/**
	 * Creates Exception Relationship of type {@link DependencyRelation#RAISES} 
	 * @param node
	 * @param type
	 * @param value
	 */
	private void createExceptionRelationship(Node node, ExceptionType type, int value) {		
		Node exceptionNode = getOrcreateExceptionNode(type);
		Relationship rel= node.createRelationshipTo(exceptionNode, DependencyRelation.RAISES);
		rel.setProperty(Properties.EXCEPTION_OCCURENCE, value);
	}
	
	private Node getOrcreateExceptionNode(ExceptionType type) {
		
		Node result = graphDB.findNode(exceptionLabel, Properties.EXCEPTION_NAME, type.getName());
		if (result == null) {
			result = graphDB.createNode(exceptionLabel);
			result.setProperty(Properties.EXCEPTION_NAME, type.getName());
		}
		return result;
		
	}
	/**
	 * Updating the Number of classes property of a given artifact
	 * @param  coordinates {@link String}
	 * @param classCount {@link Integer}
	 */
	public void updateClassCount(Artifact artifact, int classCount) {
		
		//String [] elements = MavenResolverUtil.coordinatesToElements(coordinates);
		try (Transaction tx = graphDB.beginTx()) {
			//Label label = getOrCreateLabel(groupeID);
			Node node = getNodeFromArtifactCoordinate(artifact);
			if (node == null) {
				// Sth weird is happening here!
				// This shouldn't occur!!Mhhh
				tx.close();
				return;
			}
			node.setProperty(JarEntryType.CLASS.getName(), classCount);
			tx.success();
		}	
	}
	
	public void addResolutionExceptionRelationship(Artifact artifact) {
		try (Transaction tx =  graphDB.beginTx()) {
		Node node = getNodeFromArtifactCoordinate(artifact);
		Node resolutionNode = getOrcreateExceptionNode(ExceptionType.RESOLUTION);
		node.getRelationships(DependencyRelation.DEPENDS_ON, Direction.OUTGOING);
		node.createRelationshipTo(resolutionNode, DependencyRelation.RAISES);
		tx.success();}
	}

	/**
	 * An enum type implementing Neo4j's RelationshipTypes 
	 * @author Amine BENELALLAM
 	 *
	 */
	private static enum DependencyRelation implements RelationshipType {
		DEPENDS_ON,
		NEXT,
		RAISES;
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
		
		private static final String EXCEPTION_LABEL = "exception";
		private static final String EXCEPTION_OCCURENCE = "occurence";
		private static final String EXCEPTION_NAME = "name";

	}
	
}
