import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class CacheTest {

    @Test
    fun cacheTest(){
        val a = Parameter(2.0)
        val b = Parameter(3.0)
        assertSame(a + b, a + b)
        assertSame(a * b, a * b)
        assertSame(a / b, a / b)
        assertSame(a.pow(b), a.pow(b))
        assertSame(a.smaller(b), a.smaller(b))
    }

    @Test
    fun commuTest(){
        val a = Parameter(2.0)
        val b = Parameter(3.0)
        assertSame(a + b, b + a)
        assertSame(a * b, b * a)
        assertNotSame(a / b, b / a)
        assertNotSame(a.pow(b), b.pow(a))
        assertNotSame(a.smaller(b), b.smaller(a))
    }

    @Test
    fun equalsTest(){
        val a = Parameter(2.0)
        val b = Parameter(3.0)
        assertEquals(AddValue(a,b), AddValue(a,b))
        assertEquals(TimesValue(a,b), TimesValue(a,b))
        assertEquals(DivValue(a,b), DivValue(a,b))
        assertEquals(PowValue(a,b), PowValue(a,b))
    }

    @Test
    fun addSimplification(){
        val a = Parameter(2.0)
        assertSame(a+a, 2.0 * a)
    }

    @Test
    fun timesSimplification(){
        val a = Parameter(2.0)
        assertSame(a*a, a.pow(2))
    }

    @Test
    fun complexCacheTest(){
        val a = Parameter(2.0)
        val b = Parameter(3.0)
        val c = Parameter(3.0)
        assertSame(a + b * c, a + b * c )
    }
}
