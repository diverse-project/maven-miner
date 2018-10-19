package maven.miner.procedures;

import java.util.stream.Stream;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import fr.inria.diverse.maven.common.Properties;
import maven.miner.output.OutputNode;

public class ReverseDependencyProc extends AbstractProcedureEnv{

	/**
	 * 
	 * @param start
	 * @param end
	 * @param isInclusive
	 * @return
	 */
	@Procedure (value = "maven.miner.dependent.ofRange", mode = Mode.READ)
	@Description ("Retrieving all artifacts depending on an artifact in a particular version range")
	public Stream<OutputNode>  getDependentsInVersionRange ( @Name(value = "group name or artifact and group name in the form G:A") String coordinates,
			 												 @Name(value = "Are interval bound inclusive") Boolean inclusive,
															 @Name(value = "Start version") String start,
															 @Name(value = "End version", defaultValue = "None") String end, 
															 @Name(value = "Transitive dependencies", defaultValue = "false") Boolean transitive) {
		String [] ga = coordinates.split(":");
		StringBuilder builder = new StringBuilder();

		if (ga.length > 2) {
			throw new RuntimeException("Only the group ID and the ArtifactID Should be provided");
		} else if (ga.length < 2) {
			throw new RuntimeException("Missing entry. Both the group ID and the ArtifactID Should be provided");
		}

		builder.append(String.format(" call maven.miner.version.between"
										+ "('%s','%s', '%s', %b) YIELD outputNode", 
										coordinates, 
										start,
										end,
										String.valueOf(inclusive)));
		
		builder.append(String.format(" match (d:Artifact)-[:DEPENDS_ON%s]->(outputNode)",transitive ? "*" : ""));
		builder.append(" RETURN d;");
		
		Stream<OutputNode> result = null;
		try (Transaction tx = graphDB.beginTx()) {
		
			Result queryResult = graphDB.execute(builder.toString());
			result = queryResult.columnAs("d")
								.stream()
								.map(node -> new OutputNode((Node)node));
			tx.success();
		} catch (Throwable e) {
			log.error(e.getMessage());
			throw new RuntimeException(e);	
		}
		return result;
	}

	/**
	 * 
	 * @param start
	 * @param end
	 * @param isInclusive
	 * @return
	 */
	@Procedure (value = "maven.miner.dependent.ofSingle", mode = Mode.READ)
	@Description ("Retrieving all artifacts depending on an artifact in a particular version range")
	public Stream<OutputNode>  getDependentsOfGAV ( @Name(value = "The precise artifact coordinates in the form G:A:V") String coordinates,
												   	@Name(value = "Transitive dependencies", defaultValue = "false") Boolean transitive) {
		String [] ga = coordinates.split(":");
		StringBuilder builder = new StringBuilder();
	
		if (ga.length > 3) {
			throw new RuntimeException("Extra entry. Please provide the artifact coordinats in the form of G:A:V ");
		} else if (ga.length < 3) {
			throw new RuntimeException("Missing entry. Please provide the artifact coordinats in the form of G:A:V ");
		}
	
		builder.append(String.format(" MATCH (n:`%s` {%s : '%s', %s : '%s'}) <-[:DEPENDS_ON%s]-(d)"
										+ " RETURN d;", 
										ga[0], 
										Properties.ARTIFACT,
										ga[1],
										Properties.VERSION,
										ga[2],
										transitive ? "*" : ""));
		
		Stream<OutputNode> result = null;
		try (Transaction tx = graphDB.beginTx()) {
		
			Result queryResult = graphDB.execute(builder.toString());
			result = queryResult.columnAs("d")
								.stream()
								.map(node -> new OutputNode((Node)node));
			tx.success();
		} catch (Throwable e) {
			log.error(e.getMessage());
			throw new RuntimeException(e);	
		}
		return result;
	}
}
