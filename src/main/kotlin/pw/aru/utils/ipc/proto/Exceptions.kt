package pw.aru.utils.ipc.proto

class ProtocolException(val side: ConnectionSide, val state: ConnectionState, message: String?, cause: Throwable?) : Exception(message, cause) {
    constructor(side: ConnectionSide, state: ConnectionState, message: String?) : this(side, state, message, null)

    constructor(side: ConnectionSide, state: ConnectionState, cause: Throwable?) : this(side, state, cause?.toString(), cause)

    constructor(side: ConnectionSide, state: ConnectionState) : this(
        side,
        state,
        "Protocol infringed by ${side.other()} on state $state: ${side.description(state)}"
    )
}

class InvalidProtocolException(message: String) : Exception(message, null)