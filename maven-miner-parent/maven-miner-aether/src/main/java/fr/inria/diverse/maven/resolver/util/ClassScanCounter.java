package fr.inria.diverse.maven.resolver.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class ClassScanCounter extends URLClassLoader {

	public static final String CLAZZ = "class";
	public static final String ENUM = "enum";
	public static final String INTERFACE = "interface";
	public static final String ANNOTATION = "annotation";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ClassScanCounter.class);
	public ClassScanCounter(File jarFile) throws MalformedURLException {
		super(new URL[] {jarFile.toURI().toURL()});
	}

	public ListMultimap<String, Class<?>> loadAndScanJar(File jarFile)
	        throws ClassNotFoundException, ZipException, IOException {

	    // Load the jar file into the JVM
	    // You can remove this if the jar file already loaded.
	    super.addURL(jarFile.toURI().toURL());

	    ListMultimap<String, Class<?>> classes = ArrayListMultimap.create();


	    // Count the classes loaded
	    int count = 0;

	    // Your jar file
	    JarFile jar = new JarFile(jarFile);
	    // Getting the files into the jar
	    Enumeration<? extends JarEntry> enumeration = jar.entries();

	    // Iterates into the files in the jar file
	    while (enumeration.hasMoreElements()) {
	        ZipEntry zipEntry = enumeration.nextElement();

	        // Is this a class?
	        if (zipEntry.getName().endsWith(".class")) {

	            // Relative path of file into the jar.
	            String className = zipEntry.getName();
	            // Complete class name
	            className = className.replace(".class", "").replace("/", ".");
	            // Load class definition from JVM
	            Class<?> clazz = this.loadClass(className);

	            try {
	                // Verify the type of the "class"
	                if (clazz.isInterface()) {
	                    classes.put(INTERFACE, clazz);
	                } else if (clazz.isAnnotation()) {
	                	classes.put(INTERFACE, clazz);
	                } else if (clazz.isEnum()) {
	                	classes.put(INTERFACE, clazz);
	                } else {
	                	classes.put(INTERFACE, clazz);
	                }

	                count++;
	            } catch (ClassCastException e) {
	            	LOGGER.error(e.getLocalizedMessage());
	            	throw e;
	            }
	        }
	    }

	    LOGGER.info("{} classes have been found in the artifact {}.", count, jar.getName());

	    return classes;
	}
}
