From 95f9a515aac3c19e14a39539f490203f5867dcc5 Mon Sep 17 00:00:00 2001
From: Andrew Wang <wang@apache.org>
Date: Mon, 5 Aug 2013 23:28:14 +0000
Subject: [PATCH] HADOOP-9817. FileSystem#globStatus and FileContext#globStatus
 need to work with symlinks. (Colin Patrick McCabe via Andrew Wang)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1510807 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |   3 +
 .../org/apache/hadoop/fs/FileContext.java     | 153 +---------
 .../java/org/apache/hadoop/fs/FileSystem.java | 123 +-------
 .../java/org/apache/hadoop/fs/Globber.java    | 215 ++++++++++++++
 .../java/org/apache/hadoop/fs/FSWrapper.java  |   3 +
 .../hadoop/fs/FileContextTestWrapper.java     |   6 +
 .../hadoop/fs/FileSystemTestWrapper.java      |   6 +
 .../org/apache/hadoop/fs/TestFileUtil.java    |   2 +
 .../java/org/apache/hadoop/fs/TestPath.java   |  42 ++-
 .../org/apache/hadoop/fs/TestGlobPaths.java   | 265 ++++++++++++++++++
 10 files changed, 539 insertions(+), 279 deletions(-)
 create mode 100644 hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Globber.java

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 7864c35f5c0..bafef60534b 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -314,6 +314,9 @@ Release 2.3.0 - UNRELEASED
     HADOOP-9761.  ViewFileSystem#rename fails when using DistributedFileSystem.
     (Andrew Wang via Colin Patrick McCabe)
 
    HADOOP-9817. FileSystem#globStatus and FileContext#globStatus need to work
    with symlinks. (Colin Patrick McCabe via Andrew Wang)

 Release 2.1.1-beta - UNRELEASED
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileContext.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileContext.java
index 7564e581839..83a0004f498 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileContext.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileContext.java
@@ -258,7 +258,7 @@ private FileContext(final AbstractFileSystem defFs,
    * Hence this method is not called makeAbsolute() and 
    * has been deliberately declared private.
    */
  private Path fixRelativePart(Path p) {
  Path fixRelativePart(Path p) {
     if (p.isUriPathAbsolute()) {
       return p;
     } else {
@@ -1905,7 +1905,7 @@ public LocatedFileStatus next() throws IOException {
     public FileStatus[] globStatus(Path pathPattern)
         throws AccessControlException, UnsupportedFileSystemException,
         IOException {
      return globStatus(pathPattern, DEFAULT_FILTER);
      return new Globber(FileContext.this, pathPattern, DEFAULT_FILTER).glob();
     }
     
     /**
@@ -1934,154 +1934,7 @@ public LocatedFileStatus next() throws IOException {
     public FileStatus[] globStatus(final Path pathPattern,
         final PathFilter filter) throws AccessControlException,
         UnsupportedFileSystemException, IOException {
      URI uri = getFSofPath(fixRelativePart(pathPattern)).getUri();

      String filename = pathPattern.toUri().getPath();

      List<String> filePatterns = GlobExpander.expand(filename);
      if (filePatterns.size() == 1) {
        Path absPathPattern = fixRelativePart(pathPattern);
        return globStatusInternal(uri, new Path(absPathPattern.toUri()
            .getPath()), filter);
      } else {
        List<FileStatus> results = new ArrayList<FileStatus>();
        for (String iFilePattern : filePatterns) {
          Path iAbsFilePattern = fixRelativePart(new Path(iFilePattern));
          FileStatus[] files = globStatusInternal(uri, iAbsFilePattern, filter);
          for (FileStatus file : files) {
            results.add(file);
          }
        }
        return results.toArray(new FileStatus[results.size()]);
      }
    }

    /**
     * 
     * @param uri for all the inPathPattern
     * @param inPathPattern - without the scheme & authority (take from uri)
     * @param filter
     *
     * @return an array of FileStatus objects
     *
     * @throws AccessControlException If access is denied
     * @throws IOException If an I/O error occurred
     */
    private FileStatus[] globStatusInternal(final URI uri,
        final Path inPathPattern, final PathFilter filter)
        throws AccessControlException, IOException
      {
      Path[] parents = new Path[1];
      int level = 0;
      
      assert(inPathPattern.toUri().getScheme() == null &&
          inPathPattern.toUri().getAuthority() == null && 
          inPathPattern.isUriPathAbsolute());

      
      String filename = inPathPattern.toUri().getPath();
      
      // path has only zero component
      if (filename.isEmpty() || Path.SEPARATOR.equals(filename)) {
        Path p = inPathPattern.makeQualified(uri, null);
        return getFileStatus(new Path[]{p});
      }

      // path has at least one component
      String[] components = filename.split(Path.SEPARATOR);
      
      // Path is absolute, first component is "/" hence first component
      // is the uri root
      parents[0] = new Path(new Path(uri), new Path("/"));
      level = 1;

      // glob the paths that match the parent path, ie. [0, components.length-1]
      boolean[] hasGlob = new boolean[]{false};
      Path[] relParentPaths = 
        globPathsLevel(parents, components, level, hasGlob);
      FileStatus[] results;
      
      if (relParentPaths == null || relParentPaths.length == 0) {
        results = null;
      } else {
        // fix the pathes to be abs
        Path[] parentPaths = new Path [relParentPaths.length]; 
        for(int i=0; i<relParentPaths.length; i++) {
          parentPaths[i] = relParentPaths[i].makeQualified(uri, null);
        }
        
        // Now work on the last component of the path
        GlobFilter fp = 
                    new GlobFilter(components[components.length - 1], filter);
        if (fp.hasPattern()) { // last component has a pattern
          // list parent directories and then glob the results
          try {
            results = listStatus(parentPaths, fp);
          } catch (FileNotFoundException e) {
            results = null;
          }
          hasGlob[0] = true;
        } else { // last component does not have a pattern
          // get all the path names
          ArrayList<Path> filteredPaths = 
                                      new ArrayList<Path>(parentPaths.length);
          for (int i = 0; i < parentPaths.length; i++) {
            parentPaths[i] = new Path(parentPaths[i],
              components[components.length - 1]);
            if (fp.accept(parentPaths[i])) {
              filteredPaths.add(parentPaths[i]);
            }
          }
          // get all their statuses
          results = getFileStatus(
              filteredPaths.toArray(new Path[filteredPaths.size()]));
        }
      }

      // Decide if the pathPattern contains a glob or not
      if (results == null) {
        if (hasGlob[0]) {
          results = new FileStatus[0];
        }
      } else {
        if (results.length == 0) {
          if (!hasGlob[0]) {
            results = null;
          }
        } else {
          Arrays.sort(results);
        }
      }
      return results;
    }

    /*
     * For a path of N components, return a list of paths that match the
     * components [<code>level</code>, <code>N-1</code>].
     */
    private Path[] globPathsLevel(Path[] parents, String[] filePattern,
        int level, boolean[] hasGlob) throws AccessControlException,
        FileNotFoundException, IOException {
      if (level == filePattern.length - 1) {
        return parents;
      }
      if (parents == null || parents.length == 0) {
        return null;
      }
      GlobFilter fp = new GlobFilter(filePattern[level]);
      if (fp.hasPattern()) {
        try {
          parents = FileUtil.stat2Paths(listStatus(parents, fp));
        } catch (FileNotFoundException e) {
          parents = null;
        }
        hasGlob[0] = true;
      } else {
        for (int i = 0; i < parents.length; i++) {
          parents[i] = new Path(parents[i], filePattern[level]);
        }
      }
      return globPathsLevel(parents, filePattern, level + 1, hasGlob);
      return new Globber(FileContext.this, pathPattern, filter).glob();
     }
 
     /**
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileSystem.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileSystem.java
index 8f8bc8752fb..7d9e931b34f 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileSystem.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileSystem.java
@@ -1619,7 +1619,7 @@ private void listStatus(ArrayList<FileStatus> results, Path f,
    * @throws IOException
    */
   public FileStatus[] globStatus(Path pathPattern) throws IOException {
    return globStatus(pathPattern, DEFAULT_FILTER);
    return new Globber(this, pathPattern, DEFAULT_FILTER).glob();
   }
   
   /**
@@ -1637,126 +1637,7 @@ private void listStatus(ArrayList<FileStatus> results, Path f,
    */
   public FileStatus[] globStatus(Path pathPattern, PathFilter filter)
       throws IOException {
    String filename = pathPattern.toUri().getPath();
    List<FileStatus> allMatches = null;
    
    List<String> filePatterns = GlobExpander.expand(filename);
    for (String filePattern : filePatterns) {
      Path path = new Path(filePattern.isEmpty() ? Path.CUR_DIR : filePattern);
      List<FileStatus> matches = globStatusInternal(path, filter);
      if (matches != null) {
        if (allMatches == null) {
          allMatches = matches;
        } else {
          allMatches.addAll(matches);
        }
      }
    }
    
    FileStatus[] results = null;
    if (allMatches != null) {
      results = allMatches.toArray(new FileStatus[allMatches.size()]);
    } else if (filePatterns.size() > 1) {
      // no matches with multiple expansions is a non-matching glob 
      results = new FileStatus[0];
    }
    return results;
  }

  // sort gripes because FileStatus Comparable isn't parameterized...
  @SuppressWarnings("unchecked") 
  private List<FileStatus> globStatusInternal(Path pathPattern,
      PathFilter filter) throws IOException {
    boolean patternHasGlob = false;       // pathPattern has any globs
    List<FileStatus> matches = new ArrayList<FileStatus>();

    // determine starting point
    int level = 0;
    String baseDir = Path.CUR_DIR;
    if (pathPattern.isAbsolute()) {
      level = 1; // need to skip empty item at beginning of split list
      baseDir = Path.SEPARATOR;
    }
    
    // parse components and determine if it's a glob
    String[] components = null;
    GlobFilter[] filters = null;
    String filename = pathPattern.toUri().getPath();
    if (!filename.isEmpty() && !Path.SEPARATOR.equals(filename)) {
      components = filename.split(Path.SEPARATOR);
      filters = new GlobFilter[components.length];
      for (int i=level; i < components.length; i++) {
        filters[i] = new GlobFilter(components[i]);
        patternHasGlob |= filters[i].hasPattern();
      }
      if (!patternHasGlob) {
        baseDir = unquotePathComponent(filename);
        components = null; // short through to filter check
      }
    }
    
    // seed the parent directory path, return if it doesn't exist
    try {
      matches.add(getFileStatus(new Path(baseDir)));
    } catch (FileNotFoundException e) {
      return patternHasGlob ? matches : null;
    }
    
    // skip if there are no components other than the basedir
    if (components != null) {
      // iterate through each path component
      for (int i=level; (i < components.length) && !matches.isEmpty(); i++) {
        List<FileStatus> children = new ArrayList<FileStatus>();
        for (FileStatus match : matches) {
          // don't look for children in a file matched by a glob
          if (!match.isDirectory()) {
            continue;
          }
          try {
            if (filters[i].hasPattern()) {
              // get all children matching the filter
              FileStatus[] statuses = listStatus(match.getPath(), filters[i]);
              children.addAll(Arrays.asList(statuses));
            } else {
              // the component does not have a pattern
              String component = unquotePathComponent(components[i]);
              Path child = new Path(match.getPath(), component);
              children.add(getFileStatus(child));
            }
          } catch (FileNotFoundException e) {
            // don't care
          }
        }
        matches = children;
      }
    }
    // remove anything that didn't match the filter
    if (!matches.isEmpty()) {
      Iterator<FileStatus> iter = matches.iterator();
      while (iter.hasNext()) {
        if (!filter.accept(iter.next().getPath())) {
          iter.remove();
        }
      }
    }
    // no final paths, if there were any globs return empty list
    if (matches.isEmpty()) {
      return patternHasGlob ? matches : null;
    }
    Collections.sort(matches);
    return matches;
  }

  /**
   * The glob filter builds a regexp per path component.  If the component
   * does not contain a shell metachar, then it falls back to appending the
   * raw string to the list of built up paths.  This raw path needs to have
   * the quoting removed.  Ie. convert all occurances of "\X" to "X"
   * @param name of the path component
   * @return the unquoted path component
   */
  private String unquotePathComponent(String name) {
    return name.replaceAll("\\\\(.)", "$1");
    return new Globber(this, pathPattern, filter).glob();
   }
   
   /**
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Globber.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Globber.java
new file mode 100644
index 00000000000..ad28478aeb8
-- /dev/null
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Globber.java
@@ -0,0 +1,215 @@
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
package org.apache.hadoop.fs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;

@InterfaceAudience.Private
@InterfaceStability.Unstable
class Globber {
  public static final Log LOG = LogFactory.getLog(Globber.class.getName());

  private final FileSystem fs;
  private final FileContext fc;
  private final Path pathPattern;
  private final PathFilter filter;
  
  public Globber(FileSystem fs, Path pathPattern, PathFilter filter) {
    this.fs = fs;
    this.fc = null;
    this.pathPattern = pathPattern;
    this.filter = filter;
  }

  public Globber(FileContext fc, Path pathPattern, PathFilter filter) {
    this.fs = null;
    this.fc = fc;
    this.pathPattern = pathPattern;
    this.filter = filter;
  }

  private FileStatus getFileStatus(Path path) {
    try {
      if (fs != null) {
        return fs.getFileStatus(path);
      } else {
        return fc.getFileStatus(path);
      }
    } catch (IOException e) {
      return null;
    }
  }

  private FileStatus[] listStatus(Path path) {
    try {
      if (fs != null) {
        return fs.listStatus(path);
      } else {
        return fc.util().listStatus(path);
      }
    } catch (IOException e) {
      return new FileStatus[0];
    }
  }

  private Path fixRelativePart(Path path) {
    if (fs != null) {
      return fs.fixRelativePart(path);
    } else {
      return fc.fixRelativePart(path);
    }
  }

  /**
   * Translate an absolute path into a list of path components.
   * We merge double slashes into a single slash here.
   * The first path component (i.e. root) does not get an entry in the list.
   */
  private static List<String> getPathComponents(String path)
      throws IOException {
    ArrayList<String> ret = new ArrayList<String>();
    for (String component : path.split(Path.SEPARATOR)) {
      if (!component.isEmpty()) {
        ret.add(component);
      }
    }
    return ret;
  }

  private String schemeFromPath(Path path) throws IOException {
    String scheme = pathPattern.toUri().getScheme();
    if (scheme == null) {
      if (fs != null) {
        scheme = fs.getUri().getScheme();
      } else {
        scheme = fc.getFSofPath(path).getUri().getScheme();
      }
    }
    return scheme;
  }

  private String authorityFromPath(Path path) throws IOException {
    String authority = pathPattern.toUri().getAuthority();
    if (authority == null) {
      if (fs != null) {
        authority = fs.getUri().getAuthority();
      } else {
        authority = fc.getFSofPath(path).getUri().getAuthority();
      }
    }
    return authority ;
  }

  public FileStatus[] glob() throws IOException {
    // First we get the scheme and authority of the pattern that was passed
    // in.
    String scheme = schemeFromPath(pathPattern);
    String authority = authorityFromPath(pathPattern);

    // Next we strip off everything except the pathname itself, and expand all
    // globs.  Expansion is a process which turns "grouping" clauses,
    // expressed as brackets, into separate path patterns.
    String pathPatternString = pathPattern.toUri().getPath();
    List<String> flattenedPatterns = GlobExpander.expand(pathPatternString);

    // Now loop over all flattened patterns.  In every case, we'll be trying to
    // match them to entries in the filesystem.
    ArrayList<FileStatus> results = 
        new ArrayList<FileStatus>(flattenedPatterns.size());
    boolean sawWildcard = false;
    for (String flatPattern : flattenedPatterns) {
      // Get the absolute path for this flattened pattern.  We couldn't do 
      // this prior to flattening because of patterns like {/,a}, where which
      // path you go down influences how the path must be made absolute.
      Path absPattern =
          fixRelativePart(new Path(flatPattern .isEmpty() ? "." : flatPattern ));
      // Now we break the flattened, absolute pattern into path components.
      // For example, /a/*/c would be broken into the list [a, *, c]
      List<String> components =
          getPathComponents(absPattern.toUri().getPath());
      // Starting out at the root of the filesystem, we try to match
      // filesystem entries against pattern components.
      ArrayList<FileStatus> candidates = new ArrayList<FileStatus>(1);
      candidates.add(new FileStatus(0, true, 0, 0, 0,
          new Path(scheme, authority, "/")));

      for (String component : components) {
        ArrayList<FileStatus> newCandidates =
            new ArrayList<FileStatus>(candidates.size());
        GlobFilter globFilter = new GlobFilter(component);
        if (globFilter.hasPattern()) {
          sawWildcard = true;
        }
        if (candidates.isEmpty() && sawWildcard) {
          break;
        }
        for (FileStatus candidate : candidates) {
          FileStatus resolvedCandidate = candidate;
          if (candidate.isSymlink()) {
            // We have to resolve symlinks, because otherwise we don't know
            // whether they are directories.
            resolvedCandidate = getFileStatus(candidate.getPath());
          }
          if (resolvedCandidate == null ||
              resolvedCandidate.isDirectory() == false) {
            continue;
          }
          FileStatus[] children = listStatus(candidate.getPath());
          for (FileStatus child : children) {
            // Set the child path based on the parent path.
            // This keeps the symlinks in our path.
            child.setPath(new Path(candidate.getPath(),
                    child.getPath().getName()));
            if (globFilter.accept(child.getPath())) {
              newCandidates.add(child);
            }
          }
        }
        candidates = newCandidates;
      }
      for (FileStatus status : candidates) {
        // HADOOP-3497 semantics: the user-defined filter is applied at the
        // end, once the full path is built up.
        if (filter.accept(status.getPath())) {
          results.add(status);
        }
      }
    }
    /*
     * When the input pattern "looks" like just a simple filename, and we
     * can't find it, we return null rather than an empty array.
     * This is a special case which the shell relies on.
     *
     * To be more precise: if there were no results, AND there were no
     * groupings (aka brackets), and no wildcards in the input (aka stars),
     * we return null.
     */
    if ((!sawWildcard) && results.isEmpty() &&
        (flattenedPatterns.size() <= 1)) {
      return null;
    }
    return results.toArray(new FileStatus[0]);
  }
}
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/FSWrapper.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/FSWrapper.java
index e8875bf08da..ae4ad059b68 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/FSWrapper.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/FSWrapper.java
@@ -109,4 +109,7 @@ abstract public void createSymlink(final Path target, final Path link,
   abstract public FileStatus[] listStatus(final Path f)
       throws AccessControlException, FileNotFoundException,
       UnsupportedFileSystemException, IOException;
  
  abstract public FileStatus[] globStatus(Path pathPattern, PathFilter filter)
      throws IOException;
 }
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/FileContextTestWrapper.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/FileContextTestWrapper.java
index 56736da90e0..e10b22edb7c 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/FileContextTestWrapper.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/FileContextTestWrapper.java
@@ -332,4 +332,10 @@ public void setTimes(Path f, long mtime, long atime)
       FileNotFoundException, UnsupportedFileSystemException, IOException {
     return fc.util().listStatus(f);
   }

  @Override
  public FileStatus[] globStatus(Path pathPattern, PathFilter filter)
      throws IOException {
    return fc.util().globStatus(pathPattern, filter);
  }
 }
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/FileSystemTestWrapper.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/FileSystemTestWrapper.java
index 28656288c17..eb5df084b97 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/FileSystemTestWrapper.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/FileSystemTestWrapper.java
@@ -397,4 +397,10 @@ public void setTimes(Path f, long mtime, long atime)
       FileNotFoundException, UnsupportedFileSystemException, IOException {
     return fs.listStatus(f);
   }

  @Override
  public FileStatus[] globStatus(Path pathPattern, PathFilter filter)
      throws IOException {
    return fs.globStatus(pathPattern, filter);
  }
 }
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestFileUtil.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestFileUtil.java
index 5672508127a..a9646d33b39 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestFileUtil.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestFileUtil.java
@@ -793,6 +793,8 @@ public void testCreateJarWithClassPath() throws Exception {
         }
       }
       List<String> actualClassPaths = Arrays.asList(classPathAttr.split(" "));
      Collections.sort(expectedClassPaths);
      Collections.sort(actualClassPaths);
       Assert.assertEquals(expectedClassPaths, actualClassPaths);
     } finally {
       if (jarFile != null) {
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestPath.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestPath.java
index 7a5843a8a75..0f6bf71bded 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestPath.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestPath.java
@@ -28,11 +28,38 @@
 import org.apache.hadoop.io.AvroTestUtil;
 import org.apache.hadoop.util.Shell;
 
import junit.framework.TestCase;
import com.google.common.base.Joiner;
 
import static org.junit.Assert.fail;
import junit.framework.TestCase;
 
 public class TestPath extends TestCase {
  /**
   * Merge a bunch of Path objects into a sorted semicolon-separated
   * path string.
   */
  public static String mergeStatuses(Path paths[]) {
    String pathStrings[] = new String[paths.length];
    int i = 0;
    for (Path path : paths) {
      pathStrings[i++] = path.toUri().getPath();
    }
    Arrays.sort(pathStrings);
    return Joiner.on(";").join(pathStrings);
  }

  /**
   * Merge a bunch of FileStatus objects into a sorted semicolon-separated
   * path string.
   */
  public static String mergeStatuses(FileStatus statuses[]) {
    Path paths[] = new Path[statuses.length];
    int i = 0;
    for (FileStatus status : statuses) {
      paths[i++] = status.getPath();
    }
    return mergeStatuses(paths);
  }

   @Test (timeout = 30000)
   public void testToString() {
     toStringTest("/");
@@ -352,10 +379,11 @@ public void testGlobEscapeStatus() throws Exception {
     // ensure globStatus with "*" finds all dir contents
     stats = lfs.globStatus(new Path(testRoot, "*"));
     Arrays.sort(stats);
    assertEquals(paths.length, stats.length);
    for (int i=0; i < paths.length; i++) {
      assertEquals(paths[i].getParent(), stats[i].getPath());
    Path parentPaths[] = new Path[paths.length];
    for (int i = 0; i < paths.length; i++) {
      parentPaths[i] = paths[i].getParent();
     }
    assertEquals(mergeStatuses(parentPaths), mergeStatuses(stats));
 
     // ensure that globStatus with an escaped "\*" only finds "*"
     stats = lfs.globStatus(new Path(testRoot, "\\*"));
@@ -365,9 +393,7 @@ public void testGlobEscapeStatus() throws Exception {
     // try to glob the inner file for all dirs
     stats = lfs.globStatus(new Path(testRoot, "*/f"));
     assertEquals(paths.length, stats.length);
    for (int i=0; i < paths.length; i++) {
      assertEquals(paths[i], stats[i].getPath());
    }
    assertEquals(mergeStatuses(paths), mergeStatuses(stats));
 
     // try to get the inner file for only the "*" dir
     stats = lfs.globStatus(new Path(testRoot, "\\*/f"));
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/fs/TestGlobPaths.java b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/fs/TestGlobPaths.java
index 3b69b04205c..76f668f6fee 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/fs/TestGlobPaths.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/fs/TestGlobPaths.java
@@ -20,14 +20,18 @@
 import static org.junit.Assert.*;
 
 import java.io.IOException;
import java.util.Arrays;
 import java.util.regex.Pattern;
 
 import org.apache.commons.lang.StringUtils;
 import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.permission.FsPermission;
 import org.apache.hadoop.hdfs.HdfsConfiguration;
 import org.apache.hadoop.hdfs.MiniDFSCluster;
 import org.junit.*;
 
import com.google.common.base.Joiner;

 public class TestGlobPaths {
 
   static class RegexPathFilter implements PathFilter {
@@ -784,4 +788,265 @@ public void cleanupDFS() throws IOException {
     fs.delete(new Path(USER_DIR), true);
   }
   
  /**
   * A glob test that can be run on either FileContext or FileSystem.
   */
  private static interface FSTestWrapperGlobTest {
    void run(FSTestWrapper wrap, FileSystem fs, FileContext fc)
        throws Exception;
  }

  /**
   * Run a glob test on FileSystem.
   */
  private static void testOnFileSystem(FSTestWrapperGlobTest test) throws Exception {
    Configuration conf = new HdfsConfiguration();
    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).build();
    try {
      FileSystem fs = FileSystem.get(conf);
      test.run(new FileSystemTestWrapper(fs), fs, null);
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Run a glob test on FileContext.
   */
  private static void testOnFileContext(FSTestWrapperGlobTest test) throws Exception {
    Configuration conf = new HdfsConfiguration();
    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).build();
    try {
      FileContext fc = FileContext.getFileContext(conf);
      test.run(new FileContextTestWrapper(fc), null, fc);
    } finally {
      cluster.shutdown();
    }
  }
  
  /**
   * Accept all paths.
   */
  private static class AcceptAllPathFilter implements PathFilter {
    @Override
    public boolean accept(Path path) {
      return true;
    }
  }

  /**
   * Accept only paths ending in Z.
   */
  private static class AcceptPathsEndingInZ implements PathFilter {
    @Override
    public boolean accept(Path path) {
      String stringPath = path.toUri().getPath();
      return stringPath.endsWith("z");
    }
  }

  /**
   * Test globbing through symlinks.
   */
  private static class TestGlobWithSymlinks implements FSTestWrapperGlobTest {
    public void run(FSTestWrapper wrap, FileSystem fs, FileContext fc)
        throws Exception {
      // Test that globbing through a symlink to a directory yields a path
      // containing that symlink.
      wrap.mkdir(new Path("/alpha"),
          FsPermission.getDirDefault(), false);
      wrap.createSymlink(new Path("/alpha"), new Path("/alphaLink"), false);
      wrap.mkdir(new Path("/alphaLink/beta"),
          FsPermission.getDirDefault(), false);
      // Test simple glob
      FileStatus[] statuses =
          wrap.globStatus(new Path("/alpha/*"), new AcceptAllPathFilter());
      Assert.assertEquals(1, statuses.length);
      Assert.assertEquals("/alpha/beta",
          statuses[0].getPath().toUri().getPath());
      // Test glob through symlink
      statuses =
          wrap.globStatus(new Path("/alphaLink/*"), new AcceptAllPathFilter());
      Assert.assertEquals(1, statuses.length);
      Assert.assertEquals("/alphaLink/beta",
          statuses[0].getPath().toUri().getPath());
      // If the terminal path component in a globbed path is a symlink,
      // we don't dereference that link.
      wrap.createSymlink(new Path("beta"), new Path("/alphaLink/betaLink"),
          false);
      statuses = wrap.globStatus(new Path("/alpha/betaLi*"),
          new AcceptAllPathFilter());
      Assert.assertEquals(1, statuses.length);
      Assert.assertEquals("/alpha/betaLink",
          statuses[0].getPath().toUri().getPath());
      // todo: test symlink-to-symlink-to-dir, etc.
    }
  }

  @Test
  public void testGlobWithSymlinksOnFS() throws Exception {
    testOnFileSystem(new TestGlobWithSymlinks());
  }

  @Test
  public void testGlobWithSymlinksOnFC() throws Exception {
    testOnFileContext(new TestGlobWithSymlinks());
  }

  /**
   * Test globbing symlinks to symlinks.
   *
   * Also test globbing dangling symlinks.  It should NOT throw any exceptions!
   */
  private static class TestGlobWithSymlinksToSymlinks
      implements FSTestWrapperGlobTest {
    public void run(FSTestWrapper wrap, FileSystem fs, FileContext fc)
        throws Exception {
      // Test that globbing through a symlink to a symlink to a directory
      // fully resolves
      wrap.mkdir(new Path("/alpha"), FsPermission.getDirDefault(), false);
      wrap.createSymlink(new Path("/alpha"), new Path("/alphaLink"), false);
      wrap.createSymlink(new Path("/alphaLink"),
          new Path("/alphaLinkLink"), false);
      wrap.mkdir(new Path("/alpha/beta"), FsPermission.getDirDefault(), false);
      // Test glob through symlink to a symlink to a directory
      FileStatus statuses[] =
          wrap.globStatus(new Path("/alphaLinkLink"), new AcceptAllPathFilter());
      Assert.assertEquals(1, statuses.length);
      Assert.assertEquals("/alphaLinkLink",
          statuses[0].getPath().toUri().getPath());
      statuses =
          wrap.globStatus(new Path("/alphaLinkLink/*"), new AcceptAllPathFilter());
      Assert.assertEquals(1, statuses.length);
      Assert.assertEquals("/alphaLinkLink/beta",
          statuses[0].getPath().toUri().getPath());
      // Test glob of dangling symlink (theta does not actually exist)
      wrap.createSymlink(new Path("theta"), new Path("/alpha/kappa"), false);
      statuses = wrap.globStatus(new Path("/alpha/kappa/kappa"),
              new AcceptAllPathFilter());
      Assert.assertNull(statuses);
      // Test glob of symlinks
      wrap.createFile("/alpha/beta/gamma");
      wrap.createSymlink(new Path("gamma"),
          new Path("/alpha/beta/gammaLink"), false);
      wrap.createSymlink(new Path("gammaLink"),
          new Path("/alpha/beta/gammaLinkLink"), false);
      wrap.createSymlink(new Path("gammaLinkLink"),
          new Path("/alpha/beta/gammaLinkLinkLink"), false);
      statuses = wrap.globStatus(new Path("/alpha/*/gammaLinkLinkLink"),
              new AcceptAllPathFilter());
      Assert.assertEquals(1, statuses.length);
      Assert.assertEquals("/alpha/beta/gammaLinkLinkLink",
          statuses[0].getPath().toUri().getPath());
      statuses = wrap.globStatus(new Path("/alpha/beta/*"),
              new AcceptAllPathFilter());
      Assert.assertEquals("/alpha/beta/gamma;/alpha/beta/gammaLink;" +
          "/alpha/beta/gammaLinkLink;/alpha/beta/gammaLinkLinkLink",
          TestPath.mergeStatuses(statuses));
      // Let's create two symlinks that point to each other, and glob on them.
      wrap.createSymlink(new Path("tweedledee"),
          new Path("/tweedledum"), false);
      wrap.createSymlink(new Path("tweedledum"),
          new Path("/tweedledee"), false);
      statuses = wrap.globStatus(new Path("/tweedledee/unobtainium"),
              new AcceptAllPathFilter());
      Assert.assertNull(statuses);
    }
  }

  @Test
  public void testGlobWithSymlinksToSymlinksOnFS() throws Exception {
    testOnFileSystem(new TestGlobWithSymlinksToSymlinks());
  }

  @Test
  public void testGlobWithSymlinksToSymlinksOnFC() throws Exception {
    testOnFileContext(new TestGlobWithSymlinksToSymlinks());
  }

  /**
   * Test globbing symlinks with a custom PathFilter
   */
  private static class TestGlobSymlinksWithCustomPathFilter
      implements FSTestWrapperGlobTest {
    public void run(FSTestWrapper wrap, FileSystem fs, FileContext fc)
        throws Exception {
      // Test that globbing through a symlink to a symlink to a directory
      // fully resolves
      wrap.mkdir(new Path("/alpha"), FsPermission.getDirDefault(), false);
      wrap.createSymlink(new Path("/alpha"), new Path("/alphaLinkz"), false);
      wrap.mkdir(new Path("/alpha/beta"), FsPermission.getDirDefault(), false);
      wrap.mkdir(new Path("/alpha/betaz"), FsPermission.getDirDefault(), false);
      // Test glob through symlink to a symlink to a directory, with a PathFilter
      FileStatus statuses[] =
          wrap.globStatus(new Path("/alpha/beta"), new AcceptPathsEndingInZ());
      Assert.assertNull(statuses);
      statuses =
          wrap.globStatus(new Path("/alphaLinkz/betaz"), new AcceptPathsEndingInZ());
      Assert.assertEquals(1, statuses.length);
      Assert.assertEquals("/alphaLinkz/betaz",
          statuses[0].getPath().toUri().getPath());
      statuses =
          wrap.globStatus(new Path("/*/*"), new AcceptPathsEndingInZ());
      Assert.assertEquals("/alpha/betaz;/alphaLinkz/betaz",
          TestPath.mergeStatuses(statuses));
      statuses =
          wrap.globStatus(new Path("/*/*"), new AcceptAllPathFilter());
      Assert.assertEquals("/alpha/beta;/alpha/betaz;" +
          "/alphaLinkz/beta;/alphaLinkz/betaz",
          TestPath.mergeStatuses(statuses));
    }
  }

  @Test
  public void testGlobSymlinksWithCustomPathFilterOnFS() throws Exception {
    testOnFileSystem(new TestGlobSymlinksWithCustomPathFilter());
  }

  @Test
  public void testGlobSymlinksWithCustomPathFilterOnFC() throws Exception {
    testOnFileContext(new TestGlobSymlinksWithCustomPathFilter());
  }

  /**
   * Test that globStatus fills in the scheme even when it is not provided.
   */
  private static class TestGlobFillsInScheme
      implements FSTestWrapperGlobTest {
    public void run(FSTestWrapper wrap, FileSystem fs, FileContext fc) 
        throws Exception {
      // Verify that the default scheme is hdfs, when we don't supply one.
      wrap.mkdir(new Path("/alpha"), FsPermission.getDirDefault(), false);
      wrap.createSymlink(new Path("/alpha"), new Path("/alphaLink"), false);
      FileStatus statuses[] =
          wrap.globStatus(new Path("/alphaLink"), new AcceptAllPathFilter());
      Assert.assertEquals(1, statuses.length);
      Path path = statuses[0].getPath();
      Assert.assertEquals("/alphaLink", path.toUri().getPath());
      Assert.assertEquals("hdfs", path.toUri().getScheme());
      if (fc != null) {
        // If we're using FileContext, then we can list a file:/// URI.
        // Since everyone should have the root directory, we list that.
        statuses =
            wrap.globStatus(new Path("file:///"), new AcceptAllPathFilter());
        Assert.assertEquals(1, statuses.length);
        Path filePath = statuses[0].getPath();
        Assert.assertEquals("file", filePath.toUri().getScheme());
        Assert.assertEquals("/", filePath.toUri().getPath());
      } else {
        // The FileSystem we passed in should have scheme 'hdfs'
        Assert.assertEquals("hdfs", fs.getScheme());
      }
    }
  }

  @Test
  public void testGlobFillsInSchemeOnFS() throws Exception {
    testOnFileSystem(new TestGlobFillsInScheme());
  }

  @Test
  public void testGlobFillsInSchemeOnFC() throws Exception {
    testOnFileContext(new TestGlobFillsInScheme());
  }
 }
- 
2.19.1.windows.1

