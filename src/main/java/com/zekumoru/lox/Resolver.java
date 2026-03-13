package com.zekumoru.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private enum FunctionType {
        NONE,
        FUNCTION,
        ANONYMOUS_FUNCTION,
    }

    private static class Scope {
        final Token name;
        boolean defined;
        boolean initialized;
        boolean used;

        Scope(Token name, boolean defined, boolean initialized, boolean used) {
            this.name = name;
            this.defined = defined;
            this.initialized = initialized;
            this.used = used;
        }
    }

    private final Interpreter interpreter;
    private final Globals globals;
    private final Stack<Map<String, Scope>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;

    Resolver(Globals globals, Interpreter interpreter) {
        this.globals = globals;
        this.interpreter = interpreter;
    }

    public void resolve(List<Stmt> statements) {
        // Initialize global scope.
        beginScope();
        for (Globals.Function function : globals.functions()) {
            scopes.firstElement().put(function.name(), new Scope(null, true, true, true));
        }

        for (Stmt statement : statements) {
            resolve(statement);
        }

        endScope();
    }

    private void resolveBody(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    private void endScope() {
        for (Scope scope : scopes.peek().values()) {
            if (!scope.used) {
                // Assert name is always available since native functions
                // or globals are always already "used".
                assert scope.name != null;
                Lox.error(scope.name, "Unused " + (scopes.size() == 1 ? "global" : "local") + " identifier.");
            }
        }

        scopes.pop();
    }

    private void declare(Token name) {
        Map<String, Scope> scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "Already an identifier with this name in " + (scopes.size() == 1 ? "global" : "this") + " scope.");
        }

        scope.put(name.lexeme, new Scope(name, false, false, false));
    }

    private void define(Token name) {
        scopes.peek().get(name.lexeme).defined = true;
    }

    private void initialize(Token name) {
        scopes.peek().get(name.lexeme).initialized = true;
    }

    private void bind(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.bind(expr, scopes.size() - i - 1);
                return;
            }
        }
    }

    private boolean inGlobal(String name) {
        return scopes.firstElement().containsKey(name);
    }

    private void resolveFunction(Stmt.Function function, FunctionType type) {
        resolveFunction(function.params, function.body, type);
    }

    private void resolveFunction(Expr.Function function, FunctionType type) {
        resolveFunction(function.params, function.body, type);
    }

    private void resolveFunction(List<Token> params, List<Stmt> body, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();
        for (Token param : params) {
            declare(param);
            define(param);
            initialize(param);
        }
        resolveBody(body);
        endScope();

        currentFunction = enclosingFunction;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        initialize(expr.name);
        bind(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for (Expr argument : expr.arguments) {
            resolve(argument);
        }
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitConditionalExpr(Expr.Conditional expr) {
        resolve(expr.condition);
        resolve(expr.thenBranch);
        resolve(expr.elseBranch);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        Scope scope = scopes.peek().get(expr.name.lexeme);
        if (!(inGlobal(expr.name.lexeme) || scope != null)) {
            Lox.error(expr.name, "Identifier is not defined in " + (scopes.size() == 1 ? "global" : "this") + " scope.");
        }

        assert scope != null;
        scope.used = true;
        if (!scope.defined) {
            Lox.error(expr.name, "Can't read " + (scopes.size() == 1 ? "global" : "local") + " identifier in its own initializer.");
        }

        if (!scope.initialized) {
            Lox.error(expr.name, "Can't read uninitialized " + (scopes.size() == 1 ? "global" : "local") + " identifier.");
        }

        bind(expr, expr.name);
        return null;
    }

    @Override
    public Void visitFunctionExpr(Expr.Function expr) {
        resolveFunction(expr, FunctionType.ANONYMOUS_FUNCTION);
        return null;
    }

    @Override
    public Void visitEmptyStmt(Stmt.Empty stmt) {
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolveBody(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt, FunctionType.FUNCTION);
        initialize(stmt.name);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.");
        }

        if (stmt.value != null) {
            resolve(stmt.value);
        }

        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
            initialize(stmt.name);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitLoopStmt(Stmt.Loop stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        resolve(stmt.increment);
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        return null;
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        return null;
    }
}
