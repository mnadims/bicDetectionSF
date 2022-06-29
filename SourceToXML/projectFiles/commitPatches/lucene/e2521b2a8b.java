From e2521b2a8baabdaf43b92192588f51e042d21e97 Mon Sep 17 00:00:00 2001
From: Steve Rowe <sarowe@apache.org>
Date: Fri, 28 Apr 2017 15:36:50 -0400
Subject: [PATCH] SOLR-9596: Add Solr support for SimpleTextCodec, via
 <codecFactory class=solr.SimpleTextCodecFactory/> in solrconfig.xml
 (per-field specification in the schema is not possible).

--
 .../idea/solr/core/src/java/solr-core.iml     |  1 +
 solr/CHANGES.txt                              |  3 +
 .../solr/core/SimpleTextCodecFactory.java     | 38 +++++++++++
 .../conf/schema-SimpleTextCodec.xml           | 32 +++++++++
 .../conf/solrconfig_SimpleTextCodec.xml       | 26 +++++++
 .../apache/solr/core/TestSimpleTextCodec.java | 67 +++++++++++++++++++
 6 files changed, 167 insertions(+)
 create mode 100644 solr/core/src/java/org/apache/solr/core/SimpleTextCodecFactory.java
 create mode 100644 solr/core/src/test-files/solr/collection1/conf/schema-SimpleTextCodec.xml
 create mode 100644 solr/core/src/test-files/solr/collection1/conf/solrconfig_SimpleTextCodec.xml
 create mode 100644 solr/core/src/test/org/apache/solr/core/TestSimpleTextCodec.java

diff --git a/dev-tools/idea/solr/core/src/java/solr-core.iml b/dev-tools/idea/solr/core/src/java/solr-core.iml
index 6cf1ab175f4..61387b980d7 100644
-- a/dev-tools/idea/solr/core/src/java/solr-core.iml
++ b/dev-tools/idea/solr/core/src/java/solr-core.iml
@@ -32,5 +32,6 @@
     <orderEntry type="module" module-name="join" />
     <orderEntry type="module" module-name="sandbox" />
     <orderEntry type="module" module-name="backward-codecs" />
    <orderEntry type="module" module-name="codecs" />
   </component>
 </module>
diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 8f1fe870254..81288d8eee2 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -196,6 +196,9 @@ New Features
 
 * SOLR-10521: introducing sort=childfield(field) asc for searching by {!parent} (Mikhail Khludnev) 
 
* SOLR-9596: Add Solr support for SimpleTextCodec, via <codecFactory class="solr.SimpleTextCodecFactory"/>
  in solrconfig.xml (per-field specification in the schema is not possible). (Steve Rowe)

 Optimizations
 ----------------------
 
diff --git a/solr/core/src/java/org/apache/solr/core/SimpleTextCodecFactory.java b/solr/core/src/java/org/apache/solr/core/SimpleTextCodecFactory.java
new file mode 100644
index 00000000000..de0124fce82
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/core/SimpleTextCodecFactory.java
@@ -0,0 +1,38 @@
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

package org.apache.solr.core;

import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.solr.common.util.NamedList;

public class SimpleTextCodecFactory extends CodecFactory {
  private Codec codec;

  @Override
  public void init(NamedList args) {
    super.init(args);
    assert codec == null;
    codec = new SimpleTextCodec();
  }

  @Override
  public Codec getCodec() {
    return codec;
  }
}
diff --git a/solr/core/src/test-files/solr/collection1/conf/schema-SimpleTextCodec.xml b/solr/core/src/test-files/solr/collection1/conf/schema-SimpleTextCodec.xml
new file mode 100644
index 00000000000..528de731e3f
-- /dev/null
++ b/solr/core/src/test-files/solr/collection1/conf/schema-SimpleTextCodec.xml
@@ -0,0 +1,32 @@
<?xml version="1.0" encoding="UTF-8" ?>
<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
<schema name="SimpleTextCodec" version="1.6">
  <fieldType name="string" class="solr.StrField"/>

  <fieldType name="text_general" class="solr.TextField" positionIncrementGap="100">
    <analyzer>
      <tokenizer class="solr.StandardTokenizerFactory"/>
      <filter class="solr.LowerCaseFilterFactory"/>
    </analyzer>
  </fieldType>

  <field name="id" type="string" indexed="true" stored="true" docValues="true" required="true"/>
  <field name="text" type="text_general" indexed="true" stored="true"/>

  <uniqueKey>id</uniqueKey>
</schema>
diff --git a/solr/core/src/test-files/solr/collection1/conf/solrconfig_SimpleTextCodec.xml b/solr/core/src/test-files/solr/collection1/conf/solrconfig_SimpleTextCodec.xml
new file mode 100644
index 00000000000..f3abf9bc1d3
-- /dev/null
++ b/solr/core/src/test-files/solr/collection1/conf/solrconfig_SimpleTextCodec.xml
@@ -0,0 +1,26 @@
<?xml version="1.0" encoding="UTF-8" ?>
<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

<config>
  <luceneMatchVersion>${tests.luceneMatchVersion:LATEST}</luceneMatchVersion>
  <xi:include href="solrconfig.snippet.randomindexconfig.xml" xmlns:xi="http://www.w3.org/2001/XInclude"/>
  <directoryFactory name="DirectoryFactory" class="${solr.directoryFactory:solr.RAMDirectoryFactory}"/>
  <schemaFactory class="ClassicIndexSchemaFactory"/>
  <requestHandler name="standard" class="solr.StandardRequestHandler"/>
  <codecFactory class="solr.SimpleTextCodecFactory"/>
</config>
diff --git a/solr/core/src/test/org/apache/solr/core/TestSimpleTextCodec.java b/solr/core/src/test/org/apache/solr/core/TestSimpleTextCodec.java
new file mode 100644
index 00000000000..f019f3bfb1d
-- /dev/null
++ b/solr/core/src/test/org/apache/solr/core/TestSimpleTextCodec.java
@@ -0,0 +1,67 @@
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

package org.apache.solr.core;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentInfos;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.junit.BeforeClass;

public class TestSimpleTextCodec extends SolrTestCaseJ4 {

  @BeforeClass
  public static void beforeClass() throws Exception {
    initCore("solrconfig_SimpleTextCodec.xml", "schema-SimpleTextCodec.xml");
  }

  public void test() throws Exception {
    SolrConfig config = h.getCore().getSolrConfig();
    String codecFactory =  config.get("codecFactory/@class");
    assertEquals("Unexpected solrconfig codec factory", "solr.SimpleTextCodecFactory", codecFactory);

    assertEquals("Unexpected core codec", "SimpleText", h.getCore().getCodec().getName());

    RefCounted<IndexWriter> writerRef = h.getCore().getSolrCoreState().getIndexWriter(h.getCore());
    try {
      IndexWriter writer = writerRef.get();
      assertEquals("Unexpected codec in IndexWriter config", 
          "SimpleText", writer.getConfig().getCodec().getName()); 
    } finally {
      writerRef.decref();
    }

    assertU(add(doc("id","1", "text","textual content goes here")));
    assertU(commit());

    RefCounted<SolrIndexSearcher> searcherRef = h.getCore().getSearcher();
    try {
      SolrIndexSearcher searcher = searcherRef.get();
      SegmentInfos infos = SegmentInfos.readLatestCommit(searcher.getIndexReader().directory());
      SegmentInfo info = infos.info(infos.size() - 1).info;
      assertEquals("Unexpected segment codec", "SimpleText", info.getCodec().getName());
    } finally {
      searcherRef.decref();
    }

    assertQ(req("q", "id:1"),
        "*[count(//doc)=1]");
  }
}
- 
2.19.1.windows.1

