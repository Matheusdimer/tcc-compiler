class Hello {
    var {
        string nome = "Matheus";
        int idade = 23;
        float salario = 2000.50;
    }

    methods {
        int anosParaAposentar() {
            return 60 - idade;
        }
    }

    # Método construtor do objeto
    init {
        print("Olá " + nome + ", você tem " + idade + " anos de idade.");
        print("Cálculo muito louco: " + (idade + 10.5 + (20 - 2)));
        print("Anos para se aposentar: " + anosParaAposentar());
    }
}