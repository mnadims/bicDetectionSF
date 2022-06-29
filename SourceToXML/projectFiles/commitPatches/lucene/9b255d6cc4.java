From 9b255d6cc4f105e97a559b8d402d802349fcd694 Mon Sep 17 00:00:00 2001
From: Steven Rowe <sarowe@apache.org>
Date: Wed, 25 Mar 2015 06:25:24 +0000
Subject: [PATCH] SOLR-6141: fix TestBulkSchemaConcurrent; fix field deletion
 to fail when a dynamic copy field directive has the field as its source;
 don't attempt to decrement a SchemaField's count in copyFieldTargetCounts if
 it's not present in the map.

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1669055 13f79535-47bb-0310-9956-ffa450edef68
--
 .../solr/schema/ManagedIndexSchema.java       | 22 ++++++++++++++-----
 .../solr/schema/TestBulkSchemaConcurrent.java |  6 ++---
 2 files changed, 20 insertions(+), 8 deletions(-)

diff --git a/solr/core/src/java/org/apache/solr/schema/ManagedIndexSchema.java b/solr/core/src/java/org/apache/solr/schema/ManagedIndexSchema.java
index f96a89fc0c2..0ddfb5969df 100644
-- a/solr/core/src/java/org/apache/solr/schema/ManagedIndexSchema.java
++ b/solr/core/src/java/org/apache/solr/schema/ManagedIndexSchema.java
@@ -449,9 +449,16 @@ public final class ManagedIndexSchema extends IndexSchema {
       for (String name : names) {
         SchemaField field = getFieldOrNull(name); 
         if (null != field) {
          if (copyFieldsMap.containsKey(name) || isCopyFieldTarget(field)) {
            throw new SolrException(ErrorCode.BAD_REQUEST, "Can't delete '" + name
                + "' because it's referred to by at least one copy field directive.");
          String message = "Can't delete field '" + name
              + "' because it's referred to by at least one copy field directive.";
          if (newSchema.copyFieldsMap.containsKey(name) || newSchema.isCopyFieldTarget(field)) {
            throw new SolrException(ErrorCode.BAD_REQUEST, message);
          }
          for (int i = 0 ; i < newSchema.dynamicCopyFields.length ; ++i) {
            DynamicCopy dynamicCopy = newSchema.dynamicCopyFields[i];
            if (name.equals(dynamicCopy.getRegex())) {
              throw new SolrException(ErrorCode.BAD_REQUEST, message);
            }
           }
           newSchema.fields.remove(name);
           newSchema.fieldsWithDefaultValue.remove(field);
@@ -844,7 +851,10 @@ public final class ManagedIndexSchema extends IndexSchema {
           DynamicCopy dynamicCopy = dynamicCopyFields[i];
           if (source.equals(dynamicCopy.getRegex()) && dest.equals(dynamicCopy.getDestFieldName())) {
             found = true;
            decrementCopyFieldTargetCount(dynamicCopy.getDestination().getPrototype());
            SchemaField destinationPrototype = dynamicCopy.getDestination().getPrototype();
            if (copyFieldTargetCounts.containsKey(destinationPrototype)) {
              decrementCopyFieldTargetCount(destinationPrototype);
            }
             if (dynamicCopyFields.length > 1) {
               DynamicCopy[] temp = new DynamicCopy[dynamicCopyFields.length - 1];
               System.arraycopy(dynamicCopyFields, 0, temp, 0, i);
@@ -1126,7 +1136,9 @@ public final class ManagedIndexSchema extends IndexSchema {
         if (typeName.equals(destinationPrototype.getType().getTypeName())
             || (null != sourceDynamicBase && typeName.equals(sourceDynamicBase.getPrototype().getType().getTypeName()))) {
           dynamicCopyFieldsToRebuild.add(dynamicCopy);
          newSchema.decrementCopyFieldTargetCount(destinationPrototype);
          if (newSchema.copyFieldTargetCounts.containsKey(destinationPrototype)) {
            newSchema.decrementCopyFieldTargetCount(destinationPrototype);
          }
           // don't add this dynamic copy field to newDynamicCopyFields - effectively removing it
         } else {
           newDynamicCopyFields.add(dynamicCopy);
diff --git a/solr/core/src/test/org/apache/solr/schema/TestBulkSchemaConcurrent.java b/solr/core/src/test/org/apache/solr/schema/TestBulkSchemaConcurrent.java
index 3ad79af9009..b099035fc6c 100644
-- a/solr/core/src/test/org/apache/solr/schema/TestBulkSchemaConcurrent.java
++ b/solr/core/src/test/org/apache/solr/schema/TestBulkSchemaConcurrent.java
@@ -152,7 +152,7 @@ public class TestBulkSchemaConcurrent  extends AbstractFullDistribZkTestBase {
 
     payload = payload.replace("replaceFieldA", aField);
     payload = payload.replace("replaceDynamicField", dynamicFldName);
    payload = payload.replace("replaceDynamicCopyFieldDest",dynamicCopyFldDest);
    payload = payload.replace("replaceDynamicCopyFieldDest", dynamicCopyFldDest);
     payload = payload.replace("myNewFieldTypeName", newFieldTypeName);
 
     RestTestHarness publisher = restTestHarnesses.get(r.nextInt(restTestHarnesses.size()));
@@ -269,12 +269,12 @@ public class TestBulkSchemaConcurrent  extends AbstractFullDistribZkTestBase {
 
   private void invokeBulkDeleteCall(int seed, ArrayList<String> errs) throws Exception {
     String payload = "{\n" +
        "          'delete-field' : {'name':'replaceFieldA'},\n" +
        "          'delete-dynamic-field' : {'name' :'replaceDynamicField'},\n" +
         "          'delete-copy-field' : {\n" +
         "                       'source' :'replaceFieldA',\n" +
         "                       'dest':['replaceDynamicCopyFieldDest']\n" +
         "                       },\n" +
        "          'delete-field' : {'name':'replaceFieldA'},\n" +
        "          'delete-dynamic-field' : {'name' :'replaceDynamicField'},\n" +
         "          'delete-field-type' : {'name' :'myNewFieldTypeName'}\n" +
         " }";
     String aField = "a" + seed;
- 
2.19.1.windows.1

