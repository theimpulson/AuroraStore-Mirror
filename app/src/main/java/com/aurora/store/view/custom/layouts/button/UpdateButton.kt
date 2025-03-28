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

package com.aurora.store.view.custom.layouts.button

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.aurora.extensions.runOnUiThread
import com.aurora.store.R
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.databinding.ViewUpdateButtonBinding

class UpdateButton : RelativeLayout {

    private lateinit var binding: ViewUpdateButtonBinding

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context) {
        val view = inflate(context, R.layout.view_update_button, this)
        binding = ViewUpdateButtonBinding.bind(view)
    }

    fun updateState(downloadStatus: DownloadStatus) {
        val displayChild = when (downloadStatus) {
            DownloadStatus.QUEUED,
            DownloadStatus.DOWNLOADING,
            DownloadStatus.VERIFYING -> 1

            else -> 0
        }

        if (binding.viewFlipper.displayedChild != displayChild) {
            runOnUiThread {
                binding.viewFlipper.displayedChild = displayChild
            }
        }

        // Not allowed to cancel installation at this point
        binding.btnNegative.isEnabled = downloadStatus != DownloadStatus.VERIFYING
    }

    fun addPositiveOnClickListener(onClickListener: OnClickListener?) {
        binding.btnPositive.setOnClickListener(onClickListener)
    }

    fun addNegativeOnClickListener(onClickListener: OnClickListener?) {
        binding.btnNegative.setOnClickListener(onClickListener)
    }
}
