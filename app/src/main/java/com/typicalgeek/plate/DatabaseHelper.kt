@file:JvmName("DatabaseHelper")

package com.typicalgeek.plate

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TASKS_TABLE_NAME($TASKS_COL_0 INTEGER PRIMARY KEY, $TASKS_COL_1 INTEGER, $TASKS_COL_2 TEXT, $TASKS_COL_3 TEXT, $TASKS_COL_4 TEXT,  TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        refreshDB()
    }

    fun insertTask(task: Task): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(TASKS_COL_1, 0)
        contentValues.put(TASKS_COL_2, task.taskTitle)
        contentValues.put(TASKS_COL_3, task.taskDescription)
        contentValues.put(TASKS_COL_4, task.taskDate)
        val result = db.insert(TASKS_TABLE_NAME, null, contentValues)
        db.close()
        return result != (-1).toLong()
    }

    fun getAllData(TABLE_NAME: String): Cursor {
        val db = this.writableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $TASKS_COL_1, $TASKS_COL_4 ASC", null)
    }

    fun countCurrent(TABLE_NAME: String): Int {
        val db = this.writableDatabase
        val result = db.rawQuery("SELECT * FROM $TABLE_NAME WHERE $TASKS_COL_1 = 0", null)
        val num = result.count
        result.close()
        return num
    }

    fun deleteTask(ID: Int) {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM $TASKS_TABLE_NAME WHERE $TASKS_COL_0 = $ID")
    }

    private fun refreshDB() {
        val db = this.writableDatabase
        db.execSQL("DROP TABLE IF EXISTS $TASKS_TABLE_NAME")
        onCreate(db)
    }

    fun updateTask(ID: Int, task: Task) {
        val db = this.writableDatabase
        db.execSQL("UPDATE $TASKS_TABLE_NAME SET $TASKS_COL_2 = '${task.taskTitle}', $TASKS_COL_3 = '${task.taskDescription}' WHERE $TASKS_COL_0 = $ID")
    }

    fun updateTaskStatus(ID: Int, isComplete: Int) {
        val db = this.writableDatabase
        db.execSQL("UPDATE $TASKS_TABLE_NAME SET $TASKS_COL_1 = $isComplete WHERE $TASKS_COL_0 = $ID")
    }

    companion object {
        private const val DATABASE_NAME = "TasksDB"
        private const val DATABASE_VERSION = 1
        const val TASKS_TABLE_NAME = "tasks_table"
        private const val TASKS_COL_0 = "ID"
        private const val TASKS_COL_1 = "COMPLETE"
        private const val TASKS_COL_2 = "TITLE"
        private const val TASKS_COL_3 = "DESCRIPTION"
        private const val TASKS_COL_4 = "DATE"
    }
}