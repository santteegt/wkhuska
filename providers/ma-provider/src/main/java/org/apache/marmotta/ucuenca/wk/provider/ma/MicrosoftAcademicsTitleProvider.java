/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.marmotta.ucuenca.wk.provider.ma;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import java.io.BufferedReader;

import org.apache.marmotta.ldclient.api.endpoint.Endpoint;
import org.apache.marmotta.ldclient.exception.DataRetrievalException;
import org.apache.marmotta.ldclient.services.provider.AbstractHttpProvider;

import org.openrdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.apache.marmotta.ucuenca.wk.provider.ma.MicrosoftAcademicsProvider.MAPPINGSCHEMA;
import org.apache.marmotta.ucuenca.wk.provider.ma.util.JSONtoRDF;
import org.apache.marmotta.ucuenca.wk.provider.ma.util.MapPublications;
import org.apache.marmotta.ucuenca.wk.provider.ma.util.Publication;
import org.json.simple.parser.ParseException;
import org.openrdf.model.vocabulary.FOAF;

/**
 * Support Google Scholar information as RDF
 * <p/>
 * Author: Santiago Gonzalez
 */
public class MicrosoftAcademicsTitleProvider extends AbstractHttpProvider {

    public static final String NAME = "Microsoft Academics Title Provider";
    public static final String API = "http://academic.research.microsoft.com/";
    public static final String PATTERN = "http://academic\\.research\\.microsoft\\.com/json\\.svc/search\\?AppId\\=(.*)\\&TitleQuery\\=(.*)\\&ResultObjects\\=Publication\\&PublicationContent\\=AllInfo\\&StartIdx\\=1\\&EndIdx\\=1(.*)$";

    private static String nsUcuenca = "https://www.cedia.org.ec/";
    private static Logger log = LoggerFactory.getLogger(MicrosoftAcademicsTitleProvider.class);
    public String stringSearch = null, authorSearch = null, advancedSearch = null, appId = null;
    public static final ConcurrentMap<String, String> MAPPINGSCHEMA = new ConcurrentHashMap<String, String>();
    private MapPublications mapPublications = new MapPublications();

    static {
        MAPPINGSCHEMA.put("entity::type", "http://purl.org/ontology/bibo/Document");
        MAPPINGSCHEMA.put("entity::property:uri", "http://purl.org/ontology/bibo/uri");
        MAPPINGSCHEMA.put("entity::property:title", "http://purl.org/dc/terms/title");
        MAPPINGSCHEMA.put("entity::property:abstract", "http://purl.org/ontology/bibo/abstract");
        MAPPINGSCHEMA.put("entity::property:journal", "http://purl.org/ontology/bibo/Journal");
        MAPPINGSCHEMA.put("entity::property:creator", "http://purl.org/dc/elements/1.1/creator");
        MAPPINGSCHEMA.put("entity::property:date", "http://purl.org/dc/elements/1.1/date");
        MAPPINGSCHEMA.put("entity::property:doi", "http://purl.org/ontology/bibo/doi");
        MAPPINGSCHEMA.put("entity::property:authorlist", "http://purl.org/ontology/bibo/authorList");
        MAPPINGSCHEMA.put("entity::property:quote", "http://purl.org/ontology/bibo/Quote");
        MAPPINGSCHEMA.put("entity::property:conference", "http://purl.org/ontology/bibo/Conference");
        MAPPINGSCHEMA.put("entity::property:cites", "http://purl.org/ontology/bibo/cites");
        MAPPINGSCHEMA.put("entity::property:type", nsUcuenca + "Type");
        MAPPINGSCHEMA.put("entity::property:referenceCount", nsUcuenca + "referenceCount");
        MAPPINGSCHEMA.put("entity::property:citationCount", nsUcuenca + "citationCount");
        MAPPINGSCHEMA.put("entity::property:contributor", "http://purl.org/dc/terms/contributor");
        MAPPINGSCHEMA.put("entity::property:fullversionurl", nsUcuenca + "FullVersionURL");
        MAPPINGSCHEMA.put("entity::property:fname", FOAF.FIRST_NAME.toString());
        MAPPINGSCHEMA.put("entity::property:lname", FOAF.LAST_NAME.toString());
        MAPPINGSCHEMA.put("entity::property:mname", FOAF.NICK.toString());

    }

    /**
     * Return the name of this data provider. To be used e.g. in the
     * configuration and in log messages.
     *
     * @return
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Return the list of mime types accepted by this data provider.
     *
     * @return
     */
    @Override
    public String[] listMimeTypes() {
        return new String[]{
            "text/html"
        };
    }

    /**
     * Build the URL to use to call the webservice in order to retrieve the data
     * for the resource passed as argument. In many cases, this will just return
     * the URI of the resource (e.g. Linked Data), but there might be data
     * providers that use different means for accessing the data for a resource,
     * e.g. SPARQL or a Cache.
     *
     *
     * @param resource
     * @param endpoint endpoint configuration for the data provider (optional)
     * @return
     */
    @Override
    public List<String> buildRequestUrl(String resource, Endpoint endpoint) {
        String url = null;
        Matcher m = Pattern.compile(PATTERN).matcher(resource);
        if (m.find()) {
            appId = m.group(1);
            authorSearch = m.group(2);

            log.debug("Extracting info for: {0}", authorSearch);
            if (authorSearch.length() > 0) {
                log.debug("Extra author search parameters: {0}", authorSearch);
            }
            if (appId.length() > 0) {
                log.debug("AppId search parameters: {0}", appId);
            }
            url = resource;
        }
        return Collections.singletonList(url);
    }

    @Override
    public List<String> parseResponse(String resource, String requestUrl, Model triples, InputStream input, String contentType) throws DataRetrievalException {
        log.debug("Request Successful to {0}", requestUrl);

        try {
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            StringBuilder responseStrBuilder = new StringBuilder();

            String inputStr = streamReader.readLine();
            while (inputStr != null) {
                responseStrBuilder.append(inputStr);
                inputStr = streamReader.readLine();
            }
            List<Publication> resultOutput = new ArrayList();

            resultOutput = mapPublications.getPublications(responseStrBuilder.toString());
            Gson gson = new Gson();
            JsonArray json = new JsonArray();
            for (Publication d : resultOutput) {
                json.add(gson.toJsonTree(d).getAsJsonObject());
            }
            JSONtoRDF parser = new JSONtoRDF(resource, MAPPINGSCHEMA, json, triples);
            //Model model2 = new TreeModel();

            try {
                parser.parse();
            } catch (Exception e) {
                throw new DataRetrievalException("I/O exception while retrieving resource: " + requestUrl, e);
            }

        } catch (IOException e) {
            //e.printStackTrace();
        } catch (ParseException ex) {
            java.util.logging.Logger.getLogger(MicrosoftAcademicsTitleProvider.class.getName()).log(Level.SEVERE, null, ex);
        }

        return Collections.emptyList();
    }

}
