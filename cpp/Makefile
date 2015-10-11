CFLAGS = -shared -fpic `pkg-config --cflags opencv`
CFLAGS += -I/etc/alternatives/java_sdk_1.8.0/include/ -I/etc/alternatives/java_sdk_1.8.0/include/linux
LIBS = `pkg-config --libs opencv`

all:
	g++ $(CFLAGS) $(LIBS) -o ../lib/libfacerec.so facerec.cpp
