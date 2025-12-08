package com.example.poop.ui.screens.detail

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.view.WindowManager
import java.util.Locale
import kotlin.math.sqrt

class SystemInfoManager(private val context: Context) {
    // 统一的信息项定义
    data class InfoItem(
        val key: String,
        val title: String,
        val category: String,
        val valueProvider: (SystemInfoManager) -> String
    )

    // 键名
    companion object {
        val INFO_ITEMS = listOf(
            // 系统与设备信息
            InfoItem("deviceManufacturer", "设备制造商", "系统与设备信息") { Build.MANUFACTURER },
            InfoItem("deviceBrand", "设备品牌", "系统与设备信息") { Build.BRAND },
            InfoItem("deviceModel", "设备型号", "系统与设备信息") { Build.MODEL },
            InfoItem("deviceProduct", "产品名称", "系统与设备信息") { Build.PRODUCT },
            InfoItem("systemName", "系统", "系统与设备信息") { it.getCustomOSName() },
            InfoItem("systemName", "系统", "系统与设备信息") { Build.VERSION.BASE_OS },
            InfoItem("cpu", "CPU", "系统与设备信息") { Build.HARDWARE },
            InfoItem("deviceBoard", "主板", "系统与设备信息") { Build.BOARD },
            InfoItem("", "设备代号", "系统与设备信息") { Build.DEVICE },
            InfoItem("", "系统显示版本", "系统与设备信息") { Build.DISPLAY },
            InfoItem(
                "androidVersion", "Android 版本", "系统与设备信息"
            ) { Build.VERSION.RELEASE },
            InfoItem(
                "sdkVersion", "SDK 版本", "系统与设备信息"
            ) { Build.VERSION.SDK_INT.toString() },


            // 屏幕信息
            InfoItem("screenWidth", "屏幕宽度", "屏幕信息") { "${it.screenWidth} px" },
            InfoItem("screenHeight", "屏幕高度", "屏幕信息") { "${it.screenHeight} px" },
            InfoItem("screenDensity", "屏幕密度", "屏幕信息") { it.screenDensity.toString() },
            InfoItem(
                "screenDensityDpi",
                "屏幕密度(dpi)",
                "屏幕信息"
            ) { "${it.screenDensityDpi} dpi" },
            InfoItem("screenSizeInch", "屏幕尺寸", "屏幕信息") { it.screenSizeInch },
            InfoItem(
                "screenOrientation",
                "屏幕方向",
                "屏幕信息"
            ) { it.screenOrientation },
            InfoItem("screenResolution", "屏幕分辨率", "屏幕信息") { it.screenResolution },
            InfoItem("refreshRate", "屏幕刷新率", "屏幕信息") { it.refreshRate },
            InfoItem("screenAspectRatio", "屏幕宽高比", "屏幕信息") { it.aspectRatio },
            InfoItem("screenCutout", "屏幕刘海", "屏幕信息") { it.cutout },
            InfoItem("statusBarHeight", "状态栏高度", "屏幕信息") { "${it.statusBarHeight} px" },
            InfoItem(
                "navigationBarHeight",
                "导航栏高度",
                "屏幕信息"
            ) { "${it.navigationBarHeight} px" },
            InfoItem("batteryLevel", "电池电量", "硬件状态信息") { it.batteryLevel },
            InfoItem("batteryState", "电池状态", "硬件状态信息") { it.batteryState },
            InfoItem("batteryV", "电池电压", "硬件状态信息") { it.batteryState },
            InfoItem("chargeMode", "充电方式", "硬件状态信息") { it.chargeMode },
            InfoItem("batteryTemp", "充电温度", "硬件状态信息") { it.batteryTemplate },
            InfoItem("storageTotal", "内部存储总容量", "存储信息") { it.storageTotal },
            InfoItem("storageAvailable", "内部存储可用容量", "存储信息") { it.storageAvailable },
            InfoItem("totalCaptureResult", "总内存", "存储信息") { it.totalCaptureResult },
        )
    }

    private val windowManager by lazy { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private val displayManager by lazy { context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
    private val batteryManager by lazy { context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager }
    private val activityManager by lazy { context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }


    val screenWidth: Int get() = windowManager.currentWindowMetrics.bounds.width()
    val screenHeight: Int get() = windowManager.currentWindowMetrics.bounds.height()
    val screenDensity: Float get() = context.resources.displayMetrics.density
    val screenDensityDpi: Int get() = context.resources.displayMetrics.densityDpi
    val screenSizeInch: String
        get() = String.format(
            Locale.getDefault(), "%.2f 英寸", calculateScreenSizeInch()
        )
    val screenOrientation: String
        get() = when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> "纵向"
            Configuration.ORIENTATION_LANDSCAPE -> "横向"
            else -> "未知方向"
        }
    val screenResolution: String
        get() {
            val (screenWidth, screenHeight) = run {
                val windowMetrics = windowManager.currentWindowMetrics
                val bounds: Rect = windowMetrics.bounds
                Pair(bounds.width(), bounds.height())
            }
            return "$screenWidth × $screenHeight"
        }
    val refreshRate: String
        get() = String.format(
            Locale.getDefault(),
            "%.2f Hz",
            getRefreshRate()
        )
    val aspectRatio: String
        get() = "${
            screenWidth / gcd(
                screenWidth,
                screenHeight
            )
        }:${screenHeight / gcd(screenWidth, screenHeight)}"
    val cutout: String
        get() {
            val windowInsets = windowManager.currentWindowMetrics.windowInsets
            val displayCutout = windowInsets.displayCutout
            return if (displayCutout != null) {
                "存在刘海"
            } else {
                "无刘海"
            }
        }
    val statusBarHeight: Int
        @SuppressLint("InternalInsetResource")
        get() {
            val resourceId =
                context.resources.getIdentifier("status_bar_height", "dimen", "android")
            return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
        }
    val navigationBarHeight: Int
        @SuppressLint("InternalInsetResource")
        get() {
            val resourceId =
                context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
            return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
        }
    val batteryLevel: String
        get() {
            return "${batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)}%"
        }
    val batteryState: String
        get() {
            val status =
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            return when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "充电中"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
                BatteryManager.BATTERY_STATUS_FULL -> "已充满"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "未充电"
                else -> "未知状态"
            }
        }
    val chargeMode: String
        @SuppressLint("ServiceCast")
        get() {
            // 方式 1：通过电池广播获取充电类型（官方推荐，API 1+ 兼容）
            val batteryIntent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            ) ?: return "未知充电状态"

            // 获取充电类型（EXTRA_PLUGGED 是官方正确字段）
            val chargePlug = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

            // 判断充电模式
            return when (chargePlug) {
                BatteryManager.BATTERY_PLUGGED_AC -> "有线充电"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB 充电（电脑/充电宝）"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "无线充电"
                // 处理混合充电（部分设备支持）
                BatteryManager.BATTERY_PLUGGED_AC or BatteryManager.BATTERY_PLUGGED_WIRELESS -> "有线+无线充电"
                BatteryManager.BATTERY_PLUGGED_USB or BatteryManager.BATTERY_PLUGGED_WIRELESS -> "USB+无线充电"
                // 未充电场景
                else -> "未充电"
            }
        }
    val batteryTemplate: String
        get() {
            // 获取电池温度（原始值单位：0.1℃，除以 10 转为摄氏度）
            val batteryTempRaw =
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            // 处理无效值（如返回 -1 表示获取失败）

            // 格式化保留 1 位小数，适配用户本地格式
            return String.format(
                Locale.getDefault(),
                "%.1f ℃",
                batteryTempRaw / 10.0
            )

        }
    val storageTotal: String
        get() {
            val internalStorage = Environment.getDataDirectory()
            val statFs = StatFs(internalStorage.path)

            // 适配 API 18+ 的长整型方法，避免溢出
            val blockSize =
                statFs.blockSizeLong
            val totalBlocks =
                statFs.blockCountLong

            // 总容量（字节）→ 转换为 GB
            val totalSpaceGB = (totalBlocks * blockSize).toDouble() / (1024.0 * 1024.0 * 1024.0)

            // 正确的 String.format 写法：格式字符串在前，参数在后
            return String.format(Locale.getDefault(), "%.2f GB", totalSpaceGB)
        }
    val storageAvailable: String
        get() {
            val internalStorage = Environment.getDataDirectory()
            val statFs = StatFs(internalStorage.path)

            val blockSize =
                statFs.blockSizeLong
            val availableBlocks =
                statFs.availableBlocksLong

            val availableSpaceGB =
                (availableBlocks * blockSize).toDouble() / (1024.0 * 1024.0 * 1024.0)
            return String.format(Locale.getDefault(), "%.2f GB", availableSpaceGB)
        }

    val totalCaptureResult: String
        get() {
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)

            // 1. 转换内存单位（字节 → GB/MB，保留 2 位小数）
            val totalRamGB = memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
            val availableRamGB = memoryInfo.availMem / (1024.0 * 1024.0 * 1024.0)
            val usedRamGB = totalRamGB - availableRamGB

            // 2. 低内存状态描述
            val lowMemoryStatus = if (memoryInfo.lowMemory) "是" else "否"

            // 3. 格式化最终字符串
            return String.format(
                Locale.getDefault(),
                "总内存：%.2f GB\n可用内存：%.2f GB\n已用内存：%.2f GB\n低内存状态：%s",
                totalRamGB,
                availableRamGB,
                usedRamGB,
                lowMemoryStatus
            )
        }

    fun getCustomOSName(): String {
        val display = Build.DISPLAY.lowercase()
        return when {
            display.contains("miui") -> "MIUI"
            display.contains("emui") -> "EMUI"
            display.contains("coloros") -> "ColorOS"
            display.contains("funtouchos") -> "Funtouch OS"
            display.contains("hydrogenos") -> "HydrogenOS"
            display.contains("oxygenos") -> "OxygenOS"
            display.contains("oneui") -> "One UI" // 三星定制系统
            display.contains("flyme") -> "Flyme OS" // 魅族
            else -> "Android（原生/未知定制系统）"
        }
    }

    private fun calculateScreenSizeInch(): Double {
        // 屏幕物理宽高（px）
        val screenWidthPx = screenWidth
        val screenHeightPx = screenHeight

        // 计算对角线像素长度
        val diagonalPx =
            sqrt((screenWidthPx * screenWidthPx + screenHeightPx * screenHeightPx).toDouble())

        // 屏幕英寸 = 对角线像素 / DPI
        return diagonalPx / screenDensityDpi
    }

    /**
     * 获取屏幕刷新率（完全移除 defaultDisplay，适配 API 31+）
     * @return 屏幕刷新率（Hz），异常/无效时返回 0.0f
     */
    private fun getRefreshRate(): Float {
        // 直接使用 DisplayManager 获取的主屏幕 Display 实例
        val refreshRate = displayManager.displays.firstOrNull()?.mode?.refreshRate
        // 兜底：确保返回有效值（排除 0 或负数）
        return refreshRate.takeIf { it!! > 0 } ?: 0.0f
    }

    /** 计算最大公约数（用于计算宽高比） */
    private fun gcd(a: Int, b: Int): Int {
        return if (b == 0) a else gcd(b, a % b)
    }
}

//// 传感器数据
//// 安卓提供多种内置传感器（加速度计、陀螺仪、光线传感器、重力传感器等），通过 SensorManager 获取，需申请权限 android.permission.ACCESS_FINE_LOCATION（部分传感器如气压计无需权限，位置相关传感器需要）。
//
//// 网络 / 连接信息
//// 网络状态
//// 通过 ConnectivityManager 获取网络类型（WiFi / 移动数据），需权限 android.permission.ACCESS_NETWORK_STATE：
//
//// WiFi 信息
//// 需权限 android.permission.ACCESS_WIFI_STATE、android.permission.CHANGE_WIFI_STATE：
//val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
//val isWifiEnabled = wifiManager.isWifiEnabled // WiFi是否开启
//val connectedWifiInfo = wifiManager.connectionInfo
//val ssid = connectedWifiInfo.ssid // 连接的WiFi名称（需注意：Android 10+ 需定位权限才能获取SSID）
//val ipAddress = Formatter.formatIpAddress(connectedWifiInfo.ipAddress) // IP地址
