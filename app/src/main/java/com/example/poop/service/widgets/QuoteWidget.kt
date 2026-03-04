package com.example.poop.service.widgets

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
import androidx.glance.LocalSize
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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.example.poop.R

/**
 * 每日美句桌面小组件 - 最终优化版
 * 包含：透明遮罩、动态字体、点击切换、状态持久化
 */
class QuoteWidget : GlanceAppWidget() {

    companion object {
        val indexKey = intPreferencesKey("quote_index")
    }

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            QuoteWidgetContent()
        }
    }

    @Composable
    private fun QuoteWidgetContent() {
        val prefs = currentState<Preferences>()
        val currentIndex = prefs[indexKey] ?: 0
        val quotes = QuoteRepository.getQuotes()
        val quote = quotes[currentIndex % quotes.size]

        GlanceTheme {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ImageProvider(R.drawable.img))
                    .clickable(actionRunCallback<NextQuoteAction>())
            ) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.onPrimary)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isLarge = LocalSize.current.width > 200.dp

                    if (isLarge) {
                        Text(
                            text = "每日一言",
                            style = TextStyle(
                                color = GlanceTheme.colors.onPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(12.dp))
                    }

                    Text(
                        text = quote.text,
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimary,
                            fontSize = if (isLarge) 18.sp else 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        ),
                        modifier = GlanceModifier.fillMaxWidth()
                    )

                    if (isLarge) {
                        Spacer(modifier = GlanceModifier.height(20.dp))
                        Text(
                            text = "—— ${quote.source}",
                            style = TextStyle(
                                color = GlanceTheme.colors.onPrimary,
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic
                            )
                        )
                    }
                }
            }
        }
    }
}

// ====================== Action ======================
class NextQuoteAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[QuoteWidget.indexKey] ?: 0
            prefs[QuoteWidget.indexKey] = current + 1
        }
        QuoteWidget().update(context, glanceId)
    }
}

// ====================== Receiver ======================
class QuoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuoteWidget()
}

object QuoteRepository {
    data class Quote(val text: String, val source: String)

    fun getQuotes() = listOf(
        Quote("有花堪折直须折，莫待无花空折枝。", "《金缕衣》"),
        Quote("愿岁月可回首，且以深情共白头。", "冯唐"),
        Quote("心有猛虎，细嗅蔷薇。", "西格夫里·萨松"),
        Quote("人间有味是清欢。", "苏轼"),
        Quote("此心安处是吾乡。", "苏轼")
    )
}