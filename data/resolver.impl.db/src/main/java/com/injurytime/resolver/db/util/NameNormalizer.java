/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.resolver.db.util;

import java.text.Normalizer;

public final class NameNormalizer {
  public String normalize(String s) {
    if (s == null) return "";
    // strip accents, lowercase, remove non-alnum
    String nfkd = Normalizer.normalize(s, Normalizer.Form.NFKD)
        .replaceAll("\\p{M}+", "");
    return nfkd.toLowerCase().replaceAll("[^a-z0-9]", "");
  }
}

