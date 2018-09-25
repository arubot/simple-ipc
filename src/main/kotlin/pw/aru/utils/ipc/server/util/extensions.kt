package pw.aru.utils.ipc.server.util

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

/**
 * Creates a [ThreadFactory].
 *
 * @param isDaemon if `true`, the threads will be created as a daemon threads.
 * The Java Virtual Machine exits when the only threads running are all daemon threads.
 * @param contextClassLoader the class loader to use for loading classes and resources in the created threads.
 * @param nameFormat the name format for the created threads.
 * @param priority the priority of the created threads.
 */
fun threadFactory(
    isDaemon: Boolean = false,
    contextClassLoader: ClassLoader? = null,
    nameFormat: String? = null,
    priority: Int = -1
): ThreadFactory {
    val count = if (nameFormat != null) AtomicLong(0) else null
    return ThreadFactory {
        thread(false, isDaemon, contextClassLoader, nameFormat?.format(count!!.getAndIncrement()), priority, it::run)
    }
}