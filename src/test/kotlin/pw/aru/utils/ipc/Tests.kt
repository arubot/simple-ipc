package pw.aru.utils.ipc

import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.plusOrMinus
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import pw.aru.utils.io.DataPipe
import pw.aru.utils.ipc.client.IPCClient
import pw.aru.utils.ipc.server.IPCServer
import pw.aru.utils.ipc.server.ReflectionHandler
import pw.aru.utils.ipc.server.server
import java.util.Arrays.equals
import java.util.concurrent.Executors.newSingleThreadExecutor

class Tests : StringSpec({
    val example: Byte = 100
    "Connect and Disconnect" {
        val server = IPCServer("Server#25050", 25050, emptyMap(), executor = newSingleThreadExecutor(), backlog = 1)
        val client = IPCClient(port = 25050)

        client.serverName shouldBe server.serverName
        client.commandList.isEmpty().shouldBeTrue()
        client.extensionCodes.isEmpty().shouldBeTrue()

        client.close()
        server.close()
    }

    "Create IPCServer using DSL" {
        val server = server("Server#25051", 25051, executor = newSingleThreadExecutor()) {
            backlog = 1
            call("hello") {
                it.writeString("Hello World!")
            }
            extension(example) {
                it.writeBoolean(true)
            }
        }
        val client = IPCClient(port = 25051)

        client.commandList shouldBe listOf("hello")
        client.extensionCodes shouldBe listOf(example)

        client.close()
        server.close()
    }

    "Illegal DSL calls" {
        server("Server#25052", 25052, executor = newSingleThreadExecutor()) {
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
        val server = server("Server#25053", 25053, executor = newSingleThreadExecutor()) {
            backlog = 1
            call("hello") {
                it.writeString("Hello World!")
            }
            extension(example) {
                it.writeBoolean(true)
            }
        }
        val client = IPCClient(port = 25053)

        client.call("hello").readString() shouldBe "Hello World!"
        client.extension(example).readBoolean().shouldBeTrue()

        client.close()
        server.close()
    }

    "Data Write Test" {
        val server = server("Server#25054", 25054, executor = newSingleThreadExecutor()) {
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
        val client = IPCClient(port = 25054)

        val buffer = byteArrayOf(0, 0, 0)
        client.call("raw").readFully(buffer)
        buffer shouldBe byteArrayOf(1, 2, 3)

        client.call("string").readString() shouldBe "abc"
        client.call("int").readInt() shouldBe 1
        client.call("long").readLong() shouldBe 2L
        client.call("boolean").readBoolean().shouldBeTrue()
        client.call("double").readDouble() plusOrMinus 1.0
        client.call("float").readFloat() shouldBe 2.0f
        client.call("char").readChar() shouldBe 'a'

        client.close()
        server.close()
    }

    "Data Read Test" {
        val server = server("Server#25055", 25055, executor = newSingleThreadExecutor()) {
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
        val client = IPCClient(port = 25055)

        client.call("raw").writeBytes(byteArrayOf(1, 2, 3)).readBoolean().shouldBeTrue()
        client.call("string").writeString("abc").readBoolean().shouldBeTrue()
        client.call("int").writeInt(1).readBoolean().shouldBeTrue()
        client.call("long").writeLong(2L).readBoolean().shouldBeTrue()
        client.call("boolean").writeBoolean(true).readBoolean().shouldBeTrue()
        client.call("double").writeDouble(1.0).readBoolean().shouldBeTrue()
        client.call("float").writeFloat(2.0f).readBoolean().shouldBeTrue()
        client.call("char").writeChar('a'.toInt()).readBoolean().shouldBeTrue()

        client.close()
        server.close()
    }

    "Persistence Test" {
        val server = server("Server#25056", 25056, executor = newSingleThreadExecutor()) {
            backlog = 1
            var s = ""
            call("set") {
                s = it.readString()
            }
            call("get") {
                it.writeString(s)
            }
        }
        val client1 = IPCClient(port = 25056)

        client1.call("get").readString() shouldBe ""
        client1.call("set").writeString("abc")

        client1.close()

        val client2 = IPCClient(port = 25056)

        client2.call("get").readString() shouldBe "abc"

        client2.close()
        server.close()
    }

    "Simple Reflection Test" {
        class SimpleReflectionTest {
            fun sum(a: Int, b: Int) = a + b
            fun avg(a: DoubleArray) = a.average()
        }

        val server = IPCServer("Server#25057", 25057, ReflectionHandler(SimpleReflectionTest()), executor = newSingleThreadExecutor(), backlog = 1)
        val client = IPCClient(port = 25057)

        client.call("sum").writeInt(1).writeInt(2).readInt() shouldBe 3
        client.call("avg").writeInt(3)
            .writeDouble(1.0)
            .writeDouble(2.0)
            .writeDouble(3.0)
            .readDouble() plusOrMinus 2.0

        client.close()
        server.close()
    }


    "Reflection Write Test" {
        class ReflectionWriteTest {
            fun raw() = byteArrayOf(1, 2, 3)
            fun string() = "abc"
            fun int() = 1
            fun long() = 2L
            fun boolean() = true
            fun double() = 1.0
            fun float() = 2.0f
            fun char() = 'a'
            fun direct(io: DataPipe) {
                io.writeInt(25058).writeString("xyz")
            }
        }

        val server = IPCServer("Server#25058", 25058, ReflectionHandler(ReflectionWriteTest()), executor = newSingleThreadExecutor(), backlog = 1)
        val client = IPCClient(port = 25058)

        with(client.call("raw")) {
            readInt() shouldBe 3

            val buffer = byteArrayOf(0, 0, 0)
            readFully(buffer)
            buffer shouldBe byteArrayOf(1, 2, 3)
        }

        client.call("string").readString() shouldBe "abc"
        client.call("int").readInt() shouldBe 1
        client.call("long").readLong() shouldBe 2L
        client.call("boolean").readBoolean().shouldBeTrue()
        client.call("double").readDouble() plusOrMinus 1.0
        client.call("float").readFloat() shouldBe 2.0f
        client.call("char").readChar() shouldBe 'a'

        with(client.call("direct")) {
            readInt() shouldBe 25058
            readString() shouldBe "xyz"
        }

        client.close()
        server.close()
    }

    "Reflection Read Test" {
        class ReflectionReadTest {
            fun raw(b: ByteArray) = equals(b, byteArrayOf(1, 2, 3))
            fun string(s: String) = (s == "abc")
            fun int(i: Int) = (i == 1)
            fun long(l: Long) = (l == 2L)
            fun boolean(b: Boolean) = (b)
            fun double(d: Double) = (d == 1.0)
            fun float(f: Float) = (f == 2.0f)
            fun char(c: Char) = (c == 'a')
            fun direct(io: DataPipe) = (io.readInt() == 25059 && io.readString() == "xyz")
        }

        val server = IPCServer("Server#25059", 25059, ReflectionHandler(ReflectionReadTest()), executor = newSingleThreadExecutor(), backlog = 1)
        val client = IPCClient(port = 25059)

        client.call("raw").writeInt(3).writeBytes(byteArrayOf(1, 2, 3)).readBoolean().shouldBeTrue()
        client.call("string").writeString("abc").readBoolean().shouldBeTrue()
        client.call("int").writeInt(1).readBoolean().shouldBeTrue()
        client.call("long").writeLong(2L).readBoolean().shouldBeTrue()
        client.call("boolean").writeBoolean(true).readBoolean().shouldBeTrue()
        client.call("double").writeDouble(1.0).readBoolean().shouldBeTrue()
        client.call("float").writeFloat(2.0f).readBoolean().shouldBeTrue()
        client.call("char").writeChar('a'.toInt()).readBoolean().shouldBeTrue()
        client.call("direct").writeInt(25059).writeString("xyz").readBoolean().shouldBeTrue()

        server.close()
    }

    "Valid Instance Test" {
        val server = server("Server#25060", 25060, executor = newSingleThreadExecutor()) {
            backlog = 1
            call("simple") {
                it.writeBoolean(true)
            }
        }
        val client = IPCClient(port = 25060)

        client.call("simple").readBoolean().shouldBeTrue()
        client.isValid.shouldBeTrue()

        client.close()
        server.close()
    }


    "Invalid Instance Test" {
        val server = server("Server#25061", 25061, executor = newSingleThreadExecutor()) {
            backlog = 1
            call("simple") {
                it.writeLong(0x1234567890)
            }
        }
        val client = IPCClient(port = 25061)

        client.call("simple").readInt() //wrong call, buffer still "full"
        client.isValid.shouldBeFalse()

        client.close()
        server.close()
    }
})