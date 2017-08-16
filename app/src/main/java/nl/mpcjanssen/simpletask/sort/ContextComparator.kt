package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.task.Task
import java.util.*

class ContextComparator(caseSensitive: Boolean) : Comparator<Task> {

    private val mStringComparator: AlphabeticalStringComparator

    init {
        this.mStringComparator = AlphabeticalStringComparator(caseSensitive)
    }

    override fun compare(a: Task?, b: Task?): Int {
        if (a === b) {
            return 0
        } else if (a == null) {
            return -1
        } else if (b == null) {
            return 1
        }
        return mStringComparator.compare(a.project, b.project)
    }
}
