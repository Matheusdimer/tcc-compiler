package org.dimer.compiler;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.dimer.SimpleLangBaseVisitor;
import org.dimer.SimpleLangParser;
import org.dimer.compiler.data.Method;
import org.dimer.compiler.data.Variable;
import org.dimer.compiler.util.LocalVariableManager;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static org.dimer.compiler.util.CompilerConstants.*;
import static org.objectweb.asm.Opcodes.*;

public class SimpleLangBytecodeVisitor extends SimpleLangBaseVisitor<Void> {

    private final ClassWriter classWriter;
    private final String className;
    private MethodVisitor currentMethod;
    private final Map<String, Variable> classVariables = new HashMap<>();
    private final Map<String, Method> methods = new HashMap<>();
    private boolean isFloatOperation = false;
    private final Stack<Integer> numericExpressionStack = new Stack<>();
    private final Stack<LocalVariableManager> localVariablesStack = new Stack<>();

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
            if (localVariablesStack.isEmpty()) {
                localVariablesStack.push(new LocalVariableManager());
            }

            LocalVariableManager manager = localVariablesStack.peek();
            int varIndex = manager.allocate(new Variable(varName, varType));

            if (ctx.expression() != null) {
                visit(ctx.expression());

                String type = determineTypeOfExpression(ctx.expression());
                if (!varType.equals(type)) {
                    throw new IllegalArgumentException(String.format("Linha %d: tipo de retorno %s da expressão %s não compatível com tipo %s da variável %s",
                            ctx.start.getLine(), type, ctx.expression().getText(), varType, varName));
                }

                currentMethod.visitVarInsn(determineStoreCommand(type), varIndex);
            }
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
        return super.visitStatement(ctx);
    }

    @Override
    public Void visitPrintStatement(SimpleLangParser.PrintStatementContext ctx) {
        currentMethod.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        visit(ctx.expression());

        String type = determineTypeOfExpression(ctx.expression());
        String descriptor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(typeToDescriptor(type)));

        currentMethod.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", descriptor, false);
        return null;
    }

    @Override
    public Void visitMethodDeclaration(SimpleLangParser.MethodDeclarationContext ctx) {
        String methodName = ctx.IDENTIFIER().getText();
        String methodReturnType = ctx.type().getText();
        String methodDescriptor = buildMethodDescriptor(ctx.parameterList(), methodReturnType);
        
        methods.put(methodName, new Method(methodName, methodReturnType, methodDescriptor));
        
        currentMethod = classWriter.visitMethod(ACC_PUBLIC, methodName, methodDescriptor, null, null);
        currentMethod.visitCode();

        var localVariableManager = new LocalVariableManager();
        localVariablesStack.push(localVariableManager);

        if (ctx.parameterList() != null) {
            for (var paramContext : ctx.parameterList().parameter()) {
                String paramName = paramContext.IDENTIFIER().getText();
                String paramType = paramContext.type().getText();

                localVariableManager.allocate(new Variable(paramName, paramType));
            }
        }

        visit(ctx.block());

        currentMethod.visitMaxs(0, 0); // Computado automaticamente pelo ASM

        switch (methodReturnType) {
            case TYPE_INT: currentMethod.visitInsn(IRETURN); break;
            case TYPE_FLOAT: currentMethod.visitInsn(FRETURN); break;
            case TYPE_STRING: currentMethod.visitInsn(ARETURN); break;
            case TYPE_VOID: currentMethod.visitInsn(RETURN); break;
            default: throw new IllegalArgumentException("Tipo de retorno desconhecido: " + methodReturnType + " para o método " + methodName);
        }

        currentMethod.visitEnd();
        currentMethod = null; // Reseta o `currentMethod` após o término

        localVariablesStack.pop(); // Remove as variáveis locais do método da pilha após a compilação do método

        return null;
    }

    @Override
    public Void visitMethodCall(SimpleLangParser.MethodCallContext ctx) {
        String methodName = ctx.IDENTIFIER().getText();

        String methodDescriptor = getMethod(ctx, methodName).descriptor();

        // Primeiro parâmetro de uma chamada de método deve sempre ser o this
        currentMethod.visitVarInsn(ALOAD, 0);

        // Depois, são carregados os demais parâmetros na pilha conforme as expressões
        if (ctx.argumentList() != null) {
            ctx.argumentList().expression().forEach(this::visit);
        }

        // Por fim a chamada do método
        currentMethod.visitMethodInsn(INVOKEVIRTUAL, className, methodName, methodDescriptor, false);

        return null;
    }

    @Override
    public Void visitExpression(SimpleLangParser.ExpressionContext ctx) {
        if (ctx.literal() != null) {
            currentMethod.visitLdcInsn(getLiteralValue(ctx.literal()));
        } else if (ctx.IDENTIFIER() != null) { // Aponta para uma variável
            loadVariable(ctx, ctx.IDENTIFIER().getText());
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

            if (TYPE_STRING.equals(variable.type())) {
                throw new IllegalArgumentException(String.format("Linha %d: variável %s do tipo string não pode ser usada em operação aritmética", ctx.start.getLine(), varName));
            }

            loadVariable(ctx, varName);

            // Caso a operação atual esteja lidando com floats, deve ser feita a conversão de todos os inteiros para float
            if (isFloatOperation && "int".equals(variable.type())) {
                currentMethod.visitInsn(I2F);
            }
        } else if (ctx.methodCall() != null) {
            String type = determineReturnTypeMethodCall(ctx.methodCall());

            if (TYPE_STRING.equals(type)) {
                throw new IllegalArgumentException(String.format("Linha %d: método %s com retorno tipo string não pode ser usada em operação aritmética",
                        ctx.start.getLine(), ctx.methodCall().IDENTIFIER().getText()));
            }

            visit(ctx.methodCall());

            if (isFloatOperation && TYPE_INT.equals(type)) {
                currentMethod.visitInsn(I2F); // Converte int pra float
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
    @Override
    public Void visitStringConcatenation(SimpleLangParser.StringConcatenationContext ctx) {
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
            } else if (child instanceof SimpleLangParser.MethodCallContext methodCallContext) {
                visit(methodCallContext);
                descriptor = typeToDescriptor(getMethod(methodCallContext, methodCallContext.IDENTIFIER().getText()).returnType());
            }

            currentMethod.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(" + descriptor + ")Ljava/lang/StringBuilder;", false);
        }

        currentMethod.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        return null;
    }

    private void loadVariable(ParserRuleContext ctx, String varName) {
        if (!localVariablesStack.isEmpty()) {
            var manager = localVariablesStack.peek();
            var variable = manager.load(varName);

            if (variable != null) {
                currentMethod.visitVarInsn(determineLoadCommand(variable.type()), variable.index());
                return;
            }
        }

        loadClassVariable(ctx, varName);
    }

    private void loadClassVariable(ParserRuleContext ctx, String varName) {
        currentMethod.visitVarInsn(ALOAD, 0); // Carrega 'this'
        currentMethod.visitFieldInsn(GETFIELD, className, varName, determineDescriptor(ctx, varName)); // Pega o atributo
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
        if (!localVariablesStack.isEmpty()) {
            var variable = localVariablesStack.peek().load(varName);

            if (variable != null) {
                return variable;
            }
        }

        var variable = classVariables.get(varName);

        if (variable == null) {
            throw new IllegalArgumentException(String.format("Linha %d: Variável %s não encontrada", ctx.start.getLine(), varName));
        }

        return variable;
    }

    private String buildMethodDescriptor(SimpleLangParser.ParameterListContext parameterListCtx, String returnType) {
        StringBuilder descriptor = new StringBuilder();
        descriptor.append("("); // Início da lista de parâmetros

        if (parameterListCtx != null) {
            for (var paramCtx : parameterListCtx.parameter()) {
                String paramType = paramCtx.type().getText();
                descriptor.append(typeToDescriptor(paramType)); // Converte o tipo para o descriptor ASM
            }
        }

        descriptor.append(")");
        descriptor.append(typeToDescriptor(returnType)); // Tipo de retorno

        return descriptor.toString();
    }

    private Method getMethod(ParserRuleContext ctx, String methodName) {
        var method = methods.get(methodName);

        if (method == null) {
            throw new IllegalArgumentException(String.format("Linha %d: Método %s não encontrado", ctx.start.getLine(), methodName));
        }

        return method;
    }

    private int determineStoreCommand(String type) {
        return switch (type) {
            case TYPE_INT -> ISTORE;
            case TYPE_FLOAT -> FSTORE;
            case TYPE_STRING -> ASTORE;
            default -> throw new IllegalArgumentException("Tipo desconhecido: " + type);
        };
    }

    private int determineLoadCommand(String type) {
        return switch (type) {
            case TYPE_INT -> ILOAD;
            case TYPE_FLOAT -> FLOAD;
            case TYPE_STRING -> ALOAD;
            default -> throw new IllegalArgumentException("Tipo desconhecido: " + type);
        };
    }

    private String determineTypeOfExpression(SimpleLangParser.ExpressionContext ctx) {
        if (ctx.stringConcatenation() != null) {
            return TYPE_STRING;
        }

        if (ctx.numericExpression() != null) {
            return isFloatOperation ? TYPE_FLOAT : TYPE_INT;
        }

        if (ctx.involvedExpression() != null) {
            return determineTypeOfExpression(ctx.involvedExpression().expression());
        }

        if (ctx.IDENTIFIER() != null) {
            return getVariable(ctx, ctx.IDENTIFIER().getText()).type();
        }

        if (ctx.methodCall() != null) {
            return determineReturnTypeMethodCall(ctx.methodCall());
        }

        if (ctx.literal() != null) {
            return determineLiteralType(ctx.literal());
        }

        throw new IllegalArgumentException(String.format("Linha %d: não foi possível determinar o tipo de retorno da expressão %s",
                ctx.start.getLine(), ctx.getText()));
    }

    private String determineReturnTypeMethodCall(SimpleLangParser.MethodCallContext ctx) {
        String methodName = ctx.IDENTIFIER().getText();
        Method method = getMethod(ctx, methodName);
        return method.returnType();
    }

    private String determineLiteralType(SimpleLangParser.LiteralContext ctx) {
        if (ctx == null) {
            return null;
        }

        if (ctx.INT() != null) {
            return TYPE_INT;
        } else if (ctx.FLOAT() != null) {
            return TYPE_FLOAT;
        } else if (ctx.STRING() != null) {
            return TYPE_STRING;
        } else {
            throw new IllegalArgumentException(String.format("Linha %d: não foi possível determinar o tipo de retorno da expressão %s",
                    ctx.start.getLine(), ctx.getText()));
        }
    }
}