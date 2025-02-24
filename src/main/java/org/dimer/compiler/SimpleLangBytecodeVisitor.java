package org.dimer.compiler;

import org.antlr.v4.runtime.ParserRuleContext;
import org.dimer.SimpleLangBaseVisitor;
import org.dimer.SimpleLangParser;
import org.dimer.compiler.data.Variable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class SimpleLangBytecodeVisitor extends SimpleLangBaseVisitor<Void> {

    private final ClassWriter classWriter;
    private final String className;
    private MethodVisitor currentMethod;
    private final Map<String, Variable> classVariables = new HashMap<>();

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

            // Adiciona a lista de variáveis de classe para ter seu valor preenchido no bloco do construtor
            var value  = ctx.expression() != null ? getLiteralValue(ctx.expression().literal()) : null;
            classVariables.put(varName, new Variable(varName, varType, value));
        } else {
            // TODO variável local
        }

        return null;
    }

    /**
     * Cria o construtor da classe a partir do bloco 'init' do programa.
     * Nesse bloco também são imputados os valores das variáveis da classe (do bloco var)
     */
    @Override
    public Void visitInitSection(SimpleLangParser.InitSectionContext ctx) {
        currentMethod = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        currentMethod.visitCode();

        currentMethod.visitVarInsn(ALOAD, 0);
        currentMethod.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

        // Inicializa os valores das variáveis de classe caso existam
        for (Variable classVariable : classVariables.values()) {
            if (classVariable.value() == null) {
                continue;
            }

            currentMethod.visitVarInsn(ALOAD, 0); // Carrega o this
            currentMethod.visitLdcInsn(classVariable.value()); // Load do valor
            currentMethod.visitFieldInsn(PUTFIELD, className, classVariable.name(), typeToDescriptor(classVariable.type()));
        }

        // Passa por todos os comandos do bloco init
        for (SimpleLangParser.StatementContext statementContext : ctx.statement()) {
            visit(statementContext);
        }

        currentMethod.visitInsn(RETURN);
        currentMethod.visitMaxs(0, 0); // Será calculado automaticamente pelo ASM
        currentMethod.visitEnd();
        currentMethod = null;

        return null;
    }

    @Override
    public Void visitStatement(SimpleLangParser.StatementContext ctx) {
        if (currentMethod == null) {
            throw new IllegalStateException(String.format("Erro ao processar uma declaração na linha %d: Statement %s sem estar dentro de um método", ctx.start.getLine(), ctx.getText()));
        }

        try {
            if (ctx.getText().startsWith("print")) {
                executePrint(ctx);
            } else if (ctx.getText().startsWith("return")) {
                visit(ctx.expression());
                currentMethod.visitInsn(IRETURN);
            } else {
                super.visitStatement(ctx);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Erro ao processar uma declaração na linha %d: %s", ctx.start.getLine(), e.getMessage())
            );
        }

        return null;
    }

    @Override
    public Void visitExpression(SimpleLangParser.ExpressionContext ctx) {
        if (ctx.literal() != null) {
            currentMethod.visitLdcInsn(getLiteralValue(ctx.literal()));
        } else if (ctx.IDENTIFIER() != null) { // Aponta para uma variável
            String varName = ctx.IDENTIFIER().getText();
            currentMethod.visitVarInsn(ALOAD, 0); // Carrega 'this'
            currentMethod.visitFieldInsn(GETFIELD, className, varName, determineDescriptor(varName, ctx)); // Pega o atributo
        } else if (ctx.stringConcatenation() != null) {
            executeStringConcatenation(ctx.stringConcatenation());
        } else {
            super.visitExpression(ctx); // Processa demais expressões normalmente
        }
        return null;
    }

    /**
     * Método para compilar a concatenação de strings usando o StringBuilder.
     * Funciona apenas se a concatenação começar obrigatoriamente com um string literal.
     */
    private void executeStringConcatenation(SimpleLangParser.StringConcatenationContext ctx) {
        currentMethod.visitTypeInsn(NEW, "java/lang/StringBuilder");
        currentMethod.visitInsn(DUP); // Duplica a referência no topo da pilha para usá-la duas vezes (problema de referência única)
        currentMethod.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);

        for (int i = 0; i < ctx.getChildCount(); i++) {
            var child = ctx.getChild(i);

            if (child.getText().equals("+")) {
                continue;
            }

            if (child instanceof SimpleLangParser.LiteralContext literalContext) {
                currentMethod.visitLdcInsn(getLiteralValue(literalContext));
            } else if (child instanceof SimpleLangParser.ExpressionContext expressionContext) {
                visit(expressionContext);
            }

            currentMethod.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        }

        currentMethod.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
    }

    private void executePrint(SimpleLangParser.StatementContext ctx) {
        currentMethod.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        visit(ctx.expression());
        currentMethod.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }

    private String determineDescriptor(String varName, ParserRuleContext ctx) {
        var variable = classVariables.get(varName);

        if (variable == null) {
            throw new IllegalArgumentException(String.format("Linha %d: Variável %s não encontrada", ctx.start.getLine(), varName));
        }

        return typeToDescriptor(variable.type());
    }

    /**
     * Converte a tipagem da linguagem para a tipagem da JVM
     */
    private String typeToDescriptor(String type) {
        return switch (type) {
            case "int" -> "I";
            case "float" -> "F";
            case "string" -> "Ljava/lang/String;";
            case "void" -> "V";
            default -> throw new IllegalArgumentException("Tipo desconhecido: " + type);
        };
    }

    /**
     * Retorna o valor convertido para Java de uma expressão literal.
     * Ex: int valor = 25 -> retorna int 25
     */
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