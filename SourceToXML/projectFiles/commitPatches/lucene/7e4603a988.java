From 7e4603a9888847fbd01035a93173ee504fc91268 Mon Sep 17 00:00:00 2001
From: Erik Hatcher <ehatcher@apache.org>
Date: Wed, 13 Aug 2014 12:56:13 +0000
Subject: [PATCH] SOLR-6062: Fix undesirable edismax query parser effect
 (introduced in SOLR-2058) in how phrase queries

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1617719 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |  3 +
 .../solr/search/ExtendedDismaxQParser.java    | 88 ++++++++++++++-----
 .../solr/search/TestExtendedDismaxParser.java | 36 +++++---
 3 files changed, 91 insertions(+), 36 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 3beed7735a2..3128a5b99e8 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -262,6 +262,9 @@ Bug Fixes
 
 * SOLR-6347: DELETEREPLICA throws a NPE while removing the last Replica in a Custom sharded collection.
 
* SOLR-6062: Fix undesirable edismax query parser effect (introduced in SOLR-2058) in how phrase queries
  generated from pf, pf2, and pf3 are merged into the main query.  (Michael Dodsworth via ehatcher)

 Optimizations
 ---------------------
 
diff --git a/solr/core/src/java/org/apache/solr/search/ExtendedDismaxQParser.java b/solr/core/src/java/org/apache/solr/search/ExtendedDismaxQParser.java
index 9d21150f070..dd014661b61 100644
-- a/solr/core/src/java/org/apache/solr/search/ExtendedDismaxQParser.java
++ b/solr/core/src/java/org/apache/solr/search/ExtendedDismaxQParser.java
@@ -17,6 +17,9 @@
 
 package org.apache.solr.search;
 
import com.google.common.base.Function;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.core.StopFilterFactory;
 import org.apache.lucene.analysis.util.TokenFilterFactory;
@@ -43,10 +46,12 @@ import org.apache.solr.schema.FieldType;
 import org.apache.solr.util.SolrPluginUtils;
 
 import java.util.ArrayList;
import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
import java.util.LinkedHashMap;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
@@ -57,15 +62,34 @@ import java.util.Set;
  * See Wiki page http://wiki.apache.org/solr/ExtendedDisMax
  */
 public class ExtendedDismaxQParser extends QParser {
  
  

   /**
    * A field we can't ever find in any schema, so we can safely tell
    * DisjunctionMaxQueryParser to use it as our defaultField, and
    * map aliases from it to any field in our schema.
    */
   private static String IMPOSSIBLE_FIELD_NAME = "\uFFFC\uFFFC\uFFFC";
  

  /**
   * Helper function which returns the specified {@link FieldParams}' {@link FieldParams#getWordGrams()} value.
   */
  private static final Function<FieldParams, Integer> WORD_GRAM_EXTRACTOR = new Function<FieldParams, Integer>() {
    @Override
    public Integer apply(FieldParams input) {
      return input.getWordGrams();
    }
  };

  /**
   * Helper function which returns the specified {@link FieldParams}' {@link FieldParams#getSlop()} value.
   */
  private static final Function<FieldParams, Integer> PHRASE_SLOP_EXTRACTOR = new Function<FieldParams, Integer>() {
    @Override
    public Integer apply(FieldParams input) {
      return input.getSlop();
    }
  };

   /** shorten the class references for utilities */
   private static class U extends SolrPluginUtils {
     /* :NOOP */
@@ -212,21 +236,28 @@ public class ExtendedDismaxQParser extends QParser {
         if (clause.field != null || clause.isPhrase) continue;
         // check for keywords "AND,OR,TO"
         if (clause.isBareWord()) {
          String s = clause.val.toString();
          String s = clause.val;
           // avoid putting explicit operators in the phrase query
           if ("OR".equals(s) || "AND".equals(s) || "NOT".equals(s) || "TO".equals(s)) continue;
         }
         normalClauses.add(clause);
       }
      
      // full phrase and shingles
      for (FieldParams phraseField: allPhraseFields) {
        Map<String,Float> pf = new HashMap<>(1);
        pf.put(phraseField.getField(),phraseField.getBoost());
        addShingledPhraseQueries(query, normalClauses, pf,   
            phraseField.getWordGrams(),config.tiebreaker, phraseField.getSlop());

      // create a map of {wordGram, [phraseField]}
      Multimap<Integer, FieldParams> phraseFieldsByWordGram = Multimaps.index(allPhraseFields, WORD_GRAM_EXTRACTOR);

      // for each {wordGram, [phraseField]} entry, create and add shingled field queries to the main user query
      for (Map.Entry<Integer, Collection<FieldParams>> phraseFieldsByWordGramEntry : phraseFieldsByWordGram.asMap().entrySet()) {

        // group the fields within this wordGram collection by their associated slop (it's possible that the same
        // field appears multiple times for the same wordGram count but with different slop values. In this case, we
        // should take the *sum* of those phrase queries, rather than the max across them).
        Multimap<Integer, FieldParams> phraseFieldsBySlop = Multimaps.index(phraseFieldsByWordGramEntry.getValue(), PHRASE_SLOP_EXTRACTOR);
        for (Map.Entry<Integer, Collection<FieldParams>> phraseFieldsBySlopEntry : phraseFieldsBySlop.asMap().entrySet()) {
          addShingledPhraseQueries(query, normalClauses, phraseFieldsBySlopEntry.getValue(),
              phraseFieldsByWordGramEntry.getKey(), config.tiebreaker, phraseFieldsBySlopEntry.getKey());
        }
       }
      
     }
   }
 
@@ -493,14 +524,13 @@ public class ExtendedDismaxQParser extends QParser {
    * @param fields Field =&gt; boost mappings for the phrase queries
    * @param shingleSize how big the phrases should be, 0 means a single phrase
    * @param tiebreaker tie breaker value for the DisjunctionMaxQueries
   * @param slop slop value for the constructed phrases
    */
   protected void addShingledPhraseQueries(final BooleanQuery mainQuery, 
       final List<Clause> clauses,
      final Map<String,Float> fields,
      final Collection<FieldParams> fields,
       int shingleSize,
       final float tiebreaker,
      final int slop) 
      final int slop)
           throws SyntaxError {
     
     if (null == fields || fields.isEmpty() || 
@@ -509,12 +539,12 @@ public class ExtendedDismaxQParser extends QParser {
     
     if (0 == shingleSize) shingleSize = clauses.size();
     
    final int goat = shingleSize-1; // :TODO: better name for var?
    final int lastClauseIndex = shingleSize-1;
     
     StringBuilder userPhraseQuery = new StringBuilder();
    for (int i=0; i < clauses.size() - goat; i++) {
    for (int i=0; i < clauses.size() - lastClauseIndex; i++) {
       userPhraseQuery.append('"');
      for (int j=0; j <= goat; j++) {
      for (int j=0; j <= lastClauseIndex; j++) {
         userPhraseQuery.append(clauses.get(i + j).val);
         userPhraseQuery.append(' ');
       }
@@ -524,8 +554,8 @@ public class ExtendedDismaxQParser extends QParser {
     
     /* for parsing sloppy phrases using DisjunctionMaxQueries */
     ExtendedSolrQueryParser pp = createEdismaxQueryParser(this, IMPOSSIBLE_FIELD_NAME);
    
    pp.addAlias(IMPOSSIBLE_FIELD_NAME, tiebreaker, fields);

    pp.addAlias(IMPOSSIBLE_FIELD_NAME, tiebreaker, getFieldBoosts(fields));
     pp.setPhraseSlop(slop);
     pp.setRemoveStopFilter(true);  // remove stop filter and keep stopwords
     
@@ -559,8 +589,20 @@ public class ExtendedDismaxQParser extends QParser {
       mainQuery.add(phrase, BooleanClause.Occur.SHOULD);
     }
   }
  
  

  /**
   * @return a {fieldName, fieldBoost} map for the given fields.
   */
  private Map<String, Float> getFieldBoosts(Collection<FieldParams> fields) {
    Map<String, Float> fieldBoostMap = new LinkedHashMap<>(fields.size());

    for (FieldParams field : fields) {
      fieldBoostMap.put(field.getField(), field.getBoost());
    }

    return fieldBoostMap;
  }

   @Override
   public String[] getDefaultHighlightFields() {
     return config.queryFields.keySet().toArray(new String[0]);
@@ -1221,7 +1263,7 @@ public class ExtendedDismaxQParser extends QParser {
         return null;
       }
     }
    

     private Analyzer noStopwordFilterAnalyzer(String fieldName) {
       FieldType ft = parser.getReq().getSchema().getFieldType(fieldName);
       Analyzer qa = ft.getQueryAnalyzer();
diff --git a/solr/core/src/test/org/apache/solr/search/TestExtendedDismaxParser.java b/solr/core/src/test/org/apache/solr/search/TestExtendedDismaxParser.java
index e4436e3771b..0fe410aac2c 100644
-- a/solr/core/src/test/org/apache/solr/search/TestExtendedDismaxParser.java
++ b/solr/core/src/test/org/apache/solr/search/TestExtendedDismaxParser.java
@@ -32,7 +32,6 @@ import org.apache.solr.common.SolrException;
 import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.util.AbstractSolrTestCase;
 import org.apache.solr.util.SolrPluginUtils;
 import org.junit.BeforeClass;
 import org.junit.Test;
@@ -683,7 +682,7 @@ public class TestExtendedDismaxParser extends SolrTestCaseJ4 {
     assertU(adoc("id", "s0", "phrase_sw", "foo bar a b c", "boost_d", "1.0"));    
     assertU(adoc("id", "s1", "phrase_sw", "foo a bar b c", "boost_d", "2.0"));    
     assertU(adoc("id", "s2", "phrase_sw", "foo a b bar c", "boost_d", "3.0"));    
    assertU(adoc("id", "s3", "phrase_sw", "foo a b c bar", "boost_d", "4.0"));    
    assertU(adoc("id", "s3", "phrase_sw", "foo a b c bar", "boost_d", "4.0"));
     assertU(commit());
 
     assertQ("default order assumption wrong",
@@ -695,7 +694,7 @@ public class TestExtendedDismaxParser extends SolrTestCaseJ4 {
         "//doc[1]/str[@name='id'][.='s3']",
         "//doc[2]/str[@name='id'][.='s2']",
         "//doc[3]/str[@name='id'][.='s1']",
        "//doc[4]/str[@name='id'][.='s0']"); 
        "//doc[4]/str[@name='id'][.='s0']");
 
     assertQ("pf not working",
         req("q", "foo bar",
@@ -705,37 +704,37 @@ public class TestExtendedDismaxParser extends SolrTestCaseJ4 {
             "fl", "score,*",
             "defType", "edismax"),
         "//doc[1]/str[@name='id'][.='s0']");
    

     assertQ("pf2 not working",
        req("q",   "foo bar", 
        req("q",   "foo bar",
             "qf",  "phrase_sw",
             "pf2", "phrase_sw^10",
             "bf",  "boost_d",
             "fl",  "score,*",
             "defType", "edismax"),
        "//doc[1]/str[@name='id'][.='s0']"); 
        "//doc[1]/str[@name='id'][.='s0']");
 
     assertQ("pf3 not working",
        req("q",   "a b bar", 
        req("q",   "a b bar",
             "qf",  "phrase_sw",
             "pf3", "phrase_sw^10",
             "bf",  "boost_d",
             "fl",  "score,*",
             "defType", "edismax"),
        "//doc[1]/str[@name='id'][.='s2']"); 
        "//doc[1]/str[@name='id'][.='s2']");
 
     assertQ("ps not working for pf2",
        req("q",   "bar foo", 
        req("q",   "bar foo",
             "qf",  "phrase_sw",
             "pf2", "phrase_sw^10",
             "ps",  "2",
             "bf",  "boost_d",
             "fl",  "score,*",
             "defType", "edismax"),
        "//doc[1]/str[@name='id'][.='s0']"); 
        "//doc[1]/str[@name='id'][.='s0']");
 
     assertQ("ps not working for pf3",
        req("q",   "a bar foo", 
        req("q",   "a bar foo",
             "qf",  "phrase_sw",
             "pf3", "phrase_sw^10",
             "ps",  "3",
@@ -743,8 +742,8 @@ public class TestExtendedDismaxParser extends SolrTestCaseJ4 {
             "fl",  "score,*",
             "debugQuery",  "true",
             "defType", "edismax"),
        "//doc[1]/str[@name='id'][.='s1']"); 
    
        "//doc[1]/str[@name='id'][.='s1']");

     assertQ("ps/ps2/ps3 with default slop overrides not working",
         req("q", "zzzz xxxx cccc vvvv",
             "qf", "phrase_sw",
@@ -809,6 +808,17 @@ public class TestExtendedDismaxParser extends SolrTestCaseJ4 {
         "//str[@name='parsedquery'][contains(.,'phrase_sw:\"zzzz xxxx\"~2^22.0')]"
      );
 
    assertQ("phrase field queries spanning multiple fields should be within their own dismax queries",
        req("q", "aaaa bbbb cccc",
            "qf", "phrase_sw phrase1_sw",
            "pf2", "phrase_sw phrase1_sw",
            "pf3", "phrase_sw phrase1_sw",
            "defType", "edismax",
            "debugQuery", "true"),
        "//str[@name='parsedquery'][contains(.,'(phrase_sw:\"aaaa bbbb\" | phrase1_sw:\"aaaa bbbb\")')]",
        "//str[@name='parsedquery'][contains(.,'(phrase_sw:\"bbbb cccc\" | phrase1_sw:\"bbbb cccc\")')]",
        "//str[@name='parsedquery'][contains(.,'(phrase_sw:\"aaaa bbbb cccc\" | phrase1_sw:\"aaaa bbbb cccc\")')]"
    );
   }
 
   /**
- 
2.19.1.windows.1

