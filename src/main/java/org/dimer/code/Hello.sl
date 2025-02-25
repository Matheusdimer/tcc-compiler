class Hello {
    var {
        string nome;
        int idade;
        int idadeAposentadoria = 60;
        float salario = 2000.50;
    }

    methods {
        getIdade(): int {
            return idade;
        }

        anosParaAposentar(): int {
            if (getIdade() > idadeAposentadoria) {
                return 0;
            }

            return idadeAposentadoria - getIdade();
        }

        formatAposentadoria(): string {
            return "Anos para se aposentar: " + anosParaAposentar();
        }
    }

    # Método construtor do objeto
    init {
        print("Olá, digite seu nome: ");
        read(nome);

        print("Digite sua idade: ");
        read(idade);

        print("Olá " + nome + ", você tem " + idade + " anos de idade.");
        print("Cálculo muito louco: " + (idade + 10.5 + (20 - 2)));

        print(formatAposentadoria());
    }
}