package pw.aru.utils.ipc.client

import pw.aru.utils.io.DataPipe
import java.io.Closeable

/**
 * The interface which gives methods to interact with an (possibly remote) [IServer].
 */
interface IBaseClient : Closeable {
    /**
     * Returns the list of commands available.
     */
    val commandList: List<String>

    /**
     * Returns the extension codes available.
     */
    val extensionCodes: List<Byte>

    /**
     * Calls a command by its [key].
     *
     * **Please close the [DataPipe] after using it, otherwise it'll render the command invalid.**
     *
     * It's preferred to use the lambda method instead.
     *
     * @throws IllegalArgumentException If the key doesn't exists.
     */
    fun call(key: String): DataPipe

    /**
     * Calls a command by its [key], executing the given [block] and then correctly closing it.
     *
     * @throws IllegalArgumentException If the key doesn't exists.
     */
    fun <R> call(key: String, block: (DataPipe) -> R): R {
        return call(key).use(block)
    }

    /**
     * Calls a extension by it's extension [code].
     *
     * It's preferred to use the lambda method instead.
     *
     * @throws IllegalArgumentException If the code doesn't exists.
     */
    fun extension(code: Byte): DataPipe

    /**
     * Calls a extension by it's extension [code], executing the given [block] and then correctly closing it.
     *
     * @throws IllegalArgumentException If the code doesn't exists.
     */
    fun <R> extension(code: Byte, block: (DataPipe) -> R): R {
        return extension(code).use(block)
    }

}