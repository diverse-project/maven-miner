package fr.inria.diverse.maven.resolver.processor.dependencyanalyser;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

class MethodAdapter extends MethodVisitor implements Opcodes {

    LibrariesUsage lu;
    String className;

    public MethodAdapter(final MethodVisitor mv, LibrariesUsage lu, String className) {
        super(ASM5, mv);
        this.lu = lu;
        this.className = className;
    }

    public void processUsage( final String owner, final String name, final String descriptor) {
        String sig = getSignature(name,descriptor);
        lu.insertIfPartOfPackages(owner, sig, className);
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner, final String name, final String descriptor) {
        processUsage(owner, name, descriptor);
        mv.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        processUsage(ClassAdapter.extractByteCodeTypeDesc(desc), "","");
        return super.visitAnnotation(desc,visible);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, final String descriptor, boolean itf) {
        processUsage(owner, name, descriptor);
        mv.visitMethodInsn(opcode, owner, name, descriptor, itf);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        readSig(desc);
        readSig(bsm.getDesc());
        if(bsm != null) {
            for (Object arg : bsmArgs) {
                if(arg instanceof Handle) {
                    Handle h = (Handle) arg;
                    readSig(h.getDesc());
                } else if(arg instanceof Type) {
                    Type t = (Type) arg;
                    readSig(t.getDescriptor());
                }
            }
        }
    }
    public void readSig(String sig) {
        SignatureVisitor sv = new SignatureAdapter(lu, className);
        SignatureReader r = new SignatureReader(sig);
        r.accept(sv);
    }

    public static String getSignature(String name, String desc) {
        return name + desc;
    }
}
