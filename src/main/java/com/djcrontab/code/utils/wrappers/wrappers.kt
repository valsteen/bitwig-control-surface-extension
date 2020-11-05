package com.djcrontab.code.utils.wrappers

import com.bitwig.extension.controller.api.Channel
import com.bitwig.extension.controller.api.CursorDevice
import com.bitwig.extension.controller.api.CursorTrack
import com.bitwig.extension.controller.api.PinnableCursorDevice

class CursorDeviceWrapper(val cursorDevice: CursorDevice) {
    init {
        cursorDevice.name().markInterested()
        cursorDevice.position().markInterested()
        cursorDevice.hasNext().markInterested()
        cursorDevice.exists().markInterested()
        cursorDevice.hasSlots().markInterested()
        cursorDevice.slotNames().markInterested()
    }

    val name: String
        get() {
            return cursorDevice.name().get()
        }

    val position: Int
        get() {
            return cursorDevice.position().get()
        }

    val hasNext: Boolean
        get() {
            return cursorDevice.hasNext().get()
        }

    val exists: Boolean
        get() {
            return cursorDevice.exists().get()
        }

    val hasSlots: Boolean
        get() {
            return cursorDevice.hasSlots().get()
        }

    val slotNames: Array<out String>
        get() {
            return cursorDevice.slotNames().get()
        }

    fun selectNext() {
        return cursorDevice.selectNext()
    }

    fun selectFirst() {
        return cursorDevice.selectFirst()
    }
}

class CursorTrackWrapper(val cursorTrack: CursorTrack) {
    fun createCursorDevice(): PinnableCursorDevice {
        return cursorTrack.createCursorDevice()
    }


    init {
        cursorTrack.name().markInterested()
        cursorTrack.position().markInterested()
        cursorTrack.hasNext().markInterested()
        cursorTrack.isPinned.markInterested()
    }


    val name: String
        get() {
            return cursorTrack.name().get()
        }

    val position: Int
        get() {
            return cursorTrack.position().get()
        }

    val hasNext: Boolean
        get() {
            return cursorTrack.hasNext().get()
        }

    var isPinned: Boolean
        get() {
            return cursorTrack.isPinned.get()
        }
        set(value) {
            cursorTrack.isPinned.set(value)
        }

    fun selectNext() {
        return cursorTrack.selectNext()
    }

    fun selectChannel(channel: Channel) {
        return cursorTrack.selectChannel(channel)
    }
}
