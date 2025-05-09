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
            int resultado;

            if (getIdade() > idadeAposentadoria) {
                resultado = 0;
            } else {
                resultado = idadeAposentadoria - getIdade();
            }

            return resultado;
        }

        formatAposentadoria(): string {
            int anos = anosParaAposentar();

            if (anos == 0) {
                return "Parabéns, você está aposentado.";
            }

            return "Anos para se aposentar: " + anos;
        }
    }

    # Método construtor do objeto
    init {
        print("Olá, digite seu nome: ");
        read(nome);

        while (nome == "") {
            print("Por favor, digite seu nome: ");
            read(nome);
        }

        print("Digite sua idade: ");
        read(idade);

        while (idade < 1 or idade > 120) {
            print("Idade inválida");
            print("Digite sua idade: ");
            read(idade);
        }

        if (idade < 18 or idade > 70) {
            print("Você nem trabalha");
        }

        if (idade >= 18 and idade <= 70) {
            print("Trabalhador");
        }

        print("Olá " + nome + ", você tem " + idade + " anos de idade.");
        print("Cálculo muito louco: " + (idade + 10.5 + (20 - 2)));

        print(formatAposentadoria());
    }
}