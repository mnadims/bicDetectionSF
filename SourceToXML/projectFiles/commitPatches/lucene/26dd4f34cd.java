From 26dd4f34cdeeaa41756774eadf25ca1a14a4b8b7 Mon Sep 17 00:00:00 2001
From: "Chris M. Hostetter" <hossman@apache.org>
Date: Mon, 13 Dec 2010 17:40:17 +0000
Subject: [PATCH] SOLR-1297: improvements to sort param parsing code so more
 fields with exentric names (that were supported for sorting in older versions
 of solr) will be checked for after attemptint to parse as a function

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1045253 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/solr/search/QueryParsing.java  | 203 ++++++++++++------
 .../search/function/TestFunctionQuery.java    |  52 ++++-
 2 files changed, 175 insertions(+), 80 deletions(-)

diff --git a/solr/src/java/org/apache/solr/search/QueryParsing.java b/solr/src/java/org/apache/solr/search/QueryParsing.java
index 146f6e9ce7e..128fc796af5 100644
-- a/solr/src/java/org/apache/solr/search/QueryParsing.java
++ b/solr/src/java/org/apache/solr/search/QueryParsing.java
@@ -67,6 +67,7 @@ public class QueryParsing {
   public static final String LOCALPARAM_START = "{!";
   public static final char LOCALPARAM_END = '}';
   public static final String DOCID = "_docid_";
  public static final String SCORE = "score";
 
   // true if the value was specified by the "v" param (i.e. v=myval, or v=$param)
   public static final String VAL_EXPLICIT = "__VAL_EXPLICIT__";
@@ -300,10 +301,11 @@ public class QueryParsing {
       while (sp.pos < sp.end) {
         sp.eatws();
 
        int start = sp.pos;
        final int start = sp.pos;
 
        // short circuit test for a really simple field name
         String field = sp.getId(null);
        ValueSource vs = null;
        ParseException qParserException = null;
 
         if (field == null || sp.ch() != ' ') {
           // let's try it as a function instead
@@ -311,89 +313,100 @@ public class QueryParsing {
 
           QParser parser = QParser.getParser(funcStr, FunctionQParserPlugin.NAME, req);
           Query q = null;
          if (parser instanceof FunctionQParser) {
            FunctionQParser fparser = (FunctionQParser)parser;
            fparser.setParseMultipleSources(false);
            fparser.setParseToEnd(false);

            q = fparser.getQuery();

            if (fparser.localParams != null) {
              if (fparser.valFollowedParams) {
          try {
            if (parser instanceof FunctionQParser) {
              FunctionQParser fparser = (FunctionQParser)parser;
              fparser.setParseMultipleSources(false);
              fparser.setParseToEnd(false);
              
              q = fparser.getQuery();
              
              if (fparser.localParams != null) {
                if (fparser.valFollowedParams) {
                  // need to find the end of the function query via the string parser
                  int leftOver = fparser.sp.end - fparser.sp.pos;
                  sp.pos = sp.end - leftOver;   // reset our parser to the same amount of leftover
                } else {
                  // the value was via the "v" param in localParams, so we need to find
                  // the end of the local params themselves to pick up where we left off
                  sp.pos = start + fparser.localParamsEnd;
                }
              } else {
                 // need to find the end of the function query via the string parser
                 int leftOver = fparser.sp.end - fparser.sp.pos;
                 sp.pos = sp.end - leftOver;   // reset our parser to the same amount of leftover
              } else {
                // the value was via the "v" param in localParams, so we need to find
                // the end of the local params themselves to pick up where we left off
                sp.pos = start + fparser.localParamsEnd;
               }
             } else {
              // need to find the end of the function query via the string parser
              int leftOver = fparser.sp.end - fparser.sp.pos;
              sp.pos = sp.end - leftOver;   // reset our parser to the same amount of leftover
            }
          } else {
            // A QParser that's not for function queries.
            // It must have been specified via local params.
            q = parser.getQuery();
              // A QParser that's not for function queries.
              // It must have been specified via local params.
              q = parser.getQuery();
 
            assert parser.getLocalParams() != null;
            sp.pos = start + parser.localParamsEnd;
          }
              assert parser.getLocalParams() != null;
              sp.pos = start + parser.localParamsEnd;
            }
 
          // OK, now we have our query.
          if (q instanceof FunctionQuery) {
            vs = ((FunctionQuery)q).getValueSource();
          } else {
            vs = new QueryValueSource(q, 0.0f);
            Boolean top = sp.getSortDirection();
            if (null != top) {
              // we have a Query and a valid direction
              if (q instanceof FunctionQuery) {
                lst.add(((FunctionQuery)q).getValueSource().getSortField(top));
              } else {
                lst.add((new QueryValueSource(q, 0.0f)).getSortField(top));
              }
              continue;
            }
          } catch (ParseException e) {
            // hang onto this in case the string isn't a full field name either
            qParserException = e;
           }
         }
 
        // now we have our field or value source, so find the sort order
        String order = sp.getId("Expected sort order asc/desc");
        boolean top;
        if ("desc".equals(order) || "top".equals(order)) {
          top = true;
        } else if ("asc".equals(order) || "bottom".equals(order)) {
          top = false;
        } else {
          throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Unknown sort order: " + order);
        }
        // if we made it here, we either have a "simple" field name,
        // or there was a problem parsing the string as a complex func/quer
 
        if (vs == null) {
          //we got the order, now deal with the sort
          if ("score".equals(field)) {
            if (top) {
              lst.add(SortField.FIELD_SCORE);
            } else {
              lst.add(new SortField(null, SortField.SCORE, true));
            }
          } else if (DOCID.equals(field)) {
            lst.add(new SortField(null, SortField.DOC, top));
        if (field == null) {
          // try again, simple rules for a field name with no whitespace
          sp.pos = start;
          field = sp.getSimpleString();
        }
        Boolean top = sp.getSortDirection();
        if (null == top) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, 
                                    "Can't determine Sort Order: " + sp);
        }
        
        if (SCORE.equals(field)) {
          if (top) {
            lst.add(SortField.FIELD_SCORE);
           } else {
            //See if we have a Field first, then see if it is a function, then throw an exception
            // getField could throw an exception if the name isn't found
            SchemaField sf = req.getSchema().getField(field);

            // TODO: remove this - it should be up to the FieldType
            if (!sf.indexed()) {
              throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "can not sort on unindexed field: " + field);
            }

            lst.add(sf.getType().getSortField(sf, top));


            lst.add(new SortField(null, SortField.SCORE, true));
           }
        } else if (DOCID.equals(field)) {
          lst.add(new SortField(null, SortField.DOC, top));
         } else {
          lst.add(vs.getSortField(top));
        }

        sp.eatws();
        if (sp.pos < sp.end) {
          sp.expect(",");
          // try to find the field
          SchemaField sf = req.getSchema().getFieldOrNull(field);
          if (null == sf) {
            if (null != qParserException) {
              throw new SolrException
                (SolrException.ErrorCode.BAD_REQUEST,
                 "sort param could not be parsed as a query, and is not a "+
                 "field that exists in the index: " + field,
                 qParserException);
            }
            throw new SolrException
              (SolrException.ErrorCode.BAD_REQUEST,
               "sort param fiedl can't be found: " + field);
          }
              
          // TODO: remove this - it should be up to the FieldType
          if (!sf.indexed()) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, 
                                    "can not sort on unindexed field: " 
                                    + field);
          }
          lst.add(sf.getType().getSortField(sf, top));
         }

       }
 
     } catch (ParseException e) {
@@ -767,6 +780,56 @@ public class QueryParsing {
       return null;
     }
 
    /**
     * Skips leading whitespace and returns whatever sequence of non 
     * whitespace it can find (or hte empty string)
     */
    String getSimpleString() {
      eatws();
      int startPos = pos;
      char ch;
      while (pos < end) {
        ch = val.charAt(pos);
        if (Character.isWhitespace(ch)) break;
        pos++;
      }
      return val.substring(startPos, pos);
    }

    /**
     * Sort direction or null if current position does not inidcate a 
     * sort direction. (True is desc, False is asc).  
     * Position is advanced to after the comma (or end) when result is non null 
     */
    Boolean getSortDirection() throws ParseException {
      final int startPos = pos;
      final String order = getId(null);

      Boolean top = null;

      if (null != order) {
        if ("desc".equals(order) || "top".equals(order)) {
          top = true;
        } else if ("asc".equals(order) || "bottom".equals(order)) {
          top = false;
        }

        // it's not a legal direction if more stuff comes after it
        eatws();
        final char c = ch();
        if (0 == c) {
          // :NOOP
        } else if (',' == c) {
          pos++;
        } else {
          top = null;
        }
      }

      if (null == top) pos = startPos; // no direction, reset
      return top;
    }

     // return null if not a string
     String getQuotedString() throws ParseException {
       eatws();
diff --git a/solr/src/test/org/apache/solr/search/function/TestFunctionQuery.java b/solr/src/test/org/apache/solr/search/function/TestFunctionQuery.java
index 0e7c4512e7c..2b548a12475 100755
-- a/solr/src/test/org/apache/solr/search/function/TestFunctionQuery.java
++ b/solr/src/test/org/apache/solr/search/function/TestFunctionQuery.java
@@ -365,41 +365,48 @@ public class TestFunctionQuery extends SolrTestCaseJ4 {
 
   @Test
   public void testSortByFunc() throws Exception {
    assertU(adoc("id", "1", "x_i", "100"));
    assertU(adoc("id", "2", "x_i", "300"));
    assertU(adoc("id", "3", "x_i", "200"));
    assertU(adoc("id", "1", "const_s", "xx", "x_i", "100", "1_s", "a"));
    assertU(adoc("id", "2", "const_s", "xx", "x_i", "300", "1_s", "c"));
    assertU(adoc("id", "3", "const_s", "xx", "x_i", "200", "1_s", "b"));
     assertU(commit());
 
     String desc = "/response/docs==[{'x_i':300},{'x_i':200},{'x_i':100}]";
     String asc =  "/response/docs==[{'x_i':100},{'x_i':200},{'x_i':300}]";
 
    String threeonetwo =  "/response/docs==[{'x_i':200},{'x_i':100},{'x_i':300}]";

     String q = "id:[1 TO 3]";
     assertJQ(req("q",q,  "fl","x_i", "sort","add(x_i,x_i) desc")
       ,desc
     );
 
     // param sub of entire function
    assertJQ(req("q",q,  "fl","x_i", "sort", "$x asc", "x","add(x_i,x_i)")
    assertJQ(req("q",q,  "fl","x_i", "sort", "const_s asc, $x asc", "x","add(x_i,x_i)")
       ,asc
     );
 
     // multiple functions
    assertJQ(req("q",q,  "fl","x_i", "sort", "$x asc, $y desc", "x", "5", "y","add(x_i,x_i)")
    assertJQ(req("q",q,  "fl","x_i", "sort", "$x asc, const_s asc, $y desc", "x", "5", "y","add(x_i,x_i)")
       ,desc
     );
 
     // multiple functions inline
    assertJQ(req("q",q,  "fl","x_i", "sort", "add( 10 , 10 ) asc, add(x_i , $const) desc", "const","50")
    assertJQ(req("q",q,  "fl","x_i", "sort", "add( 10 , 10 ) asc, const_s asc, add(x_i , $const) desc", "const","50")
       ,desc
     );
 
     // test function w/ local params + func inline
     assertJQ(req("q",q,  "fl","x_i", "sort", "{!key=foo}add(x_i,x_i) desc")
      ,desc
    assertJQ(req("q",q,  "fl","x_i", 
                 "sort", "const_s asc, {!key=foo}add(x_i,x_i) desc")
             ,desc
    );
    assertJQ(req("q",q,  "fl","x_i", 
                 "sort", "{!key=foo}add(x_i,x_i) desc, const_s asc")
             ,desc
     );
 
     // test multiple functions w/ local params + func inline
    assertJQ(req("q",q,  "fl","x_i", "sort", "{!key=bar}add(10,20) asc, {!key=foo}add(x_i,x_i) desc")
    assertJQ(req("q",q,  "fl","x_i", "sort", "{!key=bar}add(10,20) asc, const_s asc, {!key=foo}add(x_i,x_i) desc")
       ,desc
     );
 
@@ -407,6 +414,31 @@ public class TestFunctionQuery extends SolrTestCaseJ4 {
     assertJQ(req("q",q,  "fl","x_i", "sort", "{!key=bar v=$s1} asc, {!key=foo v=$s2} desc", "s1","add(3,4)", "s2","add(x_i,5)")
       ,desc
     );

    // no space between inlined localparams and sort order
    assertJQ(req("q",q,  "fl","x_i", "sort", "{!key=bar v=$s1}asc,const_s asc,{!key=foo v=$s2}desc", "s1","add(3,4)", "s2","add(x_i,5)")
      ,desc
    );

    // field name that isn't a legal java Identifier 
    // and starts with a number to trick function parser
    assertJQ(req("q",q,  "fl","x_i", "sort", "1_s asc")
             ,asc
    );

    // really ugly field name that isn't a java Id, and can't be 
    // parsed as a func, but sorted fine in Solr 1.4
    assertJQ(req("q",q,  "fl","x_i", 
                 "sort", "[]_s asc, {!key=foo}add(x_i,x_i) desc")
             ,desc
    );
    // use localparms to sort by a lucene query, then a function
    assertJQ(req("q",q,  "fl","x_i", 
                 "sort", "{!lucene v='id:3'}desc, {!key=foo}add(x_i,x_i) asc")
             ,threeonetwo
    );


   }
 
   @Test
@@ -478,4 +510,4 @@ public class TestFunctionQuery extends SolrTestCaseJ4 {
   }
 
 
}
\ No newline at end of file
}
- 
2.19.1.windows.1

