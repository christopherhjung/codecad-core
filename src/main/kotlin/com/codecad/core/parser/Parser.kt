package com.codecad.core.parser

import com.codecad.core.ast.primitive.*
import com.codecad.core.exception.ParseException
import com.codecad.core.lexer.Lexer
import com.codecad.core.lexer.Token
import com.codecad.core.scope.MutualScope
import com.codecad.core.scope.StaticScope
import com.codecad.core.World
import com.codecad.core.ast.controlflow.*

class Parser private constructor(private val lexer: Lexer, private val world: World) {
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

    private fun parseIdentOpt(): String? {
        if (isa(Token.Kind.Ident)) {
            return next().symbol
        }
        return null
    }

    private fun parseIdent(): String {
        return parseIdentOpt() ?: throw ParseException("Expected identifier")
    }

    private fun parseIdentExpr(): IdentExpr {
        return IdentExpr(world, parseIdent())
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
        return LetExpr(world, ptrn, init)
    }

    private fun parseSketchExpr(): Expr {
        expect(Token.Kind.Sketch)
        val name = parseIdent()
        val body = parseBlock()
        return com.codecad.core.ast.complex.SketchExpr(world, name, body)
    }

    private fun parseItem(): Expr {
        val functions = ArrayList<FunctionExpr>()
        val exprs = ArrayList<Expr>()
        while (!isa(Token.Kind.EOL)) {
            when (peek().kind) {
                Token.Kind.Semi -> {
                    next()
                    continue
                }
                Token.Kind.Fn -> functions.add(parseFunction())
                Token.Kind.Let -> exprs.add(parseLetExpr())
                Token.Kind.Sketch -> exprs.add(parseSketchExpr())
                else -> exprs.add(parseExpr())
            }
        }
        val expr : Expr = if (exprs.size == 1) exprs[0] else BlockExpr(
            world,
            exprs.toTypedArray()
        )
        return if (functions.isEmpty()) {
            expr
        } else {
            val scopeBuilder: StaticScope.Builder = StaticScope.builder()
            for (function in functions) {
                scopeBuilder.add(function.name, function)
            }
            ScopedExpr(world, scopeBuilder.build(), expr)
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
        val name = parseIdent()
        expect(Token.Kind.LeftParen)
        val params = ArrayList<Expr>()
        while (!accept(Token.Kind.RightParen)) {
            if (params.isNotEmpty()) {
                expect(Token.Kind.Comma)
            }
            params.add(parseIdentExpr())
        }
        val body = parseBlock()
        val paramArr = params.toTypedArray()
        return FunctionExpr(world, name, TupleExpr(world, paramArr), body)
    }

    private fun parsePrefixExpr(op: Op): Expr {
        return when(op){
            Op.LeftParen -> {
                val tuple = parseTuple()
                if (!accept(Token.Kind.Arrow)) {
                    return tuple
                }
                val body = parseExpr()
                LambdaExpr(world, TupleExpr.asTuple(tuple), body)
            }
            else -> {
                val expr = parseExpr(op.prec().next())
                world.prefix(expr, op)
            }
        }
    }

    private fun parseInfixExpr(lhs: Expr, op: Op): Expr {
        if (op == Op.Chain) {
            return if (accept(Token.Kind.LeftParen)) {
                val arg: TupleExpr = TupleExpr.asTuple(parseTuple())
                CallExpr(world, lhs, arg, true)
            } else {
                FieldExpr(world, lhs, parseIdent(), true)
            }
        } else if (op == Op.Dot) {
            return FieldExpr(world, lhs, parseIdent())
        }
        val rhs = parseExpr(op.prec().next())
        return world.infix(lhs, rhs, op)
    }

    private fun parseTuple(): Expr {
        if (!accept(Token.Kind.RightParen)) {
            val expr = parseExpr()
            if (accept(Token.Kind.Comma)) {
                return if (accept(Token.Kind.RightParen)) {
                    TupleExpr(world, arrayOf(expr))
                } else {
                    val exprs = ArrayList<Expr>()
                    exprs.add(expr)
                    do {
                        exprs.add(parseExpr())
                    } while (accept(Token.Kind.Comma))
                    expect(Token.Kind.RightParen)
                    TupleExpr(world, exprs.toTypedArray())
                }
            }
            expect(Token.Kind.RightParen)
            return expr
        }
        return TupleExpr(world, emptyArray())
    }

    private fun parseIf(): Expr {
        expect(Token.Kind.If)
        val condition = parseExpr()
        val trueBranch = parseBlock()
        var falseBranch = null as Expr?
        if (accept(Token.Kind.Else)) {
            falseBranch = parseBlock()
        }
        return IfExpr(world, condition, trueBranch, falseBranch)
    }

    private fun parseWhile(label: String?): Expr {
        expect(Token.Kind.While)
        val condition = parseExpr()
        val body = parseBlock()
        return WhileExpr(world, condition, body, label)
    }

    private fun parseFor(label: String?): Expr {
        expect(Token.Kind.For)
        val variable = parseExpr()
        expect(Token.Kind.In)
        val range = parseExpr()
        val body = parseBlock()
        return ForExpr(world, variable, range, body, label)
    }

    private fun parseBlock(): Expr {
        expect(Token.Kind.LeftBrace)
        val exprs = ArrayList<Expr>()
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
        return BlockExpr(world, exprs.toTypedArray())
    }

    private fun parsePostfixExpr(expr: Expr, op: Op): Expr {
        return when (op) {
            Op.LeftParen -> {
                val arg = parseTuple()
                CallExpr(world, expr, TupleExpr.asTuple(arg))
            }
            Op.Inc, Op.Dec -> world.postfix(expr, op)
            else -> throw ParseException("Postfix Expr not yet implemented")
        }
    }

    private fun parsePrimaryExpr(): Expr {
        val label = label
        this.label = null
        return when (peek().kind) {
            Token.Kind.Ident -> {
                val sym = next().symbol!!
                if (accept(Token.Kind.Colon)) {
                    this.label = sym
                    parseExpr()
                }else{
                    IdentExpr(world, sym)
                }
            }
            Token.Kind.String -> LiteralExpr(world, next().symbol)
            Token.Kind.Boolean -> LiteralExpr(world, java.lang.Boolean.parseBoolean(next().symbol))
            Token.Kind.Number -> LiteralExpr(world, next().symbol!!.toInt())
            Token.Kind.Real -> LiteralExpr(world, next().symbol!!.toDouble())
            Token.Kind.Null -> {
                next()
                LiteralExpr(world, null)
            }
            Token.Kind.Return -> {
                next()
                var expr: Expr? = null
                if (!isa(Token.Kind.Semi) && !isa(Token.Kind.RightBrace)) {
                    expr = parseExpr()
                }
                ReturnExpr(world, expr)
            }
            Token.Kind.Break -> {
                next()
                BreakExpr(world, parseIdentOpt())
            }
            Token.Kind.Continue -> {
                next()
                ContinueExpr(world, parseIdentOpt())
            }
            Token.Kind.If -> parseIf()
            Token.Kind.While -> parseWhile(label)
            Token.Kind.For -> parseFor(label)
            Token.Kind.LeftBrace -> parseBlock()
            else -> throw ParseException("Expected Identifier, String, Boolean or Number")
        }
    }

    private fun parseExpr(prec: Prec = Prec.Bottom): Expr {
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
                if (prec.largerThan(op.prec())) {
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
        fun parse(expr: String, world: World = World()): Expr {
            val lex = Lexer(expr)
            val parser = Parser(lex, world)
            var ast = parser.parse()
            val bindScope = MutualScope()
            ast = ast.bind(bindScope, false)
            return ast
        }
    }
}