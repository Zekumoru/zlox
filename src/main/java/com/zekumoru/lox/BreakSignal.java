package com.zekumoru.lox;

class BreakSignal extends RuntimeException {
    public BreakSignal() {
        super(null, null, false, false);
    }
}
