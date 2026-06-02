package mcmodvalidator.pro.issue

import java.util.UUID

enum class Severity {
    INFO, WARN, ERROR
}

enum class Category {
    KOTLIN_INTEROP, PORTING_OBSOLETE_CLASS, PORTING_DEPRECATED_API
}

data class DetectedIssue(
    val id: UUID = UUID.randomUUID(),
    val modId: String,
    val severity: Severity,
    val category: Category,
    val targetClass: String,
    val message: String,
    val suggestedFix: String,
    val autoFixable: Boolean,
    val applied: Boolean = false
)
