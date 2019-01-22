package com.example.pwiktorek.learntime


import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.text.SimpleDateFormat
import java.util.*


private const val KEY_OVERALL_TIME = "OVERALL_TIME"
private const val KEY_TIMER_IS_RUNNING = "TIMER_IS_RUNNING"
private const val KEY_INTERVAL_START_TIME = "INTERVAL_START_TIME"
private const val KEY_TOTAL_TIME = "TOTAL_TIME"
private const val KEY_INTERVALS = "INTERVALS"
private const val KEY_STOP_TIME = "STOP_TIME"
private const val KEY_NOTIFICATION_COUNTER = "NOTIFICATION_COUNTER"
private const val KEY_GOAL_TIME = "GOAL_TIME"
private const val KEY_REMINDER_TIME = "REMINDER_TIME"
private const val SHARED_PREFERENCES_NAME = "MY_SHARED_PREFERENCES"

private const val TIME_PATTERN = "HH:mm:ss"
private const val LEARN_TIME_MIN_VALUE = 1
private const val LEARN_TIME_MAX_VALUE = 16
private const val REMINDER_TIME_MIN_VALUE = 30
private const val REMINDER_TIME_MAX_VALUE = 120
private const val DEFAULT_LEARN_TIME = 8 //8hours
private const val DEFAULT_REMINDER_TIME = 60 // 60 minutes
private const val CHANNEL_ID = "LearnTime"
private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {


    var timerIsRunning = false
    var totalTime = 0
    var intervals = ""
    var intervalStartTime = 0L
    var stopTime = 0L
    var overallTime = 0
    var notificationCounter = 0
    var goalTime = DEFAULT_LEARN_TIME
    var reminderTime = DEFAULT_REMINDER_TIME


    lateinit var sharedPreferences: SharedPreferences
    var timer: Timer? = null


    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        createNotificationChannel()
        sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Activity.MODE_PRIVATE)
        //sharedPreferences.edit().clear().apply()


        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        loadPreferences()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val mBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pause)
            .setContentTitle(resources.getString(R.string.notification_title))
            .setContentText(resources.getString(R.string.notification_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)



        supportActionBar?.setIcon(R.drawable.ic_timer)
        progressBar.max = goalTime * 3600//seconds


        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {

                    if (timerIsRunning) {
                        totalTime++
                        notificationCounter++
                        if ((notificationCounter / 60) == reminderTime) {
                            notificationCounter = 0
                            with(NotificationManagerCompat.from(applicationContext)) {
                                notify(System.currentTimeMillis().toInt(), mBuilder.build())
                            }

                        }
                        tv_timer.text = secondsToTime(totalTime) //hours,Min and Second with am/pm
                        progressBar.progress = totalTime
                        Log.d(TAG, (totalTime).toString())
                    }


                }
            }
        }, 0, 1000)//1000 is a Refreshing Time (1second)


        btn_start.setOnClickListener { view ->
            when (timerIsRunning) {

                true -> {
                    timerIsRunning = false
                    notificationCounter = 0
                    stopTime = System.currentTimeMillis()
                    btn_start.setImageResource(R.drawable.ic_start)
                    tv_intervals_list.text = addInterval(intervalStartTime, stopTime)
                }
                false -> {
                    timerIsRunning = true
                    intervalStartTime = System.currentTimeMillis()
                    btn_start.setImageResource(R.drawable.ic_pause)
                }
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> {
                val alertDialog = AlertDialog.Builder(this)
                alertDialog.setTitle(R.string.settings_dialog_title)
                alertDialog.setMessage(R.string.settings_dialog_message)

                val alertLayout = layoutInflater.inflate(R.layout.settings_dialog, null)
                alertDialog.setView(alertLayout)

                val seekBarLearnTime = alertLayout.findViewById<SeekBar>(R.id.seekBarLearnTime)
                val seekBarReminderTime = alertLayout.findViewById<SeekBar>(R.id.seekBarReminderTime)
                val tvLearnTime = alertLayout.findViewById<TextView>(R.id.tvLearnTime)
                val tvReminderTime = alertLayout.findViewById<TextView>(R.id.tvReminderTime)


                tvLearnTime.text = goalTime.toString()
                var realProgress = goalTime
                seekBarLearnTime.progress = (realProgress - LEARN_TIME_MIN_VALUE) * 100 /
                        (LEARN_TIME_MAX_VALUE - LEARN_TIME_MIN_VALUE)

                seekBarLearnTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                        realProgress = (progress * (LEARN_TIME_MAX_VALUE - LEARN_TIME_MIN_VALUE) / 100) +
                                LEARN_TIME_MIN_VALUE
                        tvLearnTime.text = realProgress.toString()
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    }
                })


                tvReminderTime.text = reminderTime.toString()
                var realReminderProgress = reminderTime
                seekBarReminderTime.progress = (realReminderProgress - REMINDER_TIME_MIN_VALUE) * 100 /
                        (REMINDER_TIME_MAX_VALUE - REMINDER_TIME_MIN_VALUE)
                seekBarReminderTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        realReminderProgress = (progress * (REMINDER_TIME_MAX_VALUE - REMINDER_TIME_MIN_VALUE) / 100) +
                                REMINDER_TIME_MIN_VALUE
                        tvReminderTime.text = realReminderProgress.toString()
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    }
                })



                alertDialog.run {
                    setPositiveButton("Ok") { dialog, whichButton ->
                        goalTime = realProgress
                        progressBar.max = realProgress * 3600
                        progressBar.progress = totalTime
                        reminderTime = realReminderProgress
                    }
                    setNegativeButton("Cancel") { dialog, whichButton ->
                        // Canceled.
                    }
                    show()
                }
            }
            R.id.action_clear -> {
                overallTime = sharedPreferences.getInt(KEY_OVERALL_TIME, 0)
                overallTime += totalTime
                goalTime = sharedPreferences.getInt(KEY_GOAL_TIME, 0)
                reminderTime = sharedPreferences.getInt(KEY_REMINDER_TIME, 0)
                sharedPreferences
                    .edit()
                    .clear()
                    .putInt(KEY_OVERALL_TIME, overallTime)
                    .putInt(KEY_GOAL_TIME, goalTime)
                    .putInt(KEY_REMINDER_TIME, reminderTime)
                    .putInt(KEY_NOTIFICATION_COUNTER, 0)
                    .apply()
                loadPreferences()

            }
            R.id.action_info -> {
                Toast.makeText(
                    this,
                    "Total learn time with this app is: ${com.example.pwiktorek.learntime.secondsToTime(overallTime + totalTime)}",
                    Toast.LENGTH_LONG
                ).show()


            }

            else -> super.onOptionsItemSelected(item)
        }
        return true
    }


    override fun onPause() {
        super.onPause()
        stopTime = System.currentTimeMillis()
        sharedPreferences
            .edit()
            .putInt(KEY_TOTAL_TIME, totalTime)
            .putString(KEY_INTERVALS, intervals)
            .putLong(KEY_STOP_TIME, stopTime)
            .putLong(KEY_INTERVAL_START_TIME, intervalStartTime)
            .putBoolean(KEY_TIMER_IS_RUNNING, timerIsRunning)
            .putInt(KEY_OVERALL_TIME, overallTime)
            .putInt(KEY_GOAL_TIME, goalTime)
            .putInt(KEY_REMINDER_TIME, reminderTime)
            .putInt(KEY_NOTIFICATION_COUNTER, notificationCounter)
            .apply()
    }

    override fun onResume() {
        super.onResume()
        loadPreferences()

    }


    private fun addInterval(start: Long, stop: Long): String {
        intervals += ((SimpleDateFormat(TIME_PATTERN).format(start)) + "  --->  " + (SimpleDateFormat(TIME_PATTERN).format(
            stop
        )) + "\n")
        return intervals
    }


    private fun loadPreferences() {

        totalTime = sharedPreferences.getInt(KEY_TOTAL_TIME, 0)
        timerIsRunning = sharedPreferences.getBoolean(KEY_TIMER_IS_RUNNING, false)
        intervals = sharedPreferences.getString(KEY_INTERVALS, "")
        stopTime = sharedPreferences.getLong(KEY_STOP_TIME, 0)
        intervalStartTime = sharedPreferences.getLong(KEY_INTERVAL_START_TIME, 0)
        overallTime = sharedPreferences.getInt(KEY_OVERALL_TIME, 0)
        goalTime = sharedPreferences.getInt(KEY_GOAL_TIME, DEFAULT_LEARN_TIME)
        reminderTime = sharedPreferences.getInt(KEY_REMINDER_TIME, DEFAULT_REMINDER_TIME)
        notificationCounter = sharedPreferences.getInt(KEY_NOTIFICATION_COUNTER, 0)

        if (timerIsRunning) {
            btn_start.setImageResource(R.drawable.ic_pause)
            Log.d(TAG, "stop time: $stopTime")
            totalTime += ((System.currentTimeMillis() - stopTime) / 1000).toInt()
        } else {
            btn_start.setImageResource(R.drawable.ic_start)
        }
        Log.d(TAG, "Loading preferences")
        tv_timer.text = secondsToTime(totalTime)
        progressBar.max = goalTime * 3600
        progressBar.progress = totalTime
        tv_intervals_list.text = intervals


    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }

    override fun onBackPressed() {
        moveTaskToBack(false)
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Support for Android Oreo: Notification Channels
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LearTime notification",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
    }


}

fun secondsToTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return (String.format("%02d : %02d : %02d", hours, minutes, seconds % 60))
}


