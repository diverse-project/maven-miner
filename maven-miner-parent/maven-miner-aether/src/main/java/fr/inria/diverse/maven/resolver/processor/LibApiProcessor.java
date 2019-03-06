package fr.inria.diverse.maven.resolver.processor;

import fr.inria.diverse.maven.resolver.MetaResolver;
import fr.inria.diverse.maven.resolver.processor.dependencyanalyser.API;
import fr.inria.diverse.maven.resolver.processor.dependencyanalyser.ClassAPIVisitor;
import fr.inria.diverse.maven.resolver.processor.dependencyanalyser.LibraryApi;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;

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
import java.util.zip.ZipEntry;

public class LibApiProcessor extends CollectArtifactProcessor {

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
	public LibApiProcessor() {
		super();
	}
	Connection db;
	public LibApiProcessor(Connection db) {
		this.db = db;
	}

	static String getLibIdQuery = "SELECT id FROM library WHERE coordinates=?;";
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
		try {
			String coordinates = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
			LOGGER.info("Resolving jar file for artifact: {}", artifact);
			ArtifactRequest artifactRequest = new ArtifactRequest();

			PreparedStatement getLibIdQueryStmt = db.prepareStatement(getLibIdQuery);
			getLibIdQueryStmt.setString(1, coordinates);
			ResultSet result = getLibIdQueryStmt.executeQuery();
			int libID;
			if (result.next()) {
				libID = result.getInt("id");
				result.close();
			} else {
				throw new SQLException("Library not found.");
			}

			artifactRequest.setArtifact( artifact );
			artifactRequest.addRepository(repo);
			ArtifactResult artifactResult=null;
			File jarFile=null;

			//artifactResult = system.resolveArtifact(session, artifactRequest);
			//jarFile= artifactResult.getArtifact().getFile();
			jarFile = MetaResolver.resolveJar(artifactRequest);
			if (jarFile == null) {
				throw new NullPointerException();
			}

			processJar(jarFile, libID);
			//doStuff
			resolved++;

		} catch (SecurityException
				| NullPointerException
				| SQLException
				| IOException e) {
			LOGGER.error("Unable to read artifact {}", artifact);
			//e.printStackTrace();
			nonResolved++;
			e.printStackTrace();
		}
		return artifact;
	}

	private void processJar(File jarFileFile, int id) throws IOException, SQLException {
		API api = new API(id,db);

		JarFile jarFile = new JarFile(jarFileFile);
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			String entryName = entry.getName();
			if (entryName.endsWith(".class")) {
				InputStream classFileInputStream = jarFile.getInputStream(entry);
				try {
					ClassReader cr = new ClassReader(classFileInputStream);
					ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					ClassAPIVisitor cv = new ClassAPIVisitor(cw, api);
					cr.accept(cv, 0);
				} finally {
					classFileInputStream.close();
				}
			}
		}
		api.pushToDB(db);
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
