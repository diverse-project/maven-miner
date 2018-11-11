package fr.inria.diverse.maven.resolver.processor;

import fr.inria.diverse.maven.resolver.MetaResolver;
import org.apache.commons.io.FileUtils;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public class DependencyUsageProcessor extends CollectArtifactProcessor {

	private Map<String, Set<String>> libs = new HashMap<>();
	private File outputDir = new File("./output");
	/**
	 * used for reporting
	 */
	private int resolved=0;
	/**
	 * used or reporting
	 */
	private int nonResolved=0;
	/**
	 * a class counter
	 */
	private ClassScanCounter counter;
	/**
	 * default constructor
	 */
	public DependencyUsageProcessor() {
		super();
	}
	/**
	 * Constructor
	 * @param visitor
	 */
	public DependencyUsageProcessor(MultiTaskDependencyVisitor visitor) {
		super(visitor);
	}
	/**
	 * Constructor
	 * @param visitor
	 * @param counter
	 */
	public DependencyUsageProcessor (MultiTaskDependencyVisitor visitor, ClassScanCounter counter) {
		super(visitor);
		this.counter = counter;
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
		ArtifactRequest artifactRequest = new ArtifactRequest();


		artifactRequest.setArtifact( artifact );
		artifactRequest.addRepository(repo);
		ArtifactResult artifactResult=null;
		File jarFile=null;
		try {

			//artifactResult = system.resolveArtifact(session, artifactRequest);
			//jarFile= artifactResult.getArtifact().getFile();
			jarFile = MetaResolver.resolveJar(artifactRequest);
			if (jarFile == null) {
				throw new NullPointerException();
			}
			processJar(jarFile, artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion());
			//doStuff
			resolved++;

		} catch (SecurityException
				| NullPointerException
				| IOException e) {
			LOGGER.error("Unable to read artifact {}", artifact);
			//e.printStackTrace();
			nonResolved++;
		}
		return artifact;
	}

	private void processJar(File jarFile, String gav) throws IOException {
		Set<String> packages = new HashSet<>();
		JarFile jar = new JarFile(jarFile);

		//TODO

	}

	@Override
	public void report() {
		if(!outputDir.exists()) outputDir.mkdirs();
		for(String gav: libs.keySet()) {
			File gavFile = new File(outputDir, gav);
			try {
				FileUtils.write(gavFile, libs.get(gav).stream().collect(Collectors.joining("\n")));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		LOGGER.info("{} artifacts jar have been resolved", resolved);
		LOGGER.info("{} artifacts jar gave failed resolution", nonResolved);
		//Report
	}
}