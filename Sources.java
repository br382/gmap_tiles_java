import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.HashMap;
//External .jar source: javax.json.*
//https://repo1.maven.org/maven2/org/glassfish/javax.json/1.0.4/javax.json-1.0.4.jar
import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.JsonStructure;
import javax.json.JsonObject;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

/**
* This interface class uses a filename.json with the structure of:
* {
*     "sources": {
*         "939393": {
*             "name":    "Test Name",
*             "type":    "tester",
*             "notes":   "This entry is just plain text notes.",
*             "ext":     "jpg",
*             "prefix":  "https://domain.root.com/path/interface&lang=en",
*             "x":       "&x=",
*             "y":       "&y=",
*             "zoom":    "&z=",
*             "postfix": "&metadata=null"
*          },
*         "787998" : {...}
*     },
*     "un-used crap" : {...}
* }
*/

class Sources {
    public static void saveJson(String filename, JsonObject struct) {
        String output = ppjson( struct );
        File       fd = new File( filename );
        try {
            FileWriter fd_wr = new FileWriter( fd );
            fd_wr.write( output );
            fd_wr.flush();
            fd_wr.close();
        } catch (IOException e) {
            System.err.println("ERR -- Sources.saveJson -- Unable to save JSON ouput: " + e);
        }
    }
    
    /**
    * Sources::openJson(String input) { return JsonObject; }
    * <p>
    * Notes:<p>
    *   This family of functions converts a JSON filename, or string into a JSonObject.<p>
    */
    public static JsonObject openJson(String input) {
        File fd = new File( input );
        JsonReader json_reader;
        if (fd.exists()&&fd.isFile()) {
            try {
                json_reader = Json.createReader( new FileReader( input ));
            } catch (IOException e) {
                System.err.println("ERR -- Sources.openJson -- Unable to open file: " + e);
                json_reader = Json.createReader(new StringReader(input ));
            }
        } else {
            json_reader = Json.createReader(new StringReader(input ));
        }
        return json_reader.readObject();
    }
    
    /**
    * Sources::ppjson([String,JsonObject] input) { return String json.prettyprint(); }
    * <p>
    * Notes:<p>
    *    Accepts JSON filename, string, or Object in this family of functions.
    */
    public static String ppjson(String input) {
        return ppjson( openJson( input ) );
    }
    public static String ppjson(JsonObject input) {
        Map<String, Boolean> json_config  = new HashMap<>();
                             json_config.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory    json_factory = Json.createWriterFactory( json_config );
        StringWriter         json_strwr   = new StringWriter();
        JsonWriter           json_writer  = json_factory.createWriter( json_strwr );
                             json_writer.writeObject( input );
                             json_writer.close();
        return(              json_strwr.toString() );
    }
    
    public static int hashJson(JsonObject json) {
        return ppjson( json ).hashCode();
    }
    public static int hashJson(String json) {
        return ppjson( json ).hashCode();
    }

    public static void addSource(String filename, String type, String name, String prefix, String postfix, String x, String y, String zoom, String ext, String notes) {
        JsonObject src = Json.createObjectBuilder().build();
        return;
    }
    
    public static JsonObject searchSource(String filename, JsonObject search) {
        return Json.createObjectBuilder().build();
    }
    
    public static boolean rmSource(String filename, int uid) {
        return false;
    }

    public static void main(String[] args) {
        String fname = "sources.json";
        System.out.println( "Hash of " + fname + ": " + hashJson( openJson(fname) ) );
        System.out.println( ppjson( fname ) );
        addSource(fname, "Satellite", "Test Source", "www.google.com/", "&fetch=True", "&x=", "&y=", "&z=", ".jpg", "Test source notes.");
        System.out.println( ppjson( fname ) );
        rmSource(fname,9090);
    }
}
