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

public class ArtifactsRankingProc extends AbstractProcedureEnv {
	
	@Procedure(value="maven.miner.artifacts.top", mode = Mode.READ)
	@Description("top X most used artifacts")
	public Stream<OutputNode> getTopArtifacts(
			@Name(value = "the number to limit the top too")long top,
			@Name(value = "dependency scope. none by default", defaultValue = "any") String scope) {
		return getTopArtifacts(Properties.ARTIFACT_LABEL, top, scope);
	}
	
	
	@Procedure(value="maven.miner.artifacts.group.top", mode = Mode.READ)
	@Description("top X most used artifacts of a group")
	public Stream<OutputNode> getTopArtifacts(
			@Name("the groupName")String artifactLabel, 
			@Name(value = "the number to limit the top too")long top,
			@Name(value = "dependency scope. none by default", defaultValue = "any") String scope) {

		Stream<OutputNode> result = null;
		String scopeS = "";
		if (scope.equals("Runtime")
				|| scope.equals("Compile")
				|| scope.equals("Test")
				|| scope.equals("Provided")) {
			scopeS = String.format("{scope : '%s' }",scope);
		} else if (!scope.equalsIgnoreCase("any")) {
			throw new RuntimeException("Supported scopes are: Runtime, Compile, Provided, "
										+ "and Test. To refer  to all scopes use 'any'!");
		}
		try (Transaction tx = graphDB.beginTx()) {
			String query = String.format("match (n:`%s`) " + 
										 "with n, size(()-[:DEPENDS_ON %s]->(n)) as sizes " + 
										 "return n order by sizes DESC limit %d",
										 artifactLabel,
										 scopeS,
										 top
										 );
			Result queryResult = graphDB.execute(query);
			result = queryResult.columnAs("n")
								.stream()
								.map(node -> new OutputNode((Node)node));
			tx.success();
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new RuntimeException(e.getCause());	
		}
		return result;
	}

}
