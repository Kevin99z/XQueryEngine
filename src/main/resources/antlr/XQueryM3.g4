grammar XQueryM3;
import XPath;

// Define tokens
Var : '$' NAME;

// Define parser rules
xq : Var #XqVar
   | STRING #XqString
   | ap #XqAp
   | '(' xq ')' #XqParens
   | xq ',' xq #XqComma
   | xq '/' rp #XqSlashRp
   | xq '//' rp #XqDblSlashRp
   | '<' NAME '>' '{' xq  '}' '</' NAME '>' #XqTag
   | '<' NAME '>'  xq  '</' NAME '>' #XqTag
   | forClause letClause? whereClause? returnClause #XqFLWR
   | letClause xq #XqLet
   | joinClause #XqJoin
   ;

list: '[' NAME (',' NAME)* ']' | '[' ']';

joinClause : 'join' '(' xq ',' xq ',' list ',' list ')';

forClause : 'for' Var 'in' xq (',' Var 'in' xq)* ;

letClause : (Var ':=' xq)+ ;

whereClause : 'where' cond ;

returnClause : 'return' xq ;

cond : xq '=' xq #CondEq
     | xq 'eq' xq #CondEq
     | xq '==' xq #CondIs
     | xq 'is' xq #CondIs
     | 'empty' '(' xq ')' #CondEmpty
     | 'some' Var 'in' xq (',' Var 'in' xq)* 'satisfies' cond #CondSome
     | '(' cond ')' #CondParens
     | cond 'and' cond #CondAnd
     | cond 'or' cond #CondOr
     | 'not' cond  #CondNot
     ;

