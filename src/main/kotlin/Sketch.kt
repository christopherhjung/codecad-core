import java.lang.RuntimeException

class Sketch {
    val params = HashSet<Value>()
    val constraints = HashSet<Constraint>()
    val elements = HashSet<Element>()

    fun createParameter(value: Double = 0.0) : ProxyValue{
        val param = Parameter(value)
        params.add(param)
        return ProxyValue(param)
    }

    fun createConst(value: Double = 0.0) : Parameter{
        return Parameter(value)
    }

    fun createConstPoint(x: Double = 0.0, y: Double = 0.0) : Point{
        val a = createConst(x)
        val b = createConst(y)
        val point = Point(a,b)
        elements.add(point)
        return point
    }

    fun createPoint(x: Double = 0.0, y: Double = 0.0) : Point{
        val a = createParameter(x)
        val b = createParameter(y)
        val point = Point(a,b)
        elements.add(point)
        return point
    }

    fun createLine(a: Point, b:Point) : Line{
        val line = Line(a,b)
        elements.add(line)
        return line
    }

    fun createCircle(center: Point, radius: Value) : Circle{
        val circle = Circle(center, radius)
        elements.add(circle)
        return circle
    }

    fun createCircle() : Circle{
        return createCircle(createPoint(), createParameter(1.0))
    }

    fun createLine(x: Double = 0.0, y: Double = 0.0, x2: Double = 0.0, y2: Double = 0.0) : Line{
        return createLine(createPoint(x,y),createPoint(x2,y2))
    }

    fun createConstLine(x: Double = 0.0, y: Double = 0.0, x2: Double = 0.0, y2: Double = 0.0) : Line{
        return createLine(createConstPoint(x,y),createConstPoint(x2,y2))
    }

    fun addConstraint(constraint: Constraint){
        constraints.add(constraint)
        constraint.prune(this)
    }

    fun paramIsEquals(left: Value, right: Value){
        if(left is ProxyValue ){
            if(right is ProxyValue){
                if(left.proxy != right.proxy){
                    params.remove(left.proxy)
                    left.proxy = right.proxy
                }
            }else{
                left.proxy = right
            }
        }else if(right is ProxyValue){
            params.remove(right.proxy)
            right.proxy = left
        }else{
            throw RuntimeException("const cant be set equals")
        }
    }

    fun solve(){
        val solver = Solver()
        println(solver.solve(ArrayList(params),ArrayList(constraints)))
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for(element in elements){
            sb.append(element).append("\n")
        }
        return sb.toString()
    }
}
