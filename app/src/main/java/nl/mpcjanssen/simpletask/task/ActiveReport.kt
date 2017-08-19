package nl.mpcjanssen.simpletask.task

import nl.mpcjanssen.simpletask.sort.ContextComparator


class ActiveReport(val name: String) {
    val comparator: Comparator<Task> = ContextComparator(false)

}