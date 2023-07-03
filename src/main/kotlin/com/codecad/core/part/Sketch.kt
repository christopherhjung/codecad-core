package com.codecad.core.part

import com.codecad.core.*
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.primitive.LiteralExpr
import com.codecad.core.ast.primitive.ParamExpr
import com.codecad.core.optimizer.Solver
import com.codecad.core.sketch.*
import java.util.*

class Sketch(val project: Project, val name: String) {
    val params = HashSet<ParamExpr>()
    val constraints = HashSet<Constraint>()
    val figures = ArrayList<Figure>()
    val lineType = HashMap<Figure, LineType>()
    val world: World = project.world

    fun param(value: Double = 0.0): ParamExpr {
        val param = ParamExpr( world, value )
        project.tracker.params.add(param)
        params.add(param)
        return param
    }

    fun createLiteral(value: Double = 0.0) : Expr {
        return world.literal(value)
    }

    fun constPoint(x: Double = 0.0, y: Double = 0.0): Vec2 {
        val a = createLiteral(x)
        val b = createLiteral(y)
        val point = Vec2(a, b)
        figures.add(point)
        return point
    }

    fun point(x: Expr, y: Expr): Vec2 {
        val point = Vec2(x,y)
        figures.add(point)
        return point
    }

    fun point(x: Double = 0.0, y: Double = 0.0): Vec2 {
        val a = param(x)
        val b = param(y)
        return point(a,b)
    }

    fun line(a: Vec2, b: Vec2, type: LineType = LineType.Normal): Segment2 {
        val line = Segment2(a, b)
        figures.add(line)
        lineType.putIfAbsent(line, type)
        return line
    }

    fun cline(a: Vec2, b: Vec2) : Segment2 {
        return line(a,b, LineType.Construction)
    }

    fun circle(center: Vec2, radius: Expr): Circle {
        val circle = Circle(center, radius)
        figures.add(circle)
        return circle
    }

    fun arc(p0: Vec2, p1: Vec2, radius: Expr): Arc {
        val arc = Arc(p0,p1,radius)
        figures.add(arc)
        figures.add(arc.center)
        return arc
    }

    fun func(function : (Expr) -> Vec2) : FunctionFigure {
        val function = FunctionFigure(function)
        figures.add(function)
        return function
    }

    fun circle(): Circle {
        return circle(point(), param(1.0))
    }

    fun line(x: Double = 0.0, y: Double = 0.0, x2: Double = 0.0, y2: Double = 0.0): Segment2 {
        return line(point(x, y), point(x2, y2))
    }

    fun constLine(x: Double = 0.0, y: Double = 0.0, x2: Double = 0.0, y2: Double = 0.0): Segment2 {
        return line(constPoint(x, y), constPoint(x2, y2))
    }

    fun tangent(circle: CircleLike, line: Segment2) {
        addConstraintImpl(CircleTangent(circle, line))
    }

    fun pointOnLineMidpoint(point: Vec2, line: Segment2) {
        addConstraintImpl(PointOnLineMidpoint(point, line))
    }

    fun pointOnLine(point: Vec2, line: Segment2) {
        addConstraintImpl(PointOnLine(point, line))
    }

    fun horizontal(line: Segment2) {
        addConstraintImpl(Horizontal(line))
    }

    fun vertical(line: Segment2) {
        addConstraintImpl(Vertical(line))
    }

    fun pointOnCircle(point: Vec2, circle: Circle) {
        addConstraintImpl(PointOnCircle(point, circle))
    }

    fun equalLength(line1: Segment2, line2: Segment2) {
        addConstraintImpl(Equals(line1.length, line2.length))
    }

    fun len(line1: Segment2, length: Expr) {
        addConstraintImpl(Equals(line1.length, length))
    }

    fun angle(line1: Segment2, line2: Segment2, angle: Expr) {
        addConstraintImpl(InternalAngle(line1, line2, angle))
    }

    fun addConstraint(constraint: Constraint) {
        addConstraintImpl(constraint)
    }

    fun perp(line1: Segment2, line2: Segment2){
        addConstraintImpl(Perpendicular(line1, line2))
    }

    fun parallel(line1: Segment2, line2: Segment2){
        addConstraintImpl(Parallel(line1, line2))
    }

    fun pointOnPoint(point1: Vec2, vec2: Vec2) {
        addConstraintImpl(PointOnPoint(point1, vec2))
    }

    fun eq(point1: Vec2, vec2: Vec2) {
        addConstraintImpl(PointOnPoint(point1, vec2))
    }

    fun eq(value1 : Expr, value2: Expr) {
        addConstraintImpl(Equals(value1, value2))
    }

    fun radius(circle: Circle, value: Expr) {
        addConstraintImpl(Equals(circle.radius, value))
    }

    fun addConstraintImpl(constraint: Constraint){
        constraints.add(constraint)
    }

    fun addConstraint(constraint: Constraint, tryBest: Boolean = false) : Constraint {
        println(constraint::class.simpleName)
        constraints.add(constraint)

        val constraintLookup = mutableMapOf<Constraint, HashSet<ParamExpr>>()
        val paramLookup = mutableMapOf<ParamExpr, HashSet<Constraint>>()

        for( con in constraints ){
            for(param in params){
                val derivate = con.equation.derivative(param)
                if(derivate !is LiteralExpr){
                    constraintLookup.computeIfAbsent(con){ HashSet() }.add(param)
                    paramLookup.computeIfAbsent(param){ HashSet() }.add(con)
                }
            }
        }

        class Test(var level: Int, val param : ParamExpr) : Comparable<Test>{
            override fun compareTo(other: Test): Int {
                return level.compareTo(other.level)
            }
        }

        val priorityQueue = PriorityQueue<Test>()
        val visited = mutableSetOf<ParamExpr>()
        val stages = mutableListOf<MutableSet<ParamExpr>>()

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

    fun solveImpl(accuracy: Double, params: List<Set<ParamExpr>> = listOf(this.params)) : Boolean{
        val solver = Solver(project.tracker)
        val result = solver.solve(world, params.flatten(), ArrayList(constraints),accuracy)
        return result
    }

    fun solve(accuracy: Double, params: List<Set<ParamExpr>> = listOf(this.params)) {
        val start = System.currentTimeMillis()

        val result = solveImpl(accuracy, params)

        /*
        if(!result){
            var error = 0.0
            val locations = mutableListOf<LineError>()
            for(constraint in constraints){
                val constraintError = constraint.equation
                val value = constraintError.evalDouble()
                error += value
                if(value > accuracy){
                    //locations.add(LineError("constraint could not be resolved", constraint.lineNumber, 0))
                    //println("$constraint: line: ${constraint.lineNumber}  $constraintError > $accuracy")
                }
            }

            throw LineException(locations)
        }*/

        val end = System.currentTimeMillis()
        println("time: ${end - start}ms")
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
    abstract fun build(sketch: Sketch)
}

class RoundRect : Component(){
    lateinit var center: Vec2
    lateinit var width: Expr
    lateinit var height: Expr

    override fun build(sketch: Sketch) {
        with(sketch){
            val topLine = line(point(0.0, 1.0),point(1.0,1.0))
            val bottomLine = line(point(0.0,0.0),point(1.0,0.0))

            val vertLine = cline(topLine.p0, bottomLine.p0)
            val vertLine2 = cline(topLine.p1, bottomLine.p1)

            val leftArc = arc(vertLine.p0, vertLine.p1, param(0.2))
            val rightArc = arc(vertLine2.p1, vertLine2.p0, param(0.2))

            val centerLine = cline(leftArc.center, rightArc.center)

            eq(topLine.length, bottomLine.length)

            perp(topLine, vertLine)

            eq(leftArc.radius, rightArc.radius)
            eq(vertLine.length, vertLine2.length)

            eq(topLine.p0.y, topLine.p1.y)

            eq(leftArc.center, vertLine.midPoint )
            eq(rightArc.center, vertLine2.midPoint )

            center = centerLine.midPoint
            width = centerLine.length
            height = vertLine.length
        }
    }

}

class Rect : Component() {
    lateinit var a: Vec2
    lateinit var b: Vec2
    lateinit var c: Vec2
    lateinit var d: Vec2

    lateinit var center: Vec2

    lateinit var top: Segment2
    lateinit var right: Segment2
    lateinit var bottom: Segment2
    lateinit var left: Segment2

    lateinit var width: Expr
    lateinit var height: Expr

    fun names(): List<String> {
        return listOf("width", "height", "top", "bottom")
    }

    override fun build(sketch: Sketch) {
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

            eq((c-a).length(), (d - b).length())
            eq(top.length , bottom.length)
            eq(left.length , right.length)
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
