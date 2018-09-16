package pw.aru.utils.ipc.client.impl

import pw.aru.utils.io.DataPipe
import pw.aru.utils.ipc.client.ClientException
import pw.aru.utils.ipc.client.IClient
import pw.aru.utils.ipc.client.connector.ClientConnector
import pw.aru.utils.ipc.proto.*
import pw.aru.utils.ipc.proto.ConnectionSide.CLIENT
import pw.aru.utils.ipc.proto.ConnectionState.*

class ClientImpl<T>(protocol: Protocol, connector: ClientConnector<T>) : IClient<T>, Protocol by protocol {

    override val connector: T
    override val pipe: DataPipe

    override val serverName: String get() = svname
    private lateinit var svname: String

    override val state: ConnectionState get() = s
    private var s: ConnectionState

    init {
        checkProtocol()
        val (obj, io) = connector.createConnection()
        this.connector = obj
        this.pipe = io

        s = PRE_HANDSHAKE
        connect()
    }

    private fun connect() {
        s = HANDSHAKE
        clientCheck(s, pipe.readInt() == handshake)
        svname = pipe.readString()

        s = ACK_ME
        clientCheck(s, pipe.readUnsignedShort() == ackMe)
        pipe.writeBoolean(true)

        s = ACKED_ME
        clientCheck(s, pipe.readUnsignedByte() == ackedMe)

        s = IDLE
    }

    override fun call(key: String): DataPipe {
        clientCheck(s, isIdle, "Client should be on IDLE to accept opCALL code.")
        s = ON_CALL
        pipe.write(opCall)
        clientCheck(s, pipe.readUnsignedByte() == opReqAckParams, "opCALL answer wasn't opReqAckParams")

        val op = pipe.writeString(key).readUnsignedByte()
        when (op) {
            opReqAck -> return ClientDataPipe(pipe)
            opReqInvalidParams -> {
                s = IDLE
                throw IllegalArgumentException("Unknown call '$key'")
            }
            else -> {
                s = BROKEN
                throw ProtocolException(CLIENT, state, "Protocol infringed by SERVER: call opCALL answer to key $key")
            }
        }
    }

    override fun extension(code: Byte): DataPipe {
        val hexCode = "0x" + code.toString(16).toUpperCase()

        clientCheck(s, isIdle, "Client should be on IDLE to accept opCALL code.")
        s = ON_EXTENSION_CALL

        val op = pipe.write(code.toInt()).readUnsignedByte()
        when (op) {
            opReqAckExt -> return ClientDataPipe(pipe)
            opReqInvalid -> {
                s = IDLE
                throw IllegalArgumentException("Invalid extension code $hexCode")
            }
            else -> {
                s = BROKEN
                throw ProtocolException(CLIENT, state, "Protocol infringed by SERVER: answer to extension $code")
            }
        }
    }

    /**
     * Returns the list of commands available.
     */
    override val commandList: List<String>
        get () {
            clientCheck(s, isIdle, "Client should be on IDLE to accept opCALL code.")
            s = ON_INTERNAL_CALL

            pipe.write(opList)
            clientCheck(s, pipe.readUnsignedByte() == opReqAck, "opCALL answer wasn't opReqAck")

            val list = pipe.readSizedStringArray().toList()
            s = IDLE
            return list
        }

    /**
     * Returns the extension codes available.
     */
    override val extensionCodes: List<Byte>
        get () {
            clientCheck(s, isIdle, "Client should be on IDLE to accept opCALL code.")
            s = ON_INTERNAL_CALL

            pipe.write(opListExt)
            clientCheck(s, pipe.readUnsignedByte() == opReqAck, "opCALL answer wasn't opReqAck")

            val list = pipe.readSizedByteArray().toList()
            s = IDLE
            return list
        }

    override val isAlive: Boolean get() = !closed

    private var closed = false

    override fun close() {
        if (!closed) {
            try {
                clientCheck(s, s == IDLE, "Client should be on IDLE to accept opEXIT code.")
                pipe.write(opExit)
                s = ENDED
            } catch (p: ProtocolException) {
                s = BROKEN
                throw p
            } catch (e: Exception) {
                s = BROKEN
                throw ClientException(state, e)
            } finally {
                pipe.close()
                closed = true
            }
        }
    }

    override val isIdle: Boolean get() = s == IDLE

    override fun toString(): String = "$connector.Client on $serverName"

    private inner class ClientDataPipe(private val pipe: DataPipe) : DataPipe by pipe {
        override fun close() {
            clientCheck(s, s == ON_CALL || s == ON_INTERNAL_CALL || s == ON_EXTENSION_CALL, "Client should be on a anyOpCALL to return to IDLE.")
            s = IDLE
        }
    }
}