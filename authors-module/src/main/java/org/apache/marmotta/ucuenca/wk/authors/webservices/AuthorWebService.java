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
package org.apache.marmotta.ucuenca.wk.authors.webservices;

import com.google.common.io.CharStreams;
import com.sun.jersey.multipart.FormDataParam;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.marmotta.ucuenca.wk.authors.api.AuthorService;
import org.apache.marmotta.ucuenca.wk.authors.api.EndpointService;
import org.apache.marmotta.ucuenca.wk.authors.api.SparqlEndpoint;
import org.apache.marmotta.ucuenca.wk.authors.api.UTPLAuthorService;
import org.apache.marmotta.ucuenca.wk.authors.exceptions.DaoException;
import org.apache.marmotta.ucuenca.wk.authors.exceptions.UpdateException;
import org.openrdf.query.MalformedQueryException;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

@ApplicationScoped
@Path("/authors-module")
public class AuthorWebService {

    @Inject
    private Logger log;

    @Inject
    private AuthorService authorService;
   
    @Inject
    private UTPLAuthorService utplAuthorService;

    @Inject
    private EndpointService endpointService;

    public static final String AUTHOR_UPDATE = "/update";
    public static final String ADD_ENDPOINT = "/addendpoint";
    public static final String AUTHOR_SPLIT = "/split";

    /**
     * Add Endpoint Service
     *
     * @param resultType
     * @param request
     * @return
     */
    @POST
    @Path(ADD_ENDPOINT)
    public Response addEndpointPost(@QueryParam("Endpoint") String resultType, @Context HttpServletRequest request) {
        try {
            String params = CharStreams.toString(request.getReader());
            log.debug("Adding Endpoint", params);
            return addEndpointImpl(params);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(AuthorWebService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UpdateException ex) {
            java.util.logging.Logger.getLogger(AuthorWebService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @DELETE
    @Path("/endpoint/delete")
    public Response removeEndpoint(@QueryParam("id") String resourceid) {

        SparqlEndpoint endpoint = endpointService.getEndpoint(resourceid);
        if (endpoint == null) {
            return Response.ok().entity("notFound " + resourceid + " Endpoint").build();
        }
        endpointService.removeEndpoint(resourceid);
        return Response.ok().entity("Endpoint was successfully removed").build();
    }

    @POST
    @Path("/endpoint/updatestatus")
    public Response updateEndpoint(@QueryParam("id") String resourceid, @QueryParam("oldstatus") String oldstatus, @QueryParam("newstatus") String newstatus) {

        SparqlEndpoint endpoint = endpointService.getEndpoint(resourceid);
        if (endpoint == null) {
            return Response.ok().entity("notFound " + resourceid + " Endpoint").build();
        }
        endpointService.updateEndpoint(resourceid, oldstatus, newstatus);
        return Response.ok().entity("Endpoint was successfully removed").build();
    }

    @GET
    @Path("/endpoint/list")
    @Produces("application/json")
    public Response listEndpoints() {

        List<Map<String, Object>> result = new LinkedList<Map<String, Object>>();
        for (SparqlEndpoint endpoint : endpointService.listEndpoints()) {
            result.add(buildEndpointJSON(endpoint));
        }

        return Response.ok().entity(result).build();
    }

    private Map<String, Object> buildEndpointJSON(SparqlEndpoint endpoint) {
        HashMap<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put("id", endpoint.getResourceId());
        resultMap.put("status", endpoint.getStatus());
        resultMap.put("name", endpoint.getName());
        resultMap.put("url", endpoint.getEndpointUrl());
        resultMap.put("graph", endpoint.getGraph());
        resultMap.put("fullName", endpoint.getFullName());
        resultMap.put("city", endpoint.getCity());
        resultMap.put("province", endpoint.getProvince());
        resultMap.put("latitude", endpoint.getLatitude());
        resultMap.put("longitude", endpoint.getLongitude());
        //       resultMap.put("active", endpoint.isActive());

        return resultMap;
    }

    /**
     * Add Endpoint Impl
     *
     * @param urisString
     * @return
     * @throws UpdateException
     */
    private Response addEndpointImpl(String urisString) throws UpdateException {
        if (StringUtils.isBlank(urisString)) {
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity("Required Endpoint and GraphURI").build();
        } else {
            String status = urisString.split("\"")[3];
            String name = urisString.split("\"")[7];
            String endpoint = urisString.split("\"")[11];
            String graphUri = urisString.split("\"")[15];
            String fullName = urisString.split("\"")[19];
            String englishName = urisString.split("\"")[23];
            String city = urisString.split("\"")[27];
            String province = urisString.split("\"")[31];
            String latitude = urisString.split("\"")[35];
            String longitude = urisString.split("\"")[39];
            String result = endpointService.addEndpoint(status, name, endpoint, graphUri, fullName, englishName, city, province, latitude, longitude);
            return Response.ok().entity(result).build();
        }

    }

    /**
     * Author Load Service
     *
     * @param resultType
     * @param request
     * @return
     */
    @POST
    @Path(AUTHOR_UPDATE)
    public Response updateAuthorPost(@QueryParam("Endpoint") String resultType, @Context HttpServletRequest request) {
        try {
            String params = CharStreams.toString(request.getReader());
            log.debug("EndPoint & GraphURI: {}", params);
            return authorUpdate(params);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(AuthorWebService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UpdateException ex) {
            java.util.logging.Logger.getLogger(AuthorWebService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * AUTHOR UPDATE IMPLEMENTATION
     *
     * @param urisString // JSON contains Endpoint and GraphURI
     *
     */
    private Response authorUpdate(String urisString) throws UpdateException {
        if (StringUtils.isBlank(urisString)) {
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity("Required Endpoint and GraphURI").build();
        } else {
            try {
                String endpoint = urisString.split("\"")[3];
                String graphUri = urisString.split("\"")[7];
                String result = authorService.runAuthorsUpdateMultipleEP(endpoint, graphUri);
                return Response.ok().entity(result).build();
            } catch (DaoException ex) {
                java.util.logging.Logger.getLogger(AuthorWebService.class.getName()).log(Level.SEVERE, null, ex);
            } catch (QueryEvaluationException ex) {
                java.util.logging.Logger.getLogger(AuthorWebService.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return null;
    }

    @POST
    @Path(AUTHOR_SPLIT)
    public Response split(@QueryParam("endpointuri") String endpointuri, @QueryParam("graphuri") String graphuri) {

        try {
            String endpoint = endpointuri;
            String graph = graphuri;
            return authorSplit(endpoint, graph);
        } catch (UpdateException ex) {
            java.util.logging.Logger.getLogger(AuthorWebService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;

    }

    /**
     *
     */
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(
            @FormDataParam("file") InputStream uploadedInputStream//, @FormDataParam("file") FormDataContentDisposition fileDetail
            //, @FormParam("endpoint") String endpoint
    ) {

        String result = "";
        String endpoint = "";
        String endpointName = "";
        
        InputStreamReader inputStreamReader = new InputStreamReader(uploadedInputStream, StandardCharsets.UTF_8);

        String line = "";
        String cvsSplitBy = ",";
        int cont = 0;
        String saveAuthor = null;
        int contEPName = -1;
        
        try {
            BufferedReader br = new BufferedReader(inputStreamReader);
            line = br.readLine();
            while (line != null) {
                int two = 2;
                if (line.contains("endpointName")) {
                    contEPName = cont + 2; 
                }
                if (cont == contEPName) {
                    endpointName = line;
                }
                if (line.contains("http://")) {
                    endpoint = line;
                } else if (line.split(cvsSplitBy).length >= two)
                // use comma as separator
                {
                    result += line + "\n";
                }
                cont++;
                line = br.readLine();
            }
            
            BufferedReader reader = new BufferedReader(new StringReader(result));
            line = reader.readLine();
            while (line != null) {
                String[] researcher = line.split(cvsSplitBy);
                String keywords = null;

                int size = researcher.length;
                int two = 2;
                if (size > two) {
                    //Separate the keywords
                    keywords = researcher[2];
                }

                saveAuthor = authorService.saveAuthorFromFile(endpoint, endpointName, researcher[0], researcher[1], keywords);
                line = reader.readLine();
            }
            

            if (br != null) {
                br.close();
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(AuthorWebService.class.getName()).log(Level.SEVERE, null, ex);
        }
        //Prepare output message
        String output = "";
        if ("".equals(result)){
            output = "File not in the correct format.";
        } else {
            output = "Resultado: " + saveAuthor;
        }

        return Response.status(200).entity(output).build();

    }
    
    
    
    
    /**
     * AUTHOR UPDATE IMPLEMENTATION
     *
     * @param urisString // JSON contains Endpoint and GraphURI
     *
     */
    private Response authorSplit(String endpoint, String graph) throws UpdateException {
        if (StringUtils.isBlank(endpoint)) {
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity("Required Endpoint and GraphURI").build();
        } else {

            try {
                String result = utplAuthorService.runAuthorsSplit(endpoint, graph);
                return Response.ok().entity(result).build();
            } catch (DaoException | RepositoryException | MalformedQueryException | QueryEvaluationException ex) {
                java.util.logging.Logger.getLogger(AuthorWebService.class.getName()).log(Level.SEVERE, null, ex);
                log.error("Error: Getting Sources List");
            }

        }
        return null;
    }

}
