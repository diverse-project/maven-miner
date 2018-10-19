package maven.miner.procedures;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.harness.junit.Neo4jRule;

public class ReverseDependencyProcTest {

	 @Rule
	    public  Neo4jRule neo4j = new Neo4jRule().withProcedure( ReverseDependencyProc.class );

	   
	    @Test
	    public void precedenceshipShouldBeCreated() throws Throwable {
	    	try( Driver driver = GraphDatabase.driver(neo4j.boltURI() , 
	    											   Config.build().withoutEncryption().toConfig()))
	        {

	            Session session = driver.session();
	            
	            String query = "CREATE (p1:abbot:Artifact {\n" + 
	        			"  artifact: 'abbot',\n" + 
	        			"  groupID: 'abbot',\n" + 
	        			"  coordinates: 'abbot:abbot:0.13.0',\n" + 
	        			"  version: '0.13.0'" + 
	        			"})";
	        	query+="CREATE (p2:abbot:Artifact {\n" + 
	        			"  artifact: 'abbot',\n" + 
	        			"  groupID: 'abbot',\n" + 
	        			"  coordinates: 'abbot:abbot:0.12.1',\n" + 
	        			"  version: '0.12.1'" + 
	        			"})";
	        	query+="CREATE (p3:abbot:Artifact {\n" + 
	        			"  artifact: 'abbot',\n" + 
	        			"  groupID: 'abbot',\n" + 
	        			"  coordinates: 'abbot:abbot:1.14.0',\n" + 
	        			"  version: '1.14.0'" + 
	        			"})";
	        	session.run( query );
//	        	
//	            StatementResult result = session.run( "call maven.miner.dependent.ofRange('org.apache.camel:camel-mail',true,'2.21.0', '2.21.1',true) YIELD outputNode\n" + 
//	            		"return distinct outputNode");
//	            
//	            assertThat(result.single().get(0).asBoolean(), equalTo(true));
//	            
//	            query = "match p=(:abbot)-[:NEXT*]->(:abbot) return max(length(p))";
//	 			result = session.run(query);
//	 	
//	 			assertThat(result.single().get(0).asInt(), equalTo(2));

	        }
	    }
	    
}
