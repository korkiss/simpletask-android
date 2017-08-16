package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.task.Task
import java.util.*

class AlphabeticalComparator(caseSensitive: Boolean) : Comparator<Task> {
    val stringComp = AlphabeticalStringComparator(caseSensitive)
    override fun compare(t1: Task?, t2: Task?): Int {
        return stringComp.compare(t1?.description, t2?.description)
    }
}
