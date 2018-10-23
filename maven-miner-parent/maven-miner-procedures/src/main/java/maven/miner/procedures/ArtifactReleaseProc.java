package maven.miner.procedures;

import java.util.stream.Stream;

import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import maven.miner.output.OutputNumber;
import maven.miner.output.ReleaseDurationOutput;

public class ArtifactReleaseProc extends AbstractProcedureEnv {

	/**
	 * 
	 * @param coordinates
	 * @return
	 */
	@Procedure(value="maven.miner.time.release.average")
	@Description ("The average duration between releases ")
	public Stream<OutputNumber> getAverageDuration (@Name(value = "group:artifact") String coordinates) {
		String [] ga = coordinates.split(":");
		if (ga.length > 2) {
			throw new RuntimeException("Only the group ID and the ArtifactID Should be provided");
		} else if (ga.length < 2) {
			throw new RuntimeException("Missing entry. Both the group ID and the ArtifactID Should be provided");
		}
		StringBuilder query = new StringBuilder("");
		query.append(String.format("match p=(n:`%s`{ artifact : '%s' })-[:NEXT]->(m) ",
									ga[0],
									ga[1]));
		query.append("with maven.miner.duration.between(n.release_date,m.release_date) as durations ");
		query.append("return avg(durations) as average");
		Stream<OutputNumber> result = null;
		
		try (Transaction tx = graphDB.beginTx()) {
			
			result = graphDB.execute(query.toString())
														  .columnAs("average")
														  .stream()
														  .map(Number.class::cast)
														  .map(OutputNumber::new);					
			tx.success();
		} catch (Throwable e) {
			log.error(e.getMessage());
			throw new RuntimeException(e);	
		}
		
		return result;
	}
	/**
	 * 
	 * @param coordinates
	 * @return
	 */
	@Procedure(value="maven.miner.time.release.max")
	@Description ("The maximum duration between releases ")
	public Stream<OutputNumber> getMaxDuration (@Name(value = "group:artifact") String coordinates) {
		String [] ga = coordinates.split(":");
		if (ga.length > 2) {
			throw new RuntimeException("Only the group ID and the ArtifactID Should be provided");
		} else if (ga.length < 2) {
			throw new RuntimeException("Missing entry. Both the group ID and the ArtifactID Should be provided");
		}
		StringBuilder query = new StringBuilder("");
		query.append(String.format("match p=(n:`%s`{ artifact : '%s' })-[:NEXT]->(m) ",
									ga[0],
									ga[1]));
		query.append("with maven.miner.duration.between(n.release_date,m.release_date) as durations ");
		query.append("return max(durations) as maximum");
		Stream<OutputNumber> result = null;
		
		try (Transaction tx = graphDB.beginTx()) {
			
			result = graphDB.execute(query.toString())
										  .columnAs("maximum")
										  .stream()
										  .map(Number.class::cast)
										  .map(OutputNumber::new);					
			tx.success();
		} catch (Throwable e) {
			log.error(e.getMessage());
			throw new RuntimeException(e);	
		}
		
		return result;
	}
	/**
	 * 
	 * @param coordinates
	 * @return
	 */
	@Procedure(value="maven.miner.time.release.min")
	@Description ("The minimum duration between releases ")
	public Stream<OutputNumber> getMinDuration (@Name(value = "group:artifact") String coordinates) {
		String [] ga = coordinates.split(":");
		if (ga.length > 2) {
			throw new RuntimeException("Only the group ID and the ArtifactID Should be provided");
		} else if (ga.length < 2) {
			throw new RuntimeException("Missing entry. Both the group ID and the ArtifactID Should be provided");
		}
		StringBuilder query = new StringBuilder("");
		query.append(String.format("match p=(n:`%s`{ artifact : '%s' })-[:NEXT]->(m) ",
									ga[0],
									ga[1]));
		query.append("with maven.miner.duration.between(n.release_date,m.release_date) as durations ");
		query.append("return min(durations) as minimum");
		Stream<OutputNumber> result = null;
		
		try (Transaction tx = graphDB.beginTx()) {
			
			result = graphDB.execute(query.toString())
							.columnAs("minimum")
							.stream()
							.map(Number.class::cast)
							.map(OutputNumber::new);					
			tx.success();
		} catch (Throwable e) {
			log.error(e.getMessage());
			throw new RuntimeException(e);	
		}
		
		return result;
	}
	/**
	 * 
	 * @param coordinates
	 * @return
	 */
	@Procedure(value="maven.miner.time.release.median")
	@Description ("The median duration between releases ")
	public Stream<OutputNumber> getMeanDuration (@Name(value = "group:artifact") String coordinates) {
		
		String [] ga = coordinates.split(":");
		if (ga.length > 2) {
			throw new RuntimeException("Only the group ID and the ArtifactID Should be provided");
		} else if (ga.length < 2) {
			throw new RuntimeException("Missing entry. Both the group ID and the ArtifactID Should be provided");
		}
		
		StringBuilder query = new StringBuilder("");
		query.append(String.format("match p=(n:`%s`{ artifact : '%s' })-[:NEXT]->(m) ",
									ga[0],
									ga[1]));
		query.append("with maven.miner.duration.between(n.release_date,m.release_date) as duration ");
		query.append("return apoc.agg.median(duration) as median");
		
		Stream<OutputNumber> result = null;
		try (Transaction tx = graphDB.beginTx()) {
			
			result = graphDB.execute(query.toString())
							.columnAs("median")
							.stream()
							.map(Number.class::cast)
							.map(OutputNumber::new);															
			tx.success();
			
		} catch (Throwable e) {
			log.error(e.getMessage());
			throw new RuntimeException(e);	
		}
		return result;
	}
	/**
	 * 
	 * @param coordinates
	 * @return
	 */
	@Procedure(value="maven.miner.time.release.all")
	@Description ("The all duration between releases ")
	public Stream<ReleaseDurationOutput> getAllDurations (@Name(value = "group:artifact") String coordinates) {
		
		String [] ga = coordinates.split(":");
		if (ga.length > 2) {
			throw new RuntimeException("Only the group ID and the ArtifactID Should be provided");
		} else if (ga.length < 2) {
			throw new RuntimeException("Missing entry. Both the group ID and the ArtifactID Should be provided");
		}
		
		StringBuilder query = new StringBuilder("");
		query.append(String.format("match p=(n:`%s`{ artifact : '%s' })-[:NEXT]->(m) ",
									ga[0],
									ga[1]));
		query.append(" with maven.miner.duration.between(n.release_date, m.release_date) as duration, n, m");
		query.append(" return n.coordinates as sourceCoordinates, m.coordinates as targetCoordinates, duration");
		
		Stream<ReleaseDurationOutput> result = null;
		
		try (Transaction tx = graphDB.beginTx()) {
			
			result = graphDB.execute(query.toString())
							.stream()
							.map(ReleaseDurationOutput::new);															
			tx.success();
			
		} catch (Throwable e) {
			log.error(e.getMessage());
			throw new RuntimeException(e);	
		}
		return result;
	}
	
}
