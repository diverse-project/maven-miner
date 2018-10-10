package fr.inria.diverse.maven.resolver.db;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.validation.constraints.NotNull;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.util.layout.MavenDefaultLayout;

import fr.inria.diverse.maven.common.DependencyRelation;
import fr.inria.diverse.maven.common.Properties;
import fr.inria.diverse.maven.model.Edge.Scope;
import fr.inria.diverse.maven.model.ExceptionCounter;
import fr.inria.diverse.maven.model.JarCounter;
import fr.inria.diverse.maven.resolver.Booter;
import fr.inria.diverse.maven.resolver.processor.MultiTaskDependencyVisitor;
import fr.inria.diverse.maven.resolver.util.MavenResolverUtil;

public abstract class Neo4jGraphDBWrapper {
	/**
	 * Me: Obviously a logger... 
	 * Him (N-H): Tnx for the info Bro! You rock!
	 */
	protected static Logger LOGGER = LoggerFactory.getLogger(MultiTaskDependencyVisitor.class);
	/**
	 * An index of already resolved artifacts in the form of Neo4j {@link Node} 
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
	 * A throwable instance for managing failure execptions
	 */
	protected Throwable txEx = null;
	/**
	 * The number of times to retry a transaction in case of failure 
	 */
	protected static final int RETRIES = 5;
	/**
	 * The sleep time in case of failure
	 */
	protected static final int BACKOFF = 3000;
	/**
	 * Returns the release date of a given artifact
	 * @param artifact
	 * @return javaDate {@link ZonedDateTime}
	 * @throws ParseException 
	 */
	protected ZonedDateTime getReleaseDateFromArtifact(@NotNull Artifact artifact) {
		URL artifactURL = null;
		ZonedDateTime javaDate = null;
		try {
			artifactURL = centralURI.resolve(layout.getPath(artifact)).toURL();
	  		HttpURLConnection connexion = (HttpURLConnection) artifactURL.openConnection();
	  		connexion.setRequestMethod("HEAD");
	  		String modified = connexion.getHeaderField("Last-Modified");		  		
	  		javaDate = MavenResolverUtil.toZonedTime(modified);
	      	} catch (MalformedURLException e) {
	      		LOGGER.error("MalformedURL {}",artifactURL);
	      		e.printStackTrace();
	      	} catch (IOException e) {
	      		LOGGER.error("MalformedURL {}",artifactURL);
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

	abstract public void addResolutionExceptionRelationship(Artifact artifact);
	
	/**
	 * 
	 */
	protected void wrapException (Throwable txEx ) throws TransactionFailureException {
		if ( txEx instanceof TransactionFailureException )
		{
		    throw ((TransactionFailureException) txEx);
		}
		else if ( txEx instanceof Error )
		{
		    throw ((Error) txEx);
		}
		else if ( txEx instanceof RuntimeException )
		{
		    throw ((RuntimeException) txEx);
		}
		else
		{
		    throw new TransactionFailureException( "Failed", txEx );
		}
	}
}
