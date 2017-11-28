package nl.mpcjanssen.simpletask

import nl.mpcjanssen.simpletask.task.TToken
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.util.*

import tcl.lang.*


class Interpreter(script: String?){
    private val interp = Interp()
    private val log = Logger
    private val TAG = "Interpreter"


    val STDLIB = readAsset(TodoApplication.app.assets, "lua/stdlib.tcl")

    init {

        try {
            interp.createCommand("toast", ToastShortCmd)
            evalScript(STDLIB)
            evalScript(Config.luaConfig)
            script?.let {interp.eval(it)}

        } catch (e: InterpreterException) {
            nl.mpcjanssen.simpletask.util.log.warn(Config.TAG, "Script execution failed " + e.message)
            showToastLong(TodoApplication.app, "${getString(R.string.script_error)}:  ${e.message}")
        }

    }

    fun tasklistTextSize(): Double? {
        try {
            return TclDouble.get(interp, interp.getVar(Vars.CONFIG_TASKLIST_TEXT_SIZE_SP, TCL.GLOBAL_ONLY))
        } catch (e: TclException) {
            return null
        }
    }

    // Callback to determine the theme. Return true for dark.


    fun configTheme(): String? {
        try {
            return interp.getVar(Vars.CONFIG_THEME, TCL.GLOBAL_ONLY).toString()
        } catch (e: TclException) {
            return null
        }
    }

    fun hasOnFilterCallback(): Boolean {
        val cmd = interp.getCommand(Callbacks.ON_FILTER_NAME)
        return cmd != null
    }

    fun hasOnSortCallback(): Boolean {
        val cmd = interp.getCommand(Callbacks.ON_SORT_NAME)
        return cmd != null
    }

    fun hasOnDisplayCallback(): Boolean {
        val cmd = interp.getCommand(Callbacks.ON_SORT_NAME)
        return cmd != null
    }

    fun hasOnGroupCallback(): Boolean {
        val cmd = interp.getCommand(Callbacks.ON_GROUP_NAME)
        return cmd != null
    }

    fun onFilterCallback(t: Task): Boolean {
        if (!hasOnFilterCallback()) {
            return true
        }
        try {
            executeCallbackCommand(Callbacks.ON_FILTER_NAME,  t)
            return TclBoolean.get(interp, interp.result)
        } catch (e: TclException) {
            log.debug(TAG, "Tcl execution failed: ${interp.result}")
            return true
        }
    }

    fun onSortCallback(t1: Task?, t2: Task?): Int {
        try {
            val cmdList = TclList.newInstance()
            TclList.append(interp, cmdList,  TclString.newInstance(Callbacks.ON_SORT_NAME) )
            appendCallbackArgs(t1, cmdList)
            appendCallbackArgs(t2, cmdList)
            interp.eval(cmdList,TCL.GLOBAL_ONLY)
            return TclInteger.getInt(interp, interp.result)
        } catch (e: TclException) {
            log.debug(TAG, "Tcl execution failed " + e.message)
            return 0
        }
    }

    fun onGroupCallback(t: Task): String? {
        if (!hasOnGroupCallback()) {
            return null
        }
        try {
            executeCallbackCommand(Callbacks.ON_GROUP_NAME, t)
           return interp.result.toString()
        } catch (e: TclException) {
            log.debug(TAG, "Tcl execution failed " + e.message)
            return null
        }
    }

    fun onDisplayCallback(t: Task): String? {
        if (!hasOnDisplayCallback()) {
            return null
        }
        try {
            executeCallbackCommand(Callbacks.ON_DISPLAY_NAME, t)
            return interp.result.toString()
        } catch (e: TclException) {
            log.debug(TAG, "Tcl execution failed " + e.message)
            return null
        }
    }



    fun evalScript(script: String?): Interpreter {
        try {
            interp.eval(TclString.newInstance(script),TCL.GLOBAL_ONLY)
        } catch (e: TclException) {
            throw InterpreterException(interp.result.toString())
        }
        return this
    }


    private fun executeCallbackCommand(command: String, t: Task) {
        val cmdList = TclList.newInstance()
        TclList.append(interp, cmdList, TclString.newInstance(command))
        appendCallbackArgs(t, cmdList)
        interp.eval(cmdList, TCL.GLOBAL_ONLY)
    }

    private fun appendCallbackArgs(t: Task?, cmdList: TclObject) {
        val fieldDict = TclDict.newInstance()
        val tokenList = TclList.newInstance()
        val extensionDict = TclDict.newInstance()
        t?.let {
            t.tokens.forEach {
                val item = TclList.newInstance()
                TclList.append(interp, item, TclInteger.newInstance(it.type.toLong()))
                TclList.append(interp, item, TclString.newInstance(it.text))
                TclList.append(interp, tokenList, item)
            }

            fieldDict.put(interp, "task", t.inFileFormat())
            fieldDict.put(interp, "tokens", tokenList)
            fieldDict.put(interp, "due", t.dueDate ?: "")
            fieldDict.put(interp, "threshold", t.thresholdDate ?: "")
            fieldDict.put(interp, "createdate", t.createDate ?: "")
            fieldDict.put(interp, "completiondate", t.completionDate ?: "")
            fieldDict.put(interp, "text", t.showParts(TToken.TEXT))


            val recPat = t.recurrencePattern
            if (recPat != null) {
                fieldDict.put(interp, "recurrence", recPat)
            }
            fieldDict.put(interp, "completed", if (t.isCompleted()) "1" else "0")
            fieldDict.put(interp, "priority", t.priority.code)

            fieldDict.put(interp, "tags", javaListToTclList(t.tags))
            fieldDict.put(interp, "lists", javaListToTclList(t.lists))



            for ((key, value) in t.extensions) {
                extensionDict.put(interp, key, value)
            }
        }
        TclList.append(interp, cmdList, TclString.newInstance(t?.inFileFormat()?: ""))
        TclList.append(interp, cmdList, fieldDict)
        TclList.append(interp, cmdList, extensionDict)

    }


    private fun javaListToTclList(javaList: Iterable<String>): TclObject {
        val tclList = TclList.newInstance()
        for (item in javaList) {
            TclList.append(interp, tclList, TclString.newInstance(item))
        }
        return tclList
    }

}

object ToastShortCmd : Command {
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

class InterpreterException(message: String) : Throwable(message)

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

fun TclObject.put(interp: Interp, key: String, value: String)  {
    this.put(interp, key, TclString.newInstance(value))
}

