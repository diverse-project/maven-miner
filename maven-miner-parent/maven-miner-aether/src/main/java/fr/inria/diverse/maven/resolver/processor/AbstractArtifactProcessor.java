package fr.inria.diverse.maven.resolver.processor;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.RemoteRepository;

import fr.inria.diverse.maven.resolver.Booter;

public abstract class AbstractArtifactProcessor {
	/**
	 * Logger
	 */
	protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractArtifactProcessor.class);
	/**
	 * Maven central common properties
	 */
    protected static final RepositorySystem system = Booter.newRepositorySystem();
    protected static final RemoteRepository repo = Booter.newCentralRepository();
    protected static final RepositorySystemSession session = Booter.newRepositorySystemSession(system);
    
	/**
	 * Processing the Artifact
	 * @param artifact {@link Artifact}
	 */
	public abstract Artifact process(@NotNull Artifact artifact);
	
	/**
	 * Reporting on the processing 
	 */
	public abstract void report();
}
