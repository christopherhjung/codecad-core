package clipper2

import java.util.*

open class PolyPath {
    var parent: PolyPath? = null
    var childs: MutableList<PolyPath> = ArrayList()
    var path = Path()

    //-----------------------------------------------------
    private fun IsHoleNode(): Boolean {
        var result = true
        var node = parent
        while (node != null) {
            result = !result
            node = node.parent
        }
        return result
    }

    //-----------------------------------------------------
    fun addChild(p: Path): PolyPath {
        val child = PolyPath()
        child.parent = this
        child.path = p
        childs.add(child)
        return child
    }

    //-----------------------------------------------------
    fun Clear() {
        childs.clear()
    }

    //-----------------------------------------------------
    fun PolyTreeToPaths(): Paths {
        val paths = Paths()
        AddPolyNodeToPaths(this, paths)
        return paths
    }

    //-----------------------------------------------------
    val isHole: Boolean
        get() = IsHoleNode()
    val childCount: Int
        get() = childs.size

    companion object {
        //the following two methods are really only for debugging ...
        private fun AddPolyNodeToPaths(pp: PolyPath, paths: Paths) {
            val cnt = pp.path.size
            if (cnt > 0) {
                val p = Path(cnt)
                for (ip in pp.path) {
                    p.add(ip)
                }
                paths.add(p)
            }
            for (polyp in pp.childs) AddPolyNodeToPaths(polyp, paths)
        }
    }
}

class PolyTree : PolyPath()
