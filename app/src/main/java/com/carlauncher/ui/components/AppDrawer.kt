package com.carlauncher.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.carlauncher.R
import com.carlauncher.data.model.AppInfo
import com.carlauncher.ui.adapter.AppGridAdapter

class AppDrawer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private lateinit var searchView: SearchView
    private lateinit var sortButton: Button
    private lateinit var closeButton: ImageView
    private lateinit var appRecyclerView: RecyclerView
    
    var onClose: (() -> Unit)? = null
    var onAppClick: ((AppInfo) -> Unit)? = null

    enum class SortType { NAME, USAGE }
    private var currentSortType = SortType.NAME

    init {
        LayoutInflater.from(context).inflate(R.layout.view_app_drawer, this, true)
        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        searchView = findViewById(R.id.drawer_search)
        sortButton = findViewById(R.id.drawer_sort)
        closeButton = findViewById(R.id.drawer_close)
        appRecyclerView = findViewById(R.id.drawer_recycler)
        appRecyclerView.layoutManager = GridLayoutManager(context, 4)
    }

    private fun setupClickListeners() {
        closeButton.setOnClickListener { onClose?.invoke() }
        sortButton.setOnClickListener { toggleSort() }
    }

    fun bindApps(apps: LiveData<List<AppInfo>>, owner: LifecycleOwner) {
        apps.observe(owner) { updateApps(it) }
    }

    private fun updateApps(apps: List<AppInfo>) {
        val adapter = AppGridAdapter(apps) { app ->
            onAppClick?.invoke(app)
        }
        appRecyclerView.adapter = adapter
    }

    private fun toggleSort() {
        currentSortType = when (currentSortType) {
            SortType.NAME -> SortType.USAGE
            SortType.USAGE -> SortType.NAME
        }
        sortButton.text = when (currentSortType) {
            SortType.NAME -> "按名称"
            SortType.USAGE -> "按使用"
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
