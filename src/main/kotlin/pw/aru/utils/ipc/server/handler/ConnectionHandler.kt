package pw.aru.utils.ipc.server.handler

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pw.aru.utils.io.DataPipe
import pw.aru.utils.ipc.proto.ConnectionState.*
import pw.aru.utils.ipc.proto.DefaultProtocol
import pw.aru.utils.ipc.proto.Protocol
import pw.aru.utils.ipc.server.connector.ServerConnection
import pw.aru.utils.ipc.server.impl.ServerImpl
import java.util.concurrent.ExecutorService

/**
 * Creates an [ServerImpl] that awaits connections on the specified [port].
 *
 * @param serverName The server's name, sent to the client on the handshake.
 * @param port The server's port.
 * @param calls Map used to resolve the calls to their handlers.
 * @param executor (Optional) The [ExecutorService] used to handle the clients asynchronously.
 * @param extensions (Optional) Map used to resolve unknown opcode calls to their handlers.
 * @param backlog (Optional) Maximum length of the queue of incoming connections on the socket.
 */
class ConnectionHandler<T>(
    val serverName: String,
    private val connection: ServerConnection<T>,
    private val calls: Map<String, T.(DataPipe) -> Unit>,
    private val extensions: Map<Byte, T.(DataPipe) -> Unit> = emptyMap(),
    private val proto: Protocol = DefaultProtocol
) : () -> Unit, Protocol by proto {
    override fun invoke() {
        var state = PRE_HANDSHAKE
        val (thisObj, io) = connection
        try {
            io.use {
                state = HANDSHAKE
                (io).writeInt(handshake)
                    .writeString(serverName)
                    .writeShort(ackMe)

                state = ACK_ME
                if (!io.readBoolean()) {
                    state = ENDED
                    io.write(exitNotAckMe)
                    return@use
                }

                state = ACKED_ME
                io.write(ackedMe)

                while (true) {
                    state = IDLE
                    val op = io.readByte()
                    when (op.toInt()) {
                        opExit -> {
                            state = ENDED
                            return@use
                        }

                        opCheck -> {
                            state = ON_INTERNAL_CALL
                            (io).writeInt(handshake)
                                .writeString(serverName)
                                .writeShort(ackMe)
                        }

                        opList -> {
                            state = ON_INTERNAL_CALL
                            io.write(opReqAck).writeSizedStringArray(calls.keys.toTypedArray())
                        }

                        opListExt -> {
                            state = ON_INTERNAL_CALL
                            io.write(opReqAck).writeSizedByteArray(extensions.keys.toByteArray())
                        }

                        opCall -> {
                            state = ON_CALL
                            val call = calls[io.write(opReqAckParams).readString()]

                            if (call == null) {
                                io.write(opReqInvalidParams)
                            } else {
                                thisObj.call(io.write(opReqAck))
                            }
                        }

                        else -> {
                            state = ON_EXTENSION_CALL
                            val extension = extensions[op]

                            if (extension == null) {
                                io.write(opReqInvalid)
                            } else {
                                io.write(opReqAckExt)
                                thisObj.extension(io)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Handler of $thisObj of server $serverName caught an exception on state $state:", e)
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ConnectionHandler::class.java)
    }
}