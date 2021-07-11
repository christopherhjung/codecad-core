import kotlin.math.cos
import kotlin.math.sin

class PatternScope : SketchScope() {

    val allCrawler = mutableMapOf<Point, MutableList<MutableList<Point>>>()
    val pointLookup = mutableMapOf<Point, Array<Point?>>()

    fun all(ref: Point, list: MutableList<Point>)  {
        allCrawler.computeIfAbsent(ref){ mutableListOf()}.add(list)
    }

    var current = 0
    var repeats = 0

    private fun rotatePoint(point: Point, angle: Value) : Point{
        val array = pointLookup.computeIfAbsent(point){Array(repeats){null} }

        if(array[current] == null){
            val a = Value.sin(angle)
            val b = Value.cos(angle)
            array[current] = Point(b * point.x - a * point.y, a * point.x + b * point.y)
        }

        return array[current]!!
    }

    fun finish(count: Int, point: Point){
        sketch.solve(1e-8)
        val rawElements = sketch.elements.toList()

        repeats = count - 1

        for(i in 0 until repeats){
            val angle = const((2 * Math.PI / count) * (i + 1))
            current = i

            for(element in rawElements){
                if(element is Point){
                    sketch.elements.add(rotatePoint(element, angle))
                }else if(element is Line){
                    sketch.elements.add(Line(
                        rotatePoint(element.a, angle),
                        rotatePoint(element.b, angle)
                    ))
                }else if(element is Circle){
                    val start = if(element.start == null){
                        null
                    }else{
                        element.start + angle
                    }

                    val end = if(element.end == null){
                        null
                    }else{
                        element.end + angle
                    }

                    sketch.elements.add(Circle(
                        rotatePoint(element.center, angle),
                        element.rad,
                        start,
                        end
                    ))
                }
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

open class SketchScope {
    val sketch = Sketch()

    companion object{
        val ORIGIN = Point(Const(0.0), Const(0.0))
        val AXIS_X = Line(ORIGIN, Point(Const(1.0), Const(0.0)))
        val AXIS_Y = Line(ORIGIN, Point(Const(0.0), Const(1.0)))
    }

    val deg = 0
    val rad = 1

    val mm = 2
    val cm = 3

    infix fun Double.unit(other: Int): Double {
        return if (other == deg) {
            Math.toRadians(this)
        } else {
            this
        }
    }

    fun pattern(repeat: Int, point: Point, init: PatternScope.(Point) -> Unit): Sketch {
        val builder = PatternScope()
        builder.init(point)
        builder.finish(repeat, point)

        sketch.elements.addAll(builder.sketch.elements)

        //builder.sketch.draw()
        return builder.sketch
    }

    fun param(value: Double = 0.0): ProxyValue {
        return sketch.createParameter(value)
    }

    fun <T> list() : MutableList<T>{
        return mutableListOf()
    }

    fun const(value: Double = 0.0): Const {
        return sketch.createConst(value)
    }

    fun constPoint(x: Double = 0.0, y: Double = 0.0): Point {
        return sketch.createConstPoint(x, y)
    }

    fun point(x: Double = 0.0, y: Double = 0.0): Point {
        return sketch.createPoint(x, y)
    }

    fun line(a: Point, b: Point, type: LineType = LineType.Normal): Line {
        return sketch.createLine(a, b, type)
    }

    fun circle(center: Point, radius: Value): Circle {
        return sketch.createCircle(center, radius)
    }

    fun arc(center: Point, radius: Value, start: Value, end: Value): Circle {
        return sketch.createArc(center, radius, start, end)
    }

    fun polygon(vararg points: Point) : List<Line> {
        val points = mutableListOf(*points)
        points.add(points.first())
        var current = points.first()
        val lines = mutableListOf<Line>()
        for (point in points) {
            lines.add(line(current, point))
            current = point
        }

        return lines
    }

    fun line(x: Double = 0.0, y: Double = 0.0, x2: Double = 0.0, y2: Double = 0.0): Line {
        return sketch.createLine(x, y, x2, y2)
    }

    fun tangent(circle: Circle, line: Line) {
        addConstraintImpl(CircleTangent(circle, line))
    }

    fun pointOnLineMidpoint(point: Point, line: Line) {
        addConstraintImpl(PointOnLineMidpoint(point, line))
    }

    fun pointOnLine(point: Point, line: Line) {
        addConstraintImpl(PointOnLine(point, line))
    }

    fun horizontal(line: Line) {
        addConstraintImpl(Horizontal(line))
    }

    fun vertical(line: Line) {
        addConstraintImpl(Vertical(line))
    }

    fun pointOnCircle(point: Point, circle: Circle) {
        addConstraintImpl(PointOnCircle(point, circle))
    }

    fun equalLength(line1: Line, line2: Line) {
        addConstraintImpl(EqualLength(line1, line2))
    }

    fun length(line1: Line, length: Value) {
        addConstraintImpl(LineLength(line1, length))
    }

    fun angle(line1: Line, line2: Line, angle: Value) {
        addConstraintImpl(InternalAngle(line1, line2, angle))
    }

    fun pointOnArcStart(point: Point, arc: Circle) {
        addConstraintImpl(PointOnArcStart(point, arc))
    }

    fun addConstraint(constraint: Constraint) {
        addConstraintImpl(constraint)
    }

    fun perpendicular(line1: Line, line2: Line){
        addConstraintImpl(Perpendicular(line1, line2))
    }

    fun parallel(line1: Line, line2: Line){
        addConstraintImpl(Parallel(line1, line2))
    }

    fun pointOnPoint(point1: Point, point2: Point) {
        addConstraintImpl(PointOnPoint(point1, point2))
    }

    fun equals(value1 : Value, value2: Value) {
        addConstraintImpl(Equals(value1, value2))
    }

    fun radius(circle: Circle, value: Value) {
        addConstraintImpl(Radius(circle, value))
    }

    fun pointOnArcEnd(point: Point, arc: Circle) {
        addConstraintImpl(PointOnArcEnd(point, arc))
    }

    private fun addConstraintImpl(constraint: Constraint){
        val callersLineNumber = Thread.currentThread().stackTrace[3].lineNumber
        sketch.addConstraint(constraint).lineNumber = callersLineNumber
    }

    fun solve() {
        //sketch.solve()
    }
}
class ProjectScope(val sketches: MutableList<Sketch> = mutableListOf())

class Project(val sketches: MutableList<Sketch> = mutableListOf())

fun project(block: ProjectScope.() -> Unit) : Project{
    val projectScope = ProjectScope()
    block(projectScope)
    return Project(projectScope.sketches)
}

fun ProjectScope.sketch(init: SketchScope.() -> Unit): Sketch {
    val builder = SketchScope()
    builder.init()
    sketches.add(builder.sketch)
    builder.sketch.solve(1e-8)

    //builder.sketch.draw()
    return builder.sketch
}


class LineSegment(val a: Vector, val b: Vector)

fun sketchToLines(sketch: Sketch) : List<LineSegment>{
    val list = mutableListOf<LineSegment>()
    for(element in sketch.elements){
        if(element is Line){
            list.add(LineSegment(element.a.toVector(), element.b.toVector()))
        }else if(element is Circle){
            val span = ArcSpan(element)
            var last: Vector? = null
            for( i in 0 .. 100){
                val t = i / 100.0

                val point = span.getPoint(t)
                if(last != null){
                    list.add(LineSegment(last, point))
                }
                last = point
            }
        }
    }
    return list
}

/*
fun Canvas.line(line: Line){
    line(line.a.x.value,line.a.y.value, line.b.x.value,line.b.y.value)
}

fun Canvas.circle(circle: Circle){
    circle(circle.center.x.value,circle.center.y.value, circle.rad.value)
}*/

interface Stepper {
    fun next(): Vector
    fun hasNext(): Boolean
}


fun elementsToPath(elements: List<Element>) : Array<DoubleArray>{
    val path = mutableListOf<Double>()

    for (element in elements) {
        val stepper: Stepper = if (element is Line) {
            LineStepper(element)
        } else if (element is Circle) {
            ArcStepper(element)
        } else {
            throw RuntimeException()
        }

        while (stepper.hasNext()) {
            val next = stepper.next()
            path.add(next.x)
            path.add(next.y)
        }
    }


    return arrayOf(path.toTypedArray().toDoubleArray())
}

fun SketchScope.pathsToPoly(input : Array<DoubleArray>, lineType: LineType){
    for (path in input) {
        var last: Point? = null
        var first: Point? = null
        for (i in path.indices step 2) {
            val x = path[i]
            val y = path[i + 1]

            val thePoint = Point(const(x), const(y))

            if (last != null) {
                line(last, thePoint,  lineType)
            } else {
                first = thePoint
            }

            last = thePoint
        }


        if (last != null && first != null) {
            line(last, first,  lineType)
        }
    }
}

/*
fun Builder.getAutocadFile(filePath: String?): ArrayList<Line> {

    val lines = ArrayList<Line>()
    val parser = ParserBuilder.createDefaultParser()
    parser.parse(filePath, DXFParser.DEFAULT_ENCODING)
    val doc: DXFDocument = parser.document
    val layer0 = doc.getDXFLayer(DXFConstants.DEFAULT_LAYER)
    val lst = layer0.getDXFEntities(DXFConstants.ENTITY_TYPE_LINE)
    for (index in lst.indices) {
        val bounds = lst[index].bounds
        val line = line(
            Point(
                Parameter(bounds.minimumX),
                Parameter(bounds.minimumY)
            ),
            Point(
                Parameter( bounds.maximumX),
                Parameter( bounds.maximumY)
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

        var last: Point? = null
        for(splinePoint in spline.splinePointIterator){
            val point = Point(
                Parameter(splinePoint.x),
                Parameter(splinePoint.y)
            )

            if(last != null){
                line(point, last)
            }

            last = point
        }
    }

    return lines
}*/
