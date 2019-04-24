package com.lib.mqtt

import android.annotation.SuppressLint
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.android.Main
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.lang.Runnable
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * @version: 1.0.0
 * @Description: MqttAsyncClient二次封装
 * @Author: zhoujq on  2019/4/16 16:12
 */
class MqttClientImpl : MqttCallback {
    private val TAG = this.javaClass.simpleName
    private var client: MqttAsyncClient? = null
    val options: MqttConnectOptions = MqttConnectOptions()
    var mClientId: String = ""
    var mServerHost: String = ""
    private var connting: Boolean = false
    var openLog: Boolean = false
    var connectCallback: ((Boolean, String?) -> Unit)? = null
    var receiveCallback: ((String, ByteArray) -> Unit)? = null
    val subscribTopics = Hashtable<String, Int>(0)
    @SuppressLint("UseSparseArrays")
    val sendQueue = ConcurrentHashMap<Int, MqttMessageImpl>()
    val waitQueue = ConcurrentHashMap<Int, MqttMessageImpl>()
    private var serialNumber: Int = 0
    private val mHandler = Handler()
    var sendDelay: Long = 100
    var timeoutCheckDelay: Long = 5000
    private var startCheckTimeout = false

    init {
        printLog("$mServerHost==${options.userName}==$mClientId")
        // MQTT的连接设置
        // 设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，这里设置为true表示每次连接到服务器都以新的身份连接
        options.isCleanSession = true
        // 设置超时时间 单位为秒
        options.connectionTimeout = 15 * 1000
        //心跳时间,单位秒
        options.keepAliveInterval = 30
        //自动重连,每次连接都会增加下次重连间隔
        options.isAutomaticReconnect = false
        options.maxInflight = 10000
    }

    /**
     * 判断服务是否连接
     *
     * @return
     */
    fun isConnected(): Boolean {
        return client?.isConnected ?: false
    }


    /**
     * 连接服务器
     */
    fun connect() {
        if (connting) {
            return
        }
        connting = true
        try {
            if (TextUtils.isEmpty(mServerHost) || TextUtils.isEmpty(options.userName) || TextUtils.isEmpty(mClientId)) {
                connting = false
                connectChanged(false, "ServerHost or userName or mClientId is empty?")
                return
            }
            if ("" == mClientId) {
                mClientId = MqttClient.generateClientId()
            }
            if (options.socketFactory != null) {
                client = MqttAsyncClient("ssl://$mServerHost", mClientId, MemoryPersistence())
            } else {
                client = MqttAsyncClient("tcp://$mServerHost", mClientId, MemoryPersistence())
            }
            client?.setCallback(this)
            client?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    printLog("连接成功:$mServerHost==$mClientId")
                    connting = false
                    connectChanged(true, "")
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    exception.printStackTrace()
                    printLog("连接失败:$mServerHost==$mClientId==$exception")
                    connting = false
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
            printLog("连接失败:$mServerHost==$mClientId==$e")
            connting = false
        }
    }


    /**
     * 订阅消息
     *
     * @param topics 订阅消息的主题
     */
    fun subscribe(topics: Map<String, Int>) {
        val it = topics.keys.iterator()
        while (it.hasNext()) {
            val t = it.next()
            val i = topics[t]
            if (i != null) {
                subscribe(t, i)
            }
        }
    }

    /**
     * 订阅主题
     *
     * @param topic
     * @param qos
     */
    fun subscribe(topic: String, qos: Int) {
        subscribTopics[topic] = qos
        GlobalScope.launch(Dispatchers.IO) {
            if (isConnected()) {
                try {
                    client?.subscribe(topic, qos)
                    printLog("开始订阅topic=$topic")
                } catch (e: MqttException) {
                    e.printStackTrace()
                    printLog("订阅失败:$topic")
                }

            }
        }
    }

    /**
     * 取消订阅
     *
     * @param topics
     */
    fun unsubscribe(topics: Map<String, Int>) {
        val it = topics.keys.iterator()
        while (it.hasNext()) {
            val t = it.next()
            unsubscribe(t)
        }
    }

    /**
     * 取消订阅
     *
     * @param topic
     */
    fun unsubscribe(topic: String) {
        subscribTopics.remove(topic)
        GlobalScope.launch(Dispatchers.IO) {
            if (isConnected()) {
                try {
                    printLog("取消订阅topic=$topic")
                    client?.unsubscribe(topic)
                } catch (e: MqttException) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 通过keyWork取消订阅主题
     *
     * @param keyWork
     */
    fun unsubscribeByKey(keyWork: String) {
        val it = subscribTopics.entries.iterator()
        while (it.hasNext()) {
            val item = it.next()
            val topic = item.key
            if (topic != null && topic.contains(keyWork)) {
                unsubscribe(topic)
                it.remove()
            }
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        connectCallback = null
        receiveCallback = null
        if (client != null) {
            client?.setCallback(null)
        }
        startCheckTimeout = false
        disconnect()
    }

    /**
     * 断开链接
     */
    fun disconnect() {
        mHandler.removeCallbacksAndMessages(null)
        subscribTopics.clear()
        if (client?.isConnected == true) {
            try {
                client?.disconnect()
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }

    private fun sendResult(command: MqttMessageImpl, res: Int) {
        if (command.sendResult != null) {
            GlobalScope.launch(Dispatchers.Main) {
                command.result = res
                command.sendResult?.invoke(command)
            }
        }
    }

    private fun connectChanged(connect: Boolean, cause: String) {
        if (connectCallback != null) {
            GlobalScope.launch(Dispatchers.Main) {
                connectCallback?.invoke(connect, cause)
            }
        }
    }

    /**
     * 发布消息
     */
    fun publish(message: MqttMessageImpl) {
        serialNumber++
        if (serialNumber == Int.MAX_VALUE) {
            serialNumber = 0
        }
        message.serialNumber = serialNumber
        if (message.isEmpty()) {
            sendResult(message, MqttErrorCode.ARGUMENT_EMPTY)
            return
        }
        if (!isConnected()) {
            sendResult(message, MqttErrorCode.DISCONNCECT)
        } else {
            sendQueue[serialNumber] = message
            if (sendQueue.size == 1) {
                mHandler.postDelayed(mSendRunnable, sendDelay)
            }
        }
        checkoutTimeout()
    }

    private fun doPublish(message: MqttMessageImpl) {
        Log.i("Mqtt", "doPublish:" + message.topic)
        if (!isConnected()) {
            sendResult(message, MqttErrorCode.DISCONNCECT)
            mHandler.postDelayed(mSendRunnable, sendDelay)
        } else {
            try {
                sendQueue.remove(message.serialNumber)
                client?.publish(message.topic, message, message.serialNumber, publishCallback)
                waitQueue[message.serialNumber] = message
            } catch (e: MqttException) {
                e.printStackTrace()
                sendResult(message, MqttErrorCode.EXCEPTION)
            } finally {
                mHandler.postDelayed(mSendRunnable, sendDelay)
            }
        }
    }

    private val mSendRunnable = Runnable {
        if (sendQueue.isNotEmpty()) {
            //每次都发送list里面第一个
            val mCurrentCommand = sendQueue.values.iterator().next()
            mCurrentCommand.createTime = System.currentTimeMillis()
            doPublish(mCurrentCommand)
        }
    }

    private fun checkoutTimeout() {
        if (!startCheckTimeout) {
            startCheckTimeout = true
            GlobalScope.launch(Dispatchers.IO) {
                while (startCheckTimeout) {
                    val it = waitQueue.values.iterator()
                    while (it.hasNext()) {
                        val msg = it.next()
                        if (msg.isTimeout()) {
                            sendResult(msg, MqttErrorCode.TIMEOUT)
                            it.remove()
                        }
                    }
                    delay(timeoutCheckDelay)
                }
            }
        }
    }

    private val publishCallback = object : IMqttActionListener {
        override fun onSuccess(asyncActionToken: IMqttToken) {
            Log.i("Mqtt", "publish onSuccess:" + asyncActionToken.userContext)
            val message = waitQueue.remove(asyncActionToken.userContext as Int)
            if (message != null) {
                if (asyncActionToken.isComplete) {
                    sendResult(message, MqttErrorCode.SUCCESS)
                } else {
                    sendResult(message, MqttErrorCode.FAIL)
                }
            }
        }

        override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
            Log.i("Mqtt", "publish onFailure:$exception")
            val message = sendQueue.remove(asyncActionToken.userContext as Int)
            if (message != null) {
                sendResult(message, MqttErrorCode.EXCEPTION)
            }
        }
    }

    private fun printLog(log: String) {
        if (openLog) {
            Log.i(TAG, log)
        }
    }
    //*************发布和订阅消息的回调************************

    override fun deliveryComplete(token: IMqttDeliveryToken) {
        //publish后异步回调处理，不到这里
        printLog("deliveryComplete==" + token.isComplete + "===" + token.userContext)
    }

    override fun messageArrived(topicName: String, message: MqttMessage) {
        //subscribe后得到的消息会执行到这里面
        printLog("messageArrived==" + topicName + ":" + String(message.payload))
        receiveCallback ?: GlobalScope.launch(Dispatchers.Main) {
            receiveCallback?.invoke(topicName, message.payload)
        }
    }

    override fun connectionLost(cause: Throwable) {
        //连接断开
        printLog("connectionLost：$cause")
        connectChanged(false, cause.toString())
    }
    //*******************************************************************
}

