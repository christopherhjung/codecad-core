import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Line
import com.codecad.core.sketch.*
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import javax.imageio.ImageIO


class OffsetTest {



    @Test
    fun importTest(){
        /*val workplane = Workplane(Vec3.Zero, Vec3.DirectionZ, Vec3.DirectionX)
        val face = VolumeSuite.roundedPlane(workplane, 50.0, 5.0)
        offsetFace(face, 1.0)*/

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
        /*val workplane = Workplane(Vec3.Zero, Vec3.DirectionZ, Vec3.DirectionX)
        val face = VolumeSuite.roundedPlane(workplane, 50.0, 5.0)
        offsetFace(face, 1.0)*/

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

    fun offsetFaceFull(sketchFace : SketchFace, offset: Double) : SketchFace{
        val rawOffsetFace = offsetFace(sketchFace, offset)
        val edges = rawOffsetFace.bounds.flatMap { bound -> bound.loop.map { it.edge.edge } }
        val cutEdges = cutLines(edges)
        val loops = connectVerticesMirrored(cutEdges)
        val faces = generateFaces(loops)
        return SketchFace(faces.bounds.filter { it.loop.computeAreaVec2() > 0.0 })
    }

    @Test
    fun hourGlass(){
        /*val workplane = Workplane(Vec3.Zero, Vec3.DirectionZ, Vec3.DirectionX)
        val face = VolumeSuite.roundedPlane(workplane, 50.0, 5.0)
        offsetFace(face, 1.0)*/

        val topLeft = Vertex(Vec2(-1.0, 1.0))
        val bottomLeft = Vertex(Vec2(-1.0, -1.0))
        val topMid = Vertex(Vec2(0.0, 0.5))
        val bottomMid = Vertex(Vec2(0.0, -0.5))
        val bottomRight = Vertex(Vec2(1.0, -1.0))
        val topRight = Vertex(Vec2(1.0, 1.0))

        val loop = Loop.wireCircular(topLeft, bottomLeft, bottomMid, bottomRight, topRight, topMid)
        val sketchFace = SketchFace(listOf(FaceBound(loop, FaceBoundKind.OuterBound)))

        val offsetFace = offsetFaceFull(sketchFace, -0.55)
        val offsetFace2 = offsetFace(offsetFace, 0.55)

        val printer = DebugPrinter(1024, 1024)
        val rawEdges = loop.map { it.edge.edge }
        printer.add(rawEdges, Color.GREEN)
        //printer.add(testFaces)
        printer.add(offsetFace)
        printer.add(offsetFace2)
        printer.finish()
    }

}


class DebugPrinter(val width: Int, val height: Int){
    val dots = arrayListOf<DotPointer>()
    val image: BufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val graphics = image.createGraphics()

    class PixelPointer(val x : Int, val y : Int)
    class DotPointer(val point: Vec2, val color: Color, val size: Int)

    fun add(edges: List<Edge<Vec2>>, color: Color = Color.WHITE){
        graphics.color = color
        edges.forEach { drawEdge(it) }
    }

    fun add(face: SketchFace, color: Color = Color.WHITE){
        graphics.color = color

        face.bounds.forEach { bound ->
            bound.loop.forEach { loop ->
                drawEdge(loop.edge.edge)
            }
        }
    }

    fun projectSize(value : Double) : Int{
        return (value * 256.0).toInt()
    }

    fun project(value : Double) : Int{
        return (256.0 + value * 256.0).toInt()
    }

    fun from(vec2: Vec2) : PixelPointer{
        return PixelPointer(project((vec2.x + 1.0)), project((vec2.y + 1.0)))
    }

    fun finish(){
        graphics.color = Color.BLUE
        dots.forEach{
            graphics.color = it.color
            drawDot(from(it.point), it.size)
        }
        graphics.dispose()
        try {
            val out = FileOutputStream("debug.png")
            ImageIO.write(image, "png", out)
            out.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun drawDot(pixelPointer: PixelPointer, dotSize : Int = 4) {
        val x: Int = pixelPointer.x - dotSize / 2
        val y: Int = pixelPointer.y - dotSize / 2
        graphics.fillOval(x, y, dotSize, dotSize)
    }

    fun drawEdge(edge: Edge<Vec2>){
        val bound = edge.bound!!
        dots.add(DotPointer(bound.start.point, Color.RED, 4))
        dots.add(DotPointer(bound.end.point, Color.RED, 4))

        when(val curve = edge.curve){
            is Line -> {
                val start = from(bound.start.point)
                val end = from(bound.end.point)
                graphics.drawLine(start.x, start.y, end.x, end.y)
            }
            is Circle -> {
                val center = curve.workplane.origin
                val radius = curve.radius
                dots.add(DotPointer(center, Color.BLUE, 4))

                val upperLeft = from(center - radius)

                val (start, end) = if(bound.sense == Sense.Same){
                    arrayOf(bound.start.point - center, bound.end.point - center)
                }else{
                    arrayOf(bound.end.point - center, bound.start.point - center)
                }

                val startAngle = -Math.toDegrees(Vec2.DirX.angleTo(start))
                val arcAngle = -Math.toDegrees(start.angleTo(end))

                val size = projectSize(radius * 2)
                graphics.drawArc(upperLeft.x, upperLeft.y, size, size, startAngle.toInt(), arcAngle.toInt())
            }
        }
    }
}