package com.example.poop.ui.screens.detail

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.view.WindowManager
import java.util.Locale
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
        @SuppressLint("DefaultLocale")
        val INFO_ITEMS = listOf(
            // --- 系统与设备信息 ---
            InfoItem("deviceManufacturer", "设备制造商", "系统与设备信息") { Build.MANUFACTURER },
            InfoItem("deviceBrand", "设备品牌", "系统与设备信息") { Build.BRAND },
            InfoItem("deviceModel", "设备型号", "系统与设备信息") { Build.MODEL },
            InfoItem("deviceProduct", "产品名称", "系统与设备信息") { Build.PRODUCT },
            InfoItem("systemName", "系统", "系统与设备信息") { it.customOSName },
            InfoItem("androidVersion", "Android 版本", "系统与设备信息") { Build.VERSION.RELEASE },
            InfoItem("sdkVersion", "SDK 版本", "系统与设备信息") { Build.VERSION.SDK_INT.toString() },
            InfoItem("cpu", "CPU", "系统与设备信息") { Build.HARDWARE },
            InfoItem("deviceBoard", "主板", "系统与设备信息") { Build.BOARD },
            InfoItem("deviceDevice", "设备代号", "系统与设备信息") { Build.DEVICE },
            InfoItem("systemDisplay", "系统显示版本", "系统与设备信息") { Build.DISPLAY },

            // --- 屏幕信息 ---
            InfoItem("screenWidth", "屏幕宽度", "屏幕信息") { "${it.screenWidth} px" },
            InfoItem("screenHeight", "屏幕高度", "屏幕信息") { "${it.screenHeight} px" },
            InfoItem("screenDensity", "屏幕密度", "屏幕信息") { String.format("%.2f", it.screenDensity) },
            InfoItem("screenDensityDpi", "屏幕密度(dpi)", "屏幕信息") { "${it.screenDensityDpi} dpi" },
            InfoItem("screenSizeInch", "屏幕尺寸", "屏幕信息") { it.screenSizeInch },
            InfoItem("screenOrientation", "屏幕方向", "屏幕信息") { it.screenOrientation },
            InfoItem("screenResolution", "屏幕分辨率", "屏幕信息") { it.screenResolution },
            InfoItem("refreshRate", "屏幕刷新率", "屏幕信息") { it.refreshRate },
            InfoItem("screenAspectRatio", "屏幕宽高比", "屏幕信息") { it.aspectRatio },
            InfoItem("screenCutout", "屏幕刘海", "屏幕信息") { it.cutout },
            InfoItem("statusBarHeight", "状态栏高度", "屏幕信息") { "${it.statusBarHeight} px" },
            InfoItem("navigationBarHeight", "导航栏高度", "屏幕信息") { "${it.navigationBarHeight} px" },

            // --- 硬件状态信息 ---
            InfoItem("batteryLevel", "电池电量", "硬件状态信息") { it.batteryLevel },
            InfoItem("batteryState", "电池状态", "硬件状态信息") { it.batteryState },
            InfoItem("batteryVoltage", "电池电压", "硬件状态信息") { it.batteryVoltage },
            InfoItem("chargeMode", "充电方式", "硬件状态信息") { it.chargeMode },
            InfoItem("batteryTemp", "电池温度", "硬件状态信息") { it.batteryTemperature },

            // --- 存储与内存信息 ---
            InfoItem("memoryOverview", "内存概览", "存储信息") { it.memoryOverview },
            InfoItem("storageTotal", "内部存储总容量", "存储信息") { it.storageTotal },
            InfoItem("storageAvailable", "内部存储可用容量", "存储信息") { it.storageAvailable },
        )
    }

    // Lazy Services
    private val windowManager by lazy { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private val displayManager by lazy { context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
    private val activityManager by lazy { context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }

    // Helpers: 获取 Sticky Intent 
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
            // 使用 xdpi 和 ydpi 获取更精确的物理尺寸, 如果获取失败（0），则降级使用 densityDpi
            val xdpi = if (metrics.xdpi > 0) metrics.xdpi else metrics.densityDpi.toFloat()
            val ydpi = if (metrics.ydpi > 0) metrics.ydpi else metrics.densityDpi.toFloat()

            // 计算物理宽度和高度（英寸）
            val widthInches = screenWidth / xdpi
            val heightInches = screenHeight / ydpi

            // 计算对角线英寸数
            val diagonal = sqrt(widthInches.pow(2) + heightInches.pow(2))

            return String.format(Locale.getDefault(), "%.2f 英寸", diagonal)
        }

    val screenOrientation: String
        get() = when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> "纵向"
            Configuration.ORIENTATION_LANDSCAPE -> "横向"
            else -> "未知"
        }

    val screenResolution: String
        get() = "$screenWidth × $screenHeight"

    val refreshRate: String
        get() {
            val rate = displayManager.displays.firstOrNull()?.mode?.refreshRate ?: 0f
            return String.format(Locale.getDefault(), "%.2f Hz", rate)
        }

    val aspectRatio: String
        get() {
            val w = screenWidth
            val h = screenHeight
            val g = gcd(w, h)
            return "${w / g}:${h / g}"
        }

    val cutout: String
        get() = if (windowManager.currentWindowMetrics.windowInsets.displayCutout != null) "存在刘海" else "无刘海"

    val statusBarHeight: Int
        @SuppressLint("InternalInsetResource", "DiscouragedApi")
        get() {
            val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            return if (resId > 0) context.resources.getDimensionPixelSize(resId) else 0
        }

    val navigationBarHeight: Int
        @SuppressLint("InternalInsetResource", "DiscouragedApi")
        get() {
            val resId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
            return if (resId > 0) context.resources.getDimensionPixelSize(resId) else 0
        }

    // Battery Info
    val batteryLevel: String
        get() {
            val intent = batteryIntent ?: return "未知"
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            return if (level >= 0 && scale > 0) {
                "${(level * 100 / scale.toFloat()).toInt()}%"
            } else {
                "未知"
            }
        }

    val batteryState: String
        get() {
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            return when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "充电中"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
                BatteryManager.BATTERY_STATUS_FULL -> "已充满"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "未充电"
                else -> "未知状态"
            }
        }

    val batteryVoltage: String
        get() {
            val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
            return "$voltage mV"
        }

    val batteryTemperature: String
        get() {
            val temp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            return String.format(Locale.getDefault(), "%.1f ℃", temp / 10.0f)
        }

    val chargeMode: String
        get() {
            val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
            return when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC 充电"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB 充电"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "无线充电"
                else -> if (batteryState == "充电中") "未知充电方式" else "未充电"
            }
        }

    // Storage & Memory
    val storageTotal: String
        get() = formatSize(StatFs(Environment.getDataDirectory().path).totalBytes)

    val storageAvailable: String
        get() = formatSize(StatFs(Environment.getDataDirectory().path).availableBytes)

    val memoryOverview: String
        get() {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            val total = formatSize(memInfo.totalMem)
            val avail = formatSize(memInfo.availMem)
            val lowMem = if (memInfo.lowMemory) "(低内存)" else ""
            return "可用: $avail / 总计: $total $lowMem"
        }

    val customOSName: String
        get() {
            // 1. 先获取制造商和品牌
            val manufacturer = Build.MANUFACTURER.lowercase()
            val brand = Build.BRAND.lowercase()
            val display = Build.DISPLAY.lowercase()

            return when {
                // 小米/红米
                manufacturer.contains("xiaomi") || brand.contains("xiaomi") || display.contains("miui") -> "MIUI"
                // 华为
                manufacturer.contains("huawei") || brand.contains("huawei") || display.contains("emui") -> "EMUI"
                // OPPO
                manufacturer.contains("oppo") || brand.contains("oppo") || display.contains("coloros") -> "ColorOS"
                // vivo/iQOO
                manufacturer.contains("vivo") || brand.contains("vivo") || display.contains("funtouch") -> "Funtouch OS"
                // 一加
                manufacturer.contains("oneplus") || brand.contains("oneplus") -> "OxygenOS"
                // 三星
                manufacturer.contains("samsung") || brand.contains("samsung") || display.contains("oneui") -> "One UI"
                // 魅族
                manufacturer.contains("meizu") || brand.contains("meizu") || display.contains("flyme") -> "Flyme OS"
                // 其他情况...
                else -> display // 兜底
            }
        }

    // Utilities
    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

    private fun formatSize(bytes: Long): String {
        val gb = bytes.toDouble() / (1024 * 1024 * 1024)
        return String.format(Locale.getDefault(), "%.2f GB", gb)
    }
}
