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
package org.apache.marmotta.ucuenca.wk.authors.services;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequenceWithBigrams;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.pipe.iterator.ArrayIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.InstanceList;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.commons.io.LineIterator;
import org.apache.marmotta.commons.sesame.model.ModelCommons;
import org.apache.marmotta.commons.vocabulary.FOAF;
import org.apache.marmotta.ldclient.api.ldclient.LDClientService;
import org.apache.marmotta.ldclient.endpoint.rdf.SPARQLEndpoint;
import org.apache.marmotta.ldclient.exception.DataRetrievalException;
import org.apache.marmotta.ldclient.model.ClientConfiguration;
import org.apache.marmotta.ldclient.model.ClientResponse;
import org.apache.marmotta.ldclient.services.ldclient.LDClient;
import org.apache.marmotta.ucuenca.wk.authors.api.AuthorService;
import org.apache.marmotta.ucuenca.wk.authors.api.EndpointService;
import org.apache.marmotta.ucuenca.wk.authors.api.SparqlEndpoint;
import org.apache.marmotta.ucuenca.wk.authors.api.SparqlFunctionsService;
import org.apache.marmotta.ucuenca.wk.authors.exceptions.DaoException;
import org.apache.marmotta.ucuenca.wk.authors.exceptions.UpdateException;
import org.apache.marmotta.ucuenca.wk.commons.service.CommonsServices;
import org.apache.marmotta.ucuenca.wk.commons.service.ConstantService;
import org.apache.marmotta.ucuenca.wk.commons.service.DistanceService;
import org.apache.marmotta.ucuenca.wk.commons.service.KeywordsService;
import org.apache.marmotta.ucuenca.wk.commons.service.QueriesService;
import org.openrdf.model.Statement;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sparql.SPARQLRepository;
import org.slf4j.Logger;

/**
 * Default Implementation of {@link AuthorService} Fernando B. CEDIA
 *
 * @author Xavier Sumba
 * @author Jose Cullcay
 */
@ApplicationScoped
public class AuthorServiceImpl implements AuthorService {

    @Inject
    private Logger log;

    @Inject
    private SparqlFunctionsService sparqlFunctionsService;

//    @Inject
//    private ConfigurationService configurationService;
    @Inject
    private QueriesService queriesService;

    @Inject
    private CommonsServices commonsService;

    @Inject
    private DistanceService distanceService;

    @Inject
    private KeywordsService kservice;

    @Inject
    private EndpointService authorsendpointService;

    @Inject
    private ConstantService constantService;

    private static final int LIMIT = 5000;
    private static final int MAX_SUBJECTS = 15;
    private static List<SparqlEndpoint> endpoints;
    private final List<String> stopwords = new ArrayList<>();
    private int processpercent = 0;

    private static int upperLimitKey = 5; //Check 6 keywords
    private static int lowerLimitKey = upperLimitKey - 1; //Not less than 4 keywords
    
    private PrintWriter out;

    private static double tolerance = 0.9;

    private Set<String> setExplored = new HashSet<String>();

    private static int one = 1;
    
    private static final String FILENAME = "DesambiguacionAutoresLog.csv";
    
    private Set<Entry> pairsCompared = null;
    
    private BufferedWriter bw = null;
    //private FileWriter fw = null;
    
    @PostConstruct
    public void init() {
        BufferedReader input = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("helpers/stoplist.txt")));
        LineIterator it = new LineIterator(input);
        String line;
        while (it.hasNext()) {
            line = it.nextLine();
            String[] words = line.split("\\s+");
            stopwords.addAll(Arrays.asList(words));
        }
        it.close();

//        filterProperties = Arrays.asList("http://www.w3.org/2004/02/skos/core#prefLabel",
//                "http://www.w3.org/2000/01/rdf-schema#comment",
//                "http://www.w3.org/ns/dcat#contactPoint",
//                "http://www.w3.org/ns/dcat#landingPage",
//                "http://vivoweb.org/ontology/core#freetextKeyword",
//                "http://www.w3.org/2002/07/owl#disjointWith", "http://rdaregistry.info",
//                "http://www.w3.org/2000/01/rdf-schema#label", "http://purl.org");
    }

    /**
     * authorDocumentProperty : http://rdaregistry.info/Elements/a/P50161 |
     * http://rdaregistry.info/Elements/a/P50195
     *
     * @throws org.apache.marmotta.ucuenca.wk.authors.exceptions.DaoException
     * @throws org.apache.marmotta.ucuenca.wk.authors.exceptions.UpdateException
     */
    //private String documentProperty = "http://rdaregistry.info";
    @Override
    public String extractAuthors() throws DaoException, UpdateException {
        endpoints = authorsendpointService.listEndpoints();

        Boolean someUpdate = false;
        StringBuilder response = new StringBuilder();
        if (!endpoints.isEmpty()) {
            for (SparqlEndpoint endpoint : endpoints) {
                if (Boolean.parseBoolean(endpoint.getStatus())) {
                    try {
                        log.info("Extraction started for endpoint {}.", endpoint.getName());
                        response.append(AuthorServiceImpl.this.extractAuthors(endpoint));
                    } catch (RepositoryException ex) {
                        log.error("ERROR: Excepcion de repositorio. Problemas en conectarse a " + endpoint.getName());
                    } catch (MalformedQueryException ex) {
                        log.error("ERROR: Excepcion de forma de consulta. Revise consultas SPARQL y sintaxis. Revise estandar SPARQL");
                    } catch (QueryEvaluationException ex) {
                        log.error("ERROR: Excepcion de ejecucion de consulta. No se ha ejecutado la consulta general para la obtencion de los Authores.");
                    } catch (Exception ex) {
                        log.error("ERROR: Exception... ", ex);
                    }
                    someUpdate = true;
                }
            }
//            response.append(extractSubjects());
//            response.append(searchDuplicates());

            if (!someUpdate) {
                return "Any  Endpoints";
            }
            return response.toString();
        } else {
            return "No Endpoints";
        }

    }

    private String extractAuthors(SparqlEndpoint endpoint) throws DaoException, UpdateException, RepositoryException, MalformedQueryException, QueryEvaluationException, Exception {
        int tripletasCargadas = 0; //cantidad de tripletas actualizadaas
        int contAutoresNuevosNoCargados = 0; //cantidad de actores nuevos no cargados
        int contAutoresNuevosEncontrados = 0; //hace referencia a la cantidad de actores existentes en el archivo temporal antes de la actualizacion

        /* Conecting to repository using LDC ( Linked Data Client ) Library */
        ClientConfiguration config = new ClientConfiguration();
        config.addEndpoint(new SPARQLEndpoint(endpoint.getName(), endpoint.getEndpointUrl(), "^" + "http://" + ".*"));
        LDClientService ldClientEndpoint = new LDClient(config);

        Repository endpointTemp = new SPARQLRepository(endpoint.getEndpointUrl());
        TupleQueryResult result = executeQuery(endpointTemp, queriesService.getCountPersonQuery(endpoint.getGraph()));
        int authorsSize = Integer.parseInt(result.next().getBinding("count").getValue().stringValue());//getAuthorsSize(conn, endpoint.getGraph());//Integer.parseInt(bindingCount.getValue("count").stringValue());

        //endpointTemp.initialize();
        //After that you can use the endpoint like any other Sesame Repository, by creating a connection and doing queries on that:
        // RepositoryConnection conn = endpointTemp.getConnection();
        //Query that let me obtain all resource related with author from source sparqlendpoint 
        String getAuthorsQuery = queriesService.getAuthorsQuery(endpoint.getGraph());
        String resource = "";
        for (int offset = 0; offset < authorsSize; offset += 5000) {
            try {
                TupleQueryResult authorsResult = executeQuery(endpointTemp, getAuthorsQuery + getLimitOffset(LIMIT, offset));//conn.prepareTupleQuery(QueryLanguage.SPARQL, getAuthorsQuery + getLimitOffset(limit, offset)).evaluate();
                while (authorsResult.hasNext()) {
                    resource = authorsResult.next().getValue("s").stringValue();
                    if (!sparqlFunctionsService.askAuthor(queriesService.getAskObjectQuery(constantService.getAuthorsGraph(), resource))) {
                        contAutoresNuevosEncontrados++;
                        printPercentProcess(contAutoresNuevosEncontrados, authorsSize, endpoint.getName());
                        String localResource = buildLocalURI(resource);

                        String getResourcePropertyQuery = queriesService.getRetrieveResourceQuery();
                        ClientResponse response = ldClientEndpoint.retrieveResource(resource);
                        Repository repository = ModelCommons.asRepository(response.getData());
                        RepositoryConnection conn = repository.getConnection();
                        TupleQueryResult tripletasResult = conn.prepareTupleQuery(QueryLanguage.SPARQL, getResourcePropertyQuery).evaluate();

                        while (tripletasResult.hasNext()) {
                            BindingSet tripletsResource = tripletasResult.next();
                            String predicate = tripletsResource.getValue("y").stringValue();
                            String object = tripletsResource.getValue("z").stringValue();

                            String insert = "";
                            switch (predicate) {
                                case "http://xmlns.com/foaf/0.1/givenName":// store foaf:firstName
                                case "http://xmlns.com/foaf/0.1/firstName":
                                    insert = queriesService.buildInsertQuery(constantService.getAuthorsGraph(), localResource, FOAF.firstName.toString(), object);
                                    sparqlFunctionsService.updateAuthor(insert);
                                    break;
                                case "http://xmlns.com/foaf/0.1/familyName": // store foaf:lastName
                                case "http://xmlns.com/foaf/0.1/lastName":
                                    insert = queriesService.buildInsertQuery(constantService.getAuthorsGraph(), localResource, FOAF.lastName.toString(), object);
                                    sparqlFunctionsService.updateAuthor(insert);
                                    break;
                                case "http://xmlns.com/foaf/0.1/name": // store foaf:name
                                    insert = queriesService.buildInsertQuery(constantService.getAuthorsGraph(), localResource, FOAF.name.toString(), object);
                                    sparqlFunctionsService.updateAuthor(insert);
                                    break;
                                case "http://www.w3.org/2002/07/owl#sameAs": // If sameas found include the provenance
                                    SparqlEndpoint newEndpoint = matchWithProvenance(object);
                                    if (newEndpoint != null) {
                                        String provenanceQueryInsert = queriesService.buildInsertQuery(constantService.getAuthorsGraph(), localResource, DCTERMS.PROVENANCE.toString(), newEndpoint.getResourceId());
                                        sparqlFunctionsService.updateAuthor(provenanceQueryInsert);
                                    }
                                    insert = queriesService.buildInsertQuery(constantService.getAuthorsGraph(), localResource, OWL.SAMEAS.toString(), object);
                                    sparqlFunctionsService.updateAuthor(insert);
                                    break;
                                default:
                            }
                        }
                        // Insert sameAs, provenance, and rdf:type foaf:Person
                        String sameAs = queriesService.buildInsertQuery(constantService.getAuthorsGraph(), localResource, OWL.SAMEAS.toString(), resource);
                        sparqlFunctionsService.updateAuthor(sameAs);

                        String provenanceQueryInsert = queriesService.buildInsertQuery(constantService.getAuthorsGraph(), localResource, DCTERMS.PROVENANCE.toString(), endpoint.getResourceId());
                        sparqlFunctionsService.updateAuthor(provenanceQueryInsert);

                        String foafPerson = queriesService.buildInsertQuery(constantService.getAuthorsGraph(), localResource, RDF.TYPE.toString(), FOAF.Person.toString());
                        sparqlFunctionsService.updateAuthor(foafPerson);

                        conn.commit();
                        conn.close();
                        repository.shutDown();
                    }
                }
            } catch (QueryEvaluationException ex) {
                log.error("Something happened evaluating the query Error: {}", ex.getMessage());
            } catch (DataRetrievalException ex) {
                log.error("Something happened retrieving triples for resource {} Error: {}", resource, ex.getMessage());
                contAutoresNuevosNoCargados++;
            } finally {
                endpointTemp.shutDown();
                ldClientEndpoint.shutdown();
            }

        }

        log.info(endpoint.getName() + " endpoint. Se detectaron " + contAutoresNuevosEncontrados + " autores nuevos ");
        log.info(endpoint.getName() + " endpoint. Se cargaron " + (contAutoresNuevosEncontrados - contAutoresNuevosNoCargados) + " autores nuevos exitosamente");
        log.info(endpoint.getName() + " endpoint. Se cargaron " + tripletasCargadas + " tripletas ");
        log.info(endpoint.getName() + " endpoint. No se pudieron cargar " + contAutoresNuevosNoCargados + " autores");

        return String.format("Carga Finalizada para %s endpoint. Revise Archivo Log Para mas detalles \n", endpoint.getName());
    }

    @Override
    public String extractSubjects() {
        endpoints = authorsendpointService.listEndpoints();
        try {
            Repository repository = new SPARQLRepository(constantService.getSPARQLEndpointURL());
            int numAuthors = Integer.parseInt(executeQuery(repository, queriesService.getCountAuthors()).next().getBinding("count").getValue().stringValue());
            for (int offset = 0; offset < numAuthors; offset += 5000) {
                String allAuthorsQuery = queriesService.getAuthors() + getLimitOffset(LIMIT, offset);
                TupleQueryResult allAuthors = executeQuery(repository, allAuthorsQuery);

                while (allAuthors.hasNext()) {
                    String authorResource = allAuthors.next().getBinding("s").getValue().stringValue();
                    // Get num subjects 
                    String numSubjectsQuery = queriesService.getCountSubjects(authorResource);
                    int numSubjects = Integer.parseInt(executeQuery(repository, numSubjectsQuery).next().getValue("count").stringValue());

                    // Get SameAsAuthors
                    String sameAsAuthorsQuery = queriesService.getSameAsAuthors(authorResource);
                    TupleQueryResult sameAsAuthors = executeQuery(repository, sameAsAuthorsQuery);

                    // Get Provenance
                    String provenanceQuery = queriesService.authorGetProvenance(authorResource);
                    String provenance = executeQuery(repository, provenanceQuery).next().getValue("name").stringValue();
                    while (sameAsAuthors.hasNext() && numSubjects < 3) { // extract subjects for each author
                        Set<String> documents = new HashSet<>();
                        Set<String> subjects = new HashSet<>();
                        //Set<String> mentions = new HashSet<>();

                        String sameAsResource = sameAsAuthors.next().getBinding("o").getValue().stringValue();
                        SparqlEndpoint endpoint = matchWithProvenance(provenance);
                        if (endpoint == null) {
                            log.warn("There isn't an endpoint for {} resource.", sameAsResource);
                            continue;
                        }
                        ClientConfiguration conf = new ClientConfiguration();
                        conf.addEndpoint(new SPARQLEndpoint(endpoint.getName(), endpoint.getEndpointUrl(), "^http://.*"));
                        LDClient ldc = new LDClient(conf);
                        ClientResponse response = ldc.retrieveResource(sameAsResource);
                        for (Statement statement : response.getData()) {
                            if (statement.getPredicate().stringValue().contains("http://rdaregistry.info")) {
                                Set<String>[] result = extractSubjectsAndDocuments(ldc, statement.getObject().stringValue());
                                subjects.addAll(result[0]);
                                documents.addAll(result[1]);
                                subjects.addAll(result[2]);
                            }
                        }
                        combineSubjects(authorResource, documents, subjects);
                        ldc.shutdown();
                    }
                }
            }

            repository.shutDown();
            log.info("Finished to extract subjects");
        } catch (QueryEvaluationException | RepositoryException | MalformedQueryException | DataRetrievalException ex) {
            log.error("Cannnot extract subjects. Error: {}", ex);
            return ex.getMessage();
        }
        return "Subjects Extracted";
    }

    @Override
    public String searchDuplicates() {
        //Keep track of the pairs of authors compared to avoid compairing them again.
        pairsCompared = new HashSet<Entry>();
        
        FileWriter fw = null;
        try {
            String allAuthorsQuery = queriesService.getAuthors(); 
                    /*"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX foaf: <http://xmlns.com/foaf/0.1/>  PREFIX owl: <http://www.w3.org/2002/07/owl#>  PREFIX dct: <http://purl.org/dc/terms/>  PREFIX mm: <http://marmotta.apache.org/vocabulary/sparql-functions#>  PREFIX dcat: <http://www.w3.org/ns/dcat#>  PREFIX bibo: <http://purl.org/ontology/bibo/> PREFIX dc: <http://purl.org/dc/elements/1.1/> "
                    + "SELECT distinct ?s WHERE {  "
                    + " graph <http://localhost:8080/context/authors> {" //+ "GRAPH  <http://ucuenca.edu.ec/wkhuska/authors> {     "
                    + " ?s a foaf:Person.    ?s dct:provenance ?endpoint. ?s foaf:name ?name. filter(regex(?name, \"Bonilla A\", \"i\"))."
                    + " {"
                    + "    SELECT * { "
                    + "        	GRAPH <http://ucuenca.edu.ec/wkhuska/endpoints> { "
                    + "              ?endpoint uc:name \"UCUENCA\"^^xsd:string . "
                    + "            } "
                    + "        } "
                    + " }"
                    + "}} order by desc(strlen(str(?name)))";*/
            Repository repository = new SPARQLRepository(constantService.getSPARQLEndpointURL());
            TupleQueryResult allAuthors = executeQuery(repository, allAuthorsQuery);
            
            out = new PrintWriter("ListAuthorsCompare.txt");
            
            fw = new FileWriter(FILENAME, false);
            bw = new BufferedWriter(fw);
            
            int authorCount = 0;
            while (allAuthors.hasNext()) {
                String authorResource = allAuthors.next().getBinding("s").getValue().stringValue();
                authorCount++;
                
                out.println(" Author Number: " + authorCount);
                //bw.write(" Author Number: " + authorCount);
                log.error(" Author Number: " + authorCount, " Author Number: " + authorCount);               
                //Encontramos los nombres del autor actual
                String getNamesQuery = queriesService.getAuthorDataQuery(authorResource);
                TupleQueryResult namesAuthor = executeQuery(repository, getNamesQuery);
                String firstName = "";
                String lastName = "";
                //String fullName = "";
                if (namesAuthor.hasNext()) {
                    BindingSet next = namesAuthor.next();
                    firstName = next.getBinding("fname").getValue().stringValue().trim().replace(".", "");
                    lastName = next.getBinding("lname").getValue().stringValue().trim().replace(".", "");
                    //fullName = namesAuthor.next().getBinding("name").getValue().stringValue();
                } else {
                    continue;
                }
                //guardar en la variable sameAuthors los autores que ya tienen sameAs;
                Set<String> sameAuthors = new HashSet<String>();
                //Set<String> sameAuthorsFinal = new HashSet<String>();
                String sameAsAuthorsQuery = queriesService.getSameAsAuthors(authorResource);
                TupleQueryResult sameAsAuthors = executeQuery(repository, sameAsAuthorsQuery);
                while (sameAsAuthors.hasNext()) { // for each author
                    String sameAsResource = sameAsAuthors.next().getBinding("o").getValue().stringValue();
                    sameAuthors.add(sameAsResource);
                    
                }
                
                //Encontramos los que pueden ser iguales
                sameAuthors = findSameAuthor(repository, sameAuthors, authorResource, firstName, lastName);
                log.info("Author: " + firstName + " " + lastName);
                if (sameAuthors.size() > one) {
                    //First: find the final author to be used
                     sameAuthors.add(authorResource);
                    String finalAuthor = selectFinalAuthor(repository, sameAuthors);
                    sameAuthors.remove(finalAuthor);
                    //Second, add the new authors
                    for (String sameAuthorResource : sameAuthors) {
                        if (!sameAuthorResource.equals(finalAuthor)) {//&& sameAuthorResource.contains(authorResource.substring(0, 35))) {
                            try {
                                String sameAs = queriesService.buildInsertQuery(constantService.getAuthorsGraph(), finalAuthor, OWL.SAMEAS.toString(), sameAuthorResource);
                                sparqlFunctionsService.updateAuthor(sameAs);

                                sameAs = queriesService.buildInsertQuery(constantService.getAuthorsGraph(), sameAuthorResource, OWL.SAMEAS.toString(), finalAuthor);
                                sparqlFunctionsService.updateAuthor(sameAs);
                            } catch (UpdateException ex) {
                                log.error("Cannot insert sameAs property for: <" + finalAuthor + "> and <" + sameAuthorResource + ">. Error: {}", ex.getMessage());
                            }
                        }
                    }

                    //guardar respaldos de autores repetidos y eliminarlos del grafo de autores;
                    deleteBackupRepeatedAuthors(repository, sameAuthors); 
                }
                
            }
            repository.shutDown();
            out.close();
            log.info("Finished to search for duplicate authors DSpace.");
        } catch (QueryEvaluationException | RepositoryException | MalformedQueryException | IOException ex) {
            log.error("Cannot search for duplicate authors DSpace. Error: {}", ex);
            pairsCompared = null;
            return ex.getMessage();
        } finally {
            try {

                if (bw != null) {
                    bw.close();
                }

                if (fw != null) {
                    fw.close();
                }

            } catch (IOException ex) {

                log.error("Cannot search for duplicate authors DSpace. Error: {}", ex);

            }
            pairsCompared = null;
        }
        return "Duplicate authors searched";
    }
    
    private String selectFinalAuthor(Repository repository, Set<String> setAuthors) {
        String finalAuthor = "";
        int maxlength = 0;
        
        for (String authorResource : setAuthors) {
            try {
                String getNamesQuery = queriesService.getAuthorDataQuery(authorResource);
                TupleQueryResult namesAuthor = executeQuery(repository, getNamesQuery);
                String firstName = "";
                String lastName = "";
                //String fullName = "";
                if (namesAuthor.hasNext()) {
                    BindingSet next = namesAuthor.next();
                    firstName = next.getBinding("fname").getValue().stringValue();
                    lastName = next.getBinding("lname").getValue().stringValue();
                    int length = (firstName + lastName).replace(" ", "").replace("??", "").replace("?", "").replace(".", "")
                            .replace(",", "").replace("-", "").replace("_", "").replace("!", "").replace("(", "")
                            .replace(")", "").length();
                    if (length > maxlength) {
                        finalAuthor = authorResource;
                        maxlength = length;
                    }
                }
            } catch (QueryEvaluationException | RepositoryException | MalformedQueryException ex) {
                log.error("Error at selecting Final Author. Error: {}", ex);
            }
        }
                
        return finalAuthor;
    }
    
    private void deleteBackupRepeatedAuthors (Repository repository, Set<String> setAuthors) {
        boolean backup;
        //guardar en nuevo grafo, y luego eliminar
        for (String authorResource : setAuthors) {
            try {
                backup = false;
                String getNamesQuery = queriesService.getAuthorsTuplesQuery(authorResource);
                TupleQueryResult triplesAuthor = executeQuery(repository, getNamesQuery);

                while (triplesAuthor.hasNext()) {
                    BindingSet tripletsResource = triplesAuthor.next();
                    String predicate = tripletsResource.getValue("p").stringValue();
                    String object = tripletsResource.getValue("o").stringValue();
                    
                    String queryAuthorInsert = queriesService.buildInsertQuery(constantService.getSameAuthorsGraph(), authorResource, predicate, object);
                    sparqlFunctionsService.updateAuthor(queryAuthorInsert);
                    
                    backup = true;
                }
            } catch (QueryEvaluationException | RepositoryException | MalformedQueryException | UpdateException ex) {
                log.error("Error at backuping Author. Error: {}", ex);
                backup = false;
            }
            
            //Eliminar los autores repetidos
            try {
                if (backup) {
                    String getDeleteAuthorQuery = queriesService.getAuthorDeleteQuery(authorResource);
                    sparqlFunctionsService.updateAuthor(getDeleteAuthorQuery);
                }
            } catch (UpdateException ex) {
                log.error("Error deleting authors. Error: {}", ex.getMessage());
            }
            
        }
        
        //return result;
    }
    
    private Set<String> findSameAuthor(Repository repository, Set<String> setResult, String authorResource, String nombres, String apellidos) {
        
        setExplored = new HashSet<String>();
            
        String givenName = cleaningTextAuthor(nombres);
        String lastName = cleaningTextAuthor(apellidos);

        //Getting the names
        String givenName1 = givenName.trim().split(" ")[0];
        String givenName2 = null;
        int numberGivenNames = givenName.split(" ").length;
        if (numberGivenNames > one) {
            givenName2 = removeAccents(nombres).trim().split(" ")[1];
        }

        String lastName1 = lastName.split(" ")[0];
        /*String lastName2 = null;
        int numberLastNames = lastName.split(" ").length;
        if (numberLastNames > one) {
            lastName2 = lastName.split(" ")[1];
        }*/
        
        // 1. Busca 4 nombres sin acentos
        boolean compare = true;
        if (numberGivenNames > one && lastName.split(" ").length > one) {
            compare = false;
        } //else {
            //Si tiene solo un nombre y un apellido, compara solo una vez y pasa
            //setResult.addAll(searchSameAuthor(setResult, repository, authorResource, nombres, apellidos, 
            //givenName, lastName, compare));
            //return setResult;
        //}
        
        setResult.addAll(searchSameAuthor(setResult, repository, authorResource, nombres, apellidos, 
        givenName, lastName, compare));

        // 2. primer nombre y apellidos
        if (numberGivenNames > one) {
            setResult.addAll(searchSameAuthor(setResult, repository, authorResource, nombres, apellidos, 
                givenName1, lastName, true));
            //10. dos nombres y primer apellido
            setResult.addAll(searchSameAuthor(setResult, repository, authorResource, nombres, apellidos,
                    givenName, lastName1, true));
        }

        // 3. segundo nombre y apellidos
        if (givenName2 != null && !givenName2.trim().isEmpty()) {
            setResult.addAll(searchSameAuthor(setResult, repository, authorResource, nombres, apellidos, 
            givenName2, lastName, true));

            // 5. segundo nombre y primer apellido (si hay mas de un nombre)
            setResult.addAll(searchSameAuthor(setResult, repository, authorResource, nombres, apellidos,
                    givenName2, lastName1, true));
            
            // 8. segunda inicial y apellidos (si hay mas de un nombre)
            String inicial = removeAccents("" + nombres.trim().split(" ")[1].charAt(0));//("" + givenName2.trim().charAt(0)).toUpperCase();
            setResult.addAll(searchSameAuthor(setResult, repository, authorResource, nombres, apellidos,
                    inicial, lastName, true));

            // 9. segunda inicial y primer apellido (si hay mas de un apellido)
            setResult.addAll(searchSameAuthor(setResult, repository, authorResource, nombres, apellidos,
                    inicial, lastName1, true));
            
        }

        // 4. primer nombre y primer apellido (si hay más de un nombre y un apellido)
        setResult.addAll(searchSameAuthor(setResult, repository, authorResource, nombres, apellidos,
            givenName1, lastName1, true));

        // 6. primera inicial y apellidos (si hay mas de un nombre y el primer nombre no es inicial solamente)
        //if (givenName1 != null && !givenName1.isEmpty()) {
            String inicial = ("" + givenName1.charAt(0)).contains(".") ? removeAccents("" + nombres.trim().charAt(0)) : "" + givenName1.charAt(0);
            //if (!inicial.equals(givenName1)) {
            inicial = inicial.toUpperCase();
                setResult.addAll(searchSameAuthor(setResult, repository, authorResource, nombres, apellidos,
                        inicial, lastName, true));
            //} 

            // 7. primera inicial y primer apellido (si hay más de un apellido y el nombre no era solo inicial)
            setResult.addAll(searchSameAuthor(setResult, repository, authorResource, nombres, apellidos,
                    inicial, lastName1, true));
        //}
        
        return setResult;
        
    } 
    
    private Set<String> searchSameAuthor(Set<String> setResult, Repository repository, String authorResource, String nombresOrig, String apellidosOrig, 
            String givenName, String lastName, boolean semanticCheck) {

        try {
            String similarAuthorResource = "";
            String otherGivenName = "";
            String otherLastName = "";
            String firstNameRegex = givenName.trim();
            if (nombresOrig.split(" ").length == one && nombresOrig.length() > one && apellidosOrig.split(" ").length > one) {
                firstNameRegex = ".*" + givenName.trim() + ".*$";
            }
            String queryNames = queriesService.getAuthorsByName(
                    constantService.getAuthorsGraph(), firstNameRegex.toUpperCase(), "^" + lastName.trim() + ".*$");
            TupleQueryResult similarAuthors = executeQuery(repository, queryNames);

            while (similarAuthors.hasNext()) {
                BindingSet next = similarAuthors.next();
                similarAuthorResource = next.getBinding("subject").getValue().stringValue();
                otherGivenName = next.getBinding("firstName").getValue().stringValue().trim();
                otherLastName = next.getBinding("lastName").getValue().stringValue().trim();
                
                /*if (pairsCompared.contains(new SimpleEntry<>(authorResource, similarAuthorResource)) 
                        || pairsCompared.contains(new SimpleEntry<>(similarAuthorResource, authorResource)) ) {
                    log.error("Comparados: " + authorResource + " - " +  similarAuthorResource );
                }*/
                
                boolean equalNames = false;
                if (!setResult.contains(similarAuthorResource) && !authorResource.equals(similarAuthorResource) 
                        && !setExplored.contains(similarAuthorResource) && !(pairsCompared.contains(new SimpleEntry<>(authorResource, similarAuthorResource)) 
                        || pairsCompared.contains(new SimpleEntry<>(similarAuthorResource, authorResource))) ) {
                    
                    float jaccard = distanceService.jaccardDistance(nombresOrig + " " + apellidosOrig, otherGivenName + " " + otherLastName);
                    boolean initialMatch = checkInitials(givenName.trim(), otherGivenName);
                    equalNames = getEqualNames(nombresOrig, apellidosOrig, otherGivenName, otherLastName, semanticCheck, repository) 
                            || (jaccard > 0.88 && jaccard <= 1.0 && initialMatch);
                    //double treeshold = 0.85;
                    if (equalNames) {
                        bw.write(nombresOrig + " - " + apellidosOrig + "," + otherGivenName + " - " + otherLastName + "," 
                           + jaccard + "\n");
                    }
                    
                    if (equalNames && semanticCheck) {
                        out.println(" ");
                        out.println("URI: " + authorResource + " URI2: " + similarAuthorResource);
                        out.println("Nombres originales:   " + nombresOrig);
                        out.println("Apellidos originales: " + apellidosOrig);
                        out.println("Nombres nuevos 2:     " + otherGivenName);
                        out.println("Apellidos nuevos 2:   " + otherLastName);
                        out.println("Sintactic equal?: " + equalNames);
                        
                        /*bw.write(" \n"
                                + "URI: " + authorResource + " URI2: " + similarAuthorResource + "\n"
                                + "Nombres originales:   " + nombresOrig + "\n"
                                + "Apellidos originales: " + apellidosOrig + "\n"
                                + "Nombres nuevos 2:     " + otherGivenName + "\n"
                                + "Apellidos nuevos 2:   " + otherLastName + "\n"
                                + "Sintactic equal?: " + equalNames + "\n"
                        );*/
                        
                        
                        equalNames = semanticCheck(authorResource, similarAuthorResource, repository);
                        
                        out.println("Semantic check?: " + semanticCheck);
        
                        out.println("Semantic check Result: " + equalNames);
                        out.println(" ");
                        
                        //bw.write(" \n" + "Semantic check?: " + semanticCheck + "\n" 
                        //        + "Semantic check Result: " + equalNames + "\n");
                        
                    }
                    if (equalNames) {
                        setResult.add(similarAuthorResource);
                    }
                    
                    Entry<String,String> pair = new SimpleEntry<>(authorResource, similarAuthorResource);
                    pairsCompared.add(pair);
                }
                setExplored.add(similarAuthorResource);
            }
            return setResult;
        } catch (QueryEvaluationException | RepositoryException | MalformedQueryException | IOException ex) {
            log.error("Cannot find similar authors for duplicate authors DSpace. Error: {}", ex);
        }

        return setResult;
    }
    
    public boolean checkInitials(String nombresOrig, String otherGivenName) {
        if (nombresOrig.length() == one) {
            return otherGivenName.contains(nombresOrig.toUpperCase());
        } else {
            return true;
        }
    }
    
    public boolean getEqualNames(String nombresOrig, String apellidosOrig, String otherGivenName, String otherLastName, boolean semanticCheck, Repository repository){
        boolean equal = false;
        //Getting the original names
        String givenName1 = removeAccents(nombresOrig.split(" ")[0]).toLowerCase().trim();
        String givenName2 = null;
        int numberGivenNames = nombresOrig.split(" ").length;
        if (numberGivenNames > one) {
            givenName2 = removeAccents(nombresOrig.split(" ")[1]).toLowerCase().trim();
        }

        String lastName1 = removeAccents(apellidosOrig.split(" ")[0]).toLowerCase().trim();
        String lastName2 = null;
        int numberLastNames = apellidosOrig.split(" ").length;
        if (numberLastNames > one) {
            lastName2 = removeAccents(apellidosOrig.split(" ")[1]).toLowerCase().trim();
        }
        
        //Getting the other names
        String otherGivenName1 = removeAccents(otherGivenName.split(" ")[0]).toLowerCase().trim();
        String otherGivenName2 = null;
        if (otherGivenName.split(" ").length > one) {
            otherGivenName2 = removeAccents(otherGivenName.split(" ")[1]).toLowerCase().trim();
        }

        String otherLastName1 = removeAccents(otherLastName.split(" ")[0]).toLowerCase().trim();
        String otherLastName2 = null;
        if (otherLastName.split(" ").length > one) {
            otherLastName2 = removeAccents(otherLastName.split(" ")[1]).toLowerCase().trim();
        }
        
        if (lastName2!=null && lastName2.length() == one && otherLastName2!=null && otherLastName2.trim().length() >= one) {
            otherLastName2 = otherLastName2.trim().substring(0, 1);
        }
        
        //Compare given names and surnames
        equal = compareNames(givenName1, givenName2, lastName1, lastName2, 
                otherGivenName1, otherGivenName2, otherLastName1, otherLastName2);
        
        // 1. Busca 4 nombres sin acentos
        // 2. primer nombre y apellidos
        // 3. segundo nombre y apellidos
        // 5. segundo nombre y primer apellido (si hay mas de un nombre)
        // 4. primer nombre y primer apellido (si hay más de un nombre y un apellido)
        // 6. primera inicial y apellidos (si hay mas de un nombre y el primer nombre no es inicial solamente)
        // 7. primera inicial y primer apellido (si hay más de un apellido y el nombre no era solo inicial)
        // 8. segunda inicial y apellidos (si hay mas de un nombre)
        // 9. segunda inicial y primer apellido (si hay mas de un apellido)
                
        return equal;
        
    }
    
    public boolean compareNames(String givenName1, String givenName2, String lastName1, String lastName2, 
            String otherGivenName1, String otherGivenName2, String otherLastName1, String otherLastName2) {
        boolean result = false;
        
        if (givenName2 != null  && lastName2 != null) {
            
            if (otherGivenName2 != null && otherLastName2 != null) {
                if (compareExactStrings(givenName1, otherGivenName1) && compareExactStrings(givenName2, otherGivenName2)
                        && compareExactStrings(lastName1, otherLastName1) && compareExactStrings(lastName2, otherLastName2)) {
                    return true;
                }
            } else if (otherGivenName2 != null && otherLastName2 == null) {
                if (compareExactStrings(givenName1, otherGivenName1) && compareExactStrings(givenName2, otherGivenName2)
                        && compareExactStrings(lastName1, otherLastName1)) {
                    return true;
                }
            } else if (otherGivenName2 == null && otherLastName2 != null) {
                if ((compareExactStrings(otherGivenName1, givenName1) || compareExactStrings(otherGivenName1, givenName2))
                        && compareExactStrings(lastName1, otherLastName1) && compareExactStrings(lastName2, otherLastName2)) {
                    return true;
                }
            } else if (otherGivenName2 == null && otherLastName2 == null
                    && (compareExactStrings(otherGivenName1, givenName1) || compareExactStrings(otherGivenName1, givenName2))
                    && compareExactStrings(lastName1, otherLastName1)) {
                    return true;
                
            }

        } else if (givenName2 == null && lastName2 != null) {
            if (otherGivenName2 != null && otherLastName2 != null) {
                if ((compareExactStrings(givenName1, otherGivenName1) || compareExactStrings(givenName1, otherGivenName2))
                        && compareExactStrings(lastName1, otherLastName1) && compareExactStrings(lastName2, otherLastName2)) {
                    return true;
                }
            } else if (otherGivenName2 != null && otherLastName2 == null) {
                if ((compareExactStrings(givenName1, otherGivenName1) || compareExactStrings(givenName1, otherGivenName2))
                        && compareExactStrings(lastName1, otherLastName1)) {
                    return true;
                }
            } else if (otherGivenName2 == null && otherLastName2 != null) {
                if (compareExactStrings(otherGivenName1, givenName1)
                        && compareExactStrings(lastName1, otherLastName1) && compareExactStrings(lastName2, otherLastName2) 
                   ) {
                    return true;

                }
            } else if (otherGivenName2 == null && otherLastName2 == null &&
                    compareExactStrings(otherGivenName1, givenName1) && compareExactStrings(lastName1, otherLastName1)) {
                    return true;
                
            }

        } else if (givenName2 != null && lastName2 == null) {
            if (otherGivenName2 != null) {
                if (compareExactStrings(givenName1, otherGivenName1) && compareExactStrings(givenName2, otherGivenName2)
                        && compareExactStrings(lastName1, otherLastName1)) {
                    return true;
                }
            } else if (otherGivenName2 == null && 
                    (compareExactStrings(otherGivenName1, givenName1) || compareExactStrings(otherGivenName1, givenName2))
                        && compareExactStrings(lastName1, otherLastName1)) {
                    return true;
                
            }

        } else if (givenName2 == null && lastName2 == null) {
            if (otherGivenName2 != null) {
                if ((compareExactStrings(givenName1, otherGivenName1) || compareExactStrings(givenName1, otherGivenName2))
                        && compareExactStrings(lastName1, otherLastName1)) {
                    return true;
                }
            } else if (otherGivenName2 == null && compareExactStrings(otherGivenName1, givenName1) 
                    && compareExactStrings(lastName1, otherLastName1)) {
                    return true;
                
            }

        }
        return result;
    }
    
    public boolean semanticCheck(String authorResource, String similarAuthorResource, Repository repository) {
        boolean result = false;
        double coefficient = 1.01;

        List<String> keywordsAuthor1 = getKeywordsAuthor(authorResource, repository);

        List<String> keywordsAuthor2 = getKeywordsAuthor(similarAuthorResource, repository);

        coefficient = distanceService.semanticComparisonValue(keywordsAuthor1, keywordsAuthor2);

        result = coefficient < tolerance;

        out.println("Keywords Author 1: " + keywordsAuthor1.toString());
        out.println("Keywords Author 2: " + keywordsAuthor2.toString());
        out.println("Distance: " + coefficient);
        out.println(" ");

        /*try {
            bw.write("Keywords Author 1: " + keywordsAuthor1.toString() + "\n"
                    + "Keywords Author 2: " + keywordsAuthor2.toString() + "\n"
                            + "Distance: " + coefficient + "\n");
        } catch (IOException ex) {
            log.error("Error deleting authors. Error: {}", ex.getMessage());
        }*/

        return result;
    }
    
    public List<String> getKeywordsAuthor(String authorResource, Repository repository) {
        List<String> keywordsAuthor = new ArrayList<>();
        try {
            String getQueryKeys = queriesService.getAuthorsKeywordsQuery(authorResource);
            TupleQueryResult keywords = executeQuery(repository, getQueryKeys);

            int cont = 0;
            while (keywords.hasNext() && cont <= upperLimitKey) {
                BindingSet bindingKey = keywords.next();
                String keyword = String.valueOf(bindingKey.getValue("keyword")).replace("\"", "").replace("^^", "").split("<")[0].trim();
                if (!keywordsAuthor.contains(keyword)) {
                    keywordsAuthor.add(keyword);
                    cont++;
                }
            }
            
            if (keywordsAuthor.size() < lowerLimitKey) {
                
                //get subjects as keywords 
                getQueryKeys = queriesService.getAuthorSubjectQuery(authorResource);
                keywords = executeQuery(repository, getQueryKeys);
                
                while (keywords.hasNext() && cont <= upperLimitKey) {
                    BindingSet bindingKey = keywords.next();
                    String keyword = String.valueOf(bindingKey.getValue("keyword")).replace("\"", "").replace("^^", "").split("<")[0].trim();
                    if (!keywordsAuthor.contains(keyword)) {
                        keywordsAuthor.add(keyword);
                        cont++;
                    }
                }
            }
            
        } catch (QueryEvaluationException | RepositoryException | MalformedQueryException ex) {
            log.error("Cannot find similar authors for duplicate authors DSpace. Error: {}", ex);
        }
        return keywordsAuthor;
        
    }
    
    public boolean compareExactStrings(String string1, String string2) {
        return (string1.matches("^" + string2 + "$") || string2.matches("^" + string1 + "$"));
    }

    public String cleaningTextAuthor(String value) {
        value = value.replace("??", ".*");
        value = value.replace("?", ".*").toLowerCase();
        value = value.replace(" de ", " ");
        value = value.replace("^del ", " ");
        value = value.replace(" del ", " ");
        value = value.replace(" los ", " ");
        value = value.replace(" y ", " ");
        value = value.replace(" las ", " ");
        value = value.replace(" la ", " ");
        // Cadena de caracteres original a sustituir.
        String original = "áàäéèëíìïóòöúùñÁÀÄÉÈËÍÌÏÓÒÖÚÙÜÑçÇ";
        String output = value;
        for (int i = 0; i < original.length(); i++) {
            // Reemplazamos los caracteres especiales.
            output = output.replace("" + original.charAt(i), ".*");
        }//end for i
        return output.trim();
    }
    
    public String removeAccents(String value) {
        value = value.replace(".", "");
        value = value.replace("??", ".*").trim();
        value = value.replace("?", ".*");
        // Cadena de caracteres original a sustituir.
        String original = "áàäéèëíìïóòöúùuñÁÀÄÉÈËÍÌÏÓÒÖÚÙÜÑçÇ";
        // Cadena de caracteres ASCII que reemplazarán los originales.
        String ascii = "aaaeeeiiiooouuunAAAEEEIIIOOOUUUNcC";
        String output = value;
        for (int i = 0; i < original.length(); i++) {
            // Reemplazamos los caracteres especiales.
            output = output.replace(original.charAt(i), ascii.charAt(i));
        }//end for i
        return output;
    }//removeAccents

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

    private Set<String>[] extractSubjectsAndDocuments(LDClientService ldClient, String documentURI)
            throws DataRetrievalException, RepositoryException, MalformedQueryException, QueryEvaluationException {
        Set<String> subjects = new HashSet<>();
        Set<String> documents = new HashSet<>();
        Set<String> mentions = new HashSet<>();

        List<Set<String>> result = new ArrayList<>();
        result.add(subjects);
        result.add(documents);
        result.add(mentions);
        ClientResponse respPub = ldClient.retrieveResource(documentURI);
        String document = "";
        for (Statement statement : respPub.getData()) {
            String value = statement.getObject().stringValue().trim();
            switch (statement.getPredicate().getLocalName()) {
                case "subject":
                    subjects.add(value);
                    break;
                case "mentions":
                    mentions.add(value.substring(value.lastIndexOf('/') + 1).replace("_", " ").toUpperCase().trim());
                    break;
                case "title":
                case "abstract":
                    document += value + " ";
                    break;
                default:
            }
        }
        if (!document.trim().equals("")) {
            documents.add(document);
        }
        return result.toArray(new Set[3]);
    }

    private String buildLocalURI(String endpointURI) {
        return constantService.getAuthorResource() + endpointURI.substring(endpointURI.lastIndexOf('/') + 1);
    }

    private List<String>[] findTopics(List<String> documents, int numTopics, int numWords) {
        Set<String> topics = new TreeSet<>();

        //File stoplist = new File(getClass().getClassLoader().getResource("/helpers/stoplist.txt"));
        ArrayIterator iterator = new ArrayIterator(documents);

        ArrayList<Pipe> workflow = new ArrayList<>();
        workflow.add(new CharSequence2TokenSequence("\\p{L}+"));
        workflow.add(new TokenSequenceLowercase());
        workflow.add(new TokenSequenceRemoveStopwords(false, false).addStopWords(stopwords.toArray(new String[]{})));
        workflow.add(new TokenSequence2FeatureSequenceWithBigrams());

        InstanceList data = new InstanceList(new SerialPipes(workflow));
        data.addThruPipe(iterator);

        ParallelTopicModel lda = new ParallelTopicModel(numTopics);
        lda.addInstances(data);
        try {
            lda.estimate();
        } catch (IOException ex) {
            log.error("Cannot find topics. Error: {}", ex.getMessage());
        } catch (ArrayIndexOutOfBoundsException ex) {
            log.error("Cannot find {} topics and {} words. Error: {}", numTopics, numWords, ex.getMessage());
        }

        for (Object[] words : lda.getTopWords(numWords)) {
            for (Object word : words) {
                topics.add(String.valueOf(word));
            }
        }
        Set<String> topicsToStore = new HashSet<>();
        // store 2 for each topic because sometimes words are repeated among topics
        for (Object[] words : lda.getTopWords(2)) {
            for (Object word : words) {
                topicsToStore.add(String.valueOf(word));
            }
        }
        return new List[]{
            new ArrayList<>(topics),
            new ArrayList<>(topicsToStore)};
    }

    /*
     * 
     * @param contAutoresNuevosEncontrados
     * @param allPersons
     * @param endpointName 
     */
    private void printPercentProcess(int contAutoresNuevosEncontrados, int allPersons, String endpointName) {

        if ((contAutoresNuevosEncontrados * 100 / allPersons) != processpercent) {
            processpercent = contAutoresNuevosEncontrados * 100 / allPersons;
            log.info("Procesado el: " + processpercent + " % del Endpoint: " + endpointName);
        }
    }

    private String getLimitOffset(int limit, int offset) {
        return " " + queriesService.getLimit(String.valueOf(limit)) + " " + queriesService.getOffset(String.valueOf(offset));
    }

    private void combineSubjects(String localSubject, Set<String> documents, Set<String> subjects) {//, Set<String> mentions) {
        // find topics and weight frequent words
        List<String>[] resultTopics = findTopics(new ArrayList(documents), 5, 15);
        List<String> topics = resultTopics[0];
        Set<String> selectedSubjects = new HashSet<>(getWeightedSubjects(subjects, topics));

        // Insert subjects
        for (String keyword : selectedSubjects) {
            if ((!commonsService.isURI(keyword))) {
                try {
                    String insertKeywords = queriesService.buildInsertQuery(constantService.getAuthorsGraph(), localSubject, DCTERMS.SUBJECT.toString(), kservice.cleaningText(keyword).toUpperCase());
                    sparqlFunctionsService.updateAuthor(insertKeywords);
                } catch (UpdateException ex) {
                    log.error("Cannot insert new subjects. Error: {}", ex.getMessage());
                }
            }
        }

        // Insert some topics
        for (String topic : resultTopics[1]) {
            try {
                String insertTopic = queriesService.buildInsertQuery(constantService.getAuthorsGraph(), localSubject, FOAF.topic.toString(), topic.trim().toUpperCase());
                sparqlFunctionsService.updateAuthor(insertTopic);
            } catch (UpdateException ex) {
                log.error("Cannot insert topics. Error: {}", ex.getMessage());
            }
        }
        log.info("Resource {} has {} documents and {} subjects ", localSubject, documents.size(), selectedSubjects.size());
    }

    private List<String> getWeightedSubjects(Set<String> subjects, List<String> topics) {
        List<String> result = new ArrayList<>();
        Map<String, Integer> rank = initializeHash(subjects);

        for (Map.Entry<String, Integer> entry : rank.entrySet()) {
            String subject = entry.getKey();
            for (String topic : topics) {
                //if (areSimilar(subject.toLowerCase(), topic)) {
                if (subject.toLowerCase().contains(topic)) {
                    rank.put(subject, rank.get(subject) + 1);
                }
            }
        }

        for (Entry<String, Integer> entry : selectRankedSubjects(rank)) {
            if (entry.getValue() > 0) {
                result.add(entry.getKey());
            }
            if (result.size() == MAX_SUBJECTS) {
                break;
            }
        }
        return result;
    }

    private List<Entry<String, Integer>> selectRankedSubjects(Map<String, Integer> map) {

        map.values().remove(0);
        List<Entry<String, Integer>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, new Comparator<Entry<String, Integer>>() {
            @Override
            public int compare(Entry<String, Integer> o1,
                    Entry<String, Integer> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        return list;
    }

    private Map<String, Integer> initializeHash(Set<String> subjects) {
        ConcurrentHashMap<String, Integer> hm = new ConcurrentHashMap<>();
        for (String subject : subjects) {
            hm.put(subject, 0);
        }
        return hm;
    }

    private SparqlEndpoint matchWithProvenance(String provenanceName) {
        for (SparqlEndpoint endpoint : endpoints) {
            if (provenanceName.equals(endpoint.getName())) {
                return endpoint;
            }
        }
        return null;
    }

}
