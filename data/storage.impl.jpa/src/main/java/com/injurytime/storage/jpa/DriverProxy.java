/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.storage.jpa;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

/** Wraps a Driver instance loaded by another ClassLoader so DriverManager can use it. */
final class DriverProxy implements Driver {
  private final Driver delegate;
  DriverProxy(Driver delegate) { this.delegate = delegate; }

  @Override public Connection connect(String url, Properties info) throws SQLException { return delegate.connect(url, info); }
  @Override public boolean acceptsURL(String url) throws SQLException { return delegate.acceptsURL(url); }
  @Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException { return delegate.getPropertyInfo(url, info); }
  @Override public int getMajorVersion() { return delegate.getMajorVersion(); }
  @Override public int getMinorVersion() { return delegate.getMinorVersion(); }
  @Override public boolean jdbcCompliant() { return delegate.jdbcCompliant(); }
  @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException { return delegate.getParentLogger(); }
}

