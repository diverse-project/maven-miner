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

public class SimilartyFuncTest {
	
	//  This rule starts a Neo4j instance
  @Rule
  public Neo4jRule neo4j = new Neo4jRule()
          // This is the function we want to test
          .withFunction( SimilarityFunc.class );

  @Test
  public void shouldCosineSinglePass() throws Throwable
  {
      // This is in a try-block, to make sure we close the driver after the test
	  try( Driver driver = GraphDatabase.driver(neo4j.boltURI() , 
			   Config.build().withoutEncryption().toConfig())) {   
		  Session session = driver.session();
       

          // When
          Number result = session.run( "RETURN maven.miner.similarity.cosine.single"
          								+ "('geronimo-spec:geronimo-spec-j2ee-management:1.0-rc4', "
          								+ "'geronimo-spec:geronimo-spec-j2ee-jacc:1.0-rc4') AS result")
        		  				 .single()
        		  				 .get("result")
        		  				 .asNumber();

          // Then
         // assertThat( result, equalTo( 0.9629922474131865 ) );
      }
  }
  
  @Test
  public void shouldjaroWinklerSinglePass() throws Throwable
  {
      // This is in a try-block, to make sure we close the driver after the test
	  try( Driver driver = GraphDatabase.driver(neo4j.boltURI() , 
			   Config.build().withoutEncryption().toConfig())) {   
		  Session session = driver.session();
       

          // When
          Number result = session.run( "RETURN maven.miner.similarity.jaroWinkler.single"
          								+ "('geronimo-spec:geronimo-spec-j2ee-management:1.0-rc4', "
          								+ "'geronimo-spec:geronimo-spec-j2ee-jacc:1.0-rc4') AS result")
        		  				 .single()
        		  				 .get("result")
        		  				 .asNumber();

          // Then
         // assertThat( result, equalTo( 0.9629922474131865) );
      }
  }
  
  @Test
  public void shouldCosineSingleCustomPass() throws Throwable
  {
      // This is in a try-block, to make sure we close the driver after the test
	  try( Driver driver = GraphDatabase.driver(neo4j.boltURI() , 
			   Config.build().withoutEncryption().toConfig())) {   
		  Session session = driver.session();
       

          // When
          Number result = session.run( "RETURN maven.miner.similarity.cosine.custom.single"
          								+ "('geronimo-spec:geronimo-spec-j2ee-management:1.0-rc4', "
          								+ "'geronimo-spec:geronimo-spec-j2ee-jacc:1.0-rc4') AS result")
        		  				 .single()
        		  				 .get("result")
        		  				 .asNumber();

          // Then
         // assertThat( result, equalTo( 0.9629922474131865) );
      }
  }
  
  @Test
  public void shouldjaroWinklerSingleCustomPass() throws Throwable
  {
      // This is in a try-block, to make sure we close the driver after the test
	  try( Driver driver = GraphDatabase.driver(neo4j.boltURI() , 
			   Config.build().withoutEncryption().toConfig())) {   
		  Session session = driver.session();
       

          // When
          Number result = session.run( "RETURN maven.miner.similarity.jaroWinkler.custom.single"
          								+ "('geronimo-spec:geronimo-spec-j2ee-management:1.0-rc4', "
          								+ "'geronimo-spec:geronimo-spec-j2ee-jacc:1.0-rc4') AS result")
        		  				 .single()
        		  				 .get("result")
        		  				 .asNumber();

          // Then
         // assertThat( result, equalTo( 0.8684926731375503) );
      }
  }
  
  @Test
  public void shouldCosineListPass() throws Throwable
  {
      // This is in a try-block, to make sure we close the driver after the test
	  try( Driver driver = GraphDatabase.driver(neo4j.boltURI() , 
			   Config.build().withoutEncryption().toConfig())) {   
		  Session session = driver.session();
       

          // When
          Map result = session.run( "RETURN maven.miner.similarity.cosine"
          								+ "(['geronimo-spec:geronimo-spec-j2ee-management:1.0-rc4'], "
          								+ "['geronimo-spec:geronimo-spec-j2ee-jacc:1.0-rc4']) AS result")
        		  				 .single()
        		  				 .get("result")
        		  				.asMap();
          // Then
         // assertThat( result, equalTo( 0.9629922474131865 ) );
      }
  }
  
  @Test
  public void shouldjaroWinklerListPass() throws Throwable
  {
      // This is in a try-block, to make sure we close the driver after the test
	  try( Driver driver = GraphDatabase.driver(neo4j.boltURI() , 
			   Config.build().withoutEncryption().toConfig())) {   
		  Session session = driver.session();
       

          // When
          Map result = session.run( "RETURN maven.miner.similarity.jaroWinkler"
        								+ "(['geronimo-spec:geronimo-spec-j2ee-management:1.0-rc4'], "
        								+ "['geronimo-spec:geronimo-spec-j2ee-jacc:1.0-rc4']) AS result")
        		  				 .single()
        		  				 .get("result")
        		  				.asMap();
          // Then
         // assertThat( result, equalTo( 0.9629922474131865) );
      }
  }
  
  @Test
  public void shouldCosineListCustomPass() throws Throwable
  {
      // This is in a try-block, to make sure we close the driver after the test
	  try( Driver driver = GraphDatabase.driver(neo4j.boltURI() , 
			   Config.build().withoutEncryption().toConfig())) {   
		  Session session = driver.session();
       

          // When
          Map result = session.run( "RETURN maven.miner.similarity.cosine.custom"
					+ "(['geronimo-spec:geronimo-spec-j2ee-management:1.0-rc4'], "
					+ "['geronimo-spec:geronimo-spec-j2ee-jacc:1.0-rc4']) AS result")
        		  				 .single()
        		  				 .get("result")
        		  				 .asMap();
          // Then
         // assertThat( result, equalTo( 0.9629922474131865) );
      }
  }
  
  @Test
  public void shouldjaroWinklerListCustomPass() throws Throwable
  {
      // This is in a try-block, to make sure we close the driver after the test
	  try( Driver driver = GraphDatabase.driver(neo4j.boltURI() , 
			   Config.build().withoutEncryption().toConfig())) {   
		  Session session = driver.session();
       

          // When
          Map result = session.run( "RETURN maven.miner.similarity.jaroWinkler.custom"
										+ "(['geronimo-spec:geronimo-spec-j2ee-management:1.0-rc4'], "
										+ "['geronimo-spec:geronimo-spec-j2ee-jacc:1.0-rc4']) AS result")
        		  				 .single()
        		  				 .get("result")
        		  				 .asMap();

          // Then
         // assertThat( result, equalTo( 0.8684926731375503) );
      }
  }
}
