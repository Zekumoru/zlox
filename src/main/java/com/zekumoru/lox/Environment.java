package com.zekumoru.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    Object get(Token name) {
        return getAt(0, name);
    }

    void assign(Token name, Object value) {
        assignAt(0, name, value);
    }

    void define(String name, Object value) {
        values.put(name, value);
    }

    Environment ancestor(int distance, Token name) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            if (environment == null) {
                throw new RuntimeError(name,"Encountered a missing environment.");
            }

            environment = environment.enclosing;
        }
        return environment;
    }

    Object getAt(int distance, Token name) {
        return ancestor(distance, name).values.get(name.lexeme);
    }

    void assignAt(int distance, Token name, Object value) {
        ancestor(distance, name).values.put(name.lexeme, value);
    }
}
