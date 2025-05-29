package org.dimer.benchmark;

//import org.dimer.code.Hello;
import org.dimer.code.HelloJava;

import java.io.ByteArrayInputStream;

public class Benchmark {

    private static final String INPUTS = "\n\nMatheus\n140\n0\n17\n";
    private static final int REPETICOES = 200;

    private static void fornecerInput() {
        ByteArrayInputStream fakeIn = new ByteArrayInputStream(INPUTS.getBytes());
        System.setIn(fakeIn);
    }

    public static void main(String[] args) {
        long somaSimpleLang = 0;
        long somaJava = 0;

        // Executa Hello (SimpleLang)
        for (int i = 0; i < REPETICOES; i++) {
            fornecerInput();
            long start = System.nanoTime();
//            Hello.main(new String[]{});
            long end = System.nanoTime();
            somaSimpleLang += (end - start);
        }

        // Executa HelloJava (Java)
        for (int i = 0; i < REPETICOES; i++) {
            fornecerInput();
            long start = System.nanoTime();
            HelloJava.main(new String[]{});
            long end = System.nanoTime();
            somaJava += (end - start);
        }

        long mediaSimpleLang = somaSimpleLang / REPETICOES;
        long mediaJava = somaJava / REPETICOES;
        double diferenca = ((double)(mediaSimpleLang - mediaJava) / mediaSimpleLang) * 100;

        System.out.println("------------------------------------------");
        System.out.println("SimpleLang (média): " + mediaSimpleLang + " ns");
        System.out.println("Java (média): " + mediaJava + " ns");
        System.out.printf("Java foi %.1f%% mais rápido.\n", diferenca);
        System.out.println("------------------------------------------");
    }
}
