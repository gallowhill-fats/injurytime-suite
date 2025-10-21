/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.storage.api;

public final class AppConfigExt {
  private AppConfigExt() {}

  public static String getOr(String key, String defVal) {
    String v = AppConfig.get(key);
    return (v == null || v.isBlank()) ? defVal : v;
  }

  public static int getInt(String key, int defVal) {
    String v = AppConfig.get(key);
    if (v == null || v.isBlank()) return defVal;
    try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return defVal; }
  }

  public static boolean getBool(String key, boolean defVal) {
    String v = AppConfig.get(key);
    return (v == null || v.isBlank()) ? defVal : Boolean.parseBoolean(v.trim());
  }
}

