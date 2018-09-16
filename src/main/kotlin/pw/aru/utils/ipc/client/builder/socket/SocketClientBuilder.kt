package pw.aru.utils.ipc.client.builder.socket

import pw.aru.utils.io.DataPipeStream
import pw.aru.utils.ipc.client.builder.ClientBuilder
import pw.aru.utils.ipc.client.connector.ClientConnection
import pw.aru.utils.ipc.client.connector.ClientConnector
import pw.aru.utils.ipc.proto.Protocol
import java.net.InetAddress
import java.net.Socket

class SocketClientBuilder : ClientBuilder<Socket>() {
    private var address: InetAddress = InetAddress.getLocalHost()
    private var port: Int = -1

    override fun protocol(protocol: Protocol) = apply {
        super.protocol(protocol)
    }

    fun address(address: InetAddress) = apply {
        this.address = address
    }

    fun address(address: String) = apply {
        this.address = InetAddress.getByName(address)
    }

    fun port(port: Int) = apply {
        check(port in 1..65535) { "$port is not a valid port" }
        this.port = port
    }

    override fun buildConnector() = SocketConnector(address, port)
}

class SocketConnector(private val address: InetAddress, private val port: Int) : ClientConnector<Socket> {
    init {
        check(port in 1..65535) { "$port is not a valid port" }
    }

    override fun createConnection(): ClientConnection<Socket> {
        val socket = Socket(address, port)
        return ClientConnection(socket, DataPipeStream(socket.getInputStream(), socket.getOutputStream()))
    }

    override fun toString() = "SocketConnector[$address:$port]"
}