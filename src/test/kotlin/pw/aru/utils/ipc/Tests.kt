package pw.aru.utils.ipc

import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.plusOrMinus
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import pw.aru.utils.io.DataPipe
import pw.aru.utils.ipc.client.builder.socket.SocketClientBuilder
import pw.aru.utils.ipc.server.connector.socket.SimpleSocket
import pw.aru.utils.ipc.server.ipcServer
import java.util.Arrays.equals
import java.util.concurrent.Executors.newSingleThreadExecutor

class Tests : StringSpec({
    val example: Byte = 100
    "Connect and Disconnect" {
        val server = ipcServer(SimpleSocket(25050)) {
            serverName = "Server#25050"
            executor = ::newSingleThreadExecutor
            backlog = 1
        }

        val client = SocketClientBuilder().port(25050).build()

        client.serverName shouldBe "Server#25050"
        client.commandList.isEmpty().shouldBeTrue()
        client.extensionCodes.isEmpty().shouldBeTrue()

        client.close()
        server.close()
    }

    "Create IPCServer using DSL" {
        val server = ipcServer(SimpleSocket(25051)) {
            serverName = "Server#25051"
            executor = ::newSingleThreadExecutor
            backlog = 1
            call("hello") {
                it.writeString("Hello World!")
            }
            extension(example) {
                it.writeBoolean(true)
            }
        }
        val client = SocketClientBuilder().port(25051).build()

        client.commandList shouldBe listOf("hello")
        client.extensionCodes shouldBe listOf(example)

        client.close()
        server.close()
    }

    "Illegal DSL calls" {
        ipcServer(SimpleSocket(25052)) {
            serverName = "Server#25052"
            executor = ::newSingleThreadExecutor
            backlog = 1
            call("hello") {}
            extension(example) {}
            shouldThrow<IllegalStateException> {
                call("hello") {}
            }
            shouldThrow<IllegalStateException> {
                extension(example) {}
            }
        }.close()
    }

    "Simple Data Test" {
        val server = ipcServer(SimpleSocket(25053)) {
            serverName = "Server#25053"
            executor = ::newSingleThreadExecutor
            backlog = 1
            call("hello") {
                it.writeString("Hello World!")
            }
            extension(example) {
                it.writeBoolean(true)
            }
        }
        val client = SocketClientBuilder().port(25053).build()

        client.call("hello", DataPipe::readString) shouldBe "Hello World!"
        client.extension(example, DataPipe::readBoolean).shouldBeTrue()
        client.close()
        server.close()
    }

    "Data Write Test" {
        val server = ipcServer(SimpleSocket(25054)) {
            serverName = "Server#25054"
            executor = ::newSingleThreadExecutor
            backlog = 1
            call("raw") { it.writeBytes(byteArrayOf(1, 2, 3)) }
            call("string") { it.writeString("abc") }
            call("int") { it.writeInt(1) }
            call("long") { it.writeLong(2L) }
            call("boolean") { it.writeBoolean(true) }
            call("double") { it.writeDouble(1.0) }
            call("float") { it.writeFloat(2.0f) }
            call("char") { it.writeChar('a'.toInt()) }
        }
        val client = SocketClientBuilder().port(25054).build()

        val buffer = byteArrayOf(0, 0, 0)
        client.call("raw") { it.readFully(buffer) }
        buffer shouldBe byteArrayOf(1, 2, 3)

        client.call("string", DataPipe::readString) shouldBe "abc"
        client.call("int", DataPipe::readInt) shouldBe 1
        client.call("long", DataPipe::readLong) shouldBe 2L
        client.call("boolean", DataPipe::readBoolean).shouldBeTrue()
        client.call("double", DataPipe::readDouble) plusOrMinus 1.0
        client.call("float", DataPipe::readFloat) shouldBe 2.0f
        client.call("char", DataPipe::readChar) shouldBe 'a'

        client.close()
        server.close()
    }

    "Data Read Test" {
        val server = ipcServer(SimpleSocket(25055)) {
            serverName = "Server#25055"
            executor = ::newSingleThreadExecutor
            backlog = 1
            call("raw") {
                val buffer = byteArrayOf(0, 0, 0)
                it.readFully(buffer).writeBoolean(equals(buffer, byteArrayOf(1, 2, 3)))
            }
            call("string") { it.writeBoolean(it.readString() == "abc") }
            call("int") { it.writeBoolean(it.readInt() == 1) }
            call("long") { it.writeBoolean(it.readLong() == 2L) }
            call("boolean") { it.writeBoolean(it.readBoolean()) }
            call("double") { it.writeBoolean(it.readDouble() == 1.0) }
            call("float") { it.writeBoolean(it.readFloat() == 2.0f) }
            call("char") { it.writeBoolean(it.readChar() == 'a') }
        }
        val client = SocketClientBuilder().port(25055).build()

        client.call("raw") { it.writeBytes(byteArrayOf(1, 2, 3)).readBoolean() }.shouldBeTrue()
        client.call("string") { it.writeString("abc").readBoolean() }.shouldBeTrue()
        client.call("int") { it.writeInt(1).readBoolean() }.shouldBeTrue()
        client.call("long") { it.writeLong(2L).readBoolean() }.shouldBeTrue()
        client.call("boolean") { it.writeBoolean(true).readBoolean() }.shouldBeTrue()
        client.call("double") { it.writeDouble(1.0).readBoolean() }.shouldBeTrue()
        client.call("float") { it.writeFloat(2.0f).readBoolean() }.shouldBeTrue()
        client.call("char") { it.writeChar('a'.toInt()).readBoolean() }.shouldBeTrue()

        client.close()
        server.close()
    }

    "Persistence Test" {
        val server = ipcServer(SimpleSocket(25056)) {
            serverName = "Server#25056"
            executor = ::newSingleThreadExecutor
            backlog = 1
            var s = ""
            call("set") {
                s = it.readString()
            }
            call("get") {
                it.writeString(s)
            }
        }
        val client1 = SocketClientBuilder().port(25056).build()

        client1.call("get") { it.readString() } shouldBe ""
        client1.call("set") { it.writeString("abc") }

        client1.close()

        val client2 = SocketClientBuilder().port(25056).build()

        client2.call("get") { it.readString() } shouldBe "abc"

        client2.close()
        server.close()
    }

    "Client Pool Test" {
        val server = ipcServer(SimpleSocket(25057)) {
            serverName = "Server#25057"
            backlog
            call("hello") {
                it.writeString("Hello World!")
            }
            extension(example) {
                it.writeBoolean(true)
            }
        }

        val clientPool = SocketClientBuilder().port(25057).buildPool(1)

        clientPool.call("hello", DataPipe::readString) shouldBe "Hello World!"
        clientPool.extension(example, DataPipe::readBoolean).shouldBeTrue()

        clientPool.close()
        server.close()
    }

    "Idle/Busy Test" {
        val server = ipcServer(SimpleSocket(25058)) {
            serverName = "Server#25058"
            executor = ::newSingleThreadExecutor
            backlog = 1

            call("simple") {
                it.writeLong(0x1234567890)
            }
        }
        val client = SocketClientBuilder().port(25058).build()

        client.call("simple") {
            it.readInt() //wrong call, buffer still "full"
            client.isIdle.shouldBeFalse()
            it.readInt()
            client.isIdle.shouldBeFalse()
        }

        client.isIdle.shouldBeTrue()

        client.close()
        server.close()
    }
})