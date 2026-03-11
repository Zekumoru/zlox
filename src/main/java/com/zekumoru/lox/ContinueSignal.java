package com.zekumoru.lox;

public class ContinueSignal extends RuntimeException {
    public ContinueSignal() {
        super(null, null, false, false);
    }
}
