file='remotecontent?filepath=javax%2Fjson%2Fjavax.json-api%2F1.0%2Fjavax.json-api-1.0.jar'
name='javax.json-api-1.0.jar'
wget http://search.maven.org/remotecontent?filepath=javax/json/javax.json-api/1.0/javax.json-api-1.0.jar
mv $file $name
echo "JAR files: " $(ls | grep .jar)
