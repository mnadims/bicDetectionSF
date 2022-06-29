From ca01c28362ede63bac17e32af42809b1c90c5ce9 Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Sat, 12 Nov 2016 21:41:17 +0100
Subject: [PATCH] OOZIE-2666 Support embedding Jetty into Oozie (asasvari via
 rkanter)

--
 bin/mkdistro.sh                               |   2 +-
 core/src/main/conf/oozie-env.sh               |   6 +-
 .../org/apache/oozie/util/ConfigUtils.java    |  12 +-
 .../apache/oozie/util/Instrumentation.java    |   8 +-
 core/src/main/resources/oozie-default.xml     |  93 +++++-
 distro/pom.xml                                |  99 +++---
 distro/src/main/bin/oozie-jetty-server.sh     | 226 ++++++++++++++
 distro/src/main/bin/oozie-setup.sh            | 285 +++++++++++-------
 distro/src/main/bin/oozie-sys.sh              |  36 ++-
 distro/src/main/bin/oozie-tomcat-server.sh    |  89 ++++++
 distro/src/main/bin/oozied.sh                 |  98 ++----
 pom.xml                                       |  72 ++++-
 release-log.txt                               |   1 +
 server/pom.xml                                | 257 ++++++++++++++++
 server/src/main/assemblies/empty.xml          |  21 ++
 .../oozie/server/EmbeddedOozieServer.java     | 206 +++++++++++++
 .../org/apache/oozie/server/FilterMapper.java |  61 ++++
 .../server/HttpConfigurationWrapper.java      |  63 ++++
 .../org/apache/oozie/server/JspHandler.java   | 161 ++++++++++
 .../server/SSLServerConnectorFactory.java     | 136 +++++++++
 .../apache/oozie/server/ServletMapper.java    |  95 ++++++
 .../oozie/server/WebRootResourceLocator.java  |  39 +++
 .../ConstraintSecurityHandlerProvider.java    |  47 +++
 .../server/guice/JettyServerProvider.java     |  48 +++
 .../server/guice/JspHandlerProvider.java      |  47 +++
 .../oozie/server/guice/OozieGuiceModule.java  |  45 +++
 .../server/guice/RewriteHandlerProvider.java  |  44 +++
 .../oozie/server/guice/ServicesProvider.java  |  39 +++
 .../src/main/resources/checkstyle-header.txt  |  17 ++
 server/src/main/resources/checkstyle.xml      |  41 +++
 .../oozie/server/TestEmbeddedOozieServer.java | 119 ++++++++
 .../apache/oozie/server/TestJspHandler.java   |  94 ++++++
 .../server/TestSSLServerConnectorFactory.java | 137 +++++++++
 src/main/assemblies/distro-jetty.xml          | 155 ++++++++++
 .../{distro.xml => distro-tomcat.xml}         |   8 +-
 webapp/src/main/webapp/403.html               |  31 ++
 webapp/src/main/webapp/404.html               |  31 ++
 37 files changed, 2712 insertions(+), 257 deletions(-)
 create mode 100644 distro/src/main/bin/oozie-jetty-server.sh
 create mode 100644 distro/src/main/bin/oozie-tomcat-server.sh
 create mode 100644 server/pom.xml
 create mode 100644 server/src/main/assemblies/empty.xml
 create mode 100644 server/src/main/java/org/apache/oozie/server/EmbeddedOozieServer.java
 create mode 100644 server/src/main/java/org/apache/oozie/server/FilterMapper.java
 create mode 100644 server/src/main/java/org/apache/oozie/server/HttpConfigurationWrapper.java
 create mode 100644 server/src/main/java/org/apache/oozie/server/JspHandler.java
 create mode 100644 server/src/main/java/org/apache/oozie/server/SSLServerConnectorFactory.java
 create mode 100644 server/src/main/java/org/apache/oozie/server/ServletMapper.java
 create mode 100644 server/src/main/java/org/apache/oozie/server/WebRootResourceLocator.java
 create mode 100644 server/src/main/java/org/apache/oozie/server/guice/ConstraintSecurityHandlerProvider.java
 create mode 100644 server/src/main/java/org/apache/oozie/server/guice/JettyServerProvider.java
 create mode 100644 server/src/main/java/org/apache/oozie/server/guice/JspHandlerProvider.java
 create mode 100644 server/src/main/java/org/apache/oozie/server/guice/OozieGuiceModule.java
 create mode 100644 server/src/main/java/org/apache/oozie/server/guice/RewriteHandlerProvider.java
 create mode 100644 server/src/main/java/org/apache/oozie/server/guice/ServicesProvider.java
 create mode 100644 server/src/main/resources/checkstyle-header.txt
 create mode 100644 server/src/main/resources/checkstyle.xml
 create mode 100644 server/src/test/java/org/apache/oozie/server/TestEmbeddedOozieServer.java
 create mode 100644 server/src/test/java/org/apache/oozie/server/TestJspHandler.java
 create mode 100644 server/src/test/java/org/apache/oozie/server/TestSSLServerConnectorFactory.java
 create mode 100644 src/main/assemblies/distro-jetty.xml
 rename src/main/assemblies/{distro.xml => distro-tomcat.xml} (98%)
 create mode 100644 webapp/src/main/webapp/403.html
 create mode 100644 webapp/src/main/webapp/404.html

diff --git a/bin/mkdistro.sh b/bin/mkdistro.sh
index 1ccd495bd..e0cff3615 100755
-- a/bin/mkdistro.sh
++ b/bin/mkdistro.sh
@@ -70,7 +70,7 @@ MVN_OPTS="-Dbuild.time=${DATETIME} -Dvc.revision=${VC_REV} -Dvc.url=${VC_URL} -D
 export DATETIME2=`date -u "+%Y%m%d-%H%M%SGMT"`
 mvn clean package assembly:single ${MVN_OPTS} "$@"
 
if [ "$?" != "0" ]; then
if [ "$?" -ne "0" ]; then
   echo
   echo "ERROR, Oozie distro creation failed"
   echo
diff --git a/core/src/main/conf/oozie-env.sh b/core/src/main/conf/oozie-env.sh
index 390c955c0..bc8c60180 100644
-- a/core/src/main/conf/oozie-env.sh
++ b/core/src/main/conf/oozie-env.sh
@@ -22,7 +22,9 @@
 # Settings for the Embedded Tomcat that runs Oozie
 # Java System properties for Oozie should be specified in this variable
 #
export CATALINA_OPTS="$CATALINA_OPTS -Xmx1024m"
if [ "${OOZIE_USE_TOMCAT}" = "1" ]; then
  export CATALINA_OPTS="$CATALINA_OPTS -Xmx1024m"
fi
 
 # Oozie configuration file to load from Oozie configuration directory
 #
@@ -66,4 +68,4 @@ export CATALINA_OPTS="$CATALINA_OPTS -Xmx1024m"
 
 # The Oozie Instance ID
 #
# export OOZIE_INSTANCE_ID="${OOZIE_HTTP_HOSTNAME}"
\ No newline at end of file
# export OOZIE_INSTANCE_ID="${OOZIE_HTTP_HOSTNAME}"
diff --git a/core/src/main/java/org/apache/oozie/util/ConfigUtils.java b/core/src/main/java/org/apache/oozie/util/ConfigUtils.java
index a56c5a295..792723fd7 100644
-- a/core/src/main/java/org/apache/oozie/util/ConfigUtils.java
++ b/core/src/main/java/org/apache/oozie/util/ConfigUtils.java
@@ -28,6 +28,10 @@ import org.apache.oozie.servlet.ServicesLoader;
  */
 public class ConfigUtils {
     private final static XLog LOG = XLog.getLog(ConfigUtils.class);
    public static final String OOZIE_HTTPS_ENABLED = "oozie.https.enabled";
    public static final String OOZIE_HTTP_HOSTNAME = "oozie.http.hostname";
    public static final String OOZIE_HTTPS_PORT = "oozie.https.port";
    public static final String OOZIE_HTTP_PORT = "oozie.http.port";
 
     public static boolean BOOLEAN_DEFAULT = false;
     public static String STRING_DEFAULT = "";
@@ -92,13 +96,13 @@ public class ConfigUtils {
         else {
             sb.append("http://");
         }
        sb.append(ConfigurationService.get("oozie.http.hostname"));
        sb.append(ConfigurationService.get(OOZIE_HTTP_HOSTNAME));
         sb.append(":");
         if (secure) {
            sb.append(ConfigurationService.get("oozie.https.port"));
            sb.append(ConfigurationService.get(OOZIE_HTTPS_PORT));
         }
         else {
            sb.append(ConfigurationService.get("oozie.http.port"));
            sb.append(ConfigurationService.get(OOZIE_HTTP_PORT));
         }
         sb.append("/oozie");
         return sb.toString();
@@ -110,7 +114,7 @@ public class ConfigUtils {
      * @return the HTTP or HTTPS URL for this Oozie server
      */
     public static String getOozieEffectiveUrl() {
        return getOozieURL(ServicesLoader.isSSLEnabled());
        return getOozieURL(ServicesLoader.isSSLEnabled() || ConfigurationService.getBoolean(OOZIE_HTTPS_ENABLED));
     }
 
     public static boolean isBackwardSupportForCoordStatus() {
diff --git a/core/src/main/java/org/apache/oozie/util/Instrumentation.java b/core/src/main/java/org/apache/oozie/util/Instrumentation.java
index 99d64acec..55e00d4df 100644
-- a/core/src/main/java/org/apache/oozie/util/Instrumentation.java
++ b/core/src/main/java/org/apache/oozie/util/Instrumentation.java
@@ -784,9 +784,11 @@ public class Instrumentation {
             if (map.containsKey(name)) {
                 throw new RuntimeException(XLog.format("Sampler group=[{0}] name=[{1}] already defined", group, name));
             }
            Sampler sampler = new Sampler(period, interval, variable);
            map.put(name, sampler);
            scheduler.scheduleAtFixedRate(sampler, 0, sampler.getSamplingInterval(), TimeUnit.SECONDS);
            else {
                Sampler sampler = new Sampler(period, interval, variable);
                map.put(name, sampler);
                scheduler.scheduleAtFixedRate(sampler, 0, sampler.getSamplingInterval(), TimeUnit.SECONDS);
            }
         }
         finally {
             samplerLock.unlock();
diff --git a/core/src/main/resources/oozie-default.xml b/core/src/main/resources/oozie-default.xml
index e71ebe3b7..856564338 100644
-- a/core/src/main/resources/oozie-default.xml
++ b/core/src/main/resources/oozie-default.xml
@@ -2488,9 +2488,10 @@ will be the requeue interval for the actions which are waiting for a long time w
 
     <property>
         <name>oozie.http.hostname</name>
        <value>localhost</value>
        <value>0.0.0.0</value>
         <description>
            Oozie server host name.
            Oozie server host name. The network interface Oozie server binds to as an IP address or a hostname.
            Most users won't need to change this setting from the default value.
         </description>
     </property>
 
@@ -2502,6 +2503,22 @@ will be the requeue interval for the actions which are waiting for a long time w
         </description>
     </property>
 
    <property>
        <name>oozie.http.request.header.size</name>
        <value>65536</value>
        <description>
            Oozie HTTP request header size.
        </description>
    </property>

    <property>
        <name>oozie.http.response.header.size</name>
        <value>65536</value>
        <description>
            Oozie HTTP response header size.
        </description>
    </property>

     <property>
         <name>oozie.https.port</name>
         <value>11443</value>
@@ -2510,6 +2527,70 @@ will be the requeue interval for the actions which are waiting for a long time w
         </description>
     </property>
 
    <property>
        <name>oozie.https.enabled</name>
        <value>false</value>
        <description>
            Controls whether SSL encryption is enabled.
        </description>
    </property>

    <property>
        <name>oozie.https.truststore.file</name>
        <value>custom.truststore</value>
        <description>
            Path to a TrustStore file.
        </description>
    </property>

    <property>
        <name>oozie.https.truststore.pass</name>
        <value>cloudera</value>
        <description>
            Password to the TrustStore.
        </description>
    </property>

    <property>
        <name>oozie.https.keystore.file</name>
        <value></value>
        <description>
            Path to a KeyStore file.
        </description>
    </property>

    <property>
        <name>oozie.https.keystore.pass</name>
        <value></value>
        <description>
            Password to the KeyStore.
        </description>
    </property>

    <property>
        <name>oozie.https.include.protocols</name>
        <value>TLSv1,SSLv2Hello,TLSv1.1,TLSv1.2</value>
        <description>
            Enabled TLS protocols.
        </description>
    </property>

    <property>
        <name>oozie.https.exclude.cipher.suites</name>
        <value>TLS_ECDHE_RSA_WITH_RC4_128_SHA,SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA,SSL_RSA_WITH_DES_CBC_SHA,SSL_DHE_RSA_WITH_DES_CBC_SHA,SSL_RSA_EXPORT_WITH_RC4_40_MD5,SSL_RSA_EXPORT_WITH_DES40_CBC_SHA,SSL_RSA_WITH_RC4_128_MD5</value>
        <description>
            List of weak Cipher suites to exclude.
        </description>
    </property>

    <property>
        <name>oozie.jsp.tmp.dir</name>
        <value>/tmp</value>
        <description>
            Temporary directory for compiling JSP pages.
        </description>
    </property>

     <property>
         <name>oozie.instance.id</name>
         <value>${oozie.http.hostname}</value>
@@ -2519,6 +2600,14 @@ will be the requeue interval for the actions which are waiting for a long time w
         </description>
     </property>
 
    <property>
        <name>oozie.server.threadpool.max.threads</name>
        <value>150</value>
        <description>
             Controls the threadpool size for the Oozie Server (both Jetty and Tomcat)
        </description>
    </property>

     <!-- Sharelib Configuration -->
     <property>
         <name>oozie.service.ShareLibService.mapping.file</name>
diff --git a/distro/pom.xml b/distro/pom.xml
index def006922..ce5319f4d 100644
-- a/distro/pom.xml
++ b/distro/pom.xml
@@ -31,6 +31,10 @@
     <name>Apache Oozie Distro</name>
     <packaging>jar</packaging>
 
    <properties>
        <distro.descriptor>../src/main/assemblies/distro-jetty.xml</distro.descriptor>
    </properties>

     <dependencies>
         <dependency>
             <groupId>org.apache.oozie</groupId>
@@ -54,7 +58,6 @@
             <scope>compile</scope>
         </dependency>
     </dependencies>

     <build>
         <plugins>
             <plugin>
@@ -74,49 +77,11 @@
                 <configuration>
                     <finalName>oozie-${project.version}</finalName>
                     <descriptors>
                        <descriptor>../src/main/assemblies/distro.xml</descriptor>
                        <descriptor>${distro.descriptor}</descriptor>
                     </descriptors>
                 </configuration>
             </plugin>
            <!-- Downloading Tomcat TAR.GZ, using downloads/ dir to avoid downloading over an over -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-antrun-plugin</artifactId>
                <executions>
                    <execution>
                        <configuration>
                            <target>
                                <mkdir dir="downloads"/>
                                <get src="http://archive.apache.org/dist/tomcat/tomcat-6/v${tomcat.version}/bin/apache-tomcat-${tomcat.version}.tar.gz"
                                     dest="downloads/tomcat-${tomcat.version}.tar.gz" verbose="true" skipexisting="true"/>
                                <delete dir="target/tomcat"/>
                                <mkdir dir="target/tomcat"/>
                                <gunzip src="downloads/tomcat-${tomcat.version}.tar.gz"
                                        dest="target/tomcat/tomcat-${tomcat.version}.tar"/>
                                <untar src="target/tomcat/tomcat-${tomcat.version}.tar" dest="target/tomcat"/>
                                <move file="target/tomcat/apache-tomcat-${tomcat.version}" tofile="target/tomcat/oozie-server"/>
                                <delete dir="target/tomcat/oozie-server/webapps"/>
                                <mkdir dir="target/tomcat/oozie-server/webapps"/>
                                <delete file="target/tomcat/oozie-server/conf/server.xml"/>
                                <copy file="src/main/tomcat/server.xml" toDir="target/tomcat/oozie-server/conf"/>
                                <copy file="src/main/tomcat/logging.properties"
                                      toDir="target/tomcat/oozie-server/conf"/>
                                <mkdir dir="target/tomcat/oozie-server/conf/ssl"/>
                                <copy file="src/main/tomcat/server.xml" toDir="target/tomcat/oozie-server/conf/ssl"/>
                                <copy file="src/main/tomcat/ssl-server.xml" toDir="target/tomcat/oozie-server/conf/ssl"/>
                                <copy file="src/main/tomcat/ssl-web.xml" toDir="target/tomcat/oozie-server/conf/ssl"/>
                                <copy todir="target/tomcat/oozie-server/webapps/ROOT">
                                    <fileset dir="src/main/tomcat/ROOT"/>
                                </copy>
                            </target>
                        </configuration>
                        <goals>
                            <goal>run</goal>
                        </goals>
                        <phase>package</phase>
                    </execution>
                </executions>
            </plugin>

             <plugin>
                 <groupId>org.apache.maven.plugins</groupId>
                 <artifactId>maven-deploy-plugin</artifactId>
@@ -143,6 +108,56 @@
                 </dependency>
              </dependencies>
         </profile>

        <profile>
            <id>tomcat</id>
            <properties>
                <distro.descriptor>../src/main/assemblies/distro-tomcat.xml</distro.descriptor>
            </properties>
            <build>
                <plugins>
                    <!-- Downloading Tomcat TAR.GZ, using downloads/ dir to avoid downloading over an over -->
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-antrun-plugin</artifactId>
                        <version>1.6</version>
                        <executions>
                            <execution>
                                <configuration>
                                    <target>
                                        <mkdir dir="downloads"/>
                                        <get src="http://archive.apache.org/dist/tomcat/tomcat-6/v${tomcat.version}/bin/apache-tomcat-${tomcat.version}.tar.gz"
                                             dest="downloads/tomcat-${tomcat.version}.tar.gz" verbose="true" skipexisting="true"/>
                                        <delete dir="target/tomcat"/>
                                        <mkdir dir="target/tomcat"/>
                                        <gunzip src="downloads/tomcat-${tomcat.version}.tar.gz"
                                                dest="target/tomcat/tomcat-${tomcat.version}.tar"/>
                                        <untar src="target/tomcat/tomcat-${tomcat.version}.tar" dest="target/tomcat"/>
                                        <move file="target/tomcat/apache-tomcat-${tomcat.version}" tofile="target/tomcat/oozie-server"/>
                                        <delete dir="target/tomcat/oozie-server/webapps"/>
                                        <mkdir dir="target/tomcat/oozie-server/webapps"/>
                                        <delete file="target/tomcat/oozie-server/conf/server.xml"/>
                                        <copy file="src/main/tomcat/server.xml" toDir="target/tomcat/oozie-server/conf"/>
                                        <copy file="src/main/tomcat/logging.properties"
                                              toDir="target/tomcat/oozie-server/conf"/>
                                        <mkdir dir="target/tomcat/oozie-server/conf/ssl"/>
                                        <copy file="src/main/tomcat/server.xml" toDir="target/tomcat/oozie-server/conf/ssl"/>
                                        <copy file="src/main/tomcat/ssl-server.xml" toDir="target/tomcat/oozie-server/conf/ssl"/>
                                        <copy file="src/main/tomcat/ssl-web.xml" toDir="target/tomcat/oozie-server/conf/ssl"/>
                                        <copy todir="target/tomcat/oozie-server/webapps/ROOT">
                                            <fileset dir="src/main/tomcat/ROOT"/>
                                        </copy>
                                    </target>
                                </configuration>
                                <goals>
                                    <goal>run</goal>
                                </goals>
                                <phase>package</phase>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
     </profiles>
 </project>

diff --git a/distro/src/main/bin/oozie-jetty-server.sh b/distro/src/main/bin/oozie-jetty-server.sh
new file mode 100644
index 000000000..8acfc2ec2
-- /dev/null
++ b/distro/src/main/bin/oozie-jetty-server.sh
@@ -0,0 +1,226 @@
#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


# Set Jetty related environment variables
setup_jetty_log_and_pid() {
  if [ "${JETTY_OUT}" = "" ]; then
    export JETTY_OUT=${OOZIE_LOG}/jetty.out
    print "Setting JETTY_OUT:        ${JETTY_OUT}"
  else
    print "Using   JETTY_OUT:        ${JETTY_OUT}"
  fi

  if [ "${JETTY_PID_FILE}" = "" ]; then
    export JETTY_PID_FILE=${JETTY_DIR}/oozie.pid
    print "Setting JETTY_PID_FILE:        ${JETTY_PID_FILE}"
  else
    print "Using   JETTY_PID_FILE:        ${JETTY_PID_FILE}"
  fi
}

setup_java_opts() {
  if [ -z "${JAVA_HOME}" -a -z "${JRE_HOME}" ]; then
    if ${darwin}; then
      if [ -x '/usr/libexec/java_home' ] ; then
        export JAVA_HOME=`/usr/libexec/java_home`
      elif [ -d "/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home" ]; then
        export JAVA_HOME="/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home"
      fi
    else
      JAVA_PATH=`which java 2>/dev/null`
      if [ "x${JAVA_PATH}" != "x" ]; then
        JAVA_PATH=`dirname ${JAVA_PATH} 2>/dev/null`
      fi
      if [ "x${JRE_HOME}" = "x" ]; then
        if [ -x /usr/bin/java ]; then
          JRE_HOME=/usr
        fi
      fi
    fi
    if [ -z "${JAVA_HOME}" -a -z "${JRE_HOME}" ]; then
      echo "Neither the JAVA_HOME nor the JRE_HOME environment variable is defined"
      echo "At least one of these environment variable is needed to run this program"
      exit 1
    fi
  fi
  if [ -z "${JRE_HOME}" ]; then
    JRE_HOME="${JAVA_HOME}"
  fi

  JAVA_BIN="${JRE_HOME}"/bin/java
  echo "Using Java executable from ${JRE_HOME}"
}

setup_jetty_opts() {
  echo "Using   JETTY_OPTS:       ${JETTY_OPTS}"
  jetty_opts="-Doozie.home.dir=${OOZIE_HOME}";
  jetty_opts="${jetty_opts} -Doozie.config.dir=${OOZIE_CONFIG}";
  jetty_opts="${jetty_opts} -Doozie.log.dir=${OOZIE_LOG}";
  jetty_opts="${jetty_opts} -Doozie.data.dir=${OOZIE_DATA}";
  jetty_opts="${jetty_opts} -Doozie.config.file=${OOZIE_CONFIG_FILE}";
  jetty_opts="${jetty_opts} -Doozie.log4j.file=${OOZIE_LOG4J_FILE}";
  jetty_opts="${jetty_opts} -Doozie.log4j.reload=${OOZIE_LOG4J_RELOAD}";
  # add required native libraries such as compression codecs
  jetty_opts="${jetty_opts} -Djava.library.path=${JAVA_LIBRARY_PATH}";

  jetty_opts="${jetty_opts} -cp ${JETTY_DIR}/*:${JETTY_DIR}/dependency/*:${BASEDIR}/lib/*:${BASEDIR}/libtools/*:${JETTY_DIR}"
  echo "Adding to JETTY_OPTS:     ${jetty_opts}"

  export JETTY_OPTS="${JETTY_OPTS} ${jetty_opts}"
}

start_jetty() {
  if [ ! -z "${JETTY_PID_FILE}" ]; then
    if [ -f "${JETTY_PID_FILE}" ]; then
      if [ -s "${JETTY_PID_FILE}" ]; then
        echo "Existing PID file found during start."
        if [ -r "${JETTY_PID_FILE}" ]; then
          PID=$(cat "${JETTY_PID_FILE}")
          ps -p "$PID" >/dev/null 2>&1
          if [ $? -eq 0 ] ; then
            echo "Oozie server appears to still be running with PID $PID. Start aborted."
            echo "If the following process is not a Jetty process, remove the PID file and try again:"
            ps -f -p "$PID"
            exit 1
          else
            echo "Removing/clearing stale PID file."
            rm -f "${JETTY_PID_FILE}" >/dev/null 2>&1
            if [ $? != 0 ]; then
              if [ -w "${JETTY_PID_FILE}" ]; then
                cat /dev/null > "${JETTY_PID_FILE}"
              else
                echo "Unable to remove or clear stale PID file. Start aborted."
                exit 1
              fi
            fi
          fi
        else
          echo "Unable to read PID file. Start aborted."
          exit 1
        fi
      else
        rm -f "$JETTY_PID_FILE" >/dev/null 2>&1
        if [ $? != 0 ]; then
          if [ ! -w "$JETTY_PID_FILE" ]; then
            echo "Unable to remove or write to empty PID file. Start aborted."
            exit 1
          fi
        fi
      fi
    fi
  fi

  ${JAVA_BIN} ${JETTY_OPTS} org.apache.oozie.server.EmbeddedOozieServer >> "${JETTY_OUT}" 2>&1 &
  PID=$!
  if [ ${PID} ]; then
    echo -n "Oozie server started"
  fi

  if [ ! -z "${JETTY_PID_FILE}" ]; then
    echo -n $! > "${JETTY_PID_FILE}"
    echo -n " - PID: ${PID}."
  fi
  echo
}

run_jetty() {
  ${JAVA_BIN} ${JETTY_OPTS} org.apache.oozie.server.EmbeddedOozieServer
}

#TODO allow users to force kill jetty. Add --force
stop_jetty() {
  if [ ! -z "${JETTY_PID_FILE}" ]; then
    if [ -f "${JETTY_PID_FILE}" ]; then
      if [ -s "${JETTY_PID_FILE}" ]; then
        kill -0 "$(cat "${JETTY_PID_FILE}")" >/dev/null 2>&1
        if [ $? -gt 0 ]; then
          echo "PID file found but no matching process was found. Stop aborted."
          exit 1
        fi
      else
        echo "PID file is empty and has been ignored."
      fi
    else
      echo "\$JETTY_PID_FILE was set but the specified file does not exist. Is Oozie server running? Stop aborted."
      exit 1
    fi
  fi

  kill "$(cat "${JETTY_PID_FILE}")"

  RETRY_COUNT=5

  if [ ! -z "${JETTY_PID_FILE}" ]; then
    if [ -f "${JETTY_PID_FILE}" ]; then
      while [ $RETRY_COUNT -ge 0 ]; do
        kill -0 "$(cat ${JETTY_PID_FILE})" >/dev/null 2>&1
        if [ $? -gt 0 ]; then
          rm -f "${JETTY_PID_FILE}" >/dev/null 2>&1
          if [ $? != 0 ]; then
            if [ -w "${JETTY_PID_FILE}" ]; then
              cat /dev/null > "${JETTY_PID_FILE}"
            else
              echo "Oozie server stopped but the PID file could not be removed or cleared."
            fi
          fi
          break
        fi
        if [ ${RETRY_COUNT} -gt 0 ]; then
          sleep 1
        fi
        if [ ${RETRY_COUNT} -eq 0 ]; then
          echo "Oozie server did not stop in time. PID file was not removed."
        fi
        RETRY_COUNT=$((RETRY_COUNT - 1))
      done
    fi
  fi
}

symlink_lib() {
  test -e ${BASEDIR}/lib || ln -s ${JETTY_DIR}/webapp/WEB-INF/lib ${BASEDIR}/lib
}

jetty_main() {
  source ${BASEDIR}/bin/oozie-sys.sh
  JETTY_DIR=${BASEDIR}/embedded-oozie-server

  setup_jetty_log_and_pid
  setup_java_opts
  setup_jetty_opts

  actionCmd=$1
  case $actionCmd in
    (run)
       ${BASEDIR}/bin/oozie-setup.sh
       symlink_lib
       setup_ooziedb
       run_jetty
      ;;
    (start)
       ${BASEDIR}/bin/oozie-setup.sh
       symlink_lib
       setup_ooziedb
       start_jetty
      ;;
    (stop)
      stop_jetty
      ;;
  esac
}
diff --git a/distro/src/main/bin/oozie-setup.sh b/distro/src/main/bin/oozie-setup.sh
index 79b049bcc..9d6a2d0c9 100644
-- a/distro/src/main/bin/oozie-setup.sh
++ b/distro/src/main/bin/oozie-setup.sh
@@ -74,9 +74,7 @@ function checkExec() {
   then
     echo
     echo "Failed: $1"
    echo
    cleanUp
    exit -1;
    cleanup_and_exit
   fi
 }
 
@@ -85,9 +83,7 @@ function checkFileExists() {
   if [ ! -e ${1} ]; then
     echo
     echo "File/Dir does no exist: ${1}"
    echo
    cleanUp
    exit -1
    cleanup_and_exit
   fi
 }
 
@@ -96,9 +92,7 @@ function checkFileDoesNotExist() {
   if [ -e ${1} ]; then
     echo
     echo "File/Dir already exists: ${1}"
    echo
    cleanUp
    exit -1
    cleanup_and_exit
   fi
 }
 
@@ -119,6 +113,10 @@ done
 BASEDIR=`dirname ${PRG}`
 BASEDIR=`cd ${BASEDIR}/..;pwd`
 
JETTY_DIR=${BASEDIR}/embedded-oozie-server
JETTY_WEBAPP_DIR=${JETTY_DIR}/webapp
JETTY_LIB_DIR=${JETTY_WEBAPP_DIR}/WEB-INF/lib/

 source ${BASEDIR}/bin/oozie-sys.sh -silent
 
 addExtjs=""
@@ -145,10 +143,16 @@ do
     #Create lib directory from war if lib doesn't exist
     if [ ! -d "${BASEDIR}/lib" ]; then
       mkdir ${BASEDIR}/lib
      unzip ${BASEDIR}/oozie.war WEB-INF/lib/*.jar -d ${BASEDIR}/lib > /dev/null
      mv ${BASEDIR}/lib/WEB-INF/lib/*.jar ${BASEDIR}/lib/
      rmdir ${BASEDIR}/lib/WEB-INF/lib
      rmdir ${BASEDIR}/lib/WEB-INF

      if [ "${OOZIE_USE_TOMCAT}" = "1" ]; then
        unzip ${BASEDIR}/oozie.war WEB-INF/lib/*.jar -d ${BASEDIR}/lib > /dev/null
        mv ${BASEDIR}/lib/WEB-INF/lib/*.jar ${BASEDIR}/lib/
        rmdir ${BASEDIR}/lib/WEB-INF/lib
        rmdir ${BASEDIR}/lib/WEB-INF
      else
        cp ${JETTY_LIB_DIR}/*  ${BASEDIR}/lib
      fi

     fi
 
     OOZIECPPATH=""
@@ -187,7 +191,7 @@ do
   shift
 done
 
if [ -e "${CATALINA_PID}" ]; then
if [ -e "${CATALINA_PID}" -a "${OOZIE_USE_TOMCAT}" = "1" ]; then
   echo
   echo "ERROR: Stop Oozie first"
   echo
@@ -196,19 +200,25 @@ fi
 
 echo
 
if [ "${prepareWar}" == "" ]; then
  echo "no arguments given"
  printUsage
  exit -1
else
  if [ -e "${outputWar}" ]; then
      chmod -f u+w ${outputWar}
      rm -rf ${outputWar}
  fi
  rm -rf ${outputWarExpanded}
 
  # Adding extension JARs
log_ready_to_start() {
  echo

  echo "INFO: Oozie is ready to be started"

  echo
}

check_extjs() {
  if [ "${addExtjs}" = "true" ]; then
    checkFileExists ${extjsHome}
  else
    echo "INFO: Oozie webconsole disabled, ExtJS library not specified"
  fi
}
 
# Check if it is necessary to add extension JARs and ExtJS
check_adding_extensions() {
   libext=${OOZIE_HOME}/libext
   if [ "${additionalDir}" != "" ]; then
     libext=${additionalDir}
@@ -227,114 +237,167 @@ else
       addExtjs=true
     fi
   fi
}
 
  prepare

  checkFileExists ${inputWar}
  checkFileDoesNotExist ${outputWar}
cleanup_and_exit() {
  echo
  cleanUp
  exit -1
}
 
  if [ "${addExtjs}" = "true" ]; then
    checkFileExists ${extjsHome}
  else
    echo "INFO: Oozie webconsole disabled, ExtJS library not specified"
prepare_jetty() {
  check_adding_extensions
  check_extjs

  if [ "${addExtjs}" = "true" -a ! -e ${JETTY_WEBAPP_DIR}/ext-2.2 ]; then
     unzip ${extjsHome} -d ${JETTY_WEBAPP_DIR}
    checkExec "Extracting ExtJS to ${JETTY_WEBAPP_DIR}/"
  elif [ "${addExtjs}" = "true" -a -e ${JETTY_WEBAPP_DIR}/ext-2.2 ]; then
     # TODO
    echo "${JETTY_WEBAPP_DIR}/ext-2.2 already exists"
    cleanup_and_exit
   fi
 
   if [ "${addJars}" = "true" ]; then
      for jarPath in ${jarsPath//:/$'\n'}
      do
        checkFileExists ${jarPath}
      done
    for jarPath in ${jarsPath//:/$'\n'}
    do
      found=`ls ${JETTY_LIB_DIR}/${jarPath} 2> /dev/null | wc -l`
      checkExec "looking for JAR ${jarPath} in ${JETTY_LIB_DIR}"
      if [ ! $found = 0 ]; then
        echo
        echo "${JETTY_LIB_DIR} already contains JAR ${jarPath}"
        cleanup_and_exit
      fi
      cp ${jarPath} ${JETTY_LIB_DIR}
      checkExec "copying jar ${jarPath} to '${JETTY_LIB_DIR}'"
    done
   fi
}
 
  #Unpacking original war
  unzip ${inputWar} -d ${tmpWarDir} > /dev/null
  checkExec "unzipping Oozie input WAR"
prepare_tomcat() {
  if [ "${prepareWar}" == "" ]; then
    echo "no arguments given"
    printUsage
    exit -1
  else
    if [ -e "${outputWar}" ]; then
        chmod -f u+w ${outputWar}
        rm -rf ${outputWar}
    fi
    rm -rf ${outputWarExpanded}
 
  components=""
    check_adding_extensions
 
  if [ "${secure}" != "" ]; then
    #Use the SSL version of server.xml in oozie-server
    checkFileExists ${secureConfigsDir}/ssl-server.xml
    cp ${secureConfigsDir}/ssl-server.xml ${CATALINA_BASE}/conf/server.xml
    #Inject the SSL version of web.xml in oozie war
    checkFileExists ${secureConfigsDir}/ssl-web.xml
    cp ${secureConfigsDir}/ssl-web.xml ${tmpWarDir}/WEB-INF/web.xml
    echo "INFO: Using secure server.xml and secure web.xml"
  else
    #Use the regular version of server.xml in oozie-server
    checkFileExists ${secureConfigsDir}/server.xml
    cp ${secureConfigsDir}/server.xml ${CATALINA_BASE}/conf/server.xml
    #No need to restore web.xml because its already in the original WAR file
  fi
    prepare
 
  if [ "${addExtjs}" = "true" ]; then
    if [ ! "${components}" = "" ];then
      components="${components}, "
    fi
    components="${components}ExtJS library"
    if [ -e ${tmpWarDir}/ext-2.2 ]; then
      echo
      echo "Specified Oozie WAR '${inputWar}' already contains ExtJS library files"
      echo
      cleanUp
      exit -1
    fi
    #If the extjs path given is a ZIP, expand it and use it from there
    if [ -f ${extjsHome} ]; then
      unzip ${extjsHome} -d ${tmpDir} > /dev/null
      extjsHome=${tmpDir}/ext-2.2
    checkFileExists ${inputWar}
    checkFileDoesNotExist ${outputWar}

    check_extjs

    if [ "${addJars}" = "true" ]; then
        for jarPath in ${jarsPath//:/$'\n'}
        do
          checkFileExists ${jarPath}
        done
     fi
    #Inject the library in oozie war
    cp -r ${extjsHome} ${tmpWarDir}/ext-2.2
    checkExec "copying ExtJS files into staging"
  fi
 
  if [ "${addJars}" = "true" ]; then
    if [ ! "${components}" = "" ];then
      components="${components}, "
    #Unpacking original war
    unzip ${inputWar} -d ${tmpWarDir} > /dev/null
    checkExec "unzipping Oozie input WAR"

    components=""

    if [ "${OOZIE_USE_TOMCAT}" == "1" ]; then
      if [ "${secure}" != "" ]; then
        #Use the SSL version of server.xml in oozie-server
        checkFileExists ${secureConfigsDir}/ssl-server.xml
        cp ${secureConfigsDir}/ssl-server.xml ${CATALINA_BASE}/conf/server.xml
        #Inject the SSL version of web.xml in oozie war
        checkFileExists ${secureConfigsDir}/ssl-web.xml
        cp ${secureConfigsDir}/ssl-web.xml ${tmpWarDir}/WEB-INF/web.xml
        echo "INFO: Using secure server.xml and secure web.xml"
      else
        #Use the regular version of server.xml in oozie-server
        checkFileExists ${secureConfigsDir}/server.xml
        cp ${secureConfigsDir}/server.xml ${CATALINA_BASE}/conf/server.xml
        #No need to restore web.xml because its already in the original WAR file
      fi
     fi
    components="${components}JARs"
 
    for jarPath in ${jarsPath//:/$'\n'}
    do
      found=`ls ${tmpWarDir}/WEB-INF/lib/${jarPath} 2> /dev/null | wc -l`
      checkExec "looking for JAR ${jarPath} in input WAR"
      if [ ! $found = 0 ]; then
        echo
        echo "Specified Oozie WAR '${inputWar}' already contains JAR ${jarPath}"
    if [ "${addExtjs}" = "true" ]; then
      if [ ! "${components}" = "" ];then
        components="${components}, "
      fi
      components="${components}ExtJS library"
      if [ -e ${tmpWarDir}/ext-2.2 ]; then
         echo
        cleanUp
        exit -1
        echo "Specified Oozie WAR '${inputWar}' already contains ExtJS library files"
        cleanup_and_exit
       fi
      cp ${jarPath} ${tmpWarDir}/WEB-INF/lib/
      checkExec "copying jar ${jarPath} to staging"
    done
  fi
      #If the extjs path given is a ZIP, expand it and use it from there
      if [ -f ${extjsHome} ]; then
        unzip ${extjsHome} -d ${tmpDir} > /dev/null
        extjsHome=${tmpDir}/ext-2.2
      fi
      #Inject the library in oozie war
      cp -r ${extjsHome} ${tmpWarDir}/ext-2.2
      checkExec "copying ExtJS files into staging"
    fi
 
  #Creating new Oozie WAR
  currentDir=`pwd`
  cd ${tmpWarDir}
  zip -r oozie.war * > /dev/null
  checkExec "creating new Oozie WAR"
  cd ${currentDir}
    if [ "${addJars}" = "true" ]; then
      if [ ! "${components}" = "" ];then
        components="${components}, "
      fi
      components="${components}JARs"
 
  #copying new Oozie WAR to asked location
  cp ${tmpWarDir}/oozie.war ${outputWar}
  checkExec "copying new Oozie WAR"
      for jarPath in ${jarsPath//:/$'\n'}
      do
        found=`ls ${tmpWarDir}/WEB-INF/lib/${jarPath} 2> /dev/null | wc -l`
        checkExec "looking for JAR ${jarPath} in input WAR"
        if [ ! $found = 0 ]; then
          echo
          echo "Specified Oozie WAR '${inputWar}' already contains JAR ${jarPath}"
          cleanup_and_exit
        fi
        cp ${jarPath} ${tmpWarDir}/WEB-INF/lib/
        checkExec "copying jar ${jarPath} to staging"
      done
    fi
 
  echo
  echo "New Oozie WAR file with added '${components}' at ${outputWar}"
  echo
  cleanUp
    #Creating new Oozie WAR
    currentDir=`pwd`
    cd ${tmpWarDir}
    zip -r oozie.war * > /dev/null
    checkExec "creating new Oozie WAR"
    cd ${currentDir}
 
  if [ "$?" != "0" ]; then
    exit -1
  fi
    #copying new Oozie WAR to asked location
    if [ "${OOZIE_USE_TOMCAT}" == "1" ]; then
      cp ${tmpWarDir}/oozie.war ${outputWar}
      checkExec "copying new Oozie WAR"
 
  echo
      echo
      echo "New Oozie WAR file with added '${components}' at ${outputWar}"
      echo
    fi
 
  echo "INFO: Oozie is ready to be started"
    cleanUp
 
  echo
    if [ "$?" -ne "0" ]; then
      exit -1
    fi
 
    log_ready_to_start

  fi
}

if [ "${OOZIE_USE_TOMCAT}" = "1" ]; then
  prepare_tomcat
else
  prepare_jetty
 fi

log_ready_to_start
exit 0
diff --git a/distro/src/main/bin/oozie-sys.sh b/distro/src/main/bin/oozie-sys.sh
index 97d55a2b6..688aeb286 100755
-- a/distro/src/main/bin/oozie-sys.sh
++ b/distro/src/main/bin/oozie-sys.sh
@@ -195,7 +195,7 @@ else
   print "Using   OOZIE_BASE_URL:      ${OOZIE_BASE_URL}"
 fi
 
if [ "${CATALINA_BASE}" = "" ]; then
if [ "${OOZIE_USE_TOMCAT}" = "1" -a "${CATALINA_BASE}" = "" ]; then
   export CATALINA_BASE=${OOZIE_HOME}/oozie-server
   print "Setting CATALINA_BASE:       ${CATALINA_BASE}"
 else
@@ -223,20 +223,40 @@ else
   print "Using   OOZIE_INSTANCE_ID:       ${OOZIE_INSTANCE_ID}"
 fi
 
if [ "${CATALINA_OUT}" = "" ]; then
  export CATALINA_OUT=${OOZIE_LOG}/catalina.out
  print "Setting CATALINA_OUT:        ${CATALINA_OUT}"
else
  print "Using   CATALINA_OUT:        ${CATALINA_OUT}"
if [ "${OOZIE_USE_TOMCAT}" = "1" ]; then
  if [  "${CATALINA_OUT}" = "" ]; then
    export CATALINA_OUT=${OOZIE_LOG}/catalina.out
    print "Setting CATALINA_OUT:        ${CATALINA_OUT}"
  else
    print "Using   CATALINA_OUT:        ${CATALINA_OUT}"
  fi
 fi
 
if [ "${CATALINA_PID}" = "" ]; then
if [ "${OOZIE_USE_TOMCAT}" = "1" -a "${CATALINA_PID}" = "" ]; then
   export CATALINA_PID=${OOZIE_HOME}/oozie-server/temp/oozie.pid
   print "Setting CATALINA_PID:        ${CATALINA_PID}"
 else
   print "Using   CATALINA_PID:        ${CATALINA_PID}"
 fi
 
export CATALINA_OPTS="${CATALINA_OPTS} -Dderby.stream.error.file=${OOZIE_LOG}/derby.log"
if [ "${OOZIE_USE_TOMCAT}" = "1" ]; then
  export CATALINA_OPTS="${CATALINA_OPTS} -Dderby.stream.error.file=${OOZIE_LOG}/derby.log"
fi
 
 print

setup_ooziedb() {
  echo "Setting up oozie DB"
  ${BASEDIR}/bin/ooziedb.sh create -run
  if [ "$?" -ne "0" ]; then
    exit -1
  fi
  echo
}

if [ "${JAVA_HOME}" != "" ]; then
    JAVA_BIN=java
else
    JAVA_BIN=${JAVA_HOME}/bin/java
fi

diff --git a/distro/src/main/bin/oozie-tomcat-server.sh b/distro/src/main/bin/oozie-tomcat-server.sh
new file mode 100644
index 000000000..18dd0f600
-- /dev/null
++ b/distro/src/main/bin/oozie-tomcat-server.sh
@@ -0,0 +1,89 @@
#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

CATALINA=${OOZIE_CATALINA_HOME:-${BASEDIR}/oozie-server}/bin/catalina.sh

setup_catalina_opts() {
  # The Java System properties 'oozie.http.port' and 'oozie.https.port' are not
  # used by Oozie, they are used in Tomcat's server.xml configuration file
  #
  echo "Using   CATALINA_OPTS:       ${CATALINA_OPTS}"

  catalina_opts="-Doozie.home.dir=${OOZIE_HOME}";
  catalina_opts="${catalina_opts} -Doozie.config.dir=${OOZIE_CONFIG}";
  catalina_opts="${catalina_opts} -Doozie.log.dir=${OOZIE_LOG}";
  catalina_opts="${catalina_opts} -Doozie.data.dir=${OOZIE_DATA}";
  catalina_opts="${catalina_opts} -Doozie.instance.id=${OOZIE_INSTANCE_ID}"

  catalina_opts="${catalina_opts} -Doozie.config.file=${OOZIE_CONFIG_FILE}";

  catalina_opts="${catalina_opts} -Doozie.log4j.file=${OOZIE_LOG4J_FILE}";
  catalina_opts="${catalina_opts} -Doozie.log4j.reload=${OOZIE_LOG4J_RELOAD}";

  catalina_opts="${catalina_opts} -Doozie.http.hostname=${OOZIE_HTTP_HOSTNAME}";
  catalina_opts="${catalina_opts} -Doozie.admin.port=${OOZIE_ADMIN_PORT}";
  catalina_opts="${catalina_opts} -Doozie.http.port=${OOZIE_HTTP_PORT}";
  catalina_opts="${catalina_opts} -Doozie.https.port=${OOZIE_HTTPS_PORT}";
  catalina_opts="${catalina_opts} -Doozie.base.url=${OOZIE_BASE_URL}";
  catalina_opts="${catalina_opts} -Doozie.https.keystore.file=${OOZIE_HTTPS_KEYSTORE_FILE}";
  catalina_opts="${catalina_opts} -Doozie.https.keystore.pass=${OOZIE_HTTPS_KEYSTORE_PASS}";

  # add required native libraries such as compression codecs
  catalina_opts="${catalina_opts} -Djava.library.path=${JAVA_LIBRARY_PATH}";

  echo "Adding to CATALINA_OPTS:     ${catalina_opts}"

  export CATALINA_OPTS="${CATALINA_OPTS} ${catalina_opts}"
}

setup_oozie() {
  if [ ! -e "${CATALINA_BASE}/webapps/oozie.war" ]; then
    echo "WARN: Oozie WAR has not been set up at '${CATALINA_BASE}/webapps', doing default set up"
    ${BASEDIR}/bin/oozie-setup.sh prepare-war
    if [ "$?" -ne "0" ]; then
      exit -1
    fi
  fi
  echo
}

tomcat_main() {
  source ${BASEDIR}/bin/oozie-sys.sh

  #Create webapp directory from war if lib doesn't exist
  if [ ! -d "${BASEDIR}/embedded-oozie-server/webapp" ]; then
    unzip "${BASEDIR}/oozie.war" -d "${BASEDIR}/embedded-oozie-server/webapp" > /dev/null
  fi

  actionCmd=$1
  case $actionCmd in
    (start|run)
      setup_catalina_opts
      setup_oozie
      setup_ooziedb
      #TODO setup default oozie sharelib
      ;;
    (stop)
      setup_catalina_opts

      # A bug in catalina.sh script does not use CATALINA_OPTS for stopping the server
      export JAVA_OPTS=${CATALINA_OPTS}
      ;;
  esac
  exec $CATALINA $actionCmd "$@"
}
diff --git a/distro/src/main/bin/oozied.sh b/distro/src/main/bin/oozied.sh
index a869c3da1..462ba76e9 100644
-- a/distro/src/main/bin/oozied.sh
++ b/distro/src/main/bin/oozied.sh
@@ -17,14 +17,6 @@
 # limitations under the License.
 #
 
if [ $# -le 0 ]; then
  echo "Usage: oozied.sh (start|stop|run) [<catalina-args...>]"
  exit 1
fi

actionCmd=$1
shift

 # resolve links - $0 may be a softlink
 PRG="${0}"
 
@@ -41,76 +33,28 @@ done
 BASEDIR=`dirname ${PRG}`
 BASEDIR=`cd ${BASEDIR}/..;pwd`
 
source ${BASEDIR}/bin/oozie-sys.sh

CATALINA=${OOZIE_CATALINA_HOME:-${BASEDIR}/oozie-server}/bin/catalina.sh

setup_catalina_opts() {
  # The Java System properties 'oozie.http.port' and 'oozie.https.port' are not
  # used by Oozie, they are used in Tomcat's server.xml configuration file
  #
  echo "Using   CATALINA_OPTS:       ${CATALINA_OPTS}"

  catalina_opts="-Doozie.home.dir=${OOZIE_HOME}";
  catalina_opts="${catalina_opts} -Doozie.config.dir=${OOZIE_CONFIG}";
  catalina_opts="${catalina_opts} -Doozie.log.dir=${OOZIE_LOG}";
  catalina_opts="${catalina_opts} -Doozie.data.dir=${OOZIE_DATA}";
  catalina_opts="${catalina_opts} -Doozie.instance.id=${OOZIE_INSTANCE_ID}"

  catalina_opts="${catalina_opts} -Doozie.config.file=${OOZIE_CONFIG_FILE}";

  catalina_opts="${catalina_opts} -Doozie.log4j.file=${OOZIE_LOG4J_FILE}";
  catalina_opts="${catalina_opts} -Doozie.log4j.reload=${OOZIE_LOG4J_RELOAD}";

  catalina_opts="${catalina_opts} -Doozie.http.hostname=${OOZIE_HTTP_HOSTNAME}";
  catalina_opts="${catalina_opts} -Doozie.admin.port=${OOZIE_ADMIN_PORT}";
  catalina_opts="${catalina_opts} -Doozie.http.port=${OOZIE_HTTP_PORT}";
  catalina_opts="${catalina_opts} -Doozie.https.port=${OOZIE_HTTPS_PORT}";
  catalina_opts="${catalina_opts} -Doozie.base.url=${OOZIE_BASE_URL}";
  catalina_opts="${catalina_opts} -Doozie.https.keystore.file=${OOZIE_HTTPS_KEYSTORE_FILE}";
  catalina_opts="${catalina_opts} -Doozie.https.keystore.pass=${OOZIE_HTTPS_KEYSTORE_PASS}";

  # add required native libraries such as compression codecs
  catalina_opts="${catalina_opts} -Djava.library.path=${JAVA_LIBRARY_PATH}";

  echo "Adding to CATALINA_OPTS:     ${catalina_opts}"

  export CATALINA_OPTS="${CATALINA_OPTS} ${catalina_opts}"
}

setup_oozie() {
  if [ ! -e "${CATALINA_BASE}/webapps/oozie.war" ]; then
    echo "WARN: Oozie WAR has not been set up at '${CATALINA_BASE}/webapps', doing default set up"
    ${BASEDIR}/bin/oozie-setup.sh prepare-war
    if [ "$?" != "0" ]; then
      exit -1
    fi
  fi
  echo
}
if [ -e "${BASEDIR}/oozie-server" ]; then
  export OOZIE_USE_TOMCAT=1
else
  export OOZIE_USE_TOMCAT=0
fi
 
setup_ooziedb() {
  echo "Setting up oozie DB"
  ${BASEDIR}/bin/ooziedb.sh create -run
  if [ "$?" != "0" ]; then
    exit -1
if [ $# -le 0 ]; then
  if [ "${OOZIE_USE_TOMCAT}" -eq "1" ]; then
    echo "Usage: oozied.sh (start|stop|run) [<catalina-args...>]"
  else
    echo "Usage: oozied.sh (start|stop|run)"
   fi
  echo
}

case $actionCmd in
  (start|run)
    setup_catalina_opts
    setup_oozie
    setup_ooziedb
    #TODO setup default oozie sharelib
    ;;
  (stop)
    setup_catalina_opts
  exit 1
fi
 
    # A bug in catalina.sh script does not use CATALINA_OPTS for stopping the server
    export JAVA_OPTS=${CATALINA_OPTS}
    ;;
esac
actionCmd=$1
shift
 
exec $CATALINA $actionCmd "$@"
if [ "${OOZIE_USE_TOMCAT}" == "1" ]; then
  source ${BASEDIR}/bin/oozie-tomcat-server.sh
  tomcat_main $actionCmd
else
  source ${BASEDIR}/bin/oozie-jetty-server.sh
  jetty_main $actionCmd
fi
diff --git a/pom.xml b/pom.xml
index a3db3da3e..c9a19def7 100644
-- a/pom.xml
++ b/pom.xml
@@ -56,7 +56,7 @@
         <failIfNoTests>false</failIfNoTests>
 
         <test.timeout>5400</test.timeout>

        <clover.version>4.0.6</clover.version>
         <!-- platform encoding override -->
         <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
         <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
@@ -105,7 +105,9 @@
          <distcp.version>${hadoop.version}</distcp.version>
          <hadoop.auth.version>${hadoop.version}</hadoop.auth.version>
 
         <!-- Tomcat version -->
         <jetty.version>9.2.19.v20160908</jetty.version>

        <!-- Tomcat version -->
          <tomcat.version>6.0.44</tomcat.version>
          <jline.version>0.9.94</jline.version>
          <openjpa.version>2.4.1</openjpa.version>
@@ -130,6 +132,7 @@
         <module>docs</module>
         <module>tools</module>
         <module>minitest</module>
        <module>server</module>
         <module>distro</module>
         <module>zookeeper-security-tests</module>
     </modules>
@@ -809,6 +812,12 @@
                 <version>${streaming.version}</version>
             </dependency>
 
            <dependency>
                <groupId>org.apache.hadoop</groupId>
                <artifactId>hadoop-common</artifactId>
                <version>${hadoop.version}</version>
            </dependency>

             <dependency>
                 <groupId>org.apache.pig</groupId>
                 <artifactId>pig</artifactId>
@@ -1545,6 +1554,58 @@
                 <artifactId>gson</artifactId>
                 <version>2.7</version>
             </dependency>
            <dependency>
                <groupId>com.google.inject</groupId>
                <artifactId>guice</artifactId>
                <version>3.0</version>
            </dependency>

            <dependency>
                <groupId>org.eclipse.jetty</groupId>
                <artifactId>jetty-server</artifactId>
                <version>${jetty.version}</version>
            </dependency>
            <dependency>
                <groupId>org.eclipse.jetty</groupId>
                <artifactId>jetty-servlet</artifactId>
                <version>${jetty.version}</version>
            </dependency>
            <dependency>
                <groupId>org.eclipse.jetty</groupId>
                <artifactId>jetty-util</artifactId>
                <version>${jetty.version}</version>
            </dependency>
            <dependency>
                <groupId>org.eclipse.jetty</groupId>
                <artifactId>jetty-http</artifactId>
                <version>${jetty.version}</version>
            </dependency>
            <dependency>
                <groupId>org.eclipse.jetty</groupId>
                <artifactId>jetty-rewrite</artifactId>
                <version>${jetty.version}</version>
            </dependency>
            <dependency>
                <groupId>org.eclipse.jetty</groupId>
                <artifactId>jetty-annotations</artifactId>
                <version>${jetty.version}</version>
            </dependency>
            <dependency>
                <groupId>org.eclipse.jetty</groupId>
                <artifactId>jetty-webapp</artifactId>
                <version>${jetty.version}</version>
            </dependency>
            <dependency>
                <groupId>org.eclipse.jetty</groupId>
                <artifactId>apache-jsp</artifactId>
                <type>jar</type>
                <version>${jetty.version}</version>
            </dependency>
            <dependency>
                <groupId>org.eclipse.jetty</groupId>
                <artifactId>jetty-plus</artifactId>
                <version>${jetty.version}</version>
            </dependency>
         </dependencies>
     </dependencyManagement>
 
@@ -1659,6 +1720,7 @@
             <plugin>
                 <groupId>com.atlassian.maven.plugins</groupId>
                 <artifactId>maven-clover2-plugin</artifactId>
                <version>${clover.version}</version>
                 <configuration>
                     <licenseLocation>${clover.license}</licenseLocation>
                     <generateXml>true</generateXml>
@@ -1959,5 +2021,11 @@
                 <spark.bagel.version>1.6.2</spark.bagel.version>
             </properties>
         </profile>
        <profile>
            <id>tomcat</id>
            <activation>
                <activeByDefault>false</activeByDefault>
            </activation>
        </profile>
     </profiles>
 </project>
diff --git a/release-log.txt b/release-log.txt
index 70ffaa6a9..3071c7ba3 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.4.0 release (trunk - unreleased)
 
OOZIE-2666 Support embedding Jetty into Oozie (asasvari via rkanter)
 OOZIE-1459 Remove the version in the child poms for maven-antrun-plugin (Jan Hentschel via rkanter)
 OOZIE-2225 Add wild card filter for gathering jobs (sai-krish via rkanter)
 OOZIE-2536 Hadoop's cleanup of local directory in uber mode causing failures (satishsaley via rohini)
diff --git a/server/pom.xml b/server/pom.xml
new file mode 100644
index 000000000..a336aa80d
-- /dev/null
++ b/server/pom.xml
@@ -0,0 +1,257 @@
<?xml version="1.0" encoding="UTF-8"?>
<!--
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
-->
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.apache.oozie</groupId>
        <artifactId>oozie-main</artifactId>
        <version>4.4.0-SNAPSHOT</version>
    </parent>
    <groupId>org.apache.oozie</groupId>
    <artifactId>oozie-server</artifactId>
    <version>4.4.0-SNAPSHOT</version>
    <description>Apache Oozie Server</description>
    <name>Apache Oozie Server</name>
    <packaging>jar</packaging>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-servlet</artifactId>
        </dependency>
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-util</artifactId>
        </dependency>
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-http</artifactId>
        </dependency>
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-rewrite</artifactId>
        </dependency>

        <!--  begin JSP support -->
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-annotations</artifactId>
        </dependency>
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-webapp</artifactId>
        </dependency>
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>apache-jsp</artifactId>
            <type>jar</type>
        </dependency>
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-plus</artifactId>
        </dependency>
        <!--  end JSP support -->
        <dependency>
            <groupId>org.apache.hadoop</groupId>
            <artifactId>hadoop-common</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>org.mortbay.jetty</groupId>
                    <artifactId>jetty</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.mortbay.jetty</groupId>
                    <artifactId>jetty-util</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>org.apache.oozie</groupId>
            <artifactId>oozie-client</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>org.slf4j</groupId>
                    <artifactId>slf4j-simple</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>org.apache.oozie</groupId>
            <artifactId>oozie-core</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>org.apache.hadoop</groupId>
                    <artifactId>hadoop-core</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.apache.oozie</groupId>
                    <artifactId>hadoop-auth</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.apache.oozie</groupId>
                    <artifactId>oozie-client</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.mortbay.jetty</groupId>
                    <artifactId>jetty</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.eclipse.jetty.aggregate</groupId>
                    <artifactId>jetty-all</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>com.google.inject</groupId>
            <artifactId>guice</artifactId>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-all</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.openjpa</groupId>
                <artifactId>openjpa-maven-plugin</artifactId>
            </plugin>

            <plugin>
                <artifactId>maven-assembly-plugin</artifactId>
                <configuration>
                    <descriptors>
                        <descriptor>../src/main/assemblies/empty.xml</descriptor>
                    </descriptors>
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>org.apache.oozie.server.EmbeddedOozieServer</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <version>2.10</version>
                <executions>
                    <execution>
                        <id>copy-dependencies</id>
                        <phase>package</phase>
                        <goals>
                            <goal>copy-dependencies</goal>
                        </goals>
                        <configuration>
                            <artifactItems>
                                <artifactItem>
                                    <groupId>org.eclipse.jetty</groupId>
                                    <artifactId>jetty-server</artifactId>
                                </artifactItem>
                                <artifactItem>
                                    <groupId>org.eclipse.jetty</groupId>
                                    <artifactId>jetty-util</artifactId>
                                </artifactItem>
                                <artifactItem>
                                    <groupId>org.eclipse.jetty</groupId>
                                    <artifactId>jetty-http</artifactId>
                                </artifactItem>
                                <artifactItem>
                                    <groupId>org.eclipse.jetty</groupId>
                                    <artifactId>jetty-io</artifactId>
                                </artifactItem>
                                <artifactItem>
                                    <groupId>org.eclipse.jetty</groupId>
                                    <artifactId>jetty-webapp</artifactId>
                                </artifactItem>
                                <artifactItem>
                                    <groupId>org.eclipse.jetty</groupId>
                                    <artifactId>jetty-servlet</artifactId>
                                </artifactItem>
                                <artifactItem>
                                    <groupId>org.eclipse.jetty</groupId>
                                    <artifactId>jetty-security</artifactId>
                                </artifactItem>
                                <artifactItem>
                                    <groupId>org.eclipse.jetty</groupId>
                                    <artifactId>jetty-jsp</artifactId>
                                </artifactItem>
                                <artifactItem>
                                    <groupId>org.eclipse.jetty</groupId>
                                    <artifactId>jetty-plus</artifactId>
                                </artifactItem>
                                <artifactItem>
                                    <groupId>org.eclipse.jetty</groupId>
                                    <artifactId>jetty-annotations</artifactId>
                                </artifactItem>

                                <!--  JSP support -->
                                <artifactItem>
                                    <groupId>org.eclipse.jetty</groupId>
                                    <artifactId>jetty-annotations</artifactId>
                                </artifactItem>
                                <artifactItem>
                                    <groupId>org.eclipse.jetty</groupId>
                                    <artifactId>jetty-webapp</artifactId>
                                </artifactItem>
                                <artifactItem>
                                    <groupId>org.eclipse.jetty</groupId>
                                    <artifactId>apache-jsp</artifactId>
                                    <type>jar</type>
                                </artifactItem>
                                <artifactItem>
                                    <groupId>org.eclipse.jetty</groupId>
                                    <artifactId>jetty-plus</artifactId>
                                </artifactItem>

                                <artifactItem>
                                    <groupId>org.apache.oozie</groupId>
                                    <artifactId>webapp</artifactId>
                                </artifactItem>

                            </artifactItems>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
diff --git a/server/src/main/assemblies/empty.xml b/server/src/main/assemblies/empty.xml
new file mode 100644
index 000000000..17ff68a8b
-- /dev/null
++ b/server/src/main/assemblies/empty.xml
@@ -0,0 +1,21 @@
<!--
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
-->
<assembly>
    <id>empty</id>
    <formats/>
</assembly>
diff --git a/server/src/main/java/org/apache/oozie/server/EmbeddedOozieServer.java b/server/src/main/java/org/apache/oozie/server/EmbeddedOozieServer.java
new file mode 100644
index 000000000..b7918b735
-- /dev/null
++ b/server/src/main/java/org/apache/oozie/server/EmbeddedOozieServer.java
@@ -0,0 +1,206 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.server;

import com.google.common.base.Preconditions;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.hadoop.conf.Configuration;
import org.apache.oozie.server.guice.OozieGuiceModule;
import org.apache.oozie.service.ConfigurationService;
import org.apache.oozie.service.ServiceException;
import org.apache.oozie.service.Services;
import org.apache.oozie.util.ConfigUtils;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 *  Class to start Oozie inside an embedded Jetty server.
 */
public class EmbeddedOozieServer {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedOozieServer.class);
    protected Server server;
    private int httpPort;
    private int httpsPort;
    private final WebAppContext servletContextHandler;
    private final ServletMapper oozieServletMapper;
    private final FilterMapper oozieFilterMapper;
    private JspHandler jspHandler;
    private Services serviceController;
    private SSLServerConnectorFactory sslServerConnectorFactory;
    private Configuration conf;
    private final RewriteHandler oozieRewriteHandler;
    private final ConstraintSecurityHandler constraintSecurityHandler;

    /**
     * Construct Oozie server
     * @param server jetty server to be embedded
     * @param jspHandler handler responsible for setting webapp context for JSP
     * @param serviceController controller for Oozie services; must be already initialized
     * @param sslServerConnectorFactory factory to create server connector configured for SSL
     * @param oozieRewriteHandler URL rewriter
     * @param servletContextHandler main web application context handler
     * @param oozieServletMapper maps servlets to URLs
     * @param oozieFilterMapper  maps filters
     * @param constraintSecurityHandler
     */
    @Inject
    public EmbeddedOozieServer(final Server server,
                               final JspHandler jspHandler,
                               final Services serviceController,
                               final SSLServerConnectorFactory sslServerConnectorFactory,
                               final RewriteHandler oozieRewriteHandler,
                               final WebAppContext servletContextHandler,
                               final ServletMapper oozieServletMapper,
                               final FilterMapper oozieFilterMapper,
                               final ConstraintSecurityHandler constraintSecurityHandler)
    {
        this.constraintSecurityHandler = constraintSecurityHandler;
        this.serviceController = Preconditions.checkNotNull(serviceController, "serviceController is null");
        this.jspHandler = Preconditions.checkNotNull(jspHandler, "jspHandler is null");
        this.sslServerConnectorFactory = Preconditions.checkNotNull(sslServerConnectorFactory,
                "sslServerConnectorFactory is null");
        this.server = Preconditions.checkNotNull(server, "server is null");
        this.oozieRewriteHandler = Preconditions.checkNotNull(oozieRewriteHandler, "rewriter is null");
        this.servletContextHandler = Preconditions.checkNotNull(servletContextHandler, "servletContextHandler is null");
        this.oozieServletMapper = Preconditions.checkNotNull(oozieServletMapper, "oozieServletMapper is null");
        this.oozieFilterMapper = Preconditions.checkNotNull(oozieFilterMapper, "oozieFilterMapper is null");
    }

    /**
     * Set up the Oozie server by configuring jetty server settings and starts Oozie services
     *
     * @throws URISyntaxException
     * @throws IOException
     * @throws ServiceException
     */
    public void setup() throws URISyntaxException, IOException, ServiceException {
        conf = serviceController.get(ConfigurationService.class).getConf();

        httpPort = getConfigPort(ConfigUtils.OOZIE_HTTP_PORT);

        HttpConfiguration httpConfiguration = new HttpConfigurationWrapper(conf).getDefaultHttpConfiguration();

        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfiguration));
        connector.setPort(httpPort);
        connector.setHost(conf.get(ConfigUtils.OOZIE_HTTP_HOSTNAME));

        HandlerCollection handlerCollection = new HandlerCollection();

        if (isSecured()) {
            httpsPort =  getConfigPort(ConfigUtils.OOZIE_HTTPS_PORT);
            ServerConnector sslConnector = sslServerConnectorFactory.createSecureServerConnector(httpsPort, conf, server);
            server.setConnectors(new Connector[]{connector, sslConnector});
            constraintSecurityHandler.setHandler(servletContextHandler);
            handlerCollection.addHandler(constraintSecurityHandler);
        }
        else {
            server.setConnectors(new Connector[]{connector});
        }

        servletContextHandler.setContextPath("/oozie/");
        oozieServletMapper.mapOozieServlets();
        oozieFilterMapper.addFilters();

        servletContextHandler.setParentLoaderPriority(true);
        jspHandler.setupWebAppContext(servletContextHandler);

        addErrorHandler();

        handlerCollection.addHandler(servletContextHandler);
        handlerCollection.addHandler(oozieRewriteHandler);
        server.setHandler(handlerCollection);
    }

    private void addErrorHandler() {
        ErrorPageErrorHandler errorHandler = new ErrorPageErrorHandler();
        errorHandler.addErrorPage(404, "/404.html");
        errorHandler.addErrorPage(403, "/403.html");
        servletContextHandler.setErrorHandler(errorHandler);
    }

    private int getConfigPort(String confVar) {
        String confHttpPort = conf.get(confVar);
        int port;
        try {
            port = Integer.parseInt(confHttpPort);
        }
        catch (final NumberFormatException nfe) {
            throw new NumberFormatException(String.format("Port number for '%s \"%s\" ('%s') is not an integer.",
                    confVar, confHttpPort, confHttpPort));
        }
        return port;
    }

    private boolean isSecured() {
        String isSSLEnabled = conf.get("oozie.https.enabled");
        LOG.info("Server started with oozie.https.enabled = " + isSSLEnabled);
        return isSSLEnabled != null && Boolean.valueOf(isSSLEnabled);
    }


    public void start() throws Exception {
        server.start();
        LOG.info("Server started.");
    }

    public void join() throws InterruptedException {
        server.join();
    }

    public void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                LOG.info("Shutting down.");
                serviceController.destroy();
                LOG.info("Oozie services stopped.");
            }
        });
    }

    public static void main(String[] args) throws Exception {
        final Injector guiceInjector = Guice.createInjector(new OozieGuiceModule());

        final EmbeddedOozieServer embeddedOozieServer = guiceInjector.getInstance(EmbeddedOozieServer.class);

        embeddedOozieServer.setup();
        embeddedOozieServer.addShutdownHook();
        try {
            embeddedOozieServer.start();
        } catch (Exception e) {
            LOG.error("Could not start EmbeddedOozieServer!", e);
            System.exit(1);
        }
        embeddedOozieServer.join();
    }
}
diff --git a/server/src/main/java/org/apache/oozie/server/FilterMapper.java b/server/src/main/java/org/apache/oozie/server/FilterMapper.java
new file mode 100644
index 000000000..bd7861799
-- /dev/null
++ b/server/src/main/java/org/apache/oozie/server/FilterMapper.java
@@ -0,0 +1,61 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.server;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.apache.oozie.servlet.AuthFilter;
import org.apache.oozie.servlet.HostnameFilter;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;

import javax.servlet.DispatcherType;
import java.util.EnumSet;

public class FilterMapper {
    private final WebAppContext servletContextHandler;

    @Inject
    public FilterMapper(final WebAppContext servletContextHandler) {
        this.servletContextHandler = Preconditions.checkNotNull(servletContextHandler, "ServletContextHandler is null");
    }

    /**
     *  Map filters to endpoints. Make sure it in sync with ServletMapper when making changes
     * */
    void addFilters() {
        mapFilter(new FilterHolder(new HostnameFilter()), "/*");

        FilterHolder authFilter = new FilterHolder(new AuthFilter());
        mapFilter(authFilter, "/versions/*");
        mapFilter(authFilter, "/v0/*");
        mapFilter(authFilter, "/v1/*");
        mapFilter(authFilter, "/v2/*");
        mapFilter(authFilter, "/index.jsp");
        mapFilter(authFilter, "/admin/*");
        mapFilter(authFilter, "/*.js");
        mapFilter(authFilter, "/ext-2.2/*");
        mapFilter(authFilter, "/docs/*");
    }

    private void mapFilter(FilterHolder authFilter, String pathSpec) {
        servletContextHandler.addFilter(authFilter, pathSpec, EnumSet.of(DispatcherType.REQUEST));
    }
}
diff --git a/server/src/main/java/org/apache/oozie/server/HttpConfigurationWrapper.java b/server/src/main/java/org/apache/oozie/server/HttpConfigurationWrapper.java
new file mode 100644
index 000000000..0341f9cea
-- /dev/null
++ b/server/src/main/java/org/apache/oozie/server/HttpConfigurationWrapper.java
@@ -0,0 +1,63 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.oozie.server;

import com.google.common.base.Preconditions;
import org.apache.hadoop.conf.Configuration;
import org.eclipse.jetty.server.HttpConfiguration;

/**
 *  Class that wraps HTTP configuration settings.
 */
public class HttpConfigurationWrapper {
    public static final String OOZIE_HTTP_REQUEST_HEADER_SIZE = "oozie.http.request.header.size";
    public static final String OOZIE_HTTP_RESPONSE_HEADER_SIZE = "oozie.http.response.header.size";
    private Configuration conf;

    public HttpConfigurationWrapper(Configuration conf) {
        this.conf = Preconditions.checkNotNull(conf, "conf");
    }

    /**
     * Set up and return default HTTP configuration for the Oozie server
     * @return default HttpConfiguration with the configured request and response header size
     */
    public HttpConfiguration getDefaultHttpConfiguration() {
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setRequestHeaderSize(
                getConfigHeaderSize(OOZIE_HTTP_REQUEST_HEADER_SIZE));
        httpConfiguration.setResponseHeaderSize(
                getConfigHeaderSize(OOZIE_HTTP_RESPONSE_HEADER_SIZE));
        httpConfiguration.setSendServerVersion(false);
        httpConfiguration.setSendXPoweredBy(false);
        return httpConfiguration;
    }

    private int getConfigHeaderSize(String confVar) {
        String confHeaderSize = conf.get(confVar);
        int headerSize;
        try {
            headerSize = Integer.parseInt(confHeaderSize);
        }
        catch (final NumberFormatException nfe) {
            throw new NumberFormatException(String.format("Header size for %s \"%s\" ( '%s') is not an integer.",
                    confVar, confVar, confHeaderSize));
        }
        return headerSize;
    }
}
diff --git a/server/src/main/java/org/apache/oozie/server/JspHandler.java b/server/src/main/java/org/apache/oozie/server/JspHandler.java
new file mode 100644
index 000000000..9658fd698
-- /dev/null
++ b/server/src/main/java/org/apache/oozie/server/JspHandler.java
@@ -0,0 +1,161 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.server;

import com.google.common.base.Preconditions;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.annotations.ServletContainerInitializersStarter;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.jsp.JettyJspServlet;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class that is used to handle JSP requests in Oozie server.
 */
public class JspHandler {
    private static final Logger LOG = LoggerFactory.getLogger(JspHandler.class);
    private final File scratchDir;
    private final WebRootResourceLocator webRootResourceLocator;

    public JspHandler(final File scratchDir, final WebRootResourceLocator webRootResourceLocator) {
        this.scratchDir = scratchDir;
        this.webRootResourceLocator = webRootResourceLocator;
    }

    /**
     * Establish Scratch directory for the servlet context (used by JSP compilation)
     */
    private File getScratchDir() throws IOException
    {
        if (scratchDir.exists()) {
            LOG.info(String.format("Scratch directory exists and will be reused: %s", scratchDir.getAbsolutePath()));
            return scratchDir;
        }

        if (!scratchDir.mkdirs()) {
            throw new IOException("Unable to create scratch directory: " + scratchDir);
        }

        LOG.info(String.format("Scratch directory created: %s", scratchDir.getAbsolutePath()));
        return scratchDir;
    }

    /**
     * Setup the basic application "context" for this application at "/"
     * This is also known as the handler tree (in jetty speak)
     */
    public void setupWebAppContext(WebAppContext servletContextHandler)
            throws IOException, URISyntaxException
    {
        Preconditions.checkNotNull(servletContextHandler, "servletContextHandler is null");

        File scratchDir = getScratchDir();
        servletContextHandler.setAttribute("javax.servlet.context.tempdir", scratchDir);
        servletContextHandler.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/.*taglibs.*\\.jar$");
        URI baseUri = webRootResourceLocator.getWebRootResourceUri();
        servletContextHandler.setResourceBase(baseUri.toASCIIString());
        servletContextHandler.setAttribute("org.eclipse.jetty.containerInitializers", jspInitializers());
        servletContextHandler.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
        servletContextHandler.addBean(new ServletContainerInitializersStarter(servletContextHandler), true);
        servletContextHandler.setClassLoader(getUrlClassLoader());

        servletContextHandler.addServlet(jspServletHolder(), "*.jsp");

        servletContextHandler.addServlet(jspFileMappedServletHolder(), "/oozie/");
        servletContextHandler.addServlet(defaultServletHolder(baseUri), "/");
    }

    /**
     * Ensure the jsp engine is initialized correctly
     */
    private List<ContainerInitializer> jspInitializers()
    {
        JettyJasperInitializer sci = new JettyJasperInitializer();
        ContainerInitializer initializer = new ContainerInitializer(sci, null);
        List<ContainerInitializer> initializers = new ArrayList<>();
        initializers.add(initializer);
        return initializers;
    }

    /**
     * Set Classloader of Context to be sane (needed for JSTL)
     * JSP requires a non-System classloader, this simply wraps the
     * embedded System classloader in a way that makes it suitable
     * for JSP to use
     */
    private ClassLoader getUrlClassLoader()
    {
        ClassLoader jspClassLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());
        return jspClassLoader;
    }

    /**
     * Create JSP Servlet (must be named "jsp")
     */
    private ServletHolder jspServletHolder()
    {
        ServletHolder holderJsp = new ServletHolder("jsp", JettyJspServlet.class);
        holderJsp.setInitOrder(0);
        holderJsp.setInitParameter("logVerbosityLevel", "DEBUG");
        holderJsp.setInitParameter("fork", "false");
        holderJsp.setInitParameter("xpoweredBy", "false");
        holderJsp.setInitParameter("compilerTargetVM", "1.7");
        holderJsp.setInitParameter("compilerSourceVM", "1.7");
        holderJsp.setInitParameter("keepgenerated", "true");
        return holderJsp;
    }

    /**
     * Create Example of mapping jsp to path spec
     */
    private ServletHolder jspFileMappedServletHolder()
    {
        ServletHolder holderAltMapping = new ServletHolder();
        holderAltMapping.setName("index.jsp");
        holderAltMapping.setForcedPath("/index.jsp");
        return holderAltMapping;
    }

    /**
     * Create Default Servlet (must be named "default")
     */
    private ServletHolder defaultServletHolder(URI baseUri)
    {
        ServletHolder holderDefault = new ServletHolder("default", DefaultServlet.class);
        holderDefault.setInitParameter("resourceBase", baseUri.toASCIIString());
        holderDefault.setInitParameter("dirAllowed", "true");
        return holderDefault;
    }
}
diff --git a/server/src/main/java/org/apache/oozie/server/SSLServerConnectorFactory.java b/server/src/main/java/org/apache/oozie/server/SSLServerConnectorFactory.java
new file mode 100644
index 000000000..2797cf433
-- /dev/null
++ b/server/src/main/java/org/apache/oozie/server/SSLServerConnectorFactory.java
@@ -0,0 +1,136 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.server;


import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.apache.hadoop.conf.Configuration;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Factory that is used to configure SSL settings for the Oozie server.
 */
class SSLServerConnectorFactory {
    private static final Logger LOG = LoggerFactory.getLogger(SSLServerConnectorFactory.class);
    public static final String OOZIE_HTTPS_TRUSTSTORE_FILE = "oozie.https.truststore.file";
    public static final String OOZIE_HTTPS_TRUSTSTORE_PASS = "oozie.https.truststore.pass";
    public static final String OOZIE_HTTPS_KEYSTORE_PASS = "oozie.https.keystore.pass";
    public static final String OOZIE_HTTPS_KEYSTORE_FILE = "oozie.https.keystore.file";

    private SslContextFactory sslContextFactory;
    private Configuration conf;

    @Inject
    public SSLServerConnectorFactory(final SslContextFactory sslContextFactory) {
        this.sslContextFactory = Preconditions.checkNotNull(sslContextFactory,  "sslContextFactory is null");
    }

    /**
     *  Construct a ServerConnector object with SSL settings
     *
     *  @param oozieHttpsPort Oozie HTTPS port
     *  @param conf Oozie configuration
     *  @param server jetty Server which the connector is attached to
     *
     *  @return ServerConnector
    */
    public ServerConnector createSecureServerConnector(int oozieHttpsPort, Configuration conf, Server server) {
        this.conf = Preconditions.checkNotNull(conf, "conf is null");
        Preconditions.checkNotNull(server, "server is null");
        Preconditions.checkState(oozieHttpsPort >= 1 && oozieHttpsPort <= 65535,
                String.format("Invalid port number specified: \'%d\'. It should be between 1 and 65535.", oozieHttpsPort));

        setIncludeProtocols();
        setCipherSuites();
        setTrustStorePath();
        setTrustStorePass();

        setKeyStoreFile();
        setKeystorePass();

        HttpConfiguration httpsConfiguration = getHttpsConfiguration();
        ServerConnector secureServerConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(httpsConfiguration));

        secureServerConnector.setPort(oozieHttpsPort);

        LOG.info(String.format("Secure server connector created, listenning on port %d", oozieHttpsPort));
        return secureServerConnector;
    }

    private void setCipherSuites() {
        String excludeCipherList = conf.get("oozie.https.exclude.cipher.suites");
        String[] excludeCipherSuites = excludeCipherList.split(",");
        sslContextFactory.setExcludeCipherSuites(excludeCipherSuites);

        LOG.info(String.format("SSL context - excluding cipher suites: %s", Arrays.toString(excludeCipherSuites)));
    }

    private void setIncludeProtocols() {
        String enabledProtocolsList = conf.get("oozie.https.include.protocols");
        String[] enabledProtocols = enabledProtocolsList.split(",");
        sslContextFactory.setIncludeProtocols(enabledProtocols);

        LOG.info(String.format("SSL context - including protocols: %s", Arrays.toString(enabledProtocols)));
    }

    private void setTrustStorePath() {
        String trustStorePath = conf.get(OOZIE_HTTPS_TRUSTSTORE_FILE);
        Preconditions.checkNotNull(trustStorePath, "trustStorePath is null");
        sslContextFactory.setTrustStorePath(trustStorePath);
    }

    private void setTrustStorePass() {
        String trustStorePass = conf.get(OOZIE_HTTPS_TRUSTSTORE_PASS);
        Preconditions.checkNotNull(trustStorePass, "setTrustStorePass is null");
        sslContextFactory.setTrustStorePassword(trustStorePass);
    }

    private void setKeystorePass() {
        String keystorePass = conf.get(OOZIE_HTTPS_KEYSTORE_PASS);
        Preconditions.checkNotNull(keystorePass, "keystorePass is null");
        sslContextFactory.setKeyManagerPassword(keystorePass);
    }

    private void setKeyStoreFile() {
        String keystoreFile = conf.get(OOZIE_HTTPS_KEYSTORE_FILE);
        Preconditions.checkNotNull(keystoreFile, "keystoreFile is null");
        sslContextFactory.setKeyStorePath(keystoreFile);
    }

    private HttpConfiguration getHttpsConfiguration() {
        HttpConfiguration https = new HttpConfigurationWrapper(conf).getDefaultHttpConfiguration();
        https.setSecureScheme("https");
        https.addCustomizer(new SecureRequestCustomizer());
        return https;
    }
}
diff --git a/server/src/main/java/org/apache/oozie/server/ServletMapper.java b/server/src/main/java/org/apache/oozie/server/ServletMapper.java
new file mode 100644
index 000000000..ae27ac3d5
-- /dev/null
++ b/server/src/main/java/org/apache/oozie/server/ServletMapper.java
@@ -0,0 +1,95 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.server;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.apache.oozie.servlet.CallbackServlet;
import org.apache.oozie.servlet.SLAServlet;
import org.apache.oozie.servlet.V0AdminServlet;
import org.apache.oozie.servlet.V0JobServlet;
import org.apache.oozie.servlet.V0JobsServlet;
import org.apache.oozie.servlet.V1AdminServlet;
import org.apache.oozie.servlet.V1JobServlet;
import org.apache.oozie.servlet.V2AdminServlet;
import org.apache.oozie.servlet.V2JobServlet;
import org.apache.oozie.servlet.V2SLAServlet;
import org.apache.oozie.servlet.V2ValidateServlet;
import org.apache.oozie.servlet.VersionServlet;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.webapp.WebAppContext;

import javax.servlet.Servlet;


public class ServletMapper {
    private final WebAppContext servletContextHandler;

    @Inject
    public ServletMapper(final WebAppContext servletContextHandler) {
        this.servletContextHandler = Preconditions.checkNotNull(servletContextHandler, "ServletContextHandler is null");
    }
    /**
     * Maps Oozie servlets to path specs. Make sure it is in sync with FilterMapper when making changes.
     * */
    void mapOozieServlets() {
        mapServlet(VersionServlet.class, "/versions");
        mapServlet(V0AdminServlet.class, "/v0/admin/*");
        mapServlet(V1AdminServlet.class, "/v1/admin/*");
        mapServlet(V2AdminServlet.class, "/v2/admin/*");

        mapServlet(CallbackServlet.class, "/callback/*");

        ServletHandler servletHandler = servletContextHandler.getServletHandler();
        String voJobservletName = V0JobsServlet.class.getSimpleName();
        servletHandler.addServlet(new ServletHolder(voJobservletName, new V0JobsServlet()));
        ServletMapping jobServletMappingV0 = new ServletMapping();
        jobServletMappingV0.setPathSpec("/v0/jobs");
        jobServletMappingV0.setServletName(voJobservletName);

        ServletMapping jobServletMappingV1 = new ServletMapping();
        jobServletMappingV1.setPathSpec("/v1/jobs");
        jobServletMappingV1.setServletName(voJobservletName);

        ServletMapping jobServletMappingV2 = new ServletMapping();
        jobServletMappingV2.setPathSpec("/v2/jobs");
        jobServletMappingV2.setServletName(voJobservletName);

        servletHandler.addServletMapping(jobServletMappingV0);
        servletHandler.addServletMapping(jobServletMappingV1);
        servletHandler.addServletMapping(jobServletMappingV2);

        mapServlet(V0JobServlet.class, "/v0/job/*");
        mapServlet(V1JobServlet.class, "/v1/job/*");
        mapServlet(V2JobServlet.class, "/v2/job/*");
        mapServlet(SLAServlet.class, "/v1/sla/*");
        mapServlet(V2SLAServlet.class, "/v2/sla/*");
        mapServlet(V2ValidateServlet.class, "/v2/validate/*");
    }

    private void mapServlet(final Class<? extends Servlet> servletClass, final String servletPath) {
        try {
            servletContextHandler.addServlet(new ServletHolder(servletClass.newInstance()), servletPath);
        } catch (final InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
diff --git a/server/src/main/java/org/apache/oozie/server/WebRootResourceLocator.java b/server/src/main/java/org/apache/oozie/server/WebRootResourceLocator.java
new file mode 100644
index 000000000..190d1c203
-- /dev/null
++ b/server/src/main/java/org/apache/oozie/server/WebRootResourceLocator.java
@@ -0,0 +1,39 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.server;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class WebRootResourceLocator {
    private static final String WEBROOT_INDEX = "/webapp/";

    public URI getWebRootResourceUri() throws FileNotFoundException, URISyntaxException
    {
        URL indexUri = JspHandler.class.getResource(WebRootResourceLocator.WEBROOT_INDEX);
        if (indexUri == null)
        {
            throw new FileNotFoundException("Unable to find resource " + WebRootResourceLocator.WEBROOT_INDEX);
        }
        // Points to wherever /webroot/ (the resource) is
        return indexUri.toURI();
    }
}
diff --git a/server/src/main/java/org/apache/oozie/server/guice/ConstraintSecurityHandlerProvider.java b/server/src/main/java/org/apache/oozie/server/guice/ConstraintSecurityHandlerProvider.java
new file mode 100644
index 000000000..6c313fe67
-- /dev/null
++ b/server/src/main/java/org/apache/oozie/server/guice/ConstraintSecurityHandlerProvider.java
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
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.server.guice;

import com.google.inject.Provider;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.util.security.Constraint;

import java.util.Arrays;

class ConstraintSecurityHandlerProvider implements Provider<ConstraintSecurityHandler> {
    @Override
    public ConstraintSecurityHandler get() {
        ConstraintMapping callbackConstraintMapping = new ConstraintMapping();
        callbackConstraintMapping.setPathSpec("/callback/*");
        Constraint unsecureConstraint = new Constraint();
        unsecureConstraint.setDataConstraint(Constraint.DC_NONE);
        callbackConstraintMapping.setConstraint(unsecureConstraint);

        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec("/*");
        Constraint constraint = new Constraint();
        constraint.setDataConstraint(Constraint.DC_CONFIDENTIAL);
        mapping.setConstraint(constraint);

        ConstraintSecurityHandler security = new ConstraintSecurityHandler();
        security.setConstraintMappings(Arrays.asList(callbackConstraintMapping, mapping));
        return security;
    }
}
diff --git a/server/src/main/java/org/apache/oozie/server/guice/JettyServerProvider.java b/server/src/main/java/org/apache/oozie/server/guice/JettyServerProvider.java
new file mode 100644
index 000000000..6580a9a7d
-- /dev/null
++ b/server/src/main/java/org/apache/oozie/server/guice/JettyServerProvider.java
@@ -0,0 +1,48 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.server.guice;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.hadoop.conf.Configuration;
import org.apache.oozie.service.ConfigurationService;
import org.apache.oozie.service.Services;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

class JettyServerProvider implements Provider<Server> {
    public static final String OOZIE_SERVER_THREADPOOL_MAX_THREADS = "oozie.server.threadpool.max.threads";
    private final Configuration oozieConfiguration;

    @Inject
    public JettyServerProvider(final Services oozieServices) {
        oozieConfiguration = oozieServices.get(ConfigurationService.class).getConf();
    }

    @Override
    public Server get() {
        final QueuedThreadPool threadPool = new QueuedThreadPool();

        final int maxThreads = Integer.parseInt(
                oozieConfiguration.get(OOZIE_SERVER_THREADPOOL_MAX_THREADS));
        threadPool.setMaxThreads(maxThreads);

        return new Server(threadPool);
    }
}
diff --git a/server/src/main/java/org/apache/oozie/server/guice/JspHandlerProvider.java b/server/src/main/java/org/apache/oozie/server/guice/JspHandlerProvider.java
new file mode 100644
index 000000000..8a54a9a6d
-- /dev/null
++ b/server/src/main/java/org/apache/oozie/server/guice/JspHandlerProvider.java
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
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.server.guice;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.hadoop.conf.Configuration;
import org.apache.oozie.server.JspHandler;
import org.apache.oozie.server.WebRootResourceLocator;
import org.apache.oozie.service.ConfigurationService;
import org.apache.oozie.service.Services;

import java.io.File;

public class JspHandlerProvider implements Provider<JspHandler> {
    public static final String OOZIE_JSP_TMP_DIR = "oozie.jsp.tmp.dir";
    public static final String EMBEDDED_JETTY_JSP_DIR = "embedded-jetty-jsp";
    private final Configuration oozieConfiguration;

    @Inject
    public JspHandlerProvider(final Services oozieServices) {
        oozieConfiguration = oozieServices.get(ConfigurationService.class).getConf();
    }

    @Override
    public JspHandler get() {
        final File tempDir = new File(oozieConfiguration.get(OOZIE_JSP_TMP_DIR), EMBEDDED_JETTY_JSP_DIR);

        return new JspHandler(tempDir, new WebRootResourceLocator());
    }
}
diff --git a/server/src/main/java/org/apache/oozie/server/guice/OozieGuiceModule.java b/server/src/main/java/org/apache/oozie/server/guice/OozieGuiceModule.java
new file mode 100644
index 000000000..bb79f0f79
-- /dev/null
++ b/server/src/main/java/org/apache/oozie/server/guice/OozieGuiceModule.java
@@ -0,0 +1,45 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.server.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.apache.oozie.server.JspHandler;
import org.apache.oozie.service.Services;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

public class OozieGuiceModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Services.class).toProvider(ServicesProvider.class).in(Singleton.class);

        bind(Server.class).toProvider(JettyServerProvider.class).in(Singleton.class);

        bind(WebAppContext.class).in(Singleton.class);

        bind(ConstraintSecurityHandler.class).toProvider(ConstraintSecurityHandlerProvider.class).in(Singleton.class);

        bind(JspHandler.class).toProvider(JspHandlerProvider.class).in(Singleton.class);

        bind(RewriteHandler.class).toProvider(RewriteHandlerProvider.class).in(Singleton.class);
    }
}
diff --git a/server/src/main/java/org/apache/oozie/server/guice/RewriteHandlerProvider.java b/server/src/main/java/org/apache/oozie/server/guice/RewriteHandlerProvider.java
new file mode 100644
index 000000000..e54d0cb9f
-- /dev/null
++ b/server/src/main/java/org/apache/oozie/server/guice/RewriteHandlerProvider.java
@@ -0,0 +1,44 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.server.guice;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.eclipse.jetty.rewrite.handler.RedirectPatternRule;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;

class RewriteHandlerProvider implements Provider<RewriteHandler> {
    private final RewriteHandler rewriteHandler;

    @Override
    public RewriteHandler get() {
        return rewriteHandler;
    }

    @Inject
    public RewriteHandlerProvider(final RedirectPatternRule redirectPatternRule) {
        this.rewriteHandler = new RewriteHandler();

        redirectPatternRule.setPattern("");
        redirectPatternRule.setLocation("/oozie");
        redirectPatternRule.setTerminating(true);

        this.rewriteHandler.addRule(redirectPatternRule);
    }
}
diff --git a/server/src/main/java/org/apache/oozie/server/guice/ServicesProvider.java b/server/src/main/java/org/apache/oozie/server/guice/ServicesProvider.java
new file mode 100644
index 000000000..cc4ed1798
-- /dev/null
++ b/server/src/main/java/org/apache/oozie/server/guice/ServicesProvider.java
@@ -0,0 +1,39 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.server.guice;

import com.google.inject.Provider;
import org.apache.oozie.service.ServiceException;
import org.apache.oozie.service.Services;

class ServicesProvider implements Provider<Services> {
    @Override
    public Services get() {
        try {
            final Services oozieServices = new Services();

            oozieServices.init();

            return oozieServices;
        } catch (ServiceException e) {
            throw new ExceptionInInitializerError(
                    String.format("Could not instantiate Oozie services. [e.message=%s]", e.getMessage()));
        }
    }
}
diff --git a/server/src/main/resources/checkstyle-header.txt b/server/src/main/resources/checkstyle-header.txt
new file mode 100644
index 000000000..424745248
-- /dev/null
++ b/server/src/main/resources/checkstyle-header.txt
@@ -0,0 +1,17 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
\ No newline at end of file
diff --git a/server/src/main/resources/checkstyle.xml b/server/src/main/resources/checkstyle.xml
new file mode 100644
index 000000000..6e8be5d11
-- /dev/null
++ b/server/src/main/resources/checkstyle.xml
@@ -0,0 +1,41 @@
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC "-//Puppy Crawl//DTD Check Configuration 1.2//EN" "http://www.puppycrawl.com/dtds/configuration_1_2.dtd">
<!--
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
-->

<module name="Checker">

    <module name="RegexpSingleline">
        <property name="severity" value="warning"/>
        <property name="format" value="\s+$"/>
        <property name="message" value="Line has trailing spaces."/>
    </module>

    <module name="Header">
        <property name="headerFile" value="${checkstyle.header.file}"/>
    </module>

    <module name="TreeWalker">
        <module name="LineLength">
            <property name="severity" value="warning"/>
            <property name="max" value="132"/>
        </module>
    </module>

</module>

diff --git a/server/src/test/java/org/apache/oozie/server/TestEmbeddedOozieServer.java b/server/src/test/java/org/apache/oozie/server/TestEmbeddedOozieServer.java
new file mode 100644
index 000000000..0f36e8c57
-- /dev/null
++ b/server/src/test/java/org/apache/oozie/server/TestEmbeddedOozieServer.java
@@ -0,0 +1,119 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.server;

import org.apache.hadoop.conf.Configuration;
import org.apache.oozie.service.ConfigurationService;
import org.apache.oozie.service.ServiceException;
import org.apache.oozie.service.Services;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 *  Server tests
 */
@RunWith(MockitoJUnitRunner.class)
public class TestEmbeddedOozieServer {
    @Mock private JspHandler mockJspHandler;
    @Mock private Services mockServices;
    @Mock private SslContextFactory mockSSLContextFactory;
    @Mock private SSLServerConnectorFactory mockSSLServerConnectorFactory;
    @Mock private Server mockServer;
    @Mock private ServerConnector mockServerConnector;
    @Mock private ConfigurationService mockConfigService;
    @Mock private Configuration mockConfiguration;
    @Mock private RewriteHandler mockOozieRewriteHandler;
    @Mock private EmbeddedOozieServer embeddedOozieServer;
    @Mock private WebAppContext servletContextHandler;
    @Mock private ServletMapper oozieServletMapper;
    @Mock private FilterMapper oozieFilterMapper;
    @Mock private ConstraintSecurityHandler constraintSecurityHandler;

    @Before public void setUp() {
        embeddedOozieServer = new EmbeddedOozieServer(mockServer, mockJspHandler, mockServices, mockSSLServerConnectorFactory,
                mockOozieRewriteHandler, servletContextHandler, oozieServletMapper, oozieFilterMapper, constraintSecurityHandler);

        doReturn("11000").when(mockConfiguration).get("oozie.http.port");
        doReturn("11443").when(mockConfiguration).get("oozie.https.port");
        doReturn("65536").when(mockConfiguration).get("oozie.http.request.header.size");
        doReturn("65536").when(mockConfiguration).get("oozie.http.response.header.size");
        doReturn("42").when(mockConfiguration).get("oozie.server.threadpool.max.threads");
        doReturn(mockConfiguration).when(mockConfigService).getConf();
        doReturn(mockConfigService).when(mockServices).get(ConfigurationService.class);
    }

    @After public void tearDown() {
        verify(mockServices).get(ConfigurationService.class);

        verifyNoMoreInteractions(
                mockJspHandler,
                mockServices,
                mockServerConnector,
                mockSSLServerConnectorFactory);
    }

    @Test
    public void testServerSetup() throws Exception {
        doReturn("false").when(mockConfiguration).get("oozie.https.enabled");
        embeddedOozieServer.setup();
        verify(mockJspHandler).setupWebAppContext(isA(WebAppContext.class));
    }

    @Test
    public void testSecureServerSetup() throws Exception {
        doReturn("true").when(mockConfiguration).get("oozie.https.enabled");

        ServerConnector mockSecuredServerConnector = new ServerConnector(embeddedOozieServer.server);
        doReturn(mockSecuredServerConnector)
                .when(mockSSLServerConnectorFactory)
                .createSecureServerConnector(anyInt(), any(Configuration.class), any(Server.class));

        embeddedOozieServer.setup();

        verify(mockJspHandler).setupWebAppContext(isA(WebAppContext.class));
        verify(mockSSLServerConnectorFactory).createSecureServerConnector(
                isA(Integer.class), isA(Configuration.class), isA(Server.class));
    }

    @Test(expected=NumberFormatException.class)
    public void numberFormatExceptionThrownWithInvalidHttpPort() throws ServiceException, IOException, URISyntaxException {
        doReturn("INVALID_PORT").when(mockConfiguration).get("oozie.http.port");
        embeddedOozieServer.setup();
    }
}
diff --git a/server/src/test/java/org/apache/oozie/server/TestJspHandler.java b/server/src/test/java/org/apache/oozie/server/TestJspHandler.java
new file mode 100644
index 000000000..741aa5d9c
-- /dev/null
++ b/server/src/test/java/org/apache/oozie/server/TestJspHandler.java
@@ -0,0 +1,94 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.server;

import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TestJspHandler {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock File mockScratchDir;
    @Mock WebAppContext mockWebAppContext;
    @Mock WebRootResourceLocator mockWebRootResourceLocator;

    private JspHandler jspHandler;

    @Before
    public void setUp() throws Exception {
        jspHandler = new JspHandler(mockScratchDir, mockWebRootResourceLocator);
        when(mockWebRootResourceLocator.getWebRootResourceUri()).thenReturn(new URI("/webroot"));
    }

    @After
    public void tearDown() throws Exception {
        verify(mockScratchDir).exists();
    }

    @Test
    public void scratchDir_Is_Created_When_Setup_Called_And_ScratchDir_Did_Not_Exist() throws IOException, URISyntaxException {
        when(mockScratchDir.exists()).thenReturn(false);
        when(mockScratchDir.mkdirs()).thenReturn(true);

        jspHandler.setupWebAppContext(mockWebAppContext);

        verify(mockScratchDir).mkdirs();
    }

    @Test
    public void scratchDir_Cannot_Be_Created_When_Setup_Called_And_ScratchDir_Did_Not_Exist()
            throws IOException, URISyntaxException {
        when(mockScratchDir.exists()).thenReturn(false);
        when(mockScratchDir.mkdirs()).thenReturn(false);

        expectedException.expect(IOException.class);
        jspHandler.setupWebAppContext(mockWebAppContext);

        verify(mockScratchDir).mkdirs();
    }

    @Test
    public void scratchDir_Is_Reused_When_Setup_Called_And_ScratchDir_Existed() throws IOException, URISyntaxException {
        when(mockScratchDir.exists()).thenReturn(true);

        jspHandler.setupWebAppContext(mockWebAppContext);

        verify(mockScratchDir, times(0)).mkdirs();
    }
}
diff --git a/server/src/test/java/org/apache/oozie/server/TestSSLServerConnectorFactory.java b/server/src/test/java/org/apache/oozie/server/TestSSLServerConnectorFactory.java
new file mode 100644
index 000000000..9634da83f
-- /dev/null
++ b/server/src/test/java/org/apache/oozie/server/TestSSLServerConnectorFactory.java
@@ -0,0 +1,137 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.server;

import org.apache.hadoop.conf.Configuration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 *  Server tests
 */
@RunWith(MockitoJUnitRunner.class)
public class TestSSLServerConnectorFactory {
    @Mock private SslContextFactory mockSSLContextFactory;
    @Mock private SSLServerConnectorFactory mockSSLServerConnectorFactory;
    @Mock private Server mockServer;
    @Mock private ServerConnector mockServerConnector;

    private Configuration testConfig;
    private SSLServerConnectorFactory sslServerConnectorFactory;

    @Before public void setUp() {
        testConfig = new Configuration();
        testConfig.set("oozie.https.truststore.file", "test_truststore_file");
        testConfig.set("oozie.https.truststore.pass", "trustpass");
        testConfig.set("oozie.https.keystore.file", "test_keystore_file");
        testConfig.set("oozie.https.keystore.pass", "keypass");
        testConfig.set("oozie.http.port", "11000");
        testConfig.set("oozie.http.request.header.size", "65536");
        testConfig.set("oozie.http.response.header.size", "65536");
        testConfig.set("oozie.https.include.protocols", "TLSv1,SSLv2Hello,TLSv1.1,TLSv1.2");
        testConfig.set("oozie.https.exclude.cipher.suites",
                "TLS_ECDHE_RSA_WITH_RC4_128_SHA,SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA,SSL_RSA_WITH_DES_CBC_SHA," +
                "SSL_DHE_RSA_WITH_DES_CBC_SHA,SSL_RSA_EXPORT_WITH_RC4_40_MD5,SSL_RSA_EXPORT_WITH_DES40_CBC_SHA," +
                "SSL_RSA_WITH_RC4_128_MD5");

        sslServerConnectorFactory = new SSLServerConnectorFactory(mockSSLContextFactory);
    }

    @After
    public void tearDown() {
        verify(mockSSLContextFactory).setTrustStorePath(anyString());
        verify(mockSSLContextFactory).setTrustStorePassword(anyString());
        verify(mockSSLContextFactory).setKeyStorePath(anyString());
        verify(mockSSLContextFactory).setKeyManagerPassword(anyString());
        verifyNoMoreInteractions(
                mockServerConnector,
                mockSSLServerConnectorFactory);
    }

    private void verifyDefaultExcludeCipherSuites() {
        verify(mockSSLContextFactory).setExcludeCipherSuites(
                "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
                "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_RSA_WITH_DES_CBC_SHA",
                "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_RSA_WITH_RC4_128_MD5");
    }

    private void verifyDefaultIncludeProtocols() {
        verify(mockSSLContextFactory).setIncludeProtocols(
                "TLSv1",
                "SSLv2Hello",
                "TLSv1.1",
                "TLSv1.2");
    }

    @Test
    public void includeProtocolsHaveDefaultValues() throws Exception {
        sslServerConnectorFactory.createSecureServerConnector(42, testConfig, mockServer);

        verifyDefaultIncludeProtocols();
        verifyDefaultExcludeCipherSuites();
    }

    @Test
    public void includeProtocolsCanBeSetViaConfigFile() throws Exception {
        SSLServerConnectorFactory sslServerConnectorFactory = new SSLServerConnectorFactory(mockSSLContextFactory);
        testConfig.set("oozie.https.include.protocols", "TLSv1,TLSv1.2");
        sslServerConnectorFactory.createSecureServerConnector(42, testConfig, mockServer);

        verify(mockSSLContextFactory).setIncludeProtocols(
                "TLSv1",
                "TLSv1.2");
    }

    @Test
    public void excludeCipherSuitesHaveDefaultValues() throws Exception {
        sslServerConnectorFactory.createSecureServerConnector(42, testConfig, mockServer);

        verifyDefaultExcludeCipherSuites();
        verifyDefaultIncludeProtocols();
    }

    @Test
    public void excludeCipherSuitesCanBeSetViaConfigFile() throws Exception {
        testConfig.set("oozie.https.exclude.cipher.suites","TLS_ECDHE_RSA_WITH_RC4_128_SHA,SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA,"
                + "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA");

        sslServerConnectorFactory.createSecureServerConnector(42, testConfig, mockServer);

        verify(mockSSLContextFactory).setExcludeCipherSuites(
                "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
                "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA");
        verifyDefaultIncludeProtocols();
    }
}
diff --git a/src/main/assemblies/distro-jetty.xml b/src/main/assemblies/distro-jetty.xml
new file mode 100644
index 000000000..a4bee03c8
-- /dev/null
++ b/src/main/assemblies/distro-jetty.xml
@@ -0,0 +1,155 @@
<!--
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
-->
<assembly>
    <id>distro</id>
    <formats>
        <format>dir</format>
        <format>tar.gz</format>
    </formats>
    <includeBaseDirectory>true</includeBaseDirectory>
    <baseDirectory>oozie-${project.version}</baseDirectory>
    <fileSets>
        <!-- Oozie configuration files -->
        <fileSet>
            <directory>${basedir}/../core/src/main/conf/</directory>
            <outputDirectory>/conf</outputDirectory>
            <includes>
                <include>**</include>
            </includes>
        </fileSet>
        <!-- Distro files, readme, licenses, etc -->
        <fileSet>
            <directory>${basedir}/../</directory>
            <outputDirectory>/</outputDirectory>
            <includes>
                <include>license.txt</include>
                <include>notice.txt</include>
                <include>readme.txt</include>
                <include>release-log.txt</include>
            </includes>
        </fileSet>
        <fileSet>
            <directory>${basedir}/src/main/bin</directory>
            <outputDirectory>/bin</outputDirectory>
            <includes>
                <include>*</include>
            </includes>
            <fileMode>0755</fileMode>
        </fileSet>
        <!-- Client -->
        <fileSet>
            <directory>${basedir}/../client/target/oozie-client-${project.version}-client/oozie-client-${project.version}/bin</directory>
            <outputDirectory>/bin</outputDirectory>
            <includes>
                <include>*</include>
            </includes>
            <fileMode>0755</fileMode>
        </fileSet>
        <!-- Tools -->
        <fileSet>
            <directory>${basedir}/../tools/target/oozie-tools-${project.version}-tools/oozie-tools-${project.version}/bin</directory>
            <outputDirectory>/bin</outputDirectory>
            <includes>
                <include>*</include>
            </includes>
            <fileMode>0755</fileMode>
        </fileSet>
        <fileSet>
            <directory>${basedir}/../tools/target/oozie-tools-${project.version}-tools/oozie-tools-${project.version}/libtools</directory>
            <outputDirectory>/libtools</outputDirectory>
            <includes>
                <include>*</include>
            </includes>
        </fileSet>
        <!--  Oozie Login Server Example war and jar -->
         <fileSet>
            <directory>${basedir}/../login/target</directory>
            <outputDirectory>/</outputDirectory>
            <includes>
                <include>oozie-login.war</include>
                <include>oozie-login.jar</include>
            </includes>
            <fileMode>0555</fileMode>
        </fileSet>
        <!-- Oozie Server - embedded jetty -->
        <fileSet>
                <directory>${basedir}/../server/target/</directory>
                <outputDirectory>/embedded-oozie-server</outputDirectory>
                <includes>
                    <include>oozie-server*.jar</include>
                    <include>**/jetty*.jar</include>
                    <include>**/*jsp*.jar</include>
                    <include>**/mail*.jar</include>
                    <include>**/apache*.jar</include>
                    <include>**/commons-el*.jar</include>
                    <include>**/javax.servlet-api-3.1.0.jar</include>
                    <include>**/jasper*jar</include>
                    <include>**/taglibs-*jar</include>
                    <include>**/org.eclipse.jdt.core-*jar</include>
                </includes>
        </fileSet>
        <fileSet>
            <directory>${basedir}/../webapp/target/oozie-webapp-${project.version}</directory>
            <outputDirectory>/embedded-oozie-server/webapp</outputDirectory>
            <excludes>
                <exclude>**/web.xml</exclude>
            </excludes>
        </fileSet>
    </fileSets>
    <files>
        <!-- Oozie configuration files -->
        <file>
            <source>${basedir}/../core/src/main/resources/oozie-default.xml</source>
            <outputDirectory>/conf</outputDirectory>
        </file>
        <!-- Oozie core jar -->
        <file>
            <source>${basedir}/../core/target/oozie-core-${project.version}.jar</source>
            <outputDirectory>/oozie-core</outputDirectory>
        </file>
        <!-- Oozie core test jar -->
        <file>
            <source>${basedir}/../core/target/oozie-core-${project.version}-tests.jar</source>
            <outputDirectory>/oozie-core</outputDirectory>
        </file>
        <!-- Oozie Documentation -->
        <file>
            <source>${basedir}/../docs/target/oozie-docs-${project.version}-docs.zip</source>
            <outputDirectory>/</outputDirectory>
            <destName>docs.zip</destName>
        </file>
        <!-- Oozie Client TAR.GZ  -->
        <file>
            <source>${basedir}/../client/target/oozie-client-${project.version}-client.tar.gz</source>
            <outputDirectory>/</outputDirectory>
            <destName>oozie-client-${project.version}.tar.gz</destName>
        </file>
        <!-- Oozie examples TAR.GZ  -->
        <file>
            <source>${basedir}/../examples/target/oozie-examples-${project.version}-examples.tar.gz</source>
            <outputDirectory>/</outputDirectory>
            <destName>oozie-examples.tar.gz</destName>
        </file>
        <!-- Oozie sharelib TAR.GZ  -->
        <file>
            <source>${basedir}/../sharelib/target/oozie-sharelib-${project.version}.tar.gz</source>
            <outputDirectory>/</outputDirectory>
            <fileMode>0444</fileMode>
        </file>
    </files>
</assembly>
diff --git a/src/main/assemblies/distro.xml b/src/main/assemblies/distro-tomcat.xml
similarity index 98%
rename from src/main/assemblies/distro.xml
rename to src/main/assemblies/distro-tomcat.xml
index 1ffbfd6d2..d7018a394 100644
-- a/src/main/assemblies/distro.xml
++ b/src/main/assemblies/distro-tomcat.xml
@@ -6,9 +6,9 @@
   to you under the Apache License, Version 2.0 (the
   "License"); you may not use this file except in compliance
   with the License.  You may obtain a copy of the License at
  

        http://www.apache.org/licenses/LICENSE-2.0
  

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@@ -46,7 +46,7 @@
         <fileSet>
             <directory>${basedir}/src/main/bin</directory>
             <outputDirectory>/bin</outputDirectory>
            <includes>          
            <includes>
                 <include>*</include>
             </includes>
             <fileMode>0755</fileMode>
@@ -102,14 +102,12 @@
             </includes>
             <fileMode>0555</fileMode>
         </fileSet>

     </fileSets>
     <files>
         <!-- Oozie configuration files -->
         <file>
             <source>${basedir}/../core/src/main/resources/oozie-default.xml</source>
             <outputDirectory>/conf</outputDirectory>
            <destName>oozie-default.xml.reference</destName>
         </file>
         <!-- Oozie core jar -->
         <file>
diff --git a/webapp/src/main/webapp/403.html b/webapp/src/main/webapp/403.html
new file mode 100644
index 000000000..f3183d91b
-- /dev/null
++ b/webapp/src/main/webapp/403.html
@@ -0,0 +1,31 @@
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<!--
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
-->

<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Error 403 Not Found</title>
</head>
<body>
    <h2>HTTP ERROR 403</h2>
    <p>Problem accessing page. Reason:</p>
    <pre>    Forbidden</pre>
    <p></p>
</body>
</html>
diff --git a/webapp/src/main/webapp/404.html b/webapp/src/main/webapp/404.html
new file mode 100644
index 000000000..a953df285
-- /dev/null
++ b/webapp/src/main/webapp/404.html
@@ -0,0 +1,31 @@
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<!--
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
-->

<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Error 404 Not Found</title>
</head>
<body>
    <h2>HTTP ERROR 404</h2>
    <p>Problem accessing page Reason:</p>
    <pre>    Not Found</pre>
    <p></p>
</body>
</html>
- 
2.19.1.windows.1

