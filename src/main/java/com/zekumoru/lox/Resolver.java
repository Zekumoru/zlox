package com.zekumoru.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void>, ClassMember.Visitor<Void> {
    private enum FunctionType {
        NONE,
        FUNCTION,
        ANONYMOUS_FUNCTION,
        INITIALIZER,
        METHOD,
        CLASS_METHOD, // Static method.
        GETTER_METHOD,
    }

    private enum ClassType {
        NONE,
        CLASS,
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
    private ClassType currentClass = ClassType.NONE;
    private Scope classScope = null;

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

    private void resolve(ClassMember member) {
        member.accept(this);
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
        ScopeRef ref = refs.get(name.lexeme);
        if (ref != null) {
            Lox.error(name, "Already an identifier with this name in " + (scopes.size() == 1 ? "global" : "this") + " scope.");
            refs.put(name.lexeme, new ScopeRef(name, refs.size(), ref.defined, ref.initialized, ref.used));
        } else {
            refs.put(name.lexeme, new ScopeRef(name, refs.size(), false, false, false));
        }

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

    private void bind(Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            ScopeRef ref = scopes.get(i).refs.get(name.lexeme);
            if (ref != null) {
                interpreter.bind(name, scopes.size() - i - 1, ref.index);
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

        if (currentFunction == FunctionType.INITIALIZER) classScope = scopes.peek();
        beginScope();
        for (Token param : params) {
            declare(param);
            define(param);
            initialize(param);
            use(param);
            bind(param);
            if (classScope != null) classScope.refs.put(param.lexeme, new ScopeRef(param, -1, true, true, true));
        }
        resolveBody(body);
        endScope();
        classScope = null;

        currentFunction = enclosingFunction;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        ScopeRef ref = scopes.peek().refs.get(expr.name.lexeme);
        if (ref != null) initialize(expr.name);
        use(expr.name);
        bind(expr.name);
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
    public Void visitGetExpr(Expr.Get expr) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            ScopeRef ref = scopes.get(i).refs.get(expr.name.lexeme);
            if (ref != null) ref.used = true;
        }
        resolve(expr.object);
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
    public Void visitSetExpr(Expr.Set expr) {
        resolve(expr.object);
        resolve(expr.value);
        // Make sure that 'this' variables in the initializer are defined.
        if (classScope != null) classScope.refs.put(expr.name.lexeme, new ScopeRef(expr.name, -1, true, true, true));
        return null;
    }

    @Override
    public Void visitThisExpr(Expr.This expr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'this' outside of a class.");
            return null;
        }

        bind(expr.keyword);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitPostfixExpr(Expr.Postfix expr) {
        resolve(expr.left);
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

        bind(expr.name);
        return null;
    }

    private boolean isInOuterScope(Expr.Variable expr) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            ScopeRef ref = scopes.get(i).refs.get(expr.name.lexeme);
            if (ref != null) {
                ref.used = true;
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
    public Void visitClassStmt(Stmt.Class stmt) {
        ClassType enclosingClass = currentClass;
        currentClass = ClassType.CLASS;

        declare(stmt.name);
        define(stmt.name);

        beginScope();
        Scope scope = scopes.peek();
        scope.refs.put("this", new ScopeRef(null, scope.refs.size(), true, true, true));

        for (ClassMember member : stmt.members) {
            resolve(member);
        }

        endScope();
        currentClass = enclosingClass;

        initialize(stmt.name);
        bind(stmt.name);
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
        bind(stmt.name);
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
            if (currentFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword, "Can't return a value from an initializer.");
            }

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
        bind(stmt.name);
        return null;
    }

    @Override
    public Void visitLoopStmt(Stmt.Loop stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        if (stmt.increment != null) resolve(stmt.increment);
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

    @Override
    public Void visitFieldClassMember(ClassMember.Field member) {
        declare(member.name);
        if (member.initializer != null) {
            resolve(member.initializer);
            initialize(member.name);
        }
        define(member.name);
        bind(member.name);
        return null;
    }

    @Override
    public Void visitMethodClassMember(ClassMember.Method member) {
        FunctionType declaration;
        Stmt.Function method = member.method;

        if (member.isStatic) {
            declaration = FunctionType.CLASS_METHOD;
            if (method.name.lexeme.equals("init")) {
                Lox.error(method.name, "Can't use initializer as a static method.");
            }
        } else {
            declaration = FunctionType.METHOD;
            if (method.name.lexeme.equals("init")) {
                declaration = FunctionType.INITIALIZER;
            }
        }

        resolveFunction(method, declaration);
        return null;
    }

    @Override
    public Void visitGetterClassMember(ClassMember.Getter member) {
        FunctionType declaration;
        Stmt.Function method = member.method;

        if (member.isStatic) {
            declaration = FunctionType.CLASS_METHOD;
            if (method.name.lexeme.equals("init")) {
                Lox.error(method.name, "Can't use initializer as a static getter.");
            }
        } else {
            declaration = FunctionType.GETTER_METHOD;
            if (method.name.lexeme.equals("init")) {
                Lox.error(method.name, "Can't use initializer as a getter.");
            }
        }

        resolveFunction(method, declaration);
        return null;
    }
}
