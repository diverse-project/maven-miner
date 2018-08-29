package fr.inria.diverse.maven.resolver.db;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.artifact.Artifact;
import fr.inria.diverse.maven.resolver.Booter;
import fr.inria.diverse.maven.resolver.model.Edge.Scope;
import fr.inria.diverse.maven.resolver.model.ExceptionCounter;
import fr.inria.diverse.maven.resolver.processor.MultiTaskDependencyVisitor;
import fr.inria.diverse.maven.resolver.model.JarCounter;
import javax.validation.constraints.NotNull;

import org.sonatype.aether.util.layout.MavenDefaultLayout;

public abstract class Neo4jGraphDBWrapper {
	/**
	 * Me: Obviously a logger... 
	 * Him (N-H): Tnx for the Info Bro! You rock!
	 */
	protected static Logger LOGGER = LoggerFactory.getLogger(MultiTaskDependencyVisitor.class);
	/**
	 * An index of already resolved artifacts in the form of Neo4j {@link Node} 
	 * TODO replace with neo4j indexes
	 */

	protected RelationshipIndex edgesIndex;
	//protected ListMultimap<String, String> edgesIndex = ArrayListMultimap.create();
	/**
	 * A String to Label map to cache already created labels
	 */
	protected Map<String,Label> labelsIndex = new HashMap<String,Label>();

	/**
	 * Maven defaul layout used to construct artifact URLs
	 */
	protected MavenDefaultLayout layout = new MavenDefaultLayout();
	/**
	 * Central URI url
	 */
	protected URI centralURI = URI.create(Booter.newCentralRepository().getUrl());
	/**
	 * 
	 */
	protected static final SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss z");
	
	static {
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	/**
	 * Returns the release date of a given artifact
	 * @param artifact
	 * @return javaDate {@link ZonedDateTime}
	 */
	protected ZonedDateTime getReleaseDateFromArtifact(@NotNull Artifact artifact) {
		URL artifactURL = null;
		ZonedDateTime javaDate = null;
		try {
			artifactURL = centralURI.resolve(layout.getPath(artifact)).toURL();
	  		HttpURLConnection connexion = (HttpURLConnection) artifactURL.openConnection();
	  		connexion.setRequestMethod("HEAD");
	  		String modified = connexion.getHeaderField("Last-Modified");		  		
	  		javaDate = sdf.parse(modified).toInstant().atZone(ZoneId.systemDefault());
	      	} catch (MalformedURLException e) {
	      		LOGGER.error("MalformedURL {}",artifactURL);
	      		e.printStackTrace();
	      	} catch (IOException e) {
	      		e.printStackTrace();
	      	} catch (ParseException e) {
	      		LOGGER.error("MalformedURL {}",artifactURL);
				e.printStackTrace();
			}
		return javaDate;
	}
	
	/**
	 * Creating per Label Index on Artifact coordinates 
	 * 
	 */
	abstract public void createIndexes();


	/**
	 * Returns {@link Node} given a {@link Artifact}
	 * If the node is not in the database, it is created and returned
	 * @param dependency
	 * @return {@link Node} result
	 */
	abstract public void createNodeFromArtifactCoordinate(@NotNull Artifact artifact);
	
	/**
	 * Creates An outgoing relationship of type {@link DependencyRelation#DEPENDS_ON} from @param sourceArtifact to @param targetArtifact  
	 * @param sourceArtifact {@link Artifact}
	 * @param targetArtifact {@link Artifact}
	 * @param scope {@link Scope}
	 */
 	abstract public void addDependency(Artifact sourceArtifact, Artifact targetArtifact, Scope scope);
 	
	/**
	 * create the precedence relationship between nodes
	 */
	abstract public void createPrecedenceShip();
	/**
	 * @param firstNode
	 * @return {@link Boolean#TRUE} is  
	 */
	protected boolean isNextRelease(Node firstNode, Node secondNode) {
		
		@NotNull String artifact1 = (String) firstNode.getProperty(Properties.ARTIFACT);
		@NotNull String artifact2 = (String) secondNode.getProperty(Properties.ARTIFACT);
		return artifact1.equals(artifact2);
	}
	

	/**
	 * shutDown the graph database
	 */
	abstract public void shutdown();
	/**
	 * updates the jar entries counters of a given artifact {@link Artifact}
	 * @param artifact {@link Artifact}
	 * @param jarCounter {@link JarCounter}
	 */
	abstract public void updateDependencyCounts(Artifact artifact, JarCounter jarCounter);
	/**
	 * 
	 * @param coordinates
	 * @param jarCounter
	 */
	abstract public void updateDependencyCounts(Artifact artifact, JarCounter jarCounter, ExceptionCounter exCounter);

	/**
	 * Registers the shutdownhook
	 */
	abstract public void registerShutdownHook();
	/**
	 * An enum type implementing Neo4j's RelationshipTypes 
	 * @author Amine BENELALLAM
 	 *
	 */
	protected static enum DependencyRelation implements RelationshipType {
		DEPENDS_ON,
		NEXT,
		RAISES;
	}
	
	
	/**
	 * An internal class containing node properties used for persistence
	 * @author Amine BENELALLAM
	 *
	 */
	protected class Properties {
				
		 static final String LAST_MODIFIED = "release_date";
		 static final String COORDINATES = "coordinates";
		 static final String GROUP = "groupID";
		 static final String VERSION = "version";
		 static final String PACKAGING = "packaging";
		 static final String CLASSIFIER = "classifier";
		 static final String ARTIFACT = "artifact";
		 static final String SCOPE = "scope";
		
		 static final String EXCEPTION_LABEL = "Exception";
		 static final String EXCEPTION_OCCURENCE = "occurence";
		 static final String EXCEPTION_NAME = "name";
		 static final String ARTIFACT_LABEL = "Artifact";

	}

	abstract public void addResolutionExceptionRelationship(Artifact artifact);


}
