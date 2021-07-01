package clipper2

class Vertex {
    var Pt = Point64()
    var Next : Vertex? = null
    var Prev : Vertex? = null
    var Flags: Int = 0

    constructor(ip: Point64)
    {
        Pt.x = ip.x
        Pt.y = ip.y
    }
}
