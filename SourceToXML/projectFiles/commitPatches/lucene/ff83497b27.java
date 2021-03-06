From ff83497b27c56dd86a94f64e081c2694772bd571 Mon Sep 17 00:00:00 2001
From: Steven Rowe <sarowe@apache.org>
Date: Tue, 17 Mar 2015 05:13:36 +0000
Subject: [PATCH] SOLR-6141: Schema API: Remove fields, dynamic fields, field
 types and copy fields; and replace fields, dynamic fields and field types

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1667175 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |   3 +
 .../apache/solr/handler/SchemaHandler.java    |   4 +-
 .../org/apache/solr/schema/IndexSchema.java   | 164 ++++-
 .../solr/schema/ManagedIndexSchema.java       | 658 ++++++++++++++++--
 .../schema/ManagedIndexSchemaFactory.java     |   8 +
 .../org/apache/solr/schema/SchemaField.java   |   1 +
 .../org/apache/solr/schema/SchemaManager.java | 380 ++++++----
 .../solr/schema/ZkIndexSchemaReader.java      |   7 +-
 .../apache/solr/util/CommandOperation.java    |  12 +-
 .../solr/rest/schema/TestBulkSchemaAPI.java   | 450 ++++++++++--
 .../solr/schema/TestBulkSchemaConcurrent.java | 206 ++++--
 .../apache/solr/schema/TestSchemaManager.java |  56 +-
 12 files changed, 1590 insertions(+), 359 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 8ae25eba878..cb526075da0 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -175,6 +175,9 @@ New Features
     json.facet={count1:{query:"price:[10 TO 20]"}, count2:{query:"color:blue AND popularity:[0 TO 50]"} }
     json.facet={categories:{terms:{field:cat, sort:"x desc", facet:{x:"avg(price)", y:"sum(price)"}}}}
   (yonik)
  
* SOLR-6141: Schema API: Remove fields, dynamic fields, field types and copy
  fields; and replace fields, dynamic fields and field types. (Steve Rowe)
 
 
 Bug Fixes
diff --git a/solr/core/src/java/org/apache/solr/handler/SchemaHandler.java b/solr/core/src/java/org/apache/solr/handler/SchemaHandler.java
index 538dd7b0318..47df8f648c9 100644
-- a/solr/core/src/java/org/apache/solr/handler/SchemaHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/SchemaHandler.java
@@ -62,11 +62,9 @@ public class SchemaHandler extends RequestHandlerBase {
         }
         break;
       }

     } else {
       handleGET(req, rsp);
     }

   }
 
   private void handleGET(SolrQueryRequest req, SolrQueryResponse rsp) {
@@ -165,6 +163,6 @@ public class SchemaHandler extends RequestHandlerBase {
 
   @Override
   public String getDescription() {
    return "Edit schema.xml";
    return "CRUD operations over the Solr schema";
   }
 }
diff --git a/solr/core/src/java/org/apache/solr/schema/IndexSchema.java b/solr/core/src/java/org/apache/solr/schema/IndexSchema.java
index 235d879b262..40560534891 100644
-- a/solr/core/src/java/org/apache/solr/schema/IndexSchema.java
++ b/solr/core/src/java/org/apache/solr/schema/IndexSchema.java
@@ -755,7 +755,7 @@ public class IndexSchema {
   }
 
   /** Returns true if the given name has exactly one asterisk either at the start or end of the name */
  private static boolean isValidFieldGlob(String name) {
  protected static boolean isValidFieldGlob(String name) {
     if (name.startsWith("*") || name.endsWith("*")) {
       int count = 0;
       for (int pos = 0 ; pos < name.length() && -1 != (pos = name.indexOf('*', pos)) ; ++pos) ++count;
@@ -935,7 +935,7 @@ public class IndexSchema {
     }
   }
 
  private void registerExplicitSrcAndDestFields(String source, int maxChars, SchemaField destSchemaField, SchemaField sourceSchemaField) {
  protected void registerExplicitSrcAndDestFields(String source, int maxChars, SchemaField destSchemaField, SchemaField sourceSchemaField) {
     List<CopyField> copyFieldList = copyFieldsMap.get(source);
     if (copyFieldList == null) {
       copyFieldList = new ArrayList<>();
@@ -1108,6 +1108,8 @@ public class IndexSchema {
       this.destDynamicBase = destDynamicBase;
     }
 
    public DynamicField getDestination() { return destination; }

     public String getDestFieldName() { return destination.getRegex(); }
 
     /**
@@ -1295,7 +1297,7 @@ public class IndexSchema {
       if (df.matches(fieldName)) return df.prototype.getType();
     }
     return null;
  };
  }
 
 
   /**
@@ -1416,10 +1418,11 @@ public class IndexSchema {
     List<SimpleOrderedMap<Object>> copyFieldProperties = new ArrayList<>();
     SortedMap<String,List<CopyField>> sortedCopyFields = new TreeMap<>(copyFieldsMap);
     for (List<CopyField> copyFields : sortedCopyFields.values()) {
      copyFields = new ArrayList<>(copyFields);
       Collections.sort(copyFields, new Comparator<CopyField>() {
         @Override
         public int compare(CopyField cf1, CopyField cf2) {
          // sources are all be the same, just sorting by destination here
          // sources are all the same, just sorting by destination here
           return cf1.getDestination().getName().compareTo(cf2.getDestination().getName());
         }
       });
@@ -1494,12 +1497,12 @@ public class IndexSchema {
    * {@link #getSchemaUpdateLock()}.
    *
    * @param newField the SchemaField to add 
   * @param persist to persist the schema or not or not
   * @param persist to persist the schema or not
    * @return a new IndexSchema based on this schema with newField added
    * @see #newField(String, String, Map)
    */
   public IndexSchema addField(SchemaField newField, boolean persist) {
    return addFields(Collections.singletonList(newField),Collections.EMPTY_MAP,persist );
    return addFields(Collections.singletonList(newField), Collections.emptyMap(), persist);
   }
 
   public IndexSchema addField(SchemaField newField) {
@@ -1551,6 +1554,44 @@ public class IndexSchema {
   }
 
 
  /**
   * Copies this schema, deletes the named fields from the copy.
   * <p>
   * The schema will not be persisted.
   * <p>
   * Requires synchronizing on the object returned by
   * {@link #getSchemaUpdateLock()}.
   *
   * @param names the names of the fields to delete
   * @return a new IndexSchema based on this schema with the named fields deleted
   */
  public IndexSchema deleteFields(Collection<String> names) {
    String msg = "This IndexSchema is not mutable.";
    log.error(msg);
    throw new SolrException(ErrorCode.SERVER_ERROR, msg);
  }

  /**
   * Copies this schema, deletes the named field from the copy, creates a new field 
   * with the same name using the given args, then rebinds any referring copy fields
   * to the replacement field.
   *
   * <p>
   * The schema will not be persisted.
   * <p>
   * Requires synchronizing on the object returned by {@link #getSchemaUpdateLock()}.
   *
   * @param fieldName The name of the field to be replaced
   * @param replacementFieldType  The field type of the replacement field                                   
   * @param replacementArgs Initialization params for the replacement field
   * @return a new IndexSchema based on this schema with the named field replaced
   */
  public IndexSchema replaceField(String fieldName, FieldType replacementFieldType, Map<String,?> replacementArgs) {
    String msg = "This IndexSchema is not mutable.";
    log.error(msg);
    throw new SolrException(ErrorCode.SERVER_ERROR, msg);
  }

   /**
    * Copies this schema, adds the given dynamic fields to the copy,
    * Requires synchronizing on the object returned by
@@ -1558,7 +1599,7 @@ public class IndexSchema {
    *
    * @param newDynamicFields the SchemaFields to add
    * @param copyFieldNames 0 or more names of targets to copy this field to.  The target fields must already exist.
   * @param persist to persist the schema or not or not
   * @param persist to persist the schema or not
    * @return a new IndexSchema based on this schema with newDynamicFields added
    * @see #newDynamicField(String, String, Map)
    */
@@ -1572,20 +1613,78 @@ public class IndexSchema {
   }
 
   /**
   * Copies this schema and adds the new copy fields to the copy
   * Copies this schema, deletes the named dynamic fields from the copy.
   * <p>
   * The schema will not be persisted.
   * <p>
   * Requires synchronizing on the object returned by
   * {@link #getSchemaUpdateLock()}.
   *
   * @param fieldNamePatterns the names of the dynamic fields to delete
   * @return a new IndexSchema based on this schema with the named dynamic fields deleted
   */
  public IndexSchema deleteDynamicFields(Collection<String> fieldNamePatterns) {
    String msg = "This IndexSchema is not mutable.";
    log.error(msg);
    throw new SolrException(ErrorCode.SERVER_ERROR, msg);
  }

  /**
   * Copies this schema, deletes the named dynamic field from the copy, creates a new dynamic
   * field with the same field name pattern using the given args, then rebinds any referring
   * dynamic copy fields to the replacement dynamic field.
   *
   * <p>
   * The schema will not be persisted.
   * <p>
   * Requires synchronizing on the object returned by {@link #getSchemaUpdateLock()}.
   *
   * @param fieldNamePattern The glob for the dynamic field to be replaced
   * @param replacementFieldType  The field type of the replacement dynamic field                                   
   * @param replacementArgs Initialization params for the replacement dynamic field
   * @return a new IndexSchema based on this schema with the named dynamic field replaced
   */
  public ManagedIndexSchema replaceDynamicField
      (String fieldNamePattern, FieldType replacementFieldType, Map<String,?> replacementArgs) {
    String msg = "This IndexSchema is not mutable.";
    log.error(msg);
    throw new SolrException(ErrorCode.SERVER_ERROR, msg);
  }

    /**
     * Copies this schema and adds the new copy fields to the copy
     * Requires synchronizing on the object returned by
     * {@link #getSchemaUpdateLock()}.
     *
     * @param copyFields Key is the name of the source field name, value is a collection of target field names.  Fields must exist.
     * @param persist to persist the schema or not
     * @return The new Schema with the copy fields added
     */
  public IndexSchema addCopyFields(Map<String, Collection<String>> copyFields, boolean persist) {
    String msg = "This IndexSchema is not mutable.";
    log.error(msg);
    throw new SolrException(ErrorCode.SERVER_ERROR, msg);
  }

  /**
   * Copies this schema and deletes the given copy fields from the copy.
   * <p>
   * The schema will not be persisted.
   * <p>
    * Requires synchronizing on the object returned by
    * {@link #getSchemaUpdateLock()}.
    *
   * @param copyFields Key is the name of the source field name, value is a collection of target field names.  Fields must exist.
   * @param persist to persist the schema or not or not
   * @return The new Schema with the copy fields added
   * @param copyFields Key is the name of the source field name, value is a collection of target field names. 
   *                   Each corresponding copy field directives must exist.
   * @return The new Schema with the copy fields deleted
    */
  public IndexSchema addCopyFields(Map<String, Collection<String>> copyFields, boolean persist){
  public IndexSchema deleteCopyFields(Map<String, Collection<String>> copyFields) {
     String msg = "This IndexSchema is not mutable.";
     log.error(msg);
     throw new SolrException(ErrorCode.SERVER_ERROR, msg);
   }
 

   /**
    * Returns a SchemaField if the given fieldName does not already 
    * exist in this schema, and does not match any dynamic fields 
@@ -1623,7 +1722,7 @@ public class IndexSchema {
   }
 
   /**
   * Returns the schema update lock that should be synchronzied on
   * Returns the schema update lock that should be synchronized on
    * to update the schema.  Only applicable to mutable schemas.
    *
    * @return the schema update lock object to synchronize on
@@ -1640,7 +1739,7 @@ public class IndexSchema {
    * {@link #getSchemaUpdateLock()}.
    *
    * @param fieldTypeList a list of FieldTypes to add
   * @param persist to persist the schema or not or not
   * @param persist to persist the schema or not
    * @return a new IndexSchema based on this schema with the new types added
    * @see #newFieldType(String, String, Map)
    */
@@ -1650,6 +1749,43 @@ public class IndexSchema {
     throw new SolrException(ErrorCode.SERVER_ERROR, msg);
   }
 
  /**
   * Copies this schema, deletes the named field types from the copy.
   * <p>
   * The schema will not be persisted.
   * <p>
   * Requires synchronizing on the object returned by {@link #getSchemaUpdateLock()}.
   *
   * @param names the names of the field types to delete
   * @return a new IndexSchema based on this schema with the named field types deleted
   */
  public IndexSchema deleteFieldTypes(Collection<String> names) {
    String msg = "This IndexSchema is not mutable.";
    log.error(msg);
    throw new SolrException(ErrorCode.SERVER_ERROR, msg);
  }

  /**
   * Copies this schema, deletes the named field type from the copy, creates a new field type 
   * with the same name using the given args, rebuilds fields and dynamic fields of the given
   * type, then rebinds any referring copy fields to the rebuilt fields.
   * 
   * <p>
   * The schema will not be persisted.
   * <p>
   * Requires synchronizing on the object returned by {@link #getSchemaUpdateLock()}.
   *  
   * @param typeName The name of the field type to be replaced
   * @param replacementClassName The class name of the replacement field type
   * @param replacementArgs Initialization params for the replacement field type
   * @return a new IndexSchema based on this schema with the named field type replaced
   */
  public IndexSchema replaceFieldType(String typeName, String replacementClassName, Map<String,Object> replacementArgs) {
    String msg = "This IndexSchema is not mutable.";
    log.error(msg);
    throw new SolrException(ErrorCode.SERVER_ERROR, msg);
  }

   /**
    * Returns a FieldType if the given typeName does not already
    * exist in this schema. The resulting FieldType can be used in a call
diff --git a/solr/core/src/java/org/apache/solr/schema/ManagedIndexSchema.java b/solr/core/src/java/org/apache/solr/schema/ManagedIndexSchema.java
index 436e59c24f5..f96a89fc0c2 100644
-- a/solr/core/src/java/org/apache/solr/schema/ManagedIndexSchema.java
++ b/solr/core/src/java/org/apache/solr/schema/ManagedIndexSchema.java
@@ -68,6 +68,7 @@ import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
@@ -386,7 +387,7 @@ public final class ManagedIndexSchema extends IndexSchema {
   public ManagedIndexSchema addFields(Collection<SchemaField> newFields,
                                       Map<String, Collection<String>> copyFieldNames,
                                       boolean persist) {
    ManagedIndexSchema newSchema = null;
    ManagedIndexSchema newSchema;
     if (isMutable) {
       boolean success = false;
       if (copyFieldNames == null){
@@ -440,11 +441,129 @@ public final class ManagedIndexSchema extends IndexSchema {
     return newSchema;
   }
 
  @Override
  public ManagedIndexSchema deleteFields(Collection<String> names) {
    ManagedIndexSchema newSchema;
    if (isMutable) {
      newSchema = shallowCopy(true);
      for (String name : names) {
        SchemaField field = getFieldOrNull(name); 
        if (null != field) {
          if (copyFieldsMap.containsKey(name) || isCopyFieldTarget(field)) {
            throw new SolrException(ErrorCode.BAD_REQUEST, "Can't delete '" + name
                + "' because it's referred to by at least one copy field directive.");
          }
          newSchema.fields.remove(name);
          newSchema.fieldsWithDefaultValue.remove(field);
          newSchema.requiredFields.remove(field);
        } else {
          String msg = "The field '" + name + "' is not present in this schema, and so cannot be deleted.";
          throw new SolrException(ErrorCode.BAD_REQUEST, msg);
        }
      }
      // Run the callbacks on SchemaAware now that everything else is done
      for (SchemaAware aware : newSchema.schemaAware) {
        aware.inform(newSchema);
      }
      newSchema.refreshAnalyzers();
    } else {
      String msg = "This ManagedIndexSchema is not mutable.";
      log.error(msg);
      throw new SolrException(ErrorCode.SERVER_ERROR, msg);
    }
    return newSchema;
  }
 
  @Override
  public ManagedIndexSchema replaceField
      (String fieldName, FieldType replacementFieldType, Map<String,?> replacementArgs) {
    ManagedIndexSchema newSchema;
    if (isMutable) {
      SchemaField oldField = fields.get(fieldName);
      if (null == oldField) {
        String msg = "The field '" + fieldName + "' is not present in this schema, and so cannot be replaced.";
        throw new SolrException(ErrorCode.BAD_REQUEST, msg);
      }
      newSchema = shallowCopy(true);
      // clone data structures before modifying them
      newSchema.copyFieldsMap = cloneCopyFieldsMap(copyFieldsMap);
      newSchema.copyFieldTargetCounts
          = (Map<SchemaField,Integer>)((HashMap<SchemaField,Integer>)copyFieldTargetCounts).clone();
      newSchema.dynamicCopyFields = new DynamicCopy[dynamicCopyFields.length];
      System.arraycopy(dynamicCopyFields, 0, newSchema.dynamicCopyFields, 0, dynamicCopyFields.length);

      // Drop the old field
      newSchema.fields.remove(fieldName);
      newSchema.fieldsWithDefaultValue.remove(oldField);
      newSchema.requiredFields.remove(oldField);

      // Add the replacement field
      SchemaField replacementField = SchemaField.create(fieldName, replacementFieldType, replacementArgs);
      newSchema.fields.put(fieldName, replacementField);
      if (null != replacementField.getDefaultValue()) {
        log.debug(replacementField.getName() + " contains default value: " + replacementField.getDefaultValue());
        newSchema.fieldsWithDefaultValue.add(replacementField);
      }
      if (replacementField.isRequired()) {
        log.debug("{} is required in this schema", replacementField.getName());
        newSchema.requiredFields.add(replacementField);
      }

      List<CopyField> copyFieldsToRebuild = new ArrayList<>();
      newSchema.removeCopyFieldSource(fieldName, copyFieldsToRebuild);

      newSchema.copyFieldTargetCounts.remove(oldField); // zero out target count for this field

      // Remove copy fields where the target is this field; remember them to rebuild
      for (Map.Entry<String,List<CopyField>> entry : newSchema.copyFieldsMap.entrySet()) {
        List<CopyField> perSourceCopyFields = entry.getValue();
        Iterator<CopyField> checkDestCopyFieldsIter = perSourceCopyFields.iterator();
        while (checkDestCopyFieldsIter.hasNext()) {
          CopyField checkDestCopyField = checkDestCopyFieldsIter.next();
          if (fieldName.equals(checkDestCopyField.getDestination().getName())) {
            checkDestCopyFieldsIter.remove();
            copyFieldsToRebuild.add(checkDestCopyField);
          }
        }
      }
      newSchema.rebuildCopyFields(copyFieldsToRebuild);

      // Find dynamic copy fields where the source or destination is this field; remember them to rebuild
      List<DynamicCopy> dynamicCopyFieldsToRebuild = new ArrayList<>();
      List<DynamicCopy> newDynamicCopyFields = new ArrayList<>();
      for (int i = 0 ; i < newSchema.dynamicCopyFields.length ; ++i) {
        DynamicCopy dynamicCopy = newSchema.dynamicCopyFields[i];
        SchemaField destinationPrototype = dynamicCopy.getDestination().getPrototype();
        if (fieldName.equals(dynamicCopy.getRegex()) || fieldName.equals(destinationPrototype.getName())) {
          dynamicCopyFieldsToRebuild.add(dynamicCopy);
        } else {
          newDynamicCopyFields.add(dynamicCopy);
        }
      }
      // Rebuild affected dynamic copy fields
      if (dynamicCopyFieldsToRebuild.size() > 0) {
        newSchema.dynamicCopyFields = newDynamicCopyFields.toArray(new DynamicCopy[newDynamicCopyFields.size()]);
        for (DynamicCopy dynamicCopy : dynamicCopyFieldsToRebuild) {
          newSchema.registerCopyField(dynamicCopy.getRegex(), dynamicCopy.getDestFieldName(), dynamicCopy.getMaxChars());
        }
      }

      for (SchemaAware aware : newSchema.schemaAware) {
        aware.inform(newSchema);
      }
      newSchema.refreshAnalyzers();
    } else {
      String msg = "This ManagedIndexSchema is not mutable.";
      log.error(msg);
      throw new SolrException(ErrorCode.SERVER_ERROR, msg);
    }
    return newSchema;
  }
  
   @Override
   public ManagedIndexSchema addDynamicFields(Collection<SchemaField> newDynamicFields, 
                                              Map<String,Collection<String>> copyFieldNames, boolean persist) {
    ManagedIndexSchema newSchema = null;
    ManagedIndexSchema newSchema;
     if (isMutable) {
       boolean success = false;
       if (copyFieldNames == null){
@@ -474,7 +593,7 @@ public final class ManagedIndexSchema extends IndexSchema {
         aware.inform(newSchema);
       }
       newSchema.refreshAnalyzers();
      if(persist) {
      if (persist) {
         success = newSchema.persistManagedSchema(false); // don't just create - update it if it already exists
         if (success) {
           log.debug("Added dynamic field(s): {}", newDynamicFields);
@@ -490,9 +609,153 @@ public final class ManagedIndexSchema extends IndexSchema {
     return newSchema;
   }
 
  @Override
  public ManagedIndexSchema deleteDynamicFields(Collection<String> fieldNamePatterns) {
    ManagedIndexSchema newSchema;
    if (isMutable) {
      newSchema = shallowCopy(true);

      newSchema.dynamicCopyFields = new DynamicCopy[dynamicCopyFields.length];
      System.arraycopy(dynamicCopyFields, 0, newSchema.dynamicCopyFields, 0, dynamicCopyFields.length);

      List<DynamicCopy> dynamicCopyFieldsToRebuild = new ArrayList<>();
      List<DynamicCopy> newDynamicCopyFields = new ArrayList<>();

      for (String fieldNamePattern : fieldNamePatterns) {
        DynamicField dynamicField = null;
        int dfPos = 0;
        for ( ; dfPos < newSchema.dynamicFields.length ; ++dfPos) {
          DynamicField df = newSchema.dynamicFields[dfPos];
          if (df.getRegex().equals(fieldNamePattern)) {
            dynamicField = df;
            break;
          }
        }
        if (null == dynamicField) {
          String msg = "The dynamic field '" + fieldNamePattern
              + "' is not present in this schema, and so cannot be deleted.";
          throw new SolrException(ErrorCode.BAD_REQUEST, msg);
        }          
        for (int i = 0 ; i < newSchema.dynamicCopyFields.length ; ++i) {
          DynamicCopy dynamicCopy = newSchema.dynamicCopyFields[i];
          DynamicField destDynamicBase = dynamicCopy.getDestDynamicBase();
          DynamicField sourceDynamicBase = dynamicCopy.getSourceDynamicBase();
          if ((null != destDynamicBase && fieldNamePattern.equals(destDynamicBase.getRegex()))
              || (null != sourceDynamicBase && fieldNamePattern.equals(sourceDynamicBase.getRegex()))
              || dynamicField.matches(dynamicCopy.getRegex())
              || dynamicField.matches(dynamicCopy.getDestFieldName())) {
            dynamicCopyFieldsToRebuild.add(dynamicCopy);
            newSchema.decrementCopyFieldTargetCount(dynamicCopy.getDestination().getPrototype());
            // don't add this dynamic copy field to newDynamicCopyFields - effectively removing it
          } else {
            newDynamicCopyFields.add(dynamicCopy);
          }
        }
        if (newSchema.dynamicFields.length > 1) {
          DynamicField[] temp = new DynamicField[newSchema.dynamicFields.length - 1];
          System.arraycopy(newSchema.dynamicFields, 0, temp, 0, dfPos);
          // skip over the dynamic field to be deleted
          System.arraycopy(newSchema.dynamicFields, dfPos + 1, temp, dfPos, newSchema.dynamicFields.length - dfPos - 1);
          newSchema.dynamicFields = temp;
        } else {
          newSchema.dynamicFields = new DynamicField[0];
        }
      }
      // After removing all dynamic fields, rebuild affected dynamic copy fields.
      // This may trigger an exception, if one of the deleted dynamic fields was the only matching source or target.
      if (dynamicCopyFieldsToRebuild.size() > 0) {
        newSchema.dynamicCopyFields = newDynamicCopyFields.toArray(new DynamicCopy[newDynamicCopyFields.size()]);
        for (DynamicCopy dynamicCopy : dynamicCopyFieldsToRebuild) {
          newSchema.registerCopyField(dynamicCopy.getRegex(), dynamicCopy.getDestFieldName(), dynamicCopy.getMaxChars());
        }
      }

      // Run the callbacks on SchemaAware now that everything else is done
      for (SchemaAware aware : newSchema.schemaAware) {
        aware.inform(newSchema);
      }
      newSchema.refreshAnalyzers();
    } else {
      String msg = "This ManagedIndexSchema is not mutable.";
      log.error(msg);
      throw new SolrException(ErrorCode.SERVER_ERROR, msg);
    }
    return newSchema;
  }

  @Override
  public ManagedIndexSchema replaceDynamicField
    (String fieldNamePattern, FieldType replacementFieldType, Map<String,?> replacementArgs) {
    ManagedIndexSchema newSchema;
    if (isMutable) {
      DynamicField oldDynamicField = null;
      int dfPos = 0;
      for ( ; dfPos < dynamicFields.length ; ++dfPos) {
        DynamicField dynamicField = dynamicFields[dfPos];
        if (dynamicField.getRegex().equals(fieldNamePattern)) {
          oldDynamicField = dynamicField;
          break;
        }
      }
      if (null == oldDynamicField) {
        String msg = "The dynamic field '" + fieldNamePattern 
            + "' is not present in this schema, and so cannot be replaced.";
        throw new SolrException(ErrorCode.BAD_REQUEST, msg);
      }

      newSchema = shallowCopy(true);

      // clone data structures before modifying them
      newSchema.copyFieldTargetCounts
          = (Map<SchemaField,Integer>)((HashMap<SchemaField,Integer>)copyFieldTargetCounts).clone();
      newSchema.dynamicCopyFields = new DynamicCopy[dynamicCopyFields.length];
      System.arraycopy(dynamicCopyFields, 0, newSchema.dynamicCopyFields, 0, dynamicCopyFields.length);

      // Put the replacement dynamic field in place
      SchemaField prototype = SchemaField.create(fieldNamePattern, replacementFieldType, replacementArgs);
      newSchema.dynamicFields[dfPos] = new DynamicField(prototype);

      // Find dynamic copy fields where this dynamic field is the source or target base; remember them to rebuild
      List<DynamicCopy> dynamicCopyFieldsToRebuild = new ArrayList<>();
      List<DynamicCopy> newDynamicCopyFields = new ArrayList<>();
      for (int i = 0 ; i < newSchema.dynamicCopyFields.length ; ++i) {
        DynamicCopy dynamicCopy = newSchema.dynamicCopyFields[i];
        DynamicField destDynamicBase = dynamicCopy.getDestDynamicBase();
        DynamicField sourceDynamicBase = dynamicCopy.getSourceDynamicBase();
        if (fieldNamePattern.equals(dynamicCopy.getRegex())
            || fieldNamePattern.equals(dynamicCopy.getDestFieldName())
            || (null != destDynamicBase && fieldNamePattern.equals(destDynamicBase.getRegex()))
            || (null != sourceDynamicBase && fieldNamePattern.equals(sourceDynamicBase.getRegex()))) {
          dynamicCopyFieldsToRebuild.add(dynamicCopy);
          newSchema.decrementCopyFieldTargetCount(dynamicCopy.getDestination().getPrototype());
          // don't add this dynamic copy field to newDynamicCopyFields - effectively removing it
        } else {
          newDynamicCopyFields.add(dynamicCopy);
        }
      }
      // Rebuild affected dynamic copy fields
      if (dynamicCopyFieldsToRebuild.size() > 0) {
        newSchema.dynamicCopyFields = newDynamicCopyFields.toArray(new DynamicCopy[newDynamicCopyFields.size()]);
        for (DynamicCopy dynamicCopy : dynamicCopyFieldsToRebuild) {
          newSchema.registerCopyField(dynamicCopy.getRegex(), dynamicCopy.getDestFieldName(), dynamicCopy.getMaxChars());
        }
      }

      for (SchemaAware aware : newSchema.schemaAware) {
        aware.inform(newSchema);
      }
      newSchema.refreshAnalyzers();
    } else {
      String msg = "This ManagedIndexSchema is not mutable.";
      log.error(msg);
      throw new SolrException(ErrorCode.SERVER_ERROR, msg);
    }
    return newSchema;
  }

   @Override
   public ManagedIndexSchema addCopyFields(Map<String, Collection<String>> copyFields, boolean persist) {
    ManagedIndexSchema newSchema = null;
    ManagedIndexSchema newSchema;
     if (isMutable) {
       boolean success = false;
       newSchema = shallowCopy(true);
@@ -517,10 +780,150 @@ public final class ManagedIndexSchema extends IndexSchema {
           log.error("Failed to add copy fields for {} sources", copyFields.size());
         }
       }
    } else {
      String msg = "This ManagedIndexSchema is not mutable.";
      log.error(msg);
      throw new SolrException(ErrorCode.SERVER_ERROR, msg);
     }
     return newSchema;
   }
 
  @Override
  public ManagedIndexSchema deleteCopyFields(Map<String,Collection<String>> copyFields) {
    ManagedIndexSchema newSchema;
    if (isMutable) {
      newSchema = shallowCopy(true);
      // clone data structures before modifying them
      newSchema.copyFieldsMap = cloneCopyFieldsMap(copyFieldsMap);
      newSchema.copyFieldTargetCounts
          = (Map<SchemaField,Integer>)((HashMap<SchemaField,Integer>)copyFieldTargetCounts).clone();
      newSchema.dynamicCopyFields = new DynamicCopy[dynamicCopyFields.length];
      System.arraycopy(dynamicCopyFields, 0, newSchema.dynamicCopyFields, 0, dynamicCopyFields.length);

      for (Map.Entry<String,Collection<String>> entry : copyFields.entrySet()) {
        // Key is the source, values are the destinations
        for (String destination : entry.getValue()) {
          newSchema.deleteCopyField(entry.getKey(), destination);
        }
      }
      //TODO: move this common stuff out to shared methods
      // Run the callbacks on SchemaAware now that everything else is done
      for (SchemaAware aware : newSchema.schemaAware) {
        aware.inform(newSchema);
      }
      newSchema.refreshAnalyzers();
    } else {
      String msg = "This ManagedIndexSchema is not mutable.";
      log.error(msg);
      throw new SolrException(ErrorCode.SERVER_ERROR, msg);
    }
    return newSchema;
  }
  
  private void deleteCopyField(String source, String dest) {
    // Assumption: a copy field directive will exist only if the source & destination (dynamic) fields exist
    SchemaField destSchemaField = fields.get(dest);
    SchemaField sourceSchemaField = fields.get(source);

    final String invalidGlobMessage = "is an invalid glob: either it contains more than one asterisk,"
        + " or the asterisk occurs neither at the start nor at the end.";
    if (source.contains("*") && ! isValidFieldGlob(source)) {
      String msg = "copyField source '" + source + "' " + invalidGlobMessage;
      throw new SolrException(ErrorCode.BAD_REQUEST, msg);
    }
    if (dest.contains("*") && ! isValidFieldGlob(dest)) {
      String msg = "copyField dest '" + dest + "' " + invalidGlobMessage;
      throw new SolrException(ErrorCode.BAD_REQUEST, msg);
    }

    boolean found = false;

    if (null == destSchemaField || null == sourceSchemaField) { // Must be dynamic copy field
      if (dynamicCopyFields != null) {
        for (int i = 0 ; i < dynamicCopyFields.length ; ++i) {
          DynamicCopy dynamicCopy = dynamicCopyFields[i];
          if (source.equals(dynamicCopy.getRegex()) && dest.equals(dynamicCopy.getDestFieldName())) {
            found = true;
            decrementCopyFieldTargetCount(dynamicCopy.getDestination().getPrototype());
            if (dynamicCopyFields.length > 1) {
              DynamicCopy[] temp = new DynamicCopy[dynamicCopyFields.length - 1];
              System.arraycopy(dynamicCopyFields, 0, temp, 0, i);
              // skip over the dynamic copy field to be deleted
              System.arraycopy(dynamicCopyFields, i + 1, temp, i, dynamicCopyFields.length - i - 1);
              dynamicCopyFields = temp;
            } else {
              dynamicCopyFields = null;
            }
            break;
          }
        }
      }
    } else { // non-dynamic copy field directive
      List<CopyField> copyFieldList = copyFieldsMap.get(source);
      if (copyFieldList != null) {
        for (Iterator<CopyField> iter = copyFieldList.iterator() ; iter.hasNext() ; ) {
          CopyField copyField = iter.next();
          if (dest.equals(copyField.getDestination().getName())) {
            found = true;
            decrementCopyFieldTargetCount(copyField.getDestination());
            iter.remove();
            if (copyFieldList.isEmpty()) {
              copyFieldsMap.remove(source);
            }
            break;
          }
        }
      }
    }
    if ( ! found) {
      throw new SolrException(ErrorCode.BAD_REQUEST,
          "Copy field directive not found: '" + source + "' -> '" + dest + "'");
    }
  }

  /**
   * Removes all copy fields with the given source field name, decrements the count for the copy field target,
   * and adds the removed copy fields to removedCopyFields.
   */
  private void removeCopyFieldSource(String sourceFieldName, List<CopyField> removedCopyFields) {
    List<CopyField> sourceCopyFields = copyFieldsMap.remove(sourceFieldName);
    if (null != sourceCopyFields) {
      for (CopyField sourceCopyField : sourceCopyFields) {
        decrementCopyFieldTargetCount(sourceCopyField.getDestination());
        removedCopyFields.add(sourceCopyField);
      }
    }
  }

  /**
   * Registers new copy fields with the source, destination and maxChars taken from each of the oldCopyFields.
   * 
   * Assumption: the fields in oldCopyFields still exist in the schema. 
   */
  private void rebuildCopyFields(List<CopyField> oldCopyFields) {
    if (oldCopyFields.size() > 0) {
      for (CopyField copyField : oldCopyFields) {
        SchemaField source = fields.get(copyField.getSource().getName());
        SchemaField destination = fields.get(copyField.getDestination().getName());
        registerExplicitSrcAndDestFields
            (copyField.getSource().getName(), copyField.getMaxChars(), destination, source);
      }
    }
  }

  /**
   * Decrements the count for the given destination field in copyFieldTargetCounts.
   */
  private void decrementCopyFieldTargetCount(SchemaField dest) {
    Integer count = copyFieldTargetCounts.get(dest);
    assert count != null;
    if (count <= 1) {
      copyFieldTargetCounts.remove(dest);
    } else {
      copyFieldTargetCounts.put(dest, count - 1);
    }
  }

   public ManagedIndexSchema addFieldTypes(List<FieldType> fieldTypeList, boolean persist) {
     if (!isMutable) {
       String msg = "This ManagedIndexSchema is not mutable.";
@@ -579,6 +982,180 @@ public final class ManagedIndexSchema extends IndexSchema {
     return newSchema;
   }
 
  @Override
  public ManagedIndexSchema deleteFieldTypes(Collection<String> names) {
    ManagedIndexSchema newSchema;
    if (isMutable) {
      for (String name : names) {
        if ( ! fieldTypes.containsKey(name)) {
          String msg = "The field type '" + name + "' is not present in this schema, and so cannot be deleted.";
          throw new SolrException(ErrorCode.BAD_REQUEST, msg);
        }
        for (SchemaField field : fields.values()) {
          if (field.getType().getTypeName().equals(name)) {
            throw new SolrException(ErrorCode.BAD_REQUEST, "Can't delete '" + name
                + "' because it's the field type of field '" + field.getName() + "'.");
          }
        }
        for (DynamicField dynamicField : dynamicFields) {
          if (dynamicField.getPrototype().getType().getTypeName().equals(name)) {
            throw new SolrException(ErrorCode.BAD_REQUEST, "Can't delete '" + name
                + "' because it's the field type of dynamic field '" + dynamicField.getRegex() + "'.");
          }
        }
      }
      newSchema = shallowCopy(true);
      for (String name : names) {
        newSchema.fieldTypes.remove(name);
      }
      for (SchemaAware aware : newSchema.schemaAware) {
        aware.inform(newSchema);
      }
      for (FieldType fieldType : newSchema.fieldTypes.values()) {
        informResourceLoaderAwareObjectsForFieldType(fieldType);
      }
      newSchema.refreshAnalyzers();
    } else {
      String msg = "This ManagedIndexSchema is not mutable.";
      log.error(msg);
      throw new SolrException(ErrorCode.SERVER_ERROR, msg);
    }
    return newSchema;
  }
  
  private Map<String,List<CopyField>> cloneCopyFieldsMap(Map<String,List<CopyField>> original) {
    Map<String,List<CopyField>> clone = new HashMap<>(original.size());
    Iterator<Map.Entry<String,List<CopyField>>> iterator = original.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String,List<CopyField>> entry = iterator.next();
      clone.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }
    return clone;
  }

  @Override
  public ManagedIndexSchema replaceFieldType(String typeName, String replacementClassName, Map<String,Object> replacementArgs) {
    ManagedIndexSchema newSchema;
    if (isMutable) {
      if ( ! fieldTypes.containsKey(typeName)) {
        String msg = "The field type '" + typeName + "' is not present in this schema, and so cannot be replaced.";
        throw new SolrException(ErrorCode.BAD_REQUEST, msg);
      }
      newSchema = shallowCopy(true);
      // clone data structures before modifying them
      newSchema.fieldTypes = (Map<String,FieldType>)((HashMap<String,FieldType>)fieldTypes).clone();
      newSchema.copyFieldsMap = cloneCopyFieldsMap(copyFieldsMap);
      newSchema.copyFieldTargetCounts
          = (Map<SchemaField,Integer>)((HashMap<SchemaField,Integer>)copyFieldTargetCounts).clone();
      newSchema.dynamicCopyFields = new DynamicCopy[dynamicCopyFields.length];
      System.arraycopy(dynamicCopyFields, 0, newSchema.dynamicCopyFields, 0, dynamicCopyFields.length);
      newSchema.dynamicFields = new DynamicField[dynamicFields.length];
      System.arraycopy(dynamicFields, 0, newSchema.dynamicFields, 0, dynamicFields.length);
      
      newSchema.fieldTypes.remove(typeName);
      FieldType replacementFieldType = newSchema.newFieldType(typeName, replacementClassName, replacementArgs);
      newSchema.fieldTypes.put(typeName, replacementFieldType);

      // Rebuild fields of the type being replaced
      List<CopyField> copyFieldsToRebuild = new ArrayList<>();
      List<SchemaField> replacementFields = new ArrayList<>();
      Iterator<Map.Entry<String,SchemaField>> fieldsIter = newSchema.fields.entrySet().iterator();
      while (fieldsIter.hasNext()) {
        Map.Entry<String,SchemaField> entry = fieldsIter.next();
        SchemaField oldField = entry.getValue();
        if (oldField.getType().getTypeName().equals(typeName)) {
          String fieldName = oldField.getName();
          
          // Drop the old field
          fieldsIter.remove();
          newSchema.fieldsWithDefaultValue.remove(oldField);
          newSchema.requiredFields.remove(oldField);
          
          // Add the replacement field
          SchemaField replacementField = SchemaField.create(fieldName, replacementFieldType, oldField.getArgs());
          replacementFields.add(replacementField); // Save the new field to be added after iteration is finished
          if (null != replacementField.getDefaultValue()) {
            log.debug(replacementField.getName() + " contains default value: " + replacementField.getDefaultValue());
            newSchema.fieldsWithDefaultValue.add(replacementField);
          }
          if (replacementField.isRequired()) {
            log.debug("{} is required in this schema", replacementField.getName());
            newSchema.requiredFields.add(replacementField);
          }
          newSchema.removeCopyFieldSource(fieldName, copyFieldsToRebuild);
        }
      }
      for (SchemaField replacementField : replacementFields) {
        newSchema.fields.put(replacementField.getName(), replacementField);
      }
      // Remove copy fields where the target is of the type being replaced; remember them to rebuild
      Iterator<Map.Entry<String,List<CopyField>>> copyFieldsMapIter = newSchema.copyFieldsMap.entrySet().iterator();
      while (copyFieldsMapIter.hasNext()) {
        Map.Entry<String,List<CopyField>> entry = copyFieldsMapIter.next();
        List<CopyField> perSourceCopyFields = entry.getValue();
        Iterator<CopyField> checkDestCopyFieldsIter = perSourceCopyFields.iterator();
        while (checkDestCopyFieldsIter.hasNext()) {
          CopyField checkDestCopyField = checkDestCopyFieldsIter.next();
          SchemaField destination = checkDestCopyField.getDestination();
          if (typeName.equals(destination.getType().getTypeName())) {
            checkDestCopyFieldsIter.remove();
            copyFieldsToRebuild.add(checkDestCopyField);
            newSchema.copyFieldTargetCounts.remove(destination); // zero out target count
          }
        }
        if (perSourceCopyFields.isEmpty()) {
          copyFieldsMapIter.remove();
        }
      }
      // Rebuild dynamic fields of the type being replaced
      for (int i = 0; i < newSchema.dynamicFields.length; ++i) {
        SchemaField prototype = newSchema.dynamicFields[i].getPrototype();
        if (typeName.equals(prototype.getType().getTypeName())) {
          newSchema.dynamicFields[i] = new DynamicField
              (SchemaField.create(prototype.getName(), replacementFieldType, prototype.getArgs()));
        }
      }
      // Find dynamic copy fields where the destination field's type is being replaced
      // or the source dynamic base's type is being replaced; remember them to rebuild
      List<DynamicCopy> dynamicCopyFieldsToRebuild = new ArrayList<>();
      List<DynamicCopy> newDynamicCopyFields = new ArrayList<>();
      for (int i = 0 ; i < newSchema.dynamicCopyFields.length ; ++i) {
        DynamicCopy dynamicCopy = newSchema.dynamicCopyFields[i];
        DynamicField sourceDynamicBase = dynamicCopy.getSourceDynamicBase();
        SchemaField destinationPrototype = dynamicCopy.getDestination().getPrototype();
        if (typeName.equals(destinationPrototype.getType().getTypeName())
            || (null != sourceDynamicBase && typeName.equals(sourceDynamicBase.getPrototype().getType().getTypeName()))) {
          dynamicCopyFieldsToRebuild.add(dynamicCopy);
          newSchema.decrementCopyFieldTargetCount(destinationPrototype);
          // don't add this dynamic copy field to newDynamicCopyFields - effectively removing it
        } else {
          newDynamicCopyFields.add(dynamicCopy);
        }
      }
      // Rebuild affected dynamic copy fields
      if (dynamicCopyFieldsToRebuild.size() > 0) {
        newSchema.dynamicCopyFields = newDynamicCopyFields.toArray(new DynamicCopy[newDynamicCopyFields.size()]);
        for (DynamicCopy dynamicCopy : dynamicCopyFieldsToRebuild) {
          newSchema.registerCopyField(dynamicCopy.getRegex(), dynamicCopy.getDestFieldName(), dynamicCopy.getMaxChars());
        }
      }
      newSchema.rebuildCopyFields(copyFieldsToRebuild);

      for (SchemaAware aware : newSchema.schemaAware) {
        aware.inform(newSchema);
      }
      for (FieldType fieldType : newSchema.fieldTypes.values()) {
        newSchema.informResourceLoaderAwareObjectsForFieldType(fieldType);
      }
      newSchema.refreshAnalyzers();
    } else {
      String msg = "This ManagedIndexSchema is not mutable.";
      log.error(msg);
      throw new SolrException(ErrorCode.SERVER_ERROR, msg);
    }
    return newSchema;
  }

   /**
    * Informs analyzers used by a fieldType.
    */
@@ -696,8 +1273,8 @@ public final class ManagedIndexSchema extends IndexSchema {
     // build the new FieldType using the existing FieldTypePluginLoader framework
     // which expects XML, so we use a JSON to XML adapter to transform the JSON object
     // provided in the request into the XML format supported by the plugin loader
    Map<String, FieldType> newFieldTypes = new HashMap<String, FieldType>();
    List<SchemaAware> schemaAwareList = new ArrayList<SchemaAware>();
    Map<String,FieldType> newFieldTypes = new HashMap<>();
    List<SchemaAware> schemaAwareList = new ArrayList<>();
     FieldTypePluginLoader typeLoader = new FieldTypePluginLoader(this, newFieldTypes, schemaAwareList);
     typeLoader.loadSingle(loader, FieldTypeXmlAdapter.toNode(options));
     FieldType ft = newFieldTypes.get(typeName);
@@ -749,56 +1326,6 @@ public final class ManagedIndexSchema extends IndexSchema {
     }
   }
   

  /** 
   * Called from ZkIndexSchemaReader to merge the fields from the serialized managed schema
   * on ZooKeeper with the local managed schema.
   * 
   * @param inputSource The serialized content of the managed schema from ZooKeeper
   * @param schemaZkVersion The ZK version of the managed schema on ZooKeeper
   * @return The new merged schema
   */
  ManagedIndexSchema reloadFields(InputSource inputSource, int schemaZkVersion) {
    ManagedIndexSchema newSchema;
    try {
      newSchema = shallowCopy(false);
      Config schemaConf = new Config(loader, SCHEMA, inputSource, SLASH+SCHEMA+SLASH);
      Document document = schemaConf.getDocument();
      final XPath xpath = schemaConf.getXPath();

      // create a unified collection of field types from zk and in the local
      newSchema.mergeFieldTypesFromZk(document, xpath);

      newSchema.loadFields(document, xpath);
      // let's completely rebuild the copy fields from the schema in ZK.
      // create new copyField-related objects so we don't affect the
      // old schema
      newSchema.copyFieldsMap = new HashMap<>();
      newSchema.dynamicCopyFields = new DynamicCopy[] {};
      newSchema.copyFieldTargetCounts = new HashMap<>();
      newSchema.loadCopyFields(document, xpath);
      if (null != uniqueKeyField) {
        newSchema.requiredFields.add(uniqueKeyField);
      }
      //Run the callbacks on SchemaAware now that everything else is done
      for (SchemaAware aware : newSchema.schemaAware) {
        aware.inform(newSchema);
      }

      // notify analyzers and other objects for our fieldTypes
      for (FieldType fieldType : newSchema.fieldTypes.values())
        informResourceLoaderAwareObjectsForFieldType(fieldType);

      newSchema.refreshAnalyzers();
      newSchema.schemaZkVersion = schemaZkVersion;
    } catch (SolrException e) {
      throw e;
    } catch (Exception e) {
      throw new SolrException(ErrorCode.SERVER_ERROR, "Schema Parsing Failed: " + e.getMessage(), e);
    }
    return newSchema;
  }
  
   private ManagedIndexSchema(final SolrConfig solrConfig, final SolrResourceLoader loader, boolean isMutable,
                              String managedSchemaResourceName, int schemaZkVersion, Object schemaUpdateLock) 
       throws KeeperException, InterruptedException {
@@ -872,23 +1399,4 @@ public final class ManagedIndexSchema extends IndexSchema {
   public Object getSchemaUpdateLock() {
     return schemaUpdateLock;
   }

  /**
   * Loads FieldType objects defined in the schema.xml document.
   *
   * @param document Schema XML document where field types are defined.
   * @param xpath Used for evaluating xpath expressions to find field types defined in the schema.xml.
   * @throws javax.xml.xpath.XPathExpressionException if an error occurs when finding field type elements in the document.
   */
  protected synchronized void mergeFieldTypesFromZk(Document document, XPath xpath)
      throws XPathExpressionException
  {
    Map<String, FieldType> newFieldTypes = new HashMap<String, FieldType>();
    FieldTypePluginLoader typeLoader = new FieldTypePluginLoader(this, newFieldTypes, schemaAware);
    String expression = getFieldTypeXPathExpressions();
    NodeList nodes = (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);
    typeLoader.load(loader, nodes);
    for (String newTypeName : newFieldTypes.keySet())
      fieldTypes.put(newTypeName, newFieldTypes.get(newTypeName));
  }
 }
diff --git a/solr/core/src/java/org/apache/solr/schema/ManagedIndexSchemaFactory.java b/solr/core/src/java/org/apache/solr/schema/ManagedIndexSchemaFactory.java
index e4a067d0f07..160085d68a7 100644
-- a/solr/core/src/java/org/apache/solr/schema/ManagedIndexSchemaFactory.java
++ b/solr/core/src/java/org/apache/solr/schema/ManagedIndexSchemaFactory.java
@@ -404,4 +404,12 @@ public class ManagedIndexSchemaFactory extends IndexSchemaFactory implements Sol
     this.schema = schema;
     core.setLatestSchema(schema);
   }
  
  public boolean isMutable() {
    return isMutable;
  }

  public SolrConfig getConfig() {
    return config;
  }
 }
diff --git a/solr/core/src/java/org/apache/solr/schema/SchemaField.java b/solr/core/src/java/org/apache/solr/schema/SchemaField.java
index caf27736c2a..be99485acfc 100644
-- a/solr/core/src/java/org/apache/solr/schema/SchemaField.java
++ b/solr/core/src/java/org/apache/solr/schema/SchemaField.java
@@ -104,6 +104,7 @@ public final class SchemaField extends FieldProperties {
   public boolean sortMissingFirst() { return (properties & SORT_MISSING_FIRST)!=0; }
   public boolean sortMissingLast() { return (properties & SORT_MISSING_LAST)!=0; }
   public boolean isRequired() { return required; } 
  public Map<String,?> getArgs() { return Collections.unmodifiableMap(args); }
 
   // things that should be determined by field type, not set as options
   boolean isTokenized() { return (properties & TOKENIZED)!=0; }
diff --git a/solr/core/src/java/org/apache/solr/schema/SchemaManager.java b/solr/core/src/java/org/apache/solr/schema/SchemaManager.java
index 5130647a4fd..c4c1e5b6a34 100644
-- a/solr/core/src/java/org/apache/solr/schema/SchemaManager.java
++ b/solr/core/src/java/org/apache/solr/schema/SchemaManager.java
@@ -21,7 +21,6 @@ package org.apache.solr.schema;
 import org.apache.solr.cloud.ZkController;
 import org.apache.solr.cloud.ZkSolrResourceLoader;
 import org.apache.solr.common.SolrException;
import org.apache.solr.core.ConfigOverlay;
 import org.apache.solr.core.CoreDescriptor;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.core.SolrResourceLoader;
@@ -38,15 +37,13 @@ import java.io.InputStream;
 import java.io.Reader;
 import java.io.StringWriter;
 import java.nio.charset.StandardCharsets;
import java.util.Collection;
 import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
 import java.util.List;
import java.util.Set;
import java.util.Map;
 import java.util.concurrent.TimeUnit;
 
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.EMPTY_MAP;
import static java.util.Collections.singleton;
 import static java.util.Collections.singletonList;
 import static java.util.Collections.singletonMap;
 import static org.apache.solr.schema.FieldType.CLASS_NAME;
@@ -55,9 +52,10 @@ import static org.apache.solr.schema.IndexSchema.NAME;
 import static org.apache.solr.schema.IndexSchema.SOURCE;
 import static org.apache.solr.schema.IndexSchema.TYPE;
 
/**A utility class to manipulate schema using the bulk mode.
 * This class takes in all the commands and process them completely. It is an all or none
 * operation
/**
 * A utility class to manipulate schema using the bulk mode.
 * This class takes in all the commands and processes them completely.
 * It is an all or nothing operation.
  */
 public class SchemaManager {
   private static final Logger log = LoggerFactory.getLogger(SchemaManager.class);
@@ -65,50 +63,35 @@ public class SchemaManager {
   final SolrQueryRequest req;
   ManagedIndexSchema managedIndexSchema;
 
  public static final String ADD_FIELD = "add-field";
  public static final String ADD_COPY_FIELD = "add-copy-field";
  public static final String ADD_DYNAMIC_FIELD = "add-dynamic-field";
  public static final String ADD_FIELD_TYPE = "add-field-type";

  private static final Set<String> KNOWN_OPS = new HashSet<>();
  static {
    KNOWN_OPS.add(ADD_COPY_FIELD);
    KNOWN_OPS.add(ADD_FIELD);
    KNOWN_OPS.add(ADD_DYNAMIC_FIELD);
    KNOWN_OPS.add(ADD_FIELD_TYPE);
  }

   public SchemaManager(SolrQueryRequest req){
     this.req = req;

   }
 
  /**Take in a JSON command set and execute them . It tries to capture as many errors
   * as possible instead of failing at the frst error it encounters
   * @param rdr The input as a Reader
   * @return Lis of errors . If the List is empty then the operation is successful.
  /**
   * Take in a JSON command set and execute them. It tries to capture as many errors
   * as possible instead of failing at the first error it encounters
   * @param reader The input as a Reader
   * @return List of errors. If the List is empty then the operation was successful.
    */
  public List performOperations(Reader rdr) throws Exception {
    List<CommandOperation> ops = null;
  public List performOperations(Reader reader) throws Exception {
    List<CommandOperation> ops;
     try {
      ops = CommandOperation.parse(rdr);
      ops = CommandOperation.parse(reader);
     } catch (Exception e) {
      String msg= "Error parsing schema operations ";
      log.warn(msg  ,e );
      String msg = "Error parsing schema operations ";
      log.warn(msg, e);
       return Collections.singletonList(singletonMap(CommandOperation.ERR_MSGS, msg + ":" + e.getMessage()));
     }
     List errs = CommandOperation.captureErrors(ops);
    if(!errs.isEmpty()) return errs;
    if (!errs.isEmpty()) return errs;
 
     IndexSchema schema = req.getCore().getLatestSchema();
     if (!(schema instanceof ManagedIndexSchema)) {
      return singletonList( singletonMap(CommandOperation.ERR_MSGS,"schema is not editable"));
      return singletonList(singletonMap(CommandOperation.ERR_MSGS, "schema is not editable"));
     }

     synchronized (schema.getSchemaUpdateLock()) {
       return doOperations(ops);
     }

   }
 
   private List doOperations(List<CommandOperation> operations) throws InterruptedException, IOException, KeeperException {
@@ -116,16 +99,12 @@ public class SchemaManager {
     long startTime = System.nanoTime();
     long endTime = timeout > 0 ? System.nanoTime() + (timeout * 1000 * 1000) : Long.MAX_VALUE;
     SolrCore core = req.getCore();
    for (; System.nanoTime() < endTime; ) {
    while (System.nanoTime() < endTime) {
       managedIndexSchema = getFreshManagedSchema();
       for (CommandOperation op : operations) {
        if (ADD_FIELD.equals(op.name) || ADD_DYNAMIC_FIELD.equals(op.name)) {
          applyAddField(op);
        } else if(ADD_COPY_FIELD.equals(op.name)) {
          applyAddCopyField(op);
        } else if(ADD_FIELD_TYPE.equals(op.name)) {
          applyAddType(op);

        OpType opType = OpType.get(op.name);
        if (opType != null) {
          opType.perform(op, this);
         } else {
           op.addError("No such operation : " + op.name);
         }
@@ -150,28 +129,24 @@ public class SchemaManager {
               managedIndexSchema.getResourceName(),
               sw.toString().getBytes(StandardCharsets.UTF_8),
               true);
          return EMPTY_LIST;
          return Collections.emptyList();
         } catch (ZkController.ResourceModifiedInZkException e) {
           log.info("Race condition schema modified by another node");
          continue;
         } catch (Exception e) {
           String s = "Exception persisting schema";
           log.warn(s, e);
           return singletonList(s + e.getMessage());
         }

      }else {

      } else {
         try {
           //only for non cloud stuff
           managedIndexSchema.persistManagedSchema(false);
           core.setLatestSchema(managedIndexSchema);
           waitForOtherReplicasToUpdate(timeout, startTime);
          return EMPTY_LIST;
          return Collections.emptyList();
         } catch (ManagedIndexSchema.SchemaChangedInZkException e) {
           String s = "Failed to update schema because schema is modified";
           log.warn(s, e);
          continue;
         } catch (Exception e) {
           String s = "Exception persisting schema";
           log.warn(s, e);
@@ -179,102 +154,269 @@ public class SchemaManager {
         }
       }
     }

     return singletonList("Unable to persist schema");

   }
 
   private void waitForOtherReplicasToUpdate(int timeout, long startTime) {
    if(timeout > 0 && managedIndexSchema.getResourceLoader()instanceof ZkSolrResourceLoader){
    if (timeout > 0 && managedIndexSchema.getResourceLoader() instanceof ZkSolrResourceLoader) {
       CoreDescriptor cd = req.getCore().getCoreDescriptor();
       String collection = cd.getCollectionName();
       if (collection != null) {
         ZkSolrResourceLoader zkLoader = (ZkSolrResourceLoader) managedIndexSchema.getResourceLoader();
        long timeLeftSecs = timeout -   TimeUnit.SECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        if(timeLeftSecs<=0) throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Not enough time left to update replicas. However the schema is updated already");
        long timeLeftSecs = timeout - TimeUnit.SECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        if (timeLeftSecs <= 0) {
          throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
              "Not enough time left to update replicas. However, the schema is updated already.");
        }
         ManagedIndexSchema.waitForSchemaZkVersionAgreement(collection,
             cd.getCloudDescriptor().getCoreNodeName(),
             (managedIndexSchema).getSchemaZkVersion(),
             zkLoader.getZkController(),
             (int) timeLeftSecs);
       }

     }
   }
 
  private boolean applyAddType(CommandOperation op) {
    String name = op.getStr(NAME);
    String clz = op.getStr(CLASS_NAME);
    if(op.hasError())
      return false;
    try {
      FieldType fieldType = managedIndexSchema.newFieldType(name, clz, op.getDataMap());
      managedIndexSchema = managedIndexSchema.addFieldTypes(singletonList(fieldType), false);
      return true;
    } catch (Exception e) {
      op.addError(getErrorStr(e));
      return false;
  public enum OpType {
    ADD_FIELD_TYPE("add-field-type") {
      @Override public boolean perform(CommandOperation op, SchemaManager mgr) {
        String name = op.getStr(NAME);
        String className = op.getStr(CLASS_NAME);
        if (op.hasError())
          return false;
        try {
          FieldType fieldType = mgr.managedIndexSchema.newFieldType(name, className, op.getDataMap());
          mgr.managedIndexSchema = mgr.managedIndexSchema.addFieldTypes(singletonList(fieldType), false);
          return true;
        } catch (Exception e) {
          op.addError(getErrorStr(e));
          return false;
        }
      }
    },
    ADD_COPY_FIELD("add-copy-field") {
      @Override public boolean perform(CommandOperation op, SchemaManager mgr) {
        String src  = op.getStr(SOURCE);
        List<String> dests = op.getStrs(DESTINATION);
        if (op.hasError())
          return false;
        if ( ! op.getValuesExcluding(SOURCE, DESTINATION).isEmpty()) {
          op.addError("Only the '" + SOURCE + "' and '" + DESTINATION
              + "' params are allowed with the 'add-copy-field' operation");
          return false;
        }
        try {
          mgr.managedIndexSchema = mgr.managedIndexSchema.addCopyFields(singletonMap(src, dests), false);
          return true;
        } catch (Exception e) {
          op.addError(getErrorStr(e));
          return false;
        }
      }
    },
    ADD_FIELD("add-field") {
      @Override public boolean perform(CommandOperation op, SchemaManager mgr) {
        String name = op.getStr(NAME);
        String type = op.getStr(TYPE);
        if (op.hasError())
          return false;
        FieldType ft = mgr.managedIndexSchema.getFieldTypeByName(type);
        if (ft == null) {
          op.addError("No such field type '" + type + "'");
          return false;
        }
        try {
          SchemaField field = SchemaField.create(name, ft, op.getValuesExcluding(NAME, TYPE));
          mgr.managedIndexSchema 
              = mgr.managedIndexSchema.addFields(singletonList(field), Collections.emptyMap(), false);
          return true;
        } catch (Exception e) {
          op.addError(getErrorStr(e));
          return false;
        }
      }
    },
    ADD_DYNAMIC_FIELD("add-dynamic-field") {
      @Override public boolean perform(CommandOperation op, SchemaManager mgr) {
        String name = op.getStr(NAME);
        String type = op.getStr(TYPE);
        if (op.hasError())
          return false;
        FieldType ft = mgr.managedIndexSchema.getFieldTypeByName(type);
        if (ft == null) {
          op.addError("No such field type '" + type + "'");
          return  false;
        }
        try {
          SchemaField field = SchemaField.create(name, ft, op.getValuesExcluding(NAME, TYPE)); 
          mgr.managedIndexSchema 
              = mgr.managedIndexSchema.addDynamicFields(singletonList(field), Collections.emptyMap(), false);
          return true;
        } catch (Exception e) {
          op.addError(getErrorStr(e));
          return false;
        }
      }
    },
    DELETE_FIELD_TYPE("delete-field-type") {
      @Override public boolean perform(CommandOperation op, SchemaManager mgr) {
        String name = op.getStr(NAME);
        if (op.hasError())
          return false;
        if ( ! op.getValuesExcluding(NAME).isEmpty()) {
          op.addError("Only the '" + NAME + "' param is allowed with the 'delete-field-type' operation");
          return false;
        }
        try {
          mgr.managedIndexSchema = mgr.managedIndexSchema.deleteFieldTypes(singleton(name));
          return true;
        } catch (Exception e) {
          op.addError(getErrorStr(e));
          return false;
        }
      }
    },
    DELETE_COPY_FIELD("delete-copy-field") {
      @Override public boolean perform(CommandOperation op, SchemaManager mgr) {
        String source = op.getStr(SOURCE);
        List<String> dests = op.getStrs(DESTINATION);
        if (op.hasError())
          return false;
        if ( ! op.getValuesExcluding(SOURCE, DESTINATION).isEmpty()) {
          op.addError("Only the '" + SOURCE + "' and '" + DESTINATION 
              + "' params are allowed with the 'delete-copy-field' operation");
          return false;
        }
        try {
          mgr.managedIndexSchema = mgr.managedIndexSchema.deleteCopyFields(singletonMap(source, dests));
          return true;
        } catch (Exception e) {
          op.addError(getErrorStr(e));
          return false;
        }
      }
    },
    DELETE_FIELD("delete-field") {
      @Override public boolean perform(CommandOperation op, SchemaManager mgr) {
        String name = op.getStr(NAME);
        if (op.hasError())
          return false;
        if ( ! op.getValuesExcluding(NAME).isEmpty()) {
          op.addError("Only the '" + NAME + "' param is allowed with the 'delete-field' operation");
          return false;
        }                                                            
        try {
          mgr.managedIndexSchema = mgr.managedIndexSchema.deleteFields(singleton(name));
          return true;
        } catch (Exception e) {
          op.addError(getErrorStr(e));
          return false;
        }                                                             
      }
    },
    DELETE_DYNAMIC_FIELD("delete-dynamic-field") {
      @Override public boolean perform(CommandOperation op, SchemaManager mgr) {
        String name = op.getStr(NAME);
        if (op.hasError())
          return false;
        if ( ! op.getValuesExcluding(NAME).isEmpty()) {
          op.addError("Only the '" + NAME + "' param is allowed with the 'delete-dynamic-field' operation");
          return false;
        }
        try {
          mgr.managedIndexSchema = mgr.managedIndexSchema.deleteDynamicFields(singleton(name));
          return true;
        } catch (Exception e) {
          op.addError(getErrorStr(e));
          return false;
        }
      }
    },
    REPLACE_FIELD_TYPE("replace-field-type") {
      @Override public boolean perform(CommandOperation op, SchemaManager mgr) {
        String name = op.getStr(NAME);
        String className = op.getStr(CLASS_NAME);
        if (op.hasError())
          return false;
        try {
          mgr.managedIndexSchema = mgr.managedIndexSchema.replaceFieldType(name, className, op.getDataMap());
          return true;
        } catch (Exception e) {
          op.addError(getErrorStr(e));
          return false;
        }
      }
    },
    REPLACE_FIELD("replace-field") {
      @Override public boolean perform(CommandOperation op, SchemaManager mgr) {
        String name = op.getStr(NAME);
        String type = op.getStr(TYPE);
        if (op.hasError())
          return false;
        FieldType ft = mgr.managedIndexSchema.getFieldTypeByName(type);
        if (ft == null) {
          op.addError("No such field type '" + type + "'");
          return false;
        }
        try {
          mgr.managedIndexSchema = mgr.managedIndexSchema.replaceField(name, ft, op.getValuesExcluding(NAME, TYPE));
          return true;
        } catch (Exception e) {
          op.addError(getErrorStr(e));
          return false;
        }
      }
    },
    REPLACE_DYNAMIC_FIELD("replace-dynamic-field") {
      @Override public boolean perform(CommandOperation op, SchemaManager mgr) {
        String name = op.getStr(NAME);
        String type = op.getStr(TYPE);
        if (op.hasError())
          return false;
        FieldType ft = mgr.managedIndexSchema.getFieldTypeByName(type);
        if (ft == null) {
          op.addError("No such field type '" + type + "'");
          return  false;
        }
        try {
          mgr.managedIndexSchema = mgr.managedIndexSchema.replaceDynamicField(name, ft, op.getValuesExcluding(NAME, TYPE));
          return true;
        } catch (Exception e) {
          op.addError(getErrorStr(e));
          return false;
        }
      }
    };

    public abstract boolean perform(CommandOperation op, SchemaManager mgr);

    public static OpType get(String label) {
      return Nested.OP_TYPES.get(label);
    }

    private static class Nested { // Initializes contained static map before any enum ctor
      static final Map<String,OpType> OP_TYPES = new HashMap<>();
    }

    private OpType(String label) {
      Nested.OP_TYPES.put(label, this);
     }
   }
 
   public static String getErrorStr(Exception e) {
     StringBuilder sb = new StringBuilder();
    Throwable cause= e;
    for(int i =0;i<5;i++) {
    Throwable cause = e;
    for (int i = 0 ; i < 5 ; i++) {
       sb.append(cause.getMessage()).append("\n");
      if(cause.getCause() == null || cause.getCause() == cause) break;
      if (cause.getCause() == null || cause.getCause() == cause) break;
       cause = cause.getCause();
     }
     return sb.toString();
   }
 
  private boolean applyAddCopyField(CommandOperation op) {
    String src  = op.getStr(SOURCE);
    List<String> dest = op.getStrs(DESTINATION);
    if(op.hasError())
      return false;
    try {
      managedIndexSchema = managedIndexSchema.addCopyFields(Collections.<String,Collection<String>>singletonMap(src,dest), false);
      return true;
    } catch (Exception e) {
      op.addError(getErrorStr(e));
      return false;
    }
  }


  private boolean applyAddField( CommandOperation op) {
    String name = op.getStr(NAME);
    String type = op.getStr(TYPE);
    if(op.hasError())
      return false;
    FieldType ft = managedIndexSchema.getFieldTypeByName(type);
    if(ft==null){
      op.addError("No such field type '"+type+"'");
      return  false;
    }
    try {
      if(ADD_DYNAMIC_FIELD.equals(op.name)){
        managedIndexSchema = managedIndexSchema.addDynamicFields(
            singletonList(SchemaField.create(name, ft, op.getValuesExcluding(NAME, TYPE))),
            EMPTY_MAP,false);
      } else {
        managedIndexSchema = managedIndexSchema.addFields(
            singletonList( SchemaField.create(name, ft, op.getValuesExcluding(NAME, TYPE))),
            EMPTY_MAP,
            false);
      }
    } catch (Exception e) {
      op.addError(getErrorStr(e));
      return false;
    }
    return true;
  }

   public ManagedIndexSchema getFreshManagedSchema() throws IOException, KeeperException, InterruptedException {
     SolrResourceLoader resourceLoader = req.getCore().getResourceLoader();
     if (resourceLoader instanceof ZkSolrResourceLoader) {
      ZkSolrResourceLoader loader = (ZkSolrResourceLoader) resourceLoader;
       InputStream in = resourceLoader.openResource(req.getSchema().getResourceName());
       if (in instanceof ZkSolrResourceLoader.ZkByteArrayInputStream) {
         int version = ((ZkSolrResourceLoader.ZkByteArrayInputStream) in).getStat().getVersion();
@@ -284,15 +426,11 @@ public class SchemaManager {
             true,
             req.getSchema().getResourceName(),
             version,new Object());
      }else {
      } else {
         return (ManagedIndexSchema) req.getCore().getLatestSchema();
       }

     } else {
       return (ManagedIndexSchema) req.getCore().getLatestSchema();
     }



   }
 }
diff --git a/solr/core/src/java/org/apache/solr/schema/ZkIndexSchemaReader.java b/solr/core/src/java/org/apache/solr/schema/ZkIndexSchemaReader.java
index 1e88a448b7a..5b44cd57925 100644
-- a/solr/core/src/java/org/apache/solr/schema/ZkIndexSchemaReader.java
++ b/solr/core/src/java/org/apache/solr/schema/ZkIndexSchemaReader.java
@@ -103,10 +103,13 @@ public class ZkIndexSchemaReader implements OnReconnect {
       if (expectedZkVersion == -1 || oldSchema.schemaZkVersion < expectedZkVersion) {
         byte[] data = zkClient.getData(managedSchemaPath, watcher, stat, true);
         if (stat.getVersion() != oldSchema.schemaZkVersion) {
          log.info("Retrieved schema version "+stat.getVersion()+" from ZooKeeper");
          log.info("Retrieved schema version "+ stat.getVersion() + " from ZooKeeper");
           long start = System.nanoTime();
           InputSource inputSource = new InputSource(new ByteArrayInputStream(data));
          ManagedIndexSchema newSchema = oldSchema.reloadFields(inputSource, stat.getVersion());
          String resourceName = managedIndexSchemaFactory.getManagedSchemaResourceName();
          ManagedIndexSchema newSchema = new ManagedIndexSchema
              (managedIndexSchemaFactory.getConfig(), resourceName, inputSource,
                  managedIndexSchemaFactory.isMutable(), resourceName, stat.getVersion(), new Object());
           managedIndexSchemaFactory.setSchema(newSchema);
           long stop = System.nanoTime();
           log.info("Finished refreshing schema in " + TimeUnit.MILLISECONDS.convert(stop - start, TimeUnit.NANOSECONDS) + " ms");
diff --git a/solr/core/src/java/org/apache/solr/util/CommandOperation.java b/solr/core/src/java/org/apache/solr/util/CommandOperation.java
index ae1e6305ecb..3401780e50d 100644
-- a/solr/core/src/java/org/apache/solr/util/CommandOperation.java
++ b/solr/core/src/java/org/apache/solr/util/CommandOperation.java
@@ -56,12 +56,13 @@ public class CommandOperation {
     return o == null ? def : String.valueOf(o);
   }
 
  public Map<String, Object> getDataMap() {
  public Map<String,Object> getDataMap() {
     if (commandData instanceof Map) {
      return (Map) commandData;
      //noinspection unchecked
      return (Map<String,Object>)commandData;
     }
     addError(MessageFormat.format("The command ''{0}'' should have the values as a json object {key:val} format", name));
    return Collections.EMPTY_MAP;
    return Collections.emptyMap();
   }
 
   private Object getRootPrimitive() {
@@ -162,10 +163,11 @@ public class CommandOperation {
    * Get all the values from the metadata for the command
    * without the specified keys
    */
  public Map getValuesExcluding(String... keys) {
  public Map<String,Object> getValuesExcluding(String... keys) {
     getMapVal(null);
     if (hasError()) return emptyMap();//just to verify the type is Map
    LinkedHashMap<String, Object> cp = new LinkedHashMap<>((Map<String, ?>) commandData);
    @SuppressWarnings("unchecked") 
    LinkedHashMap<String,Object> cp = new LinkedHashMap<>((Map<String,?>)commandData);
     if (keys == null) return cp;
     for (String key : keys) {
       cp.remove(key);
diff --git a/solr/core/src/test/org/apache/solr/rest/schema/TestBulkSchemaAPI.java b/solr/core/src/test/org/apache/solr/rest/schema/TestBulkSchemaAPI.java
index 050fc082ecf..4fe2e07dbc0 100644
-- a/solr/core/src/test/org/apache/solr/rest/schema/TestBulkSchemaAPI.java
++ b/solr/core/src/test/org/apache/solr/rest/schema/TestBulkSchemaAPI.java
@@ -18,15 +18,12 @@ package org.apache.solr.rest.schema;
  */
 
 import org.apache.commons.io.FileUtils;
import org.apache.solr.SolrTestCaseJ4;
 import org.apache.solr.util.RestTestBase;
 import org.apache.solr.util.RestTestHarness;
import org.eclipse.jetty.servlet.ServletHolder;
 import org.junit.After;
 import org.junit.Before;
 import org.noggit.JSONParser;
 import org.noggit.ObjectBuilder;
import org.restlet.ext.servlet.ServerServlet;
 
 import java.io.File;
 import java.io.StringReader;
@@ -35,35 +32,22 @@ import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
 
 
 public class TestBulkSchemaAPI extends RestTestBase {
 
   private static File tmpSolrHome;
  private static File tmpConfDir;

  private static final String collection = "collection1";
  private static final String confDir = collection + "/conf";

 
   @Before
   public void before() throws Exception {
     tmpSolrHome = createTempDir().toFile();
    tmpConfDir = new File(tmpSolrHome, confDir);
     FileUtils.copyDirectory(new File(TEST_HOME()), tmpSolrHome.getAbsoluteFile());
 
    final SortedMap<ServletHolder,String> extraServlets = new TreeMap<>();
    final ServletHolder solrRestApi = new ServletHolder("SolrSchemaRestApi", ServerServlet.class);
    solrRestApi.setInitParameter("org.restlet.application", "org.apache.solr.rest.SolrSchemaRestApi");
    extraServlets.put(solrRestApi, "/schema/*");  // '/schema/*' matches '/schema', '/schema/', and '/schema/whatever...'

     System.setProperty("managed.schema.mutable", "true");
     System.setProperty("enable.update.log", "false");
 
     createJettyAndHarness(tmpSolrHome.getAbsolutePath(), "solrconfig-managed-schema.xml", "schema-rest.xml",
        "/solr", true, extraServlets);
        "/solr", true, null);
   }
 
   @After
@@ -81,7 +65,7 @@ public class TestBulkSchemaAPI extends RestTestBase {
 
   public void testMultipleAddFieldWithErrors() throws Exception {
 
    String payload = SolrTestCaseJ4.json( "{\n" +
    String payload = "{\n" +
         "    'add-field' : {\n" +
         "                 'name':'a1',\n" +
         "                 'type': 'string1',\n" +
@@ -93,10 +77,9 @@ public class TestBulkSchemaAPI extends RestTestBase {
         "                 'stored':true,\n" +
         "                 'indexed':true\n" +
         "                 }\n" +
        "   \n" +
        "    }");
        "    }";
 
    String response = restTestHarness.post("/schema?wt=json", payload);
    String response = restTestHarness.post("/schema?wt=json", json(payload));
     Map map = (Map) ObjectBuilder.getVal(new JSONParser(new StringReader(response)));
     List l = (List) map.get("errors");
 
@@ -111,6 +94,41 @@ public class TestBulkSchemaAPI extends RestTestBase {
 
 
   public void testMultipleCommands() throws Exception{
    RestTestHarness harness = restTestHarness;

    Map m = getObj(harness, "wdf_nocase", "fields");
    assertNotNull("'wdf_nocase' field does not exist in the schema", m);
    
    m = getObj(harness, "wdf_nocase", "fieldTypes");
    assertNotNull("'wdf_nocase' field type does not exist in the schema", m);
    
    m = getObj(harness, "boolean", "fieldTypes");
    assertNotNull("'boolean' field type does not exist in the schema", m);
    assertNull(m.get("sortMissingFirst"));
    assertTrue((Boolean)m.get("sortMissingLast"));
    
    m = getObj(harness, "name", "fields");
    assertNotNull("'name' field does not exist in the schema", m);
    assertEquals("nametext", m.get("type"));

    m = getObj(harness, "bind", "fields");
    assertNotNull("'bind' field does not exist in the schema", m);
    assertEquals("boolean", m.get("type"));

    m = getObj(harness, "attr_*", "dynamicFields");
    assertNotNull("'attr_*' dynamic field does not exist in the schema", m);
    assertEquals("text", m.get("type"));

    List l = getSourceCopyFields(harness, "*_i");
    Set s = new HashSet();
    assertEquals(4, l.size());
    s.add(((Map)l.get(0)).get("dest"));
    s.add(((Map)l.get(1)).get("dest"));
    s.add(((Map) l.get(2)).get("dest"));
    s.add(((Map) l.get(3)).get("dest"));
    assertTrue(s.contains("title"));
    assertTrue(s.contains("*_s"));

     String payload = "{\n" +
         "          'add-field' : {\n" +
         "                       'name':'a1',\n" +
@@ -126,48 +144,85 @@ public class TestBulkSchemaAPI extends RestTestBase {
         "                       },\n" +
         "          'add-dynamic-field' : {\n" +
         "                       'name' :'*_lol',\n" +
        "                        'type':'string',\n" +
        "                        'stored':true,\n" +
        "                        'indexed':true\n" +
        "                        },\n" +
        "                       'type':'string',\n" +
        "                       'stored':true,\n" +
        "                       'indexed':true\n" +
        "                       },\n" +
         "          'add-copy-field' : {\n" +
         "                       'source' :'a1',\n" +
        "                        'dest':['a2','hello_lol']\n" +
        "                        },\n" +
        "                       'dest':['a2','hello_lol']\n" +
        "                       },\n" +
         "          'add-field-type' : {\n" +
         "                       'name' :'mystr',\n" +
         "                       'class' : 'solr.StrField',\n" +
        "                        'sortMissingLast':'true'\n" +
        "                        },\n" +
        "                       'sortMissingLast':'true'\n" +
        "                       },\n" +
         "          'add-field-type' : {" +
        "                     'name' : 'myNewTxtField',\n" +
        "                     'class':'solr.TextField','positionIncrementGap':'100',\n" +
        "                     'analyzer' : {\n" +
        "                                  'charFilters':[\n" +
        "                                            {'class':'solr.PatternReplaceCharFilterFactory','replacement':'$1$1','pattern':'([a-zA-Z])\\\\\\\\1+'}\n" +
        "                       'name' : 'myNewTxtField',\n" +
        "                       'class':'solr.TextField',\n" +
        "                       'positionIncrementGap':'100',\n" +
        "                       'analyzer' : {\n" +
        "                               'charFilters':[\n" +
        "                                          {\n" +
        "                                           'class':'solr.PatternReplaceCharFilterFactory',\n" +
        "                                           'replacement':'$1$1',\n" +
        "                                           'pattern':'([a-zA-Z])\\\\\\\\1+'\n" +
        "                                          }\n" +
         "                                         ],\n" +
        "                     'tokenizer':{'class':'solr.WhitespaceTokenizerFactory'},\n" +
        "                     'filters':[\n" +
        "                             {'class':'solr.WordDelimiterFilterFactory','preserveOriginal':'0'},\n" +
        "                             {'class':'solr.StopFilterFactory','words':'stopwords.txt','ignoreCase':'true'},\n" +
        "                             {'class':'solr.LowerCaseFilterFactory'},\n" +
        "                             {'class':'solr.ASCIIFoldingFilterFactory'},\n" +
        "                             {'class':'solr.KStemFilterFactory'}\n" +
        "                  ]\n" +
        "                }\n" +
        "              }"+
        "          }";

    RestTestHarness harness = restTestHarness;


    String response = harness.post("/schema?wt=json", SolrTestCaseJ4.json( payload));
        "                               'tokenizer':{'class':'solr.WhitespaceTokenizerFactory'},\n" +
        "                               'filters':[\n" +
        "                                          {\n" +
        "                                           'class':'solr.WordDelimiterFilterFactory',\n" +
        "                                           'preserveOriginal':'0'\n" +
        "                                          },\n" +
        "                                          {\n" +
        "                                           'class':'solr.StopFilterFactory',\n" +
        "                                           'words':'stopwords.txt',\n" +
        "                                           'ignoreCase':'true'\n" +
        "                                          },\n" +
        "                                          {'class':'solr.LowerCaseFilterFactory'},\n" +
        "                                          {'class':'solr.ASCIIFoldingFilterFactory'},\n" +
        "                                          {'class':'solr.KStemFilterFactory'}\n" +
        "                                         ]\n" +
        "                               }\n" +
        "                       },\n"+
        "          'add-field' : {\n" +
        "                       'name':'a3',\n" +
        "                       'type': 'myNewTxtField',\n" +
        "                       'stored':true,\n" +
        "                       'indexed':true\n" +
        "                       },\n" +
        "          'delete-field' : {'name':'wdf_nocase'},\n" +
        "          'delete-field-type' : {'name':'wdf_nocase'},\n" +
        "          'delete-dynamic-field' : {'name':'*_tt'},\n" +
        "          'delete-copy-field' : {'source':'a1', 'dest':'a2'},\n" +
        "          'delete-copy-field' : {'source':'*_i', 'dest':['title', '*_s']},\n" +
        "          'replace-field-type' : {\n" +
        "                       'name':'boolean',\n" +
        "                       'class':'solr.BoolField',\n" +
        "                       'sortMissingFirst':true\n" +
        "                       },\n" +
        "          'replace-field' : {\n" +
        "                       'name':'name',\n" +
        "                       'type':'string',\n" +
        "                       'indexed':true,\n" +
        "                       'stored':true\n" +
        "                       },\n" +
        "          'replace-dynamic-field' : {\n" +
        "                       'name':'attr_*',\n" +
        "                       'type':'string',\n" +
        "                       'indexed':true,\n" +
        "                       'stored':true,\n" +
        "                       'multiValued':true\n" +
        "                       }\n" +
        "          }\n";
    
    String response = harness.post("/schema?wt=json", json(payload));
 
     Map map = (Map) ObjectBuilder.getVal(new JSONParser(new StringReader(response)));
    assertNull(response,  map.get("errors"));
    assertNull(response, map.get("errors"));
 

    Map m = getObj(harness, "a1", "fields");
    m = getObj(harness, "a1", "fields");
     assertNotNull("field a1 not created", m);
 
     assertEquals("string", m.get("type"));
@@ -182,29 +237,279 @@ public class TestBulkSchemaAPI extends RestTestBase {
     assertEquals(Boolean.TRUE, m.get("indexed"));
 
     m = getObj(harness,"*_lol", "dynamicFields");
    assertNotNull("field *_lol not created",m );
    assertNotNull("field *_lol not created", m);
 
     assertEquals("string", m.get("type"));
     assertEquals(Boolean.TRUE, m.get("stored"));
     assertEquals(Boolean.TRUE, m.get("indexed"));
 
    List l = getCopyFields(harness,"a1");
    Set s =new HashSet();
    assertEquals(2,l.size());
    l = getSourceCopyFields(harness, "a1");
    s = new HashSet();
    assertEquals(1, l.size());
     s.add(((Map) l.get(0)).get("dest"));
    s.add(((Map) l.get(1)).get("dest"));
     assertTrue(s.contains("hello_lol"));
    assertTrue(s.contains("a2"));
 
    m = getObj(harness,"mystr", "fieldTypes");
    l = getSourceCopyFields(harness, "*_i");
    s = new HashSet();
    assertEquals(2, l.size());
    s.add(((Map)l.get(0)).get("dest"));
    s.add(((Map) l.get(1)).get("dest"));
    assertFalse(s.contains("title"));
    assertFalse(s.contains("*_s"));

    m = getObj(harness, "mystr", "fieldTypes");
     assertNotNull(m);
    assertEquals("solr.StrField",m.get("class"));
    assertEquals("true",String.valueOf(m.get("sortMissingLast")));
    assertEquals("solr.StrField", m.get("class"));
    assertEquals("true", String.valueOf(m.get("sortMissingLast")));
 
    m = getObj(harness,"myNewTxtField", "fieldTypes");
    m = getObj(harness, "myNewTxtField", "fieldTypes");
     assertNotNull(m);
 
    m = getObj(harness, "a3", "fields");
    assertNotNull("field a3 not created", m);
    assertEquals("myNewTxtField", m.get("type"));

    m = getObj(harness, "wdf_nocase", "fields");
    assertNull("field 'wdf_nocase' not deleted", m);

    m = getObj(harness, "wdf_nocase", "fieldTypes");
    assertNull("field type 'wdf_nocase' not deleted", m);

    m = getObj(harness, "*_tt", "dynamicFields");
    assertNull("dynamic field '*_tt' not deleted", m);

    m = getObj(harness, "boolean", "fieldTypes");
    assertNotNull("'boolean' field type does not exist in the schema", m);
    assertNull(m.get("sortMissingLast"));
    assertTrue((Boolean)m.get("sortMissingFirst"));

    m = getObj(harness, "bind", "fields"); // this field will be rebuilt when "boolean" field type is replaced
    assertNotNull("'bind' field does not exist in the schema", m);

    m = getObj(harness, "name", "fields");
    assertNotNull("'name' field does not exist in the schema", m);
    assertEquals("string", m.get("type"));
 
    m = getObj(harness, "attr_*", "dynamicFields");
    assertNotNull("'attr_*' dynamic field does not exist in the schema", m);
    assertEquals("string", m.get("type"));
  }
  
  public void testDeleteAndReplace() throws Exception {
    RestTestHarness harness = restTestHarness;

    Map map = getObj(harness, "NewField1", "fields");
    assertNull("Field 'NewField1' already exists in the schema", map);

    map = getObj(harness, "NewField2", "fields");
    assertNull("Field 'NewField2' already exists in the schema", map);

    map = getObj(harness, "NewFieldType", "fieldTypes");
    assertNull("'NewFieldType' field type already exists in the schema", map);

    List list = getSourceCopyFields(harness, "NewField1");
    assertEquals("There is already a copy field with source 'NewField1' in the schema", 0, list.size());

    map = getObj(harness, "NewDynamicField1*", "dynamicFields");
    assertNull("Dynamic field 'NewDynamicField1*' already exists in the schema", map);

    map = getObj(harness, "NewDynamicField2*", "dynamicFields");
    assertNull("Dynamic field 'NewDynamicField2*' already exists in the schema", map);

    String cmds = "{\n" + 
        "     'add-field-type': {   'name':'NewFieldType',     'class':'solr.StrField'                    },\n" +
        "          'add-field': [{  'name':'NewField1',         'type':'NewFieldType'                    },\n" +
        "                        {  'name':'NewField2',         'type':'NewFieldType'                    },\n" +
        "                        {  'name':'NewField3',         'type':'NewFieldType'                    },\n" +
        "                        {  'name':'NewField4',         'type':'NewFieldType'                    }],\n" +
        "  'add-dynamic-field': [{  'name':'NewDynamicField1*', 'type':'NewFieldType'                    },\n" +
        "                        {  'name':'NewDynamicField2*', 'type':'NewFieldType'                    },\n" +
        "                        {  'name':'NewDynamicField3*', 'type':'NewFieldType'                    }],\n" +
        "     'add-copy-field': [{'source':'NewField1',         'dest':['NewField2', 'NewDynamicField1A']},\n" +
        "                        {'source':'NewDynamicField1*', 'dest':'NewField2'                       },\n" +
        "                        {'source':'NewDynamicField2*', 'dest':'NewField2'                       },\n" +
        "                        {'source':'NewDynamicField3*', 'dest':'NewField3'                       },\n" +
        "                        {'source':'NewField4',         'dest':'NewField3'                       }]\n" +
        "}\n";

    String response = harness.post("/schema?wt=json", json(cmds));

    map = (Map)ObjectBuilder.getVal(new JSONParser(new StringReader(response)));
    assertNull(response, map.get("errors"));

    map = getObj(harness, "NewFieldType", "fieldTypes");
    assertNotNull("'NewFieldType' is not in the schema", map);

    map = getObj(harness, "NewField1", "fields");
    assertNotNull("Field 'NewField1' is not in the schema", map);

    map = getObj(harness, "NewField2", "fields");
    assertNotNull("Field 'NewField2' is not in the schema", map);

    map = getObj(harness, "NewField3", "fields");
    assertNotNull("Field 'NewField3' is not in the schema", map);

    map = getObj(harness, "NewField4", "fields");
    assertNotNull("Field 'NewField4' is not in the schema", map);

    list = getSourceCopyFields(harness, "NewField1");
    Set set = new HashSet();
    for (Object obj : list) {
      set.add(((Map)obj).get("dest"));
    }
    assertEquals(2, list.size());
    assertTrue(set.contains("NewField2"));
    assertTrue(set.contains("NewDynamicField1A"));

    list = getSourceCopyFields(harness, "NewDynamicField1*");
    assertEquals(1, list.size());
    assertEquals("NewField2", ((Map)list.get(0)).get("dest"));

    list = getSourceCopyFields(harness, "NewDynamicField2*");
    assertEquals(1, list.size());
    assertEquals("NewField2", ((Map)list.get(0)).get("dest"));

    list = getSourceCopyFields(harness, "NewDynamicField3*");
    assertEquals(1, list.size());
    assertEquals("NewField3", ((Map)list.get(0)).get("dest"));

    list = getSourceCopyFields(harness, "NewField4");
    assertEquals(1, list.size());
    assertEquals("NewField3", ((Map)list.get(0)).get("dest"));

    cmds = "{'delete-field-type' : {'name':'NewFieldType'}}";
    response = harness.post("/schema?wt=json", json(cmds));
    map = (Map)ObjectBuilder.getVal(new JSONParser(new StringReader(response)));
    Object errors = map.get("errors");
    assertNotNull(errors);
    assertTrue(errors.toString().contains("Can't delete 'NewFieldType' because it's the field type of "));

    cmds = "{'delete-field' : {'name':'NewField1'}}";
    response = harness.post("/schema?wt=json", json(cmds));
    map = (Map)ObjectBuilder.getVal(new JSONParser(new StringReader(response)));
    errors = map.get("errors");
    assertNotNull(errors);
    assertTrue(errors.toString().contains
        ("Can't delete 'NewField1' because it's referred to by at least one copy field directive"));

    cmds = "{'delete-field' : {'name':'NewField2'}}";
    response = harness.post("/schema?wt=json", json(cmds));
    map = (Map)ObjectBuilder.getVal(new JSONParser(new StringReader(response)));
    errors = map.get("errors");
    assertNotNull(errors);
    assertTrue(errors.toString().contains
        ("Can't delete 'NewField2' because it's referred to by at least one copy field directive"));

    cmds = "{'replace-field' : {'name':'NewField1', 'type':'string'}}";
    response = harness.post("/schema?wt=json", json(cmds));
    map = (Map)ObjectBuilder.getVal(new JSONParser(new StringReader(response)));
    assertNull(map.get("errors"));
    // Make sure the copy field directives with source NewField1 are preserved
    list = getSourceCopyFields(harness, "NewField1");
    set = new HashSet();
    for (Object obj : list) {
      set.add(((Map)obj).get("dest"));
    }
    assertEquals(2, list.size());
    assertTrue(set.contains("NewField2"));
    assertTrue(set.contains("NewDynamicField1A"));

    cmds = "{'delete-dynamic-field' : {'name':'NewDynamicField1*'}}";
    response = harness.post("/schema?wt=json", json(cmds));
    map = (Map)ObjectBuilder.getVal(new JSONParser(new StringReader(response)));
    errors = map.get("errors");
    assertNotNull(errors);
    assertTrue(errors.toString().contains
        ("copyField dest :'NewDynamicField1A' is not an explicit field and doesn't match a dynamicField."));

    cmds = "{'replace-field' : {'name':'NewField2', 'type':'string'}}";
    response = harness.post("/schema?wt=json", json(cmds));
    map = (Map)ObjectBuilder.getVal(new JSONParser(new StringReader(response)));
    errors = map.get("errors");
    assertNull(errors);
    // Make sure the copy field directives with destination NewField2 are preserved
    list = getDestCopyFields(harness, "NewField2");
    set = new HashSet();
    for (Object obj : list) {
      set.add(((Map)obj).get("source"));
    }
    assertEquals(3, list.size());
    assertTrue(set.contains("NewField1"));
    assertTrue(set.contains("NewDynamicField1*"));
    assertTrue(set.contains("NewDynamicField2*"));

    cmds = "{'replace-dynamic-field' : {'name':'NewDynamicField2*', 'type':'string'}}";
    response = harness.post("/schema?wt=json", json(cmds));
    map = (Map)ObjectBuilder.getVal(new JSONParser(new StringReader(response)));
    errors = map.get("errors");
    assertNull(errors);
    // Make sure the copy field directives with source NewDynamicField2* are preserved
    list = getSourceCopyFields(harness, "NewDynamicField2*");
    assertEquals(1, list.size());
    assertEquals("NewField2", ((Map) list.get(0)).get("dest"));

    cmds = "{'replace-dynamic-field' : {'name':'NewDynamicField1*', 'type':'string'}}";
    response = harness.post("/schema?wt=json", json(cmds));
    map = (Map)ObjectBuilder.getVal(new JSONParser(new StringReader(response)));
    errors = map.get("errors");
    assertNull(errors);
    // Make sure the copy field directives with destinations matching NewDynamicField1* are preserved
    list = getDestCopyFields(harness, "NewDynamicField1A");
    assertEquals(1, list.size());
    assertEquals("NewField1", ((Map) list.get(0)).get("source"));

    cmds = "{'replace-field-type': {'name':'NewFieldType', 'class':'solr.BinaryField'}}";
    response = harness.post("/schema?wt=json", json(cmds));
    map = (Map)ObjectBuilder.getVal(new JSONParser(new StringReader(response)));
    assertNull(map.get("errors"));
    // Make sure the copy field directives with sources and destinations of type NewFieldType are preserved
    list = getDestCopyFields(harness, "NewField3");
    assertEquals(2, list.size());
    set = new HashSet();
    for (Object obj : list) {
      set.add(((Map)obj).get("source"));
    }
    assertTrue(set.contains("NewField4"));
    assertTrue(set.contains("NewDynamicField3*"));

    cmds = "{\n" +
        "  'delete-copy-field': [{'source':'NewField1',         'dest':['NewField2', 'NewDynamicField1A']},\n" +
        "                        {'source':'NewDynamicField1*', 'dest':'NewField2'                       },\n" +
        "                        {'source':'NewDynamicField2*', 'dest':'NewField2'                       },\n" +
        "                        {'source':'NewDynamicField3*', 'dest':'NewField3'                       },\n" +
        "                        {'source':'NewField4',         'dest':'NewField3'                       }]\n" +
        "}\n";
    response = harness.post("/schema?wt=json", json(cmds));
    map = (Map)ObjectBuilder.getVal(new JSONParser(new StringReader(response)));
    assertNull(map.get("errors"));
    list = getSourceCopyFields(harness, "NewField1");
    assertEquals(0, list.size());
    list = getSourceCopyFields(harness, "NewDynamicField1*");
    assertEquals(0, list.size());
    list = getSourceCopyFields(harness, "NewDynamicField2*");
    assertEquals(0, list.size());
    list = getSourceCopyFields(harness, "NewDynamicField3*");
    assertEquals(0, list.size());
    list = getSourceCopyFields(harness, "NewField4");
    assertEquals(0, list.size());
    
    cmds = "{'delete-field': [{'name':'NewField1'},{'name':'NewField2'},{'name':'NewField3'},{'name':'NewField4'}]}";
    response = harness.post("/schema?wt=json", json(cmds));
    map = (Map)ObjectBuilder.getVal(new JSONParser(new StringReader(response)));
    assertNull(map.get("errors"));

    cmds = "{'delete-dynamic-field': [{'name':'NewDynamicField1*'}," +
        "                             {'name':'NewDynamicField2*'},\n" +
        "                             {'name':'NewDynamicField3*'}]\n" +
        "}\n";
    response = harness.post("/schema?wt=json", json(cmds));
    map = (Map)ObjectBuilder.getVal(new JSONParser(new StringReader(response)));
    assertNull(map.get("errors"));
    
    cmds = "{'delete-field-type':{'name':'NewFieldType'}}";
    response = harness.post("/schema?wt=json", json(cmds));
    map = (Map)ObjectBuilder.getVal(new JSONParser(new StringReader(response)));
    assertNull(map.get("errors"));
   }
 
   public static Map getObj(RestTestHarness restHarness, String fld, String key) throws Exception {
@@ -212,7 +517,8 @@ public class TestBulkSchemaAPI extends RestTestBase {
     List l = (List) ((Map)map.get("schema")).get(key);
     for (Object o : l) {
       Map m = (Map) o;
      if(fld.equals(m.get("name"))) return m;
      if (fld.equals(m.get("name"))) 
        return m;
     }
     return null;
   }
@@ -226,17 +532,25 @@ public class TestBulkSchemaAPI extends RestTestBase {
     return (Map) ObjectBuilder.getVal(new JSONParser(new StringReader(response)));
   }
 
  public static List getCopyFields(RestTestHarness harness, String src) throws Exception {
  public static List getSourceCopyFields(RestTestHarness harness, String src) throws Exception {
     Map map = getRespMap(harness);
     List l = (List) ((Map)map.get("schema")).get("copyFields");
     List result = new ArrayList();
     for (Object o : l) {
       Map m = (Map) o;
      if(src.equals(m.get("source"))) result.add(m);
      if (src.equals(m.get("source"))) result.add(m);
     }
     return result;

   }
 

  public static List getDestCopyFields(RestTestHarness harness, String dest) throws Exception {
    Map map = getRespMap(harness);
    List l = (List) ((Map)map.get("schema")).get("copyFields");
    List result = new ArrayList();
    for (Object o : l) {
      Map m = (Map) o;
      if (dest.equals(m.get("dest"))) result.add(m);
    }
    return result;
  }
 }
diff --git a/solr/core/src/test/org/apache/solr/schema/TestBulkSchemaConcurrent.java b/solr/core/src/test/org/apache/solr/schema/TestBulkSchemaConcurrent.java
index b3ed82a9d7f..af9aba17315 100644
-- a/solr/core/src/test/org/apache/solr/schema/TestBulkSchemaConcurrent.java
++ b/solr/core/src/test/org/apache/solr/schema/TestBulkSchemaConcurrent.java
@@ -19,7 +19,7 @@ package org.apache.solr.schema;
 
 
 import static java.text.MessageFormat.format;
import static org.apache.solr.rest.schema.TestBulkSchemaAPI.getCopyFields;
import static org.apache.solr.rest.schema.TestBulkSchemaAPI.getSourceCopyFields;
 import static org.apache.solr.rest.schema.TestBulkSchemaAPI.getObj;
 
 import java.io.StringReader;
@@ -87,20 +87,20 @@ public class TestBulkSchemaConcurrent  extends AbstractFullDistribZkTestBase {
     Thread[] threads = new Thread[threadCount];
     final List<List> collectErrors = new ArrayList<>();
 

    for(int i=0;i<threadCount;i++){
    for (int i = 0 ; i < threadCount ; i++) {
       final int finalI = i;
       threads[i] = new Thread(){
         @Override
         public void run() {
          ArrayList errs = new ArrayList();
          collectErrors.add(errs);
           try {
            ArrayList errs = new ArrayList();
            collectErrors.add(errs);
            invokeBulkCall(finalI,errs);
            invokeBulkAddCall(finalI, errs);
            invokeBulkReplaceCall(finalI, errs);
            invokeBulkDeleteCall(finalI, errs);
           } catch (Exception e) {
             e.printStackTrace();
           }

         }
       };
 
@@ -112,19 +112,16 @@ public class TestBulkSchemaConcurrent  extends AbstractFullDistribZkTestBase {
     boolean success = true;
 
     for (List e : collectErrors) {
      if(e != null &&  !e.isEmpty()){
      if (e != null &&  !e.isEmpty()) {
         success = false;
         log.error(e.toString());
       }

     }
 
     assertTrue(collectErrors.toString(), success);


   }
 
  private void invokeBulkCall(int seed, ArrayList<String> errs) throws Exception {
  private void invokeBulkAddCall(int seed, ArrayList<String> errs) throws Exception {
     String payload = "{\n" +
         "          'add-field' : {\n" +
         "                       'name':'replaceFieldA',\n" +
@@ -134,39 +131,35 @@ public class TestBulkSchemaConcurrent  extends AbstractFullDistribZkTestBase {
         "                       },\n" +
         "          'add-dynamic-field' : {\n" +
         "                       'name' :'replaceDynamicField',\n" +
        "                        'type':'string',\n" +
        "                        'stored':true,\n" +
        "                        'indexed':true\n" +
        "                        },\n" +
        "                       'type':'string',\n" +
        "                       'stored':true,\n" +
        "                       'indexed':true\n" +
        "                       },\n" +
         "          'add-copy-field' : {\n" +
         "                       'source' :'replaceFieldA',\n" +
        "                        'dest':['replaceDynamicCopyFieldDest']\n" +
        "                        },\n" +
        "                       'dest':['replaceDynamicCopyFieldDest']\n" +
        "                       },\n" +
         "          'add-field-type' : {\n" +
         "                       'name' :'myNewFieldTypeName',\n" +
         "                       'class' : 'solr.StrField',\n" +
        "                        'sortMissingLast':'true'\n" +
        "                        }\n" +
        "\n" +
        "                       'sortMissingLast':'true'\n" +
        "                       }\n" +
         " }";
     String aField = "a" + seed;
     String dynamicFldName = "*_lol" + seed;
     String dynamicCopyFldDest = "hello_lol"+seed;
     String newFieldTypeName = "mystr" + seed;
 

    RestTestHarness publisher = restTestHarnesses.get(r.nextInt(restTestHarnesses.size()));
     payload = payload.replace("replaceFieldA", aField);

     payload = payload.replace("replaceDynamicField", dynamicFldName);
    payload = payload.replace("dynamicFieldLol","lol"+seed);

     payload = payload.replace("replaceDynamicCopyFieldDest",dynamicCopyFldDest);
     payload = payload.replace("myNewFieldTypeName", newFieldTypeName);

    RestTestHarness publisher = restTestHarnesses.get(r.nextInt(restTestHarnesses.size()));
     String response = publisher.post("/schema?wt=json", SolrTestCaseJ4.json(payload));
     Map map = (Map) ObjectBuilder.getVal(new JSONParser(new StringReader(response)));
     Object errors = map.get("errors");
    if(errors!= null){
    if (errors != null) {
       errs.add(new String(ZkStateReader.toJSON(errors), StandardCharsets.UTF_8));
       return;
     }
@@ -176,10 +169,8 @@ public class TestBulkSchemaConcurrent  extends AbstractFullDistribZkTestBase {
     RestTestHarness harness = restTestHarnesses.get(r.nextInt(restTestHarnesses.size()));
     try {
       long startTime = System.nanoTime();
      boolean success = false;
       long maxTimeoutMillis = 100000;
      while (!success
          && TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS) < maxTimeoutMillis) {
      while (TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS) < maxTimeoutMillis) {
         errmessages.clear();
         Map m = getObj(harness, aField, "fields");
         if (m == null) errmessages.add(format("field {0} not created", aField));
@@ -187,30 +178,163 @@ public class TestBulkSchemaConcurrent  extends AbstractFullDistribZkTestBase {
         m = getObj(harness, dynamicFldName, "dynamicFields");
         if (m == null) errmessages.add(format("dynamic field {0} not created", dynamicFldName));
         
        List l = getCopyFields(harness, "a1");
        if (!checkCopyField(l, aField, dynamicCopyFldDest)) errmessages
            .add(format("CopyField source={0},dest={1} not created", aField, dynamicCopyFldDest));
        List l = getSourceCopyFields(harness, aField);
        if (!checkCopyField(l, aField, dynamicCopyFldDest))
          errmessages.add(format("CopyField source={0},dest={1} not created", aField, dynamicCopyFldDest));
        
        m = getObj(harness, newFieldTypeName, "fieldTypes");
        if (m == null) errmessages.add(format("new type {0}  not created", newFieldTypeName));
        
        if (errmessages.isEmpty()) break;
         
        m = getObj(harness, "mystr", "fieldTypes");
        if (m == null) errmessages.add(format("new type {}  not created", newFieldTypeName));
         Thread.sleep(10);
       }
     } finally {
       harness.close();
     }
    if(!errmessages.isEmpty()){
    if (!errmessages.isEmpty()) {
      errs.addAll(errmessages);
    }
  }

  private void invokeBulkReplaceCall(int seed, ArrayList<String> errs) throws Exception {
    String payload = "{\n" +
        "          'replace-field' : {\n" +
        "                       'name':'replaceFieldA',\n" +
        "                       'type': 'text',\n" +
        "                       'stored':true,\n" +
        "                       'indexed':true\n" +
        "                       },\n" +
        "          'replace-dynamic-field' : {\n" +
        "                       'name' :'replaceDynamicField',\n" +
        "                        'type':'text',\n" +
        "                        'stored':true,\n" +
        "                        'indexed':true\n" +
        "                        },\n" +
        "          'replace-field-type' : {\n" +
        "                       'name' :'myNewFieldTypeName',\n" +
        "                       'class' : 'solr.TextField'\n" +
        "                        }\n" +
        " }";
    String aField = "a" + seed;
    String dynamicFldName = "*_lol" + seed;
    String dynamicCopyFldDest = "hello_lol"+seed;
    String newFieldTypeName = "mystr" + seed;

    payload = payload.replace("replaceFieldA", aField);
    payload = payload.replace("replaceDynamicField", dynamicFldName);
    payload = payload.replace("myNewFieldTypeName", newFieldTypeName);

    RestTestHarness publisher = restTestHarnesses.get(r.nextInt(restTestHarnesses.size()));
    String response = publisher.post("/schema?wt=json", SolrTestCaseJ4.json(payload));
    Map map = (Map) ObjectBuilder.getVal(new JSONParser(new StringReader(response)));
    Object errors = map.get("errors");
    if (errors != null) {
      errs.add(new String(ZkStateReader.toJSON(errors), StandardCharsets.UTF_8));
      return;
    }

    //get another node
    Set<String> errmessages = new HashSet<>();
    RestTestHarness harness = restTestHarnesses.get(r.nextInt(restTestHarnesses.size()));
    try {
      long startTime = System.nanoTime();
      long maxTimeoutMillis = 100000;
      while (TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS) < maxTimeoutMillis) {
        errmessages.clear();
        Map m = getObj(harness, aField, "fields");
        if (m == null) errmessages.add(format("field {0} no longer present", aField));

        m = getObj(harness, dynamicFldName, "dynamicFields");
        if (m == null) errmessages.add(format("dynamic field {0} no longer present", dynamicFldName));

        List l = getSourceCopyFields(harness, aField);
        if (!checkCopyField(l, aField, dynamicCopyFldDest))
          errmessages.add(format("CopyField source={0},dest={1} no longer present", aField, dynamicCopyFldDest));

        m = getObj(harness, newFieldTypeName, "fieldTypes");
        if (m == null) errmessages.add(format("new type {0} no longer present", newFieldTypeName));

        if (errmessages.isEmpty()) break;

        Thread.sleep(10);
      }
    } finally {
      harness.close();
    }
    if (!errmessages.isEmpty()) {
      errs.addAll(errmessages);
    }
  }

  private void invokeBulkDeleteCall(int seed, ArrayList<String> errs) throws Exception {
    String payload = "{\n" +
        "          'delete-field' : {'name':'replaceFieldA'},\n" +
        "          'delete-dynamic-field' : {'name' :'replaceDynamicField'},\n" +
        "          'delete-copy-field' : {\n" +
        "                       'source' :'replaceFieldA',\n" +
        "                       'dest':['replaceDynamicCopyFieldDest']\n" +
        "                       },\n" +
        "          'delete-field-type' : {'name' :'myNewFieldTypeName'}\n" +
        " }";
    String aField = "a" + seed;
    String dynamicFldName = "*_lol" + seed;
    String dynamicCopyFldDest = "hello_lol"+seed;
    String newFieldTypeName = "mystr" + seed;

    payload = payload.replace("replaceFieldA", aField);
    payload = payload.replace("replaceDynamicField", dynamicFldName);
    payload = payload.replace("replaceDynamicCopyFieldDest",dynamicCopyFldDest);
    payload = payload.replace("myNewFieldTypeName", newFieldTypeName);

    RestTestHarness publisher = restTestHarnesses.get(r.nextInt(restTestHarnesses.size()));
    String response = publisher.post("/schema?wt=json", SolrTestCaseJ4.json(payload));
    Map map = (Map) ObjectBuilder.getVal(new JSONParser(new StringReader(response)));
    Object errors = map.get("errors");
    if (errors != null) {
      errs.add(new String(ZkStateReader.toJSON(errors), StandardCharsets.UTF_8));
      return;
    }

    //get another node
    Set<String> errmessages = new HashSet<>();
    RestTestHarness harness = restTestHarnesses.get(r.nextInt(restTestHarnesses.size()));
    try {
      long startTime = System.nanoTime();
      long maxTimeoutMillis = 100000;
      while (TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS) < maxTimeoutMillis) {
        errmessages.clear();
        Map m = getObj(harness, aField, "fields");
        if (m != null) errmessages.add(format("field {0} still exists", aField));

        m = getObj(harness, dynamicFldName, "dynamicFields");
        if (m != null) errmessages.add(format("dynamic field {0} still exists", dynamicFldName));

        List l = getSourceCopyFields(harness, aField);
        if (checkCopyField(l, aField, dynamicCopyFldDest))
          errmessages.add(format("CopyField source={0},dest={1} still exists", aField, dynamicCopyFldDest));

        m = getObj(harness, newFieldTypeName, "fieldTypes");
        if (m != null) errmessages.add(format("new type {0} still exists", newFieldTypeName));

        if (errmessages.isEmpty()) break;

        Thread.sleep(10);
      }
    } finally {
      harness.close();
    }
    if (!errmessages.isEmpty()) {
       errs.addAll(errmessages);
     }
   }
 
   private boolean checkCopyField(List<Map> l, String src, String dest) {
    if(l == null) return false;
    if (l == null) return false;
     for (Map map : l) {
      if(src.equals(map.get("source")) &&
          dest.equals(map.get("dest"))) return true;
      if (src.equals(map.get("source")) && dest.equals(map.get("dest"))) 
        return true;
     }
     return false;
   }


 }
diff --git a/solr/core/src/test/org/apache/solr/schema/TestSchemaManager.java b/solr/core/src/test/org/apache/solr/schema/TestSchemaManager.java
index c4892c5f29e..6ff689a187d 100644
-- a/solr/core/src/test/org/apache/solr/schema/TestSchemaManager.java
++ b/solr/core/src/test/org/apache/solr/schema/TestSchemaManager.java
@@ -35,44 +35,40 @@ public class TestSchemaManager extends SolrTestCaseJ4 {
   @Test
   public void testParsing() throws IOException {
     String x = "{\n" +
        " \"add-field\" : {\n" +
        "              \"name\":\"a\",\n" +
        "              \"type\": \"string\",\n" +
        "              \"stored\":true,\n" +
        "              \"indexed\":false\n" +
        " 'add-field' : {\n" +
        "              'name':'a',\n" +
        "              'type': 'string',\n" +
        "              'stored':true,\n" +
        "              'indexed':false\n" +
         "              },\n" +
        " \"add-field\" : {\n" +
        "              \"name\":\"b\",\n" +
        "              \"type\": \"string\",\n" +
        "              \"stored\":true,\n" +
        "              \"indexed\":false\n" +
        " 'add-field' : {\n" +
        "              'name':'b',\n" +
        "              'type': 'string',\n" +
        "              'stored':true,\n" +
        "              'indexed':false\n" +
         "              }\n" +
         "\n" +
         "}";
 
    List<CommandOperation> ops = CommandOperation.parse(new StringReader(x));
    List<CommandOperation> ops = CommandOperation.parse(new StringReader(json(x)));
     assertEquals(2,ops.size());
     assertTrue( CommandOperation.captureErrors(ops).isEmpty());
 
    x = " {\"add-field\" : [{\n" +
        "                                 \"name\":\"a1\",\n" +
        "                                 \"type\": \"string\",\n" +
        "                                 \"stored\":true,\n" +
        "                                 \"indexed\":false\n" +
        "                                 },\n" +
        "                            {\n" +
        "                            \"name\":\"a2\",\n" +
        "                             \"type\": \"string\",\n" +
        "                             \"stored\":true,\n" +
        "                             \"indexed\":true\n" +
        "                             }]\n" +
        "           }";
    ops = CommandOperation.parse(new StringReader(x));
    x = " {'add-field' : [{\n" +
        "                       'name':'a1',\n" +
        "                       'type': 'string',\n" +
        "                       'stored':true,\n" +
        "                       'indexed':false\n" +
        "                      },\n" +
        "                      {\n" +
        "                       'name':'a2',\n" +
        "                       'type': 'string',\n" +
        "                       'stored':true,\n" +
        "                       'indexed':true\n" +
        "                      }]\n" +
        "      }";
    ops = CommandOperation.parse(new StringReader(json(x)));
     assertEquals(2,ops.size());
    assertTrue( CommandOperation.captureErrors(ops).isEmpty());

    assertTrue(CommandOperation.captureErrors(ops).isEmpty());
   }



 }
- 
2.19.1.windows.1

