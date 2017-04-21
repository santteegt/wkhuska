/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.marmotta.ucuenca.wk.commons.service;

import java.util.List;

/**
 *
 * @author Satellite
 */
public interface DistanceService {

    /**
     * Full Name formats FROM DBLP: Avila=Gonzales:Carlos_Andres FROM SCOPUS:
     * Avila Gonzales:Carlos Andres FROM LOCAL: Avila Gonzales:Carlos Andres
     *
     * @param args: args[0] source of full name A, args[1] full name A, args[2]
     * source of full name B, args[3] full name B
     * @return
     */
    boolean syntacticComparisonNames(String... args);

    boolean semanticComparison(List<String> listA, List<String> listB);

    boolean semanticComparison(String word, List<String> listB);
    
    Double semanticComparisonValue(List<String> listA, List<String> listB);

    double cosineSimilarityAndLevenshteinDistance(String param1, String param2);
    
    float jaccardDistance(String param1, String param2);
    
}
