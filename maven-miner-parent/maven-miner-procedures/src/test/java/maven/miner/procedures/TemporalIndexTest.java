package maven.miner.procedures;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.v1.*;
import org.neo4j.harness.junit.Neo4jRule;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class TemporalIndexTest
{

    @Rule
    public Neo4jRule neo4j = new Neo4jRule()

            // This is the Procedure we want to test
            .withProcedure( TemporalIndex.class );

    @Test
    public void shouldAllowIndexingAndFindingANode() throws Throwable
    {
    	try( Driver driver = GraphDatabase.driver(neo4j.boltURI() , 
    											   Config.build().withoutEncryption().toConfig()))
        {

            Session session = driver.session();

            String query = "CREATE (p:abbot {\n" + 
            		"  artifact: 'abbot',\n" + 
            		"  release_date: '2005-08-01T09:17:57Z[GMT]',\n" + 
            		"  groupID: 'abbot',\n" + 
            		"  coordinates: 'abbot:abbot:0.13.0',\n" + 
            		"  packaging: 'Jar',\n" + 
            		"  version: '0.13.0'" + 
            		"}) return id(p)";
            
            long nodeId = session.run( query )
			                     .single()
			                     .get(0).asLong();

            StatementResult result = session.run( "CALL maven.miner.group.temporalIndex('abbot')");
            
            assertThat(result.single().get(0).asBoolean(), equalTo(true));
        }
    }
}
