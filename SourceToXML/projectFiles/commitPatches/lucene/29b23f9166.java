From 29b23f9166226715b9c8d6865e9d3a426d0da2e2 Mon Sep 17 00:00:00 2001
From: Noble Paul <noble@apache.org>
Date: Sat, 7 Mar 2015 05:46:57 +0000
Subject: [PATCH] SOLR-7073: test files required for the feature

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1664795 13f79535-47bb-0310-9956-ffa450edef68
--
 .../runtimecode/RuntimeLibReqHandler.java     | 35 +++++++++++++++++
 .../runtimecode/RuntimeLibResponseWriter.java | 35 +++++++++++++++++
 .../RuntimeLibSearchComponent.java            | 39 +++++++++++++++++++
 .../test-files/runtimecode/runtimelibs.jar    |  2 +
 .../test-files/runtimecode/runtimelibs_v2.jar |  2 +
 5 files changed, 113 insertions(+)
 create mode 100644 solr/core/src/test-files/runtimecode/RuntimeLibReqHandler.java
 create mode 100644 solr/core/src/test-files/runtimecode/RuntimeLibResponseWriter.java
 create mode 100644 solr/core/src/test-files/runtimecode/RuntimeLibSearchComponent.java
 create mode 100644 solr/core/src/test-files/runtimecode/runtimelibs.jar
 create mode 100644 solr/core/src/test-files/runtimecode/runtimelibs_v2.jar

diff --git a/solr/core/src/test-files/runtimecode/RuntimeLibReqHandler.java b/solr/core/src/test-files/runtimecode/RuntimeLibReqHandler.java
new file mode 100644
index 00000000000..a8bc677be19
-- /dev/null
++ b/solr/core/src/test-files/runtimecode/RuntimeLibReqHandler.java
@@ -0,0 +1,35 @@
package runtimecode;

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

import java.io.IOException;

import org.apache.solr.handler.DumpRequestHandler;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;


public class RuntimeLibReqHandler extends DumpRequestHandler {
  @Override
  public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws IOException {
    super.handleRequestBody(req, rsp);
    rsp.add("class", this.getClass().getName());
    rsp.add("loader",  getClass().getClassLoader().getClass().getName() );

  }
}
diff --git a/solr/core/src/test-files/runtimecode/RuntimeLibResponseWriter.java b/solr/core/src/test-files/runtimecode/RuntimeLibResponseWriter.java
new file mode 100644
index 00000000000..19a4880d47b
-- /dev/null
++ b/solr/core/src/test-files/runtimecode/RuntimeLibResponseWriter.java
@@ -0,0 +1,35 @@
package runtimecode;

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

import java.io.IOException;
import java.io.Writer;

import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.JSONResponseWriter;
import org.apache.solr.response.SolrQueryResponse;

public class RuntimeLibResponseWriter extends JSONResponseWriter {
  @Override
  public void write(Writer writer, SolrQueryRequest req, SolrQueryResponse rsp) throws IOException {
    rsp.add("wt", RuntimeLibResponseWriter.class.getName());
    rsp.add("loader",  getClass().getClassLoader().getClass().getName() );

    super.write(writer, req, rsp);
  }
}
diff --git a/solr/core/src/test-files/runtimecode/RuntimeLibSearchComponent.java b/solr/core/src/test-files/runtimecode/RuntimeLibSearchComponent.java
new file mode 100644
index 00000000000..7bbcb022205
-- /dev/null
++ b/solr/core/src/test-files/runtimecode/RuntimeLibSearchComponent.java
@@ -0,0 +1,39 @@
package runtimecode;

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


import java.io.IOException;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.component.RealTimeGetComponent;
import org.apache.solr.handler.component.ResponseBuilder;

public class RuntimeLibSearchComponent extends RealTimeGetComponent {
  @Override
  public void prepare(ResponseBuilder rb) throws IOException {
    SolrParams params = rb.req.getParams();

    if (params.getBool(COMPONENT_NAME, true)) {
      rb.rsp.add(COMPONENT_NAME, RuntimeLibSearchComponent.class.getName());
      rb.rsp.add("loader",  getClass().getClassLoader().getClass().getName() );
      rb.rsp.add("Version", "2" );
    }
    super.process(rb);
  }
}
diff --git a/solr/core/src/test-files/runtimecode/runtimelibs.jar b/solr/core/src/test-files/runtimecode/runtimelibs.jar
new file mode 100644
index 00000000000..c28d361ccb5
-- /dev/null
++ b/solr/core/src/test-files/runtimecode/runtimelibs.jar
@@ -0,0 +1,2 @@
AnyObjectId[55c835b234da9cfdd6161938475835af8e85c008] was removed in git history.
Apache SVN contains full history.
\ No newline at end of file
diff --git a/solr/core/src/test-files/runtimecode/runtimelibs_v2.jar b/solr/core/src/test-files/runtimecode/runtimelibs_v2.jar
new file mode 100644
index 00000000000..96f5ab5198e
-- /dev/null
++ b/solr/core/src/test-files/runtimecode/runtimelibs_v2.jar
@@ -0,0 +1,2 @@
AnyObjectId[226a9dbceea9e942e9e91a33225cc97f400416a5] was removed in git history.
Apache SVN contains full history.
\ No newline at end of file
- 
2.19.1.windows.1

