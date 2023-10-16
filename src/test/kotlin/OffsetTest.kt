import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Line
import com.codecad.core.sketch.*
import org.junit.jupiter.api.Test
import java.awt.BasicStroke
import java.awt.Color
import java.awt.geom.Arc2D
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.image.BufferedImage
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import javax.imageio.ImageIO


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

        val offsetFace = offsetFace(sketchFace, -0.1)

        for( bound in offsetFace.bounds ){
            val edges = bound.loop.map { it.edge.edge }
            val cutEdges = cutLines(edges)

            println(cutEdges)
        }

        println(offsetFace)
    }

    @Test
    fun hourGlass(){
        val topLeft = Vertex(Vec2(-0.8, 1.0))
        val bottomLeft = Vertex(Vec2(-0.8, -1.0))
        val extraLeft = Vertex(Vec2(-0.2, -1.0))
        val topMid = Vertex(Vec2(0.4, 0.4))
        val bottomMid = Vertex(Vec2(-0.79, -0.4))
        val bottomRight = Vertex(Vec2(0.8, -1.0))
        val topRight = Vertex(Vec2(0.8, 1.0))

        val loop = Loop.wireCircular(topLeft, bottomLeft, extraLeft, bottomMid, bottomRight, topRight, topMid)
        val sketchFace = SketchFace(listOf(FaceBound(loop, FaceBoundKind.OuterBound)))


        val printer = DebugPrinter(4096)
        val rawEdges = loop.map { it.edge.edge }
        printer.add(rawEdges, Color.GREEN)
        //printer.add(testFaces)


        for( i in 1 until 25 ){
            val offset = -i / 25.0
            val result = offsetFace(sketchFace, offset)
            printer.add(result, Color.WHITE)
        }

        printer.finish()
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


        val printer = DebugPrinter(4096)
        val rawEdges = loop.map { it.edge.edge }
        //printer.add(rawEdges, Color.GREEN)
        //printer.add(testFaces)
/*
        for( i in 1 until 20 ){
            val offset = i / 40.0
            val result = offsetFace(sketchFace, offset)
            printer.add(result, Color.WHITE)
        }*/

        val i = 6
        val x = offsetFace(sketchFace, i / 40.0)
        val y = offsetFace(x, -i / 40.0)
        printer.add(x)
        printer.add(y)

        printer.finish()
    }

}