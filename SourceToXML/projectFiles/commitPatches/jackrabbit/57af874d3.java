From 57af874d3617dd73a4de96ce3151117a8c6b7c54 Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Thu, 23 Sep 2010 11:50:27 +0000
Subject: [PATCH] JCR-2750: MultiStatusResponse should not call
 resource.getProperties

Fixed as suggested by Stepan Koltsov.

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1000414 13f79535-47bb-0310-9956-ffa450edef68
--
 .../webdav/MultiStatusResponse.java           | 67 ++++++++++++-------
 .../webdav/property/DavPropertyNameSet.java   |  5 +-
 .../webdav/property/DavPropertySet.java       |  3 +-
 3 files changed, 48 insertions(+), 27 deletions(-)

diff --git a/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/MultiStatusResponse.java b/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/MultiStatusResponse.java
index 9dcf71645..ea8cec778 100644
-- a/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/MultiStatusResponse.java
++ b/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/MultiStatusResponse.java
@@ -31,7 +31,9 @@ import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 
 import java.util.HashMap;
import java.util.HashSet;
 import java.util.Iterator;
import java.util.Set;
 
 /**
  * <code>MultiStatusResponse</code> represents the DAV:multistatus element defined
@@ -145,7 +147,7 @@ public class MultiStatusResponse implements XmlSerializable, DavConstants {
     }
 
     /**
     * Constucts a WebDAV multistatus response and retrieves the resource
     * Constructs a WebDAV multistatus response and retrieves the resource
      * properties according to the given <code>DavPropertyNameSet</code>. It
      * adds all known property to the '200' set, while unknown properties are
      * added to the '404' set.
@@ -160,40 +162,57 @@ public class MultiStatusResponse implements XmlSerializable, DavConstants {
      * #PROPFIND_ALL_PROP}, {@link #PROPFIND_BY_PROPERTY}, {@link
      * #PROPFIND_PROPERTY_NAMES}, {@link #PROPFIND_ALL_PROP_INCLUDE}
      */
    public MultiStatusResponse(DavResource resource, DavPropertyNameSet propNameSet,
                               int propFindType) {
    public MultiStatusResponse(
            DavResource resource, DavPropertyNameSet propNameSet,
            int propFindType) {
         this(resource.getHref(), null, TYPE_PROPSTAT);
 
         // only property names requested
         if (propFindType == PROPFIND_PROPERTY_NAMES) {
            PropContainer status200 = getPropContainer(DavServletResponse.SC_OK, true);
            PropContainer status200 =
                getPropContainer(DavServletResponse.SC_OK, true);
             for (DavPropertyName propName : resource.getPropertyNames()) {
                 status200.addContent(propName);
             }
            // all or a specified set of property and their values requested.
        // all or a specified set of property and their values requested.
         } else {
            PropContainer status200 = getPropContainer(DavServletResponse.SC_OK, false);
            // clone set of property, since several resources could use this again
            propNameSet = new DavPropertyNameSet(propNameSet);
            // Add requested properties or all non-protected properties, or 
            // non-protected properties plus requested properties (allprop/include) 
            DavPropertyIterator iter = resource.getProperties().iterator();
            while (iter.hasNext()) {
                DavProperty<?> property = iter.nextProperty();
                boolean allDeadPlusRfc4918LiveProperties =
                    propFindType == PROPFIND_ALL_PROP || propFindType == PROPFIND_ALL_PROP_INCLUDE;
                boolean wasRequested = propNameSet.remove(property.getName());
                
                if ((allDeadPlusRfc4918LiveProperties && !property.isInvisibleInAllprop()) || wasRequested) {
                    status200.addContent(property);
            PropContainer status200 =
                getPropContainer(DavServletResponse.SC_OK, false);

            // Collection of missing property names for 404 responses
            Set<DavPropertyName> missing =
                new HashSet<DavPropertyName>(propNameSet.getContent());

            // Add requested properties or all non-protected properties,
            // or non-protected properties plus requested properties
            // (allprop/include) 
            if (propFindType == PROPFIND_BY_PROPERTY) {
                for (DavPropertyName propName : propNameSet) {
                    DavProperty<?> prop = resource.getProperty(propName);
                    if (prop != null) {
                        status200.addContent(prop);
                        missing.remove(propName);
                    }
                }
            } else {
                for (DavProperty<?> property : resource.getProperties()) {
                    boolean allDeadPlusRfc4918LiveProperties =
                        propFindType == PROPFIND_ALL_PROP
                        || propFindType == PROPFIND_ALL_PROP_INCLUDE;
                    boolean wasRequested = missing.remove(property.getName());

                    if ((allDeadPlusRfc4918LiveProperties
                            && !property.isInvisibleInAllprop())
                            || wasRequested) {
                        status200.addContent(property);
                    }
                 }
             }
 
            if (!propNameSet.isEmpty() && propFindType != PROPFIND_ALL_PROP) {
                PropContainer status404 = getPropContainer(DavServletResponse.SC_NOT_FOUND, true);
                DavPropertyNameIterator iter1 = propNameSet.iterator();
                while (iter1.hasNext()) {
                    DavPropertyName propName = iter1.nextPropertyName();
            if (!missing.isEmpty() && propFindType != PROPFIND_ALL_PROP) {
                PropContainer status404 =
                    getPropContainer(DavServletResponse.SC_NOT_FOUND, true);
                for (DavPropertyName propName : missing) {
                     status404.addContent(propName);
                 }
             }
diff --git a/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/property/DavPropertyNameSet.java b/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/property/DavPropertyNameSet.java
index 3528319b1..60aaaae03 100644
-- a/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/property/DavPropertyNameSet.java
++ b/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/property/DavPropertyNameSet.java
@@ -31,7 +31,8 @@ import java.util.Set;
  * <code>DavPropertyNameSet</code> represents a Set of {@link DavPropertyName}
  * objects.
  */
public class DavPropertyNameSet extends PropContainer {
public class DavPropertyNameSet extends PropContainer
        implements Iterable<DavPropertyName> {
 
     private static Logger log = LoggerFactory.getLogger(DavPropertyNameSet.class);
     private final Set<DavPropertyName> set = new HashSet<DavPropertyName>();
@@ -157,7 +158,7 @@ public class DavPropertyNameSet extends PropContainer {
      * @see PropContainer#getContent()
      */
     @Override
    public Collection<? extends PropEntry> getContent() {
    public Collection<DavPropertyName> getContent() {
         return set;
     }
 
diff --git a/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/property/DavPropertySet.java b/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/property/DavPropertySet.java
index bc56aed60..2e91904ca 100644
-- a/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/property/DavPropertySet.java
++ b/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/property/DavPropertySet.java
@@ -30,7 +30,8 @@ import java.util.NoSuchElementException;
  * The <code>DavPropertySet</code> class represents a set of WebDAV
  * property.
  */
public class DavPropertySet extends PropContainer {
public class DavPropertySet extends PropContainer
        implements Iterable<DavProperty<?>> {
 
     private static Logger log = LoggerFactory.getLogger(DavPropertySet.class);
 
- 
2.19.1.windows.1

