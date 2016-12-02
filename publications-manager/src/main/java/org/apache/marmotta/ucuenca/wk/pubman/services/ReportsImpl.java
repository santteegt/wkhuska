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

import ar.com.fdvs.dj.domain.constants.Font;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JsonDataSource;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.apache.marmotta.platform.sparql.api.sparql.SparqlService;
import org.apache.marmotta.ucuenca.wk.commons.service.ConstantService;
import org.apache.marmotta.ucuenca.wk.commons.service.QueriesService;

import org.apache.marmotta.ucuenca.wk.pubman.api.SparqlFunctionsService;

import org.apache.marmotta.ucuenca.wk.commons.impl.ConstantServiceImpl;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;

import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.apache.marmotta.ucuenca.wk.pubman.api.ReportsService;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.sparql.SPARQLRepository;

/**
 *
 * @author Jose Luis Cullcay
 *
 */
@ApplicationScoped
public class ReportsImpl implements ReportsService {

    @Inject
    private Logger log;
    @Inject
    private QueriesService queriesService;
    @Inject
    private ConstantService pubVocabService;
    @Inject
    private SparqlFunctionsService sparqlFunctionsService;
    @Inject
    private SparqlService sparqlService;

    protected String TEMP_PATH = "./../research_webapps/ROOT/tmp";
    protected String REPORTS_FOLDER = "./../research_webapps/ROOT/reports/";
    protected ConstantServiceImpl constant = new ConstantServiceImpl();

    @Override
    public String createReport(String hostname, String realPath, String name, String type, List<String> params) {

        TEMP_PATH = realPath + "/tmp";
        REPORTS_FOLDER = realPath + "/reports/";
        // Make sure the output directory exists.
        File outDir = new File(TEMP_PATH);
        outDir.mkdirs();
        //Name of the file
        SimpleDateFormat format = new SimpleDateFormat("ddMMyyyy_HHmmss");
        String nameFile = name + "_" + format.format(new Date());
        String pathFile = TEMP_PATH + "/" + nameFile + "." + type;
        try {
            // Compile jrxml file.
            JasperReport jasperReport = JasperCompileManager
                    .compileReport(REPORTS_FOLDER + name + ".jrxml");
            //String array with the json string and other parameters required for the report
            String[] json = null;
            //Datasource
            JsonDataSource dataSource = null;
            // Parameters for report
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("rutalogo", realPath + "/wkhome/images/logo_wk.png");
            InputStream stream;

            //Parameters for each report
            switch (name) {
                case "ReportAuthor":
                    // Get the Json with the list of publications, the name of the researcher and the number of publications.
                    json = getJSONAuthor(params.get(0), hostname);

                    parameters.put("name", json[1]);
                    parameters.put("numero", json[2]);
                    break;
                case "ReportAuthorCluster":
                    // Get the Json with the list of publications, the name of the researcher and the number of publications.
                    json = getJSONAuthorsCluster(params.get(0), hostname);

                    parameters.put("name", json[1]);
                    parameters.put("numero", json[2]);
                    break;
                case "ReportStatistics":
                case "ReportStatisticsPub":
                case "ReportStatisticsRes":
                    // Get the Json with the list of publications, the number of researchers and the number of publications.
                    json = getJSONStatistics(hostname);
                    break;
                case "ReportStatisticsTopResU":
                    // Get the Json with the top researchers per university (considering the number of publications).
                    json = getJSONTopResearchersUnis(hostname);

                    break;
                case "ReportPublicationsByKeyword":
                    // Get the Json with the publications related to a keyword.
                    json = getJSONPublicationsByKeyword(params.get(0), hostname);

                    parameters.put("keyword", json[1]);
                    parameters.put("numero", json[2]);

                    break;
                case "ReportPublicationsByAuthor":
                    // Get the Json with the Authors by Area.
                    json = getJSONReportPublicationsByAuthor(params.get(0), hostname);

                    parameters.put("name", json[1]);
                    parameters.put("numero", json[2]);

                    break;
                case "ReportResearchersbyIES":
                    // Get the Json with the all authors from an specific IES.
                    json = getJSONAuthorsbyIES(params.get(0), hostname);

                    parameters.put("universityFullName", json[1]);
                    parameters.put("universityName", params.get(0));
                    parameters.put("totalAuthors", json[2]);

                    break;
                case "ReportStatisticsKeywords":
                    // Get the Json with the top keywords (considering the number of publications).
                    json = getJSONStatisticsTopKeywords(hostname);

                    break;   
            }
            //Always the first element of the array has the json stream
            stream = new ByteArrayInputStream(json[0].getBytes("UTF-8"));
            dataSource = new JsonDataSource(stream);

            if (dataSource != null) {
                JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

                if (type.equals("pdf")) {
                    // 1 - Export to pdf 
                    if (jasperPrint != null) {
                        JasperExportManager.exportReportToPdfFile(jasperPrint, pathFile);
                    }

                } else if (type.equals("xls")) {
                    // 2- Export to Excel sheet
                    JRXlsExporter exporter = new JRXlsExporter();

                    List<JasperPrint> jasperPrintList = new ArrayList<>();
                    jasperPrintList.add(jasperPrint);

                    exporter.setExporterInput(SimpleExporterInput.getInstance(jasperPrintList));
                    exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(pathFile));

                    exporter.exportReport();

                }
                // Return the relative online path for the report
                return "/tmp/" + nameFile + "." + type;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

        return "";
    }

    /**
     * Extract Json with publications of an author
     *
     * @param author Author id
     * @param hostname
     * @return
     */
    public String[] getJSONAuthor(String author, String hostname) {
        String getQuery = "";
        try {
            //Variables to return with name and number of publications
            String name = "";
            Integer cont = 0;
            //Query
            getQuery = ConstantServiceImpl.PREFIX
                    + " SELECT DISTINCT ?name ?title ?abstract ?authorsName WHERE { "
                    + "  <" + author + "> foaf:name ?name. "
                    + "  <" + author + "> foaf:publications  ?publications. "
                    + "  ?publications dct:title ?title. "
                    + "  OPTIONAL {?publications bibo:abstract ?abstract} "
                    + "  ?authors foaf:publications ?publications. "
                    + "  ?authors foaf:name ?authorsName. "
                    + "}";

            log.info("Buscando Informacion de: " + author);
            Repository repo = new SPARQLRepository(hostname + "/sparql/select");
            repo.initialize();
            RepositoryConnection con = repo.getConnection();
            try {
                // perform operations on the connection
                TupleQueryResult resulta = con.prepareTupleQuery(QueryLanguage.SPARQL, getQuery).evaluate();

                //JSONObject authorJson = new JSONObject();
                Map<String, JSONObject> pubMap = new HashMap<String, JSONObject>();
                Map<String, JSONArray> coautMap = new HashMap<String, JSONArray>();
                JSONArray publications = new JSONArray();

                JSONObject coauthors = new JSONObject();
                while (resulta.hasNext()) {
                    BindingSet binding = resulta.next();
                    name = String.valueOf(binding.getValue("name")).replace("\"", "").replace("^^", "").split("<")[0];
                    //authorJson.put("name", name);
                    String pubTitle = String.valueOf(binding.getValue("title")).replace("\"", "").replace("^^", "").split("<")[0];
                    if (!pubMap.containsKey(pubTitle)) {
                        pubMap.put(pubTitle, new JSONObject());
                        pubMap.get(pubTitle).put("title", pubTitle);
                        cont++;
                        if (binding.getValue("abstract") != null) {
                            pubMap.get(pubTitle).put("abstract", String.valueOf(binding.getValue("abstract")).replace("\"", "").replace("^^", "").split("<")[0]);
                        }
                        //Coauthors
                        coautMap.put(pubTitle, new JSONArray());
                        coautMap.get(pubTitle).add(String.valueOf(binding.getValue("authorsName")).replace("\"", "").replace("^^", "").split("<")[0]);
                        //pubMap.get(pubTitle).put("title", pubTitle);
                    } else {
                        coautMap.get(pubTitle).add(String.valueOf(binding.getValue("authorsName")).replace("\"", "").replace("^^", "").split("<")[0]);
                    }
                }

                for (Map.Entry<String, JSONObject> pub : pubMap.entrySet()) {
                    pub.getValue().put("coauthors", coautMap.get(pub.getKey()));
                    publications.add(pub.getValue());
                }
                //authorJson.put("publications", publications);
                con.close();
                //return new String[] {authorJson.toJSONString(), publications.toString(), authorJson.get("name").toString(), cont.toString()};
                return new String[]{publications.toString(), name, cont.toString()};
            } catch (RepositoryException ex) {
                java.util.logging.Logger.getLogger(ReportsImpl.class.getName()).log(Level.SEVERE, null, ex);
                con.close();
            }

            return new String[]{"", ""};
        } catch (MalformedQueryException | QueryEvaluationException | RepositoryException ex) {
            java.util.logging.Logger.getLogger(ReportsImpl.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
        return new String[]{"", ""};
    }

    /**
     * Extract Json with authors related through a cluster
     *
     * @param clusterId Id of the cluster
     * @param hostname Hostname
     * @return Array of strings
     */
    public String[] getJSONAuthorsCluster(String clusterId, String hostname) {
        String getQuery = "";
        try {
            //Variables to return with name and number of publications
            String name = "";
            Integer cont = 0;
            //Query
            getQuery = ConstantServiceImpl.PREFIX
                    + " SELECT DISTINCT ?cluster ?author ?keywords "
                    + "WHERE "
                    + "{ "
                    + "  graph <" + constant.getClusterGraph() + "> "
                    + "  { "
                    + "    <" + clusterId + "> uc:hasPerson ?subject. "
                    + "    <" + clusterId + "> rdfs:label ?cluster. "
                    + "    { "
                    + "    	select DISTINCT ?subject ?author ?keywords "
                    + "        where "
                    + "        { "
                    + "        	graph <" + constant.getWkhuskaGraph() + "> "
                    + "            { "
                    + "                 ?subject foaf:name ?author. "
                    + "                 ?subject foaf:publications ?publicationUri. "
                    + "                 ?publicationUri dct:title ?title. "
                    + "                 ?publicationUri bibo:Quote ?keywords. "
                    + "             } "
                    + "        } group by ?subject ?author ?keywords "
                    + "    }"
                    + "  }"
                    + "}";

            Repository repo = new SPARQLRepository(hostname + "/sparql/select");
            repo.initialize();
            RepositoryConnection con = repo.getConnection();
            try {
                // perform operations on the connection
                TupleQueryResult resulta = con.prepareTupleQuery(QueryLanguage.SPARQL, getQuery).evaluate();

                //JSONObject authorJson = new JSONObject();
                Map<String, JSONObject> autMap = new HashMap<String, JSONObject>();
                Map<String, JSONArray> keyMap = new HashMap<String, JSONArray>();
                JSONArray authors = new JSONArray();

                JSONObject coauthors = new JSONObject();
                while (resulta.hasNext()) {
                    BindingSet binding = resulta.next();
                    name = String.valueOf(binding.getValue("cluster")).replace("\"", "").replace("^^", "").split("<")[0];
                    String authorName = String.valueOf(binding.getValue("author")).replace("\"", "").replace("^^", "").split("<")[0];
                    if (!autMap.containsKey(authorName)) {
                        autMap.put(authorName, new JSONObject());
                        autMap.get(authorName).put("author", authorName);
                        if (binding.getValue("keywords") != null) {
                            autMap.get(authorName).put("keywords", String.valueOf(binding.getValue("keywords")).replace("\"", ""));
                        }
                        //Keywords
                        keyMap.put(authorName, new JSONArray());
                        keyMap.get(authorName).add(String.valueOf(binding.getValue("keywords")).replace("\"", "").replace("^^", ""));
                    } else {
                        keyMap.get(authorName).add(String.valueOf(binding.getValue("keywords")).replace("\"", "").replace("^^", ""));
                    }
                }

                for (Map.Entry<String, JSONObject> aut : autMap.entrySet()) {
                    aut.getValue().put("keywords", keyMap.get(aut.getKey()));
                    authors.add(aut.getValue());
                }
                con.close();
                //Number of authors
                cont = autMap.size();
                return new String[]{authors.toString(), name, cont.toString()};
            } catch (RepositoryException ex) {
                java.util.logging.Logger.getLogger(ReportsImpl.class.getName()).log(Level.SEVERE, null, ex);
                con.close();
            }

            return new String[]{"", ""};
        } catch (MalformedQueryException | QueryEvaluationException | RepositoryException ex) {
            java.util.logging.Logger.getLogger(ReportsImpl.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
        return new String[]{"", ""};
    }

    /**
     * Retrieve Json with statistics about universities, their authors and
     * publications
     *
     * @param hostname Hostname
     * @return Array of strings
     */
    public String[] getJSONStatistics(String hostname) {
        String getQuery = "";
        try {

            //Query
            getQuery = ConstantServiceImpl.PREFIX
                    + " SELECT ?provenance ?name (COUNT(DISTINCT(?s)) AS ?total) (count(DISTINCT ?pub) as ?totalp) "
                    + " WHERE "
                    + "    { "
                    + "    	GRAPH <" + constant.getWkhuskaGraph() + "> { "
                    + "          ?s a foaf:Person. "
                    + "          ?s foaf:publications ?pub . "
                    + "          ?s dct:provenance ?provenance . "
                    + "          { "
                    + "              SELECT ?name "
                    + "              WHERE { "
                    + "                  GRAPH <" + constant.getEndpointsGraph() + "> { "
                    + "                       ?provenance uc:fullName ?name . "
                    + "                       FILTER (lang(?name) = \"es\")."
                    + "                  } "
                    + "              } "
                    + "          } "
                    + "    	} "
                    + "  	} GROUP BY ?provenance ?name ";

            Repository repo = new SPARQLRepository(hostname + "/sparql/select");
            repo.initialize();
            RepositoryConnection con = repo.getConnection();
            try {
                // perform operations on the connection
                TupleQueryResult resulta = con.prepareTupleQuery(QueryLanguage.SPARQL, getQuery).evaluate();

                JSONObject uni;
                JSONArray universities = new JSONArray();

                while (resulta.hasNext()) {
                    BindingSet binding = resulta.next();
                    uni = new JSONObject();
                    String uniName = String.valueOf(binding.getValue("name")).replace("\"", "").replace("^^", "").split("<")[0];
                    String totalAuthors = String.valueOf(binding.getValue("total")).replace("\"", "").replace("^^", "").split("<")[0];
                    String totalPubs = String.valueOf(binding.getValue("totalp")).replace("\"", "").replace("^^", "").split("<")[0];
                    uni.put("university", uniName);
                    uni.put("authors", totalAuthors);
                    uni.put("pubs", totalPubs);

                    universities.add(uni);
                }

                con.close();

                return new String[]{universities.toString()};
            } catch (RepositoryException ex) {
                java.util.logging.Logger.getLogger(ReportsImpl.class.getName()).log(Level.SEVERE, null, ex);
                con.close();
            }

            return new String[]{"", ""};
        } catch (MalformedQueryException | QueryEvaluationException | RepositoryException ex) {
            java.util.logging.Logger.getLogger(ReportsImpl.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
        return new String[]{"", ""};
    }

    public String[] getJSONTopResearchersUnis(String hostname) {
        String query1, query2 = "";

        try {
            query1 = ConstantServiceImpl.PREFIX
                    + "SELECT ?provenance ?uni "
                    + "WHERE "
                    + "{ "
                    + "  GRAPH <" + constant.getEndpointsGraph() + "> "
                    + "  { "
                    + "    ?provenance uc:fullName ?uni . "
                    + "     FILTER (lang(?uni) = \"es\")."
                    + "  }"
                    + "} ORDER BY ?uni";

            Repository repo = new SPARQLRepository(hostname + "/sparql/select");
            repo.initialize();
            RepositoryConnection con = repo.getConnection();

            try {
                // perform operations on the connection
                TupleQueryResult resultUnis = con.prepareTupleQuery(QueryLanguage.SPARQL, query1).evaluate();
                JSONArray authors = new JSONArray();
                //Check authors of each university
                while (resultUnis.hasNext()) {
                    BindingSet binding = resultUnis.next();
                    String uniId = String.valueOf(binding.getValue("provenance")).replace("\"", "").replace("^^", "").split("<")[0];
                    String uniName = String.valueOf(binding.getValue("uni")).replace("\"", "").replace("^^", "").split("<")[0];

                    query2 = ConstantServiceImpl.PREFIX
                            + "SELECT ?researcher (count(DISTINCT ?pub) as ?totalp) "
                            + "WHERE "
                            + "{ "
                            + "  GRAPH <" + constant.getWkhuskaGraph() + "> "
                            + "        { "
                            + "          ?s a foaf:Person. "
                            + "          ?s foaf:name ?researcher. "
                            + "          ?s foaf:publications ?pub . "
                            + "          ?s dct:provenance <" + uniId + "> "
                            + "        } "
                            + "} GROUP BY ?researcher ORDER BY DESC(?totalp) LIMIT 5";

                    JSONObject author;
                    // CONSULTA PARA OBTENER LOS CINCO INVESTIGADORES DE CADA U
                    TupleQueryResult resultAuthors = con.prepareTupleQuery(QueryLanguage.SPARQL, query2).evaluate();
                    Integer contPub = 0;
                    while (resultAuthors.hasNext()) {
                        BindingSet bind2 = resultAuthors.next();
                        contPub++;
                        //Form the Json object
                        author = new JSONObject();
                        String researcherName = String.valueOf(bind2.getValue("researcher")).replace("\"", "").replace("^^", "").split("<")[0];
                        String totalPubs = String.valueOf(bind2.getValue("totalp")).replace("\"", "").replace("^^", "").split("<")[0];
                        author.put("universityName", uniName);
                        author.put("numberResearcher", contPub);
                        author.put("name", researcherName);
                        author.put("numberPublications", totalPubs);

                        authors.add(author);
                    }

                }

                con.close();

                return new String[]{authors.toString()};

            } catch (RepositoryException ex) {
                java.util.logging.Logger.getLogger(ReportsImpl.class.getName()).log(Level.SEVERE, null, ex);
                con.close();
            }

            return new String[]{"", ""};

        } catch (MalformedQueryException | QueryEvaluationException | RepositoryException ex) {
            java.util.logging.Logger.getLogger(ReportsImpl.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
        return new String[]{"", ""};
    }

    /**
     * Extract Json with publications related by cluster
     *
     * @param keyword Keyword to search
     * @param hostname Hostname
     * @return Array of strings
     */
    public String[] getJSONPublicationsByKeyword(String keyword, String hostname) {
        String getQuery = "";
        try {
            //Variables to return with name and number of publications
            String id = "";
            String authors = "";
            String title = "";
            String abst = "";
            String uri = "";
            Integer cont = 0;
            //Query
            getQuery = ConstantServiceImpl.PREFIX
                    + "Select ?publicationUri (GROUP_CONCAT(distinct ?name;separator='; ') as ?names) ?title ?abstract ?uri "
                    + "WHERE "
                    + "{ "
                    + "  GRAPH <http://ucuenca.edu.ec/wkhuska> "
                    + "  { "
                    + "      ?subject foaf:publications ?publicationUri . "
                    + "      ?subject foaf:name ?name . "
                    + "      ?publicationUri dct:title ?title . "
                    + "      OPTIONAL{ ?publicationUri bibo:abstract  ?abstract. } "
                    + "      OPTIONAL{ ?publicationUri bibo:uri  ?uri. } "
                    + "      ?publicationUri bibo:Quote ?quote. "
                    + "      FILTER (mm:fulltext-search(?quote, '" + keyword + "' )) . "
                    + "      BIND(REPLACE( '" + keyword + "', ' ', '_', 'i') AS ?key) . "
                    + "      BIND(IRI(?key) as ?keyword) "
                    + "  } "
                    + "} group by ?publicationUri ?title ?abstract ?uri ";

            Repository repo = new SPARQLRepository(hostname + "/sparql/select");
            repo.initialize();
            RepositoryConnection con = repo.getConnection();
            try {
                // perform operations on the connection
                TupleQueryResult resulta = con.prepareTupleQuery(QueryLanguage.SPARQL, getQuery).evaluate();

                JSONArray publications = new JSONArray();
                JSONObject publication = new JSONObject();

                while (resulta.hasNext()) {
                    BindingSet binding = resulta.next();
                    //Form the Json object
                    publication = new JSONObject();

                    id = String.valueOf(binding.getValue("publicationUri")).replace("\"", "").replace("^^", "").split("<")[0];
                    authors = String.valueOf(binding.getValue("names")).replace("\"", "").replace("^^", "").split("<")[0];
                    title = String.valueOf(binding.getValue("title")).replace("\"", "").replace("^^", "").split("<")[0];
                    abst = String.valueOf(binding.getValue("abstract")).replace("\"", "").replace("^^", "").split("<")[0];
                    uri = String.valueOf(binding.getValue("uri")).replace("\"", "").replace("^^", "").split("<")[0];

                    publication.put("id", id);
                    publication.put("authors", authors);
                    publication.put("title", title);
                    publication.put("abstract", abst);
                    publication.put("uri", (uri == null || uri == "" || uri == "null") ? null : uri);

                    publications.add(publication);

                    /*name = String.valueOf(binding.getValue("publicationUri")).replace("\"", "").replace("^^", "").split("<")[0];
                    String authorName = String.valueOf(binding.getValue("author")).replace("\"", "").replace("^^", "").split("<")[0];
                    if (!autMap.containsKey(authorName)) {
                        autMap.put(authorName, new JSONObject());
                        autMap.get(authorName).put("author", authorName);
                        if (binding.getValue("keywords") != null) {
                            autMap.get(authorName).put("keywords", String.valueOf(binding.getValue("keywords")).replace("\"", ""));
                        }
                        //Keywords
                        keyMap.put(authorName, new JSONArray());
                        keyMap.get(authorName).add(String.valueOf(binding.getValue("keywords")).replace("\"", "").replace("^^", ""));
                    } else {
                        keyMap.get(authorName).add(String.valueOf(binding.getValue("keywords")).replace("\"", "").replace("^^", ""));
                    }*/
                }

                /*for (Map.Entry<String, JSONObject> aut: autMap.entrySet()) {
                    aut.getValue().put("keywords", keyMap.get(aut.getKey()));
                    authors.add(aut.getValue());
                }*/
                con.close();
                //Number of authors
                cont = publications.size();
                return new String[]{publications.toString(), keyword, cont.toString()};
            } catch (RepositoryException ex) {
                java.util.logging.Logger.getLogger(ReportsImpl.class.getName()).log(Level.SEVERE, null, ex);
                con.close();
            }

            return new String[]{"", ""};
        } catch (MalformedQueryException | QueryEvaluationException | RepositoryException ex) {
            java.util.logging.Logger.getLogger(ReportsImpl.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
        return new String[]{"", ""};
    }

    /**
     * Extract Json with publications by author
     *
     * @param author Author to search
     * @param hostname Hostname
     * @return Array of strings
     */
    public String[] getJSONReportPublicationsByAuthor(String author, String hostname) {
        String getQuery = "";
        try {
            //Variables to return with name and number of publications
            String id = "";
            String authorName = "";
            String title = "";
            String abst = "";
            String uri = "";
            String keywords = "";
            Integer cont = 0;
            //Query
            getQuery = ConstantServiceImpl.PREFIX
                    + " Select ?publicationUri ?name "
                    + " ?title ?abstract ?uri (GROUP_CONCAT(distinct ?quote;separator='; ') as ?keywords) "
                    + "WHERE "
                    + "{"
                    + "  GRAPH <http://ucuenca.edu.ec/wkhuska>"
                    + "  {"
                    + "      <" + author + "> foaf:publications ?publicationUri ."
                    + "      <" + author + "> foaf:name ?name ."
                    + "      ?publicationUri dct:title ?title . "
                    + "      OPTIONAL{ ?publicationUri bibo:abstract  ?abstract. } "
                    + "      OPTIONAL{ ?publicationUri bibo:uri  ?uri. } "
                    + "      OPTIONAL{?publicationUri bibo:Quote ?quote.} "
                    + "  }"
                    + "} group by ?publicationUri ?title ?abstract ?uri ?name";
            Repository repo = new SPARQLRepository(hostname + "/sparql/select");
            repo.initialize();
            RepositoryConnection con = repo.getConnection();
            try {
                // perform operations on the connection
                TupleQueryResult resulta = con.prepareTupleQuery(QueryLanguage.SPARQL, getQuery).evaluate();

                JSONArray publications = new JSONArray();
                JSONObject publication = new JSONObject();

                while (resulta.hasNext()) {
                    BindingSet binding = resulta.next();
                    //Form the Json object
                    publication = new JSONObject();

                    id = String.valueOf(binding.getValue("publicationUri")).replace("\"", "").replace("^^", "").split("<")[0];
                    authorName = String.valueOf(binding.getValue("name")).replace("\"", "").replace("^^", "").split("<")[0];
                    title = String.valueOf(binding.getValue("title")).replace("\"", "").replace("^^", "").split("<")[0];
                    abst = String.valueOf(binding.getValue("abstract")).replace("\"", "").replace("^^", "").split("<")[0];
                    uri = String.valueOf(binding.getValue("uri")).replace("\"", "").replace("^^", "").split("<")[0];
                    keywords = String.valueOf(binding.getValue("keywords")).replace("\"", "").replace("^^", "").split("<")[0];

                    publication.put("id", id);
                    publication.put("title", title);
                    publication.put("abstract", (abst == null || abst == "" || abst == "null") ? null : abst);
                    publication.put("uri", (uri == null || uri == "" || uri == "null") ? null : uri);
                    publication.put("keywords", (keywords == null || keywords == "" || keywords == "null") ? null : keywords);

                    publications.add(publication);

                }

                con.close();
                //Number of publications
                cont = publications.size();
                return new String[]{publications.toString(), authorName, cont.toString()};
            } catch (RepositoryException ex) {
                java.util.logging.Logger.getLogger(ReportsImpl.class.getName()).log(Level.SEVERE, null, ex);
                con.close();
            }

            return new String[]{"", ""};
        } catch (MalformedQueryException | QueryEvaluationException | RepositoryException ex) {
            java.util.logging.Logger.getLogger(ReportsImpl.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
        return new String[]{"", ""};
    }

    public String[] getJSONAuthorsbyIES(String ies, String hostname) {

        try {
            String queryAuthors = ConstantServiceImpl.PREFIX
                    + "SELECT ?name (COUNT(*) as ?totalPub)"
                    + "WHERE {  "
                    + "  GRAPH <" + constant.getWkhuskaGraph() + ">  {"
                    + "    ?author foaf:publications ?publication ;"
                    + "       dct:provenance ?endpoint ."
                    + "    ?author foaf:name ?name ."
                    + "    {"
                    + "    	SELECT * {"
                    + "        	GRAPH <" + constant.getEndpointsGraph() + "> {"
                    + "              ?endpoint uc:name \"" + ies + "\"^^xsd:string ."
                    + "            }"
                    + "        }"
                    + "    }"
                    + "  }"
                    + "} "
                    + "GROUP BY ?author ?name "
                    + "ORDER BY DESC(?totalPub)";

            String queryIES = ConstantServiceImpl.PREFIX
                    + "SELECT (STR(?name) as ?fname)"
                    + "WHERE {"
                    + "  GRAPH <" + constant.getEndpointsGraph() + ">  {"
                    + "    ?endpoint uc:name \"" + ies + "\"^^xsd:string ."
                    + "    ?endpoint uc:fullName ?name ."
                    + " FILTER (lang(?name) = \"es\")."
                    + "  }"
                    + "}";

            Repository repository = new SPARQLRepository(hostname + "/sparql/select");
            repository.initialize();
            RepositoryConnection connection = repository.getConnection();

            try {
                // perform operations on the connection
                TupleQueryResult resultAuthors = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryAuthors).evaluate();
                JSONArray authors = new JSONArray();
                JSONObject author;
                int totalAuthors = 0;
                String fname = "";

                //Check authors of each university
                while (resultAuthors.hasNext()) {
                    BindingSet binding = resultAuthors.next();
                    String name = String.valueOf(binding.getValue("name")).replace("\"", "").replace("^^", "").split("<")[0];
                    String totalPub = String.valueOf(binding.getValue("totalPub")).replace("\"", "").replace("^^", "").split("<")[0];

                    author = new JSONObject();
                    author.put("author", name);
                    author.put("numPub", totalPub);

                    authors.add(author);
                    totalAuthors++;
                }

                TupleQueryResult resultIES = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryIES).evaluate();
                if (resultIES.hasNext()) {
                    BindingSet binding = resultIES.next();
                    fname = String.valueOf(binding.getValue("fname"));
                }

                connection.close();

                return new String[]{authors.toString(), fname, String.valueOf(totalAuthors)};

            } catch (RepositoryException ex) {
                java.util.logging.Logger.getLogger(ReportsImpl.class.getName()).log(Level.SEVERE, null, ex);
                connection.close();
            }

            return new String[]{"", ""};

        } catch (MalformedQueryException | QueryEvaluationException | RepositoryException ex) {
            java.util.logging.Logger.getLogger(ReportsImpl.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
        return new String[]{"", ""};
    }
    
    public String[] getJSONStatisticsTopKeywords(String hostname) {

        String getQuery = "";
        try {
            //Query
            getQuery = ConstantServiceImpl.PREFIX
                    + " SELECT "
                    + "  ?uriArea ?keyword ?total "
                    + "WHERE "
                    + "{  "
                    + "	SELECT  ?keyword (IRI(REPLACE(?keyword, \" \", \"_\", \"i\")) as ?uriArea) ?total "
                    + "    WHERE "
                    + "	   { "
                    + "    	{ "
                    + "            SELECT DISTINCT ?keyword (COUNT(DISTINCT ?s) AS ?total) "
                    + "            WHERE "
                    + "            { "
                    + "              GRAPH <http://ucuenca.edu.ec/wkhuska> "
                    + "              { "
                    + "                ?s foaf:publications ?publications. "
                    + "                ?publications bibo:Quote ?keyword. "
                    //+ "                #?s dct:subject ?keyword. "
                    + "              } "
                    + "            } "
                    + "            GROUP BY ?keyword "
                    + "            ORDER BY DESC(?total) "
                    + "            LIMIT 10 "
                    + "        } "
                    + "        FILTER(!REGEX(?keyword,\"TESIS\")) "
                    + "    } "
                    + "} ";

            Repository repo = new SPARQLRepository(hostname + "/sparql/select");
            repo.initialize();
            RepositoryConnection con = repo.getConnection();
            try {
                // perform operations on the connection
                TupleQueryResult resulta = con.prepareTupleQuery(QueryLanguage.SPARQL, getQuery).evaluate();

                JSONArray keywords = new JSONArray();
                
                String uri, key, total;
                
                while (resulta.hasNext()) {
                    BindingSet binding = resulta.next();
                    uri = String.valueOf(binding.getValue("uriArea")).replace("\"", "").replace("^^", "").split("<")[0];
                    key = String.valueOf(binding.getValue("keyword")).replace("\"", "").replace("^^", "").split("<")[0];
                    total = String.valueOf(binding.getValue("total")).replace("\"", "").replace("^^", "").split("<")[0];
                    
                    JSONObject keyword = new JSONObject();
                    keyword.put("uri", uri);
                    keyword.put("key", key);
                    keyword.put("total", total);
                    
                    keywords.add(keyword);
                }
                con.close();
                
                return new String[]{keywords.toString(), ""};
            } catch (RepositoryException ex) {
                java.util.logging.Logger.getLogger(ReportsImpl.class.getName()).log(Level.SEVERE, null, ex);
                con.close();
            }

            return new String[]{"", ""};
        } catch (MalformedQueryException | QueryEvaluationException | RepositoryException ex) {
            java.util.logging.Logger.getLogger(ReportsImpl.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
        return new String[]{"", ""};
    }
    
}
