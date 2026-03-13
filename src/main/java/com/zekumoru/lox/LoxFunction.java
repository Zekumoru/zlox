package com.zekumoru.lox;

import java.util.List;

abstract class LoxFunction implements LoxCallable {
    private final Environment closure;

    LoxFunction(Environment closure) {
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
            environment.define(parameters().get(i).lexeme, arguments.get(i));
        }

        try {
            interpreter.executeBlock(body(), environment);
        } catch (Return returnValue) {
            return returnValue.value;
        }

        return null;
    }
}
