package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow

class GetCategoriesUseCase(private val repository: VaultRepository) {
    operator fun invoke(): Flow<List<String>> = repository.allCategories
}
