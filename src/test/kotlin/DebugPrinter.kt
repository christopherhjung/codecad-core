import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.Edge
import com.codecad.core.brep.SketchFace
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Line
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

val PASTEL_COLORS = arrayOf(
    Color(255, 182, 193),   // Pastel Pink
    Color(174, 198, 207),   // Pastel Blue
    Color(176, 229, 124),   // Pastel Green
    Color(177, 156, 217),   // Pastel Purple
    Color(255, 255, 153),   // Pastel Yellow
    Color(255, 215, 0),     // Pastel Orange
    Color(230, 230, 250),   // Pastel Lavender
    Color(152, 251, 152),   // Pastel Mint
    Color(255, 218, 185),   // Pastel Peach
    Color(192, 192, 192)    // Pastel Gray
)

class DebugPrinter(val size: Int, val scale : Double = 0.5){
    val dots = arrayListOf<DotPointer>()
    val image: BufferedImage = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
    val graphics = image.createGraphics()
    val invSize = 1.0 / size
    var idx = 0

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

    fun add(face: SketchFace, color: Color){
        graphics.color = color

        face.bounds.forEach { bound ->
            bound.loop.forEach { loop ->
                drawEdge(loop.edge.edge)
            }
        }
    }

    fun add(face: SketchFace){
        face.bounds.forEach { bound ->
            graphics.color = PASTEL_COLORS[idx++ % PASTEL_COLORS.size]
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
        val bound = edge.bound
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

                val alignedBound = bound.align()
                val start = alignedBound.start.point - center
                val end = alignedBound.end.point - center

                val startAngle = -Math.toDegrees(Vec2.DirX.angleTo(start))
                val arcAngle = -Math.toDegrees(start.angleTo(end))

                val size = radius * 2
                //graphics.drawArc(upperLeft.x, upperLeft.y, size, size, startAngle.toInt(), arcAngle.toInt())

                graphics.draw(Arc2D.Double(upperLeft.x, upperLeft.y, size, size, startAngle, arcAngle, Arc2D.OPEN))
            }
        }
    }
}