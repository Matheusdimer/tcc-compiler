grammar SimpleLang;

// Parser Rules

program       : classDeclaration+ ;
classDeclaration
              : 'class' IDENTIFIER '{' varSection methodsSection initSection '}' ;

varSection    : 'var' '{' varDeclaration* '}' ;
varDeclaration
              : type IDENTIFIER ('=' expression)? ';' ;

methodsSection
              : 'methods' '{' methodDeclaration* '}' ;
methodDeclaration
              : type IDENTIFIER '(' parameterList? ')' block ;

parameterList : parameter (',' parameter)* ;
parameter     : type IDENTIFIER ;

initSection   : 'init' '{' statement* '}' ;

block         : '{' statement* '}' ;

statement     : varDeclaration
              | methodCall ';'
              | assignment ';'
              | ifStatement
              | whileStatement
              | 'return' expression ';'
              | 'print' '(' expression ')' ';' ;

ifStatement   : 'if' '(' expression ')' block ('else' block)? ;
whileStatement
              : 'while' '(' expression ')' block ;

assignment    : IDENTIFIER '=' expression ;
methodCall    : IDENTIFIER '(' argumentList? ')' ;
argumentList  : expression (',' expression)* ;

expression    : expression ('+' | '-' | '*' | '/') expression
              | expression ('>' | '<' | '>=' | '<=' | '==' | '!=') expression
              | '(' expression ')'
              | methodCall
              | IDENTIFIER
              | literal ;

literal       : STRING
              | INT
              | FLOAT ;

type          : 'string'
              | 'int'
              | 'float'
              | 'void' ;

// Lexer Rules

COMMENT: '#' ~[\r\n]* -> skip;         // Comentários iniciados com '#' são ignorados
IDENTIFIER    : [a-zA-Z_] [a-zA-Z_0-9]* ;
STRING        : '"' .*? '"' ;
INT           : [0-9]+ ;
FLOAT         : [0-9]+ '.' [0-9]+ ;

WHITESPACE    : [ \t\r\n]+ -> skip ;
