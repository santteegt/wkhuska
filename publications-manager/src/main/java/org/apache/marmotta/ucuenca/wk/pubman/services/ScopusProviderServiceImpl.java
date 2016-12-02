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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.marmotta.commons.sesame.model.ModelCommons;
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

import org.apache.marmotta.ucuenca.wk.pubman.api.SparqlFunctionsService;

import org.apache.marmotta.ucuenca.wk.pubman.exceptions.PubException;
import org.apache.marmotta.ucuenca.wk.pubman.api.ScopusProviderService;
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
//import org.openrdf.rio.RDFFormat;
//import org.openrdf.rio.RDFHandlerException;
//import org.openrdf.rio.RDFWriter;
//import org.openrdf.rio.Rio;
import org.semarglproject.vocab.OWL;

/**
 * Default Implementation of {@link PubVocabService}
 *
 * @author Freddy Sumba
 */
@ApplicationScoped
public class ScopusProviderServiceImpl implements ScopusProviderService, Runnable {

    @Inject
    private Logger log;

    @Inject
    private QueriesService queriesService;

    @Inject
    private ConstantService constantService;

    @Inject
    private KeywordsService kservice;

    @Inject
    private CommonsServices commonsServices;

    @Inject
    private GetAuthorsGraphData getauthorsData;

    @Inject
    private DistanceService distance;

    @Inject
    private SparqlFunctionsService sparqlFunctionsService;

    private int processpercent = 0;

    private String URLSEARCHSCOPUS = "http://api.elsevier.com/content/search/author?query=authfirst%28FIRSTNAME%29authlast%28LASTNAME%29+AND+affil%28PAIS%29&apiKey=a3b64e9d82a8f7b14967b9b9ce8d513d&httpAccept=application/xml";
    private String AFFILIATIONPARAM = "+AND+affil%28PAIS%29";
    List<Map<String, Value>> uniNames = new ArrayList<>(); //nombre de universidades
    
    @Inject
    private SparqlService sparqlService;

    @Override
    public String runPublicationsTaskImpl(String param) {
        return null;
    }

    @Override
    public String runPublicationsProviderTaskImpl(String param) {
        try {

            ClientConfiguration conf = new ClientConfiguration();
            LDClient ldClient = new LDClient(conf);
            int membersSearchResult = 0;
            String nameToFind = "";
            String authorResource = "";
            int priorityToFind = 0;

            //Get names of universities from endpoints in Spanish and English
            String getEndpointsQuery = queriesService.getlistEndpointNamesQuery();
            uniNames = sparqlService.query(QueryLanguage.SPARQL, getEndpointsQuery);
            
            List<Map<String, Value>> resultAllAuthors = getauthorsData.getListOfAuthors();

            /*To Obtain Processed Percent*/
            int allPersons = resultAllAuthors.size();
            int processedPersons = 0;

            RepositoryConnection conUri = null;
            ClientResponse response = null;

            Properties propiedades = new Properties();
            InputStream entrada = null;
            Map<String, String> mapping = new HashMap<String, String>();
            try {
                ClassLoader classLoader = getClass().getClassLoader();
                entrada = classLoader.getResourceAsStream("updatePlatformProcessConfig.properties");
                propiedades.load(entrada);
                for (String source : propiedades.stringPropertyNames()) {
                    String target = propiedades.getProperty(source);
                    mapping.put(source, target);

                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (entrada != null) {
                    try {
                        entrada.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            boolean proccesAllAuthors = Boolean.parseBoolean(mapping.get("proccesAllAuthors").toString());
            boolean semanticAnalizer = Boolean.parseBoolean(mapping.get("semanticAnalizer").toString());

            for (Map<String, Value> map : resultAllAuthors) {
                processedPersons++;
                log.info("Autores procesados con Scopus: " + processedPersons + " de " + allPersons);
                authorResource = map.get("subject").stringValue();
                String firstName = map.get("fname").stringValue();
                String lastName = map.get("lname").stringValue();
                boolean ask = false;
                if (!proccesAllAuthors) {
                    String askTripletQuery = queriesService.getAskProcessAlreadyAuthorProvider(constantService.getScopusGraph(), authorResource);
                    try {

                        ask = sparqlService.ask(QueryLanguage.SPARQL, askTripletQuery);
                        if (ask) {
                            continue;
                        }
                    } catch (Exception ex) {
                        log.error("Marmotta Exception:  " + askTripletQuery);
                    }
                }
                priorityToFind = 1;
                try {
                    List<String> uri_search = new ArrayList<>();
                    membersSearchResult = 0;
                    String authorNativeResource = null;
                    String firstNameSearch = firstName.split(" ").length > 1 ? firstName.split(" ")[0] : firstName;
                    String lastNameSearch = lastName.split(" ").length > 1 ? lastName.split(" ")[0] : lastName;
                    String lastNameSearch2 = lastName.split(" ").length > 1 ? lastName.split(" ")[1] : "";
                    uri_search.add(URLSEARCHSCOPUS.replace("FIRSTNAME", firstNameSearch.length() > 0 ? firstNameSearch : firstName).replace("LASTNAME", lastNameSearch.length() > 0 ? lastNameSearch : lastName).replace("AFFILIATION", AFFILIATIONPARAM).replace("PAIS", "Ecuador"));
                    uri_search.add(URLSEARCHSCOPUS.replace("FIRSTNAME", firstNameSearch.length() > 0 ? firstNameSearch : firstName).replace("LASTNAME", lastNameSearch.length() > 1 ? lastNameSearch + "%20" + lastNameSearch2 : lastName).replace("AFFILIATION", ""));
                    uri_search.add(URLSEARCHSCOPUS.replace("FIRSTNAME", firstNameSearch.length() > 0 ? firstNameSearch : firstName).replace("LASTNAME", lastNameSearch.length() > 0 ? lastNameSearch : lastName).replace("AFFILIATION", ""));
                    String scopusfirstName = "";
                    String scopuslastName = "";
                    String scopusAuthorUri = "";
                    String providerGraph = "";
                    List <String> scopusAffiliation = new ArrayList();
                    Boolean testAffiliation = true;
                    
                    for (String uri_searchIterator : uri_search) {
                        try {
                            boolean existNativeAuthor = false;
                            nameToFind = uri_searchIterator;
//                            nameToFind = URLSEARCHSCOPUS.replace("FIRSTNAME", "Mauricio").replace("LASTNAME", "Espinoza").replace("PAIS", "all");
                            membersSearchResult = 0;

                            if (!proccesAllAuthors) {
                                existNativeAuthor = sparqlService.ask(QueryLanguage.SPARQL, queriesService.getAskResourceQuery(constantService.getScopusGraph(), nameToFind.replace(" ", "")));
                            }
                            if ((nameToFind.compareTo("") != 0) && !existNativeAuthor) {
                                response = ldClient.retrieveResource(nameToFind);

                                /**
                                 * Se inserta la tripleta que muestra el intento
                                 * de búsqueda (Esta tripleta NO ofrece sentido
                                 * semantico). Aqui porque el intento debe ser
                                 * plasmado cuando el proveedor no de error al
                                 * buscar el recurso.
                                 */
                                String nameEndpointofPublications = ldClient.getEndpoint(URLSEARCHSCOPUS + nameToFind).getName();
                                providerGraph = constantService.getProviderNsGraph() + "/" + nameEndpointofPublications.replace(" ", "");
                                String InsertQueryOneOf = buildInsertQuery(providerGraph, nameToFind.replace(" ", ""), OWL.ONE_OF, authorResource);
                                updatePub(InsertQueryOneOf);
                            } else {
                                continue;
                            }
                            String getMembersQuery = queriesService.getObjectByPropertyQuery("foaf:member");
                            conUri = ModelCommons.asRepository(response.getData()).getConnection();
                            conUri.begin();
                            TupleQueryResult membersResult = conUri.prepareTupleQuery(QueryLanguage.SPARQL, getMembersQuery).evaluate();

                            while (membersResult.hasNext()) {
                                BindingSet bindingname = membersResult.next();
                                scopusAuthorUri = bindingname.getValue("object").toString();
                                membersSearchResult++;
                            }
                            if (membersSearchResult == 1) {
                                /**
                                 * Getting contributor name to compare using
                                 * comparisonNames.syntacticComparison function
                                 * - move this query to Queries Service
                                 */
                                String getScopusAuthorName = "SELECT ?firstName ?lastName "
                                        + " WHERE { "
                                        + " <" + scopusAuthorUri + ">  <http://www.elsevier.com/xml/svapi/rdf/dtd/givenName> ?firstName. "
                                        + " <" + scopusAuthorUri + ">  <http://www.elsevier.com/xml/svapi/rdf/dtd/surname> ?lastName. "
                                        + " }";
                                TupleQueryResult nameResult = conUri.prepareTupleQuery(QueryLanguage.SPARQL, getScopusAuthorName).evaluate();
                                while (nameResult.hasNext()) {
                                    BindingSet binding = nameResult.next();
                                    scopusfirstName = binding.getValue("firstName").stringValue();
                                    scopuslastName = binding.getValue("lastName").stringValue();
                                }
                                //(Jose Luis) Test the affiliation of the researcher
                                String getScopusAffiliation = "SELECT ?affiliation "
                                        + " WHERE { "
                                        + " <" + scopusAuthorUri + "> <http://www.elsevier.com/xml/svapi/rdf/dtd/affiliation> ?uriAffi. "
                                        + " ?uriAffi <http://www.w3.org/2004/02/skos/core#prefLabel> ?affiliation "
                                        + " }";
                                TupleQueryResult affiResult = conUri.prepareTupleQuery(QueryLanguage.SPARQL, getScopusAffiliation).evaluate();
                                String affiliation = "";
                                testAffiliation = true;
                                while (affiResult.hasNext()) {
                                    BindingSet binding = affiResult.next();
                                    affiliation = binding.getValue("affiliation").stringValue();
                                    scopusAffiliation.add(affiliation);
                                    testAffiliation = testAffiliation(affiliation);
                                    if(testAffiliation) break;    
                                }
                                break;
                            }
                            if (response.getHttpStatus() == 503 || membersSearchResult != 1) {
                                log.error("Error de getStatus o Error de mas de un author como resultado de " + nameToFind);
                                continue;
                            }
                        } catch (DataRetrievalException e) {
                            log.error("Data Retrieval Exception: " + e);
                        }
                    }
                    
                    String scopusfullname = scopuslastName + ":" + scopusfirstName;
                    String localfullname = lastName + ":" + firstName;

//                    if (localfullname.toUpperCase().contains("PIEDRA")) {
//                        localfullname = localfullname.replace(".", "");
//                    }
                    if (membersSearchResult == 1 //&& testAffiliation && distance.syntacticComparisonNames("local", localfullname, "scopus", scopusfullname)
                    ) {

                        List<String> listA = kservice.getKeywordsOfAuthor(authorResource);//dspace
                        List<String> listB = new ArrayList<String>();//desde la fuente de pub
                        String getPublicationsAndTitleFromProviderQuery = queriesService.getSubjectAndObjectByPropertyQuery("dc:title");
                        TupleQuery abstracttitlequery = conUri.prepareTupleQuery(QueryLanguage.SPARQL, getPublicationsAndTitleFromProviderQuery); //
                        TupleQueryResult abstractResult = abstracttitlequery.evaluate();

                        while (abstractResult.hasNext()) {
                            BindingSet abstractResource = abstractResult.next();
                            // String abstracttext = abstractResource.getValue("abstract").toString();
                            String publication = abstractResource.getValue("subject").toString();

                            String titletext = abstractResource.getValue("object").toString();
                            listB = kservice.getKeywords(titletext);
                            int cero = 0;
                            if (semanticAnalizer && listB.size() != cero && listA.size() != cero && distance.semanticComparison(listA, listB)) {

                                String getPublicationsFromProviderQuery = queriesService.getPublicationsPropertiesQuery(publication);
                                TupleQuery pubquery = conUri.prepareTupleQuery(QueryLanguage.SPARQL, getPublicationsFromProviderQuery); //
                                TupleQueryResult tripletasResult = pubquery.evaluate();

                                while (tripletasResult.hasNext()) {
                                    try {
                                        BindingSet tripletsResource = tripletasResult.next();
                                        //authorNativeResource = tripletsResource.getValue("authorResource").toString();
                                        String publicationProperty = tripletsResource.getValue("property").toString();
                                        String publicationObject = tripletsResource.getValue("value").toString();
                                        ///insert sparql query, 
                                        String publicationInsertQuery = buildInsertQuery(providerGraph, publication, publicationProperty, publicationObject);
                                        updatePub(publicationInsertQuery);

                                        // insert dct:contributor      <> dct:contributor <http://dblp.org/pers/xr/s/Saquicela:Victor> 
                                        String contributorInsertQuery = buildInsertQuery(providerGraph, publication, "http://purl.org/dc/terms/contributor", scopusAuthorUri);
                                        updatePub(contributorInsertQuery);

                                        // sameAs triplet    <http://190.15.141.102:8080/dspace/contribuidor/autor/SaquicelaGalarza_VictorHugo> owl:sameAs <http://dblp.org/pers/xr/s/Saquicela:Victor> 
                                        String sameAsInsertQuery = buildInsertQuery(providerGraph, authorResource, "http://www.w3.org/2002/07/owl#sameAs", scopusAuthorUri);
                                        updatePub(sameAsInsertQuery);

                                        //if value is an uri then search and insert values of this value
                                        if (commonsServices.isURI(publicationObject)) {

                                            String getResourcesQuery = queriesService.getPublicationsPropertiesQuery(publicationObject);
                                            TupleQuery resourcequery = conUri.prepareTupleQuery(QueryLanguage.SPARQL, getResourcesQuery); //
                                            TupleQueryResult resourceResult = resourcequery.evaluate();

                                            while (resourceResult.hasNext()) {
                                                BindingSet resource = resourceResult.next();
                                                //authorNativeResource = tripletsResource.getValue("authorResource").toString();
                                                String resourceProperty = resource.getValue("property").toString();
                                                String resourceObject = resource.getValue("value").toString();
                                                ///insert sparql query, 
                                                String resourceInsertQuery = buildInsertQuery(providerGraph, publicationObject, resourceProperty, resourceObject);
                                                updatePub(resourceInsertQuery);
                                            }
                                        }
                                    } catch (Exception e) {
                                        log.error("ioexception " + e.toString());
                                    }

                                }

                            }//end IF semantic distance
                            else if (!semanticAnalizer){
                                String getPublicationsFromProviderQuery = queriesService.getPublicationsPropertiesQuery(publication);
                                TupleQuery pubquery = conUri.prepareTupleQuery(QueryLanguage.SPARQL, getPublicationsFromProviderQuery); //
                                TupleQueryResult tripletasResult = pubquery.evaluate();

                                while (tripletasResult.hasNext()) {
                                    try {
                                        BindingSet tripletsResource = tripletasResult.next();
                                        //authorNativeResource = tripletsResource.getValue("authorResource").toString();
                                        String publicationProperty = tripletsResource.getValue("property").toString();
                                        String publicationObject = tripletsResource.getValue("value").toString();
                                        ///insert sparql query, 
                                        String publicationInsertQuery = buildInsertQuery(providerGraph, publication, publicationProperty, publicationObject);
                                        updatePub(publicationInsertQuery);

                                        // insert dct:contributor      <> dct:contributor <http://dblp.org/pers/xr/s/Saquicela:Victor> 
                                        String contributorInsertQuery = buildInsertQuery(providerGraph, publication, "http://purl.org/dc/terms/contributor", scopusAuthorUri);
                                        updatePub(contributorInsertQuery);

                                        // sameAs triplet    <http://190.15.141.102:8080/dspace/contribuidor/autor/SaquicelaGalarza_VictorHugo> owl:sameAs <http://dblp.org/pers/xr/s/Saquicela:Victor> 
                                        String sameAsInsertQuery = buildInsertQuery(providerGraph, authorResource, "http://www.w3.org/2002/07/owl#sameAs", scopusAuthorUri);
                                        updatePub(sameAsInsertQuery);

                                        //if value is an uri then search and insert values of this value
                                        if (commonsServices.isURI(publicationObject)) {

                                            String getResourcesQuery = queriesService.getPublicationsPropertiesQuery(publicationObject);
                                            TupleQuery resourcequery = conUri.prepareTupleQuery(QueryLanguage.SPARQL, getResourcesQuery); //
                                            TupleQueryResult resourceResult = resourcequery.evaluate();

                                            while (resourceResult.hasNext()) {
                                                BindingSet resource = resourceResult.next();
                                                //authorNativeResource = tripletsResource.getValue("authorResource").toString();
                                                String resourceProperty = resource.getValue("property").toString();
                                                String resourceObject = resource.getValue("value").toString();
                                                ///insert sparql query, 
                                                String resourceInsertQuery = buildInsertQuery(providerGraph, publicationObject, resourceProperty, resourceObject);
                                                updatePub(resourceInsertQuery);
                                            }
                                        }
                                    } catch (Exception e) {
                                        log.error("ioexception " + e.toString());
                                    }

                                }
                            }//end else if NO semantic Analizer

                        }
                        conUri.commit();
                        conUri.close();
                    }
                  
                } catch (QueryEvaluationException | MalformedQueryException | RepositoryException ex) {
                    log.error("Evaluation Exception: " + ex);
                } catch (Exception e) {
                    log.error("ioexception " + e.toString());
                }
                //** end View Data
                printPercentProcess(processedPersons, allPersons, "SCOPUS");
            }
            return "True for publications";
        } catch (Exception ex) {
            log.error("Exception: " + ex);
        }
        return "fail";
    }

    /** Jose Luis
     * Tests if an affiliation matches with the universities in the enpoints.
     * @param affiliation
     * @return 
     */
    private Boolean testAffiliation(String affiliation) throws MarmottaException {
        if (affiliation.contains("Ecuador")) return true; 
        //try to match the affiliations with a university name
        for(Map<String, Value> name: uniNames){
            double dist = distance.cosineSimilarityAndLevenshteinDistance(affiliation, name.get("fullName").stringValue());
            if (dist > 0.9) {
                //cercanos.println("Distance1: " + dist + " Scopus: " + affiliation + " Endpoint: " + name.get("fullName").stringValue() );
                return true;
            }
        }
        return false;
    }
    
    @Override
    public JsonArray SearchAuthorTaskImpl(String uri) {
        return null;
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

    /*
     * 
     * @param contAutoresNuevosEncontrados
     * @param allPersons
     * @param endpointName 
     */
    public void printPercentProcess(int processedPersons, int allPersons, String provider) {

        if ((processedPersons * 100 / allPersons) != processpercent) {
            processpercent = processedPersons * 100 / allPersons;
            log.info("Procesado el: " + processpercent + " % de " + provider);
        }
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
            java.util.logging.Logger.getLogger(ScopusProviderServiceImpl.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }
}
