package com.codecad.core.sim

import com.codecad.core.ast.vec.Vec3


val CornerIndexAFromEdge = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 0, 1, 2, 3)
val CornerIndexBFromEdge = intArrayOf(1, 2, 3, 0, 5, 6, 7, 4, 4, 5, 6, 7)

val Triangulation = arrayOf(
    intArrayOf(),
    intArrayOf(0, 8, 3),
    intArrayOf(0, 1, 9),
    intArrayOf(1, 8, 3, 9, 8, 1),
    intArrayOf(1, 2, 10),
    intArrayOf(0, 8, 3, 1, 2, 10),
    intArrayOf(9, 2, 10, 0, 2, 9),
    intArrayOf(2, 8, 3, 2, 10, 8, 10, 9, 8),
    intArrayOf(3, 11, 2),
    intArrayOf(0, 11, 2, 8, 11, 0),
    intArrayOf(1, 9, 0, 2, 3, 11),
    intArrayOf(1, 11, 2, 1, 9, 11, 9, 8, 11),
    intArrayOf(3, 10, 1, 11, 10, 3),
    intArrayOf(0, 10, 1, 0, 8, 10, 8, 11, 10),
    intArrayOf(3, 9, 0, 3, 11, 9, 11, 10, 9),
    intArrayOf(9, 8, 10, 10, 8, 11),
    intArrayOf(4, 7, 8),
    intArrayOf(4, 3, 0, 7, 3, 4),
    intArrayOf(0, 1, 9, 8, 4, 7),
    intArrayOf(4, 1, 9, 4, 7, 1, 7, 3, 1),
    intArrayOf(1, 2, 10, 8, 4, 7),
    intArrayOf(3, 4, 7, 3, 0, 4, 1, 2, 10),
    intArrayOf(9, 2, 10, 9, 0, 2, 8, 4, 7),
    intArrayOf(2, 10, 9, 2, 9, 7, 2, 7, 3, 7, 9, 4),
    intArrayOf(8, 4, 7, 3, 11, 2),
    intArrayOf(11, 4, 7, 11, 2, 4, 2, 0, 4),
    intArrayOf(9, 0, 1, 8, 4, 7, 2, 3, 11),
    intArrayOf(4, 7, 11, 9, 4, 11, 9, 11, 2, 9, 2, 1),
    intArrayOf(3, 10, 1, 3, 11, 10, 7, 8, 4),
    intArrayOf(1, 11, 10, 1, 4, 11, 1, 0, 4, 7, 11, 4),
    intArrayOf(4, 7, 8, 9, 0, 11, 9, 11, 10, 11, 0, 3),
    intArrayOf(4, 7, 11, 4, 11, 9, 9, 11, 10),
    intArrayOf(9, 5, 4),
    intArrayOf(9, 5, 4, 0, 8, 3),
    intArrayOf(0, 5, 4, 1, 5, 0),
    intArrayOf(8, 5, 4, 8, 3, 5, 3, 1, 5),
    intArrayOf(1, 2, 10, 9, 5, 4),
    intArrayOf(3, 0, 8, 1, 2, 10, 4, 9, 5),
    intArrayOf(5, 2, 10, 5, 4, 2, 4, 0, 2),
    intArrayOf(2, 10, 5, 3, 2, 5, 3, 5, 4, 3, 4, 8),
    intArrayOf(9, 5, 4, 2, 3, 11),
    intArrayOf(0, 11, 2, 0, 8, 11, 4, 9, 5),
    intArrayOf(0, 5, 4, 0, 1, 5, 2, 3, 11),
    intArrayOf(2, 1, 5, 2, 5, 8, 2, 8, 11, 4, 8, 5),
    intArrayOf(10, 3, 11, 10, 1, 3, 9, 5, 4),
    intArrayOf(4, 9, 5, 0, 8, 1, 8, 10, 1, 8, 11, 10),
    intArrayOf(5, 4, 0, 5, 0, 11, 5, 11, 10, 11, 0, 3),
    intArrayOf(5, 4, 8, 5, 8, 10, 10, 8, 11),
    intArrayOf(9, 7, 8, 5, 7, 9),
    intArrayOf(9, 3, 0, 9, 5, 3, 5, 7, 3),
    intArrayOf(0, 7, 8, 0, 1, 7, 1, 5, 7),
    intArrayOf(1, 5, 3, 3, 5, 7),
    intArrayOf(9, 7, 8, 9, 5, 7, 10, 1, 2),
    intArrayOf(10, 1, 2, 9, 5, 0, 5, 3, 0, 5, 7, 3),
    intArrayOf(8, 0, 2, 8, 2, 5, 8, 5, 7, 10, 5, 2),
    intArrayOf(2, 10, 5, 2, 5, 3, 3, 5, 7),
    intArrayOf(7, 9, 5, 7, 8, 9, 3, 11, 2),
    intArrayOf(9, 5, 7, 9, 7, 2, 9, 2, 0, 2, 7, 11),
    intArrayOf(2, 3, 11, 0, 1, 8, 1, 7, 8, 1, 5, 7),
    intArrayOf(11, 2, 1, 11, 1, 7, 7, 1, 5),
    intArrayOf(9, 5, 8, 8, 5, 7, 10, 1, 3, 10, 3, 11),
    intArrayOf(5, 7, 0, 5, 0, 9, 7, 11, 0, 1, 0, 10, 11, 10, 0),
    intArrayOf(11, 10, 0, 11, 0, 3, 10, 5, 0, 8, 0, 7, 5, 7, 0),
    intArrayOf(11, 10, 5, 7, 11, 5),
    intArrayOf(10, 6, 5),
    intArrayOf(0, 8, 3, 5, 10, 6),
    intArrayOf(9, 0, 1, 5, 10, 6),
    intArrayOf(1, 8, 3, 1, 9, 8, 5, 10, 6),
    intArrayOf(1, 6, 5, 2, 6, 1),
    intArrayOf(1, 6, 5, 1, 2, 6, 3, 0, 8),
    intArrayOf(9, 6, 5, 9, 0, 6, 0, 2, 6),
    intArrayOf(5, 9, 8, 5, 8, 2, 5, 2, 6, 3, 2, 8),
    intArrayOf(2, 3, 11, 10, 6, 5),
    intArrayOf(11, 0, 8, 11, 2, 0, 10, 6, 5),
    intArrayOf(0, 1, 9, 2, 3, 11, 5, 10, 6),
    intArrayOf(5, 10, 6, 1, 9, 2, 9, 11, 2, 9, 8, 11),
    intArrayOf(6, 3, 11, 6, 5, 3, 5, 1, 3),
    intArrayOf(0, 8, 11, 0, 11, 5, 0, 5, 1, 5, 11, 6),
    intArrayOf(3, 11, 6, 0, 3, 6, 0, 6, 5, 0, 5, 9),
    intArrayOf(6, 5, 9, 6, 9, 11, 11, 9, 8),
    intArrayOf(5, 10, 6, 4, 7, 8),
    intArrayOf(4, 3, 0, 4, 7, 3, 6, 5, 10),
    intArrayOf(1, 9, 0, 5, 10, 6, 8, 4, 7),
    intArrayOf(10, 6, 5, 1, 9, 7, 1, 7, 3, 7, 9, 4),
    intArrayOf(6, 1, 2, 6, 5, 1, 4, 7, 8),
    intArrayOf(1, 2, 5, 5, 2, 6, 3, 0, 4, 3, 4, 7),
    intArrayOf(8, 4, 7, 9, 0, 5, 0, 6, 5, 0, 2, 6),
    intArrayOf(7, 3, 9, 7, 9, 4, 3, 2, 9, 5, 9, 6, 2, 6, 9),
    intArrayOf(3, 11, 2, 7, 8, 4, 10, 6, 5),
    intArrayOf(5, 10, 6, 4, 7, 2, 4, 2, 0, 2, 7, 11),
    intArrayOf(0, 1, 9, 4, 7, 8, 2, 3, 11, 5, 10, 6),
    intArrayOf(9, 2, 1, 9, 11, 2, 9, 4, 11, 7, 11, 4, 5, 10, 6),
    intArrayOf(8, 4, 7, 3, 11, 5, 3, 5, 1, 5, 11, 6),
    intArrayOf(5, 1, 11, 5, 11, 6, 1, 0, 11, 7, 11, 4, 0, 4, 11),
    intArrayOf(0, 5, 9, 0, 6, 5, 0, 3, 6, 11, 6, 3, 8, 4, 7),
    intArrayOf(6, 5, 9, 6, 9, 11, 4, 7, 9, 7, 11, 9),
    intArrayOf(10, 4, 9, 6, 4, 10),
    intArrayOf(4, 10, 6, 4, 9, 10, 0, 8, 3),
    intArrayOf(10, 0, 1, 10, 6, 0, 6, 4, 0),
    intArrayOf(8, 3, 1, 8, 1, 6, 8, 6, 4, 6, 1, 10),
    intArrayOf(1, 4, 9, 1, 2, 4, 2, 6, 4),
    intArrayOf(3, 0, 8, 1, 2, 9, 2, 4, 9, 2, 6, 4),
    intArrayOf(0, 2, 4, 4, 2, 6),
    intArrayOf(8, 3, 2, 8, 2, 4, 4, 2, 6),
    intArrayOf(10, 4, 9, 10, 6, 4, 11, 2, 3),
    intArrayOf(0, 8, 2, 2, 8, 11, 4, 9, 10, 4, 10, 6),
    intArrayOf(3, 11, 2, 0, 1, 6, 0, 6, 4, 6, 1, 10),
    intArrayOf(6, 4, 1, 6, 1, 10, 4, 8, 1, 2, 1, 11, 8, 11, 1),
    intArrayOf(9, 6, 4, 9, 3, 6, 9, 1, 3, 11, 6, 3),
    intArrayOf(8, 11, 1, 8, 1, 0, 11, 6, 1, 9, 1, 4, 6, 4, 1),
    intArrayOf(3, 11, 6, 3, 6, 0, 0, 6, 4),
    intArrayOf(6, 4, 8, 11, 6, 8),
    intArrayOf(7, 10, 6, 7, 8, 10, 8, 9, 10),
    intArrayOf(0, 7, 3, 0, 10, 7, 0, 9, 10, 6, 7, 10),
    intArrayOf(10, 6, 7, 1, 10, 7, 1, 7, 8, 1, 8, 0),
    intArrayOf(10, 6, 7, 10, 7, 1, 1, 7, 3),
    intArrayOf(1, 2, 6, 1, 6, 8, 1, 8, 9, 8, 6, 7),
    intArrayOf(2, 6, 9, 2, 9, 1, 6, 7, 9, 0, 9, 3, 7, 3, 9),
    intArrayOf(7, 8, 0, 7, 0, 6, 6, 0, 2),
    intArrayOf(7, 3, 2, 6, 7, 2),
    intArrayOf(2, 3, 11, 10, 6, 8, 10, 8, 9, 8, 6, 7),
    intArrayOf(2, 0, 7, 2, 7, 11, 0, 9, 7, 6, 7, 10, 9, 10, 7),
    intArrayOf(1, 8, 0, 1, 7, 8, 1, 10, 7, 6, 7, 10, 2, 3, 11),
    intArrayOf(11, 2, 1, 11, 1, 7, 10, 6, 1, 6, 7, 1),
    intArrayOf(8, 9, 6, 8, 6, 7, 9, 1, 6, 11, 6, 3, 1, 3, 6),
    intArrayOf(0, 9, 1, 11, 6, 7),
    intArrayOf(7, 8, 0, 7, 0, 6, 3, 11, 0, 11, 6, 0),
    intArrayOf(7, 11, 6),
    intArrayOf(7, 6, 11),
    intArrayOf(3, 0, 8, 11, 7, 6),
    intArrayOf(0, 1, 9, 11, 7, 6),
    intArrayOf(8, 1, 9, 8, 3, 1, 11, 7, 6),
    intArrayOf(10, 1, 2, 6, 11, 7),
    intArrayOf(1, 2, 10, 3, 0, 8, 6, 11, 7),
    intArrayOf(2, 9, 0, 2, 10, 9, 6, 11, 7),
    intArrayOf(6, 11, 7, 2, 10, 3, 10, 8, 3, 10, 9, 8),
    intArrayOf(7, 2, 3, 6, 2, 7),
    intArrayOf(7, 0, 8, 7, 6, 0, 6, 2, 0),
    intArrayOf(2, 7, 6, 2, 3, 7, 0, 1, 9),
    intArrayOf(1, 6, 2, 1, 8, 6, 1, 9, 8, 8, 7, 6),
    intArrayOf(10, 7, 6, 10, 1, 7, 1, 3, 7),
    intArrayOf(10, 7, 6, 1, 7, 10, 1, 8, 7, 1, 0, 8),
    intArrayOf(0, 3, 7, 0, 7, 10, 0, 10, 9, 6, 10, 7),
    intArrayOf(7, 6, 10, 7, 10, 8, 8, 10, 9),
    intArrayOf(6, 8, 4, 11, 8, 6),
    intArrayOf(3, 6, 11, 3, 0, 6, 0, 4, 6),
    intArrayOf(8, 6, 11, 8, 4, 6, 9, 0, 1),
    intArrayOf(9, 4, 6, 9, 6, 3, 9, 3, 1, 11, 3, 6),
    intArrayOf(6, 8, 4, 6, 11, 8, 2, 10, 1),
    intArrayOf(1, 2, 10, 3, 0, 11, 0, 6, 11, 0, 4, 6),
    intArrayOf(4, 11, 8, 4, 6, 11, 0, 2, 9, 2, 10, 9),
    intArrayOf(10, 9, 3, 10, 3, 2, 9, 4, 3, 11, 3, 6, 4, 6, 3),
    intArrayOf(8, 2, 3, 8, 4, 2, 4, 6, 2),
    intArrayOf(0, 4, 2, 4, 6, 2),
    intArrayOf(1, 9, 0, 2, 3, 4, 2, 4, 6, 4, 3, 8),
    intArrayOf(1, 9, 4, 1, 4, 2, 2, 4, 6),
    intArrayOf(8, 1, 3, 8, 6, 1, 8, 4, 6, 6, 10, 1),
    intArrayOf(10, 1, 0, 10, 0, 6, 6, 0, 4),
    intArrayOf(4, 6, 3, 4, 3, 8, 6, 10, 3, 0, 3, 9, 10, 9, 3),
    intArrayOf(10, 9, 4, 6, 10, 4),
    intArrayOf(4, 9, 5, 7, 6, 11),
    intArrayOf(0, 8, 3, 4, 9, 5, 11, 7, 6),
    intArrayOf(5, 0, 1, 5, 4, 0, 7, 6, 11),
    intArrayOf(11, 7, 6, 8, 3, 4, 3, 5, 4, 3, 1, 5),
    intArrayOf(9, 5, 4, 10, 1, 2, 7, 6, 11),
    intArrayOf(6, 11, 7, 1, 2, 10, 0, 8, 3, 4, 9, 5),
    intArrayOf(7, 6, 11, 5, 4, 10, 4, 2, 10, 4, 0, 2),
    intArrayOf(3, 4, 8, 3, 5, 4, 3, 2, 5, 10, 5, 2, 11, 7, 6),
    intArrayOf(7, 2, 3, 7, 6, 2, 5, 4, 9),
    intArrayOf(9, 5, 4, 0, 8, 6, 0, 6, 2, 6, 8, 7),
    intArrayOf(3, 6, 2, 3, 7, 6, 1, 5, 0, 5, 4, 0),
    intArrayOf(6, 2, 8, 6, 8, 7, 2, 1, 8, 4, 8, 5, 1, 5, 8),
    intArrayOf(9, 5, 4, 10, 1, 6, 1, 7, 6, 1, 3, 7),
    intArrayOf(1, 6, 10, 1, 7, 6, 1, 0, 7, 8, 7, 0, 9, 5, 4),
    intArrayOf(4, 0, 10, 4, 10, 5, 0, 3, 10, 6, 10, 7, 3, 7, 10),
    intArrayOf(7, 6, 10, 7, 10, 8, 5, 4, 10, 4, 8, 10),
    intArrayOf(6, 9, 5, 6, 11, 9, 11, 8, 9),
    intArrayOf(3, 6, 11, 0, 6, 3, 0, 5, 6, 0, 9, 5),
    intArrayOf(0, 11, 8, 0, 5, 11, 0, 1, 5, 5, 6, 11),
    intArrayOf(6, 11, 3, 6, 3, 5, 5, 3, 1),
    intArrayOf(1, 2, 10, 9, 5, 11, 9, 11, 8, 11, 5, 6),
    intArrayOf(0, 11, 3, 0, 6, 11, 0, 9, 6, 5, 6, 9, 1, 2, 10),
    intArrayOf(11, 8, 5, 11, 5, 6, 8, 0, 5, 10, 5, 2, 0, 2, 5),
    intArrayOf(6, 11, 3, 6, 3, 5, 2, 10, 3, 10, 5, 3),
    intArrayOf(5, 8, 9, 5, 2, 8, 5, 6, 2, 3, 8, 2),
    intArrayOf(9, 5, 6, 9, 6, 0, 0, 6, 2),
    intArrayOf(1, 5, 8, 1, 8, 0, 5, 6, 8, 3, 8, 2, 6, 2, 8),
    intArrayOf(1, 5, 6, 2, 1, 6),
    intArrayOf(1, 3, 6, 1, 6, 10, 3, 8, 6, 5, 6, 9, 8, 9, 6),
    intArrayOf(10, 1, 0, 10, 0, 6, 9, 5, 0, 5, 6, 0),
    intArrayOf(0, 3, 8, 5, 6, 10),
    intArrayOf(10, 5, 6),
    intArrayOf(11, 5, 10, 7, 5, 11),
    intArrayOf(11, 5, 10, 11, 7, 5, 8, 3, 0),
    intArrayOf(5, 11, 7, 5, 10, 11, 1, 9, 0),
    intArrayOf(10, 7, 5, 10, 11, 7, 9, 8, 1, 8, 3, 1),
    intArrayOf(11, 1, 2, 11, 7, 1, 7, 5, 1),
    intArrayOf(0, 8, 3, 1, 2, 7, 1, 7, 5, 7, 2, 11),
    intArrayOf(9, 7, 5, 9, 2, 7, 9, 0, 2, 2, 11, 7),
    intArrayOf(7, 5, 2, 7, 2, 11, 5, 9, 2, 3, 2, 8, 9, 8, 2),
    intArrayOf(2, 5, 10, 2, 3, 5, 3, 7, 5),
    intArrayOf(8, 2, 0, 8, 5, 2, 8, 7, 5, 10, 2, 5),
    intArrayOf(9, 0, 1, 5, 10, 3, 5, 3, 7, 3, 10, 2),
    intArrayOf(9, 8, 2, 9, 2, 1, 8, 7, 2, 10, 2, 5, 7, 5, 2),
    intArrayOf(1, 3, 5, 3, 7, 5),
    intArrayOf(0, 8, 7, 0, 7, 1, 1, 7, 5),
    intArrayOf(9, 0, 3, 9, 3, 5, 5, 3, 7),
    intArrayOf(9, 8, 7, 5, 9, 7),
    intArrayOf(5, 8, 4, 5, 10, 8, 10, 11, 8),
    intArrayOf(5, 0, 4, 5, 11, 0, 5, 10, 11, 11, 3, 0),
    intArrayOf(0, 1, 9, 8, 4, 10, 8, 10, 11, 10, 4, 5),
    intArrayOf(10, 11, 4, 10, 4, 5, 11, 3, 4, 9, 4, 1, 3, 1, 4),
    intArrayOf(2, 5, 1, 2, 8, 5, 2, 11, 8, 4, 5, 8),
    intArrayOf(0, 4, 11, 0, 11, 3, 4, 5, 11, 2, 11, 1, 5, 1, 11),
    intArrayOf(0, 2, 5, 0, 5, 9, 2, 11, 5, 4, 5, 8, 11, 8, 5),
    intArrayOf(9, 4, 5, 2, 11, 3),
    intArrayOf(2, 5, 10, 3, 5, 2, 3, 4, 5, 3, 8, 4),
    intArrayOf(5, 10, 2, 5, 2, 4, 4, 2, 0),
    intArrayOf(3, 10, 2, 3, 5, 10, 3, 8, 5, 4, 5, 8, 0, 1, 9),
    intArrayOf(5, 10, 2, 5, 2, 4, 1, 9, 2, 9, 4, 2),
    intArrayOf(8, 4, 5, 8, 5, 3, 3, 5, 1),
    intArrayOf(0, 4, 5, 1, 0, 5),
    intArrayOf(8, 4, 5, 8, 5, 3, 9, 0, 5, 0, 3, 5),
    intArrayOf(9, 4, 5),
    intArrayOf(4, 11, 7, 4, 9, 11, 9, 10, 11),
    intArrayOf(0, 8, 3, 4, 9, 7, 9, 11, 7, 9, 10, 11),
    intArrayOf(1, 10, 11, 1, 11, 4, 1, 4, 0, 7, 4, 11),
    intArrayOf(3, 1, 4, 3, 4, 8, 1, 10, 4, 7, 4, 11, 10, 11, 4),
    intArrayOf(4, 11, 7, 9, 11, 4, 9, 2, 11, 9, 1, 2),
    intArrayOf(9, 7, 4, 9, 11, 7, 9, 1, 11, 2, 11, 1, 0, 8, 3),
    intArrayOf(11, 7, 4, 11, 4, 2, 2, 4, 0),
    intArrayOf(11, 7, 4, 11, 4, 2, 8, 3, 4, 3, 2, 4),
    intArrayOf(2, 9, 10, 2, 7, 9, 2, 3, 7, 7, 4, 9),
    intArrayOf(9, 10, 7, 9, 7, 4, 10, 2, 7, 8, 7, 0, 2, 0, 7),
    intArrayOf(3, 7, 10, 3, 10, 2, 7, 4, 10, 1, 10, 0, 4, 0, 10),
    intArrayOf(1, 10, 2, 8, 7, 4),
    intArrayOf(4, 9, 1, 4, 1, 7, 7, 1, 3),
    intArrayOf(4, 9, 1, 4, 1, 7, 0, 8, 1, 8, 7, 1),
    intArrayOf(4, 0, 3, 7, 4, 3),
    intArrayOf(4, 8, 7),
    intArrayOf(9, 10, 8, 10, 11, 8),
    intArrayOf(3, 0, 9, 3, 9, 11, 11, 9, 10),
    intArrayOf(0, 1, 10, 0, 10, 8, 8, 10, 11),
    intArrayOf(3, 1, 10, 11, 3, 10),
    intArrayOf(1, 2, 11, 1, 11, 9, 9, 11, 8),
    intArrayOf(3, 0, 9, 3, 9, 11, 1, 2, 9, 2, 11, 9),
    intArrayOf(0, 2, 11, 8, 0, 11),
    intArrayOf(3, 2, 11),
    intArrayOf(2, 3, 8, 2, 8, 10, 10, 8, 9),
    intArrayOf(9, 10, 2, 0, 9, 2),
    intArrayOf(2, 3, 8, 2, 8, 10, 0, 1, 8, 1, 10, 8),
    intArrayOf(1, 10, 2),
    intArrayOf(1, 3, 8, 9, 1, 8),
    intArrayOf(0, 9, 1),
    intArrayOf(0, 3, 8),
    intArrayOf()
)

val Offset = arrayOf(
    Vec3Int(0, 0, 0),
    Vec3Int(1, 0, 0),
    Vec3Int(1, 0, 1),
    Vec3Int(0, 0, 1),
    Vec3Int(0, 1, 0),
    Vec3Int(1, 1, 0),
    Vec3Int(1, 1, 1),
    Vec3Int(0, 1, 1),
)

data class Vec3Int(val x: Int, val y: Int, val z: Int){
    operator fun plus(other: Vec3Int) : Vec3Int{
        return Vec3Int(x + other.x, y + other.y, z + other.z)
    }
    operator fun minus(other: Vec3Int) : Vec3Int{
        return Vec3Int(x - other.x, y - other.y, z - other.z)
    }

    operator fun times(factor: Double) : Vec3{
        return Vec3(x * factor, y  * factor, z  * factor)
    }
}

class RenderVertex(val position: Vec3, val normal: Vec3)
class Triangle(val a: RenderVertex, val b: RenderVertex, val c: RenderVertex)

const val IsoLevel = 0.0
class MarchingCubes{

    val resolution = 0.1//mm
    fun calculateNormal(coord: Vec3Int): Vec3 {
        val offsetX = Vec3Int(1, 0, 0)
        val offsetY = Vec3Int(0, 1, 0)
        val offsetZ = Vec3Int(0, 0, 1)
        val dx = sampleDensity(coord + offsetX) - sampleDensity(coord - offsetX)
        val dy = sampleDensity(coord + offsetY) - sampleDensity(coord - offsetY)
        val dz = sampleDensity(coord + offsetZ) - sampleDensity(coord - offsetZ)
        return Vec3(dx, dy, dz).normalized()
    }

    fun coordToWorld(coord: Vec3Int): Vec3 {
        return coord * resolution
    }

    fun createVertex(lhs: Vec3Int, rhs: Vec3Int) : RenderVertex{
        val posA = coordToWorld(lhs)
        val posB = coordToWorld(rhs)
        val densityA = sampleDensity(lhs)
        val densityB = sampleDensity(rhs)
        // Interpolate between the two corner points based on the density

        val t = (IsoLevel -densityA) / (densityB - densityA)
        val position = posA + (posB - posA) * t

        val normalA = calculateNormal(lhs)
        val normalB = calculateNormal(rhs)
        val normal = (normalA + (normalB - normalA) * t).normalized()

        return RenderVertex(position, normal)
    }

    fun sampleDensity(idx: Vec3Int) : Double{
         return 1.0
    }

    fun process(coord : Vec3Int){
        var variant = 0
        for (idx in 0 until 8) {
            // Think of the configuration as an 8-bit binary number (each bit represents the state of a corner point).
            // The state of each corner point is either 0: above the surface, or 1: below the surface.
            // The code below sets the corresponding bit to 1, if the point is below the surface.
            if (sampleDensity(coord + Offset[idx]) < IsoLevel) {
                variant = variant or (1 shl idx)
            }
        }

        processCube(coord, variant)
    }
    fun processCube(coord : Vec3Int, variant : Int){
        val edgeIndices = Triangulation[variant]
        val triangles = arrayListOf<Triangle>()
        for (edgeIdx in edgeIndices.indices) {
            val i = edgeIdx * 3

            val edgeIndexA = edgeIndices[i]
            val a0 = CornerIndexAFromEdge[edgeIndexA]
            val a1 = CornerIndexBFromEdge[edgeIndexA]
            val edgeIndexB = edgeIndices[i + 1]
            val b0 = CornerIndexAFromEdge[edgeIndexB]
            val b1 = CornerIndexBFromEdge[edgeIndexB]
            val edgeIndexC = edgeIndices[i + 2]
            val c0 = CornerIndexAFromEdge[edgeIndexC]
            val c1 = CornerIndexBFromEdge[edgeIndexC]

            // Calculate positions of each vertex.
            val vertexA = createVertex(coord + Offset[a0], coord + Offset[a1])
            val vertexB = createVertex(coord + Offset[b0], coord + Offset[b1])
            val vertexC = createVertex(coord + Offset[c0], coord + Offset[c1])

            // Create triangle
            val tri = Triangle(vertexC, vertexB, vertexA)
            triangles.add(tri)
        }
    }
}