class PatternScope(project: Project, val count: Int, val center: Point) : SketchScope(project) {

    val allCrawler = mutableMapOf<Point, MutableList<MutableList<Point>>>()
    val pointLookup = mutableMapOf<Point, Array<Point?>>()

    fun all(ref: Point, list: MutableList<Point>)  {
        allCrawler.computeIfAbsent(ref){ mutableListOf()}.add(list)
    }

    var current = 0

    private fun rotatePoint(point: Point, angle: Value) : Point{
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
                    }else if(element is Line){
                        Line(
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
        val AXIS_X = Line(ORIGIN, Point(Value.const(1.0), Value.const(0.0)))
        val AXIS_Y = Line(ORIGIN, Point(Value.const(0.0), Value.const(1.0)))
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

    fun arc(p0: Point, p1: Point, radius: Value): Arc {
        return sketch.createArc(p0,p1,radius)
    }

    fun func(block: (Value) -> Point): FunctionFigure{
        return sketch.createFunction(block)
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
class ProjectScope(val project: Project)

open class Volume{

}

class Extrude(val face: Face, val height: Value) : Volume(){

}

class Project(val sketches: MutableList<Sketch> = mutableListOf(), val tracker: Tracker = Tracker()){
    val volumes = mutableListOf<Volume>()
}

fun project(block: ProjectScope.() -> Unit) : Project{
    val project = Project()
    val projectScope = ProjectScope(project)
    try{
        block(projectScope)
    }catch (e: StackOverflowError){
        e.printStackTrace()
    }
    return project
}

fun ProjectScope.sketch(init: SketchScope.() -> Unit): Sketch {
    val builder = SketchScope(project)
    builder.init()
    project.sketches.add(builder.sketch)
    builder.sketch.solve(1e-8)
    return builder.sketch
}

fun ProjectScope.extrude(sketch: Sketch, pointer: Point, height: Value) {
    val face = findFace(sketchToLines(sketch), pointer.fixed())

    if(face != null){
        project.volumes.add(Extrude(face, height))
    }
}

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
}

class PointD(val x: Double, val y: Double){
    override fun toString(): String {
        return "PointD(x=$x, y=$y)"
    }

    operator fun minus(other: PointD): PointD{
        return PointD(this.x - other.x, this.y - other.y)
    }

    operator fun plus(other: PointD): PointD{
        return PointD(this.x + other.x, this.y + other.y)
    }

    fun cross(other: PointD): Double{
        return this.x * other.y - this.y * other.x
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PointD) return false

        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }
}
data class LineD(val p0: PointD, val p1: PointD){

}

fun sketchToLines(sketch: Sketch) : List<LineD>{
    val list = mutableListOf<LineD>()
    for(figure in sketch.figures){
        if(figure is Line){
            list.add(LineD(figure.p0.fixed(), figure.p1.fixed()))
        }else if(figure is Arc){
            val span = ArcSpan(figure)
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

/*
fun Canvas.line(line: Line){
    line(line.a.x.value,line.a.y.value, line.b.x.value,line.b.y.value)
}

fun Canvas.circle(circle: Circle){
    circle(circle.center.x.value,circle.center.y.value, circle.rad.value)
}*/




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
