DNSViz LookingGlass (Java)
=================

[DNSViz](http://dnsviz.net/) is a service for investigating DNS lookups. It has
an extendable feature called "looking glass" that allows DNSViz to make the
query from a different perspective.

This project enables DNSViz to operate any Java client to investigate DNS
queries from its perspective.


### Running

```bash
gradle run '-Pmyargs=::1 8080 /lg/?fn=12345 http://localhost/'
```

Command line arguments
- host
- port
- path for the socket file
- origin


