package com.zekumoru.lox;

import java.util.ArrayList;
import java.util.List;

class Environment {
    final Environment enclosing;

    // The Environment class works tightly with the Resolver
    // to ensure that the values are indexed properly.
    private final List<Object> values = new ArrayList<>();

    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    void define(int depth, Object value) {
        ancestor(depth).values.add(value);
    }

    void assign(int depth, int index, Object value) {
        ancestor(depth).values.set(index, value);
    }

    Object get(int depth, int index) {
        return ancestor(depth).values.get(index);
    }

    Environment ancestor(int depth) {
        Environment environment = this;
        for (int i = 0; i < depth; i++) {
            assert environment != null;
            environment = environment.enclosing;
        }
        return environment;
    }
}
