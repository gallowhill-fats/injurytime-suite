// in storage.impl.jpa
package com.injurytime.storage.jpa;

import com.injurytime.storage.api.JpaAccess;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.apache.derby.jdbc.EmbeddedDriver;  // <-- this should now resolve

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = JpaAccess.class, position = 100)
public final class JpaAccessImpl implements JpaAccess {
  private volatile EntityManagerFactory _emf;

  private EntityManagerFactory emf() {
    var local = _emf;
    if (local != null) return local;

    synchronized (this) {
      if (_emf != null) return _emf;

      // (Optional) quick sanity ping — safe for client mode
      try (var conn = java.sql.DriverManager.getConnection(
              "jdbc:derby://localhost:1527/c:/dbs/stramash;create=false",
              "clayton", "clayton")) {
        // ok
      } catch (java.sql.SQLException e) {
        java.util.logging.Logger.getLogger(getClass().getName())
          .log(java.util.logging.Level.WARNING, "Derby client probe failed", e);
      }

      _emf = jakarta.persistence.Persistence.createEntityManagerFactory("injurytimePU");
      return _emf;
    }
  }

  @Override
  public <R> R tx(java.util.function.Function<jakarta.persistence.EntityManager, R> work) {
    var em = emf().createEntityManager();
    try {
      var tx = em.getTransaction();
      tx.begin();
      R result = work.apply(em);
      tx.commit();
      return result;
    } catch (RuntimeException ex) {
      try { var t = em.getTransaction(); if (t.isActive()) t.rollback(); } catch (Exception ignore) {}
      throw ex;
    } finally {
      em.close();
    }
  }
}

