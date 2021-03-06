From e3ba95b8b84e0dd6161480050dc8dd9a010dbde4 Mon Sep 17 00:00:00 2001
From: Shalin Shekhar Mangar <shalin@apache.org>
Date: Thu, 20 Nov 2008 07:19:41 +0000
Subject: [PATCH] SOLR-873 -- Fix case-sensitive field names and columns

git-svn-id: https://svn.apache.org/repos/asf/lucene/solr/trunk@719187 13f79535-47bb-0310-9956-ffa450edef68
--
 contrib/dataimporthandler/CHANGES.txt         |  2 +
 .../solr/handler/dataimport/DataConfig.java   |  8 ----
 .../solr/handler/dataimport/DataImporter.java | 42 +++++--------------
 .../solr/handler/dataimport/DocBuilder.java   |  4 +-
 .../handler/dataimport/TestDocBuilder.java    |  9 ----
 .../handler/dataimport/TestDocBuilder2.java   | 21 ++++++++++
 6 files changed, 36 insertions(+), 50 deletions(-)

diff --git a/contrib/dataimporthandler/CHANGES.txt b/contrib/dataimporthandler/CHANGES.txt
index 590ab447299..9e4cea22a02 100644
-- a/contrib/dataimporthandler/CHANGES.txt
++ b/contrib/dataimporthandler/CHANGES.txt
@@ -58,6 +58,8 @@ Bug Fixes
 
 7. SOLR-864:  DataImportHandler does not catch and log Errors (shalin)
 
8. SOLR-873:  Fix case-sensitive field names and columns (Jon Baer, shalin)

 Documentation
 ----------------------
 
diff --git a/contrib/dataimporthandler/src/main/java/org/apache/solr/handler/dataimport/DataConfig.java b/contrib/dataimporthandler/src/main/java/org/apache/solr/handler/dataimport/DataConfig.java
index 96227a45fea..c8a92fe08c3 100644
-- a/contrib/dataimporthandler/src/main/java/org/apache/solr/handler/dataimport/DataConfig.java
++ b/contrib/dataimporthandler/src/main/java/org/apache/solr/handler/dataimport/DataConfig.java
@@ -191,8 +191,6 @@ public class DataConfig {
 
     public boolean multiValued = false;
 
    public String nameOrColName;

     public Map<String, String> allAttributes = new HashMap<String, String>() {
       public String put(String key, String value) {
         if (super.containsKey(key))
@@ -211,12 +209,6 @@ public class DataConfig {
       allAttributes.putAll(getAllAttributes(e));
     }
 
    public Field(String name, boolean b) {
      name = nameOrColName = column = name;
      multiValued = b;

    }

     public String getName() {
       return name == null ? column : name;
     }
diff --git a/contrib/dataimporthandler/src/main/java/org/apache/solr/handler/dataimport/DataImporter.java b/contrib/dataimporthandler/src/main/java/org/apache/solr/handler/dataimport/DataImporter.java
index 8a361ebe430..4b0b2211203 100644
-- a/contrib/dataimporthandler/src/main/java/org/apache/solr/handler/dataimport/DataImporter.java
++ b/contrib/dataimporthandler/src/main/java/org/apache/solr/handler/dataimport/DataImporter.java
@@ -126,24 +126,12 @@ public class DataImporter {
     }
     for (Map.Entry<String, DataConfig.Field> entry : fields.entrySet()) {
       DataConfig.Field fld = entry.getValue();
      FieldType fieldType = null;

      try {
        fieldType = schema.getDynamicFieldType(fld.name);
      } catch (RuntimeException ex) {
        // Ignore because it may not be a dynamic field
      }

      if (fld.name != null) {
        if (schema.getFields().get(fld.name) == null && fieldType == null) {
          errors
                  .add("The field :"
                          + fld.name
                          + " present in DataConfig does not have a counterpart in Solr Schema");
      SchemaField field = schema.getFieldOrNull(fld.getName());
      if (field == null) {
        field = config.lowerNameVsSchemaField.get(fld.getName().toLowerCase());
        if (field == null)  {
          errors.add("The field :" + fld.getName() + " present in DataConfig does not have a counterpart in Solr Schema");
         }
      } else if (schema.getFields().get(fld.column) == null
              && fieldType == null) {
        LOG.info("Column : " + fld.column + " is not a schema field");
       }
     }
 
@@ -201,30 +189,22 @@ public class DataImporter {
 
     if (e.fields != null) {
       for (DataConfig.Field f : e.fields) {
        f.nameOrColName = f.getName();
         if (schema != null) {
           SchemaField schemaField = schema.getFieldOrNull(f.getName());
          if (schemaField == null)  {
            schemaField = config.lowerNameVsSchemaField.get(f.getName().toLowerCase());
            if(schemaField != null) f.name = schemaField.getName();
          }
           if (schemaField != null) {
             f.multiValued = schemaField.multiValued();
             f.allAttributes.put(MULTI_VALUED, Boolean.toString(schemaField
                     .multiValued()));
             f.allAttributes.put(TYPE, schemaField.getType().getTypeName());
            f.allAttributes.put("indexed", Boolean
                    .toString(schemaField.indexed()));
            f.allAttributes.put("indexed", Boolean.toString(schemaField.indexed()));
             f.allAttributes.put("stored", Boolean.toString(schemaField.stored()));
             f.allAttributes.put("defaultValue", schemaField.getDefaultValue());
           } else {

            try {
              f.allAttributes.put(TYPE, schema.getDynamicFieldType(f.getName())
                      .getTypeName());
              f.allAttributes.put(MULTI_VALUED, "true");
              f.multiValued = true;
            } catch (RuntimeException e2) {
              LOG.info("Field in data-config.xml - " + f.getName()
                      + " not found in schema.xml");
              f.toWrite = false;
            }
            f.toWrite = false;
           }
         }
         fields.put(f.getName(), f);
diff --git a/contrib/dataimporthandler/src/main/java/org/apache/solr/handler/dataimport/DocBuilder.java b/contrib/dataimporthandler/src/main/java/org/apache/solr/handler/dataimport/DocBuilder.java
index 68822d6e12b..c9d46fd071e 100644
-- a/contrib/dataimporthandler/src/main/java/org/apache/solr/handler/dataimport/DocBuilder.java
++ b/contrib/dataimporthandler/src/main/java/org/apache/solr/handler/dataimport/DocBuilder.java
@@ -379,12 +379,12 @@ public class DocBuilder {
           sf = dataImporter.getConfig().lowerNameVsSchemaField.get(key.toLowerCase());
         }
         if (sf != null) {
          addFieldToDoc(entry.getValue(), key, 1.0f, sf.multiValued(), doc);
          addFieldToDoc(entry.getValue(), sf.getName(), 1.0f, sf.multiValued(), doc);
         }
         //else do nothing. if we add it it may fail
       } else {
         if (field != null && field.toWrite) {
          addFieldToDoc(entry.getValue(), key, field.boost, field.multiValued, doc);
          addFieldToDoc(entry.getValue(), field.getName(), field.boost, field.multiValued, doc);
         }
       }
 
diff --git a/contrib/dataimporthandler/src/test/java/org/apache/solr/handler/dataimport/TestDocBuilder.java b/contrib/dataimporthandler/src/test/java/org/apache/solr/handler/dataimport/TestDocBuilder.java
index 890dfe3e7eb..251970821a3 100644
-- a/contrib/dataimporthandler/src/test/java/org/apache/solr/handler/dataimport/TestDocBuilder.java
++ b/contrib/dataimporthandler/src/test/java/org/apache/solr/handler/dataimport/TestDocBuilder.java
@@ -49,9 +49,6 @@ public class TestDocBuilder {
       di.loadDataConfig(dc_singleEntity);
       DataConfig cfg = di.getConfig();
       DataConfig.Entity ent = cfg.documents.get(0).entities.get(0);
      for (DataConfig.Field field : ent.fields) {
        field.nameOrColName = field.name = field.column;
      }
       MockDataSource.setIterator("select * from x", new ArrayList().iterator());
       ent.dataSrc = new MockDataSource();
       ent.isDocRoot = true;
@@ -80,9 +77,6 @@ public class TestDocBuilder {
       di.loadDataConfig(dc_singleEntity);
       DataConfig cfg = di.getConfig();
       DataConfig.Entity ent = cfg.documents.get(0).entities.get(0);
      for (DataConfig.Field field : ent.fields) {
        field.nameOrColName = field.name = field.column;
      }
       List l = new ArrayList();
       l.add(createMap("id", 1, "desc", "one"));
       MockDataSource.setIterator("select * from x", l.iterator());
@@ -125,9 +119,6 @@ public class TestDocBuilder {
       ent.isDocRoot = true;
       DataImporter.RequestParams rp = new DataImporter.RequestParams();
       rp.command = "full-import";
      for (DataConfig.Field field : ent.fields) {
        field.nameOrColName = field.name = field.column;
      }
       List l = new ArrayList();
       l.add(createMap("id", 1, "desc", "one"));
       l.add(createMap("id", 2, "desc", "two"));
diff --git a/contrib/dataimporthandler/src/test/java/org/apache/solr/handler/dataimport/TestDocBuilder2.java b/contrib/dataimporthandler/src/test/java/org/apache/solr/handler/dataimport/TestDocBuilder2.java
index 910d608be96..2d2677f3f5e 100644
-- a/contrib/dataimporthandler/src/test/java/org/apache/solr/handler/dataimport/TestDocBuilder2.java
++ b/contrib/dataimporthandler/src/test/java/org/apache/solr/handler/dataimport/TestDocBuilder2.java
@@ -68,6 +68,18 @@ public class TestDocBuilder2 extends AbstractDataImportHandlerTest {
     assertQ(req("id:1"), "//*[@numFound='1']");
   }
 
  @Test
  @SuppressWarnings("unchecked")
  public void testSingleEntity_CaseInsensitive() throws Exception {
    List rows = new ArrayList();
    rows.add(createMap("id", "1", "desC", "one"));
    MockDataSource.setIterator("select * from x", rows.iterator());

    super.runFullImport(dataConfigWithCaseInsensitiveFields);

    assertQ(req("id:1"), "//*[@numFound='1']");
  }

   @Test
   @SuppressWarnings("unchecked")
   public void testDynamicFields() throws Exception {
@@ -144,4 +156,13 @@ public class TestDocBuilder2 extends AbstractDataImportHandlerTest {
           "        </entity>\n" +
           "    </document>\n" +
           "</dataConfig>";

  private final String dataConfigWithCaseInsensitiveFields = "<dataConfig>\n" +
          "    <document>\n" +
          "        <entity name=\"books\" query=\"select * from x\">\n" +
          "            <field column=\"ID\" />\n" +
          "            <field column=\"Desc\" />\n" +
          "        </entity>\n" +
          "    </document>\n" +
          "</dataConfig>";
 }
- 
2.19.1.windows.1

