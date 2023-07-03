package com.codecad.core.ast.controlflow.exception

class ReturnException(val returnValue: Any?) : RuntimeException(null, null, false, false)