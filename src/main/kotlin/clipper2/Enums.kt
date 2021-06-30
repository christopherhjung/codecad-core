package clipper2

enum class ClipType { None, Intersection, Union, Difference, Xor }
enum class PathType { Subject, Clip }
enum class FillRule { EvenOdd, NonZero, Positive, Negative }
object VertexFlags{
    val OpenStart: Int = 1
    val OpenEnd: Int = 2
    val LocMax: Int = 4
    val LocMin: Int = 8


}
