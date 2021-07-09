import java.lang.Math.random
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class Sketch {
    val params = HashSet<Parameter>()
    val constraints = HashSet<Constraint>()
    val elements = TreeSet<Element>(){  a,b  ->
        val comp = a.type.prio.compareTo(b.type.prio)
        if(comp == 0) 1 else comp
    }

    class PruningEntry(val proxies: MutableList<ProxyValue> = mutableListOf(), var fixed: Value? = null)
    val pruningTable = mutableMapOf<ProxyValue, PruningEntry>()

    fun createParameter(value: Double = 0.0): ProxyValue {
        val param = Parameter(value + (random() - 0.5) * 1e-5)
        params.add(param)
        val proxy = ProxyValue(param)
        pruningTable[proxy] = PruningEntry(mutableListOf(proxy))
        return proxy
    }

    fun createConst(value: Double = 0.0): Const {
        return Const(value)
    }

    fun createConstPoint(x: Double = 0.0, y: Double = 0.0): Point {
        val a = createConst(x)
        val b = createConst(y)
        val point = Point(a, b)
        elements.add(point)
        return point
    }

    fun createPoint(x: Double = 0.0, y: Double = 0.0): Point {
        val a = createParameter(x)
        val b = createParameter(y)
        val point = Point(a, b)
        elements.add(point)
        return point
    }

    fun createLine(a: Point, b: Point, type: LineType = LineType.ToolPath): Line {
        val line = Line(a, b, type)
        elements.add(line)
        return line
    }

    fun createCircle(center: Point, radius: Value): Circle {
        val circle = Circle(center, radius)
        elements.add(circle)
        return circle
    }

    fun createArc(center: Point, radius: Value, start: Value, end: Value): Circle {
        val circle = Circle(center, radius, start, end)
        elements.add(circle)
        return circle
    }

    fun createCircle(): Circle {
        return createCircle(createPoint(), createParameter(1.0))
    }

    fun createLine(x: Double = 0.0, y: Double = 0.0, x2: Double = 0.0, y2: Double = 0.0): Line {
        return createLine(createPoint(x, y), createPoint(x2, y2))
    }

    fun createConstLine(x: Double = 0.0, y: Double = 0.0, x2: Double = 0.0, y2: Double = 0.0): Line {
        return createLine(createConstPoint(x, y), createConstPoint(x2, y2))
    }

    fun addConstraint(constraint: Constraint) : Constraint{
        constraints.add(constraint)
        constraint.prune(this)
        try{
            solve(10e-6)
        }catch (e: Exception){
            throw e
        }
        return constraint
    }

    fun paramIsEquals(left: Value, right: Value) {
        if (left is ProxyValue) {
            val leftEntry = pruningTable[left]!!
            if (right is ProxyValue) {
                val rightEntry = pruningTable[right]!!

                if(leftEntry === rightEntry){
                    return
                }

                leftEntry.proxies.addAll(rightEntry.proxies)

                if(leftEntry.fixed != null && rightEntry.fixed != null){
                    throw java.lang.RuntimeException("not possible!!")
                }else if(rightEntry.fixed != null){
                    leftEntry.fixed = rightEntry.fixed
                }

                pruningTable[right] = leftEntry
            } else {
                if(leftEntry.fixed != null){
                    throw java.lang.RuntimeException("not possible!!")
                }

                leftEntry.fixed = right
            }
        } else if (right is ProxyValue) {
            val rightEntry = pruningTable[right]!!

            if(rightEntry.fixed != null){
                throw java.lang.RuntimeException("not possible!!")
            }

            rightEntry.fixed = left
        } else {
            throw RuntimeException("const cant be set equals")
        }
    }

    fun solve(accuracy: Double) {
        val start = System.currentTimeMillis()

        val solver = Solver()
        val result = solver.solve(ArrayList(params), ArrayList(constraints),accuracy)

        if(!result){
            var error = 0.0
            for(constraint in constraints){
                val constraintError = constraint.error()
                error += constraintError
                if(constraintError > accuracy){
                    println("$constraint: line: ${constraint.lineNumber}  $constraintError > $accuracy")
                }
            }

            throw RuntimeException("Could not solve constraints! Error: $error")
        }


        val end = System.currentTimeMillis()
        println("time need: ${end - start}")
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for (element in elements) {
            sb.append(element).append("\n")
        }
        return sb.toString()
    }
}

/*
* fun Canvas.line(line: Line){
    line(line.a.x.value,line.a.y.value, line.b.x.value,line.b.y.value)
}
fun Canvas.circle(circle: Circle){
    circle(circle.center.x.value,circle.center.y.value, circle.rad.value)
}
* */
