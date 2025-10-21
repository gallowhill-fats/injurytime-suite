/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.gmail.ingest;

import com.injurytime.storage.api.JpaAccess;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.function.Function;

public final class PgJpaAccess implements JpaAccess {
  private volatile EntityManagerFactory emf;

  private EntityManagerFactory emf() {
    EntityManagerFactory ref = emf;
    if (ref == null) {
      synchronized (this) {
        if (emf == null) {
          // MUST match your persistence.xml unit name
          emf = Persistence.createEntityManagerFactory("injurytime-pg");
        }
        ref = emf;
      }
    }
    return ref;
  }

  @Override
  public <R> R tx(Function<EntityManager,R> work) {
    EntityManager em = emf().createEntityManager();
    var tx = em.getTransaction();
    try {
      tx.begin();
      R out = work.apply(em);
      tx.commit();
      return out;
    } catch (RuntimeException ex) {
      if (tx.isActive()) tx.rollback();
      throw ex;
    } finally {
      em.close();
    }
  }
}

