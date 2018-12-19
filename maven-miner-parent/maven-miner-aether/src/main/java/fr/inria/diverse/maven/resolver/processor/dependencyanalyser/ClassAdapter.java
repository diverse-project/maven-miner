package fr.inria.diverse.maven.resolver.processor.dependencyanalyser;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassAdapter extends ClassVisitor implements Opcodes {

    LibrariesUsage lu;
    String className;

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
        className = name;
        lu.insertIfPartOfPackages(superName,"", name);
        for(String interfaceName: interfaces) {
            lu.insertIfPartOfPackages(interfaceName, "", name);
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        lu.insertIfPartOfPackages(extractByteCodeTypeDesc(desc),"", className);

        return super.visitAnnotation(desc, visible);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        lu.insertIfPartOfPackages(extractByteCodeTypeDesc(desc),"", className);

        return super.visitTypeAnnotation(typeRef, typePath, desc, visible);
    }


    public FieldVisitor visitField(int access, String name, String desc,
                                   String signature, Object value) {
        FieldVisitor fv = cv.visitField(access, name, desc, signature, value);

        if(signature != null) {
            SignatureVisitor sv = new SignatureAdapter(lu, className);
            SignatureReader r = new SignatureReader(signature);
            r.accept(sv);
        }

        lu.insertIfPartOfPackages(extractByteCodeTypeDesc(desc),"", className);
        return fv == null ? null : new FieldAdapter(fv, lu, className);
    }


    @Override
    public MethodVisitor visitMethod(final int access, final String name,
                                     final String desc, final String signature, final String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        return mv == null ? null : new MethodAdapter(mv, lu, className);
    }

    public static String extractByteCodeTypeDesc(String raw) {
        if(!raw.contains("L")) return raw;
        return raw.substring(raw.indexOf("L")+1,raw.indexOf(";"));
    }
}
