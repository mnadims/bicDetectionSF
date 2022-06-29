From d33e5ce118497eff8b1bdd9ad5edc8ee866bd7ba Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Mon, 3 Jul 2006 11:57:40 +0000
Subject: [PATCH] JCR-367: Explicit xmlns:prefix="namespace" attributes in XML
 exports.

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@418750 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/xml/AbstractSAXEventGenerator.java   | 29 +++++++++++++++++++
 .../core/xml/DocViewSAXEventGenerator.java    |  2 ++
 .../core/xml/SysViewSAXEventGenerator.java    |  1 +
 3 files changed, 32 insertions(+)

diff --git a/jackrabbit/src/main/java/org/apache/jackrabbit/core/xml/AbstractSAXEventGenerator.java b/jackrabbit/src/main/java/org/apache/jackrabbit/core/xml/AbstractSAXEventGenerator.java
index 99faa1626..c23b95382 100644
-- a/jackrabbit/src/main/java/org/apache/jackrabbit/core/xml/AbstractSAXEventGenerator.java
++ b/jackrabbit/src/main/java/org/apache/jackrabbit/core/xml/AbstractSAXEventGenerator.java
@@ -24,6 +24,7 @@ import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.xml.sax.ContentHandler;
 import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
 
 import javax.jcr.Node;
 import javax.jcr.NodeIterator;
@@ -192,6 +193,34 @@ abstract class AbstractSAXEventGenerator {
         }
     }
 
    /**
     * Adds explicit <code>xmlns:prefix="uri"</code> attributes to the
     * XML top-level element. The effect is the same as setting the
     * "<code>http://xml.org/sax/features/namespace-prefixes</code>"
     * property on an SAX parser.
     *
     * @param level level of the current XML element
     * @param attributes attributes of the current XML element
     * @throws RepositoryException on a repository error
     */
    protected void addNamespacePrefixes(int level, AttributesImpl attributes)
            throws RepositoryException {
        if (level == 0) {
            String[] prefixes = session.getNamespacePrefixes();
            for (int i = 0; i < prefixes.length; i++) {
                if (prefixes[i].length() > 0
                        && !QName.NS_XML_PREFIX.equals(prefixes[i])) {
                    attributes.addAttribute(
                            QName.NS_XMLNS_URI,
                            prefixes[i],
                            QName.NS_XMLNS_PREFIX + ":" + prefixes[i],
                            "CDATA",
                            session.getNamespaceURI(prefixes[i]));
                }
            }
        }
    }

     /**
      * @param node
      * @param level
diff --git a/jackrabbit/src/main/java/org/apache/jackrabbit/core/xml/DocViewSAXEventGenerator.java b/jackrabbit/src/main/java/org/apache/jackrabbit/core/xml/DocViewSAXEventGenerator.java
index 8dfddaac3..6aad8531f 100644
-- a/jackrabbit/src/main/java/org/apache/jackrabbit/core/xml/DocViewSAXEventGenerator.java
++ b/jackrabbit/src/main/java/org/apache/jackrabbit/core/xml/DocViewSAXEventGenerator.java
@@ -132,6 +132,7 @@ public class DocViewSAXEventGenerator extends AbstractSAXEventGenerator {
 
             // attributes (properties)
             AttributesImpl attrs = new AttributesImpl();
            addNamespacePrefixes(level, attrs);
             Iterator iter = props.iterator();
             while (iter.hasNext()) {
                 Property prop = (Property) iter.next();
@@ -168,6 +169,7 @@ public class DocViewSAXEventGenerator extends AbstractSAXEventGenerator {
                             attrValue.toString());
                 }
             }

             // start element (node)
             QName qName = getQName(elemName);
             contentHandler.startElement(qName.getNamespaceURI(),
diff --git a/jackrabbit/src/main/java/org/apache/jackrabbit/core/xml/SysViewSAXEventGenerator.java b/jackrabbit/src/main/java/org/apache/jackrabbit/core/xml/SysViewSAXEventGenerator.java
index d05f28447..a8347b11a 100644
-- a/jackrabbit/src/main/java/org/apache/jackrabbit/core/xml/SysViewSAXEventGenerator.java
++ b/jackrabbit/src/main/java/org/apache/jackrabbit/core/xml/SysViewSAXEventGenerator.java
@@ -86,6 +86,7 @@ public class SysViewSAXEventGenerator extends AbstractSAXEventGenerator {
     protected void entering(Node node, int level)
             throws RepositoryException, SAXException {
         AttributesImpl attrs = new AttributesImpl();
        addNamespacePrefixes(level, attrs);
         // name attribute
         String nodeName;
         if (node.getDepth() == 0) {
- 
2.19.1.windows.1

