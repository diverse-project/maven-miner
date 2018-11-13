package fr.inria.diverse.maven.resolver.processor.dependencyanalyser;

import fr.inria.diverse.maven.resolver.processor.dependencyanalyser.LibrariesUsage;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

class MethodAdapter extends MethodVisitor implements Opcodes {

    LibrariesUsage lu;

    public MethodAdapter(final MethodVisitor mv, LibrariesUsage lu) {
        super(ASM5, mv);
        this.lu = lu;
    }

    public void processUsage( final String owner, final String name, final String descriptor) {
        String sig = getSignature(name,descriptor);
        lu.insertIfPartOfPackages(owner, sig);
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner, final String name, final String descriptor) {
        processUsage(owner, name, descriptor);
        mv.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, final String descriptor, boolean itf) {
        processUsage(owner, name, descriptor);
        mv.visitMethodInsn(opcode, owner, name, descriptor, itf);
    }

    public static String getSignature(String name, String desc) {
        return name + desc;
    }
}
