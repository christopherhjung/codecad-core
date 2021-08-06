package com.codecad.core

import com.codecad.common.PointD
import de.lighti.clipper.*
import de.lighti.clipper.Clipper.*
import java.util.*


class ClipperOffset @JvmOverloads constructor(
    private val miterLimit: Double = 2.0,
    private val arcTolerance: Double = DEFAULT_ARC_TOLERANCE
) {
    private var destPolys: MutableList<MutableList<PointD>>? = null
    private var srcPoly: MutableList<PointD>? = null
    private var destPoly: MutableList<PointD>? = null
    private val normals: MutableList<PointD>
    private var delta = 0.0
    private var inA = 0.0
    private var sin = 0.0
    private var cos = 0.0
    private var miterLim = 0.0
    private var stepsPerRad = 0.0
    private var lowest: PointD
    private val polyNodes: PolyNode
    fun addPath(path: List<PointD>, joinType: JoinType?, endType: EndType) {
        var highI = path.size - 1
        if (highI < 0) {
            return
        }
        val newNode = PolyNode()
        newNode.joinType = joinType
        newNode.endType = endType

        //strip duplicate points from path and also get index to the lowest point ...
        if (endType == EndType.CLOSED_LINE || endType == EndType.CLOSED_POLYGON) {
            while (highI > 0 && path[0] === path[highI]) {
                highI--
            }
        }
        newNode.polygon.add(path[0])
        var j = 0
        var k = 0
        for (i in 1..highI) {
            if (newNode.polygon[j] !== path[i]) {
                j++
                newNode.polygon.add(path[i])
                if (path[i].y > newNode.polygon[k].y || path[i].y == newNode.polygon[k].y
                    && path[i].x < newNode.polygon[k].x
                ) {
                    k = j
                }
            }
        }
        if (endType == EndType.CLOSED_POLYGON && j < 2) {
            return
        }
        polyNodes.addChild(newNode)

        //if this path's lowest pt is lower than all the others then update m_lowest
        if (endType != EndType.CLOSED_POLYGON) {
            return
        }
        if (lowest.x < 0) {
            lowest = PointD((polyNodes.childCount - 1).toDouble(), k.toDouble())
        } else {
            val ip = polyNodes.childs[lowest.x.toInt()].polygon[lowest.y.toInt()]
            if (newNode.polygon[k].y > ip.y || newNode.polygon[k].y == ip.y
                && newNode.polygon[k].x < ip.x
            ) {
                lowest = PointD((polyNodes.childCount - 1).toDouble(), k.toDouble())
            }
        }
    }

    fun clear() {
        polyNodes.childs.clear()
        lowest = PointD(-1.0, lowest.y)
    }

    private fun doMiter(j: Int, k: Int, r: Double) {
        val q = delta / r
        destPoly!!.add(
            PointD(
                (srcPoly!![j].x + (normals[k].x + normals[j].x) * q),
                (srcPoly!![j].y + (normals[k].y + normals[j].y) * q)
            )
        )
    }

    private fun doOffset(delta: Double) {
        destPolys = mutableListOf()
        this.delta = delta

        //if Zero offset, just copy any CLOSED polygons to m_p and return ...
        if (nearZero(delta)) {
            for (i in 0 until polyNodes.childCount) {
                val node = polyNodes.childs[i]
                if (node.endType == EndType.CLOSED_POLYGON) {
                    destPolys!!.add(node.polygon)
                }
            }
            return
        }

        //see offset_triginometry3.svg in the documentation folder ...
        if (miterLimit > 2) {
            miterLim = 2 / (miterLimit * miterLimit)
        } else {
            miterLim = 0.5
        }
        val y: Double
        if (arcTolerance <= 0.0) {
            y = DEFAULT_ARC_TOLERANCE
        } else if (arcTolerance > Math.abs(delta) * DEFAULT_ARC_TOLERANCE) {
            y = Math.abs(delta) * DEFAULT_ARC_TOLERANCE
        } else {
            y = arcTolerance
        }
        //see offset_triginometry2.svg in the documentation folder ...
        val steps = Math.PI / Math.acos(1 - y / Math.abs(delta))
        sin = Math.sin(TWO_PI / steps)
        cos = Math.cos(TWO_PI / steps)
        stepsPerRad = steps / TWO_PI
        if (delta < 0.0) {
            sin = -sin
        }
        for (i in 0 until polyNodes.childCount) {
            val node = polyNodes.childs[i]
            srcPoly = node.polygon
            val len = srcPoly!!.size
            if (len == 0 || delta <= 0 && (len < 3 || node.endType != EndType.CLOSED_POLYGON)) {
                continue
            }
            destPoly = mutableListOf()

            //build m_normals ...
            normals.clear()
            for (j in 0 until len - 1) {
                normals.add(PointUtils.getUnitNormal(srcPoly!![j], srcPoly!![j + 1]))
            }
            if (node.endType == EndType.CLOSED_LINE || node.endType == EndType.CLOSED_POLYGON) {
                normals.add(PointUtils.getUnitNormal(srcPoly!!.get(len - 1), srcPoly!!.get(0)))
            } else {
                normals.add(normals[len - 2])
            }
            if (node.endType == EndType.CLOSED_POLYGON) {
                val k = intArrayOf(len - 1)
                for (j in 0 until len) {
                    offsetPoint(j, k, node.joinType!!)
                }
                destPolys!!.add(destPoly!!)
            } else if (node.endType == EndType.CLOSED_LINE) {
                val k = intArrayOf(len - 1)
                for (j in 0 until len) {
                    offsetPoint(j, k, node.joinType!!)
                }
                destPolys!!.add(destPoly!!)
                destPoly = mutableListOf()
                //re-build m_normals ...
                val n = normals[len - 1]
                for (j in len - 1 downTo 1) {
                    normals[j] = PointD(-normals[j - 1].x, -normals[j - 1].y)
                }
                normals[0] = PointD(-n.x, -n.y, 0.0)
                k[0] = 0
                for (j in len - 1 downTo 0) {
                    offsetPoint(j, k, node.joinType!!)
                }
                destPolys!!.add(destPoly!!)
            } else {
                val k = IntArray(1)
                for (j in 1 until len - 1) {
                    offsetPoint(j, k, node.joinType!!)
                }
                var pt1: PointD
                if (node.endType == EndType.OPEN_BUTT) {
                    val j = len - 1
                    pt1 = PointD(
                        (srcPoly!!.get(j).x + normals[j].x * delta), (
                            srcPoly!!.get(j)
                                .y + normals[j].y * delta
                        ), 0.0
                    )
                    destPoly!!.add(pt1)
                    pt1 = PointD(
                        (srcPoly!!.get(j).x - normals[j].x * delta), (
                            srcPoly!!.get(j)
                                .y - normals[j].y * delta
                        ), 0.0
                    )
                    destPoly!!.add(pt1)
                } else {
                    val j = len - 1
                    k[0] = len - 2
                    inA = 0.0
                    normals[j] = PointD(-normals[j].x, -normals[j].y)
                    if (node.endType == EndType.OPEN_SQUARE) {
                        doSquare(j, k[0])
                    } else {
                        doRound(j, k[0])
                    }
                }

                //re-build m_normals ...
                for (j in len - 1 downTo 1) {
                    normals[j] = PointD(-normals[j - 1].x, -normals[j - 1].y)
                }
                normals[0] = PointD(-normals[1].x, -normals[1].y)
                k[0] = len - 1
                for (j in k[0] - 1 downTo 1) {
                    offsetPoint(j, k, node.joinType!!)
                }
                if (node.endType == EndType.OPEN_BUTT) {
                    pt1 = PointD(
                        (srcPoly!!.get(0).x - normals[0].x * delta), (
                            srcPoly!!.get(0)
                                .y - normals[0].y * delta
                        )
                    )
                    destPoly!!.add(pt1)
                    pt1 = PointD(
                        (srcPoly!!.get(0).x + normals[0].x * delta), (
                            srcPoly!!.get(0)
                                .y + normals[0].y * delta
                        )
                    )
                    destPoly!!.add(pt1)
                } else {
                    k[0] = 1
                    inA = 0.0
                    if (node.endType == EndType.OPEN_SQUARE) {
                        doSquare(0, 1)
                    } else {
                        doRound(0, 1)
                    }
                }
                destPolys!!.add(destPoly!!)
            }
        }
    }

    private fun doRound(j: Int, k: Int) {
        val a = Math.atan2(inA, normals[k].x * normals[j].x + normals[k].y * normals[j].y)
        val steps = Math.max((stepsPerRad * Math.abs(a)).toInt(), 1)
        var X = normals[k].x
        var Y = normals[k].y
        var X2: Double
        for (i in 0 until steps) {
            destPoly!!.add(
                PointD(
                    (srcPoly!![j].x + X * delta), (
                        srcPoly!![j].y + Y * delta
                    )
                )
            )
            X2 = X
            X = X * cos - sin * Y
            Y = X2 * sin + Y * cos
        }
        destPoly!!.add(
            PointD(
                srcPoly!![j].x + normals[j].x * delta,
                srcPoly!![j].y  + normals[j].y * delta
            )
        )
    }

    private fun doSquare(j: Int, k: Int) {
        val nkx = normals[k].x
        val nky = normals[k].y
        val njx = normals[j].x
        val njy = normals[j].y
        val sjx = srcPoly!![j].x
        val sjy = srcPoly!![j].y
        val dx = Math.tan(Math.atan2(inA, nkx * njx + nky * njy) / 4)
        destPoly!!.add(
            PointD(
                (sjx + delta * (nkx - nky * dx)),
                (sjy + delta * (nky + nkx * dx)),
                0.0
            )
        )
        destPoly!!.add(
            PointD(
                (sjx + delta * (njx + njy * dx)),
                (sjy + delta * (njy - njx * dx)),
                0.0
            )
        )
    }

    //------------------------------------------------------------------------------
    fun execute(delta: Double) {
        fixOrientations()
        doOffset(delta)
        //now clean up 'corners' ...
        //val clpr = DefaultClipper(REVERSE_SOLUTION)
        //clpr.addPaths(destPolys!!, PolyType.SUBJECT, true)
        if (delta > 0) {
            //clpr.execute(ClipType.UNION, solution, PolyFillType.POSITIVE, PolyFillType.POSITIVE)
        } else {
            val r = destPolys!!.bounds()
            val outer = mutableListOf<PointD>()
            outer.add(PointD(r.left - 10, r.bottom + 10, 0.0))
            outer.add(PointD(r.right + 10, r.bottom + 10, 0.0))
            outer.add(PointD(r.right + 10, r.top - 10, 0.0))
            outer.add(PointD(r.left - 10, r.top - 10, 0.0))
            //clpr.addPath(outer, PolyType.SUBJECT, true)
            //clpr.execute(ClipType.UNION, solution, PolyFillType.NEGATIVE, PolyFillType.NEGATIVE)
            /*if (solution.size > 0) {
                solution.removeAt(0)
            }*/
        }
    }


    //------------------------------------------------------------------------------
    private fun fixOrientations() {
        //fixup orientations of all closed paths if the orientation of the
        //closed path with the lowermost vertex is wrong ...
        if (lowest.x >= 0 && !polyNodes.childs[lowest.x.toInt()].polygon.orientation()) {
            for (i in 0 until polyNodes.childCount) {
                val node = polyNodes.childs[i]
                if (node.endType == EndType.CLOSED_POLYGON || node.endType == EndType.CLOSED_LINE && node.polygon.orientation()) {
                    node.polygon.reverse()
                }
            }
        } else {
            for (i in 0 until polyNodes.childCount) {
                val node = polyNodes.childs[i]
                if (node.endType == EndType.CLOSED_LINE && !node.polygon.orientation()) {
                    node.polygon.reverse()
                }
            }
        }
    }

    private fun offsetPoint(j: Int, kV: IntArray, jointype: JoinType) {
        //cross product ...
        val k = kV[0]
        val nkx = normals[k].x
        val nky = normals[k].y
        val njy = normals[j].y
        val njx = normals[j].x
        val sjx = srcPoly!![j].x
        val sjy = srcPoly!![j].y
        inA = nkx * njy - njx * nky
        if (Math.abs(inA * delta) < 1.0) {
            //dot product ...
            val cosA = nkx * njx + njy * nky
            if (cosA > 0) // angle ==> 0 degrees
            {
                destPoly!!.add(PointD((sjx + nkx * delta), (sjy + nky * delta), 0.0))
                return
            }
            //else angle ==> 180 degrees
        } else if (inA > 1.0) {
            inA = 1.0
        } else if (inA < -1.0) {
            inA = -1.0
        }
        if (inA * delta < 0) {
            destPoly!!.add(PointD((sjx + nkx * delta), (sjy + nky * delta)))
            destPoly!!.add(srcPoly!![j])
            destPoly!!.add(PointD((sjx + njx * delta), (sjy + njy * delta)))
        } else {
            when (jointype) {
                JoinType.MITER -> {
                    val r = 1 + (njx * nkx) + (njy * nky)
                    if (r >= miterLim) {
                        doMiter(j, k, r)
                    } else {
                        doSquare(j, k)
                    }
                }
                JoinType.SQUARE -> doSquare(j, k)
                JoinType.ROUND -> doRound(j, k)
            }
        }
        kV[0] = j
    } //------------------------------------------------------------------------------

    companion object {
        private fun nearZero(`val`: Double): Boolean {
            return `val` > -TOLERANCE && `val` < TOLERANCE
        }

        private val TWO_PI = Math.PI * 2
        private val DEFAULT_ARC_TOLERANCE = 0.25
        private val TOLERANCE = 1.0E-20
    }

    init {
        lowest = PointD(-1.0, 0.0)
        polyNodes = PolyNode()
        normals = ArrayList()
    }
}

class RectD {
    var left: Double = 0.0
    var top: Double = 0.0
    var right: Double = 0.0
    var bottom: Double = 0.0

    constructor() {}
    constructor(l: Double, t: Double, r: Double, b: Double) {
        left = l
        top = t
        right = r
        bottom = b
    }

    constructor(ir: RectD) {
        left = ir.left
        top = ir.top
        right = ir.right
        bottom = ir.bottom
    }
}


fun List<List<PointD>>.bounds() : RectD{
    var i = 0
    val cnt = size
    val result = RectD()
    while (i < cnt && get(i).isEmpty()) {
        i++
    }
    if (i == cnt) {
        return result
    }
    result.left = get(i)[0].x
    result.right = result.left
    result.top = get(i)[0].y
    result.bottom = result.top
    while (i < cnt) {
        for (j in get(i).indices) {
            if (get(i)[j].x < result.left) {
                result.left = get(i)[j].x
            } else if (get(i)[j].x > result.right) {
                result.right = get(i)[j].x
            }
            if (get(i)[j].y < result.top) {
                result.top = get(i)[j].y
            } else if (get(i)[j].y > result.bottom) {
                result.bottom = get(i)[j].y
            }
        }
        i++
    }
    return result
}

fun List<PointD>.orientation(): Boolean {
    return area() >= 0
}


fun List<PointD>.area() : Double{
    if(size < 3){
        return 0.0
    }

    var a = 0.0
    var i = 0
    var j: Int = size - 1
    while (i < size) {
        a += (get(j).x + get(i).x) * (get(j).y - get(i).y)
        j = i
        ++i
    }
    return -a * 0.5
}


class PointUtils{
    companion object{
        fun getUnitNormal(pt1: PointD, pt2: PointD): PointD {
            var dx = (pt2.x - pt1.x)
            var dy = (pt2.y - pt1.y)
            return if (dx == 0.0 && dy == 0.0) {
                PointD()
            } else {
                val f = 1.0 / Math.sqrt(dx * dx + dy * dy)
                dx *= f
                dy *= f
                PointD(dy, -dx)
            }
        }
    }
}
