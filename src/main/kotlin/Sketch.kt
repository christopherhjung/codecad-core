import java.lang.Math.random
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
        return Const(value)
    }

    fun createConstPoint(x: Double = 0.0, y: Double = 0.0): Point {
        val a = createConst(x)
        val b = createConst(y)
        val point = Point(a, b)
        figures.add(point)
        return point
    }

    fun createPoint(x: Double = 0.0, y: Double = 0.0): Point {
        val a = createParameter(x)
        val b = createParameter(y)
        val point = Point(a, b)
        figures.add(point)
        return point
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

    fun createArc(center: Point, radius: Value, start: Value, end: Value): Arc {
        val circle = Arc(center, radius, start, end)
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

    fun addConstraint(constraint: Constraint) : Constraint{
        println(constraint::class.simpleName)
        constraints.add(constraint)



        val constraintLookup = mutableMapOf<Constraint, HashSet<Parameter>>()
        val parameterLookup = mutableMapOf<Parameter, HashSet<Constraint>>()

        for( con in constraints ){
            for(param in params){
                val derivate = con.equation.derivative(param)
                if(!derivate.isConst()){
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

        constraintLookup[constraint]!!.forEach {
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

                /*
                val constraint = left[i]

                val found = lookup[constraint]?.any { test.contains(it) } ?: false

                if(found){
                    test.addAll(lookup[constraint]!!)
                    left.removeAt(i)
                    i=0
                }else{
                    i++
                }*/
            }
        }catch (e: Exception){
            throw e
        }

        //println("${params.size} - ${test.size}")

        try{
            solve(10e-6, stages)
            //constraint.prune(this)
        }catch (e: Exception){
            throw e
        }
        return constraint
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

            rightEntry.fixed = left
            for( proxy in rightEntry.proxies ){
                params.remove(proxy.ref)
                proxy.ref = left
            }
        }
    }

    fun solve(accuracy: Double, params: List<Set<Parameter>> = listOf(this.params)) {
        val start = System.currentTimeMillis()

        val solver = Solver(project.tracker)
        val result = solver.solve(params, ArrayList(constraints),accuracy)

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

/*
* fun Canvas.line(line: Line){
    line(line.a.x.value,line.a.y.value, line.b.x.value,line.b.y.value)
}
fun Canvas.circle(circle: Circle){
    circle(circle.center.x.value,circle.center.y.value, circle.rad.value)
}
* */
