package com.lib.mqtt

interface MqttSendCallback {
    fun onSendComplete(command: MqttMessageImpl)
}
