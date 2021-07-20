import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
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
    fun equalsTest(){
        val a = Parameter(2.0)
        val b = Parameter(3.0)
        assertEquals(AddValue(a,b), AddValue(a,b))
        assertEquals(TimesValue(a,b), TimesValue(a,b))
        assertEquals(DivValue(a,b), DivValue(a,b))
        assertEquals(PowValue(a,b), PowValue(a,b))
    }

    @Test
    fun equalsTest2(){
        val a = Parameter(2.0)
        assertEquals(a+a, 2.0 * a)
    }
}
