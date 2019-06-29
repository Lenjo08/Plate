package com.typicalgeek.plate;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.res.ResourcesCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements RefreshInterface{
    static Cursor res;
    static DatabaseHelper databaseHelper;
    RecyclerView recyclerView;
    SharedPreferences pref;
    SwipeRefreshLayout refreshLayout;
    TextView tvWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        pref = getSharedPreferences("preferences", 0);
        if (pref.getBoolean("darkTheme", false)){
            setTheme(R.style.AppTheme_Dark_NoActionBar);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolbarLayout = findViewById(R.id.toolbar_layout);
        toolbarLayout.setCollapsedTitleTypeface
                (ResourcesCompat.getFont(this, R.font.nunito_light));
        toolbarLayout.setExpandedTitleTypeface
                (ResourcesCompat.getFont(this, R.font.nunito_extralight));
        databaseHelper = new DatabaseHelper(this);
        final FloatingActionButton fab = findViewById(R.id.fab);
        refreshLayout = findViewById(R.id.srlPlateItems);
        recyclerView = findViewById(R.id.rvPlateItems);
        tvWelcome = findViewById(R.id.tvWelcome);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                final View newTaskView = inflater.inflate(R.layout.layout_task_entry,
                        (ViewGroup) view.getParent(), false);
                final EditText etTaskTitle = newTaskView.findViewById(R.id.etTaskTitle);
                final EditText etTaskDescription = newTaskView.findViewById(R.id.etTaskDescription);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.new_task)
                        .setView(newTaskView)
                        .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String title = etTaskTitle.getText().toString().trim();
                                String description = etTaskDescription.getText()
                                        .toString().trim();
                                if (validateInput(title, description)) {
                                        Task entry = buildTask(title, description);
                                        if (dbInsertTask(entry))
                                            Toast.makeText(MainActivity.this, R.string.success,
                                                    Toast.LENGTH_SHORT).show();
                                        else
                                            Toast.makeText(MainActivity.this, R.string.failed,
                                                    Toast.LENGTH_SHORT).show();
                                    refreshAll();
                                } else {
                                    Toast.makeText(MainActivity.this,
                                            R.string.empty_task_error,
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .setNeutralButton(R.string.cancel, null)
                        .create().show();
            }
        });
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshAll();
            }
        });
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) fab.hide();
                else if (dy < 0) fab.show();
            }
        });
        refreshAll();
    }

    static boolean validateInput(String taskTitle, String taskDescription) {
        return !taskTitle.isEmpty() || !taskDescription.isEmpty();
    }

    static boolean dbInsertTask(Task task) {
        return databaseHelper.insertTask(task);
    }

    static Task buildTask(String taskTitle, String taskDescription) {
        final String taskDate = new SimpleDateFormat("dd-MM-yyyy HH:mm",
                Locale.getDefault()).format(new Date());
        return buildTask(taskTitle, taskDescription, taskDate);
    }

    static Task buildTask(String taskTitle, String taskDescription, String taskDate) {
        return new Task(taskTitle, taskDescription, taskDate);
    }

    public void refreshAll() {
        refreshAll(false);
    }

    public void refreshAll(boolean shouldRefresh){
        if(!refreshLayout.isRefreshing()){
            if (shouldRefresh){
                refreshLayout.setRefreshing(true);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshLayout.setRefreshing(false);
                    }
                }, 2500);
            }
        } else new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshLayout.setRefreshing(false);
            }
        }, 2500);
        recyclerView.setHasFixedSize(true);
        PlateAdapter adapter = new PlateAdapter(this, dbGetItems());
        recyclerView.setAdapter(adapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(llm);
        int itemCount = databaseHelper.countCurrent(DatabaseHelper.TASKS_TABLE_NAME);
        String format = itemCount==1?getString(R.string.item):getString(R.string.items);
        tvWelcome.setText(String.format(Locale.getDefault(),
                getString(R.string.plate_item_count), itemCount, format));
    }

    private Task[] dbGetItems() {
        res = databaseHelper.getAllData(DatabaseHelper.TASKS_TABLE_NAME);
        Task[] tasks = new Task[res.getCount()];
        if (res != null && res.getCount() > 0) {
            int i = 0;
            while (res.moveToNext()) {
                tasks[i] = new Task(res.getInt(0),
                        res.getInt(1) == 1,
                        res.getString(2),
                        res.getString(3),
                        res.getString(4));
                i++;
            }
        }
        return tasks;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.action_toggle_theme:
                pref.edit().putBoolean(getString(R.string.dark_theme),
                        !pref.getBoolean(getString(R.string.dark_theme), false)).apply();
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            case R.id.action_refresh:
                refreshAll(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

interface RefreshInterface{
    void refreshAll();
}

/*abstract class AppBarStateChangeListener implements AppBarLayout.OnOffsetChangedListener{
    public enum State{
        EXPANDED,
        COLLAPSED,
        IDLE
    }

    private State mCurrentState = State.IDLE;

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        if (i==0){
            if(mCurrentState != State.EXPANDED){
                onStateChanged(appBarLayout, State.EXPANDED);
            }
            mCurrentState = State.EXPANDED;
        } else if (Math.abs(i) >= appBarLayout.getTotalScrollRange()){
            if(mCurrentState != State.COLLAPSED){
                onStateChanged(appBarLayout, State.COLLAPSED);
            }
            mCurrentState = State.COLLAPSED;
        } else {
            if(mCurrentState != State.IDLE){
                onStateChanged(appBarLayout, State.IDLE);
            }
            mCurrentState = State.IDLE;
        }
    }
    abstract void onStateChanged(AppBarLayout appBarLayout, State state);
}*/

class Task{
    private int taskID;
    private boolean taskComplete;
    private String taskTitle;
    private String taskDescription;
    private String taskDate;

    Task(int taskID, boolean taskComplete, String taskTitle, String taskDescription, String taskDate) {
        this.taskID = taskID;
        this.taskComplete = taskComplete;
        this.taskTitle = taskTitle;
        this.taskDescription = taskDescription;
        this.taskDate = taskDate;
    }

    Task(String taskTitle, String taskDescription, String taskDate) {
        this.taskTitle = taskTitle;
        this.taskDescription = taskDescription;
        this.taskDate = taskDate;
    }

    public int getTaskID() {
        return taskID;
    }

    public boolean isTaskComplete() {
        return taskComplete;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public String getTaskDate() {
        return taskDate;
    }
}

class PlateAdapter extends RecyclerView.Adapter<PlateAdapter.MyViewHolder>{
    private static DatabaseHelper databaseHelper;
    private static Task[] mTasks;
    private static RefreshInterface mListener;

    PlateAdapter(RefreshInterface myListener, Task[] myTasks){
        mListener = myListener;
        mTasks = myTasks;
    }

    static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private CheckBox cbComplete;
        private TextView tvTask;
        private ImageButton ibDelete;

        MyViewHolder(@NonNull final View itemView) {
            super(itemView);
            databaseHelper = new DatabaseHelper(itemView.getContext());
            cbComplete = itemView.findViewById(R.id.cbComplete);
            tvTask = itemView.findViewById(R.id.tvTask);
            ibDelete = itemView.findViewById(R.id.ibDelete);
            this.itemView.setOnClickListener(this);
            cbComplete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int checked = cbComplete.isChecked()?1:0;
                    databaseHelper.updateTaskStatus
                            (mTasks[getAdapterPosition()].getTaskID(), checked);
                    mListener.refreshAll();
                }
            });
            ibDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    databaseHelper.deleteTask(mTasks[getAdapterPosition()].getTaskID());
                    mListener.refreshAll();
                }
            });
        }

        @Override
        public void onClick(final View v) {
            final Task t = mTasks[getAdapterPosition()];
            LayoutInflater inflater = LayoutInflater.from(v.getContext());
            final View detailsView = inflater.inflate(R.layout.layout_details,
                    (ViewGroup) v.getParent(), false);
            final TextView tvTaskDetails = detailsView.findViewById(R.id.tvTaskDetails);
            new AlertDialog.Builder(v.getContext())
                    .setTitle(R.string.details)
                    .setView(detailsView)
                    .setPositiveButton(R.string.edit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LayoutInflater inflater = LayoutInflater.from(v.getContext());
                            final View editTaskView = inflater.inflate(R.layout.layout_task_entry,
                                    (ViewGroup) v.getParent(), false);
                            final EditText etTaskTitle = editTaskView.findViewById(R.id.etTaskTitle);
                            final EditText etTaskDescription = editTaskView.findViewById(R.id.etTaskDescription);
                            new AlertDialog.Builder(v.getContext())
                                    .setTitle(R.string.edit_task)
                                    .setView(editTaskView)
                                    .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            String title = etTaskTitle.getText().toString().trim();
                                            String description = etTaskDescription.getText()
                                                    .toString().trim();
                                            if (MainActivity.validateInput(title, description)) {
                                                Task edits = MainActivity.buildTask
                                                        (title, description, t.getTaskDate());
                                                databaseHelper.updateTask(t.getTaskID(), edits);
                                                mListener.refreshAll();
                                            }  else {
                                                Toast.makeText(v.getContext(),
                                                        R.string.empty_task_error,
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    })
                                    .setNeutralButton(R.string.cancel, null)
                                    .create().show();
                            etTaskTitle.setText(t.getTaskTitle());
                            etTaskDescription.setText(t.getTaskDescription());
                        }
                    })
                    .setNeutralButton(R.string.close, null)
                    .create().show();
            tvTaskDetails.setText(String.format(Locale.getDefault(), v.getContext().getString(R.string.details_format),
                    t.getTaskTitle(), t.getTaskDescription(), t.getTaskDate()));
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.layout_item, viewGroup, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int i) {
        Task task = mTasks[i];
        holder.cbComplete.setChecked(task.isTaskComplete());
        holder.tvTask.setText(task.getTaskTitle());
    }

    @Override
    public int getItemCount() {
        return mTasks.length;
    }
}

class DatabaseHelper extends SQLiteOpenHelper{
    private static final String DATABASE_NAME = "TasksDB";
    private static final int DATABASE_VERSION = 1;
    static final String TASKS_TABLE_NAME = "tasks_table";
    private static final String TASKS_COL_0 = "ID";
    private static final String TASKS_COL_1 = "COMPLETE";
    private static final String TASKS_COL_2 = "TITLE";
    private static final String TASKS_COL_3 = "DESCRIPTION";
    private static final String TASKS_COL_4 = "DATE";

    DatabaseHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TASKS_TABLE_NAME + "(" + TASKS_COL_0 + " INTEGER PRIMARY KEY, "
                + TASKS_COL_1 + " INTEGER, " + TASKS_COL_2 + " TEXT, " + TASKS_COL_3 + " TEXT, " +
                TASKS_COL_4 + " TEXT, " + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        refreshDB();
    }

    public boolean insertTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(TASKS_COL_1, 0);
        contentValues.put(TASKS_COL_2, task.getTaskTitle());
        contentValues.put(TASKS_COL_3, task.getTaskDescription());
        contentValues.put(TASKS_COL_4, task.getTaskDate());
        long result = db.insert(TASKS_TABLE_NAME, null, contentValues);
        db.close();
        return result != -1;
    }

    public Cursor getAllData(String TABLE_NAME){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY " + TASKS_COL_1 + ", " +
                TASKS_COL_4 + " ASC", null);
    }

    /*public int countAll(String TABLE_NAME){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME, null).getCount();
    }*/

    public int countCurrent(String TABLE_NAME){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + TASKS_COL_1 + " = " + 0, null).getCount();
    }

    public void deleteTask(int ID){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TASKS_TABLE_NAME + " WHERE " + TASKS_COL_0 + " = " + ID);
    }

    private void refreshDB() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TASKS_TABLE_NAME);
        onCreate(db);
    }

    public void updateTask(int ID, Task task){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TASKS_TABLE_NAME + " SET " + TASKS_COL_2 + " = \'" +
                task.getTaskTitle() + "\', " + TASKS_COL_3 + " = \'" + task.getTaskDescription() +
                "\' WHERE " + TASKS_COL_0 + " = " + ID);
    }

    public void updateTaskStatus(int ID, int isComplete) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TASKS_TABLE_NAME + " SET " + TASKS_COL_1 + " = " + isComplete +
                " WHERE " + TASKS_COL_0 + " = " + ID);
    }
}