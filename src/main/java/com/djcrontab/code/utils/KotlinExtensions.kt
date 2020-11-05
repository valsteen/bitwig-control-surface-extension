package com.djcrontab.code.utils

import com.bitwig.extension.controller.api.*

operator fun <ItemType : ObjectProxy?> Bank<ItemType>.iterator(): Iterator<ItemType> {
    val i = 0
    return object : Iterator<ItemType> {
        override fun hasNext(): Boolean {
            return i < sizeOfBank
        }

        override fun next(): ItemType {
            return get(i)
        }

    }
}

operator fun <ItemType : ObjectProxy?> Bank<ItemType>.get(i: Int): ItemType {
    return getItemAt(i)
}

operator fun TrackBank.get(i: Int): Track {
    return getItemAt(i)
}

operator fun DeviceBank.get(i: Int): Device {
    return getItemAt(i)
}

suspend fun SequenceScope<Unit>.yield() {
    yield(Unit)
}
