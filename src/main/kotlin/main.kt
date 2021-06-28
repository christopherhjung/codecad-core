




fun main(args: Array<String>) {
    val constraints = ArrayList<Constraint>()

    val v1 = Value(0.0)
    val v2 = Value(0.0)
    val v3 = Value(5.0)
    val v4 = Value(1.0)
    val v5 = Value(10.0)
    val a = Point(v1,v2)
    val b = Point(v3,v4)

    val line = Line(a,b)

    constraints.add(LineLength(line, v5))

    println(Solver().solve(listOf(v4),constraints,false))

    println(v3.value)
    println(v4.value)
}

