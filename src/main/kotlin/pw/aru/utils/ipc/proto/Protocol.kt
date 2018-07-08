package pw.aru.utils.ipc.proto

//Handshake
const val handshake = 0x0800CAFE
const val ackMe = 0xDEAD

//Ack
const val exitNotAckMe = -1
const val ackedMe = 1

//Default IPC operations
const val opExit = 0
const val opCall = 1
const val opList = 2
const val opListExt = 3

//IPC operations answers
const val opReqAck = 1
const val opReqAckParams = 2
const val opReqAckExt = 3

//IPC operations errors
const val opReqInvalid = -1
const val opReqInvalidParams = -2