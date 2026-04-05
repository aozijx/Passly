package com.aozijx.passly.domain.usecase.vault

import com.aozijx.passly.domain.repository.FaviconRepository
import com.aozijx.passly.domain.repository.VaultRepository
import com.aozijx.passly.domain.usecase.vault.impl.DeleteAllEntriesUseCase
import com.aozijx.passly.domain.usecase.vault.impl.DeleteEntryUseCase
import com.aozijx.passly.domain.usecase.vault.impl.DownloadFaviconUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetCategoriesUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetEntriesByCategoryUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetEntryByIdUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetEntrySummariesByCategoryUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetHistoryByEntryIdUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetTotpCodeUseCase
import com.aozijx.passly.domain.usecase.vault.impl.InsertEntryUseCase
import com.aozijx.passly.domain.usecase.vault.impl.ObserveAllEntriesUseCase
import com.aozijx.passly.domain.usecase.vault.impl.ObserveAllEntrySummariesUseCase
import com.aozijx.passly.domain.usecase.vault.impl.SearchEntriesUseCase
import com.aozijx.passly.domain.usecase.vault.impl.SearchEntrySummariesUseCase
import com.aozijx.passly.domain.usecase.vault.impl.UpdateEntryUseCase

class VaultUseCases(
    repository: VaultRepository,
    faviconRepository: FaviconRepository
) {
    val observeAllEntries = ObserveAllEntriesUseCase(repository)
    val observeAllEntrySummaries = ObserveAllEntrySummariesUseCase(repository)
    val getEntriesByCategory = GetEntriesByCategoryUseCase(repository)
    val getEntrySummariesByCategory = GetEntrySummariesByCategoryUseCase(repository)
    val searchEntries = SearchEntriesUseCase(repository)
    val searchEntrySummaries = SearchEntrySummariesUseCase(repository)
    val getCategories = GetCategoriesUseCase(repository)
    val getHistoryByEntryId = GetHistoryByEntryIdUseCase(repository)
    val getEntryById = GetEntryByIdUseCase(repository)
    val insertEntry = InsertEntryUseCase(repository)
    val updateEntry = UpdateEntryUseCase(repository)
    val deleteEntry = DeleteEntryUseCase(repository)
    val deleteAllEntries = DeleteAllEntriesUseCase(repository)
    val getTotpCode = GetTotpCodeUseCase()
    val downloadFavicon = DownloadFaviconUseCase(faviconRepository)
}
