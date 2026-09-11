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
package org.nmdp.hlareport.candidate;

import java.util.regex.Pattern;

/**
 * Recognizes a single whitespace-delimited token as allele-shaped (e.g. {@code A*24:02},
 * {@code DRB4*01:03}, {@code C*07:04:01G}) without deciding whether it's a real, valid
 * allele -- that's IMGT/HLA reference data's job, not this module's (see the module
 * README's "No separate reference-data sourcing needed" principle). This is a shape
 * check only, used to decide whether a line looks like a locus-result row worth
 * surfacing as a candidate.
 *
 * Requires at least two colon-delimited fields (a bare "A*24" isn't allele-shaped on its
 * own in any report seen so far), matching the two-field minimum GLStringUtilities
 * already treats as the floor for a real allele (see its P_GROUP_LEVEL constant).
 */
public final class AlleleToken {
	private static final Pattern ALLELE_PATTERN = Pattern.compile("^([A-Za-z0-9]+)\\*[0-9]{1,4}(?::[0-9]{1,4}){1,3}[A-Za-z]?$");

	private AlleleToken() {
	}

	public static boolean matches(String token) {
		return token != null && ALLELE_PATTERN.matcher(token).matches();
	}

	/**
	 * @param alleleToken a token for which {@link #matches(String)} is true
	 * @return the locus-prefix portion before the "*", e.g. "DRB4" for "DRB4*01:03"
	 */
	public static String locusPrefix(String alleleToken) {
		int asteriskIndex = alleleToken.indexOf('*');
		return asteriskIndex < 0 ? null : alleleToken.substring(0, asteriskIndex);
	}
}
