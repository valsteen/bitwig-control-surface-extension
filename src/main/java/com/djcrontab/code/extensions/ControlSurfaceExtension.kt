package com.djcrontab.code.extensions

import com.bitwig.extension.controller.api.*
import kotlin.math.truncate


class DeviceController(
    private val cursorTrack: CursorTrack,
    private val cursorDevice: CursorDevice,
    val index: Int,
    val sendChange: (String) -> Unit,
    val debug: (String) -> Unit,
    private val callAction: (String) -> Unit
) {
    fun focus() {
        cursorTrack.selectInEditor()
        cursorDevice.selectInEditor()
        callAction("focus_track_header_area")
        callAction("select_item_at_cursor")
        callAction("focus_or_toggle_device_panel")
        callAction("select_item_at_cursor")
    }
}

class ParameterController(
    private val deviceController: DeviceController,
    private val remoteControl: RemoteControl,
    private val index: Int,
) {
    var lastKnownValue = 0f
    var lastKnownDisplayValue = ""
    var lastKnownName = ""

    init {
        remoteControl.name().addValueObserver {
            if (it != lastKnownName) {
                lastKnownName = it
                updateName()
            }
        }
        remoteControl.value().addValueObserver {
            val newValue = truncate((it.toFloat() * 1000f)) / 1000f
            deviceController.debug("?$newValue,$lastKnownValue,$it")
            if (newValue != lastKnownValue) {
                lastKnownValue = newValue
                updateValue()
            }
        }
        remoteControl.displayedValue().addValueObserver {
            if (it != lastKnownDisplayValue) {
                lastKnownDisplayValue = it
                updateDisplayedValue()
            }
        }
    }

    fun updateName() {
        deviceController.sendChange("${deviceController.index},$index,name,$lastKnownName")
    }

    fun updateValue() {
        deviceController.sendChange("${deviceController.index},$index,value,$lastKnownValue")
    }

    fun updateDisplayedValue() {
        deviceController.sendChange("${deviceController.index},$index,display,$lastKnownDisplayValue")
    }

    fun updateAll() {
        updateName()
        updateValue()
        updateDisplayedValue()
    }

    fun setValue(value: Double) {
        val newValue = truncate((value.toFloat() * 1000f)) / 1000f
        if (newValue != lastKnownValue) {
            remoteControl.value().set(newValue.toDouble())
            lastKnownValue = newValue
        }

    }
}


data class ParameterIndex(val deviceIndex: Int, val parameterIndex: Int)
class ParametersMap : HashMap<ParameterIndex, ParameterController>()

class ControlSurfaceExtension(private val definition: ControlSurfaceExtensionDefinition, host: ControllerHost) :
    ExtensionDebugSocketBase(definition, host) {

    private lateinit var remoteControlSocket: RemoteSocket
    private var remoteConnection: RemoteConnection? = null
    private val project: Project
        get() {
            return host.project
        }

    private var parameterControllers = ParametersMap()
    private var deviceControllers = HashMap<Int, DeviceController>()

    val messageQueue = mutableListOf<ByteArray>()

    fun send(message: String) {
        val asByteArray = (message + "\n").toByteArray()
        if (remoteConnection != null) {
            debug.out("will send $message")
            remoteConnection!!.send(asByteArray)
        } else {
            messageQueue.add(asByteArray)
            if (messageQueue.size > 100) {
                messageQueue.removeFirst()
            }
        }
    }

    private fun createRemoteControlSocket() {
        remoteControlSocket = host.createRemoteConnection("Remote control", 60123)
        remoteControlSocket.setClientConnectCallback { remoteConnection ->
            host.showPopupNotification("Remote control connected")

            for (parameter in parameterControllers.values) {
                parameter.updateAll()
            }

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
                    parameterControllers[ParameterIndex(device, parameter)]!!.setValue(value)
                } else if (action == "focus") {
                    deviceControllers[device]!!.focus()
                }
            }
        }
    }

    private fun createCursors() {
        for (i in 0..7) {
            val cursorTrack = host.createCursorTrack("Cursor ID $i", "Cursor $i", 0, 0, true)
            val cursorDevice =
                cursorTrack.createCursorDevice("Device ID $i", "Device $i", 0, CursorDeviceFollowMode.FOLLOW_SELECTION)

            val deviceController = DeviceController(
                cursorTrack,
                cursorDevice,
                i,
                this::send,
                this.debug::out
            ) { application.getAction(it).invoke() }

            deviceControllers[i] = deviceController

            val remoteControlsPage = cursorDevice.createCursorRemoteControlsPage(8)

            var deviceName = ""
            var remoteControlsPageName = ""

            fun sendDeviceName() {
                var name = deviceName
                if (remoteControlsPageName != "") {
                    name += "/$remoteControlsPageName"
                }
                send("$i,devicename,$name")
            }

            remoteControlsPage.name.addValueObserver {
                remoteControlsPageName = it
                sendDeviceName()
            }

            cursorDevice.name().addValueObserver {
                deviceName = it
                sendDeviceName()
            }

            for (j in 0..7) {
                val remoteControl: RemoteControl = remoteControlsPage.getParameter(j)
                val parameterController = ParameterController(
                    deviceController,
                    remoteControl,
                    j
                )
                parameterControllers[ParameterIndex(i, j)] = parameterController
            }
        }
    }

    override fun init() {
        application = host.createApplication()
        setupDebug()
        createCursors()
        createRemoteControlSocket()
    }

    override fun flush() {}

    override fun exit() {
        debug.out("Bye!")
    }
}
