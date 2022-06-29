From 90e07d55ace7221081a58a90e54b360ad68fa1ef Mon Sep 17 00:00:00 2001
From: Haohui Mai <wheat9@apache.org>
Date: Mon, 30 Mar 2015 11:44:22 -0700
Subject: [PATCH] HADOOP-11754. RM fails to start in non-secure mode due to
 authentication filter failure. Contributed by Haohui Mai.

--
 .../server/AuthenticationFilter.java          | 108 ++++++++----------
 .../server/TestAuthenticationFilter.java      |  20 ++--
 .../hadoop-common/CHANGES.txt                 |   3 +
 .../org/apache/hadoop/http/HttpServer2.java   |  53 ++++++++-
 .../AuthenticationFilterInitializer.java      |  18 ++-
 5 files changed, 128 insertions(+), 74 deletions(-)

diff --git a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/AuthenticationFilter.java b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/AuthenticationFilter.java
index 5c22fce0245..684e91c2bc9 100644
-- a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/AuthenticationFilter.java
++ b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/AuthenticationFilter.java
@@ -25,6 +25,7 @@
 import javax.servlet.Filter;
 import javax.servlet.FilterChain;
 import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
 import javax.servlet.ServletException;
 import javax.servlet.ServletRequest;
 import javax.servlet.ServletResponse;
@@ -183,8 +184,6 @@
   private Signer signer;
   private SignerSecretProvider secretProvider;
   private AuthenticationHandler authHandler;
  private boolean randomSecret;
  private boolean customSecretProvider;
   private long validity;
   private String cookieDomain;
   private String cookiePath;
@@ -226,7 +225,6 @@ public void init(FilterConfig filterConfig) throws ServletException {
 
     initializeAuthHandler(authHandlerClassName, filterConfig);
 

     cookieDomain = config.getProperty(COOKIE_DOMAIN, null);
     cookiePath = config.getProperty(COOKIE_PATH, null);
   }
@@ -237,11 +235,8 @@ protected void initializeAuthHandler(String authHandlerClassName, FilterConfig f
       Class<?> klass = Thread.currentThread().getContextClassLoader().loadClass(authHandlerClassName);
       authHandler = (AuthenticationHandler) klass.newInstance();
       authHandler.init(config);
    } catch (ClassNotFoundException ex) {
      throw new ServletException(ex);
    } catch (InstantiationException ex) {
      throw new ServletException(ex);
    } catch (IllegalAccessException ex) {
    } catch (ClassNotFoundException | InstantiationException |
        IllegalAccessException ex) {
       throw new ServletException(ex);
     }
   }
@@ -251,62 +246,59 @@ protected void initializeSecretProvider(FilterConfig filterConfig)
     secretProvider = (SignerSecretProvider) filterConfig.getServletContext().
         getAttribute(SIGNER_SECRET_PROVIDER_ATTRIBUTE);
     if (secretProvider == null) {
      Class<? extends SignerSecretProvider> providerClass
              = getProviderClass(config);
      try {
        secretProvider = providerClass.newInstance();
      } catch (InstantiationException ex) {
        throw new ServletException(ex);
      } catch (IllegalAccessException ex) {
        throw new ServletException(ex);
      }
      // As tomcat cannot specify the provider object in the configuration.
      // It'll go into this path
       try {
        secretProvider.init(config, filterConfig.getServletContext(), validity);
        secretProvider = constructSecretProvider(
            filterConfig.getServletContext(),
            config, false);
       } catch (Exception ex) {
         throw new ServletException(ex);
       }
    } else {
      customSecretProvider = true;
     }
     signer = new Signer(secretProvider);
   }
 
  @SuppressWarnings("unchecked")
  private Class<? extends SignerSecretProvider> getProviderClass(Properties config)
          throws ServletException {
    String providerClassName;
    String signerSecretProviderName
            = config.getProperty(SIGNER_SECRET_PROVIDER, null);
    // fallback to old behavior
    if (signerSecretProviderName == null) {
      String signatureSecretFile = config.getProperty(
          SIGNATURE_SECRET_FILE, null);
      // The precedence from high to low : file, random
      if (signatureSecretFile != null) {
        providerClassName = FileSignerSecretProvider.class.getName();
      } else {
        providerClassName = RandomSignerSecretProvider.class.getName();
        randomSecret = true;
  public static SignerSecretProvider constructSecretProvider(
      ServletContext ctx, Properties config,
      boolean disallowFallbackToRandomSecretProvider) throws Exception {
    String name = config.getProperty(SIGNER_SECRET_PROVIDER, "file");
    long validity = Long.parseLong(config.getProperty(AUTH_TOKEN_VALIDITY,
                                                      "36000")) * 1000;

    if (!disallowFallbackToRandomSecretProvider
        && "file".equals(name)
        && config.getProperty(SIGNATURE_SECRET_FILE) == null) {
      name = "random";
    }

    SignerSecretProvider provider;
    if ("file".equals(name)) {
      provider = new FileSignerSecretProvider();
      try {
        provider.init(config, ctx, validity);
      } catch (Exception e) {
        if (!disallowFallbackToRandomSecretProvider) {
          LOG.info("Unable to initialize FileSignerSecretProvider, " +
                       "falling back to use random secrets.");
          provider = new RandomSignerSecretProvider();
          provider.init(config, ctx, validity);
        } else {
          throw e;
        }
       }
    } else if ("random".equals(name)) {
      provider = new RandomSignerSecretProvider();
      provider.init(config, ctx, validity);
    } else if ("zookeeper".equals(name)) {
      provider = new ZKSignerSecretProvider();
      provider.init(config, ctx, validity);
     } else {
      if ("random".equals(signerSecretProviderName)) {
        providerClassName = RandomSignerSecretProvider.class.getName();
        randomSecret = true;
      } else if ("file".equals(signerSecretProviderName)) {
        providerClassName = FileSignerSecretProvider.class.getName();
      } else if ("zookeeper".equals(signerSecretProviderName)) {
        providerClassName = ZKSignerSecretProvider.class.getName();
      } else {
        providerClassName = signerSecretProviderName;
        customSecretProvider = true;
      }
    }
    try {
      return (Class<? extends SignerSecretProvider>) Thread.currentThread().
              getContextClassLoader().loadClass(providerClassName);
    } catch (ClassNotFoundException ex) {
      throw new ServletException(ex);
      provider = (SignerSecretProvider) Thread.currentThread().
          getContextClassLoader().loadClass(name).newInstance();
      provider.init(config, ctx, validity);
     }
    return provider;
   }
 
   /**
@@ -335,7 +327,7 @@ protected AuthenticationHandler getAuthenticationHandler() {
    * @return if a random secret is being used.
    */
   protected boolean isRandomSecret() {
    return randomSecret;
    return secretProvider.getClass() == RandomSignerSecretProvider.class;
   }
 
   /**
@@ -344,7 +336,10 @@ protected boolean isRandomSecret() {
    * @return if a custom implementation of a SignerSecretProvider is being used.
    */
   protected boolean isCustomSignerSecretProvider() {
    return customSecretProvider;
    Class<?> clazz = secretProvider.getClass();
    return clazz != FileSignerSecretProvider.class && clazz !=
        RandomSignerSecretProvider.class && clazz != ZKSignerSecretProvider
        .class;
   }
 
   /**
@@ -385,9 +380,6 @@ public void destroy() {
       authHandler.destroy();
       authHandler = null;
     }
    if (secretProvider != null) {
      secretProvider.destroy();
    }
   }
 
   /**
diff --git a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestAuthenticationFilter.java b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestAuthenticationFilter.java
index 26c10a9d31a..63b812d5e35 100644
-- a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestAuthenticationFilter.java
++ b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestAuthenticationFilter.java
@@ -18,7 +18,9 @@
 import java.io.IOException;
 import java.io.Writer;
 import java.net.HttpCookie;
import java.util.ArrayList;
 import java.util.Arrays;
import java.util.Enumeration;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Properties;
@@ -151,8 +153,7 @@ public AuthenticationToken authenticate(HttpServletRequest request, HttpServletR
   }
 
   @Test
  public void testInit() throws Exception {

  public void testFallbackToRandomSecretProvider() throws Exception {
     // minimal configuration & simple auth handler (Pseudo)
     AuthenticationFilter filter = new AuthenticationFilter();
     try {
@@ -162,8 +163,8 @@ public void testInit() throws Exception {
           AuthenticationFilter.AUTH_TOKEN_VALIDITY)).thenReturn(
           (new Long(TOKEN_VALIDITY_SEC)).toString());
       Mockito.when(config.getInitParameterNames()).thenReturn(
          new Vector<String>(Arrays.asList(AuthenticationFilter.AUTH_TYPE,
                                           AuthenticationFilter.AUTH_TOKEN_VALIDITY)).elements());
          new Vector<>(Arrays.asList(AuthenticationFilter.AUTH_TYPE,
                                     AuthenticationFilter.AUTH_TOKEN_VALIDITY)).elements());
       ServletContext context = Mockito.mock(ServletContext.class);
       Mockito.when(context.getAttribute(AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
           .thenReturn(null);
@@ -178,16 +179,17 @@ public void testInit() throws Exception {
     } finally {
       filter.destroy();
     }

  }
  @Test
  public void testInit() throws Exception {
     // custom secret as inline
    filter = new AuthenticationFilter();
    AuthenticationFilter filter = new AuthenticationFilter();
     try {
       FilterConfig config = Mockito.mock(FilterConfig.class);
       Mockito.when(config.getInitParameter(AuthenticationFilter.AUTH_TYPE)).thenReturn("simple");
      Mockito.when(config.getInitParameter(AuthenticationFilter.SIGNATURE_SECRET)).thenReturn("secret");
       Mockito.when(config.getInitParameterNames()).thenReturn(
        new Vector<String>(Arrays.asList(AuthenticationFilter.AUTH_TYPE,
                                 AuthenticationFilter.SIGNATURE_SECRET)).elements());
          new Vector<>(Arrays.asList(AuthenticationFilter.AUTH_TYPE))
              .elements());
       ServletContext context = Mockito.mock(ServletContext.class);
       Mockito.when(context.getAttribute(
           AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE)).thenReturn(
diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 8b5997259c9..b5d23037d35 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -1178,6 +1178,9 @@ Release 2.7.0 - UNRELEASED
     HADOOP-11761. Fix findbugs warnings in org.apache.hadoop.security
     .authentication. (Li Lu via wheat9)
 
    HADOOP-11754. RM fails to start in non-secure mode due to authentication
    filter failure. (wheat9)

 Release 2.6.1 - UNRELEASED
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/http/HttpServer2.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/http/HttpServer2.java
index 566861e046f..0f1c22287e2 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/http/HttpServer2.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/http/HttpServer2.java
@@ -31,6 +31,7 @@
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
import java.util.Properties;
 
 import javax.servlet.Filter;
 import javax.servlet.FilterChain;
@@ -53,6 +54,11 @@
 import org.apache.hadoop.conf.ConfServlet;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.security.AuthenticationFilterInitializer;
import org.apache.hadoop.security.authentication.util.FileSignerSecretProvider;
import org.apache.hadoop.security.authentication.util.RandomSignerSecretProvider;
import org.apache.hadoop.security.authentication.util.SignerSecretProvider;
import org.apache.hadoop.security.authentication.util.ZKSignerSecretProvider;
 import org.apache.hadoop.security.ssl.SslSocketConnectorSecure;
 import org.apache.hadoop.jmx.JMXJsonServlet;
 import org.apache.hadoop.log.LogLevel;
@@ -91,6 +97,8 @@
 import com.google.common.collect.Lists;
 import com.sun.jersey.spi.container.servlet.ServletContainer;
 
import static org.apache.hadoop.security.authentication.server
    .AuthenticationFilter.*;
 /**
  * Create a Jetty embedded server to answer http requests. The primary goal is
  * to serve up status information for the server. There are three contexts:
@@ -160,6 +168,8 @@
     private boolean findPort;
 
     private String hostName;
    private boolean disallowFallbackToRandomSignerSecretProvider;
    private String authFilterConfigurationPrefix = "hadoop.http.authentication.";
 
     public Builder setName(String name){
       this.name = name;
@@ -254,6 +264,16 @@ public Builder setKeytabConfKey(String keytabConfKey) {
       return this;
     }
 
    public Builder disallowFallbackToRandomSingerSecretProvider(boolean value) {
      this.disallowFallbackToRandomSignerSecretProvider = value;
      return this;
    }

    public Builder authFilterConfigurationPrefix(String value) {
      this.authFilterConfigurationPrefix = value;
      return this;
    }

     public HttpServer2 build() throws IOException {
       Preconditions.checkNotNull(name, "name is not set");
       Preconditions.checkState(!endpoints.isEmpty(), "No endpoints specified");
@@ -314,6 +334,18 @@ private HttpServer2(final Builder b) throws IOException {
     this.webServer = new Server();
     this.adminsAcl = b.adminsAcl;
     this.webAppContext = createWebAppContext(b.name, b.conf, adminsAcl, appDir);
    try {
      SignerSecretProvider secretProvider =
          constructSecretProvider(b, webAppContext.getServletContext());
      this.webAppContext.getServletContext().setAttribute
          (AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE,
           secretProvider);
    } catch(IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }

     this.findPort = b.findPort;
     initializeWebServer(b.name, b.hostName, b.conf, b.pathSpecs);
   }
@@ -405,9 +437,28 @@ private static WebAppContext createWebAppContext(String name,
     return ctx;
   }
 
  private static SignerSecretProvider constructSecretProvider(final Builder b,
      ServletContext ctx)
      throws Exception {
    final Configuration conf = b.conf;
    Properties config = getFilterProperties(conf,
                                            b.authFilterConfigurationPrefix);
    return AuthenticationFilter.constructSecretProvider(
        ctx, config, b.disallowFallbackToRandomSignerSecretProvider);
  }

  private static Properties getFilterProperties(Configuration conf, String
      prefix) {
    Properties prop = new Properties();
    Map<String, String> filterConfig = AuthenticationFilterInitializer
        .getFilterConfigMap(conf, prefix);
    prop.putAll(filterConfig);
    return prop;
  }

   private static void addNoCacheFilter(WebAppContext ctxt) {
     defineFilter(ctxt, NO_CACHE_FILTER, NoCacheFilter.class.getName(),
        Collections.<String, String> emptyMap(), new String[] { "/*" });
                 Collections.<String, String> emptyMap(), new String[] { "/*" });
   }
 
   @InterfaceAudience.Private
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/AuthenticationFilterInitializer.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/AuthenticationFilterInitializer.java
index cb3830d3ea2..ca221f5b3dc 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/AuthenticationFilterInitializer.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/AuthenticationFilterInitializer.java
@@ -56,6 +56,15 @@
    */
   @Override
   public void initFilter(FilterContainer container, Configuration conf) {
    Map<String, String> filterConfig = getFilterConfigMap(conf, PREFIX);

    container.addFilter("authentication",
                        AuthenticationFilter.class.getName(),
                        filterConfig);
  }

  public static Map<String, String> getFilterConfigMap(Configuration conf,
      String prefix) {
     Map<String, String> filterConfig = new HashMap<String, String>();
 
     //setting the cookie path to root '/' so it is used for all resources.
@@ -63,9 +72,9 @@ public void initFilter(FilterContainer container, Configuration conf) {
 
     for (Map.Entry<String, String> entry : conf) {
       String name = entry.getKey();
      if (name.startsWith(PREFIX)) {
      if (name.startsWith(prefix)) {
         String value = conf.get(name);
        name = name.substring(PREFIX.length());
        name = name.substring(prefix.length());
         filterConfig.put(name, value);
       }
     }
@@ -82,10 +91,7 @@ public void initFilter(FilterContainer container, Configuration conf) {
       }
       filterConfig.put(KerberosAuthenticationHandler.PRINCIPAL, principal);
     }

    container.addFilter("authentication",
                        AuthenticationFilter.class.getName(),
                        filterConfig);
    return filterConfig;
   }
 
 }
- 
2.19.1.windows.1

