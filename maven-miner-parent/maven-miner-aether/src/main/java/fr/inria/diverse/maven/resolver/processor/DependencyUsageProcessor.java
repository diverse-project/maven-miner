package fr.inria.diverse.maven.resolver.processor;

import fr.inria.diverse.maven.resolver.MetaResolver;
import fr.inria.diverse.maven.resolver.db.sql.MariaDBWrapper;
import fr.inria.diverse.maven.resolver.processor.dependencyanalyser.ClassAdapter;
import fr.inria.diverse.maven.resolver.processor.dependencyanalyser.LibrariesUsage;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class DependencyUsageProcessor extends CollectArtifactProcessor {

	private Map<String, Set<String>> libs = new HashMap<>();
	private File outputDir = new File("./output");
	private File unresolvedArtifact = new File("./unresolvedArtifacts.log");
	private File emptyDepUsageArtifact = new File("./emptyDepUsageArtifact.log");
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

	private Connection db;

	/**
	 * default constructor
	 */
	public DependencyUsageProcessor(Connection db) {
		super();
		this.db = db;
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
		String coordinates = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
		try {

			//artifactResult = system.resolveArtifact(session, artifactRequest);
			//jarFile= artifactResult.getArtifact().getFile();
			jarFile = MetaResolver.resolveJar(artifactRequest);
			if (jarFile == null) {
				throw new NullPointerException();
			}
			processJar(jarFile, coordinates);
			//doStuff
			resolved++;

		} catch (SecurityException
				| NullPointerException
				| IOException
				| SQLException e) {
			LOGGER.error("Unable to read artifact {}", artifact);
			e.printStackTrace();
			try {
				FileUtils.write(unresolvedArtifact, coordinates + " | " + e.getClass().getName() + "\n",true);
			} catch (IOException e1) {
				LOGGER.error("Could not log error for artifact {}", artifact);
				e1.printStackTrace();
			}
			nonResolved++;
		}
		return artifact;
	}



	static String getLibrariesPackages = "SELECT p.libraryid, p.package, p.id FROM client as c " +
			"JOIN dependency as d ON c.id=d.clientid " +
			"JOIN package as p ON d.libraryid=p.libraryid " +
			"WHERE c.coordinates=?";

	private void processJar(File jar, String gav) throws SQLException, IOException {
			PreparedStatement getLibrariesPackagesQuery = db.prepareStatement(getLibrariesPackages);
			getLibrariesPackagesQuery.setString(1, gav);

			ResultSet librariesPackagesResult = getLibrariesPackagesQuery.executeQuery();

			Map<Integer, Map<Integer, String>> libs = new HashMap<>();
			while (librariesPackagesResult.next()) {
				Map<Integer, String> packages = libs.computeIfAbsent(librariesPackagesResult.getInt("libraryid"), i -> new HashMap<>());
				packages.put(librariesPackagesResult.getInt("id"), librariesPackagesResult.getString("package"));
			}

			JarFile jarFile = new JarFile(jar);
			Enumeration<JarEntry> entries = jarFile.entries();

			LibrariesUsage lu = new LibrariesUsage(libs);


			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String entryName = entry.getName();
				if (entryName.endsWith(".class")) {
					try (InputStream classFileInputStream = jarFile.getInputStream(entry)) {
						ClassReader cr = new ClassReader(classFileInputStream);
						ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
						ClassVisitor cv = new ClassAdapter(cw, lu);
						cr.accept(cv, 0);
					}
				}
			}

			if(!lu.pushToDB(db, gav)) {
				FileUtils.write(emptyDepUsageArtifact, gav + " | No dep usage\n", true);

			}
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


	public static void main(String[] args) throws Exception {
		String artifactCoordinate = "com.bbossgroups:bboss-persistent:5.0.7.5";
		MariaDBWrapper db = new MariaDBWrapper();
		DependencyUsageProcessor processor = new DependencyUsageProcessor(db.getConnection());

		DefaultArtifact artifact = new DefaultArtifact(artifactCoordinate);

		processor.process(artifact);
		System.out.println("Done");
	}
}