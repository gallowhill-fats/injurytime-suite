package com.injurytime.monitoring;

import com.injurytime.storage.api.JpaAccess;
import java.lang.management.ManagementFactory;
import java.util.logging.Logger;
import javax.management.*;
import org.openide.modules.ModuleInstall;
import org.openide.util.Lookup;

public final class Installer extends ModuleInstall {

    private ObjectName objName;

    @Override
    public void restored()
    {
        try
        {
            JpaAccess jpa = Lookup.getDefault().lookup(JpaAccess.class);
            AppStats impl = new AppStats(jpa);

            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            objName = new ObjectName("com.injurytime:type=Stats");

            // Explicitly register as MXBean to avoid “not compliant” surprises
            StandardMBean mbean = new StandardMBean(impl, AppStatsMXBean.class, /*isMXBean*/ true);
            mbs.registerMBean(mbean, objName);

            var rt = java.lang.management.ManagementFactory.getRuntimeMXBean();
            var name = rt.getName();               // format "PID@hostname"
            Logger.getAnonymousLogger().info("[monitoring.mx] Runtime: " + name);

            System.out.println("[monitoring.mx] Domains after register:");
            for (String d : mbs.getDomains())
            {
                if (d.startsWith("com.") || d.equals("JMImplementation"))
                {
                    Logger.getAnonymousLogger().info("  - " + d);
                }
            }
            Logger.getAnonymousLogger().info("[monitoring.mx] Has com.injurytime? "
                    + java.util.Arrays.asList(mbs.getDomains()).contains("com.injurytime"));

            Logger.getAnonymousLogger().info("[monitoring.mx] Registered MBean " + objName);
        } catch (Exception e)
        {
            e.printStackTrace(); // check IDE Output window for any NotCompliantMBeanException
        }
    }

    @Override
    public void close()
    {
        try
        {
            if (objName != null)
            {
                ManagementFactory.getPlatformMBeanServer().unregisterMBean(objName);
                System.out.println("[monitoring.mx] Unregistered MBean " + objName);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
