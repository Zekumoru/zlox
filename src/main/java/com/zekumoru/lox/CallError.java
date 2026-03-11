package com.zekumoru.lox;

class CallError extends RuntimeException {
    CallError(String message) {
        super(message);
    }
}
