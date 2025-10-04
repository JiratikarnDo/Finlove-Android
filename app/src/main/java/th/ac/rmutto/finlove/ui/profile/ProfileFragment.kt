package th.ac.rmutto.finlove.ui.profile

import android.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import th.ac.rmutto.finlove.AddphotoActivity
import th.ac.rmutto.finlove.ChangePreferenceActivity
import th.ac.rmutto.finlove.FirstPageActivity
import th.ac.rmutto.finlove.R
import th.ac.rmutto.finlove.User
import java.util.*
import androidx.navigation.fragment.findNavController
import androidx.appcompat.widget.AppCompatButton
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import android.graphics.Rect
import android.widget.ScrollView
import android.graphics.drawable.GradientDrawable
import android.animation.ObjectAnimator
import android.animation.AnimatorSet
import android.graphics.Color
import android.view.animation.OvershootInterpolator

class ProfileFragment : Fragment() {

    private lateinit var textViewUsername: EditText
    private lateinit var textViewNickname: EditText
    private lateinit var textViewEmail: EditText
    private lateinit var textViewFirstName: EditText
    private lateinit var textViewLastName: EditText
    private lateinit var spinnerGender: Spinner
    private lateinit var textViewHeight: EditText
    private lateinit var textViewWeight: EditText
    private lateinit var textViewHome: EditText
    private lateinit var textViewBio: EditText
    private lateinit var buttonSelectDateProfile: Button
    private lateinit var imageViewProfile: ImageView
    private lateinit var spinnerInterestGender: Spinner
    private lateinit var spinnerEducation: Spinner
    private lateinit var spinnerGoal: Spinner
    private lateinit var preferenceContainer: LinearLayout
    private lateinit var verifyBadge: ImageView
    private lateinit var labelUsername: TextView
    private lateinit var labelEmail: TextView
    private lateinit var labelEducation: TextView
    private lateinit var labelGoal: TextView
    private lateinit var labelHeight: TextView
    private lateinit var labelWeight: TextView
    private lateinit var labelHome: TextView
    private lateinit var labelInterest: TextView
    private lateinit var labelDate: TextView
    private lateinit var labelCareer: TextView
    private lateinit var labelProvince: TextView
    private lateinit var spinnerProvince: Spinner
    private lateinit var spinnerCareer: Spinner
    private lateinit var careerNames: Array<String>
    private lateinit var careerIds: Array<String>
    private lateinit var labelCompleteness: TextView
    private lateinit var pbCompleteness: ProgressBar
    private lateinit var txtCompleteness: TextView

    private lateinit var user: User // ประกาศตัวแปร user ที่คลาส level


    private lateinit var buttonEditProfile: ImageButton
    private lateinit var buttonSaveProfile: Button
    private lateinit var buttonEditPreferences: Button
    private lateinit var buttonVerify: Button
    private lateinit var buttonLogout: Button
    private lateinit var buttonDeleteAccount: Button
    private lateinit var provinceList: List<String>
    private var selectedImageUri: Uri? = null
    private var selectedDateOfBirth: String? = null
    private var isEditing = false
    private val PICK_IMAGE_REQUEST = 1
    private val REQUEST_CODE_CHANGE_PREFERENCES = 1001

    // ด้านบนในคลาส ProfileFragment
    private var pendingOpenSection: String? = null
    private var pendingEditMode: Boolean = false

    private lateinit var originalUser: User
    private lateinit var currentUser: User

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize views
        initializeViews(root)

        labelCompleteness = root.findViewById(R.id.labelCompleteness)
        pbCompleteness    = root.findViewById(R.id.profileCompletenessBar)
        txtCompleteness   = root.findViewById(R.id.txtCompleteness)

// โชว์ตั้งแต่แรก และให้เป็น indeterminate จนกว่าจะได้คะแนนจริง
        labelCompleteness.visibility = View.VISIBLE
        pbCompleteness.visibility    = View.VISIBLE
        txtCompleteness.visibility   = View.VISIBLE

        pbCompleteness.isIndeterminate = true
        txtCompleteness.text = "กำลังประเมิน…"

        // ✅ รับพารามิเตอร์นำทางจากหน้า Help
        pendingEditMode   = arguments?.getBoolean("edit_mode") == true
        pendingOpenSection = arguments?.getString("open_section")

        if (pendingEditMode) {
            // จะเปิดแก้ไขทันทีไหม? (ถ้าอยากไฮไลต์เฉยๆ ไม่ต้องเปิดก็ได้)
            isEditing = true
            setEditingEnabled(true)
            showAllFields()
            textViewBio.isEnabled = true

            // ✅ เอาปุ่มที่ hide กลับมาให้เหมือน toggleEditMode()
            buttonSaveProfile.visibility = View.VISIBLE
            buttonEditPreferences.visibility = View.VISIBLE
            buttonDeleteAccount.visibility = View.VISIBLE
        }

        // Fetch user ID from intent
        val userID = requireActivity().intent.getIntExtra("userID", -1)
        Log.d("ProfileFragment", "Received userID: $userID")

        if (userID != -1) {
            fetchUserInfo(userID)
            fetchProfileCompleteness(userID)
        } else {
            Toast.makeText(requireContext(), "ไม่พบ userID", Toast.LENGTH_LONG).show()
        }

        return root
    }

    private fun initializeViews(root: View) {

        val btnRecommend = root.findViewById<AppCompatButton>(R.id.buttonreccomend)
        btnRecommend.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_helpNavigator)
        }
        // การกำหนดค่าเริ่มต้นสำหรับ Views ต่าง ๆ
        textViewUsername = root.findViewById(R.id.textViewUsername)
        textViewNickname = root.findViewById(R.id.textViewNickname)
        textViewEmail = root.findViewById(R.id.textViewEmail)
        textViewFirstName = root.findViewById(R.id.textViewFirstName)
        textViewLastName = root.findViewById(R.id.textViewLastName)
        spinnerGender = root.findViewById(R.id.spinnerGender)
        spinnerInterestGender = root.findViewById(R.id.spinnerInterestGender)
        textViewHeight = root.findViewById(R.id.textViewHeight)
        textViewWeight = root.findViewById(R.id.textViewWeight)
        textViewHome = root.findViewById(R.id.textViewHome)
        textViewBio = root.findViewById(R.id.edtBio)
        labelUsername = root.findViewById(R.id.labelUsername)
        labelEmail = root.findViewById(R.id.labelEmail)
        labelEducation = root.findViewById(R.id.labelEducation)
        labelGoal = root.findViewById(R.id.labelGoal)
        labelHeight = root.findViewById(R.id.labelHeight)
        labelWeight = root.findViewById(R.id.labelWeight)
        labelHome = root.findViewById(R.id.labelHome)
        labelInterest = root.findViewById(R.id.labelInterest)
        labelDate = root.findViewById(R.id.labelDate)
        labelCareer = root.findViewById(R.id.labelCareer)
        labelProvince = root.findViewById(R.id.labelProvince)
        buttonSelectDateProfile = root.findViewById(R.id.buttonSelectDateProfile)
        imageViewProfile = root.findViewById(R.id.imageViewProfile)
        spinnerEducation = root.findViewById(R.id.spinnerEducation)
        spinnerGoal = root.findViewById(R.id.spinnerGoal)
        spinnerProvince = root.findViewById(R.id.spinnerProvince)
        spinnerCareer = root.findViewById(R.id.spinnerCareer)
        preferenceContainer = root.findViewById(R.id.preferenceContainer)

        buttonEditProfile = root.findViewById(R.id.buttonEditProfile)
        buttonSaveProfile = root.findViewById(R.id.buttonSaveProfile)
        buttonLogout = root.findViewById(R.id.buttonLogout)
        buttonDeleteAccount = root.findViewById(R.id.buttonDeleteAccount)
        buttonEditPreferences = root.findViewById(R.id.buttonEditPreferences)
        buttonVerify = root.findViewById(R.id.buttonVerify)
        verifyBadge = root.findViewById(R.id.verifyBadge)

        textViewBio.isEnabled = false

        // Initialize Toolbar Views
        val toolbar = root.findViewById<Toolbar>(R.id.toolbarProfile)
        val toolbarTitle = toolbar.findViewById<TextView>(R.id.toolbarTitle)
        val verifyBadgeNickname = toolbar.findViewById<ImageView>(R.id.verifyBadgeNickname)

        setupSpinners()
        hideFieldsForViewingMode()

        // ตั้งค่าให้ฟิลด์ต่าง ๆ ไม่สามารถแก้ไขได้เมื่อเปิดหน้าครั้งแรก
        setEditingEnabled(false)

        // ตั้งค่า Click Listener สำหรับรูปโปรไฟล์และปุ่มอื่นๆ
        imageViewProfile.setOnClickListener {
            selectImage()
        }
        buttonEditProfile.setOnClickListener {
            toggleEditMode()
        }
        buttonSaveProfile.setOnClickListener {
            saveUserInfo(requireActivity().intent.getIntExtra("userID", -1))
        }
        buttonDeleteAccount.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("ยืนยันการลบบัญชี")
                .setMessage("คุณแน่ใจหรือไม่ว่าต้องการลบบัญชี? การกระทำนี้ไม่สามารถย้อนกลับได้")
                .setPositiveButton("ยืนยัน") { _, _ ->
                    deleteUser(requireActivity().intent.getIntExtra("userID", -1))
                }
                .setNegativeButton("ยกเลิก", null)
                .show()
        }
        buttonLogout.setOnClickListener {
            logoutUser(requireActivity().intent.getIntExtra("userID", -1))
        }
        buttonEditPreferences.setOnClickListener {
            val intent = Intent(requireContext(), ChangePreferenceActivity::class.java)
            intent.putExtra("userID", requireActivity().intent.getIntExtra("userID", -1))
            startActivityForResult(intent, REQUEST_CODE_CHANGE_PREFERENCES)
        }
        buttonVerify.setOnClickListener {
            val intent = Intent(requireContext(), AddphotoActivity::class.java)
            intent.putExtra("userID", requireActivity().intent.getIntExtra("userID", -1))
            startActivity(intent)
        }
        buttonSelectDateProfile.setOnClickListener {
            showDatePicker()
        }
    }

    private fun setupSpinners() {
        val educationAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.education_levels,
            android.R.layout.simple_spinner_item
        )
        educationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEducation.adapter = educationAdapter

        val goalAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.goal_options,
            android.R.layout.simple_spinner_item
        )
        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGoal.adapter = goalAdapter

        val genderDisplay = resources.getStringArray(R.array.gender_display_array)
        val genderAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, genderDisplay)
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGender.adapter = genderAdapter

        val interestGenderDisplay = resources.getStringArray(R.array.interest_gender_display_array)
        val interestGenderAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, interestGenderDisplay)
        interestGenderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerInterestGender.adapter = interestGenderAdapter

        // Spinner จังหวัด
        // Spinner จังหวัด (มี placeholder แถวแรก และทำให้แถวแรกกดไม่ได้)
        val provinces = resources.getStringArray(R.array.province_display_array).toMutableList()
        provinces.add(0, "กรุณาเลือกจังหวัด")

        val provinceAdapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            provinces
        ) {
            override fun isEnabled(position: Int): Boolean = position != 0
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent) as TextView
                v.setTextColor(if (position == 0) Color.GRAY else Color.BLACK)
                return v
            }
        }
        provinceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProvince.adapter = provinceAdapter
        // ⬅️ เก็บลิสต์ไว้ใช้นอกฟังก์ชัน
        provinceList = provinces

        // ===== Career (อาชีพ) =====
        careerNames = resources.getStringArray(R.array.career_name_array)
        careerIds   = resources.getStringArray(R.array.career_id_array)

        val careerAdapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, careerNames
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerCareer.adapter = careerAdapter
    }

    private fun getSelectedCareerId(): Int {
        val pos = spinnerCareer.selectedItemPosition
        return careerIds.getOrElse(pos) { "0" }.toIntOrNull() ?: 0   // กันพลาด -> 0 = ไม่ระบุ
    }

    private fun setCareerFromApi(careerId: Int?) {
        val target = careerId ?: 0
        val idx = careerIds.indexOf(target.toString())
        spinnerCareer.setSelection(if (idx >= 0) idx else 0)
    }

    private fun toggleEditMode() {
        isEditing = !isEditing
        setEditingEnabled(isEditing)

        if (isEditing) {
            buttonSaveProfile.visibility = View.VISIBLE
            buttonEditPreferences.visibility = View.VISIBLE
            buttonDeleteAccount.visibility = View.VISIBLE

            currentUser = originalUser.copy()
            showAllFields() // แสดงฟิลด์ทั้งหมดเมื่อเข้าสู่โหมดแก้ไข

            // Enable textViewBio when editing mode is turned on
            textViewBio.isEnabled = true

            // ✅ แก้ชนกันระหว่าง baseline กับ top ของ labelProvince (ครั้งแรกที่เข้า edit)
            spinnerProvince.post {
                val lp = labelProvince.layoutParams
                        as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                lp.baselineToBaseline = spinnerProvince.id
                lp.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                lp.topToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                lp.bottomToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                lp.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                labelProvince.layoutParams = lp
            }
        } else {
            restoreOriginalUserInfo()
            hideFieldsForViewingMode() // ซ่อนฟิลด์ที่ไม่จำเป็นเมื่อออกจากโหมดแก้ไข

            // Disable textViewBio when editing mode is turned off
            textViewBio.isEnabled = false
        }
    }

    private fun formatDateForDisplay(raw: String?): String {
        if (raw.isNullOrBlank()) return "-"
        val isoPatterns = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        )
        for (p in isoPatterns) {
            try {
                val inFmt = SimpleDateFormat(p, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val d = inFmt.parse(raw)
                if (d != null) return SimpleDateFormat("dd/MM/yyyy", Locale.US).format(d)
            } catch (_: Exception) {}
        }
        return try {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw)
            SimpleDateFormat("dd/MM/yyyy", Locale.US).format(d!!)
        } catch (_: Exception) {
            raw.take(10)
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == AppCompatActivity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            Glide.with(this)
                .load(selectedImageUri) // URL ควรมาจาก server
                .placeholder(R.drawable.img_1)
                .format(DecodeFormat.PREFER_RGB_565)
                .downsample(DownsampleStrategy.AT_MOST)
                .error(R.drawable.error)
                .into(imageViewProfile)

        } else if (requestCode == REQUEST_CODE_CHANGE_PREFERENCES && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            val updatedPreferences = data.getStringExtra("preferences")
            loadPreferences(updatedPreferences)
        }
    }

    private fun loadPreferences(preferences: String?) {
        preferenceContainer.removeAllViews()
        val cleaned = preferences
            ?.trim()
            ?.removePrefix("[")
            ?.removeSuffix("]")
            ?.replace("\"", "")
            ?: ""
        // ✅ split ด้วย comma (รองรับ , 、 ，) แล้ว trim/กรองค่าว่าง
        val preferencesArray = if (cleaned.isBlank()) {
            emptyList()
        } else {
            cleaned.split(Regex("\\s*[，,、]\\s*"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
        for (preference in preferencesArray) {
            val preferenceTextView = TextView(requireContext())
            preferenceTextView.text = preference
            preferenceTextView.setBackgroundResource(R.drawable.show_preference)
            preferenceTextView.setPadding(16, 16, 16, 16)
            preferenceTextView.textSize = 14f
            preferenceTextView.setTypeface(null, android.graphics.Typeface.BOLD) // ทำตัวหนังสือหนา
            preferenceTextView.gravity = Gravity.CENTER // จัดให้ตัวหนังสืออยู่ตรงกลาง
            preferenceTextView.setTextColor(resources.getColor(R.color.white))

            val layoutParams = LinearLayout.LayoutParams(165, 100) // กำหนดขนาดเป็น 50x50
            layoutParams.setMargins(16, 16, 16, 16)
            preferenceTextView.layoutParams = layoutParams
            preferenceContainer.addView(preferenceTextView)
        }
    }


    private fun fetchUserInfo(userID: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = getString(R.string.root_url) + "/api_v2/user/$userID"
                val request = Request.Builder().url(url).build()
                val response = OkHttpClient().newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    // 👇 ใส่ตรงนี้เลย
                    Log.d("API_RESPONSE", responseBody ?: "null")
                    user = parseUserInfo(responseBody) // กำหนดค่าให้ user ที่นี่

                    withContext(Dispatchers.Main) {
                        originalUser = user
                        currentUser = user
                        updateUserFields(user)
                        setCareerFromApi(user.career_id)
                        verifyBadge.visibility = if (user.verify == 1) View.VISIBLE else View.GONE
                        buttonVerify.visibility = if (user.verify == 1) View.GONE else View.VISIBLE

                        // ✅ ไฮไลต์ section ที่ส่งมาจากหน้า Help (ทำหลังอัปเดตฟิลด์แล้ว)
                        pendingOpenSection?.let { section ->
                            highlightSection(section)
                            pendingOpenSection = null
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Failed to fetch user info", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateUserFields(user: User) {
        textViewFirstName.setText(user.firstName)
        textViewLastName.setText(user.lastName)
        textViewNickname.setText(user.nickname)
        textViewUsername.setText(user.username)
        textViewEmail.setText(user.email)
        textViewHeight.setText(user.height.toString())
        textViewWeight.setText(user.weight.toString())
        textViewHome.setText(user.home)
        textViewBio.setText(user.bio ?: "")
        buttonSelectDateProfile.text = formatDateForDisplay(user.dateBirth)

        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbarProfile)
        val toolbarTitle = toolbar.findViewById<TextView>(R.id.toolbarTitle)
        val verifyBadgeNickname = toolbar.findViewById<ImageView>(R.id.verifyBadgeNickname)

        toolbarTitle.text = user.nickname
        verifyBadgeNickname.visibility = if (user.verify == 1) View.VISIBLE else View.GONE

        loadPreferences(user.preferences.toString())

        val genderValueArray = resources.getStringArray(R.array.gender_array) // อ่าน array ภาษาอังกฤษครั้งเดียว
        val genderIndex = genderValueArray.indexOf(user.gender) // user.gender เช่น "Male" "Female" "Other"
        if (genderIndex >= 0) {
            spinnerGender.setSelection(genderIndex)
        }

        val educationIndex = resources.getStringArray(R.array.education_levels).indexOf(user.education)
        if (educationIndex >= 0) {
            spinnerEducation.setSelection(educationIndex)
        }

        val goalIndex = resources.getStringArray(R.array.goal_options).indexOf(user.goal)
        if (goalIndex >= 0) {
            spinnerGoal.setSelection(goalIndex)
        }

        // --- Bio: กัน "null"/"undefined" โผล่ในช่อง ---
        val bioText = user.bio?.trim()
            ?.takeUnless { it.equals("null", true) || it.equals("undefined", true) }
            ?: ""
        textViewBio.setText(bioText)
        if (bioText.isEmpty()) {
            textViewBio.hint = "กรอกข้อมูลเกี่ยวกับตัวเอง"
        }

        // --- Province: ใช้ลิสต์ที่มี placeholder แถว 0 ---
        val rawProvince = user.province?.trim().orEmpty()
        val provinceIdx = when {
            rawProvince.isBlank() || rawProvince.equals("null", true) -> 0  // ชี้ไป placeholder
            ::provinceList.isInitialized -> provinceList.indexOf(rawProvince).takeIf { it >= 0 } ?: 0
            else -> 0
        }
        spinnerProvince.setSelection(provinceIdx, false)
// ให้หัวสปินเนอร์เป็นสีเทาถ้าอยู่ที่ placeholder
        spinnerProvince.post {
            (spinnerProvince.selectedView as? TextView)?.setTextColor(
                if (spinnerProvince.selectedItemPosition == 0) Color.GRAY else Color.BLACK
            )
        }

        val interestGenderValueArray = resources.getStringArray(R.array.interest_gender_array)
        val interestGenderIndex = interestGenderValueArray.indexOf(user.interestGender)
        if (interestGenderIndex >= 0) {
            spinnerInterestGender.setSelection(interestGenderIndex)
        }


        user.imageFile?.let { loadImage(it, imageViewProfile) }
    }


    private fun loadImage(url: String, imageView: ImageView) {
        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.img_1)
            .error(R.drawable.error)
            .into(imageView)
    }

    private fun saveUserInfo(userID: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()

                // ใช้ gender_value_array แทน selected text ตรงๆ
                val genderValueArray = resources.getStringArray(R.array.gender_array)
                val selectedGender = genderValueArray[spinnerGender.selectedItemPosition]

                val interestGenderValueArray = resources.getStringArray(R.array.interest_gender_array)
                val selectedInterestGender = interestGenderValueArray[spinnerInterestGender.selectedItemPosition]
                val selectedEducation = spinnerEducation.selectedItem.toString()
                val selectedGoal = spinnerGoal.selectedItem.toString()
                val selectedProvince = spinnerProvince.selectedItem.toString()

                val formattedDateBirth = selectedDateOfBirth ?: currentUser.dateBirth


                val requestBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("username", textViewUsername.text.toString())
                    .addFormDataPart("nickname", textViewNickname.text.toString())
                    .addFormDataPart("email", textViewEmail.text.toString())
                    .addFormDataPart("firstname", textViewFirstName.text.toString())
                    .addFormDataPart("lastname", textViewLastName.text.toString())
                    .addFormDataPart("gender", selectedGender)
                    .addFormDataPart("interestGender", selectedInterestGender)
                    .addFormDataPart("education", selectedEducation)
                    .addFormDataPart("goal", selectedGoal)
                    .addFormDataPart("height", textViewHeight.text.toString())
                    .addFormDataPart("weight", textViewWeight.text.toString())
                    .addFormDataPart("home", textViewHome.text.toString())
                    .addFormDataPart("province", selectedProvince)
                    .addFormDataPart("bio", textViewBio.text.toString())

                // ⬅️ ใส่ career_id ตรงนี้ (อยู่ในสcopeเดียวกับ requestBuilder)
                requestBuilder.addFormDataPart("career_id", getSelectedCareerId().toString())

                formattedDateBirth?.let {
                    requestBuilder.addFormDataPart("DateBirth", it)
                }

                if (selectedImageUri != null) {
                    val inputStream = requireActivity().contentResolver.openInputStream(selectedImageUri!!)
                    val fileBytes = inputStream?.readBytes()
                    if (fileBytes != null) {
                        requestBuilder.addFormDataPart(
                            "image",
                            "profile_image.jpg",
                            fileBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                        )
                    }
                }

                val requestBody = requestBuilder.build()
                val rootUrl = getString(R.string.root_url)
                val url = "$rootUrl/api_v2/user/update/$userID"
                val request = Request.Builder().url(url).put(requestBody).build()

                val response = client.newCall(request).execute()
                val success = response.isSuccessful

                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(requireContext(), "บันทึกข้อมูลสำเร็จ", Toast.LENGTH_SHORT).show()
                        // ✅ ให้ ProgressBar เป็น indeterminate ระหว่างคำนวณใหม่
                        pbCompleteness.isIndeterminate = true
                        txtCompleteness.text = "กำลังประเมิน…"

                        delay(600)
                        fetchUserInfo(userID)
                        // ✅ ยิงขอคะแนนความสมบูรณ์ใหม่ทันที (ไม่ต้องกดปุ่มคำแนะนำ)
                        fetchProfileCompleteness(userID)

                        setEditingEnabled(false)
                        hideFieldsForViewingMode()
                    } else {
                        val errorResponse = response.body?.string()
                        Toast.makeText(requireContext(), "บันทึกข้อมูลล้มเหลว: ${errorResponse ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Calculate the maximum date as 18 years before today
        calendar.add(Calendar.YEAR, -18)
        val eighteenYearsAgo = calendar.timeInMillis

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDateOfBirth = "%04d-%02d-%02d".format(selectedYear, selectedMonth + 1, selectedDay) // เก็บส่งเซิร์ฟเวอร์ (yyyy-MM-dd)
                buttonSelectDateProfile.text = formatDateForDisplay(selectedDateOfBirth)
            },
            year,
            month,
            day
        )

        datePickerDialog.datePicker.maxDate = eighteenYearsAgo
        datePickerDialog.show()
    }

    private fun setEditingEnabled(enabled: Boolean) {
        textViewUsername.isFocusable = enabled
        textViewUsername.isFocusableInTouchMode = enabled
        textViewNickname.isFocusable = enabled
        textViewNickname.isFocusableInTouchMode = enabled
        textViewEmail.isFocusable = enabled
        textViewEmail.isFocusableInTouchMode = enabled
        textViewFirstName.isFocusable = enabled
        textViewFirstName.isFocusableInTouchMode = enabled
        textViewLastName.isFocusable = enabled
        textViewLastName.isFocusableInTouchMode = enabled
        textViewWeight.isFocusable = enabled
        textViewWeight.isFocusableInTouchMode = enabled
        textViewHeight.isFocusable = enabled
        textViewHeight.isFocusableInTouchMode = enabled
        textViewHome.isFocusable = enabled
        textViewHome.isFocusableInTouchMode = enabled
        spinnerGender.isEnabled = enabled
        spinnerInterestGender.isEnabled = enabled
        spinnerEducation.isEnabled = enabled
        spinnerGoal.isEnabled = enabled
        spinnerProvince.isEnabled = enabled
        spinnerCareer.isEnabled = enabled
        buttonSelectDateProfile.isEnabled = enabled
        buttonSaveProfile.isEnabled = enabled
        buttonDeleteAccount.isEnabled = enabled
    }

    private fun showAllFields() {
        spinnerInterestGender.visibility = View.VISIBLE
        textViewUsername.visibility = View.VISIBLE
        labelUsername.visibility = View.VISIBLE
        labelEmail.visibility = View.VISIBLE
        labelEducation.visibility = View.VISIBLE
        labelGoal.visibility = View.VISIBLE
        labelHeight.visibility = View.VISIBLE
        labelWeight.visibility = View.VISIBLE
        labelCareer.visibility = View.VISIBLE
        labelHome.visibility = View.VISIBLE
        labelInterest.visibility = View.VISIBLE
        labelProvince.visibility = View.VISIBLE
        labelDate.visibility = View.VISIBLE
        textViewEmail.visibility = View.VISIBLE
        textViewHeight.visibility = View.VISIBLE
        textViewWeight.visibility = View.VISIBLE
        textViewHome.visibility = View.VISIBLE
        buttonSelectDateProfile.visibility = View.VISIBLE
        spinnerGoal.visibility = View.VISIBLE
        spinnerEducation.visibility = View.VISIBLE
        spinnerProvince.visibility = View.VISIBLE
        spinnerCareer.visibility = View.VISIBLE
    }

    private fun hideFieldsForViewingMode() {
        textViewUsername.visibility = View.GONE
        textViewEmail.visibility = View.GONE
        labelUsername.visibility = View.GONE
        labelEmail.visibility = View.GONE
        labelEducation.visibility = View.GONE
        labelGoal.visibility = View.GONE
        labelHeight.visibility = View.GONE
        labelWeight.visibility = View.GONE
        labelHome.visibility = View.GONE
        labelInterest.visibility = View.GONE
        labelDate.visibility = View.GONE
        labelCareer.visibility = View.GONE
        labelProvince.visibility = View.GONE
        textViewHeight.visibility = View.GONE
        textViewWeight.visibility = View.GONE
        textViewHome.visibility = View.GONE
        buttonSelectDateProfile.visibility = View.GONE
        spinnerGoal.visibility = View.GONE
        spinnerEducation.visibility = View.GONE
        spinnerInterestGender.visibility = View.GONE
        spinnerProvince.visibility = View.GONE
        spinnerCareer.visibility = View.GONE
        buttonDeleteAccount.visibility = View.GONE
        buttonSaveProfile.visibility = View.GONE
        buttonEditPreferences.visibility = View.GONE
    }

    private fun restoreOriginalUserInfo() {
        updateUserFields(originalUser)
    }

    private fun deleteUser(userID: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("${getString(R.string.root_url)}/api_v2/user/$userID")
                    .delete()
                    .build()

                val response = client.newCall(request).execute()
                val success = response.isSuccessful

                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(requireContext(), "ลบผู้ใช้สำเร็จ", Toast.LENGTH_SHORT).show()
                        val intent = Intent(requireContext(), FirstPageActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        Toast.makeText(requireContext(), "ลบผู้ใช้ไม่สำเร็จ", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun logoutUser(userID: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = getString(R.string.root_url) + "/api_v2/logout/$userID"
                val request = Request.Builder()
                    .url(url)
                    .post(okhttp3.FormBody.Builder().build())
                    .build()

                val response = OkHttpClient().newCall(request).execute()
                val success = response.isSuccessful

                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(requireContext(), FirstPageActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        Toast.makeText(requireContext(), "Failed to logout", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun optIntAny(json: JSONObject, vararg keys: String, default: Int = 0): Int {
        for (k in keys) if (json.has(k) && !json.isNull(k)) return json.optInt(k, default)
        return default
    }
    private fun optStringAny(json: JSONObject, vararg keys: String, default: String? = null): String? {
        for (k in keys) if (json.has(k) && !json.isNull(k)) return json.optString(k, default)
        return default
    }

    private fun parseUserInfo(responseBody: String?): User {
        val jsonObject = JSONObject(responseBody ?: "{}")
        val distance = jsonObject.optDouble("distance", 0.0)
        return User(
            id = jsonObject.optInt("id", -1),
            username = jsonObject.optString("username", ""),
            nickname = jsonObject.optString("nickname", ""),
            email = jsonObject.optString("email", ""),
            firstName = jsonObject.optString("firstname", ""),
            lastName = jsonObject.optString("lastname", ""),
            gender = jsonObject.optString("gender", ""),
            interestGender = jsonObject.optString("interestGender", ""),
            education = jsonObject.optString("education", ""),
            goal = jsonObject.optString("goal", ""),
            preferences = listOf(jsonObject.optString("preferences", "")),
            height = jsonObject.optDouble("height", 0.0),
            weight = jsonObject.optDouble("weight", 0.0),   // ✅ เพิ่มตรงนี้
            home = jsonObject.optString("home", ""),
            dateBirth = jsonObject.optString("DateBirth", ""),
            imageFile = jsonObject.optString("imageFile", ""),
            verify = jsonObject.optInt("verify", 0) , // ✅ เพิ่มตรงนี้
            longitude = jsonObject.optDouble("longitude", 0.0),
            latitude = jsonObject.optDouble("latitude", 0.0),
            distance = distance, // Pass the parsed distance here
            province = jsonObject.optString("province", ""),
            // 🔧 รองรับทั้ง snake_case/camelCase: career_id / careerID / careerId
            career_id = optIntAny(jsonObject, "career_id", "careerID", "careerId", default = 0),
            // 🔧 รองรับทั้ง snake_case/camelCase: career_name / careerName / career
            career_name = optStringAny(jsonObject, "career_name", "careerName", "career", default = null),

            bio = jsonObject.optString("bio", null),   // ✅ เพิ่มตรงนี้
        )
    }
    private fun highlightSection(section: String) {
        val ids = when (section) {
            "bio"            -> listOf(R.id.edtBio)
            "photo"          -> listOf(R.id.imageViewProfile)
            "verify"         -> listOf(R.id.buttonVerify)
            "preferences"    -> listOf(R.id.preferenceContainer, R.id.buttonEditPreferences)
            "home"           -> listOf(R.id.textViewHome)
            "province"       -> listOf(R.id.spinnerProvince)
            "location"       -> listOf(R.id.spinnerProvince, R.id.textViewHome)
            "goal"           -> listOf(R.id.spinnerGoal, R.id.spinnerInterestGender)
            "gender"         -> listOf(R.id.spinnerGender)               // ✅ ใหม่
            "birthday"       -> listOf(R.id.buttonSelectDateProfile)     // ✅ ใหม่
            "nickname"       -> listOf(R.id.textViewNickname)
            "education"      -> listOf(R.id.spinnerEducation)
            "career"         -> listOf(R.id.spinnerCareer)
            "profile"        -> listOf(R.id.textViewFirstName, R.id.textViewLastName, R.id.spinnerGender, R.id.buttonSelectDateProfile)
            else             -> listOf(R.id.edtBio)
        }
        highlightViews(ids)
    }

    private fun highlightViews(candidateIds: List<Int>) {
        val root = view ?: return
        val target = candidateIds
            .mapNotNull { root.findViewById<View>(it) }
            .firstOrNull() ?: return

        target.post {
            // เลื่อนให้เห็นก่อน
            root.findViewById<ScrollView>(R.id.profileScrollView)?.let { scroll ->
                val rect = Rect()
                target.getDrawingRect(rect)
                scroll.offsetDescendantRectToMyCoords(target, rect)
                scroll.smoothScrollTo(0, maxOf(0, rect.top - dp(32)))
            }

            // เคสพิเศษ: กล่อง "ความชอบ"
            if (target.id == R.id.preferenceContainer && target is LinearLayout) {
                if (target.childCount > 0) {
                    // มีแท็กแล้ว → ไฮไลต์แท็ก 2–3 อันแรก
                    val n = minOf(3, target.childCount)
                    for (i in 0 until n) {
                        val chip = target.getChildAt(i)
                        pulseHighlight(chip)
                        flashStroke(chip, duration = 5000L)
                    }
                } else {
                    // ยังไม่มีแท็ก → ไฮไลต์ปุ่มแก้ไขความชอบ
                    root.findViewById<View>(R.id.buttonEditPreferences)?.let { btn ->
                        pulseHighlight(btn)
                        flashStroke(btn, duration = 2200L)
                    }
                }
            } else {
                // เคสทั่วไป
                pulseHighlight(target)
                flashStroke(target)
            }
        }
    }

    private fun dp(value: Int): Int {
        val dm = resources.displayMetrics
        return (value * dm.density).toInt()
    }

    private fun pulseHighlight(v: View, pulses: Int = 2) {
        val overlay = android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#FFF4C2"))
        overlay.alpha = 0
        v.overlay.add(overlay)

        val anim = android.animation.ObjectAnimator.ofInt(overlay, "alpha", 0, 170, 0).apply {
            duration = 650
            repeatCount = pulses - 1
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    v.overlay.remove(overlay)
                }
            })
        }
        anim.start()
    }
    private fun flashStroke(
        v: View,
        color: Int = 0xFFE53935.toInt(),
        duration: Long = 6000L,
        cornerRadiusDp: Int = 12,
        strokeWidthDp: Int = 3
    ) {
        // วาดแค่เส้นขอบบน overlay (ไม่แตะ background เดิม)
        val stroke = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.TRANSPARENT)                    // โปร่งใสจริง ๆ
            cornerRadius = dp(cornerRadiusDp).toFloat()    // โค้งให้ใกล้เคียงปุ่มเดิม
            setStroke(dp(strokeWidthDp), color)            // เส้นขอบแดง
        }
        // ต้องมีขนาดแล้ว → เราเรียกใน target.post { ... } อยู่แล้ว
        stroke.setBounds(0, 0, v.width, v.height)
        v.overlay.add(stroke)

        v.postDelayed({
            v.overlay.remove(stroke)                       // เอาออกหลังครบเวลา
        }, duration)
    }
    private fun fetchProfileCompleteness(userID: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = getString(R.string.root_url) + "/api_v2/profile/recommend/$userID"
                val req = Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Accept", "application/json")
                    .build()

                val resp = OkHttpClient().newCall(req).execute()
                val body = resp.body?.string().orEmpty()

                if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}: ${body.take(200)}")

                val obj = org.json.JSONObject(body)
                val score = obj.optInt("score", 0) // 0..100
                val comp  = obj.optString("completeness", "")

                withContext(Dispatchers.Main) {
                    // เปลี่ยนจาก indeterminate -> determinate แล้วอัปเดตค่า
                    pbCompleteness.isIndeterminate = false
                    pbCompleteness.max = 100

                    // สีตามช่วงคะแนน
                    val color = when {
                        score >= 80 -> android.graphics.Color.parseColor("#2E7D32") // เขียว
                        score >= 50 -> android.graphics.Color.parseColor("#F9A825") // เหลือง
                        else        -> android.graphics.Color.parseColor("#D32F2F") // แดง
                    }
                    if (android.os.Build.VERSION.SDK_INT >= 21) {
                        pbCompleteness.progressTintList =
                            android.content.res.ColorStateList.valueOf(color)
                        pbCompleteness.progressBackgroundTintList =
                            android.content.res.ColorStateList.valueOf(0x22000000.toInt())
                    } else {
                        @Suppress("DEPRECATION")
                        pbCompleteness.progressDrawable.setColorFilter(
                            color, android.graphics.PorterDuff.Mode.SRC_IN
                        )
                    }

                    // อนิเมชันเลื่อนค่า
                    android.animation.ObjectAnimator.ofInt(
                        pbCompleteness, "progress", pbCompleteness.progress, score
                    ).apply { duration = 600 }.start()

                    txtCompleteness.text = if (comp.isNotBlank()) comp else "$score%"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // ถ้าพลาด ให้คงบาร์ไว้แต่บอกสถานะ
                    pbCompleteness.isIndeterminate = false
                    pbCompleteness.progress = 0
                    txtCompleteness.text = "—%"
                }
            }
        }
    }
}