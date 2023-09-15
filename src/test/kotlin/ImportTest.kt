import com.codecad.core.import.StlImporter
import com.codecad.core.mesh.Solidify
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.ByteBuffer

class ImportTest {

    @Test
    fun importTest(){
        val stlImporter = StlImporter()
        val bytes = File("cyl.stl").readBytes()
        val buffer = ByteBuffer.wrap(bytes)
        val context = stlImporter.import(buffer)

        val solid = Solidify.mergePlaneFaces(context.volumes.first())

        ExportHelper.saveStep(solid, "stl")
    }
}