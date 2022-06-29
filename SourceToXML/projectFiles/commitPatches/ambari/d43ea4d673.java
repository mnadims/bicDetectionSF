From d43ea4d6731cc002d4ddbba16b979804181e1eef Mon Sep 17 00:00:00 2001
From: Lisnichenko Dmitro <dlysnichenko@hortonworks.com>
Date: Tue, 28 Jul 2015 15:30:18 +0300
Subject: [PATCH] AMBARI-12517. Don't send install_packages command to hosts
 without versionable components (dlysnichenko)

--
 .../server/actionmanager/ActionScheduler.java |  11 +-
 .../ambari/server/agent/HeartBeatHandler.java |   2 +-
 .../ClusterStackVersionResourceProvider.java  |  93 ++++++++++--
 .../ActionFinalReportReceivedEvent.java       |  21 ++-
 .../DistributeRepositoriesActionListener.java |   3 +-
 .../apache/ambari/server/state/Cluster.java   |   2 +-
 .../org/apache/ambari/server/state/Host.java  |   2 +-
 .../ambari/server/state/host/HostImpl.java    |   9 ++
 .../scripts/install_packages.py               |  13 +-
 ...usterStackVersionResourceProviderTest.java | 133 +++++++++++------
 .../custom_actions/TestInstallPackages.py     | 134 ++++--------------
 11 files changed, 236 insertions(+), 187 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionScheduler.java b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionScheduler.java
index f3714b2126..7d936388f1 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionScheduler.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionScheduler.java
@@ -39,6 +39,7 @@ import org.apache.ambari.server.ServiceComponentNotFoundException;
 import org.apache.ambari.server.agent.ActionQueue;
 import org.apache.ambari.server.agent.AgentCommand.AgentCommandType;
 import org.apache.ambari.server.agent.CancelCommand;
import org.apache.ambari.server.agent.CommandReport;
 import org.apache.ambari.server.agent.ExecutionCommand;
 import org.apache.ambari.server.configuration.Configuration;
 import org.apache.ambari.server.controller.HostsMap;
@@ -971,9 +972,15 @@ class ActionScheduler implements Runnable {
       // against a concrete host without binding to a cluster)
       Long clusterId = clusterName != null ?
               clusters.getCluster(clusterName).getClusterId() : null;
      CommandReport report = new CommandReport();
      report.setRole(role);
      report.setStdOut("Action is dead");
      report.setStdErr("Action is dead");
      report.setStructuredOut("{}");
      report.setExitCode(1);
      report.setStatus(HostRoleStatus.ABORTED.toString());
       ActionFinalReportReceivedEvent event = new ActionFinalReportReceivedEvent(
              clusterId, hostname, null,
              role);
              clusterId, hostname, report, true);
       ambariEventPublisher.publish(event);
     } catch (AmbariException e) {
       LOG.error(String.format("Can not get cluster %s", clusterName), e);
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/agent/HeartBeatHandler.java b/ambari-server/src/main/java/org/apache/ambari/server/agent/HeartBeatHandler.java
index 6f34b62d78..29364e9d7b 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/agent/HeartBeatHandler.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/agent/HeartBeatHandler.java
@@ -462,7 +462,7 @@ public class HeartBeatHandler {
       if (RoleCommand.valueOf(report.getRoleCommand()) == RoleCommand.ACTIONEXECUTE &&
           HostRoleStatus.valueOf(report.getStatus()).isCompletedState()) {
         ActionFinalReportReceivedEvent event = new ActionFinalReportReceivedEvent(
                clusterId, hostname, report, report.getRole());
                clusterId, hostname, report, false);
         ambariEventPublisher.publish(event);
       }
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ClusterStackVersionResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ClusterStackVersionResourceProvider.java
index 972226dacd..6133885c4b 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ClusterStackVersionResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ClusterStackVersionResourceProvider.java
@@ -20,7 +20,6 @@ package org.apache.ambari.server.controller.internal;
 import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JDK_LOCATION;
 
 import java.util.ArrayList;
import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
@@ -57,13 +56,14 @@ import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
 import org.apache.ambari.server.controller.spi.SystemException;
 import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
 import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.events.ActionFinalReportReceivedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
 import org.apache.ambari.server.orm.dao.ClusterDAO;
 import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
 import org.apache.ambari.server.orm.dao.HostVersionDAO;
 import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
 import org.apache.ambari.server.orm.dao.StackDAO;
 import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
 import org.apache.ambari.server.orm.entities.HostVersionEntity;
 import org.apache.ambari.server.orm.entities.OperatingSystemEntity;
 import org.apache.ambari.server.orm.entities.RepositoryEntity;
@@ -71,6 +71,7 @@ import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
 import org.apache.ambari.server.orm.entities.StackEntity;
 import org.apache.ambari.server.serveraction.upgrades.FinalizeUpgradeAction;
 import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ComponentInfo;
 import org.apache.ambari.server.state.Host;
 import org.apache.ambari.server.state.RepositoryVersionState;
 import org.apache.ambari.server.state.ServiceComponentHost;
@@ -78,7 +79,6 @@ import org.apache.ambari.server.state.ServiceInfo;
 import org.apache.ambari.server.state.ServiceOsSpecific;
 import org.apache.ambari.server.state.StackId;
 import org.apache.ambari.server.utils.StageUtils;
import org.apache.ambari.server.orm.entities.ClusterEntity;
 
 import com.google.gson.Gson;
 import com.google.inject.Inject;
@@ -172,6 +172,9 @@ public class ClusterStackVersionResourceProvider extends AbstractControllerResou
   @Inject
   private static Configuration configuration;
 
  @Inject
  private static AmbariEventPublisher ambariEventPublisher;

   @Inject
   private static Injector injector;
 
@@ -313,8 +316,6 @@ public class ClusterStackVersionResourceProvider extends AbstractControllerResou
       }
     } else { // Using stack that is current for cluster
       StackId currentStackVersion = cluster.getCurrentStackVersion();
      stackName = currentStackVersion.getStackName();
      stackVersion = currentStackVersion.getStackVersion();
       stackId = currentStackVersion;
     }
 
@@ -342,6 +343,8 @@ public class ClusterStackVersionResourceProvider extends AbstractControllerResou
     int hostCount = hostsForCluster.size();
     int batchCount = (int) (Math.ceil((double)hostCount / maxTasks));
 
    ArrayList<Host> directTransitions = new ArrayList<Host>();

     long stageId = req.getLastStageId() + 1;
     if (0L == stageId) {
       stageId = 1L;
@@ -372,8 +375,13 @@ public class ClusterStackVersionResourceProvider extends AbstractControllerResou
       // Populate with commands for host
       for (int i = 0; i < maxTasks && hostsForClusterIter.hasNext(); i++) {
         Host host = hostsForClusterIter.next();
        addHostVersionInstallCommandsToStage(desiredRepoVersion,
                cluster, managementController, ami, stackId, perOsRepos, stage, host);
        if (hostHasVersionableComponents(cluster, ami, stackId, host)) {
          addHostVersionInstallCommandsToStage(desiredRepoVersion,
                  cluster, managementController, ami, stackId, perOsRepos, stage, host);
        } else {
          directTransitions.add(host);
        }

       }
     }
     req.addStages(stages);
@@ -404,6 +412,12 @@ public class ClusterStackVersionResourceProvider extends AbstractControllerResou
       // Will also initialize all Host Versions in an INSTALLING state.
       cluster.inferHostVersions(existingCSVer);
 
      // Directly transition host versions to INSTALLED for hosts that don't have
      // versionable components
      for(Host host : directTransitions) {
        transitionHostVersionToInstalled(host, cluster, existingCSVer.getRepositoryVersion().getVersion());
      }

       req.persist();
 
     } catch (AmbariException e) {
@@ -425,7 +439,7 @@ public class ClusterStackVersionResourceProvider extends AbstractControllerResou
                       "not defined. Repo version=%s, stackId=%s",
               host.getOsFamily(), desiredRepoVersion, stackId));
     }
    // For every host at cluster, determine packages for all installed services
    // determine packages for all services that are installed on host
     List<ServiceOsSpecific.Package> packages = new ArrayList<ServiceOsSpecific.Package>();
     Set<String> servicesOnHost = new HashSet<String>();
     List<ServiceComponentHost> components = cluster.getServiceComponentHosts(host.getHostName());
@@ -478,6 +492,58 @@ public class ClusterStackVersionResourceProvider extends AbstractControllerResou
   }
 
 
  /**
   * Returns true if there is at least one versionable component on host for a given
   * stack.
   */
  private boolean hostHasVersionableComponents(Cluster cluster, AmbariMetaInfo ami,
                                               StackId stackId, Host host) throws SystemException {
    List<ServiceComponentHost> components = cluster.getServiceComponentHosts(host.getHostName());
    for (ServiceComponentHost component : components) {
      ComponentInfo componentInfo;
      try {
        componentInfo = ami.getComponent(stackId.getStackName(),
                stackId.getStackVersion(), component.getServiceName(), component.getServiceComponentName());
      } catch (AmbariException e) {
        throw new SystemException(String.format("Exception while accessing component %s of service %s for stack %s",
                component.getServiceName(), component.getServiceComponentName(), stackId));
      }
      if (componentInfo.isVersionAdvertised()) {
        return true;
      }
    }
    return false;
  }


  /**
   *  Sends event for host regarding successful repo version installation
   *  without actually running any commands on host.
   *  Transitioning host version to INSTALLED state manually would not be the
   *  best idea since some additional logic may be bound to event listeners.
   */
  private void transitionHostVersionToInstalled(Host host, Cluster cluster,
                                                String version) {
    LOG.info(String.format("Transitioning version %s on host %s directly to installed" +
                    " without distributing bits to host since it has no versionable components.",
            version, host.getHostName()));
    CommandReport report = new CommandReport();
    report.setRole(INSTALL_PACKAGES_ACTION);
    report.setStdOut("Skipped distributing bits to host since it has " +
            "no versionable components installed");
    report.setStdErr("");
    // We don't set actual repo version in structured output in order
    // to avoid confusing server with fake data
    report.setStructuredOut("{}");
    report.setExitCode(0);
    report.setStatus(HostRoleStatus.COMPLETED.toString());
    ActionFinalReportReceivedEvent event = new ActionFinalReportReceivedEvent(
            cluster.getClusterId(), host.getHostName(),
            report, true);
    ambariEventPublisher.publish(event);
  }


   private RequestStageContainer createRequest() {
     ActionManager actionManager = getManagementController().getActionManager();
 
@@ -552,15 +618,12 @@ public class ClusterStackVersionResourceProvider extends AbstractControllerResou
       }
 
       // Get a host name to populate the hostrolecommand table's hostEntity.
      String defaultHostName = null;
      // TODO: remove direct access to cluster entity completely
      ClusterEntity clusterEntity = clusterDAO.findByName(clName);
      List<HostEntity> hosts = new ArrayList(clusterEntity.getHostEntities());
      if (hosts != null && !hosts.isEmpty()) {
      String defaultHostName;
      ArrayList<Host> hosts = new ArrayList<Host>(cluster.getHosts());
      if (!hosts.isEmpty()) {
         Collections.sort(hosts);
         defaultHostName = hosts.get(0).getHostName();
      }
      if (defaultHostName == null) {
      } else {
         throw new AmbariException("Could not find at least one host to set the command for");
       }
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/events/ActionFinalReportReceivedEvent.java b/ambari-server/src/main/java/org/apache/ambari/server/events/ActionFinalReportReceivedEvent.java
index 3ff5031c98..de797f33e6 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/events/ActionFinalReportReceivedEvent.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/events/ActionFinalReportReceivedEvent.java
@@ -31,6 +31,7 @@ public final class ActionFinalReportReceivedEvent extends AmbariEvent {
   private String hostname;
   private CommandReport commandReport;
   private String role;
  private Boolean emulated;
 
   /**
    * Constructor.
@@ -38,16 +39,24 @@ public final class ActionFinalReportReceivedEvent extends AmbariEvent {
    * @param clusterId (beware, may be null if action is not bound to cluster)
    * @param hostname host that is an origin for a command report
    * @param report full command report (may be null if action has been cancelled)
   * @param role host command role. It is usually present at report entity, but
   * if report is null, we still need some way to determine action type.
   * @param emulated true, if event was generated without actually receiving
   * data from agent (e.g. if we did not perform action, or action timed out,
   * but we want to trigger event listener anyway). More loose checks against
   * data will be performed in this case.
    */
   public ActionFinalReportReceivedEvent(Long clusterId, String hostname,
                                        CommandReport report, String role) {
                                        CommandReport report,
                                        Boolean emulated) {
     super(AmbariEventType.ACTION_EXECUTION_FINISHED);
     this.clusterId = clusterId;
     this.hostname = hostname;
     this.commandReport = report;
    this.role = role;
    if (report.getRole() != null) {
      this.role = report.getRole();
    } else {
      this.role = null;
    }
    this.emulated = emulated;
   }
 
   public Long getClusterId() {
@@ -66,6 +75,10 @@ public final class ActionFinalReportReceivedEvent extends AmbariEvent {
     return role;
   }
 
  public Boolean isEmulated() {
    return emulated;
  }

   @Override
   public String toString() {
     return "ActionFinalReportReceivedEvent{" +
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/events/listeners/upgrade/DistributeRepositoriesActionListener.java b/ambari-server/src/main/java/org/apache/ambari/server/events/listeners/upgrade/DistributeRepositoriesActionListener.java
index 5b7c2d6773..2c56861809 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/events/listeners/upgrade/DistributeRepositoriesActionListener.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/events/listeners/upgrade/DistributeRepositoriesActionListener.java
@@ -161,7 +161,8 @@ public class DistributeRepositoriesActionListener {
       // provide exact host stack version info) would be ignored
     for (HostVersionEntity hostVersion : hostVersions) {
 
      if (repositoryVersion != null && !hostVersion.getRepositoryVersion().getVersion().equals(repositoryVersion)) {
      if (! event.isEmulated() && // Emulated events anyway can not provide actual repo version
              ! (repositoryVersion == null || hostVersion.getRepositoryVersion().getVersion().equals(repositoryVersion))) {
         continue;
       }
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/state/Cluster.java b/ambari-server/src/main/java/org/apache/ambari/server/state/Cluster.java
index fe669bd7a3..ad481f307f 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/state/Cluster.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/state/Cluster.java
@@ -96,7 +96,7 @@ public interface Cluster {
    *
    * @return collection of hosts that are associated with this cluster
    */
  public Collection<Host> getHosts();
  Collection<Host> getHosts();
 
   /**
    * Get all of the hosts running the provided service and component.
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/state/Host.java b/ambari-server/src/main/java/org/apache/ambari/server/state/Host.java
index 34fb9958de..92682553a6 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/state/Host.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/state/Host.java
@@ -30,7 +30,7 @@ import org.apache.ambari.server.controller.HostResponse;
 import org.apache.ambari.server.orm.entities.HostVersionEntity;
 import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
 
public interface Host {
public interface Host extends Comparable {
 
   /**
    * @return the hostName
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/state/host/HostImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/state/host/HostImpl.java
index e20cd2532c..f7b74b917a 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/state/host/HostImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/state/host/HostImpl.java
@@ -263,6 +263,15 @@ public class HostImpl implements Host {
 
   }
 
  @Override
  public int compareTo(Object o) {
    if ((o != null ) && (o instanceof Host)) {
      return getHostName().compareTo(((Host) o).getHostName());
    } else {
      return -1;
    }
  }

   static class HostRegistrationReceived
       implements SingleArcTransition<HostImpl, HostEvent> {
 
diff --git a/ambari-server/src/main/resources/custom_actions/scripts/install_packages.py b/ambari-server/src/main/resources/custom_actions/scripts/install_packages.py
index bfcef1abfb..9d68b72cb3 100644
-- a/ambari-server/src/main/resources/custom_actions/scripts/install_packages.py
++ b/ambari-server/src/main/resources/custom_actions/scripts/install_packages.py
@@ -199,17 +199,8 @@ class InstallPackages(Script):
         self.put_structured_out(self.structured_output)
         Logger.info("Found actual version {0} by parsing file {1}".format(self.actual_version, REPO_VERSION_HISTORY_FILE))
       elif self.repo_version_with_build_number is None:
        # It's likely that this host does not have any Stack Components installed, so only contains AMS.
        # So just use repo version value provided by server (we already put it to structured output)
        if not os.path.exists(self.stack_root_folder):
          # Special case when this host does not contain any HDP components, but still contains other components like AMS.
          msg = "Could not determine actual version. This stack's root directory ({0}) is not present on this host, so this host does not contain any versionable components. " \
                "Therefore, ignore this host and allow other hosts to report the correct repository version.".format(self.stack_root_folder)
          Logger.info(msg)
        else:
          msg = "Could not determine actual version. This stack's root directory ({0}) exists but was not able to determine the actual repository version installed. " \
                "Try reinstalling packages again.".format(self.stack_root_folder)
          raise Fail(msg)
        msg = "Could not determine actual version installed. Try reinstalling packages again."
        raise Fail(msg)
 
 
   def install_packages(self, package_list):
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/ClusterStackVersionResourceProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/ClusterStackVersionResourceProviderTest.java
index 1dfa2fbd8b..a56823baa9 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/ClusterStackVersionResourceProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/ClusterStackVersionResourceProviderTest.java
@@ -20,10 +20,12 @@ package org.apache.ambari.server.controller.internal;
 
 import static org.easymock.EasyMock.anyLong;
 import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
 import static org.easymock.EasyMock.createMock;
 import static org.easymock.EasyMock.createNiceMock;
 import static org.easymock.EasyMock.eq;
 import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
 import static org.easymock.EasyMock.replay;
 import static org.easymock.EasyMock.verify;
 
@@ -48,6 +50,7 @@ import org.apache.ambari.server.agent.CommandReport;
 import org.apache.ambari.server.agent.ExecutionCommand;
 import org.apache.ambari.server.api.services.AmbariMetaInfo;
 import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariActionExecutionHelper;
 import org.apache.ambari.server.controller.AmbariManagementController;
 import org.apache.ambari.server.controller.RequestStatusResponse;
 import org.apache.ambari.server.controller.ResourceProviderFactory;
@@ -60,11 +63,13 @@ import org.apache.ambari.server.orm.GuiceJpaInitializer;
 import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
 import org.apache.ambari.server.orm.PersistenceType;
 import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
 import org.apache.ambari.server.orm.dao.HostDAO;
 import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
 import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
 import org.apache.ambari.server.orm.dao.StackDAO;
 import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
 import org.apache.ambari.server.orm.entities.HostEntity;
 import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
 import org.apache.ambari.server.orm.entities.ResourceEntity;
@@ -75,11 +80,15 @@ import org.apache.ambari.server.state.Cluster;
 import org.apache.ambari.server.state.Clusters;
 import org.apache.ambari.server.state.ConfigHelper;
 import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.RepositoryVersionState;
 import org.apache.ambari.server.state.ServiceComponentHost;
 import org.apache.ambari.server.state.ServiceInfo;
 import org.apache.ambari.server.state.ServiceOsSpecific;
 import org.apache.ambari.server.state.StackId;
 import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
 import org.junit.After;
 import org.junit.Assert;
 import org.junit.Before;
@@ -104,10 +113,12 @@ public class ClusterStackVersionResourceProviderTest {
   private ResourceTypeDAO resourceTypeDAO;
   private StackDAO stackDAO;
   private ClusterDAO clusterDAO;
  private ClusterVersionDAO clusterVersionDAO;
   private HostDAO hostDAO;
   private ConfigHelper configHelper;
   private Configuration configuration;
   private StageFactory stageFactory;
  private AmbariActionExecutionHelper actionExecutionHelper;
 
   private String operatingSystemsJson = "[\n" +
           "   {\n" +
@@ -139,6 +150,7 @@ public class ClusterStackVersionResourceProviderTest {
             String.valueOf(MAX_TASKS_PER_STAGE));
     configuration = new Configuration(properties);
     stageFactory = createNiceMock(StageFactory.class);
    clusterVersionDAO = createNiceMock(ClusterVersionDAO.class);
 
     // Initialize injector
     injector = Guice.createInjector(Modules.override(inMemoryModule).with(new MockModule()));
@@ -175,15 +187,33 @@ public class ClusterStackVersionResourceProviderTest {
       hostsForCluster.put(hostname, host);
     }
 
    ServiceComponentHost sch = createMock(ServiceComponentHost.class);
    List<ServiceComponentHost> schs = Collections.singletonList(sch);
    final ServiceComponentHost schDatanode = createMock(ServiceComponentHost.class);
    expect(schDatanode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schDatanode.getServiceComponentName()).andReturn("DATANODE").anyTimes();
    final ServiceComponentHost schNamenode = createMock(ServiceComponentHost.class);
    expect(schNamenode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schNamenode.getServiceComponentName()).andReturn("NAMENODE").anyTimes();
    final ServiceComponentHost schAMS = createMock(ServiceComponentHost.class);
    expect(schAMS.getServiceName()).andReturn("AMBARI_METRICS").anyTimes();
    expect(schAMS.getServiceComponentName()).andReturn("METRICS_COLLECTOR").anyTimes();
    // First host contains versionable components
    final List<ServiceComponentHost> schsH1 = new ArrayList<ServiceComponentHost>(){{
      add(schDatanode);
      add(schNamenode);
      add(schAMS);
    }};
    // Second host does not contain versionable components
    final List<ServiceComponentHost> schsH2 = new ArrayList<ServiceComponentHost>(){{
      add(schAMS);
    }};
 
     RepositoryVersionEntity repoVersion = new RepositoryVersionEntity();
    repoVersion.setId(1l);
     repoVersion.setOperatingSystems(operatingSystemsJson);
 
    ServiceOsSpecific.Package hivePackage = new ServiceOsSpecific.Package();
    hivePackage.setName("hive");
    List<ServiceOsSpecific.Package> packages = Collections.singletonList(hivePackage);
    ServiceOsSpecific.Package hdfsPackage = new ServiceOsSpecific.Package();
    hdfsPackage.setName("hdfs");
    List<ServiceOsSpecific.Package> packages = Collections.singletonList(hdfsPackage);
 
     ActionManager actionManager = createNiceMock(ActionManager.class);
 
@@ -202,7 +232,9 @@ public class ClusterStackVersionResourceProviderTest {
     expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
     expect(managementController.getJdkResourceUrl()).andReturn("/JdkResourceUrl").anyTimes();
     expect(managementController.getPackagesForServiceHost(anyObject(ServiceInfo.class),
            (Map<String, String>) anyObject(List.class), anyObject(String.class))).andReturn(packages).anyTimes();
            (Map<String, String>) anyObject(List.class), anyObject(String.class))).
            andReturn(packages).times((hostCount - 1) * 2); // 1 host has no versionable components, other hosts have 2 services
//            // that's why we don't send commands to it
 
     expect(resourceProviderFactory.getHostResourceProvider(anyObject(Set.class), anyObject(Map.class),
             eq(managementController))).andReturn(csvResourceProvider).anyTimes();
@@ -210,10 +242,20 @@ public class ClusterStackVersionResourceProviderTest {
     expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);
     expect(clusters.getHostsForCluster(anyObject(String.class))).andReturn(hostsForCluster);
 
    String clusterName = "Cluster100";
    //expect(cluster.getClusterName()).andReturn(clusterName).anyTimes();
     expect(cluster.getCurrentStackVersion()).andReturn(stackId);
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andReturn(schs).anyTimes();

    expect(sch.getServiceName()).andReturn("HIVE").anyTimes();
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andAnswer(new IAnswer<List<ServiceComponentHost>>() {
      @Override
      public List<ServiceComponentHost> answer() throws Throwable {
        String hostname = (String) EasyMock.getCurrentArguments()[0];
        if (hostname.equals("host2")) {
          return schsH2;
        } else {
          return schsH1;
        }
      }
    }).anyTimes();
 
     ExecutionCommand executionCommand = createNiceMock(ExecutionCommand.class);
     ExecutionCommandWrapper executionCommandWrapper = createNiceMock(ExecutionCommandWrapper.class);
@@ -238,10 +280,18 @@ public class ClusterStackVersionResourceProviderTest {
 
     expect(actionManager.getRequestTasks(anyLong())).andReturn(Collections.<HostRoleCommand>emptyList()).anyTimes();
 
    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterId(1l);
    clusterEntity.setClusterName(clusterName);
    ClusterVersionEntity cve = new ClusterVersionEntity(clusterEntity,
            repoVersion, RepositoryVersionState.INSTALL_FAILED, 0, "");
    expect(clusterVersionDAO.findByClusterAndStackAndVersion(anyObject(String.class),
            anyObject(StackId.class), anyObject(String.class))).andReturn(cve);

     // replay
     replay(managementController, response, clusters, resourceProviderFactory, csvResourceProvider,
        cluster, repositoryVersionDAOMock, configHelper, sch, actionManager,
            executionCommand, executionCommandWrapper,stage, stageFactory);
            cluster, repositoryVersionDAOMock, configHelper, schDatanode, schNamenode, schAMS, actionManager,
            executionCommand, executionCommandWrapper,stage, stageFactory, clusterVersionDAO);
 
     ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
         type,
@@ -260,10 +310,14 @@ public class ClusterStackVersionResourceProviderTest {
     properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
     properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "2.2.0.1-885");
     properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_STACK_PROPERTY_ID, "HDP");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID, "2.0.7");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID, "2.1.1");
 
     propertySet.add(properties);
 




     // create the request
     Request request = PropertyHelper.getCreateRequest(propertySet, null);
 
@@ -285,9 +339,7 @@ public class ClusterStackVersionResourceProviderTest {
     String clusterName = "Cluster100";
 
     AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    cluster.setClusterName(clusterName);

     StackId stackId = new StackId("HDP", "2.0.1");
     StackEntity stackEntity = stackDAO.find(stackId.getStackName(), stackId.getStackVersion());
     Assert.assertNotNull(stackEntity);
@@ -302,43 +354,27 @@ public class ClusterStackVersionResourceProviderTest {
     ResourceEntity resourceEntity = new ResourceEntity();
     resourceEntity.setResourceType(resourceTypeEntity);
 
    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterName(clusterName);
    clusterEntity.setResource(resourceEntity);
    clusterEntity.setDesiredStack(stackEntity);
    clusterDAO.create(clusterEntity);

     final Host host1 = createNiceMock("host1", Host.class);
     final Host host2 = createNiceMock("host2", Host.class);
 
    List<HostEntity> hostEntities = new ArrayList<HostEntity>();
    HostEntity hostEntity1 = new HostEntity();
    HostEntity hostEntity2 = new HostEntity();
    hostEntity1.setHostName("host1");
    hostEntity2.setHostName("host2");
    hostEntities.add(hostEntity1);
    hostEntities.add(hostEntity2);
    hostEntity1.setClusterEntities(Arrays.asList(clusterEntity));
    hostEntity2.setClusterEntities(Arrays.asList(clusterEntity));
    hostDAO.create(hostEntity1);
    hostDAO.create(hostEntity2);

    clusterEntity.setHostEntities(hostEntities);
    clusterDAO.merge(clusterEntity);

     expect(host1.getHostName()).andReturn("host1").anyTimes();
    expect(host1.getOsFamily()).andReturn("redhat6").anyTimes();
     expect(host2.getHostName()).andReturn("host2").anyTimes();
    expect(host2.getOsFamily()).andReturn("redhat6").anyTimes();
     replay(host1, host2);
    Map<String, Host> hostsForCluster = new HashMap<String, Host>() {{
      put(host1.getHostName(), host1);
      put(host2.getHostName(), host2);
    }};
 
     ServiceComponentHost sch = createMock(ServiceComponentHost.class);
     List<ServiceComponentHost> schs = Collections.singletonList(sch);
 
    Cluster cluster = createNiceMock(Cluster.class);
    cluster.setClusterName(clusterName);

    ArrayList<Host> hosts = new ArrayList<Host>() {{
      add(host1);
      add(host2);
    }};

    Clusters clusters = createNiceMock(Clusters.class);
    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);

     RepositoryVersionEntity repoVersion = new RepositoryVersionEntity();
     repoVersion.setOperatingSystems(operatingSystemsJson);
     StackEntity newDesiredStack = stackDAO.find("HDP", "2.0.1");
@@ -379,12 +415,15 @@ public class ClusterStackVersionResourceProviderTest {
     expect(resourceProviderFactory.getHostResourceProvider(anyObject(Set.class), anyObject(Map.class),
             eq(managementController))).andReturn(csvResourceProvider).anyTimes();
 
    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);
    expect(clusters.getHostsForCluster(anyObject(String.class))).andReturn(hostsForCluster);

     expect(cluster.getCurrentStackVersion()).andReturn(stackId);
     expect(cluster.getServiceComponentHosts(anyObject(String.class))).andReturn(schs).anyTimes();
 
    Capture<StackId> capturedStackId = new Capture<StackId>();
    cluster.setDesiredStackVersion(capture(capturedStackId));
      expectLastCall().once();
    expect(cluster.getHosts()).andReturn(hosts).anyTimes();


     expect(sch.getServiceName()).andReturn("HIVE").anyTimes();
 
     expect(repositoryVersionDAOMock.findByDisplayName(anyObject(String.class))).andReturn(repoVersion);
@@ -430,7 +469,8 @@ public class ClusterStackVersionResourceProviderTest {
 
     // verify
     verify(managementController, response);
    Assert.assertEquals(clusterEntity.getDesiredStack(), newDesiredStack);
    Assert.assertEquals(capturedStackId.getValue(),
            new StackId(newDesiredStack.getStackName(), newDesiredStack.getStackVersion()));
   }
 
 
@@ -441,6 +481,7 @@ public class ClusterStackVersionResourceProviderTest {
       bind(ConfigHelper.class).toInstance(configHelper);
       bind(Configuration.class).toInstance(configuration);
       bind(StageFactory.class).toInstance(stageFactory);
      bind(ClusterVersionDAO.class).toInstance(clusterVersionDAO);
     }
   }
 }
diff --git a/ambari-server/src/test/python/custom_actions/TestInstallPackages.py b/ambari-server/src/test/python/custom_actions/TestInstallPackages.py
index 5ddfaa8754..5b2a148fc7 100644
-- a/ambari-server/src/test/python/custom_actions/TestInstallPackages.py
++ b/ambari-server/src/test/python/custom_actions/TestInstallPackages.py
@@ -364,7 +364,7 @@ class TestInstallPackages(RMFTestCase):
   @patch("resource_management.libraries.functions.hdp_select.get_hdp_versions")
   @patch("resource_management.libraries.functions.repo_version_history.read_actual_version_from_history_file")
   @patch("resource_management.libraries.functions.repo_version_history.write_actual_version_to_history_file")
  def test_version_reporting__build_number_defined__usr_hdp_present(self,
  def test_version_reporting__build_number_defined(self,
                                                                                    write_actual_version_to_history_file_mock,
                                                                                    read_actual_version_from_history_file_mock,
                                                                                    hdp_versions_mock,
@@ -437,91 +437,6 @@ class TestInstallPackages(RMFTestCase):
 
 
 
  @patch("resource_management.libraries.functions.list_ambari_managed_repos.list_ambari_managed_repos")
  @patch("resource_management.libraries.functions.packages_analyzer.allInstalledPackages")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("resource_management.libraries.functions.hdp_select.get_hdp_versions")
  @patch("resource_management.libraries.functions.repo_version_history.read_actual_version_from_history_file")
  @patch("resource_management.libraries.functions.repo_version_history.write_actual_version_to_history_file")
  @patch("os.path.exists")
  def test_version_reporting__build_number_defined__usr_hdp_not_present(self,
                                                                    exists_mock,
                                                                    write_actual_version_to_history_file_mock,
                                                                    read_actual_version_from_history_file_mock,
                                                                    hdp_versions_mock,
                                                                    put_structured_out_mock, allInstalledPackages_mock, list_ambari_managed_repos_mock):
    exists_mock.return_value = False
    hdp_versions_mock.side_effect = [
      [],  # before installation attempt
      []
    ]
    read_actual_version_from_history_file_mock.return_value = None

    config_file = self.get_src_folder() + "/test/python/custom_actions/configs/install_packages_config.json"
    with open(config_file, "r") as f:
      command_json = json.load(f)

    command_json['roleParams']['repository_version'] = VERSION_STUB

    allInstalledPackages_mock.side_effect = TestInstallPackages._add_packages
    list_ambari_managed_repos_mock.return_value = []
    self.executeScript("scripts/install_packages.py",
                       classname="InstallPackages",
                       command="actionexecute",
                       config_dict=command_json,
                       target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                       os_type=('Redhat', '6.4', 'Final'),
                       )

    self.assertTrue(put_structured_out_mock.called)
    self.assertEquals(put_structured_out_mock.call_args[0][0],
                      {'package_installation_result': 'SUCCESS',
                       'stack_id': u'HDP-2.2',
                       'installed_repository_version': VERSION_STUB,
                       'actual_version': VERSION_STUB,
                       'ambari_repositories': []})

    self.assertFalse(write_actual_version_to_history_file_mock.called)

    hdp_versions_mock.reset_mock()
    write_actual_version_to_history_file_mock.reset_mock()
    put_structured_out_mock.reset_mock()

    # Test retrying install again

    hdp_versions_mock.side_effect = [
      [],  # before installation attempt
      []
    ]
    read_actual_version_from_history_file_mock.return_value = None

    config_file = self.get_src_folder() + "/test/python/custom_actions/configs/install_packages_config.json"
    with open(config_file, "r") as f:
      command_json = json.load(f)

    command_json['roleParams']['repository_version'] = VERSION_STUB

    allInstalledPackages_mock.side_effect = TestInstallPackages._add_packages
    list_ambari_managed_repos_mock.return_value = []
    self.executeScript("scripts/install_packages.py",
                       classname="InstallPackages",
                       command="actionexecute",
                       config_dict=command_json,
                       target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                       os_type=('Redhat', '6.4', 'Final'),
                       )

    self.assertTrue(put_structured_out_mock.called)
    self.assertEquals(put_structured_out_mock.call_args[0][0],
                      {'package_installation_result': 'SUCCESS',
                       'stack_id': u'HDP-2.2',
                       'installed_repository_version': VERSION_STUB,
                       'actual_version': VERSION_STUB,
                       'ambari_repositories': []})

    self.assertFalse(write_actual_version_to_history_file_mock.called)


   @patch("resource_management.libraries.functions.list_ambari_managed_repos.list_ambari_managed_repos")
   @patch("resource_management.libraries.functions.packages_analyzer.allInstalledPackages")
   @patch("resource_management.libraries.script.Script.put_structured_out")
@@ -563,6 +478,7 @@ class TestInstallPackages(RMFTestCase):
     except Fail:
       pass  # Expected
 

     self.assertTrue(put_structured_out_mock.called)
     self.assertEquals(put_structured_out_mock.call_args_list[-1][0][0],
                       { 'ambari_repositories': [],
@@ -584,7 +500,7 @@ class TestInstallPackages(RMFTestCase):
   @patch("resource_management.libraries.functions.repo_version_history.read_actual_version_from_history_file")
   @patch("resource_management.libraries.functions.repo_version_history.write_actual_version_to_history_file")
   @patch("os.path.exists")
  def test_version_reporting__build_number_not_defined__usr_hdp_not_present(self,
  def test_version_reporting__build_number_not_defined__usr_hdp_absent(self,
                                                                         exists_mock,
                                                                         write_actual_version_to_history_file_mock,
                                                                         read_actual_version_from_history_file_mock,
@@ -605,17 +521,21 @@ class TestInstallPackages(RMFTestCase):
 
     allInstalledPackages_mock.side_effect = TestInstallPackages._add_packages
     list_ambari_managed_repos_mock.return_value = []
    self.executeScript("scripts/install_packages.py",
                       classname="InstallPackages",
                       command="actionexecute",
                       config_dict=command_json,
                       target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                       os_type=('Redhat', '6.4', 'Final'),
                       )
    try:
      self.executeScript("scripts/install_packages.py",
                         classname="InstallPackages",
                         command="actionexecute",
                         config_dict=command_json,
                         target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                         os_type=('Redhat', '6.4', 'Final'),
                         )
      self.fail("Should throw exception")
    except Fail:
      pass  # Expected
 
     self.assertTrue(put_structured_out_mock.called)
     self.assertEquals(put_structured_out_mock.call_args_list[-1][0][0],
                      {'package_installation_result': 'SUCCESS',
                      {'package_installation_result': 'FAIL',
                        'stack_id': u'HDP-2.2',
                        'installed_repository_version': '2.2.0.1',
                        'ambari_repositories': []})
@@ -626,7 +546,7 @@ class TestInstallPackages(RMFTestCase):
     write_actual_version_to_history_file_mock.reset_mock()
     put_structured_out_mock.reset_mock()
 
    # Test retrying install again
    # Test retrying install again  (correct build number, provided by other nodes, is now received from server)
 
     hdp_versions_mock.side_effect = [
       [],  # before installation attempt
@@ -845,17 +765,21 @@ class TestInstallPackages(RMFTestCase):
 
     allInstalledPackages_mock.side_effect = TestInstallPackages._add_packages
     list_ambari_managed_repos_mock.return_value = []
    self.executeScript("scripts/install_packages.py",
                       classname="InstallPackages",
                       command="actionexecute",
                       config_dict=command_json,
                       target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                       os_type=('Redhat', '6.4', 'Final'),
                       )
    try:
      self.executeScript("scripts/install_packages.py",
                         classname="InstallPackages",
                         command="actionexecute",
                         config_dict=command_json,
                         target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                         os_type=('Redhat', '6.4', 'Final'),
                         )
      self.fail("Should throw exception")
    except Fail:
      pass  # Expected
 
     self.assertTrue(put_structured_out_mock.called)
     self.assertEquals(put_structured_out_mock.call_args_list[-1][0][0],
                      {'package_installation_result': 'SUCCESS',
                      {'package_installation_result': 'FAIL',
                        'stack_id': u'HDP-2.2',
                        'installed_repository_version': '2.2.0.1',
                        'ambari_repositories': []})
@@ -866,7 +790,7 @@ class TestInstallPackages(RMFTestCase):
     write_actual_version_to_history_file_mock.reset_mock()
     put_structured_out_mock.reset_mock()
 
    # Test retrying install again
    # Test retrying install again (correct build number, provided by other nodes, is now received from server)
 
     hdp_versions_mock.side_effect = [
       [],  # before installation attempt
- 
2.19.1.windows.1

