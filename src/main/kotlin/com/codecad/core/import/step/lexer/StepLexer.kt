package com.codecad.core.import.step.lexer

import com.codecad.core.lexer.Token
import org.apache.commons.lang3.CharUtils

class StepLexer(code: String) {
    private val chars: CharArray
    private var idx = 0
    private var mark = 0

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


    fun isChar(cha: Char): Boolean {
        return chars[idx] == cha
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

    private fun token(kind: StepToken.Kind, symbol: String? = null): StepToken {
        return StepToken(kind, symbol)
    }

    fun acceptWhitespace() : Boolean{
        return accept(' ') || accept('\t') || accept('\n') || accept('\r')
    }

    private fun lexChar() : StepToken.Kind?{
        return when(currChar()){
            '=' -> StepToken.Kind.Assign
            '*' -> StepToken.Kind.Star
            '$' -> StepToken.Kind.Dollar
            '(' -> StepToken.Kind.LeftParen
            ')' -> StepToken.Kind.RightParen
            ',' -> StepToken.Kind.Comma
            ';' -> StepToken.Kind.Semi
            else -> null
        }
    }

    operator fun next(): StepToken {
        while (!isEOL) {
            if(acceptWhitespace()){
                while (acceptWhitespace()) {}
                continue
            }

            lexChar()?.let {
                shift()
                return token(it)
            }

            if (accept('/')) {
                if (accept('*')) { // arbitrary comment
                    var depth = 1
                    while (true) {
                        if (isEOL) {
                            return token(StepToken.Kind.Error)
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
                        shift()
                    }
                    continue
                }
                if (accept('/')) {
                    while (true) {
                        if (isEOL) return token(StepToken.Kind.EOL)
                        if (accept('\n')) {
                            break
                        } else {
                            next()
                        }
                    }
                    continue
                }
                return token(StepToken.Kind.Error)
            }
            if (accept('.')) {
                mark()
                while(!accept('.')){
                    shift()
                }

                return when(val value = getString(-1)){
                    "T", "F" -> token(StepToken.Kind.Boolean, value)
                    else -> token(StepToken.Kind.Enum, value)
                }
            }
            if(accept('#')){
                mark()
                shift()
                while (isNumeric) {
                    shift()
                }

                return token(StepToken.Kind.Instance, getString())
            }

            if (isNumeric || isChar('-')) {
                mark()
                shift()
                while (isNumeric) {
                    shift()
                }
                if(accept('.')){
                    while(true){
                        if(accept('E') || accept('e')){
                            if(!accept('-')){
                                accept('+')
                            }

                            while (isNumeric) {
                                shift()
                            }
                        }else if(isNumeric){
                            shift()
                            continue
                        }

                        return token(StepToken.Kind.Real, getString())
                    }
                }

                return token(StepToken.Kind.Number, getString())
            }
            if (isAlpha) {
                mark()
                shift()
                while (isAlphaNumeric || isChar('-') || isChar('_')) {
                    shift()
                }
                val value = getString()
                return when{
                    value == "HEADER" -> token(StepToken.Kind.Header)
                    value == "DATA" -> token(StepToken.Kind.Data)
                    value == "ENDSEC" -> token(StepToken.Kind.EndSec)
                    value.startsWith("ISO") -> token(StepToken.Kind.BeginIso, value)
                    value.startsWith("END-ISO") -> token(StepToken.Kind.EndIso, value.substring(4))
                    else -> token(StepToken.Kind.Ident, value)
                }
            }
            if (accept('\'')) {
                mark()
                while (!accept('\'')) {
                    if (isEOL) {
                        return token(StepToken.Kind.Error)
                    }
                    shift()
                }
                return token(StepToken.Kind.String, getString(-1))
            }
            return if (isEOL) {
                token(StepToken.Kind.EOL)
            } else token(StepToken.Kind.Error)
        }
        return token(StepToken.Kind.EOL)
    }
}