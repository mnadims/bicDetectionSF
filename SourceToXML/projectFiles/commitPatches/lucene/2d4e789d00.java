From 2d4e789d00ecb5dfaf73e7df01a7da4502c404a9 Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Fri, 23 Nov 2012 19:45:50 +0000
Subject: [PATCH] SOLR-4093: solr specific parser + localParams syntax

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1413042 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |   13 +
 solr/core/build.xml                           |   52 +
 .../solr/handler/MoreLikeThisHandler.java     |    4 +-
 .../solr/handler/RequestHandlerBase.java      |    4 +-
 .../handler/component/FacetComponent.java     |    4 +-
 .../handler/component/HighlightComponent.java |    4 +-
 .../handler/component/PivotFacetHelper.java   |    4 +-
 .../handler/component/QueryComponent.java     |    5 +-
 .../org/apache/solr/parser/CharStream.java    |  115 ++
 .../apache/solr/parser/FastCharStream.java    |  123 ++
 .../apache/solr/parser/ParseException.java    |  187 +++
 .../org/apache/solr/parser/QueryParser.java   |  701 +++++++++
 .../org/apache/solr/parser/QueryParser.jj     |  267 ++++
 .../solr/parser/QueryParserConstants.java     |  129 ++
 .../solr/parser/QueryParserTokenManager.java  | 1261 +++++++++++++++++
 .../solr/parser/SolrQueryParserBase.java      | 1033 ++++++++++++++
 .../java/org/apache/solr/parser/Token.java    |  131 ++
 .../org/apache/solr/parser/TokenMgrError.java |  147 ++
 .../org/apache/solr/request/SimpleFacets.java |   17 +-
 .../solr/search/BoostQParserPlugin.java       |    5 +-
 .../org/apache/solr/search/DisMaxQParser.java |   29 +-
 .../search/ExtendedDismaxQParserPlugin.java   |   40 +-
 .../apache/solr/search/FunctionQParser.java   |   46 +-
 .../search/FunctionRangeQParserPlugin.java    |   10 +-
 .../java/org/apache/solr/search/Grouping.java |    8 +-
 .../apache/solr/search/JoinQParserPlugin.java |    3 +-
 .../solr/search/LuceneQParserPlugin.java      |   11 +-
 .../solr/search/NestedQParserPlugin.java      |    5 +-
 .../java/org/apache/solr/search/QParser.java  |   19 +-
 .../org/apache/solr/search/QueryParsing.java  |   39 +-
 .../org/apache/solr/search/ReturnFields.java  |    5 +-
 .../apache/solr/search/SolrQueryParser.java   |  212 +--
 .../solr/search/SpatialFilterQParser.java     |    3 +-
 .../solr/search/SurroundQParserPlugin.java    |    7 +-
 .../org/apache/solr/search/SyntaxError.java   |   31 +
 .../apache/solr/search/ValueSourceParser.java |   99 +-
 .../distance/HaversineConstFunction.java      |   24 +-
 .../distributed/command/QueryCommand.java     |    5 +-
 .../solr/update/DirectUpdateHandler2.java     |    4 +-
 .../org/apache/solr/util/SolrPluginUtils.java |   16 +-
 .../solr/core/DummyValueSourceParser.java     |    4 +-
 .../solr/core/TestArbitraryIndexDir.java      |    3 +-
 .../apache/solr/search/FooQParserPlugin.java  |    3 +-
 .../solr/search/TestExtendedDismaxParser.java |    2 +-
 .../solr/search/TestSolrQueryParser.java      |   37 +
 .../solr/search/TestValueSourceCache.java     |   11 +-
 .../search/function/NvlValueSourceParser.java |    4 +-
 .../apache/solr/util/DateMathParserTest.java  |    2 +-
 48 files changed, 4436 insertions(+), 452 deletions(-)
 create mode 100644 solr/core/src/java/org/apache/solr/parser/CharStream.java
 create mode 100644 solr/core/src/java/org/apache/solr/parser/FastCharStream.java
 create mode 100644 solr/core/src/java/org/apache/solr/parser/ParseException.java
 create mode 100644 solr/core/src/java/org/apache/solr/parser/QueryParser.java
 create mode 100644 solr/core/src/java/org/apache/solr/parser/QueryParser.jj
 create mode 100644 solr/core/src/java/org/apache/solr/parser/QueryParserConstants.java
 create mode 100644 solr/core/src/java/org/apache/solr/parser/QueryParserTokenManager.java
 create mode 100644 solr/core/src/java/org/apache/solr/parser/SolrQueryParserBase.java
 create mode 100644 solr/core/src/java/org/apache/solr/parser/Token.java
 create mode 100644 solr/core/src/java/org/apache/solr/parser/TokenMgrError.java
 create mode 100644 solr/core/src/java/org/apache/solr/search/SyntaxError.java

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index be074ccfdc1..48a248a78bb 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -34,6 +34,13 @@ Velocity 1.6.4 and Velocity Tools 2.0
 Apache UIMA 2.3.1
 Apache ZooKeeper 3.4.5
 
Upgrading from Solr 4.0.0-BETA
----------------------

Custom java parsing plugins need to migrade from throwing the internal
ParseException to throwing SyntaxError.


 Detailed Change List
 ----------------------
 
@@ -88,6 +95,12 @@ New Features
   'storeOffsetsWithPositions' flag on field definitions in the schema.
   (Tom Winch, Alan Woodward)
 
* SOLR-4093: Solr QParsers may now be directly invoked in the lucene
  query syntax without the _query_ magic field hack.
  Example:  foo AND {!term f=myfield v=$qq}
  (yonik)


 Optimizations
 ----------------------
 
diff --git a/solr/core/build.xml b/solr/core/build.xml
index bff9ead936b..32462398aea 100644
-- a/solr/core/build.xml
++ b/solr/core/build.xml
@@ -42,4 +42,56 @@
   </target>
 
   <target name="dist-maven" depends="dist-maven-src-java"/>




  <target name="javacc" depends="javacc-QueryParser"/>
  <target name="javacc-QueryParser" depends="resolve-javacc">
    <sequential>
      <invoke-javacc target="src/java/org/apache/solr/parser/QueryParser.jj"
                     outputDir="src/java/org/apache/solr/parser"/>

      <!-- Change the incorrect public ctors for QueryParser to be protected instead -->
      <replaceregexp file="src/java/org/apache/solr/parser/QueryParser.java"
                     byline="true"
                     match="public QueryParser\(CharStream "
                     replace="protected QueryParser(CharStream "/>
      <replaceregexp file="src/java/org/apache/solr/parser/QueryParser.java"
                     byline="true"
                     match="public QueryParser\(QueryParserTokenManager "
                     replace="protected QueryParser(QueryParserTokenManager "/>

    </sequential>
  </target>
  <target name="resolve-javacc" xmlns:ivy="antlib:org.apache.ivy.ant">
    <!-- setup a "fake" JavaCC distribution folder in ${build.dir} to make JavaCC ANT task happy: -->
    <ivy:retrieve organisation="net.java.dev.javacc" module="javacc" revision="5.0"
      inline="true" conf="default" transitive="false" type="jar" sync="true"
      pattern="${build.dir}/javacc/bin/lib/[artifact].[ext]"/>
  </target>

  <macrodef name="invoke-javacc">
    <attribute name="target"/>
    <attribute name="outputDir"/>
    <sequential>
      <mkdir dir="@{outputDir}"/>
      <delete>
        <fileset dir="@{outputDir}" includes="*.java">
          <containsregexp expression="Generated.*By.*JavaCC"/>
        </fileset>
      </delete>
      <javacc
          target="@{target}"
          outputDirectory="@{outputDir}"
          javacchome="${build.dir}/javacc"
          jdkversion="${javac.source}"
      />
      <fixcrlf srcdir="@{outputDir}" includes="*.java" encoding="UTF-8">
        <containsregexp expression="Generated.*By.*JavaCC"/>
      </fixcrlf>
    </sequential>
  </macrodef>


 </project>
diff --git a/solr/core/src/java/org/apache/solr/handler/MoreLikeThisHandler.java b/solr/core/src/java/org/apache/solr/handler/MoreLikeThisHandler.java
index b8411ea188f..62a406ae847 100644
-- a/solr/core/src/java/org/apache/solr/handler/MoreLikeThisHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/MoreLikeThisHandler.java
@@ -29,11 +29,9 @@ import java.util.List;
 import java.util.Map;
 import java.util.regex.Pattern;
 
import org.apache.lucene.document.Document;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.StoredDocument;
 import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.search.*;
 import org.apache.lucene.queries.mlt.MoreLikeThis;
 import org.apache.solr.common.SolrException;
@@ -108,7 +106,7 @@ public class MoreLikeThisHandler extends RequestHandlerBase
           }
         }
       }
    } catch (ParseException e) {
    } catch (SyntaxError e) {
       throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, e);
     }
 
diff --git a/solr/core/src/java/org/apache/solr/handler/RequestHandlerBase.java b/solr/core/src/java/org/apache/solr/handler/RequestHandlerBase.java
index d446e6852c9..3a2693d8fc6 100644
-- a/solr/core/src/java/org/apache/solr/handler/RequestHandlerBase.java
++ b/solr/core/src/java/org/apache/solr/handler/RequestHandlerBase.java
@@ -22,7 +22,6 @@ import com.yammer.metrics.core.Counter;
 import com.yammer.metrics.core.Timer;
 import com.yammer.metrics.core.TimerContext;
 import com.yammer.metrics.stats.Snapshot;
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.NamedList;
@@ -32,6 +31,7 @@ import org.apache.solr.core.SolrInfoMBean;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.request.SolrRequestHandler;
 import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.SyntaxError;
 import org.apache.solr.util.SolrPluginUtils;
 
 import java.net.URL;
@@ -163,7 +163,7 @@ public abstract class RequestHandlerBase implements SolrRequestHandler, SolrInfo
         }
       } else {
         SolrException.log(SolrCore.log,e);
        if (e instanceof ParseException) {
        if (e instanceof SyntaxError) {
           e = new SolrException(SolrException.ErrorCode.BAD_REQUEST, e);
         }
       }
diff --git a/solr/core/src/java/org/apache/solr/handler/component/FacetComponent.java b/solr/core/src/java/org/apache/solr/handler/component/FacetComponent.java
index b1843111c76..d3a6069f563 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/FacetComponent.java
++ b/solr/core/src/java/org/apache/solr/handler/component/FacetComponent.java
@@ -17,7 +17,6 @@
 
 package org.apache.solr.handler.component;
 
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.util.OpenBitSet;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.SolrException.ErrorCode;
@@ -32,6 +31,7 @@ import org.apache.solr.common.util.StrUtils;
 import org.apache.solr.request.SimpleFacets;
 import org.apache.solr.schema.FieldType;
 import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SyntaxError;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -670,7 +670,7 @@ public class FacetComponent extends SearchComponent
       this.facetStr = facetStr;
       try {
         this.localParams = QueryParsing.getLocalParams(facetStr, rb.req.getParams());
      } catch (ParseException e) {
      } catch (SyntaxError e) {
         throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, e);
       }
       this.facetOn = facetStr;
diff --git a/solr/core/src/java/org/apache/solr/handler/component/HighlightComponent.java b/solr/core/src/java/org/apache/solr/handler/component/HighlightComponent.java
index 69a3ce92540..a1e7697fd66 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/HighlightComponent.java
++ b/solr/core/src/java/org/apache/solr/handler/component/HighlightComponent.java
@@ -17,7 +17,6 @@
 
 package org.apache.solr.handler.component;
 
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.search.Query;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.params.CommonParams;
@@ -29,6 +28,7 @@ import org.apache.solr.highlight.SolrHighlighter;
 import org.apache.solr.highlight.DefaultSolrHighlighter;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;
 import org.apache.solr.util.SolrPluginUtils;
 import org.apache.solr.util.plugin.PluginInfoInitialized;
 import org.apache.solr.util.plugin.SolrCoreAware;
@@ -71,7 +71,7 @@ public class HighlightComponent extends SearchComponent implements PluginInfoIni
         try {
           QParser parser = QParser.getParser(hlq, null, rb.req);
           rb.setHighlightQuery(parser.getHighlightQuery());
        } catch (ParseException e) {
        } catch (SyntaxError e) {
           throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, e);
         }
       }
diff --git a/solr/core/src/java/org/apache/solr/handler/component/PivotFacetHelper.java b/solr/core/src/java/org/apache/solr/handler/component/PivotFacetHelper.java
index 72a0a634b2c..7606adc332e 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/PivotFacetHelper.java
++ b/solr/core/src/java/org/apache/solr/handler/component/PivotFacetHelper.java
@@ -17,7 +17,6 @@
 
 package org.apache.solr.handler.component;
 
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.util.BytesRef;
 import org.apache.solr.schema.SchemaField;
 import org.apache.solr.search.SolrIndexSearcher;
@@ -35,6 +34,7 @@ import org.apache.lucene.search.Query;
 import org.apache.lucene.search.TermQuery;
 import org.apache.lucene.search.TermRangeQuery;
 import org.apache.lucene.index.Term;
import org.apache.solr.search.SyntaxError;
 
 import java.io.IOException;
 import java.util.ArrayList;
@@ -65,7 +65,7 @@ public class PivotFacetHelper extends SimpleFacets
       //ex: pivot == "features,cat" or even "{!ex=mytag}features,cat"
       try {
         this.parseParams(FacetParams.FACET_PIVOT, pivot);
      } catch (ParseException e) {
      } catch (SyntaxError e) {
         throw new SolrException(ErrorCode.BAD_REQUEST, e);
       }
       pivot = facetValue;//facetValue potentially modified from parseParams()
diff --git a/solr/core/src/java/org/apache/solr/handler/component/QueryComponent.java b/solr/core/src/java/org/apache/solr/handler/component/QueryComponent.java
index 6ae278d8744..a731f346f57 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/QueryComponent.java
++ b/solr/core/src/java/org/apache/solr/handler/component/QueryComponent.java
@@ -23,7 +23,6 @@ import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.IndexReaderContext;
 import org.apache.lucene.index.ReaderUtil;
 import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.search.*;
 import org.apache.lucene.search.grouping.GroupDocs;
 import org.apache.lucene.search.grouping.SearchGroup;
@@ -144,7 +143,7 @@ public class QueryComponent extends SearchComponent
           rb.setFilters( filters );
         }
       }
    } catch (ParseException e) {
    } catch (SyntaxError e) {
       throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, e);
     }
 
@@ -402,7 +401,7 @@ public class QueryComponent extends SearchComponent
           rsp.getToLog().add("hits", grouping.getCommands().get(0).getMatches());
         }
         return;
      } catch (ParseException e) {
      } catch (SyntaxError e) {
         throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, e);
       }
     }
diff --git a/solr/core/src/java/org/apache/solr/parser/CharStream.java b/solr/core/src/java/org/apache/solr/parser/CharStream.java
new file mode 100644
index 00000000000..0400af47e05
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/parser/CharStream.java
@@ -0,0 +1,115 @@
/* Generated By:JavaCC: Do not edit this line. CharStream.java Version 5.0 */
/* JavaCCOptions:STATIC=false,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.apache.solr.parser;

/**
 * This interface describes a character stream that maintains line and
 * column number positions of the characters.  It also has the capability
 * to backup the stream to some extent.  An implementation of this
 * interface is used in the TokenManager implementation generated by
 * JavaCCParser.
 *
 * All the methods except backup can be implemented in any fashion. backup
 * needs to be implemented correctly for the correct operation of the lexer.
 * Rest of the methods are all used to get information like line number,
 * column number and the String that constitutes a token and are not used
 * by the lexer. Hence their implementation won't affect the generated lexer's
 * operation.
 */

public
interface CharStream {

  /**
   * Returns the next character from the selected input.  The method
   * of selecting the input is the responsibility of the class
   * implementing this interface.  Can throw any java.io.IOException.
   */
  char readChar() throws java.io.IOException;

  @Deprecated
  /**
   * Returns the column position of the character last read.
   * @deprecated
   * @see #getEndColumn
   */
  int getColumn();

  @Deprecated
  /**
   * Returns the line number of the character last read.
   * @deprecated
   * @see #getEndLine
   */
  int getLine();

  /**
   * Returns the column number of the last character for current token (being
   * matched after the last call to BeginTOken).
   */
  int getEndColumn();

  /**
   * Returns the line number of the last character for current token (being
   * matched after the last call to BeginTOken).
   */
  int getEndLine();

  /**
   * Returns the column number of the first character for current token (being
   * matched after the last call to BeginTOken).
   */
  int getBeginColumn();

  /**
   * Returns the line number of the first character for current token (being
   * matched after the last call to BeginTOken).
   */
  int getBeginLine();

  /**
   * Backs up the input stream by amount steps. Lexer calls this method if it
   * had already read some characters, but could not use them to match a
   * (longer) token. So, they will be used again as the prefix of the next
   * token and it is the implemetation's responsibility to do this right.
   */
  void backup(int amount);

  /**
   * Returns the next character that marks the beginning of the next token.
   * All characters must remain in the buffer between two successive calls
   * to this method to implement backup correctly.
   */
  char BeginToken() throws java.io.IOException;

  /**
   * Returns a string made up of characters from the marked token beginning
   * to the current buffer position. Implementations have the choice of returning
   * anything that they want to. For example, for efficiency, one might decide
   * to just return null, which is a valid implementation.
   */
  String GetImage();

  /**
   * Returns an array of characters that make up the suffix of length 'len' for
   * the currently matched token. This is used to build up the matched string
   * for use in actions in the case of MORE. A simple and inefficient
   * implementation of this is as follows :
   *
   *   {
   *      String t = GetImage();
   *      return t.substring(t.length() - len, t.length()).toCharArray();
   *   }
   */
  char[] GetSuffix(int len);

  /**
   * The lexer calls this function to indicate that it is done with the stream
   * and hence implementations can free any resources held by this class.
   * Again, the body of this function can be just empty and it will not
   * affect the lexer's operation.
   */
  void Done();

}
/* JavaCC - OriginalChecksum=48b70e7c01825c8f301c7362bf1028d8 (do not edit this line) */
diff --git a/solr/core/src/java/org/apache/solr/parser/FastCharStream.java b/solr/core/src/java/org/apache/solr/parser/FastCharStream.java
new file mode 100644
index 00000000000..38b3a7898cc
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/parser/FastCharStream.java
@@ -0,0 +1,123 @@
// FastCharStream.java
package org.apache.solr.parser;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 */

import java.io.*;

/** An efficient implementation of JavaCC's CharStream interface.  <p>Note that
 * this does not do line-number counting, but instead keeps track of the
 * character position of the token in the input, as required by Lucene's {@link
 * org.apache.lucene.analysis.Token} API. 
 * */
public final class FastCharStream implements CharStream {
  char[] buffer = null;

  int bufferLength = 0;          // end of valid chars
  int bufferPosition = 0;        // next char to read

  int tokenStart = 0;          // offset in buffer
  int bufferStart = 0;          // position in file of buffer

  Reader input;            // source of chars

  /** Constructs from a Reader. */
  public FastCharStream(Reader r) {
    input = r;
  }

  public final char readChar() throws IOException {
    if (bufferPosition >= bufferLength)
      refill();
    return buffer[bufferPosition++];
  }

  private final void refill() throws IOException {
    int newPosition = bufferLength - tokenStart;

    if (tokenStart == 0) {        // token won't fit in buffer
      if (buffer == null) {        // first time: alloc buffer
  buffer = new char[2048];
      } else if (bufferLength == buffer.length) { // grow buffer
  char[] newBuffer = new char[buffer.length*2];
  System.arraycopy(buffer, 0, newBuffer, 0, bufferLength);
  buffer = newBuffer;
      }
    } else {            // shift token to front
      System.arraycopy(buffer, tokenStart, buffer, 0, newPosition);
    }

    bufferLength = newPosition;        // update state
    bufferPosition = newPosition;
    bufferStart += tokenStart;
    tokenStart = 0;

    int charsRead =          // fill space in buffer
      input.read(buffer, newPosition, buffer.length-newPosition);
    if (charsRead == -1)
      throw new IOException("read past eof");
    else
      bufferLength += charsRead;
  }

  public final char BeginToken() throws IOException {
    tokenStart = bufferPosition;
    return readChar();
  }

  public final void backup(int amount) {
    bufferPosition -= amount;
  }

  public final String GetImage() {
    return new String(buffer, tokenStart, bufferPosition - tokenStart);
  }

  public final char[] GetSuffix(int len) {
    char[] value = new char[len];
    System.arraycopy(buffer, bufferPosition - len, value, 0, len);
    return value;
  }

  public final void Done() {
    try {
      input.close();
    } catch (IOException e) {
    }
  }

  public final int getColumn() {
    return bufferStart + bufferPosition;
  }
  public final int getLine() {
    return 1;
  }
  public final int getEndColumn() {
    return bufferStart + bufferPosition;
  }
  public final int getEndLine() {
    return 1;
  }
  public final int getBeginColumn() {
    return bufferStart + tokenStart;
  }
  public final int getBeginLine() {
    return 1;
  }
}
diff --git a/solr/core/src/java/org/apache/solr/parser/ParseException.java b/solr/core/src/java/org/apache/solr/parser/ParseException.java
new file mode 100644
index 00000000000..df1910443bc
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/parser/ParseException.java
@@ -0,0 +1,187 @@
/* Generated By:JavaCC: Do not edit this line. ParseException.java Version 5.0 */
/* JavaCCOptions:KEEP_LINE_COL=null */
package org.apache.solr.parser;

/**
 * This exception is thrown when parse errors are encountered.
 * You can explicitly create objects of this exception type by
 * calling the method generateParseException in the generated
 * parser.
 *
 * You can modify this class to customize your error reporting
 * mechanisms so long as you retain the public fields.
 */
public class ParseException extends Exception {

  /**
   * The version identifier for this Serializable class.
   * Increment only if the <i>serialized</i> form of the
   * class changes.
   */
  private static final long serialVersionUID = 1L;

  /**
   * This constructor is used by the method "generateParseException"
   * in the generated parser.  Calling this constructor generates
   * a new object of this type with the fields "currentToken",
   * "expectedTokenSequences", and "tokenImage" set.
   */
  public ParseException(Token currentTokenVal,
                        int[][] expectedTokenSequencesVal,
                        String[] tokenImageVal
                       )
  {
    super(initialise(currentTokenVal, expectedTokenSequencesVal, tokenImageVal));
    currentToken = currentTokenVal;
    expectedTokenSequences = expectedTokenSequencesVal;
    tokenImage = tokenImageVal;
  }

  /**
   * The following constructors are for use by you for whatever
   * purpose you can think of.  Constructing the exception in this
   * manner makes the exception behave in the normal way - i.e., as
   * documented in the class "Throwable".  The fields "errorToken",
   * "expectedTokenSequences", and "tokenImage" do not contain
   * relevant information.  The JavaCC generated code does not use
   * these constructors.
   */

  public ParseException() {
    super();
  }

  /** Constructor with message. */
  public ParseException(String message) {
    super(message);
  }


  /**
   * This is the last token that has been consumed successfully.  If
   * this object has been created due to a parse error, the token
   * followng this token will (therefore) be the first error token.
   */
  public Token currentToken;

  /**
   * Each entry in this array is an array of integers.  Each array
   * of integers represents a sequence of tokens (by their ordinal
   * values) that is expected at this point of the parse.
   */
  public int[][] expectedTokenSequences;

  /**
   * This is a reference to the "tokenImage" array of the generated
   * parser within which the parse error occurred.  This array is
   * defined in the generated ...Constants interface.
   */
  public String[] tokenImage;

  /**
   * It uses "currentToken" and "expectedTokenSequences" to generate a parse
   * error message and returns it.  If this object has been created
   * due to a parse error, and you do not catch it (it gets thrown
   * from the parser) the correct error message
   * gets displayed.
   */
  private static String initialise(Token currentToken,
                           int[][] expectedTokenSequences,
                           String[] tokenImage) {
    String eol = System.getProperty("line.separator", "\n");
    StringBuffer expected = new StringBuffer();
    int maxSize = 0;
    for (int i = 0; i < expectedTokenSequences.length; i++) {
      if (maxSize < expectedTokenSequences[i].length) {
        maxSize = expectedTokenSequences[i].length;
      }
      for (int j = 0; j < expectedTokenSequences[i].length; j++) {
        expected.append(tokenImage[expectedTokenSequences[i][j]]).append(' ');
      }
      if (expectedTokenSequences[i][expectedTokenSequences[i].length - 1] != 0) {
        expected.append("...");
      }
      expected.append(eol).append("    ");
    }
    String retval = "Encountered \"";
    Token tok = currentToken.next;
    for (int i = 0; i < maxSize; i++) {
      if (i != 0) retval += " ";
      if (tok.kind == 0) {
        retval += tokenImage[0];
        break;
      }
      retval += " " + tokenImage[tok.kind];
      retval += " \"";
      retval += add_escapes(tok.image);
      retval += " \"";
      tok = tok.next;
    }
    retval += "\" at line " + currentToken.next.beginLine + ", column " + currentToken.next.beginColumn;
    retval += "." + eol;
    if (expectedTokenSequences.length == 1) {
      retval += "Was expecting:" + eol + "    ";
    } else {
      retval += "Was expecting one of:" + eol + "    ";
    }
    retval += expected.toString();
    return retval;
  }

  /**
   * The end of line string for this machine.
   */
  protected String eol = System.getProperty("line.separator", "\n");

  /**
   * Used to convert raw characters to their escaped version
   * when these raw version cannot be used as part of an ASCII
   * string literal.
   */
  static String add_escapes(String str) {
      StringBuffer retval = new StringBuffer();
      char ch;
      for (int i = 0; i < str.length(); i++) {
        switch (str.charAt(i))
        {
           case 0 :
              continue;
           case '\b':
              retval.append("\\b");
              continue;
           case '\t':
              retval.append("\\t");
              continue;
           case '\n':
              retval.append("\\n");
              continue;
           case '\f':
              retval.append("\\f");
              continue;
           case '\r':
              retval.append("\\r");
              continue;
           case '\"':
              retval.append("\\\"");
              continue;
           case '\'':
              retval.append("\\\'");
              continue;
           case '\\':
              retval.append("\\\\");
              continue;
           default:
              if ((ch = str.charAt(i)) < 0x20 || ch > 0x7e) {
                 String s = "0000" + Integer.toString(ch, 16);
                 retval.append("\\u" + s.substring(s.length() - 4, s.length()));
              } else {
                 retval.append(ch);
              }
              continue;
        }
      }
      return retval.toString();
   }

}
/* JavaCC - OriginalChecksum=25e1ae9ad9614c4ce31c4b83f8a7397b (do not edit this line) */
diff --git a/solr/core/src/java/org/apache/solr/parser/QueryParser.java b/solr/core/src/java/org/apache/solr/parser/QueryParser.java
new file mode 100644
index 00000000000..7e7b95da157
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/parser/QueryParser.java
@@ -0,0 +1,701 @@
/* Generated By:JavaCC: Do not edit this line. QueryParser.java */
package org.apache.solr.parser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.search.QParser;


public class QueryParser extends SolrQueryParserBase implements QueryParserConstants {
  /** The default operator for parsing queries.
   * Use {@link QueryParserBase#setDefaultOperator} to change it.
   */
  static public enum Operator { OR, AND }

  /** Create a query parser.
   *  @param matchVersion  Lucene version to match. See <a href="#version">above</a>.
   *  @param defaultField  the default field for query terms.
   *  @param a   used to find terms in the query text.
   */
   public QueryParser(Version matchVersion, String defaultField, QParser parser) {
    this(new FastCharStream(new StringReader("")));
    init(matchVersion, defaultField, parser);
  }

// *   Query  ::= ( Clause )*
// *   Clause ::= ["+", "-"] [<TERM> ":"] ( <TERM> | "(" Query ")" )
  final public int Conjunction() throws ParseException {
  int ret = CONJ_NONE;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case AND:
    case OR:
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case AND:
        jj_consume_token(AND);
            ret = CONJ_AND;
        break;
      case OR:
        jj_consume_token(OR);
              ret = CONJ_OR;
        break;
      default:
        jj_la1[0] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
      break;
    default:
      jj_la1[1] = jj_gen;
      ;
    }
    {if (true) return ret;}
    throw new Error("Missing return statement in function");
  }

  final public int Modifiers() throws ParseException {
  int ret = MOD_NONE;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case NOT:
    case PLUS:
    case MINUS:
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case PLUS:
        jj_consume_token(PLUS);
              ret = MOD_REQ;
        break;
      case MINUS:
        jj_consume_token(MINUS);
                 ret = MOD_NOT;
        break;
      case NOT:
        jj_consume_token(NOT);
               ret = MOD_NOT;
        break;
      default:
        jj_la1[2] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
      break;
    default:
      jj_la1[3] = jj_gen;
      ;
    }
    {if (true) return ret;}
    throw new Error("Missing return statement in function");
  }

// This makes sure that there is no garbage after the query string
  final public Query TopLevelQuery(String field) throws ParseException, SyntaxError {
  Query q;
    q = Query(field);
    jj_consume_token(0);
    {if (true) return q;}
    throw new Error("Missing return statement in function");
  }

  final public Query Query(String field) throws ParseException, SyntaxError {
  List<BooleanClause> clauses = new ArrayList<BooleanClause>();
  Query q, firstQuery=null;
  int conj, mods;
    mods = Modifiers();
    q = Clause(field);
    addClause(clauses, CONJ_NONE, mods, q);
    if (mods == MOD_NONE)
        firstQuery=q;
    label_1:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case AND:
      case OR:
      case NOT:
      case PLUS:
      case MINUS:
      case BAREOPER:
      case LPAREN:
      case STAR:
      case QUOTED:
      case TERM:
      case PREFIXTERM:
      case WILDTERM:
      case REGEXPTERM:
      case RANGEIN_START:
      case RANGEEX_START:
      case LPARAMS:
      case NUMBER:
        ;
        break;
      default:
        jj_la1[4] = jj_gen;
        break label_1;
      }
      conj = Conjunction();
      mods = Modifiers();
      q = Clause(field);
      addClause(clauses, conj, mods, q);
    }
      if (clauses.size() == 1 && firstQuery != null)
        {if (true) return firstQuery;}
      else {
  {if (true) return getBooleanQuery(clauses);}
      }
    throw new Error("Missing return statement in function");
  }

  final public Query Clause(String field) throws ParseException, SyntaxError {
  Query q;
  Token fieldToken=null, boost=null;
  Token localParams=null;
    if (jj_2_1(2)) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case TERM:
        fieldToken = jj_consume_token(TERM);
        jj_consume_token(COLON);
                               field=discardEscapeChar(fieldToken.image);
        break;
      case STAR:
        jj_consume_token(STAR);
        jj_consume_token(COLON);
                      field="*";
        break;
      default:
        jj_la1[5] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
    } else {
      ;
    }
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case BAREOPER:
    case STAR:
    case QUOTED:
    case TERM:
    case PREFIXTERM:
    case WILDTERM:
    case REGEXPTERM:
    case RANGEIN_START:
    case RANGEEX_START:
    case NUMBER:
      q = Term(field);
      break;
    case LPAREN:
      jj_consume_token(LPAREN);
      q = Query(field);
      jj_consume_token(RPAREN);
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case CARAT:
        jj_consume_token(CARAT);
        boost = jj_consume_token(NUMBER);
        break;
      default:
        jj_la1[6] = jj_gen;
        ;
      }
      break;
    case LPARAMS:
      localParams = jj_consume_token(LPARAMS);
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case CARAT:
        jj_consume_token(CARAT);
        boost = jj_consume_token(NUMBER);
        break;
      default:
        jj_la1[7] = jj_gen;
        ;
      }
                                                          q=getLocalParams(field, localParams.image);
      break;
    default:
      jj_la1[8] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
       {if (true) return handleBoost(q, boost);}
    throw new Error("Missing return statement in function");
  }

  final public Query Term(String field) throws ParseException, SyntaxError {
  Token term, boost=null, fuzzySlop=null, goop1, goop2;
  boolean prefix = false;
  boolean wildcard = false;
  boolean fuzzy = false;
  boolean regexp = false;
  boolean startInc=false;
  boolean endInc=false;
  Query q;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case BAREOPER:
    case STAR:
    case TERM:
    case PREFIXTERM:
    case WILDTERM:
    case REGEXPTERM:
    case NUMBER:
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case TERM:
        term = jj_consume_token(TERM);
        break;
      case STAR:
        term = jj_consume_token(STAR);
                      wildcard=true;
        break;
      case PREFIXTERM:
        term = jj_consume_token(PREFIXTERM);
                            prefix=true;
        break;
      case WILDTERM:
        term = jj_consume_token(WILDTERM);
                          wildcard=true;
        break;
      case REGEXPTERM:
        term = jj_consume_token(REGEXPTERM);
                            regexp=true;
        break;
      case NUMBER:
        term = jj_consume_token(NUMBER);
        break;
      case BAREOPER:
        term = jj_consume_token(BAREOPER);
                          term.image = term.image.substring(0,1);
        break;
      default:
        jj_la1[9] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case FUZZY_SLOP:
        fuzzySlop = jj_consume_token(FUZZY_SLOP);
                               fuzzy=true;
        break;
      default:
        jj_la1[10] = jj_gen;
        ;
      }
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case CARAT:
        jj_consume_token(CARAT);
        boost = jj_consume_token(NUMBER);
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case FUZZY_SLOP:
          fuzzySlop = jj_consume_token(FUZZY_SLOP);
                                                        fuzzy=true;
          break;
        default:
          jj_la1[11] = jj_gen;
          ;
        }
        break;
      default:
        jj_la1[12] = jj_gen;
        ;
      }
      q = handleBareTokenQuery(getField(field), term, fuzzySlop, prefix, wildcard, fuzzy, regexp);
      break;
    case RANGEIN_START:
    case RANGEEX_START:
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case RANGEIN_START:
        jj_consume_token(RANGEIN_START);
                           startInc=true;
        break;
      case RANGEEX_START:
        jj_consume_token(RANGEEX_START);
        break;
      default:
        jj_la1[13] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case RANGE_GOOP:
        goop1 = jj_consume_token(RANGE_GOOP);
        break;
      case RANGE_QUOTED:
        goop1 = jj_consume_token(RANGE_QUOTED);
        break;
      default:
        jj_la1[14] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case RANGE_TO:
        jj_consume_token(RANGE_TO);
        break;
      default:
        jj_la1[15] = jj_gen;
        ;
      }
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case RANGE_GOOP:
        goop2 = jj_consume_token(RANGE_GOOP);
        break;
      case RANGE_QUOTED:
        goop2 = jj_consume_token(RANGE_QUOTED);
        break;
      default:
        jj_la1[16] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case RANGEIN_END:
        jj_consume_token(RANGEIN_END);
                         endInc=true;
        break;
      case RANGEEX_END:
        jj_consume_token(RANGEEX_END);
        break;
      default:
        jj_la1[17] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case CARAT:
        jj_consume_token(CARAT);
        boost = jj_consume_token(NUMBER);
        break;
      default:
        jj_la1[18] = jj_gen;
        ;
      }
         boolean startOpen=false;
         boolean endOpen=false;
         if (goop1.kind == RANGE_QUOTED) {
           goop1.image = goop1.image.substring(1, goop1.image.length()-1);
         } else if ("*".equals(goop1.image)) {
           startOpen=true;
         }
         if (goop2.kind == RANGE_QUOTED) {
           goop2.image = goop2.image.substring(1, goop2.image.length()-1);
         } else if ("*".equals(goop2.image)) {
           endOpen=true;
         }
         q = getRangeQuery(getField(field), startOpen ? null : discardEscapeChar(goop1.image), endOpen ? null : discardEscapeChar(goop2.image), startInc, endInc);
      break;
    case QUOTED:
      term = jj_consume_token(QUOTED);
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case FUZZY_SLOP:
        fuzzySlop = jj_consume_token(FUZZY_SLOP);
        break;
      default:
        jj_la1[19] = jj_gen;
        ;
      }
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case CARAT:
        jj_consume_token(CARAT);
        boost = jj_consume_token(NUMBER);
        break;
      default:
        jj_la1[20] = jj_gen;
        ;
      }
        q = handleQuotedTerm(getField(field), term, fuzzySlop);
      break;
    default:
      jj_la1[21] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    {if (true) return handleBoost(q, boost);}
    throw new Error("Missing return statement in function");
  }

  private boolean jj_2_1(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_1(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(0, xla); }
  }

  private boolean jj_3R_3() {
    if (jj_scan_token(STAR)) return true;
    if (jj_scan_token(COLON)) return true;
    return false;
  }

  private boolean jj_3R_2() {
    if (jj_scan_token(TERM)) return true;
    if (jj_scan_token(COLON)) return true;
    return false;
  }

  private boolean jj_3_1() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3R_2()) {
    jj_scanpos = xsp;
    if (jj_3R_3()) return true;
    }
    return false;
  }

  /** Generated Token Manager. */
  public QueryParserTokenManager token_source;
  /** Current token. */
  public Token token;
  /** Next token. */
  public Token jj_nt;
  private int jj_ntk;
  private Token jj_scanpos, jj_lastpos;
  private int jj_la;
  private int jj_gen;
  final private int[] jj_la1 = new int[22];
  static private int[] jj_la1_0;
  static private int[] jj_la1_1;
  static {
      jj_la1_init_0();
      jj_la1_init_1();
   }
   private static void jj_la1_init_0() {
      jj_la1_0 = new int[] {0x600,0x600,0x3800,0x3800,0x7f54fe00,0x440000,0x80000,0x80000,0x7f54c000,0x47444000,0x800000,0x800000,0x80000,0x18000000,0x0,0x80000000,0x0,0x0,0x80000,0x800000,0x80000,0x5f544000,};
   }
   private static void jj_la1_init_1() {
      jj_la1_1 = new int[] {0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0xc,0x0,0xc,0x3,0x0,0x0,0x0,0x0,};
   }
  final private JJCalls[] jj_2_rtns = new JJCalls[1];
  private boolean jj_rescan = false;
  private int jj_gc = 0;

  /** Constructor with user supplied CharStream. */
  protected QueryParser(CharStream stream) {
    token_source = new QueryParserTokenManager(stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 22; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(CharStream stream) {
    token_source.ReInit(stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 22; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Constructor with generated Token Manager. */
  protected QueryParser(QueryParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 22; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(QueryParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 22; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  private Token jj_consume_token(int kind) throws ParseException {
    Token oldToken;
    if ((oldToken = token).next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    if (token.kind == kind) {
      jj_gen++;
      if (++jj_gc > 100) {
        jj_gc = 0;
        for (int i = 0; i < jj_2_rtns.length; i++) {
          JJCalls c = jj_2_rtns[i];
          while (c != null) {
            if (c.gen < jj_gen) c.first = null;
            c = c.next;
          }
        }
      }
      return token;
    }
    token = oldToken;
    jj_kind = kind;
    throw generateParseException();
  }

  static private final class LookaheadSuccess extends java.lang.Error { }
  final private LookaheadSuccess jj_ls = new LookaheadSuccess();
  private boolean jj_scan_token(int kind) {
    if (jj_scanpos == jj_lastpos) {
      jj_la--;
      if (jj_scanpos.next == null) {
        jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();
      } else {
        jj_lastpos = jj_scanpos = jj_scanpos.next;
      }
    } else {
      jj_scanpos = jj_scanpos.next;
    }
    if (jj_rescan) {
      int i = 0; Token tok = token;
      while (tok != null && tok != jj_scanpos) { i++; tok = tok.next; }
      if (tok != null) jj_add_error_token(kind, i);
    }
    if (jj_scanpos.kind != kind) return true;
    if (jj_la == 0 && jj_scanpos == jj_lastpos) throw jj_ls;
    return false;
  }


/** Get the next Token. */
  final public Token getNextToken() {
    if (token.next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    jj_gen++;
    return token;
  }

/** Get the specific Token. */
  final public Token getToken(int index) {
    Token t = token;
    for (int i = 0; i < index; i++) {
      if (t.next != null) t = t.next;
      else t = t.next = token_source.getNextToken();
    }
    return t;
  }

  private int jj_ntk() {
    if ((jj_nt=token.next) == null)
      return (jj_ntk = (token.next=token_source.getNextToken()).kind);
    else
      return (jj_ntk = jj_nt.kind);
  }

  private java.util.List<int[]> jj_expentries = new java.util.ArrayList<int[]>();
  private int[] jj_expentry;
  private int jj_kind = -1;
  private int[] jj_lasttokens = new int[100];
  private int jj_endpos;

  private void jj_add_error_token(int kind, int pos) {
    if (pos >= 100) return;
    if (pos == jj_endpos + 1) {
      jj_lasttokens[jj_endpos++] = kind;
    } else if (jj_endpos != 0) {
      jj_expentry = new int[jj_endpos];
      for (int i = 0; i < jj_endpos; i++) {
        jj_expentry[i] = jj_lasttokens[i];
      }
      jj_entries_loop: for (java.util.Iterator<?> it = jj_expentries.iterator(); it.hasNext();) {
        int[] oldentry = (int[])(it.next());
        if (oldentry.length == jj_expentry.length) {
          for (int i = 0; i < jj_expentry.length; i++) {
            if (oldentry[i] != jj_expentry[i]) {
              continue jj_entries_loop;
            }
          }
          jj_expentries.add(jj_expentry);
          break jj_entries_loop;
        }
      }
      if (pos != 0) jj_lasttokens[(jj_endpos = pos) - 1] = kind;
    }
  }

  /** Generate ParseException. */
  public ParseException generateParseException() {
    jj_expentries.clear();
    boolean[] la1tokens = new boolean[36];
    if (jj_kind >= 0) {
      la1tokens[jj_kind] = true;
      jj_kind = -1;
    }
    for (int i = 0; i < 22; i++) {
      if (jj_la1[i] == jj_gen) {
        for (int j = 0; j < 32; j++) {
          if ((jj_la1_0[i] & (1<<j)) != 0) {
            la1tokens[j] = true;
          }
          if ((jj_la1_1[i] & (1<<j)) != 0) {
            la1tokens[32+j] = true;
          }
        }
      }
    }
    for (int i = 0; i < 36; i++) {
      if (la1tokens[i]) {
        jj_expentry = new int[1];
        jj_expentry[0] = i;
        jj_expentries.add(jj_expentry);
      }
    }
    jj_endpos = 0;
    jj_rescan_token();
    jj_add_error_token(0, 0);
    int[][] exptokseq = new int[jj_expentries.size()][];
    for (int i = 0; i < jj_expentries.size(); i++) {
      exptokseq[i] = jj_expentries.get(i);
    }
    return new ParseException(token, exptokseq, tokenImage);
  }

  /** Enable tracing. */
  final public void enable_tracing() {
  }

  /** Disable tracing. */
  final public void disable_tracing() {
  }

  private void jj_rescan_token() {
    jj_rescan = true;
    for (int i = 0; i < 1; i++) {
    try {
      JJCalls p = jj_2_rtns[i];
      do {
        if (p.gen > jj_gen) {
          jj_la = p.arg; jj_lastpos = jj_scanpos = p.first;
          switch (i) {
            case 0: jj_3_1(); break;
          }
        }
        p = p.next;
      } while (p != null);
      } catch(LookaheadSuccess ls) { }
    }
    jj_rescan = false;
  }

  private void jj_save(int index, int xla) {
    JJCalls p = jj_2_rtns[index];
    while (p.gen > jj_gen) {
      if (p.next == null) { p = p.next = new JJCalls(); break; }
      p = p.next;
    }
    p.gen = jj_gen + xla - jj_la; p.first = token; p.arg = xla;
  }

  static final class JJCalls {
    int gen;
    Token first;
    int arg;
    JJCalls next;
  }

}
diff --git a/solr/core/src/java/org/apache/solr/parser/QueryParser.jj b/solr/core/src/java/org/apache/solr/parser/QueryParser.jj
new file mode 100644
index 00000000000..697c99d51ef
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/parser/QueryParser.jj
@@ -0,0 +1,267 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

options {
  STATIC=false;
  JAVA_UNICODE_ESCAPE=true;
  USER_CHAR_STREAM=true;
}

PARSER_BEGIN(QueryParser)

package org.apache.solr.parser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.search.QParser;


public class QueryParser extends SolrQueryParserBase {
  /** The default operator for parsing queries.
   * Use {@link QueryParserBase#setDefaultOperator} to change it.
   */
  static public enum Operator { OR, AND }
  
  /** Create a query parser.
   *  @param matchVersion  Lucene version to match. See <a href="#version">above</a>.
   *  @param defaultField  the default field for query terms.
   *  @param a   used to find terms in the query text.
   */
   public QueryParser(Version matchVersion, String defaultField, QParser parser) {
    this(new FastCharStream(new StringReader("")));
    init(matchVersion, defaultField, parser);
  }
}

PARSER_END(QueryParser)

/* ***************** */
/* Token Definitions */
/* ***************** */

<*> TOKEN : {
  <#_NUM_CHAR:   ["0"-"9"] >
 // every character that follows a backslash is considered as an escaped character
 | <#_ESCAPED_CHAR: "\\" ~[] >
 | <#_TERM_START_CHAR: ( ~[ " ", "\t", "\n", "\r", "\u3000", "+", "-", "!", "(", ")", ":", "^",
                      "[", "]", "\"", "{", "}", "~", "*", "?", "\\", "/" ]
                      | <_ESCAPED_CHAR> ) >
 | <#_TERM_CHAR: ( <_TERM_START_CHAR>
                       | <_ESCAPED_CHAR> | "-" | "+" | "/" ) >
 | <#_WHITESPACE:  ( " " | "\t" | "\n" | "\r" | "\u3000") >
 | <#_QUOTED_CHAR:  ( ~[ "\"", "\\" ] | <_ESCAPED_CHAR> ) >
 | <#_SQUOTED_CHAR:  ( ~[ "'", "\\" ] | <_ESCAPED_CHAR> ) >
}

<DEFAULT, Range> SKIP : {
  < <_WHITESPACE>>
}

<DEFAULT> TOKEN : {
  <AND:       ("AND" | "&&") >
 | <OR:        ("OR" | "||") >
 | <NOT:       ("NOT" | "!") >
 | <PLUS:      "+" >
 | <MINUS:     "-" >
 | <BAREOPER:    ("+"|"-"|"!") <_WHITESPACE> >
 | <LPAREN:    "(" >
 | <RPAREN:    ")" >
 | <COLON:     ":" >
 | <STAR:      "*" >
 | <CARAT:     "^" > : Boost
 | <QUOTED:     "\"" (<_QUOTED_CHAR>)* "\"">
 | <SQUOTED:     "'" (<_SQUOTED_CHAR>)* "'">
 | <TERM:      <_TERM_START_CHAR> (<_TERM_CHAR>)*  >
 | <FUZZY_SLOP:     "~" ( (<_NUM_CHAR>)+ ( "." (<_NUM_CHAR>)+ )? )? >
 | <PREFIXTERM:  ("*") | ( <_TERM_START_CHAR> (<_TERM_CHAR>)* "*" ) >
 | <WILDTERM:  (<_TERM_START_CHAR> | [ "*", "?" ]) (<_TERM_CHAR> | ( [ "*", "?" ] ))* >
 | <REGEXPTERM: "/" (~[ "/" ] | "\\/" )* "/" >
 | <RANGEIN_START: "[" > : Range
 | <RANGEEX_START: "{" > : Range
 | <LPARAMS:     ("{!" ( (<_WHITESPACE>)* (~["=","}"])+ ( "=" (<QUOTED> | <SQUOTED> | (~[" ","}"])+ )? )? )* "}")+  (~[")"," ","\t","\n","{","^"])*  >
}

<Boost> TOKEN : {
 <NUMBER:    ("-")? (<_NUM_CHAR>)+ ( "." (<_NUM_CHAR>)+ )? > : DEFAULT
}

<Range> TOKEN : {
 <RANGE_TO: "TO">
 | <RANGEIN_END: "]"> : DEFAULT
 | <RANGEEX_END: "}"> : DEFAULT
 | <RANGE_QUOTED: "\"" (~["\""] | "\\\"")+ "\"">
 | <RANGE_GOOP: (~[ " ", "]", "}" ])+ >
}

// *   Query  ::= ( Clause )*
// *   Clause ::= ["+", "-"] [<TERM> ":"] ( <TERM> | "(" Query ")" )

int Conjunction() : {
  int ret = CONJ_NONE;
}
{
  [
    <AND> { ret = CONJ_AND; }
    | <OR>  { ret = CONJ_OR; }
  ]
  { return ret; }
}

int Modifiers() : {
  int ret = MOD_NONE;
}
{
  [
     <PLUS> { ret = MOD_REQ; }
     | <MINUS> { ret = MOD_NOT; }
     | <NOT> { ret = MOD_NOT; }
  ]
  { return ret; }
}

// This makes sure that there is no garbage after the query string
Query TopLevelQuery(String field) throws SyntaxError :
{
  Query q;
}
{
  q=Query(field) <EOF>
  {
    return q;
  }
}

Query Query(String field) throws SyntaxError :
{
  List<BooleanClause> clauses = new ArrayList<BooleanClause>();
  Query q, firstQuery=null;
  int conj, mods;
}
{
  mods=Modifiers() q=Clause(field)
  {
    addClause(clauses, CONJ_NONE, mods, q);
    if (mods == MOD_NONE)
        firstQuery=q;
  }
  (
    conj=Conjunction() mods=Modifiers() q=Clause(field)
    { addClause(clauses, conj, mods, q); }
  )*
    {
      if (clauses.size() == 1 && firstQuery != null)
        return firstQuery;
      else {
  return getBooleanQuery(clauses);
      }
    }
}

Query Clause(String field) throws SyntaxError : {
  Query q;
  Token fieldToken=null, boost=null;
  Token localParams=null;
}
{
 
  [
    LOOKAHEAD(2)
    (
    fieldToken=<TERM> <COLON> {field=discardEscapeChar(fieldToken.image);}
    | <STAR> <COLON> {field="*";}
    )
  ]


  (
   q=Term(field)
   | <LPAREN> q=Query(field) <RPAREN> (<CARAT> boost=<NUMBER>)?
   | (localParams = <LPARAMS> (<CARAT> boost=<NUMBER>)? { q=getLocalParams(field, localParams.image); }  )
  )
    {  return handleBoost(q, boost); }
}


Query Term(String field) throws SyntaxError : {
  Token term, boost=null, fuzzySlop=null, goop1, goop2;
  boolean prefix = false;
  boolean wildcard = false;
  boolean fuzzy = false;
  boolean regexp = false;
  boolean startInc=false;
  boolean endInc=false;
  Query q;
}
{
  (
    (
      term=<TERM>
      | term=<STAR> { wildcard=true; }
      | term=<PREFIXTERM> { prefix=true; }
      | term=<WILDTERM> { wildcard=true; }
      | term=<REGEXPTERM> { regexp=true; }
      | term=<NUMBER>
      | term=<BAREOPER> { term.image = term.image.substring(0,1); }
    )
    [ fuzzySlop=<FUZZY_SLOP> { fuzzy=true; } ]
    [ <CARAT> boost=<NUMBER> [ fuzzySlop=<FUZZY_SLOP> { fuzzy=true; } ] ]
    {
      q = handleBareTokenQuery(getField(field), term, fuzzySlop, prefix, wildcard, fuzzy, regexp);
    }
    | ( ( <RANGEIN_START> {startInc=true;} | <RANGEEX_START> )
        ( goop1=<RANGE_GOOP>|goop1=<RANGE_QUOTED> )
        [ <RANGE_TO> ]
        ( goop2=<RANGE_GOOP>|goop2=<RANGE_QUOTED> )
        ( <RANGEIN_END> {endInc=true;} | <RANGEEX_END>))
      [ <CARAT> boost=<NUMBER> ]
       {
         boolean startOpen=false;
         boolean endOpen=false;
         if (goop1.kind == RANGE_QUOTED) {
           goop1.image = goop1.image.substring(1, goop1.image.length()-1);
         } else if ("*".equals(goop1.image)) {
           startOpen=true;
         }
         if (goop2.kind == RANGE_QUOTED) {
           goop2.image = goop2.image.substring(1, goop2.image.length()-1);
         } else if ("*".equals(goop2.image)) {
           endOpen=true;
         }
         q = getRangeQuery(getField(field), startOpen ? null : discardEscapeChar(goop1.image), endOpen ? null : discardEscapeChar(goop2.image), startInc, endInc);
       }
    | term=<QUOTED>
      [ fuzzySlop=<FUZZY_SLOP> ]
      [ <CARAT> boost=<NUMBER> ]
      {
        q = handleQuotedTerm(getField(field), term, fuzzySlop);
      }
  )
  {
    return handleBoost(q, boost);
  }
}
diff --git a/solr/core/src/java/org/apache/solr/parser/QueryParserConstants.java b/solr/core/src/java/org/apache/solr/parser/QueryParserConstants.java
new file mode 100644
index 00000000000..2bb3d75a52f
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/parser/QueryParserConstants.java
@@ -0,0 +1,129 @@
/* Generated By:JavaCC: Do not edit this line. QueryParserConstants.java */
package org.apache.solr.parser;


/**
 * Token literal values and constants.
 * Generated by org.javacc.parser.OtherFilesGen#start()
 */
public interface QueryParserConstants {

  /** End of File. */
  int EOF = 0;
  /** RegularExpression Id. */
  int _NUM_CHAR = 1;
  /** RegularExpression Id. */
  int _ESCAPED_CHAR = 2;
  /** RegularExpression Id. */
  int _TERM_START_CHAR = 3;
  /** RegularExpression Id. */
  int _TERM_CHAR = 4;
  /** RegularExpression Id. */
  int _WHITESPACE = 5;
  /** RegularExpression Id. */
  int _QUOTED_CHAR = 6;
  /** RegularExpression Id. */
  int _SQUOTED_CHAR = 7;
  /** RegularExpression Id. */
  int AND = 9;
  /** RegularExpression Id. */
  int OR = 10;
  /** RegularExpression Id. */
  int NOT = 11;
  /** RegularExpression Id. */
  int PLUS = 12;
  /** RegularExpression Id. */
  int MINUS = 13;
  /** RegularExpression Id. */
  int BAREOPER = 14;
  /** RegularExpression Id. */
  int LPAREN = 15;
  /** RegularExpression Id. */
  int RPAREN = 16;
  /** RegularExpression Id. */
  int COLON = 17;
  /** RegularExpression Id. */
  int STAR = 18;
  /** RegularExpression Id. */
  int CARAT = 19;
  /** RegularExpression Id. */
  int QUOTED = 20;
  /** RegularExpression Id. */
  int SQUOTED = 21;
  /** RegularExpression Id. */
  int TERM = 22;
  /** RegularExpression Id. */
  int FUZZY_SLOP = 23;
  /** RegularExpression Id. */
  int PREFIXTERM = 24;
  /** RegularExpression Id. */
  int WILDTERM = 25;
  /** RegularExpression Id. */
  int REGEXPTERM = 26;
  /** RegularExpression Id. */
  int RANGEIN_START = 27;
  /** RegularExpression Id. */
  int RANGEEX_START = 28;
  /** RegularExpression Id. */
  int LPARAMS = 29;
  /** RegularExpression Id. */
  int NUMBER = 30;
  /** RegularExpression Id. */
  int RANGE_TO = 31;
  /** RegularExpression Id. */
  int RANGEIN_END = 32;
  /** RegularExpression Id. */
  int RANGEEX_END = 33;
  /** RegularExpression Id. */
  int RANGE_QUOTED = 34;
  /** RegularExpression Id. */
  int RANGE_GOOP = 35;

  /** Lexical state. */
  int Boost = 0;
  /** Lexical state. */
  int Range = 1;
  /** Lexical state. */
  int DEFAULT = 2;

  /** Literal token values. */
  String[] tokenImage = {
    "<EOF>",
    "<_NUM_CHAR>",
    "<_ESCAPED_CHAR>",
    "<_TERM_START_CHAR>",
    "<_TERM_CHAR>",
    "<_WHITESPACE>",
    "<_QUOTED_CHAR>",
    "<_SQUOTED_CHAR>",
    "<token of kind 8>",
    "<AND>",
    "<OR>",
    "<NOT>",
    "\"+\"",
    "\"-\"",
    "<BAREOPER>",
    "\"(\"",
    "\")\"",
    "\":\"",
    "\"*\"",
    "\"^\"",
    "<QUOTED>",
    "<SQUOTED>",
    "<TERM>",
    "<FUZZY_SLOP>",
    "<PREFIXTERM>",
    "<WILDTERM>",
    "<REGEXPTERM>",
    "\"[\"",
    "\"{\"",
    "<LPARAMS>",
    "<NUMBER>",
    "\"TO\"",
    "\"]\"",
    "\"}\"",
    "<RANGE_QUOTED>",
    "<RANGE_GOOP>",
  };

}
diff --git a/solr/core/src/java/org/apache/solr/parser/QueryParserTokenManager.java b/solr/core/src/java/org/apache/solr/parser/QueryParserTokenManager.java
new file mode 100644
index 00000000000..0f6c1529a03
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/parser/QueryParserTokenManager.java
@@ -0,0 +1,1261 @@
/* Generated By:JavaCC: Do not edit this line. QueryParserTokenManager.java */
package org.apache.solr.parser;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.search.QParser;

/** Token Manager. */
public class QueryParserTokenManager implements QueryParserConstants
{

  /** Debug output. */
  public  java.io.PrintStream debugStream = System.out;
  /** Set debug output. */
  public  void setDebugStream(java.io.PrintStream ds) { debugStream = ds; }
private final int jjStopStringLiteralDfa_2(int pos, long active0)
{
   switch (pos)
   {
      default :
         return -1;
   }
}
private final int jjStartNfa_2(int pos, long active0)
{
   return jjMoveNfa_2(jjStopStringLiteralDfa_2(pos, active0), pos + 1);
}
private int jjStopAtPos(int pos, int kind)
{
   jjmatchedKind = kind;
   jjmatchedPos = pos;
   return pos + 1;
}
private int jjMoveStringLiteralDfa0_2()
{
   switch(curChar)
   {
      case 40:
         return jjStopAtPos(0, 15);
      case 41:
         return jjStopAtPos(0, 16);
      case 42:
         return jjStartNfaWithStates_2(0, 18, 66);
      case 43:
         return jjStartNfaWithStates_2(0, 12, 15);
      case 45:
         return jjStartNfaWithStates_2(0, 13, 15);
      case 58:
         return jjStopAtPos(0, 17);
      case 91:
         return jjStopAtPos(0, 27);
      case 94:
         return jjStopAtPos(0, 19);
      case 123:
         return jjStartNfaWithStates_2(0, 28, 40);
      default :
         return jjMoveNfa_2(0, 0);
   }
}
private int jjStartNfaWithStates_2(int pos, int kind, int state)
{
   jjmatchedKind = kind;
   jjmatchedPos = pos;
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) { return pos + 1; }
   return jjMoveNfa_2(state, pos + 1);
}
static final long[] jjbitVec0 = {
   0x1L, 0x0L, 0x0L, 0x0L
};
static final long[] jjbitVec1 = {
   0xfffffffffffffffeL, 0xffffffffffffffffL, 0xffffffffffffffffL, 0xffffffffffffffffL
};
static final long[] jjbitVec3 = {
   0x0L, 0x0L, 0xffffffffffffffffL, 0xffffffffffffffffL
};
static final long[] jjbitVec4 = {
   0xfffefffffffffffeL, 0xffffffffffffffffL, 0xffffffffffffffffL, 0xffffffffffffffffL
};
private int jjMoveNfa_2(int startState, int curPos)
{
   int startsAt = 0;
   jjnewStateCnt = 66;
   int i = 1;
   jjstateSet[0] = startState;
   int kind = 0x7fffffff;
   for (;;)
   {
      if (++jjround == 0x7fffffff)
         ReInitRounds();
      if (curChar < 64)
      {
         long l = 1L << curChar;
         do
         {
            switch(jjstateSet[--i])
            {
               case 66:
               case 32:
                  if ((0xfbfffcf8ffffd9ffL & l) == 0L)
                     break;
                  if (kind > 25)
                     kind = 25;
                  jjCheckNAddTwoStates(32, 33);
                  break;
               case 0:
                  if ((0xfbff54f8ffffd9ffL & l) != 0L)
                  {
                     if (kind > 25)
                        kind = 25;
                     jjCheckNAddTwoStates(32, 33);
                  }
                  else if ((0x100002600L & l) != 0L)
                  {
                     if (kind > 8)
                        kind = 8;
                  }
                  else if ((0x280200000000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 15;
                  else if (curChar == 47)
                     jjCheckNAddStates(0, 2);
                  else if (curChar == 34)
                     jjCheckNAddStates(3, 5);
                  if ((0x7bff50f8ffffd9ffL & l) != 0L)
                  {
                     if (kind > 22)
                        kind = 22;
                     jjCheckNAddStates(6, 10);
                  }
                  else if (curChar == 42)
                  {
                     if (kind > 24)
                        kind = 24;
                  }
                  else if (curChar == 33)
                  {
                     if (kind > 11)
                        kind = 11;
                  }
                  if (curChar == 39)
                     jjCheckNAddStates(11, 13);
                  else if (curChar == 38)
                     jjstateSet[jjnewStateCnt++] = 4;
                  break;
               case 4:
                  if (curChar == 38 && kind > 9)
                     kind = 9;
                  break;
               case 5:
                  if (curChar == 38)
                     jjstateSet[jjnewStateCnt++] = 4;
                  break;
               case 13:
                  if (curChar == 33 && kind > 11)
                     kind = 11;
                  break;
               case 14:
                  if ((0x280200000000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 15;
                  break;
               case 15:
                  if ((0x100002600L & l) != 0L && kind > 14)
                     kind = 14;
                  break;
               case 16:
                  if (curChar == 34)
                     jjCheckNAddStates(3, 5);
                  break;
               case 17:
                  if ((0xfffffffbffffffffL & l) != 0L)
                     jjCheckNAddStates(3, 5);
                  break;
               case 19:
                  jjCheckNAddStates(3, 5);
                  break;
               case 20:
                  if (curChar == 34 && kind > 20)
                     kind = 20;
                  break;
               case 21:
                  if (curChar == 39)
                     jjCheckNAddStates(11, 13);
                  break;
               case 22:
                  if ((0xffffff7fffffffffL & l) != 0L)
                     jjCheckNAddStates(11, 13);
                  break;
               case 24:
                  jjCheckNAddStates(11, 13);
                  break;
               case 25:
                  if (curChar == 39 && kind > 21)
                     kind = 21;
                  break;
               case 27:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 23)
                     kind = 23;
                  jjAddStates(14, 15);
                  break;
               case 28:
                  if (curChar == 46)
                     jjCheckNAdd(29);
                  break;
               case 29:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 23)
                     kind = 23;
                  jjCheckNAdd(29);
                  break;
               case 30:
                  if (curChar == 42 && kind > 24)
                     kind = 24;
                  break;
               case 31:
                  if ((0xfbff54f8ffffd9ffL & l) == 0L)
                     break;
                  if (kind > 25)
                     kind = 25;
                  jjCheckNAddTwoStates(32, 33);
                  break;
               case 34:
                  if (kind > 25)
                     kind = 25;
                  jjCheckNAddTwoStates(32, 33);
                  break;
               case 35:
               case 37:
                  if (curChar == 47)
                     jjCheckNAddStates(0, 2);
                  break;
               case 36:
                  if ((0xffff7fffffffffffL & l) != 0L)
                     jjCheckNAddStates(0, 2);
                  break;
               case 39:
                  if (curChar == 47 && kind > 26)
                     kind = 26;
                  break;
               case 40:
                  if (curChar == 33)
                     jjCheckNAddStates(16, 18);
                  break;
               case 41:
                  if ((0x100002600L & l) != 0L)
                     jjCheckNAddTwoStates(41, 42);
                  break;
               case 42:
                  if ((0xdfffffffffffffffL & l) != 0L)
                     jjCheckNAddStates(19, 22);
                  break;
               case 43:
                  if (curChar == 61)
                     jjCheckNAddStates(23, 28);
                  break;
               case 44:
                  if (curChar == 34)
                     jjCheckNAddStates(29, 31);
                  break;
               case 45:
                  if ((0xfffffffbffffffffL & l) != 0L)
                     jjCheckNAddStates(29, 31);
                  break;
               case 47:
                  jjCheckNAddStates(29, 31);
                  break;
               case 48:
                  if (curChar == 34)
                     jjCheckNAddStates(16, 18);
                  break;
               case 51:
                  if ((0xfffffdfefffff9ffL & l) == 0L)
                     break;
                  if (kind > 29)
                     kind = 29;
                  jjstateSet[jjnewStateCnt++] = 51;
                  break;
               case 52:
                  if (curChar == 39)
                     jjCheckNAddStates(32, 34);
                  break;
               case 53:
                  if ((0xffffff7fffffffffL & l) != 0L)
                     jjCheckNAddStates(32, 34);
                  break;
               case 55:
                  jjCheckNAddStates(32, 34);
                  break;
               case 56:
                  if (curChar == 39)
                     jjCheckNAddStates(16, 18);
                  break;
               case 57:
                  if ((0xfffffffeffffffffL & l) != 0L)
                     jjCheckNAddStates(35, 38);
                  break;
               case 58:
                  if ((0x7bff50f8ffffd9ffL & l) == 0L)
                     break;
                  if (kind > 22)
                     kind = 22;
                  jjCheckNAddStates(6, 10);
                  break;
               case 59:
                  if ((0x7bfff8f8ffffd9ffL & l) == 0L)
                     break;
                  if (kind > 22)
                     kind = 22;
                  jjCheckNAddTwoStates(59, 60);
                  break;
               case 61:
                  if (kind > 22)
                     kind = 22;
                  jjCheckNAddTwoStates(59, 60);
                  break;
               case 62:
                  if ((0x7bfff8f8ffffd9ffL & l) != 0L)
                     jjCheckNAddStates(39, 41);
                  break;
               case 64:
                  jjCheckNAddStates(39, 41);
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else if (curChar < 128)
      {
         long l = 1L << (curChar & 077);
         do
         {
            switch(jjstateSet[--i])
            {
               case 66:
                  if ((0x97ffffff87ffffffL & l) != 0L)
                  {
                     if (kind > 25)
                        kind = 25;
                     jjCheckNAddTwoStates(32, 33);
                  }
                  else if (curChar == 92)
                     jjCheckNAddTwoStates(34, 34);
                  break;
               case 0:
                  if ((0x97ffffff87ffffffL & l) != 0L)
                  {
                     if (kind > 22)
                        kind = 22;
                     jjCheckNAddStates(6, 10);
                  }
                  else if (curChar == 92)
                     jjCheckNAddStates(42, 44);
                  else if (curChar == 123)
                     jjstateSet[jjnewStateCnt++] = 40;
                  else if (curChar == 126)
                  {
                     if (kind > 23)
                        kind = 23;
                     jjstateSet[jjnewStateCnt++] = 27;
                  }
                  if ((0x97ffffff87ffffffL & l) != 0L)
                  {
                     if (kind > 25)
                        kind = 25;
                     jjCheckNAddTwoStates(32, 33);
                  }
                  if (curChar == 78)
                     jjstateSet[jjnewStateCnt++] = 11;
                  else if (curChar == 124)
                     jjstateSet[jjnewStateCnt++] = 8;
                  else if (curChar == 79)
                     jjstateSet[jjnewStateCnt++] = 6;
                  else if (curChar == 65)
                     jjstateSet[jjnewStateCnt++] = 2;
                  break;
               case 1:
                  if (curChar == 68 && kind > 9)
                     kind = 9;
                  break;
               case 2:
                  if (curChar == 78)
                     jjstateSet[jjnewStateCnt++] = 1;
                  break;
               case 3:
                  if (curChar == 65)
                     jjstateSet[jjnewStateCnt++] = 2;
                  break;
               case 6:
                  if (curChar == 82 && kind > 10)
                     kind = 10;
                  break;
               case 7:
                  if (curChar == 79)
                     jjstateSet[jjnewStateCnt++] = 6;
                  break;
               case 8:
                  if (curChar == 124 && kind > 10)
                     kind = 10;
                  break;
               case 9:
                  if (curChar == 124)
                     jjstateSet[jjnewStateCnt++] = 8;
                  break;
               case 10:
                  if (curChar == 84 && kind > 11)
                     kind = 11;
                  break;
               case 11:
                  if (curChar == 79)
                     jjstateSet[jjnewStateCnt++] = 10;
                  break;
               case 12:
                  if (curChar == 78)
                     jjstateSet[jjnewStateCnt++] = 11;
                  break;
               case 17:
                  if ((0xffffffffefffffffL & l) != 0L)
                     jjCheckNAddStates(3, 5);
                  break;
               case 18:
                  if (curChar == 92)
                     jjstateSet[jjnewStateCnt++] = 19;
                  break;
               case 19:
                  jjCheckNAddStates(3, 5);
                  break;
               case 22:
                  if ((0xffffffffefffffffL & l) != 0L)
                     jjCheckNAddStates(11, 13);
                  break;
               case 23:
                  if (curChar == 92)
                     jjstateSet[jjnewStateCnt++] = 24;
                  break;
               case 24:
                  jjCheckNAddStates(11, 13);
                  break;
               case 26:
                  if (curChar != 126)
                     break;
                  if (kind > 23)
                     kind = 23;
                  jjstateSet[jjnewStateCnt++] = 27;
                  break;
               case 31:
                  if ((0x97ffffff87ffffffL & l) == 0L)
                     break;
                  if (kind > 25)
                     kind = 25;
                  jjCheckNAddTwoStates(32, 33);
                  break;
               case 32:
                  if ((0x97ffffff87ffffffL & l) == 0L)
                     break;
                  if (kind > 25)
                     kind = 25;
                  jjCheckNAddTwoStates(32, 33);
                  break;
               case 33:
                  if (curChar == 92)
                     jjCheckNAddTwoStates(34, 34);
                  break;
               case 34:
                  if (kind > 25)
                     kind = 25;
                  jjCheckNAddTwoStates(32, 33);
                  break;
               case 36:
                  jjAddStates(0, 2);
                  break;
               case 38:
                  if (curChar == 92)
                     jjstateSet[jjnewStateCnt++] = 37;
                  break;
               case 42:
                  if ((0xdfffffffffffffffL & l) != 0L)
                     jjCheckNAddStates(19, 22);
                  break;
               case 45:
                  if ((0xffffffffefffffffL & l) != 0L)
                     jjCheckNAddStates(29, 31);
                  break;
               case 46:
                  if (curChar == 92)
                     jjstateSet[jjnewStateCnt++] = 47;
                  break;
               case 47:
                  jjCheckNAddStates(29, 31);
                  break;
               case 49:
                  if (curChar != 125)
                     break;
                  if (kind > 29)
                     kind = 29;
                  jjCheckNAddTwoStates(50, 51);
                  break;
               case 50:
                  if (curChar == 123)
                     jjstateSet[jjnewStateCnt++] = 40;
                  break;
               case 51:
                  if ((0xf7ffffffbfffffffL & l) == 0L)
                     break;
                  if (kind > 29)
                     kind = 29;
                  jjCheckNAdd(51);
                  break;
               case 53:
                  if ((0xffffffffefffffffL & l) != 0L)
                     jjCheckNAddStates(32, 34);
                  break;
               case 54:
                  if (curChar == 92)
                     jjstateSet[jjnewStateCnt++] = 55;
                  break;
               case 55:
                  jjCheckNAddStates(32, 34);
                  break;
               case 57:
                  if ((0xdfffffffffffffffL & l) != 0L)
                     jjCheckNAddStates(35, 38);
                  break;
               case 58:
                  if ((0x97ffffff87ffffffL & l) == 0L)
                     break;
                  if (kind > 22)
                     kind = 22;
                  jjCheckNAddStates(6, 10);
                  break;
               case 59:
                  if ((0x97ffffff87ffffffL & l) == 0L)
                     break;
                  if (kind > 22)
                     kind = 22;
                  jjCheckNAddTwoStates(59, 60);
                  break;
               case 60:
                  if (curChar == 92)
                     jjCheckNAddTwoStates(61, 61);
                  break;
               case 61:
                  if (kind > 22)
                     kind = 22;
                  jjCheckNAddTwoStates(59, 60);
                  break;
               case 62:
                  if ((0x97ffffff87ffffffL & l) != 0L)
                     jjCheckNAddStates(39, 41);
                  break;
               case 63:
                  if (curChar == 92)
                     jjCheckNAddTwoStates(64, 64);
                  break;
               case 64:
                  jjCheckNAddStates(39, 41);
                  break;
               case 65:
                  if (curChar == 92)
                     jjCheckNAddStates(42, 44);
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else
      {
         int hiByte = (int)(curChar >> 8);
         int i1 = hiByte >> 6;
         long l1 = 1L << (hiByte & 077);
         int i2 = (curChar & 0xff) >> 6;
         long l2 = 1L << (curChar & 077);
         do
         {
            switch(jjstateSet[--i])
            {
               case 66:
               case 32:
                  if (!jjCanMove_2(hiByte, i1, i2, l1, l2))
                     break;
                  if (kind > 25)
                     kind = 25;
                  jjCheckNAddTwoStates(32, 33);
                  break;
               case 0:
                  if (jjCanMove_0(hiByte, i1, i2, l1, l2))
                  {
                     if (kind > 8)
                        kind = 8;
                  }
                  if (jjCanMove_2(hiByte, i1, i2, l1, l2))
                  {
                     if (kind > 25)
                        kind = 25;
                     jjCheckNAddTwoStates(32, 33);
                  }
                  if (jjCanMove_2(hiByte, i1, i2, l1, l2))
                  {
                     if (kind > 22)
                        kind = 22;
                     jjCheckNAddStates(6, 10);
                  }
                  break;
               case 15:
                  if (jjCanMove_0(hiByte, i1, i2, l1, l2) && kind > 14)
                     kind = 14;
                  break;
               case 17:
               case 19:
                  if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(3, 5);
                  break;
               case 22:
               case 24:
                  if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(11, 13);
                  break;
               case 31:
                  if (!jjCanMove_2(hiByte, i1, i2, l1, l2))
                     break;
                  if (kind > 25)
                     kind = 25;
                  jjCheckNAddTwoStates(32, 33);
                  break;
               case 34:
                  if (!jjCanMove_1(hiByte, i1, i2, l1, l2))
                     break;
                  if (kind > 25)
                     kind = 25;
                  jjCheckNAddTwoStates(32, 33);
                  break;
               case 36:
                  if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                     jjAddStates(0, 2);
                  break;
               case 41:
                  if (jjCanMove_0(hiByte, i1, i2, l1, l2))
                     jjCheckNAddTwoStates(41, 42);
                  break;
               case 42:
                  if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(19, 22);
                  break;
               case 45:
               case 47:
                  if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(29, 31);
                  break;
               case 51:
                  if (!jjCanMove_1(hiByte, i1, i2, l1, l2))
                     break;
                  if (kind > 29)
                     kind = 29;
                  jjstateSet[jjnewStateCnt++] = 51;
                  break;
               case 53:
               case 55:
                  if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(32, 34);
                  break;
               case 57:
                  if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(35, 38);
                  break;
               case 58:
                  if (!jjCanMove_2(hiByte, i1, i2, l1, l2))
                     break;
                  if (kind > 22)
                     kind = 22;
                  jjCheckNAddStates(6, 10);
                  break;
               case 59:
                  if (!jjCanMove_2(hiByte, i1, i2, l1, l2))
                     break;
                  if (kind > 22)
                     kind = 22;
                  jjCheckNAddTwoStates(59, 60);
                  break;
               case 61:
                  if (!jjCanMove_1(hiByte, i1, i2, l1, l2))
                     break;
                  if (kind > 22)
                     kind = 22;
                  jjCheckNAddTwoStates(59, 60);
                  break;
               case 62:
                  if (jjCanMove_2(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(39, 41);
                  break;
               case 64:
                  if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(39, 41);
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      if (kind != 0x7fffffff)
      {
         jjmatchedKind = kind;
         jjmatchedPos = curPos;
         kind = 0x7fffffff;
      }
      ++curPos;
      if ((i = jjnewStateCnt) == (startsAt = 66 - (jjnewStateCnt = startsAt)))
         return curPos;
      try { curChar = input_stream.readChar(); }
      catch(java.io.IOException e) { return curPos; }
   }
}
private int jjMoveStringLiteralDfa0_0()
{
   return jjMoveNfa_0(0, 0);
}
private int jjMoveNfa_0(int startState, int curPos)
{
   int startsAt = 0;
   jjnewStateCnt = 4;
   int i = 1;
   jjstateSet[0] = startState;
   int kind = 0x7fffffff;
   for (;;)
   {
      if (++jjround == 0x7fffffff)
         ReInitRounds();
      if (curChar < 64)
      {
         long l = 1L << curChar;
         do
         {
            switch(jjstateSet[--i])
            {
               case 0:
                  if ((0x3ff000000000000L & l) != 0L)
                  {
                     if (kind > 30)
                        kind = 30;
                     jjCheckNAddTwoStates(1, 2);
                  }
                  else if (curChar == 45)
                     jjCheckNAdd(1);
                  break;
               case 1:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 30)
                     kind = 30;
                  jjCheckNAddTwoStates(1, 2);
                  break;
               case 2:
                  if (curChar == 46)
                     jjCheckNAdd(3);
                  break;
               case 3:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 30)
                     kind = 30;
                  jjCheckNAdd(3);
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else if (curChar < 128)
      {
         long l = 1L << (curChar & 077);
         do
         {
            switch(jjstateSet[--i])
            {
               default : break;
            }
         } while(i != startsAt);
      }
      else
      {
         int hiByte = (int)(curChar >> 8);
         int i1 = hiByte >> 6;
         long l1 = 1L << (hiByte & 077);
         int i2 = (curChar & 0xff) >> 6;
         long l2 = 1L << (curChar & 077);
         do
         {
            switch(jjstateSet[--i])
            {
               default : break;
            }
         } while(i != startsAt);
      }
      if (kind != 0x7fffffff)
      {
         jjmatchedKind = kind;
         jjmatchedPos = curPos;
         kind = 0x7fffffff;
      }
      ++curPos;
      if ((i = jjnewStateCnt) == (startsAt = 4 - (jjnewStateCnt = startsAt)))
         return curPos;
      try { curChar = input_stream.readChar(); }
      catch(java.io.IOException e) { return curPos; }
   }
}
private final int jjStopStringLiteralDfa_1(int pos, long active0)
{
   switch (pos)
   {
      case 0:
         if ((active0 & 0x80000000L) != 0L)
         {
            jjmatchedKind = 35;
            return 6;
         }
         return -1;
      default :
         return -1;
   }
}
private final int jjStartNfa_1(int pos, long active0)
{
   return jjMoveNfa_1(jjStopStringLiteralDfa_1(pos, active0), pos + 1);
}
private int jjMoveStringLiteralDfa0_1()
{
   switch(curChar)
   {
      case 84:
         return jjMoveStringLiteralDfa1_1(0x80000000L);
      case 93:
         return jjStopAtPos(0, 32);
      case 125:
         return jjStopAtPos(0, 33);
      default :
         return jjMoveNfa_1(0, 0);
   }
}
private int jjMoveStringLiteralDfa1_1(long active0)
{
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_1(0, active0);
      return 1;
   }
   switch(curChar)
   {
      case 79:
         if ((active0 & 0x80000000L) != 0L)
            return jjStartNfaWithStates_1(1, 31, 6);
         break;
      default :
         break;
   }
   return jjStartNfa_1(0, active0);
}
private int jjStartNfaWithStates_1(int pos, int kind, int state)
{
   jjmatchedKind = kind;
   jjmatchedPos = pos;
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) { return pos + 1; }
   return jjMoveNfa_1(state, pos + 1);
}
private int jjMoveNfa_1(int startState, int curPos)
{
   int startsAt = 0;
   jjnewStateCnt = 7;
   int i = 1;
   jjstateSet[0] = startState;
   int kind = 0x7fffffff;
   for (;;)
   {
      if (++jjround == 0x7fffffff)
         ReInitRounds();
      if (curChar < 64)
      {
         long l = 1L << curChar;
         do
         {
            switch(jjstateSet[--i])
            {
               case 0:
                  if ((0xfffffffeffffffffL & l) != 0L)
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAdd(6);
                  }
                  if ((0x100002600L & l) != 0L)
                  {
                     if (kind > 8)
                        kind = 8;
                  }
                  else if (curChar == 34)
                     jjCheckNAddTwoStates(2, 4);
                  break;
               case 1:
                  if (curChar == 34)
                     jjCheckNAddTwoStates(2, 4);
                  break;
               case 2:
                  if ((0xfffffffbffffffffL & l) != 0L)
                     jjCheckNAddStates(45, 47);
                  break;
               case 3:
                  if (curChar == 34)
                     jjCheckNAddStates(45, 47);
                  break;
               case 5:
                  if (curChar == 34 && kind > 34)
                     kind = 34;
                  break;
               case 6:
                  if ((0xfffffffeffffffffL & l) == 0L)
                     break;
                  if (kind > 35)
                     kind = 35;
                  jjCheckNAdd(6);
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else if (curChar < 128)
      {
         long l = 1L << (curChar & 077);
         do
         {
            switch(jjstateSet[--i])
            {
               case 0:
               case 6:
                  if ((0xdfffffffdfffffffL & l) == 0L)
                     break;
                  if (kind > 35)
                     kind = 35;
                  jjCheckNAdd(6);
                  break;
               case 2:
                  jjAddStates(45, 47);
                  break;
               case 4:
                  if (curChar == 92)
                     jjstateSet[jjnewStateCnt++] = 3;
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else
      {
         int hiByte = (int)(curChar >> 8);
         int i1 = hiByte >> 6;
         long l1 = 1L << (hiByte & 077);
         int i2 = (curChar & 0xff) >> 6;
         long l2 = 1L << (curChar & 077);
         do
         {
            switch(jjstateSet[--i])
            {
               case 0:
                  if (jjCanMove_0(hiByte, i1, i2, l1, l2))
                  {
                     if (kind > 8)
                        kind = 8;
                  }
                  if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAdd(6);
                  }
                  break;
               case 2:
                  if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                     jjAddStates(45, 47);
                  break;
               case 6:
                  if (!jjCanMove_1(hiByte, i1, i2, l1, l2))
                     break;
                  if (kind > 35)
                     kind = 35;
                  jjCheckNAdd(6);
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      if (kind != 0x7fffffff)
      {
         jjmatchedKind = kind;
         jjmatchedPos = curPos;
         kind = 0x7fffffff;
      }
      ++curPos;
      if ((i = jjnewStateCnt) == (startsAt = 7 - (jjnewStateCnt = startsAt)))
         return curPos;
      try { curChar = input_stream.readChar(); }
      catch(java.io.IOException e) { return curPos; }
   }
}
static final int[] jjnextStates = {
   36, 38, 39, 17, 18, 20, 59, 62, 30, 63, 60, 22, 23, 25, 27, 28, 
   41, 42, 49, 41, 42, 43, 49, 41, 42, 44, 52, 57, 49, 45, 46, 48, 
   53, 54, 56, 41, 42, 57, 49, 62, 30, 63, 61, 64, 34, 2, 4, 5, 
};
private static final boolean jjCanMove_0(int hiByte, int i1, int i2, long l1, long l2)
{
   switch(hiByte)
   {
      case 48:
         return ((jjbitVec0[i2] & l2) != 0L);
      default :
         return false;
   }
}
private static final boolean jjCanMove_1(int hiByte, int i1, int i2, long l1, long l2)
{
   switch(hiByte)
   {
      case 0:
         return ((jjbitVec3[i2] & l2) != 0L);
      default :
         if ((jjbitVec1[i1] & l1) != 0L)
            return true;
         return false;
   }
}
private static final boolean jjCanMove_2(int hiByte, int i1, int i2, long l1, long l2)
{
   switch(hiByte)
   {
      case 0:
         return ((jjbitVec3[i2] & l2) != 0L);
      case 48:
         return ((jjbitVec1[i2] & l2) != 0L);
      default :
         if ((jjbitVec4[i1] & l1) != 0L)
            return true;
         return false;
   }
}

/** Token literal values. */
public static final String[] jjstrLiteralImages = {
"", null, null, null, null, null, null, null, null, null, null, null, "\53", 
"\55", null, "\50", "\51", "\72", "\52", "\136", null, null, null, null, null, null, 
null, "\133", "\173", null, null, "\124\117", "\135", "\175", null, null, };

/** Lexer state names. */
public static final String[] lexStateNames = {
   "Boost",
   "Range",
   "DEFAULT",
};

/** Lex State array. */
public static final int[] jjnewLexState = {
   -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, -1, -1, -1, -1, -1, 
   -1, -1, 1, 1, -1, 2, -1, 2, 2, -1, -1, 
};
static final long[] jjtoToken = {
   0xffffffe01L, 
};
static final long[] jjtoSkip = {
   0x100L, 
};
protected CharStream input_stream;
private final int[] jjrounds = new int[66];
private final int[] jjstateSet = new int[132];
protected char curChar;
/** Constructor. */
public QueryParserTokenManager(CharStream stream){
   input_stream = stream;
}

/** Constructor. */
public QueryParserTokenManager(CharStream stream, int lexState){
   this(stream);
   SwitchTo(lexState);
}

/** Reinitialise parser. */
public void ReInit(CharStream stream)
{
   jjmatchedPos = jjnewStateCnt = 0;
   curLexState = defaultLexState;
   input_stream = stream;
   ReInitRounds();
}
private void ReInitRounds()
{
   int i;
   jjround = 0x80000001;
   for (i = 66; i-- > 0;)
      jjrounds[i] = 0x80000000;
}

/** Reinitialise parser. */
public void ReInit(CharStream stream, int lexState)
{
   ReInit(stream);
   SwitchTo(lexState);
}

/** Switch to specified lex state. */
public void SwitchTo(int lexState)
{
   if (lexState >= 3 || lexState < 0)
      throw new TokenMgrError("Error: Ignoring invalid lexical state : " + lexState + ". State unchanged.", TokenMgrError.INVALID_LEXICAL_STATE);
   else
      curLexState = lexState;
}

protected Token jjFillToken()
{
   final Token t;
   final String curTokenImage;
   final int beginLine;
   final int endLine;
   final int beginColumn;
   final int endColumn;
   String im = jjstrLiteralImages[jjmatchedKind];
   curTokenImage = (im == null) ? input_stream.GetImage() : im;
   beginLine = input_stream.getBeginLine();
   beginColumn = input_stream.getBeginColumn();
   endLine = input_stream.getEndLine();
   endColumn = input_stream.getEndColumn();
   t = Token.newToken(jjmatchedKind, curTokenImage);

   t.beginLine = beginLine;
   t.endLine = endLine;
   t.beginColumn = beginColumn;
   t.endColumn = endColumn;

   return t;
}

int curLexState = 2;
int defaultLexState = 2;
int jjnewStateCnt;
int jjround;
int jjmatchedPos;
int jjmatchedKind;

/** Get the next Token. */
public Token getNextToken() 
{
  Token matchedToken;
  int curPos = 0;

  EOFLoop :
  for (;;)
  {
   try
   {
      curChar = input_stream.BeginToken();
   }
   catch(java.io.IOException e)
   {
      jjmatchedKind = 0;
      matchedToken = jjFillToken();
      return matchedToken;
   }

   switch(curLexState)
   {
     case 0:
       jjmatchedKind = 0x7fffffff;
       jjmatchedPos = 0;
       curPos = jjMoveStringLiteralDfa0_0();
       break;
     case 1:
       jjmatchedKind = 0x7fffffff;
       jjmatchedPos = 0;
       curPos = jjMoveStringLiteralDfa0_1();
       break;
     case 2:
       jjmatchedKind = 0x7fffffff;
       jjmatchedPos = 0;
       curPos = jjMoveStringLiteralDfa0_2();
       break;
   }
     if (jjmatchedKind != 0x7fffffff)
     {
        if (jjmatchedPos + 1 < curPos)
           input_stream.backup(curPos - jjmatchedPos - 1);
        if ((jjtoToken[jjmatchedKind >> 6] & (1L << (jjmatchedKind & 077))) != 0L)
        {
           matchedToken = jjFillToken();
       if (jjnewLexState[jjmatchedKind] != -1)
         curLexState = jjnewLexState[jjmatchedKind];
           return matchedToken;
        }
        else
        {
         if (jjnewLexState[jjmatchedKind] != -1)
           curLexState = jjnewLexState[jjmatchedKind];
           continue EOFLoop;
        }
     }
     int error_line = input_stream.getEndLine();
     int error_column = input_stream.getEndColumn();
     String error_after = null;
     boolean EOFSeen = false;
     try { input_stream.readChar(); input_stream.backup(1); }
     catch (java.io.IOException e1) {
        EOFSeen = true;
        error_after = curPos <= 1 ? "" : input_stream.GetImage();
        if (curChar == '\n' || curChar == '\r') {
           error_line++;
           error_column = 0;
        }
        else
           error_column++;
     }
     if (!EOFSeen) {
        input_stream.backup(1);
        error_after = curPos <= 1 ? "" : input_stream.GetImage();
     }
     throw new TokenMgrError(EOFSeen, curLexState, error_line, error_column, error_after, curChar, TokenMgrError.LEXICAL_ERROR);
  }
}

private void jjCheckNAdd(int state)
{
   if (jjrounds[state] != jjround)
   {
      jjstateSet[jjnewStateCnt++] = state;
      jjrounds[state] = jjround;
   }
}
private void jjAddStates(int start, int end)
{
   do {
      jjstateSet[jjnewStateCnt++] = jjnextStates[start];
   } while (start++ != end);
}
private void jjCheckNAddTwoStates(int state1, int state2)
{
   jjCheckNAdd(state1);
   jjCheckNAdd(state2);
}

private void jjCheckNAddStates(int start, int end)
{
   do {
      jjCheckNAdd(jjnextStates[start]);
   } while (start++ != end);
}

}
diff --git a/solr/core/src/java/org/apache/solr/parser/SolrQueryParserBase.java b/solr/core/src/java/org/apache/solr/parser/SolrQueryParserBase.java
new file mode 100644
index 00000000000..92b4e9ba5cf
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/parser/SolrQueryParserBase.java
@@ -0,0 +1,1033 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.parser;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CachingTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.AutomatonQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.ToStringUtils;
import org.apache.lucene.util.Version;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.BasicAutomata;
import org.apache.lucene.util.automaton.BasicOperations;
import org.apache.lucene.util.automaton.SpecialOperations;
import org.apache.solr.analysis.ReversedWildcardFilterFactory;
import org.apache.solr.analysis.TokenizerChain;
import org.apache.solr.common.SolrException;
import org.apache.solr.parser.QueryParser.Operator;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.TextField;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** This class is overridden by QueryParser in QueryParser.jj
 * and acts to separate the majority of the Java code from the .jj grammar file. 
 */
public abstract class SolrQueryParserBase {


  static final int CONJ_NONE   = 0;
  static final int CONJ_AND    = 1;
  static final int CONJ_OR     = 2;

  static final int MOD_NONE    = 0;
  static final int MOD_NOT     = 10;
  static final int MOD_REQ     = 11;

  // make it possible to call setDefaultOperator() without accessing
  // the nested class:
  /** Alternative form of QueryParser.Operator.AND */
  public static final Operator AND_OPERATOR = Operator.AND;
  /** Alternative form of QueryParser.Operator.OR */
  public static final Operator OR_OPERATOR = Operator.OR;

  /** The default operator that parser uses to combine query terms */
  Operator operator = OR_OPERATOR;

  MultiTermQuery.RewriteMethod multiTermRewriteMethod = MultiTermQuery.CONSTANT_SCORE_AUTO_REWRITE_DEFAULT;
  boolean allowLeadingWildcard = true;
  boolean enablePositionIncrements = true;

  String defaultField;
  int phraseSlop = 0;     // default slop for phrase queries
  float fuzzyMinSim = FuzzyQuery.defaultMinSimilarity;
  int fuzzyPrefixLength = FuzzyQuery.defaultPrefixLength;

  boolean autoGeneratePhraseQueries = false;


  protected IndexSchema schema;
  protected QParser parser;
  protected Analyzer analyzer;

  // implementation detail - caching ReversedWildcardFilterFactory based on type
  private Map<FieldType, ReversedWildcardFilterFactory> leadingWildcards;

  /**
   * Identifies the list of all known "magic fields" that trigger
   * special parsing behavior
   */
  public static enum MagicFieldName {
    VAL("_val_", "func"), QUERY("_query_", null);

    public final String field;
    public final String subParser;
    MagicFieldName(final String field, final String subParser) {
      this.field = field;
      this.subParser = subParser;
    }
    public String toString() {
      return field;
    }
    private final static Map<String,MagicFieldName> lookup
        = new HashMap<String,MagicFieldName>();
    static {
      for(MagicFieldName s : EnumSet.allOf(MagicFieldName.class))
        lookup.put(s.toString(), s);
    }
    public static MagicFieldName get(final String field) {
      return lookup.get(field);
    }
  }


  // So the generated QueryParser(CharStream) won't error out
  protected SolrQueryParserBase() {
  }
  // the generated parser will create these in QueryParser
  public abstract void ReInit(CharStream stream);
  public abstract Query TopLevelQuery(String field) throws ParseException, SyntaxError;


  public void init(Version matchVersion, String defaultField, QParser parser) {
    this.schema = parser.getReq().getSchema();
    this.parser = parser;
    this.defaultField = defaultField;
    this.analyzer = schema.getQueryAnalyzer();
  }

    /** Parses a query string, returning a {@link org.apache.lucene.search.Query}.
    *  @param query  the query string to be parsed.
    */
  public Query parse(String query) throws SyntaxError {
    ReInit(new FastCharStream(new StringReader(query)));
    try {
      // TopLevelQuery is a Query followed by the end-of-input (EOF)
      Query res = TopLevelQuery(null);  // pass null so we can tell later if an explicit field was provided or not
      return res!=null ? res : newBooleanQuery(false);
    }
    catch (ParseException tme) {
      throw new SyntaxError("Cannot parse '" +query+ "': " + tme.getMessage(), tme);
    }
    catch (TokenMgrError tme) {
      throw new SyntaxError("Cannot parse '" +query+ "': " + tme.getMessage(), tme);
    }
    catch (BooleanQuery.TooManyClauses tmc) {
      throw new SyntaxError("Cannot parse '" +query+ "': too many boolean clauses", tmc);
    }
  }


  /**
   * @return Returns the default field.
   */
  public String getDefaultField() {
    return this.defaultField;
  }

  /** Handles the default field if null is passed */
  public String getField(String fieldName) {
    return fieldName != null ? fieldName : this.defaultField;
  }

  /**
   * @see #setAutoGeneratePhraseQueries(boolean)
   */
  public final boolean getAutoGeneratePhraseQueries() {
    return autoGeneratePhraseQueries;
  }

  /**
   * Set to true if phrase queries will be automatically generated
   * when the analyzer returns more than one term from whitespace
   * delimited text.
   * NOTE: this behavior may not be suitable for all languages.
   * <p>
   * Set to false if phrase queries should only be generated when
   * surrounded by double quotes.
   */
  public final void setAutoGeneratePhraseQueries(boolean value) {
    this.autoGeneratePhraseQueries = value;
  }

   /**
   * Get the minimal similarity for fuzzy queries.
   */
  public float getFuzzyMinSim() {
      return fuzzyMinSim;
  }

  /**
   * Set the minimum similarity for fuzzy queries.
   * Default is 2f.
   */
  public void setFuzzyMinSim(float fuzzyMinSim) {
      this.fuzzyMinSim = fuzzyMinSim;
  }

   /**
   * Get the prefix length for fuzzy queries.
   * @return Returns the fuzzyPrefixLength.
   */
  public int getFuzzyPrefixLength() {
    return fuzzyPrefixLength;
  }

  /**
   * Set the prefix length for fuzzy queries. Default is 0.
   * @param fuzzyPrefixLength The fuzzyPrefixLength to set.
   */
  public void setFuzzyPrefixLength(int fuzzyPrefixLength) {
    this.fuzzyPrefixLength = fuzzyPrefixLength;
  }

  /**
   * Sets the default slop for phrases.  If zero, then exact phrase matches
   * are required.  Default value is zero.
   */
  public void setPhraseSlop(int phraseSlop) {
    this.phraseSlop = phraseSlop;
  }

  /**
   * Gets the default slop for phrases.
   */
  public int getPhraseSlop() {
    return phraseSlop;
  }


  /**
   * Set to <code>true</code> to allow leading wildcard characters.
   * <p>
   * When set, <code>*</code> or <code>?</code> are allowed as
   * the first character of a PrefixQuery and WildcardQuery.
   * Note that this can produce very slow
   * queries on big indexes.
   * <p>
   * Default: false.
   */
  public void setAllowLeadingWildcard(boolean allowLeadingWildcard) {
    this.allowLeadingWildcard = allowLeadingWildcard;
  }

  /**
   * @see #setAllowLeadingWildcard(boolean)
   */
  public boolean getAllowLeadingWildcard() {
    return allowLeadingWildcard;
  }

  /**
   * Set to <code>true</code> to enable position increments in result query.
   * <p>
   * When set, result phrase and multi-phrase queries will
   * be aware of position increments.
   * Useful when e.g. a StopFilter increases the position increment of
   * the token that follows an omitted token.
   * <p>
   * Default: true.
   */
  public void setEnablePositionIncrements(boolean enable) {
    this.enablePositionIncrements = enable;
  }

  /**
   * @see #setEnablePositionIncrements(boolean)
   */
  public boolean getEnablePositionIncrements() {
    return enablePositionIncrements;
  }

  /**
   * Sets the boolean operator of the QueryParser.
   * In default mode (<code>OR_OPERATOR</code>) terms without any modifiers
   * are considered optional: for example <code>capital of Hungary</code> is equal to
   * <code>capital OR of OR Hungary</code>.<br/>
   * In <code>AND_OPERATOR</code> mode terms are considered to be in conjunction: the
   * above mentioned query is parsed as <code>capital AND of AND Hungary</code>
   */
  public void setDefaultOperator(Operator op) {
    this.operator = op;
  }


  /**
   * Gets implicit operator setting, which will be either AND_OPERATOR
   * or OR_OPERATOR.
   */
  public Operator getDefaultOperator() {
    return operator;
  }


  /**
   * By default QueryParser uses {@link org.apache.lucene.search.MultiTermQuery#CONSTANT_SCORE_AUTO_REWRITE_DEFAULT}
   * when creating a PrefixQuery, WildcardQuery or RangeQuery. This implementation is generally preferable because it
   * a) Runs faster b) Does not have the scarcity of terms unduly influence score
   * c) avoids any "TooManyBooleanClauses" exception.
   * However, if your application really needs to use the
   * old-fashioned BooleanQuery expansion rewriting and the above
   * points are not relevant then use this to change
   * the rewrite method.
   */
  public void setMultiTermRewriteMethod(MultiTermQuery.RewriteMethod method) {
    multiTermRewriteMethod = method;
  }


  /**
   * @see #setMultiTermRewriteMethod
   */
  public MultiTermQuery.RewriteMethod getMultiTermRewriteMethod() {
    return multiTermRewriteMethod;
  }


  protected void addClause(List<BooleanClause> clauses, int conj, int mods, Query q) {
    boolean required, prohibited;

    // If this term is introduced by AND, make the preceding term required,
    // unless it's already prohibited
    if (clauses.size() > 0 && conj == CONJ_AND) {
      BooleanClause c = clauses.get(clauses.size()-1);
      if (!c.isProhibited())
        c.setOccur(BooleanClause.Occur.MUST);
    }

    if (clauses.size() > 0 && operator == AND_OPERATOR && conj == CONJ_OR) {
      // If this term is introduced by OR, make the preceding term optional,
      // unless it's prohibited (that means we leave -a OR b but +a OR b-->a OR b)
      // notice if the input is a OR b, first term is parsed as required; without
      // this modification a OR b would parsed as +a OR b
      BooleanClause c = clauses.get(clauses.size()-1);
      if (!c.isProhibited())
        c.setOccur(BooleanClause.Occur.SHOULD);
    }

    // We might have been passed a null query; the term might have been
    // filtered away by the analyzer.
    if (q == null)
      return;

    if (operator == OR_OPERATOR) {
      // We set REQUIRED if we're introduced by AND or +; PROHIBITED if
      // introduced by NOT or -; make sure not to set both.
      prohibited = (mods == MOD_NOT);
      required = (mods == MOD_REQ);
      if (conj == CONJ_AND && !prohibited) {
        required = true;
      }
    } else {
      // We set PROHIBITED if we're introduced by NOT or -; We set REQUIRED
      // if not PROHIBITED and not introduced by OR
      prohibited = (mods == MOD_NOT);
      required   = (!prohibited && conj != CONJ_OR);
    }
    if (required && !prohibited)
      clauses.add(newBooleanClause(q, BooleanClause.Occur.MUST));
    else if (!required && !prohibited)
      clauses.add(newBooleanClause(q, BooleanClause.Occur.SHOULD));
    else if (!required && prohibited)
      clauses.add(newBooleanClause(q, BooleanClause.Occur.MUST_NOT));
    else
      throw new RuntimeException("Clause cannot be both required and prohibited");
  }



  protected Query newFieldQuery(Analyzer analyzer, String field, String queryText, boolean quoted)  throws SyntaxError {
    // Use the analyzer to get all the tokens, and then build a TermQuery,
    // PhraseQuery, or nothing based on the term count

    TokenStream source;
    try {
      source = analyzer.tokenStream(field, new StringReader(queryText));
      source.reset();
    } catch (IOException e) {
      throw new SyntaxError("Unable to initialize TokenStream to analyze query text", e);
    }
    CachingTokenFilter buffer = new CachingTokenFilter(source);
    TermToBytesRefAttribute termAtt = null;
    PositionIncrementAttribute posIncrAtt = null;
    int numTokens = 0;

    buffer.reset();

    if (buffer.hasAttribute(TermToBytesRefAttribute.class)) {
      termAtt = buffer.getAttribute(TermToBytesRefAttribute.class);
    }
    if (buffer.hasAttribute(PositionIncrementAttribute.class)) {
      posIncrAtt = buffer.getAttribute(PositionIncrementAttribute.class);
    }

    int positionCount = 0;
    boolean severalTokensAtSamePosition = false;

    boolean hasMoreTokens = false;
    if (termAtt != null) {
      try {
        hasMoreTokens = buffer.incrementToken();
        while (hasMoreTokens) {
          numTokens++;
          int positionIncrement = (posIncrAtt != null) ? posIncrAtt.getPositionIncrement() : 1;
          if (positionIncrement != 0) {
            positionCount += positionIncrement;
          } else {
            severalTokensAtSamePosition = true;
          }
          hasMoreTokens = buffer.incrementToken();
        }
      } catch (IOException e) {
        // ignore
      }
    }
    try {
      // rewind the buffer stream
      buffer.reset();

      // close original stream - all tokens buffered
      source.close();
    }
    catch (IOException e) {
      throw new SyntaxError("Cannot close TokenStream analyzing query text", e);
    }

    BytesRef bytes = termAtt == null ? null : termAtt.getBytesRef();

    if (numTokens == 0)
      return null;
    else if (numTokens == 1) {
      try {
        boolean hasNext = buffer.incrementToken();
        assert hasNext == true;
        termAtt.fillBytesRef();
      } catch (IOException e) {
        // safe to ignore, because we know the number of tokens
      }
      return newTermQuery(new Term(field, BytesRef.deepCopyOf(bytes)));
    } else {
      if (severalTokensAtSamePosition || (!quoted && !autoGeneratePhraseQueries)) {
        if (positionCount == 1 || (!quoted && !autoGeneratePhraseQueries)) {
          // no phrase query:
          BooleanQuery q = newBooleanQuery(positionCount == 1);

          BooleanClause.Occur occur = positionCount > 1 && operator == AND_OPERATOR ?
            BooleanClause.Occur.MUST : BooleanClause.Occur.SHOULD;

          for (int i = 0; i < numTokens; i++) {
            try {
              boolean hasNext = buffer.incrementToken();
              assert hasNext == true;
              termAtt.fillBytesRef();
            } catch (IOException e) {
              // safe to ignore, because we know the number of tokens
            }
            Query currentQuery = newTermQuery(
                new Term(field, BytesRef.deepCopyOf(bytes)));
            q.add(currentQuery, occur);
          }
          return q;
        }
        else {
          // phrase query:
          MultiPhraseQuery mpq = newMultiPhraseQuery();
          mpq.setSlop(phraseSlop);
          List<Term> multiTerms = new ArrayList<Term>();
          int position = -1;
          for (int i = 0; i < numTokens; i++) {
            int positionIncrement = 1;
            try {
              boolean hasNext = buffer.incrementToken();
              assert hasNext == true;
              termAtt.fillBytesRef();
              if (posIncrAtt != null) {
                positionIncrement = posIncrAtt.getPositionIncrement();
              }
            } catch (IOException e) {
              // safe to ignore, because we know the number of tokens
            }

            if (positionIncrement > 0 && multiTerms.size() > 0) {
              if (enablePositionIncrements) {
                mpq.add(multiTerms.toArray(new Term[0]),position);
              } else {
                mpq.add(multiTerms.toArray(new Term[0]));
              }
              multiTerms.clear();
            }
            position += positionIncrement;
            multiTerms.add(new Term(field, BytesRef.deepCopyOf(bytes)));
          }
          if (enablePositionIncrements) {
            mpq.add(multiTerms.toArray(new Term[0]),position);
          } else {
            mpq.add(multiTerms.toArray(new Term[0]));
          }
          return mpq;
        }
      }
      else {
        PhraseQuery pq = newPhraseQuery();
        pq.setSlop(phraseSlop);
        int position = -1;

        for (int i = 0; i < numTokens; i++) {
          int positionIncrement = 1;

          try {
            boolean hasNext = buffer.incrementToken();
            assert hasNext == true;
            termAtt.fillBytesRef();
            if (posIncrAtt != null) {
              positionIncrement = posIncrAtt.getPositionIncrement();
            }
          } catch (IOException e) {
            // safe to ignore, because we know the number of tokens
          }

          if (enablePositionIncrements) {
            position += positionIncrement;
            pq.add(new Term(field, BytesRef.deepCopyOf(bytes)),position);
          } else {
            pq.add(new Term(field, BytesRef.deepCopyOf(bytes)));
          }
        }
        return pq;
      }
    }
  }



  /**
   * Base implementation delegates to {@link #getFieldQuery(String,String,boolean)}.
   * This method may be overridden, for example, to return
   * a SpanNearQuery instead of a PhraseQuery.
   *
   */
  protected Query getFieldQuery(String field, String queryText, int slop)
        throws SyntaxError {
    Query query = getFieldQuery(field, queryText, true);

    if (query instanceof PhraseQuery) {
      ((PhraseQuery) query).setSlop(slop);
    }
    if (query instanceof MultiPhraseQuery) {
      ((MultiPhraseQuery) query).setSlop(slop);
    }

    return query;
  }


 /**
  * Builds a new BooleanQuery instance
  * @param disableCoord disable coord
  * @return new BooleanQuery instance
  */
  protected BooleanQuery newBooleanQuery(boolean disableCoord) {
    return new BooleanQuery(disableCoord);
  }

 /**
  * Builds a new BooleanClause instance
  * @param q sub query
  * @param occur how this clause should occur when matching documents
  * @return new BooleanClause instance
  */
  protected BooleanClause newBooleanClause(Query q, BooleanClause.Occur occur) {
    return new BooleanClause(q, occur);
  }

  /**
   * Builds a new TermQuery instance
   * @param term term
   * @return new TermQuery instance
   */
  protected Query newTermQuery(Term term){
    return new TermQuery(term);
  }

  /**
   * Builds a new PhraseQuery instance
   * @return new PhraseQuery instance
   */
  protected PhraseQuery newPhraseQuery(){
    return new PhraseQuery();
  }

  /**
   * Builds a new MultiPhraseQuery instance
   * @return new MultiPhraseQuery instance
   */
  protected MultiPhraseQuery newMultiPhraseQuery(){
    return new MultiPhraseQuery();
  }

  /**
   * Builds a new PrefixQuery instance
   * @param prefix Prefix term
   * @return new PrefixQuery instance
   */
  protected Query newPrefixQuery(Term prefix){
    PrefixQuery query = new PrefixQuery(prefix);
    query.setRewriteMethod(multiTermRewriteMethod);
    return query;
  }

  /**
   * Builds a new RegexpQuery instance
   * @param regexp Regexp term
   * @return new RegexpQuery instance
   */
  protected Query newRegexpQuery(Term regexp) {
    RegexpQuery query = new RegexpQuery(regexp);
    query.setRewriteMethod(multiTermRewriteMethod);
    return query;
  }

  /**
   * Builds a new FuzzyQuery instance
   * @param term Term
   * @param minimumSimilarity minimum similarity
   * @param prefixLength prefix length
   * @return new FuzzyQuery Instance
   */
  protected Query newFuzzyQuery(Term term, float minimumSimilarity, int prefixLength) {
    // FuzzyQuery doesn't yet allow constant score rewrite
    String text = term.text();
    int numEdits = FuzzyQuery.floatToEdits(minimumSimilarity, 
        text.codePointCount(0, text.length()));
    return new FuzzyQuery(term,numEdits,prefixLength);
  }

  /**
   * Builds a new MatchAllDocsQuery instance
   * @return new MatchAllDocsQuery instance
   */
  protected Query newMatchAllDocsQuery() {
    return new MatchAllDocsQuery();
  }

  /**
   * Builds a new WildcardQuery instance
   * @param t wildcard term
   * @return new WildcardQuery instance
   */
  protected Query newWildcardQuery(Term t) {
    WildcardQuery query = new WildcardQuery(t);
    query.setRewriteMethod(multiTermRewriteMethod);
    return query;
  }

  /**
   * Factory method for generating query, given a set of clauses.
   * By default creates a boolean query composed of clauses passed in.
   *
   * Can be overridden by extending classes, to modify query being
   * returned.
   *
   * @param clauses List that contains {@link org.apache.lucene.search.BooleanClause} instances
   *    to join.
   *
   * @return Resulting {@link org.apache.lucene.search.Query} object.
   */
  protected Query getBooleanQuery(List<BooleanClause> clauses) throws SyntaxError {
    return getBooleanQuery(clauses, false);
  }

  /**
   * Factory method for generating query, given a set of clauses.
   * By default creates a boolean query composed of clauses passed in.
   *
   * Can be overridden by extending classes, to modify query being
   * returned.
   *
   * @param clauses List that contains {@link org.apache.lucene.search.BooleanClause} instances
   *    to join.
   * @param disableCoord true if coord scoring should be disabled.
   *
   * @return Resulting {@link org.apache.lucene.search.Query} object.
   */
  protected Query getBooleanQuery(List<BooleanClause> clauses, boolean disableCoord)
    throws SyntaxError
  {
    if (clauses.size()==0) {
      return null; // all clause words were filtered away by the analyzer.
    }
    BooleanQuery query = newBooleanQuery(disableCoord);
    for(final BooleanClause clause: clauses) {
      query.add(clause);
    }
    return query;
  }


   // called from parser
  Query handleBareTokenQuery(String qfield, Token term, Token fuzzySlop, boolean prefix, boolean wildcard, boolean fuzzy, boolean regexp) throws SyntaxError {
    Query q;

    String termImage=discardEscapeChar(term.image);
    if (wildcard) {
      q = getWildcardQuery(qfield, term.image);
    } else if (prefix) {
      q = getPrefixQuery(qfield,
          discardEscapeChar(term.image.substring
              (0, term.image.length()-1)));
    } else if (regexp) {
      q = getRegexpQuery(qfield, term.image.substring(1, term.image.length()-1));
    } else if (fuzzy) {
      float fms = fuzzyMinSim;
      try {
        fms = Float.valueOf(fuzzySlop.image.substring(1)).floatValue();
      } catch (Exception ignored) { }
      if(fms < 0.0f){
        throw new SyntaxError("Minimum similarity for a FuzzyQuery has to be between 0.0f and 1.0f !");
      } else if (fms >= 1.0f && fms != (int) fms) {
        throw new SyntaxError("Fractional edit distances are not allowed!");
      }
      q = getFuzzyQuery(qfield, termImage, fms);
    } else {
      q = getFieldQuery(qfield, termImage, false);
    }
    return q;
  }

  // called from parser
  Query handleQuotedTerm(String qfield, Token term, Token fuzzySlop) throws SyntaxError {
    int s = phraseSlop;  // default
    if (fuzzySlop != null) {
      try {
        s = Float.valueOf(fuzzySlop.image.substring(1)).intValue();
      }
      catch (Exception ignored) { }
    }
    return getFieldQuery(qfield, discardEscapeChar(term.image.substring(1, term.image.length()-1)), s);
  }

  // called from parser
  Query handleBoost(Query q, Token boost) {
    if (boost != null) {
      float boostVal = Float.parseFloat(boost.image);
      // avoid boosting null queries, such as those caused by stop words
      if (q != null) {
        q.setBoost(boostVal);
      }
    }
    return q;
  }



  /**
   * Returns a String where the escape char has been
   * removed, or kept only once if there was a double escape.
   *
   * Supports escaped unicode characters, e. g. translates
   * <code>\\u0041</code> to <code>A</code>.
   *
   */
  String discardEscapeChar(String input) throws SyntaxError {
    // Create char array to hold unescaped char sequence
    char[] output = new char[input.length()];

    // The length of the output can be less than the input
    // due to discarded escape chars. This variable holds
    // the actual length of the output
    int length = 0;

    // We remember whether the last processed character was
    // an escape character
    boolean lastCharWasEscapeChar = false;

    // The multiplier the current unicode digit must be multiplied with.
    // E. g. the first digit must be multiplied with 16^3, the second with 16^2...
    int codePointMultiplier = 0;

    // Used to calculate the codepoint of the escaped unicode character
    int codePoint = 0;

    for (int i = 0; i < input.length(); i++) {
      char curChar = input.charAt(i);
      if (codePointMultiplier > 0) {
        codePoint += hexToInt(curChar) * codePointMultiplier;
        codePointMultiplier >>>= 4;
        if (codePointMultiplier == 0) {
          output[length++] = (char)codePoint;
          codePoint = 0;
        }
      } else if (lastCharWasEscapeChar) {
        if (curChar == 'u') {
          // found an escaped unicode character
          codePointMultiplier = 16 * 16 * 16;
        } else {
          // this character was escaped
          output[length] = curChar;
          length++;
        }
        lastCharWasEscapeChar = false;
      } else {
        if (curChar == '\\') {
          lastCharWasEscapeChar = true;
        } else {
          output[length] = curChar;
          length++;
        }
      }
    }

    if (codePointMultiplier > 0) {
      throw new SyntaxError("Truncated unicode escape sequence.");
    }

    if (lastCharWasEscapeChar) {
      throw new SyntaxError("Term can not end with escape character.");
    }

    return new String(output, 0, length);
  }

  /** Returns the numeric value of the hexadecimal character */
  static final int hexToInt(char c) throws SyntaxError {
    if ('0' <= c && c <= '9') {
      return c - '0';
    } else if ('a' <= c && c <= 'f'){
      return c - 'a' + 10;
    } else if ('A' <= c && c <= 'F') {
      return c - 'A' + 10;
    } else {
      throw new SyntaxError("Non-hex character in Unicode escape sequence: " + c);
    }
  }

  /**
   * Returns a String where those characters that QueryParser
   * expects to be escaped are escaped by a preceding <code>\</code>.
   */
  public static String escape(String s) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      // These characters are part of the query syntax and must be escaped
      if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':'
        || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~'
        || c == '*' || c == '?' || c == '|' || c == '&' || c == '/') {
        sb.append('\\');
      }
      sb.append(c);
    }
    return sb.toString();
  }


  protected ReversedWildcardFilterFactory getReversedWildcardFilterFactory(FieldType fieldType) {
    if (leadingWildcards == null) leadingWildcards = new HashMap<FieldType, ReversedWildcardFilterFactory>();
    ReversedWildcardFilterFactory fac = leadingWildcards.get(fieldType);
    if (fac != null || leadingWildcards.containsKey(fac)) {
      return fac;
    }

    Analyzer a = fieldType.getAnalyzer();
    if (a instanceof TokenizerChain) {
      // examine the indexing analysis chain if it supports leading wildcards
      TokenizerChain tc = (TokenizerChain)a;
      TokenFilterFactory[] factories = tc.getTokenFilterFactories();
      for (TokenFilterFactory factory : factories) {
        if (factory instanceof ReversedWildcardFilterFactory) {
          fac = (ReversedWildcardFilterFactory)factory;
          break;
        }
      }
    }

    leadingWildcards.put(fieldType, fac);
    return fac;
  }


  private void checkNullField(String field) throws SolrException {
    if (field == null && defaultField == null) {
      throw new SolrException
          (SolrException.ErrorCode.BAD_REQUEST,
              "no field name specified in query and no default specified via 'df' param");
    }
  }

  protected String analyzeIfMultitermTermText(String field, String part, FieldType fieldType) {
    if (part == null) return part;

    SchemaField sf = schema.getFieldOrNull((field));
    if (sf == null || ! (fieldType instanceof TextField)) return part;
    String out = TextField.analyzeMultiTerm(field, part, ((TextField)fieldType).getMultiTermAnalyzer()).utf8ToString();
    return out;
  }


  // called from parser
  protected Query getFieldQuery(String field, String queryText, boolean quoted) throws SyntaxError {
    checkNullField(field);
    // intercept magic field name of "_" to use as a hook for our
    // own functions.
    if (field.charAt(0) == '_' && parser != null) {
      MagicFieldName magic = MagicFieldName.get(field);
      if (null != magic) {
        QParser nested = parser.subQuery(queryText, magic.subParser);
        return nested.getQuery();
      }
    }
    SchemaField sf = schema.getFieldOrNull(field);
    if (sf != null) {
      FieldType ft = sf.getType();
      // delegate to type for everything except tokenized fields
      if (ft.isTokenized()) {
        return newFieldQuery(analyzer, field, queryText, quoted || (ft instanceof TextField && ((TextField)ft).getAutoGeneratePhraseQueries()));
      } else {
        return sf.getType().getFieldQuery(parser, sf, queryText);
      }
    }

    // default to a normal field query
    return newFieldQuery(analyzer, field, queryText, quoted);
  }


  // called from parser
  protected Query getRangeQuery(String field, String part1, String part2, boolean startInclusive, boolean endInclusive) throws SyntaxError {
    checkNullField(field);
    SchemaField sf = schema.getField(field);
    return sf.getType().getRangeQuery(parser, sf, part1, part2, startInclusive, endInclusive);
  }

  // called from parser
  protected Query getPrefixQuery(String field, String termStr) throws SyntaxError {
    checkNullField(field);

    termStr = analyzeIfMultitermTermText(field, termStr, schema.getFieldType(field));

    // Solr has always used constant scoring for prefix queries.  This should return constant scoring by default.
    return newPrefixQuery(new Term(field, termStr));
  }

  // called from parser
  protected Query getWildcardQuery(String field, String termStr) throws SyntaxError {
    checkNullField(field);
    // *:* -> MatchAllDocsQuery
    if ("*".equals(field) && "*".equals(termStr)) {
      return newMatchAllDocsQuery();
    }
    FieldType fieldType = schema.getFieldType(field);
    termStr = analyzeIfMultitermTermText(field, termStr, fieldType);
    // can we use reversed wildcards in this field?
    ReversedWildcardFilterFactory factory = getReversedWildcardFilterFactory(fieldType);
    if (factory != null) {
      Term term = new Term(field, termStr);
      // fsa representing the query
      Automaton automaton = WildcardQuery.toAutomaton(term);
      // TODO: we should likely use the automaton to calculate shouldReverse, too.
      if (factory.shouldReverse(termStr)) {
        automaton = BasicOperations.concatenate(automaton, BasicAutomata.makeChar(factory.getMarkerChar()));
        SpecialOperations.reverse(automaton);
      } else {
        // reverse wildcardfilter is active: remove false positives
        // fsa representing false positives (markerChar*)
        Automaton falsePositives = BasicOperations.concatenate(
            BasicAutomata.makeChar(factory.getMarkerChar()),
            BasicAutomata.makeAnyString());
        // subtract these away
        automaton = BasicOperations.minus(automaton, falsePositives);
      }
      return new AutomatonQuery(term, automaton) {
        // override toString so its completely transparent
        @Override
        public String toString(String field) {
          StringBuilder buffer = new StringBuilder();
          if (!getField().equals(field)) {
            buffer.append(getField());
            buffer.append(":");
          }
          buffer.append(term.text());
          buffer.append(ToStringUtils.boost(getBoost()));
          return buffer.toString();
        }
      };
    }

    // Solr has always used constant scoring for wildcard queries.  This should return constant scoring by default.
    return newWildcardQuery(new Term(field, termStr));
  }

  // called from parser
  protected Query getRegexpQuery(String field, String termStr) throws SyntaxError
  {
    termStr = analyzeIfMultitermTermText(field, termStr, schema.getFieldType(field));
    return newRegexpQuery(new Term(field, termStr));
  }

  // called from parser
  protected Query getFuzzyQuery(String field, String termStr, float minSimilarity) throws SyntaxError {
    termStr = analyzeIfMultitermTermText(field, termStr, schema.getFieldType(field));
    Term t = new Term(field, termStr);
    return newFuzzyQuery(t, minSimilarity, getFuzzyPrefixLength());
  }

  // called from parser
  protected Query getLocalParams(String qfield, String lparams) throws SyntaxError {
    QParser nested = parser.subQuery(lparams, null);
    return nested.getQuery();
  }

}
diff --git a/solr/core/src/java/org/apache/solr/parser/Token.java b/solr/core/src/java/org/apache/solr/parser/Token.java
new file mode 100644
index 00000000000..0d596035928
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/parser/Token.java
@@ -0,0 +1,131 @@
/* Generated By:JavaCC: Do not edit this line. Token.java Version 5.0 */
/* JavaCCOptions:TOKEN_EXTENDS=,KEEP_LINE_COL=null,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.apache.solr.parser;

/**
 * Describes the input token stream.
 */

public class Token implements java.io.Serializable {

  /**
   * The version identifier for this Serializable class.
   * Increment only if the <i>serialized</i> form of the
   * class changes.
   */
  private static final long serialVersionUID = 1L;

  /**
   * An integer that describes the kind of this token.  This numbering
   * system is determined by JavaCCParser, and a table of these numbers is
   * stored in the file ...Constants.java.
   */
  public int kind;

  /** The line number of the first character of this Token. */
  public int beginLine;
  /** The column number of the first character of this Token. */
  public int beginColumn;
  /** The line number of the last character of this Token. */
  public int endLine;
  /** The column number of the last character of this Token. */
  public int endColumn;

  /**
   * The string image of the token.
   */
  public String image;

  /**
   * A reference to the next regular (non-special) token from the input
   * stream.  If this is the last token from the input stream, or if the
   * token manager has not read tokens beyond this one, this field is
   * set to null.  This is true only if this token is also a regular
   * token.  Otherwise, see below for a description of the contents of
   * this field.
   */
  public Token next;

  /**
   * This field is used to access special tokens that occur prior to this
   * token, but after the immediately preceding regular (non-special) token.
   * If there are no such special tokens, this field is set to null.
   * When there are more than one such special token, this field refers
   * to the last of these special tokens, which in turn refers to the next
   * previous special token through its specialToken field, and so on
   * until the first special token (whose specialToken field is null).
   * The next fields of special tokens refer to other special tokens that
   * immediately follow it (without an intervening regular token).  If there
   * is no such token, this field is null.
   */
  public Token specialToken;

  /**
   * An optional attribute value of the Token.
   * Tokens which are not used as syntactic sugar will often contain
   * meaningful values that will be used later on by the compiler or
   * interpreter. This attribute value is often different from the image.
   * Any subclass of Token that actually wants to return a non-null value can
   * override this method as appropriate.
   */
  public Object getValue() {
    return null;
  }

  /**
   * No-argument constructor
   */
  public Token() {}

  /**
   * Constructs a new token for the specified Image.
   */
  public Token(int kind)
  {
    this(kind, null);
  }

  /**
   * Constructs a new token for the specified Image and Kind.
   */
  public Token(int kind, String image)
  {
    this.kind = kind;
    this.image = image;
  }

  /**
   * Returns the image.
   */
  public String toString()
  {
    return image;
  }

  /**
   * Returns a new Token object, by default. However, if you want, you
   * can create and return subclass objects based on the value of ofKind.
   * Simply add the cases to the switch for all those special cases.
   * For example, if you have a subclass of Token called IDToken that
   * you want to create if ofKind is ID, simply add something like :
   *
   *    case MyParserConstants.ID : return new IDToken(ofKind, image);
   *
   * to the following switch statement. Then you can cast matchedToken
   * variable to the appropriate type and use sit in your lexical actions.
   */
  public static Token newToken(int ofKind, String image)
  {
    switch(ofKind)
    {
      default : return new Token(ofKind, image);
    }
  }

  public static Token newToken(int ofKind)
  {
    return newToken(ofKind, null);
  }

}
/* JavaCC - OriginalChecksum=f463ad6fd3205ca07166de02ee86b907 (do not edit this line) */
diff --git a/solr/core/src/java/org/apache/solr/parser/TokenMgrError.java b/solr/core/src/java/org/apache/solr/parser/TokenMgrError.java
new file mode 100644
index 00000000000..9dbfd06a38b
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/parser/TokenMgrError.java
@@ -0,0 +1,147 @@
/* Generated By:JavaCC: Do not edit this line. TokenMgrError.java Version 5.0 */
/* JavaCCOptions: */
package org.apache.solr.parser;

/** Token Manager Error. */
public class TokenMgrError extends Error
{

  /**
   * The version identifier for this Serializable class.
   * Increment only if the <i>serialized</i> form of the
   * class changes.
   */
  private static final long serialVersionUID = 1L;

  /*
   * Ordinals for various reasons why an Error of this type can be thrown.
   */

  /**
   * Lexical error occurred.
   */
  static final int LEXICAL_ERROR = 0;

  /**
   * An attempt was made to create a second instance of a static token manager.
   */
  static final int STATIC_LEXER_ERROR = 1;

  /**
   * Tried to change to an invalid lexical state.
   */
  static final int INVALID_LEXICAL_STATE = 2;

  /**
   * Detected (and bailed out of) an infinite loop in the token manager.
   */
  static final int LOOP_DETECTED = 3;

  /**
   * Indicates the reason why the exception is thrown. It will have
   * one of the above 4 values.
   */
  int errorCode;

  /**
   * Replaces unprintable characters by their escaped (or unicode escaped)
   * equivalents in the given string
   */
  protected static final String addEscapes(String str) {
    StringBuffer retval = new StringBuffer();
    char ch;
    for (int i = 0; i < str.length(); i++) {
      switch (str.charAt(i))
      {
        case 0 :
          continue;
        case '\b':
          retval.append("\\b");
          continue;
        case '\t':
          retval.append("\\t");
          continue;
        case '\n':
          retval.append("\\n");
          continue;
        case '\f':
          retval.append("\\f");
          continue;
        case '\r':
          retval.append("\\r");
          continue;
        case '\"':
          retval.append("\\\"");
          continue;
        case '\'':
          retval.append("\\\'");
          continue;
        case '\\':
          retval.append("\\\\");
          continue;
        default:
          if ((ch = str.charAt(i)) < 0x20 || ch > 0x7e) {
            String s = "0000" + Integer.toString(ch, 16);
            retval.append("\\u" + s.substring(s.length() - 4, s.length()));
          } else {
            retval.append(ch);
          }
          continue;
      }
    }
    return retval.toString();
  }

  /**
   * Returns a detailed message for the Error when it is thrown by the
   * token manager to indicate a lexical error.
   * Parameters :
   *    EOFSeen     : indicates if EOF caused the lexical error
   *    curLexState : lexical state in which this error occurred
   *    errorLine   : line number when the error occurred
   *    errorColumn : column number when the error occurred
   *    errorAfter  : prefix that was seen before this error occurred
   *    curchar     : the offending character
   * Note: You can customize the lexical error message by modifying this method.
   */
  protected static String LexicalError(boolean EOFSeen, int lexState, int errorLine, int errorColumn, String errorAfter, char curChar) {
    return("Lexical error at line " +
          errorLine + ", column " +
          errorColumn + ".  Encountered: " +
          (EOFSeen ? "<EOF> " : ("\"" + addEscapes(String.valueOf(curChar)) + "\"") + " (" + (int)curChar + "), ") +
          "after : \"" + addEscapes(errorAfter) + "\"");
  }

  /**
   * You can also modify the body of this method to customize your error messages.
   * For example, cases like LOOP_DETECTED and INVALID_LEXICAL_STATE are not
   * of end-users concern, so you can return something like :
   *
   *     "Internal Error : Please file a bug report .... "
   *
   * from this method for such cases in the release version of your parser.
   */
  public String getMessage() {
    return super.getMessage();
  }

  /*
   * Constructors of various flavors follow.
   */

  /** No arg constructor. */
  public TokenMgrError() {
  }

  /** Constructor with message and reason. */
  public TokenMgrError(String message, int reason) {
    super(message);
    errorCode = reason;
  }

  /** Full Constructor. */
  public TokenMgrError(boolean EOFSeen, int lexState, int errorLine, int errorColumn, String errorAfter, char curChar, int reason) {
    this(LexicalError(EOFSeen, lexState, errorLine, errorColumn, errorAfter, curChar), reason);
  }
}
/* JavaCC - OriginalChecksum=200a46f65c1a0f71a7f037b35f4e934e (do not edit this line) */
diff --git a/solr/core/src/java/org/apache/solr/request/SimpleFacets.java b/solr/core/src/java/org/apache/solr/request/SimpleFacets.java
index e1a583b50f7..f5148a028e1 100644
-- a/solr/core/src/java/org/apache/solr/request/SimpleFacets.java
++ b/solr/core/src/java/org/apache/solr/request/SimpleFacets.java
@@ -18,7 +18,6 @@
 package org.apache.solr.request;
 
 import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.search.*;
 import org.apache.lucene.search.grouping.AbstractAllGroupHeadsCollector;
 import org.apache.lucene.search.grouping.term.TermGroupFacetCollector;
@@ -95,7 +94,7 @@ public class SimpleFacets {
   }
 
 
  protected void parseParams(String type, String param) throws ParseException, IOException {
  protected void parseParams(String type, String param) throws SyntaxError, IOException {
     localParams = QueryParsing.getLocalParams(param, req.getParams());
     docs = docsOrig;
     facetValue = param;
@@ -209,7 +208,7 @@ public class SimpleFacets {
 
     } catch (IOException e) {
       throw new SolrException(ErrorCode.SERVER_ERROR, e);
    } catch (ParseException e) {
    } catch (SyntaxError e) {
       throw new SolrException(ErrorCode.BAD_REQUEST, e);
     }
     return facetResponse;
@@ -221,7 +220,7 @@ public class SimpleFacets {
    *
    * @see FacetParams#FACET_QUERY
    */
  public NamedList<Integer> getFacetQueryCounts() throws IOException,ParseException {
  public NamedList<Integer> getFacetQueryCounts() throws IOException,SyntaxError {
 
     NamedList<Integer> res = new SimpleOrderedMap<Integer>();
 
@@ -409,7 +408,7 @@ public class SimpleFacets {
    * @see #getFacetTermEnumCounts
    */
   public NamedList<Object> getFacetFieldCounts()
          throws IOException, ParseException {
          throws IOException, SyntaxError {
 
     NamedList<Object> res = new SimpleOrderedMap<Object>();
     String[] facetFs = params.getParams(FacetParams.FACET_FIELD);
@@ -826,7 +825,7 @@ public class SimpleFacets {
    */
   @Deprecated
   public NamedList<Object> getFacetDateCounts()
    throws IOException, ParseException {
    throws IOException, SyntaxError {
 
     final NamedList<Object> resOuter = new SimpleOrderedMap<Object>();
     final String[] fields = params.getParams(FacetParams.FACET_DATE);
@@ -845,7 +844,7 @@ public class SimpleFacets {
    */
   @Deprecated
   public void getFacetDateCounts(String dateFacet, NamedList<Object> resOuter)
      throws IOException, ParseException {
      throws IOException, SyntaxError {
 
     final IndexSchema schema = searcher.getSchema();
 
@@ -1008,7 +1007,7 @@ public class SimpleFacets {
    * @see FacetParams#FACET_RANGE
    */
 
  public NamedList<Object> getFacetRangeCounts() throws IOException, ParseException {
  public NamedList<Object> getFacetRangeCounts() throws IOException, SyntaxError {
     final NamedList<Object> resOuter = new SimpleOrderedMap<Object>();
     final String[] fields = params.getParams(FacetParams.FACET_RANGE);
 
@@ -1022,7 +1021,7 @@ public class SimpleFacets {
   }
 
   void getFacetRangeCounts(String facetRange, NamedList<Object> resOuter)
      throws IOException, ParseException {
      throws IOException, SyntaxError {
 
     final IndexSchema schema = searcher.getSchema();
 
diff --git a/solr/core/src/java/org/apache/solr/search/BoostQParserPlugin.java b/solr/core/src/java/org/apache/solr/search/BoostQParserPlugin.java
index 1abb0c93632..bf8e7ae97b4 100755
-- a/solr/core/src/java/org/apache/solr/search/BoostQParserPlugin.java
++ b/solr/core/src/java/org/apache/solr/search/BoostQParserPlugin.java
@@ -20,7 +20,6 @@ import org.apache.lucene.queries.function.BoostedQuery;
 import org.apache.lucene.queries.function.FunctionQuery;
 import org.apache.lucene.queries.function.ValueSource;
 import org.apache.lucene.queries.function.valuesource.QueryValueSource;
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.search.Query;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.NamedList;
@@ -52,7 +51,7 @@ public class BoostQParserPlugin extends QParserPlugin {
       String b;
 
       @Override
      public Query parse() throws ParseException {
      public Query parse() throws SyntaxError {
         b = localParams.get(BOOSTFUNC);
         baseParser = subQuery(localParams.get(QueryParsing.V), null);
         Query q = baseParser.getQuery();
@@ -74,7 +73,7 @@ public class BoostQParserPlugin extends QParserPlugin {
       }
                                            
       @Override
      public Query getHighlightQuery() throws ParseException {
      public Query getHighlightQuery() throws SyntaxError {
         return baseParser.getHighlightQuery();
       }
 
diff --git a/solr/core/src/java/org/apache/solr/search/DisMaxQParser.java b/solr/core/src/java/org/apache/solr/search/DisMaxQParser.java
index f02a061d0ba..844484930b5 100644
-- a/solr/core/src/java/org/apache/solr/search/DisMaxQParser.java
++ b/solr/core/src/java/org/apache/solr/search/DisMaxQParser.java
@@ -16,12 +16,11 @@
  */
 package org.apache.solr.search;
 
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
 import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.Query;
 import org.apache.solr.common.params.CommonParams;
import org.apache.solr.parser.QueryParser;
 import org.apache.solr.schema.IndexSchema;
 import org.apache.solr.common.params.DisMaxParams;
 import org.apache.solr.common.params.SolrParams;
@@ -58,8 +57,8 @@ public class DisMaxQParser extends QParser {
    */
   public static String parseMinShouldMatch(final IndexSchema schema, 
                                            final SolrParams params) {
    QueryParser.Operator op = QueryParsing.getQueryParserDefaultOperator
      (schema, params.get(QueryParsing.OP));
    org.apache.solr.parser.QueryParser.Operator op = QueryParsing.getQueryParserDefaultOperator
        (schema, params.get(QueryParsing.OP));
     return params.get(DisMaxParams.MM, 
                       op.equals(QueryParser.Operator.AND) ? "100%" : "0%");
   }
@@ -69,12 +68,12 @@ public class DisMaxQParser extends QParser {
    * or {@link org.apache.solr.schema.IndexSchema#getDefaultSearchFieldName()}.
    */
   public static Map<String, Float> parseQueryFields(final IndexSchema indexSchema, final SolrParams solrParams)
      throws ParseException {
      throws SyntaxError {
     Map<String, Float> queryFields = SolrPluginUtils.parseFieldBoosts(solrParams.getParams(DisMaxParams.QF));
     if (queryFields.isEmpty()) {
       String df = QueryParsing.getDefaultField(indexSchema, solrParams.get(CommonParams.DF));
       if (df == null) {
        throw new ParseException("Neither "+DisMaxParams.QF+", "+CommonParams.DF +", nor the default search field are present.");
        throw new SyntaxError("Neither "+DisMaxParams.QF+", "+CommonParams.DF +", nor the default search field are present.");
       }
       queryFields.put(df, 1.0f);
     }
@@ -96,7 +95,7 @@ public class DisMaxQParser extends QParser {
 
 
   @Override
  public Query parse() throws ParseException {
  public Query parse() throws SyntaxError {
     SolrParams solrParams = SolrParams.wrapDefaults(localParams, params);
 
     queryFields = parseQueryFields(req.getSchema(), solrParams);
@@ -115,7 +114,7 @@ public class DisMaxQParser extends QParser {
     return query;
   }
 
  protected void addBoostFunctions(BooleanQuery query, SolrParams solrParams) throws ParseException {
  protected void addBoostFunctions(BooleanQuery query, SolrParams solrParams) throws SyntaxError {
     String[] boostFuncs = solrParams.getParams(DisMaxParams.BF);
     if (null != boostFuncs && 0 != boostFuncs.length) {
       for (String boostFunc : boostFuncs) {
@@ -133,7 +132,7 @@ public class DisMaxQParser extends QParser {
     }
   }
 
  protected void addBoostQuery(BooleanQuery query, SolrParams solrParams) throws ParseException {
  protected void addBoostQuery(BooleanQuery query, SolrParams solrParams) throws SyntaxError {
     boostParams = solrParams.getParams(DisMaxParams.BQ);
     //List<Query> boostQueries = SolrPluginUtils.parseQueryStrings(req, boostParams);
     boostQueries = null;
@@ -168,7 +167,7 @@ public class DisMaxQParser extends QParser {
   }
 
   /** Adds the main query to the query argument. If its blank then false is returned. */
  protected boolean addMainQuery(BooleanQuery query, SolrParams solrParams) throws ParseException {
  protected boolean addMainQuery(BooleanQuery query, SolrParams solrParams) throws SyntaxError {
     Map<String, Float> phraseFields = SolrPluginUtils.parseFieldBoosts(solrParams.getParams(DisMaxParams.PF));
     float tiebreaker = solrParams.getFloat(DisMaxParams.TIE, 0.0f);
 
@@ -206,7 +205,7 @@ public class DisMaxQParser extends QParser {
     return true;
   }
 
  protected Query getAlternateUserQuery(SolrParams solrParams) throws ParseException {
  protected Query getAlternateUserQuery(SolrParams solrParams) throws SyntaxError {
     String altQ = solrParams.get(DisMaxParams.ALTQ);
     if (altQ != null) {
       QParser altQParser = subQuery(altQ, null);
@@ -216,7 +215,7 @@ public class DisMaxQParser extends QParser {
     }
   }
 
  protected Query getPhraseQuery(String userQuery, SolrPluginUtils.DisjunctionMaxQueryParser pp) throws ParseException {
  protected Query getPhraseQuery(String userQuery, SolrPluginUtils.DisjunctionMaxQueryParser pp) throws SyntaxError {
     /* * * Add on Phrases for the Query * * */
 
     /* build up phrase boosting queries */
@@ -231,7 +230,9 @@ public class DisMaxQParser extends QParser {
   }
 
   protected Query getUserQuery(String userQuery, SolrPluginUtils.DisjunctionMaxQueryParser up, SolrParams solrParams)
          throws ParseException {
          throws SyntaxError {


     String minShouldMatch = parseMinShouldMatch(req.getSchema(), solrParams);
     Query dis = up.parse(userQuery);
     Query query = dis;
@@ -261,7 +262,7 @@ public class DisMaxQParser extends QParser {
   }
 
   @Override
  public Query getHighlightQuery() throws ParseException {
  public Query getHighlightQuery() throws SyntaxError {
     return parsedUserQuery == null ? altUserQuery : parsedUserQuery;
   }
 
diff --git a/solr/core/src/java/org/apache/solr/search/ExtendedDismaxQParserPlugin.java b/solr/core/src/java/org/apache/solr/search/ExtendedDismaxQParserPlugin.java
index e1724d9602c..55049bae2de 100755
-- a/solr/core/src/java/org/apache/solr/search/ExtendedDismaxQParserPlugin.java
++ b/solr/core/src/java/org/apache/solr/search/ExtendedDismaxQParserPlugin.java
@@ -34,11 +34,11 @@ import org.apache.lucene.queries.function.FunctionQuery;
 import org.apache.lucene.queries.function.ValueSource;
 import org.apache.lucene.queries.function.valuesource.ProductFloatFunction;
 import org.apache.lucene.queries.function.valuesource.QueryValueSource;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
 import org.apache.lucene.search.*;
 import org.apache.solr.analysis.TokenizerChain;
import org.apache.solr.search.SolrQueryParser.MagicFieldName;
import org.apache.solr.parser.ParseException;
import org.apache.solr.parser.QueryParser;
import org.apache.solr.parser.SolrQueryParserBase.MagicFieldName;
 import org.apache.solr.common.params.DisMaxParams;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.NamedList;
@@ -118,7 +118,7 @@ class ExtendedDismaxQParser extends QParser {
 
 
   @Override
  public Query parse() throws ParseException {
  public Query parse() throws SyntaxError {
     SolrParams localParams = getLocalParams();
     SolrParams params = getParams();
     
@@ -173,7 +173,7 @@ class ExtendedDismaxQParser extends QParser {
         query.add( altUserQuery , BooleanClause.Occur.MUST );
       } else {
         return null;
        // throw new ParseException("missing query string" );
        // throw new SyntaxError("missing query string" );
       }
     }
     else {     
@@ -451,7 +451,7 @@ class ExtendedDismaxQParser extends QParser {
                                         int shingleSize,
                                         final float tiebreaker,
                                         final int slop) 
    throws ParseException {
    throws SyntaxError {
     
     if (null == fields || fields.isEmpty() || 
         null == clauses || clauses.size() < shingleSize ) 
@@ -519,7 +519,7 @@ class ExtendedDismaxQParser extends QParser {
   }
 
   @Override
  public Query getHighlightQuery() throws ParseException {
  public Query getHighlightQuery() throws SyntaxError {
     return parsedUserQuery == null ? altUserQuery : parsedUserQuery;
   }
 
@@ -900,7 +900,7 @@ class ExtendedDismaxQParser extends QParser {
     protected Map<String,Alias> aliases = new HashMap<String,Alias>(3);
 
     public ExtendedSolrQueryParser(QParser parser, String defaultField) {
      super(parser, defaultField, null);
      super(parser, defaultField);
       // don't trust that our parent class won't ever change it's default
       setDefaultOperator(QueryParser.Operator.OR);
     }
@@ -911,7 +911,7 @@ class ExtendedDismaxQParser extends QParser {
     }
 
     @Override
    protected Query getBooleanQuery(List clauses, boolean disableCoord) throws ParseException {
    protected Query getBooleanQuery(List clauses, boolean disableCoord) throws SyntaxError {
       Query q = super.getBooleanQuery(clauses, disableCoord);
       if (q != null) {
         q = QueryUtils.makeQueryable(q);
@@ -971,7 +971,7 @@ class ExtendedDismaxQParser extends QParser {
     int slop;
 
     @Override
    protected Query getFieldQuery(String field, String val, boolean quoted) throws ParseException {
    protected Query getFieldQuery(String field, String val, boolean quoted) throws SyntaxError {
 //System.out.println("getFieldQuery: val="+val);
 
       this.type = QType.FIELD;
@@ -982,7 +982,7 @@ class ExtendedDismaxQParser extends QParser {
     }
 
     @Override
    protected Query getFieldQuery(String field, String val, int slop) throws ParseException {
    protected Query getFieldQuery(String field, String val, int slop) throws SyntaxError {
 //System.out.println("getFieldQuery: val="+val+" slop="+slop);
 
       this.type = QType.PHRASE;
@@ -993,7 +993,7 @@ class ExtendedDismaxQParser extends QParser {
     }
 
     @Override
    protected Query getPrefixQuery(String field, String val) throws ParseException {
    protected Query getPrefixQuery(String field, String val) throws SyntaxError {
 //System.out.println("getPrefixQuery: val="+val);
       if (val.equals("") && field.equals("*")) {
         return new MatchAllDocsQuery();
@@ -1005,7 +1005,7 @@ class ExtendedDismaxQParser extends QParser {
     }
 
     @Override
    protected Query newFieldQuery(Analyzer analyzer, String field, String queryText, boolean quoted) throws ParseException {
    protected Query newFieldQuery(Analyzer analyzer, String field, String queryText, boolean quoted) throws SyntaxError {
       Analyzer actualAnalyzer;
       if (removeStopFilter) {
         if (nonStopFilterAnalyzerPerField == null) {
@@ -1022,7 +1022,7 @@ class ExtendedDismaxQParser extends QParser {
     }
 
     @Override
     protected Query getRangeQuery(String field, String a, String b, boolean startInclusive, boolean endInclusive) throws ParseException {
     protected Query getRangeQuery(String field, String a, String b, boolean startInclusive, boolean endInclusive) throws SyntaxError {
 //System.out.println("getRangeQuery:");
 
       this.type = QType.RANGE;
@@ -1035,7 +1035,7 @@ class ExtendedDismaxQParser extends QParser {
     }
 
     @Override
    protected Query getWildcardQuery(String field, String val) throws ParseException {
    protected Query getWildcardQuery(String field, String val) throws SyntaxError {
 //System.out.println("getWildcardQuery: val="+val);
 
       if (val.equals("*")) {
@@ -1052,7 +1052,7 @@ class ExtendedDismaxQParser extends QParser {
     }
 
     @Override
    protected Query getFuzzyQuery(String field, String val, float minSimilarity) throws ParseException {
    protected Query getFuzzyQuery(String field, String val, float minSimilarity) throws SyntaxError {
 //System.out.println("getFuzzyQuery: val="+val);
 
       this.type = QType.FUZZY;
@@ -1069,7 +1069,7 @@ class ExtendedDismaxQParser extends QParser {
      * DisjunctionMaxQuery.  (so yes: aliases which point at other
      * aliases should work)
      */
    protected Query getAliasedQuery() throws ParseException {
    protected Query getAliasedQuery() throws SyntaxError {
       Alias a = aliases.get(field);
       this.validateCyclicAliasing(field);
       if (a != null) {
@@ -1112,11 +1112,11 @@ class ExtendedDismaxQParser extends QParser {
     /**
      * Validate there is no cyclic referencing in the aliasing
      */
    private void validateCyclicAliasing(String field) throws ParseException {
    private void validateCyclicAliasing(String field) throws SyntaxError {
        Set<String> set = new HashSet<String>();
        set.add(field);
        if(validateField(field, set)) {
         throw new ParseException("Field aliases lead to a cycle");
         throw new SyntaxError("Field aliases lead to a cycle");
        }
     }
     
@@ -1138,7 +1138,7 @@ class ExtendedDismaxQParser extends QParser {
       return hascycle;
     }
 
    protected List<Query> getQueries(Alias a) throws ParseException {
    protected List<Query> getQueries(Alias a) throws SyntaxError {
        if (a == null) return null;
        if (a.fields.size()==0) return null;
        List<Query> lst= new ArrayList<Query>(4);
diff --git a/solr/core/src/java/org/apache/solr/search/FunctionQParser.java b/solr/core/src/java/org/apache/solr/search/FunctionQParser.java
index 0f0adc03559..53744eaa27d 100755
-- a/solr/core/src/java/org/apache/solr/search/FunctionQParser.java
++ b/solr/core/src/java/org/apache/solr/search/FunctionQParser.java
@@ -19,12 +19,10 @@ package org.apache.solr.search;
 import org.apache.lucene.queries.function.FunctionQuery;
 import org.apache.lucene.queries.function.ValueSource;
 import org.apache.lucene.queries.function.valuesource.*;
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.search.Query;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.function.*;
 
 import java.util.ArrayList;
 import java.util.HashMap;
@@ -60,7 +58,7 @@ public class FunctionQParser extends QParser {
   }
 
   @Override
  public Query parse() throws ParseException {
  public Query parse() throws SyntaxError {
     sp = new QueryParsing.StrParser(getString());
 
     ValueSource vs = null;
@@ -92,7 +90,7 @@ public class FunctionQParser extends QParser {
     }
 
     if (parseToEnd && sp.pos < sp.end) {
      throw new ParseException("Unexpected text after function: " + sp.val.substring(sp.pos, sp.end));
      throw new SyntaxError("Unexpected text after function: " + sp.val.substring(sp.pos, sp.end));
     }
 
     if (lst != null) {
@@ -107,7 +105,7 @@ public class FunctionQParser extends QParser {
    * 
    * @return whether more args exist
    */
  public boolean hasMoreArguments() throws ParseException {
  public boolean hasMoreArguments() throws SyntaxError {
     int ch = sp.peek();
     /* determine whether the function is ending with a paren or end of str */
     return (! (ch == 0 || ch == ')') );
@@ -116,9 +114,9 @@ public class FunctionQParser extends QParser {
   /*
    * TODO: Doc
    */
  public String parseId() throws ParseException {
  public String parseId() throws SyntaxError {
     String value = parseArg();
    if (argWasQuoted) throw new ParseException("Expected identifier instead of quoted string:" + value);
    if (argWasQuoted) throw new SyntaxError("Expected identifier instead of quoted string:" + value);
     return value;
   }
   
@@ -127,9 +125,9 @@ public class FunctionQParser extends QParser {
    * 
    * @return Float
    */
  public Float parseFloat() throws ParseException {
  public Float parseFloat() throws SyntaxError {
     String str = parseArg();
    if (argWasQuoted()) throw new ParseException("Expected float instead of quoted string:" + str);
    if (argWasQuoted()) throw new SyntaxError("Expected float instead of quoted string:" + str);
     float value = Float.parseFloat(str);
     return value;
   }
@@ -138,9 +136,9 @@ public class FunctionQParser extends QParser {
    * Parse a Double
    * @return double
    */
  public double parseDouble() throws ParseException {
  public double parseDouble() throws SyntaxError {
     String str = parseArg();
    if (argWasQuoted()) throw new ParseException("Expected double instead of quoted string:" + str);
    if (argWasQuoted()) throw new SyntaxError("Expected double instead of quoted string:" + str);
     double value = Double.parseDouble(str);
     return value;
   }
@@ -149,9 +147,9 @@ public class FunctionQParser extends QParser {
    * Parse an integer
    * @return An int
    */
  public int parseInt() throws ParseException {
  public int parseInt() throws SyntaxError {
     String str = parseArg();
    if (argWasQuoted()) throw new ParseException("Expected double instead of quoted string:" + str);
    if (argWasQuoted()) throw new SyntaxError("Expected double instead of quoted string:" + str);
     int value = Integer.parseInt(str);
     return value;
   }
@@ -162,7 +160,7 @@ public class FunctionQParser extends QParser {
     return argWasQuoted;
   }
 
  public String parseArg() throws ParseException {
  public String parseArg() throws SyntaxError {
     argWasQuoted = false;
 
     sp.eatws();
@@ -186,7 +184,7 @@ public class FunctionQParser extends QParser {
         int valStart = sp.pos;
         for (;;) {
           if (sp.pos >= sp.end) {
            throw new ParseException("Missing end to unquoted value starting at " + valStart + " str='" + sp.val +"'");
            throw new SyntaxError("Missing end to unquoted value starting at " + valStart + " str='" + sp.val +"'");
           }
           char c = sp.val.charAt(sp.pos);
           if (c==')' || c==',' || Character.isWhitespace(c)) {
@@ -209,7 +207,7 @@ public class FunctionQParser extends QParser {
    * 
    * @return List&lt;ValueSource&gt;
    */
  public List<ValueSource> parseValueSourceList() throws ParseException {
  public List<ValueSource> parseValueSourceList() throws SyntaxError {
     List<ValueSource> sources = new ArrayList<ValueSource>(3);
     while (hasMoreArguments()) {
       sources.add(parseValueSource(true));
@@ -220,7 +218,7 @@ public class FunctionQParser extends QParser {
   /**
    * Parse an individual ValueSource.
    */
  public ValueSource parseValueSource() throws ParseException {
  public ValueSource parseValueSource() throws SyntaxError {
     /* consume the delimiter afterward for an external call to parseValueSource */
     return parseValueSource(true);
   }
@@ -228,7 +226,7 @@ public class FunctionQParser extends QParser {
   /*
    * TODO: Doc
    */
  public Query parseNestedQuery() throws ParseException {
  public Query parseNestedQuery() throws SyntaxError {
     Query nestedQuery;
     
     if (sp.opt("$")) {
@@ -257,10 +255,10 @@ public class FunctionQParser extends QParser {
           sub = subQuery(qs, null);
           // int subEnd = sub.findEnd(')');
           // TODO.. implement functions to find the end of a nested query
          throw new ParseException("Nested local params must have value in v parameter.  got '" + qs + "'");
          throw new SyntaxError("Nested local params must have value in v parameter.  got '" + qs + "'");
         }
       } else {
        throw new ParseException("Nested function query must use $param or {!v=value} forms. got '" + qs + "'");
        throw new SyntaxError("Nested function query must use $param or {!v=value} forms. got '" + qs + "'");
       }
   
       sp.pos += end-start;  // advance past nested query
@@ -276,7 +274,7 @@ public class FunctionQParser extends QParser {
    * 
    * @param doConsumeDelimiter whether to consume a delimiter following the ValueSource  
    */
  protected ValueSource parseValueSource(boolean doConsumeDelimiter) throws ParseException {
  protected ValueSource parseValueSource(boolean doConsumeDelimiter) throws SyntaxError {
     ValueSource valueSource;
     
     int ch = sp.peek();
@@ -297,7 +295,7 @@ public class FunctionQParser extends QParser {
       String param = sp.getId();
       String val = getParam(param);
       if (val == null) {
        throw new ParseException("Missing param " + param + " while parsing function '" + sp.val + "'");
        throw new SyntaxError("Missing param " + param + " while parsing function '" + sp.val + "'");
       }
 
       QParser subParser = subQuery(val, "func");
@@ -349,7 +347,7 @@ public class FunctionQParser extends QParser {
         // a function... look it up.
         ValueSourceParser argParser = req.getCore().getValueSourceParser(id);
         if (argParser==null) {
          throw new ParseException("Unknown function " + id + " in FunctionQuery(" + sp + ")");
          throw new SyntaxError("Unknown function " + id + " in FunctionQuery(" + sp + ")");
         }
         valueSource = argParser.parse(this);
         sp.expect(")");
@@ -379,7 +377,7 @@ public class FunctionQParser extends QParser {
    * 
    * @return whether a delimiter was consumed
    */
  protected boolean consumeArgumentDelimiter() throws ParseException {
  protected boolean consumeArgumentDelimiter() throws SyntaxError {
     /* if a list of args is ending, don't expect the comma */
     if (hasMoreArguments()) {
       sp.expect(",");
diff --git a/solr/core/src/java/org/apache/solr/search/FunctionRangeQParserPlugin.java b/solr/core/src/java/org/apache/solr/search/FunctionRangeQParserPlugin.java
index 0e89772dfe2..095c90d937f 100755
-- a/solr/core/src/java/org/apache/solr/search/FunctionRangeQParserPlugin.java
++ b/solr/core/src/java/org/apache/solr/search/FunctionRangeQParserPlugin.java
@@ -16,23 +16,15 @@
  */
 package org.apache.solr.search;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queries.function.FunctionValues;
 import org.apache.lucene.queries.function.FunctionQuery;
 import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.ValueSourceScorer;
 import org.apache.lucene.queries.function.valuesource.QueryValueSource;
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.search.*;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrConfig;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.search.function.*;
 
import java.io.IOException;
import java.util.Map;

 /**
  * Create a range query over a function.
  * <br>Other parameters:
@@ -56,7 +48,7 @@ public class FunctionRangeQParserPlugin extends QParserPlugin {
       String funcStr;
 
       @Override
      public Query parse() throws ParseException {
      public Query parse() throws SyntaxError {
         funcStr = localParams.get(QueryParsing.V, null);
         Query funcQ = subQuery(funcStr, FunctionQParserPlugin.NAME).getQuery();
         if (funcQ instanceof FunctionQuery) {
diff --git a/solr/core/src/java/org/apache/solr/search/Grouping.java b/solr/core/src/java/org/apache/solr/search/Grouping.java
index 23599e3b6d7..1ae2561cab2 100755
-- a/solr/core/src/java/org/apache/solr/search/Grouping.java
++ b/solr/core/src/java/org/apache/solr/search/Grouping.java
@@ -18,12 +18,10 @@
 package org.apache.solr.search;
 
 import org.apache.commons.lang.ArrayUtils;
import org.apache.lucene.index.IndexableField;
 import org.apache.lucene.index.StorableField;
 import org.apache.lucene.queries.function.FunctionQuery;
 import org.apache.lucene.queries.function.ValueSource;
 import org.apache.lucene.queries.function.valuesource.QueryValueSource;
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.search.*;
 import org.apache.lucene.search.grouping.*;
 import org.apache.lucene.search.grouping.function.FunctionAllGroupHeadsCollector;
@@ -128,7 +126,7 @@ public class Grouping {
    *
    * @param field The fieldname to group by.
    */
  public void addFieldCommand(String field, SolrQueryRequest request) throws ParseException {
  public void addFieldCommand(String field, SolrQueryRequest request) throws SyntaxError {
     SchemaField schemaField = searcher.getSchema().getField(field); // Throws an exception when field doesn't exist. Bad request.
     FieldType fieldType = schemaField.getType();
     ValueSource valueSource = fieldType.getValueSource(schemaField, null);
@@ -160,7 +158,7 @@ public class Grouping {
     commands.add(gc);
   }
 
  public void addFunctionCommand(String groupByStr, SolrQueryRequest request) throws ParseException {
  public void addFunctionCommand(String groupByStr, SolrQueryRequest request) throws SyntaxError {
     QParser parser = QParser.getParser(groupByStr, "func", request);
     Query q = parser.getQuery();
     final Grouping.Command gc;
@@ -203,7 +201,7 @@ public class Grouping {
     commands.add(gc);
   }
 
  public void addQueryCommand(String groupByStr, SolrQueryRequest request) throws ParseException {
  public void addQueryCommand(String groupByStr, SolrQueryRequest request) throws SyntaxError {
     QParser parser = QParser.getParser(groupByStr, null, request);
     Query gq = parser.getQuery();
     Grouping.CommandQuery gc = new CommandQuery();
diff --git a/solr/core/src/java/org/apache/solr/search/JoinQParserPlugin.java b/solr/core/src/java/org/apache/solr/search/JoinQParserPlugin.java
index 7d444af1910..682268ac47a 100644
-- a/solr/core/src/java/org/apache/solr/search/JoinQParserPlugin.java
++ b/solr/core/src/java/org/apache/solr/search/JoinQParserPlugin.java
@@ -17,7 +17,6 @@
 package org.apache.solr.search;
 
 import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.search.*;
 import org.apache.lucene.search.similarities.Similarity;
 import org.apache.lucene.util.Bits;
@@ -53,7 +52,7 @@ public class JoinQParserPlugin extends QParserPlugin {
 
   public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
     return new QParser(qstr, localParams, params, req) {
      public Query parse() throws ParseException {
      public Query parse() throws SyntaxError {
         String fromField = getParam("from");
         String fromIndex = getParam("fromIndex");
         String toField = getParam("to");
diff --git a/solr/core/src/java/org/apache/solr/search/LuceneQParserPlugin.java b/solr/core/src/java/org/apache/solr/search/LuceneQParserPlugin.java
index 6912c4d868a..326d5aeb700 100755
-- a/solr/core/src/java/org/apache/solr/search/LuceneQParserPlugin.java
++ b/solr/core/src/java/org/apache/solr/search/LuceneQParserPlugin.java
@@ -16,7 +16,6 @@
  */
 package org.apache.solr.search;
 
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.Sort;
 import org.apache.solr.common.params.CommonParams;
@@ -55,7 +54,7 @@ class LuceneQParser extends QParser {
 
 
   @Override
  public Query parse() throws ParseException {
  public Query parse() throws SyntaxError {
     String qstr = getString();
     if (qstr == null || qstr.length()==0) return null;
 
@@ -75,7 +74,7 @@ class LuceneQParser extends QParser {
 
   @Override
   public String[] getDefaultHighlightFields() {
    return lparser == null ? new String[]{} : new String[]{lparser.getField()};
    return lparser == null ? new String[]{} : new String[]{lparser.getDefaultField()};
   }
   
 }
@@ -89,7 +88,7 @@ class OldLuceneQParser extends LuceneQParser {
   }
 
   @Override
  public Query parse() throws ParseException {
  public Query parse() throws SyntaxError {
     // handle legacy "query;sort" syntax
     if (getLocalParams() == null) {
       String qstr = getString();
@@ -107,7 +106,7 @@ class OldLuceneQParser extends LuceneQParser {
           qstr = commands.get(0);
         }
         else if (commands.size() > 2) {
          throw new ParseException("If you want to use multiple ';' in the query, use the 'sort' param.");
          throw new SyntaxError("If you want to use multiple ';' in the query, use the 'sort' param.");
         }
       }
       setString(qstr);
@@ -117,7 +116,7 @@ class OldLuceneQParser extends LuceneQParser {
   }
 
   @Override
  public SortSpec getSort(boolean useGlobal) throws ParseException {
  public SortSpec getSort(boolean useGlobal) throws SyntaxError {
     SortSpec sort = super.getSort(useGlobal);
     if (sortStr != null && sortStr.length()>0 && sort.getSort()==null) {
       Sort oldSort = QueryParsing.parseSort(sortStr, getReq());
diff --git a/solr/core/src/java/org/apache/solr/search/NestedQParserPlugin.java b/solr/core/src/java/org/apache/solr/search/NestedQParserPlugin.java
index d7fdff81c6d..c53dfadc935 100755
-- a/solr/core/src/java/org/apache/solr/search/NestedQParserPlugin.java
++ b/solr/core/src/java/org/apache/solr/search/NestedQParserPlugin.java
@@ -17,7 +17,6 @@
 package org.apache.solr.search;
 
 import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.search.Query;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.NamedList;
@@ -46,7 +45,7 @@ public class NestedQParserPlugin extends QParserPlugin {
       String b;
 
       @Override
      public Query parse() throws ParseException {
      public Query parse() throws SyntaxError {
         baseParser = subQuery(localParams.get(QueryParsing.V), null);
         return baseParser.getQuery();
       }
@@ -57,7 +56,7 @@ public class NestedQParserPlugin extends QParserPlugin {
       }
 
       @Override
      public Query getHighlightQuery() throws ParseException {
      public Query getHighlightQuery() throws SyntaxError {
         return baseParser.getHighlightQuery();
       }
 
diff --git a/solr/core/src/java/org/apache/solr/search/QParser.java b/solr/core/src/java/org/apache/solr/search/QParser.java
index 71f375a0f4e..16db2d36b5f 100755
-- a/solr/core/src/java/org/apache/solr/search/QParser.java
++ b/solr/core/src/java/org/apache/solr/search/QParser.java
@@ -16,7 +16,6 @@
  */
 package org.apache.solr.search;
 
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.ScoreDoc; //Issue 1726
 import org.apache.lucene.search.Sort;
@@ -100,7 +99,7 @@ public abstract class QParser {
    * there was no input (e.g. no query string) to parse.
    * @see #getQuery()
    **/
  public abstract Query parse() throws ParseException;
  public abstract Query parse() throws SyntaxError;
 
   public SolrParams getLocalParams() {
     return localParams;
@@ -138,7 +137,7 @@ public abstract class QParser {
    * Returns the resulting query from this QParser, calling parse() only the
    * first time and caching the Query result.
    */
  public Query getQuery() throws ParseException {
  public Query getQuery() throws SyntaxError {
     if (query==null) {
       query=parse();
 
@@ -174,9 +173,9 @@ public abstract class QParser {
     }
   }
 
  private void checkRecurse() throws ParseException {
  private void checkRecurse() throws SyntaxError {
     if (recurseCount++ >= 100) {
      throw new ParseException("Infinite Recursion detected parsing query '" + qstr + "'");
      throw new SyntaxError("Infinite Recursion detected parsing query '" + qstr + "'");
     }
   }
 
@@ -197,7 +196,7 @@ public abstract class QParser {
   }
 
   /** Create a new QParser for parsing an embedded sub-query */
  public QParser subQuery(String q, String defaultType) throws ParseException {
  public QParser subQuery(String q, String defaultType) throws SyntaxError {
     checkRecurse();
     if (defaultType == null && localParams != null) {
       // if not passed, try and get the defaultType from local params
@@ -213,7 +212,7 @@ public abstract class QParser {
    * use common params to look up pageScore and pageDoc in global params
    * @return the ScoreDoc
    */
  public ScoreDoc getPaging() throws ParseException
  public ScoreDoc getPaging() throws SyntaxError
   {
     return null;
 
@@ -244,7 +243,7 @@ public abstract class QParser {
    * @param useGlobalParams look up sort, start, rows in global params if not in local params
    * @return the sort specification
    */
  public SortSpec getSort(boolean useGlobalParams) throws ParseException {
  public SortSpec getSort(boolean useGlobalParams) throws SyntaxError {
     getQuery(); // ensure query is parsed first
 
     String sortStr = null;
@@ -288,7 +287,7 @@ public abstract class QParser {
     return new String[]{};
   }
 
  public Query getHighlightQuery() throws ParseException {
  public Query getHighlightQuery() throws SyntaxError {
     Query query = getQuery();
     return query instanceof WrappedQuery ? ((WrappedQuery)query).getWrappedQuery() : query;
   }
@@ -306,7 +305,7 @@ public abstract class QParser {
    * if qstr=<code>{!prefix f=myfield}foo</code>
    * then the prefix query parser will be used.
    */
  public static QParser getParser(String qstr, String defaultParser, SolrQueryRequest req) throws ParseException {
  public static QParser getParser(String qstr, String defaultParser, SolrQueryRequest req) throws SyntaxError {
     // SolrParams localParams = QueryParsing.getLocalParams(qstr, req.getParams());
 
     String stringIncludingLocalParams = qstr;
diff --git a/solr/core/src/java/org/apache/solr/search/QueryParsing.java b/solr/core/src/java/org/apache/solr/search/QueryParsing.java
index a465b3aaabb..cb6c930e5fb 100644
-- a/solr/core/src/java/org/apache/solr/search/QueryParsing.java
++ b/solr/core/src/java/org/apache/solr/search/QueryParsing.java
@@ -20,8 +20,6 @@ package org.apache.solr.search;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.queries.function.FunctionQuery;
 import org.apache.lucene.queries.function.valuesource.QueryValueSource;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
 import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.ConstantScoreQuery;
@@ -39,6 +37,7 @@ import org.apache.lucene.util.CharsRef;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.params.MapSolrParams;
 import org.apache.solr.common.params.SolrParams;
import org.apache.solr.parser.QueryParser;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.schema.FieldType;
 import org.apache.solr.schema.IndexSchema;
@@ -95,12 +94,12 @@ public class QueryParsing {
   }
 
   // note to self: something needs to detect infinite recursion when parsing queries
  public static int parseLocalParams(String txt, int start, Map<String, String> target, SolrParams params) throws ParseException {
  public static int parseLocalParams(String txt, int start, Map<String, String> target, SolrParams params) throws SyntaxError {
     return parseLocalParams(txt, start, target, params, LOCALPARAM_START, LOCALPARAM_END);
   }
 
 
  public static int parseLocalParams(String txt, int start, Map<String, String> target, SolrParams params, String startString, char endChar) throws ParseException {
  public static int parseLocalParams(String txt, int start, Map<String, String> target, SolrParams params, String startString, char endChar) throws SyntaxError {
     int off = start;
     if (!txt.startsWith(startString, off)) return start;
     StrParser p = new StrParser(txt, start, txt.length());
@@ -109,7 +108,7 @@ public class QueryParsing {
     for (; ;) {
       /*
       if (p.pos>=txt.length()) {
        throw new ParseException("Missing '}' parsing local params '" + txt + '"');
        throw new SyntaxError("Missing '}' parsing local params '" + txt + '"');
       }
       */
       char ch = p.peek();
@@ -119,7 +118,7 @@ public class QueryParsing {
 
       String id = p.getId();
       if (id.length() == 0) {
        throw new ParseException("Expected ending character '" + endChar + "' parsing local params '" + txt + '"');
        throw new SyntaxError("Expected ending character '" + endChar + "' parsing local params '" + txt + '"');
 
       }
       String val = null;
@@ -148,7 +147,7 @@ public class QueryParsing {
           int valStart = p.pos;
           for (; ;) {
             if (p.pos >= p.end) {
              throw new ParseException("Missing end to unquoted value starting at " + valStart + " str='" + txt + "'");
              throw new SyntaxError("Missing end to unquoted value starting at " + valStart + " str='" + txt + "'");
             }
             char c = p.val.charAt(p.pos);
             if (c == endChar || Character.isWhitespace(c)) {
@@ -202,7 +201,7 @@ public class QueryParsing {
    * "{!prefix f=myfield}yes" returns type="prefix",f="myfield",v="yes"
    * "{!prefix f=myfield v=$p}" returns type="prefix",f="myfield",v=params.get("p")
    */
  public static SolrParams getLocalParams(String txt, SolrParams params) throws ParseException {
  public static SolrParams getLocalParams(String txt, SolrParams params) throws SyntaxError {
     if (txt == null || !txt.startsWith(LOCALPARAM_START)) {
       return null;
     }
@@ -352,7 +351,7 @@ public class QueryParsing {
         }
       }
 
    } catch (ParseException e) {
    } catch (SyntaxError e) {
       throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "error in sort: " + sortSpec, e);
     }
 
@@ -628,13 +627,13 @@ public class QueryParsing {
     }
 
 
    void expect(String s) throws ParseException {
    void expect(String s) throws SyntaxError {
       eatws();
       int slen = s.length();
       if (val.regionMatches(pos, s, 0, slen)) {
         pos += slen;
       } else {
        throw new ParseException("Expected '" + s + "' at position " + pos + " in '" + val + "'");
        throw new SyntaxError("Expected '" + s + "' at position " + pos + " in '" + val + "'");
       }
     }
 
@@ -723,11 +722,11 @@ public class QueryParsing {
     }
 
 
    String getId() throws ParseException {
    String getId() throws SyntaxError {
       return getId("Expected identifier");
     }
 
    String getId(String errMessage) throws ParseException {
    String getId(String errMessage) throws SyntaxError {
       eatws();
       int id_start = pos;
       char ch;
@@ -745,12 +744,12 @@ public class QueryParsing {
       }
 
       if (errMessage != null) {
        throw new ParseException(errMessage + " at pos " + pos + " str='" + val + "'");
        throw new SyntaxError(errMessage + " at pos " + pos + " str='" + val + "'");
       }
       return null;
     }
 
    public String getGlobbedId(String errMessage) throws ParseException {
    public String getGlobbedId(String errMessage) throws SyntaxError {
       eatws();
       int id_start = pos;
       char ch;
@@ -767,7 +766,7 @@ public class QueryParsing {
       }
 
       if (errMessage != null) {
        throw new ParseException(errMessage + " at pos " + pos + " str='" + val + "'");
        throw new SyntaxError(errMessage + " at pos " + pos + " str='" + val + "'");
       }
       return null;
     }
@@ -793,7 +792,7 @@ public class QueryParsing {
      * sort direction. (True is desc, False is asc).  
      * Position is advanced to after the comma (or end) when result is non null 
      */
    Boolean getSortDirection() throws ParseException {
    Boolean getSortDirection() throws SyntaxError {
       final int startPos = pos;
       final String order = getId(null);
 
@@ -823,7 +822,7 @@ public class QueryParsing {
     }
 
     // return null if not a string
    String getQuotedString() throws ParseException {
    String getQuotedString() throws SyntaxError {
       eatws();
       char delim = peekChar();
       if (!(delim == '\"' || delim == '\'')) {
@@ -833,7 +832,7 @@ public class QueryParsing {
       StringBuilder sb = new StringBuilder(); // needed for escaping
       for (; ;) {
         if (pos >= end) {
          throw new ParseException("Missing end quote for string at pos " + (val_start - 1) + " str='" + val + "'");
          throw new SyntaxError("Missing end quote for string at pos " + (val_start - 1) + " str='" + val + "'");
         }
         char ch = val.charAt(pos);
         if (ch == '\\') {
@@ -858,7 +857,7 @@ public class QueryParsing {
               break;
             case 'u':
               if (pos + 4 >= end) {
                throw new ParseException("bad unicode escape \\uxxxx at pos" + (val_start - 1) + " str='" + val + "'");
                throw new SyntaxError("bad unicode escape \\uxxxx at pos" + (val_start - 1) + " str='" + val + "'");
               }
               ch = (char) Integer.parseInt(val.substring(pos + 1, pos + 5), 16);
               pos += 4;
diff --git a/solr/core/src/java/org/apache/solr/search/ReturnFields.java b/solr/core/src/java/org/apache/solr/search/ReturnFields.java
index a6820aa41cf..3b33faf08f3 100644
-- a/solr/core/src/java/org/apache/solr/search/ReturnFields.java
++ b/solr/core/src/java/org/apache/solr/search/ReturnFields.java
@@ -22,7 +22,6 @@ import org.apache.commons.io.FilenameUtils;
 import org.apache.lucene.queries.function.FunctionQuery;
 import org.apache.lucene.queries.function.ValueSource;
 import org.apache.lucene.queries.function.valuesource.QueryValueSource;
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.search.Query;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.params.CommonParams;
@@ -338,7 +337,7 @@ public class ReturnFields
           okFieldNames.add( funcStr );
           augmenters.addTransformer( new ValueSourceAugmenter( key, parser, vs ) );
         }
        catch (ParseException e) {
        catch (SyntaxError e) {
           // try again, simple rules for a field name with no whitespace
           sp.pos = start;
           field = sp.getSimpleString();
@@ -357,7 +356,7 @@ public class ReturnFields
        // end try as function
 
       } // end for(;;)
    } catch (ParseException e) {
    } catch (SyntaxError e) {
       throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Error parsing fieldname", e);
     }
   }
diff --git a/solr/core/src/java/org/apache/solr/search/SolrQueryParser.java b/solr/core/src/java/org/apache/solr/search/SolrQueryParser.java
index d8de2f694ea..f28262f5fff 100644
-- a/solr/core/src/java/org/apache/solr/search/SolrQueryParser.java
++ b/solr/core/src/java/org/apache/solr/search/SolrQueryParser.java
@@ -20,12 +20,9 @@ package org.apache.solr.search;
 import java.util.EnumSet;
 import java.util.HashMap;
 import java.util.Map;
import java.util.Map.Entry;
 
 import org.apache.lucene.analysis.util.TokenFilterFactory;
 import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
 import org.apache.lucene.search.*;
 import org.apache.lucene.util.ToStringUtils;
 import org.apache.lucene.util.automaton.Automaton;
@@ -36,6 +33,8 @@ import org.apache.lucene.analysis.Analyzer;
 import org.apache.solr.analysis.ReversedWildcardFilterFactory;
 import org.apache.solr.analysis.TokenizerChain;
 import org.apache.solr.common.SolrException;
import org.apache.solr.parser.ParseException;
import org.apache.solr.parser.QueryParser;
 import org.apache.solr.schema.FieldType;
 import org.apache.solr.schema.IndexSchema;
 import org.apache.solr.schema.SchemaField;
@@ -43,215 +42,12 @@ import org.apache.solr.schema.TextField;
 
 
 /**
 * A variation on the Lucene QueryParser which knows about the field 
 * types and query time analyzers configured in Solr's schema.xml.
 *
 * <p>
 * This class also deviates from the Lucene QueryParser by using 
 * ConstantScore versions of RangeQuery and PrefixQuery to prevent 
 * TooManyClauses exceptions.
 * </p> 
 *
 * <p>
 * If the magic field name "<code>_val_</code>" is used in a term or 
 * phrase query, the value is parsed as a function.
 * </p>
 * Solr's default query parser, a schema-driven superset of the classic lucene query parser.
  */
 public class SolrQueryParser extends QueryParser {
  protected final IndexSchema schema;
  protected final QParser parser;
  protected final String defaultField;

  /** 
   * Identifies the list of all known "magic fields" that trigger 
   * special parsing behavior
   */
  public static enum MagicFieldName {
    VAL("_val_", "func"), QUERY("_query_", null);
    
    public final String field;
    public final String subParser;
    MagicFieldName(final String field, final String subParser) {
      this.field = field;
      this.subParser = subParser;
    }
    public String toString() {
      return field;
    }
    private final static Map<String,MagicFieldName> lookup 
      = new HashMap<String,MagicFieldName>();
    static {
      for(MagicFieldName s : EnumSet.allOf(MagicFieldName.class))
        lookup.put(s.toString(), s);
    }
    public static MagicFieldName get(final String field) {
      return lookup.get(field);
    }
  }

  // implementation detail - caching ReversedWildcardFilterFactory based on type
  private Map<FieldType, ReversedWildcardFilterFactory> leadingWildcards;
 
   public SolrQueryParser(QParser parser, String defaultField) {
    this(parser, defaultField, parser.getReq().getSchema().getQueryAnalyzer());
  }

  public SolrQueryParser(QParser parser, String defaultField, Analyzer analyzer) {
    super(parser.getReq().getCore().getSolrConfig().luceneMatchVersion, defaultField, analyzer);
    this.schema = parser.getReq().getSchema();
    this.parser = parser;
    this.defaultField = defaultField;
    setEnablePositionIncrements(true);
    setLowercaseExpandedTerms(false);
    setAllowLeadingWildcard(true);
  }

  protected ReversedWildcardFilterFactory getReversedWildcardFilterFactory(FieldType fieldType) {
    if (leadingWildcards == null) leadingWildcards = new HashMap<FieldType, ReversedWildcardFilterFactory>();
    ReversedWildcardFilterFactory fac = leadingWildcards.get(fieldType);
    if (fac == null && leadingWildcards.containsKey(fac)) {
      return fac;
    }

    Analyzer a = fieldType.getAnalyzer();
    if (a instanceof TokenizerChain) {
      // examine the indexing analysis chain if it supports leading wildcards
      TokenizerChain tc = (TokenizerChain)a;
      TokenFilterFactory[] factories = tc.getTokenFilterFactories();
      for (TokenFilterFactory factory : factories) {
        if (factory instanceof ReversedWildcardFilterFactory) {
          fac = (ReversedWildcardFilterFactory)factory;
          break;
        }
      }
    }

    leadingWildcards.put(fieldType, fac);
    return fac;
  }

  
  private void checkNullField(String field) throws SolrException {
    if (field == null && defaultField == null) {
      throw new SolrException
        (SolrException.ErrorCode.BAD_REQUEST,
         "no field name specified in query and no default specified via 'df' param");
    }
  }

  protected String analyzeIfMultitermTermText(String field, String part, FieldType fieldType) {
    if (part == null) return part;

    SchemaField sf = schema.getFieldOrNull((field));
    if (sf == null || ! (fieldType instanceof TextField)) return part;
    String out = TextField.analyzeMultiTerm(field, part, ((TextField)fieldType).getMultiTermAnalyzer()).utf8ToString();
    // System.out.println("INPUT="+part + " OUTPUT="+out);
    return out;
  }

  @Override
  protected Query getFieldQuery(String field, String queryText, boolean quoted) throws ParseException {
    checkNullField(field);
    // intercept magic field name of "_" to use as a hook for our
    // own functions.
    if (field.charAt(0) == '_' && parser != null) {
      MagicFieldName magic = MagicFieldName.get(field);
      if (null != magic) {
        QParser nested = parser.subQuery(queryText, magic.subParser);
        return nested.getQuery();
      } 
    }
    SchemaField sf = schema.getFieldOrNull(field);
    if (sf != null) {
      FieldType ft = sf.getType();
      // delegate to type for everything except tokenized fields
      if (ft.isTokenized()) {
        return super.getFieldQuery(field, queryText, quoted || (ft instanceof TextField && ((TextField)ft).getAutoGeneratePhraseQueries()));
      } else {
        return sf.getType().getFieldQuery(parser, sf, queryText);
      }
    }

    // default to a normal field query
    return super.getFieldQuery(field, queryText, quoted);
  }

  @Override
  protected Query getRangeQuery(String field, String part1, String part2, boolean startInclusive, boolean endInclusive) throws ParseException {
    checkNullField(field);
    SchemaField sf = schema.getField(field);
    return sf.getType().getRangeQuery(parser, sf, part1, part2, startInclusive, endInclusive);
  }

  @Override
  protected Query getPrefixQuery(String field, String termStr) throws ParseException {
    checkNullField(field);

    termStr = analyzeIfMultitermTermText(field, termStr, schema.getFieldType(field));

    // Solr has always used constant scoring for prefix queries.  This should return constant scoring by default.
    return newPrefixQuery(new Term(field, termStr));
  }
  @Override
  protected Query getWildcardQuery(String field, String termStr) throws ParseException {
    checkNullField(field);
    // *:* -> MatchAllDocsQuery
    if ("*".equals(field) && "*".equals(termStr)) {
      return newMatchAllDocsQuery();
    }
    FieldType fieldType = schema.getFieldType(field);
    termStr = analyzeIfMultitermTermText(field, termStr, fieldType);
    // can we use reversed wildcards in this field?
    ReversedWildcardFilterFactory factory = getReversedWildcardFilterFactory(fieldType);
    if (factory != null) {
      Term term = new Term(field, termStr);
      // fsa representing the query
      Automaton automaton = WildcardQuery.toAutomaton(term);
      // TODO: we should likely use the automaton to calculate shouldReverse, too.
      if (factory.shouldReverse(termStr)) {
        automaton = BasicOperations.concatenate(automaton, BasicAutomata.makeChar(factory.getMarkerChar()));
        SpecialOperations.reverse(automaton);
      } else { 
        // reverse wildcardfilter is active: remove false positives
        // fsa representing false positives (markerChar*)
        Automaton falsePositives = BasicOperations.concatenate(
            BasicAutomata.makeChar(factory.getMarkerChar()), 
            BasicAutomata.makeAnyString());
        // subtract these away
        automaton = BasicOperations.minus(automaton, falsePositives);
      }
      return new AutomatonQuery(term, automaton) {
        // override toString so its completely transparent
        @Override
        public String toString(String field) {
          StringBuilder buffer = new StringBuilder();
          if (!getField().equals(field)) {
            buffer.append(getField());
            buffer.append(":");
          }
          buffer.append(term.text());
          buffer.append(ToStringUtils.boost(getBoost()));
          return buffer.toString();
        }
      };
    }

    // Solr has always used constant scoring for wildcard queries.  This should return constant scoring by default.
    return newWildcardQuery(new Term(field, termStr));
  }

  @Override
  protected Query getRegexpQuery(String field, String termStr) throws ParseException
  {
    termStr = analyzeIfMultitermTermText(field, termStr, schema.getFieldType(field));
    return newRegexpQuery(new Term(field, termStr));
  }

  @Override
  protected Query getFuzzyQuery(String field, String termStr, float minSimilarity) throws ParseException {
    termStr = analyzeIfMultitermTermText(field, termStr, schema.getFieldType(field));
    Term t = new Term(field, termStr);
    return newFuzzyQuery(t, minSimilarity, getFuzzyPrefixLength());
    super(parser.getReq().getCore().getSolrConfig().luceneMatchVersion, defaultField, parser);
   }
 
 }
diff --git a/solr/core/src/java/org/apache/solr/search/SpatialFilterQParser.java b/solr/core/src/java/org/apache/solr/search/SpatialFilterQParser.java
index 7c91cf8b1c2..b29db72ab6d 100644
-- a/solr/core/src/java/org/apache/solr/search/SpatialFilterQParser.java
++ b/solr/core/src/java/org/apache/solr/search/SpatialFilterQParser.java
@@ -17,7 +17,6 @@ package org.apache.solr.search;
  */
 
 
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.search.Query;
 import com.spatial4j.core.distance.DistanceUtils;
 import org.apache.solr.common.SolrException;
@@ -44,7 +43,7 @@ public class SpatialFilterQParser extends QParser {
   
 
   @Override
  public Query parse() throws ParseException {
  public Query parse() throws SyntaxError {
     //if more than one, we need to treat them as a point...
     //TODO: Should we accept multiple fields
     String[] fields = localParams.getParams("f");
diff --git a/solr/core/src/java/org/apache/solr/search/SurroundQParserPlugin.java b/solr/core/src/java/org/apache/solr/search/SurroundQParserPlugin.java
index f5dad7cb6a2..44f0fd4daca 100644
-- a/solr/core/src/java/org/apache/solr/search/SurroundQParserPlugin.java
++ b/solr/core/src/java/org/apache/solr/search/SurroundQParserPlugin.java
@@ -18,12 +18,10 @@ package org.apache.solr.search;
  */
 
 
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.search.Query;
 import org.apache.solr.common.params.CommonParams;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.SnapPuller;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.lucene.queryparser.surround.parser.*;
 import org.apache.lucene.queryparser.surround.query.*;
@@ -74,7 +72,7 @@ class SurroundQParser extends QParser {
 
   @Override
   public Query parse()
      throws org.apache.lucene.queryparser.classic.ParseException {
      throws SyntaxError {
     SrndQuery sq;
     String qstr = getString();
     if (qstr == null)
@@ -95,8 +93,7 @@ class SurroundQParser extends QParser {
       sq = org.apache.lucene.queryparser.surround.parser.QueryParser
           .parse(qstr);
     } catch (org.apache.lucene.queryparser.surround.parser.ParseException pe) {
      throw new org.apache.lucene.queryparser.classic.ParseException(
          pe.getMessage());
      throw new SyntaxError(pe);
     }
     
     // so what do we do with the SrndQuery ??
diff --git a/solr/core/src/java/org/apache/solr/search/SyntaxError.java b/solr/core/src/java/org/apache/solr/search/SyntaxError.java
new file mode 100644
index 00000000000..b26ca43f5d8
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/search/SyntaxError.java
@@ -0,0 +1,31 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.search;

/** Simple checked exception for parsing errors */
public class SyntaxError extends Exception {
  public SyntaxError(String msg) {
    super(msg);
  }
  public SyntaxError(String msg, Throwable cause) {
    super(msg, cause);
  }
  public SyntaxError(Throwable cause) {
    super(cause);
  }
}
diff --git a/solr/core/src/java/org/apache/solr/search/ValueSourceParser.java b/solr/core/src/java/org/apache/solr/search/ValueSourceParser.java
index 897dde8ee81..3a935f7166c 100755
-- a/solr/core/src/java/org/apache/solr/search/ValueSourceParser.java
++ b/solr/core/src/java/org/apache/solr/search/ValueSourceParser.java
@@ -26,7 +26,6 @@ import org.apache.lucene.queries.function.docvalues.BoolDocValues;
 import org.apache.lucene.queries.function.docvalues.DoubleDocValues;
 import org.apache.lucene.queries.function.docvalues.LongDocValues;
 import org.apache.lucene.queries.function.valuesource.*;
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.SortField;
@@ -61,7 +60,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
   /**
    * Parse the user input into a ValueSource.
    */
  public abstract ValueSource parse(FunctionQParser fp) throws ParseException;
  public abstract ValueSource parse(FunctionQParser fp) throws SyntaxError;
 
   /* standard functions */
   public static Map<String, ValueSourceParser> standardValueSourceParsers = new HashMap<String, ValueSourceParser>();
@@ -87,33 +86,33 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
   static {
     addParser("testfunc", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         final ValueSource source = fp.parseValueSource();
         return new TestValueSource(source);
       }
     });
     addParser("ord", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         String field = fp.parseId();
         return new OrdFieldSource(field);
       }
     });
     addParser("literal", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         return new LiteralValueSource(fp.parseArg());
       }
     });
     addParser("threadid", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         return new LongConstValueSource(Thread.currentThread().getId());
       }
     });
     addParser("sleep", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         int ms = fp.parseInt();
         ValueSource source = fp.parseValueSource();
         try {
@@ -126,14 +125,14 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
     });
     addParser("rord", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         String field = fp.parseId();
         return new ReverseOrdFieldSource(field);
       }
     });
     addParser("top", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         // top(vs) is now a no-op
         ValueSource source = fp.parseValueSource();
         return source;
@@ -141,7 +140,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
     });
     addParser("linear", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         ValueSource source = fp.parseValueSource();
         float slope = fp.parseFloat();
         float intercept = fp.parseFloat();
@@ -150,7 +149,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
     });
     addParser("recip", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         ValueSource source = fp.parseValueSource();
         float m = fp.parseFloat();
         float a = fp.parseFloat();
@@ -160,7 +159,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
     });
     addParser("scale", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         ValueSource source = fp.parseValueSource();
         float min = fp.parseFloat();
         float max = fp.parseFloat();
@@ -169,7 +168,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
     });
     addParser("div", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         ValueSource a = fp.parseValueSource();
         ValueSource b = fp.parseValueSource();
         return new DivFloatFunction(a, b);
@@ -177,7 +176,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
     });
     addParser("mod", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         ValueSource a = fp.parseValueSource();
         ValueSource b = fp.parseValueSource();
         return new DualFloatFunction(a, b) {
@@ -194,7 +193,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
     });
     addParser("map", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         ValueSource source = fp.parseValueSource();
         float min = fp.parseFloat();
         float max = fp.parseFloat();
@@ -206,7 +205,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
 
     addParser("abs", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         ValueSource source = fp.parseValueSource();
         return new SimpleFloatFunction(source) {
           @Override
@@ -223,7 +222,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
     });
     addParser("sum", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         List<ValueSource> sources = fp.parseValueSourceList();
         return new SumFloatFunction(sources.toArray(new ValueSource[sources.size()]));
       }
@@ -232,7 +231,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
 
     addParser("product", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         List<ValueSource> sources = fp.parseValueSourceList();
         return new ProductFloatFunction(sources.toArray(new ValueSource[sources.size()]));
       }
@@ -241,7 +240,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
 
     addParser("sub", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         ValueSource a = fp.parseValueSource();
         ValueSource b = fp.parseValueSource();
         return new DualFloatFunction(a, b) {
@@ -259,14 +258,14 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
     });
     addParser("vector", new ValueSourceParser(){
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException{
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         return new VectorValueSource(fp.parseValueSourceList());
       }
     });
     addParser("query", new ValueSourceParser() {
       // boost(query($q),rating)
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         Query q = fp.parseNestedQuery();
         float defVal = 0.0f;
         if (fp.hasMoreArguments()) {
@@ -277,7 +276,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
     });
     addParser("boost", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         Query q = fp.parseNestedQuery();
         ValueSource vs = fp.parseValueSource();
         BoostedQuery bq = new BoostedQuery(q, vs);
@@ -286,7 +285,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
     });
     addParser("joindf", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         String f0 = fp.parseArg();
         String qf = fp.parseArg();
         return new JoinDocFreqValueSource( f0, qf );
@@ -297,7 +296,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
 
     addParser("hsin", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
 
         double radius = fp.parseDouble();
         //SOLR-2114, make the convert flag required, since the parser doesn't support much in the way of lookahead or the ability to convert a String into a ValueSource
@@ -338,7 +337,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
 
     addParser("ghhsin", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         double radius = fp.parseDouble();
 
         ValueSource gh1 = fp.parseValueSource();
@@ -350,7 +349,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
 
     addParser("geohash", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
 
         ValueSource lat = fp.parseValueSource();
         ValueSource lon = fp.parseValueSource();
@@ -360,7 +359,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
     });
     addParser("strdist", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
 
         ValueSource str1 = fp.parseValueSource();
         ValueSource str2 = fp.parseValueSource();
@@ -385,7 +384,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
     });
     addParser("field", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
 
         String fieldName = fp.parseArg();
         SchemaField f = fp.getReq().getSchema().getField(fieldName);
@@ -527,21 +526,21 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
     });
     addParser("max", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         List<ValueSource> sources = fp.parseValueSourceList();
         return new MaxFloatFunction(sources.toArray(new ValueSource[sources.size()]));
       }
     });
     addParser("min", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         List<ValueSource> sources = fp.parseValueSourceList();
         return new MinFloatFunction(sources.toArray(new ValueSource[sources.size()]));
       }
     });
     addParser("sqedist", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         List<ValueSource> sources = fp.parseValueSourceList();
         MVResult mvr = getMultiValueSources(sources);
 
@@ -551,7 +550,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
 
     addParser("dist", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         float power = fp.parseFloat();
         List<ValueSource> sources = fp.parseValueSourceList();
         MVResult mvr = getMultiValueSources(sources);
@@ -577,7 +576,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
 
     addParser("docfreq", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         TInfo tinfo = parseTerm(fp);
         return new DocFreqValueSource(tinfo.field, tinfo.val, tinfo.indexedField, tinfo.indexedBytes);
       }
@@ -585,7 +584,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
 
     addParser("totaltermfreq", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         TInfo tinfo = parseTerm(fp);
         return new TotalTermFreqValueSource(tinfo.field, tinfo.val, tinfo.indexedField, tinfo.indexedBytes);
       }
@@ -594,7 +593,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
 
     addParser("sumtotaltermfreq", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         String field = fp.parseArg();
         return new SumTotalTermFreqValueSource(field);
       }
@@ -603,7 +602,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
 
     addParser("idf", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         TInfo tinfo = parseTerm(fp);
         return new IDFValueSource(tinfo.field, tinfo.val, tinfo.indexedField, tinfo.indexedBytes);
       }
@@ -611,7 +610,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
 
     addParser("termfreq", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         TInfo tinfo = parseTerm(fp);
         return new TermFreqValueSource(tinfo.field, tinfo.val, tinfo.indexedField, tinfo.indexedBytes);
       }
@@ -619,7 +618,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
 
     addParser("tf", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         TInfo tinfo = parseTerm(fp);
         return new TFValueSource(tinfo.field, tinfo.val, tinfo.indexedField, tinfo.indexedBytes);
       }
@@ -627,7 +626,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
 
     addParser("norm", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         String field = fp.parseArg();
         return new NormValueSource(field);
       }
@@ -663,7 +662,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
 
     addParser("exists", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         ValueSource vs = fp.parseValueSource();
         return new SimpleBoolFunction(vs) {
           @Override
@@ -680,7 +679,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
 
     addParser("not", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         ValueSource vs = fp.parseValueSource();
         return new SimpleBoolFunction(vs) {
           @Override
@@ -698,7 +697,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
 
     addParser("and", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         List<ValueSource> sources = fp.parseValueSourceList();
         return new MultiBoolFunction(sources) {
           @Override
@@ -717,7 +716,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
 
     addParser("or", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         List<ValueSource> sources = fp.parseValueSourceList();
         return new MultiBoolFunction(sources) {
           @Override
@@ -736,7 +735,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
 
     addParser("xor", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         List<ValueSource> sources = fp.parseValueSourceList();
         return new MultiBoolFunction(sources) {
           @Override
@@ -758,7 +757,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
 
     addParser("if", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         ValueSource ifValueSource = fp.parseValueSource();
         ValueSource trueValueSource = fp.parseValueSource();
         ValueSource falseValueSource = fp.parseValueSource();
@@ -769,14 +768,14 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
 
     addParser("def", new ValueSourceParser() {
       @Override
      public ValueSource parse(FunctionQParser fp) throws ParseException {
      public ValueSource parse(FunctionQParser fp) throws SyntaxError {
         return new DefFunction(fp.parseValueSourceList());
       }
     });
 
   }
 
  private static TInfo parseTerm(FunctionQParser fp) throws ParseException {
  private static TInfo parseTerm(FunctionQParser fp) throws SyntaxError {
     TInfo tinfo = new TInfo();
 
     tinfo.indexedField = tinfo.field = fp.parseArg();
@@ -888,7 +887,7 @@ class DateValueSourceParser extends ValueSourceParser {
   }
 
   @Override
  public ValueSource parse(FunctionQParser fp) throws ParseException {
  public ValueSource parse(FunctionQParser fp) throws SyntaxError {
     String first = fp.parseArg();
     String second = fp.parseArg();
     if (first == null) first = "NOW";
@@ -1079,7 +1078,7 @@ abstract class DoubleParser extends NamedParser {
   public abstract double func(int doc, FunctionValues vals);
 
   @Override
  public ValueSource parse(FunctionQParser fp) throws ParseException {
  public ValueSource parse(FunctionQParser fp) throws SyntaxError {
     return new Function(fp.parseValueSource());
   }
 
@@ -1119,7 +1118,7 @@ abstract class Double2Parser extends NamedParser {
   public abstract double func(int doc, FunctionValues a, FunctionValues b);
 
   @Override
  public ValueSource parse(FunctionQParser fp) throws ParseException {
  public ValueSource parse(FunctionQParser fp) throws SyntaxError {
     return new Function(fp.parseValueSource(), fp.parseValueSource());
   }
 
diff --git a/solr/core/src/java/org/apache/solr/search/function/distance/HaversineConstFunction.java b/solr/core/src/java/org/apache/solr/search/function/distance/HaversineConstFunction.java
index 0876450f4b1..b9efc028ede 100755
-- a/solr/core/src/java/org/apache/solr/search/function/distance/HaversineConstFunction.java
++ b/solr/core/src/java/org/apache/solr/search/function/distance/HaversineConstFunction.java
@@ -24,7 +24,6 @@ import org.apache.lucene.queries.function.valuesource.ConstNumberSource;
 import org.apache.lucene.queries.function.valuesource.DoubleConstValueSource;
 import org.apache.lucene.queries.function.valuesource.MultiValueSource;
 import org.apache.lucene.queries.function.valuesource.VectorValueSource;
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.search.IndexSearcher;
 import com.spatial4j.core.io.ParseUtils;
 import com.spatial4j.core.distance.DistanceUtils;
@@ -32,6 +31,7 @@ import com.spatial4j.core.exception.InvalidShapeException;
 import org.apache.solr.common.params.SpatialParams;
 import org.apache.solr.schema.SchemaField;
 import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.SyntaxError;
 import org.apache.solr.search.ValueSourceParser;
 
 import java.io.IOException;
@@ -48,7 +48,7 @@ public class HaversineConstFunction extends ValueSource {
 
   public static ValueSourceParser parser = new ValueSourceParser() {
     @Override
    public ValueSource parse(FunctionQParser fp) throws ParseException
    public ValueSource parse(FunctionQParser fp) throws SyntaxError
     {
       // TODO: dispatch through SpatialQueryable in the future?
       List<ValueSource> sources = fp.parseValueSourceList();
@@ -65,7 +65,7 @@ public class HaversineConstFunction extends ValueSource {
       } else if (sources.size() == 1) {
         ValueSource vs = sources.get(0);
         if (!(vs instanceof MultiValueSource)) {
          throw new ParseException("geodist - invalid parameters:" + sources);
          throw new SyntaxError("geodist - invalid parameters:" + sources);
         }
         mv1 = (MultiValueSource)vs;
       } else if (sources.size() == 2) {
@@ -88,7 +88,7 @@ public class HaversineConstFunction extends ValueSource {
           mv1 = makeMV(sources.subList(0,2), sources);
           vs1 = sources.get(2);
           if (!(vs1 instanceof MultiValueSource)) {
            throw new ParseException("geodist - invalid parameters:" + sources);
            throw new SyntaxError("geodist - invalid parameters:" + sources);
           }
           mv2 = (MultiValueSource)vs1;
         }
@@ -96,7 +96,7 @@ public class HaversineConstFunction extends ValueSource {
         mv1 = makeMV(sources.subList(0,2), sources);
         mv2 = makeMV(sources.subList(2,4), sources);
       } else if (sources.size() > 4) {
        throw new ParseException("geodist - invalid parameters:" + sources);
        throw new SyntaxError("geodist - invalid parameters:" + sources);
       }
 
       if (mv1 == null) {
@@ -109,7 +109,7 @@ public class HaversineConstFunction extends ValueSource {
       }
 
       if (mv1 == null || mv2 == null) {
        throw new ParseException("geodist - not enough parameters:" + sources);
        throw new SyntaxError("geodist - not enough parameters:" + sources);
       }
 
       // We have all the parameters at this point, now check if one of the points is constant
@@ -130,24 +130,24 @@ public class HaversineConstFunction extends ValueSource {
   };
 
   /** make a MultiValueSource from two non MultiValueSources */
  private static VectorValueSource makeMV(List<ValueSource> sources, List<ValueSource> orig) throws ParseException {
  private static VectorValueSource makeMV(List<ValueSource> sources, List<ValueSource> orig) throws SyntaxError {
     ValueSource vs1 = sources.get(0);
     ValueSource vs2 = sources.get(1);
 
     if (vs1 instanceof MultiValueSource || vs2 instanceof MultiValueSource) {
      throw new ParseException("geodist - invalid parameters:" + orig);
      throw new SyntaxError("geodist - invalid parameters:" + orig);
     }
     return  new VectorValueSource(sources);
   }
 
  private static MultiValueSource parsePoint(FunctionQParser fp) throws ParseException {
  private static MultiValueSource parsePoint(FunctionQParser fp) throws SyntaxError {
     String pt = fp.getParam(SpatialParams.POINT);
     if (pt == null) return null;
     double[] point = null;
     try {
       point = ParseUtils.parseLatitudeLongitude(pt);
     } catch (InvalidShapeException e) {
      throw new ParseException("Bad spatial pt:" + pt);
      throw new SyntaxError("Bad spatial pt:" + pt);
     }
     return new VectorValueSource(Arrays.<ValueSource>asList(new DoubleConstValueSource(point[0]),new DoubleConstValueSource(point[1])));
   }
@@ -161,13 +161,13 @@ public class HaversineConstFunction extends ValueSource {
     return null;
   }
 
  private static MultiValueSource parseSfield(FunctionQParser fp) throws ParseException {
  private static MultiValueSource parseSfield(FunctionQParser fp) throws SyntaxError {
     String sfield = fp.getParam(SpatialParams.FIELD);
     if (sfield == null) return null;
     SchemaField sf = fp.getReq().getSchema().getField(sfield);
     ValueSource vs = sf.getType().getValueSource(sf, fp);
     if (!(vs instanceof MultiValueSource)) {
      throw new ParseException("Spatial field must implement MultiValueSource:" + sf);
      throw new SyntaxError("Spatial field must implement MultiValueSource:" + sf);
     }
     return (MultiValueSource)vs;
   }
diff --git a/solr/core/src/java/org/apache/solr/search/grouping/distributed/command/QueryCommand.java b/solr/core/src/java/org/apache/solr/search/grouping/distributed/command/QueryCommand.java
index 363b8b87ed4..3262e7ef6e7 100644
-- a/solr/core/src/java/org/apache/solr/search/grouping/distributed/command/QueryCommand.java
++ b/solr/core/src/java/org/apache/solr/search/grouping/distributed/command/QueryCommand.java
@@ -17,12 +17,12 @@ package org.apache.solr.search.grouping.distributed.command;
  * limitations under the License.
  */
 
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.search.*;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.search.DocSet;
 import org.apache.solr.search.QParser;
 import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.SyntaxError;
 import org.apache.solr.search.grouping.Command;
 import org.apache.solr.search.grouping.collector.FilterCollector;
 
@@ -61,9 +61,8 @@ public class QueryCommand implements Command<QueryCommandResult> {
      * @param groupQueryString The group query string to parse
      * @param request The current request
      * @return this
     * @throws ParseException If parsing the groupQueryString failed
      */
    public Builder setQuery(String groupQueryString, SolrQueryRequest request) throws ParseException {
    public Builder setQuery(String groupQueryString, SolrQueryRequest request) throws SyntaxError {
       QParser parser = QParser.getParser(groupQueryString, null, request);
       this.queryString = groupQueryString;
       return setQuery(parser.getQuery());
diff --git a/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java b/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
index 5589e052b6c..98202a99306 100644
-- a/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
++ b/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
@@ -37,7 +37,6 @@ import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanClause.Occur;
 import org.apache.lucene.search.BooleanQuery;
@@ -57,6 +56,7 @@ import org.apache.solr.response.SolrQueryResponse;
 import org.apache.solr.schema.SchemaField;
 import org.apache.solr.search.FunctionRangeQuery;
 import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;
 import org.apache.solr.search.QueryUtils;
 import org.apache.solr.search.SolrIndexSearcher;
 import org.apache.solr.search.function.ValueSourceRangeFilter;
@@ -317,7 +317,7 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
 
       return q;
 
    } catch (ParseException e) {
    } catch (SyntaxError e) {
       throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, e);
     }
   }
diff --git a/solr/core/src/java/org/apache/solr/util/SolrPluginUtils.java b/solr/core/src/java/org/apache/solr/util/SolrPluginUtils.java
index 96aa5e23118..ad02cf95767 100644
-- a/solr/core/src/java/org/apache/solr/util/SolrPluginUtils.java
++ b/solr/core/src/java/org/apache/solr/util/SolrPluginUtils.java
@@ -17,12 +17,8 @@
 
 package org.apache.solr.util;
 
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
 import org.apache.lucene.index.StorableField;
 import org.apache.lucene.index.StoredDocument;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
 import org.apache.lucene.search.*;
 import org.apache.lucene.search.BooleanClause.Occur;
 import org.apache.solr.common.SolrDocument;
@@ -30,7 +26,6 @@ import org.apache.solr.common.SolrDocumentList;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.params.CommonParams;
 import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.params.UpdateParams;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.common.util.SimpleOrderedMap;
 import org.apache.solr.common.util.StrUtils;
@@ -38,13 +33,12 @@ import org.apache.solr.core.SolrCore;
 import org.apache.solr.handler.component.HighlightComponent;
 import org.apache.solr.handler.component.ResponseBuilder;
 import org.apache.solr.highlight.SolrHighlighter;
import org.apache.solr.parser.QueryParser;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.response.SolrQueryResponse;
 import org.apache.solr.schema.IndexSchema;
 import org.apache.solr.schema.SchemaField;
 import org.apache.solr.search.*;
import org.apache.solr.update.DocumentBuilder;
import org.slf4j.Logger;
 
 import java.io.IOException;
 import java.util.*;
@@ -386,7 +380,7 @@ public class SolrPluginUtils {
 
     DocList results = req.getSearcher().getDocList(query,(DocSet)null, sort, start, limit);
     return results;
    } catch (ParseException e) {
    } catch (SyntaxError e) {
       throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Error parsing query: " + qs);
     }
 
@@ -604,8 +598,6 @@ public class SolrPluginUtils {
 
   /**
    * Escapes all special characters except '"', '-', and '+'
   *
   * @see QueryParser#escape
    */
   public static CharSequence partialEscape(CharSequence s) {
     StringBuilder sb = new StringBuilder();
@@ -726,7 +718,7 @@ public class SolrPluginUtils {
      */
     @Override
     protected Query getFieldQuery(String field, String queryText, boolean quoted)
      throws ParseException {
      throws SyntaxError {
 
       if (aliases.containsKey(field)) {
 
@@ -798,7 +790,7 @@ public class SolrPluginUtils {
    * @return null if no queries are generated
    */
   public static List<Query> parseQueryStrings(SolrQueryRequest req,
                                              String[] queries) throws ParseException {
                                              String[] queries) throws SyntaxError {
     if (null == queries || 0 == queries.length) return null;
     List<Query> out = new ArrayList<Query>(queries.length);
     for (String q : queries) {
diff --git a/solr/core/src/test/org/apache/solr/core/DummyValueSourceParser.java b/solr/core/src/test/org/apache/solr/core/DummyValueSourceParser.java
index 4351f0dc674..068e045a23d 100644
-- a/solr/core/src/test/org/apache/solr/core/DummyValueSourceParser.java
++ b/solr/core/src/test/org/apache/solr/core/DummyValueSourceParser.java
@@ -19,9 +19,9 @@ package org.apache.solr.core;
 import org.apache.lucene.queries.function.FunctionValues;
 import org.apache.lucene.queries.function.ValueSource;
 import org.apache.lucene.queries.function.valuesource.SimpleFloatFunction;
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.SyntaxError;
 import org.apache.solr.search.ValueSourceParser;
 
 
@@ -38,7 +38,7 @@ public class DummyValueSourceParser extends ValueSourceParser {
   }
 
   @Override
  public ValueSource parse(FunctionQParser fp) throws ParseException {
  public ValueSource parse(FunctionQParser fp) throws SyntaxError {
     ValueSource source = fp.parseValueSource();
     ValueSource result = new SimpleFloatFunction(source) {
       @Override
diff --git a/solr/core/src/test/org/apache/solr/core/TestArbitraryIndexDir.java b/solr/core/src/test/org/apache/solr/core/TestArbitraryIndexDir.java
index 492130c8dc2..086a14316ba 100644
-- a/solr/core/src/test/org/apache/solr/core/TestArbitraryIndexDir.java
++ b/solr/core/src/test/org/apache/solr/core/TestArbitraryIndexDir.java
@@ -29,7 +29,6 @@ import org.apache.lucene.document.Field;
 import org.apache.lucene.document.TextField;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.util.Version;
 import org.apache.solr.common.SolrException;
@@ -86,7 +85,7 @@ public class TestArbitraryIndexDir extends AbstractSolrTestCase{
   }
 
   @Test
  public void testLoadNewIndexDir() throws IOException, ParserConfigurationException, SAXException, ParseException {
  public void testLoadNewIndexDir() throws IOException, ParserConfigurationException, SAXException {
     //add a doc in original index dir
     assertU(adoc("id", String.valueOf(1),
         "name", "name"+String.valueOf(1)));
diff --git a/solr/core/src/test/org/apache/solr/search/FooQParserPlugin.java b/solr/core/src/test/org/apache/solr/search/FooQParserPlugin.java
index bad1156d724..b6168244c86 100755
-- a/solr/core/src/test/org/apache/solr/search/FooQParserPlugin.java
++ b/solr/core/src/test/org/apache/solr/search/FooQParserPlugin.java
@@ -17,7 +17,6 @@
 
 package org.apache.solr.search;
 
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.request.SolrQueryRequest;
@@ -42,7 +41,7 @@ class FooQParser extends QParser {
   }
 
   @Override
  public Query parse() throws ParseException {
  public Query parse() throws SyntaxError {
     return new TermQuery(new Term(localParams.get(QueryParsing.F), localParams.get(QueryParsing.V)));
   }
 }
diff --git a/solr/core/src/test/org/apache/solr/search/TestExtendedDismaxParser.java b/solr/core/src/test/org/apache/solr/search/TestExtendedDismaxParser.java
index 0c97d86d69c..d51d6dc9d70 100755
-- a/solr/core/src/test/org/apache/solr/search/TestExtendedDismaxParser.java
++ b/solr/core/src/test/org/apache/solr/search/TestExtendedDismaxParser.java
@@ -772,7 +772,7 @@ public class TestExtendedDismaxParser extends AbstractSolrTestCase {
             "defType", "edismax")
         , "*[count(//doc)=1]");
     
    // Query string field 'cat_s' for special char / - causes ParseException without patch SOLR-3467
    // Query string field 'cat_s' for special char / - causes SyntaxError without patch SOLR-3467
     assertQ("Escaping string with reserved / character",
         req("q", "foo/",
             "qf", "cat_s",
diff --git a/solr/core/src/test/org/apache/solr/search/TestSolrQueryParser.java b/solr/core/src/test/org/apache/solr/search/TestSolrQueryParser.java
index f65333ae6ed..cef61461783 100644
-- a/solr/core/src/test/org/apache/solr/search/TestSolrQueryParser.java
++ b/solr/core/src/test/org/apache/solr/search/TestSolrQueryParser.java
@@ -35,6 +35,12 @@ public class TestSolrQueryParser extends SolrTestCaseJ4 {
     assertU(adoc("id","1", "text",v,  "text_np",v));
     v="now cow";
     assertU(adoc("id","2", "text",v,  "text_np",v));
    assertU(adoc("id","3", "foo_s","a ' \" \\ {! ) } ( { z"));  // A value filled with special chars

    assertU(adoc("id","10", "qqq_s","X"));
    assertU(adoc("id","11", "www_s","X"));
    assertU(adoc("id","12", "eee_s","X"));

     assertU(commit());
   }
 
@@ -49,4 +55,35 @@ public class TestSolrQueryParser extends SolrTestCaseJ4 {
         ,"//*[@numFound='2']"
     );
   }

  @Test
  public void testLocalParamsInQP() throws Exception {
    assertJQ(req("q","qaz {!term f=text v=$qq} wsx", "qq","now")
        ,"/response/numFound==2"
    );

    assertJQ(req("q","qaz {!term f=text v=$qq} wsx", "qq","nomatch")
        ,"/response/numFound==0"
    );

    assertJQ(req("q","qaz {!term f=text}now wsx", "qq","now")
        ,"/response/numFound==2"
    );

    assertJQ(req("q","qaz {!term f=foo_s v='a \\' \" \\\\ {! ) } ( { z'} wsx")           // single quote escaping
        ,"/response/numFound==1"
    );

    assertJQ(req("q","qaz {!term f=foo_s v=\"a ' \\\" \\\\ {! ) } ( { z\"} wsx")         // double quote escaping
        ,"/response/numFound==1"
    );

    // double-join to test back-to-back local params
    assertJQ(req("q","qaz {!join from=www_s to=eee_s}{!join from=qqq_s to=www_s}id:10" )
        ,"/response/docs/[0]/id=='12'"
    );


  }

 }
diff --git a/solr/core/src/test/org/apache/solr/search/TestValueSourceCache.java b/solr/core/src/test/org/apache/solr/search/TestValueSourceCache.java
index d9e35a1fabb..b6da59185c1 100644
-- a/solr/core/src/test/org/apache/solr/search/TestValueSourceCache.java
++ b/solr/core/src/test/org/apache/solr/search/TestValueSourceCache.java
@@ -17,7 +17,6 @@ package org.apache.solr.search;
  * limitations under the License.
  */
 
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.QueryUtils;
 import org.apache.solr.SolrTestCaseJ4;
@@ -41,14 +40,14 @@ public class TestValueSourceCache extends SolrTestCaseJ4 {
     _func = null;
   }
 
  Query getQuery(String query) throws ParseException {
  Query getQuery(String query) throws SyntaxError {
     _func.setString(query);
     return _func.parse();
   }
 
   // This is actually also tested by the tests for val_d1 below, but the bug was reported against geodist()...
   @Test
  public void testGeodistSource() throws ParseException {
  public void testGeodistSource() throws SyntaxError {
     Query q_home = getQuery("geodist(home_ll, 45.0, 43.0)");
     Query q_work = getQuery("geodist(work_ll, 45.0, 43.0)");
     Query q_home2 = getQuery("geodist(home_ll, 45.0, 43.0)");
@@ -57,7 +56,7 @@ public class TestValueSourceCache extends SolrTestCaseJ4 {
   }
 
   @Test
  public void testNumerics() throws ParseException {
  public void testNumerics() throws SyntaxError {
     String[] templates = new String[]{
         "sum(#v0, #n0)",
         "product(pow(#v0,#n0),#v1,#n1)",
@@ -94,7 +93,7 @@ public class TestValueSourceCache extends SolrTestCaseJ4 {
 
   // This test should will fail because q1 and q3 evaluate as equal unless
   // fixes for bug 2829 are in place.
  void tryQuerySameTypes(String template, String numbers, String type) throws ParseException {
  void tryQuerySameTypes(String template, String numbers, String type) throws SyntaxError {
     String s1 = template;
     String s2 = template;
     String s3 = template;
@@ -120,7 +119,7 @@ public class TestValueSourceCache extends SolrTestCaseJ4 {
 
   // These should always and forever fail, and would have failed without the fixes for 2829, but why not make
   // some more tests just in case???
  void tryQueryDiffTypes(String template, String numbers, String[] types) throws ParseException {
  void tryQueryDiffTypes(String template, String numbers, String[] types) throws SyntaxError {
     String s1 = template;
     String s2 = template;
 
diff --git a/solr/core/src/test/org/apache/solr/search/function/NvlValueSourceParser.java b/solr/core/src/test/org/apache/solr/search/function/NvlValueSourceParser.java
index 49a240afe88..f56ad15edec 100755
-- a/solr/core/src/test/org/apache/solr/search/function/NvlValueSourceParser.java
++ b/solr/core/src/test/org/apache/solr/search/function/NvlValueSourceParser.java
@@ -20,9 +20,9 @@ package org.apache.solr.search.function;
 import org.apache.lucene.queries.function.FunctionValues;
 import org.apache.lucene.queries.function.ValueSource;
 import org.apache.lucene.queries.function.valuesource.SimpleFloatFunction;
import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.SyntaxError;
 import org.apache.solr.search.ValueSourceParser;
 
 /**
@@ -46,7 +46,7 @@ public class NvlValueSourceParser extends ValueSourceParser {
     private float nvlFloatValue = 0.0f;
 
     @Override
    public ValueSource parse(FunctionQParser fp) throws ParseException {
    public ValueSource parse(FunctionQParser fp) throws SyntaxError {
       ValueSource source = fp.parseValueSource();
       final float nvl = fp.parseFloat();
 
diff --git a/solr/core/src/test/org/apache/solr/util/DateMathParserTest.java b/solr/core/src/test/org/apache/solr/util/DateMathParserTest.java
index 83055810b1d..715055bb600 100644
-- a/solr/core/src/test/org/apache/solr/util/DateMathParserTest.java
++ b/solr/core/src/test/org/apache/solr/util/DateMathParserTest.java
@@ -329,7 +329,7 @@ public class DateMathParserTest extends LuceneTestCase {
     for (String command : badCommands.keySet()) {
       try {
         Date out = p.parseMath(command);
        fail("Didn't generate ParseException for: " + command);
        fail("Didn't generate SyntaxError for: " + command);
       } catch (ParseException e) {
         assertEquals("Wrong pos for: " + command + " => " + e.getMessage(),
                      badCommands.get(command).intValue(), e.getErrorOffset());
- 
2.19.1.windows.1

