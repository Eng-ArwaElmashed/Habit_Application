package com.android.habitapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.android.habitapplication.databinding.ActivityMainBinding
import com.android.habitapplication.ui.notifications.NotificationsFragment
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create all notification channels
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

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                R.id.nav_your_states,
                R.id.nav_challenges,
                R.id.nav_all_habits,
                R.id.nav_notification,
                R.id.nav_settings,
                R.id.nav_try_free
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // 🔄 Change AppBar color based on destination
        val appBarLayout = binding.appBarMain.toolbar
        val toolbarTitle = appBarLayout.findViewById<TextView>(R.id.toolbar_title)
        toolbarTitle.text = "Your Dynamic Title"
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val colorRes = when (destination.id) {
                R.id.nav_settings -> R.color.light_pink
                R.id.nav_profile -> R.color.main_pink
                R.id.nav_today -> R.color.light_pink
                R.id.nav_challenges -> R.color.main_pink

                R.id.nav_your_states -> R.color.main_pink

                R.id.nav_all_habits -> R.color.light_pink
                R.id.nav_notification -> R.color.light_pink

                R.id.nav_try_free-> R.color.main_pink
                else -> R.color.light_pink
            }
            toolbarTitle.text = when (destination.id) {
                R.id.nav_profile -> "Profile"
                R.id.nav_today-> "Today"
                R.id.nav_your_states-> "Your Status"
                R.id.nav_challenges-> "Challenges"
                R.id.nav_all_habits-> "All Habits"
                R.id.nav_notification-> "Notifications"
                R.id.nav_settings-> "Settings"
                R.id.nav_try_free-> "Subscription"

                else -> "Habit App"
            }

            appBarLayout.setBackgroundColor(ContextCompat.getColor(this, colorRes))
        }

        checkNotificationPermission()
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
