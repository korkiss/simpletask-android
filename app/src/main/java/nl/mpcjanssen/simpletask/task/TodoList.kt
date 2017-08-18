/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).
 *
 *
 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)
 *
 *
 * LICENSE:
 *
 *
 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 *
 *
 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 *
 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * //www.gnu.org/licenses/>.

 * @author Todo.txt contributors @yahoogroups.com>
 * *
 * @license http://www.gnu.org/licenses/gpl.html
 * *
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 */
package nl.mpcjanssen.simpletask.task

import android.app.Activity
import android.content.Intent
import android.util.Log
import nl.mpcjanssen.simpletask.*

import nl.mpcjanssen.simpletask.remote.TaskWarrior
import nl.mpcjanssen.simpletask.sort.MultiComparator
import nl.mpcjanssen.simpletask.util.*
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.collections.ArrayList

/**
 * Implementation of the in memory representation of the Todo list
 * uses an ActionQueue to ensure modifications and access of the underlying todo list are
 * sequential. If this is not done properly the result is a likely ConcurrentModificationException.

 * @author Mark Janssen
 */




object TodoList {

    private var mLists: ArrayList<String>? = null
    private var mTags: ArrayList<String>? = null
    val todoItems = CopyOnWriteArrayList<Task>()
    val selectedItems = CopyOnWriteArraySet<Task>()
    internal val TAG = TodoList::class.java.simpleName

    fun hasPendingAction(): Boolean {
        return ActionQueue.hasPending()
    }

    // Wait until there are no more pending actions
    @Suppress("unused") // Used in test suite
    fun settle() {
        while (hasPendingAction()) {
            Thread.sleep(10)
        }
    }

    fun queue(description: String, body: () -> Unit) {
        val r = Runnable(body)
        ActionQueue.add(description, r)
    }

    fun add(items: List<Task>, atEnd: Boolean) {
        queue("Add task ${items.size} atEnd: $atEnd") {
            if (atEnd) {
                todoItems.addAll(items)
            } else {
                todoItems.addAll(0, items)
            }
        }
    }

    fun add(t: Task, atEnd: Boolean) {
        add(listOf(t), atEnd)
    }

    fun removeAll(tasks: List<Task>) {
        queue("Remove") {
            TaskWarrior.callTaskForSelection(tasks, "delete")
            selectedItems.removeAll(tasks)
            reload()
        }
    }

    fun size(): Int {
        return todoItems.size
    }


    val projects: ArrayList<String>
        get() {
            val lists = mLists
            if (lists != null) {
                return lists
            }
            val res = HashSet<String>()
            todoItems.forEach {
                it.project?.let { res.add(it)}
            }
            val newLists = ArrayList<String>()
            newLists.addAll(res)
            mLists = newLists
            return newLists
        }

    val tags: ArrayList<String>
        get() {
            val tags = mTags
            if (tags != null) {
                return tags
            }
            val res = HashSet<String>()
            todoItems.toMutableList().forEach {
                res.addAll(it.tags)
            }
            val newTags = ArrayList<String>()
            newTags.addAll(res)
            mTags = newTags
            return newTags
        }

    fun uncomplete(tasks: List<Task>) {
        queue("Uncomplete") {
            Log.d(TAG,"Uncompleting tasks")
            TaskWarrior.callTaskForSelection(tasks, "modify", "status:pending")
            reload()
        }
    }

    fun complete(tasks: List<Task>) {
        queue("Complete") {
            TaskWarrior.callTaskForSelection(tasks, "done")
            reload()
        }

    }

    fun prioritize(tasks: List<Task>, prio: Priority) {


    }

    fun defer(deferString: String, tasks: List<Task>, dateType: DateType) {
        queue("Defer") {
            // TODO: implement
//            tasks.forEach {
//                when (dateType) {
//                    DateType.DUE -> it.deferDueDate(deferString, todayAsString)
//                    DateType.THRESHOLD -> it.deferThresholdDate(deferString, todayAsString)
//                }
//            }
        }
    }

    var selectedTasks: List<Task> = ArrayList()
        get() {
            return selectedItems.toList()
        }

    var completedTasks: List<Task> = ArrayList()
        get() {
            return todoItems.filter { it.isCompleted }
        }

    fun startAddTaskActivity(act: Activity, prefill: String) {
        queue("Start add/edit task activity") {
            Log.i(TAG, "Starting addTask activity")
            val intent = Intent(act, AddTask::class.java)
            intent.putExtra(Constants.EXTRA_PREFILL_TEXT, prefill)
            act.startActivity(intent)
        }
    }

    fun getSortedTasks(filter: ActiveFilter, sorts: ArrayList<String>, caseSensitive: Boolean): Sequence<Task> {
        val comp = MultiComparator(sorts, STWApplication.app.today, caseSensitive, filter.createIsThreshold)
        val itemsToSort = if (comp.fileOrder) {
            todoItems
        } else {
            todoItems.reversed()
        }
        val filteredTasks = filter.apply(itemsToSort.asSequence())
        comp.comparator?.let {
            return filteredTasks.sortedWith(it)
        }
        return filteredTasks
    }

    fun sync() {
        queue("Sync") {
            STWApplication.app.localBroadCastManager.sendBroadcast(Intent(Constants.BROADCAST_SYNC_START))
            TaskWarrior.callTask("sync")
            reload()
        }
    }
    fun reload(reason: String = "") {
        val logText = "Reload: " + reason
        queue(logText) {

            STWApplication.app.localBroadCastManager.sendBroadcast(Intent(Constants.BROADCAST_SYNC_START))
            if (!Config.hasKeepSelection) {
                TodoList.clearSelection()
            }
            todoItems.clear()
            todoItems.addAll(TaskWarrior.taskList())
            mLists = null
            mTags = null
            broadcastRefreshUI(STWApplication.app.localBroadCastManager)
        }
    }

    fun isSelected(item: Task): Boolean {
        return selectedItems.indexOf(item) > -1
    }

    fun numSelected(): Int {
        return selectedItems.size
    }

    fun selectTasks(items: List<Task>) {
        queue("Select") {
            selectedItems.addAll(items)
            broadcastRefreshSelection(STWApplication.app.localBroadCastManager)
        }
    }

    fun selectTask(item: Task?) {
        item?.let {
            selectTasks(listOf(item))
        }
    }

    fun unSelectTask(item: Task) {
        unSelectTasks(listOf(item))
    }

    fun unSelectTasks(items: List<Task>) {
        queue("Unselect") {
            selectedItems.removeAll(items)
            broadcastRefreshSelection(STWApplication.app.localBroadCastManager)
        }
    }


    fun clearSelection() {
        queue("Clear selection") {
            selectedItems.clear()
            broadcastRefreshSelection(STWApplication.app.localBroadCastManager)
        }
    }

    fun getTaskCount(): Long {
        val items = todoItems
        return items.size.toLong()
    }

    fun add(tasks: List<String>) {
        queue( "Adding ${tasks.size} tasks" ) {
            tasks.forEach {
                TaskWarrior.callTask("add", *it.split(" ").toTypedArray())
            }
            reload()
        }
    }

    fun updateTags(tasks: List<Task>, tagsToAdd: ArrayList<String>, tagsToRemove: ArrayList<String>) {
        queue("Update tags") {
            val args = ArrayList<String>()
            args.add("modify")
            tagsToAdd.forEach {
                args.add("+$it")
            }
            tagsToRemove.forEach {
                args.add("-$it")
            }
            TaskWarrior.callTaskForSelection(tasks, *args.toTypedArray())
            reload()


        }
    }
    fun updateProject(tasks: List<Task>, project: String) {
        queue("Update tags") {
            val args = ArrayList<String>()
            args.add("modify")
            args.add("project:$project")
            TaskWarrior.callTaskForSelection(tasks, *args.toTypedArray())
            reload()
        }
    }
}

