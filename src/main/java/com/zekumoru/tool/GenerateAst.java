package com.zekumoru.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }

        String outputDir = args[0];
        defineAst(outputDir, "Expr", Arrays.asList(
                "Assign       : Token name, Expr value",
                "Binary       : Expr left, Token operator, Expr right",
                "Call         : Expr callee, Token paren, List<Expr> arguments",
                "Grouping     : Expr expression",
                "Literal      : Object value",
                "Logical      : Expr left, Token operator, Expr right",
                "Unary        : Token operator, Expr right",
                "Conditional  : Expr condition, Expr thenBranch, Expr elseBranch",
                "Variable     : Token name"
        ));

        defineAst(outputDir, "Stmt", Arrays.asList(
                "Block      : List<Stmt> statements",
                "Expression : Expr expression",
                "Function   : Token name, List<Token> params, List<Stmt> body",
                "If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
                "Print      : Expr expression",
                "Var        : Token name, Expr initializer",
                "Loop       : Expr condition, Expr increment, Stmt body",
                "Break      : Token keyword",
                "Continue   : Token keyword"
        ));
    }

    private static void defineAst(
            String outputDir, String baseName, List<String> types)
            throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter printWriter = new PrintWriter(path, StandardCharsets.UTF_8);
        PrintIndentWriter writer = new PrintIndentWriter(printWriter, 4);

        writer.println("package com.zekumoru.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");

        defineVisitor(writer, baseName, types);
        writer.println();

        // The AST classes.
        for (int i = 0; i < types.size(); i++) {
            String[] parts = types.get(i).split(":");
            String className = parts[0].trim();
            String fields = parts[1].trim();

            defineType(writer, baseName, className, fields);

            if (i < types.size() - 1) {
                writer.println();
            }
        }

        // The base accept() method.
        writer.println();
        writer.println("abstract <R> R accept(Visitor<R> visitor);");

        writer.println("}");
        printWriter.close();
    }

    private static void defineVisitor(
            PrintIndentWriter writer, String baseName, List<String> types) {
        writer.println("interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("R visit" + typeName + baseName + "(" +
                    typeName + " " + baseName.toLowerCase() + ");");
        }

        writer.println("}");
    }

    private static void defineType(
            PrintIndentWriter writer, String baseName,
            String className, String fieldList) {
        writer.println("public static class " + className + " extends " + baseName + " {");

        // Fields.
        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            writer.println("final " + field + ";");
        }
        writer.println();

        // Constructor.
        writer.println(className + "(" + fieldList + ") {");
        // Store parameters in fields.
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("this." + name + " = " + name + ";");
        }
        writer.println("}");

        // Visitor pattern.
        writer.println();
        writer.println("@Override");
        writer.println("<R> R accept(Visitor<R> visitor) {");
        writer.println("return visitor.visit" + className + baseName + "(this);");
        writer.println("}");

        writer.println("}");
    }

    // Automatic indentation printer.
    private static class PrintIndentWriter {
        final PrintWriter writer;
        final String spaces;
        int depth;

        PrintIndentWriter(PrintWriter writer, int spaces) {
            this.writer = writer;
            this.spaces = " ".repeat(spaces);
            this.depth = 0;
        }

        public void print(String s) {
            writer.println(indentation() + s);
        }

        public void println() {
            writer.println(indentation());
        }

        public void println(String s) {
            if (!s.isEmpty() && s.charAt(s.length() - 1) == '}') outdent();
            writer.println(indentation() + s);
            if (!s.isEmpty() && s.charAt(s.length() - 1) == '{') indent();
        }

        public void indent() {
            depth++;
        }

        public void outdent() {
            if (depth <= 0) return;
            depth--;
        }

        private String indentation() {
            return spaces.repeat(depth);
        }
    }
}
