package th.ac.rmutto.finlove

data class Place(
    val title: String,
    val description: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val imageResId: Int
)