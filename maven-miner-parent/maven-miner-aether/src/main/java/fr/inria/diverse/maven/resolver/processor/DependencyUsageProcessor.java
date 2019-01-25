package fr.inria.diverse.maven.resolver.processor;

import com.rabbitmq.client.Channel;
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
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class DependencyUsageProcessor extends CollectArtifactProcessor {

	private Map<String, Set<String>> libs = new HashMap<>();
	private File outputDir = new File("./output");
	private File unresolvedArtifact = new File("./unresolvedArtifacts.log");
	private File emptyDepUsageArtifact = new File("./emptyDepUsageArtifact.log");
	private File timeLog = new File("./DepUsage-time.log");
	private File networkError = new File("./network.error");
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

	private MariaDBWrapper db;

	private boolean publishResultOnQueue;
	private boolean debug = false;
	private Channel channel;

	private SimpleDateFormat formatter;

	/**
	 * default constructor
	 */
	public DependencyUsageProcessor(MariaDBWrapper db) {
		super();
		this.db = db;
		formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		publishResultOnQueue = false;
	}
	public DependencyUsageProcessor(MariaDBWrapper db, boolean debug) {
		super();
		this.db = db;
		formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		publishResultOnQueue = false;
		this.debug = debug;
	}
	public DependencyUsageProcessor(MariaDBWrapper db, Channel channel) {
		super();
		this.db = db;
		this.channel = channel;
		this.publishResultOnQueue = true;
		formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
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
			FileUtils.write(timeLog, formatter.format(new Date()) + " | " + coordinates + " | Start\n",true);

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
				| IOException e) {
			LOGGER.error("Unable to read artifact {}", artifact);

			e.printStackTrace();
			try {
				FileUtils.write(unresolvedArtifact, coordinates + " | " + e.getClass().getName() + "\n",true);
				FileUtils.write(timeLog, formatter.format(new Date()) + " | " + coordinates + " | Error | " + e.getClass().getName() + "\n",true);
			} catch (IOException e1) {
				LOGGER.error("Could not log error for artifact {}", artifact);
				e1.printStackTrace();
			}
			nonResolved++;
		} catch (SQLException e) {
			LOGGER.error("Unable to read artifact {}", artifact);

			e.printStackTrace();
			try {
				FileUtils.write(unresolvedArtifact, coordinates + " | " + e.getClass().getName() + "\n",true);
				FileUtils.write(timeLog, formatter.format(new Date()) + " | " + coordinates + " | Error | " + e.getClass().getName() + "\n",true);
			} catch (IOException e1) {
				LOGGER.error("Could not log error for artifact {}", artifact);
				e1.printStackTrace();
			}
			try {
				db.reset();
				FileUtils.write(networkError, formatter.format(new Date()) + " | Connection reset failed" + "\n",true);
			} catch (SQLException | IOException | InterruptedException e1) {
				e1.printStackTrace();
				System.exit(-1);
			}

		}
		return artifact;
	}



	static String getLibrariesPackages = "SELECT p.libraryid, p.package, p.id FROM client as c " +
			"JOIN dependency as d ON c.id=d.clientid " +
			"JOIN package as p ON d.libraryid=p.libraryid " +
			"WHERE c.coordinates=?";

	private void processJar(File jar, String gav) throws SQLException, IOException {
		FileUtils.write(timeLog, formatter.format(new Date()) + " | " + gav + " | Retrieve data\n",true);



		PreparedStatement getLibrariesPackagesQuery = db.getConnection().prepareStatement(getLibrariesPackages);
		getLibrariesPackagesQuery.setString(1, gav);

		ResultSet librariesPackagesResult = getLibrariesPackagesQuery.executeQuery();
		getLibrariesPackagesQuery.close();

		Map<Integer, Map<Integer, String>> libs = new HashMap<>();
		while (librariesPackagesResult.next()) {
			Map<Integer, String> packages = libs.computeIfAbsent(librariesPackagesResult.getInt("libraryid"), i -> new HashMap<>());
			packages.put(librariesPackagesResult.getInt("id"), librariesPackagesResult.getString("package"));
		}

		JarFile jarFile = new JarFile(jar);
		Enumeration<JarEntry> entries = jarFile.entries();

		LibrariesUsage lu = new LibrariesUsage(libs);
		FileUtils.write(timeLog, formatter.format(new Date()) + " | " + gav + " | Analyze\n",true);


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
		FileUtils.write(timeLog, formatter.format(new Date()) + " | " + gav + " | Push data\n",true);

		boolean notEmpty = false;
		if(!debug) {
			if (publishResultOnQueue) {
				notEmpty = lu.pushToQueue(db.getConnection(), channel, gav);
			} else {
				notEmpty = lu.pushToDB(db.getConnection(), gav);
			}
		}
		if(!notEmpty) {
			FileUtils.write(emptyDepUsageArtifact, gav + " | No dep usage\n", true);
		}
		FileUtils.write(timeLog, formatter.format(new Date()) + " | " + gav + " | " + lu.getQuerySize() + " Done\n",true);
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
		DependencyUsageProcessor processor = new DependencyUsageProcessor(db);

		DefaultArtifact artifact = new DefaultArtifact(artifactCoordinate);

		processor.process(artifact);
		System.out.println("Done");
	}
}