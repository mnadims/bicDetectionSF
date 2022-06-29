From f389654f9c0763127df748769b9239a510e4dea8 Mon Sep 17 00:00:00 2001
From: Steven Rowe <sarowe@apache.org>
Date: Mon, 14 Nov 2011 16:48:09 +0000
Subject: [PATCH] SOLR-2382: Switch javadoc tag @solr.experimental to
 @lucene.experimental (the build's javadoc invocation does not include
 @solr.experimental, and @lucene.experimental is already used in many places
 in Solr's code)

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1201784 13f79535-47bb-0310-9956-ffa450edef68
--
 .../solr/handler/dataimport/DIHCache.java     | 19 ++++++++++++++++++-
 1 file changed, 18 insertions(+), 1 deletion(-)

diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHCache.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHCache.java
index 049e503330c..2b6ae9a9663 100644
-- a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHCache.java
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHCache.java
@@ -1,5 +1,22 @@
 package org.apache.solr.handler.dataimport;
 
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

 import java.util.Iterator;
 import java.util.Map;
 
@@ -9,7 +26,7 @@ import java.util.Map;
  * to other data and/or indexed.
  * </p>
  * 
 * @solr.experimental
 * @lucene.experimental
  */
 public interface DIHCache extends Iterable<Map<String,Object>> {
   
- 
2.19.1.windows.1

