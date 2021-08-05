package com.codecad.core

import com.codecad.common.LineError
import java.lang.RuntimeException

class LineException(val locations : List<LineError>, message: String = "") : RuntimeException("$message $locations") {
}
