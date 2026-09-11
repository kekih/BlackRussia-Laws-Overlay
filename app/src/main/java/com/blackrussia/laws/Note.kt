package com.blackrussia.laws

import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    var content: String, // HTML content for rich formatting
    var order: Int = 0
)
