CC=g++
CFLAGS=-c -Wall -std=c++17
LDFLAGS=
SOURCES=caf-local-coef-clustering.cpp
OBJECTS=$(SOURCES:.cpp=.o)
EXECUTABLE= caf-local-coef-clustering

all: $(SOURCES) $(EXECUTABLE)

$(EXECUTABLE): $(OBJECTS)
	$(CC) $(LDFLAGS) $(OBJECTS) -o $@ -lcaf_io -lcaf_core

.cpp.o:
	$(CC) $(CFLAGS) $< -o $@

clean:
	rm -f $(EXECUTABLE) *.o

