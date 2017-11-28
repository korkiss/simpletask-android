package nl.mpcjanssen.simpletask

import nl.mpcjanssen.simpletask.task.Priority
import nl.mpcjanssen.simpletask.task.TToken
import nl.mpcjanssen.simpletask.task.Task
import java.util.*
import kotlin.Comparator

class ScriptComparator(val interp: Interpreter) : Comparator<Task> {
    override fun compare(t1: Task?, t2: Task?): Int {
        return interp.onSortCallback(t1,t2)

    }
}
