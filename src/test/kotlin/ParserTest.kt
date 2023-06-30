import com.codecad.core.*
import com.codecad.core.parser.ObjectFunction
import com.codecad.core.parser.Parser
import com.codecad.core.parser.ast.primitive.Expr
import com.codecad.core.scope.MutualScope
import com.codecad.core.scope.*
import org.junit.jupiter.api.Test

class ParserTest {

    @Test
    fun cacheTest(){
        var expr = Parser.parse("""
            sketch test{
                let a = param(9)
                let start = vec2(0.0, 0.0)
                let end = vec2(a, 1.0)
                let l1 = line(start, end)
                len(l1, 1.41421356237)
                fit()
                println("test----")
                println(a)
            }
               
        """.trimIndent())


        val scope = MutualScope()

        val project = Project()
        scope.project = project
        scope.world = project.world
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
            val init = (args[0] as Number).toDouble()
            return@ObjectFunction sketch.param(init)
        }, true)
        scope.setObject("line", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            val lhs = args[0] as Vec2
            val rhs = args[1] as Vec2
            return@ObjectFunction sketch.line(lhs, rhs)
        }, true)
        scope.setObject("circle", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            val world = scope.world
            val lhs = args[0] as Vec2
            val rhs = Expr.orLiteral(world, args[1])
            return@ObjectFunction sketch.circle(lhs, rhs)
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
        scope.setObject("fit", ObjectFunction{ scope, args ->
            val sketch = scope.sketch
            sketch.solve(1e-5)
        }, true)

        val result = expr.eval(scope)
        println(expr)
    }

}
