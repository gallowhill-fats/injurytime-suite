/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.storage.jpa;

import com.injurytime.storage.api.JpaAccess;
import jakarta.persistence.*;
import java.util.*;
import java.util.function.*;
import com.injurytime.storage.api.AppConfig; // your class shown earlier

@org.openide.util.lookup.ServiceProvider(service = JpaAccess.class) // <-- binds it
public final class PgJpaAccess implements JpaAccess {

  private static volatile EntityManagerFactory EMF;

  private static Map<String,Object> buildOverrides() {
    // Allow overrides from injurytime.local.properties (optional)
    Map<String,Object> m = new HashMap<>();
    String url = AppConfig.get("PG_JDBC_URL");
    String user = AppConfig.get("PG_USER");
    String pass = AppConfig.get("PG_PASSWORD");

    if (url  != null) m.put("jakarta.persistence.jdbc.url", url);
    if (user != null) m.put("jakarta.persistence.jdbc.user", user);
    if (pass != null) m.put("jakarta.persistence.jdbc.password", pass);

    // Safe defaults; Hibernate will auto-pick PG dialect
    m.putIfAbsent("hibernate.hbm2ddl.auto", "none");
    m.putIfAbsent("hibernate.show_sql", "false");
    m.putIfAbsent("hibernate.format_sql", "true");
    return m;
  }

  private static EntityManagerFactory emf() {
    EntityManagerFactory ref = EMF;
    if (ref == null) {
      synchronized (PgJpaAccess.class) {
        ref = EMF;
        if (ref == null) {
          EMF = ref = Persistence.createEntityManagerFactory("injurytime-pg", buildOverrides());
          // Close cleanly on app exit
          Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { if (EMF != null && EMF.isOpen()) EMF.close(); } catch (Throwable ignored) {}
          }));
        }
      }
    }
    return ref;
  }

  @Override
  public <R> R tx(Function<EntityManager, R> work) {
    EntityManager em = emf().createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      R result = work.apply(em);
      tx.commit();
      return result;
    } catch (RuntimeException ex) {
      if (tx.isActive()) tx.rollback();
      throw ex;
    } finally {
      em.close();
    }
  }

  // convenience if you like explicit name
  public static EntityManagerFactory getEmf() { return emf(); }
}

