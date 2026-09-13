/*

    Copyright (c) 2014-2015 National Marrow Donor Program (NMDP)

    This library is free software; you can redistribute it and/or modify it
    under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation; either version 3 of the License, or (at
    your option) any later version.

    This library is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
    License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this library;  if not, write to the Free Software Foundation,
    Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA.

    > http://www.gnu.org/licenses/lgpl.html

*/
package org.nmdp.hlareport.candidate.histogenetics;

import java.util.regex.Pattern;

/**
 * Recognizes Histogenetics' G-code / NMDP-code value shape (e.g. {@code "02:01:01G"},
 * {@code "02:DKCVG"}, {@code "13:01:01"}) -- shared by {@link HistogeneticsAppendixAccumulator}
 * (issue #50) and the page-1 table detector (issue #49), the first genuine sign of what
 * those two Histogenetics-specific pieces have in common, same as {@code LocusLookup}
 * was for CeGaT/Versiti.
 *
 * This shape check is also how the page-1 detector distinguishes a real result from a
 * FAILED/PENDING/XXXX placeholder -- confirmed against the real fixture that
 * {@code "Typing Status : Complete"} is NOT a reliable signal for that (it appears on
 * the FAILED and XXXX/narrative-ambiguity reports too, apparently meaning "this report
 * was finalized," not "typing succeeded"). {@link #isCodeShaped(String)} on the actual
 * reported value is what correctly excludes those cases: none of FAILED, PENDING, XXXX,
 * or NA start with a digit.
 */
public final class HistogeneticsCode {
	// Covers both a real G-code ("02:01:01G") and an NMDP allele code ("02:DMFHE",
	// "13:01:01") -- digit-led, colon-separated fields where any field may be digits,
	// letters, or both.
	private static final Pattern CODE_TOKEN_PATTERN = Pattern.compile("^[0-9]+(:[0-9A-Za-z]+)*$");

	private HistogeneticsCode() {
	}

	/**
	 * @param value a token from a report's G-code/NMDP-code column
	 * @return true if it's shaped like a code at all (G-code or NMDP-code) -- false for
	 *         placeholders like "NA", "FAILED", "PENDING", "XXXX", which never start
	 *         with a digit
	 */
	public static boolean isCodeShaped(String value) {
		return value != null && CODE_TOKEN_PATTERN.matcher(value).matches();
	}

	/**
	 * @param value a token already confirmed {@link #isCodeShaped(String)}
	 * @return true if it's a real G-code (ends in a digit followed by "G", per the
	 *         report's own note #3: "Allele bearing suffix G"), false if it's an
	 *         NMDP-code fallback (e.g. "02:DKCVG", which ends in a LETTER followed by
	 *         "G" -- not the same shape). A convenience interpretation, not an
	 *         authoritative decode -- a reviewer can always check the exact text
	 *         themselves.
	 */
	public static boolean isGCode(String value) {
		return value != null && value.length() >= 2 && value.charAt(value.length() - 1) == 'G'
				&& Character.isDigit(value.charAt(value.length() - 2));
	}
}
