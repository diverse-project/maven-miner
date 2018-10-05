package maven.miner.procedures;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.v1.*;
import org.neo4j.harness.junit.Neo4jRule;
import fr.inria.diverse.maven.common.DependencyRelation;
import fr.inria.diverse.maven.common.Properties;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class TemporalIndexTest
{

    @Rule
    public Neo4jRule neo4j = new Neo4jRule().withProcedure( TemporalIndexProc.class );

    @Test
    public void shouldPass() throws Throwable {
    	try( Driver driver = GraphDatabase.driver(neo4j.boltURI() , 
    											   Config.build().withoutEncryption().toConfig())) {   
    		Session session = driver.session();
            
            String query = "CREATE (p:abbot {\n" + 
        			"  artifact: 'abbot',\n" + 
        			"  release_date: '2005-08-01T09:17:57Z[GMT]',\n" + 
        			"  groupID: 'abbot',\n" + 
        			"  coordinates: 'abbot:abbot:0.13.0',\n" + 
        			"  packaging: 'Jar',\n" + 
        			"  version: '0.13.0'" + 
        			"}) return id(p)";
        			
        	session.run( query );
            StatementResult result = session.run( "CALL maven.miner.group.temporalIndex('abbot')");
            
            assertThat(result.single().get(0).asBoolean(), equalTo(true));
            
            query = String.format("match (n:abbot)-[ry:%s]->(y)\n"
					+ "match (n:abbot)-[rm:%s]->(m)\n"
					+ "match (n:abbot)-[rd:%s]->(d)\n"
					+ "return y, m, d", 
					DependencyRelation.YEAR.toString(),
					DependencyRelation.MONTH.toString(),
					DependencyRelation.DAY.toString());
 			result = session.run(query);
 			Record resultRecord = result.single();
 			
 			assertThat(resultRecord.get("y").get(Properties.YEAR).asInt(), equalTo(2005));
			assertThat(resultRecord.get("m").get(Properties.MONTH).asInt(), equalTo(8));
			assertThat(resultRecord.get("d").get(Properties.DAY).asInt(), equalTo(1));
			
			result = session.run( "CALL maven.miner.group.temporalIndex('abbot')");
			
			assertThat(result.single().get(0).asBoolean(), equalTo(true));
			
			query = String.format("match (n:abbot)-[ry:%s]->(y)\n"
									+ "match (n:abbot)-[rm:%s]->(m)\n"
									+ "match (n:abbot)-[rd:%s]->(d)\n"
									+ "return count(y), "
									+ " count(m),"
									+ " count(d),"
									+ " count(ry),"
									+ " count(rm),"
									+ " count(rd)", 
									DependencyRelation.YEAR.toString(),
									DependencyRelation.MONTH.toString(),
									DependencyRelation.DAY.toString());
			result = session.run(query);
			resultRecord = result.single();
			
			assertThat(resultRecord.get(0).asInt(), equalTo(1));
			assertThat(resultRecord.get(1).asInt(), equalTo(1));
			assertThat(resultRecord.get(2).asInt(), equalTo(1));
			assertThat(resultRecord.get(3).asInt(), equalTo(1));
			assertThat(resultRecord.get(4).asInt(), equalTo(1));
			assertThat(resultRecord.get(5).asInt(), equalTo(1));		
        }
    }
    
}
