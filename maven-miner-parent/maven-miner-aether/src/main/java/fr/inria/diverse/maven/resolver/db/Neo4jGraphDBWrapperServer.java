package fr.inria.diverse.maven.resolver.db;

import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.kernel.DeadlockDetectedException;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.version.GenericVersionScheme;
import org.sonatype.aether.version.InvalidVersionSpecificationException;
import org.sonatype.aether.version.Version;

import static org.neo4j.driver.v1.Values.parameters;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import fr.inria.diverse.maven.common.Properties;
import fr.inria.diverse.maven.model.Edge.Scope;
import fr.inria.diverse.maven.model.ExceptionCounter;
import fr.inria.diverse.maven.model.ExceptionCounter.ExceptionType;
import fr.inria.diverse.maven.model.JarCounter.JarEntryType;
import fr.inria.diverse.maven.util.MavenMinerUtil;
import fr.inria.diverse.maven.model.JarCounter;

public class Neo4jGraphDBWrapperServer extends Neo4jGraphDBWrapper implements AutoCloseable{

	private final Driver driver;
	
	public Neo4jGraphDBWrapperServer(String uri) {
		this.driver = GraphDatabase.driver("bolt://"+uri);
		initDB();
	}
	/**
	 * Initializes the database, including schema and indexes creation, and database cleaning
	 */
	private void initDB() {

		// creating schema 
		try ( Session session = driver.session() )
        {	
            session.writeTransaction( tx ->
            {	
            	String query = String.format("CREATE CONSTRAINT ON (artf:%s) "
            													+ "ASSERT artf.%s IS UNIQUE",
																Properties.ARTIFACT_LABEL, 
																Properties.COORDINATES);
            	query+="\n";
            	query+= String.format("CREATE CONSTRAINT ON (artf:%s) "
						+ "ASSERT EXISTS(artf.%s)",
						Properties.ARTIFACT_LABEL, 
						Properties.COORDINATES);
            	tx.run(query);
            	
            	LOGGER.info("Schema was succesfully created");
                return null;
            });
        } catch (Exception e) {
			LOGGER.error("Unable to create the schema");
			throw e;
		}
		try ( Session session = driver.session() )
        {
            session.writeTransaction( tx ->
            {	
            	String query = String.format("CREATE CONSTRAINT ON (exp:%s) "
            								+ "ASSERT exp.%s IS UNIQUE",
											Properties.EXCEPTION_LABEL, 
											Properties.EXCEPTION_NAME);
            	
            	tx.run(query);
                return null;
            });
        } catch (Exception e) {
			LOGGER.error("Unable to create the schema");
			throw e;
		}
		LOGGER.info("Schema was succesfully created");   
		//Creating indexes
		
		try ( Session session = driver.session() )
        {
            session.writeTransaction( tx ->
            {	
            	String query = String.format("CREATE INDEX ON :%s(%s )",
					            			Properties.ARTIFACT_LABEL,  
					        				Properties.GROUP);
            	tx.run(query); 
                return null;
            });
        } catch (Exception e) {
			LOGGER.error("Unable to create the indexes");
			throw e;
		}
		
    	
    	LOGGER.info("Indexes were succesfully created");
	}
	
	@Override
//	public @NotNull(message = "The returned node should not be null") 
	public void createNodeFromArtifactCoordinate(Artifact artifact) {
		
		ZonedDateTime javaDate =getReleaseDateFromArtifact(artifact);
		for (int i = 0; i < RETRIES; i++) {
			 try ( Session session = driver.session() )
		        {
		            session.writeTransaction( tx ->
		            {
		            	String query =String.format("MERGE (a:%s:`%s` { %s : $coordinatesValue }) "
		            			+ "SET a+= {"
								+ "%s : $artifactValue, "
								+ "%s : $groupValue, "
								+ "%s : $versionValue, "
								+ "%s : $packagingValue, "
								+ "%s : $classifierValue, "
								+ "%s : $releaseValue } "
								+ "RETURN a.%s",
								Properties.ARTIFACT_LABEL,
								artifact.getGroupId(),
								Properties.COORDINATES,
								Properties.ARTIFACT,
								Properties.GROUP, 
								Properties.VERSION,
								Properties.PACKAGING,
								Properties.CLASSIFIER,
								Properties.LAST_MODIFIED,
								Properties.COORDINATES);	
		            	StatementResult result = tx.run( query,
							                             parameters(
							                                "groupValue", artifact.getGroupId(),
							                            	"artifactValue", artifact.getArtifactId(),
							                            	"coordinatesValue", MavenMinerUtil.artifactToCoordinate(artifact),
							                            	"versionValue", artifact.getVersion(),
							                            	"packagingValue", MavenMinerUtil.derivePackaging(artifact).toString(),
							                            	"classifierValue", artifact.getClassifier(),
							                            	"releaseID", Properties.LAST_MODIFIED,
							                            	"releaseValue",javaDate.toString()
							                              ));
		                result.single().get(0).asString();
		                
		                return null;
		            } );
		        }  catch  (Throwable ex)  {
					 txEx = ex;
				     if (!(ex instanceof DeadlockDetectedException)) {
				         break;
				     }
				     if ( i < RETRIES-1 ) {
					     try {
					            Thread.sleep( BACKOFF );
					     } catch ( InterruptedException e ) {
					            throw new TransactionFailureException( "Trasaction failed due to thread interuption", e );
					     }
				     }
		        }
			}
		// THEN
	    wrapException(txEx);
	}
	@Override
	public void addDependency(Artifact sourceArtifact, Artifact targetArtifact, Scope scope) {
		for (int i = 0; i < RETRIES; i++) {
			try ( Session session = driver.session() )
	        {
	            session.writeTransaction( tx ->  {		
	            	String query =  String.format("MATCH (source : `%s` { "
	            		+ "%s : $coordinatesValue1 }), "
	            		+ "(target : `%s` { %s : $coordinatesValue2 }) "
	            		+ "MERGE (source)-[r : DEPENDS_ON { %s : $scopeValue } ]->(target)"
	            		+ "RETURN source.%s",
	            		sourceArtifact.getGroupId(),
	            		Properties.COORDINATES,
	            		targetArtifact.getGroupId(),
	            		Properties.COORDINATES,
	            		Properties.SCOPE,
	            		Properties.COORDINATES);
	            	
	            	StatementResult result = tx.run(query, parameters("coordinatesValue1", MavenMinerUtil.artifactToCoordinate(sourceArtifact),
								                            		  "coordinatesValue2", MavenMinerUtil.artifactToCoordinate(targetArtifact),
								                            		  "scopeValue",scope.toString()
					                            		 	       ));
	                result.single().get(0).asString();
	                return null;
	            } );
	        } catch  (Throwable ex)  {
				 txEx = ex;
			     if (!(ex instanceof DeadlockDetectedException)) {
			         break;
			     }
			     if ( i < RETRIES-1 ) {
				     try {
				            Thread.sleep( BACKOFF );
				     } catch ( InterruptedException e ) {
				            throw new TransactionFailureException( "Trasaction failed due to thread interuption", e );
				     }
			     }
	        }
		}
		// THEN
	    wrapException(txEx);
	}

	@Override
	public void updateDependencyCounts(Artifact artifact, JarCounter jarCounter) {
		for (int i = 0; i < RETRIES; i++) {
			try ( Session session = driver.session() )
	        {
	            session.writeTransaction( tx ->
	            {		
	            		final StringBuilder query = new StringBuilder(String.format("MATCH (a:`%s` {%s:$coordinatesValue}) "
	            				+ "SET a+= {",artifact.getGroupId(),Properties.COORDINATES));
	            		
	            		Arrays.asList(JarEntryType.values())
	            			  .forEach(type -> query.append(String.format(" %s : %d, ", type.getName(), jarCounter.getValueForType(type))));           			                   
	            		query.delete(query.length()-2, query.length()-1);
	            		query.append('}');
	            		query.append(System.getProperty("line.separator"));
	            		query.append(String.format("RETURN a.%s",Properties.GROUP));
	            		StatementResult result = tx.run( query.toString(),
	                             parameters("groupValue", artifact.getGroupId(),
	                            		 	"coordinatesValue", MavenMinerUtil.artifactToCoordinate(artifact)
	                              )
	                           );
	            		result.single().get(0).asString();
	                    return null;
	            } );
	        } catch  (Throwable ex)  {
				 txEx = ex;
			     if (!(ex instanceof DeadlockDetectedException)) {
			         break;
			     }
			     if ( i < RETRIES-1 ) {
				     try {
				            Thread.sleep( BACKOFF );
				     } catch ( InterruptedException e ) {
				            throw new TransactionFailureException( "Trasaction failed due to thread interuption", e );
				     }
			     }
	        }
		}
		// THEN
	    wrapException(txEx);
	}
	/**
	 * @see Neo4jGraphDBWrapper#updateDependencyCounts(Artifact, JarCounter, ExceptionCounter)
	 */
	@Override
	public void updateDependencyCounts(Artifact artifact, JarCounter jarCounter, ExceptionCounter exCounter) {
		//updating dependency counts
		updateDependencyCounts(artifact, jarCounter);
		Arrays.asList(ExceptionType.values())
	      	  .forEach(type -> { updateExceptionCounter(artifact, type, exCounter);});	  
	} 		
	
	private void updateExceptionCounter(Artifact artifact, ExceptionType type, ExceptionCounter exCounter) {
		for (int i = 0; i < RETRIES; i++) {
			try ( Session session = driver.session() ) {
				session.writeTransaction( tx -> {
					final StringBuilder query = new StringBuilder(String.format("MATCH (a:`%s` {%s:$coordinatesValue})",artifact.getGroupId(), Properties.COORDINATES));
					query.append(String.format("MERGE (e : %s { %s : $name})", 
			 				Properties.EXCEPTION_LABEL,
			 				Properties.EXCEPTION_NAME
			 				));
					query.append(System.getProperty("line.separator"));
					query.append(String.format("CREATE UNIQUE (a)-[r : RAISES {%s:$value}]->(e)", 
								Properties.EXCEPTION_OCCURENCE
				 				));
					query.append(System.getProperty("line.separator"));
					query.append(String.format("RETURN e.%s",Properties.EXCEPTION_NAME));
					
					StatementResult result = tx.run( query.toString(),
			                 						parameters( "coordinatesValue",MavenMinerUtil.artifactToCoordinate(artifact),
			                 									"value",exCounter.getValueForType(type),
			                 									"name",type.name()));
			                 
					result.single().get(0).asString();
			        return null;
		        } );
			
			} catch  (Throwable ex)  {
				 txEx = ex;
			     if (!(ex instanceof DeadlockDetectedException)) {
			         break;
			     }
			     if ( i < RETRIES-1 ) {
				     try {
				            Thread.sleep( BACKOFF );
				     } catch ( InterruptedException e ) {
				            throw new TransactionFailureException( "Trasaction failed due to thread interuption", e );
				     }
			     }
	        }
		}
		// THEN
	    wrapException(txEx);
	}


	/**
	 * @see Neo4jGraphDBWrapper#createIndexes()
	 */
	@Override
	public void addResolutionExceptionRelationship(Artifact artifact) {
		for (int i = 0; i < RETRIES; i++) {
			try ( Session session = driver.session() ) {
				session.writeTransaction( tx -> {
					final StringBuilder query = new StringBuilder(String.format("MERGE (a:`%s` {%s:$coordinatesValue})",artifact.getGroupId(), Properties.COORDINATES));
					query.append(System.getProperty("line.separator"));
					query.append(String.format("MERGE (e : %s { %s : '%s' })", 
				 				Properties.EXCEPTION_LABEL,
				 				Properties.EXCEPTION_NAME,
				 				ExceptionType.RESOLUTION.name()
				 				));
					query.append(System.getProperty("line.separator"));
					query.append("CREATE UNIQUE (a)-[r : RAISES]->(e)");
					query.append(System.getProperty("line.separator"));
					query.append(String.format("RETURN e.%s",Properties.EXCEPTION_NAME));
					
					StatementResult result = tx.run( query.toString(),
			                 						parameters("coordinatesValue", 
			                 							MavenMinerUtil.artifactToCoordinate(artifact)));
			                 
					result.single().get(0).asString();
			        return null;
		        } );
			} catch  (Throwable ex)  {
				 txEx = ex;
			     if (!(ex instanceof DeadlockDetectedException)) {
			         break;
			     }
			     if ( i < RETRIES-1 ) {
				     try {
				            Thread.sleep( BACKOFF );
				     } catch ( InterruptedException e ) {
				            throw new TransactionFailureException( "Trasaction failed due to thread interuption", e );
				     }
			     }
	        }
		}
		// THEN
	    wrapException(txEx);
	
	}
	/**
	 * @see Neo4jGraphDBWrapper#shutdown()
	 */
	@Override
	public void shutdown() {
		driver.close();
	}
	/**
	 * @see Neo4jGraphDBWrapper#createPrecedenceShip()
	 */
	@Override
	public void createPrecedenceShip() {
		
		LOGGER.info("Creating plugins version's evolution ");
		
		@SuppressWarnings("unchecked")
		List<String> labels = driver.session(AccessMode.READ).readTransaction(tx -> {
			final String query = String.format("MATCH (n) "
					+ "WITH distinct labels(n) as allLabels "
					+ "WITH collect([label in allLabels WHERE label <> '%s' AND label <> '%s' | label ]) as filteredLabels "
					+ "RETURN reduce(s=[], x IN filteredLabels | s  + x) as reduced"
					,Properties.ARTIFACT_LABEL, Properties.EXCEPTION_LABEL);
			StatementResult result =  tx.run(query);
			return (List<String>) result.single().asMap().get("reduced");
			
		});
		labels.forEach(label -> {
			List<DefaultArtifact> artifactsPerLabel=null; 
			try (Session session =  driver.session()) {
				artifactsPerLabel = session.readTransaction(tx -> {
					final String innerQuery = String.format("match (n:`%s`) "
			 				+ "return n.%s as %s, "
			 				+ "n.%s as %s, "
			 				+ "n.%s as %s"
			 				,label
			 				,Properties.GROUP
			 				,Properties.GROUP
			 				,Properties.VERSION
			 				,Properties.VERSION
			 				,Properties.ARTIFACT
			 				,Properties.ARTIFACT);
					StatementResult result = tx.run(innerQuery);
					return  result.list()
								  .stream()
								  .map(entry -> new DefaultArtifact(String.format("%s:%s:%s"
				 					  										  ,entry.get(Properties.GROUP).asString()
				 					  										  ,entry.get(Properties.ARTIFACT).asString()
				 					  										  ,entry.get(Properties.VERSION).asString())))
								  .sorted(new Comparator<DefaultArtifact>() {
									@Override
									public int compare(DefaultArtifact n1, DefaultArtifact n2) {
										//String p1 = n1.getProperty
										String p1 = (String) n1.getArtifactId();
										String p2 = (String) n2.getArtifactId();
										Version v1 = null;
										Version v2 = null;
										if (p1.compareTo(p2) != 0) return p1.compareTo(p2);
										final GenericVersionScheme versionScheme = new GenericVersionScheme();
										try {
											v1 = versionScheme.parseVersion(n1.getVersion());
											v2 = versionScheme.parseVersion(n2.getVersion());
										} catch (InvalidVersionSpecificationException e) {
											LOGGER.error(e.getMessage());
											e.printStackTrace();
										}
									    return v1.compareTo(v2); 		
									}
								  })
								  .collect(Collectors.toList());
				});
			}
			for (int i =0; i< artifactsPerLabel.size() - 2; i++) {
				DefaultArtifact firstNode = artifactsPerLabel.get(i);
				DefaultArtifact secondNode = artifactsPerLabel.get(i+1);
				
				if (firstNode.getArtifactId().equals(secondNode.getArtifactId())) {
					createNextRelationship(firstNode, secondNode);
				}
			}
		});
	}
	/**
	 * Creating next relationship between two artifacts
	 * @param firstNode
	 * @param secondNode
	 */
	private void createNextRelationship(DefaultArtifact firstNode, DefaultArtifact secondNode) {
		for ( int i = 0; i < RETRIES; i++ ) {
			try {
				driver.session().writeTransaction(tx -> {
					final String  query = String.format("MATCH (source : `%s` { %s : $coordinates1}), "
														+ "(target : `%s` {%s : $coordinates2}) "
														+ " MERGE (source)-[r:%s]->(target) "
														+ "RETURN target"
														,firstNode.getGroupId()
														,Properties.COORDINATES
														,secondNode.getGroupId()
														,Properties.COORDINATES
														,"NEXT");
					
					final String coordinates1 = String.format("%s:%s:%s"
																,firstNode.getGroupId()
																,firstNode.getArtifactId()
																//,firstNode.getProperties().get(Properties.PACKAGING)
																,firstNode.getVersion());
					
					final String coordinates2 = String.format("%s:%s:%s"
							,secondNode.getGroupId()
							,secondNode.getArtifactId()
							//,secondNode.getProperties().get(Properties.PACKAGING)
							,secondNode.getVersion());
		
					StatementResult result = tx.run(query, parameters("coordinates1",coordinates1,
																	  "coordinates2",coordinates2));
					result.single().get(0).asNode();
					return null;
					});
			 } catch (Throwable ex) {
				txEx = ex;
		        if (!(ex instanceof DeadlockDetectedException)) {
		            break;
		        }
		        if ( i < RETRIES - 1 ) {
			        try {
			            Thread.sleep( BACKOFF );
			        } catch ( InterruptedException e ) {
			            throw new TransactionFailureException( "Trasaction failed due to thread interuption", e );
			        }
		        }  
			}
		}
		// THEN
	    wrapException(txEx);
		
	}
	/**
	 * @see Neo4jGraphDBWrapper#registerShutdownHook()
	 */
	@Override
	public void registerShutdownHook() {
		throw new UnsupportedOperationException();
	}
	/**
	 * @see Neo4jGraphDBWrapper#close()
	 */
	@Override
	public void close() throws Exception {
		driver.close();
	}
	/**
	 * @see Neo4jGraphDBWrapper#createIndexes()
	 */
	@Override
	public void createIndexes() {
		
	}
}

