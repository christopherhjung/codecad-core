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
        val faceBounds = faces.bounds
        val trees = nestHoles(faceBounds)
        return SketchFace(trees.children.map { it.bound })
    }

    @Test
    fun hourGlass(){
        val topLeft = Vertex(Vec2(-0.8, 1.0))
        val bottomLeft = Vertex(Vec2(-0.8, -1.0))
        val topMid = Vertex(Vec2(0.2, 0.1))
        val bottomMid = Vertex(Vec2(-0.2, -1.2))
        val bottomRight = Vertex(Vec2(0.8, -1.0))
        val topRight = Vertex(Vec2(0.8, 1.0))

        val loop = Loop.wireCircular(topLeft, bottomLeft, bottomMid, bottomRight, topRight, topMid)
        val sketchFace = SketchFace(listOf(FaceBound(loop, FaceBoundKind.OuterBound)))


        val printer = DebugPrinter(4096)
        val rawEdges = loop.map { it.edge.edge }
        printer.add(rawEdges, Color.GREEN)
        //printer.add(testFaces)


       val rawOffsetFace = offsetFace(sketchFace, -0.68)
        val edges = rawOffsetFace.bounds.flatMap { bound -> bound.loop.map { it.edge.edge } }
        val cutEdges = cutLines(edges)
        val loops = connectVerticesMirrored(cutEdges)
        val face = generateFaces(loops)
        val faceBounds = face.bounds
        val trees = nestHoles(faceBounds)


        printer.add(face)

        //val result = offsetFaceFull(sketchFace, -0.3)
        //printer.add(result)
        /*
        result.bounds.forEach {
            val area1 = it.loop.computeAreaVec2()
            val area2 = it.loop.computeAreaVec2(true)
            println(area1)
            println(area2)
            println(area2)
        }
       */


        printer.finish()
    }

}


class DebugPrinter(val size: Int, val scale : Double = 0.5){
    val dots = arrayListOf<DotPointer>()
    val image: BufferedImage = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
    val graphics = image.createGraphics()
    val invSize = 1.0 / size

    init {
        graphics.stroke = BasicStroke(10.0f * invSize.toFloat())
        val halfSize = size.toDouble() * 0.5
        val quadSize = size.toDouble() * 0.25
        graphics.translate(halfSize, halfSize)
        graphics.scale(quadSize, -quadSize)
    }

    class DotPointer(val point: Vec2, val color: Color, val size: Double)

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

    fun finish(){
        graphics.color = Color.BLUE
        dots.forEach{
            graphics.color = it.color
            drawDot(it.point, it.size)
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

    private fun drawDot(point: Vec2, dotSize : Double = 4.0) {
        val scaledDotSize = invSize * dotSize
        val x = point.x - scaledDotSize / 2
        val y = point.y - scaledDotSize / 2
        graphics.fill(Ellipse2D.Double(x, y, scaledDotSize, scaledDotSize))
    }

    fun drawEdge(edge: Edge<Vec2>){
        val bound = edge.bound!!
        dots.add(DotPointer(bound.start.point, Color.RED, 30.0))
        dots.add(DotPointer(bound.end.point, Color.RED, 30.0))

        when(val curve = edge.curve){
            is Line -> {
                val start = bound.start.point
                val end = bound.end.point
                graphics.draw(Line2D.Double(start.x, start.y, end.x, end.y))
            }
            is Circle -> {
                val center = curve.workplane.origin
                val radius = curve.radius
                dots.add(DotPointer(center, Color.BLUE, 50.0))

                val upperLeft = center - radius

                val start = if(bound.sense == Sense.Same){
                    bound.start.point
                }else{
                    bound.end.point
                } - center

                val end = if(bound.sense == Sense.Same){
                    bound.end.point
                }else{
                    bound.start.point
                } - center

                val startAngle = -Math.toDegrees(Vec2.DirX.angleTo(start))
                val arcAngle = -Math.toDegrees(start.angleTo(end))

                val size = radius * 2
                //graphics.drawArc(upperLeft.x, upperLeft.y, size, size, startAngle.toInt(), arcAngle.toInt())

                graphics.draw(Arc2D.Double(upperLeft.x, upperLeft.y, size, size, startAngle, arcAngle, Arc2D.OPEN))
            }
        }
    }
}