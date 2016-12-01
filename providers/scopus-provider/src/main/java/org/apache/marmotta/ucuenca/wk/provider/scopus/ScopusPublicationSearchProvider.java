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
package org.apache.marmotta.ucuenca.wk.provider.scopus;

//import org.apache.marmotta.ucuenca.wk.provider.dblp.*;
import org.apache.marmotta.ldclient.api.endpoint.Endpoint;
import org.apache.marmotta.ldclient.exception.DataRetrievalException;
import org.apache.marmotta.ldclient.model.ClientConfiguration;
import org.apache.marmotta.ldclient.model.ClientResponse;
import org.apache.marmotta.ldclient.services.ldclient.LDClient;
import org.apache.marmotta.ldclient.services.provider.AbstractHttpProvider;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jdom2.xpath.XPathFactory;
import org.openrdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom2.Namespace;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.FOAF;

/**
 * Support Scopus Publication information as XML
 * <p/>
 * @author Freddy Sumba
 */
    public class ScopusPublicationSearchProvider extends AbstractHttpProvider {

    public static final String NAME = "Scopus  Search Publication Provider";
    public static final String API = "http://api.elsevier.com/content/search/scopus?query=&apiKey=";
    public static final String PATTERN = "http://api\\.elsevier\\.com/content/search/scopus\\?query\\=au\\-id%28(.*)%29\\&apiKey\\=(.*)\\&httpAccept\\=application/xml\\&view\\=COMPLETE";
    public static final String URL_RESOURCE_PUBLICATION = "http://api.elsevier.com/content/abstract/doi/DOIParam?apiKey=apiKeyParam&httpAccept=application/rdf%2Bxml";
    public static final String URL_RESOURCE_PUBLICATIONPARAM = "http://api.elsevier.com/content/abstract/doi/DOIParam";
    private static Logger log = LoggerFactory.getLogger(ScopusPublicationSearchProvider.class);
    private static String apiKeyParam = "";
    private static String authorIdParam = "";
    public static final Namespace NAMESPACE_DC = Namespace.getNamespace("dc", "http://purl.org/dc/elements/1.1/");
    public static final Namespace NAMESPACE_PRISM = Namespace.getNamespace("prism", "http://prismstandard.org/namespaces/basic/2.0/");
    public static final Namespace NAMESPACE_ATOM=Namespace.getNamespace("atom", "http://www.w3.org/2005/Atom");

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
            "text/xml"
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
            url = resource;
            apiKeyParam = m.group(2);
            authorIdParam = m.group(1);
        }
        return Collections.singletonList(url);
    }

    @Override
    public List<String> parseResponse(String resource, String requestUrl, Model triples, InputStream input, String contentType) throws DataRetrievalException {
        log.debug("Request Successful to {0}", requestUrl);
        try {
            final Document doc = new SAXBuilder(XMLReaders.NONVALIDATING).build(input);
            Element aux = doc.getRootElement();
            boolean ecuatoriano = false;
            for (Element element : aux.getChildren("entry", aux.getNamespace())) {
                if (affiliationEcuador(element.getChildren("affiliation", NAMESPACE_ATOM))) {
                    ecuatoriano = true;
                    break;
                }
            }
            if (ecuatoriano) {
                for (Element element : aux.getChildren("entry", aux.getNamespace())) {
                    String abstractDoiParam = element.getChildText("doi", NAMESPACE_PRISM);
                    String abstractURLParam = element.getChildText("url", NAMESPACE_PRISM);
                    String abstractAbstractParam = element.getChildText("description", NAMESPACE_DC);
                    List<String> creatorsList = new ArrayList<>();
                    List<Element> creators = element.getChildren("author",NAMESPACE_ATOM);
                    for (Element author : creators) {
                        creatorsList.add(author.getChildText("author-url",NAMESPACE_ATOM));
                    }

                    ClientConfiguration conf = new ClientConfiguration();
                    LDClient ldClient = new LDClient(conf);
                    if (abstractDoiParam != null) {
                        Model candidateModel = null;
                        String authorUrlResourceCleaned = URL_RESOURCE_PUBLICATION.replace("DOIParam", abstractDoiParam).replace("apiKeyParam", apiKeyParam);

                        ClientResponse response = ldClient.retrieveResource(authorUrlResourceCleaned);
                        ValueFactory factory = ValueFactoryImpl.getInstance();
                        triples.add(factory.createURI("http://api.elsevier.com/content/author/author_id/" + authorIdParam), FOAF.PUBLICATIONS, factory.createURI(abstractURLParam));
                        if (abstractAbstractParam != null) {
                            triples.add(factory.createStatement(factory.createURI(abstractURLParam),
                                factory.createURI("http://purl.org/ontology/bibo/abstract"), factory.createLiteral(abstractAbstractParam)));
                        }
                        for (String uriCreator : creatorsList) {
                            triples.add(factory.createStatement(factory.createURI(abstractURLParam),
                                    factory.createURI("http://purl.org/dc/terms/contributor"), factory.createURI(uriCreator)));

                        }
                        Model authorModel = response.getData();
                        if (candidateModel == null) {
                            candidateModel = authorModel;
                        } else {
                            candidateModel.addAll(authorModel);
                        }
                        triples.addAll(candidateModel);

                    }

                }
            }
        } catch (IOException e) {
            throw new DataRetrievalException("I/O error while parsing HTML response", e);
        } catch (JDOMException e) {
            throw new DataRetrievalException("could not parse XML response. It is not in proper XML format", e);
        }
        return Collections.emptyList();
    }
    
    private boolean affiliationEcuador(List<Element> affiliations) {
        boolean ecuadorian = false;

        for (Element affiliation : affiliations) {
            if (affiliation.getChildText("affiliation-country", NAMESPACE_ATOM)
                    .equalsIgnoreCase("ecuador")) {
                ecuadorian = true;
                break;
            }
        }
            
        return ecuadorian;
    }

    protected static List<Element> queryElements(Document n, String query) {
        return XPathFactory.instance().compile(query, new ElementFilter(), null, n.getNamespacesInherited()).evaluate(n);
    }

}
