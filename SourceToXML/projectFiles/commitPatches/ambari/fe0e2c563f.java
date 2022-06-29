From fe0e2c563fae61b5f5e12bd2d45b4fd504b6f275 Mon Sep 17 00:00:00 2001
From: Yusaku Sako <yusaku@apache.org>
Date: Thu, 30 May 2013 22:03:58 +0000
Subject: [PATCH] AMBARI-2234. Host Details Page: Update host component actions
 and icons depending on state. (yusaku) AMBARI-2230. Nagios user is presented
 in Admin > Misc page when Nagios service was not installed. (yusaku)
 AMBARI-2231. Service web UI links map to hostnames not resolvable by the
 client. (yusaku)

git-svn-id: https://svn.apache.org/repos/asf/incubator/ambari/trunk@1488035 13f79535-47bb-0310-9956-ffa450edef68
--
 CHANGES.txt                                   |   9 +
 .../modules/hdp-oozie/manifests/init.pp       |   4 +-
 .../files/validateYarnComponentStatus.py      |  10 +-
 .../modules/hdp-yarn/manifests/nodemanager.pp |  12 +-
 .../modules/hdp-yarn/manifests/params.pp      |   9 +-
 .../modules/hdp-yarn/manifests/smoketest.pp   |  12 +-
 .../src/main/python/ambari_agent/Heartbeat.py |   2 +
 ambari-server/docs/api/v1/clusters.md         |   1 +
 ambari-server/docs/api/v1/create-cluster.md   |   1 +
 ambari-server/docs/api/v1/create-component.md |   1 +
 ambari-server/docs/api/v1/create-host.md      |   1 +
 .../docs/api/v1/create-hostcomponent.md       |   1 +
 ambari-server/docs/api/v1/create-service.md   |   1 +
 ambari-server/docs/api/v1/delete-cluster.md   |   1 +
 .../docs/api/v1/host-component-resources.md   |   1 +
 ambari-server/docs/api/v1/host-components.md  |   2 +-
 ambari-server/docs/api/v1/index.md            | 702 +++++++++++++++++-
 ambari-server/docs/api/v1/job-resources.md    |  22 +
 ambari-server/docs/api/v1/jobs-job.md         |  60 ++
 ambari-server/docs/api/v1/jobs.md             |  42 ++
 ambari-server/docs/api/v1/services.md         |   2 +-
 .../docs/api/v1/taskattempt-resources.md      |  22 +
 .../docs/api/v1/taskattempts-taskattempt.md   |  48 ++
 ambari-server/docs/api/v1/taskattempts.md     |  70 ++
 .../docs/api/v1/update-hostcomponent.md       |   3 +-
 ambari-server/docs/api/v1/update-service.md   |  57 +-
 ambari-server/docs/api/v1/update-services.md  |   1 +
 .../docs/api/v1/workflow-resources.md         |  22 +
 .../docs/api/v1/workflows-workflow.md         |  57 ++
 ambari-server/docs/api/v1/workflows.md        |  48 ++
 .../server/state/ServiceComponentImpl.java    | 632 ++++++++++------
 .../ambari/server/state/ServiceImpl.java      | 519 ++++++++-----
 .../ambari/server/state/host/HostImpl.java    |  22 +-
 .../svccomphost/ServiceComponentHostImpl.java | 203 +++--
 .../stacks/HDP/1.3.0/repos/repoinfo.xml       |  16 +-
 .../stacks/HDPLocal/1.3.0/repos/repoinfo.xml  |  16 +-
 .../src/test/resources/deploy_HDP2.sh         |   2 +-
 ambari-web/app/config.js                      |   2 +
 .../controllers/global/cluster_controller.js  |   4 +-
 .../controllers/main/admin/misc_controller.js |   3 +
 ambari-web/app/controllers/main/host.js       |  38 +-
 .../app/controllers/main/host/details.js      |   4 +-
 .../controllers/wizard/step7_controller.js    |  15 +-
 ambari-web/app/data/HDP2/config_mapping.js    | 137 +---
 ambari-web/app/data/HDP2/config_properties.js | 277 ++++++-
 ambari-web/app/data/config_properties.js      |  72 +-
 ambari-web/app/messages.js                    |   3 +-
 ambari-web/app/models/host_component.js       |  34 +-
 ambari-web/app/models/service.js              |   2 +-
 ambari-web/app/models/service_config.js       |   4 -
 ambari-web/app/styles/application.less        |  20 +
 ambari-web/app/templates/main/admin/misc.hbs  |  10 +-
 .../app/templates/main/host/summary.hbs       |  81 +-
 .../wizard/step9HostTasksLogPopup.hbs         |   4 +-
 ambari-web/app/utils/config.js                |  20 +-
 ambari-web/app/utils/db.js                    |  84 ++-
 .../views/common/configs/services_config.js   |   2 +-
 ambari-web/app/views/common/filter_view.js    |   5 +
 .../app/views/common/quick_view_link_view.js  |  11 +-
 ambari-web/app/views/common/sort_view.js      |  49 +-
 ambari-web/app/views/common/table_view.js     | 117 ++-
 .../app/views/main/dashboard/service/hbase.js |   2 +-
 .../app/views/main/dashboard/service/hdfs.js  |   2 +-
 .../views/main/dashboard/service/mapreduce.js |   2 +-
 .../app/views/main/dashboard/service/oozie.js |   2 +-
 ambari-web/app/views/main/host.js             |  60 +-
 ambari-web/app/views/main/host/summary.js     | 138 ++--
 .../app/views/main/mirroring/datasets_view.js |  18 +-
 .../app/views/main/mirroring/jobs_view.js     |  15 +-
 .../app/views/main/service/info/summary.js    |   2 +-
 pom.xml                                       |   1 -
 71 files changed, 2946 insertions(+), 928 deletions(-)
 create mode 100644 ambari-server/docs/api/v1/job-resources.md
 create mode 100644 ambari-server/docs/api/v1/jobs-job.md
 create mode 100644 ambari-server/docs/api/v1/jobs.md
 create mode 100644 ambari-server/docs/api/v1/taskattempt-resources.md
 create mode 100644 ambari-server/docs/api/v1/taskattempts-taskattempt.md
 create mode 100644 ambari-server/docs/api/v1/taskattempts.md
 create mode 100644 ambari-server/docs/api/v1/workflow-resources.md
 create mode 100644 ambari-server/docs/api/v1/workflows-workflow.md
 create mode 100644 ambari-server/docs/api/v1/workflows.md

diff --git a/CHANGES.txt b/CHANGES.txt
index bf48c76767..1160bd29cc 100644
-- a/CHANGES.txt
++ b/CHANGES.txt
@@ -312,6 +312,9 @@ Trunk (unreleased changes):
 
  IMPROVEMENTS
 
 AMBARI-2234. Host Details Page: Update host component actions and icons
 depending on state. (yusaku)

  AMBARI-2199. Add a legend to Job Timeline. (billie via yusaku)
 
  AMBARI-2212. Change config loading mechanism to allow for different
@@ -906,6 +909,12 @@ Trunk (unreleased changes):
  Farrellee via mahadev)
 
  BUG FIXES
 
 AMBARI-2230. Nagios user is presented in Admin > Misc page when Nagios
 service was not installed. (yusaku)

 AMBARI-2231. Service web UI links map to hostnames not resolvable by the
 client. (yusaku)
 
  AMBARI-2239. secure cluster: Namenode and SNamenode should use same
  keytab. (jaimin)
diff --git a/ambari-agent/src/main/puppet/modules/hdp-oozie/manifests/init.pp b/ambari-agent/src/main/puppet/modules/hdp-oozie/manifests/init.pp
index f413f535cf..a88dcedc61 100644
-- a/ambari-agent/src/main/puppet/modules/hdp-oozie/manifests/init.pp
++ b/ambari-agent/src/main/puppet/modules/hdp-oozie/manifests/init.pp
@@ -39,13 +39,13 @@ class hdp-oozie(
       configuration => $configuration['oozie-site'],
       owner => $oozie_user,
       group => $hdp::params::user_group,
      mode => '0660'
      mode => '0664'
     }
   } else {
     file { "${oozie_config_dir}/oozie-site.xml":
       owner => $oozie_user,
       group => $hdp::params::user_group,
      mode => '0660'
      mode => '0664'
     }
   }
 
diff --git a/ambari-agent/src/main/puppet/modules/hdp-yarn/files/validateYarnComponentStatus.py b/ambari-agent/src/main/puppet/modules/hdp-yarn/files/validateYarnComponentStatus.py
index 33bdba0bb2..7cc32301bb 100644
-- a/ambari-agent/src/main/puppet/modules/hdp-yarn/files/validateYarnComponentStatus.py
++ b/ambari-agent/src/main/puppet/modules/hdp-yarn/files/validateYarnComponentStatus.py
@@ -27,10 +27,10 @@ HISTORYSERVER ='hs'
 
 STARTED_STATE = 'STARTED'
 
def validate(component, path, port):
def validate(component, path, address):
 
   try:
    url = 'http://localhost:' + str(port) + path
    url = 'http://' + address + path
     opener = urllib2.build_opener()
     urllib2.install_opener(opener)
     request = urllib2.Request(url)
@@ -71,14 +71,14 @@ def validateResponse(component, response):
 #
 def main():
   parser = optparse.OptionParser(usage="usage: %prog [options] component ")
  parser.add_option("-p", "--port", dest="port", help="Port for rest api of desired component")
  parser.add_option("-p", "--port", dest="address", help="Host:Port for REST API of a desired component")
 
 
   (options, args) = parser.parse_args()
 
   component = args[0]
   
  port = options.port
  address = options.address
   
   if component == RESOURCEMANAGER:
     path = '/ws/v1/cluster/info'
@@ -87,7 +87,7 @@ def main():
   else:
     parser.error("Invalid component")
 
  validate(component, path, port)
  validate(component, path, address)
 
 if __name__ == "__main__":
   main()
diff --git a/ambari-agent/src/main/puppet/modules/hdp-yarn/manifests/nodemanager.pp b/ambari-agent/src/main/puppet/modules/hdp-yarn/manifests/nodemanager.pp
index 6e945c7b73..df852ad13f 100644
-- a/ambari-agent/src/main/puppet/modules/hdp-yarn/manifests/nodemanager.pp
++ b/ambari-agent/src/main/puppet/modules/hdp-yarn/manifests/nodemanager.pp
@@ -24,6 +24,7 @@ class hdp-yarn::nodemanager(
 ) inherits hdp-yarn::params
 {
   $yarn_user = $hdp-yarn::params::yarn_user
  $nm_local_dirs = $hdp-yarn::params::nm_local_dirs
   
   if ($service_state == 'no_op') {
   } elsif ($service_state in 'installed_and_configured') {
@@ -36,12 +37,21 @@ class hdp-yarn::nodemanager(
   } elsif ($service_state in ['running','stopped']) {
 
     include hdp-yarn::initialize
 

    hdp::directory_recursive_create { $nm_local_dirs: 
      owner       => $yarn_user,
      context_tag => 'yarn_service',
      service_state => $service_state,
      force => true
    }

     hdp-yarn::service{ 'nodemanager':
       ensure       => $service_state,
       user         => $yarn_user
     }
 
    Hdp::Directory_recursive_create[$nm_local_dirs] -> Hdp-yarn::Service['nodemanager']

   } else {
     hdp_fail("TODO not implemented yet: service_state = ${service_state}")
   }
diff --git a/ambari-agent/src/main/puppet/modules/hdp-yarn/manifests/params.pp b/ambari-agent/src/main/puppet/modules/hdp-yarn/manifests/params.pp
index 3bdaced1d9..c1faf1c9f2 100644
-- a/ambari-agent/src/main/puppet/modules/hdp-yarn/manifests/params.pp
++ b/ambari-agent/src/main/puppet/modules/hdp-yarn/manifests/params.pp
@@ -36,8 +36,9 @@ class hdp-yarn::params(
   $yarn_pid_dir_prefix = hdp_default("hadoop/yarn-env/yarn_pid_dir_prefix","/var/run/hadoop-yarn")
   
   ## yarn-site
  $rm_webui_port = hdp_default("yarn-site/yarn.resourcemanager.webapp.address", "8088")
  $nm_webui_port = hdp_default("yarn-site/yarn.nodemanager.webapp.address", "8042")
  $hs_webui_port = hdp_default("yarn-site/mapreduce.jobhistory.address", "19888")

  $rm_webui_address = hdp_default("yarn-site/yarn.resourcemanager.webapp.address", "localhost:8088")
  $nm_webui_address = hdp_default("yarn-site/yarn.nodemanager.webapp.address", "localhost:8042")
  $hs_webui_address = hdp_default("mapred-site/mapreduce.jobhistory.webapp.address", "localhost:19888")
  
  $nm_local_dirs = hdp_default("yarn-site/yarn.nodemanager.local-dirs", "/yarn/loc/dir") 
 }
diff --git a/ambari-agent/src/main/puppet/modules/hdp-yarn/manifests/smoketest.pp b/ambari-agent/src/main/puppet/modules/hdp-yarn/manifests/smoketest.pp
index 960688012f..383138f243 100644
-- a/ambari-agent/src/main/puppet/modules/hdp-yarn/manifests/smoketest.pp
++ b/ambari-agent/src/main/puppet/modules/hdp-yarn/manifests/smoketest.pp
@@ -22,16 +22,16 @@ class hdp-yarn::smoketest(
   $component_name = undef
 )
 {
  $rm_webui_port = $hdp-yarn::params::rm_webui_port
  $nm_webui_port = $hdp-yarn::params::nm_webui_port
  $hs_webui_port = $hdp-yarn::params::hs_webui_port
  $rm_webui_address = $hdp-yarn::params::rm_webui_address
  $nm_webui_address = $hdp-yarn::params::nm_webui_address
  $hs_webui_address = $hdp-yarn::params::hs_webui_address
 
   if ($component_name == 'resourcemanager') {
     $component_type = 'rm'
    $component_port = $rm_webui_port
    $component_address = $rm_webui_address
   } elsif ($component_name == 'historyserver') {
     $component_type = 'hs' 
    $component_port = $hs_webui_port
    $component_address = $hs_webui_address
   } else {
     hdp_fail("Unsupported component name: $component_name")
   }
@@ -41,7 +41,7 @@ class hdp-yarn::smoketest(
   $validateStatusFileName = "validateYarnComponentStatus.py"
   $validateStatusFilePath = "/tmp/$validateStatusFileName"
 
  $validateStatusCmd = "su - ${smoke_test_user} -c 'python $validateStatusFilePath $component_type -p $component_port'"
  $validateStatusCmd = "su - ${smoke_test_user} -c 'python $validateStatusFilePath $component_type -p $component_address'"
 
   file { $validateStatusFilePath:
     ensure => present,
diff --git a/ambari-agent/src/main/python/ambari_agent/Heartbeat.py b/ambari-agent/src/main/python/ambari_agent/Heartbeat.py
index 79390ca217..7c0d9b5f32 100644
-- a/ambari-agent/src/main/python/ambari_agent/Heartbeat.py
++ b/ambari-agent/src/main/python/ambari_agent/Heartbeat.py
@@ -57,6 +57,8 @@ class Heartbeat:
       heartbeat['reports'] = queueResult['reports']
       heartbeat['componentStatus'] = queueResult['componentStatus']
       pass
    logger.info("Sending heartbeat with response id: " + str(id) + " and "
                "timestamp: " + str(timestamp))
     logger.debug("Heartbeat : " + pformat(heartbeat))
 
     if (int(id) >= 0) and state_interval > 0 and (int(id) % state_interval) == 0:
diff --git a/ambari-server/docs/api/v1/clusters.md b/ambari-server/docs/api/v1/clusters.md
index 6b3a5c1fd9..df895f5761 100644
-- a/ambari-server/docs/api/v1/clusters.md
++ b/ambari-server/docs/api/v1/clusters.md
@@ -1,3 +1,4 @@

 <!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements. See the NOTICE file distributed with
diff --git a/ambari-server/docs/api/v1/create-cluster.md b/ambari-server/docs/api/v1/create-cluster.md
index b228b85781..07197b8ea9 100644
-- a/ambari-server/docs/api/v1/create-cluster.md
++ b/ambari-server/docs/api/v1/create-cluster.md
@@ -1,3 +1,4 @@

 <!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements. See the NOTICE file distributed with
diff --git a/ambari-server/docs/api/v1/create-component.md b/ambari-server/docs/api/v1/create-component.md
index 26a1ab36db..f67705b409 100644
-- a/ambari-server/docs/api/v1/create-component.md
++ b/ambari-server/docs/api/v1/create-component.md
@@ -1,3 +1,4 @@

 <!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements. See the NOTICE file distributed with
diff --git a/ambari-server/docs/api/v1/create-host.md b/ambari-server/docs/api/v1/create-host.md
index c6629a08c5..a686f786a8 100644
-- a/ambari-server/docs/api/v1/create-host.md
++ b/ambari-server/docs/api/v1/create-host.md
@@ -1,3 +1,4 @@

 <!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements. See the NOTICE file distributed with
diff --git a/ambari-server/docs/api/v1/create-hostcomponent.md b/ambari-server/docs/api/v1/create-hostcomponent.md
index 2dee137261..f7f6ec43a0 100644
-- a/ambari-server/docs/api/v1/create-hostcomponent.md
++ b/ambari-server/docs/api/v1/create-hostcomponent.md
@@ -1,3 +1,4 @@

 <!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements. See the NOTICE file distributed with
diff --git a/ambari-server/docs/api/v1/create-service.md b/ambari-server/docs/api/v1/create-service.md
index dc681d2118..9037c904f1 100644
-- a/ambari-server/docs/api/v1/create-service.md
++ b/ambari-server/docs/api/v1/create-service.md
@@ -1,3 +1,4 @@

 <!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements. See the NOTICE file distributed with
diff --git a/ambari-server/docs/api/v1/delete-cluster.md b/ambari-server/docs/api/v1/delete-cluster.md
index 66b1b32473..138cf0d375 100644
-- a/ambari-server/docs/api/v1/delete-cluster.md
++ b/ambari-server/docs/api/v1/delete-cluster.md
@@ -1,3 +1,4 @@

 <!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements. See the NOTICE file distributed with
diff --git a/ambari-server/docs/api/v1/host-component-resources.md b/ambari-server/docs/api/v1/host-component-resources.md
index eaeb84a214..c7cf7a2e34 100644
-- a/ambari-server/docs/api/v1/host-component-resources.md
++ b/ambari-server/docs/api/v1/host-component-resources.md
@@ -158,3 +158,4 @@ A component can be stopped through the API by setting its state to be INSTALLED
 The user can update the desired state of a host component through the API to be MAINTENANCE (see [update host component](update-hostcomponent.md)).  When a host component is into maintenance state it is basically taken off line. This state can be used, for example, to move a component like NameNode.  The NameNode component can be put in MAINTENANCE mode and then a new NameNode can be created for the service. 
 
 

diff --git a/ambari-server/docs/api/v1/host-components.md b/ambari-server/docs/api/v1/host-components.md
index a00adaeceb..a2f4f700c4 100644
-- a/ambari-server/docs/api/v1/host-components.md
++ b/ambari-server/docs/api/v1/host-components.md
@@ -94,4 +94,4 @@ Returns a collection of components running on a the host named "h1" on the clust
     		},
     		...
 		]
	}
	}
\ No newline at end of file
diff --git a/ambari-server/docs/api/v1/index.md b/ambari-server/docs/api/v1/index.md
index 2f190fda5c..edaa322f90 100644
-- a/ambari-server/docs/api/v1/index.md
++ b/ambari-server/docs/api/v1/index.md
@@ -1 +1,701 @@
<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
->

Ambari API Reference v1
=========

The Ambari API facilitates the management and monitoring of the resources of an Apache Hadoop cluster. This document describes the resources and syntax used in the Ambari API and is intended for developers who want to integrate with Ambari.

 [Release Version](#release-version)
 [Authentication](#authentication)
 [Monitoring](#monitoring)
 [Management](#management)
 [Resources](#resources)
 [Partial Response](#partial-response)
 [Query Parameters](#query-parameters)
 [Batch Requests](#batch-requests)
 [RequestInfo](#request-info)
 [Errors](#errors)


Release Version
---
_Last Updated April 25, 2013_

Authentication
---

The operations you perform against the Ambari API require authentication. Access to the API requires the use of **Basic Authentication**. To use Basic Authentication, you need to send the **Authorization: Basic** header with your requests. For example, this can be handled when using curl and the --user option.

    curl --user name:password http://{your.ambari.server}/api/v1/clusters

_Note: The authentication method and source is configured at the Ambari Server. Changing and configuring the authentication method and source is not covered in this document._

Monitoring
---
The Ambari API provides access to monitoring and metrics information of an Apache Hadoop cluster.

###GET
Use the GET method to read the properties, metrics and sub-resources of an Ambari resource.  Calling the GET method returns the requested resources and produces no side-effects.  A response code of 200 indicates that the request was successfully processed with the requested resource included in the response body.
 
**Example**

Get the DATANODE component resource for the HDFS service of the cluster named 'c1'.

    GET /clusters/c1/services/HDFS/components/DATANODE

**Response**

    200 OK
    {
    	"href" : "http://your.ambari.server/api/v1/clusters/c1/services/HDFS/components/DATANODE",
    	"metrics" : {
    		"process" : {
              "proc_total" : 697.75,
              "proc_run" : 0.875
    		},
      		"rpc" : {
        		...
      		},
      		"ugi" : {
      			...
      		},
      		"dfs" : {
        		"datanode" : {
          		...
        		}
      		},
      		"disk" : {
        		...
      		},
      		"cpu" : {
        		...
      		}
      		...
        },
    	"ServiceComponentInfo" : {
      		"cluster_name" : "c1",
      		"component_name" : "DATANODE",
      		"service_name" : "HDFS"
      		"state" : "STARTED"
    	},
    	"host_components" : [
      		{
      			"href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/host1/host_components/DATANODE",
      			"HostRoles" : {
        			"cluster_name" : "c1",
        			"component_name" : "DATANODE",
        			"host_name" : "host1"
        		}
      		}
       	]
    }


Management
---
The Ambari API provides for the management of the resources of an Apache Hadoop cluster.  This includes the creation, deletion and updating of resources.

###POST
The POST method creates a new resource. If a new resource is created then a 201 response code is returned.  The code 202 can also be returned to indicate that the instruction was accepted by the server (see [asynchronous response](#asynchronous-response)). 

**Example**

Create the HDFS service.


    POST /clusters/c1/services/HDFS


**Response**

    201 Created

###PUT
Use the PUT method to update resources.  If an existing resource is modified then a 200 response code is retrurned to indicate successful completion of the request.  The response code 202 can also be returned to indicate that the instruction was accepted by the server (see [asynchronous response](#asynchronous-response)).

**Example**

Start the HDFS service (update the state of the HDFS service to be ‘STARTED’).


    PUT /clusters/c1/services/HDFS/

**Body**

    {
      "ServiceInfo": {
        "state" : "STARTED”
      }
    }


**Response**

The response code 202 indicates that the server has accepted the instruction to update the resource.  The body of the response contains the ID and href of the request resource that was created to carry out the instruction (see [asynchronous response](#asynchronous-response)).

    202 Accepted
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/3",
      "Requests" : {
        "id" : 3,
        "status" : "InProgress"
      } 
    }


###DELETE
Use the DELETE method to delete a resource. If an existing resource is deleted then a 200 response code is retrurned to indicate successful completion of the request.  The response code 202 can also be returned which indicates that the instruction was accepted by the server and the resource was marked for deletion (see [asynchronous response](#asynchronous-response)).

**Example**

Delete the cluster named 'c1'.

    DELETE /clusters/c1

**Response**

    200 OK

###Asynchronous Response

The managment APIs can return a response code of 202 which indicates that the request has been accepted.  The body of the response contains the ID and href of the request resource that was created to carry out the instruction. 
    
    202 Accepted
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/6",
      "Requests" : {
        "id" : 6,
        "status" : "InProgress"
      } 
    }

The href in the response body can then be used to query the associated request resource and monitor the progress of the request.  A request resource has one or more task sub resources.  The following example shows how to use [partial response](#partial-response) to query for task resources of a request resource. 

    /clusters/c1/requests/6?fields=tasks/Tasks/*   
    
The returned task resources can be used to determine the status of the request.

    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/6",
      "Requests" : {
        "id" : 6,
        "cluster_name" : "c1"
      },
      "tasks" : [
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/6/tasks/32",
          "Tasks" : {
            "exit_code" : 777,
            "stdout" : "default org.apache.hadoop.mapred.CapacityTaskScheduler\nwarning: Dynamic lookup of ...",
            "status" : "IN_PROGRESS",
            "stderr" : "",
            "host_name" : "dev.hortonworks.com",
            "id" : 32,
            "cluster_name" : "c1",
            "attempt_cnt" : 1,
            "request_id" : 6,
            "command" : "START",
            "role" : "NAMENODE",
            "start_time" : 1367240498196,
            "stage_id" : 1
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/6/tasks/33",
          "Tasks" : {
            "exit_code" : 999,
            "stdout" : "",
            "status" : "PENDING",
            ...
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/6/tasks/31",
          "Tasks" : {
            "exit_code" : 0,
            "stdout" : "warning: Dynamic lookup of $ambari_db_rca_username ...",
            "status" : "COMPLETED",
            ...
          }
        }
      ]
    }

Resources
---
###Collection Resources


A collection resource is a set of resources of the same type, rather than any specific resource. For example:

    /clusters  

  _Refers to a collection of clusters_

###Instance Resources

An instance resource is a single specific resource. For example:

    /clusters/c1

  _Refers to the cluster resource identified by the id "c1"_

###Types
Resources are grouped into types.  This allows the user to query for collections of resources of the same type.  Some resource types are composed of subtypes (e.g. services are sub-resources of clusters).

The following is a list of some of the Ambari resource types with descriptions and usage examples.
 
#### clusters
Cluster resources represent named Hadoop clusters.  Clusters are top level resources. 

[Cluster Resources](cluster-resources.md)

#### services
Service resources are services of a Hadoop cluster (e.g. HDFS, MapReduce and Ganglia).  Service resources are sub-resources of clusters. 

[Service Resources](service-resources.md)

#### components
Component resources are the individual components of a service (e.g. HDFS/NameNode and MapReduce/JobTracker).  Components are sub-resources of services.

[Component Resources](component-resources.md)

#### hosts
Host resources are the host machines that make up a Hadoop cluster.  Hosts are top level resources but can also be sub-resources of clusters. 

[Host Resources](host-resources.md)


#### host_components
Host component resources are usages of a component on a particular host.  Host components are sub-resources of hosts.

[Host Component Resources](host-component-resources.md)


#### configurations
Configuration resources are sets of key/value pairs that configure the services of a Hadoop cluster.

[Configuration Resource Overview](configuration.md)


#### workflows
Workflow resources are DAGs of MapReduce jobs in a Hadoop cluster.

[Workflow Resources](workflow-resources.md)

#### jobs
Job resources represent individual nodes (MapReduce jobs) in a workflow.

[Job Resources](job-resources.md)

#### taskattempts
Task attempt resources are individual attempts at map or reduce tasks for a job.

[Task Attempt Resources](taskattempt-resources.md)


Partial Response
---

Used to control which fields are returned by a query.  Partial response can be used to restrict which fields are returned and additionally, it allows a query to reach down and return data from sub-resources.  The keyword “fields” is used to specify a partial response.  Only the fields specified will be returned to the client.  To specify sub-elements, use the notation “a/b/c”.  Properties, categories and sub-resources can be specified.  The wildcard ‘*’ can be used to show all categories, fields and sub-resources for a resource.  This can be combined to provide ‘expand’ functionality for sub-components.  Some fields are always returned for a resource regardless of the specified partial response fields.  These fields are the fields, which uniquely identify the resource.  This would be the primary id field of the resource and the foreign keys to the primary id fields of all ancestors of the resource.

**Example: Using Partial Response to restrict response to a specific field**

    GET    /api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk/disk_total

    200 OK
	{
    	“href”: “.../api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk/disk_total”,
    	“ServiceComponentInfo” : {
        	“cluster_name” : “c1”,
        	“component_name” : NAMENODE”,
        	“service_name” : “HDFS”
    	},
    	“metrics” : {
        	"disk" : {       
            	"disk_total" : 100000
        	}
    	}
    }

**Example: Using Partial Response to restrict response to specified category**

    GET    /api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk

    200 OK
	{
    	“href”: “.../api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk”,
    	“ServiceComponentInfo” : {
        	“cluster_name” : “c1”,
        	“component_name” : NAMENODE”,
        	“service_name” : “HDFS”
    	},
    	“metrics” : {
        	"disk" : {       
            	"disk_total" : 100000,
            	“disk_free” : 50000,
            	“part_max_used” : 1010
        	}
    	}
	}

**Example – Using Partial Response to restrict response to multiple fields/categories**

	GET	/api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk/disk_total,metrics/cpu
	
	200 OK
	{
    	“href”: “.../api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk/disk_total,metrics/cpu”,
    	“ServiceComponentInfo” : {
        	“cluster_name” : “c1”,
        	“component_name” : NAMENODE”,
        	“service_name” : “HDFS”
    	},
    	“metrics” : {
        	"disk" : {       
            	"disk_total" : 100000
        	},
        	“cpu” : {
            	“cpu_speed” : 10000000,
            	“cpu_num” : 4,
            	“cpu_idle” : 999999,
            	...
        	}
    	}
	}

**Example – Using Partial Response to restrict response to a sub-resource**

	GET	/api/v1/clusters/c1/hosts/host1?fields=host_components

	200 OK
	{
    	“href”: “.../api/v1/clusters/c1/hosts/host1?fields=host_components”,
    	“Hosts” : {
        	“cluster_name” : “c1”,
        	“host_name” : “host1”
    	},
    	“host_components”: [
        	{
            	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/NAMENODE”
            	“HostRoles” : {
                	“cluster_name” : “c1”,
                	“component_name” : “NAMENODE”,
                	“host_name” : “host1”
            	}
        	},
        	{
            	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/DATANODE”
            	“HostRoles” : {
                	“cluster_name” : “c1”,
                	“component_name” : DATANODE”,
                	“host_name” : “host1”
            	}
        	},
            ... 
    	]
	}

**Example – Using Partial Response to expand a sub-resource one level deep**

	GET	/api/v1/clusters/c1/hosts/host1?fields=host_components/*

	200 OK
	{
    	“href”: “.../api/v1/clusters/c1/hosts/host1?fields=host_components/*”,
    	“Hosts” : {
        	“cluster_name” : “c1”,
        	“host_name” : “host1”
        },
        “host_components”: [
        	{
            	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/DATANODE”
            	“HostRoles” : {
                	“cluster_name” : “c1”,
               		“component_name” : DATANODE”,
                	“host_name” : “host1”,
                	“state” : “RUNNING”,
                	...
            	},        
            	"host" : {     
                	"href" : ".../api/v1/clusters/c1/hosts/host1"  
            	},
            	“metrics” : {
                	"disk" : {       
                    	"disk_total" : 100000000,       
                    	"disk_free" : 5000000,       
                    	"part_max_used" : 10101     
                	},
                	...
            	},
            	"component" : {
                	"href" : "http://ambari.server/api/v1/clusters/c1/services/HDFS/components/NAMENODE", 
                	“ServiceComponentInfo” : {
                    	"cluster_name" : "c1",         
                    	"component_name" : "NAMENODE",         
                    	"service_name" : "HDFS"       
                	}
            	}  
        	},
        	...
    	]
	}

**Example – Using Partial Response for multi-level expansion of sub-resources**
	
	GET /api/v1/clusters/c1/hosts/host1?fields=host_components/component/*
	
	200 OK
	{
    	“href”: “http://ambari.server/api/v1/clusters/c1/hosts/host1?fields=host_components/*”,
    	“Hosts” : {
        	“cluster_name” : “c1”,
        	“host_name” : “host1”
        	...
    	},
    	“host_components”: [
    		{
            	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/DATANODE”,
            	“HostRoles” : {
                	“cluster_name” : “c1”,
                	“component_name” : DATANODE”,
                	“host_name” : “host1”
            	}, 
            	"component" : {
                	"href" : "http://ambari.server/api/v1/clusters/c1/services/HDFS/components/DATANODE", 
                	“ServiceComponentInfo” : {
                   		"cluster_name" : "c1",         
                    	"component_name" : "DATANODE",         
                    	"service_name" : "HDFS"  
                    	...     
                	},
             		“metrics”: {
                   		“dfs”: {
                       		“datanode” : {
          	                	“blocks_written " :  10000,
          	                	“blocks_read" : 5000,
                             	...
                        	}
                    	},
                    	“disk”: {
                       		"disk_total " :  1000000,
                        	“disk_free" : 50000,
                        	...
                    	},
                   		... 	
					}
            	}
        	},
        	{
            	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/NAMENODE”,
            	“HostRoles” : {
                	“cluster_name” : “c1”,
                	“component_name” : NAMENODE”,
                	“host_name” : “host1”
            	}, 
            	"component" : {
                	"href" : "http://ambari.server/api/v1/clusters/c1/services/HDFS/components/NAMENODE", 
                	“ServiceComponentInfo” : {
                   		"cluster_name" : "c1",         
                    	"component_name" : "NAMENODE",         
                    	"service_name" : "HDFS"       
                	},
             		“metrics”: {
                    	“dfs”: {
                       		“namenode” : {
          	            		“FilesRenamed " :  10,
          	            		“FilesDeleted" : 5
                         		…
                    		}
						},	
                    	“disk”: {
                       		"disk_total " :  1000000,
                       		“disk_free" : 50000,
                        	...
                    	}
                	},
                	...
            	}
        	},
        	...
    	]
	}

**Example: Using Partial Response to expand collection resource instances one level deep**

	GET /api/v1/clusters/c1/hosts?fields=*

	200 OK
	{
    	“href” : “http://ambari.server/api/v1/clusters/c1/hosts/?fields=*”,    
    	“items”: [ 
        	{
            	“href” : “http://ambari.server/api/v1/clusters/c1/hosts/host1”,
            	“Hosts” : {
                	“cluster_name” :  “c1”,
                	“host_name” : “host1”
            	},
            	“metrics”: {
                	“process”: {          	    
                   		"proc_total" : 1000,
          	       		"proc_run" : 1000
                	},
                	...
            	},
            	“host_components”: [
                	{
                   		“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/NAMENODE”
                    	“HostRoles” : {
                       		“cluster_name” : “c1”,
                         	“component_name” : “NAMENODE”,
                        	“host_name” : “host1”
                    	}
                	},
                	{
                    	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/DATANODE”
                    	“HostRoles” : {
                       		“cluster_name” : “c1”,
                        	“component_name” : DATANODE”,
                        	“host_name” : “host1”
                    	}
                	},
                	...
            	},
            	...
        	},
        	{
            	“href” : “http://ambari.server/api/v1/clusters/c1/hosts/host2”,
            	“Hosts” : {
                	“cluster_name” :  “c1”,
                	“host_name” : “host2”
            	},
            	“metrics”: {
               		“process”: {          	    
                   		"proc_total" : 555,
          	     		"proc_run" : 55
                	},
                	...
            	},
            	“host_components”: [
                	{
                   		“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/DATANODE”
                    	“HostRoles” : {
                       		“cluster_name” : “c1”,
                        	“component_name” : “DATANODE”,
                        	“host_name” : “host2”
                    	}
                	},
                	...
            	],
            	...
        	},
        	...
    	]
	}

### Additional Partial Response Examples

**Example – For each cluster, get cluster name, all hostname’s and all service names**

	GET   /api/v1/clusters?fields=Clusters/cluster_name,hosts/Hosts/host_name,services/ServiceInfo/service_name

**Example - Get all hostname’s for a given component**

	GET	/api/v1/clusters/c1/services/HDFS/components/DATANODE?fields=host_components/HostRoles/host_name

**Example - Get all hostname’s and component names for a given service**

	GET	/api/v1/clusters/c1/services/HDFS?fields=components/host_components/HostRoles/host_name,
                                      	          components/host_components/HostRoles/component_name



Query Predicates
---

Used to limit which data is returned by a query.  This is synonymous to the “where” clause in a SQL query.  Providing query parameters does not result in any link expansion in the data that is returned, with the exception of the fields used in the predicates.  Query predicates can only be applied to collection resources.  A predicate consists of at least one relational expression.  Predicates with multiple relational expressions also contain logical operators, which connect the relational expressions.  Predicates may also use brackets for explicit grouping of expressions. 

###Relational Query Operators

<table>
  <tr>
    <th>Operator</th>
    <th>Example</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>=</td>
    <td>name=host1</td>
    <td>String or numerical EQUALS</td>
  </tr>
  <tr>
    <td>!=</td>
    <td>name!=host1</td>
    <td>String or numerical NOT EQUALS</td>
  </tr>
  <tr>
    <td>&lt;</td>
    <td>disk_total&lt;50</td>
    <td>Numerical LESS THAN</td>
  </tr>
  <tr>
    <td>&gt;</td>
    <td>disk_total&gt;50</td>
    <td>Numerical GREATER THAN</td>
  </tr>
  <tr>
    <td>&lt;=</td>
    <td>disk_total&lt;=50</td>
    <td>Numerical LESS THAN OR EQUALS</td>
  </tr>
  <tr>
    <td>&gt;=</td>
    <td>disk_total&gt;=50</td>
    <td>Numerical GREATER THAN OR EQUALS</td>
  </tr>  
</table>

###Logical Query Operators

<table>
  <tr>
    <th>Operator</th>
    <th>Example</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>|</td>
    <td>name=host1|name=host2</td>
    <td>Logical OR operator</td>
  </tr>
  <tr>
    <td>&</td>
    <td>prop1=foo&prop2=bar</td>
    <td>Logical AND operator</td>
  </tr>
  <tr>
    <td>!</td>
    <td>!prop<50</td>
    <td>Logical NOT operator</td>
  </tr>
</table>

**Logical Operator Precedence**

Standard logical operator precedence rules apply.  The above logical operators are listed in order of precedence starting with the lowest priority.  

###Brackets

<table>
  <tr>
    <th>Bracket</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>(</td>
    <td>Opening Bracket</td>
  </tr>
  <tr>
    <td>)</td>
    <td>Closing Bracket</td>
  </tr>

</table>
  
Brackets can be used to provide explicit grouping of expressions. Expressions within brackets have the highest precedence.

###Operator Functions
 
<table>
  <tr>
    <th>Operator</th>
    <th>Example</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>in()</td>
    <td>name.in(foo,bar)</td>
    <td>IN function.  More compact form of name=foo|name=bar. </td>
  </tr>
  <tr>
    <td>isEmpty()</td>
    <td>category.isEmpty()</td>
    <td>Used to determine if a category contains any properties. </td>
  </tr>
</table>
Operator functions behave like relational operators and provide additional functionality.  Some operator functions, such as in(), act as binary operators like the above relational operators, where there is a left and right operand.  Some operator functions are unary operators, such as isEmpty(), where there is only a single operand.

###Query Examples

**Example – Get all hosts with “HEALTHY” status that have 2 or more cpu**
	
	GET	/api/v1/clusters/c1/hosts?Hosts/host_status=HEALTHY&Hosts/cpu_count>=2
	
**Example – Get all hosts with less than 2 cpu or host status != HEALTHY**
	

	GET	/api/v1/clusters/c1/hosts?Hosts/cpu_count<2|Hosts/host_status!=HEALTHY

**Example – Get all “rhel6” hosts with less than 2 cpu or “centos6” hosts with 3 or more cpu**  

	GET	/api/v1/clusters/c1/hosts?Hosts/os_type=rhel6&Hosts/cpu_count<2|Hosts/os_type=centos6&Hosts/cpu_count>=3

**Example – Get all hosts where either state != “HEALTHY” or last_heartbeat_time < 1360600135905 and rack_info=”default_rack”**

	GET	/api/v1/clusters/c1/hosts?(Hosts/host_status!=HEALTHY|Hosts/last_heartbeat_time<1360600135905)
                                  &Hosts/rack_info=default_rack

**Example – Get hosts with host name of host1 or host2 or host3 using IN operator**
	
	GET	/api/v1/clusters/c1/hosts?Hosts/host_name.in(host1,host2,host3)

**Example – Get and expand all HDFS components, which have at least 1 property in the “metrics/jvm” category (combines query and partial response syntax)**

	GET	/api/v1/clusters/c1/services/HDFS/components?!metrics/jvm.isEmpty()&fields=*

**Example – Update the state of all ‘INSTALLED’ services to be ‘STARTED’**

	PUT /api/v1/clusters/c1/services?ServiceInfo/state=INSTALLED 
    {
      "ServiceInfo": {
        "state" : "STARTED”
      }
    }

Batch Requests
---
Requests can be batched.  This allows for multiple bodies to be specified as an array in a single request. 

**Example – Creating multiple hosts in a single request**
     
    POST /api/v1/clusters/c1/hosts/         

    [
      {
        "Hosts" : {
          "cluster_name" : "c1",
          "host_name" : "host1"
        }
      },
      {
        "Hosts" : {
          "cluster_name" : "c1",
          "host_name" : "host2"
        }
      },
      {
        "Hosts" : {
          "cluster_name" : "c1",
          "host_name" : "host3"
        }
      }
    ]


RequestInfo
---
RequestInfo allows the user to specify additional properties in the body of a request.

<table>
  <tr>
    <th>Key</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>query</td>
    <td>The query string for the request.  Useful for overcoming length limits of the URL and for specifying a query string for each element of a batch request.</td>  
  </tr>
  <tr>
    <td>context</td>
    <td>The context string.  The API client can pass a string context to able to specify what the requests were for (e.g., "HDFS Service Start", "Add Hosts", etc.)</td>  
  </tr>
</table>

###query


The query property allows the user to specify yhe query string as part of the request body.  This is sometimes required in the case of a very long query string that causes the request to exceed the limits of the URL.

**Example – Specifying the query string in the request body**

    PUT  /clusters/c1/services
    
    {
      "RequestInfo":{
        "query":"ServiceInfo/state=STARTED&ServiceInfo/service_name=HDFS&…"
      },

      "Body":
      {
        "ServiceInfo": {
          "state" : "INSTALLED"
        }
      }
    }
    
The query property can also be applied to the elements of a [batch request](#batch-request).

**Example – Specifying the query string in the request body for a batch request**


    PUT /api/v1/clusters/c1/hosts
    
    [
      {
        "RequestInfo":{
          "query":"Hosts/host_name=host1"
        },
        "Body":
        {
          "Hosts": {
            "desired_config": {
              "type": "global",
              "tag": "version50",
              "properties": { "a": "b", "x": "y" }
            }
          }
        }
      },
      {
        "RequestInfo":{
          "query":"Hosts/host_name=host2"
        },
        "Body":
        {
          "Hosts": {
            "desired_config": {
              "type": "global",
              "tag": "version51",
              "properties": { "a": "c", "x": "z" }
            }
          }
        }
      }
    ]


###context
In some cases a request will return a 202 to indicate that the instruction was accepted by the server (see [asynchronous response](#asynchronous-response)).  In these cases the body of the response contains the ID and href of the request resource that was created to carry out the instruction.  It may be desirable to attach a context string to the request which will then be assigned to the resulting request response.

In the following example a request is made to stop the HDFS service.  Notice that a context is passed as a RequestInfo property.

**Example – Specifying the query string in the request body**

    PUT  /clusters/c1/services
    
    {
      "RequestInfo":{
        "query":"ServiceInfo/state=STARTED&ServiceInfo/service_name=HDFS",
        "context":"Stop HDFS service."
      },

      "Body":
      {
        "ServiceInfo": {
          "state" : "INSTALLED"
        }
      }
    }

**Response**

    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/13",
      "Requests" : {
        "id" : 13,
        "status" : "InProgress"
      }
    }
    
When the request resource returned in the above example is queried, the supplied context string is returned as part of the response.

    GET api/v1/clusters/c1/requests/13 

**Response**


    {
      "href" : "http://ec2-50-19-183-89.compute-1.amazonaws.com:8080/api/v1/clusters/c1/requests/13",
      "Requests" : {
        "id" : 13,
        "cluster_name" : "c1",
        "request_context" : "Stop HDFS service."
      },
      "tasks" : [
        …
      ]
    }
 

Temporal Metrics
---

Some metrics have values that are available across a range in time.  To query a metric for a range of values, the following partial response syntax is used.  

To get temporal data for a single property:
?fields=category/property[start-time,end-time,step]	

To get temporal data for all properties in a category:
?fields=category[start-time,end-time,step]

start-time: Required field.  The start time for the query in Unix epoch time format.
end-time: Optional field, defaults to now.  The end time for the query in Unix epoch time format.
step: Optional field, defaults to the corresponding metrics system’s default value.  If provided, end-time must also be provided. The interval of time between returned data points specified in seconds. The larger the value provided, the fewer data points returned so this can be used to limit how much data is returned for the given time range.  This is only used as a suggestion so the result interval may differ from the one specified.

The returned result is a list of data points over the specified time range.  Each data point is a value / timestamp pair.

**Note**: It is important to understand that requesting large amounts of temporal data may result in severe performance degradation.  **Always** request the minimal amount of information necessary.  If large amounts of data are required, consider splitting the request up into multiple smaller requests.

**Example – Temporal Query for a single property using only start-time**

	GET	/api/v1/clusters/c1/hosts/host1?fields=metrics/jvm/gcCount[1360610225]

	
	200 OK
	{
    	“href” : …/api/v1/clusters/c1/hosts/host1?fields=metrics/jvm/gcCount[1360610225]”,
    	...
    	“metrics”: [
        	{
            	“jvm”: {
          	    	"gcCount" : [
                   		[10, 1360610165],
                     	[12, 1360610180],
                     	[13, 1360610195],
                     	[14, 1360610210],
                     	[15, 1360610225]
                  	]
             	}
         	}
    	]
	}

**Example – Temporal Query for a category using start-time, end-time and step**

	GET	/api/v1/clusters/c1/hosts/host1?fields=metrics/jvm[1360610200,1360610500,100]

	200 OK
	{
    	“href” : …/clusters/c1/hosts/host1?fields=metrics/jvm[1360610200,1360610500,100]”,
    	...
    	“metrics”: [
        	{
            	“jvm”: {
          	    	"gcCount" : [
                   		[10, 1360610200],
                     	[12, 1360610300],
                     	[13, 1360610400],
                     	[14, 1360610500]
                  	],
                	"gcTimeMillis" : [
                   		[1000, 1360610200],
                     	[2000, 1360610300],
                     	[5000, 1360610400],
                     	[9500, 1360610500]
                  	],
                  	...
             	}
         	}
    	]
	}

	


HTTP Return Codes
---

The following HTTP codes may be returned by the API.
<table>
  <tr>
    <th>HTTP CODE</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>200</td>
    <td>OK</td>  
  </tr>
  <tr>
    <td>400</td>
    <td>Bad Request</td>  
  </tr>
  <tr>
    <td>401</td>
    <td>Unauthorized</td>  
  </tr>
  <tr>
    <td>403</td>
    <td>Forbidden</td>  
  </tr> 
  <tr>
    <td>404</td>
    <td>Not Found</td>  
  </tr>
  <tr>
    <td>500</td>
    <td>Internal Server Error</td>  
  </tr>
</table>


Errors
---

**Example errors responses**

    404 Not Found
	{   
    	"status" : 404,   
    	"message" : "The requested resource doesn't exist: Cluster not found, clusterName=someInvalidCluster" 
	} 

&nbsp;

	400 Bad Request
	{   
    	"status" : 400,   
    	"message" : "The properties [foo] specified in the request or predicate are not supported for the 
                	 resource type Cluster."
	}


\ No newline at end of file
<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

Ambari API Reference v1
=========

The Ambari API facilitates the management and monitoring of the resources of an Apache Hadoop cluster. This document describes the resources and syntax used in the Ambari API and is intended for developers who want to integrate with Ambari.

- [Release Version](#release-version)
- [Authentication](#authentication)
- [Monitoring](#monitoring)
- [Management](#management)
- [Resources](#resources)
- [Partial Response](#partial-response)
- [Query Parameters](#query-parameters)
- [Batch Requests](#batch-requests)
- [RequestInfo](#request-info)
- [Errors](#errors)


Release Version
----
_Last Updated April 25, 2013_

Authentication
----

The operations you perform against the Ambari API require authentication. Access to the API requires the use of **Basic Authentication**. To use Basic Authentication, you need to send the **Authorization: Basic** header with your requests. For example, this can be handled when using curl and the --user option.

    curl --user name:password http://{your.ambari.server}/api/v1/clusters

_Note: The authentication method and source is configured at the Ambari Server. Changing and configuring the authentication method and source is not covered in this document._

Monitoring
----
The Ambari API provides access to monitoring and metrics information of an Apache Hadoop cluster.

###GET
Use the GET method to read the properties, metrics and sub-resources of an Ambari resource.  Calling the GET method returns the requested resources and produces no side-effects.  A response code of 200 indicates that the request was successfully processed with the requested resource included in the response body.
 
**Example**

Get the DATANODE component resource for the HDFS service of the cluster named 'c1'.

    GET /clusters/c1/services/HDFS/components/DATANODE

**Response**

    200 OK
    {
    	"href" : "http://your.ambari.server/api/v1/clusters/c1/services/HDFS/components/DATANODE",
    	"metrics" : {
    		"process" : {
              "proc_total" : 697.75,
              "proc_run" : 0.875
    		},
      		"rpc" : {
        		...
      		},
      		"ugi" : {
      			...
      		},
      		"dfs" : {
        		"datanode" : {
          		...
        		}
      		},
      		"disk" : {
        		...
      		},
      		"cpu" : {
        		...
      		}
      		...
        },
    	"ServiceComponentInfo" : {
      		"cluster_name" : "c1",
      		"component_name" : "DATANODE",
      		"service_name" : "HDFS"
      		"state" : "STARTED"
    	},
    	"host_components" : [
      		{
      			"href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/host1/host_components/DATANODE",
      			"HostRoles" : {
        			"cluster_name" : "c1",
        			"component_name" : "DATANODE",
        			"host_name" : "host1"
        		}
      		}
       	]
    }


Management
----
The Ambari API provides for the management of the resources of an Apache Hadoop cluster.  This includes the creation, deletion and updating of resources.

###POST
The POST method creates a new resource. If a new resource is created then a 201 response code is returned.  The code 202 can also be returned to indicate that the instruction was accepted by the server (see [asynchronous response](#asynchronous-response)). 

**Example**

Create the HDFS service.


    POST /clusters/c1/services/HDFS


**Response**

    201 Created

###PUT
Use the PUT method to update resources.  If an existing resource is modified then a 200 response code is retrurned to indicate successful completion of the request.  The response code 202 can also be returned to indicate that the instruction was accepted by the server (see [asynchronous response](#asynchronous-response)).

**Example**

Start the HDFS service (update the state of the HDFS service to be ‘STARTED’).


    PUT /clusters/c1/services/HDFS/

**Body**

    {
      "ServiceInfo": {
        "state" : "STARTED”
      }
    }


**Response**

The response code 202 indicates that the server has accepted the instruction to update the resource.  The body of the response contains the ID and href of the request resource that was created to carry out the instruction (see [asynchronous response](#asynchronous-response)).

    202 Accepted
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/3",
      "Requests" : {
        "id" : 3,
        "status" : "InProgress"
      } 
    }


###DELETE
Use the DELETE method to delete a resource. If an existing resource is deleted then a 200 response code is retrurned to indicate successful completion of the request.  The response code 202 can also be returned which indicates that the instruction was accepted by the server and the resource was marked for deletion (see [asynchronous response](#asynchronous-response)).

**Example**

Delete the cluster named 'c1'.

    DELETE /clusters/c1

**Response**

    200 OK

###Asynchronous Response

The managment APIs can return a response code of 202 which indicates that the request has been accepted.  The body of the response contains the ID and href of the request resource that was created to carry out the instruction. 
    
    202 Accepted
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/6",
      "Requests" : {
        "id" : 6,
        "status" : "InProgress"
      } 
    }

The href in the response body can then be used to query the associated request resource and monitor the progress of the request.  A request resource has one or more task sub resources.  The following example shows how to use [partial response](#partial-response) to query for task resources of a request resource. 

    /clusters/c1/requests/6?fields=tasks/Tasks/*   
    
The returned task resources can be used to determine the status of the request.

    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/6",
      "Requests" : {
        "id" : 6,
        "cluster_name" : "c1"
      },
      "tasks" : [
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/6/tasks/32",
          "Tasks" : {
            "exit_code" : 777,
            "stdout" : "default org.apache.hadoop.mapred.CapacityTaskScheduler\nwarning: Dynamic lookup of ...",
            "status" : "IN_PROGRESS",
            "stderr" : "",
            "host_name" : "dev.hortonworks.com",
            "id" : 32,
            "cluster_name" : "c1",
            "attempt_cnt" : 1,
            "request_id" : 6,
            "command" : "START",
            "role" : "NAMENODE",
            "start_time" : 1367240498196,
            "stage_id" : 1
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/6/tasks/33",
          "Tasks" : {
            "exit_code" : 999,
            "stdout" : "",
            "status" : "PENDING",
            ...
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/6/tasks/31",
          "Tasks" : {
            "exit_code" : 0,
            "stdout" : "warning: Dynamic lookup of $ambari_db_rca_username ...",
            "status" : "COMPLETED",
            ...
          }
        }
      ]
    }

Resources
----
###Collection Resources


A collection resource is a set of resources of the same type, rather than any specific resource. For example:

    /clusters  

  _Refers to a collection of clusters_

###Instance Resources

An instance resource is a single specific resource. For example:

    /clusters/c1

  _Refers to the cluster resource identified by the id "c1"_

###Types
Resources are grouped into types.  This allows the user to query for collections of resources of the same type.  Some resource types are composed of subtypes (e.g. services are sub-resources of clusters).

The following is a list of some of the Ambari resource types with descriptions and usage examples.
 
#### clusters
Cluster resources represent named Hadoop clusters.  Clusters are top level resources. 

[Cluster Resources](cluster-resources.md)

#### services
Service resources are services of a Hadoop cluster (e.g. HDFS, MapReduce and Ganglia).  Service resources are sub-resources of clusters. 

[Service Resources](service-resources.md)

#### components
Component resources are the individual components of a service (e.g. HDFS/NameNode and MapReduce/JobTracker).  Components are sub-resources of services.

[Component Resources](component-resources.md)

#### hosts
Host resources are the host machines that make up a Hadoop cluster.  Hosts are top level resources but can also be sub-resources of clusters. 

[Host Resources](host-resources.md)


#### host_components
Host component resources are usages of a component on a particular host.  Host components are sub-resources of hosts.

[Host Component Resources](host-component-resources.md)


#### configurations
Configuration resources are sets of key/value pairs that configure the services of a Hadoop cluster.

[Configuration Resource Overview](configuration.md)


#### workflows
Workflow resources are DAGs of MapReduce jobs in a Hadoop cluster.

[Workflow Resources](workflow-resources.md)

#### jobs
Job resources represent individual nodes (MapReduce jobs) in a workflow.

[Job Resources](job-resources.md)

#### taskattempts
Task attempt resources are individual attempts at map or reduce tasks for a job.

[Task Attempt Resources](taskattempt-resources.md)


Partial Response
----

Used to control which fields are returned by a query.  Partial response can be used to restrict which fields are returned and additionally, it allows a query to reach down and return data from sub-resources.  The keyword “fields” is used to specify a partial response.  Only the fields specified will be returned to the client.  To specify sub-elements, use the notation “a/b/c”.  Properties, categories and sub-resources can be specified.  The wildcard ‘*’ can be used to show all categories, fields and sub-resources for a resource.  This can be combined to provide ‘expand’ functionality for sub-components.  Some fields are always returned for a resource regardless of the specified partial response fields.  These fields are the fields, which uniquely identify the resource.  This would be the primary id field of the resource and the foreign keys to the primary id fields of all ancestors of the resource.

**Example: Using Partial Response to restrict response to a specific field**

    GET    /api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk/disk_total

    200 OK
	{
    	“href”: “.../api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk/disk_total”,
    	“ServiceComponentInfo” : {
        	“cluster_name” : “c1”,
        	“component_name” : NAMENODE”,
        	“service_name” : “HDFS”
    	},
    	“metrics” : {
        	"disk" : {       
            	"disk_total" : 100000
        	}
    	}
    }

**Example: Using Partial Response to restrict response to specified category**

    GET    /api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk

    200 OK
	{
    	“href”: “.../api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk”,
    	“ServiceComponentInfo” : {
        	“cluster_name” : “c1”,
        	“component_name” : NAMENODE”,
        	“service_name” : “HDFS”
    	},
    	“metrics” : {
        	"disk" : {       
            	"disk_total" : 100000,
            	“disk_free” : 50000,
            	“part_max_used” : 1010
        	}
    	}
	}

**Example – Using Partial Response to restrict response to multiple fields/categories**

	GET	/api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk/disk_total,metrics/cpu
	
	200 OK
	{
    	“href”: “.../api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk/disk_total,metrics/cpu”,
    	“ServiceComponentInfo” : {
        	“cluster_name” : “c1”,
        	“component_name” : NAMENODE”,
        	“service_name” : “HDFS”
    	},
    	“metrics” : {
        	"disk" : {       
            	"disk_total" : 100000
        	},
        	“cpu” : {
            	“cpu_speed” : 10000000,
            	“cpu_num” : 4,
            	“cpu_idle” : 999999,
            	...
        	}
    	}
	}

**Example – Using Partial Response to restrict response to a sub-resource**

	GET	/api/v1/clusters/c1/hosts/host1?fields=host_components

	200 OK
	{
    	“href”: “.../api/v1/clusters/c1/hosts/host1?fields=host_components”,
    	“Hosts” : {
        	“cluster_name” : “c1”,
        	“host_name” : “host1”
    	},
    	“host_components”: [
        	{
            	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/NAMENODE”
            	“HostRoles” : {
                	“cluster_name” : “c1”,
                	“component_name” : “NAMENODE”,
                	“host_name” : “host1”
            	}
        	},
        	{
            	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/DATANODE”
            	“HostRoles” : {
                	“cluster_name” : “c1”,
                	“component_name” : DATANODE”,
                	“host_name” : “host1”
            	}
        	},
            ... 
    	]
	}

**Example – Using Partial Response to expand a sub-resource one level deep**

	GET	/api/v1/clusters/c1/hosts/host1?fields=host_components/*

	200 OK
	{
    	“href”: “.../api/v1/clusters/c1/hosts/host1?fields=host_components/*”,
    	“Hosts” : {
        	“cluster_name” : “c1”,
        	“host_name” : “host1”
        },
        “host_components”: [
        	{
            	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/DATANODE”
            	“HostRoles” : {
                	“cluster_name” : “c1”,
               		“component_name” : DATANODE”,
                	“host_name” : “host1”,
                	“state” : “RUNNING”,
                	...
            	},        
            	"host" : {     
                	"href" : ".../api/v1/clusters/c1/hosts/host1"  
            	},
            	“metrics” : {
                	"disk" : {       
                    	"disk_total" : 100000000,       
                    	"disk_free" : 5000000,       
                    	"part_max_used" : 10101     
                	},
                	...
            	},
            	"component" : {
                	"href" : "http://ambari.server/api/v1/clusters/c1/services/HDFS/components/NAMENODE", 
                	“ServiceComponentInfo” : {
                    	"cluster_name" : "c1",         
                    	"component_name" : "NAMENODE",         
                    	"service_name" : "HDFS"       
                	}
            	}  
        	},
        	...
    	]
	}

**Example – Using Partial Response for multi-level expansion of sub-resources**
	
	GET /api/v1/clusters/c1/hosts/host1?fields=host_components/component/*
	
	200 OK
	{
    	“href”: “http://ambari.server/api/v1/clusters/c1/hosts/host1?fields=host_components/*”,
    	“Hosts” : {
        	“cluster_name” : “c1”,
        	“host_name” : “host1”
        	...
    	},
    	“host_components”: [
    		{
            	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/DATANODE”,
            	“HostRoles” : {
                	“cluster_name” : “c1”,
                	“component_name” : DATANODE”,
                	“host_name” : “host1”
            	}, 
            	"component" : {
                	"href" : "http://ambari.server/api/v1/clusters/c1/services/HDFS/components/DATANODE", 
                	“ServiceComponentInfo” : {
                   		"cluster_name" : "c1",         
                    	"component_name" : "DATANODE",         
                    	"service_name" : "HDFS"  
                    	...     
                	},
             		“metrics”: {
                   		“dfs”: {
                       		“datanode” : {
          	                	“blocks_written " :  10000,
          	                	“blocks_read" : 5000,
                             	...
                        	}
                    	},
                    	“disk”: {
                       		"disk_total " :  1000000,
                        	“disk_free" : 50000,
                        	...
                    	},
                   		... 	
					}
            	}
        	},
        	{
            	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/NAMENODE”,
            	“HostRoles” : {
                	“cluster_name” : “c1”,
                	“component_name” : NAMENODE”,
                	“host_name” : “host1”
            	}, 
            	"component" : {
                	"href" : "http://ambari.server/api/v1/clusters/c1/services/HDFS/components/NAMENODE", 
                	“ServiceComponentInfo” : {
                   		"cluster_name" : "c1",         
                    	"component_name" : "NAMENODE",         
                    	"service_name" : "HDFS"       
                	},
             		“metrics”: {
                    	“dfs”: {
                       		“namenode” : {
          	            		“FilesRenamed " :  10,
          	            		“FilesDeleted" : 5
                         		…
                    		}
						},	
                    	“disk”: {
                       		"disk_total " :  1000000,
                       		“disk_free" : 50000,
                        	...
                    	}
                	},
                	...
            	}
        	},
        	...
    	]
	}

**Example: Using Partial Response to expand collection resource instances one level deep**

	GET /api/v1/clusters/c1/hosts?fields=*

	200 OK
	{
    	“href” : “http://ambari.server/api/v1/clusters/c1/hosts/?fields=*”,    
    	“items”: [ 
        	{
            	“href” : “http://ambari.server/api/v1/clusters/c1/hosts/host1”,
            	“Hosts” : {
                	“cluster_name” :  “c1”,
                	“host_name” : “host1”
            	},
            	“metrics”: {
                	“process”: {          	    
                   		"proc_total" : 1000,
          	       		"proc_run" : 1000
                	},
                	...
            	},
            	“host_components”: [
                	{
                   		“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/NAMENODE”
                    	“HostRoles” : {
                       		“cluster_name” : “c1”,
                         	“component_name” : “NAMENODE”,
                        	“host_name” : “host1”
                    	}
                	},
                	{
                    	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/DATANODE”
                    	“HostRoles” : {
                       		“cluster_name” : “c1”,
                        	“component_name” : DATANODE”,
                        	“host_name” : “host1”
                    	}
                	},
                	...
            	},
            	...
        	},
        	{
            	“href” : “http://ambari.server/api/v1/clusters/c1/hosts/host2”,
            	“Hosts” : {
                	“cluster_name” :  “c1”,
                	“host_name” : “host2”
            	},
            	“metrics”: {
               		“process”: {          	    
                   		"proc_total" : 555,
          	     		"proc_run" : 55
                	},
                	...
            	},
            	“host_components”: [
                	{
                   		“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/DATANODE”
                    	“HostRoles” : {
                       		“cluster_name” : “c1”,
                        	“component_name” : “DATANODE”,
                        	“host_name” : “host2”
                    	}
                	},
                	...
            	],
            	...
        	},
        	...
    	]
	}

### Additional Partial Response Examples

**Example – For each cluster, get cluster name, all hostname’s and all service names**

	GET   /api/v1/clusters?fields=Clusters/cluster_name,hosts/Hosts/host_name,services/ServiceInfo/service_name

**Example - Get all hostname’s for a given component**

	GET	/api/v1/clusters/c1/services/HDFS/components/DATANODE?fields=host_components/HostRoles/host_name

**Example - Get all hostname’s and component names for a given service**

	GET	/api/v1/clusters/c1/services/HDFS?fields=components/host_components/HostRoles/host_name,
                                      	          components/host_components/HostRoles/component_name



Query Predicates
----

Used to limit which data is returned by a query.  This is synonymous to the “where” clause in a SQL query.  Providing query parameters does not result in any link expansion in the data that is returned, with the exception of the fields used in the predicates.  Query predicates can only be applied to collection resources.  A predicate consists of at least one relational expression.  Predicates with multiple relational expressions also contain logical operators, which connect the relational expressions.  Predicates may also use brackets for explicit grouping of expressions. 

###Relational Query Operators

<table>
  <tr>
    <th>Operator</th>
    <th>Example</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>=</td>
    <td>name=host1</td>
    <td>String or numerical EQUALS</td>
  </tr>
  <tr>
    <td>!=</td>
    <td>name!=host1</td>
    <td>String or numerical NOT EQUALS</td>
  </tr>
  <tr>
    <td>&lt;</td>
    <td>disk_total&lt;50</td>
    <td>Numerical LESS THAN</td>
  </tr>
  <tr>
    <td>&gt;</td>
    <td>disk_total&gt;50</td>
    <td>Numerical GREATER THAN</td>
  </tr>
  <tr>
    <td>&lt;=</td>
    <td>disk_total&lt;=50</td>
    <td>Numerical LESS THAN OR EQUALS</td>
  </tr>
  <tr>
    <td>&gt;=</td>
    <td>disk_total&gt;=50</td>
    <td>Numerical GREATER THAN OR EQUALS</td>
  </tr>  
</table>

###Logical Query Operators

<table>
  <tr>
    <th>Operator</th>
    <th>Example</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>|</td>
    <td>name=host1|name=host2</td>
    <td>Logical OR operator</td>
  </tr>
  <tr>
    <td>&</td>
    <td>prop1=foo&prop2=bar</td>
    <td>Logical AND operator</td>
  </tr>
  <tr>
    <td>!</td>
    <td>!prop<50</td>
    <td>Logical NOT operator</td>
  </tr>
</table>

**Logical Operator Precedence**

Standard logical operator precedence rules apply.  The above logical operators are listed in order of precedence starting with the lowest priority.  

###Brackets

<table>
  <tr>
    <th>Bracket</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>(</td>
    <td>Opening Bracket</td>
  </tr>
  <tr>
    <td>)</td>
    <td>Closing Bracket</td>
  </tr>

</table>
  
Brackets can be used to provide explicit grouping of expressions. Expressions within brackets have the highest precedence.

###Operator Functions
 
<table>
  <tr>
    <th>Operator</th>
    <th>Example</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>in()</td>
    <td>name.in(foo,bar)</td>
    <td>IN function.  More compact form of name=foo|name=bar. </td>
  </tr>
  <tr>
    <td>isEmpty()</td>
    <td>category.isEmpty()</td>
    <td>Used to determine if a category contains any properties. </td>
  </tr>
</table>
Operator functions behave like relational operators and provide additional functionality.  Some operator functions, such as in(), act as binary operators like the above relational operators, where there is a left and right operand.  Some operator functions are unary operators, such as isEmpty(), where there is only a single operand.

###Query Examples

**Example – Get all hosts with “HEALTHY” status that have 2 or more cpu**
	
	GET	/api/v1/clusters/c1/hosts?Hosts/host_status=HEALTHY&Hosts/cpu_count>=2
	
**Example – Get all hosts with less than 2 cpu or host status != HEALTHY**
	

	GET	/api/v1/clusters/c1/hosts?Hosts/cpu_count<2|Hosts/host_status!=HEALTHY

**Example – Get all “rhel6” hosts with less than 2 cpu or “centos6” hosts with 3 or more cpu**  

	GET	/api/v1/clusters/c1/hosts?Hosts/os_type=rhel6&Hosts/cpu_count<2|Hosts/os_type=centos6&Hosts/cpu_count>=3

**Example – Get all hosts where either state != “HEALTHY” or last_heartbeat_time < 1360600135905 and rack_info=”default_rack”**

	GET	/api/v1/clusters/c1/hosts?(Hosts/host_status!=HEALTHY|Hosts/last_heartbeat_time<1360600135905)
                                  &Hosts/rack_info=default_rack

**Example – Get hosts with host name of host1 or host2 or host3 using IN operator**
	
	GET	/api/v1/clusters/c1/hosts?Hosts/host_name.in(host1,host2,host3)

**Example – Get and expand all HDFS components, which have at least 1 property in the “metrics/jvm” category (combines query and partial response syntax)**

	GET	/api/v1/clusters/c1/services/HDFS/components?!metrics/jvm.isEmpty()&fields=*

**Example – Update the state of all ‘INSTALLED’ services to be ‘STARTED’**

	PUT /api/v1/clusters/c1/services?ServiceInfo/state=INSTALLED 
    {
      "ServiceInfo": {
        "state" : "STARTED”
      }
    }

Batch Requests
---
Requests can be batched.  This allows for multiple bodies to be specified as an array in a single request. 

**Example – Creating multiple hosts in a single request**
     
    POST /api/v1/clusters/c1/hosts/         

    [
      {
        "Hosts" : {
          "cluster_name" : "c1",
          "host_name" : "host1"
        }
      },
      {
        "Hosts" : {
          "cluster_name" : "c1",
          "host_name" : "host2"
        }
      },
      {
        "Hosts" : {
          "cluster_name" : "c1",
          "host_name" : "host3"
        }
      }
    ]


RequestInfo
---
RequestInfo allows the user to specify additional properties in the body of a request.

<table>
  <tr>
    <th>Key</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>query</td>
    <td>The query string for the request.  Useful for overcoming length limits of the URL and for specifying a query string for each element of a batch request.</td>  
  </tr>
  <tr>
    <td>context</td>
    <td>The context string.  The API client can pass a string context to able to specify what the requests were for (e.g., "HDFS Service Start", "Add Hosts", etc.)</td>  
  </tr>
</table>

###query


The query property allows the user to specify yhe query string as part of the request body.  This is sometimes required in the case of a very long query string that causes the request to exceed the limits of the URL.

**Example – Specifying the query string in the request body**

    PUT  /clusters/c1/services
    
    {
      "RequestInfo":{
        "query":"ServiceInfo/state=STARTED&ServiceInfo/service_name=HDFS&…"
      },

      "Body":
      {
        "ServiceInfo": {
          "state" : "INSTALLED"
        }
      }
    }
    
The query property can also be applied to the elements of a [batch request](#batch-request).

**Example – Specifying the query string in the request body for a batch request**


    PUT /api/v1/clusters/c1/hosts
    
    [
      {
        "RequestInfo":{
          "query":"Hosts/host_name=host1"
        },
        "Body":
        {
          "Hosts": {
            "desired_config": {
              "type": "global",
              "tag": "version50",
              "properties": { "a": "b", "x": "y" }
            }
          }
        }
      },
      {
        "RequestInfo":{
          "query":"Hosts/host_name=host2"
        },
        "Body":
        {
          "Hosts": {
            "desired_config": {
              "type": "global",
              "tag": "version51",
              "properties": { "a": "c", "x": "z" }
            }
          }
        }
      }
    ]


###context
In some cases a request will return a 202 to indicate that the instruction was accepted by the server (see [asynchronous response](#asynchronous-response)).  In these cases the body of the response contains the ID and href of the request resource that was created to carry out the instruction.  It may be desirable to attach a context string to the request which will then be assigned to the resulting request response.

In the following example a request is made to stop the HDFS service.  Notice that a context is passed as a RequestInfo property.

**Example – Specifying the query string in the request body**

    PUT  /clusters/c1/services
    
    {
      "RequestInfo":{
        "query":"ServiceInfo/state=STARTED&ServiceInfo/service_name=HDFS",
        "context":"Stop HDFS service."
      },

      "Body":
      {
        "ServiceInfo": {
          "state" : "INSTALLED"
        }
      }
    }

**Response**

    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/13",
      "Requests" : {
        "id" : 13,
        "status" : "InProgress"
      }
    }
    
When the request resource returned in the above example is queried, the supplied context string is returned as part of the response.

    GET api/v1/clusters/c1/requests/13 

**Response**


    {
      "href" : "http://ec2-50-19-183-89.compute-1.amazonaws.com:8080/api/v1/clusters/c1/requests/13",
      "Requests" : {
        "id" : 13,
        "cluster_name" : "c1",
        "request_context" : "Stop HDFS service."
      },
      "tasks" : [
        …
      ]
    }
 

Temporal Metrics
----

Some metrics have values that are available across a range in time.  To query a metric for a range of values, the following partial response syntax is used.  

To get temporal data for a single property:
?fields=category/property[start-time,end-time,step]	

To get temporal data for all properties in a category:
?fields=category[start-time,end-time,step]

start-time: Required field.  The start time for the query in Unix epoch time format.
end-time: Optional field, defaults to now.  The end time for the query in Unix epoch time format.
step: Optional field, defaults to the corresponding metrics system’s default value.  If provided, end-time must also be provided. The interval of time between returned data points specified in seconds. The larger the value provided, the fewer data points returned so this can be used to limit how much data is returned for the given time range.  This is only used as a suggestion so the result interval may differ from the one specified.

The returned result is a list of data points over the specified time range.  Each data point is a value / timestamp pair.

**Note**: It is important to understand that requesting large amounts of temporal data may result in severe performance degradation.  **Always** request the minimal amount of information necessary.  If large amounts of data are required, consider splitting the request up into multiple smaller requests.

**Example – Temporal Query for a single property using only start-time**

	GET	/api/v1/clusters/c1/hosts/host1?fields=metrics/jvm/gcCount[1360610225]

	
	200 OK
	{
    	“href” : …/api/v1/clusters/c1/hosts/host1?fields=metrics/jvm/gcCount[1360610225]”,
    	...
    	“metrics”: [
        	{
            	“jvm”: {
          	    	"gcCount" : [
                   		[10, 1360610165],
                     	[12, 1360610180],
                     	[13, 1360610195],
                     	[14, 1360610210],
                     	[15, 1360610225]
                  	]
             	}
         	}
    	]
	}

**Example – Temporal Query for a category using start-time, end-time and step**

	GET	/api/v1/clusters/c1/hosts/host1?fields=metrics/jvm[1360610200,1360610500,100]

	200 OK
	{
    	“href” : …/clusters/c1/hosts/host1?fields=metrics/jvm[1360610200,1360610500,100]”,
    	...
    	“metrics”: [
        	{
            	“jvm”: {
          	    	"gcCount" : [
                   		[10, 1360610200],
                     	[12, 1360610300],
                     	[13, 1360610400],
                     	[14, 1360610500]
                  	],
                	"gcTimeMillis" : [
                   		[1000, 1360610200],
                     	[2000, 1360610300],
                     	[5000, 1360610400],
                     	[9500, 1360610500]
                  	],
                  	...
             	}
         	}
    	]
	}

	


HTTP Return Codes
----

The following HTTP codes may be returned by the API.
<table>
  <tr>
    <th>HTTP CODE</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>200</td>
    <td>OK</td>  
  </tr>
  <tr>
    <td>400</td>
    <td>Bad Request</td>  
  </tr>
  <tr>
    <td>401</td>
    <td>Unauthorized</td>  
  </tr>
  <tr>
    <td>403</td>
    <td>Forbidden</td>  
  </tr> 
  <tr>
    <td>404</td>
    <td>Not Found</td>  
  </tr>
  <tr>
    <td>500</td>
    <td>Internal Server Error</td>  
  </tr>
</table>


Errors
----

**Example errors responses**

    404 Not Found
	{   
    	"status" : 404,   
    	"message" : "The requested resource doesn't exist: Cluster not found, clusterName=someInvalidCluster" 
	} 

&nbsp;

	400 Bad Request
	{   
    	"status" : 400,   
    	"message" : "The properties [foo] specified in the request or predicate are not supported for the 
                	 resource type Cluster."
	}


diff --git a/ambari-server/docs/api/v1/job-resources.md b/ambari-server/docs/api/v1/job-resources.md
new file mode 100644
index 0000000000..428bc50e1d
-- /dev/null
++ b/ambari-server/docs/api/v1/job-resources.md
@@ -0,0 +1,22 @@
<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Job Resources
 

- [List jobs](jobs.md)
- [View job information](jobs-job.md)
diff --git a/ambari-server/docs/api/v1/jobs-job.md b/ambari-server/docs/api/v1/jobs-job.md
new file mode 100644
index 0000000000..d5570836de
-- /dev/null
++ b/ambari-server/docs/api/v1/jobs-job.md
@@ -0,0 +1,60 @@
<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

View Job Information
=====

[Back to Resources](index.md#resources)

Returns information about a single job in a given workflow.

    GET /clusters/:name/workflows/:workflowid/jobs/:jobid

**Response**

    200 OK
    {
        "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305061943_0001/jobs/job_201305061943_0001",
        "Job" : {
            "cluster_name" : "c1",
            "workflow_id" : "mr_201305061943_0001",
            "job_id" : "job_201305061943_0001",
            "reduces" : 1,
            "status" : "SUCCESS",
            "workflow_entity_name" : "X",
            "input_bytes" : 2009,
            "output_bytes" : 1968,
            "conf_path" : "hdfs://your.server:8020/user/ambari-qa/\\.staging/job_201305061943_0001/job\\.xml",
            "user_name" : "ambari-qa",
            "elapsed_time" : 25734,
            "maps" : 1,
            "name" : "word count",
            "submit_time" : 1367883861310
        },
        "taskattempts" : [
            {
                "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305061943_0001/jobs/job_201305061943_0001/taskattempts/attempt_201305061943_0001_m_000000_0",
                "Job" : {
                    "cluster_name" : "c1",
                    "workflow_id" : "mr_201305061943_0001",
                    "job_id" : "job_201305061943_0001",
                    "task_attempt_id" : "attempt_201305061943_0001_m_000000_0"
                }
            },
            ...
        ]
    }
diff --git a/ambari-server/docs/api/v1/jobs.md b/ambari-server/docs/api/v1/jobs.md
new file mode 100644
index 0000000000..13e0b00b89
-- /dev/null
++ b/ambari-server/docs/api/v1/jobs.md
@@ -0,0 +1,42 @@
<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

List Jobs
=====

[Back to Resources](index.md#resources)

Returns a collection of all jobs in a given workflow.

    GET /clusters/:name/workflows/:workflowid/jobs

**Response**

    200 OK
    {
        "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305061943_0001/jobs",
        "items" : [
            {
                "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305061943_0001/jobs/job_201305061943_0001",
                "Job" : {
                    "cluster_name" : "c1",
                    "workflow_id" : "mr_201305061943_0001",
                    "job_id" : "job_201305061943_0001"
                }
            }
        ]
    }
diff --git a/ambari-server/docs/api/v1/services.md b/ambari-server/docs/api/v1/services.md
index a983a71fec..8a9b68d8df 100644
-- a/ambari-server/docs/api/v1/services.md
++ b/ambari-server/docs/api/v1/services.md
@@ -92,4 +92,4 @@ Get the collection of the services for the cluster named "c1".
         	  	}	
         	}
         ]
    }    
    }    
\ No newline at end of file
diff --git a/ambari-server/docs/api/v1/taskattempt-resources.md b/ambari-server/docs/api/v1/taskattempt-resources.md
new file mode 100644
index 0000000000..0e540a72ff
-- /dev/null
++ b/ambari-server/docs/api/v1/taskattempt-resources.md
@@ -0,0 +1,22 @@
<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# TaskAttempt Resources
 

- [List task attempts](taskattempts.md)
- [View task attempt information](taskattempts-taskattempt.md)
diff --git a/ambari-server/docs/api/v1/taskattempts-taskattempt.md b/ambari-server/docs/api/v1/taskattempts-taskattempt.md
new file mode 100644
index 0000000000..72bedf1dc1
-- /dev/null
++ b/ambari-server/docs/api/v1/taskattempts-taskattempt.md
@@ -0,0 +1,48 @@
<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

View Task Attempt Information
=====

[Back to Resources](index.md#resources)

Returns information about a single task attempt for a given job.

    GET /clusters/:name/workflows/:workflowid/jobs/:jobid/taskattempts/:taskattempt

**Response**

    200 OK
    {
        "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305061943_0001/jobs/job_201305061943_0001/taskattempts/attempt_201305061943_0001_m_000000_0",
        "TaskAttempt" : {
            "cluster_name" : "c1",
            "workflow_id" : "mr_201305061943_0001",
            "job_id" : "job_201305061943_0001",
            "task_attempt_id" : "attempt_201305061943_0001_m_000000_0",
            "status" : "SUCCESS",
            "finish_time" : 1367883874107,
            "input_bytes" : 2009,
            "type" : "MAP",
            "output_bytes" : 61842,
            "shuffle_finish_time" : 0,
            "locality" : "NODE_LOCAL",
            "start_time" : 1367883871399,
            "sort_finish_fime" : 0,
            "map_finish_time" : 1367883874107
        }
    }
diff --git a/ambari-server/docs/api/v1/taskattempts.md b/ambari-server/docs/api/v1/taskattempts.md
new file mode 100644
index 0000000000..1cdbca965d
-- /dev/null
++ b/ambari-server/docs/api/v1/taskattempts.md
@@ -0,0 +1,70 @@
<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

List Task Attempts
=====

[Back to Resources](index.md#resources)

Returns a collection of all task attempts for a given job.

    GET /clusters/:name/workflows/:workflowid/jobs/:jobid/taskattempts

**Response**

    200 OK
    {
        "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305061943_0001/jobs/job_201305061943_0001/taskattempts",
        "items" : [
            {
                "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305061943_0001/jobs/job_201305061943_0001/taskattempts/attempt_201305061943_0001_m_000001_0",
                "TaskAttempt" : {
                    "cluster_name" : "c1",
                    "task_attempt_id" : "attempt_201305061943_0001_m_000001_0",
                    "workflow_id" : "mr_201305061943_0001",
                    "job_id" : "job_201305061943_0001"
                }
            },
            {
                "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305061943_0001/jobs/job_201305061943_0001/taskattempts/attempt_201305061943_0001_m_000002_0",
                "TaskAttempt" : {
                    "cluster_name" : "c1",
                    "task_attempt_id" : "attempt_201305061943_0001_m_000002_0",
                    "workflow_id" : "mr_201305061943_0001",
                    "job_id" : "job_201305061943_0001"
                }
            },
            {
                "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305061943_0001/jobs/job_201305061943_0001/taskattempts/attempt_201305061943_0001_r_000000_0",
                "TaskAttempt" : {
                    "cluster_name" : "c1",
                    "task_attempt_id" : "attempt_201305061943_0001_r_000000_0",
                    "workflow_id" : "mr_201305061943_0001",
                    "job_id" : "job_201305061943_0001"
                }
            },
            {
                "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305061943_0001/jobs/job_201305061943_0001/taskattempts/attempt_201305061943_0001_m_000000_0",
                "TaskAttempt" : {
                    "cluster_name" : "c1",
                    "task_attempt_id" : "attempt_201305061943_0001_m_000000_0",
                    "workflow_id" : "mr_201305061943_0001",
                    "job_id" : "job_201305061943_0001"
                }
            }
        ]
    }
diff --git a/ambari-server/docs/api/v1/update-hostcomponent.md b/ambari-server/docs/api/v1/update-hostcomponent.md
index 161ea07815..ba862afe64 100644
-- a/ambari-server/docs/api/v1/update-hostcomponent.md
++ b/ambari-server/docs/api/v1/update-hostcomponent.md
@@ -1,3 +1,4 @@

 <!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements. See the NOTICE file distributed with
@@ -127,4 +128,4 @@ Put the NAMENODE component into 'MAINTENANCE' mode.
         "id" : 14,
         "status" : "InProgress"
       }
    }    
    }    
\ No newline at end of file
diff --git a/ambari-server/docs/api/v1/update-service.md b/ambari-server/docs/api/v1/update-service.md
index f27ed5ecdf..2f47ccd32e 100644
-- a/ambari-server/docs/api/v1/update-service.md
++ b/ambari-server/docs/api/v1/update-service.md
@@ -1,3 +1,4 @@

 <!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements. See the NOTICE file distributed with
@@ -22,39 +23,8 @@ Update Service
 
 **Summary**
 

    PUT /clusters/c1/services/HDFS/

**Body**

Start the HDFS service (update the state of the HDFS service to be ‘STARTED’).


    PUT /clusters/c1/services/HDFS
    {
      "ServiceInfo": {
        "state" : "STARTED”
      }
    }

 Update the service identified by ":serviceName" of the cluster identified by ":clusterName".
 
**Response**

    202 Accepted
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/3",
      "Requests" : {
        "id" : 3,
        "status" : "InProgress"
      } 
    }

**Example 2**

Stop the HDFS service (update the state of the HDFS service to be ‘INSTALLED’).


     PUT /clusters/:clusterName/services/:serviceName
 
 
@@ -94,6 +64,31 @@ Stop the HDFS service (update the state of the HDFS service to be ‘INSTALLED
 
 **Example 1**
 
Start the HDFS service (update the state of the HDFS service to be ‘STARTED’).


    PUT /clusters/c1/services/HDFS
    {
      "ServiceInfo": {
        "state" : "STARTED”
      }
    }

    202 Accepted
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/3",
      "Requests" : {
        "id" : 3,
        "status" : "InProgress"
      } 
    }

**Example 2**

Stop the HDFS service (update the state of the HDFS service to be ‘INSTALLED’).


    PUT /clusters/c1/services/HDFS/
     {
       "ServiceInfo": {
         "state" : "INSTALLED”
diff --git a/ambari-server/docs/api/v1/update-services.md b/ambari-server/docs/api/v1/update-services.md
index 6fe3febe0e..a230ae654e 100644
-- a/ambari-server/docs/api/v1/update-services.md
++ b/ambari-server/docs/api/v1/update-services.md
@@ -1,3 +1,4 @@

 <!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements. See the NOTICE file distributed with
diff --git a/ambari-server/docs/api/v1/workflow-resources.md b/ambari-server/docs/api/v1/workflow-resources.md
new file mode 100644
index 0000000000..ab47339ead
-- /dev/null
++ b/ambari-server/docs/api/v1/workflow-resources.md
@@ -0,0 +1,22 @@
<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Workflow Resources
 

- [List workflows](workflows.md)
- [View workflow information](workflows-workflow.md)
diff --git a/ambari-server/docs/api/v1/workflows-workflow.md b/ambari-server/docs/api/v1/workflows-workflow.md
new file mode 100644
index 0000000000..9280367289
-- /dev/null
++ b/ambari-server/docs/api/v1/workflows-workflow.md
@@ -0,0 +1,57 @@
<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

View Workflow Information
=====

[Back to Resources](index.md#resources)

Returns information about a single workflow in a given cluster.

    GET /clusters/:name/workflows/:workflowid

**Response**

    200 OK
    {
        "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305061943_0001",
        "Workflow" : {
            "cluster_name" : "c1",
            "workflow_id" : "mr_201305061943_0001"
            "last_update_time" : 1367883887044,
            "input_bytes" : 2009,
            "output_bytes" : 1968,
            "user_name" : "ambari-qa",
            "elapsed_time" : 25734,
            "num_jobs_total" : 1,
            "num_jobs_completed" : 1,
            "name" : "word count",
            "context" : "{\"workflowId\":null,\"workflowName\":null,\"workflowDag\":{\"entries\":[{\"source\":\"X\",\"targets\":[]}]},\"parentWorkflowContext\":null,\"workflowEntityName\":null}",
            "start_time" : 1367883861310,
            "parent_id" : null
        },
        "jobs" : [
            {
                "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305061943_0001/jobs/job_201305061943_0001",
                "Job" : {
                    "cluster_name" : "c1",
                    "workflow_id" : "mr_201305061943_0001",
                    "job_id" : "job_201305061943_0001"
                }
            }
        ]
    }
diff --git a/ambari-server/docs/api/v1/workflows.md b/ambari-server/docs/api/v1/workflows.md
new file mode 100644
index 0000000000..b40a22ea1f
-- /dev/null
++ b/ambari-server/docs/api/v1/workflows.md
@@ -0,0 +1,48 @@
<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

List Workflows
=====

[Back to Resources](index.md#resources)

Returns a collection of all workflows in a given cluster.

    GET /clusters/:name/workflows

**Response**

    200 OK
    {
        "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows",
        "items" : [
            {
                "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305061943_0001",
                "Workflow" : {
                    "cluster_name" : "c1",
                    "workflow_id" : "mr_201305061943_0001"
                }
            },
            {
                "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305061943_0002",
                "Workflow" : {
                    "cluster_name" : "c1",
                    "workflow_id" : "mr_201305061943_0002"
                }
            }
        ]
    }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/state/ServiceComponentImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/state/ServiceComponentImpl.java
index ca50d35ab2..2ee986c198 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/state/ServiceComponentImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/state/ServiceComponentImpl.java
@@ -20,6 +20,8 @@ package org.apache.ambari.server.state;
 
 import java.util.*;
 import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
 
 import com.google.gson.Gson;
 import com.google.inject.Inject;
@@ -41,6 +43,8 @@ public class ServiceComponentImpl implements ServiceComponent {
 
   private final static Logger LOG =
       LoggerFactory.getLogger(ServiceComponentImpl.class);
  
  ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
 
   private final Service service;
 
@@ -154,260 +158,374 @@ public class ServiceComponentImpl implements ServiceComponent {
   }
 
   @Override
  public synchronized String getName() {
    return desiredStateEntity.getComponentName();
  public String getName() {
    readWriteLock.readLock().lock();
    try {
      return desiredStateEntity.getComponentName();
    } finally {
      readWriteLock.readLock().unlock();
    }

   }
 
   @Override
  public synchronized String getServiceName() {
    return service.getName();
  public String getServiceName() {
    readWriteLock.readLock().lock();
    try {
      return service.getName();
    } finally {
      readWriteLock.readLock().unlock();
    }

   }
 
   @Override
  public synchronized long getClusterId() {
    return this.service.getClusterId();
  public long getClusterId() {
    readWriteLock.readLock().lock();
    try {
      return this.service.getClusterId();
    } finally {
      readWriteLock.readLock().unlock();
    }

   }
 
   @Override
  public synchronized Map<String, ServiceComponentHost>
  public Map<String, ServiceComponentHost>
       getServiceComponentHosts() {
    return Collections.unmodifiableMap(hostComponents);
    readWriteLock.readLock().lock();
    try {
      return Collections.unmodifiableMap(hostComponents);
    } finally {
      readWriteLock.readLock().unlock();
    }

   }
 
   @Override
  public synchronized void addServiceComponentHosts(
  public void addServiceComponentHosts(
       Map<String, ServiceComponentHost> hostComponents) throws AmbariException {
    // TODO validation
    for (Entry<String, ServiceComponentHost> entry :
      hostComponents.entrySet()) {
      if (!entry.getKey().equals(entry.getValue().getHostName())) {
        throw new AmbariException("Invalid arguments in map"
            + ", hostname does not match the key in map");
    readWriteLock.writeLock().lock();
    try {
      // TODO validation
      for (Entry<String, ServiceComponentHost> entry :
          hostComponents.entrySet()) {
        if (!entry.getKey().equals(entry.getValue().getHostName())) {
          throw new AmbariException("Invalid arguments in map"
              + ", hostname does not match the key in map");
        }
       }
      for (ServiceComponentHost sch : hostComponents.values()) {
        addServiceComponentHost(sch);
      }
    } finally {
      readWriteLock.writeLock().unlock();
     }
    for (ServiceComponentHost sch : hostComponents.values()) {
      addServiceComponentHost(sch);
    }

   }
 
   @Override
  public synchronized void addServiceComponentHost(
  public void addServiceComponentHost(
       ServiceComponentHost hostComponent) throws AmbariException {
    // TODO validation
    // TODO ensure host belongs to cluster
    if (LOG.isDebugEnabled()) {
      LOG.debug("Adding a ServiceComponentHost to ServiceComponent"
          + ", clusterName=" + service.getCluster().getClusterName()
          + ", clusterId=" + service.getCluster().getClusterId()
          + ", serviceName=" + service.getName()
          + ", serviceComponentName=" + getName()
          + ", hostname=" + hostComponent.getHostName());
    }
    if (hostComponents.containsKey(hostComponent.getHostName())) {
      throw new AmbariException("Cannot add duplicate ServiceComponentHost"
          + ", clusterName=" + service.getCluster().getClusterName()
          + ", clusterId=" + service.getCluster().getClusterId()
          + ", serviceName=" + service.getName()
          + ", serviceComponentName=" + getName()
          + ", hostname=" + hostComponent.getHostName());
    readWriteLock.writeLock().lock();
    try {
      // TODO validation
      // TODO ensure host belongs to cluster
      if (LOG.isDebugEnabled()) {
        LOG.debug("Adding a ServiceComponentHost to ServiceComponent"
            + ", clusterName=" + service.getCluster().getClusterName()
            + ", clusterId=" + service.getCluster().getClusterId()
            + ", serviceName=" + service.getName()
            + ", serviceComponentName=" + getName()
            + ", hostname=" + hostComponent.getHostName());
      }
      if (hostComponents.containsKey(hostComponent.getHostName())) {
        throw new AmbariException("Cannot add duplicate ServiceComponentHost"
            + ", clusterName=" + service.getCluster().getClusterName()
            + ", clusterId=" + service.getCluster().getClusterId()
            + ", serviceName=" + service.getName()
            + ", serviceComponentName=" + getName()
            + ", hostname=" + hostComponent.getHostName());
      }
      // FIXME need a better approach of caching components by host
      ClusterImpl clusterImpl = (ClusterImpl) service.getCluster();
      clusterImpl.addServiceComponentHost(hostComponent);
      this.hostComponents.put(hostComponent.getHostName(), hostComponent);
    } finally {
      readWriteLock.writeLock().unlock();
     }
    // FIXME need a better approach of caching components by host
    ClusterImpl clusterImpl = (ClusterImpl) service.getCluster();
    clusterImpl.addServiceComponentHost(hostComponent);
    this.hostComponents.put(hostComponent.getHostName(), hostComponent);

   }
 
   @Override
  public synchronized ServiceComponentHost addServiceComponentHost(
  public ServiceComponentHost addServiceComponentHost(
       String hostName) throws AmbariException {
    // TODO validation
    // TODO ensure host belongs to cluster
    if (LOG.isDebugEnabled()) {
      LOG.debug("Adding a ServiceComponentHost to ServiceComponent"
          + ", clusterName=" + service.getCluster().getClusterName()
          + ", clusterId=" + service.getCluster().getClusterId()
          + ", serviceName=" + service.getName()
          + ", serviceComponentName=" + getName()
          + ", hostname=" + hostName);
    }
    if (hostComponents.containsKey(hostName)) {
      throw new AmbariException("Cannot add duplicate ServiceComponentHost"
          + ", clusterName=" + service.getCluster().getClusterName()
          + ", clusterId=" + service.getCluster().getClusterId()
          + ", serviceName=" + service.getName()
          + ", serviceComponentName=" + getName()
          + ", hostname=" + hostName);
    }
    ServiceComponentHost hostComponent =
        serviceComponentHostFactory.createNew(this, hostName, this.isClientComponent());
    // FIXME need a better approach of caching components by host
    ClusterImpl clusterImpl = (ClusterImpl) service.getCluster();
    clusterImpl.addServiceComponentHost(hostComponent);
    readWriteLock.writeLock().lock();
    try {
      // TODO validation
      // TODO ensure host belongs to cluster
      if (LOG.isDebugEnabled()) {
        LOG.debug("Adding a ServiceComponentHost to ServiceComponent"
            + ", clusterName=" + service.getCluster().getClusterName()
            + ", clusterId=" + service.getCluster().getClusterId()
            + ", serviceName=" + service.getName()
            + ", serviceComponentName=" + getName()
            + ", hostname=" + hostName);
      }
      if (hostComponents.containsKey(hostName)) {
        throw new AmbariException("Cannot add duplicate ServiceComponentHost"
            + ", clusterName=" + service.getCluster().getClusterName()
            + ", clusterId=" + service.getCluster().getClusterId()
            + ", serviceName=" + service.getName()
            + ", serviceComponentName=" + getName()
            + ", hostname=" + hostName);
      }
      ServiceComponentHost hostComponent =
          serviceComponentHostFactory.createNew(this, hostName, this.isClientComponent());
      // FIXME need a better approach of caching components by host
      ClusterImpl clusterImpl = (ClusterImpl) service.getCluster();
      clusterImpl.addServiceComponentHost(hostComponent);
 
    this.hostComponents.put(hostComponent.getHostName(), hostComponent);
      this.hostComponents.put(hostComponent.getHostName(), hostComponent);

      return hostComponent;
    } finally {
      readWriteLock.writeLock().unlock();
    }
 
    return hostComponent;
   }
 
   @Override
   public ServiceComponentHost getServiceComponentHost(String hostname)
     throws AmbariException {
    if (!hostComponents.containsKey(hostname)) {
      throw new ServiceComponentHostNotFoundException(getClusterName(),
          getServiceName(), getName(), hostname);
    readWriteLock.readLock().lock();
    try {
      if (!hostComponents.containsKey(hostname)) {
        throw new ServiceComponentHostNotFoundException(getClusterName(),
            getServiceName(), getName(), hostname);
      }
      return this.hostComponents.get(hostname);
    } finally {
      readWriteLock.readLock().unlock();
     }
    return this.hostComponents.get(hostname);

   }
 
   @Override
  public synchronized State getDesiredState() {
    return desiredStateEntity.getDesiredState();
  public State getDesiredState() {
    readWriteLock.readLock().lock();
    try {
      return desiredStateEntity.getDesiredState();
    } finally {
      readWriteLock.readLock().unlock();
    }

   }
 
   @Override
  public synchronized void setDesiredState(State state) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting DesiredState of Service"
          + ", clusterName=" + service.getCluster().getClusterName()
          + ", clusterId=" + service.getCluster().getClusterId()
          + ", serviceName=" + service.getName()
          + ", serviceComponentName=" + getName()
          + ", oldDesiredState=" + getDesiredState()
          + ", newDesiredState=" + state);
  public void setDesiredState(State state) {
    readWriteLock.writeLock().lock();
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Setting DesiredState of Service"
            + ", clusterName=" + service.getCluster().getClusterName()
            + ", clusterId=" + service.getCluster().getClusterId()
            + ", serviceName=" + service.getName()
            + ", serviceComponentName=" + getName()
            + ", oldDesiredState=" + getDesiredState()
            + ", newDesiredState=" + state);
      }
      desiredStateEntity.setDesiredState(state);
      saveIfPersisted();
    } finally {
      readWriteLock.writeLock().unlock();
     }
    desiredStateEntity.setDesiredState(state);
    saveIfPersisted();

   }
 
   @Override
  public synchronized Map<String, Config> getDesiredConfigs() {
    Map<String, Config> map = new HashMap<String, Config>();
    for (Entry<String, String> entry : desiredConfigs.entrySet()) {
      Config config = service.getCluster().getConfig(entry.getKey(), entry.getValue());
      if (null != config) {
        map.put(entry.getKey(), config);
  public Map<String, Config> getDesiredConfigs() {
    readWriteLock.readLock().lock();
    try {
      Map<String, Config> map = new HashMap<String, Config>();
      for (Entry<String, String> entry : desiredConfigs.entrySet()) {
        Config config = service.getCluster().getConfig(entry.getKey(), entry.getValue());
        if (null != config) {
          map.put(entry.getKey(), config);
        }
       }
    }
 
    Map<String, Config> svcConfigs = service.getDesiredConfigs();
    for (Entry<String, Config> entry : svcConfigs.entrySet()) {
      if (!map.containsKey(entry.getKey())) {
        map.put(entry.getKey(), entry.getValue());
      Map<String, Config> svcConfigs = service.getDesiredConfigs();
      for (Entry<String, Config> entry : svcConfigs.entrySet()) {
        if (!map.containsKey(entry.getKey())) {
          map.put(entry.getKey(), entry.getValue());
        }
       }

      return Collections.unmodifiableMap(map);
    } finally {
      readWriteLock.readLock().unlock();
     }
 
    return Collections.unmodifiableMap(map);
   }
 
   @Override
  public synchronized void updateDesiredConfigs(Map<String, Config> configs) {

    for (Entry<String,Config> entry : configs.entrySet()) {
      boolean contains = false;

      for (ComponentConfigMappingEntity componentConfigMappingEntity : desiredStateEntity.getComponentConfigMappingEntities()) {
        if (entry.getKey().equals(componentConfigMappingEntity.getConfigType())) {
          contains = true;
          componentConfigMappingEntity.setTimestamp(new Date().getTime());
          componentConfigMappingEntity.setVersionTag(entry.getValue().getVersionTag());
          if (persisted) {
            componentConfigMappingDAO.merge(componentConfigMappingEntity);
  public void updateDesiredConfigs(Map<String, Config> configs) {

    readWriteLock.writeLock().lock();
    try {
      for (Entry<String, Config> entry : configs.entrySet()) {
        boolean contains = false;

        for (ComponentConfigMappingEntity componentConfigMappingEntity : desiredStateEntity.getComponentConfigMappingEntities()) {
          if (entry.getKey().equals(componentConfigMappingEntity.getConfigType())) {
            contains = true;
            componentConfigMappingEntity.setTimestamp(new Date().getTime());
            componentConfigMappingEntity.setVersionTag(entry.getValue().getVersionTag());
            if (persisted) {
              componentConfigMappingDAO.merge(componentConfigMappingEntity);
            }
           }
         }
      }
 
      if (!contains) {
        ComponentConfigMappingEntity newEntity = new ComponentConfigMappingEntity();
        newEntity.setClusterId(desiredStateEntity.getClusterId());
        newEntity.setServiceName(desiredStateEntity.getServiceName());
        newEntity.setComponentName(desiredStateEntity.getComponentName());
        newEntity.setConfigType(entry.getKey());
        newEntity.setVersionTag(entry.getValue().getVersionTag());
        newEntity.setTimestamp(new Date().getTime());
        newEntity.setServiceComponentDesiredStateEntity(desiredStateEntity);
        desiredStateEntity.getComponentConfigMappingEntities().add(newEntity);
        if (!contains) {
          ComponentConfigMappingEntity newEntity = new ComponentConfigMappingEntity();
          newEntity.setClusterId(desiredStateEntity.getClusterId());
          newEntity.setServiceName(desiredStateEntity.getServiceName());
          newEntity.setComponentName(desiredStateEntity.getComponentName());
          newEntity.setConfigType(entry.getKey());
          newEntity.setVersionTag(entry.getValue().getVersionTag());
          newEntity.setTimestamp(new Date().getTime());
          newEntity.setServiceComponentDesiredStateEntity(desiredStateEntity);
          desiredStateEntity.getComponentConfigMappingEntities().add(newEntity);
 
      }
        }
 
 
      this.desiredConfigs.put(entry.getKey(), entry.getValue().getVersionTag());
        this.desiredConfigs.put(entry.getKey(), entry.getValue().getVersionTag());
      }

      saveIfPersisted();
    } finally {
      readWriteLock.writeLock().unlock();
     }
 
    saveIfPersisted();
   }
 
   @Override
  public synchronized StackId getDesiredStackVersion() {
    return gson.fromJson(desiredStateEntity.getDesiredStackVersion(), StackId.class);
  public StackId getDesiredStackVersion() {
    readWriteLock.readLock().lock();
    try {
      return gson.fromJson(desiredStateEntity.getDesiredStackVersion(), StackId.class);
    } finally {
      readWriteLock.readLock().unlock();
    }

   }
 
   @Override
  public synchronized void setDesiredStackVersion(StackId stackVersion) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting DesiredStackVersion of Service"
          + ", clusterName=" + service.getCluster().getClusterName()
          + ", clusterId=" + service.getCluster().getClusterId()
          + ", serviceName=" + service.getName()
          + ", serviceComponentName=" + getName()
          + ", oldDesiredStackVersion=" + getDesiredStackVersion()
          + ", newDesiredStackVersion=" + stackVersion);
  public void setDesiredStackVersion(StackId stackVersion) {
    readWriteLock.writeLock().lock();
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Setting DesiredStackVersion of Service"
            + ", clusterName=" + service.getCluster().getClusterName()
            + ", clusterId=" + service.getCluster().getClusterId()
            + ", serviceName=" + service.getName()
            + ", serviceComponentName=" + getName()
            + ", oldDesiredStackVersion=" + getDesiredStackVersion()
            + ", newDesiredStackVersion=" + stackVersion);
      }
      desiredStateEntity.setDesiredStackVersion(gson.toJson(stackVersion));
      saveIfPersisted();
    } finally {
      readWriteLock.writeLock().unlock();
     }
    desiredStateEntity.setDesiredStackVersion(gson.toJson(stackVersion));
    saveIfPersisted();

   }
 
   @Override
  public synchronized ServiceComponentResponse convertToResponse() {
    ServiceComponentResponse r  = new ServiceComponentResponse(
        getClusterId(), service.getCluster().getClusterName(),
        service.getName(), getName(), this.desiredConfigs,
        getDesiredStackVersion().getStackId(),
        getDesiredState().toString());
    return r;
  public ServiceComponentResponse convertToResponse() {
    readWriteLock.readLock().lock();
    try {
      ServiceComponentResponse r = new ServiceComponentResponse(
          getClusterId(), service.getCluster().getClusterName(),
          service.getName(), getName(), this.desiredConfigs,
          getDesiredStackVersion().getStackId(),
          getDesiredState().toString());
      return r;
    } finally {
      readWriteLock.readLock().unlock();
    }

   }
 
   @Override
   public String getClusterName() {
    return service.getCluster().getClusterName();
    readWriteLock.readLock().lock();
    try {
      return service.getCluster().getClusterName();
    } finally {
      readWriteLock.readLock().unlock();
    }

   }
 
   @Override
  public synchronized void debugDump(StringBuilder sb) {
    sb.append("ServiceComponent={ serviceComponentName=" + getName()
        + ", clusterName=" + service.getCluster().getClusterName()
        + ", clusterId=" + service.getCluster().getClusterId()
        + ", serviceName=" + service.getName()
        + ", desiredStackVersion=" + getDesiredStackVersion()
        + ", desiredState=" + getDesiredState().toString()
        + ", hostcomponents=[ ");
    boolean first = true;
    for(ServiceComponentHost sch : hostComponents.values()) {
      if (!first) {
        sb.append(" , ");
        first = false;
  public void debugDump(StringBuilder sb) {
    readWriteLock.readLock().lock();
    try {
      sb.append("ServiceComponent={ serviceComponentName=" + getName()
          + ", clusterName=" + service.getCluster().getClusterName()
          + ", clusterId=" + service.getCluster().getClusterId()
          + ", serviceName=" + service.getName()
          + ", desiredStackVersion=" + getDesiredStackVersion()
          + ", desiredState=" + getDesiredState().toString()
          + ", hostcomponents=[ ");
      boolean first = true;
      for (ServiceComponentHost sch : hostComponents.values()) {
        if (!first) {
          sb.append(" , ");
          first = false;
        }
        sb.append("\n        ");
        sch.debugDump(sb);
        sb.append(" ");
       }
      sb.append("\n        ");
      sch.debugDump(sb);
      sb.append(" ");
      sb.append(" ] }");
    } finally {
      readWriteLock.readLock().unlock();
     }
    sb.append(" ] }");

   }
 
   @Override
  public synchronized boolean isPersisted() {
  public boolean isPersisted() {
    readWriteLock.readLock().lock();
    try {
       return persisted;
    } finally {
      readWriteLock.readLock().unlock();
    }

   }
 
   @Override
  public synchronized void persist() {
    if (!persisted) {
      persistEntities();
      refresh();
      service.refresh();
      persisted = true;
    } else {
      saveIfPersisted();
  public void persist() {
    readWriteLock.writeLock().lock();
    try {
      if (!persisted) {
        persistEntities();
        refresh();
        service.refresh();
        persisted = true;
      } else {
        saveIfPersisted();
      }
    } finally {
      readWriteLock.writeLock().unlock();
     }

   }
 
   @Transactional
@@ -424,23 +542,35 @@ public class ServiceComponentImpl implements ServiceComponent {
 
   @Override
   @Transactional
  public synchronized void refresh() {
    if (isPersisted()) {
      ServiceComponentDesiredStateEntityPK pk = new ServiceComponentDesiredStateEntityPK();
      pk.setComponentName(getName());
      pk.setClusterId(getClusterId());
      pk.setServiceName(getServiceName());
      // TODO: desiredStateEntity is assigned in unsynchronized way, may be a bug
      desiredStateEntity = serviceComponentDesiredStateDAO.findByPK(pk);
      serviceComponentDesiredStateDAO.refresh(desiredStateEntity);
  public void refresh() {
    readWriteLock.writeLock().lock();
    try {
      if (isPersisted()) {
        ServiceComponentDesiredStateEntityPK pk = new ServiceComponentDesiredStateEntityPK();
        pk.setComponentName(getName());
        pk.setClusterId(getClusterId());
        pk.setServiceName(getServiceName());
        // TODO: desiredStateEntity is assigned in unway, may be a bug
        desiredStateEntity = serviceComponentDesiredStateDAO.findByPK(pk);
        serviceComponentDesiredStateDAO.refresh(desiredStateEntity);
      }
    } finally {
      readWriteLock.writeLock().unlock();
     }

   }
 
   @Transactional
  private synchronized void saveIfPersisted() {
    if (isPersisted()) {
      serviceComponentDesiredStateDAO.merge(desiredStateEntity);
  private void saveIfPersisted() {
    readWriteLock.writeLock().lock();
    try {
      if (isPersisted()) {
        serviceComponentDesiredStateDAO.merge(desiredStateEntity);
      }
    } finally {
      readWriteLock.writeLock().unlock();
     }

   }
 
   @Override
@@ -449,95 +579,125 @@ public class ServiceComponentImpl implements ServiceComponent {
   }
 
   @Override
  public synchronized boolean canBeRemoved() {
    if (!getDesiredState().isRemovableState()) {
      return false;
    }

    for (ServiceComponentHost sch : hostComponents.values()) {
      if (!sch.canBeRemoved()) {
        LOG.warn("Found non removable hostcomponent when trying to"
            + " delete service component"
            + ", clusterName=" + getClusterName()
            + ", serviceName=" + getServiceName()
            + ", componentName=" + getName()
            + ", hostname=" + sch.getHostName());
  public boolean canBeRemoved() {
    readWriteLock.readLock().lock();
    try {
      if (!getDesiredState().isRemovableState()) {
         return false;
       }

      for (ServiceComponentHost sch : hostComponents.values()) {
        if (!sch.canBeRemoved()) {
          LOG.warn("Found non removable hostcomponent when trying to"
              + " delete service component"
              + ", clusterName=" + getClusterName()
              + ", serviceName=" + getServiceName()
              + ", componentName=" + getName()
              + ", hostname=" + sch.getHostName());
          return false;
        }
      }
      return true;
    } finally {
      readWriteLock.readLock().unlock();
     }
    return true;

   }
 
   @Override
   @Transactional
  public synchronized void deleteAllServiceComponentHosts()
  public void deleteAllServiceComponentHosts()
       throws AmbariException {
    LOG.info("Deleting all servicecomponenthosts for component"
        + ", clusterName=" + getClusterName()
        + ", serviceName=" + getServiceName()
        + ", componentName=" + getName());
    for (ServiceComponentHost sch : hostComponents.values()) {
      if (!sch.canBeRemoved()) {
        throw new AmbariException("Found non removable hostcomponent "
            + " when trying to delete"
            + " all hostcomponents from servicecomponent"
            + ", clusterName=" + getClusterName()
            + ", serviceName=" + getServiceName()
            + ", componentName=" + getName()
            + ", hostname=" + sch.getHostName());
    readWriteLock.writeLock().lock();
    try {
      LOG.info("Deleting all servicecomponenthosts for component"
          + ", clusterName=" + getClusterName()
          + ", serviceName=" + getServiceName()
          + ", componentName=" + getName());
      for (ServiceComponentHost sch : hostComponents.values()) {
        if (!sch.canBeRemoved()) {
          throw new AmbariException("Found non removable hostcomponent "
              + " when trying to delete"
              + " all hostcomponents from servicecomponent"
              + ", clusterName=" + getClusterName()
              + ", serviceName=" + getServiceName()
              + ", componentName=" + getName()
              + ", hostname=" + sch.getHostName());
        }
      }

      for (ServiceComponentHost serviceComponentHost : hostComponents.values()) {
        serviceComponentHost.delete();
       }
    }
 
    for (ServiceComponentHost serviceComponentHost : hostComponents.values()) {
      serviceComponentHost.delete();
      hostComponents.clear();
    } finally {
      readWriteLock.writeLock().unlock();
     }
 
    hostComponents.clear();
   }
 
   @Override
  public synchronized void deleteServiceComponentHosts(String hostname)
  public void deleteServiceComponentHosts(String hostname)
       throws AmbariException {
    ServiceComponentHost sch = getServiceComponentHost(hostname);
    LOG.info("Deleting servicecomponenthost for cluster"
        + ", clusterName=" + getClusterName()
        + ", serviceName=" + getServiceName()
        + ", componentName=" + getName()
        + ", hostname=" + sch.getHostName());
    if (!sch.canBeRemoved()) {
      throw new AmbariException("Could not delete hostcomponent from cluster"
    readWriteLock.writeLock().lock();
    try {
      ServiceComponentHost sch = getServiceComponentHost(hostname);
      LOG.info("Deleting servicecomponenthost for cluster"
           + ", clusterName=" + getClusterName()
           + ", serviceName=" + getServiceName()
           + ", componentName=" + getName()
           + ", hostname=" + sch.getHostName());
      if (!sch.canBeRemoved()) {
        throw new AmbariException("Could not delete hostcomponent from cluster"
            + ", clusterName=" + getClusterName()
            + ", serviceName=" + getServiceName()
            + ", componentName=" + getName()
            + ", hostname=" + sch.getHostName());
      }
      sch.delete();
      hostComponents.remove(hostname);

      // FIXME need a better approach of caching components by host
      ClusterImpl clusterImpl = (ClusterImpl) service.getCluster();
      clusterImpl.removeServiceComponentHost(sch);
    } finally {
      readWriteLock.writeLock().unlock();
     }
    sch.delete();
    hostComponents.remove(hostname);
 
    // FIXME need a better approach of caching components by host
    ClusterImpl clusterImpl = (ClusterImpl) service.getCluster();
    clusterImpl.removeServiceComponentHost(sch);
   }
 
   @Override
  public synchronized void deleteDesiredConfigs(Set<String> configTypes) {
    componentConfigMappingDAO.removeByType(configTypes);
    for (String configType : configTypes) {
      desiredConfigs.remove(configType);
  public void deleteDesiredConfigs(Set<String> configTypes) {
    readWriteLock.writeLock().lock();
    try {
      componentConfigMappingDAO.removeByType(configTypes);
      for (String configType : configTypes) {
        desiredConfigs.remove(configType);
      }
    } finally {
      readWriteLock.writeLock().unlock();
     }

   }
 
   @Override
   @Transactional
  public synchronized void delete() throws AmbariException {
    deleteAllServiceComponentHosts();
  public void delete() throws AmbariException {
    readWriteLock.writeLock().lock();
    try {
      deleteAllServiceComponentHosts();

      if (persisted) {
        removeEntities();
        persisted = false;
      }
 
    if (persisted) {
      removeEntities();
      persisted = false;
      desiredConfigs.clear();
    } finally {
      readWriteLock.writeLock().unlock();
     }
 
    desiredConfigs.clear();
   }
 
   @Transactional
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/state/ServiceImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/state/ServiceImpl.java
index 97585269ca..35c680f484 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/state/ServiceImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/state/ServiceImpl.java
@@ -20,6 +20,8 @@ package org.apache.ambari.server.state;
 
 import java.util.*;
 import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
 
 import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.ServiceComponentNotFoundException;
@@ -39,6 +41,7 @@ import com.google.inject.persist.Transactional;
 
 
 public class ServiceImpl implements Service {
  private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
 
   private ClusterServiceEntity serviceEntity;
   private ServiceDesiredStateEntity serviceDesiredStateEntity;
@@ -141,7 +144,7 @@ public class ServiceImpl implements Service {
 
   @Override
   public String getName() {
      return serviceEntity.getServiceName();
    return serviceEntity.getServiceName();
   }
 
   @Override
@@ -150,172 +153,244 @@ public class ServiceImpl implements Service {
   }
 
   @Override
  public synchronized Map<String, ServiceComponent> getServiceComponents() {
    return Collections.unmodifiableMap(components);
  public Map<String, ServiceComponent> getServiceComponents() {
    readWriteLock.readLock().lock();
    try {
      return Collections.unmodifiableMap(components);
    } finally {
      readWriteLock.readLock().unlock();
    }

   }
 
   @Override
  public synchronized void addServiceComponents(
  public void addServiceComponents(
       Map<String, ServiceComponent> components) throws AmbariException {
    for (ServiceComponent sc : components.values()) {
      addServiceComponent(sc);
    readWriteLock.writeLock().lock();
    try {
      for (ServiceComponent sc : components.values()) {
        addServiceComponent(sc);
      }
    } finally {
      readWriteLock.writeLock().unlock();
     }

   }
 
   @Override
  public synchronized void addServiceComponent(ServiceComponent component)
  public void addServiceComponent(ServiceComponent component)
       throws AmbariException {
    // TODO validation
    if (LOG.isDebugEnabled()) {
      LOG.debug("Adding a ServiceComponent to Service"
          + ", clusterName=" + cluster.getClusterName()
          + ", clusterId=" + cluster.getClusterId()
          + ", serviceName=" + getName()
          + ", serviceComponentName=" + component.getName());
    }
    if (components.containsKey(component.getName())) {
      throw new AmbariException("Cannot add duplicate ServiceComponent"
          + ", clusterName=" + cluster.getClusterName()
          + ", clusterId=" + cluster.getClusterId()
          + ", serviceName=" + getName()
          + ", serviceComponentName=" + component.getName());
    readWriteLock.writeLock().lock();
    try {
      // TODO validation
      if (LOG.isDebugEnabled()) {
        LOG.debug("Adding a ServiceComponent to Service"
            + ", clusterName=" + cluster.getClusterName()
            + ", clusterId=" + cluster.getClusterId()
            + ", serviceName=" + getName()
            + ", serviceComponentName=" + component.getName());
      }
      if (components.containsKey(component.getName())) {
        throw new AmbariException("Cannot add duplicate ServiceComponent"
            + ", clusterName=" + cluster.getClusterName()
            + ", clusterId=" + cluster.getClusterId()
            + ", serviceName=" + getName()
            + ", serviceComponentName=" + component.getName());
      }
      this.components.put(component.getName(), component);
    } finally {
      readWriteLock.writeLock().unlock();
     }
    this.components.put(component.getName(), component);

   }
 
   @Override
  public synchronized ServiceComponent addServiceComponent(
  public ServiceComponent addServiceComponent(
       String serviceComponentName) throws AmbariException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Adding a ServiceComponent to Service"
          + ", clusterName=" + cluster.getClusterName()
          + ", clusterId=" + cluster.getClusterId()
          + ", serviceName=" + getName()
          + ", serviceComponentName=" + serviceComponentName);
    }
    if (components.containsKey(serviceComponentName)) {
      throw new AmbariException("Cannot add duplicate ServiceComponent"
          + ", clusterName=" + cluster.getClusterName()
          + ", clusterId=" + cluster.getClusterId()
          + ", serviceName=" + getName()
          + ", serviceComponentName=" + serviceComponentName);
    readWriteLock.writeLock().lock();
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Adding a ServiceComponent to Service"
            + ", clusterName=" + cluster.getClusterName()
            + ", clusterId=" + cluster.getClusterId()
            + ", serviceName=" + getName()
            + ", serviceComponentName=" + serviceComponentName);
      }
      if (components.containsKey(serviceComponentName)) {
        throw new AmbariException("Cannot add duplicate ServiceComponent"
            + ", clusterName=" + cluster.getClusterName()
            + ", clusterId=" + cluster.getClusterId()
            + ", serviceName=" + getName()
            + ", serviceComponentName=" + serviceComponentName);
      }
      ServiceComponent component = serviceComponentFactory.createNew(this, serviceComponentName);
      this.components.put(component.getName(), component);
      return component;
    } finally {
      readWriteLock.writeLock().unlock();
     }
    ServiceComponent component = serviceComponentFactory.createNew(this, serviceComponentName);
    this.components.put(component.getName(), component);
    return component;

   }
 
   @Override
   public ServiceComponent getServiceComponent(String componentName)
       throws AmbariException {
    if (!components.containsKey(componentName)) {
      throw new ServiceComponentNotFoundException(cluster.getClusterName(),
          getName(),
          componentName);
    readWriteLock.readLock().lock();
    try {
      if (!components.containsKey(componentName)) {
        throw new ServiceComponentNotFoundException(cluster.getClusterName(),
            getName(),
            componentName);
      }
      return this.components.get(componentName);
    } finally {
      readWriteLock.readLock().unlock();
     }
    return this.components.get(componentName);

   }
 
   @Override
  public synchronized State getDesiredState() {
    return this.serviceDesiredStateEntity.getDesiredState();
  public State getDesiredState() {
    readWriteLock.readLock().lock();
    try {
      return this.serviceDesiredStateEntity.getDesiredState();
    } finally {
      readWriteLock.readLock().unlock();
    }

   }
 
   @Override
  public synchronized void setDesiredState(State state) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting DesiredState of Service"
          + ", clusterName=" + cluster.getClusterName()
          + ", clusterId=" + cluster.getClusterId()
          + ", serviceName=" + getName()
          + ", oldDesiredState=" + this.getDesiredState()
          + ", newDesiredState=" + state);
  public void setDesiredState(State state) {
    readWriteLock.writeLock().lock();
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Setting DesiredState of Service"
            + ", clusterName=" + cluster.getClusterName()
            + ", clusterId=" + cluster.getClusterId()
            + ", serviceName=" + getName()
            + ", oldDesiredState=" + this.getDesiredState()
            + ", newDesiredState=" + state);
      }
      this.serviceDesiredStateEntity.setDesiredState(state);
      saveIfPersisted();
    } finally {
      readWriteLock.writeLock().unlock();
     }
    this.serviceDesiredStateEntity.setDesiredState(state);
    saveIfPersisted();

   }
 
   @Override
  public synchronized Map<String, Config> getDesiredConfigs() {
    Map<String, Config> map = new HashMap<String, Config>();
    for (Entry<String, String> entry : desiredConfigs.entrySet()) {
      Config config = cluster.getConfig(entry.getKey(), entry.getValue());
      if (null != config) {
        map.put(entry.getKey(), config);
      } else {
        // FIXME this is an error - should throw a proper exception
        throw new RuntimeException("Found an invalid config"
            + ", clusterName=" + getCluster().getClusterName()
            + ", serviceName=" + getName()
            + ", configType=" + entry.getKey()
            + ", configVersionTag=" + entry.getValue());
  public Map<String, Config> getDesiredConfigs() {
    readWriteLock.readLock().lock();
    try {
      Map<String, Config> map = new HashMap<String, Config>();
      for (Entry<String, String> entry : desiredConfigs.entrySet()) {
        Config config = cluster.getConfig(entry.getKey(), entry.getValue());
        if (null != config) {
          map.put(entry.getKey(), config);
        } else {
          // FIXME this is an error - should throw a proper exception
          throw new RuntimeException("Found an invalid config"
              + ", clusterName=" + getCluster().getClusterName()
              + ", serviceName=" + getName()
              + ", configType=" + entry.getKey()
              + ", configVersionTag=" + entry.getValue());
        }
       }
      return Collections.unmodifiableMap(map);
    } finally {
      readWriteLock.readLock().unlock();
     }
    return Collections.unmodifiableMap(map);

   }
 
   @Override
  public synchronized void updateDesiredConfigs(Map<String, Config> configs) {
  public void updateDesiredConfigs(Map<String, Config> configs) {

    readWriteLock.writeLock().lock();
    try {
      for (Entry<String, Config> entry : configs.entrySet()) {
        boolean contains = false;

        for (ServiceConfigMappingEntity serviceConfigMappingEntity : serviceEntity.getServiceConfigMappings()) {
          if (entry.getKey().equals(serviceConfigMappingEntity.getConfigType())) {
            contains = true;
            serviceConfigMappingEntity.setTimestamp(new Date().getTime());
            serviceConfigMappingEntity.setVersionTag(entry.getValue().getVersionTag());
          }
        }
 
    for (Entry<String,Config> entry : configs.entrySet()) {
      boolean contains = false;
        if (!contains) {
          ServiceConfigMappingEntity newEntity = new ServiceConfigMappingEntity();
          newEntity.setClusterId(serviceEntity.getClusterId());
          newEntity.setServiceName(serviceEntity.getServiceName());
          newEntity.setConfigType(entry.getKey());
          newEntity.setVersionTag(entry.getValue().getVersionTag());
          newEntity.setTimestamp(new Date().getTime());
          newEntity.setServiceEntity(serviceEntity);
          serviceEntity.getServiceConfigMappings().add(newEntity);
 
      for (ServiceConfigMappingEntity serviceConfigMappingEntity : serviceEntity.getServiceConfigMappings()) {
        if (entry.getKey().equals(serviceConfigMappingEntity.getConfigType())) {
          contains = true;
          serviceConfigMappingEntity.setTimestamp(new Date().getTime());
          serviceConfigMappingEntity.setVersionTag(entry.getValue().getVersionTag());
         }
      }
 
      if (!contains) {
        ServiceConfigMappingEntity newEntity = new ServiceConfigMappingEntity();
        newEntity.setClusterId(serviceEntity.getClusterId());
        newEntity.setServiceName(serviceEntity.getServiceName());
        newEntity.setConfigType(entry.getKey());
        newEntity.setVersionTag(entry.getValue().getVersionTag());
        newEntity.setTimestamp(new Date().getTime());
        newEntity.setServiceEntity(serviceEntity);
        serviceEntity.getServiceConfigMappings().add(newEntity);
 
        this.desiredConfigs.put(entry.getKey(), entry.getValue().getVersionTag());
       }
 

      this.desiredConfigs.put(entry.getKey(), entry.getValue().getVersionTag());
      saveIfPersisted();
    } finally {
      readWriteLock.writeLock().unlock();
     }
 
    saveIfPersisted();
 
   }
 
   @Override
  public synchronized StackId getDesiredStackVersion() {
    return gson.fromJson(serviceDesiredStateEntity.getDesiredStackVersion(), StackId.class);
  public StackId getDesiredStackVersion() {
    readWriteLock.readLock().lock();
    try {
      return gson.fromJson(serviceDesiredStateEntity.getDesiredStackVersion(), StackId.class);
    } finally {
      readWriteLock.readLock().unlock();
    }

   }
 
   @Override
  public synchronized void setDesiredStackVersion(StackId stackVersion) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting DesiredStackVersion of Service"
          + ", clusterName=" + cluster.getClusterName()
          + ", clusterId=" + cluster.getClusterId()
          + ", serviceName=" + getName()
          + ", oldDesiredStackVersion=" + getDesiredStackVersion()
          + ", newDesiredStackVersion=" + stackVersion);
  public void setDesiredStackVersion(StackId stackVersion) {
    readWriteLock.writeLock().lock();
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Setting DesiredStackVersion of Service"
            + ", clusterName=" + cluster.getClusterName()
            + ", clusterId=" + cluster.getClusterId()
            + ", serviceName=" + getName()
            + ", oldDesiredStackVersion=" + getDesiredStackVersion()
            + ", newDesiredStackVersion=" + stackVersion);
      }
      serviceDesiredStateEntity.setDesiredStackVersion(gson.toJson(stackVersion));
      saveIfPersisted();
    } finally {
      readWriteLock.writeLock().unlock();
     }
    serviceDesiredStateEntity.setDesiredStackVersion(gson.toJson(stackVersion));
    saveIfPersisted();

   }
 
   @Override
  public synchronized ServiceResponse convertToResponse() {
    ServiceResponse r = new ServiceResponse(cluster.getClusterId(),
        cluster.getClusterName(),
        getName(),
        desiredConfigs,
        getDesiredStackVersion().getStackId(),
        getDesiredState().toString());
    return r;
  public ServiceResponse convertToResponse() {
    readWriteLock.readLock().lock();
    try {
      ServiceResponse r = new ServiceResponse(cluster.getClusterId(),
          cluster.getClusterName(),
          getName(),
          desiredConfigs,
          getDesiredStackVersion().getStackId(),
          getDesiredState().toString());
      return r;
    } finally {
      readWriteLock.readLock().unlock();
    }

   }
 
   @Override
@@ -324,54 +399,72 @@ public class ServiceImpl implements Service {
   }
 
   @Override
  public synchronized void debugDump(StringBuilder sb) {
    sb.append("Service={ serviceName=" + getName()
        + ", clusterName=" + cluster.getClusterName()
        + ", clusterId=" + cluster.getClusterId()
        + ", desiredStackVersion=" + getDesiredStackVersion()
        + ", desiredState=" + getDesiredState().toString()
        + ", configs=[");
    boolean first = true;
    if (desiredConfigs != null) {
      for (Entry<String, String> entry : desiredConfigs.entrySet()) {
  public void debugDump(StringBuilder sb) {
    readWriteLock.readLock().lock();
    try {
      sb.append("Service={ serviceName=" + getName()
          + ", clusterName=" + cluster.getClusterName()
          + ", clusterId=" + cluster.getClusterId()
          + ", desiredStackVersion=" + getDesiredStackVersion()
          + ", desiredState=" + getDesiredState().toString()
          + ", configs=[");
      boolean first = true;
      if (desiredConfigs != null) {
        for (Entry<String, String> entry : desiredConfigs.entrySet()) {
          if (!first) {
            sb.append(" , ");
          }
          first = false;
          sb.append("{ Config type=" + entry.getKey()
              + ", versionTag=" + entry.getValue() + "}");
        }
      }
      sb.append("], components=[ ");

      first = true;
      for (ServiceComponent sc : components.values()) {
         if (!first) {
           sb.append(" , ");
         }
         first = false;
        sb.append("{ Config type=" + entry.getKey()
            + ", versionTag=" + entry.getValue() + "}");
        sb.append("\n      ");
        sc.debugDump(sb);
        sb.append(" ");
       }
      sb.append(" ] }");
    } finally {
      readWriteLock.readLock().unlock();
     }
    sb.append("], components=[ ");
 
    first = true;
    for(ServiceComponent sc : components.values()) {
      if (!first) {
        sb.append(" , ");
      }
      first = false;
      sb.append("\n      ");
      sc.debugDump(sb);
      sb.append(" ");
    }
    sb.append(" ] }");
   }
 
   @Override
  public synchronized boolean isPersisted() {
  public boolean isPersisted() {
    readWriteLock.readLock().lock();
    try {
       return persisted;
    } finally {
      readWriteLock.readLock().unlock();
    }

   }
 
   @Override
  public synchronized void persist() {
    if (!persisted) {
      persistEntities();
      refresh();
      cluster.refresh();
      persisted = true;
    } else {
      saveIfPersisted();
  public void persist() {
    readWriteLock.writeLock().lock();
    try {
      if (!persisted) {
        persistEntities();
        refresh();
        cluster.refresh();
        persisted = true;
      } else {
        saveIfPersisted();
      }
    } finally {
      readWriteLock.writeLock().unlock();
     }

   }
 
   @Transactional
@@ -398,78 +491,102 @@ public class ServiceImpl implements Service {
 
   @Override
   @Transactional
  public synchronized void refresh() {
    if (isPersisted()) {
      ClusterServiceEntityPK pk = new ClusterServiceEntityPK();
      pk.setClusterId(getClusterId());
      pk.setServiceName(getName());
      serviceEntity = clusterServiceDAO.findByPK(pk);
      serviceDesiredStateEntity = serviceEntity.getServiceDesiredStateEntity();
      clusterServiceDAO.refresh(serviceEntity);
      serviceDesiredStateDAO.refresh(serviceDesiredStateEntity);
  public void refresh() {
    readWriteLock.writeLock().lock();
    try {
      if (isPersisted()) {
        ClusterServiceEntityPK pk = new ClusterServiceEntityPK();
        pk.setClusterId(getClusterId());
        pk.setServiceName(getName());
        serviceEntity = clusterServiceDAO.findByPK(pk);
        serviceDesiredStateEntity = serviceEntity.getServiceDesiredStateEntity();
        clusterServiceDAO.refresh(serviceEntity);
        serviceDesiredStateDAO.refresh(serviceDesiredStateEntity);
      }
    } finally {
      readWriteLock.writeLock().unlock();
     }

   }
 
   @Override
  public synchronized boolean canBeRemoved() {
    if (!getDesiredState().isRemovableState()) {
      return false;
    }

    for (ServiceComponent sc : components.values()) {
      if (!sc.canBeRemoved()) {
        LOG.warn("Found non removable component when trying to delete service"
            + ", clusterName=" + cluster.getClusterName()
            + ", serviceName=" + getName()
            + ", componentName=" + sc.getName());
  public boolean canBeRemoved() {
    readWriteLock.readLock().lock();
    try {
      if (!getDesiredState().isRemovableState()) {
         return false;
       }

      for (ServiceComponent sc : components.values()) {
        if (!sc.canBeRemoved()) {
          LOG.warn("Found non removable component when trying to delete service"
              + ", clusterName=" + cluster.getClusterName()
              + ", serviceName=" + getName()
              + ", componentName=" + sc.getName());
          return false;
        }
      }
      return true;
    } finally {
      readWriteLock.readLock().unlock();
     }
    return true;

   }
 
   @Override
   @Transactional
  public synchronized void deleteAllComponents() throws AmbariException {
    LOG.info("Deleting all components for service"
        + ", clusterName=" + cluster.getClusterName()
        + ", serviceName=" + getName());
    // FIXME check dependencies from meta layer
    for (ServiceComponent component : components.values()) {
      if (!component.canBeRemoved()) {
        throw new AmbariException("Found non removable component when trying to"
            + " delete all components from service"
            + ", clusterName=" + cluster.getClusterName()
            + ", serviceName=" + getName()
            + ", componentName=" + component.getName());
  public void deleteAllComponents() throws AmbariException {
    readWriteLock.writeLock().lock();
    try {
      LOG.info("Deleting all components for service"
          + ", clusterName=" + cluster.getClusterName()
          + ", serviceName=" + getName());
      // FIXME check dependencies from meta layer
      for (ServiceComponent component : components.values()) {
        if (!component.canBeRemoved()) {
          throw new AmbariException("Found non removable component when trying to"
              + " delete all components from service"
              + ", clusterName=" + cluster.getClusterName()
              + ", serviceName=" + getName()
              + ", componentName=" + component.getName());
        }
      }

      for (ServiceComponent serviceComponent : components.values()) {
        serviceComponent.delete();
       }
    }
 
    for (ServiceComponent serviceComponent : components.values()) {
      serviceComponent.delete();
      components.clear();
    } finally {
      readWriteLock.writeLock().unlock();
     }
 
    components.clear();
   }
 
   @Override
  public synchronized void deleteServiceComponent(String componentName)
  public void deleteServiceComponent(String componentName)
       throws AmbariException {
    ServiceComponent component = getServiceComponent(componentName);
    LOG.info("Deleting servicecomponent for cluster"
        + ", clusterName=" + cluster.getClusterName()
        + ", serviceName=" + getName()
        + ", componentName=" + componentName);
    // FIXME check dependencies from meta layer
    if (!component.canBeRemoved()) {
      throw new AmbariException("Could not delete component from cluster"
    readWriteLock.writeLock().lock();
    try {
      ServiceComponent component = getServiceComponent(componentName);
      LOG.info("Deleting servicecomponent for cluster"
           + ", clusterName=" + cluster.getClusterName()
           + ", serviceName=" + getName()
           + ", componentName=" + componentName);
      // FIXME check dependencies from meta layer
      if (!component.canBeRemoved()) {
        throw new AmbariException("Could not delete component from cluster"
            + ", clusterName=" + cluster.getClusterName()
            + ", serviceName=" + getName()
            + ", componentName=" + componentName);
      }

      component.delete();
      components.remove(componentName);
    } finally {
      readWriteLock.writeLock().unlock();
     }
 
    component.delete();
    components.remove(componentName);
   }
 
   @Override
@@ -479,15 +596,21 @@ public class ServiceImpl implements Service {
 
   @Override
   @Transactional
  public synchronized void delete() throws AmbariException {
    deleteAllComponents();
  public void delete() throws AmbariException {
    readWriteLock.writeLock().lock();
    try {
      deleteAllComponents();

      if (persisted) {
        removeEntities();
        persisted = false;
      }
 
    if (persisted) {
      removeEntities();
      persisted = false;
      desiredConfigs.clear();
    } finally {
      readWriteLock.writeLock().unlock();
     }
 
    desiredConfigs.clear();
   }
 
   @Transactional
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/state/host/HostImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/state/host/HostImpl.java
index cb27c95b25..d9264f1895 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/state/host/HostImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/state/host/HostImpl.java
@@ -355,6 +355,9 @@ public class HostImpl implements Host {
     }
   }
 
  /**
   * @param hostInfo
   */
   @Override
   public void importHostInfo(HostInfo hostInfo) {
     try {
@@ -457,17 +460,26 @@ public class HostImpl implements Host {
     }
   }
 
  /**
   * @param hostInfo
   */
   @Override
   public void setLastAgentEnv(AgentEnv env) {
    lastAgentEnv = env;
    writeLock.lock();
    try {
      lastAgentEnv = env;
    } finally {
      writeLock.unlock();
    }

   }
   
   @Override
   public AgentEnv getLastAgentEnv() {
    return lastAgentEnv;
    readLock.lock();
    try {
      return lastAgentEnv;
    } finally {
      readLock.unlock();
    }

   }
 
   @Override
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/state/svccomphost/ServiceComponentHostImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/state/svccomphost/ServiceComponentHostImpl.java
index 0fe064ae5b..b5e157ddac 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/state/svccomphost/ServiceComponentHostImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/state/svccomphost/ServiceComponentHostImpl.java
@@ -64,8 +64,9 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
 
   // FIXME need more debug logs
 
  private final Lock readLock;
  private final Lock writeLock;
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private final Lock readLock = readWriteLock.readLock();
  private final Lock writeLock = readWriteLock.writeLock();
 
   private final ServiceComponent serviceComponent;
   private final Host host;
@@ -600,9 +601,6 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
       this.stateMachine = daemonStateMachineFactory.make(this);
     }
 
    ReadWriteLock rwLock = new ReentrantReadWriteLock();
    this.readLock = rwLock.readLock();
    this.writeLock = rwLock.writeLock();
     this.serviceComponent = serviceComponent;
 
     stateEntity = new HostComponentStateEntity();
@@ -641,9 +639,6 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
                                   @Assisted HostComponentDesiredStateEntity desiredStateEntity,
                                   Injector injector) {
     injector.injectMembers(this);
    ReadWriteLock rwLock = new ReentrantReadWriteLock();
    this.readLock = rwLock.readLock();
    this.writeLock = rwLock.writeLock();
     this.serviceComponent = serviceComponent;
 
 
@@ -678,8 +673,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
 
   @Override
   public State getState() {
    readLock.lock();
     try {
      readLock.lock();
       return stateMachine.getCurrentState();
     }
     finally {
@@ -689,8 +684,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
 
   @Override
   public void setState(State state) {
    writeLock.lock();
     try {
      writeLock.lock();
       stateMachine.setCurrentState(state);
       stateEntity.setCurrentState(state);
       saveIfPersisted();
@@ -746,20 +741,32 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
 
   @Override
   public String getServiceComponentName() {
    return serviceComponent.getName();
    readLock.lock();
    try {
      return serviceComponent.getName();
    } finally {
      readLock.unlock();
    }

   }
 
   @Override
   public String getHostName() {
    return host.getHostName();
    readLock.lock();
    try {
      return host.getHostName();
    } finally {
      readLock.unlock();
    }

   }
 
   /**
    * @return the lastOpStartTime
    */
   public long getLastOpStartTime() {
    readLock.lock();
     try {
      readLock.lock();
       return lastOpStartTime;
     }
     finally {
@@ -771,8 +778,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
    * @param lastOpStartTime the lastOpStartTime to set
    */
   public void setLastOpStartTime(long lastOpStartTime) {
    writeLock.lock();
     try {
      writeLock.lock();
       this.lastOpStartTime = lastOpStartTime;
     }
     finally {
@@ -784,8 +791,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
    * @return the lastOpEndTime
    */
   public long getLastOpEndTime() {
    readLock.lock();
     try {
      readLock.lock();
       return lastOpEndTime;
     }
     finally {
@@ -797,8 +804,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
    * @param lastOpEndTime the lastOpEndTime to set
    */
   public void setLastOpEndTime(long lastOpEndTime) {
    writeLock.lock();
     try {
      writeLock.lock();
       this.lastOpEndTime = lastOpEndTime;
     }
     finally {
@@ -810,8 +817,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
    * @return the lastOpLastUpdateTime
    */
   public long getLastOpLastUpdateTime() {
    readLock.lock();
     try {
      readLock.lock();
       return lastOpLastUpdateTime;
     }
     finally {
@@ -823,8 +830,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
    * @param lastOpLastUpdateTime the lastOpLastUpdateTime to set
    */
   public void setLastOpLastUpdateTime(long lastOpLastUpdateTime) {
    writeLock.lock();
     try {
      writeLock.lock();
       this.lastOpLastUpdateTime = lastOpLastUpdateTime;
     }
     finally {
@@ -834,17 +841,29 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
 
   @Override
   public long getClusterId() {
    return serviceComponent.getClusterId();
    readLock.lock();
    try {
      return serviceComponent.getClusterId();
    } finally {
      readLock.unlock();
    }

   }
 
   @Override
   public String getServiceName() {
    return serviceComponent.getServiceName();
    readLock.lock();
    try {
      return serviceComponent.getServiceName();
    } finally {
      readLock.unlock();
    }

   }
 
   Map<String, String> getConfigVersions() {
    readLock.lock();
     try {
      readLock.lock();
       if (this.configs != null) {
         return Collections.unmodifiableMap(configs);
       } else {
@@ -859,8 +878,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
 
   @Override
   public Map<String, Config> getConfigs() throws AmbariException {
    readLock.lock();
     try {
      readLock.lock();
       Map<String, Config> map = new HashMap<String, Config>();
       Cluster cluster = clusters.getClusterById(getClusterId());
       for (Entry<String, String> entry : configs.entrySet()) {
@@ -879,8 +898,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
 
   @Transactional
   void setConfigs(Map<String, String> configs) {
    writeLock.lock();
     try {
      writeLock.lock();
 
       Set<String> deletedTypes = new HashSet<String>();
       for (String type : this.configs.keySet()) {
@@ -971,8 +990,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
 
   @Override
   public StackId getStackVersion() {
    readLock.lock();
     try {
      readLock.lock();
       return gson.fromJson(stateEntity.getCurrentStackVersion(), StackId.class);
     }
     finally {
@@ -982,8 +1001,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
 
   @Override
   public void setStackVersion(StackId stackVersion) {
    writeLock.lock();
     try {
      writeLock.lock();
       stateEntity.setCurrentStackVersion(gson.toJson(stackVersion));
       saveIfPersisted();
     }
@@ -995,8 +1014,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
 
   @Override
   public State getDesiredState() {
    readLock.lock();
     try {
      readLock.lock();
       return desiredStateEntity.getDesiredState();
     }
     finally {
@@ -1006,8 +1025,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
 
   @Override
   public void setDesiredState(State state) {
    writeLock.lock();
     try {
      writeLock.lock();
       desiredStateEntity.setDesiredState(state);
       saveIfPersisted();
     }
@@ -1018,8 +1037,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
 
   @Override
   public Map<String, String> getDesiredConfigVersionsRecursive() {
    readLock.lock();
     try {
      readLock.lock();
       Map<String, String> fullDesiredConfigVersions =
           new HashMap<String, String>();
       Map<String, Config> desiredConfs = getDesiredConfigs();
@@ -1037,8 +1056,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
   @Override
   public Map<String, Config> getDesiredConfigs() {
     Map<String, Config> map = new HashMap<String, Config>();
    readLock.lock();
     try {
      readLock.lock();
       for (Entry<String, String> entry : desiredConfigs.entrySet()) {
         Config config = clusters.getClusterById(getClusterId()).getConfig(
             entry.getKey(), entry.getValue());
@@ -1067,8 +1086,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
   @Override
   @Transactional
   public void updateDesiredConfigs(Map<String, Config> configs) {
    writeLock.lock();
     try {
      writeLock.lock();
 
       for (Entry<String,Config> entry : configs.entrySet()) {
 
@@ -1109,8 +1128,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
 
   @Override
   public StackId getDesiredStackVersion() {
    readLock.lock();
     try {
      readLock.lock();
       return gson.fromJson(desiredStateEntity.getDesiredStackVersion(), StackId.class);
     }
     finally {
@@ -1120,8 +1139,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
 
   @Override
   public void setDesiredStackVersion(StackId stackVersion) {
    writeLock.lock();
     try {
      writeLock.lock();
       desiredStateEntity.setDesiredStackVersion(gson.toJson(stackVersion));
       saveIfPersisted();
     }
@@ -1132,8 +1151,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
 
   @Override
   public ServiceComponentHostResponse convertToResponse() {
    readLock.lock();
     try {
      readLock.lock();
       ServiceComponentHostResponse r = new ServiceComponentHostResponse(
           serviceComponent.getClusterName(),
           serviceComponent.getServiceName(),
@@ -1157,13 +1176,19 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
 
   @Override
   public String getClusterName() {
    return serviceComponent.getClusterName();
    readLock.lock();
    try {
      return serviceComponent.getClusterName();
    } finally {
      readLock.unlock();
    }

   }
 
   @Override
   public void debugDump(StringBuilder sb) {
    readLock.lock();
     try {
      readLock.lock();
       sb.append("ServiceComponentHost={ hostname=" + getHostName()
           + ", serviceComponentName=" + serviceComponent.getName()
           + ", clusterName=" + serviceComponent.getClusterName()
@@ -1181,8 +1206,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
 
   @Override
   public boolean isPersisted() {
    readLock.lock();
     try {
      readLock.lock();
       return persisted;
     } finally {
       readLock.unlock();
@@ -1191,8 +1216,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
 
   @Override
   public void persist() {
    writeLock.lock();
     try {
      writeLock.lock();
       if (!persisted) {
         persistEntities();
         refresh();
@@ -1235,23 +1260,29 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
 
   @Override
   @Transactional
  public synchronized void refresh() {
    if (isPersisted()) {
      HostComponentStateEntityPK pk = new HostComponentStateEntityPK();
      HostComponentDesiredStateEntityPK dpk = new HostComponentDesiredStateEntityPK();
      pk.setClusterId(getClusterId());
      pk.setComponentName(getServiceComponentName());
      pk.setServiceName(getServiceName());
      pk.setHostName(getHostName());
      dpk.setClusterId(getClusterId());
      dpk.setComponentName(getServiceComponentName());
      dpk.setServiceName(getServiceName());
      dpk.setHostName(getHostName());
      stateEntity = hostComponentStateDAO.findByPK(pk);
      desiredStateEntity = hostComponentDesiredStateDAO.findByPK(dpk);
      hostComponentStateDAO.refresh(stateEntity);
      hostComponentDesiredStateDAO.refresh(desiredStateEntity);
  public void refresh() {
    writeLock.lock();
    try {
      if (isPersisted()) {
        HostComponentStateEntityPK pk = new HostComponentStateEntityPK();
        HostComponentDesiredStateEntityPK dpk = new HostComponentDesiredStateEntityPK();
        pk.setClusterId(getClusterId());
        pk.setComponentName(getServiceComponentName());
        pk.setServiceName(getServiceName());
        pk.setHostName(getHostName());
        dpk.setClusterId(getClusterId());
        dpk.setComponentName(getServiceComponentName());
        dpk.setServiceName(getServiceName());
        dpk.setHostName(getHostName());
        stateEntity = hostComponentStateDAO.findByPK(pk);
        desiredStateEntity = hostComponentDesiredStateDAO.findByPK(dpk);
        hostComponentStateDAO.refresh(stateEntity);
        hostComponentDesiredStateDAO.refresh(desiredStateEntity);
      }
    } finally {
      writeLock.unlock();
     }

   }
 
   @Transactional
@@ -1263,9 +1294,9 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
   }
 
   @Override
  public synchronized boolean canBeRemoved() {
  public boolean canBeRemoved() {
    readLock.lock();
     try {
      readLock.lock();
 
       return (getDesiredState().isRemovableState() &&
               getState().isRemovableState());
@@ -1277,8 +1308,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
 
   @Override
   public void deleteDesiredConfigs(Set<String> configTypes) {
    writeLock.lock();
     try {
      writeLock.lock();
       hostComponentDesiredConfigMappingDAO.removeByType(configTypes);
       for (String configType : configTypes) {
         desiredConfigs.remove(configType);
@@ -1290,8 +1321,8 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
 
   @Override
   public void delete() {
    writeLock.lock();
     try {
      writeLock.lock();
       if (persisted) {
         removeEntities();
         persisted = false;
@@ -1324,35 +1355,53 @@ public class ServiceComponentHostImpl implements ServiceComponentHost {
   
   @Override
   public void updateActualConfigs(Map<String, Map<String, String>> configTags) {
    actualConfigs = new HashMap<String, DesiredConfig>();
    
    String hostName = getHostName();
    
    for (Entry<String, Map<String,String>> entry : configTags.entrySet()) {
      String type = entry.getKey();
      Map<String, String> values = entry.getValue();
      
      String tag = values.get("tag");
      String hostTag = values.get("host_override_tag");
      
      DesiredConfig dc = new DesiredConfig();
      dc.setVersion(tag);
      actualConfigs.put(type, dc);
      if (null != hostTag && null != hostName) {
        List<HostOverride> list = new ArrayList<HostOverride>();
        list.add (new HostOverride(hostName, hostTag));
        dc.setHostOverrides(list);
    writeLock.lock();
    try {
      actualConfigs = new HashMap<String, DesiredConfig>();

      String hostName = getHostName();

      for (Entry<String, Map<String, String>> entry : configTags.entrySet()) {
        String type = entry.getKey();
        Map<String, String> values = entry.getValue();

        String tag = values.get("tag");
        String hostTag = values.get("host_override_tag");

        DesiredConfig dc = new DesiredConfig();
        dc.setVersion(tag);
        actualConfigs.put(type, dc);
        if (null != hostTag && null != hostName) {
          List<HostOverride> list = new ArrayList<HostOverride>();
          list.add(new HostOverride(hostName, hostTag));
          dc.setHostOverrides(list);
        }
       }
    } finally {
      writeLock.unlock();
     }

   }
   
   @Override
   public Map<String, DesiredConfig> getActualConfigs() {
    return actualConfigs;
    readLock.lock();
    try {
      return actualConfigs;
    } finally {
      readLock.unlock();
    }

   }
 
   @Override
   public HostState getHostState() {
    return host.getState();
    readLock.lock();
    try {
      return host.getState();
    } finally {
      readLock.unlock();
    }

   }
 }
diff --git a/ambari-server/src/main/resources/stacks/HDP/1.3.0/repos/repoinfo.xml b/ambari-server/src/main/resources/stacks/HDP/1.3.0/repos/repoinfo.xml
index cff983a78f..eb92e2ecd8 100644
-- a/ambari-server/src/main/resources/stacks/HDP/1.3.0/repos/repoinfo.xml
++ b/ambari-server/src/main/resources/stacks/HDP/1.3.0/repos/repoinfo.xml
@@ -18,7 +18,7 @@
 <reposinfo>
   <os type="centos6">
     <repo>
      <baseurl>http://s3.amazonaws.com/dev.hortonworks.com/HDP-1.3.0/repos/centos6</baseurl>
      <baseurl>http://public-repo-1.hortonworks.com/HDP/centos6/1.x/GA/1.3.0.0</baseurl>
       <repoid>HDP-1.3.0</repoid>
       <reponame>HDP</reponame>
     </repo>
@@ -31,7 +31,7 @@
   </os>
   <os type="centos5">
     <repo>
      <baseurl>http://s3.amazonaws.com/dev.hortonworks.com/HDP-1.3.0/repos/centos5</baseurl>
      <baseurl>http://public-repo-1.hortonworks.com/HDP/centos5/1.x/GA/1.3.0.0</baseurl>
       <repoid>HDP-1.3.0</repoid>
       <reponame>HDP</reponame>
     </repo>
@@ -44,7 +44,7 @@
   </os>
   <os type="redhat6">
     <repo>
      <baseurl>http://s3.amazonaws.com/dev.hortonworks.com/HDP-1.3.0/repos/centos6</baseurl>
      <baseurl>http://public-repo-1.hortonworks.com/HDP/centos6/1.x/GA/1.3.0.0</baseurl>
       <repoid>HDP-1.3.0</repoid>
       <reponame>HDP</reponame>
     </repo>
@@ -57,7 +57,7 @@
   </os>
   <os type="redhat5">
     <repo>
      <baseurl>http://s3.amazonaws.com/dev.hortonworks.com/HDP-1.3.0/repos/centos5</baseurl>
      <baseurl>http://public-repo-1.hortonworks.com/HDP/centos5/1.x/GA/1.3.0.0</baseurl>
       <repoid>HDP-1.3.0</repoid>
       <reponame>HDP</reponame>
     </repo>
@@ -70,7 +70,7 @@
   </os>
   <os type="oraclelinux6">
     <repo>
      <baseurl>http://s3.amazonaws.com/dev.hortonworks.com/HDP-1.3.0/repos/centos6</baseurl>
      <baseurl>http://public-repo-1.hortonworks.com/HDP/centos6/1.x/GA/1.3.0.0</baseurl>
       <repoid>HDP-1.3.0</repoid>
       <reponame>HDP</reponame>
     </repo>
@@ -83,7 +83,7 @@
   </os>
   <os type="oraclelinux5">
     <repo>
      <baseurl>http://s3.amazonaws.com/dev.hortonworks.com/HDP-1.3.0/repos/centos5</baseurl>
      <baseurl>http://public-repo-1.hortonworks.com/HDP/centos5/1.x/GA/1.3.0.0</baseurl>
       <repoid>HDP-1.3.0</repoid>
       <reponame>HDP</reponame>
     </repo>
@@ -96,14 +96,14 @@
   </os>
   <os type="suse11">
     <repo>
      <baseurl>http://s3.amazonaws.com/dev.hortonworks.com/HDP-1.3.0/repos/suse11</baseurl>
      <baseurl>http://public-repo-1.hortonworks.com/HDP/suse11/1.x/GA/1.3.0.0</baseurl>
       <repoid>HDP-1.3.0</repoid>
       <reponame>HDP</reponame>
     </repo>
   </os>
     <os type="sles11">
     <repo>
      <baseurl>http://s3.amazonaws.com/dev.hortonworks.com/HDP-1.3.0/repos/suse11</baseurl>
      <baseurl>http://public-repo-1.hortonworks.com/HDP/suse11/1.x/GA/1.3.0.0</baseurl>
       <repoid>HDP-1.3.0</repoid>
       <reponame>HDP</reponame>
     </repo>
diff --git a/ambari-server/src/main/resources/stacks/HDPLocal/1.3.0/repos/repoinfo.xml b/ambari-server/src/main/resources/stacks/HDPLocal/1.3.0/repos/repoinfo.xml
index cff983a78f..eb92e2ecd8 100644
-- a/ambari-server/src/main/resources/stacks/HDPLocal/1.3.0/repos/repoinfo.xml
++ b/ambari-server/src/main/resources/stacks/HDPLocal/1.3.0/repos/repoinfo.xml
@@ -18,7 +18,7 @@
 <reposinfo>
   <os type="centos6">
     <repo>
      <baseurl>http://s3.amazonaws.com/dev.hortonworks.com/HDP-1.3.0/repos/centos6</baseurl>
      <baseurl>http://public-repo-1.hortonworks.com/HDP/centos6/1.x/GA/1.3.0.0</baseurl>
       <repoid>HDP-1.3.0</repoid>
       <reponame>HDP</reponame>
     </repo>
@@ -31,7 +31,7 @@
   </os>
   <os type="centos5">
     <repo>
      <baseurl>http://s3.amazonaws.com/dev.hortonworks.com/HDP-1.3.0/repos/centos5</baseurl>
      <baseurl>http://public-repo-1.hortonworks.com/HDP/centos5/1.x/GA/1.3.0.0</baseurl>
       <repoid>HDP-1.3.0</repoid>
       <reponame>HDP</reponame>
     </repo>
@@ -44,7 +44,7 @@
   </os>
   <os type="redhat6">
     <repo>
      <baseurl>http://s3.amazonaws.com/dev.hortonworks.com/HDP-1.3.0/repos/centos6</baseurl>
      <baseurl>http://public-repo-1.hortonworks.com/HDP/centos6/1.x/GA/1.3.0.0</baseurl>
       <repoid>HDP-1.3.0</repoid>
       <reponame>HDP</reponame>
     </repo>
@@ -57,7 +57,7 @@
   </os>
   <os type="redhat5">
     <repo>
      <baseurl>http://s3.amazonaws.com/dev.hortonworks.com/HDP-1.3.0/repos/centos5</baseurl>
      <baseurl>http://public-repo-1.hortonworks.com/HDP/centos5/1.x/GA/1.3.0.0</baseurl>
       <repoid>HDP-1.3.0</repoid>
       <reponame>HDP</reponame>
     </repo>
@@ -70,7 +70,7 @@
   </os>
   <os type="oraclelinux6">
     <repo>
      <baseurl>http://s3.amazonaws.com/dev.hortonworks.com/HDP-1.3.0/repos/centos6</baseurl>
      <baseurl>http://public-repo-1.hortonworks.com/HDP/centos6/1.x/GA/1.3.0.0</baseurl>
       <repoid>HDP-1.3.0</repoid>
       <reponame>HDP</reponame>
     </repo>
@@ -83,7 +83,7 @@
   </os>
   <os type="oraclelinux5">
     <repo>
      <baseurl>http://s3.amazonaws.com/dev.hortonworks.com/HDP-1.3.0/repos/centos5</baseurl>
      <baseurl>http://public-repo-1.hortonworks.com/HDP/centos5/1.x/GA/1.3.0.0</baseurl>
       <repoid>HDP-1.3.0</repoid>
       <reponame>HDP</reponame>
     </repo>
@@ -96,14 +96,14 @@
   </os>
   <os type="suse11">
     <repo>
      <baseurl>http://s3.amazonaws.com/dev.hortonworks.com/HDP-1.3.0/repos/suse11</baseurl>
      <baseurl>http://public-repo-1.hortonworks.com/HDP/suse11/1.x/GA/1.3.0.0</baseurl>
       <repoid>HDP-1.3.0</repoid>
       <reponame>HDP</reponame>
     </repo>
   </os>
     <os type="sles11">
     <repo>
      <baseurl>http://s3.amazonaws.com/dev.hortonworks.com/HDP-1.3.0/repos/suse11</baseurl>
      <baseurl>http://public-repo-1.hortonworks.com/HDP/suse11/1.x/GA/1.3.0.0</baseurl>
       <repoid>HDP-1.3.0</repoid>
       <reponame>HDP</reponame>
     </repo>
diff --git a/ambari-server/src/test/resources/deploy_HDP2.sh b/ambari-server/src/test/resources/deploy_HDP2.sh
index 1adf4ab368..37f5528fc0 100644
-- a/ambari-server/src/test/resources/deploy_HDP2.sh
++ b/ambari-server/src/test/resources/deploy_HDP2.sh
@@ -16,7 +16,7 @@ curl -i -X PUT -d '{"Clusters":{"desired_configs":{"type":"mapred-queue-acls","t
 echo "-----------------------mapred-queue-acls-----------------------"
 curl -i -X PUT -d '{"Clusters":{"desired_configs":{"type":"mapred-site","tag":"version1","properties":{"io.sort.mb":"","io.sort.record.percent":".2","io.sort.spill.percent":"","io.sort.factor":"100","mapred.tasktracker.tasks.sleeptime-before-sigkill":"250","mapred.job.tracker.handler.count":"50","mapred.system.dir":"/mapred/system","mapred.job.tracker":"","mapred.job.tracker.http.address":"","mapred.local.dir":"","mapreduce.cluster.administrators":"hadoop","mapred.reduce.parallel.copies":"30","mapred.tasktracker.map.tasks.maximum":"","mapred.tasktracker.reduce.tasks.maximum":"","tasktracker.http.threads":"50","mapred.map.tasks.speculative.execution":"false","mapred.reduce.tasks.speculative.execution":"false","mapred.reduce.slowstart.completed.maps":"0.05","mapred.inmem.merge.threshold":"1000","mapred.job.shuffle.merge.percent":"0.66","mapred.job.shuffle.input.buffer.percent":"0.7","mapred.map.output.compression.codec":"","mapred.output.compression.type":"BLOCK","mapred.jobtracker.completeuserjobs.maximum":"0","mapred.jobtracker.taskScheduler":"","mapred.jobtracker.restart.recover":"false","mapred.job.reduce.input.buffer.percent":"0.0","mapreduce.reduce.input.limit":"10737418240","mapred.compress.map.output":"","mapred.task.timeout":"600000","jetty.connector":"org.mortbay.jetty.nio.SelectChannelConnector","mapred.task.tracker.task-controller":"","mapred.child.root.logger":"INFO,TLA","mapred.child.java.opts":"","mapred.cluster.map.memory.mb":"","mapred.cluster.reduce.memory.mb":"","mapred.job.map.memory.mb":"","mapred.job.reduce.memory.mb":"","mapred.cluster.max.map.memory.mb":"","mapred.cluster.max.reduce.memory.mb":"","mapred.hosts":"","mapred.hosts.exclude":"","mapred.max.tracker.blacklists":"16","mapred.healthChecker.script.path":"","mapred.healthChecker.interval":"135000","mapred.healthChecker.script.timeout":"60000","mapred.job.tracker.persist.jobstatus.active":"false","mapred.job.tracker.persist.jobstatus.hours":"1","mapred.job.tracker.persist.jobstatus.dir":"","mapred.jobtracker.retirejob.check":"10000","mapred.jobtracker.retirejob.interval":"0","mapred.job.tracker.history.completed.location":"/mapred/history/done","mapred.task.maxvmem":"","mapred.jobtracker.maxtasks.per.job":"","mapreduce.fileoutputcommitter.marksuccessfuljobs":"false","mapred.userlog.retain.hours":"","mapred.job.reuse.jvm.num.tasks":"1","mapreduce.jobtracker.kerberos.principal":"","mapreduce.tasktracker.kerberos.principal":"","hadoop.job.history.user.location":"none","mapreduce.jobtracker.keytab.file":"","mapreduce.tasktracker.keytab.file":"","mapreduce.jobtracker.staging.root.dir":"/user","mapreduce.tasktracker.group":"hadoop","mapreduce.jobtracker.split.metainfo.maxsize":"50000000","mapreduce.history.server.embedded":"false","mapreduce.history.server.http.address":"","mapreduce.jobhistory.kerberos.principal":"","mapreduce.jobhistory.keytab.file":"","mapred.jobtracker.blacklist.fault-timeout-window":"180","mapred.jobtracker.blacklist.fault-bucket-width":"15","mapred.queue.names":"default","mapreduce.shuffle.port":"8081","mapreduce.jobhistory.intermediate-done-dir" : "/mr-history/tmp","mapreduce.jobhistory.done-dir" : "/mr-history/done"}}}}' -u admin:admin http://localhost:8080/api/v1/clusters/c1
 echo "-----------------------mapred-site-----------------------"
curl -i -X PUT -d `echo '{"Clusters":{"desired_configs":{"type":"yarn-site","tag":"version1","properties":{"yarn.resourcemanager.resource-tracker.address":"HOST:8025","yarn.resourcemanager.scheduler.address":"HOST:8030","yarn.resourcemanager.address":"HOST:8050","yarn.resourcemanager.admin.address":"HOST:8141","yarn.resourcemanager.scheduler.class":"org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CapacityScheduler","yarn.scheduler.minimum-allocation-mb":"1024","yarn.scheduler.maximum-allocation-mb":"8192","yarn.nodemanager.address":"0.0.0.0:45454","yarn.nodemanager.local-dirs":"/yarn/loc/dir","yarn.nodemanager.resource.memory-mb":"8192","yarn.application.classpath":"/etc/hadoop/conf,/usr/lib/hadoop/*,/usr/lib/hadoop/lib/*,/usr/lib/hadoop-hdfs/*,/usr/lib/hadoop-hdfs/lib/*,/usr/lib/hadoop-yarn/*,/usr/lib/hadoop-yarn/lib/*,/usr/lib/hadoop-mapreduce/*,/usr/lib/hadoop-mapreduce/lib/*,/usr/lib/ambari-server/*","yarn.nodemanager.vmem-pmem-ratio":"2.1","yarn.nodemanager.container-executor.class":"org.apache.hadoop.yarn.server.nodemanager.DefaultContainerExecutor","yarn.nodemanager.aux-services":"mapreduce.shuffle","yarn.nodemanager.aux-services.class":"org.apache.hadoop.mapred.ShuffleHandler","yarn.nodemanager.log-dirs":"/var/log/yarn","yarn.nodemanager.container-monitor.interval-ms":"3000","yarn.nodemanager.health-checker.script.path":"/etc/hadoop/conf/health_check","yarn.nodemanager.health-checker.interval-ms":"135000","yarn.nodemanager.health-checker.script.timeout-ms":"60000","yarn.nodemanager.log.retain-second":"604800","yarn.log-aggregation-enable":"true","yarn.nodemanager.remote-app-log-dir":"/app-logs","yarn.nodemanager.remote-app-log-dir-suffix":"logs","yarn.nodemanager.log-aggregation.compression-type":"gz","yarn.nodemanager.delete.debug-delay-sec":"36000"}}}}'| sed s/HOST/$HOST/g` -u admin:admin http://localhost:8080/api/v1/clusters/c1
curl -i -X PUT -d `echo '{"Clusters":{"desired_configs":{"type":"yarn-site","tag":"version1","properties":{"yarn.resourcemanager.resource-tracker.address":"HOST:8025","yarn.resourcemanager.scheduler.address":"HOST:8030","yarn.resourcemanager.address":"HOST:8050","yarn.resourcemanager.admin.address":"HOST:8141","yarn.resourcemanager.scheduler.class":"org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CapacityScheduler","yarn.scheduler.minimum-allocation-mb":"1024","yarn.scheduler.maximum-allocation-mb":"8192","yarn.nodemanager.address":"0.0.0.0:45454","yarn.nodemanager.local-dirs":"/yarn/loc/dir","yarn.nodemanager.resource.memory-mb":"8192","yarn.application.classpath":"/etc/hadoop/conf,/usr/lib/hadoop/*,/usr/lib/hadoop/lib/*,/usr/lib/hadoop-hdfs/*,/usr/lib/hadoop-hdfs/lib/*,/usr/lib/hadoop-yarn/*,/usr/lib/hadoop-yarn/lib/*,/usr/lib/hadoop-mapreduce/*,/usr/lib/hadoop-mapreduce/lib/*,/usr/lib/ambari-server/*","yarn.nodemanager.vmem-pmem-ratio":"2.1","yarn.nodemanager.container-executor.class":"org.apache.hadoop.yarn.server.nodemanager.DefaultContainerExecutor","yarn.nodemanager.aux-services":"mapreduce.shuffle","yarn.nodemanager.aux-services.class":"org.apache.hadoop.mapred.ShuffleHandler","yarn.nodemanager.log-dirs":"/var/log/hadoop-yarn/yarn","yarn.nodemanager.container-monitor.interval-ms":"3000","yarn.nodemanager.health-checker.script.path":"/etc/hadoop/conf/health_check","yarn.nodemanager.health-checker.interval-ms":"135000","yarn.nodemanager.health-checker.script.timeout-ms":"60000","yarn.nodemanager.log.retain-second":"604800","yarn.log-aggregation-enable":"true","yarn.nodemanager.remote-app-log-dir":"/app-logs","yarn.nodemanager.remote-app-log-dir-suffix":"logs","yarn.nodemanager.log-aggregation.compression-type":"gz","yarn.nodemanager.delete.debug-delay-sec":"36000"}}}}'| sed s/HOST/$HOST/g` -u admin:admin http://localhost:8080/api/v1/clusters/c1
 echo "-----------------------yarn-site-----------------------"
 
 curl -i -X POST -d '{"components":[{"ServiceComponentInfo":{"component_name":"NAMENODE"}},{"ServiceComponentInfo":{"component_name":"SECONDARY_NAMENODE"}},{"ServiceComponentInfo":{"component_name":"DATANODE"}},{"ServiceComponentInfo":{"component_name":"HDFS_CLIENT"}}]}' -u admin:admin http://localhost:8080/api/v1/clusters/c1/services?ServiceInfo/service_name=HDFS
diff --git a/ambari-web/app/config.js b/ambari-web/app/config.js
index 7fb985ec1f..a20488bc04 100644
-- a/ambari-web/app/config.js
++ b/ambari-web/app/config.js
@@ -34,6 +34,8 @@ App.componentsUpdateInterval = 6000;
 App.contentUpdateInterval = 15000;
 App.maxRunsForAppBrowser = 500;
 App.pageReloadTime=3600000;
App.singleNodeInstall = false;
App.singleNodeAlias = document.location.hostname;
 
 // experimental features are automatically enabled if running on brunch server
 App.enableExperimental = false;
diff --git a/ambari-web/app/controllers/global/cluster_controller.js b/ambari-web/app/controllers/global/cluster_controller.js
index 3b97fbe9ff..954b8c053d 100644
-- a/ambari-web/app/controllers/global/cluster_controller.js
++ b/ambari-web/app/controllers/global/cluster_controller.js
@@ -120,7 +120,7 @@ App.ClusterController = Em.Controller.extend({
               if (host) {
                 hostName = host.get('publicHostName');
               }
              return "http://" + hostName + "/ganglia";
              return "http://" + (App.singleNodeInstall ? App.singleNodeAlias : hostName) + "/ganglia";
             }
           }
         }
@@ -154,7 +154,7 @@ App.ClusterController = Em.Controller.extend({
               if (host) {
                 hostName = host.get('publicHostName');
               }
              return "http://" + hostName + "/nagios";
              return "http://" + (App.singleNodeInstall ? App.singleNodeAlias : hostName) + "/nagios";
             }
           }
         }
diff --git a/ambari-web/app/controllers/main/admin/misc_controller.js b/ambari-web/app/controllers/main/admin/misc_controller.js
index 6ef06f60c7..c2e040b6a9 100644
-- a/ambari-web/app/controllers/main/admin/misc_controller.js
++ b/ambari-web/app/controllers/main/admin/misc_controller.js
@@ -42,6 +42,7 @@ App.MainAdminMiscController = App.MainServiceInfoConfigsController.extend({
     });
   },
   loadServiceTagSuccess: function(data, opt, params) {
    var installedServices = App.Service.find().mapProperty("serviceName");
     var serviceConfigsDef = params.serviceConfigsDef;
     var serviceName = this.get('content.serviceName');
     var loadedClusterSiteToTagMap = {};
@@ -57,6 +58,8 @@ App.MainAdminMiscController = App.MainServiceInfoConfigsController.extend({
 
     var misc_configs = configSet.globalConfigs.filterProperty('serviceName', this.get('selectedService')).filterProperty('category', 'Users and Groups').filterProperty('isVisible', true);
 
    misc_configs = App.config.miscConfigVisibleProperty(misc_configs, installedServices);

     var sortOrder = this.get('configs').filterProperty('serviceName', this.get('selectedService')).filterProperty('category', 'Users and Groups').filterProperty('isVisible', true).mapProperty('name');
 
     var sorted = [];
diff --git a/ambari-web/app/controllers/main/host.js b/ambari-web/app/controllers/main/host.js
index 34aa750bcf..240930fa4c 100644
-- a/ambari-web/app/controllers/main/host.js
++ b/ambari-web/app/controllers/main/host.js
@@ -23,7 +23,6 @@ var componentHelper = require('utils/component');
 App.MainHostController = Em.ArrayController.extend({
   name:'mainHostController',
   content: App.Host.find(),
  comeWithFilter: false,
 
   alerts: function () {
     return App.router.get('clusterController.alerts').filterProperty('isOk', false).filterProperty('ignoredForHosts', false);
@@ -56,27 +55,32 @@ App.MainHostController = Em.ArrayController.extend({
    */
   filterByComponent:function (component) {
     var id = component.get('componentName');

    var column = 6;
     this.get('componentsForFilter').setEach('checkedForHostFilter', false);
    this.get('componentsForFilter').filterProperty('id', id).setEach('checkedForHostFilter', true);

    this.set('comeWithFilter', true);
  },
 
  /**
   * On click callback for decommission button
   * @param event
   */
  decommissionButtonPopup:function () {
    var self = this;
    App.showConfirmationPopup(function(){
      alert('do');
    });
    var filterForComponent = {
      iColumn: column,
      value: id,
      type: 'multiple'
    };

    var filterConditions = App.db.getFilterConditions(this.get('name'));
    if (filterConditions) {
      var component = filterConditions.findProperty('iColumn', column);
      if (component) {
        component.value = id;
      }
      else {
        filterConditions.push(filterForComponent);
      }
      App.db.setFilterConditions(this.get('name'), filterConditions);
    }
    else {
      App.db.setFilterConditions(this.get('name'), [filterForComponent]);
    }
   },

   /**
    * On click callback for delete button
   * @param event
    */
   deleteButtonPopup:function () {
     var self = this;
diff --git a/ambari-web/app/controllers/main/host/details.js b/ambari-web/app/controllers/main/host/details.js
index 06fc9494b0..d449f3c1f5 100644
-- a/ambari-web/app/controllers/main/host/details.js
++ b/ambari-web/app/controllers/main/host/details.js
@@ -50,7 +50,9 @@ App.MainHostDetailsController = Em.Controller.extend({
   /**
    * Send specific command to server
    * @param url
   * @param data Object to send
   * @param _method
   * @param postData
   * @param callback
    */
   sendCommandToServer : function(url, postData, _method, callback){
     var url =  (App.testMode) ?
diff --git a/ambari-web/app/controllers/wizard/step7_controller.js b/ambari-web/app/controllers/wizard/step7_controller.js
index 90f56b7328..e93f388226 100644
-- a/ambari-web/app/controllers/wizard/step7_controller.js
++ b/ambari-web/app/controllers/wizard/step7_controller.js
@@ -91,20 +91,7 @@ App.WizardStep7Controller = Em.Controller.extend({
    */
   activateSpecialConfigs: function () {
     var miscConfigs = this.get('stepConfigs').findProperty('serviceName', 'MISC').configs;
    var showProxyGroup = this.get('selectedServiceNames').contains('HIVE') ||
      this.get('selectedServiceNames').contains('HCATALOG') ||
      this.get('selectedServiceNames').contains('OOZIE');
    miscConfigs.findProperty('name', 'proxyuser_group').set('isVisible', showProxyGroup);
    miscConfigs.findProperty('name', 'hbase_user').set('isVisible', this.get('selectedServiceNames').contains('HBASE'));
    miscConfigs.findProperty('name', 'mapred_user').set('isVisible', this.get('selectedServiceNames').contains('MAPREDUCE'));
    miscConfigs.findProperty('name', 'hive_user').set('isVisible', this.get('selectedServiceNames').contains('HIVE'));
    miscConfigs.findProperty('name', 'hcat_user').set('isVisible', this.get('selectedServiceNames').contains('HCATALOG'));
    miscConfigs.findProperty('name', 'webhcat_user').set('isVisible', this.get('selectedServiceNames').contains('WEBHCAT'));
    miscConfigs.findProperty('name', 'oozie_user').set('isVisible', this.get('selectedServiceNames').contains('OOZIE'));
    miscConfigs.findProperty('name', 'zk_user').set('isVisible', this.get('selectedServiceNames').contains('ZOOKEEPER'));
    miscConfigs.findProperty('name', 'gmetad_user').set('isVisible', this.get('selectedServiceNames').contains('GANGLIA'));
    miscConfigs.findProperty('name', 'rrdcached_base_dir').set('isVisible', this.get('selectedServiceNames').contains('GANGLIA'));
    miscConfigs.findProperty('name', 'nagios_user').set('isVisible', this.get('selectedServiceNames').contains('NAGIOS'));
    miscConfigs = App.config.miscConfigVisibleProperty(miscConfigs, this.get('selectedServiceNames'));
   },
 
   /**
diff --git a/ambari-web/app/data/HDP2/config_mapping.js b/ambari-web/app/data/HDP2/config_mapping.js
index b525063001..7d1d53f98f 100644
-- a/ambari-web/app/data/HDP2/config_mapping.js
++ b/ambari-web/app/data/HDP2/config_mapping.js
@@ -303,7 +303,7 @@ var configs = [
   },
 /**********************************************yarn-site***************************************/
   {
    "name": "yarn.resourcemanager.resourcetracker.address",
    "name": "yarn.resourcemanager.resource-tracker.address",
     "templateName": ["rm_host"],
     "foreignKey": null,
     "value": "hdfs://<templateName[0]>:8025",
@@ -317,7 +317,7 @@ var configs = [
     "filename": "yarn-site.xml"
   },
   {
    "name": "yarn.resourcemanager.addresss",
    "name": "yarn.resourcemanager.address",
     "templateName": ["rm_host"],
     "foreignKey": null,
     "value": "hdfs://<templateName[0]>:8050",
@@ -359,30 +359,24 @@ var configs = [
     "value": "<templateName[0]>",
     "filename": "mapred-site.xml"
   },
  //io.sort.mb -> mapreduce.task.io.sort.mb
   {
    "name": "io.sort.mb",
    "templateName": ["io_sort_mb"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  /*{
    "name": "mapred.cluster.reduce.memory.mb",
    "templateName": ["mapred_cluster_red_mem_mb"],
    "name": "mapreduce.task.io.sort.mb",
    "templateName": ["mapreduce_task_io_sort_mb"],
     "foreignKey": null,
     "value": "<templateName[0]>",
     "filename": "mapred-site.xml"
   },
   {
    "name": "mapred.cluster.max.map.memory.mb",
    "templateName": ["mapred_cluster_max_map_mem_mb"],
    "name": "mapred.system.dir",
    "templateName": ["mapred_system_dir"],
     "foreignKey": null,
     "value": "<templateName[0]>",
     "filename": "mapred-site.xml"
   },
   {
    "name": "mapred.cluster.max.reduce.memory.mb",
    "templateName": ["mapred_cluster_max_red_mem_mb"],
    "name": "mapred.cluster.reduce.memory.mb",
    "templateName": ["mapred_cluster_red_mem_mb"],
     "foreignKey": null,
     "value": "<templateName[0]>",
     "filename": "mapred-site.xml"
@@ -395,68 +389,12 @@ var configs = [
     "filename": "mapred-site.xml"
   },
   {
    "name": "mapred.hosts.exclude",
    "templateName": ["hadoop_conf_dir", "mapred_hosts_exclude"],
    "foreignKey": null,
    "value": "<templateName[0]>/<templateName[1]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.healthChecker.script.path",
    "templateName": ["mapred_jobstatus_dir"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.job.tracker.persist.jobstatus.dir",
    "templateName": ["hadoop_conf_dir"],
    "foreignKey": null,
    "value": "<templateName[0]>/health_check",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.child.java.opts",
    "templateName": ["mapred_child_java_opts_sz"],
    "foreignKey": null,
    "value": "-server -Xmx<templateName[0]>m -Djava.net.preferIPv4Stack=true",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.cluster.map.memory.mb",
    "templateName": ["mapred_cluster_map_mem_mb"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "io.sort.spill.percent",
    "templateName": ["io_sort_spill_percent"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.system.dir",
    "templateName": ["mapred_system_dir"],
    "name": "mapred.jobtracker.maxtasks.per.job",
    "templateName": ["maxtasks_per_job"],
     "foreignKey": null,
     "value": "<templateName[0]>",
     "filename": "mapred-site.xml"
   },
  {
    "name": "mapred.job.tracker",
    "templateName": ["jobtracker_host"],
    "foreignKey": null,
    "value": "<templateName[0]>:50300",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.job.tracker.http.address",
    "templateName": ["jobtracker_host"],
    "foreignKey": null,
    "value": "<templateName[0]>:50030",
    "filename": "mapred-site.xml"
  },
   {
     "name": "mapred.userlog.retain.hours",
     "templateName": ["mapreduce_userlog_retainhours"],
@@ -465,8 +403,8 @@ var configs = [
     "filename": "mapred-site.xml"
   },
   {
    "name": "mapred.jobtracker.maxtasks.per.job",
    "templateName": ["maxtasks_per_job"],
    "name": "mapred.local.dir",
    "templateName": ["mapred_local_dir"],
     "foreignKey": null,
     "value": "<templateName[0]>",
     "filename": "mapred-site.xml"
@@ -479,61 +417,54 @@ var configs = [
     "filename": "mapred-site.xml"
   },
   {
    "name": "mapreduce.jobtracker.kerberos.principal",
    "templateName": ["kerberos_domain"],
    "name": "mapred.tasktracker.map.tasks.maximum",
    "templateName": ["mapred_map_tasks_max"],
     "foreignKey": null,
    "value": "jt/_HOST@<templateName[0]>",
    "value": "<templateName[0]>",
     "filename": "mapred-site.xml"
   },
   {
    "name": "mapreduce.tasktracker.kerberos.principal",
    "templateName": ["kerberos_domain"],
    "name": "mapred.hosts.exclude",
    "templateName": ["hadoop_conf_dir", "mapred_hosts_exclude"],
     "foreignKey": null,
    "value": "tt/_HOST@<templateName[0]>",
    "value": "<templateName[0]>/<templateName[1]>",
     "filename": "mapred-site.xml"
   },
   {
    "name": "mapreduce.jobtracker.keytab.file",
    "templateName": ["keytab_path"],
    "name": "mapred.cluster.max.map.memory.mb",
    "templateName": ["mapred_cluster_max_map_mem_mb"],
     "foreignKey": null,
    "value": "<templateName[0]>/jt.service.keytab",
    "value": "<templateName[0]>",
     "filename": "mapred-site.xml"
   },
   {
    "name": "mapreduce.tasktracker.keytab.file",
    "templateName": ["keytab_path"],
    "name": "mapred.cluster.max.reduce.memory.mb",
    "templateName": ["mapred_cluster_max_red_mem_mb"],
     "foreignKey": null,
    "value": "<templateName[0]>/tt.service.keytab",
    "value": "<templateName[0]>",
     "filename": "mapred-site.xml"
   },
   {
    "name": "mapreduce.history.server.embedded",
    "templateName": [],
    "name": "mapred.jobtracker.taskScheduler",
    "templateName": ["scheduler_name"],
     "foreignKey": null,
    "value": "false",
    "value": "<templateName[0]>",
     "filename": "mapred-site.xml"
   },
   {
    "name": "mapreduce.history.server.http.address",
    "templateName": ["jobtracker_host"],
    "name": "mapred.healthChecker.script.path",
    "templateName": ["mapred_jobstatus_dir"],
     "foreignKey": null,
    "value": "<templateName[0]>:51111",
    "value": "<templateName[0]>",
     "filename": "mapred-site.xml"
   },
   {
    "name": "mapreduce.jobhistory.kerberos.principal",
    "templateName": ["kerberos_domain"],
    "name": "mapred.cluster.map.memory.mb",
    "templateName": ["mapred_cluster_map_mem_mb"],
     "foreignKey": null,
    "value": "jt/_HOST@<templateName[0]>",
    "value": "<templateName[0]>",
     "filename": "mapred-site.xml"
   },
  {
    "name": "mapreduce.jobhistory.keytab.file",
    "templateName": ["keytab_path"],
    "foreignKey": null,
    "value": "<templateName[0]>/jt.service.keytab",
    "filename": "mapred-site.xml"
  },*/
   /**********************************************hbase-site***************************************/
   {
     "name": "hbase.rootdir",
diff --git a/ambari-web/app/data/HDP2/config_properties.js b/ambari-web/app/data/HDP2/config_properties.js
index 7c98fc067f..d0eacbdb95 100644
-- a/ambari-web/app/data/HDP2/config_properties.js
++ b/ambari-web/app/data/HDP2/config_properties.js
@@ -58,7 +58,7 @@
  *
  *   serviceName:
  *     The service that the config property belongs to.
 *     E.g., "HDFS", "MAPREDUCE", "ZOOKEEPER", etc.
 *     E.g., "HDFS", "MAPREDUCE2", "ZOOKEEPER", etc.
  *
  *   category: the category that the config property belongs to (used for grouping config properties in the UI).
  *     if unspecified, "General" is assumed.
@@ -494,6 +494,209 @@ module.exports =
       "category": "HistoryServer",
       "index": 0
     },
    {
      "id": "puppet var",
      "name": "mapred_local_dir",
      "displayName": "MapReduce local directories",
      "description": "Directories for MapReduce to store intermediate data files",
      "defaultValue": "",
      "defaultDirectory": "/hadoop/mapred",
      "displayType": "directories",
      "isReconfigurable": true,
      "isVisible": true,
      "domain": "global",
      "serviceName": "MAPREDUCE2"
    },
    {
      "id": "puppet var",
      "name": "mapred_cluster_red_mem_mb",
      "displayName": "Cluster's Reduce slot size (virtual memory)",
      "description": "The virtual memory size of a single Reduce slot in the MapReduce framework",
      "defaultValue": "-1",
      "displayType": "int",
      "unit": "MB",
      "isVisible": true,
      "domain": "global",
      "serviceName": "MAPREDUCE2"
    },
    {
      "id": "puppet var",
      "name": "mapred_map_tasks_max",
      "displayName": "Number of Map slots per node",
      "description": "Number of slots that Map tasks that run simultaneously can occupy on a TaskTracker",
      "defaultValue": "4",
      "displayType": "int",
      "isVisible": true,
      "domain": "global",
      "serviceName": "MAPREDUCE2"
    },
    {
      "id": "puppet var",
      "name": "mapred_job_map_mem_mb",
      "displayName": "Default virtual memory for a job's map-task",
      "description": "Virtual memory for single Map task",
      "defaultValue": "-1",
      "displayType": "int",
      "unit": "MB",
      "isVisible": true,
      "domain": "global",
      "serviceName": "MAPREDUCE2"
    },
    {
      "id": "puppet var",
      "name": "mapred_job_red_mem_mb",
      "displayName": "Default virtual memory for a job's reduce-task",
      "description": "Virtual memory for single Reduce task",
      "defaultValue": "-1",
      "displayType": "int",
      "unit": "MB",
      "isVisible": true,
      "domain": "global",
      "serviceName": "MAPREDUCE2"
    },
    {
      "id": "puppet var",
      "name": "mapreduce_task_io_sort_mb",
      "displayName": "Map-side sort buffer memory",
      "description": "The total amount of Map-side buffer memory to use while sorting files (Expert-only configuration)",
      "defaultValue": "200",
      "displayType": "int",
      "unit": "MB",
      "isVisible": true,
      "domain": "global",
      "serviceName": "MAPREDUCE2"
    },
    {
      "id": "puppet var",
      "name": "mapreduce_userlog_retainhours",
      "displayName": "Job log retention (hours)",
      "description": "The maximum time, in hours, for which the user-logs are to be retained after the job completion.",
      "defaultValue": "24",
      "displayType": "int",
      "unit": "hours",
      "isVisible": true,
      "domain": "global",
      "serviceName": "MAPREDUCE2"
    },
    {
      "id": "puppet var",
      "name": "maxtasks_per_job",
      "displayName": "Maximum number tasks for a Job",
      "description": "Maximum number of tasks for a single Job",
      "defaultValue": "-1",
      "displayType": "int",
      "isVisible": true,
      "domain": "global",
      "serviceName": "MAPREDUCE2"
    },
    {
      "id": "puppet var",
      "name": "mapred_cluster_max_map_mem_mb",
      "displayName": "Upper limit on virtual memory for single Map task",
      "description": "Upper limit on virtual memory size for a single Map task of any MapReduce job",
      "defaultValue": "-1",
      "displayType": "int",
      "unit": "MB",
      "isVisible": true,
      "domain": "global",
      "serviceName": "MAPREDUCE2"
    },
    {
      "id": "puppet var",
      "name": "mapred_cluster_max_red_mem_mb",
      "displayName": "Upper limit on virtual memory for single Reduce task",
      "description": "Upper limit on virtual memory size for a single Reduce task of any MapReduce job",
      "defaultValue": "-1",
      "displayType": "int",
      "unit": "MB",
      "isVisible": true,
      "domain": "global",
      "serviceName": "MAPREDUCE2"
    },
    {
      "id": "puppet var",
      "name": "scheduler_name",
      "displayName": "MapReduce Capacity Scheduler",
      "description": "The scheduler to use for scheduling of MapReduce jobs",
      "defaultValue": "org.apache.hadoop.mapred.CapacityTaskScheduler",
      "displayType": "advanced",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MAPREDUCE2"
    },
    {
      "id": "puppet var",
      "name": "mapred_cluster_map_mem_mb",
      "displayName": "Cluster's Map slot size (virtual memory)",
      "description": "The virtual memory size of a single Map slot in the MapReduce framework",
      "defaultValue": "-1",
      "displayType": "int",
      "unit": "MB",
      "isVisible": true,
      "domain": "global",
      "serviceName": "MAPREDUCE2"
    },
    {
      "id": "puppet var",
      "name": "mapred_system_dir",
      "displayName": "MapReduce system directories",
      "description": "",
      "defaultValue": "/mapred/system",
      "displayType": "directories",
      "isReconfigurable": true,
      "isVisible": true,
      "domain": "global",
      "serviceName": "MAPREDUCE2",
      "category": "Advanced"
    },
    {
      "id": "puppet var",
      "name": "mapred_hosts_include",
      "displayName": "Include hosts",
      "description": "Include enetered hosts",
      "defaultValue": "mapred.include",
      "displayType": "directories",
      "isVisible": false,
      "serviceName": "MAPREDUCE2",
      "domain": "global",
      "category": "Advanced"
    },
    {
      "id": "puppet var",
      "name": "task_controller",
      "displayName": "task_controller",
      "description": "",
      "defaultValue": "org.apache.hadoop.mapred.DefaultTaskController",
      "displayType": "advanced",
      "isVisible": false,
      "serviceName": "MAPREDUCE2",
      "domain": "global",
      "category": "Advanced"
    },
    {
      "id": "puppet var",
      "name": "mapred_hosts_exclude",
      "displayName": "Exclude hosts",
      "description": "Exclude entered hosts",
      "defaultValue": "mapred.exclude",
      "displayType": "directories",
      "isVisible": false,
      "serviceName": "MAPREDUCE2",
      "domain": "global",
      "category": "Advanced"
    },
    {
      "id": "puppet var",
      "name": "mapred_jobstatus_dir",
      "displayName": "Job Status directory",
      "description": "Directory path to view job status",
      "defaultValue": "file:////mapred/jobstatus",
      "displayType": "advanced",
      "isVisible": false,
      "serviceName": "MAPREDUCE2",
      "domain": "global",
      "category": "Advanced"
    },
   /**********************************************YARN***************************************/
     {
       "id": "puppet var",
@@ -1695,7 +1898,8 @@ module.exports =
       "isVisible": false,
       "domain": "global",
       "serviceName": "MISC",
      "category": "General"
      "category": "General",
      "belongsToService":[]
     },
     {
       "id": "puppet var",
@@ -1710,7 +1914,8 @@ module.exports =
       "filename": "core-site.xml",
       "domain": "global",
       "serviceName": "MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["HIVE","WEBHCAT","OOZIE"]
     },
     {
       "id": "puppet var",
@@ -1724,7 +1929,8 @@ module.exports =
       "filename": "hdfs-site.xml",
       "domain": "global",
       "serviceName": "MISC",
      "category": "Advanced"
      "category": "Advanced",
      "belongsToService":[]
     },
     {
       "id": "puppet var",
@@ -1738,7 +1944,8 @@ module.exports =
       "filename": "hdfs-site.xml",
       "domain": "global",
       "serviceName": "MISC",
      "category": "Advanced"
      "category": "Advanced",
      "belongsToService":[]
     },
     {
       "id": "puppet var",
@@ -1751,7 +1958,8 @@ module.exports =
       "isVisible": false,
       "domain": "global",
       "serviceName": "MISC",
      "category": "General"
      "category": "General",
      "belongsToService":[]
     },
     {
       "id": "puppet var",
@@ -1764,7 +1972,8 @@ module.exports =
       "isVisible": false,
       "domain": "global",
       "serviceName": "MISC",
      "category": "General"
      "category": "General",
      "belongsToService":[]
     },
     {
       "id": "puppet var",
@@ -1777,7 +1986,8 @@ module.exports =
       "isVisible": false,
       "domain": "global",
       "serviceName": "MISC",
      "category": "General"
      "category": "General",
      "belongsToService":[]
     },
     {
       "id": "puppet var",
@@ -1790,7 +2000,8 @@ module.exports =
       "displayType": "directory",
       "isVisible": false,
       "domain": "global",
      "serviceName": "MISC"
      "serviceName": "MISC",
      "belongsToService":[]
     },
     {
       "id": "puppet var",
@@ -1804,7 +2015,8 @@ module.exports =
       "isVisible": false,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Advanced"
      "category": "Advanced",
      "belongsToService":[]
     },
     {
       "id": "puppet var",
@@ -1818,7 +2030,8 @@ module.exports =
       "isVisible": false,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Advanced"
      "category": "Advanced",
      "belongsToService":[]
     },
     {
       "id": "puppet var",
@@ -1832,7 +2045,8 @@ module.exports =
       "isVisible": true,
       "serviceName": "MISC",
       "domain": "global",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["HDFS"]
     },
     {
       "id": "puppet var",
@@ -1846,7 +2060,8 @@ module.exports =
       "isVisible": true,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["MAPREDUCE"]
     },
     {
       "id": "puppet var",
@@ -1860,7 +2075,8 @@ module.exports =
       "isVisible": true,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["HBASE"]
     },
     {
       "id": "puppet var",
@@ -1874,7 +2090,8 @@ module.exports =
       "isVisible": true,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["HIVE"]
     },
     {
       "id": "puppet var",
@@ -1888,7 +2105,8 @@ module.exports =
       "isVisible": true,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["HCATALOG"]
     },
     {
       "id": "puppet var",
@@ -1902,7 +2120,8 @@ module.exports =
       "isVisible": true,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["WEBHCAT"]
     },
     {
       "id": "puppet var",
@@ -1916,7 +2135,8 @@ module.exports =
       "isVisible": true,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["OOZIE"]
     },
     {
       "id": "puppet var",
@@ -1930,7 +2150,8 @@ module.exports =
       "isVisible": true,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["ZOOKEEPER"]
     },
     {
       "id": "puppet var",
@@ -1944,7 +2165,8 @@ module.exports =
       "isVisible": true,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["GANGLIA"]
     },
     {
       "id": "puppet var",
@@ -1958,7 +2180,8 @@ module.exports =
       "isVisible": false,
       "domain": "global",
       "serviceName":"MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":[]
     },
     {
       "id": "puppet var",
@@ -1972,7 +2195,8 @@ module.exports =
       "domain": "global",
       "isVisible": true,
       "serviceName":"MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["NAGIOS"]
     },
     {
       "id": "puppet var",
@@ -1986,7 +2210,8 @@ module.exports =
       "isVisible": App.supports.customizeSmokeTestUser,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["HDFS"]
     },
     {
       "id": "puppet var",
@@ -2000,7 +2225,8 @@ module.exports =
       "isVisible": true,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["HDFS"]
     },
     {
       "id": "puppet var",
@@ -2012,7 +2238,8 @@ module.exports =
       "isOverridable": false,
       "domain": "global",
       "serviceName": "MISC",
      "category": "General"
      "category": "General",
      "belongsToService":["GANGLIA"]
     }
   ]
 };
diff --git a/ambari-web/app/data/config_properties.js b/ambari-web/app/data/config_properties.js
index d2a4abd601..318af51ed8 100644
-- a/ambari-web/app/data/config_properties.js
++ b/ambari-web/app/data/config_properties.js
@@ -2132,7 +2132,8 @@ module.exports =
       "isVisible": false,
       "domain": "global",
       "serviceName": "MISC",
      "category": "General"
      "category": "General",
      "belongsToService":[]
     },
     {
       "id": "puppet var",
@@ -2147,7 +2148,8 @@ module.exports =
       "filename": "core-site.xml",
       "domain": "global",
       "serviceName": "MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["HIVE","WEBHCAT","OOZIE"]
     },
     {
       "id": "puppet var",
@@ -2161,7 +2163,8 @@ module.exports =
       "filename": "hdfs-site.xml",
       "domain": "global",
       "serviceName": "MISC",
      "category": "Advanced"
      "category": "Advanced",
      "belongsToService":[]
     },
     {
       "id": "puppet var",
@@ -2175,7 +2178,8 @@ module.exports =
       "filename": "hdfs-site.xml",
       "domain": "global",
       "serviceName": "MISC",
      "category": "Advanced"
      "category": "Advanced",
      "belongsToService":[]
     },
     {
       "id": "puppet var",
@@ -2188,7 +2192,8 @@ module.exports =
       "isVisible": false,
       "domain": "global",
       "serviceName": "MISC",
      "category": "General"
      "category": "General",
      "belongsToService":[]
     },
     {
       "id": "puppet var",
@@ -2201,7 +2206,8 @@ module.exports =
       "isVisible": false,
       "domain": "global",
       "serviceName": "MISC",
      "category": "General"
      "category": "General",
      "belongsToService":[]
     },
     {
       "id": "puppet var",
@@ -2214,7 +2220,8 @@ module.exports =
       "isVisible": false,
       "domain": "global",
       "serviceName": "MISC",
      "category": "General"
      "category": "General",
      "belongsToService":[]
     },
     /*
     {
@@ -2269,7 +2276,8 @@ module.exports =
       "displayType": "directory",
       "isVisible": false,
       "domain": "global",
      "serviceName": "MISC"
      "serviceName": "MISC",
      "belongsToService":[]
     },
     {
       "id": "puppet var",
@@ -2283,7 +2291,8 @@ module.exports =
       "isVisible": false,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Advanced"
      "category": "Advanced",
      "belongsToService":[]
     },
     {
       "id": "puppet var",
@@ -2297,7 +2306,8 @@ module.exports =
       "isVisible": false,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Advanced"
      "category": "Advanced",
      "belongsToService":[]
     },
     /*
     {
@@ -2413,7 +2423,8 @@ module.exports =
       "isVisible": true,
       "serviceName": "MISC",
       "domain": "global",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["HDFS"]
     },
     {
       "id": "puppet var",
@@ -2427,7 +2438,8 @@ module.exports =
       "isVisible": true,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["MAPREDUCE"]
     },
     {
       "id": "puppet var",
@@ -2441,7 +2453,8 @@ module.exports =
       "isVisible": true,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["HBASE"]
     },
     {
       "id": "puppet var",
@@ -2455,7 +2468,8 @@ module.exports =
       "isVisible": true,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["HIVE"]
     },
     {
       "id": "puppet var",
@@ -2469,7 +2483,8 @@ module.exports =
       "isVisible": true,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["HCATALOG"]
     },
     {
       "id": "puppet var",
@@ -2483,7 +2498,8 @@ module.exports =
       "isVisible": true,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["WEBHCAT"]
     },
     {
       "id": "puppet var",
@@ -2497,7 +2513,8 @@ module.exports =
       "isVisible": true,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["OOZIE"]
     },
     /*
     {
@@ -2568,7 +2585,8 @@ module.exports =
       "isVisible": true,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["ZOOKEEPER"]
     },
     {
       "id": "puppet var",
@@ -2582,7 +2600,8 @@ module.exports =
       "isVisible": true,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["GANGLIA"]
     },
     {
       "id": "puppet var",
@@ -2596,7 +2615,8 @@ module.exports =
       "isVisible": false,
       "domain": "global",
       "serviceName":"MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":[]
     },
     {
       "id": "puppet var",
@@ -2610,7 +2630,8 @@ module.exports =
       "domain": "global",
       "isVisible": true,
       "serviceName":"MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["NAGIOS"]
     },
     {
       "id": "puppet var",
@@ -2624,7 +2645,8 @@ module.exports =
       "isVisible": App.supports.customizeSmokeTestUser,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["HDFS"]
     },
     {
       "id": "puppet var",
@@ -2638,7 +2660,8 @@ module.exports =
       "isVisible": true,
       "domain": "global",
       "serviceName": "MISC",
      "category": "Users and Groups"
      "category": "Users and Groups",
      "belongsToService":["HDFS"]
     },
     /*
     {
@@ -2664,7 +2687,8 @@ module.exports =
       "isOverridable": false,
       "domain": "global",
       "serviceName": "MISC",
      "category": "General"
      "category": "General",
      "belongsToService":["GANGLIA"]
     }
   ]
 };
diff --git a/ambari-web/app/messages.js b/ambari-web/app/messages.js
index 2849398007..9f1a04c566 100644
-- a/ambari-web/app/messages.js
++ b/ambari-web/app/messages.js
@@ -115,6 +115,7 @@ Em.I18n.translations = {
   'common.search': 'Search',
   'common.confirm': 'Confirm',
   'common.upgrade': 'Upgrade',
  'common.reUpgrade': 'Retry Upgrade',
   'common.security':'Security',
   'common.cluster':'Cluster',
   'common.add': 'Add',
@@ -128,7 +129,7 @@ Em.I18n.translations = {
   'common.abort': 'Abort',
   'common.misc': 'Misc',
   'common.operations': 'Operations',
  'common.install': 'Install',
  'common.reinstall': 'Re-Install',
   'common.errorPopup.header': 'An error has been encountered',
   'common.use': 'Use',
   'common.stacks': 'Stacks',
diff --git a/ambari-web/app/models/host_component.js b/ambari-web/app/models/host_component.js
index 3d2ccd5f11..ad3e7f0428 100644
-- a/ambari-web/app/models/host_component.js
++ b/ambari-web/app/models/host_component.js
@@ -88,7 +88,33 @@ App.HostComponent = DS.Model.extend({
       }
     }
     return decommissioning;
  }.property('componentName', 'host.hostName', 'App.router.mainServiceController.hdfsService.decommissionDataNodes.@each.hostName')
  }.property('componentName', 'host.hostName', 'App.router.mainServiceController.hdfsService.decommissionDataNodes.@each.hostName'),
  /**
   * User friendly host component status
   */
  componentTextStatus: function () {
    var value = this.get("workStatus");

    switch(value){
      case "INSTALLING":
        return 'Installing...';
      case "INSTALL_FAILED":
        return 'Install Failed';
      case "INSTALLED":
        return 'Stopped';
      case "STARTED":
        return 'Started';
      case "STARTING":
        return 'Starting...';
      case "STOPPING":
        return 'Stopping...';
      case "UNKNOWN":
        return 'Heartbeat lost...';
      case "UPGRADE_FAILED":
        return 'Upgrade Failed';
    }
    return 'Unknown';
  }.property('workStatus','isDecommissioning')
 });
 
 App.HostComponent.FIXTURES = [];
@@ -98,8 +124,6 @@ App.HostComponentStatus = {
   starting: "STARTING",
   stopped: "INSTALLED",
   stopping: "STOPPING",
  stop_failed: "STOP_FAILED",
  start_failed: "START_FAILED",
   install_failed: "INSTALL_FAILED",
   installing: "INSTALLING",
   upgrade_failed: "UPGRADE_FAILED",
@@ -115,10 +139,6 @@ App.HostComponentStatus = {
         return 'installed';
       case this.stopping:
         return 'stopping';
      case this.stop_failed:
        return 'stop_failed';
      case this.start_failed:
        return 'start_failed';
       case this.install_failed:
         return 'install_failed';
       case this.installing:
diff --git a/ambari-web/app/models/service.js b/ambari-web/app/models/service.js
index 9507b1df88..2d643d7418 100644
-- a/ambari-web/app/models/service.js
++ b/ambari-web/app/models/service.js
@@ -57,7 +57,7 @@ App.Service = DS.Model.extend({
       this.set('healthStatus', 'green');
     } else if (components.someProperty('workStatus', App.HostComponentStatus.starting)) {
       this.set('healthStatus', 'green-blinking');
    } else if (components.someProperty('workStatus', App.HostComponentStatus.stopped) || components.someProperty('workStatus', App.HostComponentStatus.start_failed)) {
    } else if (components.someProperty('workStatus', App.HostComponentStatus.stopped)) {
       this.set('healthStatus', 'red');
     } else if (components.someProperty('workStatus', App.HostComponentStatus.unknown)) {
       this.set('healthStatus', 'yellow');
diff --git a/ambari-web/app/models/service_config.js b/ambari-web/app/models/service_config.js
index bec655dc59..7b07a30e1b 100644
-- a/ambari-web/app/models/service_config.js
++ b/ambari-web/app/models/service_config.js
@@ -19,10 +19,6 @@
 var App = require('app');
 var validator = require('utils/validator');
 
App.ConfigProperties = Ember.ArrayProxy.extend({
  content: require('data/config_properties').configProperties
});

 App.ServiceConfig = Ember.Object.extend({
   serviceName: '',
   configCategories: [],
diff --git a/ambari-web/app/styles/application.less b/ambari-web/app/styles/application.less
index e4903e3b45..94c52fc729 100644
-- a/ambari-web/app/styles/application.less
++ b/ambari-web/app/styles/application.less
@@ -2170,6 +2170,14 @@ table.graphs {
     .marker;
   }
 
  .health-status-color-blue {
    color:#0000ff;
  }

  .health-status-color-red {
    color:#ff0000;
  }

   .health-status-DEAD-ORANGE {
     background-image: @status-dead-orange-marker;
     .marker;
@@ -2242,6 +2250,18 @@ table.graphs {
 
   .host-components .btn-group {
     margin: 0 5px 10px 0;

    .component-text-status{
      cursor: default;
      display: block;
      padding: 3px 20px;
      clear: both;
      font-weight: normal;
      line-height: 20px;
      color: #333333;
      white-space: nowrap;
      font-style: italic;
    }
   }
 }
 
diff --git a/ambari-web/app/templates/main/admin/misc.hbs b/ambari-web/app/templates/main/admin/misc.hbs
index a420dd3717..09b7020dd8 100644
-- a/ambari-web/app/templates/main/admin/misc.hbs
++ b/ambari-web/app/templates/main/admin/misc.hbs
@@ -31,10 +31,12 @@
           </thead>
           <tbody>
             {{#each user in controller.users}}
              <tr>
                <td>{{user.displayName}}</td>
                <td>{{user.value}}</td>
              </tr>
              {{#if user.isVisible}}
                <tr>
                  <td>{{user.displayName}}</td>
                  <td>{{user.value}}</td>
                </tr>
              {{/if}}
             {{/each}}
           </tbody>
         </table>
diff --git a/ambari-web/app/templates/main/host/summary.hbs b/ambari-web/app/templates/main/host/summary.hbs
index 647bdc4cc0..bb4f0c783b 100644
-- a/ambari-web/app/templates/main/host/summary.hbs
++ b/ambari-web/app/templates/main/host/summary.hbs
@@ -64,43 +64,60 @@
             <div class="span5">
               {{#if App.isAdmin}}
               <div class="btn-group">
                <a {{ bindAttr class=":btn :dropdown-toggle view.disabledClass"}} data-toggle="dropdown">
                <a {{ bindAttr class=":btn :dropdown-toggle"}} data-toggle="dropdown">
                   {{t common.action}}
                   <span class="caret pull-right"></span>
                 </a>
                 <ul class="dropdown-menu">
                   <li>
                      <div class="component-text-status">
                       {{view.componentTextStatus}}
                      </div>
                    </li>
                   {{#if view.isDataNode}}
                  <li {{bindAttr class="view.isDataNodeDecommissionAvailable::hidden"}}>
                    <a href="javascript:void(null)" data-toggle="modal" {{action "decommission" view.content target="controller"}}>
                      {{t common.decommission}}
                    </a>
                  </li>
                  <li {{bindAttr class="view.isDataNodeRecommissionAvailable::hidden"}}>
                    <a href="javascript:void(null)" data-toggle="modal" {{action "recommission" view.content target="controller"}}>
                      {{t common.recommission}}
                    </a>
                  </li>
                    {{#if view.isDataNodeDecommissionAvailable}}
                      <li {{bindAttr class="view.noActionAvailable"}}>
                        <a href="javascript:void(null)" data-toggle="modal" {{action "decommission" view.content target="controller"}}>
                          {{t common.decommission}}
                        </a>
                      </li>
                    {{/if}}
                    {{#if view.isDataNodeRecommissionAvailable}}
                      <li {{bindAttr class="view.noActionAvailable"}}>
                        <a href="javascript:void(null)" data-toggle="modal" {{action "recommission" view.content target="controller"}}>
                          {{t common.recommission}}
                        </a>
                      </li>
                    {{/if}}
                  {{/if}}
                  {{#if view.isStart}}
                    <li {{bindAttr class=" view.isDecommissioning:hidden view.noActionAvailable"}}>
                      <a href="javascript:void(null)" data-toggle="modal" {{action "stopComponent" view.content target="controller"}}>
                        {{t common.stop}}
                      </a>
                    </li>
                  {{/if}}
                  {{#unless view.isStart}}
                    <li {{bindAttr class="view.isUpgradeFailed:hidden view.isInstallFailed:hidden view.isDecommissioning:hidden view.noActionAvailable"}}>
                      <a href="javascript:void(null)" data-toggle="modal" {{action "startComponent" view.content target="controller"}}>
                        {{t common.start}}
                      </a>
                    </li>
                  {{/unless}}
                  {{#if view.isUpgradeFailed}}
                    <li {{bindAttr class="view.noActionAvailable"}}>
                      <a href="javascript:void(null)" data-toggle="modal" {{action "upgradeComponent" view.content target="controller"}}>
                        {{t common.reUpgrade}}
                      </a>
                    </li>
                  {{/if}}
                  {{#if view.isInstallFailed}}
                    <li {{bindAttr class="view.noActionAvailable"}}>
                      <a href="javascript:void(null)" data-toggle="modal" {{action "installComponent" view.content target="controller"}}>
                        {{t common.reinstall}}
                      </a>
                    </li>
                   {{/if}}
                  <li {{bindAttr class="view.isStart::hidden"}}>
                    <a href="javascript:void(null)" data-toggle="modal" {{action "stopComponent" view.content target="controller"}}>
                      {{t common.stop}}
                    </a>
                  </li>
                  <li {{bindAttr class="view.isStart:hidden: view.isInstallFailed:hidden: view.isDataNodeRecommissionAvailable:hidden"}}>
                    <a href="javascript:void(null)" data-toggle="modal" {{action "startComponent" view.content target="controller"}}>
                      {{t common.start}}
                    </a>
                  </li>
                  <li {{bindAttr class="view.isUpgradeFailed::hidden"}}>
                    <a href="javascript:void(null)" data-toggle="modal" {{action "upgradeComponent" view.content target="controller"}}>
                      {{t common.upgrade}}
                    </a>
                  </li>
                  <li {{bindAttr class="view.isInstallFailed::hidden"}}>
                    <a href="javascript:void(null)" data-toggle="modal" {{action "installComponent" view.content target="controller"}}>
                      {{t common.install}}
                    </a>
                  </li>
                 </ul>
               </div>
               {{/if}}
@@ -165,4 +182,4 @@
 	  </div>
   </div>
 </div>
</div>
</div>
\ No newline at end of file
diff --git a/ambari-web/app/templates/wizard/step9HostTasksLogPopup.hbs b/ambari-web/app/templates/wizard/step9HostTasksLogPopup.hbs
index 9765ee8403..d3c3a7018f 100644
-- a/ambari-web/app/templates/wizard/step9HostTasksLogPopup.hbs
++ b/ambari-web/app/templates/wizard/step9HostTasksLogPopup.hbs
@@ -55,8 +55,8 @@
       <span class="task-detail-log-rolename" >{{{view.openedTask.role}} {{view.openedTask.command}}</span>
     </div>
     <div class="task-detail-ico-wrap">
      <div title="Click to Copy" {{action "textTrigger" taskInfo target="view"}} class="task-detail-copy"><i class="icon-copy"></i> {{t common.copy}}</div>
      <div title="Open in New Window" {{action openTaskLogInDialog}} class="task-detail-open-dialog"><i class="icon-external-link"></i> {{t common.open}}</div>
      <a title="Click to Copy" {{action "textTrigger" taskInfo target="view"}} class="task-detail-copy"><i class="icon-copy"></i> {{t common.copy}}</a>
      <a title="Open in New Window" {{action openTaskLogInDialog}} class="task-detail-open-dialog"><i class="icon-external-link"></i> {{t common.open}}</a>
     </div>
   </div>
   <div class="task-detail-log-info">
diff --git a/ambari-web/app/utils/config.js b/ambari-web/app/utils/config.js
index 2710a52c12..d7d24138aa 100644
-- a/ambari-web/app/utils/config.js
++ b/ambari-web/app/utils/config.js
@@ -37,9 +37,9 @@ App.config = Em.Object.create({
   configMapping: function() {
       if (stringUtils.compareVersions(App.get('currentStackVersionNumber'), "2.0") === 1 ||
         stringUtils.compareVersions(App.get('currentStackVersionNumber'), "2.0") === 0) {
        return require('data/config_mapping');
        return require('data/HDP2/config_mapping');
       }
      return require('data/HDP2/config_mapping');
    return require('data/config_mapping');
   }.property('App.currentStackVersionNumber'),
   preDefinedConfigProperties: function() {
     if (stringUtils.compareVersions(App.get('currentStackVersionNumber'), "2.0") === 1 ||
@@ -154,15 +154,16 @@ App.config = Em.Object.create({
       properties = (properties.length) ? properties.objectAt(0).properties : {};
       for (var index in properties) {
         var configsPropertyDef = preDefinedConfigs.findProperty('name', index) || null;
        var serviceConfigObj = {
        var serviceConfigObj = App.ServiceConfig.create({
           name: index,
           value: properties[index],
           defaultValue: properties[index],
           filename: _tag.siteName + ".xml",
           isUserProperty: false,
           isOverridable: true,
          serviceName: serviceName
        };
          serviceName: serviceName,
          belongsToService: []
        });
 
         if (configsPropertyDef) {
           serviceConfigObj.displayType = configsPropertyDef.displayType;
@@ -174,6 +175,7 @@ App.config = Em.Object.create({
           serviceConfigObj.isOverridable = configsPropertyDef.isOverridable === undefined ? true : configsPropertyDef.isOverridable;
           serviceConfigObj.serviceName = configsPropertyDef ? configsPropertyDef.serviceName : null;
           serviceConfigObj.index = configsPropertyDef.index;
          serviceConfigObj.belongsToService = configsPropertyDef.belongsToService;
         }
         // MAPREDUCE contains core-site properties but doesn't show them
         if(serviceConfigObj.serviceName === 'MAPREDUCE' && serviceConfigObj.filename === 'core-site.xml'){
@@ -367,6 +369,14 @@ App.config = Em.Object.create({
         break;
     }
   },

  miscConfigVisibleProperty: function (configs, serviceToShow) {
    configs.forEach(function(item) {
      item.set("isVisible", item.belongsToService.some(function(cur){return serviceToShow.contains(cur)}));
    });
    return configs;
  },

   /**
    * render configs, distribute them by service
    * and wrap each in ServiceConfigProperty object
diff --git a/ambari-web/app/utils/db.js b/ambari-web/app/utils/db.js
index 061fb55400..1be1292487 100644
-- a/ambari-web/app/utils/db.js
++ b/ambari-web/app/utils/db.js
@@ -49,7 +49,13 @@ App.db.cleanUp = function () {
   App.db.data = {
     'app': {
       'loginName': '',
      'authenticated': false
      'authenticated': false,
      'tables': {
        'filterConditions': {},
        'displayLength': {},
        'startIndex': {},
        'sortingConditions': {}
      }
     },
 
     'Installer' : {},
@@ -111,6 +117,46 @@ App.db.setAuthenticated = function (authenticated) {
   console.log('Now present value of authentication is: ' + App.db.data.app.authenticated);
 };
 
App.db.setFilterConditions = function(name, filterConditions) {
  console.log('TRACE: Entering db:setFilterConditions function');
  App.db.data = localStorage.getObject('ambari');
  if (!App.db.data.app.tables.filterConditions) {
    App.db.data.app.tables.filterConditions = {};
  }
  App.db.data.app.tables.filterConditions[name] = filterConditions;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setDisplayLength = function(name, displayLength) {
  console.log('TRACE: Entering db:setDisplayLength function');
  App.db.data = localStorage.getObject('ambari');
  if (!App.db.data.app.tables.displayLength) {
    App.db.data.app.tables.displayLength = {};
  }
  App.db.data.app.tables.displayLength[name] = displayLength;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setStartIndex = function(name, startIndex) {
  console.log('TRACE: Entering db:setStartIndex function');
  App.db.data = localStorage.getObject('ambari');
  if (!App.db.data.app.tables.startIndex) {
    App.db.data.app.tables.startIndex = {};
  }
  App.db.data.app.tables.startIndex[name] = startIndex;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setSortingStatuses = function(name, sortingConditions) {
  console.log('TRACE: Entering db:setSortingConditions function');
  App.db.data = localStorage.getObject('ambari');
  if (!App.db.data.app.tables.sortingConditions) {
    App.db.data.app.tables.sortingConditions = {};
  }
  App.db.data.app.tables.sortingConditions[name] = sortingConditions;
  localStorage.setObject('ambari', App.db.data);
};

 App.db.setAllHostNames = function (hostNames) {
   console.log('TRACE: Entering db:setAllHostNames function');
   App.db.data = localStorage.getObject('ambari');
@@ -297,6 +343,42 @@ App.db.getAmbariStacks = function () {
   return App.db.data.app.stacks;
 };
 
App.db.getFilterConditions = function(name) {
  console.log('TRACE: Entering db:getFilterConditions function');
  App.db.data = localStorage.getObject('ambari');
  if (App.db.data.app.tables.filterConditions[name]) {
    return App.db.data.app.tables.filterConditions[name];
  }
  return null;
};

App.db.getDisplayLength = function(name) {
  console.log('TRACE: Entering db:getDisplayLength function');
  App.db.data = localStorage.getObject('ambari');
  if (App.db.data.app.tables.displayLength[name]) {
    return App.db.data.app.tables.displayLength[name];
  }
  return null;
};

App.db.getStartIndex = function(name) {
  console.log('TRACE: Entering db:getStartIndex function');
  App.db.data = localStorage.getObject('ambari');
  if (App.db.data.app.tables.startIndex[name]) {
    return App.db.data.app.tables.startIndex[name];
  }
  return null;
};

App.db.getSortingStatuses = function(name) {
  console.log('TRACE: Entering db:getSortingConditions function');
  App.db.data = localStorage.getObject('ambari');
  if (App.db.data.app.tables.sortingConditions[name]) {
    return App.db.data.app.tables.sortingConditions[name];
  }
  return null;
};

 /**
  * Return current step for specified Wizard Type
  * @param wizardType
diff --git a/ambari-web/app/views/common/configs/services_config.js b/ambari-web/app/views/common/configs/services_config.js
index f76f9057b4..b1f873ad8c 100644
-- a/ambari-web/app/views/common/configs/services_config.js
++ b/ambari-web/app/views/common/configs/services_config.js
@@ -189,7 +189,7 @@ App.ServiceConfigsByCategoryView = Ember.View.extend({
         var name = this.get('name');
         if(name.trim() != ""){
           if(validator.isValidConfigKey(name)){
            var configMappingProperty = App.config.configMapping.all().findProperty('name', name);
            var configMappingProperty = App.config.get('configMapping').all().findProperty('name', name);
             if((configMappingProperty == null) && (!serviceConfigNames.contains(name))){
               this.set("isKeyError", false);
               this.set("errorMessage", "");
diff --git a/ambari-web/app/views/common/filter_view.js b/ambari-web/app/views/common/filter_view.js
index 69eff56f8c..942c8a2673 100644
-- a/ambari-web/app/views/common/filter_view.js
++ b/ambari-web/app/views/common/filter_view.js
@@ -32,6 +32,11 @@ var wrapperView = Ember.View.extend({
 
   value: null,
 
  /**
   * Column index
   */
  column: null,

   /**
    * If this field is exists we dynamically create hidden input element and set value there.
    * Used for some cases, where this values will be used outside of component
diff --git a/ambari-web/app/views/common/quick_view_link_view.js b/ambari-web/app/views/common/quick_view_link_view.js
index 18647bdccb..09708f57c4 100644
-- a/ambari-web/app/views/common/quick_view_link_view.js
++ b/ambari-web/app/views/common/quick_view_link_view.js
@@ -29,13 +29,18 @@ App.QuickViewLinks = Em.View.extend({
     var host;
 
     if (serviceName === 'HDFS') {
      host = components.findProperty('componentName', 'NAMENODE').get('host.publicHostName');
      host = App.singleNodeInstall ? App.singleNodeAlias : components.findProperty('componentName', 'NAMENODE').get('host.publicHostName');
     } else if (serviceName === 'MAPREDUCE') {
      host = components.findProperty('componentName', 'JOBTRACKER').get('host.publicHostName');
      host = App.singleNodeInstall ? App.singleNodeAlias : components.findProperty('componentName', 'JOBTRACKER').get('host.publicHostName');
     } else if (serviceName === 'HBASE') {
       var component = components.filterProperty('componentName', 'HBASE_MASTER').findProperty('haStatus', 'active');
       if(component){
        host = component.get('host.publicHostName');
        if(App.singleNodeInstall){
          host = App.singleNodeAlias;
        }else{
          host = component.get('host.publicHostName');
        }

       }
     }
     if (!host) {
diff --git a/ambari-web/app/views/common/sort_view.js b/ambari-web/app/views/common/sort_view.js
index 49ab2981ba..40bec6b12c 100644
-- a/ambari-web/app/views/common/sort_view.js
++ b/ambari-web/app/views/common/sort_view.js
@@ -17,6 +17,7 @@
  */
 
 var misc = require('utils/misc');
var App = require('app');
 
 /**
  * Wrapper View for all sort components. Layout template and common actions are located inside of it.
@@ -27,6 +28,51 @@ var misc = require('utils/misc');
  */
 var wrapperView = Em.View.extend({
   tagName: 'tr',

  /**
   * Load sort statuses from local storage
   * Works only after finish filtering in the parent View
   */
  loadSortStatuses: function() {
    var statuses = App.db.getSortingStatuses(this.get('controller.name'));
    if (!this.get('parentView.filteringComplete')) return;
    if (statuses) {
      var childViews = this.get('childViews');
      var self = this;
      statuses.forEach(function(st) {
        if (st.status != 'sorting') {
          var sortOrder = false;
          if(st.status == 'sorting_desc') {
            sortOrder = true;
          }
          self.sort(childViews.findProperty('name', st.name), sortOrder);

          childViews.findProperty('name', st.name).set('status', (sortOrder)?'sorting_desc':'sorting_asc');
        }
        else {
          childViews.findProperty('name', st.name).set('status', st.status);
        }
      });
      this.get('parentView').showProperPage();
    }
  }.observes('parentView.filteringComplete'),

  /**
   * Save sort statuses to local storage
   * Works only after finish filtering in the parent View
   */
  saveSortStatuses: function() {
    if (!this.get('parentView.filteringComplete')) return;
    var statuses = [];
    this.get('childViews').forEach(function(childView) {
      statuses.push({
        name: childView.get('name'),
        status: childView.get('status')
      });
    });
    App.db.setSortingStatuses(this.get('controller.name'), statuses);
  }.observes('childViews.@each.status'),

   /**
    * sort content by property
    * @param property
@@ -119,7 +165,8 @@ var fieldView = Em.View.extend({
     if(this.get('status') === 'sorting_desc'){
       this.get('parentView').sort(this, false);
       this.set('status', 'sorting_asc');
    } else {
    }
    else {
       this.get('parentView').sort(this, true);
       this.set('status', 'sorting_desc');
     }
diff --git a/ambari-web/app/views/common/table_view.js b/ambari-web/app/views/common/table_view.js
index 606874aa4a..2f82d84dc1 100644
-- a/ambari-web/app/views/common/table_view.js
++ b/ambari-web/app/views/common/table_view.js
@@ -22,13 +22,74 @@ var sort = require('views/common/sort_view');
 
 App.TableView = Em.View.extend({
 
  didInsertElement: function () {
    this.set('filterConditions', []);
    this.filter();
  /**
   * Shows if all data is loaded and filtered
   */
  filteringComplete: false,

  /**
   * Loaded from local storage startIndex value
   */
  startIndexOnLoad: null,
  /**
   * Loaded from local storage displayLength value
   */
  displayLengthOnLoad: null,

  /**
   * Do filtering, using saved in the local storage filter conditions
   */
  willInsertElement:function () {
    var self = this;
    var name = this.get('controller.name');

    this.set('startIndexOnLoad', App.db.getStartIndex(name));
    this.set('displayLengthOnLoad', App.db.getDisplayLength(name));

    var filterConditions = App.db.getFilterConditions(name);
    if (filterConditions) {
      this.set('filterConditions', filterConditions);

      var childViews = this.get('childViews');

      filterConditions.forEach(function(condition) {
        var view = childViews.findProperty('column', condition.iColumn);
        if (view) {
          view.set('value', condition.value);
          Em.run.next(function() {
            view.showClearFilter();
          });
        }
      });
    }

    Em.run.next(function() {
      Em.run.next(function() {
        self.set('filteringComplete', true);
      });
    });
   },
 
   /**
   * return pagination information displayed on the mirroring page
   * Do pagination after filtering and sorting
   * Don't call this method! It's already used where it's need
   */
  showProperPage: function() {
    var self = this;
    Em.run.next(function() {
      Em.run.next(function() {
        if (self.get('displayLengthOnLoad')) {
          self.set('displayLength', self.get('displayLengthOnLoad'));
        }
        if(self.get('startIndexOnLoad')) {
          self.set('startIndex', self.get('startIndexOnLoad'));
        }
      });
    });
  },

  /**
   * return pagination information displayed on the page
    */
   paginationInfo: function () {
     return this.t('apps.filters.paginationInfo').format(this.get('startIndex'), this.get('endIndex'), this.get('filteredContent.length'));
@@ -70,16 +131,16 @@ App.TableView = Em.View.extend({
     content: ['10', '25', '50']
   }),
 
  // start index for displayed content on the mirroring page
  // start index for displayed content on the page
   startIndex: 1,
 
  // calculate end index for displayed content on the mirroring page
  // calculate end index for displayed content on the page
   endIndex: function () {
     return Math.min(this.get('filteredContent.length'), this.get('startIndex') + parseInt(this.get('displayLength')) - 1);
   }.property('startIndex', 'displayLength', 'filteredContent.length'),
 
   /**
   * onclick handler for previous page button on the mirroring page
   * onclick handler for previous page button on the page
    */
   previousPage: function () {
     var result = this.get('startIndex') - parseInt(this.get('displayLength'));
@@ -90,7 +151,7 @@ App.TableView = Em.View.extend({
   },
 
   /**
   * onclick handler for next page button on the mirroring page
   * onclick handler for next page button on the page
    */
   nextPage: function () {
     var result = this.get('startIndex') + parseInt(this.get('displayLength'));
@@ -99,7 +160,7 @@ App.TableView = Em.View.extend({
     }
   },
 
  // the number of mirroring to show on every page of the mirroring page view
  // the number of rows to show on every page
   displayLength: null,
 
   // calculates default value for startIndex property after applying filter or changing displayLength
@@ -112,22 +173,54 @@ App.TableView = Em.View.extend({
    *
    * @param iColumn number of column by which filter
    * @param value
   * @param type
    */
   updateFilter: function (iColumn, value, type) {
     var filterCondition = this.get('filterConditions').findProperty('iColumn', iColumn);
     if (filterCondition) {
       filterCondition.value = value;
    } else {
    }
    else {
       filterCondition = {
         iColumn: iColumn,
         value: value,
         type: type
      }
      };
       this.get('filterConditions').push(filterCondition);
     }
    this.saveFilterConditions();
     this.filter();
   },
 
  saveFilterConditions: function() {
    App.db.setFilterConditions(this.get('controller.name'), this.get('filterConditions'));
  },

  saveDisplayLength: function() {
    var self = this;
    Em.run.next(function() {
      App.db.setDisplayLength(self.get('controller.name'), self.get('displayLength'));
    });
  }.observes('displayLength'),

  saveStartIndex: function() {
    if (this.get('filteringComplete')) {
      App.db.setStartIndex(this.get('controller.name'), this.get('startIndex'));
    }
  }.observes('startIndex'),

  clearFilterCondition: function() {
    App.db.setFilterConditions(this.get('controller.name'), null);
  },

  clearDisplayLength: function() {
    App.db.setDisplayLength(this.get('controller.name'), null);
  },

  clearStartIndex: function() {
    App.db.setStartIndex(this.get('controller.name'), null);
  },

   /**
    * contain filter conditions for each column
    */
@@ -135,7 +228,7 @@ App.TableView = Em.View.extend({
 
   filteredContent: [],
 
  // contain content to show on the current page of mirroring page view
  // contain content to show on the current page of data page view
   pageContent: function () {
     return this.get('filteredContent').slice(this.get('startIndex') - 1, this.get('endIndex'));
   }.property('filteredContent.length', 'startIndex', 'endIndex'),
diff --git a/ambari-web/app/views/main/dashboard/service/hbase.js b/ambari-web/app/views/main/dashboard/service/hbase.js
index 2642a9068f..72578ee2ee 100644
-- a/ambari-web/app/views/main/dashboard/service/hbase.js
++ b/ambari-web/app/views/main/dashboard/service/hbase.js
@@ -80,7 +80,7 @@ App.MainDashboardServiceHbaseView = App.MainDashboardServiceView.extend({
 
   hbaseMasterWebUrl: function () {
     if (this.get('activeMaster.host') && this.get('activeMaster.host').get('publicHostName')) {
      return "http://" + this.get('activeMaster.host').get('publicHostName') + ":60010";
      return "http://" + (App.singleNodeInstall ? App.singleNodeAlias : this.get('activeMaster.host').get('publicHostName')) + ":60010";
     }
   }.property('activeMaster'),
 
diff --git a/ambari-web/app/views/main/dashboard/service/hdfs.js b/ambari-web/app/views/main/dashboard/service/hdfs.js
index 9fa4a3a812..ce21b59393 100644
-- a/ambari-web/app/views/main/dashboard/service/hdfs.js
++ b/ambari-web/app/views/main/dashboard/service/hdfs.js
@@ -70,7 +70,7 @@ App.MainDashboardServiceHdfsView = App.MainDashboardServiceView.extend({
   }.property("service.nameNodeStartTime"),
 
   nodeWebUrl: function () {
    return "http://" + this.get('service').get('nameNode').get('publicHostName') + ":50070";
    return "http://" + (App.singleNodeInstall ? App.singleNodeAlias :  this.get('service').get('nameNode').get('publicHostName')) + ":50070";
   }.property('service.nameNode'),
 
   nodeHeap: function () {
diff --git a/ambari-web/app/views/main/dashboard/service/mapreduce.js b/ambari-web/app/views/main/dashboard/service/mapreduce.js
index 94ddd0d338..e3b9f18b8e 100644
-- a/ambari-web/app/views/main/dashboard/service/mapreduce.js
++ b/ambari-web/app/views/main/dashboard/service/mapreduce.js
@@ -22,7 +22,7 @@ App.MainDashboardServiceMapreduceView = App.MainDashboardServiceView.extend({
   templateName: require('templates/main/dashboard/service/mapreduce'),
   serviceName: 'MAPREDUCE',
   jobTrackerWebUrl: function () {
    return "http://" + this.get('service').get('jobTracker').get('publicHostName') + ":50030";
    return "http://" + (App.singleNodeInstall ? App.singleNodeAlias : this.get('service').get('jobTracker').get('publicHostName')) + ":50030";
   }.property('service.nameNode'),
 
   Chart: App.ChartLinearView.extend({
diff --git a/ambari-web/app/views/main/dashboard/service/oozie.js b/ambari-web/app/views/main/dashboard/service/oozie.js
index 9c7d8db426..06b87d7c6f 100644
-- a/ambari-web/app/views/main/dashboard/service/oozie.js
++ b/ambari-web/app/views/main/dashboard/service/oozie.js
@@ -23,7 +23,7 @@ App.MainDashboardServiceOozieView = App.MainDashboardServiceView.extend({
   templateName: require('templates/main/dashboard/service/oozie'),
 
   webUi: function () {
    var hostName = this.get('service.hostComponents').findProperty('componentName', 'OOZIE_SERVER').get('host.publicHostName');
    var hostName = App.singleNodeInstall ? App.singleNodeAlias : this.get('service.hostComponents').findProperty('componentName', 'OOZIE_SERVER').get('host.publicHostName');
     return "http://{0}:11000/oozie".format(hostName);
   }.property('service')
 });
\ No newline at end of file
diff --git a/ambari-web/app/views/main/host.js b/ambari-web/app/views/main/host.js
index c35eb2d313..6c122ae024 100644
-- a/ambari-web/app/views/main/host.js
++ b/ambari-web/app/views/main/host.js
@@ -27,7 +27,7 @@ App.MainHostView = App.TableView.extend({
     return this.get('controller.content');
   }.property('controller.content.length'),
 
  didInsertElement:function () {
  willInsertElement: function() {
     this._super();
   },
 
@@ -60,6 +60,7 @@ App.MainHostView = App.TableView.extend({
     displayName: Em.I18n.t('common.loadAvg'),
     type: 'number'
   }),

   HostView:Em.View.extend({
     content:null,
     tagName: 'tr',
@@ -203,8 +204,9 @@ App.MainHostView = App.TableView.extend({
    * Based on <code>filters</code> library
    */
   nameFilterView: filters.createTextView({
    column: 1,
     onChangeValue: function(){
      this.get('parentView').updateFilter(1, this.get('value'), 'string');
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'string');
     }
   }),
 
@@ -213,8 +215,9 @@ App.MainHostView = App.TableView.extend({
    * Based on <code>filters</code> library
    */
   ipFilterView: filters.createTextView({
    column: 2,
     onChangeValue: function(){
      this.get('parentView').updateFilter(2, this.get('value'), 'string');
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'string');
     }
   }),
 
@@ -225,8 +228,9 @@ App.MainHostView = App.TableView.extend({
   cpuFilterView: filters.createTextView({
     fieldType: 'input-mini',
     fieldId: 'cpu_filter',
    column: 3,
     onChangeValue: function(){
      this.get('parentView').updateFilter(3, this.get('value'), 'number');
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'number');
     }
   }),
 
@@ -237,8 +241,9 @@ App.MainHostView = App.TableView.extend({
   loadAvgFilterView: filters.createTextView({
     fieldType: 'input-mini',
     fieldId: 'load_avg_filter',
    column: 5,
     onChangeValue: function(){
      this.get('parentView').updateFilter(5, this.get('value'), 'number');
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'number');
     }
   }),
 
@@ -249,8 +254,9 @@ App.MainHostView = App.TableView.extend({
   ramFilterView: filters.createTextView({
     fieldType: 'input-mini',
     fieldId: 'ram_filter',
    column: 4,
     onChangeValue: function(){
      this.get('parentView').updateFilter(4, this.get('value'), 'ambari-bandwidth');
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'ambari-bandwidth');
     }
   }),
 
@@ -259,6 +265,9 @@ App.MainHostView = App.TableView.extend({
    * Based on <code>filters</code> library
    */
   componentsFilterView: filters.createComponentView({

    column: 6,

     /**
      * Inner FilterView. Used just to render component. Value bind to <code>mainview.value</code> property
      * Base methods was implemented in <code>filters.componentFieldView</code>
@@ -318,7 +327,7 @@ App.MainHostView = App.TableView.extend({
        */
       applyFilter:function() {
         this._super();

        var self = this;
         var chosenComponents = [];
 
         this.get('masterComponents').filterProperty('checkedForHostFilter', true).forEach(function(item){
@@ -330,21 +339,42 @@ App.MainHostView = App.TableView.extend({
         this.get('clientComponents').filterProperty('checkedForHostFilter', true).forEach(function(item){
           chosenComponents.push(item.get('id'));
         });
        this.set('value', chosenComponents.toString());
        Em.run.next(function() {
          self.set('value', chosenComponents.toString());
        });
       },
 
      didInsertElement:function () {
        if (this.get('controller.comeWithFilter')) {
          this.applyFilter();
          this.set('controller.comeWithFilter', false);
        } else {
          this.clearFilter();
      /**
       * Verify that checked checkboxes are equal to value stored in hidden field (components ids list)
       */
      checkComponents: function() {
        var components = this.get('value').split(',');
        var self = this;
        if (components) {
          components.forEach(function(componentId) {
            if(!self.tryCheckComponent(self, 'masterComponents', componentId)) {
              if(!self.tryCheckComponent(self, 'slaveComponents', componentId)) {
                self.tryCheckComponent(self, 'clientComponents', componentId);
              }
            }
          });
        }
      }.observes('value'),

      tryCheckComponent: function(self, category, componentId) {
        var c = self.get(category).findProperty('id', componentId);
        if (c) {
          if (!c.get('checkedForHostFilter')) {
            c.set('checkedForHostFilter', true);
            return true;
          }
         }
        return false;
       }
 
     }),
     onChangeValue: function(){
      this.get('parentView').updateFilter(6, this.get('value'), 'multiple');
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'multiple');
     }
   }),
 
diff --git a/ambari-web/app/views/main/host/summary.js b/ambari-web/app/views/main/host/summary.js
index aead99a17c..de7c509ce4 100644
-- a/ambari-web/app/views/main/host/summary.js
++ b/ambari-web/app/views/main/host/summary.js
@@ -91,14 +91,14 @@ App.MainHostSummaryView = Em.View.extend({
   didInsertElement: function () {
     this.loadDecommissionNodesList();
   },
  sortedComponents: function() {
  sortedComponents: function () {
     var slaveComponents = [];
     var masterComponents = [];
    this.get('content.hostComponents').forEach(function(component){
      if(component.get('workStatus') != 'INSTALLING'){
        if(component.get('isMaster')){
    this.get('content.hostComponents').forEach(function (component) {
      if (component.get('workStatus') != 'INSTALLING') {
        if (component.get('isMaster')) {
           masterComponents.push(component);
        } else if(component.get('isSlave')) {
        } else if (component.get('isSlave')) {
           slaveComponents.push(component);
         }
       }
@@ -106,16 +106,16 @@ App.MainHostSummaryView = Em.View.extend({
     }, this);
     return masterComponents.concat(slaveComponents);
   }.property('content', 'content.hostComponents.length'),
  clients: function(){
  clients: function () {
     var clients = [];
    this.get('content.hostComponents').forEach(function(component){
      if(!component.get('componentName')){
    this.get('content.hostComponents').forEach(function (component) {
      if (!component.get('componentName')) {
         //temporary fix because of different data in hostComponents and serviceComponents
         return;
       }
       if (!component.get('isSlave') && !component.get('isMaster')) {
         if (clients.length) {
          clients[clients.length-1].set('isLast', false);
          clients[clients.length - 1].set('isLast', false);
         }
         component.set('isLast', true);
         clients.push(component);
@@ -126,21 +126,21 @@ App.MainHostSummaryView = Em.View.extend({
 
   addableComponentObject: Em.Object.extend({
     componentName: '',
    displayName: function(){
    displayName: function () {
       return App.format.role(this.get('componentName'));
     }.property('componentName')
   }),
  isAddComponent: function(){
  isAddComponent: function () {
     return this.get('content.healthClass') !== 'health-status-DEAD-YELLOW';
   }.property('content.healthClass'),
  addableComponents:function(){
  addableComponents: function () {
     var components = [];
     var services = App.Service.find();
     var dataNodeExists = false;
     var taskTrackerExists = false;
     var regionServerExists = false;
 
    this.get('content.hostComponents').forEach(function(component) {
    this.get('content.hostComponents').forEach(function (component) {
       switch (component.get('componentName')) {
         case 'DATANODE':
           dataNodeExists = true;
@@ -173,74 +173,96 @@ App.MainHostSummaryView = Em.View.extend({
         this.doBlinking();
       }
     },
    hostComponent: function(){
    hostComponent: function () {
       var hostComponent = null;
       var serviceComponent = this.get('content');
       var host = App.router.get('mainHostDetailsController.content');
      if(host){
      if (host) {
         hostComponent = host.get('hostComponents').findProperty('componentName', serviceComponent.get('componentName'));
       }
       return hostComponent;
     }.property('content', 'App.router.mainHostDetailsController.content'),
    workStatus: function(){
    workStatus: function () {
       var workStatus = this.get('content.workStatus');
       var hostComponent = this.get('hostComponent');
      if(hostComponent){
      if (hostComponent) {
         workStatus = hostComponent.get('workStatus');
       }
       return workStatus;
    }.property('content.workStatus','hostComponent.workStatus'),
    statusClass: function(){
    }.property('content.workStatus', 'hostComponent.workStatus'),

    /**
     * Return host component text status
     */
    componentTextStatus: function () {
      var workStatus = this.get("workStatus");
      var componentTextStatus = this.get('content.componentTextStatus');
      var hostComponent = this.get('hostComponent');
      if (hostComponent) {
        componentTextStatus = hostComponent.get('componentTextStatus');
        if(this.get("isDataNode"))
          if(this.get('isDataNodeRecommissionAvailable')){
            if(App.HostComponentStatus.started == workStatus){
              componentTextStatus = "Decommissioning...";
            }else if(App.HostComponentStatus.stopped == workStatus){
              componentTextStatus = "Decommissioned";
            }
          }
      }
      return componentTextStatus;
    }.property('workStatus','isDataNodeRecommissionAvailable'),

    statusClass: function () {
       var statusClass = null;
      if(this.get('isDataNode')){
        if(this.get('isDataNodeRecommissionAvailable') && this.get('isStart')){
          // Orange is shown only when service is started/starting and it is decommissioned.

      //If the component is DataNode
      if (this.get('isDataNode')) {
        if (this.get('isDataNodeRecommissionAvailable') && (this.get('isStart') || this.get('workStatus') == 'INSTALLED')) {
           return 'health-status-DEAD-ORANGE';
         }
       }
      if(this.get('workStatus') === App.HostComponentStatus.install_failed){
        return 'icon-remove';

      //Class when install failed
      if (this.get('workStatus') === App.HostComponentStatus.install_failed) {
        return 'health-status-color-red icon-cog';
       }

      //Class when installing
      if (this.get('workStatus') === App.HostComponentStatus.installing) {
        return 'health-status-color-blue icon-cog';
      }

      //For all other cases
       return 'health-status-' + App.HostComponentStatus.getKeyName(this.get('workStatus'));
    }.property('workStatus', 'isDataNodeRecommissionAvailable'),
    }.property('workStatus', 'isDataNodeRecommissionAvailable', 'this.content.isDecommissioning'),
     /**
      * For Upgrade failed state
      */
    isUpgradeFailed:function(){
    isUpgradeFailed: function () {
       return App.HostComponentStatus.getKeyName(this.get('workStatus')) === "upgrade_failed";
     }.property("workStatus"),
     /**
      * For Install failed state
      */
    isInstallFailed:function(){
    isInstallFailed: function () {
       return App.HostComponentStatus.getKeyName(this.get('workStatus')) === "install_failed";
     }.property("workStatus"),
    /**
     * Disable element while component is starting/stopping
     */
    disabledClass:function(){
      var workStatus = this.get('workStatus');
      if([App.HostComponentStatus.starting, App.HostComponentStatus.stopping, App.HostComponentStatus.unknown].contains(workStatus) ){
        return 'disabled';
      } else {
        return '';
      }
    }.property('workStatus'),
     /**
      * Do blinking for 1 minute
      */
    doBlinking : function(){
    doBlinking: function () {
       var workStatus = this.get('workStatus');
       var self = this;
       var pulsate = [ App.HostComponentStatus.starting, App.HostComponentStatus.stopping ].contains(workStatus);
       if (!pulsate && this.get('isDataNode')) {
         var dataNodeComponent = this.get('content');
        if (dataNodeComponent)
          pulsate = dataNodeComponent.get('isDecommissioning');
        if (dataNodeComponent && workStatus != "INSTALLED") {
          pulsate = this.get('isDataNodeRecommissionAvailable');
        }
       }
       if (pulsate && !self.get('isBlinking')) {
         self.set('isBlinking', true);
        uiEffects.pulsate(self.$('.components-health'), 1000, function(){
        uiEffects.pulsate(self.$('.components-health'), 1000, function () {
           !self.get('isDestroyed') && self.set('isBlinking', false);
           self.doBlinking();
         });
@@ -249,26 +271,42 @@ App.MainHostSummaryView = Em.View.extend({
     /**
      * Start blinking when host component is starting/stopping
      */
    startBlinking:function(){
    startBlinking: function () {
       this.$('.components-health').stop(true, true);
       this.$('.components-health').css({opacity: 1.0});
       this.doBlinking();
    }.observes('workStatus'),
    }.observes('workStatus','isDataNodeRecommissionAvailable'),
 
    isStart : function() {
      return (this.get('workStatus') === App.HostComponentStatus.started || this.get('workStatus') === App.HostComponentStatus.starting);
    isStart: function () {
      return (this.get('workStatus') == App.HostComponentStatus.started || this.get('workStatus') == App.HostComponentStatus.starting);
     }.property('workStatus'),
 
    isInProgress : function() {
      return (this.get('workStatus') === App.HostComponentStatus.stopping || this.get('workStatus') === App.HostComponentStatus.starting);
    /**
     * No action available while component is starting/stopping/unknown
     */
    noActionAvailable: function () {
      var workStatus = this.get('workStatus');
      if ([App.HostComponentStatus.starting, App.HostComponentStatus.stopping, App.HostComponentStatus.unknown].contains(workStatus)) {
        return "hidden";
      }else{
        return "";
      }
     }.property('workStatus'),

    isInProgress: function () {
      return (this.get('workStatus') === App.HostComponentStatus.stopping || this.get('workStatus') === App.HostComponentStatus.starting) || this.get('isDataNodeRecommissionAvailable');
    }.property('workStatus', 'isDataNodeRecommissionAvailable'),
     /**
      * Shows whether we need to show Decommision/Recomission buttons
      */
    isDataNode: function() {
    isDataNode: function () {
       return this.get('content.componentName') === 'DATANODE';
     }.property('content'),
 
    isDecommissioning: function () {
      return this.get('isDataNode') &&  this.get("isDataNodeRecommissionAvailable");
    }.property("workStatus", "isDataNodeRecommissionAvailable"),

     /**
      * Set in template via binding from parent view
      */
@@ -287,7 +325,7 @@ App.MainHostSummaryView = Em.View.extend({
     isDataNodeRecommissionAvailable: function () {
       var decommissionHostNames = this.get('decommissionDataNodeHostNames');
       var hostName = App.router.get('mainHostDetailsController.content.hostName');
      return decommissionHostNames!=null && decommissionHostNames.contains(hostName);
      return decommissionHostNames != null && decommissionHostNames.contains(hostName);
     }.property('App.router.mainHostDetailsController.content', 'decommissionDataNodeHostNames')
 
   }),
diff --git a/ambari-web/app/views/main/mirroring/datasets_view.js b/ambari-web/app/views/main/mirroring/datasets_view.js
index db5d3e345d..884fd05bd4 100644
-- a/ambari-web/app/views/main/mirroring/datasets_view.js
++ b/ambari-web/app/views/main/mirroring/datasets_view.js
@@ -83,45 +83,51 @@ App.MainDatasetsView = App.TableView.extend({
    */
   nameFilterView: filters.createTextView({
     fieldType: 'input-small',
    column: 1,
     onChangeValue: function () {
      this.get('parentView').updateFilter(1, this.get('value'), 'string');
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'string');
     }
   }),
 
   datasetSourceFilterView: filters.createTextView({
     fieldType: 'input-small',
    column: 2,
     onChangeValue: function () {
      this.get('parentView').updateFilter(2, this.get('value'), 'string');
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'string');
     }
   }),
 
   lastSuccessFilterView: filters.createSelectView({
     fieldType: 'input-medium',
    column: 3,
     content: ['Any', 'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days'],
     onChangeValue: function () {
      this.get('parentView').updateFilter(3, this.get('value'), 'date');
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'date');
     }
   }),
 
   lastFailFilterView: filters.createSelectView({
     fieldType: 'input-medium',
    column: 4,
     content: ['Any', 'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days'],
     onChangeValue: function () {
      this.get('parentView').updateFilter(4, this.get('value'), 'date');
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'date');
     }
   }),
 
   lastDurationFilterView: filters.createTextView({
     fieldType: 'input-small',
    column: 5,
     onChangeValue: function () {
      this.get('parentView').updateFilter(5, this.get('value'), 'duration');
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'duration');
     }
   }),
 
   avgDataFilterView: filters.createTextView({
     fieldType: 'input-small',
    column: 6,
     onChangeValue: function () {
      this.get('parentView').updateFilter(6, this.get('value'), 'ambari-bandwidth');
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'ambari-bandwidth');
     }
   }),
 
diff --git a/ambari-web/app/views/main/mirroring/jobs_view.js b/ambari-web/app/views/main/mirroring/jobs_view.js
index cbf972323a..bd9579c8b2 100644
-- a/ambari-web/app/views/main/mirroring/jobs_view.js
++ b/ambari-web/app/views/main/mirroring/jobs_view.js
@@ -67,38 +67,43 @@ App.MainJobsView = App.TableView.extend({
    */
   idFilterView: filters.createTextView({
     fieldType: 'input-small',
    column: 1,
     onChangeValue: function () {
      this.get('parentView').updateFilter(1, this.get('value'), 'number');
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'number');
     }
   }),
 
   startFilterView: filters.createSelectView({
     fieldType: 'input-small',
    column: 2,
     content: ['Any', 'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days'],
     onChangeValue: function () {
      this.get('parentView').updateFilter(2, this.get('value'), 'date');
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'date');
     }
   }),
 
   endFilterView: filters.createSelectView({
     fieldType: 'input-medium',
    column: 3,
     content: ['Any', 'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days'],
     onChangeValue: function () {
      this.get('parentView').updateFilter(3, this.get('value'), 'date');
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'date');
     }
   }),
 
   durationFilterView: filters.createTextView({
     fieldType: 'input-medium',
    column: 4,
     onChangeValue: function () {
      this.get('parentView').updateFilter(4, this.get('value'), 'duration');
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'duration');
     }
   }),
 
   dataFilterView: filters.createTextView({
     fieldType: 'input-small',
    column: 5,
     onChangeValue: function () {
      this.get('parentView').updateFilter(5, this.get('value'), 'ambari-bandwidth');
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'ambari-bandwidth');
     }
   }),
 
diff --git a/ambari-web/app/views/main/service/info/summary.js b/ambari-web/app/views/main/service/info/summary.js
index d6c6ad647e..615d2d0089 100644
-- a/ambari-web/app/views/main/service/info/summary.js
++ b/ambari-web/app/views/main/service/info/summary.js
@@ -174,7 +174,7 @@ App.MainServiceInfoSummaryView = Em.View.extend({
   getServer: function(serviceName) {
     var service=this.get('controller.content');
     if(service.get("id") == serviceName) {
      return service.get("hostComponents").findProperty('isMaster', true).get("host").get("publicHostName");
      return (App.singleNodeInstall ? App.singleNodeAlias : service.get("hostComponents").findProperty('isMaster', true).get("host").get("publicHostName"));
     }
     else {
       return '';
diff --git a/pom.xml b/pom.xml
index a2f7224e23..720b590fc6 100644
-- a/pom.xml
++ b/pom.xml
@@ -51,7 +51,6 @@
     <module>ambari-project</module>
     <module>ambari-server</module>
     <module>ambari-agent</module>
    <module>ambari-client</module>
   </modules>
   <build>
     <pluginManagement>
- 
2.19.1.windows.1

