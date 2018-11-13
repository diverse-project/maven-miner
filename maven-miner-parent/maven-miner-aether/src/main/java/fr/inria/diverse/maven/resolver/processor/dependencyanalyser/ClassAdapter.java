package fr.inria.diverse.maven.resolver.processor.dependencyanalyser;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ClassAdapter extends ClassVisitor implements Opcodes {

    LibrariesUsage lu;

    public ClassAdapter(final ClassVisitor cv, LibrariesUsage lu) {
        super(ASM5, cv);
        this.lu = lu;
    }

    @Override
    public void visit(
            final int version,
            final int access,
            final String name,
            final String signature,
            final String superName,
            final String[] interfaces) {
        super.visit(version,access,name,signature,superName,interfaces);
        lu.insertIfPartOfPackages(superName,"");
        for(String interfaceName: interfaces) {
            lu.insertIfPartOfPackages(interfaceName, "");
        }
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name,
                                     final String desc, final String signature, final String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        return mv == null ? null : new MethodAdapter(mv, lu);
    }
}
