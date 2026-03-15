package com.zekumoru.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    record BindingRef(int depth, int index) {}

    final static Object NO_PRINT = new Object();

    final Environment globals = new Environment();
    private Environment environment = globals;
    final Map<Token, BindingRef> bindings = new HashMap<>();

    Interpreter(Globals globals) {
        for (Globals.Function function : globals.functions()) {
            this.globals.define(0, function.callable());
        }
    }

    void interpretRepl(List<Stmt> statements) {
        try {
            // Print the last expression statement's result.
            String result = null;

            for (Stmt statement : statements) {
                if (statement instanceof Stmt.Expression) {
                    Object value = evaluate(((Stmt.Expression)statement).expression);
                    if (value != NO_PRINT) result = stringify(value);
                } else {
                    execute(statement);
                }
            }

            if (result != null) {
                System.out.println(result);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
            case GREATER:
                checkComparisonOperands(expr.operator, left, right);

                if (left instanceof String && right instanceof String) {
                    return ((String) left).compareTo((String) right) > 0;
                }

                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkComparisonOperands(expr.operator, left, right);

                if (left instanceof String && right instanceof String) {
                    return ((String) left).compareTo((String) right) >= 0;
                }

                return (double)left >= (double) right;
            case LESS:
                checkComparisonOperands(expr.operator, left, right);

                if (left instanceof String && right instanceof String) {
                    return ((String) left).compareTo((String) right) < 0;
                }

                return (double)left < (double)right;
            case LESS_EQUAL:
                checkComparisonOperands(expr.operator, left, right);

                if (left instanceof String && right instanceof String) {
                    return ((String) left).compareTo((String) right) <= 0;
                }

                return (double)left <= (double) right;
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }

                if (left instanceof String || right instanceof String) {
                    return stringify(left) + stringify(right);
                }

                throw new RuntimeError(expr.operator, "Operands must be two numbers or at least one string.");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);

                if ((double)right == 0.0) {
                    throw new RuntimeError(expr.operator, "Division by zero.");
                }

                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case PERCENT:
                checkNumberOperands(expr.operator, left, right);
                return (double)left % (double)right;
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LoxCallable function)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " +
                    function.arity() + " arguments but got " +
                    arguments.size() + ".");
        }

        try {
            return function.call(this, arguments);
        } catch (CallError error) {
            throw new RuntimeError(expr.paren, error.getMessage());
        }
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof LoxInstance) {
            return ((LoxInstance)object).get(expr.name);
        }

        throw new RuntimeError(expr.name, "Only instances have properties.");
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);

        if (!(object instanceof LoxInstance)) {
            throw new RuntimeError(expr.name, "Only instances have fields.");
        }

        Object value = evaluate(expr.value);
        ((LoxInstance)object).set(expr.name, value);
        return null;
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr.keyword);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
            case PLUS_PLUS:
            case MINUS_MINUS:
                if (!(expr.right instanceof Expr.Variable var)) {
                    throw new RuntimeError(expr.operator, "Prefix expression must be a variable.");
                }

                checkNumberOperand(expr.operator, right);
                double value = (double)right;
                BindingRef ref = bindings.get(var.name);

                switch (expr.operator.type) {
                    case PLUS_PLUS: value++; break;
                    case MINUS_MINUS: value--; break;
                }

                environment.assign(ref.depth, ref.index, value);
                return value;
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitPostfixExpr(Expr.Postfix expr) {
        if (!(expr.left instanceof Expr.Variable var)) {
            throw new RuntimeError(expr.operator, "Postfix expression must be a variable.");
        }

        Object left = evaluate(expr.left);
        checkNumberOperand(expr.operator, left);
        double value = (double)left;
        final double prevValue = value;
        BindingRef ref = bindings.get(var.name);

        switch (expr.operator.type) {
            case PLUS_PLUS: value++; break;
            case MINUS_MINUS: value--; break;
        }

        environment.assign(ref.depth, ref.index, value);
        return prevValue;
    }

    @Override
    public Object visitConditionalExpr(Expr.Conditional expr) {
        if (isTruthy(evaluate(expr.condition))) return evaluate(expr.thenBranch);
        return evaluate(expr.elseBranch);
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name);
    }

    private Object lookUpVariable(Token name) {
        BindingRef ref = bindings.get(name);
        assert ref != null;
        return environment.get(ref.depth, ref.index);
    }

    @Override
    public Object visitFunctionExpr(Expr.Function expr) {
        return new LoxFunctionExpr(expr, environment);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand '" + stringify(operand) + "' must be a number");
    }

    private void checkComparisonOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        if (left instanceof String && right instanceof String) return;
        throw new RuntimeError(operator, "Operands must be two numbers or two strings.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private boolean isTruthy(Object object) {
        return switch (object) {
            case null -> false;
            case Boolean b -> (boolean) object;
            case String s -> !s.isEmpty();
            case Double v -> v != 0.0;
            default -> true;
        };
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void bind(Token token, int depth, int index) {
        bindings.put(token, new BindingRef(depth, index));
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;

        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitEmptyStmt(Stmt.Empty stmt) {
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        BindingRef ref = bindings.get(stmt.name);
        assert ref != null;
        environment.define(ref.depth, null);

        Map<String, LoxFunctionStmt> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            LoxFunctionStmt function = new LoxFunctionStmt(method, environment);
            methods.put(method.name.lexeme, function);
        }

        LoxClass klass = new LoxClass(stmt.name.lexeme, methods);
        environment.assign(ref.depth, ref.index, klass);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunctionStmt(stmt, environment);
        BindingRef ref = bindings.get(stmt.name);
        assert ref != null;
        environment.define(ref.depth, function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }

        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        throw new Return(value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        BindingRef ref = bindings.get(stmt.name);
        if (ref == null) return null;

        if (stmt.initializer != null) {
            Object value = evaluate(stmt.initializer);
            environment.define(ref.depth, value);
        } else {
            environment.define(ref.depth, null);
        }

        return null;
    }

    @Override
    public Void visitLoopStmt(Stmt.Loop stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            try {
                execute(stmt.body);
            } catch (ContinueSignal _) {
            } catch (BreakSignal signal) {
                break;
            }

            if (stmt.increment != null) {
                evaluate(stmt.increment);
            }
        }

        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new BreakSignal();
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        throw new ContinueSignal();
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        BindingRef ref = bindings.get(expr.name);

        assert ref != null;

        if (expr.operator.type == TokenType.EQUAL) {
            environment.assign(ref.depth, ref.index, value);
            return value;
        }

        if (value instanceof String) {
            Object prevValue = environment.get(ref.depth, ref.index);
            if (!(prevValue instanceof String newValue)) {
                throw new RuntimeError(expr.operator, "Variable '" + expr.name.lexeme + "' must be a string.");
            }
            newValue += value;
            environment.assign(ref.depth, ref.index, newValue);
            return newValue;
        }

        checkNumberOperand(expr.operator, value);
        Object prevValue = environment.get(ref.depth, ref.index);
        if (!(prevValue instanceof Double newValue)) {
            throw new RuntimeError(expr.operator, "Variable '" + expr.name.lexeme + "' must be a number.");
        }

        switch (expr.operator.type) {
            case PLUS_EQUAL: newValue += (double)value; break;
            case MINUS_EQUAL: newValue -= (double)value; break;
            case STAR_EQUAL: newValue *= (double)value; break;
            case SLASH_EQUAL: newValue /= (double)value; break;
            case PERCENT_EQUAL: newValue %= (double)value; break;
        }

        environment.assign(ref.depth, ref.index, newValue);
        return newValue;
    }
}
