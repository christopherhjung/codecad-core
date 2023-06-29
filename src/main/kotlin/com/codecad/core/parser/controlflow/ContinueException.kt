package com.codecad.core.parser.controlflow

class ContinueException(val label: String?) : RuntimeException(null, null, false, false)