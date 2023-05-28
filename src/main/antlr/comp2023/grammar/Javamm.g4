grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INT : '0' | [1-9][0-9]* ;
ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;

WS : [ \t\n\r\f]+ -> skip ;


COMMENT_TRADITIONAL : '/*' .*? '*/' -> skip ;
COMMENT_EOL : '//' ~[\r\n]* -> skip ;

program : (importDeclaration)* classDeclaration EOF ;

importDeclaration : 'import' id = ID (subimportDeclaration)* ';' #Import;

subimportDeclaration : '.' id = ID #SubImport;

classDeclaration :
    ('public')? 'class' id = ID (classExtension)? '{' (varDeclaration)* (instanceMethodDeclaration)* (mainMethodDeclaration)? (instanceMethodDeclaration)*'}' #Class;

classExtension : 'extends' id = ID #Extends;

varDeclaration : type id = ID ';' #Var ;

mainMethodDeclaration :
    ('public')? 'static' 'void' 'main' '(' 'String' '[' ']' ID ')' '{' (varDeclaration)* (statement)* '}' #MainMethod ;

instanceMethodDeclaration :
    ('public')? staticMethod? returnType id = ID '(' (argumentObject (',' argumentObject)*)? ')' '{' (varDeclaration)* (statement)* 'return' returnObject ';' '}' #InstanceMethod;

staticMethod: 'static';

returnType : type;

returnObject : expression;

argumentObject : type id = ID;

type :
    id = 'int' isArray?
    | id ='boolean'
    | id = 'String'
    | id = ID
    ;

isArray :
    '[' id = (INT | ID)? ']'
    ;

statement :
    '{' (statement)* '}' #Block
    | 'if' '(' condition ')' statement (elseStatement)? #If
    | 'while' '(' condition ')' statement #While
    | expression ';' #ExpressionStatement
    | id = ID '=' expression ';' #Assignment
    | id = ID '[' expression ']' '=' expression ';' #ArrayAssignment
    ;

condition : expression;

elseStatement : 'else' statement #Else;

expression :
    '(' expression ')' #Parentheses
    | expression '[' expression ']' #ArrayAccess
    | expression op = ('++' | '--') #UnaryOp
    | op = ('++' | '--') expression #UnaryOp
    | '!' expression #Negate
    | expression ('.' id = ID)+ #ChainMethods
    | expression '(' ( expression ( ',' expression )* )? ')' #MethodCall
    | expression '.' 'length' #MemberAccessLength
    | expression op = ('*' | '/') expression #BinaryOp
    | expression op = ('+' | '-') expression #BinaryOp
    | expression op = ('<' | '<=' | '>' | '>=') expression #BinaryOp
    | expression op = ('==' | '!=') expression #BinaryOp
    | expression op = '&&' expression #BinaryOp
    | expression op = '||' expression #BinaryOp
    | 'new' 'int' '['expression']' #NewArray
    | 'new' id = ID '(' ')' #NewObject
    | value = INT #Literal
    | value = 'true' #Literal
    | value = 'false' #Literal
    | id = ID #Variable
    | value = 'this' #Literal
    ;
