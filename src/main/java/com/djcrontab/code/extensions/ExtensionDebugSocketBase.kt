package com.djcrontab.code.extensions

import com.bitwig.extension.controller.ControllerExtension
import com.bitwig.extension.controller.api.ControllerHost
import com.djcrontab.code.utils.debug.DebugSocket

abstract class ExtensionDebugSocketBase(definition: ControlSurfaceExtensionDefinition, host: ControllerHost) : ControllerExtension(definition, host) {
    protected lateinit var debug: DebugSocket
    protected fun setupDebug() {
        val queuedMessages = ArrayList<String>()
        val remoteSocket = host.createRemoteConnection("Debug Socket", 43266)
        debug = DebugSocket(
            remoteSocket,
            fallback = {
                queuedMessages.add(it)
            },
            connectedCallback = {
                for (message in queuedMessages) {
                    debug.out(message)
                }
                queuedMessages.clear()
            },
            disconnectedCallback = {
                host.showPopupNotification("debug socket disconnected")
            },
            incomingMessageCallback = {
                host.showPopupNotification("incoming $it")
                if (it == "restart") host.restart()
            }
        )
    }
}