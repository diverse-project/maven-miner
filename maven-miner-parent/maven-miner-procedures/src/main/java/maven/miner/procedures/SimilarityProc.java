package maven.miner.procedures;

import java.util.Map;
import java.util.stream.Stream;

import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;


import maven.miner.output.MapResult;

public class SimilarityProc extends AbstractProcedureEnv {

	/**
	 * This procedure returns all latest artifacts
	 */
	@Procedure(value="maven.miner.artifacts.dependencySwap", mode = Mode.READ)
	@Description("retrieve all latest artifacts") 
	public Stream<MapResult> dependecySwap () {
		
		Stream<MapResult> result = null;
		
		log.info("Retrieving all  plugins version's evolution ");
		result = allLabelsInUse().parallel()
						.flatMap(this::dependecySwapGroup);		
		
		return result;
	}
	/**
	 * 
	 * This procedure returns all latest artifacts
	 */
	@Procedure(value="maven.miner.artifacts.group.dependencySwap", mode = Mode.READ)
	@Description("retrieve all latest artifacts") 
	public Stream<MapResult> dependecySwapGroup (
						@Name(value = "The group name")String labelName) {
		Stream<MapResult> result = null;
		try (Transaction tx = graphDB.beginTx()) {
			   String query = String.format("match (d2:Artifact)<-[:DEPENDS_ON]-(v2:`%s`)"
										 	+ "<-[:NEXT]-(v1:`%s`)-[:DEPENDS_ON]->(d1:Artifact) \n" 
											+ "with v1, v2, d1, d2, v1.coordinates as oldVersion, v2.coordinates as newVersion\n" 
											+ "return v1 { oldVersion, \n" 
											+ "		    newVersion,  \n" 
											+ "            oldDependencies : apoc.coll.subtract(collect(d1.coordinates),collect(d2.coordinates)), \n"  
											+ "            newDependecies : apoc.coll.subtract(collect(d2.coordinates),collect(d1.coordinates))\n" 
											+ "          }",
											labelName,
											labelName
											);

			  Result queryResult = graphDB.execute(query);
			  result = queryResult.columnAs("v1")
								.map(Map.class::cast)
								.stream()
								.parallel()
								.map(MapResult::new);								
			  tx.success();
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new RuntimeException(e);	
		}
		//result.forEach(MapResult::computeSimilarity);
		return result;
	}
}
