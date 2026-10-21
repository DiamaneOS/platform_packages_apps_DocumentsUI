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

package com.android.documentsui.queries;

import static org.junit.Assert.assertEquals;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public final class SearchIntentQueryTest {

    @Test
    public void testCleanIntentQuery() {
        assertEquals(null, SearchIntentQuery.cleanIntentQuery(null));
        assertEquals(null, SearchIntentQuery.cleanIntentQuery(" \u0000\u202e "));
        assertEquals("a b", SearchIntentQuery.cleanIntentQuery("a\u0007\u202eb"));
        final int max = SearchIntentQuery.MAX_INTENT_QUERY_LENGTH;
        assertEquals(max, SearchIntentQuery.cleanIntentQuery("x".repeat(max + 50)).length());
        // Never half of a character outside the basic plane.
        final String emoji = "x".repeat(max - 1) + "\ud83d\ude00";
        assertEquals(max - 1, SearchIntentQuery.cleanIntentQuery(emoji).length());
    }
}
