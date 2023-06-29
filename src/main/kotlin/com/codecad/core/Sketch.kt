package com.codecad.core

import com.codecad.common.LineError
import com.codecad.core.sketch.World
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class Sketch(val project: Project) {
    val world = World()
    val params = HashSet<Param>()
    val constraints = HashSet<Constraint>()
    val figures = TreeSet<Figure>(){ a, b  ->
        val comp = a.type.prio.compareTo(b.type.prio)
        if(comp == 0) 1 else comp
    }

    fun createParam(value: Double = 0.0): Param {
        val param = Param( world, value )
        project.tracker.params.add(param)
        params.add(param)
        return param
    }

    fun createLiteral(value: Double = 0.0) : Expr {
        return world.literal(value)
    }

    fun createConstPoint(x: Double = 0.0, y: Double = 0.0): Point {
        val a = createLiteral(x)
        val b = createLiteral(y)
        val point = Point(a, b)
        figures.add(point)
        return point
    }

    fun createPoint(x: Expr, y: Expr): Point {
        val point = Point(x,y)
        figures.add(point)
        return point
    }

    fun createPoint(x: Double = 0.0, y: Double = 0.0): Point {
        val a = createParam(x)
        val b = createParam(y)
        return createPoint(a,b)
    }

    fun createLine(a: Point, b: Point, type: LineType = LineType.Normal): LineSegment {
        val line = LineSegment(a, b, type)
        figures.add(line)
        return line
    }

    fun createCircle(center: Point, radius: Expr): Circle {
        val circle = Circle(center, radius)
        figures.add(circle)
        return circle
    }

    fun createArc(p0: Point, p1: Point, radius: Expr): Arc {
        val circle = Arc(p0,p1,radius)
        figures.add(circle)
        return circle
    }

    fun createFunction(function : (Expr) -> Point) : FunctionFigure {
        val function = FunctionFigure(function)
        figures.add(function)
        return function
    }

    fun createCircle(): Circle {
        return createCircle(createPoint(), createParam(1.0))
    }

    fun createLine(x: Double = 0.0, y: Double = 0.0, x2: Double = 0.0, y2: Double = 0.0): LineSegment {
        return createLine(createPoint(x, y), createPoint(x2, y2))
    }

    fun createConstLine(x: Double = 0.0, y: Double = 0.0, x2: Double = 0.0, y2: Double = 0.0): LineSegment {
        return createLine(createConstPoint(x, y), createConstPoint(x2, y2))
    }

    fun addConstraintImpl(constraint: Constraint){
        constraints.add(constraint)
    }

    fun addConstraint(constraint: Constraint, tryBest: Boolean = false) : Constraint {
        println(constraint::class.simpleName)
        constraints.add(constraint)


        val constraintLookup = mutableMapOf<Constraint, HashSet<Param>>()
        val paramLookup = mutableMapOf<Param, HashSet<Constraint>>()

        for( con in constraints ){
            for(param in params){
                val derivate = con.equation.derivative(param)
                if(derivate !is Literal){
                    constraintLookup.computeIfAbsent(con){ HashSet() }.add(param)
                    paramLookup.computeIfAbsent(param){ HashSet() }.add(con)
                }
            }
        }

        class Test(var level: Int, val param : Param) : Comparable<Test>{
            override fun compareTo(other: Test): Int {
                return level.compareTo(other.level)
            }
        }

        val priorityQueue = PriorityQueue<Test>()
        val visited = mutableSetOf<Param>()
        val stages = mutableListOf<MutableSet<Param>>()

        stages.add( mutableSetOf())

        constraintLookup[constraint]?.forEach {
            visited.add(it)
            priorityQueue.offer(Test(0, it))
            stages[0].add(it)
        }

        try{
            while(priorityQueue.isNotEmpty()){
                val test = priorityQueue.poll()

                paramLookup[test.param]!!.forEach { con ->
                    constraintLookup[con]!!.forEach { param ->
                        if(visited.add(param)){
                            priorityQueue.offer(Test(test.level + 1, param))
                            if(test.level + 1 >= stages.size){
                                stages.add(mutableSetOf())
                            }
                            stages[test.level + 1].add(param)
                        }
                    }
                }
            }

            if(tryBest){
                solveImpl(10e-8, stages)
            }else{
                solve(10e-8, stages)
            }
            //TODO
            //constraint.prune(this)
        }catch (e: Exception){
            throw e
        }

        return constraint
    }

    fun solveImpl(accuracy: Double, params: List<Set<Param>> = listOf(this.params)) : Boolean{
        val solver = Solver(project.tracker)
        val result = solver.solve(world, listOf(this.params), ArrayList(constraints),accuracy)
        return result
    }

    fun solve(accuracy: Double, params: List<Set<Param>> = listOf(this.params)) {
        val start = System.currentTimeMillis()

        val result = solveImpl(accuracy, params)

        if(!result){
            var error = 0.0
            val locations = mutableListOf<LineError>()
            for(constraint in constraints){
                val constraintError = constraint.equation
                val value = constraintError.evalDouble()
                error += value
                if(value > accuracy){
                    locations.add(LineError("constraint could not be resolved", constraint.lineNumber, 0))
                    println("$constraint: line: ${constraint.lineNumber}  $constraintError > $accuracy")
                }
            }

            throw LineException(locations)
        }


        val end = System.currentTimeMillis()
        println("time need: ${end - start}")
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for (element in figures) {
            sb.append(element).append("\n")
        }
        return sb.toString()
    }
}


abstract class Component{
    //abstract fun names() : List<String>
    abstract fun build(sketch: SketchScope)
}

class RoundRect : Component(){
    lateinit var center: Point
    lateinit var width: Expr
    lateinit var height: Expr

    override fun build(sketch: SketchScope) {
        with(sketch){
            val topLine = line(point(0.0, 1.0),point(1.0,1.0))
            val bottomLine = line(point(0.0,0.0),point(1.0,0.0))

            val vertLine = cline(topLine.p0, bottomLine.p0)
            val vertLine2 = cline(topLine.p1, bottomLine.p1)

            val leftArc = arc(vertLine.p0, vertLine.p1, param(0.2))
            val rightArc = arc(vertLine2.p1, vertLine2.p0, param(0.2))

            val centerLine = cline(leftArc.center, rightArc.center)

            equals(topLine.length, bottomLine.length)

            perpendicular(topLine, vertLine)

            equals(leftArc.radius, rightArc.radius)
            equals(vertLine.length, vertLine2.length)

            equals(topLine.p0.y, topLine.p1.y)

            equals(leftArc.center, vertLine.midPoint )
            equals(rightArc.center, vertLine2.midPoint )

            center = centerLine.midPoint
            width = centerLine.length
            height = vertLine.length
        }
    }

}

class Rect : Component() {
    lateinit var a: Point
    lateinit var b: Point
    lateinit var c: Point
    lateinit var d: Point

    lateinit var center: Point

    lateinit var top: LineSegment
    lateinit var right: LineSegment
    lateinit var bottom: LineSegment
    lateinit var left: LineSegment

    lateinit var width: Expr
    lateinit var height: Expr

    fun names(): List<String> {
        return listOf("width", "height", "top", "bottom")
    }

    override fun build(sketch: SketchScope) {
        with(sketch){
            a = point(0.0,0.0)
            b = point(1.0,0.0)
            c = point(1.0,1.0)
            d = point(0.0,1.0)

            top = line(a,b)
            right = line(b,c)
            bottom = line(c,d)
            left = line(d,a)

            width = top.length
            height = right.length
            center = (a + b + c + d) / 4.0

            equals((c-a).length(), (d - b).length())
            equals(top.length , bottom.length)
            equals(left.length , right.length)
        }
    }
}

/*
* fun Canvas.line(line: com.codecad.core.Line){
    line(line.a.x.value,line.a.y.value, line.b.x.value,line.b.y.value)
}
fun Canvas.circle(circle: com.codecad.core.Circle){
    circle(circle.center.x.value,circle.center.y.value, circle.rad.value)
}
* */
