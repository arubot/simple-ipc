package pw.aru.utils.ipc.server

import pw.aru.utils.io.DataPipe
import java.lang.reflect.Array.*
import java.lang.reflect.Method
import java.net.Socket

class ReflectionHandler<T : Any>(val obj: T) : AbstractMap<String, Socket.(DataPipe) -> Unit>() {

    private val methods = obj.javaClass.methods.filter {
        (it.declaringClass != Any::class.java || it.name == "toString" || it.name == "hashCode")
            && isTypeValid(it.returnType)
            && it.parameterTypes.all(this::isTypeValid)
    }

    override val entries: Set<Map.Entry<String, Socket.(DataPipe) -> Unit>> by lazy {
        methods.mapTo(LinkedHashSet()) {
            object : Map.Entry<String, Socket.(DataPipe) -> Unit> {
                override val key: String = it.name
                override val value: Socket.(DataPipe) -> Unit by lazy { handler(it) }
            }
        }
    }

    private fun isTypeValid(c: Class<*>): Boolean = c.isPrimitive || c == String::class.java || (c.isArray && isTypeValid(c.componentType))

    private fun readType(c: Class<*>, io: DataPipe): Any {
        return when (c) {
            java.lang.Boolean.TYPE -> io.readBoolean()
            java.lang.Byte.TYPE -> io.readByte()
            java.lang.Character.TYPE -> io.readChar()
            java.lang.Short.TYPE -> io.readShort()
            java.lang.Integer.TYPE -> io.readInt()
            java.lang.Float.TYPE -> io.readFloat()
            java.lang.Long.TYPE -> io.readLong()
            java.lang.Double.TYPE -> io.readDouble()
            java.lang.String::class.java -> io.readString()
            else -> when {
                c.isArray && isTypeValid(c.componentType) -> {
                    val t = c.componentType
                    val size = io.readInt()
                    val array = newInstance(t, size)
                    for (i in 0 until size) set(array, i, readType(t, io))
                    array
                }
                else -> throw IllegalStateException("$c is not a primitive nor primitive nor String")
            }
        }
    }

    private fun writeObject(obj: Any, io: DataPipe) {
        when (obj) {
            is Boolean -> io.writeBoolean(obj)
            is Byte -> io.writeByte(obj.toInt())
            is Char -> io.writeChar(obj.toInt())
            is Short -> io.writeShort(obj.toInt())
            is Int -> io.writeInt(obj)
            is Float -> io.writeFloat(obj)
            is Long -> io.writeLong(obj)
            is Double -> io.writeDouble(obj)
            is String -> io.writeString(obj)

            else -> when {
                obj.javaClass.isArray && isTypeValid(obj.javaClass.componentType) -> {
                    val size = getLength(obj)
                    io.writeInt(size)
                    for (i in 0 until size) writeObject(get(obj, i), io)
                }
                else -> throw IllegalStateException("$obj is not a primitive nor String")
            }
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")

    private fun handler(method: Method): Socket.(DataPipe) -> Unit = { io ->
        val params = method.parameterTypes.map { readType(it, io) }

        val r = method.invoke(obj, *params.toTypedArray())

        if (r != null) writeObject(r, io)
    }
}