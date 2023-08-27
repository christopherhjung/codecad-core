import com.codecad.core.face.entity.*
import com.codecad.core.face.entity.curve.Circle
import com.codecad.core.face.entity.surface.CylindricalSurface
import com.codecad.core.face.entity.surface.PlaneSurface
import com.codecad.core.volume.Volume

object VolumeSuite {
    fun createCylinder(bottomWorkplane: WorkplaneExpr, outerRadius: Double, height: Double) : Volume {
        val world = bottomWorkplane.origin.world
        val outerRadius = world.literal(outerRadius)
        val height = world.literal(height)

        val topWorkplane = WorkplaneExpr(bottomWorkplane.origin + bottomWorkplane.axisUp * height, bottomWorkplane.axisA, bottomWorkplane.axisB )
        val zeroXYWorkplane = WorkplaneExpr(world.ZeroVec3, bottomWorkplane.axisA, bottomWorkplane.axisB )

        val topEdge = Edge(Circle(topWorkplane, outerRadius))
        val bottomEdge = Edge(Circle(bottomWorkplane, outerRadius))

        val topSurface = PlaneSurface(topWorkplane)
        val bottomSurface = PlaneSurface(bottomWorkplane)

        val topEdgeLoop = EdgeLoop.of(topEdge)
        val bottomEdgeLoop = EdgeLoop.of(bottomEdge)

        val topFace = Face(topSurface, listOf(FaceBound(topEdgeLoop, true)))
        val bottomFace = Face(bottomSurface, listOf(FaceBound(bottomEdgeLoop, true)))

        val outerCylindricalSurface = CylindricalSurface(zeroXYWorkplane, outerRadius)
        val outerCylindricalFace = Face(outerCylindricalSurface,
            listOf(
                FaceBound(topEdgeLoop, true),
                FaceBound(bottomEdgeLoop, true)
            )
        )

        val shell = Shell(listOf(topFace, outerCylindricalFace, bottomFace))
        val volume = Volume(listOf(shell))
        return volume
    }

    fun createPipe(bottomWorkplane: WorkplaneExpr, outerRadius: Double, innerRadius: Double, height: Double) : Volume {
        val world = bottomWorkplane.origin.world
        val outerRadius = world.literal(outerRadius)
        val innerRadius = world.literal(innerRadius)
        val height = world.literal(height)

        val topWorkplane = WorkplaneExpr(bottomWorkplane.origin + bottomWorkplane.axisUp * height, bottomWorkplane.axisA, bottomWorkplane.axisB )
        val zeroXYWorkplane = WorkplaneExpr(world.ZeroVec3, bottomWorkplane.axisA, bottomWorkplane.axisB )

        val topEdge = Edge(Circle(topWorkplane, outerRadius))
        val bottomEdge = Edge(Circle(bottomWorkplane, outerRadius))

        val topHoleEdge = Edge(Circle(topWorkplane, innerRadius))
        val bottomHoleEdge = Edge(Circle(bottomWorkplane, innerRadius))

        val topSurface = PlaneSurface(topWorkplane)
        val bottomSurface = PlaneSurface(bottomWorkplane)

        val topEdgeLoop = EdgeLoop.of(topEdge)
        val bottomEdgeLoop = EdgeLoop.of(bottomEdge)

        val topHoleEdgeLoop = EdgeLoop.of(topHoleEdge)
        val bottomHoleEdgeLoop = EdgeLoop.of(bottomHoleEdge)

        val topFace = Face(topSurface, listOf(FaceBound(topEdgeLoop, true), FaceBound(topHoleEdgeLoop, false)))
        val bottomFace = Face(bottomSurface, listOf(FaceBound(bottomEdgeLoop, true), FaceBound(bottomHoleEdgeLoop, false)))

        val outerCylindricalSurface = CylindricalSurface(zeroXYWorkplane, outerRadius)
        val outerCylindricalFace = Face(outerCylindricalSurface,
            listOf(
                FaceBound(topEdgeLoop, true),
                FaceBound(bottomEdgeLoop, true)
            )
        )

        val innerCylindricalSurface = CylindricalSurface(zeroXYWorkplane, innerRadius)
        val innerCylindricalFace = Face(innerCylindricalSurface,
            listOf(
                FaceBound(topHoleEdgeLoop, true),
                FaceBound(bottomHoleEdgeLoop, true)
            )
        )

        val shell = Shell(listOf(topFace, outerCylindricalFace, bottomFace, innerCylindricalFace))
        val volume = Volume(listOf(shell))
        return volume
    }


    fun createPlane(workplane: WorkplaneExpr, size: Double) : Volume {
        val origin = workplane.origin
        val world = origin.world
        val size = world.literal(size)
        val halfSize = size / 2.0

        val axisA = workplane.axisA
        val axisB = workplane.axisB

        val a = Vertex(origin - axisA * halfSize - axisB * halfSize)
        val b = Vertex(origin + axisA * halfSize - axisB * halfSize)
        val c = Vertex(origin + axisA * halfSize + axisB * halfSize)
        val d = Vertex(origin - axisA * halfSize + axisB * halfSize)

        val abEdge = Edge.line(a, b)
        val bcEdge = Edge.line(b, c)
        val cdEdge = Edge.line(c, d)
        val daEdge = Edge.line(d, a)

        val surface = PlaneSurface(workplane)

        val edgeLoop = EdgeLoop.of(abEdge, bcEdge, cdEdge, daEdge)

        val face = Face(surface, listOf(FaceBound(edgeLoop, true)))

        val shell = Shell(listOf(face))
        val volume = Volume(listOf(shell))
        return volume
    }

    fun roundedPlane(workplane: WorkplaneExpr, size: Double, radius: Double) : Volume {
        val origin = workplane.origin
        val world = origin.world
        val size = world.literal(size)
        val radius = world.literal(radius)
        val halfSize = size / 2.0
        val halfLength = halfSize - radius

        val a1 = Vertex(workplane.unproject(-halfLength, -halfSize))
        val a2 = Vertex(workplane.unproject(halfLength, -halfSize))

        val b1 = Vertex(workplane.unproject(halfSize, -halfLength))
        val b2 = Vertex(workplane.unproject(halfSize, halfLength))

        val c1 = Vertex(workplane.unproject(halfLength, halfSize))
        val c2 = Vertex(workplane.unproject(-halfLength, halfSize))

        val d1 = Vertex(workplane.unproject(-halfSize, halfLength))
        val d2 = Vertex(workplane.unproject(-halfSize, -halfLength))

        val llCircle = workplane.unproject(-halfLength, -halfLength)
        val lrCircle = workplane.unproject(halfLength, -halfLength)
        val urCircle = workplane.unproject(halfLength, halfLength)
        val ulCircle = workplane.unproject(-halfLength, halfLength)

        val aEdge = Edge.line(a1, a2)
        val bEdge = Edge.line(b1, b2)
        val cEdge = Edge.line(c1, c2)
        val dEdge = Edge.line(d1, d2)

        val abArc = Edge.arc(workplane.withOrigin(lrCircle), a2, b1, Sense.CCW)
        val bcArc = Edge.arc(workplane.withOrigin(urCircle), b2, c1, Sense.CCW)
        val cdArc = Edge.arc(workplane.withOrigin(ulCircle), c2, d1, Sense.CCW)
        val daArc = Edge.arc(workplane.withOrigin(llCircle), d2, a1, Sense.CCW)

        val surface = PlaneSurface(workplane)
        val edgeLoop = EdgeLoop.of(aEdge, abArc, bEdge, bcArc, cEdge, cdArc, dEdge, daArc)
        val face = Face(surface, listOf(FaceBound(edgeLoop, true)))

        val shell = Shell(listOf(face))
        val volume = Volume(listOf(shell))
        return volume
    }
}