




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

    val v3 = Value(0.1)
    val v4 = Value(0.0)

    val v5 = Value(3.0)
    val v6 = Value(3.0)
    val v7 = Value(1.0)

    val p1 = Point(v1,v2)
    val p2 = Point(v3,v4)
    val p3 = Point(v5,v6)

    val line = Line(p1,p2)
    val circle = Circle(p3,v7)

    constraints.add(CircleTangent(circle, line))
    constraints.add(PointOnCircle(p2, circle))

    val start = System.currentTimeMillis()
    println(Solver().solve(listOf(v3,v4),constraints,false))
    val end = System.currentTimeMillis()

    println("time need: ${end-start}")

    println("v1= " + v1.value)
    println("v2= " + v2.value)
    println("v3= " + v3.value)
    println("v4= " + v4.value)
    println("v5= " + v5.value)
    println(v6.value)
    println(v7.value)

}

