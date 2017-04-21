/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.marmotta.ucuenca.wk.commons.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.slf4j.Logger;
import javax.inject.Inject;
import org.apache.marmotta.platform.core.exception.MarmottaException;
import org.apache.marmotta.platform.sparql.api.sparql.SparqlService;
import org.apache.marmotta.ucuenca.wk.commons.service.CommonsServices;
import org.apache.marmotta.ucuenca.wk.commons.service.ConstantService;
import org.apache.marmotta.ucuenca.wk.commons.service.DistanceService;
import org.apache.marmotta.ucuenca.wk.commons.service.GetAuthorsGraphData;
import org.apache.marmotta.ucuenca.wk.commons.service.QueriesService;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.FOAF;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.UpdateExecutionException;

/**
 *
 * @author Fernando Baculima
 */
public class GetAuthorsGraphDataImpl implements GetAuthorsGraphData {

    private QueriesService queriesService = new QueriesServiceImpl();

    private CommonsServices commonService = new CommonsServicesImpl();

    @Inject
    private ConstantService constantService;

    private DistanceService distanceService = new DistanceServiceImpl();

    @Inject
    private SparqlService sparqlService;

    @Inject
    private Logger log;
    private String propertiesFile = "parameters.properties";

    /**
     * Retorna la lista de autores (Desde el grafo de autores ) que seran
     * procesados (Buscados en las Fuentes de publicaciones)
     *
     * @return
     */
    @Override
    public List<Map<String, Value>> getListOfAuthors() {

        List<Map<String, Value>> resultAllAuthors = null;
        List<Map<String, Value>> resultFilterAuthors = new LinkedList<Map<String, Value>>();

        String fichero = "";
        double valueDistance;
        InputStream inputStream = null;
        String dspaceName = "";
        String fileName = "";
        try {
            String getAllAuthorsDataQuery = queriesService.getAuthorsDataQuery(constantService.getAuthorsGraph(), constantService.getEndpointsGraph());
            resultAllAuthors = sparqlService.query(QueryLanguage.SPARQL, getAllAuthorsDataQuery);

            /**
             * Filter by Specific List of Authors
             */
            boolean filterByList = Boolean.parseBoolean(commonService.readPropertyFromFile(propertiesFile, "filterByList"));

            /**
             * the format of the names in the file should be {LastName
             * FirstName}
             */
            if (filterByList) {
                fichero = commonService.readPropertyFromFile(propertiesFile, "fileToFilter"); //Formato:  {1Nombre+1Apellido}
                valueDistance = Double.parseDouble(commonService.readPropertyFromFile(propertiesFile, "valueDistanceComparation")); //Formato:  {1Nombre+1Apellido}

                //Get file from resources folder
                ClassLoader classLoader = this.getClass().getClassLoader();
                inputStream = classLoader.getResourceAsStream(fichero);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line = reader.readLine();
                while (line != null) {
                    fileName = getNameFromString(line, 4);
                    Iterator<Map<String, Value>> iter = resultAllAuthors.iterator();
                    boolean found = false;
                    String authorResource = "";
                    while (iter.hasNext()) {
                        Map<String, Value> element = iter.next();
                        String firstName = element.get("fname").stringValue();
                        String lastName = element.get("lname").stringValue();
                        authorResource = element.get("subject").stringValue();

                        dspaceName = firstName + " " + lastName;
                        //log.info("Analyzed names - fileName: " + fileName + " - dspaceName: " + dspaceName);
                        if (distanceService.cosineSimilarityAndLevenshteinDistance(fileName.split(":")[0], firstName) > valueDistance && distanceService.cosineSimilarityAndLevenshteinDistance(fileName.split(":")[1], lastName) > valueDistance) {
//                            resultFilterAuthors.add(element);
                            found = true;
                            break;
                        }

                    }
                    //Update search parameters of author or insert new author.
                    authorUpdate(authorResource, fileName, resultFilterAuthors, found);

                    try {
                        line = reader.readLine();
                    } catch (Exception e) {
                        line = "";
                        log.error("finished reading names from filterByList File ");
                    }
                }
                reader.close();
                inputStream.close();
                return resultFilterAuthors;
            }
        } catch (MarmottaException ex) {
            log.error("MarmottaException in GetListOfAuthors" + ex);
        } catch (Exception ex) {
            log.error("Exception fileName: " + fileName + " - " + " dspaceName: " + dspaceName);
            log.error("Exception reading file " + fichero + ": " + ex);
        }

        return resultAllAuthors;
    }

    /**
     *
     * Format 1: {1LastName 1FirstName}. Example Perez Juan | Format 2:
     * {1LastName 1FirstName 2FirstName}. Example Perez Juan Carlos | Format 3:
     * ...
     *
     * @param line String name of the person who comes from the file
     * @param format int format of line param
     * @return fullLastName:fullFirstName
     */
    public String getNameFromString(String line, int format) {
        String firstNameFile = "";
        String lastNameFile = "";
        String fileName = line;

        try {
            switch (format) {
                case 1: {
                    if (line.length() > format) {
                        firstNameFile = line.split(" ")[0];
                        lastNameFile = line.split(" ")[1];
                        fileName = lastNameFile + ":" + firstNameFile;
                    }
                    break;
                }
                case 2: { //change split format
                    firstNameFile = line.split(" ")[0];
                    lastNameFile = line.split(" ")[1];
                    fileName = lastNameFile + ":" + firstNameFile;
                    break;
                }
                case 3: {//change split format
                    firstNameFile = line.split(" ")[0];
                    lastNameFile = line.split(" ")[1];
                    fileName = lastNameFile + ":" + firstNameFile;
                    break;
                }
                case 4: {//change split format

                    fileName = line;
                    break;
                }

                default: {
                    firstNameFile = line.split(" ")[0];
                    lastNameFile = line.split(" ")[1];
                    fileName = lastNameFile + ":" + firstNameFile;
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Problems split name " + firstNameFile + " - " + lastNameFile + " from filter line. ");
        }
        return fileName;
    }

    /**
     * @See Build a new insert query.
     * @param args
     * @return
     */
    public String buildInsertQuery(String... args) {
        if (commonService.isURI(args[3])) {
            return queriesService.getInsertDataUriQuery(args[0], args[1], args[2], args[3]);
        } else {
            return queriesService.getInsertDataLiteralQuery(args[0], args[1], args[2], args[3]);
        }
    }

    /**
     * @See Insert a new author or update foaf:nick used in providers modules.
     * @param authorResource
     * @param fileName
     * @param list
     * @param found
     */
    public void authorUpdate(String authorResource, String fileName, List<Map<String, Value>> list, boolean found) {
        List<Map<String, Value>> resultNewAllAuthors = new LinkedList<Map<String, Value>>();
        try {
            if (!found) {
                String getNewAuthorsDataQuery = queryInsertNewAutor(fileName);
                resultNewAllAuthors = sparqlService.query(QueryLanguage.SPARQL, getNewAuthorsDataQuery);
            } else {

                insertSearchName(authorResource, fileName);
                String getNewAuthorsDataQuery = queriesService.getAuthorsDataQueryByUri(constantService.getAuthorsGraph(), constantService.getEndpointsGraph(), authorResource);
                resultNewAllAuthors = sparqlService.query(QueryLanguage.SPARQL, getNewAuthorsDataQuery);
            }
            list.addAll(resultNewAllAuthors);

        } catch (MarmottaException ex) {
            java.util.logging.Logger.getLogger(GetAuthorsGraphDataImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @See Get value from triple store.
     *
     * @param query
     * @param property
     * @return
     */
    public String getValueFromTripleStore(String query, String property) {
        try {
            String valueReturn = "";
            List<Map<String, Value>> endPoint = sparqlService.query(QueryLanguage.SPARQL, query);
            Iterator<Map<String, Value>> iter = endPoint.iterator();

            while (iter.hasNext()) {
                Map<String, Value> element = iter.next();
                valueReturn = element.get(property).stringValue();
            }
            return valueReturn;
        } catch (MarmottaException ex) {
            java.util.logging.Logger.getLogger(GetAuthorsGraphDataImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * @See Insert new author in tripleStore
     *
     * @param fileName
     * @return
     */
    public String queryInsertNewAutor(String fileName) {
        String authorName = fileName.replace(" ", "_").replace(":", "_");

        //Endpoint URI by name parameter.
        String endpoitName = commonService.readPropertyFromFile(propertiesFile, "nameEndPointFilter");
        String queryGetUriEndpoint = queriesService.getEndPointUriByName(endpoitName);
        String uriEndpointValue = getValueFromTripleStore(queryGetUriEndpoint, "object");

        //Endpoint graph by uri parameter.
        String queryGetGraphEndpoint = queriesService.getEndpointByIdQuery(constantService.getEndpointsGraph(), uriEndpointValue);
        String getGraphEndpoint = getValueFromTripleStore(queryGetGraphEndpoint, "graph");
        String prefixProvenance = getGraphEndpoint + "/contribuyente/";

        //insert new author.
        insertTriple(constantService.getAuthorsGraph(), prefixProvenance + authorName.toUpperCase(), RDF.TYPE.toString(), FOAF.PERSON.toString());
        insertTriple(constantService.getAuthorsGraph(), prefixProvenance + authorName.toUpperCase(), FOAF.NAME.toString(), " " + fileName.replace(":", " ") + " ");
        insertTriple(constantService.getAuthorsGraph(), prefixProvenance + authorName.toUpperCase(), FOAF.FIRST_NAME.toString(), " " + fileName.split(":")[0] + " ");
        insertTriple(constantService.getAuthorsGraph(), prefixProvenance + authorName.toUpperCase(), FOAF.LAST_NAME.toString(), " " + fileName.split(":")[1] + " ");
        insertTriple(constantService.getAuthorsGraph(), prefixProvenance + authorName.toUpperCase(), "http://purl.org/dc/terms/provenance", uriEndpointValue);
        insertTriple(constantService.getAuthorsGraph(), prefixProvenance + authorName.toUpperCase(), FOAF.NICK.toString(), " " + fileName.replace(":", " ") + " ");
        return queriesService.getAuthorsDataQueryByUri(constantService.getAuthorsGraph(), constantService.getEndpointsGraph(), prefixProvenance + authorName.toUpperCase());

    }

    /**
     * See Method to insert a triple in TripleStore.
     *
     * @param args
     */
    public void insertTriple(String... args) {

        String triple = buildInsertQuery(args[0], args[1], args[2], args[3]);

        try {
            sparqlService.update(QueryLanguage.SPARQL, triple);

        } catch (MalformedQueryException ex) {
            log.error("Malformed Query:  " + triple);
        } catch (UpdateExecutionException ex) {
            log.error("Update Query:  " + triple);
        } catch (MarmottaException ex) {
            log.error("Marmotta Exception:  " + triple);
        } catch (Exception ex) {

        }
    }

    /**
     * @See Insert foaf:nick like a name to search in external sources.
     * @param authorResource
     * @param fileName
     */
    private void insertSearchName(String authorResource, String fileName) {
        insertTriple(constantService.getAuthorsGraph(), authorResource, FOAF.NICK.toString(), " " + fileName.replace(":", " ") + " ");
    }

}
