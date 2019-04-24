package com.lib.mqtt

interface MqttReceiveCallback {
    fun onMqttReceive(topicName: String, data: ByteArray)
}
