package com.codecad.core.ast.controlflow.exception

class ContinueException(val label: String?) : RuntimeException(null, null, false, false)