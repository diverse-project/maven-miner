package fr.inria.diverse.maven.resolver.processor;

import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.CollectResult;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.resolution.ArtifactResolutionException;

public class CollectArtifactProcessor extends AbstractArtifactProcessor {

	protected  MultiTaskDependencyVisitor visitor = new MultiTaskDependencyVisitor();
	
	protected int skipped = 0;
	
	protected int collected = 0;
		
	public  CollectArtifactProcessor() {
		super();	
	}
	public CollectArtifactProcessor(MultiTaskDependencyVisitor visitor) {
		super();
		this.visitor = visitor;
	}
    /** 
     * Collects the dependencies of a given artifact, and  
     * @param artifact {@link Artifact}
     * @throws DependencyCollectionException
     * @throws ArtifactResolutionException
     */
	@Override
	public Artifact process(Artifact artifact) {
		LOGGER.info("Collecting dependencies for artifact: " + artifact);
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, "")); 
        collectRequest.addRepository(repo);
        CollectResult collectResult;
		try {

			collectResult = system.collectDependencies(session, collectRequest);
			//collectResult.getRoot().getData();
			collectResult.getRoot().accept(visitor);
			collected++;
			// in case the artifact was moved, this assignment ensures we're dealing with the new one
			
			return collectResult.getRoot().getDependency().getArtifact();
		} catch (Exception e) {
			LOGGER.error("Unable to collect dependency for artifact {}", artifact);
			skipped++;
			e.printStackTrace();		
		}
		return null;
	}
	
	@Override
	public void report() {
		LOGGER.info("{} artifacts have been collected", collected);
		LOGGER.info("{} artifacts have failed collection", skipped);
		
	}

}
