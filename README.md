DNSViz LookingGlass (Java)
=================

[DNSViz](http://dnsviz.net/) is a service for investigating DNS lookups. It has
an extendable feature called "looking glass" that allows DNSViz to make the
query from a different perspective.

This project enables DNSViz to operate any Java client to investigate DNS
queries from its perspective.


### API
The primary classes are `dnsviz.lookingglass.DNSLookingGlass` and
`dnsviz.websocket.WebSocketClient`. The `DNSLookingGlass` constructor takes no
arguments, but the `WebSocketClient` takes the arguments outlined below for the
CLI, in the same order. To initiate a connection, run the following:

```java
WebSocketClient webSocket = new WebSocketClient(host, port, socketPath, origin);
new DNSLookingGlass().interact(webSocket);
```

This will initiate a connection with the DNSViz server outlined by the
parameters. `interact` will cause the `WebSocketClient` to passively listen for
queries from the DNSViz server, and send back the result of those queries. To
designate specific queries, please use the DNSViz server to push queries to the
LookingGlass.

Currently the entire library is single-threaded, so if using in a UI-based
application such as Android, the above lines of code should be executed within
a background task, with the results fetched onto the main thread for display.


### Running the CLI Tool

```bash
gradle run '-Pmyargs=::1 8080 /lg/?fn=12345 http://localhost/'
```

Or compiling a jar and executing it on the commandline:

```bash
make
```

Command line arguments
- HOST
- PORT
- SOCK_PATH
- ORIGIN

Any of these can be overridde in the `Makefile` using `-e` like so:

```bash
make -e PORT=5000
```

This `make` target will produce a Jar under `build/libs` that can be dropped
into other Java projects.
