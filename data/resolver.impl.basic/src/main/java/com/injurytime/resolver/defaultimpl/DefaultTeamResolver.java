/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.resolver.defaultimpl;

import com.injurytime.resolver.api.*;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = TeamResolver.class, position = 100)
public final class DefaultTeamResolver implements TeamResolver {
  @Override
  public TeamResolution resolveTeam(String rawName, SeasonKey hint) {
    return new TeamResolution(null, rawName, 0.0); // fallback no-op
  }
}

