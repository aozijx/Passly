package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.domain.repository.vault.VaultSearchRepository
import kotlinx.coroutines.flow.Flow

class GetCategoriesUseCase(private val repository: VaultSearchRepository) {
    operator fun invoke(): Flow<List<String>> = repository.allCategories
}
