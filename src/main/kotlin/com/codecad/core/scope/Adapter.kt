package com.codecad.core.scope

import com.codecad.core.World
import com.codecad.core.part.PartStudio
import com.codecad.core.part.Sketch


var Scope.partStudio : PartStudio
    get() = getObject("\$project") as PartStudio
    set(value) {setObject("\$project", value, true)}

var Scope.sketch : Sketch
    get() = getObject("\$sketch") as Sketch
    set(value) {setObject("\$sketch", value, true)}

var Scope.world : World
    get() = getObject("\$world") as World
    set(value) {setObject("\$world", value, true)}