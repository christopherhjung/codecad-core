package com.codecad.core.lexer

class Token(val kind: Kind, val enter: Enter?, val symbol: String?) {

    enum class Kind {
        Ident, String, Boolean, Number, Real, Null, Assign, AssignPlus, AssignMinus,
        AssignStar, AssignSlash, Eq, Ne, Lt, Le, Gt, Ge, Not, And, Or, BitAnd,
        BitOr, BitXor, Plus, Minus, Star, Slash, Inc, Dec, LeftParen, RightParen,
        LeftBrace, RightBrace, Dot, Range, Ellipsis, Comma, Semi, Colon, Fn, If,
        While, Else, Break, Continue, Return, For, In, Quest, Nullish, Chain, Pow,
        Arrow, Let, EOL, Error,

        Sketch
    }

    enum class Enter {
        Token, Space, NL
    }
}