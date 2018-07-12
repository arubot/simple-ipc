package pw.aru.utils.ipc.server

import pw.aru.utils.io.DataPipe
import java.lang.reflect.Array.*
import java.lang.reflect.Method
import java.net.Socket

/**
 * Call-resolver map for [IPCServer], which resolves it's map based on reflection on the given object [obj].
 *
 * The methods that will be available needs to meet the following conditions:
 *  - The method shall be public;
 *  - The method shall not collide with methods with same name but different signatures;
 *  - The method parameters, if any, shall be either of:
 *     - `boolean`, `byte`, `char`, `short`, `int`, `float`, `long`, `double` (primitive types only) or [String] (not-null)
 *     - unidimensional or multidimensional array of any of the above types
 *     - [DataPipe], for direct io access
 *  - The method return type shall be either void or any of the valid parameter types;
 *
 * @param obj The object that will be scanned and the methods called.
 * @param allowObjectAccess (Optional) If set to true, the methods [Object.toString] and [Object.hashCode] will be available, otherwise not.
 */
class ReflectionHandler(private val obj: Any, allowObjectAccess: Boolean = false) : AbstractMap<String, Socket.(DataPipe) -> Unit>() {

    private val methods = obj.javaClass.methods.filter {
        (it.declaringClass != Object::class.java || (allowObjectAccess && (it.name == "toString" || it.name == "hashCode")))
            && isTypeValid(it.returnType)
            && it.parameterTypes.all { this.isTypeValid(it) }
    }.groupBy { it.name }.values.mapNotNull { it.singleOrNull() }

    override val entries: Set<Map.Entry<String, Socket.(DataPipe) -> Unit>> by lazy {
        methods.mapTo(LinkedHashSet()) {
            object : Map.Entry<String, Socket.(DataPipe) -> Unit> {
                override val key = it.name
                override val value by lazy { methodHandler(it) }
            }
        }
    }

    private fun methodHandler(method: Method): (Socket.(DataPipe) -> Unit) = { io ->
        val params = method.parameterTypes.map { readType(it, io) }
        val r = method.invoke(obj, *params.toTypedArray())
        if (r != null) writeObject(r, io)
    }

    private fun isTypeValid(c: Class<*>, insideArray: Boolean = false): Boolean = (!insideArray && c == DataPipe::class.java)
        || c.isPrimitive
        || c == String::class.java
        || (c.isArray && isTypeValid(c.componentType, true))

    private fun readType(c: Class<*>, io: DataPipe): Any {
        return when (c) {
            DataPipe::class.java -> io
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
                else -> throw IllegalStateException("$c is not a valid input type")
            }
        }
    }

    private fun writeObject(obj: Any, io: DataPipe) {
        when (obj) {
            is DataPipe -> return
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
                else -> throw IllegalStateException("$obj is not a valid return type")
            }
        }
    }
}