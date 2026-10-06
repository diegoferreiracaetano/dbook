package com.dbook.infrastructure.persistence.crm

import com.dbook.domain.crm.CustomerNote

fun CustomerNoteJpaEntity.toDomain() =
    CustomerNote(id, customerId, authorId, body, pinned, createdAt, editedAt, deletedAt, version)

fun CustomerNote.toJpaEntity() =
    CustomerNoteJpaEntity(id, customerId, authorId, body, pinned, createdAt, editedAt, deletedAt, version)
