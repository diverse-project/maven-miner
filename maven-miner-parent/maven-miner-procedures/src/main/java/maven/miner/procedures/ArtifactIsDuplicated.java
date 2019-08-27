package maven.miner.procedures;

import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.neo4j.graphdb.Transaction;
//import org.neo4j.graphdb.traversal.Uniqueness;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import maven.miner.output.ConflictingOutput;

public class ArtifactIsDuplicated extends AbstractProcedureEnv {
	
	/**
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	@Procedure (value = "maven.miner.library.isConflicted.stream", mode = Mode.READ)
	@Description ("maven.miner.library.isConflicted('g:a:v', 'g:a:v') "
			+ "- Retrieving all conflicting artifacts within the dependency tree, given a dependency pair identified bu the client gav and the lib gav")
	public Stream<ConflictingOutput>  getConflicts (@Name(value = "Client coordinates") String client,
														   @Name(value = "Library coordinates") String lib) {
		String [] gaCli = client.split(":");
		String [] gaLib = lib.split(":");
		
		if (gaCli.length > 3 || gaLib.length > 3) {
			throw new RuntimeException("A");
		} else if (gaCli.length < 3 || gaLib.length < 3) {
			throw new RuntimeException("Missing entry! The groupID, the ArtifactID and the version should be provided");
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append(String.format(" WITH '%s' as client , '%s' as lib\n" + 
				"MATCH (n:Artifact {coordinates:client})-[r:DEPENDS_ON]->(m:Artifact {coordinates:lib}) WITH m,n,r\n" + 
				"MATCH (n)-[:DEPENDS_ON*1..10]->(x) WHERE (x)-[:LIBRARY]->()<-[:LIBRARY]-(m) and not id(x)=id(m) WITH m,n,x,r\n" + 
				"RETURN n.coordinates AS client, m.coordinates AS lib, r.scope as scope, CASE WHEN size(collect(distinct x.coordinates)) < 2 THEN FALSE ELSE TRUE END AS conflict",client, lib));
		
		//builder.append(" RETURN n;");
		Stream<ConflictingOutput> result = null;
		try (Transaction tx = graphDB.beginTx()) {
		
	         result =  graphDB.execute(builder.toString()).stream().map(ConflictingOutput::new);

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
	 * @return
	 */
	@Procedure (value = "maven.miner.library.isConflicted", mode = Mode.WRITE)
	@Description ("maven.miner.library.isConflicted('g:a:v', 'g:a:v') "
			+ "- Retrieving all conflicting artifacts within the dependency tree, given a dependency pair identified bu the client gav and the lib gav")
	public Stream<ConflictingOutput>  setConflicts (@Name(value = "Client coordinates") String client,
														   @Name(value = "Library coordinates") String lib) {
		String [] gaCli = client.split(":");
		String [] gaLib = lib.split(":");
		
		if (gaCli.length > 3 || gaLib.length > 3) {
			throw new RuntimeException("A");
		} else if (gaCli.length < 3 || gaLib.length < 3) {
			throw new RuntimeException("Missing entry! The groupID, the ArtifactID and the version should be provided");
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append(String.format(" WITH '%s' as client , '%s' as lib\n" + 
				"MATCH (n:Artifact {coordinates:client})-[r:DEPENDS_ON]->(m:Artifact {coordinates:lib}) WHERE not exists (r.isConflicted) WITH m,n,r\n" + 
				"MATCH (n)-[:DEPENDS_ON*1..10]->(x) WHERE (x)-[:LIBRARY]->()<-[:LIBRARY]-(m) and not id(x)=id(m) WITH m,n,x,r\n" + 
				"WITH r, CASE WHEN size(collect(distinct x.coordinates)) < 2 THEN FALSE ELSE TRUE END  as conflict SET r.isConflicted =  conflict",client, lib));
		
		
		try (Transaction tx = graphDB.beginTx()) {
		
	         graphDB.execute(builder.toString()).stream().map(ConflictingOutput::new);

			tx.success();
		} catch (Throwable e) {
			log.error(e.getMessage());
			throw new RuntimeException(e);	
		}
		return null;
	}
	
	/**
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	@Procedure (value = "maven.miner.library.isConflicted.csv", mode = Mode.WRITE)
	@Description ("maven.miner.library.isConflicted('g:a:v', 'g:a:v') "
			+ "- Retrieving all conflicting artifacts within the dependency tree, given a dependency pair identified bu the client gav and the lib gav")
	public Stream<ConflictingOutput>  getConflictsFromCSV  (@Name( value = "PATH to the CSV file with the pair coordinates") String path ){
		
		List <ConflictingOutput> result = new ArrayList<ConflictingOutput>();
		try {
			Reader in = new FileReader(path);
			Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(in);
			for (CSVRecord record : records) {
			    String cli = record.get(0);
			    String lib = record.get(1);
			    setConflicts(cli, lib);
			}

		} catch (Throwable e) {
			log.error(e.getMessage());
			throw new RuntimeException(e);	
		}
		
		return result.stream();
	}
	
}



