From 659a33e8e094f68e46c824374d25da67b4c30421 Mon Sep 17 00:00:00 2001
From: Mike Walch <mwalch@apache.org>
Date: Fri, 3 Mar 2017 11:04:41 -0500
Subject: [PATCH] ACCUMULO-4596 Stopped using ACCUMULO_HOME for native libs

* Using system property 'accumulo.native.lib.path' rather than
  environment variable to configure native libraries.
* Removed unnnecessary system property for xml parsing
* Tablet servers will die if native maps are enabled but not set up.
* Remove broken rsync from 'accumulo-util load-jars-hdfs' which
  fixes ACCUMULO-4598.
* Stopped using ACCUMULO_HOME & ACCUMULO_CONF_DIR in accumulo-env.sh
* Added command to accumulo-env.sh to build native library by default
--
 assemble/bin/accumulo                         | 34 ++++-------
 assemble/bin/accumulo-service                 |  8 ++-
 assemble/bin/accumulo-util                    | 28 ++++-----
 assemble/conf/templates/accumulo-env.sh       | 45 ++++++++------
 .../apache/accumulo/tserver/NativeMap.java    | 60 +++++++++----------
 .../apache/accumulo/test/InMemoryMapIT.java   |  5 +-
 .../accumulo/test/functional/NativeMapIT.java |  4 +-
 7 files changed, 88 insertions(+), 96 deletions(-)

diff --git a/assemble/bin/accumulo b/assemble/bin/accumulo
index 69b95dfdc..b10d93164 100755
-- a/assemble/bin/accumulo
++ b/assemble/bin/accumulo
@@ -36,16 +36,12 @@ function main() {
      SOURCE="$(readlink "${SOURCE}")"
      [[ "${SOURCE}" != /* ]] && SOURCE="${bin}/${SOURCE}" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
   done
  bin="$( cd -P "$( dirname "${SOURCE}" )" && pwd )"
  basedir=$( cd -P "${bin}"/.. && pwd )
  conf="${basedir}/conf"
  lib="${basedir}/lib"

  # Might be needed by accumulo-env.sh, accumulo-site.xml, and other Accumulo configuration
  export ACCUMULO_HOME="$basedir"
  export ACCUMULO_CONF_DIR="$conf"

  export ACCUMULO_CMD="$1"
  # Set up variables needed by accumulo-env.sh
  export bin="$( cd -P "$( dirname "${SOURCE}" )" && pwd )"
  export basedir=$( cd -P "${bin}"/.. && pwd )
  export conf="${basedir}/conf"
  export lib="${basedir}/lib"
  export cmd="$1"
 
   if [[ -z $conf || ! -d $conf ]]; then
     echo "$conf is not a valid directory.  Please make sure it exists"
@@ -58,6 +54,10 @@ function main() {
   fi
   source "$conf/accumulo-env.sh"
 
  # Might be needed by accumulo-env.sh, accumulo-site.xml, and other Accumulo configuration
  export ACCUMULO_HOME="$basedir"
  export ACCUMULO_CONF_DIR="$conf"

   # Verify setting in accumulo-env.sh
 
   : "${JAVA_OPTS:?"variable is not set in accumulo-env.sh"}"
@@ -69,23 +69,15 @@ function main() {
   verify_env_dir "ZOOKEEPER_HOME" "${ZOOKEEPER_HOME}"
   : "${MALLOC_ARENA_MAX:?"variable is not set in accumulo-env.sh"}"
 
  export HADOOP_HOME=$HADOOP_PREFIX
  export HADOOP_HOME_WARN_SUPPRESS=true

  # This is default for hadoop 2.x; for another distribution, specify (DY)LD_LIBRARY_PATH explicitly above
  if [ -e "${HADOOP_PREFIX}/lib/native/libhadoop.so" ]; then
    export LD_LIBRARY_PATH="${HADOOP_PREFIX}/lib/native:${LD_LIBRARY_PATH}"     # For Linux
    export DYLD_LIBRARY_PATH="${HADOOP_PREFIX}/lib/native:${DYLD_LIBRARY_PATH}" # For Mac
  fi
   # ACCUMULO_XTRAJARS is where all of the commandline -add items go into for reading by accumulo.
   # It also holds the JAR run with the jar command and, if possible, any items in the JAR manifest's Class-Path.
  if [[ "$ACCUMULO_CMD" = "-add" ]]; then
  if [[ "$cmd" = "-add" ]]; then
     export ACCUMULO_XTRAJARS="$2"
     shift 2
   else
     export ACCUMULO_XTRAJARS=""
   fi
  if [[ "$ACCUMULO_CMD" = "jar" && -f "$2" ]]; then
  if [[ "$cmd" = "jar" && -f "$2" ]]; then
     if [[ $2 =~ ^/ ]]; then
       jardir="$(dirname "$2")"
       jarfile="$2"
@@ -122,7 +114,7 @@ function main() {
   
   CLASSPATH="${lib}/accumulo-start.jar:${conf}:${lib}/slf4j-api.jar:${lib}/slf4j-log4j12.jar:${lib}/log4j.jar:${CLASSPATH}"
 
  exec "${JAVA[@]}" "-Dapp=$ACCUMULO_CMD" \
  exec "${JAVA[@]}" "-Dapp=$cmd" \
      "${JAVA_OPTS[@]}" \
      -classpath "${CLASSPATH}" \
      -Dhadoop.home.dir="${HADOOP_PREFIX}" \
diff --git a/assemble/bin/accumulo-service b/assemble/bin/accumulo-service
index d1e12fae0..a08a0c02c 100755
-- a/assemble/bin/accumulo-service
++ b/assemble/bin/accumulo-service
@@ -114,9 +114,11 @@ function main() {
      SOURCE="$(readlink "${SOURCE}")"
      [[ "${SOURCE}" != /* ]] && SOURCE="${bin}/${SOURCE}"
   done
  bin="$( cd -P "$( dirname "${SOURCE}" )" && pwd )"
  basedir=$( cd -P "${bin}"/.. && pwd )
  conf="${basedir}/conf"
  # Set up variables needed by accumulo-env.sh
  export bin="$( cd -P "$( dirname "${SOURCE}" )" && pwd )"
  export basedir=$( cd -P "${bin}"/.. && pwd )
  export conf="${basedir}/conf"
  export lib="${basedir}/lib"
 
   if [ -f "${conf}/accumulo-env.sh" ]; then
     source "${conf}/accumulo-env.sh"
diff --git a/assemble/bin/accumulo-util b/assemble/bin/accumulo-util
index 267ccb608..e8abc4fee 100755
-- a/assemble/bin/accumulo-util
++ b/assemble/bin/accumulo-util
@@ -411,12 +411,16 @@ function create_config() {
 }
 
 function build_native() {
  native_tarball="$basedir/lib/accumulo-native.tar.gz"
   final_native_target="$basedir/lib/native"
  if [ -f "$final_native_target/libaccumulo.so" -o -f "$final_native_target/libaccumulo.dylib" ]; then
    echo "Accumulo native library already exists in $final_native_target"
    exit 0
  fi
 
  native_tarball="$basedir/lib/accumulo-native.tar.gz"
   if [[ ! -f $native_tarball ]]; then
      echo "Could not find native code artifact: ${native_tarball}";
      exit 1
    echo "Could not find native code artifact: ${native_tarball}" 1>&2
    exit 1
   fi
 
   # Make the destination for the native library
@@ -512,13 +516,8 @@ function gen_monitor_cert() {
 }
 
 function load_jars_hdfs() {
  export ACCUMULO_HOME="$basedir"

  if [ -f "${conf}/accumulo-env.sh" ]; then
    source "$conf/accumulo-env.sh"
  fi
   if [ -z "$HADOOP_PREFIX" ]; then
     echo "HADOOP_PREFIX is not set.  Please make sure it's set globally or in $conf/accumulo-env.sh"
     echo "HADOOP_PREFIX is not set!"
      exit 1
   fi
 
@@ -569,22 +568,15 @@ function load_jars_hdfs() {
   "$HADOOP_PREFIX/bin/hadoop" fs -rm "$SYSTEM_CONTEXT_HDFS_DIR/accumulo-start.jar"  > /dev/null
   "$HADOOP_PREFIX/bin/hadoop" fs -copyToLocal "$SYSTEM_CONTEXT_HDFS_DIR/slf4j*.jar" "$lib/."  > /dev/null
   "$HADOOP_PREFIX/bin/hadoop" fs -rm "$SYSTEM_CONTEXT_HDFS_DIR/slf4j*.jar"  > /dev/null
  for f in $(grep -v '^#' "${conf}/tservers")
  do
    rsync -ra --delete "$ACCUMULO_HOME" "$(dirname "$ACCUMULO_HOME")"
  done
 }
 
 function hadoop_jar() {
  if [ -f "${conf}/accumulo-env.sh" ]; then
    source "$conf/accumulo-env.sh"
  fi
   if [ -z "$HADOOP_PREFIX" ]; then
     echo "HADOOP_PREFIX is not set.  Please make sure it's set globally or in $conf/accumulo-env.sh"
     echo "HADOOP_PREFIX must be set!"
      exit 1
   fi
   if [ -z "$ZOOKEEPER_HOME" ]; then
     echo "ZOOKEEPER_HOME is not set.  Please make sure it's set globally or in $conf/accumulo-env.sh"
     echo "ZOOKEEPER_HOME must be set!"
      exit 1
   fi
 
diff --git a/assemble/conf/templates/accumulo-env.sh b/assemble/conf/templates/accumulo-env.sh
index fafc5f6dd..a58d8164c 100644
-- a/assemble/conf/templates/accumulo-env.sh
++ b/assemble/conf/templates/accumulo-env.sh
@@ -17,16 +17,18 @@
 
 ## Before accumulo-env.sh is loaded, these environment variables are set and can be used in this file:
 
# ACCUMULO_CMD - Command that is being called such as tserver, master, etc.
# ACCUMULO_HOME - Root directory of Accumulo installation
# ACCUMULO_CONF_DIR - Directory containing Accumulo configuration
# cmd - Command that is being called such as tserver, master, etc.
# basedir - Root of Accumulo installation
# bin - Directory containing Accumulo scripts
# conf - Directory containing Accumulo configuration
# lib - Directory containing Accumulo libraries
 
 ############################
 # Variables that must be set
 ############################
 
 ## Accumulo logs directory. Referenced by logger config.
export ACCUMULO_LOG_DIR="${ACCUMULO_LOG_DIR:-$ACCUMULO_HOME/logs}"
export ACCUMULO_LOG_DIR="${ACCUMULO_LOG_DIR:-${basedir}/logs}"
 ## Hadoop installation
 export HADOOP_PREFIX="${HADOOP_PREFIX:-/path/to/hadoop}"
 ## Hadoop configuration
@@ -39,12 +41,20 @@ export ZOOKEEPER_HOME="${ZOOKEEPER_HOME:-/path/to/zookeeper}"
 ##################################################################
 
 ## JVM options set for all processes. Extra options can be passed in by setting ACCUMULO_JAVA_OPTS to an array of options.
JAVA_OPTS=("${ACCUMULO_JAVA_OPTS[@]}" '-XX:+UseConcMarkSweepGC' '-XX:CMSInitiatingOccupancyFraction=75' '-XX:+CMSClassUnloadingEnabled'
'-XX:OnOutOfMemoryError=kill -9 %p' '-XX:-OmitStackTraceInFastThrow' '-Djava.net.preferIPv4Stack=true' 
'-Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl')
JAVA_OPTS=("${ACCUMULO_JAVA_OPTS[@]}" 
  '-XX:+UseConcMarkSweepGC'
  '-XX:CMSInitiatingOccupancyFraction=75'
  '-XX:+CMSClassUnloadingEnabled'
  '-XX:OnOutOfMemoryError=kill -9 %p'
  '-XX:-OmitStackTraceInFastThrow'
  '-Djava.net.preferIPv4Stack=true'
  "-Daccumulo.native.lib.path=${lib}/native")

## Make sure Accumulo native libraries are built since they are enabled by default
${bin}/accumulo-util build-native &> /dev/null
 
 ## JVM options set for individual applications
case "$ACCUMULO_CMD" in
case "$cmd" in
 master)  JAVA_OPTS=("${JAVA_OPTS[@]}" ${masterHigh_masterLow}) ;;
 monitor) JAVA_OPTS=("${JAVA_OPTS[@]}" ${monitorHigh_monitorLow}) ;;
 gc)      JAVA_OPTS=("${JAVA_OPTS[@]}" ${gcHigh_gcLow}) ;;
@@ -56,13 +66,13 @@ esac
 ## JVM options set for logging.  Review logj4 properties files to see how they are used.
 JAVA_OPTS=("${JAVA_OPTS[@]}" 
   "-Daccumulo.log.dir=${ACCUMULO_LOG_DIR}"
  "-Daccumulo.service.id=${ACCUMULO_CMD}${ACCUMULO_SERVICE_INSTANCE}_$(hostname)"
  "-Daccumulo.service.id=${cmd}${ACCUMULO_SERVICE_INSTANCE}_$(hostname)"
   "-Daccumulo.audit.log=$(hostname).audit")
 
case "$ACCUMULO_CMD" in
monitor)                    JAVA_OPTS=("${JAVA_OPTS[@]}" "-Dlog4j.configuration=file:${ACCUMULO_CONF_DIR}/log4j-monitor.properties") ;;
gc|master|tserver|tracer)   JAVA_OPTS=("${JAVA_OPTS[@]}" "-Dlog4j.configuration=file:${ACCUMULO_CONF_DIR}/log4j-service.properties") ;;
*)                          JAVA_OPTS=("${JAVA_OPTS[@]}" "-Dlog4j.configuration=file:${ACCUMULO_CONF_DIR}/log4j.properties") ;;
case "$cmd" in
monitor)                    JAVA_OPTS=("${JAVA_OPTS[@]}" "-Dlog4j.configuration=file:${conf}/log4j-monitor.properties") ;;
gc|master|tserver|tracer)   JAVA_OPTS=("${JAVA_OPTS[@]}" "-Dlog4j.configuration=file:${conf}/log4j-service.properties") ;;
*)                          JAVA_OPTS=("${JAVA_OPTS[@]}" "-Dlog4j.configuration=file:${conf}/log4j.properties") ;;
 esac
 
 export JAVA_OPTS
@@ -72,6 +82,11 @@ export JAVA_OPTS
 ############################
 
 export MALLOC_ARENA_MAX=${MALLOC_ARENA_MAX:-1}
## Add Hadoop native libraries to shared library paths given operating system
case "$(uname)" in 
Darwin) export DYLD_LIBRARY_PATH="${HADOOP_PREFIX}/lib/native:${DYLD_LIBRARY_PATH}" ;; 
*)      export LD_LIBRARY_PATH="${HADOOP_PREFIX}/lib/native:${LD_LIBRARY_PATH}" ;;
esac
 
 ###############################################
 # Variables that are optional. Uncomment to set
@@ -79,7 +94,3 @@ export MALLOC_ARENA_MAX=${MALLOC_ARENA_MAX:-1}
 
 ## Specifies command that will be placed before calls to Java in accumulo script
 # export ACCUMULO_JAVA_PREFIX=""
## Optionally look for hadoop and accumulo native libraries for your platform in additional
## directories. (Use DYLD_LIBRARY_PATH on Mac OS X.) May not be necessary for Hadoop 2.x or
## using an RPM that installs to the correct system library directory.
# export LD_LIBRARY_PATH=${HADOOP_PREFIX}/lib/native/${PLATFORM}:${LD_LIBRARY_PATH}
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/NativeMap.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/NativeMap.java
index 4e3bf4d4b..a447937de 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/NativeMap.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/NativeMap.java
@@ -20,7 +20,6 @@ import java.io.File;
 import java.io.IOException;
 import java.util.AbstractMap.SimpleImmutableEntry;
 import java.util.ArrayList;
import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.ConcurrentModificationException;
@@ -36,6 +35,7 @@ import java.util.concurrent.locks.ReadWriteLock;
 import java.util.concurrent.locks.ReentrantReadWriteLock;
 
 import org.apache.accumulo.core.client.SampleNotPresentException;
import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.data.ByteSequence;
 import org.apache.accumulo.core.data.ColumnUpdate;
 import org.apache.accumulo.core.data.Key;
@@ -60,7 +60,6 @@ import com.google.common.annotations.VisibleForTesting;
  * would be a mistake for long lived NativeMaps. Long lived objects are not garbage collected quickly, therefore a process could easily use too much memory.
  *
  */

 public class NativeMap implements Iterable<Map.Entry<Key,Value>> {
 
   private static final Logger log = LoggerFactory.getLogger(NativeMap.class);
@@ -68,31 +67,37 @@ public class NativeMap implements Iterable<Map.Entry<Key,Value>> {
 
   // Load native library
   static {
    // Check standard directories
    List<File> directories = new ArrayList<>(Arrays.asList(new File[] {new File("/usr/lib64"), new File("/usr/lib")}));
    // Check in ACCUMULO_HOME location, too
    String accumuloHome = System.getenv("ACCUMULO_HOME");
    if (accumuloHome != null) {
      directories.add(new File(accumuloHome + "/lib/native"));
      directories.add(new File(accumuloHome + "/lib/native/map")); // old location, just in case somebody puts it here
    // Check in directories set by JVM system property
    List<File> directories = new ArrayList<>();
    String accumuloNativeLibDirs = System.getProperty("accumulo.native.lib.path");
    if (accumuloNativeLibDirs != null) {
      for (String libDir : accumuloNativeLibDirs.split(":")) {
        directories.add(new File(libDir));
      }
     }
     // Attempt to load from these directories, using standard names
     loadNativeLib(directories);
 
     // Check LD_LIBRARY_PATH (DYLD_LIBRARY_PATH on Mac)
     if (!isLoaded()) {
      log.error("Tried and failed to load Accumulo native library from {}", accumuloNativeLibDirs);
       String ldLibraryPath = System.getProperty("java.library.path");
      String errMsg = "Tried and failed to load native map library from " + ldLibraryPath;
       try {
         System.loadLibrary("accumulo");
         loadedNativeLibraries.set(true);
         log.info("Loaded native map shared library from " + ldLibraryPath);
      } catch (Exception e) {
        log.error(errMsg, e);
      } catch (UnsatisfiedLinkError e) {
        log.error(errMsg, e);
      } catch (Exception | UnsatisfiedLinkError e) {
        log.error("Tried and failed to load Accumulo native library from {}", ldLibraryPath, e);
       }
     }

    // Exit if native libraries could not be loaded
    if (!isLoaded()) {
      log.error("FATAL! Accumulo native libraries were requested but could not be be loaded. Either set '{}' to false in accumulo-site.xml "
          + " or make sure native libraries are created in directories set by the JVM system property 'accumulo.native.lib.path' in accumulo-env.sh!",
          Property.TSERV_NATIVEMAP_ENABLED);
      System.exit(1);
    }
   }
 
   /**
@@ -151,16 +156,13 @@ public class NativeMap implements Iterable<Map.Entry<Key,Value>> {
   private static boolean loadNativeLib(File libFile) {
     log.debug("Trying to load native map library " + libFile);
     if (libFile.exists() && libFile.isFile()) {
      String errMsg = "Tried and failed to load native map library " + libFile;
       try {
         System.load(libFile.getAbsolutePath());
         loadedNativeLibraries.set(true);
         log.info("Loaded native map shared library " + libFile);
         return true;
      } catch (Exception e) {
        log.error(errMsg, e);
      } catch (UnsatisfiedLinkError e) {
        log.error(errMsg, e);
      } catch (Exception | UnsatisfiedLinkError e) {
        log.error("Tried and failed to load native map library " + libFile, e);
       }
     } else {
       log.debug("Native map library " + libFile + " not found or is not a file.");
@@ -174,7 +176,7 @@ public class NativeMap implements Iterable<Map.Entry<Key,Value>> {
   private final Lock rlock;
   private final Lock wlock;
 
  int modCount = 0;
  private int modCount = 0;
 
   private static native long createNM();
 
@@ -201,18 +203,12 @@ public class NativeMap implements Iterable<Map.Entry<Key,Value>> {
     if (!init) {
       allocatedNativeMaps = new HashSet<>();
 
      Runnable r = new Runnable() {
        @Override
        public void run() {
          if (allocatedNativeMaps.size() > 0) {
            log.info("There are " + allocatedNativeMaps.size() + " allocated native maps");
          }

          log.debug(totalAllocations + " native maps were allocated");
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        if (allocatedNativeMaps.size() > 0) {
          log.info("There are " + allocatedNativeMaps.size() + " allocated native maps");
         }
      };

      Runtime.getRuntime().addShutdownHook(new Thread(r));
        log.debug(totalAllocations + " native maps were allocated");
      }));
 
       init = true;
     }
@@ -441,7 +437,7 @@ public class NativeMap implements Iterable<Map.Entry<Key,Value>> {
       byte cf[] = new byte[fieldsLens[1]];
       byte cq[] = new byte[fieldsLens[2]];
       byte cv[] = new byte[fieldsLens[3]];
      boolean deleted = fieldsLens[4] == 0 ? false : true;
      boolean deleted = fieldsLens[4] != 0;
       byte val[] = new byte[fieldsLens[5]];
 
       nmiGetData(nmiPointer, row, cf, cq, cv, val);
diff --git a/test/src/main/java/org/apache/accumulo/test/InMemoryMapIT.java b/test/src/main/java/org/apache/accumulo/test/InMemoryMapIT.java
index fc8945b56..797f106d4 100644
-- a/test/src/main/java/org/apache/accumulo/test/InMemoryMapIT.java
++ b/test/src/main/java/org/apache/accumulo/test/InMemoryMapIT.java
@@ -83,16 +83,13 @@ public class InMemoryMapIT {
   @BeforeClass
   public static void ensureNativeLibrary() throws FileNotFoundException {
     File nativeMapLocation = NativeMapIT.nativeMapLocation();
    log.debug("Native map location " + nativeMapLocation);
    NativeMap.loadNativeLib(Collections.singletonList(nativeMapLocation));
    System.setProperty("accumulo.native.lib.path", nativeMapLocation.getAbsolutePath());
     if (!NativeMap.isLoaded()) {
       fail("Missing the native library from " + nativeMapLocation.getAbsolutePath() + "\nYou need to build the libaccumulo binary first. "
           + "\nTry running 'mvn clean install -Dit.test=InMemoryMapIT -Dtest=foo -DfailIfNoTests=false -Dfindbugs.skip -Dcheckstyle.skip'");
       // afterwards, you can run the following
       // mvn clean verify -Dit.test=InMemoryMapIT -Dtest=foo -DfailIfNoTests=false -Dfindbugs.skip -Dcheckstyle.skip -pl :accumulo-test
     }
    log.debug("Native map loaded");

   }
 
   @Test
diff --git a/test/src/main/java/org/apache/accumulo/test/functional/NativeMapIT.java b/test/src/main/java/org/apache/accumulo/test/functional/NativeMapIT.java
index fc9e8d14c..12605139b 100644
-- a/test/src/main/java/org/apache/accumulo/test/functional/NativeMapIT.java
++ b/test/src/main/java/org/apache/accumulo/test/functional/NativeMapIT.java
@@ -42,6 +42,7 @@ import org.apache.accumulo.core.util.Pair;
 import org.apache.accumulo.test.categories.SunnyDayTests;
 import org.apache.accumulo.tserver.NativeMap;
 import org.apache.hadoop.io.Text;
import org.junit.Assert;
 import org.junit.BeforeClass;
 import org.junit.Test;
 import org.junit.experimental.categories.Category;
@@ -74,7 +75,8 @@ public class NativeMapIT {
 
   @BeforeClass
   public static void setUp() {
    NativeMap.loadNativeLib(Collections.singletonList(nativeMapLocation()));
    System.setProperty("accumulo.native.lib.path", nativeMapLocation().getAbsolutePath());
    Assert.assertTrue(NativeMap.isLoaded());
   }
 
   private void verifyIterator(int start, int end, int valueOffset, Iterator<Entry<Key,Value>> iter) {
- 
2.19.1.windows.1

