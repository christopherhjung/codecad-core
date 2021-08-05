package com.codecad.core

import java.lang.RuntimeException

class LineException(val locations : List<Location>, message: String = "") : RuntimeException("$message $locations") {
}
