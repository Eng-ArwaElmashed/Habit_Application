package com.android.habitapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Menu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.android.habitapplication.databinding.ActivityMainBinding
import com.android.habitapplication.ui.notifications.NotificationsFragment
import com.google.android.material.navigation.NavigationView
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the toolbar
        setSupportActionBar(binding.appBarMain.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Top level destinations
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_profile,
                R.id.nav_today,
                R.id.nav_all_habits,
                R.id.nav_notification,
                R.id.nav_settings,
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Set up notification channels and check scheduling
        setupNotifications()
        
        // Set up navigation UI
        setupNavigationUI(navController)
    }

    private fun setupNavigationUI(navController: NavController) {
        val appBarLayout = binding.appBarMain.toolbar
        val toolbarTitle = appBarLayout.findViewById<TextView>(R.id.toolbar_title)
        toolbarTitle.text = "Your Dynamic Title"
        
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val colorRes = when (destination.id) {
                R.id.nav_settings -> R.color.light_pink
                R.id.nav_profile -> R.color.main_pink
                R.id.nav_today -> R.color.light_pink
                R.id.nav_all_habits -> R.color.light_pink
                R.id.nav_notification -> R.color.light_pink
                else -> R.color.light_pink
            }
            
            toolbarTitle.text = when (destination.id) {
                R.id.nav_profile -> "Profile"
                R.id.nav_today -> "Today"
                R.id.nav_all_habits -> "All Habits"
                R.id.nav_notification -> "Notifications"
                R.id.nav_settings -> "Settings"
                else -> "Habit App"
            }

            appBarLayout.setBackgroundColor(ContextCompat.getColor(this, colorRes))
        }
    }

    private fun setupNotifications() {
        // Create notification channels first
        createNotificationChannels()
        
        // Check notification permissions
        checkNotificationPermission()

        // Get current time and user preferences
        val prefs = getSharedPreferences("user_times", Context.MODE_PRIVATE)
        val wakeHour = prefs.getInt("wakeHour", 8)
        val wakeMinute = prefs.getInt("wakeMinute", 0)
        val sleepHour = prefs.getInt("sleepHour", 22)
        val sleepMinute = prefs.getInt("sleepMinute", 0)

        val cal = Calendar.getInstance()
        val now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val wake = wakeHour * 60 + wakeMinute
        val sleep = sleepHour * 60 + sleepMinute

        // Check if we're within active hours
        if (now in wake until sleep) {
            // Schedule notifications if we're in active hours
            NotificationScheduler.scheduleRandomNotifications(this)
            Log.d("MainActivity", "Scheduled notifications during active hours")
        } else {
            // If outside active hours, schedule for next wake time
            cal.apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, wakeHour)
                set(Calendar.MINUTE, wakeMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            Log.d("MainActivity", "Outside active hours, notifications will start at next wake time")
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    NotificationsFragment.CHANNEL_ID,
                    "Habit Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for habit reminders"
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                },
                NotificationChannel(
                    "wake_channel",
                    "Wake Up Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for wake up reminders"
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                },
                NotificationChannel(
                    "sleep_channel",
                    "Sleep Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for sleep reminders"
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                }
            )
            
            val manager = getSystemService(NotificationManager::class.java)
            channels.forEach { manager.createNotificationChannel(it) }
            Log.d("MainActivity", "Created all notification channels")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with notifications
            } else {
                Toast.makeText(this, "Notification permission is required for reminders", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 123
    }

}
