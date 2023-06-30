package com.codecad.core

import com.codecad.common.LineD
import com.codecad.common.PointD
import com.codecad.core.parser.ast.primitive.Expr
import com.codecad.core.sketch.*
import kotlin.reflect.KClass

class PatternScope(project: Project, val count: Int, val center: Vec2) : SketchScope(project) {

    val allCrawler = mutableMapOf<Vec2, MutableList<MutableList<Vec2>>>()
    val pointLookup = mutableMapOf<Vec2, Array<Vec2?>>()

    fun all(ref: Vec2, list: MutableList<Vec2>)  {
        allCrawler.computeIfAbsent(ref){ mutableListOf()}.add(list)
    }

    var current = 0

    private fun rotatePoint(point: Vec2, angle: Expr) : Vec2 {
        val array = pointLookup.computeIfAbsent(point){Array(count - 1){null} }

        if(array[current] == null){
            array[current] = point.rotate(center, angle)
        }

        return array[current]!!
    }

    fun finish(){
        sketch.solve(1e-8)
        val rawElements = sketch.figures.toList()

        for(i in 0 until count - 1){
            val angle = literal((2 * Math.PI / count) * (i + 1))
            current = i

            for(element in rawElements){
                sketch.figures.add(
                    if(element is Vec2){
                        rotatePoint(element, angle)
                    }else if(element is Segment2){
                        Segment2(
                            rotatePoint(element.p0, angle),
                            rotatePoint(element.p1, angle)
                        )
                    }else if(element is Circle){
                        if(element is Arc){
                            Arc(
                                rotatePoint(element.p0, angle),
                                rotatePoint(element.p1, angle),
                                element.helper
                            )
                        }else{
                            Circle(
                                rotatePoint(element.center, angle),
                                element.radius
                            )
                        }
                    }else continue
                )
            }
        }

        for((ref, crawler) in allCrawler.entries){
            for(list in crawler){
                list.add(ref)
                list.addAll(pointLookup[ref]!!.map { it!! })
            }
        }
    }
}

open class SketchScope(val project: Project) {
    val sketch = Sketch(project)

    val deg = 0
    val rad = 1

    val mm = 2
    val cm = 3

    fun Expr.isEquals(other : Expr) {
        equal(this, other)
    }

    infix fun Double.unit(other: Int): Double {
        return if (other == deg) {
            Math.toRadians(this)
        } else {
            this
        }
    }

    fun <T> create(type: KClass<T>) : T where T : Component{
        val component = type.constructors.first().call()
        component.build(sketch)
        return component
    }

    inline fun <reified T> create() : T where T : Component{
        val component = T::class.constructors.first().call()
        component.build(sketch)
        return component
    }

    fun pattern(repeat: Int, point: Vec2, init: PatternScope.(Vec2) -> Unit): Sketch {
        val builder = PatternScope(project, repeat, point)
        builder.init(point)
        builder.finish()
        sketch.figures.addAll(builder.sketch.figures)
        return builder.sketch
    }

    fun param(value: Number = 0.0): Expr {
        return sketch.param(value.toDouble())
    }

    fun <T> list() : MutableList<T>{
        return mutableListOf()
    }

    fun literal(value: Number = 0.0): Expr {
        return sketch.createLiteral(value.toDouble())
    }

    fun literalPoint(x: Number = 0.0, y: Number = 0.0): Vec2 {
        return sketch.constPoint(x.toDouble(), y.toDouble())
    }

    fun point(x: Number = 0.0, y: Number = 0.0): Vec2 {
        return sketch.point(x.toDouble(), y.toDouble())
    }

    fun line(a: Vec2, b: Vec2): Segment2 {
        return sketch.line(a, b, LineType.Normal)
    }

    fun cline(a: Vec2, b: Vec2): Segment2 {
        return sketch.line(a, b, LineType.Construction)
    }

    fun circle(center: Vec2, radius: Expr): Circle {
        return sketch.circle(center, radius)
    }

    fun arc(p0: Vec2, p1: Vec2, radius: Expr): Arc {
        return sketch.arc(p0,p1,radius)
    }

    fun func(block: (Expr) -> Vec2): FunctionFigure {
        return sketch.func(block)
    }

    fun polygon(vararg points: Vec2) : List<Segment2> {
        val lines = mutableListOf<Segment2>()
        for ((left, right) in points.toList().rollover()) {
            lines.add(line(left, right))
        }

        return lines
    }

    fun line(x: Number = 0.0, y: Number = 0.0, x2: Number = 0.0, y2: Number = 0.0): Segment2 {
        return sketch.line(x.toDouble(), y.toDouble(), x2.toDouble(), y2.toDouble())
    }

    fun tangent(circle: Circle, line: Segment2) {
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
        addConstraintImpl(EqualLength(line1, line2))
    }

    fun length(line1: Segment2, length: Expr) {
        addConstraintImpl(LineLength(line1, length))
    }

    fun angle(line1: Segment2, line2: Segment2, angle: Expr) {
        addConstraintImpl(InternalAngle(line1, line2, angle))
    }

    fun addConstraint(constraint: Constraint) {
        addConstraintImpl(constraint)
    }

    fun perpendicular(line1: Segment2, line2: Segment2){
        addConstraintImpl(Perpendicular(line1, line2))
    }

    fun parallel(line1: Segment2, line2: Segment2){
        addConstraintImpl(Parallel(line1, line2))
    }

    fun pointOnPoint(point1: Vec2, vec2: Vec2) {
        addConstraintImpl(PointOnPoint(point1, vec2))
    }

    fun equal(point1: Vec2, vec2: Vec2) {
        addConstraintImpl(PointOnPoint(point1, vec2))
    }

    fun equal(value1 : Expr, value2: Expr) {
        addConstraintImpl(Equals(value1, value2))
    }

    fun radius(circle: Circle, value: Expr) {
        addConstraintImpl(Radius(circle, value))
    }

    fun origin() : Vec2 {
        return sketch.world.ORIGIN
    }

    fun axisX() : Segment2 {
        return sketch.world.AXIS_X
    }

    fun axisY() : Segment2 {
        return sketch.world.AXIS_Y
    }

    private fun addConstraintImpl(constraint: Constraint){
        //val callersLineNumber = Thread.currentThread().stackTrace[3].lineNumber
        sketch.addConstraint(constraint)
    }

    fun add(component: Component){
        component.build(sketch)
    }
}




/*
class Point3D(val x: Double, val y: Double, val z: Double){
    override fun toString(): String {
        return "Point3D(x=$x, y=$y)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Point3D) return false

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        return result
    }
}*/


fun sketchToLines(sketch: Sketch, ignoreConstruction: Boolean = false) : List<LineD>{
/*
    val split = HashMap<Figure, MutableList<Point>>()

    for( constraint in sketch.constraints ){
        if( constraint is PointOnCircle ){
            split.computeIfAbsent(constraint.circle){ mutableListOf()}
        }else if(constraint is PointOnPoint){

        }
    }*/

    val list = mutableListOf<LineD>()
    for(figure in sketch.figures){
        if(ignoreConstruction && sketch.lineType[figure] == LineType.Construction){
            continue
        }

        if(figure is Segment2){
            list.add(LineD(figure.p0.fixed(), figure.p1.fixed()))
        }else if(figure is Arc){
            val span = ArcPlotter(figure)
            var last: Vec2? = null
            for( i in 0 .. 200){
                val t = i / 200.0

                val point = span.getPoint(t)
                if(last != null){
                    list.add(LineD(last.fixed(), point.fixed()))
                }
                last = point
            }
        }else if(figure is Circle){
            val span = CirclePlotter(figure)
            var last: Vec2? = null
            for( i in 0 .. 500){
                val t = i / 500.0

                val point = span.getPoint(t)
                if(last != null){
                    list.add(LineD(last.fixed(), point.fixed()))
                }
                last = point
            }
        }else if(figure is FunctionFigure){
            val span = FunctionPlotter(figure)
            var last: Vec2? = null
            for( i in 0 .. 500){
                val t = i / 500.0

                val point = span.getPoint(t)
                if(last != null){
                    list.add(LineD(last.fixed(), point.fixed()))
                }
                last = point
            }
        }
    }
    return list
}
fun figureToPoints(figure: Figure) : List<PointD>{
    val list = mutableListOf<PointD>()

    if(figure is Segment2){
        list.add(figure.p0.fixed())
        list.add(figure.p1.fixed())
    }else if(figure is Arc){
        val span = ArcPlotter(figure)
        for( i in 0 .. 200){
            val t = i / 200.0

            val point = span.getPoint(t)
            list.add(point.fixed())
        }
    }else if(figure is Circle){
        val span = CirclePlotter(figure)
        var last: Vec2? = null
        for( i in 0 .. 500){
            val t = i / 500.0

            val point = span.getPoint(t)
            list.add(point.fixed())
        }
    }else if(figure is FunctionFigure){
        val span = FunctionPlotter(figure)
        var last: Vec2? = null
        for( i in 0 .. 500){
            val t = i / 500.0

            val point = span.getPoint(t)
            list.add(point.fixed())
        }
    }

    return list
}

/*
fun Canvas.line(line: com.codecad.core.Line){
    line(line.a.x.value,line.a.y.value, line.b.x.value,line.b.y.value)
}

fun Canvas.circle(circle: com.codecad.core.Circle){
    circle(circle.center.x.value,circle.center.y.value, circle.rad.value)
}*/



/*
fun Builder.getAutocadFile(filePath: String?): ArrayList<com.codecad.core.Line> {

    val lines = ArrayList<com.codecad.core.Line>()
    val parser = ParserBuilder.createDefaultParser()
    parser.parse(filePath, DXFParser.DEFAULT_ENCODING)
    val doc: DXFDocument = parser.document
    val layer0 = doc.getDXFLayer(DXFConstants.DEFAULT_LAYER)
    val lst = layer0.getDXFEntities(DXFConstants.ENTITY_TYPE_LINE)
    for (index in lst.indices) {
        val bounds = lst[index].bounds
        val line = line(
            com.codecad.core.Point(
                com.codecad.core.Parameter(bounds.minimumX),
                com.codecad.core.Parameter(bounds.minimumY)
            ),
            com.codecad.core.Point(
                com.codecad.core.Parameter( bounds.maximumX),
                com.codecad.core.Parameter( bounds.maximumY)
            )
        )
        lines.add(line)
    }

    val splines = layer0.getDXFEntities(DXFConstants.ENTITY_TYPE_SPLINE)



    val verticies = layer0.getDXFEntities(DXFConstants.ENTITY_TYPE_VERTEX)

    for(vertex in verticies){

    }


    for(spline in splines){
        spline as DXFSpline

        var last: com.codecad.core.Point? = null
        for(splinePoint in spline.splinePointIterator){
            val point = com.codecad.core.Point(
                com.codecad.core.Parameter(splinePoint.x),
                com.codecad.core.Parameter(splinePoint.y)
            )

            if(last != null){
                line(point, last)
            }

            last = point
        }
    }

    return lines
}*/
