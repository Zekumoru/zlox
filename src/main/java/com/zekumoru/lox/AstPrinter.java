package com.zekumoru.lox;

import java.util.List;

public class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
    String print(Expr expr) {
        return expr.accept(this);
    }

    String print(Stmt stmt) {
        return stmt.accept(this);
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
    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
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

    private String block(List<Stmt> statements) {
        StringBuilder builder = new StringBuilder();

        builder.append("(block");
        for (Stmt statement : statements) {
            builder.append(" ");
            builder.append(print(statement));
        }
        builder.append(")");

        return builder.toString();
    }

    @Override
    public String visitBlockStmt(Stmt.Block stmt) {
        return block(stmt.statements);
    }

    @Override
    public String visitExpressionStmt(Stmt.Expression stmt) {
        return parenthesize("expr", stmt.expression);
    }

    @Override
    public String visitIfStmt(Stmt.If stmt) {
        String condition = stmt.condition.accept(this);
        String thenBranch = stmt.thenBranch.accept(this);
        String elseBranch = "()";
        if (stmt.elseBranch != null) elseBranch = stmt.elseBranch.accept(this);
        return parenthesize("if " + condition + " " + thenBranch + " " + elseBranch);
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
    public String visitWhileStmt(Stmt.While stmt) {
        String condition = stmt.condition.accept(this);
        String body = stmt.body.accept(this);
        return parenthesize("while " + condition + " " + body);
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
