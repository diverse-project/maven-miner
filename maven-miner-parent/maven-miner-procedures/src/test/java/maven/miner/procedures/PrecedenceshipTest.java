package maven.miner.procedures;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.v1.*;
import org.neo4j.harness.junit.Neo4jRule;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class PrecedenceshipTest
{

    @Rule
    public  Neo4jRule neo4j = new Neo4jRule().withProcedure( PrecedenshipProc.class );

   
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
        	
            StatementResult result = session.run( "CALL maven.miner.group.precedenceship('abbot')");
            
            assertThat(result.single().get(0).asBoolean(), equalTo(true));
            
            query = "match p=(:abbot)-[:NEXT*]->(:abbot) return max(length(p))";
 			result = session.run(query);
 	
 			assertThat(result.single().get(0).asInt(), equalTo(2));

        }
    }
    

    @Test
    public void allGroupsPrecedenceShouldBeCreated() throws Throwable {
    	try( Driver driver = GraphDatabase.driver(neo4j.boltURI() , 
    											   Config.build().withoutEncryption().toConfig()))
        {

            Session session = driver.session();
            
            String query = "CREATE (p1:abbot:Artifact {\n" + 
        			"  artifact: 'abbot',\n" + 
        			"  groupID: 'abbot',\n" + 
        			"  coordinates: 'abbot:abbot:0.13.0',\n" + 
        			"  version: '0.13.0'" + 
        			"})\n";
        	query+="CREATE (p2:abbot:Artifact {\n" + 
        			"  artifact: 'abbot',\n" + 
        			"  groupID: 'abbot',\n" + 
        			"  coordinates: 'abbot:abbot:0.12.1',\n" + 
        			"  version: '0.12.1'" + 
        			"})\n";
        	query+="CREATE (p3:abbot:Artifact {\n" + 
        			"  artifact: 'abbot',\n" + 
        			"  groupID: 'abbot',\n" + 
        			"  coordinates: 'abbot:abbot:1.14.0',\n" + 
        			"  version: '1.14.0'" + 
        			"})\n";
        	query+="CREATE (p4:ant:Artifact {\n" + 
        			" artifact: 'ant',"+
        			" groupID: 'ant',"+
        			" coordinates: 'ant:ant:1.6',"+
        			" version: '1.6'"+
        			"})\n";
        	query+="CREATE (p5:ant:Artifact {\n" + 
        			" artifact: 'ant',"+
        			" groupID: 'ant',"+
        			" coordinates: 'ant:ant:0.3',"+
        			" version: '0.3'"+
        			"})\n";
        
        	session.run( query );
        	
            StatementResult result = session.run( "CALL maven.miner.precedenceship");
            assertThat(result.single().get(0).asBoolean(), equalTo(true));
            
            query = "match p=(:abbot)-[:NEXT*]->(:abbot) return max(length(p))";
 			result = session.run(query);
 			assertThat(result.single().get(0).asInt(), equalTo(2));
 			
 			query = "call maven.miner.artifacts.latest";
 			result = session.run(query);
 			assertThat(result.list().size(), equalTo(2));
        }
    }
    
    @Test 
    public void isIdempotent() {
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
			
			session.run( "CALL maven.miner.group.precedenceship('abbot')");
			StatementResult result = session.run( "CALL maven.miner.group.precedenceship('abbot')");			
			assertThat(result.single().get(0).asBoolean(), equalTo(true));
			
			query = "match p=(:abbot{coordinates: 'abbot:abbot:0.13.0'})-[r:NEXT]->(:abbot {coordinates: 'abbot:abbot:1.14.0'}) return count(r)";
			result = session.run(query);
			
			assertThat(result.single().get(0).asInt(), equalTo(1));
			
			query = "match p=(:abbot{coordinates: 'abbot:abbot:0.12.1'})-[r:NEXT]->(:abbot {coordinates: 'abbot:abbot:0.13.0'}) return count(r)";
			result = session.run(query);
			
			assertThat(result.single().get(0).asInt(), equalTo(1));
			}
    }
}
