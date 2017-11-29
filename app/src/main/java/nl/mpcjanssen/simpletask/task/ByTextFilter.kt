/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).

 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)

 * LICENSE:

 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.

 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.

 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * //www.gnu.org/licenses/>.

 * @author Todo.txt contributors @yahoogroups.com>
 * *
 * @license http://www.gnu.org/licenses/gpl.html
 * *
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 */
package nl.mpcjanssen.simpletask.task

import java.util.*

/**
 * A filter that matches Tasks containing the specified text
 *
 * @author Tim Barlotta
 */
class ByTextFilter(val moduleName : String, searchText: String?, internal val isCaseSensitive: Boolean) : TaskFilter {
    /* FOR TESTING ONLY, DO NOT USE IN APPLICATION */
    private val parts: Array<String>
    private var casedText: String
    val text = searchText ?: ""
    init {
        this.casedText = if (isCaseSensitive) text else text.toUpperCase(Locale.getDefault())
        this.parts = this.casedText.split("\\s".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    }

    override fun apply(task: Task): Boolean {
        val taskText = if (isCaseSensitive)
            task.text
        else
            task.text.toUpperCase(Locale.getDefault())

        for (part in parts) {
            if (part.length > 0 && !taskText.contains(part))
                return false
        }

        return true
    }
}
