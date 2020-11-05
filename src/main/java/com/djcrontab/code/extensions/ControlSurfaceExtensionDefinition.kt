package com.djcrontab.code.extensions

import com.bitwig.extension.api.PlatformType
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList
import com.bitwig.extension.controller.ControllerExtensionDefinition
import com.bitwig.extension.controller.api.ControllerHost
import java.util.*

class ControlSurfaceExtensionDefinition : ControllerExtensionDefinition() {
    override fun getName(): String {
        return "control-surface-extension"
    }

    override fun getAuthor(): String {
        return "Vincent Alsteen"
    }

    override fun getVersion(): String {
        return "0.1"
    }

    override fun getId(): UUID {
        return UUID.fromString("5ae000a9-42a3-4a9a-a8f8-4e700118c09d")
    }

    override fun getHardwareVendor(): String {
        return "dj-crontab"
    }

    override fun getHardwareModel(): String {
        return "control-surface-extension"
    }

    override fun getRequiredAPIVersion(): Int {
        return 12
    }

    override fun getNumMidiInPorts(): Int {
        return 0
    }

    override fun getNumMidiOutPorts(): Int {
        return 0
    }

    override fun listAutoDetectionMidiPortNames(list: AutoDetectionMidiPortNamesList, platformType: PlatformType) {}
    override fun createInstance(host: ControllerHost): ControlSurfaceExtension {
        return ControlSurfaceExtension(this, host)
    }
}
