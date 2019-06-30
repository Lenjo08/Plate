@file:JvmName("Task")

package com.typicalgeek.plate

class Task(val taskID: Int = 0, var isTaskComplete: Boolean = false, var taskTitle: String="", var taskDescription: String="", var taskDate: String="")