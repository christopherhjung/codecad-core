package com.codecad.core.lexer

import org.apache.commons.lang3.CharUtils

class Lexer(code: String) {
    private val chars: CharArray
    private var idx = 0
    private var mark = 0
    private var enter: Token.Enter? = null

    init {
        chars = code.toCharArray()
    }

    private fun curr(): Int {
        return if (isEOL) -1 else chars[idx].code
    }

    private fun currChar(): Char {
        assert(!isEOL)
        return chars[idx]
    }

    private fun eat(): Char {
        assert(!isEOL)
        val result = chars[idx]
        shift()
        return result
    }

    private fun shift() {
        idx++
    }

    private val isEOL: Boolean
        private get() = idx >= chars.size

    fun ahead(offset: Int): Int {
        return if (idx + offset + 1 >= chars.size) {
            -1
        } else chars[idx].code
    }

    fun `is`(cha: Int, offset: Int): Boolean {
        return ahead(offset) == cha
    }

    fun accept(cha: Int): Boolean {
        if (curr() == cha) {
            idx++
            return true
        }
        return false
    }

    fun accept(cha: Char): Boolean {
        return accept(cha.code)
    }

    private val isNumeric: Boolean
        private get() {
            val curr = curr()
            return if (curr == -1) {
                false
            } else {
                CharUtils.isAsciiNumeric(curr.toChar())
            }
        }
    private val isAlpha: Boolean
        private get() {
            val curr = curr()
            return if (curr == -1) {
                false
            } else {
                CharUtils.isAsciiAlpha(curr.toChar())
            }
        }
    private val isAlphaNumeric: Boolean
        private get() {
            val curr = curr()
            return if (curr == -1) {
                false
            } else {
                CharUtils.isAsciiAlphanumeric(curr.toChar())
            }
        }

    private fun mark() {
        mark = idx
    }

    private fun getString(length: Int = 0): String {
        val correctedLength = if(length <= 0){
            idx - mark + length
        }else{
            length
        }
        return String(chars, mark, correctedLength)
    }

    private fun token(kind: Token.Kind, symbol: String? = null): Token {
        return Token(kind, enter, symbol)
    }

    operator fun next(): Token {
        enter = Token.Enter.Token
        while (!isEOL) {
            while (true) {
                if (accept(' ') || accept('\t')) {
                    if (enter == Token.Enter.Token) {
                        enter = Token.Enter.Space
                    }
                } else if (accept('\n') || accept('\r')) {
                    enter = Token.Enter.NL
                } else {
                    break
                }
            }
            if (accept('=')) {
                return if (accept('=')) {
                    token(Token.Kind.Eq)
                } else {
                    token(Token.Kind.Assign)
                }
            }
            if (accept('!')) {
                return if (accept('=')) {
                    token(Token.Kind.Ne)
                } else {
                    token(Token.Kind.Not)
                }
            }
            if (accept('<')) {
                return if (accept('=')) {
                    token(Token.Kind.Le)
                } else {
                    token(Token.Kind.Lt)
                }
            }
            if (accept('>')) {
                return if (accept('=')) {
                    token(Token.Kind.Ge)
                } else {
                    token(Token.Kind.Gt)
                }
            }
            if (accept('?')) {
                return if (accept('?')) {
                    token(Token.Kind.Nullish)
                } else if (accept('.')) {
                    token(Token.Kind.Chain)
                } else {
                    token(Token.Kind.Quest)
                }
            }
            if (accept('&')) {
                return token(Token.Kind.BitAnd)
            }
            if (accept('|')) {
                return token(Token.Kind.BitOr)
            }
            if (accept('^')) {
                return token(Token.Kind.BitXor)
            }
            if (accept('(')) {
                return token(Token.Kind.LeftParen)
            }
            if (accept(')')) {
                return token(Token.Kind.RightParen)
            }
            if (accept('{')) {
                return token(Token.Kind.LeftBrace)
            }
            if (accept('}')) {
                return token(Token.Kind.RightBrace)
            }
            if (accept('+')) {
                if (accept('+')) {
                    return token(Token.Kind.Inc)
                } else if (accept('=')) {
                    return token(Token.Kind.AssignPlus)
                }
                return token(Token.Kind.Plus)
            }
            if (accept('-')) {
                if (accept('-')) {
                    return token(Token.Kind.Dec)
                } else if (accept('>')) {
                    return token(Token.Kind.Arrow)
                } else if (accept('=')) {
                    return token(Token.Kind.AssignMinus)
                }
                return token(Token.Kind.Minus)
            }
            if (accept('*')) {
                if (accept('*')) {
                    return token(Token.Kind.Pow)
                } else if (accept('=')) {
                    return token(Token.Kind.AssignStar)
                }
                return token(Token.Kind.Star)
            }
            if (accept('/')) {
                if (accept('=')) {
                    return token(Token.Kind.AssignSlash)
                }
                if (accept('*')) { // arbitrary comment
                    var depth = 1
                    while (true) {
                        if (isEOL) {
                            return token(Token.Kind.Error)
                        }
                        if (accept('/')) {
                            if (accept('*')) {
                                depth += 1
                            }
                        } else if (accept('*')) {
                            if (accept('/')) {
                                depth -= 1
                                if (depth == 0) {
                                    break
                                }
                            }
                            continue
                        }
                        next()
                    }
                    enter = Token.Enter.NL
                    continue
                }
                if (accept('/')) {
                    while (true) {
                        if (isEOL) {
                            return token(Token.Kind.EOL)
                        }
                        if (accept('\n')) {
                            break
                        } else {
                            next()
                        }
                    }
                    enter = Token.Enter.NL
                    continue
                }
                return token(Token.Kind.Slash)
            }
            if (accept('.')) {
                if (!accept('.')) {
                    return token(Token.Kind.Dot)
                }
                return if (!accept('.')) {
                    token(Token.Kind.Range)
                } else token(Token.Kind.Ellipsis)
            }
            if (accept(',')) {
                return token(Token.Kind.Comma)
            }
            if (accept(';')) {
                return token(Token.Kind.Semi)
            }
            if (isNumeric) {
                mark()
                shift()
                while (isNumeric) {
                    shift()
                }
                if(accept('.')){
                    if(accept('E') || accept('e')){
                        accept('-')
                        accept('+')
                        while (isNumeric) {
                            shift()
                        }

                        return token(Token.Kind.Real, getString())
                    }

                    while (isNumeric) {
                        shift()
                    }

                    return token(Token.Kind.Real, getString())
                }

                return token(Token.Kind.Number, getString())
            }
            if (isAlpha) {
                mark()
                shift()
                while (isAlphaNumeric) {
                    shift()
                }
                val value = getString()
                when (value) {
                    "true", "false" -> return token(Token.Kind.Boolean, value)
                    "fn" -> return token(Token.Kind.Fn)
                    "if" -> return token(Token.Kind.If)
                    "else" -> return token(Token.Kind.Else)
                    "break" -> return token(Token.Kind.Break)
                    "continue" -> return token(Token.Kind.Continue)
                    "while" -> return token(Token.Kind.While)
                    "return" -> return token(Token.Kind.Return)
                    "null" -> return token(Token.Kind.Null)
                    "and" -> return token(Token.Kind.And)
                    "or" -> return token(Token.Kind.Or)
                    "let" -> return token(Token.Kind.Let)
                    "for" -> return token(Token.Kind.For)
                    "in" -> return token(Token.Kind.In)
                    "sketch" -> return token(Token.Kind.Sketch)
                }
                return token(Token.Kind.Ident, value)
            }
            if (accept('\"')) {
                mark()
                while (!accept('\"')) {
                    if (isEOL) {
                        return token(Token.Kind.Error)
                    }
                    shift()
                }
                return token(Token.Kind.String, getString(-1 ))
            }
            return if (isEOL) {
                token(Token.Kind.EOL)
            } else token(Token.Kind.Error)
        }
        return token(Token.Kind.EOL)
    }
}