package pw.aru.utils.ipc.client.builder.provided

import pw.aru.utils.io.DataPipe
import pw.aru.utils.ipc.client.builder.ClientBuilder
import pw.aru.utils.ipc.client.connector.ClientConnection
import pw.aru.utils.ipc.client.connector.ClientConnector

class ProvidedClientBuilder(private val provider: () -> DataPipe) : ClientBuilder<Nothing?>() {
    override fun buildConnector() = ProvidedConnector(provider)
}

class ProvidedConnector(private val provider: () -> DataPipe) : ClientConnector<Nothing?> {
    override fun createConnection() = ClientConnection(null, provider())

    override fun toString() = "ProvidedConnector"
}