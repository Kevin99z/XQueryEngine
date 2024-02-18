grammar XPath;

// Parser rules
ap: DOC '(' fileName ')' '/'  rp #ApChild
    | DOC '(' fileName ')' '//' rp #ApDescendent
    ;

rp: tagName #RpTagName
   | '*' #Wildcard
   | '.' #CurrentNode
   | '..' #ParentNode
   | TEXT #Text
   | '@' attName #RpAttName
   | '(' rp ')' #ParenthesizedRp
   | rp '/' rp #RpChild
   | rp '//' rp #RpDescendant
   | rp '[' f ']' #RpFilter
   | rp ',' rp #RpConcatenate
   ;

f: rp #FilterRp
  | rp '=' rp #FilterEqual
  | rp EQ rp #FilterEqual
  | rp '==' rp #FilterIs
  | rp IS rp #FilterIs
  | rp '=' STRING #FilterStringEqual
  | '(' f ')' #ParenthesizedF
  | f AND f #FilterAnd
  | f OR f #FilterOr
  | NOT f #FilterNot
  ;

// Lexer rules
DOC: 'doc' | 'document';
TEXT: 'text()';
EQ: 'eq';
IS: 'is';
AND: 'and';
OR: 'or';
NOT: 'not';
tagName: NAME;
attName: NAME;
fileName: STRING;
STRING: '"' (~["\r\n])* '"' | '\'' (~["\r\n])* '\'';
NAME: [a-zA-Z_][a-zA-Z_0-9]*;
WS: [ \t\r\n]+ -> skip;

