/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.monitoring;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import org.openide.modules.ModuleInstall;

public class Installer extends ModuleInstall {
  private ObjectName name;

  @Override public void restored() {
    try {
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      name = new ObjectName("com.injurytime:type=AppStats");
      if (!server.isRegistered(name)) {
        server.registerMBean(new AppStats(), name);
      }
    } catch (Exception e) {
      // log if you like
    }
  }

  @Override public void close() {
    try {
      if (name != null) {
        ManagementFactory.getPlatformMBeanServer().unregisterMBean(name);
      }
    } catch (Exception ignored) {}
  }
}
