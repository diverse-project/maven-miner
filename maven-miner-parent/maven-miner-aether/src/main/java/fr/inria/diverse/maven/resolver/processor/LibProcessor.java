package fr.inria.diverse.maven.resolver.processor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import fr.inria.diverse.maven.resolver.MetaResolver;
import net.lingala.zip4j.core.ZipFile;
import org.apache.commons.io.FileUtils;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;

public class LibProcessor extends CollectArtifactProcessor {

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
	public LibProcessor() {
		super();
	}
	/**
	 * Constructor
	 * @param visitor
	 */
	public LibProcessor(MultiTaskDependencyVisitor visitor) {
		super(visitor);
	}
	/**
	 * Constructor
	 * @param visitor
	 * @param counter
	 */
	public LibProcessor (MultiTaskDependencyVisitor visitor, ClassScanCounter counter) {
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
		//Getting the files into the jar
		Enumeration<? extends JarEntry> enumeration = jar.entries();
		while (enumeration.hasMoreElements()) {
			Class<?> clazz = null;
			ZipEntry zipEntry = null;
			//try {
			zipEntry = enumeration.nextElement();
			// Relative path of file into the jar.
			String className = zipEntry.getName();
			if (!className.endsWith(".class") ||
					className.contains("/test/")) {
				continue;
			}
			String packageName = className.replaceFirst("/[^/]+\\.class", "");
			packages.add(packageName);
			/*} catch () {

			}*/
		}
		libs.put(gav, packages);

	}

	private void processAar(File jarFile, String gav) throws IOException, net.lingala.zip4j.exception.ZipException {
		JarFile aar = new JarFile(jarFile);
		ZipFile zip = new ZipFile(jarFile);

		zip.extractFile("classes.jar",gav + "-classes");
		processJar(new File(gav + "-classes/classes.jar"), gav);
	}

	public static void main(String[] args) throws IOException, net.lingala.zip4j.exception.ZipException {
		File aar = new File("/home/nharrand/Downloads/appcompat-v7-18.0.0.aar");
		LibProcessor p = new LibProcessor();
		p.processAar(aar, "com.android.support:appcompat-v7:18.0.0");
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
