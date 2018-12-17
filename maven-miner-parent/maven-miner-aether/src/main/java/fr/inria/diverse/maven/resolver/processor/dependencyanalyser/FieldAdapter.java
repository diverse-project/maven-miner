package fr.inria.diverse.maven.resolver.processor.dependencyanalyser;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

public class FieldAdapter extends FieldVisitor implements Opcodes  {

	LibrariesUsage lu;

	public FieldAdapter(final FieldVisitor fv, LibrariesUsage lu) {
		super(ASM5, fv);
		this.lu = lu;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		lu.insertIfPartOfPackages(ClassAdapter.extractByteCodeTypeDesc(desc), "");
		return super.visitAnnotation(desc, visible);
	}

}
