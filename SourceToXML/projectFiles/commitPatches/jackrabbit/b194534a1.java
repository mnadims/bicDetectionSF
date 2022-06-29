From b194534a19f210933237df127bc9b8fb3315d787 Mon Sep 17 00:00:00 2001
From: Felix Meschberger <fmeschbe@apache.org>
Date: Fri, 30 Jun 2006 18:35:47 +0000
Subject: [PATCH] JCR-367 - Remove dependency on Xerces    Part 2: JAXP
 Transform instead of Xerces Serializer

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@418357 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/jackrabbit/core/SessionImpl.java   | 43 +++++++++++++------
 .../test/api/AbstractImportXmlTest.java       | 24 ++++++++---
 .../test/api/ExportDocViewTest.java           | 31 +++++++------
 3 files changed, 67 insertions(+), 31 deletions(-)

diff --git a/jackrabbit/src/main/java/org/apache/jackrabbit/core/SessionImpl.java b/jackrabbit/src/main/java/org/apache/jackrabbit/core/SessionImpl.java
index 3355bec5d..5f76afb3f 100644
-- a/jackrabbit/src/main/java/org/apache/jackrabbit/core/SessionImpl.java
++ b/jackrabbit/src/main/java/org/apache/jackrabbit/core/SessionImpl.java
@@ -45,8 +45,6 @@ import org.apache.jackrabbit.name.QName;
 import org.apache.jackrabbit.uuid.UUID;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
 import org.xml.sax.ContentHandler;
 import org.xml.sax.InputSource;
 import org.xml.sax.SAXException;
@@ -77,6 +75,14 @@ import javax.jcr.nodetype.ConstraintViolationException;
 import javax.jcr.nodetype.NoSuchNodeTypeException;
 import javax.jcr.version.VersionException;
 import javax.security.auth.Subject;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

 import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
@@ -1111,12 +1117,19 @@ public class SessionImpl implements Session, Dumpable {
     public void exportDocumentView(String absPath, OutputStream out,
                                    boolean skipBinary, boolean noRecurse)
             throws IOException, PathNotFoundException, RepositoryException {
        boolean indenting = false;
        OutputFormat format = new OutputFormat("xml", "UTF-8", indenting);
        XMLSerializer serializer = new XMLSerializer(out, format);

        SAXTransformerFactory stf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();

         try {
            exportDocumentView(absPath, serializer.asContentHandler(),
                    skipBinary, noRecurse);
            TransformerHandler th = stf.newTransformerHandler();
            th.setResult(new StreamResult(out));
            th.getTransformer().setParameter(OutputKeys.METHOD, "xml");
            th.getTransformer().setParameter(OutputKeys.ENCODING, "UTF-8");
            th.getTransformer().setParameter(OutputKeys.INDENT, "no");

            exportDocumentView(absPath, th, skipBinary, noRecurse);
        } catch (TransformerException te) {
            throw new RepositoryException(te);
         } catch (SAXException se) {
             throw new RepositoryException(se);
         }
@@ -1146,12 +1159,18 @@ public class SessionImpl implements Session, Dumpable {
     public void exportSystemView(String absPath, OutputStream out,
                                  boolean skipBinary, boolean noRecurse)
             throws IOException, PathNotFoundException, RepositoryException {
        boolean indenting = false;
        OutputFormat format = new OutputFormat("xml", "UTF-8", indenting);
        XMLSerializer serializer = new XMLSerializer(out, format);

        SAXTransformerFactory stf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
         try {
            exportSystemView(absPath, serializer.asContentHandler(),
                    skipBinary, noRecurse);
            TransformerHandler th = stf.newTransformerHandler();
            th.setResult(new StreamResult(out));
            th.getTransformer().setParameter(OutputKeys.METHOD, "xml");
            th.getTransformer().setParameter(OutputKeys.ENCODING, "UTF-8");
            th.getTransformer().setParameter(OutputKeys.INDENT, "no");

            exportSystemView(absPath, th, skipBinary, noRecurse);
        } catch (TransformerException te) {
            throw new RepositoryException(te);
         } catch (SAXException se) {
             throw new RepositoryException(se);
         }
diff --git a/jackrabbit/src/test/java/org/apache/jackrabbit/test/api/AbstractImportXmlTest.java b/jackrabbit/src/test/java/org/apache/jackrabbit/test/api/AbstractImportXmlTest.java
index c84b35d7c..04320c8a2 100644
-- a/jackrabbit/src/test/java/org/apache/jackrabbit/test/api/AbstractImportXmlTest.java
++ b/jackrabbit/src/test/java/org/apache/jackrabbit/test/api/AbstractImportXmlTest.java
@@ -17,8 +17,6 @@
 package org.apache.jackrabbit.test.api;
 
 import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 import org.w3c.dom.Attr;
@@ -39,6 +37,15 @@ import javax.jcr.RepositoryException;
 import javax.jcr.PathNotFoundException;
 import javax.xml.parsers.DocumentBuilder;
 import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

 import java.util.Set;
 import java.util.HashSet;
 import java.util.Arrays;
@@ -391,10 +398,15 @@ abstract class AbstractImportXmlTest extends AbstractJCRTest {
         BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
         try {
             // disable pretty printing/default line wrapping!
            boolean indenting = false;
            OutputFormat format = new OutputFormat("xml", "UTF-8", indenting);
            XMLSerializer serializer = new XMLSerializer(bos, format);
            serializer.serialize(document);
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setParameter(OutputKeys.METHOD, "xml");
            t.setParameter(OutputKeys.ENCODING, "UTF-8");
            t.setParameter(OutputKeys.INDENT, "no");
            Source s = new DOMSource(document);
            Result r = new StreamResult(bos);
            t.transform(s, r);
        } catch (TransformerException te) {
            throw (IOException) new IOException(te.getMessage()).initCause(te);
         } finally {
             bos.close();
         }
diff --git a/jackrabbit/src/test/java/org/apache/jackrabbit/test/api/ExportDocViewTest.java b/jackrabbit/src/test/java/org/apache/jackrabbit/test/api/ExportDocViewTest.java
index 32aa56f7c..4731ac56f 100644
-- a/jackrabbit/src/test/java/org/apache/jackrabbit/test/api/ExportDocViewTest.java
++ b/jackrabbit/src/test/java/org/apache/jackrabbit/test/api/ExportDocViewTest.java
@@ -17,7 +17,7 @@
 package org.apache.jackrabbit.test.api;
 
 import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.xerces.util.XMLChar;
import org.apache.jackrabbit.util.XMLChar;
 
 import org.xml.sax.SAXException;
 import org.xml.sax.ContentHandler;
@@ -26,6 +26,9 @@ import org.w3c.dom.Element;
 import org.w3c.dom.Attr;
 import org.w3c.dom.NamedNodeMap;
 
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
 import javax.xml.transform.stream.StreamSource;
 import javax.xml.transform.dom.DOMResult;
 import javax.xml.transform.TransformerException;
@@ -139,42 +142,42 @@ public class ExportDocViewTest extends AbstractJCRTest {
     }
 
     public void testExportDocView_handler_session_skipBinary_noRecurse()
            throws IOException, RepositoryException, SAXException {
            throws IOException, RepositoryException, SAXException, TransformerException {
         doTestExportDocView(CONTENTHANDLER, SKIPBINARY, NORECURSE);
     }
 
     public void testExportDocView_handler_session_skipBinary_recurse()
            throws IOException, RepositoryException, SAXException {
            throws IOException, RepositoryException, SAXException, TransformerException {
         doTestExportDocView(CONTENTHANDLER, SKIPBINARY, RECURSE);
     }
 
     public void testExportDocView_handler_session_saveBinary_noRecurse()
            throws IOException, RepositoryException, SAXException {
            throws IOException, RepositoryException, SAXException, TransformerException {
         doTestExportDocView(CONTENTHANDLER, SAVEBINARY, NORECURSE);
     }
 
     public void testExportDocView_handler_session_saveBinary_recurse()
            throws IOException, RepositoryException, SAXException {
            throws IOException, RepositoryException, SAXException, TransformerException {
         doTestExportDocView(CONTENTHANDLER, SAVEBINARY, RECURSE);
     }
 
     public void testExportDocView_stream_session_skipBinary_recurse()
            throws IOException, RepositoryException, SAXException {
            throws IOException, RepositoryException, SAXException, TransformerException {
         doTestExportDocView(STREAM, SKIPBINARY, RECURSE);
     }
 
     public void testExportDocView_stream_session_skipBinary_noRecurse()
            throws IOException, RepositoryException, SAXException {
            throws IOException, RepositoryException, SAXException, TransformerException {
         doTestExportDocView(STREAM, SKIPBINARY, NORECURSE);
     }
 
     public void testExportDocView_stream_session_saveBinary_noRecurse()
            throws IOException, RepositoryException, SAXException {
            throws IOException, RepositoryException, SAXException, TransformerException {
         doTestExportDocView(STREAM, SAVEBINARY, NORECURSE);
     }
 
     public void testExportDocView_stream_session_saveBinary_recurse()
            throws IOException, RepositoryException, SAXException {
            throws IOException, RepositoryException, SAXException, TransformerException {
         doTestExportDocView(STREAM, SAVEBINARY, RECURSE);
     }
 
@@ -194,7 +197,7 @@ public class ExportDocViewTest extends AbstractJCRTest {
      * @param noRecurse
      */
     public void doTestExportDocView(boolean withHandler, boolean skipBinary, boolean noRecurse)
            throws RepositoryException, IOException, SAXException {
            throws RepositoryException, IOException, SAXException, TransformerException {
 
         this.skipBinary = skipBinary;
         this.noRecurse = noRecurse;
@@ -202,9 +205,11 @@ public class ExportDocViewTest extends AbstractJCRTest {
         BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));
         try {
             if (withHandler) {
                ContentHandler handler =
                        new org.apache.xml.serialize.XMLSerializer(os, null).asContentHandler();
                session.exportDocumentView(testPath, handler, skipBinary, noRecurse);
                SAXTransformerFactory stf =
                    (SAXTransformerFactory) SAXTransformerFactory.newInstance();
                TransformerHandler th = stf.newTransformerHandler();
                th.setResult(new StreamResult(os));
                session.exportDocumentView(testPath, th, skipBinary, noRecurse);
             } else {
                 session.exportDocumentView(testPath, os, skipBinary, noRecurse);
             }
- 
2.19.1.windows.1

