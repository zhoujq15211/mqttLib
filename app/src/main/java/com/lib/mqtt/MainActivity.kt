package com.lib.mqtt

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

/**
 * mqtt库使用demo
 */
class MainActivity : AppCompatActivity(), View.OnClickListener {
    val client: MqttClientImpl = MqttClientImpl()
    val topics = HashMap<String, Int>(0)
    val testCount = 1000
    var count = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        client.mServerHost = "ServerHost"
        //是否需要ssl加密
        client.options.socketFactory = null
        //打开log
        client.openLog = true
        //连接状态回调
        client.connectCallback = connectCallback
        //收到数据回调
        client.receiveCallback = receiveCallback
        //指令发送间隔，内部使用顺序发送，不设置间隔时间mqttv3库会收不到发送结果
        client.sendDelay = 100
    }

    override fun onDestroy() {
        super.onDestroy()
        client.release()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.button_connect -> {
                if (client.isConnected()) {
                    client.disconnect()
                    button_connect.text = "connect"
                } else {
                    docConnect()
                }
            }
            R.id.button_subscribe -> {
                client.subscribe(topics)
            }
            R.id.button_unsubscribe -> {
                client.unsubscribe(topics)
            }
            R.id.button_send -> {
                //test publish
                count = 0
                for (i in 0..testCount) {
                    for (t in topics.keys) {
                        val message = MqttMessageImpl()
                        message.topic = t
                        message.qos = 0
                        message.payload = "test".toByteArray()
                        message.sendResult = sendResult
                        client.publish(message)
                    }
                }
            }
        }
    }

    private fun docConnect() {
        client.options.userName = ""
        client.options.password = "".toCharArray()
        client.mClientId = ""
        topics["/topics"] = 0
        client.connect()

    }


    private val sendResult: (MqttMessageImpl) -> Unit = { msg ->
        count++
        Log.i("test", "$count==sendResult:$msg")
    }
    private val connectCallback: (Boolean, String?) -> Unit = { connect, error ->
        if (connect) {
            button_connect.text = "disconnect"
        } else {
            button_connect.text = "connect"
        }
        Log.i("test", "connectCallback:$connect==$error")
    }
    private val receiveCallback: (String, ByteArray) -> Unit = { topic, data ->
        Log.i("test", "receiveCallback:$topic==$data")
    }
}
