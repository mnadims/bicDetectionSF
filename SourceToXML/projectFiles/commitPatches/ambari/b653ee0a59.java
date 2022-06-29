From b653ee0a5967a8255f087f43a1cb6a878e765833 Mon Sep 17 00:00:00 2001
From: Sandor Magyari <smagyari@hortonworks.com>
Date: Fri, 22 Sep 2017 17:48:54 +0200
Subject: [PATCH] AMBARI-22012. BP deploys failing intermittently with error 
 (magyari_sandor)

--
 .../events/ClusterConfigFinishedEvent.java    | 15 +++-
 .../server/topology/TopologyManager.java      | 88 ++++++++++++-------
 .../topology/tasks/ConfigureClusterTask.java  | 13 ++-
 .../tasks/ConfigureClusterTaskFactory.java    |  3 +-
 .../utils/ManagedThreadPoolExecutor.java      | 83 +++++++++++++++++
 .../ClusterDeployWithStartOnlyTest.java       |  2 +-
 ...stallWithoutStartOnComponentLevelTest.java |  2 +-
 .../ClusterInstallWithoutStartTest.java       |  2 +-
 .../topology/ConfigureClusterTaskTest.java    | 18 +++-
 .../utils/ManagedThreadPoolExecutorTest.java  | 51 +++++++++++
 10 files changed, 236 insertions(+), 41 deletions(-)
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/utils/ManagedThreadPoolExecutor.java
 create mode 100644 ambari-server/src/test/java/org/apache/ambari/server/utils/ManagedThreadPoolExecutorTest.java

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/events/ClusterConfigFinishedEvent.java b/ambari-server/src/main/java/org/apache/ambari/server/events/ClusterConfigFinishedEvent.java
index cdb86ac953..f0574d054b 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/events/ClusterConfigFinishedEvent.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/events/ClusterConfigFinishedEvent.java
@@ -23,17 +23,27 @@ package org.apache.ambari.server.events;
  * cluster configuration is successfully updated.
  */
 public class ClusterConfigFinishedEvent extends AmbariEvent {

  private final long clusterId;
   private final String clusterName;
 
 
  public ClusterConfigFinishedEvent(String clusterName) {
  public ClusterConfigFinishedEvent(long clusterId, String clusterName) {
     super(AmbariEventType.CLUSTER_CONFIG_FINISHED);
    this.clusterId = clusterId;
     this.clusterName = clusterName;
   }
 
  /**
   * Get the cluster id
   * @return
   */
  public long getClusterId() {
    return clusterId;
  }

   /**
    * Get the cluster name
   *
    * @return
    */
   public String getClusterName() {
@@ -46,6 +56,7 @@ public class ClusterConfigFinishedEvent extends AmbariEvent {
   @Override
   public String toString() {
     StringBuilder buffer = new StringBuilder("ClusterConfigChangedEvent{");
    buffer.append("clusterId=").append(getClusterId());
     buffer.append("clusterName=").append(getClusterName());
     buffer.append("}");
     return buffer.toString();
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/TopologyManager.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/TopologyManager.java
index 3af62e819e..9769fae4b8 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/topology/TopologyManager.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/TopologyManager.java
@@ -29,9 +29,10 @@ import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
 
 import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.actionmanager.HostRoleCommand;
@@ -73,6 +74,7 @@ import org.apache.ambari.server.state.quicklinksprofile.QuickLinksProfile;
 import org.apache.ambari.server.topology.tasks.ConfigureClusterTask;
 import org.apache.ambari.server.topology.tasks.ConfigureClusterTaskFactory;
 import org.apache.ambari.server.topology.validators.TopologyValidatorService;
import org.apache.ambari.server.utils.ManagedThreadPoolExecutor;
 import org.apache.ambari.server.utils.RetryHelper;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
@@ -102,9 +104,23 @@ public class TopologyManager {
   private static final String CLUSTER_CONFIG_TASK_MAX_TIME_IN_MILLIS_PROPERTY_NAME = "cluster_configure_task_timeout";
 
   private PersistedState persistedState;

  /**
   * Single threaded executor to execute async tasks. At the moment it's only used to execute ConfigureClusterTask.
   */
   private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final Executor taskExecutor; // executes TopologyTasks
  private final boolean parallelTaskCreationEnabled;

  /**
   * Thread pool size for topology task executors.
   */
  private int topologyTaskExecutorThreadPoolSize;
  /**
   * There is one ExecutorService for each cluster to execute TopologyTasks.
   * TopologyTasks are submitted into ExecutorService for each cluster,
   * however the ExecutorService is started only after cluster configuration is finished.
   */
  private final Map<Long, ManagedThreadPoolExecutor> topologyTaskExecutorServiceMap = new HashMap<>();

   private Collection<String> hostsToIgnore = new HashSet<>();
   private final List<HostImpl> availableHosts = new LinkedList<>();
   private final Map<String, LogicalRequest> reservedHosts = new HashMap<>();
@@ -158,17 +174,15 @@ public class TopologyManager {
   private Map<Long, Boolean> clusterProvisionWithBlueprintCreationFinished = new HashMap<>();
 
   public TopologyManager() {
    parallelTaskCreationEnabled = false;
    taskExecutor = executor;
    topologyTaskExecutorThreadPoolSize = 1;
   }
 
   @Inject
   public TopologyManager(Configuration configuration) {
    int threadPoolSize = configuration.getParallelTopologyTaskCreationThreadPoolSize();
    parallelTaskCreationEnabled = configuration.isParallelTopologyTaskCreationEnabled() && threadPoolSize > 1;
    taskExecutor = parallelTaskCreationEnabled
      ? Executors.newFixedThreadPool(threadPoolSize)
      : executor;
    topologyTaskExecutorThreadPoolSize = configuration.getParallelTopologyTaskCreationThreadPoolSize();
    if (!configuration.isParallelTopologyTaskCreationEnabled()) {
      topologyTaskExecutorThreadPoolSize = 1;
    }
   }
 
   // executed by the IoC framework after creating the object (guice)
@@ -310,6 +324,10 @@ public class TopologyManager {
     // set provision action requested
     topology.setProvisionAction(request.getProvisionAction());
 

    // create task executor for TopologyTasks
    getOrCreateTopologyTaskExecutor(clusterId);

     // persist request
     LogicalRequest logicalRequest = RetryHelper.executeWithRetry(new Callable<LogicalRequest>() {
         @Override
@@ -325,15 +343,6 @@ public class TopologyManager {
     addClusterConfigRequest(topology, new ClusterConfigurationRequest(ambariContext, topology, true,
       stackAdvisorBlueprintProcessor, securityType == SecurityType.KERBEROS));
 
    // Notify listeners that cluster configuration finished
    executor.submit(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        ambariEventPublisher.publish(new ClusterConfigFinishedEvent(clusterName));
        return Boolean.TRUE;
      }
    });

     // Process the logical request
     processRequest(request, topology, logicalRequest);
 
@@ -345,6 +354,17 @@ public class TopologyManager {
     return getRequestStatus(logicalRequest.getRequestId());
   }
 
  @Subscribe
  public void onClusterConfigFinishedEvent(ClusterConfigFinishedEvent event) {
    ManagedThreadPoolExecutor taskExecutor = topologyTaskExecutorServiceMap.get(event.getClusterId());
    if (taskExecutor == null) {
      LOG.error("Can't find executor service taskQueue not found for cluster: {} ", event.getClusterName());
    } else {
      LOG.info("Starting topology task ExecutorService for cluster: {}", event.getClusterName());
      taskExecutor.start();
    }
  }

 
   /**
    * Saves the quick links profile to the DB as an Ambari setting. Creates a new setting entity or updates the existing
@@ -941,16 +961,8 @@ public class TopologyManager {
     }
 
     LOG.info("TopologyManager.processAcceptedHostOffer: queue tasks for host = {} which responded {}", hostName, response.getAnswer());
    if (parallelTaskCreationEnabled) {
      executor.execute(new Runnable() { // do not start until cluster config done
        @Override
        public void run() {
          queueHostTasks(topology, response, hostName);
        }
      });
    } else {
      queueHostTasks(topology, response, hostName);
    }
    queueHostTasks(topology, response, hostName);

   }
 
   @Transactional
@@ -959,9 +971,23 @@ public class TopologyManager {
     persistedState.registerInTopologyHostInfo(host);
   }
 
  private ExecutorService getOrCreateTopologyTaskExecutor(Long clusterId) {
    ManagedThreadPoolExecutor topologyTaskExecutor = this.topologyTaskExecutorServiceMap.get(clusterId);
    if (topologyTaskExecutor == null) {
      LOG.info("Creating TopologyTaskExecutorService for clusterId: {}", clusterId);

      topologyTaskExecutor = new ManagedThreadPoolExecutor(topologyTaskExecutorThreadPoolSize,
              topologyTaskExecutorThreadPoolSize, 0L, TimeUnit.MILLISECONDS,
              new LinkedBlockingQueue<Runnable>());
      topologyTaskExecutorServiceMap.put(clusterId, topologyTaskExecutor);
    }
    return topologyTaskExecutor;
  }

   private void queueHostTasks(ClusterTopology topology, HostOfferResponse response, String hostName) {
     LOG.info("TopologyManager.processAcceptedHostOffer: queueing tasks for host = {}", hostName);
    response.executeTasks(taskExecutor, hostName, topology, ambariContext);
    ExecutorService executorService = getOrCreateTopologyTaskExecutor(topology.getClusterId());
    response.executeTasks(executorService, hostName, topology, ambariContext);
   }
 
   private void updateHostWithRackInfo(ClusterTopology topology, HostOfferResponse response, HostImpl host) {
@@ -1108,7 +1134,7 @@ public class TopologyManager {
     }
 
     ConfigureClusterTask configureClusterTask = configureClusterTaskFactory.createConfigureClusterTask(topology,
      configurationRequest);
      configurationRequest, ambariEventPublisher);
 
     AsyncCallableService<Boolean> asyncCallableService = new AsyncCallableService<>(configureClusterTask, timeout, delay,
         Executors.newScheduledThreadPool(1));
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/tasks/ConfigureClusterTask.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/tasks/ConfigureClusterTask.java
index 0ce59822c6..60eaa591b9 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/topology/tasks/ConfigureClusterTask.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/tasks/ConfigureClusterTask.java
@@ -22,6 +22,8 @@ import java.util.Collections;
 import java.util.Map;
 import java.util.concurrent.Callable;
 
import org.apache.ambari.server.events.ClusterConfigFinishedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
 import org.apache.ambari.server.security.authorization.internal.RunWithInternalSecurityContext;
 import org.apache.ambari.server.topology.ClusterConfigurationRequest;
 import org.apache.ambari.server.topology.ClusterTopology;
@@ -39,11 +41,14 @@ public class ConfigureClusterTask implements Callable<Boolean> {
 
   private ClusterConfigurationRequest configRequest;
   private ClusterTopology topology;
  private AmbariEventPublisher ambariEventPublisher;
 
   @AssistedInject
  public ConfigureClusterTask(@Assisted ClusterTopology topology, @Assisted ClusterConfigurationRequest configRequest) {
  public ConfigureClusterTask(@Assisted ClusterTopology topology, @Assisted ClusterConfigurationRequest configRequest,
                              @Assisted AmbariEventPublisher ambariEventPublisher) {
     this.configRequest = configRequest;
     this.topology = topology;
    this.ambariEventPublisher = ambariEventPublisher;
   }
 
   @Override
@@ -72,6 +77,12 @@ public class ConfigureClusterTask implements Callable<Boolean> {
       throw new Exception(e);
     }
 
    LOG.info("Cluster configuration finished successfully!");
    // Notify listeners that cluster configuration finished
    long clusterId = topology.getClusterId();
    ambariEventPublisher.publish(new ClusterConfigFinishedEvent(clusterId,
            topology.getAmbariContext().getClusterName(clusterId)));

     LOG.info("TopologyManager.ConfigureClusterTask: Exiting");
     return true;
   }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/tasks/ConfigureClusterTaskFactory.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/tasks/ConfigureClusterTaskFactory.java
index 9e3c15189e..558af308f7 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/topology/tasks/ConfigureClusterTaskFactory.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/tasks/ConfigureClusterTaskFactory.java
@@ -18,6 +18,7 @@
 
 package org.apache.ambari.server.topology.tasks;
 
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
 import org.apache.ambari.server.topology.ClusterConfigurationRequest;
 import org.apache.ambari.server.topology.ClusterTopology;
 
@@ -25,6 +26,6 @@ import org.apache.ambari.server.topology.ClusterTopology;
 public interface ConfigureClusterTaskFactory {
 
    ConfigureClusterTask createConfigureClusterTask(ClusterTopology topology, ClusterConfigurationRequest
    configRequest);
    configRequest, AmbariEventPublisher ambariEventPublisher);
 
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/utils/ManagedThreadPoolExecutor.java b/ambari-server/src/main/java/org/apache/ambari/server/utils/ManagedThreadPoolExecutor.java
new file mode 100644
index 0000000000..167c9cb23e
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/utils/ManagedThreadPoolExecutor.java
@@ -0,0 +1,83 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distribut
 * ed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.utils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ThreadPoolExecutor extension which is stopped by default and can be started & stopped.
 */
public class ManagedThreadPoolExecutor extends ThreadPoolExecutor {

  private volatile boolean isStopped;
  private final ReentrantLock pauseLock = new ReentrantLock();
  private final Condition unpaused = pauseLock.newCondition();

  public ManagedThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                            long keepAliveTime, TimeUnit unit,
                            BlockingQueue<Runnable> workQueue) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
            Executors.defaultThreadFactory());
    isStopped = true;
  }

  protected void beforeExecute(Thread t, Runnable r) {
    super.beforeExecute(t, r);
    pauseLock.lock();
    try {
      while (isStopped) {
        unpaused.await();
      }
    } catch (InterruptedException ie) {
      t.interrupt();
    } finally {
      pauseLock.unlock();
    }
  }

  public void start() {
    pauseLock.lock();
    try {
      isStopped = false;
      unpaused.signalAll();
    } finally {
      pauseLock.unlock();
    }
  }

  public void stop() {
    pauseLock.lock();
    try {
      isStopped = true;
    } finally {
      pauseLock.unlock();
    }
  }

  public boolean isRunning() {
    return !isStopped;
  }

}
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterDeployWithStartOnlyTest.java b/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterDeployWithStartOnlyTest.java
index c3248a381f..0daa20fc5e 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterDeployWithStartOnlyTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterDeployWithStartOnlyTest.java
@@ -391,7 +391,7 @@ public class ClusterDeployWithStartOnlyTest extends EasyMockSupport {
     ambariContext.persistInstallStateForUI(CLUSTER_NAME, STACK_NAME, STACK_VERSION);
     expectLastCall().once();
 
    expect(executor.submit(anyObject(AsyncCallableService.class))).andReturn(mockFuture).times(2);
    expect(executor.submit(anyObject(AsyncCallableService.class))).andReturn(mockFuture).times(1);
 
     persistedTopologyRequest = new PersistedTopologyRequest(1, request);
     expect(persistedState.getAllRequests()).andReturn(Collections.emptyMap()).once();
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterInstallWithoutStartOnComponentLevelTest.java b/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterInstallWithoutStartOnComponentLevelTest.java
index 372d0a14a0..bbf4fdbf01 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterInstallWithoutStartOnComponentLevelTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterInstallWithoutStartOnComponentLevelTest.java
@@ -368,7 +368,7 @@ public class ClusterInstallWithoutStartOnComponentLevelTest extends EasyMockSupp
     ambariContext.persistInstallStateForUI(CLUSTER_NAME, STACK_NAME, STACK_VERSION);
     expectLastCall().once();
 
    expect(executor.submit(anyObject(AsyncCallableService.class))).andReturn(mockFuture).times(2);
    expect(executor.submit(anyObject(AsyncCallableService.class))).andReturn(mockFuture).times(1);
 
     persistedTopologyRequest = new PersistedTopologyRequest(1, request);
     expect(persistedState.getAllRequests()).andReturn(Collections.emptyMap()).once();
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterInstallWithoutStartTest.java b/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterInstallWithoutStartTest.java
index 9620507240..059a8be735 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterInstallWithoutStartTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterInstallWithoutStartTest.java
@@ -363,7 +363,7 @@ public class ClusterInstallWithoutStartTest extends EasyMockSupport {
     ambariContext.persistInstallStateForUI(CLUSTER_NAME, STACK_NAME, STACK_VERSION);
     expectLastCall().once();
 
    expect(executor.submit(anyObject(AsyncCallableService.class))).andReturn(mockFuture).times(2);
    expect(executor.submit(anyObject(AsyncCallableService.class))).andReturn(mockFuture).times(1);
 
     persistedTopologyRequest = new PersistedTopologyRequest(1, request);
     expect(persistedState.getAllRequests()).andReturn(Collections.emptyMap()).once();
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/topology/ConfigureClusterTaskTest.java b/ambari-server/src/test/java/org/apache/ambari/server/topology/ConfigureClusterTaskTest.java
index feefcab842..b2dac8f01a 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/topology/ConfigureClusterTaskTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/topology/ConfigureClusterTaskTest.java
@@ -18,6 +18,7 @@
 
 package org.apache.ambari.server.topology;
 
import static org.easymock.EasyMock.anyObject;
 import static org.easymock.EasyMock.expect;
 import static org.easymock.EasyMock.replay;
 import static org.easymock.EasyMock.reset;
@@ -30,6 +31,7 @@ import java.util.HashMap;
 import java.util.Map;
 import java.util.concurrent.Executors;
 
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
 import org.apache.ambari.server.topology.tasks.ConfigureClusterTask;
 import org.easymock.EasyMockRule;
 import org.easymock.Mock;
@@ -60,12 +62,18 @@ public class ConfigureClusterTaskTest {
   @Mock(type = MockType.STRICT)
   private ClusterTopology clusterTopology;
 
  @Mock(type = MockType.STRICT)
  private AmbariContext ambariContext;

  @Mock(type = MockType.NICE)
  private AmbariEventPublisher ambariEventPublisher;

   private ConfigureClusterTask testSubject;
 
   @Before
   public void before() {
    reset(clusterConfigurationRequest, clusterTopology);
    testSubject = new ConfigureClusterTask(clusterTopology, clusterConfigurationRequest);
    reset(clusterConfigurationRequest, clusterTopology, ambariContext, ambariEventPublisher);
    testSubject = new ConfigureClusterTask(clusterTopology, clusterConfigurationRequest, ambariEventPublisher);
   }
 
   @Test
@@ -75,11 +83,15 @@ public class ConfigureClusterTaskTest {
     // is it OK to handle the non existence of hostgroups as a success?!
     expect(clusterConfigurationRequest.getRequiredHostGroups()).andReturn(Collections.emptyList());
     expect(clusterTopology.getHostGroupInfo()).andReturn(Collections.emptyMap());
    expect(clusterTopology.getClusterId()).andReturn(1L).anyTimes();
    expect(clusterTopology.getAmbariContext()).andReturn(ambariContext);
    expect(ambariContext.getClusterName(1L)).andReturn("testCluster");
 
     // this is only called if the "prerequisites" are satisfied
     clusterConfigurationRequest.process();
    ambariEventPublisher.publish(anyObject());
 
    replay(clusterConfigurationRequest, clusterTopology);
    replay(clusterConfigurationRequest, clusterTopology, ambariContext, ambariEventPublisher);
 
     // WHEN
     Boolean result = testSubject.call();
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/utils/ManagedThreadPoolExecutorTest.java b/ambari-server/src/test/java/org/apache/ambari/server/utils/ManagedThreadPoolExecutorTest.java
new file mode 100644
index 0000000000..e94b25c809
-- /dev/null
++ b/ambari-server/src/test/java/org/apache/ambari/server/utils/ManagedThreadPoolExecutorTest.java
@@ -0,0 +1,51 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import junit.framework.Assert;

public class ManagedThreadPoolExecutorTest {

  @Test
  public void testGetHostAndPortFromProperty() {

    ManagedThreadPoolExecutor  topologyTaskExecutor = new ManagedThreadPoolExecutor(1,
            1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());
    Future<Boolean> feature = topologyTaskExecutor.submit(new Callable<Boolean>() {
      @Override
      public Boolean call() {
        return Boolean.TRUE;
      }
    });

    Assert.assertTrue(!topologyTaskExecutor.isRunning());
    topologyTaskExecutor.start();
    Assert.assertTrue(topologyTaskExecutor.isRunning());
    topologyTaskExecutor.stop();
    Assert.assertTrue(!topologyTaskExecutor.isRunning());

  }
}
- 
2.19.1.windows.1

