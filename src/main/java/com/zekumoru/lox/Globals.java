package com.zekumoru.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

class Globals {
    public record Function(String name, LoxCallable callable) {
        @Override
        public String toString() { return "<native fn>"; }
    }

    private final List<Function> functions = new ArrayList<>();

    Globals() {
        functions.add(new Function("clock", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis() / 1000.0;
            }
        }));

        functions.add(new Function("sleep", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Object value = arguments.getFirst();
                if (!(value instanceof Double)) {
                    throw new CallError("Argument must be a number.");
                }

                try {
                    Thread.sleep(Math.round((Double) value));
                } catch (InterruptedException error) {
                    throw new CallError("Cannot perform sleep.");
                }

                return null;
            }
        }));

        functions.add(new Function("print", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                System.out.println(interpreter.stringify(arguments.getFirst()));
                return Interpreter.NO_PRINT;
            }
        }));

        functions.add(new Function("input", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                InputStreamReader input = new InputStreamReader(System.in);
                BufferedReader reader = new BufferedReader(input);
                System.out.print(arguments.getFirst());

                try {
                    return reader.readLine();
                } catch (IOException error) {
                    throw new CallError("Cannot read input.");
                }
            }
        }));

        functions.add(new Function("number", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Object value = arguments.getFirst();

                try {
                    return Double.parseDouble(value.toString());
                } catch (NumberFormatException error) {
                    throw new CallError("Cannot parse argument to number.");
                }
            }
        }));

        functions.add(new Function("random", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return Math.random();
            }
        }));

        functions.add(new Function("round", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Object value = arguments.getFirst();
                if (!(value instanceof Double)) {
                    throw new CallError("Argument must be a number to round.");
                }
                return Math.round((Double) value);
            }
        }));

        functions.add(new Function("floor", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Object value = arguments.getFirst();
                if (!(value instanceof Double)) {
                    throw new CallError("Argument must be a number to floor.");
                }
                return Math.floor((Double) value);
            }
        }));

        functions.add(new Function("ceil", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Object value = arguments.getFirst();
                if (!(value instanceof Double)) {
                    throw new CallError("Argument must be a number to ceil.");
                }
                return Math.ceil((Double) value);
            }
        }));
    }

    public List<Function> functions() {
        return functions;
    }
}