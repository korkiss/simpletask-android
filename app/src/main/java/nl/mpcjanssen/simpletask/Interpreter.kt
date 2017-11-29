package nl.mpcjanssen.simpletask

import nl.mpcjanssen.simpletask.task.TToken
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.util.*

import tcl.lang.*

class ToastShortCmd : Command {
    override fun cmdProc(interp: Interp?, objv: Array<out TclObject>?) {
        interp?.let {
            if (objv == null || objv.size != 2) {
                throw TclNumArgsException(interp, 1, objv,
                        "toastText")
            }
            showToastShort(TodoApplication.app, objv[1].toString())
        }
    }

}

fun Interp.init(script: String?) : Interp  {
    try {
        this.createCommand("toast", ToastShortCmd())
        this.eval(Config.STDLIB)
        this.eval(Config.luaConfig)
        script?.let { this.eval(it) }
    } catch (e: TclException) {
        Logger.warn(Config.TAG, "Script execution failed " + this.result)
        showToastLong(TodoApplication.app, "${getString(R.string.script_error)}:  ${e.message}")
    }
    return this
}


fun Interp.tasklistTextSize(): Double? {
    try {
        return TclDouble.get(this, this.getVar(Vars.CONFIG_TASKLIST_TEXT_SIZE_SP, TCL.GLOBAL_ONLY))
    } catch (e: TclException) {
        return null
    }
}

// Callback to determine the theme. Return true for dark.


fun Interp.configTheme(): String? {
    try {
        return this.getVar(Vars.CONFIG_THEME, TCL.GLOBAL_ONLY).toString()
    } catch (e: TclException) {
        return null
    }
}

fun Interp.hasOnFilterCallback(): Boolean {
    val cmd = this.getCommand(Callbacks.ON_FILTER_NAME)
    return cmd != null
}

fun Interp.hasOnSortCallback(): Boolean {
    val cmd = this.getCommand(Callbacks.ON_SORT_NAME)
    return cmd != null
}

fun Interp.hasOnDisplayCallback(): Boolean {
    val cmd = this.getCommand(Callbacks.ON_SORT_NAME)
    return cmd != null
}

fun Interp.hasOnGroupCallback(): Boolean {
    val cmd = this.getCommand(Callbacks.ON_GROUP_NAME)
    return cmd != null
}

fun Interp.onFilterCallback(t: Task): Boolean {
    if (!hasOnFilterCallback()) {
        return true
    }
    try {
        executeCallbackCommand(Callbacks.ON_FILTER_NAME, t)
        return TclBoolean.get(this, this.result)
    } catch (e: TclException) {
        log.debug(TAG, "Tcl execution failed: ${this.result}")
        return true
    }
}

fun Interp.onSortCallback(t1: Task?, t2: Task?): Int {
    try {
        val cmdList = TclList.newInstance()
        TclList.append(this, cmdList, TclString.newInstance(Callbacks.ON_SORT_NAME))
        appendCallbackArgs(t1, cmdList)
        appendCallbackArgs(t2, cmdList)
        this.eval(cmdList, TCL.GLOBAL_ONLY)
        return TclInteger.getInt(this, this.result)
    } catch (e: TclException) {
        log.debug(TAG, "Tcl execution failed " + e.message)
        return 0
    }
}

fun Interp.onGroupCallback(t: Task): String? {
    if (!hasOnGroupCallback()) {
        return null
    }
    try {
        executeCallbackCommand(Callbacks.ON_GROUP_NAME, t)
        return this.result.toString()
    } catch (e: TclException) {
        log.debug(TAG, "Tcl execution failed " + e.message)
        return null
    }
}

fun Interp.onDisplayCallback(t: Task): String? {
    if (!hasOnDisplayCallback()) {
        return null
    }
    try {
        executeCallbackCommand(Callbacks.ON_DISPLAY_NAME, t)
        return this.result.toString()
    } catch (e: TclException) {
        log.debug(TAG, "Tcl execution failed " + e.message)
        return null
    }
}


private fun Interp.executeCallbackCommand(command: String, t: Task) {
    val cmdList = TclList.newInstance()
    TclList.append(this, cmdList, TclString.newInstance(command))
    appendCallbackArgs(t, cmdList)
    this.eval(cmdList, TCL.GLOBAL_ONLY)
}

private fun Interp.appendCallbackArgs(t: Task?, cmdList: TclObject) {
    val fieldDict = TclDict.newInstance()
    val extensionDict = TclDict.newInstance()
    t?.let {
        fieldDict.put(this, "task", t.inFileFormat())
        fieldDict.put(this, "due", t.dueDate ?: "")
        fieldDict.put(this, "threshold", t.thresholdDate ?: "")
        fieldDict.put(this, "createdate", t.createDate ?: "")
        fieldDict.put(this, "completiondate", t.completionDate ?: "")
        fieldDict.put(this, "text", t.showParts(TToken.TEXT))


        val recPat = t.recurrencePattern
        if (recPat != null) {
            fieldDict.put(this, "recurrence", recPat)
        }
        fieldDict.put(this, "completed", if (t.isCompleted()) "1" else "0")
        fieldDict.put(this, "priority", t.priority.code)

        fieldDict.put(this, "tags", javaListToTclList(t.tags))
        fieldDict.put(this, "lists", javaListToTclList(t.lists))



        for ((key, value) in t.extensions) {
            extensionDict.put(this, key, value)
        }
    }
    TclList.append(this, cmdList, TclString.newInstance(t?.inFileFormat() ?: ""))
    TclList.append(this, cmdList, fieldDict)
    TclList.append(this, cmdList, extensionDict)

}


private fun Interp.javaListToTclList(javaList: Iterable<String>): TclObject {
    val tclList = TclList.newInstance()
    for (item in javaList) {
        TclList.append(this, tclList, TclString.newInstance(item))
    }
    return tclList
}


object Vars {
    val CONFIG_TASKLIST_TEXT_SIZE_SP = "tasklistTextSize"
    val CONFIG_THEME = "theme"
}

object Callbacks {
    val ON_DISPLAY_NAME = "onDisplay"
    val ON_FILTER_NAME = "onFilter"
    val ON_GROUP_NAME = "onGroup"
    val ON_SORT_NAME = "onSort"
}

fun TclObject.put(interp: Interp, key: String, value: TclObject) {
    TclDict.put(interp, this, TclString.newInstance(key), value)
}

fun TclObject.put(interp: Interp, key: String, value: String) {
    this.put(interp, key, TclString.newInstance(value))
}

