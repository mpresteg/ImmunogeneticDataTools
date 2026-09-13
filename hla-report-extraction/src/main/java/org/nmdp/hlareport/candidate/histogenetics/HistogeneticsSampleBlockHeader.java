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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dash.valid.Locus;
import org.nmdp.hlareport.candidate.LocusLookup;

/**
 * Two pieces shared by every detector that reads Histogenetics' page-1-style sample
 * blocks (a real "Complete" result -- {@link HistogeneticsPageOneTableDetector}, issue
 * #49 -- or a FAILED/PENDING/XXXX placeholder block --
 * {@link HistogeneticsPlaceholderTableDetector}, issue #51): recognizing the
 * {@code "Histo ID :"} sample marker, and parsing the 9-column locus-label header row
 * (e.g. {@code "A* B* C* DRB1* DRB345* DQB1* DQA1* DPB1* DPA1*"}).
 *
 * Factored out once #51 needed the identical logic #49 already had -- confirmed real
 * duplication, not a preemptive abstraction (same discipline as {@link HistogeneticsCode}
 * and {@link LocusLookup}).
 */
final class HistogeneticsSampleBlockHeader {
	static final String HISTO_ID_MARKER = "Histo ID :";

	// e.g. "A*", "DRB345*" -- a locus header token is just the locus's own shortName
	// with a trailing "*", nothing else on the token.
	private static final Pattern LOCUS_HEADER_TOKEN_PATTERN = Pattern.compile("^([A-Za-z0-9]+)\\*$");

	private HistogeneticsSampleBlockHeader() {
	}

	/**
	 * @param trimmedLine a line possibly containing the "Histo ID :" marker anywhere in
	 *                    it (it shares a physical line with an unrelated "First Name :"
	 *                    field in the real report)
	 * @return the sample id (everything after the marker, trimmed), or null if this
	 *         line doesn't contain the marker at all
	 */
	static String extractSampleId(String trimmedLine) {
		int histoIdIndex = trimmedLine.indexOf(HISTO_ID_MARKER);
		if (histoIdIndex < 0) {
			return null;
		}
		return trimmedLine.substring(histoIdIndex + HISTO_ID_MARKER.length()).trim();
	}

	/**
	 * @param trimmedLine a candidate locus-label header row
	 * @return the loci in column order, or null if trimmedLine doesn't match this
	 *         shape at all (every token must be a recognized locus shortName plus a
	 *         trailing "*", nothing else)
	 */
	static List<Locus> parseLocusHeaderRow(String trimmedLine) {
		String[] tokens = trimmedLine.split("\\s+");
		if (tokens.length == 0) {
			return null;
		}

		List<Locus> loci = new ArrayList<>();
		for (String token : tokens) {
			Matcher matcher = LOCUS_HEADER_TOKEN_PATTERN.matcher(token);
			if (!matcher.matches()) {
				return null;
			}
			Locus locus = LocusLookup.byShortName(matcher.group(1));
			if (locus == null) {
				return null;
			}
			loci.add(locus);
		}

		return loci;
	}
}
