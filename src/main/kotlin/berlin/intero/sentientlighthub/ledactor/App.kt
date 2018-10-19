package berlin.intero.sentientlighthub.ledactor

import berlin.intero.sentientlighthub.common.tasks.SerialPortOpenAsyncTask
import berlin.intero.sentientlighthub.common.tasks.SerialSetLEDAsyncTask
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.core.task.SyncTaskExecutor
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.logging.Logger

@SpringBootApplication
@EnableScheduling
class App

fun main(args: Array<String>) {
    runApplication<App>(*args)
    val log = Logger.getLogger(App::class.simpleName)

    log.info("Sentient Light Hub LED Actor (serial)")

    // val portName = "COM8" // Windows
    val portName = "/dev/ttyUSB1" // Linux
    val repeatCount = 10
    val ledCount = 10

    SyncTaskExecutor().execute(SerialPortOpenAsyncTask(portName))
    Thread.sleep(5000)

    repeat(repeatCount) {
        repeat(ledCount) {
            SyncTaskExecutor().execute(SerialSetLEDAsyncTask(portName, (it + 1).toShort(), 0x7F, 0x7F, 0x7F))
            SyncTaskExecutor().execute(SerialSetLEDAsyncTask(portName, (it).toShort(), 0x00, 0x00, 0x00))
        }
        repeat(ledCount) {
            SyncTaskExecutor().execute(SerialSetLEDAsyncTask(portName, (ledCount - it).toShort(), 0x7F, 0x7F, 0x7F))
            SyncTaskExecutor().execute(SerialSetLEDAsyncTask(portName, (ledCount - it + 1).toShort(), 0x00, 0x00, 0x00))
        }
    }

    Thread.sleep(5000)
    SyncTaskExecutor().execute(SerialPortOpenAsyncTask(portName))
}
