import com.codecad.core.*
import com.codecad.core.sketch.*
import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConstraintTest {


    @Test
    fun tangentSolveFromAbove(){
        val project = Project()
        val sketch = Sketch(project)
        val x = sketch.createParam(0.0)
        val y = sketch.createParam(1.0)
        val center = Point(x, y)
        val line = sketch.createLine(sketch.createConstPoint(0.0,0.0), sketch.createConstPoint(1.0,0.0))
        val circle =  sketch.createCircle(center, sketch.createLiteral(0.5))
        sketch.addConstraint(CircleTangent(circle, line))
        sketch.solve(1e-6)
        assertEquals(0.5, y.evalDouble(), 1e-3)
        assertEquals(0.0, x.evalDouble(), 1e-3)
    }

    @Test
    fun tangentSolveFromAboveLarge(){
        val project = Project()
        val sketch = Sketch(project)
        val x = sketch.createParam(10.0)
        val y = sketch.createParam(100.0)
        val center = Point(x, y)
        val line = sketch.createLine(sketch.createConstPoint(0.0,0.0), sketch.createConstPoint(1.0,0.0))
        val circle =  sketch.createCircle(center, sketch.createLiteral(0.5))
        sketch.addConstraint(CircleTangent(circle, line))
        sketch.solve(1e-6)
        assertEquals(0.5, y.evalDouble(), 1e-3)
        assertEquals(10.0, x.evalDouble(), 1e-3)
    }

    @Test
    fun tangentSolveFromBelow(){
        val project = Project()
        val sketch = Sketch(project)
        val x = sketch.createParam(0.0)
        val y = sketch.createParam(-1.0)
        val center = Point(x, y)
        val line = sketch.createLine(sketch.createConstPoint(0.0,0.0), sketch.createConstPoint(1.0,0.0))
        val circle =  sketch.createCircle(center, sketch.createLiteral(0.5))
        sketch.addConstraint(CircleTangent(circle, line))
        sketch.solve(1e-6)
        assertEquals(-0.5, y.evalDouble(), 1e-3)
        assertEquals(0.0, x.evalDouble(), 1e-3)
    }

    @Test
    fun tangentSolveDiagonal(){
        val project = Project()
        val sketch = Sketch(project)
        val x = sketch.createParam(0.0)
        val y = sketch.createParam(1.0)
        val center = Point(x, y)
        val line = sketch.createLine(sketch.createConstPoint(0.0,0.0), sketch.createConstPoint(1.0,1.0))
        val circle =  sketch.createCircle(center, sketch.createLiteral(0.5))
        sketch.addConstraint(CircleTangent(circle, line))
        val success = sketch.solveImpl(1e-8)
        val offset = sketch.createLiteral(0.5 / sqrt(2.0))
        val target = line.midPoint + Point(-offset, offset)
        assertEquals(target.x.evalDouble(), center.x.evalDouble(), 1e-3)
        assertEquals(target.y.evalDouble(), center.y.evalDouble(), 1e-3)
        assertTrue(success)
    }

    @Test
    fun pointOnPointSimple(){
        val project = Project()
        val sketch = Sketch(project)
        val x = sketch.createParam(10.0)
        val y = sketch.createParam(100.0)
        val current = sketch.createPoint(x,y)
        val target = sketch.createConstPoint(Math.PI, 2 * Math.PI)
        val constraint = PointOnPoint(current, target)
        val derivative = constraint.equation.derivative(y)
        val sizeBefore = sketch.params.size
        sketch.addConstraintImpl(constraint)
        val sizeAfter = sketch.params.size
        //assertEquals(sizeBefore, sizeAfter + 2)
        val success = sketch.solveImpl(1e-6)
        assertEquals(Math.PI, x.evalDouble(), 1e-3)
        assertEquals(2 * Math.PI, y.evalDouble(), 1e-3)
        println(success)
    }

}
