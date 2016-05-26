package org.apache.marmotta.ucuenca.wk.provider.ma.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrdf.model.Model;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JSONtoRDF {

    private String resource;
    private JsonArray data;
    private Model model;
    private Map<String, String> schema;
    private final ValueFactory factory = ValueFactoryImpl.getInstance();
    private String authorNamespace="http://academic.research.microsoft.com/Author/";

    public JSONtoRDF(String resource, Map<String, String> schema, JsonArray data, Model model) {
        this.resource = resource;
        this.schema = schema;
        this.data = data;
        this.model = model;
    }

    public void buldHead() {
        model.add(factory.createStatement(factory.createURI(resource),
                RDF.TYPE, factory.createURI(schema.get("entity::type")))
        );

        int i = 0;

        while (i < data.size()) {
            JsonObject json = data.get(i).getAsJsonObject();

            model.add(factory.createStatement(factory.createURI(resource),
                    factory.createURI("http://xmlns.com/foaf/0.1/publications"), factory.createURI("http://academic.research.microsoft.com/Publication/" + json.get("id").getAsString() + "/")));
            i++;
        }

    }

    public void parse() throws Exception {
        Integer i = 0;
        buldHead();
        while (i < data.size()) {
            JsonObject json = data.get(i).getAsJsonObject();
            this.mappingProcess("http://academic.research.microsoft.com/Publication/" + json.get("id").getAsString() + "/", json);
            i++;
        }
        /*
         PrintWriter out = new PrintWriter("/home/santteegt/JSON" + this.hashCode() + ".ttl");
         out.print(data.toString());
         out.close();*/
    }

    private void mappingProcess(String resource, JsonObject json) {
        for (String key : schema.keySet()) {
            if (key.matches("^entity::type$")) {
                model.add(factory.createStatement(factory.createURI(resource),
                        RDF.TYPE, factory.createURI(schema.get("entity::type"))));
                continue;
            }
            Matcher m = Pattern.compile("^(entity::property:)(.*)$").matcher(key);
            if (m.find()) {

                getGenericAttributes(m, key, json, resource);
            }
            getAttributesPartOne(key, json, resource);
            getAttributesPartTwo(key, json, resource);

        }

    }

    public void getAttributesPartOne(String key, JsonObject json, String resource) {

        switch (key) {
            case "entity::property:abstract":
                String value = json.get("abstractt").getAsString();
                model.add(factory.createStatement(factory.createURI(resource),
                        factory.createURI(schema.get(key)), factory.createLiteral(value)));
                break;
            case "entity::property:fullversionurl":
                JsonArray aux = json.get("fullVersionURL").getAsJsonArray();
                for (JsonElement version : aux) {
                    model.add(factory.createStatement(factory.createURI(resource),
                            factory.createURI(schema.get(key)), factory.createURI(version.getAsString())));

                }
                break;
            case "entity::property:date":
                value = json.get("year").getAsString();
                model.add(factory.createStatement(factory.createURI(resource),
                        factory.createURI(schema.get(key)), factory.createLiteral(value)));
                break;
            default:
                break;

        }
    }

    public void getAttributesPartTwo(String key, JsonObject json, String resource) {

        switch (key) {
            case "entity::property:uri":
                model.add(factory.createStatement(factory.createURI(resource),
                        factory.createURI(schema.get(key)), factory.createURI("http://academic.research.microsoft.com/Publication/" + json.get("id").getAsString() + "/")));
                break;
            case "entity::property:quote":
                JsonArray aux = json.get("keyWord").getAsJsonArray();
                for (JsonElement version : aux) {
                    model.add(factory.createStatement(factory.createURI(resource),
                            factory.createURI(schema.get(key)), factory.createLiteral(version.getAsString())));

                }
                break;
            case "entity::property:authorlist":
                aux = json.get("authors").getAsJsonArray();
                String authors = "";
                for (JsonElement version : aux) {
                    authors += "<" + ((authorNamespace + version.getAsJsonObject().get("id").getAsString() + "/").trim()) + "> ";

                }

                model.add(factory.createStatement(factory.createURI(resource),
                        factory.createURI(schema.get(key)), factory.createLiteral("(" + authors + ")")));
                break;
            case "entity::property:creator":
                aux = json.get("authors").getAsJsonArray();
                for (JsonElement version : aux) {
                    model.add(factory.createStatement(factory.createURI(resource),
                            factory.createURI(schema.get(key)), factory.createURI(authorNamespace + version.getAsJsonObject().get("id").getAsString() + "/")));
                    model.add(factory.createStatement(factory.createURI(authorNamespace + version.getAsJsonObject().get("id").getAsString() + "/"),
                            factory.createURI(schema.get("entity::property:fname")), factory.createLiteral(version.getAsJsonObject().get("firstName").getAsString())));
                    model.add(factory.createStatement(factory.createURI(authorNamespace + version.getAsJsonObject().get("id").getAsString() + "/"),
                            factory.createURI(schema.get("entity::property:lname")), factory.createLiteral(version.getAsJsonObject().get("lastName").getAsString())));
                      model.add(factory.createStatement(factory.createURI(authorNamespace + version.getAsJsonObject().get("id").getAsString() + "/"),
                            factory.createURI(schema.get("entity::property:mname")), factory.createLiteral(version.getAsJsonObject().get("middleName").getAsString())));
                }

                break;
            default:
                break;

        }

    }

    public void getGenericAttributes(Matcher m, String key, JsonObject json, String resource) {
        if (json.has(m.group(2))) {
            String value = json.get(m.group(2)).getAsString();

            model.add(factory.createStatement(factory.createURI(resource),
                    factory.createURI(schema.get(key)), factory.createLiteral(value)));
        }
    }
}
