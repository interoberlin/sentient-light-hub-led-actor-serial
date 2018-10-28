package berlin.intero.sentientlighthub.ledactor.tasks.async

import berlin.intero.sentientlighthub.common.SentientProperties
import berlin.intero.sentientlighthub.common.tasks.SerialDiscoverPortsAsyncTask
import berlin.intero.sentientlighthub.common.tasks.SerialPortCloseAsyncTask
import berlin.intero.sentientlighthub.common.tasks.SerialPortOpenAsyncTask
import berlin.intero.sentientlighthub.common.tasks.SerialSetLEDAsyncTask
import org.springframework.core.task.SyncTaskExecutor
import java.util.logging.Logger

class SampleLEDAsyncTask : Runnable {

    companion object {
        private val log: Logger = Logger.getLogger(SampleLEDAsyncTask::class.simpleName)
    }

    override fun run() {
        log.info("${SentientProperties.Color.TASK}-- SAMPLE LED ASYNC${SentientProperties.Color.RESET}")


        // val portName = "COM8" // Windows
        val portName = "/dev/ttyUSB1" // Linux
        val repeatCount = 10
        val ledCount = 21

        SyncTaskExecutor().execute(SerialDiscoverPortsAsyncTask())
        SyncTaskExecutor().execute(SerialPortOpenAsyncTask(portName))

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
        SyncTaskExecutor().execute(SerialPortCloseAsyncTask(portName))
    }
}