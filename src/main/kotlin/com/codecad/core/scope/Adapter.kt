package com.codecad.core.scope

import com.codecad.core.Project
import com.codecad.core.Sketch
import com.codecad.core.sketch.World


var Scope.project : Project
    get() = getObject("\$project") as Project
    set(value) {setObject("\$project", value, true)}

var Scope.sketch : Sketch
    get() = getObject("\$sketch") as Sketch
    set(value) {setObject("\$sketch", value, true)}

var Scope.world : World
    get() = getObject("\$world") as World
    set(value) {setObject("\$world", value, true)}