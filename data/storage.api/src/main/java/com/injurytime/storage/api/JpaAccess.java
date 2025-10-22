/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.storage.api;

import jakarta.persistence.EntityManager;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 *
 * @author clayton
 */
public interface JpaAccess {

    <R> R tx(java.util.function.Function<jakarta.persistence.EntityManager, R> work);

    // rename this so there's no overload clash
    default void txVoid(java.util.function.Consumer<jakarta.persistence.EntityManager> work)
    {
        tx(em ->
        {
            work.accept(em);
            return null;
        });
    }
}
