package pw.aru.utils.ipc.client.builder

import pw.aru.utils.ipc.client.IClient
import pw.aru.utils.ipc.client.IClientPool
import pw.aru.utils.ipc.client.connector.ClientConnector
import pw.aru.utils.ipc.client.impl.ClientImpl
import pw.aru.utils.ipc.client.impl.ClientPoolImpl
import pw.aru.utils.ipc.proto.DefaultProtocol
import pw.aru.utils.ipc.proto.Protocol

abstract class ClientBuilder<T> {
    private var protocol: Protocol = DefaultProtocol

    open fun protocol(protocol: Protocol) = apply {
        this.protocol = protocol
    }

    protected abstract fun buildConnector(): ClientConnector<T>

    fun build(): IClient<T> = ClientImpl(protocol, buildConnector())

    fun buildPool(queueSize: Int = Int.MAX_VALUE, buildInitial: Boolean = true): IClientPool<T> = ClientPoolImpl(protocol, buildConnector(), queueSize, buildInitial)
}

