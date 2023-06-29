package com.codecad.core.parser

import com.codecad.core.exception.ParseException
import com.codecad.core.lexer.Lexer
import com.codecad.core.lexer.Token
import com.codecad.core.parser.ast.*
import com.codecad.core.scope.*

class Parser private constructor(private val lexer: Lexer) {
    private var lastOp: Op? = null
    private val lookahead = arrayOfNulls<Token>(LOOKAHEAD_SIZE)
    private var currIdx = 0
    private var label: String? = null

    init {
        for (i in 0 until LOOKAHEAD_SIZE) {
            lookahead[i] = lexer.next()
        }
    }

    private fun peek(): Token {
        return lookahead[currIdx]!!
    }

    private fun ahead(idx: Int): Token {
        if (idx > LOOKAHEAD_SIZE) {
            throw ParseException("Out of Lookahead buffer")
        }
        return lookahead[(currIdx + idx) % LOOKAHEAD_SIZE]!!
    }

    private fun shift() {
        lookahead[currIdx] = lexer.next()
        currIdx = (currIdx + 1) % LOOKAHEAD_SIZE
    }

    private operator fun next(): Token {
        val prev = lookahead[currIdx]
        shift()
        return prev!!
    }

    private fun isa(kind: Token.Kind): Boolean {
        return peek().kind == kind
    }

    private fun accept(kind: Token.Kind): Boolean {
        if (isa(kind)) {
            shift()
            return true
        }
        return false
    }

    private fun expect(kind: Token.Kind): Token? {
        val curr = peek()
        if (!accept(kind)) {
            throw ParseException("Expected $kind")
        }
        return curr
    }

    private fun enter(enter: Token.Enter): Boolean {
        return peek().enter == enter
    }

    private fun expectEnter(enter: Token.Enter) {
        if (!enter(enter)) {
            throw ParseException("Expected enter $enter")
        }
    }

    private fun follow(kind: Token.Kind): Boolean {
        return if (enter(Token.Enter.Token)) {
            accept(kind)
        } else false
    }

    private val isEOL: Boolean
        get() = peek().kind == Token.Kind.EOL

    fun parse(): Expr {
        val result = parseItem()
        expect(Token.Kind.EOL)
        return result
    }

    private fun parseIdent(expect: Boolean): String? {
        if (isa(Token.Kind.Ident)) {
            return next().symbol
        } else if (expect) {
            throw ParseException("Expected identifier")
        }
        return null
    }

    private fun parseIdentExpr(): IdentExpr {
        return IdentExpr(parseIdent(true))
    }

    private fun parsePtrn(): Expr {
        return if (accept(Token.Kind.LeftParen)) {
            parseTuple()
        } else {
            parseIdentExpr()
        }
    }

    private fun parseLetExpr(): Expr {
        expect(Token.Kind.Let)
        val ptrn = parsePtrn()
        var init: Expr? = null
        if (accept(Token.Kind.Assign)) {
            init = parseExpr()
        }
        return LetExpr(ptrn, init)
    }

    private fun parseItem(): Expr {
        val functions = ArrayList<FunctionExpr>()
        val exprs = ArrayList<Expr?>()
        while (!isa(Token.Kind.EOL)) {
            when (peek().kind) {
                Token.Kind.Semi -> {
                    next()
                    continue
                }
                Token.Kind.Fn -> {
                    functions.add(parseFunction())
                }
                Token.Kind.Let -> {
                    exprs.add(parseLetExpr())
                }
                else -> exprs.add(parseExpr())
            }
        }
        val expr : Expr = if (exprs.size == 1) exprs[0] ?: TupleExpr(arrayOf()) else BlockExpr(exprs.toTypedArray())
        return if (functions.isEmpty()) {
            expr
        } else {
            val scopeBuilder: StaticScope.Builder = StaticScope.Companion.builder()
            for (function in functions) {
                scopeBuilder.add(function.name, function)
            }
            ScopedExpr(scopeBuilder.build(), expr)
        }
    }

    private fun parseStmt(): Expr {
        while (!isa(Token.Kind.EOL)) {
            return when (peek().kind) {
                Token.Kind.Let -> parseLetExpr()
                else -> parseExpr()
            }
        }
        throw ParseException("Expected let or expr")
    }

    private fun parseFunction(): FunctionExpr {
        expect(Token.Kind.Fn)
        val name = parseIdent(true)
        expect(Token.Kind.LeftParen)
        val params = ArrayList<Expr?>()
        while (!accept(Token.Kind.RightParen)) {
            if (!params.isEmpty()) {
                expect(Token.Kind.Comma)
            }
            params.add(parseIdentExpr())
        }
        val body = parseBlock()
        val paramArr = params.toTypedArray()
        return FunctionExpr(name, TupleExpr(paramArr), body)
    }

    private fun parsePrefixExpr(op: Op): Expr {
        return if (op == Op.LeftParen) {
            val tuple = parseTuple()
            if (!accept(Token.Kind.Arrow)) {
                return tuple
            }
            val body = parseExpr()
            LambdaExpr(TupleExpr.Companion.asTuple(tuple), body)
        } else {
            val expr = parseExpr(op.prec().next())
            PrefixExpr(expr, op)
        }
    }

    private fun parseInfixExpr(lhs: Expr, op: Op): Expr {
        if (op == Op.Chain) {
            return if (accept(Token.Kind.LeftParen)) {
                val arg: TupleExpr = TupleExpr.Companion.asTuple(parseTuple())
                CallExpr(lhs, arg, true)
            } else {
                FieldExpr(lhs, parseIdent(true), true)
            }
        } else if (op == Op.Dot) {
            return FieldExpr(lhs, parseIdent(true))
        }
        val rhs = parseExpr(op.prec().next())
        return InfixExpr(lhs, rhs, op)
    }

    private fun parseTuple(): Expr {
        if (!accept(Token.Kind.RightParen)) {
            val expr = parseExpr()
            if (accept(Token.Kind.Comma)) {
                return if (accept(Token.Kind.RightParen)) {
                    TupleExpr(arrayOf<Expr?>(expr))
                } else {
                    val exprs = ArrayList<Expr?>()
                    exprs.add(expr)
                    do {
                        exprs.add(parseExpr())
                    } while (accept(Token.Kind.Comma))
                    expect(Token.Kind.RightParen)
                    TupleExpr(exprs.toTypedArray())
                }
            }
            expect(Token.Kind.RightParen)
            return expr
        }
        return TupleExpr(arrayOfNulls(0))
    }

    private fun parseIf(): Expr {
        expect(Token.Kind.If)
        val condition = parseExpr()
        val trueBranch = parseBlock()
        var falseBranch = null as Expr?
        if (accept(Token.Kind.Else)) {
            falseBranch = parseBlock()
        }
        return IfExpr(condition, trueBranch, falseBranch)
    }

    private fun parseWhile(label: String?): Expr {
        expect(Token.Kind.While)
        val condition = parseExpr()
        val body = parseBlock()
        return WhileExpr(condition, body, label)
    }

    private fun parseFor(label: String?): Expr {
        expect(Token.Kind.For)
        val variable = parseExpr()
        expect(Token.Kind.In)
        val range = parseExpr()
        val body = parseBlock()
        return ForExpr(variable, range, body, label)
    }

    private fun parseBlock(): Expr {
        expect(Token.Kind.LeftBrace)
        val exprs = ArrayList<Expr?>()
        var valid = true
        while (true) {
            while (accept(Token.Kind.Semi)) {
                valid = true
            }
            if (accept(Token.Kind.RightBrace)) break
            if (!enter(Token.Enter.NL) && !valid) {
                throw ParseException("Expected newline or semi to separate statements")
            }
            exprs.add(parseStmt())
            valid = false
        }
        return BlockExpr(exprs.toTypedArray())
    }

    private fun parsePostfixExpr(lhs: Expr, op: Op): Expr {
        when (op) {
            Op.LeftParen -> {
                val arg = parseTuple()
                return CallExpr(lhs, TupleExpr.Companion.asTuple(arg))
            }
            Op.Inc, Op.Dec -> return PostfixExpr(lhs, op)
        }
        throw ParseException("Postfix Expr not yet implemented")
    }

    private fun parsePrimaryExpr(): Expr {
        val label = label
        this.label = null
        when (peek().kind) {
            Token.Kind.Ident -> {
                val sym = next().symbol
                if (accept(Token.Kind.Colon)) {
                    this.label = sym
                    return parseExpr()
                }
                return IdentExpr(sym)
            }
            Token.Kind.String -> return LiteralExpr(next().symbol)
            Token.Kind.Boolean -> return LiteralExpr(java.lang.Boolean.parseBoolean(next().symbol))
            Token.Kind.Number -> return LiteralExpr(next().symbol?.toInt())
            Token.Kind.Null -> {
                next()
                return LiteralExpr(null)
            }
            Token.Kind.Return -> {
                next()
                var expr: Expr? = null
                if (!isa(Token.Kind.Semi) && !isa(Token.Kind.RightBrace)) {
                    expr = parseExpr()
                }
                return ReturnExpr(expr)
            }
            Token.Kind.Break -> {
                next()
                return BreakExpr(parseIdent(false))
            }
            Token.Kind.Continue -> {
                next()
                return ContinueExpr(parseIdent(false))
            }
            Token.Kind.If -> return parseIf()
            Token.Kind.While -> return parseWhile(label)
            Token.Kind.For -> return parseFor(label)
            Token.Kind.LeftBrace -> return parseBlock()
        }
        throw ParseException("Expected Identifier, String, Boolean or Number")
    }

    private fun parseExpr(prec: Prec? = Prec.Bottom): Expr {
        val prefixOp = parseOp()
        var lhs = prefixOp?.let { parsePrefixExpr(it) } ?: parsePrimaryExpr()
        while (!isEOL) {
            if (enter(Token.Enter.NL)) {
                break
            }
            val op = parseOp()
            if (op == null) {
                break
            } else if (op.isInfix) {
                if (prec!!.largerThan(op.prec())) {
                    lastOp = op
                    break
                }
                lhs = parseInfixExpr(lhs, op)
            } else if (op.isPostfix) {
                if (prec == Prec.Top) {
                    lastOp = op
                    break
                }
                lhs = parsePostfixExpr(lhs, op)
            } else {
                throw ParseException("Expr exception")
            }
        }
        return lhs
    }

    private fun parseOp(): Op? {
        val tmpLastOp = lastOp
        if (tmpLastOp != null) {
            lastOp = null
            return tmpLastOp
        }
        val op = parseOpImpl()
        if (op != null) {
            next()
        }
        return op
    }

    private fun parseOpImpl(): Op? {
        return when (peek().kind) {
            Token.Kind.Eq -> Op.Eq
            Token.Kind.Ne -> Op.Ne
            Token.Kind.Le -> Op.Le
            Token.Kind.Lt -> Op.Lt
            Token.Kind.Ge -> Op.Ge
            Token.Kind.Gt -> Op.Gt
            Token.Kind.Plus -> Op.Add
            Token.Kind.Minus -> Op.Sub
            Token.Kind.Star -> Op.Mul
            Token.Kind.Slash -> Op.Div
            Token.Kind.Dot -> Op.Dot
            Token.Kind.And -> Op.And
            Token.Kind.Or -> Op.Or
            Token.Kind.LeftParen -> Op.LeftParen
            Token.Kind.Assign -> Op.Assign
            Token.Kind.AssignMinus -> Op.AssignSub
            Token.Kind.AssignPlus -> Op.AssignAdd
            Token.Kind.AssignStar -> Op.AssignMul
            Token.Kind.AssignSlash -> Op.AssignDiv
            Token.Kind.Nullish -> Op.Nullish
            Token.Kind.Chain -> Op.Chain
            Token.Kind.Pow -> Op.Pow
            Token.Kind.Not -> Op.Not
            Token.Kind.BitAnd -> Op.BitAnd
            Token.Kind.BitOr -> Op.BitOr
            Token.Kind.BitXor -> Op.BitXor
            Token.Kind.Dec -> Op.Dec
            Token.Kind.Inc -> Op.Inc
            Token.Kind.Range -> Op.Range
            Token.Kind.Ellipsis -> Op.Spread
            else -> null
        }
    }

    companion object {
        private const val LOOKAHEAD_SIZE = 2
        fun parse(expr: String): Expr {
            val lex = Lexer(expr)
            val parser = Parser(lex)
            var ast = parser.parse()
            val bindScope = MutualScope()
            ast = ast.bind(bindScope, false)
            return ast
        }
    }
}