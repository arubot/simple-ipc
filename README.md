# SimpleIPC
IPC Server/Client in Kotlin, with the default Java Sockets implementation and a complete API for other implementations.

## Getting Started

Get it on JCenter: `pw.aru.utils:simple-ipc:LATEST`

Latest Version:

![Latest Version](https://api.bintray.com/packages/adriantodt/maven/simple-ipc/images/download.svg)

### Server:

```kotlin
import pw.aru.utils.ipc.server.connector.socket.SimpleSocket
import pw.aru.utils.ipc.server.ipcServer

fun main(args: Array<String>) {
    ipcServer(SimpleSocket(port)) {
        ...
    }
}
```

### Client:

```kotlin
import pw.aru.utils.ipc.client.builder.socket.SocketClientBuilder

fun main(args: Array<String>) {
    val client = SocketClientBuilder().port(25050).build()
    
    ...
}
```

#### Using Client Pools:
```kotlin
import pw.aru.utils.ipc.client.builder.socket.SocketClientBuilder

fun main(args: Array<String>) {
    val pool = SocketClientBuilder().port(25050).buildPool()
    
    pool.borrow { 
        ...
    }
}
```
