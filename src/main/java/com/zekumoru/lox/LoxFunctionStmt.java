package com.zekumoru.lox;

import java.util.List;

class LoxFunctionStmt extends LoxFunction {
    private final Stmt.Function declaration;

    LoxFunctionStmt(Stmt.Function declaration, Environment closure) {
        super(closure);
        this.declaration = declaration;
    }

    LoxFunctionStmt bind(LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define(0, instance);
        return new LoxFunctionStmt(declaration, environment);
    }

    @Override
    List<Token> parameters() {
        return declaration.params;
    }

    @Override
    List<Stmt> body() {
        return declaration.body;
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}
