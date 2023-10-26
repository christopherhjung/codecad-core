
import com.codecad.core.invSqrt
import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.test.assertEquals

class InvSqrtTest {
    @Test
    fun importTest2(){
        assertEquals(1.0 / sqrt(2.0), invSqrt(2.0), 1e-10)
    }
}