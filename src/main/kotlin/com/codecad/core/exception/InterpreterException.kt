package com.codecad.core.exception

class InterpreterException : RuntimeException {
    constructor(msg: String?) : super(msg)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}