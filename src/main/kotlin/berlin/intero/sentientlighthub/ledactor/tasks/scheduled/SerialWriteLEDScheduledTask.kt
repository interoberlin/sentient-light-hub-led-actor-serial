package berlin.intero.sentientlighthub.ledactor.tasks.scheduled

import berlin.intero.sentientlighthub.common.SentientProperties
import berlin.intero.sentientlighthub.common.model.payload.SingleLEDPayload
import berlin.intero.sentientlighthub.common.services.ConfigurationService
import berlin.intero.sentientlighthub.common.services.PayloadConverterService
import berlin.intero.sentientlighthub.common.tasks.MQTTSubscribeAsyncTask
import berlin.intero.sentientlighthub.common.tasks.SerialPortOpenAsyncTask
import berlin.intero.sentientlighthub.common.tasks.SerialSetLEDAsyncTask
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.core.task.SyncTaskExecutor
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.logging.Logger

/**
 * This scheduled task
 * <li> calls {@link MQTTSubscribeAsyncTask} to subscribe mapping values from MQTT broker
 * <li> calls {@link SerialSetLEDAsyncTask} to write values via serial port
 */
@Component
class SerialWriteLEDScheduledTask {
    val values: MutableMap<String, String> = HashMap()
    val valuesHistoric: MutableMap<String, String> = HashMap()

    companion object {
        private val log: Logger = Logger.getLogger(SerialWriteLEDScheduledTask::class.simpleName)
    }

    init {
        val topic = "${SentientProperties.MQTT.Topic.LED}/#"
        val callback = object : MqttCallback {
            override fun messageArrived(topic: String, message: MqttMessage) {
                log.fine("MQTT value receiced")

                // Parse payload
                val payload = String(message.payload)
                val value = Gson().fromJson(payload, SingleLEDPayload::class.java)

                // Determine identifier
                val identifier = "${value.stripId}/${value.ledId}"

                values[identifier] = payload
            }

            override fun connectionLost(cause: Throwable?) {
                log.fine("MQTT connection lost")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                log.fine("MQTT delivery complete")
            }
        }

        // Call SerialPortOpenAsyncTask for all serial devices
        ConfigurationService.getSerialActors()?.forEach { actor ->
            SyncTaskExecutor().execute(SerialPortOpenAsyncTask(actor.port))
        }

        // Call MQTTSubscribeAsyncTask
        SimpleAsyncTaskExecutor().execute(MQTTSubscribeAsyncTask(topic, callback))
    }

    @Scheduled(fixedDelay = SentientProperties.Frequency.SENTIENT_WRITE_DELAY)
    @SuppressWarnings("unused")
    fun write() {
        log.fine("${SentientProperties.Color.TASK}-- SERIAL WRITE LED TASK${SentientProperties.Color.RESET}")

        values.forEach { identifier, payload ->

            // Parse payload
            val value = Gson().fromJson(payload, SingleLEDPayload::class.java)

            try {
                val stripId = value.stripId
                val ledId = value.ledId

                val actor = ConfigurationService.getActor(stripId, ledId)

                if (actor != null && payload != valuesHistoric.get(identifier)) {
                    valuesHistoric.set(identifier, payload)

                    val portName = actor.port

                    // Call SerialSetLEDAsyncTask
                    log.info("${SentientProperties.Color.TASK}Send${SentientProperties.Color.RESET} ${portName} ${ledId} : [${value.warmWhite} ${value.coldWhite} ${value.amber}]")

                    SyncTaskExecutor().execute(SerialSetLEDAsyncTask(
                            portName,
                            ledId.toShort(),
                            PayloadConverterService.convertStringToByte(value.warmWhite),
                            PayloadConverterService.convertStringToByte(value.coldWhite),
                            PayloadConverterService.convertStringToByte(value.amber)))
                }
            } catch (jse: JsonSyntaxException) {
                log.severe("${SentientProperties.Color.ERROR}${jse}${SentientProperties.Color.RESET}")
            }
        }
    }
}
