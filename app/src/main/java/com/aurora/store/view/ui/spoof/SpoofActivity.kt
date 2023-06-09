/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.view.ui.spoof

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.data.providers.NativeDeviceInfoProvider
import com.aurora.store.databinding.ActivityGenericPagerBinding
import com.aurora.store.util.PathUtil
import com.aurora.store.view.ui.commons.BaseActivity
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi

class SpoofActivity : BaseActivity() {

    private lateinit var B: ActivityGenericPagerBinding

    // Android is weird, even if export device config with proper mime type, it will refuse to open
    // it again with same mime type
    private val importMimeType = "application/octet-stream"
    private val exportMimeType = "text/x-java-properties"

    private val startForDocumentImport =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) {
            if (it != null) importDeviceConfig(it) else toast(R.string.toast_import_failed)
        }
    private val startForDocumentExport =
        registerForActivityResult(ActivityResultContracts.CreateDocument(exportMimeType)) {
            if (it != null) exportDeviceConfig(it) else toast(R.string.toast_export_failed)
        }

    override fun onConnected() {

    }

    override fun onDisconnected() {

    }

    override fun onReconnected() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        B = ActivityGenericPagerBinding.inflate(layoutInflater)
        setContentView(B.root)

        attachToolbar()
        attachViewPager()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_spoof, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
            R.id.action_import -> {
                startForDocumentImport.launch(arrayOf(importMimeType))
            }
            R.id.action_export -> {
                startForDocumentExport
                    .launch("aurora_store_${Build.BRAND}_${Build.DEVICE}.properties")
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun attachToolbar() {
        setSupportActionBar(B.layoutActionToolbar.toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.elevation = 0f
            actionBar.title = getString(R.string.title_spoof_manager)
        }
    }

    private fun attachViewPager() {
        B.pager.adapter = ViewPagerAdapter(supportFragmentManager, lifecycle)

        TabLayoutMediator(B.tabLayout, B.pager, true) { tab: TabLayout.Tab, position: Int ->
            when (position) {
                0 -> tab.text = getString(R.string.title_device)
                1 -> tab.text = getString(R.string.title_language)
                else -> {
                }
            }
        }.attach()
    }

    private fun importDeviceConfig(uri: Uri) {
        task {
            contentResolver.openInputStream(uri)?.use { input ->
                PathUtil.getNewEmptySpoofConfig(this).outputStream().use {
                    input.copyTo(it)
                }
            }
        } successUi {
            toast(R.string.toast_import_success)
            recreate()
        } failUi {
            it.printStackTrace()
            toast(R.string.toast_import_failed)
        }
    }

    private fun exportDeviceConfig(uri: Uri) {
        task {
            NativeDeviceInfoProvider(this)
                .getNativeDeviceProperties()
                .store(contentResolver.openOutputStream(uri), "DEVICE_CONFIG")
        } successUi {
            toast(R.string.toast_export_success)
        } failUi {
            it.printStackTrace()
            toast(R.string.toast_export_failed)
        }
    }

    internal class ViewPagerAdapter(fragment: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragment, lifecycle) {
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> DeviceSpoofFragment.newInstance()
                1 -> LocaleSpoofFragment.newInstance()
                else -> Fragment()
            }
        }

        override fun getItemCount(): Int {
            return 2
        }
    }
}
