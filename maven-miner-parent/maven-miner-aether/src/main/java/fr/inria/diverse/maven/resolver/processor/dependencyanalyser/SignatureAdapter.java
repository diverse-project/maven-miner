package fr.inria.diverse.maven.resolver.processor.dependencyanalyser;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureVisitor;

public class SignatureAdapter extends SignatureVisitor implements Opcodes {

	LibrariesUsage lu;
	String className;

	public SignatureAdapter(LibrariesUsage lu, String className) {
		super(ASM5);
		this.lu = lu;
		this.className = className;
	}

	@Override
	public void  visitClassType(String name) {
		super.visitClassType(name);
		lu.insertIfPartOfPackages(name, "", className);
	}
}
