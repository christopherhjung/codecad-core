package com.codecad.core.export

import com.codecad.core.mesh.Mesh
import com.codecad.core.part.Context


interface ModelExport {
    fun export(context: Context): ByteArray
}