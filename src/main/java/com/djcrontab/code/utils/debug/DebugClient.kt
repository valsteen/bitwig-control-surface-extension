package com.djcrontab.code.utils.debug

import java.io.IOException
import java.io.OutputStream
import java.lang.Thread.sleep
import java.net.InetSocketAddress
import java.net.Socket

class DebugClient(val host: String, val port: Int) {
    private var clientSocket: Socket? = null
    private var outputStream: OutputStream? = null

    fun readloop() {
        val stream = clientSocket!!.getInputStream()
        while (true) {
            val buffer = ByteArray(1024)
            val len = stream.read(buffer)
            if (len == -1) { break }
            System.out.write(buffer, 0, len)
        }
    }

    fun writeloop() {
        while (true) try {
            while (true) {
                System.console().readLine().let {
                    if (it.isEmpty()) return
                    out(it)
                }

            }
        } catch (e: IOException) {
            print("Not connected... retrying")
            sleep(1000)
        }
    }

    fun makeByteArray(s: String): ByteArray {
        val asByteArray = s.toByteArray()
        val header = asByteArray.let {
            val byteArray = ByteArray(4)
            var size = it.size
            for (i in 3 downTo 0) {
                byteArray[i] = (size and 0xff).toByte()
                size /= 256
            }
            byteArray
        }
        return header + asByteArray
    }

    fun out(s: String) {
        outputStream?.apply {
            write(makeByteArray(s))
            return
        }
        println("socket closed, ignored")
    }

    init {
        val thread = Thread {
            while (true) {
                try {
                    println("connecting..")
                    clientSocket = Socket()
                    clientSocket!!.connect(InetSocketAddress(host, port))
                    outputStream = clientSocket!!.getOutputStream()
                } catch (e: IOException) {
                    println("not connected, retrying")
                    sleep(1000)
                    continue
                }
                kotlin.runCatching { readloop() }
                clientSocket?.close()
                if (Thread.currentThread().isInterrupted) {
                    break
                }
            }
        }
        thread.start()
        writeloop()
        thread.interrupt()
        clientSocket?.close()
    }
}


fun main(args: Array<String>) {

    val port = args.let {
        if (args.isEmpty()) {
            5555
        } else {
            try {
                args[0].toInt()
            } catch (e: NumberFormatException) {
                5555
            }
        }
    }
    DebugClient("localhost", port)
}
