From e10f6922eb972c581102b75d7fa50eabe0487fa2 Mon Sep 17 00:00:00 2001
From: Jonathan Hurley <jhurley@hortonworks.com>
Date: Mon, 9 Jan 2017 20:51:20 -0500
Subject: [PATCH] AMBARI-19435 - NodeManager restart fails during HOU if it is
 on same host as RM (jonathanhurley)

--
 .../server/controller/ControllerModule.java   |   2 +
 .../internal/UpgradeResourceProvider.java     |  13 +-
 .../ambari/server/stageplanner/RoleGraph.java | 120 +++++++++++++++++-
 .../apache/ambari/server/state/Cluster.java   |   8 ++
 .../ambari/server/state/UpgradeContext.java   |  43 ++++++-
 .../server/state/UpgradeContextFactory.java   |  47 +++++++
 .../server/state/cluster/ClusterImpl.java     |  17 +++
 .../stack/upgrade/HostOrderGrouping.java      | 114 ++++++++++++++---
 .../state/stack/upgrade/StageWrapper.java     |  11 ++
 .../state/stack/upgrade/TaskWrapper.java      |  14 +-
 .../server/agent/AgentResourceTest.java       |   3 +
 .../server/controller/KerberosHelperTest.java |   6 +
 ...ctiveWidgetLayoutResourceProviderTest.java |   3 +
 .../StackUpgradeConfigurationMergeTest.java   |   6 +-
 ...UserAuthorizationResourceProviderTest.java |   3 +
 .../internal/UserResourceProviderTest.java    |   3 +
 .../ambari/server/metadata/RoleGraphTest.java |  73 +++++++++++
 .../server/state/UpgradeHelperTest.java       |  74 +++++++----
 .../cluster/ClusterEffectiveVersionTest.java  |   4 +
 .../HDP/2.1.1/services/HBASE/metainfo.xml     |  44 +++++++
 20 files changed, 553 insertions(+), 55 deletions(-)
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/state/UpgradeContextFactory.java
 create mode 100644 ambari-server/src/test/resources/stacks/HDP/2.1.1/services/HBASE/metainfo.xml

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/ControllerModule.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/ControllerModule.java
index 9c93c6089e..b7c9e85ab8 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/ControllerModule.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/ControllerModule.java
@@ -124,6 +124,7 @@ import org.apache.ambari.server.state.ServiceComponentHostFactory;
 import org.apache.ambari.server.state.ServiceComponentImpl;
 import org.apache.ambari.server.state.ServiceFactory;
 import org.apache.ambari.server.state.ServiceImpl;
import org.apache.ambari.server.state.UpgradeContextFactory;
 import org.apache.ambari.server.state.cluster.ClusterFactory;
 import org.apache.ambari.server.state.cluster.ClusterImpl;
 import org.apache.ambari.server.state.cluster.ClustersImpl;
@@ -477,6 +478,7 @@ public class ControllerModule extends AbstractModule {
     install(new FactoryModuleBuilder().build(StackManagerFactory.class));
     install(new FactoryModuleBuilder().build(ExecutionCommandWrapperFactory.class));
     install(new FactoryModuleBuilder().build(MetricPropertyProviderFactory.class));
    install(new FactoryModuleBuilder().build(UpgradeContextFactory.class));
 
     bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
     bind(SecurityHelper.class).toInstance(SecurityHelperImpl.getInstance());
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/UpgradeResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/UpgradeResourceProvider.java
index 5191e835ad..4c01964337 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/UpgradeResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/UpgradeResourceProvider.java
@@ -97,6 +97,7 @@ import org.apache.ambari.server.state.ServiceInfo;
 import org.apache.ambari.server.state.StackId;
 import org.apache.ambari.server.state.StackInfo;
 import org.apache.ambari.server.state.UpgradeContext;
import org.apache.ambari.server.state.UpgradeContextFactory;
 import org.apache.ambari.server.state.UpgradeHelper;
 import org.apache.ambari.server.state.UpgradeHelper.UpgradeGroupHolder;
 import org.apache.ambari.server.state.stack.ConfigUpgradePack;
@@ -290,6 +291,13 @@ public class UpgradeResourceProvider extends AbstractControllerResourceProvider
   @Inject
   private static Gson s_gson;
 
  /**
   * Used to create instances of {@link UpgradeContext} with injected
   * dependencies.
   */
  @Inject
  private static UpgradeContextFactory s_upgradeContextFactory;

   static {
     // properties
     PROPERTY_IDS.add(UPGRADE_CLUSTER_NAME);
@@ -385,8 +393,8 @@ public class UpgradeResourceProvider extends AbstractControllerResourceProvider
           }
         }
 
        final UpgradeContext upgradeContext = new UpgradeContext(cluster, upgradeType, direction,
            requestMap);
        final UpgradeContext upgradeContext = s_upgradeContextFactory.create(cluster, upgradeType,
            direction, requestMap);
 
         UpgradePack upgradePack = validateRequest(upgradeContext);
         upgradeContext.setUpgradePack(upgradePack);
@@ -1475,7 +1483,6 @@ public class UpgradeResourceProvider extends AbstractControllerResourceProvider
     }
 
     s_commandExecutionHelper.get().addExecutionCommandsToStage(actionContext, stage, requestParams);

     request.addStages(Collections.singletonList(stage));
   }
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/stageplanner/RoleGraph.java b/ambari-server/src/main/java/org/apache/ambari/server/stageplanner/RoleGraph.java
index 404e4ffa64..65a86c00f7 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/stageplanner/RoleGraph.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/stageplanner/RoleGraph.java
@@ -18,13 +18,16 @@
 package org.apache.ambari.server.stageplanner;
 
 import java.util.ArrayList;
import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.TreeMap;
 
import org.apache.ambari.server.Role;
 import org.apache.ambari.server.RoleCommand;
 import org.apache.ambari.server.actionmanager.CommandExecutionType;
 import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
 import org.apache.ambari.server.actionmanager.Stage;
 import org.apache.ambari.server.actionmanager.StageFactory;
 import org.apache.ambari.server.metadata.RoleCommandOrder;
@@ -48,13 +51,20 @@ public class RoleGraph {
   @Inject
   private StageFactory stageFactory;
 
  /**
   * Used for created {@link HostRoleCommand}s when building structures to
   * represent an ordered set of stages.
   */
  @Inject
  private HostRoleCommandFactory hrcFactory;

   @AssistedInject
   public RoleGraph() {
   }
 
   @AssistedInject
   public RoleGraph(@Assisted RoleCommandOrder rd) {
    this.roleDependencies = rd;
    roleDependencies = rd;
   }
 
   public CommandExecutionType getCommandExecutionType() {
@@ -67,15 +77,41 @@ public class RoleGraph {
 
   /**
    * Given a stage builds a DAG of all execution commands within the stage.
   *
   * @see #getStages()
    */
   public void build(Stage stage) {
     if (stage == null) {
       throw new IllegalArgumentException("Null stage");
     }
    graph = new TreeMap<String, RoleGraphNode>();

    if (commandExecutionType == CommandExecutionType.DEPENDENCY_ORDERED) {
      LOG.info("Build stage with DEPENDENCY_ORDERED commandExecutionType: {} ",
          stage.getRequestContext());
    }

     initialStage = stage;
 
     Map<String, Map<String, HostRoleCommand>> hostRoleCommands = stage.getHostRoleCommands();
    build(hostRoleCommands);
  }

  /**
   * Initializes {@link #graph} with the supplied unordered commands. The
   * commands specified are in the following format: Input:
   * 
   * <pre>
   * {c6401={NAMENODE=STOP}, c6402={DATANODE=STOP}, NODEMANAGER=STOP}}
   * </pre>
   *
   * @param hostRoleCommands
   *          the unordered commands to build a DAG from. The map is keyed first
   *          by host and the for each host it is keyed by {@link Role} to
   *          {@link RoleCommand}.
   */
  private void build(Map<String, Map<String, HostRoleCommand>> hostRoleCommands) {
    graph = new TreeMap<String, RoleGraphNode>();

     for (String host : hostRoleCommands.keySet()) {
       for (String role : hostRoleCommands.get(host).keySet()) {
         HostRoleCommand hostRoleCommand = hostRoleCommands.get(host).get(role);
@@ -110,10 +146,9 @@ public class RoleGraph {
           }
         }
       }
    } else {
      LOG.info("Build stage with DEPENDENCY_ORDERED commandExecutionType: {} ", stage.getRequestContext());
     }
   }

   /**
    * This method return more detailed RoleCommand type. For now, i've added code
    * only for RESTART name of CUSTOM COMMAND, but in future i think all other will be added too.
@@ -136,7 +171,7 @@ public class RoleGraph {
     List<RoleGraphNode> firstStageNodes = new ArrayList<RoleGraphNode>();
     while (!graph.isEmpty()) {
       if (LOG.isDebugEnabled()) {
        LOG.debug(this.stringifyGraph());
        LOG.debug(stringifyGraph());
       }
 
       for (String role: graph.keySet()) {
@@ -151,7 +186,7 @@ public class RoleGraph {
       //Remove first stage nodes from the graph, we know that none of
       //these nodes have an incoming edges.
       for (RoleGraphNode rgn : firstStageNodes) {
        if (this.sameHostOptimization) {
        if (sameHostOptimization) {
           //Perform optimization
         }
         removeZeroInDegreeNode(rgn.getRole().toString());
@@ -161,12 +196,83 @@ public class RoleGraph {
     return stageList;
   }
 
  /**
   * Gets a representation of the role ordering of the specified commands
   * without constructing {@link Stage} instances. The commands to order are
   * supplied as mapping of host to role/command. Each item of the returned list
   * represents a single stage. The map is of host to commands. For example:
   * <br/>
   * <br/>
   * Input:
   * <pre>
   * {c6401={NAMENODE=STOP}, c6402={DATANODE=STOP}, NODEMANAGER=STOP}}
   * </pre>
   *
   * Output:
   * <pre>
   * [{c6402=[NODEMANAGER/STOP, DATANODE-STOP]}, c6401=[NAMENODE/STOP]]
   *
   * <pre>
   *
   * @param unorderedCommands
   *          a mapping of {@link Role} to {@link HostRoleCommand} by host.
   * @return and ordered list where each item represents a single stage and each
   *         stage's commands are mapped by host.
   */
  public List<Map<String, List<HostRoleCommand>>> getOrderedHostRoleCommands(
      Map<String, Map<String, HostRoleCommand>> unorderedCommands) {
    build(unorderedCommands);

    // represents an ordered list of stages
    List<Map<String, List<HostRoleCommand>>> orderedCommands = new ArrayList<>();

    List<RoleGraphNode> firstStageNodes = new ArrayList<RoleGraphNode>();
    while (!graph.isEmpty()) {
      for (String role : graph.keySet()) {
        RoleGraphNode rgn = graph.get(role);
        if (rgn.getInDegree() == 0) {
          firstStageNodes.add(rgn);
        }
      }

      // represents a stage
      Map<String, List<HostRoleCommand>> commandsPerHost = new HashMap<>();

      for (RoleGraphNode rgn : firstStageNodes) {
        // for every host for this stage, create the ordered commands
        for (String host : rgn.getHosts()) {
          List<HostRoleCommand> commands = commandsPerHost.get(host);
          if (null == commands) {
            commands = new ArrayList<>();
            commandsPerHost.put(host, commands);
          }

          HostRoleCommand hrc = hrcFactory.create(host, rgn.getRole(), null, rgn.getCommand());
          commands.add(hrc);
        }
      }

      // add the stage to the list of stages
      orderedCommands.add(commandsPerHost);

      // Remove first stage nodes from the graph, we know that none of
      // these nodes have an incoming edges.
      for (RoleGraphNode rgn : firstStageNodes) {
        removeZeroInDegreeNode(rgn.getRole().toString());
      }

      firstStageNodes.clear();
    }

    return orderedCommands;
  }

   /**
    * Assumes there are no incoming edges.
    */
   private synchronized void removeZeroInDegreeNode(String role) {
     RoleGraphNode nodeToRemove = graph.remove(role);
    for (RoleGraphNode edgeNode: nodeToRemove.getEdges()) {
    for (RoleGraphNode edgeNode : nodeToRemove.getEdges()) {
       edgeNode.decrementInDegree();
     }
   }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/state/Cluster.java b/ambari-server/src/main/java/org/apache/ambari/server/state/Cluster.java
index c6ae0502fb..4e37c926af 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/state/Cluster.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/state/Cluster.java
@@ -27,6 +27,7 @@ import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.controller.ClusterResponse;
 import org.apache.ambari.server.controller.ServiceConfigVersionResponse;
 import org.apache.ambari.server.events.ClusterConfigChangedEvent;
import org.apache.ambari.server.metadata.RoleCommandOrder;
 import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
 import org.apache.ambari.server.orm.entities.HostEntity;
 import org.apache.ambari.server.orm.entities.HostVersionEntity;
@@ -738,4 +739,11 @@ public interface Cluster {
    * @return number of hosts that form the cluster
    */
   int  getClusterSize();

  /**
   * Gets a new instance of a {@link RoleCommandOrder} for this cluster.
   *
   * @return the role command order instance (not {@code null}).
   */
  RoleCommandOrder getRoleCommandOrder();
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/state/UpgradeContext.java b/ambari-server/src/main/java/org/apache/ambari/server/state/UpgradeContext.java
index 8e7e5de334..1d51b0d818 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/state/UpgradeContext.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/state/UpgradeContext.java
@@ -26,13 +26,19 @@ import java.util.Set;
 
 import org.apache.ambari.annotations.Experimental;
 import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
 import org.apache.ambari.server.api.services.AmbariMetaInfo;
 import org.apache.ambari.server.stack.MasterHostResolver;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
 import org.apache.ambari.server.state.stack.UpgradePack;
 import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.Grouping;
 import org.apache.ambari.server.state.stack.upgrade.UpgradeScope;
 import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
 
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

 /**
  * Used to hold various helper objects required to process an upgrade pack.
  */
@@ -120,6 +126,20 @@ public class UpgradeContext {
 
   private UpgradeScope m_scope = UpgradeScope.ANY;
 
  /**
   * Used by some {@link Grouping}s to generate commands. It is exposed here
   * mainly for injection purposes since the XML is not created by Guice.
   */
  @Inject
  private HostRoleCommandFactory m_hrcFactory;

  /**
   * Used by some {@link Grouping}s to determine command ordering. It is exposed
   * here mainly for injection purposes since the XML is not created by Guice.
   */
  @Inject
  private RoleGraphFactory m_roleGraphFactory;

   /**
    * Constructor.
    *
@@ -132,8 +152,9 @@ public class UpgradeContext {
    * @param upgradeRequestMap
    *          the original map of paramters used to create the upgrade
    */
  public UpgradeContext(Cluster cluster, UpgradeType type, Direction direction,
      Map<String, Object> upgradeRequestMap) {
  @Inject
  public UpgradeContext(@Assisted Cluster cluster, @Assisted UpgradeType type,
      @Assisted Direction direction, @Assisted Map<String, Object> upgradeRequestMap) {
     m_cluster = cluster;
     m_type = type;
     m_direction = direction;
@@ -482,4 +503,22 @@ public class UpgradeContext {
   public boolean isScoped(UpgradeScope scope) {
     return m_scope.isScoped(scope);
   }

  /**
   * Gets the injected instance of a {@link RoleGraphFactory}.
   *
   * @return a {@link RoleGraphFactory} instance (never {@code null}).
   */
  public RoleGraphFactory getRoleGraphFactory() {
    return m_roleGraphFactory;
  }

  /**
   * Gets the injected instance of a {@link HostRoleCommandFactory}.
   *
   * @return a {@link HostRoleCommandFactory} instance (never {@code null}).
   */
  public HostRoleCommandFactory getHostRoleCommandFactory() {
    return m_hrcFactory;
  }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/state/UpgradeContextFactory.java b/ambari-server/src/main/java/org/apache/ambari/server/state/UpgradeContextFactory.java
new file mode 100644
index 0000000000..4b988e8b1f
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/state/UpgradeContextFactory.java
@@ -0,0 +1,47 @@
/**
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
package org.apache.ambari.server.state;

import java.util.Map;

import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;

/**
 * The {@link UpgradeContextFactory} is used to create dependency-injected
 * instances of {@link UpgradeContext}s.
 */
public interface UpgradeContextFactory {

  /**
   * Creates an {@link UpgradeContext} which is injected with dependencies.
   *
   * @param cluster
   *          the cluster that the upgrade is for
   * @param type
   *          the type of upgrade, either rolling or non_rolling
   * @param direction
   *          the direction for the upgrade
   * @param upgradeRequestMap
   *          the original map of paramters used to create the upgrade
   *
   * @return an initialized {@link UpgradeContext}.
   */
  UpgradeContext create(Cluster cluster, UpgradeType type, Direction direction,
      Map<String, Object> upgradeRequestMap);
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/state/cluster/ClusterImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/state/cluster/ClusterImpl.java
index 46e2f8e386..6455d6ea37 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/state/cluster/ClusterImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/state/cluster/ClusterImpl.java
@@ -47,6 +47,7 @@ import org.apache.ambari.server.ConfigGroupNotFoundException;
 import org.apache.ambari.server.DuplicateResourceException;
 import org.apache.ambari.server.ObjectNotFoundException;
 import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.RoleCommand;
 import org.apache.ambari.server.ServiceComponentHostNotFoundException;
 import org.apache.ambari.server.ServiceComponentNotFoundException;
 import org.apache.ambari.server.ServiceNotFoundException;
@@ -68,6 +69,8 @@ import org.apache.ambari.server.events.jpa.JPAEvent;
 import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
 import org.apache.ambari.server.events.publishers.JPAEventPublisher;
 import org.apache.ambari.server.logging.LockFactory;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
 import org.apache.ambari.server.orm.RequiresSession;
 import org.apache.ambari.server.orm.cache.HostConfigMapping;
 import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
@@ -306,6 +309,12 @@ public class ClusterImpl implements Cluster {
   @Inject
   private JPAEventPublisher jpaEventPublisher;
 
  /**
   * Used for getting instances of {@link RoleCommand} for this cluster.
   */
  @Inject
  private RoleCommandOrderProvider roleCommandOrderProvider;

   /**
    * A simple cache for looking up {@code cluster-env} properties for a cluster.
    * This map is changed whenever {{cluster-env}} is changed and we receive a
@@ -3474,4 +3483,12 @@ public class ClusterImpl implements Cluster {
 
     m_clusterPropertyCache.clear();
   }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoleCommandOrder getRoleCommandOrder() {
    return roleCommandOrderProvider.getRoleCommandOrder(this);
  }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/state/stack/upgrade/HostOrderGrouping.java b/ambari-server/src/main/java/org/apache/ambari/server/state/stack/upgrade/HostOrderGrouping.java
index 5d723f5715..abb2aab5f2 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/state/stack/upgrade/HostOrderGrouping.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/state/stack/upgrade/HostOrderGrouping.java
@@ -19,13 +19,22 @@ package org.apache.ambari.server.state.stack.upgrade;
 
 import java.util.ArrayList;
 import java.util.Collections;
import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import javax.xml.bind.annotation.XmlType;
 
 import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.metadata.RoleCommandOrder;
 import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.stageplanner.RoleGraph;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
 import org.apache.ambari.server.state.Cluster;
 import org.apache.ambari.server.state.ComponentInfo;
 import org.apache.ambari.server.state.ServiceComponentHost;
@@ -114,9 +123,16 @@ public class HostOrderGrouping extends Grouping {
     }
 
     /**
     * @param upgradeContext  the context
     * @param hosts           the list of hostnames
     * @return  the wrappers for a host
     * Builds the stages for each host which typically consist of a STOP, a
     * manual wait, and a START. The starting of components can be a single
     * stage or may consist of several stages if the host components have
     * dependencies on each other.
     *
     * @param upgradeContext
     *          the context
     * @param hosts
     *          the list of hostnames
     * @return the wrappers for a host
      */
     private List<StageWrapper> buildHosts(UpgradeContext upgradeContext, List<String> hosts) {
       if (CollectionUtils.isEmpty(hosts)) {
@@ -126,11 +142,20 @@ public class HostOrderGrouping extends Grouping {
       Cluster cluster = upgradeContext.getCluster();
       List<StageWrapper> wrappers = new ArrayList<>();
 
      for (String hostName : hosts) {
      HostRoleCommandFactory hrcFactory = upgradeContext.getHostRoleCommandFactory();
 
      for (String hostName : hosts) {
        // initialize the collection for all stop tasks for every component on
        // the host
         List<TaskWrapper> stopTasks = new ArrayList<>();
        List<TaskWrapper> upgradeTasks = new ArrayList<>();
 
        // initialize the collection which will be passed into the RoleGraph for
        // ordering
        Map<String, Map<String, HostRoleCommand>> restartCommandsForHost = new HashMap<>();
        Map<String, HostRoleCommand> restartCommandsByRole = new HashMap<>();
        restartCommandsForHost.put(hostName, restartCommandsByRole);

        // iterating over every host component, build the commands
         for (ServiceComponentHost sch : cluster.getServiceComponentHosts(hostName)) {
           if (!isVersionAdvertised(upgradeContext, sch)) {
             continue;
@@ -149,31 +174,90 @@ public class HostOrderGrouping extends Grouping {
             continue;
           }
 
          // create a STOP task for this host component
           if (!sch.isClientComponent()) {
             stopTasks.add(new TaskWrapper(sch.getServiceName(), sch.getServiceComponentName(),
                 Collections.singleton(hostName), new StopTask()));
           }
 
          // !!! simple restart will do
          upgradeTasks.add(new TaskWrapper(sch.getServiceName(), sch.getServiceComponentName(),
              Collections.singleton(hostName), new RestartTask()));
          // generate a placeholder HRC that can be used to generate the
          // dependency graph - we must use START here since that's what the
          // role command order is defined with - each of these will turn into a
          // RESTART when we create the wrappers later on
          Role role = Role.valueOf(sch.getServiceComponentName());
          HostRoleCommand hostRoleCommand = hrcFactory.create(hostName, role, null,
              RoleCommand.START);

          // add the newly created HRC RESTART
          restartCommandsByRole.put(role.name(), hostRoleCommand);
         }
 
        if (stopTasks.isEmpty() && upgradeTasks.isEmpty()) {
          LOG.info("No tasks for {}", hostName);
        // short circuit and move to the next host if there are no commands
        if (stopTasks.isEmpty() && restartCommandsByRole.isEmpty()) {
          LOG.info("There were no {} commands generated for {}",
              upgradeContext.getDirection().getText(false), hostName);

           continue;
         }
 
        // build the single STOP stage
         StageWrapper stopWrapper = new StageWrapper(StageWrapper.Type.STOP, String.format("Stop on %s", hostName),
             stopTasks.toArray(new TaskWrapper[stopTasks.size()]));
 
        StageWrapper startWrapper = new StageWrapper(StageWrapper.Type.RESTART, String.format("Start on %s", hostName),
            upgradeTasks.toArray(new TaskWrapper[upgradeTasks.size()]));
        // now process the HRCs created so that we can create the appropriate
        // stage/task wrappers for the RESTARTs
        RoleGraphFactory roleGraphFactory = upgradeContext.getRoleGraphFactory();
        RoleCommandOrder roleCommandOrder = cluster.getRoleCommandOrder();
        RoleGraph roleGraph = roleGraphFactory.createNew(roleCommandOrder);
        List<Map<String, List<HostRoleCommand>>> stages = roleGraph.getOrderedHostRoleCommands(
            restartCommandsForHost);

        // initialize the list of stage wrappers
        List<StageWrapper> stageWrappers = new ArrayList<>();

        // for every stage, create a stage wrapper around the tasks
        int phaseCounter = 1;
        for (Map<String, List<HostRoleCommand>> stage : stages) {
          List<HostRoleCommand> stageCommandsForHost = stage.get(hostName);
          String stageTitle = String.format("Starting components on %s (phase %d)", hostName,
              phaseCounter++);

          // create task wrappers
          List<TaskWrapper> taskWrappers = new ArrayList<>();
          for (HostRoleCommand command : stageCommandsForHost) {
            StackId stackId = upgradeContext.getEffectiveStackId();
            String componentName = command.getRole().name();

            String serviceName = null;

            try {
              AmbariMetaInfo ambariMetaInfo = upgradeContext.getAmbariMetaInfo();
              serviceName = ambariMetaInfo.getComponentToService(stackId.getStackName(),
                  stackId.getStackVersion(), componentName);
            } catch (AmbariException ambariException) {
              LOG.error("Unable to lookup service by component {} for stack {}-{}", componentName,
                  stackId.getStackName(), stackId.getStackVersion());
            }

            TaskWrapper taskWrapper = new TaskWrapper(serviceName, componentName,
                Collections.singleton(hostName), new RestartTask());

            taskWrappers.add(taskWrapper);
          }
 
        String message = String.format("Please acknowledge that host %s has been prepared.", hostName);
          if (!taskWrappers.isEmpty()) {
            StageWrapper startWrapper = new StageWrapper(StageWrapper.Type.RESTART, stageTitle,
                taskWrappers.toArray(new TaskWrapper[taskWrappers.size()]));

            stageWrappers.add(startWrapper);
          }
        }
 
        // create the manual task between the STOP and START stages
         ManualTask mt = new ManualTask();
        String message = String.format("Please acknowledge that host %s has been prepared.", hostName);
         mt.messages.add(message);

         JsonObject structuredOut = new JsonObject();
         structuredOut.addProperty(TYPE, HostOrderItem.HostOrderActionType.HOST_UPGRADE.toString());
         structuredOut.addProperty(HOST, hostName);
@@ -184,9 +268,9 @@ public class HostOrderGrouping extends Grouping {
 
         wrappers.add(stopWrapper);
         wrappers.add(manualWrapper);
        // !!! TODO install_packages for hdp and conf-select changes.  Hopefully these will no-op.
        wrappers.add(startWrapper);
 
        // !!! TODO install_packages for hdp and conf-select changes.  Hopefully these will no-op.
        wrappers.addAll(stageWrappers);
       }
 
       return wrappers;
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/state/stack/upgrade/StageWrapper.java b/ambari-server/src/main/java/org/apache/ambari/server/state/stack/upgrade/StageWrapper.java
index 5ec7ddb25f..669d50fe14 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/state/stack/upgrade/StageWrapper.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/state/stack/upgrade/StageWrapper.java
@@ -25,6 +25,7 @@ import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
import com.google.common.base.Objects;
 import com.google.gson.Gson;
 
 /**
@@ -152,4 +153,14 @@ public class StageWrapper {
     START,
     CONFIGURE
   }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return Objects.toStringHelper(this).add("type", type)
        .add("text",text)
        .omitNullValues().toString();
  }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/state/stack/upgrade/TaskWrapper.java b/ambari-server/src/main/java/org/apache/ambari/server/state/stack/upgrade/TaskWrapper.java
index 5fdf91c321..11e27cfb9f 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/state/stack/upgrade/TaskWrapper.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/state/stack/upgrade/TaskWrapper.java
@@ -23,6 +23,8 @@ import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
import com.google.common.base.Objects;

 /**
  * Aggregates all upgrade tasks for a HostComponent into one wrapper.
  */
@@ -43,7 +45,7 @@ public class TaskWrapper {
   public TaskWrapper(String s, String c, Set<String> hosts, Task... tasks) {
     this(s, c, hosts, null, Arrays.asList(tasks));
   }
  

   /**
    * @param s the service name for the tasks
    * @param c the component name for the tasks
@@ -92,10 +94,16 @@ public class TaskWrapper {
     return hosts;
   }
 

  /**
   * {@inheritDoc}
   */
   @Override
   public String toString() {
    return service + ":" + component + ":" + tasks + ":" + hosts;
    return Objects.toStringHelper(this).add("service", service)
        .add("component", component)
        .add("tasks", tasks)
        .add("hosts", hosts)
        .omitNullValues().toString();
   }
 
   /**
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/agent/AgentResourceTest.java b/ambari-server/src/test/java/org/apache/ambari/server/agent/AgentResourceTest.java
index 674025cbd1..17b1e27ae8 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/agent/AgentResourceTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/agent/AgentResourceTest.java
@@ -36,6 +36,8 @@ import org.apache.ambari.server.actionmanager.StageFactory;
 import org.apache.ambari.server.agent.rest.AgentResource;
 import org.apache.ambari.server.api.services.AmbariMetaInfo;
 import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.metadata.CachedRoleCommandOrderProvider;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
 import org.apache.ambari.server.orm.DBAccessor;
 import org.apache.ambari.server.orm.dao.HostDAO;
 import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
@@ -316,6 +318,7 @@ public class AgentResourceTest extends RandomPortJerseyTest {
       bind(HostDAO.class).toInstance(createNiceMock(HostDAO.class));
       bind(Clusters.class).toInstance(createNiceMock(Clusters.class));
       bind(PersistedState.class).toInstance(createNiceMock(PersistedState.class));
      bind(RoleCommandOrderProvider.class).to(CachedRoleCommandOrderProvider.class);
     }
 
     private void installDependencies() {
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/KerberosHelperTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/KerberosHelperTest.java
index 8a70f0cf2b..91cd6084ce 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/KerberosHelperTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/KerberosHelperTest.java
@@ -58,6 +58,8 @@ import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.Role;
 import org.apache.ambari.server.actionmanager.ActionManager;
 import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
 import org.apache.ambari.server.actionmanager.RequestFactory;
 import org.apache.ambari.server.actionmanager.Stage;
 import org.apache.ambari.server.actionmanager.StageFactory;
@@ -70,7 +72,9 @@ import org.apache.ambari.server.configuration.Configuration;
 import org.apache.ambari.server.controller.internal.RequestStageContainer;
 import org.apache.ambari.server.controller.spi.ClusterController;
 import org.apache.ambari.server.controller.utilities.KerberosChecker;
import org.apache.ambari.server.metadata.CachedRoleCommandOrderProvider;
 import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
 import org.apache.ambari.server.orm.DBAccessor;
 import org.apache.ambari.server.orm.dao.ArtifactDAO;
 import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
@@ -247,6 +251,8 @@ public class KerberosHelperTest extends EasyMockSupport {
         bind(AuditLogger.class).toInstance(createNiceMock(AuditLogger.class));
         bind(ArtifactDAO.class).toInstance(createNiceMock(ArtifactDAO.class));
         bind(KerberosPrincipalDAO.class).toInstance(createNiceMock(KerberosPrincipalDAO.class));
        bind(RoleCommandOrderProvider.class).to(CachedRoleCommandOrderProvider.class);
        bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
 
         requestStaticInjection(KerberosChecker.class);
       }
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/ActiveWidgetLayoutResourceProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/ActiveWidgetLayoutResourceProviderTest.java
index 4b3782f709..5cce3fc2dc 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/ActiveWidgetLayoutResourceProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/ActiveWidgetLayoutResourceProviderTest.java
@@ -34,6 +34,8 @@ import javax.persistence.EntityManager;
 
 import org.apache.ambari.server.actionmanager.ActionDBAccessor;
 import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
 import org.apache.ambari.server.actionmanager.StageFactory;
 import org.apache.ambari.server.api.services.AmbariMetaInfo;
 import org.apache.ambari.server.controller.AbstractRootServiceResponseFactory;
@@ -405,6 +407,7 @@ public class ActiveWidgetLayoutResourceProviderTest extends EasyMockSupport {
         bind(HostRoleCommandDAO.class).toInstance(createMock(HostRoleCommandDAO.class));
         bind(HookContextFactory.class).toInstance(createMock(HookContextFactory.class));
         bind(HookService.class).toInstance(createMock(HookService.class));
        bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
       }
     });
   }
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/StackUpgradeConfigurationMergeTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/StackUpgradeConfigurationMergeTest.java
index 27d3d7bb94..1c45589c47 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/StackUpgradeConfigurationMergeTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/StackUpgradeConfigurationMergeTest.java
@@ -24,6 +24,8 @@ import java.util.Map;
 import javax.persistence.EntityManager;
 
 import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
 import org.apache.ambari.server.actionmanager.RequestFactory;
 import org.apache.ambari.server.actionmanager.StageFactory;
 import org.apache.ambari.server.api.services.AmbariMetaInfo;
@@ -53,6 +55,7 @@ import org.apache.ambari.server.state.ServiceComponentHostFactory;
 import org.apache.ambari.server.state.ServiceFactory;
 import org.apache.ambari.server.state.ServiceInfo;
 import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.UpgradeContextFactory;
 import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
 import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
 import org.apache.ambari.server.state.stack.OsFamily;
@@ -295,7 +298,8 @@ public class StackUpgradeConfigurationMergeTest extends EasyMockSupport {
       binder.bind(RepositoryVersionDAO.class).toInstance(createNiceMock(RepositoryVersionDAO.class));
       binder.bind(HookContextFactory.class).toInstance(createMock(HookContextFactory.class));
       binder.bind(HookService.class).toInstance(createMock(HookService.class));

      binder.install(new FactoryModuleBuilder().build(UpgradeContextFactory.class));
      binder.bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
 
       binder.requestStaticInjection(UpgradeResourceProvider.class);
     }
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/UserAuthorizationResourceProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/UserAuthorizationResourceProviderTest.java
index 37c48c3a24..fd96c8e1f3 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/UserAuthorizationResourceProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/UserAuthorizationResourceProviderTest.java
@@ -32,6 +32,8 @@ import javax.persistence.EntityManager;
 
 import org.apache.ambari.server.actionmanager.ActionDBAccessor;
 import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
 import org.apache.ambari.server.actionmanager.StageFactory;
 import org.apache.ambari.server.api.services.AmbariMetaInfo;
 import org.apache.ambari.server.controller.AbstractRootServiceResponseFactory;
@@ -417,6 +419,7 @@ public class UserAuthorizationResourceProviderTest extends EasyMockSupport {
         bind(HostRoleCommandDAO.class).toInstance(createMock(HostRoleCommandDAO.class));
         bind(HookContextFactory.class).toInstance(createMock(HookContextFactory.class));
         bind(HookService.class).toInstance(createMock(HookService.class));
        bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
       }
     });
   }
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/UserResourceProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/UserResourceProviderTest.java
index b8e027f280..cc0f2b6846 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/UserResourceProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/UserResourceProviderTest.java
@@ -34,6 +34,8 @@ import javax.persistence.EntityManager;
 
 import org.apache.ambari.server.actionmanager.ActionDBAccessor;
 import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
 import org.apache.ambari.server.actionmanager.RequestFactory;
 import org.apache.ambari.server.actionmanager.StageFactory;
 import org.apache.ambari.server.api.services.AmbariMetaInfo;
@@ -252,6 +254,7 @@ public class UserResourceProviderTest extends EasyMockSupport {
         bind(HostRoleCommandDAO.class).toInstance(createMock(HostRoleCommandDAO.class));
         bind(HookService.class).toInstance(createMock(HookService.class));
         bind(HookContextFactory.class).toInstance(createMock(HookContextFactory.class));
        bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
       }
     });
   }
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/metadata/RoleGraphTest.java b/ambari-server/src/test/java/org/apache/ambari/server/metadata/RoleGraphTest.java
index 53686aa0c0..f04efde611 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/metadata/RoleGraphTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/metadata/RoleGraphTest.java
@@ -22,16 +22,25 @@ package org.apache.ambari.server.metadata;
 import static org.mockito.Mockito.mock;
 import static org.mockito.Mockito.when;
 
import java.util.HashMap;
import java.util.List;
import java.util.Map;

 import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.Role;
 import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
 import org.apache.ambari.server.orm.GuiceJpaInitializer;
 import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.stageplanner.RoleGraph;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
 import org.apache.ambari.server.stageplanner.RoleGraphNode;
 import org.apache.ambari.server.state.Service;
 import org.apache.ambari.server.state.ServiceComponent;
 import org.apache.ambari.server.state.StackId;
 import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.hadoop.metrics2.sink.relocated.google.common.collect.Lists;
 import org.junit.After;
 import org.junit.Before;
 import org.junit.Test;
@@ -46,12 +55,16 @@ public class RoleGraphTest {
 
   private Injector injector;
   private RoleCommandOrderProvider roleCommandOrderProvider;
  private RoleGraphFactory roleGraphFactory;
  private HostRoleCommandFactory hrcFactory;
 
   @Before
   public void setup() throws Exception {
     injector = Guice.createInjector(new InMemoryDefaultTestModule());
     injector.getInstance(GuiceJpaInitializer.class);
     roleCommandOrderProvider = injector.getInstance(RoleCommandOrderProvider.class);
    roleGraphFactory = injector.getInstance(RoleGraphFactory.class);
    hrcFactory = injector.getInstance(HostRoleCommandFactory.class);
   }
 
   @After
@@ -139,4 +152,64 @@ public class RoleGraphTest {
     Assert.assertEquals(1, rco.order(nn_start, zk_server_start));
     Assert.assertEquals(1, rco.order(zkfc_start, nn_start));
   }

  /**
   * Tests the ordering of
   * {@link RoleGraph#getOrderedHostRoleCommands(java.util.Map)}.
   *
   * @throws AmbariException
   */
  @Test
  public void testGetOrderedHostRoleCommands() throws AmbariException {
    ClusterImpl cluster = mock(ClusterImpl.class);
    when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP-2.0.6"));
    when(cluster.getClusterId()).thenReturn(1L);

    RoleCommandOrder rco = roleCommandOrderProvider.getRoleCommandOrder(cluster);
    RoleGraph roleGraph = roleGraphFactory.createNew(rco);

    Map<String, Map<String, HostRoleCommand>> unorderedCommands = new HashMap<>();
    Map<String, HostRoleCommand> c6401Commands = new HashMap<>();
    Map<String, HostRoleCommand> c6402Commands = new HashMap<>();
    Map<String, HostRoleCommand> c6403Commands = new HashMap<>();

    HostRoleCommand hrcNameNode = hrcFactory.create("c6041", Role.NAMENODE, null, RoleCommand.START);
    HostRoleCommand hrcZooKeeperHost1 = hrcFactory.create("c6041", Role.ZOOKEEPER_SERVER, null, RoleCommand.START);
    HostRoleCommand hrcHBaseMaster = hrcFactory.create("c6042", Role.HBASE_MASTER, null, RoleCommand.START);
    HostRoleCommand hrcZooKeeperHost3 = hrcFactory.create("c6043", Role.ZOOKEEPER_SERVER, null, RoleCommand.START);

    c6401Commands.put(hrcNameNode.getRole().name(), hrcNameNode);
    c6401Commands.put(hrcZooKeeperHost1.getRole().name(), hrcZooKeeperHost1);
    c6402Commands.put(hrcHBaseMaster.getRole().name(), hrcHBaseMaster);
    c6403Commands.put(hrcZooKeeperHost3.getRole().name(), hrcZooKeeperHost3);

    unorderedCommands.put("c6401", c6401Commands);
    unorderedCommands.put("c6402", c6402Commands);
    unorderedCommands.put("c6403", c6403Commands);

    List<Map<String, List<HostRoleCommand>>> stages = roleGraph.getOrderedHostRoleCommands(unorderedCommands);

    Assert.assertEquals(2, stages.size());

    Map<String, List<HostRoleCommand>> stage1 = stages.get(0);
    Map<String, List<HostRoleCommand>> stage2 = stages.get(1);

    Assert.assertEquals(2, stage1.size());
    Assert.assertEquals(1, stage2.size());

    List<HostRoleCommand> stage1CommandsHost1 = stage1.get("c6401");
    List<HostRoleCommand> stage1CommandsHost3 = stage1.get("c6403");
    List<HostRoleCommand> stage2CommandsHost2 = stage2.get("c6402");

    Assert.assertEquals(3, stage1CommandsHost1.size() + stage1CommandsHost3.size());
    Assert.assertEquals(1, stage2CommandsHost2.size());

    List<Role> stage1Roles = Lists.newArrayList(stage1CommandsHost1.get(0).getRole(),
        stage1CommandsHost1.get(1).getRole(), stage1CommandsHost3.get(0).getRole());

    Assert.assertTrue(stage1Roles.contains(Role.NAMENODE));
    Assert.assertTrue(stage1Roles.contains(Role.ZOOKEEPER_SERVER));
    Assert.assertEquals(Role.ZOOKEEPER_SERVER, stage1CommandsHost3.get(0).getRole());
    Assert.assertEquals(Role.HBASE_MASTER, stage2CommandsHost2.get(0).getRole());
  }
 }
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/state/UpgradeHelperTest.java b/ambari-server/src/test/java/org/apache/ambari/server/state/UpgradeHelperTest.java
index ea1f18afdf..0d1a2fa9ec 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/state/UpgradeHelperTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/state/UpgradeHelperTest.java
@@ -107,6 +107,7 @@ public class UpgradeHelperTest {
   private ConfigHelper m_configHelper;
   private AmbariManagementController m_managementController;
   private Gson m_gson = new Gson();
  private UpgradeContextFactory m_upgradeContextFactory;
 
   /**
    * Because test cases need to share config mocks, put common ones in this function.
@@ -147,6 +148,7 @@ public class UpgradeHelperTest {
     m_upgradeHelper = injector.getInstance(UpgradeHelper.class);
     m_masterHostResolver = EasyMock.createMock(MasterHostResolver.class);
     m_managementController = injector.getInstance(AmbariManagementController.class);
    m_upgradeContextFactory = injector.getInstance(UpgradeContextFactory.class);
 
     // Set the authenticated user
     // TODO: remove this or replace the authenticated user to test authorization rules
@@ -1879,9 +1881,15 @@ public class UpgradeHelperTest {
     assertTrue(groups.isEmpty());
   }
 
  /**
   * Tests {@link UpgradeType#HOST_ORDERED}, specifically that the orchestration
   * can properly expand the single {@link HostOrderGrouping} and create the
   * correct stages based on the dependencies of the components.
   *
   * @throws Exception
   */
   @Test
   public void testHostGroupingOrchestration() throws Exception {

     Clusters clusters = injector.getInstance(Clusters.class);
     ServiceFactory serviceFactory = injector.getInstance(ServiceFactory.class);
 
@@ -1894,12 +1902,13 @@ public class UpgradeHelperTest {
 
     helper.getOrCreateRepositoryVersion(stackId,
         c.getDesiredStackVersion().getStackVersion());
    helper.getOrCreateRepositoryVersion(stackId2,"2.2.0");
    helper.getOrCreateRepositoryVersion(stackId2, "2.2.0");
 
     c.createClusterVersion(stackId,
         c.getDesiredStackVersion().getStackVersion(), "admin",
         RepositoryVersionState.INSTALLING);
 
    // create 2 hosts
     for (int i = 0; i < 2; i++) {
       String hostName = "h" + (i+1);
       clusters.addHost(hostName);
@@ -1914,19 +1923,24 @@ public class UpgradeHelperTest {
       clusters.mapHostToCluster(hostName, clusterName);
     }
 
    // !!! add storm
    // add ZK Server to both hosts, and then Nimbus to only 1 - this will test
    // how the HOU breaks out dependencies into stages
     c.addService(serviceFactory.createNew(c, "ZOOKEEPER"));

    Service s = c.getService("ZOOKEEPER");
    ServiceComponent sc = s.addServiceComponent("ZOOKEEPER_SERVER");
    ServiceComponentHost sch1 = sc.addServiceComponentHost("h1");
    ServiceComponentHost sch2 = sc.addServiceComponentHost("h2");
    c.addService(serviceFactory.createNew(c, "HBASE"));
    Service zookeeper = c.getService("ZOOKEEPER");
    Service hbase = c.getService("HBASE");
    ServiceComponent zookeeperServer = zookeeper.addServiceComponent("ZOOKEEPER_SERVER");
    ServiceComponentHost zookeeperServer1 = zookeeperServer.addServiceComponentHost("h1");
    ServiceComponentHost zookeeperServer2 = zookeeperServer.addServiceComponentHost("h2");
    ServiceComponent hbaseMaster = hbase.addServiceComponent("HBASE_MASTER");
    ServiceComponentHost hbaseMaster1 = hbaseMaster.addServiceComponentHost("h1");
 
     // !!! make a custom grouping
     HostOrderItem hostItem = new HostOrderItem(HostOrderActionType.HOST_UPGRADE,
         Lists.newArrayList("h1", "h2"));

     HostOrderItem checkItem = new HostOrderItem(HostOrderActionType.SERVICE_CHECK,
        Lists.newArrayList("ZOOKEEPER", "STORM"));
        Lists.newArrayList("ZOOKEEPER", "HBASE"));
 
     Grouping g = new HostOrderGrouping();
     ((HostOrderGrouping) g).setHostOrderItems(Lists.newArrayList(hostItem, checkItem));
@@ -1943,9 +1957,11 @@ public class UpgradeHelperTest {
     field.setAccessible(true);
     field.set(upgradePack, UpgradeType.HOST_ORDERED);
 

     MasterHostResolver resolver = new MasterHostResolver(m_configHelper, c);
    UpgradeContext context = new UpgradeContext(c, UpgradeType.HOST_ORDERED, Direction.UPGRADE, new HashMap<String, Object>());

    UpgradeContext context = m_upgradeContextFactory.create(c, UpgradeType.HOST_ORDERED,
        Direction.UPGRADE, new HashMap<String, Object>());

     context.setResolver(resolver);
     context.setSourceAndTargetStacks(stackId, stackId2);
     context.setVersion("2.2.0");
@@ -1954,13 +1970,13 @@ public class UpgradeHelperTest {
     assertEquals(1, groups.size());
 
     UpgradeGroupHolder holder = groups.get(0);
    assertEquals(7, holder.items.size());
    assertEquals(9, holder.items.size());
 
    for (int i = 0; i < 6; i++) {
    for (int i = 0; i < 7; i++) {
       StageWrapper w = holder.items.get(i);
      if (i == 0 || i == 3) {
      if (i == 0 || i == 4) {
         assertEquals(StageWrapper.Type.STOP, w.getType());
      } else if (i == 1 || i == 4) {
      } else if (i == 1 || i == 5) {
         assertEquals(StageWrapper.Type.SERVER_SIDE_ACTION, w.getType());
         assertEquals(1, w.getTasks().size());
         assertEquals(1, w.getTasks().get(0).getTasks().size());
@@ -1976,33 +1992,43 @@ public class UpgradeHelperTest {
         assertEquals(StageWrapper.Type.RESTART, w.getType());
       }
     }
    assertEquals(StageWrapper.Type.SERVICE_CHECK, holder.items.get(6).getType());

    assertEquals(StageWrapper.Type.SERVICE_CHECK, holder.items.get(7).getType());
    assertEquals(StageWrapper.Type.SERVICE_CHECK, holder.items.get(8).getType());
 
     // !!! test downgrade when all host components have failed
    sch1.setVersion("2.1.1");
    sch2.setVersion("2.1.1");
    zookeeperServer1.setVersion("2.1.1");
    zookeeperServer2.setVersion("2.1.1");
    hbaseMaster1.setVersion("2.1.1");
     resolver = new MasterHostResolver(m_configHelper, c, "2.1.1");
    context = new UpgradeContext(c, UpgradeType.HOST_ORDERED, Direction.DOWNGRADE, new HashMap<String, Object>());

    m_upgradeContextFactory.create(c, UpgradeType.HOST_ORDERED, Direction.DOWNGRADE,
        new HashMap<String, Object>());

     context.setResolver(resolver);
     context.setSourceAndTargetStacks(stackId2, stackId);
     context.setVersion("2.1.1");
     groups = m_upgradeHelper.createSequence(upgradePack, context);
 
     assertEquals(1, groups.size());
    assertEquals(1, groups.get(0).items.size());
    assertEquals(2, groups.get(0).items.size());
 
     // !!! test downgrade when one of the hosts had failed
    sch1.setVersion("2.1.1");
    sch2.setVersion("2.2.0");
    zookeeperServer1.setVersion("2.1.1");
    zookeeperServer2.setVersion("2.2.0");
    hbaseMaster1.setVersion("2.1.1");
     resolver = new MasterHostResolver(m_configHelper, c, "2.1.1");
    context = new UpgradeContext(c, UpgradeType.HOST_ORDERED, Direction.DOWNGRADE, new HashMap<String, Object>());

    m_upgradeContextFactory.create(c, UpgradeType.HOST_ORDERED, Direction.DOWNGRADE,
        new HashMap<String, Object>());

     context.setResolver(resolver);
     context.setSourceAndTargetStacks(stackId2, stackId);
     context.setVersion("2.1.1");
     groups = m_upgradeHelper.createSequence(upgradePack, context);
 
     assertEquals(1, groups.size());
    assertEquals(4, groups.get(0).items.size());
    assertEquals(5, groups.get(0).items.size());
   }
 
   /**
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/state/cluster/ClusterEffectiveVersionTest.java b/ambari-server/src/test/java/org/apache/ambari/server/state/cluster/ClusterEffectiveVersionTest.java
index 8ba891a702..d01249d63e 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/state/cluster/ClusterEffectiveVersionTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/state/cluster/ClusterEffectiveVersionTest.java
@@ -34,6 +34,8 @@ import org.apache.ambari.server.controller.spi.ClusterController;
 import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
 import org.apache.ambari.server.hooks.HookContextFactory;
 import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.metadata.CachedRoleCommandOrderProvider;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
 import org.apache.ambari.server.orm.DBAccessor;
 import org.apache.ambari.server.orm.dao.ClusterDAO;
 import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
@@ -277,6 +279,8 @@ public class ClusterEffectiveVersionTest extends EasyMockSupport {
       binder.install(new FactoryModuleBuilder().implement(
           Cluster.class, ClusterImpl.class).build(ClusterFactory.class));
 
      binder.bind(RoleCommandOrderProvider.class).to(CachedRoleCommandOrderProvider.class);

       try {
         AmbariMetaInfo ambariMetaInfo = EasyMock.createNiceMock(AmbariMetaInfo.class);
         EasyMock.expect(
diff --git a/ambari-server/src/test/resources/stacks/HDP/2.1.1/services/HBASE/metainfo.xml b/ambari-server/src/test/resources/stacks/HDP/2.1.1/services/HBASE/metainfo.xml
new file mode 100644
index 0000000000..5725d571f7
-- /dev/null
++ b/ambari-server/src/test/resources/stacks/HDP/2.1.1/services/HBASE/metainfo.xml
@@ -0,0 +1,44 @@
<?xml version="1.0"?>
<!--
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->
<metainfo>
  <schemaVersion>2.0</schemaVersion>
  <services>
    <service>
      <name>HBASE</name>
      <comment>Non-relational distributed database and centralized service for configuration management &amp; synchronization</comment>
      <version>0.95.2.2.0.5.0</version>

      <components>
        <component>
          <name>HBASE_MASTER</name>
          <versionAdvertised>true</versionAdvertised>
        </component>

        <component>
          <name>HBASE_REGIONSERVER</name>
          <versionAdvertised>true</versionAdvertised>
        </component>

        <component>
          <name>HBASE_CLIENT</name>
          <versionAdvertised>false</versionAdvertised>
        </component>
      </components>
    </service>
  </services>
</metainfo>
- 
2.19.1.windows.1

