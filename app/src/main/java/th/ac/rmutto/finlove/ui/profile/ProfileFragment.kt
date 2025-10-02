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

    private lateinit var user: User // ประกาศตัวแปร user ที่คลาส level


    private lateinit var buttonEditProfile: ImageButton
    private lateinit var buttonSaveProfile: Button
    private lateinit var buttonEditPreferences: Button
    private lateinit var buttonVerify: Button
    private lateinit var buttonLogout: Button
    private lateinit var buttonDeleteAccount: Button
    private var selectedImageUri: Uri? = null
    private var selectedDateOfBirth: String? = null
    private var isEditing = false
    private val PICK_IMAGE_REQUEST = 1
    private val REQUEST_CODE_CHANGE_PREFERENCES = 1001

    private lateinit var originalUser: User
    private lateinit var currentUser: User

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize views
        initializeViews(root)

        // Fetch user ID from intent
        val userID = requireActivity().intent.getIntExtra("userID", -1)
        Log.d("ProfileFragment", "Received userID: $userID")

        if (userID != -1) {
            fetchUserInfo(userID)
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
        val provinceAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.province_display_array, // อ้างอิงจาก strings.xml
            android.R.layout.simple_spinner_item
        )
        provinceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProvince.adapter = provinceAdapter

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

        val provinceArray = resources.getStringArray(R.array.province_display_array)
        val provinceIndex = provinceArray.indexOf(user.province)
        if (provinceIndex >= 0) {
            spinnerProvince.setSelection(provinceIndex)
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

                        delay(600)
                        fetchUserInfo(userID)

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
}