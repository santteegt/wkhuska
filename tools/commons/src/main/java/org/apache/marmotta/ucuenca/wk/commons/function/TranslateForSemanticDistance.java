/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.marmotta.ucuenca.wk.commons.function;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author FernandoBac
 * @author Jose Cullcay
 */
public class TranslateForSemanticDistance {

    // JDBC driver name and database URL
    private String dburl = "";
    //  Database credentials
    //private String user = "";
    //private String pass = "";
    //private CommonsServices commonservices = new CommonsServicesImpl();

    //private Connection conn = null;
    //Statement stmt = null;

    private JsonObject config = null;
    
    //load all languages:
    private List<LanguageProfile> languageProfiles = new LanguageProfileReader().readAllBuiltIn();
    
    //build language detector:
    private LanguageDetector languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
                    .withProfiles(languageProfiles)
                    .build();
    
    //private Cache cache = Cache.getInstance();

    public TranslateForSemanticDistance() throws IOException, ClassNotFoundException {
        JsonParser parser = new JsonParser();
        InputStream resourceAsStream = this.getClass().getResourceAsStream("/config.cnf");
        //String readFile = readFile("./config.cnf", Charset.defaultCharset());
        String theString = IOUtils.toString(resourceAsStream, Charset.defaultCharset().toString());

        config = parser.parse(theString).getAsJsonObject();
        dburl = dburl + config.get("dbServer").getAsString() + "/" + config.get("dbSchema").getAsString();
        //user = config.get("dbUser").getAsString();
        //pass = config.get("dbPassword").getAsString();

    }

    public void close() throws SQLException {
        //conn.close();
    }

    public List<String> traductor(List<String> join) throws SQLException, IOException, ClassNotFoundException {
        //Class.forName("org.postgresql.Driver");
        //conn = DriverManager.getConnection(dburl, user, pass);
        Cache cache = new Cache();
        cache.getInstanceDB();
        //Logger.getLogger(TranslateForSemanticDistance.class.getName()).log(Level.INFO, "Estado de la coneccion a la Base de datos Traductor: Cerrada:" + (conn != null? conn.isClosed(): "null") + " URL: " + dburl + "User: " + user + ". Pass: " + pass );
        List<String> ls = new ArrayList();
        try {
            for (String w : join) {
                String translated = "";
                String language = detectLanguage(w);
                String english = "en";
                if (!english.equals(language)) {

                    /*Statement stmt = conn.createStatement();
                String sql;
                sql = "SELECT * FROM translation where translation.word='" + w.trim() + "'";
                java.sql.ResultSet rs;
                
                try {
                    rs = stmt.executeQuery(sql);
                } catch (SQLException ex) {
                    rs = null;
                    Logger.getLogger(TranslateForSemanticDistance.class.getName()).log(Level.SEVERE, "Error while executing the sql: " + sql + ". Message Translator: " + ex.getMessage() );
                }*/
                    translated = cache.get(w.toLowerCase());

                    if (translated == null) {
                        translated = traductorBing(w.trim()).trim().toLowerCase();
                        if (translated.equals(w.trim().toLowerCase())) {
                            translated = traductorYandex(w.trim()).toLowerCase();
                        }

                        cache.put(w.toLowerCase(), translated);

                    }

                    /*if (rs != null) {
                    if (rs.next()) {
                        translated = rs.getString("value");
                        rs.close();
                        stmt.close();
                    } else {
                        rs.close();
                        stmt.close();
                        translated = traductorYandex(w.trim());
                        if (translated.equals(w.trim())) {
                            translated = traductorBing(w.trim()).trim().toLowerCase();
                        }
                        try {
                            PreparedStatement stmt2 = conn.prepareStatement("INSERT INTO translation (word, value) values (?, ?)");
                            stmt2.setString(1, w.trim());
                            stmt2.setString(2, translated);
                            stmt2.executeUpdate();
                            stmt2.close();
                        } catch (SQLException ex) {
                            Logger.getLogger(SemanticDistance.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } else {
                    translated = traductorYandex(w.trim());
                    if (translated.equals(w.trim())) {
                        translated = traductorBing(w.trim()).trim().toLowerCase();
                    }
                }*/
                } else {
                    translated = w.trim().toLowerCase();
                }
                ls.add(translated.trim().toLowerCase());
            }
        } catch (Exception ex) {
            Logger.getLogger(TranslateForSemanticDistance.class.getName()).log(Level.SEVERE, "Error en traduccion.", ex);
        }
//        conn.close();
        cache.kill();
        return ls;
    }
    
    private String detectLanguage(String word) {
        String language = "es";

        //create a text object factory
        TextObjectFactory textObjectFactory = CommonTextObjectFactories.forDetectingShortCleanText();
        
        //Text to query:
        TextObject textObject;
        textObject = textObjectFactory.forText(word.toLowerCase());

        //Detect the language
        Optional<LdLocale> lang = languageDetector.detect(textObject);
        if (lang.isPresent()) {
            language = lang.get().getLanguage();
        }//languageDetector.detect(textObjectFactory.forText("bullying bullying bullying bullying bullying bullying bullying bullying bullying ")).get().getLanguage();
        //"es" para espanol y "en" para ingles.
        return language;

    }
    
    private String traductorYandex(String palabras) throws UnsupportedEncodingException, SQLException, IOException {
        String url = "https://translate.yandex.net/api/v1.5/tr.json/translate";
        //String url = "https://translate.yandex.net/api/v1.5/tr.json/translate?key=trnsl.1.1.20160321T160516Z.43cfb95e23a69315.6c0a2ae19f56388c134615f4740fbb1d400f15d3&lang=en&text=" + URLEncoder.encode(palabras, "UTF-8");
        ConcurrentHashMap<String, String> mp = new ConcurrentHashMap<>();
        mp.put("key", "trnsl.1.1.20160321T160516Z.43cfb95e23a69315.6c0a2ae19f56388c134615f4740fbb1d400f15d3");
        mp.put("lang", "en");
        mp.put("text", palabras);
        mp.put("options", "1");
        boolean c = true;
        int i = 0;
        int maxqueries = 10;
        do {
            try {
                i++;
                if (i == maxqueries) {
                    c = false;
                }
                String http = http2(url, mp);
                String res = http;
                JsonParser parser = new JsonParser();
                JsonObject parse = parser.parse(res).getAsJsonObject();
                JsonArray asArray = parse.get("text").getAsJsonArray();
                res = asArray.get(0).getAsString();
                palabras = res;
                c = false;
            } catch (Exception e) {
                e.printStackTrace(new PrintStream(System.out));

                try {
                    Thread.sleep(400);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SemanticDistance.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            //System.out.println("In2");
        } while (c);
        return palabras;

    }

    private String traductorBing(String palabras) {
        //boolean falsevalue = false;
        double randomNum = Math.random();
        double prob = 0.2;
        if (randomNum < prob) {
            Translate.setClientId("fedquest");
            Translate.setClientSecret("ohCuvdnTlx8Sac4r7gfqyHy0xOJJpKK9duFC4tn9Sho=");
        } else {
            Translate.setClientId("karyabad");
            Translate.setClientSecret("viz4JYZAD8samvwuoV6gJ5MczDig8cBHyP0NnY1gRF0=");
        }

        String translatedText;
        try {
            translatedText = Translate.execute(palabras, Language.ENGLISH);

            return translatedText;
        } catch (Exception ex) {

            try {
                String[] ls = palabras.split("\\s\\|\\s");
                int chunk = ls.length / 2; // chunk size to divide
                String pal = "";
                for (int i = 0; i < ls.length; i += chunk) {
                    String[] pr = java.util.Arrays.copyOfRange(ls, i, i + chunk);
                    pr = clean2(pr);
                    String u = Joiner.on(" | ").join(pr);
                    u = traductorBing(u);
                    pal += u + " ";

                }
                return pal;

            } catch (Exception exx) {
                exx.printStackTrace(new PrintStream(System.out));
            }
        }
        return palabras;
    }

    private synchronized String http2(String s, Map<String, String> mp) throws SQLException, IOException {
        /*String md = s + mp.toString();
        Statement stmt = conn.createStatement();
        String sql;
        sql = "SELECT * FROM cache where cache.key='" + commonservices.getMD5(md) + "'";
        Logger.getLogger(TranslateForSemanticDistance.class.getName()).log(Level.INFO, "Estado de la coneccion a la Base de datos http2: Cerrada:" + conn.isClosed() + " URL: " + dburl + "User: " + user + ". Pass: " + pass );
        java.sql.ResultSet rs;
                
        try {
            rs = stmt.executeQuery(sql);
        } catch (SQLException ex) {
            rs = null;
            Logger.getLogger(TranslateForSemanticDistance.class.getName()).log(Level.SEVERE, "Error while executing the sql in http2: " + sql + ". Message Translator: " + ex.getMessage() );
            
        }*/
        
        String resp = "";
        /*if (rs!= null &&rs.next()) {
            resp = rs.getString("value");
            rs.close();
            stmt.close();
        } else {
            rs.close();
            stmt.close();*/

            HttpClient client = new HttpClient();
            PostMethod method = new PostMethod(s);

            //Add any parameter if u want to send it with Post req.
            for (Map.Entry<String, String> mcc : mp.entrySet()) {
                method.addParameter(mcc.getKey(), mcc.getValue());
            }
            int statusCode = client.executeMethod(method);

            if (statusCode != -1) {
                InputStream in = method.getResponseBodyAsStream();
                final Scanner reader = new Scanner(in, "UTF-8");
                while (reader.hasNextLine()) {
                    final String line = reader.nextLine();
                    resp += line + "\n";
                }
                reader.close();
                /*try {
                    JsonParser parser = new JsonParser();
                    parser.parse(resp);
                    PreparedStatement stmt2 = conn.prepareStatement("INSERT INTO cache (key, value) values (?, ?)");
                    stmt2.setString(1, commonservices.getMD5(md));
                    stmt2.setString(2, resp);
                    stmt2.executeUpdate();
                    stmt2.close();
                } catch (Exception e) {
                    Logger.getLogger(TranslateForSemanticDistance.class.getName()).log(Level.SEVERE, "Error in the http2 Insert Function. Used by TraductorYandex. Possibly the database." + e.getMessage() );
                }*/
            }
        //}

        return resp;
    }

    private String[] clean2(final String... v) {
        List<String> list = new ArrayList<String>(java.util.Arrays.asList(v));
        list.removeAll(Collections.singleton(null));
        return list.toArray(new String[list.size()]);
    }

    public List<String> clean(List<String> ls) {
        List<String> al = ls;
        Set<String> hs = new HashSet<>();
        hs.addAll(al);
        al.clear();
        al.addAll(hs);
        JsonArray asArray = config.get("stopwords").getAsJsonArray();
        for (JsonElement s : asArray) {
            al.remove(s.getAsString());
        }

        return al;
    }

}
