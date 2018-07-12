package pw.aru.utils.ipc.client

import pw.aru.utils.io.DataPipe
import pw.aru.utils.io.DataPipeStream
import pw.aru.utils.ipc.proto.*
import java.io.Closeable
import java.lang.Thread.sleep
import java.net.InetAddress
import java.net.InetAddress.getLocalHost
import java.net.Socket

/**
 * Creates an [IPCClient] that connects on the specified [address] and [port].
 *
 * Once created, the client will automatically open a socket and init a handshake with the server.
 *
 * # Warning: Each [IPCClient] instance is **NOT** thread-safe.
 * ## Use a [IPCClientPool] to share instances across threads.
 *
 * @param address The [InetAddress] the client will connect/is connected to.
 * @param port The port the client will connect/is connected to.
 * @throws IllegalStateException If the handshake fails or the protocol is infringed.
 */
class IPCClient(val address: InetAddress = getLocalHost(), val port: Int) : Closeable {
    private val socket = Socket(address, port)

    /**
     * The raw [DataPipe] used to interface with the socket.
     *
     * (You can use [DataPipe.getInputStream] and [DataPipe.getOutputStream] to get the original streams.)
     */
    val io = DataPipeStream(socket.getInputStream(), socket.getOutputStream())

    /**
     * @returns The server's name, defined on the server handshake.
     */
    val serverName: String

    init {
        check(io.readInt() == handshake, "Server Handshake")
        serverName = io.readString()
        check(io.readUnsignedShort() == ackMe, "Server ACK Code")
        io.writeBoolean(true)
        check(io.readUnsignedByte() == ackedMe, "Client ACK by Server")
    }

    /**
     * Calls a command by it's name.
     *
     * @return The raw [DataPipe] used to interface with the socket.
     * @throws IllegalArgumentException If the key doesn't exists.
     * @throws IllegalStateException If the protocol is infringed by the server.
     */
    fun call(key: String): DataPipe {
        check(io.write(opCall).readUnsignedByte() == opReqAckParams, "OP call answer code")
        val op = io.writeString(key).readUnsignedByte()
        check(op == opReqAck) {
            when (op) {
                opReqInvalidParams -> throw IllegalArgumentException("Unknown call '$key'")
                else -> "Protocol infringed by server on step: OP call answer code"
            }
        }

        return io
    }

    /**
     * Calls a extension by it's extension code.
     *
     * @return The raw [DataPipe] used to interface with the socket.
     * @throws IllegalArgumentException If the code doesn't exists.
     * @throws IllegalStateException If the protocol is infringed by the server.
     */
    fun extension(code: Byte): DataPipe {
        val op = io.write(code.toInt()).readUnsignedByte()
        check(op == opReqAckExt) {
            when (op) {
                opReqInvalid -> throw IllegalArgumentException("Invalid extension code '${code.toString(16)}'")
                else -> "Protocol infringed by server on step: OP call answer code"
            }
        }

        return io
    }

    /**
     * Returns the list of commands available.
     */
    val commandList: List<String>
        get () {
            check(io.write(opList).readUnsignedByte() == opReqAck, "OP call answer code")
            return (0 until io.readInt()).map { io.readString() }
        }

    /**
     * Returns the extension codes available.
     */
    val extensionCodes: List<Byte>
        get () {
            check(io.write(opListExt).readUnsignedByte() == opReqAck, "OP call answer code")
            return (0 until io.readInt()).map { io.readByte() }
        }

    /**
     * Returns if the socket is still alive.
     */
    val isAlive: Boolean
        get() = !socket.isClosed

    /**
     * Checks if the client is valid, meaning that it can answer calls/extensions.
     * # Warning: Calling it in a not valid state WILL render the current operation mostly useless.
     */
    val isValid: Boolean
        get() {
            if (!isAlive) return false

            return try {
                io.write(opCheck)

                var available = false
                for (i in 0..2) {
                    available = io.inputStream.available() >= Integer.BYTES
                    if (available) break
                    sleep(50)
                }

                available && io.write(opCheck).readInt() == handshake && io.readString() == serverName && io.readUnsignedShort() == ackMe
            } catch (e: Exception) {
                false
            }
        }

    /**
     * Closes the socket and frees the resources.
     */
    override fun close() {
        if (isAlive) {
            io.write(opExit)
            socket.close()
        }
    }

    override fun toString() = "IPCClient[address=$address,port=$port,serverName=$serverName]"

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        private inline fun check(value: Boolean, step: String) {
            if (!value) {
                throw IllegalStateException("Protocol infringed by server on step: $step")
            }
        }
    }
}