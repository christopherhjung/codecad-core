package com.codecad.core.face.entity

import com.codecad.core.ast.vec.Vec3
import com.codecad.core.face.entity.surface.PlaneSurface
import com.codecad.core.face.entity.surface.Surface
import com.codecad.core.face.isPointInPolygon
import org.poly2tri.Poly2Tri
import org.poly2tri.geometry.polygon.PolygonPoint
import org.poly2tri.triangulation.TriangulationPoint


class Face(var surface : Surface, var bounds : List<FaceBound>)

