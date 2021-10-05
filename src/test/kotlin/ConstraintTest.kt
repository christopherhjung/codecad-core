import com.codecad.core.*
import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConstraintTest {

    fun assertPerpendicular(line1: LineSegment, line2: LineSegment){
        val equ = Perpendicular(line1, line2).equation
        assertEquals(equ.value, 0.0)
    }

    fun assertCrossPerpendicular(line1: LineSegment, line2: LineSegment){
        assertPerpendicular(line1, line2)
        assertPerpendicular(line2, line1)
    }

    @Test
    fun perpendicularTest(){
        val origin = Point.const(1.0,2.0)
        val right = LineSegment(origin, Point.const(2.0,2.0))
        val up = LineSegment(origin, Point.const(1.0,3.0))
        val left = LineSegment(origin, Point.const(0.0,2.0))
        val down = LineSegment(origin, Point.const(1.0,1.0))

        assertCrossPerpendicular(right, up)
        assertCrossPerpendicular(right, down)

        assertCrossPerpendicular(left, up)
        assertCrossPerpendicular(left, down)
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
