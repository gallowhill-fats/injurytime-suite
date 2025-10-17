/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.pg.tools.squad;

import com.injurytime.storage.api.JpaAccess;
import com.injurytime.storage.api.AppConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.Map;
import static java.util.Map.entry;
import org.hibernate.jpa.HibernatePersistenceProvider;

final class PgJpaAccess implements JpaAccess {
  private volatile EntityManagerFactory emf;

  private EntityManagerFactory emf() {
  var ref = emf;
  if (ref != null) return ref;

  synchronized (this) {
    ref = emf;
    if (ref != null) return ref;

    // DB overrides from AppConfig
    Map<String, Object> overrides = Map.ofEntries(
        entry("jakarta.persistence.jdbc.url",      AppConfig.get("PG_URL")),
        entry("jakarta.persistence.jdbc.user",     AppConfig.get("PG_USER")),
        entry("jakarta.persistence.jdbc.password", AppConfig.get("PG_PASS"))
    );

    // IMPORTANT: use this module's classloader as TCCL while creating EMF
    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    ClassLoader mine = PgJpaAccess.class.getClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(mine);

      // 1) try standard discovery
      try {
        ref = Persistence.createEntityManagerFactory("injurytime-pg", overrides);
      } catch (Exception discoveryFail) {
        // 2) explicit provider fallback
        ref = new HibernatePersistenceProvider()
                .createEntityManagerFactory("injurytime-pg", overrides);
      }

      if (ref == null) {
        // one more hard fail with a helpful message
        throw new IllegalStateException(
          "Could not create EMF for 'injurytime-pg'. " +
          "persistence.xml visible? " + (mine.getResource("META-INF/persistence.xml") != null) +
          ", provider visible? " + canLoad("org.hibernate.jpa.HibernatePersistenceProvider", mine)
        );
      }

      emf = ref;
      return ref;

    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }
  }
}
  
  private static boolean canLoad(String cn, ClassLoader cl) {
  try { Class.forName(cn, false, cl); return true; } catch (Throwable t) { return false; }
}

  @Override public <T> T tx(java.util.function.Function<EntityManager, T> body) {
    var em = emf().createEntityManager();
    var tx = em.getTransaction();
    try {
      tx.begin();
      T result = body.apply(em);
      tx.commit();
      return result;
    } catch (RuntimeException ex) {
      if (tx.isActive()) tx.rollback();
      throw ex;
    } finally {
      em.close();
    }
  }
}

