package pw.aru.utils.ipc.server.connector.socket

import pw.aru.utils.io.DataPipeStream
import pw.aru.utils.ipc.proto.Protocol
import pw.aru.utils.ipc.server.connector.ServerConnection
import pw.aru.utils.ipc.server.connector.ServerConnector
import pw.aru.utils.ipc.server.connector.ServerConnectorFactory
import pw.aru.utils.ipc.server.dsl.ServerBuilder
import java.net.ServerSocket
import java.net.Socket

class SimpleSocket(private val port: Int) : ServerConnectorFactory<SocketServerBuilder, Socket> {
    init {
        check(port in 1..65535) { "$port is not a valid port" }
    }

    override fun makeBuilder(protocol: Protocol) = SocketServerBuilder(protocol)
    override fun buildConnector(builder: SocketServerBuilder) = SocketConnector(port, builder.backlog)
}

open class SocketServerBuilder(protocol: Protocol) : ServerBuilder<Socket>(protocol) {
    /**
     * Maximum length of the queue of incoming connections on the socket.
     */
    var backlog: Int = 50
        set(value) {
            check(value in 1..65535) { "$value is not a valid value" }
            field = value
        }
}

class SocketConnector(port: Int, backlog: Int) : ServerConnector<Socket> {
    init {
        check(port in 1..65535) { "$port is not a valid port" }
        check(backlog in 1..65535) { "$backlog is not a valid backlog size" }
    }

    private val server: ServerSocket = ServerSocket(port, backlog)

    override fun awaitNextConnection(): ServerConnection<Socket> {
        val socket = server.accept()
        return ServerConnection(socket, DataPipeStream(socket.getInputStream(), socket.getOutputStream()))
    }

    override fun shutdown() {
        server.close()
    }
}