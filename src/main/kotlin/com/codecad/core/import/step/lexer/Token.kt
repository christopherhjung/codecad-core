package com.codecad.core.import.step.lexer

data class StepToken(val kind: Kind, val symbol: String?) {
    enum class Kind {
        Instance, Ident, Enum, Comma, Semi, Number, Boolean, Real, String, LeftParen, RightParen, Assign, Star, Dollar,
        BeginIso, EndIso,
        EndSec, Header, Data,
        EOL, Error,
    }
}