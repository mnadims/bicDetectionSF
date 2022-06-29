From f33a250c0e7624b6cbc0a11ffce12506eaa95d9a Mon Sep 17 00:00:00 2001
From: Jonathan Hurley <jhurley@hortonworks.com>
Date: Fri, 7 Jul 2017 14:36:05 -0400
Subject: [PATCH] AMBARI-21430 - Allow Multiple Versions of Stack Tools to
 Co-Exist (jonathanhurley)

--
 .../libraries/functions/stack_features.py     |   13 +
 .../libraries/functions/stack_tools.py        |   39 +
 .../libraries/script/script.py                |   19 +-
 .../server/api/query/JpaPredicateVisitor.java |    8 +-
 .../controller/ActionExecutionContext.java    |   26 +
 .../AmbariActionExecutionHelper.java          |   26 +-
 .../BlueprintConfigurationProcessor.java      |   59 +-
 .../ClusterStackVersionResourceProvider.java  |  163 +-
 .../ambari/server/state/ConfigHelper.java     |   32 +
 .../ambari/server/topology/AmbariContext.java |   18 +
 .../server/upgrade/UpgradeCatalog252.java     |   61 +
 .../package/alerts/alert_hive_metastore.py    |   11 +-
 .../package/alerts/alert_llap_app_status.py   |   12 +-
 .../alerts/alert_check_oozie_server.py        |    8 +-
 .../host_scripts/alert_disk_space.py          |   10 +-
 .../host_scripts/alert_version_select.py      |   16 +-
 .../HDP/2.0.6/configuration/cluster-env.xml   |   16 +-
 .../HDP/2.0.6/properties/stack_features.json  |  852 +++++------
 .../HDP/2.0.6/properties/stack_tools.json     |   16 +-
 .../PERF/1.0/configuration/cluster-env.xml    |   16 +-
 .../PERF/1.0/properties/stack_features.json   |   38 +-
 .../PERF/1.0/properties/stack_tools.json      |   16 +-
 .../BlueprintConfigurationProcessorTest.java  |   41 +-
 ...usterStackVersionResourceProviderTest.java |    4 +-
 .../ClusterConfigurationRequestTest.java      |   60 +-
 .../common-services/configs/hawq_default.json |    6 +-
 .../python/host_scripts/TestAlertDiskSpace.py |   16 +-
 .../2.5/configs/ranger-admin-default.json     |  990 ++++++-------
 .../2.5/configs/ranger-admin-secured.json     | 1108 +++++++-------
 .../2.5/configs/ranger-kms-default.json       | 1158 +++++++--------
 .../2.5/configs/ranger-kms-secured.json       | 1320 ++++++++---------
 .../2.6/configs/ranger-admin-default.json     |  953 ++++++------
 .../2.6/configs/ranger-admin-secured.json     | 1066 ++++++-------
 .../test/python/stacks/utils/RMFTestCase.py   |    8 +-
 34 files changed, 4353 insertions(+), 3852 deletions(-)

diff --git a/ambari-common/src/main/python/resource_management/libraries/functions/stack_features.py b/ambari-common/src/main/python/resource_management/libraries/functions/stack_features.py
index cbd32e7d14..576c138308 100644
-- a/ambari-common/src/main/python/resource_management/libraries/functions/stack_features.py
++ b/ambari-common/src/main/python/resource_management/libraries/functions/stack_features.py
@@ -43,6 +43,12 @@ def check_stack_feature(stack_feature, stack_version):
 
   from resource_management.libraries.functions.default import default
   from resource_management.libraries.functions.version import compare_versions

  stack_name = default("/hostLevelParams/stack_name", None)
  if stack_name is None:
    Logger.warning("Cannot find the stack name in the command. Stack features cannot be loaded")
    return False

   stack_features_config = default("/configurations/cluster-env/stack_features", None)
 
   if not stack_version:
@@ -51,6 +57,13 @@ def check_stack_feature(stack_feature, stack_version):
 
   if stack_features_config:
     data = json.loads(stack_features_config)

    if stack_name not in data:
      Logger.warning("Cannot find stack features for the stack named {0}".format(stack_name))
      return False

    data = data[stack_name]

     for feature in data["stack_features"]:
       if feature["name"] == stack_feature:
         if "min_version" in feature:
diff --git a/ambari-common/src/main/python/resource_management/libraries/functions/stack_tools.py b/ambari-common/src/main/python/resource_management/libraries/functions/stack_tools.py
index 02ae62daf3..420ae11cca 100644
-- a/ambari-common/src/main/python/resource_management/libraries/functions/stack_tools.py
++ b/ambari-common/src/main/python/resource_management/libraries/functions/stack_tools.py
@@ -39,15 +39,33 @@ def get_stack_tool(name):
   :return: tool_name, tool_path, tool_package
   """
   from resource_management.libraries.functions.default import default

  stack_name = default("/hostLevelParams/stack_name", None)
  if stack_name is None:
    Logger.warning("Cannot find the stack name in the command. Stack tools cannot be loaded")
    return (None, None, None)

   stack_tools = None
   stack_tools_config = default("/configurations/cluster-env/stack_tools", None)
   if stack_tools_config:
     stack_tools = json.loads(stack_tools_config)
 
  if stack_tools is None:
    Logger.warning("The stack tools could not be found in cluster-env")
    return (None, None, None)

  if stack_name not in stack_tools:
    Logger.warning("Cannot find stack tools for the stack named {0}".format(stack_name))
    return (None, None, None)

  # load the stack tooks keyed by the stack name
  stack_tools = stack_tools[stack_name]

   if not stack_tools or not name or name.lower() not in stack_tools:
     Logger.warning("Cannot find config for {0} stack tool in {1}".format(str(name), str(stack_tools)))
     return (None, None, None)
 

   tool_config = stack_tools[name.lower()]
 
   # Return fixed length (tool_name, tool_path tool_package) tuple
@@ -81,3 +99,24 @@ def get_stack_tool_package(name):
   """
   (tool_name, tool_path, tool_package) = get_stack_tool(name)
   return tool_package


def get_stack_root(stack_name, stack_root_json):
  """
  Get the stack-specific install root directory from the raw, JSON-escaped properties.
  :param stack_name:
  :param stack_root_json:
  :return: stack_root
  """
  from resource_management.libraries.functions.default import default

  if stack_root_json is None:
    return "/usr/{0}".format(stack_name.lower())

  stack_root = json.loads(stack_root_json)

  if stack_name not in stack_root:
    Logger.warning("Cannot determine stack root for stack named {0}".format(stack_name))
    return "/usr/{0}".format(stack_name.lower())

  return stack_root[stack_name]
diff --git a/ambari-common/src/main/python/resource_management/libraries/script/script.py b/ambari-common/src/main/python/resource_management/libraries/script/script.py
index 2c56a13c93..2b374c5cff 100644
-- a/ambari-common/src/main/python/resource_management/libraries/script/script.py
++ b/ambari-common/src/main/python/resource_management/libraries/script/script.py
@@ -597,7 +597,11 @@ class Script(object):
     :return: a stack name or None
     """
     from resource_management.libraries.functions.default import default
    return default("/hostLevelParams/stack_name", "HDP")
    stack_name = default("/hostLevelParams/stack_name", None)
    if stack_name is None:
      stack_name = default("/configurations/cluster-env/stack_name", "HDP")

    return stack_name
 
   @staticmethod
   def get_stack_root():
@@ -607,7 +611,18 @@ class Script(object):
     """
     from resource_management.libraries.functions.default import default
     stack_name = Script.get_stack_name()
    return default("/configurations/cluster-env/stack_root", "/usr/{0}".format(stack_name.lower()))
    stack_root_json = default("/configurations/cluster-env/stack_root", None)

    if stack_root_json is None:
      return "/usr/{0}".format(stack_name.lower())

    stack_root = json.loads(stack_root_json)

    if stack_name not in stack_root:
      Logger.warning("Cannot determine stack root for stack named {0}".format(stack_name))
      return "/usr/{0}".format(stack_name.lower())

    return stack_root[stack_name]
 
   @staticmethod
   def get_stack_version():
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/query/JpaPredicateVisitor.java b/ambari-server/src/main/java/org/apache/ambari/server/api/query/JpaPredicateVisitor.java
index 984dc3b54d..84e9dd94f2 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/query/JpaPredicateVisitor.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/query/JpaPredicateVisitor.java
@@ -62,11 +62,6 @@ public abstract class JpaPredicateVisitor<T> implements PredicateVisitor {
    */
   final private CriteriaQuery<T> m_query;
 
  /**
   * The entity class that the root of the query is built from.
   */
  final private Class<T> m_entityClass;

   /**
    * The last calculated predicate.
    */
@@ -92,7 +87,6 @@ public abstract class JpaPredicateVisitor<T> implements PredicateVisitor {
   public JpaPredicateVisitor(EntityManager entityManager, Class<T> entityClass) {
     m_entityManager = entityManager;
     m_builder = m_entityManager.getCriteriaBuilder();
    m_entityClass = entityClass;
     m_query = m_builder.createQuery(entityClass);
     m_root = m_query.from(entityClass);
   }
@@ -178,7 +172,7 @@ public abstract class JpaPredicateVisitor<T> implements PredicateVisitor {
     }
 
     String operator = predicate.getOperator();
    Comparable<?> value = predicate.getValue();
    Comparable value = predicate.getValue();
 
     // convert string to enum for proper JPA comparisons
     if (lastSingularAttribute != null) {
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/ActionExecutionContext.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/ActionExecutionContext.java
index 42a95c083e..34d6db9433 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/ActionExecutionContext.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/ActionExecutionContext.java
@@ -27,6 +27,7 @@ import org.apache.ambari.server.actionmanager.TargetHostType;
 import org.apache.ambari.server.agent.ExecutionCommand;
 import org.apache.ambari.server.controller.internal.RequestOperationLevel;
 import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.state.StackId;
 
 /**
  * The context required to create tasks and stages for a custom action
@@ -43,6 +44,7 @@ public class ActionExecutionContext {
   private String expectedComponentName;
   private boolean hostsInMaintenanceModeExcluded = true;
   private boolean allowRetry = false;
  private StackId stackId;
 
   private List<ExecutionCommandVisitor> m_visitors = new ArrayList<>();
 
@@ -172,6 +174,30 @@ public class ActionExecutionContext {
     this.autoSkipFailures = autoSkipFailures;
   }
 
  /**
   * Gets the stack to use for generating stack-associated values for a command.
   * In some cases the cluster's stack is not the correct one to use, such as
   * when distributing a repository.
   *
   * @return the stackId the stack to use when generating stack-specific content
   *         for the command.
   */
  public StackId getStackId() {
    return stackId;
  }

  /**
   * Sets the stack to use for generating stack-associated values for a command.
   * In some cases the cluster's stack is not the correct one to use, such as
   * when distributing a repository.
   *
   * @param stackId
   *          the stackId to use for stack-based properties on the command.
   */
  public void setStackId(StackId stackId) {
    this.stackId = stackId;
  }

   /**
    * Adds a command visitor that will be invoked after a command is created.  Provides access
    * to the command.
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariActionExecutionHelper.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariActionExecutionHelper.java
index 8f522b0c71..391daa9418 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariActionExecutionHelper.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariActionExecutionHelper.java
@@ -465,7 +465,10 @@ public class AmbariActionExecutionHelper {
 
       if (StringUtils.isNotBlank(serviceName)) {
         Service service = cluster.getService(serviceName);
        addRepoInfoToHostLevelParams(service.getDesiredRepositoryVersion(), hostLevelParams, hostName);
        addRepoInfoToHostLevelParams(actionContext, service.getDesiredRepositoryVersion(),
            hostLevelParams, hostName);
      } else {
        addRepoInfoToHostLevelParams(actionContext, null, hostLevelParams, hostName);
       }
 
 
@@ -529,9 +532,19 @@ public class AmbariActionExecutionHelper {
   *
   * */
 
  private void addRepoInfoToHostLevelParams(RepositoryVersionEntity repositoryVersion,
      Map<String, String> hostLevelParams, String hostName) throws AmbariException {
  private void addRepoInfoToHostLevelParams(ActionExecutionContext actionContext,
      RepositoryVersionEntity repositoryVersion, Map<String, String> hostLevelParams,
      String hostName) throws AmbariException {

    // if the repo is null, see if any values from the context should go on the
    // host params and then return
     if (null == repositoryVersion) {
      if (null != actionContext.getStackId()) {
        StackId stackId = actionContext.getStackId();
        hostLevelParams.put(STACK_NAME, stackId.getStackName());
        hostLevelParams.put(STACK_VERSION, stackId.getStackVersion());
      }

       return;
     }
 
@@ -557,7 +570,10 @@ public class AmbariActionExecutionHelper {
 
     hostLevelParams.put(REPO_INFO, rootJsonObject.toString());
 
    hostLevelParams.put(STACK_NAME, repositoryVersion.getStackName());
    hostLevelParams.put(STACK_VERSION, repositoryVersion.getStackVersion());
    // set the host level params if not already set by whoever is creating this command
    if (!hostLevelParams.containsKey(STACK_NAME) || !hostLevelParams.containsKey(STACK_VERSION)) {
      hostLevelParams.put(STACK_NAME, repositoryVersion.getStackName());
      hostLevelParams.put(STACK_VERSION, repositoryVersion.getStackVersion());
    }
   }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessor.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessor.java
index e93b2f7378..37284bec01 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessor.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessor.java
@@ -36,7 +36,9 @@ import java.util.regex.Pattern;
 
 import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ConfigHelper;
 import org.apache.ambari.server.state.PropertyDependencyInfo;
import org.apache.ambari.server.state.StackId;
 import org.apache.ambari.server.state.ValueAttributesInfo;
 import org.apache.ambari.server.topology.AdvisedConfiguration;
 import org.apache.ambari.server.topology.Blueprint;
@@ -356,7 +358,7 @@ public class BlueprintConfigurationProcessor {
             final String originalValue = typeMap.get(propertyName);
             final String updatedValue =
               updater.updateForClusterCreate(propertyName, originalValue, clusterProps, clusterTopology);
            

             if(updatedValue == null ) {
               continue;
             }
@@ -419,6 +421,7 @@ public class BlueprintConfigurationProcessor {
     }
 
     // Explicitly set any properties that are required but not currently provided in the stack definition.
    setStackToolsAndFeatures(clusterConfig, configTypesUpdated);
     setRetryConfiguration(clusterConfig, configTypesUpdated);
     setupHDFSProxyUsers(clusterConfig, configTypesUpdated);
     addExcludedConfigProperties(clusterConfig, configTypesUpdated, clusterTopology.getBlueprint().getStack());
@@ -531,7 +534,7 @@ public class BlueprintConfigurationProcessor {
     try {
       String clusterName = clusterTopology.getAmbariContext().getClusterName(clusterTopology.getClusterId());
       Cluster cluster = clusterTopology.getAmbariContext().getController().getClusters().getCluster(clusterName);
      authToLocalPerClusterMap = new HashMap<Long, Set<String>>();
      authToLocalPerClusterMap = new HashMap<>();
       authToLocalPerClusterMap.put(Long.valueOf(clusterTopology.getClusterId()), clusterTopology.getAmbariContext().getController().getKerberosHelper().getKerberosDescriptor(cluster).getAllAuthToLocalProperties());
       } catch (AmbariException e) {
         LOG.error("Error while getting authToLocal properties. ", e);
@@ -2186,8 +2189,9 @@ public class BlueprintConfigurationProcessor {
       StringBuilder sb = new StringBuilder();
 
       Matcher m = REGEX_IN_BRACKETS.matcher(origValue);
      if (m.matches())
      if (m.matches()) {
         origValue = m.group("INNER");
      }
 
       if (origValue != null) {
         sb.append("[");
@@ -2195,8 +2199,9 @@ public class BlueprintConfigurationProcessor {
         for (String value : origValue.split(",")) {
 
           m = REGEX_IN_QUOTES.matcher(value);
          if (m.matches())
          if (m.matches()) {
             value = m.group("INNER");
          }
 
           if (!isFirst) {
             sb.append(",");
@@ -2230,6 +2235,7 @@ public class BlueprintConfigurationProcessor {
    */
   private static class OriginalValuePropertyUpdater implements PropertyUpdater {
 
    @Override
     public String updateForClusterCreate(String propertyName,
                                          String origValue,
                                          Map<String, Map<String, String>> properties,
@@ -2949,6 +2955,49 @@ public class BlueprintConfigurationProcessor {
   }
 
 
  /**
   * Sets the read-only properties for stack features & tools, overriding
   * anything provided in the blueprint.
   *
   * @param configuration
   *          the configuration to update with values from the stack.
   * @param configTypesUpdated
   *          the list of configuration types updated (cluster-env will be added
   *          to this).
   * @throws ConfigurationTopologyException
   */
  private void setStackToolsAndFeatures(Configuration configuration, Set<String> configTypesUpdated)
      throws ConfigurationTopologyException {
    ConfigHelper configHelper = clusterTopology.getAmbariContext().getConfigHelper();
    Stack stack = clusterTopology.getBlueprint().getStack();
    String stackName = stack.getName();
    String stackVersion = stack.getVersion();

    StackId stackId = new StackId(stackName, stackVersion);

    Set<String> properties = Sets.newHashSet(ConfigHelper.CLUSTER_ENV_STACK_NAME_PROPERTY,
        ConfigHelper.CLUSTER_ENV_STACK_ROOT_PROPERTY, ConfigHelper.CLUSTER_ENV_STACK_TOOLS_PROPERTY,
        ConfigHelper.CLUSTER_ENV_STACK_FEATURES_PROPERTY);

    try {
      Map<String, Map<String, String>> defaultStackProperties = configHelper.getDefaultStackProperties(stackId);
      Map<String,String> clusterEnvDefaultProperties = defaultStackProperties.get(CLUSTER_ENV_CONFIG_TYPE_NAME);

      for( String property : properties ){
        if (defaultStackProperties.containsKey(property)) {
          configuration.setProperty(CLUSTER_ENV_CONFIG_TYPE_NAME, property,
              clusterEnvDefaultProperties.get(property));

          // make sure to include the configuration type as being updated
          configTypesUpdated.add(CLUSTER_ENV_CONFIG_TYPE_NAME);
        }
      }
    } catch( AmbariException ambariException ){
      throw new ConfigurationTopologyException("Unable to retrieve the stack tools and features",
          ambariException);
    }
  }

   /**
    * Ensure that the specified property exists.
    * If not, set a default value.
@@ -3099,7 +3148,7 @@ public class BlueprintConfigurationProcessor {
 
     @Override
     public boolean isPropertyIncluded(String propertyName, String propertyValue, String configType, ClusterTopology topology) {
      return !(this.propertyConfigType.equals(configType) &&
      return !(propertyConfigType.equals(configType) &&
              this.propertyName.equals(propertyName));
     }
   }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ClusterStackVersionResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ClusterStackVersionResourceProvider.java
index 93c02bedfa..c4fce8a640 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ClusterStackVersionResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ClusterStackVersionResourceProvider.java
@@ -67,11 +67,13 @@ import org.apache.ambari.server.security.authorization.RoleAuthorization;
 import org.apache.ambari.server.state.Cluster;
 import org.apache.ambari.server.state.Clusters;
 import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
 import org.apache.ambari.server.state.Host;
 import org.apache.ambari.server.state.RepositoryType;
 import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Service;
 import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceOsSpecific;
 import org.apache.ambari.server.state.StackId;
 import org.apache.ambari.server.state.repository.VersionDefinitionXml;
 import org.apache.ambari.server.state.stack.upgrade.RepositoryVersionHelper;
@@ -83,6 +85,7 @@ import org.apache.commons.lang.math.NumberUtils;
 import com.google.common.collect.ImmutableMap;
 import com.google.common.collect.Lists;
 import com.google.common.collect.Sets;
import com.google.gson.Gson;
 import com.google.inject.Inject;
 import com.google.inject.Provider;
 import com.google.inject.persist.Transactional;
@@ -171,11 +174,19 @@ public class ClusterStackVersionResourceProvider extends AbstractControllerResou
   @Inject
   private static RepositoryVersionHelper repoVersionHelper;
 

  @Inject
  private static Gson gson;
 
   @Inject
   private static Provider<Clusters> clusters;
 
  /**
   * Used for updating the existing stack tools with those of the stack being
   * distributed.
   */
  @Inject
  private static Provider<ConfigHelper> configHelperProvider;

   /**
    * Constructor.
    */
@@ -287,8 +298,6 @@ public class ClusterStackVersionResourceProvider extends AbstractControllerResou
 
     String clName;
     final String desiredRepoVersion;
    String stackName;
    String stackVersion;
 
     Map<String, Object> propertyMap = iterator.next();
 
@@ -327,30 +336,30 @@ public class ClusterStackVersionResourceProvider extends AbstractControllerResou
           cluster.getClusterName(), entity.getDirection().getText(false)));
     }
 
    Set<StackId> stackIds = new HashSet<>();
    if (propertyMap.containsKey(CLUSTER_STACK_VERSION_STACK_PROPERTY_ID) &&
            propertyMap.containsKey(CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID)) {
      stackName = (String) propertyMap.get(CLUSTER_STACK_VERSION_STACK_PROPERTY_ID);
      stackVersion = (String) propertyMap.get(CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID);
      StackId stackId = new StackId(stackName, stackVersion);
      if (! ami.isSupportedStack(stackName, stackVersion)) {
        throw new NoSuchParentResourceException(String.format("Stack %s is not supported",
                stackId));
      }
      stackIds.add(stackId);
    } else { // Using stack that is current for cluster
      for (Service service : cluster.getServices().values()) {
        stackIds.add(service.getDesiredStackId());
      }
    String stackName = (String) propertyMap.get(CLUSTER_STACK_VERSION_STACK_PROPERTY_ID);
    String stackVersion = (String) propertyMap.get(CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID);
    if (StringUtils.isBlank(stackName) || StringUtils.isBlank(stackVersion)) {
      String message = String.format(
          "Both the %s and %s properties are required when distributing a new stack",
          CLUSTER_STACK_VERSION_STACK_PROPERTY_ID, CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID);

      throw new SystemException(message);
     }
 
    if (stackIds.size() > 1) {
      throw new SystemException("Could not determine stack to add out of " + StringUtils.join(stackIds, ','));
    StackId stackId = new StackId(stackName, stackVersion);

    if (!ami.isSupportedStack(stackName, stackVersion)) {
      throw new NoSuchParentResourceException(String.format("Stack %s is not supported", stackId));
     }
 
    StackId stackId = stackIds.iterator().next();
    stackName = stackId.getStackName();
    stackVersion = stackId.getStackVersion();
    // bootstrap the stack tools if necessary for the stack which is being
    // distributed
    try {
      bootstrapStackTools(stackId, cluster);
    } catch (AmbariException ambariException) {
      throw new SystemException("Unable to modify stack tools for new stack being distributed",
          ambariException);
    }
 
     RepositoryVersionEntity repoVersionEntity = repositoryVersionDAO.findByStackAndVersion(
         stackId, desiredRepoVersion);
@@ -580,6 +589,7 @@ public class ClusterStackVersionResourceProvider extends AbstractControllerResou
     }
 
     // determine packages for all services that are installed on host
    List<ServiceOsSpecific.Package> packages = new ArrayList<>();
     Set<String> servicesOnHost = new HashSet<>();
     List<ServiceComponentHost> components = cluster.getServiceComponentHosts(host.getHostName());
     for (ServiceComponentHost component : components) {
@@ -600,16 +610,15 @@ public class ClusterStackVersionResourceProvider extends AbstractControllerResou
     RequestResourceFilter filter = new RequestResourceFilter(null, null,
             Collections.singletonList(host.getHostName()));
 
    ActionExecutionContext actionContext = new ActionExecutionContext(
            cluster.getClusterName(), INSTALL_PACKAGES_ACTION,
            Collections.singletonList(filter),
            roleParams);
    ActionExecutionContext actionContext = new ActionExecutionContext(cluster.getClusterName(),
        INSTALL_PACKAGES_ACTION, Collections.singletonList(filter), roleParams);

    actionContext.setStackId(stackId);
     actionContext.setTimeout(Short.valueOf(configuration.getDefaultAgentTaskTimeout(true)));
 
     repoVersionHelper.addCommandRepository(actionContext, osFamily, repoVersion, repoInfo);
 
     return actionContext;

   }
 
 
@@ -698,4 +707,100 @@ public class ClusterStackVersionResourceProvider extends AbstractControllerResou
   }
 
 
  /**
   * Ensures that the stack tools and stack features are set on
   * {@link ConfigHelper#CLUSTER_ENV} for the stack of the repository being
   * distributed. This step ensures that the new repository can be distributed
   * with the correct tools.
   * <p/>
   * If the cluster's current stack name matches that of the new stack or the
   * new stack's tools are already added in the configuration, then this method
   * will not change anything.
   *
   * @param stackId
   *          the stack of the repository being distributed (not {@code null}).
   * @param cluster
   *          the cluster the new stack/repo is being distributed for (not
   *          {@code null}).
   * @throws AmbariException
   */
  private void bootstrapStackTools(StackId stackId, Cluster cluster) throws AmbariException {
    // if the stack name is the same as the cluster's current stack name, then
    // there's no work to do
    if (StringUtils.equals(stackId.getStackName(),
        cluster.getCurrentStackVersion().getStackName())) {
      return;
    }

    ConfigHelper configHelper = configHelperProvider.get();

    // get the stack tools/features for the stack being distributed
    Map<String, Map<String, String>> defaultStackConfigurationsByType = configHelper.getDefaultStackProperties(stackId);

    Map<String, String> clusterEnvDefaults = defaultStackConfigurationsByType.get(
        ConfigHelper.CLUSTER_ENV);

    Config clusterEnv = cluster.getDesiredConfigByType(ConfigHelper.CLUSTER_ENV);
    Map<String, String> clusterEnvProperties = clusterEnv.getProperties();

    // the 3 properties we need to check and update
    Set<String> properties = Sets.newHashSet(ConfigHelper.CLUSTER_ENV_STACK_ROOT_PROPERTY,
        ConfigHelper.CLUSTER_ENV_STACK_TOOLS_PROPERTY,
        ConfigHelper.CLUSTER_ENV_STACK_FEATURES_PROPERTY);

    // any updates are stored here and merged into the existing config type
    Map<String, String> updatedProperties = new HashMap<>();

    for (String property : properties) {
      // determine if the property exists in the stack being distributed (it
      // kind of has to, but we'll be safe if it's not found)
      String newStackDefaultJson = clusterEnvDefaults.get(property);
      if (StringUtils.isBlank(newStackDefaultJson)) {
        continue;
      }

      String existingPropertyJson = clusterEnvProperties.get(property);

      // if the stack tools/features property doesn't exist, then just set the
      // one from the new stack
      if (StringUtils.isBlank(existingPropertyJson)) {
        updatedProperties.put(property, newStackDefaultJson);
        continue;
      }

      // now is the hard part - we need to check to see if the new stack tools
      // exists alongside the current tools and if it doesn't, then add the new
      // tools in
      final Map<String, Object> existingJson;
      final Map<String, ?> newStackJsonAsObject;
      if (StringUtils.equals(property, ConfigHelper.CLUSTER_ENV_STACK_ROOT_PROPERTY)) {
        existingJson = gson.<Map<String, Object>> fromJson(existingPropertyJson, Map.class);
        newStackJsonAsObject = gson.<Map<String, String>> fromJson(newStackDefaultJson, Map.class);
      } else {
        existingJson = gson.<Map<String, Object>> fromJson(existingPropertyJson,
            Map.class);

        newStackJsonAsObject = gson.<Map<String, Map<Object, Object>>> fromJson(newStackDefaultJson,
            Map.class);
      }

      if (existingJson.keySet().contains(stackId.getStackName())) {
        continue;
      }

      existingJson.put(stackId.getStackName(), newStackJsonAsObject.get(stackId.getStackName()));

      String newJson = gson.toJson(existingJson);
      updatedProperties.put(property, newJson);
    }

    if (!updatedProperties.isEmpty()) {
      AmbariManagementController amc = getManagementController();
      String serviceNote = String.format(
          "Adding stack tools for %s while distributing a new repository", stackId.toString());

      configHelper.updateConfigType(cluster, stackId, amc, clusterEnv.getType(), updatedProperties,
          null, amc.getAuthName(), serviceNote);
    }
  }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/state/ConfigHelper.java b/ambari-server/src/main/java/org/apache/ambari/server/state/ConfigHelper.java
index 9f75bf99f3..a3a676d9d7 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/state/ConfigHelper.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/state/ConfigHelper.java
@@ -88,8 +88,10 @@ public class ConfigHelper {
   public static final String CLUSTER_ENV_RETRY_COMMANDS = "commands_to_retry";
   public static final String CLUSTER_ENV_RETRY_MAX_TIME_IN_SEC = "command_retry_max_time_in_sec";
   public static final String COMMAND_RETRY_MAX_TIME_IN_SEC_DEFAULT = "600";
  public static final String CLUSTER_ENV_STACK_NAME_PROPERTY = "stack_name";
   public static final String CLUSTER_ENV_STACK_FEATURES_PROPERTY = "stack_features";
   public static final String CLUSTER_ENV_STACK_TOOLS_PROPERTY = "stack_tools";
  public static final String CLUSTER_ENV_STACK_ROOT_PROPERTY = "stack_root";
 
   public static final String HTTP_ONLY = "HTTP_ONLY";
   public static final String HTTPS_ONLY = "HTTPS_ONLY";
@@ -1145,6 +1147,36 @@ public class ConfigHelper {
   /**
    * Gets the default properties for the specified service. These properties
    * represent those which would be used when a service is first installed.
   *
   * @param stack
   *          the stack to pull stack-values from (not {@code null})
   * @return a mapping of configuration type to map of key/value pairs for the
   *         default configurations.
   * @throws AmbariException
   */
  public Map<String, Map<String, String>> getDefaultStackProperties(StackId stack)
      throws AmbariException {
    Map<String, Map<String, String>> defaultPropertiesByType = new HashMap<>();

    // populate the stack (non-service related) properties
    Set<org.apache.ambari.server.state.PropertyInfo> stackConfigurationProperties = ambariMetaInfo.getStackProperties(
        stack.getStackName(), stack.getStackVersion());

    for (PropertyInfo stackDefaultProperty : stackConfigurationProperties) {
      String type = ConfigHelper.fileNameToConfigType(stackDefaultProperty.getFilename());

      if (!defaultPropertiesByType.containsKey(type)) {
        defaultPropertiesByType.put(type, new HashMap<String, String>());
      }

      defaultPropertiesByType.get(type).put(stackDefaultProperty.getName(),
          stackDefaultProperty.getValue());
    }

    return defaultPropertiesByType;
  }

  /**
    *
    * @param stack
    *          the stack to pull stack-values from (not {@code null})
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/AmbariContext.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/AmbariContext.java
index 0467b9b859..9b64edc8e5 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/topology/AmbariContext.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/AmbariContext.java
@@ -69,6 +69,7 @@ import org.apache.ambari.server.state.Cluster;
 import org.apache.ambari.server.state.Clusters;
 import org.apache.ambari.server.state.Config;
 import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
 import org.apache.ambari.server.state.DesiredConfig;
 import org.apache.ambari.server.state.Host;
 import org.apache.ambari.server.state.SecurityType;
@@ -80,6 +81,7 @@ import org.slf4j.LoggerFactory;
 
 import com.google.common.collect.Iterables;
 import com.google.common.collect.Sets;
import com.google.inject.Provider;
 
 
 /**
@@ -100,6 +102,12 @@ public class AmbariContext {
   @Inject
   ConfigFactory configFactory;
 
  /**
   * Used for getting configuration property values from stack and services.
   */
  @Inject
  private Provider<ConfigHelper> configHelper;

   private static AmbariManagementController controller;
   private static ClusterController clusterController;
   //todo: task id's.  Use existing mechanism for getting next task id sequence
@@ -674,6 +682,16 @@ public class AmbariContext {
     return String.format("%s:%s", bpName, hostGroupName);
   }
 
  /**
   * Gets an instance of {@link ConfigHelper} for classes which are not
   * dependency injected.
   *
   * @return a {@link ConfigHelper} instance.
   */
  public ConfigHelper getConfigHelper() {
    return configHelper.get();
  }

   private synchronized HostResourceProvider getHostResourceProvider() {
     if (hostResourceProvider == null) {
       hostResourceProvider = (HostResourceProvider)
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog252.java b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog252.java
index 74f8f35349..fa3aea326f 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog252.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog252.java
@@ -18,10 +18,20 @@
 package org.apache.ambari.server.upgrade;
 
 import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
 
 import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.hadoop.metrics2.sink.relocated.commons.lang.StringUtils;
 
import com.google.common.collect.Sets;
 import com.google.inject.Inject;
 import com.google.inject.Injector;
 
@@ -33,6 +43,8 @@ public class UpgradeCatalog252 extends AbstractUpgradeCatalog {
   static final String CLUSTERCONFIG_TABLE = "clusterconfig";
   static final String SERVICE_DELETED_COLUMN = "service_deleted";
 
  private static final String CLUSTER_ENV = "cluster-env";

   /**
    * Constructor.
    *
@@ -79,6 +91,7 @@ public class UpgradeCatalog252 extends AbstractUpgradeCatalog {
    */
   @Override
   protected void executeDMLUpdates() throws AmbariException, SQLException {
    resetStackToolsAndFeatures();
   }
 
   /**
@@ -91,4 +104,52 @@ public class UpgradeCatalog252 extends AbstractUpgradeCatalog {
     dbAccessor.addColumn(CLUSTERCONFIG_TABLE,
         new DBColumnInfo(SERVICE_DELETED_COLUMN, Short.class, null, 0, false));
   }

  /**
   * Resets the following properties in {@code cluster-env} to their new
   * defaults:
   * <ul>
   * <li>stack_root
   * <li>stack_tools
   * <li>stack_features
   * <ul>
   *
   * @throws AmbariException
   */
  private void resetStackToolsAndFeatures() throws AmbariException {
    Set<String> propertiesToReset = Sets.newHashSet("stack_tools", "stack_features", "stack_root");

    Clusters clusters = injector.getInstance(Clusters.class);
    ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);

    Map<String, Cluster> clusterMap = clusters.getClusters();
    for (Cluster cluster : clusterMap.values()) {
      Config clusterEnv = cluster.getDesiredConfigByType(CLUSTER_ENV);
      if (null == clusterEnv) {
        continue;
      }

      Map<String, String> newStackProperties = new HashMap<>();
      Set<PropertyInfo> stackProperties = configHelper.getStackProperties(cluster);
      if (null == stackProperties) {
        continue;
      }

      for (PropertyInfo propertyInfo : stackProperties) {
        String fileName = propertyInfo.getFilename();
        if (StringUtils.isEmpty(fileName)) {
          continue;
        }

        if (StringUtils.equals(ConfigHelper.fileNameToConfigType(fileName), CLUSTER_ENV)) {
          String stackPropertyName = propertyInfo.getName();
          if (propertiesToReset.contains(stackPropertyName)) {
            newStackProperties.put(stackPropertyName, propertyInfo.getValue());
          }
        }
      }

      updateConfigurationPropertiesForCluster(cluster, CLUSTER_ENV, newStackProperties, true, false);
    }
  }
 }
diff --git a/ambari-server/src/main/resources/common-services/HIVE/0.12.0.2.0/package/alerts/alert_hive_metastore.py b/ambari-server/src/main/resources/common-services/HIVE/0.12.0.2.0/package/alerts/alert_hive_metastore.py
index 32df7d3819..5b4fd6846b 100644
-- a/ambari-server/src/main/resources/common-services/HIVE/0.12.0.2.0/package/alerts/alert_hive_metastore.py
++ b/ambari-server/src/main/resources/common-services/HIVE/0.12.0.2.0/package/alerts/alert_hive_metastore.py
@@ -27,6 +27,7 @@ import logging
 from resource_management.core import global_lock
 from resource_management.libraries.functions import format
 from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions import stack_tools
 from resource_management.core.resources import Execute
 from resource_management.core.signal_utils import TerminateStrategy
 from ambari_commons.os_check import OSConst
@@ -56,6 +57,7 @@ SMOKEUSER_PRINCIPAL_DEFAULT = 'ambari-qa@EXAMPLE.COM'
 SMOKEUSER_SCRIPT_PARAM_KEY = 'default.smoke.user'
 SMOKEUSER_DEFAULT = 'ambari-qa'
 
STACK_NAME = '{{cluster-env/stack_name}}'
 STACK_ROOT = '{{cluster-env/stack_root}}'
 
 HIVE_CONF_DIR_LEGACY = '/etc/hive/conf.server'
@@ -78,7 +80,7 @@ def get_tokens():
   """
   return (SECURITY_ENABLED_KEY,SMOKEUSER_KEYTAB_KEY,SMOKEUSER_PRINCIPAL_KEY,
     HIVE_METASTORE_URIS_KEY, SMOKEUSER_KEY, KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY,
    STACK_ROOT)
    STACK_NAME, STACK_ROOT)
 
 @OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
 def get_tokens():
@@ -175,9 +177,10 @@ def execute(configurations={}, parameters={}, host_name=None):
     bin_dir = HIVE_BIN_DIR_LEGACY
 
 
    if STACK_ROOT in configurations:
      hive_conf_dir = configurations[STACK_ROOT] + format("/current/hive-metastore/conf")
      hive_bin_dir = configurations[STACK_ROOT] + format("/current/hive-metastore/bin")
    if STACK_NAME in configurations and STACK_ROOT in configurations:
      stack_root = stack_tools.get_stack_root(configurations[STACK_NAME], configurations[STACK_ROOT])
      hive_conf_dir = stack_root + format("/current/hive-metastore/conf")
      hive_bin_dir = stack_root + format("/current/hive-metastore/bin")
 
       if os.path.exists(hive_conf_dir):
         conf_dir = hive_conf_dir
diff --git a/ambari-server/src/main/resources/common-services/HIVE/0.12.0.2.0/package/alerts/alert_llap_app_status.py b/ambari-server/src/main/resources/common-services/HIVE/0.12.0.2.0/package/alerts/alert_llap_app_status.py
index 98d1899776..e46c896507 100644
-- a/ambari-server/src/main/resources/common-services/HIVE/0.12.0.2.0/package/alerts/alert_llap_app_status.py
++ b/ambari-server/src/main/resources/common-services/HIVE/0.12.0.2.0/package/alerts/alert_llap_app_status.py
@@ -26,7 +26,7 @@ import subprocess
 
 from resource_management.libraries.functions import format
 from resource_management.libraries.functions import get_kinit_path
from ambari_commons.os_check import OSConst
from resource_management.libraries.functions import stack_tools
 from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
 from resource_management.core import shell
 from resource_management.core.resources import Execute
@@ -58,6 +58,7 @@ HIVE_AUTHENTICATION_DEFAULT = 'NOSASL'
 HIVE_USER_KEY = '{{hive-env/hive_user}}'
 HIVE_USER_DEFAULT = 'default.smoke.user'
 
STACK_NAME = '{{cluster-env/stack_name}}'
 STACK_ROOT = '{{cluster-env/stack_root}}'
 STACK_ROOT_DEFAULT = Script.get_stack_root()
 
@@ -88,7 +89,7 @@ def get_tokens():
   to build the dictionary passed into execute
   """
   return (SECURITY_ENABLED_KEY, KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY, HIVE_PRINCIPAL_KEY, HIVE_PRINCIPAL_KEYTAB_KEY,
          HIVE_USER_KEY, STACK_ROOT, LLAP_APP_NAME_KEY)
          HIVE_USER_KEY, STACK_NAME, STACK_ROOT, LLAP_APP_NAME_KEY)
 
 
 @OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
@@ -159,8 +160,11 @@ def execute(configurations={}, parameters={}, host_name=None):
 
 
     start_time = time.time()
    if STACK_ROOT in configurations:
      llap_status_cmd = configurations[STACK_ROOT] + format("/current/hive-server2-hive2/bin/hive --service llapstatus --name {llap_app_name}  --findAppTimeout {LLAP_APP_STATUS_CMD_TIMEOUT}")
    if STACK_NAME in configurations and STACK_ROOT in configurations:
      stack_root = stack_tools.get_stack_root(configurations[STACK_NAME],
        configurations[STACK_ROOT])

      llap_status_cmd = stack_root + format("/current/hive-server2-hive2/bin/hive --service llapstatus --name {llap_app_name}  --findAppTimeout {LLAP_APP_STATUS_CMD_TIMEOUT}")
     else:
       llap_status_cmd = STACK_ROOT_DEFAULT + format("/current/hive-server2-hive2/bin/hive --service llapstatus --name {llap_app_name} --findAppTimeout {LLAP_APP_STATUS_CMD_TIMEOUT}")
 
diff --git a/ambari-server/src/main/resources/common-services/OOZIE/4.0.0.2.0/package/alerts/alert_check_oozie_server.py b/ambari-server/src/main/resources/common-services/OOZIE/4.0.0.2.0/package/alerts/alert_check_oozie_server.py
index 0e9fe741d0..54eef188d2 100644
-- a/ambari-server/src/main/resources/common-services/OOZIE/4.0.0.2.0/package/alerts/alert_check_oozie_server.py
++ b/ambari-server/src/main/resources/common-services/OOZIE/4.0.0.2.0/package/alerts/alert_check_oozie_server.py
@@ -26,6 +26,7 @@ from resource_management.core.resources import Execute
 from resource_management.libraries.functions import format
 from resource_management.libraries.functions import get_kinit_path
 from resource_management.libraries.functions import get_klist_path
from resource_management.libraries.functions import stack_tools
 from ambari_commons.os_check import OSConst, OSCheck
 from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
 from urlparse import urlparse
@@ -66,6 +67,7 @@ USER_PRINCIPAL_DEFAULT = 'oozie@EXAMPLE.COM'
 # default user
 USER_DEFAULT = 'oozie'
 
STACK_NAME_KEY = '{{cluster-env/stack_name}}'
 STACK_ROOT_KEY = '{{cluster-env/stack_root}}'
 STACK_ROOT_DEFAULT = '/usr/hdp'
 
@@ -86,7 +88,7 @@ def get_tokens():
   to build the dictionary passed into execute
   """
   return (OOZIE_URL_KEY, USER_PRINCIPAL_KEY, SECURITY_ENABLED, USER_KEYTAB_KEY, KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY,
          USER_KEY, OOZIE_HTTPS_PORT, OOZIE_ENV_CONTENT, STACK_ROOT_KEY)
          USER_KEY, OOZIE_HTTPS_PORT, OOZIE_ENV_CONTENT, STACK_NAME_KEY, STACK_ROOT_KEY)
 
 @OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
 def get_check_command(oozie_url, host_name, configurations):
@@ -158,8 +160,8 @@ def get_check_command(oozie_url, host_name, configurations, parameters, only_kin
 
   # Configure stack root
   stack_root = STACK_ROOT_DEFAULT
  if STACK_ROOT_KEY in configurations:
    stack_root = configurations[STACK_ROOT_KEY].lower()
  if STACK_NAME_KEY in configurations and STACK_ROOT_KEY in configurations:
    stack_root = stack_tools.get_stack_root(configurations[STACK_NAME_KEY], configurations[STACK_ROOT_KEY]).lower()
 
   # oozie configuration directory using a symlink
   oozie_config_directory = OOZIE_CONF_DIR.replace(STACK_ROOT_PATTERN, stack_root)
diff --git a/ambari-server/src/main/resources/host_scripts/alert_disk_space.py b/ambari-server/src/main/resources/host_scripts/alert_disk_space.py
index 4c5834f83b..f3c64060c1 100644
-- a/ambari-server/src/main/resources/host_scripts/alert_disk_space.py
++ b/ambari-server/src/main/resources/host_scripts/alert_disk_space.py
@@ -23,6 +23,7 @@ import os
 import platform
 from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
 from ambari_commons import OSConst
from resource_management.libraries.functions import stack_tools
 
 DiskInfo = collections.namedtuple('DiskInfo', 'total used free path')
 
@@ -36,6 +37,7 @@ MIN_FREE_SPACE_DEFAULT = 5000000000L
 PERCENT_USED_WARNING_DEFAULT = 50
 PERCENT_USED_CRITICAL_DEFAULT = 80
 
STACK_NAME = '{{cluster-env/stack_name}}'
 STACK_ROOT = '{{cluster-env/stack_root}}'
 
 def get_tokens():
@@ -43,7 +45,7 @@ def get_tokens():
   Returns a tuple of tokens in the format {{site/property}} that will be used
   to build the dictionary passed into execute
   """
  return (STACK_ROOT, )
  return (STACK_NAME, STACK_ROOT)
 
 
 @OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
@@ -64,10 +66,10 @@ def execute(configurations={}, parameters={}, host_name=None):
   if configurations is None:
     return (('UNKNOWN', ['There were no configurations supplied to the script.']))
 
  if not STACK_ROOT in configurations:
    return (('STACK_ROOT', ['cluster-env/stack_root is not specified']))
  if not STACK_NAME in configurations or not STACK_ROOT in configurations:
    return (('STACK_ROOT', ['cluster-env/stack_name and cluster-env/stack_root are required']))
 
  path = configurations[STACK_ROOT]
  path = stack_tools.get_stack_root(configurations[STACK_NAME], configurations[STACK_ROOT])
 
   try:
     disk_usage = _get_disk_usage(path)
diff --git a/ambari-server/src/main/resources/host_scripts/alert_version_select.py b/ambari-server/src/main/resources/host_scripts/alert_version_select.py
index 0ce79e7305..f54ccad7e7 100644
-- a/ambari-server/src/main/resources/host_scripts/alert_version_select.py
++ b/ambari-server/src/main/resources/host_scripts/alert_version_select.py
@@ -31,6 +31,7 @@ RESULT_STATE_WARNING = 'WARNING'
 RESULT_STATE_CRITICAL = 'CRITICAL'
 RESULT_STATE_UNKNOWN = 'UNKNOWN'
 
STACK_NAME = '{{cluster-env/stack_name}}'
 STACK_TOOLS = '{{cluster-env/stack_tools}}'
 
 
@@ -42,7 +43,7 @@ def get_tokens():
   Returns a tuple of tokens in the format {{site/property}} that will be used
   to build the dictionary passed into execute
   """
  return (STACK_TOOLS,)
  return (STACK_NAME, STACK_TOOLS)
 
 
 def execute(configurations={}, parameters={}, host_name=None):
@@ -65,8 +66,10 @@ def execute(configurations={}, parameters={}, host_name=None):
     if STACK_TOOLS not in configurations:
       return (RESULT_STATE_UNKNOWN, ['{0} is a required parameter for the script'.format(STACK_TOOLS)])
 
    stack_name = Script.get_stack_name()

     # Of the form,
    # { "stack_selector": ["hdp-select", "/usr/bin/hdp-select", "hdp-select"], "conf_selector": ["conf-select", "/usr/bin/conf-select", "conf-select"] }
    # { "HDP" : { "stack_selector": ["hdp-select", "/usr/bin/hdp-select", "hdp-select"], "conf_selector": ["conf-select", "/usr/bin/conf-select", "conf-select"] } }
     stack_tools_str = configurations[STACK_TOOLS]
 
     if stack_tools_str is None:
@@ -75,6 +78,7 @@ def execute(configurations={}, parameters={}, host_name=None):
     distro_select = "unknown-distro-select"
     try:
       stack_tools = json.loads(stack_tools_str)
      stack_tools = stack_tools[stack_name]
       distro_select = stack_tools["stack_selector"][0]
     except:
       pass
@@ -87,18 +91,18 @@ def execute(configurations={}, parameters={}, host_name=None):
       (code, out, versions) = unsafe_get_stack_versions()
 
       if code == 0:
        msg.append("Ok. {0}".format(distro_select))
        msg.append("{0} ".format(distro_select))
         if versions is not None and type(versions) is list and len(versions) > 0:
          msg.append("Versions: {0}".format(", ".join(versions)))
          msg.append("reported the following versions: {0}".format(", ".join(versions)))
         return (RESULT_STATE_OK, ["\n".join(msg)])
       else:
        msg.append("Failed, check dir {0} for unexpected contents.".format(stack_root_dir))
        msg.append("{0} could not properly read {1}. Check this directory for unexpected contents.".format(distro_select, stack_root_dir))
         if out is not None:
           msg.append(out)
 
         return (RESULT_STATE_CRITICAL, ["\n".join(msg)])
     else:
      msg.append("Ok. No stack root {0} to check.".format(stack_root_dir))
      msg.append("No stack root {0} to check.".format(stack_root_dir))
       return (RESULT_STATE_OK, ["\n".join(msg)])
   except Exception, e:
     return (RESULT_STATE_CRITICAL, [e.message])
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6/configuration/cluster-env.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6/configuration/cluster-env.xml
index f7d5de54ef..e6ec28511e 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6/configuration/cluster-env.xml
++ b/ambari-server/src/main/resources/stacks/HDP/2.0.6/configuration/cluster-env.xml
@@ -220,6 +220,18 @@ gpgcheck=0</value>
     </value-attributes>
     <on-ambari-upgrade add="true"/>
   </property>
  <!-- Define stack_name property in the base stack. DO NOT override this property for each stack version -->
  <property>
    <name>stack_name</name>
    <value>HDP</value>
    <description>The name of the stack.</description>
    <value-attributes>
      <read-only>true</read-only>
      <overridable>false</overridable>
      <visible>false</visible>
    </value-attributes>
    <on-ambari-upgrade add="true"/>
  </property>
   <!-- Define stack_tools property in the base stack. DO NOT override this property for each stack version -->
   <property>
     <name>stack_tools</name>
@@ -252,8 +264,8 @@ gpgcheck=0</value>
   </property>
   <property>
     <name>stack_root</name>
    <value>/usr/hdp</value>
    <description>Stack root folder</description>
    <value>{"HDP":"/usr/hdp"}</value>
    <description>JSON which defines the stack root by stack name</description>
     <value-attributes>
       <read-only>true</read-only>
       <overridable>false</overridable>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6/properties/stack_features.json b/ambari-server/src/main/resources/stacks/HDP/2.0.6/properties/stack_features.json
index 878645b62a..31cf0c869d 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6/properties/stack_features.json
++ b/ambari-server/src/main/resources/stacks/HDP/2.0.6/properties/stack_features.json
@@ -1,427 +1,429 @@
 {
  "stack_features": [
    {
      "name": "snappy",
      "description": "Snappy compressor/decompressor support",
      "min_version": "2.0.0.0",
      "max_version": "2.2.0.0"
    },
    {
      "name": "lzo",
      "description": "LZO libraries support",
      "min_version": "2.2.1.0"
    },
    {
      "name": "express_upgrade",
      "description": "Express upgrade support",
      "min_version": "2.1.0.0"
    },
    {
      "name": "rolling_upgrade",
      "description": "Rolling upgrade support",
      "min_version": "2.2.0.0"
    },
    {
      "name": "kafka_acl_migration_support",
      "description": "ACL migration support",
      "min_version": "2.3.4.0"
    },
    {
      "name": "secure_zookeeper",
      "description": "Protect ZNodes with SASL acl in secure clusters",
      "min_version": "2.6.0.0"
    },
    {
      "name": "config_versioning",
      "description": "Configurable versions support",
      "min_version": "2.3.0.0"
    },
    {
      "name": "datanode_non_root",
      "description": "DataNode running as non-root support (AMBARI-7615)",
      "min_version": "2.2.0.0"
    },
    {
      "name": "remove_ranger_hdfs_plugin_env",
      "description": "HDFS removes Ranger env files (AMBARI-14299)",
      "min_version": "2.3.0.0"
    },
    {
      "name": "ranger",
      "description": "Ranger Service support",
      "min_version": "2.2.0.0"
    },
    {
      "name": "ranger_tagsync_component",
      "description": "Ranger Tagsync component support (AMBARI-14383)",
      "min_version": "2.5.0.0"
    },
    {
      "name": "phoenix",
      "description": "Phoenix Service support",
      "min_version": "2.3.0.0"
    },
    {
      "name": "nfs",
      "description": "NFS support",
      "min_version": "2.3.0.0"
    },
    {
      "name": "tez_for_spark",
      "description": "Tez dependency for Spark",
      "min_version": "2.2.0.0",
      "max_version": "2.3.0.0"
    },
    {
      "name": "timeline_state_store",
      "description": "Yarn application timeline-service supports state store property (AMBARI-11442)",
      "min_version": "2.2.0.0"
    },
    {
      "name": "copy_tarball_to_hdfs",
      "description": "Copy tarball to HDFS support (AMBARI-12113)",
      "min_version": "2.2.0.0"
    },
    {
      "name": "spark_16plus",
      "description": "Spark 1.6+",
      "min_version": "2.4.0.0"
    },
    {
      "name": "spark_thriftserver",
      "description": "Spark Thrift Server",
      "min_version": "2.3.2.0"
    },
    {
      "name": "storm_kerberos",
      "description": "Storm Kerberos support (AMBARI-7570)",
      "min_version": "2.2.0.0"
    },
    {
      "name": "storm_ams",
      "description": "Storm AMS integration (AMBARI-10710)",
      "min_version": "2.2.0.0"
    },
    {
      "name": "create_kafka_broker_id",
      "description": "Ambari should create Kafka Broker Id (AMBARI-12678)",
      "min_version": "2.2.0.0",
      "max_version": "2.3.0.0"
    },
    {
      "name": "kafka_listeners",
      "description": "Kafka listeners (AMBARI-10984)",
      "min_version": "2.3.0.0"
    },
    {
      "name": "kafka_kerberos",
      "description": "Kafka Kerberos support (AMBARI-10984)",
      "min_version": "2.3.0.0"
    },
    {
      "name": "pig_on_tez",
      "description": "Pig on Tez support (AMBARI-7863)",
      "min_version": "2.2.0.0"
    },
    {
      "name": "ranger_usersync_non_root",
      "description": "Ranger Usersync as non-root user (AMBARI-10416)",
      "min_version": "2.3.0.0"
    },
    {
      "name": "ranger_audit_db_support",
      "description": "Ranger Audit to DB support",
      "min_version": "2.2.0.0",
      "max_version": "2.4.99.99"
    },
    {
      "name": "accumulo_kerberos_user_auth",
      "description": "Accumulo Kerberos User Auth (AMBARI-10163)",
      "min_version": "2.3.0.0"
    },
    {
      "name": "knox_versioned_data_dir",
      "description": "Use versioned data dir for Knox (AMBARI-13164)",
      "min_version": "2.3.2.0"
    },
    {
      "name": "knox_sso_topology",
      "description": "Knox SSO Topology support (AMBARI-13975)",
      "min_version": "2.3.8.0"
    },
    {
      "name": "atlas_rolling_upgrade",
      "description": "Rolling upgrade support for Atlas",
      "min_version": "2.3.0.0"
    },
    {
      "name": "oozie_admin_user",
      "description": "Oozie install user as an Oozie admin user (AMBARI-7976)",
      "min_version": "2.2.0.0"
    },
    {
      "name": "oozie_create_hive_tez_configs",
      "description": "Oozie create configs for Ambari Hive and Tez deployments (AMBARI-8074)",
      "min_version": "2.2.0.0"
    },
    {
      "name": "oozie_setup_shared_lib",
      "description": "Oozie setup tools used to shared Oozie lib to HDFS (AMBARI-7240)",
      "min_version": "2.2.0.0"
    },
    {
      "name": "oozie_host_kerberos",
      "description": "Oozie in secured clusters uses _HOST in Kerberos principal (AMBARI-9775)",
      "min_version": "2.0.0.0"
    },
    {
      "name": "falcon_extensions",
      "description": "Falcon Extension",
      "min_version": "2.5.0.0"
    },
    {
      "name": "hive_metastore_upgrade_schema",
      "description": "Hive metastore upgrade schema support (AMBARI-11176)",
      "min_version": "2.3.0.0"
     },
    {
      "name": "hive_server_interactive",
      "description": "Hive server interactive support (AMBARI-15573)",
      "min_version": "2.5.0.0"
     },
    {
      "name": "hive_webhcat_specific_configs",
      "description": "Hive webhcat specific configurations support (AMBARI-12364)",
      "min_version": "2.3.0.0"
     },
    {
      "name": "hive_purge_table",
      "description": "Hive purge table support (AMBARI-12260)",
      "min_version": "2.3.0.0"
     },
    {
      "name": "hive_server2_kerberized_env",
      "description": "Hive server2 working on kerberized environment (AMBARI-13749)",
      "min_version": "2.2.3.0",
      "max_version": "2.2.5.0"
     },
    {
      "name": "hive_env_heapsize",
      "description": "Hive heapsize property defined in hive-env (AMBARI-12801)",
      "min_version": "2.2.0.0"
    },
    {
      "name": "ranger_kms_hsm_support",
      "description": "Ranger KMS HSM support (AMBARI-15752)",
      "min_version": "2.5.0.0"
    },
    {
      "name": "ranger_log4j_support",
      "description": "Ranger supporting log-4j properties (AMBARI-15681)",
      "min_version": "2.5.0.0"
    },
    {
      "name": "ranger_kerberos_support",
      "description": "Ranger Kerberos support",
      "min_version": "2.5.0.0"
    },
    {
      "name": "hive_metastore_site_support",
      "description": "Hive Metastore site support",
      "min_version": "2.5.0.0"
    },
    {
      "name": "ranger_usersync_password_jceks",
      "description": "Saving Ranger Usersync credentials in jceks",
      "min_version": "2.5.0.0"
    },
    {
      "name": "ranger_install_infra_client",
      "description": "Ambari Infra Service support",
      "min_version": "2.5.0.0"
    },
    {
      "name": "falcon_atlas_support_2_3",
      "description": "Falcon Atlas integration support for 2.3 stack",
      "min_version": "2.3.99.0",
      "max_version": "2.4.0.0"
    },
    {
      "name": "falcon_atlas_support",
      "description": "Falcon Atlas integration",
      "min_version": "2.5.0.0"
    },
    {
      "name": "hbase_home_directory",
      "description": "Hbase home directory in HDFS needed for HBASE backup",
      "min_version": "2.5.0.0"
    },
    {
      "name": "spark_livy",
      "description": "Livy as slave component of spark",
      "min_version": "2.5.0.0"
    },
    {
      "name": "spark_livy2",
      "description": "Livy as slave component of spark",
      "min_version": "2.6.0.0"
    },
    {
      "name": "atlas_ranger_plugin_support",
      "description": "Atlas Ranger plugin support",
      "min_version": "2.5.0.0"
    },
    {
      "name": "atlas_conf_dir_in_path",
      "description": "Prepend the Atlas conf dir (/etc/atlas/conf) to the classpath of Storm and Falcon",
      "min_version": "2.3.0.0",
      "max_version": "2.4.99.99"
    },
    {
      "name": "atlas_upgrade_support",
      "description": "Atlas supports express and rolling upgrades",
      "min_version": "2.5.0.0"
    },
    {
      "name": "atlas_hook_support",
      "description": "Atlas support for hooks in Hive, Storm, Falcon, and Sqoop",
      "min_version": "2.5.0.0"
    },
    {
      "name": "ranger_pid_support",
      "description": "Ranger Service support pid generation AMBARI-16756",
      "min_version": "2.5.0.0"
    },
    {
      "name": "ranger_kms_pid_support",
      "description": "Ranger KMS Service support pid generation",
      "min_version": "2.5.0.0"
    },
    {
      "name": "ranger_admin_password_change",
      "description": "Allow ranger admin credentials to be specified during cluster creation (AMBARI-17000)",
      "min_version": "2.5.0.0"
    },
    {
      "name": "ranger_setup_db_on_start",
      "description": "Allows setup of ranger db and java patches to be called multiple times on each START",
      "min_version": "2.6.0.0"
    },
    {
      "name": "storm_metrics_apache_classes",
      "description": "Metrics sink for Storm that uses Apache class names",
      "min_version": "2.5.0.0"
    },
    {
      "name": "spark_java_opts_support",
      "description": "Allow Spark to generate java-opts file",
      "min_version": "2.2.0.0",
      "max_version": "2.4.0.0"
    },
    {
      "name": "atlas_hbase_setup",
      "description": "Use script to create Atlas tables in Hbase and set permissions for Atlas user.",
      "min_version": "2.5.0.0"
    },
    {
      "name": "ranger_hive_plugin_jdbc_url",
      "description": "Handle Ranger hive repo config jdbc url change for stack 2.5 (AMBARI-18386)",
      "min_version": "2.5.0.0"
    },
    {
      "name": "zkfc_version_advertised",
      "description": "ZKFC advertise version",
      "min_version": "2.5.0.0"
    },
    {
      "name": "phoenix_core_hdfs_site_required",
      "description": "HDFS and CORE site required for Phoenix",
      "max_version": "2.5.9.9"
    },
    {
      "name": "ranger_tagsync_ssl_xml_support",
      "description": "Ranger Tagsync ssl xml support.",
      "min_version": "2.6.0.0"
    },
    {
      "name": "ranger_xml_configuration",
      "description": "Ranger code base support xml configurations",
      "min_version": "2.3.0.0"
    },
    {
      "name": "kafka_ranger_plugin_support",
      "description": "Ambari stack changes for Ranger Kafka Plugin (AMBARI-11299)",
      "min_version": "2.3.0.0"
    },
    {
      "name": "yarn_ranger_plugin_support",
      "description": "Implement Stack changes for Ranger Yarn Plugin integration (AMBARI-10866)",
      "min_version": "2.3.0.0"
    },
    {
      "name": "ranger_solr_config_support",
      "description": "Showing Ranger solrconfig.xml on UI",
      "min_version": "2.6.0.0"
    },
    {
      "name": "hive_interactive_atlas_hook_required",
      "description": "Registering Atlas Hook for Hive Interactive.",
      "min_version": "2.6.0.0"
    },
    {
      "name": "core_site_for_ranger_plugins",
      "description": "Adding core-site.xml in when Ranger plugin is enabled for Storm, Kafka, and Knox.",
      "min_version": "2.6.0.0"
    },
    {
      "name": "atlas_install_hook_package_support",
      "description": "Stop installing packages from 2.6",
      "max_version": "2.5.9.9"
    },
    {
      "name": "atlas_hdfs_site_on_namenode_ha",
      "description": "Need to create hdfs-site under atlas-conf dir when Namenode-HA is enabled.",
      "min_version": "2.6.0.0"
    },
    {
      "name": "hive_interactive_ga",
      "description": "Hive Interactive GA support",
      "min_version": "2.6.0.0"
    },
    {
      "name": "secure_ranger_ssl_password",
      "description": "Securing Ranger Admin and Usersync SSL and Trustore related passwords in jceks",
      "min_version": "2.6.0.0"
    },
    {
      "name": "ranger_kms_ssl",
      "description": "Ranger KMS SSL properties in ambari stack",
      "min_version": "2.6.0.0"
    },
    {
      "name": "nifi_encrypt_config",
      "description": "Encrypt sensitive properties written to nifi property file",
      "min_version": "2.6.0.0"
    },
    {
      "name": "toolkit_config_update",
      "description": "Support separate input and output for toolkit configuration",
      "min_version": "2.6.0.0"
    },
    {
      "name": "admin_toolkit_support",
      "description": "Supports the nifi admin toolkit",
      "min_version": "2.6.0.0"
    },
    {
      "name": "tls_toolkit_san",
      "description": "Support subject alternative name flag",
      "min_version": "2.6.0.0"
    },
    {
      "name": "nifi_jaas_conf_create",
      "description": "Create NIFI jaas configuration when kerberos is enabled",
      "min_version": "2.6.0.0"
    }
  ]
  "HDP": {
    "stack_features": [
      {
        "name": "snappy",
        "description": "Snappy compressor/decompressor support",
        "min_version": "2.0.0.0",
        "max_version": "2.2.0.0"
      },
      {
        "name": "lzo",
        "description": "LZO libraries support",
        "min_version": "2.2.1.0"
      },
      {
        "name": "express_upgrade",
        "description": "Express upgrade support",
        "min_version": "2.1.0.0"
      },
      {
        "name": "rolling_upgrade",
        "description": "Rolling upgrade support",
        "min_version": "2.2.0.0"
      },
      {
        "name": "kafka_acl_migration_support",
        "description": "ACL migration support",
        "min_version": "2.3.4.0"
      },
      {
        "name": "secure_zookeeper",
        "description": "Protect ZNodes with SASL acl in secure clusters",
        "min_version": "2.6.0.0"
      },
      {
        "name": "config_versioning",
        "description": "Configurable versions support",
        "min_version": "2.3.0.0"
      },
      {
        "name": "datanode_non_root",
        "description": "DataNode running as non-root support (AMBARI-7615)",
        "min_version": "2.2.0.0"
      },
      {
        "name": "remove_ranger_hdfs_plugin_env",
        "description": "HDFS removes Ranger env files (AMBARI-14299)",
        "min_version": "2.3.0.0"
      },
      {
        "name": "ranger",
        "description": "Ranger Service support",
        "min_version": "2.2.0.0"
      },
      {
        "name": "ranger_tagsync_component",
        "description": "Ranger Tagsync component support (AMBARI-14383)",
        "min_version": "2.5.0.0"
      },
      {
        "name": "phoenix",
        "description": "Phoenix Service support",
        "min_version": "2.3.0.0"
      },
      {
        "name": "nfs",
        "description": "NFS support",
        "min_version": "2.3.0.0"
      },
      {
        "name": "tez_for_spark",
        "description": "Tez dependency for Spark",
        "min_version": "2.2.0.0",
        "max_version": "2.3.0.0"
      },
      {
        "name": "timeline_state_store",
        "description": "Yarn application timeline-service supports state store property (AMBARI-11442)",
        "min_version": "2.2.0.0"
      },
      {
        "name": "copy_tarball_to_hdfs",
        "description": "Copy tarball to HDFS support (AMBARI-12113)",
        "min_version": "2.2.0.0"
      },
      {
        "name": "spark_16plus",
        "description": "Spark 1.6+",
        "min_version": "2.4.0.0"
      },
      {
        "name": "spark_thriftserver",
        "description": "Spark Thrift Server",
        "min_version": "2.3.2.0"
      },
      {
        "name": "storm_kerberos",
        "description": "Storm Kerberos support (AMBARI-7570)",
        "min_version": "2.2.0.0"
      },
      {
        "name": "storm_ams",
        "description": "Storm AMS integration (AMBARI-10710)",
        "min_version": "2.2.0.0"
      },
      {
        "name": "create_kafka_broker_id",
        "description": "Ambari should create Kafka Broker Id (AMBARI-12678)",
        "min_version": "2.2.0.0",
        "max_version": "2.3.0.0"
      },
      {
        "name": "kafka_listeners",
        "description": "Kafka listeners (AMBARI-10984)",
        "min_version": "2.3.0.0"
      },
      {
        "name": "kafka_kerberos",
        "description": "Kafka Kerberos support (AMBARI-10984)",
        "min_version": "2.3.0.0"
      },
      {
        "name": "pig_on_tez",
        "description": "Pig on Tez support (AMBARI-7863)",
        "min_version": "2.2.0.0"
      },
      {
        "name": "ranger_usersync_non_root",
        "description": "Ranger Usersync as non-root user (AMBARI-10416)",
        "min_version": "2.3.0.0"
      },
      {
        "name": "ranger_audit_db_support",
        "description": "Ranger Audit to DB support",
        "min_version": "2.2.0.0",
        "max_version": "2.4.99.99"
      },
      {
        "name": "accumulo_kerberos_user_auth",
        "description": "Accumulo Kerberos User Auth (AMBARI-10163)",
        "min_version": "2.3.0.0"
      },
      {
        "name": "knox_versioned_data_dir",
        "description": "Use versioned data dir for Knox (AMBARI-13164)",
        "min_version": "2.3.2.0"
      },
      {
        "name": "knox_sso_topology",
        "description": "Knox SSO Topology support (AMBARI-13975)",
        "min_version": "2.3.8.0"
      },
      {
        "name": "atlas_rolling_upgrade",
        "description": "Rolling upgrade support for Atlas",
        "min_version": "2.3.0.0"
      },
      {
        "name": "oozie_admin_user",
        "description": "Oozie install user as an Oozie admin user (AMBARI-7976)",
        "min_version": "2.2.0.0"
      },
      {
        "name": "oozie_create_hive_tez_configs",
        "description": "Oozie create configs for Ambari Hive and Tez deployments (AMBARI-8074)",
        "min_version": "2.2.0.0"
      },
      {
        "name": "oozie_setup_shared_lib",
        "description": "Oozie setup tools used to shared Oozie lib to HDFS (AMBARI-7240)",
        "min_version": "2.2.0.0"
      },
      {
        "name": "oozie_host_kerberos",
        "description": "Oozie in secured clusters uses _HOST in Kerberos principal (AMBARI-9775)",
        "min_version": "2.0.0.0"
      },
      {
        "name": "falcon_extensions",
        "description": "Falcon Extension",
        "min_version": "2.5.0.0"
      },
      {
        "name": "hive_metastore_upgrade_schema",
        "description": "Hive metastore upgrade schema support (AMBARI-11176)",
        "min_version": "2.3.0.0"
      },
      {
        "name": "hive_server_interactive",
        "description": "Hive server interactive support (AMBARI-15573)",
        "min_version": "2.5.0.0"
      },
      {
        "name": "hive_webhcat_specific_configs",
        "description": "Hive webhcat specific configurations support (AMBARI-12364)",
        "min_version": "2.3.0.0"
      },
      {
        "name": "hive_purge_table",
        "description": "Hive purge table support (AMBARI-12260)",
        "min_version": "2.3.0.0"
      },
      {
        "name": "hive_server2_kerberized_env",
        "description": "Hive server2 working on kerberized environment (AMBARI-13749)",
        "min_version": "2.2.3.0",
        "max_version": "2.2.5.0"
      },
      {
        "name": "hive_env_heapsize",
        "description": "Hive heapsize property defined in hive-env (AMBARI-12801)",
        "min_version": "2.2.0.0"
      },
      {
        "name": "ranger_kms_hsm_support",
        "description": "Ranger KMS HSM support (AMBARI-15752)",
        "min_version": "2.5.0.0"
      },
      {
        "name": "ranger_log4j_support",
        "description": "Ranger supporting log-4j properties (AMBARI-15681)",
        "min_version": "2.5.0.0"
      },
      {
        "name": "ranger_kerberos_support",
        "description": "Ranger Kerberos support",
        "min_version": "2.5.0.0"
      },
      {
        "name": "hive_metastore_site_support",
        "description": "Hive Metastore site support",
        "min_version": "2.5.0.0"
      },
      {
        "name": "ranger_usersync_password_jceks",
        "description": "Saving Ranger Usersync credentials in jceks",
        "min_version": "2.5.0.0"
      },
      {
        "name": "ranger_install_infra_client",
        "description": "Ambari Infra Service support",
        "min_version": "2.5.0.0"
      },
      {
        "name": "falcon_atlas_support_2_3",
        "description": "Falcon Atlas integration support for 2.3 stack",
        "min_version": "2.3.99.0",
        "max_version": "2.4.0.0"
      },
      {
        "name": "falcon_atlas_support",
        "description": "Falcon Atlas integration",
        "min_version": "2.5.0.0"
      },
      {
        "name": "hbase_home_directory",
        "description": "Hbase home directory in HDFS needed for HBASE backup",
        "min_version": "2.5.0.0"
      },
      {
        "name": "spark_livy",
        "description": "Livy as slave component of spark",
        "min_version": "2.5.0.0"
      },
      {
        "name": "spark_livy2",
        "description": "Livy as slave component of spark",
        "min_version": "2.6.0.0"
      },
      {
        "name": "atlas_ranger_plugin_support",
        "description": "Atlas Ranger plugin support",
        "min_version": "2.5.0.0"
      },
      {
        "name": "atlas_conf_dir_in_path",
        "description": "Prepend the Atlas conf dir (/etc/atlas/conf) to the classpath of Storm and Falcon",
        "min_version": "2.3.0.0",
        "max_version": "2.4.99.99"
      },
      {
        "name": "atlas_upgrade_support",
        "description": "Atlas supports express and rolling upgrades",
        "min_version": "2.5.0.0"
      },
      {
        "name": "atlas_hook_support",
        "description": "Atlas support for hooks in Hive, Storm, Falcon, and Sqoop",
        "min_version": "2.5.0.0"
      },
      {
        "name": "ranger_pid_support",
        "description": "Ranger Service support pid generation AMBARI-16756",
        "min_version": "2.5.0.0"
      },
      {
        "name": "ranger_kms_pid_support",
        "description": "Ranger KMS Service support pid generation",
        "min_version": "2.5.0.0"
      },
      {
        "name": "ranger_admin_password_change",
        "description": "Allow ranger admin credentials to be specified during cluster creation (AMBARI-17000)",
        "min_version": "2.5.0.0"
      },
      {
        "name": "ranger_setup_db_on_start",
        "description": "Allows setup of ranger db and java patches to be called multiple times on each START",
        "min_version": "2.6.0.0"
      },
      {
        "name": "storm_metrics_apache_classes",
        "description": "Metrics sink for Storm that uses Apache class names",
        "min_version": "2.5.0.0"
      },
      {
        "name": "spark_java_opts_support",
        "description": "Allow Spark to generate java-opts file",
        "min_version": "2.2.0.0",
        "max_version": "2.4.0.0"
      },
      {
        "name": "atlas_hbase_setup",
        "description": "Use script to create Atlas tables in Hbase and set permissions for Atlas user.",
        "min_version": "2.5.0.0"
      },
      {
        "name": "ranger_hive_plugin_jdbc_url",
        "description": "Handle Ranger hive repo config jdbc url change for stack 2.5 (AMBARI-18386)",
        "min_version": "2.5.0.0"
      },
      {
        "name": "zkfc_version_advertised",
        "description": "ZKFC advertise version",
        "min_version": "2.5.0.0"
      },
      {
        "name": "phoenix_core_hdfs_site_required",
        "description": "HDFS and CORE site required for Phoenix",
        "max_version": "2.5.9.9"
      },
      {
        "name": "ranger_tagsync_ssl_xml_support",
        "description": "Ranger Tagsync ssl xml support.",
        "min_version": "2.6.0.0"
      },
      {
        "name": "ranger_xml_configuration",
        "description": "Ranger code base support xml configurations",
        "min_version": "2.3.0.0"
      },
      {
        "name": "kafka_ranger_plugin_support",
        "description": "Ambari stack changes for Ranger Kafka Plugin (AMBARI-11299)",
        "min_version": "2.3.0.0"
      },
      {
        "name": "yarn_ranger_plugin_support",
        "description": "Implement Stack changes for Ranger Yarn Plugin integration (AMBARI-10866)",
        "min_version": "2.3.0.0"
      },
      {
        "name": "ranger_solr_config_support",
        "description": "Showing Ranger solrconfig.xml on UI",
        "min_version": "2.6.0.0"
      },
      {
        "name": "hive_interactive_atlas_hook_required",
        "description": "Registering Atlas Hook for Hive Interactive.",
        "min_version": "2.6.0.0"
      },
      {
        "name": "core_site_for_ranger_plugins",
        "description": "Adding core-site.xml in when Ranger plugin is enabled for Storm, Kafka, and Knox.",
        "min_version": "2.6.0.0"
      },
      {
        "name": "atlas_install_hook_package_support",
        "description": "Stop installing packages from 2.6",
        "max_version": "2.5.9.9"
      },
      {
        "name": "atlas_hdfs_site_on_namenode_ha",
        "description": "Need to create hdfs-site under atlas-conf dir when Namenode-HA is enabled.",
        "min_version": "2.6.0.0"
      },
      {
        "name": "hive_interactive_ga",
        "description": "Hive Interactive GA support",
        "min_version": "2.6.0.0"
      },
      {
        "name": "secure_ranger_ssl_password",
        "description": "Securing Ranger Admin and Usersync SSL and Trustore related passwords in jceks",
        "min_version": "2.6.0.0"
      },
      {
        "name": "ranger_kms_ssl",
        "description": "Ranger KMS SSL properties in ambari stack",
        "min_version": "2.6.0.0"
      },
      {
        "name": "nifi_encrypt_config",
        "description": "Encrypt sensitive properties written to nifi property file",
        "min_version": "2.6.0.0"
      },
      {
        "name": "toolkit_config_update",
        "description": "Support separate input and output for toolkit configuration",
        "min_version": "2.6.0.0"
      },
      {
        "name": "admin_toolkit_support",
        "description": "Supports the nifi admin toolkit",
        "min_version": "2.6.0.0"
      },
      {
        "name": "tls_toolkit_san",
        "description": "Support subject alternative name flag",
        "min_version": "2.6.0.0"
      },
      {
        "name": "nifi_jaas_conf_create",
        "description": "Create NIFI jaas configuration when kerberos is enabled",
        "min_version": "2.6.0.0"
      }
    ]
  }
 }
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6/properties/stack_tools.json b/ambari-server/src/main/resources/stacks/HDP/2.0.6/properties/stack_tools.json
index d1aab4bba8..c515d579e8 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6/properties/stack_tools.json
++ b/ambari-server/src/main/resources/stacks/HDP/2.0.6/properties/stack_tools.json
@@ -1,4 +1,14 @@
 {
  "stack_selector": ["hdp-select", "/usr/bin/hdp-select", "hdp-select"],
  "conf_selector": ["conf-select", "/usr/bin/conf-select", "conf-select"]
}
\ No newline at end of file
  "HDP": {
    "stack_selector": [
      "hdp-select",
      "/usr/bin/hdp-select",
      "hdp-select"
    ],
    "conf_selector": [
      "conf-select",
      "/usr/bin/conf-select",
      "conf-select"
    ]
  }
}
diff --git a/ambari-server/src/main/resources/stacks/PERF/1.0/configuration/cluster-env.xml b/ambari-server/src/main/resources/stacks/PERF/1.0/configuration/cluster-env.xml
index 7df00eea8e..f19ac5237e 100644
-- a/ambari-server/src/main/resources/stacks/PERF/1.0/configuration/cluster-env.xml
++ b/ambari-server/src/main/resources/stacks/PERF/1.0/configuration/cluster-env.xml
@@ -20,6 +20,18 @@
  */
 -->
 <configuration>
  <!-- Define stack_name property in the base stack. DO NOT override this property for each stack version -->
  <property>
    <name>stack_name</name>
    <value>PERF</value>
    <description>The name of the stack.</description>
    <value-attributes>
      <read-only>true</read-only>
      <overridable>false</overridable>
      <visible>false</visible>
    </value-attributes>
    <on-ambari-upgrade add="true"/>
  </property>
 
   <!-- Define stack_tools property in the base stack. DO NOT override this property for each stack version -->
   <property>
@@ -55,8 +67,8 @@
 
   <property>
     <name>stack_root</name>
    <value>/usr/perf</value>
    <description>Stack root folder</description>
    <value>{"PERF":"/usr/perf"}</value>
    <description>JSON which defines the stack root by stack name</description>  
     <value-attributes>
       <read-only>true</read-only>
       <overridable>false</overridable>
diff --git a/ambari-server/src/main/resources/stacks/PERF/1.0/properties/stack_features.json b/ambari-server/src/main/resources/stacks/PERF/1.0/properties/stack_features.json
index e9e0ed219c..839e8e6df6 100644
-- a/ambari-server/src/main/resources/stacks/PERF/1.0/properties/stack_features.json
++ b/ambari-server/src/main/resources/stacks/PERF/1.0/properties/stack_features.json
@@ -1,19 +1,21 @@
 {
  "stack_features": [
    {
      "name": "rolling_upgrade",
      "description": "Rolling upgrade support",
      "min_version": "1.0.0.0"
    },
    {
      "name": "secure_zookeeper",
      "description": "Protect ZNodes with SASL acl in secure clusters",
      "min_version": "2.6.0.0"
    },
    {
      "name": "config_versioning",
      "description": "Configurable versions support",
      "min_version": "1.0.0.0"
    }
  ]
}
  "PERF": {
    "stack_features": [
      {
        "name": "rolling_upgrade",
        "description": "Rolling upgrade support",
        "min_version": "1.0.0.0"
      },
      {
        "name": "secure_zookeeper",
        "description": "Protect ZNodes with SASL acl in secure clusters",
        "min_version": "2.6.0.0"
      },
      {
        "name": "config_versioning",
        "description": "Configurable versions support",
        "min_version": "1.0.0.0"
      }
    ]
  }
}
\ No newline at end of file
diff --git a/ambari-server/src/main/resources/stacks/PERF/1.0/properties/stack_tools.json b/ambari-server/src/main/resources/stacks/PERF/1.0/properties/stack_tools.json
index 535b9d9e0d..62562f882c 100644
-- a/ambari-server/src/main/resources/stacks/PERF/1.0/properties/stack_tools.json
++ b/ambari-server/src/main/resources/stacks/PERF/1.0/properties/stack_tools.json
@@ -1,4 +1,14 @@
 {
  "stack_selector": ["distro-select", "/usr/bin/distro-select", "distro-select"],
  "conf_selector": ["conf-select", "/usr/bin/conf-select", "conf-select"]
}
\ No newline at end of file
  "PERF": {
    "stack_selector": [
      "distro-select",
      "/usr/bin/distro-select",
      "distro-select"
    ],
    "conf_selector": [
      "conf-select",
      "/usr/bin/conf-select",
      "conf-select"
    ]
  }
}
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessorTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessorTest.java
index ca579ead2e..bade23810e 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessorTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessorTest.java
@@ -46,9 +46,11 @@ import org.apache.ambari.server.controller.KerberosHelper;
 import org.apache.ambari.server.controller.StackConfigurationResponse;
 import org.apache.ambari.server.state.Cluster;
 import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
 import org.apache.ambari.server.state.PropertyDependencyInfo;
 import org.apache.ambari.server.state.PropertyInfo;
 import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
 import org.apache.ambari.server.state.ValueAttributesInfo;
 import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
 import org.apache.ambari.server.topology.AdvisedConfiguration;
@@ -66,6 +68,7 @@ import org.apache.ambari.server.topology.HostGroupInfo;
 import org.apache.ambari.server.topology.InvalidTopologyException;
 import org.apache.ambari.server.topology.TopologyRequest;
 import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;
 import org.easymock.EasyMockRule;
 import org.easymock.EasyMockSupport;
 import org.easymock.Mock;
@@ -95,6 +98,10 @@ public class BlueprintConfigurationProcessorTest extends EasyMockSupport {
 
   private static final Configuration EMPTY_CONFIG = new Configuration(Collections.<String, Map<String, String>>emptyMap(), Collections.<String, Map<String, Map<String, String>>>emptyMap());
   private final Map<String, Collection<String>> serviceComponents = new HashMap<>();
  private final Map<String, Map<String, String>> stackProperties = new HashMap<>();

  private final String STACK_NAME = "testStack";
  private final String STACK_VERSION = "1";
 
   @Rule
   public EasyMockRule mocks = new EasyMockRule(this);
@@ -129,13 +136,16 @@ public class BlueprintConfigurationProcessorTest extends EasyMockSupport {
   @Mock
   private TopologyRequest topologyRequestMock;
 
  @Mock(type = MockType.NICE)
  private ConfigHelper configHelper;

   @Before
   public void init() throws Exception {
     expect(bp.getStack()).andReturn(stack).anyTimes();
     expect(bp.getName()).andReturn("test-bp").anyTimes();
 
    expect(stack.getName()).andReturn("testStack").anyTimes();
    expect(stack.getVersion()).andReturn("1").anyTimes();
    expect(stack.getName()).andReturn(STACK_NAME).atLeastOnce();
    expect(stack.getVersion()).andReturn(STACK_VERSION).atLeastOnce();
     // return false for all components since for this test we don't care about the value
     expect(stack.isMasterComponent((String) anyObject())).andReturn(false).anyTimes();
     expect(stack.getConfigurationPropertiesWithMetadata(anyObject(String.class), anyObject(String.class))).andReturn(Collections.<String, Stack.ConfigProperty>emptyMap()).anyTimes();
@@ -225,6 +235,11 @@ public class BlueprintConfigurationProcessorTest extends EasyMockSupport {
 
     Set<String> emptySet = Collections.emptySet();
     expect(stack.getExcludedConfigurationTypes(anyObject(String.class))).andReturn(emptySet).anyTimes();

    expect(ambariContext.getConfigHelper()).andReturn(configHelper).anyTimes();
    expect(configHelper.getDefaultStackProperties(
        EasyMock.eq(new StackId(STACK_NAME, STACK_VERSION)))).andReturn(stackProperties).anyTimes();

     expect(ambariContext.isClusterKerberosEnabled(1)).andReturn(true).once();
     expect(ambariContext.getClusterName(1L)).andReturn("clusterName").anyTimes();
     PowerMock.mockStatic(AmbariServer.class);
@@ -234,14 +249,14 @@ public class BlueprintConfigurationProcessorTest extends EasyMockSupport {
     expect(controller.getKerberosHelper()).andReturn(kerberosHelper).anyTimes();
     expect(controller.getClusters()).andReturn(clusters).anyTimes();
     expect(kerberosHelper.getKerberosDescriptor(cluster)).andReturn(kerberosDescriptor).anyTimes();
    Set<String> properties = new HashSet<String>();
    Set<String> properties = new HashSet<>();
     properties.add("core-site/hadoop.security.auth_to_local");
     expect(kerberosDescriptor.getAllAuthToLocalProperties()).andReturn(properties).anyTimes();
   }
 
   @After
   public void tearDown() {
    reset(bp, serviceInfo, stack, ambariContext);
    reset(bp, serviceInfo, stack, ambariContext, configHelper);
   }
 
   @Test
@@ -6322,13 +6337,16 @@ public class BlueprintConfigurationProcessorTest extends EasyMockSupport {
     topology.getAdvisedConfigurations().putAll(createAdvisedConfigMap());
     topology.setConfigRecommendationStrategy(ConfigRecommendationStrategy.ONLY_STACK_DEFAULTS_APPLY);
     BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

     reset(stack);
    expect(stack.getName()).andReturn(STACK_NAME).anyTimes();
    expect(stack.getVersion()).andReturn(STACK_VERSION).anyTimes();
     expect(stack.getConfiguration(bp.getServices())).andReturn(createStackDefaults()).anyTimes();
 
     Set<String> emptySet = Collections.emptySet();
     expect(stack.getExcludedConfigurationTypes(anyObject(String.class))).andReturn(emptySet).anyTimes();

     replay(stack);

     // WHEN
     Set<String> configTypeUpdated = configProcessor.doUpdateForClusterCreate();
     // THEN
@@ -6379,13 +6397,17 @@ public class BlueprintConfigurationProcessorTest extends EasyMockSupport {
     topology.getAdvisedConfigurations().putAll(createAdvisedConfigMap());
     topology.setConfigRecommendationStrategy(ConfigRecommendationStrategy.ONLY_STACK_DEFAULTS_APPLY);
     BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);

     reset(stack);
    expect(stack.getName()).andReturn(STACK_NAME).anyTimes();
    expect(stack.getVersion()).andReturn(STACK_VERSION).anyTimes();
     expect(stack.getConfiguration(bp.getServices())).andReturn(createStackDefaults()).anyTimes();
 
     Set<String> emptySet = Collections.emptySet();
     expect(stack.getExcludedConfigurationTypes(anyObject(String.class))).andReturn(emptySet).anyTimes();
 
     replay(stack);

     // WHEN
     configProcessor.doUpdateForClusterCreate();
     // THEN
@@ -8050,6 +8072,10 @@ public class BlueprintConfigurationProcessorTest extends EasyMockSupport {
   @Test
   public void testValuesTrimming() throws Exception {
     reset(stack);

    expect(stack.getName()).andReturn(STACK_NAME).anyTimes();
    expect(stack.getVersion()).andReturn(STACK_VERSION).anyTimes();

     Map<String, Map<String, String>> properties = new HashMap<>();
 
     Map<String, String> hdfsSite = new HashMap<>();
@@ -8073,6 +8099,7 @@ public class BlueprintConfigurationProcessorTest extends EasyMockSupport {
       new StackConfigurationResponse(null, null, null, null, "hdfs-site", null, Collections.singleton(PropertyInfo.PropertyType.PASSWORD), null, null, null)));
     propertyConfigs.put("test.host", new Stack.ConfigProperty(
       new StackConfigurationResponse(null, null, null, null, "hdfs-site", null, null, null, valueAttributesInfoHost, null)));

     expect(stack.getServiceForConfigType("hdfs-site")).andReturn("HDFS").anyTimes();
     expect(stack.getConfigurationPropertiesWithMetadata("HDFS", "hdfs-site")).andReturn(propertyConfigs).anyTimes();
 
@@ -8144,7 +8171,7 @@ public class BlueprintConfigurationProcessorTest extends EasyMockSupport {
     throws InvalidTopologyException {
 
 
    replay(stack, serviceInfo, ambariContext, controller, kerberosHelper, kerberosDescriptor, clusters, cluster);
    replay(stack, serviceInfo, ambariContext, configHelper, controller, kerberosHelper, kerberosDescriptor, clusters, cluster);
 
     Map<String, HostGroupInfo> hostGroupInfo = new HashMap<>();
     Collection<String> allServices = new HashSet<>();
@@ -8207,7 +8234,7 @@ public class BlueprintConfigurationProcessorTest extends EasyMockSupport {
       this.name = name;
       this.components = components;
       this.hosts = hosts;
      this.configuration = new Configuration(Collections.<String, Map<String, String>>emptyMap(),
      configuration = new Configuration(Collections.<String, Map<String, String>>emptyMap(),
         Collections.<String, Map<String, Map<String, String>>>emptyMap());
     }
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/ClusterStackVersionResourceProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/ClusterStackVersionResourceProviderTest.java
index 32a535840f..39aee82151 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/ClusterStackVersionResourceProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/ClusterStackVersionResourceProviderTest.java
@@ -1414,8 +1414,8 @@ public class ClusterStackVersionResourceProviderTest {
     expect(cluster.getClusterId()).andReturn(1L).anyTimes();
     expect(cluster.getHosts()).andReturn(hostsForCluster.values()).atLeastOnce();
     expect(cluster.getServices()).andReturn(serviceMap).anyTimes();
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andReturn(
        serviceComponentHosts).anyTimes();
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andReturn(serviceComponentHosts).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(stackId).atLeastOnce();
 
     expect(repositoryVersionDAOMock.findByStackAndVersion(anyObject(StackId.class),
         anyObject(String.class))).andReturn(repoVersionEntity);
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterConfigurationRequestTest.java b/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterConfigurationRequestTest.java
index 8b08dc47a7..5535256f13 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterConfigurationRequestTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterConfigurationRequestTest.java
@@ -50,6 +50,8 @@ import org.apache.ambari.server.controller.internal.Stack;
 import org.apache.ambari.server.serveraction.kerberos.KerberosInvalidConfigurationException;
 import org.apache.ambari.server.state.Cluster;
 import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.StackId;
 import org.easymock.Capture;
 import org.easymock.CaptureType;
 import org.easymock.EasyMock;
@@ -103,6 +105,13 @@ public class ClusterConfigurationRequestTest {
   @Mock(type = MockType.NICE)
   private KerberosHelper kerberosHelper;
 
  @Mock(type = MockType.NICE)
  private ConfigHelper configHelper;

  private final String STACK_NAME = "testStack";
  private final String STACK_VERSION = "1";
  private final Map<String, Map<String, String>> stackProperties = new HashMap<>();

   /**
    * testConfigType config type should be in updatedConfigTypes, as no custom property in Blueprint
    * ==> Kerberos config property should be updated
@@ -221,6 +230,8 @@ public class ClusterConfigurationRequestTest {
     expect(clusters.getCluster("testCluster")).andReturn(cluster).anyTimes();
 
     expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(stack.getName()).andReturn(STACK_NAME).anyTimes();
    expect(stack.getVersion()).andReturn(STACK_VERSION).anyTimes();
     expect(stack.getServiceForConfigType("testConfigType")).andReturn("KERBEROS").anyTimes();
     expect(stack.getAllConfigurationTypes(anyString())).andReturn(Collections.singletonList("testConfigType")
     ).anyTimes();
@@ -246,6 +257,7 @@ public class ClusterConfigurationRequestTest {
     expect(blueprint.getComponents("KERBEROS")).andReturn(kerberosComponents).anyTimes();
     expect(blueprint.getComponents("ZOOKEPER")).andReturn(zookeeperComponents).anyTimes();
 
    expect(topology.getAmbariContext()).andReturn(ambariContext).anyTimes();
     expect(topology.getConfigRecommendationStrategy()).andReturn(ConfigRecommendationStrategy.NEVER_APPLY).anyTimes();
     expect(topology.getBlueprint()).andReturn(blueprint).anyTimes();
     expect(blueprint.isValidConfigType("testConfigType")).andReturn(true).anyTimes();
@@ -256,10 +268,14 @@ public class ClusterConfigurationRequestTest {
     expect(topology.getHostGroupsForComponent(anyString())).andReturn(Collections.<String>emptyList())
       .anyTimes();
 
      expect(ambariContext.getClusterName(Long.valueOf(1))).andReturn("testCluster").anyTimes();
    expect(ambariContext.getConfigHelper()).andReturn(configHelper).anyTimes();
    expect(ambariContext.getClusterName(Long.valueOf(1))).andReturn("testCluster").anyTimes();
     expect(ambariContext.createConfigurationRequests(EasyMock.<Map<String, Object>>anyObject())).andReturn(Collections
       .<ConfigurationRequest>emptyList()).anyTimes();
 
    expect(configHelper.getDefaultStackProperties(
        EasyMock.eq(new StackId(STACK_NAME, STACK_VERSION)))).andReturn(stackProperties).anyTimes();

     if (kerberosConfig == null) {
       kerberosConfig = new HashMap<>();
       Map<String, String> properties = new HashMap<>();
@@ -277,15 +293,14 @@ public class ClusterConfigurationRequestTest {
       (captureUpdatedConfigTypes));
     expectLastCall();
 
    PowerMock.replay(stack, blueprint, topology, controller, clusters, kerberosHelper, ambariContext,
      AmbariContext
        .class);
    PowerMock.replay(stack, blueprint, topology, controller, clusters, kerberosHelper,
        ambariContext, AmbariContext.class, configHelper);
 
     ClusterConfigurationRequest clusterConfigurationRequest = new ClusterConfigurationRequest(
       ambariContext, topology, false, stackAdvisorBlueprintProcessor, true);
     clusterConfigurationRequest.process();
 
    verify(blueprint, topology, ambariContext, controller, kerberosHelper);
    verify(blueprint, topology, ambariContext, controller, kerberosHelper, configHelper);
 
 
     String clusterName = captureClusterName.getValue();
@@ -308,8 +323,9 @@ public class ClusterConfigurationRequestTest {
     expect(clusters.getCluster("testCluster")).andReturn(cluster).anyTimes();
 
     expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(stack.getAllConfigurationTypes(anyString())).andReturn(Collections.singletonList("testConfigType")
    ).anyTimes();
    expect(stack.getName()).andReturn(STACK_NAME).anyTimes();
    expect(stack.getVersion()).andReturn(STACK_VERSION).anyTimes();
    expect(stack.getAllConfigurationTypes(anyString())).andReturn(Collections.<String>singletonList("testConfigType")).anyTimes();
     expect(stack.getExcludedConfigurationTypes(anyString())).andReturn(Collections.<String>emptySet()).anyTimes();
     expect(stack.getConfigurationPropertiesWithMetadata(anyString(), anyString())).andReturn(Collections.<String,
       Stack.ConfigProperty>emptyMap()).anyTimes();
@@ -331,25 +347,29 @@ public class ClusterConfigurationRequestTest {
     expect(blueprint.getComponents("KERBEROS")).andReturn(kerberosComponents).anyTimes();
     expect(blueprint.getComponents("ZOOKEPER")).andReturn(zookeeperComponents).anyTimes();
 
    expect(topology.getAmbariContext()).andReturn(ambariContext).anyTimes();
     expect(topology.getConfigRecommendationStrategy()).andReturn(ConfigRecommendationStrategy.NEVER_APPLY).anyTimes();
     expect(topology.getBlueprint()).andReturn(blueprint).anyTimes();
     expect(topology.getConfiguration()).andReturn(stackConfig).anyTimes();
     expect(topology.getHostGroupInfo()).andReturn(Collections.<String, HostGroupInfo>emptyMap()).anyTimes();
     expect(topology.getClusterId()).andReturn(Long.valueOf(1)).anyTimes();

    expect(ambariContext.getConfigHelper()).andReturn(configHelper).anyTimes();
     expect(ambariContext.getClusterName(Long.valueOf(1))).andReturn("testCluster").anyTimes();
     expect(ambariContext.createConfigurationRequests(EasyMock.<Map<String, Object>>anyObject())).andReturn(Collections
       .<ConfigurationRequest>emptyList()).anyTimes();
 
    expect(configHelper.getDefaultStackProperties(
        EasyMock.eq(new StackId(STACK_NAME, STACK_VERSION)))).andReturn(stackProperties).anyTimes();
 
     PowerMock.replay(stack, blueprint, topology, controller, clusters, ambariContext,
      AmbariContext
        .class);
        AmbariContext.class, configHelper);
 
     ClusterConfigurationRequest clusterConfigurationRequest = new ClusterConfigurationRequest(
       ambariContext, topology, false, stackAdvisorBlueprintProcessor);
     clusterConfigurationRequest.process();
 
    verify(blueprint, topology, ambariContext, controller);
    verify(blueprint, topology, ambariContext, controller, configHelper);
 
   }
 
@@ -365,6 +385,7 @@ public class ClusterConfigurationRequestTest {
     hg1.setConfiguration(createConfigurationsForHostGroup());
     hostGroupInfoMap.put("hg1", hg1);
 
    expect(topology.getAmbariContext()).andReturn(ambariContext).anyTimes();
     expect(topology.getConfiguration()).andReturn(configuration).anyTimes();
     expect(topology.getBlueprint()).andReturn(blueprint).anyTimes();
     expect(topology.getHostGroupInfo()).andReturn(hostGroupInfoMap);
@@ -377,7 +398,12 @@ public class ClusterConfigurationRequestTest {
     expect(blueprint.isValidConfigType("cluster-env")).andReturn(true).anyTimes();
     expect(blueprint.isValidConfigType("global")).andReturn(true).anyTimes();
 
    EasyMock.replay(stack, blueprint, topology);
    expect(ambariContext.getConfigHelper()).andReturn(configHelper).anyTimes();

    expect(configHelper.getDefaultStackProperties(
        EasyMock.eq(new StackId(STACK_NAME, STACK_VERSION)))).andReturn(stackProperties).anyTimes();

    EasyMock.replay(stack, blueprint, topology, ambariContext, configHelper);
     // WHEN
     new ClusterConfigurationRequest(ambariContext, topology, false, stackAdvisorBlueprintProcessor);
     // THEN
@@ -388,7 +414,7 @@ public class ClusterConfigurationRequestTest {
 
     assertFalse("SPARK service not present in topology host group config thus 'spark-env' config type should be removed from config.", hg1.getConfiguration().getFullAttributes().containsKey("spark-env"));
     assertTrue("HDFS service is present in topology host group config thus 'hdfs-site' config type should be left in the config.", hg1.getConfiguration().getFullAttributes().containsKey("hdfs-site"));
    verify(stack, blueprint, topology);
    verify(stack, blueprint, topology, ambariContext, configHelper);
   }
 
   @Test
@@ -409,6 +435,7 @@ public class ClusterConfigurationRequestTest {
     hg1.setConfiguration(createConfigurationsForHostGroup());
     hostGroupInfoMap.put("hg1", hg1);
 
    expect(topology.getAmbariContext()).andReturn(ambariContext).anyTimes();
     expect(topology.getConfiguration()).andReturn(configuration).anyTimes();
     expect(topology.getBlueprint()).andReturn(blueprint).anyTimes();
     expect(topology.getHostGroupInfo()).andReturn(hostGroupInfoMap);
@@ -419,7 +446,12 @@ public class ClusterConfigurationRequestTest {
     expect(blueprint.isValidConfigType("cluster-env")).andReturn(true).anyTimes();
     expect(blueprint.isValidConfigType("global")).andReturn(true).anyTimes();
 
    EasyMock.replay(stack, blueprint, topology);
    expect(ambariContext.getConfigHelper()).andReturn(configHelper).anyTimes();

    expect(configHelper.getDefaultStackProperties(
        EasyMock.eq(new StackId(STACK_NAME, STACK_VERSION)))).andReturn(stackProperties).anyTimes();

    EasyMock.replay(stack, blueprint, topology, ambariContext, configHelper);
 
     // When
 
@@ -431,7 +463,7 @@ public class ClusterConfigurationRequestTest {
 
     assertFalse("SPARK service not present in topology host group config thus 'spark-env' config type should be removed from config.", hg1.getConfiguration().getFullAttributes().containsKey("spark-env"));
     assertTrue("HDFS service is present in topology host group config thus 'hdfs-site' config type should be left in the config.", hg1.getConfiguration().getFullAttributes().containsKey("hdfs-site"));
    verify(stack, blueprint, topology);
    verify(stack, blueprint, topology, ambariContext, configHelper);
 
   }
 
diff --git a/ambari-server/src/test/python/common-services/configs/hawq_default.json b/ambari-server/src/test/python/common-services/configs/hawq_default.json
index 79864a9525..1b6fafb2c6 100644
-- a/ambari-server/src/test/python/common-services/configs/hawq_default.json
++ b/ambari-server/src/test/python/common-services/configs/hawq_default.json
@@ -73,7 +73,11 @@
         "cluster-env": {
             "managed_hdfs_resource_property_names": "",
             "security_enabled": "false",
            "user_group": "hadoop"
            "user_group": "hadoop",
            "stack_name": "PHD",
            "stack_root": "{\"PHD\": \"/usr/phd\"}",
            "stack_tools": "{\n \"PHD\": { \"stack_selector\": [\"phd-select\", \"/usr/bin/phd-select\", \"phd-select\"],\n  \"conf_selector\": [\"conf-select\", \"/usr/bin/conf-select\", \"conf-select\"]\n}\n}",
            "stack_features": "{\"PHD\":{\"stack_features\":[{\"name\":\"express_upgrade\",\"description\":\"Express upgrade support\",\"min_version\":\"3.0.0.0\"},{\"name\":\"rolling_upgrade\",\"description\":\"Rolling upgrade support\",\"min_version\":\"3.0.0.0\"},{\"name\":\"config_versioning\",\"description\":\"Configurable versions support\",\"min_version\":\"3.0.0.0\"}]\n}\n}"
         }
     },
     "clusterHostInfo": {
diff --git a/ambari-server/src/test/python/host_scripts/TestAlertDiskSpace.py b/ambari-server/src/test/python/host_scripts/TestAlertDiskSpace.py
index 0d47061945..e6cce98c91 100644
-- a/ambari-server/src/test/python/host_scripts/TestAlertDiskSpace.py
++ b/ambari-server/src/test/python/host_scripts/TestAlertDiskSpace.py
@@ -41,7 +41,11 @@ class TestAlertDiskSpace(RMFTestCase):
       total = 21673930752L, used = 5695861760L,
       free = 15978068992L, path="/")
 
    res = alert_disk_space.execute(configurations={'{{cluster-env/stack_root}}': '/usr/hdp'})
    configurations = {'{{cluster-env/stack_name}}': 'HDP',
      '{{cluster-env/stack_root}}': '{"HDP":"/usr/hdp"}'}

    res = alert_disk_space.execute(configurations=configurations)

     self.assertEqual(res,
       ('OK', ['Capacity Used: [26.28%, 5.7 GB], Capacity Total: [21.7 GB], path=/']))
 
@@ -50,7 +54,7 @@ class TestAlertDiskSpace(RMFTestCase):
       total = 21673930752L, used = 14521533603L,
       free = 7152397149L, path="/")
 
    res = alert_disk_space.execute(configurations={'{{cluster-env/stack_root}}': '/usr/hdp'})
    res = alert_disk_space.execute(configurations = configurations)
     self.assertEqual(res, (
       'WARNING',
       ['Capacity Used: [67.00%, 14.5 GB], Capacity Total: [21.7 GB], path=/']))
@@ -60,7 +64,7 @@ class TestAlertDiskSpace(RMFTestCase):
       total = 21673930752L, used = 20590234214L,
       free = 1083696538, path="/")
 
    res = alert_disk_space.execute(configurations={'{{cluster-env/stack_root}}': '/usr/hdp'})
    res = alert_disk_space.execute(configurations = configurations)
     self.assertEqual(res, ('CRITICAL',
     ['Capacity Used: [95.00%, 20.6 GB], Capacity Total: [21.7 GB], path=/']))
 
@@ -69,7 +73,7 @@ class TestAlertDiskSpace(RMFTestCase):
       total = 5418482688L, used = 1625544806L,
       free = 3792937882L, path="/")
 
    res = alert_disk_space.execute(configurations={'{{cluster-env/stack_root}}': '/usr/hdp'})
    res = alert_disk_space.execute(configurations = configurations)
     self.assertEqual(res, ('WARNING', [
       'Capacity Used: [30.00%, 1.6 GB], Capacity Total: [5.4 GB], path=/. Total free space is less than 5.0 GB']))
 
@@ -81,7 +85,7 @@ class TestAlertDiskSpace(RMFTestCase):
       total = 21673930752L, used = 5695861760L,
       free = 15978068992L, path="/usr/hdp")
 
    res = alert_disk_space.execute(configurations={'{{cluster-env/stack_root}}': '/usr/hdp'})
    res = alert_disk_space.execute(configurations = configurations)
     self.assertEqual(res,
       ('OK', ['Capacity Used: [26.28%, 5.7 GB], Capacity Total: [21.7 GB], path=/usr/hdp']))
 
@@ -90,6 +94,6 @@ class TestAlertDiskSpace(RMFTestCase):
       total = 5418482688L, used = 1625544806L,
       free = 3792937882L, path="/usr/hdp")
 
    res = alert_disk_space.execute(configurations={'{{cluster-env/stack_root}}': '/usr/hdp'})
    res = alert_disk_space.execute(configurations = configurations)
     self.assertEqual(res, (
       'WARNING', ["Capacity Used: [30.00%, 1.6 GB], Capacity Total: [5.4 GB], path=/usr/hdp. Total free space is less than 5.0 GB"]))
diff --git a/ambari-server/src/test/python/stacks/2.5/configs/ranger-admin-default.json b/ambari-server/src/test/python/stacks/2.5/configs/ranger-admin-default.json
index a1d930c5c1..fb77531c6c 100644
-- a/ambari-server/src/test/python/stacks/2.5/configs/ranger-admin-default.json
++ b/ambari-server/src/test/python/stacks/2.5/configs/ranger-admin-default.json
@@ -1,150 +1,150 @@
 {
     "localComponents": [
        "NAMENODE", 
        "SECONDARY_NAMENODE", 
        "ZOOKEEPER_SERVER", 
        "DATANODE", 
        "HDFS_CLIENT", 
        "ZOOKEEPER_CLIENT", 
        "RANGER_USERSYNC", 
        "RANGER_ADMIN", 
        "NAMENODE",
        "SECONDARY_NAMENODE",
        "ZOOKEEPER_SERVER",
        "DATANODE",
        "HDFS_CLIENT",
        "ZOOKEEPER_CLIENT",
        "RANGER_USERSYNC",
        "RANGER_ADMIN",
         "RANGER_TAGSYNC",
         "LOGSEARCH_SOLR",
         "LOGSEARCH_SOLR_CLIENT"
    ], 
    ],
     "configuration_attributes": {
        "ranger-hdfs-audit": {}, 
        "ssl-client": {}, 
        "ranger-admin-site": {}, 
        "ranger-hdfs-policymgr-ssl": {}, 
        "tagsync-application-properties": {}, 
        "ranger-env": {}, 
        "usersync-log4j": {}, 
        "admin-properties": {}, 
        "ranger-ugsync-site": {}, 
        "ranger-hdfs-audit": {},
        "ssl-client": {},
        "ranger-admin-site": {},
        "ranger-hdfs-policymgr-ssl": {},
        "tagsync-application-properties": {},
        "ranger-env": {},
        "usersync-log4j": {},
        "admin-properties": {},
        "ranger-ugsync-site": {},
         "hdfs-site": {
             "final": {
                "dfs.datanode.data.dir": "true", 
                "dfs.namenode.http-address": "true", 
                "dfs.datanode.failed.volumes.tolerated": "true", 
                "dfs.support.append": "true", 
                "dfs.namenode.name.dir": "true", 
                "dfs.datanode.data.dir": "true",
                "dfs.namenode.http-address": "true",
                "dfs.datanode.failed.volumes.tolerated": "true",
                "dfs.support.append": "true",
                "dfs.namenode.name.dir": "true",
                 "dfs.webhdfs.enabled": "true"
             }
        }, 
        "ranger-tagsync-site": {}, 
        "zoo.cfg": {}, 
        "hadoop-policy": {}, 
        "hdfs-log4j": {}, 
        "ranger-hdfs-plugin-properties": {}, 
        },
        "ranger-tagsync-site": {},
        "zoo.cfg": {},
        "hadoop-policy": {},
        "hdfs-log4j": {},
        "ranger-hdfs-plugin-properties": {},
         "core-site": {
             "final": {
                 "fs.defaultFS": "true"
             }
        }, 
        "hadoop-env": {}, 
        "zookeeper-log4j": {}, 
        "ssl-server": {}, 
        "ranger-site": {}, 
        "admin-log4j": {}, 
        "tagsync-log4j": {}, 
        "ranger-hdfs-security": {}, 
        "usersync-properties": {}, 
        },
        "hadoop-env": {},
        "zookeeper-log4j": {},
        "ssl-server": {},
        "ranger-site": {},
        "admin-log4j": {},
        "tagsync-log4j": {},
        "ranger-hdfs-security": {},
        "usersync-properties": {},
         "zookeeper-env": {},
         "infra-solr-env": {},
         "infra-solr-client-log4j": {},
         "cluster-env": {}
    }, 
    "public_hostname": "c6401.ambari.apache.org", 
    "commandId": "11-0", 
    "hostname": "c6401.ambari.apache.org", 
    "kerberosCommandParams": [], 
    "serviceName": "RANGER", 
    "role": "RANGER_ADMIN", 
    "forceRefreshConfigTagsBeforeExecution": [], 
    "requestId": 11, 
    },
    "public_hostname": "c6401.ambari.apache.org",
    "commandId": "11-0",
    "hostname": "c6401.ambari.apache.org",
    "kerberosCommandParams": [],
    "serviceName": "RANGER",
    "role": "RANGER_ADMIN",
    "forceRefreshConfigTagsBeforeExecution": [],
    "requestId": 11,
     "agentConfigParams": {
         "agent": {
             "parallel_execution": 0
         }
    }, 
    "clusterName": "c1", 
    "commandType": "EXECUTION_COMMAND", 
    "taskId": 31, 
    "roleParams": {}, 
    },
    "clusterName": "c1",
    "commandType": "EXECUTION_COMMAND",
    "taskId": 31,
    "roleParams": {},
     "configurationTags": {
         "ranger-hdfs-audit": {
             "tag": "version1466705299922"
        }, 
        },
         "ssl-client": {
             "tag": "version1"
        }, 
        },
         "ranger-admin-site": {
             "tag": "version1466705299949"
        }, 
        },
         "ranger-hdfs-policymgr-ssl": {
             "tag": "version1466705299922"
        }, 
        },
         "tagsync-application-properties": {
             "tag": "version1466705299949"
        }, 
        },
         "ranger-env": {
             "tag": "version1466705299949"
        }, 
        },
         "usersync-log4j": {
             "tag": "version1466705299949"
        }, 
        },
         "admin-properties": {
             "tag": "version1466705299949"
        }, 
        },
         "ranger-ugsync-site": {
             "tag": "version1466705299949"
        }, 
        },
         "hdfs-site": {
             "tag": "version1"
        }, 
        },
         "ranger-tagsync-site": {
             "tag": "version1466705299949"
        }, 
        },
         "zoo.cfg": {
             "tag": "version1"
        }, 
        },
         "hadoop-policy": {
             "tag": "version1"
        }, 
        },
         "hdfs-log4j": {
             "tag": "version1"
        }, 
        },
         "ranger-hdfs-plugin-properties": {
             "tag": "version1466705299922"
        }, 
        },
         "core-site": {
             "tag": "version1"
        }, 
        },
         "hadoop-env": {
             "tag": "version1"
        }, 
        },
         "zookeeper-log4j": {
             "tag": "version1"
        }, 
        },
         "ssl-server": {
             "tag": "version1"
        }, 
        },
         "ranger-site": {
             "tag": "version1466705299949"
        }, 
        },
         "admin-log4j": {
             "tag": "version1466705299949"
        }, 
        },
         "tagsync-log4j": {
             "tag": "version1466705299949"
        }, 
        },
         "ranger-hdfs-security": {
             "tag": "version1466705299922"
        }, 
        },
         "usersync-properties": {
             "tag": "version1466705299949"
        }, 
        },
         "zookeeper-env": {
             "tag": "version1"
         },
@@ -157,492 +157,492 @@
         "cluster-env": {
             "tag": "version1"
         }
    }, 
    "roleCommand": "START", 
    },
    "roleCommand": "START",
     "hostLevelParams": {
        "agent_stack_retry_on_unavailability": "false", 
        "stack_name": "HDP", 
        "package_version": "2_5_0_0_*", 
        "agent_stack_retry_on_unavailability": "false",
        "stack_name": "HDP",
        "package_version": "2_5_0_0_*",
         "custom_mysql_jdbc_name": "mysql-connector-java.jar",
         "previous_custom_mysql_jdbc_name": "mysql-connector-java-old.jar",
        "host_sys_prepped": "false", 
        "ambari_db_rca_username": "mapred", 
        "current_version": "2.5.0.0-801", 
        "mysql_jdbc_url": "http://c6401.ambari.apache.org:8080/resources//mysql-connector-java.jar", 
        "agent_stack_retry_count": "5", 
        "stack_version": "2.5", 
        "jdk_name": "jdk-8u60-linux-x64.tar.gz", 
        "ambari_db_rca_driver": "org.postgresql.Driver", 
        "java_home": "/usr/jdk64/jdk1.7.0_45", 
        "repository_version_id": "1", 
        "jdk_location": "http://c6401.ambari.apache.org:8080/resources/", 
        "not_managed_hdfs_path_list": "[\"/tmp\"]", 
        "ambari_db_rca_url": "jdbc:postgresql://c6401.ambari.apache.org/ambarirca", 
        "java_version": "8", 
        "repo_info": "[{\"baseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.5.0.0-801\",\"osType\":\"redhat6\",\"repoId\":\"HDP-2.5\",\"repoName\":\"HDP\",\"defaultBaseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/updates/2.5.0.0\",\"latestBaseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.5.0.0-801\",\"baseSaved\":true},{\"baseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"osType\":\"redhat6\",\"repoId\":\"HDP-UTILS-1.1.0.21\",\"repoName\":\"HDP-UTILS\",\"defaultBaseUrl\":\"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"latestBaseUrl\":\"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"baseSaved\":true}]", 
        "package_list": "[{\"name\":\"ranger_${stack_version}-admin\",\"condition\":\"\",\"skipUpgrade\":false},{\"name\":\"ranger_${stack_version}-usersync\",\"condition\":\"\",\"skipUpgrade\":false},{\"name\":\"ranger_${stack_version}-tagsync\",\"condition\":\"should_install_ranger_tagsync\",\"skipUpgrade\":false},{\"name\":\"ambari-logsearch-solr-client\",\"condition\":\"should_install_logsearch_solr_client\",\"skipUpgrade\":false}]", 
        "db_name": "ambari", 
        "group_list": "[\"ranger\",\"hadoop\",\"users\"]", 
        "agentCacheDir": "/var/lib/ambari-agent/cache", 
        "ambari_db_rca_password": "mapred", 
        "jce_name": "jce_policy-8.zip", 
        "oracle_jdbc_url": "http://c6401.ambari.apache.org:8080/resources//ojdbc6.jar", 
        "db_driver_filename": "mysql-connector-java.jar", 
        "user_list": "[\"zookeeper\",\"ambari-qa\",\"ranger\",\"hdfs\"]", 
        "host_sys_prepped": "false",
        "ambari_db_rca_username": "mapred",
        "current_version": "2.5.0.0-801",
        "mysql_jdbc_url": "http://c6401.ambari.apache.org:8080/resources//mysql-connector-java.jar",
        "agent_stack_retry_count": "5",
        "stack_version": "2.5",
        "jdk_name": "jdk-8u60-linux-x64.tar.gz",
        "ambari_db_rca_driver": "org.postgresql.Driver",
        "java_home": "/usr/jdk64/jdk1.7.0_45",
        "repository_version_id": "1",
        "jdk_location": "http://c6401.ambari.apache.org:8080/resources/",
        "not_managed_hdfs_path_list": "[\"/tmp\"]",
        "ambari_db_rca_url": "jdbc:postgresql://c6401.ambari.apache.org/ambarirca",
        "java_version": "8",
        "repo_info": "[{\"baseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.5.0.0-801\",\"osType\":\"redhat6\",\"repoId\":\"HDP-2.5\",\"repoName\":\"HDP\",\"defaultBaseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/updates/2.5.0.0\",\"latestBaseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.5.0.0-801\",\"baseSaved\":true},{\"baseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"osType\":\"redhat6\",\"repoId\":\"HDP-UTILS-1.1.0.21\",\"repoName\":\"HDP-UTILS\",\"defaultBaseUrl\":\"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"latestBaseUrl\":\"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"baseSaved\":true}]",
        "package_list": "[{\"name\":\"ranger_${stack_version}-admin\",\"condition\":\"\",\"skipUpgrade\":false},{\"name\":\"ranger_${stack_version}-usersync\",\"condition\":\"\",\"skipUpgrade\":false},{\"name\":\"ranger_${stack_version}-tagsync\",\"condition\":\"should_install_ranger_tagsync\",\"skipUpgrade\":false},{\"name\":\"ambari-logsearch-solr-client\",\"condition\":\"should_install_logsearch_solr_client\",\"skipUpgrade\":false}]",
        "db_name": "ambari",
        "group_list": "[\"ranger\",\"hadoop\",\"users\"]",
        "agentCacheDir": "/var/lib/ambari-agent/cache",
        "ambari_db_rca_password": "mapred",
        "jce_name": "jce_policy-8.zip",
        "oracle_jdbc_url": "http://c6401.ambari.apache.org:8080/resources//ojdbc6.jar",
        "db_driver_filename": "mysql-connector-java.jar",
        "user_list": "[\"zookeeper\",\"ambari-qa\",\"ranger\",\"hdfs\"]",
         "clientsToUpdateConfigs": "[\"*\"]"
    }, 
    },
     "commandParams": {
        "service_package_folder": "common-services/RANGER/0.4.0/package", 
        "script": "scripts/ranger_admin.py", 
        "hooks_folder": "HDP/2.0.6/hooks", 
        "version": "2.5.0.0-801", 
        "max_duration_for_retries": "0", 
        "command_retry_enabled": "false", 
        "command_timeout": "600", 
        "service_package_folder": "common-services/RANGER/0.4.0/package",
        "script": "scripts/ranger_admin.py",
        "hooks_folder": "HDP/2.0.6/hooks",
        "version": "2.5.0.0-801",
        "max_duration_for_retries": "0",
        "command_retry_enabled": "false",
        "command_timeout": "600",
         "script_type": "PYTHON"
    }, 
    "forceRefreshConfigTags": [], 
    "stageId": 0, 
    },
    "forceRefreshConfigTags": [],
    "stageId": 0,
     "clusterHostInfo": {
         "snamenode_host": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "ambari_server_use_ssl": [
             "false"
        ], 
        ],
         "all_ping_ports": [
             "8670"
        ], 
        ],
         "ranger_tagsync_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "ranger_usersync_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "all_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "slave_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "namenode_host": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "ambari_server_port": [
             "8080"
        ], 
        ],
         "ranger_admin_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "all_racks": [
             "/default-rack"
        ], 
        ],
         "all_ipv4_ips": [
             "172.22.83.73"
        ], 
        ],
         "ambari_server_host": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "zookeeper_hosts": [
             "c6401.ambari.apache.org"
         ],
         "infra_solr_hosts": [
             "c6401.ambari.apache.org"
         ]
    }, 
    },
     "configurations": {
         "ranger-hdfs-audit": {
            "xasecure.audit.destination.solr.zookeepers": "NONE", 
            "xasecure.audit.destination.solr.urls": "", 
            "xasecure.audit.destination.solr.batch.filespool.dir": "/var/log/hadoop/hdfs/audit/solr/spool", 
            "xasecure.audit.destination.hdfs.batch.filespool.dir": "/var/log/hadoop/hdfs/audit/hdfs/spool", 
            "xasecure.audit.destination.solr.zookeepers": "NONE",
            "xasecure.audit.destination.solr.urls": "",
            "xasecure.audit.destination.solr.batch.filespool.dir": "/var/log/hadoop/hdfs/audit/solr/spool",
            "xasecure.audit.destination.hdfs.batch.filespool.dir": "/var/log/hadoop/hdfs/audit/hdfs/spool",
             "xasecure.audit.destination.hdfs": "true",
            "xasecure.audit.destination.solr": "false", 
            "xasecure.audit.destination.solr": "false",
             "xasecure.audit.provider.summary.enabled": "false",
             "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit",
             "xasecure.audit.is.enabled": "true"
        }, 
        },
         "ssl-client": {
            "ssl.client.truststore.reload.interval": "10000", 
            "ssl.client.keystore.password": "bigdata", 
            "ssl.client.truststore.type": "jks", 
            "ssl.client.keystore.location": "/etc/security/clientKeys/keystore.jks", 
            "ssl.client.truststore.location": "/etc/security/clientKeys/all.jks", 
            "ssl.client.truststore.password": "bigdata", 
            "ssl.client.truststore.reload.interval": "10000",
            "ssl.client.keystore.password": "bigdata",
            "ssl.client.truststore.type": "jks",
            "ssl.client.keystore.location": "/etc/security/clientKeys/keystore.jks",
            "ssl.client.truststore.location": "/etc/security/clientKeys/all.jks",
            "ssl.client.truststore.password": "bigdata",
             "ssl.client.keystore.type": "jks"
        }, 
        },
         "ranger-admin-site": {
             "ranger.admin.kerberos.cookie.domain": "",
            "ranger.kms.service.user.hdfs": "hdfs", 
            "ranger.spnego.kerberos.principal": "", 
            "ranger.ldap.ad.url": "{{ranger_ug_ldap_url}}", 
            "ranger.plugins.hive.serviceuser": "hive", 
            "ranger.lookup.kerberos.keytab": "", 
            "ranger.plugins.kms.serviceuser": "kms", 
            "ranger.service.https.attrib.ssl.enabled": "false", 
            "ranger.sso.browser.useragent": "Mozilla,chrome", 
            "ranger.jpa.jdbc.url": "jdbc:mysql://c6401.ambari.apache.org:3306/ranger01", 
            "ranger.plugins.hbase.serviceuser": "hbase", 
            "ranger.plugins.hdfs.serviceuser": "hdfs", 
            "ranger.ldap.group.searchbase": "{{ranger_ug_ldap_group_searchbase}}", 
            "ranger.ldap.user.dnpattern": "uid={0},ou=users,dc=xasecure,dc=net", 
            "ranger.plugins.knox.serviceuser": "knox", 
            "ranger.ldap.base.dn": "dc=example,dc=com", 
            "ranger.sso.publicKey": "", 
            "ranger.admin.kerberos.cookie.path": "/", 
            "ranger.service.https.attrib.clientAuth": "want", 
            "ranger.jpa.jdbc.user": "{{ranger_db_user}}", 
            "ranger.ldap.ad.user.searchfilter": "(sAMAccountName={0})", 
            "ranger.ldap.group.roleattribute": "cn", 
            "ranger.plugins.kafka.serviceuser": "kafka", 
            "ranger.admin.kerberos.principal": "", 
            "ranger.ldap.ad.bind.dn": "{{ranger_ug_ldap_bind_dn}}", 
            "ranger.credential.provider.path": "/etc/ranger/admin/rangeradmin.jceks", 
            "ranger.ldap.referral": "ignore", 
            "ranger.service.http.port": "6080", 
            "ranger.ldap.user.searchfilter": "(uid={0})", 
            "ranger.plugins.atlas.serviceuser": "atlas", 
            "ranger.truststore.password": "changeit", 
            "ranger.ldap.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}", 
            "ranger.audit.solr.password": "NONE", 
            "ranger.kms.service.user.hdfs": "hdfs",
            "ranger.spnego.kerberos.principal": "",
            "ranger.ldap.ad.url": "{{ranger_ug_ldap_url}}",
            "ranger.plugins.hive.serviceuser": "hive",
            "ranger.lookup.kerberos.keytab": "",
            "ranger.plugins.kms.serviceuser": "kms",
            "ranger.service.https.attrib.ssl.enabled": "false",
            "ranger.sso.browser.useragent": "Mozilla,chrome",
            "ranger.jpa.jdbc.url": "jdbc:mysql://c6401.ambari.apache.org:3306/ranger01",
            "ranger.plugins.hbase.serviceuser": "hbase",
            "ranger.plugins.hdfs.serviceuser": "hdfs",
            "ranger.ldap.group.searchbase": "{{ranger_ug_ldap_group_searchbase}}",
            "ranger.ldap.user.dnpattern": "uid={0},ou=users,dc=xasecure,dc=net",
            "ranger.plugins.knox.serviceuser": "knox",
            "ranger.ldap.base.dn": "dc=example,dc=com",
            "ranger.sso.publicKey": "",
            "ranger.admin.kerberos.cookie.path": "/",
            "ranger.service.https.attrib.clientAuth": "want",
            "ranger.jpa.jdbc.user": "{{ranger_db_user}}",
            "ranger.ldap.ad.user.searchfilter": "(sAMAccountName={0})",
            "ranger.ldap.group.roleattribute": "cn",
            "ranger.plugins.kafka.serviceuser": "kafka",
            "ranger.admin.kerberos.principal": "",
            "ranger.ldap.ad.bind.dn": "{{ranger_ug_ldap_bind_dn}}",
            "ranger.credential.provider.path": "/etc/ranger/admin/rangeradmin.jceks",
            "ranger.ldap.referral": "ignore",
            "ranger.service.http.port": "6080",
            "ranger.ldap.user.searchfilter": "(uid={0})",
            "ranger.plugins.atlas.serviceuser": "atlas",
            "ranger.truststore.password": "changeit",
            "ranger.ldap.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}",
            "ranger.audit.solr.password": "NONE",
             "ranger.audit.solr.zookeepers": "c6401.ambari.apache.org:2181/ambari-solr",
            "ranger.lookup.kerberos.principal": "", 
            "ranger.service.https.port": "6182", 
            "ranger.plugins.storm.serviceuser": "storm", 
            "ranger.externalurl": "{{ranger_external_url}}", 
            "ranger.truststore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks", 
            "ranger.kms.service.user.hive": "", 
            "ranger.https.attrib.keystore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks", 
            "ranger.jpa.jdbc.dialect": "{{jdbc_dialect}}", 
            "ranger.service.host": "{{ranger_host}}", 
            "ranger.service.https.attrib.keystore.keyalias": "rangeradmin", 
            "ranger.service.https.attrib.keystore.pass": "xasecure", 
            "ranger.unixauth.remote.login.enabled": "true", 
            "ranger.jpa.jdbc.credential.alias": "rangeradmin", 
            "ranger.ldap.ad.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}", 
            "ranger.audit.solr.username": "ranger_solr", 
            "ranger.sso.enabled": "false", 
            "ranger.audit.solr.urls": "", 
            "ranger.ldap.ad.domain": "", 
            "ranger.plugins.yarn.serviceuser": "yarn", 
            "ranger.audit.source.type": "solr", 
            "ranger.ldap.bind.dn": "{{ranger_ug_ldap_bind_dn}}", 
            "ranger.ldap.url": "{{ranger_ug_ldap_url}}", 
            "ranger.authentication.method": "UNIX", 
            "ranger.service.http.enabled": "true", 
            "ranger.ldap.group.searchfilter": "{{ranger_ug_ldap_group_searchfilter}}", 
            "ranger.ldap.ad.referral": "ignore", 
            "ranger.ldap.ad.base.dn": "dc=example,dc=com", 
            "ranger.jpa.jdbc.password": "_", 
            "ranger.spnego.kerberos.keytab": "", 
            "ranger.sso.providerurl": "", 
            "ranger.unixauth.service.hostname": "{{ugsync_host}}", 
            "ranger.admin.kerberos.keytab": "", 
            "ranger.admin.kerberos.token.valid.seconds": "30", 
            "ranger.jpa.jdbc.driver": "com.mysql.jdbc.Driver", 
            "ranger.lookup.kerberos.principal": "",
            "ranger.service.https.port": "6182",
            "ranger.plugins.storm.serviceuser": "storm",
            "ranger.externalurl": "{{ranger_external_url}}",
            "ranger.truststore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks",
            "ranger.kms.service.user.hive": "",
            "ranger.https.attrib.keystore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks",
            "ranger.jpa.jdbc.dialect": "{{jdbc_dialect}}",
            "ranger.service.host": "{{ranger_host}}",
            "ranger.service.https.attrib.keystore.keyalias": "rangeradmin",
            "ranger.service.https.attrib.keystore.pass": "xasecure",
            "ranger.unixauth.remote.login.enabled": "true",
            "ranger.jpa.jdbc.credential.alias": "rangeradmin",
            "ranger.ldap.ad.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}",
            "ranger.audit.solr.username": "ranger_solr",
            "ranger.sso.enabled": "false",
            "ranger.audit.solr.urls": "",
            "ranger.ldap.ad.domain": "",
            "ranger.plugins.yarn.serviceuser": "yarn",
            "ranger.audit.source.type": "solr",
            "ranger.ldap.bind.dn": "{{ranger_ug_ldap_bind_dn}}",
            "ranger.ldap.url": "{{ranger_ug_ldap_url}}",
            "ranger.authentication.method": "UNIX",
            "ranger.service.http.enabled": "true",
            "ranger.ldap.group.searchfilter": "{{ranger_ug_ldap_group_searchfilter}}",
            "ranger.ldap.ad.referral": "ignore",
            "ranger.ldap.ad.base.dn": "dc=example,dc=com",
            "ranger.jpa.jdbc.password": "_",
            "ranger.spnego.kerberos.keytab": "",
            "ranger.sso.providerurl": "",
            "ranger.unixauth.service.hostname": "{{ugsync_host}}",
            "ranger.admin.kerberos.keytab": "",
            "ranger.admin.kerberos.token.valid.seconds": "30",
            "ranger.jpa.jdbc.driver": "com.mysql.jdbc.Driver",
             "ranger.unixauth.service.port": "5151"
        }, 
        },
         "ranger-hdfs-policymgr-ssl": {
            "xasecure.policymgr.clientssl.keystore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-keystore.jks", 
            "xasecure.policymgr.clientssl.truststore.password": "changeit", 
            "xasecure.policymgr.clientssl.keystore.credential.file": "jceks://file{{credential_file}}", 
            "xasecure.policymgr.clientssl.truststore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-truststore.jks", 
            "xasecure.policymgr.clientssl.truststore.credential.file": "jceks://file{{credential_file}}", 
            "xasecure.policymgr.clientssl.keystore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-keystore.jks",
            "xasecure.policymgr.clientssl.truststore.password": "changeit",
            "xasecure.policymgr.clientssl.keystore.credential.file": "jceks://file{{credential_file}}",
            "xasecure.policymgr.clientssl.truststore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-truststore.jks",
            "xasecure.policymgr.clientssl.truststore.credential.file": "jceks://file{{credential_file}}",
             "xasecure.policymgr.clientssl.keystore.password": "myKeyFilePassword"
        }, 
        },
         "tagsync-application-properties": {
            "atlas.kafka.entities.group.id": "ranger_entities_consumer", 
            "atlas.kafka.zookeeper.connect": "c6401.ambari.apache.org:2181", 
            "atlas.kafka.entities.group.id": "ranger_entities_consumer",
            "atlas.kafka.zookeeper.connect": "c6401.ambari.apache.org:2181",
             "atlas.kafka.bootstrap.servers": "localhost:6667"
        }, 
        },
         "ranger-env": {
            "ranger_solr_shards": "1", 
            "ranger_solr_config_set": "ranger_audits", 
            "ranger_user": "ranger", 
            "ranger_solr_shards": "1",
            "ranger_solr_config_set": "ranger_audits",
            "ranger_user": "ranger",
             "ranger_solr_replication_factor": "1",
            "xml_configurations_supported": "true", 
            "ranger-atlas-plugin-enabled": "No", 
            "ranger-hbase-plugin-enabled": "No", 
            "ranger-yarn-plugin-enabled": "No", 
            "bind_anonymous": "false", 
            "ranger_admin_username": "amb_ranger_admin", 
            "admin_password": "admin", 
            "is_solrCloud_enabled": "true", 
            "ranger-storm-plugin-enabled": "No", 
            "ranger-hdfs-plugin-enabled": "No", 
            "ranger_group": "ranger", 
            "ranger-knox-plugin-enabled": "No", 
            "ranger_admin_log_dir": "/var/log/ranger/admin", 
            "ranger-kafka-plugin-enabled": "No", 
            "ranger_privelege_user_jdbc_url": "jdbc:mysql://c6401.ambari.apache.org:3306", 
            "ranger-hive-plugin-enabled": "No", 
            "xasecure.audit.destination.solr": "true", 
            "ranger_pid_dir": "/var/run/ranger", 
            "xasecure.audit.destination.hdfs": "true", 
            "admin_username": "admin", 
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit", 
            "create_db_dbuser": "true", 
            "ranger_solr_collection_name": "ranger_audits", 
            "ranger_admin_password": "P1!q9xa96SMi5NCl", 
            "xml_configurations_supported": "true",
            "ranger-atlas-plugin-enabled": "No",
            "ranger-hbase-plugin-enabled": "No",
            "ranger-yarn-plugin-enabled": "No",
            "bind_anonymous": "false",
            "ranger_admin_username": "amb_ranger_admin",
            "admin_password": "admin",
            "is_solrCloud_enabled": "true",
            "ranger-storm-plugin-enabled": "No",
            "ranger-hdfs-plugin-enabled": "No",
            "ranger_group": "ranger",
            "ranger-knox-plugin-enabled": "No",
            "ranger_admin_log_dir": "/var/log/ranger/admin",
            "ranger-kafka-plugin-enabled": "No",
            "ranger_privelege_user_jdbc_url": "jdbc:mysql://c6401.ambari.apache.org:3306",
            "ranger-hive-plugin-enabled": "No",
            "xasecure.audit.destination.solr": "true",
            "ranger_pid_dir": "/var/run/ranger",
            "xasecure.audit.destination.hdfs": "true",
            "admin_username": "admin",
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit",
            "create_db_dbuser": "true",
            "ranger_solr_collection_name": "ranger_audits",
            "ranger_admin_password": "P1!q9xa96SMi5NCl",
             "ranger_usersync_log_dir": "/var/log/ranger/usersync"
        }, 
        },
         "usersync-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#      http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n#\n\nlog4j.rootLogger = info,logFile\n\n# logFile\nlog4j.appender.logFile=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.logFile.file=${logdir}/usersync.log\nlog4j.appender.logFile.datePattern='.'yyyy-MM-dd\nlog4j.appender.logFile.layout=org.apache.log4j.PatternLayout\nlog4j.appender.logFile.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %m%n\n\n# console\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.Target=System.out\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %m%n"
        }, 
        },
         "admin-properties": {
            "db_user": "rangeradmin01", 
            "DB_FLAVOR": "MYSQL", 
            "db_password": "rangeradmin01", 
            "db_root_user": "root", 
            "policymgr_external_url": "http://c6401.ambari.apache.org:6080", 
            "db_name": "ranger01", 
            "db_host": "c6401.ambari.apache.org", 
            "db_root_password": "vagrant", 
            "db_user": "rangeradmin01",
            "DB_FLAVOR": "MYSQL",
            "db_password": "rangeradmin01",
            "db_root_user": "root",
            "policymgr_external_url": "http://c6401.ambari.apache.org:6080",
            "db_name": "ranger01",
            "db_host": "c6401.ambari.apache.org",
            "db_root_password": "vagrant",
             "SQL_CONNECTOR_JAR": "{{driver_curl_target}}"
        }, 
        },
         "ranger-ugsync-site": {
            "ranger.usersync.ldap.binddn": "", 
            "ranger.usersync.policymgr.username": "rangerusersync", 
            "ranger.usersync.policymanager.mockrun": "false", 
            "ranger.usersync.group.searchbase": "", 
            "ranger.usersync.ldap.bindalias": "testldapalias", 
            "ranger.usersync.truststore.file": "/usr/hdp/current/ranger-usersync/conf/mytruststore.jks", 
            "ranger.usersync.port": "5151", 
            "ranger.usersync.pagedresultssize": "500", 
            "ranger.usersync.group.memberattributename": "", 
            "ranger.usersync.kerberos.principal": "", 
            "ranger.usersync.source.impl.class": "org.apache.ranger.unixusersync.process.UnixUserGroupBuilder", 
            "ranger.usersync.ldap.referral": "ignore", 
            "ranger.usersync.group.searchfilter": "", 
            "ranger.usersync.ldap.user.objectclass": "person", 
            "ranger.usersync.logdir": "{{usersync_log_dir}}", 
            "ranger.usersync.ldap.user.searchfilter": "", 
            "ranger.usersync.ldap.groupname.caseconversion": "none", 
            "ranger.usersync.ldap.ldapbindpassword": "", 
            "ranger.usersync.unix.minUserId": "500", 
            "ranger.usersync.policymanager.maxrecordsperapicall": "1000", 
            "ranger.usersync.group.nameattribute": "", 
            "ranger.usersync.policymgr.alias": "ranger.usersync.policymgr.password", 
            "ranger.usersync.keystore.file": "/usr/hdp/current/ranger-usersync/conf/unixauthservice.jks", 
            "ranger.usersync.user.searchenabled": "false", 
            "ranger.usersync.group.usermapsyncenabled": "true", 
            "ranger.usersync.ldap.bindkeystore": "", 
            "ranger.usersync.ldap.user.groupnameattribute": "memberof, ismemberof", 
            "ranger.usersync.kerberos.keytab": "", 
            "ranger.usersync.passwordvalidator.path": "./native/credValidator.uexe", 
            "ranger.usersync.group.objectclass": "", 
            "ranger.usersync.ldap.user.searchscope": "sub", 
            "ranger.usersync.unix.password.file": "/etc/passwd", 
            "ranger.usersync.ldap.user.nameattribute": "", 
            "ranger.usersync.pagedresultsenabled": "true", 
            "ranger.usersync.policymanager.baseURL": "{{ranger_external_url}}", 
            "ranger.usersync.group.search.first.enabled": "false", 
            "ranger.usersync.group.searchenabled": "false", 
            "ranger.usersync.sink.impl.class": "org.apache.ranger.unixusersync.process.PolicyMgrUserGroupBuilder", 
            "ranger.usersync.ssl": "true", 
            "ranger.usersync.ldap.url": "", 
            "ranger.usersync.ldap.searchBase": "dc=hadoop,dc=apache,dc=org", 
            "ranger.usersync.policymgr.keystore": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks", 
            "ranger.usersync.ldap.user.searchbase": "", 
            "ranger.usersync.ldap.username.caseconversion": "none", 
            "ranger.usersync.credstore.filename": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks", 
            "ranger.usersync.keystore.password": "UnIx529p", 
            "ranger.usersync.unix.group.file": "/etc/group", 
            "ranger.usersync.filesource.file": "/tmp/usergroup.txt", 
            "ranger.usersync.group.searchscope": "", 
            "ranger.usersync.truststore.password": "changeit", 
            "ranger.usersync.enabled": "true", 
            "ranger.usersync.sleeptimeinmillisbetweensynccycle": "60000", 
            "ranger.usersync.ldap.binddn": "",
            "ranger.usersync.policymgr.username": "rangerusersync",
            "ranger.usersync.policymanager.mockrun": "false",
            "ranger.usersync.group.searchbase": "",
            "ranger.usersync.ldap.bindalias": "testldapalias",
            "ranger.usersync.truststore.file": "/usr/hdp/current/ranger-usersync/conf/mytruststore.jks",
            "ranger.usersync.port": "5151",
            "ranger.usersync.pagedresultssize": "500",
            "ranger.usersync.group.memberattributename": "",
            "ranger.usersync.kerberos.principal": "",
            "ranger.usersync.source.impl.class": "org.apache.ranger.unixusersync.process.UnixUserGroupBuilder",
            "ranger.usersync.ldap.referral": "ignore",
            "ranger.usersync.group.searchfilter": "",
            "ranger.usersync.ldap.user.objectclass": "person",
            "ranger.usersync.logdir": "{{usersync_log_dir}}",
            "ranger.usersync.ldap.user.searchfilter": "",
            "ranger.usersync.ldap.groupname.caseconversion": "none",
            "ranger.usersync.ldap.ldapbindpassword": "",
            "ranger.usersync.unix.minUserId": "500",
            "ranger.usersync.policymanager.maxrecordsperapicall": "1000",
            "ranger.usersync.group.nameattribute": "",
            "ranger.usersync.policymgr.alias": "ranger.usersync.policymgr.password",
            "ranger.usersync.keystore.file": "/usr/hdp/current/ranger-usersync/conf/unixauthservice.jks",
            "ranger.usersync.user.searchenabled": "false",
            "ranger.usersync.group.usermapsyncenabled": "true",
            "ranger.usersync.ldap.bindkeystore": "",
            "ranger.usersync.ldap.user.groupnameattribute": "memberof, ismemberof",
            "ranger.usersync.kerberos.keytab": "",
            "ranger.usersync.passwordvalidator.path": "./native/credValidator.uexe",
            "ranger.usersync.group.objectclass": "",
            "ranger.usersync.ldap.user.searchscope": "sub",
            "ranger.usersync.unix.password.file": "/etc/passwd",
            "ranger.usersync.ldap.user.nameattribute": "",
            "ranger.usersync.pagedresultsenabled": "true",
            "ranger.usersync.policymanager.baseURL": "{{ranger_external_url}}",
            "ranger.usersync.group.search.first.enabled": "false",
            "ranger.usersync.group.searchenabled": "false",
            "ranger.usersync.sink.impl.class": "org.apache.ranger.unixusersync.process.PolicyMgrUserGroupBuilder",
            "ranger.usersync.ssl": "true",
            "ranger.usersync.ldap.url": "",
            "ranger.usersync.ldap.searchBase": "dc=hadoop,dc=apache,dc=org",
            "ranger.usersync.policymgr.keystore": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks",
            "ranger.usersync.ldap.user.searchbase": "",
            "ranger.usersync.ldap.username.caseconversion": "none",
            "ranger.usersync.credstore.filename": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks",
            "ranger.usersync.keystore.password": "UnIx529p",
            "ranger.usersync.unix.group.file": "/etc/group",
            "ranger.usersync.filesource.file": "/tmp/usergroup.txt",
            "ranger.usersync.group.searchscope": "",
            "ranger.usersync.truststore.password": "changeit",
            "ranger.usersync.enabled": "true",
            "ranger.usersync.sleeptimeinmillisbetweensynccycle": "60000",
             "ranger.usersync.filesource.text.delimiter": ","
        }, 
        },
         "hdfs-site": {
            "dfs.namenode.checkpoint.period": "21600", 
            "dfs.namenode.avoid.write.stale.datanode": "true", 
            "dfs.namenode.startup.delay.block.deletion.sec": "3600", 
            "dfs.namenode.checkpoint.txns": "1000000", 
            "dfs.content-summary.limit": "5000", 
            "dfs.support.append": "true", 
            "dfs.datanode.address": "0.0.0.0:50010", 
            "dfs.cluster.administrators": " hdfs", 
            "dfs.namenode.audit.log.async": "true", 
            "dfs.datanode.balance.bandwidthPerSec": "6250000", 
            "dfs.namenode.safemode.threshold-pct": "1", 
            "dfs.namenode.checkpoint.edits.dir": "${dfs.namenode.checkpoint.dir}", 
            "dfs.namenode.rpc-address": "c6401.ambari.apache.org:8020", 
            "dfs.permissions.enabled": "true", 
            "dfs.client.read.shortcircuit": "true", 
            "dfs.https.port": "50470", 
            "dfs.namenode.https-address": "c6401.ambari.apache.org:50470", 
            "nfs.file.dump.dir": "/tmp/.hdfs-nfs", 
            "dfs.blocksize": "134217728", 
            "dfs.blockreport.initialDelay": "120", 
            "dfs.journalnode.edits.dir": "/hadoop/hdfs/journalnode", 
            "dfs.namenode.fslock.fair": "false", 
            "dfs.datanode.max.transfer.threads": "4096", 
            "dfs.heartbeat.interval": "3", 
            "dfs.replication": "3", 
            "dfs.namenode.handler.count": "50", 
            "dfs.namenode.checkpoint.dir": "/grid/0/hadoop/hdfs/namesecondary", 
            "fs.permissions.umask-mode": "022", 
            "dfs.namenode.stale.datanode.interval": "30000", 
            "dfs.datanode.ipc.address": "0.0.0.0:8010", 
            "dfs.datanode.failed.volumes.tolerated": "0", 
            "dfs.datanode.data.dir": "/grid/0/hadoop/hdfs/data", 
            "dfs.namenode.http-address": "c6401.ambari.apache.org:50070", 
            "dfs.webhdfs.enabled": "true", 
            "dfs.encrypt.data.transfer.cipher.suites": "AES/CTR/NoPadding", 
            "dfs.namenode.accesstime.precision": "0", 
            "dfs.datanode.https.address": "0.0.0.0:50475", 
            "dfs.namenode.write.stale.datanode.ratio": "1.0f", 
            "dfs.namenode.secondary.http-address": "c6401.ambari.apache.org:50090", 
            "nfs.exports.allowed.hosts": "* rw", 
            "dfs.datanode.http.address": "0.0.0.0:50075", 
            "dfs.datanode.du.reserved": "33011188224", 
            "dfs.client.read.shortcircuit.streams.cache.size": "4096", 
            "dfs.http.policy": "HTTP_ONLY", 
            "dfs.block.access.token.enable": "true", 
            "dfs.client.retry.policy.enabled": "false", 
            "dfs.namenode.name.dir.restore": "true", 
            "dfs.permissions.superusergroup": "hdfs", 
            "dfs.journalnode.https-address": "0.0.0.0:8481", 
            "dfs.journalnode.http-address": "0.0.0.0:8480", 
            "dfs.domain.socket.path": "/var/lib/hadoop-hdfs/dn_socket", 
            "dfs.namenode.avoid.read.stale.datanode": "true", 
            "dfs.hosts.exclude": "/etc/hadoop/conf/dfs.exclude", 
            "dfs.datanode.data.dir.perm": "750", 
            "dfs.encryption.key.provider.uri": "", 
            "dfs.replication.max": "50", 
            "dfs.namenode.checkpoint.period": "21600",
            "dfs.namenode.avoid.write.stale.datanode": "true",
            "dfs.namenode.startup.delay.block.deletion.sec": "3600",
            "dfs.namenode.checkpoint.txns": "1000000",
            "dfs.content-summary.limit": "5000",
            "dfs.support.append": "true",
            "dfs.datanode.address": "0.0.0.0:50010",
            "dfs.cluster.administrators": " hdfs",
            "dfs.namenode.audit.log.async": "true",
            "dfs.datanode.balance.bandwidthPerSec": "6250000",
            "dfs.namenode.safemode.threshold-pct": "1",
            "dfs.namenode.checkpoint.edits.dir": "${dfs.namenode.checkpoint.dir}",
            "dfs.namenode.rpc-address": "c6401.ambari.apache.org:8020",
            "dfs.permissions.enabled": "true",
            "dfs.client.read.shortcircuit": "true",
            "dfs.https.port": "50470",
            "dfs.namenode.https-address": "c6401.ambari.apache.org:50470",
            "nfs.file.dump.dir": "/tmp/.hdfs-nfs",
            "dfs.blocksize": "134217728",
            "dfs.blockreport.initialDelay": "120",
            "dfs.journalnode.edits.dir": "/hadoop/hdfs/journalnode",
            "dfs.namenode.fslock.fair": "false",
            "dfs.datanode.max.transfer.threads": "4096",
            "dfs.heartbeat.interval": "3",
            "dfs.replication": "3",
            "dfs.namenode.handler.count": "50",
            "dfs.namenode.checkpoint.dir": "/grid/0/hadoop/hdfs/namesecondary",
            "fs.permissions.umask-mode": "022",
            "dfs.namenode.stale.datanode.interval": "30000",
            "dfs.datanode.ipc.address": "0.0.0.0:8010",
            "dfs.datanode.failed.volumes.tolerated": "0",
            "dfs.datanode.data.dir": "/grid/0/hadoop/hdfs/data",
            "dfs.namenode.http-address": "c6401.ambari.apache.org:50070",
            "dfs.webhdfs.enabled": "true",
            "dfs.encrypt.data.transfer.cipher.suites": "AES/CTR/NoPadding",
            "dfs.namenode.accesstime.precision": "0",
            "dfs.datanode.https.address": "0.0.0.0:50475",
            "dfs.namenode.write.stale.datanode.ratio": "1.0f",
            "dfs.namenode.secondary.http-address": "c6401.ambari.apache.org:50090",
            "nfs.exports.allowed.hosts": "* rw",
            "dfs.datanode.http.address": "0.0.0.0:50075",
            "dfs.datanode.du.reserved": "33011188224",
            "dfs.client.read.shortcircuit.streams.cache.size": "4096",
            "dfs.http.policy": "HTTP_ONLY",
            "dfs.block.access.token.enable": "true",
            "dfs.client.retry.policy.enabled": "false",
            "dfs.namenode.name.dir.restore": "true",
            "dfs.permissions.superusergroup": "hdfs",
            "dfs.journalnode.https-address": "0.0.0.0:8481",
            "dfs.journalnode.http-address": "0.0.0.0:8480",
            "dfs.domain.socket.path": "/var/lib/hadoop-hdfs/dn_socket",
            "dfs.namenode.avoid.read.stale.datanode": "true",
            "dfs.hosts.exclude": "/etc/hadoop/conf/dfs.exclude",
            "dfs.datanode.data.dir.perm": "750",
            "dfs.encryption.key.provider.uri": "",
            "dfs.replication.max": "50",
             "dfs.namenode.name.dir": "/grid/0/hadoop/hdfs/namenode"
        }, 
        },
         "ranger-tagsync-site": {
            "ranger.tagsync.dest.ranger.ssl.config.filename": "/usr/hdp/current/ranger-tagsync/conf/mytruststore.jks", 
            "ranger.tagsync.source.atlasrest.username": "", 
            "ranger.tagsync.logdir": "/var/log/ranger/tagsync", 
            "ranger.tagsync.source.atlasrest.download.interval.millis": "", 
            "ranger.tagsync.keystore.filename": "/usr/hdp/current/ranger-tagsync/conf/rangertagsync.jceks", 
            "ranger.tagsync.source.file.check.interval.millis": "", 
            "ranger.tagsync.source.atlasrest.endpoint": "", 
            "ranger.tagsync.dest.ranger.username": "rangertagsync", 
            "ranger.tagsync.dest.ranger.endpoint": "{{ranger_external_url}}", 
            "ranger.tagsync.kerberos.principal": "", 
            "ranger.tagsync.kerberos.keytab": "", 
            "ranger.tagsync.source.atlas": "false", 
            "ranger.tagsync.source.atlasrest": "false", 
            "ranger.tagsync.source.file": "false", 
            "ranger.tagsync.dest.ranger.ssl.config.filename": "/usr/hdp/current/ranger-tagsync/conf/mytruststore.jks",
            "ranger.tagsync.source.atlasrest.username": "",
            "ranger.tagsync.logdir": "/var/log/ranger/tagsync",
            "ranger.tagsync.source.atlasrest.download.interval.millis": "",
            "ranger.tagsync.keystore.filename": "/usr/hdp/current/ranger-tagsync/conf/rangertagsync.jceks",
            "ranger.tagsync.source.file.check.interval.millis": "",
            "ranger.tagsync.source.atlasrest.endpoint": "",
            "ranger.tagsync.dest.ranger.username": "rangertagsync",
            "ranger.tagsync.dest.ranger.endpoint": "{{ranger_external_url}}",
            "ranger.tagsync.kerberos.principal": "",
            "ranger.tagsync.kerberos.keytab": "",
            "ranger.tagsync.source.atlas": "false",
            "ranger.tagsync.source.atlasrest": "false",
            "ranger.tagsync.source.file": "false",
             "ranger.tagsync.source.file.filename": ""
        }, 
        },
         "zoo.cfg": {
            "clientPort": "2181", 
            "autopurge.purgeInterval": "24", 
            "syncLimit": "5", 
            "dataDir": "/grid/0/hadoop/zookeeper", 
            "initLimit": "10", 
            "tickTime": "2000", 
            "clientPort": "2181",
            "autopurge.purgeInterval": "24",
            "syncLimit": "5",
            "dataDir": "/grid/0/hadoop/zookeeper",
            "initLimit": "10",
            "tickTime": "2000",
             "autopurge.snapRetainCount": "30"
        }, 
        },
         "hadoop-policy": {
            "security.job.client.protocol.acl": "*", 
            "security.job.task.protocol.acl": "*", 
            "security.datanode.protocol.acl": "*", 
            "security.namenode.protocol.acl": "*", 
            "security.client.datanode.protocol.acl": "*", 
            "security.inter.tracker.protocol.acl": "*", 
            "security.refresh.usertogroups.mappings.protocol.acl": "hadoop", 
            "security.client.protocol.acl": "*", 
            "security.refresh.policy.protocol.acl": "hadoop", 
            "security.admin.operations.protocol.acl": "hadoop", 
            "security.job.client.protocol.acl": "*",
            "security.job.task.protocol.acl": "*",
            "security.datanode.protocol.acl": "*",
            "security.namenode.protocol.acl": "*",
            "security.client.datanode.protocol.acl": "*",
            "security.inter.tracker.protocol.acl": "*",
            "security.refresh.usertogroups.mappings.protocol.acl": "hadoop",
            "security.client.protocol.acl": "*",
            "security.refresh.policy.protocol.acl": "hadoop",
            "security.admin.operations.protocol.acl": "hadoop",
             "security.inter.datanode.protocol.acl": "*"
        }, 
        },
         "hdfs-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#  http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing,\n# software distributed under the License is distributed on an\n# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n# KIND, either express or implied.  See the License for the\n# specific language governing permissions and limitations\n# under the License.\n#\n\n\n# Define some default values that can be overridden by system properties\n# To change daemon root logger use hadoop_root_logger in hadoop-env\nhadoop.root.logger=INFO,console\nhadoop.log.dir=.\nhadoop.log.file=hadoop.log\n\n\n# Define the root logger to the system property \"hadoop.root.logger\".\nlog4j.rootLogger=${hadoop.root.logger}, EventCounter\n\n# Logging Threshold\nlog4j.threshhold=ALL\n\n#\n# Daily Rolling File Appender\n#\n\nlog4j.appender.DRFA=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.DRFA.File=${hadoop.log.dir}/${hadoop.log.file}\n\n# Rollver at midnight\nlog4j.appender.DRFA.DatePattern=.yyyy-MM-dd\n\n# 30-day backup\n#log4j.appender.DRFA.MaxBackupIndex=30\nlog4j.appender.DRFA.layout=org.apache.log4j.PatternLayout\n\n# Pattern format: Date LogLevel LoggerName LogMessage\nlog4j.appender.DRFA.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\n# Debugging Pattern format\n#log4j.appender.DRFA.layout.ConversionPattern=%d{ISO8601} %-5p %c{2} (%F:%M(%L)) - %m%n\n\n\n#\n# console\n# Add \"console\" to rootlogger above if you want to use this\n#\n\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.target=System.err\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{yy/MM/dd HH:mm:ss} %p %c{2}: %m%n\n\n#\n# TaskLog Appender\n#\n\n#Default values\nhadoop.tasklog.taskid=null\nhadoop.tasklog.iscleanup=false\nhadoop.tasklog.noKeepSplits=4\nhadoop.tasklog.totalLogFileSize=100\nhadoop.tasklog.purgeLogSplits=true\nhadoop.tasklog.logsRetainHours=12\n\nlog4j.appender.TLA=org.apache.hadoop.mapred.TaskLogAppender\nlog4j.appender.TLA.taskId=${hadoop.tasklog.taskid}\nlog4j.appender.TLA.isCleanup=${hadoop.tasklog.iscleanup}\nlog4j.appender.TLA.totalLogFileSize=${hadoop.tasklog.totalLogFileSize}\n\nlog4j.appender.TLA.layout=org.apache.log4j.PatternLayout\nlog4j.appender.TLA.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\n\n#\n#Security audit appender\n#\nhadoop.security.logger=INFO,console\nhadoop.security.log.maxfilesize=256MB\nhadoop.security.log.maxbackupindex=20\nlog4j.category.SecurityLogger=${hadoop.security.logger}\nhadoop.security.log.file=SecurityAuth.audit\nlog4j.appender.DRFAS=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.DRFAS.File=${hadoop.log.dir}/${hadoop.security.log.file}\nlog4j.appender.DRFAS.layout=org.apache.log4j.PatternLayout\nlog4j.appender.DRFAS.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\nlog4j.appender.DRFAS.DatePattern=.yyyy-MM-dd\n\nlog4j.appender.RFAS=org.apache.log4j.RollingFileAppender\nlog4j.appender.RFAS.File=${hadoop.log.dir}/${hadoop.security.log.file}\nlog4j.appender.RFAS.layout=org.apache.log4j.PatternLayout\nlog4j.appender.RFAS.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\nlog4j.appender.RFAS.MaxFileSize=${hadoop.security.log.maxfilesize}\nlog4j.appender.RFAS.MaxBackupIndex=${hadoop.security.log.maxbackupindex}\n\n#\n# hdfs audit logging\n#\nhdfs.audit.logger=INFO,console\nlog4j.logger.org.apache.hadoop.hdfs.server.namenode.FSNamesystem.audit=${hdfs.audit.logger}\nlog4j.additivity.org.apache.hadoop.hdfs.server.namenode.FSNamesystem.audit=false\nlog4j.appender.DRFAAUDIT=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.DRFAAUDIT.File=${hadoop.log.dir}/hdfs-audit.log\nlog4j.appender.DRFAAUDIT.layout=org.apache.log4j.PatternLayout\nlog4j.appender.DRFAAUDIT.layout.ConversionPattern=%d{ISO8601} %p %c{2}: %m%n\nlog4j.appender.DRFAAUDIT.DatePattern=.yyyy-MM-dd\n\n#\n# NameNode metrics logging.\n# The default is to retain two namenode-metrics.log files up to 64MB each.\n#\nnamenode.metrics.logger=INFO,NullAppender\nlog4j.logger.NameNodeMetricsLog=${namenode.metrics.logger}\nlog4j.additivity.NameNodeMetricsLog=false\nlog4j.appender.NNMETRICSRFA=org.apache.log4j.RollingFileAppender\nlog4j.appender.NNMETRICSRFA.File=${hadoop.log.dir}/namenode-metrics.log\nlog4j.appender.NNMETRICSRFA.layout=org.apache.log4j.PatternLayout\nlog4j.appender.NNMETRICSRFA.layout.ConversionPattern=%d{ISO8601} %m%n\nlog4j.appender.NNMETRICSRFA.MaxBackupIndex=1\nlog4j.appender.NNMETRICSRFA.MaxFileSize=64MB\n\n#\n# mapred audit logging\n#\nmapred.audit.logger=INFO,console\nlog4j.logger.org.apache.hadoop.mapred.AuditLogger=${mapred.audit.logger}\nlog4j.additivity.org.apache.hadoop.mapred.AuditLogger=false\nlog4j.appender.MRAUDIT=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.MRAUDIT.File=${hadoop.log.dir}/mapred-audit.log\nlog4j.appender.MRAUDIT.layout=org.apache.log4j.PatternLayout\nlog4j.appender.MRAUDIT.layout.ConversionPattern=%d{ISO8601} %p %c{2}: %m%n\nlog4j.appender.MRAUDIT.DatePattern=.yyyy-MM-dd\n\n#\n# Rolling File Appender\n#\n\nlog4j.appender.RFA=org.apache.log4j.RollingFileAppender\nlog4j.appender.RFA.File=${hadoop.log.dir}/${hadoop.log.file}\n\n# Logfile size and and 30-day backups\nlog4j.appender.RFA.MaxFileSize=256MB\nlog4j.appender.RFA.MaxBackupIndex=10\n\nlog4j.appender.RFA.layout=org.apache.log4j.PatternLayout\nlog4j.appender.RFA.layout.ConversionPattern=%d{ISO8601} %-5p %c{2} - %m%n\nlog4j.appender.RFA.layout.ConversionPattern=%d{ISO8601} %-5p %c{2} (%F:%M(%L)) - %m%n\n\n\n# Custom Logging levels\n\nhadoop.metrics.log.level=INFO\n#log4j.logger.org.apache.hadoop.mapred.JobTracker=DEBUG\n#log4j.logger.org.apache.hadoop.mapred.TaskTracker=DEBUG\n#log4j.logger.org.apache.hadoop.fs.FSNamesystem=DEBUG\nlog4j.logger.org.apache.hadoop.metrics2=${hadoop.metrics.log.level}\n\n# Jets3t library\nlog4j.logger.org.jets3t.service.impl.rest.httpclient.RestS3Service=ERROR\n\n#\n# Null Appender\n# Trap security logger on the hadoop client side\n#\nlog4j.appender.NullAppender=org.apache.log4j.varia.NullAppender\n\n#\n# Event Counter Appender\n# Sends counts of logging messages at different severity levels to Hadoop Metrics.\n#\nlog4j.appender.EventCounter=org.apache.hadoop.log.metrics.EventCounter\n\n# Removes \"deprecated\" messages\nlog4j.logger.org.apache.hadoop.conf.Configuration.deprecation=WARN\n\n#\n# HDFS block state change log from block manager\n#\n# Uncomment the following to suppress normal block state change\n# messages from BlockManager in NameNode.\n#log4j.logger.BlockStateChange=WARN"
        }, 
        },
         "ranger-hdfs-plugin-properties": {
            "hadoop.rpc.protection": "authentication", 
            "ranger-hdfs-plugin-enabled": "No", 
            "REPOSITORY_CONFIG_USERNAME": "hadoop", 
            "policy_user": "ambari-qa", 
            "common.name.for.certificate": "", 
            "hadoop.rpc.protection": "authentication",
            "ranger-hdfs-plugin-enabled": "No",
            "REPOSITORY_CONFIG_USERNAME": "hadoop",
            "policy_user": "ambari-qa",
            "common.name.for.certificate": "",
             "REPOSITORY_CONFIG_PASSWORD": "hadoop"
        }, 
        },
         "core-site": {
            "hadoop.proxyuser.root.hosts": "c6401.ambari.apache.org", 
            "io.serializations": "org.apache.hadoop.io.serializer.WritableSerialization", 
            "fs.trash.interval": "360", 
            "ha.failover-controller.active-standby-elector.zk.op.retries": "120", 
            "hadoop.http.authentication.simple.anonymous.allowed": "true", 
            "hadoop.security.authentication": "simple", 
            "hadoop.proxyuser.root.groups": "*", 
            "ipc.client.connection.maxidletime": "30000", 
            "hadoop.security.key.provider.path": "", 
            "mapreduce.jobtracker.webinterface.trusted": "false", 
            "hadoop.security.authorization": "false", 
            "net.topology.script.file.name": "/etc/hadoop/conf/topology_script.py", 
            "ipc.server.tcpnodelay": "true", 
            "ipc.client.connect.max.retries": "50", 
            "hadoop.security.auth_to_local": "DEFAULT", 
            "io.file.buffer.size": "131072", 
            "hadoop.proxyuser.hdfs.hosts": "*", 
            "hadoop.proxyuser.hdfs.groups": "*", 
            "ipc.client.idlethreshold": "8000", 
            "fs.defaultFS": "hdfs://c6401.ambari.apache.org:8020", 
            "hadoop.proxyuser.root.hosts": "c6401.ambari.apache.org",
            "io.serializations": "org.apache.hadoop.io.serializer.WritableSerialization",
            "fs.trash.interval": "360",
            "ha.failover-controller.active-standby-elector.zk.op.retries": "120",
            "hadoop.http.authentication.simple.anonymous.allowed": "true",
            "hadoop.security.authentication": "simple",
            "hadoop.proxyuser.root.groups": "*",
            "ipc.client.connection.maxidletime": "30000",
            "hadoop.security.key.provider.path": "",
            "mapreduce.jobtracker.webinterface.trusted": "false",
            "hadoop.security.authorization": "false",
            "net.topology.script.file.name": "/etc/hadoop/conf/topology_script.py",
            "ipc.server.tcpnodelay": "true",
            "ipc.client.connect.max.retries": "50",
            "hadoop.security.auth_to_local": "DEFAULT",
            "io.file.buffer.size": "131072",
            "hadoop.proxyuser.hdfs.hosts": "*",
            "hadoop.proxyuser.hdfs.groups": "*",
            "ipc.client.idlethreshold": "8000",
            "fs.defaultFS": "hdfs://c6401.ambari.apache.org:8020",
             "io.compression.codecs": "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec,org.apache.hadoop.io.compress.SnappyCodec"
        }, 
        },
         "hadoop-env": {
            "keyserver_port": "", 
            "proxyuser_group": "users", 
            "hdfs_user_nproc_limit": "65536", 
            "hdfs_log_dir_prefix": "/var/log/hadoop", 
            "hdfs_user_nofile_limit": "128000", 
            "hdfs_user": "hdfs", 
            "keyserver_host": " ", 
            "namenode_opt_maxnewsize": "128m", 
            "namenode_opt_maxpermsize": "256m", 
            "content": "\n# Set Hadoop-specific environment variables here.\n\n# The only required environment variable is JAVA_HOME.  All others are\n# optional.  When running a distributed configuration it is best to\n# set JAVA_HOME in this file, so that it is correctly defined on\n# remote nodes.\n\n# The java implementation to use.  Required.\nexport JAVA_HOME={{java_home}}\nexport HADOOP_HOME_WARN_SUPPRESS=1\n\n# Hadoop home directory\nexport HADOOP_HOME=${HADOOP_HOME:-{{hadoop_home}}}\n\n# Hadoop Configuration Directory\n\n{# this is different for HDP1 #}\n# Path to jsvc required by secure HDP 2.0 datanode\nexport JSVC_HOME={{jsvc_path}}\n\n\n# The maximum amount of heap to use, in MB. Default is 1000.\nexport HADOOP_HEAPSIZE=\"{{hadoop_heapsize}}\"\n\nexport HADOOP_NAMENODE_INIT_HEAPSIZE=\"-Xms{{namenode_heapsize}}\"\n\n# Extra Java runtime options.  Empty by default.\nexport HADOOP_OPTS=\"-Djava.net.preferIPv4Stack=true ${HADOOP_OPTS}\"\n\n# Command specific options appended to HADOOP_OPTS when specified\nHADOOP_JOBTRACKER_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{jtnode_opt_newsize}} -XX:MaxNewSize={{jtnode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xmx{{jtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dmapred.audit.logger=INFO,MRAUDIT -Dhadoop.mapreduce.jobsummary.logger=INFO,JSA ${HADOOP_JOBTRACKER_OPTS}\"\n\nHADOOP_TASKTRACKER_OPTS=\"-server -Xmx{{ttnode_heapsize}} -Dhadoop.security.logger=ERROR,console -Dmapred.audit.logger=ERROR,console ${HADOOP_TASKTRACKER_OPTS}\"\n\n{% if java_version < 8 %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -XX:PermSize={{namenode_opt_permsize}} -XX:MaxPermSize={{namenode_opt_maxpermsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -XX:PermSize=128m -XX:MaxPermSize=256m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m -XX:MaxPermSize=512m $HADOOP_CLIENT_OPTS\"\n\n{% else %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m $HADOOP_CLIENT_OPTS\"\n{% endif %}\n\nHADOOP_NFS3_OPTS=\"-Xmx{{nfsgateway_heapsize}}m -Dhadoop.security.logger=ERROR,DRFAS ${HADOOP_NFS3_OPTS}\"\nHADOOP_BALANCER_OPTS=\"-server -Xmx{{hadoop_heapsize}}m ${HADOOP_BALANCER_OPTS}\"\n\n\n# On secure datanodes, user to run the datanode as after dropping privileges\nexport HADOOP_SECURE_DN_USER=${HADOOP_SECURE_DN_USER:-{{hadoop_secure_dn_user}}}\n\n# Extra ssh options.  Empty by default.\nexport HADOOP_SSH_OPTS=\"-o ConnectTimeout=5 -o SendEnv=HADOOP_CONF_DIR\"\n\n# Where log files are stored.  $HADOOP_HOME/logs by default.\nexport HADOOP_LOG_DIR={{hdfs_log_dir_prefix}}/$USER\n\n# History server logs\nexport HADOOP_MAPRED_LOG_DIR={{mapred_log_dir_prefix}}/$USER\n\n# Where log files are stored in the secure data environment.\nexport HADOOP_SECURE_DN_LOG_DIR={{hdfs_log_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# File naming remote slave hosts.  $HADOOP_HOME/conf/slaves by default.\n# export HADOOP_SLAVES=${HADOOP_HOME}/conf/slaves\n\n# host:path where hadoop code should be rsync'd from.  Unset by default.\n# export HADOOP_MASTER=master:/home/$USER/src/hadoop\n\n# Seconds to sleep between slave commands.  Unset by default.  This\n# can be useful in large clusters, where, e.g., slave rsyncs can\n# otherwise arrive faster than the master can service them.\n# export HADOOP_SLAVE_SLEEP=0.1\n\n# The directory where pid files are stored. /tmp by default.\nexport HADOOP_PID_DIR={{hadoop_pid_dir_prefix}}/$USER\nexport HADOOP_SECURE_DN_PID_DIR={{hadoop_pid_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# History server pid\nexport HADOOP_MAPRED_PID_DIR={{mapred_pid_dir_prefix}}/$USER\n\nYARN_RESOURCEMANAGER_OPTS=\"-Dyarn.server.resourcemanager.appsummary.logger=INFO,RMSUMMARY\"\n\n# A string representing this instance of hadoop. $USER by default.\nexport HADOOP_IDENT_STRING=$USER\n\n# The scheduling priority for daemon processes.  See 'man nice'.\n\n# export HADOOP_NICENESS=10\n\n# Add database libraries\nJAVA_JDBC_LIBS=\"\"\nif [ -d \"/usr/share/java\" ]; then\n  for jarFile in `ls /usr/share/java | grep -E \"(mysql|ojdbc|postgresql|sqljdbc)\" 2>/dev/null`\n  do\n    JAVA_JDBC_LIBS=${JAVA_JDBC_LIBS}:$jarFile\n  done\nfi\n\n# Add libraries to the hadoop classpath - some may not need a colon as they already include it\nexport HADOOP_CLASSPATH=${HADOOP_CLASSPATH}${JAVA_JDBC_LIBS}\n\n# Setting path to hdfs command line\nexport HADOOP_LIBEXEC_DIR={{hadoop_libexec_dir}}\n\n# Mostly required for hadoop 2.0\nexport JAVA_LIBRARY_PATH=${JAVA_LIBRARY_PATH}\n\nexport HADOOP_OPTS=\"-Dhdp.version=$HDP_VERSION $HADOOP_OPTS\"\n\n{% if is_datanode_max_locked_memory_set %}\n# Fix temporary bug, when ulimit from conf files is not picked up, without full relogin. \n# Makes sense to fix only when runing DN as root \nif [ \"$command\" == \"datanode\" ] && [ \"$EUID\" -eq 0 ] && [ -n \"$HADOOP_SECURE_DN_USER\" ]; then\n  ulimit -l {{datanode_max_locked_memory}}\nfi\n{% endif %}", 
            "namenode_heapsize": "1024m", 
            "namenode_opt_newsize": "128m", 
            "nfsgateway_heapsize": "1024", 
            "dtnode_heapsize": "1024m", 
            "hadoop_root_logger": "INFO,RFA", 
            "hadoop_heapsize": "1024", 
            "hadoop_pid_dir_prefix": "/var/run/hadoop", 
            "namenode_opt_permsize": "128m", 
            "keyserver_port": "",
            "proxyuser_group": "users",
            "hdfs_user_nproc_limit": "65536",
            "hdfs_log_dir_prefix": "/var/log/hadoop",
            "hdfs_user_nofile_limit": "128000",
            "hdfs_user": "hdfs",
            "keyserver_host": " ",
            "namenode_opt_maxnewsize": "128m",
            "namenode_opt_maxpermsize": "256m",
            "content": "\n# Set Hadoop-specific environment variables here.\n\n# The only required environment variable is JAVA_HOME.  All others are\n# optional.  When running a distributed configuration it is best to\n# set JAVA_HOME in this file, so that it is correctly defined on\n# remote nodes.\n\n# The java implementation to use.  Required.\nexport JAVA_HOME={{java_home}}\nexport HADOOP_HOME_WARN_SUPPRESS=1\n\n# Hadoop home directory\nexport HADOOP_HOME=${HADOOP_HOME:-{{hadoop_home}}}\n\n# Hadoop Configuration Directory\n\n{# this is different for HDP1 #}\n# Path to jsvc required by secure HDP 2.0 datanode\nexport JSVC_HOME={{jsvc_path}}\n\n\n# The maximum amount of heap to use, in MB. Default is 1000.\nexport HADOOP_HEAPSIZE=\"{{hadoop_heapsize}}\"\n\nexport HADOOP_NAMENODE_INIT_HEAPSIZE=\"-Xms{{namenode_heapsize}}\"\n\n# Extra Java runtime options.  Empty by default.\nexport HADOOP_OPTS=\"-Djava.net.preferIPv4Stack=true ${HADOOP_OPTS}\"\n\n# Command specific options appended to HADOOP_OPTS when specified\nHADOOP_JOBTRACKER_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{jtnode_opt_newsize}} -XX:MaxNewSize={{jtnode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xmx{{jtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dmapred.audit.logger=INFO,MRAUDIT -Dhadoop.mapreduce.jobsummary.logger=INFO,JSA ${HADOOP_JOBTRACKER_OPTS}\"\n\nHADOOP_TASKTRACKER_OPTS=\"-server -Xmx{{ttnode_heapsize}} -Dhadoop.security.logger=ERROR,console -Dmapred.audit.logger=ERROR,console ${HADOOP_TASKTRACKER_OPTS}\"\n\n{% if java_version < 8 %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -XX:PermSize={{namenode_opt_permsize}} -XX:MaxPermSize={{namenode_opt_maxpermsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -XX:PermSize=128m -XX:MaxPermSize=256m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m -XX:MaxPermSize=512m $HADOOP_CLIENT_OPTS\"\n\n{% else %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m $HADOOP_CLIENT_OPTS\"\n{% endif %}\n\nHADOOP_NFS3_OPTS=\"-Xmx{{nfsgateway_heapsize}}m -Dhadoop.security.logger=ERROR,DRFAS ${HADOOP_NFS3_OPTS}\"\nHADOOP_BALANCER_OPTS=\"-server -Xmx{{hadoop_heapsize}}m ${HADOOP_BALANCER_OPTS}\"\n\n\n# On secure datanodes, user to run the datanode as after dropping privileges\nexport HADOOP_SECURE_DN_USER=${HADOOP_SECURE_DN_USER:-{{hadoop_secure_dn_user}}}\n\n# Extra ssh options.  Empty by default.\nexport HADOOP_SSH_OPTS=\"-o ConnectTimeout=5 -o SendEnv=HADOOP_CONF_DIR\"\n\n# Where log files are stored.  $HADOOP_HOME/logs by default.\nexport HADOOP_LOG_DIR={{hdfs_log_dir_prefix}}/$USER\n\n# History server logs\nexport HADOOP_MAPRED_LOG_DIR={{mapred_log_dir_prefix}}/$USER\n\n# Where log files are stored in the secure data environment.\nexport HADOOP_SECURE_DN_LOG_DIR={{hdfs_log_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# File naming remote slave hosts.  $HADOOP_HOME/conf/slaves by default.\n# export HADOOP_SLAVES=${HADOOP_HOME}/conf/slaves\n\n# host:path where hadoop code should be rsync'd from.  Unset by default.\n# export HADOOP_MASTER=master:/home/$USER/src/hadoop\n\n# Seconds to sleep between slave commands.  Unset by default.  This\n# can be useful in large clusters, where, e.g., slave rsyncs can\n# otherwise arrive faster than the master can service them.\n# export HADOOP_SLAVE_SLEEP=0.1\n\n# The directory where pid files are stored. /tmp by default.\nexport HADOOP_PID_DIR={{hadoop_pid_dir_prefix}}/$USER\nexport HADOOP_SECURE_DN_PID_DIR={{hadoop_pid_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# History server pid\nexport HADOOP_MAPRED_PID_DIR={{mapred_pid_dir_prefix}}/$USER\n\nYARN_RESOURCEMANAGER_OPTS=\"-Dyarn.server.resourcemanager.appsummary.logger=INFO,RMSUMMARY\"\n\n# A string representing this instance of hadoop. $USER by default.\nexport HADOOP_IDENT_STRING=$USER\n\n# The scheduling priority for daemon processes.  See 'man nice'.\n\n# export HADOOP_NICENESS=10\n\n# Add database libraries\nJAVA_JDBC_LIBS=\"\"\nif [ -d \"/usr/share/java\" ]; then\n  for jarFile in `ls /usr/share/java | grep -E \"(mysql|ojdbc|postgresql|sqljdbc)\" 2>/dev/null`\n  do\n    JAVA_JDBC_LIBS=${JAVA_JDBC_LIBS}:$jarFile\n  done\nfi\n\n# Add libraries to the hadoop classpath - some may not need a colon as they already include it\nexport HADOOP_CLASSPATH=${HADOOP_CLASSPATH}${JAVA_JDBC_LIBS}\n\n# Setting path to hdfs command line\nexport HADOOP_LIBEXEC_DIR={{hadoop_libexec_dir}}\n\n# Mostly required for hadoop 2.0\nexport JAVA_LIBRARY_PATH=${JAVA_LIBRARY_PATH}\n\nexport HADOOP_OPTS=\"-Dhdp.version=$HDP_VERSION $HADOOP_OPTS\"\n\n{% if is_datanode_max_locked_memory_set %}\n# Fix temporary bug, when ulimit from conf files is not picked up, without full relogin. \n# Makes sense to fix only when runing DN as root \nif [ \"$command\" == \"datanode\" ] && [ \"$EUID\" -eq 0 ] && [ -n \"$HADOOP_SECURE_DN_USER\" ]; then\n  ulimit -l {{datanode_max_locked_memory}}\nfi\n{% endif %}",
            "namenode_heapsize": "1024m",
            "namenode_opt_newsize": "128m",
            "nfsgateway_heapsize": "1024",
            "dtnode_heapsize": "1024m",
            "hadoop_root_logger": "INFO,RFA",
            "hadoop_heapsize": "1024",
            "hadoop_pid_dir_prefix": "/var/run/hadoop",
            "namenode_opt_permsize": "128m",
             "hdfs_tmp_dir": "/tmp"
        }, 
        },
         "zookeeper-log4j": {
             "content": "\n#\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#   http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing,\n# software distributed under the License is distributed on an\n# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n# KIND, either express or implied.  See the License for the\n# specific language governing permissions and limitations\n# under the License.\n#\n#\n#\n\n#\n# ZooKeeper Logging Configuration\n#\n\n# DEFAULT: console appender only\nlog4j.rootLogger=INFO, CONSOLE\n\n# Example with rolling log file\n#log4j.rootLogger=DEBUG, CONSOLE, ROLLINGFILE\n\n# Example with rolling log file and tracing\n#log4j.rootLogger=TRACE, CONSOLE, ROLLINGFILE, TRACEFILE\n\n#\n# Log INFO level and above messages to the console\n#\nlog4j.appender.CONSOLE=org.apache.log4j.ConsoleAppender\nlog4j.appender.CONSOLE.Threshold=INFO\nlog4j.appender.CONSOLE.layout=org.apache.log4j.PatternLayout\nlog4j.appender.CONSOLE.layout.ConversionPattern=%d{ISO8601} - %-5p [%t:%C{1}@%L] - %m%n\n\n#\n# Add ROLLINGFILE to rootLogger to get log file output\n#    Log DEBUG level and above messages to a log file\nlog4j.appender.ROLLINGFILE=org.apache.log4j.RollingFileAppender\nlog4j.appender.ROLLINGFILE.Threshold=DEBUG\nlog4j.appender.ROLLINGFILE.File=zookeeper.log\n\n# Max log file size of 10MB\nlog4j.appender.ROLLINGFILE.MaxFileSize=10MB\n# uncomment the next line to limit number of backup files\n#log4j.appender.ROLLINGFILE.MaxBackupIndex=10\n\nlog4j.appender.ROLLINGFILE.layout=org.apache.log4j.PatternLayout\nlog4j.appender.ROLLINGFILE.layout.ConversionPattern=%d{ISO8601} - %-5p [%t:%C{1}@%L] - %m%n\n\n\n#\n# Add TRACEFILE to rootLogger to get log file output\n#    Log DEBUG level and above messages to a log file\nlog4j.appender.TRACEFILE=org.apache.log4j.FileAppender\nlog4j.appender.TRACEFILE.Threshold=TRACE\nlog4j.appender.TRACEFILE.File=zookeeper_trace.log\n\nlog4j.appender.TRACEFILE.layout=org.apache.log4j.PatternLayout\n### Notice we are including log4j's NDC here (%x)\nlog4j.appender.TRACEFILE.layout.ConversionPattern=%d{ISO8601} - %-5p [%t:%C{1}@%L][%x] - %m%n"
        }, 
        },
         "ssl-server": {
            "ssl.server.keystore.location": "/etc/security/serverKeys/keystore.jks", 
            "ssl.server.keystore.keypassword": "bigdata", 
            "ssl.server.truststore.location": "/etc/security/serverKeys/all.jks", 
            "ssl.server.keystore.password": "bigdata", 
            "ssl.server.truststore.password": "bigdata", 
            "ssl.server.truststore.type": "jks", 
            "ssl.server.keystore.type": "jks", 
            "ssl.server.keystore.location": "/etc/security/serverKeys/keystore.jks",
            "ssl.server.keystore.keypassword": "bigdata",
            "ssl.server.truststore.location": "/etc/security/serverKeys/all.jks",
            "ssl.server.keystore.password": "bigdata",
            "ssl.server.truststore.password": "bigdata",
            "ssl.server.truststore.type": "jks",
            "ssl.server.keystore.type": "jks",
             "ssl.server.truststore.reload.interval": "10000"
        }, 
        "ranger-site": {}, 
        },
        "ranger-site": {},
         "admin-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#      http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n#\n\n\nlog4j.rootLogger = warn,xa_log_appender\n\n\n# xa_logger\nlog4j.appender.xa_log_appender=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.xa_log_appender.file=${logdir}/xa_portal.log\nlog4j.appender.xa_log_appender.datePattern='.'yyyy-MM-dd\nlog4j.appender.xa_log_appender.append=true\nlog4j.appender.xa_log_appender.layout=org.apache.log4j.PatternLayout\nlog4j.appender.xa_log_appender.layout.ConversionPattern=%d [%t] %-5p %C{6} (%F:%L) - %m%n\n# xa_log_appender : category and additivity\nlog4j.category.org.springframework=warn,xa_log_appender\nlog4j.additivity.org.springframework=false\n\nlog4j.category.org.apache.ranger=info,xa_log_appender\nlog4j.additivity.org.apache.ranger=false\n\nlog4j.category.xa=info,xa_log_appender\nlog4j.additivity.xa=false\n\n# perf_logger\nlog4j.appender.perf_appender=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.perf_appender.file=${logdir}/ranger_admin_perf.log\nlog4j.appender.perf_appender.datePattern='.'yyyy-MM-dd\nlog4j.appender.perf_appender.append=true\nlog4j.appender.perf_appender.layout=org.apache.log4j.PatternLayout\nlog4j.appender.perf_appender.layout.ConversionPattern=%d [%t] %m%n\n\n\n# sql_appender\nlog4j.appender.sql_appender=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.sql_appender.file=${logdir}/xa_portal_sql.log\nlog4j.appender.sql_appender.datePattern='.'yyyy-MM-dd\nlog4j.appender.sql_appender.append=true\nlog4j.appender.sql_appender.layout=org.apache.log4j.PatternLayout\nlog4j.appender.sql_appender.layout.ConversionPattern=%d [%t] %-5p %C{6} (%F:%L) - %m%n\n\n# sql_appender : category and additivity\nlog4j.category.org.hibernate.SQL=warn,sql_appender\nlog4j.additivity.org.hibernate.SQL=false\n\nlog4j.category.jdbc.sqlonly=fatal,sql_appender\nlog4j.additivity.jdbc.sqlonly=false\n\nlog4j.category.jdbc.sqltiming=warn,sql_appender\nlog4j.additivity.jdbc.sqltiming=false\n\nlog4j.category.jdbc.audit=fatal,sql_appender\nlog4j.additivity.jdbc.audit=false\n\nlog4j.category.jdbc.resultset=fatal,sql_appender\nlog4j.additivity.jdbc.resultset=false\n\nlog4j.category.jdbc.connection=fatal,sql_appender\nlog4j.additivity.jdbc.connection=false"
        }, 
        },
         "tagsync-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#      http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n#\n\n\nlog4j.rootLogger = info,logFile\n\n# logFile\nlog4j.appender.logFile=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.logFile.file=${logdir}/tagsync.log\nlog4j.appender.logFile.datePattern='.'yyyy-MM-dd\nlog4j.appender.logFile.layout=org.apache.log4j.PatternLayout\nlog4j.appender.logFile.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %L %m%n\n\n# console\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.Target=System.out\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %L %m%n"
        }, 
        },
         "ranger-hdfs-security": {
            "ranger.plugin.hdfs.policy.source.impl": "org.apache.ranger.admin.client.RangerAdminRESTClient", 
            "ranger.plugin.hdfs.service.name": "{{repo_name}}", 
            "ranger.plugin.hdfs.policy.cache.dir": "/etc/ranger/{{repo_name}}/policycache", 
            "ranger.plugin.hdfs.policy.pollIntervalMs": "30000", 
            "ranger.plugin.hdfs.policy.rest.url": "{{policymgr_mgr_url}}", 
            "ranger.plugin.hdfs.policy.rest.ssl.config.file": "/etc/hadoop/conf/ranger-policymgr-ssl.xml", 
            "ranger.plugin.hdfs.policy.source.impl": "org.apache.ranger.admin.client.RangerAdminRESTClient",
            "ranger.plugin.hdfs.service.name": "{{repo_name}}",
            "ranger.plugin.hdfs.policy.cache.dir": "/etc/ranger/{{repo_name}}/policycache",
            "ranger.plugin.hdfs.policy.pollIntervalMs": "30000",
            "ranger.plugin.hdfs.policy.rest.url": "{{policymgr_mgr_url}}",
            "ranger.plugin.hdfs.policy.rest.ssl.config.file": "/etc/hadoop/conf/ranger-policymgr-ssl.xml",
             "xasecure.add-hadoop-authorization": "true"
        }, 
        "usersync-properties": {}, 
        },
        "usersync-properties": {},
         "zookeeper-env": {
            "zk_log_dir": "/var/log/zookeeper", 
            "content": "\nexport JAVA_HOME={{java64_home}}\nexport ZOOKEEPER_HOME={{zk_home}}\nexport ZOO_LOG_DIR={{zk_log_dir}}\nexport ZOOPIDFILE={{zk_pid_file}}\nexport SERVER_JVMFLAGS={{zk_server_heapsize}}\nexport JAVA=$JAVA_HOME/bin/java\nexport CLASSPATH=$CLASSPATH:/usr/share/zookeeper/*\n\n{% if security_enabled %}\nexport SERVER_JVMFLAGS=\"$SERVER_JVMFLAGS -Djava.security.auth.login.config={{zk_server_jaas_file}}\"\nexport CLIENT_JVMFLAGS=\"$CLIENT_JVMFLAGS -Djava.security.auth.login.config={{zk_client_jaas_file}}\"\n{% endif %}", 
            "zk_server_heapsize": "1024m", 
            "zk_pid_dir": "/var/run/zookeeper", 
            "zk_log_dir": "/var/log/zookeeper",
            "content": "\nexport JAVA_HOME={{java64_home}}\nexport ZOOKEEPER_HOME={{zk_home}}\nexport ZOO_LOG_DIR={{zk_log_dir}}\nexport ZOOPIDFILE={{zk_pid_file}}\nexport SERVER_JVMFLAGS={{zk_server_heapsize}}\nexport JAVA=$JAVA_HOME/bin/java\nexport CLASSPATH=$CLASSPATH:/usr/share/zookeeper/*\n\n{% if security_enabled %}\nexport SERVER_JVMFLAGS=\"$SERVER_JVMFLAGS -Djava.security.auth.login.config={{zk_server_jaas_file}}\"\nexport CLIENT_JVMFLAGS=\"$CLIENT_JVMFLAGS -Djava.security.auth.login.config={{zk_client_jaas_file}}\"\n{% endif %}",
            "zk_server_heapsize": "1024m",
            "zk_pid_dir": "/var/run/zookeeper",
             "zk_user": "zookeeper"
         },
         "infra-solr-env": {
@@ -651,7 +651,7 @@
             "infra_solr_kerberos_name_rules": "DEFAULT",
             "infra_solr_user": "infra-solr",
             "infra_solr_maxmem": "1024",
            "content": "#!/bin/bash\n# Licensed to the Apache Software Foundation (ASF) under one or more\n# contributor license agreements. See the NOTICE file distributed with\n# this work for additional information regarding copyright ownership.\n# The ASF licenses this file to You under the Apache License, Version 2.0\n# (the \"License\"); you may not use this file except in compliance with\n# the License. You may obtain a copy of the License at\n#\n# http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n\n# By default the script will use JAVA_HOME to determine which java\n# to use, but you can set a specific path for Solr to use without\n# affecting other Java applications on your server/workstation.\nSOLR_JAVA_HOME={{java64_home}}\n\n# Increase Java Min/Max Heap as needed to support your indexing / query needs\nSOLR_JAVA_MEM=\"-Xms{{logsearch_solr_min_mem}}m -Xmx{{logsearch_solr_max_mem}}m\"\n\n# Enable verbose GC logging\nGC_LOG_OPTS=\"-verbose:gc -XX:+PrintHeapAtGC -XX:+PrintGCDetails \\\n-XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationStoppedTime\"\n\n# These GC settings have shown to work well for a number of common Solr workloads\nGC_TUNE=\"-XX:NewRatio=3 \\\n-XX:SurvivorRatio=4 \\\n-XX:TargetSurvivorRatio=90 \\\n-XX:MaxTenuringThreshold=8 \\\n-XX:+UseConcMarkSweepGC \\\n-XX:+UseParNewGC \\\n-XX:ConcGCThreads=4 -XX:ParallelGCThreads=4 \\\n-XX:+CMSScavengeBeforeRemark \\\n-XX:PretenureSizeThreshold=64m \\\n-XX:+UseCMSInitiatingOccupancyOnly \\\n-XX:CMSInitiatingOccupancyFraction=50 \\\n-XX:CMSMaxAbortablePrecleanTime=6000 \\\n-XX:+CMSParallelRemarkEnabled \\\n-XX:+ParallelRefProcEnabled\"\n\n# Set the ZooKeeper connection string if using an external ZooKeeper ensemble\n# e.g. host1:2181,host2:2181/chroot\n# Leave empty if not using SolrCloud\nZK_HOST=\"{{zookeeper_quorum}}{{logsearch_solr_znode}}\"\n\n# Set the ZooKeeper client timeout (for SolrCloud mode)\nZK_CLIENT_TIMEOUT=\"60000\"\n\n# By default the start script uses \"localhost\"; override the hostname here\n# for production SolrCloud environments to control the hostname exposed to cluster state\n#SOLR_HOST=\"192.168.1.1\"\n\n# By default the start script uses UTC; override the timezone if needed\n#SOLR_TIMEZONE=\"UTC\"\n\n# Set to true to activate the JMX RMI connector to allow remote JMX client applications\n# to monitor the JVM hosting Solr; set to \"false\" to disable that behavior\n# (false is recommended in production environments)\nENABLE_REMOTE_JMX_OPTS=\"true\"\n\n# The script will use SOLR_PORT+10000 for the RMI_PORT or you can set it here\nRMI_PORT={{logsearch_solr_jmx_port}}\n\n# Anything you add to the SOLR_OPTS variable will be included in the java\n# start command line as-is, in ADDITION to other options. If you specify the\n# -a option on start script, those options will be appended as well. Examples:\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.autoSoftCommit.maxTime=3000\"\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.autoCommit.maxTime=60000\"\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.clustering.enabled=true\"\n\n# Location where the bin/solr script will save PID files for running instances\n# If not set, the script will create PID files in $SOLR_TIP/bin\nSOLR_PID_DIR={{logsearch_solr_piddir}}\n\n# Path to a directory where Solr creates index files, the specified directory\n# must contain a solr.xml; by default, Solr will use server/solr\nSOLR_HOME={{logsearch_solr_datadir}}\n\n# Solr provides a default Log4J configuration properties file in server/resources\n# however, you may want to customize the log settings and file appender location\n# so you can point the script to use a different log4j.properties file\nLOG4J_PROPS={{logsearch_solr_conf}}/log4j.properties\n\n# Location where Solr should write logs to; should agree with the file appender\n# settings in server/resources/log4j.properties\nSOLR_LOGS_DIR={{logsearch_solr_log_dir}}\n\n# Sets the port Solr binds to, default is 8983\nSOLR_PORT={{logsearch_solr_port}}\n\n# Be sure to update the paths to the correct keystore for your environment\n{% if logsearch_solr_ssl_enabled %}\nSOLR_SSL_KEY_STORE={{logsearch_solr_keystore_location}}\nSOLR_SSL_KEY_STORE_PASSWORD={{logsearch_solr_keystore_password}}\nSOLR_SSL_TRUST_STORE={{logsearch_solr_keystore_location}}\nSOLR_SSL_TRUST_STORE_PASSWORD={{logsearch_solr_keystore_password}}\nSOLR_SSL_NEED_CLIENT_AUTH=false\nSOLR_SSL_WANT_CLIENT_AUTH=false\n{% endif %}\n\n# Uncomment to set a specific SSL port (-Djetty.ssl.port=N); if not set\n# and you are using SSL, then the start script will use SOLR_PORT for the SSL port\n#SOLR_SSL_PORT=\n\n{% if security_enabled -%}\nSOLR_HOST=`hostname -f`\nSOLR_JAAS_FILE={{logsearch_solr_jaas_file}}\nSOLR_KERB_KEYTAB={{logsearch_solr_web_kerberos_keytab}}\nSOLR_KERB_PRINCIPAL={{logsearch_solr_web_kerberos_principal}}\nSOLR_KERB_NAME_RULES={{logsearch_solr_kerberos_name_rules}}\n\nSOLR_AUTHENTICATION_CLIENT_CONFIGURER=\"org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer\"\nSOLR_AUTHENTICATION_OPTS=\" -DauthenticationPlugin=org.apache.solr.security.KerberosPlugin -Djava.security.auth.login.config=$SOLR_JAAS_FILE -Dsolr.kerberos.principal=${SOLR_KERB_PRINCIPAL} -Dsolr.kerberos.keytab=${SOLR_KERB_KEYTAB} -Dsolr.kerberos.cookie.domain=${SOLR_HOST} -Dsolr.kerberos.name.rules=${SOLR_KERB_NAME_RULES}\"\n{% endif %}", 
            "content": "#!/bin/bash\n# Licensed to the Apache Software Foundation (ASF) under one or more\n# contributor license agreements. See the NOTICE file distributed with\n# this work for additional information regarding copyright ownership.\n# The ASF licenses this file to You under the Apache License, Version 2.0\n# (the \"License\"); you may not use this file except in compliance with\n# the License. You may obtain a copy of the License at\n#\n# http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n\n# By default the script will use JAVA_HOME to determine which java\n# to use, but you can set a specific path for Solr to use without\n# affecting other Java applications on your server/workstation.\nSOLR_JAVA_HOME={{java64_home}}\n\n# Increase Java Min/Max Heap as needed to support your indexing / query needs\nSOLR_JAVA_MEM=\"-Xms{{logsearch_solr_min_mem}}m -Xmx{{logsearch_solr_max_mem}}m\"\n\n# Enable verbose GC logging\nGC_LOG_OPTS=\"-verbose:gc -XX:+PrintHeapAtGC -XX:+PrintGCDetails \\\n-XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationStoppedTime\"\n\n# These GC settings have shown to work well for a number of common Solr workloads\nGC_TUNE=\"-XX:NewRatio=3 \\\n-XX:SurvivorRatio=4 \\\n-XX:TargetSurvivorRatio=90 \\\n-XX:MaxTenuringThreshold=8 \\\n-XX:+UseConcMarkSweepGC \\\n-XX:+UseParNewGC \\\n-XX:ConcGCThreads=4 -XX:ParallelGCThreads=4 \\\n-XX:+CMSScavengeBeforeRemark \\\n-XX:PretenureSizeThreshold=64m \\\n-XX:+UseCMSInitiatingOccupancyOnly \\\n-XX:CMSInitiatingOccupancyFraction=50 \\\n-XX:CMSMaxAbortablePrecleanTime=6000 \\\n-XX:+CMSParallelRemarkEnabled \\\n-XX:+ParallelRefProcEnabled\"\n\n# Set the ZooKeeper connection string if using an external ZooKeeper ensemble\n# e.g. host1:2181,host2:2181/chroot\n# Leave empty if not using SolrCloud\nZK_HOST=\"{{zookeeper_quorum}}{{logsearch_solr_znode}}\"\n\n# Set the ZooKeeper client timeout (for SolrCloud mode)\nZK_CLIENT_TIMEOUT=\"60000\"\n\n# By default the start script uses \"localhost\"; override the hostname here\n# for production SolrCloud environments to control the hostname exposed to cluster state\n#SOLR_HOST=\"192.168.1.1\"\n\n# By default the start script uses UTC; override the timezone if needed\n#SOLR_TIMEZONE=\"UTC\"\n\n# Set to true to activate the JMX RMI connector to allow remote JMX client applications\n# to monitor the JVM hosting Solr; set to \"false\" to disable that behavior\n# (false is recommended in production environments)\nENABLE_REMOTE_JMX_OPTS=\"true\"\n\n# The script will use SOLR_PORT+10000 for the RMI_PORT or you can set it here\nRMI_PORT={{logsearch_solr_jmx_port}}\n\n# Anything you add to the SOLR_OPTS variable will be included in the java\n# start command line as-is, in ADDITION to other options. If you specify the\n# -a option on start script, those options will be appended as well. Examples:\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.autoSoftCommit.maxTime=3000\"\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.autoCommit.maxTime=60000\"\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.clustering.enabled=true\"\n\n# Location where the bin/solr script will save PID files for running instances\n# If not set, the script will create PID files in $SOLR_TIP/bin\nSOLR_PID_DIR={{logsearch_solr_piddir}}\n\n# Path to a directory where Solr creates index files, the specified directory\n# must contain a solr.xml; by default, Solr will use server/solr\nSOLR_HOME={{logsearch_solr_datadir}}\n\n# Solr provides a default Log4J configuration properties file in server/resources\n# however, you may want to customize the log settings and file appender location\n# so you can point the script to use a different log4j.properties file\nLOG4J_PROPS={{logsearch_solr_conf}}/log4j.properties\n\n# Location where Solr should write logs to; should agree with the file appender\n# settings in server/resources/log4j.properties\nSOLR_LOGS_DIR={{logsearch_solr_log_dir}}\n\n# Sets the port Solr binds to, default is 8983\nSOLR_PORT={{logsearch_solr_port}}\n\n# Be sure to update the paths to the correct keystore for your environment\n{% if logsearch_solr_ssl_enabled %}\nSOLR_SSL_KEY_STORE={{logsearch_solr_keystore_location}}\nSOLR_SSL_KEY_STORE_PASSWORD={{logsearch_solr_keystore_password}}\nSOLR_SSL_TRUST_STORE={{logsearch_solr_keystore_location}}\nSOLR_SSL_TRUST_STORE_PASSWORD={{logsearch_solr_keystore_password}}\nSOLR_SSL_NEED_CLIENT_AUTH=false\nSOLR_SSL_WANT_CLIENT_AUTH=false\n{% endif %}\n\n# Uncomment to set a specific SSL port (-Djetty.ssl.port=N); if not set\n# and you are using SSL, then the start script will use SOLR_PORT for the SSL port\n#SOLR_SSL_PORT=\n\n{% if security_enabled -%}\nSOLR_HOST=`hostname -f`\nSOLR_JAAS_FILE={{logsearch_solr_jaas_file}}\nSOLR_KERB_KEYTAB={{logsearch_solr_web_kerberos_keytab}}\nSOLR_KERB_PRINCIPAL={{logsearch_solr_web_kerberos_principal}}\nSOLR_KERB_NAME_RULES={{logsearch_solr_kerberos_name_rules}}\n\nSOLR_AUTHENTICATION_CLIENT_CONFIGURER=\"org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer\"\nSOLR_AUTHENTICATION_OPTS=\" -DauthenticationPlugin=org.apache.solr.security.KerberosPlugin -Djava.security.auth.login.config=$SOLR_JAAS_FILE -Dsolr.kerberos.principal=${SOLR_KERB_PRINCIPAL} -Dsolr.kerberos.keytab=${SOLR_KERB_KEYTAB} -Dsolr.kerberos.cookie.domain=${SOLR_HOST} -Dsolr.kerberos.name.rules=${SOLR_KERB_NAME_RULES}\"\n{% endif %}",
             "infra_solr_pid_dir": "/var/run/ambari-infra-solr",
             "infra_solr_truststore_password": "bigdata",
             "infra_solr_truststore_type": "jks",
@@ -675,30 +675,30 @@
             "content": "content"
         },
         "cluster-env": {
            "security_enabled": "false", 
            "override_uid": "true", 
            "fetch_nonlocal_groups": "true", 
            "one_dir_per_partition": "true", 
            "repo_ubuntu_template": "{{package_type}} {{base_url}} {{components}}", 
            "ignore_groupsusers_create": "false", 
            "alerts_repeat_tolerance": "1", 
            "smokeuser_keytab": "/etc/security/keytabs/smokeuser.headless.keytab", 
            "kerberos_domain": "EXAMPLE.COM", 
            "security_enabled": "false",
            "override_uid": "true",
            "fetch_nonlocal_groups": "true",
            "one_dir_per_partition": "true",
            "repo_ubuntu_template": "{{package_type}} {{base_url}} {{components}}",
            "ignore_groupsusers_create": "false",
            "alerts_repeat_tolerance": "1",
            "smokeuser_keytab": "/etc/security/keytabs/smokeuser.headless.keytab",
            "kerberos_domain": "EXAMPLE.COM",
             "manage_dirs_on_root": "true",
            "recovery_lifetime_max_count": "1024", 
            "recovery_type": "AUTO_START", 
            "ignore_bad_mounts": "false", 
            "recovery_window_in_minutes": "60", 
            "user_group": "hadoop", 
            "stack_tools": "{\n  \"stack_selector\": [\"hdp-select\", \"/usr/bin/hdp-select\", \"hdp-select\"],\n  \"conf_selector\": [\"conf-select\", \"/usr/bin/conf-select\", \"conf-select\"]\n}", 
            "recovery_retry_interval": "5", 
            "stack_features": "{\n  \"stack_features\": [\n    {\n      \"name\": \"snappy\",\n      \"description\": \"Snappy compressor/decompressor support\",\n      \"min_version\": \"2.0.0.0\",\n      \"max_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"lzo\",\n      \"description\": \"LZO libraries support\",\n      \"min_version\": \"2.2.1.0\"\n    },\n    {\n      \"name\": \"express_upgrade\",\n      \"description\": \"Express upgrade support\",\n      \"min_version\": \"2.1.0.0\"\n    },\n    {\n      \"name\": \"rolling_upgrade\",\n      \"description\": \"Rolling upgrade support\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"config_versioning\",\n      \"description\": \"Configurable versions support\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"datanode_non_root\",\n      \"description\": \"DataNode running as non-root support (AMBARI-7615)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"remove_ranger_hdfs_plugin_env\",\n      \"description\": \"HDFS removes Ranger env files (AMBARI-14299)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"ranger\",\n      \"description\": \"Ranger Service support\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"ranger_tagsync_component\",\n      \"description\": \"Ranger Tagsync component support (AMBARI-14383)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"phoenix\",\n      \"description\": \"Phoenix Service support\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"nfs\",\n      \"description\": \"NFS support\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"tez_for_spark\",\n      \"description\": \"Tez dependency for Spark\",\n      \"min_version\": \"2.2.0.0\",\n      \"max_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"timeline_state_store\",\n      \"description\": \"Yarn application timeline-service supports state store property (AMBARI-11442)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"copy_tarball_to_hdfs\",\n      \"description\": \"Copy tarball to HDFS support (AMBARI-12113)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"spark_16plus\",\n      \"description\": \"Spark 1.6+\",\n      \"min_version\": \"2.4.0.0\"\n    },\n    {\n      \"name\": \"spark_thriftserver\",\n      \"description\": \"Spark Thrift Server\",\n      \"min_version\": \"2.3.2.0\"\n    },\n    {\n      \"name\": \"storm_kerberos\",\n      \"description\": \"Storm Kerberos support (AMBARI-7570)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"storm_ams\",\n      \"description\": \"Storm AMS integration (AMBARI-10710)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"create_kafka_broker_id\",\n      \"description\": \"Ambari should create Kafka Broker Id (AMBARI-12678)\",\n      \"min_version\": \"2.2.0.0\",\n      \"max_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"kafka_listeners\",\n      \"description\": \"Kafka listeners (AMBARI-10984)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"kafka_kerberos\",\n      \"description\": \"Kafka Kerberos support (AMBARI-10984)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"pig_on_tez\",\n      \"description\": \"Pig on Tez support (AMBARI-7863)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"ranger_usersync_non_root\",\n      \"description\": \"Ranger Usersync as non-root user (AMBARI-10416)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"ranger_audit_db_support\",\n      \"description\": \"Ranger Audit to DB support\",\n      \"min_version\": \"2.2.0.0\",\n      \"max_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"accumulo_kerberos_user_auth\",\n      \"description\": \"Accumulo Kerberos User Auth (AMBARI-10163)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"knox_versioned_data_dir\",\n      \"description\": \"Use versioned data dir for Knox (AMBARI-13164)\",\n      \"min_version\": \"2.3.2.0\"\n    },\n    {\n      \"name\": \"knox_sso_topology\",\n      \"description\": \"Knox SSO Topology support (AMBARI-13975)\",\n      \"min_version\": \"2.3.8.0\"\n    },\n    {\n      \"name\": \"atlas_rolling_upgrade\",\n      \"description\": \"Rolling upgrade support for Atlas\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"oozie_admin_user\",\n      \"description\": \"Oozie install user as an Oozie admin user (AMBARI-7976)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"oozie_create_hive_tez_configs\",\n      \"description\": \"Oozie create configs for Ambari Hive and Tez deployments (AMBARI-8074)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"oozie_setup_shared_lib\",\n      \"description\": \"Oozie setup tools used to shared Oozie lib to HDFS (AMBARI-7240)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"oozie_host_kerberos\",\n      \"description\": \"Oozie in secured clusters uses _HOST in Kerberos principal (AMBARI-9775)\",\n      \"min_version\": \"2.0.0.0\",\n      \"max_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"falcon_extensions\",\n      \"description\": \"Falcon Extension\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hive_metastore_upgrade_schema\",\n      \"description\": \"Hive metastore upgrade schema support (AMBARI-11176)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"hive_server_interactive\",\n      \"description\": \"Hive server interactive support (AMBARI-15573)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hive_webhcat_specific_configs\",\n      \"description\": \"Hive webhcat specific configurations support (AMBARI-12364)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"hive_purge_table\",\n      \"description\": \"Hive purge table support (AMBARI-12260)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"hive_server2_kerberized_env\",\n      \"description\": \"Hive server2 working on kerberized environment (AMBARI-13749)\",\n      \"min_version\": \"2.2.3.0\",\n      \"max_version\": \"2.2.5.0\"\n    },\n    {\n      \"name\": \"hive_env_heapsize\",\n      \"description\": \"Hive heapsize property defined in hive-env (AMBARI-12801)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"ranger_kms_hsm_support\",\n      \"description\": \"Ranger KMS HSM support (AMBARI-15752)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_log4j_support\",\n      \"description\": \"Ranger supporting log-4j properties (AMBARI-15681)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_kerberos_support\",\n      \"description\": \"Ranger Kerberos support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hive_metastore_site_support\",\n      \"description\": \"Hive Metastore site support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_usersync_password_jceks\",\n      \"description\": \"Saving Ranger Usersync credentials in jceks\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_install_infra_client\",\n      \"description\": \"LogSearch Service support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hbase_home_directory\",\n      \"description\": \"Hbase home directory in HDFS needed for HBASE backup\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"spark_livy\",\n      \"description\": \"Livy as slave component of spark\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"atlas_ranger_plugin_support\",\n      \"description\": \"Atlas Ranger plugin support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_pid_support\",\n      \"description\": \"Ranger Service support pid generation AMBARI-16756\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_kms_pid_support\",\n      \"description\": \"Ranger KMS Service support pid generation\",\n      \"min_version\": \"2.5.0.0\"\n    }\n  ]\n}",
            "recovery_enabled": "true", 
            "recovery_max_count": "6", 
            "stack_root": "/usr/hdp", 
            "repo_suse_rhel_template": "[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0", 
            "managed_hdfs_resource_property_names": "", 
            "recovery_lifetime_max_count": "1024",
            "recovery_type": "AUTO_START",
            "ignore_bad_mounts": "false",
            "recovery_window_in_minutes": "60",
            "user_group": "hadoop",
            "stack_name": "HDP",
            "stack_root": "{\"HDP\": \"/usr/hdp\"}",
            "stack_tools": "{\n \"HDP\": { \"stack_selector\": [\"hdp-select\", \"/usr/bin/hdp-select\", \"hdp-select\"],\n  \"conf_selector\": [\"conf-select\", \"/usr/bin/conf-select\", \"conf-select\"]\n}\n}",
            "recovery_retry_interval": "5",
            "recovery_enabled": "true",
            "recovery_max_count": "6",
            "repo_suse_rhel_template": "[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0",
            "managed_hdfs_resource_property_names": "",
             "smokeuser": "ambari-qa"
         }
     }
}
\ No newline at end of file
}
diff --git a/ambari-server/src/test/python/stacks/2.5/configs/ranger-admin-secured.json b/ambari-server/src/test/python/stacks/2.5/configs/ranger-admin-secured.json
index f959b1fd5d..7f1e5494d3 100644
-- a/ambari-server/src/test/python/stacks/2.5/configs/ranger-admin-secured.json
++ b/ambari-server/src/test/python/stacks/2.5/configs/ranger-admin-secured.json
@@ -1,159 +1,159 @@
 {
     "localComponents": [
        "SECONDARY_NAMENODE", 
        "HDFS_CLIENT", 
        "DATANODE", 
        "NAMENODE", 
        "RANGER_ADMIN", 
        "RANGER_TAGSYNC", 
        "RANGER_USERSYNC", 
        "ZOOKEEPER_SERVER", 
        "ZOOKEEPER_CLIENT", 
        "SECONDARY_NAMENODE",
        "HDFS_CLIENT",
        "DATANODE",
        "NAMENODE",
        "RANGER_ADMIN",
        "RANGER_TAGSYNC",
        "RANGER_USERSYNC",
        "ZOOKEEPER_SERVER",
        "ZOOKEEPER_CLIENT",
         "KERBEROS_CLIENT",
         "LOGSEARCH_SOLR",
         "LOGSEARCH_SOLR_CLIENT"
    ], 
    ],
     "configuration_attributes": {
        "ranger-hdfs-audit": {}, 
        "ssl-client": {}, 
        "ranger-admin-site": {}, 
        "ranger-hdfs-policymgr-ssl": {}, 
        "tagsync-application-properties": {}, 
        "ranger-env": {}, 
        "usersync-log4j": {}, 
        "ranger-hdfs-plugin-properties": {}, 
        "kerberos-env": {}, 
        "admin-properties": {}, 
        "ranger-ugsync-site": {}, 
        "ranger-hdfs-audit": {},
        "ssl-client": {},
        "ranger-admin-site": {},
        "ranger-hdfs-policymgr-ssl": {},
        "tagsync-application-properties": {},
        "ranger-env": {},
        "usersync-log4j": {},
        "ranger-hdfs-plugin-properties": {},
        "kerberos-env": {},
        "admin-properties": {},
        "ranger-ugsync-site": {},
         "hdfs-site": {
             "final": {
                "dfs.datanode.data.dir": "true", 
                "dfs.namenode.http-address": "true", 
                "dfs.datanode.failed.volumes.tolerated": "true", 
                "dfs.support.append": "true", 
                "dfs.namenode.name.dir": "true", 
                "dfs.datanode.data.dir": "true",
                "dfs.namenode.http-address": "true",
                "dfs.datanode.failed.volumes.tolerated": "true",
                "dfs.support.append": "true",
                "dfs.namenode.name.dir": "true",
                 "dfs.webhdfs.enabled": "true"
             }
        }, 
        "ranger-tagsync-site": {}, 
        "zoo.cfg": {}, 
        "hadoop-policy": {}, 
        "hdfs-log4j": {}, 
        "krb5-conf": {}, 
        },
        "ranger-tagsync-site": {},
        "zoo.cfg": {},
        "hadoop-policy": {},
        "hdfs-log4j": {},
        "krb5-conf": {},
         "core-site": {
             "final": {
                 "fs.defaultFS": "true"
             }
        }, 
        "hadoop-env": {}, 
        "zookeeper-log4j": {}, 
        "ssl-server": {}, 
        "ranger-site": {}, 
        "admin-log4j": {}, 
        "tagsync-log4j": {}, 
        "ranger-hdfs-security": {}, 
        "usersync-properties": {}, 
        },
        "hadoop-env": {},
        "zookeeper-log4j": {},
        "ssl-server": {},
        "ranger-site": {},
        "admin-log4j": {},
        "tagsync-log4j": {},
        "ranger-hdfs-security": {},
        "usersync-properties": {},
         "zookeeper-env": {},
         "infra-solr-env": {},
         "infra-solr-client-log4j": {},
         "cluster-env": {}
    }, 
    "public_hostname": "c6401.ambari.apache.org", 
    "commandId": "41-2", 
    "hostname": "c6401.ambari.apache.org", 
    "kerberosCommandParams": [], 
    "serviceName": "RANGER", 
    "role": "RANGER_ADMIN", 
    "forceRefreshConfigTagsBeforeExecution": [], 
    "requestId": 41, 
    },
    "public_hostname": "c6401.ambari.apache.org",
    "commandId": "41-2",
    "hostname": "c6401.ambari.apache.org",
    "kerberosCommandParams": [],
    "serviceName": "RANGER",
    "role": "RANGER_ADMIN",
    "forceRefreshConfigTagsBeforeExecution": [],
    "requestId": 41,
     "agentConfigParams": {
         "agent": {
             "parallel_execution": 0
         }
    }, 
    "clusterName": "test_Cluster01", 
    "commandType": "EXECUTION_COMMAND", 
    "taskId": 186, 
    "roleParams": {}, 
    },
    "clusterName": "test_Cluster01",
    "commandType": "EXECUTION_COMMAND",
    "taskId": 186,
    "roleParams": {},
     "configurationTags": {
         "ranger-hdfs-audit": {
             "tag": "version1466705299922"
        }, 
        },
         "ssl-client": {
             "tag": "version1"
        }, 
        },
         "ranger-admin-site": {
             "tag": "version1467016680635"
        }, 
        },
         "ranger-hdfs-policymgr-ssl": {
             "tag": "version1466705299922"
        }, 
        },
         "tagsync-application-properties": {
             "tag": "version1467016680511"
        }, 
        },
         "ranger-env": {
             "tag": "version1466705299949"
        }, 
        },
         "ranger-ugsync-site": {
             "tag": "version1467016680537"
        }, 
        },
         "ranger-hdfs-plugin-properties": {
             "tag": "version1466705299922"
        }, 
        },
         "kerberos-env": {
             "tag": "version1467016537243"
        }, 
        },
         "admin-properties": {
             "tag": "version1466705299949"
        }, 
        },
         "hdfs-site": {
             "tag": "version1467016680401"
        }, 
        },
         "ranger-tagsync-site": {
             "tag": "version1467016680586"
        }, 
        },
         "zoo.cfg": {
             "tag": "version1"
        }, 
        },
         "hadoop-policy": {
             "tag": "version1"
        }, 
        },
         "hdfs-log4j": {
             "tag": "version1"
        }, 
        },
         "usersync-log4j": {
             "tag": "version1466705299949"
        }, 
        },
         "krb5-conf": {
             "tag": "version1467016537243"
        }, 
        },
         "core-site": {
             "tag": "version1467016680612"
        }, 
        },
         "hadoop-env": {
             "tag": "version1467016680446"
        }, 
        },
         "zookeeper-log4j": {
             "tag": "version1"
        }, 
        },
         "ssl-server": {
             "tag": "version1"
        }, 
        },
         "ranger-site": {
             "tag": "version1466705299949"
        }, 
        },
         "admin-log4j": {
             "tag": "version1466705299949"
        }, 
        },
         "tagsync-log4j": {
             "tag": "version1466705299949"
        }, 
        },
         "ranger-hdfs-security": {
             "tag": "version1466705299922"
        }, 
        },
         "usersync-properties": {
             "tag": "version1466705299949"
        }, 
        },
         "zookeeper-env": {
             "tag": "version1467016680492"
         },
@@ -166,550 +166,550 @@
         "cluster-env": {
             "tag": "version1467016680567"
         }
    }, 
    "roleCommand": "START", 
    },
    "roleCommand": "START",
     "hostLevelParams": {
        "agent_stack_retry_on_unavailability": "false", 
        "stack_name": "HDP", 
        "package_version": "2_5_0_0_*", 
        "agent_stack_retry_on_unavailability": "false",
        "stack_name": "HDP",
        "package_version": "2_5_0_0_*",
         "custom_mysql_jdbc_name": "mysql-connector-java.jar",
         "previous_custom_mysql_jdbc_name": "mysql-connector-java-old.jar",
        "host_sys_prepped": "false", 
        "ambari_db_rca_username": "mapred", 
        "current_version": "2.5.0.0-801", 
        "mysql_jdbc_url": "http://c6401.ambari.apache.org:8080/resources//mysql-connector-java.jar", 
        "agent_stack_retry_count": "5", 
        "stack_version": "2.5", 
        "jdk_name": "jdk-8u60-linux-x64.tar.gz", 
        "ambari_db_rca_driver": "org.postgresql.Driver", 
        "host_sys_prepped": "false",
        "ambari_db_rca_username": "mapred",
        "current_version": "2.5.0.0-801",
        "mysql_jdbc_url": "http://c6401.ambari.apache.org:8080/resources//mysql-connector-java.jar",
        "agent_stack_retry_count": "5",
        "stack_version": "2.5",
        "jdk_name": "jdk-8u60-linux-x64.tar.gz",
        "ambari_db_rca_driver": "org.postgresql.Driver",
         "java_home": "/usr/jdk64/jdk1.7.0_45",
        "repository_version_id": "1", 
        "jdk_location": "http://c6401.ambari.apache.org:8080/resources/", 
        "not_managed_hdfs_path_list": "[\"/tmp\"]", 
        "ambari_db_rca_url": "jdbc:postgresql://c6401.ambari.apache.org/ambarirca", 
        "java_version": "8", 
        "repo_info": "[{\"baseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.5.0.0-801\",\"osType\":\"redhat6\",\"repoId\":\"HDP-2.5\",\"repoName\":\"HDP\",\"defaultBaseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/updates/2.5.0.0\",\"latestBaseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.5.0.0-801\",\"baseSaved\":true},{\"baseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"osType\":\"redhat6\",\"repoId\":\"HDP-UTILS-1.1.0.21\",\"repoName\":\"HDP-UTILS\",\"defaultBaseUrl\":\"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"latestBaseUrl\":\"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"baseSaved\":true}]", 
        "package_list": "[{\"name\":\"ranger_${stack_version}-admin\",\"condition\":\"\",\"skipUpgrade\":false},{\"name\":\"ranger_${stack_version}-usersync\",\"condition\":\"\",\"skipUpgrade\":false},{\"name\":\"ranger_${stack_version}-tagsync\",\"condition\":\"should_install_ranger_tagsync\",\"skipUpgrade\":false},{\"name\":\"ambari-logsearch-solr-client\",\"condition\":\"should_install_logsearch_solr_client\",\"skipUpgrade\":false}]", 
        "db_name": "ambari", 
        "group_list": "[\"ranger\",\"hadoop\",\"users\"]", 
        "agentCacheDir": "/var/lib/ambari-agent/cache", 
        "ambari_db_rca_password": "mapred", 
        "jce_name": "jce_policy-8.zip", 
        "oracle_jdbc_url": "http://c6401.ambari.apache.org:8080/resources//ojdbc6.jar", 
        "db_driver_filename": "mysql-connector-java.jar", 
        "user_list": "[\"zookeeper\",\"ambari-qa\",\"ranger\",\"hdfs\"]", 
        "repository_version_id": "1",
        "jdk_location": "http://c6401.ambari.apache.org:8080/resources/",
        "not_managed_hdfs_path_list": "[\"/tmp\"]",
        "ambari_db_rca_url": "jdbc:postgresql://c6401.ambari.apache.org/ambarirca",
        "java_version": "8",
        "repo_info": "[{\"baseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.5.0.0-801\",\"osType\":\"redhat6\",\"repoId\":\"HDP-2.5\",\"repoName\":\"HDP\",\"defaultBaseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/updates/2.5.0.0\",\"latestBaseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.5.0.0-801\",\"baseSaved\":true},{\"baseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"osType\":\"redhat6\",\"repoId\":\"HDP-UTILS-1.1.0.21\",\"repoName\":\"HDP-UTILS\",\"defaultBaseUrl\":\"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"latestBaseUrl\":\"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"baseSaved\":true}]",
        "package_list": "[{\"name\":\"ranger_${stack_version}-admin\",\"condition\":\"\",\"skipUpgrade\":false},{\"name\":\"ranger_${stack_version}-usersync\",\"condition\":\"\",\"skipUpgrade\":false},{\"name\":\"ranger_${stack_version}-tagsync\",\"condition\":\"should_install_ranger_tagsync\",\"skipUpgrade\":false},{\"name\":\"ambari-logsearch-solr-client\",\"condition\":\"should_install_logsearch_solr_client\",\"skipUpgrade\":false}]",
        "db_name": "ambari",
        "group_list": "[\"ranger\",\"hadoop\",\"users\"]",
        "agentCacheDir": "/var/lib/ambari-agent/cache",
        "ambari_db_rca_password": "mapred",
        "jce_name": "jce_policy-8.zip",
        "oracle_jdbc_url": "http://c6401.ambari.apache.org:8080/resources//ojdbc6.jar",
        "db_driver_filename": "mysql-connector-java.jar",
        "user_list": "[\"zookeeper\",\"ambari-qa\",\"ranger\",\"hdfs\"]",
         "clientsToUpdateConfigs": "[\"*\"]"
    }, 
    },
     "commandParams": {
        "service_package_folder": "common-services/RANGER/0.4.0/package", 
        "script": "scripts/ranger_admin.py", 
        "hooks_folder": "HDP/2.0.6/hooks", 
        "version": "2.5.0.0-801", 
        "max_duration_for_retries": "0", 
        "command_retry_enabled": "false", 
        "command_timeout": "600", 
        "service_package_folder": "common-services/RANGER/0.4.0/package",
        "script": "scripts/ranger_admin.py",
        "hooks_folder": "HDP/2.0.6/hooks",
        "version": "2.5.0.0-801",
        "max_duration_for_retries": "0",
        "command_retry_enabled": "false",
        "command_timeout": "600",
         "script_type": "PYTHON"
    }, 
    "forceRefreshConfigTags": [], 
    "stageId": 2, 
    },
    "forceRefreshConfigTags": [],
    "stageId": 2,
     "clusterHostInfo": {
         "snamenode_host": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "ambari_server_use_ssl": [
             "false"
        ], 
        ],
         "all_ping_ports": [
             "8670"
        ], 
        ],
         "ranger_tagsync_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "ranger_usersync_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "all_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "slave_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "namenode_host": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "ambari_server_port": [
             "8080"
        ], 
        ],
         "ranger_admin_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "all_racks": [
             "/default-rack"
        ], 
        ],
         "all_ipv4_ips": [
             "172.22.83.73"
        ], 
        ],
         "ambari_server_host": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "zookeeper_hosts": [
             "c6401.ambari.apache.org"
         ],
         "infra_solr_hosts": [
             "c6401.ambari.apache.org"
         ]
    }, 
    },
     "configurations": {
         "ranger-hdfs-audit": {
            "xasecure.audit.destination.solr.zookeepers": "NONE", 
            "xasecure.audit.destination.solr.urls": "", 
            "xasecure.audit.destination.solr.batch.filespool.dir": "/var/log/hadoop/hdfs/audit/solr/spool", 
            "xasecure.audit.destination.solr.zookeepers": "NONE",
            "xasecure.audit.destination.solr.urls": "",
            "xasecure.audit.destination.solr.batch.filespool.dir": "/var/log/hadoop/hdfs/audit/solr/spool",
             "xasecure.audit.destination.hdfs.batch.filespool.dir": "/var/log/hadoop/hdfs/audit/hdfs/spool",
            "xasecure.audit.destination.hdfs": "true", 
            "xasecure.audit.destination.hdfs": "true",
             "xasecure.audit.destination.solr": "false",
            "xasecure.audit.provider.summary.enabled": "false", 
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit", 
            "xasecure.audit.provider.summary.enabled": "false",
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit",
             "xasecure.audit.is.enabled": "true"
        }, 
        },
         "ssl-client": {
            "ssl.client.truststore.reload.interval": "10000", 
            "ssl.client.keystore.password": "bigdata", 
            "ssl.client.truststore.type": "jks", 
            "ssl.client.keystore.location": "/etc/security/clientKeys/keystore.jks", 
            "ssl.client.truststore.location": "/etc/security/clientKeys/all.jks", 
            "ssl.client.truststore.password": "bigdata", 
            "ssl.client.truststore.reload.interval": "10000",
            "ssl.client.keystore.password": "bigdata",
            "ssl.client.truststore.type": "jks",
            "ssl.client.keystore.location": "/etc/security/clientKeys/keystore.jks",
            "ssl.client.truststore.location": "/etc/security/clientKeys/all.jks",
            "ssl.client.truststore.password": "bigdata",
             "ssl.client.keystore.type": "jks"
        }, 
        },
         "ranger-admin-site": {
             "ranger.is.solr.kerberised": "true",
            "ranger.admin.kerberos.cookie.domain": "{{ranger_host}}", 
            "ranger.kms.service.user.hdfs": "hdfs", 
            "ranger.spnego.kerberos.principal": "HTTP/_HOST@EXAMPLE.COM", 
            "ranger.ldap.ad.url": "{{ranger_ug_ldap_url}}", 
            "ranger.plugins.hive.serviceuser": "hive", 
            "ranger.lookup.kerberos.keytab": "/etc/security/keytabs/rangerlookup.service.keytab", 
            "ranger.plugins.kms.serviceuser": "kms", 
            "ranger.service.https.attrib.ssl.enabled": "false", 
            "ranger.sso.browser.useragent": "Mozilla,chrome", 
            "ranger.jpa.jdbc.url": "jdbc:mysql://c6401.ambari.apache.org:3306/ranger01", 
            "ranger.plugins.hbase.serviceuser": "hbase", 
            "ranger.plugins.hdfs.serviceuser": "hdfs", 
            "ranger.ldap.group.searchbase": "{{ranger_ug_ldap_group_searchbase}}", 
            "ranger.ldap.user.dnpattern": "uid={0},ou=users,dc=xasecure,dc=net", 
            "ranger.plugins.knox.serviceuser": "knox", 
            "ranger.ldap.base.dn": "dc=example,dc=com", 
            "ranger.sso.publicKey": "", 
            "ranger.admin.kerberos.cookie.path": "/", 
            "ranger.service.https.attrib.clientAuth": "want", 
            "ranger.jpa.jdbc.user": "{{ranger_db_user}}", 
            "ranger.ldap.ad.user.searchfilter": "(sAMAccountName={0})", 
            "ranger.ldap.group.roleattribute": "cn", 
            "ranger.plugins.kafka.serviceuser": "kafka", 
            "ranger.admin.kerberos.principal": "rangeradmin/_HOST@EXAMPLE.COM", 
            "ranger.ldap.ad.bind.dn": "{{ranger_ug_ldap_bind_dn}}", 
            "ranger.admin.kerberos.cookie.domain": "{{ranger_host}}",
            "ranger.kms.service.user.hdfs": "hdfs",
            "ranger.spnego.kerberos.principal": "HTTP/_HOST@EXAMPLE.COM",
            "ranger.ldap.ad.url": "{{ranger_ug_ldap_url}}",
            "ranger.plugins.hive.serviceuser": "hive",
            "ranger.lookup.kerberos.keytab": "/etc/security/keytabs/rangerlookup.service.keytab",
            "ranger.plugins.kms.serviceuser": "kms",
            "ranger.service.https.attrib.ssl.enabled": "false",
            "ranger.sso.browser.useragent": "Mozilla,chrome",
            "ranger.jpa.jdbc.url": "jdbc:mysql://c6401.ambari.apache.org:3306/ranger01",
            "ranger.plugins.hbase.serviceuser": "hbase",
            "ranger.plugins.hdfs.serviceuser": "hdfs",
            "ranger.ldap.group.searchbase": "{{ranger_ug_ldap_group_searchbase}}",
            "ranger.ldap.user.dnpattern": "uid={0},ou=users,dc=xasecure,dc=net",
            "ranger.plugins.knox.serviceuser": "knox",
            "ranger.ldap.base.dn": "dc=example,dc=com",
            "ranger.sso.publicKey": "",
            "ranger.admin.kerberos.cookie.path": "/",
            "ranger.service.https.attrib.clientAuth": "want",
            "ranger.jpa.jdbc.user": "{{ranger_db_user}}",
            "ranger.ldap.ad.user.searchfilter": "(sAMAccountName={0})",
            "ranger.ldap.group.roleattribute": "cn",
            "ranger.plugins.kafka.serviceuser": "kafka",
            "ranger.admin.kerberos.principal": "rangeradmin/_HOST@EXAMPLE.COM",
            "ranger.ldap.ad.bind.dn": "{{ranger_ug_ldap_bind_dn}}",
             "ranger.credential.provider.path": "/etc/ranger/admin/rangeradmin.jceks",
            "ranger.ldap.referral": "ignore", 
            "ranger.service.http.port": "6080", 
            "ranger.ldap.user.searchfilter": "(uid={0})", 
            "ranger.plugins.atlas.serviceuser": "atlas", 
            "ranger.truststore.password": "changeit", 
            "ranger.ldap.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}", 
            "ranger.audit.solr.password": "NONE", 
            "ranger.audit.solr.zookeepers": "c6401.ambari.apache.org:2181/ambari-solr", 
            "ranger.ldap.referral": "ignore",
            "ranger.service.http.port": "6080",
            "ranger.ldap.user.searchfilter": "(uid={0})",
            "ranger.plugins.atlas.serviceuser": "atlas",
            "ranger.truststore.password": "changeit",
            "ranger.ldap.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}",
            "ranger.audit.solr.password": "NONE",
            "ranger.audit.solr.zookeepers": "c6401.ambari.apache.org:2181/ambari-solr",
             "ranger.lookup.kerberos.principal": "rangerlookup/_HOST@EXAMPLE.COM",
            "ranger.service.https.port": "6182", 
            "ranger.plugins.storm.serviceuser": "storm", 
            "ranger.externalurl": "{{ranger_external_url}}", 
            "ranger.truststore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks", 
            "ranger.kms.service.user.hive": "", 
            "ranger.https.attrib.keystore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks", 
            "ranger.jpa.jdbc.dialect": "{{jdbc_dialect}}", 
            "ranger.service.host": "{{ranger_host}}", 
            "ranger.service.https.attrib.keystore.keyalias": "rangeradmin", 
            "ranger.service.https.attrib.keystore.pass": "xasecure", 
            "ranger.unixauth.remote.login.enabled": "true", 
            "ranger.service.https.port": "6182",
            "ranger.plugins.storm.serviceuser": "storm",
            "ranger.externalurl": "{{ranger_external_url}}",
            "ranger.truststore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks",
            "ranger.kms.service.user.hive": "",
            "ranger.https.attrib.keystore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks",
            "ranger.jpa.jdbc.dialect": "{{jdbc_dialect}}",
            "ranger.service.host": "{{ranger_host}}",
            "ranger.service.https.attrib.keystore.keyalias": "rangeradmin",
            "ranger.service.https.attrib.keystore.pass": "xasecure",
            "ranger.unixauth.remote.login.enabled": "true",
             "ranger.jpa.jdbc.credential.alias": "rangeradmin",
            "ranger.ldap.ad.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}", 
            "ranger.audit.solr.username": "ranger_solr", 
            "ranger.sso.enabled": "false", 
            "ranger.audit.solr.urls": "", 
            "ranger.ldap.ad.domain": "", 
            "ranger.plugins.yarn.serviceuser": "yarn", 
            "ranger.audit.source.type": "solr", 
            "ranger.ldap.bind.dn": "{{ranger_ug_ldap_bind_dn}}", 
            "ranger.ldap.url": "{{ranger_ug_ldap_url}}", 
            "ranger.authentication.method": "UNIX", 
            "ranger.service.http.enabled": "true", 
            "ranger.ldap.group.searchfilter": "{{ranger_ug_ldap_group_searchfilter}}", 
            "ranger.ldap.ad.referral": "ignore", 
            "ranger.ldap.ad.base.dn": "dc=example,dc=com", 
            "ranger.jpa.jdbc.password": "_", 
            "ranger.spnego.kerberos.keytab": "/etc/security/keytabs/spnego.service.keytab", 
            "ranger.sso.providerurl": "", 
            "ranger.unixauth.service.hostname": "{{ugsync_host}}", 
            "ranger.admin.kerberos.keytab": "/etc/security/keytabs/rangeradmin.service.keytab", 
            "ranger.admin.kerberos.token.valid.seconds": "30", 
            "ranger.jpa.jdbc.driver": "com.mysql.jdbc.Driver", 
            "ranger.ldap.ad.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}",
            "ranger.audit.solr.username": "ranger_solr",
            "ranger.sso.enabled": "false",
            "ranger.audit.solr.urls": "",
            "ranger.ldap.ad.domain": "",
            "ranger.plugins.yarn.serviceuser": "yarn",
            "ranger.audit.source.type": "solr",
            "ranger.ldap.bind.dn": "{{ranger_ug_ldap_bind_dn}}",
            "ranger.ldap.url": "{{ranger_ug_ldap_url}}",
            "ranger.authentication.method": "UNIX",
            "ranger.service.http.enabled": "true",
            "ranger.ldap.group.searchfilter": "{{ranger_ug_ldap_group_searchfilter}}",
            "ranger.ldap.ad.referral": "ignore",
            "ranger.ldap.ad.base.dn": "dc=example,dc=com",
            "ranger.jpa.jdbc.password": "_",
            "ranger.spnego.kerberos.keytab": "/etc/security/keytabs/spnego.service.keytab",
            "ranger.sso.providerurl": "",
            "ranger.unixauth.service.hostname": "{{ugsync_host}}",
            "ranger.admin.kerberos.keytab": "/etc/security/keytabs/rangeradmin.service.keytab",
            "ranger.admin.kerberos.token.valid.seconds": "30",
            "ranger.jpa.jdbc.driver": "com.mysql.jdbc.Driver",
             "ranger.unixauth.service.port": "5151"
        }, 
        },
         "ranger-hdfs-policymgr-ssl": {
            "xasecure.policymgr.clientssl.keystore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-keystore.jks", 
            "xasecure.policymgr.clientssl.truststore.password": "changeit", 
            "xasecure.policymgr.clientssl.keystore.credential.file": "jceks://file{{credential_file}}", 
            "xasecure.policymgr.clientssl.truststore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-truststore.jks", 
            "xasecure.policymgr.clientssl.truststore.credential.file": "jceks://file{{credential_file}}", 
            "xasecure.policymgr.clientssl.keystore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-keystore.jks",
            "xasecure.policymgr.clientssl.truststore.password": "changeit",
            "xasecure.policymgr.clientssl.keystore.credential.file": "jceks://file{{credential_file}}",
            "xasecure.policymgr.clientssl.truststore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-truststore.jks",
            "xasecure.policymgr.clientssl.truststore.credential.file": "jceks://file{{credential_file}}",
             "xasecure.policymgr.clientssl.keystore.password": "myKeyFilePassword"
        }, 
        },
         "tagsync-application-properties": {
            "atlas.kafka.zookeeper.connect": "c6401.ambari.apache.org:2181", 
            "atlas.kafka.security.protocol": "SASL_PLAINTEXT", 
            "atlas.jaas.KafkaClient.option.principal": "{{tagsync_jaas_principal}}", 
            "atlas.jaas.KafkaClient.option.keyTab": "{{tagsync_keytab_path}}", 
            "atlas.kafka.entities.group.id": "ranger_entities_consumer", 
            "atlas.jaas.KafkaClient.loginModuleName": "com.sun.security.auth.module.Krb5LoginModule", 
            "atlas.jaas.KafkaClient.option.serviceName": "kafka", 
            "atlas.kafka.bootstrap.servers": "localhost:6667", 
            "atlas.jaas.KafkaClient.option.useKeyTab": "true", 
            "atlas.jaas.KafkaClient.option.storeKey": "true", 
            "atlas.jaas.KafkaClient.loginModuleControlFlag": "required", 
            "atlas.kafka.zookeeper.connect": "c6401.ambari.apache.org:2181",
            "atlas.kafka.security.protocol": "SASL_PLAINTEXT",
            "atlas.jaas.KafkaClient.option.principal": "{{tagsync_jaas_principal}}",
            "atlas.jaas.KafkaClient.option.keyTab": "{{tagsync_keytab_path}}",
            "atlas.kafka.entities.group.id": "ranger_entities_consumer",
            "atlas.jaas.KafkaClient.loginModuleName": "com.sun.security.auth.module.Krb5LoginModule",
            "atlas.jaas.KafkaClient.option.serviceName": "kafka",
            "atlas.kafka.bootstrap.servers": "localhost:6667",
            "atlas.jaas.KafkaClient.option.useKeyTab": "true",
            "atlas.jaas.KafkaClient.option.storeKey": "true",
            "atlas.jaas.KafkaClient.loginModuleControlFlag": "required",
             "atlas.kafka.sasl.kerberos.service.name": "kafka"
        }, 
        },
         "ranger-env": {
            "ranger_solr_shards": "1", 
            "ranger_solr_config_set": "ranger_audits", 
            "ranger_user": "ranger", 
            "ranger_solr_shards": "1",
            "ranger_solr_config_set": "ranger_audits",
            "ranger_user": "ranger",
             "ranger_solr_replication_factor": "1",
            "xml_configurations_supported": "true", 
            "ranger-atlas-plugin-enabled": "No", 
            "ranger-hbase-plugin-enabled": "No", 
            "ranger-yarn-plugin-enabled": "No", 
            "bind_anonymous": "false", 
            "ranger_admin_username": "amb_ranger_admin", 
            "admin_password": "admin", 
            "is_solrCloud_enabled": "true", 
            "ranger-storm-plugin-enabled": "No", 
            "ranger-hdfs-plugin-enabled": "No", 
            "ranger_group": "ranger", 
            "ranger-knox-plugin-enabled": "No", 
            "ranger_admin_log_dir": "/var/log/ranger/admin", 
            "ranger-kafka-plugin-enabled": "No", 
            "ranger_privelege_user_jdbc_url": "jdbc:mysql://c6401.ambari.apache.org:3306", 
            "ranger-hive-plugin-enabled": "No", 
            "xasecure.audit.destination.solr": "true", 
            "ranger_pid_dir": "/var/run/ranger", 
            "xasecure.audit.destination.hdfs": "true", 
            "admin_username": "admin", 
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit", 
            "create_db_dbuser": "true", 
            "ranger_solr_collection_name": "ranger_audits", 
            "ranger_admin_password": "P1!q9xa96SMi5NCl", 
            "xml_configurations_supported": "true",
            "ranger-atlas-plugin-enabled": "No",
            "ranger-hbase-plugin-enabled": "No",
            "ranger-yarn-plugin-enabled": "No",
            "bind_anonymous": "false",
            "ranger_admin_username": "amb_ranger_admin",
            "admin_password": "admin",
            "is_solrCloud_enabled": "true",
            "ranger-storm-plugin-enabled": "No",
            "ranger-hdfs-plugin-enabled": "No",
            "ranger_group": "ranger",
            "ranger-knox-plugin-enabled": "No",
            "ranger_admin_log_dir": "/var/log/ranger/admin",
            "ranger-kafka-plugin-enabled": "No",
            "ranger_privelege_user_jdbc_url": "jdbc:mysql://c6401.ambari.apache.org:3306",
            "ranger-hive-plugin-enabled": "No",
            "xasecure.audit.destination.solr": "true",
            "ranger_pid_dir": "/var/run/ranger",
            "xasecure.audit.destination.hdfs": "true",
            "admin_username": "admin",
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit",
            "create_db_dbuser": "true",
            "ranger_solr_collection_name": "ranger_audits",
            "ranger_admin_password": "P1!q9xa96SMi5NCl",
             "ranger_usersync_log_dir": "/var/log/ranger/usersync"
        }, 
        },
         "usersync-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#      http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n#\n\nlog4j.rootLogger = info,logFile\n\n# logFile\nlog4j.appender.logFile=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.logFile.file=${logdir}/usersync.log\nlog4j.appender.logFile.datePattern='.'yyyy-MM-dd\nlog4j.appender.logFile.layout=org.apache.log4j.PatternLayout\nlog4j.appender.logFile.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %m%n\n\n# console\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.Target=System.out\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %m%n"
        }, 
        },
         "ranger-hdfs-plugin-properties": {
            "hadoop.rpc.protection": "authentication", 
            "ranger-hdfs-plugin-enabled": "No", 
            "REPOSITORY_CONFIG_USERNAME": "hadoop", 
            "policy_user": "ambari-qa", 
            "common.name.for.certificate": "", 
            "hadoop.rpc.protection": "authentication",
            "ranger-hdfs-plugin-enabled": "No",
            "REPOSITORY_CONFIG_USERNAME": "hadoop",
            "policy_user": "ambari-qa",
            "common.name.for.certificate": "",
             "REPOSITORY_CONFIG_PASSWORD": "hadoop"
        }, 
        },
         "kerberos-env": {
            "kdc_hosts": "c6401.ambari.apache.org", 
            "manage_auth_to_local": "true", 
            "install_packages": "true", 
            "realm": "EXAMPLE.COM", 
            "encryption_types": "aes des3-cbc-sha1 rc4 des-cbc-md5", 
            "ad_create_attributes_template": "\n{\n  \"objectClass\": [\"top\", \"person\", \"organizationalPerson\", \"user\"],\n  \"cn\": \"$principal_name\",\n  #if( $is_service )\n  \"servicePrincipalName\": \"$principal_name\",\n  #end\n  \"userPrincipalName\": \"$normalized_principal\",\n  \"unicodePwd\": \"$password\",\n  \"accountExpires\": \"0\",\n  \"userAccountControl\": \"66048\"\n}", 
            "kdc_create_attributes": "", 
            "admin_server_host": "c6401.ambari.apache.org", 
            "group": "ambari-managed-principals", 
            "password_length": "20", 
            "ldap_url": "", 
            "manage_identities": "true", 
            "password_min_lowercase_letters": "1", 
            "create_ambari_principal": "true", 
            "service_check_principal_name": "${cluster_name|toLower()}-${short_date}", 
            "executable_search_paths": "/usr/bin, /usr/kerberos/bin, /usr/sbin, /usr/lib/mit/bin, /usr/lib/mit/sbin", 
            "password_chat_timeout": "5", 
            "kdc_type": "mit-kdc", 
            "set_password_expiry": "false", 
            "password_min_punctuation": "1", 
            "container_dn": "", 
            "case_insensitive_username_rules": "false", 
            "password_min_whitespace": "0", 
            "password_min_uppercase_letters": "1", 
            "kdc_hosts": "c6401.ambari.apache.org",
            "manage_auth_to_local": "true",
            "install_packages": "true",
            "realm": "EXAMPLE.COM",
            "encryption_types": "aes des3-cbc-sha1 rc4 des-cbc-md5",
            "ad_create_attributes_template": "\n{\n  \"objectClass\": [\"top\", \"person\", \"organizationalPerson\", \"user\"],\n  \"cn\": \"$principal_name\",\n  #if( $is_service )\n  \"servicePrincipalName\": \"$principal_name\",\n  #end\n  \"userPrincipalName\": \"$normalized_principal\",\n  \"unicodePwd\": \"$password\",\n  \"accountExpires\": \"0\",\n  \"userAccountControl\": \"66048\"\n}",
            "kdc_create_attributes": "",
            "admin_server_host": "c6401.ambari.apache.org",
            "group": "ambari-managed-principals",
            "password_length": "20",
            "ldap_url": "",
            "manage_identities": "true",
            "password_min_lowercase_letters": "1",
            "create_ambari_principal": "true",
            "service_check_principal_name": "${cluster_name|toLower()}-${short_date}",
            "executable_search_paths": "/usr/bin, /usr/kerberos/bin, /usr/sbin, /usr/lib/mit/bin, /usr/lib/mit/sbin",
            "password_chat_timeout": "5",
            "kdc_type": "mit-kdc",
            "set_password_expiry": "false",
            "password_min_punctuation": "1",
            "container_dn": "",
            "case_insensitive_username_rules": "false",
            "password_min_whitespace": "0",
            "password_min_uppercase_letters": "1",
             "password_min_digits": "1"
        }, 
        },
         "admin-properties": {
            "db_user": "rangeradmin01", 
            "DB_FLAVOR": "MYSQL", 
            "db_password": "rangeradmin01", 
            "db_root_user": "root", 
            "policymgr_external_url": "http://c6401.ambari.apache.org:6080", 
            "db_name": "ranger01", 
            "db_host": "c6401.ambari.apache.org", 
            "db_root_password": "vagrant", 
            "db_user": "rangeradmin01",
            "DB_FLAVOR": "MYSQL",
            "db_password": "rangeradmin01",
            "db_root_user": "root",
            "policymgr_external_url": "http://c6401.ambari.apache.org:6080",
            "db_name": "ranger01",
            "db_host": "c6401.ambari.apache.org",
            "db_root_password": "vagrant",
             "SQL_CONNECTOR_JAR": "{{driver_curl_target}}"
        }, 
        },
         "ranger-ugsync-site": {
            "ranger.usersync.ldap.binddn": "", 
            "ranger.usersync.policymgr.username": "rangerusersync", 
            "ranger.usersync.policymanager.mockrun": "false", 
            "ranger.usersync.group.searchbase": "", 
            "ranger.usersync.ldap.bindalias": "testldapalias", 
            "ranger.usersync.truststore.file": "/usr/hdp/current/ranger-usersync/conf/mytruststore.jks", 
            "ranger.usersync.port": "5151", 
            "ranger.usersync.pagedresultssize": "500", 
            "ranger.usersync.group.memberattributename": "", 
            "ranger.usersync.kerberos.principal": "rangerusersync/_HOST@EXAMPLE.COM", 
            "ranger.usersync.source.impl.class": "org.apache.ranger.unixusersync.process.UnixUserGroupBuilder", 
            "ranger.usersync.ldap.referral": "ignore", 
            "ranger.usersync.group.searchfilter": "", 
            "ranger.usersync.ldap.user.objectclass": "person", 
            "ranger.usersync.logdir": "{{usersync_log_dir}}", 
            "ranger.usersync.ldap.user.searchfilter": "", 
            "ranger.usersync.ldap.groupname.caseconversion": "none", 
            "ranger.usersync.ldap.ldapbindpassword": "", 
            "ranger.usersync.unix.minUserId": "500", 
            "ranger.usersync.policymanager.maxrecordsperapicall": "1000", 
            "ranger.usersync.group.nameattribute": "", 
            "ranger.usersync.policymgr.alias": "ranger.usersync.policymgr.password", 
            "ranger.usersync.keystore.file": "/usr/hdp/current/ranger-usersync/conf/unixauthservice.jks", 
            "ranger.usersync.user.searchenabled": "false", 
            "ranger.usersync.group.usermapsyncenabled": "true", 
            "ranger.usersync.ldap.bindkeystore": "", 
            "ranger.usersync.ldap.user.groupnameattribute": "memberof, ismemberof", 
            "ranger.usersync.kerberos.keytab": "/etc/security/keytabs/rangerusersync.service.keytab", 
            "ranger.usersync.passwordvalidator.path": "./native/credValidator.uexe", 
            "ranger.usersync.group.objectclass": "", 
            "ranger.usersync.ldap.user.searchscope": "sub", 
            "ranger.usersync.unix.password.file": "/etc/passwd", 
            "ranger.usersync.ldap.user.nameattribute": "", 
            "ranger.usersync.pagedresultsenabled": "true", 
            "ranger.usersync.policymanager.baseURL": "{{ranger_external_url}}", 
            "ranger.usersync.group.search.first.enabled": "false", 
            "ranger.usersync.group.searchenabled": "false", 
            "ranger.usersync.sink.impl.class": "org.apache.ranger.unixusersync.process.PolicyMgrUserGroupBuilder", 
            "ranger.usersync.ssl": "true", 
            "ranger.usersync.ldap.url": "", 
            "ranger.usersync.ldap.searchBase": "dc=hadoop,dc=apache,dc=org", 
            "ranger.usersync.policymgr.keystore": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks", 
            "ranger.usersync.ldap.user.searchbase": "", 
            "ranger.usersync.ldap.username.caseconversion": "none", 
            "ranger.usersync.credstore.filename": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks", 
            "ranger.usersync.keystore.password": "UnIx529p", 
            "ranger.usersync.unix.group.file": "/etc/group", 
            "ranger.usersync.filesource.file": "/tmp/usergroup.txt", 
            "ranger.usersync.group.searchscope": "", 
            "ranger.usersync.truststore.password": "changeit", 
            "ranger.usersync.enabled": "true", 
            "ranger.usersync.sleeptimeinmillisbetweensynccycle": "60000", 
            "ranger.usersync.ldap.binddn": "",
            "ranger.usersync.policymgr.username": "rangerusersync",
            "ranger.usersync.policymanager.mockrun": "false",
            "ranger.usersync.group.searchbase": "",
            "ranger.usersync.ldap.bindalias": "testldapalias",
            "ranger.usersync.truststore.file": "/usr/hdp/current/ranger-usersync/conf/mytruststore.jks",
            "ranger.usersync.port": "5151",
            "ranger.usersync.pagedresultssize": "500",
            "ranger.usersync.group.memberattributename": "",
            "ranger.usersync.kerberos.principal": "rangerusersync/_HOST@EXAMPLE.COM",
            "ranger.usersync.source.impl.class": "org.apache.ranger.unixusersync.process.UnixUserGroupBuilder",
            "ranger.usersync.ldap.referral": "ignore",
            "ranger.usersync.group.searchfilter": "",
            "ranger.usersync.ldap.user.objectclass": "person",
            "ranger.usersync.logdir": "{{usersync_log_dir}}",
            "ranger.usersync.ldap.user.searchfilter": "",
            "ranger.usersync.ldap.groupname.caseconversion": "none",
            "ranger.usersync.ldap.ldapbindpassword": "",
            "ranger.usersync.unix.minUserId": "500",
            "ranger.usersync.policymanager.maxrecordsperapicall": "1000",
            "ranger.usersync.group.nameattribute": "",
            "ranger.usersync.policymgr.alias": "ranger.usersync.policymgr.password",
            "ranger.usersync.keystore.file": "/usr/hdp/current/ranger-usersync/conf/unixauthservice.jks",
            "ranger.usersync.user.searchenabled": "false",
            "ranger.usersync.group.usermapsyncenabled": "true",
            "ranger.usersync.ldap.bindkeystore": "",
            "ranger.usersync.ldap.user.groupnameattribute": "memberof, ismemberof",
            "ranger.usersync.kerberos.keytab": "/etc/security/keytabs/rangerusersync.service.keytab",
            "ranger.usersync.passwordvalidator.path": "./native/credValidator.uexe",
            "ranger.usersync.group.objectclass": "",
            "ranger.usersync.ldap.user.searchscope": "sub",
            "ranger.usersync.unix.password.file": "/etc/passwd",
            "ranger.usersync.ldap.user.nameattribute": "",
            "ranger.usersync.pagedresultsenabled": "true",
            "ranger.usersync.policymanager.baseURL": "{{ranger_external_url}}",
            "ranger.usersync.group.search.first.enabled": "false",
            "ranger.usersync.group.searchenabled": "false",
            "ranger.usersync.sink.impl.class": "org.apache.ranger.unixusersync.process.PolicyMgrUserGroupBuilder",
            "ranger.usersync.ssl": "true",
            "ranger.usersync.ldap.url": "",
            "ranger.usersync.ldap.searchBase": "dc=hadoop,dc=apache,dc=org",
            "ranger.usersync.policymgr.keystore": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks",
            "ranger.usersync.ldap.user.searchbase": "",
            "ranger.usersync.ldap.username.caseconversion": "none",
            "ranger.usersync.credstore.filename": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks",
            "ranger.usersync.keystore.password": "UnIx529p",
            "ranger.usersync.unix.group.file": "/etc/group",
            "ranger.usersync.filesource.file": "/tmp/usergroup.txt",
            "ranger.usersync.group.searchscope": "",
            "ranger.usersync.truststore.password": "changeit",
            "ranger.usersync.enabled": "true",
            "ranger.usersync.sleeptimeinmillisbetweensynccycle": "60000",
             "ranger.usersync.filesource.text.delimiter": ","
        }, 
        },
         "hdfs-site": {
            "dfs.namenode.checkpoint.period": "21600", 
            "dfs.namenode.avoid.write.stale.datanode": "true", 
            "dfs.permissions.superusergroup": "hdfs", 
            "dfs.namenode.startup.delay.block.deletion.sec": "3600", 
            "dfs.namenode.kerberos.internal.spnego.principal": "HTTP/_HOST@EXAMPLE.COM", 
            "dfs.datanode.kerberos.principal": "dn/_HOST@EXAMPLE.COM", 
            "dfs.heartbeat.interval": "3", 
            "dfs.content-summary.limit": "5000", 
            "dfs.support.append": "true", 
            "dfs.datanode.address": "0.0.0.0:1019", 
            "dfs.cluster.administrators": " hdfs", 
            "dfs.namenode.audit.log.async": "true", 
            "dfs.datanode.balance.bandwidthPerSec": "6250000", 
            "dfs.namenode.safemode.threshold-pct": "1", 
            "dfs.namenode.checkpoint.edits.dir": "${dfs.namenode.checkpoint.dir}", 
            "dfs.namenode.rpc-address": "c6401.ambari.apache.org:8020", 
            "dfs.permissions.enabled": "true", 
            "dfs.namenode.kerberos.principal": "nn/_HOST@EXAMPLE.COM", 
            "dfs.client.read.shortcircuit": "true", 
            "dfs.https.port": "50470", 
            "dfs.namenode.https-address": "c6401.ambari.apache.org:50470", 
            "nfs.file.dump.dir": "/tmp/.hdfs-nfs", 
            "dfs.blocksize": "134217728", 
            "dfs.blockreport.initialDelay": "120", 
            "dfs.journalnode.edits.dir": "/hadoop/hdfs/journalnode", 
            "dfs.namenode.fslock.fair": "false", 
            "dfs.datanode.max.transfer.threads": "4096", 
            "dfs.secondary.namenode.kerberos.internal.spnego.principal": "HTTP/_HOST@EXAMPLE.COM", 
            "dfs.replication": "3", 
            "dfs.namenode.handler.count": "50", 
            "dfs.web.authentication.kerberos.keytab": "/etc/security/keytabs/spnego.service.keytab", 
            "fs.permissions.umask-mode": "022", 
            "dfs.namenode.stale.datanode.interval": "30000", 
            "dfs.datanode.ipc.address": "0.0.0.0:8010", 
            "dfs.datanode.failed.volumes.tolerated": "0", 
            "dfs.datanode.data.dir": "/grid/0/hadoop/hdfs/data", 
            "dfs.namenode.http-address": "c6401.ambari.apache.org:50070", 
            "dfs.webhdfs.enabled": "true", 
            "dfs.encrypt.data.transfer.cipher.suites": "AES/CTR/NoPadding", 
            "dfs.namenode.accesstime.precision": "0", 
            "dfs.namenode.write.stale.datanode.ratio": "1.0f", 
            "dfs.datanode.https.address": "0.0.0.0:50475", 
            "dfs.namenode.checkpoint.dir": "/grid/0/hadoop/hdfs/namesecondary", 
            "dfs.namenode.secondary.http-address": "c6401.ambari.apache.org:50090", 
            "nfs.exports.allowed.hosts": "* rw", 
            "dfs.namenode.checkpoint.txns": "1000000", 
            "dfs.datanode.http.address": "0.0.0.0:1022", 
            "dfs.datanode.du.reserved": "33011188224", 
            "dfs.client.read.shortcircuit.streams.cache.size": "4096", 
            "dfs.secondary.namenode.keytab.file": "/etc/security/keytabs/nn.service.keytab", 
            "dfs.web.authentication.kerberos.principal": "HTTP/_HOST@EXAMPLE.COM", 
            "dfs.http.policy": "HTTP_ONLY", 
            "dfs.block.access.token.enable": "true", 
            "dfs.client.retry.policy.enabled": "false", 
            "dfs.secondary.namenode.kerberos.principal": "nn/_HOST@EXAMPLE.COM", 
            "dfs.datanode.keytab.file": "/etc/security/keytabs/dn.service.keytab", 
            "dfs.namenode.name.dir.restore": "true", 
            "dfs.namenode.keytab.file": "/etc/security/keytabs/nn.service.keytab", 
            "dfs.journalnode.https-address": "0.0.0.0:8481", 
            "dfs.journalnode.http-address": "0.0.0.0:8480", 
            "dfs.domain.socket.path": "/var/lib/hadoop-hdfs/dn_socket", 
            "dfs.namenode.avoid.read.stale.datanode": "true", 
            "dfs.hosts.exclude": "/etc/hadoop/conf/dfs.exclude", 
            "dfs.datanode.data.dir.perm": "750", 
            "dfs.encryption.key.provider.uri": "kms://http@c6401.ambari.apache.org:9292/kms", 
            "dfs.replication.max": "50", 
            "dfs.namenode.checkpoint.period": "21600",
            "dfs.namenode.avoid.write.stale.datanode": "true",
            "dfs.permissions.superusergroup": "hdfs",
            "dfs.namenode.startup.delay.block.deletion.sec": "3600",
            "dfs.namenode.kerberos.internal.spnego.principal": "HTTP/_HOST@EXAMPLE.COM",
            "dfs.datanode.kerberos.principal": "dn/_HOST@EXAMPLE.COM",
            "dfs.heartbeat.interval": "3",
            "dfs.content-summary.limit": "5000",
            "dfs.support.append": "true",
            "dfs.datanode.address": "0.0.0.0:1019",
            "dfs.cluster.administrators": " hdfs",
            "dfs.namenode.audit.log.async": "true",
            "dfs.datanode.balance.bandwidthPerSec": "6250000",
            "dfs.namenode.safemode.threshold-pct": "1",
            "dfs.namenode.checkpoint.edits.dir": "${dfs.namenode.checkpoint.dir}",
            "dfs.namenode.rpc-address": "c6401.ambari.apache.org:8020",
            "dfs.permissions.enabled": "true",
            "dfs.namenode.kerberos.principal": "nn/_HOST@EXAMPLE.COM",
            "dfs.client.read.shortcircuit": "true",
            "dfs.https.port": "50470",
            "dfs.namenode.https-address": "c6401.ambari.apache.org:50470",
            "nfs.file.dump.dir": "/tmp/.hdfs-nfs",
            "dfs.blocksize": "134217728",
            "dfs.blockreport.initialDelay": "120",
            "dfs.journalnode.edits.dir": "/hadoop/hdfs/journalnode",
            "dfs.namenode.fslock.fair": "false",
            "dfs.datanode.max.transfer.threads": "4096",
            "dfs.secondary.namenode.kerberos.internal.spnego.principal": "HTTP/_HOST@EXAMPLE.COM",
            "dfs.replication": "3",
            "dfs.namenode.handler.count": "50",
            "dfs.web.authentication.kerberos.keytab": "/etc/security/keytabs/spnego.service.keytab",
            "fs.permissions.umask-mode": "022",
            "dfs.namenode.stale.datanode.interval": "30000",
            "dfs.datanode.ipc.address": "0.0.0.0:8010",
            "dfs.datanode.failed.volumes.tolerated": "0",
            "dfs.datanode.data.dir": "/grid/0/hadoop/hdfs/data",
            "dfs.namenode.http-address": "c6401.ambari.apache.org:50070",
            "dfs.webhdfs.enabled": "true",
            "dfs.encrypt.data.transfer.cipher.suites": "AES/CTR/NoPadding",
            "dfs.namenode.accesstime.precision": "0",
            "dfs.namenode.write.stale.datanode.ratio": "1.0f",
            "dfs.datanode.https.address": "0.0.0.0:50475",
            "dfs.namenode.checkpoint.dir": "/grid/0/hadoop/hdfs/namesecondary",
            "dfs.namenode.secondary.http-address": "c6401.ambari.apache.org:50090",
            "nfs.exports.allowed.hosts": "* rw",
            "dfs.namenode.checkpoint.txns": "1000000",
            "dfs.datanode.http.address": "0.0.0.0:1022",
            "dfs.datanode.du.reserved": "33011188224",
            "dfs.client.read.shortcircuit.streams.cache.size": "4096",
            "dfs.secondary.namenode.keytab.file": "/etc/security/keytabs/nn.service.keytab",
            "dfs.web.authentication.kerberos.principal": "HTTP/_HOST@EXAMPLE.COM",
            "dfs.http.policy": "HTTP_ONLY",
            "dfs.block.access.token.enable": "true",
            "dfs.client.retry.policy.enabled": "false",
            "dfs.secondary.namenode.kerberos.principal": "nn/_HOST@EXAMPLE.COM",
            "dfs.datanode.keytab.file": "/etc/security/keytabs/dn.service.keytab",
            "dfs.namenode.name.dir.restore": "true",
            "dfs.namenode.keytab.file": "/etc/security/keytabs/nn.service.keytab",
            "dfs.journalnode.https-address": "0.0.0.0:8481",
            "dfs.journalnode.http-address": "0.0.0.0:8480",
            "dfs.domain.socket.path": "/var/lib/hadoop-hdfs/dn_socket",
            "dfs.namenode.avoid.read.stale.datanode": "true",
            "dfs.hosts.exclude": "/etc/hadoop/conf/dfs.exclude",
            "dfs.datanode.data.dir.perm": "750",
            "dfs.encryption.key.provider.uri": "kms://http@c6401.ambari.apache.org:9292/kms",
            "dfs.replication.max": "50",
             "dfs.namenode.name.dir": "/grid/0/hadoop/hdfs/namenode"
        }, 
        },
         "ranger-tagsync-site": {
            "ranger.tagsync.dest.ranger.ssl.config.filename": "/usr/hdp/current/ranger-tagsync/conf/mytruststore.jks", 
            "ranger.tagsync.source.atlasrest.username": "", 
            "ranger.tagsync.logdir": "/var/log/ranger/tagsync", 
            "ranger.tagsync.source.atlasrest.download.interval.millis": "", 
            "ranger.tagsync.keystore.filename": "/usr/hdp/current/ranger-tagsync/conf/rangertagsync.jceks", 
            "ranger.tagsync.source.file.check.interval.millis": "", 
            "ranger.tagsync.source.atlasrest.endpoint": "", 
            "ranger.tagsync.dest.ranger.username": "rangertagsync", 
            "ranger.tagsync.dest.ranger.endpoint": "{{ranger_external_url}}", 
            "ranger.tagsync.kerberos.principal": "rangertagsync/_HOST@EXAMPLE.COM", 
            "ranger.tagsync.kerberos.keytab": "/etc/security/keytabs/rangertagsync.service.keytab", 
            "ranger.tagsync.source.atlas": "false", 
            "ranger.tagsync.source.atlasrest": "false", 
            "ranger.tagsync.source.file": "false", 
            "ranger.tagsync.dest.ranger.ssl.config.filename": "/usr/hdp/current/ranger-tagsync/conf/mytruststore.jks",
            "ranger.tagsync.source.atlasrest.username": "",
            "ranger.tagsync.logdir": "/var/log/ranger/tagsync",
            "ranger.tagsync.source.atlasrest.download.interval.millis": "",
            "ranger.tagsync.keystore.filename": "/usr/hdp/current/ranger-tagsync/conf/rangertagsync.jceks",
            "ranger.tagsync.source.file.check.interval.millis": "",
            "ranger.tagsync.source.atlasrest.endpoint": "",
            "ranger.tagsync.dest.ranger.username": "rangertagsync",
            "ranger.tagsync.dest.ranger.endpoint": "{{ranger_external_url}}",
            "ranger.tagsync.kerberos.principal": "rangertagsync/_HOST@EXAMPLE.COM",
            "ranger.tagsync.kerberos.keytab": "/etc/security/keytabs/rangertagsync.service.keytab",
            "ranger.tagsync.source.atlas": "false",
            "ranger.tagsync.source.atlasrest": "false",
            "ranger.tagsync.source.file": "false",
             "ranger.tagsync.source.file.filename": ""
        }, 
        },
         "zoo.cfg": {
            "clientPort": "2181", 
            "autopurge.purgeInterval": "24", 
            "syncLimit": "5", 
            "dataDir": "/grid/0/hadoop/zookeeper", 
            "initLimit": "10", 
            "tickTime": "2000", 
            "clientPort": "2181",
            "autopurge.purgeInterval": "24",
            "syncLimit": "5",
            "dataDir": "/grid/0/hadoop/zookeeper",
            "initLimit": "10",
            "tickTime": "2000",
             "autopurge.snapRetainCount": "30"
        }, 
        },
         "hadoop-policy": {
            "security.job.client.protocol.acl": "*", 
            "security.job.task.protocol.acl": "*", 
            "security.datanode.protocol.acl": "*", 
            "security.namenode.protocol.acl": "*", 
            "security.client.datanode.protocol.acl": "*", 
            "security.inter.tracker.protocol.acl": "*", 
            "security.refresh.usertogroups.mappings.protocol.acl": "hadoop", 
            "security.client.protocol.acl": "*", 
            "security.refresh.policy.protocol.acl": "hadoop", 
            "security.admin.operations.protocol.acl": "hadoop", 
            "security.job.client.protocol.acl": "*",
            "security.job.task.protocol.acl": "*",
            "security.datanode.protocol.acl": "*",
            "security.namenode.protocol.acl": "*",
            "security.client.datanode.protocol.acl": "*",
            "security.inter.tracker.protocol.acl": "*",
            "security.refresh.usertogroups.mappings.protocol.acl": "hadoop",
            "security.client.protocol.acl": "*",
            "security.refresh.policy.protocol.acl": "hadoop",
            "security.admin.operations.protocol.acl": "hadoop",
             "security.inter.datanode.protocol.acl": "*"
        }, 
        },
         "hdfs-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#  http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing,\n# software distributed under the License is distributed on an\n# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n# KIND, either express or implied.  See the License for the\n# specific language governing permissions and limitations\n# under the License.\n#\n\n\n# Define some default values that can be overridden by system properties\n# To change daemon root logger use hadoop_root_logger in hadoop-env\nhadoop.root.logger=INFO,console\nhadoop.log.dir=.\nhadoop.log.file=hadoop.log\n\n\n# Define the root logger to the system property \"hadoop.root.logger\".\nlog4j.rootLogger=${hadoop.root.logger}, EventCounter\n\n# Logging Threshold\nlog4j.threshhold=ALL\n\n#\n# Daily Rolling File Appender\n#\n\nlog4j.appender.DRFA=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.DRFA.File=${hadoop.log.dir}/${hadoop.log.file}\n\n# Rollver at midnight\nlog4j.appender.DRFA.DatePattern=.yyyy-MM-dd\n\n# 30-day backup\n#log4j.appender.DRFA.MaxBackupIndex=30\nlog4j.appender.DRFA.layout=org.apache.log4j.PatternLayout\n\n# Pattern format: Date LogLevel LoggerName LogMessage\nlog4j.appender.DRFA.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\n# Debugging Pattern format\n#log4j.appender.DRFA.layout.ConversionPattern=%d{ISO8601} %-5p %c{2} (%F:%M(%L)) - %m%n\n\n\n#\n# console\n# Add \"console\" to rootlogger above if you want to use this\n#\n\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.target=System.err\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{yy/MM/dd HH:mm:ss} %p %c{2}: %m%n\n\n#\n# TaskLog Appender\n#\n\n#Default values\nhadoop.tasklog.taskid=null\nhadoop.tasklog.iscleanup=false\nhadoop.tasklog.noKeepSplits=4\nhadoop.tasklog.totalLogFileSize=100\nhadoop.tasklog.purgeLogSplits=true\nhadoop.tasklog.logsRetainHours=12\n\nlog4j.appender.TLA=org.apache.hadoop.mapred.TaskLogAppender\nlog4j.appender.TLA.taskId=${hadoop.tasklog.taskid}\nlog4j.appender.TLA.isCleanup=${hadoop.tasklog.iscleanup}\nlog4j.appender.TLA.totalLogFileSize=${hadoop.tasklog.totalLogFileSize}\n\nlog4j.appender.TLA.layout=org.apache.log4j.PatternLayout\nlog4j.appender.TLA.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\n\n#\n#Security audit appender\n#\nhadoop.security.logger=INFO,console\nhadoop.security.log.maxfilesize=256MB\nhadoop.security.log.maxbackupindex=20\nlog4j.category.SecurityLogger=${hadoop.security.logger}\nhadoop.security.log.file=SecurityAuth.audit\nlog4j.appender.DRFAS=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.DRFAS.File=${hadoop.log.dir}/${hadoop.security.log.file}\nlog4j.appender.DRFAS.layout=org.apache.log4j.PatternLayout\nlog4j.appender.DRFAS.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\nlog4j.appender.DRFAS.DatePattern=.yyyy-MM-dd\n\nlog4j.appender.RFAS=org.apache.log4j.RollingFileAppender\nlog4j.appender.RFAS.File=${hadoop.log.dir}/${hadoop.security.log.file}\nlog4j.appender.RFAS.layout=org.apache.log4j.PatternLayout\nlog4j.appender.RFAS.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\nlog4j.appender.RFAS.MaxFileSize=${hadoop.security.log.maxfilesize}\nlog4j.appender.RFAS.MaxBackupIndex=${hadoop.security.log.maxbackupindex}\n\n#\n# hdfs audit logging\n#\nhdfs.audit.logger=INFO,console\nlog4j.logger.org.apache.hadoop.hdfs.server.namenode.FSNamesystem.audit=${hdfs.audit.logger}\nlog4j.additivity.org.apache.hadoop.hdfs.server.namenode.FSNamesystem.audit=false\nlog4j.appender.DRFAAUDIT=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.DRFAAUDIT.File=${hadoop.log.dir}/hdfs-audit.log\nlog4j.appender.DRFAAUDIT.layout=org.apache.log4j.PatternLayout\nlog4j.appender.DRFAAUDIT.layout.ConversionPattern=%d{ISO8601} %p %c{2}: %m%n\nlog4j.appender.DRFAAUDIT.DatePattern=.yyyy-MM-dd\n\n#\n# NameNode metrics logging.\n# The default is to retain two namenode-metrics.log files up to 64MB each.\n#\nnamenode.metrics.logger=INFO,NullAppender\nlog4j.logger.NameNodeMetricsLog=${namenode.metrics.logger}\nlog4j.additivity.NameNodeMetricsLog=false\nlog4j.appender.NNMETRICSRFA=org.apache.log4j.RollingFileAppender\nlog4j.appender.NNMETRICSRFA.File=${hadoop.log.dir}/namenode-metrics.log\nlog4j.appender.NNMETRICSRFA.layout=org.apache.log4j.PatternLayout\nlog4j.appender.NNMETRICSRFA.layout.ConversionPattern=%d{ISO8601} %m%n\nlog4j.appender.NNMETRICSRFA.MaxBackupIndex=1\nlog4j.appender.NNMETRICSRFA.MaxFileSize=64MB\n\n#\n# mapred audit logging\n#\nmapred.audit.logger=INFO,console\nlog4j.logger.org.apache.hadoop.mapred.AuditLogger=${mapred.audit.logger}\nlog4j.additivity.org.apache.hadoop.mapred.AuditLogger=false\nlog4j.appender.MRAUDIT=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.MRAUDIT.File=${hadoop.log.dir}/mapred-audit.log\nlog4j.appender.MRAUDIT.layout=org.apache.log4j.PatternLayout\nlog4j.appender.MRAUDIT.layout.ConversionPattern=%d{ISO8601} %p %c{2}: %m%n\nlog4j.appender.MRAUDIT.DatePattern=.yyyy-MM-dd\n\n#\n# Rolling File Appender\n#\n\nlog4j.appender.RFA=org.apache.log4j.RollingFileAppender\nlog4j.appender.RFA.File=${hadoop.log.dir}/${hadoop.log.file}\n\n# Logfile size and and 30-day backups\nlog4j.appender.RFA.MaxFileSize=256MB\nlog4j.appender.RFA.MaxBackupIndex=10\n\nlog4j.appender.RFA.layout=org.apache.log4j.PatternLayout\nlog4j.appender.RFA.layout.ConversionPattern=%d{ISO8601} %-5p %c{2} - %m%n\nlog4j.appender.RFA.layout.ConversionPattern=%d{ISO8601} %-5p %c{2} (%F:%M(%L)) - %m%n\n\n\n# Custom Logging levels\n\nhadoop.metrics.log.level=INFO\n#log4j.logger.org.apache.hadoop.mapred.JobTracker=DEBUG\n#log4j.logger.org.apache.hadoop.mapred.TaskTracker=DEBUG\n#log4j.logger.org.apache.hadoop.fs.FSNamesystem=DEBUG\nlog4j.logger.org.apache.hadoop.metrics2=${hadoop.metrics.log.level}\n\n# Jets3t library\nlog4j.logger.org.jets3t.service.impl.rest.httpclient.RestS3Service=ERROR\n\n#\n# Null Appender\n# Trap security logger on the hadoop client side\n#\nlog4j.appender.NullAppender=org.apache.log4j.varia.NullAppender\n\n#\n# Event Counter Appender\n# Sends counts of logging messages at different severity levels to Hadoop Metrics.\n#\nlog4j.appender.EventCounter=org.apache.hadoop.log.metrics.EventCounter\n\n# Removes \"deprecated\" messages\nlog4j.logger.org.apache.hadoop.conf.Configuration.deprecation=WARN\n\n#\n# HDFS block state change log from block manager\n#\n# Uncomment the following to suppress normal block state change\n# messages from BlockManager in NameNode.\n#log4j.logger.BlockStateChange=WARN"
        }, 
        },
         "krb5-conf": {
            "domains": "", 
            "manage_krb5_conf": "true", 
            "content": "\n[libdefaults]\n  renew_lifetime = 7d\n  forwardable = true\n  default_realm = {{realm}}\n  ticket_lifetime = 24h\n  dns_lookup_realm = false\n  dns_lookup_kdc = false\n  default_ccache_name = /tmp/krb5cc_%{uid}\n  #default_tgs_enctypes = {{encryption_types}}\n  #default_tkt_enctypes = {{encryption_types}}\n{% if domains %}\n[domain_realm]\n{%- for domain in domains.split(',') %}\n  {{domain|trim()}} = {{realm}}\n{%- endfor %}\n{% endif %}\n[logging]\n  default = FILE:/var/log/krb5kdc.log\n  admin_server = FILE:/var/log/kadmind.log\n  kdc = FILE:/var/log/krb5kdc.log\n\n[realms]\n  {{realm}} = {\n{%- if kdc_hosts > 0 -%}\n{%- set kdc_host_list = kdc_hosts.split(',')  -%}\n{%- if kdc_host_list and kdc_host_list|length > 0 %}\n    admin_server = {{admin_server_host|default(kdc_host_list[0]|trim(), True)}}\n{%- if kdc_host_list -%}\n{% for kdc_host in kdc_host_list %}\n    kdc = {{kdc_host|trim()}}\n{%- endfor -%}\n{% endif %}\n{%- endif %}\n{%- endif %}\n  }\n\n{# Append additional realm declarations below #}", 
            "domains": "",
            "manage_krb5_conf": "true",
            "content": "\n[libdefaults]\n  renew_lifetime = 7d\n  forwardable = true\n  default_realm = {{realm}}\n  ticket_lifetime = 24h\n  dns_lookup_realm = false\n  dns_lookup_kdc = false\n  default_ccache_name = /tmp/krb5cc_%{uid}\n  #default_tgs_enctypes = {{encryption_types}}\n  #default_tkt_enctypes = {{encryption_types}}\n{% if domains %}\n[domain_realm]\n{%- for domain in domains.split(',') %}\n  {{domain|trim()}} = {{realm}}\n{%- endfor %}\n{% endif %}\n[logging]\n  default = FILE:/var/log/krb5kdc.log\n  admin_server = FILE:/var/log/kadmind.log\n  kdc = FILE:/var/log/krb5kdc.log\n\n[realms]\n  {{realm}} = {\n{%- if kdc_hosts > 0 -%}\n{%- set kdc_host_list = kdc_hosts.split(',')  -%}\n{%- if kdc_host_list and kdc_host_list|length > 0 %}\n    admin_server = {{admin_server_host|default(kdc_host_list[0]|trim(), True)}}\n{%- if kdc_host_list -%}\n{% for kdc_host in kdc_host_list %}\n    kdc = {{kdc_host|trim()}}\n{%- endfor -%}\n{% endif %}\n{%- endif %}\n{%- endif %}\n  }\n\n{# Append additional realm declarations below #}",
             "conf_dir": "/etc"
        }, 
        },
         "core-site": {
            "net.topology.script.file.name": "/etc/hadoop/conf/topology_script.py", 
            "hadoop.proxyuser.hdfs.groups": "*", 
            "fs.trash.interval": "360", 
            "ipc.server.tcpnodelay": "true", 
            "io.compression.codecs": "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec,org.apache.hadoop.io.compress.SnappyCodec", 
            "ipc.client.idlethreshold": "8000", 
            "io.file.buffer.size": "131072", 
            "hadoop.proxyuser.ambari-server-test_cluster01.groups": "*", 
            "io.serializations": "org.apache.hadoop.io.serializer.WritableSerialization", 
            "hadoop.security.authentication": "kerberos", 
            "mapreduce.jobtracker.webinterface.trusted": "false", 
            "hadoop.proxyuser.hdfs.hosts": "*", 
            "hadoop.proxyuser.HTTP.groups": "users", 
            "fs.defaultFS": "hdfs://c6401.ambari.apache.org:8020", 
            "ha.failover-controller.active-standby-elector.zk.op.retries": "120", 
            "hadoop.security.key.provider.path": "kms://http@c6401.ambari.apache.org:9292/kms", 
            "hadoop.security.authorization": "true", 
            "hadoop.http.authentication.simple.anonymous.allowed": "true", 
            "ipc.client.connect.max.retries": "50", 
            "hadoop.security.auth_to_local": "RULE:[1:$1@$0](ambari-qa-test_cluster01@EXAMPLE.COM)s/.*/ambari-qa/\nRULE:[1:$1@$0](hdfs-test_cluster01@EXAMPLE.COM)s/.*/hdfs/\nRULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\nRULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\nRULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\nRULE:[2:$1@$0](rangeradmin@EXAMPLE.COM)s/.*/ranger/\nRULE:[2:$1@$0](rangertagsync@EXAMPLE.COM)s/.*/rangertagsync/\nRULE:[2:$1@$0](rangerusersync@EXAMPLE.COM)s/.*/rangerusersync/\nDEFAULT", 
            "hadoop.proxyuser.ambari-server-test_cluster01.hosts": "c6401.ambari.apache.org", 
            "net.topology.script.file.name": "/etc/hadoop/conf/topology_script.py",
            "hadoop.proxyuser.hdfs.groups": "*",
            "fs.trash.interval": "360",
            "ipc.server.tcpnodelay": "true",
            "io.compression.codecs": "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec,org.apache.hadoop.io.compress.SnappyCodec",
            "ipc.client.idlethreshold": "8000",
            "io.file.buffer.size": "131072",
            "hadoop.proxyuser.ambari-server-test_cluster01.groups": "*",
            "io.serializations": "org.apache.hadoop.io.serializer.WritableSerialization",
            "hadoop.security.authentication": "kerberos",
            "mapreduce.jobtracker.webinterface.trusted": "false",
            "hadoop.proxyuser.hdfs.hosts": "*",
            "hadoop.proxyuser.HTTP.groups": "users",
            "fs.defaultFS": "hdfs://c6401.ambari.apache.org:8020",
            "ha.failover-controller.active-standby-elector.zk.op.retries": "120",
            "hadoop.security.key.provider.path": "kms://http@c6401.ambari.apache.org:9292/kms",
            "hadoop.security.authorization": "true",
            "hadoop.http.authentication.simple.anonymous.allowed": "true",
            "ipc.client.connect.max.retries": "50",
            "hadoop.security.auth_to_local": "RULE:[1:$1@$0](ambari-qa-test_cluster01@EXAMPLE.COM)s/.*/ambari-qa/\nRULE:[1:$1@$0](hdfs-test_cluster01@EXAMPLE.COM)s/.*/hdfs/\nRULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\nRULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\nRULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\nRULE:[2:$1@$0](rangeradmin@EXAMPLE.COM)s/.*/ranger/\nRULE:[2:$1@$0](rangertagsync@EXAMPLE.COM)s/.*/rangertagsync/\nRULE:[2:$1@$0](rangerusersync@EXAMPLE.COM)s/.*/rangerusersync/\nDEFAULT",
            "hadoop.proxyuser.ambari-server-test_cluster01.hosts": "c6401.ambari.apache.org",
             "ipc.client.connection.maxidletime": "30000"
        }, 
        },
         "hadoop-env": {
            "keyserver_port": "", 
            "proxyuser_group": "users", 
            "hdfs_user_nproc_limit": "65536", 
            "hdfs_log_dir_prefix": "/var/log/hadoop", 
            "hdfs_user_nofile_limit": "128000", 
            "hdfs_user": "hdfs", 
            "hdfs_principal_name": "hdfs-test_cluster01@EXAMPLE.COM", 
            "keyserver_host": " ", 
            "namenode_opt_maxnewsize": "128m", 
            "hdfs_user_keytab": "/etc/security/keytabs/hdfs.headless.keytab", 
            "namenode_opt_maxpermsize": "256m", 
            "content": "\n# Set Hadoop-specific environment variables here.\n\n# The only required environment variable is JAVA_HOME.  All others are\n# optional.  When running a distributed configuration it is best to\n# set JAVA_HOME in this file, so that it is correctly defined on\n# remote nodes.\n\n# The java implementation to use.  Required.\nexport JAVA_HOME={{java_home}}\nexport HADOOP_HOME_WARN_SUPPRESS=1\n\n# Hadoop home directory\nexport HADOOP_HOME=${HADOOP_HOME:-{{hadoop_home}}}\n\n# Hadoop Configuration Directory\n\n{# this is different for HDP1 #}\n# Path to jsvc required by secure HDP 2.0 datanode\nexport JSVC_HOME={{jsvc_path}}\n\n\n# The maximum amount of heap to use, in MB. Default is 1000.\nexport HADOOP_HEAPSIZE=\"{{hadoop_heapsize}}\"\n\nexport HADOOP_NAMENODE_INIT_HEAPSIZE=\"-Xms{{namenode_heapsize}}\"\n\n# Extra Java runtime options.  Empty by default.\nexport HADOOP_OPTS=\"-Djava.net.preferIPv4Stack=true ${HADOOP_OPTS}\"\n\n# Command specific options appended to HADOOP_OPTS when specified\nHADOOP_JOBTRACKER_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{jtnode_opt_newsize}} -XX:MaxNewSize={{jtnode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xmx{{jtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dmapred.audit.logger=INFO,MRAUDIT -Dhadoop.mapreduce.jobsummary.logger=INFO,JSA ${HADOOP_JOBTRACKER_OPTS}\"\n\nHADOOP_TASKTRACKER_OPTS=\"-server -Xmx{{ttnode_heapsize}} -Dhadoop.security.logger=ERROR,console -Dmapred.audit.logger=ERROR,console ${HADOOP_TASKTRACKER_OPTS}\"\n\n{% if java_version < 8 %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -XX:PermSize={{namenode_opt_permsize}} -XX:MaxPermSize={{namenode_opt_maxpermsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -XX:PermSize=128m -XX:MaxPermSize=256m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m -XX:MaxPermSize=512m $HADOOP_CLIENT_OPTS\"\n\n{% else %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m $HADOOP_CLIENT_OPTS\"\n{% endif %}\n\nHADOOP_NFS3_OPTS=\"-Xmx{{nfsgateway_heapsize}}m -Dhadoop.security.logger=ERROR,DRFAS ${HADOOP_NFS3_OPTS}\"\nHADOOP_BALANCER_OPTS=\"-server -Xmx{{hadoop_heapsize}}m ${HADOOP_BALANCER_OPTS}\"\n\n\n# On secure datanodes, user to run the datanode as after dropping privileges\nexport HADOOP_SECURE_DN_USER=${HADOOP_SECURE_DN_USER:-{{hadoop_secure_dn_user}}}\n\n# Extra ssh options.  Empty by default.\nexport HADOOP_SSH_OPTS=\"-o ConnectTimeout=5 -o SendEnv=HADOOP_CONF_DIR\"\n\n# Where log files are stored.  $HADOOP_HOME/logs by default.\nexport HADOOP_LOG_DIR={{hdfs_log_dir_prefix}}/$USER\n\n# History server logs\nexport HADOOP_MAPRED_LOG_DIR={{mapred_log_dir_prefix}}/$USER\n\n# Where log files are stored in the secure data environment.\nexport HADOOP_SECURE_DN_LOG_DIR={{hdfs_log_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# File naming remote slave hosts.  $HADOOP_HOME/conf/slaves by default.\n# export HADOOP_SLAVES=${HADOOP_HOME}/conf/slaves\n\n# host:path where hadoop code should be rsync'd from.  Unset by default.\n# export HADOOP_MASTER=master:/home/$USER/src/hadoop\n\n# Seconds to sleep between slave commands.  Unset by default.  This\n# can be useful in large clusters, where, e.g., slave rsyncs can\n# otherwise arrive faster than the master can service them.\n# export HADOOP_SLAVE_SLEEP=0.1\n\n# The directory where pid files are stored. /tmp by default.\nexport HADOOP_PID_DIR={{hadoop_pid_dir_prefix}}/$USER\nexport HADOOP_SECURE_DN_PID_DIR={{hadoop_pid_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# History server pid\nexport HADOOP_MAPRED_PID_DIR={{mapred_pid_dir_prefix}}/$USER\n\nYARN_RESOURCEMANAGER_OPTS=\"-Dyarn.server.resourcemanager.appsummary.logger=INFO,RMSUMMARY\"\n\n# A string representing this instance of hadoop. $USER by default.\nexport HADOOP_IDENT_STRING=$USER\n\n# The scheduling priority for daemon processes.  See 'man nice'.\n\n# export HADOOP_NICENESS=10\n\n# Add database libraries\nJAVA_JDBC_LIBS=\"\"\nif [ -d \"/usr/share/java\" ]; then\n  for jarFile in `ls /usr/share/java | grep -E \"(mysql|ojdbc|postgresql|sqljdbc)\" 2>/dev/null`\n  do\n    JAVA_JDBC_LIBS=${JAVA_JDBC_LIBS}:$jarFile\n  done\nfi\n\n# Add libraries to the hadoop classpath - some may not need a colon as they already include it\nexport HADOOP_CLASSPATH=${HADOOP_CLASSPATH}${JAVA_JDBC_LIBS}\n\n# Setting path to hdfs command line\nexport HADOOP_LIBEXEC_DIR={{hadoop_libexec_dir}}\n\n# Mostly required for hadoop 2.0\nexport JAVA_LIBRARY_PATH=${JAVA_LIBRARY_PATH}\n\nexport HADOOP_OPTS=\"-Dhdp.version=$HDP_VERSION $HADOOP_OPTS\"\n\n{% if is_datanode_max_locked_memory_set %}\n# Fix temporary bug, when ulimit from conf files is not picked up, without full relogin. \n# Makes sense to fix only when runing DN as root \nif [ \"$command\" == \"datanode\" ] && [ \"$EUID\" -eq 0 ] && [ -n \"$HADOOP_SECURE_DN_USER\" ]; then\n  ulimit -l {{datanode_max_locked_memory}}\nfi\n{% endif %}", 
            "namenode_heapsize": "1024m", 
            "namenode_opt_newsize": "128m", 
            "nfsgateway_heapsize": "1024", 
            "dtnode_heapsize": "1024m", 
            "hadoop_root_logger": "INFO,RFA", 
            "hadoop_heapsize": "1024", 
            "hadoop_pid_dir_prefix": "/var/run/hadoop", 
            "namenode_opt_permsize": "128m", 
            "keyserver_port": "",
            "proxyuser_group": "users",
            "hdfs_user_nproc_limit": "65536",
            "hdfs_log_dir_prefix": "/var/log/hadoop",
            "hdfs_user_nofile_limit": "128000",
            "hdfs_user": "hdfs",
            "hdfs_principal_name": "hdfs-test_cluster01@EXAMPLE.COM",
            "keyserver_host": " ",
            "namenode_opt_maxnewsize": "128m",
            "hdfs_user_keytab": "/etc/security/keytabs/hdfs.headless.keytab",
            "namenode_opt_maxpermsize": "256m",
            "content": "\n# Set Hadoop-specific environment variables here.\n\n# The only required environment variable is JAVA_HOME.  All others are\n# optional.  When running a distributed configuration it is best to\n# set JAVA_HOME in this file, so that it is correctly defined on\n# remote nodes.\n\n# The java implementation to use.  Required.\nexport JAVA_HOME={{java_home}}\nexport HADOOP_HOME_WARN_SUPPRESS=1\n\n# Hadoop home directory\nexport HADOOP_HOME=${HADOOP_HOME:-{{hadoop_home}}}\n\n# Hadoop Configuration Directory\n\n{# this is different for HDP1 #}\n# Path to jsvc required by secure HDP 2.0 datanode\nexport JSVC_HOME={{jsvc_path}}\n\n\n# The maximum amount of heap to use, in MB. Default is 1000.\nexport HADOOP_HEAPSIZE=\"{{hadoop_heapsize}}\"\n\nexport HADOOP_NAMENODE_INIT_HEAPSIZE=\"-Xms{{namenode_heapsize}}\"\n\n# Extra Java runtime options.  Empty by default.\nexport HADOOP_OPTS=\"-Djava.net.preferIPv4Stack=true ${HADOOP_OPTS}\"\n\n# Command specific options appended to HADOOP_OPTS when specified\nHADOOP_JOBTRACKER_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{jtnode_opt_newsize}} -XX:MaxNewSize={{jtnode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xmx{{jtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dmapred.audit.logger=INFO,MRAUDIT -Dhadoop.mapreduce.jobsummary.logger=INFO,JSA ${HADOOP_JOBTRACKER_OPTS}\"\n\nHADOOP_TASKTRACKER_OPTS=\"-server -Xmx{{ttnode_heapsize}} -Dhadoop.security.logger=ERROR,console -Dmapred.audit.logger=ERROR,console ${HADOOP_TASKTRACKER_OPTS}\"\n\n{% if java_version < 8 %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -XX:PermSize={{namenode_opt_permsize}} -XX:MaxPermSize={{namenode_opt_maxpermsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -XX:PermSize=128m -XX:MaxPermSize=256m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m -XX:MaxPermSize=512m $HADOOP_CLIENT_OPTS\"\n\n{% else %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m $HADOOP_CLIENT_OPTS\"\n{% endif %}\n\nHADOOP_NFS3_OPTS=\"-Xmx{{nfsgateway_heapsize}}m -Dhadoop.security.logger=ERROR,DRFAS ${HADOOP_NFS3_OPTS}\"\nHADOOP_BALANCER_OPTS=\"-server -Xmx{{hadoop_heapsize}}m ${HADOOP_BALANCER_OPTS}\"\n\n\n# On secure datanodes, user to run the datanode as after dropping privileges\nexport HADOOP_SECURE_DN_USER=${HADOOP_SECURE_DN_USER:-{{hadoop_secure_dn_user}}}\n\n# Extra ssh options.  Empty by default.\nexport HADOOP_SSH_OPTS=\"-o ConnectTimeout=5 -o SendEnv=HADOOP_CONF_DIR\"\n\n# Where log files are stored.  $HADOOP_HOME/logs by default.\nexport HADOOP_LOG_DIR={{hdfs_log_dir_prefix}}/$USER\n\n# History server logs\nexport HADOOP_MAPRED_LOG_DIR={{mapred_log_dir_prefix}}/$USER\n\n# Where log files are stored in the secure data environment.\nexport HADOOP_SECURE_DN_LOG_DIR={{hdfs_log_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# File naming remote slave hosts.  $HADOOP_HOME/conf/slaves by default.\n# export HADOOP_SLAVES=${HADOOP_HOME}/conf/slaves\n\n# host:path where hadoop code should be rsync'd from.  Unset by default.\n# export HADOOP_MASTER=master:/home/$USER/src/hadoop\n\n# Seconds to sleep between slave commands.  Unset by default.  This\n# can be useful in large clusters, where, e.g., slave rsyncs can\n# otherwise arrive faster than the master can service them.\n# export HADOOP_SLAVE_SLEEP=0.1\n\n# The directory where pid files are stored. /tmp by default.\nexport HADOOP_PID_DIR={{hadoop_pid_dir_prefix}}/$USER\nexport HADOOP_SECURE_DN_PID_DIR={{hadoop_pid_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# History server pid\nexport HADOOP_MAPRED_PID_DIR={{mapred_pid_dir_prefix}}/$USER\n\nYARN_RESOURCEMANAGER_OPTS=\"-Dyarn.server.resourcemanager.appsummary.logger=INFO,RMSUMMARY\"\n\n# A string representing this instance of hadoop. $USER by default.\nexport HADOOP_IDENT_STRING=$USER\n\n# The scheduling priority for daemon processes.  See 'man nice'.\n\n# export HADOOP_NICENESS=10\n\n# Add database libraries\nJAVA_JDBC_LIBS=\"\"\nif [ -d \"/usr/share/java\" ]; then\n  for jarFile in `ls /usr/share/java | grep -E \"(mysql|ojdbc|postgresql|sqljdbc)\" 2>/dev/null`\n  do\n    JAVA_JDBC_LIBS=${JAVA_JDBC_LIBS}:$jarFile\n  done\nfi\n\n# Add libraries to the hadoop classpath - some may not need a colon as they already include it\nexport HADOOP_CLASSPATH=${HADOOP_CLASSPATH}${JAVA_JDBC_LIBS}\n\n# Setting path to hdfs command line\nexport HADOOP_LIBEXEC_DIR={{hadoop_libexec_dir}}\n\n# Mostly required for hadoop 2.0\nexport JAVA_LIBRARY_PATH=${JAVA_LIBRARY_PATH}\n\nexport HADOOP_OPTS=\"-Dhdp.version=$HDP_VERSION $HADOOP_OPTS\"\n\n{% if is_datanode_max_locked_memory_set %}\n# Fix temporary bug, when ulimit from conf files is not picked up, without full relogin. \n# Makes sense to fix only when runing DN as root \nif [ \"$command\" == \"datanode\" ] && [ \"$EUID\" -eq 0 ] && [ -n \"$HADOOP_SECURE_DN_USER\" ]; then\n  ulimit -l {{datanode_max_locked_memory}}\nfi\n{% endif %}",
            "namenode_heapsize": "1024m",
            "namenode_opt_newsize": "128m",
            "nfsgateway_heapsize": "1024",
            "dtnode_heapsize": "1024m",
            "hadoop_root_logger": "INFO,RFA",
            "hadoop_heapsize": "1024",
            "hadoop_pid_dir_prefix": "/var/run/hadoop",
            "namenode_opt_permsize": "128m",
             "hdfs_tmp_dir": "/tmp"
        }, 
        },
         "zookeeper-log4j": {
             "content": "\n#\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#   http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing,\n# software distributed under the License is distributed on an\n# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n# KIND, either express or implied.  See the License for the\n# specific language governing permissions and limitations\n# under the License.\n#\n#\n#\n\n#\n# ZooKeeper Logging Configuration\n#\n\n# DEFAULT: console appender only\nlog4j.rootLogger=INFO, CONSOLE\n\n# Example with rolling log file\n#log4j.rootLogger=DEBUG, CONSOLE, ROLLINGFILE\n\n# Example with rolling log file and tracing\n#log4j.rootLogger=TRACE, CONSOLE, ROLLINGFILE, TRACEFILE\n\n#\n# Log INFO level and above messages to the console\n#\nlog4j.appender.CONSOLE=org.apache.log4j.ConsoleAppender\nlog4j.appender.CONSOLE.Threshold=INFO\nlog4j.appender.CONSOLE.layout=org.apache.log4j.PatternLayout\nlog4j.appender.CONSOLE.layout.ConversionPattern=%d{ISO8601} - %-5p [%t:%C{1}@%L] - %m%n\n\n#\n# Add ROLLINGFILE to rootLogger to get log file output\n#    Log DEBUG level and above messages to a log file\nlog4j.appender.ROLLINGFILE=org.apache.log4j.RollingFileAppender\nlog4j.appender.ROLLINGFILE.Threshold=DEBUG\nlog4j.appender.ROLLINGFILE.File=zookeeper.log\n\n# Max log file size of 10MB\nlog4j.appender.ROLLINGFILE.MaxFileSize=10MB\n# uncomment the next line to limit number of backup files\n#log4j.appender.ROLLINGFILE.MaxBackupIndex=10\n\nlog4j.appender.ROLLINGFILE.layout=org.apache.log4j.PatternLayout\nlog4j.appender.ROLLINGFILE.layout.ConversionPattern=%d{ISO8601} - %-5p [%t:%C{1}@%L] - %m%n\n\n\n#\n# Add TRACEFILE to rootLogger to get log file output\n#    Log DEBUG level and above messages to a log file\nlog4j.appender.TRACEFILE=org.apache.log4j.FileAppender\nlog4j.appender.TRACEFILE.Threshold=TRACE\nlog4j.appender.TRACEFILE.File=zookeeper_trace.log\n\nlog4j.appender.TRACEFILE.layout=org.apache.log4j.PatternLayout\n### Notice we are including log4j's NDC here (%x)\nlog4j.appender.TRACEFILE.layout.ConversionPattern=%d{ISO8601} - %-5p [%t:%C{1}@%L][%x] - %m%n"
        }, 
        },
         "ssl-server": {
            "ssl.server.keystore.location": "/etc/security/serverKeys/keystore.jks", 
            "ssl.server.keystore.keypassword": "bigdata", 
            "ssl.server.truststore.location": "/etc/security/serverKeys/all.jks", 
            "ssl.server.keystore.password": "bigdata", 
            "ssl.server.truststore.password": "bigdata", 
            "ssl.server.truststore.type": "jks", 
            "ssl.server.keystore.type": "jks", 
            "ssl.server.keystore.location": "/etc/security/serverKeys/keystore.jks",
            "ssl.server.keystore.keypassword": "bigdata",
            "ssl.server.truststore.location": "/etc/security/serverKeys/all.jks",
            "ssl.server.keystore.password": "bigdata",
            "ssl.server.truststore.password": "bigdata",
            "ssl.server.truststore.type": "jks",
            "ssl.server.keystore.type": "jks",
             "ssl.server.truststore.reload.interval": "10000"
        }, 
        "ranger-site": {}, 
        },
        "ranger-site": {},
         "admin-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#      http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n#\n\n\nlog4j.rootLogger = warn,xa_log_appender\n\n\n# xa_logger\nlog4j.appender.xa_log_appender=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.xa_log_appender.file=${logdir}/xa_portal.log\nlog4j.appender.xa_log_appender.datePattern='.'yyyy-MM-dd\nlog4j.appender.xa_log_appender.append=true\nlog4j.appender.xa_log_appender.layout=org.apache.log4j.PatternLayout\nlog4j.appender.xa_log_appender.layout.ConversionPattern=%d [%t] %-5p %C{6} (%F:%L) - %m%n\n# xa_log_appender : category and additivity\nlog4j.category.org.springframework=warn,xa_log_appender\nlog4j.additivity.org.springframework=false\n\nlog4j.category.org.apache.ranger=info,xa_log_appender\nlog4j.additivity.org.apache.ranger=false\n\nlog4j.category.xa=info,xa_log_appender\nlog4j.additivity.xa=false\n\n# perf_logger\nlog4j.appender.perf_appender=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.perf_appender.file=${logdir}/ranger_admin_perf.log\nlog4j.appender.perf_appender.datePattern='.'yyyy-MM-dd\nlog4j.appender.perf_appender.append=true\nlog4j.appender.perf_appender.layout=org.apache.log4j.PatternLayout\nlog4j.appender.perf_appender.layout.ConversionPattern=%d [%t] %m%n\n\n\n# sql_appender\nlog4j.appender.sql_appender=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.sql_appender.file=${logdir}/xa_portal_sql.log\nlog4j.appender.sql_appender.datePattern='.'yyyy-MM-dd\nlog4j.appender.sql_appender.append=true\nlog4j.appender.sql_appender.layout=org.apache.log4j.PatternLayout\nlog4j.appender.sql_appender.layout.ConversionPattern=%d [%t] %-5p %C{6} (%F:%L) - %m%n\n\n# sql_appender : category and additivity\nlog4j.category.org.hibernate.SQL=warn,sql_appender\nlog4j.additivity.org.hibernate.SQL=false\n\nlog4j.category.jdbc.sqlonly=fatal,sql_appender\nlog4j.additivity.jdbc.sqlonly=false\n\nlog4j.category.jdbc.sqltiming=warn,sql_appender\nlog4j.additivity.jdbc.sqltiming=false\n\nlog4j.category.jdbc.audit=fatal,sql_appender\nlog4j.additivity.jdbc.audit=false\n\nlog4j.category.jdbc.resultset=fatal,sql_appender\nlog4j.additivity.jdbc.resultset=false\n\nlog4j.category.jdbc.connection=fatal,sql_appender\nlog4j.additivity.jdbc.connection=false"
        }, 
        },
         "tagsync-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#      http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n#\n\n\nlog4j.rootLogger = info,logFile\n\n# logFile\nlog4j.appender.logFile=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.logFile.file=${logdir}/tagsync.log\nlog4j.appender.logFile.datePattern='.'yyyy-MM-dd\nlog4j.appender.logFile.layout=org.apache.log4j.PatternLayout\nlog4j.appender.logFile.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %L %m%n\n\n# console\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.Target=System.out\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %L %m%n"
        }, 
        },
         "ranger-hdfs-security": {
            "ranger.plugin.hdfs.policy.source.impl": "org.apache.ranger.admin.client.RangerAdminRESTClient", 
            "ranger.plugin.hdfs.service.name": "{{repo_name}}", 
            "ranger.plugin.hdfs.policy.cache.dir": "/etc/ranger/{{repo_name}}/policycache", 
            "ranger.plugin.hdfs.policy.pollIntervalMs": "30000", 
            "ranger.plugin.hdfs.policy.rest.url": "{{policymgr_mgr_url}}", 
            "ranger.plugin.hdfs.policy.rest.ssl.config.file": "/etc/hadoop/conf/ranger-policymgr-ssl.xml", 
            "ranger.plugin.hdfs.policy.source.impl": "org.apache.ranger.admin.client.RangerAdminRESTClient",
            "ranger.plugin.hdfs.service.name": "{{repo_name}}",
            "ranger.plugin.hdfs.policy.cache.dir": "/etc/ranger/{{repo_name}}/policycache",
            "ranger.plugin.hdfs.policy.pollIntervalMs": "30000",
            "ranger.plugin.hdfs.policy.rest.url": "{{policymgr_mgr_url}}",
            "ranger.plugin.hdfs.policy.rest.ssl.config.file": "/etc/hadoop/conf/ranger-policymgr-ssl.xml",
             "xasecure.add-hadoop-authorization": "true"
        }, 
        "usersync-properties": {}, 
        },
        "usersync-properties": {},
         "zookeeper-env": {
            "zk_server_heapsize": "1024m", 
            "zookeeper_keytab_path": "/etc/security/keytabs/zk.service.keytab", 
            "zk_user": "zookeeper", 
            "zk_log_dir": "/var/log/zookeeper", 
            "content": "\nexport JAVA_HOME={{java64_home}}\nexport ZOOKEEPER_HOME={{zk_home}}\nexport ZOO_LOG_DIR={{zk_log_dir}}\nexport ZOOPIDFILE={{zk_pid_file}}\nexport SERVER_JVMFLAGS={{zk_server_heapsize}}\nexport JAVA=$JAVA_HOME/bin/java\nexport CLASSPATH=$CLASSPATH:/usr/share/zookeeper/*\n\n{% if security_enabled %}\nexport SERVER_JVMFLAGS=\"$SERVER_JVMFLAGS -Djava.security.auth.login.config={{zk_server_jaas_file}}\"\nexport CLIENT_JVMFLAGS=\"$CLIENT_JVMFLAGS -Djava.security.auth.login.config={{zk_client_jaas_file}}\"\n{% endif %}", 
            "zk_pid_dir": "/var/run/zookeeper", 
            "zk_server_heapsize": "1024m",
            "zookeeper_keytab_path": "/etc/security/keytabs/zk.service.keytab",
            "zk_user": "zookeeper",
            "zk_log_dir": "/var/log/zookeeper",
            "content": "\nexport JAVA_HOME={{java64_home}}\nexport ZOOKEEPER_HOME={{zk_home}}\nexport ZOO_LOG_DIR={{zk_log_dir}}\nexport ZOOPIDFILE={{zk_pid_file}}\nexport SERVER_JVMFLAGS={{zk_server_heapsize}}\nexport JAVA=$JAVA_HOME/bin/java\nexport CLASSPATH=$CLASSPATH:/usr/share/zookeeper/*\n\n{% if security_enabled %}\nexport SERVER_JVMFLAGS=\"$SERVER_JVMFLAGS -Djava.security.auth.login.config={{zk_server_jaas_file}}\"\nexport CLIENT_JVMFLAGS=\"$CLIENT_JVMFLAGS -Djava.security.auth.login.config={{zk_client_jaas_file}}\"\n{% endif %}",
            "zk_pid_dir": "/var/run/zookeeper",
             "zookeeper_principal_name": "zookeeper/_HOST@EXAMPLE.COM"
         },
         "infra-solr-env": {
@@ -718,7 +718,7 @@
             "infra_solr_kerberos_name_rules": "DEFAULT",
             "infra_solr_user": "infra-solr",
             "infra_solr_maxmem": "1024",
            "content": "#!/bin/bash\n# Licensed to the Apache Software Foundation (ASF) under one or more\n# contributor license agreements. See the NOTICE file distributed with\n# this work for additional information regarding copyright ownership.\n# The ASF licenses this file to You under the Apache License, Version 2.0\n# (the \"License\"); you may not use this file except in compliance with\n# the License. You may obtain a copy of the License at\n#\n# http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n\n# By default the script will use JAVA_HOME to determine which java\n# to use, but you can set a specific path for Solr to use without\n# affecting other Java applications on your server/workstation.\nSOLR_JAVA_HOME={{java64_home}}\n\n# Increase Java Min/Max Heap as needed to support your indexing / query needs\nSOLR_JAVA_MEM=\"-Xms{{logsearch_solr_min_mem}}m -Xmx{{logsearch_solr_max_mem}}m\"\n\n# Enable verbose GC logging\nGC_LOG_OPTS=\"-verbose:gc -XX:+PrintHeapAtGC -XX:+PrintGCDetails \\\n-XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationStoppedTime\"\n\n# These GC settings have shown to work well for a number of common Solr workloads\nGC_TUNE=\"-XX:NewRatio=3 \\\n-XX:SurvivorRatio=4 \\\n-XX:TargetSurvivorRatio=90 \\\n-XX:MaxTenuringThreshold=8 \\\n-XX:+UseConcMarkSweepGC \\\n-XX:+UseParNewGC \\\n-XX:ConcGCThreads=4 -XX:ParallelGCThreads=4 \\\n-XX:+CMSScavengeBeforeRemark \\\n-XX:PretenureSizeThreshold=64m \\\n-XX:+UseCMSInitiatingOccupancyOnly \\\n-XX:CMSInitiatingOccupancyFraction=50 \\\n-XX:CMSMaxAbortablePrecleanTime=6000 \\\n-XX:+CMSParallelRemarkEnabled \\\n-XX:+ParallelRefProcEnabled\"\n\n# Set the ZooKeeper connection string if using an external ZooKeeper ensemble\n# e.g. host1:2181,host2:2181/chroot\n# Leave empty if not using SolrCloud\nZK_HOST=\"{{zookeeper_quorum}}{{logsearch_solr_znode}}\"\n\n# Set the ZooKeeper client timeout (for SolrCloud mode)\nZK_CLIENT_TIMEOUT=\"60000\"\n\n# By default the start script uses \"localhost\"; override the hostname here\n# for production SolrCloud environments to control the hostname exposed to cluster state\n#SOLR_HOST=\"192.168.1.1\"\n\n# By default the start script uses UTC; override the timezone if needed\n#SOLR_TIMEZONE=\"UTC\"\n\n# Set to true to activate the JMX RMI connector to allow remote JMX client applications\n# to monitor the JVM hosting Solr; set to \"false\" to disable that behavior\n# (false is recommended in production environments)\nENABLE_REMOTE_JMX_OPTS=\"true\"\n\n# The script will use SOLR_PORT+10000 for the RMI_PORT or you can set it here\nRMI_PORT={{logsearch_solr_jmx_port}}\n\n# Anything you add to the SOLR_OPTS variable will be included in the java\n# start command line as-is, in ADDITION to other options. If you specify the\n# -a option on start script, those options will be appended as well. Examples:\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.autoSoftCommit.maxTime=3000\"\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.autoCommit.maxTime=60000\"\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.clustering.enabled=true\"\n\n# Location where the bin/solr script will save PID files for running instances\n# If not set, the script will create PID files in $SOLR_TIP/bin\nSOLR_PID_DIR={{logsearch_solr_piddir}}\n\n# Path to a directory where Solr creates index files, the specified directory\n# must contain a solr.xml; by default, Solr will use server/solr\nSOLR_HOME={{logsearch_solr_datadir}}\n\n# Solr provides a default Log4J configuration properties file in server/resources\n# however, you may want to customize the log settings and file appender location\n# so you can point the script to use a different log4j.properties file\nLOG4J_PROPS={{logsearch_solr_conf}}/log4j.properties\n\n# Location where Solr should write logs to; should agree with the file appender\n# settings in server/resources/log4j.properties\nSOLR_LOGS_DIR={{logsearch_solr_log_dir}}\n\n# Sets the port Solr binds to, default is 8983\nSOLR_PORT={{logsearch_solr_port}}\n\n# Be sure to update the paths to the correct keystore for your environment\n{% if logsearch_solr_ssl_enabled %}\nSOLR_SSL_KEY_STORE={{logsearch_solr_keystore_location}}\nSOLR_SSL_KEY_STORE_PASSWORD={{logsearch_solr_keystore_password}}\nSOLR_SSL_TRUST_STORE={{logsearch_solr_keystore_location}}\nSOLR_SSL_TRUST_STORE_PASSWORD={{logsearch_solr_keystore_password}}\nSOLR_SSL_NEED_CLIENT_AUTH=false\nSOLR_SSL_WANT_CLIENT_AUTH=false\n{% endif %}\n\n# Uncomment to set a specific SSL port (-Djetty.ssl.port=N); if not set\n# and you are using SSL, then the start script will use SOLR_PORT for the SSL port\n#SOLR_SSL_PORT=\n\n{% if security_enabled -%}\nSOLR_HOST=`hostname -f`\nSOLR_JAAS_FILE={{logsearch_solr_jaas_file}}\nSOLR_KERB_KEYTAB={{logsearch_solr_web_kerberos_keytab}}\nSOLR_KERB_PRINCIPAL={{logsearch_solr_web_kerberos_principal}}\nSOLR_KERB_NAME_RULES={{logsearch_solr_kerberos_name_rules}}\n\nSOLR_AUTHENTICATION_CLIENT_CONFIGURER=\"org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer\"\nSOLR_AUTHENTICATION_OPTS=\" -DauthenticationPlugin=org.apache.solr.security.KerberosPlugin -Djava.security.auth.login.config=$SOLR_JAAS_FILE -Dsolr.kerberos.principal=${SOLR_KERB_PRINCIPAL} -Dsolr.kerberos.keytab=${SOLR_KERB_KEYTAB} -Dsolr.kerberos.cookie.domain=${SOLR_HOST} -Dsolr.kerberos.name.rules=${SOLR_KERB_NAME_RULES}\"\n{% endif %}", 
            "content": "#!/bin/bash\n# Licensed to the Apache Software Foundation (ASF) under one or more\n# contributor license agreements. See the NOTICE file distributed with\n# this work for additional information regarding copyright ownership.\n# The ASF licenses this file to You under the Apache License, Version 2.0\n# (the \"License\"); you may not use this file except in compliance with\n# the License. You may obtain a copy of the License at\n#\n# http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n\n# By default the script will use JAVA_HOME to determine which java\n# to use, but you can set a specific path for Solr to use without\n# affecting other Java applications on your server/workstation.\nSOLR_JAVA_HOME={{java64_home}}\n\n# Increase Java Min/Max Heap as needed to support your indexing / query needs\nSOLR_JAVA_MEM=\"-Xms{{logsearch_solr_min_mem}}m -Xmx{{logsearch_solr_max_mem}}m\"\n\n# Enable verbose GC logging\nGC_LOG_OPTS=\"-verbose:gc -XX:+PrintHeapAtGC -XX:+PrintGCDetails \\\n-XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationStoppedTime\"\n\n# These GC settings have shown to work well for a number of common Solr workloads\nGC_TUNE=\"-XX:NewRatio=3 \\\n-XX:SurvivorRatio=4 \\\n-XX:TargetSurvivorRatio=90 \\\n-XX:MaxTenuringThreshold=8 \\\n-XX:+UseConcMarkSweepGC \\\n-XX:+UseParNewGC \\\n-XX:ConcGCThreads=4 -XX:ParallelGCThreads=4 \\\n-XX:+CMSScavengeBeforeRemark \\\n-XX:PretenureSizeThreshold=64m \\\n-XX:+UseCMSInitiatingOccupancyOnly \\\n-XX:CMSInitiatingOccupancyFraction=50 \\\n-XX:CMSMaxAbortablePrecleanTime=6000 \\\n-XX:+CMSParallelRemarkEnabled \\\n-XX:+ParallelRefProcEnabled\"\n\n# Set the ZooKeeper connection string if using an external ZooKeeper ensemble\n# e.g. host1:2181,host2:2181/chroot\n# Leave empty if not using SolrCloud\nZK_HOST=\"{{zookeeper_quorum}}{{logsearch_solr_znode}}\"\n\n# Set the ZooKeeper client timeout (for SolrCloud mode)\nZK_CLIENT_TIMEOUT=\"60000\"\n\n# By default the start script uses \"localhost\"; override the hostname here\n# for production SolrCloud environments to control the hostname exposed to cluster state\n#SOLR_HOST=\"192.168.1.1\"\n\n# By default the start script uses UTC; override the timezone if needed\n#SOLR_TIMEZONE=\"UTC\"\n\n# Set to true to activate the JMX RMI connector to allow remote JMX client applications\n# to monitor the JVM hosting Solr; set to \"false\" to disable that behavior\n# (false is recommended in production environments)\nENABLE_REMOTE_JMX_OPTS=\"true\"\n\n# The script will use SOLR_PORT+10000 for the RMI_PORT or you can set it here\nRMI_PORT={{logsearch_solr_jmx_port}}\n\n# Anything you add to the SOLR_OPTS variable will be included in the java\n# start command line as-is, in ADDITION to other options. If you specify the\n# -a option on start script, those options will be appended as well. Examples:\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.autoSoftCommit.maxTime=3000\"\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.autoCommit.maxTime=60000\"\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.clustering.enabled=true\"\n\n# Location where the bin/solr script will save PID files for running instances\n# If not set, the script will create PID files in $SOLR_TIP/bin\nSOLR_PID_DIR={{logsearch_solr_piddir}}\n\n# Path to a directory where Solr creates index files, the specified directory\n# must contain a solr.xml; by default, Solr will use server/solr\nSOLR_HOME={{logsearch_solr_datadir}}\n\n# Solr provides a default Log4J configuration properties file in server/resources\n# however, you may want to customize the log settings and file appender location\n# so you can point the script to use a different log4j.properties file\nLOG4J_PROPS={{logsearch_solr_conf}}/log4j.properties\n\n# Location where Solr should write logs to; should agree with the file appender\n# settings in server/resources/log4j.properties\nSOLR_LOGS_DIR={{logsearch_solr_log_dir}}\n\n# Sets the port Solr binds to, default is 8983\nSOLR_PORT={{logsearch_solr_port}}\n\n# Be sure to update the paths to the correct keystore for your environment\n{% if logsearch_solr_ssl_enabled %}\nSOLR_SSL_KEY_STORE={{logsearch_solr_keystore_location}}\nSOLR_SSL_KEY_STORE_PASSWORD={{logsearch_solr_keystore_password}}\nSOLR_SSL_TRUST_STORE={{logsearch_solr_keystore_location}}\nSOLR_SSL_TRUST_STORE_PASSWORD={{logsearch_solr_keystore_password}}\nSOLR_SSL_NEED_CLIENT_AUTH=false\nSOLR_SSL_WANT_CLIENT_AUTH=false\n{% endif %}\n\n# Uncomment to set a specific SSL port (-Djetty.ssl.port=N); if not set\n# and you are using SSL, then the start script will use SOLR_PORT for the SSL port\n#SOLR_SSL_PORT=\n\n{% if security_enabled -%}\nSOLR_HOST=`hostname -f`\nSOLR_JAAS_FILE={{logsearch_solr_jaas_file}}\nSOLR_KERB_KEYTAB={{logsearch_solr_web_kerberos_keytab}}\nSOLR_KERB_PRINCIPAL={{logsearch_solr_web_kerberos_principal}}\nSOLR_KERB_NAME_RULES={{logsearch_solr_kerberos_name_rules}}\n\nSOLR_AUTHENTICATION_CLIENT_CONFIGURER=\"org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer\"\nSOLR_AUTHENTICATION_OPTS=\" -DauthenticationPlugin=org.apache.solr.security.KerberosPlugin -Djava.security.auth.login.config=$SOLR_JAAS_FILE -Dsolr.kerberos.principal=${SOLR_KERB_PRINCIPAL} -Dsolr.kerberos.keytab=${SOLR_KERB_KEYTAB} -Dsolr.kerberos.cookie.domain=${SOLR_HOST} -Dsolr.kerberos.name.rules=${SOLR_KERB_NAME_RULES}\"\n{% endif %}",
             "infra_solr_pid_dir": "/var/run/ambari-infra-solr",
             "infra_solr_truststore_password": "bigdata",
             "infra_solr_truststore_type": "jks",
@@ -742,32 +742,32 @@
             "content": "content"
         },
         "cluster-env": {
            "security_enabled": "true", 
            "override_uid": "true", 
            "fetch_nonlocal_groups": "true", 
            "one_dir_per_partition": "true", 
            "repo_ubuntu_template": "{{package_type}} {{base_url}} {{components}}", 
            "ignore_groupsusers_create": "false", 
            "alerts_repeat_tolerance": "1", 
            "smokeuser_keytab": "/etc/security/keytabs/smokeuser.headless.keytab", 
            "kerberos_domain": "EXAMPLE.COM", 
            "security_enabled": "true",
            "override_uid": "true",
            "fetch_nonlocal_groups": "true",
            "one_dir_per_partition": "true",
            "repo_ubuntu_template": "{{package_type}} {{base_url}} {{components}}",
            "ignore_groupsusers_create": "false",
            "alerts_repeat_tolerance": "1",
            "smokeuser_keytab": "/etc/security/keytabs/smokeuser.headless.keytab",
            "kerberos_domain": "EXAMPLE.COM",
             "manage_dirs_on_root": "true",
            "recovery_lifetime_max_count": "1024", 
            "recovery_type": "AUTO_START", 
            "ignore_bad_mounts": "false", 
            "recovery_window_in_minutes": "60", 
            "user_group": "hadoop", 
            "stack_tools": "{\n  \"stack_selector\": [\"hdp-select\", \"/usr/bin/hdp-select\", \"hdp-select\"],\n  \"conf_selector\": [\"conf-select\", \"/usr/bin/conf-select\", \"conf-select\"]\n}", 
            "recovery_retry_interval": "5", 
            "stack_features": "{\n  \"stack_features\": [\n    {\n      \"name\": \"snappy\",\n      \"description\": \"Snappy compressor/decompressor support\",\n      \"min_version\": \"2.0.0.0\",\n      \"max_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"lzo\",\n      \"description\": \"LZO libraries support\",\n      \"min_version\": \"2.2.1.0\"\n    },\n    {\n      \"name\": \"express_upgrade\",\n      \"description\": \"Express upgrade support\",\n      \"min_version\": \"2.1.0.0\"\n    },\n    {\n      \"name\": \"rolling_upgrade\",\n      \"description\": \"Rolling upgrade support\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"config_versioning\",\n      \"description\": \"Configurable versions support\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"datanode_non_root\",\n      \"description\": \"DataNode running as non-root support (AMBARI-7615)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"remove_ranger_hdfs_plugin_env\",\n      \"description\": \"HDFS removes Ranger env files (AMBARI-14299)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"ranger\",\n      \"description\": \"Ranger Service support\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"ranger_tagsync_component\",\n      \"description\": \"Ranger Tagsync component support (AMBARI-14383)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"phoenix\",\n      \"description\": \"Phoenix Service support\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"nfs\",\n      \"description\": \"NFS support\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"tez_for_spark\",\n      \"description\": \"Tez dependency for Spark\",\n      \"min_version\": \"2.2.0.0\",\n      \"max_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"timeline_state_store\",\n      \"description\": \"Yarn application timeline-service supports state store property (AMBARI-11442)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"copy_tarball_to_hdfs\",\n      \"description\": \"Copy tarball to HDFS support (AMBARI-12113)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"spark_16plus\",\n      \"description\": \"Spark 1.6+\",\n      \"min_version\": \"2.4.0.0\"\n    },\n    {\n      \"name\": \"spark_thriftserver\",\n      \"description\": \"Spark Thrift Server\",\n      \"min_version\": \"2.3.2.0\"\n    },\n    {\n      \"name\": \"storm_kerberos\",\n      \"description\": \"Storm Kerberos support (AMBARI-7570)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"storm_ams\",\n      \"description\": \"Storm AMS integration (AMBARI-10710)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"create_kafka_broker_id\",\n      \"description\": \"Ambari should create Kafka Broker Id (AMBARI-12678)\",\n      \"min_version\": \"2.2.0.0\",\n      \"max_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"kafka_listeners\",\n      \"description\": \"Kafka listeners (AMBARI-10984)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"kafka_kerberos\",\n      \"description\": \"Kafka Kerberos support (AMBARI-10984)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"pig_on_tez\",\n      \"description\": \"Pig on Tez support (AMBARI-7863)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"ranger_usersync_non_root\",\n      \"description\": \"Ranger Usersync as non-root user (AMBARI-10416)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"ranger_audit_db_support\",\n      \"description\": \"Ranger Audit to DB support\",\n      \"min_version\": \"2.2.0.0\",\n      \"max_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"accumulo_kerberos_user_auth\",\n      \"description\": \"Accumulo Kerberos User Auth (AMBARI-10163)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"knox_versioned_data_dir\",\n      \"description\": \"Use versioned data dir for Knox (AMBARI-13164)\",\n      \"min_version\": \"2.3.2.0\"\n    },\n    {\n      \"name\": \"knox_sso_topology\",\n      \"description\": \"Knox SSO Topology support (AMBARI-13975)\",\n      \"min_version\": \"2.3.8.0\"\n    },\n    {\n      \"name\": \"atlas_rolling_upgrade\",\n      \"description\": \"Rolling upgrade support for Atlas\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"oozie_admin_user\",\n      \"description\": \"Oozie install user as an Oozie admin user (AMBARI-7976)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"oozie_create_hive_tez_configs\",\n      \"description\": \"Oozie create configs for Ambari Hive and Tez deployments (AMBARI-8074)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"oozie_setup_shared_lib\",\n      \"description\": \"Oozie setup tools used to shared Oozie lib to HDFS (AMBARI-7240)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"oozie_host_kerberos\",\n      \"description\": \"Oozie in secured clusters uses _HOST in Kerberos principal (AMBARI-9775)\",\n      \"min_version\": \"2.0.0.0\",\n      \"max_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"falcon_extensions\",\n      \"description\": \"Falcon Extension\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hive_metastore_upgrade_schema\",\n      \"description\": \"Hive metastore upgrade schema support (AMBARI-11176)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"hive_server_interactive\",\n      \"description\": \"Hive server interactive support (AMBARI-15573)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hive_webhcat_specific_configs\",\n      \"description\": \"Hive webhcat specific configurations support (AMBARI-12364)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"hive_purge_table\",\n      \"description\": \"Hive purge table support (AMBARI-12260)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"hive_server2_kerberized_env\",\n      \"description\": \"Hive server2 working on kerberized environment (AMBARI-13749)\",\n      \"min_version\": \"2.2.3.0\",\n      \"max_version\": \"2.2.5.0\"\n    },\n    {\n      \"name\": \"hive_env_heapsize\",\n      \"description\": \"Hive heapsize property defined in hive-env (AMBARI-12801)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"ranger_kms_hsm_support\",\n      \"description\": \"Ranger KMS HSM support (AMBARI-15752)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_log4j_support\",\n      \"description\": \"Ranger supporting log-4j properties (AMBARI-15681)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_kerberos_support\",\n      \"description\": \"Ranger Kerberos support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hive_metastore_site_support\",\n      \"description\": \"Hive Metastore site support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_usersync_password_jceks\",\n      \"description\": \"Saving Ranger Usersync credentials in jceks\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_install_infra_client\",\n      \"description\": \"LogSearch Service support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hbase_home_directory\",\n      \"description\": \"Hbase home directory in HDFS needed for HBASE backup\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"spark_livy\",\n      \"description\": \"Livy as slave component of spark\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"atlas_ranger_plugin_support\",\n      \"description\": \"Atlas Ranger plugin support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_pid_support\",\n      \"description\": \"Ranger Service support pid generation AMBARI-16756\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_kms_pid_support\",\n      \"description\": \"Ranger KMS Service support pid generation\",\n      \"min_version\": \"2.5.0.0\"\n    }\n  ]\n}",
            "recovery_enabled": "true", 
            "smokeuser_principal_name": "ambari-qa-test_cluster01@EXAMPLE.COM", 
            "recovery_max_count": "6", 
            "stack_root": "/usr/hdp", 
            "repo_suse_rhel_template": "[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0", 
            "ambari_principal_name": "ambari-server-test_cluster01@EXAMPLE.COM", 
            "managed_hdfs_resource_property_names": "", 
            "recovery_lifetime_max_count": "1024",
            "recovery_type": "AUTO_START",
            "ignore_bad_mounts": "false",
            "recovery_window_in_minutes": "60",
            "user_group": "hadoop",
            "stack_name": "HDP",
            "stack_root": "{\"HDP\": \"/usr/hdp\"}",
            "stack_tools": "{\n \"HDP\": { \"stack_selector\": [\"hdp-select\", \"/usr/bin/hdp-select\", \"hdp-select\"],\n  \"conf_selector\": [\"conf-select\", \"/usr/bin/conf-select\", \"conf-select\"]\n}\n}",
            "recovery_retry_interval": "5",
            "recovery_enabled": "true",
            "smokeuser_principal_name": "ambari-qa-test_cluster01@EXAMPLE.COM",
            "recovery_max_count": "6",
            "repo_suse_rhel_template": "[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0",
            "ambari_principal_name": "ambari-server-test_cluster01@EXAMPLE.COM",
            "managed_hdfs_resource_property_names": "",
             "smokeuser": "ambari-qa"
         }
     }
}
\ No newline at end of file
}
diff --git a/ambari-server/src/test/python/stacks/2.5/configs/ranger-kms-default.json b/ambari-server/src/test/python/stacks/2.5/configs/ranger-kms-default.json
index 05cb78a42b..cafbeded90 100644
-- a/ambari-server/src/test/python/stacks/2.5/configs/ranger-kms-default.json
++ b/ambari-server/src/test/python/stacks/2.5/configs/ranger-kms-default.json
@@ -1,55 +1,55 @@
 {
     "localComponents": [
        "SECONDARY_NAMENODE", 
        "HDFS_CLIENT", 
        "DATANODE", 
        "NAMENODE", 
        "ZOOKEEPER_SERVER", 
        "ZOOKEEPER_CLIENT", 
        "RANGER_USERSYNC", 
        "RANGER_ADMIN", 
        "SECONDARY_NAMENODE",
        "HDFS_CLIENT",
        "DATANODE",
        "NAMENODE",
        "ZOOKEEPER_SERVER",
        "ZOOKEEPER_CLIENT",
        "RANGER_USERSYNC",
        "RANGER_ADMIN",
         "RANGER_TAGSYNC",
         "RANGER_KMS_SERVER"
    ], 
    ],
     "configuration_attributes": {
        "ranger-hdfs-audit": {}, 
        "ssl-client": {}, 
        "ranger-admin-site": {}, 
        "ranger-hdfs-policymgr-ssl": {}, 
        "tagsync-application-properties": {}, 
        "ranger-env": {}, 
        "usersync-log4j": {}, 
        "admin-properties": {}, 
        "ranger-ugsync-site": {}, 
        "ranger-hdfs-audit": {},
        "ssl-client": {},
        "ranger-admin-site": {},
        "ranger-hdfs-policymgr-ssl": {},
        "tagsync-application-properties": {},
        "ranger-env": {},
        "usersync-log4j": {},
        "admin-properties": {},
        "ranger-ugsync-site": {},
         "hdfs-site": {
             "final": {
                "dfs.datanode.data.dir": "true", 
                "dfs.namenode.http-address": "true", 
                "dfs.datanode.failed.volumes.tolerated": "true", 
                "dfs.support.append": "true", 
                "dfs.namenode.name.dir": "true", 
                "dfs.datanode.data.dir": "true",
                "dfs.namenode.http-address": "true",
                "dfs.datanode.failed.volumes.tolerated": "true",
                "dfs.support.append": "true",
                "dfs.namenode.name.dir": "true",
                 "dfs.webhdfs.enabled": "true"
             }
        }, 
        "ranger-tagsync-site": {}, 
        "zoo.cfg": {}, 
        "hadoop-policy": {}, 
        "hdfs-log4j": {}, 
        "ranger-hdfs-plugin-properties": {}, 
        },
        "ranger-tagsync-site": {},
        "zoo.cfg": {},
        "hadoop-policy": {},
        "hdfs-log4j": {},
        "ranger-hdfs-plugin-properties": {},
         "core-site": {
             "final": {
                 "fs.defaultFS": "true"
             }
        }, 
        "hadoop-env": {}, 
        "zookeeper-log4j": {}, 
        "ssl-server": {}, 
        "ranger-site": {}, 
        "admin-log4j": {}, 
        "tagsync-log4j": {}, 
        "ranger-hdfs-security": {}, 
        "usersync-properties": {}, 
        "zookeeper-env": {}, 
        },
        "hadoop-env": {},
        "zookeeper-log4j": {},
        "ssl-server": {},
        "ranger-site": {},
        "admin-log4j": {},
        "tagsync-log4j": {},
        "ranger-hdfs-security": {},
        "usersync-properties": {},
        "zookeeper-env": {},
         "cluster-env": {},
         "dbks-site": {},
         "kms-env": {},
@@ -60,744 +60,744 @@
         "ranger-kms-site": {},
         "ranger-kms-policymgr-ssl": {},
         "ranger-kms-audit": {}
    }, 
    "public_hostname": "c6401.ambari.apache.org", 
    "commandId": "9-1", 
    "hostname": "c6401.ambari.apache.org", 
    "kerberosCommandParams": [], 
    "serviceName": "RANGER_KMS", 
    "role": "RANGER_KMS_SERVER", 
    "forceRefreshConfigTagsBeforeExecution": [], 
    "requestId": 9, 
    },
    "public_hostname": "c6401.ambari.apache.org",
    "commandId": "9-1",
    "hostname": "c6401.ambari.apache.org",
    "kerberosCommandParams": [],
    "serviceName": "RANGER_KMS",
    "role": "RANGER_KMS_SERVER",
    "forceRefreshConfigTagsBeforeExecution": [],
    "requestId": 9,
     "agentConfigParams": {
         "agent": {
             "parallel_execution": 0
         }
    }, 
    "clusterName": "c1", 
    "commandType": "EXECUTION_COMMAND", 
    "taskId": 64, 
    "roleParams": {}, 
    },
    "clusterName": "c1",
    "commandType": "EXECUTION_COMMAND",
    "taskId": 64,
    "roleParams": {},
     "configurationTags": {
         "ranger-hdfs-audit": {
             "tag": "version1466427664617"
        }, 
        },
         "ssl-client": {
             "tag": "version1"
        }, 
        },
         "ranger-admin-site": {
             "tag": "version1466427664621"
        }, 
        },
         "ranger-hdfs-policymgr-ssl": {
             "tag": "version1466427664617"
        }, 
        },
         "tagsync-application-properties": {
             "tag": "version1466427664621"
        }, 
        },
         "ranger-env": {
             "tag": "version1466427664621"
        }, 
        },
         "usersync-log4j": {
             "tag": "version1466427664621"
        }, 
        },
         "admin-properties": {
             "tag": "version1466427664621"
        }, 
        },
         "ranger-ugsync-site": {
             "tag": "version1466427664621"
        }, 
        },
         "hdfs-site": {
             "tag": "version1"
        }, 
        },
         "ranger-tagsync-site": {
             "tag": "version1466427664621"
        }, 
        },
         "zoo.cfg": {
             "tag": "version1"
        }, 
        },
         "hadoop-policy": {
             "tag": "version1"
        }, 
        },
         "hdfs-log4j": {
             "tag": "version1"
        }, 
        },
         "ranger-hdfs-plugin-properties": {
             "tag": "version1466427664617"
        }, 
        },
         "core-site": {
             "tag": "version1"
        }, 
        },
         "hadoop-env": {
             "tag": "version1"
        }, 
        },
         "zookeeper-log4j": {
             "tag": "version1"
        }, 
        },
         "ssl-server": {
             "tag": "version1"
        }, 
        },
         "ranger-site": {
             "tag": "version1466427664621"
        }, 
        },
         "admin-log4j": {
             "tag": "version1466427664621"
        }, 
        },
         "tagsync-log4j": {
             "tag": "version1466427664621"
        }, 
        },
         "ranger-hdfs-security": {
             "tag": "version1466427664617"
        }, 
        },
         "usersync-properties": {
             "tag": "version1466427664621"
        }, 
        },
         "zookeeper-env": {
             "tag": "version1"
        }, 
        },
         "cluster-env": {
             "tag": "version1"
         },
         "dbks-site": {
            "tag": "version1"            
            "tag": "version1"
             },
         "kms-env": {
            "tag": "version1"            
            "tag": "version1"
             },
         "kms-log4j": {
            "tag": "version1"            
            "tag": "version1"
             },
         "kms-properties": {
            "tag": "version1"            
            "tag": "version1"
             },
         "kms-site": {
            "tag": "version1"            
            "tag": "version1"
             },
         "ranger-kms-security": {
            "tag": "version1"            
            "tag": "version1"
             },
         "ranger-kms-site": {
            "tag": "version1"            
            "tag": "version1"
             },
         "ranger-kms-policymgr-ssl": {
            "tag": "version1"            
            "tag": "version1"
             },
         "ranger-kms-audit": {
            "tag": "version1"            
            "tag": "version1"
         }
    }, 
    "roleCommand": "START", 
    },
    "roleCommand": "START",
     "hostLevelParams": {
        "agent_stack_retry_on_unavailability": "false", 
        "stack_name": "HDP", 
        "agent_stack_retry_on_unavailability": "false",
        "stack_name": "HDP",
         "custom_mysql_jdbc_name": "mysql-connector-java.jar",
         "previous_custom_mysql_jdbc_name": "mysql-connector-java-old.jar",
        "host_sys_prepped": "false", 
        "ambari_db_rca_username": "mapred", 
        "current_version": "2.5.0.0-777", 
        "mysql_jdbc_url": "http://c6401.ambari.apache.org:8080/resources//mysql-connector-java.jar", 
        "agent_stack_retry_count": "5", 
        "stack_version": "2.5", 
        "jdk_name": "jdk-8u60-linux-x64.tar.gz", 
        "ambari_db_rca_driver": "org.postgresql.Driver", 
        "host_sys_prepped": "false",
        "ambari_db_rca_username": "mapred",
        "current_version": "2.5.0.0-777",
        "mysql_jdbc_url": "http://c6401.ambari.apache.org:8080/resources//mysql-connector-java.jar",
        "agent_stack_retry_count": "5",
        "stack_version": "2.5",
        "jdk_name": "jdk-8u60-linux-x64.tar.gz",
        "ambari_db_rca_driver": "org.postgresql.Driver",
         "java_home": "/usr/jdk64/jdk1.7.0_45",
        "repository_version_id": "1", 
        "jdk_location": "http://c6401.ambari.apache.org:8080/resources/", 
        "not_managed_hdfs_path_list": "[\"/tmp\"]", 
        "ambari_db_rca_url": "jdbc:postgresql://c6401.ambari.apache.org/ambarirca", 
        "java_version": "8", 
        "repo_info": "[{\"baseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.5.0.0-777\",\"osType\":\"redhat6\",\"repoId\":\"HDP-2.5\",\"repoName\":\"HDP\",\"defaultBaseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/updates/2.5.0.0\",\"latestBaseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.5.0.0-776\",\"baseSaved\":true},{\"baseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"osType\":\"redhat6\",\"repoId\":\"HDP-UTILS-1.1.0.21\",\"repoName\":\"HDP-UTILS\",\"defaultBaseUrl\":\"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"latestBaseUrl\":\"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"baseSaved\":true}]", 
        "package_list": "[{\"name\":\"ranger_${stack_version}-admin\",\"condition\":\"\",\"skipUpgrade\":false},{\"name\":\"ranger_${stack_version}-usersync\",\"condition\":\"\",\"skipUpgrade\":false},{\"name\":\"ranger_${stack_version}-tagsync\",\"condition\":\"should_install_ranger_tagsync\",\"skipUpgrade\":false},{\"name\":\"ambari-logsearch-solr-client\",\"condition\":\"should_install_logsearch_solr_client\",\"skipUpgrade\":false}]", 
        "db_name": "ambari", 
        "group_list": "[\"ranger\",\"hadoop\",\"users\"]", 
        "agentCacheDir": "/var/lib/ambari-agent/cache", 
        "ambari_db_rca_password": "mapred", 
        "repository_version_id": "1",
        "jdk_location": "http://c6401.ambari.apache.org:8080/resources/",
        "not_managed_hdfs_path_list": "[\"/tmp\"]",
        "ambari_db_rca_url": "jdbc:postgresql://c6401.ambari.apache.org/ambarirca",
        "java_version": "8",
        "repo_info": "[{\"baseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.5.0.0-777\",\"osType\":\"redhat6\",\"repoId\":\"HDP-2.5\",\"repoName\":\"HDP\",\"defaultBaseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/updates/2.5.0.0\",\"latestBaseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.5.0.0-776\",\"baseSaved\":true},{\"baseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"osType\":\"redhat6\",\"repoId\":\"HDP-UTILS-1.1.0.21\",\"repoName\":\"HDP-UTILS\",\"defaultBaseUrl\":\"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"latestBaseUrl\":\"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"baseSaved\":true}]",
        "package_list": "[{\"name\":\"ranger_${stack_version}-admin\",\"condition\":\"\",\"skipUpgrade\":false},{\"name\":\"ranger_${stack_version}-usersync\",\"condition\":\"\",\"skipUpgrade\":false},{\"name\":\"ranger_${stack_version}-tagsync\",\"condition\":\"should_install_ranger_tagsync\",\"skipUpgrade\":false},{\"name\":\"ambari-logsearch-solr-client\",\"condition\":\"should_install_logsearch_solr_client\",\"skipUpgrade\":false}]",
        "db_name": "ambari",
        "group_list": "[\"ranger\",\"hadoop\",\"users\"]",
        "agentCacheDir": "/var/lib/ambari-agent/cache",
        "ambari_db_rca_password": "mapred",
         "jce_name": "UnlimitedJCEPolicyJDK7.zip",
        "oracle_jdbc_url": "http://c6401.ambari.apache.org:8080/resources//ojdbc6.jar", 
        "db_driver_filename": "mysql-connector-java.jar", 
        "user_list": "[\"zookeeper\",\"ambari-qa\",\"ranger\",\"hdfs\"]", 
        "oracle_jdbc_url": "http://c6401.ambari.apache.org:8080/resources//ojdbc6.jar",
        "db_driver_filename": "mysql-connector-java.jar",
        "user_list": "[\"zookeeper\",\"ambari-qa\",\"ranger\",\"hdfs\"]",
         "clientsToUpdateConfigs": "[\"*\"]"
    }, 
    },
     "commandParams": {
        "service_package_folder": "common-services/RANGER/0.4.0/package", 
        "script": "scripts/ranger_usersync.py", 
        "hooks_folder": "HDP/2.0.6/hooks", 
        "version": "2.5.0.0-777", 
        "max_duration_for_retries": "0", 
        "command_retry_enabled": "false", 
        "command_timeout": "600", 
        "service_package_folder": "common-services/RANGER/0.4.0/package",
        "script": "scripts/ranger_usersync.py",
        "hooks_folder": "HDP/2.0.6/hooks",
        "version": "2.5.0.0-777",
        "max_duration_for_retries": "0",
        "command_retry_enabled": "false",
        "command_timeout": "600",
         "script_type": "PYTHON"
    }, 
    "forceRefreshConfigTags": [], 
    "stageId": 1, 
    },
    "forceRefreshConfigTags": [],
    "stageId": 1,
     "clusterHostInfo": {
         "snamenode_host": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "ambari_server_use_ssl": [
             "false"
        ], 
        ],
         "all_ping_ports": [
             "8670"
        ], 
        ],
         "ranger_tagsync_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "ranger_usersync_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "all_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "slave_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "namenode_host": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "ambari_server_port": [
             "8080"
        ], 
        ],
         "ranger_admin_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "all_racks": [
             "/default-rack"
        ], 
        ],
         "all_ipv4_ips": [
             "172.22.125.4"
        ], 
        ],
         "ambari_server_host": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "zookeeper_hosts": [
             "c6401.ambari.apache.org"
         ],
         "ranger_kms_server_hosts": [
             "c6401.ambari.apache.org"
         ]
    }, 
    },
     "configurations": {
         "ranger-hdfs-audit": {
             "xasecure.audit.destination.solr.zookeepers": "c6401.ambari.apache.org:2181/ranger_audits",
            "xasecure.audit.destination.solr.urls": "", 
            "xasecure.audit.destination.solr.batch.filespool.dir": "/var/log/hadoop/hdfs/audit/solr/spool", 
            "xasecure.audit.destination.hdfs.batch.filespool.dir": "/var/log/hadoop/hdfs/audit/hdfs/spool", 
            "xasecure.audit.destination.solr.urls": "",
            "xasecure.audit.destination.solr.batch.filespool.dir": "/var/log/hadoop/hdfs/audit/solr/spool",
            "xasecure.audit.destination.hdfs.batch.filespool.dir": "/var/log/hadoop/hdfs/audit/hdfs/spool",
             "xasecure.audit.destination.hdfs": "true",
            "xasecure.audit.destination.solr": "true", 
            "xasecure.audit.provider.summary.enabled": "false", 
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit", 
            "xasecure.audit.destination.solr": "true",
            "xasecure.audit.provider.summary.enabled": "false",
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit",
             "xasecure.audit.is.enabled": "true"
        }, 
        },
         "ssl-client": {
            "ssl.client.truststore.reload.interval": "10000", 
            "ssl.client.keystore.password": "bigdata", 
            "ssl.client.truststore.type": "jks", 
            "ssl.client.keystore.location": "/etc/security/clientKeys/keystore.jks", 
            "ssl.client.truststore.location": "/etc/security/clientKeys/all.jks", 
            "ssl.client.truststore.password": "bigdata", 
            "ssl.client.truststore.reload.interval": "10000",
            "ssl.client.keystore.password": "bigdata",
            "ssl.client.truststore.type": "jks",
            "ssl.client.keystore.location": "/etc/security/clientKeys/keystore.jks",
            "ssl.client.truststore.location": "/etc/security/clientKeys/all.jks",
            "ssl.client.truststore.password": "bigdata",
             "ssl.client.keystore.type": "jks"
        }, 
        },
         "ranger-admin-site": {
            "ranger.admin.kerberos.cookie.domain": "", 
            "ranger.kms.service.user.hdfs": "hdfs", 
            "ranger.spnego.kerberos.principal": "", 
            "ranger.ldap.ad.url": "{{ranger_ug_ldap_url}}", 
            "ranger.plugins.hive.serviceuser": "hive", 
            "ranger.lookup.kerberos.keytab": "", 
            "ranger.plugins.kms.serviceuser": "kms", 
            "ranger.service.https.attrib.ssl.enabled": "false", 
            "ranger.sso.browser.useragent": "Mozilla,chrome", 
            "ranger.jpa.jdbc.url": "jdbc:mysql://c6401.ambari.apache.org:3306/ranger01", 
            "ranger.plugins.hbase.serviceuser": "hbase", 
            "ranger.plugins.hdfs.serviceuser": "hdfs", 
            "ranger.ldap.group.searchbase": "{{ranger_ug_ldap_group_searchbase}}", 
            "ranger.ldap.user.dnpattern": "uid={0},ou=users,dc=xasecure,dc=net", 
            "ranger.plugins.knox.serviceuser": "knox", 
            "ranger.ldap.base.dn": "dc=example,dc=com", 
            "ranger.sso.publicKey": "", 
            "ranger.admin.kerberos.cookie.path": "/", 
            "ranger.service.https.attrib.clientAuth": "want", 
            "ranger.jpa.jdbc.user": "{{ranger_db_user}}", 
            "ranger.ldap.ad.user.searchfilter": "(sAMAccountName={0})", 
            "ranger.ldap.group.roleattribute": "cn", 
            "ranger.plugins.kafka.serviceuser": "kafka", 
            "ranger.admin.kerberos.principal": "", 
            "ranger.ldap.ad.bind.dn": "{{ranger_ug_ldap_bind_dn}}", 
            "ranger.admin.kerberos.cookie.domain": "",
            "ranger.kms.service.user.hdfs": "hdfs",
            "ranger.spnego.kerberos.principal": "",
            "ranger.ldap.ad.url": "{{ranger_ug_ldap_url}}",
            "ranger.plugins.hive.serviceuser": "hive",
            "ranger.lookup.kerberos.keytab": "",
            "ranger.plugins.kms.serviceuser": "kms",
            "ranger.service.https.attrib.ssl.enabled": "false",
            "ranger.sso.browser.useragent": "Mozilla,chrome",
            "ranger.jpa.jdbc.url": "jdbc:mysql://c6401.ambari.apache.org:3306/ranger01",
            "ranger.plugins.hbase.serviceuser": "hbase",
            "ranger.plugins.hdfs.serviceuser": "hdfs",
            "ranger.ldap.group.searchbase": "{{ranger_ug_ldap_group_searchbase}}",
            "ranger.ldap.user.dnpattern": "uid={0},ou=users,dc=xasecure,dc=net",
            "ranger.plugins.knox.serviceuser": "knox",
            "ranger.ldap.base.dn": "dc=example,dc=com",
            "ranger.sso.publicKey": "",
            "ranger.admin.kerberos.cookie.path": "/",
            "ranger.service.https.attrib.clientAuth": "want",
            "ranger.jpa.jdbc.user": "{{ranger_db_user}}",
            "ranger.ldap.ad.user.searchfilter": "(sAMAccountName={0})",
            "ranger.ldap.group.roleattribute": "cn",
            "ranger.plugins.kafka.serviceuser": "kafka",
            "ranger.admin.kerberos.principal": "",
            "ranger.ldap.ad.bind.dn": "{{ranger_ug_ldap_bind_dn}}",
             "ranger.credential.provider.path": "/etc/ranger/admin/rangeradmin.jceks",
            "ranger.ldap.referral": "ignore", 
            "ranger.service.http.port": "6080", 
            "ranger.ldap.user.searchfilter": "(uid={0})", 
            "ranger.plugins.atlas.serviceuser": "atlas", 
            "ranger.truststore.password": "changeit", 
            "ranger.ldap.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}", 
            "ranger.audit.solr.password": "NONE", 
            "ranger.audit.solr.zookeepers": "c6401.ambari.apache.org:2181/ranger_audits", 
            "ranger.lookup.kerberos.principal": "", 
            "ranger.service.https.port": "6182", 
            "ranger.plugins.storm.serviceuser": "storm", 
            "ranger.externalurl": "{{ranger_external_url}}", 
            "ranger.truststore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks", 
            "ranger.kms.service.user.hive": "", 
            "ranger.https.attrib.keystore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks", 
            "ranger.jpa.jdbc.dialect": "{{jdbc_dialect}}", 
            "ranger.service.host": "{{ranger_host}}", 
            "ranger.service.https.attrib.keystore.keyalias": "rangeradmin", 
            "ranger.service.https.attrib.keystore.pass": "xasecure", 
            "ranger.unixauth.remote.login.enabled": "true", 
            "ranger.jpa.jdbc.credential.alias": "rangeradmin", 
            "ranger.ldap.ad.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}", 
            "ranger.audit.solr.username": "ranger_solr", 
            "ranger.sso.enabled": "false", 
            "ranger.audit.solr.urls": "", 
            "ranger.ldap.ad.domain": "", 
            "ranger.plugins.yarn.serviceuser": "yarn", 
            "ranger.audit.source.type": "solr", 
            "ranger.ldap.bind.dn": "{{ranger_ug_ldap_bind_dn}}", 
            "ranger.ldap.url": "{{ranger_ug_ldap_url}}", 
            "ranger.authentication.method": "UNIX", 
            "ranger.service.http.enabled": "true", 
            "ranger.ldap.group.searchfilter": "{{ranger_ug_ldap_group_searchfilter}}", 
            "ranger.ldap.ad.referral": "ignore", 
            "ranger.ldap.ad.base.dn": "dc=example,dc=com", 
            "ranger.jpa.jdbc.password": "_", 
            "ranger.spnego.kerberos.keytab": "", 
            "ranger.sso.providerurl": "", 
            "ranger.unixauth.service.hostname": "{{ugsync_host}}", 
            "ranger.admin.kerberos.keytab": "", 
            "ranger.admin.kerberos.token.valid.seconds": "30", 
            "ranger.jpa.jdbc.driver": "com.mysql.jdbc.Driver", 
            "ranger.ldap.referral": "ignore",
            "ranger.service.http.port": "6080",
            "ranger.ldap.user.searchfilter": "(uid={0})",
            "ranger.plugins.atlas.serviceuser": "atlas",
            "ranger.truststore.password": "changeit",
            "ranger.ldap.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}",
            "ranger.audit.solr.password": "NONE",
            "ranger.audit.solr.zookeepers": "c6401.ambari.apache.org:2181/ranger_audits",
            "ranger.lookup.kerberos.principal": "",
            "ranger.service.https.port": "6182",
            "ranger.plugins.storm.serviceuser": "storm",
            "ranger.externalurl": "{{ranger_external_url}}",
            "ranger.truststore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks",
            "ranger.kms.service.user.hive": "",
            "ranger.https.attrib.keystore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks",
            "ranger.jpa.jdbc.dialect": "{{jdbc_dialect}}",
            "ranger.service.host": "{{ranger_host}}",
            "ranger.service.https.attrib.keystore.keyalias": "rangeradmin",
            "ranger.service.https.attrib.keystore.pass": "xasecure",
            "ranger.unixauth.remote.login.enabled": "true",
            "ranger.jpa.jdbc.credential.alias": "rangeradmin",
            "ranger.ldap.ad.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}",
            "ranger.audit.solr.username": "ranger_solr",
            "ranger.sso.enabled": "false",
            "ranger.audit.solr.urls": "",
            "ranger.ldap.ad.domain": "",
            "ranger.plugins.yarn.serviceuser": "yarn",
            "ranger.audit.source.type": "solr",
            "ranger.ldap.bind.dn": "{{ranger_ug_ldap_bind_dn}}",
            "ranger.ldap.url": "{{ranger_ug_ldap_url}}",
            "ranger.authentication.method": "UNIX",
            "ranger.service.http.enabled": "true",
            "ranger.ldap.group.searchfilter": "{{ranger_ug_ldap_group_searchfilter}}",
            "ranger.ldap.ad.referral": "ignore",
            "ranger.ldap.ad.base.dn": "dc=example,dc=com",
            "ranger.jpa.jdbc.password": "_",
            "ranger.spnego.kerberos.keytab": "",
            "ranger.sso.providerurl": "",
            "ranger.unixauth.service.hostname": "{{ugsync_host}}",
            "ranger.admin.kerberos.keytab": "",
            "ranger.admin.kerberos.token.valid.seconds": "30",
            "ranger.jpa.jdbc.driver": "com.mysql.jdbc.Driver",
             "ranger.unixauth.service.port": "5151"
        }, 
        },
         "ranger-hdfs-policymgr-ssl": {
            "xasecure.policymgr.clientssl.keystore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-keystore.jks", 
            "xasecure.policymgr.clientssl.truststore.password": "changeit", 
            "xasecure.policymgr.clientssl.keystore.credential.file": "jceks://file{{credential_file}}", 
            "xasecure.policymgr.clientssl.truststore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-truststore.jks", 
            "xasecure.policymgr.clientssl.truststore.credential.file": "jceks://file{{credential_file}}", 
            "xasecure.policymgr.clientssl.keystore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-keystore.jks",
            "xasecure.policymgr.clientssl.truststore.password": "changeit",
            "xasecure.policymgr.clientssl.keystore.credential.file": "jceks://file{{credential_file}}",
            "xasecure.policymgr.clientssl.truststore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-truststore.jks",
            "xasecure.policymgr.clientssl.truststore.credential.file": "jceks://file{{credential_file}}",
             "xasecure.policymgr.clientssl.keystore.password": "myKeyFilePassword"
        }, 
        },
         "tagsync-application-properties": {
            "atlas.kafka.entities.group.id": "ranger_entities_consumer", 
            "atlas.kafka.zookeeper.connect": "c6401.ambari.apache.org:2181", 
            "atlas.kafka.entities.group.id": "ranger_entities_consumer",
            "atlas.kafka.zookeeper.connect": "c6401.ambari.apache.org:2181",
             "atlas.kafka.bootstrap.servers": "localhost:6667"
        }, 
        },
         "ranger-env": {
            "ranger_solr_shards": "1", 
            "ranger_solr_config_set": "ranger_audits", 
            "ranger_user": "ranger", 
            "xml_configurations_supported": "true", 
            "ranger-atlas-plugin-enabled": "No", 
            "ranger-hbase-plugin-enabled": "No", 
            "ranger-yarn-plugin-enabled": "No", 
            "bind_anonymous": "false", 
            "ranger_admin_username": "amb_ranger_admin", 
            "admin_password": "admin", 
            "is_solrCloud_enabled": "true", 
            "ranger-storm-plugin-enabled": "No", 
            "ranger-hdfs-plugin-enabled": "No", 
            "ranger_group": "ranger", 
            "ranger-knox-plugin-enabled": "No", 
            "ranger_admin_log_dir": "/var/log/ranger/admin", 
            "ranger-kafka-plugin-enabled": "No", 
            "ranger_privelege_user_jdbc_url": "jdbc:mysql://c6401.ambari.apache.org:3306", 
            "ranger-hive-plugin-enabled": "No", 
            "xasecure.audit.destination.solr": "true", 
            "ranger_pid_dir": "/var/run/ranger", 
            "xasecure.audit.destination.hdfs": "true", 
            "admin_username": "admin", 
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit", 
            "create_db_dbuser": "true", 
            "ranger_solr_collection_name": "ranger_audits", 
            "ranger_admin_password": "P1!qLEQwP24KVlWY", 
            "ranger_solr_shards": "1",
            "ranger_solr_config_set": "ranger_audits",
            "ranger_user": "ranger",
            "xml_configurations_supported": "true",
            "ranger-atlas-plugin-enabled": "No",
            "ranger-hbase-plugin-enabled": "No",
            "ranger-yarn-plugin-enabled": "No",
            "bind_anonymous": "false",
            "ranger_admin_username": "amb_ranger_admin",
            "admin_password": "admin",
            "is_solrCloud_enabled": "true",
            "ranger-storm-plugin-enabled": "No",
            "ranger-hdfs-plugin-enabled": "No",
            "ranger_group": "ranger",
            "ranger-knox-plugin-enabled": "No",
            "ranger_admin_log_dir": "/var/log/ranger/admin",
            "ranger-kafka-plugin-enabled": "No",
            "ranger_privelege_user_jdbc_url": "jdbc:mysql://c6401.ambari.apache.org:3306",
            "ranger-hive-plugin-enabled": "No",
            "xasecure.audit.destination.solr": "true",
            "ranger_pid_dir": "/var/run/ranger",
            "xasecure.audit.destination.hdfs": "true",
            "admin_username": "admin",
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit",
            "create_db_dbuser": "true",
            "ranger_solr_collection_name": "ranger_audits",
            "ranger_admin_password": "P1!qLEQwP24KVlWY",
             "ranger_usersync_log_dir": "/var/log/ranger/usersync"
        }, 
        },
         "usersync-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#      http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n#\n\nlog4j.rootLogger = info,logFile\n\n# logFile\nlog4j.appender.logFile=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.logFile.file=${logdir}/usersync.log\nlog4j.appender.logFile.datePattern='.'yyyy-MM-dd\nlog4j.appender.logFile.layout=org.apache.log4j.PatternLayout\nlog4j.appender.logFile.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %m%n\n\n# console\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.Target=System.out\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %m%n"
        }, 
        },
         "admin-properties": {
            "db_user": "rangeradmin01", 
            "DB_FLAVOR": "MYSQL", 
            "db_password": "rangeradmin01", 
            "db_root_user": "root", 
            "policymgr_external_url": "http://c6401.ambari.apache.org:6080", 
            "db_name": "ranger01", 
            "db_host": "c6401.ambari.apache.org", 
            "db_root_password": "vagrant", 
            "db_user": "rangeradmin01",
            "DB_FLAVOR": "MYSQL",
            "db_password": "rangeradmin01",
            "db_root_user": "root",
            "policymgr_external_url": "http://c6401.ambari.apache.org:6080",
            "db_name": "ranger01",
            "db_host": "c6401.ambari.apache.org",
            "db_root_password": "vagrant",
             "SQL_CONNECTOR_JAR": "{{driver_curl_target}}"
        }, 
        },
         "ranger-ugsync-site": {
            "ranger.usersync.ldap.binddn": "", 
            "ranger.usersync.policymgr.username": "rangerusersync", 
            "ranger.usersync.policymanager.mockrun": "false", 
            "ranger.usersync.group.searchbase": "", 
            "ranger.usersync.ldap.bindalias": "testldapalias", 
            "ranger.usersync.truststore.file": "/usr/hdp/current/ranger-usersync/conf/mytruststore.jks", 
            "ranger.usersync.port": "5151", 
            "ranger.usersync.pagedresultssize": "500", 
            "ranger.usersync.group.memberattributename": "", 
            "ranger.usersync.kerberos.principal": "", 
            "ranger.usersync.source.impl.class": "org.apache.ranger.unixusersync.process.UnixUserGroupBuilder", 
            "ranger.usersync.ldap.referral": "ignore", 
            "ranger.usersync.group.searchfilter": "", 
            "ranger.usersync.ldap.user.objectclass": "person", 
            "ranger.usersync.logdir": "{{usersync_log_dir}}", 
            "ranger.usersync.ldap.user.searchfilter": "", 
            "ranger.usersync.ldap.groupname.caseconversion": "none", 
            "ranger.usersync.ldap.ldapbindpassword": "", 
            "ranger.usersync.unix.minUserId": "500", 
            "ranger.usersync.policymanager.maxrecordsperapicall": "1000", 
            "ranger.usersync.group.nameattribute": "", 
            "ranger.usersync.policymgr.alias": "ranger.usersync.policymgr.password", 
            "ranger.usersync.keystore.file": "/usr/hdp/current/ranger-usersync/conf/unixauthservice.jks", 
            "ranger.usersync.user.searchenabled": "false", 
            "ranger.usersync.group.usermapsyncenabled": "true", 
            "ranger.usersync.ldap.bindkeystore": "", 
            "ranger.usersync.ldap.user.groupnameattribute": "memberof, ismemberof", 
            "ranger.usersync.kerberos.keytab": "", 
            "ranger.usersync.passwordvalidator.path": "./native/credValidator.uexe", 
            "ranger.usersync.group.objectclass": "", 
            "ranger.usersync.ldap.user.searchscope": "sub", 
            "ranger.usersync.unix.password.file": "/etc/passwd", 
            "ranger.usersync.ldap.user.nameattribute": "", 
            "ranger.usersync.pagedresultsenabled": "true", 
            "ranger.usersync.policymanager.baseURL": "{{ranger_external_url}}", 
            "ranger.usersync.group.search.first.enabled": "false", 
            "ranger.usersync.group.searchenabled": "false", 
            "ranger.usersync.sink.impl.class": "org.apache.ranger.unixusersync.process.PolicyMgrUserGroupBuilder", 
            "ranger.usersync.ssl": "true", 
            "ranger.usersync.ldap.url": "", 
            "ranger.usersync.ldap.searchBase": "dc=hadoop,dc=apache,dc=org", 
            "ranger.usersync.policymgr.keystore": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks", 
            "ranger.usersync.ldap.user.searchbase": "", 
            "ranger.usersync.ldap.username.caseconversion": "none", 
            "ranger.usersync.credstore.filename": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks", 
            "ranger.usersync.keystore.password": "UnIx529p", 
            "ranger.usersync.unix.group.file": "/etc/group", 
            "ranger.usersync.filesource.file": "/tmp/usergroup.txt", 
            "ranger.usersync.group.searchscope": "", 
            "ranger.usersync.truststore.password": "changeit", 
            "ranger.usersync.enabled": "true", 
            "ranger.usersync.sleeptimeinmillisbetweensynccycle": "60000", 
            "ranger.usersync.ldap.binddn": "",
            "ranger.usersync.policymgr.username": "rangerusersync",
            "ranger.usersync.policymanager.mockrun": "false",
            "ranger.usersync.group.searchbase": "",
            "ranger.usersync.ldap.bindalias": "testldapalias",
            "ranger.usersync.truststore.file": "/usr/hdp/current/ranger-usersync/conf/mytruststore.jks",
            "ranger.usersync.port": "5151",
            "ranger.usersync.pagedresultssize": "500",
            "ranger.usersync.group.memberattributename": "",
            "ranger.usersync.kerberos.principal": "",
            "ranger.usersync.source.impl.class": "org.apache.ranger.unixusersync.process.UnixUserGroupBuilder",
            "ranger.usersync.ldap.referral": "ignore",
            "ranger.usersync.group.searchfilter": "",
            "ranger.usersync.ldap.user.objectclass": "person",
            "ranger.usersync.logdir": "{{usersync_log_dir}}",
            "ranger.usersync.ldap.user.searchfilter": "",
            "ranger.usersync.ldap.groupname.caseconversion": "none",
            "ranger.usersync.ldap.ldapbindpassword": "",
            "ranger.usersync.unix.minUserId": "500",
            "ranger.usersync.policymanager.maxrecordsperapicall": "1000",
            "ranger.usersync.group.nameattribute": "",
            "ranger.usersync.policymgr.alias": "ranger.usersync.policymgr.password",
            "ranger.usersync.keystore.file": "/usr/hdp/current/ranger-usersync/conf/unixauthservice.jks",
            "ranger.usersync.user.searchenabled": "false",
            "ranger.usersync.group.usermapsyncenabled": "true",
            "ranger.usersync.ldap.bindkeystore": "",
            "ranger.usersync.ldap.user.groupnameattribute": "memberof, ismemberof",
            "ranger.usersync.kerberos.keytab": "",
            "ranger.usersync.passwordvalidator.path": "./native/credValidator.uexe",
            "ranger.usersync.group.objectclass": "",
            "ranger.usersync.ldap.user.searchscope": "sub",
            "ranger.usersync.unix.password.file": "/etc/passwd",
            "ranger.usersync.ldap.user.nameattribute": "",
            "ranger.usersync.pagedresultsenabled": "true",
            "ranger.usersync.policymanager.baseURL": "{{ranger_external_url}}",
            "ranger.usersync.group.search.first.enabled": "false",
            "ranger.usersync.group.searchenabled": "false",
            "ranger.usersync.sink.impl.class": "org.apache.ranger.unixusersync.process.PolicyMgrUserGroupBuilder",
            "ranger.usersync.ssl": "true",
            "ranger.usersync.ldap.url": "",
            "ranger.usersync.ldap.searchBase": "dc=hadoop,dc=apache,dc=org",
            "ranger.usersync.policymgr.keystore": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks",
            "ranger.usersync.ldap.user.searchbase": "",
            "ranger.usersync.ldap.username.caseconversion": "none",
            "ranger.usersync.credstore.filename": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks",
            "ranger.usersync.keystore.password": "UnIx529p",
            "ranger.usersync.unix.group.file": "/etc/group",
            "ranger.usersync.filesource.file": "/tmp/usergroup.txt",
            "ranger.usersync.group.searchscope": "",
            "ranger.usersync.truststore.password": "changeit",
            "ranger.usersync.enabled": "true",
            "ranger.usersync.sleeptimeinmillisbetweensynccycle": "60000",
             "ranger.usersync.filesource.text.delimiter": ","
        }, 
        },
         "hdfs-site": {
            "dfs.namenode.checkpoint.period": "21600", 
            "dfs.namenode.avoid.write.stale.datanode": "true", 
            "dfs.namenode.startup.delay.block.deletion.sec": "3600", 
            "dfs.namenode.checkpoint.txns": "1000000", 
            "dfs.content-summary.limit": "5000", 
            "dfs.support.append": "true", 
            "dfs.datanode.address": "0.0.0.0:50010", 
            "dfs.cluster.administrators": " hdfs", 
            "dfs.namenode.audit.log.async": "true", 
            "dfs.datanode.balance.bandwidthPerSec": "6250000", 
            "dfs.namenode.safemode.threshold-pct": "1", 
            "dfs.namenode.checkpoint.edits.dir": "${dfs.namenode.checkpoint.dir}", 
            "dfs.namenode.rpc-address": "c6401.ambari.apache.org:8020", 
            "dfs.permissions.enabled": "true", 
            "dfs.client.read.shortcircuit": "true", 
            "dfs.https.port": "50470", 
            "dfs.namenode.https-address": "c6401.ambari.apache.org:50470", 
            "nfs.file.dump.dir": "/tmp/.hdfs-nfs", 
            "dfs.blocksize": "134217728", 
            "dfs.blockreport.initialDelay": "120", 
            "dfs.journalnode.edits.dir": "/hadoop/hdfs/journalnode", 
            "dfs.namenode.fslock.fair": "false", 
            "dfs.datanode.max.transfer.threads": "4096", 
            "dfs.heartbeat.interval": "3", 
            "dfs.replication": "3", 
            "dfs.namenode.handler.count": "50", 
            "dfs.namenode.checkpoint.dir": "/grid/0/hadoop/hdfs/namesecondary", 
            "fs.permissions.umask-mode": "022", 
            "dfs.namenode.stale.datanode.interval": "30000", 
            "dfs.datanode.ipc.address": "0.0.0.0:8010", 
            "dfs.datanode.failed.volumes.tolerated": "0", 
            "dfs.datanode.data.dir": "/grid/0/hadoop/hdfs/data", 
            "dfs.namenode.http-address": "c6401.ambari.apache.org:50070", 
            "dfs.webhdfs.enabled": "true", 
            "dfs.encrypt.data.transfer.cipher.suites": "AES/CTR/NoPadding", 
            "dfs.namenode.accesstime.precision": "0", 
            "dfs.datanode.https.address": "0.0.0.0:50475", 
            "dfs.namenode.write.stale.datanode.ratio": "1.0f", 
            "dfs.namenode.secondary.http-address": "c6401.ambari.apache.org:50090", 
            "nfs.exports.allowed.hosts": "* rw", 
            "dfs.datanode.http.address": "0.0.0.0:50075", 
            "dfs.datanode.du.reserved": "33011188224", 
            "dfs.client.read.shortcircuit.streams.cache.size": "4096", 
            "dfs.http.policy": "HTTP_ONLY", 
            "dfs.block.access.token.enable": "true", 
            "dfs.client.retry.policy.enabled": "false", 
            "dfs.namenode.name.dir.restore": "true", 
            "dfs.permissions.superusergroup": "hdfs", 
            "dfs.journalnode.https-address": "0.0.0.0:8481", 
            "dfs.journalnode.http-address": "0.0.0.0:8480", 
            "dfs.domain.socket.path": "/var/lib/hadoop-hdfs/dn_socket", 
            "dfs.namenode.avoid.read.stale.datanode": "true", 
            "dfs.hosts.exclude": "/etc/hadoop/conf/dfs.exclude", 
            "dfs.datanode.data.dir.perm": "750", 
            "dfs.encryption.key.provider.uri": "", 
            "dfs.replication.max": "50", 
            "dfs.namenode.checkpoint.period": "21600",
            "dfs.namenode.avoid.write.stale.datanode": "true",
            "dfs.namenode.startup.delay.block.deletion.sec": "3600",
            "dfs.namenode.checkpoint.txns": "1000000",
            "dfs.content-summary.limit": "5000",
            "dfs.support.append": "true",
            "dfs.datanode.address": "0.0.0.0:50010",
            "dfs.cluster.administrators": " hdfs",
            "dfs.namenode.audit.log.async": "true",
            "dfs.datanode.balance.bandwidthPerSec": "6250000",
            "dfs.namenode.safemode.threshold-pct": "1",
            "dfs.namenode.checkpoint.edits.dir": "${dfs.namenode.checkpoint.dir}",
            "dfs.namenode.rpc-address": "c6401.ambari.apache.org:8020",
            "dfs.permissions.enabled": "true",
            "dfs.client.read.shortcircuit": "true",
            "dfs.https.port": "50470",
            "dfs.namenode.https-address": "c6401.ambari.apache.org:50470",
            "nfs.file.dump.dir": "/tmp/.hdfs-nfs",
            "dfs.blocksize": "134217728",
            "dfs.blockreport.initialDelay": "120",
            "dfs.journalnode.edits.dir": "/hadoop/hdfs/journalnode",
            "dfs.namenode.fslock.fair": "false",
            "dfs.datanode.max.transfer.threads": "4096",
            "dfs.heartbeat.interval": "3",
            "dfs.replication": "3",
            "dfs.namenode.handler.count": "50",
            "dfs.namenode.checkpoint.dir": "/grid/0/hadoop/hdfs/namesecondary",
            "fs.permissions.umask-mode": "022",
            "dfs.namenode.stale.datanode.interval": "30000",
            "dfs.datanode.ipc.address": "0.0.0.0:8010",
            "dfs.datanode.failed.volumes.tolerated": "0",
            "dfs.datanode.data.dir": "/grid/0/hadoop/hdfs/data",
            "dfs.namenode.http-address": "c6401.ambari.apache.org:50070",
            "dfs.webhdfs.enabled": "true",
            "dfs.encrypt.data.transfer.cipher.suites": "AES/CTR/NoPadding",
            "dfs.namenode.accesstime.precision": "0",
            "dfs.datanode.https.address": "0.0.0.0:50475",
            "dfs.namenode.write.stale.datanode.ratio": "1.0f",
            "dfs.namenode.secondary.http-address": "c6401.ambari.apache.org:50090",
            "nfs.exports.allowed.hosts": "* rw",
            "dfs.datanode.http.address": "0.0.0.0:50075",
            "dfs.datanode.du.reserved": "33011188224",
            "dfs.client.read.shortcircuit.streams.cache.size": "4096",
            "dfs.http.policy": "HTTP_ONLY",
            "dfs.block.access.token.enable": "true",
            "dfs.client.retry.policy.enabled": "false",
            "dfs.namenode.name.dir.restore": "true",
            "dfs.permissions.superusergroup": "hdfs",
            "dfs.journalnode.https-address": "0.0.0.0:8481",
            "dfs.journalnode.http-address": "0.0.0.0:8480",
            "dfs.domain.socket.path": "/var/lib/hadoop-hdfs/dn_socket",
            "dfs.namenode.avoid.read.stale.datanode": "true",
            "dfs.hosts.exclude": "/etc/hadoop/conf/dfs.exclude",
            "dfs.datanode.data.dir.perm": "750",
            "dfs.encryption.key.provider.uri": "",
            "dfs.replication.max": "50",
             "dfs.namenode.name.dir": "/grid/0/hadoop/hdfs/namenode"
        }, 
        },
         "ranger-tagsync-site": {
            "ranger.tagsync.atlas.to.ranger.service.mapping": "", 
            "ranger.tagsync.dest.ranger.ssl.config.filename": "/usr/hdp/current/ranger-tagsync/conf/mytruststore.jks", 
            "ranger.tagsync.source.file.check.interval.millis": "", 
            "ranger.tagsync.logdir": "/var/log/ranger/tagsync", 
            "ranger.tagsync.source.atlasrest.download.interval.millis": "", 
            "ranger.tagsync.keystore.filename": "/usr/hdp/current/ranger-tagsync/conf/rangertagsync.jceks", 
            "ranger.tagsync.source.atlasrest.endpoint": "", 
            "ranger.tagsync.dest.ranger.username": "rangertagsync", 
            "ranger.tagsync.kerberos.principal": "", 
            "ranger.tagsync.dest.ranger.endpoint": "{{ranger_external_url}}", 
            "ranger.tagsync.atlas.custom.resource.mappers": "", 
            "ranger.tagsync.kerberos.keytab": "", 
            "ranger.tagsync.source.atlas": "false", 
            "ranger.tagsync.source.atlasrest": "false", 
            "ranger.tagsync.source.file": "false", 
            "ranger.tagsync.atlas.to.ranger.service.mapping": "",
            "ranger.tagsync.dest.ranger.ssl.config.filename": "/usr/hdp/current/ranger-tagsync/conf/mytruststore.jks",
            "ranger.tagsync.source.file.check.interval.millis": "",
            "ranger.tagsync.logdir": "/var/log/ranger/tagsync",
            "ranger.tagsync.source.atlasrest.download.interval.millis": "",
            "ranger.tagsync.keystore.filename": "/usr/hdp/current/ranger-tagsync/conf/rangertagsync.jceks",
            "ranger.tagsync.source.atlasrest.endpoint": "",
            "ranger.tagsync.dest.ranger.username": "rangertagsync",
            "ranger.tagsync.kerberos.principal": "",
            "ranger.tagsync.dest.ranger.endpoint": "{{ranger_external_url}}",
            "ranger.tagsync.atlas.custom.resource.mappers": "",
            "ranger.tagsync.kerberos.keytab": "",
            "ranger.tagsync.source.atlas": "false",
            "ranger.tagsync.source.atlasrest": "false",
            "ranger.tagsync.source.file": "false",
             "ranger.tagsync.source.file.filename": ""
        }, 
        },
         "zoo.cfg": {
            "clientPort": "2181", 
            "autopurge.purgeInterval": "24", 
            "syncLimit": "5", 
            "dataDir": "/grid/0/hadoop/zookeeper", 
            "initLimit": "10", 
            "tickTime": "2000", 
            "clientPort": "2181",
            "autopurge.purgeInterval": "24",
            "syncLimit": "5",
            "dataDir": "/grid/0/hadoop/zookeeper",
            "initLimit": "10",
            "tickTime": "2000",
             "autopurge.snapRetainCount": "30"
        }, 
        },
         "hadoop-policy": {
            "security.job.client.protocol.acl": "*", 
            "security.job.task.protocol.acl": "*", 
            "security.datanode.protocol.acl": "*", 
            "security.namenode.protocol.acl": "*", 
            "security.client.datanode.protocol.acl": "*", 
            "security.inter.tracker.protocol.acl": "*", 
            "security.refresh.usertogroups.mappings.protocol.acl": "hadoop", 
            "security.client.protocol.acl": "*", 
            "security.refresh.policy.protocol.acl": "hadoop", 
            "security.admin.operations.protocol.acl": "hadoop", 
            "security.job.client.protocol.acl": "*",
            "security.job.task.protocol.acl": "*",
            "security.datanode.protocol.acl": "*",
            "security.namenode.protocol.acl": "*",
            "security.client.datanode.protocol.acl": "*",
            "security.inter.tracker.protocol.acl": "*",
            "security.refresh.usertogroups.mappings.protocol.acl": "hadoop",
            "security.client.protocol.acl": "*",
            "security.refresh.policy.protocol.acl": "hadoop",
            "security.admin.operations.protocol.acl": "hadoop",
             "security.inter.datanode.protocol.acl": "*"
        }, 
        },
         "hdfs-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#  http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing,\n# software distributed under the License is distributed on an\n# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n# KIND, either express or implied.  See the License for the\n# specific language governing permissions and limitations\n# under the License.\n#\n\n\n# Define some default values that can be overridden by system properties\n# To change daemon root logger use hadoop_root_logger in hadoop-env\nhadoop.root.logger=INFO,console\nhadoop.log.dir=.\nhadoop.log.file=hadoop.log\n\n\n# Define the root logger to the system property \"hadoop.root.logger\".\nlog4j.rootLogger=${hadoop.root.logger}, EventCounter\n\n# Logging Threshold\nlog4j.threshhold=ALL\n\n#\n# Daily Rolling File Appender\n#\n\nlog4j.appender.DRFA=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.DRFA.File=${hadoop.log.dir}/${hadoop.log.file}\n\n# Rollver at midnight\nlog4j.appender.DRFA.DatePattern=.yyyy-MM-dd\n\n# 30-day backup\n#log4j.appender.DRFA.MaxBackupIndex=30\nlog4j.appender.DRFA.layout=org.apache.log4j.PatternLayout\n\n# Pattern format: Date LogLevel LoggerName LogMessage\nlog4j.appender.DRFA.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\n# Debugging Pattern format\n#log4j.appender.DRFA.layout.ConversionPattern=%d{ISO8601} %-5p %c{2} (%F:%M(%L)) - %m%n\n\n\n#\n# console\n# Add \"console\" to rootlogger above if you want to use this\n#\n\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.target=System.err\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{yy/MM/dd HH:mm:ss} %p %c{2}: %m%n\n\n#\n# TaskLog Appender\n#\n\n#Default values\nhadoop.tasklog.taskid=null\nhadoop.tasklog.iscleanup=false\nhadoop.tasklog.noKeepSplits=4\nhadoop.tasklog.totalLogFileSize=100\nhadoop.tasklog.purgeLogSplits=true\nhadoop.tasklog.logsRetainHours=12\n\nlog4j.appender.TLA=org.apache.hadoop.mapred.TaskLogAppender\nlog4j.appender.TLA.taskId=${hadoop.tasklog.taskid}\nlog4j.appender.TLA.isCleanup=${hadoop.tasklog.iscleanup}\nlog4j.appender.TLA.totalLogFileSize=${hadoop.tasklog.totalLogFileSize}\n\nlog4j.appender.TLA.layout=org.apache.log4j.PatternLayout\nlog4j.appender.TLA.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\n\n#\n#Security audit appender\n#\nhadoop.security.logger=INFO,console\nhadoop.security.log.maxfilesize=256MB\nhadoop.security.log.maxbackupindex=20\nlog4j.category.SecurityLogger=${hadoop.security.logger}\nhadoop.security.log.file=SecurityAuth.audit\nlog4j.appender.DRFAS=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.DRFAS.File=${hadoop.log.dir}/${hadoop.security.log.file}\nlog4j.appender.DRFAS.layout=org.apache.log4j.PatternLayout\nlog4j.appender.DRFAS.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\nlog4j.appender.DRFAS.DatePattern=.yyyy-MM-dd\n\nlog4j.appender.RFAS=org.apache.log4j.RollingFileAppender\nlog4j.appender.RFAS.File=${hadoop.log.dir}/${hadoop.security.log.file}\nlog4j.appender.RFAS.layout=org.apache.log4j.PatternLayout\nlog4j.appender.RFAS.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\nlog4j.appender.RFAS.MaxFileSize=${hadoop.security.log.maxfilesize}\nlog4j.appender.RFAS.MaxBackupIndex=${hadoop.security.log.maxbackupindex}\n\n#\n# hdfs audit logging\n#\nhdfs.audit.logger=INFO,console\nlog4j.logger.org.apache.hadoop.hdfs.server.namenode.FSNamesystem.audit=${hdfs.audit.logger}\nlog4j.additivity.org.apache.hadoop.hdfs.server.namenode.FSNamesystem.audit=false\nlog4j.appender.DRFAAUDIT=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.DRFAAUDIT.File=${hadoop.log.dir}/hdfs-audit.log\nlog4j.appender.DRFAAUDIT.layout=org.apache.log4j.PatternLayout\nlog4j.appender.DRFAAUDIT.layout.ConversionPattern=%d{ISO8601} %p %c{2}: %m%n\nlog4j.appender.DRFAAUDIT.DatePattern=.yyyy-MM-dd\n\n#\n# NameNode metrics logging.\n# The default is to retain two namenode-metrics.log files up to 64MB each.\n#\nnamenode.metrics.logger=INFO,NullAppender\nlog4j.logger.NameNodeMetricsLog=${namenode.metrics.logger}\nlog4j.additivity.NameNodeMetricsLog=false\nlog4j.appender.NNMETRICSRFA=org.apache.log4j.RollingFileAppender\nlog4j.appender.NNMETRICSRFA.File=${hadoop.log.dir}/namenode-metrics.log\nlog4j.appender.NNMETRICSRFA.layout=org.apache.log4j.PatternLayout\nlog4j.appender.NNMETRICSRFA.layout.ConversionPattern=%d{ISO8601} %m%n\nlog4j.appender.NNMETRICSRFA.MaxBackupIndex=1\nlog4j.appender.NNMETRICSRFA.MaxFileSize=64MB\n\n#\n# mapred audit logging\n#\nmapred.audit.logger=INFO,console\nlog4j.logger.org.apache.hadoop.mapred.AuditLogger=${mapred.audit.logger}\nlog4j.additivity.org.apache.hadoop.mapred.AuditLogger=false\nlog4j.appender.MRAUDIT=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.MRAUDIT.File=${hadoop.log.dir}/mapred-audit.log\nlog4j.appender.MRAUDIT.layout=org.apache.log4j.PatternLayout\nlog4j.appender.MRAUDIT.layout.ConversionPattern=%d{ISO8601} %p %c{2}: %m%n\nlog4j.appender.MRAUDIT.DatePattern=.yyyy-MM-dd\n\n#\n# Rolling File Appender\n#\n\nlog4j.appender.RFA=org.apache.log4j.RollingFileAppender\nlog4j.appender.RFA.File=${hadoop.log.dir}/${hadoop.log.file}\n\n# Logfile size and and 30-day backups\nlog4j.appender.RFA.MaxFileSize=256MB\nlog4j.appender.RFA.MaxBackupIndex=10\n\nlog4j.appender.RFA.layout=org.apache.log4j.PatternLayout\nlog4j.appender.RFA.layout.ConversionPattern=%d{ISO8601} %-5p %c{2} - %m%n\nlog4j.appender.RFA.layout.ConversionPattern=%d{ISO8601} %-5p %c{2} (%F:%M(%L)) - %m%n\n\n\n# Custom Logging levels\n\nhadoop.metrics.log.level=INFO\n#log4j.logger.org.apache.hadoop.mapred.JobTracker=DEBUG\n#log4j.logger.org.apache.hadoop.mapred.TaskTracker=DEBUG\n#log4j.logger.org.apache.hadoop.fs.FSNamesystem=DEBUG\nlog4j.logger.org.apache.hadoop.metrics2=${hadoop.metrics.log.level}\n\n# Jets3t library\nlog4j.logger.org.jets3t.service.impl.rest.httpclient.RestS3Service=ERROR\n\n#\n# Null Appender\n# Trap security logger on the hadoop client side\n#\nlog4j.appender.NullAppender=org.apache.log4j.varia.NullAppender\n\n#\n# Event Counter Appender\n# Sends counts of logging messages at different severity levels to Hadoop Metrics.\n#\nlog4j.appender.EventCounter=org.apache.hadoop.log.metrics.EventCounter\n\n# Removes \"deprecated\" messages\nlog4j.logger.org.apache.hadoop.conf.Configuration.deprecation=WARN\n\n#\n# HDFS block state change log from block manager\n#\n# Uncomment the following to suppress normal block state change\n# messages from BlockManager in NameNode.\n#log4j.logger.BlockStateChange=WARN"
        }, 
        },
         "ranger-hdfs-plugin-properties": {
            "hadoop.rpc.protection": "authentication", 
            "ranger-hdfs-plugin-enabled": "No", 
            "REPOSITORY_CONFIG_USERNAME": "hadoop", 
            "policy_user": "ambari-qa", 
            "common.name.for.certificate": "", 
            "hadoop.rpc.protection": "authentication",
            "ranger-hdfs-plugin-enabled": "No",
            "REPOSITORY_CONFIG_USERNAME": "hadoop",
            "policy_user": "ambari-qa",
            "common.name.for.certificate": "",
             "REPOSITORY_CONFIG_PASSWORD": "hadoop"
        }, 
        },
         "core-site": {
            "hadoop.proxyuser.root.hosts": "*", 
            "io.serializations": "org.apache.hadoop.io.serializer.WritableSerialization", 
            "fs.trash.interval": "360", 
            "ha.failover-controller.active-standby-elector.zk.op.retries": "120", 
            "hadoop.http.authentication.simple.anonymous.allowed": "true", 
            "hadoop.security.authentication": "simple", 
            "hadoop.proxyuser.root.groups": "*", 
            "ipc.client.connection.maxidletime": "30000", 
            "hadoop.security.key.provider.path": "", 
            "mapreduce.jobtracker.webinterface.trusted": "false", 
            "hadoop.security.authorization": "false", 
            "net.topology.script.file.name": "/etc/hadoop/conf/topology_script.py", 
            "ipc.server.tcpnodelay": "true", 
            "ipc.client.connect.max.retries": "50", 
            "hadoop.security.auth_to_local": "DEFAULT", 
            "io.file.buffer.size": "131072", 
            "hadoop.proxyuser.hdfs.hosts": "*", 
            "hadoop.proxyuser.hdfs.groups": "*", 
            "ipc.client.idlethreshold": "8000", 
            "fs.defaultFS": "hdfs://c6401.ambari.apache.org:8020", 
            "hadoop.proxyuser.root.hosts": "*",
            "io.serializations": "org.apache.hadoop.io.serializer.WritableSerialization",
            "fs.trash.interval": "360",
            "ha.failover-controller.active-standby-elector.zk.op.retries": "120",
            "hadoop.http.authentication.simple.anonymous.allowed": "true",
            "hadoop.security.authentication": "simple",
            "hadoop.proxyuser.root.groups": "*",
            "ipc.client.connection.maxidletime": "30000",
            "hadoop.security.key.provider.path": "",
            "mapreduce.jobtracker.webinterface.trusted": "false",
            "hadoop.security.authorization": "false",
            "net.topology.script.file.name": "/etc/hadoop/conf/topology_script.py",
            "ipc.server.tcpnodelay": "true",
            "ipc.client.connect.max.retries": "50",
            "hadoop.security.auth_to_local": "DEFAULT",
            "io.file.buffer.size": "131072",
            "hadoop.proxyuser.hdfs.hosts": "*",
            "hadoop.proxyuser.hdfs.groups": "*",
            "ipc.client.idlethreshold": "8000",
            "fs.defaultFS": "hdfs://c6401.ambari.apache.org:8020",
             "io.compression.codecs": "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec,org.apache.hadoop.io.compress.SnappyCodec"
        }, 
        },
         "hadoop-env": {
            "keyserver_port": "", 
            "proxyuser_group": "users", 
            "hdfs_user_nproc_limit": "65536", 
            "hdfs_log_dir_prefix": "/var/log/hadoop", 
            "hdfs_user_nofile_limit": "128000", 
            "hdfs_user": "hdfs", 
            "keyserver_host": " ", 
            "namenode_opt_maxnewsize": "128m", 
            "namenode_opt_maxpermsize": "256m", 
            "content": "\n# Set Hadoop-specific environment variables here.\n\n# The only required environment variable is JAVA_HOME.  All others are\n# optional.  When running a distributed configuration it is best to\n# set JAVA_HOME in this file, so that it is correctly defined on\n# remote nodes.\n\n# The java implementation to use.  Required.\nexport JAVA_HOME={{java_home}}\nexport HADOOP_HOME_WARN_SUPPRESS=1\n\n# Hadoop home directory\nexport HADOOP_HOME=${HADOOP_HOME:-{{hadoop_home}}}\n\n# Hadoop Configuration Directory\n\n{# this is different for HDP1 #}\n# Path to jsvc required by secure HDP 2.0 datanode\nexport JSVC_HOME={{jsvc_path}}\n\n\n# The maximum amount of heap to use, in MB. Default is 1000.\nexport HADOOP_HEAPSIZE=\"{{hadoop_heapsize}}\"\n\nexport HADOOP_NAMENODE_INIT_HEAPSIZE=\"-Xms{{namenode_heapsize}}\"\n\n# Extra Java runtime options.  Empty by default.\nexport HADOOP_OPTS=\"-Djava.net.preferIPv4Stack=true ${HADOOP_OPTS}\"\n\n# Command specific options appended to HADOOP_OPTS when specified\nHADOOP_JOBTRACKER_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{jtnode_opt_newsize}} -XX:MaxNewSize={{jtnode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xmx{{jtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dmapred.audit.logger=INFO,MRAUDIT -Dhadoop.mapreduce.jobsummary.logger=INFO,JSA ${HADOOP_JOBTRACKER_OPTS}\"\n\nHADOOP_TASKTRACKER_OPTS=\"-server -Xmx{{ttnode_heapsize}} -Dhadoop.security.logger=ERROR,console -Dmapred.audit.logger=ERROR,console ${HADOOP_TASKTRACKER_OPTS}\"\n\n{% if java_version < 8 %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -XX:PermSize={{namenode_opt_permsize}} -XX:MaxPermSize={{namenode_opt_maxpermsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -XX:PermSize=128m -XX:MaxPermSize=256m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m -XX:MaxPermSize=512m $HADOOP_CLIENT_OPTS\"\n\n{% else %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m $HADOOP_CLIENT_OPTS\"\n{% endif %}\n\nHADOOP_NFS3_OPTS=\"-Xmx{{nfsgateway_heapsize}}m -Dhadoop.security.logger=ERROR,DRFAS ${HADOOP_NFS3_OPTS}\"\nHADOOP_BALANCER_OPTS=\"-server -Xmx{{hadoop_heapsize}}m ${HADOOP_BALANCER_OPTS}\"\n\n\n# On secure datanodes, user to run the datanode as after dropping privileges\nexport HADOOP_SECURE_DN_USER=${HADOOP_SECURE_DN_USER:-{{hadoop_secure_dn_user}}}\n\n# Extra ssh options.  Empty by default.\nexport HADOOP_SSH_OPTS=\"-o ConnectTimeout=5 -o SendEnv=HADOOP_CONF_DIR\"\n\n# Where log files are stored.  $HADOOP_HOME/logs by default.\nexport HADOOP_LOG_DIR={{hdfs_log_dir_prefix}}/$USER\n\n# History server logs\nexport HADOOP_MAPRED_LOG_DIR={{mapred_log_dir_prefix}}/$USER\n\n# Where log files are stored in the secure data environment.\nexport HADOOP_SECURE_DN_LOG_DIR={{hdfs_log_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# File naming remote slave hosts.  $HADOOP_HOME/conf/slaves by default.\n# export HADOOP_SLAVES=${HADOOP_HOME}/conf/slaves\n\n# host:path where hadoop code should be rsync'd from.  Unset by default.\n# export HADOOP_MASTER=master:/home/$USER/src/hadoop\n\n# Seconds to sleep between slave commands.  Unset by default.  This\n# can be useful in large clusters, where, e.g., slave rsyncs can\n# otherwise arrive faster than the master can service them.\n# export HADOOP_SLAVE_SLEEP=0.1\n\n# The directory where pid files are stored. /tmp by default.\nexport HADOOP_PID_DIR={{hadoop_pid_dir_prefix}}/$USER\nexport HADOOP_SECURE_DN_PID_DIR={{hadoop_pid_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# History server pid\nexport HADOOP_MAPRED_PID_DIR={{mapred_pid_dir_prefix}}/$USER\n\nYARN_RESOURCEMANAGER_OPTS=\"-Dyarn.server.resourcemanager.appsummary.logger=INFO,RMSUMMARY\"\n\n# A string representing this instance of hadoop. $USER by default.\nexport HADOOP_IDENT_STRING=$USER\n\n# The scheduling priority for daemon processes.  See 'man nice'.\n\n# export HADOOP_NICENESS=10\n\n# Add database libraries\nJAVA_JDBC_LIBS=\"\"\nif [ -d \"/usr/share/java\" ]; then\n  for jarFile in `ls /usr/share/java | grep -E \"(mysql|ojdbc|postgresql|sqljdbc)\" 2>/dev/null`\n  do\n    JAVA_JDBC_LIBS=${JAVA_JDBC_LIBS}:$jarFile\n  done\nfi\n\n# Add libraries to the hadoop classpath - some may not need a colon as they already include it\nexport HADOOP_CLASSPATH=${HADOOP_CLASSPATH}${JAVA_JDBC_LIBS}\n\n# Setting path to hdfs command line\nexport HADOOP_LIBEXEC_DIR={{hadoop_libexec_dir}}\n\n# Mostly required for hadoop 2.0\nexport JAVA_LIBRARY_PATH=${JAVA_LIBRARY_PATH}\n\nexport HADOOP_OPTS=\"-Dhdp.version=$HDP_VERSION $HADOOP_OPTS\"\n\n{% if is_datanode_max_locked_memory_set %}\n# Fix temporary bug, when ulimit from conf files is not picked up, without full relogin. \n# Makes sense to fix only when runing DN as root \nif [ \"$command\" == \"datanode\" ] && [ \"$EUID\" -eq 0 ] && [ -n \"$HADOOP_SECURE_DN_USER\" ]; then\n  ulimit -l {{datanode_max_locked_memory}}\nfi\n{% endif %}", 
            "namenode_heapsize": "1024m", 
            "namenode_opt_newsize": "128m", 
            "nfsgateway_heapsize": "1024", 
            "dtnode_heapsize": "1024m", 
            "hadoop_root_logger": "INFO,RFA", 
            "hadoop_heapsize": "1024", 
            "hadoop_pid_dir_prefix": "/var/run/hadoop", 
            "namenode_opt_permsize": "128m", 
            "keyserver_port": "",
            "proxyuser_group": "users",
            "hdfs_user_nproc_limit": "65536",
            "hdfs_log_dir_prefix": "/var/log/hadoop",
            "hdfs_user_nofile_limit": "128000",
            "hdfs_user": "hdfs",
            "keyserver_host": " ",
            "namenode_opt_maxnewsize": "128m",
            "namenode_opt_maxpermsize": "256m",
            "content": "\n# Set Hadoop-specific environment variables here.\n\n# The only required environment variable is JAVA_HOME.  All others are\n# optional.  When running a distributed configuration it is best to\n# set JAVA_HOME in this file, so that it is correctly defined on\n# remote nodes.\n\n# The java implementation to use.  Required.\nexport JAVA_HOME={{java_home}}\nexport HADOOP_HOME_WARN_SUPPRESS=1\n\n# Hadoop home directory\nexport HADOOP_HOME=${HADOOP_HOME:-{{hadoop_home}}}\n\n# Hadoop Configuration Directory\n\n{# this is different for HDP1 #}\n# Path to jsvc required by secure HDP 2.0 datanode\nexport JSVC_HOME={{jsvc_path}}\n\n\n# The maximum amount of heap to use, in MB. Default is 1000.\nexport HADOOP_HEAPSIZE=\"{{hadoop_heapsize}}\"\n\nexport HADOOP_NAMENODE_INIT_HEAPSIZE=\"-Xms{{namenode_heapsize}}\"\n\n# Extra Java runtime options.  Empty by default.\nexport HADOOP_OPTS=\"-Djava.net.preferIPv4Stack=true ${HADOOP_OPTS}\"\n\n# Command specific options appended to HADOOP_OPTS when specified\nHADOOP_JOBTRACKER_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{jtnode_opt_newsize}} -XX:MaxNewSize={{jtnode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xmx{{jtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dmapred.audit.logger=INFO,MRAUDIT -Dhadoop.mapreduce.jobsummary.logger=INFO,JSA ${HADOOP_JOBTRACKER_OPTS}\"\n\nHADOOP_TASKTRACKER_OPTS=\"-server -Xmx{{ttnode_heapsize}} -Dhadoop.security.logger=ERROR,console -Dmapred.audit.logger=ERROR,console ${HADOOP_TASKTRACKER_OPTS}\"\n\n{% if java_version < 8 %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -XX:PermSize={{namenode_opt_permsize}} -XX:MaxPermSize={{namenode_opt_maxpermsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -XX:PermSize=128m -XX:MaxPermSize=256m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m -XX:MaxPermSize=512m $HADOOP_CLIENT_OPTS\"\n\n{% else %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m $HADOOP_CLIENT_OPTS\"\n{% endif %}\n\nHADOOP_NFS3_OPTS=\"-Xmx{{nfsgateway_heapsize}}m -Dhadoop.security.logger=ERROR,DRFAS ${HADOOP_NFS3_OPTS}\"\nHADOOP_BALANCER_OPTS=\"-server -Xmx{{hadoop_heapsize}}m ${HADOOP_BALANCER_OPTS}\"\n\n\n# On secure datanodes, user to run the datanode as after dropping privileges\nexport HADOOP_SECURE_DN_USER=${HADOOP_SECURE_DN_USER:-{{hadoop_secure_dn_user}}}\n\n# Extra ssh options.  Empty by default.\nexport HADOOP_SSH_OPTS=\"-o ConnectTimeout=5 -o SendEnv=HADOOP_CONF_DIR\"\n\n# Where log files are stored.  $HADOOP_HOME/logs by default.\nexport HADOOP_LOG_DIR={{hdfs_log_dir_prefix}}/$USER\n\n# History server logs\nexport HADOOP_MAPRED_LOG_DIR={{mapred_log_dir_prefix}}/$USER\n\n# Where log files are stored in the secure data environment.\nexport HADOOP_SECURE_DN_LOG_DIR={{hdfs_log_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# File naming remote slave hosts.  $HADOOP_HOME/conf/slaves by default.\n# export HADOOP_SLAVES=${HADOOP_HOME}/conf/slaves\n\n# host:path where hadoop code should be rsync'd from.  Unset by default.\n# export HADOOP_MASTER=master:/home/$USER/src/hadoop\n\n# Seconds to sleep between slave commands.  Unset by default.  This\n# can be useful in large clusters, where, e.g., slave rsyncs can\n# otherwise arrive faster than the master can service them.\n# export HADOOP_SLAVE_SLEEP=0.1\n\n# The directory where pid files are stored. /tmp by default.\nexport HADOOP_PID_DIR={{hadoop_pid_dir_prefix}}/$USER\nexport HADOOP_SECURE_DN_PID_DIR={{hadoop_pid_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# History server pid\nexport HADOOP_MAPRED_PID_DIR={{mapred_pid_dir_prefix}}/$USER\n\nYARN_RESOURCEMANAGER_OPTS=\"-Dyarn.server.resourcemanager.appsummary.logger=INFO,RMSUMMARY\"\n\n# A string representing this instance of hadoop. $USER by default.\nexport HADOOP_IDENT_STRING=$USER\n\n# The scheduling priority for daemon processes.  See 'man nice'.\n\n# export HADOOP_NICENESS=10\n\n# Add database libraries\nJAVA_JDBC_LIBS=\"\"\nif [ -d \"/usr/share/java\" ]; then\n  for jarFile in `ls /usr/share/java | grep -E \"(mysql|ojdbc|postgresql|sqljdbc)\" 2>/dev/null`\n  do\n    JAVA_JDBC_LIBS=${JAVA_JDBC_LIBS}:$jarFile\n  done\nfi\n\n# Add libraries to the hadoop classpath - some may not need a colon as they already include it\nexport HADOOP_CLASSPATH=${HADOOP_CLASSPATH}${JAVA_JDBC_LIBS}\n\n# Setting path to hdfs command line\nexport HADOOP_LIBEXEC_DIR={{hadoop_libexec_dir}}\n\n# Mostly required for hadoop 2.0\nexport JAVA_LIBRARY_PATH=${JAVA_LIBRARY_PATH}\n\nexport HADOOP_OPTS=\"-Dhdp.version=$HDP_VERSION $HADOOP_OPTS\"\n\n{% if is_datanode_max_locked_memory_set %}\n# Fix temporary bug, when ulimit from conf files is not picked up, without full relogin. \n# Makes sense to fix only when runing DN as root \nif [ \"$command\" == \"datanode\" ] && [ \"$EUID\" -eq 0 ] && [ -n \"$HADOOP_SECURE_DN_USER\" ]; then\n  ulimit -l {{datanode_max_locked_memory}}\nfi\n{% endif %}",
            "namenode_heapsize": "1024m",
            "namenode_opt_newsize": "128m",
            "nfsgateway_heapsize": "1024",
            "dtnode_heapsize": "1024m",
            "hadoop_root_logger": "INFO,RFA",
            "hadoop_heapsize": "1024",
            "hadoop_pid_dir_prefix": "/var/run/hadoop",
            "namenode_opt_permsize": "128m",
             "hdfs_tmp_dir": "/tmp"
        }, 
        },
         "zookeeper-log4j": {
             "content": "\n#\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#   http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing,\n# software distributed under the License is distributed on an\n# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n# KIND, either express or implied.  See the License for the\n# specific language governing permissions and limitations\n# under the License.\n#\n#\n#\n\n#\n# ZooKeeper Logging Configuration\n#\n\n# DEFAULT: console appender only\nlog4j.rootLogger=INFO, CONSOLE\n\n# Example with rolling log file\n#log4j.rootLogger=DEBUG, CONSOLE, ROLLINGFILE\n\n# Example with rolling log file and tracing\n#log4j.rootLogger=TRACE, CONSOLE, ROLLINGFILE, TRACEFILE\n\n#\n# Log INFO level and above messages to the console\n#\nlog4j.appender.CONSOLE=org.apache.log4j.ConsoleAppender\nlog4j.appender.CONSOLE.Threshold=INFO\nlog4j.appender.CONSOLE.layout=org.apache.log4j.PatternLayout\nlog4j.appender.CONSOLE.layout.ConversionPattern=%d{ISO8601} - %-5p [%t:%C{1}@%L] - %m%n\n\n#\n# Add ROLLINGFILE to rootLogger to get log file output\n#    Log DEBUG level and above messages to a log file\nlog4j.appender.ROLLINGFILE=org.apache.log4j.RollingFileAppender\nlog4j.appender.ROLLINGFILE.Threshold=DEBUG\nlog4j.appender.ROLLINGFILE.File=zookeeper.log\n\n# Max log file size of 10MB\nlog4j.appender.ROLLINGFILE.MaxFileSize=10MB\n# uncomment the next line to limit number of backup files\n#log4j.appender.ROLLINGFILE.MaxBackupIndex=10\n\nlog4j.appender.ROLLINGFILE.layout=org.apache.log4j.PatternLayout\nlog4j.appender.ROLLINGFILE.layout.ConversionPattern=%d{ISO8601} - %-5p [%t:%C{1}@%L] - %m%n\n\n\n#\n# Add TRACEFILE to rootLogger to get log file output\n#    Log DEBUG level and above messages to a log file\nlog4j.appender.TRACEFILE=org.apache.log4j.FileAppender\nlog4j.appender.TRACEFILE.Threshold=TRACE\nlog4j.appender.TRACEFILE.File=zookeeper_trace.log\n\nlog4j.appender.TRACEFILE.layout=org.apache.log4j.PatternLayout\n### Notice we are including log4j's NDC here (%x)\nlog4j.appender.TRACEFILE.layout.ConversionPattern=%d{ISO8601} - %-5p [%t:%C{1}@%L][%x] - %m%n"
        }, 
        },
         "ssl-server": {
            "ssl.server.keystore.location": "/etc/security/serverKeys/keystore.jks", 
            "ssl.server.keystore.keypassword": "bigdata", 
            "ssl.server.truststore.location": "/etc/security/serverKeys/all.jks", 
            "ssl.server.keystore.password": "bigdata", 
            "ssl.server.truststore.password": "bigdata", 
            "ssl.server.truststore.type": "jks", 
            "ssl.server.keystore.type": "jks", 
            "ssl.server.keystore.location": "/etc/security/serverKeys/keystore.jks",
            "ssl.server.keystore.keypassword": "bigdata",
            "ssl.server.truststore.location": "/etc/security/serverKeys/all.jks",
            "ssl.server.keystore.password": "bigdata",
            "ssl.server.truststore.password": "bigdata",
            "ssl.server.truststore.type": "jks",
            "ssl.server.keystore.type": "jks",
             "ssl.server.truststore.reload.interval": "10000"
        }, 
        "ranger-site": {}, 
        },
        "ranger-site": {},
         "admin-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#      http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n#\n\n\nlog4j.rootLogger = warn,xa_log_appender\n\n\n# xa_logger\nlog4j.appender.xa_log_appender=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.xa_log_appender.file=${logdir}/xa_portal.log\nlog4j.appender.xa_log_appender.datePattern='.'yyyy-MM-dd\nlog4j.appender.xa_log_appender.append=true\nlog4j.appender.xa_log_appender.layout=org.apache.log4j.PatternLayout\nlog4j.appender.xa_log_appender.layout.ConversionPattern=%d [%t] %-5p %C{6} (%F:%L) - %m%n\n# xa_log_appender : category and additivity\nlog4j.category.org.springframework=warn,xa_log_appender\nlog4j.additivity.org.springframework=false\n\nlog4j.category.org.apache.ranger=info,xa_log_appender\nlog4j.additivity.org.apache.ranger=false\n\nlog4j.category.xa=info,xa_log_appender\nlog4j.additivity.xa=false\n\n# perf_logger\nlog4j.appender.perf_appender=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.perf_appender.file=${logdir}/ranger_admin_perf.log\nlog4j.appender.perf_appender.datePattern='.'yyyy-MM-dd\nlog4j.appender.perf_appender.append=true\nlog4j.appender.perf_appender.layout=org.apache.log4j.PatternLayout\nlog4j.appender.perf_appender.layout.ConversionPattern=%d [%t] %m%n\n\n\n# sql_appender\nlog4j.appender.sql_appender=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.sql_appender.file=${logdir}/xa_portal_sql.log\nlog4j.appender.sql_appender.datePattern='.'yyyy-MM-dd\nlog4j.appender.sql_appender.append=true\nlog4j.appender.sql_appender.layout=org.apache.log4j.PatternLayout\nlog4j.appender.sql_appender.layout.ConversionPattern=%d [%t] %-5p %C{6} (%F:%L) - %m%n\n\n# sql_appender : category and additivity\nlog4j.category.org.hibernate.SQL=warn,sql_appender\nlog4j.additivity.org.hibernate.SQL=false\n\nlog4j.category.jdbc.sqlonly=fatal,sql_appender\nlog4j.additivity.jdbc.sqlonly=false\n\nlog4j.category.jdbc.sqltiming=warn,sql_appender\nlog4j.additivity.jdbc.sqltiming=false\n\nlog4j.category.jdbc.audit=fatal,sql_appender\nlog4j.additivity.jdbc.audit=false\n\nlog4j.category.jdbc.resultset=fatal,sql_appender\nlog4j.additivity.jdbc.resultset=false\n\nlog4j.category.jdbc.connection=fatal,sql_appender\nlog4j.additivity.jdbc.connection=false"
        }, 
        },
         "tagsync-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#      http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n#\n\n\nlog4j.rootLogger = info,logFile\n\n# logFile\nlog4j.appender.logFile=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.logFile.file=${logdir}/tagsync.log\nlog4j.appender.logFile.datePattern='.'yyyy-MM-dd\nlog4j.appender.logFile.layout=org.apache.log4j.PatternLayout\nlog4j.appender.logFile.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %L %m%n\n\n# console\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.Target=System.out\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %L %m%n"
        }, 
        },
         "ranger-hdfs-security": {
            "ranger.plugin.hdfs.policy.source.impl": "org.apache.ranger.admin.client.RangerAdminRESTClient", 
            "ranger.plugin.hdfs.service.name": "{{repo_name}}", 
            "ranger.plugin.hdfs.policy.cache.dir": "/etc/ranger/{{repo_name}}/policycache", 
            "ranger.plugin.hdfs.policy.pollIntervalMs": "30000", 
            "ranger.plugin.hdfs.policy.rest.url": "{{policymgr_mgr_url}}", 
            "ranger.plugin.hdfs.policy.rest.ssl.config.file": "/etc/hadoop/conf/ranger-policymgr-ssl.xml", 
            "ranger.plugin.hdfs.policy.source.impl": "org.apache.ranger.admin.client.RangerAdminRESTClient",
            "ranger.plugin.hdfs.service.name": "{{repo_name}}",
            "ranger.plugin.hdfs.policy.cache.dir": "/etc/ranger/{{repo_name}}/policycache",
            "ranger.plugin.hdfs.policy.pollIntervalMs": "30000",
            "ranger.plugin.hdfs.policy.rest.url": "{{policymgr_mgr_url}}",
            "ranger.plugin.hdfs.policy.rest.ssl.config.file": "/etc/hadoop/conf/ranger-policymgr-ssl.xml",
             "xasecure.add-hadoop-authorization": "true"
        }, 
        "usersync-properties": {}, 
        },
        "usersync-properties": {},
         "zookeeper-env": {
            "zk_log_dir": "/var/log/zookeeper", 
            "content": "\nexport JAVA_HOME={{java64_home}}\nexport ZOOKEEPER_HOME={{zk_home}}\nexport ZOO_LOG_DIR={{zk_log_dir}}\nexport ZOOPIDFILE={{zk_pid_file}}\nexport SERVER_JVMFLAGS={{zk_server_heapsize}}\nexport JAVA=$JAVA_HOME/bin/java\nexport CLASSPATH=$CLASSPATH:/usr/share/zookeeper/*\n\n{% if security_enabled %}\nexport SERVER_JVMFLAGS=\"$SERVER_JVMFLAGS -Djava.security.auth.login.config={{zk_server_jaas_file}}\"\nexport CLIENT_JVMFLAGS=\"$CLIENT_JVMFLAGS -Djava.security.auth.login.config={{zk_client_jaas_file}}\"\n{% endif %}", 
            "zk_server_heapsize": "1024m", 
            "zk_pid_dir": "/var/run/zookeeper", 
            "zk_log_dir": "/var/log/zookeeper",
            "content": "\nexport JAVA_HOME={{java64_home}}\nexport ZOOKEEPER_HOME={{zk_home}}\nexport ZOO_LOG_DIR={{zk_log_dir}}\nexport ZOOPIDFILE={{zk_pid_file}}\nexport SERVER_JVMFLAGS={{zk_server_heapsize}}\nexport JAVA=$JAVA_HOME/bin/java\nexport CLASSPATH=$CLASSPATH:/usr/share/zookeeper/*\n\n{% if security_enabled %}\nexport SERVER_JVMFLAGS=\"$SERVER_JVMFLAGS -Djava.security.auth.login.config={{zk_server_jaas_file}}\"\nexport CLIENT_JVMFLAGS=\"$CLIENT_JVMFLAGS -Djava.security.auth.login.config={{zk_client_jaas_file}}\"\n{% endif %}",
            "zk_server_heapsize": "1024m",
            "zk_pid_dir": "/var/run/zookeeper",
             "zk_user": "zookeeper"
        }, 
        },
         "cluster-env": {
            "security_enabled": "false", 
            "override_uid": "true", 
            "fetch_nonlocal_groups": "true", 
            "one_dir_per_partition": "true", 
            "repo_ubuntu_template": "{{package_type}} {{base_url}} {{components}}", 
            "ignore_groupsusers_create": "false", 
            "alerts_repeat_tolerance": "1", 
            "smokeuser_keytab": "/etc/security/keytabs/smokeuser.headless.keytab", 
            "kerberos_domain": "EXAMPLE.COM", 
            "security_enabled": "false",
            "override_uid": "true",
            "fetch_nonlocal_groups": "true",
            "one_dir_per_partition": "true",
            "repo_ubuntu_template": "{{package_type}} {{base_url}} {{components}}",
            "ignore_groupsusers_create": "false",
            "alerts_repeat_tolerance": "1",
            "smokeuser_keytab": "/etc/security/keytabs/smokeuser.headless.keytab",
            "kerberos_domain": "EXAMPLE.COM",
             "manage_dirs_on_root": "true",
            "recovery_lifetime_max_count": "1024", 
            "recovery_type": "AUTO_START", 
            "ignore_bad_mounts": "false", 
            "recovery_window_in_minutes": "60", 
            "user_group": "hadoop", 
            "stack_tools": "{\n  \"stack_selector\": [\"hdp-select\", \"/usr/bin/hdp-select\", \"hdp-select\"],\n  \"conf_selector\": [\"conf-select\", \"/usr/bin/conf-select\", \"conf-select\"]\n}", 
            "recovery_retry_interval": "5", 
            "stack_features": "{\n  \"stack_features\": [\n    {\n      \"name\": \"snappy\",\n      \"description\": \"Snappy compressor/decompressor support\",\n      \"min_version\": \"2.0.0.0\",\n      \"max_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"lzo\",\n      \"description\": \"LZO libraries support\",\n      \"min_version\": \"2.2.1.0\"\n    },\n    {\n      \"name\": \"express_upgrade\",\n      \"description\": \"Express upgrade support\",\n      \"min_version\": \"2.1.0.0\"\n    },\n    {\n      \"name\": \"rolling_upgrade\",\n      \"description\": \"Rolling upgrade support\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"config_versioning\",\n      \"description\": \"Configurable versions support\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"datanode_non_root\",\n      \"description\": \"DataNode running as non-root support (AMBARI-7615)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"remove_ranger_hdfs_plugin_env\",\n      \"description\": \"HDFS removes Ranger env files (AMBARI-14299)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"ranger\",\n      \"description\": \"Ranger Service support\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"ranger_tagsync_component\",\n      \"description\": \"Ranger Tagsync component support (AMBARI-14383)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"phoenix\",\n      \"description\": \"Phoenix Service support\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"nfs\",\n      \"description\": \"NFS support\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"tez_for_spark\",\n      \"description\": \"Tez dependency for Spark\",\n      \"min_version\": \"2.2.0.0\",\n      \"max_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"timeline_state_store\",\n      \"description\": \"Yarn application timeline-service supports state store property (AMBARI-11442)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"copy_tarball_to_hdfs\",\n      \"description\": \"Copy tarball to HDFS support (AMBARI-12113)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"spark_16plus\",\n      \"description\": \"Spark 1.6+\",\n      \"min_version\": \"2.4.0.0\"\n    },\n    {\n      \"name\": \"spark_thriftserver\",\n      \"description\": \"Spark Thrift Server\",\n      \"min_version\": \"2.3.2.0\"\n    },\n    {\n      \"name\": \"storm_kerberos\",\n      \"description\": \"Storm Kerberos support (AMBARI-7570)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"storm_ams\",\n      \"description\": \"Storm AMS integration (AMBARI-10710)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"create_kafka_broker_id\",\n      \"description\": \"Ambari should create Kafka Broker Id (AMBARI-12678)\",\n      \"min_version\": \"2.2.0.0\",\n      \"max_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"kafka_listeners\",\n      \"description\": \"Kafka listeners (AMBARI-10984)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"kafka_kerberos\",\n      \"description\": \"Kafka Kerberos support (AMBARI-10984)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"pig_on_tez\",\n      \"description\": \"Pig on Tez support (AMBARI-7863)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"ranger_usersync_non_root\",\n      \"description\": \"Ranger Usersync as non-root user (AMBARI-10416)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"ranger_audit_db_support\",\n      \"description\": \"Ranger Audit to DB support\",\n      \"min_version\": \"2.2.0.0\",\n      \"max_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"accumulo_kerberos_user_auth\",\n      \"description\": \"Accumulo Kerberos User Auth (AMBARI-10163)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"knox_versioned_data_dir\",\n      \"description\": \"Use versioned data dir for Knox (AMBARI-13164)\",\n      \"min_version\": \"2.3.2.0\"\n    },\n    {\n      \"name\": \"knox_sso_topology\",\n      \"description\": \"Knox SSO Topology support (AMBARI-13975)\",\n      \"min_version\": \"2.3.8.0\"\n    },\n    {\n      \"name\": \"atlas_rolling_upgrade\",\n      \"description\": \"Rolling upgrade support for Atlas\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"oozie_admin_user\",\n      \"description\": \"Oozie install user as an Oozie admin user (AMBARI-7976)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"oozie_create_hive_tez_configs\",\n      \"description\": \"Oozie create configs for Ambari Hive and Tez deployments (AMBARI-8074)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"oozie_setup_shared_lib\",\n      \"description\": \"Oozie setup tools used to shared Oozie lib to HDFS (AMBARI-7240)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"oozie_host_kerberos\",\n      \"description\": \"Oozie in secured clusters uses _HOST in Kerberos principal (AMBARI-9775)\",\n      \"min_version\": \"2.0.0.0\",\n      \"max_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"falcon_extensions\",\n      \"description\": \"Falcon Extension\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hive_metastore_upgrade_schema\",\n      \"description\": \"Hive metastore upgrade schema support (AMBARI-11176)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"hive_server_interactive\",\n      \"description\": \"Hive server interactive support (AMBARI-15573)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hive_webhcat_specific_configs\",\n      \"description\": \"Hive webhcat specific configurations support (AMBARI-12364)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"hive_purge_table\",\n      \"description\": \"Hive purge table support (AMBARI-12260)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"hive_server2_kerberized_env\",\n      \"description\": \"Hive server2 working on kerberized environment (AMBARI-13749)\",\n      \"min_version\": \"2.2.3.0\",\n      \"max_version\": \"2.2.5.0\"\n    },\n    {\n      \"name\": \"hive_env_heapsize\",\n      \"description\": \"Hive heapsize property defined in hive-env (AMBARI-12801)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"ranger_kms_hsm_support\",\n      \"description\": \"Ranger KMS HSM support (AMBARI-15752)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_log4j_support\",\n      \"description\": \"Ranger supporting log-4j properties (AMBARI-15681)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_kerberos_support\",\n      \"description\": \"Ranger Kerberos support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hive_metastore_site_support\",\n      \"description\": \"Hive Metastore site support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_usersync_password_jceks\",\n      \"description\": \"Saving Ranger Usersync credentials in jceks\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_install_infra_client\",\n      \"description\": \"LogSearch Service support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hbase_home_directory\",\n      \"description\": \"Hbase home directory in HDFS needed for HBASE backup\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"spark_livy\",\n      \"description\": \"Livy as slave component of spark\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"atlas_ranger_plugin_support\",\n      \"description\": \"Atlas Ranger plugin support\",\n      \"min_version\": \"2.5.0.0\"\n    }\n  ]\n}",
            "recovery_enabled": "true", 
            "recovery_max_count": "6", 
            "stack_root": "/usr/hdp", 
            "repo_suse_rhel_template": "[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0", 
            "managed_hdfs_resource_property_names": "", 
            "recovery_lifetime_max_count": "1024",
            "recovery_type": "AUTO_START",
            "ignore_bad_mounts": "false",
            "recovery_window_in_minutes": "60",
            "user_group": "hadoop",
            "stack_name": "HDP",
            "stack_root": "{\"HDP\": \"/usr/hdp\"}",
            "stack_tools": "{\n \"HDP\": { \"stack_selector\": [\"hdp-select\", \"/usr/bin/hdp-select\", \"hdp-select\"],\n  \"conf_selector\": [\"conf-select\", \"/usr/bin/conf-select\", \"conf-select\"]\n}\n}",
            "recovery_retry_interval": "5",
            "recovery_enabled": "true",
            "recovery_max_count": "6",
            "repo_suse_rhel_template": "[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0",
            "managed_hdfs_resource_property_names": "",
             "smokeuser": "ambari-qa"
         },
         "dbks-site": {
            "ranger.ks.jpa.jdbc.credential.provider.path": "/etc/ranger/kms/rangerkms.jceks", 
            "ranger.ks.kerberos.keytab": "/etc/security/keytabs/rangerkms.service.keytab", 
            "ranger.ks.hsm.partition.password": "_", 
            "ranger.ks.jpa.jdbc.driver": "com.mysql.jdbc.Driver", 
            "ranger.ks.jpa.jdbc.credential.alias": "ranger.ks.jdbc.password", 
            "ranger.ks.kerberos.principal": "rangerkms12/_HOST@EXAMPLE.COM", 
            "ranger.db.encrypt.key.password": "_", 
            "ranger.ks.hsm.enabled": "false", 
            "ranger.ks.jpa.jdbc.password": "_", 
            "ranger.ks.masterkey.credential.alias": "ranger.ks.masterkey.password", 
            "ranger.ks.jpa.jdbc.url": "jdbc:mysql://c6401.ambari.apache.org:3306/rangerkms01", 
            "hadoop.kms.blacklist.DECRYPT_EEK": "hdfs", 
            "ranger.ks.jdbc.sqlconnectorjar": "{{ews_lib_jar_path}}", 
            "ranger.ks.jpa.jdbc.user": "{{db_user}}", 
            "ranger.ks.hsm.partition.password.alias": "ranger.kms.hsm.partition.password", 
            "ranger.ks.hsm.type": "LunaProvider", 
            "ranger.ks.hsm.partition.name": "par19", 
            "ranger.ks.jpa.jdbc.credential.provider.path": "/etc/ranger/kms/rangerkms.jceks",
            "ranger.ks.kerberos.keytab": "/etc/security/keytabs/rangerkms.service.keytab",
            "ranger.ks.hsm.partition.password": "_",
            "ranger.ks.jpa.jdbc.driver": "com.mysql.jdbc.Driver",
            "ranger.ks.jpa.jdbc.credential.alias": "ranger.ks.jdbc.password",
            "ranger.ks.kerberos.principal": "rangerkms12/_HOST@EXAMPLE.COM",
            "ranger.db.encrypt.key.password": "_",
            "ranger.ks.hsm.enabled": "false",
            "ranger.ks.jpa.jdbc.password": "_",
            "ranger.ks.masterkey.credential.alias": "ranger.ks.masterkey.password",
            "ranger.ks.jpa.jdbc.url": "jdbc:mysql://c6401.ambari.apache.org:3306/rangerkms01",
            "hadoop.kms.blacklist.DECRYPT_EEK": "hdfs",
            "ranger.ks.jdbc.sqlconnectorjar": "{{ews_lib_jar_path}}",
            "ranger.ks.jpa.jdbc.user": "{{db_user}}",
            "ranger.ks.hsm.partition.password.alias": "ranger.kms.hsm.partition.password",
            "ranger.ks.hsm.type": "LunaProvider",
            "ranger.ks.hsm.partition.name": "par19",
             "ranger.ks.jpa.jdbc.dialect": "{{jdbc_dialect}}"
         },
         "kms-env": {
            "kms_group": "kms", 
            "kms_log_dir": "/var/log/ranger/kms", 
            "hsm_partition_password": "", 
            "kms_user": "kms", 
            "create_db_user": "true", 
            "kms_group": "kms",
            "kms_log_dir": "/var/log/ranger/kms",
            "hsm_partition_password": "",
            "kms_user": "kms",
            "create_db_user": "true",
             "kms_port": "9292"
         },
         "kms-log4j": {
             "content": "\n#\n# Licensed under the Apache License, Version 2.0 (the \"License\");\n# you may not use this file except in compliance with the License.\n# You may obtain a copy of the License at\n#\n#    http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License. See accompanying LICENSE file.\n#\n\n# If the Java System property 'kms.log.dir' is not defined at KMS start up time\n# Setup sets its value to '${kms.home}/logs'\n\nlog4j.appender.kms=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.kms.DatePattern='.'yyyy-MM-dd\nlog4j.appender.kms.File=${kms.log.dir}/kms.log\nlog4j.appender.kms.Append=true\nlog4j.appender.kms.layout=org.apache.log4j.PatternLayout\nlog4j.appender.kms.layout.ConversionPattern=%d{ISO8601} %-5p %c{1} - %m%n\n\nlog4j.appender.kms-audit=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.kms-audit.DatePattern='.'yyyy-MM-dd\nlog4j.appender.kms-audit.File=${kms.log.dir}/kms-audit.log\nlog4j.appender.kms-audit.Append=true\nlog4j.appender.kms-audit.layout=org.apache.log4j.PatternLayout\nlog4j.appender.kms-audit.layout.ConversionPattern=%d{ISO8601} %m%n\n\nlog4j.logger.kms-audit=INFO, kms-audit\nlog4j.additivity.kms-audit=false\n\nlog4j.rootLogger=ALL, kms\nlog4j.logger.org.apache.hadoop.conf=ERROR\nlog4j.logger.org.apache.hadoop=INFO\nlog4j.logger.com.sun.jersey.server.wadl.generators.WadlGeneratorJAXBGrammarGenerator=OFF"
         },
         "kms-properties": {
            "REPOSITORY_CONFIG_USERNAME": "keyadmin", 
            "db_user": "rangerkms01", 
            "DB_FLAVOR": "MYSQL", 
            "db_password": "rangerkms01", 
            "KMS_MASTER_KEY_PASSWD": "StrongPassword01", 
            "db_root_user": "root", 
            "db_name": "rangerkms01", 
            "db_host": "c6401.ambari.apache.org", 
            "db_root_password": "vagrant", 
            "SQL_CONNECTOR_JAR": "{{driver_curl_target}}", 
            "REPOSITORY_CONFIG_USERNAME": "keyadmin",
            "db_user": "rangerkms01",
            "DB_FLAVOR": "MYSQL",
            "db_password": "rangerkms01",
            "KMS_MASTER_KEY_PASSWD": "StrongPassword01",
            "db_root_user": "root",
            "db_name": "rangerkms01",
            "db_host": "c6401.ambari.apache.org",
            "db_root_password": "vagrant",
            "SQL_CONNECTOR_JAR": "{{driver_curl_target}}",
             "REPOSITORY_CONFIG_PASSWORD": "keyadmin"
         },
         "kms-site": {
            "hadoop.kms.proxyuser.ranger.hosts": "*", 
            "hadoop.kms.authentication.type": "simple", 
            "hadoop.kms.proxyuser.ranger.groups": "*", 
            "hadoop.kms.authentication.signer.secret.provider.zookeeper.path": "/hadoop-kms/hadoop-auth-signature-secret", 
            "hadoop.kms.security.authorization.manager": "org.apache.ranger.authorization.kms.authorizer.RangerKmsAuthorizer", 
            "hadoop.kms.authentication.kerberos.name.rules": "DEFAULT", 
            "hadoop.kms.authentication.signer.secret.provider.zookeeper.kerberos.keytab": "/etc/security/keytabs/spnego.service.keytab", 
            "hadoop.kms.current.key.cache.timeout.ms": "30000", 
            "hadoop.kms.authentication.kerberos.keytab": "/etc/security/keytabs/spnego.service.keytab", 
            "hadoop.kms.audit.aggregation.window.ms": "10000", 
            "hadoop.kms.proxyuser.ranger.users": "*", 
            "hadoop.kms.authentication.signer.secret.provider.zookeeper.auth.type": "kerberos", 
            "hadoop.kms.key.provider.uri": "dbks://http@localhost:9292/kms", 
            "hadoop.security.keystore.JavaKeyStoreProvider.password": "none", 
            "hadoop.kms.authentication.signer.secret.provider.zookeeper.kerberos.principal": "HTTP/_HOST@EXAMPLE.COM", 
            "hadoop.kms.authentication.signer.secret.provider": "random", 
            "hadoop.kms.authentication.signer.secret.provider.zookeeper.connection.string": "#HOSTNAME#:#PORT#,...", 
            "hadoop.kms.cache.enable": "true", 
            "hadoop.kms.cache.timeout.ms": "600000", 
            "hadoop.kms.proxyuser.ranger.hosts": "*",
            "hadoop.kms.authentication.type": "simple",
            "hadoop.kms.proxyuser.ranger.groups": "*",
            "hadoop.kms.authentication.signer.secret.provider.zookeeper.path": "/hadoop-kms/hadoop-auth-signature-secret",
            "hadoop.kms.security.authorization.manager": "org.apache.ranger.authorization.kms.authorizer.RangerKmsAuthorizer",
            "hadoop.kms.authentication.kerberos.name.rules": "DEFAULT",
            "hadoop.kms.authentication.signer.secret.provider.zookeeper.kerberos.keytab": "/etc/security/keytabs/spnego.service.keytab",
            "hadoop.kms.current.key.cache.timeout.ms": "30000",
            "hadoop.kms.authentication.kerberos.keytab": "/etc/security/keytabs/spnego.service.keytab",
            "hadoop.kms.audit.aggregation.window.ms": "10000",
            "hadoop.kms.proxyuser.ranger.users": "*",
            "hadoop.kms.authentication.signer.secret.provider.zookeeper.auth.type": "kerberos",
            "hadoop.kms.key.provider.uri": "dbks://http@localhost:9292/kms",
            "hadoop.security.keystore.JavaKeyStoreProvider.password": "none",
            "hadoop.kms.authentication.signer.secret.provider.zookeeper.kerberos.principal": "HTTP/_HOST@EXAMPLE.COM",
            "hadoop.kms.authentication.signer.secret.provider": "random",
            "hadoop.kms.authentication.signer.secret.provider.zookeeper.connection.string": "#HOSTNAME#:#PORT#,...",
            "hadoop.kms.cache.enable": "true",
            "hadoop.kms.cache.timeout.ms": "600000",
             "hadoop.kms.authentication.kerberos.principal": "*"
         },
         "ranger-kms-audit": {
             "xasecure.audit.destination.solr.zookeepers": "c6401.ambari.apache.org:2181/ranger_audits",
            "xasecure.audit.destination.solr.urls": "", 
            "xasecure.audit.destination.solr.batch.filespool.dir": "/var/log/ranger/kms/audit/solr/spool", 
            "xasecure.audit.destination.hdfs.batch.filespool.dir": "/var/log/ranger/kms/audit/hdfs/spool", 
            "xasecure.audit.destination.hdfs": "true", 
            "xasecure.audit.destination.solr.urls": "",
            "xasecure.audit.destination.solr.batch.filespool.dir": "/var/log/ranger/kms/audit/solr/spool",
            "xasecure.audit.destination.hdfs.batch.filespool.dir": "/var/log/ranger/kms/audit/hdfs/spool",
            "xasecure.audit.destination.hdfs": "true",
             "xasecure.audit.destination.solr": "true",
            "xasecure.audit.provider.summary.enabled": "false", 
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit", 
            "xasecure.audit.provider.summary.enabled": "false",
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit",
             "xasecure.audit.is.enabled": "true"
         },
         "ranger-kms-policymgr-ssl": {
            "xasecure.policymgr.clientssl.keystore": "/usr/hdp/current/ranger-kms/conf/ranger-plugin-keystore.jks", 
            "xasecure.policymgr.clientssl.truststore.password": "changeit", 
            "xasecure.policymgr.clientssl.keystore.credential.file": "jceks://file{{credential_file}}", 
            "xasecure.policymgr.clientssl.truststore": "/usr/hdp/current/ranger-kms/conf/ranger-plugin-truststore.jks", 
            "xasecure.policymgr.clientssl.truststore.credential.file": "jceks://file{{credential_file}}", 
            "xasecure.policymgr.clientssl.keystore": "/usr/hdp/current/ranger-kms/conf/ranger-plugin-keystore.jks",
            "xasecure.policymgr.clientssl.truststore.password": "changeit",
            "xasecure.policymgr.clientssl.keystore.credential.file": "jceks://file{{credential_file}}",
            "xasecure.policymgr.clientssl.truststore": "/usr/hdp/current/ranger-kms/conf/ranger-plugin-truststore.jks",
            "xasecure.policymgr.clientssl.truststore.credential.file": "jceks://file{{credential_file}}",
             "xasecure.policymgr.clientssl.keystore.password": "myKeyFilePassword"
         },
         "ranger-kms-security": {
            "ranger.plugin.kms.policy.pollIntervalMs": "30000", 
            "ranger.plugin.kms.service.name": "{{repo_name}}", 
            "ranger.plugin.kms.policy.source.impl": "org.apache.ranger.admin.client.RangerAdminRESTClient", 
            "ranger.plugin.kms.policy.rest.ssl.config.file": "/etc/ranger/kms/conf/ranger-policymgr-ssl.xml", 
            "ranger.plugin.kms.policy.cache.dir": "/etc/ranger/{{repo_name}}/policycache", 
            "ranger.plugin.kms.policy.pollIntervalMs": "30000",
            "ranger.plugin.kms.service.name": "{{repo_name}}",
            "ranger.plugin.kms.policy.source.impl": "org.apache.ranger.admin.client.RangerAdminRESTClient",
            "ranger.plugin.kms.policy.rest.ssl.config.file": "/etc/ranger/kms/conf/ranger-policymgr-ssl.xml",
            "ranger.plugin.kms.policy.cache.dir": "/etc/ranger/{{repo_name}}/policycache",
             "ranger.plugin.kms.policy.rest.url": "{{policymgr_mgr_url}}"
         },
         "ranger-kms-site": {
            "ranger.service.https.port": "9393", 
            "ranger.service.https.attrib.ssl.enabled": "false", 
            "xa.webapp.dir": "./webapp", 
            "ranger.service.host": "{{kms_host}}", 
            "ranger.service.shutdown.port": "7085", 
            "ranger.contextName": "/kms", 
            "ranger.service.https.port": "9393",
            "ranger.service.https.attrib.ssl.enabled": "false",
            "xa.webapp.dir": "./webapp",
            "ranger.service.host": "{{kms_host}}",
            "ranger.service.shutdown.port": "7085",
            "ranger.contextName": "/kms",
             "ranger.service.http.port": "{{kms_port}}"
         }
     }
}
\ No newline at end of file
}
diff --git a/ambari-server/src/test/python/stacks/2.5/configs/ranger-kms-secured.json b/ambari-server/src/test/python/stacks/2.5/configs/ranger-kms-secured.json
index 4e7d8573d2..bcadd039c4 100644
-- a/ambari-server/src/test/python/stacks/2.5/configs/ranger-kms-secured.json
++ b/ambari-server/src/test/python/stacks/2.5/configs/ranger-kms-secured.json
@@ -1,873 +1,873 @@
 {
     "localComponents": [
        "SECONDARY_NAMENODE", 
        "HDFS_CLIENT", 
        "DATANODE", 
        "NAMENODE", 
        "RANGER_ADMIN", 
        "RANGER_TAGSYNC", 
        "RANGER_USERSYNC", 
        "ZOOKEEPER_SERVER", 
        "ZOOKEEPER_CLIENT", 
        "KERBEROS_CLIENT", 
        "SECONDARY_NAMENODE",
        "HDFS_CLIENT",
        "DATANODE",
        "NAMENODE",
        "RANGER_ADMIN",
        "RANGER_TAGSYNC",
        "RANGER_USERSYNC",
        "ZOOKEEPER_SERVER",
        "ZOOKEEPER_CLIENT",
        "KERBEROS_CLIENT",
         "RANGER_KMS_SERVER"
    ], 
    ],
     "configuration_attributes": {
        "ranger-kms-site": {}, 
        "ranger-hdfs-audit": {}, 
        "ssl-client": {}, 
        "ranger-admin-site": {}, 
        "kms-log4j": {}, 
        "ranger-hdfs-policymgr-ssl": {}, 
        "tagsync-application-properties": {}, 
        "ranger-env": {}, 
        "ranger-ugsync-site": {}, 
        "ranger-hdfs-plugin-properties": {}, 
        "ranger-kms-security": {}, 
        "kerberos-env": {}, 
        "kms-properties": {}, 
        "admin-properties": {}, 
        "ranger-kms-policymgr-ssl": {}, 
        "ranger-kms-site": {},
        "ranger-hdfs-audit": {},
        "ssl-client": {},
        "ranger-admin-site": {},
        "kms-log4j": {},
        "ranger-hdfs-policymgr-ssl": {},
        "tagsync-application-properties": {},
        "ranger-env": {},
        "ranger-ugsync-site": {},
        "ranger-hdfs-plugin-properties": {},
        "ranger-kms-security": {},
        "kerberos-env": {},
        "kms-properties": {},
        "admin-properties": {},
        "ranger-kms-policymgr-ssl": {},
         "hdfs-site": {
             "final": {
                "dfs.datanode.data.dir": "true", 
                "dfs.namenode.http-address": "true", 
                "dfs.datanode.failed.volumes.tolerated": "true", 
                "dfs.support.append": "true", 
                "dfs.namenode.name.dir": "true", 
                "dfs.datanode.data.dir": "true",
                "dfs.namenode.http-address": "true",
                "dfs.datanode.failed.volumes.tolerated": "true",
                "dfs.support.append": "true",
                "dfs.namenode.name.dir": "true",
                 "dfs.webhdfs.enabled": "true"
             }
        }, 
        "ranger-tagsync-site": {}, 
        "tagsync-log4j": {}, 
        "ranger-kms-audit": {}, 
        "hadoop-policy": {}, 
        "hdfs-log4j": {}, 
        "usersync-log4j": {}, 
        "krb5-conf": {}, 
        "kms-site": {}, 
        },
        "ranger-tagsync-site": {},
        "tagsync-log4j": {},
        "ranger-kms-audit": {},
        "hadoop-policy": {},
        "hdfs-log4j": {},
        "usersync-log4j": {},
        "krb5-conf": {},
        "kms-site": {},
         "core-site": {
             "final": {
                 "fs.defaultFS": "true"
             }
        }, 
        "hadoop-env": {}, 
        "zookeeper-log4j": {}, 
        "ssl-server": {}, 
        "ranger-site": {}, 
        "zookeeper-env": {}, 
        "admin-log4j": {}, 
        "zoo.cfg": {}, 
        "ranger-hdfs-security": {}, 
        "usersync-properties": {}, 
        "kms-env": {}, 
        "dbks-site": {}, 
        },
        "hadoop-env": {},
        "zookeeper-log4j": {},
        "ssl-server": {},
        "ranger-site": {},
        "zookeeper-env": {},
        "admin-log4j": {},
        "zoo.cfg": {},
        "ranger-hdfs-security": {},
        "usersync-properties": {},
        "kms-env": {},
        "dbks-site": {},
         "cluster-env": {}
    }, 
    "public_hostname": "c6401.ambari.apache.org", 
    "commandId": "43-0", 
    "hostname": "c6401.ambari.apache.org", 
    "kerberosCommandParams": [], 
    "serviceName": "RANGER_KMS", 
    "role": "RANGER_KMS_SERVER", 
    "forceRefreshConfigTagsBeforeExecution": [], 
    "requestId": 43, 
    },
    "public_hostname": "c6401.ambari.apache.org",
    "commandId": "43-0",
    "hostname": "c6401.ambari.apache.org",
    "kerberosCommandParams": [],
    "serviceName": "RANGER_KMS",
    "role": "RANGER_KMS_SERVER",
    "forceRefreshConfigTagsBeforeExecution": [],
    "requestId": 43,
     "agentConfigParams": {
         "agent": {
             "parallel_execution": 0
         }
    }, 
    "clusterName": "c1", 
    "commandType": "EXECUTION_COMMAND", 
    "taskId": 200, 
    "roleParams": {}, 
    },
    "clusterName": "c1",
    "commandType": "EXECUTION_COMMAND",
    "taskId": 200,
    "roleParams": {},
     "configurationTags": {
         "ranger-kms-site": {
             "tag": "version1467026737262"
        }, 
        },
         "ranger-hdfs-audit": {
             "tag": "version1466705299922"
        }, 
        },
         "ssl-client": {
             "tag": "version1"
        }, 
        },
         "ranger-admin-site": {
             "tag": "version1467016680635"
        }, 
        },
         "kms-log4j": {
             "tag": "version1467026737262"
        }, 
        },
         "ranger-hdfs-policymgr-ssl": {
             "tag": "version1466705299922"
        }, 
        },
         "tagsync-application-properties": {
             "tag": "version1467016680511"
        }, 
        },
         "ranger-env": {
             "tag": "version1466705299949"
        }, 
        },
         "ranger-ugsync-site": {
             "tag": "version1467016680537"
        }, 
        },
         "ranger-hdfs-plugin-properties": {
             "tag": "version1466705299922"
        }, 
        },
         "ranger-kms-security": {
             "tag": "version1467026737262"
        }, 
        },
         "kerberos-env": {
             "tag": "version1467016537243"
        }, 
        },
         "admin-log4j": {
             "tag": "version1466705299949"
        }, 
        },
         "admin-properties": {
             "tag": "version1466705299949"
        }, 
        },
         "ranger-kms-policymgr-ssl": {
             "tag": "version1467026737262"
        }, 
        },
         "hdfs-site": {
             "tag": "version1467016680401"
        }, 
        },
         "ranger-tagsync-site": {
             "tag": "version1467016680586"
        }, 
        },
         "zoo.cfg": {
             "tag": "version1"
        }, 
        },
         "ranger-kms-audit": {
             "tag": "version1467026737262"
        }, 
        },
         "hadoop-policy": {
             "tag": "version1"
        }, 
        },
         "hdfs-log4j": {
             "tag": "version1"
        }, 
        },
         "usersync-log4j": {
             "tag": "version1466705299949"
        }, 
        },
         "krb5-conf": {
             "tag": "version1467016537243"
        }, 
        },
         "kms-site": {
             "tag": "version1467026751210"
        }, 
        },
         "core-site": {
             "tag": "version1467026751256"
        }, 
        },
         "hadoop-env": {
             "tag": "version1467016680446"
        }, 
        },
         "zookeeper-log4j": {
             "tag": "version1"
        }, 
        },
         "ssl-server": {
             "tag": "version1"
        }, 
        },
         "ranger-site": {
             "tag": "version1466705299949"
        }, 
        },
         "zookeeper-env": {
             "tag": "version1467016680492"
        }, 
        },
         "kms-properties": {
             "tag": "version1467026737262"
        }, 
        },
         "tagsync-log4j": {
             "tag": "version1466705299949"
        }, 
        },
         "ranger-hdfs-security": {
             "tag": "version1466705299922"
        }, 
        },
         "usersync-properties": {
             "tag": "version1466705299949"
        }, 
        },
         "kms-env": {
             "tag": "version1467026737262"
        }, 
        },
         "dbks-site": {
             "tag": "version1467026751234"
        }, 
        },
         "cluster-env": {
             "tag": "version1467016680567"
         }
    }, 
    "roleCommand": "START", 
    },
    "roleCommand": "START",
     "hostLevelParams": {
        "agent_stack_retry_on_unavailability": "false", 
        "stack_name": "HDP", 
        "package_version": "2_5_0_0_*", 
        "agent_stack_retry_on_unavailability": "false",
        "stack_name": "HDP",
        "package_version": "2_5_0_0_*",
         "custom_mysql_jdbc_name": "mysql-connector-java.jar",
         "previous_custom_mysql_jdbc_name": "mysql-connector-java-old.jar",
        "host_sys_prepped": "false", 
        "ambari_db_rca_username": "mapred", 
        "current_version": "2.5.0.0-801", 
        "mysql_jdbc_url": "http://c6401.ambari.apache.org:8080/resources//mysql-connector-java.jar", 
        "agent_stack_retry_count": "5", 
        "stack_version": "2.5", 
        "jdk_name": "jdk-8u60-linux-x64.tar.gz", 
        "ambari_db_rca_driver": "org.postgresql.Driver", 
        "java_home": "/usr/jdk64/jdk1.7.0_45", 
        "repository_version_id": "1", 
        "jdk_location": "http://c6401.ambari.apache.org:8080/resources/", 
        "not_managed_hdfs_path_list": "[\"/tmp\"]", 
        "ambari_db_rca_url": "jdbc:postgresql://c6401.ambari.apache.org/ambarirca", 
        "java_version": "8", 
        "repo_info": "[{\"baseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.5.0.0-801\",\"osType\":\"redhat6\",\"repoId\":\"HDP-2.5\",\"repoName\":\"HDP\",\"defaultBaseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/updates/2.5.0.0\",\"latestBaseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.5.0.0-801\",\"baseSaved\":true},{\"baseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"osType\":\"redhat6\",\"repoId\":\"HDP-UTILS-1.1.0.21\",\"repoName\":\"HDP-UTILS\",\"defaultBaseUrl\":\"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"latestBaseUrl\":\"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"baseSaved\":true}]", 
        "package_list": "[{\"name\":\"ranger_${stack_version}-kms\",\"condition\":\"\",\"skipUpgrade\":false}]", 
        "db_name": "ambari", 
        "group_list": "[\"kms\",\"ranger\",\"hadoop\",\"users\"]", 
        "agentCacheDir": "/var/lib/ambari-agent/cache", 
        "ambari_db_rca_password": "mapred", 
        "jce_name": "UnlimitedJCEPolicyJDK7.zip", 
        "oracle_jdbc_url": "http://c6401.ambari.apache.org:8080/resources//ojdbc6.jar", 
        "db_driver_filename": "mysql-connector-java.jar", 
        "user_list": "[\"kms\",\"zookeeper\",\"ambari-qa\",\"ranger\",\"hdfs\"]", 
        "host_sys_prepped": "false",
        "ambari_db_rca_username": "mapred",
        "current_version": "2.5.0.0-801",
        "mysql_jdbc_url": "http://c6401.ambari.apache.org:8080/resources//mysql-connector-java.jar",
        "agent_stack_retry_count": "5",
        "stack_version": "2.5",
        "jdk_name": "jdk-8u60-linux-x64.tar.gz",
        "ambari_db_rca_driver": "org.postgresql.Driver",
        "java_home": "/usr/jdk64/jdk1.7.0_45",
        "repository_version_id": "1",
        "jdk_location": "http://c6401.ambari.apache.org:8080/resources/",
        "not_managed_hdfs_path_list": "[\"/tmp\"]",
        "ambari_db_rca_url": "jdbc:postgresql://c6401.ambari.apache.org/ambarirca",
        "java_version": "8",
        "repo_info": "[{\"baseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.5.0.0-801\",\"osType\":\"redhat6\",\"repoId\":\"HDP-2.5\",\"repoName\":\"HDP\",\"defaultBaseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/updates/2.5.0.0\",\"latestBaseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.5.0.0-801\",\"baseSaved\":true},{\"baseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"osType\":\"redhat6\",\"repoId\":\"HDP-UTILS-1.1.0.21\",\"repoName\":\"HDP-UTILS\",\"defaultBaseUrl\":\"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"latestBaseUrl\":\"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"baseSaved\":true}]",
        "package_list": "[{\"name\":\"ranger_${stack_version}-kms\",\"condition\":\"\",\"skipUpgrade\":false}]",
        "db_name": "ambari",
        "group_list": "[\"kms\",\"ranger\",\"hadoop\",\"users\"]",
        "agentCacheDir": "/var/lib/ambari-agent/cache",
        "ambari_db_rca_password": "mapred",
        "jce_name": "UnlimitedJCEPolicyJDK7.zip",
        "oracle_jdbc_url": "http://c6401.ambari.apache.org:8080/resources//ojdbc6.jar",
        "db_driver_filename": "mysql-connector-java.jar",
        "user_list": "[\"kms\",\"zookeeper\",\"ambari-qa\",\"ranger\",\"hdfs\"]",
         "clientsToUpdateConfigs": "[\"*\"]"
    }, 
    },
     "commandParams": {
        "service_package_folder": "common-services/RANGER_KMS/0.5.0.2.3/package", 
        "script": "scripts/kms_server.py", 
        "hooks_folder": "HDP/2.0.6/hooks", 
        "version": "2.5.0.0-801", 
        "max_duration_for_retries": "0", 
        "command_retry_enabled": "false", 
        "command_timeout": "600", 
        "service_package_folder": "common-services/RANGER_KMS/0.5.0.2.3/package",
        "script": "scripts/kms_server.py",
        "hooks_folder": "HDP/2.0.6/hooks",
        "version": "2.5.0.0-801",
        "max_duration_for_retries": "0",
        "command_retry_enabled": "false",
        "command_timeout": "600",
         "script_type": "PYTHON"
    }, 
    "forceRefreshConfigTags": [], 
    "stageId": 0, 
    },
    "forceRefreshConfigTags": [],
    "stageId": 0,
     "clusterHostInfo": {
         "snamenode_host": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "ambari_server_use_ssl": [
             "false"
        ], 
        ],
         "all_ping_ports": [
             "8670"
        ], 
        ],
         "ranger_tagsync_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "ranger_kms_server_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "ranger_usersync_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "all_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "slave_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "namenode_host": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "ambari_server_port": [
             "8080"
        ], 
        ],
         "ranger_admin_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "all_racks": [
             "/default-rack"
        ], 
        ],
         "all_ipv4_ips": [
             "172.22.83.73"
        ], 
        ],
         "ambari_server_host": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "zookeeper_hosts": [
             "c6401.ambari.apache.org"
         ]
    }, 
    },
     "configurations": {
         "ranger-kms-site": {
            "ranger.service.https.port": "9393", 
            "ranger.service.https.attrib.ssl.enabled": "false", 
            "xa.webapp.dir": "./webapp", 
            "ranger.service.host": "{{kms_host}}", 
            "ranger.service.shutdown.port": "7085", 
            "ranger.contextName": "/kms", 
            "ranger.service.https.port": "9393",
            "ranger.service.https.attrib.ssl.enabled": "false",
            "xa.webapp.dir": "./webapp",
            "ranger.service.host": "{{kms_host}}",
            "ranger.service.shutdown.port": "7085",
            "ranger.contextName": "/kms",
             "ranger.service.http.port": "{{kms_port}}"
        }, 
        },
         "ranger-hdfs-audit": {
             "xasecure.audit.destination.solr.zookeepers": "NONE",
            "xasecure.audit.destination.solr.urls": "", 
            "xasecure.audit.destination.solr.batch.filespool.dir": "/var/log/hadoop/hdfs/audit/solr/spool", 
            "xasecure.audit.destination.solr.urls": "",
            "xasecure.audit.destination.solr.batch.filespool.dir": "/var/log/hadoop/hdfs/audit/solr/spool",
             "xasecure.audit.destination.hdfs.batch.filespool.dir": "/var/log/hadoop/hdfs/audit/hdfs/spool",
            "xasecure.audit.destination.hdfs": "true", 
            "xasecure.audit.destination.solr": "false", 
            "xasecure.audit.provider.summary.enabled": "false", 
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit", 
            "xasecure.audit.destination.hdfs": "true",
            "xasecure.audit.destination.solr": "false",
            "xasecure.audit.provider.summary.enabled": "false",
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit",
             "xasecure.audit.is.enabled": "true"
        }, 
        },
         "ssl-client": {
            "ssl.client.truststore.reload.interval": "10000", 
            "ssl.client.keystore.password": "bigdata", 
            "ssl.client.truststore.type": "jks", 
            "ssl.client.keystore.location": "/etc/security/clientKeys/keystore.jks", 
            "ssl.client.truststore.location": "/etc/security/clientKeys/all.jks", 
            "ssl.client.truststore.password": "bigdata", 
            "ssl.client.truststore.reload.interval": "10000",
            "ssl.client.keystore.password": "bigdata",
            "ssl.client.truststore.type": "jks",
            "ssl.client.keystore.location": "/etc/security/clientKeys/keystore.jks",
            "ssl.client.truststore.location": "/etc/security/clientKeys/all.jks",
            "ssl.client.truststore.password": "bigdata",
             "ssl.client.keystore.type": "jks"
        }, 
        },
         "ranger-admin-site": {
            "ranger.admin.kerberos.cookie.domain": "{{ranger_host}}", 
            "ranger.kms.service.user.hdfs": "hdfs", 
            "ranger.spnego.kerberos.principal": "HTTP/_HOST@EXAMPLE.COM", 
            "ranger.ldap.ad.url": "{{ranger_ug_ldap_url}}", 
            "ranger.plugins.hive.serviceuser": "hive", 
            "ranger.lookup.kerberos.keytab": "/etc/security/keytabs/rangerlookup.service.keytab", 
            "ranger.plugins.kms.serviceuser": "kms", 
            "ranger.service.https.attrib.ssl.enabled": "false", 
            "ranger.sso.browser.useragent": "Mozilla,chrome", 
            "ranger.jpa.jdbc.url": "jdbc:mysql://c6401.ambari.apache.org:3306/ranger01", 
            "ranger.plugins.hbase.serviceuser": "hbase", 
            "ranger.admin.kerberos.cookie.domain": "{{ranger_host}}",
            "ranger.kms.service.user.hdfs": "hdfs",
            "ranger.spnego.kerberos.principal": "HTTP/_HOST@EXAMPLE.COM",
            "ranger.ldap.ad.url": "{{ranger_ug_ldap_url}}",
            "ranger.plugins.hive.serviceuser": "hive",
            "ranger.lookup.kerberos.keytab": "/etc/security/keytabs/rangerlookup.service.keytab",
            "ranger.plugins.kms.serviceuser": "kms",
            "ranger.service.https.attrib.ssl.enabled": "false",
            "ranger.sso.browser.useragent": "Mozilla,chrome",
            "ranger.jpa.jdbc.url": "jdbc:mysql://c6401.ambari.apache.org:3306/ranger01",
            "ranger.plugins.hbase.serviceuser": "hbase",
             "ranger.plugins.hdfs.serviceuser": "hdfs",
            "ranger.ldap.group.searchbase": "{{ranger_ug_ldap_group_searchbase}}", 
            "ranger.ldap.user.dnpattern": "uid={0},ou=users,dc=xasecure,dc=net", 
            "ranger.plugins.knox.serviceuser": "knox", 
            "ranger.ldap.base.dn": "dc=example,dc=com", 
            "ranger.sso.publicKey": "", 
            "ranger.admin.kerberos.cookie.path": "/", 
            "ranger.ldap.group.searchbase": "{{ranger_ug_ldap_group_searchbase}}",
            "ranger.ldap.user.dnpattern": "uid={0},ou=users,dc=xasecure,dc=net",
            "ranger.plugins.knox.serviceuser": "knox",
            "ranger.ldap.base.dn": "dc=example,dc=com",
            "ranger.sso.publicKey": "",
            "ranger.admin.kerberos.cookie.path": "/",
             "ranger.service.https.attrib.clientAuth": "want",
            "ranger.jpa.jdbc.user": "{{ranger_db_user}}", 
            "ranger.ldap.ad.user.searchfilter": "(sAMAccountName={0})", 
            "ranger.ldap.group.roleattribute": "cn", 
            "ranger.plugins.kafka.serviceuser": "kafka", 
            "ranger.admin.kerberos.principal": "rangeradmin/_HOST@EXAMPLE.COM", 
            "ranger.ldap.ad.bind.dn": "{{ranger_ug_ldap_bind_dn}}", 
            "ranger.jpa.jdbc.user": "{{ranger_db_user}}",
            "ranger.ldap.ad.user.searchfilter": "(sAMAccountName={0})",
            "ranger.ldap.group.roleattribute": "cn",
            "ranger.plugins.kafka.serviceuser": "kafka",
            "ranger.admin.kerberos.principal": "rangeradmin/_HOST@EXAMPLE.COM",
            "ranger.ldap.ad.bind.dn": "{{ranger_ug_ldap_bind_dn}}",
             "ranger.credential.provider.path": "/etc/ranger/admin/rangeradmin.jceks",
            "ranger.ldap.referral": "ignore", 
            "ranger.service.http.port": "6080", 
            "ranger.ldap.user.searchfilter": "(uid={0})", 
            "ranger.plugins.atlas.serviceuser": "atlas", 
            "ranger.truststore.password": "changeit", 
            "ranger.ldap.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}", 
            "ranger.audit.solr.password": "NONE", 
            "ranger.audit.solr.zookeepers": "NONE", 
            "ranger.lookup.kerberos.principal": "rangerlookup/_HOST@EXAMPLE.COM", 
            "ranger.service.https.port": "6182", 
            "ranger.plugins.storm.serviceuser": "storm", 
            "ranger.externalurl": "{{ranger_external_url}}", 
            "ranger.truststore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks", 
            "ranger.kms.service.user.hive": "", 
            "ranger.https.attrib.keystore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks", 
            "ranger.jpa.jdbc.dialect": "{{jdbc_dialect}}", 
            "ranger.service.host": "{{ranger_host}}", 
            "ranger.ldap.referral": "ignore",
            "ranger.service.http.port": "6080",
            "ranger.ldap.user.searchfilter": "(uid={0})",
            "ranger.plugins.atlas.serviceuser": "atlas",
            "ranger.truststore.password": "changeit",
            "ranger.ldap.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}",
            "ranger.audit.solr.password": "NONE",
            "ranger.audit.solr.zookeepers": "NONE",
            "ranger.lookup.kerberos.principal": "rangerlookup/_HOST@EXAMPLE.COM",
            "ranger.service.https.port": "6182",
            "ranger.plugins.storm.serviceuser": "storm",
            "ranger.externalurl": "{{ranger_external_url}}",
            "ranger.truststore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks",
            "ranger.kms.service.user.hive": "",
            "ranger.https.attrib.keystore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks",
            "ranger.jpa.jdbc.dialect": "{{jdbc_dialect}}",
            "ranger.service.host": "{{ranger_host}}",
             "ranger.service.https.attrib.keystore.keyalias": "rangeradmin",
            "ranger.service.https.attrib.keystore.pass": "xasecure", 
            "ranger.unixauth.remote.login.enabled": "true", 
            "ranger.jpa.jdbc.credential.alias": "rangeradmin", 
            "ranger.ldap.ad.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}", 
            "ranger.audit.solr.username": "ranger_solr", 
            "ranger.sso.enabled": "false", 
            "ranger.audit.solr.urls": "", 
            "ranger.ldap.ad.domain": "", 
            "ranger.plugins.yarn.serviceuser": "yarn", 
            "ranger.audit.source.type": "solr", 
            "ranger.ldap.bind.dn": "{{ranger_ug_ldap_bind_dn}}", 
            "ranger.ldap.url": "{{ranger_ug_ldap_url}}", 
            "ranger.authentication.method": "UNIX", 
            "ranger.service.http.enabled": "true", 
            "ranger.ldap.group.searchfilter": "{{ranger_ug_ldap_group_searchfilter}}", 
            "ranger.ldap.ad.referral": "ignore", 
            "ranger.ldap.ad.base.dn": "dc=example,dc=com", 
            "ranger.jpa.jdbc.password": "_", 
            "ranger.spnego.kerberos.keytab": "/etc/security/keytabs/spnego.service.keytab", 
            "ranger.sso.providerurl": "", 
            "ranger.unixauth.service.hostname": "{{ugsync_host}}", 
            "ranger.admin.kerberos.keytab": "/etc/security/keytabs/rangeradmin.service.keytab", 
            "ranger.admin.kerberos.token.valid.seconds": "30", 
            "ranger.jpa.jdbc.driver": "com.mysql.jdbc.Driver", 
            "ranger.service.https.attrib.keystore.pass": "xasecure",
            "ranger.unixauth.remote.login.enabled": "true",
            "ranger.jpa.jdbc.credential.alias": "rangeradmin",
            "ranger.ldap.ad.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}",
            "ranger.audit.solr.username": "ranger_solr",
            "ranger.sso.enabled": "false",
            "ranger.audit.solr.urls": "",
            "ranger.ldap.ad.domain": "",
            "ranger.plugins.yarn.serviceuser": "yarn",
            "ranger.audit.source.type": "solr",
            "ranger.ldap.bind.dn": "{{ranger_ug_ldap_bind_dn}}",
            "ranger.ldap.url": "{{ranger_ug_ldap_url}}",
            "ranger.authentication.method": "UNIX",
            "ranger.service.http.enabled": "true",
            "ranger.ldap.group.searchfilter": "{{ranger_ug_ldap_group_searchfilter}}",
            "ranger.ldap.ad.referral": "ignore",
            "ranger.ldap.ad.base.dn": "dc=example,dc=com",
            "ranger.jpa.jdbc.password": "_",
            "ranger.spnego.kerberos.keytab": "/etc/security/keytabs/spnego.service.keytab",
            "ranger.sso.providerurl": "",
            "ranger.unixauth.service.hostname": "{{ugsync_host}}",
            "ranger.admin.kerberos.keytab": "/etc/security/keytabs/rangeradmin.service.keytab",
            "ranger.admin.kerberos.token.valid.seconds": "30",
            "ranger.jpa.jdbc.driver": "com.mysql.jdbc.Driver",
             "ranger.unixauth.service.port": "5151"
        }, 
        },
         "kms-log4j": {
             "content": "\n#\n# Licensed under the Apache License, Version 2.0 (the \"License\");\n# you may not use this file except in compliance with the License.\n# You may obtain a copy of the License at\n#\n#    http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License. See accompanying LICENSE file.\n#\n\n# If the Java System property 'kms.log.dir' is not defined at KMS start up time\n# Setup sets its value to '${kms.home}/logs'\n\nlog4j.appender.kms=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.kms.DatePattern='.'yyyy-MM-dd\nlog4j.appender.kms.File=${kms.log.dir}/kms.log\nlog4j.appender.kms.Append=true\nlog4j.appender.kms.layout=org.apache.log4j.PatternLayout\nlog4j.appender.kms.layout.ConversionPattern=%d{ISO8601} %-5p %c{1} - %m%n\n\nlog4j.appender.kms-audit=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.kms-audit.DatePattern='.'yyyy-MM-dd\nlog4j.appender.kms-audit.File=${kms.log.dir}/kms-audit.log\nlog4j.appender.kms-audit.Append=true\nlog4j.appender.kms-audit.layout=org.apache.log4j.PatternLayout\nlog4j.appender.kms-audit.layout.ConversionPattern=%d{ISO8601} %m%n\n\nlog4j.logger.kms-audit=INFO, kms-audit\nlog4j.additivity.kms-audit=false\n\nlog4j.rootLogger=ALL, kms\nlog4j.logger.org.apache.hadoop.conf=ERROR\nlog4j.logger.org.apache.hadoop=INFO\nlog4j.logger.com.sun.jersey.server.wadl.generators.WadlGeneratorJAXBGrammarGenerator=OFF"
        }, 
        },
         "ranger-hdfs-policymgr-ssl": {
            "xasecure.policymgr.clientssl.keystore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-keystore.jks", 
            "xasecure.policymgr.clientssl.truststore.password": "changeit", 
            "xasecure.policymgr.clientssl.keystore.credential.file": "jceks://file{{credential_file}}", 
            "xasecure.policymgr.clientssl.truststore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-truststore.jks", 
            "xasecure.policymgr.clientssl.truststore.credential.file": "jceks://file{{credential_file}}", 
            "xasecure.policymgr.clientssl.keystore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-keystore.jks",
            "xasecure.policymgr.clientssl.truststore.password": "changeit",
            "xasecure.policymgr.clientssl.keystore.credential.file": "jceks://file{{credential_file}}",
            "xasecure.policymgr.clientssl.truststore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-truststore.jks",
            "xasecure.policymgr.clientssl.truststore.credential.file": "jceks://file{{credential_file}}",
             "xasecure.policymgr.clientssl.keystore.password": "myKeyFilePassword"
        }, 
        },
         "tagsync-application-properties": {
            "atlas.kafka.zookeeper.connect": "c6401.ambari.apache.org:2181", 
            "atlas.kafka.security.protocol": "SASL_PLAINTEXT", 
            "atlas.jaas.KafkaClient.option.principal": "{{tagsync_jaas_principal}}", 
            "atlas.jaas.KafkaClient.option.keyTab": "{{tagsync_keytab_path}}", 
            "atlas.kafka.entities.group.id": "ranger_entities_consumer", 
            "atlas.jaas.KafkaClient.loginModuleName": "com.sun.security.auth.module.Krb5LoginModule", 
            "atlas.jaas.KafkaClient.option.serviceName": "kafka", 
            "atlas.kafka.bootstrap.servers": "localhost:6667", 
            "atlas.jaas.KafkaClient.option.useKeyTab": "true", 
            "atlas.jaas.KafkaClient.option.storeKey": "true", 
            "atlas.jaas.KafkaClient.loginModuleControlFlag": "required", 
            "atlas.kafka.zookeeper.connect": "c6401.ambari.apache.org:2181",
            "atlas.kafka.security.protocol": "SASL_PLAINTEXT",
            "atlas.jaas.KafkaClient.option.principal": "{{tagsync_jaas_principal}}",
            "atlas.jaas.KafkaClient.option.keyTab": "{{tagsync_keytab_path}}",
            "atlas.kafka.entities.group.id": "ranger_entities_consumer",
            "atlas.jaas.KafkaClient.loginModuleName": "com.sun.security.auth.module.Krb5LoginModule",
            "atlas.jaas.KafkaClient.option.serviceName": "kafka",
            "atlas.kafka.bootstrap.servers": "localhost:6667",
            "atlas.jaas.KafkaClient.option.useKeyTab": "true",
            "atlas.jaas.KafkaClient.option.storeKey": "true",
            "atlas.jaas.KafkaClient.loginModuleControlFlag": "required",
             "atlas.kafka.sasl.kerberos.service.name": "kafka"
        }, 
        },
         "ranger-env": {
            "ranger_solr_shards": "1", 
            "ranger_solr_config_set": "ranger_audits", 
            "ranger_user": "ranger", 
            "xml_configurations_supported": "true", 
            "ranger-atlas-plugin-enabled": "No", 
            "ranger-hbase-plugin-enabled": "No", 
            "ranger-yarn-plugin-enabled": "No", 
            "bind_anonymous": "false", 
            "ranger_admin_username": "amb_ranger_admin", 
            "admin_password": "admin", 
            "is_solrCloud_enabled": "false", 
            "ranger-storm-plugin-enabled": "No", 
            "ranger-hdfs-plugin-enabled": "No", 
            "ranger_group": "ranger", 
            "ranger-knox-plugin-enabled": "No", 
            "ranger_admin_log_dir": "/var/log/ranger/admin", 
            "ranger-kafka-plugin-enabled": "No", 
            "ranger_privelege_user_jdbc_url": "jdbc:mysql://c6401.ambari.apache.org:3306", 
            "ranger-hive-plugin-enabled": "No", 
            "xasecure.audit.destination.solr": "false", 
            "ranger_pid_dir": "/var/run/ranger", 
            "xasecure.audit.destination.hdfs": "true", 
            "admin_username": "admin", 
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit", 
            "create_db_dbuser": "true", 
            "ranger_solr_collection_name": "ranger_audits", 
            "ranger_admin_password": "P1!q9xa96SMi5NCl", 
            "ranger_solr_shards": "1",
            "ranger_solr_config_set": "ranger_audits",
            "ranger_user": "ranger",
            "xml_configurations_supported": "true",
            "ranger-atlas-plugin-enabled": "No",
            "ranger-hbase-plugin-enabled": "No",
            "ranger-yarn-plugin-enabled": "No",
            "bind_anonymous": "false",
            "ranger_admin_username": "amb_ranger_admin",
            "admin_password": "admin",
            "is_solrCloud_enabled": "false",
            "ranger-storm-plugin-enabled": "No",
            "ranger-hdfs-plugin-enabled": "No",
            "ranger_group": "ranger",
            "ranger-knox-plugin-enabled": "No",
            "ranger_admin_log_dir": "/var/log/ranger/admin",
            "ranger-kafka-plugin-enabled": "No",
            "ranger_privelege_user_jdbc_url": "jdbc:mysql://c6401.ambari.apache.org:3306",
            "ranger-hive-plugin-enabled": "No",
            "xasecure.audit.destination.solr": "false",
            "ranger_pid_dir": "/var/run/ranger",
            "xasecure.audit.destination.hdfs": "true",
            "admin_username": "admin",
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit",
            "create_db_dbuser": "true",
            "ranger_solr_collection_name": "ranger_audits",
            "ranger_admin_password": "P1!q9xa96SMi5NCl",
             "ranger_usersync_log_dir": "/var/log/ranger/usersync"
        }, 
        },
         "ranger-ugsync-site": {
            "ranger.usersync.ldap.binddn": "", 
            "ranger.usersync.policymgr.username": "rangerusersync", 
            "ranger.usersync.policymanager.mockrun": "false", 
            "ranger.usersync.group.searchbase": "", 
            "ranger.usersync.ldap.bindalias": "testldapalias", 
            "ranger.usersync.truststore.file": "/usr/hdp/current/ranger-usersync/conf/mytruststore.jks", 
            "ranger.usersync.port": "5151", 
            "ranger.usersync.pagedresultssize": "500", 
            "ranger.usersync.group.memberattributename": "", 
            "ranger.usersync.kerberos.principal": "rangerusersync/_HOST@EXAMPLE.COM", 
            "ranger.usersync.source.impl.class": "org.apache.ranger.unixusersync.process.UnixUserGroupBuilder", 
            "ranger.usersync.ldap.referral": "ignore", 
            "ranger.usersync.group.searchfilter": "", 
            "ranger.usersync.ldap.user.objectclass": "person", 
            "ranger.usersync.logdir": "{{usersync_log_dir}}", 
            "ranger.usersync.ldap.user.searchfilter": "", 
            "ranger.usersync.ldap.groupname.caseconversion": "none", 
            "ranger.usersync.ldap.ldapbindpassword": "", 
            "ranger.usersync.unix.minUserId": "500", 
            "ranger.usersync.policymanager.maxrecordsperapicall": "1000", 
            "ranger.usersync.group.nameattribute": "", 
            "ranger.usersync.policymgr.alias": "ranger.usersync.policymgr.password", 
            "ranger.usersync.keystore.file": "/usr/hdp/current/ranger-usersync/conf/unixauthservice.jks", 
            "ranger.usersync.user.searchenabled": "false", 
            "ranger.usersync.group.usermapsyncenabled": "true", 
            "ranger.usersync.ldap.bindkeystore": "", 
            "ranger.usersync.ldap.user.groupnameattribute": "memberof, ismemberof", 
            "ranger.usersync.kerberos.keytab": "/etc/security/keytabs/rangerusersync.service.keytab", 
            "ranger.usersync.passwordvalidator.path": "./native/credValidator.uexe", 
            "ranger.usersync.group.objectclass": "", 
            "ranger.usersync.ldap.user.searchscope": "sub", 
            "ranger.usersync.unix.password.file": "/etc/passwd", 
            "ranger.usersync.ldap.user.nameattribute": "", 
            "ranger.usersync.pagedresultsenabled": "true", 
            "ranger.usersync.policymanager.baseURL": "{{ranger_external_url}}", 
            "ranger.usersync.group.search.first.enabled": "false", 
            "ranger.usersync.group.searchenabled": "false", 
            "ranger.usersync.sink.impl.class": "org.apache.ranger.unixusersync.process.PolicyMgrUserGroupBuilder", 
            "ranger.usersync.ssl": "true", 
            "ranger.usersync.ldap.url": "", 
            "ranger.usersync.ldap.searchBase": "dc=hadoop,dc=apache,dc=org", 
            "ranger.usersync.policymgr.keystore": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks", 
            "ranger.usersync.ldap.user.searchbase": "", 
            "ranger.usersync.ldap.username.caseconversion": "none", 
            "ranger.usersync.credstore.filename": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks", 
            "ranger.usersync.keystore.password": "UnIx529p", 
            "ranger.usersync.unix.group.file": "/etc/group", 
            "ranger.usersync.filesource.file": "/tmp/usergroup.txt", 
            "ranger.usersync.group.searchscope": "", 
            "ranger.usersync.truststore.password": "changeit", 
            "ranger.usersync.enabled": "true", 
            "ranger.usersync.sleeptimeinmillisbetweensynccycle": "60000", 
            "ranger.usersync.ldap.binddn": "",
            "ranger.usersync.policymgr.username": "rangerusersync",
            "ranger.usersync.policymanager.mockrun": "false",
            "ranger.usersync.group.searchbase": "",
            "ranger.usersync.ldap.bindalias": "testldapalias",
            "ranger.usersync.truststore.file": "/usr/hdp/current/ranger-usersync/conf/mytruststore.jks",
            "ranger.usersync.port": "5151",
            "ranger.usersync.pagedresultssize": "500",
            "ranger.usersync.group.memberattributename": "",
            "ranger.usersync.kerberos.principal": "rangerusersync/_HOST@EXAMPLE.COM",
            "ranger.usersync.source.impl.class": "org.apache.ranger.unixusersync.process.UnixUserGroupBuilder",
            "ranger.usersync.ldap.referral": "ignore",
            "ranger.usersync.group.searchfilter": "",
            "ranger.usersync.ldap.user.objectclass": "person",
            "ranger.usersync.logdir": "{{usersync_log_dir}}",
            "ranger.usersync.ldap.user.searchfilter": "",
            "ranger.usersync.ldap.groupname.caseconversion": "none",
            "ranger.usersync.ldap.ldapbindpassword": "",
            "ranger.usersync.unix.minUserId": "500",
            "ranger.usersync.policymanager.maxrecordsperapicall": "1000",
            "ranger.usersync.group.nameattribute": "",
            "ranger.usersync.policymgr.alias": "ranger.usersync.policymgr.password",
            "ranger.usersync.keystore.file": "/usr/hdp/current/ranger-usersync/conf/unixauthservice.jks",
            "ranger.usersync.user.searchenabled": "false",
            "ranger.usersync.group.usermapsyncenabled": "true",
            "ranger.usersync.ldap.bindkeystore": "",
            "ranger.usersync.ldap.user.groupnameattribute": "memberof, ismemberof",
            "ranger.usersync.kerberos.keytab": "/etc/security/keytabs/rangerusersync.service.keytab",
            "ranger.usersync.passwordvalidator.path": "./native/credValidator.uexe",
            "ranger.usersync.group.objectclass": "",
            "ranger.usersync.ldap.user.searchscope": "sub",
            "ranger.usersync.unix.password.file": "/etc/passwd",
            "ranger.usersync.ldap.user.nameattribute": "",
            "ranger.usersync.pagedresultsenabled": "true",
            "ranger.usersync.policymanager.baseURL": "{{ranger_external_url}}",
            "ranger.usersync.group.search.first.enabled": "false",
            "ranger.usersync.group.searchenabled": "false",
            "ranger.usersync.sink.impl.class": "org.apache.ranger.unixusersync.process.PolicyMgrUserGroupBuilder",
            "ranger.usersync.ssl": "true",
            "ranger.usersync.ldap.url": "",
            "ranger.usersync.ldap.searchBase": "dc=hadoop,dc=apache,dc=org",
            "ranger.usersync.policymgr.keystore": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks",
            "ranger.usersync.ldap.user.searchbase": "",
            "ranger.usersync.ldap.username.caseconversion": "none",
            "ranger.usersync.credstore.filename": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks",
            "ranger.usersync.keystore.password": "UnIx529p",
            "ranger.usersync.unix.group.file": "/etc/group",
            "ranger.usersync.filesource.file": "/tmp/usergroup.txt",
            "ranger.usersync.group.searchscope": "",
            "ranger.usersync.truststore.password": "changeit",
            "ranger.usersync.enabled": "true",
            "ranger.usersync.sleeptimeinmillisbetweensynccycle": "60000",
             "ranger.usersync.filesource.text.delimiter": ","
        }, 
        },
         "ranger-hdfs-plugin-properties": {
            "hadoop.rpc.protection": "authentication", 
            "ranger-hdfs-plugin-enabled": "No", 
            "REPOSITORY_CONFIG_USERNAME": "hadoop", 
            "policy_user": "ambari-qa", 
            "common.name.for.certificate": "", 
            "hadoop.rpc.protection": "authentication",
            "ranger-hdfs-plugin-enabled": "No",
            "REPOSITORY_CONFIG_USERNAME": "hadoop",
            "policy_user": "ambari-qa",
            "common.name.for.certificate": "",
             "REPOSITORY_CONFIG_PASSWORD": "hadoop"
        }, 
        },
         "ranger-kms-security": {
            "ranger.plugin.kms.policy.pollIntervalMs": "30000", 
            "ranger.plugin.kms.service.name": "{{repo_name}}", 
            "ranger.plugin.kms.policy.source.impl": "org.apache.ranger.admin.client.RangerAdminRESTClient", 
            "ranger.plugin.kms.policy.rest.ssl.config.file": "/etc/ranger/kms/conf/ranger-policymgr-ssl.xml", 
            "ranger.plugin.kms.policy.cache.dir": "/etc/ranger/{{repo_name}}/policycache", 
            "ranger.plugin.kms.policy.pollIntervalMs": "30000",
            "ranger.plugin.kms.service.name": "{{repo_name}}",
            "ranger.plugin.kms.policy.source.impl": "org.apache.ranger.admin.client.RangerAdminRESTClient",
            "ranger.plugin.kms.policy.rest.ssl.config.file": "/etc/ranger/kms/conf/ranger-policymgr-ssl.xml",
            "ranger.plugin.kms.policy.cache.dir": "/etc/ranger/{{repo_name}}/policycache",
             "ranger.plugin.kms.policy.rest.url": "{{policymgr_mgr_url}}"
        }, 
        },
         "kerberos-env": {
            "kdc_hosts": "c6401.ambari.apache.org", 
            "manage_auth_to_local": "true", 
            "install_packages": "true", 
            "realm": "EXAMPLE.COM", 
            "encryption_types": "aes des3-cbc-sha1 rc4 des-cbc-md5", 
            "ad_create_attributes_template": "\n{\n  \"objectClass\": [\"top\", \"person\", \"organizationalPerson\", \"user\"],\n  \"cn\": \"$principal_name\",\n  #if( $is_service )\n  \"servicePrincipalName\": \"$principal_name\",\n  #end\n  \"userPrincipalName\": \"$normalized_principal\",\n  \"unicodePwd\": \"$password\",\n  \"accountExpires\": \"0\",\n  \"userAccountControl\": \"66048\"\n}", 
            "kdc_create_attributes": "", 
            "admin_server_host": "c6401.ambari.apache.org", 
            "group": "ambari-managed-principals", 
            "password_length": "20", 
            "ldap_url": "", 
            "manage_identities": "true", 
            "password_min_lowercase_letters": "1", 
            "create_ambari_principal": "true", 
            "service_check_principal_name": "${cluster_name|toLower()}-${short_date}", 
            "executable_search_paths": "/usr/bin, /usr/kerberos/bin, /usr/sbin, /usr/lib/mit/bin, /usr/lib/mit/sbin", 
            "password_chat_timeout": "5", 
            "kdc_type": "mit-kdc", 
            "set_password_expiry": "false", 
            "password_min_punctuation": "1", 
            "container_dn": "", 
            "case_insensitive_username_rules": "false", 
            "password_min_whitespace": "0", 
            "password_min_uppercase_letters": "1", 
            "kdc_hosts": "c6401.ambari.apache.org",
            "manage_auth_to_local": "true",
            "install_packages": "true",
            "realm": "EXAMPLE.COM",
            "encryption_types": "aes des3-cbc-sha1 rc4 des-cbc-md5",
            "ad_create_attributes_template": "\n{\n  \"objectClass\": [\"top\", \"person\", \"organizationalPerson\", \"user\"],\n  \"cn\": \"$principal_name\",\n  #if( $is_service )\n  \"servicePrincipalName\": \"$principal_name\",\n  #end\n  \"userPrincipalName\": \"$normalized_principal\",\n  \"unicodePwd\": \"$password\",\n  \"accountExpires\": \"0\",\n  \"userAccountControl\": \"66048\"\n}",
            "kdc_create_attributes": "",
            "admin_server_host": "c6401.ambari.apache.org",
            "group": "ambari-managed-principals",
            "password_length": "20",
            "ldap_url": "",
            "manage_identities": "true",
            "password_min_lowercase_letters": "1",
            "create_ambari_principal": "true",
            "service_check_principal_name": "${cluster_name|toLower()}-${short_date}",
            "executable_search_paths": "/usr/bin, /usr/kerberos/bin, /usr/sbin, /usr/lib/mit/bin, /usr/lib/mit/sbin",
            "password_chat_timeout": "5",
            "kdc_type": "mit-kdc",
            "set_password_expiry": "false",
            "password_min_punctuation": "1",
            "container_dn": "",
            "case_insensitive_username_rules": "false",
            "password_min_whitespace": "0",
            "password_min_uppercase_letters": "1",
             "password_min_digits": "1"
        }, 
        },
         "kms-properties": {
            "REPOSITORY_CONFIG_USERNAME": "keyadmin", 
            "db_user": "rangerkms01", 
            "DB_FLAVOR": "MYSQL", 
            "db_password": "rangerkms01", 
            "KMS_MASTER_KEY_PASSWD": "StrongPassword01", 
            "db_root_user": "root", 
            "db_name": "rangerkms01", 
            "db_host": "c6401.ambari.apache.org", 
            "db_root_password": "vagrant", 
            "SQL_CONNECTOR_JAR": "{{driver_curl_target}}", 
            "REPOSITORY_CONFIG_USERNAME": "keyadmin",
            "db_user": "rangerkms01",
            "DB_FLAVOR": "MYSQL",
            "db_password": "rangerkms01",
            "KMS_MASTER_KEY_PASSWD": "StrongPassword01",
            "db_root_user": "root",
            "db_name": "rangerkms01",
            "db_host": "c6401.ambari.apache.org",
            "db_root_password": "vagrant",
            "SQL_CONNECTOR_JAR": "{{driver_curl_target}}",
             "REPOSITORY_CONFIG_PASSWORD": "keyadmin"
        }, 
        },
         "admin-properties": {
            "db_user": "rangeradmin01", 
            "DB_FLAVOR": "MYSQL", 
            "db_password": "rangeradmin01", 
            "db_root_user": "root", 
            "policymgr_external_url": "http://c6401.ambari.apache.org:6080", 
            "db_name": "ranger01", 
            "db_host": "c6401.ambari.apache.org", 
            "db_root_password": "vagrant", 
            "db_user": "rangeradmin01",
            "DB_FLAVOR": "MYSQL",
            "db_password": "rangeradmin01",
            "db_root_user": "root",
            "policymgr_external_url": "http://c6401.ambari.apache.org:6080",
            "db_name": "ranger01",
            "db_host": "c6401.ambari.apache.org",
            "db_root_password": "vagrant",
             "SQL_CONNECTOR_JAR": "{{driver_curl_target}}"
        }, 
        },
         "ranger-kms-policymgr-ssl": {
            "xasecure.policymgr.clientssl.keystore": "/usr/hdp/current/ranger-kms/conf/ranger-plugin-keystore.jks", 
            "xasecure.policymgr.clientssl.truststore.password": "changeit", 
            "xasecure.policymgr.clientssl.keystore.credential.file": "jceks://file{{credential_file}}", 
            "xasecure.policymgr.clientssl.truststore": "/usr/hdp/current/ranger-kms/conf/ranger-plugin-truststore.jks", 
            "xasecure.policymgr.clientssl.truststore.credential.file": "jceks://file{{credential_file}}", 
            "xasecure.policymgr.clientssl.keystore": "/usr/hdp/current/ranger-kms/conf/ranger-plugin-keystore.jks",
            "xasecure.policymgr.clientssl.truststore.password": "changeit",
            "xasecure.policymgr.clientssl.keystore.credential.file": "jceks://file{{credential_file}}",
            "xasecure.policymgr.clientssl.truststore": "/usr/hdp/current/ranger-kms/conf/ranger-plugin-truststore.jks",
            "xasecure.policymgr.clientssl.truststore.credential.file": "jceks://file{{credential_file}}",
             "xasecure.policymgr.clientssl.keystore.password": "myKeyFilePassword"
        }, 
        },
         "hdfs-site": {
            "dfs.namenode.checkpoint.period": "21600", 
            "dfs.namenode.avoid.write.stale.datanode": "true", 
            "dfs.permissions.superusergroup": "hdfs", 
            "dfs.namenode.startup.delay.block.deletion.sec": "3600", 
            "dfs.namenode.kerberos.internal.spnego.principal": "HTTP/_HOST@EXAMPLE.COM", 
            "dfs.datanode.kerberos.principal": "dn/_HOST@EXAMPLE.COM", 
            "dfs.heartbeat.interval": "3", 
            "dfs.content-summary.limit": "5000", 
            "dfs.support.append": "true", 
            "dfs.datanode.address": "0.0.0.0:1019", 
            "dfs.cluster.administrators": " hdfs", 
            "dfs.namenode.audit.log.async": "true", 
            "dfs.datanode.balance.bandwidthPerSec": "6250000", 
            "dfs.namenode.safemode.threshold-pct": "1", 
            "dfs.namenode.checkpoint.edits.dir": "${dfs.namenode.checkpoint.dir}", 
            "dfs.namenode.rpc-address": "c6401.ambari.apache.org:8020", 
            "dfs.permissions.enabled": "true", 
            "dfs.namenode.kerberos.principal": "nn/_HOST@EXAMPLE.COM", 
            "dfs.client.read.shortcircuit": "true", 
            "dfs.https.port": "50470", 
            "dfs.namenode.https-address": "c6401.ambari.apache.org:50470", 
            "nfs.file.dump.dir": "/tmp/.hdfs-nfs", 
            "dfs.blocksize": "134217728", 
            "dfs.blockreport.initialDelay": "120", 
            "dfs.journalnode.edits.dir": "/hadoop/hdfs/journalnode", 
            "dfs.namenode.fslock.fair": "false", 
            "dfs.datanode.max.transfer.threads": "4096", 
            "dfs.secondary.namenode.kerberos.internal.spnego.principal": "HTTP/_HOST@EXAMPLE.COM", 
            "dfs.replication": "3", 
            "dfs.namenode.handler.count": "50", 
            "dfs.web.authentication.kerberos.keytab": "/etc/security/keytabs/spnego.service.keytab", 
            "fs.permissions.umask-mode": "022", 
            "dfs.namenode.stale.datanode.interval": "30000", 
            "dfs.datanode.ipc.address": "0.0.0.0:8010", 
            "dfs.datanode.failed.volumes.tolerated": "0", 
            "dfs.datanode.data.dir": "/grid/0/hadoop/hdfs/data", 
            "dfs.namenode.http-address": "c6401.ambari.apache.org:50070", 
            "dfs.webhdfs.enabled": "true", 
            "dfs.encrypt.data.transfer.cipher.suites": "AES/CTR/NoPadding", 
            "dfs.namenode.accesstime.precision": "0", 
            "dfs.namenode.write.stale.datanode.ratio": "1.0f", 
            "dfs.datanode.https.address": "0.0.0.0:50475", 
            "dfs.namenode.checkpoint.dir": "/grid/0/hadoop/hdfs/namesecondary", 
            "dfs.namenode.secondary.http-address": "c6401.ambari.apache.org:50090", 
            "nfs.exports.allowed.hosts": "* rw", 
            "dfs.namenode.checkpoint.txns": "1000000", 
            "dfs.datanode.http.address": "0.0.0.0:1022", 
            "dfs.datanode.du.reserved": "33011188224", 
            "dfs.client.read.shortcircuit.streams.cache.size": "4096", 
            "dfs.secondary.namenode.keytab.file": "/etc/security/keytabs/nn.service.keytab", 
            "dfs.web.authentication.kerberos.principal": "HTTP/_HOST@EXAMPLE.COM", 
            "dfs.http.policy": "HTTP_ONLY", 
            "dfs.block.access.token.enable": "true", 
            "dfs.client.retry.policy.enabled": "false", 
            "dfs.secondary.namenode.kerberos.principal": "nn/_HOST@EXAMPLE.COM", 
            "dfs.datanode.keytab.file": "/etc/security/keytabs/dn.service.keytab", 
            "dfs.namenode.name.dir.restore": "true", 
            "dfs.namenode.keytab.file": "/etc/security/keytabs/nn.service.keytab", 
            "dfs.journalnode.https-address": "0.0.0.0:8481", 
            "dfs.journalnode.http-address": "0.0.0.0:8480", 
            "dfs.domain.socket.path": "/var/lib/hadoop-hdfs/dn_socket", 
            "dfs.namenode.avoid.read.stale.datanode": "true", 
            "dfs.hosts.exclude": "/etc/hadoop/conf/dfs.exclude", 
            "dfs.datanode.data.dir.perm": "750", 
            "dfs.encryption.key.provider.uri": "kms://http@c6401.ambari.apache.org:9292/kms", 
            "dfs.replication.max": "50", 
            "dfs.namenode.checkpoint.period": "21600",
            "dfs.namenode.avoid.write.stale.datanode": "true",
            "dfs.permissions.superusergroup": "hdfs",
            "dfs.namenode.startup.delay.block.deletion.sec": "3600",
            "dfs.namenode.kerberos.internal.spnego.principal": "HTTP/_HOST@EXAMPLE.COM",
            "dfs.datanode.kerberos.principal": "dn/_HOST@EXAMPLE.COM",
            "dfs.heartbeat.interval": "3",
            "dfs.content-summary.limit": "5000",
            "dfs.support.append": "true",
            "dfs.datanode.address": "0.0.0.0:1019",
            "dfs.cluster.administrators": " hdfs",
            "dfs.namenode.audit.log.async": "true",
            "dfs.datanode.balance.bandwidthPerSec": "6250000",
            "dfs.namenode.safemode.threshold-pct": "1",
            "dfs.namenode.checkpoint.edits.dir": "${dfs.namenode.checkpoint.dir}",
            "dfs.namenode.rpc-address": "c6401.ambari.apache.org:8020",
            "dfs.permissions.enabled": "true",
            "dfs.namenode.kerberos.principal": "nn/_HOST@EXAMPLE.COM",
            "dfs.client.read.shortcircuit": "true",
            "dfs.https.port": "50470",
            "dfs.namenode.https-address": "c6401.ambari.apache.org:50470",
            "nfs.file.dump.dir": "/tmp/.hdfs-nfs",
            "dfs.blocksize": "134217728",
            "dfs.blockreport.initialDelay": "120",
            "dfs.journalnode.edits.dir": "/hadoop/hdfs/journalnode",
            "dfs.namenode.fslock.fair": "false",
            "dfs.datanode.max.transfer.threads": "4096",
            "dfs.secondary.namenode.kerberos.internal.spnego.principal": "HTTP/_HOST@EXAMPLE.COM",
            "dfs.replication": "3",
            "dfs.namenode.handler.count": "50",
            "dfs.web.authentication.kerberos.keytab": "/etc/security/keytabs/spnego.service.keytab",
            "fs.permissions.umask-mode": "022",
            "dfs.namenode.stale.datanode.interval": "30000",
            "dfs.datanode.ipc.address": "0.0.0.0:8010",
            "dfs.datanode.failed.volumes.tolerated": "0",
            "dfs.datanode.data.dir": "/grid/0/hadoop/hdfs/data",
            "dfs.namenode.http-address": "c6401.ambari.apache.org:50070",
            "dfs.webhdfs.enabled": "true",
            "dfs.encrypt.data.transfer.cipher.suites": "AES/CTR/NoPadding",
            "dfs.namenode.accesstime.precision": "0",
            "dfs.namenode.write.stale.datanode.ratio": "1.0f",
            "dfs.datanode.https.address": "0.0.0.0:50475",
            "dfs.namenode.checkpoint.dir": "/grid/0/hadoop/hdfs/namesecondary",
            "dfs.namenode.secondary.http-address": "c6401.ambari.apache.org:50090",
            "nfs.exports.allowed.hosts": "* rw",
            "dfs.namenode.checkpoint.txns": "1000000",
            "dfs.datanode.http.address": "0.0.0.0:1022",
            "dfs.datanode.du.reserved": "33011188224",
            "dfs.client.read.shortcircuit.streams.cache.size": "4096",
            "dfs.secondary.namenode.keytab.file": "/etc/security/keytabs/nn.service.keytab",
            "dfs.web.authentication.kerberos.principal": "HTTP/_HOST@EXAMPLE.COM",
            "dfs.http.policy": "HTTP_ONLY",
            "dfs.block.access.token.enable": "true",
            "dfs.client.retry.policy.enabled": "false",
            "dfs.secondary.namenode.kerberos.principal": "nn/_HOST@EXAMPLE.COM",
            "dfs.datanode.keytab.file": "/etc/security/keytabs/dn.service.keytab",
            "dfs.namenode.name.dir.restore": "true",
            "dfs.namenode.keytab.file": "/etc/security/keytabs/nn.service.keytab",
            "dfs.journalnode.https-address": "0.0.0.0:8481",
            "dfs.journalnode.http-address": "0.0.0.0:8480",
            "dfs.domain.socket.path": "/var/lib/hadoop-hdfs/dn_socket",
            "dfs.namenode.avoid.read.stale.datanode": "true",
            "dfs.hosts.exclude": "/etc/hadoop/conf/dfs.exclude",
            "dfs.datanode.data.dir.perm": "750",
            "dfs.encryption.key.provider.uri": "kms://http@c6401.ambari.apache.org:9292/kms",
            "dfs.replication.max": "50",
             "dfs.namenode.name.dir": "/grid/0/hadoop/hdfs/namenode"
        }, 
        },
         "ranger-tagsync-site": {
            "ranger.tagsync.dest.ranger.ssl.config.filename": "/usr/hdp/current/ranger-tagsync/conf/mytruststore.jks", 
            "ranger.tagsync.source.atlasrest.username": "", 
            "ranger.tagsync.logdir": "/var/log/ranger/tagsync", 
            "ranger.tagsync.source.atlasrest.download.interval.millis": "", 
            "ranger.tagsync.keystore.filename": "/usr/hdp/current/ranger-tagsync/conf/rangertagsync.jceks", 
            "ranger.tagsync.source.file.check.interval.millis": "", 
            "ranger.tagsync.source.atlasrest.endpoint": "", 
            "ranger.tagsync.dest.ranger.username": "rangertagsync", 
            "ranger.tagsync.dest.ranger.endpoint": "{{ranger_external_url}}", 
            "ranger.tagsync.kerberos.principal": "rangertagsync/_HOST@EXAMPLE.COM", 
            "ranger.tagsync.kerberos.keytab": "/etc/security/keytabs/rangertagsync.service.keytab", 
            "ranger.tagsync.source.atlas": "false", 
            "ranger.tagsync.source.atlasrest": "false", 
            "ranger.tagsync.source.file": "false", 
            "ranger.tagsync.dest.ranger.ssl.config.filename": "/usr/hdp/current/ranger-tagsync/conf/mytruststore.jks",
            "ranger.tagsync.source.atlasrest.username": "",
            "ranger.tagsync.logdir": "/var/log/ranger/tagsync",
            "ranger.tagsync.source.atlasrest.download.interval.millis": "",
            "ranger.tagsync.keystore.filename": "/usr/hdp/current/ranger-tagsync/conf/rangertagsync.jceks",
            "ranger.tagsync.source.file.check.interval.millis": "",
            "ranger.tagsync.source.atlasrest.endpoint": "",
            "ranger.tagsync.dest.ranger.username": "rangertagsync",
            "ranger.tagsync.dest.ranger.endpoint": "{{ranger_external_url}}",
            "ranger.tagsync.kerberos.principal": "rangertagsync/_HOST@EXAMPLE.COM",
            "ranger.tagsync.kerberos.keytab": "/etc/security/keytabs/rangertagsync.service.keytab",
            "ranger.tagsync.source.atlas": "false",
            "ranger.tagsync.source.atlasrest": "false",
            "ranger.tagsync.source.file": "false",
             "ranger.tagsync.source.file.filename": ""
        }, 
        },
         "tagsync-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#      http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n#\n\n\nlog4j.rootLogger = info,logFile\n\n# logFile\nlog4j.appender.logFile=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.logFile.file=${logdir}/tagsync.log\nlog4j.appender.logFile.datePattern='.'yyyy-MM-dd\nlog4j.appender.logFile.layout=org.apache.log4j.PatternLayout\nlog4j.appender.logFile.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %L %m%n\n\n# console\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.Target=System.out\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %L %m%n"
        }, 
        },
         "ranger-kms-audit": {
            "xasecure.audit.destination.solr.zookeepers": "NONE", 
            "xasecure.audit.destination.solr.urls": "", 
            "xasecure.audit.destination.solr.batch.filespool.dir": "/var/log/ranger/kms/audit/solr/spool", 
            "xasecure.audit.destination.hdfs.batch.filespool.dir": "/var/log/ranger/kms/audit/hdfs/spool", 
            "xasecure.audit.destination.hdfs": "true", 
            "xasecure.audit.destination.solr": "true", 
            "xasecure.audit.destination.solr.zookeepers": "NONE",
            "xasecure.audit.destination.solr.urls": "",
            "xasecure.audit.destination.solr.batch.filespool.dir": "/var/log/ranger/kms/audit/solr/spool",
            "xasecure.audit.destination.hdfs.batch.filespool.dir": "/var/log/ranger/kms/audit/hdfs/spool",
            "xasecure.audit.destination.hdfs": "true",
            "xasecure.audit.destination.solr": "true",
             "xasecure.audit.provider.summary.enabled": "false",
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit", 
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit",
             "xasecure.audit.is.enabled": "true"
        }, 
        },
         "hadoop-policy": {
            "security.job.client.protocol.acl": "*", 
            "security.job.task.protocol.acl": "*", 
            "security.datanode.protocol.acl": "*", 
            "security.namenode.protocol.acl": "*", 
            "security.client.datanode.protocol.acl": "*", 
            "security.inter.tracker.protocol.acl": "*", 
            "security.refresh.usertogroups.mappings.protocol.acl": "hadoop", 
            "security.client.protocol.acl": "*", 
            "security.refresh.policy.protocol.acl": "hadoop", 
            "security.admin.operations.protocol.acl": "hadoop", 
            "security.job.client.protocol.acl": "*",
            "security.job.task.protocol.acl": "*",
            "security.datanode.protocol.acl": "*",
            "security.namenode.protocol.acl": "*",
            "security.client.datanode.protocol.acl": "*",
            "security.inter.tracker.protocol.acl": "*",
            "security.refresh.usertogroups.mappings.protocol.acl": "hadoop",
            "security.client.protocol.acl": "*",
            "security.refresh.policy.protocol.acl": "hadoop",
            "security.admin.operations.protocol.acl": "hadoop",
             "security.inter.datanode.protocol.acl": "*"
        }, 
        },
         "hdfs-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#  http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing,\n# software distributed under the License is distributed on an\n# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n# KIND, either express or implied.  See the License for the\n# specific language governing permissions and limitations\n# under the License.\n#\n\n\n# Define some default values that can be overridden by system properties\n# To change daemon root logger use hadoop_root_logger in hadoop-env\nhadoop.root.logger=INFO,console\nhadoop.log.dir=.\nhadoop.log.file=hadoop.log\n\n\n# Define the root logger to the system property \"hadoop.root.logger\".\nlog4j.rootLogger=${hadoop.root.logger}, EventCounter\n\n# Logging Threshold\nlog4j.threshhold=ALL\n\n#\n# Daily Rolling File Appender\n#\n\nlog4j.appender.DRFA=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.DRFA.File=${hadoop.log.dir}/${hadoop.log.file}\n\n# Rollver at midnight\nlog4j.appender.DRFA.DatePattern=.yyyy-MM-dd\n\n# 30-day backup\n#log4j.appender.DRFA.MaxBackupIndex=30\nlog4j.appender.DRFA.layout=org.apache.log4j.PatternLayout\n\n# Pattern format: Date LogLevel LoggerName LogMessage\nlog4j.appender.DRFA.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\n# Debugging Pattern format\n#log4j.appender.DRFA.layout.ConversionPattern=%d{ISO8601} %-5p %c{2} (%F:%M(%L)) - %m%n\n\n\n#\n# console\n# Add \"console\" to rootlogger above if you want to use this\n#\n\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.target=System.err\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{yy/MM/dd HH:mm:ss} %p %c{2}: %m%n\n\n#\n# TaskLog Appender\n#\n\n#Default values\nhadoop.tasklog.taskid=null\nhadoop.tasklog.iscleanup=false\nhadoop.tasklog.noKeepSplits=4\nhadoop.tasklog.totalLogFileSize=100\nhadoop.tasklog.purgeLogSplits=true\nhadoop.tasklog.logsRetainHours=12\n\nlog4j.appender.TLA=org.apache.hadoop.mapred.TaskLogAppender\nlog4j.appender.TLA.taskId=${hadoop.tasklog.taskid}\nlog4j.appender.TLA.isCleanup=${hadoop.tasklog.iscleanup}\nlog4j.appender.TLA.totalLogFileSize=${hadoop.tasklog.totalLogFileSize}\n\nlog4j.appender.TLA.layout=org.apache.log4j.PatternLayout\nlog4j.appender.TLA.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\n\n#\n#Security audit appender\n#\nhadoop.security.logger=INFO,console\nhadoop.security.log.maxfilesize=256MB\nhadoop.security.log.maxbackupindex=20\nlog4j.category.SecurityLogger=${hadoop.security.logger}\nhadoop.security.log.file=SecurityAuth.audit\nlog4j.appender.DRFAS=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.DRFAS.File=${hadoop.log.dir}/${hadoop.security.log.file}\nlog4j.appender.DRFAS.layout=org.apache.log4j.PatternLayout\nlog4j.appender.DRFAS.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\nlog4j.appender.DRFAS.DatePattern=.yyyy-MM-dd\n\nlog4j.appender.RFAS=org.apache.log4j.RollingFileAppender\nlog4j.appender.RFAS.File=${hadoop.log.dir}/${hadoop.security.log.file}\nlog4j.appender.RFAS.layout=org.apache.log4j.PatternLayout\nlog4j.appender.RFAS.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\nlog4j.appender.RFAS.MaxFileSize=${hadoop.security.log.maxfilesize}\nlog4j.appender.RFAS.MaxBackupIndex=${hadoop.security.log.maxbackupindex}\n\n#\n# hdfs audit logging\n#\nhdfs.audit.logger=INFO,console\nlog4j.logger.org.apache.hadoop.hdfs.server.namenode.FSNamesystem.audit=${hdfs.audit.logger}\nlog4j.additivity.org.apache.hadoop.hdfs.server.namenode.FSNamesystem.audit=false\nlog4j.appender.DRFAAUDIT=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.DRFAAUDIT.File=${hadoop.log.dir}/hdfs-audit.log\nlog4j.appender.DRFAAUDIT.layout=org.apache.log4j.PatternLayout\nlog4j.appender.DRFAAUDIT.layout.ConversionPattern=%d{ISO8601} %p %c{2}: %m%n\nlog4j.appender.DRFAAUDIT.DatePattern=.yyyy-MM-dd\n\n#\n# NameNode metrics logging.\n# The default is to retain two namenode-metrics.log files up to 64MB each.\n#\nnamenode.metrics.logger=INFO,NullAppender\nlog4j.logger.NameNodeMetricsLog=${namenode.metrics.logger}\nlog4j.additivity.NameNodeMetricsLog=false\nlog4j.appender.NNMETRICSRFA=org.apache.log4j.RollingFileAppender\nlog4j.appender.NNMETRICSRFA.File=${hadoop.log.dir}/namenode-metrics.log\nlog4j.appender.NNMETRICSRFA.layout=org.apache.log4j.PatternLayout\nlog4j.appender.NNMETRICSRFA.layout.ConversionPattern=%d{ISO8601} %m%n\nlog4j.appender.NNMETRICSRFA.MaxBackupIndex=1\nlog4j.appender.NNMETRICSRFA.MaxFileSize=64MB\n\n#\n# mapred audit logging\n#\nmapred.audit.logger=INFO,console\nlog4j.logger.org.apache.hadoop.mapred.AuditLogger=${mapred.audit.logger}\nlog4j.additivity.org.apache.hadoop.mapred.AuditLogger=false\nlog4j.appender.MRAUDIT=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.MRAUDIT.File=${hadoop.log.dir}/mapred-audit.log\nlog4j.appender.MRAUDIT.layout=org.apache.log4j.PatternLayout\nlog4j.appender.MRAUDIT.layout.ConversionPattern=%d{ISO8601} %p %c{2}: %m%n\nlog4j.appender.MRAUDIT.DatePattern=.yyyy-MM-dd\n\n#\n# Rolling File Appender\n#\n\nlog4j.appender.RFA=org.apache.log4j.RollingFileAppender\nlog4j.appender.RFA.File=${hadoop.log.dir}/${hadoop.log.file}\n\n# Logfile size and and 30-day backups\nlog4j.appender.RFA.MaxFileSize=256MB\nlog4j.appender.RFA.MaxBackupIndex=10\n\nlog4j.appender.RFA.layout=org.apache.log4j.PatternLayout\nlog4j.appender.RFA.layout.ConversionPattern=%d{ISO8601} %-5p %c{2} - %m%n\nlog4j.appender.RFA.layout.ConversionPattern=%d{ISO8601} %-5p %c{2} (%F:%M(%L)) - %m%n\n\n\n# Custom Logging levels\n\nhadoop.metrics.log.level=INFO\n#log4j.logger.org.apache.hadoop.mapred.JobTracker=DEBUG\n#log4j.logger.org.apache.hadoop.mapred.TaskTracker=DEBUG\n#log4j.logger.org.apache.hadoop.fs.FSNamesystem=DEBUG\nlog4j.logger.org.apache.hadoop.metrics2=${hadoop.metrics.log.level}\n\n# Jets3t library\nlog4j.logger.org.jets3t.service.impl.rest.httpclient.RestS3Service=ERROR\n\n#\n# Null Appender\n# Trap security logger on the hadoop client side\n#\nlog4j.appender.NullAppender=org.apache.log4j.varia.NullAppender\n\n#\n# Event Counter Appender\n# Sends counts of logging messages at different severity levels to Hadoop Metrics.\n#\nlog4j.appender.EventCounter=org.apache.hadoop.log.metrics.EventCounter\n\n# Removes \"deprecated\" messages\nlog4j.logger.org.apache.hadoop.conf.Configuration.deprecation=WARN\n\n#\n# HDFS block state change log from block manager\n#\n# Uncomment the following to suppress normal block state change\n# messages from BlockManager in NameNode.\n#log4j.logger.BlockStateChange=WARN"
        }, 
        },
         "usersync-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#      http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n#\n\nlog4j.rootLogger = info,logFile\n\n# logFile\nlog4j.appender.logFile=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.logFile.file=${logdir}/usersync.log\nlog4j.appender.logFile.datePattern='.'yyyy-MM-dd\nlog4j.appender.logFile.layout=org.apache.log4j.PatternLayout\nlog4j.appender.logFile.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %m%n\n\n# console\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.Target=System.out\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %m%n"
        }, 
        },
         "krb5-conf": {
            "domains": "", 
            "manage_krb5_conf": "true", 
            "content": "\n[libdefaults]\n  renew_lifetime = 7d\n  forwardable = true\n  default_realm = {{realm}}\n  ticket_lifetime = 24h\n  dns_lookup_realm = false\n  dns_lookup_kdc = false\n  default_ccache_name = /tmp/krb5cc_%{uid}\n  #default_tgs_enctypes = {{encryption_types}}\n  #default_tkt_enctypes = {{encryption_types}}\n{% if domains %}\n[domain_realm]\n{%- for domain in domains.split(',') %}\n  {{domain|trim()}} = {{realm}}\n{%- endfor %}\n{% endif %}\n[logging]\n  default = FILE:/var/log/krb5kdc.log\n  admin_server = FILE:/var/log/kadmind.log\n  kdc = FILE:/var/log/krb5kdc.log\n\n[realms]\n  {{realm}} = {\n{%- if kdc_hosts > 0 -%}\n{%- set kdc_host_list = kdc_hosts.split(',')  -%}\n{%- if kdc_host_list and kdc_host_list|length > 0 %}\n    admin_server = {{admin_server_host|default(kdc_host_list[0]|trim(), True)}}\n{%- if kdc_host_list -%}\n{% for kdc_host in kdc_host_list %}\n    kdc = {{kdc_host|trim()}}\n{%- endfor -%}\n{% endif %}\n{%- endif %}\n{%- endif %}\n  }\n\n{# Append additional realm declarations below #}", 
            "domains": "",
            "manage_krb5_conf": "true",
            "content": "\n[libdefaults]\n  renew_lifetime = 7d\n  forwardable = true\n  default_realm = {{realm}}\n  ticket_lifetime = 24h\n  dns_lookup_realm = false\n  dns_lookup_kdc = false\n  default_ccache_name = /tmp/krb5cc_%{uid}\n  #default_tgs_enctypes = {{encryption_types}}\n  #default_tkt_enctypes = {{encryption_types}}\n{% if domains %}\n[domain_realm]\n{%- for domain in domains.split(',') %}\n  {{domain|trim()}} = {{realm}}\n{%- endfor %}\n{% endif %}\n[logging]\n  default = FILE:/var/log/krb5kdc.log\n  admin_server = FILE:/var/log/kadmind.log\n  kdc = FILE:/var/log/krb5kdc.log\n\n[realms]\n  {{realm}} = {\n{%- if kdc_hosts > 0 -%}\n{%- set kdc_host_list = kdc_hosts.split(',')  -%}\n{%- if kdc_host_list and kdc_host_list|length > 0 %}\n    admin_server = {{admin_server_host|default(kdc_host_list[0]|trim(), True)}}\n{%- if kdc_host_list -%}\n{% for kdc_host in kdc_host_list %}\n    kdc = {{kdc_host|trim()}}\n{%- endfor -%}\n{% endif %}\n{%- endif %}\n{%- endif %}\n  }\n\n{# Append additional realm declarations below #}",
             "conf_dir": "/etc"
        }, 
        },
         "kms-site": {
            "hadoop.kms.proxyuser.ranger.hosts": "*", 
            "hadoop.kms.authentication.type": "kerberos", 
            "hadoop.kms.proxyuser.ranger.groups": "*", 
            "hadoop.kms.authentication.signer.secret.provider.zookeeper.path": "/hadoop-kms/hadoop-auth-signature-secret", 
            "hadoop.kms.security.authorization.manager": "org.apache.ranger.authorization.kms.authorizer.RangerKmsAuthorizer", 
            "hadoop.kms.authentication.kerberos.name.rules": "RULE:[1:$1@$0](ambari-qa-test_cluster01@EXAMPLE.COM)s/.*/ambari-qa/\nRULE:[1:$1@$0](hdfs-test_cluster01@EXAMPLE.COM)s/.*/hdfs/\nRULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\nRULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\nRULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\nRULE:[2:$1@$0](rangeradmin@EXAMPLE.COM)s/.*/ranger/\nRULE:[2:$1@$0](rangerkms@EXAMPLE.COM)s/.*/keyadmin/\nRULE:[2:$1@$0](rangertagsync@EXAMPLE.COM)s/.*/rangertagsync/\nRULE:[2:$1@$0](rangerusersync@EXAMPLE.COM)s/.*/rangerusersync/\nDEFAULT", 
            "hadoop.kms.authentication.signer.secret.provider.zookeeper.kerberos.keytab": "/etc/security/keytabs/spnego.service.keytab", 
            "hadoop.kms.current.key.cache.timeout.ms": "30000", 
            "hadoop.kms.authentication.kerberos.keytab": "/etc/security/keytabs/spnego.service.keytab", 
            "hadoop.kms.audit.aggregation.window.ms": "10000", 
            "hadoop.kms.proxyuser.ranger.users": "*", 
            "hadoop.kms.authentication.signer.secret.provider.zookeeper.auth.type": "kerberos", 
            "hadoop.kms.key.provider.uri": "dbks://http@localhost:9292/kms", 
            "hadoop.security.keystore.JavaKeyStoreProvider.password": "none", 
            "hadoop.kms.authentication.signer.secret.provider.zookeeper.kerberos.principal": "HTTP/_HOST@EXAMPLE.COM", 
            "hadoop.kms.authentication.signer.secret.provider": "random", 
            "hadoop.kms.authentication.signer.secret.provider.zookeeper.connection.string": "#HOSTNAME#:#PORT#,...", 
            "hadoop.kms.cache.enable": "true", 
            "hadoop.kms.cache.timeout.ms": "600000", 
            "hadoop.kms.proxyuser.ranger.hosts": "*",
            "hadoop.kms.authentication.type": "kerberos",
            "hadoop.kms.proxyuser.ranger.groups": "*",
            "hadoop.kms.authentication.signer.secret.provider.zookeeper.path": "/hadoop-kms/hadoop-auth-signature-secret",
            "hadoop.kms.security.authorization.manager": "org.apache.ranger.authorization.kms.authorizer.RangerKmsAuthorizer",
            "hadoop.kms.authentication.kerberos.name.rules": "RULE:[1:$1@$0](ambari-qa-test_cluster01@EXAMPLE.COM)s/.*/ambari-qa/\nRULE:[1:$1@$0](hdfs-test_cluster01@EXAMPLE.COM)s/.*/hdfs/\nRULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\nRULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\nRULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\nRULE:[2:$1@$0](rangeradmin@EXAMPLE.COM)s/.*/ranger/\nRULE:[2:$1@$0](rangerkms@EXAMPLE.COM)s/.*/keyadmin/\nRULE:[2:$1@$0](rangertagsync@EXAMPLE.COM)s/.*/rangertagsync/\nRULE:[2:$1@$0](rangerusersync@EXAMPLE.COM)s/.*/rangerusersync/\nDEFAULT",
            "hadoop.kms.authentication.signer.secret.provider.zookeeper.kerberos.keytab": "/etc/security/keytabs/spnego.service.keytab",
            "hadoop.kms.current.key.cache.timeout.ms": "30000",
            "hadoop.kms.authentication.kerberos.keytab": "/etc/security/keytabs/spnego.service.keytab",
            "hadoop.kms.audit.aggregation.window.ms": "10000",
            "hadoop.kms.proxyuser.ranger.users": "*",
            "hadoop.kms.authentication.signer.secret.provider.zookeeper.auth.type": "kerberos",
            "hadoop.kms.key.provider.uri": "dbks://http@localhost:9292/kms",
            "hadoop.security.keystore.JavaKeyStoreProvider.password": "none",
            "hadoop.kms.authentication.signer.secret.provider.zookeeper.kerberos.principal": "HTTP/_HOST@EXAMPLE.COM",
            "hadoop.kms.authentication.signer.secret.provider": "random",
            "hadoop.kms.authentication.signer.secret.provider.zookeeper.connection.string": "#HOSTNAME#:#PORT#,...",
            "hadoop.kms.cache.enable": "true",
            "hadoop.kms.cache.timeout.ms": "600000",
             "hadoop.kms.authentication.kerberos.principal": "*"
        }, 
        },
         "core-site": {
            "net.topology.script.file.name": "/etc/hadoop/conf/topology_script.py", 
            "hadoop.proxyuser.hdfs.groups": "*", 
            "fs.trash.interval": "360", 
            "ipc.server.tcpnodelay": "true", 
            "io.compression.codecs": "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec,org.apache.hadoop.io.compress.SnappyCodec", 
            "ipc.client.idlethreshold": "8000", 
            "io.file.buffer.size": "131072", 
            "hadoop.proxyuser.ambari-server-test_cluster01.groups": "*", 
            "io.serializations": "org.apache.hadoop.io.serializer.WritableSerialization", 
            "hadoop.security.authentication": "kerberos", 
            "mapreduce.jobtracker.webinterface.trusted": "false", 
            "hadoop.proxyuser.kms.groups": "*", 
            "hadoop.proxyuser.hdfs.hosts": "*", 
            "hadoop.proxyuser.HTTP.groups": "users", 
            "fs.defaultFS": "hdfs://c6401.ambari.apache.org:8020", 
            "ha.failover-controller.active-standby-elector.zk.op.retries": "120", 
            "hadoop.security.key.provider.path": "kms://http@c6401.ambari.apache.org:9292/kms", 
            "hadoop.security.authorization": "true", 
            "hadoop.http.authentication.simple.anonymous.allowed": "true", 
            "ipc.client.connect.max.retries": "50", 
            "hadoop.security.auth_to_local": "RULE:[1:$1@$0](ambari-qa-test_cluster01@EXAMPLE.COM)s/.*/ambari-qa/\nRULE:[1:$1@$0](hdfs-test_cluster01@EXAMPLE.COM)s/.*/hdfs/\nRULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\nRULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\nRULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\nRULE:[2:$1@$0](rangeradmin@EXAMPLE.COM)s/.*/ranger/\nRULE:[2:$1@$0](rangerkms@EXAMPLE.COM)s/.*/keyadmin/\nRULE:[2:$1@$0](rangertagsync@EXAMPLE.COM)s/.*/rangertagsync/\nRULE:[2:$1@$0](rangerusersync@EXAMPLE.COM)s/.*/rangerusersync/\nDEFAULT", 
            "hadoop.proxyuser.ambari-server-test_cluster01.hosts": "c6401.ambari.apache.org", 
            "net.topology.script.file.name": "/etc/hadoop/conf/topology_script.py",
            "hadoop.proxyuser.hdfs.groups": "*",
            "fs.trash.interval": "360",
            "ipc.server.tcpnodelay": "true",
            "io.compression.codecs": "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec,org.apache.hadoop.io.compress.SnappyCodec",
            "ipc.client.idlethreshold": "8000",
            "io.file.buffer.size": "131072",
            "hadoop.proxyuser.ambari-server-test_cluster01.groups": "*",
            "io.serializations": "org.apache.hadoop.io.serializer.WritableSerialization",
            "hadoop.security.authentication": "kerberos",
            "mapreduce.jobtracker.webinterface.trusted": "false",
            "hadoop.proxyuser.kms.groups": "*",
            "hadoop.proxyuser.hdfs.hosts": "*",
            "hadoop.proxyuser.HTTP.groups": "users",
            "fs.defaultFS": "hdfs://c6401.ambari.apache.org:8020",
            "ha.failover-controller.active-standby-elector.zk.op.retries": "120",
            "hadoop.security.key.provider.path": "kms://http@c6401.ambari.apache.org:9292/kms",
            "hadoop.security.authorization": "true",
            "hadoop.http.authentication.simple.anonymous.allowed": "true",
            "ipc.client.connect.max.retries": "50",
            "hadoop.security.auth_to_local": "RULE:[1:$1@$0](ambari-qa-test_cluster01@EXAMPLE.COM)s/.*/ambari-qa/\nRULE:[1:$1@$0](hdfs-test_cluster01@EXAMPLE.COM)s/.*/hdfs/\nRULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\nRULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\nRULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\nRULE:[2:$1@$0](rangeradmin@EXAMPLE.COM)s/.*/ranger/\nRULE:[2:$1@$0](rangerkms@EXAMPLE.COM)s/.*/keyadmin/\nRULE:[2:$1@$0](rangertagsync@EXAMPLE.COM)s/.*/rangertagsync/\nRULE:[2:$1@$0](rangerusersync@EXAMPLE.COM)s/.*/rangerusersync/\nDEFAULT",
            "hadoop.proxyuser.ambari-server-test_cluster01.hosts": "c6401.ambari.apache.org",
             "ipc.client.connection.maxidletime": "30000"
        }, 
        },
         "hadoop-env": {
            "keyserver_port": "", 
            "proxyuser_group": "users", 
            "hdfs_user_nproc_limit": "65536", 
            "hdfs_log_dir_prefix": "/var/log/hadoop", 
            "hdfs_user_nofile_limit": "128000", 
            "hdfs_user": "hdfs", 
            "keyserver_port": "",
            "proxyuser_group": "users",
            "hdfs_user_nproc_limit": "65536",
            "hdfs_log_dir_prefix": "/var/log/hadoop",
            "hdfs_user_nofile_limit": "128000",
            "hdfs_user": "hdfs",
             "hdfs_principal_name": "hdfs-cl1@EXAMPLE.COM",
            "keyserver_host": " ", 
            "namenode_opt_maxnewsize": "128m", 
            "hdfs_user_keytab": "/etc/security/keytabs/hdfs.headless.keytab", 
            "namenode_opt_maxpermsize": "256m", 
            "content": "\n# Set Hadoop-specific environment variables here.\n\n# The only required environment variable is JAVA_HOME.  All others are\n# optional.  When running a distributed configuration it is best to\n# set JAVA_HOME in this file, so that it is correctly defined on\n# remote nodes.\n\n# The java implementation to use.  Required.\nexport JAVA_HOME={{java_home}}\nexport HADOOP_HOME_WARN_SUPPRESS=1\n\n# Hadoop home directory\nexport HADOOP_HOME=${HADOOP_HOME:-{{hadoop_home}}}\n\n# Hadoop Configuration Directory\n\n{# this is different for HDP1 #}\n# Path to jsvc required by secure HDP 2.0 datanode\nexport JSVC_HOME={{jsvc_path}}\n\n\n# The maximum amount of heap to use, in MB. Default is 1000.\nexport HADOOP_HEAPSIZE=\"{{hadoop_heapsize}}\"\n\nexport HADOOP_NAMENODE_INIT_HEAPSIZE=\"-Xms{{namenode_heapsize}}\"\n\n# Extra Java runtime options.  Empty by default.\nexport HADOOP_OPTS=\"-Djava.net.preferIPv4Stack=true ${HADOOP_OPTS}\"\n\n# Command specific options appended to HADOOP_OPTS when specified\nHADOOP_JOBTRACKER_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{jtnode_opt_newsize}} -XX:MaxNewSize={{jtnode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xmx{{jtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dmapred.audit.logger=INFO,MRAUDIT -Dhadoop.mapreduce.jobsummary.logger=INFO,JSA ${HADOOP_JOBTRACKER_OPTS}\"\n\nHADOOP_TASKTRACKER_OPTS=\"-server -Xmx{{ttnode_heapsize}} -Dhadoop.security.logger=ERROR,console -Dmapred.audit.logger=ERROR,console ${HADOOP_TASKTRACKER_OPTS}\"\n\n{% if java_version < 8 %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -XX:PermSize={{namenode_opt_permsize}} -XX:MaxPermSize={{namenode_opt_maxpermsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -XX:PermSize=128m -XX:MaxPermSize=256m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m -XX:MaxPermSize=512m $HADOOP_CLIENT_OPTS\"\n\n{% else %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m $HADOOP_CLIENT_OPTS\"\n{% endif %}\n\nHADOOP_NFS3_OPTS=\"-Xmx{{nfsgateway_heapsize}}m -Dhadoop.security.logger=ERROR,DRFAS ${HADOOP_NFS3_OPTS}\"\nHADOOP_BALANCER_OPTS=\"-server -Xmx{{hadoop_heapsize}}m ${HADOOP_BALANCER_OPTS}\"\n\n\n# On secure datanodes, user to run the datanode as after dropping privileges\nexport HADOOP_SECURE_DN_USER=${HADOOP_SECURE_DN_USER:-{{hadoop_secure_dn_user}}}\n\n# Extra ssh options.  Empty by default.\nexport HADOOP_SSH_OPTS=\"-o ConnectTimeout=5 -o SendEnv=HADOOP_CONF_DIR\"\n\n# Where log files are stored.  $HADOOP_HOME/logs by default.\nexport HADOOP_LOG_DIR={{hdfs_log_dir_prefix}}/$USER\n\n# History server logs\nexport HADOOP_MAPRED_LOG_DIR={{mapred_log_dir_prefix}}/$USER\n\n# Where log files are stored in the secure data environment.\nexport HADOOP_SECURE_DN_LOG_DIR={{hdfs_log_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# File naming remote slave hosts.  $HADOOP_HOME/conf/slaves by default.\n# export HADOOP_SLAVES=${HADOOP_HOME}/conf/slaves\n\n# host:path where hadoop code should be rsync'd from.  Unset by default.\n# export HADOOP_MASTER=master:/home/$USER/src/hadoop\n\n# Seconds to sleep between slave commands.  Unset by default.  This\n# can be useful in large clusters, where, e.g., slave rsyncs can\n# otherwise arrive faster than the master can service them.\n# export HADOOP_SLAVE_SLEEP=0.1\n\n# The directory where pid files are stored. /tmp by default.\nexport HADOOP_PID_DIR={{hadoop_pid_dir_prefix}}/$USER\nexport HADOOP_SECURE_DN_PID_DIR={{hadoop_pid_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# History server pid\nexport HADOOP_MAPRED_PID_DIR={{mapred_pid_dir_prefix}}/$USER\n\nYARN_RESOURCEMANAGER_OPTS=\"-Dyarn.server.resourcemanager.appsummary.logger=INFO,RMSUMMARY\"\n\n# A string representing this instance of hadoop. $USER by default.\nexport HADOOP_IDENT_STRING=$USER\n\n# The scheduling priority for daemon processes.  See 'man nice'.\n\n# export HADOOP_NICENESS=10\n\n# Add database libraries\nJAVA_JDBC_LIBS=\"\"\nif [ -d \"/usr/share/java\" ]; then\n  for jarFile in `ls /usr/share/java | grep -E \"(mysql|ojdbc|postgresql|sqljdbc)\" 2>/dev/null`\n  do\n    JAVA_JDBC_LIBS=${JAVA_JDBC_LIBS}:$jarFile\n  done\nfi\n\n# Add libraries to the hadoop classpath - some may not need a colon as they already include it\nexport HADOOP_CLASSPATH=${HADOOP_CLASSPATH}${JAVA_JDBC_LIBS}\n\n# Setting path to hdfs command line\nexport HADOOP_LIBEXEC_DIR={{hadoop_libexec_dir}}\n\n# Mostly required for hadoop 2.0\nexport JAVA_LIBRARY_PATH=${JAVA_LIBRARY_PATH}\n\nexport HADOOP_OPTS=\"-Dhdp.version=$HDP_VERSION $HADOOP_OPTS\"\n\n{% if is_datanode_max_locked_memory_set %}\n# Fix temporary bug, when ulimit from conf files is not picked up, without full relogin. \n# Makes sense to fix only when runing DN as root \nif [ \"$command\" == \"datanode\" ] && [ \"$EUID\" -eq 0 ] && [ -n \"$HADOOP_SECURE_DN_USER\" ]; then\n  ulimit -l {{datanode_max_locked_memory}}\nfi\n{% endif %}", 
            "namenode_heapsize": "1024m", 
            "namenode_opt_newsize": "128m", 
            "nfsgateway_heapsize": "1024", 
            "dtnode_heapsize": "1024m", 
            "hadoop_root_logger": "INFO,RFA", 
            "hadoop_heapsize": "1024", 
            "hadoop_pid_dir_prefix": "/var/run/hadoop", 
            "namenode_opt_permsize": "128m", 
            "keyserver_host": " ",
            "namenode_opt_maxnewsize": "128m",
            "hdfs_user_keytab": "/etc/security/keytabs/hdfs.headless.keytab",
            "namenode_opt_maxpermsize": "256m",
            "content": "\n# Set Hadoop-specific environment variables here.\n\n# The only required environment variable is JAVA_HOME.  All others are\n# optional.  When running a distributed configuration it is best to\n# set JAVA_HOME in this file, so that it is correctly defined on\n# remote nodes.\n\n# The java implementation to use.  Required.\nexport JAVA_HOME={{java_home}}\nexport HADOOP_HOME_WARN_SUPPRESS=1\n\n# Hadoop home directory\nexport HADOOP_HOME=${HADOOP_HOME:-{{hadoop_home}}}\n\n# Hadoop Configuration Directory\n\n{# this is different for HDP1 #}\n# Path to jsvc required by secure HDP 2.0 datanode\nexport JSVC_HOME={{jsvc_path}}\n\n\n# The maximum amount of heap to use, in MB. Default is 1000.\nexport HADOOP_HEAPSIZE=\"{{hadoop_heapsize}}\"\n\nexport HADOOP_NAMENODE_INIT_HEAPSIZE=\"-Xms{{namenode_heapsize}}\"\n\n# Extra Java runtime options.  Empty by default.\nexport HADOOP_OPTS=\"-Djava.net.preferIPv4Stack=true ${HADOOP_OPTS}\"\n\n# Command specific options appended to HADOOP_OPTS when specified\nHADOOP_JOBTRACKER_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{jtnode_opt_newsize}} -XX:MaxNewSize={{jtnode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xmx{{jtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dmapred.audit.logger=INFO,MRAUDIT -Dhadoop.mapreduce.jobsummary.logger=INFO,JSA ${HADOOP_JOBTRACKER_OPTS}\"\n\nHADOOP_TASKTRACKER_OPTS=\"-server -Xmx{{ttnode_heapsize}} -Dhadoop.security.logger=ERROR,console -Dmapred.audit.logger=ERROR,console ${HADOOP_TASKTRACKER_OPTS}\"\n\n{% if java_version < 8 %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -XX:PermSize={{namenode_opt_permsize}} -XX:MaxPermSize={{namenode_opt_maxpermsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -XX:PermSize=128m -XX:MaxPermSize=256m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m -XX:MaxPermSize=512m $HADOOP_CLIENT_OPTS\"\n\n{% else %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m $HADOOP_CLIENT_OPTS\"\n{% endif %}\n\nHADOOP_NFS3_OPTS=\"-Xmx{{nfsgateway_heapsize}}m -Dhadoop.security.logger=ERROR,DRFAS ${HADOOP_NFS3_OPTS}\"\nHADOOP_BALANCER_OPTS=\"-server -Xmx{{hadoop_heapsize}}m ${HADOOP_BALANCER_OPTS}\"\n\n\n# On secure datanodes, user to run the datanode as after dropping privileges\nexport HADOOP_SECURE_DN_USER=${HADOOP_SECURE_DN_USER:-{{hadoop_secure_dn_user}}}\n\n# Extra ssh options.  Empty by default.\nexport HADOOP_SSH_OPTS=\"-o ConnectTimeout=5 -o SendEnv=HADOOP_CONF_DIR\"\n\n# Where log files are stored.  $HADOOP_HOME/logs by default.\nexport HADOOP_LOG_DIR={{hdfs_log_dir_prefix}}/$USER\n\n# History server logs\nexport HADOOP_MAPRED_LOG_DIR={{mapred_log_dir_prefix}}/$USER\n\n# Where log files are stored in the secure data environment.\nexport HADOOP_SECURE_DN_LOG_DIR={{hdfs_log_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# File naming remote slave hosts.  $HADOOP_HOME/conf/slaves by default.\n# export HADOOP_SLAVES=${HADOOP_HOME}/conf/slaves\n\n# host:path where hadoop code should be rsync'd from.  Unset by default.\n# export HADOOP_MASTER=master:/home/$USER/src/hadoop\n\n# Seconds to sleep between slave commands.  Unset by default.  This\n# can be useful in large clusters, where, e.g., slave rsyncs can\n# otherwise arrive faster than the master can service them.\n# export HADOOP_SLAVE_SLEEP=0.1\n\n# The directory where pid files are stored. /tmp by default.\nexport HADOOP_PID_DIR={{hadoop_pid_dir_prefix}}/$USER\nexport HADOOP_SECURE_DN_PID_DIR={{hadoop_pid_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# History server pid\nexport HADOOP_MAPRED_PID_DIR={{mapred_pid_dir_prefix}}/$USER\n\nYARN_RESOURCEMANAGER_OPTS=\"-Dyarn.server.resourcemanager.appsummary.logger=INFO,RMSUMMARY\"\n\n# A string representing this instance of hadoop. $USER by default.\nexport HADOOP_IDENT_STRING=$USER\n\n# The scheduling priority for daemon processes.  See 'man nice'.\n\n# export HADOOP_NICENESS=10\n\n# Add database libraries\nJAVA_JDBC_LIBS=\"\"\nif [ -d \"/usr/share/java\" ]; then\n  for jarFile in `ls /usr/share/java | grep -E \"(mysql|ojdbc|postgresql|sqljdbc)\" 2>/dev/null`\n  do\n    JAVA_JDBC_LIBS=${JAVA_JDBC_LIBS}:$jarFile\n  done\nfi\n\n# Add libraries to the hadoop classpath - some may not need a colon as they already include it\nexport HADOOP_CLASSPATH=${HADOOP_CLASSPATH}${JAVA_JDBC_LIBS}\n\n# Setting path to hdfs command line\nexport HADOOP_LIBEXEC_DIR={{hadoop_libexec_dir}}\n\n# Mostly required for hadoop 2.0\nexport JAVA_LIBRARY_PATH=${JAVA_LIBRARY_PATH}\n\nexport HADOOP_OPTS=\"-Dhdp.version=$HDP_VERSION $HADOOP_OPTS\"\n\n{% if is_datanode_max_locked_memory_set %}\n# Fix temporary bug, when ulimit from conf files is not picked up, without full relogin. \n# Makes sense to fix only when runing DN as root \nif [ \"$command\" == \"datanode\" ] && [ \"$EUID\" -eq 0 ] && [ -n \"$HADOOP_SECURE_DN_USER\" ]; then\n  ulimit -l {{datanode_max_locked_memory}}\nfi\n{% endif %}",
            "namenode_heapsize": "1024m",
            "namenode_opt_newsize": "128m",
            "nfsgateway_heapsize": "1024",
            "dtnode_heapsize": "1024m",
            "hadoop_root_logger": "INFO,RFA",
            "hadoop_heapsize": "1024",
            "hadoop_pid_dir_prefix": "/var/run/hadoop",
            "namenode_opt_permsize": "128m",
             "hdfs_tmp_dir": "/tmp"
        }, 
        },
         "zookeeper-log4j": {
             "content": "\n#\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#   http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing,\n# software distributed under the License is distributed on an\n# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n# KIND, either express or implied.  See the License for the\n# specific language governing permissions and limitations\n# under the License.\n#\n#\n#\n\n#\n# ZooKeeper Logging Configuration\n#\n\n# DEFAULT: console appender only\nlog4j.rootLogger=INFO, CONSOLE\n\n# Example with rolling log file\n#log4j.rootLogger=DEBUG, CONSOLE, ROLLINGFILE\n\n# Example with rolling log file and tracing\n#log4j.rootLogger=TRACE, CONSOLE, ROLLINGFILE, TRACEFILE\n\n#\n# Log INFO level and above messages to the console\n#\nlog4j.appender.CONSOLE=org.apache.log4j.ConsoleAppender\nlog4j.appender.CONSOLE.Threshold=INFO\nlog4j.appender.CONSOLE.layout=org.apache.log4j.PatternLayout\nlog4j.appender.CONSOLE.layout.ConversionPattern=%d{ISO8601} - %-5p [%t:%C{1}@%L] - %m%n\n\n#\n# Add ROLLINGFILE to rootLogger to get log file output\n#    Log DEBUG level and above messages to a log file\nlog4j.appender.ROLLINGFILE=org.apache.log4j.RollingFileAppender\nlog4j.appender.ROLLINGFILE.Threshold=DEBUG\nlog4j.appender.ROLLINGFILE.File=zookeeper.log\n\n# Max log file size of 10MB\nlog4j.appender.ROLLINGFILE.MaxFileSize=10MB\n# uncomment the next line to limit number of backup files\n#log4j.appender.ROLLINGFILE.MaxBackupIndex=10\n\nlog4j.appender.ROLLINGFILE.layout=org.apache.log4j.PatternLayout\nlog4j.appender.ROLLINGFILE.layout.ConversionPattern=%d{ISO8601} - %-5p [%t:%C{1}@%L] - %m%n\n\n\n#\n# Add TRACEFILE to rootLogger to get log file output\n#    Log DEBUG level and above messages to a log file\nlog4j.appender.TRACEFILE=org.apache.log4j.FileAppender\nlog4j.appender.TRACEFILE.Threshold=TRACE\nlog4j.appender.TRACEFILE.File=zookeeper_trace.log\n\nlog4j.appender.TRACEFILE.layout=org.apache.log4j.PatternLayout\n### Notice we are including log4j's NDC here (%x)\nlog4j.appender.TRACEFILE.layout.ConversionPattern=%d{ISO8601} - %-5p [%t:%C{1}@%L][%x] - %m%n"
        }, 
        },
         "ssl-server": {
            "ssl.server.keystore.location": "/etc/security/serverKeys/keystore.jks", 
            "ssl.server.keystore.keypassword": "bigdata", 
            "ssl.server.truststore.location": "/etc/security/serverKeys/all.jks", 
            "ssl.server.keystore.password": "bigdata", 
            "ssl.server.truststore.password": "bigdata", 
            "ssl.server.truststore.type": "jks", 
            "ssl.server.keystore.type": "jks", 
            "ssl.server.keystore.location": "/etc/security/serverKeys/keystore.jks",
            "ssl.server.keystore.keypassword": "bigdata",
            "ssl.server.truststore.location": "/etc/security/serverKeys/all.jks",
            "ssl.server.keystore.password": "bigdata",
            "ssl.server.truststore.password": "bigdata",
            "ssl.server.truststore.type": "jks",
            "ssl.server.keystore.type": "jks",
             "ssl.server.truststore.reload.interval": "10000"
        }, 
        "ranger-site": {}, 
        },
        "ranger-site": {},
         "zookeeper-env": {
            "zk_server_heapsize": "1024m", 
            "zookeeper_keytab_path": "/etc/security/keytabs/zk.service.keytab", 
            "zk_user": "zookeeper", 
            "zk_log_dir": "/var/log/zookeeper", 
            "content": "\nexport JAVA_HOME={{java64_home}}\nexport ZOOKEEPER_HOME={{zk_home}}\nexport ZOO_LOG_DIR={{zk_log_dir}}\nexport ZOOPIDFILE={{zk_pid_file}}\nexport SERVER_JVMFLAGS={{zk_server_heapsize}}\nexport JAVA=$JAVA_HOME/bin/java\nexport CLASSPATH=$CLASSPATH:/usr/share/zookeeper/*\n\n{% if security_enabled %}\nexport SERVER_JVMFLAGS=\"$SERVER_JVMFLAGS -Djava.security.auth.login.config={{zk_server_jaas_file}}\"\nexport CLIENT_JVMFLAGS=\"$CLIENT_JVMFLAGS -Djava.security.auth.login.config={{zk_client_jaas_file}}\"\n{% endif %}", 
            "zk_pid_dir": "/var/run/zookeeper", 
            "zk_server_heapsize": "1024m",
            "zookeeper_keytab_path": "/etc/security/keytabs/zk.service.keytab",
            "zk_user": "zookeeper",
            "zk_log_dir": "/var/log/zookeeper",
            "content": "\nexport JAVA_HOME={{java64_home}}\nexport ZOOKEEPER_HOME={{zk_home}}\nexport ZOO_LOG_DIR={{zk_log_dir}}\nexport ZOOPIDFILE={{zk_pid_file}}\nexport SERVER_JVMFLAGS={{zk_server_heapsize}}\nexport JAVA=$JAVA_HOME/bin/java\nexport CLASSPATH=$CLASSPATH:/usr/share/zookeeper/*\n\n{% if security_enabled %}\nexport SERVER_JVMFLAGS=\"$SERVER_JVMFLAGS -Djava.security.auth.login.config={{zk_server_jaas_file}}\"\nexport CLIENT_JVMFLAGS=\"$CLIENT_JVMFLAGS -Djava.security.auth.login.config={{zk_client_jaas_file}}\"\n{% endif %}",
            "zk_pid_dir": "/var/run/zookeeper",
             "zookeeper_principal_name": "zookeeper/_HOST@EXAMPLE.COM"
        }, 
        },
         "admin-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#      http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n#\n\n\nlog4j.rootLogger = warn,xa_log_appender\n\n\n# xa_logger\nlog4j.appender.xa_log_appender=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.xa_log_appender.file=${logdir}/xa_portal.log\nlog4j.appender.xa_log_appender.datePattern='.'yyyy-MM-dd\nlog4j.appender.xa_log_appender.append=true\nlog4j.appender.xa_log_appender.layout=org.apache.log4j.PatternLayout\nlog4j.appender.xa_log_appender.layout.ConversionPattern=%d [%t] %-5p %C{6} (%F:%L) - %m%n\n# xa_log_appender : category and additivity\nlog4j.category.org.springframework=warn,xa_log_appender\nlog4j.additivity.org.springframework=false\n\nlog4j.category.org.apache.ranger=info,xa_log_appender\nlog4j.additivity.org.apache.ranger=false\n\nlog4j.category.xa=info,xa_log_appender\nlog4j.additivity.xa=false\n\n# perf_logger\nlog4j.appender.perf_appender=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.perf_appender.file=${logdir}/ranger_admin_perf.log\nlog4j.appender.perf_appender.datePattern='.'yyyy-MM-dd\nlog4j.appender.perf_appender.append=true\nlog4j.appender.perf_appender.layout=org.apache.log4j.PatternLayout\nlog4j.appender.perf_appender.layout.ConversionPattern=%d [%t] %m%n\n\n\n# sql_appender\nlog4j.appender.sql_appender=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.sql_appender.file=${logdir}/xa_portal_sql.log\nlog4j.appender.sql_appender.datePattern='.'yyyy-MM-dd\nlog4j.appender.sql_appender.append=true\nlog4j.appender.sql_appender.layout=org.apache.log4j.PatternLayout\nlog4j.appender.sql_appender.layout.ConversionPattern=%d [%t] %-5p %C{6} (%F:%L) - %m%n\n\n# sql_appender : category and additivity\nlog4j.category.org.hibernate.SQL=warn,sql_appender\nlog4j.additivity.org.hibernate.SQL=false\n\nlog4j.category.jdbc.sqlonly=fatal,sql_appender\nlog4j.additivity.jdbc.sqlonly=false\n\nlog4j.category.jdbc.sqltiming=warn,sql_appender\nlog4j.additivity.jdbc.sqltiming=false\n\nlog4j.category.jdbc.audit=fatal,sql_appender\nlog4j.additivity.jdbc.audit=false\n\nlog4j.category.jdbc.resultset=fatal,sql_appender\nlog4j.additivity.jdbc.resultset=false\n\nlog4j.category.jdbc.connection=fatal,sql_appender\nlog4j.additivity.jdbc.connection=false"
        }, 
        },
         "zoo.cfg": {
            "clientPort": "2181", 
            "autopurge.purgeInterval": "24", 
            "syncLimit": "5", 
            "dataDir": "/grid/0/hadoop/zookeeper", 
            "initLimit": "10", 
            "tickTime": "2000", 
            "clientPort": "2181",
            "autopurge.purgeInterval": "24",
            "syncLimit": "5",
            "dataDir": "/grid/0/hadoop/zookeeper",
            "initLimit": "10",
            "tickTime": "2000",
             "autopurge.snapRetainCount": "30"
        }, 
        },
         "ranger-hdfs-security": {
            "ranger.plugin.hdfs.policy.source.impl": "org.apache.ranger.admin.client.RangerAdminRESTClient", 
            "ranger.plugin.hdfs.service.name": "{{repo_name}}", 
            "ranger.plugin.hdfs.policy.cache.dir": "/etc/ranger/{{repo_name}}/policycache", 
            "ranger.plugin.hdfs.policy.pollIntervalMs": "30000", 
            "ranger.plugin.hdfs.policy.rest.url": "{{policymgr_mgr_url}}", 
            "ranger.plugin.hdfs.policy.rest.ssl.config.file": "/etc/hadoop/conf/ranger-policymgr-ssl.xml", 
            "ranger.plugin.hdfs.policy.source.impl": "org.apache.ranger.admin.client.RangerAdminRESTClient",
            "ranger.plugin.hdfs.service.name": "{{repo_name}}",
            "ranger.plugin.hdfs.policy.cache.dir": "/etc/ranger/{{repo_name}}/policycache",
            "ranger.plugin.hdfs.policy.pollIntervalMs": "30000",
            "ranger.plugin.hdfs.policy.rest.url": "{{policymgr_mgr_url}}",
            "ranger.plugin.hdfs.policy.rest.ssl.config.file": "/etc/hadoop/conf/ranger-policymgr-ssl.xml",
             "xasecure.add-hadoop-authorization": "true"
        }, 
        "usersync-properties": {}, 
        },
        "usersync-properties": {},
         "kms-env": {
            "kms_group": "kms", 
            "kms_log_dir": "/var/log/ranger/kms", 
            "hsm_partition_password": "", 
            "kms_user": "kms", 
            "create_db_user": "true", 
            "kms_port": "9292", 
            "kms_group": "kms",
            "kms_log_dir": "/var/log/ranger/kms",
            "hsm_partition_password": "",
            "kms_user": "kms",
            "create_db_user": "true",
            "kms_port": "9292",
             "ranger_kms_pid_dir": "/var/run/ranger_kms"
        }, 
        },
         "dbks-site": {
            "ranger.ks.jpa.jdbc.credential.provider.path": "/etc/ranger/kms/rangerkms.jceks", 
            "ranger.ks.kerberos.keytab": "/etc/security/keytabs/rangerkms.service.keytab", 
            "ranger.ks.hsm.partition.password": "_", 
            "ranger.ks.jpa.jdbc.driver": "com.mysql.jdbc.Driver", 
            "ranger.ks.jpa.jdbc.credential.alias": "ranger.ks.jdbc.password", 
            "ranger.ks.kerberos.principal": "rangerkms/_HOST@EXAMPLE.COM", 
            "ranger.db.encrypt.key.password": "_", 
            "ranger.ks.hsm.enabled": "false", 
            "ranger.ks.jpa.jdbc.password": "_", 
            "ranger.ks.masterkey.credential.alias": "ranger.ks.masterkey.password", 
            "ranger.ks.jpa.jdbc.url": "jdbc:mysql://c6401.ambari.apache.org:3306/rangerkms01", 
            "hadoop.kms.blacklist.DECRYPT_EEK": "hdfs", 
            "ranger.ks.jdbc.sqlconnectorjar": "{{ews_lib_jar_path}}", 
            "ranger.ks.jpa.jdbc.user": "{{db_user}}", 
            "ranger.ks.hsm.partition.password.alias": "ranger.kms.hsm.partition.password", 
            "ranger.ks.hsm.type": "LunaProvider", 
            "ranger.ks.hsm.partition.name": "par19", 
            "ranger.ks.jpa.jdbc.credential.provider.path": "/etc/ranger/kms/rangerkms.jceks",
            "ranger.ks.kerberos.keytab": "/etc/security/keytabs/rangerkms.service.keytab",
            "ranger.ks.hsm.partition.password": "_",
            "ranger.ks.jpa.jdbc.driver": "com.mysql.jdbc.Driver",
            "ranger.ks.jpa.jdbc.credential.alias": "ranger.ks.jdbc.password",
            "ranger.ks.kerberos.principal": "rangerkms/_HOST@EXAMPLE.COM",
            "ranger.db.encrypt.key.password": "_",
            "ranger.ks.hsm.enabled": "false",
            "ranger.ks.jpa.jdbc.password": "_",
            "ranger.ks.masterkey.credential.alias": "ranger.ks.masterkey.password",
            "ranger.ks.jpa.jdbc.url": "jdbc:mysql://c6401.ambari.apache.org:3306/rangerkms01",
            "hadoop.kms.blacklist.DECRYPT_EEK": "hdfs",
            "ranger.ks.jdbc.sqlconnectorjar": "{{ews_lib_jar_path}}",
            "ranger.ks.jpa.jdbc.user": "{{db_user}}",
            "ranger.ks.hsm.partition.password.alias": "ranger.kms.hsm.partition.password",
            "ranger.ks.hsm.type": "LunaProvider",
            "ranger.ks.hsm.partition.name": "par19",
             "ranger.ks.jpa.jdbc.dialect": "{{jdbc_dialect}}"
        }, 
        },
         "cluster-env": {
            "security_enabled": "true", 
            "override_uid": "true", 
            "fetch_nonlocal_groups": "true", 
            "one_dir_per_partition": "true", 
            "repo_ubuntu_template": "{{package_type}} {{base_url}} {{components}}", 
            "ignore_groupsusers_create": "false", 
            "alerts_repeat_tolerance": "1", 
            "smokeuser_keytab": "/etc/security/keytabs/smokeuser.headless.keytab", 
            "kerberos_domain": "EXAMPLE.COM", 
            "security_enabled": "true",
            "override_uid": "true",
            "fetch_nonlocal_groups": "true",
            "one_dir_per_partition": "true",
            "repo_ubuntu_template": "{{package_type}} {{base_url}} {{components}}",
            "ignore_groupsusers_create": "false",
            "alerts_repeat_tolerance": "1",
            "smokeuser_keytab": "/etc/security/keytabs/smokeuser.headless.keytab",
            "kerberos_domain": "EXAMPLE.COM",
             "manage_dirs_on_root": "true",
            "recovery_lifetime_max_count": "1024", 
            "recovery_type": "AUTO_START", 
            "ignore_bad_mounts": "false", 
            "recovery_window_in_minutes": "60", 
            "user_group": "hadoop", 
            "stack_tools": "{\n  \"stack_selector\": [\"hdp-select\", \"/usr/bin/hdp-select\", \"hdp-select\"],\n  \"conf_selector\": [\"conf-select\", \"/usr/bin/conf-select\", \"conf-select\"]\n}", 
            "recovery_retry_interval": "5", 
            "stack_features": "{\n  \"stack_features\": [\n    {\n      \"name\": \"snappy\",\n      \"description\": \"Snappy compressor/decompressor support\",\n      \"min_version\": \"2.0.0.0\",\n      \"max_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"lzo\",\n      \"description\": \"LZO libraries support\",\n      \"min_version\": \"2.2.1.0\"\n    },\n    {\n      \"name\": \"express_upgrade\",\n      \"description\": \"Express upgrade support\",\n      \"min_version\": \"2.1.0.0\"\n    },\n    {\n      \"name\": \"rolling_upgrade\",\n      \"description\": \"Rolling upgrade support\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"config_versioning\",\n      \"description\": \"Configurable versions support\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"datanode_non_root\",\n      \"description\": \"DataNode running as non-root support (AMBARI-7615)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"remove_ranger_hdfs_plugin_env\",\n      \"description\": \"HDFS removes Ranger env files (AMBARI-14299)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"ranger\",\n      \"description\": \"Ranger Service support\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"ranger_tagsync_component\",\n      \"description\": \"Ranger Tagsync component support (AMBARI-14383)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"phoenix\",\n      \"description\": \"Phoenix Service support\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"nfs\",\n      \"description\": \"NFS support\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"tez_for_spark\",\n      \"description\": \"Tez dependency for Spark\",\n      \"min_version\": \"2.2.0.0\",\n      \"max_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"timeline_state_store\",\n      \"description\": \"Yarn application timeline-service supports state store property (AMBARI-11442)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"copy_tarball_to_hdfs\",\n      \"description\": \"Copy tarball to HDFS support (AMBARI-12113)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"spark_16plus\",\n      \"description\": \"Spark 1.6+\",\n      \"min_version\": \"2.4.0.0\"\n    },\n    {\n      \"name\": \"spark_thriftserver\",\n      \"description\": \"Spark Thrift Server\",\n      \"min_version\": \"2.3.2.0\"\n    },\n    {\n      \"name\": \"storm_kerberos\",\n      \"description\": \"Storm Kerberos support (AMBARI-7570)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"storm_ams\",\n      \"description\": \"Storm AMS integration (AMBARI-10710)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"create_kafka_broker_id\",\n      \"description\": \"Ambari should create Kafka Broker Id (AMBARI-12678)\",\n      \"min_version\": \"2.2.0.0\",\n      \"max_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"kafka_listeners\",\n      \"description\": \"Kafka listeners (AMBARI-10984)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"kafka_kerberos\",\n      \"description\": \"Kafka Kerberos support (AMBARI-10984)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"pig_on_tez\",\n      \"description\": \"Pig on Tez support (AMBARI-7863)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"ranger_usersync_non_root\",\n      \"description\": \"Ranger Usersync as non-root user (AMBARI-10416)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"ranger_audit_db_support\",\n      \"description\": \"Ranger Audit to DB support\",\n      \"min_version\": \"2.2.0.0\",\n      \"max_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"accumulo_kerberos_user_auth\",\n      \"description\": \"Accumulo Kerberos User Auth (AMBARI-10163)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"knox_versioned_data_dir\",\n      \"description\": \"Use versioned data dir for Knox (AMBARI-13164)\",\n      \"min_version\": \"2.3.2.0\"\n    },\n    {\n      \"name\": \"knox_sso_topology\",\n      \"description\": \"Knox SSO Topology support (AMBARI-13975)\",\n      \"min_version\": \"2.3.8.0\"\n    },\n    {\n      \"name\": \"atlas_rolling_upgrade\",\n      \"description\": \"Rolling upgrade support for Atlas\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"oozie_admin_user\",\n      \"description\": \"Oozie install user as an Oozie admin user (AMBARI-7976)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"oozie_create_hive_tez_configs\",\n      \"description\": \"Oozie create configs for Ambari Hive and Tez deployments (AMBARI-8074)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"oozie_setup_shared_lib\",\n      \"description\": \"Oozie setup tools used to shared Oozie lib to HDFS (AMBARI-7240)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"oozie_host_kerberos\",\n      \"description\": \"Oozie in secured clusters uses _HOST in Kerberos principal (AMBARI-9775)\",\n      \"min_version\": \"2.0.0.0\",\n      \"max_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"falcon_extensions\",\n      \"description\": \"Falcon Extension\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hive_metastore_upgrade_schema\",\n      \"description\": \"Hive metastore upgrade schema support (AMBARI-11176)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"hive_server_interactive\",\n      \"description\": \"Hive server interactive support (AMBARI-15573)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hive_webhcat_specific_configs\",\n      \"description\": \"Hive webhcat specific configurations support (AMBARI-12364)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"hive_purge_table\",\n      \"description\": \"Hive purge table support (AMBARI-12260)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"hive_server2_kerberized_env\",\n      \"description\": \"Hive server2 working on kerberized environment (AMBARI-13749)\",\n      \"min_version\": \"2.2.3.0\",\n      \"max_version\": \"2.2.5.0\"\n    },\n    {\n      \"name\": \"hive_env_heapsize\",\n      \"description\": \"Hive heapsize property defined in hive-env (AMBARI-12801)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"ranger_kms_hsm_support\",\n      \"description\": \"Ranger KMS HSM support (AMBARI-15752)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_log4j_support\",\n      \"description\": \"Ranger supporting log-4j properties (AMBARI-15681)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_kerberos_support\",\n      \"description\": \"Ranger Kerberos support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hive_metastore_site_support\",\n      \"description\": \"Hive Metastore site support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_usersync_password_jceks\",\n      \"description\": \"Saving Ranger Usersync credentials in jceks\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_install_infra_client\",\n      \"description\": \"LogSearch Service support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hbase_home_directory\",\n      \"description\": \"Hbase home directory in HDFS needed for HBASE backup\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"spark_livy\",\n      \"description\": \"Livy as slave component of spark\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"atlas_ranger_plugin_support\",\n      \"description\": \"Atlas Ranger plugin support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_pid_support\",\n      \"description\": \"Ranger Service support pid generation AMBARI-16756\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_kms_pid_support\",\n      \"description\": \"Ranger KMS Service support pid generation\",\n      \"min_version\": \"2.5.0.0\"\n    }\n  ]\n}",
            "recovery_enabled": "true", 
            "smokeuser_principal_name": "ambari-qa-test_cluster01@EXAMPLE.COM", 
            "recovery_max_count": "6", 
            "stack_root": "/usr/hdp", 
            "repo_suse_rhel_template": "[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0", 
            "ambari_principal_name": "ambari-server-test_cluster01@EXAMPLE.COM", 
            "managed_hdfs_resource_property_names": "", 
            "recovery_lifetime_max_count": "1024",
            "recovery_type": "AUTO_START",
            "ignore_bad_mounts": "false",
            "recovery_window_in_minutes": "60",
            "user_group": "hadoop",
            "stack_name": "HDP",
            "stack_root": "{\"HDP\": \"/usr/hdp\"}",
            "stack_tools": "{\n \"HDP\": { \"stack_selector\": [\"hdp-select\", \"/usr/bin/hdp-select\", \"hdp-select\"],\n  \"conf_selector\": [\"conf-select\", \"/usr/bin/conf-select\", \"conf-select\"]\n}\n}",
            "recovery_retry_interval": "5",
            "recovery_enabled": "true",
            "smokeuser_principal_name": "ambari-qa-test_cluster01@EXAMPLE.COM",
            "recovery_max_count": "6",
            "repo_suse_rhel_template": "[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0",
            "ambari_principal_name": "ambari-server-test_cluster01@EXAMPLE.COM",
            "managed_hdfs_resource_property_names": "",
             "smokeuser": "ambari-qa"
         }
     }
}
\ No newline at end of file
}
diff --git a/ambari-server/src/test/python/stacks/2.6/configs/ranger-admin-default.json b/ambari-server/src/test/python/stacks/2.6/configs/ranger-admin-default.json
index abe84abe47..e5abe32430 100644
-- a/ambari-server/src/test/python/stacks/2.6/configs/ranger-admin-default.json
++ b/ambari-server/src/test/python/stacks/2.6/configs/ranger-admin-default.json
@@ -1,143 +1,143 @@
 {
     "localComponents": [
        "NAMENODE", 
        "SECONDARY_NAMENODE", 
        "ZOOKEEPER_SERVER", 
        "DATANODE", 
        "HDFS_CLIENT", 
        "ZOOKEEPER_CLIENT", 
        "RANGER_USERSYNC", 
        "RANGER_ADMIN", 
        "NAMENODE",
        "SECONDARY_NAMENODE",
        "ZOOKEEPER_SERVER",
        "DATANODE",
        "HDFS_CLIENT",
        "ZOOKEEPER_CLIENT",
        "RANGER_USERSYNC",
        "RANGER_ADMIN",
         "RANGER_TAGSYNC",
         "LOGSEARCH_SOLR",
         "LOGSEARCH_SOLR_CLIENT"
    ], 
    ],
     "configuration_attributes": {
        "ranger-hdfs-audit": {}, 
        "ssl-client": {}, 
        "ranger-admin-site": {}, 
        "ranger-hdfs-policymgr-ssl": {}, 
        "tagsync-application-properties": {}, 
        "ranger-env": {}, 
        "usersync-log4j": {}, 
        "admin-properties": {}, 
        "ranger-ugsync-site": {}, 
        "ranger-hdfs-audit": {},
        "ssl-client": {},
        "ranger-admin-site": {},
        "ranger-hdfs-policymgr-ssl": {},
        "tagsync-application-properties": {},
        "ranger-env": {},
        "usersync-log4j": {},
        "admin-properties": {},
        "ranger-ugsync-site": {},
         "hdfs-site": {
             "final": {
                "dfs.datanode.data.dir": "true", 
                "dfs.namenode.http-address": "true", 
                "dfs.datanode.failed.volumes.tolerated": "true", 
                "dfs.support.append": "true", 
                "dfs.namenode.name.dir": "true", 
                "dfs.datanode.data.dir": "true",
                "dfs.namenode.http-address": "true",
                "dfs.datanode.failed.volumes.tolerated": "true",
                "dfs.support.append": "true",
                "dfs.namenode.name.dir": "true",
                 "dfs.webhdfs.enabled": "true"
             }
        }, 
        },
         "ranger-tagsync-site": {},
         "ranger-tagsync-policymgr-ssl": {},
         "zoo.cfg": {},
         "hadoop-policy": {},
        "hdfs-log4j": {}, 
        "ranger-hdfs-plugin-properties": {}, 
        "hdfs-log4j": {},
        "ranger-hdfs-plugin-properties": {},
         "core-site": {
             "final": {
                 "fs.defaultFS": "true"
             }
        }, 
        "hadoop-env": {}, 
        "zookeeper-log4j": {}, 
        "ssl-server": {}, 
        "ranger-site": {}, 
        "admin-log4j": {}, 
        "tagsync-log4j": {}, 
        "ranger-hdfs-security": {}, 
        },
        "hadoop-env": {},
        "zookeeper-log4j": {},
        "ssl-server": {},
        "ranger-site": {},
        "admin-log4j": {},
        "tagsync-log4j": {},
        "ranger-hdfs-security": {},
         "ranger-solr-configuration": {},
         "usersync-properties": {},
         "zookeeper-env": {},
         "infra-solr-env": {},
         "infra-solr-client-log4j": {},
         "cluster-env": {}
    }, 
    "public_hostname": "c6401.ambari.apache.org", 
    "commandId": "11-0", 
    "hostname": "c6401.ambari.apache.org", 
    "kerberosCommandParams": [], 
    "serviceName": "RANGER", 
    "role": "RANGER_ADMIN", 
    "forceRefreshConfigTagsBeforeExecution": [], 
    "requestId": 11, 
    },
    "public_hostname": "c6401.ambari.apache.org",
    "commandId": "11-0",
    "hostname": "c6401.ambari.apache.org",
    "kerberosCommandParams": [],
    "serviceName": "RANGER",
    "role": "RANGER_ADMIN",
    "forceRefreshConfigTagsBeforeExecution": [],
    "requestId": 11,
     "agentConfigParams": {
         "agent": {
             "parallel_execution": 0
         }
    }, 
    "clusterName": "c1", 
    "commandType": "EXECUTION_COMMAND", 
    "taskId": 31, 
    "roleParams": {}, 
    },
    "clusterName": "c1",
    "commandType": "EXECUTION_COMMAND",
    "taskId": 31,
    "roleParams": {},
     "configurationTags": {
         "ranger-hdfs-audit": {
             "tag": "version1466705299922"
        }, 
        },
         "ssl-client": {
             "tag": "version1"
        }, 
        },
         "ranger-admin-site": {
             "tag": "version1466705299949"
        }, 
        },
         "ranger-hdfs-policymgr-ssl": {
             "tag": "version1466705299922"
        }, 
        },
         "tagsync-application-properties": {
             "tag": "version1466705299949"
        }, 
        },
         "ranger-env": {
             "tag": "version1466705299949"
        }, 
        },
         "usersync-log4j": {
             "tag": "version1466705299949"
        }, 
        },
         "admin-properties": {
             "tag": "version1466705299949"
        }, 
        },
         "ranger-ugsync-site": {
             "tag": "version1466705299949"
        }, 
        },
         "hdfs-site": {
             "tag": "version1"
        }, 
        },
         "ranger-tagsync-site": {
             "tag": "version1466705299949"
        }, 
        },
         "zoo.cfg": {
             "tag": "version1"
        }, 
        },
         "hadoop-policy": {
             "tag": "version1"
        }, 
        },
         "hdfs-log4j": {
             "tag": "version1"
        }, 
        },
         "ranger-hdfs-plugin-properties": {
             "tag": "version1466705299922"
        }, 
        },
         "core-site": {
             "tag": "version1"
        }, 
        },
         "hadoop-env": {
             "tag": "version1"
        }, 
        },
         "zookeeper-log4j": {
             "tag": "version1"
        }, 
        },
         "ssl-server": {
             "tag": "version1"
        }, 
        },
         "ranger-site": {
             "tag": "version1466705299949"
        }, 
        },
         "admin-log4j": {
             "tag": "version1466705299949"
        }, 
        },
         "tagsync-log4j": {
             "tag": "version1466705299949"
         },
@@ -146,7 +146,7 @@
         },
         "ranger-hdfs-security": {
             "tag": "version1466705299922"
        }, 
        },
         "usersync-properties": {
             "tag": "version1466705299949"
         },
@@ -165,116 +165,116 @@
         "cluster-env": {
             "tag": "version1"
         }
    }, 
    "roleCommand": "START", 
    },
    "roleCommand": "START",
     "hostLevelParams": {
        "agent_stack_retry_on_unavailability": "false", 
        "stack_name": "HDP", 
        "agent_stack_retry_on_unavailability": "false",
        "stack_name": "HDP",
         "package_version": "2_6_0_0_*",
         "custom_mysql_jdbc_name": "mysql-connector-java.jar",
         "previous_custom_mysql_jdbc_name": "mysql-connector-java-old.jar",
        "host_sys_prepped": "false", 
        "ambari_db_rca_username": "mapred", 
        "host_sys_prepped": "false",
        "ambari_db_rca_username": "mapred",
         "current_version": "2.6.0.0-801",
         "mysql_jdbc_url": "http://c6401.ambari.apache.org:8080/resources//mysql-connector-java.jar",
         "agent_stack_retry_count": "5",
         "stack_version": "2.6",
        "jdk_name": "jdk-8u60-linux-x64.tar.gz", 
        "ambari_db_rca_driver": "org.postgresql.Driver", 
        "java_home": "/usr/jdk64/jdk1.7.0_45", 
        "repository_version_id": "1", 
        "jdk_location": "http://c6401.ambari.apache.org:8080/resources/", 
        "not_managed_hdfs_path_list": "[\"/tmp\"]", 
        "ambari_db_rca_url": "jdbc:postgresql://c6401.ambari.apache.org/ambarirca", 
        "java_version": "8", 
        "jdk_name": "jdk-8u60-linux-x64.tar.gz",
        "ambari_db_rca_driver": "org.postgresql.Driver",
        "java_home": "/usr/jdk64/jdk1.7.0_45",
        "repository_version_id": "1",
        "jdk_location": "http://c6401.ambari.apache.org:8080/resources/",
        "not_managed_hdfs_path_list": "[\"/tmp\"]",
        "ambari_db_rca_url": "jdbc:postgresql://c6401.ambari.apache.org/ambarirca",
        "java_version": "8",
         "repo_info": "[{\"baseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.6.0.0-801\",\"osType\":\"redhat6\",\"repoId\":\"HDP-2.6\",\"repoName\":\"HDP\",\"defaultBaseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/updates/2.6.0.0\",\"latestBaseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.6.0.0-801\",\"baseSaved\":true},{\"baseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"osType\":\"redhat6\",\"repoId\":\"HDP-UTILS-1.1.0.21\",\"repoName\":\"HDP-UTILS\",\"defaultBaseUrl\":\"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"latestBaseUrl\":\"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"baseSaved\":true}]",
         "package_list": "[{\"name\":\"ranger_${stack_version}-admin\",\"condition\":\"\",\"skipUpgrade\":false},{\"name\":\"ranger_${stack_version}-usersync\",\"condition\":\"\",\"skipUpgrade\":false},{\"name\":\"ranger_${stack_version}-tagsync\",\"condition\":\"should_install_ranger_tagsync\",\"skipUpgrade\":false},{\"name\":\"ambari-logsearch-solr-client\",\"condition\":\"should_install_logsearch_solr_client\",\"skipUpgrade\":false}]",
        "db_name": "ambari", 
        "group_list": "[\"ranger\",\"hadoop\",\"users\"]", 
        "agentCacheDir": "/var/lib/ambari-agent/cache", 
        "ambari_db_rca_password": "mapred", 
        "jce_name": "jce_policy-8.zip", 
        "oracle_jdbc_url": "http://c6401.ambari.apache.org:8080/resources//ojdbc6.jar", 
        "db_driver_filename": "mysql-connector-java.jar", 
        "user_list": "[\"zookeeper\",\"ambari-qa\",\"ranger\",\"hdfs\"]", 
        "db_name": "ambari",
        "group_list": "[\"ranger\",\"hadoop\",\"users\"]",
        "agentCacheDir": "/var/lib/ambari-agent/cache",
        "ambari_db_rca_password": "mapred",
        "jce_name": "jce_policy-8.zip",
        "oracle_jdbc_url": "http://c6401.ambari.apache.org:8080/resources//ojdbc6.jar",
        "db_driver_filename": "mysql-connector-java.jar",
        "user_list": "[\"zookeeper\",\"ambari-qa\",\"ranger\",\"hdfs\"]",
         "clientsToUpdateConfigs": "[\"*\"]"
    }, 
    },
     "commandParams": {
        "service_package_folder": "common-services/RANGER/0.4.0/package", 
        "script": "scripts/ranger_admin.py", 
        "service_package_folder": "common-services/RANGER/0.4.0/package",
        "script": "scripts/ranger_admin.py",
         "hooks_folder": "HDP/2.0.6/hooks",
         "version": "2.6.0.0-801",
        "max_duration_for_retries": "0", 
        "command_retry_enabled": "false", 
        "command_timeout": "600", 
        "max_duration_for_retries": "0",
        "command_retry_enabled": "false",
        "command_timeout": "600",
         "script_type": "PYTHON"
    }, 
    "forceRefreshConfigTags": [], 
    "stageId": 0, 
    },
    "forceRefreshConfigTags": [],
    "stageId": 0,
     "clusterHostInfo": {
         "snamenode_host": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "ambari_server_use_ssl": [
             "false"
        ], 
        ],
         "all_ping_ports": [
             "8670"
        ], 
        ],
         "ranger_tagsync_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "ranger_usersync_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "all_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "slave_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "namenode_host": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "ambari_server_port": [
             "8080"
        ], 
        ],
         "ranger_admin_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "all_racks": [
             "/default-rack"
        ], 
        ],
         "all_ipv4_ips": [
             "172.22.83.73"
        ], 
        ],
         "ambari_server_host": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "zookeeper_hosts": [
             "c6401.ambari.apache.org"
         ],
         "infra_solr_hosts": [
             "c6401.ambari.apache.org"
         ]
    }, 
    },
     "configurations": {
         "ranger-hdfs-audit": {
            "xasecure.audit.destination.solr.zookeepers": "NONE", 
            "xasecure.audit.destination.solr.urls": "", 
            "xasecure.audit.destination.solr.batch.filespool.dir": "/var/log/hadoop/hdfs/audit/solr/spool", 
            "xasecure.audit.destination.hdfs.batch.filespool.dir": "/var/log/hadoop/hdfs/audit/hdfs/spool", 
            "xasecure.audit.destination.solr.zookeepers": "NONE",
            "xasecure.audit.destination.solr.urls": "",
            "xasecure.audit.destination.solr.batch.filespool.dir": "/var/log/hadoop/hdfs/audit/solr/spool",
            "xasecure.audit.destination.hdfs.batch.filespool.dir": "/var/log/hadoop/hdfs/audit/hdfs/spool",
             "xasecure.audit.destination.hdfs": "true",
            "xasecure.audit.destination.solr": "false", 
            "xasecure.audit.destination.solr": "false",
             "xasecure.audit.provider.summary.enabled": "false",
             "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit",
             "xasecure.audit.is.enabled": "true"
         },
         "ranger-tagsync-policymgr-ssl": {
            "xasecure.policymgr.clientssl.keystore": "/etc/security/serverKeys/ranger-tagsync-keystore.jks", 
            "xasecure.policymgr.clientssl.truststore.password": "changeit", 
            "xasecure.policymgr.clientssl.keystore": "/etc/security/serverKeys/ranger-tagsync-keystore.jks",
            "xasecure.policymgr.clientssl.truststore.password": "changeit",
             "xasecure.policymgr.clientssl.keystore.credential.file": "jceks://file{{ranger_tagsync_credential_file}}",
            "xasecure.policymgr.clientssl.truststore": "/etc/security/serverKeys/ranger-tagsync-mytruststore.jks", 
            "xasecure.policymgr.clientssl.truststore": "/etc/security/serverKeys/ranger-tagsync-mytruststore.jks",
             "xasecure.policymgr.clientssl.truststore.credential.file": "jceks://file{{ranger_tagsync_credential_file}}",
             "xasecure.policymgr.clientssl.keystore.password": "myKeyFilePassword"
         },
@@ -287,143 +287,143 @@
             "xasecure.policymgr.clientssl.keystore.password": "myKeyFilePassword"
         },
         "ssl-client": {
            "ssl.client.truststore.reload.interval": "10000", 
            "ssl.client.keystore.password": "bigdata", 
            "ssl.client.truststore.type": "jks", 
            "ssl.client.keystore.location": "/etc/security/clientKeys/keystore.jks", 
            "ssl.client.truststore.location": "/etc/security/clientKeys/all.jks", 
            "ssl.client.truststore.password": "bigdata", 
            "ssl.client.truststore.reload.interval": "10000",
            "ssl.client.keystore.password": "bigdata",
            "ssl.client.truststore.type": "jks",
            "ssl.client.keystore.location": "/etc/security/clientKeys/keystore.jks",
            "ssl.client.truststore.location": "/etc/security/clientKeys/all.jks",
            "ssl.client.truststore.password": "bigdata",
             "ssl.client.keystore.type": "jks"
        }, 
        },
         "ranger-admin-site": {
             "ranger.admin.kerberos.cookie.domain": "",
            "ranger.kms.service.user.hdfs": "hdfs", 
            "ranger.spnego.kerberos.principal": "", 
            "ranger.ldap.ad.url": "{{ranger_ug_ldap_url}}", 
            "ranger.plugins.hive.serviceuser": "hive", 
            "ranger.lookup.kerberos.keytab": "", 
            "ranger.plugins.kms.serviceuser": "kms", 
            "ranger.service.https.attrib.ssl.enabled": "false", 
            "ranger.sso.browser.useragent": "Mozilla,chrome", 
            "ranger.jpa.jdbc.url": "jdbc:mysql://c6401.ambari.apache.org:3306/ranger01", 
            "ranger.plugins.hbase.serviceuser": "hbase", 
            "ranger.plugins.hdfs.serviceuser": "hdfs", 
            "ranger.ldap.group.searchbase": "{{ranger_ug_ldap_group_searchbase}}", 
            "ranger.ldap.user.dnpattern": "uid={0},ou=users,dc=xasecure,dc=net", 
            "ranger.plugins.knox.serviceuser": "knox", 
            "ranger.ldap.base.dn": "dc=example,dc=com", 
            "ranger.sso.publicKey": "", 
            "ranger.admin.kerberos.cookie.path": "/", 
            "ranger.service.https.attrib.clientAuth": "want", 
            "ranger.jpa.jdbc.user": "{{ranger_db_user}}", 
            "ranger.ldap.ad.user.searchfilter": "(sAMAccountName={0})", 
            "ranger.ldap.group.roleattribute": "cn", 
            "ranger.plugins.kafka.serviceuser": "kafka", 
            "ranger.admin.kerberos.principal": "", 
            "ranger.ldap.ad.bind.dn": "{{ranger_ug_ldap_bind_dn}}", 
            "ranger.credential.provider.path": "/etc/ranger/admin/rangeradmin.jceks", 
            "ranger.ldap.referral": "ignore", 
            "ranger.service.http.port": "6080", 
            "ranger.ldap.user.searchfilter": "(uid={0})", 
            "ranger.plugins.atlas.serviceuser": "atlas", 
            "ranger.kms.service.user.hdfs": "hdfs",
            "ranger.spnego.kerberos.principal": "",
            "ranger.ldap.ad.url": "{{ranger_ug_ldap_url}}",
            "ranger.plugins.hive.serviceuser": "hive",
            "ranger.lookup.kerberos.keytab": "",
            "ranger.plugins.kms.serviceuser": "kms",
            "ranger.service.https.attrib.ssl.enabled": "false",
            "ranger.sso.browser.useragent": "Mozilla,chrome",
            "ranger.jpa.jdbc.url": "jdbc:mysql://c6401.ambari.apache.org:3306/ranger01",
            "ranger.plugins.hbase.serviceuser": "hbase",
            "ranger.plugins.hdfs.serviceuser": "hdfs",
            "ranger.ldap.group.searchbase": "{{ranger_ug_ldap_group_searchbase}}",
            "ranger.ldap.user.dnpattern": "uid={0},ou=users,dc=xasecure,dc=net",
            "ranger.plugins.knox.serviceuser": "knox",
            "ranger.ldap.base.dn": "dc=example,dc=com",
            "ranger.sso.publicKey": "",
            "ranger.admin.kerberos.cookie.path": "/",
            "ranger.service.https.attrib.clientAuth": "want",
            "ranger.jpa.jdbc.user": "{{ranger_db_user}}",
            "ranger.ldap.ad.user.searchfilter": "(sAMAccountName={0})",
            "ranger.ldap.group.roleattribute": "cn",
            "ranger.plugins.kafka.serviceuser": "kafka",
            "ranger.admin.kerberos.principal": "",
            "ranger.ldap.ad.bind.dn": "{{ranger_ug_ldap_bind_dn}}",
            "ranger.credential.provider.path": "/etc/ranger/admin/rangeradmin.jceks",
            "ranger.ldap.referral": "ignore",
            "ranger.service.http.port": "6080",
            "ranger.ldap.user.searchfilter": "(uid={0})",
            "ranger.plugins.atlas.serviceuser": "atlas",
             "ranger.truststore.password": "changeit",
             "ranger.truststore.alias": "trustStoreAlias",
            "ranger.ldap.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}", 
            "ranger.audit.solr.password": "NONE", 
            "ranger.ldap.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}",
            "ranger.audit.solr.password": "NONE",
             "ranger.audit.solr.zookeepers": "c6401.ambari.apache.org:2181/infra-solr",
            "ranger.lookup.kerberos.principal": "", 
            "ranger.service.https.port": "6182", 
            "ranger.plugins.storm.serviceuser": "storm", 
            "ranger.externalurl": "{{ranger_external_url}}", 
            "ranger.truststore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks", 
            "ranger.kms.service.user.hive": "", 
            "ranger.https.attrib.keystore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks", 
            "ranger.jpa.jdbc.dialect": "{{jdbc_dialect}}", 
            "ranger.service.host": "{{ranger_host}}", 
            "ranger.service.https.attrib.keystore.keyalias": "rangeradmin", 
            "ranger.service.https.attrib.keystore.pass": "xasecure", 
            "ranger.unixauth.remote.login.enabled": "true", 
            "ranger.jpa.jdbc.credential.alias": "rangeradmin", 
            "ranger.ldap.ad.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}", 
            "ranger.audit.solr.username": "ranger_solr", 
            "ranger.sso.enabled": "false", 
            "ranger.audit.solr.urls": "", 
            "ranger.ldap.ad.domain": "", 
            "ranger.plugins.yarn.serviceuser": "yarn", 
            "ranger.audit.source.type": "solr", 
            "ranger.ldap.bind.dn": "{{ranger_ug_ldap_bind_dn}}", 
            "ranger.ldap.url": "{{ranger_ug_ldap_url}}", 
            "ranger.authentication.method": "UNIX", 
            "ranger.service.http.enabled": "true", 
            "ranger.ldap.group.searchfilter": "{{ranger_ug_ldap_group_searchfilter}}", 
            "ranger.ldap.ad.referral": "ignore", 
            "ranger.ldap.ad.base.dn": "dc=example,dc=com", 
            "ranger.jpa.jdbc.password": "_", 
            "ranger.spnego.kerberos.keytab": "", 
            "ranger.sso.providerurl": "", 
            "ranger.unixauth.service.hostname": "{{ugsync_host}}", 
            "ranger.admin.kerberos.keytab": "", 
            "ranger.admin.kerberos.token.valid.seconds": "30", 
            "ranger.jpa.jdbc.driver": "com.mysql.jdbc.Driver", 
            "ranger.lookup.kerberos.principal": "",
            "ranger.service.https.port": "6182",
            "ranger.plugins.storm.serviceuser": "storm",
            "ranger.externalurl": "{{ranger_external_url}}",
            "ranger.truststore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks",
            "ranger.kms.service.user.hive": "",
            "ranger.https.attrib.keystore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks",
            "ranger.jpa.jdbc.dialect": "{{jdbc_dialect}}",
            "ranger.service.host": "{{ranger_host}}",
            "ranger.service.https.attrib.keystore.keyalias": "rangeradmin",
            "ranger.service.https.attrib.keystore.pass": "xasecure",
            "ranger.unixauth.remote.login.enabled": "true",
            "ranger.jpa.jdbc.credential.alias": "rangeradmin",
            "ranger.ldap.ad.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}",
            "ranger.audit.solr.username": "ranger_solr",
            "ranger.sso.enabled": "false",
            "ranger.audit.solr.urls": "",
            "ranger.ldap.ad.domain": "",
            "ranger.plugins.yarn.serviceuser": "yarn",
            "ranger.audit.source.type": "solr",
            "ranger.ldap.bind.dn": "{{ranger_ug_ldap_bind_dn}}",
            "ranger.ldap.url": "{{ranger_ug_ldap_url}}",
            "ranger.authentication.method": "UNIX",
            "ranger.service.http.enabled": "true",
            "ranger.ldap.group.searchfilter": "{{ranger_ug_ldap_group_searchfilter}}",
            "ranger.ldap.ad.referral": "ignore",
            "ranger.ldap.ad.base.dn": "dc=example,dc=com",
            "ranger.jpa.jdbc.password": "_",
            "ranger.spnego.kerberos.keytab": "",
            "ranger.sso.providerurl": "",
            "ranger.unixauth.service.hostname": "{{ugsync_host}}",
            "ranger.admin.kerberos.keytab": "",
            "ranger.admin.kerberos.token.valid.seconds": "30",
            "ranger.jpa.jdbc.driver": "com.mysql.jdbc.Driver",
             "ranger.unixauth.service.port": "5151",
             "ranger.service.https.attrib.keystore.credential.alias": "keyStoreCredentialAlias"
        }, 
        },
         "ranger-hdfs-policymgr-ssl": {
            "xasecure.policymgr.clientssl.keystore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-keystore.jks", 
            "xasecure.policymgr.clientssl.truststore.password": "changeit", 
            "xasecure.policymgr.clientssl.keystore.credential.file": "jceks://file{{credential_file}}", 
            "xasecure.policymgr.clientssl.truststore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-truststore.jks", 
            "xasecure.policymgr.clientssl.truststore.credential.file": "jceks://file{{credential_file}}", 
            "xasecure.policymgr.clientssl.keystore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-keystore.jks",
            "xasecure.policymgr.clientssl.truststore.password": "changeit",
            "xasecure.policymgr.clientssl.keystore.credential.file": "jceks://file{{credential_file}}",
            "xasecure.policymgr.clientssl.truststore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-truststore.jks",
            "xasecure.policymgr.clientssl.truststore.credential.file": "jceks://file{{credential_file}}",
             "xasecure.policymgr.clientssl.keystore.password": "myKeyFilePassword"
        }, 
        },
         "tagsync-application-properties": {
            "atlas.kafka.entities.group.id": "ranger_entities_consumer", 
            "atlas.kafka.zookeeper.connect": "c6401.ambari.apache.org:2181", 
            "atlas.kafka.entities.group.id": "ranger_entities_consumer",
            "atlas.kafka.zookeeper.connect": "c6401.ambari.apache.org:2181",
             "atlas.kafka.bootstrap.servers": "localhost:6667"
        }, 
        },
         "ranger-env": {
            "ranger_solr_shards": "1", 
            "ranger_solr_config_set": "ranger_audits", 
            "ranger_user": "ranger", 
            "ranger_solr_shards": "1",
            "ranger_solr_config_set": "ranger_audits",
            "ranger_user": "ranger",
             "ranger_solr_replication_factor": "1",
            "xml_configurations_supported": "true", 
            "ranger-atlas-plugin-enabled": "No", 
            "ranger-hbase-plugin-enabled": "No", 
            "ranger-yarn-plugin-enabled": "No", 
            "bind_anonymous": "false", 
            "ranger_admin_username": "amb_ranger_admin", 
            "admin_password": "admin", 
            "is_solrCloud_enabled": "true", 
            "ranger-storm-plugin-enabled": "No", 
            "ranger-hdfs-plugin-enabled": "No", 
            "ranger_group": "ranger", 
            "ranger-knox-plugin-enabled": "No", 
            "ranger_admin_log_dir": "/var/log/ranger/admin", 
            "ranger-kafka-plugin-enabled": "No", 
            "ranger_privelege_user_jdbc_url": "jdbc:mysql://c6401.ambari.apache.org:3306", 
            "ranger-hive-plugin-enabled": "No", 
            "xasecure.audit.destination.solr": "true", 
            "ranger_pid_dir": "/var/run/ranger", 
            "xasecure.audit.destination.hdfs": "true", 
            "admin_username": "admin", 
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit", 
            "create_db_dbuser": "true", 
            "ranger_solr_collection_name": "ranger_audits", 
            "ranger_admin_password": "P1!q9xa96SMi5NCl", 
            "xml_configurations_supported": "true",
            "ranger-atlas-plugin-enabled": "No",
            "ranger-hbase-plugin-enabled": "No",
            "ranger-yarn-plugin-enabled": "No",
            "bind_anonymous": "false",
            "ranger_admin_username": "amb_ranger_admin",
            "admin_password": "admin",
            "is_solrCloud_enabled": "true",
            "ranger-storm-plugin-enabled": "No",
            "ranger-hdfs-plugin-enabled": "No",
            "ranger_group": "ranger",
            "ranger-knox-plugin-enabled": "No",
            "ranger_admin_log_dir": "/var/log/ranger/admin",
            "ranger-kafka-plugin-enabled": "No",
            "ranger_privelege_user_jdbc_url": "jdbc:mysql://c6401.ambari.apache.org:3306",
            "ranger-hive-plugin-enabled": "No",
            "xasecure.audit.destination.solr": "true",
            "ranger_pid_dir": "/var/run/ranger",
            "xasecure.audit.destination.hdfs": "true",
            "admin_username": "admin",
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit",
            "create_db_dbuser": "true",
            "ranger_solr_collection_name": "ranger_audits",
            "ranger_admin_password": "P1!q9xa96SMi5NCl",
             "ranger_usersync_log_dir": "/var/log/ranger/usersync"
        }, 
        },
         "usersync-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#      http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n#\n\nlog4j.rootLogger = info,logFile\n\n# logFile\nlog4j.appender.logFile=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.logFile.file=${logdir}/usersync.log\nlog4j.appender.logFile.datePattern='.'yyyy-MM-dd\nlog4j.appender.logFile.layout=org.apache.log4j.PatternLayout\nlog4j.appender.logFile.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %m%n\n\n# console\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.Target=System.out\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %m%n"
        }, 
        },
         "admin-properties": {
            "db_user": "rangeradmin01", 
            "DB_FLAVOR": "MYSQL", 
            "db_password": "rangeradmin01", 
            "db_root_user": "root", 
            "policymgr_external_url": "http://c6401.ambari.apache.org:6080", 
            "db_name": "ranger01", 
            "db_host": "c6401.ambari.apache.org", 
            "db_root_password": "vagrant", 
            "db_user": "rangeradmin01",
            "DB_FLAVOR": "MYSQL",
            "db_password": "rangeradmin01",
            "db_root_user": "root",
            "policymgr_external_url": "http://c6401.ambari.apache.org:6080",
            "db_name": "ranger01",
            "db_host": "c6401.ambari.apache.org",
            "db_root_password": "vagrant",
             "SQL_CONNECTOR_JAR": "{{driver_curl_target}}"
         },
         "ranger-solr-configuration": {
@@ -432,248 +432,248 @@
             "ranger_audit_logs_merge_factor": "5"
         },
         "ranger-ugsync-site": {
            "ranger.usersync.ldap.binddn": "", 
            "ranger.usersync.policymgr.username": "rangerusersync", 
            "ranger.usersync.policymanager.mockrun": "false", 
            "ranger.usersync.group.searchbase": "", 
            "ranger.usersync.ldap.bindalias": "testldapalias", 
            "ranger.usersync.truststore.file": "/usr/hdp/current/ranger-usersync/conf/mytruststore.jks", 
            "ranger.usersync.port": "5151", 
            "ranger.usersync.pagedresultssize": "500", 
            "ranger.usersync.group.memberattributename": "", 
            "ranger.usersync.kerberos.principal": "", 
            "ranger.usersync.source.impl.class": "org.apache.ranger.unixusersync.process.UnixUserGroupBuilder", 
            "ranger.usersync.ldap.referral": "ignore", 
            "ranger.usersync.group.searchfilter": "", 
            "ranger.usersync.ldap.user.objectclass": "person", 
            "ranger.usersync.logdir": "{{usersync_log_dir}}", 
            "ranger.usersync.ldap.user.searchfilter": "", 
            "ranger.usersync.ldap.groupname.caseconversion": "none", 
            "ranger.usersync.ldap.ldapbindpassword": "", 
            "ranger.usersync.unix.minUserId": "500", 
            "ranger.usersync.policymanager.maxrecordsperapicall": "1000", 
            "ranger.usersync.group.nameattribute": "", 
            "ranger.usersync.policymgr.alias": "ranger.usersync.policymgr.password", 
            "ranger.usersync.keystore.file": "/usr/hdp/current/ranger-usersync/conf/unixauthservice.jks", 
            "ranger.usersync.user.searchenabled": "false", 
            "ranger.usersync.group.usermapsyncenabled": "true", 
            "ranger.usersync.ldap.bindkeystore": "", 
            "ranger.usersync.ldap.user.groupnameattribute": "memberof, ismemberof", 
            "ranger.usersync.kerberos.keytab": "", 
            "ranger.usersync.passwordvalidator.path": "./native/credValidator.uexe", 
            "ranger.usersync.group.objectclass": "", 
            "ranger.usersync.ldap.user.searchscope": "sub", 
            "ranger.usersync.unix.password.file": "/etc/passwd", 
            "ranger.usersync.ldap.user.nameattribute": "", 
            "ranger.usersync.pagedresultsenabled": "true", 
            "ranger.usersync.policymanager.baseURL": "{{ranger_external_url}}", 
            "ranger.usersync.group.search.first.enabled": "false", 
            "ranger.usersync.group.searchenabled": "false", 
            "ranger.usersync.sink.impl.class": "org.apache.ranger.unixusersync.process.PolicyMgrUserGroupBuilder", 
            "ranger.usersync.ssl": "true", 
            "ranger.usersync.ldap.url": "", 
            "ranger.usersync.ldap.searchBase": "dc=hadoop,dc=apache,dc=org", 
            "ranger.usersync.policymgr.keystore": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks", 
            "ranger.usersync.ldap.user.searchbase": "", 
            "ranger.usersync.ldap.username.caseconversion": "none", 
            "ranger.usersync.credstore.filename": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks", 
            "ranger.usersync.keystore.password": "UnIx529p", 
            "ranger.usersync.unix.group.file": "/etc/group", 
            "ranger.usersync.filesource.file": "/tmp/usergroup.txt", 
            "ranger.usersync.group.searchscope": "", 
            "ranger.usersync.truststore.password": "changeit", 
            "ranger.usersync.enabled": "true", 
            "ranger.usersync.sleeptimeinmillisbetweensynccycle": "60000", 
            "ranger.usersync.ldap.binddn": "",
            "ranger.usersync.policymgr.username": "rangerusersync",
            "ranger.usersync.policymanager.mockrun": "false",
            "ranger.usersync.group.searchbase": "",
            "ranger.usersync.ldap.bindalias": "testldapalias",
            "ranger.usersync.truststore.file": "/usr/hdp/current/ranger-usersync/conf/mytruststore.jks",
            "ranger.usersync.port": "5151",
            "ranger.usersync.pagedresultssize": "500",
            "ranger.usersync.group.memberattributename": "",
            "ranger.usersync.kerberos.principal": "",
            "ranger.usersync.source.impl.class": "org.apache.ranger.unixusersync.process.UnixUserGroupBuilder",
            "ranger.usersync.ldap.referral": "ignore",
            "ranger.usersync.group.searchfilter": "",
            "ranger.usersync.ldap.user.objectclass": "person",
            "ranger.usersync.logdir": "{{usersync_log_dir}}",
            "ranger.usersync.ldap.user.searchfilter": "",
            "ranger.usersync.ldap.groupname.caseconversion": "none",
            "ranger.usersync.ldap.ldapbindpassword": "",
            "ranger.usersync.unix.minUserId": "500",
            "ranger.usersync.policymanager.maxrecordsperapicall": "1000",
            "ranger.usersync.group.nameattribute": "",
            "ranger.usersync.policymgr.alias": "ranger.usersync.policymgr.password",
            "ranger.usersync.keystore.file": "/usr/hdp/current/ranger-usersync/conf/unixauthservice.jks",
            "ranger.usersync.user.searchenabled": "false",
            "ranger.usersync.group.usermapsyncenabled": "true",
            "ranger.usersync.ldap.bindkeystore": "",
            "ranger.usersync.ldap.user.groupnameattribute": "memberof, ismemberof",
            "ranger.usersync.kerberos.keytab": "",
            "ranger.usersync.passwordvalidator.path": "./native/credValidator.uexe",
            "ranger.usersync.group.objectclass": "",
            "ranger.usersync.ldap.user.searchscope": "sub",
            "ranger.usersync.unix.password.file": "/etc/passwd",
            "ranger.usersync.ldap.user.nameattribute": "",
            "ranger.usersync.pagedresultsenabled": "true",
            "ranger.usersync.policymanager.baseURL": "{{ranger_external_url}}",
            "ranger.usersync.group.search.first.enabled": "false",
            "ranger.usersync.group.searchenabled": "false",
            "ranger.usersync.sink.impl.class": "org.apache.ranger.unixusersync.process.PolicyMgrUserGroupBuilder",
            "ranger.usersync.ssl": "true",
            "ranger.usersync.ldap.url": "",
            "ranger.usersync.ldap.searchBase": "dc=hadoop,dc=apache,dc=org",
            "ranger.usersync.policymgr.keystore": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks",
            "ranger.usersync.ldap.user.searchbase": "",
            "ranger.usersync.ldap.username.caseconversion": "none",
            "ranger.usersync.credstore.filename": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks",
            "ranger.usersync.keystore.password": "UnIx529p",
            "ranger.usersync.unix.group.file": "/etc/group",
            "ranger.usersync.filesource.file": "/tmp/usergroup.txt",
            "ranger.usersync.group.searchscope": "",
            "ranger.usersync.truststore.password": "changeit",
            "ranger.usersync.enabled": "true",
            "ranger.usersync.sleeptimeinmillisbetweensynccycle": "60000",
             "ranger.usersync.filesource.text.delimiter": ","
        }, 
        },
         "hdfs-site": {
            "dfs.namenode.checkpoint.period": "21600", 
            "dfs.namenode.avoid.write.stale.datanode": "true", 
            "dfs.namenode.startup.delay.block.deletion.sec": "3600", 
            "dfs.namenode.checkpoint.txns": "1000000", 
            "dfs.content-summary.limit": "5000", 
            "dfs.support.append": "true", 
            "dfs.datanode.address": "0.0.0.0:50010", 
            "dfs.cluster.administrators": " hdfs", 
            "dfs.namenode.audit.log.async": "true", 
            "dfs.datanode.balance.bandwidthPerSec": "6250000", 
            "dfs.namenode.safemode.threshold-pct": "1", 
            "dfs.namenode.checkpoint.edits.dir": "${dfs.namenode.checkpoint.dir}", 
            "dfs.namenode.rpc-address": "c6401.ambari.apache.org:8020", 
            "dfs.permissions.enabled": "true", 
            "dfs.client.read.shortcircuit": "true", 
            "dfs.https.port": "50470", 
            "dfs.namenode.https-address": "c6401.ambari.apache.org:50470", 
            "nfs.file.dump.dir": "/tmp/.hdfs-nfs", 
            "dfs.blocksize": "134217728", 
            "dfs.blockreport.initialDelay": "120", 
            "dfs.journalnode.edits.dir": "/hadoop/hdfs/journalnode", 
            "dfs.namenode.fslock.fair": "false", 
            "dfs.datanode.max.transfer.threads": "4096", 
            "dfs.heartbeat.interval": "3", 
            "dfs.replication": "3", 
            "dfs.namenode.handler.count": "50", 
            "dfs.namenode.checkpoint.dir": "/grid/0/hadoop/hdfs/namesecondary", 
            "fs.permissions.umask-mode": "022", 
            "dfs.namenode.stale.datanode.interval": "30000", 
            "dfs.datanode.ipc.address": "0.0.0.0:8010", 
            "dfs.datanode.failed.volumes.tolerated": "0", 
            "dfs.datanode.data.dir": "/grid/0/hadoop/hdfs/data", 
            "dfs.namenode.http-address": "c6401.ambari.apache.org:50070", 
            "dfs.webhdfs.enabled": "true", 
            "dfs.encrypt.data.transfer.cipher.suites": "AES/CTR/NoPadding", 
            "dfs.namenode.accesstime.precision": "0", 
            "dfs.datanode.https.address": "0.0.0.0:50475", 
            "dfs.namenode.write.stale.datanode.ratio": "1.0f", 
            "dfs.namenode.secondary.http-address": "c6401.ambari.apache.org:50090", 
            "nfs.exports.allowed.hosts": "* rw", 
            "dfs.datanode.http.address": "0.0.0.0:50075", 
            "dfs.datanode.du.reserved": "33011188224", 
            "dfs.client.read.shortcircuit.streams.cache.size": "4096", 
            "dfs.http.policy": "HTTP_ONLY", 
            "dfs.block.access.token.enable": "true", 
            "dfs.client.retry.policy.enabled": "false", 
            "dfs.namenode.name.dir.restore": "true", 
            "dfs.permissions.superusergroup": "hdfs", 
            "dfs.journalnode.https-address": "0.0.0.0:8481", 
            "dfs.journalnode.http-address": "0.0.0.0:8480", 
            "dfs.domain.socket.path": "/var/lib/hadoop-hdfs/dn_socket", 
            "dfs.namenode.avoid.read.stale.datanode": "true", 
            "dfs.hosts.exclude": "/etc/hadoop/conf/dfs.exclude", 
            "dfs.datanode.data.dir.perm": "750", 
            "dfs.encryption.key.provider.uri": "", 
            "dfs.replication.max": "50", 
            "dfs.namenode.checkpoint.period": "21600",
            "dfs.namenode.avoid.write.stale.datanode": "true",
            "dfs.namenode.startup.delay.block.deletion.sec": "3600",
            "dfs.namenode.checkpoint.txns": "1000000",
            "dfs.content-summary.limit": "5000",
            "dfs.support.append": "true",
            "dfs.datanode.address": "0.0.0.0:50010",
            "dfs.cluster.administrators": " hdfs",
            "dfs.namenode.audit.log.async": "true",
            "dfs.datanode.balance.bandwidthPerSec": "6250000",
            "dfs.namenode.safemode.threshold-pct": "1",
            "dfs.namenode.checkpoint.edits.dir": "${dfs.namenode.checkpoint.dir}",
            "dfs.namenode.rpc-address": "c6401.ambari.apache.org:8020",
            "dfs.permissions.enabled": "true",
            "dfs.client.read.shortcircuit": "true",
            "dfs.https.port": "50470",
            "dfs.namenode.https-address": "c6401.ambari.apache.org:50470",
            "nfs.file.dump.dir": "/tmp/.hdfs-nfs",
            "dfs.blocksize": "134217728",
            "dfs.blockreport.initialDelay": "120",
            "dfs.journalnode.edits.dir": "/hadoop/hdfs/journalnode",
            "dfs.namenode.fslock.fair": "false",
            "dfs.datanode.max.transfer.threads": "4096",
            "dfs.heartbeat.interval": "3",
            "dfs.replication": "3",
            "dfs.namenode.handler.count": "50",
            "dfs.namenode.checkpoint.dir": "/grid/0/hadoop/hdfs/namesecondary",
            "fs.permissions.umask-mode": "022",
            "dfs.namenode.stale.datanode.interval": "30000",
            "dfs.datanode.ipc.address": "0.0.0.0:8010",
            "dfs.datanode.failed.volumes.tolerated": "0",
            "dfs.datanode.data.dir": "/grid/0/hadoop/hdfs/data",
            "dfs.namenode.http-address": "c6401.ambari.apache.org:50070",
            "dfs.webhdfs.enabled": "true",
            "dfs.encrypt.data.transfer.cipher.suites": "AES/CTR/NoPadding",
            "dfs.namenode.accesstime.precision": "0",
            "dfs.datanode.https.address": "0.0.0.0:50475",
            "dfs.namenode.write.stale.datanode.ratio": "1.0f",
            "dfs.namenode.secondary.http-address": "c6401.ambari.apache.org:50090",
            "nfs.exports.allowed.hosts": "* rw",
            "dfs.datanode.http.address": "0.0.0.0:50075",
            "dfs.datanode.du.reserved": "33011188224",
            "dfs.client.read.shortcircuit.streams.cache.size": "4096",
            "dfs.http.policy": "HTTP_ONLY",
            "dfs.block.access.token.enable": "true",
            "dfs.client.retry.policy.enabled": "false",
            "dfs.namenode.name.dir.restore": "true",
            "dfs.permissions.superusergroup": "hdfs",
            "dfs.journalnode.https-address": "0.0.0.0:8481",
            "dfs.journalnode.http-address": "0.0.0.0:8480",
            "dfs.domain.socket.path": "/var/lib/hadoop-hdfs/dn_socket",
            "dfs.namenode.avoid.read.stale.datanode": "true",
            "dfs.hosts.exclude": "/etc/hadoop/conf/dfs.exclude",
            "dfs.datanode.data.dir.perm": "750",
            "dfs.encryption.key.provider.uri": "",
            "dfs.replication.max": "50",
             "dfs.namenode.name.dir": "/grid/0/hadoop/hdfs/namenode"
        }, 
        },
         "ranger-tagsync-site": {
             "ranger.tagsync.dest.ranger.ssl.config.filename": "{{stack_root}}/current/ranger-tagsync/conf/ranger-policymgr-ssl.xml",
             "ranger.tagsync.source.atlasrest.username": "",
            "ranger.tagsync.logdir": "/var/log/ranger/tagsync", 
            "ranger.tagsync.source.atlasrest.download.interval.millis": "", 
            "ranger.tagsync.keystore.filename": "/usr/hdp/current/ranger-tagsync/conf/rangertagsync.jceks", 
            "ranger.tagsync.source.file.check.interval.millis": "", 
            "ranger.tagsync.source.atlasrest.endpoint": "", 
            "ranger.tagsync.dest.ranger.username": "rangertagsync", 
            "ranger.tagsync.dest.ranger.endpoint": "{{ranger_external_url}}", 
            "ranger.tagsync.kerberos.principal": "", 
            "ranger.tagsync.kerberos.keytab": "", 
            "ranger.tagsync.source.atlas": "false", 
            "ranger.tagsync.source.atlasrest": "false", 
            "ranger.tagsync.source.file": "false", 
            "ranger.tagsync.logdir": "/var/log/ranger/tagsync",
            "ranger.tagsync.source.atlasrest.download.interval.millis": "",
            "ranger.tagsync.keystore.filename": "/usr/hdp/current/ranger-tagsync/conf/rangertagsync.jceks",
            "ranger.tagsync.source.file.check.interval.millis": "",
            "ranger.tagsync.source.atlasrest.endpoint": "",
            "ranger.tagsync.dest.ranger.username": "rangertagsync",
            "ranger.tagsync.dest.ranger.endpoint": "{{ranger_external_url}}",
            "ranger.tagsync.kerberos.principal": "",
            "ranger.tagsync.kerberos.keytab": "",
            "ranger.tagsync.source.atlas": "false",
            "ranger.tagsync.source.atlasrest": "false",
            "ranger.tagsync.source.file": "false",
             "ranger.tagsync.source.file.filename": ""
        }, 
        },
         "zoo.cfg": {
            "clientPort": "2181", 
            "autopurge.purgeInterval": "24", 
            "syncLimit": "5", 
            "dataDir": "/grid/0/hadoop/zookeeper", 
            "initLimit": "10", 
            "tickTime": "2000", 
            "clientPort": "2181",
            "autopurge.purgeInterval": "24",
            "syncLimit": "5",
            "dataDir": "/grid/0/hadoop/zookeeper",
            "initLimit": "10",
            "tickTime": "2000",
             "autopurge.snapRetainCount": "30"
        }, 
        },
         "hadoop-policy": {
            "security.job.client.protocol.acl": "*", 
            "security.job.task.protocol.acl": "*", 
            "security.datanode.protocol.acl": "*", 
            "security.namenode.protocol.acl": "*", 
            "security.client.datanode.protocol.acl": "*", 
            "security.inter.tracker.protocol.acl": "*", 
            "security.refresh.usertogroups.mappings.protocol.acl": "hadoop", 
            "security.client.protocol.acl": "*", 
            "security.refresh.policy.protocol.acl": "hadoop", 
            "security.admin.operations.protocol.acl": "hadoop", 
            "security.job.client.protocol.acl": "*",
            "security.job.task.protocol.acl": "*",
            "security.datanode.protocol.acl": "*",
            "security.namenode.protocol.acl": "*",
            "security.client.datanode.protocol.acl": "*",
            "security.inter.tracker.protocol.acl": "*",
            "security.refresh.usertogroups.mappings.protocol.acl": "hadoop",
            "security.client.protocol.acl": "*",
            "security.refresh.policy.protocol.acl": "hadoop",
            "security.admin.operations.protocol.acl": "hadoop",
             "security.inter.datanode.protocol.acl": "*"
        }, 
        },
         "hdfs-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#  http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing,\n# software distributed under the License is distributed on an\n# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n# KIND, either express or implied.  See the License for the\n# specific language governing permissions and limitations\n# under the License.\n#\n\n\n# Define some default values that can be overridden by system properties\n# To change daemon root logger use hadoop_root_logger in hadoop-env\nhadoop.root.logger=INFO,console\nhadoop.log.dir=.\nhadoop.log.file=hadoop.log\n\n\n# Define the root logger to the system property \"hadoop.root.logger\".\nlog4j.rootLogger=${hadoop.root.logger}, EventCounter\n\n# Logging Threshold\nlog4j.threshhold=ALL\n\n#\n# Daily Rolling File Appender\n#\n\nlog4j.appender.DRFA=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.DRFA.File=${hadoop.log.dir}/${hadoop.log.file}\n\n# Rollver at midnight\nlog4j.appender.DRFA.DatePattern=.yyyy-MM-dd\n\n# 30-day backup\n#log4j.appender.DRFA.MaxBackupIndex=30\nlog4j.appender.DRFA.layout=org.apache.log4j.PatternLayout\n\n# Pattern format: Date LogLevel LoggerName LogMessage\nlog4j.appender.DRFA.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\n# Debugging Pattern format\n#log4j.appender.DRFA.layout.ConversionPattern=%d{ISO8601} %-5p %c{2} (%F:%M(%L)) - %m%n\n\n\n#\n# console\n# Add \"console\" to rootlogger above if you want to use this\n#\n\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.target=System.err\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{yy/MM/dd HH:mm:ss} %p %c{2}: %m%n\n\n#\n# TaskLog Appender\n#\n\n#Default values\nhadoop.tasklog.taskid=null\nhadoop.tasklog.iscleanup=false\nhadoop.tasklog.noKeepSplits=4\nhadoop.tasklog.totalLogFileSize=100\nhadoop.tasklog.purgeLogSplits=true\nhadoop.tasklog.logsRetainHours=12\n\nlog4j.appender.TLA=org.apache.hadoop.mapred.TaskLogAppender\nlog4j.appender.TLA.taskId=${hadoop.tasklog.taskid}\nlog4j.appender.TLA.isCleanup=${hadoop.tasklog.iscleanup}\nlog4j.appender.TLA.totalLogFileSize=${hadoop.tasklog.totalLogFileSize}\n\nlog4j.appender.TLA.layout=org.apache.log4j.PatternLayout\nlog4j.appender.TLA.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\n\n#\n#Security audit appender\n#\nhadoop.security.logger=INFO,console\nhadoop.security.log.maxfilesize=256MB\nhadoop.security.log.maxbackupindex=20\nlog4j.category.SecurityLogger=${hadoop.security.logger}\nhadoop.security.log.file=SecurityAuth.audit\nlog4j.appender.DRFAS=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.DRFAS.File=${hadoop.log.dir}/${hadoop.security.log.file}\nlog4j.appender.DRFAS.layout=org.apache.log4j.PatternLayout\nlog4j.appender.DRFAS.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\nlog4j.appender.DRFAS.DatePattern=.yyyy-MM-dd\n\nlog4j.appender.RFAS=org.apache.log4j.RollingFileAppender\nlog4j.appender.RFAS.File=${hadoop.log.dir}/${hadoop.security.log.file}\nlog4j.appender.RFAS.layout=org.apache.log4j.PatternLayout\nlog4j.appender.RFAS.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\nlog4j.appender.RFAS.MaxFileSize=${hadoop.security.log.maxfilesize}\nlog4j.appender.RFAS.MaxBackupIndex=${hadoop.security.log.maxbackupindex}\n\n#\n# hdfs audit logging\n#\nhdfs.audit.logger=INFO,console\nlog4j.logger.org.apache.hadoop.hdfs.server.namenode.FSNamesystem.audit=${hdfs.audit.logger}\nlog4j.additivity.org.apache.hadoop.hdfs.server.namenode.FSNamesystem.audit=false\nlog4j.appender.DRFAAUDIT=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.DRFAAUDIT.File=${hadoop.log.dir}/hdfs-audit.log\nlog4j.appender.DRFAAUDIT.layout=org.apache.log4j.PatternLayout\nlog4j.appender.DRFAAUDIT.layout.ConversionPattern=%d{ISO8601} %p %c{2}: %m%n\nlog4j.appender.DRFAAUDIT.DatePattern=.yyyy-MM-dd\n\n#\n# NameNode metrics logging.\n# The default is to retain two namenode-metrics.log files up to 64MB each.\n#\nnamenode.metrics.logger=INFO,NullAppender\nlog4j.logger.NameNodeMetricsLog=${namenode.metrics.logger}\nlog4j.additivity.NameNodeMetricsLog=false\nlog4j.appender.NNMETRICSRFA=org.apache.log4j.RollingFileAppender\nlog4j.appender.NNMETRICSRFA.File=${hadoop.log.dir}/namenode-metrics.log\nlog4j.appender.NNMETRICSRFA.layout=org.apache.log4j.PatternLayout\nlog4j.appender.NNMETRICSRFA.layout.ConversionPattern=%d{ISO8601} %m%n\nlog4j.appender.NNMETRICSRFA.MaxBackupIndex=1\nlog4j.appender.NNMETRICSRFA.MaxFileSize=64MB\n\n#\n# mapred audit logging\n#\nmapred.audit.logger=INFO,console\nlog4j.logger.org.apache.hadoop.mapred.AuditLogger=${mapred.audit.logger}\nlog4j.additivity.org.apache.hadoop.mapred.AuditLogger=false\nlog4j.appender.MRAUDIT=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.MRAUDIT.File=${hadoop.log.dir}/mapred-audit.log\nlog4j.appender.MRAUDIT.layout=org.apache.log4j.PatternLayout\nlog4j.appender.MRAUDIT.layout.ConversionPattern=%d{ISO8601} %p %c{2}: %m%n\nlog4j.appender.MRAUDIT.DatePattern=.yyyy-MM-dd\n\n#\n# Rolling File Appender\n#\n\nlog4j.appender.RFA=org.apache.log4j.RollingFileAppender\nlog4j.appender.RFA.File=${hadoop.log.dir}/${hadoop.log.file}\n\n# Logfile size and and 30-day backups\nlog4j.appender.RFA.MaxFileSize=256MB\nlog4j.appender.RFA.MaxBackupIndex=10\n\nlog4j.appender.RFA.layout=org.apache.log4j.PatternLayout\nlog4j.appender.RFA.layout.ConversionPattern=%d{ISO8601} %-5p %c{2} - %m%n\nlog4j.appender.RFA.layout.ConversionPattern=%d{ISO8601} %-5p %c{2} (%F:%M(%L)) - %m%n\n\n\n# Custom Logging levels\n\nhadoop.metrics.log.level=INFO\n#log4j.logger.org.apache.hadoop.mapred.JobTracker=DEBUG\n#log4j.logger.org.apache.hadoop.mapred.TaskTracker=DEBUG\n#log4j.logger.org.apache.hadoop.fs.FSNamesystem=DEBUG\nlog4j.logger.org.apache.hadoop.metrics2=${hadoop.metrics.log.level}\n\n# Jets3t library\nlog4j.logger.org.jets3t.service.impl.rest.httpclient.RestS3Service=ERROR\n\n#\n# Null Appender\n# Trap security logger on the hadoop client side\n#\nlog4j.appender.NullAppender=org.apache.log4j.varia.NullAppender\n\n#\n# Event Counter Appender\n# Sends counts of logging messages at different severity levels to Hadoop Metrics.\n#\nlog4j.appender.EventCounter=org.apache.hadoop.log.metrics.EventCounter\n\n# Removes \"deprecated\" messages\nlog4j.logger.org.apache.hadoop.conf.Configuration.deprecation=WARN\n\n#\n# HDFS block state change log from block manager\n#\n# Uncomment the following to suppress normal block state change\n# messages from BlockManager in NameNode.\n#log4j.logger.BlockStateChange=WARN"
        }, 
        },
         "ranger-hdfs-plugin-properties": {
            "hadoop.rpc.protection": "authentication", 
            "ranger-hdfs-plugin-enabled": "No", 
            "REPOSITORY_CONFIG_USERNAME": "hadoop", 
            "policy_user": "ambari-qa", 
            "common.name.for.certificate": "", 
            "hadoop.rpc.protection": "authentication",
            "ranger-hdfs-plugin-enabled": "No",
            "REPOSITORY_CONFIG_USERNAME": "hadoop",
            "policy_user": "ambari-qa",
            "common.name.for.certificate": "",
             "REPOSITORY_CONFIG_PASSWORD": "hadoop"
        }, 
        },
         "core-site": {
            "hadoop.proxyuser.root.hosts": "c6401.ambari.apache.org", 
            "io.serializations": "org.apache.hadoop.io.serializer.WritableSerialization", 
            "fs.trash.interval": "360", 
            "ha.failover-controller.active-standby-elector.zk.op.retries": "120", 
            "hadoop.http.authentication.simple.anonymous.allowed": "true", 
            "hadoop.security.authentication": "simple", 
            "hadoop.proxyuser.root.groups": "*", 
            "ipc.client.connection.maxidletime": "30000", 
            "hadoop.security.key.provider.path": "", 
            "mapreduce.jobtracker.webinterface.trusted": "false", 
            "hadoop.security.authorization": "false", 
            "net.topology.script.file.name": "/etc/hadoop/conf/topology_script.py", 
            "ipc.server.tcpnodelay": "true", 
            "ipc.client.connect.max.retries": "50", 
            "hadoop.security.auth_to_local": "DEFAULT", 
            "io.file.buffer.size": "131072", 
            "hadoop.proxyuser.hdfs.hosts": "*", 
            "hadoop.proxyuser.hdfs.groups": "*", 
            "ipc.client.idlethreshold": "8000", 
            "fs.defaultFS": "hdfs://c6401.ambari.apache.org:8020", 
            "hadoop.proxyuser.root.hosts": "c6401.ambari.apache.org",
            "io.serializations": "org.apache.hadoop.io.serializer.WritableSerialization",
            "fs.trash.interval": "360",
            "ha.failover-controller.active-standby-elector.zk.op.retries": "120",
            "hadoop.http.authentication.simple.anonymous.allowed": "true",
            "hadoop.security.authentication": "simple",
            "hadoop.proxyuser.root.groups": "*",
            "ipc.client.connection.maxidletime": "30000",
            "hadoop.security.key.provider.path": "",
            "mapreduce.jobtracker.webinterface.trusted": "false",
            "hadoop.security.authorization": "false",
            "net.topology.script.file.name": "/etc/hadoop/conf/topology_script.py",
            "ipc.server.tcpnodelay": "true",
            "ipc.client.connect.max.retries": "50",
            "hadoop.security.auth_to_local": "DEFAULT",
            "io.file.buffer.size": "131072",
            "hadoop.proxyuser.hdfs.hosts": "*",
            "hadoop.proxyuser.hdfs.groups": "*",
            "ipc.client.idlethreshold": "8000",
            "fs.defaultFS": "hdfs://c6401.ambari.apache.org:8020",
             "io.compression.codecs": "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec,org.apache.hadoop.io.compress.SnappyCodec"
        }, 
        },
         "hadoop-env": {
            "keyserver_port": "", 
            "proxyuser_group": "users", 
            "hdfs_user_nproc_limit": "65536", 
            "hdfs_log_dir_prefix": "/var/log/hadoop", 
            "hdfs_user_nofile_limit": "128000", 
            "hdfs_user": "hdfs", 
            "keyserver_host": " ", 
            "namenode_opt_maxnewsize": "128m", 
            "namenode_opt_maxpermsize": "256m", 
            "content": "\n# Set Hadoop-specific environment variables here.\n\n# The only required environment variable is JAVA_HOME.  All others are\n# optional.  When running a distributed configuration it is best to\n# set JAVA_HOME in this file, so that it is correctly defined on\n# remote nodes.\n\n# The java implementation to use.  Required.\nexport JAVA_HOME={{java_home}}\nexport HADOOP_HOME_WARN_SUPPRESS=1\n\n# Hadoop home directory\nexport HADOOP_HOME=${HADOOP_HOME:-{{hadoop_home}}}\n\n# Hadoop Configuration Directory\n\n{# this is different for HDP1 #}\n# Path to jsvc required by secure HDP 2.0 datanode\nexport JSVC_HOME={{jsvc_path}}\n\n\n# The maximum amount of heap to use, in MB. Default is 1000.\nexport HADOOP_HEAPSIZE=\"{{hadoop_heapsize}}\"\n\nexport HADOOP_NAMENODE_INIT_HEAPSIZE=\"-Xms{{namenode_heapsize}}\"\n\n# Extra Java runtime options.  Empty by default.\nexport HADOOP_OPTS=\"-Djava.net.preferIPv4Stack=true ${HADOOP_OPTS}\"\n\n# Command specific options appended to HADOOP_OPTS when specified\nHADOOP_JOBTRACKER_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{jtnode_opt_newsize}} -XX:MaxNewSize={{jtnode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xmx{{jtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dmapred.audit.logger=INFO,MRAUDIT -Dhadoop.mapreduce.jobsummary.logger=INFO,JSA ${HADOOP_JOBTRACKER_OPTS}\"\n\nHADOOP_TASKTRACKER_OPTS=\"-server -Xmx{{ttnode_heapsize}} -Dhadoop.security.logger=ERROR,console -Dmapred.audit.logger=ERROR,console ${HADOOP_TASKTRACKER_OPTS}\"\n\n{% if java_version < 8 %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -XX:PermSize={{namenode_opt_permsize}} -XX:MaxPermSize={{namenode_opt_maxpermsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -XX:PermSize=128m -XX:MaxPermSize=256m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m -XX:MaxPermSize=512m $HADOOP_CLIENT_OPTS\"\n\n{% else %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m $HADOOP_CLIENT_OPTS\"\n{% endif %}\n\nHADOOP_NFS3_OPTS=\"-Xmx{{nfsgateway_heapsize}}m -Dhadoop.security.logger=ERROR,DRFAS ${HADOOP_NFS3_OPTS}\"\nHADOOP_BALANCER_OPTS=\"-server -Xmx{{hadoop_heapsize}}m ${HADOOP_BALANCER_OPTS}\"\n\n\n# On secure datanodes, user to run the datanode as after dropping privileges\nexport HADOOP_SECURE_DN_USER=${HADOOP_SECURE_DN_USER:-{{hadoop_secure_dn_user}}}\n\n# Extra ssh options.  Empty by default.\nexport HADOOP_SSH_OPTS=\"-o ConnectTimeout=5 -o SendEnv=HADOOP_CONF_DIR\"\n\n# Where log files are stored.  $HADOOP_HOME/logs by default.\nexport HADOOP_LOG_DIR={{hdfs_log_dir_prefix}}/$USER\n\n# History server logs\nexport HADOOP_MAPRED_LOG_DIR={{mapred_log_dir_prefix}}/$USER\n\n# Where log files are stored in the secure data environment.\nexport HADOOP_SECURE_DN_LOG_DIR={{hdfs_log_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# File naming remote slave hosts.  $HADOOP_HOME/conf/slaves by default.\n# export HADOOP_SLAVES=${HADOOP_HOME}/conf/slaves\n\n# host:path where hadoop code should be rsync'd from.  Unset by default.\n# export HADOOP_MASTER=master:/home/$USER/src/hadoop\n\n# Seconds to sleep between slave commands.  Unset by default.  This\n# can be useful in large clusters, where, e.g., slave rsyncs can\n# otherwise arrive faster than the master can service them.\n# export HADOOP_SLAVE_SLEEP=0.1\n\n# The directory where pid files are stored. /tmp by default.\nexport HADOOP_PID_DIR={{hadoop_pid_dir_prefix}}/$USER\nexport HADOOP_SECURE_DN_PID_DIR={{hadoop_pid_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# History server pid\nexport HADOOP_MAPRED_PID_DIR={{mapred_pid_dir_prefix}}/$USER\n\nYARN_RESOURCEMANAGER_OPTS=\"-Dyarn.server.resourcemanager.appsummary.logger=INFO,RMSUMMARY\"\n\n# A string representing this instance of hadoop. $USER by default.\nexport HADOOP_IDENT_STRING=$USER\n\n# The scheduling priority for daemon processes.  See 'man nice'.\n\n# export HADOOP_NICENESS=10\n\n# Add database libraries\nJAVA_JDBC_LIBS=\"\"\nif [ -d \"/usr/share/java\" ]; then\n  for jarFile in `ls /usr/share/java | grep -E \"(mysql|ojdbc|postgresql|sqljdbc)\" 2>/dev/null`\n  do\n    JAVA_JDBC_LIBS=${JAVA_JDBC_LIBS}:$jarFile\n  done\nfi\n\n# Add libraries to the hadoop classpath - some may not need a colon as they already include it\nexport HADOOP_CLASSPATH=${HADOOP_CLASSPATH}${JAVA_JDBC_LIBS}\n\n# Setting path to hdfs command line\nexport HADOOP_LIBEXEC_DIR={{hadoop_libexec_dir}}\n\n# Mostly required for hadoop 2.0\nexport JAVA_LIBRARY_PATH=${JAVA_LIBRARY_PATH}\n\nexport HADOOP_OPTS=\"-Dhdp.version=$HDP_VERSION $HADOOP_OPTS\"\n\n{% if is_datanode_max_locked_memory_set %}\n# Fix temporary bug, when ulimit from conf files is not picked up, without full relogin. \n# Makes sense to fix only when runing DN as root \nif [ \"$command\" == \"datanode\" ] && [ \"$EUID\" -eq 0 ] && [ -n \"$HADOOP_SECURE_DN_USER\" ]; then\n  ulimit -l {{datanode_max_locked_memory}}\nfi\n{% endif %}", 
            "namenode_heapsize": "1024m", 
            "namenode_opt_newsize": "128m", 
            "nfsgateway_heapsize": "1024", 
            "dtnode_heapsize": "1024m", 
            "hadoop_root_logger": "INFO,RFA", 
            "hadoop_heapsize": "1024", 
            "hadoop_pid_dir_prefix": "/var/run/hadoop", 
            "namenode_opt_permsize": "128m", 
            "keyserver_port": "",
            "proxyuser_group": "users",
            "hdfs_user_nproc_limit": "65536",
            "hdfs_log_dir_prefix": "/var/log/hadoop",
            "hdfs_user_nofile_limit": "128000",
            "hdfs_user": "hdfs",
            "keyserver_host": " ",
            "namenode_opt_maxnewsize": "128m",
            "namenode_opt_maxpermsize": "256m",
            "content": "\n# Set Hadoop-specific environment variables here.\n\n# The only required environment variable is JAVA_HOME.  All others are\n# optional.  When running a distributed configuration it is best to\n# set JAVA_HOME in this file, so that it is correctly defined on\n# remote nodes.\n\n# The java implementation to use.  Required.\nexport JAVA_HOME={{java_home}}\nexport HADOOP_HOME_WARN_SUPPRESS=1\n\n# Hadoop home directory\nexport HADOOP_HOME=${HADOOP_HOME:-{{hadoop_home}}}\n\n# Hadoop Configuration Directory\n\n{# this is different for HDP1 #}\n# Path to jsvc required by secure HDP 2.0 datanode\nexport JSVC_HOME={{jsvc_path}}\n\n\n# The maximum amount of heap to use, in MB. Default is 1000.\nexport HADOOP_HEAPSIZE=\"{{hadoop_heapsize}}\"\n\nexport HADOOP_NAMENODE_INIT_HEAPSIZE=\"-Xms{{namenode_heapsize}}\"\n\n# Extra Java runtime options.  Empty by default.\nexport HADOOP_OPTS=\"-Djava.net.preferIPv4Stack=true ${HADOOP_OPTS}\"\n\n# Command specific options appended to HADOOP_OPTS when specified\nHADOOP_JOBTRACKER_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{jtnode_opt_newsize}} -XX:MaxNewSize={{jtnode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xmx{{jtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dmapred.audit.logger=INFO,MRAUDIT -Dhadoop.mapreduce.jobsummary.logger=INFO,JSA ${HADOOP_JOBTRACKER_OPTS}\"\n\nHADOOP_TASKTRACKER_OPTS=\"-server -Xmx{{ttnode_heapsize}} -Dhadoop.security.logger=ERROR,console -Dmapred.audit.logger=ERROR,console ${HADOOP_TASKTRACKER_OPTS}\"\n\n{% if java_version < 8 %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -XX:PermSize={{namenode_opt_permsize}} -XX:MaxPermSize={{namenode_opt_maxpermsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -XX:PermSize=128m -XX:MaxPermSize=256m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m -XX:MaxPermSize=512m $HADOOP_CLIENT_OPTS\"\n\n{% else %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m $HADOOP_CLIENT_OPTS\"\n{% endif %}\n\nHADOOP_NFS3_OPTS=\"-Xmx{{nfsgateway_heapsize}}m -Dhadoop.security.logger=ERROR,DRFAS ${HADOOP_NFS3_OPTS}\"\nHADOOP_BALANCER_OPTS=\"-server -Xmx{{hadoop_heapsize}}m ${HADOOP_BALANCER_OPTS}\"\n\n\n# On secure datanodes, user to run the datanode as after dropping privileges\nexport HADOOP_SECURE_DN_USER=${HADOOP_SECURE_DN_USER:-{{hadoop_secure_dn_user}}}\n\n# Extra ssh options.  Empty by default.\nexport HADOOP_SSH_OPTS=\"-o ConnectTimeout=5 -o SendEnv=HADOOP_CONF_DIR\"\n\n# Where log files are stored.  $HADOOP_HOME/logs by default.\nexport HADOOP_LOG_DIR={{hdfs_log_dir_prefix}}/$USER\n\n# History server logs\nexport HADOOP_MAPRED_LOG_DIR={{mapred_log_dir_prefix}}/$USER\n\n# Where log files are stored in the secure data environment.\nexport HADOOP_SECURE_DN_LOG_DIR={{hdfs_log_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# File naming remote slave hosts.  $HADOOP_HOME/conf/slaves by default.\n# export HADOOP_SLAVES=${HADOOP_HOME}/conf/slaves\n\n# host:path where hadoop code should be rsync'd from.  Unset by default.\n# export HADOOP_MASTER=master:/home/$USER/src/hadoop\n\n# Seconds to sleep between slave commands.  Unset by default.  This\n# can be useful in large clusters, where, e.g., slave rsyncs can\n# otherwise arrive faster than the master can service them.\n# export HADOOP_SLAVE_SLEEP=0.1\n\n# The directory where pid files are stored. /tmp by default.\nexport HADOOP_PID_DIR={{hadoop_pid_dir_prefix}}/$USER\nexport HADOOP_SECURE_DN_PID_DIR={{hadoop_pid_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# History server pid\nexport HADOOP_MAPRED_PID_DIR={{mapred_pid_dir_prefix}}/$USER\n\nYARN_RESOURCEMANAGER_OPTS=\"-Dyarn.server.resourcemanager.appsummary.logger=INFO,RMSUMMARY\"\n\n# A string representing this instance of hadoop. $USER by default.\nexport HADOOP_IDENT_STRING=$USER\n\n# The scheduling priority for daemon processes.  See 'man nice'.\n\n# export HADOOP_NICENESS=10\n\n# Add database libraries\nJAVA_JDBC_LIBS=\"\"\nif [ -d \"/usr/share/java\" ]; then\n  for jarFile in `ls /usr/share/java | grep -E \"(mysql|ojdbc|postgresql|sqljdbc)\" 2>/dev/null`\n  do\n    JAVA_JDBC_LIBS=${JAVA_JDBC_LIBS}:$jarFile\n  done\nfi\n\n# Add libraries to the hadoop classpath - some may not need a colon as they already include it\nexport HADOOP_CLASSPATH=${HADOOP_CLASSPATH}${JAVA_JDBC_LIBS}\n\n# Setting path to hdfs command line\nexport HADOOP_LIBEXEC_DIR={{hadoop_libexec_dir}}\n\n# Mostly required for hadoop 2.0\nexport JAVA_LIBRARY_PATH=${JAVA_LIBRARY_PATH}\n\nexport HADOOP_OPTS=\"-Dhdp.version=$HDP_VERSION $HADOOP_OPTS\"\n\n{% if is_datanode_max_locked_memory_set %}\n# Fix temporary bug, when ulimit from conf files is not picked up, without full relogin. \n# Makes sense to fix only when runing DN as root \nif [ \"$command\" == \"datanode\" ] && [ \"$EUID\" -eq 0 ] && [ -n \"$HADOOP_SECURE_DN_USER\" ]; then\n  ulimit -l {{datanode_max_locked_memory}}\nfi\n{% endif %}",
            "namenode_heapsize": "1024m",
            "namenode_opt_newsize": "128m",
            "nfsgateway_heapsize": "1024",
            "dtnode_heapsize": "1024m",
            "hadoop_root_logger": "INFO,RFA",
            "hadoop_heapsize": "1024",
            "hadoop_pid_dir_prefix": "/var/run/hadoop",
            "namenode_opt_permsize": "128m",
             "hdfs_tmp_dir": "/tmp"
        }, 
        },
         "zookeeper-log4j": {
             "content": "\n#\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#   http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing,\n# software distributed under the License is distributed on an\n# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n# KIND, either express or implied.  See the License for the\n# specific language governing permissions and limitations\n# under the License.\n#\n#\n#\n\n#\n# ZooKeeper Logging Configuration\n#\n\n# DEFAULT: console appender only\nlog4j.rootLogger=INFO, CONSOLE\n\n# Example with rolling log file\n#log4j.rootLogger=DEBUG, CONSOLE, ROLLINGFILE\n\n# Example with rolling log file and tracing\n#log4j.rootLogger=TRACE, CONSOLE, ROLLINGFILE, TRACEFILE\n\n#\n# Log INFO level and above messages to the console\n#\nlog4j.appender.CONSOLE=org.apache.log4j.ConsoleAppender\nlog4j.appender.CONSOLE.Threshold=INFO\nlog4j.appender.CONSOLE.layout=org.apache.log4j.PatternLayout\nlog4j.appender.CONSOLE.layout.ConversionPattern=%d{ISO8601} - %-5p [%t:%C{1}@%L] - %m%n\n\n#\n# Add ROLLINGFILE to rootLogger to get log file output\n#    Log DEBUG level and above messages to a log file\nlog4j.appender.ROLLINGFILE=org.apache.log4j.RollingFileAppender\nlog4j.appender.ROLLINGFILE.Threshold=DEBUG\nlog4j.appender.ROLLINGFILE.File=zookeeper.log\n\n# Max log file size of 10MB\nlog4j.appender.ROLLINGFILE.MaxFileSize=10MB\n# uncomment the next line to limit number of backup files\n#log4j.appender.ROLLINGFILE.MaxBackupIndex=10\n\nlog4j.appender.ROLLINGFILE.layout=org.apache.log4j.PatternLayout\nlog4j.appender.ROLLINGFILE.layout.ConversionPattern=%d{ISO8601} - %-5p [%t:%C{1}@%L] - %m%n\n\n\n#\n# Add TRACEFILE to rootLogger to get log file output\n#    Log DEBUG level and above messages to a log file\nlog4j.appender.TRACEFILE=org.apache.log4j.FileAppender\nlog4j.appender.TRACEFILE.Threshold=TRACE\nlog4j.appender.TRACEFILE.File=zookeeper_trace.log\n\nlog4j.appender.TRACEFILE.layout=org.apache.log4j.PatternLayout\n### Notice we are including log4j's NDC here (%x)\nlog4j.appender.TRACEFILE.layout.ConversionPattern=%d{ISO8601} - %-5p [%t:%C{1}@%L][%x] - %m%n"
        }, 
        },
         "ssl-server": {
            "ssl.server.keystore.location": "/etc/security/serverKeys/keystore.jks", 
            "ssl.server.keystore.keypassword": "bigdata", 
            "ssl.server.truststore.location": "/etc/security/serverKeys/all.jks", 
            "ssl.server.keystore.password": "bigdata", 
            "ssl.server.truststore.password": "bigdata", 
            "ssl.server.truststore.type": "jks", 
            "ssl.server.keystore.type": "jks", 
            "ssl.server.keystore.location": "/etc/security/serverKeys/keystore.jks",
            "ssl.server.keystore.keypassword": "bigdata",
            "ssl.server.truststore.location": "/etc/security/serverKeys/all.jks",
            "ssl.server.keystore.password": "bigdata",
            "ssl.server.truststore.password": "bigdata",
            "ssl.server.truststore.type": "jks",
            "ssl.server.keystore.type": "jks",
             "ssl.server.truststore.reload.interval": "10000"
        }, 
        "ranger-site": {}, 
        },
        "ranger-site": {},
         "admin-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#      http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n#\n\n\nlog4j.rootLogger = warn,xa_log_appender\n\n\n# xa_logger\nlog4j.appender.xa_log_appender=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.xa_log_appender.file=${logdir}/xa_portal.log\nlog4j.appender.xa_log_appender.datePattern='.'yyyy-MM-dd\nlog4j.appender.xa_log_appender.append=true\nlog4j.appender.xa_log_appender.layout=org.apache.log4j.PatternLayout\nlog4j.appender.xa_log_appender.layout.ConversionPattern=%d [%t] %-5p %C{6} (%F:%L) - %m%n\n# xa_log_appender : category and additivity\nlog4j.category.org.springframework=warn,xa_log_appender\nlog4j.additivity.org.springframework=false\n\nlog4j.category.org.apache.ranger=info,xa_log_appender\nlog4j.additivity.org.apache.ranger=false\n\nlog4j.category.xa=info,xa_log_appender\nlog4j.additivity.xa=false\n\n# perf_logger\nlog4j.appender.perf_appender=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.perf_appender.file=${logdir}/ranger_admin_perf.log\nlog4j.appender.perf_appender.datePattern='.'yyyy-MM-dd\nlog4j.appender.perf_appender.append=true\nlog4j.appender.perf_appender.layout=org.apache.log4j.PatternLayout\nlog4j.appender.perf_appender.layout.ConversionPattern=%d [%t] %m%n\n\n\n# sql_appender\nlog4j.appender.sql_appender=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.sql_appender.file=${logdir}/xa_portal_sql.log\nlog4j.appender.sql_appender.datePattern='.'yyyy-MM-dd\nlog4j.appender.sql_appender.append=true\nlog4j.appender.sql_appender.layout=org.apache.log4j.PatternLayout\nlog4j.appender.sql_appender.layout.ConversionPattern=%d [%t] %-5p %C{6} (%F:%L) - %m%n\n\n# sql_appender : category and additivity\nlog4j.category.org.hibernate.SQL=warn,sql_appender\nlog4j.additivity.org.hibernate.SQL=false\n\nlog4j.category.jdbc.sqlonly=fatal,sql_appender\nlog4j.additivity.jdbc.sqlonly=false\n\nlog4j.category.jdbc.sqltiming=warn,sql_appender\nlog4j.additivity.jdbc.sqltiming=false\n\nlog4j.category.jdbc.audit=fatal,sql_appender\nlog4j.additivity.jdbc.audit=false\n\nlog4j.category.jdbc.resultset=fatal,sql_appender\nlog4j.additivity.jdbc.resultset=false\n\nlog4j.category.jdbc.connection=fatal,sql_appender\nlog4j.additivity.jdbc.connection=false"
        }, 
        },
         "tagsync-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#      http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n#\n\n\nlog4j.rootLogger = info,logFile\n\n# logFile\nlog4j.appender.logFile=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.logFile.file=${logdir}/tagsync.log\nlog4j.appender.logFile.datePattern='.'yyyy-MM-dd\nlog4j.appender.logFile.layout=org.apache.log4j.PatternLayout\nlog4j.appender.logFile.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %L %m%n\n\n# console\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.Target=System.out\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %L %m%n"
        }, 
        },
         "ranger-hdfs-security": {
            "ranger.plugin.hdfs.policy.source.impl": "org.apache.ranger.admin.client.RangerAdminRESTClient", 
            "ranger.plugin.hdfs.service.name": "{{repo_name}}", 
            "ranger.plugin.hdfs.policy.cache.dir": "/etc/ranger/{{repo_name}}/policycache", 
            "ranger.plugin.hdfs.policy.pollIntervalMs": "30000", 
            "ranger.plugin.hdfs.policy.rest.url": "{{policymgr_mgr_url}}", 
            "ranger.plugin.hdfs.policy.rest.ssl.config.file": "/etc/hadoop/conf/ranger-policymgr-ssl.xml", 
            "ranger.plugin.hdfs.policy.source.impl": "org.apache.ranger.admin.client.RangerAdminRESTClient",
            "ranger.plugin.hdfs.service.name": "{{repo_name}}",
            "ranger.plugin.hdfs.policy.cache.dir": "/etc/ranger/{{repo_name}}/policycache",
            "ranger.plugin.hdfs.policy.pollIntervalMs": "30000",
            "ranger.plugin.hdfs.policy.rest.url": "{{policymgr_mgr_url}}",
            "ranger.plugin.hdfs.policy.rest.ssl.config.file": "/etc/hadoop/conf/ranger-policymgr-ssl.xml",
             "xasecure.add-hadoop-authorization": "true"
        }, 
        "usersync-properties": {}, 
        },
        "usersync-properties": {},
         "zookeeper-env": {
            "zk_log_dir": "/var/log/zookeeper", 
            "content": "\nexport JAVA_HOME={{java64_home}}\nexport ZOOKEEPER_HOME={{zk_home}}\nexport ZOO_LOG_DIR={{zk_log_dir}}\nexport ZOOPIDFILE={{zk_pid_file}}\nexport SERVER_JVMFLAGS={{zk_server_heapsize}}\nexport JAVA=$JAVA_HOME/bin/java\nexport CLASSPATH=$CLASSPATH:/usr/share/zookeeper/*\n\n{% if security_enabled %}\nexport SERVER_JVMFLAGS=\"$SERVER_JVMFLAGS -Djava.security.auth.login.config={{zk_server_jaas_file}}\"\nexport CLIENT_JVMFLAGS=\"$CLIENT_JVMFLAGS -Djava.security.auth.login.config={{zk_client_jaas_file}}\"\n{% endif %}", 
            "zk_server_heapsize": "1024m", 
            "zk_pid_dir": "/var/run/zookeeper", 
            "zk_log_dir": "/var/log/zookeeper",
            "content": "\nexport JAVA_HOME={{java64_home}}\nexport ZOOKEEPER_HOME={{zk_home}}\nexport ZOO_LOG_DIR={{zk_log_dir}}\nexport ZOOPIDFILE={{zk_pid_file}}\nexport SERVER_JVMFLAGS={{zk_server_heapsize}}\nexport JAVA=$JAVA_HOME/bin/java\nexport CLASSPATH=$CLASSPATH:/usr/share/zookeeper/*\n\n{% if security_enabled %}\nexport SERVER_JVMFLAGS=\"$SERVER_JVMFLAGS -Djava.security.auth.login.config={{zk_server_jaas_file}}\"\nexport CLIENT_JVMFLAGS=\"$CLIENT_JVMFLAGS -Djava.security.auth.login.config={{zk_client_jaas_file}}\"\n{% endif %}",
            "zk_server_heapsize": "1024m",
            "zk_pid_dir": "/var/run/zookeeper",
             "zk_user": "zookeeper"
         },
         "infra-solr-env": {
@@ -682,7 +682,7 @@
             "infra_solr_kerberos_name_rules": "DEFAULT",
             "infra_solr_user": "infra-solr",
             "infra_solr_maxmem": "1024",
            "content": "#!/bin/bash\n# Licensed to the Apache Software Foundation (ASF) under one or more\n# contributor license agreements. See the NOTICE file distributed with\n# this work for additional information regarding copyright ownership.\n# The ASF licenses this file to You under the Apache License, Version 2.0\n# (the \"License\"); you may not use this file except in compliance with\n# the License. You may obtain a copy of the License at\n#\n# http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n\n# By default the script will use JAVA_HOME to determine which java\n# to use, but you can set a specific path for Solr to use without\n# affecting other Java applications on your server/workstation.\nSOLR_JAVA_HOME={{java64_home}}\n\n# Increase Java Min/Max Heap as needed to support your indexing / query needs\nSOLR_JAVA_MEM=\"-Xms{{logsearch_solr_min_mem}}m -Xmx{{logsearch_solr_max_mem}}m\"\n\n# Enable verbose GC logging\nGC_LOG_OPTS=\"-verbose:gc -XX:+PrintHeapAtGC -XX:+PrintGCDetails \\\n-XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationStoppedTime\"\n\n# These GC settings have shown to work well for a number of common Solr workloads\nGC_TUNE=\"-XX:NewRatio=3 \\\n-XX:SurvivorRatio=4 \\\n-XX:TargetSurvivorRatio=90 \\\n-XX:MaxTenuringThreshold=8 \\\n-XX:+UseConcMarkSweepGC \\\n-XX:+UseParNewGC \\\n-XX:ConcGCThreads=4 -XX:ParallelGCThreads=4 \\\n-XX:+CMSScavengeBeforeRemark \\\n-XX:PretenureSizeThreshold=64m \\\n-XX:+UseCMSInitiatingOccupancyOnly \\\n-XX:CMSInitiatingOccupancyFraction=50 \\\n-XX:CMSMaxAbortablePrecleanTime=6000 \\\n-XX:+CMSParallelRemarkEnabled \\\n-XX:+ParallelRefProcEnabled\"\n\n# Set the ZooKeeper connection string if using an external ZooKeeper ensemble\n# e.g. host1:2181,host2:2181/chroot\n# Leave empty if not using SolrCloud\nZK_HOST=\"{{zookeeper_quorum}}{{logsearch_solr_znode}}\"\n\n# Set the ZooKeeper client timeout (for SolrCloud mode)\nZK_CLIENT_TIMEOUT=\"60000\"\n\n# By default the start script uses \"localhost\"; override the hostname here\n# for production SolrCloud environments to control the hostname exposed to cluster state\n#SOLR_HOST=\"192.168.1.1\"\n\n# By default the start script uses UTC; override the timezone if needed\n#SOLR_TIMEZONE=\"UTC\"\n\n# Set to true to activate the JMX RMI connector to allow remote JMX client applications\n# to monitor the JVM hosting Solr; set to \"false\" to disable that behavior\n# (false is recommended in production environments)\nENABLE_REMOTE_JMX_OPTS=\"true\"\n\n# The script will use SOLR_PORT+10000 for the RMI_PORT or you can set it here\nRMI_PORT={{logsearch_solr_jmx_port}}\n\n# Anything you add to the SOLR_OPTS variable will be included in the java\n# start command line as-is, in ADDITION to other options. If you specify the\n# -a option on start script, those options will be appended as well. Examples:\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.autoSoftCommit.maxTime=3000\"\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.autoCommit.maxTime=60000\"\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.clustering.enabled=true\"\n\n# Location where the bin/solr script will save PID files for running instances\n# If not set, the script will create PID files in $SOLR_TIP/bin\nSOLR_PID_DIR={{logsearch_solr_piddir}}\n\n# Path to a directory where Solr creates index files, the specified directory\n# must contain a solr.xml; by default, Solr will use server/solr\nSOLR_HOME={{logsearch_solr_datadir}}\n\n# Solr provides a default Log4J configuration properties file in server/resources\n# however, you may want to customize the log settings and file appender location\n# so you can point the script to use a different log4j.properties file\nLOG4J_PROPS={{logsearch_solr_conf}}/log4j.properties\n\n# Location where Solr should write logs to; should agree with the file appender\n# settings in server/resources/log4j.properties\nSOLR_LOGS_DIR={{logsearch_solr_log_dir}}\n\n# Sets the port Solr binds to, default is 8983\nSOLR_PORT={{logsearch_solr_port}}\n\n# Be sure to update the paths to the correct keystore for your environment\n{% if logsearch_solr_ssl_enabled %}\nSOLR_SSL_KEY_STORE={{logsearch_solr_keystore_location}}\nSOLR_SSL_KEY_STORE_PASSWORD={{logsearch_solr_keystore_password}}\nSOLR_SSL_TRUST_STORE={{logsearch_solr_keystore_location}}\nSOLR_SSL_TRUST_STORE_PASSWORD={{logsearch_solr_keystore_password}}\nSOLR_SSL_NEED_CLIENT_AUTH=false\nSOLR_SSL_WANT_CLIENT_AUTH=false\n{% endif %}\n\n# Uncomment to set a specific SSL port (-Djetty.ssl.port=N); if not set\n# and you are using SSL, then the start script will use SOLR_PORT for the SSL port\n#SOLR_SSL_PORT=\n\n{% if security_enabled -%}\nSOLR_HOST=`hostname -f`\nSOLR_JAAS_FILE={{logsearch_solr_jaas_file}}\nSOLR_KERB_KEYTAB={{logsearch_solr_web_kerberos_keytab}}\nSOLR_KERB_PRINCIPAL={{logsearch_solr_web_kerberos_principal}}\nSOLR_KERB_NAME_RULES={{logsearch_solr_kerberos_name_rules}}\n\nSOLR_AUTHENTICATION_CLIENT_CONFIGURER=\"org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer\"\nSOLR_AUTHENTICATION_OPTS=\" -DauthenticationPlugin=org.apache.solr.security.KerberosPlugin -Djava.security.auth.login.config=$SOLR_JAAS_FILE -Dsolr.kerberos.principal=${SOLR_KERB_PRINCIPAL} -Dsolr.kerberos.keytab=${SOLR_KERB_KEYTAB} -Dsolr.kerberos.cookie.domain=${SOLR_HOST} -Dsolr.kerberos.name.rules=${SOLR_KERB_NAME_RULES}\"\n{% endif %}", 
            "content": "#!/bin/bash\n# Licensed to the Apache Software Foundation (ASF) under one or more\n# contributor license agreements. See the NOTICE file distributed with\n# this work for additional information regarding copyright ownership.\n# The ASF licenses this file to You under the Apache License, Version 2.0\n# (the \"License\"); you may not use this file except in compliance with\n# the License. You may obtain a copy of the License at\n#\n# http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n\n# By default the script will use JAVA_HOME to determine which java\n# to use, but you can set a specific path for Solr to use without\n# affecting other Java applications on your server/workstation.\nSOLR_JAVA_HOME={{java64_home}}\n\n# Increase Java Min/Max Heap as needed to support your indexing / query needs\nSOLR_JAVA_MEM=\"-Xms{{logsearch_solr_min_mem}}m -Xmx{{logsearch_solr_max_mem}}m\"\n\n# Enable verbose GC logging\nGC_LOG_OPTS=\"-verbose:gc -XX:+PrintHeapAtGC -XX:+PrintGCDetails \\\n-XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationStoppedTime\"\n\n# These GC settings have shown to work well for a number of common Solr workloads\nGC_TUNE=\"-XX:NewRatio=3 \\\n-XX:SurvivorRatio=4 \\\n-XX:TargetSurvivorRatio=90 \\\n-XX:MaxTenuringThreshold=8 \\\n-XX:+UseConcMarkSweepGC \\\n-XX:+UseParNewGC \\\n-XX:ConcGCThreads=4 -XX:ParallelGCThreads=4 \\\n-XX:+CMSScavengeBeforeRemark \\\n-XX:PretenureSizeThreshold=64m \\\n-XX:+UseCMSInitiatingOccupancyOnly \\\n-XX:CMSInitiatingOccupancyFraction=50 \\\n-XX:CMSMaxAbortablePrecleanTime=6000 \\\n-XX:+CMSParallelRemarkEnabled \\\n-XX:+ParallelRefProcEnabled\"\n\n# Set the ZooKeeper connection string if using an external ZooKeeper ensemble\n# e.g. host1:2181,host2:2181/chroot\n# Leave empty if not using SolrCloud\nZK_HOST=\"{{zookeeper_quorum}}{{logsearch_solr_znode}}\"\n\n# Set the ZooKeeper client timeout (for SolrCloud mode)\nZK_CLIENT_TIMEOUT=\"60000\"\n\n# By default the start script uses \"localhost\"; override the hostname here\n# for production SolrCloud environments to control the hostname exposed to cluster state\n#SOLR_HOST=\"192.168.1.1\"\n\n# By default the start script uses UTC; override the timezone if needed\n#SOLR_TIMEZONE=\"UTC\"\n\n# Set to true to activate the JMX RMI connector to allow remote JMX client applications\n# to monitor the JVM hosting Solr; set to \"false\" to disable that behavior\n# (false is recommended in production environments)\nENABLE_REMOTE_JMX_OPTS=\"true\"\n\n# The script will use SOLR_PORT+10000 for the RMI_PORT or you can set it here\nRMI_PORT={{logsearch_solr_jmx_port}}\n\n# Anything you add to the SOLR_OPTS variable will be included in the java\n# start command line as-is, in ADDITION to other options. If you specify the\n# -a option on start script, those options will be appended as well. Examples:\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.autoSoftCommit.maxTime=3000\"\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.autoCommit.maxTime=60000\"\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.clustering.enabled=true\"\n\n# Location where the bin/solr script will save PID files for running instances\n# If not set, the script will create PID files in $SOLR_TIP/bin\nSOLR_PID_DIR={{logsearch_solr_piddir}}\n\n# Path to a directory where Solr creates index files, the specified directory\n# must contain a solr.xml; by default, Solr will use server/solr\nSOLR_HOME={{logsearch_solr_datadir}}\n\n# Solr provides a default Log4J configuration properties file in server/resources\n# however, you may want to customize the log settings and file appender location\n# so you can point the script to use a different log4j.properties file\nLOG4J_PROPS={{logsearch_solr_conf}}/log4j.properties\n\n# Location where Solr should write logs to; should agree with the file appender\n# settings in server/resources/log4j.properties\nSOLR_LOGS_DIR={{logsearch_solr_log_dir}}\n\n# Sets the port Solr binds to, default is 8983\nSOLR_PORT={{logsearch_solr_port}}\n\n# Be sure to update the paths to the correct keystore for your environment\n{% if logsearch_solr_ssl_enabled %}\nSOLR_SSL_KEY_STORE={{logsearch_solr_keystore_location}}\nSOLR_SSL_KEY_STORE_PASSWORD={{logsearch_solr_keystore_password}}\nSOLR_SSL_TRUST_STORE={{logsearch_solr_keystore_location}}\nSOLR_SSL_TRUST_STORE_PASSWORD={{logsearch_solr_keystore_password}}\nSOLR_SSL_NEED_CLIENT_AUTH=false\nSOLR_SSL_WANT_CLIENT_AUTH=false\n{% endif %}\n\n# Uncomment to set a specific SSL port (-Djetty.ssl.port=N); if not set\n# and you are using SSL, then the start script will use SOLR_PORT for the SSL port\n#SOLR_SSL_PORT=\n\n{% if security_enabled -%}\nSOLR_HOST=`hostname -f`\nSOLR_JAAS_FILE={{logsearch_solr_jaas_file}}\nSOLR_KERB_KEYTAB={{logsearch_solr_web_kerberos_keytab}}\nSOLR_KERB_PRINCIPAL={{logsearch_solr_web_kerberos_principal}}\nSOLR_KERB_NAME_RULES={{logsearch_solr_kerberos_name_rules}}\n\nSOLR_AUTHENTICATION_CLIENT_CONFIGURER=\"org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer\"\nSOLR_AUTHENTICATION_OPTS=\" -DauthenticationPlugin=org.apache.solr.security.KerberosPlugin -Djava.security.auth.login.config=$SOLR_JAAS_FILE -Dsolr.kerberos.principal=${SOLR_KERB_PRINCIPAL} -Dsolr.kerberos.keytab=${SOLR_KERB_KEYTAB} -Dsolr.kerberos.cookie.domain=${SOLR_HOST} -Dsolr.kerberos.name.rules=${SOLR_KERB_NAME_RULES}\"\n{% endif %}",
             "infra_solr_pid_dir": "/var/run/ambari-infra-solr",
             "infra_solr_truststore_password": "bigdata",
             "infra_solr_truststore_type": "jks",
@@ -706,30 +706,29 @@
             "content": "content"
         },
         "cluster-env": {
            "security_enabled": "false", 
            "override_uid": "true", 
            "fetch_nonlocal_groups": "true", 
            "one_dir_per_partition": "true", 
            "repo_ubuntu_template": "{{package_type}} {{base_url}} {{components}}", 
            "ignore_groupsusers_create": "false", 
            "alerts_repeat_tolerance": "1", 
            "smokeuser_keytab": "/etc/security/keytabs/smokeuser.headless.keytab", 
            "kerberos_domain": "EXAMPLE.COM", 
            "security_enabled": "false",
            "override_uid": "true",
            "fetch_nonlocal_groups": "true",
            "one_dir_per_partition": "true",
            "repo_ubuntu_template": "{{package_type}} {{base_url}} {{components}}",
            "ignore_groupsusers_create": "false",
            "alerts_repeat_tolerance": "1",
            "smokeuser_keytab": "/etc/security/keytabs/smokeuser.headless.keytab",
            "kerberos_domain": "EXAMPLE.COM",
             "manage_dirs_on_root": "true",
            "recovery_lifetime_max_count": "1024", 
            "recovery_type": "AUTO_START", 
            "ignore_bad_mounts": "false", 
            "recovery_window_in_minutes": "60", 
            "user_group": "hadoop", 
            "stack_tools": "{\n  \"stack_selector\": [\"hdp-select\", \"/usr/bin/hdp-select\", \"hdp-select\"],\n  \"conf_selector\": [\"conf-select\", \"/usr/bin/conf-select\", \"conf-select\"]\n}",
            "recovery_retry_interval": "5",
            "stack_features": "{\n  \"stack_features\": [\n    {\n      \"name\": \"snappy\",\n      \"description\": \"Snappy compressor/decompressor support\",\n      \"min_version\": \"2.0.0.0\",\n      \"max_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"lzo\",\n      \"description\": \"LZO libraries support\",\n      \"min_version\": \"2.2.1.0\"\n    },\n    {\n      \"name\": \"express_upgrade\",\n      \"description\": \"Express upgrade support\",\n      \"min_version\": \"2.1.0.0\"\n    },\n    {\n      \"name\": \"rolling_upgrade\",\n      \"description\": \"Rolling upgrade support\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"config_versioning\",\n      \"description\": \"Configurable versions support\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"datanode_non_root\",\n      \"description\": \"DataNode running as non-root support (AMBARI-7615)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"remove_ranger_hdfs_plugin_env\",\n      \"description\": \"HDFS removes Ranger env files (AMBARI-14299)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"ranger\",\n      \"description\": \"Ranger Service support\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"ranger_tagsync_component\",\n      \"description\": \"Ranger Tagsync component support (AMBARI-14383)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"phoenix\",\n      \"description\": \"Phoenix Service support\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"nfs\",\n      \"description\": \"NFS support\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"tez_for_spark\",\n      \"description\": \"Tez dependency for Spark\",\n      \"min_version\": \"2.2.0.0\",\n      \"max_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"timeline_state_store\",\n      \"description\": \"Yarn application timeline-service supports state store property (AMBARI-11442)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"copy_tarball_to_hdfs\",\n      \"description\": \"Copy tarball to HDFS support (AMBARI-12113)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"spark_16plus\",\n      \"description\": \"Spark 1.6+\",\n      \"min_version\": \"2.4.0.0\"\n    },\n    {\n      \"name\": \"spark_thriftserver\",\n      \"description\": \"Spark Thrift Server\",\n      \"min_version\": \"2.3.2.0\"\n    },\n    {\n      \"name\": \"storm_kerberos\",\n      \"description\": \"Storm Kerberos support (AMBARI-7570)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"storm_ams\",\n      \"description\": \"Storm AMS integration (AMBARI-10710)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"create_kafka_broker_id\",\n      \"description\": \"Ambari should create Kafka Broker Id (AMBARI-12678)\",\n      \"min_version\": \"2.2.0.0\",\n      \"max_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"kafka_listeners\",\n      \"description\": \"Kafka listeners (AMBARI-10984)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"kafka_kerberos\",\n      \"description\": \"Kafka Kerberos support (AMBARI-10984)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"pig_on_tez\",\n      \"description\": \"Pig on Tez support (AMBARI-7863)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"ranger_usersync_non_root\",\n      \"description\": \"Ranger Usersync as non-root user (AMBARI-10416)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"ranger_audit_db_support\",\n      \"description\": \"Ranger Audit to DB support\",\n      \"min_version\": \"2.2.0.0\",\n      \"max_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"accumulo_kerberos_user_auth\",\n      \"description\": \"Accumulo Kerberos User Auth (AMBARI-10163)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"knox_versioned_data_dir\",\n      \"description\": \"Use versioned data dir for Knox (AMBARI-13164)\",\n      \"min_version\": \"2.3.2.0\"\n    },\n    {\n      \"name\": \"knox_sso_topology\",\n      \"description\": \"Knox SSO Topology support (AMBARI-13975)\",\n      \"min_version\": \"2.3.8.0\"\n    },\n    {\n      \"name\": \"atlas_rolling_upgrade\",\n      \"description\": \"Rolling upgrade support for Atlas\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"oozie_admin_user\",\n      \"description\": \"Oozie install user as an Oozie admin user (AMBARI-7976)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"oozie_create_hive_tez_configs\",\n      \"description\": \"Oozie create configs for Ambari Hive and Tez deployments (AMBARI-8074)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"oozie_setup_shared_lib\",\n      \"description\": \"Oozie setup tools used to shared Oozie lib to HDFS (AMBARI-7240)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"oozie_host_kerberos\",\n      \"description\": \"Oozie in secured clusters uses _HOST in Kerberos principal (AMBARI-9775)\",\n      \"min_version\": \"2.0.0.0\",\n      \"max_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"falcon_extensions\",\n      \"description\": \"Falcon Extension\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hive_metastore_upgrade_schema\",\n      \"description\": \"Hive metastore upgrade schema support (AMBARI-11176)\",\n      \"min_version\": \"2.3.0.0\"\n     },\n    {\n      \"name\": \"hive_server_interactive\",\n      \"description\": \"Hive server interactive support (AMBARI-15573)\",\n      \"min_version\": \"2.5.0.0\"\n     },\n    {\n      \"name\": \"hive_webhcat_specific_configs\",\n      \"description\": \"Hive webhcat specific configurations support (AMBARI-12364)\",\n      \"min_version\": \"2.3.0.0\"\n     },\n    {\n      \"name\": \"hive_purge_table\",\n      \"description\": \"Hive purge table support (AMBARI-12260)\",\n      \"min_version\": \"2.3.0.0\"\n     },\n    {\n      \"name\": \"hive_server2_kerberized_env\",\n      \"description\": \"Hive server2 working on kerberized environment (AMBARI-13749)\",\n      \"min_version\": \"2.2.3.0\",\n      \"max_version\": \"2.2.5.0\"\n     },\n    {\n      \"name\": \"hive_env_heapsize\",\n      \"description\": \"Hive heapsize property defined in hive-env (AMBARI-12801)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"ranger_kms_hsm_support\",\n      \"description\": \"Ranger KMS HSM support (AMBARI-15752)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_log4j_support\",\n      \"description\": \"Ranger supporting log-4j properties (AMBARI-15681)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_kerberos_support\",\n      \"description\": \"Ranger Kerberos support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hive_metastore_site_support\",\n      \"description\": \"Hive Metastore site support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_usersync_password_jceks\",\n      \"description\": \"Saving Ranger Usersync credentials in jceks\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_install_infra_client\",\n      \"description\": \"Ambari Infra Service support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"falcon_atlas_support_2_3\",\n      \"description\": \"Falcon Atlas integration support for 2.3 stack\",\n      \"min_version\": \"2.3.99.0\",\n      \"max_version\": \"2.4.0.0\"\n    },\n    {\n      \"name\": \"falcon_atlas_support\",\n      \"description\": \"Falcon Atlas integration\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hbase_home_directory\",\n      \"description\": \"Hbase home directory in HDFS needed for HBASE backup\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"spark_livy\",\n      \"description\": \"Livy as slave component of spark\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"atlas_ranger_plugin_support\",\n      \"description\": \"Atlas Ranger plugin support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"atlas_conf_dir_in_path\",\n      \"description\": \"Prepend the Atlas conf dir (/etc/atlas/conf) to the classpath of Storm and Falcon\",\n      \"min_version\": \"2.3.0.0\",\n      \"max_version\": \"2.4.99.99\"\n    },\n    {\n      \"name\": \"atlas_upgrade_support\",\n      \"description\": \"Atlas supports express and rolling upgrades\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"atlas_hook_support\",\n      \"description\": \"Atlas support for hooks in Hive, Storm, Falcon, and Sqoop\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_pid_support\",\n      \"description\": \"Ranger Service support pid generation AMBARI-16756\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_kms_pid_support\",\n      \"description\": \"Ranger KMS Service support pid generation\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_admin_password_change\",\n      \"description\": \"Allow ranger admin credentials to be specified during cluster creation (AMBARI-17000)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"storm_metrics_apache_classes\",\n      \"description\": \"Metrics sink for Storm that uses Apache class names\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"spark_java_opts_support\",\n      \"description\": \"Allow Spark to generate java-opts file\",\n      \"min_version\": \"2.2.0.0\",\n      \"max_version\": \"2.4.0.0\"\n    },\n    {\n      \"name\": \"atlas_hbase_setup\",\n      \"description\": \"Use script to create Atlas tables in Hbase and set permissions for Atlas user.\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_hive_plugin_jdbc_url\",\n      \"description\": \"Handle Ranger hive repo config jdbc url change for stack 2.5 (AMBARI-18386)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"zkfc_version_advertised\",\n      \"description\": \"ZKFC advertise version\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_tagsync_ssl_xml_support\",\n      \"description\": \"Ranger Tagsync ssl xml support.\",\n      \"min_version\": \"2.6.0.0\"\n    }\n  ]\n}",
            "recovery_lifetime_max_count": "1024",
            "recovery_type": "AUTO_START",
            "ignore_bad_mounts": "false",
            "recovery_window_in_minutes": "60",
            "user_group": "hadoop",
            "stack_name": "HDP",
            "stack_root": "{\"HDP\": \"/usr/hdp\"}",
            "stack_tools": "{\n \"HDP\": { \"stack_selector\": [\"hdp-select\", \"/usr/bin/hdp-select\", \"hdp-select\"],\n  \"conf_selector\": [\"conf-select\", \"/usr/bin/conf-select\", \"conf-select\"]\n}\n}",
             "recovery_enabled": "true",
            "recovery_max_count": "6", 
            "stack_root": "/usr/hdp", 
            "repo_suse_rhel_template": "[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0", 
            "managed_hdfs_resource_property_names": "", 
            "recovery_max_count": "6",
            "repo_suse_rhel_template": "[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0",
            "managed_hdfs_resource_property_names": "",
             "smokeuser": "ambari-qa"
         }
     }
}
\ No newline at end of file
}
diff --git a/ambari-server/src/test/python/stacks/2.6/configs/ranger-admin-secured.json b/ambari-server/src/test/python/stacks/2.6/configs/ranger-admin-secured.json
index fa791c1f09..64e7d5289e 100644
-- a/ambari-server/src/test/python/stacks/2.6/configs/ranger-admin-secured.json
++ b/ambari-server/src/test/python/stacks/2.6/configs/ranger-admin-secured.json
@@ -1,101 +1,101 @@
 {
     "localComponents": [
        "SECONDARY_NAMENODE", 
        "HDFS_CLIENT", 
        "DATANODE", 
        "NAMENODE", 
        "RANGER_ADMIN", 
        "RANGER_TAGSYNC", 
        "RANGER_USERSYNC", 
        "ZOOKEEPER_SERVER", 
        "ZOOKEEPER_CLIENT", 
        "SECONDARY_NAMENODE",
        "HDFS_CLIENT",
        "DATANODE",
        "NAMENODE",
        "RANGER_ADMIN",
        "RANGER_TAGSYNC",
        "RANGER_USERSYNC",
        "ZOOKEEPER_SERVER",
        "ZOOKEEPER_CLIENT",
         "KERBEROS_CLIENT",
         "LOGSEARCH_SOLR",
         "LOGSEARCH_SOLR_CLIENT"
    ], 
    ],
     "configuration_attributes": {
        "ranger-hdfs-audit": {}, 
        "ssl-client": {}, 
        "ranger-admin-site": {}, 
        "ranger-hdfs-policymgr-ssl": {}, 
        "tagsync-application-properties": {}, 
        "ranger-env": {}, 
        "usersync-log4j": {}, 
        "ranger-hdfs-plugin-properties": {}, 
        "kerberos-env": {}, 
        "admin-properties": {}, 
        "ranger-ugsync-site": {}, 
        "ranger-hdfs-audit": {},
        "ssl-client": {},
        "ranger-admin-site": {},
        "ranger-hdfs-policymgr-ssl": {},
        "tagsync-application-properties": {},
        "ranger-env": {},
        "usersync-log4j": {},
        "ranger-hdfs-plugin-properties": {},
        "kerberos-env": {},
        "admin-properties": {},
        "ranger-ugsync-site": {},
         "hdfs-site": {
             "final": {
                "dfs.datanode.data.dir": "true", 
                "dfs.namenode.http-address": "true", 
                "dfs.datanode.failed.volumes.tolerated": "true", 
                "dfs.support.append": "true", 
                "dfs.namenode.name.dir": "true", 
                "dfs.datanode.data.dir": "true",
                "dfs.namenode.http-address": "true",
                "dfs.datanode.failed.volumes.tolerated": "true",
                "dfs.support.append": "true",
                "dfs.namenode.name.dir": "true",
                 "dfs.webhdfs.enabled": "true"
             }
        }, 
        },
         "ranger-tagsync-site": {},
        "ranger-tagsync-policymgr-ssl": {}, 
        "ranger-tagsync-policymgr-ssl": {},
         "zoo.cfg": {},
         "hadoop-policy": {},
        "hdfs-log4j": {}, 
        "krb5-conf": {}, 
        "hdfs-log4j": {},
        "krb5-conf": {},
         "core-site": {
             "final": {
                 "fs.defaultFS": "true"
             }
        }, 
        "hadoop-env": {}, 
        "zookeeper-log4j": {}, 
        "ssl-server": {}, 
        "ranger-site": {}, 
        "admin-log4j": {}, 
        "tagsync-log4j": {}, 
        "ranger-hdfs-security": {}, 
        },
        "hadoop-env": {},
        "zookeeper-log4j": {},
        "ssl-server": {},
        "ranger-site": {},
        "admin-log4j": {},
        "tagsync-log4j": {},
        "ranger-hdfs-security": {},
         "ranger-solr-configuration": {},
         "usersync-properties": {},
         "zookeeper-env": {},
         "infra-solr-env": {},
         "infra-solr-client-log4j": {},
         "cluster-env": {}
    }, 
    "public_hostname": "c6401.ambari.apache.org", 
    "commandId": "41-2", 
    "hostname": "c6401.ambari.apache.org", 
    "kerberosCommandParams": [], 
    "serviceName": "RANGER", 
    "role": "RANGER_ADMIN", 
    "forceRefreshConfigTagsBeforeExecution": [], 
    "requestId": 41, 
    },
    "public_hostname": "c6401.ambari.apache.org",
    "commandId": "41-2",
    "hostname": "c6401.ambari.apache.org",
    "kerberosCommandParams": [],
    "serviceName": "RANGER",
    "role": "RANGER_ADMIN",
    "forceRefreshConfigTagsBeforeExecution": [],
    "requestId": 41,
     "agentConfigParams": {
         "agent": {
             "parallel_execution": 0
         }
    }, 
    "clusterName": "test_Cluster01", 
    "commandType": "EXECUTION_COMMAND", 
    "taskId": 186, 
    "roleParams": {}, 
    },
    "clusterName": "test_Cluster01",
    "commandType": "EXECUTION_COMMAND",
    "taskId": 186,
    "roleParams": {},
     "configurationTags": {
         "ranger-hdfs-audit": {
             "tag": "version1466705299922"
        }, 
        },
         "ssl-client": {
             "tag": "version1"
        }, 
        },
         "ranger-admin-site": {
             "tag": "version1467016680635"
        }, 
        },
         "ranger-hdfs-policymgr-ssl": {
             "tag": "version1466705299922"
        }, 
        },
         "tagsync-application-properties": {
             "tag": "version1467016680511"
        }, 
        },
         "ranger-env": {
             "tag": "version1466705299949"
        }, 
        },
         "ranger-ugsync-site": {
             "tag": "version1467016680537"
         },
@@ -104,52 +104,52 @@
         },
         "ranger-hdfs-plugin-properties": {
             "tag": "version1466705299922"
        }, 
        },
         "kerberos-env": {
             "tag": "version1467016537243"
        }, 
        },
         "admin-properties": {
             "tag": "version1466705299949"
        }, 
        },
         "hdfs-site": {
             "tag": "version1467016680401"
        }, 
        },
         "ranger-tagsync-site": {
             "tag": "version1467016680586"
        }, 
        },
         "zoo.cfg": {
             "tag": "version1"
        }, 
        },
         "hadoop-policy": {
             "tag": "version1"
        }, 
        },
         "hdfs-log4j": {
             "tag": "version1"
        }, 
        },
         "usersync-log4j": {
             "tag": "version1466705299949"
        }, 
        },
         "krb5-conf": {
             "tag": "version1467016537243"
        }, 
        },
         "core-site": {
             "tag": "version1467016680612"
        }, 
        },
         "hadoop-env": {
             "tag": "version1467016680446"
        }, 
        },
         "zookeeper-log4j": {
             "tag": "version1"
        }, 
        },
         "ssl-server": {
             "tag": "version1"
        }, 
        },
         "ranger-site": {
             "tag": "version1466705299949"
        }, 
        },
         "admin-log4j": {
             "tag": "version1466705299949"
        }, 
        },
         "tagsync-log4j": {
             "tag": "version1466705299949"
         },
@@ -158,10 +158,10 @@
         },
         "ranger-hdfs-security": {
             "tag": "version1466705299922"
        }, 
        },
         "usersync-properties": {
             "tag": "version1466705299949"
        }, 
        },
         "zookeeper-env": {
             "tag": "version1467016680492"
         },
@@ -174,116 +174,116 @@
         "cluster-env": {
             "tag": "version1467016680567"
         }
    }, 
    "roleCommand": "START", 
    },
    "roleCommand": "START",
     "hostLevelParams": {
        "agent_stack_retry_on_unavailability": "false", 
        "stack_name": "HDP", 
        "agent_stack_retry_on_unavailability": "false",
        "stack_name": "HDP",
         "package_version": "2_6_0_0_*",
         "custom_mysql_jdbc_name": "mysql-connector-java.jar",
         "previous_custom_mysql_jdbc_name": "mysql-connector-java-old.jar",
        "host_sys_prepped": "false", 
        "ambari_db_rca_username": "mapred", 
        "host_sys_prepped": "false",
        "ambari_db_rca_username": "mapred",
         "current_version": "2.6.0.0-801",
         "mysql_jdbc_url": "http://c6401.ambari.apache.org:8080/resources//mysql-connector-java.jar",
         "agent_stack_retry_count": "5",
         "stack_version": "2.6",
        "jdk_name": "jdk-8u60-linux-x64.tar.gz", 
        "ambari_db_rca_driver": "org.postgresql.Driver", 
        "jdk_name": "jdk-8u60-linux-x64.tar.gz",
        "ambari_db_rca_driver": "org.postgresql.Driver",
         "java_home": "/usr/jdk64/jdk1.7.0_45",
         "repository_version_id": "1",
         "jdk_location": "http://c6401.ambari.apache.org:8080/resources/",
        "not_managed_hdfs_path_list": "[\"/tmp\"]", 
        "ambari_db_rca_url": "jdbc:postgresql://c6401.ambari.apache.org/ambarirca", 
        "java_version": "8", 
        "not_managed_hdfs_path_list": "[\"/tmp\"]",
        "ambari_db_rca_url": "jdbc:postgresql://c6401.ambari.apache.org/ambarirca",
        "java_version": "8",
         "repo_info": "[{\"baseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.6.0.0-801\",\"osType\":\"redhat6\",\"repoId\":\"HDP-2.6\",\"repoName\":\"HDP\",\"defaultBaseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/updates/2.6.0.0\",\"latestBaseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.6.0.0-801\",\"baseSaved\":true},{\"baseUrl\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"osType\":\"redhat6\",\"repoId\":\"HDP-UTILS-1.1.0.21\",\"repoName\":\"HDP-UTILS\",\"defaultBaseUrl\":\"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"latestBaseUrl\":\"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6\",\"baseSaved\":true}]",
         "package_list": "[{\"name\":\"ranger_${stack_version}-admin\",\"condition\":\"\",\"skipUpgrade\":false},{\"name\":\"ranger_${stack_version}-usersync\",\"condition\":\"\",\"skipUpgrade\":false},{\"name\":\"ranger_${stack_version}-tagsync\",\"condition\":\"should_install_ranger_tagsync\",\"skipUpgrade\":false},{\"name\":\"ambari-logsearch-solr-client\",\"condition\":\"should_install_logsearch_solr_client\",\"skipUpgrade\":false}]",
         "db_name": "ambari",
        "group_list": "[\"ranger\",\"hadoop\",\"users\"]", 
        "agentCacheDir": "/var/lib/ambari-agent/cache", 
        "ambari_db_rca_password": "mapred", 
        "jce_name": "jce_policy-8.zip", 
        "oracle_jdbc_url": "http://c6401.ambari.apache.org:8080/resources//ojdbc6.jar", 
        "db_driver_filename": "mysql-connector-java.jar", 
        "user_list": "[\"zookeeper\",\"ambari-qa\",\"ranger\",\"hdfs\"]", 
        "group_list": "[\"ranger\",\"hadoop\",\"users\"]",
        "agentCacheDir": "/var/lib/ambari-agent/cache",
        "ambari_db_rca_password": "mapred",
        "jce_name": "jce_policy-8.zip",
        "oracle_jdbc_url": "http://c6401.ambari.apache.org:8080/resources//ojdbc6.jar",
        "db_driver_filename": "mysql-connector-java.jar",
        "user_list": "[\"zookeeper\",\"ambari-qa\",\"ranger\",\"hdfs\"]",
         "clientsToUpdateConfigs": "[\"*\"]"
    }, 
    },
     "commandParams": {
        "service_package_folder": "common-services/RANGER/0.4.0/package", 
        "script": "scripts/ranger_admin.py", 
        "hooks_folder": "HDP/2.0.6/hooks", 
        "service_package_folder": "common-services/RANGER/0.4.0/package",
        "script": "scripts/ranger_admin.py",
        "hooks_folder": "HDP/2.0.6/hooks",
         "version": "2.6.0.0-801",
         "max_duration_for_retries": "0",
         "command_retry_enabled": "false",
        "command_timeout": "600", 
        "command_timeout": "600",
         "script_type": "PYTHON"
    }, 
    "forceRefreshConfigTags": [], 
    "stageId": 2, 
    },
    "forceRefreshConfigTags": [],
    "stageId": 2,
     "clusterHostInfo": {
         "snamenode_host": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "ambari_server_use_ssl": [
             "false"
        ], 
        ],
         "all_ping_ports": [
             "8670"
        ], 
        ],
         "ranger_tagsync_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "ranger_usersync_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "all_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "slave_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "namenode_host": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "ambari_server_port": [
             "8080"
        ], 
        ],
         "ranger_admin_hosts": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "all_racks": [
             "/default-rack"
        ], 
        ],
         "all_ipv4_ips": [
             "172.22.83.73"
        ], 
        ],
         "ambari_server_host": [
             "c6401.ambari.apache.org"
        ], 
        ],
         "zookeeper_hosts": [
             "c6401.ambari.apache.org"
         ],
         "infra_solr_hosts": [
             "c6401.ambari.apache.org"
         ]
    }, 
    },
     "configurations": {
         "ranger-hdfs-audit": {
            "xasecure.audit.destination.solr.zookeepers": "NONE", 
            "xasecure.audit.destination.solr.urls": "", 
            "xasecure.audit.destination.solr.batch.filespool.dir": "/var/log/hadoop/hdfs/audit/solr/spool", 
            "xasecure.audit.destination.solr.zookeepers": "NONE",
            "xasecure.audit.destination.solr.urls": "",
            "xasecure.audit.destination.solr.batch.filespool.dir": "/var/log/hadoop/hdfs/audit/solr/spool",
             "xasecure.audit.destination.hdfs.batch.filespool.dir": "/var/log/hadoop/hdfs/audit/hdfs/spool",
            "xasecure.audit.destination.hdfs": "true", 
            "xasecure.audit.destination.hdfs": "true",
             "xasecure.audit.destination.solr": "false",
            "xasecure.audit.provider.summary.enabled": "false", 
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit", 
            "xasecure.audit.provider.summary.enabled": "false",
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit",
             "xasecure.audit.is.enabled": "true"
         },
         "ranger-tagsync-policymgr-ssl": {
            "xasecure.policymgr.clientssl.keystore": "/etc/security/serverKeys/ranger-tagsync-keystore.jks", 
            "xasecure.policymgr.clientssl.truststore.password": "changeit", 
            "xasecure.policymgr.clientssl.keystore": "/etc/security/serverKeys/ranger-tagsync-keystore.jks",
            "xasecure.policymgr.clientssl.truststore.password": "changeit",
             "xasecure.policymgr.clientssl.keystore.credential.file": "jceks://file{{ranger_tagsync_credential_file}}",
            "xasecure.policymgr.clientssl.truststore": "/etc/security/serverKeys/ranger-tagsync-mytruststore.jks", 
            "xasecure.policymgr.clientssl.truststore": "/etc/security/serverKeys/ranger-tagsync-mytruststore.jks",
             "xasecure.policymgr.clientssl.truststore.credential.file": "jceks://file{{ranger_tagsync_credential_file}}",
             "xasecure.policymgr.clientssl.keystore.password": "myKeyFilePassword"
         },
@@ -296,186 +296,186 @@
             "xasecure.policymgr.clientssl.keystore.password": "myKeyFilePassword"
         },
         "ssl-client": {
            "ssl.client.truststore.reload.interval": "10000", 
            "ssl.client.keystore.password": "bigdata", 
            "ssl.client.truststore.type": "jks", 
            "ssl.client.keystore.location": "/etc/security/clientKeys/keystore.jks", 
            "ssl.client.truststore.location": "/etc/security/clientKeys/all.jks", 
            "ssl.client.truststore.password": "bigdata", 
            "ssl.client.truststore.reload.interval": "10000",
            "ssl.client.keystore.password": "bigdata",
            "ssl.client.truststore.type": "jks",
            "ssl.client.keystore.location": "/etc/security/clientKeys/keystore.jks",
            "ssl.client.truststore.location": "/etc/security/clientKeys/all.jks",
            "ssl.client.truststore.password": "bigdata",
             "ssl.client.keystore.type": "jks"
        }, 
        },
         "ranger-admin-site": {
             "ranger.is.solr.kerberised": "true",
            "ranger.admin.kerberos.cookie.domain": "{{ranger_host}}", 
            "ranger.kms.service.user.hdfs": "hdfs", 
            "ranger.spnego.kerberos.principal": "HTTP/_HOST@EXAMPLE.COM", 
            "ranger.ldap.ad.url": "{{ranger_ug_ldap_url}}", 
            "ranger.plugins.hive.serviceuser": "hive", 
            "ranger.lookup.kerberos.keytab": "/etc/security/keytabs/rangerlookup.service.keytab", 
            "ranger.plugins.kms.serviceuser": "kms", 
            "ranger.service.https.attrib.ssl.enabled": "false", 
            "ranger.sso.browser.useragent": "Mozilla,chrome", 
            "ranger.jpa.jdbc.url": "jdbc:mysql://c6401.ambari.apache.org:3306/ranger01", 
            "ranger.plugins.hbase.serviceuser": "hbase", 
            "ranger.plugins.hdfs.serviceuser": "hdfs", 
            "ranger.ldap.group.searchbase": "{{ranger_ug_ldap_group_searchbase}}", 
            "ranger.ldap.user.dnpattern": "uid={0},ou=users,dc=xasecure,dc=net", 
            "ranger.plugins.knox.serviceuser": "knox", 
            "ranger.ldap.base.dn": "dc=example,dc=com", 
            "ranger.sso.publicKey": "", 
            "ranger.admin.kerberos.cookie.path": "/", 
            "ranger.service.https.attrib.clientAuth": "want", 
            "ranger.jpa.jdbc.user": "{{ranger_db_user}}", 
            "ranger.ldap.ad.user.searchfilter": "(sAMAccountName={0})", 
            "ranger.ldap.group.roleattribute": "cn", 
            "ranger.plugins.kafka.serviceuser": "kafka", 
            "ranger.admin.kerberos.principal": "rangeradmin/_HOST@EXAMPLE.COM", 
            "ranger.ldap.ad.bind.dn": "{{ranger_ug_ldap_bind_dn}}", 
            "ranger.admin.kerberos.cookie.domain": "{{ranger_host}}",
            "ranger.kms.service.user.hdfs": "hdfs",
            "ranger.spnego.kerberos.principal": "HTTP/_HOST@EXAMPLE.COM",
            "ranger.ldap.ad.url": "{{ranger_ug_ldap_url}}",
            "ranger.plugins.hive.serviceuser": "hive",
            "ranger.lookup.kerberos.keytab": "/etc/security/keytabs/rangerlookup.service.keytab",
            "ranger.plugins.kms.serviceuser": "kms",
            "ranger.service.https.attrib.ssl.enabled": "false",
            "ranger.sso.browser.useragent": "Mozilla,chrome",
            "ranger.jpa.jdbc.url": "jdbc:mysql://c6401.ambari.apache.org:3306/ranger01",
            "ranger.plugins.hbase.serviceuser": "hbase",
            "ranger.plugins.hdfs.serviceuser": "hdfs",
            "ranger.ldap.group.searchbase": "{{ranger_ug_ldap_group_searchbase}}",
            "ranger.ldap.user.dnpattern": "uid={0},ou=users,dc=xasecure,dc=net",
            "ranger.plugins.knox.serviceuser": "knox",
            "ranger.ldap.base.dn": "dc=example,dc=com",
            "ranger.sso.publicKey": "",
            "ranger.admin.kerberos.cookie.path": "/",
            "ranger.service.https.attrib.clientAuth": "want",
            "ranger.jpa.jdbc.user": "{{ranger_db_user}}",
            "ranger.ldap.ad.user.searchfilter": "(sAMAccountName={0})",
            "ranger.ldap.group.roleattribute": "cn",
            "ranger.plugins.kafka.serviceuser": "kafka",
            "ranger.admin.kerberos.principal": "rangeradmin/_HOST@EXAMPLE.COM",
            "ranger.ldap.ad.bind.dn": "{{ranger_ug_ldap_bind_dn}}",
             "ranger.credential.provider.path": "/etc/ranger/admin/rangeradmin.jceks",
            "ranger.ldap.referral": "ignore", 
            "ranger.service.http.port": "6080", 
            "ranger.ldap.user.searchfilter": "(uid={0})", 
            "ranger.plugins.atlas.serviceuser": "atlas", 
            "ranger.truststore.password": "changeit", 
            "ranger.ldap.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}", 
            "ranger.audit.solr.password": "NONE", 
            "ranger.ldap.referral": "ignore",
            "ranger.service.http.port": "6080",
            "ranger.ldap.user.searchfilter": "(uid={0})",
            "ranger.plugins.atlas.serviceuser": "atlas",
            "ranger.truststore.password": "changeit",
            "ranger.ldap.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}",
            "ranger.audit.solr.password": "NONE",
             "ranger.audit.solr.zookeepers": "c6401.ambari.apache.org:2181/infra-solr",
             "ranger.lookup.kerberos.principal": "rangerlookup/_HOST@EXAMPLE.COM",
            "ranger.service.https.port": "6182", 
            "ranger.plugins.storm.serviceuser": "storm", 
            "ranger.externalurl": "{{ranger_external_url}}", 
            "ranger.truststore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks", 
            "ranger.kms.service.user.hive": "", 
            "ranger.https.attrib.keystore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks", 
            "ranger.jpa.jdbc.dialect": "{{jdbc_dialect}}", 
            "ranger.service.host": "{{ranger_host}}", 
            "ranger.service.https.attrib.keystore.keyalias": "rangeradmin", 
            "ranger.service.https.attrib.keystore.pass": "xasecure", 
            "ranger.unixauth.remote.login.enabled": "true", 
            "ranger.service.https.port": "6182",
            "ranger.plugins.storm.serviceuser": "storm",
            "ranger.externalurl": "{{ranger_external_url}}",
            "ranger.truststore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks",
            "ranger.kms.service.user.hive": "",
            "ranger.https.attrib.keystore.file": "/etc/ranger/admin/conf/ranger-admin-keystore.jks",
            "ranger.jpa.jdbc.dialect": "{{jdbc_dialect}}",
            "ranger.service.host": "{{ranger_host}}",
            "ranger.service.https.attrib.keystore.keyalias": "rangeradmin",
            "ranger.service.https.attrib.keystore.pass": "xasecure",
            "ranger.unixauth.remote.login.enabled": "true",
             "ranger.jpa.jdbc.credential.alias": "rangeradmin",
            "ranger.ldap.ad.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}", 
            "ranger.audit.solr.username": "ranger_solr", 
            "ranger.sso.enabled": "false", 
            "ranger.audit.solr.urls": "", 
            "ranger.ldap.ad.domain": "", 
            "ranger.plugins.yarn.serviceuser": "yarn", 
            "ranger.audit.source.type": "solr", 
            "ranger.ldap.bind.dn": "{{ranger_ug_ldap_bind_dn}}", 
            "ranger.ldap.url": "{{ranger_ug_ldap_url}}", 
            "ranger.authentication.method": "UNIX", 
            "ranger.service.http.enabled": "true", 
            "ranger.ldap.group.searchfilter": "{{ranger_ug_ldap_group_searchfilter}}", 
            "ranger.ldap.ad.referral": "ignore", 
            "ranger.ldap.ad.base.dn": "dc=example,dc=com", 
            "ranger.jpa.jdbc.password": "_", 
            "ranger.spnego.kerberos.keytab": "/etc/security/keytabs/spnego.service.keytab", 
            "ranger.sso.providerurl": "", 
            "ranger.unixauth.service.hostname": "{{ugsync_host}}", 
            "ranger.admin.kerberos.keytab": "/etc/security/keytabs/rangeradmin.service.keytab", 
            "ranger.admin.kerberos.token.valid.seconds": "30", 
            "ranger.jpa.jdbc.driver": "com.mysql.jdbc.Driver", 
            "ranger.ldap.ad.bind.password": "{{ranger_usersync_ldap_ldapbindpassword}}",
            "ranger.audit.solr.username": "ranger_solr",
            "ranger.sso.enabled": "false",
            "ranger.audit.solr.urls": "",
            "ranger.ldap.ad.domain": "",
            "ranger.plugins.yarn.serviceuser": "yarn",
            "ranger.audit.source.type": "solr",
            "ranger.ldap.bind.dn": "{{ranger_ug_ldap_bind_dn}}",
            "ranger.ldap.url": "{{ranger_ug_ldap_url}}",
            "ranger.authentication.method": "UNIX",
            "ranger.service.http.enabled": "true",
            "ranger.ldap.group.searchfilter": "{{ranger_ug_ldap_group_searchfilter}}",
            "ranger.ldap.ad.referral": "ignore",
            "ranger.ldap.ad.base.dn": "dc=example,dc=com",
            "ranger.jpa.jdbc.password": "_",
            "ranger.spnego.kerberos.keytab": "/etc/security/keytabs/spnego.service.keytab",
            "ranger.sso.providerurl": "",
            "ranger.unixauth.service.hostname": "{{ugsync_host}}",
            "ranger.admin.kerberos.keytab": "/etc/security/keytabs/rangeradmin.service.keytab",
            "ranger.admin.kerberos.token.valid.seconds": "30",
            "ranger.jpa.jdbc.driver": "com.mysql.jdbc.Driver",
             "ranger.unixauth.service.port": "5151"
        }, 
        },
         "ranger-hdfs-policymgr-ssl": {
            "xasecure.policymgr.clientssl.keystore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-keystore.jks", 
            "xasecure.policymgr.clientssl.truststore.password": "changeit", 
            "xasecure.policymgr.clientssl.keystore.credential.file": "jceks://file{{credential_file}}", 
            "xasecure.policymgr.clientssl.truststore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-truststore.jks", 
            "xasecure.policymgr.clientssl.truststore.credential.file": "jceks://file{{credential_file}}", 
            "xasecure.policymgr.clientssl.keystore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-keystore.jks",
            "xasecure.policymgr.clientssl.truststore.password": "changeit",
            "xasecure.policymgr.clientssl.keystore.credential.file": "jceks://file{{credential_file}}",
            "xasecure.policymgr.clientssl.truststore": "/usr/hdp/current/hadoop-client/conf/ranger-plugin-truststore.jks",
            "xasecure.policymgr.clientssl.truststore.credential.file": "jceks://file{{credential_file}}",
             "xasecure.policymgr.clientssl.keystore.password": "myKeyFilePassword"
        }, 
        },
         "tagsync-application-properties": {
            "atlas.kafka.zookeeper.connect": "c6401.ambari.apache.org:2181", 
            "atlas.kafka.security.protocol": "SASL_PLAINTEXT", 
            "atlas.jaas.KafkaClient.option.principal": "{{tagsync_jaas_principal}}", 
            "atlas.jaas.KafkaClient.option.keyTab": "{{tagsync_keytab_path}}", 
            "atlas.kafka.entities.group.id": "ranger_entities_consumer", 
            "atlas.jaas.KafkaClient.loginModuleName": "com.sun.security.auth.module.Krb5LoginModule", 
            "atlas.jaas.KafkaClient.option.serviceName": "kafka", 
            "atlas.kafka.bootstrap.servers": "localhost:6667", 
            "atlas.jaas.KafkaClient.option.useKeyTab": "true", 
            "atlas.jaas.KafkaClient.option.storeKey": "true", 
            "atlas.jaas.KafkaClient.loginModuleControlFlag": "required", 
            "atlas.kafka.zookeeper.connect": "c6401.ambari.apache.org:2181",
            "atlas.kafka.security.protocol": "SASL_PLAINTEXT",
            "atlas.jaas.KafkaClient.option.principal": "{{tagsync_jaas_principal}}",
            "atlas.jaas.KafkaClient.option.keyTab": "{{tagsync_keytab_path}}",
            "atlas.kafka.entities.group.id": "ranger_entities_consumer",
            "atlas.jaas.KafkaClient.loginModuleName": "com.sun.security.auth.module.Krb5LoginModule",
            "atlas.jaas.KafkaClient.option.serviceName": "kafka",
            "atlas.kafka.bootstrap.servers": "localhost:6667",
            "atlas.jaas.KafkaClient.option.useKeyTab": "true",
            "atlas.jaas.KafkaClient.option.storeKey": "true",
            "atlas.jaas.KafkaClient.loginModuleControlFlag": "required",
             "atlas.kafka.sasl.kerberos.service.name": "kafka"
        }, 
        },
         "ranger-env": {
            "ranger_solr_shards": "1", 
            "ranger_solr_config_set": "ranger_audits", 
            "ranger_user": "ranger", 
            "ranger_solr_shards": "1",
            "ranger_solr_config_set": "ranger_audits",
            "ranger_user": "ranger",
             "ranger_solr_replication_factor": "1",
            "xml_configurations_supported": "true", 
            "ranger-atlas-plugin-enabled": "No", 
            "ranger-hbase-plugin-enabled": "No", 
            "ranger-yarn-plugin-enabled": "No", 
            "bind_anonymous": "false", 
            "ranger_admin_username": "amb_ranger_admin", 
            "admin_password": "admin", 
            "is_solrCloud_enabled": "true", 
            "ranger-storm-plugin-enabled": "No", 
            "ranger-hdfs-plugin-enabled": "No", 
            "ranger_group": "ranger", 
            "ranger-knox-plugin-enabled": "No", 
            "ranger_admin_log_dir": "/var/log/ranger/admin", 
            "ranger-kafka-plugin-enabled": "No", 
            "ranger_privelege_user_jdbc_url": "jdbc:mysql://c6401.ambari.apache.org:3306", 
            "ranger-hive-plugin-enabled": "No", 
            "xasecure.audit.destination.solr": "true", 
            "ranger_pid_dir": "/var/run/ranger", 
            "xasecure.audit.destination.hdfs": "true", 
            "admin_username": "admin", 
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit", 
            "create_db_dbuser": "true", 
            "ranger_solr_collection_name": "ranger_audits", 
            "ranger_admin_password": "P1!q9xa96SMi5NCl", 
            "xml_configurations_supported": "true",
            "ranger-atlas-plugin-enabled": "No",
            "ranger-hbase-plugin-enabled": "No",
            "ranger-yarn-plugin-enabled": "No",
            "bind_anonymous": "false",
            "ranger_admin_username": "amb_ranger_admin",
            "admin_password": "admin",
            "is_solrCloud_enabled": "true",
            "ranger-storm-plugin-enabled": "No",
            "ranger-hdfs-plugin-enabled": "No",
            "ranger_group": "ranger",
            "ranger-knox-plugin-enabled": "No",
            "ranger_admin_log_dir": "/var/log/ranger/admin",
            "ranger-kafka-plugin-enabled": "No",
            "ranger_privelege_user_jdbc_url": "jdbc:mysql://c6401.ambari.apache.org:3306",
            "ranger-hive-plugin-enabled": "No",
            "xasecure.audit.destination.solr": "true",
            "ranger_pid_dir": "/var/run/ranger",
            "xasecure.audit.destination.hdfs": "true",
            "admin_username": "admin",
            "xasecure.audit.destination.hdfs.dir": "hdfs://c6401.ambari.apache.org:8020/ranger/audit",
            "create_db_dbuser": "true",
            "ranger_solr_collection_name": "ranger_audits",
            "ranger_admin_password": "P1!q9xa96SMi5NCl",
             "ranger_usersync_log_dir": "/var/log/ranger/usersync"
        }, 
        },
         "usersync-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#      http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n#\n\nlog4j.rootLogger = info,logFile\n\n# logFile\nlog4j.appender.logFile=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.logFile.file=${logdir}/usersync.log\nlog4j.appender.logFile.datePattern='.'yyyy-MM-dd\nlog4j.appender.logFile.layout=org.apache.log4j.PatternLayout\nlog4j.appender.logFile.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %m%n\n\n# console\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.Target=System.out\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %m%n"
        }, 
        },
         "ranger-hdfs-plugin-properties": {
            "hadoop.rpc.protection": "authentication", 
            "ranger-hdfs-plugin-enabled": "No", 
            "REPOSITORY_CONFIG_USERNAME": "hadoop", 
            "policy_user": "ambari-qa", 
            "common.name.for.certificate": "", 
            "hadoop.rpc.protection": "authentication",
            "ranger-hdfs-plugin-enabled": "No",
            "REPOSITORY_CONFIG_USERNAME": "hadoop",
            "policy_user": "ambari-qa",
            "common.name.for.certificate": "",
             "REPOSITORY_CONFIG_PASSWORD": "hadoop"
        }, 
        },
         "kerberos-env": {
            "kdc_hosts": "c6401.ambari.apache.org", 
            "manage_auth_to_local": "true", 
            "install_packages": "true", 
            "realm": "EXAMPLE.COM", 
            "encryption_types": "aes des3-cbc-sha1 rc4 des-cbc-md5", 
            "ad_create_attributes_template": "\n{\n  \"objectClass\": [\"top\", \"person\", \"organizationalPerson\", \"user\"],\n  \"cn\": \"$principal_name\",\n  #if( $is_service )\n  \"servicePrincipalName\": \"$principal_name\",\n  #end\n  \"userPrincipalName\": \"$normalized_principal\",\n  \"unicodePwd\": \"$password\",\n  \"accountExpires\": \"0\",\n  \"userAccountControl\": \"66048\"\n}", 
            "kdc_create_attributes": "", 
            "admin_server_host": "c6401.ambari.apache.org", 
            "group": "ambari-managed-principals", 
            "password_length": "20", 
            "ldap_url": "", 
            "manage_identities": "true", 
            "password_min_lowercase_letters": "1", 
            "create_ambari_principal": "true", 
            "service_check_principal_name": "${cluster_name|toLower()}-${short_date}", 
            "executable_search_paths": "/usr/bin, /usr/kerberos/bin, /usr/sbin, /usr/lib/mit/bin, /usr/lib/mit/sbin", 
            "password_chat_timeout": "5", 
            "kdc_type": "mit-kdc", 
            "set_password_expiry": "false", 
            "password_min_punctuation": "1", 
            "container_dn": "", 
            "case_insensitive_username_rules": "false", 
            "password_min_whitespace": "0", 
            "password_min_uppercase_letters": "1", 
            "kdc_hosts": "c6401.ambari.apache.org",
            "manage_auth_to_local": "true",
            "install_packages": "true",
            "realm": "EXAMPLE.COM",
            "encryption_types": "aes des3-cbc-sha1 rc4 des-cbc-md5",
            "ad_create_attributes_template": "\n{\n  \"objectClass\": [\"top\", \"person\", \"organizationalPerson\", \"user\"],\n  \"cn\": \"$principal_name\",\n  #if( $is_service )\n  \"servicePrincipalName\": \"$principal_name\",\n  #end\n  \"userPrincipalName\": \"$normalized_principal\",\n  \"unicodePwd\": \"$password\",\n  \"accountExpires\": \"0\",\n  \"userAccountControl\": \"66048\"\n}",
            "kdc_create_attributes": "",
            "admin_server_host": "c6401.ambari.apache.org",
            "group": "ambari-managed-principals",
            "password_length": "20",
            "ldap_url": "",
            "manage_identities": "true",
            "password_min_lowercase_letters": "1",
            "create_ambari_principal": "true",
            "service_check_principal_name": "${cluster_name|toLower()}-${short_date}",
            "executable_search_paths": "/usr/bin, /usr/kerberos/bin, /usr/sbin, /usr/lib/mit/bin, /usr/lib/mit/sbin",
            "password_chat_timeout": "5",
            "kdc_type": "mit-kdc",
            "set_password_expiry": "false",
            "password_min_punctuation": "1",
            "container_dn": "",
            "case_insensitive_username_rules": "false",
            "password_min_whitespace": "0",
            "password_min_uppercase_letters": "1",
             "password_min_digits": "1"
        }, 
        },
         "admin-properties": {
            "db_user": "rangeradmin01", 
            "DB_FLAVOR": "MYSQL", 
            "db_password": "rangeradmin01", 
            "db_root_user": "root", 
            "policymgr_external_url": "http://c6401.ambari.apache.org:6080", 
            "db_name": "ranger01", 
            "db_host": "c6401.ambari.apache.org", 
            "db_root_password": "vagrant", 
            "db_user": "rangeradmin01",
            "DB_FLAVOR": "MYSQL",
            "db_password": "rangeradmin01",
            "db_root_user": "root",
            "policymgr_external_url": "http://c6401.ambari.apache.org:6080",
            "db_name": "ranger01",
            "db_host": "c6401.ambari.apache.org",
            "db_root_password": "vagrant",
             "SQL_CONNECTOR_JAR": "{{driver_curl_target}}"
         },
         "ranger-solr-configuration": {
@@ -484,261 +484,261 @@
             "ranger_audit_logs_merge_factor": "5"
         },
         "ranger-ugsync-site": {
            "ranger.usersync.ldap.binddn": "", 
            "ranger.usersync.policymgr.username": "rangerusersync", 
            "ranger.usersync.policymanager.mockrun": "false", 
            "ranger.usersync.group.searchbase": "", 
            "ranger.usersync.ldap.bindalias": "testldapalias", 
            "ranger.usersync.truststore.file": "/usr/hdp/current/ranger-usersync/conf/mytruststore.jks", 
            "ranger.usersync.port": "5151", 
            "ranger.usersync.pagedresultssize": "500", 
            "ranger.usersync.group.memberattributename": "", 
            "ranger.usersync.kerberos.principal": "rangerusersync/_HOST@EXAMPLE.COM", 
            "ranger.usersync.source.impl.class": "org.apache.ranger.unixusersync.process.UnixUserGroupBuilder", 
            "ranger.usersync.ldap.referral": "ignore", 
            "ranger.usersync.group.searchfilter": "", 
            "ranger.usersync.ldap.user.objectclass": "person", 
            "ranger.usersync.logdir": "{{usersync_log_dir}}", 
            "ranger.usersync.ldap.user.searchfilter": "", 
            "ranger.usersync.ldap.groupname.caseconversion": "none", 
            "ranger.usersync.ldap.ldapbindpassword": "", 
            "ranger.usersync.unix.minUserId": "500", 
            "ranger.usersync.policymanager.maxrecordsperapicall": "1000", 
            "ranger.usersync.group.nameattribute": "", 
            "ranger.usersync.policymgr.alias": "ranger.usersync.policymgr.password", 
            "ranger.usersync.keystore.file": "/usr/hdp/current/ranger-usersync/conf/unixauthservice.jks", 
            "ranger.usersync.user.searchenabled": "false", 
            "ranger.usersync.group.usermapsyncenabled": "true", 
            "ranger.usersync.ldap.bindkeystore": "", 
            "ranger.usersync.ldap.user.groupnameattribute": "memberof, ismemberof", 
            "ranger.usersync.kerberos.keytab": "/etc/security/keytabs/rangerusersync.service.keytab", 
            "ranger.usersync.passwordvalidator.path": "./native/credValidator.uexe", 
            "ranger.usersync.group.objectclass": "", 
            "ranger.usersync.ldap.user.searchscope": "sub", 
            "ranger.usersync.unix.password.file": "/etc/passwd", 
            "ranger.usersync.ldap.user.nameattribute": "", 
            "ranger.usersync.pagedresultsenabled": "true", 
            "ranger.usersync.policymanager.baseURL": "{{ranger_external_url}}", 
            "ranger.usersync.group.search.first.enabled": "false", 
            "ranger.usersync.group.searchenabled": "false", 
            "ranger.usersync.sink.impl.class": "org.apache.ranger.unixusersync.process.PolicyMgrUserGroupBuilder", 
            "ranger.usersync.ssl": "true", 
            "ranger.usersync.ldap.url": "", 
            "ranger.usersync.ldap.searchBase": "dc=hadoop,dc=apache,dc=org", 
            "ranger.usersync.policymgr.keystore": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks", 
            "ranger.usersync.ldap.user.searchbase": "", 
            "ranger.usersync.ldap.username.caseconversion": "none", 
            "ranger.usersync.credstore.filename": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks", 
            "ranger.usersync.keystore.password": "UnIx529p", 
            "ranger.usersync.unix.group.file": "/etc/group", 
            "ranger.usersync.filesource.file": "/tmp/usergroup.txt", 
            "ranger.usersync.group.searchscope": "", 
            "ranger.usersync.truststore.password": "changeit", 
            "ranger.usersync.enabled": "true", 
            "ranger.usersync.sleeptimeinmillisbetweensynccycle": "60000", 
            "ranger.usersync.ldap.binddn": "",
            "ranger.usersync.policymgr.username": "rangerusersync",
            "ranger.usersync.policymanager.mockrun": "false",
            "ranger.usersync.group.searchbase": "",
            "ranger.usersync.ldap.bindalias": "testldapalias",
            "ranger.usersync.truststore.file": "/usr/hdp/current/ranger-usersync/conf/mytruststore.jks",
            "ranger.usersync.port": "5151",
            "ranger.usersync.pagedresultssize": "500",
            "ranger.usersync.group.memberattributename": "",
            "ranger.usersync.kerberos.principal": "rangerusersync/_HOST@EXAMPLE.COM",
            "ranger.usersync.source.impl.class": "org.apache.ranger.unixusersync.process.UnixUserGroupBuilder",
            "ranger.usersync.ldap.referral": "ignore",
            "ranger.usersync.group.searchfilter": "",
            "ranger.usersync.ldap.user.objectclass": "person",
            "ranger.usersync.logdir": "{{usersync_log_dir}}",
            "ranger.usersync.ldap.user.searchfilter": "",
            "ranger.usersync.ldap.groupname.caseconversion": "none",
            "ranger.usersync.ldap.ldapbindpassword": "",
            "ranger.usersync.unix.minUserId": "500",
            "ranger.usersync.policymanager.maxrecordsperapicall": "1000",
            "ranger.usersync.group.nameattribute": "",
            "ranger.usersync.policymgr.alias": "ranger.usersync.policymgr.password",
            "ranger.usersync.keystore.file": "/usr/hdp/current/ranger-usersync/conf/unixauthservice.jks",
            "ranger.usersync.user.searchenabled": "false",
            "ranger.usersync.group.usermapsyncenabled": "true",
            "ranger.usersync.ldap.bindkeystore": "",
            "ranger.usersync.ldap.user.groupnameattribute": "memberof, ismemberof",
            "ranger.usersync.kerberos.keytab": "/etc/security/keytabs/rangerusersync.service.keytab",
            "ranger.usersync.passwordvalidator.path": "./native/credValidator.uexe",
            "ranger.usersync.group.objectclass": "",
            "ranger.usersync.ldap.user.searchscope": "sub",
            "ranger.usersync.unix.password.file": "/etc/passwd",
            "ranger.usersync.ldap.user.nameattribute": "",
            "ranger.usersync.pagedresultsenabled": "true",
            "ranger.usersync.policymanager.baseURL": "{{ranger_external_url}}",
            "ranger.usersync.group.search.first.enabled": "false",
            "ranger.usersync.group.searchenabled": "false",
            "ranger.usersync.sink.impl.class": "org.apache.ranger.unixusersync.process.PolicyMgrUserGroupBuilder",
            "ranger.usersync.ssl": "true",
            "ranger.usersync.ldap.url": "",
            "ranger.usersync.ldap.searchBase": "dc=hadoop,dc=apache,dc=org",
            "ranger.usersync.policymgr.keystore": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks",
            "ranger.usersync.ldap.user.searchbase": "",
            "ranger.usersync.ldap.username.caseconversion": "none",
            "ranger.usersync.credstore.filename": "/usr/hdp/current/ranger-usersync/conf/ugsync.jceks",
            "ranger.usersync.keystore.password": "UnIx529p",
            "ranger.usersync.unix.group.file": "/etc/group",
            "ranger.usersync.filesource.file": "/tmp/usergroup.txt",
            "ranger.usersync.group.searchscope": "",
            "ranger.usersync.truststore.password": "changeit",
            "ranger.usersync.enabled": "true",
            "ranger.usersync.sleeptimeinmillisbetweensynccycle": "60000",
             "ranger.usersync.filesource.text.delimiter": ","
        }, 
        },
         "hdfs-site": {
            "dfs.namenode.checkpoint.period": "21600", 
            "dfs.namenode.avoid.write.stale.datanode": "true", 
            "dfs.permissions.superusergroup": "hdfs", 
            "dfs.namenode.startup.delay.block.deletion.sec": "3600", 
            "dfs.namenode.kerberos.internal.spnego.principal": "HTTP/_HOST@EXAMPLE.COM", 
            "dfs.datanode.kerberos.principal": "dn/_HOST@EXAMPLE.COM", 
            "dfs.heartbeat.interval": "3", 
            "dfs.content-summary.limit": "5000", 
            "dfs.support.append": "true", 
            "dfs.datanode.address": "0.0.0.0:1019", 
            "dfs.cluster.administrators": " hdfs", 
            "dfs.namenode.audit.log.async": "true", 
            "dfs.datanode.balance.bandwidthPerSec": "6250000", 
            "dfs.namenode.safemode.threshold-pct": "1", 
            "dfs.namenode.checkpoint.edits.dir": "${dfs.namenode.checkpoint.dir}", 
            "dfs.namenode.rpc-address": "c6401.ambari.apache.org:8020", 
            "dfs.permissions.enabled": "true", 
            "dfs.namenode.kerberos.principal": "nn/_HOST@EXAMPLE.COM", 
            "dfs.client.read.shortcircuit": "true", 
            "dfs.https.port": "50470", 
            "dfs.namenode.https-address": "c6401.ambari.apache.org:50470", 
            "nfs.file.dump.dir": "/tmp/.hdfs-nfs", 
            "dfs.blocksize": "134217728", 
            "dfs.blockreport.initialDelay": "120", 
            "dfs.journalnode.edits.dir": "/hadoop/hdfs/journalnode", 
            "dfs.namenode.fslock.fair": "false", 
            "dfs.datanode.max.transfer.threads": "4096", 
            "dfs.secondary.namenode.kerberos.internal.spnego.principal": "HTTP/_HOST@EXAMPLE.COM", 
            "dfs.replication": "3", 
            "dfs.namenode.handler.count": "50", 
            "dfs.web.authentication.kerberos.keytab": "/etc/security/keytabs/spnego.service.keytab", 
            "fs.permissions.umask-mode": "022", 
            "dfs.namenode.stale.datanode.interval": "30000", 
            "dfs.datanode.ipc.address": "0.0.0.0:8010", 
            "dfs.datanode.failed.volumes.tolerated": "0", 
            "dfs.datanode.data.dir": "/grid/0/hadoop/hdfs/data", 
            "dfs.namenode.http-address": "c6401.ambari.apache.org:50070", 
            "dfs.webhdfs.enabled": "true", 
            "dfs.encrypt.data.transfer.cipher.suites": "AES/CTR/NoPadding", 
            "dfs.namenode.accesstime.precision": "0", 
            "dfs.namenode.write.stale.datanode.ratio": "1.0f", 
            "dfs.datanode.https.address": "0.0.0.0:50475", 
            "dfs.namenode.checkpoint.dir": "/grid/0/hadoop/hdfs/namesecondary", 
            "dfs.namenode.secondary.http-address": "c6401.ambari.apache.org:50090", 
            "nfs.exports.allowed.hosts": "* rw", 
            "dfs.namenode.checkpoint.txns": "1000000", 
            "dfs.datanode.http.address": "0.0.0.0:1022", 
            "dfs.datanode.du.reserved": "33011188224", 
            "dfs.client.read.shortcircuit.streams.cache.size": "4096", 
            "dfs.secondary.namenode.keytab.file": "/etc/security/keytabs/nn.service.keytab", 
            "dfs.web.authentication.kerberos.principal": "HTTP/_HOST@EXAMPLE.COM", 
            "dfs.http.policy": "HTTP_ONLY", 
            "dfs.block.access.token.enable": "true", 
            "dfs.client.retry.policy.enabled": "false", 
            "dfs.secondary.namenode.kerberos.principal": "nn/_HOST@EXAMPLE.COM", 
            "dfs.datanode.keytab.file": "/etc/security/keytabs/dn.service.keytab", 
            "dfs.namenode.name.dir.restore": "true", 
            "dfs.namenode.keytab.file": "/etc/security/keytabs/nn.service.keytab", 
            "dfs.journalnode.https-address": "0.0.0.0:8481", 
            "dfs.journalnode.http-address": "0.0.0.0:8480", 
            "dfs.domain.socket.path": "/var/lib/hadoop-hdfs/dn_socket", 
            "dfs.namenode.avoid.read.stale.datanode": "true", 
            "dfs.hosts.exclude": "/etc/hadoop/conf/dfs.exclude", 
            "dfs.datanode.data.dir.perm": "750", 
            "dfs.encryption.key.provider.uri": "kms://http@c6401.ambari.apache.org:9292/kms", 
            "dfs.replication.max": "50", 
            "dfs.namenode.checkpoint.period": "21600",
            "dfs.namenode.avoid.write.stale.datanode": "true",
            "dfs.permissions.superusergroup": "hdfs",
            "dfs.namenode.startup.delay.block.deletion.sec": "3600",
            "dfs.namenode.kerberos.internal.spnego.principal": "HTTP/_HOST@EXAMPLE.COM",
            "dfs.datanode.kerberos.principal": "dn/_HOST@EXAMPLE.COM",
            "dfs.heartbeat.interval": "3",
            "dfs.content-summary.limit": "5000",
            "dfs.support.append": "true",
            "dfs.datanode.address": "0.0.0.0:1019",
            "dfs.cluster.administrators": " hdfs",
            "dfs.namenode.audit.log.async": "true",
            "dfs.datanode.balance.bandwidthPerSec": "6250000",
            "dfs.namenode.safemode.threshold-pct": "1",
            "dfs.namenode.checkpoint.edits.dir": "${dfs.namenode.checkpoint.dir}",
            "dfs.namenode.rpc-address": "c6401.ambari.apache.org:8020",
            "dfs.permissions.enabled": "true",
            "dfs.namenode.kerberos.principal": "nn/_HOST@EXAMPLE.COM",
            "dfs.client.read.shortcircuit": "true",
            "dfs.https.port": "50470",
            "dfs.namenode.https-address": "c6401.ambari.apache.org:50470",
            "nfs.file.dump.dir": "/tmp/.hdfs-nfs",
            "dfs.blocksize": "134217728",
            "dfs.blockreport.initialDelay": "120",
            "dfs.journalnode.edits.dir": "/hadoop/hdfs/journalnode",
            "dfs.namenode.fslock.fair": "false",
            "dfs.datanode.max.transfer.threads": "4096",
            "dfs.secondary.namenode.kerberos.internal.spnego.principal": "HTTP/_HOST@EXAMPLE.COM",
            "dfs.replication": "3",
            "dfs.namenode.handler.count": "50",
            "dfs.web.authentication.kerberos.keytab": "/etc/security/keytabs/spnego.service.keytab",
            "fs.permissions.umask-mode": "022",
            "dfs.namenode.stale.datanode.interval": "30000",
            "dfs.datanode.ipc.address": "0.0.0.0:8010",
            "dfs.datanode.failed.volumes.tolerated": "0",
            "dfs.datanode.data.dir": "/grid/0/hadoop/hdfs/data",
            "dfs.namenode.http-address": "c6401.ambari.apache.org:50070",
            "dfs.webhdfs.enabled": "true",
            "dfs.encrypt.data.transfer.cipher.suites": "AES/CTR/NoPadding",
            "dfs.namenode.accesstime.precision": "0",
            "dfs.namenode.write.stale.datanode.ratio": "1.0f",
            "dfs.datanode.https.address": "0.0.0.0:50475",
            "dfs.namenode.checkpoint.dir": "/grid/0/hadoop/hdfs/namesecondary",
            "dfs.namenode.secondary.http-address": "c6401.ambari.apache.org:50090",
            "nfs.exports.allowed.hosts": "* rw",
            "dfs.namenode.checkpoint.txns": "1000000",
            "dfs.datanode.http.address": "0.0.0.0:1022",
            "dfs.datanode.du.reserved": "33011188224",
            "dfs.client.read.shortcircuit.streams.cache.size": "4096",
            "dfs.secondary.namenode.keytab.file": "/etc/security/keytabs/nn.service.keytab",
            "dfs.web.authentication.kerberos.principal": "HTTP/_HOST@EXAMPLE.COM",
            "dfs.http.policy": "HTTP_ONLY",
            "dfs.block.access.token.enable": "true",
            "dfs.client.retry.policy.enabled": "false",
            "dfs.secondary.namenode.kerberos.principal": "nn/_HOST@EXAMPLE.COM",
            "dfs.datanode.keytab.file": "/etc/security/keytabs/dn.service.keytab",
            "dfs.namenode.name.dir.restore": "true",
            "dfs.namenode.keytab.file": "/etc/security/keytabs/nn.service.keytab",
            "dfs.journalnode.https-address": "0.0.0.0:8481",
            "dfs.journalnode.http-address": "0.0.0.0:8480",
            "dfs.domain.socket.path": "/var/lib/hadoop-hdfs/dn_socket",
            "dfs.namenode.avoid.read.stale.datanode": "true",
            "dfs.hosts.exclude": "/etc/hadoop/conf/dfs.exclude",
            "dfs.datanode.data.dir.perm": "750",
            "dfs.encryption.key.provider.uri": "kms://http@c6401.ambari.apache.org:9292/kms",
            "dfs.replication.max": "50",
             "dfs.namenode.name.dir": "/grid/0/hadoop/hdfs/namenode"
        }, 
        },
         "ranger-tagsync-site": {
             "ranger.tagsync.dest.ranger.ssl.config.filename": "{{stack_root}}/current/ranger-tagsync/conf/ranger-policymgr-ssl.xml",
             "ranger.tagsync.source.atlasrest.username": "",
             "ranger.tagsync.logdir": "/var/log/ranger/tagsync",
            "ranger.tagsync.source.atlasrest.download.interval.millis": "", 
            "ranger.tagsync.keystore.filename": "/usr/hdp/current/ranger-tagsync/conf/rangertagsync.jceks", 
            "ranger.tagsync.source.file.check.interval.millis": "", 
            "ranger.tagsync.source.atlasrest.endpoint": "", 
            "ranger.tagsync.dest.ranger.username": "rangertagsync", 
            "ranger.tagsync.dest.ranger.endpoint": "{{ranger_external_url}}", 
            "ranger.tagsync.kerberos.principal": "rangertagsync/_HOST@EXAMPLE.COM", 
            "ranger.tagsync.kerberos.keytab": "/etc/security/keytabs/rangertagsync.service.keytab", 
            "ranger.tagsync.source.atlas": "false", 
            "ranger.tagsync.source.atlasrest": "false", 
            "ranger.tagsync.source.file": "false", 
            "ranger.tagsync.source.atlasrest.download.interval.millis": "",
            "ranger.tagsync.keystore.filename": "/usr/hdp/current/ranger-tagsync/conf/rangertagsync.jceks",
            "ranger.tagsync.source.file.check.interval.millis": "",
            "ranger.tagsync.source.atlasrest.endpoint": "",
            "ranger.tagsync.dest.ranger.username": "rangertagsync",
            "ranger.tagsync.dest.ranger.endpoint": "{{ranger_external_url}}",
            "ranger.tagsync.kerberos.principal": "rangertagsync/_HOST@EXAMPLE.COM",
            "ranger.tagsync.kerberos.keytab": "/etc/security/keytabs/rangertagsync.service.keytab",
            "ranger.tagsync.source.atlas": "false",
            "ranger.tagsync.source.atlasrest": "false",
            "ranger.tagsync.source.file": "false",
             "ranger.tagsync.source.file.filename": ""
        }, 
        },
         "zoo.cfg": {
            "clientPort": "2181", 
            "autopurge.purgeInterval": "24", 
            "syncLimit": "5", 
            "dataDir": "/grid/0/hadoop/zookeeper", 
            "initLimit": "10", 
            "tickTime": "2000", 
            "clientPort": "2181",
            "autopurge.purgeInterval": "24",
            "syncLimit": "5",
            "dataDir": "/grid/0/hadoop/zookeeper",
            "initLimit": "10",
            "tickTime": "2000",
             "autopurge.snapRetainCount": "30"
        }, 
        },
         "hadoop-policy": {
            "security.job.client.protocol.acl": "*", 
            "security.job.task.protocol.acl": "*", 
            "security.datanode.protocol.acl": "*", 
            "security.namenode.protocol.acl": "*", 
            "security.client.datanode.protocol.acl": "*", 
            "security.inter.tracker.protocol.acl": "*", 
            "security.refresh.usertogroups.mappings.protocol.acl": "hadoop", 
            "security.client.protocol.acl": "*", 
            "security.refresh.policy.protocol.acl": "hadoop", 
            "security.admin.operations.protocol.acl": "hadoop", 
            "security.job.client.protocol.acl": "*",
            "security.job.task.protocol.acl": "*",
            "security.datanode.protocol.acl": "*",
            "security.namenode.protocol.acl": "*",
            "security.client.datanode.protocol.acl": "*",
            "security.inter.tracker.protocol.acl": "*",
            "security.refresh.usertogroups.mappings.protocol.acl": "hadoop",
            "security.client.protocol.acl": "*",
            "security.refresh.policy.protocol.acl": "hadoop",
            "security.admin.operations.protocol.acl": "hadoop",
             "security.inter.datanode.protocol.acl": "*"
        }, 
        },
         "hdfs-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#  http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing,\n# software distributed under the License is distributed on an\n# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n# KIND, either express or implied.  See the License for the\n# specific language governing permissions and limitations\n# under the License.\n#\n\n\n# Define some default values that can be overridden by system properties\n# To change daemon root logger use hadoop_root_logger in hadoop-env\nhadoop.root.logger=INFO,console\nhadoop.log.dir=.\nhadoop.log.file=hadoop.log\n\n\n# Define the root logger to the system property \"hadoop.root.logger\".\nlog4j.rootLogger=${hadoop.root.logger}, EventCounter\n\n# Logging Threshold\nlog4j.threshhold=ALL\n\n#\n# Daily Rolling File Appender\n#\n\nlog4j.appender.DRFA=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.DRFA.File=${hadoop.log.dir}/${hadoop.log.file}\n\n# Rollver at midnight\nlog4j.appender.DRFA.DatePattern=.yyyy-MM-dd\n\n# 30-day backup\n#log4j.appender.DRFA.MaxBackupIndex=30\nlog4j.appender.DRFA.layout=org.apache.log4j.PatternLayout\n\n# Pattern format: Date LogLevel LoggerName LogMessage\nlog4j.appender.DRFA.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\n# Debugging Pattern format\n#log4j.appender.DRFA.layout.ConversionPattern=%d{ISO8601} %-5p %c{2} (%F:%M(%L)) - %m%n\n\n\n#\n# console\n# Add \"console\" to rootlogger above if you want to use this\n#\n\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.target=System.err\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{yy/MM/dd HH:mm:ss} %p %c{2}: %m%n\n\n#\n# TaskLog Appender\n#\n\n#Default values\nhadoop.tasklog.taskid=null\nhadoop.tasklog.iscleanup=false\nhadoop.tasklog.noKeepSplits=4\nhadoop.tasklog.totalLogFileSize=100\nhadoop.tasklog.purgeLogSplits=true\nhadoop.tasklog.logsRetainHours=12\n\nlog4j.appender.TLA=org.apache.hadoop.mapred.TaskLogAppender\nlog4j.appender.TLA.taskId=${hadoop.tasklog.taskid}\nlog4j.appender.TLA.isCleanup=${hadoop.tasklog.iscleanup}\nlog4j.appender.TLA.totalLogFileSize=${hadoop.tasklog.totalLogFileSize}\n\nlog4j.appender.TLA.layout=org.apache.log4j.PatternLayout\nlog4j.appender.TLA.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\n\n#\n#Security audit appender\n#\nhadoop.security.logger=INFO,console\nhadoop.security.log.maxfilesize=256MB\nhadoop.security.log.maxbackupindex=20\nlog4j.category.SecurityLogger=${hadoop.security.logger}\nhadoop.security.log.file=SecurityAuth.audit\nlog4j.appender.DRFAS=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.DRFAS.File=${hadoop.log.dir}/${hadoop.security.log.file}\nlog4j.appender.DRFAS.layout=org.apache.log4j.PatternLayout\nlog4j.appender.DRFAS.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\nlog4j.appender.DRFAS.DatePattern=.yyyy-MM-dd\n\nlog4j.appender.RFAS=org.apache.log4j.RollingFileAppender\nlog4j.appender.RFAS.File=${hadoop.log.dir}/${hadoop.security.log.file}\nlog4j.appender.RFAS.layout=org.apache.log4j.PatternLayout\nlog4j.appender.RFAS.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\nlog4j.appender.RFAS.MaxFileSize=${hadoop.security.log.maxfilesize}\nlog4j.appender.RFAS.MaxBackupIndex=${hadoop.security.log.maxbackupindex}\n\n#\n# hdfs audit logging\n#\nhdfs.audit.logger=INFO,console\nlog4j.logger.org.apache.hadoop.hdfs.server.namenode.FSNamesystem.audit=${hdfs.audit.logger}\nlog4j.additivity.org.apache.hadoop.hdfs.server.namenode.FSNamesystem.audit=false\nlog4j.appender.DRFAAUDIT=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.DRFAAUDIT.File=${hadoop.log.dir}/hdfs-audit.log\nlog4j.appender.DRFAAUDIT.layout=org.apache.log4j.PatternLayout\nlog4j.appender.DRFAAUDIT.layout.ConversionPattern=%d{ISO8601} %p %c{2}: %m%n\nlog4j.appender.DRFAAUDIT.DatePattern=.yyyy-MM-dd\n\n#\n# NameNode metrics logging.\n# The default is to retain two namenode-metrics.log files up to 64MB each.\n#\nnamenode.metrics.logger=INFO,NullAppender\nlog4j.logger.NameNodeMetricsLog=${namenode.metrics.logger}\nlog4j.additivity.NameNodeMetricsLog=false\nlog4j.appender.NNMETRICSRFA=org.apache.log4j.RollingFileAppender\nlog4j.appender.NNMETRICSRFA.File=${hadoop.log.dir}/namenode-metrics.log\nlog4j.appender.NNMETRICSRFA.layout=org.apache.log4j.PatternLayout\nlog4j.appender.NNMETRICSRFA.layout.ConversionPattern=%d{ISO8601} %m%n\nlog4j.appender.NNMETRICSRFA.MaxBackupIndex=1\nlog4j.appender.NNMETRICSRFA.MaxFileSize=64MB\n\n#\n# mapred audit logging\n#\nmapred.audit.logger=INFO,console\nlog4j.logger.org.apache.hadoop.mapred.AuditLogger=${mapred.audit.logger}\nlog4j.additivity.org.apache.hadoop.mapred.AuditLogger=false\nlog4j.appender.MRAUDIT=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.MRAUDIT.File=${hadoop.log.dir}/mapred-audit.log\nlog4j.appender.MRAUDIT.layout=org.apache.log4j.PatternLayout\nlog4j.appender.MRAUDIT.layout.ConversionPattern=%d{ISO8601} %p %c{2}: %m%n\nlog4j.appender.MRAUDIT.DatePattern=.yyyy-MM-dd\n\n#\n# Rolling File Appender\n#\n\nlog4j.appender.RFA=org.apache.log4j.RollingFileAppender\nlog4j.appender.RFA.File=${hadoop.log.dir}/${hadoop.log.file}\n\n# Logfile size and and 30-day backups\nlog4j.appender.RFA.MaxFileSize=256MB\nlog4j.appender.RFA.MaxBackupIndex=10\n\nlog4j.appender.RFA.layout=org.apache.log4j.PatternLayout\nlog4j.appender.RFA.layout.ConversionPattern=%d{ISO8601} %-5p %c{2} - %m%n\nlog4j.appender.RFA.layout.ConversionPattern=%d{ISO8601} %-5p %c{2} (%F:%M(%L)) - %m%n\n\n\n# Custom Logging levels\n\nhadoop.metrics.log.level=INFO\n#log4j.logger.org.apache.hadoop.mapred.JobTracker=DEBUG\n#log4j.logger.org.apache.hadoop.mapred.TaskTracker=DEBUG\n#log4j.logger.org.apache.hadoop.fs.FSNamesystem=DEBUG\nlog4j.logger.org.apache.hadoop.metrics2=${hadoop.metrics.log.level}\n\n# Jets3t library\nlog4j.logger.org.jets3t.service.impl.rest.httpclient.RestS3Service=ERROR\n\n#\n# Null Appender\n# Trap security logger on the hadoop client side\n#\nlog4j.appender.NullAppender=org.apache.log4j.varia.NullAppender\n\n#\n# Event Counter Appender\n# Sends counts of logging messages at different severity levels to Hadoop Metrics.\n#\nlog4j.appender.EventCounter=org.apache.hadoop.log.metrics.EventCounter\n\n# Removes \"deprecated\" messages\nlog4j.logger.org.apache.hadoop.conf.Configuration.deprecation=WARN\n\n#\n# HDFS block state change log from block manager\n#\n# Uncomment the following to suppress normal block state change\n# messages from BlockManager in NameNode.\n#log4j.logger.BlockStateChange=WARN"
        }, 
        },
         "krb5-conf": {
            "domains": "", 
            "manage_krb5_conf": "true", 
            "content": "\n[libdefaults]\n  renew_lifetime = 7d\n  forwardable = true\n  default_realm = {{realm}}\n  ticket_lifetime = 24h\n  dns_lookup_realm = false\n  dns_lookup_kdc = false\n  default_ccache_name = /tmp/krb5cc_%{uid}\n  #default_tgs_enctypes = {{encryption_types}}\n  #default_tkt_enctypes = {{encryption_types}}\n{% if domains %}\n[domain_realm]\n{%- for domain in domains.split(',') %}\n  {{domain|trim()}} = {{realm}}\n{%- endfor %}\n{% endif %}\n[logging]\n  default = FILE:/var/log/krb5kdc.log\n  admin_server = FILE:/var/log/kadmind.log\n  kdc = FILE:/var/log/krb5kdc.log\n\n[realms]\n  {{realm}} = {\n{%- if kdc_hosts > 0 -%}\n{%- set kdc_host_list = kdc_hosts.split(',')  -%}\n{%- if kdc_host_list and kdc_host_list|length > 0 %}\n    admin_server = {{admin_server_host|default(kdc_host_list[0]|trim(), True)}}\n{%- if kdc_host_list -%}\n{% for kdc_host in kdc_host_list %}\n    kdc = {{kdc_host|trim()}}\n{%- endfor -%}\n{% endif %}\n{%- endif %}\n{%- endif %}\n  }\n\n{# Append additional realm declarations below #}", 
            "domains": "",
            "manage_krb5_conf": "true",
            "content": "\n[libdefaults]\n  renew_lifetime = 7d\n  forwardable = true\n  default_realm = {{realm}}\n  ticket_lifetime = 24h\n  dns_lookup_realm = false\n  dns_lookup_kdc = false\n  default_ccache_name = /tmp/krb5cc_%{uid}\n  #default_tgs_enctypes = {{encryption_types}}\n  #default_tkt_enctypes = {{encryption_types}}\n{% if domains %}\n[domain_realm]\n{%- for domain in domains.split(',') %}\n  {{domain|trim()}} = {{realm}}\n{%- endfor %}\n{% endif %}\n[logging]\n  default = FILE:/var/log/krb5kdc.log\n  admin_server = FILE:/var/log/kadmind.log\n  kdc = FILE:/var/log/krb5kdc.log\n\n[realms]\n  {{realm}} = {\n{%- if kdc_hosts > 0 -%}\n{%- set kdc_host_list = kdc_hosts.split(',')  -%}\n{%- if kdc_host_list and kdc_host_list|length > 0 %}\n    admin_server = {{admin_server_host|default(kdc_host_list[0]|trim(), True)}}\n{%- if kdc_host_list -%}\n{% for kdc_host in kdc_host_list %}\n    kdc = {{kdc_host|trim()}}\n{%- endfor -%}\n{% endif %}\n{%- endif %}\n{%- endif %}\n  }\n\n{# Append additional realm declarations below #}",
             "conf_dir": "/etc"
        }, 
        },
         "core-site": {
            "net.topology.script.file.name": "/etc/hadoop/conf/topology_script.py", 
            "hadoop.proxyuser.hdfs.groups": "*", 
            "fs.trash.interval": "360", 
            "ipc.server.tcpnodelay": "true", 
            "io.compression.codecs": "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec,org.apache.hadoop.io.compress.SnappyCodec", 
            "ipc.client.idlethreshold": "8000", 
            "io.file.buffer.size": "131072", 
            "hadoop.proxyuser.ambari-server-test_cluster01.groups": "*", 
            "io.serializations": "org.apache.hadoop.io.serializer.WritableSerialization", 
            "hadoop.security.authentication": "kerberos", 
            "mapreduce.jobtracker.webinterface.trusted": "false", 
            "hadoop.proxyuser.hdfs.hosts": "*", 
            "hadoop.proxyuser.HTTP.groups": "users", 
            "fs.defaultFS": "hdfs://c6401.ambari.apache.org:8020", 
            "ha.failover-controller.active-standby-elector.zk.op.retries": "120", 
            "hadoop.security.key.provider.path": "kms://http@c6401.ambari.apache.org:9292/kms", 
            "hadoop.security.authorization": "true", 
            "hadoop.http.authentication.simple.anonymous.allowed": "true", 
            "ipc.client.connect.max.retries": "50", 
            "hadoop.security.auth_to_local": "RULE:[1:$1@$0](ambari-qa-test_cluster01@EXAMPLE.COM)s/.*/ambari-qa/\nRULE:[1:$1@$0](hdfs-test_cluster01@EXAMPLE.COM)s/.*/hdfs/\nRULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\nRULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\nRULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\nRULE:[2:$1@$0](rangeradmin@EXAMPLE.COM)s/.*/ranger/\nRULE:[2:$1@$0](rangertagsync@EXAMPLE.COM)s/.*/rangertagsync/\nRULE:[2:$1@$0](rangerusersync@EXAMPLE.COM)s/.*/rangerusersync/\nDEFAULT", 
            "hadoop.proxyuser.ambari-server-test_cluster01.hosts": "c6401.ambari.apache.org", 
            "net.topology.script.file.name": "/etc/hadoop/conf/topology_script.py",
            "hadoop.proxyuser.hdfs.groups": "*",
            "fs.trash.interval": "360",
            "ipc.server.tcpnodelay": "true",
            "io.compression.codecs": "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec,org.apache.hadoop.io.compress.SnappyCodec",
            "ipc.client.idlethreshold": "8000",
            "io.file.buffer.size": "131072",
            "hadoop.proxyuser.ambari-server-test_cluster01.groups": "*",
            "io.serializations": "org.apache.hadoop.io.serializer.WritableSerialization",
            "hadoop.security.authentication": "kerberos",
            "mapreduce.jobtracker.webinterface.trusted": "false",
            "hadoop.proxyuser.hdfs.hosts": "*",
            "hadoop.proxyuser.HTTP.groups": "users",
            "fs.defaultFS": "hdfs://c6401.ambari.apache.org:8020",
            "ha.failover-controller.active-standby-elector.zk.op.retries": "120",
            "hadoop.security.key.provider.path": "kms://http@c6401.ambari.apache.org:9292/kms",
            "hadoop.security.authorization": "true",
            "hadoop.http.authentication.simple.anonymous.allowed": "true",
            "ipc.client.connect.max.retries": "50",
            "hadoop.security.auth_to_local": "RULE:[1:$1@$0](ambari-qa-test_cluster01@EXAMPLE.COM)s/.*/ambari-qa/\nRULE:[1:$1@$0](hdfs-test_cluster01@EXAMPLE.COM)s/.*/hdfs/\nRULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\nRULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\nRULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\nRULE:[2:$1@$0](rangeradmin@EXAMPLE.COM)s/.*/ranger/\nRULE:[2:$1@$0](rangertagsync@EXAMPLE.COM)s/.*/rangertagsync/\nRULE:[2:$1@$0](rangerusersync@EXAMPLE.COM)s/.*/rangerusersync/\nDEFAULT",
            "hadoop.proxyuser.ambari-server-test_cluster01.hosts": "c6401.ambari.apache.org",
             "ipc.client.connection.maxidletime": "30000"
        }, 
        },
         "hadoop-env": {
            "keyserver_port": "", 
            "proxyuser_group": "users", 
            "hdfs_user_nproc_limit": "65536", 
            "hdfs_log_dir_prefix": "/var/log/hadoop", 
            "hdfs_user_nofile_limit": "128000", 
            "hdfs_user": "hdfs", 
            "hdfs_principal_name": "hdfs-test_cluster01@EXAMPLE.COM", 
            "keyserver_host": " ", 
            "namenode_opt_maxnewsize": "128m", 
            "hdfs_user_keytab": "/etc/security/keytabs/hdfs.headless.keytab", 
            "namenode_opt_maxpermsize": "256m", 
            "content": "\n# Set Hadoop-specific environment variables here.\n\n# The only required environment variable is JAVA_HOME.  All others are\n# optional.  When running a distributed configuration it is best to\n# set JAVA_HOME in this file, so that it is correctly defined on\n# remote nodes.\n\n# The java implementation to use.  Required.\nexport JAVA_HOME={{java_home}}\nexport HADOOP_HOME_WARN_SUPPRESS=1\n\n# Hadoop home directory\nexport HADOOP_HOME=${HADOOP_HOME:-{{hadoop_home}}}\n\n# Hadoop Configuration Directory\n\n{# this is different for HDP1 #}\n# Path to jsvc required by secure HDP 2.0 datanode\nexport JSVC_HOME={{jsvc_path}}\n\n\n# The maximum amount of heap to use, in MB. Default is 1000.\nexport HADOOP_HEAPSIZE=\"{{hadoop_heapsize}}\"\n\nexport HADOOP_NAMENODE_INIT_HEAPSIZE=\"-Xms{{namenode_heapsize}}\"\n\n# Extra Java runtime options.  Empty by default.\nexport HADOOP_OPTS=\"-Djava.net.preferIPv4Stack=true ${HADOOP_OPTS}\"\n\n# Command specific options appended to HADOOP_OPTS when specified\nHADOOP_JOBTRACKER_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{jtnode_opt_newsize}} -XX:MaxNewSize={{jtnode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xmx{{jtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dmapred.audit.logger=INFO,MRAUDIT -Dhadoop.mapreduce.jobsummary.logger=INFO,JSA ${HADOOP_JOBTRACKER_OPTS}\"\n\nHADOOP_TASKTRACKER_OPTS=\"-server -Xmx{{ttnode_heapsize}} -Dhadoop.security.logger=ERROR,console -Dmapred.audit.logger=ERROR,console ${HADOOP_TASKTRACKER_OPTS}\"\n\n{% if java_version < 8 %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -XX:PermSize={{namenode_opt_permsize}} -XX:MaxPermSize={{namenode_opt_maxpermsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -XX:PermSize=128m -XX:MaxPermSize=256m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m -XX:MaxPermSize=512m $HADOOP_CLIENT_OPTS\"\n\n{% else %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m $HADOOP_CLIENT_OPTS\"\n{% endif %}\n\nHADOOP_NFS3_OPTS=\"-Xmx{{nfsgateway_heapsize}}m -Dhadoop.security.logger=ERROR,DRFAS ${HADOOP_NFS3_OPTS}\"\nHADOOP_BALANCER_OPTS=\"-server -Xmx{{hadoop_heapsize}}m ${HADOOP_BALANCER_OPTS}\"\n\n\n# On secure datanodes, user to run the datanode as after dropping privileges\nexport HADOOP_SECURE_DN_USER=${HADOOP_SECURE_DN_USER:-{{hadoop_secure_dn_user}}}\n\n# Extra ssh options.  Empty by default.\nexport HADOOP_SSH_OPTS=\"-o ConnectTimeout=5 -o SendEnv=HADOOP_CONF_DIR\"\n\n# Where log files are stored.  $HADOOP_HOME/logs by default.\nexport HADOOP_LOG_DIR={{hdfs_log_dir_prefix}}/$USER\n\n# History server logs\nexport HADOOP_MAPRED_LOG_DIR={{mapred_log_dir_prefix}}/$USER\n\n# Where log files are stored in the secure data environment.\nexport HADOOP_SECURE_DN_LOG_DIR={{hdfs_log_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# File naming remote slave hosts.  $HADOOP_HOME/conf/slaves by default.\n# export HADOOP_SLAVES=${HADOOP_HOME}/conf/slaves\n\n# host:path where hadoop code should be rsync'd from.  Unset by default.\n# export HADOOP_MASTER=master:/home/$USER/src/hadoop\n\n# Seconds to sleep between slave commands.  Unset by default.  This\n# can be useful in large clusters, where, e.g., slave rsyncs can\n# otherwise arrive faster than the master can service them.\n# export HADOOP_SLAVE_SLEEP=0.1\n\n# The directory where pid files are stored. /tmp by default.\nexport HADOOP_PID_DIR={{hadoop_pid_dir_prefix}}/$USER\nexport HADOOP_SECURE_DN_PID_DIR={{hadoop_pid_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# History server pid\nexport HADOOP_MAPRED_PID_DIR={{mapred_pid_dir_prefix}}/$USER\n\nYARN_RESOURCEMANAGER_OPTS=\"-Dyarn.server.resourcemanager.appsummary.logger=INFO,RMSUMMARY\"\n\n# A string representing this instance of hadoop. $USER by default.\nexport HADOOP_IDENT_STRING=$USER\n\n# The scheduling priority for daemon processes.  See 'man nice'.\n\n# export HADOOP_NICENESS=10\n\n# Add database libraries\nJAVA_JDBC_LIBS=\"\"\nif [ -d \"/usr/share/java\" ]; then\n  for jarFile in `ls /usr/share/java | grep -E \"(mysql|ojdbc|postgresql|sqljdbc)\" 2>/dev/null`\n  do\n    JAVA_JDBC_LIBS=${JAVA_JDBC_LIBS}:$jarFile\n  done\nfi\n\n# Add libraries to the hadoop classpath - some may not need a colon as they already include it\nexport HADOOP_CLASSPATH=${HADOOP_CLASSPATH}${JAVA_JDBC_LIBS}\n\n# Setting path to hdfs command line\nexport HADOOP_LIBEXEC_DIR={{hadoop_libexec_dir}}\n\n# Mostly required for hadoop 2.0\nexport JAVA_LIBRARY_PATH=${JAVA_LIBRARY_PATH}\n\nexport HADOOP_OPTS=\"-Dhdp.version=$HDP_VERSION $HADOOP_OPTS\"\n\n{% if is_datanode_max_locked_memory_set %}\n# Fix temporary bug, when ulimit from conf files is not picked up, without full relogin. \n# Makes sense to fix only when runing DN as root \nif [ \"$command\" == \"datanode\" ] && [ \"$EUID\" -eq 0 ] && [ -n \"$HADOOP_SECURE_DN_USER\" ]; then\n  ulimit -l {{datanode_max_locked_memory}}\nfi\n{% endif %}", 
            "namenode_heapsize": "1024m", 
            "namenode_opt_newsize": "128m", 
            "nfsgateway_heapsize": "1024", 
            "dtnode_heapsize": "1024m", 
            "hadoop_root_logger": "INFO,RFA", 
            "hadoop_heapsize": "1024", 
            "hadoop_pid_dir_prefix": "/var/run/hadoop", 
            "namenode_opt_permsize": "128m", 
            "keyserver_port": "",
            "proxyuser_group": "users",
            "hdfs_user_nproc_limit": "65536",
            "hdfs_log_dir_prefix": "/var/log/hadoop",
            "hdfs_user_nofile_limit": "128000",
            "hdfs_user": "hdfs",
            "hdfs_principal_name": "hdfs-test_cluster01@EXAMPLE.COM",
            "keyserver_host": " ",
            "namenode_opt_maxnewsize": "128m",
            "hdfs_user_keytab": "/etc/security/keytabs/hdfs.headless.keytab",
            "namenode_opt_maxpermsize": "256m",
            "content": "\n# Set Hadoop-specific environment variables here.\n\n# The only required environment variable is JAVA_HOME.  All others are\n# optional.  When running a distributed configuration it is best to\n# set JAVA_HOME in this file, so that it is correctly defined on\n# remote nodes.\n\n# The java implementation to use.  Required.\nexport JAVA_HOME={{java_home}}\nexport HADOOP_HOME_WARN_SUPPRESS=1\n\n# Hadoop home directory\nexport HADOOP_HOME=${HADOOP_HOME:-{{hadoop_home}}}\n\n# Hadoop Configuration Directory\n\n{# this is different for HDP1 #}\n# Path to jsvc required by secure HDP 2.0 datanode\nexport JSVC_HOME={{jsvc_path}}\n\n\n# The maximum amount of heap to use, in MB. Default is 1000.\nexport HADOOP_HEAPSIZE=\"{{hadoop_heapsize}}\"\n\nexport HADOOP_NAMENODE_INIT_HEAPSIZE=\"-Xms{{namenode_heapsize}}\"\n\n# Extra Java runtime options.  Empty by default.\nexport HADOOP_OPTS=\"-Djava.net.preferIPv4Stack=true ${HADOOP_OPTS}\"\n\n# Command specific options appended to HADOOP_OPTS when specified\nHADOOP_JOBTRACKER_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{jtnode_opt_newsize}} -XX:MaxNewSize={{jtnode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xmx{{jtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dmapred.audit.logger=INFO,MRAUDIT -Dhadoop.mapreduce.jobsummary.logger=INFO,JSA ${HADOOP_JOBTRACKER_OPTS}\"\n\nHADOOP_TASKTRACKER_OPTS=\"-server -Xmx{{ttnode_heapsize}} -Dhadoop.security.logger=ERROR,console -Dmapred.audit.logger=ERROR,console ${HADOOP_TASKTRACKER_OPTS}\"\n\n{% if java_version < 8 %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -XX:PermSize={{namenode_opt_permsize}} -XX:MaxPermSize={{namenode_opt_maxpermsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -XX:PermSize=128m -XX:MaxPermSize=256m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m -XX:MaxPermSize=512m $HADOOP_CLIENT_OPTS\"\n\n{% else %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m $HADOOP_CLIENT_OPTS\"\n{% endif %}\n\nHADOOP_NFS3_OPTS=\"-Xmx{{nfsgateway_heapsize}}m -Dhadoop.security.logger=ERROR,DRFAS ${HADOOP_NFS3_OPTS}\"\nHADOOP_BALANCER_OPTS=\"-server -Xmx{{hadoop_heapsize}}m ${HADOOP_BALANCER_OPTS}\"\n\n\n# On secure datanodes, user to run the datanode as after dropping privileges\nexport HADOOP_SECURE_DN_USER=${HADOOP_SECURE_DN_USER:-{{hadoop_secure_dn_user}}}\n\n# Extra ssh options.  Empty by default.\nexport HADOOP_SSH_OPTS=\"-o ConnectTimeout=5 -o SendEnv=HADOOP_CONF_DIR\"\n\n# Where log files are stored.  $HADOOP_HOME/logs by default.\nexport HADOOP_LOG_DIR={{hdfs_log_dir_prefix}}/$USER\n\n# History server logs\nexport HADOOP_MAPRED_LOG_DIR={{mapred_log_dir_prefix}}/$USER\n\n# Where log files are stored in the secure data environment.\nexport HADOOP_SECURE_DN_LOG_DIR={{hdfs_log_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# File naming remote slave hosts.  $HADOOP_HOME/conf/slaves by default.\n# export HADOOP_SLAVES=${HADOOP_HOME}/conf/slaves\n\n# host:path where hadoop code should be rsync'd from.  Unset by default.\n# export HADOOP_MASTER=master:/home/$USER/src/hadoop\n\n# Seconds to sleep between slave commands.  Unset by default.  This\n# can be useful in large clusters, where, e.g., slave rsyncs can\n# otherwise arrive faster than the master can service them.\n# export HADOOP_SLAVE_SLEEP=0.1\n\n# The directory where pid files are stored. /tmp by default.\nexport HADOOP_PID_DIR={{hadoop_pid_dir_prefix}}/$USER\nexport HADOOP_SECURE_DN_PID_DIR={{hadoop_pid_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# History server pid\nexport HADOOP_MAPRED_PID_DIR={{mapred_pid_dir_prefix}}/$USER\n\nYARN_RESOURCEMANAGER_OPTS=\"-Dyarn.server.resourcemanager.appsummary.logger=INFO,RMSUMMARY\"\n\n# A string representing this instance of hadoop. $USER by default.\nexport HADOOP_IDENT_STRING=$USER\n\n# The scheduling priority for daemon processes.  See 'man nice'.\n\n# export HADOOP_NICENESS=10\n\n# Add database libraries\nJAVA_JDBC_LIBS=\"\"\nif [ -d \"/usr/share/java\" ]; then\n  for jarFile in `ls /usr/share/java | grep -E \"(mysql|ojdbc|postgresql|sqljdbc)\" 2>/dev/null`\n  do\n    JAVA_JDBC_LIBS=${JAVA_JDBC_LIBS}:$jarFile\n  done\nfi\n\n# Add libraries to the hadoop classpath - some may not need a colon as they already include it\nexport HADOOP_CLASSPATH=${HADOOP_CLASSPATH}${JAVA_JDBC_LIBS}\n\n# Setting path to hdfs command line\nexport HADOOP_LIBEXEC_DIR={{hadoop_libexec_dir}}\n\n# Mostly required for hadoop 2.0\nexport JAVA_LIBRARY_PATH=${JAVA_LIBRARY_PATH}\n\nexport HADOOP_OPTS=\"-Dhdp.version=$HDP_VERSION $HADOOP_OPTS\"\n\n{% if is_datanode_max_locked_memory_set %}\n# Fix temporary bug, when ulimit from conf files is not picked up, without full relogin. \n# Makes sense to fix only when runing DN as root \nif [ \"$command\" == \"datanode\" ] && [ \"$EUID\" -eq 0 ] && [ -n \"$HADOOP_SECURE_DN_USER\" ]; then\n  ulimit -l {{datanode_max_locked_memory}}\nfi\n{% endif %}",
            "namenode_heapsize": "1024m",
            "namenode_opt_newsize": "128m",
            "nfsgateway_heapsize": "1024",
            "dtnode_heapsize": "1024m",
            "hadoop_root_logger": "INFO,RFA",
            "hadoop_heapsize": "1024",
            "hadoop_pid_dir_prefix": "/var/run/hadoop",
            "namenode_opt_permsize": "128m",
             "hdfs_tmp_dir": "/tmp"
        }, 
        },
         "zookeeper-log4j": {
             "content": "\n#\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#   http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing,\n# software distributed under the License is distributed on an\n# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n# KIND, either express or implied.  See the License for the\n# specific language governing permissions and limitations\n# under the License.\n#\n#\n#\n\n#\n# ZooKeeper Logging Configuration\n#\n\n# DEFAULT: console appender only\nlog4j.rootLogger=INFO, CONSOLE\n\n# Example with rolling log file\n#log4j.rootLogger=DEBUG, CONSOLE, ROLLINGFILE\n\n# Example with rolling log file and tracing\n#log4j.rootLogger=TRACE, CONSOLE, ROLLINGFILE, TRACEFILE\n\n#\n# Log INFO level and above messages to the console\n#\nlog4j.appender.CONSOLE=org.apache.log4j.ConsoleAppender\nlog4j.appender.CONSOLE.Threshold=INFO\nlog4j.appender.CONSOLE.layout=org.apache.log4j.PatternLayout\nlog4j.appender.CONSOLE.layout.ConversionPattern=%d{ISO8601} - %-5p [%t:%C{1}@%L] - %m%n\n\n#\n# Add ROLLINGFILE to rootLogger to get log file output\n#    Log DEBUG level and above messages to a log file\nlog4j.appender.ROLLINGFILE=org.apache.log4j.RollingFileAppender\nlog4j.appender.ROLLINGFILE.Threshold=DEBUG\nlog4j.appender.ROLLINGFILE.File=zookeeper.log\n\n# Max log file size of 10MB\nlog4j.appender.ROLLINGFILE.MaxFileSize=10MB\n# uncomment the next line to limit number of backup files\n#log4j.appender.ROLLINGFILE.MaxBackupIndex=10\n\nlog4j.appender.ROLLINGFILE.layout=org.apache.log4j.PatternLayout\nlog4j.appender.ROLLINGFILE.layout.ConversionPattern=%d{ISO8601} - %-5p [%t:%C{1}@%L] - %m%n\n\n\n#\n# Add TRACEFILE to rootLogger to get log file output\n#    Log DEBUG level and above messages to a log file\nlog4j.appender.TRACEFILE=org.apache.log4j.FileAppender\nlog4j.appender.TRACEFILE.Threshold=TRACE\nlog4j.appender.TRACEFILE.File=zookeeper_trace.log\n\nlog4j.appender.TRACEFILE.layout=org.apache.log4j.PatternLayout\n### Notice we are including log4j's NDC here (%x)\nlog4j.appender.TRACEFILE.layout.ConversionPattern=%d{ISO8601} - %-5p [%t:%C{1}@%L][%x] - %m%n"
        }, 
        },
         "ssl-server": {
            "ssl.server.keystore.location": "/etc/security/serverKeys/keystore.jks", 
            "ssl.server.keystore.keypassword": "bigdata", 
            "ssl.server.truststore.location": "/etc/security/serverKeys/all.jks", 
            "ssl.server.keystore.password": "bigdata", 
            "ssl.server.truststore.password": "bigdata", 
            "ssl.server.truststore.type": "jks", 
            "ssl.server.keystore.type": "jks", 
            "ssl.server.keystore.location": "/etc/security/serverKeys/keystore.jks",
            "ssl.server.keystore.keypassword": "bigdata",
            "ssl.server.truststore.location": "/etc/security/serverKeys/all.jks",
            "ssl.server.keystore.password": "bigdata",
            "ssl.server.truststore.password": "bigdata",
            "ssl.server.truststore.type": "jks",
            "ssl.server.keystore.type": "jks",
             "ssl.server.truststore.reload.interval": "10000"
        }, 
        "ranger-site": {}, 
        },
        "ranger-site": {},
         "admin-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#      http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n#\n\n\nlog4j.rootLogger = warn,xa_log_appender\n\n\n# xa_logger\nlog4j.appender.xa_log_appender=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.xa_log_appender.file=${logdir}/xa_portal.log\nlog4j.appender.xa_log_appender.datePattern='.'yyyy-MM-dd\nlog4j.appender.xa_log_appender.append=true\nlog4j.appender.xa_log_appender.layout=org.apache.log4j.PatternLayout\nlog4j.appender.xa_log_appender.layout.ConversionPattern=%d [%t] %-5p %C{6} (%F:%L) - %m%n\n# xa_log_appender : category and additivity\nlog4j.category.org.springframework=warn,xa_log_appender\nlog4j.additivity.org.springframework=false\n\nlog4j.category.org.apache.ranger=info,xa_log_appender\nlog4j.additivity.org.apache.ranger=false\n\nlog4j.category.xa=info,xa_log_appender\nlog4j.additivity.xa=false\n\n# perf_logger\nlog4j.appender.perf_appender=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.perf_appender.file=${logdir}/ranger_admin_perf.log\nlog4j.appender.perf_appender.datePattern='.'yyyy-MM-dd\nlog4j.appender.perf_appender.append=true\nlog4j.appender.perf_appender.layout=org.apache.log4j.PatternLayout\nlog4j.appender.perf_appender.layout.ConversionPattern=%d [%t] %m%n\n\n\n# sql_appender\nlog4j.appender.sql_appender=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.sql_appender.file=${logdir}/xa_portal_sql.log\nlog4j.appender.sql_appender.datePattern='.'yyyy-MM-dd\nlog4j.appender.sql_appender.append=true\nlog4j.appender.sql_appender.layout=org.apache.log4j.PatternLayout\nlog4j.appender.sql_appender.layout.ConversionPattern=%d [%t] %-5p %C{6} (%F:%L) - %m%n\n\n# sql_appender : category and additivity\nlog4j.category.org.hibernate.SQL=warn,sql_appender\nlog4j.additivity.org.hibernate.SQL=false\n\nlog4j.category.jdbc.sqlonly=fatal,sql_appender\nlog4j.additivity.jdbc.sqlonly=false\n\nlog4j.category.jdbc.sqltiming=warn,sql_appender\nlog4j.additivity.jdbc.sqltiming=false\n\nlog4j.category.jdbc.audit=fatal,sql_appender\nlog4j.additivity.jdbc.audit=false\n\nlog4j.category.jdbc.resultset=fatal,sql_appender\nlog4j.additivity.jdbc.resultset=false\n\nlog4j.category.jdbc.connection=fatal,sql_appender\nlog4j.additivity.jdbc.connection=false"
        }, 
        },
         "tagsync-log4j": {
             "content": "\n#\n# Licensed to the Apache Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# distributed with this work for additional information\n# regarding copyright ownership.  The ASF licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may not use this file except in compliance\n# with the License.  You may obtain a copy of the License at\n#\n#      http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n#\n\n\nlog4j.rootLogger = info,logFile\n\n# logFile\nlog4j.appender.logFile=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.logFile.file=${logdir}/tagsync.log\nlog4j.appender.logFile.datePattern='.'yyyy-MM-dd\nlog4j.appender.logFile.layout=org.apache.log4j.PatternLayout\nlog4j.appender.logFile.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %L %m%n\n\n# console\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.Target=System.out\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{dd MMM yyyy HH:mm:ss} %5p %c{1} [%t] - %L %m%n"
        }, 
        },
         "ranger-hdfs-security": {
            "ranger.plugin.hdfs.policy.source.impl": "org.apache.ranger.admin.client.RangerAdminRESTClient", 
            "ranger.plugin.hdfs.service.name": "{{repo_name}}", 
            "ranger.plugin.hdfs.policy.cache.dir": "/etc/ranger/{{repo_name}}/policycache", 
            "ranger.plugin.hdfs.policy.pollIntervalMs": "30000", 
            "ranger.plugin.hdfs.policy.rest.url": "{{policymgr_mgr_url}}", 
            "ranger.plugin.hdfs.policy.rest.ssl.config.file": "/etc/hadoop/conf/ranger-policymgr-ssl.xml", 
            "ranger.plugin.hdfs.policy.source.impl": "org.apache.ranger.admin.client.RangerAdminRESTClient",
            "ranger.plugin.hdfs.service.name": "{{repo_name}}",
            "ranger.plugin.hdfs.policy.cache.dir": "/etc/ranger/{{repo_name}}/policycache",
            "ranger.plugin.hdfs.policy.pollIntervalMs": "30000",
            "ranger.plugin.hdfs.policy.rest.url": "{{policymgr_mgr_url}}",
            "ranger.plugin.hdfs.policy.rest.ssl.config.file": "/etc/hadoop/conf/ranger-policymgr-ssl.xml",
             "xasecure.add-hadoop-authorization": "true"
        }, 
        "usersync-properties": {}, 
        },
        "usersync-properties": {},
         "zookeeper-env": {
            "zk_server_heapsize": "1024m", 
            "zookeeper_keytab_path": "/etc/security/keytabs/zk.service.keytab", 
            "zk_user": "zookeeper", 
            "zk_log_dir": "/var/log/zookeeper", 
            "content": "\nexport JAVA_HOME={{java64_home}}\nexport ZOOKEEPER_HOME={{zk_home}}\nexport ZOO_LOG_DIR={{zk_log_dir}}\nexport ZOOPIDFILE={{zk_pid_file}}\nexport SERVER_JVMFLAGS={{zk_server_heapsize}}\nexport JAVA=$JAVA_HOME/bin/java\nexport CLASSPATH=$CLASSPATH:/usr/share/zookeeper/*\n\n{% if security_enabled %}\nexport SERVER_JVMFLAGS=\"$SERVER_JVMFLAGS -Djava.security.auth.login.config={{zk_server_jaas_file}}\"\nexport CLIENT_JVMFLAGS=\"$CLIENT_JVMFLAGS -Djava.security.auth.login.config={{zk_client_jaas_file}}\"\n{% endif %}", 
            "zk_pid_dir": "/var/run/zookeeper", 
            "zk_server_heapsize": "1024m",
            "zookeeper_keytab_path": "/etc/security/keytabs/zk.service.keytab",
            "zk_user": "zookeeper",
            "zk_log_dir": "/var/log/zookeeper",
            "content": "\nexport JAVA_HOME={{java64_home}}\nexport ZOOKEEPER_HOME={{zk_home}}\nexport ZOO_LOG_DIR={{zk_log_dir}}\nexport ZOOPIDFILE={{zk_pid_file}}\nexport SERVER_JVMFLAGS={{zk_server_heapsize}}\nexport JAVA=$JAVA_HOME/bin/java\nexport CLASSPATH=$CLASSPATH:/usr/share/zookeeper/*\n\n{% if security_enabled %}\nexport SERVER_JVMFLAGS=\"$SERVER_JVMFLAGS -Djava.security.auth.login.config={{zk_server_jaas_file}}\"\nexport CLIENT_JVMFLAGS=\"$CLIENT_JVMFLAGS -Djava.security.auth.login.config={{zk_client_jaas_file}}\"\n{% endif %}",
            "zk_pid_dir": "/var/run/zookeeper",
             "zookeeper_principal_name": "zookeeper/_HOST@EXAMPLE.COM"
         },
         "infra-solr-env": {
@@ -747,7 +747,7 @@
             "infra_solr_kerberos_name_rules": "DEFAULT",
             "infra_solr_user": "infra-solr",
             "infra_solr_maxmem": "1024",
            "content": "#!/bin/bash\n# Licensed to the Apache Software Foundation (ASF) under one or more\n# contributor license agreements. See the NOTICE file distributed with\n# this work for additional information regarding copyright ownership.\n# The ASF licenses this file to You under the Apache License, Version 2.0\n# (the \"License\"); you may not use this file except in compliance with\n# the License. You may obtain a copy of the License at\n#\n# http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n\n# By default the script will use JAVA_HOME to determine which java\n# to use, but you can set a specific path for Solr to use without\n# affecting other Java applications on your server/workstation.\nSOLR_JAVA_HOME={{java64_home}}\n\n# Increase Java Min/Max Heap as needed to support your indexing / query needs\nSOLR_JAVA_MEM=\"-Xms{{logsearch_solr_min_mem}}m -Xmx{{logsearch_solr_max_mem}}m\"\n\n# Enable verbose GC logging\nGC_LOG_OPTS=\"-verbose:gc -XX:+PrintHeapAtGC -XX:+PrintGCDetails \\\n-XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationStoppedTime\"\n\n# These GC settings have shown to work well for a number of common Solr workloads\nGC_TUNE=\"-XX:NewRatio=3 \\\n-XX:SurvivorRatio=4 \\\n-XX:TargetSurvivorRatio=90 \\\n-XX:MaxTenuringThreshold=8 \\\n-XX:+UseConcMarkSweepGC \\\n-XX:+UseParNewGC \\\n-XX:ConcGCThreads=4 -XX:ParallelGCThreads=4 \\\n-XX:+CMSScavengeBeforeRemark \\\n-XX:PretenureSizeThreshold=64m \\\n-XX:+UseCMSInitiatingOccupancyOnly \\\n-XX:CMSInitiatingOccupancyFraction=50 \\\n-XX:CMSMaxAbortablePrecleanTime=6000 \\\n-XX:+CMSParallelRemarkEnabled \\\n-XX:+ParallelRefProcEnabled\"\n\n# Set the ZooKeeper connection string if using an external ZooKeeper ensemble\n# e.g. host1:2181,host2:2181/chroot\n# Leave empty if not using SolrCloud\nZK_HOST=\"{{zookeeper_quorum}}{{logsearch_solr_znode}}\"\n\n# Set the ZooKeeper client timeout (for SolrCloud mode)\nZK_CLIENT_TIMEOUT=\"60000\"\n\n# By default the start script uses \"localhost\"; override the hostname here\n# for production SolrCloud environments to control the hostname exposed to cluster state\n#SOLR_HOST=\"192.168.1.1\"\n\n# By default the start script uses UTC; override the timezone if needed\n#SOLR_TIMEZONE=\"UTC\"\n\n# Set to true to activate the JMX RMI connector to allow remote JMX client applications\n# to monitor the JVM hosting Solr; set to \"false\" to disable that behavior\n# (false is recommended in production environments)\nENABLE_REMOTE_JMX_OPTS=\"true\"\n\n# The script will use SOLR_PORT+10000 for the RMI_PORT or you can set it here\nRMI_PORT={{logsearch_solr_jmx_port}}\n\n# Anything you add to the SOLR_OPTS variable will be included in the java\n# start command line as-is, in ADDITION to other options. If you specify the\n# -a option on start script, those options will be appended as well. Examples:\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.autoSoftCommit.maxTime=3000\"\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.autoCommit.maxTime=60000\"\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.clustering.enabled=true\"\n\n# Location where the bin/solr script will save PID files for running instances\n# If not set, the script will create PID files in $SOLR_TIP/bin\nSOLR_PID_DIR={{logsearch_solr_piddir}}\n\n# Path to a directory where Solr creates index files, the specified directory\n# must contain a solr.xml; by default, Solr will use server/solr\nSOLR_HOME={{logsearch_solr_datadir}}\n\n# Solr provides a default Log4J configuration properties file in server/resources\n# however, you may want to customize the log settings and file appender location\n# so you can point the script to use a different log4j.properties file\nLOG4J_PROPS={{logsearch_solr_conf}}/log4j.properties\n\n# Location where Solr should write logs to; should agree with the file appender\n# settings in server/resources/log4j.properties\nSOLR_LOGS_DIR={{logsearch_solr_log_dir}}\n\n# Sets the port Solr binds to, default is 8983\nSOLR_PORT={{logsearch_solr_port}}\n\n# Be sure to update the paths to the correct keystore for your environment\n{% if logsearch_solr_ssl_enabled %}\nSOLR_SSL_KEY_STORE={{logsearch_solr_keystore_location}}\nSOLR_SSL_KEY_STORE_PASSWORD={{logsearch_solr_keystore_password}}\nSOLR_SSL_TRUST_STORE={{logsearch_solr_keystore_location}}\nSOLR_SSL_TRUST_STORE_PASSWORD={{logsearch_solr_keystore_password}}\nSOLR_SSL_NEED_CLIENT_AUTH=false\nSOLR_SSL_WANT_CLIENT_AUTH=false\n{% endif %}\n\n# Uncomment to set a specific SSL port (-Djetty.ssl.port=N); if not set\n# and you are using SSL, then the start script will use SOLR_PORT for the SSL port\n#SOLR_SSL_PORT=\n\n{% if security_enabled -%}\nSOLR_HOST=`hostname -f`\nSOLR_JAAS_FILE={{logsearch_solr_jaas_file}}\nSOLR_KERB_KEYTAB={{logsearch_solr_web_kerberos_keytab}}\nSOLR_KERB_PRINCIPAL={{logsearch_solr_web_kerberos_principal}}\nSOLR_KERB_NAME_RULES={{logsearch_solr_kerberos_name_rules}}\n\nSOLR_AUTHENTICATION_CLIENT_CONFIGURER=\"org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer\"\nSOLR_AUTHENTICATION_OPTS=\" -DauthenticationPlugin=org.apache.solr.security.KerberosPlugin -Djava.security.auth.login.config=$SOLR_JAAS_FILE -Dsolr.kerberos.principal=${SOLR_KERB_PRINCIPAL} -Dsolr.kerberos.keytab=${SOLR_KERB_KEYTAB} -Dsolr.kerberos.cookie.domain=${SOLR_HOST} -Dsolr.kerberos.name.rules=${SOLR_KERB_NAME_RULES}\"\n{% endif %}", 
            "content": "#!/bin/bash\n# Licensed to the Apache Software Foundation (ASF) under one or more\n# contributor license agreements. See the NOTICE file distributed with\n# this work for additional information regarding copyright ownership.\n# The ASF licenses this file to You under the Apache License, Version 2.0\n# (the \"License\"); you may not use this file except in compliance with\n# the License. You may obtain a copy of the License at\n#\n# http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to in writing, software\n# distributed under the License is distributed on an \"AS IS\" BASIS,\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n# See the License for the specific language governing permissions and\n# limitations under the License.\n\n# By default the script will use JAVA_HOME to determine which java\n# to use, but you can set a specific path for Solr to use without\n# affecting other Java applications on your server/workstation.\nSOLR_JAVA_HOME={{java64_home}}\n\n# Increase Java Min/Max Heap as needed to support your indexing / query needs\nSOLR_JAVA_MEM=\"-Xms{{logsearch_solr_min_mem}}m -Xmx{{logsearch_solr_max_mem}}m\"\n\n# Enable verbose GC logging\nGC_LOG_OPTS=\"-verbose:gc -XX:+PrintHeapAtGC -XX:+PrintGCDetails \\\n-XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationStoppedTime\"\n\n# These GC settings have shown to work well for a number of common Solr workloads\nGC_TUNE=\"-XX:NewRatio=3 \\\n-XX:SurvivorRatio=4 \\\n-XX:TargetSurvivorRatio=90 \\\n-XX:MaxTenuringThreshold=8 \\\n-XX:+UseConcMarkSweepGC \\\n-XX:+UseParNewGC \\\n-XX:ConcGCThreads=4 -XX:ParallelGCThreads=4 \\\n-XX:+CMSScavengeBeforeRemark \\\n-XX:PretenureSizeThreshold=64m \\\n-XX:+UseCMSInitiatingOccupancyOnly \\\n-XX:CMSInitiatingOccupancyFraction=50 \\\n-XX:CMSMaxAbortablePrecleanTime=6000 \\\n-XX:+CMSParallelRemarkEnabled \\\n-XX:+ParallelRefProcEnabled\"\n\n# Set the ZooKeeper connection string if using an external ZooKeeper ensemble\n# e.g. host1:2181,host2:2181/chroot\n# Leave empty if not using SolrCloud\nZK_HOST=\"{{zookeeper_quorum}}{{logsearch_solr_znode}}\"\n\n# Set the ZooKeeper client timeout (for SolrCloud mode)\nZK_CLIENT_TIMEOUT=\"60000\"\n\n# By default the start script uses \"localhost\"; override the hostname here\n# for production SolrCloud environments to control the hostname exposed to cluster state\n#SOLR_HOST=\"192.168.1.1\"\n\n# By default the start script uses UTC; override the timezone if needed\n#SOLR_TIMEZONE=\"UTC\"\n\n# Set to true to activate the JMX RMI connector to allow remote JMX client applications\n# to monitor the JVM hosting Solr; set to \"false\" to disable that behavior\n# (false is recommended in production environments)\nENABLE_REMOTE_JMX_OPTS=\"true\"\n\n# The script will use SOLR_PORT+10000 for the RMI_PORT or you can set it here\nRMI_PORT={{logsearch_solr_jmx_port}}\n\n# Anything you add to the SOLR_OPTS variable will be included in the java\n# start command line as-is, in ADDITION to other options. If you specify the\n# -a option on start script, those options will be appended as well. Examples:\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.autoSoftCommit.maxTime=3000\"\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.autoCommit.maxTime=60000\"\n#SOLR_OPTS=\"$SOLR_OPTS -Dsolr.clustering.enabled=true\"\n\n# Location where the bin/solr script will save PID files for running instances\n# If not set, the script will create PID files in $SOLR_TIP/bin\nSOLR_PID_DIR={{logsearch_solr_piddir}}\n\n# Path to a directory where Solr creates index files, the specified directory\n# must contain a solr.xml; by default, Solr will use server/solr\nSOLR_HOME={{logsearch_solr_datadir}}\n\n# Solr provides a default Log4J configuration properties file in server/resources\n# however, you may want to customize the log settings and file appender location\n# so you can point the script to use a different log4j.properties file\nLOG4J_PROPS={{logsearch_solr_conf}}/log4j.properties\n\n# Location where Solr should write logs to; should agree with the file appender\n# settings in server/resources/log4j.properties\nSOLR_LOGS_DIR={{logsearch_solr_log_dir}}\n\n# Sets the port Solr binds to, default is 8983\nSOLR_PORT={{logsearch_solr_port}}\n\n# Be sure to update the paths to the correct keystore for your environment\n{% if logsearch_solr_ssl_enabled %}\nSOLR_SSL_KEY_STORE={{logsearch_solr_keystore_location}}\nSOLR_SSL_KEY_STORE_PASSWORD={{logsearch_solr_keystore_password}}\nSOLR_SSL_TRUST_STORE={{logsearch_solr_keystore_location}}\nSOLR_SSL_TRUST_STORE_PASSWORD={{logsearch_solr_keystore_password}}\nSOLR_SSL_NEED_CLIENT_AUTH=false\nSOLR_SSL_WANT_CLIENT_AUTH=false\n{% endif %}\n\n# Uncomment to set a specific SSL port (-Djetty.ssl.port=N); if not set\n# and you are using SSL, then the start script will use SOLR_PORT for the SSL port\n#SOLR_SSL_PORT=\n\n{% if security_enabled -%}\nSOLR_HOST=`hostname -f`\nSOLR_JAAS_FILE={{logsearch_solr_jaas_file}}\nSOLR_KERB_KEYTAB={{logsearch_solr_web_kerberos_keytab}}\nSOLR_KERB_PRINCIPAL={{logsearch_solr_web_kerberos_principal}}\nSOLR_KERB_NAME_RULES={{logsearch_solr_kerberos_name_rules}}\n\nSOLR_AUTHENTICATION_CLIENT_CONFIGURER=\"org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer\"\nSOLR_AUTHENTICATION_OPTS=\" -DauthenticationPlugin=org.apache.solr.security.KerberosPlugin -Djava.security.auth.login.config=$SOLR_JAAS_FILE -Dsolr.kerberos.principal=${SOLR_KERB_PRINCIPAL} -Dsolr.kerberos.keytab=${SOLR_KERB_KEYTAB} -Dsolr.kerberos.cookie.domain=${SOLR_HOST} -Dsolr.kerberos.name.rules=${SOLR_KERB_NAME_RULES}\"\n{% endif %}",
             "infra_solr_pid_dir": "/var/run/ambari-infra-solr",
             "infra_solr_truststore_password": "bigdata",
             "infra_solr_truststore_type": "jks",
@@ -771,32 +771,32 @@
             "content": "content"
         },
         "cluster-env": {
            "security_enabled": "true", 
            "override_uid": "true", 
            "fetch_nonlocal_groups": "true", 
            "one_dir_per_partition": "true", 
            "repo_ubuntu_template": "{{package_type}} {{base_url}} {{components}}", 
            "ignore_groupsusers_create": "false", 
            "alerts_repeat_tolerance": "1", 
            "smokeuser_keytab": "/etc/security/keytabs/smokeuser.headless.keytab", 
            "kerberos_domain": "EXAMPLE.COM", 
            "security_enabled": "true",
            "override_uid": "true",
            "fetch_nonlocal_groups": "true",
            "one_dir_per_partition": "true",
            "repo_ubuntu_template": "{{package_type}} {{base_url}} {{components}}",
            "ignore_groupsusers_create": "false",
            "alerts_repeat_tolerance": "1",
            "smokeuser_keytab": "/etc/security/keytabs/smokeuser.headless.keytab",
            "kerberos_domain": "EXAMPLE.COM",
             "manage_dirs_on_root": "true",
            "recovery_lifetime_max_count": "1024", 
            "recovery_type": "AUTO_START", 
            "ignore_bad_mounts": "false", 
            "recovery_window_in_minutes": "60", 
            "user_group": "hadoop", 
            "stack_tools": "{\n  \"stack_selector\": [\"hdp-select\", \"/usr/bin/hdp-select\", \"hdp-select\"],\n  \"conf_selector\": [\"conf-select\", \"/usr/bin/conf-select\", \"conf-select\"]\n}", 
            "recovery_retry_interval": "5", 
            "stack_features": "{\n  \"stack_features\": [\n    {\n      \"name\": \"snappy\",\n      \"description\": \"Snappy compressor/decompressor support\",\n      \"min_version\": \"2.0.0.0\",\n      \"max_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"lzo\",\n      \"description\": \"LZO libraries support\",\n      \"min_version\": \"2.2.1.0\"\n    },\n    {\n      \"name\": \"express_upgrade\",\n      \"description\": \"Express upgrade support\",\n      \"min_version\": \"2.1.0.0\"\n    },\n    {\n      \"name\": \"rolling_upgrade\",\n      \"description\": \"Rolling upgrade support\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"config_versioning\",\n      \"description\": \"Configurable versions support\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"datanode_non_root\",\n      \"description\": \"DataNode running as non-root support (AMBARI-7615)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"remove_ranger_hdfs_plugin_env\",\n      \"description\": \"HDFS removes Ranger env files (AMBARI-14299)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"ranger\",\n      \"description\": \"Ranger Service support\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"ranger_tagsync_component\",\n      \"description\": \"Ranger Tagsync component support (AMBARI-14383)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"phoenix\",\n      \"description\": \"Phoenix Service support\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"nfs\",\n      \"description\": \"NFS support\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"tez_for_spark\",\n      \"description\": \"Tez dependency for Spark\",\n      \"min_version\": \"2.2.0.0\",\n      \"max_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"timeline_state_store\",\n      \"description\": \"Yarn application timeline-service supports state store property (AMBARI-11442)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"copy_tarball_to_hdfs\",\n      \"description\": \"Copy tarball to HDFS support (AMBARI-12113)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"spark_16plus\",\n      \"description\": \"Spark 1.6+\",\n      \"min_version\": \"2.4.0.0\"\n    },\n    {\n      \"name\": \"spark_thriftserver\",\n      \"description\": \"Spark Thrift Server\",\n      \"min_version\": \"2.3.2.0\"\n    },\n    {\n      \"name\": \"storm_kerberos\",\n      \"description\": \"Storm Kerberos support (AMBARI-7570)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"storm_ams\",\n      \"description\": \"Storm AMS integration (AMBARI-10710)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"create_kafka_broker_id\",\n      \"description\": \"Ambari should create Kafka Broker Id (AMBARI-12678)\",\n      \"min_version\": \"2.2.0.0\",\n      \"max_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"kafka_listeners\",\n      \"description\": \"Kafka listeners (AMBARI-10984)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"kafka_kerberos\",\n      \"description\": \"Kafka Kerberos support (AMBARI-10984)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"pig_on_tez\",\n      \"description\": \"Pig on Tez support (AMBARI-7863)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"ranger_usersync_non_root\",\n      \"description\": \"Ranger Usersync as non-root user (AMBARI-10416)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"ranger_audit_db_support\",\n      \"description\": \"Ranger Audit to DB support\",\n      \"min_version\": \"2.2.0.0\",\n      \"max_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"accumulo_kerberos_user_auth\",\n      \"description\": \"Accumulo Kerberos User Auth (AMBARI-10163)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"knox_versioned_data_dir\",\n      \"description\": \"Use versioned data dir for Knox (AMBARI-13164)\",\n      \"min_version\": \"2.3.2.0\"\n    },\n    {\n      \"name\": \"knox_sso_topology\",\n      \"description\": \"Knox SSO Topology support (AMBARI-13975)\",\n      \"min_version\": \"2.3.8.0\"\n    },\n    {\n      \"name\": \"atlas_rolling_upgrade\",\n      \"description\": \"Rolling upgrade support for Atlas\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"oozie_admin_user\",\n      \"description\": \"Oozie install user as an Oozie admin user (AMBARI-7976)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"oozie_create_hive_tez_configs\",\n      \"description\": \"Oozie create configs for Ambari Hive and Tez deployments (AMBARI-8074)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"oozie_setup_shared_lib\",\n      \"description\": \"Oozie setup tools used to shared Oozie lib to HDFS (AMBARI-7240)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"oozie_host_kerberos\",\n      \"description\": \"Oozie in secured clusters uses _HOST in Kerberos principal (AMBARI-9775)\",\n      \"min_version\": \"2.0.0.0\",\n      \"max_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"falcon_extensions\",\n      \"description\": \"Falcon Extension\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hive_metastore_upgrade_schema\",\n      \"description\": \"Hive metastore upgrade schema support (AMBARI-11176)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"hive_server_interactive\",\n      \"description\": \"Hive server interactive support (AMBARI-15573)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hive_webhcat_specific_configs\",\n      \"description\": \"Hive webhcat specific configurations support (AMBARI-12364)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"hive_purge_table\",\n      \"description\": \"Hive purge table support (AMBARI-12260)\",\n      \"min_version\": \"2.3.0.0\"\n    },\n    {\n      \"name\": \"hive_server2_kerberized_env\",\n      \"description\": \"Hive server2 working on kerberized environment (AMBARI-13749)\",\n      \"min_version\": \"2.2.3.0\",\n      \"max_version\": \"2.2.5.0\"\n    },\n    {\n      \"name\": \"hive_env_heapsize\",\n      \"description\": \"Hive heapsize property defined in hive-env (AMBARI-12801)\",\n      \"min_version\": \"2.2.0.0\"\n    },\n    {\n      \"name\": \"ranger_kms_hsm_support\",\n      \"description\": \"Ranger KMS HSM support (AMBARI-15752)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_log4j_support\",\n      \"description\": \"Ranger supporting log-4j properties (AMBARI-15681)\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_kerberos_support\",\n      \"description\": \"Ranger Kerberos support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hive_metastore_site_support\",\n      \"description\": \"Hive Metastore site support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_usersync_password_jceks\",\n      \"description\": \"Saving Ranger Usersync credentials in jceks\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_install_infra_client\",\n      \"description\": \"LogSearch Service support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"hbase_home_directory\",\n      \"description\": \"Hbase home directory in HDFS needed for HBASE backup\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"spark_livy\",\n      \"description\": \"Livy as slave component of spark\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"atlas_ranger_plugin_support\",\n      \"description\": \"Atlas Ranger plugin support\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_pid_support\",\n      \"description\": \"Ranger Service support pid generation AMBARI-16756\",\n      \"min_version\": \"2.5.0.0\"\n    },\n    {\n      \"name\": \"ranger_kms_pid_support\",\n      \"description\": \"Ranger KMS Service support pid generation\",\n      \"min_version\": \"2.5.0.0\"\n    }\n  ]\n}",
            "recovery_enabled": "true", 
            "smokeuser_principal_name": "ambari-qa-test_cluster01@EXAMPLE.COM", 
            "recovery_max_count": "6", 
            "stack_root": "/usr/hdp", 
            "repo_suse_rhel_template": "[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0", 
            "ambari_principal_name": "ambari-server-test_cluster01@EXAMPLE.COM", 
            "managed_hdfs_resource_property_names": "", 
            "recovery_lifetime_max_count": "1024",
            "recovery_type": "AUTO_START",
            "ignore_bad_mounts": "false",
            "recovery_window_in_minutes": "60",
            "user_group": "hadoop",
            "stack_name": "HDP",
            "stack_root": "{\"HDP\": \"/usr/hdp\"}",
            "stack_tools": "{\n \"HDP\": { \"stack_selector\": [\"hdp-select\", \"/usr/bin/hdp-select\", \"hdp-select\"],\n  \"conf_selector\": [\"conf-select\", \"/usr/bin/conf-select\", \"conf-select\"]\n}\n}",
            "recovery_retry_interval": "5",
            "recovery_enabled": "true",
            "smokeuser_principal_name": "ambari-qa-test_cluster01@EXAMPLE.COM",
            "recovery_max_count": "6",
            "repo_suse_rhel_template": "[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0",
            "ambari_principal_name": "ambari-server-test_cluster01@EXAMPLE.COM",
            "managed_hdfs_resource_property_names": "",
             "smokeuser": "ambari-qa"
         }
     }
}
\ No newline at end of file
}
diff --git a/ambari-server/src/test/python/stacks/utils/RMFTestCase.py b/ambari-server/src/test/python/stacks/utils/RMFTestCase.py
index 282b542198..2f3794d061 100644
-- a/ambari-server/src/test/python/stacks/utils/RMFTestCase.py
++ b/ambari-server/src/test/python/stacks/utils/RMFTestCase.py
@@ -102,8 +102,12 @@ class RMFTestCase(TestCase):
     else:
       raise RuntimeError("Please specify either config_file_path or config_dict parameter")
 
    self.config_dict["configurations"]["cluster-env"]["stack_tools"] = RMFTestCase.get_stack_tools()
    self.config_dict["configurations"]["cluster-env"]["stack_features"] = RMFTestCase.get_stack_features()
    # add the stack tools & features from the stack if the test case's JSON file didn't have them
    if "stack_tools" not in self.config_dict["configurations"]["cluster-env"]:
      self.config_dict["configurations"]["cluster-env"]["stack_tools"] = RMFTestCase.get_stack_tools()

    if "stack_features" not in self.config_dict["configurations"]["cluster-env"]:
      self.config_dict["configurations"]["cluster-env"]["stack_features"] = RMFTestCase.get_stack_features()
 
     if config_overrides:
       for key, value in config_overrides.iteritems():
- 
2.19.1.windows.1

