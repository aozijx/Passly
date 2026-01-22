package com.example.poop.ui.screens.widgets

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.example.poop.R

// 数据类，包含句子和出处
data class Quote(val text: String, val source: String)

// 美句列表
val beautySentences = listOf(
    Quote("有花堪折直须折，莫待无花空折枝。", "《金缕衣》"),
    Quote("愿岁月可回首，且以深情共白头。", "冯唐"),
    Quote("心有猛虎，细嗅蔷薇。", "西格夫里·萨松"),
    Quote("人间有味是清欢。", "苏轼《浣溪沙·细雨斜风作晓寒》"),
    Quote("此心安处是吾乡。", "苏轼《定风波·南海归赠王定国侍人寓娘》")
)

class BeautySentenceWidget : GlanceAppWidget() {
    companion object {
        val indexKey = intPreferencesKey("index")
    }

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
        val quote = beautySentences[currentIndex % beautySentences.size]

        GlanceTheme {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(
                        ImageProvider(R.drawable.img) // 使用一个占位图
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "每日一言",
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.fillMaxWidth()
                )

                // Use Spacer with weight to push content to center
                Spacer(modifier = GlanceModifier.defaultWeight())

                Text(
                    text = quote.text,
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    ),
                    modifier = GlanceModifier.fillMaxWidth()
                )

                // Use Spacer with weight to push footer to bottom
                Spacer(modifier = GlanceModifier.defaultWeight())

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "—— ${quote.source}",
                        style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 12.sp),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Box(modifier = GlanceModifier.clickable(actionRunCallback<UpdateSentenceAction>())) {
                         Text("↻", style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 20.sp))
                    }
                }
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
        updateAppWidgetState(context, glanceId) { prefs ->
            val currentIndex = prefs[BeautySentenceWidget.indexKey] ?: 0
            prefs[BeautySentenceWidget.indexKey] = (currentIndex + 1) % beautySentences.size
        }
        BeautySentenceWidget().update(context, glanceId)
    }
}

class BeautySentenceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BeautySentenceWidget()
}
