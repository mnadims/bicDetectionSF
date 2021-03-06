From 708cea853c8b8de1556209837e5f926c019c41df Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Thu, 30 Sep 2010 15:35:18 +0000
Subject: [PATCH] SOLR-1297: fix sort by function parsing

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1003107 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |   2 +-
 .../handler/component/QueryComponent.java     |   4 +-
 .../apache/solr/search/FunctionQParser.java   |   2 +-
 .../solr/search/LuceneQParserPlugin.java      |   2 +-
 .../java/org/apache/solr/search/QParser.java  |  43 ++-
 .../org/apache/solr/search/QueryParsing.java  | 271 +++++++++---------
 .../apache/solr/tst/OldRequestHandler.java    |   2 +-
 .../apache/solr/tst/TestRequestHandler.java   |   2 +-
 .../org/apache/solr/util/SolrPluginUtils.java |  20 +-
 .../apache/solr/search/QueryParsingTest.java  |  45 +--
 .../search/function/TestFunctionQuery.java    |  46 +++
 11 files changed, 262 insertions(+), 177 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index c67e2fbfe40..a6ae2419f39 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -118,7 +118,7 @@ New Features
 
 * SOLR-1625: Add regexp support for TermsComponent (Uri Boness via noble)
 
* SOLR-1297: Add sort by Function capability (gsingers)
* SOLR-1297: Add sort by Function capability (gsingers, yonik)
 
 * SOLR-1139: Add TermsComponent Query and Response Support in SolrJ (Matt Weber via shalin)
 
diff --git a/solr/src/java/org/apache/solr/handler/component/QueryComponent.java b/solr/src/java/org/apache/solr/handler/component/QueryComponent.java
index 9fe18f5e5be..c32eb1bafd7 100644
-- a/solr/src/java/org/apache/solr/handler/component/QueryComponent.java
++ b/solr/src/java/org/apache/solr/handler/component/QueryComponent.java
@@ -194,7 +194,9 @@ public class QueryComponent extends SearchComponent
         String[] funcs = params.getParams(GroupParams.GROUP_FUNC);
         String[] queries = params.getParams(GroupParams.GROUP_QUERY);
         String groupSortStr = params.get(GroupParams.GROUP_SORT);
        Sort groupSort = groupSortStr != null ? QueryParsing.parseSort(groupSortStr, req.getSchema()) : null;

        // TODO: don't use groupSort==null to test for the presense of a sort since "score desc" will normalize to null
        Sort groupSort = groupSortStr != null ? QueryParsing.parseSort(groupSortStr, req) : null;
 
         int limitDefault = cmd.getLen(); // this is normally from "rows"
         int docsPerGroupDefault = params.getInt(GroupParams.GROUP_LIMIT, 1);
diff --git a/solr/src/java/org/apache/solr/search/FunctionQParser.java b/solr/src/java/org/apache/solr/search/FunctionQParser.java
index a2312e1ab6d..c5e710acd54 100755
-- a/solr/src/java/org/apache/solr/search/FunctionQParser.java
++ b/solr/src/java/org/apache/solr/search/FunctionQParser.java
@@ -48,7 +48,7 @@ public class FunctionQParser extends QParser {
   }
 
   public void setParseToEnd(boolean parseToEnd) {
    this.parseMultipleSources = parseMultipleSources;
    this.parseToEnd = parseToEnd;
   }
 
   /** throw exception if there is extra stuff at the end of the parsed valuesource(s). */
diff --git a/solr/src/java/org/apache/solr/search/LuceneQParserPlugin.java b/solr/src/java/org/apache/solr/search/LuceneQParserPlugin.java
index 1c2e8dadcc3..0c8d1a16e4a 100755
-- a/solr/src/java/org/apache/solr/search/LuceneQParserPlugin.java
++ b/solr/src/java/org/apache/solr/search/LuceneQParserPlugin.java
@@ -117,7 +117,7 @@ class OldLuceneQParser extends LuceneQParser {
   public SortSpec getSort(boolean useGlobal) throws ParseException {
     SortSpec sort = super.getSort(useGlobal);
     if (sortStr != null && sortStr.length()>0 && sort.getSort()==null) {
      Sort oldSort = QueryParsing.parseSort(sortStr, getReq().getSchema());
      Sort oldSort = QueryParsing.parseSort(sortStr, getReq());
       if( oldSort != null ) {
         sort.sort = oldSort;
       }
diff --git a/solr/src/java/org/apache/solr/search/QParser.java b/solr/src/java/org/apache/solr/search/QParser.java
index c942ef57c88..52254858d17 100755
-- a/solr/src/java/org/apache/solr/search/QParser.java
++ b/solr/src/java/org/apache/solr/search/QParser.java
@@ -20,6 +20,7 @@ import org.apache.lucene.queryParser.ParseException;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.Sort;
 import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.MapSolrParams;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.common.util.StrUtils;
@@ -41,6 +42,10 @@ public abstract class QParser {
 
   protected Query query;
 
  protected String stringIncludingLocalParams;   // the original query string including any local params
  protected boolean valFollowedParams;           // true if the value "qstr" followed the localParams
  protected int localParamsEnd;                  // the position one past where the localParams ended 

   /**
    * Constructor for the QParser
    * @param qstr The part of the query string specific to this parser
@@ -214,7 +219,7 @@ public abstract class QParser {
 
     Sort sort = null;
     if( sortStr != null ) {
      sort = QueryParsing.parseSort(sortStr, req.getSchema());
      sort = QueryParsing.parseSort(sortStr, req);
     }
     return new SortSpec( sort, start, rows );
   }
@@ -241,7 +246,32 @@ public abstract class QParser {
    * then the prefix query parser will be used.
    */
   public static QParser getParser(String qstr, String defaultType, SolrQueryRequest req) throws ParseException {
    SolrParams localParams = QueryParsing.getLocalParams(qstr, req.getParams());
    // SolrParams localParams = QueryParsing.getLocalParams(qstr, req.getParams());

    String stringIncludingLocalParams = qstr;
    SolrParams localParams = null;
    SolrParams globalParams = req.getParams();
    boolean valFollowedParams = true;
    int localParamsEnd = -1;

    if (qstr != null && qstr.startsWith(QueryParsing.LOCALPARAM_START)) {
      Map<String, String> localMap = new HashMap<String, String>();
      localParamsEnd = QueryParsing.parseLocalParams(qstr, 0, localMap, globalParams);

      String val = localMap.get(QueryParsing.V);
      if (val != null) {
        // val was directly specified in localParams via v=<something> or v=$arg
        valFollowedParams = false;
      } else {
        // use the remainder of the string as the value
        valFollowedParams = true;
        val = qstr.substring(localParamsEnd);
        localMap.put(QueryParsing.V, val);
      }
      localParams = new MapSolrParams(localMap);
    }


     String type;
     
     if (localParams == null) {
@@ -254,7 +284,12 @@ public abstract class QParser {
     type = type==null ? QParserPlugin.DEFAULT_QTYPE : type;
 
     QParserPlugin qplug = req.getCore().getQueryPlugin(type);
    return qplug.createParser(qstr, localParams, req.getParams(), req);
  }                            
    QParser parser =  qplug.createParser(qstr, localParams, req.getParams(), req);

    parser.stringIncludingLocalParams = stringIncludingLocalParams;
    parser.valFollowedParams = valFollowedParams;
    parser.localParamsEnd = localParamsEnd;
    return parser;
  }
 
 }
diff --git a/solr/src/java/org/apache/solr/search/QueryParsing.java b/solr/src/java/org/apache/solr/search/QueryParsing.java
index 973e4ca1df8..62f538e0957 100644
-- a/solr/src/java/org/apache/solr/search/QueryParsing.java
++ b/solr/src/java/org/apache/solr/search/QueryParsing.java
@@ -38,10 +38,12 @@ import org.apache.solr.common.params.MapSolrParams;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.schema.FieldType;
 import org.apache.solr.schema.IndexSchema;
 import org.apache.solr.schema.SchemaField;
 import org.apache.solr.search.function.FunctionQuery;
import org.apache.solr.search.function.QueryValueSource;
 import org.apache.solr.search.function.ValueSource;
 
 import java.io.IOException;
@@ -66,6 +68,10 @@ public class QueryParsing {
   public static final char LOCALPARAM_END = '}';
   public static final String DOCID = "_docid_";
 
  // true if the value was specified by the "v" param (i.e. v=myval, or v=$param)
  public static final String VAL_EXPLICIT = "__VAL_EXPLICIT__";


   /**
    * Returns the "prefered" default operator for use by Query Parsers, 
    * based on the settings in the IndexSchema which may be overridden using 
@@ -253,20 +259,17 @@ public class QueryParsing {
     Map<String, String> localParams = new HashMap<String, String>();
     int start = QueryParsing.parseLocalParams(txt, 0, localParams, params);
 
    String val;
    if (start >= txt.length()) {
      // if the rest of the string is empty, check for "v" to provide the value
      val = localParams.get(V);
      val = val == null ? "" : val;
    } else {
    String val = localParams.get(V);
    if (val == null) {
       val = txt.substring(start);
      localParams.put(V, val);
    } else {
      // localParams.put(VAL_EXPLICIT, "true");
     }
    localParams.put(V, val);
     return new MapSolrParams(localParams);
   }
 
 

   /**
    * Returns null if the sortSpec is the standard sort desc.
    * <p/>
@@ -287,150 +290,129 @@ public class QueryParsing {
    *   height desc,weight asc   #sort by height descending, using weight ascending as a tiebreaker
    * </pre>
    */
  public static Sort parseSort(String sortSpec, IndexSchema schema) {
  public static Sort parseSort(String sortSpec, SolrQueryRequest req) {
     if (sortSpec == null || sortSpec.length() == 0) return null;
    char[] chars = sortSpec.toCharArray();
    int i = 0;
    StringBuilder buffer = new StringBuilder(sortSpec.length());
    String sort = null;
    String order = null;
    int functionDepth = 0;
    boolean score = true;
    List<SortField> lst = new ArrayList<SortField>(5);
    boolean needOrder = false;
    while (i < chars.length) {
      if (Character.isWhitespace(chars[i]) && functionDepth == 0) {
        if (buffer.length() == 0) {
          //do nothing
        } else {
          if (needOrder == false) {
            sort = buffer.toString().trim();
            buffer.setLength(0);
            needOrder = true;
    List<SortField> lst = new ArrayList<SortField>(4);

    try {

      StrParser sp = new StrParser(sortSpec);
      while (sp.pos < sp.end) {
        sp.eatws();

        int start = sp.pos;

        String field = sp.getId(null);
        ValueSource vs = null;

        if (field == null || sp.ch() != ' ') {
          // let's try it as a function instead
          String funcStr = sp.val.substring(start);

          QParser parser = QParser.getParser(funcStr, FunctionQParserPlugin.NAME, req);
          Query q = null;
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
            }
           } else {
            order = buffer.toString().trim();
            buffer.setLength(0);
            needOrder = false;
            // A QParser that's not for function queries.
            // It must have been specified via local params.
            q = parser.getQuery();

            assert parser.getLocalParams() != null;
            sp.pos = start + parser.localParamsEnd;
           }
        }
      } else if (chars[i] == '(' && functionDepth >= 0) {
        buffer.append(chars[i]);
        functionDepth++;
      } else if (chars[i] == ')' && functionDepth > 0) {
        buffer.append(chars[i]);
        functionDepth--;//close up one layer
      } else if (chars[i] == ',' && functionDepth == 0) {//can either be a separator of sort declarations, or a separator in a function
        //we have a separator between sort declarations,
        // We may need an order still, but then evaluate it, as we should have everything we need
        if (needOrder == true && buffer.length() > 0){
          order = buffer.toString().trim();
          buffer.setLength(0);
          needOrder = false;
        }
        score = processSort(schema, sort, order, lst);
        sort = null;
        order = null;
        buffer.setLength(0);//get ready for the next one, if there is one
      } else if (chars[i] == ',' && functionDepth > 0) {
        //we are in a function
        buffer.append(chars[i]);
      } else {
        //just a regular old char, add it to the buffer
        buffer.append(chars[i]);
      }
      i++;
    }
    if (buffer.length() > 0 && needOrder){//see if we have anything left, at most it should be an order
      order = buffer.toString().trim();
      buffer.setLength(0);
      needOrder = false;
    }
 
    //do some sanity checks
    if (functionDepth != 0){
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Unable to parse sort spec, mismatched parentheses: " + sortSpec);
    }
    if (buffer.length() > 0){//there's something wrong, as everything should have been parsed by now
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Unable to parse sort spec: " + sortSpec);
    }
    if (needOrder == false && sort != null && sort.equals("") == false && order != null && order.equals("") == false){//handle the last declaration
      score = processSort(schema, sort, order, lst);
    }
    //If the normal case (by score desc) do nothing
    if (lst.size() == 1 && score == true && lst.get(0).getReverse() == false) {
      return null; // do normal scoring...
    }
    return new Sort((SortField[]) lst.toArray(new SortField[lst.size()]));
  }
          // OK, now we have our query.
          if (q instanceof FunctionQuery) {
            vs = ((FunctionQuery)q).getValueSource();
          } else {
            vs = new QueryValueSource(q, 0.0f);
          }
        }
 
  private static boolean processSort(IndexSchema schema, String sort, String order, List<SortField> lst) {
    boolean score = false;
    if (sort != null && order != null) {
      boolean top = true;
      if ("desc".equals(order) || "top".equals(order)) {
        top = true;
      } else if ("asc".equals(order) || "bottom".equals(order)) {
        top = false;
      } else {
        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Unknown sort order: " + order);
      }
      //we got the order, now deal with the sort
      if ("score".equals(sort)) {
        score = true;
        if (top) {
          lst.add(SortField.FIELD_SCORE);
        // now we have our field or value source, so find the sort order
        String order = sp.getId("Expected sort order asc/desc");
        boolean top;
        if ("desc".equals(order) || "top".equals(order)) {
          top = true;
        } else if ("asc".equals(order) || "bottom".equals(order)) {
          top = false;
         } else {
          lst.add(new SortField(null, SortField.SCORE, true));
        }
      } else if (DOCID.equals(sort)) {
        lst.add(new SortField(null, SortField.DOC, top));
      } else {
        //See if we have a Field first, then see if it is a function, then throw an exception
        // getField could throw an exception if the name isn't found
        SchemaField f = null;
        try {
          f = schema.getField(sort);
        }
        catch (SolrException e) {
          //Not an error just yet
          throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Unknown sort order: " + order);
         }
        if (f != null) {
          if (f == null || !f.indexed()) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "can not sort on unindexed field: " + sort);
          }
          lst.add(f.getType().getSortField(f, top));
        } else {
          //See if we have a function:
          FunctionQuery query = null;
          try {
            query = parseFunction(sort, schema);
            if (query != null) {
              ValueSource valueSource = query.getValueSource();
              //We have a function query
              try {
                lst.add(valueSource.getSortField(top));
              } catch (IOException e) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "error getting the sort for this function: " + sort, e);
              }

        if (vs == null) {
          //we got the order, now deal with the sort
          if ("score".equals(field)) {
            if (top) {
              lst.add(SortField.FIELD_SCORE);
             } else {
              throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "can not sort on undefined function: " + sort);
              lst.add(new SortField(null, SortField.SCORE, true));
            }
          } else if (DOCID.equals(field)) {
            lst.add(new SortField(null, SortField.DOC, top));
          } else {
            //See if we have a Field first, then see if it is a function, then throw an exception
            // getField could throw an exception if the name isn't found
            SchemaField sf = req.getSchema().getField(field);

            // TODO: remove this - it should be up to the FieldType
            if (!sf.indexed()) {
              throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "can not sort on unindexed field: " + field);
             }
          } catch (ParseException e) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "can not sort on undefined field or function: " + sort, e);

            lst.add(sf.getType().getSortField(sf, top));


           }
        } else {
          lst.add(vs.getSortField(top));
        }
 
        sp.eatws();
        if (sp.pos < sp.end) {
          sp.expect(",");
         }

       }
    } else if (sort == null) {//no sort value
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
              "Must declare sort field or function");
    } else if (order == null) {
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Missing sort order: ");

    } catch (ParseException e) {
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "error in sort: " + sortSpec, e);
    } catch (IOException e) {
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "error in sort: " + sortSpec, e);
     }
    return score;


    // normalize a sort on score desc to null
    if (lst.size()==1 && lst.get(0) == SortField.FIELD_SCORE) {
      return null;
    }

    return new Sort((SortField[]) lst.toArray(new SortField[lst.size()]));
   }
 
 

   ///////////////////////////
   ///////////////////////////
   ///////////////////////////
@@ -640,6 +622,10 @@ public class QueryParsing {
       while (pos < end && Character.isWhitespace(val.charAt(pos))) pos++;
     }
 
    char ch() {
      return pos < end ? val.charAt(pos) : 0;
    }

     void skip(int nChars) {
       pos = Math.max(pos + nChars, end);
     }
@@ -756,12 +742,17 @@ public class QueryParsing {
 
 
     String getId() throws ParseException {
      return getId("Expected identifier");
    }

    String getId(String errMessage) throws ParseException {
       eatws();
       int id_start = pos;
      if (pos < end && Character.isJavaIdentifierStart(val.charAt(pos))) {
      char ch;
      if (pos < end && (ch = val.charAt(pos)) != '$' && Character.isJavaIdentifierStart(ch)) {
         pos++;
         while (pos < end) {
          char ch = val.charAt(pos);
          ch = val.charAt(pos);
           if (!Character.isJavaIdentifierPart(ch) && ch != '.') {
             break;
           }
@@ -769,7 +760,11 @@ public class QueryParsing {
         }
         return val.substring(id_start, pos);
       }
      throw new ParseException("Expected identifier at pos " + pos + " str='" + val + "'");

      if (errMessage != null) {
        throw new ParseException(errMessage + " at pos " + pos + " str='" + val + "'");
      }
      return null;
     }
 
     // return null if not a string
diff --git a/solr/src/java/org/apache/solr/tst/OldRequestHandler.java b/solr/src/java/org/apache/solr/tst/OldRequestHandler.java
index a42461f4f12..13183e3ed2d 100644
-- a/solr/src/java/org/apache/solr/tst/OldRequestHandler.java
++ b/solr/src/java/org/apache/solr/tst/OldRequestHandler.java
@@ -63,7 +63,7 @@ public class OldRequestHandler implements SolrRequestHandler {
     // we can use the Lucene sort ability.
     Sort sort = null;
     if (commands.size() >= 2) {
      sort = QueryParsing.parseSort(commands.get(1), req.getSchema());
      sort = QueryParsing.parseSort(commands.get(1), req);
     }
 
 
diff --git a/solr/src/java/org/apache/solr/tst/TestRequestHandler.java b/solr/src/java/org/apache/solr/tst/TestRequestHandler.java
index edabc056ab1..c17942aca0e 100644
-- a/solr/src/java/org/apache/solr/tst/TestRequestHandler.java
++ b/solr/src/java/org/apache/solr/tst/TestRequestHandler.java
@@ -105,7 +105,7 @@ public class TestRequestHandler implements SolrRequestHandler {
       // we can use the Lucene sort ability.
       Sort sort = null;
       if (commands.size() >= 2) {
        sort = QueryParsing.parseSort(commands.get(1), req.getSchema());
        sort = QueryParsing.parseSort(commands.get(1), req);
       }
 
       SolrIndexSearcher searcher = req.getSearcher();
diff --git a/solr/src/java/org/apache/solr/util/SolrPluginUtils.java b/solr/src/java/org/apache/solr/util/SolrPluginUtils.java
index 03507a0301c..b7ce01cbbdf 100644
-- a/solr/src/java/org/apache/solr/util/SolrPluginUtils.java
++ b/solr/src/java/org/apache/solr/util/SolrPluginUtils.java
@@ -366,7 +366,7 @@ public class SolrPluginUtils {
       String otherQueryS = req.getParams().get(CommonParams.EXPLAIN_OTHER);
       if (otherQueryS != null && otherQueryS.length() > 0) {
         DocList otherResults = doSimpleQuery
                (otherQueryS, req.getSearcher(), req.getSchema(), 0, 10);
                (otherQueryS, req, 0, 10);
         dbg.add("otherQuery", otherQueryS);
         NamedList<Explanation> explainO
                 = getExplanations(query, otherResults, searcher, schema);
@@ -467,26 +467,30 @@ public class SolrPluginUtils {
   }
 
   /**
   * Executes a basic query in lucene syntax
   * Executes a basic query
    */
   public static DocList doSimpleQuery(String sreq,
                                      SolrIndexSearcher searcher,
                                      IndexSchema schema,
                                      SolrQueryRequest req,
                                       int start, int limit) throws IOException {
     List<String> commands = StrUtils.splitSmart(sreq,';');
 
     String qs = commands.size() >= 1 ? commands.get(0) : "";
    Query query = QueryParsing.parseQuery(qs, schema);
    try {
    Query query = QParser.getParser(qs, null, req).getQuery();
 
     // If the first non-query, non-filter command is a simple sort on an indexed field, then
     // we can use the Lucene sort ability.
     Sort sort = null;
     if (commands.size() >= 2) {
      sort = QueryParsing.parseSort(commands.get(1), schema);
      sort = QueryParsing.parseSort(commands.get(1), req);
     }
 
    DocList results = searcher.getDocList(query,(DocSet)null, sort, start, limit);
    DocList results = req.getSearcher().getDocList(query,(DocSet)null, sort, start, limit);
     return results;
    } catch (ParseException e) {
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Error parsing query: " + qs);
    }

   }
 
   /**
@@ -855,7 +859,7 @@ public class SolrPluginUtils {
     SolrException sortE = null;
     Sort ss = null;
     try {
      ss = QueryParsing.parseSort(sort, req.getSchema());
      ss = QueryParsing.parseSort(sort, req);
     } catch (SolrException e) {
       sortE = e;
     }
diff --git a/solr/src/test/org/apache/solr/search/QueryParsingTest.java b/solr/src/test/org/apache/solr/search/QueryParsingTest.java
index 559c001cb8d..95087bc3bb7 100644
-- a/solr/src/test/org/apache/solr/search/QueryParsingTest.java
++ b/solr/src/test/org/apache/solr/search/QueryParsingTest.java
@@ -20,6 +20,7 @@ import org.apache.lucene.search.Sort;
 import org.apache.lucene.search.SortField;
 import org.apache.solr.SolrTestCaseJ4;
 import org.apache.solr.common.SolrException;
import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.schema.IndexSchema;
 import org.junit.BeforeClass;
 import org.junit.Test;
@@ -39,22 +40,23 @@ public class QueryParsingTest extends SolrTestCaseJ4 {
   @Test
   public void testSort() throws Exception {
     Sort sort;
    SolrQueryRequest req = req();
 
     IndexSchema schema = h.getCore().getSchema();
    sort = QueryParsing.parseSort("score desc", schema);
    sort = QueryParsing.parseSort("score desc", req);
     assertNull("sort", sort);//only 1 thing in the list, no Sort specified
 
    sort = QueryParsing.parseSort("score asc", schema);
    sort = QueryParsing.parseSort("score asc", req);
     SortField[] flds = sort.getSort();
     assertEquals(flds[0].getType(), SortField.SCORE);
     assertTrue(flds[0].getReverse());
 
    sort = QueryParsing.parseSort("weight desc", schema);
    sort = QueryParsing.parseSort("weight desc", req);
     flds = sort.getSort();
     assertEquals(flds[0].getType(), SortField.FLOAT);
     assertEquals(flds[0].getField(), "weight");
     assertEquals(flds[0].getReverse(), true);
    sort = QueryParsing.parseSort("weight desc,bday asc", schema);
    sort = QueryParsing.parseSort("weight desc,bday asc", req);
     flds = sort.getSort();
     assertEquals(flds[0].getType(), SortField.FLOAT);
     assertEquals(flds[0].getField(), "weight");
@@ -63,7 +65,7 @@ public class QueryParsingTest extends SolrTestCaseJ4 {
     assertEquals(flds[1].getField(), "bday");
     assertEquals(flds[1].getReverse(), false);
     //order aliases
    sort = QueryParsing.parseSort("weight top,bday asc", schema);
    sort = QueryParsing.parseSort("weight top,bday asc", req);
     flds = sort.getSort();
     assertEquals(flds[0].getType(), SortField.FLOAT);
     assertEquals(flds[0].getField(), "weight");
@@ -71,7 +73,7 @@ public class QueryParsingTest extends SolrTestCaseJ4 {
     assertEquals(flds[1].getType(), SortField.LONG);
     assertEquals(flds[1].getField(), "bday");
     assertEquals(flds[1].getReverse(), false);
    sort = QueryParsing.parseSort("weight top,bday bottom", schema);
    sort = QueryParsing.parseSort("weight top,bday bottom", req);
     flds = sort.getSort();
     assertEquals(flds[0].getType(), SortField.FLOAT);
     assertEquals(flds[0].getField(), "weight");
@@ -81,20 +83,20 @@ public class QueryParsingTest extends SolrTestCaseJ4 {
     assertEquals(flds[1].getReverse(), false);
 
     //test weird spacing
    sort = QueryParsing.parseSort("weight         desc,            bday         asc", schema);
    sort = QueryParsing.parseSort("weight         desc,            bday         asc", req);
     flds = sort.getSort();
     assertEquals(flds[0].getType(), SortField.FLOAT);
     assertEquals(flds[0].getField(), "weight");
     assertEquals(flds[1].getField(), "bday");
     assertEquals(flds[1].getType(), SortField.LONG);
     //handles trailing commas
    sort = QueryParsing.parseSort("weight desc,", schema);
    sort = QueryParsing.parseSort("weight desc,", req);
     flds = sort.getSort();
     assertEquals(flds[0].getType(), SortField.FLOAT);
     assertEquals(flds[0].getField(), "weight");
 
     //test functions
    sort = QueryParsing.parseSort("pow(weight, 2) desc", schema);
    sort = QueryParsing.parseSort("pow(weight, 2) desc", req);
     flds = sort.getSort();
     assertEquals(flds[0].getType(), SortField.CUSTOM);
     //Not thrilled about the fragility of string matching here, but...
@@ -102,12 +104,12 @@ public class QueryParsingTest extends SolrTestCaseJ4 {
     assertEquals(flds[0].getField(), "pow(float(weight),const(2.0))");
     
     //test functions (more deep)
    sort = QueryParsing.parseSort("sum(product(r_f,sum(d_f,t_f,1)),a_f) asc", schema);
    sort = QueryParsing.parseSort("sum(product(r_f,sum(d_f,t_f,1)),a_f) asc", req);
     flds = sort.getSort();
     assertEquals(flds[0].getType(), SortField.CUSTOM);
     assertEquals(flds[0].getField(), "sum(product(float(r_f),sum(float(d_f),float(t_f),const(1.0))),float(a_f))");
 
    sort = QueryParsing.parseSort("pow(weight,                 2)         desc", schema);
    sort = QueryParsing.parseSort("pow(weight,                 2)         desc", req);
     flds = sort.getSort();
     assertEquals(flds[0].getType(), SortField.CUSTOM);
     //Not thrilled about the fragility of string matching here, but...
@@ -115,7 +117,7 @@ public class QueryParsingTest extends SolrTestCaseJ4 {
     assertEquals(flds[0].getField(), "pow(float(weight),const(2.0))");
 
 
    sort = QueryParsing.parseSort("pow(weight, 2) desc, weight    desc,   bday    asc", schema);
    sort = QueryParsing.parseSort("pow(weight, 2) desc, weight    desc,   bday    asc", req);
     flds = sort.getSort();
     assertEquals(flds[0].getType(), SortField.CUSTOM);
 
@@ -129,19 +131,19 @@ public class QueryParsingTest extends SolrTestCaseJ4 {
     assertEquals(flds[2].getType(), SortField.LONG);
     
     //handles trailing commas
    sort = QueryParsing.parseSort("weight desc,", schema);
    sort = QueryParsing.parseSort("weight desc,", req);
     flds = sort.getSort();
     assertEquals(flds[0].getType(), SortField.FLOAT);
     assertEquals(flds[0].getField(), "weight");
 
     //Test literals in functions
    sort = QueryParsing.parseSort("strdist(foo_s, \"junk\", jw) desc", schema);
    sort = QueryParsing.parseSort("strdist(foo_s, \"junk\", jw) desc", req);
     flds = sort.getSort();
     assertEquals(flds[0].getType(), SortField.CUSTOM);
     //the value sources get wrapped, so the out field is different than the input
     assertEquals(flds[0].getField(), "strdist(str(foo_s),literal(junk), dist=org.apache.lucene.search.spell.JaroWinklerDistance)");
 
    sort = QueryParsing.parseSort("", schema);
    sort = QueryParsing.parseSort("", req);
     assertNull(sort);
 
   }
@@ -149,44 +151,45 @@ public class QueryParsingTest extends SolrTestCaseJ4 {
   @Test
   public void testBad() throws Exception {
     Sort sort;
    SolrQueryRequest req = req();
 
     IndexSchema schema = h.getCore().getSchema();
     //test some bad vals
     try {
      sort = QueryParsing.parseSort("weight, desc", schema);
      sort = QueryParsing.parseSort("weight, desc", req);
       assertTrue(false);
     } catch (SolrException e) {
       //expected
     }
     try {
      sort = QueryParsing.parseSort("w", schema);
      sort = QueryParsing.parseSort("w", req);
       assertTrue(false);
     } catch (SolrException e) {
       //expected
     }
     try {
      sort = QueryParsing.parseSort("weight desc, bday", schema);
      sort = QueryParsing.parseSort("weight desc, bday", req);
       assertTrue(false);
     } catch (SolrException e) {
     }
 
     try {
       //bad number of commas
      sort = QueryParsing.parseSort("pow(weight,,2) desc, bday asc", schema);
      sort = QueryParsing.parseSort("pow(weight,,2) desc, bday asc", req);
       assertTrue(false);
     } catch (SolrException e) {
     }
 
     try {
       //bad function
      sort = QueryParsing.parseSort("pow() desc, bday asc", schema);
      sort = QueryParsing.parseSort("pow() desc, bday asc", req);
       assertTrue(false);
     } catch (SolrException e) {
     }
 
     try {
       //bad number of parens
      sort = QueryParsing.parseSort("pow((weight,2) desc, bday asc", schema);
      sort = QueryParsing.parseSort("pow((weight,2) desc, bday asc", req);
       assertTrue(false);
     } catch (SolrException e) {
     }
diff --git a/solr/src/test/org/apache/solr/search/function/TestFunctionQuery.java b/solr/src/test/org/apache/solr/search/function/TestFunctionQuery.java
index 7f2025f3d70..91d458885a1 100755
-- a/solr/src/test/org/apache/solr/search/function/TestFunctionQuery.java
++ b/solr/src/test/org/apache/solr/search/function/TestFunctionQuery.java
@@ -359,6 +359,52 @@ public class TestFunctionQuery extends SolrTestCaseJ4 {
     purgeFieldCache(FieldCache.DEFAULT);   // avoid FC insanity
   }
 
  @Test
  public void testSortByFunc() throws Exception {
    assertU(adoc("id", "1", "x_i", "100"));
    assertU(adoc("id", "2", "x_i", "300"));
    assertU(adoc("id", "3", "x_i", "200"));
    assertU(commit());

    String desc = "/response/docs==[{'x_i':300},{'x_i':200},{'x_i':100}]";
    String asc =  "/response/docs==[{'x_i':100},{'x_i':200},{'x_i':300}]";

    String q = "id:[1 TO 3]";
    assertJQ(req("q",q,  "fl","x_i", "sort","add(x_i,x_i) desc")
      ,desc
    );

    // param sub of entire function
    assertJQ(req("q",q,  "fl","x_i", "sort", "$x asc", "x","add(x_i,x_i)")
      ,asc
    );

    // multiple functions
    assertJQ(req("q",q,  "fl","x_i", "sort", "$x asc, $y desc", "x", "5", "y","add(x_i,x_i)")
      ,desc
    );

    // multiple functions inline
    assertJQ(req("q",q,  "fl","x_i", "sort", "add( 10 , 10 ) asc, add(x_i , $const) desc", "const","50")
      ,desc
    );

    // test function w/ local params + func inline
     assertJQ(req("q",q,  "fl","x_i", "sort", "{!key=foo}add(x_i,x_i) desc")
      ,desc
    );

    // test multiple functions w/ local params + func inline
    assertJQ(req("q",q,  "fl","x_i", "sort", "{!key=bar}add(10,20) asc, {!key=foo}add(x_i,x_i) desc")
      ,desc
    );

    // test multiple functions w/ local param value not inlined
    assertJQ(req("q",q,  "fl","x_i", "sort", "{!key=bar v=$s1} asc, {!key=foo v=$s2} desc", "s1","add(3,4)", "s2","add(x_i,5)")
      ,desc
    );
  }

   @Test
   public void testDegreeRads() throws Exception {    
     assertU(adoc("id", "1", "x_td", "0", "y_td", "0"));
- 
2.19.1.windows.1

