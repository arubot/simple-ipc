package pw.aru.utils.ipc.server.connector

import pw.aru.utils.io.DataPipe
import pw.aru.utils.ipc.proto.Protocol
import pw.aru.utils.ipc.server.dsl.ServerBuilder

interface ServerConnectorFactory<TBuilder : ServerBuilder<TInstance>, TInstance> {
    fun makeBuilder(protocol: Protocol): TBuilder

    fun buildConnector(builder: TBuilder): ServerConnector<TInstance>
}

interface ServerConnector<T> {
    fun awaitNextConnection(): ServerConnection<T>
    fun shutdown()
}

data class ServerConnection<T>(val connection: T, val pipe: DataPipe)