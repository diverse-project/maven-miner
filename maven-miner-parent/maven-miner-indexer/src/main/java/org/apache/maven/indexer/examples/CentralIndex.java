package org.apache.maven.indexer.examples;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Collection of some use cases.
 */
public class CentralIndex
{
	private static final Logger LOGGER = LoggerFactory.getLogger(CentralIndex.class);
	

    public static void main( String[] args )
        throws Exception
    {
        final CentralIndex index = new CentralIndex();
        index.init().dumpAt("allArtifactsInfo"+new Timestamp(System.currentTimeMillis()));
    }


    private final PlexusContainer plexusContainer;

    private final Indexer indexer;

    private final IndexUpdater indexUpdater;

    private final Wagon httpWagon;

    private IndexingContext centralContext;

    private static final int DUMP_LIMIT = 100000;
    
    private static final String SEPARATOR = ":";
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

        	// Update the index (incremental update will happen if this is not 1st run and files are not deleted)
            // This whole block below should not be executed on every app start, but rather controlled by some configuration
            // since this block will always emit at least one HTTP GET. Central indexes are updated once a week, but
            // other index sources might have different index publishing frequency.
            // Preferred frequency is once a week.
            LOGGER.info( "Updating Index..." );
            LOGGER.info( "This might take a while on first run, so please be patient!" );
            // Create ResourceFetcher implementation to be used with IndexUpdateRequest
            // Here, we use Wagon based one as shorthand, but all we need is a ResourceFetcher implementation
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
                LOGGER.info( "Full update happened!" );
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

        public void dumpAt(String filename) throws IOException {
            
        	final IndexSearcher searcher = centralContext.acquireIndexSearcher();
        	
            final BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            
            int error = 0;
            int count = 0;
            
            try
            {
                final IndexReader ir = searcher.getIndexReader();
                Bits liveDocs = MultiFields.getLiveDocs( ir );
                
                String text="";
                
                for ( int i = 0; i < ir.maxDoc(); i++ )
                {
                    if ( liveDocs == null || liveDocs.get( i ) )
                    {
                        final Document doc = ir.document( i );
                        final ArtifactInfo ai = IndexUtils.constructArtifactInfo( doc, centralContext );
                        text =  ai.getGroupId() + SEPARATOR  
                        		+ ai.getArtifactId() + SEPARATOR 
                        		+ ai.getVersion()
                        		+ System.getProperty("line.separator");
                        count++;
                    }
                    
                    if (i%DUMP_LIMIT == 0) {
                    	bw.write(text);
                        bw.flush();
                    }
                }
            } catch (Exception e){
            	error++;
            }
            finally
            {
                centralContext.releaseIndexSearcher( searcher );
                bw.close();
            }
        
        }
}

