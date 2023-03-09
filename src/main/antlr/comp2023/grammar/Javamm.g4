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

importDeclaration : 'import' id = ID (subimportDeclaration)* ';' #Import;

subimportDeclaration : '.' id = ID #SubImport;

classDeclaration :
    'class' id = ID (classExtension)? '{' (varDeclaration)* (instanceMethodDeclaration)* (mainMethodDeclaration)? (instanceMethodDeclaration)*'}' #Class;

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
    | id = ID isArray? '=' expression ';' #ArrayAssignment
    ;

condition : expression;

elseStatement : 'else' statement #Else;

expression :
    '(' expression ')' #Parentheses
    | '!' expression #Negate
    | expression op = ('*' | '/') expression #BinaryOp
    | expression op = ('+' | '-') expression #BinaryOp
    | expression op = ('<' | '<=' | '>' | '>=') expression #BinaryOp
    | expression op = ('==' | '!=') expression #BinaryOp
    | expression op = '&&' expression #BinaryOp
    | expression op = '||' expression #BinaryOp
    | expression '[' expression ']' #ArrayAccess
    | expression '.' 'length' #MemberAccessLength
    | expression '.' id = ID '(' (expression (',' expression)*)? ')' #MethodCall
    | 'new' type isArray #NewArray
    | 'new' id = ID '(' ')' #NewObject
    | value = INT #Literal
    | value = 'true' #Literal
    | value = 'false' #Literal
    | id = ID #Variable
    | value = 'this' #Literal
    ;


