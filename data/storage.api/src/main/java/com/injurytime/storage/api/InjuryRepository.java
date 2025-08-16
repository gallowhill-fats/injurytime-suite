/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.storage.api;
import com.injurytime.model.InjuryFact;
import java.util.*;

public interface InjuryRepository {
  void upsert(InjuryFact fact);
  List<InjuryFact> recent(int limit);
  Optional<InjuryFact> findByFingerprint(String fp);
}

