
A software framework in java that pattern matching like automatons can
be plugged into.

  Scanner

    The scanner class employs "nio" character buffering, and applies
    Pattern and Match to file parsing.

  Pattern and Match

    Essential interfaces to implement for Scanner.

  Resource

    A convenient abstraction over File and Resource sources for
    Scanner.

Raison d'etre

  For the application of trivial automata in an enclosing scope that
  employs additional structures for parsing, or a simple interface to
  complex automata.

Package Automaton

  Included is a modified copy of Automaton by Anders Moller (and
  friends) for the implementation of {jaut.Re}.

  Modifications to Automaton have developed its flexibility under
  application.

  http://www.brics.dk/~amoeller/automaton/

    Automaton Regular Expressions

       Regular expressions are built from the following abstract syntax.

       regexp  ::=     unionexp                
		      | <Empty>
       unionexp        ::=     interexp | unionexp             (union) 
		      |        interexp                
       interexp        ::=     concatexp & interexp            (intersection)
		      |        concatexp               
       concatexp       ::=     repeatexp concatexp             (concatenation) 
		      |        repeatexp               
       repeatexp       ::=     repeatexp ?                     (zero or one occurrence)        
		      |        repeatexp *                     (zero or more occurrences)      
		      |        repeatexp +                     (one or more occurrences)       
		      |        repeatexp {n}                   (n occurrences) 
		      |        repeatexp {n,}                  (n or more occurrences) 
		      |        repeatexp {n,m}                 (n to m occurrences, including both)    
		      |        complexp                
       complexp        ::=     ~ complexp                      (complement)
		      |        charclassexp            
       charclassexp    ::=     [ charclasses ]                 (character class)       
		      |        [^ charclasses ]                (negated character class)       
		      |        simpleexp               
       charclasses     ::=     charclass charclasses           
		      |        charclass               
       charclass       ::=     charexp - charexp               (character range, including end-points) 
		      |        charexp         
       simpleexp       ::=     charexp         
		      |        .                               (any single character)  
		      |        #                               (the empty language)
		      |        @                               (any string)
		      |        " <String Without Double-Quotes> "      (a string)
		      |        ( )                             (the empty string)      
		      |        ( unionexp )                    (precedence override)   
		      |        < <Identifier> >                (named automaton)
		      |        < n-m >                         (numerical interval)
       charexp ::=             <Unicode Character>             (a single non-reserved character)       
		      |        \ <Unicode Character>           (a single character)

       Reserved characters must be escaped with backslash (\) or
       double-quotes ("...").  (In contrast to other regexp syntaxes, this is
       required also in character classes).  Be aware that dash (-) has a
       special meaning in character class expressions.  An identifier is a
       string not containing right angle bracket (>) or dash (-).  Numerical
       intervals are specified by non-negative decimal integers and include
       both end points, and if n and m have the same number of digits, then
       the conforming strings must have that length (i.e. prefixed by 0's).

See also

 * http://swtch.com/~rsc/regexp/
