From 4a4dd27571c44b2374d6a909a88bdd04817b0f11 Mon Sep 17 00:00:00 2001
From: Hemanth Yamijala <yhemanth@apache.org>
Date: Tue, 8 Sep 2009 10:56:15 +0000
Subject: [PATCH] HADOOP-6243. Fix a NullPointerException in processing
 deprecated keys. Contributed by Sreekanth Ramakrishnan.

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@812455 13f79535-47bb-0310-9956-ffa450edef68
--
 CHANGES.txt                                   |  3 ++
 src/java/core-default.xml                     |  7 ---
 .../org/apache/hadoop/conf/Configuration.java | 54 +++----------------
 .../conf/TestConfigurationDeprecation.java    | 48 ++++++++---------
 4 files changed, 31 insertions(+), 81 deletions(-)

diff --git a/CHANGES.txt b/CHANGES.txt
index f90a206c27d..75917b6261d 100644
-- a/CHANGES.txt
++ b/CHANGES.txt
@@ -984,6 +984,9 @@ Trunk (unreleased changes)
     HADOOP-6229. Attempt to make a directory under an existing file on
     LocalFileSystem should throw an Exception. (Boris Shkolnik via tomwhite)
 
    HADOOP-6243. Fix a NullPointerException in processing deprecated keys.
    (Sreekanth Ramakrishnan via yhemanth)

 Release 0.20.1 - Unreleased
 
   INCOMPATIBLE CHANGES
diff --git a/src/java/core-default.xml b/src/java/core-default.xml
index 9034a9e655e..9a2ae76ee0d 100644
-- a/src/java/core-default.xml
++ b/src/java/core-default.xml
@@ -485,11 +485,4 @@
     IP address.
   </description>
 </property>

<property>
  <name>hadoop.conf.extra.classes</name>
  <value>org.apache.hadoop.mapred.JobConf</value>
  <final>true</final>
</property>

 </configuration>
diff --git a/src/java/org/apache/hadoop/conf/Configuration.java b/src/java/org/apache/hadoop/conf/Configuration.java
index 8bf4c1c436e..d2572595842 100644
-- a/src/java/org/apache/hadoop/conf/Configuration.java
++ b/src/java/org/apache/hadoop/conf/Configuration.java
@@ -336,6 +336,12 @@ private String handleDeprecation(String name) {
     }
     addDefaultResource("core-default.xml");
     addDefaultResource("core-site.xml");
    //Add code for managing deprecated key mapping
    //for example
    //addDeprecation("oldKey1",new String[]{"newkey1","newkey2"});
    //adds deprecation for oldKey1 to two new keys(newkey1, newkey2).
    //so get or set of oldKey1 will correctly populate/access values of 
    //newkey1 and newkey2
   }
   
   private Properties properties;
@@ -1364,56 +1370,8 @@ private void loadResources(Properties properties,
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
diff --git a/src/test/core/org/apache/hadoop/conf/TestConfigurationDeprecation.java b/src/test/core/org/apache/hadoop/conf/TestConfigurationDeprecation.java
index bbb0e758c58..a5cb59de7ff 100644
-- a/src/test/core/org/apache/hadoop/conf/TestConfigurationDeprecation.java
++ b/src/test/core/org/apache/hadoop/conf/TestConfigurationDeprecation.java
@@ -81,30 +81,27 @@ void appendProperty(String name, String val, boolean isFinal)
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
  private void addDeprecationToConfiguration() {
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
   
   /**
@@ -123,8 +120,6 @@ void appendProperty(String name, String val, boolean isFinal)
   public void testDeprecation() throws IOException {
     out=new BufferedWriter(new FileWriter(CONFIG));
     startConfig();
    appendProperty("hadoop.conf.extra.classes", MyConf.class.getName()
        + ",myconf1");
     // load keys with default values. Some of them are set to final to
     // test the precedence order between deprecation and being final
     appendProperty("new.key1","default.value1",true);
@@ -145,6 +140,7 @@ public void testDeprecation() throws IOException {
     appendProperty("new.key16","default.value16");
     endConfig();
     Path fileResource = new Path(CONFIG);
    addDeprecationToConfiguration();
     conf.addResource(fileResource);
     
     out=new BufferedWriter(new FileWriter(CONFIG2));
- 
2.19.1.windows.1

