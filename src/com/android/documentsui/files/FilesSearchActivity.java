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

import static com.android.documentsui.util.Material3Config.getRes;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.documentsui.R;
import com.android.documentsui.queries.SearchIntentQuery;
import com.android.documentsui.queries.SearchViewManager;

/**
 * DiamaneOS: opens Files searching for words another app hands it ({@link Intent#ACTION_SEARCH}
 * with {@link SearchManager#QUERY}), as Home's search does. Files' own search takes them
 * ({@link SearchViewManager#parseQueryContentFromIntent}), as typed into its search field.
 *
 * <p>Only the words are read from the caller's intent, cleaned and capped: no other extra, no
 * data, no flags. Files opens in browse mode ({@link FilesActivity}), never as a picker, in a task
 * of its own, so nothing goes back to the caller. It starts in internal storage, so the search
 * covers all of it ({@link ActionHandler#initLocation}). This activity shows nothing and finishes
 * at once.
 */
public class FilesSearchActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            startActivity(createSearchIntent(this, queryOf(getIntent())));
        }
        finish();
    }

    /** The words to search for in {@code intent}, cleaned and capped, or null. */
    @VisibleForTesting
    static @Nullable String queryOf(@Nullable Intent intent) {
        if (intent == null || !Intent.ACTION_SEARCH.equals(intent.getAction())) {
            return null;
        }
        try {
            return SearchIntentQuery.cleanIntentQuery(intent.getStringExtra(SearchManager.QUERY));
        } catch (RuntimeException e) {
            // Extras that cannot be read: no words, and Files opens as usual.
            return null;
        }
    }

    /**
     * The intent that opens Files (browse mode, looking as when opened from the launcher)
     * searching for {@code query}, or just opens it when there is none.
     */
    @VisibleForTesting
    static Intent createSearchIntent(Context context, @Nullable String query) {
        final Intent intent = new Intent(context, FilesActivity.class);
        intent.putExtra(LauncherActivity.TASK_LABEL_RES, getRes(R.string.launcher_label));
        intent.putExtra(LauncherActivity.TASK_ICON_RES, getRes(R.drawable.launcher_icon));
        if (query != null) {
            intent.setAction(Intent.ACTION_SEARCH);
            intent.putExtra(SearchManager.QUERY, query);
        }
        return intent;
    }
}
