package pw.aru.utils.ipc.server

import pw.aru.utils.ipc.client.IClient
import java.io.Closeable

/**
 * Represents a server that can be connected by [IClient]s.
 */
interface IServer : Closeable