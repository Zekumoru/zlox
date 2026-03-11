package com.zekumoru.lox;

import java.util.List;

class LoxFunctionStmt extends LoxFunction {
    private final Stmt.Function declaration;

    LoxFunctionStmt(Stmt.Function declaration, Environment closure) {
        super(closure);
        this.declaration = declaration;
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
