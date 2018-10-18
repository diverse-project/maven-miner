package maven.miner.procedures;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.harness.junit.Neo4jRule;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class ArtifactInRangeTest
{

    @Rule
    public Neo4jRule neo4j = new Neo4jRule().withProcedure( ArtifactInRangeProc.class );

    @Test
    public void shouldPass() throws Throwable {
    	try( Driver driver = GraphDatabase.driver(neo4j.boltURI() , 
    											   Config.build().withoutEncryption().toConfig())) {   
    		Session session = driver.session();
            
            String query = "CREATE (p:abbot {artifact: 'abbot', release : '2005-05-01T09:17:57Z[GMT]', version : '3.2.2-a'})," 
				           + " (p1:abbot { artifact: 'abbot', release : '2005-08-01T09:17:57Z[GMT]', version : '3.2'})," 
						   + " (p2:abbot2 { artifact: 'abbot', release : '2005-08-01T09:17:57Z[GMT]', version : '3.2.2'}),"
						   + " (y:Calendar {year : 2005})," 
						   + " (m1:Calendar {month : 5})," 
						   + " (m2:Calendar {month : 8})," 
						   + " (m1)-[:MONTH]->(p),"
						   + " (p)<-[:YEAR]-(y)," 
						   + " (m2)-[:MONTH]->(p1),"
						   + " (p1)<-[:YEAR]-(y)," 
						   + " (m1)-[:MONTH]->(p2),"
						   + " (p2)<-[:YEAR]-(y)" 
						   + " return p";	
        	session.run( query );
        	
        	StatementResult result1 = session.run( "match (n) return count(n)");
            assertThat(result1.single().get(0).asInt(), equalTo(6));
            
        	result1 = session.run( " MATCH p=()-->() RETURN count(p)");
            assertThat(result1.single().get(0).asInt(), equalTo(6));
                   
//            StatementResult  result = session.run( "call maven.miner.version.between('activemq:activemq-web', '3.2','3.2.1',true)");
//            assertThat(result.list().size(), equalTo(1));
//            
//            result = session.run( "CALL maven.miner.artifacts.group.during('abbot', 2005)");
//            assertThat(result.list().size(), equalTo(2));
//            
//            result = session.run( "CALL maven.miner.artifacts.during(2005)");
//            assertThat(result.list().size(), equalTo(3));
//            
//            result = session.run( "CALL maven.miner.artifacts.group.during('abbot',2005, 8)");
//            assertThat(result.list().size(), equalTo(1));
//            
//            result = session.run( "CALL maven.miner.artifacts.during(2005, 8)");
//            assertThat(result.list().size(), equalTo(2));        
            }
    }
    
}
