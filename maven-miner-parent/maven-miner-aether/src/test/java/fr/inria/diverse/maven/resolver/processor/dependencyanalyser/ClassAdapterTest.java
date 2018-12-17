package fr.inria.diverse.maven.resolver.processor.dependencyanalyser;

import org.junit.Ignore;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassAdapterTest {

	@Ignore
	@Test
	public void test() throws IOException {
		Map<Integer, Set<String>> libs = new HashMap<>();
		Set<String> packages = new HashSet<>();
		packages.add("se/kth/castor/types");
		libs.put(0,packages);
		String jar = "/home/nharrand/Documents/tmp/dep-analyzer/src/test/resources/uselib/target/uselib-1.0-SNAPSHOT.jar";

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

		System.out.println("Done.");
	}

}