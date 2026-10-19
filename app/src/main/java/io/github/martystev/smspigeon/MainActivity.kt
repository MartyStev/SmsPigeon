package io.github.martystev.smspigeon

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var settings: SettingsManager
    private var isAuthenticated: Boolean = false

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_AUTH, isAuthenticated)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureEdgeToEdge()
        settings = SettingsManager(this)
        isAuthenticated = savedInstanceState?.getBoolean(STATE_AUTH) ?: false

        if (!isAuthenticated) {
            if (requestDeviceCredential()) {
                return
            }
        }

        initUi()
    }

    override fun onResume() {
        super.onResume()
        if (settings.isForwardingEnabled()) {
            startForwardService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CONFIRM_DEVICE_CREDENTIAL) {
            if (resultCode == RESULT_OK) {
                isAuthenticated = true
                initUi()
            } else {
                finish()
            }
        }
    }

    private fun initUi() {
        setContentView(R.layout.activity_main)

        applyWindowInsets()

        val tokenInput = findViewById<EditText>(R.id.input_token)
        val chatIdInput = findViewById<EditText>(R.id.input_chat_id)
        val sim1Input = findViewById<EditText>(R.id.input_sim1)
        val sim2Input = findViewById<EditText>(R.id.input_sim2)
        val blacklistInput = findViewById<EditText>(R.id.input_whitelist)
        val enabledSwitch = findViewById<Switch>(R.id.switch_enabled)
        val modeGroup = findViewById<RadioGroup>(R.id.group_mode)
        val modeAll = findViewById<RadioButton>(R.id.mode_all)
        val modeBlacklist = findViewById<RadioButton>(R.id.mode_whitelist)
        val saveButton = findViewById<Button>(R.id.button_save)

        tokenInput.setText(settings.getToken())
        chatIdInput.setText(settings.getChatId())
        sim1Input.setText(settings.getSim1Name())
        sim2Input.setText(settings.getSim2Name())
        blacklistInput.setText(settings.getBlacklistRaw())
        enabledSwitch.isChecked = settings.isForwardingEnabled()
        if (settings.isBlacklistMode()) {
            modeGroup.check(modeBlacklist.id)
        } else {
            modeGroup.check(modeAll.id)
        }

        saveButton.setOnClickListener {
            settings.setToken(tokenInput.text.toString())
            settings.setChatId(chatIdInput.text.toString())
            settings.setSim1Name(sim1Input.text.toString())
            settings.setSim2Name(sim2Input.text.toString())
            settings.setBlacklistRaw(blacklistInput.text.toString())
            settings.setForwardingEnabled(enabledSwitch.isChecked)
            settings.setBlacklistMode(modeGroup.checkedRadioButtonId == modeBlacklist.id)

            if (settings.isForwardingEnabled()) {
                startForwardService()
            } else {
                stopService(Intent(this, ForwardService::class.java))
            }

            Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show()
        }

        requestNeededPermissions()
    }

    private fun requestDeviceCredential(): Boolean {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (keyguard != null && keyguard.isDeviceSecure) {
            val intent = keyguard.createConfirmDeviceCredentialIntent(
                getString(R.string.auth_title),
                getString(R.string.auth_desc)
            )
            if (intent != null) {
                startActivityForResult(intent, REQ_CONFIRM_DEVICE_CREDENTIAL)
                return true
            }
        } else {
            isAuthenticated = true
        }
        return false
    }

    private fun startForwardService() {
        val intent = Intent(this, ForwardService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun requestNeededPermissions() {
        val permissions = ArrayList<String>()
        if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECEIVE_SMS)
        }
        if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_SMS)
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), 100)
        }
    }

    private fun configureEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                (window.decorView.systemUiVisibility or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    private fun applyWindowInsets() {
        val root = findViewById<android.view.View>(R.id.root)
        root.setOnApplyWindowInsetsListener { v, insets ->
            val sys = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(android.view.WindowInsets.Type.systemBars())
            } else {
                @Suppress("DEPRECATION")
                android.graphics.Insets.of(
                    insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom
                )
            }
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }
    }

    companion object {
        private const val REQ_CONFIRM_DEVICE_CREDENTIAL = 200
        private const val STATE_AUTH = "state_auth"
    }
}
