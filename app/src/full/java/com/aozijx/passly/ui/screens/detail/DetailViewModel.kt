package com.aozijx.passly.ui.screens.detail

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.view.WindowManager
import com.aozijx.passly.core.util.Logcat
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale
import java.util.TimeZone
import kotlin.math.pow
import kotlin.math.sqrt

// 数据模型提取到顶级
data class InfoItem(
    val key: String,
    val title: String,
    val category: String,
    val valueProvider: (SystemInfoManager) -> String
)

class SystemInfoManager(private val context: Context) {

    companion object {
        val INFO_ITEMS = listOf(
            // --- 系统与设备信息 ---
            InfoItem("deviceManufacturer", "设备制造商", "系统与设备信息") { Build.MANUFACTURER },
            InfoItem("deviceBrand", "设备品牌", "系统与设备信息") { Build.BRAND },
            InfoItem("deviceModel", "设备型号", "系统与设备信息") { Build.MODEL },
            InfoItem("systemName", "系统", "系统与设备信息") { it.customOSName },
            InfoItem("androidVersion", "Android 版本", "系统与设备信息") { Build.VERSION.RELEASE },
            InfoItem("sdkVersion", "SDK 版本", "系统与设备信息") { Build.VERSION.SDK_INT.toString() },
            InfoItem("cpu", "CPU 硬件", "系统与设备信息") { Build.HARDWARE },
            InfoItem("kernelVersion", "内核版本", "系统与设备信息") { System.getProperty("os.version") ?: "未知" },
            InfoItem("bootloader", "Bootloader", "系统与设备信息") { Build.BOOTLOADER },

            // --- CPU 详情 ---
            InfoItem("abis", "支持的架构 (ABIs)", "CPU 详情") { Build.SUPPORTED_ABIS.joinToString(", ") },

            // --- 屏幕信息 ---
            InfoItem("screenResolution", "屏幕分辨率", "屏幕信息") { it.screenResolution },
            InfoItem("screenSizeInch", "屏幕尺寸", "屏幕信息") { it.screenSizeInch },
            InfoItem("screenDensity", "屏幕密度", "屏幕信息") { String.format(Locale.US, "%.2f", it.screenDensity) },
            InfoItem("screenDensityDpi", "屏幕密度(dpi)", "屏幕信息") { "${it.screenDensityDpi} dpi" },
            InfoItem("screenOrientation", "屏幕方向", "屏幕信息") { it.screenOrientation },
            InfoItem("refreshRate", "屏幕刷新率", "屏幕信息") { it.refreshRate },

            // --- 网络信息 ---
            InfoItem("networkStatus", "连接状态", "网络信息") { it.networkStatus },
            InfoItem("networkType", "连接类型", "网络信息") { it.networkType },
            InfoItem("ipAddress", "本机 IP (IPv4)", "网络信息") { it.ipAddress },
            InfoItem("vpnStatus", "VPN 状态", "网络信息") { it.vpnStatus },

            // --- 系统功能支持 ---
            InfoItem("hasNfc", "NFC 支持", "系统功能支持") { if (it.hasFeature(PackageManager.FEATURE_NFC)) "支持" else "不支持" },
            InfoItem("hasBluetooth", "蓝牙支持", "系统功能支持") { if (it.hasFeature(PackageManager.FEATURE_BLUETOOTH)) "支持" else "不支持" },
            InfoItem("hasFingerprint", "指纹识别", "系统功能支持") { if (it.hasFeature(PackageManager.FEATURE_FINGERPRINT)) "支持" else "不支持" },
            InfoItem("hasGps", "GPS 支持", "系统功能支持") { if (it.hasFeature(PackageManager.FEATURE_LOCATION_GPS)) "支持" else "不支持" },

            // --- 区域与时间 ---
            InfoItem("language", "当前语言", "区域与时间") { Locale.getDefault().displayLanguage },
            InfoItem("timezone", "当前时区", "区域与时间") { TimeZone.getDefault().id },

            // --- 硬件状态信息 ---
            InfoItem("batteryLevel", "电池电量", "硬件状态信息") { it.batteryLevel },
            InfoItem("batteryState", "电池状态", "硬件状态信息") { it.batteryState },
            InfoItem("batteryTemp", "电池温度", "硬件状态信息") { it.batteryTemperature },

            // --- 存储与内存信息 ---
            InfoItem("ramTotal", "运行内存 (RAM) 总额", "存储信息") { it.ramTotal },
            InfoItem("ramAvailable", "运行内存 (RAM) 可用", "存储信息") { it.ramAvailable },
            InfoItem("ramUsage", "内存使用率", "存储信息") { it.ramUsageRate },
            InfoItem("storageTotal", "内部存储总容量", "存储信息") { it.storageTotal },
            InfoItem("storageAvailable", "内部存储可用容量", "存储信息") { it.storageAvailable },
            InfoItem("storageUsage", "内部存储使用率", "存储信息") { it.storageUsageRate },
            InfoItem("systemStorageTotal", "系统分区总容量", "存储信息") { it.systemStorageTotal },
            InfoItem("systemStorageAvailable", "系统分区可用容量", "存储信息") { it.systemStorageAvailable },
            InfoItem("externalStorageState", "外部存储状态", "存储信息") { it.externalStorageState },
            InfoItem("externalStorageTotal", "外部存储总容量", "存储信息") { it.externalStorageTotal },
            InfoItem("externalStorageAvailable", "外部存储可用容量", "存储信息") { it.externalStorageAvailable },
        )
    }

    // Lazy Services
    private val windowManager by lazy { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private val displayManager by lazy { context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
    private val activityManager by lazy { context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }
    private val connectivityManager by lazy { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }

    // Helpers
    fun hasFeature(feature: String): Boolean = context.packageManager.hasSystemFeature(feature)

    private val batteryIntent: Intent?
        get() = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    // Properties implementation
    val screenWidth: Int get() = windowManager.currentWindowMetrics.bounds.width()
    val screenHeight: Int get() = windowManager.currentWindowMetrics.bounds.height()
    val screenDensity: Float get() = context.resources.displayMetrics.density
    val screenDensityDpi: Int get() = context.resources.displayMetrics.densityDpi
    val screenSizeInch: String
        get() {
            val metrics = context.resources.displayMetrics
            val xdpi = if (metrics.xdpi > 0) metrics.xdpi else metrics.densityDpi.toFloat()
            val ydpi = if (metrics.ydpi > 0) metrics.ydpi else metrics.densityDpi.toFloat()
            val diagonal = sqrt((screenWidth / xdpi).pow(2) + (screenHeight / ydpi).pow(2))
            return String.format(Locale.getDefault(), "%.2f 英寸", diagonal)
        }

    val screenOrientation: String
        get() = when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> "纵向"
            Configuration.ORIENTATION_LANDSCAPE -> "横向"
            else -> "未知"
        }

    val screenResolution: String get() = "$screenWidth × $screenHeight"

    val refreshRate: String
        get() {
            val rate = displayManager.displays.firstOrNull()?.mode?.refreshRate ?: 0f
            return String.format(Locale.getDefault(), "%.2f Hz", rate)
        }

    // Network Info
    val networkStatus: String
        get() {
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            return if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) "已连接" else "未连接"
        }

    val networkType: String
        get() {
            val activeNetwork = connectivityManager.activeNetwork ?: return "无"
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return "未知"
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "移动数据"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "以太网"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                else -> "其他"
            }
        }

    val ipAddress: String
        get() {
            try {
                for (intf in Collections.list(NetworkInterface.getNetworkInterfaces())) {
                    for (addr in Collections.list(intf.inetAddresses)) {
                        if (!addr.isLoopbackAddress) {
                            val sAddr = addr.hostAddress ?: continue
                            if (sAddr.indexOf(':') < 0) return sAddr
                        }
                    }
                }
            } catch (e: Exception) {
                Logcat.e("SystemInfo", "获取 IP 地址失败", e)
            }
            return "未知"
        }

    val vpnStatus: String
        get() = if (connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) "已开启" else "未开启"

    // Battery Info
    val batteryLevel: String
        get() {
            val intent = batteryIntent ?: return "未知"
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            return if (level >= 0 && scale > 0) "${(level * 100 / scale.toFloat()).toInt()}%" else "未知"
        }

    val batteryState: String
        get() = when (batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "充电中"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
            BatteryManager.BATTERY_STATUS_FULL -> "已充满"
            else -> "未知"
        }

    val batteryTemperature: String
        get() = String.format(Locale.getDefault(), "%.1f ℃", (batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0f)

    // Storage & Memory
    val ramTotal: String get() {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return formatSize(memInfo.totalMem)
    }

    val ramAvailable: String get() {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return formatSize(memInfo.availMem)
    }

    val ramUsageRate: String get() {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val used = memInfo.totalMem - memInfo.availMem
        return String.format(Locale.getDefault(), "%.1f%%", (used.toDouble() / memInfo.totalMem) * 100)
    }

    val storageTotal: String get() = formatSize(StatFs(Environment.getDataDirectory().path).totalBytes)
    val storageAvailable: String get() = formatSize(StatFs(Environment.getDataDirectory().path).availableBytes)

    val storageUsageRate: String get() {
        val stat = StatFs(Environment.getDataDirectory().path)
        val used = stat.totalBytes - stat.availableBytes
        if (stat.totalBytes == 0L) return "0%"
        return String.format(Locale.getDefault(), "%.1f%%", (used.toDouble() / stat.totalBytes) * 100)
    }

    val systemStorageTotal: String get() = formatSize(StatFs(Environment.getRootDirectory().path).totalBytes)
    val systemStorageAvailable: String get() = formatSize(StatFs(Environment.getRootDirectory().path).availableBytes)

    val externalStorageState: String get() = when(Environment.getExternalStorageState()) {
        Environment.MEDIA_MOUNTED -> "已挂载 (可读写)"
        Environment.MEDIA_MOUNTED_READ_ONLY -> "已挂载 (只读)"
        else -> "未挂载/不可用"
    }

    val externalStorageTotal: String get() = try {
        formatSize(StatFs(Environment.getExternalStorageDirectory().path).totalBytes)
    } catch (e: Exception) {
        Logcat.e("SystemInfo", "获取外部存储总容量失败", e)
        "无法获取"
    }

    val externalStorageAvailable: String get() = try {
        formatSize(StatFs(Environment.getExternalStorageDirectory().path).availableBytes)
    } catch (e: Exception) {
        Logcat.e("SystemInfo", "获取外部存储可用容量失败", e)
        "无法获取"
    }

    val customOSName: String
        get() {
            val manufacturer = Build.MANUFACTURER.lowercase()
            val display = Build.DISPLAY.lowercase()
            return when {
                manufacturer.contains("xiaomi") || display.contains("miui") -> "MIUI / HyperOS"
                manufacturer.contains("huawei") || display.contains("emui") -> "EMUI / HarmonyOS"
                manufacturer.contains("oppo") || display.contains("coloros") -> "ColorOS"
                manufacturer.contains("vivo") -> "OriginOS / Funtouch"
                else -> display
            }
        }

    private fun formatSize(bytes: Long): String = String.format(Locale.getDefault(), "%.2f GB", bytes.toDouble() / (1024 * 1024 * 1024))
}
