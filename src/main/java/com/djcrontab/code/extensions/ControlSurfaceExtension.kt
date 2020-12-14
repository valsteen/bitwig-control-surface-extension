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
    private var devices = mutableListOf<CursorDevice>()
    private var tracks = mutableListOf<CursorTrack>()

    val messageQueue = mutableListOf<ByteArray>()

    fun send(message: String) {
        val asByteArray = (message + "\n").toByteArray()
        if (remoteConnection != null) {
            remoteConnection!!.send(asByteArray)
        } else {
            messageQueue.add(asByteArray)
        }
    }

    override fun init() {
        setupDebug()

        application = host.createApplication()

        val mainCursorTrack = host.createCursorTrack(0,0)
        val mainCursorDevice = mainCursorTrack.createCursorDevice()

        remoteControlsPages = List<RemoteControlsPage>(8) { i ->
            val cursorTrack = host.createCursorTrack("Cursor ID $i", "Cursor $i", 0, 0, true)
            val cursorDevice = cursorTrack.createCursorDevice("Device ID $i", "Device $i", 0, CursorDeviceFollowMode.FOLLOW_SELECTION)

            tracks.add(cursorTrack)
            devices.add(cursorDevice)

            val remoteControlsPage = cursorDevice.createCursorRemoteControlsPage(8)

            remoteControlsPage.name.addValueObserver {

            }

            for (j in 0..7) {
                val parameter = remoteControlsPage.getParameter(j)
                parameter.name().addValueObserver {
                    send("$i,$j,name,$it")
                }
                var lastKnownValue = 0f ;
                parameter.value().addValueObserver {
                    val newValue = truncate(it.toFloat() * 1000f) / 1000f
                    if (newValue != lastKnownValue) {
                        send("$i,$j,value,$newValue")
                    }
                    lastKnownValue = newValue
                }

                var lastKnownDisplayedValue = "" ;
                parameter.displayedValue().addValueObserver {
                    if (it != lastKnownDisplayedValue) {
                        send("$i,$j,display,$it")
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
                val action = parts[0]
                val device = parts[1].toInt()
                if (action == "value") {
                    val parameter = parts[2].toInt()
                    val value = parts[3].toDouble()
                    remoteControlsPages[device].getParameter(parameter).value().set(value)
                } else if (action == "focus") {
                    tracks[device].selectInEditor()
                    devices[device].selectInEditor()
                    application.getAction("focus_track_header_area").invoke()
                    application.getAction("select_item_at_cursor").invoke()
                    application.getAction("focus_or_toggle_device_panel").invoke()
                    application.getAction("select_item_at_cursor").invoke()
                }
            }
        }
    }

    override fun flush() {}

    override fun exit() {
        debug.out("Bye!")
    }
}
