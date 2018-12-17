package maven.miner.procedures;

import java.util.stream.Stream;

import org.neo4j.driver.v1.types.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import maven.miner.output.OutputNumber;
import maven.miner.output.TimeToReleaseOutput;

public class ArtifactReleaseProc extends AbstractProcedureEnv {

	/**
	 * 
	 * @param coordinates
	 * @return
	 */
	@Procedure(value="maven.miner.upgrade.group.average")
	@Description ("maven.miner.time.upgrade.average('group:artifact')"
			+ " - The average upgrade time between two successive versions of a given library ('group:artifact') in DAYS ")
	public Stream<OutputNumber> getAverageDuration (@Name(value = "group:artifact") String coordinates) {
		String [] ga = coordinates.split(":");
		if (ga.length > 2) {
			throw new RuntimeException("Only the group ID and the ArtifactID Should be provided");
		} else if (ga.length < 2) {
			throw new RuntimeException("Missing entry. Both the group ID and the ArtifactID Should be provided");
		}
		StringBuilder query = new StringBuilder("");
		query.append(String.format("call maven.miner.upgrade.group('%s') YIELD durations  ",
				coordinates));
		query.append("UNWIND durations as duration ");
		query.append("return avg(duration) as average");
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
	@Procedure(value="maven.miner.upgrade.group.max")
	@Description ("maven.miner.time.upgrade.average('group:artifact')"
			+ " - The maximum upgrade time between two successive versions of a given library ('group:artifact') in DAYS ")
	public Stream<OutputNumber> getMaxDuration (@Name(value = "group:artifact") String coordinates) {
		String [] ga = coordinates.split(":");
		if (ga.length > 2) {
			throw new RuntimeException("Only the group ID and the ArtifactID Should be provided");
		} else if (ga.length < 2) {
			throw new RuntimeException("Missing entry. Both the group ID and the ArtifactID Should be provided");
		}
		
		StringBuilder query = new StringBuilder("");
		query.append(String.format("call maven.miner.upgrade.group('%s') YIELD durations  ",
				coordinates));
		query.append("UNWIND durations as duration ");
		query.append("return max(duration) as maximum");
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
	@Procedure(value="maven.miner.upgrade.group.min")
	@Description ("maven.miner.time.upgrade.min('group:artifact')"
			+ " - The minimum upgrade time between two successive versions of a given library ('group:artifact') in DAYS ")
	
	public Stream<OutputNumber> getMinDuration (@Name(value = "group:artifact") String coordinates) {
		String [] ga = coordinates.split(":");
		if (ga.length > 2) {
			throw new RuntimeException("Only the group ID and the ArtifactID Should be provided");
		} else if (ga.length < 2) {
			throw new RuntimeException("Missing entry. Both the group ID and the ArtifactID Should be provided");
		}
		StringBuilder query = new StringBuilder("");
		query.append(String.format("call maven.miner.upgrade.group('%s') YIELD durations  ",
				coordinates));
		query.append("UNWIND durations as duration ");
		query.append("return min(duration) as minimum");
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
	@Procedure(value="maven.miner.upgrade.group.median")
	@Description ("maven.miner.time.upgrade.median('group:artifact')"
			+ " - The median upgrade time between two successive versions of a given library ('group:artifact') in DAYS ")
	
	public Stream<OutputNumber> getMeanDuration (@Name(value = "group:artifact") String coordinates) {
		
		String [] ga = coordinates.split(":");
		if (ga.length > 2) {
			throw new RuntimeException("Only the group ID and the ArtifactID Should be provided");
		} else if (ga.length < 2) {
			throw new RuntimeException("Missing entry. Both the group ID and the ArtifactID Should be provided");
		}
		
		StringBuilder query = new StringBuilder("");
		query.append(String.format("call maven.miner.upgrade.group('%s') YIELD durations  ",
									coordinates));
		query.append("UNWIND durations as duration ");
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
	@Procedure(value="maven.miner.upgrade.group")
	@Description ("maven.miner.upgrade.group ('g:a')- All th duration between upgrades for a given library")
	public Stream<TimeToReleaseOutput> getUpgrades (@Name(value = "group:artifact") String coordinates) {
		
		String [] ga = coordinates.split(":");
		if (ga.length > 2) {
			throw new RuntimeException("Only the group ID and the ArtifactID Should be provided");
		} else if (ga.length < 2) {
			throw new RuntimeException("Missing entry. Both the group ID and the ArtifactID Should be provided");
		}
		
		StringBuilder query = new StringBuilder("");
		
		query.append(String.format(" match (n:`%s`{ artifact : '%s' }) where not (n)<-[:NEXT]-() ",
									ga[0],
									ga[1]));
		query.append(" with n match (n)-[:NEXT*]->(m) with n.release_date as baseDate, n, m ");
		query.append(" return collect(maven.miner.duration.between(baseDate, m.release_date)) "
				+ "as timeToRelease, n.groupID as groupId, n.artifact as artifactId ");
		
		Stream<TimeToReleaseOutput> result = null;
		
		try (Transaction tx = graphDB.beginTx()) {
			
			result = graphDB.execute(query.toString())
							.stream()
							.map(TimeToReleaseOutput::new);															
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
	@Procedure(value="maven.miner.ttr.group")
	@Description ("maven.miner.ttr.group ('g:a')- return the time to of each artifact version given a 'g:a' ")
	public Stream<TimeToReleaseOutput> getTimeToRelease (@Name(value = "group:artifact") String coordinates) {
		
		String [] ga = coordinates.split(":");
		if (ga.length > 2) {
			throw new RuntimeException("Only the group ID and the ArtifactID Should be provided");
		} else if (ga.length < 2) {
			throw new RuntimeException("Missing entry. Both the group ID and the ArtifactID Should be provided");
		}
		
		StringBuilder query = new StringBuilder("");
		
		query.append(String.format(" match (n:`%s`{ artifact : '%s' }) where not (n)<-[:NEXT]-() ",
									ga[0],
									ga[1]));
		query.append(" with n match (n)-[:NEXT*]->(m) with n.release_date as baseDate, n, m ");
		query.append(" return collect(maven.miner.duration.between(baseDate, m.release_date)) "
				+ "as timeToRelease, n.groupID as groupId, n.artifact as artifactId ");
		
		Stream<TimeToReleaseOutput> result = null;
		
		try (Transaction tx = graphDB.beginTx()) {
			
			result = graphDB.execute(query.toString())
							.stream()
							.map(TimeToReleaseOutput::new);															
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
	@Procedure(value="maven.miner.upgrade.group.count")
	@Description ("maven.miner.upgrade.group.count ('g:a')- The number of upgrapdes per library")
	public Stream<OutputNumber> getUpgradesCount (@Name(value = "group:artifact") String coordinates) {
		
		String [] ga = coordinates.split(":");
		if (ga.length > 2) {
			throw new RuntimeException("Only the group ID and the ArtifactID Should be provided");
		} else if (ga.length < 2) {
			throw new RuntimeException("Missing entry. Both the group ID and the ArtifactID Should be provided");
		}
		
		StringBuilder query = new StringBuilder("");
		query.append(String.format("call maven.miner.upgrade.group('%s') YIELD durations  ",
				coordinates));
		query.append("UNWIND durations as duration ");
		query.append("return count(duration) as counts");
		Stream<OutputNumber> result = null;
		
		try (Transaction tx = graphDB.beginTx()) {
			
			result = graphDB.execute(query.toString())
							.columnAs("counts")
							.stream()
							.map(Number.class::cast)
							.map(OutputNumber::new);			
			
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
	@Procedure(value="maven.miner.upgrade.all")
	@Description ("All the durations between upgrades for all libraries with more than one upgrade")
	public Stream<TimeToReleaseOutput> getAllUpgrades () {
		
		StringBuilder query = new StringBuilder("");
		
		query.append(" match (n:Artifact) where not (n)<-[:NEXT]-() ");
		query.append(" with n match (n)-[:NEXT*]->(m) with n.release_date as baseDate, n, m ");
		query.append(" return collect(maven.miner.duration.between(baseDate, m.release_date)) "
				+ "as timeToRelease, n.groupID as groupId, n.artifact as artifactId ");
		
		Stream<TimeToReleaseOutput> result = null;
		
		try (Transaction tx = graphDB.beginTx()) {
			
			result = graphDB.execute(query.toString())
							.stream()
							.map(TimeToReleaseOutput::new);															
			tx.success();
			
		} catch (Throwable e) {
			log.error(e.getMessage());
			throw new RuntimeException(e);	
		}
		return result;
	}
	
	private Node getMostRecentVersion(Node node) {
		
		return null;
		
	}
}
