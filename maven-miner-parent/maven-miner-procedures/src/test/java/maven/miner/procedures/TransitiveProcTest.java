package maven.miner.procedures;

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.harness.junit.Neo4jRule;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;

public class TransitiveProcTest {

	 @Rule
	  public Neo4jRule neo4j = new Neo4jRule().withProcedure( TransitiveProc.class );

    @Test
    public void shouldPass() throws Throwable {
    	try( Driver driver = GraphDatabase.driver(neo4j.boltURI() , 
    											   Config.build().withoutEncryption().toConfig())) {   
    		Session session = driver.session();
            
            String query = "CREATE (p:abbot {coordinates : 'abbot:abbot:1.2.0'}),"
            			   + " (p1:abbot {coordinates :'abbot:abbot:1.4.0'})," 
						   + " (d1:junit {coordinates :'junit:junit:4.4'}),"  
						   + " (d2:junit {coordinates :'junit:junit:4.9'}),"
						   + " (d3:groovy {coordinates :'groovy:groovy-all:1.0-jsr-04'})," 
						   + " (p)-[:DEPENDS_ON]->(d1), "
						   + " (p1)-[:DEPENDS_ON]->(d2), "	
						   + " (d2)-[:DEPENDS_ON]->(d3), "
						   + " (d1)-[:DEPENDS_ON]->(d3), "	
						   + " (p)-[:NEXT]->(p1), "
						   + " (d1)-[:NEXT]->(d2) "	
						   + " return p" 
						    ;
        			
        	session.run( query );
        	
        	StatementResult result1 = session.run( "match (n) return count(n)");
            assertThat(result1.single().get(0).asInt(), equalTo(5));
            List<Record> myList= null;
        	result1 = session.run( " MATCH p=()-->() RETURN count(p)");
            assertThat(result1.single().get(0).asInt(), equalTo(6));
            
//            result1 = session.run( " call maven.miner.artifacts.group.dead('junit') ");
//            myList= result1.list();
//            assertThat(myList.get(0).get(1).asBoolean(), equalTo(true));
//            assertThat(myList.get(1).get(1).asBoolean(), equalTo(false));
//            
//            result1 = session.run( " call maven.miner.artifacts.group.dead('abbot') ");
//            myList= result1.list();
//            assertThat(myList.get(0).get(1).asBoolean(), equalTo(true));
//            assertThat(myList.get(1).get(1).asBoolean(), equalTo(true));
            
            result1 = session.run( " call maven.miner.artifacts.group.dead('groovy') ");
            myList= result1.list();
            assertThat(myList.get(0).get(1).asBoolean(), equalTo(false));
            
//            StatementResult result = session.run( "CALL maven.miner.artifacts.dependencySwap");
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
