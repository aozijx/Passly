package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.domain.model.icon.FaviconOutcome
import com.aozijx.passly.domain.model.icon.FaviconResult
import com.aozijx.passly.domain.repository.vault.FaviconRepository

/**
 * 核心图标获取用例
 * 策略：协调存储库执行“深度解析 (HTML) -> 默认路径 -> 持久化”的完整流程
 */
class DownloadFaviconUseCase(
    private val repository: FaviconRepository
) {
    /**
     * @param input 可以是完整的 URL，也可以是域名 (例如 "google.com")
     */
    suspend operator fun invoke(input: String): FaviconOutcome {
        if (input.isBlank()) {
            return FaviconOutcome(FaviconResult.EMPTY_INPUT)
        }
        
        // 委托给 Repository 执行具体的下载和解析逻辑
        // Repository 内部应调用 FaviconUtils.downloadAndSaveFavicon 来实现：
        // 1. 解析 HTML 中的 link 标签 (高清图标)
        // 2. 尝试常见的 /favicon.ico 路径
        // 3. 将结果保存到本地并返回路径
        return repository.downloadFavicon(input)
    }
}
