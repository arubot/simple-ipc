@file:JvmName("IPC")
@file:JvmMultifileClass

package pw.aru.utils.ipc.server

import pw.aru.utils.ipc.proto.DefaultProtocol
import pw.aru.utils.ipc.proto.Protocol
import pw.aru.utils.ipc.server.connector.ServerConnectorFactory
import pw.aru.utils.ipc.server.dsl.ServerBuilder
import pw.aru.utils.ipc.server.impl.ServerImpl

/**
 * Creates and configures an [IServer].
 *
 * @param factory The connector factory.
 * @param block The configuration of the server.
 * @return the configurated and running [IServer].
 */
fun <B : ServerBuilder<T>, T> ipcServer(
    factory: ServerConnectorFactory<B, T>, protocol: Protocol = DefaultProtocol, block: B.() -> Unit
): IServer {
    val builder = factory.makeBuilder(protocol).apply(block)
    val connector = factory.buildConnector(builder)

    return ServerImpl(
        protocol,
        builder.serverName,
        connector,
        builder.executor(),
        builder.calls,
        builder.extensions
    )
}