package fr.inria.diverse.maven.resolver.processor;

import fr.inria.diverse.maven.model.Edge;
import fr.inria.diverse.maven.resolver.db.Neo4jGraphDBWrapper;
import fr.inria.diverse.maven.util.MavenMinerUtil;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.resolution.ArtifactDescriptorException;
import org.sonatype.aether.resolution.ArtifactDescriptorRequest;
import org.sonatype.aether.resolution.ArtifactDescriptorResult;


public class GraphBuilderProcessor extends AbstractArtifactProcessor {
	static boolean doubleCheckLeaves = true;

	protected Neo4jGraphDBWrapper dbWrapper;


	public GraphBuilderProcessor(Neo4jGraphDBWrapper dbWrapper) {
		this.dbWrapper = dbWrapper;
	}

	protected int skipped = 0;

	protected int collected = 0;

	@Override
	public Artifact process(Artifact artifact) {

		LOGGER.info("Collecting dependencies for artifact: " + artifact);

		ArtifactDescriptorRequest r = new ArtifactDescriptorRequest();
		r.setArtifact(artifact);
		r.addRepository(repo);
		try {
			ArtifactDescriptorResult result = system.readArtifactDescriptor(session,r);

			if(doubleCheckLeaves && result.getDependencies().size() == 0) return result.getArtifact();


			dbWrapper.createNodeFromArtifactCoordinate( result.getArtifact());


			for (Dependency dependency : result.getDependencies()) {
				dbWrapper.createNodeFromArtifactCoordinate(dependency.getArtifact());
				Edge.Scope scope = MavenMinerUtil.deriveScope(dependency);
				dbWrapper.addDependency(result.getArtifact(), dependency.getArtifact(), scope);
			}


			collected++;
			return result.getArtifact();
		} catch (ArtifactDescriptorException e) {
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

	/**
	 *
	 * @return dbWrapper {@link Neo4jGraphDBWrapper}
	 */
	public Neo4jGraphDBWrapper getDbWrapper() {
		return dbWrapper;
	}
	/**
	 *
	 * @param dbWrapper {@link Neo4jGraphDBWrapper}
	 */
	public void setDbWrapper(Neo4jGraphDBWrapper dbWrapper) {
		this.dbWrapper = dbWrapper;
	}

}
