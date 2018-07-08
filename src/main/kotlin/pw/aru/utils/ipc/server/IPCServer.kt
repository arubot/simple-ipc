package pw.aru.utils.ipc.server

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

class IPCServer(
    private val serverName: String,
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

                    opCall -> {
                        val call = calls[io.write(opReqAckParams).readString()]

                        if (call == null) {
                            io.write(opReqInvalidParams)
                        } else {
                            socket.call(io.write(opReqAck))
                        }
                    }

                    opList -> {
                        io.write(opReqAck).writeInt(calls.size)
                        calls.keys.forEach { io.writeString(it) }
                    }

                    opListExt -> {
                        io.write(opReqAck).writeInt(extensions.size)
                        extensions.keys.forEach { io.writeByte(it.toInt()) }
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
    }

    override fun close() {
        executor.shutdown()
        thread.interrupt()
        server.close()
    }
}