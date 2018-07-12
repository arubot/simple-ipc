@file:JvmName("IPC")
@file:JvmMultifileClass

package pw.aru.utils.ipc.server

import pw.aru.utils.io.DataPipe
import pw.aru.utils.ipc.proto.opCall
import pw.aru.utils.ipc.proto.opExit
import pw.aru.utils.ipc.proto.opList
import pw.aru.utils.ipc.proto.opListExt
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors.newCachedThreadPool
import kotlin.concurrent.thread

/**
 * Creates and configures an [IPCServer].
 *
 * @param serverName The server's name, sent to the client on the handshake.
 * @param port The server's port.
 * @param executor (Optional) The [ExecutorService] used to handle the clients asynchronously.
 * @param block The configuration of the server.
 * @return the configurated [IPCServer].
 */
fun server(
    serverName: String,
    port: Int,
    executor: ExecutorService = newCachedThreadPool { thread(start = false, name = "$serverName/SocketThread-%d", block = it::run) },
    block: IPCServerBuilder.() -> Unit
): IPCServer {
    return IPCServerBuilder().apply(block).run {
        IPCServer(serverName, port, calls, executor, extensions, backlog)
    }
}

class IPCServerBuilder internal constructor() {
    /**
     * Map used to resolve the calls to their handlers.
     */
    var calls: MutableMap<String, Socket.(DataPipe) -> Unit> = LinkedHashMap()
    /**
     * Map used to resolve unknown opcode calls to their handlers.
     */
    var extensions: MutableMap<Byte, Socket.(DataPipe) -> Unit> = LinkedHashMap()

    /**
     * Maximum length of the queue of incoming connections on the socket.
     */
    var backlog: Int = 50
        set(value) {
            check(value in 1..65535) { "$value is not a valid port" }
            field = value
        }

    /**
     * Registers a call and its handler to the map.
     */
    fun call(key: String, block: Socket.(DataPipe) -> Unit) {
        check(!calls.contains(key)) { "Key '$key' already exists." }
        calls[key] = block
    }

    /**
     * Registers an extension and its handler to the map.
     */
    fun extension(opcode: Byte, block: Socket.(DataPipe) -> Unit) {
        check(opcode != opExit.toByte() || opcode != opCall.toByte() || opcode != opList.toByte() || opcode != opListExt.toByte()) {
            "Opcode '0x${opcode.toString(16)}' is a reserved opcode."
        }
        check(!extensions.contains(opcode)) { "Opcode '0x${opcode.toString(16)}' already exists." }
        extensions[opcode] = block
    }
}