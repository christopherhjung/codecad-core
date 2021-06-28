




fun main(args: Array<String>) {
    val constraints = ArrayList<Constraint>()

    /*val v1 = Value(0.0)
    val v2 = Value(0.0)
    val v3 = Value(5.0)
    val v4 = Value(-1.0)
    val v5 = Value(10.0)
    val a = Point(v1,v2)
    val b = Point(v3,v4)

    val line = Line(a,b)

    constraints.add(LineLength(line, v5))

    println(Solver().solve(listOf(v4),constraints,false))

    println(v3.value)
    println(v4.value)*/

    val v1 = Value(0.0)
    val v2 = Value(0.0)
    val v3 = Value(0.0)
    val v4 = Value(0.0)
    val v5 = Value(0.0)
    val v6 = Value(0.0)
    val v7 = Value(6.0)
    val v8 = Value(6.0)

    val p1 = Point(v1,v2)
    val p2 = Point(v3,v4)
    val p3 = Point(v5,v6)
    val p4 = Point(v7,v8)

    val lineA = Line(p1,p2)
    val lineB = Line(p3,p4)

    constraints.add(Horizontal(lineA))
    constraints.add(Vertical(lineB))
    constraints.add(LineLength(lineB, Value(3.2)))
    val testValue = Value(0.0)
    constraints.add(LineLength(lineB, testValue))
    constraints.add(PointOnPoint(p2,p3))

    println(Solver().solve(listOf(v1,v2,v3,v4,v5,v6, testValue),constraints,false))

    println(v1.value)
    println(v2.value)
    println(v3.value)
    println(v4.value)
    println(v5.value)
    println(v6.value)
    println(v7.value)
    println(v8.value)
    println(testValue.value)

}

