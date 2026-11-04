# Typed Diagnostic Events Design

## Intent

Replace the global `LogFieldKey` plus `LogStatements` field-building API with Kotlin event payload types and generated codecs. The current setup defines event names in `Events`, field names and kinds in `LogFieldKeys`, event-to-field mappings in `LogStatements`, and then repeats field-kind validation in `PrivacyPolicy`. Adding or changing an event therefore requires edits across several catalogs, while callers can still assemble generic field maps.

The new API should make each event's allowed data visible in its type and let compilation reject an unsupported event shape, incomplete property mapping, or severity/error combination. It should remove the generic field-key registry and its duplicate allowlist without adding reflection or a runtime compatibility layer.

## Goals

- Business code logs a typed event with one `Logger.log(event)` call.
- A typed event declares its stable event identifier, severity/error contract, and all data it may record.
- Sensitive values have dedicated types and are transformed at the privacy boundary; raw strings cannot enter a sink through a generic field map.
- The event schema is the only declaration of each event's fields. There is no global field-key catalog or per-field allowlist.
- Event categories and event subclasses remain sealed and exhaustive.
- Internal source APIs may change directly. No compatibility facade for `LogStatements`, `LogFieldKey`, or arbitrary field maps is retained.

## Non-goals

- Changing Logger trace ownership, session attribution, sink behavior, sampling, or lifecycle propagation.
- Replacing the encrypted log store, Android Logcat sink, diagnostics viewer, or export pipeline.
- Recording Autofill content such as domains, IDs, hints, labels, package names in plaintext, or credentials.
- Adding reflection-based event discovery or accepting arbitrary string-keyed events from business code.

## Current evidence and affected paths

The current event pipeline is:

`LogStatements` → `Logger` generic event/field methods → `PendingLogEvent` → `PrivacyPolicy` → `SanitizedLogRecord` → aggregation and sinks.

`EventCatalog.kt` declares `Events`, `Events.All`, every `LogFieldKey`, and every `LogFieldKind`. `LogStatements.kt` contains the event-specific field maps. `PrivacyPolicy.kt` checks event membership, key-kind/value agreement, identifier namespaces, numeric constraints, and error contracts. `DefaultLogger` accepts generic event and `LogFields` arguments. `LogRecordCodec` persists string field names and stable value tags. JSONL, TSV, Logcat, storage, and the viewer consume `SanitizedLogRecord`.

The API is used outside Autofill, including application lifecycle, database/session, authentication, Vault, backup, diagnostics export, and post-commit cleanup. This is a global ingress refactor, not an Autofill-only change.

## Approaches

1. **Single field catalog.** Keep generic field maps, but define each field's type and privacy rule once. This is the smallest migration, though callers continue to compose a generic schema and the `LogFieldKey` concept remains.
2. **Typed event payloads with handwritten codecs.** Each event is a sealed Kotlin data class/object whose properties are its complete payload. `Logger.log` accepts only these event types, and a handwritten exhaustive encoder converts them into the existing sanitized-record representation. This removes per-field registration and keeps the runtime simple, but a new constructor property can be forgotten in the handwritten projection without a compiler error.
3. **Generated typed event codecs (selected).** Annotate payload data classes and use KSP to generate field projection, event registration, and codecs. The processor validates every sealed event and every constructor property, so a property must be declared as an output field or explicitly excluded. This adds a small processor module and generated-source build contract, but addresses forgotten fields while keeping runtime reflection-free.

## Proposed design

### Event model and logger boundary

Introduce a sealed `DiagnosticEvent` hierarchy grouped by domain. Each leaf event is a data class or data object annotated for KSP processing. Its declared properties are the only payload values that can be emitted for that event. `Logger.log(event)` becomes the business logging entry point; generic `debug/info/degraded/warn/error/fatal(eventKey, LogFields)` methods and `LogStatements` are removed after call-site migration.

Use sealed severity contracts so ordinary observations cannot carry error payloads, known degradations carry only `ErrorCode` and `ReasonCode`, and exception failures require a `Throwable`. The encoder derives level and event identifier from the concrete event type, not a caller-supplied string or severity parameter.

Example shape (names are illustrative):

```kotlin
sealed interface DiagnosticEvent

sealed interface ObservationEvent : DiagnosticEvent

sealed interface DegradedEvent : DiagnosticEvent {
    val errorCode: ErrorCode
    val reason: ReasonCode
}

sealed interface FailureEvent : DiagnosticEvent {
    val errorCode: ErrorCode
    val reason: ReasonCode
    val throwable: Throwable
}

data class EntryCreateCompleted(
    val entryType: EntryTypeLogValue,
    val trigger: EntryCreateTrigger,
    val elapsedMs: ElapsedMs,
) : ObservationEvent
```

Final names and grouping remain implementation details, provided a call site names one event type and supplies its typed payload without constructing a generic field map.

### KSP completeness contract

Define annotations for stable event identifiers, event severity contracts, field wire names/privacy kinds, and explicit exclusions. The KSP processor traverses the sealed event hierarchy and emits the exhaustive encoder plus registered event identifiers. Build fails when:

- a concrete event in the sealed hierarchy lacks the event annotation or a stable unique wire identifier;
- an event constructor property is neither annotated as a log field nor explicitly marked `NotLogged` with a reason;
- a field has an unsupported Kotlin type, an invalid type/privacy combination, or a duplicate wire name within its event;
- an event identifier is duplicated, or an error event violates its required error payload contract;
- a generated codec cannot serialize a property without using reflection or an unrestricted string conversion.

Supported properties use closed scalar types and explicit stable-value types. Raw free-form strings are rejected. Sensitive package identifiers use a dedicated input type and generated code routes them through the existing HMAC sanitizer. Optional values may be omitted when null. The processor emits projection code from every annotated constructor property; developers do not hand-maintain a property-to-map function.

Implement the processor in a small Kotlin/JVM module, `:core:observability-processor`, using the repository's existing Kotlin/KSP version. Core applies it to the typed event source package and depends on its generated source. The processor has no Android dependency and does not run at app runtime.

### Safe values and privacy boundary

Use constrained scalar types for non-negative counts/durations and finite ratios. Use stable wire-value types for enums; serializers must not emit Kotlin `enum.name` or ordinals. Package identifiers enter through a dedicated pending identifier type carrying an explicit namespace and are HMACed by `PrivacyPolicy`. No event payload may carry an unrestricted `String` that its codec can serialize as a field value.

The generated event encoder is private to Core and projects each event's annotated properties into the record's field-name/value map. `PrivacyPolicy` remains the mandatory boundary for identifier hashing and exception snapshot creation, and validates numeric/safe-value invariants. It no longer maintains a global event-field whitelist or compares every field key with a duplicated kind registry: arbitrary maps cannot be submitted through `Logger`.

`PendingLogEvent` and raw field-map construction become internal implementation details. Production callers cannot bypass the typed event API. Sanitized records remain immutable and contain no pending identifiers or `Throwable` objects.

### Stable records and exports

Keep the current sanitized-record concepts consumed by aggregation, storage, Logcat, JSONL/TSV/ZIP export, and the diagnostics viewer. Keep event IDs, field names, severity/category wire IDs, value tags, and JSONL schema 1 unchanged. The user-visible export format and existing encrypted log files therefore do not require a migration. This is not an internal API compatibility facade; it preserves the diagnostic data contract while replacing event construction.

Remove `LogFieldKey` from the sanitized-record map and use the generated field-name string at this internal serialization boundary. Event declarations own those names. Keep explicit event/value validation in the record codec; decoding an authenticated record does not make an untrusted caller able to construct a pending event.

### Autofill integration

Autofill fill/save, parser, discovery, classification, presentation, response, and save events become typed event payloads in the same hierarchy. `LegacyAutofillService` emits these typed events directly or through a single platform adapter where Android extraction materially belongs. Do not keep a forwarding diagnostics facade solely to translate one `LogStatements` call into another.

The Android-specific parser continues to own traversal measurements. The response builder returns a small builder-owned summary; the typed response event accepts that summary or its scalar values without making the response builder depend on logging types.

### Errors, traces, and aggregation

Keep trace metadata out of event payloads. `DefaultLogger` attaches the active Trace, clock, process/session identifiers, invokes the sanitizer, and sends accepted records to the aggregator and sinks as it does today. `ExceptionMapper` and `ExceptionSnapshotFactory` keep their existing ownership and data limits.

### Removal scope

After all production and test call sites migrate, remove generic event/field logging APIs, `LogStatements`, `LogFieldKey`/`LogFieldKeys`, `LogFieldKind`, and the business-facing field-map builder. Replace the manually curated `Events.All` list with the generated event registry for record codec validation. Keep low-level sanitized value encoding only where storage and output require it.

## Constraints and risks

- This touches Core, App, Data, diagnostics exports, and all logging call sites. The migration must be batched by event domain while keeping each intermediate source state buildable.
- `LogFieldKey` currently doubles as a storage/export map key. The refactor must remove it from record payloads without changing encoded field-name strings or value tags.
- Explicit wire values are required for event IDs, enums, errors, and categories. Renaming a Kotlin class or enum must not silently change the exported schema.
- KSP is not a Kotlin compiler plugin. Its processor must recursively inspect sealed subclasses and require explicit `@LogField` or `@NotLogged` on every constructor property; otherwise it cannot claim completeness. Processor fixtures must fail for each omission, duplicate, unsupported type, invalid privacy annotation, and error-contract violation.
- Keep failure semantics, cancellation behavior, and trace boundaries unchanged while migrating call sites.
- A platform diagnostic adapter is justified only when it extracts Android platform types or owns a coherent platform-to-event conversion. Avoid a one-method facade per event.

## Acceptance criteria

- Production callers have no generic `Logger` API that accepts arbitrary event keys or field maps.
- Each event has one typed declaration and no duplicate field-kind/allowlist registration.
- The event hierarchy and severity/error contracts are sealed and exhaustively encoded.
- HMAC inputs are never serialized before hashing; forbidden user values have no event property or encoder path.
- Existing event names, context keys, value tags, JSONL/TSV/ZIP schema 1, and encrypted record schema 1 remain byte-compatible for equivalent records.
- Trace, aggregation, storage, Logcat, viewing, and export behavior remain unchanged.
- Autofill diagnostics use typed events and retain useful request/parse/response/save stage visibility without emitting field text or identifiers.

## Verification plan

- Compile every logging producer after removing the generic Logger API; this makes any unmigrated production call site a compile failure.
- Add processor fixtures that verify successful generation and compile failure for missing annotations, duplicate IDs/field names, unsupported property types, invalid privacy kinds, and malformed failure contracts.
- Add/adjust event encoding coverage that enumerates every generated event and verifies stable ID, level, fields, and error contract.
- Exercise privacy boundaries with secret sentinel strings in forbidden event inputs and verify they cannot be represented or emitted; verify package identifiers appear only as HMAC values.
- Run Core/App/Data/Domain unit suites affected by observability, architecture/boundary checks, record-codec tests, export-format tests, and `git diff --check`.
- Confirm no tests, docs, or production source uses `LogFieldKey`, `LogFieldKeys`, or `LogStatements` after migration.

## Decisions made for the implementer

- Preserve the current exported and encrypted record wire format; remove source-level compatibility APIs.
- Use direct Kotlin sealed payloads with KSP-generated codecs; processor checks every constructor property as `@LogField` or reasoned `@NotLogged`, and generated code must not use runtime reflection.
- Keep typed event definitions in Core; keep Android extraction in the existing App Autofill adapter only where it has real platform work.
- Do not commit until review and verification are complete.
