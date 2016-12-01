/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.marmotta.ucuenca.wk.authors.api;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.marmotta.platform.core.test.base.EmbeddedMarmotta;
import org.apache.marmotta.ucuenca.wk.authors.services.EndpointServiceImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Satellite
 */
public class EndpointServiceTest {
    private static EmbeddedMarmotta marmotta;
    private static EndpointService myService;
    public EndpointServiceTest() {
    }
    
    @BeforeClass
    public static void setUp() {
        marmotta = new EmbeddedMarmotta();
        myService = marmotta.getService(EndpointService.class);
    }

    @AfterClass
    public static void tearDownClass() {
        marmotta.shutdown();
    }
    
    
    @After
    public void tearDown() {
    }

    /**
     * Test of addEndpoint method, of class EndpointService.
     */
    @Test
    public void testAddEndpoint() {
        System.out.println("addEndpoint");
        String state = "true";
        String name = "PRUEBA";
        String endpointUrl = "http://example.ec/sparql";
        String graphUri = "http://example.ec/data";
        String fullName = "UNIVERSIDAD DE PRUEBA";
        String engName = "TEST UNIVERSITY";
        String city = "CIUDAD PRUEBA";
        String province = "PROVINCIA PRUEBA";
        String latitude = "0.1000 S";
        String longitude = "4.1245 W";
        EndpointService instance = new EndpointServiceImpl();
        String expResult = "Endpoint Insertado Correctamente";
        String result = myService.addEndpoint(state, name, endpointUrl, graphUri, fullName, engName, city, province, latitude, longitude);
        //String result = instance.addEndpoint(name, endpointUrl, graphUri);
        Assert.assertEquals(expResult, result);
    }

    /**
     * Test of getEndpoint method, of class EndpointService.
     */
    @Test
    public void testGetEndpoint() {
    }

    
     /**
     * Function that return a Hash Code to get a unique ID for resource URI
     * according name, url and graph. example:
     * http://localhost:8080/endpoint/21552MED5454AFDCSS55422354F54D5SX9VRSSDB
     *
     * @param name
     * @param url
     * @param graph
     * @return
     * @throws NoSuchAlgorithmException
     */
    public String getHashCode(String name, String url, String graph) throws NoSuchAlgorithmException {
        String plaintext = name + url + graph;
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.reset();
        m.update(plaintext.getBytes());
        byte[] digest = m.digest();
        BigInteger bigInt = new BigInteger(1, digest);
        return bigInt.toString(16);
    }
    
    
    /**
     * Test of listEndpoints method, of class EndpointService.
     */
    @Test
    public void testListEndpoints() {
    }

    /**
     * Test of removeEndpoint method, of class EndpointService.
     */
    @Test
    public void testRemoveEndpoint() {
        System.out.println("removeEndpoint");
         try {
            System.out.println("getEndpoint");
            String name = "PRUEBA";
            String endpointUrl = "http://example.ec/sparql";
            String graphUri = "http://example.ec/data";
            String resourceHash = getHashCode(name, endpointUrl, graphUri);
            String resourceId = "http://localhost:8080/endpoint/"+resourceHash;
            EndpointService instance = new EndpointServiceImpl();
            String expResult = "Endpoint was DELETE";
            String result = myService.removeEndpoint(resourceId);
            //String result = instance.removeEndpoint(resourceId);
            assertEquals(expResult, result);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(EndpointServiceTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        
        
        
    }

    
}
