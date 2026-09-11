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

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.dash.valid.Locus;

/**
 * A fast, silent locus-shortName lookup, shared by every per-lab detector (originally
 * factored out of CegatLocusResultLineDetector when VersitiLocusResultLineDetector
 * needed the identical lookup -- see issue #42).
 *
 * Deliberately not just {@code org.dash.valid.Locus#lookup(String)}: that method logs a
 * WARNING on every miss, which is fine when called on text a human already expects to
 * be a locus name, but not when probing arbitrary report lines/tokens -- almost every
 * line in a real report isn't a locus label, and warning-per-miss would spam the log
 * once per non-matching line in the whole document.
 */
public final class LocusLookup {
	private static final Map<String, Locus> BY_SHORT_NAME = Arrays.stream(Locus.values())
			.collect(Collectors.toMap(Locus::getShortName, locus -> locus));

	private LocusLookup() {
	}

	/**
	 * @param shortName e.g. "A", "DRB1", "DRB345"
	 * @return the matching Locus, or null if shortName isn't a recognized locus label
	 *         (silently -- see class comment for why this doesn't delegate to
	 *         {@code Locus.lookup()})
	 */
	public static Locus byShortName(String shortName) {
		return BY_SHORT_NAME.get(shortName);
	}
}
