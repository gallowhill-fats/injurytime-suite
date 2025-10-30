/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.monitoring;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.openide.modules.OnStart;
import org.openide.util.Lookup;

@OnStart
public final class MXBootstrap implements Runnable {
  private static final Logger LOG = Logger.getLogger("com.injurytime.monitoring");
  private ObjectName objName;

  @Override public void run() {
    try {
      // dial the module logger up to INFO just in case
      LOG.setLevel(Level.INFO);

      // resolve JpaAccess now, so AppStats has it
      var jpa = Lookup.getDefault().lookup(com.injurytime.storage.api.JpaAccess.class);
      var impl = new AppStats(jpa);                // implements AppStatsMXBean

      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      objName = new ObjectName("com.injurytime:type=Stats");

      // register as MXBean explicitly (safer across tools)
      var mx = new javax.management.StandardMBean(impl, AppStatsMXBean.class, true);
      mbs.registerMBean(mx, objName);

      // log PID + domains to prove we’re here
      var pid = ManagementFactory.getRuntimeMXBean().getName(); // "12345@host"
      LOG.info(() -> "[monitoring.mx] Registered MBean " + objName + " in PID " + pid);
      LOG.info(() -> "[monitoring.mx] Domains: " + Arrays.toString(mbs.getDomains()));
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "JMX bootstrap failed", e);
    }
  }
}

