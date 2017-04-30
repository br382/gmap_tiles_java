jar_url='http://search.maven.org/remotecontent?filepath=javax/json/javax.json-api/1.0/javax.json-api-1.0.jar'
jar_file=javax.json-api-1.0.jar
jars=./$(jar_file)


all: setup build javadocs run clean

setup:
	wget $(jar_url) -nc -O $(jar_file) || echo ""
build:
	javac $$(find ./*.java) -cp $(jars)
javadocs:
	javadoc *.java -cp $(jars) -d ./javadocs
run: *.class
	$$(for f in $(ls Unit*.class | cut -d'.' -f1); do java $(f); done)
clean:
	rm ./*.class || echo "Nothing to clean."
