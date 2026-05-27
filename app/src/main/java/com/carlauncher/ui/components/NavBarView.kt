package com.carlauncher.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import com.carlauncher.R
import com.carlauncher.utils.visible

class NavBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private lateinit var backButton: ImageView
    private lateinit var homeButton: ImageView
    private lateinit var recentButton: ImageView
    private lateinit var drawerButton: ImageView

    var onBackClick: (() -> Unit)? = null
    var onHomeClick: (() -> Unit)? = null
    var onRecentClick: (() -> Unit)? = null
    var onDrawerClick: (() -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_nav_bar, this, true)
        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        backButton = findViewById(R.id.nav_bar_back)
        homeButton = findViewById(R.id.nav_bar_home)
        recentButton = findViewById(R.id.nav_bar_recent)
        drawerButton = findViewById(R.id.nav_bar_drawer)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener { onBackClick?.invoke() }
        homeButton.setOnClickListener { onHomeClick?.invoke() }
        recentButton.setOnClickListener { onRecentClick?.invoke() }
        drawerButton.setOnClickListener { onDrawerClick?.invoke() }
    }

    fun setBackButtonVisibility(visible: Boolean) {
        backButton.visible(visible)
    }

    fun setHomeButtonVisibility(visible: Boolean) {
        homeButton.visible(visible)
    }

    fun setRecentButtonVisibility(visible: Boolean) {
        recentButton.visible(visible)
    }

    fun setDrawerButtonVisibility(visible: Boolean) {
        drawerButton.visible(visible)
    }
}
