import java.util.*
import kotlin.collections.HashMap

class Event(
    val p: PointD,
    val line: LineD,
    val isLeft: Boolean
) : Comparable<Event> {

    override fun compareTo(other: Event): Int {
        if (p.x == other.p.x) return p.y.compareTo(other.p.y)
        return p.x.compareTo(other.p.x)
    }
}

fun findIntersection(line1: LineD, line2: LineD): PointD? {
    val p0_x = line1.p0.x
    val p0_y = line1.p0.y
    val p1_x = line1.p1.x
    val p1_y = line1.p1.y
    val p2_x = line2.p0.x
    val p2_y = line2.p0.y
    val p3_x = line2.p1.x
    val p3_y = line2.p1.y

    val s1_x = p1_x - p0_x
    val s1_y = p1_y - p0_y
    val s2_x = p3_x - p2_x
    val s2_y = p3_y - p2_y

    val s = (-s1_y * (p0_x - p2_x) + s1_x * (p0_y - p2_y)) / (-s2_x * s1_y + s1_x * s2_y)
    val t = (s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) / (-s2_x * s1_y + s1_x * s2_y)

    val epsilon = 1e-5
    if (s - epsilon > 0 && s + epsilon < 1 && t - epsilon > 0 && t + epsilon < 1) {
        val x = p0_x + (t * s1_x)
        val y = p0_y + (t * s1_y)
        return PointD(x, y)
    }

    return null
}


// Returns true if any two lines intersect.
fun isIntersect(arr: List<LineD>): Int {
    val events = LinkedList<Event>()

    val ordered = mutableListOf<LineD>()
    for(line in arr){
        if (line.p0.x > line.p1.x) {
            ordered.add(LineD(line.p1, line.p0))
        }else{
            ordered.add(line)
        }
    }

    for (line in ordered) {
        events.add(Event(line.p0, line, true))
        events.add(Event(line.p1, line, false))
    }

    events.sort()

    var intersections = 0
    val active = HashMap<LineD, Event>()
    val splittingPoints = HashMap<LineD, MutableList<PointD>>()
    for (event in events) {
        if (event.isLeft) {
            for (other in active.values) {
                val intersection = findIntersection(other.line, event.line)
                if (intersection != null) {
                    splittingPoints.computeIfAbsent(event.line){ mutableListOf()}.add(intersection)
                    splittingPoints.computeIfAbsent(other.line){ mutableListOf()}.add(intersection)
                }
            }

            active[event.line] = event
        } else {
            active.remove(event.line)
        }
    }

    val result = mutableListOf<LineD>()

    for( line in ordered ){
        val splits = splittingPoints[line]
        if( splits != null ){
            var left = line.p0
            for( split in splits ){
                result.add(LineD(left, split))
                left = split
            }
            result.add(LineD(left, line.p1))
        }else{
            result.add(line)
        }
    }

    return intersections
}
