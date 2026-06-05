all: MemView.class

MemView.class: MemView.java Hilbert.java BinaryVisualizer.java
	javac -cp .:json-simple-1.1.1.jar -g MemView.java

run: MemView.class
	java -cp .:json-simple-1.1.1.jar MemView
