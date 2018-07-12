package pw.aru.utils.ipc.client

import java.net.InetAddress
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.math.max

/**
 * Creates a pool of [IPCClient]s connected on the specified [address] and [port].
 *
 * Once created, the pool will automatically init a single client.
 *
 * @param address The [InetAddress] the clients from the pool will connect/are connected to.
 * @param port The port the clients from the pool will connect/are connected to.
 * @throws IllegalStateException If the handshake fails or the protocol is infringed on the first connection.
 */
class IPCClientPool(val address: InetAddress = InetAddress.getLocalHost(), val port: Int) {
    private val queue: Queue<IPCClient> = LinkedBlockingQueue()
    private var created = 0
    private var maxCreated = 0

    init {
        queue.add(IPCClient(address, port))
    }

    /**
     * The amount of clients available to be borrowed from the pool.
     */
    val available: Int
        get() = queue.size

    /**
     * Obtains an [IPCClient] from this pool and executes the given [block] function on this resource.
     *
     * If no clients are available, a new client will be created.
     *
     * @param block a function where the [IPCClient] will be available.
     * @return the result of [block] function.
     */
    fun <R> borrow(block: (IPCClient) -> R): R {
        val client = queue.poll().let {
            if (it == null || !it.isValid) {
                it?.close()

                created++
                maxCreated = max(maxCreated, created)
                IPCClient(address, port)
            } else {
                created = 0
                it
            }
        }

        try {
            return block(client)
        } finally {
            if (client.isValid) {
                queue.offer(client)

                val c = maxCreated
                if (available > c + 2) {
                    maxCreated = 0
                    launchCleanup(c)
                }
            }
        }
    }

    private fun launchCleanup(lastMaxCreated: Int) {
        thread(name = "IPCClientPool-CleanupThread") {
            for (i in 0..(available - lastMaxCreated)) {
                queue.poll()?.close()
            }
        }
    }
}