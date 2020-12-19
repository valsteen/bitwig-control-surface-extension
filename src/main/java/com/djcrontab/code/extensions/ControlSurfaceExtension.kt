package com.djcrontab.code.extensions

import com.bitwig.extension.controller.api.*
import java.io.IOException
import kotlin.math.truncate

interface Flushable {
    val onDirty: (flushable: Flushable) -> Unit
    fun flush ()
}

class DeviceController (
    private val cursorTrack: CursorTrack,
    private val cursorDevice: CursorDevice,
    private val remoteControlsPage: CursorRemoteControlsPage,
    val index: Int,
    val sendChange: (String) -> Unit,
    val debug: (String) -> Unit,
    private val callAction: (String) -> Unit,
    override val onDirty: (Flushable) -> Unit
) : Flushable {
    var deviceName = ""
    var remoteControlsPageName = ""

    fun focus() {
        cursorTrack.selectInEditor()
        cursorDevice.selectInEditor()
        callAction("focus_track_header_area")
        callAction("select_item_at_cursor")
        callAction("focus_or_toggle_device_panel")
        callAction("select_item_at_cursor")
    }

    fun getDeviceNameMessage() : String {
        var name = deviceName
        if (remoteControlsPageName != "") {
            name += "/$remoteControlsPageName"
        }
        return "$index,devicename,$name"
    }

    fun sendDeviceName() {
        sendChange(getDeviceNameMessage())
    }

    fun updateAll() {
        sendDeviceName()
    }

    override fun flush() {
        if (pageNameIsDirty || nameIsDirty) {
            sendDeviceName()
            pageNameIsDirty = false
            nameIsDirty = false
            onDirty(this)
        }
    }

    var pageNameIsDirty = false
    var nameIsDirty = false

    init {
        // TODO check createCursorRemoteControlsPage
        remoteControlsPage.name.addValueObserver {
            remoteControlsPageName = it
            pageNameIsDirty = true
            onDirty(this)
        }

        cursorDevice.name().addValueObserver {
            deviceName = it
            nameIsDirty = true
            onDirty(this)
        }
    }

}


class ParameterController (
    private val deviceController: DeviceController,
    private val remoteControl: RemoteControl,
    private val index: Int, override val onDirty: (flushable: Flushable) -> Unit,
) : Flushable {
    var lastKnownValue = 0f
    var lastKnownDisplayValue = ""
    var lastKnownName = ""

    var nameIsDirty = false
    var valueIsDirty = false
    var displayedValueIsDirty = false

    init {
        remoteControl.name().addValueObserver {
            if (it != lastKnownName) {
                lastKnownName = it
                nameIsDirty = true
                onDirty(this)
            }
        }
        remoteControl.value().addValueObserver {
            val newValue = truncate((it.toFloat() * 1000f)) / 1000f

            if (newValue != lastKnownValue) {
                lastKnownValue = newValue
                valueIsDirty = true
                onDirty(this)
            }
        }
        remoteControl.displayedValue().addValueObserver {
            if (it != lastKnownDisplayValue) {
                lastKnownDisplayValue = it
                displayedValueIsDirty = true
                onDirty(this)
            }
        }
    }

    fun touch(value: Boolean) {
        remoteControl.touch(value)
    }

    override fun flush() {
        if (nameIsDirty) {
            updateName()
            nameIsDirty = false
        }
        if (valueIsDirty) {
            updateValue()
            valueIsDirty = false
        }

        if (displayedValueIsDirty) {
            updateDisplayedValue()
            displayedValueIsDirty = false
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
    private var deviceControllers = mutableListOf<DeviceController>()

    var messageBuffer : String = ""

    fun bufferedSend(message: String) {
        messageBuffer += "$message\n"
    }

    fun sendBuffer() {
        if (messageBuffer == "") { return }

        if (remoteConnection != null) {
            val asByteArray = messageBuffer.toByteArray()
            debug.out("will send:\n$messageBuffer")
            try {
                remoteConnection!!.send(asByteArray)
            } catch (e: IOException) {

            }
        }
        messageBuffer = ""
    }

    private fun createRemoteControlSocket() {
        remoteControlSocket = host.createRemoteConnection("Remote control", 60123)
        remoteControlSocket.setClientConnectCallback { remoteConnection ->
            this.remoteConnection = remoteConnection
            host.showPopupNotification("Remote control connected")

            for (device in deviceControllers) {
                device.updateAll()
            }
            for (parameter in parameterControllers.values) {
                parameter.updateAll()
            }
            flush()

            remoteConnection.setDisconnectCallback {
                host.showPopupNotification("Remote control disconnected")
                this.remoteConnection = null
            }

            remoteConnection.setReceiveCallback { message ->
                val parts = message.decodeToString().split(",")
                val action = parts[0]
                val device = parts[1].toInt()
                if (action == "value") {
                    val parameter = parts[2].toInt()
                    val value = parts[3].toDouble()
                    parameterControllers[ParameterIndex(device, parameter)]!!.setValue(value)
                } else if (action == "focus") {
                    deviceControllers[device].focus()
                } else if (action == "touch") {
                    val parameter = parts[2].toInt()
                    val value = parts[3].toInt()
                    parameterControllers[ParameterIndex(device, parameter)]!!.touch(value != 0)
                }
            }
        }
    }

    private fun createCursors() {
        for (i in 0 until 12) {
            val cursorTrack = host.createCursorTrack("Cursor ID $i", "Cursor $i", 0, 0, true)
            val cursorDevice =
                cursorTrack.createCursorDevice("Device ID $i", "Device $i", 0, CursorDeviceFollowMode.FOLLOW_SELECTION)
            val remoteControlsPage = cursorDevice.createCursorRemoteControlsPage(8)
            val t: CursorRemoteControlsPage = cursorDevice.createCursorRemoteControlsPage("Remotes cursor $i", 8, null)

            var pageCount = 0
            t.pageCount().addValueObserver {
                pageCount = it
            }

            t.pageNames().addValueObserver {
                debug.out("page name for $i: ${it.joinToString(", ")}")
            }

            val flushables = mutableSetOf<Flushable>()
            var flushIsScheduled = false

            fun scheduleFlush(flushable: Flushable) {
                flushables.add(flushable)
                if (!flushIsScheduled) {
                    host.scheduleTask({
                        for (flushableItem in flushables) {
                            flushableItem.flush()
                        }
                        flushIsScheduled = false
                        flush()
                    }, 20)
                    flushIsScheduled = true
                }
            }

            val deviceController = DeviceController(
                cursorTrack,
                cursorDevice,
                remoteControlsPage,
                i,
                this::bufferedSend,
                this.debug::out,
                { application.getAction(it).invoke() }
            ) {
                scheduleFlush(it)
            }

            deviceControllers.add(deviceController)

            for (j in 0..7) {
                val remoteControl: RemoteControl = remoteControlsPage.getParameter(j)
                val parameterController = ParameterController(
                    deviceController,
                    remoteControl,
                    j
                ) {
                    scheduleFlush(it)
                }
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

    override fun flush() {
        sendBuffer()
    }

    override fun exit() {
        debug.out("Bye!")
    }
}
