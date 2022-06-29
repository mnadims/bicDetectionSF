From e858b20bee3119fb1e97077cfd76223e3107fc11 Mon Sep 17 00:00:00 2001
From: tbeerbower <tbeerbower@hortonworks.com>
Date: Sat, 20 Jun 2015 17:12:42 -0400
Subject: [PATCH] AMBARI-12048 - Views : Error deploying all non-system views
 (tbeerbower)

--
 .../server/orm/entities/ViewEntity.java       | 15 ++----
 .../server/view/ViewArchiveUtility.java       | 46 +++++++++----------
 .../ambari/server/view/ViewRegistry.java      | 13 +++---
 .../ambari/server/view/ViewRegistryTest.java  | 46 +++++++++----------
 4 files changed, 55 insertions(+), 65 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/ViewEntity.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/ViewEntity.java
index c7630edfdd..29dc2a75df 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/ViewEntity.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/ViewEntity.java
@@ -327,6 +327,11 @@ public class ViewEntity implements ViewDefinition {
     return statusDetail;
   }
 
  @Override
  public String getMask() {
    return mask;
  }

 
   // ----- ViewEntity --------------------------------------------------------
 
@@ -797,16 +802,6 @@ public class ViewEntity implements ViewDefinition {
     this.mask = mask;
   }
 
  /**
   * Get the mask class name.
   *
   * @return the mask class name.
   */
  @Override
  public String getMask() {
    return mask;
  }

   /**
    * Determine whether or not the view is a system view.
    *
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/view/ViewArchiveUtility.java b/ambari-server/src/main/java/org/apache/ambari/server/view/ViewArchiveUtility.java
index 04727562ea..d1ead32fe5 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/view/ViewArchiveUtility.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/view/ViewArchiveUtility.java
@@ -34,7 +34,6 @@ import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
import java.net.MalformedURLException;
 import java.net.URL;
 import java.net.URLClassLoader;
 import java.util.jar.JarInputStream;
@@ -64,26 +63,27 @@ public class ViewArchiveUtility {
    * @throws JAXBException if xml is malformed
    */
   public ViewConfig getViewConfigFromArchive(File archiveFile)
      throws MalformedURLException, JAXBException, IOException {
    ViewConfig res = null;
    InputStream configStream = null;
    try {
      throws JAXBException, IOException {
     ClassLoader cl = URLClassLoader.newInstance(new URL[]{archiveFile.toURI().toURL()});
 
    configStream = cl.getResourceAsStream(VIEW_XML);
    InputStream configStream = cl.getResourceAsStream(VIEW_XML);
     if (configStream == null) {
       configStream = cl.getResourceAsStream(WEB_INF_VIEW_XML);
      if (configStream == null) {
        throw new IllegalStateException(
            String.format("Archive %s doesn't contain a view descriptor.", archiveFile.getAbsolutePath()));
      }
     }
 
    JAXBContext jaxbContext       = JAXBContext.newInstance(ViewConfig.class);
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    res = (ViewConfig) jaxbUnmarshaller.unmarshal(configStream);
    try {

      JAXBContext jaxbContext       = JAXBContext.newInstance(ViewConfig.class);
      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

      return (ViewConfig) jaxbUnmarshaller.unmarshal(configStream);
     } finally {
      if (configStream != null) {
        configStream.close();
      }
      configStream.close();
     }
    return res;
   }
 
   /**
@@ -100,9 +100,6 @@ public class ViewArchiveUtility {
    */
   public ViewConfig getViewConfigFromExtractedArchive(String archivePath, boolean validate)
       throws JAXBException, IOException, SAXException {
    ViewConfig res = null;
    InputStream  configStream = null;
    try {
     File configFile = new File(archivePath + File.separator + VIEW_XML);
 
     if (!configFile.exists()) {
@@ -113,17 +110,16 @@ public class ViewArchiveUtility {
       validateConfig(new FileInputStream(configFile));
     }
 
    configStream     = new FileInputStream(configFile);
    JAXBContext  jaxbContext      = JAXBContext.newInstance(ViewConfig.class);
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    res = (ViewConfig) jaxbUnmarshaller.unmarshal(configStream);
    InputStream  configStream = new FileInputStream(configFile);
    try {

      JAXBContext  jaxbContext      = JAXBContext.newInstance(ViewConfig.class);
      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

      return (ViewConfig) jaxbUnmarshaller.unmarshal(configStream);
     } finally {
      if (configStream != null) {
        configStream.close();
      }
      configStream.close();
     }

    return res;
   }
 
   /**
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/view/ViewRegistry.java b/ambari-server/src/main/java/org/apache/ambari/server/view/ViewRegistry.java
index 28016eaef2..29b9000833 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/view/ViewRegistry.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/view/ViewRegistry.java
@@ -464,7 +464,7 @@ public class ViewRegistry {
    * Read all view archives.
    */
   public void readViewArchives() {
    readViewArchives(false, true, ALL_VIEWS_REG_EXP, true);
    readViewArchives(false, true, ALL_VIEWS_REG_EXP);
   }
 
   /**
@@ -473,7 +473,7 @@ public class ViewRegistry {
    * @param viewNameRegExp view name regular expression
    */
   public void readViewArchives(String viewNameRegExp) {
    readViewArchives(false, false, viewNameRegExp, false);
    readViewArchives(false, false, viewNameRegExp);
   }
 
   /**
@@ -1438,7 +1438,7 @@ public class ViewRegistry {
 
   // read the view archives.
   private void readViewArchives(boolean systemOnly, boolean useExecutor,
                                String viewNameRegExp, boolean removeUndeployed) {
                                String viewNameRegExp) {
     try {
       File viewDir = configuration.getViewsDir();
 
@@ -1535,17 +1535,16 @@ public class ViewRegistry {
     LOG.info("Reading view archive " + archiveFile + ".");
 
     try {
      // extract the archive and get the class loader
      ClassLoader cl = extractor.extractViewArchive(viewDefinition, archiveFile, extractedArchiveDirFile);
 
       ViewConfig viewConfig = archiveUtility.getViewConfigFromExtractedArchive(extractedArchiveDirPath,
           configuration.isViewValidationEnabled());
 
      if (viewConfig == null) {
        setViewStatus(viewDefinition, ViewEntity.ViewStatus.ERROR, "View configuration not found");
      } 
       viewDefinition.setConfiguration(viewConfig);
 
       if (checkViewVersions(viewDefinition, serverVersion)) {
        setupViewDefinition(viewDefinition, extractor.extractViewArchive(viewDefinition, archiveFile, extractedArchiveDirFile));
        setupViewDefinition(viewDefinition, cl);
 
         Set<ViewInstanceEntity> instanceDefinitions = new HashSet<ViewInstanceEntity>();
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/view/ViewRegistryTest.java b/ambari-server/src/test/java/org/apache/ambari/server/view/ViewRegistryTest.java
index 09df011ca3..4d2c8e2b1e 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/view/ViewRegistryTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/view/ViewRegistryTest.java
@@ -108,19 +108,19 @@ import org.springframework.security.core.GrantedAuthority;
  */
 public class ViewRegistryTest {
 
  private static final String view_xml1 = "<view>\n" +
  private static final String VIEW_XML_1 = "<view>\n" +
       "    <name>MY_VIEW</name>\n" +
       "    <label>My View!</label>\n" +
       "    <version>1.0.0</version>\n" +
       "</view>";
 
  private static final String view_xml2 = "<view>\n" +
  private static final String VIEW_XML_2 = "<view>\n" +
       "    <name>MY_VIEW</name>\n" +
       "    <label>My View!</label>\n" +
       "    <version>2.0.0</version>\n" +
       "</view>";
 
  private static final String xml_valid_instance = "<view>\n" +
  private static final String XML_VALID_INSTANCE = "<view>\n" +
       "    <name>MY_VIEW</name>\n" +
       "    <label>My View!</label>\n" +
       "    <version>1.0.0</version>\n" +
@@ -149,7 +149,7 @@ public class ViewRegistryTest {
       "    </instance>\n" +
       "</view>";
 
  private static final String xml_invalid_instance = "<view>\n" +
  private static final String XML_INVALID_INSTANCE = "<view>\n" +
       "    <name>MY_VIEW</name>\n" +
       "    <label>My View!</label>\n" +
       "    <version>1.0.0</version>\n" +
@@ -597,7 +597,7 @@ public class ViewRegistryTest {
     TestListener listener = new TestListener();
     registry.registerListener(listener, "MY_VIEW", "1.0.0");
 
    EventImpl event = EventImplTest.getEvent("MyEvent", Collections.<String, String>emptyMap(), view_xml1);
    EventImpl event = EventImplTest.getEvent("MyEvent", Collections.<String, String>emptyMap(), VIEW_XML_1);
 
     registry.fireEvent(event);
 
@@ -606,7 +606,7 @@ public class ViewRegistryTest {
     listener.clear();
 
     // fire an event for a different view
    event = EventImplTest.getEvent("MyEvent", Collections.<String, String>emptyMap(), view_xml2);
    event = EventImplTest.getEvent("MyEvent", Collections.<String, String>emptyMap(), VIEW_XML_2);
 
     registry.fireEvent(event);
 
@@ -615,7 +615,7 @@ public class ViewRegistryTest {
     // un-register the listener
     registry.unregisterListener(listener, "MY_VIEW", "1.0.0");
 
    event = EventImplTest.getEvent("MyEvent", Collections.<String, String>emptyMap(), view_xml1);
    event = EventImplTest.getEvent("MyEvent", Collections.<String, String>emptyMap(), VIEW_XML_1);
 
     registry.fireEvent(event);
 
@@ -629,7 +629,7 @@ public class ViewRegistryTest {
     TestListener listener = new TestListener();
     registry.registerListener(listener, "MY_VIEW", null); // all versions of MY_VIEW
 
    EventImpl event = EventImplTest.getEvent("MyEvent", Collections.<String, String>emptyMap(), view_xml1);
    EventImpl event = EventImplTest.getEvent("MyEvent", Collections.<String, String>emptyMap(), VIEW_XML_1);
 
     registry.fireEvent(event);
 
@@ -638,7 +638,7 @@ public class ViewRegistryTest {
     listener.clear();
 
     // fire an event for a different view
    event = EventImplTest.getEvent("MyEvent", Collections.<String, String>emptyMap(), view_xml2);
    event = EventImplTest.getEvent("MyEvent", Collections.<String, String>emptyMap(), VIEW_XML_2);
 
     registry.fireEvent(event);
 
@@ -649,13 +649,13 @@ public class ViewRegistryTest {
     // un-register the listener
     registry.unregisterListener(listener, "MY_VIEW", null); // all versions of MY_VIEW
 
    event = EventImplTest.getEvent("MyEvent", Collections.<String, String>emptyMap(), view_xml1);
    event = EventImplTest.getEvent("MyEvent", Collections.<String, String>emptyMap(), VIEW_XML_1);
 
     registry.fireEvent(event);
 
     Assert.assertNull(listener.getLastEvent());
 
    event = EventImplTest.getEvent("MyEvent", Collections.<String, String>emptyMap(), view_xml2);
    event = EventImplTest.getEvent("MyEvent", Collections.<String, String>emptyMap(), VIEW_XML_2);
 
     registry.fireEvent(event);
 
@@ -812,7 +812,7 @@ public class ViewRegistryTest {
 
     Configuration ambariConfig = new Configuration(properties);
 
    ViewConfig config = ViewConfigTest.getConfig(xml_valid_instance);
    ViewConfig config = ViewConfigTest.getConfig(XML_VALID_INSTANCE);
     ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
     ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));
     ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
@@ -850,7 +850,7 @@ public class ViewRegistryTest {
 
     Configuration ambariConfig = new Configuration(properties);
 
    ViewConfig config = ViewConfigTest.getConfig(xml_invalid_instance);
    ViewConfig config = ViewConfigTest.getConfig(XML_INVALID_INSTANCE);
     ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
     ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));
 
@@ -878,7 +878,7 @@ public class ViewRegistryTest {
     Validator validator = createNiceMock(Validator.class);
     ValidationResult result = createNiceMock(ValidationResult.class);
 
    ViewConfig config = ViewConfigTest.getConfig(xml_valid_instance);
    ViewConfig config = ViewConfigTest.getConfig(XML_VALID_INSTANCE);
     ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
     viewEntity.setValidator(validator);
 
@@ -920,7 +920,7 @@ public class ViewRegistryTest {
     Validator validator = createNiceMock(Validator.class);
     ValidationResult result = createNiceMock(ValidationResult.class);
 
    ViewConfig config = ViewConfigTest.getConfig(xml_valid_instance);
    ViewConfig config = ViewConfigTest.getConfig(XML_VALID_INSTANCE);
     ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
     viewEntity.setValidator(validator);
 
@@ -957,7 +957,7 @@ public class ViewRegistryTest {
 
     Configuration ambariConfig = new Configuration(properties);
 
    ViewConfig config = ViewConfigTest.getConfig(xml_valid_instance);
    ViewConfig config = ViewConfigTest.getConfig(XML_VALID_INSTANCE);
     ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
     ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));
     viewInstanceEntity.setViewName("BOGUS_VIEW");
@@ -984,7 +984,7 @@ public class ViewRegistryTest {
 
     Configuration ambariConfig = new Configuration(properties);
 
    ViewConfig config = ViewConfigTest.getConfig(xml_valid_instance);
    ViewConfig config = ViewConfigTest.getConfig(XML_VALID_INSTANCE);
     ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
     ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));
     ViewInstanceEntity updateInstance = getViewInstanceEntity(viewEntity, config.getInstances().get(0));
@@ -1030,7 +1030,7 @@ public class ViewRegistryTest {
 
     Configuration ambariConfig = new Configuration(properties);
 
    ViewConfig config = ViewConfigTest.getConfig(xml_valid_instance);
    ViewConfig config = ViewConfigTest.getConfig(XML_VALID_INSTANCE);
     ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
     ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));
 
@@ -1052,7 +1052,7 @@ public class ViewRegistryTest {
 
     Configuration ambariConfig = new Configuration(new Properties());
 
    ViewConfig config = ViewConfigTest.getConfig(xml_valid_instance);
    ViewConfig config = ViewConfigTest.getConfig(XML_VALID_INSTANCE);
     ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
     ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));
     ResourceEntity resource = new ResourceEntity();
@@ -1100,8 +1100,8 @@ public class ViewRegistryTest {
 
     Configuration ambariConfig = new Configuration(properties);
 
    ViewConfig config = ViewConfigTest.getConfig(xml_valid_instance);
    ViewConfig invalidConfig = ViewConfigTest.getConfig(xml_invalid_instance);
    ViewConfig config = ViewConfigTest.getConfig(XML_VALID_INSTANCE);
    ViewConfig invalidConfig = ViewConfigTest.getConfig(XML_INVALID_INSTANCE);
     ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
     ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));
     ViewInstanceEntity updateInstance = getViewInstanceEntity(viewEntity, invalidConfig.getInstances().get(0));
@@ -1134,7 +1134,7 @@ public class ViewRegistryTest {
     Validator validator = createNiceMock(Validator.class);
     ValidationResult result = createNiceMock(ValidationResult.class);
 
    ViewConfig config = ViewConfigTest.getConfig(xml_valid_instance);
    ViewConfig config = ViewConfigTest.getConfig(XML_VALID_INSTANCE);
     ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
     viewEntity.setValidator(validator);
     ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));
@@ -1177,7 +1177,7 @@ public class ViewRegistryTest {
     Validator validator = createNiceMock(Validator.class);
     ValidationResult result = createNiceMock(ValidationResult.class);
 
    ViewConfig config = ViewConfigTest.getConfig(xml_valid_instance);
    ViewConfig config = ViewConfigTest.getConfig(XML_VALID_INSTANCE);
     ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
     viewEntity.setValidator(validator);
     ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));
- 
2.19.1.windows.1

