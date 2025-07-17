package th.ac.rmutto.finlove

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import th.ac.rmutto.finlove.R
import th.ac.rmutto.finlove.ui.Dateing.DatingPlaceFragment

class DatingPlaceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dating_place)

        val fragment = DatingPlaceFragment()
        val bundle = Bundle().apply {
            putInt("matchID", intent.getIntExtra("matchID", -1))
            putInt("senderID", intent.getIntExtra("senderID", -1))
            putString("nickname", intent.getStringExtra("nickname"))
        }
        fragment.arguments = bundle

        // ✅ ต้องตรงกับ ID ของ container ใน XML เช่น R.id.datingPlaceContainer
        supportFragmentManager.beginTransaction()
            .replace(R.id.datingPlaceContainer, fragment)
            .commit()
    }
}

