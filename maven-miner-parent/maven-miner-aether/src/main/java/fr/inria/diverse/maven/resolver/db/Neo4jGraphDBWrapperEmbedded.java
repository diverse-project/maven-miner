package fr.inria.diverse.maven.resolver.db;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.kernel.DeadlockDetectedException;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.util.version.GenericVersionScheme;
import org.sonatype.aether.version.InvalidVersionSpecificationException;
import org.sonatype.aether.version.Version;

import fr.inria.diverse.maven.common.DependencyRelation;
import fr.inria.diverse.maven.common.Properties;
import fr.inria.diverse.maven.model.Edge.Scope;
import fr.inria.diverse.maven.model.ExceptionCounter;
import fr.inria.diverse.maven.model.ExceptionCounter.ExceptionType;
import fr.inria.diverse.maven.model.JarCounter;
import fr.inria.diverse.maven.model.JarCounter.JarEntryType;
import fr.inria.diverse.maven.resolver.tasks.Neo4jGraphDependencyVisitorTask;
import fr.inria.diverse.maven.util.MavenMinerUtil;

public class Neo4jGraphDBWrapperEmbedded extends Neo4jGraphDBWrapper {
	/**
	 * The exception label 
	 */
	protected final Label exceptionLabel = Label.label(Properties.EXCEPTION_LABEL);
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
	public Neo4jGraphDBWrapperEmbedded(String graphDirectory) {
		this.graphDirectory = graphDirectory;
		graphDB = initDB();	
	}
	/**
	 * Default constructor
	 * @return {@link Neo4jGraphDependencyVisitorTask}
	 */
	public Neo4jGraphDBWrapperEmbedded() {
		File tmpFile = FileUtils.getTempDirectory();
		this.graphDirectory = tmpFile.getAbsolutePath();
		graphDB = initDB();
	}

	/**
	 * Initiating the database with default option config.
	 * Using Neo4j default configuration
	 * @return {@link GraphDatabaseService}
	 */
	protected GraphDatabaseService initDB() {
		GraphDatabaseService db =new GraphDatabaseFactory()
	    .newEmbeddedDatabaseBuilder(FileUtils.getFile(graphDirectory))
	    .newGraphDatabase();
		try(Transaction tx = db.beginTx()){
			edgesIndex = db.index().forRelationships(DependencyRelation.DEPENDS_ON.name());	
		}
		return db;
	}
	/**
	 * Creating per Label Index on Artifact coordinates per label
	 * TODO change this 
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
	protected Label getOrCreateLabel(String key) {
		
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
	protected Node getNodeFromArtifactCoordinate(Artifact artifact) {
		String depKey = MavenMinerUtil.artifactToCoordinate(artifact);
		
		Node result;
		//Label label;
		for (int i = 0; i < RETRIES; i++) {
			try ( Transaction tx = graphDB.beginTx()) {
				
				Label artifactLabel = getOrCreateLabel(Properties.ARTIFACT_LABEL);		
				result = graphDB.findNode(artifactLabel, Properties.COORDINATES, depKey);
				
				if (result == null) {
				
					LOGGER.debug("adding artifact: "+depKey);
					result = graphDB.createNode(artifactLabel);
					
					// setting the artifact metadata properties
					result.setProperty(Properties.COORDINATES, depKey);
					result.setProperty(Properties.GROUP, artifact.getGroupId());
					result.setProperty(Properties.CLASSIFIER, artifact.getClassifier());
					result.setProperty(Properties.VERSION, artifact.getVersion());
					result.setProperty(Properties.PACKAGING, MavenMinerUtil.derivePackaging(artifact).toString());
					result.setProperty(Properties.ARTIFACT, artifact.getArtifactId());
					
					//resolving last-modified from central
			    	URL artifactURL = null;
					try {
						artifactURL = centralURI.resolve(layout.getPath(artifact)).toURL();
				  		HttpURLConnection connexion = (HttpURLConnection) artifactURL.openConnection();
				  		connexion.setRequestMethod("HEAD");
				  		String modified = connexion.getHeaderField("Last-Modified");		  		
				  		ZonedDateTime javaDate = sdf.parse(modified).toInstant().atZone(ZoneId.systemDefault());
				  		result.setProperty(Properties.LAST_MODIFIED, javaDate);
				      	} catch (MalformedURLException e) {
				      		LOGGER.error("MalformedURL {}",artifactURL);
				      		e.printStackTrace();
				      	} catch (IOException e) {
				      		e.printStackTrace();
				      	} catch (ParseException e) {
				      		LOGGER.error("MalformedURL {}",artifactURL);
							e.printStackTrace();
						}
				} 
				result.addLabel(getOrCreateLabel(artifact.getGroupId()));
				tx.success();
				return  result;
			} catch  (Throwable ex)  {
				 txEx = ex;
			     if (!(ex instanceof DeadlockDetectedException)) {
			         break;
			     }
			     if ( i < RETRIES-1 ) {
				     try {
				            Thread.sleep( BACKOFF );
				     } catch ( InterruptedException e ) {
				            throw new TransactionFailureException( "Trasaction failed due to thread interuption", e );
				     }
			     }
	        }
		}
		// THEN
	    wrapException(txEx);
	    // Never visited
	    return null;
		
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
 	public void addDependency(Artifact sourceArtifact,  Artifact targetArtifact,Scope scope) {	
 		Node source = getNodeFromArtifactCoordinate(sourceArtifact);
		Node target = getNodeFromArtifactCoordinate(targetArtifact);
		for (int i = 0; i < RETRIES; i++) {
			try ( Transaction tx = graphDB.beginTx() ) { 
	
				if(!edgesIndex.get(Properties.SCOPE, scope.toString(), source, target).hasNext()) {
					Relationship relation = source.createRelationshipTo(target, DependencyRelation.DEPENDS_ON);
					relation.setProperty(Properties.SCOPE, scope.toString());
					edgesIndex.add(relation, Properties.SCOPE, scope.toString());
				}
				tx.success();
				return;
			} catch  (Throwable ex)  {
				 txEx = ex;
			     if (!(ex instanceof DeadlockDetectedException)) {
			         break;
			     }
			     if ( i < RETRIES-1 ) {
				     try {
				            Thread.sleep( BACKOFF );
				     } catch ( InterruptedException e ) {
				            throw new TransactionFailureException( "Trasaction failed due to thread interuption", e );
				     }
			     }
	        }
		}
		// THEN
	    wrapException(txEx);
	}
 	
	/**
	 * create the precedence relationship between nodes
	 */
	public void createPrecedenceShip() {
		LOGGER.info("Creating plugins version's evolution ");
		for (int r = 0; r < RETRIES; r++) {
			try (Transaction tx = graphDB.beginTx()) {
				 graphDB.getAllLabelsInUse().stream()
						.filter(label -> ! label.name().equals(Properties.EXCEPTION_LABEL) ||
											! label.name().equals(Properties.EXCEPTION_LABEL))							
						.forEach(label ->
				{							
					
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
				return;
			} catch  (Throwable ex)  {
				 txEx = ex;
			     if (!(ex instanceof DeadlockDetectedException)) {
			         break;
			     }
			     if ( r < RETRIES-1 ) {
				     try {
				            Thread.sleep( BACKOFF );
				     } catch ( InterruptedException e ) {
				            throw new TransactionFailureException( "Trasaction failed due to thread interuption", e );
				     }
			     }
			}
		}
		// THEN
	    wrapException(txEx);
		
	}

	public void shutdown() {
		graphDB.shutdown();	
	}
	/**
	 * updates the jar entries counters of a given artifact {@link Artifact}
	 * @param artifact {@link Artifact}
	 * @param jarCounter {@link JarCounter}
	 */
	public void updateDependencyCounts(Artifact artifact, JarCounter jarCounter) {
		for (int r = 0; r < RETRIES; r++) {
			try (Transaction tx = graphDB.beginTx()) {
				// retrieving the artifact's node
				Node node = getNodeFromArtifactCoordinate(artifact);
				// updating jar entries count
				Arrays.asList(JarEntryType.values())
				      .forEach(type -> node.setProperty(type.getName(), jarCounter.getValueForType(type)));
				
				tx.success();
				return;
			} catch  (Throwable ex)  {
				 txEx = ex;
			     if (!(ex instanceof DeadlockDetectedException)) {
			         break;
			     }
			     if ( r < RETRIES-1 ) {
				     try {
				            Thread.sleep( BACKOFF );
				     } catch ( InterruptedException e ) {
				            throw new TransactionFailureException( "Trasaction failed due to thread interuption", e );
				     }
			     }
			}
		}
		// THEN
	    wrapException(txEx);	
	}
	/**
	 * 
	 * @param coordinates
	 * @param jarCounter
	 */
	public void updateDependencyCounts(Artifact artifact, JarCounter jarCounter, ExceptionCounter exCounter) {
		for (int r = 0; r < RETRIES; r++) {
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
				return;
			} catch  (Throwable ex)  {
				 txEx = ex;
			     if (!(ex instanceof DeadlockDetectedException)) {
			         break;
			     }
			     if ( r < RETRIES-1 ) {
				     try {
				            Thread.sleep( BACKOFF );
				     } catch ( InterruptedException e ) {
				            throw new TransactionFailureException( "Trasaction failed due to thread interuption", e );
				     }
			     }
			}
		}
		// THEN
		wrapException(txEx);
	
	}
	/**
	 * Creates Exception Relationship of type {@link DependencyRelation#RAISES} 
	 * @param node
	 * @param type
	 * @param value
	 */
	protected void createExceptionRelationship(Node node, ExceptionType type, int value) {		
		Node exceptionNode = getOrcreateExceptionNode(type);
		Relationship rel= node.createRelationshipTo(exceptionNode, DependencyRelation.RAISES);
		rel.setProperty(Properties.EXCEPTION_OCCURENCE, value);
	}
	
	protected Node getOrcreateExceptionNode(ExceptionType type) {
		
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
		for (int r = 0; r < RETRIES; r++) {
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
				return;
			
			} catch  (Throwable ex)  {
				 txEx = ex;
			     if (!(ex instanceof DeadlockDetectedException)) {
			         break;
			     }
			     if ( r < RETRIES-1 ) {
				     try {
				            Thread.sleep( BACKOFF );
				     } catch ( InterruptedException e ) {
				            throw new TransactionFailureException( "Trasaction failed due to thread interuption", e );
				     }
			     }
			}
		}
		// THEN
		wrapException(txEx);
	}
	/**
	 * Creating a resolution relationship
	 * @param artifact
	 */
	public void addResolutionExceptionRelationship(Artifact artifact) {
		try (Transaction tx =  graphDB.beginTx()) {
		Node node = getNodeFromArtifactCoordinate(artifact);
		Node resolutionNode = getOrcreateExceptionNode(ExceptionType.RESOLUTION);
		node.getRelationships(DependencyRelation.DEPENDS_ON, Direction.OUTGOING);
		node.createRelationshipTo(resolutionNode, DependencyRelation.RAISES);
		tx.success();}
	}
	@Override
	public void createNodeFromArtifactCoordinate( Artifact artifact) {
		getNodeFromArtifactCoordinate(artifact);	
	}
}
