package com.zekumoru.lox;

import java.util.List;

abstract class LoxFunction implements LoxCallable {
    protected final Environment closure;
    protected final boolean isInitializer;
    protected final boolean isGetter;

    LoxFunction(Environment closure, boolean isInitializer, boolean isGetter) {
        this.isInitializer = isInitializer;
        this.isGetter = isGetter;
        this.closure = closure;
    }

    abstract List<Token> parameters();

    abstract List<Stmt> body();

    @Override
    public int arity() {
        return parameters().size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < parameters().size(); i++) {
            Interpreter.BindingRef ref = interpreter.bindings.get(parameters().get(i));
            assert ref != null;
            environment.define(ref.depth(), arguments.get(i));
        }

        try {
            interpreter.executeBlock(body(), environment);
        } catch (Return returnValue) {
            if (isInitializer) return closure.get(0, 0);

            return returnValue.value;
        }

        if (isInitializer) return closure.get(0, 0);

        return null;
    }
}
