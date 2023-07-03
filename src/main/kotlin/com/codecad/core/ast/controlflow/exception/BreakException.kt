package com.codecad.core.ast.controlflow.exception

class BreakException(val label: String?) : RuntimeException(null, null, false, false)