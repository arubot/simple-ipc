package pw.aru.utils.ipc.server

import pw.aru.utils.ipc.proto.ConnectionState

class ServerException(val state: ConnectionState, message: String?, cause: Throwable?) : Exception(message, cause) {
    constructor(state: ConnectionState, message: String?) : this(state, message, null)

    constructor(state: ConnectionState, cause: Throwable?) : this(state, cause?.toString(), cause)

    constructor(state: ConnectionState) : this(state, null, null)
}