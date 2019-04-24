package com.lib.mqtt

interface MqttConnectCallback {
    fun onConnectChange(connect: Boolean, msg: String)
}
