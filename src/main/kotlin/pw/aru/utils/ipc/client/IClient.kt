package pw.aru.utils.ipc.client

import pw.aru.utils.io.DataPipe
import pw.aru.utils.ipc.proto.ConnectionState

interface IClient<T> : IBaseClient {
    /**
     * The raw Connector object.
     */
    val connector: T

    /**
     * @returns The server's name, defined on the server handshake.
     */
    val serverName: String

    /**
     * The raw [DataPipe] used to interface with the socket.
     *
     * (You can use [DataPipe.getInputStream] and [DataPipe.getOutputStream] to get the original streams.)
     */
    val pipe: DataPipe
    /**
     * Returns the Client's current Connection State.
     */
    val state: ConnectionState
    /**
     * Returns if the socket is still alive.
     */
    val isAlive: Boolean
    /**
     * Checks if the client is valid, meaning that it can answer calls/extensions.
     */
    val isIdle: Boolean

}