package org.dimer.compiler;

import org.dimer.SimpleLangBaseVisitor;
import org.dimer.SimpleLangParser;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.V1_8;

public class SimpleLangBytecodeVisitor extends SimpleLangBaseVisitor<Void> {

    private final ClassWriter classWriter;
    private final String className;
    private MethodVisitor currentMethod;

    public SimpleLangBytecodeVisitor(String className) {
        this.className = className;
        this.classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    }

    public byte[] getBytecode() {
        return classWriter.toByteArray();
    }

    @Override
    public Void visitClassDeclaration(SimpleLangParser.ClassDeclarationContext ctx) {
        // Criação da classe com ASM
        classWriter.visit(V1_8, ACC_PUBLIC, className, null, "java/lang/Object", null);

        visit(ctx.varSection()); // Bloco var
        visit(ctx.methodsSection()); // Bloco methods
        visit(ctx.initSection()); // Método init

        classWriter.visitEnd();
        return null;
    }
}