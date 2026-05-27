package com.carlauncher.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.carlauncher.R
import com.carlauncher.data.model.NotificationInfo
import com.carlauncher.utils.formatTimeAgo

class NotificationPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private lateinit var closeButton: ImageView
    private lateinit var notificationList: LinearLayout
    private lateinit var brightnessToggle: Switch
    private lateinit var volumeToggle: Switch
    private lateinit var bluetoothToggle: Switch
    private lateinit var wifiToggle: Switch

    var onClose: (() -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_notification_panel, this, true)
        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        closeButton = findViewById(R.id.notification_close)
        notificationList = findViewById(R.id.notification_list)
        brightnessToggle = findViewById(R.id.quick_settings_brightness)
        volumeToggle = findViewById(R.id.quick_settings_volume)
        bluetoothToggle = findViewById(R.id.quick_settings_bluetooth)
        wifiToggle = findViewById(R.id.quick_settings_wifi)
    }

    private fun setupClickListeners() {
        closeButton.setOnClickListener { onClose?.invoke() }
    }

    fun bindNotifications(notifications: LiveData<List<NotificationInfo>>, owner: LifecycleOwner) {
        notifications.observe(owner) { updateNotifications(it) }
    }

    private fun updateNotifications(notifications: List<NotificationInfo>) {
        notificationList.removeAllViews()
        
        if (notifications.isEmpty()) {
            val emptyView = LayoutInflater.from(context).inflate(R.layout.item_empty_notification, notificationList, false)
            notificationList.addView(emptyView)
        } else {
            notifications.forEach { notification ->
                val view = LayoutInflater.from(context).inflate(R.layout.item_notification, notificationList, false)
                val title = view.findViewById<TextView>(R.id.notification_title)
                val content = view.findViewById<TextView>(R.id.notification_content)
                val time = view.findViewById<TextView>(R.id.notification_time)
                
                title.text = notification.title
                content.text = notification.content
                time.text = notification.postTime.formatTimeAgo()
                
                notificationList.addView(view)
            }
        }
    }

    fun show() {
        visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.GONE
    }

    fun isVisible(): Boolean {
        return visibility == View.VISIBLE
    }
}
