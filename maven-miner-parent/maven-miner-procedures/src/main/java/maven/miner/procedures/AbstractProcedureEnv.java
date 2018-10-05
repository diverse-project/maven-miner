package maven.miner.procedures;

import java.util.stream.Stream;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;

import fr.inria.diverse.maven.common.Properties;

public class AbstractProcedureEnv {

	/**
	 * The contextual graph database
	 */
	@Context 
	 public GraphDatabaseService graphDB;
	/**
	 * Neo4j Logger
	 */
	@Context 
	public Log log;
	/**
	 * The calendar Label
	 */
	protected static final Label calendarLabel = Label.label(Properties.CALENDAR_LABEL);
	
	/**
	 * The Artifact Label
	 */
	protected static final Label artifactLabel = Label.label(Properties.ARTIFACT_LABEL);
	/**
	 * 
	 * @return {@link Stream<String>} allLabels
	 */
	protected Stream<String> allLabelsInUse() {
		Stream<String> allLabels = null;
		log.info("Retrieving all labels in use");
		try (Transaction tx = graphDB.beginTx()) {
			 allLabels = graphDB.getAllLabelsInUse().stream()
					.filter( label -> ! label.name().equals(Properties.EXCEPTION_LABEL) &&
									  ! label.name().equals(Properties.ARTIFACT_LABEL)  &&
									  ! label.name().equals(Properties.CALENDAR_LABEL))	
					.map(Label::toString);
			tx.success();
			
		} catch (Exception e ) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
		return allLabels;
	}
}
