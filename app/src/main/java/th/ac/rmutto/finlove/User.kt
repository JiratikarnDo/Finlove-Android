package th.ac.rmutto.finlove

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName(value = "UserID", alternate = ["userID", "id"])  // ← เพิ่มบรรทัด
    val id: Int = 0,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val nickname: String,
    val gender: String,
    val interestGender: String,
    val height: Double,
    val home: String,
    @SerializedName("DateBirth") val dateBirth: String,
    val education: String,
    val goal: String,
    val imageFile: String,
    @JsonAdapter(StringListAdapter::class)
    val preferences: List<String> = emptyList(),
    val verify: Int,

    // ✅ เพิ่มฟิลด์พิกัดตำแหน่งของผู้ใช้
    val latitude: Double,
    val longitude: Double
)

