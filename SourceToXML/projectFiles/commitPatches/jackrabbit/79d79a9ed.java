From 79d79a9ed6c1a9887a761ecdbf1c0e4b7b6c9fd6 Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Thu, 3 Sep 2009 15:44:20 +0000
Subject: [PATCH] JCR-2170: Remove PropDefId and NodeDefId

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@811001 13f79535-47bb-0310-9956-ffa450edef68
--
 .../java/org/apache/jackrabbit/core/PropertyImpl.java | 11 +++++------
 .../apache/jackrabbit/core/data/GarbageCollector.java |  2 +-
 .../jackrabbit/core/query/lucene/RowIteratorImpl.java |  2 +-
 .../query/lucene/constraint/PropertyValueOperand.java |  2 +-
 .../core/security/user/AuthorizableImpl.java          |  4 ++--
 .../core/security/user/TraversingNodeResolver.java    |  2 +-
 .../apache/jackrabbit/core/version/NodeStateEx.java   |  2 +-
 .../apache/jackrabbit/core/xml/SessionImporter.java   |  2 +-
 .../jackrabbit/core/ConcurrentReadWriteTest.java      |  2 +-
 .../apache/jackrabbit/core/xml/DocumentViewTest.java  |  2 +-
 .../org/apache/jackrabbit/commons/xml/Exporter.java   |  4 ++--
 .../jackrabbit/server/remoting/davex/JsonWriter.java  |  4 ++--
 .../jackrabbit/webdav/jcr/DefaultItemResource.java    |  2 +-
 .../apache/jackrabbit/jcr2spi/PropertyLengthTest.java |  2 +-
 .../apache/jackrabbit/spi2jcr/PropertyInfoImpl.java   |  4 ++--
 15 files changed, 23 insertions(+), 24 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/PropertyImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/PropertyImpl.java
index 706b2ec94..b80130ddf 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/PropertyImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/PropertyImpl.java
@@ -228,12 +228,11 @@ public class PropertyImpl extends ItemImpl implements Property {
             LockException, ConstraintViolationException,
             RepositoryException {
         NodeImpl parent = (NodeImpl) getParent();
        PropertyDefinition definition = data.getPropertyDefinition();
         // check multi-value flag
        if (multipleValues != definition.isMultiple()) {
        if (multipleValues != isMultiple()) {
             String msg = (multipleValues) ?
                     "Single-valued property can not be set to an array of values:" :
                    "Multivalued property can not be set to a single value (an array of lenght one is OK): ";
                    "Multivalued property can not be set to a single value (an array of length one is OK): ";
             throw new ValueFormatException(msg + this);
         }
 
@@ -416,7 +415,7 @@ public class PropertyImpl extends ItemImpl implements Property {
      */
     public InternalValue[] internalGetValues() throws RepositoryException {
         final PropertyDefinition definition = data.getPropertyDefinition();
        if (definition.isMultiple()) {
        if (isMultiple()) {
             return getPropertyState().getValues();
         } else {
             throw new ValueFormatException(
@@ -435,7 +434,7 @@ public class PropertyImpl extends ItemImpl implements Property {
      */
     public InternalValue internalGetValue() throws RepositoryException {
         final PropertyDefinition definition = data.getPropertyDefinition();
        if (definition.isMultiple()) {
        if (isMultiple()) {
             throw new ValueFormatException(
                     this + " is a multi-valued property,"
                     + " so it's values can only be retrieved as an array");
@@ -789,7 +788,7 @@ public class PropertyImpl extends ItemImpl implements Property {
         // check state of this instance
         sanityCheck();
 
        return data.getPropertyDefinition().isMultiple();
        return getPropertyState().isMultiValued();
     }
 
     //-----------------------------------------------------------------< Item >
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/data/GarbageCollector.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/data/GarbageCollector.java
index 964936f5a..77a762c23 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/data/GarbageCollector.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/data/GarbageCollector.java
@@ -321,7 +321,7 @@ public class GarbageCollector {
                         } else {
                             rememberNode(n.getPath());
                         }
                        if (p.getDefinition().isMultiple()) {
                        if (p.isMultiple()) {
                             p.getLengths();
                         } else {
                             p.getLength();
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/RowIteratorImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/RowIteratorImpl.java
index 9ebd0e00f..7676712e5 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/RowIteratorImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/RowIteratorImpl.java
@@ -341,7 +341,7 @@ class RowIteratorImpl implements RowIterator {
                     return valueFactory.createValue(p);
                 } else if (n.hasProperty(col.getPropertyName())) {
                     Property p = n.getProperty(col.getPropertyName());
                    if (p.getDefinition().isMultiple()) {
                    if (p.isMultiple()) {
                         // mvp values cannot be returned
                         return null;
                     } else {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/constraint/PropertyValueOperand.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/constraint/PropertyValueOperand.java
index 6bbaade79..4dfc45821 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/constraint/PropertyValueOperand.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/constraint/PropertyValueOperand.java
@@ -104,7 +104,7 @@ public class PropertyValueOperand extends DynamicOperand {
         if (prop == null) {
             return EMPTY;
         } else {
            if (prop.getDefinition().isMultiple()) {
            if (prop.isMultiple()) {
                 return prop.getValues();
             } else {
                 return new Value[]{prop.getValue()};
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/security/user/AuthorizableImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/security/user/AuthorizableImpl.java
index d3265190f..dd0886d60 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/security/user/AuthorizableImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/security/user/AuthorizableImpl.java
@@ -185,7 +185,7 @@ abstract class AuthorizableImpl implements Authorizable, UserConstants {
         if (hasProperty(name)) {
             Property prop = node.getProperty(name);
             if (isAuthorizableProperty(prop)) {
                if (prop.getDefinition().isMultiple()) {
                if (prop.isMultiple()) {
                     return prop.getValues();
                 } else {
                     return new Value[] {prop.getValue()};
@@ -249,7 +249,7 @@ abstract class AuthorizableImpl implements Authorizable, UserConstants {
             if (node.hasProperty(name)) {
                 // 'node' is protected -> use setValue instead of Property.remove()
                 Property p = node.getProperty(name);
                if (p.getDefinition().isMultiple()) {
                if (p.isMultiple()) {
                     p.setValue((Value[]) null);
                 } else {
                     p.setValue((Value) null);
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/security/user/TraversingNodeResolver.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/security/user/TraversingNodeResolver.java
index 32ea7a7fb..1cc9181e4 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/security/user/TraversingNodeResolver.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/security/user/TraversingNodeResolver.java
@@ -222,7 +222,7 @@ class TraversingNodeResolver extends NodeResolver {
                             Name propertyName = pItr.next();
                             if (node.hasProperty(propertyName)) {
                                 Property prop = node.getProperty(propertyName);
                                if (prop.getDefinition().isMultiple()) {
                                if (prop.isMultiple()) {
                                     Value[] values = prop.getValues();
                                     for (int i = 0; i < values.length && !match; i++) {
                                         match = matches(value, values[i].getString(), exact);
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/NodeStateEx.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/NodeStateEx.java
index cfe36d849..ac6701216 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/NodeStateEx.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/NodeStateEx.java
@@ -795,7 +795,7 @@ public class NodeStateEx {
      * @throws RepositoryException if an error occurs
      */
     public void copyFrom(PropertyImpl prop) throws RepositoryException {
        if (prop.getDefinition().isMultiple()) {
        if (prop.isMultiple()) {
             InternalValue[] values = prop.internalGetValues();
             InternalValue[] copiedValues = new InternalValue[values.length];
             for (int i = 0; i < values.length; i++) {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/SessionImporter.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/SessionImporter.java
index d9816024f..ec069508c 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/SessionImporter.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/SessionImporter.java
@@ -418,7 +418,7 @@ public class SessionImporter implements Importer {
                     && prop.getType() != PropertyType.WEAKREFERENCE) {
                 continue;
             }
            if (prop.getDefinition().isMultiple()) {
            if (prop.isMultiple()) {
                 Value[] values = prop.getValues();
                 Value[] newVals = new Value[values.length];
                 for (int i = 0; i < values.length; i++) {
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/ConcurrentReadWriteTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/ConcurrentReadWriteTest.java
index b021d5e6b..17db6ef7f 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/ConcurrentReadWriteTest.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/ConcurrentReadWriteTest.java
@@ -64,7 +64,7 @@ public class ConcurrentReadWriteTest extends AbstractConcurrencyTest {
                                 try {
                                     for (PropertyIterator it = n.getProperties(); it.hasNext(); ) {
                                         Property p = it.nextProperty();
                                        if (p.getDefinition().isMultiple()) {
                                        if (p.isMultiple()) {
                                             p.getValues();
                                         } else {
                                             p.getValue();
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/xml/DocumentViewTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/xml/DocumentViewTest.java
index f68d24eaf..5aa5aa72f 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/xml/DocumentViewTest.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/xml/DocumentViewTest.java
@@ -114,7 +114,7 @@ public class DocumentViewTest extends AbstractJCRTest {
                 ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
         try {
             Property property = root.getProperty("multi-value-test/test");
            assertTrue(message, property.getDefinition().isMultiple());
            assertTrue(message, property.isMultiple());
             assertEquals(message, property.getValues().length, 2);
             assertTrue(message, property.getValues()[0].getBoolean());
             assertFalse(message, property.getValues()[1].getBoolean());
diff --git a/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/xml/Exporter.java b/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/xml/Exporter.java
index 8544b9f8f..508590e09 100644
-- a/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/xml/Exporter.java
++ b/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/xml/Exporter.java
@@ -357,7 +357,7 @@ public abstract class Exporter {
 
         int type = property.getType();
         if (type != PropertyType.BINARY || binary) {
            if (property.getDefinition().isMultiple()) {
            if (property.isMultiple()) {
                 exportProperty(uri, local, type, property.getValues());
             } else {
                 exportProperty(uri, local, property.getValue());
@@ -365,7 +365,7 @@ public abstract class Exporter {
         } else {
             ValueFactory factory = session.getValueFactory();
             Value value = factory.createValue("", PropertyType.BINARY);
            if (property.getDefinition().isMultiple()) {
            if (property.isMultiple()) {
                 exportProperty(uri, local, type, new Value[] { value });
             } else {
                 exportProperty(uri, local, value);
diff --git a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/remoting/davex/JsonWriter.java b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/remoting/davex/JsonWriter.java
index 6e675056b..3c0a1b96c 100644
-- a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/remoting/davex/JsonWriter.java
++ b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/remoting/davex/JsonWriter.java
@@ -162,14 +162,14 @@ class JsonWriter {
             // mark binary properties with a leading ':'
             // the value(s) reflect the jcr-values length instead of the binary data.
             String key = ":" + p.getName();
            if (p.getDefinition().isMultiple()) {
            if (p.isMultiple()) {
                 long[] binLengths = p.getLengths();
                 writeKeyArray(w, key, binLengths);
             } else {
                 writeKeyValue(w, key, p.getLength());
             }
         } else {
            boolean isMultiple = p.getDefinition().isMultiple();
            boolean isMultiple = p.isMultiple();
             if (type == PropertyType.NAME || type == PropertyType.PATH ||
                     type == PropertyType.REFERENCE || type == PropertyType.DATE ||
                     (isMultiple && p.getValues().length == 0)) {
diff --git a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/webdav/jcr/DefaultItemResource.java b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/webdav/jcr/DefaultItemResource.java
index 485fc4abd..49191560d 100644
-- a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/webdav/jcr/DefaultItemResource.java
++ b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/webdav/jcr/DefaultItemResource.java
@@ -335,7 +335,7 @@ public class DefaultItemResource extends AbstractItemResource {
      */
     private boolean isMultiple() {
         try {
            if (exists() && ((Property)item).getDefinition().isMultiple()) {
            if (exists() && ((Property)item).isMultiple()) {
                 return true;
             }
         } catch (RepositoryException e) {
diff --git a/jackrabbit-jcr2spi/src/test/java/org/apache/jackrabbit/jcr2spi/PropertyLengthTest.java b/jackrabbit-jcr2spi/src/test/java/org/apache/jackrabbit/jcr2spi/PropertyLengthTest.java
index 15f970604..0776b5326 100644
-- a/jackrabbit-jcr2spi/src/test/java/org/apache/jackrabbit/jcr2spi/PropertyLengthTest.java
++ b/jackrabbit-jcr2spi/src/test/java/org/apache/jackrabbit/jcr2spi/PropertyLengthTest.java
@@ -99,7 +99,7 @@ public class PropertyLengthTest extends AbstractJCRTest {
     }
 
     private static void checkLength(Property p) throws RepositoryException {
        if (p.getDefinition().isMultiple()) {
        if (p.isMultiple()) {
             Value[] vals = p.getValues();
             long[] lengths = p.getLengths();
             for (int i = 0; i < lengths.length; i++) {
diff --git a/jackrabbit-spi2jcr/src/main/java/org/apache/jackrabbit/spi2jcr/PropertyInfoImpl.java b/jackrabbit-spi2jcr/src/main/java/org/apache/jackrabbit/spi2jcr/PropertyInfoImpl.java
index f89e2b0cf..29962319e 100644
-- a/jackrabbit-spi2jcr/src/main/java/org/apache/jackrabbit/spi2jcr/PropertyInfoImpl.java
++ b/jackrabbit-spi2jcr/src/main/java/org/apache/jackrabbit/spi2jcr/PropertyInfoImpl.java
@@ -50,7 +50,7 @@ class PropertyInfoImpl
             throws RepositoryException, NameException {
         super(resolver.getQPath(property.getPath()),
                 idFactory.createPropertyId(property, resolver),
                property.getType(), property.getDefinition().isMultiple(),
                property.getType(), property.isMultiple(),
                 getValues(property, resolver, qValueFactory));
     }
 
@@ -67,7 +67,7 @@ class PropertyInfoImpl
                                       NamePathResolver resolver,
                                       QValueFactory factory)
             throws RepositoryException {
        boolean isMultiValued = property.getDefinition().isMultiple();
        boolean isMultiValued = property.isMultiple();
         QValue[] values;
         if (isMultiValued) {
             Value[] jcrValues = property.getValues();
- 
2.19.1.windows.1

