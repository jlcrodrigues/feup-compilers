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
    ('public')? returnType id = ID '(' (argumentObject (',' argumentObject)*)? ')' '{' (varDeclaration)* (statement)* 'return' returnObject ';' '}' #InstanceMethod;

returnType : type;

returnObject : expression;

argumentObject : type id = ID;

type :
    id = 'int' isArray?
    | id ='boolean' isArray?
    | id = 'String' isArray?
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
    | expression op = ('++' | '--') #UnaryOp
    | op = ('++' | '--') expression #UnaryOp
    | '!' expression #Negate
    | expression op = ('*' | '/') expression #BinaryOp
    | expression op = ('+' | '-') expression #BinaryOp
    | expression op = ('<' | '<=' | '>' | '>=') expression #BinaryOp
    | expression op = ('==' | '!=') expression #BinaryOp
    | expression op = '&&' expression #BinaryOp
    | expression op = '||' expression #BinaryOp
    | expression '[' expression ']' #ArrayAccess
    | expression '.' 'length' #MemberAccessLength //TODO
    | expression ('.' id = ID)+ #ChainMethods //TODO
    | expression '(' (expression (',' expression)*)? ')' #MethodCall //TODO
    //| 'new' type isArray #NewArray
    | 'new' 'int' '['expression']' #NewArray //TODO
    | 'new' id = ID '(' ')' #NewObject //TODO
    | value = INT #Literal
    | value = 'true' #Literal
    | value = 'false' #Literal
    | id = ID #Variable
    | value = 'this' #Literal //TODO
    ;


