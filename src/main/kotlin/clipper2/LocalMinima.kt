package clipper2


class LocalMinima {
    var Vertex: Vertex? = null
    var PathType: PathType? = null
    var IsOpen: Boolean = false
}


class Active {
    var Bot: Point64? = null
    var Curr: Point64? = null       //current (updated for every new Scanline)
    var Top: Point64? = null
    var Dx: Double? = null
    var WindDx: Int = 0  //wind direction (ascending: +1; descending: -1)
    var WindCnt: Int = 0//current wind count
    var WindCnt2: Int = 0//current wind count of opposite TPathType
    var OutRec: OutRec? = null
    var NextInAEL: Active? = null
    var PrevInAEL: Active? = null
    var NextInSEL: Active? = null
    var PrevInSEL: Active? = null
    var MergeJump: Active? = null
    var VertTop: Vertex? = null
    var LocalMin: LocalMinima? = null
}

class ScanLine {
    var Y: Long? = null
    var Next: ScanLine? = null
}

class OutPt {
    var Pt: Point64? = null
    var Next: OutPt? = null
    var Prev: OutPt? = null
}

enum class OutrecFlag { Inner, Outer, Open }

//OutRec: contains a path in the clipping solution. Edges in the AEL will
//carry a pointer to an OutRec when they are part of the clipping solution.
class OutRec {
    var IDx: Int? = null
    var Owner: OutRec? = null
    var StartE: Active? = null
    var EndE: Active? = null
    var Pts: OutPt? = null
    var PolyPath: PolyPath? = null
    var Flag: OutrecFlag? = null
}

class IntersectNode {
    var Edge1: Active? = null
    var Edge2: Active? = null
    var Pt: Point64? = null
}

class MyIntersectNodeSort : Comparator<IntersectNode> {
    override fun compare(node1: IntersectNode?, node2: IntersectNode?): Int {
        return node2!!.Pt!!.Y.compareTo(node1!!.Pt!!.Y) //descending soft
    }
}

class MyLocalMinSort : Comparator<LocalMinima> {
    override fun compare(lm1: LocalMinima?, lm2: LocalMinima?): Int {
        return lm2!!.Vertex!!.Pt.Y.compareTo(lm1!!.Vertex!!.Pt.Y) //descending soft
    }
}
