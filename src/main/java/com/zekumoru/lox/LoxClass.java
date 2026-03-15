package com.zekumoru.lox;

import java.util.List;
import java.util.Map;

class LoxClass extends LoxInstance implements LoxCallable {
    final String name;
    private final Map<String, LoxFunctionStmt> methods;

    LoxClass(LoxClass metaclass, String name, Map<String, LoxFunctionStmt> methods) {
        super(metaclass);
        this.name = name;
        this.methods = methods;
    }

    LoxFunctionStmt findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        return null;
    }

    @Override
    public int arity() {
        LoxFunctionStmt initializer = findMethod("init");
        if (initializer == null) return 0;
        return initializer.arity();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);
        LoxFunctionStmt initializer = findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }

    @Override
    public String toString() {
        return name;
    }
}
