From 5f0485ced554bad6e7da05da0dfb62deb59d6117 Mon Sep 17 00:00:00 2001
From: Julian Reschke <reschke@apache.org>
Date: Sun, 29 Apr 2007 14:14:54 +0000
Subject: [PATCH] JCR-892: fix setting of Transformer output properties.

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@533508 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/jackrabbit/jcr2spi/SessionImpl.java   | 12 ++++++------
 .../java/org/apache/jackrabbit/core/SessionImpl.java | 12 ++++++------
 .../apache/jackrabbit/commons/AbstractSession.java   |  6 +++---
 .../jackrabbit/test/api/AbstractImportXmlTest.java   |  6 +++---
 4 files changed, 18 insertions(+), 18 deletions(-)

diff --git a/contrib/spi/jcr2spi/src/main/java/org/apache/jackrabbit/jcr2spi/SessionImpl.java b/contrib/spi/jcr2spi/src/main/java/org/apache/jackrabbit/jcr2spi/SessionImpl.java
index 7185005bf..45e468cc8 100644
-- a/contrib/spi/jcr2spi/src/main/java/org/apache/jackrabbit/jcr2spi/SessionImpl.java
++ b/contrib/spi/jcr2spi/src/main/java/org/apache/jackrabbit/jcr2spi/SessionImpl.java
@@ -448,10 +448,10 @@ public class SessionImpl implements Session, ManagerProvider {
         SAXTransformerFactory stf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
         try {
             TransformerHandler th = stf.newTransformerHandler();
            th.getTransformer().setOutputProperty(OutputKeys.METHOD, "xml");
            th.getTransformer().setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            th.getTransformer().setOutputProperty(OutputKeys.INDENT, "no");
             th.setResult(new StreamResult(out));
            th.getTransformer().setParameter(OutputKeys.METHOD, "xml");
            th.getTransformer().setParameter(OutputKeys.ENCODING, "UTF-8");
            th.getTransformer().setParameter(OutputKeys.INDENT, "no");
 
             exportSystemView(absPath, th, skipBinary, noRecurse);
         } catch (TransformerException te) {
@@ -481,10 +481,10 @@ public class SessionImpl implements Session, ManagerProvider {
         SAXTransformerFactory stf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
         try {
             TransformerHandler th = stf.newTransformerHandler();
            th.getTransformer().setOutputProperty(OutputKeys.METHOD, "xml");
            th.getTransformer().setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            th.getTransformer().setOutputProperty(OutputKeys.INDENT, "no");
             th.setResult(new StreamResult(out));
            th.getTransformer().setParameter(OutputKeys.METHOD, "xml");
            th.getTransformer().setParameter(OutputKeys.ENCODING, "UTF-8");
            th.getTransformer().setParameter(OutputKeys.INDENT, "no");
 
             exportDocumentView(absPath, th, skipBinary, noRecurse);
         } catch (TransformerException te) {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/SessionImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/SessionImpl.java
index 0fa35105c..9cc67463b 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/SessionImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/SessionImpl.java
@@ -1158,10 +1158,10 @@ public class SessionImpl implements Session, NamePathResolver, Dumpable {
 
         try {
             TransformerHandler th = stf.newTransformerHandler();
            th.getTransformer().setOutputProperty(OutputKeys.METHOD, "xml");
            th.getTransformer().setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            th.getTransformer().setOutputProperty(OutputKeys.INDENT, "no");
             th.setResult(new StreamResult(out));
            th.getTransformer().setParameter(OutputKeys.METHOD, "xml");
            th.getTransformer().setParameter(OutputKeys.ENCODING, "UTF-8");
            th.getTransformer().setParameter(OutputKeys.INDENT, "no");
 
             exportDocumentView(absPath, th, skipBinary, noRecurse);
         } catch (TransformerException te) {
@@ -1199,10 +1199,10 @@ public class SessionImpl implements Session, NamePathResolver, Dumpable {
         SAXTransformerFactory stf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
         try {
             TransformerHandler th = stf.newTransformerHandler();
            th.getTransformer().setOutputProperty(OutputKeys.METHOD, "xml");
            th.getTransformer().setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            th.getTransformer().setOutputProperty(OutputKeys.INDENT, "no");
             th.setResult(new StreamResult(out));
            th.getTransformer().setParameter(OutputKeys.METHOD, "xml");
            th.getTransformer().setParameter(OutputKeys.ENCODING, "UTF-8");
            th.getTransformer().setParameter(OutputKeys.INDENT, "no");
 
             exportSystemView(absPath, th, skipBinary, noRecurse);
         } catch (TransformerException te) {
diff --git a/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/AbstractSession.java b/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/AbstractSession.java
index b9b6d7ece..def03ce8c 100644
-- a/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/AbstractSession.java
++ b/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/AbstractSession.java
@@ -225,9 +225,9 @@ public abstract class AbstractSession implements Session {
             TransformerHandler handler = stf.newTransformerHandler();
 
             Transformer transformer = handler.getTransformer();
            transformer.setParameter(OutputKeys.METHOD, "xml");
            transformer.setParameter(OutputKeys.ENCODING, "UTF-8");
            transformer.setParameter(OutputKeys.INDENT, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
 
             handler.setResult(new StreamResult(stream));
             return handler;
diff --git a/jackrabbit-jcr-tests/src/main/java/org/apache/jackrabbit/test/api/AbstractImportXmlTest.java b/jackrabbit-jcr-tests/src/main/java/org/apache/jackrabbit/test/api/AbstractImportXmlTest.java
index 82acb561e..de7834f1c 100644
-- a/jackrabbit-jcr-tests/src/main/java/org/apache/jackrabbit/test/api/AbstractImportXmlTest.java
++ b/jackrabbit-jcr-tests/src/main/java/org/apache/jackrabbit/test/api/AbstractImportXmlTest.java
@@ -417,9 +417,9 @@ abstract class AbstractImportXmlTest extends AbstractJCRTest {
         try {
             // disable pretty printing/default line wrapping!
             Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setParameter(OutputKeys.METHOD, "xml");
            t.setParameter(OutputKeys.ENCODING, "UTF-8");
            t.setParameter(OutputKeys.INDENT, "no");
            t.setOutputProperty(OutputKeys.METHOD, "xml");
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            t.setOutputProperty(OutputKeys.INDENT, "no");
             Source s = new DOMSource(document);
             Result r = new StreamResult(bos);
             t.transform(s, r);
- 
2.19.1.windows.1

