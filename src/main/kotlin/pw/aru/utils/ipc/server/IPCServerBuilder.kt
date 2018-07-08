@file:JvmName("IPC")
@file:JvmMultifileClass

package pw.aru.utils.ipc.server

import pw.aru.utils.io.DataPipe
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors.newCachedThreadPool
import kotlin.concurrent.thread

fun server(
    serverName: String,
    port: Int,
    executor: ExecutorService = newCachedThreadPool { thread(start = false, name = "$serverName/SocketThread-%d", block = it::run) },
    configurator: IPCServerBuilder.() -> Unit
): IPCServer {
    return IPCServerBuilder().apply(configurator).run {
        IPCServer(serverName, port, calls, executor, extensions, backlog)
    }
}

class IPCServerBuilder internal constructor() {
    var calls: MutableMap<String, Socket.(DataPipe) -> Unit> = LinkedHashMap()
    var extensions: MutableMap<Byte, Socket.(DataPipe) -> Unit> = LinkedHashMap()

    val backlog: Int = 50

    fun call(key: String, block: Socket.(DataPipe) -> Unit) {
        check(!calls.contains(key)) { "Key '$key' already exists." }
        calls[key] = block
    }

    fun extension(opcode: Byte, block: Socket.(DataPipe) -> Unit) {
        check(!extensions.contains(opcode)) { "Opcode '${opcode.toString(16)}' already exists." }
        extensions[opcode] = block
    }
}