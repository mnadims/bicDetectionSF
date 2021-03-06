From 5650939a8d41b7bad584947a2c9dcedf3774b8de Mon Sep 17 00:00:00 2001
From: Noble Paul <noble@apache.org>
Date: Fri, 25 Nov 2016 00:51:38 +0530
Subject: [PATCH] SOLR-9784: Refactor CloudSolrClient to eliminate direct
 dependency on ZK SOLR-9512: CloudSolrClient's cluster state cache can break
 direct updates to leaders

--
 .../client/solrj/impl/CloudSolrClient.java    | 313 +++++++++++-------
 1 file changed, 191 insertions(+), 122 deletions(-)

diff --git a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/CloudSolrClient.java b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/CloudSolrClient.java
index 9bc45294b3c..241e2a145f1 100644
-- a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/CloudSolrClient.java
++ b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/CloudSolrClient.java
@@ -16,6 +16,7 @@
  */
 package org.apache.solr.client.solrj.impl;
 
import java.io.Closeable;
 import java.io.IOException;
 import java.lang.invoke.MethodHandles;
 import java.net.ConnectException;
@@ -37,6 +38,7 @@ import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Future;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
 import java.util.concurrent.locks.Lock;
 import java.util.concurrent.locks.ReentrantLock;
 
@@ -56,7 +58,6 @@ import org.apache.solr.common.SolrException;
 import org.apache.solr.common.SolrException.ErrorCode;
 import org.apache.solr.common.SolrInputDocument;
 import org.apache.solr.common.ToleratedUpdateError;
import org.apache.solr.common.cloud.Aliases;
 import org.apache.solr.common.cloud.ClusterState;
 import org.apache.solr.common.cloud.CollectionStatePredicate;
 import org.apache.solr.common.cloud.CollectionStateWatcher;
@@ -68,7 +69,6 @@ import org.apache.solr.common.cloud.Slice;
 import org.apache.solr.common.cloud.ZkCoreNodeProps;
 import org.apache.solr.common.cloud.ZkNodeProps;
 import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.common.cloud.ZooKeeperException;
 import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.common.params.ShardParams;
 import org.apache.solr.common.params.SolrParams;
@@ -79,7 +79,6 @@ import org.apache.solr.common.util.NamedList;
 import org.apache.solr.common.util.SimpleOrderedMap;
 import org.apache.solr.common.util.SolrjNamedThreadFactory;
 import org.apache.solr.common.util.StrUtils;
import org.apache.zookeeper.KeeperException;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.slf4j.MDC;
@@ -101,10 +100,7 @@ public class CloudSolrClient extends SolrClient {
 
   private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
 
  private volatile ZkStateReader zkStateReader;
  private String zkHost; // the zk server connect string
  private int zkConnectTimeout = 10000;
  private int zkClientTimeout = 10000;
  private final ClusterStateProvider stateProvider;
   private volatile String defaultCollection;
   private final LBHttpSolrClient lbClient;
   private final boolean shutdownLBHttpSolrServer;
@@ -122,6 +118,7 @@ public class CloudSolrClient extends SolrClient {
           "CloudSolrClient ThreadPool"));
   private String idField = "id";
   public static final String STATE_VERSION = "_stateVer_";
  private long retryExpiryTime = TimeUnit.NANOSECONDS.convert(3, TimeUnit.SECONDS);//3 seconds or 3 million nanos
   private final Set<String> NON_ROUTABLE_PARAMS;
   {
     NON_ROUTABLE_PARAMS = new HashSet<>();
@@ -139,12 +136,15 @@ public class CloudSolrClient extends SolrClient {
     // NON_ROUTABLE_PARAMS.add(UpdateParams.ROLLBACK);
 
   }
  private volatile long timeToLive = 60* 1000L;
   private volatile List<Object> locks = objectList(3);
 
 
  protected final Map<String, ExpiringCachedDocCollection> collectionStateCache = new ConcurrentHashMap<String, ExpiringCachedDocCollection>(){
  static class StateCache extends ConcurrentHashMap<String, ExpiringCachedDocCollection> {
    final AtomicLong puts = new AtomicLong();
    final AtomicLong hits = new AtomicLong();
     final Lock evictLock = new ReentrantLock(true);
    private volatile long timeToLive = 60 * 1000L;

     @Override
     public ExpiringCachedDocCollection get(Object key) {
       ExpiringCachedDocCollection val = super.get(key);
@@ -158,9 +158,16 @@ public class CloudSolrClient extends SolrClient {
         super.remove(key);
         return null;
       }
      hits.incrementAndGet();
       return val;
     }
 
    @Override
    public ExpiringCachedDocCollection put(String key, ExpiringCachedDocCollection value) {
      puts.incrementAndGet();
      return super.put(key, value);
    }

     void evictStale() {
       if(!evictLock.tryLock()) return;
       try {
@@ -174,11 +181,30 @@ public class CloudSolrClient extends SolrClient {
       }
     }
 
  };
  }

  /**
   * This is the time to wait to refetch the state
   * after getting the same state version from ZK
   * <p>
   * secs
   */
  public void setRetryExpiryTime(int secs) {
    this.retryExpiryTime = TimeUnit.NANOSECONDS.convert(secs, TimeUnit.SECONDS);
  }

  public void setSoTimeout(int timeout) {
    lbClient.setSoTimeout(timeout);
  }
 
  protected final StateCache collectionStateCache = new StateCache();
   class ExpiringCachedDocCollection {
     final DocCollection cached;
    long cachedAt;
    final long cachedAt;
    //This is the time at which the collection is retried and got the same old version
    long retriedAt = -1;
    //flag that suggests that this is potentially to be rechecked
    boolean maybeStale = false;
 
     ExpiringCachedDocCollection(DocCollection cached) {
       this.cached = cached;
@@ -189,6 +215,21 @@ public class CloudSolrClient extends SolrClient {
       return (System.nanoTime() - cachedAt)
           > TimeUnit.NANOSECONDS.convert(timeToLiveMs, TimeUnit.MILLISECONDS);
     }

    boolean shoulRetry() {
      if (maybeStale) {// we are not sure if it is stale so check with retry time
        if ((retriedAt == -1 ||
            (System.nanoTime() - retriedAt) > retryExpiryTime)) {
          return true;// we retried a while back. and we could not get anything new.
          //it's likely that it is not going to be available now also.
        }
      }
      return false;
    }

    void setRetriedAt() {
      retriedAt = System.nanoTime();
    }
   }
 
   /**
@@ -215,7 +256,7 @@ public class CloudSolrClient extends SolrClient {
    */
   @Deprecated
   public CloudSolrClient(String zkHost) {
      this.zkHost = zkHost;
    this.stateProvider = new ZkClientClusterStateProvider(zkHost);
       this.clientIsInternal = true;
       this.myClient = HttpClientUtil.createClient(null);
       this.lbClient = new LBHttpSolrClient.Builder()
@@ -255,8 +296,8 @@ public class CloudSolrClient extends SolrClient {
    * @deprecated use {@link Builder} instead.
    */
   @Deprecated
  public CloudSolrClient(String zkHost, HttpClient httpClient)  {
    this.zkHost = zkHost;
  public CloudSolrClient(String zkHost, HttpClient httpClient) {
    this.stateProvider = new ZkClientClusterStateProvider(zkHost);
     this.clientIsInternal = httpClient == null;
     this.myClient = httpClient == null ? HttpClientUtil.createClient(null) : httpClient;
     this.lbClient = createLBHttpSolrClient(myClient);
@@ -314,7 +355,7 @@ public class CloudSolrClient extends SolrClient {
    */
   @Deprecated
   public CloudSolrClient(Collection<String> zkHosts, String chroot, HttpClient httpClient) {
    this.zkHost = buildZkHostString(zkHosts, chroot);
    this.stateProvider = new ZkClientClusterStateProvider(zkHosts, chroot);
     this.clientIsInternal = httpClient == null;
     this.myClient = httpClient == null ? HttpClientUtil.createClient(null) : httpClient;
     this.lbClient = createLBHttpSolrClient(myClient);
@@ -350,7 +391,7 @@ public class CloudSolrClient extends SolrClient {
    */
   @Deprecated
   public CloudSolrClient(Collection<String> zkHosts, String chroot, HttpClient httpClient, LBHttpSolrClient lbSolrClient, boolean updatesToLeaders) {
    this(zkHosts, chroot, httpClient, lbSolrClient, null, updatesToLeaders, false);
    this(zkHosts, chroot, httpClient, lbSolrClient, null, updatesToLeaders, false, null);
   }
 
   /**
@@ -385,8 +426,15 @@ public class CloudSolrClient extends SolrClient {
                           LBHttpSolrClient lbSolrClient,
                           LBHttpSolrClient.Builder lbHttpSolrClientBuilder,
                           boolean updatesToLeaders,
                          boolean directUpdatesToLeadersOnly) {
    this.zkHost = buildZkHostString(zkHosts, chroot);
                          boolean directUpdatesToLeadersOnly,
                          ClusterStateProvider stateProvider

  ) {
    if (stateProvider == null) {
      this.stateProvider = new ZkClientClusterStateProvider(zkHosts, chroot);
    } else {
      this.stateProvider = stateProvider;
    }
     this.clientIsInternal = httpClient == null;
     this.shutdownLBHttpSolrServer = lbSolrClient == null;
     if(lbHttpSolrClientBuilder != null) lbSolrClient = lbHttpSolrClientBuilder.build();
@@ -424,7 +472,7 @@ public class CloudSolrClient extends SolrClient {
    */
   @Deprecated
   public CloudSolrClient(String zkHost, boolean updatesToLeaders, HttpClient httpClient) {
    this.zkHost = zkHost;
    this.stateProvider = new ZkClientClusterStateProvider(zkHost);
     this.clientIsInternal = httpClient == null;
     this.myClient = httpClient == null ? HttpClientUtil.createClient(null) : httpClient;
     this.lbClient = new LBHttpSolrClient.Builder()
@@ -443,7 +491,7 @@ public class CloudSolrClient extends SolrClient {
    */
   public void setCollectionCacheTTl(int seconds){
     assert seconds > 0;
    timeToLive = seconds*1000L;
    this.collectionStateCache.timeToLive = seconds * 1000L;
   }
 
   /**
@@ -471,8 +519,8 @@ public class CloudSolrClient extends SolrClient {
    */
   @Deprecated
   public CloudSolrClient(String zkHost, LBHttpSolrClient lbClient, boolean updatesToLeaders) {
    this.zkHost = zkHost;
     this.lbClient = lbClient;
    this.stateProvider = new ZkClientClusterStateProvider(zkHost);
     this.updatesToLeaders = updatesToLeaders;
     this.directUpdatesToLeadersOnly = false;
     shutdownLBHttpSolrServer = false;
@@ -508,11 +556,15 @@ public class CloudSolrClient extends SolrClient {
    * @return the zkHost value used to connect to zookeeper.
    */
   public String getZkHost() {
    return zkHost;
    return assertZKStateProvider().zkHost;
   }
 
   public ZkStateReader getZkStateReader() {
    return zkStateReader;
    if (stateProvider instanceof ZkClientClusterStateProvider) {
      ZkClientClusterStateProvider provider = (ZkClientClusterStateProvider) stateProvider;
      return provider.zkStateReader;
    }
    throw new IllegalStateException("This has no Zk stateReader");
   }
 
   /**
@@ -541,12 +593,12 @@ public class CloudSolrClient extends SolrClient {
 
   /** Set the connect timeout to the zookeeper ensemble in ms */
   public void setZkConnectTimeout(int zkConnectTimeout) {
    this.zkConnectTimeout = zkConnectTimeout;
    assertZKStateProvider().zkConnectTimeout = zkConnectTimeout;
   }
 
   /** Set the timeout to the zookeeper ensemble in ms */
   public void setZkClientTimeout(int zkClientTimeout) {
    this.zkClientTimeout = zkClientTimeout;
    assertZKStateProvider().zkClientTimeout = zkClientTimeout;
   }
 
   /**
@@ -555,29 +607,7 @@ public class CloudSolrClient extends SolrClient {
    *
    */
   public void connect() {
    if (zkStateReader == null) {
      synchronized (this) {
        if (zkStateReader == null) {
          ZkStateReader zk = null;
          try {
            zk = new ZkStateReader(zkHost, zkClientTimeout, zkConnectTimeout);
            zk.createClusterStateWatchersAndUpdate();
            zkStateReader = zk;
          } catch (InterruptedException e) {
            zk.close();
            Thread.currentThread().interrupt();
            throw new ZooKeeperException(SolrException.ErrorCode.SERVER_ERROR, "", e);
          } catch (KeeperException e) {
            zk.close();
            throw new ZooKeeperException(SolrException.ErrorCode.SERVER_ERROR, "", e);
          } catch (Exception e) {
            if (zk != null) zk.close();
            // do not wrap because clients may be relying on the underlying exception being thrown
            throw e;
          }
        }
      }
    }
    stateProvider.connect();
   }
 
   /**
@@ -588,12 +618,12 @@ public class CloudSolrClient extends SolrClient {
    * @throws InterruptedException if the wait is interrupted
    */
   public void connect(long duration, TimeUnit timeUnit) throws TimeoutException, InterruptedException {
    log.info("Waiting for {} {} for cluster at {} to be ready", duration, timeUnit, zkHost);
    log.info("Waiting for {} {} for cluster at {} to be ready", duration, timeUnit, stateProvider);
     long timeout = System.nanoTime() + timeUnit.toNanos(duration);
     while (System.nanoTime() < timeout) {
       try {
         connect();
        log.info("Cluster at {} ready", zkHost);
        log.info("Cluster at {} ready", stateProvider);
         return;
       }
       catch (RuntimeException e) {
@@ -620,8 +650,16 @@ public class CloudSolrClient extends SolrClient {
    * @throws IOException if an IO error occurs
    */
   public void uploadConfig(Path configPath, String configName) throws IOException {
    connect();
    zkStateReader.getConfigManager().uploadConfigDir(configPath, configName);
    stateProvider.connect();
    assertZKStateProvider().uploadConfig(configPath, configName);
  }

  private ZkClientClusterStateProvider assertZKStateProvider() {
    if (stateProvider instanceof ZkClientClusterStateProvider) {
      return (ZkClientClusterStateProvider) stateProvider;
    }
    throw new IllegalArgumentException("This client does not use ZK");

   }
 
   /**
@@ -631,8 +669,7 @@ public class CloudSolrClient extends SolrClient {
    * @throws IOException  if an I/O exception occurs
    */
   public void downloadConfig(String configName, Path downloadPath) throws IOException {
    connect();
    zkStateReader.getConfigManager().downloadConfigDir(configName, downloadPath);
    assertZKStateProvider().downloadConfig(configName, downloadPath);
   }
 
   /**
@@ -650,8 +687,8 @@ public class CloudSolrClient extends SolrClient {
    */
   public void waitForState(String collection, long wait, TimeUnit unit, CollectionStatePredicate predicate)
       throws InterruptedException, TimeoutException {
    connect();
    zkStateReader.waitForState(collection, wait, unit, predicate);
    stateProvider.connect();
    assertZKStateProvider().zkStateReader.waitForState(collection, wait, unit, predicate);
   }
 
   /**
@@ -665,11 +702,11 @@ public class CloudSolrClient extends SolrClient {
    * @param watcher    a watcher that will be called when the state changes
    */
   public void registerCollectionStateWatcher(String collection, CollectionStateWatcher watcher) {
    connect();
    zkStateReader.registerCollectionStateWatcher(collection, watcher);
    stateProvider.connect();
    assertZKStateProvider().zkStateReader.registerCollectionStateWatcher(collection, watcher);
   }
 
  private NamedList<Object> directUpdate(AbstractUpdateRequest request, String collection, ClusterState clusterState) throws SolrServerException {
  private NamedList<Object> directUpdate(AbstractUpdateRequest request, String collection) throws SolrServerException {
     UpdateRequest updateRequest = (UpdateRequest) request;
     ModifiableSolrParams params = (ModifiableSolrParams) request.getParams();
     ModifiableSolrParams routableParams = new ModifiableSolrParams();
@@ -689,15 +726,9 @@ public class CloudSolrClient extends SolrClient {
 
 
     //Check to see if the collection is an alias.
    Aliases aliases = zkStateReader.getAliases();
    if(aliases != null) {
      Map<String, String> collectionAliases = aliases.getCollectionAliasMap();
      if(collectionAliases != null && collectionAliases.containsKey(collection)) {
        collection = collectionAliases.get(collection);
      }
    }
    collection = stateProvider.getCollectionName(collection);
 
    DocCollection col = getDocCollection(clusterState, collection,null);
    DocCollection col = getDocCollection(collection, null);
 
     DocRouter router = col.getRouter();
     
@@ -1018,12 +1049,12 @@ public class CloudSolrClient extends SolrClient {
     List<DocCollection> requestedCollections = null;
     boolean isAdmin = ADMIN_PATHS.contains(request.getPath());
     if (collection != null &&  !isAdmin) { // don't do _stateVer_ checking for admin requests
      Set<String> requestedCollectionNames = getCollectionNames(getZkStateReader().getClusterState(), collection);
      Set<String> requestedCollectionNames = getCollectionNames(collection);
 
       StringBuilder stateVerParamBuilder = null;
       for (String requestedCollection : requestedCollectionNames) {
         // track the version of state we're using on the client side using the _stateVer_ param
        DocCollection coll = getDocCollection(getZkStateReader().getClusterState(), requestedCollection,null);
        DocCollection coll = getDocCollection(requestedCollection, null);
         int collVer = coll.getZNodeVersion();
         if (coll.getStateFormat()>1) {
           if(requestedCollections == null) requestedCollections = new ArrayList<>(requestedCollectionNames.size());
@@ -1064,7 +1095,7 @@ public class CloudSolrClient extends SolrClient {
         Map invalidStates = (Map) o;
         for (Object invalidEntries : invalidStates.entrySet()) {
           Map.Entry e = (Map.Entry) invalidEntries;
          getDocCollection(getZkStateReader().getClusterState(),(String)e.getKey(), (Integer)e.getValue());
          getDocCollection((String) e.getKey(), (Integer) e.getValue());
         }
 
       }
@@ -1097,6 +1128,26 @@ public class CloudSolrClient extends SolrClient {
               rootCause instanceof NoHttpResponseException ||
               rootCause instanceof SocketException);
 
      if (wasCommError) {
        // it was a communication error. it is likely that
        // the node to which the request to be sent is down . So , expire the state
        // so that the next attempt would fetch the fresh state
        // just re-read state for all of them, if it has not been retired
        // in retryExpiryTime time
        for (DocCollection ext : requestedCollections) {
          ExpiringCachedDocCollection cacheEntry = collectionStateCache.get(ext.getName());
          if (cacheEntry == null) continue;
          cacheEntry.maybeStale = true;
        }
        if (retryCount < MAX_STALE_RETRIES) {//if it is a communication error , we must try again
          //may be, we have a stale version of the collection state
          // and we could not get any information from the server
          //it is probably not worth trying again and again because
          // the state would not have been updated
          return requestWithRetryOnStaleState(request, retryCount + 1, collection);
        }
      }

       boolean stateWasStale = false;
       if (retryCount < MAX_STALE_RETRIES  &&
           requestedCollections != null    &&
@@ -1121,7 +1172,7 @@ public class CloudSolrClient extends SolrClient {
           !requestedCollections.isEmpty() &&
           wasCommError) {
         for (DocCollection ext : requestedCollections) {
          DocCollection latestStateFromZk = getDocCollection(zkStateReader.getClusterState(), ext.getName(),null);
          DocCollection latestStateFromZk = getDocCollection(ext.getName(), null);
           if (latestStateFromZk.getZNodeVersion() != ext.getZNodeVersion()) {
             // looks like we couldn't reach the server because the state was stale == retry
             stateWasStale = true;
@@ -1158,15 +1209,13 @@ public class CloudSolrClient extends SolrClient {
   protected NamedList<Object> sendRequest(SolrRequest request, String collection)
       throws SolrServerException, IOException {
     connect();
    
    ClusterState clusterState = zkStateReader.getClusterState();
    

     boolean sendToLeaders = false;
     List<String> replicas = null;
     
     if (request instanceof IsUpdateRequest) {
       if (request instanceof UpdateRequest) {
        NamedList<Object> response = directUpdate((AbstractUpdateRequest) request, collection, clusterState);
        NamedList<Object> response = directUpdate((AbstractUpdateRequest) request, collection);
         if (response != null) {
           return response;
         }
@@ -1181,9 +1230,10 @@ public class CloudSolrClient extends SolrClient {
     }
     List<String> theUrlList = new ArrayList<>();
     if (ADMIN_PATHS.contains(request.getPath())) {
      Set<String> liveNodes = clusterState.getLiveNodes();
      Set<String> liveNodes = stateProvider.liveNodes();
       for (String liveNode : liveNodes) {
        theUrlList.add(zkStateReader.getBaseUrlForNodeName(liveNode));
        theUrlList.add(ZkStateReader.getBaseUrlForNodeName(liveNode,
            (String) stateProvider.getClusterProperties().getOrDefault(ZkStateReader.URL_SCHEME,"http")));
       }
     } else {
       
@@ -1191,8 +1241,8 @@ public class CloudSolrClient extends SolrClient {
         throw new SolrServerException(
             "No collection param specified on request and no default collection has been set.");
       }
      
      Set<String> collectionNames = getCollectionNames(clusterState, collection);

      Set<String> collectionNames = getCollectionNames(collection);
       if (collectionNames.size() == 0) {
         throw new SolrException(ErrorCode.BAD_REQUEST,
             "Could not find collection: " + collection);
@@ -1209,11 +1259,11 @@ public class CloudSolrClient extends SolrClient {
       // add it to the Map of slices.
       Map<String,Slice> slices = new HashMap<>();
       for (String collectionName : collectionNames) {
        DocCollection col = getDocCollection(clusterState, collectionName, null);
        DocCollection col = getDocCollection(collectionName, null);
         Collection<Slice> routeSlices = col.getRouter().getSearchSlices(shardKeys, reqParams , col);
         ClientUtils.addSlices(slices, collectionName, routeSlices, true);
       }
      Set<String> liveNodes = clusterState.getLiveNodes();
      Set<String> liveNodes = stateProvider.liveNodes();
 
       List<String> leaderUrlList = null;
       List<String> urlList = null;
@@ -1289,16 +1339,14 @@ public class CloudSolrClient extends SolrClient {
     return rsp.getResponse();
   }
 
  private Set<String> getCollectionNames(ClusterState clusterState,
                                         String collection) {
  Set<String> getCollectionNames(String collection) {
     // Extract each comma separated collection name and store in a List.
     List<String> rawCollectionsList = StrUtils.splitSmart(collection, ",", true);
     Set<String> collectionNames = new HashSet<>();
     // validate collections
     for (String collectionName : rawCollectionsList) {
      if (!clusterState.hasCollection(collectionName)) {
        Aliases aliases = zkStateReader.getAliases();
        String alias = aliases.getCollectionAlias(collectionName);
      if (stateProvider.getState(collectionName) == null) {
        String alias = stateProvider.getAlias(collection);
         if (alias != null) {
           List<String> aliasList = StrUtils.splitSmart(alias, ",", true);
           collectionNames.addAll(aliasList);
@@ -1315,13 +1363,7 @@ public class CloudSolrClient extends SolrClient {
 
   @Override
   public void close() throws IOException {
    if (zkStateReader != null) {
      synchronized(this) {
        if (zkStateReader!= null)
          zkStateReader.close();
        zkStateReader = null;
      }
    }
    stateProvider.close();
     
     if (shutdownLBHttpSolrServer) {
       lbClient.close();
@@ -1367,15 +1409,17 @@ public class CloudSolrClient extends SolrClient {
   }
 
 
  protected DocCollection getDocCollection(ClusterState clusterState, String collection, Integer expectedVersion) throws SolrException {
  protected DocCollection getDocCollection(String collection, Integer expectedVersion) throws SolrException {
    if (expectedVersion == null) expectedVersion = -1;
     if (collection == null) return null;
    DocCollection col = getFromCache(collection);
    ExpiringCachedDocCollection cacheEntry = collectionStateCache.get(collection);
    DocCollection col = cacheEntry == null ? null : cacheEntry.cached;
     if (col != null) {
      if (expectedVersion == null) return col;
      if (expectedVersion.intValue() == col.getZNodeVersion()) return col;
      if (expectedVersion <= col.getZNodeVersion()
          && !cacheEntry.shoulRetry()) return col;
     }
 
    ClusterState.CollectionRef ref = clusterState.getCollectionRef(collection);
    ClusterState.CollectionRef ref = getCollectionRef(collection);
     if (ref == null) {
       //no such collection exists
       return null;
@@ -1386,30 +1430,34 @@ public class CloudSolrClient extends SolrClient {
     }
     List locks = this.locks;
     final Object lock = locks.get(Math.abs(Hash.murmurhash3_x86_32(collection, 0, collection.length(), 0) % locks.size()));
    DocCollection fetchedCol = null;
     synchronized (lock) {
      //we have waited for sometime just check once again
      col = getFromCache(collection);
      /*we have waited for sometime just check once again*/
      cacheEntry = collectionStateCache.get(collection);
      col = cacheEntry == null ? null : cacheEntry.cached;
       if (col != null) {
        if (expectedVersion == null) return col;
        if (expectedVersion.intValue() == col.getZNodeVersion()) {
          return col;
        } else {
          collectionStateCache.remove(collection);
        }
        if (expectedVersion <= col.getZNodeVersion()
            && !cacheEntry.shoulRetry()) return col;
      }
      // We are going to fetch a new version
      // we MUST try to get a new version
      fetchedCol = ref.get();//this is a call to ZK
      if (fetchedCol == null) return null;// this collection no more exists
      if (col != null && fetchedCol.getZNodeVersion() == col.getZNodeVersion()) {
        cacheEntry.setRetriedAt();//we retried and found that it is the same version
        cacheEntry.maybeStale = false;
      } else {
        if (fetchedCol.getStateFormat() > 1)
          collectionStateCache.put(collection, new ExpiringCachedDocCollection(fetchedCol));
       }
      col = ref.get();//this is a call to ZK
      return fetchedCol;
     }
    if (col == null) return null;
    if (col.getStateFormat() > 1) collectionStateCache.put(collection, new ExpiringCachedDocCollection(col));
    return col;
   }
 
  private DocCollection getFromCache(String c){
    ExpiringCachedDocCollection cachedState = collectionStateCache.get(c);
    return cachedState != null ? cachedState.cached : null;
  ClusterState.CollectionRef getCollectionRef(String collection) {
    return stateProvider.getState(collection);
   }
 

   /**
    * Useful for determining the minimum achieved replication factor across
    * all shards involved in processing an update request, typically useful
@@ -1445,9 +1493,9 @@ public class CloudSolrClient extends SolrClient {
     Map<String,Integer> results = new HashMap<String,Integer>();
     if (resp instanceof CloudSolrClient.RouteResponse) {
       NamedList routes = ((CloudSolrClient.RouteResponse)resp).getRouteResponses();
      ClusterState clusterState = zkStateReader.getClusterState();     
      DocCollection coll = getDocCollection(collection, null);
       Map<String,String> leaders = new HashMap<String,String>();
      for (Slice slice : clusterState.getActiveSlices(collection)) {
      for (Slice slice : coll.getActiveSlices()) {
         Replica leader = slice.getLeader();
         if (leader != null) {
           ZkCoreNodeProps zkProps = new ZkCoreNodeProps(leader);
@@ -1484,10 +1532,6 @@ public class CloudSolrClient extends SolrClient {
     this.lbClient.setConnectionTimeout(timeout); 
   }
 
  public void setSoTimeout(int timeout) {
    this.lbClient.setSoTimeout(timeout);
  }

   private static boolean hasInfoToFindLeaders(UpdateRequest updateRequest, String idField) {
     final Map<SolrInputDocument,Map<String,Object>> documents = updateRequest.getDocumentsMap();
     final Map<String,Map<String,Object>> deleteById = updateRequest.getDeleteByIdMap();
@@ -1564,7 +1608,9 @@ public class CloudSolrClient extends SolrClient {
     private LBHttpSolrClient.Builder lbClientBuilder;
     private boolean shardLeadersOnly;
     private boolean directUpdatesToLeadersOnly;
    
    private ClusterStateProvider stateProvider;


     public Builder() {
       this.zkHosts = new ArrayList();
       this.shardLeadersOnly = true;
@@ -1666,12 +1712,35 @@ public class CloudSolrClient extends SolrClient {
       return this;
     }
 
    public Builder withClusterStateProvider(ClusterStateProvider stateProvider) {
      this.stateProvider = stateProvider;
      return this;
    }

     /**
      * Create a {@link CloudSolrClient} based on the provided configuration.
      */
     public CloudSolrClient build() {
      if (stateProvider == null) {
        stateProvider = new ZkClientClusterStateProvider(zkHosts, zkChroot);
      }
       return new CloudSolrClient(zkHosts, zkChroot, httpClient, loadBalancedSolrClient, lbClientBuilder,
          shardLeadersOnly, directUpdatesToLeadersOnly);
          shardLeadersOnly, directUpdatesToLeadersOnly, stateProvider);
     }
   }

  interface ClusterStateProvider extends Closeable {

    ClusterState.CollectionRef getState(String collection);

    Set<String> liveNodes();

    String getAlias(String collection);

    String getCollectionName(String name);

    Map<String, Object> getClusterProperties();

    void connect();
  }
 }
- 
2.19.1.windows.1

