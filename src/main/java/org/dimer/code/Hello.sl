class Hello {
    var {
        string nome = "Matheus";
        int idade = 23;
        float salario = 2000.50;
    }

    methods {
        getIdade(): int {
            return idade;
        }

        anosParaAposentar(): int {
            int idadeAposentadoria = 60;
            return idadeAposentadoria - getIdade();
        }

        formatAposentadoria(): string {
            return "Anos para se aposentar: " + anosParaAposentar();
        }
    }

    # Método construtor do objeto
    init {
        print("Olá " + nome + ", você tem " + idade + " anos de idade.");
        print("Cálculo muito louco: " + (idade + 10.5 + (20 - 2)));

        print(formatAposentadoria());
    }
}