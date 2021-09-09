package com.codecad.core

import com.codecad.common.LineD
import com.codecad.common.PointD
import de.lighti.clipper.Clipper
import de.lighti.clipper.ClipperOffset
import de.lighti.clipper.Path
import de.lighti.clipper.Paths

class PatternScope(project: Project, val count: Int, val center: Point) : SketchScope(project) {

    val allCrawler = mutableMapOf<Point, MutableList<MutableList<Point>>>()
    val pointLookup = mutableMapOf<Point, Array<Point?>>()

    fun all(ref: Point, list: MutableList<Point>)  {
        allCrawler.computeIfAbsent(ref){ mutableListOf()}.add(list)
    }

    var current = 0

    private fun rotatePoint(point: Point, angle: Value) : Point {
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
            val angle = const((2 * Math.PI / count) * (i + 1))
            current = i

            for(element in rawElements){
                sketch.figures.add(
                    if(element is Point){
                        rotatePoint(element, angle)
                    }else if(element is LineSegment){
                        LineSegment(
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

    companion object{
        val ORIGIN = Point(Value.const(0.0), Value.const(0.0))
        val AXIS_X = LineSegment(ORIGIN, Point(Value.const(1.0), Value.const(0.0)))
        val AXIS_Y = LineSegment(ORIGIN, Point(Value.const(0.0), Value.const(1.0)))
    }

    val deg = 0
    val rad = 1

    val mm = 2
    val cm = 3

    fun Value.isEquals(other : Value) {
        equals(this, other)
    }

    infix fun Double.unit(other: Int): Double {
        return if (other == deg) {
            Math.toRadians(this)
        } else {
            this
        }
    }

    fun pattern(repeat: Int, point: Point, init: PatternScope.(Point) -> Unit): Sketch {
        val builder = PatternScope(project, repeat, point)
        builder.init(point)
        builder.finish()
        sketch.figures.addAll(builder.sketch.figures)
        return builder.sketch
    }

    fun offset(pointer: Point, offsetValue: Number, block: SketchScope.() -> Unit){
        val sketchScope = SketchScope(project)
        sketchScope.block()

        val face = findFace(sketchToLines(sketchScope.sketch), pointer.fixed())
        val paths = mutableListOf<DoubleArray>()

        val factor = 1000000

        val offset = ClipperOffset()
        val path = Path()

        if(face != null){
            val arr = DoubleArray(face.positions.size * 2)
            paths.add(arr)
            var i = 0
            for( point in face.positions.map { it.point }){
                arr[i++] = point.x
                arr[i++] = point.y

                path.add(de.lighti.clipper.Point.LongPoint((point.x * factor).toLong(), (point.y * factor).toLong()))
            }
        }

        offset.addPath(path, Clipper.JoinType.ROUND, Clipper.EndType.CLOSED_POLYGON );

        val result = Paths()
        offset.execute(result, offsetValue.toDouble() * factor)

        val points = mutableListOf<Point>()
        for( resultPath in result ){
            for( i in resultPath.indices step 2 ){
                points.add(Point(Const(resultPath[i].x.toDouble() / factor), Const(resultPath[i].y.toDouble() / factor)))
            }
        }

        if(points.size != 0){
            polygon(*points.toTypedArray())
        }
    }

    fun param(value: Number = 0.0): ProxyValue {
        return sketch.createParameter(value.toDouble())
    }

    fun <T> list() : MutableList<T>{
        return mutableListOf()
    }

    fun const(value: Number = 0.0): Const {
        return sketch.createConst(value.toDouble())
    }

    fun constPoint(x: Number = 0.0, y: Number = 0.0): Point {
        return sketch.createConstPoint(x.toDouble(), y.toDouble())
    }

    fun point(x: Number = 0.0, y: Number = 0.0): Point {
        return sketch.createPoint(x.toDouble(), y.toDouble())
    }

    fun line(a: Point, b: Point): LineSegment {
        return sketch.createLine(a, b, LineType.Normal)
    }

    fun cline(a: Point, b: Point): LineSegment {
        return sketch.createLine(a, b, LineType.Construction)
    }

    fun circle(center: Point, radius: Value): Circle {
        return sketch.createCircle(center, radius)
    }

    fun arc(p0: Point, p1: Point, radius: Value): Arc {
        return sketch.createArc(p0,p1,radius)
    }

    fun func(block: (Value) -> Point): FunctionFigure {
        return sketch.createFunction(block)
    }

    fun polygon(vararg points: Point) : List<LineSegment> {
        val points = mutableListOf(*points)
        points.add(points.first())
        var current = points.first()
        val lines = mutableListOf<LineSegment>()
        for (point in points) {
            lines.add(line(current, point))
            current = point
        }

        return lines
    }

    fun line(x: Number = 0.0, y: Number = 0.0, x2: Number = 0.0, y2: Number = 0.0): LineSegment {
        return sketch.createLine(x.toDouble(), y.toDouble(), x2.toDouble(), y2.toDouble())
    }

    fun tangent(circle: Circle, line: LineSegment) {
        addConstraintImpl(CircleTangent(circle, line))
    }

    fun pointOnLineMidpoint(point: Point, line: LineSegment) {
        addConstraintImpl(PointOnLineMidpoint(point, line))
    }

    fun pointOnLine(point: Point, line: LineSegment) {
        addConstraintImpl(PointOnLine(point, line))
    }

    fun horizontal(line: LineSegment) {
        addConstraintImpl(Horizontal(line))
    }

    fun vertical(line: LineSegment) {
        addConstraintImpl(Vertical(line))
    }

    fun pointOnCircle(point: Point, circle: Circle) {
        addConstraintImpl(PointOnCircle(point, circle))
    }

    fun equalLength(line1: LineSegment, line2: LineSegment) {
        addConstraintImpl(EqualLength(line1, line2))
    }

    fun length(line1: LineSegment, length: Value) {
        addConstraintImpl(LineLength(line1, length))
    }

    fun angle(line1: LineSegment, line2: LineSegment, angle: Value) {
        addConstraintImpl(InternalAngle(line1, line2, angle))
    }

    fun addConstraint(constraint: Constraint) {
        addConstraintImpl(constraint)
    }

    fun perpendicular(line1: LineSegment, line2: LineSegment){
        addConstraintImpl(Perpendicular(line1, line2))
    }

    fun parallel(line1: LineSegment, line2: LineSegment){
        addConstraintImpl(Parallel(line1, line2))
    }

    fun pointOnPoint(point1: Point, point2: Point) {
        addConstraintImpl(PointOnPoint(point1, point2))
    }

    fun equals(point1: Point, point2: Point) {
        addConstraintImpl(PointOnPoint(point1, point2))
    }

    fun equals(value1 : Value, value2: Value) {
        addConstraintImpl(Equals(value1, value2))
    }

    fun radius(circle: Circle, value: Value) {
        addConstraintImpl(Radius(circle, value))
    }

    private fun addConstraintImpl(constraint: Constraint){
        val callersLineNumber = Thread.currentThread().stackTrace[3].lineNumber
        sketch.addConstraint(constraint).lineNumber = callersLineNumber
    }

    fun add(pattern: Pattern){
        pattern.build(this)
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
        if(ignoreConstruction && figure.type == LineType.Construction){
            continue
        }

        if(figure is LineSegment){
            list.add(LineD(figure.p0.fixed(), figure.p1.fixed()))
        }else if(figure is Arc){
            val span = ArcSpan(figure)
            var last: Point? = null
            for( i in 0 .. 200){
                val t = i / 200.0

                val point = span.getPoint(t)
                if(last != null){
                    list.add(LineD(last.fixed(), point.fixed()))
                }
                last = point
            }
        }else if(figure is Circle){
            val span = CircleSpan(figure)
            var last: Point? = null
            for( i in 0 .. 500){
                val t = i / 500.0

                val point = span.getPoint(t)
                if(last != null){
                    list.add(LineD(last.fixed(), point.fixed()))
                }
                last = point
            }
        }else if(figure is FunctionFigure){
            val span = FunctionSpan(figure)
            var last: Point? = null
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

    if(figure is LineSegment){
        list.add(figure.p0.fixed())
        list.add(figure.p1.fixed())
    }else if(figure is Arc){
        val span = ArcSpan(figure)
        for( i in 0 .. 200){
            val t = i / 200.0

            val point = span.getPoint(t)
            list.add(point.fixed())
        }
    }else if(figure is Circle){
        val span = CircleSpan(figure)
        var last: Point? = null
        for( i in 0 .. 500){
            val t = i / 500.0

            val point = span.getPoint(t)
            list.add(point.fixed())
        }
    }else if(figure is FunctionFigure){
        val span = FunctionSpan(figure)
        var last: Point? = null
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
