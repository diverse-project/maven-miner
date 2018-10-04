package maven.miner.procedures;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.v1.*;
import org.neo4j.harness.junit.Neo4jRule;
import org.sonatype.aether.graph.Dependency;

import fr.inria.diverse.maven.common.DependencyRelation;
import fr.inria.diverse.maven.common.Properties;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

public class TemporalIndexTest
{

    @Rule
    public Neo4jRule neo4j = new Neo4jRule().withProcedure( TemporalIndex.class );

    @Test
    public void shouldPass() throws Throwable {
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
        			
        			session.run( query );
            StatementResult result = session.run( "CALL maven.miner.group.temporalIndex('abbot')");
            
            assertThat(result.single().get(0).asBoolean(), equalTo(true));
            
            query = String.format("match (n:abbot)-[:%s]->(y)\n"
					+ "match (n:abbot)-[:%s]->(m)\n"
					+ "match (n:abbot)-[:%s]->(d)\n"
					+ "return y, m, d", 
					DependencyRelation.YEAR.toString(),
					DependencyRelation.MONTH.toString(),
					DependencyRelation.DAY.toString());
 			result = session.run(query);
 			Record resultRecord = result.single();
 			
 			assertThat(resultRecord.get("y").get(Properties.YEAR).asString(), equalTo("2005"));
//			assertThat(result.list().get(0).asNode().get(Properties.YEAR).asString(), equalTo("2005"));
			assertThat(resultRecord.get("m").get(Properties.MONTH).asString(), equalTo("AUGUST"));
			assertThat(resultRecord.get("d").get(Properties.DAY).asString(), equalTo("1"));
        }
    }
    
    
}
