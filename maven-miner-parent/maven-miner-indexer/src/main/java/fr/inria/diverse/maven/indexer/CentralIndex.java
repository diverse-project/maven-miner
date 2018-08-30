package fr.inria.diverse.maven.indexer;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.Bits;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexUtils;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdateResult;
import org.apache.maven.index.updater.IndexUpdater;
import org.apache.maven.index.updater.ResourceFetcher;
import org.apache.maven.index.updater.WagonHelper;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.observers.AbstractTransferListener;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
/**
 * Collection of some use cases.
 */
public class CentralIndex
{	
	//Cli
    private static final Options options = new Options();
    
    //Logger
	private static final Logger LOGGER = LoggerFactory.getLogger(CentralIndex.class);
	
	//default values
    private static final String ARTIFACT_QUEUE_NAME = "artifactsQueue";
    
    private static final String DEFAULT_PATH_VALUE = "allArtifactsInfo"+new Timestamp(System.currentTimeMillis());
    
    private static final int DUMP_LIMIT = 10000;
    
    private static final String SEPARATOR = ":";
    
    private static final String FILTER_FILE_NAME = "filtered.properties";
    
    private static boolean fromFile= false;
    
    // Aether variables
    private final PlexusContainer plexusContainer;

    private final Indexer indexer;

    private final IndexUpdater indexUpdater;

    private final Wagon httpWagon;

    private IndexingContext centralContext;
    
    //AMQ fields
    private static ConnectionFactory factory;
    
    private static Connection connection;
    
	private static Channel channel;

	private static String DEFAULT_USERNAME = "user"; 
	
	// filtered patterns
    private  List<String> patterns;
    
    //Visited artifacts
    private HashSet<String> visitedArtifacts = new HashSet<String>();
    
    public CentralIndex()
        throws PlexusContainerException, ComponentLookupException
    {
        // here we create Plexus container, the Maven default IoC container
        // Plexus falls outside of MI scope, just accept the fact that
        // MI is a Plexus component ;)
        // If needed more info, ask on Maven Users list or Plexus Users list
        // google is your friend!
        final DefaultContainerConfiguration config = new DefaultContainerConfiguration();
        config.setClassPathScanning( PlexusConstants.SCANNING_INDEX );
        this.plexusContainer = new DefaultPlexusContainer( config );

        // lookup the indexer components from plexus
        this.indexer = plexusContainer.lookup( Indexer.class );
        this.indexUpdater = plexusContainer.lookup( IndexUpdater.class );
        // lookup wagon used to remotely fetch index
        this.httpWagon = plexusContainer.lookup( Wagon.class, "http" );
        this.patterns =  getPatternsFromCP();
    }
    /**
     * Transforms
     * @return
     */
    private List<String> getPatternsFromCP() {
//		BufferedReader fileStream = new BufferedReader(
//										new InputStreamReader(CentralIndex.class
//																		  .getClassLoader()
//																		  .getResourceAsStream(FILTER_FILE_NAME)));
//		return fileStream.lines().filter(line -> ! line.startsWith("#")).collect(Collectors.toList());
    	return Arrays.asList(".*-sample.*", ".*:\\d+\\.\\d+\\.\\d+\\.\\d+.*");
	}

	public CentralIndex init()
        throws IOException, ComponentLookupException, InvalidVersionSpecificationException
    {
        // Files where local cache is (if any) and Lucene Index should be located
    	LOGGER.info("Initializing the central index director");
        File centralLocalCache = new File( "target/central-cache" );
        File centralIndexDir = new File( "target/central-index" );

        // Creators we want to use (search for fields it defines)
        List<IndexCreator> indexers = new ArrayList<IndexCreator>();
        indexers.add( plexusContainer.lookup( IndexCreator.class, "min" ) );
        indexers.add( plexusContainer.lookup( IndexCreator.class, "jarContent" ) );
        indexers.add( plexusContainer.lookup( IndexCreator.class, "maven-plugin" ) );

        LOGGER.info("Creating index context");
        // Create context for central repository index
        centralContext =
            indexer.createIndexingContext( "central-context", "central", centralLocalCache, centralIndexDir,
                                           "http://repo1.maven.org/maven2", null, true, true, indexers );

        	
            LOGGER.info( "Updating Index..." );
            LOGGER.info( "This might take a while on first run, so please be patient!" );

            TransferListener listener = new AbstractTransferListener()
            {
                public void transferStarted( TransferEvent transferEvent )
                {
                    LOGGER.info( "  Downloading " + transferEvent.getResource().getName() );
                }

                public void transferProgress( TransferEvent transferEvent, byte[] buffer, int length )
                {
                }

                public void transferCompleted( TransferEvent transferEvent )
                {
                    LOGGER.info( " - Done" );
                }
            };
            ResourceFetcher resourceFetcher = new WagonHelper.WagonFetcher( httpWagon, listener, null, null );

            Date centralContextCurrentTimestamp = centralContext.getTimestamp();
            IndexUpdateRequest updateRequest = new IndexUpdateRequest( centralContext, resourceFetcher );
            IndexUpdateResult updateResult = indexUpdater.fetchAndUpdateIndex( updateRequest );
            if ( updateResult.isFullUpdate() )
            {
                LOGGER.info( "Index has fully updated" );
            }
            else if ( updateResult.getTimestamp().equals( centralContextCurrentTimestamp ) )
            {
                LOGGER.info( "No update needed, index is up to date!" );
            }
            else
            {
                LOGGER.info(
                    "Incremental update happened, change covered " + centralContextCurrentTimestamp + " - "
                        + updateResult.getTimestamp() + " period." );
            }
            return this;
        }

    	
    	
    	private void dumpAtFile(String filename) throws IOException {
            
        	final IndexSearcher searcher = centralContext.acquireIndexSearcher();
        	
            final BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            
            int error = 0;
            int count = 0;
            
            try {
                final IndexReader ir = searcher.getIndexReader();
                
                Bits liveDocs = MultiFields.getLiveDocs( ir );
                
                String texts="";
                int maxDocs = ir.maxDoc();
                ArtifactInfo ai;
                for ( int i = 0; i < maxDocs; i++ )
                {
                    if ( liveDocs == null || liveDocs.get( i ) )
                    {
                    	
                        try {
                        	final Document doc = ir.document( i );
                        	ai= IndexUtils.constructArtifactInfo( doc, centralContext );
                            if (ai == null) {
                            	error++;
                            	continue;      	
                            }
                            
                            String message =  ai.getGroupId() + SEPARATOR  
                            		+ ai.getArtifactId() + SEPARATOR 
                            		+ ai.getVersion();
                            		
                            if (patterns.stream().anyMatch(pattern -> message.matches(pattern))) {
                        		LOGGER.info("{} is skipped",message);
                            } else if (! visitedArtifacts.add(message)) {
                            	LOGGER.info("{} is redundunt",message);
                            } else {
                            	texts += System.getProperty("line.separator") + message;                            	
                            	LOGGER.info("{} is published",message);
                                count++;
                            }

                        } catch (NullPointerException e) {
                        	LOGGER.error("gav with name {} not found");
							LOGGER.error(e.getMessage());
							error++;
						}
                        
                    } 
                    
                    if ((i%DUMP_LIMIT == 0) || 
                    		  (i == maxDocs)) {
                    	bw.write(texts);
                        bw.flush();
                        texts="";
                        LOGGER.info("{} artifacts have been indexed", count);
                    }
                }
            } catch (Exception e){
				LOGGER.error(e.getMessage());
            	error++;
            } finally {
                centralContext.releaseIndexSearcher( searcher );
                bw.close();
            }
            LOGGER.info("{} artifacts have been dumped", count);
            LOGGER.info("{} artifacts have been skipped", error);
        }
        
     private static void help() {

  		  HelpFormatter formater = new HelpFormatter();

  		  formater.printHelp("Maven-indexer", options);

  		  System.exit(0);

  	}
     /**
      * 
      * @param args
      * @throws Exception
      */
     public static void main( String[] args )
    	        throws Exception
    	    {
    	 		final CentralIndex index = new CentralIndex();
    	    	String path = DEFAULT_PATH_VALUE;
    	    	
    			options.addOption(new Option("f", "from-file", true, "File path to retrieve artiacts coordinates list. If not specified, the maven central index is used instead. Note, artifacts are per line and come in the form groupId:artifactId:version. "));
    			options.addOption(new Option("q", "queue", true, "Hostname and port of the RabbitMQ broker. Note, URI comes in the form hostname:port"));
    			options.addOption(new Option("t","to-file",true, "Dumping the index into a file with name allArtifacsInfo. Noe the args \'t\' and \'q\' are mutually exclusive, only one should be provided"));
    		
    			CommandLineParser parser = new DefaultParser();
    			 
    			CommandLine cmd = null;
    			try {
    				   cmd = parser.parse(options, args);
    				   if(cmd.getArgs().length==-1) {
    					   LOGGER.error("No arguments have been provided!");
    					   help();
    				   }
    				  
    				   if (cmd.hasOption("h")) {
    				    help();
    				   }
    				   if (cmd.hasOption("q") && cmd.hasOption("t")) {
    					   LOGGER.error("q and t are mutually exclusive (X-OR). Only one option should be provided");
    					   help();
    				   }
    				   if (cmd.hasOption("q") || cmd.hasOption("f") ) {
    					   factory = new ConnectionFactory();

    		    	      if (cmd.hasOption("q")) {
    		    	    	   String [] values = cmd.getOptionValue("q").split(":");
    		    	    	   //check the presence of the port number 
    		    	    	   if (values.length!=2) {
		    					   LOGGER.error("Couldnt handle hostname \"{}\"",cmd.getOptionValue("q"));
		    					   help();
		    				   }
		    				   factory.setHost(values[0]);
		    				   factory.setPort(Integer.valueOf(values[1]));
		    				   factory.setUsername(DEFAULT_USERNAME );
		    				   factory.setPassword(DEFAULT_USERNAME);
		    				   connection = factory.newConnection();
	    		    	       channel = connection.createChannel();
		    				   channel.queueDeclareNoWait(ARTIFACT_QUEUE_NAME, true, false, false, null);
		    				   channel.queuePurge(ARTIFACT_QUEUE_NAME);

    		    	      }
		    			  if (cmd.hasOption("f")) {
		    				   fromFile = true;
		    				   path = cmd.getOptionValue("f");
		    			  } 
		    			  index.init().publish(path);
    				   } else if (cmd.hasOption("t")) {
    					   path = cmd.getOptionValue("t");
    					   index.init().dumpAtFile(path);
    				   }
    					  
    			} catch (ShutdownSignalException | IOException e) {
    				LOGGER.error("Channel creation error {}", e.getMessage());
    				e.printStackTrace();
    			}
    			  catch (Exception e) {
    				LOGGER.error("Failed to parse comand line properties {}", e.getMessage());
    				e.printStackTrace();
    				help();
    			}
    	    }    			
    	        

	private void publish(String path) throws IOException, TimeoutException {
		if (fromFile) {
			publishFromFile(path);
		} else {
			publishFromIndex(ARTIFACT_QUEUE_NAME);
		}
	}

	
	private void publishFromFile(String filename) throws IOException, TimeoutException {
			BufferedReader resultsReader = null;
    		try {
				resultsReader = new BufferedReader(new FileReader(filename));
	            String artifactCoordinate="";

	            while ((artifactCoordinate = resultsReader.readLine()) != null) {
			            	if (artifactCoordinate.startsWith("#")) continue;
			            	//Dirty workaround
			            	final String message = artifactCoordinate;
			            	if (patterns.stream().anyMatch(pattern -> message.matches(pattern))) {
	                    		LOGGER.info("{} is skipped",message);
	                        } else if (! visitedArtifacts.add(message)) {
	                        	LOGGER.info("{} is redundunt",message);
	                        } else {
	                        	channel.basicPublish("", ARTIFACT_QUEUE_NAME, null, message.getBytes("UTF-8"));	
	                        	LOGGER.info("{} is published",message);
	                        }
	            	}
			   		            
	            } catch (Exception e) {
	            	LOGGER.error("Couldn't read file {}", filename );
	             	e.printStackTrace();
	            } finally {
	            	resultsReader.close();
	            	channel.close();
	                connection.close();
	            }         
    }
		
	private void publishFromIndex(String artifactQueueName) throws IOException, TimeoutException {
		
		
		final IndexSearcher searcher = centralContext.acquireIndexSearcher();
    	
        int error = 0;
        int count = 0;
        
        try {
            final IndexReader ir = searcher.getIndexReader();
            
            Bits liveDocs = MultiFields.getLiveDocs( ir );
            int maxDocs = ir.maxDoc();
            ArtifactInfo ai;
            for ( int i = 0; i < maxDocs; i++ )
            {
                if ( liveDocs == null || liveDocs.get( i ) )
                {
                	
                    try {
                    	final Document doc = ir.document( i );
                    	ai= IndexUtils.constructArtifactInfo( doc, centralContext );
                        if (ai == null) {
                        	error++;
                        	continue;      	
                        }
                        String message = ai.getGroupId() + SEPARATOR  
		                        	   + ai.getArtifactId() + SEPARATOR 
		                        	   + ai.getVersion();
                        
                    	if (patterns.stream().anyMatch(pattern -> message.matches(pattern))) {
                    		LOGGER.info("{} is skipped",message);
                    		error++;
                        } else if (! visitedArtifacts.add(message)) {
                        	LOGGER.info("{} is redundunt",message);
                        	error++;
                        } else {
                        	channel.basicPublish("", artifactQueueName, null, message.getBytes("UTF-8"));
                        	LOGGER.info("{} is published",message);
                            count++;
                        }
                    	
                    } catch (NullPointerException e) {
                    	LOGGER.error("gav with name {} not found");
						LOGGER.error(e.getMessage());
						error++;
					} catch (Exception e) {
						LOGGER.error(e.getMessage());
						throw e;
					}
                }             
            }
        } catch (Exception e){
        	error++;
        } finally {
            centralContext.releaseIndexSearcher( searcher );
            channel.close();
            connection.close();
        }
        LOGGER.info("{} artifacts have been dumped", count);
        LOGGER.info("{} artifacts have been skipped", error);
		
	}
}

