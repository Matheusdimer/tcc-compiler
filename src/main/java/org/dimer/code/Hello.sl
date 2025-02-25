class Hello {
    var {
        string nome = "Matheus";
        int idade = 23;
        float salario = 2000.50;
    }

    methods {
        int anosParaAposentar() {
            int idadeAposentadoria = 60;
            return idadeAposentadoria - idade;
        }

        string formatAposentadoria(int anos) {
            return "Anos para se aposentar: " + anos;
        }
    }

    # Método construtor do objeto
    init {
        print("Olá " + nome + ", você tem " + idade + " anos de idade.");
        print("Cálculo muito louco: " + (idade + 10.5 + (20 - 2)));

        int anos = anosParaAposentar();

        print(formatAposentadoria(anos));
    }
}