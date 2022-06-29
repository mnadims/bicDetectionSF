From 01527d4f4e980fa35230183287f64c3bb0c15bf2 Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Mon, 20 Feb 2012 19:57:39 +0000
Subject: [PATCH] JCR-3236: Can not instantiate lucene Analyzer in SearchIndex

Support also Analyzer classes that require a Version instance to be passed to a constructor

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1291424 13f79535-47bb-0310-9956-ffa450edef68
--
 .../lucene/IndexingConfigurationImpl.java     | 51 +++++--------
 .../core/query/lucene/JackrabbitAnalyzer.java | 73 ++++++++++++++++++-
 .../core/query/lucene/SearchIndex.java        | 19 ++---
 .../core/query/lucene/SearchIndexTest.java    | 38 ++++++++++
 4 files changed, 133 insertions(+), 48 deletions(-)
 create mode 100644 jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/SearchIndexTest.java

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexingConfigurationImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexingConfigurationImpl.java
index 0f73aede1..fcdb36d74 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexingConfigurationImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexingConfigurationImpl.java
@@ -159,41 +159,26 @@ public class IndexingConfigurationImpl
                 for (int j = 0; j < childNodes.getLength(); j++) {
                     Node analyzerNode = childNodes.item(j);
                     if (analyzerNode.getNodeName().equals("analyzer")) {
                        String analyzerClassName = analyzerNode.getAttributes().getNamedItem("class").getNodeValue();
                        try {
                            @SuppressWarnings("rawtypes")
                            Class clazz = Class.forName(analyzerClassName);
                            if (clazz == JackrabbitAnalyzer.class) {
                                log.warn("Not allowed to configure " + JackrabbitAnalyzer.class.getName() +  " for a property. "
                                        + "Using default analyzer for that property.");
                            }
                            else if (Analyzer.class.isAssignableFrom(clazz)) {
                                Analyzer analyzer = (Analyzer) clazz.newInstance();
                                NodeList propertyChildNodes = analyzerNode.getChildNodes();
                                for (int k = 0; k < propertyChildNodes.getLength(); k++) {
                                    Node propertyNode = propertyChildNodes.item(k);
                                    if (propertyNode.getNodeName().equals("property")) {
                                        // get property name
                                        Name propName = resolver.getQName(getTextContent(propertyNode));
                                        String fieldName = nsMappings.translateName(propName);
                                        // set analyzer for the fulltext property fieldname
                                        int idx = fieldName.indexOf(':');
                                        fieldName = fieldName.substring(0, idx + 1)
                                                    + FieldNames.FULLTEXT_PREFIX + fieldName.substring(idx + 1);
                                        Object prevAnalyzer = analyzers.put(fieldName, analyzer);
                                        if (prevAnalyzer != null) {
                                            log.warn("Property " + propName.getLocalName()
                                                    + " has been configured for multiple analyzers. "
                                                    + " Last configured analyzer is used");
                                        }
                                    }
                        Analyzer analyzer = JackrabbitAnalyzer.getAnalyzerInstance(
                                analyzerNode.getAttributes().getNamedItem("class").getNodeValue());
                        NodeList propertyChildNodes = analyzerNode.getChildNodes();
                        for (int k = 0; k < propertyChildNodes.getLength(); k++) {
                            Node propertyNode = propertyChildNodes.item(k);
                            if (propertyNode.getNodeName().equals("property")) {
                                // get property name
                                Name propName = resolver.getQName(getTextContent(propertyNode));
                                String fieldName = nsMappings.translateName(propName);
                                // set analyzer for the fulltext property fieldname
                                int idx = fieldName.indexOf(':');
                                fieldName = fieldName.substring(0, idx + 1)
                                        + FieldNames.FULLTEXT_PREFIX + fieldName.substring(idx + 1);
                                Object prevAnalyzer = analyzers.put(fieldName, analyzer);
                                if (prevAnalyzer != null) {
                                    log.warn("Property " + propName.getLocalName()
                                            + " has been configured for multiple analyzers. "
                                            + " Last configured analyzer is used");
                                 }
                            } else {
                                log.warn("org.apache.lucene.analysis.Analyzer is not a superclass of "
                                        + analyzerClassName + ". Ignoring this configure analyzer" );
                             }
                        } catch (ClassNotFoundException e) {
                            log.warn("Analyzer class not found: " + analyzerClassName, e);
                         }
                     }
                 }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitAnalyzer.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitAnalyzer.java
index 9a7fbeeec..5bb614ad3 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitAnalyzer.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitAnalyzer.java
@@ -18,12 +18,15 @@ package org.apache.jackrabbit.core.query.lucene;
 
 import java.io.IOException;
 import java.io.Reader;
import java.lang.reflect.Constructor;
 import java.util.Collections;
 
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.standard.StandardAnalyzer;
 import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
 /**
  * This is the global jackrabbit lucene analyzer. By default, all
@@ -34,14 +37,68 @@ import org.apache.lucene.util.Version;
  * indexed with a specific analyzer. If configured, this analyzer is used to
  * index the text of the property and to parse searchtext for this property.
  */
public class JackrabbitAnalyzer extends Analyzer {
 
public class JackrabbitAnalyzer  extends Analyzer {
    private static Logger log =
            LoggerFactory.getLogger(JackrabbitAnalyzer.class);

    private static final Analyzer DEFAULT_ANALYZER =
            new StandardAnalyzer(Version.LUCENE_24, Collections.emptySet());
 
     /**
     * The default Jackrabbit analyzer if none is configured in <code><SearchIndex></code>
     * configuration.
     * Returns a new instance of the named Lucene {@link Analyzer} class,
     * or the default analyzer if the given class can not be instantiated.
     *
     * @param className name of the analyzer class
     * @return new analyzer instance, or the default analyzer
      */
    private Analyzer defaultAnalyzer =  new StandardAnalyzer(Version.LUCENE_24, Collections.emptySet());
    static Analyzer getAnalyzerInstance(String className) {
        Class<?> analyzerClass;
        try {
            analyzerClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            log.warn(className + " could not be found", e);
            return DEFAULT_ANALYZER;
        }
        if (!Analyzer.class.isAssignableFrom(analyzerClass)) {
            log.warn(className + " is not a Lucene Analyzer");
            return DEFAULT_ANALYZER;
        } else if (JackrabbitAnalyzer.class.isAssignableFrom(analyzerClass)) {
            log.warn(className + " can not be used as a JackrabbitAnalyzer component");
            return DEFAULT_ANALYZER;
        }

        Exception cause = null;
        Constructor<?>[] constructors = analyzerClass.getConstructors();
        for (Constructor<?> constructor : constructors) {
            Class<?>[] types = constructor.getParameterTypes();
            if (types.length == 1 && types[0] == Version.class) {
                try {
                    return (Analyzer) constructor.newInstance(Version.LUCENE_24);
                } catch (Exception e) {
                    cause = e;
                }
            }
        }
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterTypes().length == 0) {
                try {
                    return (Analyzer) constructor.newInstance();
                } catch (Exception e) {
                    cause = e;
                }
            }
        }

        log.warn(className + " could not be instantiated", cause);
        return DEFAULT_ANALYZER;
    }

    /**
     * The default Jackrabbit analyzer if none is configured in
     * <code>&lt;SearchIndex&gt;</code> configuration.
     */
    private Analyzer defaultAnalyzer = DEFAULT_ANALYZER;
 
     /**
      * The indexing configuration.
@@ -62,6 +119,14 @@ public class JackrabbitAnalyzer  extends Analyzer {
         defaultAnalyzer = analyzer;
     }
 
    String getDefaultAnalyzerClass() {
        return defaultAnalyzer.getClass().getName();
    }

    void setDefaultAnalyzerClass(String className) {
        setDefaultAnalyzer(getAnalyzerInstance(className));
    }

     /**
      * Creates a TokenStream which tokenizes all the text in the provided
      * Reader. If the fieldName (property) is configured to have a different
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SearchIndex.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SearchIndex.java
index 140c9ba9a..a02bbbf0b 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SearchIndex.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SearchIndex.java
@@ -99,6 +99,7 @@ import org.apache.lucene.search.Similarity;
 import org.apache.lucene.search.Sort;
 import org.apache.lucene.search.SortField;
 import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;
 import org.apache.tika.config.TikaConfig;
 import org.apache.tika.fork.ForkParser;
 import org.apache.tika.parser.AutoDetectParser;
@@ -1867,11 +1868,12 @@ public class SearchIndex extends AbstractQueryHandler {
     //--------------------------< properties >----------------------------------
 
     /**
     * Sets the analyzer in use for indexing. The given analyzer class name
     * must satisfy the following conditions:
     * Sets the default analyzer in use for indexing. The given analyzer
     * class name must satisfy the following conditions:
      * <ul>
      *   <li>the class must exist in the class path</li>
     *   <li>the class must have a public default constructor</li>
     *   <li>the class must have a public default constructor, or
     *       a constructor that takes a Lucene {@link Version} argument</li>
      *   <li>the class must be a Lucene Analyzer</li>
      * </ul>
      * <p>
@@ -1886,21 +1888,16 @@ public class SearchIndex extends AbstractQueryHandler {
      * @param analyzerClassName the analyzer class name
      */
     public void setAnalyzer(String analyzerClassName) {
        try {
            Class<?> analyzerClass = Class.forName(analyzerClassName);
            analyzer.setDefaultAnalyzer((Analyzer) analyzerClass.newInstance());
        } catch (Exception e) {
            log.warn("Invalid Analyzer class: " + analyzerClassName, e);
        }
        analyzer.setDefaultAnalyzerClass(analyzerClassName);
     }
 
     /**
     * Returns the class name of the analyzer that is currently in use.
     * Returns the class name of the default analyzer that is currently in use.
      *
      * @return class name of analyzer in use.
      */
     public String getAnalyzer() {
        return analyzer.getClass().getName();
        return analyzer.getDefaultAnalyzerClass();
     }
 
     /**
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/SearchIndexTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/SearchIndexTest.java
new file mode 100644
index 000000000..68ad0dea9
-- /dev/null
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/SearchIndexTest.java
@@ -0,0 +1,38 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.query.lucene;

import junit.framework.TestCase;

public class SearchIndexTest extends TestCase {

    /**
     * @see <a href="https://issues.apache.org/jira/browse/JCR-3236">JCR-3236</a>
     */
    public void testSetAnalyzer() {
        String[] analyzers = {
                "org.apache.lucene.analysis.SimpleAnalyzer",
                "org.apache.lucene.analysis.StopAnalyzer",
                "org.apache.lucene.analysis.standard.StandardAnalyzer" };
        SearchIndex index = new SearchIndex();
        for (String analyzer : analyzers) {
            index.setAnalyzer(analyzer);
            assertEquals(analyzer, index.getAnalyzer());
        }
    }

}
- 
2.19.1.windows.1

