From 67c006c322c3925b42322f6ced841a54084f582a Mon Sep 17 00:00:00 2001
From: Suresh Srinivas <suresh@apache.org>
Date: Sat, 24 Apr 2010 00:01:27 +0000
Subject: [PATCH] HADOOP-6521. User specified umask using deprecated dfs.umask
 must override server configured using new dfs.umaskmode for backward
 compatibility. Contributed by Suresh Srinivas.

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@937559 13f79535-47bb-0310-9956-ffa450edef68
--
 CHANGES.txt                                   |  4 ++
 .../org/apache/hadoop/conf/Configuration.java | 19 -------
 .../hadoop/fs/permission/FsPermission.java    | 53 +++++++++++++------
 .../conf/TestConfigurationDeprecation.java    | 24 ---------
 .../hadoop/conf/TestDeprecatedKeys.java       | 10 ----
 5 files changed, 40 insertions(+), 70 deletions(-)

diff --git a/CHANGES.txt b/CHANGES.txt
index 34be5b663b0..d833ec3d029 100644
-- a/CHANGES.txt
++ b/CHANGES.txt
@@ -276,6 +276,10 @@ Trunk (unreleased changes)
     HADOOP-6540. Contrib unit tests have invalid XML for core-site, etc.
     (Aaron Kimball via tomwhite)
 
    HADOOP-6521. User specified umask using deprecated dfs.umask must override
    server configured using new dfs.umaskmode for backward compatibility.
    (suresh)
    
     HADOOP-6522. Fix decoding of codepoint zero in UTF8. (cutting)
 
     HADOOP-6505. Use tr rather than sed to effect literal substitution in the
diff --git a/src/java/org/apache/hadoop/conf/Configuration.java b/src/java/org/apache/hadoop/conf/Configuration.java
index 5db9f19d21b..d49fd1c3825 100644
-- a/src/java/org/apache/hadoop/conf/Configuration.java
++ b/src/java/org/apache/hadoop/conf/Configuration.java
@@ -295,20 +295,6 @@ private static boolean isDeprecated(String key) {
     return deprecatedKeyMap.containsKey(key);
   }
  
  /**
   * Check whether or not the deprecated key has been specified in the
   * configuration file rather than the new key
   * 
   * Returns false if the specified key is not included in the deprecated
   * key mapping.
   * 
   * @param oldKey Old configuration key 
   * @return If the old configuration key was specified rather than the new one
   */
  public boolean deprecatedKeyWasSet(String oldKey) {
    return isDeprecated(oldKey) && deprecatedKeyMap.get(oldKey).accessed;
  }
  
   /**
    * Checks for the presence of the property <code>name</code> in the
    * deprecation map. Returns the first of the list of new keys if present
@@ -1876,11 +1862,6 @@ private static void addDeprecatedKeys() {
                new String[]{CommonConfigurationKeys.NET_TOPOLOGY_CONFIGURED_NODE_MAPPING_KEY});
     Configuration.addDeprecation("topology.node.switch.mapping.impl", 
                new String[]{CommonConfigurationKeys.NET_TOPOLOGY_NODE_SWITCH_MAPPING_IMPL_KEY});
    Configuration.addDeprecation("dfs.umask", 
               new String[]{CommonConfigurationKeys.FS_PERMISSIONS_UMASK_KEY},
               "dfs.umask is deprecated, use " + 
               CommonConfigurationKeys.FS_PERMISSIONS_UMASK_KEY + 
               " with octal or symbolic specifications.");
     Configuration.addDeprecation("dfs.df.interval", 
                new String[]{CommonConfigurationKeys.FS_DF_INTERVAL_KEY});
     Configuration.addDeprecation("dfs.client.buffer.dir", 
diff --git a/src/java/org/apache/hadoop/fs/permission/FsPermission.java b/src/java/org/apache/hadoop/fs/permission/FsPermission.java
index 572441c5b6b..b5f9d7be3ac 100644
-- a/src/java/org/apache/hadoop/fs/permission/FsPermission.java
++ b/src/java/org/apache/hadoop/fs/permission/FsPermission.java
@@ -21,6 +21,8 @@
 import java.io.DataOutput;
 import java.io.IOException;
 
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.CommonConfigurationKeys;
 import org.apache.hadoop.io.Writable;
@@ -31,6 +33,8 @@
  * A class for file/directory permissions.
  */
 public class FsPermission implements Writable {
  private static final Log LOG = LogFactory.getLog(FsPermission.class);

   static final WritableFactory FACTORY = new WritableFactory() {
     public Writable newInstance() { return new FsPermission(); }
   };
@@ -199,25 +203,39 @@ public FsPermission applyUMask(FsPermission umask) {
   public static FsPermission getUMask(Configuration conf) {
     int umask = DEFAULT_UMASK;
     
    // Attempt to pull value from configuration, trying new key first and then
    // deprecated key, along with a warning, if not present
    // To ensure backward compatibility first use the deprecated key.
    // If the deprecated key is not present then check for the new key
     if(conf != null) {
       String confUmask = conf.get(UMASK_LABEL);
      if(confUmask != null) { // UMASK_LABEL is set
        try {
          if(conf.deprecatedKeyWasSet(DEPRECATED_UMASK_LABEL)) 
            umask = Integer.parseInt(confUmask); // Evaluate as decimal value
          else
            return new FsPermission(confUmask);
        } catch(IllegalArgumentException iae) {
          // Provide more explanation for user-facing message
          String type = iae instanceof NumberFormatException ? "decimal" 
                                                          : "octal or symbolic";
          
          throw new IllegalArgumentException("Unable to parse " + confUmask + 
                                              " as " + type + " umask.");
      int oldUmask = conf.getInt(DEPRECATED_UMASK_LABEL, Integer.MIN_VALUE);
      try {
        if(confUmask != null) {
          umask = new UmaskParser(confUmask).getUMask();
        }
      } catch(IllegalArgumentException iae) {
        // Provide more explanation for user-facing message
        String type = iae instanceof NumberFormatException ? "decimal"
            : "octal or symbolic";
        String error = "Unable to parse configuration " + UMASK_LABEL
            + " with value " + confUmask + " as " + type + " umask.";
        LOG.warn(error);
        
        // If oldUmask is not set, then throw the exception
        if (oldUmask == Integer.MIN_VALUE) {
          throw new IllegalArgumentException(error);
         }
      } 
      }
        
      if(oldUmask != Integer.MIN_VALUE) { // Property was set with old key
        if (umask != oldUmask) {
          LOG.warn(DEPRECATED_UMASK_LABEL
              + " configuration key is deprecated. " + "Convert to "
              + UMASK_LABEL + ", using octal or symbolic umask "
              + "specifications.");
          // Old and new umask values do not match - Use old umask
          umask = oldUmask;
        }
      }
     }
     
     return new FsPermission((short)umask);
@@ -229,7 +247,8 @@ public boolean getStickyBit() {
 
   /** Set the user file creation mask (umask) */
   public static void setUMask(Configuration conf, FsPermission umask) {
    conf.setInt(UMASK_LABEL, umask.toShort());
    conf.set(UMASK_LABEL, String.format("%1$03o", umask.toShort()));
    conf.setInt(DEPRECATED_UMASK_LABEL, umask.toShort());
   }
 
   /** Get the default permission. */
diff --git a/src/test/core/org/apache/hadoop/conf/TestConfigurationDeprecation.java b/src/test/core/org/apache/hadoop/conf/TestConfigurationDeprecation.java
index 21c7d7955b1..a55781e49d6 100644
-- a/src/test/core/org/apache/hadoop/conf/TestConfigurationDeprecation.java
++ b/src/test/core/org/apache/hadoop/conf/TestConfigurationDeprecation.java
@@ -249,28 +249,4 @@ public void testDeprecationForFinalParameters() throws IOException {
     assertNull(conf.get("I"));
     assertNull(conf.get("J"));
   }
  
  // Ensure that wasDeprecatedKeySet returns the correct result under
  // the three code paths possible 
  @Test
  public void testWasDeprecatedKeySet() {
    Configuration.addDeprecation("oldKeyA", new String [] { "newKeyA"});
    Configuration.addDeprecation("oldKeyB", new String [] { "newKeyB"});
    
    // Used the deprecated key rather than the new, therefore should trigger
    conf.set("oldKeyA", "AAA");
    assertEquals("AAA", conf.get("newKeyA"));
    assertTrue(conf.deprecatedKeyWasSet("oldKeyA"));
  
    // There is a deprecated key, but it wasn't specified. Therefore, don't trigger
    conf.set("newKeyB", "AndrewBird");
    assertEquals("AndrewBird", conf.get("newKeyB"));
    assertFalse(conf.deprecatedKeyWasSet("oldKeyB"));
    
    // Not a deprecated key, therefore shouldn't trigger deprecatedKeyWasSet
    conf.set("BrandNewKey", "BrandNewValue");
    assertEquals("BrandNewValue", conf.get("BrandNewKey"));
    assertFalse(conf.deprecatedKeyWasSet("BrandNewKey"));
  }

 }
diff --git a/src/test/core/org/apache/hadoop/conf/TestDeprecatedKeys.java b/src/test/core/org/apache/hadoop/conf/TestDeprecatedKeys.java
index 93c5d80346e..7008544f7b8 100644
-- a/src/test/core/org/apache/hadoop/conf/TestDeprecatedKeys.java
++ b/src/test/core/org/apache/hadoop/conf/TestDeprecatedKeys.java
@@ -20,8 +20,6 @@
 
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.log4j.Level;
 
 import junit.framework.TestCase;
 
@@ -33,13 +31,5 @@ public void testDeprecatedKeys() throws Exception {
     conf.set("topology.script.file.name", "xyz");
     String scriptFile = conf.get(CommonConfigurationKeys.NET_TOPOLOGY_SCRIPT_FILE_NAME_KEY);
     assertTrue(scriptFile.equals("xyz")) ;
    int m = conf.getInt(CommonConfigurationKeys.FS_PERMISSIONS_UMASK_KEY, CommonConfigurationKeys.FS_PERMISSIONS_UMASK_DEFAULT) ;
    assertTrue(m == 0022) ;
    conf.setInt("dfs.umask", 0077);
    m = conf.getInt(CommonConfigurationKeys.FS_PERMISSIONS_UMASK_KEY, CommonConfigurationKeys.FS_PERMISSIONS_UMASK_DEFAULT) ;
    assertTrue(m == 0077) ;
    conf.set(CommonConfigurationKeys.FS_PERMISSIONS_UMASK_KEY, "405");
    String umask = conf.get(CommonConfigurationKeys.FS_PERMISSIONS_UMASK_KEY);
    assertTrue(umask.equals("405"));
   }
 }
- 
2.19.1.windows.1

