From 9180eca59258fe07ee067c66e432cf322148025a Mon Sep 17 00:00:00 2001
From: Alejandro Abdelnur <tucu@apache.org>
Date: Wed, 14 Mar 2012 17:07:26 +0000
Subject: [PATCH] HADOOP-8167. Configuration deprecation logic breaks backwards
 compatibility (tucu)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1300642 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |  2 +
 .../org/apache/hadoop/conf/Configuration.java | 49 ++++++++++++-----
 .../conf/TestConfigurationDeprecation.java    | 54 +++++++++++++++++++
 3 files changed, 91 insertions(+), 14 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 1b76cf1a031..68065faf78b 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -232,6 +232,8 @@ Release 0.23.3 - UNRELEASED
     HADOOP-8169.  javadoc generation fails with java.lang.OutOfMemoryError:
     Java heap space (tgraves via bobby)
 
    HADOOP-8167. Configuration deprecation logic breaks backwards compatibility (tucu)

   BREAKDOWN OF HADOOP-7454 SUBTASKS
 
     HADOOP-7455. HA: Introduce HA Service Protocol Interface. (suresh)
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/conf/Configuration.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/conf/Configuration.java
index d71aaf58106..3d8c3a5d450 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/conf/Configuration.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/conf/Configuration.java
@@ -306,7 +306,26 @@ public synchronized static void addDeprecation(String key, String[] newKeys) {
   private static boolean isDeprecated(String key) {
     return deprecatedKeyMap.containsKey(key);
   }
 

  /**
   * Returns the alternate name for a key if the property name is deprecated
   * or if deprecates a property name.
   *
   * @param name property name.
   * @return alternate name.
   */
  private String getAlternateName(String name) {
    String altName;
    DeprecatedKeyInfo keyInfo = deprecatedKeyMap.get(name);
    if (keyInfo != null) {
      altName = (keyInfo.newKeys.length > 0) ? keyInfo.newKeys[0] : null;
    }
    else {
      altName = reverseDeprecatedKeyMap.get(name);
    }
    return altName;
  }

   /**
    * Checks for the presence of the property <code>name</code> in the
    * deprecation map. Returns the first of the list of new keys if present
@@ -619,8 +638,8 @@ public String getRaw(String name) {
 
   /** 
    * Set the <code>value</code> of the <code>name</code> property. If 
   * <code>name</code> is deprecated, it sets the <code>value</code> to the keys
   * that replace the deprecated key.
   * <code>name</code> is deprecated or there is a deprecated name associated to it,
   * it sets the value to both names.
    * 
    * @param name property name.
    * @param value property value.
@@ -629,18 +648,17 @@ public void set(String name, String value) {
     if (deprecatedKeyMap.isEmpty()) {
       getProps();
     }
    if (!isDeprecated(name)) {
      getOverlay().setProperty(name, value);
      getProps().setProperty(name, value);
      updatingResource.put(name, UNKNOWN_RESOURCE);
    getOverlay().setProperty(name, value);
    getProps().setProperty(name, value);
    updatingResource.put(name, UNKNOWN_RESOURCE);
    String altName = getAlternateName(name);
    if (altName != null) {
      getOverlay().setProperty(altName, value);
      getProps().setProperty(altName, value);
     }
    else {
    if (isDeprecated(name)) {
       DeprecatedKeyInfo keyInfo = deprecatedKeyMap.get(name);
       LOG.warn(keyInfo.getWarningMessage(name));
      for (String newKey : keyInfo.newKeys) {
        getOverlay().setProperty(newKey, value);
        getProps().setProperty(newKey, value);
      }
     }
   }
   
@@ -648,10 +666,13 @@ public void set(String name, String value) {
    * Unset a previously set property.
    */
   public synchronized void unset(String name) {
    name = handleDeprecation(name);

    String altName = getAlternateName(name);
     getOverlay().remove(name);
     getProps().remove(name);
    if (altName !=null) {
      getOverlay().remove(altName);
       getProps().remove(altName);
    }
   }
 
   /**
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfigurationDeprecation.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfigurationDeprecation.java
index a6f95ef097e..e5a7748d5cf 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfigurationDeprecation.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfigurationDeprecation.java
@@ -20,6 +20,7 @@
 
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
 import static org.junit.Assert.assertNull;
 import static org.junit.Assert.assertTrue;
 
@@ -27,6 +28,7 @@
 import java.io.File;
 import java.io.FileWriter;
 import java.io.IOException;
import java.util.Map;
 
 import org.apache.hadoop.fs.Path;
 import org.junit.After;
@@ -270,4 +272,56 @@ public void testSetBeforeAndGetAfterDeprecationAndDefaults() {
         new String[]{ "tests.fake-default.new-key" });
     assertEquals("hello", conf.get("tests.fake-default.new-key"));
   }

  @Test
  public void testIteratorWithDeprecatedKeys() {
    Configuration conf = new Configuration();
    Configuration.addDeprecation("dK", new String[]{"nK"});
    conf.set("k", "v");
    conf.set("dK", "V");
    assertEquals("V", conf.get("dK"));
    assertEquals("V", conf.get("nK"));
    conf.set("nK", "VV");
    assertEquals("VV", conf.get("dK"));
    assertEquals("VV", conf.get("nK"));
    boolean kFound = false;
    boolean dKFound = false;
    boolean nKFound = false;
    for (Map.Entry<String, String> entry : conf) {
      if (entry.getKey().equals("k")) {
        assertEquals("v", entry.getValue());
        kFound = true;
      }
      if (entry.getKey().equals("dK")) {
        assertEquals("VV", entry.getValue());
        dKFound = true;
      }
      if (entry.getKey().equals("nK")) {
        assertEquals("VV", entry.getValue());
        nKFound = true;
      }
    }
    assertTrue("regular Key not found", kFound);
    assertTrue("deprecated Key not found", dKFound);
    assertTrue("new Key not found", nKFound);
  }

  @Test
  public void testUnsetWithDeprecatedKeys() {
    Configuration conf = new Configuration();
    Configuration.addDeprecation("dK", new String[]{"nK"});
    conf.set("nK", "VV");
    assertEquals("VV", conf.get("dK"));
    assertEquals("VV", conf.get("nK"));
    conf.unset("dK");
    assertNull(conf.get("dK"));
    assertNull(conf.get("nK"));
    conf.set("nK", "VV");
    assertEquals("VV", conf.get("dK"));
    assertEquals("VV", conf.get("nK"));
    conf.unset("nK");
    assertNull(conf.get("dK"));
    assertNull(conf.get("nK"));
  }

 }
- 
2.19.1.windows.1

