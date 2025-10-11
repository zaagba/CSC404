# Extension Project 3: Break Statements

For this extension project, I implemented functioning break statements, which exit a loop when called.

```java
Example Program:

var a = 0;
var temp;

for (var b = 1; a < 10000; b = temp + b) {
    print a;
    break;
    temp = a;
    a = b;
}

Output:

0
- end -

```

- In order to implement break, the following modifications were made to the source code:

  - SCANNER:

    - The scanner needed to be able to recognize the break statement, so the following was done:

      - TokenType.java had the BREAK keyword added

      - ```java
           
        enum TokenType { ...
        // Keywords.
            AND, CLASS, ELSE, FALSE, FUN,
            FOR, IF, NIL, OR, PRINT,
            RETURN, SUPER, THIS, TRUE,
            VAR, WHILE, *BREAK*,
        
        	... 
                       }
        ```

      - Since it's a keyword, we add it to keyword Map in the Scanner:

      - ```java
        private static final Map<String, TokenType> keywords;
        static {
        ...
        
                keywords.put("this", THIS);
                keywords.put("true", TRUE);
                keywords.put("var", VAR);
                keywords.put("while", WHILE);
                *keywords.put("break", BREAK);*
        }
        ```

      - After that, we can generate the break statement with the AST.

      - ```java
        abstract class Stmt {
          interface Visitor<R> {
            R visitBlockStmt(Block stmt);
            R visitExpressionStmt(Expression stmt);
            R visitIfStmt(If stmt);
            R visitPrintStmt(Print stmt);
            R visitWhileStmt(While stmt);
            R visitVarStmt(Var stmt);
            *R visitBreakStmt(Break stmt);*
          }
        
        ```

      - This leads to the Break class:

      - ```java
          static class Break extends Stmt {
            Break(Token keyword) {
              this.keyword = keyword;
            }
        
            @Override
            <R> R accept(Visitor<R> visitor) {
              return visitor.visitBreakStmt(this);
            }
        
            final Token keyword;
        
            @Override
            public String toString() {
              return "Break(" + keyword + ")";
            }
          }
        ```

        - With the Break statement in our scanner and set as a statement, we can add it to our parser.
        - My implementation relies on a field being added to the Parser, breakCount, which makes sure that breaks are not used outside of for/while loops (they rely on incrementing then decrementing breakCount only when loops are called -- with an error being thrown if breakCount == 0, since that would mean it's called outside a loop)

        

        ```java
          
        public class Parser {
            private static class ParseError extends RuntimeException {}
        
            private final List<Token> tokens;
            private int current = 0;
            *private int breakCount = 0;*
        
        ...
        ```

          

      - Next, we wire up break as a statement:

      - ```java
            private Stmt statement() {
                if (match(FOR)) return forStatement();
                if (match(IF)) return ifStatement();
                if (match(PRINT)) return printStatement();
                if (match(WHILE)) return whileStatement();
                if (match(LEFT_BRACE)) return new Stmt.Block(block());
                *if (match(TokenType.BREAK)) return breakStatement();*
                return expressionStatement();
            }
        
        ```

      - Next, we add break functionality into our native looping statements:

      - ```java
            private Stmt forStatement() {
               ...
                Stmt body;
              *  try {
                    breakCount++;
                    body = statement();
                } finally {
                    breakCount--;
                }*
        ...
                return body;
            }
        
        ```

      - The BreakStatement() itself follows the logic we outlined previously:

      - ```java
            private Stmt breakStatement() {
                Token breakToken = previous();
                if (breakCount == 0) {
                    error(breakToken, "Cannot use 'break' outside of a loop.");
                }
                consume(TokenType.SEMICOLON, "Expect ';' after break.");
                return new Stmt.Break(breakToken);
            }
        ```

      - Finally, we can implement our break methods into our interpreter. To begin, I created another breaking field that is a boolean:

      - ```java
        public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {  
            private Environment environment = new Environment();
            *private boolean breaking = false;*
        ...
        ```

      - We can also implement our visitBreakStatement():

      - ```java
            @Override
            public Void visitBreakStmt(Stmt.Break stmt) {
                breaking = true;
                return null;
            }
        ```

      - All this does is set our breaking flag to true

      - Now, we can edit our executeBlock() method to account for the fact that breaks may be called during statements:

        ```java
            void executeBlock(List<Stmt> statements, Environment environment) {
                Environment previous = this.environment;
                try {
                    this.environment = environment;
        
                    for (Stmt statement : statements) {
                        execute(statement);
                        *if (breaking) break;*
                    }
                } finally {
                    this.environment = previous;    // restore
                }
            }
        
        ```

      - Finally, we can edit our visitWhileStatement() to break if breaking is true, and we're done!

      - ```java
            @Override
            public Void visitWhileStmt(Stmt.While stmt) {
                while (isTruthy(evaluate(stmt.condition))) {
                    execute(stmt.body);
                    if (breaking) {
                        breaking = false;
                        break;
                    }
                }
                return null;
            }
        ```

      - Now that everything is wired up, breaks are implemented into Lox!



#### Testing:

- Testing was accomplished purely through the console and modifying the supplied code, along with testing various for/while loops.

- ```java
  Example Program 1:
  
  for (var i = 0; i < 5; i = i + 1) {
    if (i == 2) break;
    print i;
  }
  
  OUTPUT:
  0
  1
      
  Example Program 2:
  
  
  for (var i = 0; i < 5; i = i + 1) {
      print i;
      break;
  }
  OUTPUT:
  0
  ```

