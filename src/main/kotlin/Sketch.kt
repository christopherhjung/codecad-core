import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class Sketch(val project: Project) {
    val params = HashSet<Parameter>()
    val constraints = HashSet<Constraint>()
    val figures = TreeSet<Figure>(){ a, b  ->
        val comp = a.type.prio.compareTo(b.type.prio)
        if(comp == 0) 1 else comp
    }

    class PruningEntry(val proxies: MutableList<ProxyValue> = mutableListOf(), var fixed: Value? = null)
    val pruningTable = mutableMapOf<ProxyValue, PruningEntry>()

    fun createParameter(value: Double = 0.0): ProxyValue {
        val param = Parameter(value )
        project.tracker.params.add(param)
        params.add(param)
        val proxy = ProxyValue(param)
        pruningTable[proxy] = PruningEntry(mutableListOf(proxy))
        return proxy
    }

    fun createConst(value: Double = 0.0): Const {
        return Value.const(value)
    }

    fun createConstPoint(x: Double = 0.0, y: Double = 0.0): Point {
        val a = createConst(x)
        val b = createConst(y)
        val point = Point(a, b)
        figures.add(point)
        return point
    }

    fun createPoint(x: Value , y: Value ): Point {
        val point = Point(x,y)
        figures.add(point)
        return point
    }

    fun createPoint(x: Double = 0.0, y: Double = 0.0): Point {
        val a = createParameter(x)
        val b = createParameter(y)
        return createPoint(a,b)
    }

    fun createLine(a: Point, b: Point, type: LineType = LineType.ToolPath): Line {
        val line = Line(a, b, type)
        figures.add(line)
        return line
    }

    fun createCircle(center: Point, radius: Value): Circle {
        val circle = Circle(center, radius)
        figures.add(circle)
        return circle
    }

    fun createArc(p0: Point, p1: Point, radius: Value): Arc {
        val circle = Arc(p0,p1,radius)
        figures.add(circle)
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

    fun addConstraintImpl(constraint: Constraint){
        constraints.add(constraint)
    }

    fun addConstraint(constraint: Constraint, tryBest: Boolean = false) : Constraint{
        println(constraint::class.simpleName)
        constraints.add(constraint)


        val constraintLookup = mutableMapOf<Constraint, HashSet<Parameter>>()
        val parameterLookup = mutableMapOf<Parameter, HashSet<Constraint>>()

        for( con in constraints ){
            for(param in params){
                val derivate = con.equation.derivative(param)
                if(derivate !is Const){
                    constraintLookup.computeIfAbsent(con){ HashSet() }.add(param)
                    parameterLookup.computeIfAbsent(param){ HashSet() }.add(con)
                }
            }
        }

        class Test(var level: Int, val parameter : Parameter) : Comparable<Test>{
            override fun compareTo(other: Test): Int {
                return level.compareTo(other.level)
            }
        }

        val priorityQueue = PriorityQueue<Test>()
        val visited = mutableSetOf<Parameter>()
        val stages = mutableListOf<MutableSet<Parameter>>()

        stages.add( mutableSetOf())

        constraintLookup[constraint]?.forEach {
            visited.add(it)
            priorityQueue.offer(Test(0, it))
            stages[0].add(it)
        }

        try{
            while(priorityQueue.isNotEmpty()){
                val test = priorityQueue.poll()

                parameterLookup[test.parameter]!!.forEach { con ->
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
                solveImpl(10e-4, stages)
            }else{
                solve(10e-4, stages)
            }
            constraint.prune(this)
        }catch (e: Exception){
            throw e
        }

        return constraint
    }

    fun collectReferences(value: Value, res: MutableSet<ProxyValue>){
        if(value is ProxyValue){
            res.add(value)
            collectReferences(value.ref, res)
        }else if(value !is Parameter){
            for(ref in value.proxyChildren){
                collectReferences(ref, res)
            }
        }
    }

    fun merge(left: Value, right: Value) {
        if (left is ProxyValue) {
            val leftEntry = pruningTable[left]!!
            if (right is ProxyValue) {
                val rightEntry = pruningTable[right]!!

                if(leftEntry === rightEntry){
                    return
                }

                leftEntry.proxies.addAll(rightEntry.proxies)

                if(leftEntry.fixed != null && rightEntry.fixed != null){
                    return
                }else if(rightEntry.fixed != null){
                    val set = mutableSetOf<ProxyValue>()
                    collectReferences(rightEntry.fixed!!, set)

                    if(leftEntry.proxies.any { set.contains(it) }){
                        return
                    }

                    leftEntry.fixed = rightEntry.fixed
                    for( proxy in leftEntry.proxies ){
                        params.remove(proxy.ref)
                        proxy.ref = rightEntry.fixed!!
                    }
                }

                pruningTable[right] = leftEntry
            } else {
                if(leftEntry.fixed != null){
                    return
                }

                val set = mutableSetOf<ProxyValue>()
                collectReferences(right, set)

                if(leftEntry.proxies.any { set.contains(it) }){
                    return
                }

                leftEntry.fixed = right
                for( proxy in leftEntry.proxies ){
                    params.remove(proxy.ref)
                    proxy.ref = right
                }
            }
        } else if (right is ProxyValue) {
            val rightEntry = pruningTable[right]!!

            if(rightEntry.fixed != null){
                return
            }

            val set = mutableSetOf<ProxyValue>()
            collectReferences(left, set)

            if(rightEntry.proxies.any { set.contains(it) }){
                return
            }

            rightEntry.fixed = left
            for( proxy in rightEntry.proxies ){
                params.remove(proxy.ref)
                proxy.ref = left
            }
        }
    }

    fun solveImpl(accuracy: Double, params: List<Set<Parameter>> = listOf(this.params)) : Boolean{
        val solver = Solver(project.tracker)
        val result = solver.solve(listOf(this.params), ArrayList(constraints),accuracy)
        return result
    }

    fun solve(accuracy: Double, params: List<Set<Parameter>> = listOf(this.params)) {
        val start = System.currentTimeMillis()

        val result = solveImpl(accuracy, params)

        if(!result){
            var error = 0.0
            val locations = mutableListOf<Location>()
            for(constraint in constraints){
                val constraintError = constraint.equation
                val value = constraintError.value
                error += value
                if(value > accuracy){
                    locations.add(Location(constraint.lineNumber, 0))
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


abstract class Pattern{
    abstract fun names() : List<String>
    abstract fun build(sketch: SketchScope)
}

class Rect : Pattern() {
    lateinit var a: Point
    lateinit var b: Point
    lateinit var c: Point
    lateinit var d: Point

    lateinit var center: Point

    lateinit var top: Line
    lateinit var right: Line
    lateinit var bottom: Line
    lateinit var left: Line

    lateinit var width: Value
    lateinit var height: Value

    override fun names(): List<String> {
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
* fun Canvas.line(line: Line){
    line(line.a.x.value,line.a.y.value, line.b.x.value,line.b.y.value)
}
fun Canvas.circle(circle: Circle){
    circle(circle.center.x.value,circle.center.y.value, circle.rad.value)
}
* */
