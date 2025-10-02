
package lox;

import java.util.List;

class ArrayClass extends LoxClass {

    ArrayClass() {
        super("Array", null, null);
    }

    LoxFunction findMethod(String name) {
        return null;
    }

    @Override
    public Object call(Interpreter interpreter,
                        List<Object> arguments) {
        Object arg = arguments.get(0);
        if (! (arg instanceof Double)) {
            throw new RuntimeError(
                new Token(null, "", arg, -1),
                "Array length expected, got: " + arg);
        }
        int length = (int)((double)arg);
        return new ArrayInstance(this, length);
    }

    @Override
    public int arity() {
        return 1;
    }
}

class ArrayInstance extends LoxInstance {
    private int length;
    private Object[] theArray;
    private final LoxCallable thePeekMethod =   // how do local anonymous classes work
        new LoxCallable() {
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Object arg = arguments.get(0);
                // TODO: check that it's a number
                int index = (int)((double)arg);
                // TODO: check that 0 <= index < length
                return theArray[index];
            }
            
        };

    ArrayInstance(ArrayClass klass, int length) {
        super( klass );
        this.length = length;
        this.theArray = new Object[length];
    }

    @Override
    Object get(Token name) {
        if ("length".equals(name.lexeme)) {
            return this.length;
        } else if ("peek".equals(name.lexeme)) {
            return thePeekMethod;
        } else {
            return super.get(name);
        }
    }

    void set(Token name, Object value) {
        return;
    }

    int length() { 
        return length;
     }
    Object getAt(int index) {
        return theArray[index];
    }
    void setAt(int index, Object value) {
        theArray[index] = value;
    }

}

