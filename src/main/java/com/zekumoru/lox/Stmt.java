package com.zekumoru.lox;

import java.util.List;

abstract class Stmt {
    interface Visitor<R> {
        R visitEmptyStmt(Empty stmt);
        R visitBlockStmt(Block stmt);
        R visitClassStmt(Class stmt);
        R visitExpressionStmt(Expression stmt);
        R visitFunctionStmt(Function stmt);
        R visitIfStmt(If stmt);
        R visitReturnStmt(Return stmt);
        R visitVarStmt(Var stmt);
        R visitLoopStmt(Loop stmt);
        R visitBreakStmt(Break stmt);
        R visitContinueStmt(Continue stmt);
    }
    
    public static class Empty extends Stmt {
        final Token token;
        
        Empty(Token token) {
            this.token = token;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitEmptyStmt(this);
        }
    }
    
    public static class Block extends Stmt {
        final List<Stmt> statements;
        
        Block(List<Stmt> statements) {
            this.statements = statements;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitBlockStmt(this);
        }
    }
    
    public static class Class extends Stmt {
        final Token name;
        final List<Stmt.Function> methods;
        final List<Stmt.Function> classMethods;
        
        Class(Token name, List<Stmt.Function> methods, List<Stmt.Function> classMethods) {
            this.name = name;
            this.methods = methods;
            this.classMethods = classMethods;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitClassStmt(this);
        }
    }
    
    public static class Expression extends Stmt {
        final Expr expression;
        
        Expression(Expr expression) {
            this.expression = expression;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitExpressionStmt(this);
        }
    }
    
    public static class Function extends Stmt {
        final Token name;
        final List<Token> params;
        final List<Stmt> body;
        
        Function(Token name, List<Token> params, List<Stmt> body) {
            this.name = name;
            this.params = params;
            this.body = body;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitFunctionStmt(this);
        }
    }
    
    public static class If extends Stmt {
        final Expr condition;
        final Stmt thenBranch;
        final Stmt elseBranch;
        
        If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitIfStmt(this);
        }
    }
    
    public static class Return extends Stmt {
        final Token keyword;
        final Expr value;
        
        Return(Token keyword, Expr value) {
            this.keyword = keyword;
            this.value = value;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitReturnStmt(this);
        }
    }
    
    public static class Var extends Stmt {
        final Token name;
        final Expr initializer;
        
        Var(Token name, Expr initializer) {
            this.name = name;
            this.initializer = initializer;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitVarStmt(this);
        }
    }
    
    public static class Loop extends Stmt {
        final Expr condition;
        final Expr increment;
        final Stmt body;
        
        Loop(Expr condition, Expr increment, Stmt body) {
            this.condition = condition;
            this.increment = increment;
            this.body = body;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLoopStmt(this);
        }
    }
    
    public static class Break extends Stmt {
        final Token keyword;
        
        Break(Token keyword) {
            this.keyword = keyword;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitBreakStmt(this);
        }
    }
    
    public static class Continue extends Stmt {
        final Token keyword;
        
        Continue(Token keyword) {
            this.keyword = keyword;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitContinueStmt(this);
        }
    }
    
    abstract <R> R accept(Visitor<R> visitor);
}
