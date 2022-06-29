From 7d1b804d3a31c644b1af9fc4f7917f1f25f793d9 Mon Sep 17 00:00:00 2001
From: Robert Joseph Evans <bobby@apache.org>
Date: Tue, 1 May 2012 21:00:52 +0000
Subject: [PATCH] HADOOP-8172. Configuration no longer sets all keys in a
 deprecated key list. (Anupam Seth via bobby)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1332821 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |   3 +
 .../org/apache/hadoop/conf/Configuration.java | 141 ++++++++++++++----
 .../conf/TestConfigurationDeprecation.java    |   4 +-
 .../hadoop/conf/TestDeprecatedKeys.java       |  50 +++++++
 4 files changed, 164 insertions(+), 34 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 8b5d1f90d4a..189ddd3cc4f 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -447,6 +447,9 @@ Release 2.0.0 - UNRELEASED
     HADOOP-8317. Update maven-assembly-plugin to 2.3 - fix build on FreeBSD
     (Radim Kolar via bobby)
 
    HADOOP-8172. Configuration no longer sets all keys in a deprecated key 
    list. (Anupam Seth via bobby)

 Release 0.23.3 - UNRELEASED
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/conf/Configuration.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/conf/Configuration.java
index 99af904a1c2..044e5cb08a3 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/conf/Configuration.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/conf/Configuration.java
@@ -33,6 +33,7 @@
 import java.net.InetSocketAddress;
 import java.net.URL;
 import java.util.ArrayList;
import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Enumeration;
@@ -269,10 +270,18 @@ private final String getWarningMessage(String key) {
    * This is to be used only by the developers in order to add deprecation of
    * keys, and attempts to call this method after loading resources once,
    * would lead to <tt>UnsupportedOperationException</tt>
   * 
   * If a key is deprecated in favor of multiple keys, they are all treated as 
   * aliases of each other, and setting any one of them resets all the others 
   * to the new value.
   * 
    * @param key
    * @param newKeys
    * @param customMessage
   * @deprecated use {@link addDeprecation(String key, String newKey,
      String customMessage)} instead
    */
  @Deprecated
   public synchronized static void addDeprecation(String key, String[] newKeys,
       String customMessage) {
     if (key == null || key.length() == 0 ||
@@ -288,6 +297,22 @@ public synchronized static void addDeprecation(String key, String[] newKeys,
       }
     }
   }
  
  /**
   * Adds the deprecated key to the deprecation map.
   * It does not override any existing entries in the deprecation map.
   * This is to be used only by the developers in order to add deprecation of
   * keys, and attempts to call this method after loading resources once,
   * would lead to <tt>UnsupportedOperationException</tt>
   * 
   * @param key
   * @param newKey
   * @param customMessage
   */
  public synchronized static void addDeprecation(String key, String newKey,
	      String customMessage) {
	  addDeprecation(key, new String[] {newKey}, customMessage);
  }
 
   /**
    * Adds the deprecated key to the deprecation map when no custom message
@@ -297,13 +322,34 @@ public synchronized static void addDeprecation(String key, String[] newKeys,
    * keys, and attempts to call this method after loading resources once,
    * would lead to <tt>UnsupportedOperationException</tt>
    * 
   * If a key is deprecated in favor of multiple keys, they are all treated as 
   * aliases of each other, and setting any one of them resets all the others 
   * to the new value.
   * 
    * @param key Key that is to be deprecated
    * @param newKeys list of keys that take up the values of deprecated key
   * @deprecated use {@link addDeprecation(String key, String newKey)} instead
    */
  @Deprecated
   public synchronized static void addDeprecation(String key, String[] newKeys) {
     addDeprecation(key, newKeys, null);
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
   * @param newKey key that takes up the value of deprecated key
   */
  public synchronized static void addDeprecation(String key, String newKey) {
	addDeprecation(key, new String[] {newKey}, null);
  }
  
   /**
    * checks whether the given <code>key</code> is deprecated.
    * 
@@ -322,16 +368,26 @@ public static boolean isDeprecated(String key) {
    * @param name property name.
    * @return alternate name.
    */
  private String getAlternateName(String name) {
    String altName;
  private String[] getAlternateNames(String name) {
    String oldName, altNames[] = null;
     DeprecatedKeyInfo keyInfo = deprecatedKeyMap.get(name);
    if (keyInfo != null) {
      altName = (keyInfo.newKeys.length > 0) ? keyInfo.newKeys[0] : null;
    }
    else {
      altName = reverseDeprecatedKeyMap.get(name);
    if (keyInfo == null) {
      altNames = (reverseDeprecatedKeyMap.get(name) != null ) ? 
        new String [] {reverseDeprecatedKeyMap.get(name)} : null;
      if(altNames != null && altNames.length > 0) {
    	//To help look for other new configs for this deprecated config
    	keyInfo = deprecatedKeyMap.get(altNames[0]);
      }      
    } 
    if(keyInfo != null && keyInfo.newKeys.length > 0) {
      List<String> list = new ArrayList<String>(); 
      if(altNames != null) {
    	  list.addAll(Arrays.asList(altNames));
      }
      list.addAll(Arrays.asList(keyInfo.newKeys));
      altNames = list.toArray(new String[list.size()]);
     }
    return altName;
    return altNames;
   }
 
   /**
@@ -346,24 +402,29 @@ private String getAlternateName(String name) {
    * @return the first property in the list of properties mapping
    *         the <code>name</code> or the <code>name</code> itself.
    */
  private String handleDeprecation(String name) {
    if (isDeprecated(name)) {
  private String[] handleDeprecation(String name) {
    ArrayList<String > names = new ArrayList<String>();
	if (isDeprecated(name)) {
       DeprecatedKeyInfo keyInfo = deprecatedKeyMap.get(name);
       warnOnceIfDeprecated(name);
       for (String newKey : keyInfo.newKeys) {
         if(newKey != null) {
          name = newKey;
          break;
          names.add(newKey);
         }
       }
     }
    String deprecatedKey = reverseDeprecatedKeyMap.get(name);
    if (deprecatedKey != null && !getOverlay().containsKey(name) &&
        getOverlay().containsKey(deprecatedKey)) {
      getProps().setProperty(name, getOverlay().getProperty(deprecatedKey));
      getOverlay().setProperty(name, getOverlay().getProperty(deprecatedKey));
    if(names.size() == 0) {
    	names.add(name);
     }
    return name;
    for(String n : names) {
	  String deprecatedKey = reverseDeprecatedKeyMap.get(n);
	  if (deprecatedKey != null && !getOverlay().containsKey(n) &&
	      getOverlay().containsKey(deprecatedKey)) {
	    getProps().setProperty(n, getOverlay().getProperty(deprecatedKey));
	    getOverlay().setProperty(n, getOverlay().getProperty(deprecatedKey));
	  }
    }
    return names.toArray(new String[names.size()]);
   }
  
   private void handleDeprecation() {
@@ -595,8 +656,12 @@ private String substituteVars(String expr) {
    *         or null if no such property exists.
    */
   public String get(String name) {
    name = handleDeprecation(name);
    return substituteVars(getProps().getProperty(name));
    String[] names = handleDeprecation(name);
    String result = null;
    for(String n : names) {
      result = substituteVars(getProps().getProperty(n));
    }
    return result;
   }
   
   /**
@@ -633,8 +698,12 @@ public String getTrimmed(String name) {
    *         its replacing property and null if no such property exists.
    */
   public String getRaw(String name) {
    name = handleDeprecation(name);
    return getProps().getProperty(name);
    String[] names = handleDeprecation(name);
    String result = null;
    for(String n : names) {
      result = getProps().getProperty(n);
    }
    return result;
   }
 
   /** 
@@ -652,10 +721,12 @@ public void set(String name, String value) {
     getOverlay().setProperty(name, value);
     getProps().setProperty(name, value);
     updatingResource.put(name, UNKNOWN_RESOURCE);
    String altName = getAlternateName(name);
    if (altName != null) {
      getOverlay().setProperty(altName, value);
      getProps().setProperty(altName, value);
    String[] altNames = getAlternateNames(name);
    if (altNames != null && altNames.length > 0) {
      for(String altName : altNames) {
    	getOverlay().setProperty(altName, value);
        getProps().setProperty(altName, value);
      }
     }
     warnOnceIfDeprecated(name);
   }
@@ -671,12 +742,14 @@ private void warnOnceIfDeprecated(String name) {
    * Unset a previously set property.
    */
   public synchronized void unset(String name) {
    String altName = getAlternateName(name);
    String[] altNames = getAlternateNames(name);
     getOverlay().remove(name);
     getProps().remove(name);
    if (altName !=null) {
      getOverlay().remove(altName);
       getProps().remove(altName);
    if (altNames !=null && altNames.length > 0) {
      for(String altName : altNames) {
    	getOverlay().remove(altName);
    	getProps().remove(altName);
      }
     }
   }
 
@@ -711,8 +784,12 @@ private synchronized Properties getOverlay() {
    *         doesn't exist.                    
    */
   public String get(String name, String defaultValue) {
    name = handleDeprecation(name);
    return substituteVars(getProps().getProperty(name, defaultValue));
    String[] names = handleDeprecation(name);
    String result = null;
    for(String n : names) {
      result = substituteVars(getProps().getProperty(n, defaultValue));
    }
    return result;
   }
     
   /** 
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfigurationDeprecation.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfigurationDeprecation.java
index e5a7748d5cf..df346dd657b 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfigurationDeprecation.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfigurationDeprecation.java
@@ -164,7 +164,7 @@ public void testDeprecation() throws IOException {
     conf.set("Y", "y");
     conf.set("Z", "z");
     // get old key
    assertEquals("y", conf.get("X"));
    assertEquals("z", conf.get("X"));
   }
 
   /**
@@ -305,7 +305,7 @@ public void testIteratorWithDeprecatedKeys() {
     assertTrue("deprecated Key not found", dKFound);
     assertTrue("new Key not found", nKFound);
   }

  
   @Test
   public void testUnsetWithDeprecatedKeys() {
     Configuration conf = new Configuration();
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestDeprecatedKeys.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestDeprecatedKeys.java
index 584b3372b89..b8f820c024d 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestDeprecatedKeys.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestDeprecatedKeys.java
@@ -18,10 +18,15 @@
 
 package org.apache.hadoop.conf;
 
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

 import java.io.ByteArrayOutputStream;
import java.util.Map;
 
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.junit.Test;
 
 import junit.framework.TestCase;
 
@@ -53,4 +58,49 @@ public void testReadWriteWithDeprecatedKeys() throws Exception {
     assertTrue(fileContents.contains("old.config.yet.to.be.deprecated"));
     assertTrue(fileContents.contains("new.conf.to.replace.deprecated.conf"));
   }
  
  @Test
  public void testIteratorWithDeprecatedKeysMappedToMultipleNewKeys() {
    Configuration conf = new Configuration();
    Configuration.addDeprecation("dK", new String[]{"nK1", "nK2"});
    conf.set("k", "v");
    conf.set("dK", "V");
    assertEquals("V", conf.get("dK"));
    assertEquals("V", conf.get("nK1"));
    assertEquals("V", conf.get("nK2"));
    conf.set("nK1", "VV");
    assertEquals("VV", conf.get("dK"));
    assertEquals("VV", conf.get("nK1"));
    assertEquals("VV", conf.get("nK2"));
    conf.set("nK2", "VVV");
    assertEquals("VVV", conf.get("dK"));
    assertEquals("VVV", conf.get("nK2"));
    assertEquals("VVV", conf.get("nK1"));
    boolean kFound = false;
    boolean dKFound = false;
    boolean nK1Found = false;
    boolean nK2Found = false;
    for (Map.Entry<String, String> entry : conf) {
      if (entry.getKey().equals("k")) {
        assertEquals("v", entry.getValue());
        kFound = true;
      }
      if (entry.getKey().equals("dK")) {
        assertEquals("VVV", entry.getValue());
        dKFound = true;
      }
      if (entry.getKey().equals("nK1")) {
        assertEquals("VVV", entry.getValue());
        nK1Found = true;
      }
      if (entry.getKey().equals("nK2")) {
        assertEquals("VVV", entry.getValue());
        nK2Found = true;
      }
    }
    assertTrue("regular Key not found", kFound);
    assertTrue("deprecated Key not found", dKFound);
    assertTrue("new Key 1 not found", nK1Found);
    assertTrue("new Key 2 not found", nK2Found);
  }
 }
- 
2.19.1.windows.1

