
HOST=::1
PORT=8080
SOCK_PATH=/lg/?fn=12345
ORIGIN=http://localhost/

PARAMS := $(HOST) $(PORT) $(SOCK_PATH) $(ORIGIN)

all:
	gradle fatJar
	java -jar ./build/libs/LookingGlass-Java-all-1.0.jar $(PARAMS)
