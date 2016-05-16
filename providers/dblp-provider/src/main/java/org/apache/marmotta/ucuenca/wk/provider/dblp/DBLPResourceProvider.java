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
package org.apache.marmotta.ucuenca.wk.provider.dblp;

import org.apache.commons.lang3.StringUtils;
import org.apache.marmotta.commons.vocabulary.DCTERMS;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.apache.marmotta.ldclient.api.endpoint.Endpoint;
import org.apache.marmotta.ldclient.api.provider.DataProvider;
import org.apache.marmotta.ldclient.provider.xml.AbstractXMLDataProvider;
import org.apache.marmotta.ldclient.provider.xml.mapping.XPathLiteralMapper;
import org.apache.marmotta.ldclient.provider.xml.mapping.XPathValueMapper;
import org.apache.marmotta.ucuenca.wk.provider.dblp.mapper.DBLPURIMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Support DBLP Resource Data as RDF
 * <p/>
 * Author: Santiago Gonzalez
 */
@Deprecated
public class DBLPResourceProvider extends AbstractXMLDataProvider implements DataProvider {

    private static final String NS_DOCUMENT = "http://purl.org/ontology/bibo/";
    public static final String NAME = "DBLP Resource Provider";
    public static final String PATTERN = "http://dblp\\.org\\/rec/(.*)";
    public static final String LEGACY_PATTERN = "(http://dblp\\.uni\\-trier\\.de/rec/)(.*)";

    private static ConcurrentMap<String, String> dblpNamespaces = new ConcurrentHashMap<String, String>();

    static {
        dblpNamespaces.put("dblp", "http://dblp.dagstuhl.de/rdf/schema-2015-01-26#");
        dblpNamespaces.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        dblpNamespaces.put("owl", "http://www.w3.org/2002/07/owl#");
        dblpNamespaces.put("dcterms", "http://purl.org/dc/terms/");
        dblpNamespaces.put("foaf", "http://xmlns.com/foaf/0.1");
        dblpNamespaces.put("bibtex", "http://data.bibbase.org/ontology/#");
    }

    private static final String ROOT = "/rdf:RDF/dblp:Publication/";

    private static ConcurrentMap<String, XPathValueMapper> mediaOntMappings = new ConcurrentHashMap<String, XPathValueMapper>();

    static {
        //mediaOntMappings.put(OWL.SAMEAS.stringValue(), new DBLPURIMapper(ROOT + "owl:sameAs/@rdf:resource", dblpNamespaces));
    	mediaOntMappings.put(DCTERMS.title.stringValue(), new XPathLiteralMapper(ROOT + "dblp:title", dblpNamespaces));
    	mediaOntMappings.put(DCTERMS.contributor.stringValue(), new DBLPURIMapper(ROOT + "dblp:authoredBy/@rdf:resource", dblpNamespaces));
    	mediaOntMappings.put(NS_DOCUMENT + "uri", new DBLPURIMapper(ROOT + "dblp:primaryElectronicEdition/@rdf:resource", dblpNamespaces));
    	mediaOntMappings.put(DCTERMS.publisher.stringValue(), new XPathLiteralMapper(ROOT + "dblp:publishedInBook", dblpNamespaces));
        mediaOntMappings.put(NS_DOCUMENT + "numPages", new XPathLiteralMapper(ROOT + "dblp:pageNumbers", dblpNamespaces));
    	mediaOntMappings.put(DCTERMS.isPartOf.stringValue() , new DBLPURIMapper(ROOT + "dblp:publishedAsPartOf/@rdf:resource", dblpNamespaces));
    	mediaOntMappings.put(DCTERMS.license.stringValue(), new DBLPURIMapper(ROOT + "dcterms:license/@rdf:resource", dblpNamespaces));
    }

    private static Logger log = LoggerFactory.getLogger(DBLPResourceProvider.class);

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
            "application/rdf+xml"
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
        String uri = "http://dblp.dagstuhl.de/rec";
        Matcher m = Pattern.compile(LEGACY_PATTERN).matcher(resource);
        if (m.find()) {
            uri += m.group(2);
        } else {
            m = Pattern.compile(PATTERN).matcher(resource);
            Preconditions.checkState(StringUtils.isNotBlank(resource) && m.find());
            uri = resource;
        }
        log.debug("API URI {0}", uri);
        return Collections.singletonList(uri.concat(".rdf"));
    }
    /**
     * Return a mapping table mapping from RDF properties to XPath Value
     * Mappers. Each entry in the map is evaluated in turn; in case the XPath
     * expression yields a result, the property is added for the processed
     * resource.
     *
     * @return
     * @param requestUrl
     */
    @Override
    protected Map<String, XPathValueMapper> getXPathMappings(String requestUrl) {
        return mediaOntMappings;
    }

    /**
     * Return a list of URIs that should be added as types for each processed
     * resource.
     *
     * @return
     * @param resource
     */
    @Override
    protected List<String> getTypes(org.openrdf.model.URI resource) {
        return ImmutableList.of(NS_DOCUMENT + "Document");
    }

    /**
     * Provide namespace mappings for the XPath expressions from namespace
     * prefix to namespace URI. May be overridden by subclasses as appropriate,
     * the default implementation returns an empty map.
     *
     * @return
     */
    @Override
    protected Map<String, String> getNamespaceMappings() {
        return dblpNamespaces;
    }

}
