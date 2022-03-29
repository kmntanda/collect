package org.odk.collect.android.formlist

import android.view.Menu
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.SearchView
import androidx.core.internal.view.SupportMenu
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.odk.collect.android.R
import org.odk.collect.android.network.NetworkStateProvider
import org.odk.collect.android.support.CollectHelpers
import org.odk.collect.androidshared.livedata.MutableNonNullLiveData
import org.robolectric.Shadows
import org.robolectric.fakes.RoboMenuItem
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowToast

@RunWith(AndroidJUnit4::class)
class FormListMenuDelegateTest {
    private lateinit var activity: FragmentActivity

    private val viewModel: FormListViewModel = mock()
    private val networkStateProvider: NetworkStateProvider = mock()

    private val menuInflater: SupportMenuInflater
        get() = SupportMenuInflater(activity)

    @Before
    fun setup() {
        whenever(networkStateProvider.isDeviceOnline).thenReturn(true)
        whenever(viewModel.isSyncingWithServer()).thenReturn(MutableLiveData(false))
        whenever(viewModel.isOutOfSyncWithServer()).thenReturn(MutableLiveData(false))

        activity = CollectHelpers.createThemedActivity(FragmentActivity::class.java)
    }

    @Test
    fun `onPrepareOptionsMenu when not out of sync shows sync icon`() {
        val menuDelegate = createMenuDelegate()
        val menu = createdMenu()

        menuDelegate.onCreateOptionsMenu(menuInflater, menu)
        menuDelegate.onPrepareOptionsMenu(menu)

        assertThat(
            Shadows.shadowOf(menu.findItem(R.id.menu_refresh).icon).createdFromResId,
            `is`(R.drawable.ic_baseline_refresh_24)
        )
    }

    @Test
    fun `onPrepareOptionsMenu when out of sync shows error sync icon`() {
        whenever(viewModel.isOutOfSyncWithServer()).thenReturn(MutableLiveData(true))

        val menuDelegate = createMenuDelegate()
        val menu = createdMenu()

        menuDelegate.onCreateOptionsMenu(menuInflater, menu)
        menuDelegate.onPrepareOptionsMenu(menu)

        assertThat(
            Shadows.shadowOf(menu.findItem(R.id.menu_refresh).icon).createdFromResId,
            `is`(R.drawable.ic_baseline_refresh_error_24)
        )
    }

    @Test
    fun `onPrepareOptionsMenu when syncing disables refresh button`() {
        whenever(viewModel.isSyncingWithServer()).thenReturn(MutableLiveData(true))

        val menuDelegate = createMenuDelegate()
        val menu = createdMenu()

        menuDelegate.onCreateOptionsMenu(menuInflater, menu)
        menuDelegate.onPrepareOptionsMenu(menu)

        assertThat(menu.findItem(R.id.menu_refresh).isEnabled, `is`(false))
    }

    @Test
    fun `onPrepareOptionsMenu when not syncing enables refresh button`() {
        val menuDelegate = createMenuDelegate()
        val menu = createdMenu()

        menuDelegate.onCreateOptionsMenu(menuInflater, menu)
        menuDelegate.onPrepareOptionsMenu(menu)

        assertThat(menu.findItem(R.id.menu_refresh).isEnabled, `is`(true))
    }

    @Test
    fun `clicking refresh for sync shows success toast`() {
        whenever(viewModel.syncWithServer()).thenReturn(MutableLiveData(true))

        val menuDelegate = createMenuDelegate()
        menuDelegate.onOptionsItemSelected(RoboMenuItem(R.id.menu_refresh))

        assertThat(
            ShadowToast.getTextOfLatestToast(), `is`(activity.getString(R.string.form_update_succeeded))
        )
    }

    @Test
    fun `clicking refresh for sync when syncing fails does not show toast`() {
        whenever(viewModel.syncWithServer()).thenReturn(MutableLiveData(false))

        val menuDelegate = createMenuDelegate()
        menuDelegate.onOptionsItemSelected(RoboMenuItem(R.id.menu_refresh))

        assertThat(ShadowToast.getLatestToast(), nullValue())
    }

    @Test
    fun `clicking refresh for sync when device is offline shows error toast and does not sync`() {
        whenever(networkStateProvider.isDeviceOnline).thenReturn(false)

        val menuDelegate = createMenuDelegate()
        menuDelegate.onOptionsItemSelected(RoboMenuItem(R.id.menu_refresh))

        assertThat(
            ShadowToast.getTextOfLatestToast(), `is`(activity.getString(R.string.no_connection))
        )
        verify(viewModel, never()).syncWithServer()
    }

    @Test
    fun `clicking sort displays sorting dialog`() {
        whenever(viewModel.sortingOrder).thenReturn(MutableNonNullLiveData(0))

        val menuDelegate = createMenuDelegate()
        menuDelegate.onOptionsItemSelected(RoboMenuItem(R.id.menu_sort))

        assertThat(ShadowDialog.getLatestDialog(), instanceOf(ListSortingDialog::class.java))
    }

    @Test
    fun `changing search text should set filterText in viewModel`() {
        whenever(viewModel.filterText).thenReturn(MutableNonNullLiveData(""))

        val menu = createdMenu()
        val menuDelegate = createMenuDelegate()

        menuDelegate.onCreateOptionsMenu(menuInflater, menu)
        menuDelegate.onPrepareOptionsMenu(menu)

        val searchView = (menu.findItem(R.id.menu_filter).actionView as SearchView).findViewById<SearchView.SearchAutoComplete>(androidx.appcompat.R.id.search_src_text)
        searchView.setText("abc")

        assertThat(viewModel.filterText.value, `is`("abc"))
    }

    @Test
    fun `clicking search when matchExactly enabled hides refresh and sort and then collapsing search shows them again`() {
        whenever(viewModel.isMatchExactlyEnabled()).thenReturn(true)

        val menu = createdMenu()
        val menuDelegate = createMenuDelegate()

        menuDelegate.onCreateOptionsMenu(menuInflater, menu)
        menuDelegate.onPrepareOptionsMenu(menu)

        menu.findItem(R.id.menu_filter).expandActionView()

        assertThat(menu.findItem(R.id.menu_refresh).isVisible, `is`(false))
        assertThat(menu.findItem(R.id.menu_sort).isVisible, `is`(false))

        menu.findItem(R.id.menu_filter).collapseActionView()

        assertThat(menu.findItem(R.id.menu_refresh).isVisible, `is`(true))
        assertThat(menu.findItem(R.id.menu_sort).isVisible, `is`(true))
    }

    @Test
    fun `collapsing search when matchExactly not enabled does not show refresh`() {
        whenever(viewModel.isMatchExactlyEnabled()).thenReturn(false)

        val menu = createdMenu()
        val menuDelegate = createMenuDelegate()

        menuDelegate.onCreateOptionsMenu(menuInflater, menu)
        menuDelegate.onPrepareOptionsMenu(menu)

        menu.findItem(R.id.menu_filter).expandActionView()
        menu.findItem(R.id.menu_filter).collapseActionView()

        assertThat(menu.findItem(R.id.menu_refresh).isVisible, `is`(false))
    }

    private fun createMenuDelegate(): FormListMenuDelegate {
        return FormListMenuDelegate(activity, viewModel, networkStateProvider)
    }

    private fun createdMenu(): Menu {
        val menu: SupportMenu = MenuBuilder(activity)
        menuInflater.inflate(R.menu.list_menu, menu)
        return menu
    }
}
