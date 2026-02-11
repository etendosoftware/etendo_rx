# Phase 3: Generic Repository Layer - Context

**Gathered:** 2026-02-06
**Status:** Ready for planning

<domain>
## Phase Boundary

Create a dynamic repository that wraps JPA EntityManager to provide CRUD + pagination using the DynamicDTOConverter from Phase 2. This repository replaces the generated `*DASRepository` classes with a single dynamic implementation that uses runtime metadata to perform operations on any entity.

</domain>

<decisions>
## Implementation Decisions

### CRUD Semantics
- **Upsert on POST**: Check existence first. If entity exists, update it. If not, create it. Same behavior as generated repos.
- **Partial update on PUT**: Only fields present in the DTO are written. Missing fields keep their current values. No full-replace semantics.
- **Batch operations supported**: Accept List<Map> and process all entities. Needed for connector batch imports.
- **Field filtering on findAll**: Support `?field=value` query params for equality filtering on any field, in addition to pagination and sorting.

### Entity Resolution
- **EntityManager directly**: Use EntityManager.find(), createQuery(), persist(), merge() directly. No Spring Data JPA repositories needed.
- **Hibernate metamodel for class resolution**: Use EntityManager.getMetamodel() to scan registered entity classes and match by table name. Do NOT use AD_Table.javaClassName.
- **New standalone class**: DynamicRepository with its own API, not implementing the existing DASRepository interface. Phase 4 controller will be new anyway.

### Transaction & Error Handling
- **One transaction per batch**: All entities in a batch are saved in a single transaction. If any entity fails, the entire batch rolls back.
- **RestCallTransactionHandler**: Use the existing RestCallTransactionHandler for transaction management. Consistent with generated repo behavior.
- **Throw on not-found**: findById throws EntityNotFoundException when entity doesn't exist. Controller catches and maps to 404.

### ExternalId & Audit Flow
- **Always register ExternalId on save**: Call ExternalIdService.add() after every save, not just for connector requests.
- **Order of operations**: Match generated repos exactly. Research needed to determine: audit -> persist -> externalId or another order.
- **ExternalIdService.flush() timing**: Match generated repo flush pattern. Research needed.
- **Audit field behavior**: Match AuditServiceInterceptor behavior exactly (sets updatedBy/updated always, createdBy/creationDate only when null).

### Claude's Discretion
- Pagination approach (Spring Pageable/Page or custom) -- pick what's most practical
- Error format for validation failures -- pick cleanest approach for controller layer
- Internal class structure and helper methods

</decisions>

<specifics>
## Specific Ideas

- The repository should be the single entry point for all entity data access in the dynamic DAS layer
- Field filtering should work with any DM field (direct property access on entity)
- Batch save in one transaction is critical for connector import performance
- Partial update prevents data loss when connectors send sparse DTOs

</specifics>

<deferred>
## Deferred Ideas

None -- discussion stayed within phase scope

</deferred>

---

*Phase: 03-generic-repository-layer*
*Context gathered: 2026-02-06*
