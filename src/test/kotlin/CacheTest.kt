import com.codecad.core.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class CacheTest {

    @Test
    fun cacheTest(){
        val a = Param(2.0)
        val b = Param(3.0)
        assertSame(a + b, a + b)
        assertSame(a * b, a * b)
        assertSame(a / b, a / b)
        assertSame(a.pow(b), a.pow(b))
        assertSame(a.smaller(b), a.smaller(b))
    }

    @Test
    fun commuTest(){
        val a = Param(2.0)
        val b = Param(3.0)
        assertSame(a + b, b + a)
        assertSame(a * b, b * a)
        assertNotSame(a / b, b / a)
        assertNotSame(a.pow(b), b.pow(a))
        assertNotSame(a.smaller(b), b.smaller(a))
    }

    @Test
    fun equalsTest(){
        val a = Param(2.0)
        val b = Param(3.0)
        assertEquals(AddExpr(a,b), AddExpr(a,b))
        assertEquals(TimesExpr(a,b), TimesExpr(a,b))
        assertEquals(DivExpr(a,b), DivExpr(a,b))
        assertEquals(PowExpr(a,b), PowExpr(a,b))
    }

    @Test
    fun addSimplification(){
        val a = Param(2.0)
        assertSame(a+a, 2.0 * a)
    }

    @Test
    fun timesSimplification(){
        val a = Param(2.0)
        assertSame(a*a, a.pow(2))
    }

    @Test
    fun complexCacheTest(){
        val a = Param(2.0)
        val b = Param(3.0)
        val c = Param(3.0)
        assertSame(a + b * c, c * b + a )
    }
}
