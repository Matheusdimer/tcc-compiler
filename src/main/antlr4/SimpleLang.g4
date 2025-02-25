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
              : IDENTIFIER LPAREN parameterList? RPAREN COLON type block ;

parameterList : parameter (COMMA parameter)* ;
parameter     : type IDENTIFIER ;

initSection   : INIT LBRACE statement* RBRACE ;

block         : LBRACE statement* RBRACE ;

statement     : varDeclaration
              | methodCall SEMICOLON
              | assignment SEMICOLON
              | ifStatement
              | whileStatement
              | returnStatement
              | printStatement
              | readStatement ;

printStatement: PRINT LPAREN expression RPAREN SEMICOLON ;

readStatement : READ LPAREN IDENTIFIER RPAREN SEMICOLON ;

ifStatement   : IF LPAREN expression RPAREN block (ELSE block)? ;
whileStatement
              : WHILE LPAREN expression RPAREN block ;

returnStatement
              : RETURN expression SEMICOLON ;

assignment    : IDENTIFIER ASSIGN expression ;
methodCall    : IDENTIFIER LPAREN argumentList? RPAREN ;
argumentList  : expression (COMMA expression)* ;

// Alteração importante para diferenciar tipos:
expression
              : stringConcatenation
              | numericExpression
              | comparisonExpression
              | comparisonStringExpression
              | involvedExpression
              | methodCall
              | IDENTIFIER
              | literal
              ;

stringConcatenation
              : STRING (PLUS (literal | IDENTIFIER | involvedNumericExpression | methodCall))+ ;

numericExpression
              : (operand | involvedNumericExpression) ((PLUS | MINUS | MULT | DIV) (operand | involvedNumericExpression))+ ;

involvedNumericExpression
              : LPAREN numericExpression RPAREN ;

comparisonExpression
              : (operand) (GT | LT | GTE | LTE | EQUAL | NOTEQUAL) (operand) ;

comparisonStringExpression
              : (STRING | IDENTIFIER) (EQUAL (STRING | IDENTIFIER))+ ;

involvedExpression
              : LPAREN expression RPAREN ;

literal       : STRING
              | INT
              | FLOAT ;

type          : STRING_TYPE
              | INT_TYPE
              | FLOAT_TYPE
              | VOID_TYPE ;

operand       : INT | FLOAT | IDENTIFIER | methodCall ;

// Lexer Rules

// Palavras-chave
CLASS         : 'class' ;
VAR           : 'var' ;
METHODS       : 'methods' ;
INIT          : 'init' ;
RETURN        : 'return' ;
PRINT         : 'print' ;
READ          : 'read' ;
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
COLON         : ':' ;
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