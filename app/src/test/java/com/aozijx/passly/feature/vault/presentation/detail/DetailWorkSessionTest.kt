package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.policy.EntryEdit
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.domain.sensitive.OwnedChars
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DetailWorkSessionTest {
    @Test
    fun expiredTokenCannotStartAnotherWrite() = runTest {
        val fixture = Fixture()
        val old = fixture.session.open(ID)
        fixture.session.close()
        fixture.session.open(ID)
        fixture.save(old)
        assertEquals(0, fixture.editCalls)
        assertTrue(fixture.mutations.isEmpty())
    }

    @Test
    fun staleRevealBuffersAreWipedWithoutPublication() = runTest {
        val fixture = Fixture()
        val token = fixture.session.open(ID)
        val value = OwnedChars.fromString("synthetic")
        fixture.session.close()
        fixture.session.publishReveals(token, listOf(FieldKey.PASSWORD to value)) { _, _ -> fail("Stale publication") }
        assertTrue(value.isEmpty)
    }

    @Test
    fun closingDuringPublicationWipesRemainingValues() = runTest {
        val fixture = Fixture()
        val token = fixture.session.open(ID)
        val first = OwnedChars.fromString("first")
        val second = OwnedChars.fromString("second")
        var published = 0
        try {
            fixture.session.publishReveals(token, listOf(1 to first, 2 to second)) { _, _ ->
                published++
                fixture.session.close()
            }
            assertEquals(1, published)
            assertTrue(second.isEmpty)
        } finally { first.wipe(); second.wipe() }
    }

    @Test
    fun publisherFailureWipesBuffersAndPropagatesTheOriginalFailure() = runTest {
        val fixture = Fixture()
        val token = fixture.session.open(ID)
        val values = listOf(OwnedChars.fromString("one"), OwnedChars.fromString("two"))
        val expected = IllegalStateException("Publisher failed")
        try {
            fixture.session.publishReveals(token, values.withIndex().map { it.index to it.value }) { _, _ -> throw expected }
            fail("Expected publisher failure")
        } catch (actual: IllegalStateException) {
            assertSame(expected, actual)
            assertTrue(values.all { it.isEmpty })
        } finally { values.forEach(SensitiveValue::wipe) }
    }
    @Test
    fun closedSessionIgnoresAnEditThatCompletesLater() = runTest {
        val gate = CompletableDeferred<Unit>()
        val fixture = Fixture(beforeEdit = { gate.await() })
        val token = fixture.session.open(ID)
        launch { fixture.save(token) }
        runCurrent()
        fixture.session.close()
        gate.complete(Unit)
        runCurrent()
        try {
            assertTrue(fixture.mutations.isEmpty())
            assertTrue(fixture.reveals.isEmpty())
            assertEquals(0, fixture.presentCalls)
        } finally { fixture.wipe() }
    }

    @Test
    fun switchingAwayAndBackDoesNotAcceptAnOldPresentationForTheSameId() = runTest {
        val gate = CompletableDeferred<Unit>()
        val fixture = Fixture(beforePresent = { gate.await() })
        val token = fixture.session.open(ID)
        launch { fixture.save(token) }
        runCurrent()
        fixture.session.open(EntryId("other"))
        fixture.session.open(ID)
        gate.complete(Unit)
        runCurrent()
        try {
            assertTrue(fixture.mutations.isEmpty())
            assertTrue(fixture.reveals.isEmpty())
        } finally { fixture.wipe() }
    }

    @Test
    fun revokedAccessDuringPresentationPreventsPublicationAndReveal() = runTest {
        val gate = CompletableDeferred<Unit>()
        val fixture = Fixture(beforePresent = { gate.await() })
        val token = fixture.session.open(ID)
        launch { fixture.save(token) }
        runCurrent()
        fixture.access = false
        gate.complete(Unit)
        runCurrent()
        try {
            assertTrue(fixture.mutations.isEmpty())
            assertTrue(fixture.reveals.isEmpty())
        } finally { fixture.wipe() }
    }

    @Test
    fun currentSessionPublishesThePresentationRevealAndCompletion() = runTest {
        val fixture = Fixture()
        fixture.save(fixture.session.open(ID))
        try {
            assertTrue(fixture.mutations[0] is DetailMutation.EntryPresented)
            assertEquals(DetailMutation.SaveSucceeded(DetailEditCompletion.Field(FieldKey.PASSWORD)), fixture.mutations[1])
            assertEquals(1, fixture.reveals.size)
        } finally { fixture.wipe() }
    }

    private class Fixture(
        beforeEdit: suspend () -> Unit = {},
        beforePresent: suspend () -> Unit = {},
    ) {
        var access = true
        var editCalls = 0
        var presentCalls = 0
        val mutations = mutableListOf<DetailMutation>()
        val reveals = mutableListOf<SensitiveValue>()
        private val entry = Entry(EntryIdentity(ID, EntryType.LOGIN, timestamps = EntryTimestamps(1)),
            EntryProfile(title = "Login"), EntrySecret(LoginCredential()))
        val session = DetailWorkSession(
            hasAccess = { access },
            editEntry = { _, _ -> editCalls++; beforeEdit(); AppResult.Success(entry) },
            present = {
                presentCalls++
                beforePresent()
                DetailEntryPresentation(it, emptyList(), emptyList(), emptySet())
            },
            onMutation = mutations::add,
            onReveal = { _, value -> reveals += value },
        )
        suspend fun save(token: DetailWorkSession.Scope.Active) = session.persist(token,
            EntryEdit.SetFieldValue(FieldKey.PASSWORD, "new-password"), DetailEditCompletion.Field(FieldKey.PASSWORD))
        fun wipe() = reveals.forEach(SensitiveValue::wipe)
    }

    private companion object { val ID = EntryId("entry") }
}
