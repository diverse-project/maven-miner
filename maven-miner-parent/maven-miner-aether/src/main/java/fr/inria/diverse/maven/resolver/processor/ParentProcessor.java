package fr.inria.diverse.maven.resolver.processor;

import fr.inria.diverse.maven.resolver.MetaResolver;
import fr.inria.diverse.maven.resolver.db.Neo4jGraphDBWrapper;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.io.File;
import java.io.IOException;

public class ParentProcessor extends CollectArtifactProcessor {


	/**
	 * A wrapper to perform common operations to store maven dependencies
	 */
	protected Neo4jGraphDBWrapper dbWrapper;

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

	/**
	 * Collects the dependencies of a given artifact,
	 * and resolves the jar in order to count the number of classes
	 * @param artifact {@link Artifact}
	 * @throws DependencyCollectionException
	 * @throws ArtifactResolutionException
	 */
	@Override
	public Artifact process(Artifact artifact) {

		if (artifact == null) return null;
		LOGGER.info("Resolving jar file for artifact: {}", artifact);
		try {

			//artifactResult = system.resolveArtifact(session, artifactRequest);
			//jarFile= artifactResult.getArtifact().getFile();
			//jarFile = MetaResolver.resolveJar(artifactRequest);

			String parentGAV = MetaResolver.deriveParent(artifact);
			if(parentGAV != null) {
				Artifact parentA = new DefaultArtifact(parentGAV);
				dbWrapper.createNodeFromArtifactCoordinate(parentA);
				dbWrapper.createParenthood(artifact, parentA);
			}
		} catch (SecurityException
				| NullPointerException e) {
			LOGGER.error("Unable to read artifact {}", artifact);
		}
		return artifact;
	}
}
