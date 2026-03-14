package com.zekumoru.lox;

import java.util.ArrayList;
import java.util.List;

class Environment {
    final Environment enclosing;

    record Temp (String name, Object value) {
        @Override public String toString() { return name; }
    }

    // The Environment class works tightly with the Resolver
    // to ensure that the values are indexed properly.
    private final List<Temp> values = new ArrayList<>();

    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    void define(int depth, String name, Object value) {
        var env = ancestor(depth).values;
        System.out.println("Adding '" + name + "' to a scope with lexical depth " + depth + " and index of " + env.size() + ".");
        env.add(new Temp(name, value));
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
