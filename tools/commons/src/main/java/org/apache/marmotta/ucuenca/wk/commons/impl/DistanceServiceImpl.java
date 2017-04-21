/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.marmotta.ucuenca.wk.commons.impl;

import org.apache.marmotta.ucuenca.wk.commons.function.SyntacticDistance;
import org.apache.marmotta.ucuenca.wk.commons.function.SemanticDistance;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.apache.marmotta.ucuenca.wk.commons.service.CommonsServices;
import org.apache.marmotta.ucuenca.wk.commons.service.DistanceService;
import org.simmetrics.metrics.CosineSimilarity;
import org.simmetrics.metrics.Levenshtein;
import org.simmetrics.simplifiers.Simplifiers;
import org.simmetrics.tokenizers.Tokenizers;
import org.simmetrics.StringMetric;
import static org.simmetrics.StringMetricBuilder.with;
import org.simmetrics.metrics.JaccardSimilarity;

/**
 *
 * @author Satellite
 */
public class DistanceServiceImpl implements DistanceService {

    @Inject
    private org.slf4j.Logger log;

    private CommonsServices commonService = new CommonsServicesImpl();
    private SemanticDistance dist = null;

    public DistanceServiceImpl() {
        try {
            this.dist = new SemanticDistance();
        } catch (IOException ex) {
            Logger.getLogger(DistanceServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(DistanceServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public boolean semanticComparison(List<String> listA, List<String> listB) {
        try {
            //SemanticDistance dist = new SemanticDistance();
            double value = dist.semanticKeywordsDistance(listA, listB);

            double semthreshold = Double.parseDouble(commonService.readPropertyFromFile("parameters.properties", "semanticDistanceListAListB"));
            if (value < semthreshold) {
                return true;
            }
        } catch (IOException | ClassNotFoundException | SQLException ex) {
            log.error("ERROR IN SemanticDistance:" + ex);
            //         Logger.getLogger(DistanceServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    @Override
    public Double semanticComparisonValue(List<String> listA, List<String> listB) {
        try {
            //SemanticDistance dist = new SemanticDistance();
            return dist.nwdDistance(listA, listB); //double value = dist.semanticKeywordsDistance(listA, listB);

        } catch (IOException | ClassNotFoundException | SQLException ex) {
            log.error("ERROR IN SemanticDistance:" + ex);
        }
        return 1.11;
    }

    @Override
    public boolean semanticComparison(String word, List<String> listB) {
        try {
            List<String> listA = new ArrayList<>();
            listA.add(word);
            SemanticDistance dist = new SemanticDistance();

            double value = dist.semanticKeywordsDistance(listA, listB);
            double semthreshold = Double.parseDouble(commonService.readPropertyFromFile("parameters.properties", "semanticDistanceWordListB"));
            if (value < semthreshold) {
                return true;
            }
        } catch (IOException | ClassNotFoundException | SQLException ex) {
            log.error("ERROR IN SemanticDistance:" + ex);
            //         Logger.getLogger(DistanceServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    @Override
    public boolean syntacticComparisonNames(String... args) {

        try {
            SyntacticDistance syntacticdistance = new SyntacticDistance();
            if (args != null && args.length > 0) {
                for (int i = 0; i < args.length; i++) {
                    if (args[i] == null) {
                        return false;
                    }
                }
            } else {
                return false;
            }
            return syntacticdistance.compareNames(args);
        } catch (Exception ex) {
            Logger.getLogger(DistanceServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public double cosineSimilarityAndLevenshteinDistance(String param1, String param2) {

        String a = param1;
        String b = param2;

        StringMetric metric
                = with(new CosineSimilarity<String>())
                .simplify(Simplifiers.toLowerCase())
                .simplify(Simplifiers.removeNonWord()).simplifierCache()
                .tokenize(Tokenizers.qGram(3)).tokenizerCache().build();
        float compare = metric.compare(a, b);

        StringMetric metric2
                = with(new Levenshtein())
                .simplify(Simplifiers.removeDiacritics())
                .simplify(Simplifiers.toLowerCase()).build();

        float compare2 = metric2.compare(a, b);

        float similarity = (float) ((compare + compare2) / 2.0);

        return similarity;
    }
    
    @Override
    public float jaccardDistance(String param1, String param2) {
        StringMetric metric2
                = with(new JaccardSimilarity<String>())
                .simplify(Simplifiers.removeDiacritics())
                .simplify(Simplifiers.toLowerCase())
                .tokenize(Tokenizers.qGram(2)).tokenizerCache().build();

        return metric2.compare(param1, param2);
    }

}
