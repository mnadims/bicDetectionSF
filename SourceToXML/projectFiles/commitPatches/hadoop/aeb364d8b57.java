From aeb364d8b57f4de79d3489bef2071cea9c0c840e Mon Sep 17 00:00:00 2001
From: Robert Joseph Evans <bobby@apache.org>
Date: Thu, 23 Aug 2012 18:06:56 +0000
Subject: [PATCH] HADOOP-8225. DistCp fails when invoked by Oozie (daryn via
 bobby)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1376618 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |  2 +
 .../apache/hadoop/security/Credentials.java   |  6 ++
 .../hadoop/security/UserGroupInformation.java | 80 +++++++++++++++---
 .../hadoop/security/TestCredentials.java      | 19 ++++-
 .../security/TestUserGroupInformation.java    | 83 ++++++++++++++++++-
 .../org/apache/hadoop/mapred/YarnChild.java   | 41 ++-------
 .../hadoop/mapreduce/v2/app/MRAppMaster.java  | 12 +--
 .../java/org/apache/hadoop/mapreduce/Job.java |  2 +
 .../org/apache/hadoop/mapreduce/TestJob.java  | 19 ++++-
 .../java/org/apache/hadoop/tools/DistCp.java  |  6 +-
 10 files changed, 206 insertions(+), 64 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index f2371dc1e99..2cc443f19a2 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -964,6 +964,8 @@ Release 0.23.3 - UNRELEASED
     HADOOP-8611. Allow fall-back to the shell-based implementation when 
     JNI-based users-group mapping fails (Robert Parker via bobby) 
 
    HADOOP-8225. DistCp fails when invoked by Oozie (daryn via bobby)

 Release 0.23.2 - UNRELEASED 
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/Credentials.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/Credentials.java
index a258c7f88ca..6d5b048c8a4 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/Credentials.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/Credentials.java
@@ -274,4 +274,10 @@ private void addAll(Credentials other, boolean overwrite) {
       }
     }
   }
  
  public void addTokensToUGI(UserGroupInformation ugi) {
    for (Map.Entry<Text, Token<?>> token: tokenMap.entrySet()) {
      ugi.addToken(token.getKey(), token.getValue());
    }
  }
 }
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
index 23b333b367b..967f0df89ff 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
@@ -55,6 +55,7 @@
 import org.apache.hadoop.classification.InterfaceStability;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
 import org.apache.hadoop.metrics2.annotation.Metric;
 import org.apache.hadoop.metrics2.annotation.Metrics;
 import org.apache.hadoop.metrics2.lib.DefaultMetricsSystem;
@@ -646,9 +647,7 @@ static UserGroupInformation getLoginUser() throws IOException {
           // user.
           Credentials cred = Credentials.readTokenStorageFile(
               new Path("file:///" + fileLocation), conf);
          for (Token<?> token: cred.getAllTokens()) {
            loginUser.addToken(token);
          }
          cred.addTokensToUGI(loginUser);
         }
         loginUser.spawnAutoRenewalThreadForUserCreds();
       } catch (LoginException le) {
@@ -1177,6 +1176,41 @@ public synchronized boolean addTokenIdentifier(TokenIdentifier tokenId) {
   public synchronized Set<TokenIdentifier> getTokenIdentifiers() {
     return subject.getPublicCredentials(TokenIdentifier.class);
   }

  // wrapper to retain the creds key for the token
  private class NamedToken {
    Text alias;
    Token<? extends TokenIdentifier> token;
    NamedToken(Text alias, Token<? extends TokenIdentifier> token) {
      this.alias = alias;
      this.token = token;
    }
    @Override
    public boolean equals(Object o) {
      boolean equals;
      if (o == this) {
        equals = true;
      } else if (!(o instanceof NamedToken)) {
        equals = false;
      } else {
        Text otherAlias = ((NamedToken)o).alias;
        if (alias == otherAlias) {
          equals = true;
        } else {
          equals = (otherAlias != null && otherAlias.equals(alias));
        }
      }
      return equals;
    }
    @Override
    public int hashCode() {
      return (alias != null) ? alias.hashCode() : -1; 
    }
    @Override
    public String toString() {
      return "NamedToken: alias="+alias+" token="+token;
    }
  }
   
   /**
    * Add a token to this UGI
@@ -1185,7 +1219,22 @@ public synchronized boolean addTokenIdentifier(TokenIdentifier tokenId) {
    * @return true on successful add of new token
    */
   public synchronized boolean addToken(Token<? extends TokenIdentifier> token) {
    return subject.getPrivateCredentials().add(token);
    return addToken(token.getService(), token);
  }

  /**
   * Add a named token to this UGI
   * 
   * @param alias Name of the token
   * @param token Token to be added
   * @return true on successful add of new token
   */
  public synchronized boolean addToken(Text alias,
                                       Token<? extends TokenIdentifier> token) {
    NamedToken namedToken = new NamedToken(alias, token);
    Collection<Object> ugiCreds = subject.getPrivateCredentials();
    ugiCreds.remove(namedToken); // allow token to be replaced
    return ugiCreds.add(new NamedToken(alias, token));
   }
   
   /**
@@ -1195,14 +1244,23 @@ public synchronized boolean addToken(Token<? extends TokenIdentifier> token) {
    */
   public synchronized
   Collection<Token<? extends TokenIdentifier>> getTokens() {
    Set<Object> creds = subject.getPrivateCredentials();
    List<Token<?>> result = new ArrayList<Token<?>>(creds.size());
    for(Object o: creds) {
      if (o instanceof Token<?>) {
        result.add((Token<?>) o);
      }
    return Collections.unmodifiableList(
        new ArrayList<Token<?>>(getCredentials().getAllTokens()));
  }

  /**
   * Obtain the tokens in credentials form associated with this user.
   * 
   * @return Credentials of tokens associated with this user
   */
  public synchronized Credentials getCredentials() {
    final Credentials credentials = new Credentials();
    final Set<NamedToken> namedTokens =
        subject.getPrivateCredentials(NamedToken.class);
    for (final NamedToken namedToken : namedTokens) {
      credentials.addToken(namedToken.alias, namedToken.token);
     }
    return Collections.unmodifiableList(result);
    return credentials;
   }
 
   /**
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestCredentials.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestCredentials.java
index 56b5c32521d..d432623be02 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestCredentials.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestCredentials.java
@@ -213,5 +213,22 @@ public void mergeAll() {
     // new token & secret should be added
     assertEquals(token[2], creds.getToken(service[2]));
     assertEquals(secret[2], new Text(creds.getSecretKey(secret[2])));
 }
  }
  
  @Test
  public void testAddTokensToUGI() {
    UserGroupInformation ugi = UserGroupInformation.createRemoteUser("someone");
    Credentials creds = new Credentials();
    
    for (int i=0; i < service.length; i++) {
      creds.addToken(service[i], token[i]);
    }
    creds.addTokensToUGI(ugi);

    creds = ugi.getCredentials();
    for (int i=0; i < service.length; i++) {
      assertSame(token[i], creds.getToken(service[i]));
    }
    assertEquals(service.length, creds.numberOfTokens());
  }
 }
\ No newline at end of file
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java
index cb6f889e8e2..4d8224b7cca 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java
@@ -19,8 +19,7 @@
 import static org.junit.Assert.*;
 import org.junit.*;
 
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.*;
 
 import java.io.BufferedReader;
 import java.io.IOException;
@@ -35,6 +34,7 @@
 import javax.security.auth.login.LoginContext;
 
 import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
 import org.apache.hadoop.metrics2.MetricsRecordBuilder;
 import org.apache.hadoop.security.UserGroupInformation.AuthenticationMethod;
 import org.apache.hadoop.security.token.Token;
@@ -194,8 +194,6 @@ public void testEquals() throws Exception {
   public void testEqualsWithRealUser() throws Exception {
     UserGroupInformation realUgi1 = UserGroupInformation.createUserForTesting(
         "RealUser", GROUP_NAMES);
    UserGroupInformation realUgi2 = UserGroupInformation.createUserForTesting(
        "RealUser", GROUP_NAMES);
     UserGroupInformation proxyUgi1 = UserGroupInformation.createProxyUser(
         USER_NAME, realUgi1);
     UserGroupInformation proxyUgi2 =
@@ -213,7 +211,82 @@ public void testGettingGroups() throws Exception {
     assertArrayEquals(new String[]{GROUP1_NAME, GROUP2_NAME, GROUP3_NAME},
                       uugi.getGroupNames());
   }

  @SuppressWarnings("unchecked") // from Mockito mocks
  @Test
  public <T extends TokenIdentifier> void testAddToken() throws Exception {
    UserGroupInformation ugi = 
        UserGroupInformation.createRemoteUser("someone"); 
    
    Token<T> t1 = mock(Token.class);
    Token<T> t2 = mock(Token.class);
    Token<T> t3 = mock(Token.class);
    
    // add token to ugi
    ugi.addToken(t1);
    checkTokens(ugi, t1);

    // replace token t1 with t2 - with same key (null)
    ugi.addToken(t2);
    checkTokens(ugi, t2);
    
    // change t1 service and add token
    when(t1.getService()).thenReturn(new Text("t1"));
    ugi.addToken(t1);
    checkTokens(ugi, t1, t2);
  
    // overwrite t1 token with t3 - same key (!null)
    when(t3.getService()).thenReturn(new Text("t1"));
    ugi.addToken(t3);
    checkTokens(ugi, t2, t3);

    // just try to re-add with new name
    when(t1.getService()).thenReturn(new Text("t1.1"));
    ugi.addToken(t1);
    checkTokens(ugi, t1, t2, t3);    

    // just try to re-add with new name again
    ugi.addToken(t1);
    checkTokens(ugi, t1, t2, t3);    
  }
   
  private void checkTokens(UserGroupInformation ugi, Token<?> ... tokens) {
    // check the ugi's token collection
    Collection<Token<?>> ugiTokens = ugi.getTokens();
    for (Token<?> t : tokens) {
      assertTrue(ugiTokens.contains(t));
    }
    assertEquals(tokens.length, ugiTokens.size());

    // check the ugi's credentials
    Credentials ugiCreds = ugi.getCredentials();
    for (Token<?> t : tokens) {
      assertSame(t, ugiCreds.getToken(t.getService()));
    }
    assertEquals(tokens.length, ugiCreds.numberOfTokens());
  }

  @SuppressWarnings("unchecked") // from Mockito mocks
  @Test
  public <T extends TokenIdentifier> void testAddNamedToken() throws Exception {
    UserGroupInformation ugi = 
        UserGroupInformation.createRemoteUser("someone"); 
    
    Token<T> t1 = mock(Token.class);
    Text service1 = new Text("t1");
    Text service2 = new Text("t2");
    when(t1.getService()).thenReturn(service1);
    
    // add token
    ugi.addToken(service1, t1);
    assertSame(t1, ugi.getCredentials().getToken(service1));

    // add token with another name
    ugi.addToken(service2, t1);
    assertSame(t1, ugi.getCredentials().getToken(service1));
    assertSame(t1, ugi.getCredentials().getToken(service2));
  }

   @SuppressWarnings("unchecked") // from Mockito mocks
   @Test
   public <T extends TokenIdentifier> void testUGITokens() throws Exception {
@@ -221,7 +294,9 @@ public void testGettingGroups() throws Exception {
       UserGroupInformation.createUserForTesting("TheDoctor", 
                                                 new String [] { "TheTARDIS"});
     Token<T> t1 = mock(Token.class);
    when(t1.getService()).thenReturn(new Text("t1"));
     Token<T> t2 = mock(Token.class);
    when(t2.getService()).thenReturn(new Text("t2"));
     
     ugi.addToken(t1);
     ugi.addToken(t2);
diff --git a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-app/src/main/java/org/apache/hadoop/mapred/YarnChild.java b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-app/src/main/java/org/apache/hadoop/mapred/YarnChild.java
index 0ca3710fd66..64ac83e8cfd 100644
-- a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-app/src/main/java/org/apache/hadoop/mapred/YarnChild.java
++ b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-app/src/main/java/org/apache/hadoop/mapred/YarnChild.java
@@ -39,7 +39,6 @@
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.fs.permission.FsPermission;
 import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.Text;
 import org.apache.hadoop.ipc.RPC;
 import org.apache.hadoop.mapreduce.MRConfig;
 import org.apache.hadoop.mapreduce.MRJobConfig;
@@ -55,7 +54,6 @@
 import org.apache.hadoop.security.SecurityUtil;
 import org.apache.hadoop.security.UserGroupInformation;
 import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenIdentifier;
 import org.apache.hadoop.util.DiskChecker.DiskErrorException;
 import org.apache.hadoop.util.StringUtils;
 import org.apache.hadoop.yarn.YarnUncaughtExceptionHandler;
@@ -92,11 +90,15 @@ public static void main(String[] args) throws Throwable {
     DefaultMetricsSystem.initialize(
         StringUtils.camelize(firstTaskid.getTaskType().name()) +"Task");
 
    Token<JobTokenIdentifier> jt = loadCredentials(defaultConf, address);

    // Security framework already loaded the tokens into current ugi
    Credentials credentials =
        UserGroupInformation.getCurrentUser().getCredentials();
    
     // Create TaskUmbilicalProtocol as actual task owner.
     UserGroupInformation taskOwner =
       UserGroupInformation.createRemoteUser(firstTaskid.getJobID().toString());
    Token<JobTokenIdentifier> jt = TokenCache.getJobToken(credentials);
    SecurityUtil.setTokenService(jt, address);
     taskOwner.addToken(jt);
     final TaskUmbilicalProtocol umbilical =
       taskOwner.doAs(new PrivilegedExceptionAction<TaskUmbilicalProtocol>() {
@@ -132,17 +134,14 @@ public TaskUmbilicalProtocol run() throws Exception {
       YarnChild.taskid = task.getTaskID();
 
       // Create the job-conf and set credentials
      final JobConf job =
        configureTask(task, defaultConf.getCredentials(), jt);
      final JobConf job = configureTask(task, credentials, jt);
 
       // Initiate Java VM metrics
       JvmMetrics.initSingleton(jvmId.toString(), job.getSessionId());
       childUGI = UserGroupInformation.createRemoteUser(System
           .getenv(ApplicationConstants.Environment.USER.toString()));
       // Add tokens to new user so that it may execute its task correctly.
      for(Token<?> token : UserGroupInformation.getCurrentUser().getTokens()) {
        childUGI.addToken(token);
      }
      job.getCredentials().addTokensToUGI(childUGI);
 
       // Create a final reference to the task for the doAs block
       final Task taskFinal = task;
@@ -206,30 +205,6 @@ public Object run() throws Exception {
     }
   }
 
  private static Token<JobTokenIdentifier> loadCredentials(JobConf conf,
      InetSocketAddress address) throws IOException {
    //load token cache storage
    String tokenFileLocation =
        System.getenv(ApplicationConstants.CONTAINER_TOKEN_FILE_ENV_NAME);
    String jobTokenFile =
        new Path(tokenFileLocation).makeQualified(FileSystem.getLocal(conf))
            .toUri().getPath();
    Credentials credentials =
      TokenCache.loadTokens(jobTokenFile, conf);
    LOG.debug("loading token. # keys =" +credentials.numberOfSecretKeys() +
        "; from file=" + jobTokenFile);
    Token<JobTokenIdentifier> jt = TokenCache.getJobToken(credentials);
    SecurityUtil.setTokenService(jt, address);
    UserGroupInformation current = UserGroupInformation.getCurrentUser();
    current.addToken(jt);
    for (Token<? extends TokenIdentifier> tok : credentials.getAllTokens()) {
      current.addToken(tok);
    }
    // Set the credentials
    conf.setCredentials(credentials);
    return jt;
  }

   /**
    * Configure mapred-local dirs. This config is used by the task for finding
    * out an output directory.
diff --git a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-app/src/main/java/org/apache/hadoop/mapreduce/v2/app/MRAppMaster.java b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-app/src/main/java/org/apache/hadoop/mapreduce/v2/app/MRAppMaster.java
index e81a4237fb2..463a3edec69 100644
-- a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-app/src/main/java/org/apache/hadoop/mapreduce/v2/app/MRAppMaster.java
++ b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-app/src/main/java/org/apache/hadoop/mapreduce/v2/app/MRAppMaster.java
@@ -87,8 +87,6 @@
 import org.apache.hadoop.metrics2.lib.DefaultMetricsSystem;
 import org.apache.hadoop.security.Credentials;
 import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenIdentifier;
 import org.apache.hadoop.util.ReflectionUtils;
 import org.apache.hadoop.util.ShutdownHookManager;
 import org.apache.hadoop.yarn.Clock;
@@ -489,15 +487,7 @@ protected void downloadTokensAndSetupUGI(Configuration conf) {
         fsTokens.addAll(Credentials.readTokenStorageFile(jobTokenFile, conf));
         LOG.info("jobSubmitDir=" + jobSubmitDir + " jobTokenFile="
             + jobTokenFile);

        for (Token<? extends TokenIdentifier> tk : fsTokens.getAllTokens()) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Token of kind " + tk.getKind()
                + "in current ugi in the AppMaster for service "
                + tk.getService());
          }
          currentUser.addToken(tk); // For use by AppMaster itself.
        }
        fsTokens.addTokensToUGI(currentUser); // For use by AppMaster itself.
       }
     } catch (IOException e) {
       throw new YarnException(e);
diff --git a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/main/java/org/apache/hadoop/mapreduce/Job.java b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/main/java/org/apache/hadoop/mapreduce/Job.java
index a2a59005b9d..abbe1e194cc 100644
-- a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/main/java/org/apache/hadoop/mapreduce/Job.java
++ b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/main/java/org/apache/hadoop/mapreduce/Job.java
@@ -131,6 +131,8 @@ public Job(Configuration conf, String jobName) throws IOException {
 
   Job(JobConf conf) throws IOException {
     super(conf, null);
    // propagate existing user credentials to job
    this.credentials.mergeAll(this.ugi.getCredentials());
     this.cluster = null;
   }
 
diff --git a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/test/java/org/apache/hadoop/mapreduce/TestJob.java b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/test/java/org/apache/hadoop/mapreduce/TestJob.java
index 110acba2080..6d2f5e6b692 100644
-- a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/test/java/org/apache/hadoop/mapreduce/TestJob.java
++ b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/test/java/org/apache/hadoop/mapreduce/TestJob.java
@@ -18,14 +18,17 @@
 
 package org.apache.hadoop.mapreduce;
 
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
 
 import java.io.IOException;
 
import org.apache.hadoop.io.Text;
 import org.apache.hadoop.mapred.JobConf;
 import org.apache.hadoop.mapreduce.JobStatus.State;
 import org.apache.hadoop.mapreduce.protocol.ClientProtocol;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
 import org.junit.Assert;
 import org.junit.Test;
 
@@ -50,4 +53,16 @@ public void testJobToString() throws IOException, InterruptedException {
     Assert.assertNotNull(job.toString());
   }
 
  @Test
  public void testUGICredentialsPropogation() throws Exception {
    Token<?> token = mock(Token.class);
    Text service = new Text("service");
    
    UserGroupInformation ugi = UserGroupInformation.getCurrentUser();
    ugi.addToken(service, token);
    
    JobConf jobConf = new JobConf();
    Job job = new Job(jobConf);
    assertSame(token, job.getCredentials().getToken(service));
  }
 }
diff --git a/hadoop-tools/hadoop-distcp/src/main/java/org/apache/hadoop/tools/DistCp.java b/hadoop-tools/hadoop-distcp/src/main/java/org/apache/hadoop/tools/DistCp.java
index 523609b5327..416847b821d 100644
-- a/hadoop-tools/hadoop-distcp/src/main/java/org/apache/hadoop/tools/DistCp.java
++ b/hadoop-tools/hadoop-distcp/src/main/java/org/apache/hadoop/tools/DistCp.java
@@ -359,18 +359,20 @@ private Path createMetaFolderPath() throws Exception {
    * @param argv Command-line arguments sent to DistCp.
    */
   public static void main(String argv[]) {
    int exitCode;
     try {
       DistCp distCp = new DistCp();
       Cleanup CLEANUP = new Cleanup(distCp);
 
       ShutdownHookManager.get().addShutdownHook(CLEANUP,
         SHUTDOWN_HOOK_PRIORITY);
      System.exit(ToolRunner.run(getDefaultConf(), distCp, argv));
      exitCode = ToolRunner.run(getDefaultConf(), distCp, argv);
     }
     catch (Exception e) {
       LOG.error("Couldn't complete DistCp operation: ", e);
      System.exit(DistCpConstants.UNKNOWN_ERROR);
      exitCode = DistCpConstants.UNKNOWN_ERROR;
     }
    System.exit(exitCode);
   }
 
   /**
- 
2.19.1.windows.1

