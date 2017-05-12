jar_url='https://repo1.maven.org/maven2/org/glassfish/javax.json/1.0.4/javax.json-1.0.4.jar'
jar_file=$(shell echo $(jar_url)| rev | cut -d'/' -f 1 | rev)
jars=./$(jar_file)


all: setup clean build javadocs run

setup:
	wget $(jar_url) -nc -O $(jar_file) || echo ""
build:
	javac $$(find ./*.java) -cp $(jars)
javadocs:
	javadoc *.java -cp $(jars) -d ./javadocs
run:
	for file in $$(find *.class | cut -d'.' -f1);\
	do\
	    echo "====== RUNNING $$file.class ======";\
	    java -classpath .:$(jars) "$$file";\
	    echo "";\
	done
clean:
	rm ./*.class || echo "Nothing to clean."
