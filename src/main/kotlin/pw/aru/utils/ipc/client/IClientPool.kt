package pw.aru.utils.ipc.client

import pw.aru.utils.io.DataPipe

interface IClientPool<T> : IBaseClient {
    /**
     * The amount of clients available to be borrowed from the pool.
     */
    val available: Int

    fun borrowClient(): IClient<T>

    /**
     * Obtains an [IPCClient] from this pool and executes the given [block] function on this resource.
     *
     * If no clients are available, a new client will be created.
     *
     * @param block a function where the [IPCClient] will be available.
     * @return the result of [block] function.
     */
    fun <R> borrow(block: (IClient<T>) -> R): R {
        return borrowClient().use(block)
    }

    fun <R> tryBorrow(block: (IClient<T>) -> R): R? {
        return try {
            borrow(block)
        } catch (e: Exception) {
            null
        }
    }

    fun cleanup()

    override fun call(key: String): DataPipe {
        return borrowClient().call(key)
    }

    override fun <R> call(key: String, block: (DataPipe) -> R): R {
        return borrow { it.call(key).use(block) }
    }

    fun <R> tryCall(key: String, block: (DataPipe) -> R): R? {
        return tryBorrow { it.call(key).use(block) }
    }

    override fun extension(code: Byte): DataPipe {
        return borrowClient().extension(code)
    }

    override fun <R> extension(code: Byte, block: (DataPipe) -> R): R {
        return borrow { it.extension(code).use(block) }
    }

    fun <R> tryExtension(code: Byte, block: (DataPipe) -> R): R? {
        return tryBorrow { it.extension(code).use(block) }
    }

    override val commandList: List<String>
        get() = borrow(IClient<T>::commandList)

    override val extensionCodes: List<Byte>
        get() = borrow(IClient<T>::extensionCodes)
}