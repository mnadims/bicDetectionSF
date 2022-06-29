From 571264bcc007a3d853f678dd7ac5b529644e938d Mon Sep 17 00:00:00 2001
From: jdyer1 <jdyer@apache.org>
Date: Mon, 8 May 2017 13:26:42 -0500
Subject: [PATCH] SOLR-10522: Revert "SOLR-9972: SpellCheckComponent collations
 and suggestions returned as a JSON object rather than a list"

This reverts commit 4cd3d15da8ef77ef50e2bda91ed6d3c6e87b5426.
--
 .../component/SpellCheckComponent.java        | 12 +--
 .../component/SpellCheckComponentTest.java    | 79 ++++++++++---------
 2 files changed, 50 insertions(+), 41 deletions(-)

diff --git a/solr/core/src/java/org/apache/solr/handler/component/SpellCheckComponent.java b/solr/core/src/java/org/apache/solr/handler/component/SpellCheckComponent.java
index 4e3cd125c27..eee36ccce5a 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/SpellCheckComponent.java
++ b/solr/core/src/java/org/apache/solr/handler/component/SpellCheckComponent.java
@@ -199,7 +199,8 @@ public class SpellCheckComponent extends SearchComponent implements SolrCoreAwar
         boolean isCorrectlySpelled = hits > (maxResultsForSuggest==null ? 0 : maxResultsForSuggest);
 
         NamedList response = new SimpleOrderedMap();
        response.add("suggestions", toNamedList(shardRequest, spellingResult, q, extendedResults));
        NamedList suggestions = toNamedList(shardRequest, spellingResult, q, extendedResults);
        response.add("suggestions", suggestions);
 
         if (extendedResults) {
           response.add("correctlySpelled", isCorrectlySpelled);
@@ -299,7 +300,7 @@ public class SpellCheckComponent extends SearchComponent implements SolrCoreAwar
     //even in cases when the internal rank is the same.
     Collections.sort(collations);
 
    NamedList collationList = new SimpleOrderedMap();
    NamedList collationList = new NamedList();
     for (SpellCheckCollation collation : collations) {
       if (collationExtendedResults) {
         NamedList extendedResult = new SimpleOrderedMap();
@@ -423,7 +424,8 @@ public class SpellCheckComponent extends SearchComponent implements SolrCoreAwar
 
     NamedList response = new SimpleOrderedMap();
 
    response.add("suggestions", toNamedList(false, result, origQuery, extendedResults));
    NamedList suggestions = toNamedList(false, result, origQuery, extendedResults);
    response.add("suggestions", suggestions);
 
     if (extendedResults) {
       response.add("correctlySpelled", isCorrectlySpelled);
@@ -434,7 +436,7 @@ public class SpellCheckComponent extends SearchComponent implements SolrCoreAwar
           .toArray(new SpellCheckCollation[mergeData.collations.size()]);
       Arrays.sort(sortedCollations);
 
      NamedList collations = new SimpleOrderedMap();
      NamedList collations = new NamedList();
       int i = 0;
       while (i < maxCollations && i < sortedCollations.length) {
         SpellCheckCollation collation = sortedCollations[i];
@@ -634,7 +636,7 @@ public class SpellCheckComponent extends SearchComponent implements SolrCoreAwar
 
   protected NamedList toNamedList(boolean shardRequest,
       SpellingResult spellingResult, String origQuery, boolean extendedResults) {
    NamedList result = new SimpleOrderedMap();
    NamedList result = new NamedList();
     Map<Token,LinkedHashMap<String,Integer>> suggestions = spellingResult
         .getSuggestions();
     boolean hasFreqInfo = spellingResult.hasTokenFrequencyInfo();
diff --git a/solr/core/src/test/org/apache/solr/handler/component/SpellCheckComponentTest.java b/solr/core/src/test/org/apache/solr/handler/component/SpellCheckComponentTest.java
index 473153aec71..37d02d9be87 100644
-- a/solr/core/src/test/org/apache/solr/handler/component/SpellCheckComponentTest.java
++ b/solr/core/src/test/org/apache/solr/handler/component/SpellCheckComponentTest.java
@@ -82,62 +82,77 @@ public class SpellCheckComponentTest extends SolrTestCaseJ4 {
   public void testMaximumResultsForSuggest() throws Exception {
    assertJQ(req("qt",rh, SpellCheckComponent.COMPONENT_NAME, "true", SpellingParams.SPELLCHECK_BUILD, "true", "q","lowerfilt:(this OR brwn)",
         SpellingParams.SPELLCHECK_COUNT,"5", SpellingParams.SPELLCHECK_EXTENDED_RESULTS,"false", SpellingParams.SPELLCHECK_MAX_RESULTS_FOR_SUGGEST, "7")
        ,"/spellcheck/suggestions/brwn/numFound==1"
        ,"/spellcheck/suggestions/[0]=='brwn'"
        ,"/spellcheck/suggestions/[1]/numFound==1"
      );

   assertJQ(req("qt",rh, SpellCheckComponent.COMPONENT_NAME, "true", SpellingParams.SPELLCHECK_BUILD, "true", "q","lowerfilt:(this OR brwn)",
       SpellingParams.SPELLCHECK_COUNT,"5", SpellingParams.SPELLCHECK_EXTENDED_RESULTS,"false", SpellingParams.SPELLCHECK_MAX_RESULTS_FOR_SUGGEST, "6")
       ,"/spellcheck/suggestions=={}");
   // there should have been no suggestions (6<7)

    try {
      assertJQ(req("qt",rh, SpellCheckComponent.COMPONENT_NAME, "true", SpellingParams.SPELLCHECK_BUILD, "true", "q","lowerfilt:(this OR brwn)",
          SpellingParams.SPELLCHECK_COUNT,"5", SpellingParams.SPELLCHECK_EXTENDED_RESULTS,"false", SpellingParams.SPELLCHECK_MAX_RESULTS_FOR_SUGGEST, "6")
          ,"/spellcheck/suggestions/[1]/numFound==1"
       );
      fail("there should have been no suggestions (6<7)");
    } catch(Exception e) {
      //correctly threw exception
    }
     assertJQ(req("qt",rh, SpellCheckComponent.COMPONENT_NAME, "true", SpellingParams.SPELLCHECK_BUILD, "true", "q","lowerfilt:(this OR brwn)",
         "fq", "id:[0 TO 9]", /*returns 10, less selective */ "fq", "lowerfilt:th*", /* returns 8, most selective */
         SpellingParams.SPELLCHECK_COUNT,"5", SpellingParams.SPELLCHECK_EXTENDED_RESULTS,"false", SpellingParams.SPELLCHECK_MAX_RESULTS_FOR_SUGGEST, ".90")
        ,"/spellcheck/suggestions/brwn/numFound==1"
        ,"/spellcheck/suggestions/[0]=='brwn'"
        ,"/spellcheck/suggestions/[1]/numFound==1"
      );

    assertJQ(req("qt",rh, SpellCheckComponent.COMPONENT_NAME, "true", SpellingParams.SPELLCHECK_BUILD, "true", "q","lowerfilt:(this OR brwn)",
        "fq", "id:[0 TO 9]", /*returns 10, less selective */ "fq", "lowerfilt:th*", /* returns 8, most selective */
        SpellingParams.SPELLCHECK_COUNT,"5", SpellingParams.SPELLCHECK_EXTENDED_RESULTS,"false", SpellingParams.SPELLCHECK_MAX_RESULTS_FOR_SUGGEST, ".80")
        ,"/spellcheck/suggestions=={}");
    // there should have been no suggestions ((.8 * 8)<7)
    try {
      assertJQ(req("qt",rh, SpellCheckComponent.COMPONENT_NAME, "true", SpellingParams.SPELLCHECK_BUILD, "true", "q","lowerfilt:(this OR brwn)",
          "fq", "id:[0 TO 9]", /*returns 10, less selective */ "fq", "lowerfilt:th*", /* returns 8, most selective */
          SpellingParams.SPELLCHECK_COUNT,"5", SpellingParams.SPELLCHECK_EXTENDED_RESULTS,"false", SpellingParams.SPELLCHECK_MAX_RESULTS_FOR_SUGGEST, ".80")
          ,"/spellcheck/suggestions/[1]/numFound==1"
       );
      fail("there should have been no suggestions ((.8 * 8)<7)");
    } catch(Exception e) {
      //correctly threw exception
    }
     
     
     assertJQ(req("qt",rh, SpellCheckComponent.COMPONENT_NAME, "true", SpellingParams.SPELLCHECK_BUILD, "true", "q","lowerfilt:(this OR brwn)",
         "fq", "id:[0 TO 9]", SpellingParams.SPELLCHECK_MAX_RESULTS_FOR_SUGGEST_FQ, "id:[0 TO 9]", 
         SpellingParams.SPELLCHECK_COUNT,"5", SpellingParams.SPELLCHECK_EXTENDED_RESULTS,"false", SpellingParams.SPELLCHECK_MAX_RESULTS_FOR_SUGGEST, ".70")
        ,"/spellcheck/suggestions/brwn/numFound==1"
        ,"/spellcheck/suggestions/[0]=='brwn'"
        ,"/spellcheck/suggestions/[1]/numFound==1"
      );

    assertJQ(req("qt",rh, SpellCheckComponent.COMPONENT_NAME, "true", SpellingParams.SPELLCHECK_BUILD, "true", "q","lowerfilt:(this OR brwn)",
        "fq", "id:[0 TO 9]", SpellingParams.SPELLCHECK_MAX_RESULTS_FOR_SUGGEST_FQ, "lowerfilt:th*", 
        SpellingParams.SPELLCHECK_COUNT,"5", SpellingParams.SPELLCHECK_EXTENDED_RESULTS,"false", SpellingParams.SPELLCHECK_MAX_RESULTS_FOR_SUGGEST, ".64")
        ,"/spellcheck/suggestions=={}");
    // there should have been no suggestions ((.64 * 10)<7)
    try {
      assertJQ(req("qt",rh, SpellCheckComponent.COMPONENT_NAME, "true", SpellingParams.SPELLCHECK_BUILD, "true", "q","lowerfilt:(this OR brwn)",
          "fq", "id:[0 TO 9]", SpellingParams.SPELLCHECK_MAX_RESULTS_FOR_SUGGEST_FQ, "lowerfilt:th*", 
          SpellingParams.SPELLCHECK_COUNT,"5", SpellingParams.SPELLCHECK_EXTENDED_RESULTS,"false", SpellingParams.SPELLCHECK_MAX_RESULTS_FOR_SUGGEST, ".64")
          ,"/spellcheck/suggestions/[1]/numFound==1"
       );
      fail("there should have been no suggestions ((.64 * 10)<7)");
    } catch(Exception e) {
      //correctly threw exception
    }
   } 
   
   @Test
   public void testExtendedResultsCount() throws Exception {
     assertJQ(req("qt",rh, SpellCheckComponent.COMPONENT_NAME, "true", SpellingParams.SPELLCHECK_BUILD, "true", "q","bluo", SpellingParams.SPELLCHECK_COUNT,"5", SpellingParams.SPELLCHECK_EXTENDED_RESULTS,"false")
       ,"/spellcheck/suggestions/bluo/numFound==5"
       ,"/spellcheck/suggestions/[0]=='bluo'"
       ,"/spellcheck/suggestions/[1]/numFound==5"
     );
 
     assertJQ(req("qt",rh, SpellCheckComponent.COMPONENT_NAME, "true", "q","bluo", SpellingParams.SPELLCHECK_COUNT,"3", SpellingParams.SPELLCHECK_EXTENDED_RESULTS,"true")
       ,"/spellcheck/suggestions/bluo/suggestion==[{'word':'blud','freq':1}, {'word':'blue','freq':1}, {'word':'blee','freq':1}]"
       ,"/spellcheck/suggestions/[1]/suggestion==[{'word':'blud','freq':1}, {'word':'blue','freq':1}, {'word':'blee','freq':1}]"
     );
   }
 
   @Test
   public void test() throws Exception {
     assertJQ(req("qt",rh, SpellCheckComponent.COMPONENT_NAME, "true", "q","documemt")
       ,"/spellcheck=={'suggestions':{'documemt':{'numFound':1,'startOffset':0,'endOffset':8,'suggestion':['document']}}}"
       ,"/spellcheck=={'suggestions':['documemt',{'numFound':1,'startOffset':0,'endOffset':8,'suggestion':['document']}]}"
     );
   }
   
   @Test
   public void testNumericQuery() throws Exception {
     assertJQ(req("qt",rh, SpellCheckComponent.COMPONENT_NAME, "true", "q","12346")
       ,"/spellcheck=={'suggestions':{'12346':{'numFound':1,'startOffset':0,'endOffset':5,'suggestion':['12345']}}}"
       ,"/spellcheck=={'suggestions':['12346',{'numFound':1,'startOffset':0,'endOffset':5,'suggestion':['12345']}]}"
     );
   }
 
@@ -171,21 +186,13 @@ public class SpellCheckComponentTest extends SolrTestCaseJ4 {
   @Test
   public void testCollateExtendedResultsWithJsonNl() throws Exception {
     final String q = "documemtsss broens";
    final String jsonNl = (random().nextBoolean() ? "map" : "arrntv");
    final String jsonNl = "map";
     final boolean collateExtendedResults = random().nextBoolean();
     final List<String> testsList = new ArrayList<String>();
     if (collateExtendedResults) {
       testsList.add("/spellcheck/collations/collation/collationQuery=='document brown'");
       testsList.add("/spellcheck/collations/collation/hits==0");
       switch (jsonNl) {
        case "arrntv":
          testsList.add("/spellcheck/collations/collation/misspellingsAndCorrections/[0]/name=='documemtsss'");
          testsList.add("/spellcheck/collations/collation/misspellingsAndCorrections/[0]/type=='str'");
          testsList.add("/spellcheck/collations/collation/misspellingsAndCorrections/[0]/value=='document'");
          testsList.add("/spellcheck/collations/collation/misspellingsAndCorrections/[1]/name=='broens'");
          testsList.add("/spellcheck/collations/collation/misspellingsAndCorrections/[1]/type=='str'");
          testsList.add("/spellcheck/collations/collation/misspellingsAndCorrections/[1]/value=='brown'");
          break;
         case "map":
           testsList.add("/spellcheck/collations/collation/misspellingsAndCorrections/documemtsss=='document'");
           testsList.add("/spellcheck/collations/collation/misspellingsAndCorrections/broens=='brown'");
@@ -304,11 +311,11 @@ public class SpellCheckComponentTest extends SolrTestCaseJ4 {
         //while "document" is present.
 
         assertJQ(req("qt",rh, SpellCheckComponent.COMPONENT_NAME, "true", "q","documenq", SpellingParams.SPELLCHECK_DICT, "threshold", SpellingParams.SPELLCHECK_COUNT,"5", SpellingParams.SPELLCHECK_EXTENDED_RESULTS,"true")
            ,"/spellcheck/suggestions/documenq/suggestion==[{'word':'document','freq':2}]"
            ,"/spellcheck/suggestions/[1]/suggestion==[{'word':'document','freq':2}]"
         );
 
         assertJQ(req("qt",rh, SpellCheckComponent.COMPONENT_NAME, "true", "q","documenq", SpellingParams.SPELLCHECK_DICT, "threshold_direct", SpellingParams.SPELLCHECK_COUNT,"5", SpellingParams.SPELLCHECK_EXTENDED_RESULTS,"true")
            ,"/spellcheck/suggestions/documenq/suggestion==[{'word':'document','freq':2}]"
            ,"/spellcheck/suggestions/[1]/suggestion==[{'word':'document','freq':2}]"
         );
 
         //TODO:  how do we make this into a 1-liner using "assertQ()" ???
- 
2.19.1.windows.1

