package com.codecad.core

import java.lang.RuntimeException

class Location(val line: Int, val column: Int){
    override fun toString(): String {
        return "Location(line=$line, column=$column)"
    }
}

class LineException(val locations : List<Location>, message: String = "") : RuntimeException(message + " " + locations) {
}
