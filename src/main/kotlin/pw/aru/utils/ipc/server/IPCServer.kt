package pw.aru.utils.ipc.server

import org.slf4j.LoggerFactory
import pw.aru.utils.io.DataPipe
import pw.aru.utils.io.DataPipeStream
import pw.aru.utils.ipc.proto.*
import java.io.Closeable
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors.newCachedThreadPool
import kotlin.concurrent.thread

/**
 * Creates an [IPCServer] that awaits connections on the specified [port].
 *
 * @param serverName The server's name, sent to the client on the handshake.
 * @param port The server's port.
 * @param calls Map used to resolve the calls to their handlers.
 * @param executor (Optional) The [ExecutorService] used to handle the clients asynchronously.
 * @param extensions (Optional) Map used to resolve unknown opcode calls to their handlers.
 * @param backlog (Optional) Maximum length of the queue of incoming connections on the socket.
 */
class IPCServer(
    val serverName: String,
    port: Int,
    private val calls: Map<String, Socket.(DataPipe) -> Unit>,
    private val executor: ExecutorService = newCachedThreadPool { thread(start = false, name = "$serverName/SocketThread-%d", block = it::run) },
    private val extensions: Map<Byte, Socket.(DataPipe) -> Unit> = emptyMap(),
    backlog: Int = 50
) : Closeable {

    private val server: ServerSocket = ServerSocket(port, backlog)

    private val thread: Thread = thread(name = "$serverName/ListeningThread") {
        try {
            while (!executor.isShutdown) executor.execute(processSocket(server.accept()))
        } catch (_: InterruptedException) {
        } catch (_: SocketException) {
        }
    }

    private fun processSocket(socket: Socket): () -> Unit = {
        try {
            socket.use { _ ->
                val io = DataPipeStream(socket.inputStream, socket.outputStream)

                (io).writeInt(handshake)
                    .writeString(serverName)
                    .writeShort(ackMe)

                if (!io.readBoolean()) {
                    io.write(exitNotAckMe)
                    return@use
                }

                io.write(ackedMe)

                while (true) {
                    val op = io.readByte()
                    when (op.toInt()) {
                        opExit -> return@use

                        opCheck -> {
                            (io).writeInt(handshake)
                                .writeString(serverName)
                                .writeShort(ackMe)
                        }

                        opList -> {
                            io.write(opReqAck).writeSizedStringArray(calls.keys.toTypedArray())
                        }

                        opListExt -> {
                            io.write(opReqAck).writeSizedByteArray(extensions.keys.toByteArray())
                        }

                        opCall -> {
                            val call = calls[io.write(opReqAckParams).readString()]

                            if (call == null) {
                                io.write(opReqInvalidParams)
                            } else {
                                socket.call(io.write(opReqAck))
                            }
                        }

                        else -> {
                            val extension = extensions[op]

                            if (extension == null) {
                                io.write(opReqInvalid)
                            } else {
                                io.write(opReqAckExt)
                                socket.extension(io)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("SocketHandler of socket $socket of server $serverName caught an exception:", e)
        }
    }

    /**
     * Closes the executor, the listening thread and the server socket, freeing the resources.
     */
    override fun close() {
        executor.shutdown()
        thread.interrupt()
        server.close()
    }

    companion object {
        val logger = LoggerFactory.getLogger(IPCServer::class.java)
    }
}