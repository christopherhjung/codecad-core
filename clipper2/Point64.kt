package clipper2

class Point64(var x: Long = 0L, var y: Long = 0L)
{
    constructor(X: Double, Y:Double) : this(X.toLong(), Y.toLong())
    constructor(pt: Point64)  : this(pt.x, pt.y)

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other is Point64) {
            return (x == other.x) && (y == other.y)
        } else return false
    }

    override fun hashCode(): Int {
        return x.hashCode() xor y.hashCode()
    }
}
