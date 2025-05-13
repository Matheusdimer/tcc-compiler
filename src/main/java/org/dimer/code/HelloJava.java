package org.dimer.code;

import java.util.Scanner;

public class HelloJava {
    private String nome;
    private int idade;
    private int idadeAposentadoria = 60;
    private float salario;

    public int getIdade() {
        return this.idade;
    }

    public int anosParaAposentar() {
        int resultado;
        if (this.getIdade() <= this.idadeAposentadoria) {
            resultado = this.idadeAposentadoria - this.getIdade();
        } else {
            resultado = 0;
        }

        return resultado;
    }

    public String formatAposentadoria() {
        int anos = this.anosParaAposentar();
        if (anos == 0) {
            return "Parabéns, você está aposentado.";
        }
        return "Anos para se aposentar: " + anos;
    }

    public HelloJava() {
        System.out.println("Olá, digite seu nome: ");
        Scanner scanner = new Scanner(System.in);

        this.nome = scanner.nextLine();

        while (this.nome.equals("")) {
            System.out.println("Por favor, digite seu nome: ");
            this.nome = scanner.nextLine();
        }

        System.out.println("Digite sua idade: ");

        this.idade = Integer.parseInt(scanner.nextLine());

        while (this.idade < 1 || this.idade > 120) {
            System.out.println("Idade inválida");
            System.out.println("Digite sua idade: ");
            this.idade = Integer.parseInt(scanner.nextLine());
        }

        System.out.println("Olá " + this.nome + ", você tem " + this.idade + " anos de idade.");
        if (this.idade < 18 || this.idade > 70) {
            System.out.println("Você não trabalha.");
        }

        System.out.println("Cálculo complexo: " + (this.idade + 10.5F + (20 - 2)));
        System.out.println(this.formatAposentadoria());
    }

    public static void main(String[] args) {
        new HelloJava();
    }
}
