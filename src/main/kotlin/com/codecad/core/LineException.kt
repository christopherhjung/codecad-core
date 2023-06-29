package com.codecad.core

import com.codecad.common.LineError

class LineException(val locations : List<LineError>, message: String = "") : RuntimeException("$message $locations") {
}
