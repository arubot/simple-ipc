package pw.aru.utils.ipc.server.connector.provided

import pw.aru.utils.ipc.proto.Protocol
import pw.aru.utils.ipc.server.connector.ServerConnection
import pw.aru.utils.ipc.server.connector.ServerConnector
import pw.aru.utils.ipc.server.connector.ServerConnectorFactory
import pw.aru.utils.ipc.server.dsl.ServerBuilder

class Provided<T> : ServerConnectorFactory<ProvidedServerBuilder<T>, T> {
    override fun makeBuilder(protocol: Protocol) = ProvidedServerBuilder<T>(protocol)
    override fun buildConnector(builder: ProvidedServerBuilder<T>) = ProvidedConnector(
        builder.provider ?: throw IllegalStateException("No provider given.")
    )
}

open class ProvidedServerBuilder<T>(protocol: Protocol) : ServerBuilder<T>(protocol) {
    var provider: (() -> ServerConnection<T>)? = null
}

class ProvidedConnector<T>(val provider: () -> ServerConnection<T>) : ServerConnector<T> {
    override fun awaitNextConnection(): ServerConnection<T> = provider()
    override fun shutdown() = Unit
}