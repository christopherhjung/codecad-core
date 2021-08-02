import com.codecad.core.Executor
import org.junit.jupiter.api.Test


class ExecutorTest {

    @Test
    fun executorTest(){
        System.setSecurityManager(SecurityManager())
        try{
            Executor.execute("System.exit(-1)")
        }catch (e: Throwable){
            e.printStackTrace()
        }
    }

}
