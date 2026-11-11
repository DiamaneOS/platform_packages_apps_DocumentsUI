/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.documentsui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.platform.test.annotations.EnableFlags
import android.view.MenuItem
import android.widget.ActionMenuView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.android.documentsui.flags.Flags.FLAG_USE_MATERIAL3
import com.android.documentsui.flags.Flags.FLAG_VISUAL_SIGNALS_RO
import com.android.documentsui.rules.OverrideFlagsRule
import com.android.documentsui.services.FileOperationService
import com.android.documentsui.services.FileOperationService.ACTION_PROGRESS
import com.android.documentsui.services.FileOperationService.EXTRA_PROCESS_TOKEN
import com.android.documentsui.services.FileOperationService.EXTRA_PROGRESS
import com.android.documentsui.services.Job
import com.android.documentsui.services.JobProgress
import com.android.documentsui.testing.MutableJobProgress
import com.android.documentsui.testing.TestActionHandler
import com.android.documentsui.util.Material3Config.Companion.getRes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@SmallTest
@EnableFlags(FLAG_USE_MATERIAL3, FLAG_VISUAL_SIGNALS_RO)
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class JobPanelControllerTest {
    @get:Rule val setFlags = OverrideFlagsRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var progressBar: ProgressBar
    private lateinit var badge: ImageView
    private lateinit var menuItem: MenuItem

    private lateinit var viewModel: JobPanelViewModel
    private lateinit var controller: JobPanelController
    private var lastId = 0L

    private fun sendProgress(
        progress: ArrayList<JobProgress>,
        id: Long = lastId++,
        token: IBinder? = null,
    ) {
        var intent =
            Intent(ACTION_PROGRESS).apply {
                `package` = context.packageName
                putExtra("id", id)
                putParcelableArrayListExtra(EXTRA_PROGRESS, progress)
                if (token != null) {
                    putExtras(Bundle().apply { putBinder(EXTRA_PROCESS_TOKEN, token) })
                }
            }
        controller.onReceive(context, intent)
    }

    private fun linkedDeathRecipient(token: IBinder): IBinder.DeathRecipient {
        val recipient = ArgumentCaptor.forClass(IBinder.DeathRecipient::class.java)
        verify(token).linkToDeath(recipient.capture(), eq(0))
        return recipient.value
    }

    private fun runningCopy() =
        MutableJobProgress(
            id = "jobId1",
            operationType = FileOperationService.OPERATION_COPY,
            state = Job.STATE_SET_UP,
            filename = "big.bin",
            numFiles = 1,
            hasFailures = false,
            currentBytes = 4,
            requiredBytes = 10,
            msRemaining = -1,
        )

    @Before
    fun setUp() {
        // The default progress bar only has an indeterminate state, so we need to style it to allow
        // determinate progress.
        progressBar =
            ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                id = getRes(R.id.job_progress_toolbar_indicator)
            }
        badge = ImageView(context).apply { id = getRes(R.id.job_progress_toolbar_badge) }
        menuItem =
            ActionMenuView(context).menu.add("job_panel").apply {
                actionView =
                    FrameLayout(context).apply {
                        id = getRes(R.id.job_progress_toolbar_container)
                        addView(progressBar)
                        addView(badge)
                    }
            }

        viewModel = JobPanelViewModel()
        controller = JobPanelController(context, TestActionHandler(), viewModel)
        TestScope().launch(UnconfinedTestDispatcher()) { controller.observeViewModel() }
        controller.setMenuItem(menuItem)
    }

    @Test
    fun testSingleJob() {
        assertFalse(menuItem.isVisible())
        assertFalse(menuItem.isEnabled())

        val progress =
            MutableJobProgress(
                id = "jobId1",
                operationType = FileOperationService.OPERATION_COPY,
                state = Job.STATE_STARTED,
                numFiles = 1,
                hasFailures = false,
                currentBytes = 0,
                requiredBytes = 10,
                msRemaining = -1,
            )
        sendProgress(arrayListOf(progress.toJobProgress()))

        assertTrue(menuItem.isVisible())
        assertTrue(menuItem.isEnabled())
        assertEquals(0, progressBar.progress)

        progress.apply {
            state = Job.STATE_SET_UP
            currentBytes = 4
        }
        sendProgress(arrayListOf(progress.toJobProgress()))

        assertTrue(menuItem.isVisible())
        assertTrue(menuItem.isEnabled())
        assertEquals(40, progressBar.progress)

        progress.apply {
            state = Job.STATE_COMPLETED
            currentBytes = 10
        }
        sendProgress(arrayListOf(progress.toJobProgress()))

        assertTrue(menuItem.isVisible())
        assertTrue(menuItem.isEnabled())
        assertEquals(100, progressBar.progress)
    }

    @Test
    fun testMultipleJobs() {
        assertFalse(menuItem.isVisible())
        assertFalse(menuItem.isEnabled())

        val progress1 =
            MutableJobProgress(
                id = "jobId1",
                operationType = FileOperationService.OPERATION_MOVE,
                state = Job.STATE_STARTED,
                numFiles = 1,
                hasFailures = false,
                currentBytes = 0,
                requiredBytes = 10,
                msRemaining = -1,
            )
        val progress2 =
            MutableJobProgress(
                id = "jobId2",
                operationType = FileOperationService.OPERATION_DELETE,
                state = Job.STATE_STARTED,
                numFiles = 1,
                hasFailures = false,
                currentBytes = 0,
                requiredBytes = 50,
                msRemaining = -1,
            )
        sendProgress(arrayListOf(progress1.toJobProgress(), progress2.toJobProgress()))

        assertTrue(menuItem.isVisible())
        assertTrue(menuItem.isEnabled())
        assertEquals(0, progressBar.progress)

        progress1.apply {
            state = Job.STATE_SET_UP
            currentBytes = 4
        }
        sendProgress(arrayListOf(progress1.toJobProgress(), progress2.toJobProgress()))

        assertTrue(menuItem.isVisible())
        assertTrue(menuItem.isEnabled())
        assertEquals(20, progressBar.progress)

        progress1.apply {
            state = Job.STATE_COMPLETED
            currentBytes = 10
        }
        sendProgress(arrayListOf(progress1.toJobProgress(), progress2.toJobProgress()))

        assertTrue(menuItem.isVisible())
        assertTrue(menuItem.isEnabled())
        assertEquals(50, progressBar.progress)

        progress2.apply {
            state = Job.STATE_SET_UP
            currentBytes = 30
        }
        sendProgress(arrayListOf(progress1.toJobProgress(), progress2.toJobProgress()))

        assertTrue(menuItem.isVisible())
        assertTrue(menuItem.isEnabled())
        assertEquals(80, progressBar.progress)

        progress2.apply {
            state = Job.STATE_COMPLETED
            currentBytes = 40
        }
        sendProgress(arrayListOf(progress1.toJobProgress(), progress2.toJobProgress()))

        assertTrue(menuItem.isVisible())
        assertTrue(menuItem.isEnabled())
        assertEquals(100, progressBar.progress)
    }

    @Test
    fun testIndeterminateJobs() {
        val indeterminate =
            MutableJobProgress(
                id = "indeterminate",
                operationType = FileOperationService.OPERATION_MOVE,
                state = Job.STATE_SET_UP,
                numFiles = 1,
                hasFailures = false,
                currentBytes = -1,
                requiredBytes = -1,
                msRemaining = -1,
            )
        val determinate =
            MutableJobProgress(
                id = "determinate",
                operationType = FileOperationService.OPERATION_COPY,
                state = Job.STATE_SET_UP,
                numFiles = 1,
                hasFailures = false,
                currentBytes = 40,
                requiredBytes = 100,
                msRemaining = -1,
            )
        sendProgress(arrayListOf(indeterminate.toJobProgress()))

        assertTrue(menuItem.isVisible())
        assertTrue(progressBar.isIndeterminate)

        sendProgress(arrayListOf(indeterminate.toJobProgress(), determinate.toJobProgress()))

        assertTrue(menuItem.isVisible())
        assertFalse(progressBar.isIndeterminate)
        assertEquals(20, progressBar.progress)
    }

    @Test
    fun onDestroy_unregistersReceiver() {
        // Create a mock context to verify interactions.
        val mockContext = mock<Context>()
        // The controller registers itself as a receiver in the init block.
        val controller = JobPanelController(mockContext, TestActionHandler(), JobPanelViewModel())
        // Call onDestroy, which should unregister the receiver.
        controller.onDestroy(mock())
        // Verify that unregisterReceiver was called on the context.
        verify(mockContext).unregisterReceiver(controller)
    }

    @Test
    fun testServiceDeathFailsRunningJobs() {
        val lost = ArrayList<JobProgress>()
        controller.jobLostNotifier = { _, jobProgress -> lost.add(jobProgress) }
        val token = mock<IBinder>()
        sendProgress(arrayListOf(runningCopy().toJobProgress()), token = token)

        linkedDeathRecipient(token).binderDied()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        val failed = viewModel.currentJobs.getValue("jobId1").jobProgress
        assertEquals(Job.STATE_COMPLETED, failed.state)
        assertTrue(failed.hasFailures)
        assertEquals(arrayListOf("big.bin"), failed.failedPaths)
        assertEquals(listOf(failed), lost)
    }

    @Test
    fun testServiceDeathLeavesFinishedJobs() {
        val lost = ArrayList<JobProgress>()
        controller.jobLostNotifier = { _, jobProgress -> lost.add(jobProgress) }
        val token = mock<IBinder>()
        val done =
            MutableJobProgress(
                    id = "jobId1",
                    operationType = FileOperationService.OPERATION_COPY,
                    state = Job.STATE_COMPLETED,
                    numFiles = 1,
                    hasFailures = false,
                )
                .toJobProgress()
        sendProgress(arrayListOf(done), token = token)

        linkedDeathRecipient(token).binderDied()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertEquals(done, viewModel.currentJobs.getValue("jobId1").jobProgress)
        assertTrue(lost.isEmpty())
    }

    @Test
    fun testServiceDeathLinkIsDropped() {
        val lost = ArrayList<JobProgress>()
        controller.jobLostNotifier = { _, jobProgress -> lost.add(jobProgress) }
        val oldToken = mock<IBinder>()
        val newToken = mock<IBinder>()
        val progress = arrayListOf(runningCopy().toJobProgress())

        // The same process is linked once.
        sendProgress(progress, token = oldToken)
        sendProgress(progress, token = oldToken)
        val oldRecipient = linkedDeathRecipient(oldToken)

        // A newer process replaces the link.
        sendProgress(progress, token = newToken)
        verify(oldToken).unlinkToDeath(oldRecipient, 0)
        val newRecipient = linkedDeathRecipient(newToken)

        controller.onDestroy(mock())
        verify(newToken).unlinkToDeath(newRecipient, 0)

        // Deaths seen after that change nothing.
        oldRecipient.binderDied()
        newRecipient.binderDied()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertFalse(viewModel.currentJobs.getValue("jobId1").jobProgress.isFinal)
        assertTrue(lost.isEmpty())
    }
}
