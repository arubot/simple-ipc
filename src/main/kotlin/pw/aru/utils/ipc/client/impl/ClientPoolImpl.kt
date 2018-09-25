package pw.aru.utils.ipc.client.impl

import pw.aru.utils.ipc.client.IClient
import pw.aru.utils.ipc.client.IClientPool
import pw.aru.utils.ipc.client.connector.ClientConnector
import pw.aru.utils.ipc.proto.Protocol
import pw.aru.utils.ipc.proto.checkProtocol
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.math.max

internal class ClientPoolImpl<T>(val protocol: Protocol, val connector: ClientConnector<T>, queueSize: Int, buildInitial: Boolean) : IClientPool<T> {

    private var closed = false
    private val queue: Queue<PoolClient> = LinkedBlockingQueue(queueSize)
    private val queueLock = Any()
    private var created = 0
    private var maxCreated = 0

    init {
        protocol.checkProtocol()
        if (buildInitial) queue.add(makeClient())
    }

    private fun makeClient() = PoolClient(ClientImpl(protocol, connector))

    override val available: Int
        get() = queue.size

    override fun borrowClient(): IClient<T> {
        val client = tryPoll()

        if (client == null) {
            created++
            maxCreated = max(maxCreated, created)
            return makeClient()
        }

        created = 0
        return client
    }

    override fun close() {
        closed = true
        for (i in 0..queue.size) {
            try {
                queue.poll()?.client?.close()
            } catch (_: Exception) {
            }
        }
    }

    override fun cleanup() {
        for (i in 0..(queue.size / 2)) {
            try {
                queue.poll()?.client?.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun tryPoll(): IClient<T>? {
        if (closed) throw IllegalStateException("pool closed")
        while (true) {
            val client: IClient<T> = queue.poll() ?: return null

            if (!client.isAlive || !client.isIdle) {
                try {
                    client.close()
                } catch (_: Exception) {
                }

                continue
            }

            return client
        }
    }

    private fun launchCleanup(lastMaxCreated: Int) {
        thread(name = "IPCClientPool-CleanupThread") {
            for (i in 0..(available - lastMaxCreated)) {
                try {
                    queue.poll()?.client?.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun toString(): String = "$connector.ClientPool with $available available clients"

    private inner class PoolClient(val client: IClient<T>) : IClient<T> by client {
        override fun close() {
            //Thread Pool closed
            if (closed) {
                try {
                    client.close()
                } catch (_: Exception) {
                }
                return
            }

            // Invalid
            if (!isAlive || !isIdle) {
                try {
                    client.close()
                } catch (_: Exception) {
                }
                return
            }

            // Queue Full
            if (!queue.offer(this)) {
                try {
                    client.close()
                } catch (_: Exception) {
                }
                return
            }

            // Client offered; proceed to auto-cleanup
            synchronized(queueLock) {
                val lastMaxCreated = maxCreated
                if (available > lastMaxCreated + 2) {
                    maxCreated = 0
                    launchCleanup(lastMaxCreated)
                }
            }
        }
    }
}