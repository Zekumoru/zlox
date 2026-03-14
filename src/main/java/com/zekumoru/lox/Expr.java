package com.zekumoru.lox;

import java.util.List;

abstract class Expr {
    interface Visitor<R> {
        R visitAssignExpr(Assign expr);
        R visitBinaryExpr(Binary expr);
        R visitCallExpr(Call expr);
        R visitGroupingExpr(Grouping expr);
        R visitLiteralExpr(Literal expr);
        R visitLogicalExpr(Logical expr);
        R visitUnaryExpr(Unary expr);
        R visitPostfixExpr(Postfix expr);
        R visitConditionalExpr(Conditional expr);
        R visitVariableExpr(Variable expr);
        R visitFunctionExpr(Function expr);
    }
    
    public static class Assign extends Expr {
        final Token name;
        final Token operator;
        final Expr value;
        
        Assign(Token name, Token operator, Expr value) {
            this.name = name;
            this.operator = operator;
            this.value = value;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitAssignExpr(this);
        }
    }
    
    public static class Binary extends Expr {
        final Expr left;
        final Token operator;
        final Expr right;
        
        Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinaryExpr(this);
        }
    }
    
    public static class Call extends Expr {
        final Expr callee;
        final Token paren;
        final List<Expr> arguments;
        
        Call(Expr callee, Token paren, List<Expr> arguments) {
            this.callee = callee;
            this.paren = paren;
            this.arguments = arguments;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitCallExpr(this);
        }
    }
    
    public static class Grouping extends Expr {
        final Expr expression;
        
        Grouping(Expr expression) {
            this.expression = expression;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }
    }
    
    public static class Literal extends Expr {
        final Object value;
        
        Literal(Object value) {
            this.value = value;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }
    }
    
    public static class Logical extends Expr {
        final Expr left;
        final Token operator;
        final Expr right;
        
        Logical(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLogicalExpr(this);
        }
    }
    
    public static class Unary extends Expr {
        final Token operator;
        final Expr right;
        
        Unary(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }
    }
    
    public static class Postfix extends Expr {
        final Expr left;
        final Token operator;
        
        Postfix(Expr left, Token operator) {
            this.left = left;
            this.operator = operator;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitPostfixExpr(this);
        }
    }
    
    public static class Conditional extends Expr {
        final Expr condition;
        final Expr thenBranch;
        final Expr elseBranch;
        
        Conditional(Expr condition, Expr thenBranch, Expr elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitConditionalExpr(this);
        }
    }
    
    public static class Variable extends Expr {
        final Token name;
        
        Variable(Token name) {
            this.name = name;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitVariableExpr(this);
        }
    }
    
    public static class Function extends Expr {
        final List<Token> params;
        final List<Stmt> body;
        
        Function(List<Token> params, List<Stmt> body) {
            this.params = params;
            this.body = body;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitFunctionExpr(this);
        }
    }
    
    abstract <R> R accept(Visitor<R> visitor);
}
