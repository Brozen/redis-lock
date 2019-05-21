package com.limbo.lock;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * @author Brozen
 * @date 2019/5/8 3:32 PM
 */
@Slf4j
public class Utils {

    /**
     * 获取JVM进程ID
     */
    public static int getPID() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        return Integer.valueOf(runtimeMXBean.getName().split("@")[0]);
    }

    /**
     * 获取MAC地址，取第一个可用网卡的MAC地址，没有可用网卡则返回空字符串
     */
    public static String getMac() {
        try {
            Enumeration<NetworkInterface> el = NetworkInterface.getNetworkInterfaces();
            InetAddress ip;
            while (el.hasMoreElements()) {
                NetworkInterface ni = el.nextElement();
                byte[] mac = ni.getHardwareAddress();
                if (mac == null || mac.length == 0)
                    continue;

                Enumeration<InetAddress> nii = ni.getInetAddresses();
                while (nii.hasMoreElements()) {
                    ip = nii.nextElement();
                    if (ip instanceof Inet6Address)
                        continue;
                    if (!ip.isReachable(3000))
                        continue;

                    StringBuilder sb = new StringBuilder();
                    for (byte aMac : mac) {
                        //字节转换为整数
                        int temp = aMac & 0xff;
                        String str = Integer.toHexString(temp);

                        if (str.length() == 1) {
                            sb.append("0").append(str);
                        } else {
                            sb.append(str);
                        }
                    }
                    return sb.toString();
                }
            }
            return "";
        } catch (Exception e) {
            log.error("获取Mac地址错误！", e);
            throw new RuntimeException(e);
        }
    }




}
