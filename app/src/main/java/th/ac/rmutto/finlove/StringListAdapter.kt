package th.ac.rmutto.finlove

import com.google.gson.*
import java.lang.reflect.Type

class StringListAdapter : JsonDeserializer<List<String>> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): List<String> {
        return when {
            // ✅ ถ้าเป็น Array
            json.isJsonArray -> json.asJsonArray
                .mapNotNull { if (it.isJsonPrimitive) it.asString else null }

            // ✅ ถ้าเป็น null
            json.isJsonNull -> emptyList()

            // ✅ ถ้าเป็น String ธรรมดา
            json.isJsonPrimitive -> json.asString
                .split(",", "、", " ") // กันไว้เผื่อ backend ใช้ตัวคั่นหลายแบบ
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            else -> emptyList()
        }
    }
}