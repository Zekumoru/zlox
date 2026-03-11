package com.zekumoru.lox;

import java.util.List;

class LoxFunctionExpr extends LoxFunction {
    private final Expr.Function expression;

    LoxFunctionExpr(Expr.Function expression, Environment closure) {
        super(closure);
        this.expression = expression;
    }

    @Override
    List<Token> parameters() {
        return expression.params;
    }

    @Override
    List<Stmt> body() {
        return expression.body;
    }

    @Override
    public String toString() {
        return "<fn anonymous>";
    }
}
