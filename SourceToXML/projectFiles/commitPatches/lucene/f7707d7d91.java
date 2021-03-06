From f7707d7d912dd4f192eb48e758d74f52858b097a Mon Sep 17 00:00:00 2001
From: "Chris M. Hostetter" <hossman@apache.org>
Date: Fri, 1 Jun 2012 22:20:18 +0000
Subject: [PATCH] SOLR-2796: uniqueKey field can no longer be populated via
 <copyField/> or <field default=...>.  Also corrected CHANGES.txt entry for
 related SOLR-3495

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1345376 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              | 13 ++++++-
 .../org/apache/solr/schema/IndexSchema.java   | 20 ++++++++++-
 ...bad-schema-uniquekey-is-copyfield-dest.xml | 36 +++++++++++++++++++
 .../bad-schema-uniquekey-uses-default.xml     | 33 +++++++++++++++++
 .../solr/schema/BadIndexSchemaTest.java       | 21 ++++++-----
 5 files changed, 112 insertions(+), 11 deletions(-)
 create mode 100644 solr/core/src/test-files/solr/conf/bad-schema-uniquekey-is-copyfield-dest.xml
 create mode 100644 solr/core/src/test-files/solr/conf/bad-schema-uniquekey-uses-default.xml

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index c6b0020e007..4936f58cc45 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -89,6 +89,14 @@ Upgrading from Solr 3.6-dev
   paths have been fixed to be resolved against the data dir.  See the example 
   solrconfig.xml and SOLR-1258 for more details.
 
* Due to low level changes to support SolrCloud, the uniqueKey field can no 
  longer be populated via <copyField/> or <field default=...> in the 
  schema.xml.  Users wishing to have Solr automaticly generate a uniqueKey 
  value when adding documents should instead use an instance of
  solr.UUIDUpdateProcessorFactory in their update processor chain.  See 
  SOLR-2796 for more details.


 Detailed Change List
 ----------------------
 
@@ -320,7 +328,7 @@ New Features
   prior "FieldName^boost" syntax is still accepted.  In such cases the value on the
   "ps" parameter serves as the default slop.  (Ron Mayer via James Dyer)
 
* SOLR-2796: New UpdateProcessors have been added to create default values for 
* SOLR-3495: New UpdateProcessors have been added to create default values for 
   configured fields.  These works similarly to the <field default="..."/> 
   option in schema.xml, but are applied in the UpdateProcessorChain, so they 
   may be used prior to other UpdateProcessors, or to generate a uniqueKey field 
@@ -609,6 +617,9 @@ Other Changes
 * SOLR-3083: JMX beans now report Numbers as numeric values rather then String
   (Tagged Siteops, Greg Bowyer via ryan)
 
* SOLR-2796: Due to low level changes to support SolrCloud, the uniqueKey 
  field can no longer be populated via <copyField/> or <field default=...>
  in the schema.xml.
 
 Documentation
 ----------------------
diff --git a/solr/core/src/java/org/apache/solr/schema/IndexSchema.java b/solr/core/src/java/org/apache/solr/schema/IndexSchema.java
index 6b48187b58a..4b53d7b5d0c 100644
-- a/solr/core/src/java/org/apache/solr/schema/IndexSchema.java
++ b/solr/core/src/java/org/apache/solr/schema/IndexSchema.java
@@ -1,4 +1,4 @@
/**
/*
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
@@ -465,6 +465,14 @@ public final class IndexSchema {
       log.warn("no uniqueKey specified in schema.");
     } else {
       uniqueKeyField=getIndexedField(node.getNodeValue().trim());
      if (null != uniqueKeyField.getDefaultValue()) {
        String msg = "uniqueKey field ("+uniqueKeyFieldName+
          ") can not be configured with a default value ("+
          uniqueKeyField.getDefaultValue()+")";
        log.error(msg);
        throw new SolrException( SolrException.ErrorCode.SERVER_ERROR, msg );
      }

       if (!uniqueKeyField.stored()) {
         log.error("uniqueKey is not stored - distributed search will not work");
       }
@@ -507,6 +515,14 @@ public final class IndexSchema {
           }
         }
 
        if (dest.equals(uniqueKeyFieldName)) {
          String msg = "uniqueKey field ("+uniqueKeyFieldName+
            ") can not be the dest of a copyField (src="+source+")";
          log.error(msg);
          throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, msg);
          
        }

         registerCopyField(source, dest, maxCharsInt);
      }
       
@@ -517,6 +533,8 @@ public final class IndexSchema {
                       entry.getValue()+")");
         }
       }


       //Run the callbacks on SchemaAware now that everything else is done
       for (SchemaAware aware : schemaAware) {
         aware.inform(this);
diff --git a/solr/core/src/test-files/solr/conf/bad-schema-uniquekey-is-copyfield-dest.xml b/solr/core/src/test-files/solr/conf/bad-schema-uniquekey-is-copyfield-dest.xml
new file mode 100644
index 00000000000..bf1d53212e4
-- /dev/null
++ b/solr/core/src/test-files/solr/conf/bad-schema-uniquekey-is-copyfield-dest.xml
@@ -0,0 +1,36 @@
<?xml version="1.0" ?>
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

<schema name="bad-schema-uniquekey-is-copyfield-dest" version="1.4">
  <types>
    <fieldType name="string" class="solr.StrField"/>
 </types>

 <fields>
   <field name="id" type="string" indexed="true" stored="true"/>
   <field name="XXX" type="string" indexed="true" stored="true"/>

   <!-- BEGIN BAD STUFF -->
   <copyField source="XXX" dest="id"/>
   <!-- END BAD STUFF -->
 </fields>

 <defaultSearchField>id</defaultSearchField>
 <uniqueKey>id</uniqueKey>

</schema>
diff --git a/solr/core/src/test-files/solr/conf/bad-schema-uniquekey-uses-default.xml b/solr/core/src/test-files/solr/conf/bad-schema-uniquekey-uses-default.xml
new file mode 100644
index 00000000000..026b529a942
-- /dev/null
++ b/solr/core/src/test-files/solr/conf/bad-schema-uniquekey-uses-default.xml
@@ -0,0 +1,33 @@
<?xml version="1.0" ?>
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

<schema name="bad-schema-uniquekey-uses-default" version="1.4">
  <types>
    <fieldType name="string" class="solr.StrField"/>
 </types>

 <fields>
   <!-- BEGIN BAD STUFF -->
   <field name="id" type="string" indexed="true" stored="true" default="XXX"/>
   <!-- END BAD STUFF -->
 </fields>

 <defaultSearchField>id</defaultSearchField>
 <uniqueKey>id</uniqueKey>

</schema>
diff --git a/solr/core/src/test/org/apache/solr/schema/BadIndexSchemaTest.java b/solr/core/src/test/org/apache/solr/schema/BadIndexSchemaTest.java
index 1076a327402..49dcd7086e3 100644
-- a/solr/core/src/test/org/apache/solr/schema/BadIndexSchemaTest.java
++ b/solr/core/src/test/org/apache/solr/schema/BadIndexSchemaTest.java
@@ -1,4 +1,4 @@
/**
/*
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
@@ -38,7 +38,8 @@ public class BadIndexSchemaTest extends SolrTestCaseJ4 {
       // short circuit out if we found what we expected
       if (-1 != e.getMessage().indexOf(errString)) return;
       // Test the cause too in case the expected error is wrapped
      if (-1 != e.getCause().getMessage().indexOf(errString)) return;
      if (null != e.getCause() && 
          -1 != e.getCause().getMessage().indexOf(errString)) return;
 
       // otherwise, rethrow it, possibly completley unrelated
       throw new SolrException
@@ -50,7 +51,6 @@ public class BadIndexSchemaTest extends SolrTestCaseJ4 {
     fail("Did not encounter any exception from: " + schema);
   }
 
  @Test
   public void testSevereErrorsForInvalidFieldOptions() throws Exception {
     doTest("bad-schema-not-indexed-but-norms.xml", "bad_field");
     doTest("bad-schema-not-indexed-but-tf.xml", "bad_field");
@@ -58,29 +58,32 @@ public class BadIndexSchemaTest extends SolrTestCaseJ4 {
     doTest("bad-schema-omit-tf-but-not-pos.xml", "bad_field");
   }
 
  @Test
   public void testSevereErrorsForDuplicateFields() throws Exception {
     doTest("bad-schema-dup-field.xml", "fAgain");
   }
 
  @Test
   public void testSevereErrorsForDuplicateDynamicField() throws Exception {
     doTest("bad-schema-dup-dynamicField.xml", "_twice");
   }
 
  @Test
   public void testSevereErrorsForDuplicateFieldType() throws Exception {
     doTest("bad-schema-dup-fieldType.xml", "ftAgain");
   }
 
  @Test
   public void testSevereErrorsForUnexpectedAnalyzer() throws Exception {
     doTest("bad-schema-nontext-analyzer.xml", "StrField (bad_type)");
   }
 
  @Test
   public void testBadExternalFileField() throws Exception {
     doTest("bad-schema-external-filefield.xml",
        "Only float and pfloat");
           "Only float and pfloat");
   }

  public void testUniqueKeyRules() throws Exception {
    doTest("bad-schema-uniquekey-is-copyfield-dest.xml", 
           "can not be the dest of a copyField");
    doTest("bad-schema-uniquekey-uses-default.xml", 
           "can not be configured with a default value");
  }

 }
- 
2.19.1.windows.1

