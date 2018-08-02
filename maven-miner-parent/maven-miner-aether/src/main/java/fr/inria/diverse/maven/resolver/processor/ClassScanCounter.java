package fr.inria.diverse.maven.resolver.processor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.artifact.Artifact;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import fr.inria.diverse.maven.resolver.db.Neo4jGraphDBWrapper;
import fr.inria.diverse.maven.resolver.model.JarCounter;
import fr.inria.diverse.maven.resolver.util.MavenResolverUtil;

public class ClassScanCounter extends URLClassLoader {
	/**
	 * class properties
	 */
	private static final String CLAZZ = "class";
	private static final String ENUM = "enum";
	private static final String INTERFACE = "interface";
	private static final String ANNOTATION = "annotation";
	@SuppressWarnings("unused")
	private static final String METHODS = "methods";
	/**
	 * Local logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ClassScanCounter.class);
	/**
	 * the GraphDb Operations Wrapper
	 */
	private Neo4jGraphDBWrapper dbwrapper;
	
	/**
	 * return the value of the dbwrapper
	 * @return dbwrapper {@link Neo4jGraphDBWrapper}
	 */
	public Neo4jGraphDBWrapper getDbwrapper() {
		return dbwrapper;
	}
	
	/**
	 * set the dbWrapper field
	 * @param dbwrapper {@link Neo4jGraphDBWrapper}
	 */
	public void setDbwrapper(Neo4jGraphDBWrapper dbwrapper) {
		this.dbwrapper = dbwrapper;
	}

	public ClassScanCounter(){
		super(new URL[] {MavenResolverUtil.dummyURL()});
	}
	
	public void loadJarAndStoreCount(Artifact artifact) throws MalformedURLException, ZipException, IOException {
		
		File jarFile = artifact.getFile();
		super.addURL(jarFile.toURI().toURL());
		
		int methodCount= 0;
		int classCount= 0;
		int interfaceCount= 0;
		int enumCount= 0;
		int annotationCount= 0;

		@SuppressWarnings("resource")
		JarFile jar = new JarFile(jarFile);
		//Getting the files into the jar
		Enumeration<? extends JarEntry> enumeration = jar.entries();
		// Iterates into the files in the jar file
		while (enumeration.hasMoreElements()) {
		   try { ZipEntry zipEntry = enumeration.nextElement();	    
		    // Is this a class?
		    if (zipEntry.getName().endsWith(".class")) 
		    	classCount++;
		   } catch (Exception e) {
			   LOGGER.error(e.getMessage());
			   e.printStackTrace();
			   throw e;
		   }
		}
		dbwrapper.updateClassCount(artifact, classCount);
		    
//		    {
//		    	Class<?> clazz = null;
//		    	 try {
//		            // Relative path of file into the jar.
//		            String className = zipEntry.getName();
//		            // Complete class name
//		            className = className.replace(".class", "").replace("/", ".");
//		            // Load class definition from JVM
//		            
//		           clazz = this.loadClass(className);
//		            methodCount += clazz.getMethods().length;
//		           
//		                // Verify the type of the "class"
//		                if (clazz.isInterface()) {
//		                    interfaceCount ++;
//		                } else if (clazz.isAnnotation()) {
//		                	annotationCount++;
//		                } else if (clazz.isEnum()) {
//		                	enumCount++;
//		                } else {
//		                	classCount++;
//		                }
//		                
//		            } catch (ClassCastException e) {
//		            	LOGGER.error("ClassCastException ");
//		            	e.printStackTrace();
//		            } catch (NullPointerException e) {
//		            	LOGGER.error("Class for zipEntry {} return null", zipEntry.getName());
//		            } catch (ClassNotFoundException e){
//		            	LOGGER.error("Unable to find class {}", zipEntry.getName());
//		            	e.printStackTrace();
//		            } catch (NoClassDefFoundError e) {
//		            	LOGGER.error("Unable to find Class {}", zipEntry.getName());
//		            } catch (SecurityException e) {
//						LOGGER.error(e.getMessage());
//						e.printStackTrace();
//					}
//		        }
//		    }
//		dbwrapper.updateDependencyCounts(MavenResolverUtil.artifactToCoordinate(artifact),
//										 new JarCounter(methodCount, 
//														classCount, 
//														interfaceCount, 
//														enumCount, 
//														annotationCount));
			
	}
	//TODO add the storing capability in dbWrapper
	public void loadJarAndStoreContent(File jarFile)
	        throws ClassNotFoundException, ZipException, IOException {

	    // Load the jar file into the JVM
	    // You can remove this if the jar file already loaded.
	    super.addURL(jarFile.toURI().toURL());

	    ListMultimap<String, Class<?>> content = ArrayListMultimap.create();

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
	                    content.put(INTERFACE, clazz);
	                } else if (clazz.isAnnotation()) {
	                	content.put(ANNOTATION, clazz);
	                } else if (clazz.isEnum()) {
	                	content.put(ENUM, clazz);
	                } else {
	                	content.put(CLAZZ, clazz);
	                }
	                count++;
	            } catch (ClassCastException e) {
	            	LOGGER.error(e.getLocalizedMessage());
	            	throw e;
	            }
	        }
	    }
	    
	    jar.close();
	    LOGGER.debug("{} classes have been found in the artifact {}.", count, jar.getName());

	}
}
