all:
	mkdir -p build/
	javac -d ./build src/*.java src/*/*.java

clean:
	rm -rf build/*
