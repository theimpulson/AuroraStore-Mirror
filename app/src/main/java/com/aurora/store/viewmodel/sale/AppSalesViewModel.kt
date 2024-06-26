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

package com.aurora.store.viewmodel.sale

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.AppSalesHelper
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.viewmodel.all.PaginatedAppList
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

@HiltViewModel
@SuppressLint("StaticFieldLeak") // false positive, see https://github.com/google/dagger/issues/3253
class AppSalesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authProvider: AuthProvider
) : ViewModel() {

    private val appSalesHelper: AppSalesHelper = AppSalesHelper(authProvider.authData)
        .using(HttpClient.getPreferredClient(context))

    private var page: Int = 0
    private val appList: MutableList<App> = mutableListOf()

    val liveData: MutableLiveData<PaginatedAppList> = MutableLiveData()

    init {
        observe()
    }

    fun observe() {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    val nextAppList = getSearchResults()

                    if (nextAppList.isEmpty()) {
                        liveData.postValue(
                            PaginatedAppList(
                                appList,
                                hasMore = false
                            )
                        )
                    } else {
                        appList.addAll(nextAppList)
                        liveData.postValue(
                            PaginatedAppList(
                                appList,
                                hasMore = true
                            )
                        )
                    }

                } catch (_: Exception) {
                }
            }
        }
    }

    private fun getSearchResults(): List<App> {
        return try {
            appSalesHelper.getAppsOnSale(page = page++, offer = 100)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
