package pw.aru.utils.ipc.proto

enum class ConnectionSide {
    CLIENT, SERVER;

    fun description(state: ConnectionState) = when (this) {
        CLIENT -> state.clientDescription
        SERVER -> state.serverDescription
    }

    fun other() = when (this) {
        CLIENT -> SERVER
        SERVER -> CLIENT
    }
}

enum class ConnectionState(val clientDescription: String, val serverDescription: String) {
    PRE_HANDSHAKE("Before Handshake"),
    HANDSHAKE("Handshaking"),
    ACK_ME("Asked AckMe to Client", "Server Asked AckMe"),
    ACKED_ME("Client AckEd Server", "AckEd Server"),

    IDLE("Idle"),

    ON_CALL("On Call()"),
    ON_INTERNAL_CALL("On Internal Operations"),
    ON_EXTENSION_CALL("On Extension()"),

    ENDED("Gracefully Ended"),
    BROKEN("Broken Connection");

    constructor(description: String) : this(description, description)
}