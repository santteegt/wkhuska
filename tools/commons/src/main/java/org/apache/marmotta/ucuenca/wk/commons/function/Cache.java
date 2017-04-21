/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.marmotta.ucuenca.wk.commons.function;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.List;
import java.util.concurrent.TimeUnit;
//import java.util.logging.Level;
//import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerCompressionWrapper;
import org.mapdb.serializer.SerializerArrayTuple;

/**
 *
 * @author Jose Luis Cullcay
 */
public final class Cache {

    private DB db = null;
    private DB dbDist = null;
    private HTreeMap<String, String> create = null;
    private BTreeMap<Object[], Double> distance = null;
    private double limit = 0.001;
    //private List<String> BlackList = new ArrayList();
    //private JsonObject config = null;
    
    //private static final Cache INSTANCE = new Cache();

    private Cache() {
        InputStream resourceAsStream = this.getClass().getResourceAsStream("/config.cnf");
        String theString = null;
        try {
            theString = IOUtils.toString(resourceAsStream, Charset.defaultCharset().toString());
        } catch (IOException ex) {
            //Logger.getLogger(Cache.class.getName()).log(Level.SEVERE, null, ex);
        }
        JsonObject config = JSON.parse(theString).getAsObject();
        
        
        db = DBMaker.fileDB(config.get("CacheFile").getAsString().value()).fileLockDisable().make();
        
        dbDist = DBMaker.fileDB(config.get("DistanceFile").getAsString().value()).fileLockDisable().make();
        
        create = db.hashMap("cache", Serializer.STRING, new SerializerCompressionWrapper(Serializer.STRING)).expireAfterCreate(365, TimeUnit.DAYS).createOrOpen();

        distance = dbDist.treeMap("distance")
                .keySerializer(new SerializerArrayTuple(Serializer.STRING, Serializer.STRING))
                .valueSerializer(Serializer.DOUBLE)
                .createOrOpen();
        
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                try {
                    //System.out.println("Killing ");
                    kill();
                } catch (SQLException ex) {
                    //Logger.getLogger(Cache.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        ));
    }

    public void put(String key, String value) {
        create.put(key, value);
        if (Math.random() < limit) {
            db.commit();
        }
    }

    public void kill() throws SQLException {
        if (db != null) {
            db.close();
        }
        if (dbDist != null) {
            dbDist.close();
        }
        create.close();
        distance.close();
    }

    public String get(String key) {
        return create.get(key);
    }

    public Double getDistance(String word1, String word2) {
        
        Double dist = distance.get(new Object[]{word1, word2});
        
        return (dist != null) ? dist : distance.get(new Object[]{word2, word1});
    }
    
    public void putDistance(String word1, String word2, Double dist) {
        distance.put(new Object[]{word1, word2}, dist);
        if (Math.random() < limit) {
            dbDist.commit();
        }
    }  

    public static Cache getInstance() {
        return new Cache();
    }

    public String getMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            String hashtext = number.toString(16);
            // Now we need to zero pad it if you actually want the full 32 chars.
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
