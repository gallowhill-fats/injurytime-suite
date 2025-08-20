/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.storage.jpa;

import com.injurytime.storage.api.JpaAccess;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = com.injurytime.storage.api.JpaAccess.class)
public final class JpaAccessImpl implements JpaAccess {

  private final EntityManagerFactory emf =
      Persistence.createEntityManagerFactory("injuryPU"); // your persistence-unit name

  @Override
  public <R> R tx(java.util.function.Function<EntityManager, R> work) {
    EntityManager em = emf.createEntityManager();
    try {
      var tx = em.getTransaction();
      tx.begin();
      R r = work.apply(em);
      tx.commit();
      return r;
    } catch (RuntimeException ex) {
      var t = em.getTransaction();
      if (t.isActive()) t.rollback();
      throw ex;
    } finally {
      em.close();
    }
  }
}

