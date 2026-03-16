package com.zekumoru.lox;

import java.util.HashMap;
import java.util.Map;

class LoxInstance {
    protected final LoxClass klass;
    private final Map<String, Object> fields = new HashMap<>();

    LoxInstance(LoxClass klass) {
        this.klass = klass;

        if (klass instanceof LoxInstance instance) {
            this.fields.putAll(instance.fields);
        }
    }

    Object get(Token name) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        if (this instanceof LoxClass) {
            return ((LoxClass)this).findMethod(name.lexeme);
        }

        LoxFunctionStmt method = klass.findMethod(name.lexeme);
        if (method != null) return method.bind(this);

        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
    }

    void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }

    @Override
    public String toString() {
        return klass.name + " instance";
    }
}
