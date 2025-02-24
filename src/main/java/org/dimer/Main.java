package org.dimer;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Uso: java Main <caminho_do_arquivo>");
            System.exit(1);
        }
        String filePath = args[0];
        String code = Files.readString(Paths.get(filePath));
        CharStream input = CharStreams.fromString(code);

        SimpleLangLexer lexer = new SimpleLangLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SimpleLangParser parser = new SimpleLangParser(tokens);

        SimpleLangParser.ProgramContext tree = parser.program();

        System.out.println(tree.toStringTree(parser));
    }
}