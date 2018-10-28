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
                values[topic] = String(message.payload)
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

        values.forEach { topic, value ->

            try {
                // Parse payload
                val payload = Gson().fromJson(value, SingleLEDPayload::class.java)
                log.fine("payload ${payload}")
                log.fine("payload stripID ${payload.stripId}")
                log.fine("payload ledID ${payload.ledId}")
                log.fine("payload warmWhite ${payload.warmWhite}")
                log.fine("payload coldWhite ${payload.coldWhite}")
                log.fine("payload amber ${payload.amber}")

                val stripId = payload.stripId
                val ledId = payload.ledId

                val actor = ConfigurationService.getActor(stripId, ledId)

                log.fine("${SentientProperties.Color.VALUE}topic $topic / val $value / strip $stripId / ledID $ledId / actor ${actor?.port} ${SentientProperties.Color.RESET}")

                if (actor != null && value != valuesHistoric.get(topic)) {
                    valuesHistoric.set(topic, value)
                    val portName = actor.port

                    // Call SerialSetLEDAsyncTask
                    SyncTaskExecutor().execute(SerialSetLEDAsyncTask(
                            portName,
                            ledId.toShort(),
                            PayloadConverterService.convertStringToByte(payload.warmWhite),
                            PayloadConverterService.convertStringToByte(payload.coldWhite),
                            PayloadConverterService.convertStringToByte(payload.amber)))
                }
            } catch (jse: JsonSyntaxException) {
                log.severe("${SentientProperties.Color.ERROR}${jse}${SentientProperties.Color.RESET}")
            }
        }
    }
}
