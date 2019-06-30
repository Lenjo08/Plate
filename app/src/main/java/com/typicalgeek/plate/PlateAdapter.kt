@file:JvmName("PlateAdapter")

package com.typicalgeek.plate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import java.util.*

internal class PlateAdapter(myListener: RefreshInterface, myTasks: Array<Task>) : RecyclerView.Adapter<PlateAdapter.MyViewHolder>() {
    init {
        mListener = myListener
        mTasks = myTasks
    }

    internal class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        internal val cbComplete: CheckBox
        internal val tvTask: TextView
        private val ibDelete: ImageButton

        init {
            databaseHelper = DatabaseHelper(itemView.context)
            cbComplete = itemView.findViewById(R.id.cbComplete)
            tvTask = itemView.findViewById(R.id.tvTask)
            ibDelete = itemView.findViewById(R.id.ibDelete)
            this.itemView.setOnClickListener(this)
            cbComplete.setOnClickListener {
                val checked = if (cbComplete.isChecked) 1 else 0
                databaseHelper.updateTaskStatus(mTasks[adapterPosition].taskID, checked)
                mListener.refreshAll()
            }
            ibDelete.setOnClickListener {
                databaseHelper.deleteTask(mTasks[adapterPosition].taskID)
                mListener.refreshAll()
            }
        }

        override fun onClick(v: View) {
            val t = mTasks[adapterPosition]
            val inflater = LayoutInflater.from(v.context)
            val detailsView = inflater.inflate(R.layout.layout_details,
                    v.parent as ViewGroup, false)
            val tvTaskDetails = detailsView.findViewById<TextView>(R.id.tvTaskDetails)
            AlertDialog.Builder(v.context)
                    .setTitle(R.string.details)
                    .setView(detailsView)
                    .setPositiveButton(R.string.edit) { _, _ ->
                        val editTaskView = inflater.inflate(R.layout.layout_task_entry,
                                v.parent as ViewGroup, false)
                        val etTaskTitle = editTaskView.findViewById<EditText>(R.id.etTaskTitle)
                        val etTaskDescription = editTaskView.findViewById<EditText>(R.id.etTaskDescription)
                        AlertDialog.Builder(v.context)
                                .setTitle(R.string.edit_task)
                                .setView(editTaskView)
                                .setPositiveButton(R.string.save) { _, _ ->
                                    val title = etTaskTitle.text.toString().trim { it <= ' ' }
                                    val description = etTaskDescription.text
                                            .toString().trim { it <= ' ' }
                                    if (MainActivity.validateInput(title, description)) {
                                        val edits = MainActivity.buildTask(title, description, t.taskDate)
                                        databaseHelper.updateTask(t.taskID, edits)
                                        mListener.refreshAll()
                                    } else {
                                        Toast.makeText(v.context,
                                                R.string.empty_task_error,
                                                Toast.LENGTH_LONG).show()
                                    }
                                }
                                .setNeutralButton(R.string.cancel, null)
                                .create().show()
                        etTaskTitle.setText(t.taskTitle)
                        etTaskDescription.setText(t.taskDescription)
                    } // HERE
                    .setNeutralButton(R.string.close, null)
                    .create().show()
            tvTaskDetails.text = String.format(Locale.getDefault(), v.context.getString(R.string.details_format),
                    t.taskTitle, t.taskDescription, t.taskDate)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): MyViewHolder {
        val v = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.layout_item, viewGroup, false)
        return MyViewHolder(v)
    }

    override fun onBindViewHolder(holder: MyViewHolder, i: Int) {
        val task = mTasks[i]
        holder.cbComplete.isChecked = task.isTaskComplete
        holder.tvTask.text = task.taskTitle
    }

    override fun getItemCount(): Int = mTasks.size

    companion object {
        private lateinit var databaseHelper: DatabaseHelper
        private lateinit var mTasks: Array<Task>
        private lateinit var mListener: RefreshInterface
    }
}