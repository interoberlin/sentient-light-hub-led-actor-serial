package berlin.intero.sentientlighthub.ledactor

import berlin.intero.sentientlighthub.common.tasks.SerialDiscoverPortsAsyncTask
import berlin.intero.sentientlighthub.common.tasks.SerialInitializeAsyncTask
import berlin.intero.sentientlighthub.common.tasks.SerialSendByteAsyncTask
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.logging.Logger

@SpringBootApplication
@EnableScheduling
class App

fun main(args: Array<String>) {
    runApplication<App>(*args)
    val log = Logger.getLogger(App::class.simpleName)

    log.info("Sentient Light Hub LED Actor (serial)")

    val portName = "COM8"

    SimpleAsyncTaskExecutor().execute(SerialDiscoverPortsAsyncTask())
    SimpleAsyncTaskExecutor().execute(SerialInitializeAsyncTask(portName))

    while (true) {
        SimpleAsyncTaskExecutor().execute(SerialSendByteAsyncTask(portName, 0x00))
        Thread.sleep(1000)
        SimpleAsyncTaskExecutor().execute(SerialSendByteAsyncTask(portName, 0x01))
        Thread.sleep(1000)
    }
}
