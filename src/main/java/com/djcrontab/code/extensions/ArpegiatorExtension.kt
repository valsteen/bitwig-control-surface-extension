package com.djcrontab.code.extensions

import com.bitwig.extension.controller.ControllerExtension
import com.bitwig.extension.controller.api.*


class ArpegiatorExtension(definition: ArpegiatorExtensionDefinition, host: ControllerHost) : ControllerExtension(definition, host) {
    protected lateinit var application: Application

    private val project: Project
        get() {
            return host.project
        }

    override fun init() {
        application = host.createApplication()
        val host = host

        val midiIn = host.getMidiInPort(0)

        // setting a wildcard expression makes sure that timbre is properly interpreted
        // ( actually cc 74, mask "B?40??" )
        val noteInput = midiIn.createNoteInput("", "??????")
        noteInput.includeInAllInputs().set(false)


        noteInput.setUseExpressiveMidi(true, 0, 48)
    }

    override fun flush() {

    }

    override fun exit() {

    }
}
