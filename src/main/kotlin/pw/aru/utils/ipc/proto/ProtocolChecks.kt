@file:Suppress("NOTHING_TO_INLINE")

package pw.aru.utils.ipc.proto

import pw.aru.utils.ipc.proto.ConnectionSide.CLIENT

inline fun clientCheck(state: ConnectionState, value: Boolean) {
    protoCheck(CLIENT, state, value)
}

inline fun clientCheck(state: ConnectionState, value: Boolean, check: String) {
    protoCheck(CLIENT, state, value, check)
}

inline fun serverCheck(state: ConnectionState, value: Boolean) {
    protoCheck(CLIENT, state, value)
}

inline fun serverCheck(state: ConnectionState, value: Boolean, check: String) {
    protoCheck(CLIENT, state, value)
}

inline fun protoCheck(side: ConnectionSide, state: ConnectionState, value: Boolean) {
    if (!value) throw ProtocolException(side, state)
}

inline fun protoCheck(side: ConnectionSide, state: ConnectionState, value: Boolean, check: String) {
    if (!value) throw ProtocolException(side, state, "Protocol Check failed on state $state: $check")
}

fun Protocol.checkProtocol() {
    protoValidCheck(ackedMe != exitNotAckMe, "AckEdMe and ExitNotAckMe are equal")

    val ops = listOf(
        "opEXIT" to opExit,
        "opCHECK" to opCheck,
        "opLIST" to opList,
        "opLIST_EXT" to opListExt,
        "opCALL" to opCall
    ).groupBy({ it.second.toByte() }, { it.first }).values.filter { it.size > 1 }

    protoValidCheck(ops.isEmpty(), "Opcodes have the same value: ${ops.joinToString { it.joinToString(" , ", "( ", " )") }}")

    val reqs = listOf(
        "opReqACK" to opReqAck,
        "opReqACK_PARAMS" to opReqAckParams,
        "opReqACK_EXT" to opReqAckExt,
        "opReqINVALID" to opReqInvalid,
        "opReqINVALID_PARAMS" to opReqInvalidParams
    ).groupBy({ it.second.toByte() }, { it.first }).values.filter { it.size > 1 }

    protoValidCheck(reqs.isEmpty(), "Opcodes have the same value: ${reqs.joinToString { it.joinToString(" , ", "( ", " )") }}")
}

private inline fun protoValidCheck(value: Boolean, check: String) {
    if (!value) throw InvalidProtocolException(check)
}