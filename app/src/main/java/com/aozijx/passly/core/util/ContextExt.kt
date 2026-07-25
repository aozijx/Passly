package com.aozijx.passly.core.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Process.killProcess
import android.os.Process.myPid

/**
 * 安全重启应用（推荐用于语言/主题切换后）
 * @param activity 当前 Activity（用于 finish，如果 context 不是 Activity 则传入）
 */
fun Context.restartApp(activity: Activity? = null) {
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    if (intent != null) {
        val restartIntent = Intent.makeRestartActivityTask(intent.component)
        startActivity(restartIntent)
        // 结束当前 Activity
        (this as? Activity)?.finish() ?: activity?.finish()
    } else {
        // 极端降级方案（一般不会触发）
        killProcess(myPid())
    }
}