package fr.inria.diverse.maven.resolver.processor.dependencyanalyser;

import org.objectweb.asm.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassAPIVisitor extends ClassVisitor implements Opcodes {

	public LibraryApi api;

	public ClassAPIVisitor(ClassVisitor cv, LibraryApi api) {
		super(ASM5, cv);
		this.api = api;
	}

	String owner;

	@Override
	public void visit(
			final int version,
			final int access,
			final String name,
			final String signature,
			final String superName,
			final String[] interfaces) {
		super.visit(version,access,name,signature,superName,interfaces);
		if(Modifier.isPublic(access)) {
			api.insert(name,"", true);
			//System.out.println("[C] " + getSignature(name, "NULL"));
		}
		owner = name;
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name,
	                                 final String desc, final String signature, final String[] exceptions) {
		if(Modifier.isPublic(access) || Modifier.isProtected(access)) {
			api.insert(owner, getSignature(name, desc), Modifier.isPublic(access));
			//System.out.println("[M] " + getSignature(name, desc));
		}
		return cv.visitMethod(access, name, desc, signature, exceptions);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc,
	                               String signature, Object value) {
		if(Modifier.isPublic(access) || Modifier.isProtected(access)) {
			api.insert(owner, getSignature(name, desc), Modifier.isPublic(access));
			//System.out.println("[F] " + getSignature(name, desc));
		}
		return cv.visitField(access, name, desc, signature, value);
	}

	public static String getSignature(String name, String desc) {
		return name + desc;
	}

	public static void main(String[] args) throws IOException {
		String pathToJar = "/home/nharrand/Documents/tmp/dep-analyzer/src/test/resources/toylib/target/toy-lib-1.0-SNAPSHOT.jar";

		LibraryApi toyApi = new LibraryApi(0);

		JarFile jarFile = new JarFile(new File(pathToJar));
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			String entryName = entry.getName();
			if (entryName.endsWith(".class")) {
				InputStream classFileInputStream = jarFile.getInputStream(entry);
				try {
					ClassReader cr = new ClassReader(classFileInputStream);
					ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					ClassAPIVisitor cv = new ClassAPIVisitor(cw,toyApi);
					cr.accept(cv, 0);
				} finally {
					classFileInputStream.close();
				}
			}
		}
		System.out.println("Done");
	}
}
