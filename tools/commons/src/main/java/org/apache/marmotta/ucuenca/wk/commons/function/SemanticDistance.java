/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.marmotta.ucuenca.wk.commons.function;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.PreparedStatement;
import java.sql.SQLException;
//import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

//import org.apache.marmotta.ucuenca.wk.commons.impl.CommonsServicesImpl;
//import org.apache.marmotta.ucuenca.wk.commons.service.CommonsServices;

/**
 *
 * @author bibliodigital
 */
public class SemanticDistance {

    // JDBC driver name and database URL
    private String dburl = "";
    private final TranslateForSemanticDistance trans = new TranslateForSemanticDistance();
    //  Database credentials
    //private String user = "";
    //private String pass = "";
    //private CommonsServices commonservices = new CommonsServicesImpl();

    //private Connection conn = null;
    //Statement stmt = null;

    public SemanticDistance() throws IOException, ClassNotFoundException {
        JsonObject config = null;
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

    /**
     * @param args the command line arguments
     */
    public synchronized double semanticKeywordsDistance(List<String> a, List<String> b) throws ClassNotFoundException, SQLException, IOException {
        //Class.forName("org.postgresql.Driver");
        //conn = DriverManager.getConnection(dburl, user, pass);
        ConcurrentHashMap<String, List<String>> map = new ConcurrentHashMap<>();
        List<String> authors = new ArrayList();
        authors.add("a1");
        authors.add("a2");
        ConcurrentHashMap<String, Double> result = new ConcurrentHashMap<>();
        double avg = 0;
        double har = 0;
        for (int i = 0; i < authors.size(); i++) {
            for (int j = i + 1; j < authors.size(); j++) {
                String a1 = authors.get(i);
                String a2 = authors.get(j);
                List<String> ka1 = null;
                List<String> ka2 = null;
                if (map.containsKey(a1)) {
                    ka1 = map.get(a1);
                } else {
                    ka1 = formatList(a);//consultado2R(a1, Endpoints.get(i));
                    map.put(a1, ka1);
                }
                if (map.containsKey(a2)) {
                    ka2 = map.get(a2);
                } else {
                    ka2 = formatList(b);//consultado2R(a2, Endpoints.get(j));
                    map.put(a2, ka2);
                }
                double sum = 0;
                double num = 0;
                
                for (String t1 : ka1) {
                    for (String t2 : ka2) {
                        num++;
                        String tt1 = t1;
                        String tt2 = t2;
                        double v = ngd(tt1, tt2);
                        sum += v;
                        }
                    }
                double prom = sum / num;
                if (num == 0 && sum == 0) {
                    prom = 2;
                }
                result.put(i + "," + j, prom);

                avg = avg(avg, prom);
                har = har(har, prom);
            }
        }

        //conn.close();
        return mapEntry(result);
    }
    
    /**
     * @param args the command line arguments
     */
    public synchronized double nwdDistance(List<String> a, List<String> b) throws ClassNotFoundException, SQLException, IOException {
        //Class.forName("org.postgresql.Driver");
        //conn = DriverManager.getConnection(dburl, user, pass);
        ConcurrentHashMap<String, List<String>> map = new ConcurrentHashMap<>();
        List<String> authors = new ArrayList();
        authors.add("a1");
        authors.add("a2");
        ConcurrentHashMap<String, Double> result = new ConcurrentHashMap<>();
        double avg = 0;
        double har = 0;
        for (int i = 0; i < authors.size(); i++) {
            for (int j = i + 1; j < authors.size(); j++) {
                String a1 = authors.get(i);
                String a2 = authors.get(j);
                List<String> ka1 = null;
                List<String> ka2 = null;
                if (map.containsKey(a1)) {
                    ka1 = map.get(a1);
                } else {
                    ka1 = formatList(a, a.size());//consultado2R(a1, Endpoints.get(i));
                    map.put(a1, ka1);
                }
                if (map.containsKey(a2)) {
                    ka2 = map.get(a2);
                } else {
                    ka2 = formatList(b, b.size());//consultado2R(a2, Endpoints.get(j));
                    map.put(a2, ka2);
                }
                double sum = 0;
                double num = 0;
                
	        double min;

                for (String t1 : ka1) {
                    min = 1.2;
                    for (String t2 : ka2) {
                        
                        String tt1 = t1;
                        String tt2 = t2;
                        double v = ngd(tt1, tt2);
                        if (v < min) {
                            min = v;
                        }
                    }
                    sum += min;		
	            num++;
                }
                double prom = sum / num;
                if (num == 0 && sum == 0) {
                    prom = 2;
                }
                result.put(i + "," + j, prom);

                avg = avg(avg, prom);
                har = har(har, prom);
            }
        }

        //conn.close();
        return mapEntry(result);
    }

    private double har(double har, double prom) {
        if (har == 0) {
            har = prom;
        } else {
            har = 2 / (1 / har + 1 / prom);
        }
        return har;
    }

    private double avg(double avg, double prom) {
        if (avg == 0) {
            avg = prom;
        } else {
            avg = (avg + prom) / 2;
        }

        return avg;
    }

    private double mapEntry(ConcurrentHashMap<String, Double> result) {
        double r = 0;
        for (Map.Entry<String, Double> cc : result.entrySet()) {
            r = cc.getValue();
        }
        return r;
    }

    private List<String> formatList(List<String> a) throws SQLException, IOException, ClassNotFoundException {
        //TranslateForSemanticDistance trans = new TranslateForSemanticDistance();
        a = trans.traductor(a);//new LinkedList<String>(java.util.Arrays.asList(t1_.split("\\s\\|\\s")));
        a = trans.clean(a);
            a = topT(a, (int) (2.0 * Math.log(a.size())));
        return a;
    }
    
    private List<String> formatList(List<String> a, Integer top) throws SQLException, IOException, ClassNotFoundException {
        //TranslateForSemanticDistance trans = new TranslateForSemanticDistance();
        a = trans.traductor(a);//new LinkedList<String>(java.util.Arrays.asList(t1_.split("\\s\\|\\s")));
        a = trans.clean(a);
        if (top == 0) {
            a = topT(a, (int) (2.0 * Math.log(a.size())));
        } else {
            a = topT(a, top);
        }
        return a;
    }

    private List<String> topT(List<String> m, int n) throws IOException, SQLException {
        int value1 = 1;
        n = (n <= 0) ? 1 : n;
        if (m.size() == value1) {
            m.add(m.get(0));
        }
        ConcurrentHashMap<String, Double> mapa = new ConcurrentHashMap();
        for (int i = 0; i < m.size(); i++) {
            for (int j = i + 1; j < m.size(); j++) {
                double v = ngd(m.get(i), m.get(j));
                //System.out.print(i+"/"+m.size()+"\t");

                if (mapa.containsKey(m.get(i))) {
                    mapa.put(m.get(i), mapa.get(m.get(i)) + v);
                } else {
                    mapa.put(m.get(i), v);
                }

                if (mapa.containsKey(m.get(j))) {
                    mapa.put(m.get(j), mapa.get(m.get(j)) + v);
                } else {
                    mapa.put(m.get(j), v);
                }
            }
        }
        Map<String, Double> sortByValue = sortByValue(mapa);
        List<String> ls = new ArrayList<>();
        ArrayList<String> arrayList = new ArrayList(sortByValue.keySet());
        for (int i = 0; i < n; i++) {
            if (i < sortByValue.size()) {
                ls.add(arrayList.get(i));
                // System.out.println(arrayList.get(i)+"__"+arrayList2.get(i));
            }
        }
        return ls;
    }

    private double ngd(String a, String b) throws IOException, SQLException {
        /*
        Statement stmt = conn.createStatement();
        String sql;
        sql = "SELECT * FROM distance where ((distance.\"firstWord\"='" + a + "' and distance.\"secondWord\"='" + b + "') or (distance.\"firstWord\"='" + b + "' and distance.\"secondWord\"='" + a + "'))";
        //log.info("Estado de la coneccion a la Base de datos http2: Cerrada:" + conn.isClosed() + " URL: " + dburl + "User: " + user + ". Pass: " + pass );
        java.sql.ResultSet rs;
                
        try {
            rs = stmt.executeQuery(sql);
            return rs.getDouble("coefficient");
        } catch (Exception ex) {
            rs = null;
            //log.error("Error while executing the sql in http2: " + sql + ". Message Translator: " + ex.getMessage() );
        }
        */
        
        Cache cache = Cache.getInstance();
        
        Double dist = cache.getDistance(a, b);
        
        if (dist == null) {
            int min = 0;
            int min2 = 1;
            a = a.trim();
            b = b.trim();

            if (a.compareToIgnoreCase(b) == min) {
                return 0;
            }

            //double n0 = getResultsCount(""+a+"");
            //double n1 = getResultsCount(""+b+"");
            //String c = ""+a+" "+b+"";
            double n0 = getResultsCount("\"" + a + "\"~10");
            double n1 = getResultsCount("\"" + b + "\"~10");
            String c = "\"" + a + " " + b + "\"~50";

            double n2 = getResultsCount(c);
            double m = 5029469;
            double distance = 0;
            int measure = 0;
            double l1 = Math.max(Math.log10(n0), Math.log10(n1)) - Math.log10(n2);
            double l2 = Math.log10(m) - Math.min(Math.log10(n0), Math.log10(n1));

            if (measure == min) {
                distance = l1 / l2;
            }
            if (measure == min2) {
                distance = 1 - (Math.log10(n2) / Math.log10(n0 + n1 - n2));
            }
            if (n0 == min || n1 == min || n2 == min) {
                distance = 1;
            }
            
            cache.putDistance(a, b, distance);
            cache.kill();
            return distance;
        } else {
            cache.kill();
            return dist;
        }
        /*
        try {
            PreparedStatement stmt2 = conn.prepareStatement("INSERT INTO distance (firstWord, secondWord, coefficient) values (?, ?, ?)");
            stmt2.setString(1, a);
            stmt2.setString(2, b);
            stmt2.setDouble(3, distance);
            stmt2.executeUpdate();
            stmt2.close();
        } catch (Exception e) {
            //log.error("Error in the http2 Insert Function. Used by TraductorYandex. Possibly the database." + e.getMessage());
        }
        */
    }

    private double getResultsCount(String query) throws IOException, SQLException {
        double c = 0;
        c = getResultsCount1(query);
        return c;
    }

    private double getResultsCount1(final String query) throws IOException, SQLException {

        String url = "https://en.wikipedia.org/w/api.php?action=query&list=search&format=json&srsearch=" + URLEncoder.encode(query, "UTF-8");
        double v = -1;
        do {
            try {
                String s = http(url);
                JsonParser parser = new JsonParser();
                JsonObject parse = parser.parse(s).getAsJsonObject();
                v = parse.get("query").getAsJsonObject().get("searchinfo").getAsJsonObject().get("totalhits").getAsNumber().doubleValue();
            } catch (Exception e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SemanticDistance.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } while (v == -1);
        return v;
    }

    private <K, V extends Comparable<? super V>> Map<K, V>
            sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list
                = new LinkedList<>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        ConcurrentHashMap<K, V> result = new ConcurrentHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private synchronized String http(String s) throws SQLException, IOException {

        //Statement stmt = conn.createStatement();
        //String sql;
        //sql = "SELECT * FROM cache where cache.key='" + commonservices.getMD5(s) + "'";
        //java.sql.ResultSet rs = stmt.executeQuery(sql);
        String resp = "";
        /*if (rs.next()) {
            resp = rs.getString("value");
            rs.close();
            stmt.close();
        } else {
            rs.close();
            stmt.close();*/
            final URL url = new URL(s);
            final URLConnection connection = url.openConnection();
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            connection.addRequestProperty("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:44.0) Gecko/20100101 Firefox/44.0");
            connection.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            final Scanner reader = new Scanner(connection.getInputStream(), "UTF-8");
            while (reader.hasNextLine()) {
                final String line = reader.nextLine();
                resp += line + "\n";
            }
            reader.close();

            /*try {
                JsonParser parser = new JsonParser();
                parser.parse(resp);
                PreparedStatement stmt2 = conn.prepareStatement("INSERT INTO cache (key, value) values (?, ?)");
                stmt2.setString(1, commonservices.getMD5(s));
                stmt2.setString(2, resp);
                stmt2.executeUpdate();
                stmt2.close();
            } catch (Exception e) {

            }
        }*/

        return resp;
    }

}
