package org.dimer.compiler;

import org.dimer.SimpleLangBaseVisitor;
import org.dimer.SimpleLangParser;
import org.dimer.compiler.data.Variable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.util.LinkedList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class SimpleLangBytecodeVisitor extends SimpleLangBaseVisitor<Void> {

    private final ClassWriter classWriter;
    private final String className;
    private MethodVisitor currentMethod;
    private final List<Variable> classVariables = new LinkedList<>();

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

    @Override
    public Void visitVarDeclaration(SimpleLangParser.VarDeclarationContext ctx) {
        String varName = ctx.IDENTIFIER().getText();
        String varType = ctx.type().getText();
        String descriptor = typeToDescriptor(varType);

        if (currentMethod == null) { // Significa que é variável da classe
            classWriter.visitField(ACC_PRIVATE, varName, descriptor, null, null).visitEnd();

            var value  = ctx.expression() != null ? getLiteralValue(ctx.expression().literal()) : null;
            classVariables.add(new Variable(varName, varType, value));
        } else {
            // TODO variável local
        }

        return null;
    }

    @Override
    public Void visitInitSection(SimpleLangParser.InitSectionContext ctx) {
        currentMethod = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        currentMethod.visitCode();

        currentMethod.visitVarInsn(ALOAD, 0);
        currentMethod.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

        // Inicializa os valores das variáveis de classe caso existam
        for (Variable classVariable : classVariables) {
            if (classVariable.getValue() == null) {
                continue;
            }

            currentMethod.visitVarInsn(ALOAD, 0); // Carrega o this
            currentMethod.visitLdcInsn(classVariable.getValue()); // Load do valor
            currentMethod.visitFieldInsn(PUTFIELD, className, classVariable.getName(), typeToDescriptor(classVariable.getType()));
        }

        currentMethod.visitInsn(RETURN);
        currentMethod.visitMaxs(0, 0); // Será calculado automaticamente pelo ASM
        currentMethod.visitEnd();
        currentMethod = null;

        return null;
    }

    private String typeToDescriptor(String type) {
        return switch (type) {
            case "int" -> "I";
            case "float" -> "F";
            case "string" -> "Ljava/lang/String;";
            case "void" -> "V";
            default -> throw new IllegalArgumentException("Tipo desconhecido: " + type);
        };
    }

    private Object getLiteralValue(SimpleLangParser.LiteralContext ctx) {
        if (ctx == null) {
            return null;
        }

        if (ctx.INT() != null) {
            return Integer.parseInt(ctx.INT().getText());
        } else if (ctx.FLOAT() != null) {
            return Float.parseFloat(ctx.FLOAT().getText());
        } else if (ctx.STRING() != null) {
            return ctx.STRING().getText().substring(1, ctx.STRING().getText().length() - 1);
        } else {
            throw new IllegalArgumentException("Literal desconhecido: " + ctx.getText());
        }
    }
}