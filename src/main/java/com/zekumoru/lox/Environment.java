package com.zekumoru.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
    // Used during initialization.
    private static final Object UNINITIALIZED = new Object();
    // Used for declared only variables.
    private static final Object UNDEFINED = new Object();

    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            Object value = values.get(name.lexeme);

            if (value == UNINITIALIZED) {
                throw new RuntimeError(name, "Cannot use '" + name.lexeme + "' before initialization.");
            }

            if (value == UNDEFINED) {
                throw new RuntimeError(name, "Variable '" + name.lexeme + "' must be initialized before use.");
            }

            return value;
        }

        if (enclosing != null) return enclosing.get(name);

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    void declare(Token name) {
        define(name, UNINITIALIZED);
    }

    void define(Token name) {
        define(name, UNDEFINED);
    }

    void define(Token name, Object value) {
        if (values.containsKey(name.lexeme) && values.get(name.lexeme) != UNINITIALIZED) {
            throw new RuntimeError(name, "'" + name.lexeme + "' is already defined.");
        }

        values.put(name.lexeme, value);
    }
}
