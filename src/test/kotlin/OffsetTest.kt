import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Line
import com.codecad.core.sketch.*
import org.junit.jupiter.api.Test
import java.awt.Color
import java.util.*
import java.util.Collections.emptyList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.ceil


class OffsetTest {



    @Test
    fun importTest(){
        val topLeft = Vertex(Vec2(-1.0, 1.0))
        val bottomLeft = Vertex(Vec2(-1.0, -1.0))
        val bottomRight = Vertex(Vec2(1.0, -1.0))
        val topRight = Vertex(Vec2(1.0, 1.0))

        val left = Edge.line(topLeft, bottomLeft)
        val bottom = Edge.line(bottomLeft, bottomRight)
        val right = Edge.line(bottomRight, topRight)
        val top = Edge.line(topRight, topLeft)

        val loop = Loop.wireCircular(left, bottom, right, top)
        val sketchFace = SketchFace(listOf(FaceBound(loop, FaceBoundKind.OuterBound)))

        val offsetFace = offsetFace(sketchFace, -0.1)

        println(offsetFace)
    }

    @Test
    fun importTest2(){
        val topLeft = Vertex(Vec2(-1.0, 1.0))
        val bottomLeft = Vertex(Vec2(-1.0, -1.0))
        val bottomRight = Vertex(Vec2(1.0, -1.0))
        val topRight = Vertex(Vec2(1.0, 1.0))

        val leftArcWp = Workplane(Vec2(-1.0, 0.0), Vec2.DirY, Vec2.DirX)
        val rightArcWp = Workplane(Vec2(1.0, 0.0), Vec2.DirY, Vec2.DirX)

        val left = Edge.arc(leftArcWp, topLeft, bottomLeft, Sense.Same)
        val bottom = Edge.line(bottomLeft, bottomRight)
        val right = Edge.arc(rightArcWp, bottomRight, topRight, Sense.Same)
        val top = Edge.line(topRight, topLeft)

        val loop = Loop.wireCircular(left, bottom, right, top)
        val sketchFace = SketchFace(listOf(FaceBound(loop, FaceBoundKind.OuterBound)))

        val offsetFace = offsetFace(sketchFace, -0.1).toSketchFace()

        for( bound in offsetFace.bounds ){
            val edges = bound.loop.map { it.edge.edge }
            val cutEdges = cutLines(edges)

            println(cutEdges)
        }

        println(offsetFace)
    }

    @Test
    fun hourGlass(){
        val topLeft = Vertex(Vec2(-0.8, 1.0) * 100.0)
        val bottomLeft = Vertex(Vec2(-0.8, -1.0) * 100.0)
        val extraLeft = Vertex(Vec2(-0.2, -1.0) * 100.0)
        val topMid = Vertex(Vec2(0.4, 0.4) * 100.0)
        val bottomMid = Vertex(Vec2(-0.79, -0.4) * 100.0)
        val bottomRight = Vertex(Vec2(0.8, -1.0) * 100.0)
        val topRight = Vertex(Vec2(0.8, 1.0) * 100.0)

        val loop = Loop.wireCircular(topLeft, bottomLeft, extraLeft, bottomMid, bottomRight, topRight, topMid)
        val loop2 = Loop.wireCircular(Edge.circle(Vec2(0.0, 0.0), 0.2 * 100.0, Sense.Opposite))
        val sketchFace = SketchFace(listOf(
            FaceBound(loop, FaceBoundKind.OuterBound),
            FaceBound(loop2, FaceBoundKind.InnerBound)
        ))

        val tree = buildPathTree(sketchFace, -3.0, -4.0,0.5)
        val gen = ProgramGenerator()
        val gcode = gen.generate(tree, 0.0, 1.0, -10.0, 2.0)

        println(gcode)
        val printer = DebugPrinter(4096, 1/50.0)
        printer.add(sketchFace, Color.GREEN)
        tree.print(printer)
        /*
        //tree.print(printer)
        val x = offsetFace(offsetFace(sketchFace, -31.5).toSketchFace(), 0.5).toSketchFace()
        val x2 = SketchFace(x.bounds.filter { it.loop.computeAreaVec2() < 10.0 })
        printer.add(x2)
        printer.add(offsetFace(x2, -1.0).toSketchFace())
        printer.add(offsetFace(x2, -2.0).toSketchFace())*/
        printer.finish()
    }

    fun gen(face: SketchFace) : String{
        val tree = buildPathTree(face, -0.05, 0.01,0.01)
        var gen = ProgramGenerator()
        return gen.generate(tree, 1.0, 0.0, 0.0, 0.1)
    }
    fun genOut(face: SketchFace) : String{
        val firstOffset = offsetFace(face, 0.1)

        val trees = ArrayList<PathTree>()
        for( offsetBound in firstOffset.children ){
            val children = ArrayList<PathTree>()
            val tree = BoundedPathTree(offsetBound, children)
            trees.add(tree)
        }

        val tree = if(trees.size == 1){
            trees.first()
        }else{
            PathTree(trees)
        }

        var gen = ProgramGenerator()
        return gen.generate(tree, 1.0, 0.0, 0.0, 0.1)
    }

    open class PathTree(val children : List<PathTree> = emptyList()){
        open fun print(printer: DebugPrinter){
            children.forEach { it.print(printer) }
        }
        fun collect() : List<FaceBound<Vec2>>{
            val list = arrayListOf<FaceBound<Vec2>>()
            collect(list)
            return list
        }

        protected open fun collect(list: MutableList<FaceBound<Vec2>>){
            children.forEach { it.collect(list) }
        }
    }

    class BoundedPathTree(val tree : BoundedFaceTree, children : List<PathTree> = emptyList()) : PathTree(children)
    {
        override fun print(printer: DebugPrinter) {
            printer.add(tree.toSketchFace())
            super.print(printer)
        }
        override fun collect(list: MutableList<FaceBound<Vec2>>){
            super.collect(list)
            list.add(tree.bound)
            tree.children.forEach{list.add(it.bound)}
        }
    }


    fun smoothOffset(face : SketchFace, offset: Double, factor: Double) : FaceTree{
        val first = offsetFace(face, offset + factor).toSketchFace()
        return offsetFace(first, -factor)
    }

    private fun buildPathTree(face : SketchFace, offset: Double, nextOffset: Double, round : Double) : PathTree{
        val firstOffset = smoothOffset(face, offset, -round)

        val trees = ArrayList<PathTree>()
        for( offsetBound in firstOffset.children ){
            val children = ArrayList<PathTree>()
            val tree = BoundedPathTree(offsetBound, children)
            children.add(buildPathTree(offsetBound.toSketchFace(), nextOffset, nextOffset, round))
            trees.add(tree)
        }

        if(trees.size == 1){
            return trees.first()
        }

        return PathTree(trees)
    }

    abstract class Command{
        var feed: Double? = null

        fun Double.format(scale: Int) = "%.${scale}f".format(Locale.US, this)

        abstract fun gcode(prev : Vec2) : String
    }

    class LineCommand(val target: Vec2, val depth: Double, val fast: Boolean) : Command(){
        override fun gcode(prev : Vec2): String {
            val code = if(fast){
                "G0"
            }else{
                "G1"
            }

            return "${code} X${target.x.format(4)} Y${target.y.format(4)} Z${depth.format(4)}"
        }
    }
    class HelixCommand(val center: Vec2, val end: Vec2, val depth: Double, val sense : Sense) : Command(){
        override fun gcode(prev : Vec2): String {
            val code = if(sense == Sense.Same){
                "G3"
            }else{
                "G2"
            }

            return "$code X${end.x.format(4)} Y${end.y.format(4)} Z${depth.format(4)} I${center.x.format(4)} J${center.y.format(4)}"
        }
    }

    class Program{
        private val commands = arrayListOf<Command>()

        fun moveTo(target: Vec2, z : Double, fast: Boolean = false){
            commands.add(LineCommand(target, z, fast))
        }

        fun helixTo(center: Vec2, end: Vec2, z : Double, sense: Sense){
            commands.add(HelixCommand(center, end, z, sense))
        }

        fun gcode() : String{
            val sb = StringBuilder()
            for(commend in commands){
                sb.append(commend.gcode(Vec2.Zero)).append("\n")
            }
            return sb.toString()
        }
    }

    class ProgramGenerator(){
        private val program = Program()

        fun generate(pathTree: PathTree, startHeight: Double, securityDistance : Double, endHeight: Double, increment: Double) : String{
            val bounds = pathTree.collect()
            assert(endHeight <= startHeight)

            val approachingHeight = startHeight + securityDistance
            val depth = abs(endHeight - startHeight)
            val cuts = ceil(depth / increment).toInt()

            for( bound in bounds ){
                val currentLoop = bound.loop
                val startEdge = currentLoop.edge.normalized()
                val startPoint = startEdge.bound.start.point

                program.moveTo(startPoint, approachingHeight, true)
                program.moveTo(startPoint, startHeight, true)

                for(cut in 0 until cuts){
                    val cutDepth = startHeight - cut * increment
                    generateHelix(currentLoop, cutDepth, increment)
                }

                generate(currentLoop, endHeight)
                program.moveTo(startPoint, approachingHeight, true)
            }

            return program.gcode()
        }

        fun generateHelix(currentLoop: Loop<Vec2>, startZ: Double, increment: Double) : Double{
            var currentZ = startZ
            val lengthFactor = increment / currentLoop.length() //desired depth

            for(loop in currentLoop){
                loop.edge.let {
                    val offset = it.edge.length() * lengthFactor
                    currentZ -= offset
                    generate(it, currentZ)
                }
            }

            return currentZ
        }

        fun generate(currentLoop: Loop<Vec2>, currentZ: Double){
            for(loop in currentLoop){
                generate(loop.edge, currentZ)
            }
        }

        fun generate(edge: OrientedEdge<Vec2>, depth : Double){
            val edge = edge.normalized()
            val start = edge.bound.start.point
            val end = edge.bound.end.point
            when(val curve = edge.curve){
                is Line -> program.moveTo(end, depth)
                is Circle ->  program.helixTo(curve.workplane.origin - start, end, depth, edge.bound.sense)
            }
        }
    }


    @Test
    fun outInside(){
        val a = Vertex(Vec2(0.0, 0.0))
        val b = Vertex(Vec2(1.0, 0.0))
        val c = Vertex(Vec2(1.0, 0.8))
        val d = Vertex(Vec2(0.9, 0.1))
        val e = Vertex(Vec2(0.1, 0.1))
        val f = Vertex(Vec2(0.1, 0.9))
        val g = Vertex(Vec2(0.8, 1.0))
        val h = Vertex(Vec2(0.0, 1.0))

        val loop = Loop.wireCircular(a,b,c,d,e,f,g,h)
        val sketchFace = SketchFace(listOf(FaceBound(loop, FaceBoundKind.OuterBound)))

        val code = gen(sketchFace)

        println(code)

    }
    @Test
    fun rect(){
        val a = Vertex(Vec2(0.0, 0.0))
        val b = Vertex(Vec2(1.0, 0.0))
        val c = Vertex(Vec2(1.0, 1.0))
        val d = Vertex(Vec2(0.0, 1.0))

        val loop = Loop.wireCircular(a,b,c,d)
        val sketchFace = SketchFace(listOf(FaceBound(loop, FaceBoundKind.OuterBound)))

        val code = genOut(sketchFace)

        println(code)

    }

}