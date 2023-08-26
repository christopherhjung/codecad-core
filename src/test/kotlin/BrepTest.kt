import com.codecad.common.Mesh
import com.codecad.core.Plane
import com.codecad.core.World
import com.codecad.core.export.StlExport
import com.codecad.core.face.entity.*
import com.codecad.core.face.entity.curve.Circle
import com.codecad.core.face.entity.surface.CylindricalSurface
import com.codecad.core.face.entity.surface.PlaneSurface
import com.codecad.core.mesh.MeshGenerator
import com.codecad.core.volume.Volume
import org.junit.jupiter.api.Test
import java.io.FileOutputStream

class BrepTest {

    @Test
    fun test(){
        val world = World()

        val topCenter = world.vec3(world.Zero, world.Zero, world.literal(50.0))
        val bottomCenter = world.vec3(world.Zero, world.Zero, world.literal(0.0))

        val topWorkplane = Workplane(topCenter, world.DirectionX, world.DirectionY )
        val bottomWorkplane = Workplane(bottomCenter, world.DirectionX, world.DirectionY )
        val zeroXYWorkplane = Workplane(world.ZeroVec3, world.DirectionX, world.DirectionY )

        val radius = world.literal(50.0)

        val topEdge = Edge(Circle(topWorkplane, radius))
        val bottomEdge = Edge(Circle(bottomWorkplane, radius))

        val topSurface = PlaneSurface(topWorkplane)
        val bottomSurface = PlaneSurface(bottomWorkplane)

        val topEdgeLoop = EdgeLoop.closed(topEdge)
        val bottomEdgeLoop = EdgeLoop.closed(bottomEdge)

        val topFace = Face(topSurface, listOf(FaceBound(topEdgeLoop, false)))
        val bottomFace = Face(bottomSurface, listOf(FaceBound(bottomEdgeLoop, false)))

        val cylindricalSurface = CylindricalSurface(zeroXYWorkplane, radius)
        val cylindricalFace = Face(cylindricalSurface,
            listOf(
                FaceBound(topEdgeLoop, true),
                FaceBound(bottomEdgeLoop, true)
            )
        )

        val shell = Shell(listOf(topFace, cylindricalFace, bottomFace))

        val volume = Volume(listOf(shell))

        val meshGenerator = MeshGenerator()
        val mesh = meshGenerator.generate(volume)

        val stlExport = StlExport()
        val byteArray = stlExport.export(mesh)

        val outStream = FileOutputStream("test.stl")
        outStream.write(byteArray)
        outStream.close()

        println(volume)
    }
}