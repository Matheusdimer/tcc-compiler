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
    }

    # Método construtor do objeto
    init {
        print("Olá " + nome + ", você tem " + idade + " anos de idade.");
        print("Cálculo muito louco: " + (idade + 10.5 + (20 - 2)));

        int anos = anosParaAposentar();

        print("Anos para se aposentar: " + anos);
    }
}