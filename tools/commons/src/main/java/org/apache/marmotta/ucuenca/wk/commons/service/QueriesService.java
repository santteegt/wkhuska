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
public interface QueriesService {

    String getAuthorsDataQuery(String graph, String endpointsgraph);

    String getCountPersonQuery(String graph);

    String getLimit(String limit);

    String getOffset(String offset);

    /**
     * return query to obtain all subject ( keywords ) of an author , using
     * dct:subject property
     *
     * @return
     */
    String getRetrieveKeysQuery();

    /**
     * return a query to obtain all resource related with Authors
     *
     * @param wkhuskagraph
     * @return
     */
    String getAuthorsQuery(String wkhuskagraph);

    /**
     * return query to obtain all properties of a resource using LDC ( Linked
     * Data Client )
     *
     * @return
     */
    String getRetrieveResourceQuery();

    /**
     * Return a INSERT QUERY when object is a LITERAL
     *
     * @param args
     * @return
     */
    String getInsertDataLiteralQuery(String... args);

    /**
     * Return a INSERT QUERY when object is a URI
     *
     * @param args
     * @return
     */
    String getInsertDataUriQuery(String... args);

    /**
     *
     * @param graph
     * @param resource
     * @return
     */
    String getAskResourceQuery(String graph, String resource);

    /**
     * ASK is exist a triplet
     *
     * @param args //graph, subject, predicate, object arguments
     * @return
     */
    String getAskQuery(String... args);

    String getEndpointDataQuery(String... arg);

    String getlisEndpointsQuery(String endpointsGraph);
    
    String getlistEndpointNamesQuery();

    String getEndpointByIdQuery(String endpointsGraph, String id);

    String getEndpointDeleteQuery(String endpointsGraph, String id);

    String getEndpointUpdateStatusQuery(String... args);

    String getGraphsQuery();

    String getPublicationsQuery(String providerGraph);

    String getPublicationsPropertiesQuery(String providerGraph, String publicationResource);

    String getPublicationsPropertiesQuery(String publicationResource);

    //String getMembersQuery();
    String getPublicationFromProviderQuery();

    String getPublicationForExternalAuthorFromProviderQuery(String property);

    String getPublicationPropertiesQuery(String property);

    //Microsoft Academics
//    String getPublicationsMAQuery(String providerGraph);
    //  String getPublicationFromMAProviderQuery();
    //  String getPublicationMAPropertiesQuery();
    String getAllTitlesDataQuery(String graph);

    // String getAllTitlesDataQuery();
    //String getMembersByTitleQuery();
    String getObjectByPropertyQuery(String subject, String property);

    String getObjectByPropertyQuery(String property);

    String getAbstractAndTitleQuery(String resource);

    String getSubjectAndObjectByPropertyQuery(String property);
    //Google Scholar

    // String getAllCreatorsDataQuery();
    //String getPublicationFromGSProviderQuery();
    String getAuthorPublicationsQuery(String... varargs);

    String getAuthorPublicationsQueryFromProvider(String... varargs);

    String getPublicationDetails(String publicationResource);

    String getPublicationsTitleQuery(String providerGraph, String prefix);

    String getPublicationsTitleScopusQuery(String providerGraph, String prefix);

    String getPublicationsCount(String graph);

    String getTotalAuthorWithPublications(String graph);

    String getPublicationsCountCentralGraph();

    String deleteDataGraph(String graph);

    String getPublicationPropertiesAsResourcesQuery();

    String getTitlePublications(String graph);

    String getFirstNameLastNameAuhor(String graph, String authorResource);

    String authorDetailsOfProvenance(String graph, String authorResource);

    String authorGetProvenance(String graph, String authorResource);

    String getAuthorPublicationFilter(String graph, String fname, String lname);

    String getAskResourcePropertieQuery(String graph, String resource, String propertie);

    String getAskProcessAlreadyAuthorProvider(String providerGraph, String authorResource);

    String getAuthorsKeywordsQuery(String resource);

    /**
     * Get All Data sources from UTPL ENDPOINT
     *
     * @param graph
     * @return
     */
    String getSourcesfromUniqueEndpoit(String graph);

    /**
     * Get All documents from UTPL ENDPOINT
     *
     * @param repository
     * @return
     */
    String getDocumentsAuthors(String repository, String graph);

    /**
     * @See Get all resources of an specific type
     *
     * @param type
     * @return
     */
    String getResourceUriByType(String type);

    /**
     * @See Get all properties of a resource by type
     * @param type
     * @return
     */
    String getPropertiesOfResourceByType(String type);

    /**
     * Get all titles of publications give a type.
     *
     * @param graph
     * @param type
     * @return
     */
    String getPublicationsTitleByType(String graph, String type);

    /**
     * @See Get all properties of an author by uri.
     * @param graph
     * @param endpointsgraph
     * @param resource
     * @return
     */
    String getAuthorsDataQueryByUri(String graph, String endpointsgraph, String resource);

    /**
     * @See Get triples by filters, parameters of search in args[]
     * @param args
     * @return
     */
    String getTriplesByFilter(String... args);

    /**
     * @See Get an uri endpoint by name of endpoint.
     *
     * @param nameEndpint
     * @return
     */
    String getEndPointUriByName(String nameEndpint);

    String getAuthorPublicationsQueryFromGenericProvider(String... args);
}
