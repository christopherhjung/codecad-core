package com.codecad.core

import java.lang.RuntimeException

class Location(val line: Int, val column: Int)

class LineException(val locations : List<Location>, message: String = "") : RuntimeException(message) {
}
