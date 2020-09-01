CC=g++
CFLAGS=-c -Wall -std=c++17
LDFLAGS=
SOURCES=caf-distribuido-coef-clustering.cpp
OBJECTS=$(SOURCES:.cpp=.o)
EXECUTABLE= caf-distribuido-coef-clustering

all: $(SOURCES) $(EXECUTABLE)

$(EXECUTABLE): $(OBJECTS)
	$(CC) $(LDFLAGS) $(OBJECTS) -o $@ -lcaf_io -lcaf_core

.cpp.o:
	$(CC) $(CFLAGS) $< -o $@

clean:
	rm -f $(EXECUTABLE) *.o

