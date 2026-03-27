package com.tumuyan.ncnn.realsr;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.text.format.Formatter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class DeviceInfo {
    public static String getInfo(Context context) {
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        
        if (mActivityManager != null) {
            mActivityManager.getMemoryInfo(memoryInfo);
        }

        String totalMemStr = Formatter.formatFileSize(context, memoryInfo.totalMem);
        String cpuHardware = getCpuName();
        return context.getString(R.string.model) + ": " + Build.MODEL + ", " +
               context.getString(R.string.system_version) + ": " + Build.VERSION.RELEASE + ", " +
               context.getString(R.string.cpu_model) + ": " + cpuHardware + ", " +
               context.getString(R.string.ram_size) + ": " + totalMemStr;
    }

    private static String getCpuName() {
        String hardware = Build.HARDWARE;
        if ("unknown".equalsIgnoreCase(hardware) || hardware.length() < 4) {
            try (BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains("Hardware") || line.contains("model name")) {
                        return line.split(":")[1].trim();
                    }
                }
            } catch (IOException ignored) {}
        }
        return hardware;
    }

    public static String getConfigStr(boolean cpu, int tile) {
        StringBuilder sb = new StringBuilder(cpu ? "CPU" : "GPU");
        if (tile > 0) {
            sb.append(", tile=").append(tile);
        }
        return sb.toString();
    }
}
