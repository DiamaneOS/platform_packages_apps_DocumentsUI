/*
 * Copyright (C) 2026 The DiamaneOS Project
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

package com.android.documentsui.files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

/** What Files takes from a search another app hands it, and how it opens. */
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class FilesSearchActivityTest {

    private final Context mContext =
            InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Test
    public void testQueryOf_onlyTheSearchQuery() {
        final Intent intent = new Intent(Intent.ACTION_SEARCH)
                .putExtra(SearchManager.QUERY, " batt\n")
                .putExtra(Intent.EXTRA_TITLE, "title");
        assertEquals("batt", FilesSearchActivity.queryOf(intent));
    }

    @Test
    public void testQueryOf_noWordsWithoutTheSearchAction() {
        assertNull(FilesSearchActivity.queryOf(null));
        assertNull(FilesSearchActivity.queryOf(
                new Intent(Intent.ACTION_VIEW).putExtra(SearchManager.QUERY, "batt")));
        assertNull(FilesSearchActivity.queryOf(new Intent(Intent.ACTION_SEARCH)));
    }

    @Test
    public void testCreateSearchIntent_browseModeWithOnlyTheWords() {
        final Intent intent = FilesSearchActivity.createSearchIntent(mContext, "batt");
        assertEquals(FilesActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals(Intent.ACTION_SEARCH, intent.getAction());
        assertNull(intent.getData());
        assertEquals(0, intent.getFlags());
        final Bundle extras = intent.getExtras();
        assertEquals(Set.of(SearchManager.QUERY, LauncherActivity.TASK_LABEL_RES,
                LauncherActivity.TASK_ICON_RES), extras.keySet());
        assertEquals("batt", extras.getString(SearchManager.QUERY));
    }

    @Test
    public void testCreateSearchIntent_noWordsJustOpensFiles() {
        final Intent intent = FilesSearchActivity.createSearchIntent(mContext, null);
        assertEquals(FilesActivity.class.getName(), intent.getComponent().getClassName());
        assertNull(intent.getAction());
        assertNull(intent.getStringExtra(SearchManager.QUERY));
    }
}
