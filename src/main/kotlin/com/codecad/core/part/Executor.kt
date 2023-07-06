package com.codecad.core.part

import com.codecad.core.CircleLike
import com.codecad.core.Segment2
import com.codecad.core.Vec2
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.parser.ObjectFunction
import com.codecad.core.parser.Parser
import com.codecad.core.scope.MutualScope
import com.codecad.core.scope.partStudio
import com.codecad.core.scope.sketch
import com.codecad.core.scope.world
import com.codecad.core.shape.Rect
import com.codecad.core.shape.RoundRect
import org.slf4j.LoggerFactory

class ExecutionResult(val output: String, val partStudio: PartStudio)

class Executor private constructor(){
    companion object{
        private val LOGGER = LoggerFactory.getLogger(Executor::class.java)
        fun execute(code: String) : ExecutionResult {
            return Executor().execute(code)
        }
    }

    fun run(expr: Expr) : PartStudio {
        val scope = MutualScope()

        val partStudio = PartStudio()
        scope.partStudio = partStudio
        val world = partStudio.world
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
        scope.setObject("rect", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            val rect = Rect()
            rect.build(sketch)
            return@ObjectFunction rect
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
        scope.setObject("minimize", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            val expr = Expr.orLiteral(world, args[0])
            return@ObjectFunction sketch.minimize(expr)
        }, true)

        scope.setObject("extrude", ObjectFunction{ scope, args ->
            val project = scope.partStudio
            val name = args[0] as String
            val sketch = project.sketches.find { it.name == name }!!
            val height = Expr.orLiteral(scope.world, args[1])
            return@ObjectFunction project.extrude(sketch, height)
        }, true)
        scope.setObject("extrudeAll", ObjectFunction{ scope, args ->
            val project = scope.partStudio
            val name = args[0] as String
            val sketch = project.sketches.find { it.name == name }!!
            val height = Expr.orLiteral(scope.world, args[1])
            return@ObjectFunction project.extrudeAll(sketch, height)
        }, true)
        scope.setObject("origin", world.ORIGIN, true)

        scope.setObject("fit", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            sketch.solve(1e-7)
        }, true)

        val result = expr.eval(scope)

        return partStudio
    }

    fun execute(code: String) : ExecutionResult {
        val expr = Parser.parse(code)
        val project = run(expr)
        return ExecutionResult("", project)
    }
}
