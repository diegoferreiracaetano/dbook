package com.dbook.domain.crm

class CustomerNoteNotFoundException(id: Long) : RuntimeException("Note not found: $id")

class NoteAccessDeniedException : RuntimeException("Only the author can change this note")
