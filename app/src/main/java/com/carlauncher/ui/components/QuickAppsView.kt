package com.carlauncher.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.carlauncher.R
import com.carlauncher.data.model.AppInfo

class QuickAppsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GridLayout(context, attrs, defStyleAttr) {

    var onAppClick: ((AppInfo) -> Unit)? = null

    init {
        columnCount = 4
        rowCount = 2
    }

    fun bindApps(apps: LiveData<List<AppInfo>>, owner: LifecycleOwner) {
        apps.observe(owner) { updateApps(it) }
    }

    private fun updateApps(apps: List<AppInfo>) {
        removeAllViews()
        
        apps.take(8).forEach { app ->
            val view = LayoutInflater.from(context).inflate(R.layout.item_quick_app, this, false)
            val icon = view.findViewById<ImageView>(R.id.quick_app_icon)
            val name = view.findViewById<TextView>(R.id.quick_app_name)
            
            icon.setImageDrawable(app.icon)
            name.text = app.appName
            
            view.setOnClickListener { onAppClick?.invoke(app) }
            addView(view)
        }
    }
}
