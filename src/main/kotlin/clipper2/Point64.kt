package clipper2

class Point64(var X: Long = 0L, var Y: Long = 0L)
{
    constructor(X: Double, Y:Double) : this(X.toLong(), Y.toLong())
    constructor(pt: Point64)  : this(pt.X, pt.Y)

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other is Point64) {
            return (X == other.X) && (Y == other.Y)
        } else return false
    }

    override fun hashCode(): Int {
        return X.hashCode() xor Y.hashCode()
    }
}
