package maven.miner.functions;

//import org.junit.Rule;
//import org.junit.Test;
//
//import org.neo4j.driver.v1.Config;
//import org.neo4j.driver.v1.Driver;
//import org.neo4j.driver.v1.GraphDatabase;
//import org.neo4j.driver.v1.Session;
//import org.neo4j.harness.junit.Neo4jRule;
//
//import maven.miner.procedures.Last;
//
//import static org.hamcrest.CoreMatchers.equalTo;
//import static org.junit.Assert.*;

public class LastTest
{
//    // This rule starts a Neo4j instance
//    @Rule
//    public Neo4jRule neo4j = new Neo4jRule()
//
//            // This is the function we want to test
//            .withAggregationFunction( Last.class );
//
//    @Test
//    public void shouldAllowReturningTheLastValue() throws Throwable
//    {
//        // This is in a try-block, to make sure we close the driver after the test
//        try( @SuppressWarnings("deprecation")
//		Driver driver = GraphDatabase
//                .driver( neo4j.boltURI() , Config.build().withEncryptionLevel( Config.EncryptionLevel.NONE ).toConfig() ) )
//        {
//            // Given
//            Session session = driver.session();
//
//            // When
//            Long result = session.run( "UNWIND range(1,10) as value RETURN example.last(value) AS last").single().get("last").asLong();
//
//            // Then
//            assertThat( result, equalTo( 10L ) );
//        }
//    }
}