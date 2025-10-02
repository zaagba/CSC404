# Extension Project 5

## Implemented Arrays

* The goal for this extension project was to implement arrays such that they would emulate the behavior of Java arrays, as demonstrated in the following code block:

```java
var a = Array(4); // creates an array of size 4
a[0] = "hello";
a[1] = ",";
a[2] = "World";
a[3] = "!";

print(a[0]); // "hello"
print(a.length); // 4

```



* To accomplish this, the following changes were made to the source code (additions are encapsulated with *):

  * **STEP 1**: *Adding bracket - [] - tokens, and implementing them in the scanner*

    * In the single character token section in TokenType, I added LEFT_BRACKET and RIGHT_BRACKET so that the scanner can add them as their tokens

      * ```java
        package lox;
        
        enum TokenType {
            // Single-character tokens.
            LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
            COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,
            *LEFT_BRACKET, RIGHT_BRACKET,*
        
        ```

    * Next, we add the brackets into the scanner and add those tokens

      * ```java
        
            private void scanToken() {
                char c = advance();
                switch (c) {    
                    case '(' -> addToken(LEFT_PAREN);                       // .nah.   updated switch syntax 
                    case ')' -> addToken(RIGHT_PAREN);
                    *case '[' -> addToken(LEFT_BRACKET);
                    case ']' -> addToken(RIGHT_BRACKET);*
        
        ```

    * Now that we have a way for the scanner to recognize an array's brackets, we turn to the AST to create the expressions our array class will use— namely, *Array[index]* (Index), and *Array[index] = Value* (SetIndex).

      * We add their visitor expressions in the Visitor interface:

        * ```java
          
          abstract class Expr {
            interface Visitor<R> {
              R visitAssignExpr(Assign expr);
              R visitBinaryExpr(Binary expr);
              R visitCallExpr(Call expr);
              R visitGetExpr(Get expr);
              R visitGroupingExpr(Grouping expr);
              R visitLiteralExpr(Literal expr);
              R visitLogicalExpr(Logical expr);
              R visitSetExpr(Set expr);
              R visitSuperExpr(Super expr);
              R visitThisExpr(This expr);
              R visitUnaryExpr(Unary expr);
              R visitVariableExpr(Variable expr);
              *R visitIndexExpr(Index expr);
              R visitSetIndexExpr(SetIndex expr);*
            }
          ```
      
        * We can then add the corresponding expressions to our Expr.java file
      * ```java
          
        static class Index extends Expr {
          final Expr array;
          final Expr index;
          Index(Expr array, Expr index) {
            this.array = array;
            this.index = index;
          
          }
            @Override
            <R> R accept(Visitor<R> visitor) {
              return visitor.visitIndexExpr(this);
            }
          
        ```
      
      * ```java
        
        static class SetIndex extends Expr {
          final Expr array;
          final Expr index;
          final Expr value;
          SetIndex(Expr array, Expr index, Expr value) {
            this.array = array;
            this.index = index;
            this.value = value;
          }
            @Override
            <R> R accept(Visitor<R> visitor) {
              return visitor.visitSetIndexExpr(this);
            }
          
        ```
      
      * Next, I added  *getAt* and *setAt* methods to the Arrays.java class in order to get a value of an array at index i, and set a value v at index i of an array. 
      
          * ```java
                  Object getAt(int index) {
                      return theArray[index];
                  }
              ```
      
          * ```java
                  void setAt(int index, Object value) {
                      theArray[index] = value;
                  }
              ```
      
      * After this, I moved to the Parser to add bracket handling to the call() method in order to handle expressions
      
          * ```java
                  private Expr call() {
                      Expr expr = primary();
              
                      while (true) { 
                      if (match(LEFT_PAREN)) {
                          expr = finishCall(expr);
                      } else if (match(DOT)) {
                          Token name = consume(IDENTIFIER,
                              "Expect property name after '.'.");
                          expr = new Expr.Get(expr, name);
                      } *else if (match(LEFT_BRACKET)) { 
                          Expr index = expression();
                          consume(RIGHT_BRACKET, "Must have ']' after index.");
                          expr = new Expr.Index(expr, index);*
                      }   
                      else {
                          break;
                      }
                      }
              
                      return expr;
                  }
              
              ```
      
      * Next, to handle assigning a value v to index i of an array, we need to implement that case into the Parser's assignment method.
      
          * ```java
                  private Expr assignment() {
                      Expr expr = or();
              
                      if (match(EQUAL)) {
                          Token equals = previous(); // for error reporting
                          Expr value = assignment();
              
                          if (expr instanceof Expr.Variable) {
                              Token name = ((Expr.Variable) expr).name;
                              return new Expr.Assign(name, value);
                          } else if (expr instanceof Expr.Get) {
                              Expr.Get get = (Expr.Get)expr;
                              return new Expr.Set(get.object, get.name, value);
                          } *else if (expr instanceof Expr.Index) {
                              Expr.Index index = (Expr.Index)expr;
                              return new Expr.SetIndex(index.array, index.index, value);*
                          }
                          error(equals, "Invalid assignment target.");
                      }
              
                      return expr;
                  }
              
              ```
      
      * The resolver's methods for visitIndexExpr and visitSetIndexExpr get added next. Copilot auto completed these two, although I could not get the screenshot of its completion.
      
          * ```java
                  @Override
                  public Void visitIndexExpr(Expr.Index expr) {
                      resolve(expr.array);
                      resolve(expr.index);
                      return null;
                  }
              
                  @Override
                  public Void visitSetIndexExpr(Expr.SetIndex expr) {
                      resolve(expr.array);
                      resolve(expr.index);
                      resolve(expr.value);
                      return null;
                  }
              ```
      
      * Finally, we wire through our *visitIndexExpr* and *visitSetIndexExpr* through our interpreter
      
          * ```java
              
                  @Override
                  public Object visitIndexExpr(Expr.Index expr) {
                      Object array = evaluate(expr.array);
                      Object index = evaluate(expr.index);
              
                      if (!(array instanceof ArrayInstance)) {
                          throw new RuntimeError(null, "Only instances have indexing.");
                      }
                      if (!(index instanceof Double)) {
                          throw new RuntimeError(null, "Array index must be a number.");
                      }
              
                      ArrayInstance arr = (ArrayInstance) array;
                      int idx = ((Double)index).intValue();
                      return arr.getAt(idx);
                  }
              
              ```
      
          * ```java
                  @Override 
                  public Object visitSetIndexExpr(Expr.SetIndex expr) {
                      Object array = evaluate(expr.array);
                      Object index = evaluate(expr.index);
                      Object value = evaluate(expr.value);
              
                      if (!(array instanceof ArrayInstance)) {
                          throw new RuntimeError(null, "Only instances have indexing.");
                      }
                      if (!(index instanceof Double)) {
                          throw new RuntimeError(null, "Array index must be a number.");
                      }
              
                      ArrayInstance arr = (ArrayInstance) array;
                      int idx = ((Double)index).intValue();
                      arr.setAt(idx, value);
                      return value;
                  }
              
              ```
      
          * With these additions, I have implemented working arrays in Lox.
      
          * Testing was done through the console, with these example commands:
      
          * ```java
              var a = Array(3); // initialize an array of size 3
              a[0] = "hello!"; // first element = string
              a[1] = 2; // second element = double
              print a[2]; // returns nil
              
              
              ```
      
          * 
      

