import com.codecad.core.Executor
import com.codecad.core.Util
import org.junit.jupiter.api.Test


class ExecutorTest {

    @Test
    fun executorTest(){
        val result = Executor.execute(Util.load("offset.kts"))
        println(result)
    }

}
