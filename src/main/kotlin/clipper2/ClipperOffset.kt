package clipper2

import kotlin.math.*


enum class JoinType { Square, Round, Miter }
enum class EndType { Polygon, OpenJoined, OpenButt, OpenSquare, OpenRound }

class PointD(var x: Double = 0.0, var y: Double = 0.0) {
    constructor(dp: PointD) : this(dp.x, dp.y)
    constructor(dp: Point64) : this(dp.x.toDouble(), dp.y.toDouble())

} //PointD

class PathNode(p: Path, jt: JoinType, et: EndType) {
    var path = Path()
    var joinType: JoinType = JoinType.Round
    var endType: EndType = EndType.OpenJoined
    var lowestIdx: Int = 0

    init {
        joinType = jt
        endType = et

        var lenP = p . size
                if (et == EndType.Polygon || et == EndType.OpenJoined)
                    while (lenP > 1 && p[lenP - 1] == p[0]) lenP--
                else if (lenP == 2 && p[1] == p[0])
                    lenP = 1
        if (lenP != 0) {
            if (lenP < 3 && (et == EndType.Polygon || et == EndType.OpenJoined)) {
                if (jt == JoinType.Round) endType = EndType.OpenRound
                else endType = EndType.OpenSquare
            }

            path = Path(lenP)
            path.add(p[0])

            var lastIp = p[0]
            lowestIdx = 0
            for ( i in 1 until lenP)
            {
                if (lastIp == p[i]) continue
                path.add(p[i])
                lastIp = p[i]
                if (et != EndType.Polygon) continue
                if (p[i].y >= path[lowestIdx].y &&
                    (p[i].y > path[lowestIdx].y || p[i].x < path[lowestIdx].x)
                )
                    lowestIdx = path.size - 1
            }
            if (endType == EndType.Polygon && path.size < 3) path.clear()
        }
    }
} //PathNode


class ClipperOffset(val MiterLimit: Double = 2.0, val ArcTolerance: Double = 0.0) {

    var delta: Double = 0.0
    var sinA: Double = 0.0
    var sin: Double = 0.0
    var cos: Double = 0.0
    var miterLim: Double = 0.0
    var stepsPerRad: Double = 0.0

    private var solution: Paths? = null
    private var pathIn: Path? = null
    private var pathOut: Path? = null
    private var norms = mutableListOf<PointD>()
    private var nodes = mutableListOf<PathNode>()
    private var lowestIdx: Int = 0
    private var PointZero = Point64(0, 0)

    private val TwoPi = Math.PI * 2
    private val DefaultArcFrac = 0.02
    private val Tolerance = 1.0E-15


    fun Clear() {
        nodes.clear()
        norms.clear()
        solution?.clear()
    }


    fun addPath(p: Path, jt: JoinType, et: EndType) {
        var pn: PathNode? = PathNode(p, jt, et)
        if (pn?.path == null) pn = null
        else nodes.add(pn)
    }


    fun addPaths(paths: Paths, jt: JoinType, et: EndType) {
        for (p in paths) {
            addPath(p, jt, et)
        }
    }


    private fun GetLowestPolygonIdx() {
        lowestIdx = -1
        var ip1 = PointZero
        var ip2: Point64
        for (i in nodes.indices) {
            val node = nodes[i]
            if (node.endType != EndType.Polygon) continue
            if (lowestIdx < 0) {
                ip1 = node.path[node.lowestIdx]
                lowestIdx = i
            } else {
                ip2 = node.path[node.lowestIdx]
                if (ip2.y >= ip1.y && (ip2.y > ip1.y || ip2.x < ip1.x)) {
                    lowestIdx = i
                    ip1 = ip2
                }
            }
        }
    }


    fun OffsetPoint(j: Int,k : Int, jointype: JoinType) : Int {
        //A: angle between adjoining paths on left side (left WRT winding direction).
        //A == 0 deg (or A == 360 deg): collinear edges heading in same direction
        //A == 180 deg: collinear edges heading in opposite directions (ie a 'spike')
        //sin(A) < 0: convex on left.
        //cos(A) > 0: angles on both left and right sides > 90 degrees

        //cross product ...
        sinA = (norms[k].x * norms[j].y - norms[j].x * norms[k].y)

        if (abs(sinA * delta) < 1.0) //angle is approaching 180 or 360 deg.
        {
            //dot product ...
            var cosA = (norms[k].x * norms[j].x + norms[j].y * norms[k].y)
            if (cosA > 0) //given condition above the angle is approaching 360 deg.
            {
                //with angles approaching 360 deg collinear (whether concave or convex),
                //offsetting with two or more vertices (that would be so close together)
                //occasionally causes tiny self-intersections due to rounding.
                //So we offset with just a single vertex here ...
                pathOut!!.add(
                    Point64(
                        round(pathIn!![j].x + norms[k].x * delta),
                        round(pathIn!![j].y + norms[k].y * delta)
                    )
                )
                return k
            }
        } else if (sinA > 1.0) sinA = 1.0
        else if (sinA < -1.0) sinA = -1.0

        if (sinA * delta < 0) //ie a concave offset
        {
            pathOut!!.add(
                Point64(
                    round(pathIn!![j].x + norms[k].x * delta),
                    round(pathIn!![j].y + norms[k].y * delta)
                )
            )
            pathOut!!.add(pathIn!![j])
            pathOut!!.add(
                Point64(
                    round(pathIn!![j].x + norms[j].x * delta),
                    round(pathIn!![j].y + norms[j].y * delta)
                )
            )
        } else {
            //convex offsets here ...
            var cosA: Double
            when (jointype) {
                JoinType.Miter -> {
                    cosA = (norms[j].x * norms[k].x + norms[j].y * norms[k].y)
                    //see offset_triginometry3.svg
                    if (1 + cosA < miterLim) DoSquare(j, k)
                    else DoMiter(j, k, 1 + cosA)
                }
                JoinType . Square -> {
                    cosA = (norms[j].x * norms[k].x + norms[j].y * norms[k].y)
                    if (cosA >= 0) DoMiter(j, k, 1 + cosA) //angles >= 90 deg. don't need squaring
                    else DoSquare(j, k)
                }
                JoinType . Round -> DoRound (j, k)
            }
        }
        return j
    }


    fun DoSquare(j: Int, k: Int) {
        //Two vertices, one using the prior offset's (k) normal one the current (j).
        //Do a 'normal' offset (by delta) and then another by 'de-normaling' the
        //normal hence parallel to the direction of the respective edges.
        if (delta > 0) {
            pathOut!!.add(
                Point64(
                    round(pathIn!![j].x + delta * (norms[k].x - norms[k].y)),
                    round(pathIn!![j].y + delta * (norms[k].y + norms[k].x))
                )
            )
            pathOut!!.add(
                Point64(
                    round(pathIn!![j].x + delta * (norms[j].x + norms[j].y)),
                    round(pathIn!![j].y + delta * (norms[j].y - norms[j].x))
                )
            )
        } else {
            pathOut!!.add(
                Point64(
                    round(pathIn!![j].x + delta * (norms[k].x + norms[k].y)),
                    round(pathIn!![j].y + delta * (norms[k].y - norms[k].x))
                )
            )
            pathOut!!.add(
                Point64(
                    round(pathIn!![j].x + delta * (norms[j].x - norms[j].y)),
                    round(pathIn!![j].y + delta * (norms[j].y + norms[j].x))
                )
            )
        }
    }


    fun DoMiter(j: Int, k: Int, cosAplus1: Double) {
        //see offset_triginometry4.svg
        var q = delta / cosAplus1 //0 < cosAplus1 <= 2
        pathOut!!.add(
            Point64(
                round(pathIn!![j].x + (norms[k].x + norms[j].x) * q),
                round(pathIn!![j].y + (norms[k].y + norms[j].y) * q)
            )
        )
    }


    fun DoRound(j: Int, k: Int) {
        val a = atan2(
            sinA,
            norms[k].x * norms[j].x + norms[k].y * norms[j].y
        )
        val steps = max(round(stepsPerRad * abs(a)), 1)

        var X = norms[k].x
        var Y = norms[k].y
        var X2 : Double
        for ( i in 0 until steps)
        {
            pathOut!!.add(
                Point64(
                    round(pathIn!![j].x + X * delta),
                    round(pathIn!![j].y + Y * delta)
                )
            )
            X2 = X
            X = X * cos - sin * Y
            Y = X2 * sin + Y * cos
        }
        pathOut!!.add(
            Point64(
                round(pathIn!![j].x + norms[j].x * delta),
                round(pathIn!![j].y + norms[j].y * delta)
            )
        )
    }

    private fun DoOffset(d: Double) {
        solution = null
        delta = d
        val absDelta = abs(d)

        //if a Zero offset, then just copy CLOSED polygons to FSolution and return ...
        if (absDelta < Tolerance) {
            solution = Paths()
            for( node in nodes){
                if (node.endType == EndType.Polygon) {
                    solution?.add(node.path)
                }
            }
            return
        }

        //MiterLimit: see offset_triginometry3.svg in the documentation folder ...
        if (MiterLimit > 2)
            miterLim = 2 / (MiterLimit * MiterLimit)
        else
            miterLim = 0.5

        val arcTol = if (ArcTolerance < DefaultArcFrac)
             absDelta * DefaultArcFrac; else
             ArcTolerance

        //see offset_triginometry2.svg in the documentation folder ...
        var steps = Math.PI / acos(1 - arcTol / absDelta)  //steps per 360 degrees
        if (steps > absDelta * Math.PI) steps = absDelta * Math.PI //ie excessive precision check

        sin = sin(TwoPi / steps)
        cos = cos(TwoPi / steps)
        if (d < 0) sin = -sin
        stepsPerRad = steps / TwoPi

        solution = Paths()
        for( node in nodes)
        {
            pathIn = node.path
            pathOut = Path()
            val pathInCnt = pathIn!!.size

            //if a single vertex then build circle or a square ...
            if (pathInCnt == 1) {
                if (node.joinType == JoinType.Round) {
                    var X = 1.0
                    var Y = 0.0
                    for (j in 1 until ceil(steps).roundToInt())
                    {
                        pathOut!!.add(
                            Point64(
                                round(pathIn!![0].x + X * delta),
                                round(pathIn!![0].y + Y * delta)
                            )
                        )
                        val X2 = X
                        X = X * cos - sin * Y
                        Y = X2 * sin + Y * cos
                    }
                } else {
                    var X = - 1.0
                    var Y = -1.0
                    for ( j in 0 until 4)
                    {
                        pathOut!!.add(
                            Point64(
                                round(pathIn!![0].x + X * delta),
                                round(pathIn!![0].y + Y * delta)
                            )
                        )
                        if (X < 0) X = 1.0
                        else if (Y < 0) Y = 1.0
                        else X = -1.0
                    }
                }
                solution?.add(pathOut!!)
                continue
            } //end of single vertex offsetting

            //build norms ...
            norms.clear()
            for (j in 0 until pathInCnt - 1){
                norms.add(getUnitNormal(pathIn!![j], pathIn!![j + 1]))
            }
            if (node.endType == EndType.OpenJoined || node.endType == EndType.Polygon)
                norms.add(getUnitNormal(pathIn!![pathInCnt - 1], pathIn!![0]))
            else
                norms.add(PointD (norms[pathInCnt - 2]))

            if (node.endType == EndType.Polygon) {
                var k = pathInCnt - 1
                for (j in 0 until pathInCnt){
                    k = OffsetPoint(j, k, node.joinType)
                }
                solution?.add(pathOut!!)
            } else if (node.endType == EndType.OpenJoined) {
                var k = pathInCnt - 1
                for (j in 0 until pathInCnt){
                    k = OffsetPoint(j, k, node.joinType)
                }
                solution?.add(pathOut!!)
                pathOut = Path()
                //re-build norms ...
                var n = norms [pathInCnt - 1]
                for (j in pathInCnt - 1 downTo  1) {
                    norms[j] = PointD(-norms[j - 1].x, -norms[j - 1].y)
                }
                norms[0] = PointD(-n.x, -n.y)
                k = 0
                for (j in pathInCnt - 1 downTo  0){
                    k =  OffsetPoint(j, k, node.joinType)
                }
                solution?.add(pathOut!!)
            } else {
                var k = 0
                for (j in 1 until pathInCnt - 1)
                k = OffsetPoint(j, k, node.joinType)

                var pt1: Point64
                if (node.endType == EndType.OpenButt) {
                    var j = pathInCnt -1
                    pt1 = Point64(
                        round(pathIn!![j].x + norms[j].x *
                                delta), round(pathIn!![j].y + norms[j].y * delta)
                    )
                    pathOut!!.add(pt1)
                    pt1 = Point64(
                        round(pathIn!![j].x - norms[j].x *
                                delta), round(pathIn!![j].y - norms[j].y * delta)
                    )
                    pathOut!!.add(pt1)
                } else {
                    var j = pathInCnt -1
                    k = pathInCnt - 2
                    sinA = 0.0
                    norms[j] = PointD(-norms[j].x, -norms[j].y)
                    if (node.endType == EndType.OpenSquare)
                        DoSquare(j, k)
                    else
                        DoRound(j, k)
                }

                //reverse norms ...
                for (j in pathInCnt - 1 downTo  1 ){
                    norms[j] = PointD(-norms[j - 1].x, -norms[j - 1].y)

                }
                norms[0] = PointD(-norms[1].x, -norms[1].y)

                k = pathInCnt - 1
                for (j in k - 1 downTo  1 ) {
                    k = OffsetPoint(j, k, node.joinType)
                }

                if (node.endType == EndType.OpenButt) {
                    pt1 = Point64(
                         round(pathIn!![0].x - norms[0].x * delta),
                         round(pathIn!![0].y - norms[0].y * delta)
                    )
                    pathOut!!.add(pt1)
                    pt1 = Point64(
                         round(pathIn!![0].x + norms[0].x * delta),
                         round(pathIn!![0].y + norms[0].y * delta)
                    )
                    pathOut!!.add(pt1)
                } else {
                    k = 1
                    sinA = 0.0
                    if (node.endType == EndType.OpenSquare)
                        DoSquare(0, 1)
                    else
                        DoRound(0, 1)
                }
                solution?.add(pathOut!!)
            }
        }
    }


    fun execute(sol: Paths, delta: Double) {
        sol.clear()
        if (nodes.size == 0) return

        GetLowestPolygonIdx()
        var negate = (lowestIdx >= 0 && area(nodes[lowestIdx].path) < 0)
        //if polygon orientations are reversed, then 'negate' ...
        if (negate) this.delta = -delta
        else this.delta = delta
        DoOffset(this.delta)



        //now clean up 'corners' ...
        var clpr = Clipper()
        clpr.addPaths(solution!!, PathType.Subject)
        if (negate)
            clpr.Execute(ClipType.Union, sol, FillRule.Negative)
        else
            clpr.Execute(ClipType.Union, sol, FillRule.Positive)
    }


    companion object {


        fun getUnitNormal(pt1: Point64, pt2: Point64): PointD {
            var dx = (pt2.x - pt1.x).toDouble()
            var dy = (pt2.y - pt1.y).toDouble()
            if ((dx == 0.0) && (dy == 0.0)) return PointD()

            var f = 1 * 1.0 / sqrt(dx * dx + dy * dy)
            dx *= f
            dy *= f

            return PointD(dy, -dx)
        }

        fun offsetPaths(pp: Paths, delta: Double, jt: JoinType, et: EndType): Paths {
            var result = Paths()
            var co = ClipperOffset ()
            co.addPaths(pp, jt, et)
            co.execute( result, delta)
            return result
        }


        fun area(p: Path): Double {
            val cnt = p.size
            if (cnt < 3) return 0.0
            var a = 0.0
            var j = cnt - 1
            for (i in 0 until cnt) {
                a += (p[j].x + p[i].x).toDouble() * (p[j].y - p[i].y).toDouble()
                j = i
            }
            return -a * 0.5
        }

        fun round(value: Double): Long {
            return if(value < 0 )  (value-0.5).toLong() else (value+0.5).toLong()
        }
    }


} //ClipperOffset

