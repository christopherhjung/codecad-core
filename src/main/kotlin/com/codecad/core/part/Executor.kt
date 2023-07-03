package com.codecad.core.part

import com.codecad.common.LineError
import com.codecad.core.CircleLike
import com.codecad.core.Segment2
import com.codecad.core.Vec2
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.exception.LineException
import com.codecad.core.parser.ObjectFunction
import com.codecad.core.parser.Parser
import com.codecad.core.scope.MutualScope
import com.codecad.core.scope.project
import com.codecad.core.scope.sketch
import com.codecad.core.scope.world
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.PrintWriter
import javax.script.ScriptException
import javax.script.SimpleScriptContext

class ExecutionResult(val output: String, val project: Project)

class Executor{
    companion object{
        private val LOGGER = LoggerFactory.getLogger(Executor::class.java)
        fun execute(code: String) : ExecutionResult {
            return Executor().execute(code)
        }
    }

    fun run(expr: Expr) : Project {
        val scope = MutualScope()

        val project = Project()
        scope.project = project
        val world = project.world
        scope.world = world
        scope.setObject("println", ObjectFunction{ _, args ->
            LOGGER.info(args[0].toString())
        }, true)
        scope.setObject("vec2", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            val world = scope.world
            val lhs = Expr.orLiteral(world, args[0])
            val rhs = Expr.orLiteral(world, args[1])
            return@ObjectFunction sketch.point(lhs, rhs)
        }, true)
        scope.setObject("param", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            val init = (args[0] as Number).toDouble()
            return@ObjectFunction sketch.param(init)
        }, true)
        scope.setObject("param2", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            val lhs = sketch.param(args[0] as Double)
            val rhs = sketch.param(args[1] as Double)
            return@ObjectFunction sketch.point(lhs, rhs)
        }, true)
        scope.setObject("line", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            val lhs = args[0] as Vec2
            val rhs = args[1] as Vec2
            return@ObjectFunction sketch.line(lhs, rhs)
        }, true)
        scope.setObject("cline", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            val lhs = args[0] as Vec2
            val rhs = args[1] as Vec2
            return@ObjectFunction sketch.cline(lhs, rhs)
        }, true)
        scope.setObject("circle", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            val world = scope.world
            val lhs = args[0] as Vec2
            val rhs = Expr.orLiteral(world, args[1])
            return@ObjectFunction sketch.circle(lhs, rhs)
        }, true)
        scope.setObject("arc", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            val world = scope.world
            val p0 = args[0] as Vec2
            val p1 = args[1] as Vec2
            return@ObjectFunction sketch.arc(p0, p1, sketch.param(-1.0))
        }, true)


        scope.setObject("eq", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            val world = scope.world
            var lhs = args[0]
            var rhs = args[1]
            if( lhs is Vec2 && rhs is Vec2 ){
                return@ObjectFunction sketch.eq(lhs, rhs)
            }

            lhs = Expr.orLiteral(world, args[0])
            rhs = Expr.orLiteral(world, args[1])
            return@ObjectFunction sketch.eq(lhs, rhs)
        }, true)
        scope.setObject("len", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            val world = scope.world
            val lhs = args[0] as Segment2
            val rhs = Expr.orLiteral(world, args[1])
            return@ObjectFunction sketch.len(lhs, rhs)
        }, true)
        scope.setObject("perp", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            val seg0 = args[0] as Segment2
            val seg1 = args[1] as Segment2
            return@ObjectFunction sketch.perp(seg0, seg1)
        }, true)
        scope.setObject("tangent", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            val circle = args[0] as CircleLike
            val line = args[1] as Segment2
            return@ObjectFunction sketch.tangent(circle, line)
        }, true)
        scope.setObject("extrude", ObjectFunction{ scope, args ->
            val project = scope.project
            val name = args[0] as String
            val sketch = project.sketches.find { it.name == name }!!
            val height = Expr.orLiteral(scope.world, args[1])
            return@ObjectFunction project.extrude(sketch, height)
        }, true)
        scope.setObject("origin", world.ORIGIN, true)

        scope.setObject("fit", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            sketch.solve(1e-7)
        }, true)

        val result = expr.eval(scope)

        return project
    }

    fun execute(code: String) : ExecutionResult {
        val reset = System.out

        val newContext = SimpleScriptContext()
        val output = ByteArrayOutputStream()
        val printWriter = PrintWriter(output, true)
        newContext.writer = printWriter
        newContext.errorWriter = printWriter
        val stream = PrintStream(output)

        //System.setOut(stream)
        //System.setErr(stream)
        try{
            val expr = Parser.parse(code)
            val project = run(expr)
            return ExecutionResult(output.toString(), project)
        } catch (e: ScriptException){
            e.printStackTrace()
            val cause = e.cause
            if(cause is LineException){
                throw cause
            }else{
                val lineErrors = mutableListOf<LineError>()
                val pattern = "^(?<msg>.+) \\((?<file>.+?):(?<line>\\d+):(?<column>\\d+)\\)$".toRegex()
                for(line in e.message?.lines() ?: emptyList()){
                    val result = pattern.matchEntire(line)
                    if(result != null){

                        val msg = result.groups[1]!!.value
                        val file = result.groups[2]!!.value
                        val line = result.groups[3]!!.value.toInt()
                        val column = result.groups[4]!!.value.toInt()

                        lineErrors.add(LineError(msg, line, column))
                    }
                }

                throw LineException(lineErrors, output.toString() + " " + e.message)
            }
        }finally {
            System.setOut(reset)
            System.setErr(reset)
        }
    }
}
