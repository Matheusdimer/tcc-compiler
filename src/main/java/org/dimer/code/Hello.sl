class Hello {
    var {
        string nome;
        int idade;
        int idadeAposentadoria = 60;
        float salario;
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

        print("Olá " + nome + ", você tem " + idade + " anos de idade.");
        if (idade < 18 or idade > 70) {
            print("Você não trabalha.");
        }
        print("Cálculo complexo: " + (idade + 10.5 + (20 - 2)));
        print(formatAposentadoria());
    }
}