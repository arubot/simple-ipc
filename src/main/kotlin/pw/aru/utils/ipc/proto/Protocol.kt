package pw.aru.utils.ipc.proto

interface Protocol {
    //Handshake
    val handshake: Int
    val ackMe: Int

    //Ack
    val exitNotAckMe: Int
    val ackedMe: Int

    //Default IPC operations
    val opExit: Int
    val opCheck: Int
    val opList: Int
    val opListExt: Int
    val opCall: Int

    //IPC operations answers
    val opReqAck: Int
    val opReqAckParams: Int
    val opReqAckExt: Int

    //IPC operations errors
    val opReqInvalid: Int
    val opReqInvalidParams: Int
}

object DefaultProtocol : Protocol {
    //Handshake
    override val handshake = 0x0800CAFE
    override val ackMe = 0xDEAD

    //Ack
    override val exitNotAckMe = -1
    override val ackedMe = 1

    //Default IPC operations
    override val opExit = 0
    override val opCheck = 1
    override val opList = 2
    override val opListExt = 3
    override val opCall = 4

    //IPC operations answers
    override val opReqAck = 1
    override val opReqAckParams = 2
    override val opReqAckExt = 3

    //IPC operations errors
    override val opReqInvalid = -1
    override val opReqInvalidParams = -2
}