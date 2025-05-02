package com.android.habitapplication

import android.os.Bundle
import android.view.Menu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.android.habitapplication.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
