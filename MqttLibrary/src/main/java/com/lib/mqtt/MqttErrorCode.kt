package com.lib.mqtt

interface MqttErrorCode {
    companion object {
        val FAIL = -1
        val SUCCESS = 0
        val CONNCECT = 1
        val DISCONNCECT = 2
        val ARGUMENT_EMPTY = 3
        val EXCEPTION = 4
        val TIMEOUT = 5
    }
}
