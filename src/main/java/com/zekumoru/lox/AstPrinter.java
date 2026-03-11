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
    public String visitCallExpr(Expr.Call expr) {
        return parenthesize(expr.callee.accept(this), expr.arguments.toArray(Expr[]::new));
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

    @Override
    public String visitFunctionExpr(Expr.Function expr) {
        return parenthesize("fun-expr (" + parameters(expr.params) + ")");
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
    public String visitEmptyStmt(Stmt.Empty stmt) {
        return "";
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
    public String visitFunctionStmt(Stmt.Function stmt) {
        return parenthesize("fun " + stmt.name.lexeme + " (" + parameters(stmt.params) + ")");
    }

    private String parameters(List<Token> params) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < params.size(); i++) {
            builder.append(params.get(i).lexeme);
            if (i < params.size() - 1) builder.append(", ");
        }
        return builder.toString();
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
    public String visitReturnStmt(Stmt.Return stmt) {
        return parenthesize("return", stmt.value);
    }

    @Override
    public String visitVarStmt(Stmt.Var stmt) {
        return parenthesize("var " + stmt.name.lexeme, stmt.initializer);
    }

    @Override
    public String visitLoopStmt(Stmt.Loop stmt) {
        String condition = stmt.condition.accept(this);
        String increment = stmt.increment.accept(this);
        String body = stmt.body.accept(this);
        return parenthesize("while " + condition + " " + body + " " + increment);
    }

    @Override
    public String visitBreakStmt(Stmt.Break stmt) {
        return parenthesize("break");
    }

    @Override
    public String visitContinueStmt(Stmt.Continue stmt) {
        return parenthesize("continue");
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
