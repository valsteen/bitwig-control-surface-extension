package com.djcrontab.code.extensions

import com.bitwig.extension.controller.api.*
import kotlin.math.truncate

class ControlSurfaceExtension(private val definition: ControlSurfaceExtensionDefinition, host: ControllerHost) :
    ExtensionDebugSocketBase(definition, host) {

    private lateinit var remoteControlSocket: RemoteSocket
    private var remoteConnection: RemoteConnection? = null
    private val project: Project
        get() {
            return host.project
        }
    private lateinit var remoteControlsPages : List<RemoteControlsPage>

    override fun init() {
        val messageQueue = mutableListOf<ByteArray>()
        setupDebug()


        remoteControlsPages = List<RemoteControlsPage>(8) { i ->
            val cursorTrack = host.createCursorTrack("Cursor ID $i", "Cursor $i", 0, 0, true)
            val cursorDevice = cursorTrack.createCursorDevice("Device ID $i", "Device $i", 0, CursorDeviceFollowMode.FOLLOW_SELECTION)
            val remoteControlsPage = cursorDevice.createCursorRemoteControlsPage(8)

            for (j in 0..7) {
                val parameter = remoteControlsPage.getParameter(j)
                parameter.name().addValueObserver {
                    val message = "$i,$j,name,$it\n".toByteArray()

                    if (remoteConnection != null) {
                        remoteConnection!!.send(message)
                    } else {
                        messageQueue.add(message)
                    }
                }
                var lastKnownValue = 0f ;
                parameter.value().addValueObserver {
                    val newValue = truncate(it.toFloat() * 1000f) / 1000f
                    if (newValue != lastKnownValue) {
                        val message = "$i,$j,value,$newValue\n".toByteArray()
                        if (remoteConnection != null) {
                            remoteConnection!!.send(message)
                        } else {
                            messageQueue.add(message)
                        }
                    }
                    lastKnownValue = newValue
                }

                var lastKnownDisplayedValue = "" ;
                parameter.displayedValue().addValueObserver {
                    if (it != lastKnownDisplayedValue) {
                        val message = "$i,$j,display,$it\n".toByteArray()
                        if (remoteConnection != null) {
                            remoteConnection!!.send(message)
                        } else {
                            messageQueue.add(message)
                        }
                    }
                    lastKnownDisplayedValue = it
                }
            }

            remoteControlsPage
        }

        remoteControlSocket = host.createRemoteConnection("Remote control", 60123)
        remoteControlSocket.setClientConnectCallback { remoteConnection ->
            host.showPopupNotification("Remote control connected")
            this.remoteConnection = remoteConnection
            remoteConnection.setDisconnectCallback {
                host.showPopupNotification("Remote control disconnected")
                this.remoteConnection = null
            }

            for (message in messageQueue) {
                remoteConnection.send(message)
            }
            messageQueue.clear()

            remoteConnection.setReceiveCallback { message ->
                val parts = message.decodeToString().split(",")
                val device = parts[0].toInt()
                val parameter = parts[1].toInt()
                val value = parts[2].toDouble()

                remoteControlsPages[device].getParameter(parameter).value().set(value)
            }
        }
    }

    override fun flush() {}

    override fun exit() {
        debug.out("Bye!")
    }
}
