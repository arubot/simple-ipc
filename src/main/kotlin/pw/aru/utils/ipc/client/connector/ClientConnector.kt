package pw.aru.utils.ipc.client.connector

import pw.aru.utils.io.DataPipe

interface ClientConnector<T> {
    fun createConnection(): ClientConnection<T>
}

data class ClientConnection<T>(val connection: T, val pipe: DataPipe)