/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.marmotta.ucuenca.wk.commons.service;

/**
 *
 * @author Satellite
 */
public interface ConstantService {

    String PUBPROPERTY = "http://xmlns.com/foaf/0.1/publications";

    String TITLEPROPERTY = "http://purl.org/dc/terms/title";

    String INSERTDATA = "INSERT DATA { ";

    String ENDPOINTPREFIX = "http://ucuenca.edu.ec/wkhuska/endpoint/";

    String UCPREFIX = "http://ucuenca.edu.ec/ontology#";

    String PREFIX = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
            + " PREFIX foaf: <http://xmlns.com/foaf/0.1/>"
            + " PREFIX owl: <http://www.w3.org/2002/07/owl#> "
            + " PREFIX dct: <http://purl.org/dc/terms/> "
            + " PREFIX mm: <http://marmotta.apache.org/vocabulary/sparql-functions#> "
            + " PREFIX uc: <http://ucuenca.edu.ec/ontology#> "
            + " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
            + " PREFIX bibo: <http://purl.org/ontology/bibo/> ";

    String getPrefixes();
    
    String getPubProperty();

    String getTittleProperty();

    String getGraphString(String graph);

    String getWkhuskaGraph();
    
    String getEndpointsGraph();

    String getProvenanceProperty();

    String getLimit(String limit);

    String getOffset(String offset);

    String uc(String pred);

    String foaf(String pred);

    String owl(String pred);

    String dblp(String pred);

    String getDBLPGraph();

    String getExternalAuthorsGraph();
    
    String getScopusGraph();

    String getMAGraph();

    String getGSGraph();
    
    String getClusterGraph();
    
    String getLogoPath();
    
    String getAuthorsGraph();

    String getProviderNsGraph();
}
