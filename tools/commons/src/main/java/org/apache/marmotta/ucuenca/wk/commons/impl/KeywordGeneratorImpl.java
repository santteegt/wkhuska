
package org.apache.marmotta.ucuenca.wk.commons.impl;

import com.google.common.base.Optional;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;
import java.io.IOException;
import java.io.InputStream;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
//import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.tartarus.snowball.ext.PorterStemmer;

import java.io.StringReader;
import java.nio.charset.Charset;
//import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.apache.commons.io.IOUtils;

/**
 * @author Jose Luis Cullcay
 */
public class KeywordGeneratorImpl {
    
    // Similarity tolerance
    private Double tolerance = 0.8;
    //private String endpoint = "";
    //private String endpointUpdate = "http://localhost:8080/marmotta/sparql/update";
    
    //Logger and Lucene Analyzer
    private static final Logger LOGGER = LoggerFactory.getLogger(KeywordGeneratorImpl.class);
    //Para extraer keywords en Espanol usar SpanishAnalyzer, y para Ingles usar StandardAnalyzer
    private final StandardAnalyzer analyzer;
    //private final PorterStemmer stemmer ;
    private TranslationServiceImpl translator = new TranslationServiceImpl();

    public KeywordGeneratorImpl() {
        this(null);
        //Configuration parameters
        JsonObject config;
        try {
            InputStream resourceAsStream = this.getClass().getResourceAsStream("/config.cnf");
            String theString = IOUtils.toString(resourceAsStream, Charset.defaultCharset().toString());
            JsonParser parse = new JsonParser();
            config =  parse.parse(theString).getAsJsonObject();
            //Getting the configuration Parameters
            tolerance = Double.valueOf(config.get("tolerance").getAsString());
            //endpoint = config.get("endpoint").getAsString();
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(KeywordGeneratorImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public List<String> getKeywordsAuthor(String keys){
        List<String> lista1 = new ArrayList<>();
        try {
            //Distance d = new Distance();
            //WordSelector getkeywords = new WordSelector();
            String keyString = keys;//getkeywords.getKeywordsAuthor(keys);

            //load all languages:
            List<LanguageProfile> languageProfiles = new LanguageProfileReader().readAllBuiltIn();

            //build language detector:
            LanguageDetector languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
                    .withProfiles(languageProfiles)
                    .build();

            //create a text object factory
            TextObjectFactory textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();

            //Text to query:
            TextObject textObject;
            textObject = textObjectFactory.forText(keyString);

            //Detect the language
            Optional<LdLocale> lang = languageDetector.detect(textObject);
            String language = "es";
            if (lang.isPresent()) {
                language = lang.get().getLanguage();
            }
            //"es" para espanol y "en" para ingles.

            if ("es".equals(language)) {
                //translate the list of words
                JsonObject jsonKeys = translator.translate(keyString);
                keyString = jsonKeys.get("result").getAsString();
            }

            //Get the keywords with Lucece
            //KeywordGenerator keywordGenerator = new KeywordGeneratorImpl();
            String[] split = keyString.split(",");

            for (int i = 0; i < split.length; i++) {
                lista1.add(split[i].trim());
            }

        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(KeywordGeneratorImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return lista1;
    }
    
    public KeywordGeneratorImpl(Set<?> stopWords) {
        analyzer = stopWords == null ? new StandardAnalyzer(Version.LUCENE_36)  : new StandardAnalyzer(Version.LUCENE_36,stopWords);
        //stemmer = new PorterStemmer();
    }

    public Set<String> generateKeyWords(String content) {
        Set<String> keywords = new HashSet<>();
        TokenStream stream = analyzer.tokenStream("contents", new StringReader(content));
        analyzer.getStopwordSet();
        try {
            stream.reset();
            while(stream.incrementToken()) {
                String kw = stream.getAttribute(CharTermAttribute.class).toString();
                /*stemmer.setCurrent(kw);
                stemmer.stem();
                keywords.add(stemmer.getCurrent());*/
                keywords.add(kw);
            }
        }catch(Exception ex) {
            LOGGER.error(ex.getMessage());
        }finally {

            try {
                stream.end();
                stream.close();
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }
        return keywords;
    }
    
    public Set<String> generateKeyWords(String title, String abst) {
        String content = title + ". " + abst;
        return generateKeyWords(content);
    }

    public Double getTOLERANCE() {
        return tolerance;
    }

    /*public String getEndpoint() {
        return endpoint;
    }

    public String getEndpointUpdate() {
        return getEndpoint().replace("select", "update");
    }*/
    
    
}