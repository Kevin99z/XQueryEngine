grammar XQuerySub;
// Lexer rules
DOC: 'doc' | 'document';
TEXT: 'text()';
EQ: 'eq' | '=';
IS: 'is' | '==';
AND: 'and';
OR: 'or';
NOT: 'not';
STRING: '"' (~["\r\n])* '"' | '\'' (~["\r\n])* '\'';
NAME: [a-zA-Z_][a-zA-Z_0-9]*;
WS: [ \t\r\n]+ -> skip;

Var : '$' NAME;
SEP : '/' | '//';

// Define parser rules
xq : 'for' Var 'in' path (',' Var 'in' path)* 'where' cond 'return' return;

return : Var #ReturnVar
       |   return ',' return #ReturnComma
       |   '<' NAME '>' '{' return '}' '</' NAME '>' #ReturnTag
       | path #ReturnPath
;

cond : (Var | STRING) EQ (Var| STRING) #CondEq
       |   cond AND cond #CondAnd
       ;

path: (DOC '(' STRING ')' | Var) (SEP NAME)* #PathName
 |   (DOC '(' STRING ')' | Var) (SEP NAME)* SEP TEXT #PathText
 ;