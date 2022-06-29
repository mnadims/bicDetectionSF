From 86d4a61797cd8df5dc810ea3ebb9124b5f0e52a4 Mon Sep 17 00:00:00 2001
From: "Chris M. Hostetter" <hossman@apache.org>
Date: Wed, 11 Sep 2013 17:35:24 +0000
Subject: [PATCH] SOLR-5231: Fixed a bug with the behavior of BoolField that
 caused documents w/o a value for the field to act as if the value were true
 in functions if no other documents in the same index segment had a value of
 true.

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1521948 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |  5 ++++
 .../org/apache/solr/schema/BoolField.java     |  3 +-
 .../search/function/TestFunctionQuery.java    | 28 +++++++++++++++++++
 3 files changed, 35 insertions(+), 1 deletion(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index b3bdca90287..9c2f2e16276 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -225,6 +225,11 @@ Bug Fixes
 * SOLR-5227: Correctly fail schema initalization if a dynamicField is configured to
   be required, or have a default value.  (hossman)
 
* SOLR-5231: Fixed a bug with the behavior of BoolField that caused documents w/o
  a value for the field to act as if the value were true in functions if no other
  documents in the same index segment had a value of true.
  (Robert Muir, hossman, yonik)

 Optimizations
 ----------------------
 
diff --git a/solr/core/src/java/org/apache/solr/schema/BoolField.java b/solr/core/src/java/org/apache/solr/schema/BoolField.java
index 3704120a152..d7be93b91fd 100644
-- a/solr/core/src/java/org/apache/solr/schema/BoolField.java
++ b/solr/core/src/java/org/apache/solr/schema/BoolField.java
@@ -173,7 +173,8 @@ class BoolFieldSource extends ValueSource {
     // figure out what ord maps to true
     int nord = sindex.getValueCount();
     BytesRef br = new BytesRef();
    int tord = -1;
    // if no values in the segment, default trueOrd to something other then -1 (missing)
    int tord = -2;
     for (int i=0; i<nord; i++) {
       sindex.lookupOrd(i, br);
       if (br.length==1 && br.bytes[br.offset]=='T') {
diff --git a/solr/core/src/test/org/apache/solr/search/function/TestFunctionQuery.java b/solr/core/src/test/org/apache/solr/search/function/TestFunctionQuery.java
index 7778240910b..7068b1620c9 100644
-- a/solr/core/src/test/org/apache/solr/search/function/TestFunctionQuery.java
++ b/solr/core/src/test/org/apache/solr/search/function/TestFunctionQuery.java
@@ -723,4 +723,32 @@ public class TestFunctionQuery extends SolrTestCaseJ4 {
         , "/response/docs/[0]=={'a':1, 'b':2.0,'c':'X','d':'A'}");
   }
 
  public void testMissingFieldFunctionBehavior() throws Exception {
    clearIndex();
    // add a doc that has no values in any interesting fields
    assertU(adoc("id", "1"));
    assertU(commit());

    // it's important that these functions not only use fields that
    // out doc have no values for, but also that that no other doc ever added
    // to the index might have ever had a value for, so that the segment
    // term metadata doesn't exist
    
    for (String suffix : new String[] {"s", "b", "dt", "tdt",
                                       "i", "l", "f", "d", 
                                       "pi", "pl", "pf", "pd",
                                       "ti", "tl", "tf", "td"    }) {
      final String field = "no__vals____" + suffix;
      assertQ(req("q","id:1",
                  "fl","noval_if:if("+field+",42,-99)",
                  "fl","noval_def:def("+field+",-99)",
                  "fl","noval_not:not("+field+")",
                  "fl","noval_exists:exists("+field+")"),
              "//long[@name='noval_if']='-99'",
              "//long[@name='noval_def']='-99'",
              "//bool[@name='noval_not']='true'",
              "//bool[@name='noval_exists']='false'");
    }
  }

 }
- 
2.19.1.windows.1

