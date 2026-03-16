package com.zekumoru.lox;

import java.util.List;

abstract class ClassMember {
    interface Visitor<R> {
        R visitFieldClassMember(Field classmember);
        R visitMethodClassMember(Method classmember);
        R visitGetterClassMember(Getter classmember);
    }
    
    public static class Field extends ClassMember {
        final Token name;
        final Expr initializer;
        final boolean isStatic;
        
        Field(Token name, Expr initializer, boolean isStatic) {
            this.name = name;
            this.initializer = initializer;
            this.isStatic = isStatic;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitFieldClassMember(this);
        }
    }
    
    public static class Method extends ClassMember {
        final Stmt.Function method;
        final boolean isStatic;
        
        Method(Stmt.Function method, boolean isStatic) {
            this.method = method;
            this.isStatic = isStatic;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitMethodClassMember(this);
        }
    }
    
    public static class Getter extends ClassMember {
        final Stmt.Function method;
        final boolean isStatic;
        
        Getter(Stmt.Function method, boolean isStatic) {
            this.method = method;
            this.isStatic = isStatic;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitGetterClassMember(this);
        }
    }
    
    abstract <R> R accept(Visitor<R> visitor);
}
