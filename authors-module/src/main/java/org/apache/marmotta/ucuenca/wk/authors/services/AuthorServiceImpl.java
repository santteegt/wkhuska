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

//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.marmotta.commons.sesame.model.ModelCommons;
import org.apache.marmotta.ldclient.api.ldclient.LDClientService;
import org.apache.marmotta.ldclient.endpoint.rdf.SPARQLEndpoint;
import org.apache.marmotta.ldclient.exception.DataRetrievalException;
import org.apache.marmotta.ldclient.model.ClientConfiguration;
import org.apache.marmotta.ldclient.model.ClientResponse;
import org.apache.marmotta.ldclient.services.ldclient.LDClient;
import org.apache.marmotta.platform.core.api.config.ConfigurationService;
//import org.apache.marmotta.platform.core.exception.InvalidArgumentException;
import org.apache.marmotta.ucuenca.wk.authors.api.AuthorService;
import org.apache.marmotta.ucuenca.wk.authors.api.SparqlFunctionsService;
import org.apache.marmotta.ucuenca.wk.authors.exceptions.AskException;
import org.apache.marmotta.ucuenca.wk.authors.exceptions.DaoException;
import org.apache.marmotta.ucuenca.wk.authors.exceptions.UpdateException;
import org.apache.marmotta.ucuenca.wk.commons.service.QueriesService;
import org.apache.marmotta.ucuenca.wk.authors.api.EndpointService;
import org.apache.marmotta.ucuenca.wk.authors.api.SparqlEndpoint;
import org.apache.marmotta.ucuenca.wk.commons.service.CommonsServices;
import org.apache.marmotta.ucuenca.wk.commons.service.ConstantService;
import org.apache.marmotta.ucuenca.wk.commons.service.KeywordsService;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sparql.SPARQLRepository;

/**
 * Default Implementation of {@link AuthorService} Fernando B. CEDIA
 */
@ApplicationScoped
public class AuthorServiceImpl implements AuthorService {

    @Inject
    private Logger log;

    @Inject
    private SparqlFunctionsService sparqlFunctionsService;

    @Inject
    private ConfigurationService configurationService;

    @Inject
    private QueriesService queriesService;

    @Inject
    private CommonsServices commonsService;

    @Inject
    private KeywordsService kservice;

    @Inject
    private EndpointService authorsendpointService;
    
    @Inject
    private ConstantService constantService;

    private int limit = 5000;

    private int processpercent = 0;

    private boolean provenanceinsert = false; //variable to know if the provenance of an author was already inserted

    /**
     * authorDocumentProperty : http://rdaregistry.info/Elements/a/P50161 |
     * http://rdaregistry.info/Elements/a/P50195
     */
    private String authorDocumentProperty = "http://rdaregistry.info";

    @Override
    public String runAuthorsUpdateMultipleEP(String endpp, String graph) throws DaoException, UpdateException {
        Boolean someUpdate = false;
        StringBuilder response = new StringBuilder();
        if (authorsendpointService.listEndpoints().size() != 0) {
            for (SparqlEndpoint endpoint : authorsendpointService.listEndpoints()) {
                if (endpoint.getStatus().equals("true")) {
                    response.append("\n ENDPOINT: ");
                    response.append(endpoint.getName());
                    response.append(":  ");
                    try {
                        response.append(getAuthorsMultipleEP(endpoint));

                    } catch (RepositoryException ex) {
                        log.error("Excepcion de repositorio. Problemas en conectarse a " + endpoint.getName());
                        java.util.logging.Logger.getLogger(AuthorServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (MalformedQueryException ex) {
                        log.error("Excepcion de forma de consulta. Revise consultas SPARQL y sintaxis. Revise estandar SPARQL");
                        java.util.logging.Logger.getLogger(AuthorServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (QueryEvaluationException ex) {
                        log.error("Excepcion de ejecucion de consulta. No se ha ejecutado la consulta general para la obtencion de los Authores.");
                        java.util.logging.Logger.getLogger(AuthorServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    someUpdate = true;
                }// end if endpoint.status == true
            }
            if (!someUpdate) {
                return "Any  Endpoints";
            }
            return response.toString();
        } else {
            return "No Endpoints";
        }

    }

    public String getAuthorsMultipleEP(SparqlEndpoint endpoint) throws DaoException, UpdateException, RepositoryException, MalformedQueryException, QueryEvaluationException {
        int tripletasCargadas = 0; //cantidad de tripletas actualizadaas
        int contAutoresNuevosNoCargados = 0; //cantidad de actores nuevos no cargados
        int contAutoresNuevosEncontrados = 0; //hace referencia a la cantidad de actores existentes en el archivo temporal antes de la actualizacion
        configurationService.getHome();
        String lastUpdateUrisFile = configurationService.getHome() + "\\listAuthorsUpdate_" + endpoint.getName() + ".aut";
        /* Conecting to repository using LDC ( Linked Data Client ) Library */
        ClientConfiguration config = new ClientConfiguration();
        config.addEndpoint(new SPARQLEndpoint(endpoint.getName(), endpoint.getEndpointUrl(), "^" + "http://" + ".*"));
        LDClientService ldClientEndpoint = new LDClient(config);

        Repository endpointTemp = new SPARQLRepository(endpoint.getEndpointUrl());
        endpointTemp.initialize();
        //After that you can use the endpoint like any other Sesame Repository, by creating a connection and doing queries on that:
        RepositoryConnection conn = endpointTemp.getConnection();
        String querytoCount = "";
        querytoCount = queriesService.getCountPersonQuery(endpoint.getGraph());
        TupleQueryResult countPerson = conn.prepareTupleQuery(QueryLanguage.SPARQL, querytoCount).evaluate();
        BindingSet bindingCount = countPerson.next();
        int allPersons = Integer.parseInt(bindingCount.getValue("count").stringValue());
        //Query that let me obtain all resource related with author from source sparqlendpoint 
        String getAuthorsQuery = queriesService.getAuthorsQuery(endpoint.getGraph());
        String resource = "";
        for (int offset = 0; offset < allPersons; offset += 5000) {
            try {
                TupleQueryResult authorsResult = conn.prepareTupleQuery(QueryLanguage.SPARQL, getAuthorsQuery + getLimitOffset(limit, offset)).evaluate();
                while (authorsResult.hasNext()) {
                    BindingSet binding = authorsResult.next();
                    resource = String.valueOf(binding.getValue("s"));
                    try {
                        if (!sparqlFunctionsService.askAuthor(queriesService.getAskResourceQuery(constantService.getAuthorsGraph(), resource))) {
                            contAutoresNuevosEncontrados++;
                            printPercentProcess(contAutoresNuevosEncontrados, allPersons, endpoint.getName());
                            //properties and values quering with LDClient Library de Marmotta
                            String getResourcePropertyQuery = "";
                            try {
                                ClientResponse respUri = ldClientEndpoint.retrieveResource(utf8DecodeQuery(resource));
                                RepositoryConnection conUri = ModelCommons.asRepository(respUri.getData()).getConnection();
                                conUri.begin();
                                // SPARQL to get all data of a Resource
                                getResourcePropertyQuery = queriesService.getRetrieveResourceQuery();
                                TupleQuery resourcequery = conUri.prepareTupleQuery(QueryLanguage.SPARQL, getResourcePropertyQuery); //
                                TupleQueryResult tripletasResult = resourcequery.evaluate();
                                provenanceinsert = false;
                                while (tripletasResult.hasNext()) {
                                    //obtengo name, lastname, firstname, type, etc.,   para formar tripletas INSERT
                                    BindingSet tripletsResource = tripletasResult.next();
                                    String sujeto = tripletsResource.getValue("x").toString();
                                    String predicado = tripletsResource.getValue("y").toString();
                                    String objeto = tripletsResource.getValue("z").toString();
                                    ///insert sparql query,
                                    tripletasCargadas = tripletasCargadas + executeInsertQuery(sujeto, predicado, objeto, endpoint, provenanceinsert);
                                }
                                conUri.commit();
                                conUri.close();
                            } catch (QueryEvaluationException ex) {
                                log.error("Al evaluar la consulta: " + getResourcePropertyQuery);
                                //java.util.logging.Logger.getLogger(AuthorServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (DataRetrievalException ex) {
                                contAutoresNuevosNoCargados++;
                                //log.error("Al recuperar datos del recurso : " + resource);
                                //java.util.logging.Logger.getLogger(AuthorServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }//end  if (!sparqlFunctionsService.askAuthor( ... )
                    } catch (AskException ex) {
                        log.error("Failure to ask existence of: " + resource);
                    }
                }// while (authorsResult.hasNext()) 
            } catch (QueryEvaluationException ex) {
                log.error("Fallo consulta ASK de:  " + resource+ "error"+ ex.getMessage());
            }

        }//END FOR   Obteniedo resultados de acuerdo a LIMIT y OFFSET
                /*    
         *    @deprecated
         *    ESCRIBIENDO URIS DE AUTORES EN ARCHIVO TEMPORAL
         *    @param conn, conection endpoint and configuration
         *    @param query, query to obtain all resource uris of authors
         *    @param lastUpdateUrisFile path of temporal file to save last uris update   */
        sparqlFunctionsService.updateLastAuthorsFile(conn, getAuthorsQuery, lastUpdateUrisFile);
        ldClientEndpoint.shutdown();
        log.info(endpoint.getName() + " endpoint. Se detectaron " + contAutoresNuevosEncontrados + " autores nuevos ");
        log.info(endpoint.getName() + " endpoint. Se cargaron " + (contAutoresNuevosEncontrados - contAutoresNuevosNoCargados) + " autores nuevos exitosamente");
        log.info(endpoint.getName() + " endpoint. Se cargaron " + tripletasCargadas + " tripletas ");
        log.info(endpoint.getName() + " endpoint. No se pudieron cargar " + contAutoresNuevosNoCargados + " autores");
        conn.close();
        return "Carga Finalizada. Revise Archivo Log Para mas detalles";
    }

    public void insertKeywordsAndAbstractValues(String publication, String author, SparqlEndpoint endpoint) throws DataRetrievalException, QueryEvaluationException, RepositoryException, MalformedQueryException, IOException, ClassNotFoundException {
        ClientConfiguration config = new ClientConfiguration();
        config.addEndpoint(new SPARQLEndpoint(endpoint.getName(), endpoint.getEndpointUrl(), "^" + "http://" + ".*"));
        LDClientService ldClientEndpoint = new LDClient(config);
        String getRetrieveKeysQuery = "";
            ClientResponse respPub = ldClientEndpoint.retrieveResource(utf8DecodeQuery(publication));
            RepositoryConnection conUriPub = ModelCommons.asRepository(respPub.getData()).getConnection();
            conUriPub.begin();
            // SPARQL to get all data of a Resource
            getRetrieveKeysQuery = queriesService.getRetrieveKeysQuery();
            TupleQuery keysquery = conUriPub.prepareTupleQuery(QueryLanguage.SPARQL, getRetrieveKeysQuery); //
            TupleQueryResult tripletaskeysResult = keysquery.evaluate();
            while (tripletaskeysResult.hasNext()) {
                BindingSet tripletskeysResource = tripletaskeysResult.next();
                String subjectproperty = tripletskeysResource.getValue("y").toString();
                String keyword = tripletskeysResource.getValue("z").toString();
               //only insert Literal Subjects
                if ((!commonsService.isURI(keyword)) && (kservice.isValidKeyword(keyword))) {
                    executeInsertQuery(author, subjectproperty, kservice.cleaningText(keyword).toUpperCase(), endpoint, provenanceinsert);
                }
            }
            String getAbstractAndTitleQuery = queriesService.getAbstractAndTitleQuery(publication);
            TupleQuery abstractTitlequery = conUriPub.prepareTupleQuery(QueryLanguage.SPARQL, getAbstractAndTitleQuery); //
            TupleQueryResult tripletasATResult = abstractTitlequery.evaluate();
            while (tripletasATResult.hasNext()) {
                BindingSet tripletsATResource = tripletasATResult.next();
                StringBuilder textAnalized = new StringBuilder();
                if (tripletsATResource.getValue("title") != null) {
                    String title = tripletsATResource.getValue("title").toString();
                    textAnalized.append(title);
                }
                if (tripletsATResource.getValue("abstract") != null) {
                    String abstractvalue = tripletsATResource.getValue("abstract").toString();
                    executeInsertQuery(author, "dct:description", abstractvalue, endpoint, provenanceinsert);
                    textAnalized.append(abstractvalue);
                }
                if (tripletsATResource.getValue("description") != null)
                {
                    String abstractvalue = tripletsATResource.getValue("description").toString();
                    executeInsertQuery(author, "dct:description", abstractvalue, endpoint, provenanceinsert);
                    textAnalized.append(abstractvalue);
                }    
                /*insert keywords from the abstract of publications from dspace*/
                for (String keywordfromAbstract : kservice.getKeywords(textAnalized.toString())) {
                    executeInsertQuery(author, "dct:subject","\"" +  keywordfromAbstract.toUpperCase() + "\"", endpoint, provenanceinsert);
                }
            }
            conUriPub.commit();
            conUriPub.close();
        
    }

    public int executeInsertQuery(String sujeto, String predicado, String objeto, SparqlEndpoint endpoint, boolean provenanceinsert) throws DataRetrievalException, QueryEvaluationException {
        ///insert sparql query,
        if (!predicado.contains(authorDocumentProperty)) {
            //insert provenance triplet query
            if (!provenanceinsert) {
                String provenanceQueryInsert = buildInsertQuery(constantService.getAuthorsGraph(), sujeto, "http://purl.org/dc/terms/provenance", endpoint.getResourceId());
                updateAuthor(provenanceQueryInsert);
                provenanceinsert = true;
            }
            String queryAuthorInsert = buildInsertQuery(constantService.getAuthorsGraph(), sujeto, predicado, objeto);
            //load data related with author
            updateAuthor(queryAuthorInsert);
            return 1;
        } else {
            try {
                insertKeywordsAndAbstractValues(objeto, sujeto, endpoint);
            } catch (RepositoryException | MalformedQueryException | IOException | ClassNotFoundException ex) {
                java.util.logging.Logger.getLogger(AuthorServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return 0;
    }

    /*
     * 
     * @param contAutoresNuevosEncontrados
     * @param allPersons
     * @param endpointName 
     */
    public void printPercentProcess(int contAutoresNuevosEncontrados, int allPersons, String endpointName) {

        if ((contAutoresNuevosEncontrados * 100 / allPersons) != processpercent) {
            processpercent = contAutoresNuevosEncontrados * 100 / allPersons;
            log.info("Procesado el: " + processpercent + " % del Endpoint: " + endpointName);
        }
    }

    public String getLimitOffset(int limit, int offset) {
        return " " + queriesService.getLimit(String.valueOf(limit)) + " " + queriesService.getOffset(String.valueOf(offset));
    }

    /*
     *   ASK - with SPARQL MODULE, to check if the resource already exists in kiwi triple store
     *   
     */
    public Boolean askAuthor(String querytoAsk) throws AskException {
        return sparqlFunctionsService.askAuthor(querytoAsk);
    }

    /*
     *   UPDATE - with SPARQL MODULE, to check if the resource already exists in kiwi triple store
     *   
     */
    public String updateAuthor(String querytoUpdate) {
        try {
            sparqlFunctionsService.updateAuthor(querytoUpdate);
            return "Correcto";
        } catch (UpdateException ex) {
            log.error("Error al intentar cargar al Autor" + querytoUpdate);
            java.util.logging.Logger.getLogger(AuthorServiceImpl.class.getName()).log(Level.SEVERE, null, ex);

        }
        return "Error" + querytoUpdate;
    }
    
    @Override
    public String saveAuthorFromFile(String... args) {
        String output = "";
        String graphUri = args[0];
        //String university = args[1];
        String name = args[2];
        String surname = args[3];
        String keywords = null;
        if (args[4] != null) {
            keywords = args[4];
        }
        
        String sujeto = "http://ucuenca.edu.ec/resource/author/" + (surname.replace(" ", "_").toUpperCase() + "__" + name.replace(" ", "_").toUpperCase()).replace(".", "");
        
        //Type rdf:type person
        String queryAuthorProvenanceInsert = buildInsertQuery(constantService.getAuthorsGraph(), sujeto, "dct:provenance", graphUri);
        updateAuthor(queryAuthorProvenanceInsert);

        //Type rdf:type person
        String queryAuthorTypeInsert = buildInsertQuery(constantService.getAuthorsGraph(), sujeto, "rdf:type", "http://xmlns.com/foaf/0.1/Person");
        updateAuthor(queryAuthorTypeInsert);
        
        //FirstName
        String queryAuthorNameInsert = buildInsertQuery(constantService.getAuthorsGraph(), sujeto, "foaf:firstName ", " " + name + " ");
        updateAuthor(queryAuthorNameInsert);
        
        //Lastname
        String queryAuthorSurnameInsert = buildInsertQuery(constantService.getAuthorsGraph(), sujeto, "foaf:lastName ", " " + surname + " ");
        updateAuthor(queryAuthorSurnameInsert);
        
        //FullName
        String queryAuthorFullNameInsert = buildInsertQuery(constantService.getAuthorsGraph(), sujeto, "foaf:name", " " + surname + ", " + name + " ");
        updateAuthor(queryAuthorFullNameInsert);
        
        //Source: File
        String queryAuthorSourceInsert = buildInsertQuery(constantService.getAuthorsGraph(), sujeto, "<http://ucuenca.edu.ec/ontology#source>", " file ");
        updateAuthor(queryAuthorSourceInsert);
        
        //Subjects Optional
        if (keywords != null) {
            String[] subjects = keywords.split(";");
            for (String subject : subjects) {
                String querySubjectInsert = buildInsertQuery(constantService.getAuthorsGraph(), sujeto, "dct:subject", " " + subject + " ");
                updateAuthor(querySubjectInsert);
            }
        }        
        
        return output;
    }

    /*
     *   ASK Query, to check if the resource already exists in temporal file  *.aut
     *   
     */
//    public Boolean existeAuthor(List listURIS, String acthorURIfromEndpoint) {
//        String acthorURIfromLastUpdaFile = "";
//
//        if (listURIS != null) {
//            for (Iterator it = listURIS.iterator(); it.hasNext();) {
//                acthorURIfromLastUpdaFile = it.next().toString();
//                //    String individualURI = listURIS.listIterator().next().toString();
//                if (acthorURIfromLastUpdaFile.equals(acthorURIfromEndpoint))//elemento existente
//                {
//                    return true;//la uri del author existe
//                }
//            }
//        }
//        return false; //no se ha enocntrado la uri del author, es un author nuevo
//    }
    //construyendo sparql query insert 
    public String buildInsertQuery(String... args) {
        String graph = args[0];
        String sujeto = args[1];
        String predicado = args[2];
        String objeto = args[3];
        if (commonsService.isURI(objeto)) {
            return queriesService.getInsertDataUriQuery(graph, sujeto, predicado, objeto);
        } else {
            return queriesService.getInsertDataLiteralQuery(graph, sujeto, predicado, objeto);
        }
    }

    /**
     * Permite decodificar la uri formato UTF-8
     *
     * @param query
     * @return
     */
    private String utf8DecodeQuery(String query) {
        try {
            byte[] bytes = query.getBytes("UTF-8"); // Charset to encode into
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            java.util.logging.Logger.getLogger(AuthorServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
