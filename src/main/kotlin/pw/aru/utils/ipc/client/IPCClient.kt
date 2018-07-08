package pw.aru.utils.ipc.client

import pw.aru.utils.io.DataPipe
import pw.aru.utils.io.DataPipeStream
import pw.aru.utils.ipc.proto.*
import java.io.Closeable
import java.net.InetAddress
import java.net.InetAddress.getLocalHost
import java.net.Socket

class IPCClient(val address: InetAddress = getLocalHost(), val port: Int) : Closeable {
    private val socket = Socket(address, port)
    val io = DataPipeStream(socket.getInputStream(), socket.getOutputStream())
    val serverName: String

    override fun toString() = "IPCClient[address=$address,port=$port,serverName=$serverName]"

    init {
        check(io.readInt() == handshake, "Server Handshake")
        serverName = io.readString()
        check(io.readUnsignedShort() == ackMe, "Server ACK Code")
        io.writeBoolean(true)
        check(io.readUnsignedByte() == ackedMe, "Client ACK by Server")
    }

    fun call(key: String): DataPipe {
        check(io.write(opCall).readUnsignedByte() == opReqAckParams, "OP call answer code")
        val op = io.writeString(key).readUnsignedByte()
        check(op == opReqAck) {
            when (op) {
                opReqInvalidParams -> "Unknown call '$key'"
                else -> "Protocol infringed by server on step: OP call answer code"
            }
        }

        return io
    }

    fun extension(code: Byte): DataPipe {
        check(io.write(code.toInt()).readUnsignedByte() == opReqAckExt) {
            "Invalid extension code '${code.toString(16)}'"
        }

        return io
    }

    val commandList: List<String>
        get () {
            check(io.write(opList).readUnsignedByte() == opReqAck, "OP call answer code")
            return (0 until io.readInt()).map { io.readString() }
        }

    val extensionCodes: List<Byte>
        get () {
            check(io.write(opList).readUnsignedByte() == opReqAck, "OP call answer code")
            return (0 until io.readInt()).map { io.readByte() }
        }

    override fun close() {
        io.write(opExit)
        socket.close()
    }

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        private inline fun check(value: Boolean, step: String) {
            if (!value) {
                throw IllegalStateException("Protocol infringed by server on step: $step")
            }
        }
    }
}