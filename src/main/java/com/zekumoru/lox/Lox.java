package com.zekumoru.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

public class Lox {
    private static final Globals globals = new Globals();
    private static final Interpreter interpreter = new Interpreter(globals);
    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            System.setErr(System.out); // Fix logging order when in REPL.
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // Indicate an error in the exit code.
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);
        StringBuilder buffer = new StringBuilder();

        while (true) {
            System.out.print(buffer.isEmpty() ? "> " : "  ");
            String line = reader.readLine();
            if (line == null) break;
            if (line.trim().equals("exit")) break;

            buffer.append(line).append("\n");

            if (!isCompleteSource(buffer.toString())) {
                continue;
            }

            run(buffer.toString(), Lox::insertSemicolon);
            buffer.setLength(0);
            System.err.flush();
            hadError = false;
        }
    }

    private static void run(String source) {
        run(source, null);
    }

    private static void run(String source, Consumer<List<Token>> consumer) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        if (consumer != null) consumer.accept(tokens);

        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Stop if there was a syntax error.
        if (hadError) return;

        Resolver resolver = new Resolver(globals, interpreter);
        resolver.resolve(statements);

        AstPrinter printer = new AstPrinter();
        System.out.println(printer.print(statements.getFirst()));

        // Stop if there was a resolution error.
        if (hadError) return;

        // Consumer is used as a flag to check if in REPL.
        if (consumer != null) {
            interpreter.interpretRepl(statements);
        } else {
            interpreter.interpret(statements);
        }
    }

    private static boolean isCompleteSource(String source) {
        int parens = 0;
        int braces = 0;
        int brackets = 0;
        boolean inString = false;

        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);

            // Note that escape is not implemented yet.
            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (inString) continue;

            switch (c) {
                case '(': parens++; break;
                case ')': parens--; break;
                case '{': braces++; break;
                case '}': braces--; break;
                case '[': brackets++; break;
                case ']': brackets--; break;
            }
        }

        if (inString) return false;
        if (parens > 0 || braces > 0 || brackets > 0) return false;

        String trimmed = source.stripTrailing();
        if (!trimmed.isEmpty()) {
            char last = trimmed.charAt(trimmed.length() - 1);
            return "+-*/=.".indexOf(last) == -1;
        }

        return true;
    }

    private static void insertSemicolon(List<Token> tokens) {
        if (tokens.size() < 2) return;

        Token secondLast = tokens.get(tokens.size() - 2);
        Token semicolon = new Token(TokenType.SEMICOLON, ";", null, secondLast.line);
        tokens.add(tokens.size() - 1, semicolon);
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }
}
