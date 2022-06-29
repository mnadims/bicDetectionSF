From 76213e083ee1818cacb64b612656d48af5e956e3 Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Wed, 28 Oct 2009 16:50:36 +0000
Subject: [PATCH] JCR-2334: Tika-based type detection in jcr-server

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@830670 13f79535-47bb-0310-9956-ffa450edef68
--
 jackrabbit-jcr-server/pom.xml                 |   5 +
 .../server/io/AbstractExportContext.java      |  15 +--
 .../jackrabbit/server/io/DefaultHandler.java  |  53 ++++++--
 .../server/io/DefaultIOManager.java           |   1 +
 .../jackrabbit/server/io/ExportContext.java   |   8 +-
 .../server/io/ExportContextImpl.java          |  11 +-
 .../jackrabbit/server/io/IOManager.java       |  15 +++
 .../jackrabbit/server/io/IOManagerImpl.java   |  34 +++++-
 .../apache/jackrabbit/server/io/IOUtil.java   |   6 -
 .../jackrabbit/server/io/ImportContext.java   |  10 +-
 .../server/io/ImportContextImpl.java          | 114 +++++-------------
 .../jackrabbit/server/io/MimeResolver.java    | 103 ----------------
 .../jackrabbit/server/io/XmlHandler.java      |   2 +-
 .../jackrabbit/server/io/ZipHandler.java      |   7 +-
 .../webdav/simple/DavResourceImpl.java        |   7 +-
 .../webdav/simple/ResourceConfig.java         |  75 ++++++------
 .../webdav/simple/ResourceFactoryImpl.java    |  13 +-
 .../webdav/simple/SimpleWebdavServlet.java    |  53 +++++++-
 18 files changed, 236 insertions(+), 296 deletions(-)

diff --git a/jackrabbit-jcr-server/pom.xml b/jackrabbit-jcr-server/pom.xml
index c7c3c3303..210b46900 100644
-- a/jackrabbit-jcr-server/pom.xml
++ b/jackrabbit-jcr-server/pom.xml
@@ -77,6 +77,11 @@
       <groupId>org.apache.tika</groupId>
       <artifactId>tika-core</artifactId>
     </dependency>
    <dependency>
      <groupId>org.apache.tika</groupId>
      <artifactId>tika-core</artifactId>
      <version>0.4</version>
    </dependency>
     <dependency>
       <groupId>org.slf4j</groupId>
       <artifactId>slf4j-api</artifactId>
diff --git a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/AbstractExportContext.java b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/AbstractExportContext.java
index ad8b64dfd..461dbae25 100644
-- a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/AbstractExportContext.java
++ b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/AbstractExportContext.java
@@ -32,21 +32,14 @@ public abstract class AbstractExportContext implements ExportContext {
     private final IOListener ioListener;
     private final Item exportRoot;
     private final boolean hasStream;
    private final MimeResolver mimeResolver;
 
     protected boolean completed;
 
    public AbstractExportContext(Item exportRoot, boolean hasStream,
                                 IOListener ioListener) {
        this(exportRoot, hasStream, ioListener, null);
    }

    public AbstractExportContext(Item exportRoot, boolean hasStream,
                                 IOListener ioListener, MimeResolver mimeResolver) {
    public AbstractExportContext(
            Item exportRoot, boolean hasStream, IOListener ioListener) {
         this.exportRoot = exportRoot;
         this.hasStream = hasStream;
         this.ioListener = (ioListener != null) ? ioListener : new DefaultIOListener(log);
        this.mimeResolver = (mimeResolver != null) ? mimeResolver : IOUtil.MIME_RESOLVER;
     }
 
     public IOListener getIOListener() {
@@ -57,10 +50,6 @@ public abstract class AbstractExportContext implements ExportContext {
         return exportRoot;
     }
 
    public MimeResolver getMimeResolver() {
        return mimeResolver;
    }

     public boolean hasStream() {
         return hasStream;
     }
diff --git a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/DefaultHandler.java b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/DefaultHandler.java
index 35faa0c00..4dd4bc803 100644
-- a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/DefaultHandler.java
++ b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/DefaultHandler.java
@@ -24,6 +24,10 @@ import org.apache.jackrabbit.webdav.DavResource;
 import org.apache.jackrabbit.webdav.xml.Namespace;
 import org.apache.jackrabbit.webdav.property.DavPropertyName;
 import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -70,21 +74,26 @@ public class DefaultHandler implements IOHandler, PropertyHandler {
 
     private static Logger log = LoggerFactory.getLogger(DefaultHandler.class);
 
    private String collectionNodetype = JcrConstants.NT_FOLDER;
    private String defaultNodetype = JcrConstants.NT_FILE;
    /* IMPORTANT NOTE: for webDAV compliance the default nodetype of the content
       node has been changed from nt:resource to nt:unstructured. */
    private String contentNodetype = JcrConstants.NT_UNSTRUCTURED;
    private String collectionNodetype;

    private String defaultNodetype;

    private String contentNodetype;
 
     private IOManager ioManager;
 
     /**
     * Creates a new <code>DefaultHandler</code> with default nodetype definitions
     * and without setting the IOManager.
     * Creates a new <code>DefaultHandler</code> with default nodetype definitions:<br>
     * <ul>
     * <li>Nodetype for Collection: {@link JcrConstants#NT_FOLDER nt:folder}</li>
     * <li>Nodetype for Non-Collection: {@link JcrConstants#NT_FILE nt:file}</li>
     * <li>Nodetype for Non-Collection content: {@link JcrConstants#NT_UNSTRUCTURED nt:unstructured}</li>
     * </ul>
      *
     * @see IOHandler#setIOManager(IOManager)
     * @param ioManager the I/O manager
      */
     public DefaultHandler() {
        this(null);
     }
 
     /**
@@ -92,13 +101,19 @@ public class DefaultHandler implements IOHandler, PropertyHandler {
      * <ul>
      * <li>Nodetype for Collection: {@link JcrConstants#NT_FOLDER nt:folder}</li>
      * <li>Nodetype for Non-Collection: {@link JcrConstants#NT_FILE nt:file}</li>
     * <li>Nodetype for Non-Collection content: {@link JcrConstants#NT_RESOURCE nt:resource}</li>
     * <li>Nodetype for Non-Collection content: {@link JcrConstants#NT_UNSTRUCTURED nt:unstructured}</li>
      * </ul>
      *
      * @param ioManager the I/O manager
      */
     public DefaultHandler(IOManager ioManager) {
        this.ioManager = ioManager;
        this(ioManager,
                JcrConstants.NT_FOLDER,
                JcrConstants.NT_FILE,
                // IMPORTANT NOTE: for webDAV compliance the default type
                // of the content node has been changed from nt:resource to
                // nt:unstructured
                JcrConstants.NT_UNSTRUCTURED);
     }
 
     /**
@@ -640,6 +655,24 @@ public class DefaultHandler implements IOHandler, PropertyHandler {
         return failures;
     }
 
    /**
     * Detects the media type of a document based on the given name.
     *
     * @param name document name
     * @return detected content type (or application/octet-stream)
     */
    protected String detect(String name) {
        try {
            Metadata metadata = new Metadata();
            metadata.set(Metadata.RESOURCE_NAME_KEY, name);
            return ioManager.getDetector().detect(null, metadata).toString();
        } catch (IOException e) {
            // Can not happen since the InputStream above is null
            throw new IllegalStateException(
                    "Unexpected IOException", e);
        }
    }

     //------------------------------------------------------------< private >---
     /**
      * Builds a webdav property name from the given jcrName. In case the jcrName
diff --git a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/DefaultIOManager.java b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/DefaultIOManager.java
index d0c47e64a..0fa5be62e 100644
-- a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/DefaultIOManager.java
++ b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/DefaultIOManager.java
@@ -16,6 +16,7 @@
  */
 package org.apache.jackrabbit.server.io;
 
import org.apache.tika.detect.Detector;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
diff --git a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/ExportContext.java b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/ExportContext.java
index a807d44cd..9fa428bbe 100644
-- a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/ExportContext.java
++ b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/ExportContext.java
@@ -17,6 +17,7 @@
 package org.apache.jackrabbit.server.io;
 
 import javax.jcr.Item;

 import java.io.OutputStream;
 
 /**
@@ -38,13 +39,6 @@ public interface ExportContext extends IOContext {
      */
     public OutputStream getOutputStream();
 
    /**
     * Return the <code>MimeResolver</code> defined for this export context.
     *
     * @return mimetype resolver defined for this export context.
     */
    public MimeResolver getMimeResolver();

     /**
      * Set the content type for the resource content
      *
diff --git a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/ExportContextImpl.java b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/ExportContextImpl.java
index ba7679f31..173b34117 100644
-- a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/ExportContextImpl.java
++ b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/ExportContextImpl.java
@@ -19,6 +19,7 @@ package org.apache.jackrabbit.server.io;
 import org.apache.jackrabbit.webdav.DavConstants;
 import org.apache.jackrabbit.webdav.DavResource;
 import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.tika.detect.Detector;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -53,13 +54,9 @@ public class ExportContextImpl extends AbstractExportContext {
     private File outFile;
     private OutputStream outStream;
 
    public ExportContextImpl(Item exportRoot, OutputContext outputCtx) throws IOException {
        this(exportRoot, outputCtx, null);
    }

    public ExportContextImpl(Item exportRoot, OutputContext outputCtx,
                             MimeResolver mimeResolver) throws IOException {
        super(exportRoot, (outputCtx != null) ? outputCtx.hasStream() : false, null, mimeResolver);
    public ExportContextImpl(Item exportRoot, OutputContext outputCtx)
            throws IOException {
        super(exportRoot, outputCtx != null && outputCtx.hasStream(), null);
         this.outputCtx = outputCtx;
         if (hasStream()) {
             // we need a tmp file, since the export could fail
diff --git a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/IOManager.java b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/IOManager.java
index 99bfd5bb4..a31031eea 100644
-- a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/IOManager.java
++ b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/IOManager.java
@@ -17,6 +17,7 @@
 package org.apache.jackrabbit.server.io;
 
 import org.apache.jackrabbit.webdav.DavResource;
import org.apache.tika.detect.Detector;
 
 import java.io.IOException;
 
@@ -40,6 +41,20 @@ public interface IOManager {
      */
     public IOHandler[] getIOHandlers();
 
    /**
     * Return the configured type detector.
     *
     * @return content type detector
     */
    Detector getDetector();

    /**
     * Sets the configured type detector.
     *
     * @param detector content type detector.
     */
    void setDetector(Detector detector);

     /**
      * Passes the specified context and boolean value to the IOHandlers present
      * on this manager.
diff --git a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/IOManagerImpl.java b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/IOManagerImpl.java
index cb3b38b2f..ddd1524ba 100644
-- a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/IOManagerImpl.java
++ b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/IOManagerImpl.java
@@ -16,13 +16,14 @@
  */
 package org.apache.jackrabbit.server.io;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.webdav.DavResource;

 import java.io.IOException;
import java.util.List;
 import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.webdav.DavResource;
import org.apache.tika.detect.Detector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
 /**
  * <code>IOManagerImpl</code> represents the most simple <code>IOManager</code>
@@ -33,6 +34,11 @@ public class IOManagerImpl implements IOManager {
 
     private static Logger log = LoggerFactory.getLogger(IOManagerImpl.class);
 
    /**
     * Content type detector.
     */
    private Detector detector;

     private final List ioHandlers = new ArrayList();
 
     /**
@@ -63,6 +69,24 @@ public class IOManagerImpl implements IOManager {
         return (IOHandler[]) ioHandlers.toArray(new IOHandler[ioHandlers.size()]);
     }
 
    /**
     * Return the configured type detector.
     *
     * @return content type detector
     */
    public Detector getDetector() {
        return detector;
    }

    /**
     * Sets the configured type detector.
     *
     * @param detector content type detector
     */
    public void setDetector(Detector detector) {
        this.detector = detector;
    }

     /**
      * @see IOManager#importContent(ImportContext, boolean)
      */
diff --git a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/IOUtil.java b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/IOUtil.java
index 10dbda6d7..9c11e39aa 100644
-- a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/IOUtil.java
++ b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/IOUtil.java
@@ -48,12 +48,6 @@ public class IOUtil {
      */
     public static final long UNDEFINED_LENGTH = -1;
 
    /**
     * MimeType resolver used to retrieve the mimetype if no content type is
     * available during import.
     */
    public static final MimeResolver MIME_RESOLVER = new MimeResolver();

     /**
      * Return the last modification time as formatted string.
      *
diff --git a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/ImportContext.java b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/ImportContext.java
index ab58f51cd..b30df6392 100644
-- a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/ImportContext.java
++ b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/ImportContext.java
@@ -17,6 +17,9 @@
 package org.apache.jackrabbit.server.io;
 
 import javax.jcr.Item;

import org.apache.tika.detect.Detector;

 import java.io.InputStream;
 
 /**
@@ -32,13 +35,6 @@ public interface ImportContext extends IOContext {
      */
     public Item getImportRoot();
 
    /**
     * Return the <code>MimeResolver</code> defined for this import context.
     *
     * @return mimetype resolver defined for this import context.
     */
    public MimeResolver getMimeResolver();

     /**
      * Returns the system id of the resource to be imported. This id depends on
      * the system the resource is comming from. it can be a filename, a
diff --git a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/ImportContextImpl.java b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/ImportContextImpl.java
index 51352a03b..05433c203 100644
-- a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/ImportContextImpl.java
++ b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/ImportContextImpl.java
@@ -17,10 +17,15 @@
 package org.apache.jackrabbit.server.io;
 
 import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import javax.jcr.Item;

import java.io.BufferedInputStream;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
@@ -38,64 +43,13 @@ public class ImportContextImpl implements ImportContext {
     private final Item importRoot;
     private final String systemId;
     private final File inputFile;
    private final MimeResolver mimeResolver;
 
     private InputContext inputCtx;
     private boolean completed;
 
    /**
     * Creates a new item import context with the given root item and the
     * specified <code>InputContext</code>. If the input context provides an
     * input stream, the stream is written to a temporary file in order to avoid
     * problems with multiple IOHandlers that try to run the import but fail.
     * The temporary file is deleted as soon as this context is informed that
     * the import has been completed and it will not be used any more.
     *
     * @param importRoot the import root node
     * @param systemId
     * @param inputCtx wrapped by this <code>ImportContext</code>
     */
    public ImportContextImpl(Item importRoot, String systemId, InputContext inputCtx) throws IOException {
        this(importRoot, systemId, inputCtx, null);
    }
    private final Detector detector;
 
    /**
     * Creates a new item import context with the given root item and the
     * specified <code>InputContext</code>. If the input context provides an
     * input stream, the stream is written to a temporary file in order to avoid
     * problems with multiple IOHandlers that try to run the import but fail.
     * The temporary file is deleted as soon as this context is informed that
     * the import has been completed and it will not be used any more.
     *
     * @param importRoot the import root node
     * @param systemId
     * @param inputCtx wrapped by this <code>ImportContext</code>
     * @param mimeResolver
     */
    public ImportContextImpl(Item importRoot, String systemId, InputContext inputCtx,
                             MimeResolver mimeResolver) throws IOException {
        this(importRoot, systemId, (inputCtx != null) ? inputCtx.getInputStream() : null, null, mimeResolver);
        this.inputCtx = inputCtx;
    }

    /**
     * Creates a new item import context. The specified InputStream is written
     * to a temporary file in order to avoid problems with multiple IOHandlers
     * that try to run the import but fail. The temporary file is deleted as soon
     * as this context is informed that the import has been completed and it
     * will not be used any more.
     *
     * @param importRoot
     * @param systemId
     * @param in
     * @param ioListener
     * @throws IOException
     * @see ImportContext#informCompleted(boolean)
     */
    public ImportContextImpl(Item importRoot, String systemId, InputStream in,
                             IOListener ioListener) throws IOException {
        this(importRoot, systemId, in, ioListener, null);
    }
    private final MediaType type;
 
     /**
      * Creates a new item import context. The specified InputStream is written
@@ -106,20 +60,35 @@ public class ImportContextImpl implements ImportContext {
      *
      * @param importRoot
      * @param systemId
     * @param in
     * @param inputCtx input context, or <code>null</code>
     * @param stream document input stream, or <code>null</code>
      * @param ioListener
     * @param mimeResolver
     * @param detector content type detector
      * @throws IOException
      * @see ImportContext#informCompleted(boolean)
      */
    public ImportContextImpl(Item importRoot, String systemId, InputStream in,
                             IOListener ioListener, MimeResolver mimeResolver)
    public ImportContextImpl(
            Item importRoot, String systemId, InputContext inputCtx,
            InputStream stream, IOListener ioListener, Detector detector)
             throws IOException {
         this.importRoot = importRoot;
         this.systemId = systemId;
        this.inputFile = IOUtil.getTempFile(in);
        this.inputCtx = inputCtx;
         this.ioListener = (ioListener != null) ? ioListener : new DefaultIOListener(log);
        this.mimeResolver = (mimeResolver == null) ? IOUtil.MIME_RESOLVER : mimeResolver;

        Metadata metadata = new Metadata();
        if (inputCtx != null && inputCtx.getContentType() != null) {
            metadata.set(Metadata.CONTENT_TYPE, inputCtx.getContentType());
        }
        if (systemId != null) {
            metadata.set(Metadata.RESOURCE_NAME_KEY, systemId);
        }
        if (stream != null && !stream.markSupported()) {
            stream = new BufferedInputStream(stream);
        }
        this.detector = detector;
        this.type = detector.detect(stream, metadata);
        this.inputFile = IOUtil.getTempFile(stream);
     }
 
     /**
@@ -137,10 +106,10 @@ public class ImportContextImpl implements ImportContext {
     }
 
     /**
     * @see ImportContext#getImportRoot()
     * @see ImportContext#getDetector()
      */
    public MimeResolver getMimeResolver() {
        return mimeResolver;
    public Detector getDetector() {
        return detector;
     }
 
     /**
@@ -209,35 +178,18 @@ public class ImportContextImpl implements ImportContext {
         return length;
     }
 
    /**
     * @return the content type present on the <code>InputContext</code> or
     * <code>null</code>
     * @see InputContext#getContentType()
     */
    private String getContentType() {
        return (inputCtx != null) ? inputCtx.getContentType() : null;
    }

     /**
      * @see ImportContext#getMimeType()
      */
     public String getMimeType() {
        String contentType = getContentType();
        String mimeType = null;
        if (contentType != null) {
            mimeType = IOUtil.getMimeType(contentType);
        } else if (getSystemId() != null) {
            mimeType = mimeResolver.getMimeType(getSystemId());
        }
        return mimeType;
        return IOUtil.getMimeType(type.toString());
     }
 
     /**
      * @see ImportContext#getEncoding()
      */
     public String getEncoding() {
        String contentType = getContentType();
        return (contentType != null) ? IOUtil.getEncoding(contentType) : null;
        return IOUtil.getEncoding(type.toString());
     }
 
     /**
diff --git a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/MimeResolver.java b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/MimeResolver.java
index 58fe5e4a7..e69de29bb 100644
-- a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/MimeResolver.java
++ b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/MimeResolver.java
@@ -1,103 +0,0 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.server.io;

import org.apache.jackrabbit.util.Text;

import java.io.IOException;
import java.util.Properties;

/**
 * This Class implements a very simple mime type resolver.
 */
public class MimeResolver {

    /**
     * the loaded mimetypes
     */
    private Properties mimeTypes = new Properties();

    /**
     * the default mimetype
     */
    private String defaultMimeType = "application/octet-stream";

    /**
     * Creates a new mimetype resolver containing the default mappings and having
     * "application/octet-stream" set as default mimetype.
     */
    public MimeResolver() {
        try {
            // init the mime types
            mimeTypes.load(getClass().getResourceAsStream("mimetypes.properties"));
        } catch (IOException e) {
            throw new InternalError("Unable to load mimetypes: " + e.toString());
        }
    }

    /**
     * Creates a new mime type resolver extending the default mapping by the
     * entries of the given Properties. The default mimetype is set to the
     * given <code>defaultMimeType</code>.
     *
     * @param additionalProperties MimeType mappings to be added to the default
     * properties.
     * @param defaultMimeType The default mimetype. A non-null String with a
     * length greater than 0.
     */
    public MimeResolver(Properties additionalProperties, String defaultMimeType) {
        // init default mimetypes.
        this();
        // extend or adjust mapping.
        if (additionalProperties != null && !additionalProperties.isEmpty()) {
            mimeTypes.putAll(additionalProperties);
        }
        // set the default type.
        if (defaultMimeType != null && defaultMimeType.length() > 0) {
            this.defaultMimeType = defaultMimeType;
        }
    }

    /**
     * Returns the default mime type
     * @return
     */
    public String getDefaultMimeType() {
        return defaultMimeType;
    }

    /**
     * Sets the default mime type
     * @param defaultMimeType
     */
    public void setDefaultMimeType(String defaultMimeType) {
        this.defaultMimeType = defaultMimeType;
    }

    /**
     * Retrusn the mime type for the given name.
     * @param filename
     * @return
     */
    public String getMimeType(String filename) {
        String ext = Text.getName(filename, '.');
        if (ext.equals("")) {
            ext = filename;
        }
        return mimeTypes.getProperty(ext.toLowerCase(), defaultMimeType);
    }
}
diff --git a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/XmlHandler.java b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/XmlHandler.java
index 060d29a26..e41e57ca2 100644
-- a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/XmlHandler.java
++ b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/XmlHandler.java
@@ -149,7 +149,7 @@ public class XmlHandler extends DefaultHandler {
                 if (contentNode.hasProperty(JcrConstants.JCR_MIMETYPE)) {
                     mimeType = contentNode.getProperty(JcrConstants.JCR_MIMETYPE).getString();
                 } else {
                    mimeType = context.getMimeResolver().getMimeType(context.getExportRoot().getName());
                    mimeType = detect(context.getExportRoot().getName());
                 }
             } catch (RepositoryException e) {
                 // ignore and return false
diff --git a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/ZipHandler.java b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/ZipHandler.java
index 347f47eaf..1047e7bb1 100644
-- a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/ZipHandler.java
++ b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/ZipHandler.java
@@ -153,7 +153,7 @@ public class ZipHandler extends DefaultHandler {
                 if (contentNode.hasProperty(JcrConstants.JCR_MIMETYPE)) {
                     mimeType  = contentNode.getProperty(JcrConstants.JCR_MIMETYPE).getString();
                 } else {
                    mimeType = context.getMimeResolver().getMimeType(context.getExportRoot().getName());
                    mimeType = detect(context.getExportRoot().getName());
                 }
             } catch (RepositoryException e) {
                 // ignore and return false
@@ -283,7 +283,8 @@ public class ZipHandler extends DefaultHandler {
         private final ZipEntry entry;
 
         private ZipEntryImportContext(ImportContext context, ZipEntry entry, BoundedInputStream bin, Node contentNode) throws IOException, RepositoryException {
            super(contentNode, Text.getName(makeValidJCRPath(entry.getName(), true)), bin, context.getIOListener(), context.getMimeResolver());
            super(contentNode, Text.getName(makeValidJCRPath(entry.getName(), true)),
                    null, bin, context.getIOListener(), getIOManager().getDetector());
             this.entry = entry;
             String path = makeValidJCRPath(entry.getName(), true);
             importRoot = IOUtil.mkDirs(contentNode, Text.getRelativeParent(path, 1), getCollectionNodeType());
@@ -312,7 +313,7 @@ public class ZipHandler extends DefaultHandler {
         private OutputStream out;
 
         private ZipEntryExportContext(Item exportRoot, OutputStream out, ExportContext context, int pos) {
            super(exportRoot, out != null, context.getIOListener(), context.getMimeResolver());
            super(exportRoot, out != null, context.getIOListener());
             this.out = out;
             try {
                 String entryPath = (exportRoot.getPath().length() > pos) ? exportRoot.getPath().substring(pos) : "";
diff --git a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/webdav/simple/DavResourceImpl.java b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/webdav/simple/DavResourceImpl.java
index 285f0374a..efc59acd4 100644
-- a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/webdav/simple/DavResourceImpl.java
++ b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/webdav/simple/DavResourceImpl.java
@@ -897,7 +897,10 @@ public class DavResourceImpl implements DavResource, BindableResource, JcrConsta
      * @throws IOException
      */
     protected ImportContext getImportContext(InputContext inputCtx, String systemId) throws IOException {
        return new ImportContextImpl(node, systemId, inputCtx, config.getMimeResolver());
        return new ImportContextImpl(
                node, systemId, inputCtx,
                (inputCtx != null) ? inputCtx.getInputStream() : null,
                new DefaultIOListener(log), config.getDetector());
     }
 
     /**
@@ -908,7 +911,7 @@ public class DavResourceImpl implements DavResource, BindableResource, JcrConsta
      * @throws IOException
      */
     protected ExportContext getExportContext(OutputContext outputCtx) throws IOException {
        return new ExportContextImpl(node, outputCtx, config.getMimeResolver());
        return new ExportContextImpl(node, outputCtx);
     }
 
     /**
diff --git a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/webdav/simple/ResourceConfig.java b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/webdav/simple/ResourceConfig.java
index 064c8b9ac..79d7e035b 100644
-- a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/webdav/simple/ResourceConfig.java
++ b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/webdav/simple/ResourceConfig.java
@@ -16,33 +16,33 @@
  */
 package org.apache.jackrabbit.webdav.simple;
 
import org.apache.jackrabbit.server.io.IOManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

 import org.apache.jackrabbit.server.io.DefaultIOManager;
 import org.apache.jackrabbit.server.io.IOHandler;
import org.apache.jackrabbit.server.io.PropertyManager;
import org.apache.jackrabbit.server.io.IOManager;
 import org.apache.jackrabbit.server.io.PropertyHandler;
import org.apache.jackrabbit.server.io.PropertyManager;
 import org.apache.jackrabbit.server.io.PropertyManagerImpl;
import org.apache.jackrabbit.server.io.MimeResolver;
import org.apache.jackrabbit.webdav.xml.ElementIterator;
 import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.ElementIterator;
import org.apache.tika.detect.Detector;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
 import org.w3c.dom.Document;
import org.w3c.dom.Element;
 import org.xml.sax.SAXException;
 
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.io.IOException;
import java.io.InputStream;

 /**
  * <code>ResourceConfig</code>...
  */
@@ -50,12 +50,20 @@ public class ResourceConfig {
 
     private static Logger log = LoggerFactory.getLogger(ResourceConfig.class);
 
    /**
     * Content type detector.
     */
    private final Detector detector;

     private ItemFilter itemFilter;
     private IOManager ioManager;
     private PropertyManager propManager;
     private String[] nodetypeNames = new String[0];
     private boolean collectionNames = false;
    private MimeResolver mimeResolver;

    public ResourceConfig(Detector detector) {
        this.detector = detector;
    }
 
     /**
      * Tries to parse the given xml configuration file.
@@ -86,6 +94,11 @@ public class ResourceConfig {
      *    &gt;
      * &lt;!ELEMENT defaultmimetype (CDATA) &gt;
      * </pre>
     * <p>
     * The &lt;mimetypeproperties/&gt; settings have been deprecated and will
     * be ignored with a warning. Instead you can use the
     * {@link SimpleWebdavServlet#INIT_PARAM_MIME_INFO mime-info}
     * servlet initialization parameter to customize the media type settings.
      *
      * @param configURL
      */
@@ -107,6 +120,7 @@ public class ResourceConfig {
                 Object inst = buildClassFromConfig(el);
                 if (inst != null && inst instanceof IOManager) {
                     ioManager = (IOManager)inst;
                    ioManager.setDetector(detector);
                     // get optional 'iohandler' child elements and populate the
                     // ioManager with the instances
                     ElementIterator iohElements = DomUtil.getChildren(el, "iohandler", null);
@@ -177,21 +191,11 @@ public class ResourceConfig {
                 log.debug("Resource configuration: no 'filter' element specified.");
             }
 
            // optional mimetype properties
            Properties properties = new Properties();
            String defaultMimetype = null;
             el = DomUtil.getChildElement(config, "mimetypeproperties", null);
             if (el != null) {
                defaultMimetype = DomUtil.getChildText(el, "defaultmimetype", null);
                ElementIterator it = DomUtil.getChildren(el, "mimemapping", null);
                while (it.hasNext()) {
                    Element mimeMapping = it.nextElement();
                    String extension = DomUtil.getAttribute(mimeMapping, "extension", null);
                    String mimetype = DomUtil.getAttribute(mimeMapping, "mimetype", null);
                    properties.put(extension, mimetype);
                }
                log.warn("Ignoring deprecated mimetypeproperties settings: {}",
                        configURL);
             }
            mimeResolver = new MimeResolver(properties, defaultMimetype);
         } catch (IOException e) {
             log.debug("Invalid resource configuration: " + e.getMessage());
         } catch (ParserConfigurationException e) {
@@ -270,6 +274,7 @@ public class ResourceConfig {
         if (ioManager == null) {
             log.debug("ResourceConfig: missing io-manager > building DefaultIOManager ");
             ioManager = new DefaultIOManager();
            ioManager.setDetector(detector);
         }
         return ioManager;
     }
@@ -331,10 +336,12 @@ public class ResourceConfig {
     }
 
     /**
     * Returns the configured content type detector.
      *
     * @return
     * @return content type detector
      */
    public MimeResolver getMimeResolver() {
        return mimeResolver;
    public Detector getDetector() {
        return detector;
     }
}
\ No newline at end of file

}
diff --git a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/webdav/simple/ResourceFactoryImpl.java b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/webdav/simple/ResourceFactoryImpl.java
index cd0db7ef8..ad2c48d5d 100644
-- a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/webdav/simple/ResourceFactoryImpl.java
++ b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/webdav/simple/ResourceFactoryImpl.java
@@ -48,17 +48,6 @@ public class ResourceFactoryImpl implements DavResourceFactory {
     private final LockManager lockMgr;
     private final ResourceConfig resourceConfig;
 
    /**
     * Create a new <code>ResourceFactory</code> that uses the given lock
     * manager and the default {@link ResourceConfig resource config}.
     *
     * @param lockMgr
     */
    public ResourceFactoryImpl(LockManager lockMgr) {
        this.lockMgr = lockMgr;
        this.resourceConfig = new ResourceConfig();
    }

     /**
      * Create a new <code>ResourceFactory</code> that uses the given lock
      * manager and resource filter.
@@ -68,7 +57,7 @@ public class ResourceFactoryImpl implements DavResourceFactory {
      */
     public ResourceFactoryImpl(LockManager lockMgr, ResourceConfig resourceConfig) {
         this.lockMgr = lockMgr;
        this.resourceConfig = (resourceConfig != null) ? resourceConfig : new ResourceConfig();
        this.resourceConfig = resourceConfig;
     }
 
     /**
diff --git a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/webdav/simple/SimpleWebdavServlet.java b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/webdav/simple/SimpleWebdavServlet.java
index eca5a3023..023d9f41d 100644
-- a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/webdav/simple/SimpleWebdavServlet.java
++ b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/webdav/simple/SimpleWebdavServlet.java
@@ -28,13 +28,19 @@ import org.apache.jackrabbit.webdav.WebdavRequest;
 import org.apache.jackrabbit.webdav.lock.LockManager;
 import org.apache.jackrabbit.webdav.lock.SimpleLockManager;
 import org.apache.jackrabbit.webdav.server.AbstractWebdavServlet;
import org.apache.tika.detect.Detector;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypesFactory;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import javax.jcr.Repository;
 import javax.servlet.ServletContext;
 import javax.servlet.ServletException;

import java.io.IOException;
 import java.net.MalformedURLException;
import java.net.URL;
 
 /**
  * WebdavServlet provides webdav support (level 1 and 2 complient) for
@@ -75,6 +81,13 @@ public abstract class SimpleWebdavServlet extends AbstractWebdavServlet {
      */
     public static final String INIT_PARAM_RESOURCE_CONFIG = "resource-config";
 
    /**
     * Name of the parameter that specifies the servlet resource path of
     * a custom &lt;mime-info/&gt; configuration file. The default setting
     * is to use the MIME media type database included in Apache Tika.
     */
    public static final String INIT_PARAM_MIME_INFO = "mime-info";

     /**
      * Servlet context attribute used to store the path prefix instead of
      * having a static field with this servlet. The latter causes problems
@@ -150,10 +163,10 @@ public abstract class SimpleWebdavServlet extends AbstractWebdavServlet {
         }
         log.info("WWW-Authenticate header = '" + authenticate_header + "'");
 
        config = new ResourceConfig(getDetector());
         String configParam = getInitParameter(INIT_PARAM_RESOURCE_CONFIG);
         if (configParam != null) {
             try {
                config = new ResourceConfig();
                 config.parse(getServletContext().getResource(configParam));
             } catch (MalformedURLException e) {
                 log.debug("Unable to build resource filter provider.");
@@ -161,6 +174,40 @@ public abstract class SimpleWebdavServlet extends AbstractWebdavServlet {
         }
     }
 
    /**
     * Reads and returns the configured &lt;mime-info/&gt; database.
     *
     * @see #INIT_PARAM_MIME_INFO
     * @return MIME media type database
     * @throws ServletException if the database is invalid or can not be read
     */
    private Detector getDetector() throws ServletException {
        URL url;

        String mimeInfo = getInitParameter(INIT_PARAM_MIME_INFO);
        if (mimeInfo != null) {
            try {
                url = getServletContext().getResource(mimeInfo);
            } catch (MalformedURLException e) {
                throw new ServletException(
                        "Invalid " + INIT_PARAM_MIME_INFO
                        + " configuration setting: " + mimeInfo, e);
            }
        } else {
            url = MimeTypesFactory.class.getResource("tika-mimetypes.xml");
        }

        try {
            return MimeTypesFactory.create(url);
        } catch (MimeTypeException e) {
            throw new ServletException(
                    "Invalid MIME media type database: " + url, e);
        } catch (IOException e) {
            throw new ServletException(
                    "Unable to read MIME media type database: " + url, e);
        }
    }

     /**
      * {@inheritDoc}
      */
@@ -345,10 +392,6 @@ public abstract class SimpleWebdavServlet extends AbstractWebdavServlet {
      * @return the resource configuration.
      */
     public ResourceConfig getResourceConfig() {
        // fallback if no config present
        if (config == null) {
            config = new ResourceConfig();
        }
         return config;
     }
 
- 
2.19.1.windows.1

