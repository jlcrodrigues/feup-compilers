grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INT : '0' | [1-9][0-9]* ;
ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;

WS : [ \t\n\r]+ -> skip ;

COMMENT_TRADITIONAL : '/*' .*? '*/' -> skip ;
COMMENT_EOL : '//' ~[\r\n]* -> skip ;

program : (importDeclaration)* classDeclaration EOF ;

importDeclaration : 'import' ID ('.' ID)* ';' ;

classDeclaration :
    'class' ID ('extends' ID)? '{' (varDeclaration)* (instanceMethodDeclaration)* (mainMethodDeclaration)? (instanceMethodDeclaration)*'}' ;

varDeclaration : type ID ';' ;

mainMethodDeclaration :
    ('public')? 'static' 'void' 'main' '(' 'String' '[' ']' ID ')' '{' (varDeclaration)* (statement)* '}' ;

instanceMethodDeclaration :
    ('public')? type ID '(' (type ID (',' type ID)*)? ')' '{' (varDeclaration)* (statement)* 'return' expression ';' '}';

type : 'int' '[' ']' | 'boolean' | 'int' | 'String' | ID ;

statement :
    '{' (statement)* '}'
    | 'if' '(' expression ')' statement ('else' statement)?
    | 'while' '(' expression ')' statement
    | expression ';'
    | ID '=' expression ';'
    | ID '[' expression ']' '=' expression ';' ;

expression :
    expression ('&&' | '<' | '+' | '-' | '*' | '/') expression
    | expression '[' expression ']'
    | expression '.' 'length'
    | expression '.' ID '(' (expression (',' expression)*)? ')'
    | 'new' 'int' '[' expression ']'
    | 'new' ID '(' ')'
    | '!' expression
    | '(' expression ')'
    | INT
    | 'true'
    | 'false'
    | ID
    | 'this' ;
