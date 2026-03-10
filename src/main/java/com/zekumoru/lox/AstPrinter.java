package com.zekumoru.lox;

public class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
    void print(Expr expr) {
        System.out.println(expr.accept(this));
    }

    void print(Stmt stmt) {
        System.out.println(stmt.accept(this));
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        return literal(expr.value);
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitConditionalExpr(Expr.Conditional expr) {
        return parenthesize("if", expr.condition, expr.thenBranch, expr.elseBranch);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme;
    }

    private String literal(Object value) {
        if (value == null) return "nil";
        if (value instanceof String) return "\"" + value + "\"";
        return value.toString();
    }

    @Override
    public String visitExpressionStmt(Stmt.Expression stmt) {
        return parenthesize("expr", stmt.expression);
    }

    @Override
    public String visitPrintStmt(Stmt.Print stmt) {
        return parenthesize("print", stmt.expression);
    }

    @Override
    public String visitVarStmt(Stmt.Var stmt) {
        return parenthesize("var " + stmt.name.lexeme, stmt.initializer);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return parenthesize("= " + expr.name.lexeme, expr.value);
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);
        for (Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");

        return builder.toString();
    }
}
