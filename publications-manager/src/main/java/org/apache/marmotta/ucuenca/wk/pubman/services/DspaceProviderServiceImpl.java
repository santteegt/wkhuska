/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.marmotta.ucuenca.wk.pubman.services;

import com.google.gson.JsonArray;
import java.io.BufferedReader;
import java.io.FileReader;
//import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.marmotta.commons.sesame.model.ModelCommons;
import org.apache.marmotta.commons.vocabulary.FOAF;
import org.apache.marmotta.ldclient.api.ldclient.LDClientService;
import org.apache.marmotta.ldclient.endpoint.rdf.SPARQLEndpoint;
import org.apache.marmotta.ldclient.exception.DataRetrievalException;
import org.apache.marmotta.ldclient.model.ClientConfiguration;
import org.apache.marmotta.ldclient.model.ClientResponse;
import org.apache.marmotta.ldclient.services.ldclient.LDClient;
import org.apache.marmotta.platform.core.exception.MarmottaException;
import org.apache.marmotta.platform.sparql.api.sparql.SparqlService;
import org.apache.marmotta.ucuenca.wk.commons.impl.ConstantServiceImpl;
import org.apache.marmotta.ucuenca.wk.commons.service.DistanceService;
import org.apache.marmotta.ucuenca.wk.commons.service.ConstantService;
import org.apache.marmotta.ucuenca.wk.commons.service.QueriesService;
import org.apache.marmotta.ucuenca.wk.commons.service.CommonsServices;
import org.apache.marmotta.ucuenca.wk.commons.service.KeywordsService;
import org.apache.marmotta.ucuenca.wk.commons.service.GetAuthorsGraphData;

import org.apache.marmotta.ucuenca.wk.authors.api.EndpointService;
import org.apache.marmotta.ucuenca.wk.authors.api.SparqlEndpoint;

import org.apache.marmotta.ucuenca.wk.pubman.api.SparqlFunctionsService;

import org.apache.marmotta.ucuenca.wk.pubman.exceptions.PubException;
import org.apache.marmotta.ucuenca.wk.pubman.api.DspaceProviderService;
//import org.openrdf.model.Model;
//import org.openrdf.model.Statement;

import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;

import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;

import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.repository.Repository;
import org.openrdf.repository.sparql.SPARQLRepository;
//import org.openrdf.rio.RDFFormat;
//import org.openrdf.rio.RDFHandlerException;
//import org.openrdf.rio.RDFWriter;
//import org.openrdf.rio.Rio;
import org.semarglproject.vocab.OWL;

/**
 * Default Implementation of {@link PubVocabService}
 *
 * @author Jose Luis Cullcay
 */
@ApplicationScoped
public class DspaceProviderServiceImpl implements DspaceProviderService, Runnable {

    @Inject
    private Logger log;

    @Inject
    private QueriesService queriesService;

    @Inject
    private ConstantService constantService;

    @Inject
    private CommonsServices commonsServices;

    @Inject
    private GetAuthorsGraphData getauthorsData;

    @Inject
    private DistanceService distance;

    @Inject
    private SparqlFunctionsService sparqlFunctionsService;
    
    @Inject
    private EndpointService authorsendpointService;

    List<Map<String, Value>> uniNames = new ArrayList<>(); //nombre de universidades
    
    @Inject
    private SparqlService sparqlService;


    @Override
    public String runPublicationsProviderTaskImpl(String param) {
        
        try {
            String allAuthorsQuery = queriesService.getAuthors();
                    /*"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX foaf: <http://xmlns.com/foaf/0.1/>  PREFIX owl: <http://www.w3.org/2002/07/owl#>  PREFIX dct: <http://purl.org/dc/terms/>  PREFIX mm: <http://marmotta.apache.org/vocabulary/sparql-functions#>  PREFIX dcat: <http://www.w3.org/ns/dcat#>  PREFIX bibo: <http://purl.org/ontology/bibo/> PREFIX dc: <http://purl.org/dc/elements/1.1/> prefix uc: <http://ucuenca.edu.ec/ontology#> "
                    + "SELECT distinct ?s WHERE {  "
                    + "GRAPH  <http://ucuenca.edu.ec/wkhuska/authors> {     "
                    + " ?s a foaf:Person.    ?s dct:provenance ?endpoint."
                    + " {"
                    + "    SELECT * { "
                    + "        	GRAPH <http://ucuenca.edu.ec/wkhuska/endpoints> { "
                    + "              ?endpoint uc:name \"UCUENCA\"^^xsd:string . "
                    + "            } "
                    + "        } "
                    + " }"
                    + "}}"*/
            Repository repository = new SPARQLRepository(constantService.getSPARQLEndpointURL());
            TupleQueryResult allAuthors = executeQuery(repository, allAuthorsQuery);
            
            int authorCount = 0;
            while (allAuthors.hasNext()) {
                String authorResource = allAuthors.next().getBinding("s").getValue().stringValue();
                authorCount++;
                
                //guardar en la variable sameAuthors los autores que ya tienen sameAs;
                Set<String> sameAuthors = getAllSameAuthors(repository, authorResource);
                
                for (String author : sameAuthors) {
                    try {
                        // 1. get the endpointurl from the author
                        //Encontramos el provenance del autor actual
                        String getNamesQuery = queriesService.getAuthorDataQuery(author);
                        TupleQueryResult resultAuthor = executeQuery(repository, getNamesQuery);
                        String endpointId = null;
                        if (resultAuthor.hasNext()) {
                            BindingSet next = resultAuthor.next();
                            endpointId = next.getBinding("provenance").getValue().stringValue();
                            //lastName = next.getBinding("lname").getValue().stringValue();
                            //fullName = namesAuthor.next().getBinding("name").getValue().stringValue();
                        }
                        //provenance
                        SparqlEndpoint endpoint = authorsendpointService.getEndpoint(endpointId);

                        /* Conecting to repository using LDC ( Linked Data Client ) Library */
                        ClientConfiguration config = new ClientConfiguration();
                        config.addEndpoint(new SPARQLEndpoint(endpoint.getName(), endpoint.getEndpointUrl(), "^" + "http://" + ".*"));
                        LDClientService ldClientEndpoint = new LDClient(config);

                        Repository endpointTempRepo = new SPARQLRepository(endpoint.getEndpointUrl());
                        TupleQueryResult resultArticles = executeQuery(endpointTempRepo, queriesService.getArticlesFromDspaceQuery(endpoint.getGraph(), author));

                        // 2. get the publications from author and save them with authorResource link
                        String articleId = null;
                        if (resultArticles.hasNext()) {
                            BindingSet next = resultArticles.next();
                            articleId = next.getBinding("docu").getValue().stringValue();
                            
                            saveAuthor(repository, authorResource);
                            saveArticleFromDspace(endpointTempRepo, repository, articleId, authorResource, ldClientEndpoint);

                        }

                        endpointTempRepo.shutDown();
                    } catch (Exception ex) {
                        log.error("Exception: " + ex);
                    }
                }
                
                
                
            }
            repository.shutDown();
            log.info("Finished to search publications from authors DSpace.");
            return "True for publications from Dspace";
            
        } catch (RepositoryException | MalformedQueryException | QueryEvaluationException ex) {
            log.error("Exception: " + ex);
        }
        return "fail";
    }
    
    private Set<String> getAllSameAuthors(Repository repository, String authorResource) {
        Set<String> sameAuthors = new HashSet<String>();
        sameAuthors.add(authorResource);
        try {
            
            String sameAsAuthorsQuery = queriesService.getSameAsAuthors(constantService.getAuthorsGraph(), authorResource);
            TupleQueryResult sameAsAuthors = executeQuery(repository, sameAsAuthorsQuery);
            while (sameAsAuthors.hasNext()) { // for each author
                String sameAsResource = sameAsAuthors.next().getBinding("o").getValue().stringValue();
                sameAuthors.add(sameAsResource);
                
                sameAsAuthorsQuery = queriesService.getSameAsAuthors(constantService.getSameAuthorsGraph(), sameAsResource);
                TupleQueryResult sameAsAuthors2 = executeQuery(repository, sameAsAuthorsQuery);
                while (sameAsAuthors2.hasNext()) { // for each author
                    String sameAsResource2 = sameAsAuthors2.next().getBinding("o").getValue().stringValue();
                    sameAuthors.add(sameAsResource2);
                }
            }
            
        } catch (RepositoryException | MalformedQueryException | QueryEvaluationException ex) {
            log.error("Exception: " + ex);
        }
        return sameAuthors;
    }
    
    private void saveAuthor(Repository repository, String authorResource) {
        try {
            String askQuery = queriesService.getAskResourceQuery(constantService.getWkhuskaGraph(), authorResource);
            if (!sparqlService.ask(QueryLanguage.SPARQL, askQuery)) {
                String getAuthor = queriesService.getAuthorsTuplesQuery(authorResource);
                TupleQueryResult authorProperties = executeQuery(repository, getAuthor);
                
                while (authorProperties.hasNext()) {
                    BindingSet tripletsResource = authorProperties.next();
                    String predicate = tripletsResource.getValue("p").stringValue();
                    String object = tripletsResource.getValue("o").stringValue();
                    String queryInsert = queriesService.buildInsertQuery(constantService.getWkhuskaGraph(), authorResource, predicate, object);
                    sparqlFunctionsService.updatePub(queryInsert);
                }
                
            }
//            MarmottaException
        } catch (RepositoryException | MalformedQueryException | QueryEvaluationException | MarmottaException | PubException ex) {
            log.error("Exception: " + ex);
        }
    }
    
    private void saveArticleFromDspace(Repository DspaceRepo, Repository localRepo, String articleId, 
            String authorResource, LDClientService ldClientEndpoint) {
        //TupleQueryResult result = executeQuery(endpointTemp, queriesService.getArticlesFromDspaceQuery(endpoint.getGraph(), author));
        try {

            String askQuery = queriesService.getAskResourceQuery(constantService.getWkhuskaGraph(), articleId);
            if (!sparqlService.ask(QueryLanguage.SPARQL, askQuery)) {
                //GRAPH
                String graphToSave = constantService.getWkhuskaGraph();//constantService.getDspaceGraph();

                String getResourcePropertyQuery = queriesService.getRetrieveResourceQuery();
                ClientResponse response = ldClientEndpoint.retrieveResource(articleId);
                Repository repository = ModelCommons.asRepository(response.getData());
                RepositoryConnection conn = repository.getConnection();
                TupleQueryResult tripletasResult = conn.prepareTupleQuery(QueryLanguage.SPARQL, getResourcePropertyQuery).evaluate();
                TupleQueryResult tripletasResult2 = conn.prepareTupleQuery(QueryLanguage.SPARQL, getResourcePropertyQuery).evaluate();
                String firstTitle = "";
                String secondTitle = "";
                String journal = "";
                while (tripletasResult.hasNext()) {
                    BindingSet tripletsResource = tripletasResult.next();
                    String predicate = tripletsResource.getValue("y").stringValue();
                    String object = tripletsResource.getValue("z").stringValue();

                    if ("http://purl.org/dc/terms/title".equals(predicate)) {

                        if (!firstTitle.isEmpty()) {
                            if (firstTitle.length() <= 45 && object.length() > 55) {
                                journal = firstTitle;
                                firstTitle = object;
                            } else if (firstTitle.length() > 55 && object.length() <= 45) {
                                journal = object;
                            } else {
                                secondTitle = object;
                            }
                        } else {
                            firstTitle = object;
                        }
                    }

                }
                String publicationURI = "http://ucuenca.edu.ec/wkhuska/publication/" + firstTitle.replaceAll("[^A-Za-z0-9 ]", "").replaceAll(" ", "-");

                while (tripletasResult2.hasNext()) {
                    BindingSet tripletsResource = tripletasResult2.next();
                    String predicate = tripletsResource.getValue("y").stringValue();
                    String object = tripletsResource.getValue("z").stringValue();
                    String queryPublicationInsert = "";

                    //Save creator and contributor
                    if ("http://purl.org/dc/terms/creator".equals(predicate)) {
                        queryPublicationInsert = queriesService.buildInsertQuery(graphToSave, publicationURI, "dc:creator", object);
                        sparqlFunctionsService.updatePub(queryPublicationInsert);
                    }

                    if ("http://purl.org/dc/terms/contributor".equals(predicate)) {
                        queryPublicationInsert = queriesService.buildInsertQuery(graphToSave, publicationURI, "http://purl.org/dc/terms/contributor", object);
                        sparqlFunctionsService.updatePub(queryPublicationInsert);
                    }
                    //dct:contributor   	http://purl.org/dc/terms/creator http://190.15.141.66:8899/ucuenca/contribuyente/ESPINOZA_MEJIA__M
                    //dc:creator   	http://purl.org/dc/terms/creator

                    //Save Subjects
                    if ("http://schema.org/mentions".equals(predicate)) {
                        queryPublicationInsert = queriesService.buildInsertQuery(
                                graphToSave, publicationURI, "dct:subject", getSubject(object));
                        sparqlFunctionsService.updatePub(queryPublicationInsert);
                        //dct:subject   	http://schema.org/mentions
                    }

                    //Save Keywords
                    //http://purl.org/ontology/bibo/Quote
                    //IsPartOf
                    if ("http://purl.org/dc/terms/isPartOf".equals(predicate)) {
                        queryPublicationInsert = queriesService.buildInsertQuery(
                                graphToSave, publicationURI, predicate, object);
                        sparqlFunctionsService.updatePub(queryPublicationInsert);
                        //dct:subject   	http://schema.org/mentions
                    }

                    //Save Abstract (Español e ingles)
                    //http://purl.org/ontology/bibo/abstract   	http://purl.org/ontology/bibo/abstract
                    if ("http://purl.org/ontology/bibo/abstract".equals(predicate)) {
                        queryPublicationInsert = queriesService.buildInsertQuery(
                                graphToSave, publicationURI, predicate, object);
                        sparqlFunctionsService.updatePub(queryPublicationInsert);
                    }

                    //Save URI
                    //http://purl.org/ontology/bibo/uri http://purl.org/ontology/bibo/uri
                    //(revisar)
                    if ("http://purl.org/ontology/bibo/uri".equals(predicate)) {
                        queryPublicationInsert = queriesService.buildInsertQuery(
                                graphToSave, publicationURI, "http://purl.org/ontology/bibo/uri", object);
                        sparqlFunctionsService.updatePub(queryPublicationInsert);
                    }

                    //http://purl.org/ontology/bibo/identifier http://purl.org/ontology/bibo/handle
                    //http://ucuenca.edu.ec/resource/sourceId http://purl.org/ontology/bibo/handle
                    if ("http://purl.org/ontology/bibo/handle".equals(predicate)) {
                        queryPublicationInsert = queriesService.buildInsertQuery(
                                graphToSave, publicationURI, "http://ucuenca.edu.ec/resource/sourceId", object);
                        sparqlFunctionsService.updatePub(queryPublicationInsert);
                        queryPublicationInsert = queriesService.buildInsertQuery(
                                graphToSave, publicationURI, "http://purl.org/ontology/bibo/identifier", object);
                        sparqlFunctionsService.updatePub(queryPublicationInsert);
                    }

                    //Save Year
                    //http://prismstandard.org/namespaces/basic/2.0/copyrightYear
                    //http://prismstandard.org/namespaces/basic/2.0/publicationYear   
                    //dc:date   
                    //http://purl.org/dc/terms/issued (solo año)
                    if ("http://purl.org/dc/terms/issued".equals(predicate)) {
                        queryPublicationInsert = queriesService.buildInsertQuery(
                                graphToSave, publicationURI, "dc:date", getYear(object));
                        sparqlFunctionsService.updatePub(queryPublicationInsert);
                        queryPublicationInsert = queriesService.buildInsertQuery(
                                graphToSave, publicationURI, "http://prismstandard.org/namespaces/basic/2.0/publicationYear", getYear(object));
                        sparqlFunctionsService.updatePub(queryPublicationInsert);
                        queryPublicationInsert = queriesService.buildInsertQuery(
                                graphToSave, publicationURI, "http://prismstandard.org/namespaces/basic/2.0/copyrightYear", getYear(object));
                        sparqlFunctionsService.updatePub(queryPublicationInsert);
                    }

                    //Save ISSN
                    //http://prismstandard.org/namespaces/basic/2.0/issn
                }

                //Save Title
                String queryPublicationInsert = queriesService.buildInsertQuery(
                        graphToSave, publicationURI, "dct:title", firstTitle);
                sparqlFunctionsService.updatePub(queryPublicationInsert);

                //Save Creator
                queryPublicationInsert = queriesService.buildInsertQuery(graphToSave, publicationURI, "dc:creator", authorResource);
                sparqlFunctionsService.updatePub(queryPublicationInsert);

                queryPublicationInsert = queriesService.buildInsertQuery(graphToSave, authorResource, "foaf:publications", publicationURI);
                sparqlFunctionsService.updatePub(queryPublicationInsert);

                //Other title
                if (!secondTitle.isEmpty()) {
                    queryPublicationInsert = queriesService.buildInsertQuery(
                            graphToSave, publicationURI, "dct:title", secondTitle);
                    sparqlFunctionsService.updatePub(queryPublicationInsert);
                }

                //Save Journal 
                if (!journal.isEmpty()) {
                    queryPublicationInsert = queriesService.buildInsertQuery(
                            graphToSave, publicationURI, "http://purl.org/ontology/bibo/Journal", journal);
                    sparqlFunctionsService.updatePub(queryPublicationInsert);
                    queryPublicationInsert = queriesService.buildInsertQuery(
                            graphToSave, publicationURI, "http://prismstandard.org/namespaces/basic/2.0/publicationName", journal);
                    sparqlFunctionsService.updatePub(queryPublicationInsert);
                }

                //Save Type
                queryPublicationInsert = queriesService.buildInsertQuery(
                        graphToSave, publicationURI, "rdf:type", "http://purl.org/ontology/bibo/Document");
                sparqlFunctionsService.updatePub(queryPublicationInsert);

                //Save Origin
                queryPublicationInsert = queriesService.buildInsertQuery(
                        graphToSave, publicationURI, "http://ucuenca.edu.ec/ontology#origin", "Dspace");
                sparqlFunctionsService.updatePub(queryPublicationInsert);

                //Save Organization
                queryPublicationInsert = queriesService.buildInsertQuery(
                        graphToSave, publicationURI, "foaf:Organization", "http://dspace/");
                sparqlFunctionsService.updatePub(queryPublicationInsert);

                conn.commit();
                conn.close();
            }
        } catch (Exception ex) {
            log.error("Exception: " + ex);
        }

    }
    
    private String getSubject (String uri) {
        return uri.substring(uri.lastIndexOf('/') + 1).replace("_", " ");
    }
    
    private String getYear (String dateString) {
        String[] date = dateString.split("-");//2012-01
        for (String dmy : date) {
            if (dmy.trim().length() == 4) {
                return dmy;
            }
        }
        return dateString;
    } 
    
    private TupleQueryResult executeQuery(Repository repository, String query) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        if (!repository.isInitialized()) {
            repository.initialize();
        }
        RepositoryConnection conn = repository.getConnection();
        conn.begin();
        TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate();
        conn.close();
        return result;
    }
    
    
    /*
     *   UPDATE - with SPARQL MODULE, to load triplet in marmotta plataform
     *   
     */
    public String updatePub(String querytoUpdate) {

        try {
            sparqlFunctionsService.updatePub(querytoUpdate);
        } catch (PubException ex) {
            log.error("No se pudo insertar: " + querytoUpdate);
            //         java.util.logging.Logger.getLogger(PubVocabServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "Correcto";

    }

    //construyendo sparql query insert 
    public String buildInsertQuery(String grapfhProv, String sujeto, String predicado, String objeto) {
        if (commonsServices.isURI(objeto)) {
            return queriesService.getInsertDataUriQuery(grapfhProv, sujeto, predicado, objeto);
        } else {
            return queriesService.getInsertDataLiteralQuery(grapfhProv, sujeto, predicado, objeto);
        }
    }

    @Override
    public void run() {
        runPublicationsProviderTaskImpl("uri");
    }

    public void insertSubResources(RepositoryConnection conUri, TupleQueryResult tripletasResult, String providerGraph) {
        try {
            String getPublicationPropertiesQuery = queriesService.getPublicationPropertiesAsResourcesQuery();
            TupleQuery resourcequery = conUri.prepareTupleQuery(QueryLanguage.SPARQL, getPublicationPropertiesQuery); //
            tripletasResult = resourcequery.evaluate();
            while (tripletasResult.hasNext()) {
                BindingSet tripletsResource = tripletasResult.next();
                String publicationResource = tripletsResource.getValue("publicationResource").toString();
                String publicationProperties = tripletsResource.getValue("publicationProperties").toString();
                String publicationPropertiesValue = tripletsResource.getValue("publicationPropertiesValue").toString();
                ///insert sparql query,
                String publicationPropertiesInsertQuery = buildInsertQuery(providerGraph, publicationResource, publicationProperties, publicationPropertiesValue);
                //load values publications to publications resource
                updatePub(publicationPropertiesInsertQuery);

            }
        } catch (RepositoryException | MalformedQueryException | QueryEvaluationException ex) {
            java.util.logging.Logger.getLogger(DspaceProviderServiceImpl.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }
}
