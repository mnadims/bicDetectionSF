From 6f6e170325d39f9f7b543a39791b2cb54692f83d Mon Sep 17 00:00:00 2001
From: Robert Joseph Evans <bobby@apache.org>
Date: Thu, 30 Aug 2012 19:58:07 +0000
Subject: [PATCH] HADOOP-8726. The Secrets in Credentials are not available to
 MR tasks (daryn and Benoy Antony via bobby)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1379100 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |  3 +
 .../apache/hadoop/security/Credentials.java   |  6 --
 .../hadoop/security/UserGroupInformation.java | 75 +++++++------------
 .../hadoop/security/TestCredentials.java      |  2 +-
 .../security/TestUserGroupInformation.java    | 73 ++++++++++++++++++
 .../org/apache/hadoop/mapred/YarnChild.java   |  2 +-
 .../hadoop/mapreduce/v2/app/MRAppMaster.java  |  2 +-
 .../org/apache/hadoop/mapreduce/TestJob.java  | 17 +++--
 8 files changed, 117 insertions(+), 63 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 6bd92f8af68..2b0ba1da2df 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -993,6 +993,9 @@ Release 0.23.3 - UNRELEASED
 
     HADOOP-8725. MR is broken when security is off (daryn via bobby)
 
    HADOOP-8726. The Secrets in Credentials are not available to MR tasks
    (daryn and Benoy Antony via bobby)

 Release 0.23.2 - UNRELEASED 
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/Credentials.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/Credentials.java
index 6d5b048c8a4..a258c7f88ca 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/Credentials.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/Credentials.java
@@ -274,10 +274,4 @@ private void addAll(Credentials other, boolean overwrite) {
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
index 184b40d8ed5..64ca98cf28a 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
@@ -27,7 +27,6 @@
 import java.security.PrivilegedAction;
 import java.security.PrivilegedActionException;
 import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
@@ -646,7 +645,7 @@ static UserGroupInformation getLoginUser() throws IOException {
           // user.
           Credentials cred = Credentials.readTokenStorageFile(
               new Path("file:///" + fileLocation), conf);
          cred.addTokensToUGI(loginUser);
          loginUser.addCredentials(cred);
         }
         loginUser.spawnAutoRenewalThreadForUserCreds();
       } catch (LoginException le) {
@@ -1176,41 +1175,6 @@ public synchronized boolean addTokenIdentifier(TokenIdentifier tokenId) {
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
@@ -1219,7 +1183,7 @@ public String toString() {
    * @return true on successful add of new token
    */
   public synchronized boolean addToken(Token<? extends TokenIdentifier> token) {
    return addToken(token.getService(), token);
    return (token != null) ? addToken(token.getService(), token) : false;
   }
 
   /**
@@ -1231,10 +1195,8 @@ public synchronized boolean addToken(Token<? extends TokenIdentifier> token) {
    */
   public synchronized boolean addToken(Text alias,
                                        Token<? extends TokenIdentifier> token) {
    NamedToken namedToken = new NamedToken(alias, token);
    Collection<Object> ugiCreds = subject.getPrivateCredentials();
    ugiCreds.remove(namedToken); // allow token to be replaced
    return ugiCreds.add(new NamedToken(alias, token));
    getCredentialsInternal().addToken(alias, token);
    return true;
   }
   
   /**
@@ -1244,8 +1206,8 @@ public synchronized boolean addToken(Text alias,
    */
   public synchronized
   Collection<Token<? extends TokenIdentifier>> getTokens() {
    return Collections.unmodifiableList(
        new ArrayList<Token<?>>(getCredentials().getAllTokens()));
    return Collections.unmodifiableCollection(
        getCredentialsInternal().getAllTokens());
   }
 
   /**
@@ -1254,11 +1216,26 @@ public synchronized boolean addToken(Text alias,
    * @return Credentials of tokens associated with this user
    */
   public synchronized Credentials getCredentials() {
    final Credentials credentials = new Credentials();
    final Set<NamedToken> namedTokens =
        subject.getPrivateCredentials(NamedToken.class);
    for (final NamedToken namedToken : namedTokens) {
      credentials.addToken(namedToken.alias, namedToken.token);
    return new Credentials(getCredentialsInternal());
  }
  
  /**
   * Add the given Credentials to this user.
   * @param credentials of tokens and secrets
   */
  public synchronized void addCredentials(Credentials credentials) {
    getCredentialsInternal().addAll(credentials);
  }

  private synchronized Credentials getCredentialsInternal() {
    final Credentials credentials;
    final Set<Credentials> credentialsSet =
      subject.getPrivateCredentials(Credentials.class);
    if (!credentialsSet.isEmpty()){
      credentials = credentialsSet.iterator().next();
    } else {
      credentials = new Credentials();
      subject.getPrivateCredentials().add(credentials);
     }
     return credentials;
   }
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestCredentials.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestCredentials.java
index 72d02dbc6e3..cad0262a92d 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestCredentials.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestCredentials.java
@@ -220,7 +220,7 @@ public void testAddTokensToUGI() {
     for (int i=0; i < service.length; i++) {
       creds.addToken(service[i], token[i]);
     }
    creds.addTokensToUGI(ugi);
    ugi.addCredentials(creds);
 
     creds = ugi.getCredentials();
     for (int i=0; i < service.length; i++) {
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java
index ce8ee28207c..a1bbd984d14 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java
@@ -250,6 +250,70 @@ public void testGettingGroups() throws Exception {
     ugi.addToken(t1);
     checkTokens(ugi, t1, t2, t3);    
   }

  @SuppressWarnings("unchecked") // from Mockito mocks
  @Test
  public <T extends TokenIdentifier> void testGetCreds() throws Exception {
    UserGroupInformation ugi = 
        UserGroupInformation.createRemoteUser("someone"); 
    
    Text service = new Text("service");
    Token<T> t1 = mock(Token.class);
    when(t1.getService()).thenReturn(service);
    Token<T> t2 = mock(Token.class);
    when(t2.getService()).thenReturn(new Text("service2"));
    Token<T> t3 = mock(Token.class);
    when(t3.getService()).thenReturn(service);
    
    // add token to ugi
    ugi.addToken(t1);
    ugi.addToken(t2);
    checkTokens(ugi, t1, t2);

    Credentials creds = ugi.getCredentials();
    creds.addToken(t3.getService(), t3);
    assertSame(t3, creds.getToken(service));
    // check that ugi wasn't modified
    checkTokens(ugi, t1, t2);
  }

  @SuppressWarnings("unchecked") // from Mockito mocks
  @Test
  public <T extends TokenIdentifier> void testAddCreds() throws Exception {
    UserGroupInformation ugi = 
        UserGroupInformation.createRemoteUser("someone"); 
    
    Text service = new Text("service");
    Token<T> t1 = mock(Token.class);
    when(t1.getService()).thenReturn(service);
    Token<T> t2 = mock(Token.class);
    when(t2.getService()).thenReturn(new Text("service2"));
    byte[] secret = new byte[]{};
    Text secretKey = new Text("sshhh");

    // fill credentials
    Credentials creds = new Credentials();
    creds.addToken(t1.getService(), t1);
    creds.addToken(t2.getService(), t2);
    creds.addSecretKey(secretKey, secret);
    
    // add creds to ugi, and check ugi
    ugi.addCredentials(creds);
    checkTokens(ugi, t1, t2);
    assertSame(secret, ugi.getCredentials().getSecretKey(secretKey));
  }

  @SuppressWarnings("unchecked") // from Mockito mocks
  @Test
  public <T extends TokenIdentifier> void testGetCredsNotSame()
      throws Exception {
    UserGroupInformation ugi = 
        UserGroupInformation.createRemoteUser("someone"); 
    Credentials creds = ugi.getCredentials();
    // should always get a new copy
    assertNotSame(creds, ugi.getCredentials());
  }

   
   private void checkTokens(UserGroupInformation ugi, Token<?> ... tokens) {
     // check the ugi's token collection
@@ -299,13 +363,22 @@ private void checkTokens(UserGroupInformation ugi, Token<?> ... tokens) {
     Token<T> t2 = mock(Token.class);
     when(t2.getService()).thenReturn(new Text("t2"));
     
    Credentials creds = new Credentials();
    byte[] secretKey = new byte[]{};
    Text secretName = new Text("shhh");
    creds.addSecretKey(secretName, secretKey);
    
     ugi.addToken(t1);
     ugi.addToken(t2);
    ugi.addCredentials(creds);
     
     Collection<Token<? extends TokenIdentifier>> z = ugi.getTokens();
     assertTrue(z.contains(t1));
     assertTrue(z.contains(t2));
     assertEquals(2, z.size());
    Credentials ugiCreds = ugi.getCredentials();
    assertSame(secretKey, ugiCreds.getSecretKey(secretName));
    assertEquals(1, ugiCreds.numberOfSecretKeys());
     
     try {
       z.remove(t1);
diff --git a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-app/src/main/java/org/apache/hadoop/mapred/YarnChild.java b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-app/src/main/java/org/apache/hadoop/mapred/YarnChild.java
index 64ac83e8cfd..c05c7aa69db 100644
-- a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-app/src/main/java/org/apache/hadoop/mapred/YarnChild.java
++ b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-app/src/main/java/org/apache/hadoop/mapred/YarnChild.java
@@ -141,7 +141,7 @@ public TaskUmbilicalProtocol run() throws Exception {
       childUGI = UserGroupInformation.createRemoteUser(System
           .getenv(ApplicationConstants.Environment.USER.toString()));
       // Add tokens to new user so that it may execute its task correctly.
      job.getCredentials().addTokensToUGI(childUGI);
      childUGI.addCredentials(credentials);
 
       // Create a final reference to the task for the doAs block
       final Task taskFinal = task;
diff --git a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-app/src/main/java/org/apache/hadoop/mapreduce/v2/app/MRAppMaster.java b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-app/src/main/java/org/apache/hadoop/mapreduce/v2/app/MRAppMaster.java
index 463a3edec69..d80653767e4 100644
-- a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-app/src/main/java/org/apache/hadoop/mapreduce/v2/app/MRAppMaster.java
++ b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-app/src/main/java/org/apache/hadoop/mapreduce/v2/app/MRAppMaster.java
@@ -487,7 +487,7 @@ protected void downloadTokensAndSetupUGI(Configuration conf) {
         fsTokens.addAll(Credentials.readTokenStorageFile(jobTokenFile, conf));
         LOG.info("jobSubmitDir=" + jobSubmitDir + " jobTokenFile="
             + jobTokenFile);
        fsTokens.addTokensToUGI(currentUser); // For use by AppMaster itself.
        currentUser.addCredentials(fsTokens); // For use by AppMaster itself.
       }
     } catch (IOException e) {
       throw new YarnException(e);
diff --git a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/test/java/org/apache/hadoop/mapreduce/TestJob.java b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/test/java/org/apache/hadoop/mapreduce/TestJob.java
index 6d2f5e6b692..94f49acf971 100644
-- a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/test/java/org/apache/hadoop/mapreduce/TestJob.java
++ b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/test/java/org/apache/hadoop/mapreduce/TestJob.java
@@ -27,6 +27,7 @@
 import org.apache.hadoop.mapred.JobConf;
 import org.apache.hadoop.mapreduce.JobStatus.State;
 import org.apache.hadoop.mapreduce.protocol.ClientProtocol;
import org.apache.hadoop.security.Credentials;
 import org.apache.hadoop.security.UserGroupInformation;
 import org.apache.hadoop.security.token.Token;
 import org.junit.Assert;
@@ -55,14 +56,20 @@ public void testJobToString() throws IOException, InterruptedException {
 
   @Test
   public void testUGICredentialsPropogation() throws Exception {
    Credentials creds = new Credentials();
     Token<?> token = mock(Token.class);
    Text service = new Text("service");
    
    UserGroupInformation ugi = UserGroupInformation.getCurrentUser();
    ugi.addToken(service, token);
    Text tokenService = new Text("service");
    Text secretName = new Text("secret");
    byte secret[] = new byte[]{};
        
    creds.addToken(tokenService,  token);
    creds.addSecretKey(secretName, secret);
    UserGroupInformation.getLoginUser().addCredentials(creds);
     
     JobConf jobConf = new JobConf();
     Job job = new Job(jobConf);
    assertSame(token, job.getCredentials().getToken(service));

    assertSame(token, job.getCredentials().getToken(tokenService));
    assertSame(secret, job.getCredentials().getSecretKey(secretName));
   }
 }
- 
2.19.1.windows.1

