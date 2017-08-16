package nl.mpcjanssen.simpletask.task

import hirondelle.date4j.DateTime
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

data class Task(
        val json: JSONObject,
        val uuid: String,
        val description: String,
        val annotations: List<String>,
        val project: String?,
        val tags: List<String>,
        val priority: Priority,
        val status: String,
        val dueDate: String?,
        val waitDate: String?,
        val endDate: String?,
        val entryDate: String) {

    fun isCompleted(): Boolean {
        return status =="completed"
    }

    fun isDeleted(): Boolean {
        return status =="deleted"
    }


    fun inFuture(today: String): Boolean {
            if (waitDate != null) {
                return (waitDate > today)
            } else {
                return false
            }
        }

    val displayText: String = description


    fun getHeader(sort: String, empty: String, createIsThreshold: Boolean): String {
        if (sort.contains("by_context")) {
            if (project != null) {
                return project
            } else {
                return empty
            }
        } else if (sort.contains("by_project")) {
            if (tags.size > 0) {
                return tags.first()
            } else {
                return empty
            }
        } else if (sort.contains("by_threshold_date")) {
            if (createIsThreshold) {
                return waitDate ?: entryDate ?: empty
            } else {
                return waitDate ?: empty
            }
        } else if (sort.contains("by_prio")) {
            return priority.code
        } else if (sort.contains("by_due_date")) {
            return dueDate ?: empty
        }
        return ""
    }


    companion object {
        const val DATE_FORMAT = "YYYY-MM-DD"
    }
}

val <Task> List<Task>.asJSON: String
    get() {
        val result = JSONArray()
        this.forEach {
            result.put(it)
        }
        return result.toString(1)
    }