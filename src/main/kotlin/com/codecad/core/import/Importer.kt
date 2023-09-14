package com.codecad.core.import

import com.codecad.core.part.Context
import java.io.InputStream
import java.nio.ByteBuffer


interface Importer {
    fun import(buffer: ByteBuffer): Context
}