import com.codecad.core.*
import com.codecad.core.part.PartStudio
import com.codecad.core.parser.ObjectFunction
import com.codecad.core.parser.Parser
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec2Expr
import com.codecad.core.scope.MutualScope
import com.codecad.core.scope.*
import org.junit.jupiter.api.Test

class ParserTest {

    @Test
    fun cacheTest(){
        val world = World()
        val expr = Parser.parse("""
            sketch test{               
                line topLine(param2(0.0, 1.0), param2(1.0, 1.0))
                line bottomLine(origin, param2(1.0, 0.0))
                line vertLine(topLine.p0, bottomLine.p0)
                line vertLine2(topLine.p1, bottomLine.p1)
                
                eq(topLine.p0.y, topLine.p1.y)
                eq(bottomLine.p0.y, bottomLine.p1.y)
                eq(vertLine.p0.x, vertLine.p1.x)
                eq(vertLine2.p0.x, vertLine2.p1.x)

                eq(topLine.length, bottomLine.length)
                eq(vertLine.length, vertLine2.length)
                eq(vertLine2.length, 2.0)
                eq(topLine.length, 1.0)

                perp(topLine, vertLine)
                
                fit()
            }
               
        """.trimIndent(), world)


        val scope = MutualScope()

        val partStudio = PartStudio(world)
        scope.partStudio = partStudio
        scope.world = world
        scope.setObject("println", ObjectFunction{ _, args ->
            println(args[0])
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
            return@ObjectFunction sketch.param()
        }, true)
        scope.setObject("line", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            return@ObjectFunction sketch.line()
        }, true)
        scope.setObject("circle", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            val world = scope.world
            val lhs = args[0] as Vec2Expr
            val rhs = Expr.orLiteral(world, args[1])
            return@ObjectFunction sketch.circle(lhs, rhs)
        }, true)
        scope.setObject("arc", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            val world = scope.world
            val p0 = args[0] as Vec2Expr
            val p1 = args[1] as Vec2Expr
            return@ObjectFunction sketch.arc(p0, p1)
        }, true)
        scope.setObject("eq", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            val world = scope.world
            var lhs = args[0]
            var rhs = args[1]
            if( lhs is Vec2Expr && rhs is Vec2Expr){
                return@ObjectFunction sketch.eq(lhs, rhs)
            }

            lhs = Expr.orLiteral(world, args[0])
            rhs = Expr.orLiteral(world, args[1])
            return@ObjectFunction sketch.eq(lhs, rhs)
        }, true)
        scope.setObject("len", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            val world = scope.world
            val lhs = args[0] as SketchLineExpr
            val rhs = Expr.orLiteral(world, args[1])
            return@ObjectFunction sketch.len(lhs, rhs)
        }, true)
        scope.setObject("perp", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            val seg0 = args[0] as SketchLineExpr
            val seg1 = args[1] as SketchLineExpr
            return@ObjectFunction sketch.perp(seg0, seg1)
        }, true)
        scope.setObject("origin", world.ZeroVec2, true)

        scope.setObject("fit", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            sketch.solve(1e-7)
        }, true)

        val result = expr.eval(scope)
    }

}
