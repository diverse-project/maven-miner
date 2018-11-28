package maven.miner.functions;

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.harness.junit.Neo4jRule;

import static org.hamcrest.core.IsEqual.equalTo;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

public class VersionFuncTest {
	
	//  This rule starts a Neo4j instance
  @Rule
  public Neo4jRule neo4j = new Neo4jRule()
          // This is the function we want to test
          .withFunction( VersionFunc.class );

  @Test
  public void shouldPassIsSameMinor() throws Throwable
  {
      // This is in a try-block, to make sure we close the driver after the test
	  try( Driver driver = GraphDatabase.driver(neo4j.boltURI() , 
			   Config.build().withoutEncryption().toConfig())) {   
		  Session session = driver.session();
       

          // When
          Boolean result = session.run( "RETURN maven.miner.version.isSameMinor('1.5.6', '2.6') AS result")
        		  				 .single()
        		  				 .get("result")
        		  				 .asBoolean();

          // Then
           assertThat( result, equalTo(true) );
      }
  }
  
}
