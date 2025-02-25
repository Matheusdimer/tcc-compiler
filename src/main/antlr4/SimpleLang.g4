grammar SimpleLang;

// Parser Rules

program       : classDeclaration+ ;
classDeclaration
              : CLASS IDENTIFIER LBRACE varSection methodsSection initSection RBRACE ;

varSection    : VAR LBRACE varDeclaration* RBRACE ;
varDeclaration
              : type IDENTIFIER (ASSIGN expression)? SEMICOLON ;

methodsSection
              : METHODS LBRACE methodDeclaration* RBRACE ;
methodDeclaration
              : type IDENTIFIER LPAREN parameterList? RPAREN block ;

parameterList : parameter (COMMA parameter)* ;
parameter     : type IDENTIFIER ;

initSection   : INIT LBRACE statement* RBRACE ;

block         : LBRACE statement* RBRACE ;

statement     : varDeclaration
              | methodCall SEMICOLON
              | assignment SEMICOLON
              | ifStatement
              | whileStatement
              | RETURN expression SEMICOLON
              | PRINT LPAREN expression RPAREN SEMICOLON ;

ifStatement   : IF LPAREN expression RPAREN block (ELSE block)? ;
whileStatement
              : WHILE LPAREN expression RPAREN block ;

assignment    : IDENTIFIER ASSIGN expression ;
methodCall    : IDENTIFIER LPAREN argumentList? RPAREN ;
argumentList  : expression (COMMA expression)* ;

// Alteração importante para diferenciar tipos:
expression
              : stringConcatenation
              | numericExpression
              | comparisonExpression
              | comparisonStringExpression
              | parenthesizedExpression
              | methodCall
              | IDENTIFIER
              | literal
              ;

stringConcatenation
              : STRING (PLUS (literal | IDENTIFIER))+ ;

numericExpression
              : (operand | LBRACE numericExpression RBRACE) (PLUS | MINUS | MULT | DIV) (operand | LBRACE numericExpression RBRACE)* ;

comparisonExpression
              : (INT | FLOAT | IDENTIFIER) (GT | LT | GTE | LTE | EQUAL | NOTEQUAL) (INT | FLOAT | IDENTIFIER) ;

comparisonStringExpression
              : (STRING | IDENTIFIER) (EQUAL (STRING | IDENTIFIER))+ ;

parenthesizedExpression
              : LPAREN expression RPAREN ;

literal       : STRING
              | INT
              | FLOAT ;

type          : STRING_TYPE
              | INT_TYPE
              | FLOAT_TYPE
              | VOID_TYPE ;

operand       : INT | FLOAT | IDENTIFIER ;

// Lexer Rules

// Palavras-chave
CLASS         : 'class' ;
VAR           : 'var' ;
METHODS       : 'methods' ;
INIT          : 'init' ;
RETURN        : 'return' ;
PRINT         : 'print' ;
IF            : 'if' ;
ELSE          : 'else' ;
WHILE         : 'while' ;
STRING_TYPE   : 'string' ;
INT_TYPE      : 'int' ;
FLOAT_TYPE    : 'float' ;
VOID_TYPE     : 'void' ;

// Símbolos e operadores
LBRACE        : '{' ;
RBRACE        : '}' ;
LPAREN        : '(' ;
RPAREN        : ')' ;
SEMICOLON     : ';' ;
COMMA         : ',' ;
ASSIGN        : '=' ;
PLUS          : '+' ;
MINUS         : '-' ;
MULT          : '*' ;
DIV           : '/' ;
GT            : '>' ;
LT            : '<' ;
GTE           : '>=' ;
LTE           : '<=' ;
EQUAL         : '==' ;
NOTEQUAL      : '!=' ;

// Outros tokens
COMMENT       : '#' ~[\r\n]* -> skip;         // Comentários iniciados com '#' são ignorados
BLOCK_COMMENT : '##' .*? '##' -> skip;       // Comentários de bloco entre '##' ... '##'
IDENTIFIER    : [a-zA-Z_] [a-zA-Z_0-9]* ;
STRING        : '"' .*? '"' ;
INT           : [0-9]+ ;
FLOAT         : [0-9]+ '.' [0-9]+ ;

WHITESPACE    : [ \t\r\n]+ -> skip ;          // Espaços em branco, tabs, novas linhas ignorados