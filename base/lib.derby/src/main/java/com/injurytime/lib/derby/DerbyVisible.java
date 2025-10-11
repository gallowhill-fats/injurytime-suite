/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.lib.derby;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.Lookup;

public final class DerbyVisible {
  private static final Logger LOG = Logger.getLogger(DerbyVisible.class.getName());
  private static volatile boolean registered;

  private DerbyVisible() {}

  public static void ensureLoaded() {
    if (registered) return;

    try {
      // Locate THIS module (either classes/ or the module JAR)
      URL location = DerbyVisible.class.getProtectionDomain().getCodeSource().getLocation();
      URI fileUri;

      if ("jar".equalsIgnoreCase(location.getProtocol())) {
        // jar:file:/.../com-injurytime-lib-derby.jar!/
        JarURLConnection juc = (JarURLConnection) location.openConnection();
        fileUri = juc.getJarFileURL().toURI(); // -> file:/.../com-injurytime-lib-derby.jar
      } else {
        // file:/.../modules/com-injurytime-lib-derby.jar  OR  file:/.../classes/
        fileUri = location.toURI();
      }

      Path base = Paths.get(fileUri);
      // If running from classes/, step up to .../modules/
      if (Files.isDirectory(base)) {
        // e.g. .../target/classes  -> .../target/nbm/cluster/.../modules
        // In a dev run, classes doesn’t have ext/, so prefer app image at runtime.
        // Fall back to classes/../modules/ layout if present.
        Path modDirCandidate = base.getParent(); // target/
        if (modDirCandidate != null) {
          Path modules = findModulesDir(modDirCandidate);
          if (modules != null) base = modules.resolve("com-injurytime-lib-derby.jar");
        }
      }

      // modulesDir = .../clusters/<cluster>/modules
      Path modulesDir = base.getParent();
      if (modulesDir == null) throw new IllegalStateException("Cannot resolve modules/ dir from: " + base);

      Path extDir = modulesDir.resolve("ext")
          .resolve("com.injurytime.lib.derby")
          .resolve("org-apache-derby");

      Path derbyJar       = extDir.resolve("derby.jar");
      Path derbySharedJar = extDir.resolve("derbyshared.jar");

      LOG.info(() -> "DerbyVisible: derby jars at:\n  " +
          derbyJar.toAbsolutePath() + "\n  " + derbySharedJar.toAbsolutePath());

      if (!Files.isRegularFile(derbyJar) || !Files.isRegularFile(derbySharedJar)) {
        throw new IllegalStateException("Derby jars not present: " + extDir.toAbsolutePath());
      }

      URL[] cp = { derbyJar.toUri().toURL(), derbySharedJar.toUri().toURL() };
      LOG.info(() -> "DerbyVisible: loading with URLs:\n  " + cp[0] + "\n  " + cp[1]);

      // Isolated CL so we’re not depending on NetBeans honoring Class-Path:
      try (URLClassLoader derbyCL = new URLClassLoader(cp, null)) {
        Class<?> drvClass = Class.forName("org.apache.derby.jdbc.EmbeddedDriver", true, derbyCL);
        Driver real = (Driver) drvClass.getDeclaredConstructor().newInstance();
        DriverManager.registerDriver(new DriverShim(real));
        registered = true;
        LOG.info("Derby EmbeddedDriver registered via URLClassLoader");
        return;
      }
    } catch (Exception first) {
      LOG.log(Level.WARNING, "URLClassLoader path failed; will try NB aggregate CL", first);
      // Fallback: NetBeans aggregate classloader (often sees Class-Path libs)
      try {
        ClassLoader nbCl = Lookup.getDefault().lookup(ClassLoader.class);
        Class<?> drvClass = Class.forName("org.apache.derby.jdbc.EmbeddedDriver", true, nbCl);
        Driver real = (Driver) drvClass.getDeclaredConstructor().newInstance();
        DriverManager.registerDriver(new DriverShim(real));
        registered = true;
        LOG.info("Derby EmbeddedDriver registered via NetBeans aggregate ClassLoader");
        return;
      } catch (Exception e) {
        LOG.log(Level.SEVERE, "Driver lookup/registration failed", e);
        throw new IllegalStateException("Derby EmbeddedDriver not visible/loaded from lib.derby module", e);
      }
    }
  }

  // Try to find .../modules when running from .../target or build tree
  private static Path findModulesDir(Path targetOrSimilar) {
    // Walk a few levels looking for a 'modules' dir that contains our module jar
    for (int i = 0; i < 5 && targetOrSimilar != null; i++, targetOrSimilar = targetOrSimilar.getParent()) {
      Path modules = targetOrSimilar.resolve("nbm").resolve("clusters").resolve("extra").resolve("modules");
      if (Files.isDirectory(modules)) return modules;
    }
    return null;
  }
  
  static final class DriverShim implements Driver {
  private final Driver delegate;
  DriverShim(Driver delegate) { this.delegate = delegate; }
  @Override public Connection connect(String url, Properties info) throws SQLException { return delegate.connect(url, info); }
  @Override public boolean acceptsURL(String url) throws SQLException { return delegate.acceptsURL(url); }
  @Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException { return delegate.getPropertyInfo(url, info); }
  @Override public int getMajorVersion() { return delegate.getMajorVersion(); }
  @Override public int getMinorVersion() { return delegate.getMinorVersion(); }
  @Override public boolean jdbcCompliant() { return delegate.jdbcCompliant(); }
  @Override public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException { return delegate.getParentLogger(); }
}
}
