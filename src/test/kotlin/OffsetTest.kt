import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.*
import com.codecad.core.debug.DebugPrinter
import com.codecad.core.gcode.ProgramGenerator
import com.codecad.core.sketch.*
import com.codecad.core.toolpath.BoundedContourTree
import com.codecad.core.toolpath.ContourTree
import com.codecad.core.toolpath.buildContourTree
import org.junit.jupiter.api.Test
import java.awt.Color
import java.util.*

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

        val tree = buildContourTree(sketchFace, -4.0, -3.0,0.5)
        val gen = ProgramGenerator()
        val gcode = gen.generate(tree, 0.0, 1.0, -10.0, 2.0)

        println(gcode)
        val printer = DebugPrinter(4096, 1/50.0)
        printer.add(sketchFace, Color.GREEN)
        tree.print(printer)
        printer.finish()
    }

    fun gen(face: SketchFace) : String{
        val tree = buildContourTree(face, -0.05, 0.01,0.01)
        var gen = ProgramGenerator()
        return gen.generate(tree, 1.0, 0.0, 0.0, 0.1)
    }


    fun genOut(face: SketchFace) : String{
        val firstOffset = offsetFace(face, 0.1)

        val trees = ArrayList<ContourTree>()
        for( offsetBound in firstOffset.children ){
            val children = ArrayList<ContourTree>()
            val tree = BoundedContourTree(offsetBound, children)
            trees.add(tree)
        }

        val tree = if(trees.size == 1){
            trees.first()
        }else{
            ContourTree(trees)
        }

        var gen = ProgramGenerator()
        return gen.generate(tree, 1.0, 0.0, 0.0, 0.1)
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