package com.codecad.core.test

import com.codecad.common.PointD

class CircleList {
    val init : CircleListItem? = null

    fun add(p : PointD){

    }
}

class CircleListItem(val value : PointD){
    var previous : CircleListItem? = null
    var next : CircleListItem? = null

}


