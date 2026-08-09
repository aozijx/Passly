# Compose 共享元素与阴影裁剪

状态：当前实现与排障约定。

## 结论

Passly 使用 `SharedTransitionLayout` 作为共享元素动画的基础设施。它在动画期间把匹配的元素绘制到
`SharedTransitionScope` overlay，使元素脱离目的地页面的淡入淡出和普通父容器裁剪。

阴影扩边不是 `SharedTransitionLayout` 的替代方案。只有 elevation、blur 或自定义绘制越过共享元素自身布局边界，
并且实际出现裁剪时，才叠加 `withSharedTransitionVisualOverflow`。项目保留以下两级方案：

| 方案                                                            | 适用情况                              | 默认选择 |
|---------------------------------------------------------------|-----------------------------------|------|
| A. 官方 `SharedTransitionLayout` + `sharedElement/sharedBounds` | 图片、文字、无外溢绘制的 Surface，以及仅受父容器裁剪的元素 | 是    |
| B. 方案 A + visual overflow                                     | 阴影、blur、glow 等绘制超出元素自身边界且仍被裁剪     | 按需   |

## 根容器边界

共享动画容器保持在路由内容外层：

```text
App Shell
└─ content
   └─ SharedTransitionLayout
      └─ NavHost
         └─ destination
```

底部导航、Navigation Rail、全局 Snackbar 和认证遮罩不进入共享动画容器，避免共享内容在动画期间覆盖固定
App Chrome。详细宿主结构见 [UI 宿主、导航与命名](../architecture/ui-shell-and-naming.md)。

## 方案 A：官方 overlay

普通共享元素直接使用官方 API：

```kotlin
SharedTransitionLayout {
    NavHost(/* ... */) {
        composable(/* ... */) {
            Surface(
                modifier = Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(key),
                    animatedVisibilityScope = this@composable
                )
            ) {
                // content
            }
        }
    }
}
```

约束：

- 两端必须位于同一个 `SharedTransitionScope`，并使用同一个稳定 key。
- 两端必须把共享 modifier 绑定到相同语义层级；不能一端绑定图标，另一端绑定整个按钮或 Card。
- 相同内容优先使用 `sharedElement`；两端视觉结构不同但边界连续时使用 `sharedBounds`。
- 需要参与动画的 `shadow`、`background`、`clip` 应位于共享 modifier 之后，成为共享层的子绘制内容。
- `clipInOverlayDuringTransition` 默认继承父 `sharedBounds` 的裁剪；没有父共享边界时默认不裁剪。
- 仅存在兄弟元素遮挡时，使用 `zIndexInOverlay`；不要用扩大布局边界解决层级问题。

官方资料：

- [Shared element transitions in Compose](https://developer.android.com/develop/ui/compose/animation/shared-elements)
- [SharedTransitionScope API](https://developer.android.com/reference/kotlin/androidx/compose/animation/SharedTransitionScope)
- [OverlayClip API](https://developer.android.com/reference/kotlin/androidx/compose/animation/SharedTransitionScope.OverlayClip)

## 方案 B：为外溢绘制预留共享边界

`OverlayClip` 只决定 overlay 是否应用裁剪路径，不会扩大共享 RenderNode 自身的尺寸。元素阴影仍在自身边界被
截断时，使用 `core/ui/animation/SharedTransitionVisualOverflow.kt`：

```kotlin
val sharedModifier = with(sharedTransitionScope) {
    Modifier.sharedBounds(
        sharedContentState = rememberSharedContentState(key),
        animatedVisibilityScope = animatedVisibilityScope,
        clipInOverlayDuringTransition = SharedTransitionOverlayClip.None
    )
}

Surface(
    modifier = Modifier.withSharedTransitionVisualOverflow(
        sharedModifier = sharedModifier,
        visualOverflow = 12.dp
    ),
    shadowElevation = 6.dp
) {
    // content
}
```

该扩展采用固定顺序：

```text
补偿布局 → 共享图层 → 透明外溢空间 → 实际组件及阴影
```

透明空间会进入共享图层，但补偿布局继续向 `Scaffold`、Row 或 Column 报告组件原始尺寸，因此不会改变组件最终
位置。动画两端必须采用相同的共享策略；`visualOverflow` 至少覆盖两端最大的阴影、模糊半径和绘制偏移。

经验值只能作为起点：

| 视觉效果            | 建议检查值                |
|-----------------|----------------------|
| 2–4dp elevation | 8–12dp               |
| 6–8dp elevation | 12–16dp              |
| 自定义 blur/glow   | blur radius + 最大方向偏移 |

不应给全部共享元素统一添加较大的 overflow。它会扩大过渡期图层面积和重绘区域，应由需要外溢绘制的组件显式
选择。

## 常见错误

### 两端共享层级不同

错误示例是入口页给 24dp 图标添加 `sharedBounds`，目标页却给整个扩展 FAB 添加。动画边界、阴影和点击区域均不
连续。共享 modifier 应绑定到两端的完整按钮容器。

### 把裁剪放在共享层外侧

父级 `clip`、`graphicsLayer(clip = true)` 或 Pager/Lazy 容器可能裁剪未进入 overlay 的内容。先确认共享元素确实
由 `SharedTransitionLayout` 管理，并保持形状裁剪属于共享层内部。

### 把遮挡误判为裁剪

如果阴影完整但被另一个共享元素或固定 UI 覆盖，应调整 `zIndexInOverlay` 或 App Shell 边界。visual
overflow
只增加可绘制区域，不改变绘制顺序。

### 使用不同的扩边值

两端 overflow 不一致会让共享边界本身在动画开始或结束时跳变。统一使用组件级 token，例如当前新增条目 FAB
的
`AddEntryFabVisualOverflow`。

## 验收清单

- 正向导航和返回导航都没有阴影截断或矩形硬边。
- 动画开始、结束时组件位置不跳动。
- FAB、Card 的点击区域没有因为透明扩边而扩大。
- 动画两端使用相同 key、相同语义容器和相同 overflow。
- 深色、浅色和动态配色下都检查阴影；浅色背景更容易暴露硬裁剪边缘。
- 若存在底栏或认证遮罩，确认共享元素没有越过 App Shell 的固定层。

## 相关实现

- `app/navigation/PasslyNavHost.kt`：`SharedTransitionLayout` 与 `NavHost` 宿主。
- `core/ui/animation/SharedTransitionVisualOverflow.kt`：无裁剪策略和通用 visual overflow。
- `feature/vault/components/fab/VaultFab.kt`：入口 FAB。
- `feature/vault/editor/common/AddEntryScaffold.kt`：目标保存 FAB。
