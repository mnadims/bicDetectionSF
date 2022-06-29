From e24071be0aec7e63a0b73bf01f4bbd9930ece066 Mon Sep 17 00:00:00 2001
From: Hemanth Yamijala <yhemanth@apache.org>
Date: Mon, 7 Sep 2009 11:04:17 +0000
Subject: [PATCH] HADOOP-6105. Adds support for automatically handling
 deprecation of configuration keys. Contributed by V.V.Chaitanya Krishna.

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@812078 13f79535-47bb-0310-9956-ffa450edef68
--
 CHANGES.txt                                   |   3 +
 src/java/core-default.xml                     |   6 +-
 .../org/apache/hadoop/conf/Configuration.java | 282 +++++++++++++++-
 .../conf/TestConfigurationDeprecation.java    | 312 ++++++++++++++++++
 4 files changed, 592 insertions(+), 11 deletions(-)
 create mode 100644 src/test/core/org/apache/hadoop/conf/TestConfigurationDeprecation.java

diff --git a/CHANGES.txt b/CHANGES.txt
index 96b8111cfd4..b210bf9ad02 100644
-- a/CHANGES.txt
++ b/CHANGES.txt
@@ -171,6 +171,9 @@ Trunk (unreleased changes)
 
     HADOOP-6165. Add metadata to Serializations. (tomwhite)
 
    HADOOP-6105. Adds support for automatically handling deprecation of
    configuration keys. (V.V.Chaitanya Krishna via yhemanth)
    
   IMPROVEMENTS
 
     HADOOP-4565. Added CombineFileInputFormat to use data locality information
diff --git a/src/java/core-default.xml b/src/java/core-default.xml
index 146a0238a3e..9034a9e655e 100644
-- a/src/java/core-default.xml
++ b/src/java/core-default.xml
@@ -486,6 +486,10 @@
   </description>
 </property>
 

<property>
  <name>hadoop.conf.extra.classes</name>
  <value>org.apache.hadoop.mapred.JobConf</value>
  <final>true</final>
</property>
 
 </configuration>
diff --git a/src/java/org/apache/hadoop/conf/Configuration.java b/src/java/org/apache/hadoop/conf/Configuration.java
index edf0f15afef..cfb1ba8d70a 100644
-- a/src/java/org/apache/hadoop/conf/Configuration.java
++ b/src/java/org/apache/hadoop/conf/Configuration.java
@@ -44,6 +44,7 @@
 import java.util.Set;
 import java.util.StringTokenizer;
 import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
@@ -182,6 +183,141 @@
    */
   private HashMap<String, String> updatingResource;
   
  /**
   * Class to keep the information about the keys which replace the deprecated
   * ones.
   * 
   * This class stores the new keys which replace the deprecated keys and also
   * gives a provision to have a custom message for each of the deprecated key
   * that is being replaced. It also provides method to get the appropriate
   * warning message which can be logged whenever the deprecated key is used.
   */
  private static class DeprecatedKeyInfo {
    private String[] newKeys;
    private String customMessage;
    private boolean accessed;
    DeprecatedKeyInfo(String[] newKeys, String customMessage) {
      this.newKeys = newKeys;
      this.customMessage = customMessage;
      accessed = false;
    }
    DeprecatedKeyInfo(String[] newKeys) {
      this(newKeys, null);
    }

    /**
     * Method to provide the warning message. It gives the custom message if
     * non-null, and default message otherwise.
     * @param key the associated deprecated key.
     * @return message that is to be logged when a deprecated key is used.
     */
    private final String getWarningMessage(String key) {
      String warningMessage;
      if(customMessage == null) {
        StringBuilder message = new StringBuilder(key);
        String deprecatedKeySuffix = " is deprecated. Instead, use ";
        message.append(deprecatedKeySuffix);
        for (int i = 0; i < newKeys.length; i++) {
          message.append(newKeys[i]);
          if(i != newKeys.length-1) {
            message.append(", ");
          }
        }
        warningMessage = message.toString();
      }
      else {
        warningMessage = customMessage;
      }
      accessed = true;
      return warningMessage;
    }
  }
  
  /**
   * Stores the deprecated keys, the new keys which replace the deprecated keys
   * and custom message(if any provided).
   */
  private static Map<String, DeprecatedKeyInfo> deprecatedKeyMap = 
    new HashMap<String, DeprecatedKeyInfo>();
  
  /**
   * Adds the deprecated key to the deprecation map.
   * It does not override any existing entries in the deprecation map.
   * This is to be used only by the developers in order to add deprecation of
   * keys, and attempts to call this method after loading resources once,
   * would lead to <tt>UnsupportedOperationException</tt>
   * @param key
   * @param newKeys
   * @param customMessage
   */
  public synchronized static void addDeprecation(String key, String[] newKeys,
      String customMessage) {
    if (key == null || key.length() == 0 ||
        newKeys == null || newKeys.length == 0) {
      throw new IllegalArgumentException();
    }
    if (!isDeprecated(key)) {
      DeprecatedKeyInfo newKeyInfo;
      if (customMessage == null) {
        newKeyInfo = new DeprecatedKeyInfo(newKeys);
      }
      else {
        newKeyInfo = new DeprecatedKeyInfo(newKeys, customMessage);
      }
      deprecatedKeyMap.put(key, newKeyInfo);
    }
  }

  /**
   * Adds the deprecated key to the deprecation map when no custom message
   * is provided.
   * It does not override any existing entries in the deprecation map.
   * This is to be used only by the developers in order to add deprecation of
   * keys, and attempts to call this method after loading resources once,
   * would lead to <tt>UnsupportedOperationException</tt>
   * 
   * @param key Key that is to be deprecated
   * @param newKeys list of keys that take up the values of deprecated key
   */
  public synchronized static void addDeprecation(String key, String[] newKeys) {
    addDeprecation(key, newKeys, null);
  }
  
  /**
   * checks whether the given <code>key</code> is deprecated.
   * 
   * @param key the parameter which is to be checked for deprecation
   * @return <code>true</code> if the key is deprecated and 
   *         <code>false</code> otherwise.
   */
  private static boolean isDeprecated(String key) {
    return deprecatedKeyMap.containsKey(key);
  }
 
  /**
   * Checks for the presence of the property <code>name</code> in the
   * deprecation map. Returns the first of the list of new keys if present
   * in the deprecation map or the <code>name</code> itself.
   * @param name the property name
   * @return the first property in the list of properties mapping
   *         the <code>name</code> or the <code>name</code> itself.
   */
  private String handleDeprecation(String name) {
    if (isDeprecated(name)) {
      DeprecatedKeyInfo keyInfo = deprecatedKeyMap.get(name);
      if (!keyInfo.accessed) {
        LOG.warn(keyInfo.getWarningMessage(name));
      }
      for (String newKey : keyInfo.newKeys) {
        if(newKey != null) {
          name = newKey;
          break;
        }
      }
    }
    return name;
  }
  
   static{
     //print deprecation warning if hadoop-site.xml is found in classpath
     ClassLoader cL = Thread.currentThread().getContextClassLoader();
@@ -405,40 +541,60 @@ private String substituteVars(String expr) {
   
   /**
    * Get the value of the <code>name</code> property, <code>null</code> if
   * no such property exists.
   * no such property exists. If the key is deprecated, it returns the value of
   * the first key which replaces the deprecated key and is not null
    * 
    * Values are processed for <a href="#VariableExpansion">variable expansion</a> 
    * before being returned. 
    * 
    * @param name the property name.
   * @return the value of the <code>name</code> property, 
   * @return the value of the <code>name</code> or its replacing property, 
    *         or null if no such property exists.
    */
   public String get(String name) {
    name = handleDeprecation(name);
     return substituteVars(getProps().getProperty(name));
   }
 
   /**
    * Get the value of the <code>name</code> property, without doing
   * <a href="#VariableExpansion">variable expansion</a>.
   * <a href="#VariableExpansion">variable expansion</a>.If the key is 
   * deprecated, it returns the value of the first key which replaces 
   * the deprecated key and is not null.
    * 
    * @param name the property name.
   * @return the value of the <code>name</code> property, 
   *         or null if no such property exists.
   * @return the value of the <code>name</code> property or 
   *         its replacing property and null if no such property exists.
    */
   public String getRaw(String name) {
    name = handleDeprecation(name);
     return getProps().getProperty(name);
   }
 
   /** 
   * Set the <code>value</code> of the <code>name</code> property.
   * Set the <code>value</code> of the <code>name</code> property. If 
   * <code>name</code> is deprecated, it sets the <code>value</code> to the keys
   * that replace the deprecated key.
    * 
    * @param name property name.
    * @param value property value.
    */
   public void set(String name, String value) {
    getOverlay().setProperty(name, value);
    getProps().setProperty(name, value);
    if (deprecatedKeyMap.isEmpty()) {
      getProps();
    }
    if (!isDeprecated(name)) {
      getOverlay().setProperty(name, value);
      getProps().setProperty(name, value);
    }
    else {
      DeprecatedKeyInfo keyInfo = deprecatedKeyMap.get(name);
      LOG.warn(keyInfo.getWarningMessage(name));
      for (String newKey : keyInfo.newKeys) {
        getOverlay().setProperty(newKey, value);
        getProps().setProperty(newKey, value);
      }
    }
   }
   
   /**
@@ -460,8 +616,11 @@ private synchronized Properties getOverlay() {
   }
 
   /** 
   * Get the value of the <code>name</code> property. If no such property 
   * exists, then <code>defaultValue</code> is returned.
   * Get the value of the <code>name</code>. If the key is deprecated,
   * it returns the value of the first key which replaces the deprecated key
   * and is not null.
   * If no such property exists,
   * then <code>defaultValue</code> is returned.
    * 
    * @param name property name.
    * @param defaultValue default value.
@@ -469,6 +628,7 @@ private synchronized Properties getOverlay() {
    *         doesn't exist.                    
    */
   public String get(String name, String defaultValue) {
    name = handleDeprecation(name);
     return substituteVars(getProps().getProperty(name, defaultValue));
   }
     
@@ -1180,8 +1340,110 @@ private void loadResources(Properties properties,
     for (Object resource : resources) {
       loadResource(properties, resource, quiet);
     }
    // process for deprecation.
    processDeprecation();
   }
  
  /**
   * Flag to ensure that the classes mentioned in the value of the property
   * <code>hadoop.conf.extra.classes</code> are loaded only once for
   * all instances of <code>Configuration</code>
   */
  private static AtomicBoolean loadedDeprecation = new AtomicBoolean(false);
  
  private static final String extraConfKey = "hadoop.conf.extra.classes";
 
  /**
   * adds all the deprecations to the deprecatedKeyMap and updates the values of
   * the appropriate keys
   */
  private void processDeprecation() {
    populateDeprecationMapping();
    processDeprecatedKeys();
  }
  
  /**
   * Loads all the classes in mapred and hdfs that extend Configuration and that
   * have deprecations to be added into deprecatedKeyMap
   */
  private synchronized void populateDeprecationMapping() {
    if (!loadedDeprecation.get()) {
      // load classes from mapred and hdfs which extend Configuration and have 
      // deprecations added in their static blocks
      String classnames = substituteVars(properties.getProperty(extraConfKey));
      if (classnames == null) {
        return;
      }
      String[] classes = StringUtils.getStrings(classnames);
      for (String className : classes) {
        try {
          Class.forName(className);
        } catch (ClassNotFoundException e) {
          LOG.warn(className + " is not in the classpath");
        }
      }
      // make deprecatedKeyMap unmodifiable in order to prevent changes to 
      // it in user's code.
      deprecatedKeyMap = Collections.unmodifiableMap(deprecatedKeyMap);
      // ensure that deprecation processing is done only once for all 
      // instances of this object
      loadedDeprecation.set(true);
    }
  }

  /**
   * Updates the keys that are replacing the deprecated keys and removes the 
   * deprecated keys from memory.
   */
  private void processDeprecatedKeys() {
    for (Map.Entry<String, DeprecatedKeyInfo> item : 
      deprecatedKeyMap.entrySet()) {
      if (!properties.containsKey(item.getKey())) {
        continue;
      }
      String oldKey = item.getKey();
      deprecatedKeyMap.get(oldKey).accessed = false;
      setDeprecatedValue(oldKey, properties.getProperty(oldKey),
          finalParameters.contains(oldKey));
      properties.remove(oldKey);
      if (finalParameters.contains(oldKey)) {
        finalParameters.remove(oldKey);
      }
      if (storeResource) {
        updatingResource.remove(oldKey);
      }
    }
  }
  
  /**
   * Sets the deprecated key's value to the associated mapped keys
   * @param attr the deprecated key
   * @param value the value corresponding to the deprecated key
   * @param finalParameter flag to indicate if <code>attr</code> is
   *        marked as final
   */
  private void setDeprecatedValue(String attr,
      String value, boolean finalParameter) {
    DeprecatedKeyInfo keyInfo = deprecatedKeyMap.get(attr);
    for (String key:keyInfo.newKeys) {
      // update replacing keys with deprecated key's value in all cases,
      // except when the replacing key is already set to final
      // and finalParameter is false
      if (finalParameters.contains(key) && !finalParameter) {
        LOG.warn("An attempt to override final parameter: "+key
            +";  Ignoring.");
        continue;
      }
      properties.setProperty(key, value);
      if (storeResource) {
        updatingResource.put(key, updatingResource.get(attr));
      }
      if (finalParameter) {
        finalParameters.add(key);
      }
    }
  }
  
   private void loadResource(Properties properties, Object name, boolean quiet) {
     try {
       DocumentBuilderFactory docBuilderFactory 
diff --git a/src/test/core/org/apache/hadoop/conf/TestConfigurationDeprecation.java b/src/test/core/org/apache/hadoop/conf/TestConfigurationDeprecation.java
new file mode 100644
index 00000000000..bbb0e758c58
-- /dev/null
++ b/src/test/core/org/apache/hadoop/conf/TestConfigurationDeprecation.java
@@ -0,0 +1,312 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.conf;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

 
public class TestConfigurationDeprecation {
  private Configuration conf;
  final static String CONFIG = new File("./test-config.xml").getAbsolutePath();
  final static String CONFIG2 = 
    new File("./test-config2.xml").getAbsolutePath();
  final static String CONFIG3 = 
    new File("./test-config3.xml").getAbsolutePath();
  BufferedWriter out;

  @Before
  public void setUp() throws Exception {
    conf = new Configuration(false);
  }

  @After
  public void tearDown() throws Exception {
    new File(CONFIG).delete();
    new File(CONFIG2).delete();
    new File(CONFIG3).delete();
  }
  
  private void startConfig() throws IOException{
    out.write("<?xml version=\"1.0\"?>\n");
    out.write("<configuration>\n");
  }

  private void endConfig() throws IOException{
    out.write("</configuration>\n");
    out.close();
  }

  void appendProperty(String name, String val) throws IOException {
    appendProperty(name, val, false);
  }
 
  void appendProperty(String name, String val, boolean isFinal)
    throws IOException {
    out.write("<property>");
    out.write("<name>");
    out.write(name);
    out.write("</name>");
    out.write("<value>");
    out.write(val);
    out.write("</value>");
    if (isFinal) {
      out.write("<final>true</final>");
    }
    out.write("</property>\n");
  }
  
  static class MyConf extends Configuration {
    static {
      // add deprecation mappings.
      Configuration.addDeprecation("old.key1", new String[]{"new.key1"});
      Configuration.addDeprecation("old.key2", new String[]{"new.key2"});
      Configuration.addDeprecation("old.key3", new String[]{"new.key3"});
      Configuration.addDeprecation("old.key4", new String[]{"new.key4"});
      Configuration.addDeprecation("old.key5", new String[]{"new.key5"});
      Configuration.addDeprecation("old.key6", new String[]{"new.key6"});
      Configuration.addDeprecation("old.key7", new String[]{"new.key7"});
      Configuration.addDeprecation("old.key8", new String[]{"new.key8"});
      Configuration.addDeprecation("old.key9", new String[]{"new.key9"});
      Configuration.addDeprecation("old.key10", new String[]{"new.key10"});
      Configuration.addDeprecation("old.key11", new String[]{"new.key11"});
      Configuration.addDeprecation("old.key12", new String[]{"new.key12"});
      Configuration.addDeprecation("old.key13", new String[]{"new.key13"});
      Configuration.addDeprecation("old.key14", new String[]{"new.key14"});
      Configuration.addDeprecation("old.key15", new String[]{"new.key15"});
      Configuration.addDeprecation("old.key16", new String[]{"new.key16"});
      Configuration.addDeprecation("A", new String[]{"B"});
      Configuration.addDeprecation("C", new String[]{"D"});
      Configuration.addDeprecation("E", new String[]{"F"});
      Configuration.addDeprecation("G", new String[]{"H","I"});
    }
  }
  
  /**
   * This test is to check the precedence order between being final and 
   * deprecation.Based on the order of occurrence of deprecated key and 
   * its corresponding mapping key, various cases arise.
   * The precedence order being followed is:
   * 1. Final Parameter 
   * 2. Deprecated key's value.
   * @throws IOException 
   * 
   * @throws IOException
   * @throws ClassNotFoundException 
   */
  @Test
  public void testDeprecation() throws IOException {
    out=new BufferedWriter(new FileWriter(CONFIG));
    startConfig();
    appendProperty("hadoop.conf.extra.classes", MyConf.class.getName()
        + ",myconf1");
    // load keys with default values. Some of them are set to final to
    // test the precedence order between deprecation and being final
    appendProperty("new.key1","default.value1",true);
    appendProperty("new.key2","default.value2");
    appendProperty("new.key3","default.value3",true);
    appendProperty("new.key4","default.value4");
    appendProperty("new.key5","default.value5",true);
    appendProperty("new.key6","default.value6");
    appendProperty("new.key7","default.value7",true);
    appendProperty("new.key8","default.value8");
    appendProperty("new.key9","default.value9");
    appendProperty("new.key10","default.value10");
    appendProperty("new.key11","default.value11");
    appendProperty("new.key12","default.value12");
    appendProperty("new.key13","default.value13");
    appendProperty("new.key14","default.value14");
    appendProperty("new.key15","default.value15");
    appendProperty("new.key16","default.value16");
    endConfig();
    Path fileResource = new Path(CONFIG);
    conf.addResource(fileResource);
    
    out=new BufferedWriter(new FileWriter(CONFIG2));
    startConfig();
    // add keys that are tested while they are loaded just after their 
    // corresponding default values
    appendProperty("old.key1","old.value1",true);
    appendProperty("old.key2","old.value2",true);
    appendProperty("old.key3","old.value3");
    appendProperty("old.key4","old.value4");
    appendProperty("new.key5","new.value5",true);
    appendProperty("new.key6","new.value6",true);
    appendProperty("new.key7","new.value7");
    appendProperty("new.key8","new.value8");
    
    // add keys that are tested while they are loaded first and are followed by
    // loading of their corresponding deprecated or replacing key
    appendProperty("new.key9","new.value9",true);
    appendProperty("new.key10","new.value10");
    appendProperty("new.key11","new.value11",true);
    appendProperty("new.key12","new.value12");
    appendProperty("old.key13","old.value13",true);
    appendProperty("old.key14","old.value14");
    appendProperty("old.key15","old.value15",true);
    appendProperty("old.key16","old.value16");
    endConfig();
    Path fileResource1 = new Path(CONFIG2);
    conf.addResource(fileResource1);
    
    out=new BufferedWriter(new FileWriter(CONFIG3));
    startConfig();
    // add keys which are already loaded by the corresponding replacing or 
    // deprecated key.
    appendProperty("old.key9","old.value9",true);
    appendProperty("old.key10","old.value10",true);
    appendProperty("old.key11","old.value11");
    appendProperty("old.key12","old.value12");
    appendProperty("new.key13","new.value13",true);
    appendProperty("new.key14","new.value14",true);
    appendProperty("new.key15","new.value15");
    appendProperty("new.key16","new.value16");
    appendProperty("B", "valueB");
    endConfig();
    Path fileResource2 = new Path(CONFIG3);
    conf.addResource(fileResource2);
    
    // get the values. Also check for consistency in get of old and new keys, 
    // when they are set to final or non-final
    // Key - the key that is being loaded
    // isFinal - true if the key is marked as final
    // prev.occurrence - key that most recently loaded the current key 
    //                   with its value.
    // isPrevFinal - true if key corresponding to 
    //               prev.occurrence is marked as final.
    
    // Key-deprecated , isFinal-true, prev.occurrence-default.xml,
    // isPrevFinal-true
    assertEquals("old.value1", conf.get("new.key1"));
    // check consistency in get of old and new keys
    assertEquals(conf.get("old.key1"), conf.get("new.key1"));
    // Key-deprecated , isFinal-true, prev.occurrence-default.xml,
    // isPrevFinal-false
    assertEquals("old.value2", conf.get("new.key2"));
    // check consistency in get of old and new keys
    assertEquals(conf.get("old.key2"), conf.get("new.key2"));
    // Key-deprecated , isFinal-false, prev.occurrence-default.xml,
    // isPrevFinal-true
    assertEquals("default.value3", conf.get("new.key3"));
    // check consistency in get of old and new keys
    assertEquals(conf.get("old.key3"), conf.get("new.key3"));
    // Key-deprecated , isFinal-false, prev.occurrence-default.xml,
    // isPrevFinal-false
    assertEquals("old.value4", conf.get("new.key4"));
    // check consistency in get of old and new keys
    assertEquals(conf.get("old.key4"), conf.get("new.key4"));
    // Key-site.xml , isFinal-true, prev.occurrence-default.xml,
    // isPrevFinal-true
    assertEquals("default.value5", conf.get("new.key5"));
    // check consistency in get of old and new keys
    assertEquals(conf.get("old.key5"), conf.get("new.key5"));
    // Key-site.xml , isFinal-true, prev.occurrence-default.xml,
    // isPrevFinal-false
    assertEquals("new.value6",conf.get("new.key6"));
    // check consistency in get of old and new keys
    assertEquals(conf.get("old.key6"), conf.get("new.key6"));
    // Key-site.xml , isFinal-false, prev.occurrence-default.xml,
    // isPrevFinal-true
    assertEquals("default.value7", conf.get("new.key7"));
    // check consistency in get of old and new keys
    assertEquals(conf.get("old.key7"), conf.get("new.key7"));
    // Key-site.xml , isFinal-false, prev.occurrence-default.xml,
    // isPrevFinal-false
    assertEquals("new.value8",conf.get("new.key8"));
    // check consistency in get of old and new keys
    assertEquals(conf.get("old.key8"), conf.get("new.key8"));
    // Key-deprecated , isFinal-true, prev.occurrence-site.xml,
    // isPrevFinal-true
    assertEquals("old.value9", conf.get("new.key9"));
    // check consistency in get of old and new keys
    assertEquals(conf.get("old.key9"), conf.get("new.key9"));
    // Key-deprecated , isFinal-true, prev.occurrence-site.xml,
    // isPrevFinal-false
    assertEquals("old.value10", conf.get("new.key10"));
    // check consistency in get of old and new keys
    assertEquals(conf.get("old.key10"), conf.get("new.key10"));
    // Key-deprecated , isFinal-false, prev.occurrence-site.xml,
    // isPrevFinal-true
    assertEquals("new.value11", conf.get("new.key11"));
    // check consistency in get of old and new keys
    assertEquals(conf.get("old.key11"), conf.get("new.key11"));
    // Key-deprecated , isFinal-false, prev.occurrence-site.xml,
    // isPrevFinal-false
    assertEquals("old.value12", conf.get("new.key12"));
    // check consistency in get of old and new keys
    assertEquals(conf.get("old.key12"), conf.get("new.key12"));
    // Key-site.xml , isFinal-true, prev.occurrence-deprecated,
    // isPrevFinal-true
    assertEquals("old.value13", conf.get("new.key13"));
    // check consistency in get of old and new keys
    assertEquals(conf.get("old.key13"), conf.get("new.key13"));
    // Key-site.xml , isFinal-true, prev.occurrence-deprecated,
    // isPrevFinal-false
    assertEquals("new.value14", conf.get("new.key14"));
    // check consistency in get of old and new keys
    assertEquals(conf.get("old.key14"), conf.get("new.key14"));
    // Key-site.xml , isFinal-false, prev.occurrence-deprecated,
    // isPrevFinal-true
    assertEquals("old.value15", conf.get("new.key15"));
    // check consistency in get of old and new keys
    assertEquals(conf.get("old.key15"), conf.get("new.key15"));
    // Key-site.xml , isFinal-false, prev.occurrence-deprecated,
    // isPrevFinal-false
    assertEquals("old.value16", conf.get("new.key16"));
    // check consistency in get of old and new keys
    assertEquals(conf.get("old.key16"), conf.get("new.key16"));
    
    // ensure that reloadConfiguration doesn't deprecation information
    conf.reloadConfiguration();
    assertEquals(conf.get("A"), "valueB");
    // check consistency in get of old and new keys
    assertEquals(conf.get("A"), conf.get("B"));
    
    // check for consistency in get and set of deprecated and corresponding 
    // new keys from the user code
    // set old key
    conf.set("C", "valueC");
    // get new key
    assertEquals("valueC",conf.get("D"));
    // check consistency in get of old and new keys
    assertEquals(conf.get("C"), conf.get("D"));
    
    // set new key
    conf.set("F","valueF");
    // get old key
    assertEquals("valueF", conf.get("E"));
    // check consistency in get of old and new keys
    assertEquals(conf.get("E"), conf.get("F"));
    
    conf.set("G", "valueG");
    assertEquals("valueG", conf.get("G"));
    assertEquals("valueG", conf.get("H"));
    assertEquals("valueG", conf.get("I"));
  }

}
- 
2.19.1.windows.1

