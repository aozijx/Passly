package com.example.poop.ui.screens.widgets

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

// 美句列表
val beautySentences = listOf(
    "有花堪折直须折，莫待无花空折枝。",
    "愿岁月可回首，且以深情共白头。",
    "岁月不饶人，我亦未曾饶过岁月。",
    "凌晨四点醒来，发现海棠花未眠。",
    "一生温暖纯良，不舍爱与自由。",
    "我有一瓢酒，可以慰风尘。",
    "人生如逆旅，我亦是行人。",
    "世界微尘里，吾宁爱与憎。",
    "心有猛虎，细嗅蔷薇。",
    "人间有味是清欢。",
    "此心安处是吾乡。"
)

class BeautySentenceWidget : GlanceAppWidget() {

    companion object {
        const val INDEX_KEY = "index"
        val indexKey = intPreferencesKey(INDEX_KEY)
    }

    // 指定状态定义
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Content()
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    fun Content() {
        val prefs = currentState<Preferences>()
        val currentIndex = prefs[indexKey] ?: 0
        val sentence = beautySentences[currentIndex % beautySentences.size]

        GlanceTheme {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
                    .padding(16.dp)
                    .clickable(actionRunCallback<UpdateSentenceAction>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sentence,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF333333)),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

// 创建 Action 类来处理点击事件
class UpdateSentenceAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // 更新状态
        updateAppWidgetState(context, glanceId) { prefs ->
            val currentIndex = prefs[BeautySentenceWidget.indexKey] ?: 0
            val newIndex = (currentIndex + 1) % beautySentences.size
            prefs.toMutablePreferences()[BeautySentenceWidget.indexKey] = newIndex
        }

        // 更新小部件
        BeautySentenceWidget().update(context, glanceId)
    }
}

class BeautySentenceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BeautySentenceWidget()
}