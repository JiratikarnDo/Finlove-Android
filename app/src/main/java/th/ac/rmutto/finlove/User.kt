package th.ac.rmutto.finlove

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("userID") val id: Int,  // ← เพิ่มบรรทัด
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
    val preferences: String?,
    val verify: Int,

    // ✅ เพิ่มฟิลด์พิกัดตำแหน่งของผู้ใช้
    val latitude: Double,
    val longitude: Double
)

