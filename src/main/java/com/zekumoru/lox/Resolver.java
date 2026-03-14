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

    private static class ScopeRef {
        final Token name;
        final int index;
        boolean defined;
        boolean initialized;
        boolean used;

        ScopeRef(Token name, int index, boolean defined, boolean initialized, boolean used) {
            this.name = name;
            this.index = index;
            this.defined = defined;
            this.initialized = initialized;
            this.used = used;
        }
    }

    private static class Scope {
        final Map<String, ScopeRef> refs = new HashMap<>();
    }

    private final Interpreter interpreter;
    private final Globals globals;
    private final Stack<Scope> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;

    Resolver(Globals globals, Interpreter interpreter) {
        this.globals = globals;
        this.interpreter = interpreter;
    }

    public void resolve(List<Stmt> statements) {
        // Initialize global scope.
        beginScope();
        for (int i = 0; i < globals.functions().size(); i++) {
            Globals.Function function = globals.functions().get(i);
            scopes.firstElement().refs.put(function.name(), new ScopeRef(null, i, true, true, true));
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
        scopes.push(new Scope());
    }

    private void endScope() {
        for (ScopeRef scopeRef : scopes.peek().refs.values()) {
            if (!scopeRef.used) {
                // Assert name is always available since native functions
                // or globals are always already "used".
                assert scopeRef.name != null;
                Lox.error(scopeRef.name, "Unused " + (scopes.size() == 1 ? "global" : "local") + " identifier.");
            }
        }

        scopes.pop();
    }

    private void declare(Token name) {
        Map<String, ScopeRef> refs = scopes.peek().refs;
        if (refs.containsKey(name.lexeme)) {
            Lox.error(name, "Already an identifier with this name in " + (scopes.size() == 1 ? "global" : "this") + " scope.");
        }

        refs.put(name.lexeme, new ScopeRef(name, refs.size(), false, false, false));
    }

    private void define(Token name) {
        scopes.peek().refs.get(name.lexeme).defined = true;
    }

    private void initialize(Token name) {
        scopes.peek().refs.get(name.lexeme).initialized = true;
    }

    private void use(Token name) {
        scopes.peek().refs.get(name.lexeme).used = true;
    }

    private void bind(Object exprOrStmt, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).refs.containsKey(name.lexeme)) {
                interpreter.bind(exprOrStmt, scopes.size() - i - 1);
                return;
            }
        }
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
            use(param);
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
        ScopeRef scopeRef = scopes.peek().refs.get(expr.name.lexeme);
        if (scopeRef == null && !isInOuterScope(expr)) {
            Lox.error(expr.name, "Identifier is not defined in " + (scopes.size() == 1 ? "global" : "this") + " scope.");
        }

        if (scopeRef != null) {
            use(expr.name);

            if (!scopeRef.defined) {
                Lox.error(expr.name, "Can't read " + (scopes.size() == 1 ? "global" : "local") + " identifier in its own initializer.");
            } else if (!scopeRef.initialized) {
                Lox.error(expr.name, "Can't read uninitialized " + (scopes.size() == 1 ? "global" : "local") + " identifier.");
            }
        }

        bind(expr, expr.name);
        return null;
    }

    private boolean isInOuterScope(Expr.Variable expr) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Scope scope = scopes.get(i);
            if (scope.refs.containsKey(expr.name.lexeme)) {
                return true;
            }
        }

        return false;
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
        bind(stmt, stmt.name);
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
