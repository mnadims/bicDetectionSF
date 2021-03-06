From bfedaea2dd0b54f1af6d621a26264fe9d70dfabe Mon Sep 17 00:00:00 2001
From: Grant Ingersoll <gsingers@apache.org>
Date: Fri, 11 Dec 2009 21:36:41 +0000
Subject: [PATCH] SOLR-1297: establish some standalone, baseline tests for
 parsing the sort strings

git-svn-id: https://svn.apache.org/repos/asf/lucene/solr/trunk@889825 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/solr/search/QueryParsingTest.java  | 104 ++++++++++++++++++
 1 file changed, 104 insertions(+)
 create mode 100644 src/test/org/apache/solr/search/QueryParsingTest.java

diff --git a/src/test/org/apache/solr/search/QueryParsingTest.java b/src/test/org/apache/solr/search/QueryParsingTest.java
new file mode 100644
index 00000000000..1ba6970d5b4
-- /dev/null
++ b/src/test/org/apache/solr/search/QueryParsingTest.java
@@ -0,0 +1,104 @@
package org.apache.solr.search;
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

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.solr.common.SolrException;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.util.AbstractSolrTestCase;


/**
 *
 *
 **/
public class QueryParsingTest extends AbstractSolrTestCase {
  public String getSchemaFile() {
    return "schema.xml";
  }

  public String getSolrConfigFile() {
    return "solrconfig.xml";
  }


  public void testSort() throws Exception {
    Sort sort;

    IndexSchema schema = h.getCore().getSchema();
    sort = QueryParsing.parseSort("score desc", schema);
    assertNull("sort", sort);//only 1 thing in the list, no Sort specified
    sort = QueryParsing.parseSort("weight desc", schema);
    SortField[] flds = sort.getSort();
    assertEquals(flds[0].getType(), SortField.FLOAT);
    assertEquals(flds[0].getField(), "weight");
    assertEquals(flds[0].getReverse(), true);
    sort = QueryParsing.parseSort("weight desc,bday asc", schema);
    flds = sort.getSort();
    assertEquals(flds[0].getType(), SortField.FLOAT);
    assertEquals(flds[0].getField(), "weight");
    assertEquals(flds[0].getReverse(), true);
    assertEquals(flds[1].getType(), SortField.LONG);
    assertEquals(flds[1].getField(), "bday");
    assertEquals(flds[1].getReverse(), false);
    //order aliases
    sort = QueryParsing.parseSort("weight top,bday asc", schema);
    flds = sort.getSort();
    assertEquals(flds[0].getType(), SortField.FLOAT);
    assertEquals(flds[0].getField(), "weight");
    assertEquals(flds[0].getReverse(), true);
    assertEquals(flds[1].getType(), SortField.LONG);
    assertEquals(flds[1].getField(), "bday");
    assertEquals(flds[1].getReverse(), false);
    sort = QueryParsing.parseSort("weight top,bday bottom", schema);
    flds = sort.getSort();
    assertEquals(flds[0].getType(), SortField.FLOAT);
    assertEquals(flds[0].getField(), "weight");
    assertEquals(flds[0].getReverse(), true);
    assertEquals(flds[1].getType(), SortField.LONG);
    assertEquals(flds[1].getField(), "bday");
    assertEquals(flds[1].getReverse(), false);

    //test weird spacing
    sort = QueryParsing.parseSort("weight         desc,            bday         asc", schema);
    flds = sort.getSort();
    assertEquals(flds[0].getType(), SortField.FLOAT);
    assertEquals(flds[0].getField(), "weight");
    assertEquals(flds[1].getType(), SortField.LONG);
    assertEquals(flds[1].getField(), "bday");
    //handles trailing commas
    sort = QueryParsing.parseSort("weight desc,", schema);
    flds = sort.getSort();
    assertEquals(flds[0].getType(), SortField.FLOAT);
    assertEquals(flds[0].getField(), "weight");
    //test some bad vals
    try {
      sort = QueryParsing.parseSort("weight, desc", schema);
      assertTrue(false);
    } catch (SolrException e) {
      //expected
    }
    try {
      sort = QueryParsing.parseSort("weight desc, bday", schema);
      assertTrue(false);
    } catch (SolrException e) {
    }

  }

}
- 
2.19.1.windows.1

