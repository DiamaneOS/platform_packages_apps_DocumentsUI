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

import androidx.annotation.Nullable;

/** DiamaneOS: the words another app hands Files to search for. */
public final class SearchIntentQuery {
    /** DiamaneOS: the most of the words another app hands Files to search for that are kept. */
    public static final int MAX_INTENT_QUERY_LENGTH = 200;

    /**
     * DiamaneOS: the words another app hands Files to search for, as searched: control and format
     * characters become spaces, runs of spaces one, trimmed, and at most
     * {@link #MAX_INTENT_QUERY_LENGTH} characters (never half a character). Null when nothing is
     * left.
     */
    public static @Nullable String cleanIntentQuery(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        final StringBuilder out = new StringBuilder(Math.min(raw.length(), 256));
        boolean space = false;
        for (int i = 0; i < raw.length(); ) {
            final int cp = raw.codePointAt(i);
            i += Character.charCount(cp);
            if (Character.isWhitespace(cp) || Character.isSpaceChar(cp)
                    || Character.isISOControl(cp) || Character.getType(cp) == Character.FORMAT) {
                space = out.length() > 0;
                continue;
            }
            final int width = Character.charCount(cp) + (space ? 1 : 0);
            if (out.length() + width > MAX_INTENT_QUERY_LENGTH) {
                break;
            }
            if (space) {
                out.append(' ');
                space = false;
            }
            out.appendCodePoint(cp);
        }
        return out.length() == 0 ? null : out.toString();
    }

    private SearchIntentQuery() {}
}
