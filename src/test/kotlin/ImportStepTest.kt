import com.codecad.core.import.step.StepImporter
import com.codecad.core.import.step.parser.StepParser
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.StandardCharsets

class ImportStepTest {


    @Test
    fun importTest(){
        val text = File("vakuum.step").readText(StandardCharsets.UTF_8)
        val stepFile = StepParser.parse(text)

        val importer = StepImporter()
        val context = importer.import(stepFile)


        println()
    }
}