@file:JvmName("MainActivity")

package com.typicalgeek.plate

import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), RefreshInterface {
    private lateinit var recyclerView: RecyclerView
    private lateinit var pref: SharedPreferences
    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var tvWelcome: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
            pref = getSharedPreferences("preferences", 0)
            if (pref.getBoolean("darkTheme", false)) setTheme(R.style.AppTheme_Dark_NoActionBar)
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            val toolbarLayout = findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout)
            toolbarLayout.setCollapsedTitleTypeface(ResourcesCompat.getFont(this, R.font.nunito_light))
            toolbarLayout.setExpandedTitleTypeface(ResourcesCompat.getFont(this, R.font.nunito_extralight))
            databaseHelper = DatabaseHelper(this)
            val fab = findViewById<FloatingActionButton>(R.id.fab)
            refreshLayout = findViewById(R.id.srlPlateItems)
            recyclerView = findViewById(R.id.rvPlateItems)
            tvWelcome = findViewById(R.id.tvWelcome)
            fab.setOnClickListener { view ->
                val inflater = LayoutInflater.from(this@MainActivity)
                val newTaskView = inflater.inflate(R.layout.layout_task_entry,
                        view.parent as ViewGroup, false)
                val etTaskTitle = newTaskView.findViewById<EditText>(R.id.etTaskTitle)
                val etTaskDescription = newTaskView.findViewById<EditText>(R.id.etTaskDescription)
                AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.new_task)
                        .setView(newTaskView)
                        .setPositiveButton(R.string.save) { _, _ ->
                            val title = etTaskTitle.text.toString().trim { it <= ' ' }
                            val description = etTaskDescription.text
                                    .toString().trim { it <= ' ' }
                            if (validateInput(title, description)) {
                                val entry = buildTask(title, description)
                                if (dbInsertTask(entry))
                                    Toast.makeText(this@MainActivity, R.string.success,
                                            Toast.LENGTH_SHORT).show()
                                else
                                    Toast.makeText(this@MainActivity, R.string.failed,
                                            Toast.LENGTH_SHORT).show()
                                refreshAll()
                            } else Toast.makeText(this@MainActivity,
                                        R.string.empty_task_error,
                                        Toast.LENGTH_LONG).show()
                        }
                        .setNeutralButton(R.string.cancel, null)
                        .create().show()
            }
            refreshLayout.setOnRefreshListener{refreshAll()}
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) fab.hide()
                    else if (dy < 0) fab.show()
                }
            })
            refreshAll()
    }

    override fun refreshAll(shouldRefresh: Boolean) {
        if (!refreshLayout.isRefreshing){
            if (shouldRefresh) refreshLayout.isRefreshing = true
            Handler().postDelayed({refreshLayout.isRefreshing = false}, 2500)
        } else Handler().postDelayed({refreshLayout.isRefreshing = false}, 2500)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = PlateAdapter(this, dbGetItems())
        recyclerView.layoutManager = LinearLayoutManager(this)
        val itemCount = databaseHelper.countCurrent(DatabaseHelper.TASKS_TABLE_NAME)
        val format = if (itemCount == 1) getString(R.string.item) else getString(R.string.items)
        tvWelcome.text = String.format(Locale.getDefault(),
                getString(R.string.plate_item_count), itemCount, format)
    }

    private fun dbGetItems(): Array<Task> {
        val res = databaseHelper.getAllData(DatabaseHelper.TASKS_TABLE_NAME)
        val tasks: Array<Task> = Array(res.count) {Task()}
        if (res.count > 0) {
            var i = 0
            while (res.moveToNext()) {
                tasks[i] = Task(res.getInt(0), res.getInt(1) == 1, res.getString(2), res.getString(3), res.getString(4))
                i++
            }
        }
        return tasks
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_theme -> {
                pref.edit().putBoolean(getString(R.string.dark_theme),
                        !pref.getBoolean(getString(R.string.dark_theme), false)).apply()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                true
            }
            R.id.action_refresh -> {
                refreshAll(true)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        internal var res: Cursor? = null
        internal lateinit var databaseHelper: DatabaseHelper

        internal fun validateInput(taskTitle: String, taskDescription: String): Boolean
            = taskTitle.isNotEmpty() || taskDescription.isNotEmpty()


        internal fun dbInsertTask(task: Task): Boolean
            = databaseHelper.insertTask(task)

        internal fun buildTask(taskTitle: String, taskDescription: String,
                               taskDate: String = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date()) ): Task
            = Task(taskTitle = taskTitle, taskDescription = taskDescription, taskDate = taskDate)
    }
}