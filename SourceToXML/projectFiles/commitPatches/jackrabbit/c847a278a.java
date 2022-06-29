From c847a278af665f9c931ae4a756cee079799d5d2e Mon Sep 17 00:00:00 2001
From: Tobias Bocanegra <tripod@apache.org>
Date: Wed, 15 Jun 2005 08:47:56 +0000
Subject: [PATCH] - [JCR-134] extensibility patch for DavResourceImpl - moving
 JcrConstants.java to commons - fixing minor import issues - readding lost
 stuff - adding proper shutdown to RepositoryStartupServlet.java

git-svn-id: https://svn.apache.org/repos/asf/incubator/jackrabbit/trunk@190721 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/jackrabbit/JcrConstants.java   |   0
 .../server/AbstractWebdavServlet.java         |  27 +++-
 .../server/io/AbstractImportCommand.java      |   4 +-
 .../server/io/XMLImportCommand.java           | 128 +++++++++++++++++-
 .../webdav/simple/DavResourceImpl.java        |   8 ++
 contrib/jcr-server/webapp/project.xml         |  18 +++
 .../j2ee/JCRWebdavServerServlet.java          |  61 +++++++--
 .../j2ee/RepositoryStartupServlet.java        |  13 +-
 .../jackrabbit/j2ee/SimpleWebdavServlet.java  | 117 +++++++++++++---
 .../webdav/lock/SimpleLockManager.java        |  71 +++++-----
 10 files changed, 378 insertions(+), 69 deletions(-)
 rename contrib/jcr-server/{webdav => commons}/src/java/org/apache/jackrabbit/JcrConstants.java (100%)

diff --git a/contrib/jcr-server/webdav/src/java/org/apache/jackrabbit/JcrConstants.java b/contrib/jcr-server/commons/src/java/org/apache/jackrabbit/JcrConstants.java
similarity index 100%
rename from contrib/jcr-server/webdav/src/java/org/apache/jackrabbit/JcrConstants.java
rename to contrib/jcr-server/commons/src/java/org/apache/jackrabbit/JcrConstants.java
diff --git a/contrib/jcr-server/server/src/java/org/apache/jackrabbit/server/AbstractWebdavServlet.java b/contrib/jcr-server/server/src/java/org/apache/jackrabbit/server/AbstractWebdavServlet.java
index 5affa0951..b09acc8a0 100644
-- a/contrib/jcr-server/server/src/java/org/apache/jackrabbit/server/AbstractWebdavServlet.java
++ b/contrib/jcr-server/server/src/java/org/apache/jackrabbit/server/AbstractWebdavServlet.java
@@ -107,7 +107,14 @@ abstract public class AbstractWebdavServlet extends HttpServlet implements DavCo
      *
      * @return the session provider
      */
    abstract public DavSessionProvider getSessionProvider();
    abstract public DavSessionProvider getDavSessionProvider();

    /**
     * Returns the <code>DavSessionProvider</code>.
     *
     * @param davSessionProvider
     */
    abstract public void setDavSessionProvider(DavSessionProvider davSessionProvider);
 
     /**
      * Returns the <code>DavLocatorFactory</code>.
@@ -116,6 +123,13 @@ abstract public class AbstractWebdavServlet extends HttpServlet implements DavCo
      */
     abstract public DavLocatorFactory getLocatorFactory();
 
    /**
     * Sets the <code>DavLocatorFactory</code>.
     *
     * @param locatorFactory
     */
    abstract public void setLocatorFactory(DavLocatorFactory locatorFactory);

     /**
      * Returns the <code>DavResourceFactory</code>.
      *
@@ -123,6 +137,13 @@ abstract public class AbstractWebdavServlet extends HttpServlet implements DavCo
      */
     abstract public DavResourceFactory getResourceFactory();
 
    /**
     * Sets the <code>DavResourceFactory</code>.
     *
     * @param resourceFactory
     */
    abstract public void setResourceFactory(DavResourceFactory resourceFactory);

     /**
      * Returns the value of the 'WWW-Authenticate' header, that is returned in
      * case of 401 error.
@@ -146,7 +167,7 @@ abstract public class AbstractWebdavServlet extends HttpServlet implements DavCo
         WebdavResponse webdavResponse = new WebdavResponseImpl(response);
         try {
             // make sure there is a authenticated user
            if (!getSessionProvider().attachSession(webdavRequest)) {
            if (!getDavSessionProvider().attachSession(webdavRequest)) {
                 return;
             }
 
@@ -170,7 +191,7 @@ abstract public class AbstractWebdavServlet extends HttpServlet implements DavCo
                 webdavResponse.sendErrorResponse(e);
             }
         } finally {
            getSessionProvider().releaseSession(webdavRequest);
            getDavSessionProvider().releaseSession(webdavRequest);
         }
     }
 
diff --git a/contrib/jcr-server/server/src/java/org/apache/jackrabbit/server/io/AbstractImportCommand.java b/contrib/jcr-server/server/src/java/org/apache/jackrabbit/server/io/AbstractImportCommand.java
index 49a2380c7..1bde321f8 100644
-- a/contrib/jcr-server/server/src/java/org/apache/jackrabbit/server/io/AbstractImportCommand.java
++ b/contrib/jcr-server/server/src/java/org/apache/jackrabbit/server/io/AbstractImportCommand.java
@@ -75,9 +75,9 @@ public abstract class AbstractImportCommand extends AbstractCommand {
 
         if (importResource(context, fileNode, in)) {
             context.setInputStream(null);
            // set current node
            context.setNode(fileNode);
         }
        // set current node
        context.setNode(fileNode);
         return false;
     }
 
diff --git a/contrib/jcr-server/server/src/java/org/apache/jackrabbit/server/io/XMLImportCommand.java b/contrib/jcr-server/server/src/java/org/apache/jackrabbit/server/io/XMLImportCommand.java
index cf9ad8b27..2d91fc14d 100644
-- a/contrib/jcr-server/server/src/java/org/apache/jackrabbit/server/io/XMLImportCommand.java
++ b/contrib/jcr-server/server/src/java/org/apache/jackrabbit/server/io/XMLImportCommand.java
@@ -16,10 +16,15 @@
  */
 package org.apache.jackrabbit.server.io;
 
import org.apache.log4j.Logger;

 import javax.jcr.Node;
 import javax.jcr.ImportUUIDBehavior;
 import javax.jcr.RepositoryException;
 import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
 import java.util.Calendar;
 
 /**
@@ -31,7 +36,12 @@ import java.util.Calendar;
  * <li>jcr:lastModified (from current time)
  * </ul>
  */
public class XMLImportCommand extends AbstractImportCommand {
public class XMLImportCommand extends AbstractCommand {

    /**
     * the default logger
     */
    private static Logger log = Logger.getLogger(XMLImportCommand.class);
 
     /**
      * the xml content type
@@ -43,6 +53,100 @@ public class XMLImportCommand extends AbstractImportCommand {
      */
     private String contentNodeType = NT_UNSTRUCTURED;
 
    /**
     * the nodetype for the node
     */
    private String nodeType = "nt:file";

    /**
     * Executes this command by calling {@link #importResource} if
     * the given context is of the correct class.
     *
     * @param context the (import) context.
     * @return the return value of the delegated method or false;
     * @throws Exception in an error occurrs
     */
    public boolean execute(AbstractContext context) throws Exception {
        if (context instanceof ImportContext) {
            return execute((ImportContext) context);
        } else {
            return false;
        }
    }

    /**
     * Executes this command. It checks if this command can handle the content
     * type and delegates it to {@link #importResource}. If the import is
     * successfull, the input stream of the importcontext is cleared.
     *
     * @param context the import context
     * @return false
     * @throws Exception if an error occurrs
     */
    public boolean execute(ImportContext context) throws Exception {
        Node parentNode = context.getNode();
        InputStream in = context.getInputStream();
        if (in == null) {
            // assume already consumed
            return false;
        }
        if (!canHandle(context.getContentType())) {
            // ignore imports
            return false;
        }

        // we need a tmp file, since the import could fail
        File tmpFile = File.createTempFile("__xmlimport", "xml");
        FileOutputStream out = new FileOutputStream(tmpFile);
        byte[] buffer = new byte[8192];
        boolean first = true;
        boolean isSysView = false;
        int read;
        while ((read=in.read(buffer))>0) {
            out.write(buffer, 0, read);
            if (first) {
                first = false;
                // could be too less information. is a bit a lazy test
                isSysView = new String(buffer, 0, read).indexOf("<sv:node") >= 0;
            }
        }
        out.close();
        in.close();
        in = new FileInputStream(tmpFile);
        context.setInputStream(in);

        if (isSysView) {
            // just import sys view
            try {
                parentNode.getSession().importXML(parentNode.getPath(), in,
                        ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
                context.setInputStream(null);
                // no further processing
                return true;
            } catch (RepositoryException e) {
                // if error occurrs, reset input stream
                context.setInputStream(new FileInputStream(tmpFile));
                log.error("Unable to import sysview. will store as normal file: " + e.toString());
                parentNode.refresh(false);
            } finally {
                in.close();
            }
        } else {
            // check 'file' node
            Node fileNode = parentNode.hasNode(context.getSystemId())
                    ? parentNode.getNode(context.getSystemId())
                    : parentNode.addNode(context.getSystemId(), nodeType);
            if (importResource(context, fileNode, in)) {
                context.setInputStream(null);
                // set current node
                context.setNode(fileNode);
            } else {
                context.setInputStream(new FileInputStream(tmpFile));
            }
        }
        return false;
    }

     /**
      * Imports the resource by deseriaizing the xml.
      * @param ctx
@@ -54,6 +158,7 @@ public class XMLImportCommand extends AbstractImportCommand {
     public boolean importResource(ImportContext ctx, Node parentNode,
                                   InputStream in)
             throws Exception {

         Node content = parentNode.hasNode(JCR_CONTENT)
                 ? parentNode.getNode(JCR_CONTENT)
                 : parentNode.addNode(JCR_CONTENT, contentNodeType);
@@ -71,7 +176,17 @@ public class XMLImportCommand extends AbstractImportCommand {
         } catch (RepositoryException e) {
             // ignore
         }
        parentNode.getSession().importXML(content.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        try {
            parentNode.getSession().importXML(content.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        } catch (RepositoryException e) {
            // if this fails, we ignore import and pass to next command
            if (content.isNew()) {
                content.remove();
            }
            return false;
        } finally {
            in.close();
        }
         return true;
     }
 
@@ -101,4 +216,13 @@ public class XMLImportCommand extends AbstractImportCommand {
     public void setContentNodeType(String contentNodeType) {
         this.contentNodeType = contentNodeType;
     }

    /**
     * Sets the node type
     * @param nodeType
     */
    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

 }
diff --git a/contrib/jcr-server/server/src/java/org/apache/jackrabbit/webdav/simple/DavResourceImpl.java b/contrib/jcr-server/server/src/java/org/apache/jackrabbit/webdav/simple/DavResourceImpl.java
index 985ee2057..2c12477de 100644
-- a/contrib/jcr-server/server/src/java/org/apache/jackrabbit/webdav/simple/DavResourceImpl.java
++ b/contrib/jcr-server/server/src/java/org/apache/jackrabbit/webdav/simple/DavResourceImpl.java
@@ -586,6 +586,14 @@ public class DavResourceImpl implements DavResource, JcrConstants {
         return factory;
     }
 
    /**
     * Returns the node that is wrapped by this resource.
     * @return
     */
    protected Node getNode() {
        return node;
    }
    
     /**
      * Returns true, if this webdav resource allows for locking without checking
      * its current lock status.
diff --git a/contrib/jcr-server/webapp/project.xml b/contrib/jcr-server/webapp/project.xml
index 246ce5c9d..b4b5966a1 100644
-- a/contrib/jcr-server/webapp/project.xml
++ b/contrib/jcr-server/webapp/project.xml
@@ -68,14 +68,23 @@
             <groupId>jsr170</groupId>
             <artifactId>jcr</artifactId>
             <version>${jackrabbit.build.version.jcr}</version>
            <properties>
                <war.bundle>true</war.bundle>
            </properties>
         </dependency>
         <dependency>
             <id>jackrabbit</id>
             <version>${jackrabbit.build.version.jackrabbit}</version>
            <properties>
                <war.bundle>true</war.bundle>
            </properties>
         </dependency>
         <dependency>
             <id>jcr-rmi</id>
             <version>${jackrabbit.build.version.jcr.rmi}</version>
            <properties>
                <war.bundle>true</war.bundle>
            </properties>
         </dependency>
         
         <!-- non-jackrabbit dependencies -->
@@ -100,14 +109,23 @@
         <dependency>
             <id>commons-chain</id>
             <version>1.0</version>
            <properties>
                <war.bundle>true</war.bundle>
            </properties>
         </dependency>
         <dependency>
             <id>commons-digester</id>
             <version>1.6</version>
            <properties>
                <war.bundle>true</war.bundle>
            </properties>
         </dependency>
         <dependency>
             <id>commons-beanutils</id>
             <version>1.7.0</version>
            <properties>
                <war.bundle>true</war.bundle>
            </properties>
         </dependency>
 
         <!-- dependencies of jackrabbit -->
diff --git a/contrib/jcr-server/webapp/src/java/org/apache/jackrabbit/j2ee/JCRWebdavServerServlet.java b/contrib/jcr-server/webapp/src/java/org/apache/jackrabbit/j2ee/JCRWebdavServerServlet.java
index 5051dff79..efeeee9e1 100644
-- a/contrib/jcr-server/webapp/src/java/org/apache/jackrabbit/j2ee/JCRWebdavServerServlet.java
++ b/contrib/jcr-server/webapp/src/java/org/apache/jackrabbit/j2ee/JCRWebdavServerServlet.java
@@ -47,6 +47,7 @@ public class JCRWebdavServerServlet extends AbstractWebdavServlet implements Dav
      */
     public static final String INIT_PARAM_PREFIX = "resource-path-prefix";
 
    private String pathPrefix;
     private JCRWebdavServer server;
     private DavResourceFactory resourceFactory;
     private DavLocatorFactory locatorFactory;
@@ -66,7 +67,7 @@ public class JCRWebdavServerServlet extends AbstractWebdavServlet implements Dav
         super.init();
 
 	// set resource path prefix
	String pathPrefix = getInitParameter(INIT_PARAM_PREFIX);
	pathPrefix = getInitParameter(INIT_PARAM_PREFIX);
 	log.debug(INIT_PARAM_PREFIX + " = " + pathPrefix);
 
 	Repository repository = RepositoryAccessServlet.getRepository();
@@ -89,10 +90,14 @@ public class JCRWebdavServerServlet extends AbstractWebdavServlet implements Dav
     }
 
     /**
     * Returns true if the preconditions are met. This includes validation of
     * {@link WebdavRequest#matchesIfHeader(DavResource) If header} and validation
     * of {@link org.apache.jackrabbit.webdav.transaction.TransactionConstants#HEADER_TRANSACTIONID
     * TransactionId header}. This method will also return false if the requested
     * resource lays within a differenct workspace as is assigned to the repository
     * session attached to the given request.
      *
     * @param request
     * @param resource
     * @return
     * @see AbstractWebdavServlet#isPreconditionValid(WebdavRequest, DavResource)
      */
     protected boolean isPreconditionValid(WebdavRequest request, DavResource resource) {
         // first check matching If header
@@ -117,26 +122,66 @@ public class JCRWebdavServerServlet extends AbstractWebdavServlet implements Dav
     }
 
     /**
     * {@inheritDoc}
     * Returns the <code>DavSessionProvider</code>
     *
     * @return server
     * @see AbstractWebdavServlet#getDavSessionProvider()
      */
    public DavSessionProvider getSessionProvider() {
    public DavSessionProvider getDavSessionProvider() {
         return server;
     }
 
     /**
     * {@inheritDoc}
     * Throws <code>UnsupportedOperationException</code>.
     *
     * @see AbstractWebdavServlet#setDavSessionProvider(DavSessionProvider)
     */
    public void setDavSessionProvider(DavSessionProvider davSessionProvider) {
        throw new UnsupportedOperationException("Not implemented. DavSession(s) are provided by the 'JCRWebdavServer'");
    }

    /**
     * Returns the <code>DavLocatorFactory</code>
     *
     * @see AbstractWebdavServlet#getLocatorFactory()
      */
     public DavLocatorFactory getLocatorFactory() {
        if (locatorFactory == null) {
            locatorFactory = new DavLocatorFactoryImpl(pathPrefix);
        }
         return locatorFactory;
     }
 
     /**
     * {@inheritDoc}
     * Sets the <code>DavLocatorFactory</code>
     *
     * @see AbstractWebdavServlet#setLocatorFactory(DavLocatorFactory)
     */
    public void setLocatorFactory(DavLocatorFactory locatorFactory) {
        this.locatorFactory = locatorFactory;
    }

    /**
     * Returns the <code>DavResourceFactory</code>. 
     *
     * @see AbstractWebdavServlet#getResourceFactory()
      */
     public DavResourceFactory getResourceFactory() {
        if (resourceFactory == null) {
            resourceFactory = new DavResourceFactoryImpl(txMgr, subscriptionMgr);
        }
         return resourceFactory;
     }
 
    /**
     * Sets the <code>DavResourceFactory</code>.
     *
     * @see AbstractWebdavServlet#setResourceFactory(org.apache.jackrabbit.webdav.DavResourceFactory)
     */
    public void setResourceFactory(DavResourceFactory resourceFactory) {
        this.resourceFactory = resourceFactory;
    }

     /**
      * Returns {@link #DEFAULT_AUTHENTICATE_HEADER}.
      *
diff --git a/contrib/jcr-server/webapp/src/java/org/apache/jackrabbit/j2ee/RepositoryStartupServlet.java b/contrib/jcr-server/webapp/src/java/org/apache/jackrabbit/j2ee/RepositoryStartupServlet.java
index d36f9ea06..d4c40186b 100644
-- a/contrib/jcr-server/webapp/src/java/org/apache/jackrabbit/j2ee/RepositoryStartupServlet.java
++ b/contrib/jcr-server/webapp/src/java/org/apache/jackrabbit/j2ee/RepositoryStartupServlet.java
@@ -98,9 +98,14 @@ public class RepositoryStartupServlet extends HttpServlet {
 	} else {
 	    log.info("RepositoryStartupServlet shutting down...");
 	}
        shutdownRepository();
 	unregisterRMI();
 	unregisterJNDI();
	log("RepositoryStartupServlet shut down.");
        if (log == null) {
            log("RepositoryStartupServlet shut down.");
        } else {
            log.info("RepositoryStartupServlet shut down.");
        }
     }
 
     /**
@@ -179,6 +184,12 @@ public class RepositoryStartupServlet extends HttpServlet {
 	}
     }
 
    private void shutdownRepository() {
        if (repository instanceof RepositoryImpl) {
            ((RepositoryImpl) repository).shutdown();
            repository = null;
        }
    }
     /**
      * Creates the repository for the given config and homedir.
      *
diff --git a/contrib/jcr-server/webapp/src/java/org/apache/jackrabbit/j2ee/SimpleWebdavServlet.java b/contrib/jcr-server/webapp/src/java/org/apache/jackrabbit/j2ee/SimpleWebdavServlet.java
index 47ec11b9c..575e361a8 100644
-- a/contrib/jcr-server/webapp/src/java/org/apache/jackrabbit/j2ee/SimpleWebdavServlet.java
++ b/contrib/jcr-server/webapp/src/java/org/apache/jackrabbit/j2ee/SimpleWebdavServlet.java
@@ -118,12 +118,12 @@ public class SimpleWebdavServlet extends AbstractWebdavServlet {
     private Repository repository;
 
     /**
     * the session provider
     * the webdav session provider
      */
     private DavSessionProvider davSessionProvider;
 
     /**
     * the session provider
     * the repository session provider
      */
     private SessionProvider sessionProvider;
 
@@ -145,11 +145,6 @@ public class SimpleWebdavServlet extends AbstractWebdavServlet {
         }
         log.info(INIT_PARAM_RESOURCE_PATH_PREFIX + " = '" + resourcePathPrefix + "'");
 
        // init repository
        repository = RepositoryAccessServlet.getRepository();
        if (repository == null) {
            throw new ServletException("Repository could not be retrieved. Check config of 'RepositoryAccessServlet'.");
        }
         try {
             String chain = getInitParameter(INIT_PARAM_CHAIN_CATALOG);
             URL chainUrl = getServletContext().getResource(chain);
@@ -276,6 +271,7 @@ public class SimpleWebdavServlet extends AbstractWebdavServlet {
      * returned.
      *
      * @return the locator factory
     * @see AbstractWebdavServlet#getLocatorFactory()
      */
     public DavLocatorFactory getLocatorFactory() {
         if (locatorFactory == null) {
@@ -284,6 +280,16 @@ public class SimpleWebdavServlet extends AbstractWebdavServlet {
         return locatorFactory;
     }
 
    /**
     * Sets the <code>DavLocatorFactory</code>.
     *
     * @param locatorFactory
     * @see AbstractWebdavServlet#setLocatorFactory(DavLocatorFactory)
     */
    public void setLocatorFactory(DavLocatorFactory locatorFactory) {
        this.locatorFactory = locatorFactory;
    }

     /**
      * Returns the <code>LockManager</code>. If no lock manager has
      * been set or created a new instance of {@link SimpleLockManager} is
@@ -298,12 +304,22 @@ public class SimpleWebdavServlet extends AbstractWebdavServlet {
         return lockManager;
     }
 
    /**
     * Sets the <code>LockManager</code>.
     *
     * @param lockManager
     */
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

     /**
      * Returns the <code>DavResourceFactory</code>. If no request factory has
      * been set or created a new instance of {@link ResourceFactoryImpl} is
      * returned.
      *
      * @return the resource factory
     * @see org.apache.jackrabbit.server.AbstractWebdavServlet#getResourceFactory()
      */
     public DavResourceFactory getResourceFactory() {
         if (resourceFactory == null) {
@@ -313,23 +329,25 @@ public class SimpleWebdavServlet extends AbstractWebdavServlet {
     }
 
     /**
     * Returns the header value retrieved from the {@link #INIT_PARAM_AUTHENTICATE_HEADER}
     * init parameter. If the parameter is missing, the value defaults to
     * {@link #DEFAULT_AUTHENTICATE_HEADER}.
     * Sets the <code>DavResourceFactory</code>.
      *
     * @return the header value retrieved from the corresponding init parameter
     * or {@link #DEFAULT_AUTHENTICATE_HEADER}.
     * @param resourceFactory
     * @see AbstractWebdavServlet#setResourceFactory(org.apache.jackrabbit.webdav.DavResourceFactory)
      */
    public String getAuthenticateHeaderValue() {
        return authenticate_header;
    public void setResourceFactory(DavResourceFactory resourceFactory) {
        this.resourceFactory = resourceFactory;
     }
 
     /**
     * Returns the <code>DavSessionProvider</code>.
     * Returns the <code>SessionProvider</code>. If no session provider has been
     * set or created a new instance of {@link SessionProviderImpl} that extracts
     * credentials from the request's <code>Authorization</code> header is
     * returned.
      *
      * @return the session provider
     * @see RepositoryAccessServlet#getCredentialsFromHeader(String)
      */
    public synchronized SessionProvider getRepositorySessionProvider() {
    public synchronized SessionProvider getSessionProvider() {
         if (sessionProvider == null) {
             CredentialsProvider cp = new CredentialsProvider() {
                 public Credentials getCredentials(HttpServletRequest request) throws LoginException, ServletException {
@@ -342,16 +360,77 @@ public class SimpleWebdavServlet extends AbstractWebdavServlet {
     }
 
     /**
     * Returns the <code>DavSessionProvider</code>.
     * Sets the <code>SessionProvider</code>.
     *
     * @param sessionProvider
     */
    public synchronized void setSessionProvider(SessionProvider sessionProvider) {
        this.sessionProvider = sessionProvider;
    }

    /**
     * Returns the <code>DavSessionProvider</code>. If no session provider has
     * been set or created a new instance of {@link DavSessionProviderImpl}
     * is returned.
      *
      * @return the session provider
     * @see org.apache.jackrabbit.server.AbstractWebdavServlet#getDavSessionProvider()
      */
    public synchronized DavSessionProvider getSessionProvider() {
    public synchronized DavSessionProvider getDavSessionProvider() {
         if (davSessionProvider == null) {
             davSessionProvider =
                    new DavSessionProviderImpl(repository, getRepositorySessionProvider());
                new DavSessionProviderImpl(getRepository(), getSessionProvider());
         }
         return davSessionProvider;
     }
 
    /**
     * Sets the <code>DavSessionProvider</code>.
     *
     * @param sessionProvider
     * @see AbstractWebdavServlet#setDavSessionProvider(org.apache.jackrabbit.webdav.DavSessionProvider)
     */
    public synchronized void setDavSessionProvider(DavSessionProvider sessionProvider) {
        this.davSessionProvider = sessionProvider;
    }

    /**
     * Returns the header value retrieved from the {@link #INIT_PARAM_AUTHENTICATE_HEADER}
     * init parameter. If the parameter is missing, the value defaults to
     * {@link #DEFAULT_AUTHENTICATE_HEADER}.
     *
     * @return the header value retrieved from the corresponding init parameter
     * or {@link #DEFAULT_AUTHENTICATE_HEADER}.
     * @see org.apache.jackrabbit.server.AbstractWebdavServlet#getAuthenticateHeaderValue()
     */
    public String getAuthenticateHeaderValue() {
        return authenticate_header;
    }

    /**
     * Returns the <code>Repository</code>. If no repository has been set or
     * created the repository initialized by <code>RepositoryAccessServlet</code>
     * is returned.
     *
     * @return repository
     * @see RepositoryAccessServlet#getRepository()
     */
    public Repository getRepository() {
        if (repository == null) {
            repository = RepositoryAccessServlet.getRepository();
            if (repository == null) {
                throw new IllegalStateException("Repository could not be retrieved. Check config of 'RepositoryAccessServlet'.");
            }
        }
        return repository;
    }

    /**
     * Sets the <code>Repository</code>.
     *
     * @param repository
     */
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
 }
diff --git a/contrib/jcr-server/webdav/src/java/org/apache/jackrabbit/webdav/lock/SimpleLockManager.java b/contrib/jcr-server/webdav/src/java/org/apache/jackrabbit/webdav/lock/SimpleLockManager.java
index 147a32d72..8da52ba05 100644
-- a/contrib/jcr-server/webdav/src/java/org/apache/jackrabbit/webdav/lock/SimpleLockManager.java
++ b/contrib/jcr-server/webdav/src/java/org/apache/jackrabbit/webdav/lock/SimpleLockManager.java
@@ -23,8 +23,6 @@ import org.apache.jackrabbit.util.Text;
 
 /**
  * Simple manager for webdav locks.<br>
 * NOTE: the timeout requested is always replace by a infinite timeout and
 * expiration of locks is not checked.
  */
 public class SimpleLockManager implements LockManager {
 
@@ -55,33 +53,37 @@ public class SimpleLockManager implements LockManager {
      * @param resource
      * @return lock that applies to the given resource or <code>null</code>.
      */
    public ActiveLock getLock(Type type, Scope scope, DavResource resource) {
    public synchronized ActiveLock getLock(Type type, Scope scope, DavResource resource) {
 	if (!(Type.WRITE.equals(type) && Scope.EXCLUSIVE.equals(scope))) {
 	    return null;
 	}
	String key = resource.getResourcePath();
	ActiveLock lock = (locks.containsKey(key)) ? (ActiveLock)locks.get(key) : null;
        return getLock(resource.getResourcePath());
    }
 
	// look for an inherited lock
	if (lock == null) {
	    // cut path instead of retrieving the parent resource
	    String parentPath = Text.getRelativeParent(key, 1);
	    boolean found = false;
	    /* stop as soon as parent lock is found:
	    if the lock is deep or the parent is a collection the lock
	    applies to the given resource. */
	    while (!"/".equals(parentPath) && !(found = locks.containsKey(parentPath))) {
		parentPath = Text.getRelativeParent(parentPath, 1);
	    }
	    if (found) {
		ActiveLock parentLock = (ActiveLock)locks.get(parentPath);
		if (parentLock.isDeep()) {
		    lock = parentLock;
		}
	    }
	}
	// since locks have infinite timeout, check for expired lock is omitted.
	return lock;
    /**
     * Recursivly tries to find the lock
     *
     * @param path
     * @return
     */
    private ActiveLock getLock(String path) {
	ActiveLock lock = (ActiveLock) locks.get(path);
        if (lock != null) {
            // check if not expired
            if (lock.isExpired()) {
                lock = null;
            }
        }
        if (lock == null) {
            // check, if child of deep locked parent
            if (!path.equals("/")) {
                ActiveLock parentLock = getLock(Text.getRelativeParent(path, 1));
                if (parentLock != null && parentLock.isDeep()) {
                    lock = parentLock;
                }
            }
        }
        return lock;
     }
 
     /**
@@ -90,7 +92,8 @@ public class SimpleLockManager implements LockManager {
      * @param lockInfo
      * @param resource being the lock holder
      */
    public synchronized ActiveLock createLock(LockInfo lockInfo, DavResource resource)
    public synchronized ActiveLock createLock(LockInfo lockInfo,
                                              DavResource resource)
 	    throws DavException {
 	if (lockInfo == null || resource == null) {
 	    throw new IllegalArgumentException("Neither lockInfo nor resource must be null.");
@@ -98,7 +101,12 @@ public class SimpleLockManager implements LockManager {
 
 	String resourcePath = resource.getResourcePath();
 	// test if there is already a lock present on this resource
	if (locks.containsKey(resourcePath)) {
        ActiveLock lock = (ActiveLock) locks.get(resourcePath);
        if (lock != null && lock.isExpired()) {
            locks.remove(resourcePath);
            lock = null;
        }
        if (lock != null) {
 	    throw new DavException(DavServletResponse.SC_LOCKED, "Resource '" + resource.getResourcePath() + "' already holds a lock.");
 	}
 	// test if the new lock would conflict with any lock inherited from the
@@ -119,10 +127,7 @@ public class SimpleLockManager implements LockManager {
 
 	    }
 	}
	ActiveLock lock = new DefaultActiveLock(lockInfo);
	// Lazy: reset the timeout to 'Infinite', in order to omit the tests for
	// lock expiration.
	lock.setTimeout(DavConstants.INFINITE_TIMEOUT);
	lock = new DefaultActiveLock(lockInfo);
 	locks.put(resource.getResourcePath(), lock);
 	return lock;
     }
@@ -138,13 +143,13 @@ public class SimpleLockManager implements LockManager {
      */
     public ActiveLock refreshLock(LockInfo lockInfo, String lockToken, DavResource resource)
 	    throws DavException {
	// timeout is always infinite > no test for expiration or adjusting timeout needed.
 	ActiveLock lock = getLock(lockInfo.getType(), lockInfo.getScope(), resource);
 	if (lock == null) {
 	    throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED);
 	} else if (!lock.getToken().equals(lockToken)) {
 	    throw new DavException(DavServletResponse.SC_LOCKED);
 	}
        lock.setTimeout(lockInfo.getTimeout());
 	return lock;
     }
 
@@ -159,8 +164,6 @@ public class SimpleLockManager implements LockManager {
 	if (!locks.containsKey(resource.getResourcePath())) {
 	    throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED);
 	}
	// since locks have infinite timeout, check for expiration is omitted.

 	ActiveLock lock = (ActiveLock) locks.get(resource.getResourcePath());
 	if (lock.getToken().equals(lockToken)) {
 	    locks.remove(resource.getResourcePath());
- 
2.19.1.windows.1

