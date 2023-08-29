package com.codecad.core

//class Line(val origin: Vec3Expr, val direction: Vec3Expr) : Curve(){
  //  companion object{
        /*private fun projectPlane(a: Plane, b: Plane) : Vec3Expr {
            return (a.distance - b.distance * ( a.normal.dot(b.normal) ))/
                    (1.0 - a.normal.dot(b.normal).pow(2.0)) * a.normal
        }

        fun projectPlaneFull(a: Plane, b: Plane) : Vec3Expr {
            return (a.distance * b.normal.squaredLength() - b.distance * ( a.normal.dot(b.normal) ))/
                    (a.normal.squaredLength() * b.normal.squaredLength() - a.normal.dot(b.normal).pow(2.0)) * a.normal
        }

        fun fromPlanes(a: Plane, b: Plane) : Line {
            val a = a.normalized()
            val b = b.normalized()
            val direction = a.normal.cross(b.normal)
            val origin = projectPlane(a,b) + projectPlane(b,a)
            return Line(origin, direction)
        }*/
   // }
//}