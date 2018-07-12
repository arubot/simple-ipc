# Simple-IPC
IPC Server/Client in Kotlin implemented with Java Sockets.

## Getting Started

Get it on JCenter: `pw.aru.utils:simple-ipc:1.1`

### Server:

```kotlin
import pw.aru.utils.ipc.server.server

fun main(args: Array<String>) {
    server(serverName, serverPort) {
        ...
    }
}
```

### Client:

```kotlin
import pw.aru.utils.ipc.client.IPCClient

fun main(args: Array<String>) {
    val client = IPCClient(port = 2020)
    
    ...
}
```

#### Using Client Pools:
```kotlin
import pw.aru.utils.ipc.client.IPCClientPool

fun main(args: Array<String>) {
    val pool = IPCClientPool(port = 2020)
    
    pool.borrow { 
        ...
    }
}
```