package com.djcrontab.code.extensions

import com.bitwig.extension.controller.api.ControllerHost
import com.bitwig.extension.controller.api.Project

class ControlSurfaceExtension(private val definition: ControlSurfaceExtensionDefinition, host: ControllerHost) :
    ExtensionDebugSocketBase(definition, host) {

    private val project: Project
        get() {
            return host.project
        }

    override fun init() {
        setupDebug()
    }

    override fun flush() {}

    override fun exit() {
        debug.out("Bye!")
    }
}
