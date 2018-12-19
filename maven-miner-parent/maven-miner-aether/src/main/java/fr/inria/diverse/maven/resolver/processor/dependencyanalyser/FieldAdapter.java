package fr.inria.diverse.maven.resolver.processor.dependencyanalyser;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

public class FieldAdapter extends FieldVisitor implements Opcodes  {

	LibrariesUsage lu;
	String className;

	public FieldAdapter(final FieldVisitor fv, LibrariesUsage lu, String className) {
		super(ASM5, fv);
		this.lu = lu;
		this.className = className;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		lu.insertIfPartOfPackages(ClassAdapter.extractByteCodeTypeDesc(desc), "", className);
		return super.visitAnnotation(desc, visible);
	}

}
