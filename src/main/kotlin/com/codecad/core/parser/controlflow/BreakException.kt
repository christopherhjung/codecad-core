package com.codecad.core.parser.controlflow

class BreakException(val label: String?) : RuntimeException(null, null, false, false)