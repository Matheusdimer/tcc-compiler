package org.dimer.compiler;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.dimer.SimpleLangBaseVisitor;
import org.dimer.SimpleLangParser;
import org.dimer.compiler.data.Variable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static org.objectweb.asm.Opcodes.*;

public class SimpleLangBytecodeVisitor extends SimpleLangBaseVisitor<Void> {

    private final ClassWriter classWriter;
    private final String className;
    private MethodVisitor currentMethod;
    private final Map<String, Variable> classVariables = new HashMap<>();
    private boolean isFloatOperation = false;
    private final Stack<Integer> numericExpressionStack = new Stack<>();

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
                    String.format("Erro ao processar uma declaração na linha %d: %s", ctx.start.getLine(), e.getMessage()),
                    e
            );
        }

        return null;
    }

    @Override
    public Void visitExpression(SimpleLangParser.ExpressionContext ctx) {
        if (ctx.literal() != null) {
            currentMethod.visitLdcInsn(getLiteralValue(ctx.literal()));
        } else if (ctx.IDENTIFIER() != null) { // Aponta para uma variável
            loadVariable(ctx, ctx.IDENTIFIER().getText());
        } else if (ctx.stringConcatenation() != null) {
            executeStringConcatenation(ctx.stringConcatenation());
        } else {
            super.visitExpression(ctx); // Processa demais expressões normalmente
        }
        return null;
    }

    @Override
    public Void visitNumericExpression(SimpleLangParser.NumericExpressionContext ctx) {
        // Gambiarra para caso alguma expressão pai tiver float, não sobrescrever o valor
        if (numericExpressionStack.isEmpty() || !isFloatOperation) {
            isFloatOperation = isFloatOperation(ctx);
        }

        numericExpressionStack.push(0);

        visit(ctx.getChild(0));

        for (int i = 1; i < ctx.getChildCount(); i += 2) {
            visit(ctx.getChild(i + 1));
            String operator = ctx.getChild(i).getText();
            switch (operator) {
                case "+":
                    currentMethod.visitInsn(isFloatOperation ? FADD : IADD);
                    break;
                case "-":
                    currentMethod.visitInsn(isFloatOperation ? FSUB : ISUB);
                    break;
                case "*":
                    currentMethod.visitInsn(isFloatOperation ? FMUL : IMUL);
                    break;
                case "/":
                    currentMethod.visitInsn(isFloatOperation ? FDIV : IDIV);
                    break;
                default:
                    throw new IllegalArgumentException("Operador não suportado: " + operator);
            }
        }

        return null;
    }

    @Override
    public Void visitInvolvedNumericExpression(SimpleLangParser.InvolvedNumericExpressionContext ctx) {
        visit(ctx.numericExpression());
        return null;
    }

    @Override
    public Void visitOperand(SimpleLangParser.OperandContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            String varName = ctx.IDENTIFIER().getText();
            Variable variable = getVariable(ctx, varName);

            if ("string".equals(variable.type())) {
                throw new IllegalArgumentException(String.format("Linha %d: variável %s do tipo string não pode ser usada em operação aritmética", ctx.start.getLine(), varName));
            }

            loadVariable(ctx, varName);

            // Caso a operação atual esteja lidando com floats, deve ser feita a conversão de todos os inteiros para float
            if (isFloatOperation && "int".equals(variable.type())) {
                currentMethod.visitInsn(I2F);
            }
        } else {
            currentMethod.visitLdcInsn(getLiteralValue(ctx));
            if (isFloatOperation && ctx.INT() != null) {
                currentMethod.visitInsn(I2F); // Converte int pra float
            }
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

        // Toda concatenação começa com uma string literal, então será adicionada na pilha automaticamente
        currentMethod.visitLdcInsn(getStringValue(ctx.STRING().getText()));
        currentMethod.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

        for (int i = 1; i < ctx.getChildCount(); i++) {
            var child = ctx.getChild(i);
            var descriptor = "Ljava/lang/String;";

            if (child.getText().equals("+")) {
                continue;
            }

            if (child instanceof SimpleLangParser.LiteralContext literalContext) {
                currentMethod.visitLdcInsn(getLiteralValue(literalContext));
                descriptor = determineDescriptor(literalContext);
            } else if (isAnIdentifier(child)) {
                loadVariable(ctx, child.getText());
                descriptor = determineDescriptor(ctx, child.getText());
            } else if (child instanceof SimpleLangParser.InvolvedNumericExpressionContext involvedNumericExpressionContext) {
                visit(involvedNumericExpressionContext);
                descriptor = isFloatOperation ? Type.FLOAT_TYPE.getDescriptor() : Type.INT_TYPE.getDescriptor();
            }

            currentMethod.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(" + descriptor + ")Ljava/lang/StringBuilder;", false);
        }

        currentMethod.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
    }

    private void loadVariable(ParserRuleContext ctx, String varName) {
        currentMethod.visitVarInsn(ALOAD, 0); // Carrega 'this'
        currentMethod.visitFieldInsn(GETFIELD, className, varName, determineDescriptor(ctx, varName)); // Pega o atributo
    }

    private void executePrint(SimpleLangParser.StatementContext ctx) {
        currentMethod.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        visit(ctx.expression());

        String descriptor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class));

        if (ctx.expression().numericExpression() != null) {
            descriptor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE);
        }

        currentMethod.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", descriptor, false);
    }

    private String determineDescriptor(ParserRuleContext ctx, String varName) {
        return typeToDescriptor(getVariable(ctx, varName).type());
    }

    private String determineDescriptor(SimpleLangParser.LiteralContext ctx) {
        if (ctx.INT() != null) {
            return Type.INT_TYPE.getDescriptor();
        } else if (ctx.FLOAT() != null) {
            return Type.FLOAT_TYPE.getDescriptor();
        } else if (ctx.STRING() != null) {
            return Type.getType(String.class).getDescriptor();
        } else {
            throw new IllegalArgumentException("Literal desconhecido: " + ctx.getText());
        }
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
            return getStringValue(ctx.STRING().getText());
        } else {
            throw new IllegalArgumentException("Literal desconhecido: " + ctx.getText());
        }
    }

    private Object getLiteralValue(SimpleLangParser.OperandContext ctx) {
        if (ctx == null) {
            return null;
        }

        if (ctx.INT() != null) {
            return Integer.parseInt(ctx.INT().getText());
        } else if (ctx.FLOAT() != null) {
            return Float.parseFloat(ctx.FLOAT().getText());
        } else {
            throw new IllegalArgumentException("Literal desconhecido: " + ctx.getText());
        }
    }


    private String getStringValue(String text) {
        return text.substring(1, text.length() - 1);
    }

    private boolean isAnIdentifier(ParseTree child) {
        return child instanceof TerminalNode && SimpleLangParser.IDENTIFIER == ((TerminalNode) child).getSymbol().getType();
    }

    private boolean isFloatOperation(SimpleLangParser.NumericExpressionContext ctx) {
        // Verifica se algum dos operandos é um float
        for (int i = 0; i < ctx.getChildCount(); i += 2) {
            ParseTree operand = ctx.getChild(i);
            if (isFloatOperand(operand)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFloatOperand(ParseTree operand) {
        if (operand instanceof SimpleLangParser.OperandContext ctx) {
            if (ctx.FLOAT() != null) {
                return true;
            }

            if (ctx.IDENTIFIER() != null) {
                Variable variable = getVariable(ctx, ctx.IDENTIFIER().getText());
                return "float".equals(variable.type());
            }
        }

        if (operand instanceof SimpleLangParser.InvolvedNumericExpressionContext ctx) {
            return isFloatOperand(ctx.numericExpression());
        }

        return false;
    }

    private Variable getVariable(ParserRuleContext ctx, String varName) {
        var variable = classVariables.get(varName);

        if (variable == null) {
            throw new IllegalArgumentException(String.format("Linha %d: Variável %s não encontrada", ctx.start.getLine(), varName));
        }

        return variable;
    }

}