package mcmodvalidator.pro.issue

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object IssueRegistry {
    private val issues = ConcurrentHashMap.newKeySet<DetectedIssue>()

    fun register(issue: DetectedIssue) {
        issues.add(issue)
    }

    fun registerAll(newIssues: List<DetectedIssue>) {
        issues.addAll(newIssues)
    }

    fun getAll(): Set<DetectedIssue> = issues.toSet()

    fun getByModId(modId: String): List<DetectedIssue> =
        issues.filter { it.modId == modId }

    fun getById(id: UUID): DetectedIssue? =
        issues.find { it.id == id }

    fun markApplied(id: UUID): Boolean {
        val issue = getById(id) ?: return false
        issues.remove(issue)
        issues.add(issue.copy(applied = true))
        return true
    }

    fun clear() = issues.clear()

    fun countByModId(modId: String): Int =
        issues.count { it.modId == modId }
}
