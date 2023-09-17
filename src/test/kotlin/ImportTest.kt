import com.codecad.core.import.StlImporter
import com.codecad.core.mesh.Solidify
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.ByteBuffer

class ImportTest {

    @Test
    fun importTest(){
        val stlImporter = StlImporter()
        val bytes = File("testStl.stl").readBytes()
        val buffer = ByteBuffer.wrap(bytes)
        val context = stlImporter.import(buffer)

        var obj = context.volumes.first()
        obj = Solidify.mergePlaneFaces(obj)

        println(obj.toString())
        //ExportHelper.saveStep(obj.shells.first().faces[4], "stl")
        ExportHelper.saveStep(obj, "stl")
    }
}