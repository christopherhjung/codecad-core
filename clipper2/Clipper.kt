package clipper2

import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs


//------------------------------------------------------------------------------
// Clipper
//------------------------------------------------------------------------------


class Clipper {

    var scanline: ScanLine? = null
    var HasOpenPaths: Boolean = false
    var CurrentLocMinIdx: Int = 0
    var LocMinListSorted: Boolean = false
    var VertexList = ArrayList<List<Vertex>>()
    var OutRecList = ArrayList<OutRec>()
    var Actives: Active? = null
    private var SEL: Active? = null
    private var LocMinimaList = ArrayList<LocalMinima>()
    var LocalMinimaComparer = MyLocalMinSort()
    private var IntersectList = ArrayList<IntersectNode>()
    var IntersectNodeComparer = MyIntersectNodeSort()
    private var clipType: ClipType = ClipType.Union
    private var fillType: FillRule = FillRule.Positive

    private fun IsHotEdge(Edge: Active): Boolean {
        return Edge.OutRec != null
    }

    private fun IsStartSide(Edge: Active): Boolean {
        return (Edge == Edge.OutRec!!.StartE)
    }

    fun GetTopDeltaX(e1: Active, e2: Active): Long {
        if (e1.Top!!.y > e2.Top!!.y)
            return TopX(e2, e1.Top!!.y) - e1.Top!!.x
        else
            return e2.Top!!.x - TopX(e1, e2.Top!!.y)
    }

    private fun E2InsertsBeforeE1(e1: Active, e2: Active): Boolean {
        return if (e2.Curr!!.x == e1.Curr!!.x) GetTopDeltaX(e1, e2) < 0 else e2.Curr!!.x < e1.Curr!!.x
    }

    private fun GetIntersectPoint(edge1: Active, edge2: Active): Point64 {
        var ip = Point64()
        //nb: with very large coordinate values, it's possible for SlopesEqual() to
        //return false but for the edge.Dx value be equal due to Double precision rounding.
        if (edge1.Dx == edge2.Dx) {
            ip.y = edge1.Curr!!.y
            ip.x = TopX(edge1, ip.y)
            return ip
        }

        if (edge1.Dx == 0.0) {
            ip.x = edge1.Bot!!.x
            if (IsHorizontal(edge2)) {
                ip.y = edge2.Bot!!.y
            } else {
                val b2 = edge2.Bot!!.y - (edge2.Bot!!.x / edge2.Dx!!)
                ip.y = Round(ip.x / edge2.Dx!! + b2)
            }
        } else if (edge2.Dx == 0.0) {
            ip.x = edge2.Bot!!.x
            if (IsHorizontal(edge1)) {
                ip.y = edge1.Bot!!.y
            } else {
                val b1 = edge1.Bot!!.y - (edge1.Bot!!.x / edge1.Dx!!)
                ip.y = Round(ip.x / edge1.Dx!! + b1)
            }
        } else {
            val b1 = edge1.Bot!!.x - edge1.Bot!!.y * edge1.Dx!!
            val b2 = edge2.Bot!!.x - edge2.Bot!!.y * edge2.Dx!!
            val q = (b2 - b1) / (edge1.Dx!! - edge2.Dx!!)
            ip.y = Round(q)
            if (abs(edge1.Dx!!) < abs(edge2.Dx!!))
                ip.x = Round(edge1.Dx!! * q + b1)
            else
                ip.x = Round(edge2.Dx!! * q + b2)
        }
        return ip
    }

    private fun SetDx(e: Active) {
        val dy: Long = (e.Top!!.y - e.Bot!!.y)
        e.Dx = if(dy == 0L) horizontal else ((e.Top!!.x-e.Bot!!.x) / dy).toDouble()
    }
    //---------------------------------------------------------------------------

    private fun NextVertex(e: Active): Vertex? {
        return (if (e.WindDx > 0) e.VertTop!!.Next else e.VertTop!!.Prev)
    }

    private fun IsMaxima(e: Active): Boolean {
        return (VertexFlags.LocMax and e.VertTop!!.Flags) != 0
    }

    fun GetMaximaPair(e: Active): Active? {
        var e2: Active? = null
        if (IsHorizontal(e)) {
            //we can't be sure whether the MaximaPair is on the left or right, so ...
            e2 = e.PrevInAEL
            while (e2 != null && e2.Curr!!.x >= e.Top!!.x) {
                if (e2.VertTop == e.VertTop) return e2  //Found!
                e2 = e2.PrevInAEL
            }
            e2 = e.NextInAEL
            while (e2 != null && TopX(e2, e.Top!!.y) <= e.Top!!.x) {
                if (e2.VertTop == e.VertTop) return e2  //Found!
                e2 = e2.NextInAEL
            }
        } else {
            e2 = e.NextInAEL
            while (e2 != null) {
                if (e2.VertTop == e.VertTop) return e2 //Found!
                e2 = e2.NextInAEL
            }
        }
        return null
    }

    fun Clear() {
        LocMinimaList.clear()
        CurrentLocMinIdx = 0
        VertexList.clear()
        HasOpenPaths = false
    }

    open fun CleanUp() {
        while (Actives != null) {
            DeleteFromAEL(Actives!!)
        }
        DisposeScanLineList()
        OutRecList.clear()
    }

    private fun Reset() {
        if (!LocMinListSorted) {
            Collections.sort(LocMinimaList, LocalMinimaComparer)
            LocMinListSorted = true
        }
        for (locMin in LocMinimaList) {
            InsertScanline(locMin.Vertex!!.Pt.y)
        }
        CurrentLocMinIdx = 0
        Actives = null
        SEL = null
    }

    private fun InsertScanline(Y: Long) {
        //single-linked list: sorted descending, ignoring dups.
        if (scanline == null) {
            scanline = ScanLine()
            scanline!!.Next = null
            scanline!!.Y = Y
        } else if (Y > scanline!!.Y!!) {
            val newSb = ScanLine()
            newSb.Y = Y
            newSb.Next = scanline
            scanline = newSb
        } else {
            var sb2 = ScanLine()
            while (sb2.Next != null && (Y <= sb2.Next!!.Y!!)) {
                sb2 = sb2.Next!!
            }
            if (Y == sb2.Y) return //ie ignores duplicates
            val newSb = ScanLine()
            newSb.Y = Y
            newSb.Next = sb2.Next
            sb2.Next = newSb
        }
    }

    fun PopScanline(): Pair<Boolean, Long?> {
        if (scanline == null) {
            return false to 0L
        }
        val Y = scanline!!.Y
        val tmp = scanline!!.Next
        scanline = null
        scanline = tmp
        return true to Y
    }

    private fun DisposeScanLineList() {
        while (scanline != null) {
            val tmp = scanline!!.Next
            scanline = null
            scanline = tmp
        }
    }

    private fun PopLocalMinima(Y: Long): Pair<Boolean, LocalMinima?> {
        if (CurrentLocMinIdx == LocMinimaList.size) return false to null
        val locMin = LocMinimaList[CurrentLocMinIdx]
        if (locMin.Vertex!!.Pt.y == Y) {
            CurrentLocMinIdx++
            return true to locMin
        }
        return false to locMin
    }

    private fun addLocMin(vert: Vertex, pt: PathType, isOpen: Boolean) {
        //make sure the vertex is added only once ...
        if ((VertexFlags.LocMin and vert.Flags) != 0) {
            return
        }
        vert.Flags = vert.Flags or VertexFlags.LocMin
        val lm = LocalMinima()
        lm.Vertex = vert
        lm.PathType = pt
        lm.IsOpen = isOpen
        LocMinimaList.add(lm)
    }
    //----------------------------------------------------------------------------

    private fun addPathToVertexList(p: Path, pt: PathType, isOpen: Boolean) {
        var pathLen = p.size
        while (pathLen > 1 && p[pathLen - 1] == p[0]) {
            pathLen--
        }
        if (pathLen < 2) {
            return
        }

        var P0IsMinima = false
        var P0IsMaxima = false
        var goingUp = false
        var i = 1
        //find the first non-horizontal segment in the path ...
        while (i < pathLen && p[i].y == p[0].y) i++
        if (i == pathLen) //it's a totally flat path
        {
            if (!isOpen) return       //Ignore closed paths that have ZERO area.
        } else {
            goingUp = p[i].y < p[0].y //because I'm using an inverted Y-axis display
            if (goingUp) {
                i = pathLen - 1
                while (p[i].y == p[0].y) i--
                P0IsMinima = p[i].y < p[0].y //p[0].Y == a minima
            } else {
                i = pathLen - 1
                while (p[i].y == p[0].y) i--
                P0IsMaxima = p[i].y > p[0].y //p[0].Y == a maxima
            }
        }

        val va = mutableListOf<Vertex>()
        VertexList.add(va)
        var v = Vertex(p[0])
        if (isOpen) {
            v.Flags = VertexFlags.OpenStart
            if (goingUp) addLocMin(v, pt, isOpen)
            else v.Flags = v.Flags or VertexFlags.LocMax
        }
        va.add(v)
        //nb: polygon orientation is determined later (see InsertLocalMinimaIntoAEL).
        for (j in 1 until pathLen) {
            if (p[j] == v.Pt) continue //ie skips duplicates
            val v2 = Vertex(p[j])
            v.Next = v2
            v2.Prev = v
            if (v2.Pt.y > v.Pt.y && goingUp) {
                v.Flags = v.Flags or VertexFlags.LocMax
                goingUp = false
            } else if (v2.Pt.y < v.Pt.y && !goingUp) {
                goingUp = true
                addLocMin(v, pt, isOpen)
            }
            va.add(v2)
            v = v2
        }
        //i: index of the last vertex in the path.
        v.Next = va[0]
        va[0].Prev = v

        if (isOpen) {
            v.Flags = v.Flags or VertexFlags.OpenEnd
            if (goingUp) {
                v.Flags or VertexFlags.LocMax
            } else {
                addLocMin(v, pt, isOpen)
            }
        } else if (goingUp) {
            //going up so find local maxima ...
            while (v.Next!!.Pt.y <= v.Pt.y) {
                v = v.Next!!
            }
            v.Flags = v.Flags or VertexFlags.LocMax
            if (P0IsMinima) addLocMin(va[0], pt, isOpen) //ie just turned to going up
        } else {
            //going down so find local minima ...
            while (v.Next!!.Pt.y >= v.Pt.y) {
                v = v.Next!!
            }
            addLocMin(v, pt, isOpen)
            if (P0IsMaxima) va[0].Flags = v.Flags or VertexFlags.LocMax
        }
    }

    fun addPath(path: Path, pt: PathType, isOpen: Boolean = false) {
        if (isOpen) {
            if (pt == PathType.Clip)
                throw ClipperException("addPath: Only PathType.Subject paths can be open.")
            HasOpenPaths = true
        }
        addPathToVertexList(path, pt, isOpen)
        LocMinListSorted = false
    }

    fun addPaths(paths: Paths, pt: PathType, isOpen: Boolean = false) {
        for (path in paths) {
            addPath(path, pt, isOpen)
        }
    }

    private fun GetPathType(e: Active): PathType {
        return e.LocalMin!!.PathType!!
    }

    private fun IsSamePathType(e1: Active, e2: Active): Boolean {
        return (e1.LocalMin!!.PathType == e2.LocalMin!!.PathType)
    }


    private fun IsContributingClosed(e: Active): Boolean {
        when (this.fillType) {
            FillRule.NonZero -> if(abs(e.WindCnt) != 1) return false
            FillRule.Positive -> if (e.WindCnt != 1) return false
            FillRule.Negative -> if (e.WindCnt != -1) return false
        }
        when (this.clipType) {
            ClipType.Intersection -> return when (this.fillType) {
                FillRule.EvenOdd, FillRule.NonZero -> e.WindCnt2 != 0
                FillRule.Positive -> e.WindCnt2 > 0
                FillRule.Negative -> e.WindCnt2 < 0
            }
            ClipType.Union -> return when (this.fillType) {
                FillRule.EvenOdd, FillRule.NonZero -> e.WindCnt2 == 0
                FillRule.Positive -> e.WindCnt2 <= 0
                FillRule.Negative -> e.WindCnt2 >= 0
            }
            ClipType.Difference -> {
                return if (GetPathType(e) === PathType.Subject) when (this.fillType) {
                    FillRule.EvenOdd, FillRule.NonZero -> e.WindCnt2 == 0
                    FillRule.Positive -> e.WindCnt2 <= 0
                    FillRule.Negative -> e.WindCnt2 >= 0
                } else when (this.fillType) {
                    FillRule.EvenOdd, FillRule.NonZero -> e.WindCnt2 != 0
                    FillRule.Positive -> e.WindCnt2 > 0
                    FillRule.Negative -> e.WindCnt2 < 0
                }
            }
            ClipType.Xor -> return true //XOr is always contributing unless open
            else -> return false
        }
    }

    /*private fun IsContributingClosed(e: Active): Boolean {
        when (fillType) {
            FillRule.NonZero -> if (abs(e.WindCnt) != 1) return false
            FillRule.Positive -> if (e.WindCnt != 1) return false
            FillRule.Negative -> if (e.WindCnt != -1) return false
        }

        return when (clipType) {
            ClipType.Intersection -> !test(e)
            ClipType.Union -> test(e)
            ClipType.Difference -> {
                if (GetPathType(e) == PathType.Subject)
                    test(e)
                else
                    !test(e)
            }
            ClipType.Xor ->
                true //XOr is always contributing unless open
            else ->
                false
        }
    }*/

    fun test(e: Active): Boolean {
        return when (fillType) {
            FillRule.EvenOdd, FillRule.NonZero -> (e.WindCnt2 == 0)
            FillRule.Positive -> (e.WindCnt2 <= 0)
            FillRule.Negative -> (e.WindCnt2 >= 0)
        }
    }

    private fun IsContributingOpen(e: Active): Boolean {
        when (clipType) {
            ClipType.Intersection -> return (e.WindCnt2 != 0)
            ClipType.Union -> return (e.WindCnt == 0 && e.WindCnt2 == 0)
            ClipType.Difference -> return (e.WindCnt2 == 0)
            ClipType.Xor -> return (e.WindCnt != 0) != (e.WindCnt2 != 0)
        }
        return false //stops compiler error
    }


    private fun SetWindingLeftEdgeOpen(e: Active) {
        var e2: Active? = Actives
        if (fillType == FillRule.EvenOdd) {
            var cnt1 = 0
            var cnt2 = 0
            while (e2 != e) {
                if (GetPathType(e2!!) == PathType.Clip) cnt2++
                else if (!IsOpen(e2)) cnt1++
                e2 = e2.NextInAEL
            }
            e.WindCnt = cnt1 % 2
            e.WindCnt2 = cnt2 % 2
        } else {
            //if FClipType in [ctUnion, ctDifference] then e.WindCnt := e.WindDx;
            while (e2 != e) {
                if (GetPathType(e2!!) == PathType.Clip) e.WindCnt2 += e2.WindDx
                else if (!IsOpen(e2)) e.WindCnt += e2.WindDx
                e2 = e2.NextInAEL
            }
        }
    }

    private fun SetWindingLeftEdgeClosed(leftE: Active) {
        //Wind counts generally er to polygon regions not edges, so here an edge's
        //WindCnt indicates the higher of the two wind counts of the regions touching
        //the edge. (Note also that adjacent region wind counts only ever differ
        //by one, and open paths have no meaningful wind directions or counts.)

        var e: Active? = leftE.PrevInAEL
        //find the nearest closed path edge of the same PathType in AEL (heading left)
        var pt = GetPathType(leftE)
        while (e != null && (GetPathType(e) != pt || IsOpen(e))) e = e.PrevInAEL!!

        if (e == null) {
            leftE.WindCnt = leftE.WindDx
            e = Actives
        } else if (fillType == FillRule.EvenOdd) {
            leftE.WindCnt = leftE.WindDx
            leftE.WindCnt2 = e.WindCnt2
            e = e.NextInAEL
        } else {
            //NonZero, Positive, or Negative filling here ...
            //if e's WindCnt is in the SAME direction as its WindDx, then e is either
            //an outer left or a hole right boundary, so leftE must be inside 'e'.
            //(neither e.WindCnt nor e.WindDx should ever be 0)
            if (e.WindCnt * e.WindDx < 0) {
                //opposite directions so leftE is outside 'e' ...
                if (abs(e.WindCnt) > 1) {
                    //outside prev poly but still inside another.
                    if (e.WindDx * leftE.WindDx < 0)
                    //reversing direction so use the same WC
                        leftE.WindCnt = e.WindCnt
                    else
                    //otherwise keep 'reducing' the WC by 1 (ie towards 0) ...
                        leftE.WindCnt = e.WindCnt + leftE.WindDx
                } else
                //now outside all polys of same PathType so set own WC ...
                    leftE.WindCnt = if (IsOpen(leftE))  1 else leftE.WindDx
            } else {
                //leftE must be inside 'e'
                if (e.WindDx * leftE.WindDx < 0)
                //reversing direction so use the same WC
                    leftE.WindCnt = e.WindCnt
                else
                //otherwise keep 'increasing' the WC by 1 (ie away from 0) ...
                    leftE.WindCnt = e.WindCnt + leftE.WindDx
            }
            leftE.WindCnt2 = e.WindCnt2
            e = e.NextInAEL //ie get ready to calc WindCnt2
        }

        //update WindCnt2 ...
        if (fillType == FillRule.EvenOdd)
            while (e != leftE) {
                if (GetPathType(e!!) != pt && !IsOpen(e))
                    leftE.WindCnt2 = if(leftE.WindCnt2 == 0)  1 else 0
                e = e.NextInAEL
            }
        else
            while (e != leftE) {
                if (GetPathType(e!!) != pt && !IsOpen(e))
                    leftE.WindCnt2 += e.WindDx
                e = e.NextInAEL
            }
    }

    private fun InsertEdgeIntoAEL(edge: Active, startEdge: Active?) {
        var startEdge = startEdge
        if (Actives == null) {
            edge.PrevInAEL = null
            edge.NextInAEL = null
            Actives = edge
        } else if (startEdge == null && E2InsertsBeforeE1(Actives!!, edge)) {
            edge.PrevInAEL = null
            edge.NextInAEL = Actives
            Actives!!.PrevInAEL = edge
            Actives = edge
        } else {
            if (startEdge == null) startEdge = Actives!!
            while (startEdge!!.NextInAEL != null &&
                !E2InsertsBeforeE1(startEdge.NextInAEL!!, edge)
            )
                startEdge = startEdge.NextInAEL

            edge.NextInAEL = startEdge.NextInAEL
            if (startEdge.NextInAEL != null)
                startEdge.NextInAEL!!.PrevInAEL = edge
            edge.PrevInAEL = startEdge
            startEdge.NextInAEL = edge
        }
    }
    //----------------------------------------------------------------------

    private fun MoveEdgeToFollowLeftInAEL(e: Active, eLeft: Active) {
        //extract first ...
        val aelPrev = e.PrevInAEL
        val aelNext = e.NextInAEL
        aelPrev!!.NextInAEL = aelNext
        if (aelNext != null) {
            aelNext.PrevInAEL = aelPrev
        }
        //now reinsert ...
        e.NextInAEL = eLeft.NextInAEL
        eLeft.NextInAEL!!.PrevInAEL = e
        e.PrevInAEL = eLeft
        eLeft.NextInAEL = e
    }

    //----------------------------------------------------------------------------
    private fun InsertLocalMinimaIntoAEL(BotY: Long) {
        var leftB: Active?
        var rightB: Active?

        var locMin: LocalMinima?
        //add any local minima at BotY ...
        while (PopLocalMinima(BotY).run { locMin = second; first }) {
            if ((locMin!!.Vertex!!.Flags and VertexFlags.OpenStart) > 0)
            {
                leftB = null
            }
            else
            {
                leftB = Active()
                leftB.Bot = locMin!!.Vertex!!.Pt
                leftB.Curr = leftB.Bot
                leftB.VertTop = locMin!!.Vertex!!.Prev //ie descending
                leftB.Top = leftB.VertTop!!.Pt
                leftB.WindDx = -1
                leftB.LocalMin = locMin
                SetDx(leftB)
            }

            if ((locMin!!.Vertex!!.Flags and VertexFlags.OpenEnd) > 0)
            {
                rightB = null
            }
            else
            {
                rightB = Active()
                rightB.Bot = locMin!!.Vertex!!.Pt
                rightB.Curr = rightB.Bot
                rightB.VertTop = locMin!!.Vertex!!.Next //ie ascending
                rightB.Top = rightB.VertTop!!.Pt
                rightB.WindDx = 1
                rightB.LocalMin = locMin
                SetDx(rightB)
            }

            //Currently LeftB is just the descending bound and RightB is the ascending.
            //Now if the LeftB isn't on the left of RightB then we need swap them.
            if (leftB != null && rightB != null) {
                if (IsHorizontal(leftB)) {
                    if (leftB.Top!!.x > leftB.Bot!!.x) leftB = rightB.also { rightB = leftB }
                } else if (IsHorizontal(rightB!!)) {
                    if (rightB!!.Top!!.x < rightB!!.Bot!!.x) leftB = rightB.also { rightB = leftB }
                } else if (leftB.Dx!! < rightB!!.Dx!!) leftB = rightB.also { rightB = leftB }
            } else if (leftB == null) {
                leftB = rightB
                rightB = null
            }

            InsertEdgeIntoAEL(leftB!!, null)      //insert left edge
            var contributing =if (IsOpen(leftB)) {
                SetWindingLeftEdgeOpen(leftB)
                IsContributingOpen(leftB)
            } else {
                SetWindingLeftEdgeClosed(leftB)
                IsContributingClosed(leftB)
            }

            if (rightB != null) {
                rightB!!.WindCnt = leftB.WindCnt
                rightB!!.WindCnt2 = leftB.WindCnt2
                InsertEdgeIntoAEL(rightB!!, leftB) //insert right edge
                if (contributing)
                    addLocalMinPoly(leftB, rightB!!, leftB.Bot!!)

                if (IsHorizontal(rightB!!))
                    PushHorz(rightB!!)
                else
                    InsertScanline(rightB!!.Top!!.y)
            } else if (contributing)
                StartOpenPath(leftB, leftB.Bot!!)

            if (IsHorizontal(leftB))
                PushHorz(leftB); else
                InsertScanline(leftB.Top!!.y)

            if (rightB != null && leftB.NextInAEL != rightB) {
                //intersect edges that are between left and right bounds ...
                var e: Active? = rightB!!.NextInAEL
                MoveEdgeToFollowLeftInAEL(rightB!!, leftB)
                while (rightB!!.NextInAEL != e) {
                    //nb: For calculating winding counts etc, IntersectEdges() assumes
                    //that rightB will be to the right of e ABOVE the intersection ...
                    IntersectEdges(rightB!!, rightB!!.NextInAEL!!, rightB!!.Bot!!)
                    SwapPositionsInAEL(rightB!!, rightB!!.NextInAEL!!)
                }
            }
        }
    }

    private fun SetOrientation( outRec: OutRec, e1: Active, e2: Active) {
        outRec.StartE = e1
        outRec.EndE = e2
        e1.OutRec = outRec
        e2.OutRec = outRec
    }

    private fun GetOwner(e: Active): OutRec? {
        var e: Active? = e
        return if (IsHorizontal(e!!) && e.Top!!.x < e.Bot!!.x) {
            e = e.NextInAEL
            while (e != null && (!IsHotEdge(e) || IsOpen(e)))
                e = e.NextInAEL
            if (e == null) null
            else if ((e.OutRec!!.Flag == OutrecFlag.Outer) == (e.OutRec!!.StartE == e))
                e.OutRec!!.Owner; else e.OutRec
        } else {
            e = e.PrevInAEL
            while (e != null && (!IsHotEdge(e) || IsOpen(e)))
                e = e.PrevInAEL
            if (e == null) null
            else if ((e.OutRec!!.Flag == OutrecFlag.Outer) == (e.OutRec!!.EndE == e))
                e.OutRec!!.Owner; else e.OutRec
        }
    }

    protected open fun addLocalMinPoly(e1: Active, e2: Active, pt: Point64) {
        var outRec = CreateOutRec()
        outRec.IDx = OutRecList.size
        OutRecList.add(outRec)
        outRec.Owner = GetOwner(e1)
        outRec.PolyPath = null

        if (IsOpen(e1)) {
            outRec.Owner = null
            outRec.Flag = OutrecFlag.Open
        } else if (outRec.Owner == null || (outRec.Owner!!.Flag == OutrecFlag.Inner))
            outRec.Flag = OutrecFlag.Outer
        else
            outRec.Flag = OutrecFlag.Inner

        //now set orientation ...
        var swapSideNeeded = false    //todo: recheck this with open paths
        if (IsHorizontal(e1)) {
            if (e1.Top!!.x > e1.Bot!!.x) swapSideNeeded = true
        } else if (IsHorizontal(e2)) {
            if (e2.Top!!.x < e2.Bot!!.x) swapSideNeeded = true
        } else if (e1.Dx!! < e2.Dx!!) swapSideNeeded = true
        if ((outRec.Flag == OutrecFlag.Inner) == swapSideNeeded)
            SetOrientation(outRec, e1, e2); else
            SetOrientation(outRec, e2, e1)

        var op = CreateOutPt()
        op.Pt = pt
        op.Next = op
        op.Prev = op
        outRec.Pts = op
    }

    private fun EndOutRec( outRec: OutRec) {
        outRec.StartE!!.OutRec = null
        if (outRec.EndE != null) outRec.EndE!!.OutRec = null
        outRec.StartE = null
        outRec.EndE = null
    }

    protected open fun addLocalMaxPoly(e1: Active, e2: Active, Pt: Point64) {
        if (!IsHotEdge(e2))
            throw ClipperException("Error in addLocalMaxPoly().")

        addOutPt(e1, Pt)
        if (e1.OutRec == e2.OutRec) EndOutRec(e1.OutRec!!)
        //and to preserve the winding orientation of Outrec ...
        else if (e1.OutRec!!.IDx!! < e2.OutRec!!.IDx!!)
            JoinOutrecPaths(e1, e2); else
            JoinOutrecPaths(e2, e1)
    }

    fun SwapSides(outrec: OutRec) {
        var e2: Active? = outrec.StartE
        outrec.StartE = outrec.EndE
        outrec.EndE = e2
        outrec.Pts = outrec.Pts!!.Next
    }

    fun FixOrientation(e: Active): Boolean {
        var result = true
        var e2: Active? = e
        while (e2!!.PrevInAEL != null) {
            e2 = e2.PrevInAEL
            if (e2!!.OutRec != null && !IsOpen(e2)) result = !result
        }
        if (result != IsStartSide(e)) {
            if (result) e.OutRec!!.Flag = OutrecFlag.Outer
            else e.OutRec!!.Flag = OutrecFlag.Inner
            SwapSides(e.OutRec!!)
            return true //all fixed
        } else return false //no fix needed
    }

    private fun JoinOutrecPaths(e1: Active, e2: Active) {
        if (IsStartSide(e1) == IsStartSide(e2)) {
            //one or other edge orientation is wrong...
            if (IsOpen(e1)) SwapSides(e2.OutRec!!)
            else if (!FixOrientation(e1) && !FixOrientation(e2))
                throw ClipperException("Error in JoinOutrecPaths")
            if (e1.OutRec!!.Owner == e2.OutRec)
                e1.OutRec!!.Owner = e2.OutRec!!.Owner
        }

        //join E2 outrec path onto E1 outrec path and then delete E2 outrec path
        //pointers. (nb: Only very rarely do the joining ends share the same coords.)
        var P1_st = e1.OutRec!!.Pts
        var P2_st = e2.OutRec!!.Pts
        var P1_end = P1_st!!.Next
        var P2_end = P2_st!!.Next
        if (IsStartSide(e1)) {
            P2_end!!.Prev = P1_st
            P1_st.Next = P2_end
            P2_st.Next = P1_end
            P1_end!!.Prev = P2_st
            e1.OutRec!!.Pts = P2_st
            e1.OutRec!!.StartE = e2.OutRec!!.StartE
            if (e1.OutRec!!.StartE != null) //ie closed path
                e1.OutRec!!.StartE!!.OutRec = e1.OutRec
        } else {
            P1_end!!.Prev = P2_st
            P2_st.Next = P1_end
            P1_st.Next = P2_end
            P2_end!!.Prev = P1_st
            e1.OutRec!!.EndE = e2.OutRec!!.EndE
            if (e1.OutRec!!.EndE != null) //ie closed path
                e1.OutRec!!.EndE!!.OutRec = e1.OutRec
        }

        e2.OutRec!!.StartE = null
        e2.OutRec!!.EndE = null
        e2.OutRec!!.Pts = null
        e2.OutRec!!.Owner = e1.OutRec //this may be redundant

        e1.OutRec = null
        e2.OutRec = null
    }

    private fun PushHorz(e: Active) {
        e.NextInSEL = if (SEL != null) SEL else null
        SEL = e
    }


    private fun PopHorz(): Pair<Boolean, Active?> {
        val result = SEL
        if (SEL == null) return false to null
        SEL = SEL!!.NextInSEL
        return true to result
    }

    private fun StartOpenPath(e: Active, pt: Point64) {
        var outRec = CreateOutRec()
        outRec.IDx = OutRecList.size
        OutRecList.add(outRec)
        outRec.Flag = OutrecFlag.Open
        e.OutRec = outRec

        var op = CreateOutPt()
        op.Pt = pt
        op.Next = op
        op.Prev = op
        outRec.Pts = op
    }

    private fun TerminateHotOpen(e: Active) {
        if (e.OutRec!!.StartE == e)
            e.OutRec!!.StartE = null; else
            e.OutRec!!.EndE = null
        e.OutRec = null
    }

    protected open fun CreateOutPt(): OutPt {
        //this is a open method as descendant classes may need
        //to produce descendant classes of OutPt ...
        return OutPt()
    }

    protected open fun CreateOutRec(): OutRec {
        //this is a open method as descendant classes may need
        //to produce descendant classes of OutRec ...
        return OutRec()
    }

    private fun SwapOutrecs(e1: Active, e2: Active) {
        var or1 = e1.OutRec
        var or2 = e2.OutRec
        if (or1 == or2) {
            val e: Active? = or1!!.StartE
            or1.StartE = or1.EndE
            or1.EndE = e
            return
        }
        if (or1 != null) {
            if (e1 == or1.StartE)
                or1.StartE = e2; else
                or1.EndE = e2
        }
        if (or2 != null) {
            if (e2 == or2.StartE)
                or2.StartE = e1; else
                or2.EndE = e1
        }
        e1.OutRec = or2
        e2.OutRec = or1
    }

    protected open fun addOutPt(e: Active, pt: Point64): OutPt {
        //Outrec.Pts: a circular Double-linked-list of POutPt.
        var toStart = IsStartSide(e)
        var opStart = e.OutRec!!.Pts
        var opEnd = opStart!!.Next
        if (toStart) {
            if (pt == opStart.Pt) return opStart
        } else if (pt == opEnd!!.Pt) return opEnd

        var opNew = CreateOutPt()
        opNew.Pt = pt
        opEnd!!.Prev = opNew
        opNew.Prev = opStart
        opNew.Next = opEnd
        opStart.Next = opNew
        if (toStart) e.OutRec!!.Pts = opNew
        return opNew
    }

    private fun UpdateEdgeIntoAEL(e: Active) {
        e.Bot = e.Top
        e.VertTop = NextVertex(e)
        e.Top = e.VertTop!!.Pt
        e.Curr = e.Bot
        SetDx(e)
        if (!IsHorizontal(e)) InsertScanline(e.Top!!.y)
    }

    private fun IntersectEdges(e1: Active, e2: Active, pt: Point64) {
        var e1 = e1
        var e2 = e2
        e1.Curr = pt
        e2.Curr = pt

        //if either edge is an OPEN path ...
        if (HasOpenPaths && (IsOpen(e1) || IsOpen(e2))) {
            if (IsOpen(e1) && IsOpen(e2)) return //ignore lines that intersect
            //the following line just afuns duplicating a whole lot of code ...
            if (IsOpen(e2)) e1 = e2.also { e2 = e1 }
            when (clipType) {
                ClipType.Intersection,
                ClipType.Difference -> if (IsSamePathType(e1, e2) || (abs(e2.WindCnt) != 1)) return

                ClipType.Union -> {
                    if (IsHotEdge(e1) != ((abs(e2.WindCnt) != 1) ||
                                (IsHotEdge(e1) != (e2.WindCnt2 != 0)))
                    )
                    return //just works!
                }

                ClipType.Xor ->
                    if (abs(e2.WindCnt) != 1)
                        return

            }
            //toggle contribution ...
            if (IsHotEdge(e1)) {
                addOutPt(e1, pt)
                TerminateHotOpen(e1)
            } else StartOpenPath(e1, pt)
            return
        }

        //update winding counts...
        //assumes that e1 will be to the right of e2 ABOVE the intersection
        var oldE1WindCnt: Int
        var oldE2WindCnt: Int
        if (e1.LocalMin!!.PathType == e2.LocalMin!!.PathType) {
            if (fillType == FillRule.EvenOdd) {
                oldE1WindCnt = e1.WindCnt
                e1.WindCnt = e2.WindCnt
                e2.WindCnt = oldE1WindCnt
            } else {
                if (e1.WindCnt + e2.WindDx == 0) e1.WindCnt = -e1.WindCnt
                else e1.WindCnt += e2.WindDx
                if (e2.WindCnt - e1.WindDx == 0) e2.WindCnt = -e2.WindCnt
                else e2.WindCnt -= e1.WindDx
            }
        } else {
            if (fillType != FillRule.EvenOdd) e1.WindCnt2 += e2.WindDx
            else e1.WindCnt2 = if (e1.WindCnt2 == 0) 1 else 0
            if (fillType != FillRule.EvenOdd) e2.WindCnt2 -= e1.WindDx
            else e2.WindCnt2 = if (e2.WindCnt2 == 0) 1 else 0
        }

        when (fillType) {
            FillRule.Positive -> {
                oldE1WindCnt = e1.WindCnt
                oldE2WindCnt = e2.WindCnt
            }

            FillRule.Negative
            -> {
                oldE1WindCnt = -e1.WindCnt
                oldE2WindCnt = -e2.WindCnt
            }

            else -> {
                oldE1WindCnt = abs(e1.WindCnt)
                oldE2WindCnt = abs(e2.WindCnt)
            }
        }

        if (IsHotEdge(e1) && IsHotEdge(e2)) {
            if ((oldE1WindCnt != 0 && oldE1WindCnt != 1) || (oldE2WindCnt != 0 && oldE2WindCnt != 1) ||
                (e1.LocalMin!!.PathType != e2.LocalMin!!.PathType && clipType != ClipType.Xor)
            ) {
                addLocalMaxPoly(e1, e2, pt)
            } else if (e1.OutRec == e2.OutRec) //optional
            {
                addLocalMaxPoly(e1, e2, pt)
                addLocalMinPoly(e1, e2, pt)
            } else {
                addOutPt(e1, pt)
                addOutPt(e2, pt)
                SwapOutrecs(e1, e2)
            }
        } else if (IsHotEdge(e1)) {
            if (oldE2WindCnt == 0 || oldE2WindCnt == 1) {
                addOutPt(e1, pt)
                SwapOutrecs(e1, e2)
            }
        } else if (IsHotEdge(e2)) {
            if (oldE1WindCnt == 0 || oldE1WindCnt == 1) {
                addOutPt(e2, pt)
                SwapOutrecs(e1, e2)
            }
        } else if ((oldE1WindCnt == 0 || oldE1WindCnt == 1) &&
            (oldE2WindCnt == 0 || oldE2WindCnt == 1)
        ) {
            //neither edge is currently contributing ...
            var e1Wc2: Long
            var e2Wc2: Long
            when (fillType) {
                FillRule.Positive -> {
                    e1Wc2 = e1.WindCnt2.toLong()
                    e2Wc2 = e2.WindCnt2.toLong()
                }

                FillRule.Negative -> {
                    e1Wc2 = -e1.WindCnt2.toLong()
                    e2Wc2 = -e2.WindCnt2.toLong()
                }
                else -> {
                    e1Wc2 = abs(e1.WindCnt2).toLong()
                    e2Wc2 = abs(e2.WindCnt2).toLong()
                }
            }

            if (e1.LocalMin!!.PathType != e2.LocalMin!!.PathType) {
                addLocalMinPoly(e1, e2, pt)
            } else if (oldE1WindCnt == 1 && oldE2WindCnt == 1)
                when (clipType) {
                    ClipType.Intersection ->
                        if (e1Wc2 > 0 && e2Wc2 > 0)
                            addLocalMinPoly(e1, e2, pt)

                    ClipType.Union ->
                        if (e1Wc2 <= 0 && e2Wc2 <= 0)
                            addLocalMinPoly(e1, e2, pt)

                    ClipType.Difference -> {
                        if (((GetPathType(e1) == PathType.Clip) && (e1Wc2 > 0) && (e2Wc2 > 0)) ||
                            ((GetPathType(e1) == PathType.Subject) && (e1Wc2 <= 0) && (e2Wc2 <= 0))
                        )
                            addLocalMinPoly(e1, e2, pt)
                    }
                    ClipType.Xor ->
                        addLocalMinPoly(e1, e2, pt)

                }
        }
    }

    private fun DeleteFromAEL(e: Active) {
        var AelPrev: Active? = e.PrevInAEL
        var AelNext: Active? = e.NextInAEL
        if (AelPrev == null && AelNext == null && (e != Actives))
            return //already deleted
        if (AelPrev != null) AelPrev.NextInAEL = AelNext
        else Actives = AelNext
        if (AelNext != null)
            AelNext.PrevInAEL = AelPrev
        e.NextInAEL = null
        e.PrevInAEL = null
    }

    private fun CopyAELToSEL() {
        var e: Active? = Actives
        SEL = e
        while (e != null) {
            e.PrevInSEL = e.PrevInAEL
            e.NextInSEL = e.NextInAEL
            e = e.NextInAEL
        }
    }

    private fun CopyActivesToSELAdjustCurrX(topY: Long) {
        var e: Active? = Actives
        SEL = e
        while (e != null) {
            e.PrevInSEL = e.PrevInAEL
            e.NextInSEL = e.NextInAEL
            e.Curr!!.x = TopX(e, topY)
            e = e.NextInAEL
        }
    }

    protected open fun ExecuteInternal(ct: ClipType, ft: FillRule): Boolean {
        if (ct == ClipType.None) return true
        fillType = ft
        clipType = ct
        Reset()
        var Y : Long?
        if (!PopScanline().run {Y = second; first }) return false

        while (true) /////////////////////////////////////////////
        {
            InsertLocalMinimaIntoAEL(Y!!)

            var e: Active?
            while (PopHorz().run {e = second; first }) ProcessHorizontal(e!!)

            if (!PopScanline().run {Y = second; first }) break   //Y is now at the top of the scanbeam
            ProcessIntersections(Y!!)
            SEL = null                       //SEL reused to flag horizontals
            DoTopOfScanbeam(Y!!)
        } ////////////////////////////////////////////////////////
        return true
    }

    open fun Execute(clipType: ClipType, Closed: Paths?, ft: FillRule = FillRule.EvenOdd): Boolean {
        try {
            if (Closed == null) return false
            Closed.clear()
            if (!ExecuteInternal(clipType, ft)) return false
            BuildResult(Closed, null)
            return true
        } finally {
            CleanUp(); }
    }

    open fun Execute(clipType: ClipType, Closed: Paths?, Open: Paths?, ft: FillRule = FillRule.EvenOdd): Boolean {
        try {
            if (Closed == null) return false
            Closed.clear()
            Open?.clear()
            if (!ExecuteInternal(clipType, ft)) return false
            BuildResult(Closed, Open)
            return true
        } finally {
            CleanUp(); }
    }

    open fun Execute(
        clipType: ClipType,
        polytree: PolyTree?,
        Open: Paths?,
        ft: FillRule = FillRule.EvenOdd
    ): Boolean {
        try {
            if (polytree == null) return false
            polytree.Clear()
            Open?.clear()
            if (!ExecuteInternal(clipType, ft)) return false
            BuildResult2(polytree, Open)
            return true
        } finally {
            CleanUp(); }
    }

    private fun ProcessIntersections(topY: Long) {
        BuildIntersectList(topY)
        if (IntersectList.size == 0) return
        try {
            FixupIntersectionOrder()
            ProcessIntersectList()
        } finally {
            IntersectList.clear() //clean up only needed if there's been an error
        }
    }

    private fun InsertNewIntersectNode(e1: Active, e2: Active, topY: Long) {
        val pt = GetIntersectPoint(e1, e2)

        //Rounding errors can occasionally place the calculated intersection
        //point either below or above the scanbeam, so check and correct ...
        if (pt.y > e1.Curr!!.y) {
            pt.y = e1.Curr!!.y      //E.Curr.Y is still the bottom of scanbeam
            //use the more vertical of the 2 edges to derive pt.X ...
            if (abs(e1.Dx!!) < abs(e2.Dx!!))
                pt.x = TopX(e1, pt.y)
            else
                pt.x = TopX(e2, pt.y)
        } else if (pt.y < topY) {
            pt.y = topY          //TopY = top of scanbeam

            if (e1.Top!!.y == topY) pt.x = e1.Top!!.x
            else if (e2.Top!!.y == topY) pt.x = e2.Top!!.x
            else if (abs(e1.Dx!!) < abs(e2.Dx!!)) pt.x = e1.Curr!!.x
            else pt.x = e2.Curr!!.x
        }

        val node = IntersectNode()
        node.Edge1 = e1
        node.Edge2 = e2
        node.Pt = pt
        IntersectList.add(node)
    }

    private fun BuildIntersectList(TopY: Long) {
        if (Actives == null || Actives!!.NextInAEL == null) return

        CopyActivesToSELAdjustCurrX(TopY)

        //Merge sort FActives into their positions at the top of scanbeam, and
        //create an intersection node every time an edge crosses over another ...

        var mul = 1
        while (true) {
            var first: Active? = SEL
            var second: Active?
            var baseE: Active?
            var prevBase: Active? = null
            var tmp: Active?

            //sort successive larger 'mul' count of nodes ...
            while (first != null) {
                if (mul == 1) {
                    second = first.NextInSEL
                    if (second == null) break
                    first.MergeJump = second.NextInSEL
                } else {
                    second = first.MergeJump
                    if (second == null) break
                    first.MergeJump = second.MergeJump
                }

                //now sort first and second groups ...
                baseE = first
                var lCnt = mul
                var rCnt = mul
                while (lCnt > 0 && rCnt > 0) {
                    if (second!!.Curr!!.x < first!!.Curr!!.x) {
                        // create one or more Intersect nodes ///////////
                        tmp = second.PrevInSEL
                        for (i in 0 until lCnt)
                        {
                            //create a intersect node...
                            InsertNewIntersectNode(tmp!!, second, TopY)
                            tmp = tmp.PrevInSEL
                        }
                        /////////////////////////////////////////////////

                        if (first == baseE) {
                            if (prevBase != null) prevBase.MergeJump = second
                            baseE = second
                            baseE.MergeJump = first.MergeJump
                            if (first.PrevInSEL == null) SEL = second
                        }
                        tmp = second.NextInSEL
                        //now move the out of place edge to it's position in SEL ...
                        Insert2Before1InSel(first, second)
                        second = tmp
                        if (second == null) break
                        --rCnt
                    } else {
                        first = first.NextInSEL
                        --lCnt
                    }
                }
                first = baseE!!.MergeJump
                prevBase = baseE
            }
            if (SEL!!.MergeJump == null) break
            else mul = mul shl 1
        }
    }

    private fun ProcessIntersectList() {
        for (iNode in IntersectList) {
            IntersectEdges(iNode.Edge1!!, iNode.Edge2!!, iNode.Pt!!)
            SwapPositionsInAEL(iNode.Edge1!!, iNode.Edge2!!)
        }
        IntersectList.clear()
    }

    private fun EdgesAdjacentInSEL(inode: IntersectNode): Boolean {
        return (inode.Edge1!!.NextInSEL == inode.Edge2) ||
                (inode.Edge1!!.PrevInSEL == inode.Edge2)
    }
//------------------------------------------------------------------------------

    private fun FixupIntersectionOrder() {
        val cnt = IntersectList.size

        if (cnt < 2) return
        //It's important that edge intersections are processed from the bottom up,
        //but it's also crucial that intersections only occur between adjacent edges.
        //The first sort here (a quicksort), arranges intersections relative to their
        //vertical positions within the scanbeam ...
        Collections.sort(IntersectList, IntersectNodeComparer)

        //Now we simulate processing these intersections, and as we do, we make sure
        //that the intersecting edges remain adjacent. If they aren't, this simulated
        //intersection is delayed until such time as these edges do become adjacent.
        CopyAELToSEL()
        for (i in 0 until cnt) {
            if (!EdgesAdjacentInSEL(IntersectList[i])) {
                var j = i +1
                while (!EdgesAdjacentInSEL(IntersectList[j])) {
                    j++
                }
                var tmp = IntersectList [i]
                IntersectList[i] = IntersectList[j]
                IntersectList[j] = tmp
            }
            SwapPositionsInSEL(IntersectList[i].Edge1!!, IntersectList[i].Edge2!!)
        }
    }

    fun SwapPositionsInAEL(e1: Active, e2: Active) {
        var next: Active?
        var prev: Active?
        if (e1.NextInAEL == e2) {
            next = e2.NextInAEL
            if (next != null) next.PrevInAEL = e1
            prev = e1.PrevInAEL
            if (prev != null) prev.NextInAEL = e2
            e2.PrevInAEL = prev
            e2.NextInAEL = e1
            e1.PrevInAEL = e2
            e1.NextInAEL = next
            if (e2.PrevInAEL == null) Actives = e2
        } else if (e2.NextInAEL == e1) {
            next = e1.NextInAEL
            if (next != null) next.PrevInAEL = e2
            prev = e2.PrevInAEL
            if (prev != null) prev.NextInAEL = e1
            e1.PrevInAEL = prev
            e1.NextInAEL = e2
            e2.PrevInAEL = e1
            e2.NextInAEL = next
            if (e1.PrevInAEL == null) Actives = e1
        } else
            throw ClipperException("Clipping error in SwapPositionsInAEL")
    }

    private fun SwapPositionsInSEL(e1: Active, e2: Active) {
        var next: Active?
        var prev: Active?
        if (e1.NextInSEL == e2) {
            next = e2.NextInSEL
            if (next != null) next.PrevInSEL = e1
            prev = e1.PrevInSEL
            if (prev != null) prev.NextInSEL = e2
            e2.PrevInSEL = prev
            e2.NextInSEL = e1
            e1.PrevInSEL = e2
            e1.NextInSEL = next
            if (e2.PrevInSEL == null) SEL = e2
        } else if (e2.NextInSEL == e1) {
            next = e1.NextInSEL
            if (next != null) next.PrevInSEL = e2
            prev = e2.PrevInSEL
            if (prev != null) prev.NextInSEL = e1
            e1.PrevInSEL = prev
            e1.NextInSEL = e2
            e2.PrevInSEL = e1
            e2.NextInSEL = next
            if (e1.PrevInSEL == null) SEL = e1
        } else
            throw ClipperException("Clipping error in SwapPositionsInSEL")
    }

    private fun Insert2Before1InSel(first: Active, second: Active) {
        //remove second from list ...
        var prev: Active? = second.PrevInSEL
        var next: Active? = second.NextInSEL
        prev!!.NextInSEL = next //always a prev since we're moving from right to left
        if (next != null) next.PrevInSEL = prev
        //insert back into list ...
        prev = first.PrevInSEL
        if (prev != null) prev.NextInSEL = second
        first.PrevInSEL = second
        second.PrevInSEL = prev
        second.NextInSEL = first
    }

    private fun ResetHorzDirection(
        horz: Active, maxPair: Active?
    ): Triple<Boolean, Long, Long> {
        var horzLeft: Long
        var horzRight: Long
        var result: Boolean
        if (horz.Bot!!.x == horz.Top!!.x) {
            //the horizontal edge is going nowhere ...
            horzLeft = horz.Curr!!.x
            horzRight = horz.Curr!!.x
            var e: Active? = horz.NextInAEL
            while (e != null && e != maxPair) e = e.NextInAEL
            result = e != null
        } else if (horz.Curr!!.x < horz.Top!!.x) {
            horzLeft = horz.Curr!!.x
            horzRight = horz.Top!!.x
            result = true
        } else {
            horzLeft = horz.Top!!.x
            horzRight = horz.Curr!!.x
            result = false //right to left
        }

        return Triple(result, horzLeft, horzRight)
    }
    //------------------------------------------------------------------------

    private fun ProcessHorizontal(horz: Active)
            /*******************************************************************************
             * Notes: Horizontal edges (HEs) at scanline intersections (ie at the top or    *
             * bottom of a scanbeam) are processed as if layered.The order in which HEs     *
             * are processed doesn't matter. HEs intersect with the bottom vertices of      *
             * other HEs[#] and with non-horizontal edges [*]. Once these intersections     *
             * are completed, intermediate HEs are 'promoted' to the next edge in their     *
             * bounds, and they in turn may be intersected[%] by other HEs.                 *
             *                                                                              *
             * eg: 3 horizontals at a scanline:    /   |                     /           /  *
             *              |                     /    |     (HE3)o ========%========== o   *
             *              o ======= o(HE2)     /     |         /         /                *
             *          o ============#=========*======*========#=========o (HE1)           *
             *         /              |        /       |       /                            *
             *******************************************************************************/
    {
        var pt: Point64
        //with closed paths, simplify consecutive horizontals into a 'single' edge ...
        if (!IsOpen(horz)) {
            pt = horz.Bot!!
            while (!IsMaxima(horz) && NextVertex(horz)!!.Pt.y == pt.y)
                UpdateEdgeIntoAEL(horz)
            horz.Bot = pt
            horz.Curr = pt
        }

        var maxPair: Active? = null
        if (IsMaxima(horz) && (!IsOpen(horz) ||
                    ((horz.VertTop!!.Flags and (VertexFlags.OpenStart or VertexFlags.OpenEnd)) == 0)))
        maxPair = GetMaximaPair(horz)

        var horzLeft : Long
        var horzRight : Long

        var isLeftToRight = ResetHorzDirection(horz, maxPair).run {
            horzLeft = second
            horzRight = third
            first
        }

        if (IsHotEdge(horz)) addOutPt(horz, horz.Curr!!)

        while (true) //loops through consec. horizontal edges (if open)
        {
            val isMax = IsMaxima (horz)
            var e = if (isLeftToRight)
                horz.NextInAEL
            else
                horz.PrevInAEL

            while (e != null) {
                //break if we've gone past the } of the horizontal ...
                if ((isLeftToRight && (e.Curr!!.x > horzRight)) ||
                    (!isLeftToRight && (e.Curr!!.x < horzLeft))
                ) break
                //or if we've got to the } of an intermediate horizontal edge ...
                if (e.Curr!!.x == horz.Top!!.x && !isMax && !IsHorizontal(e)) {
                    pt = NextVertex(horz)!!.Pt
                    if (isLeftToRight && (TopX(e, pt.y) >= pt.x) ||
                        (!isLeftToRight && (TopX(e, pt.y) <= pt.x))
                    ) break
                }

                if (e == maxPair) {
                    if (IsHotEdge(horz))
                        addLocalMaxPoly(horz, e, horz.Top!!)
                    DeleteFromAEL(e)
                    DeleteFromAEL(horz)
                    return
                }

                if (isLeftToRight) {
                    pt = Point64(e.Curr!!.x, horz.Curr!!.y)
                    IntersectEdges(horz, e, pt)
                } else {
                    pt = Point64(e.Curr!!.x, horz.Curr!!.y)
                    IntersectEdges(e, horz, pt)
                }

                var eNext = if (isLeftToRight) e.NextInAEL; else e.PrevInAEL
                SwapPositionsInAEL(horz, e)
                e = eNext
            }

            //check if we've finished with (consecutive) horizontals ...
            if (isMax || NextVertex(horz)!!.Pt.y != horz.Top!!.y) break

            //still more horizontals in bound to process ...
            UpdateEdgeIntoAEL(horz)

            isLeftToRight = ResetHorzDirection(horz, maxPair).run {
                horzLeft = second
                horzRight = third
                first
            }

            if (IsOpen(horz)) {
                if (IsMaxima(horz)) maxPair = GetMaximaPair(horz)
                if (IsHotEdge(horz)) addOutPt(horz, horz.Bot!!)
            }
        }

        if (IsHotEdge(horz)) addOutPt(horz, horz.Top!!)

        if (!IsOpen(horz))
            UpdateEdgeIntoAEL(horz) //this is the } of an intermediate horiz.
        else if (!IsMaxima(horz))
            UpdateEdgeIntoAEL(horz)
        else if (maxPair == null)      //ie open at top
            DeleteFromAEL(horz)
        else if (IsHotEdge(horz))
            addLocalMaxPoly(horz, maxPair, horz.Top!!)
        else {
            DeleteFromAEL(maxPair); DeleteFromAEL(horz); }

    }

    private fun DoTopOfScanbeam(Y: Long) {
        var e = Actives
        while (e != null) {
            //nb: E will never be horizontal at this point
            if (e.Top!!.y == Y) {
                e.Curr = e.Top //needed for horizontal processing
                if (IsMaxima(e)) {
                    e = DoMaxima(e) //TOP OF BOUND (MAXIMA)
                    continue
                } else {
                    //INTERMEDIATE VERTEX ...
                    UpdateEdgeIntoAEL(e)
                    if (IsHotEdge(e)) addOutPt(e, e.Bot!!)
                    if (IsHorizontal(e))
                        PushHorz(e) //horizontals are processed later
                }
            } else {
                e.Curr!!.y = Y
                e.Curr!!.x = TopX(e, Y)
            }
            e = e.NextInAEL
        }
    }

    private fun DoMaxima(e: Active): Active? {
        var eMaxPair: Active?
        var ePrev = e.PrevInAEL
        var eNext = e.NextInAEL
        if (IsOpen(e) && ((e.VertTop!!.Flags and (VertexFlags.OpenStart or VertexFlags.OpenEnd)) != 0)) {
            if (IsHotEdge(e)) addOutPt(e, e.Top!!)
            if (!IsHorizontal(e)) {
                if (IsHotEdge(e)) TerminateHotOpen(e)
                DeleteFromAEL(e)
            }
            return eNext
        } else {
            eMaxPair = GetMaximaPair(e)
            if (eMaxPair == null) return eNext //eMaxPair is horizontal
        }

        //only non-horizontal maxima here.
        //process any edges between maxima pair ...
        while (eNext != eMaxPair) {
            IntersectEdges(e, eNext!!, e.Top!!)
            SwapPositionsInAEL(e, eNext)
            eNext = e.NextInAEL
        }

        if (IsOpen(e)) {
            if (IsHotEdge(e)) {
                if (eMaxPair != null)
                    addLocalMaxPoly(e, eMaxPair, e.Top!!); else
                    addOutPt(e, e.Top!!)
            }
            if (eMaxPair != null)
                DeleteFromAEL(eMaxPair)
            DeleteFromAEL(e)
            return if(ePrev != null) ePrev.NextInAEL else Actives
        }
        //here E.NextInAEL == ENext == EMaxPair ...
        if (IsHotEdge(e))
            addLocalMaxPoly(e, eMaxPair, e.Top!!)

        DeleteFromAEL(e)
        DeleteFromAEL(eMaxPair)
        return if(ePrev != null)  ePrev.NextInAEL else Actives
    }

    private fun PointCount(op: OutPt?): Int {
        if (op == null) return 0
        var p = op
        var cnt = 0
        do {
            cnt++
            p = p!!.Next
        } while (p != op)
        return cnt
    }

    private fun BuildResult(closedPaths: Paths, openPaths: Paths?) {
        closedPaths.clear()
        openPaths?.clear()

        for (outrec in OutRecList) {
            if (outrec.Pts != null) {
                var op = outrec.Pts!!.Next
                var cnt = PointCount (op!!)
                //fixup for duplicate start and } points ...
                if (op.Pt == outrec.Pts!!.Pt) cnt--

                if (outrec.Flag == OutrecFlag.Open) {
                    if (cnt < 2 || openPaths == null) continue
                    val p = Path (cnt)
                    for (i in 0 until cnt) { p.add(op!!.Pt!!); op = op.Next; }
                    openPaths.add(p)
                } else {
                    if (cnt < 3) continue
                    val p = Path (cnt)
                    for ( i in 0 until cnt) { p.add(op!!.Pt!!); op = op.Next; }
                    closedPaths.add(p)
                }
            }
        }
    }

    private fun BuildResult2(pt: PolyTree?, openPaths: Paths?) {
        if (pt == null) return
        openPaths?.clear()

        for (outrec in OutRecList) {
            if (outrec.Pts != null) {
                var op = outrec.Pts!!.Next
                        var cnt = PointCount(op!!)
                //fixup for duplicate start and end points ...
                if (op.Pt == outrec.Pts!!.Pt) cnt--

                if (cnt < 3) {
                    if (outrec.Flag == OutrecFlag.Open || cnt < 2) continue
                }

                var p = Path (cnt)
                for (i in 0 until cnt) { p.add(op!!.Pt!!); op = op.Prev; }
                if (outrec.Flag == OutrecFlag.Open)
                    openPaths?.add(p)
                else {
                    if (outrec.Owner != null && outrec.Owner!!.PolyPath != null)
                        outrec.PolyPath = outrec.Owner!!.PolyPath!!.addChild(p)
                    else
                        outrec.PolyPath = pt.addChild(p)
                }
            }
        }
    }

    companion object {
        val horizontal = Double.NEGATIVE_INFINITY

        fun Round(value: Double): Long {
            return if (value < 0) (value - 0.5).toLong() else (value + 0.5).toLong()
        }

        private fun IntersectNodeSort(node1: IntersectNode, node2: IntersectNode): Int {
            //the following typecast should be safe because the differences in Pt.Y will
            //be limited to the height of the Scanline ...
            return (node2.Pt!!.y - node1.Pt!!.y) as Int
        }

        private fun TopX(edge: Active, currentY: Long): Long {
            if (currentY == edge.Top!!.y)
                return edge.Top!!.x
            return edge.Bot!!.x + Round(edge.Dx!! * (currentY - edge.Bot!!.y))
        }

        fun IsHorizontal(e: Active): Boolean {
            return e.Dx == horizontal
        }

        fun IsOpen(e: Active): Boolean {
            return e.LocalMin!!.IsOpen
        }
    }
} //Clipper

class ClipperException(message: String) : Exception(message)
//------------------------------------------------------------------------------
