package th.ac.rmutto.finlove

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.badge.BadgeDrawable
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.ViewGroup
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import th.ac.rmutto.finlove.utils.AnimationHelper

class MainActivity : AppCompatActivity() {
    private var userID: Int = -1

    private lateinit var sharedPref: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        // *** Initialize sharedPref ที่นี่ ***
        sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        val rootView = findViewById<android.view.View>(R.id.root_layout)
        rootView.setOnApplyWindowInsetsListener { view, insets ->
            val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets)
            val statusBarHeight = insetsCompat.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(0, statusBarHeight, 0, 0)
            insets
        }

        userID = intent.getIntExtra("userID", -1)
        if (userID == -1) {
            Toast.makeText(this, "ไม่พบ userID", Toast.LENGTH_LONG).show()
            return
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.nav_view)
        bottomNavigationView.setOnApplyWindowInsetsListener { view, insets ->
            val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets)
            val navBarHeight = insetsCompat.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.updatePadding(bottom = navBarHeight)
            insets
        }

        bottomNavigationView.setupWithNavController(navController)

        val bundle = Bundle().apply {
            putInt("userID", userID)
        }
        navController.navigate(R.id.navigation_home, bundle)

        bottomNavigationView.setOnItemSelectedListener { menuItem ->

            val selectedItemView = bottomNavigationView.findViewById<ViewGroup>(menuItem.itemId)

            selectedItemView?.let {
                AnimationHelper.animateViewAppear(it) // หรือใช้ animateButtonPressBounceRotate ถ้าเป็น ImageButton
            }

            val bundle = Bundle().apply {
                putInt("userID", userID)
            }
            when (menuItem.itemId) {
                R.id.navigation_profile -> {
                    if (navController.currentDestination?.id != R.id.navigation_profile) {
                        navController.navigate(R.id.navigation_profile, bundle)
                    }
                    true
                }
                R.id.navigation_home -> {
                    if (navController.currentDestination?.id != R.id.navigation_home) {
                        navController.navigate(R.id.navigation_home, bundle)
                    }
                    true
                }
                R.id.navigation_message -> {
                    if (navController.currentDestination?.id != R.id.navigation_message) {
                        navController.navigate(R.id.navigation_message, bundle)
                    }
                    true
                }
                R.id.navigation_viewuser -> {
                    if (navController.currentDestination?.id != R.id.navigation_viewuser) {
                        navController.navigate(R.id.navigation_viewuser, bundle)
                    }
                    true
                }
                else -> false
            }
        }

        // ซ่อน/แสดงเมนู Settings ตามหน้า
        val topAppBar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(topAppBar)  // <-- เพิ่มบรรทัดนี้

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isHome = destination.id == R.id.navigation_home || destination.id == R.id.navigation_profile || destination.id == R.id.navigation_message || destination.id == R.id.navigation_viewuser
            topAppBar.menu.findItem(R.id.action_settings)?.isVisible = isHome
        }

    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume called.")
    }

    // *** เมธอด onDestroy() ต้องอยู่ตรงนี้ (นอก onCreate()) ***
    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy called.")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_appbar_menu, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val bundle = Bundle().apply {
                    putInt("userID", userID)
                }
                val navHostFragment =
                    supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
                val navController = navHostFragment.navController

                if (navController.currentDestination?.id != R.id.settingsFragment) {
                    navController.navigate(R.id.settingsFragment, bundle)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    }


