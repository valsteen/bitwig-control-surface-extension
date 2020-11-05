package com.djcrontab.code.utils.debug


import com.bitwig.extension.controller.api.RemoteConnection
import com.bitwig.extension.controller.api.RemoteSocket
import org.apache.commons.lang3.exception.ExceptionUtils


class DebugSocket(
    remoteSocket: RemoteSocket,
    private val fallback: (String) -> Unit,
    connectedCallback: () -> Unit,
    disconnectedCallback: () -> Unit,
    incomingMessageCallback: (String) -> Unit

) {
    private var clientSocket: RemoteConnection? = null

    init {
        remoteSocket.setClientConnectCallback {
            clientSocket?.disconnect()
            clientSocket = it
            out("connected!")
            it.setReceiveCallback { byteArray -> Unit
                incomingMessageCallback(byteArray.decodeToString())
            }
            it.setDisconnectCallback(disconnectedCallback)
            connectedCallback()
        }
    }

    fun out(s: String) {
        clientSocket?.apply {
            send("$s\n".toByteArray())
            return
        }
        fallback(s)
    }

    fun out(e: Exception) {
        out(ExceptionUtils.getStackTrace(e))
    }
}