package com.codecad.core.exception

import com.codecad.common.LineError

class LineException(val locations : List<LineError>, message: String = "") : RuntimeException("$message $locations") {
}
