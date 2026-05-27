package com.carlauncher.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import com.carlauncher.R
import com.carlauncher.utils.setVisibility
import com.google.android.material.button.MaterialButton

class NavBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private lateinit var backButton: MaterialButton
    private lateinit var homeButton: MaterialButton
    private lateinit var recentButton: MaterialButton
    private lateinit var drawerButton: MaterialButton

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
        backButton = findViewById(R.id.nav_back)
        homeButton = findViewById(R.id.nav_home)
        recentButton = findViewById(R.id.nav_recent)
        drawerButton = findViewById(R.id.nav_drawer)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener { onBackClick?.invoke() }
        homeButton.setOnClickListener { onHomeClick?.invoke() }
        recentButton.setOnClickListener { onRecentClick?.invoke() }
        drawerButton.setOnClickListener { onDrawerClick?.invoke() }
    }

    fun setBackButtonVisibility(visible: Boolean) {
        backButton.setVisibility(visible)
    }

    fun setHomeButtonVisibility(visible: Boolean) {
        homeButton.setVisibility(visible)
    }

    fun setRecentButtonVisibility(visible: Boolean) {
        recentButton.setVisibility(visible)
    }

    fun setDrawerButtonVisibility(visible: Boolean) {
        drawerButton.setVisibility(visible)
    }
}