import java.util.*
import kotlin.math.abs

// An event for sweep line algorithm
// An event has a point, the position
// of point (whether left or right) and
// index of point in the original input
// array of segments.
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

/*
fun doIntersect(s1: LineD,  s2: LineD) : PointD?
{
    val x1 = s1.p0.x
    val x2 = s1.p1.x
    val x3 = s2.p0.x
    val x4 = s2.p1.x

    val y1 = s1.p0.y
    val y2 = s1.p1.y
    val y3 = s2.p0.y
    val y4 = s2.p1.y

    val x12 = s1.p0.x - s1.p1.x
    val x34 = s2.p0.x - s2.p1.x
    val y12 = s1.p0.y - s1.p1.y
    val y34 = s2.p0.y - s2.p1.y

    val c = x12 * y34 - y12 * x34

    /*if(abs(c) < 0.0001)
        return false*/

    val a = x1 * y2 - y1 * x2
    val b = x3 * y4 - y3 * x4
    val x = (a * x34 - b * x12) / c
    val y = (a * y34 - b * y12) / c

    if(s1.p0.x < x && x <= s1.p1.x
            && s2.p0.x < x && x <= s2.p1.x &&
            s1.p0.y < y && y <= s1.p1.y
            && s2.p0.y < y && y <= s2.p1.y)
                return PointD(x, y)

    return null
}*/

fun onSegment(p: PointD, q: PointD, r: PointD): Boolean {
    return q.x <= p.x.coerceAtLeast(r.x) && q.x >= p.x.coerceAtMost(r.x) && q.y <= p.y.coerceAtLeast(r.y) && q.y >= p.y.coerceAtMost(
        r.y
    )
}


fun get_line_intersection(line1: LineD, line2: LineD): PointD? {
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

    return null // No collision
}


// Returns true if any two lines intersect.
fun isIntersect(arr: List<LineD>): Int {
    val e = LinkedList<Event>()

    // Pushing all points to a vector of events
    for (i in arr.indices) {
        if (arr[i].p0.x < arr[i].p1.x) {
            e.add(Event(arr[i].p0, arr[i], true))
            e.add(Event(arr[i].p1, arr[i], false))
        } else {
            e.add(Event(arr[i].p1, arr[i], true))
            e.add(Event(arr[i].p0, arr[i], false))
        }
    }

    e.sort()

    var intersections = 0
    val s = HashMap<LineD, Event>()
    for (event in e) {
        if (event.isLeft) {
            for (otherEvent in s.values) {
                val intersection = get_line_intersection(otherEvent.line, event.line)
                if (intersection != null) {
                    intersections++
                    println(intersection)
                }
            }

            s[event.line] = event
        } else {
            s.remove(event.line)
        }
    }

    println(intersections)

    return intersections
}
