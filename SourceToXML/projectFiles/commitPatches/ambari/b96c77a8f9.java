From b96c77a8f9dfd021aa4a0b2f825dc2e270f79050 Mon Sep 17 00:00:00 2001
From: Swapan Shridhar <sshridhar@hortonworks.com>
Date: Thu, 24 Aug 2017 12:13:10 -0700
Subject: [PATCH] AMBARI-21076. Move superset as an independent project.
 (Nishant Bangarwa via Swapan Shridhar).

--
 .../server/upgrade/UpgradeCatalog260.java     |  48 ++++++-
 .../configuration/druid-superset-env.xml      |  12 ++
 .../common-services/DRUID/0.9.2/metainfo.xml  |  21 ---
 .../DRUID/0.9.2/package/scripts/params.py     |  45 -------
 .../0.9.2/package/scripts/status_params.py    |   2 -
 .../DRUID/0.9.2/package/scripts/superset.py   |  40 +++---
 .../DRUID/0.9.2/package/templates/superset.sh |   2 +-
 .../DRUID/0.9.2/quicklinks/quicklinks.json    |  13 --
 .../DRUID/0.9.2/role_command_order.json       |   3 +-
 .../DRUID/0.9.2/themes/theme.json             |  82 +-----------
 .../SUPERSET/0.15.0/metainfo.xml              |  88 +++++++++++++
 .../SUPERSET/0.15.0/package/scripts/params.py |  89 +++++++++++++
 .../0.15.0/package/scripts/service_check.py   |  37 ++++++
 .../0.15.0/package/scripts/status_params.py   |  25 ++++
 .../0.15.0/quicklinks/quicklinks.json         |  24 ++++
 .../SUPERSET/0.15.0/role_command_order.json   |   7 +
 .../SUPERSET/0.15.0/themes/theme.json         | 120 ++++++++++++++++++
 .../HDP/2.6/services/DRUID/kerberos.json      |   6 -
 .../HDP/2.6/services/SUPERSET/kerberos.json   |  51 ++++++++
 .../HDP/2.6/services/SUPERSET/metainfo.xml    |  28 ++++
 .../stacks/HDP/2.6/services/stack_advisor.py  |   8 +-
 .../server/upgrade/UpgradeCatalog260Test.java |  43 +++++++
 .../python/stacks/2.6/configs/default.json    |   2 +-
 23 files changed, 602 insertions(+), 194 deletions(-)
 create mode 100644 ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/metainfo.xml
 create mode 100644 ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/package/scripts/params.py
 create mode 100644 ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/package/scripts/service_check.py
 create mode 100644 ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/package/scripts/status_params.py
 create mode 100644 ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/quicklinks/quicklinks.json
 create mode 100644 ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/role_command_order.json
 create mode 100644 ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/themes/theme.json
 create mode 100644 ambari-server/src/main/resources/stacks/HDP/2.6/services/SUPERSET/kerberos.json
 create mode 100644 ambari-server/src/main/resources/stacks/HDP/2.6/services/SUPERSET/metainfo.xml

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog260.java b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog260.java
index ce84cc6717..2d094e5bbc 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog260.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog260.java
@@ -92,6 +92,10 @@ public class UpgradeCatalog260 extends AbstractUpgradeCatalog {
   public static final String FK_UPGRADE_HIST_FROM_REPO = "FK_upgrade_hist_from_repo";
   public static final String FK_UPGRADE_HIST_TARGET_REPO = "FK_upgrade_hist_target_repo";
   public static final String UQ_UPGRADE_HIST = "UQ_upgrade_hist";
  public static final String SERVICE_CONFIG_MAPPING_TABLE = "serviceconfigmapping";
  public static final String SERVICE_COMPONENT_DESIRED_STATE = "servicecomponentdesiredstate";
  public static final String HOST_COMPONENT_DESIRED_STATE = "hostcomponentdesiredstate";
  public static final String HOST_COMPONENT_STATE = "hostcomponentstate";
 
   /**
    * Logger.
@@ -117,14 +121,14 @@ public class UpgradeCatalog260 extends AbstractUpgradeCatalog {
    * {@inheritDoc}
    */
   @Override
  public String getSourceVersion() {
  public String getSourceVersion()
  {
     return "2.5.2";
   }
 
   /**
    * {@inheritDoc}
    */
  @Override
   public String getTargetVersion() {
     return "2.6.0";
   }
@@ -312,6 +316,7 @@ public class UpgradeCatalog260 extends AbstractUpgradeCatalog {
   @Override
   protected void executeDMLUpdates() throws AmbariException, SQLException {
     addNewConfigurationsFromXml();
    removeSupersetFromDruid();
   }
 
   public int getCurrentVersionID() throws AmbariException, SQLException {
@@ -322,4 +327,43 @@ public class UpgradeCatalog260 extends AbstractUpgradeCatalog {
     }
     return currentVersionList.get(0);
   }

  // Superset is moved as an independent service in Ambari-2.6.
  // This will remove superset component if installed under Druid Service
  protected void removeSupersetFromDruid() throws SQLException {
    removeComponent("DRUID_SUPERSET", "druid-superset");
  }

  private void removeComponent(String componentName, String configPrefix) throws SQLException {
    String supersetConfigMappingRemoveSQL = String.format(
        "DELETE FROM %s WHERE type_name like '%s%%'",
        CLUSTER_CONFIG_MAPPING_TABLE, configPrefix);

    String serviceConfigMappingRemoveSQL = String.format(
        "DELETE FROM %s WHERE config_id IN (SELECT config_id from %s where type_name like '%s%%')",
        SERVICE_CONFIG_MAPPING_TABLE, CLUSTER_CONFIG_TABLE, configPrefix);

    String supersetConfigRemoveSQL = String.format(
        "DELETE FROM %s WHERE type_name like '%s%%'",
        CLUSTER_CONFIG_TABLE, configPrefix);

    String hostComponentDesiredStateRemoveSQL = String.format(
        "DELETE FROM %s WHERE component_name = '%s'",
        HOST_COMPONENT_DESIRED_STATE, componentName);

    String hostComponentStateRemoveSQL = String.format(
        "DELETE FROM %s WHERE component_name = '%s'",
        HOST_COMPONENT_STATE, componentName);

    String serviceComponentDesiredStateRemoveSQL = String.format(
        "DELETE FROM %s WHERE component_name = '%s'",
        SERVICE_COMPONENT_DESIRED_STATE, componentName);

    dbAccessor.executeQuery(supersetConfigMappingRemoveSQL);
    dbAccessor.executeQuery(serviceConfigMappingRemoveSQL);
    dbAccessor.executeQuery(supersetConfigRemoveSQL);
    dbAccessor.executeQuery(hostComponentDesiredStateRemoveSQL);
    dbAccessor.executeQuery(hostComponentStateRemoveSQL);
    dbAccessor.executeQuery(serviceComponentDesiredStateRemoveSQL);
  }
 }
diff --git a/ambari-server/src/main/resources/common-services/DRUID/0.9.2/configuration/druid-superset-env.xml b/ambari-server/src/main/resources/common-services/DRUID/0.9.2/configuration/druid-superset-env.xml
index 728434203a..71fa3b66a9 100644
-- a/ambari-server/src/main/resources/common-services/DRUID/0.9.2/configuration/druid-superset-env.xml
++ b/ambari-server/src/main/resources/common-services/DRUID/0.9.2/configuration/druid-superset-env.xml
@@ -84,6 +84,18 @@
     <value></value>
     <on-ambari-upgrade add="true"/>
   </property>
  <property>
    <name>superset_user</name>
    <display-name>Superset User</display-name>
    <value>superset</value>
    <property-type>USER</property-type>
    <description></description>
    <value-attributes>
      <type>user</type>
      <overridable>false</overridable>
    </value-attributes>
    <on-ambari-upgrade add="false"/>
  </property>
   <property>
     <name>content</name>
     <display-name>superset-env template</display-name>
diff --git a/ambari-server/src/main/resources/common-services/DRUID/0.9.2/metainfo.xml b/ambari-server/src/main/resources/common-services/DRUID/0.9.2/metainfo.xml
index 404545c271..f9f1a35797 100644
-- a/ambari-server/src/main/resources/common-services/DRUID/0.9.2/metainfo.xml
++ b/ambari-server/src/main/resources/common-services/DRUID/0.9.2/metainfo.xml
@@ -168,21 +168,6 @@
             <config-type>druid-router</config-type>
           </configuration-dependencies>
         </component>
        <component>
          <name>DRUID_SUPERSET</name>
          <displayName>Druid Superset</displayName>
          <category>MASTER</category>
          <cardinality>1+</cardinality>
          <versionAdvertised>true</versionAdvertised>
          <commandScript>
            <script>scripts/superset.py</script>
            <scriptType>PYTHON</scriptType>
            <timeout>600</timeout>
          </commandScript>
          <configuration-dependencies>
            <config-type>druid-superset</config-type>
          </configuration-dependencies>
        </component>
       </components>
 
       <themes>
@@ -199,9 +184,6 @@
             <package>
               <name>druid_${stack_version}</name>
             </package>
            <package>
              <name>superset_${stack_version}</name>
            </package>
           </packages>
         </osSpecific>
         <osSpecific>
@@ -210,9 +192,6 @@
             <package>
               <name>druid-${stack_version}</name>
             </package>
            <package>
              <name>superset-${stack_version}</name>
            </package>
           </packages>
         </osSpecific>
       </osSpecifics>
diff --git a/ambari-server/src/main/resources/common-services/DRUID/0.9.2/package/scripts/params.py b/ambari-server/src/main/resources/common-services/DRUID/0.9.2/package/scripts/params.py
index 7e90475a15..fd1cde6134 100644
-- a/ambari-server/src/main/resources/common-services/DRUID/0.9.2/package/scripts/params.py
++ b/ambari-server/src/main/resources/common-services/DRUID/0.9.2/package/scripts/params.py
@@ -192,51 +192,6 @@ if has_metric_collector:
         metric_collector_protocol = 'http'
     pass
 
superset_home_dir = format("{stack_root}/current/druid-superset")
superset_bin_dir = format("{superset_home_dir}/bin")
superset_log_dir = default("/configurations/druid-superset-env/superset_log_dir", '/var/log/superset')
superset_pid_dir = status_params.superset_pid_dir
superset_config_dir = '/etc/superset/conf'
superset_admin_user = config['configurations']['druid-superset-env']['superset_admin_user']
superset_admin_password = config['configurations']['druid-superset-env']['superset_admin_password']
superset_admin_firstname = config['configurations']['druid-superset-env']['superset_admin_firstname']
superset_admin_lastname = config['configurations']['druid-superset-env']['superset_admin_lastname']
superset_admin_email = config['configurations']['druid-superset-env']['superset_admin_email']
superset_env_sh_template = config['configurations']['druid-superset-env']['content']
superset_protocol = "http"
superset_webserver_address=config['configurations']['druid-superset']['SUPERSET_WEBSERVER_ADDRESS']
superset_webserver_port = config['configurations']['druid-superset']['SUPERSET_WEBSERVER_PORT']
superset_timeout = config['configurations']['druid-superset']['SUPERSET_TIMEOUT']
superset_workers =  config['configurations']['druid-superset']['SUPERSET_WORKERS']
superset_hosts = default('/clusterHostInfo/superset_hosts', None)

# superset database configs
superset_db_type = config['configurations']['druid-superset']['SUPERSET_DATABASE_TYPE']
superset_db_name = config['configurations']['druid-superset']['SUPERSET_DATABASE_NAME']
superset_db_password = config['configurations']['druid-superset']['SUPERSET_DATABASE_PASSWORD']
superset_db_user = config['configurations']['druid-superset']['SUPERSET_DATABASE_USER']
superset_db_port = config['configurations']['druid-superset']['SUPERSET_DATABASE_PORT']
superset_db_host = config['configurations']['druid-superset']['SUPERSET_DATABASE_HOSTNAME']

superset_db_uri = None
if superset_db_type == "sqlite":
  superset_db_uri = format("sqlite:///{superset_config_dir}/{superset_db_name}.db")
elif superset_db_type == "postgresql":
  superset_db_uri = format("postgresql+pygresql://{superset_db_user}:{superset_db_password}@{superset_db_host}:{superset_db_port}/{superset_db_name}")
elif superset_db_type == "mysql":
  superset_db_uri = format("mysql+pymysql://{superset_db_user}:{superset_db_password}@{superset_db_host}:{superset_db_port}/{superset_db_name}")

druid_coordinator_hosts = default("/clusterHostInfo/druid_coordinator_hosts", [])
druid_coordinator_host = ""
if not len(druid_coordinator_hosts) == 0:
  druid_coordinator_host = druid_coordinator_hosts[0]
druid_router_hosts = default("/clusterHostInfo/druid_router_hosts", [])
druid_router_host = ""
if not len(druid_router_hosts) == 0:
  druid_router_host = druid_router_hosts[0]
druid_coordinator_port = config['configurations']['druid-coordinator']['druid.port']
druid_router_port = config['configurations']['druid-router']['druid.port']

 # Create current Hadoop Clients  Libs
 stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
 io_compression_codecs = default("/configurations/core-site/io.compression.codecs", None)
diff --git a/ambari-server/src/main/resources/common-services/DRUID/0.9.2/package/scripts/status_params.py b/ambari-server/src/main/resources/common-services/DRUID/0.9.2/package/scripts/status_params.py
index d48ff83b35..ee1d61cc6c 100644
-- a/ambari-server/src/main/resources/common-services/DRUID/0.9.2/package/scripts/status_params.py
++ b/ambari-server/src/main/resources/common-services/DRUID/0.9.2/package/scripts/status_params.py
@@ -22,5 +22,3 @@ from resource_management.libraries.script.script import Script
 config = Script.get_config()
 
 druid_pid_dir = config['configurations']['druid-env']['druid_pid_dir']
superset_pid_dir = config['configurations']['druid-superset-env']['superset_pid_dir']

diff --git a/ambari-server/src/main/resources/common-services/DRUID/0.9.2/package/scripts/superset.py b/ambari-server/src/main/resources/common-services/DRUID/0.9.2/package/scripts/superset.py
index a5dd4fb8da..adbe7393ad 100644
-- a/ambari-server/src/main/resources/common-services/DRUID/0.9.2/package/scripts/superset.py
++ b/ambari-server/src/main/resources/common-services/DRUID/0.9.2/package/scripts/superset.py
@@ -35,6 +35,9 @@ from resource_management.libraries.resources.properties_file import PropertiesFi
 
 class Superset(Script):
 
  def get_component_name(self):
    return format("superset")

   def install(self, env):
     self.install_packages(env)
 
@@ -44,7 +47,7 @@ class Superset(Script):
       [params.superset_pid_dir, params.superset_log_dir, params.superset_config_dir, params.superset_home_dir],
       mode=0755,
       cd_access='a',
      owner=params.druid_user,
      owner=params.superset_user,
       group=params.user_group,
       create_parents=True,
       recursive_ownership=True
@@ -52,18 +55,18 @@ class Superset(Script):
 
     File(format("{params.superset_config_dir}/superset-env.sh"),
          mode=0755,
         owner=params.druid_user,
         owner=params.superset_user,
          group=params.user_group,
          content=InlineTemplate(params.superset_env_sh_template)
          )
 
     File(os.path.join(params.superset_bin_dir, 'superset.sh'),
         owner=params.druid_user,
         owner=params.superset_user,
          group=params.user_group,
          mode=0755,
          content=Template("superset.sh")
          )
    superset_config =  mutable_config_dict(params.config["configurations"]["druid-superset"])
    superset_config =  mutable_config_dict(params.config["configurations"]["superset"])
 
     if params.superset_db_uri:
       superset_config["SQLALCHEMY_DATABASE_URI"] = params.superset_db_uri
@@ -71,24 +74,25 @@ class Superset(Script):
     PropertiesFile("superset_config.py",
                    dir=params.superset_config_dir,
                    properties=quote_string_values(superset_config),
                   owner=params.druid_user,
                   owner=params.superset_user,
                    group=params.user_group
                    )
 
     # Initialize DB and create admin user.
     Execute(format("source {params.superset_config_dir}/superset-env.sh ; {params.superset_bin_dir}/superset db upgrade"),
            user=params.druid_user)
            user=params.superset_user)
     Execute(format("source {params.superset_config_dir}/superset-env.sh ; {params.superset_bin_dir}/fabmanager create-admin --app superset --username '{params.superset_admin_user}' --password '{params.superset_admin_password!p}' --firstname '{params.superset_admin_firstname}' --lastname '{params.superset_admin_lastname}' --email '{params.superset_admin_email}'"),
            user=params.druid_user)
            user=params.superset_user)
     Execute(format("source {params.superset_config_dir}/superset-env.sh ; {params.superset_bin_dir}/superset init"),
            user=params.druid_user)
            user=params.superset_user)
 
     # Configure Druid Cluster in superset DB
    Execute(format("source {params.superset_config_dir}/superset-env.sh ; {params.superset_bin_dir}/superset configure_druid_cluster --name druid-ambari --coordinator-host {params.druid_coordinator_host} --coordinator-port {params.druid_coordinator_port} --broker-host {params.druid_router_host} --broker-port {params.druid_router_port} --coordinator-endpoint druid/coordinator/v1/metadata --broker-endpoint druid/v2"),
            user=params.druid_user)
    if len(params.druid_coordinator_hosts) > 0 :
      Execute(format("source {params.superset_config_dir}/superset-env.sh ; {params.superset_bin_dir}/superset configure_druid_cluster --name druid-ambari --coordinator-host {params.druid_coordinator_host} --coordinator-port {params.druid_coordinator_port} --broker-host {params.druid_router_host} --broker-port {params.druid_router_port} --coordinator-endpoint druid/coordinator/v1/metadata --broker-endpoint druid/v2"),
            user=params.superset_user)
 
   def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing druid-superset Upgrade pre-restart")
    Logger.info("Executing superset Upgrade pre-restart")
     import params
 
     env.set_params(params)
@@ -103,10 +107,10 @@ class Superset(Script):
     daemon_cmd = self.get_daemon_cmd(params, "start")
     try:
       Execute(daemon_cmd,
              user=params.druid_user
              user=params.superset_user
               )
     except:
      show_logs(params.druid_log_dir, params.druid_user)
      show_logs(params.superset_log_dir, params.superset_user)
       raise
 
   def stop(self, env, upgrade_type=None):
@@ -116,10 +120,10 @@ class Superset(Script):
     daemon_cmd = self.get_daemon_cmd(params, "stop")
     try:
       Execute(daemon_cmd,
              user=params.druid_user
              user=params.superset_user
               )
     except:
      show_logs(params.druid_log_dir, params.druid_user)
      show_logs(params.superset_log_dir, params.superset_user)
       raise
 
   def status(self, env):
@@ -130,11 +134,11 @@ class Superset(Script):
 
   def get_log_folder(self):
     import params
    return params.druid_log_dir
    return params.superset_log_dir
 
   def get_user(self):
     import params
    return params.druid_user
    return params.superset_user
 
   def get_daemon_cmd(self, params=None, command=None):
     return format('source {params.superset_config_dir}/superset-env.sh ; {params.superset_bin_dir}/superset.sh {command}')
@@ -160,4 +164,4 @@ def quote_string_value(value):
 
 
 if __name__ == "__main__":
  Superset().execute()
\ No newline at end of file
  Superset().execute()
diff --git a/ambari-server/src/main/resources/common-services/DRUID/0.9.2/package/templates/superset.sh b/ambari-server/src/main/resources/common-services/DRUID/0.9.2/package/templates/superset.sh
index 3e327a6c21..ffef1fe6b0 100644
-- a/ambari-server/src/main/resources/common-services/DRUID/0.9.2/package/templates/superset.sh
++ b/ambari-server/src/main/resources/common-services/DRUID/0.9.2/package/templates/superset.sh
@@ -20,7 +20,7 @@
 
 ## Runs superset as a daemon
 ## Environment Variables used by this script -
## SUPERSET_CONFIG_DIR - directory having druid config files
## SUPERSET_CONFIG_DIR - directory having superset config files
 ## SUPERSET_LOG_DIR - directory used to store superset logs
 ## SUPERSET_PID_DIR - directory used to store pid file
 
diff --git a/ambari-server/src/main/resources/common-services/DRUID/0.9.2/quicklinks/quicklinks.json b/ambari-server/src/main/resources/common-services/DRUID/0.9.2/quicklinks/quicklinks.json
index 16f5d5c990..c68b9b90d1 100644
-- a/ambari-server/src/main/resources/common-services/DRUID/0.9.2/quicklinks/quicklinks.json
++ b/ambari-server/src/main/resources/common-services/DRUID/0.9.2/quicklinks/quicklinks.json
@@ -31,19 +31,6 @@
           "regex": "^(\\d+)$",
           "site": "druid-overlord"
         }
      },
      {
        "name": "superset",
        "label": "Superset",
        "component_name": "DRUID_SUPERSET",
        "requires_user_name": "false",
        "url": "%@://%@:%@",
        "port": {
          "http_property": "SUPERSET_WEBSERVER_PORT",
          "http_default_port": "9088",
          "regex": "^(\\d+)$",
          "site": "druid-superset"
        }
       }
     ]
   }
diff --git a/ambari-server/src/main/resources/common-services/DRUID/0.9.2/role_command_order.json b/ambari-server/src/main/resources/common-services/DRUID/0.9.2/role_command_order.json
index 8f65c2e3d6..4d697fe8f6 100644
-- a/ambari-server/src/main/resources/common-services/DRUID/0.9.2/role_command_order.json
++ b/ambari-server/src/main/resources/common-services/DRUID/0.9.2/role_command_order.json
@@ -7,12 +7,11 @@
     "DRUID_BROKER-START" : ["ZOOKEEPER_SERVER-START", "NAMENODE-START", "DATANODE-START", "RESOURCEMANAGER-START", "NODEMANAGER-START"],
     "DRUID_ROUTER-START" : ["ZOOKEEPER_SERVER-START", "NAMENODE-START", "DATANODE-START", "RESOURCEMANAGER-START", "NODEMANAGER-START"],
     "DRUID_COORDINATOR-START" : ["ZOOKEEPER_SERVER-START", "NAMENODE-START", "DATANODE-START", "RESOURCEMANAGER-START", "NODEMANAGER-START"],
    "DRUID_SUPERSET-START" : ["DRUID_COORDINATOR-START", "DRUID_BROKER-START"],
     "DRUID_OVERLORD-RESTART" : ["DRUID_HISTORICAL-RESTART"],
     "DRUID_MIDDLEMANAGER-RESTART" : ["DRUID_OVERLORD-RESTART"],
     "DRUID_BROKER-RESTART" : ["DRUID_MIDDLEMANAGER-RESTART"],
     "DRUID_ROUTER-RESTART" : ["DRUID_BROKER-RESTART"],
     "DRUID_COORDINATOR-RESTART" : ["DRUID_ROUTER-RESTART"],
    "DRUID_SERVICE_CHECK-SERVICE_CHECK" : ["DRUID_HISTORICAL-START", "DRUID_COORDINATOR-START", "DRUID_OVERLORD-START", "DRUID_MIDDLEMANAGER-START", "DRUID_BROKER-START", "DRUID_ROUTER-START", "DRUID_SUPERSET-START"]
    "DRUID_SERVICE_CHECK-SERVICE_CHECK" : ["DRUID_HISTORICAL-START", "DRUID_COORDINATOR-START", "DRUID_OVERLORD-START", "DRUID_MIDDLEMANAGER-START", "DRUID_BROKER-START", "DRUID_ROUTER-START"]
   }
 }
diff --git a/ambari-server/src/main/resources/common-services/DRUID/0.9.2/themes/theme.json b/ambari-server/src/main/resources/common-services/DRUID/0.9.2/themes/theme.json
index 151478baa5..7033e19f19 100644
-- a/ambari-server/src/main/resources/common-services/DRUID/0.9.2/themes/theme.json
++ b/ambari-server/src/main/resources/common-services/DRUID/0.9.2/themes/theme.json
@@ -10,7 +10,7 @@
             "name": "metadata_storage",
             "display-name": "META DATA STORAGE CONFIG",
             "layout": {
              "tab-columns": "2",
              "tab-columns": "",
               "tab-rows": "1",
               "sections": [
                 {
@@ -19,7 +19,7 @@
                   "row-index": "0",
                   "column-index": "0",
                   "row-span": "2",
                  "column-span": "2",
                  "column-span": "1",
                   "section-columns": "1",
                   "section-rows": "1",
                   "subsections": [
@@ -30,14 +30,6 @@
                       "column-index": "0",
                       "row-span": "1",
                       "column-span": "1"
                    },
                    {
                      "name": "subsection-metadata-storage-row1-col2",
                      "display-name": "SUPERSET META DATA STORAGE",
                      "row-index": "0",
                      "column-index": "1",
                      "row-span": "1",
                      "column-span": "1"
                     }
                   ]
                 }
@@ -77,34 +69,6 @@
         {
           "config": "druid-common/druid.metadata.storage.connector.connectURI",
           "subsection-name": "subsection-metadata-storage-row1-col1"
        },
        {
          "config": "druid-superset/SUPERSET_DATABASE_NAME",
          "subsection-name": "subsection-metadata-storage-row1-col2"
        },
        {
          "config": "druid-superset/SUPERSET_DATABASE_TYPE",
          "subsection-name": "subsection-metadata-storage-row1-col2"
        },
        {
          "config": "druid-superset/SUPERSET_DATABASE_USER",
          "subsection-name": "subsection-metadata-storage-row1-col2"
        },
        {
          "config": "druid-superset/SUPERSET_DATABASE_PASSWORD",
          "subsection-name": "subsection-metadata-storage-row1-col2"
        },
        {
          "config": "druid-superset/SUPERSET_DATABASE_HOSTNAME",
          "subsection-name": "subsection-metadata-storage-row1-col2"
        },
        {
          "config": "druid-superset/SUPERSET_DATABASE_PORT",
          "subsection-name": "subsection-metadata-storage-row1-col2"
        },
        {
          "config": "druid-superset/SECRET_KEY",
          "subsection-name": "subsection-metadata-storage-row1-col2"
         }
       ]
     },
@@ -150,48 +114,6 @@
         "widget": {
           "type": "text-field"
         }
      },
      {
        "config": "druid-superset/SUPERSET_DATABASE_NAME",
        "widget": {
          "type": "text-field"
        }
      },
      {
        "config": "druid-superset/SUPERSET_DATABASE_TYPE",
        "widget": {
          "type": "combo"
        }
      },
      {
        "config": "druid-superset/SUPERSET_DATABASE_USER",
        "widget": {
          "type": "text-field"
        }
      },
      {
        "config": "druid-superset/SUPERSET_DATABASE_PASSWORD",
        "widget": {
          "type": "password"
        }
      },
      {
        "config": "druid-superset/SUPERSET_DATABASE_HOSTNAME",
        "widget": {
          "type": "text-field"
        }
      },
      {
        "config": "druid-superset/SUPERSET_DATABASE_PORT",
        "widget": {
          "type": "text-field"
        }
      },
      {
        "config": "druid-superset/SECRET_KEY",
        "widget": {
          "type": "password"
        }
       }
     ]
   }
diff --git a/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/metainfo.xml b/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/metainfo.xml
new file mode 100644
index 0000000000..5c6ed117fd
-- /dev/null
++ b/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/metainfo.xml
@@ -0,0 +1,88 @@
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
      <name>SUPERSET</name>
      <displayName>Superset</displayName>
      <comment>Superset is a data exploration platform designed to be visual, intuitive and interactive.</comment>
      <version>0.15.0</version>
      <components>
        <component>
          <name>SUPERSET</name>
          <displayName>Superset</displayName>
          <category>MASTER</category>
          <cardinality>1+</cardinality>
          <versionAdvertised>true</versionAdvertised>
          <commandScript>
            <script>scripts/superset.py</script>
            <scriptType>PYTHON</scriptType>
            <timeout>600</timeout>
          </commandScript>
          <configuration-dependencies>
            <config-type>superset</config-type>
          </configuration-dependencies>
        </component>
      </components>

      <themes>
        <theme>
          <fileName>theme.json</fileName>
          <default>true</default>
        </theme>
      </themes>

      <osSpecifics>
        <osSpecific>
          <osFamily>redhat7,amazon2015,redhat6,suse11,suse12</osFamily>
          <packages>
            <package>
              <name>superset_${stack_version}</name>
            </package>
          </packages>
        </osSpecific>
        <osSpecific>
          <osFamily>debian7,ubuntu12,ubuntu14,ubuntu16</osFamily>
          <packages>
            <package>
              <name>superset-${stack_version}</name>
            </package>
          </packages>
        </osSpecific>
      </osSpecifics>
      <commandScript>
        <script>scripts/service_check.py</script>
        <scriptType>PYTHON</scriptType>
        <timeout>300</timeout>
      </commandScript>
      <configuration-dependencies>
        <config-type>superset-env</config-type>
        <config-type>superset</config-type>
      </configuration-dependencies>

      <quickLinksConfigurations>
        <quickLinksConfiguration>
          <fileName>quicklinks.json</fileName>
          <default>true</default>
        </quickLinksConfiguration>
      </quickLinksConfigurations>

    </service>
  </services>
</metainfo>
diff --git a/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/package/scripts/params.py b/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/package/scripts/params.py
new file mode 100644
index 0000000000..b38d6438aa
-- /dev/null
++ b/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/package/scripts/params.py
@@ -0,0 +1,89 @@
#!/usr/bin/env python
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

"""
from ambari_commons import OSCheck
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import format
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
from resource_management.libraries.functions.default import default

import status_params

# server configurations
config = Script.get_config()
stack_root = Script.get_stack_root()
stack_name = default("/hostLevelParams/stack_name", None)
user_group = config['configurations']['cluster-env']['user_group']

# stack version
stack_version = default("/commandParams/version", None)

hostname = config['hostname']

# status params
status_pid_dir = status_params.superset_pid_dir

superset_home_dir = format("{stack_root}/current/superset")
superset_bin_dir = format("{superset_home_dir}/bin")
superset_log_dir = default("/configurations/superset-env/superset_log_dir", '/var/log/superset')
superset_pid_dir = status_params.superset_pid_dir
superset_config_dir = '/etc/superset/conf'
superset_admin_user = config['configurations']['superset-env']['superset_admin_user']
superset_admin_password = config['configurations']['superset-env']['superset_admin_password']
superset_admin_firstname = config['configurations']['superset-env']['superset_admin_firstname']
superset_admin_lastname = config['configurations']['superset-env']['superset_admin_lastname']
superset_admin_email = config['configurations']['superset-env']['superset_admin_email']
superset_env_sh_template = config['configurations']['superset-env']['content']
superset_user = config['configurations']['superset-env']['superset_user']
superset_protocol = "http"
superset_webserver_address=config['configurations']['superset']['SUPERSET_WEBSERVER_ADDRESS']
superset_webserver_port = config['configurations']['superset']['SUPERSET_WEBSERVER_PORT']
superset_timeout = config['configurations']['superset']['SUPERSET_TIMEOUT']
superset_workers =  config['configurations']['superset']['SUPERSET_WORKERS']
superset_hosts = default('/clusterHostInfo/superset_hosts', None)

# superset database configs
superset_db_type = config['configurations']['superset']['SUPERSET_DATABASE_TYPE']
superset_db_name = config['configurations']['superset']['SUPERSET_DATABASE_NAME']
superset_db_password = config['configurations']['superset']['SUPERSET_DATABASE_PASSWORD']
superset_db_user = config['configurations']['superset']['SUPERSET_DATABASE_USER']
superset_db_port = config['configurations']['superset']['SUPERSET_DATABASE_PORT']
superset_db_host = config['configurations']['superset']['SUPERSET_DATABASE_HOSTNAME']

superset_db_uri = None
if superset_db_type == "sqlite":
  superset_db_uri = format("sqlite:///{superset_config_dir}/{superset_db_name}.db")
elif superset_db_type == "postgresql":
  superset_db_uri = format("postgresql+pygresql://{superset_db_user}:{superset_db_password}@{superset_db_host}:{superset_db_port}/{superset_db_name}")
elif superset_db_type == "mysql":
  superset_db_uri = format("mysql+pymysql://{superset_db_user}:{superset_db_password}@{superset_db_host}:{superset_db_port}/{superset_db_name}")

druid_coordinator_hosts = default("/clusterHostInfo/druid_coordinator_hosts", [])

if not len(druid_coordinator_hosts) == 0:
  druid_coordinator_host = druid_coordinator_hosts[0]
  druid_coordinator_port = config['configurations']['druid-coordinator']['druid.port']
druid_router_hosts = default("/clusterHostInfo/druid_router_hosts", [])
if not len(druid_router_hosts) == 0:
  druid_router_host = druid_router_hosts[0]
  druid_router_port = config['configurations']['druid-router']['druid.port']
diff --git a/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/package/scripts/service_check.py b/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/package/scripts/service_check.py
new file mode 100644
index 0000000000..1204c0839a
-- /dev/null
++ b/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/package/scripts/service_check.py
@@ -0,0 +1,37 @@
#!/usr/bin/env python
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

"""
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.format import format
from resource_management.core.resources.system import Execute


class ServiceCheck(Script):
  def service_check(self, env):
    import params
    env.set_params(params)
    for superset_host in params.config['clusterHostInfo']['superset_hosts']:
      Execute(format(
        "curl -s -o /dev/null -w'%{{http_code}}' --negotiate -u: -k {superset_host}:{params.superset_webserver_port}/health | grep 200"),
        tries=10,
        try_sleep=3,
        logoutput=True)

if __name__ == "__main__":
  ServiceCheck().execute()
diff --git a/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/package/scripts/status_params.py b/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/package/scripts/status_params.py
new file mode 100644
index 0000000000..c0009c3a84
-- /dev/null
++ b/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/package/scripts/status_params.py
@@ -0,0 +1,25 @@
#!/usr/bin/env python
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

"""
from resource_management.libraries.script.script import Script

config = Script.get_config()

superset_pid_dir = config['configurations']['superset-env']['superset_pid_dir']

diff --git a/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/quicklinks/quicklinks.json b/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/quicklinks/quicklinks.json
new file mode 100644
index 0000000000..89e5c7004f
-- /dev/null
++ b/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/quicklinks/quicklinks.json
@@ -0,0 +1,24 @@
{
  "name": "default",
  "description": "default quick links configuration",
  "configuration": {
    "protocol": {
      "type": "HTTP_ONLY"
    },
    "links": [
      {
        "name": "superset",
        "label": "Superset",
        "component_name": "SUPERSET",
        "requires_user_name": "false",
        "url": "%@://%@:%@",
        "port": {
          "http_property": "SUPERSET_WEBSERVER_PORT",
          "http_default_port": "9088",
          "regex": "^(\\d+)$",
          "site": "superset"
        }
      }
    ]
  }
}
diff --git a/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/role_command_order.json b/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/role_command_order.json
new file mode 100644
index 0000000000..905af5b0ac
-- /dev/null
++ b/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/role_command_order.json
@@ -0,0 +1,7 @@
{
  "general_deps" : {
    "_comment" : "dependencies for Superset",
    "SUPERSET-START" : ["DRUID_COORDINATOR-START", "DRUID_BROKER-START"],
    "SUPERSET_SERVICE_CHECK-SERVICE_CHECK" : ["SUPERSET-START"]
  }
}
diff --git a/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/themes/theme.json b/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/themes/theme.json
new file mode 100644
index 0000000000..a2c6001a07
-- /dev/null
++ b/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/themes/theme.json
@@ -0,0 +1,120 @@
{
  "name": "default",
  "description": "Default theme for Superset service",
  "configuration": {
    "layouts": [
      {
        "name": "default",
        "tabs": [
          {
            "name": "metadata_storage",
            "display-name": "META DATA STORAGE CONFIG",
            "layout": {
              "tab-columns": "1",
              "tab-rows": "1",
              "sections": [
                {
                  "name": "section-metadata-storage",
                  "display-name": "",
                  "row-index": "0",
                  "column-index": "0",
                  "row-span": "1",
                  "column-span": "1",
                  "section-columns": "1",
                  "section-rows": "1",
                  "subsections": [
                    {
                      "name": "subsection-metadata-storage-row1-col1",
                      "display-name": "SUPERSET META DATA STORAGE",
                      "row-index": "0",
                      "column-index": "0",
                      "row-span": "1",
                      "column-span": "1"
                    }
                  ]
                }
              ]
            }
          }
        ]
      }
    ],
    "placement": {
      "configuration-layout": "default",
      "configs": [
        {
          "config": "superset/SUPERSET_DATABASE_NAME",
          "subsection-name": "subsection-metadata-storage-row1-col1"
        },
        {
          "config": "superset/SUPERSET_DATABASE_TYPE",
          "subsection-name": "subsection-metadata-storage-row1-col1"
        },
        {
          "config": "superset/SUPERSET_DATABASE_USER",
          "subsection-name": "subsection-metadata-storage-row1-col1"
        },
        {
          "config": "superset/SUPERSET_DATABASE_PASSWORD",
          "subsection-name": "subsection-metadata-storage-row1-col1"
        },
        {
          "config": "superset/SUPERSET_DATABASE_HOSTNAME",
          "subsection-name": "subsection-metadata-storage-row1-col1"
        },
        {
          "config": "superset/SUPERSET_DATABASE_PORT",
          "subsection-name": "subsection-metadata-storage-row1-col1"
        },
        {
          "config": "superset/SECRET_KEY",
          "subsection-name": "subsection-metadata-storage-row1-col1"
        }
      ]
    },
    "widgets": [
      {
        "config": "superset/SUPERSET_DATABASE_NAME",
        "widget": {
          "type": "text-field"
        }
      },
      {
        "config": "superset/SUPERSET_DATABASE_TYPE",
        "widget": {
          "type": "combo"
        }
      },
      {
        "config": "superset/SUPERSET_DATABASE_USER",
        "widget": {
          "type": "text-field"
        }
      },
      {
        "config": "superset/SUPERSET_DATABASE_PASSWORD",
        "widget": {
          "type": "password"
        }
      },
      {
        "config": "superset/SUPERSET_DATABASE_HOSTNAME",
        "widget": {
          "type": "text-field"
        }
      },
      {
        "config": "superset/SUPERSET_DATABASE_PORT",
        "widget": {
          "type": "text-field"
        }
      },
      {
        "config": "superset/SECRET_KEY",
        "widget": {
          "type": "password"
        }
      }
    ]
  }
}
\ No newline at end of file
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.6/services/DRUID/kerberos.json b/ambari-server/src/main/resources/stacks/HDP/2.6/services/DRUID/kerberos.json
index 6aefc630f5..5b6b5737a7 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.6/services/DRUID/kerberos.json
++ b/ambari-server/src/main/resources/stacks/HDP/2.6/services/DRUID/kerberos.json
@@ -114,12 +114,6 @@
             "druid.hadoop.security.spnego.excludedPaths": "[\"/status\"]",
             "druid.security.extensions.loadList": "[\"druid-kerberos\"]"
           }
        },
        {
          "druid-superset": {
            "ENABLE_KERBEROS_AUTHENTICATION": "True",
            "KERBEROS_REINIT_TIME_SEC": "3600"
          }
         }
       ],
       "auth_to_local_properties": [
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.6/services/SUPERSET/kerberos.json b/ambari-server/src/main/resources/stacks/HDP/2.6/services/SUPERSET/kerberos.json
new file mode 100644
index 0000000000..b79bac9805
-- /dev/null
++ b/ambari-server/src/main/resources/stacks/HDP/2.6/services/SUPERSET/kerberos.json
@@ -0,0 +1,51 @@
{
  "services": [
    {
      "name": "SUPERSET",
      "identities": [
        {
          "name": "superset",
          "principal": {
            "value": "${superset-env/superset_user}@${realm}",
            "type": "user",
            "configuration": "superset/KERBEROS_PRINCIPAL",
            "local_username": "${superset-env/superset_user}"
          },
          "keytab": {
            "file": "${keytab_dir}/superset.headless.keytab",
            "owner": {
              "name": "${superset-env/superset_user}",
              "access": "r"
            },
            "group": {
              "name": "${cluster-env/user_group}",
              "access": "r"
            },
            "configuration": "superset/KERBEROS_KEYTAB"
          }
        },
        {
          "name": "/smokeuser"
        }
      ],
      "components": [
        {
          "name": "SUPERSET",
          "identities": [
            {
              "name": "/superset"
            }
          ]
        }
      ],
      "configurations": [
        {
          "superset": {
            "ENABLE_KERBEROS_AUTHENTICATION": "True",
            "KERBEROS_REINIT_TIME_SEC": "3600"
          }
        }
      ]
    }
  ]
}
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.6/services/SUPERSET/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.6/services/SUPERSET/metainfo.xml
new file mode 100644
index 0000000000..2fd15726ef
-- /dev/null
++ b/ambari-server/src/main/resources/stacks/HDP/2.6/services/SUPERSET/metainfo.xml
@@ -0,0 +1,28 @@
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
      <name>SUPERSET</name>
      <version>0.15.0</version>
      <extends>common-services/SUPERSET/0.15.0</extends>
      <selection>TECH_PREVIEW</selection>
    </service>
  </services>
</metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.6/services/stack_advisor.py b/ambari-server/src/main/resources/stacks/HDP/2.6/services/stack_advisor.py
index 6108351b31..723ff4e3b1 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.6/services/stack_advisor.py
++ b/ambari-server/src/main/resources/stacks/HDP/2.6/services/stack_advisor.py
@@ -32,6 +32,7 @@ class HDP26StackAdvisor(HDP25StackAdvisor):
       parentRecommendConfDict = super(HDP26StackAdvisor, self).getServiceConfigurationRecommenderDict()
       childRecommendConfDict = {
         "DRUID": self.recommendDruidConfigurations,
        "SUPERSET": self.recommendSupersetConfigurations,
         "ATLAS": self.recommendAtlasConfigurations,
         "TEZ": self.recommendTezConfigurations,
         "RANGER": self.recommendRangerConfigurations,
@@ -156,11 +157,12 @@ class HDP26StackAdvisor(HDP25StackAdvisor):
               putComponentProperty('druid.processing.numThreads', processingThreads)
               putComponentProperty('druid.server.http.numThreads', max(10, (totalAvailableCpu * 17) / 16 + 2) + 30)
 
  def recommendSupersetConfigurations(self, configurations, clusterData, services, hosts):
       # superset is in list of services to be installed
      if 'druid-superset' in services['configurations']:
      if 'superset' in services['configurations']:
         # Recommendations for Superset
        superset_database_type = services['configurations']["druid-superset"]["properties"]["SUPERSET_DATABASE_TYPE"]
        putSupersetProperty = self.putProperty(configurations, "druid-superset", services)
        superset_database_type = services['configurations']["superset"]["properties"]["SUPERSET_DATABASE_TYPE"]
        putSupersetProperty = self.putProperty(configurations, "superset", services)
 
         if superset_database_type == "mysql":
             putSupersetProperty("SUPERSET_DATABASE_PORT", "3306")
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog260Test.java b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog260Test.java
index 44b5d914fd..df15657152 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog260Test.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog260Test.java
@@ -464,5 +464,48 @@ public class UpgradeCatalog260Test {
         capture(scdcaptureKey), capture(scdcaptureValue), eq(false))).andReturn(current).once();
   }
 
  @Test
  public void testRemoveDruidSuperset() throws Exception {

    List<Integer> current = new ArrayList<Integer>();
    current.add(1);

    expect(dbAccessor.getConnection()).andReturn(connection).anyTimes();
    expect(connection.createStatement()).andReturn(statement).anyTimes();
    expect(statement.executeQuery(anyObject(String.class))).andReturn(resultSet).anyTimes();
    expect(configuration.getDatabaseType()).andReturn(Configuration.DatabaseType.POSTGRES).anyTimes();

    dbAccessor.executeQuery("DELETE FROM clusterconfigmapping WHERE type_name like 'druid-superset%'");
    expectLastCall().once();
    dbAccessor.executeQuery("DELETE FROM serviceconfigmapping WHERE config_id IN (SELECT config_id from clusterconfig where type_name like 'druid-superset%')");
    expectLastCall().once();
    dbAccessor.executeQuery("DELETE FROM clusterconfig WHERE type_name like 'druid-superset%'");
    expectLastCall().once();
    dbAccessor.executeQuery("DELETE FROM hostcomponentdesiredstate WHERE component_name = 'DRUID_SUPERSET'");
    expectLastCall().once();
    dbAccessor.executeQuery("DELETE FROM hostcomponentstate WHERE component_name = 'DRUID_SUPERSET'");
    expectLastCall().once();
    dbAccessor.executeQuery("DELETE FROM servicecomponentdesiredstate WHERE component_name = 'DRUID_SUPERSET'");
    expectLastCall().once();
    replay(dbAccessor, configuration, connection, statement, resultSet);

    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(dbAccessor);
        binder.bind(OsFamily.class).toInstance(osFamily);
        binder.bind(EntityManager.class).toInstance(entityManager);
        binder.bind(Configuration.class).toInstance(configuration);
      }
    };

    Injector injector = Guice.createInjector(module);
    UpgradeCatalog260 upgradeCatalog260 = injector.getInstance(UpgradeCatalog260.class);
    upgradeCatalog260.removeSupersetFromDruid();

    verify(dbAccessor);

  }

 
 }
diff --git a/ambari-server/src/test/python/stacks/2.6/configs/default.json b/ambari-server/src/test/python/stacks/2.6/configs/default.json
index add31d04a8..31f3dbde71 100644
-- a/ambari-server/src/test/python/stacks/2.6/configs/default.json
++ b/ambari-server/src/test/python/stacks/2.6/configs/default.json
@@ -463,7 +463,7 @@
     "druid-logrotate" : {
       "content" : "<![CDATA[\n    {{druid_log_dir}}/*.log {\n        copytruncate\n        rotate 7\n        daily\n        nocompress\n        missingok\n        notifempty\n        create 660 druid users\n        dateext\n        dateformat -%Y-%m-%d-%s\n        }\n      ]]>\n"
     },
    "druid-superset" : {
    "superset" : {
       "SUPERSET_DATABASE_TYPE" : "sqllite"
     }
   },
- 
2.19.1.windows.1

