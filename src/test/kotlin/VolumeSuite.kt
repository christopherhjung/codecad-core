import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.BSpline
import com.codecad.core.brep.curve.BSplineControlPoint
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.surface.CylindricalSurface
import com.codecad.core.brep.surface.PlaneSurface
import com.codecad.core.volume.Volume

object VolumeSuite {
    fun createCylinder(bottomWorkplane: Workplane, outerRadius: Double, height: Double) : Volume {

        val topWorkplane = Workplane(
            bottomWorkplane.origin + bottomWorkplane.normal * height,
            bottomWorkplane.normal,
            bottomWorkplane.normal
        )
        val zeroXYWorkplane = Workplane(Vec3.ZERO, bottomWorkplane.normal, bottomWorkplane.normal)

        val topEdge = Edge(Circle(topWorkplane, outerRadius))
        val bottomEdge = Edge(Circle(bottomWorkplane, outerRadius))

        val topSurface = PlaneSurface(topWorkplane)
        val bottomSurface = PlaneSurface(bottomWorkplane)

        val topEdgeLoop = EdgeLoop.of(topEdge)
        val bottomEdgeLoop = EdgeLoop.of(bottomEdge)

        val topFace = Face(topSurface, listOf(FaceBound(topEdgeLoop, FaceBoundKind.OuterBound)))
        val bottomFace = Face(bottomSurface, listOf(FaceBound(bottomEdgeLoop, FaceBoundKind.OuterBound)))

        val outerCylindricalSurface = CylindricalSurface(zeroXYWorkplane, outerRadius)
        val outerCylindricalFace = Face(outerCylindricalSurface,
            listOf(
                FaceBound(topEdgeLoop, FaceBoundKind.OuterBound),
                FaceBound(bottomEdgeLoop, FaceBoundKind.OuterBound)
            )
        )

        val shell = Shell(listOf(topFace, outerCylindricalFace, bottomFace))
        val volume = Volume(listOf(shell))
        return volume
    }

    fun createPipe(bottomWorkplane: Workplane, outerRadius: Double, innerRadius: Double, height: Double) : Volume {

        val topWorkplane = bottomWorkplane.move(bottomWorkplane.normal * height)
        val zeroXYWorkplane = bottomWorkplane.withOrigin(Vec3.ZERO)

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

        val topFace = Face(topSurface, listOf(FaceBound(topEdgeLoop, FaceBoundKind.OuterBound), FaceBound(topHoleEdgeLoop, FaceBoundKind.InnerBound)))
        val bottomFace = Face(bottomSurface, listOf(FaceBound(bottomEdgeLoop, FaceBoundKind.OuterBound), FaceBound(bottomHoleEdgeLoop, FaceBoundKind.InnerBound)))

        val outerCylindricalSurface = CylindricalSurface(zeroXYWorkplane, outerRadius)
        val outerCylindricalFace = Face(outerCylindricalSurface,
            listOf(
                FaceBound(topEdgeLoop, FaceBoundKind.OuterBound),
                FaceBound(bottomEdgeLoop, FaceBoundKind.OuterBound)
            )
        )

        val innerCylindricalSurface = CylindricalSurface(zeroXYWorkplane, innerRadius)
        val innerCylindricalFace = Face(innerCylindricalSurface,
            listOf(
                FaceBound(topHoleEdgeLoop, FaceBoundKind.OuterBound),
                FaceBound(bottomHoleEdgeLoop, FaceBoundKind.OuterBound)
            )
        )

        val shell = Shell(listOf(topFace, outerCylindricalFace, bottomFace, innerCylindricalFace))
        val volume = Volume(listOf(shell))
        return volume
    }


    fun createCircleWithHole(bottomWorkplane: Workplane, outerRadius: Double, innerRadius: Double) : Face {
        val bottomEdge = Edge(Circle(bottomWorkplane, outerRadius))
        val bottomHoleEdge = Edge(Circle(bottomWorkplane, innerRadius))

        val bottomSurface = PlaneSurface(bottomWorkplane)

        val bottomEdgeLoop = EdgeLoop.of(bottomEdge)
        val bottomHoleEdgeLoop = EdgeLoop.of(bottomHoleEdge)

        val face = Face(bottomSurface, listOf(FaceBound(bottomEdgeLoop, FaceBoundKind.OuterBound), FaceBound(bottomHoleEdgeLoop, FaceBoundKind.InnerBound)))

        return face
    }

    fun createCircle(bottomWorkplane: Workplane, radius: Double) : Face {
        val bottomEdge = Edge(Circle(bottomWorkplane, radius))
        val bottomSurface = PlaneSurface(bottomWorkplane)
        val bottomEdgeLoop = EdgeLoop.of(bottomEdge)
        val face = Face(bottomSurface, listOf(FaceBound(bottomEdgeLoop, FaceBoundKind.OuterBound)))
        return face
    }

    fun createPlane(workplane: Workplane, size: Double) : Face {
        val halfSize = size / 2.0

        val a = Vertex(workplane.unproject(-halfSize, -halfSize))
        val b = Vertex(workplane.unproject(halfSize, -halfSize))
        val c = Vertex(workplane.unproject(halfSize, halfSize))
        val d = Vertex(workplane.unproject(-halfSize, halfSize))

        val surface = PlaneSurface(workplane)
        val edgeLoop = EdgeLoop.polygon(a,b,c,d)

        val face = Face(surface, listOf(FaceBound(edgeLoop, FaceBoundKind.OuterBound)))

        return face
    }

    fun createTriangle(workplane: Workplane, size: Double) : Face {
        val halfSize = size / 2.0

        val a = Vertex(workplane.unproject(-halfSize, -halfSize))
        val b = Vertex(workplane.unproject(-halfSize, halfSize))
        val c = Vertex(workplane.unproject(halfSize, 0.0))

        val surface = PlaneSurface(workplane)
        val edgeLoop = EdgeLoop.polygon(a,b,c)

        val face = Face(surface, listOf(FaceBound(edgeLoop, FaceBoundKind.OuterBound)))

        return face
    }

    fun roundedPlane(workplane: Workplane, size: Double, radius: Double) : Face {
        val origin = workplane.origin
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

        val abArc = Edge.arc(workplane.withOrigin(lrCircle), a2, b1, Sense.CW)
        val bcArc = Edge.arc(workplane.withOrigin(urCircle), b2, c1, Sense.CW)
        val cdArc = Edge.arc(workplane.withOrigin(ulCircle), c2, d1, Sense.CW)
        val daArc = Edge.arc(workplane.withOrigin(llCircle), d2, a1, Sense.CW)

        val surface = PlaneSurface(workplane)
        val edgeLoop = EdgeLoop.forward(aEdge, abArc, bEdge, bcArc, cEdge, cdArc, dEdge, daArc)
        val face = Face(surface, listOf(FaceBound(edgeLoop, FaceBoundKind.OuterBound)))

        return face
    }

    fun splineCircle(workplane: Workplane, radius: Double) : Face {
        val origin = workplane.origin

        val aPoint = BSplineControlPoint(workplane.unproject(-radius, -radius), 1.0)
        val bPoint = BSplineControlPoint(workplane.unproject(radius, -radius), 1.0)
        val cPoint = BSplineControlPoint(workplane.unproject(radius, radius), 1.0)
        val dPoint = BSplineControlPoint(workplane.unproject(-radius, radius), 1.0)

        val spline = BSpline(arrayOf(aPoint, bPoint, cPoint, dPoint, aPoint))

        val aVertex = Vertex(aPoint.point)
        //val bVertex = Vertex(bPoint.point)
        //val cVertex = Vertex(cPoint.point)
        //val dVertex = Vertex(dPoint.point)

        val aEdge = Edge(spline, EdgeBound(aVertex, aVertex, Sense.None))
        val surface = PlaneSurface(workplane)
        val edgeLoop = EdgeLoop.of(aEdge)
        val face = Face(surface, listOf(FaceBound(edgeLoop, FaceBoundKind.OuterBound)))

        return face
    }
}