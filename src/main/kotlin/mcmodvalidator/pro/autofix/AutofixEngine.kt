package mcmodvalidator.pro.autofix

import mcmodvalidator.pro.issue.DetectedIssue
import mcmodvalidator.pro.issue.IssueRegistry
import org.slf4j.LoggerFactory

object AutofixEngine {
    private val logger = LoggerFactory.getLogger("mc-modvalidator-fabric")

    fun canApplyFix(issue: DetectedIssue): Boolean {
        return issue.autoFixable && !issue.applied
    }

    fun applyFix(issue: DetectedIssue): Boolean {
        if (!canApplyFix(issue)) {
            logger.warn("Cannot apply fix for issue ${issue.id}: autoFixable=${issue.autoFixable}, applied=${issue.applied}")
            return false
        }

        logger.info("Applying fix for issue ${issue.id}: ${issue.message}")

        return when (issue.category) {
            else -> {
                logger.warn("No autofix implementation for category ${issue.category}")
                false
            }
        }
    }
}
