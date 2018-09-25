package pw.aru.utils.ipc.server.dsl

import pw.aru.utils.io.DataPipe
import pw.aru.utils.ipc.proto.Protocol
import pw.aru.utils.ipc.server.util.threadFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors.newCachedThreadPool
import java.util.concurrent.atomic.AtomicInteger

open class ServerBuilder<T>(private val protocol: Protocol) {
    companion object {
        val count = AtomicInteger()
    }

    /**
     * Name of the Server
     */
    var serverName: String = "server=${count.getAndIncrement()}"

    /**
     * Executor creator
     */
    var executor: () -> ExecutorService = {
        newCachedThreadPool(threadFactory(nameFormat = "$serverName/ExecutingThread-%d"))
    }

    /**
     * Map used to resolve the calls to their handlers.
     */
    var calls: MutableMap<String, T.(DataPipe) -> Unit> = LinkedHashMap()

    /**
     * Map used to resolve unknown opcode calls to their handlers.
     */
    var extensions: MutableMap<Byte, T.(DataPipe) -> Unit> = LinkedHashMap()

    /**
     * Registers a call and its handler to the map.
     */
    fun call(key: String, block: T.(DataPipe) -> Unit) {
        check(!calls.contains(key)) { "Key '$key' already exists." }
        calls[key] = block
    }

    /**
     * Registers an extension and its handler to the map.
     */
    fun extension(opcode: Byte, block: T.(DataPipe) -> Unit) {
        check(opcode != protocol.opExit.toByte() || opcode != protocol.opCall.toByte() || opcode != protocol.opList.toByte() || opcode != protocol.opListExt.toByte()) {
            "Opcode '0x${opcode.toString(16)}' is a reserved opcode by the Protocol."
        }
        check(!extensions.contains(opcode)) { "Opcode '0x${opcode.toString(16)}' already exists." }
        extensions[opcode] = block
    }
}