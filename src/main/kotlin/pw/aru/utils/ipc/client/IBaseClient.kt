package pw.aru.utils.ipc.client

import pw.aru.utils.io.DataPipe
import java.io.Closeable

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
     * Calls a command by it's name.
     *
     * @return The raw [DataPipe] used to interface with the socket.
     * @throws IllegalArgumentException If the key doesn't exists.
     * @throws IllegalStateException If the protocol is infringed by the server.
     */
    fun call(key: String): DataPipe

    fun <R> call(key: String, block: (DataPipe) -> R): R {
        return call(key).use(block)
    }

    /**
     * Calls a extension by it's extension code.
     *
     * @return The raw [DataPipe] used to interface with the socket.
     * @throws IllegalArgumentException If the code doesn't exists.
     * @throws IllegalStateException If the protocol is infringed by the server.
     */
    fun extension(code: Byte): DataPipe

    fun <R> extension(code: Byte, block: (DataPipe) -> R): R {
        return extension(code).use(block)
    }

}