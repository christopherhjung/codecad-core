class Sketch {
    val params = HashSet<Value>()
    val constraints = HashSet<Constraint>()

    val shared = HashMap<Value, SharedValue>()

    fun createParameter(value: Double = 0.0) : Parameter{
        val param = Parameter(value)
        params.add(param)
        return param
    }

    fun createConst(value: Double = 0.0) : Parameter{
        val param = Parameter(value)
        return param
    }

    fun createPoint(x: Double = 0.0, y: Double = 0.0) : Point{
        val a = createParameter(x)
        val b = createParameter(y)
        val point = Point(a,b)
        return point
    }

    fun createLine(x: Double = 0.0, y: Double = 0.0, x2: Double = 0.0, y2: Double = 0.0) : Line{
        val a = createPoint(x,y)
        val b = createPoint(x2,y2)
        return Line(a,b)
    }

    fun addConstraint(constraint: Constraint){
        constraints.add(constraint)
        constraint.pruning(this)
    }

    fun paramIsEquals(left: Value, right: Value){
        if(true)
            return

        val leftValues = if(left is SharedValue) left else shared[left]
        val rightValues = if(right is SharedValue) right else shared[right]

        if(leftValues != null){
            if(rightValues != null){
                leftValues.values.addAll(rightValues.values)
                params.remove(rightValues)
            }else{
                leftValues.values.add(right)
                params.remove(right)
                shared[right] = leftValues
            }
        }else if(rightValues != null){
            rightValues.values.add(left)
            shared[left] = rightValues
            params.remove(left)
        }else{
            val newShared = SharedValue()
            params.add(newShared)
            params.remove(right)
            params.remove(left)
            shared[left] = newShared
            shared[right] = newShared
        }
    }

    fun solve(){
        val solver = Solver()
        println(solver.solve(ArrayList(params),ArrayList(constraints)))
    }
}
