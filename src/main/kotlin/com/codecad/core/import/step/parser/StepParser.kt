package com.codecad.core.import.step.parser

import com.codecad.core.exception.ParseException
import com.codecad.core.import.step.ast.*
import com.codecad.core.import.step.lexer.StepLexer
import com.codecad.core.import.step.lexer.StepToken
import com.codecad.core.part.Context

class StepParser private constructor(private val lexer: StepLexer) {
    private var lookahead : StepToken? = null
    private val context = Context()

    init {
        shift()
    }

    private fun peek(): StepToken {
        return lookahead!!
    }

    private fun shift() {
        lookahead = lexer.next()
    }

    private operator fun next(): StepToken {
        val prev = lookahead
        shift()
        return prev!!
    }

    private fun isa(kind: StepToken.Kind): Boolean {
        return peek().kind == kind
    }

    private fun accept(kind: StepToken.Kind): Boolean {
        if (isa(kind)) {
            shift()
            return true
        }
        return false
    }

    private fun expect(kind: StepToken.Kind): StepToken {
        val curr = peek()
        if (!accept(kind)) {
            throw ParseException("Expected $kind")
        }
        return curr
    }

    private val isEOL: Boolean
        get() = peek().kind == StepToken.Kind.EOL

    fun parse() : StepFile {
        val prog = parseFile()
        expect(StepToken.Kind.EOL)
        return prog
    }

    fun parseFile() : StepFile{
        expect(StepToken.Kind.BeginIso)
        val map = hashMapOf<Int, StepDef>()
        while(!isa(StepToken.Kind.Error) && !isEOL){
            if(isa(StepToken.Kind.Instance)){
                val instance = next()
                val idx = instance.symbol?.toIntOrNull() ?: throw RuntimeException("")
                expect(StepToken.Kind.Assign)
                map[idx] = parseValue()
            }

            next()
        }

        for( def in map.values ){
            def.bind(map)
        }

        return StepFile(map.values.toTypedArray())
    }

    fun parseValue() : StepDef{
        return when(lookahead?.kind){
            StepToken.Kind.LeftParen -> parseMultiObject()
            StepToken.Kind.Ident -> parseObject()
            StepToken.Kind.Real -> StepDouble(next().symbol!!.toDouble())
            StepToken.Kind.Number -> StepInt(next().symbol!!.toInt())
            StepToken.Kind.String,
                StepToken.Kind.Enum -> StepString(next().symbol!!)
            StepToken.Kind.Instance -> StepInstance(next().symbol!!.toInt())
            StepToken.Kind.Boolean -> StepBoolean(next().symbol == "T")
            StepToken.Kind.Star,
                StepToken.Kind.Dollar -> {
                    shift()
                    StepNull
                }
            else -> throw RuntimeException(lookahead!!.toString())
        }
    }

    fun parseObject() : StepObject{
        val ident = expect(StepToken.Kind.Ident)
        expect(StepToken.Kind.LeftParen)
        val args = parseArgs()
        expect(StepToken.Kind.RightParen)
        return StepObject(ident.symbol!!, args)
    }

    fun parseMultiObject() : StepDef{
        expect(StepToken.Kind.LeftParen)
        if(!isa(StepToken.Kind.Ident)){
            return parseTuple(false)
        }

        val objs = arrayListOf<StepObject>()
        if(!isa(StepToken.Kind.RightParen)){
            do{
                objs.add(parseObject())
            }while (!isa(StepToken.Kind.RightParen))
        }
        return StepMultiObject(objs.toTypedArray())
    }

    fun parseTuple(init : Boolean = true) : StepDef{
        if(init) expect(StepToken.Kind.LeftParen)
        val args = parseArgs()
        expect(StepToken.Kind.RightParen)
        return StepTuple(args)
    }

    fun parseArgs() : Array<StepDef>{
        val list = arrayListOf<StepDef>()
        if(!isa(StepToken.Kind.RightParen)){
            do{
                list.add(parseValue())
            }while (accept(StepToken.Kind.Comma))
        }
        return list.toTypedArray()
    }

    companion object {
        fun parse(expr: String): StepFile {
            val lex = StepLexer(expr)
            val parser = StepParser(lex)
            return parser.parse()
        }
    }
}