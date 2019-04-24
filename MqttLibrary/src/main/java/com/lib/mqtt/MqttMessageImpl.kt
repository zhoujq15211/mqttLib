package com.lib.mqtt

import org.eclipse.paho.client.mqttv3.MqttMessage

class MqttMessageImpl : MqttMessage() {
    var result: Int = 0
    var serialNumber: Int = 0
    var topic: String? = null
    var createTime: Long = 0
    var timeout: Int = 15 * 1000
    var sendResult: ((MqttMessageImpl) -> Unit)? = null
    fun isEmpty(): Boolean {
        return payload == null || payload.isEmpty() || topic == null || topic?.isEmpty() ?: true
    }

    fun isTimeout(): Boolean {
        return createTime > 0 && System.currentTimeMillis() - createTime > timeout
    }

    override fun toString(): String {
        return "MqttMessageImpl(result=$result, serialNumber=$serialNumber, topic=$topic)"
    }


}
