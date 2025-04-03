package org.dimer;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.dimer.compiler.SimpleLangBytecodeVisitor;

import java.io.File;
import java.io.FileOutputStream;

public class Main {
    private static final String BASE_TARGET_PATH = "./target/classes/";
    private static final File BASE_SOURCE_PATH = new File("./src/main/java/");

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Uso: java Main <caminho_do_arquivo>");
            System.exit(1);
        }
        String filePath = args[0];
        CharStream input = CharStreams.fromFileName(filePath);

        SimpleLangLexer lexer = new SimpleLangLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SimpleLangParser parser = new SimpleLangParser(tokens);

        SimpleLangParser.ProgramContext tree = parser.program();

        System.out.println(tree.toStringTree(parser));

        String className = tree.classDeclaration().getFirst().IDENTIFIER().getText();
        String pack = determinePackage(filePath);

        // Visitor para geração de bytecode
        SimpleLangBytecodeVisitor visitor = new SimpleLangBytecodeVisitor(determineClassName(pack, className));
        visitor.visit(tree);

        String outputPath = BASE_TARGET_PATH + pack + "/" + className + ".class";

        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();

        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        byte[] bytecode = visitor.getBytecode();

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(bytecode);
        }

        System.out.println("Bytecode gerado e salvo em " + outputFile.getPath());
    }

    private static String determinePackage(String filePath) {
        String path = new File(filePath).getParentFile().getPath();
        return path.replace(BASE_SOURCE_PATH.getPath(), "").replace("\\", "/");
    }

    private static String determineClassName(String packageName, String className) {
        if (packageName.startsWith("/")) {
            packageName = packageName.substring(1);
        }

        return packageName + "/" + className;
    }
}