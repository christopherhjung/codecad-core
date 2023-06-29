package com.codecad.core.parser.controlflow

class ReturnException(val returnValue: Any?) : RuntimeException(null, null, false, false)