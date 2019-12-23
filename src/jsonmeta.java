package jsonparse;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;

import com.esotericsoftware.kryo.util.IdentityMap.Entry;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

class Attribute {

    private String name;
    private String datatype;
    private String format;
    private String diType;
    private Integer length;
    private String qualifiedName;
    private Integer nestedLevel;
    private Integer dimension;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getDiType() {
        return diType;
    }

    public void setDiType(String diType) {
        this.diType = diType;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    public Integer getNestedLevel() {
        return nestedLevel;
    }

    public void setNestedLevel(Integer nestedLevel) {
        this.nestedLevel = nestedLevel;
    }

    public Integer getDimension() {
        return dimension;
    }

    public void setDimension(Integer dimension) {
        this.dimension = dimension;
    }

}


class Table {

    private List<Attribute> attributes = null;

    public List<Attribute> getAttributes() {
        if (attributes == null) {
            attributes = new ArrayList<>();
        }
        return attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }
}


public class jsonmeta {
    
    private static Table table = new Table();
    private static List<Map<String, String>> jlist = new ArrayList<>();
    private static Map<String, String> jmap = new HashMap<>();
    
    public static String primitiveDatatype(JsonPrimitive p) {
        String datatype = "null";
        if (p.isString()) {
            datatype = "string";
        } else if (p.isNumber()) {
            datatype = "number";
        } else if (p.isBoolean()) {
            datatype = "boolean";
        }
        return datatype;
    }
    
    public static void parse(JsonObject obj, String qname) {
        String prefix = qname != "" ? qname + "." : "";
        for (Map.Entry<String, JsonElement> k : obj.entrySet()) {
            if (k.getValue().isJsonPrimitive()) {
                Attribute attr = new Attribute();
                table.getAttributes().add(attr);
                attr.setName(prefix + k.getKey());
                JsonPrimitive p = k.getValue().getAsJsonPrimitive();
                attr.setDatatype(primitiveDatatype(p));
                Map<String, String> val = new HashMap<String, String>() {{
                    put(prefix + k.getKey(), k.getValue().toString());
                }};
                jlist.get(jlist.size()-1).put(prefix + k.getKey(), k.getValue().toString());
                //jlist.get(jlist.size()-1).add(val);
                System.out.println(prefix + k.getKey() + ":" + k.getValue() + ", type : " + primitiveDatatype(p));
            }
            if (k.getValue().isJsonObject()) {
                Attribute attr = new Attribute();
                table.getAttributes().add(attr);
                attr.setName(prefix + k.getKey());
                attr.setDatatype("struct");
                System.out.println(prefix + k.getKey() + ":" + k.getValue() + ", type : struct");
                parse(k.getValue().getAsJsonObject(), prefix + k.getKey());
            }
            if (k.getValue().isJsonArray()) {
                Attribute attr = new Attribute();
                table.getAttributes().add(attr);
                attr.setName(prefix + k.getKey());
                attr.setDatatype("array");
                System.out.println(prefix + k.getKey() + ":" + k.getValue() + ", type : array");
                parse(k.getValue().getAsJsonArray(), prefix + k.getKey());
            }
        }
    }

    public static void parse(JsonArray obj, String qname) {
        int count = 0;
        for (JsonElement k : obj) {
            if (k.isJsonPrimitive()) {
                count++;
                Attribute attr = new Attribute();
                String element = qname + ".__element" + count;
                attr.setName(element);
                table.getAttributes().add(attr);
                attr.setNestedLevel(count);
                JsonPrimitive p = k.getAsJsonPrimitive();
                attr.setDatatype(primitiveDatatype(p));
                Map<String, String> val = new HashMap<String, String>() {{
                    put(element, p.toString());
                }};
                jlist.get(jlist.size()-1).put(element, p.toString());
                System.out.println(element + ":" + p + ", type: " + primitiveDatatype(p));
            }
            if (k.isJsonObject()) {
                parse(k.getAsJsonObject(), qname);
            }
            if (k.isJsonArray() && k.getAsJsonArray().size() > 0) {
                parse(k.getAsJsonArray(), qname);
            }
        }
    }

    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        //new JsonReader(new FileReader("C:\\tmp\\json_rec.json"));
        JsonReader jsonReader = new JsonReader(new InputStreamReader(
                new FileInputStream("C:\\tmp\\example.json"), "UTF8"));
        jsonReader.setLenient(true);
        JsonParser parser = new JsonParser();
        JsonElement jsonTree = null;
        try {
            do {
                jsonTree = parser.parse(jsonReader);
                System.out.println(jsonTree);
                System.out.println("JsonObject: " + jsonTree.isJsonObject());
                System.out.println("JsonArray: " + jsonTree.isJsonArray());
                Map<String, String> m = new HashMap<>();
                jlist.add(m);
                if (jsonTree.isJsonArray()) {
                    parse(jsonTree.getAsJsonArray(), "");
                } else {
                    parse(jsonTree.getAsJsonObject(), "");
                }
            } while (jsonReader.peek() != JsonToken.END_DOCUMENT);
        } catch (JsonIOException | JsonSyntaxException | IOException e) {           
            e.printStackTrace();
        }

       Gson gson = new Gson();
       for (Attribute attr : table.getAttributes()) {
           System.out.println(gson.toJson(attr));
       }
       Set<String> columns = new LinkedHashSet<String>();
       for (Map<String, String> e : jlist) {
           columns.addAll(e.keySet());
       }
       String header = StringUtils.join(columns.toArray(), ",");
       System.out.println(header);
       List<String> values = new ArrayList<>();
       for (Map<String, String> m : jlist) {
           String list = StringUtils.join(m.values().toArray(), "");
           values.add(list);
       }
       System.out.println(values);
    }
}
