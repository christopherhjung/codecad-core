import org.junit.jupiter.api.Test
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConstraintTest {

    @Test
    fun tangentConstraint(){
        val sqrt2 = 2.0.pow(-.5)
        val x = Parameter(0.0)
        val y = Parameter(1.0)
        val line = Line(Point(Parameter(0.0), Parameter(0.0)), Point(Parameter(1.0), Parameter(1.0)))
        val circle = Circle(Point(x, y), Value.const(1.0))
        val constraint = CircleTangent(circle, line)
        val delta = constraint.getDelta()
        assertEquals(sqrt2, delta.value)
        y.value = -1.0
        assertEquals(sqrt2, delta.value)
    }

    @Test
    fun tangentSolveFromAbove(){
        val project = Project()
        val sketch = Sketch(project)
        val x = sketch.createParameter(0.0)
        val y = sketch.createParameter(1.0)
        val center = Point(x, y)
        val line = sketch.createLine(sketch.createConstPoint(0.0,0.0), sketch.createConstPoint(1.0,0.0))
        val circle =  sketch.createCircle(center, sketch.createConst(0.5))
        sketch.addConstraint(CircleTangent(circle, line))
        sketch.solve(1e-6)
        assertEquals(0.5, y.value, 1e-3)
        assertEquals(0.0, x.value, 1e-3)
    }

    @Test
    fun tangentSolveFromAboveLarge(){
        val project = Project()
        val sketch = Sketch(project)
        val x = sketch.createParameter(10.0)
        val y = sketch.createParameter(100.0)
        val center = Point(x, y)
        val line = sketch.createLine(sketch.createConstPoint(0.0,0.0), sketch.createConstPoint(1.0,0.0))
        val circle =  sketch.createCircle(center, sketch.createConst(0.5))
        sketch.addConstraint(CircleTangent(circle, line))
        sketch.solve(1e-6)
        assertEquals(0.5, y.value, 1e-3)
        assertEquals(10.0, x.value, 1e-3)
    }

    @Test
    fun tangentSolveFromBelow(){
        val project = Project()
        val sketch = Sketch(project)
        val x = sketch.createParameter(0.0)
        val y = sketch.createParameter(-1.0)
        val center = Point(x, y)
        val line = sketch.createLine(sketch.createConstPoint(0.0,0.0), sketch.createConstPoint(1.0,0.0))
        val circle =  sketch.createCircle(center, sketch.createConst(0.5))
        sketch.addConstraint(CircleTangent(circle, line))
        sketch.solve(1e-6)
        assertEquals(-0.5, y.value, 1e-3)
        assertEquals(0.0, x.value, 1e-3)
    }

    @Test
    fun tangentSolveDiagonal(){
        val project = Project()
        val sketch = Sketch(project)
        val x = sketch.createParameter(0.0)
        val y = sketch.createParameter(1.0)
        val center = Point(x, y)
        val line = sketch.createLine(sketch.createConstPoint(0.0,0.0), sketch.createConstPoint(1.0,1.0))
        val circle =  sketch.createCircle(center, sketch.createConst(0.5))
        sketch.addConstraint(CircleTangent(circle, line))
        val success = sketch.solveImpl(1e-8)
        val offset = Const(0.5 / sqrt(2.0))
        val target = line.midPoint + Point(-offset, offset)
        assertEquals(target.x.value, center.x.value, 1e-3)
        assertEquals(target.y.value, center.y.value, 1e-3)
        assertTrue(success)
    }

    @Test
    fun pointOnPointSimple(){
        val project = Project()
        val sketch = Sketch(project)
        val x = sketch.createParameter(10.0)
        val y = sketch.createParameter(100.0)
        val current = sketch.createPoint(x,y)
        val target = sketch.createConstPoint(Math.PI, 2 * Math.PI)
        val constraint = PointOnPoint(current, target)
        val derivative = constraint.equation.derivative(y.ref as Parameter)
        val sizeBefore = sketch.params.size
        sketch.addConstraintImpl(constraint)
        val sizeAfter = sketch.params.size
        //assertEquals(sizeBefore, sizeAfter + 2)
        val success = sketch.solveImpl(1e-6)
        assertEquals(Math.PI, x.value, 1e-3)
        assertEquals(2 * Math.PI, y.value, 1e-3)
        println(success)
    }

}
