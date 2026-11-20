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
 * Detects Histogenetics' narrative-ambiguity sentences in a report's "Report History"
 * section -- one per locus, e.g.
 * {@code "Possible Allele in A : 01:01:01/02:01:01/30:01:01."} -- rather than a table
 * row. See issue #51. A prose-parsing detector, not a table-shape one, unlike every
 * other detector in this module (including {@link HistogeneticsPlaceholderTableDetector},
 * which handles this same report's page-1 XXXX table -- this class is strictly the
 * separate narrative piece).
 *
 * The first such sentence shares a physical line with a
 * {@code "Sample Id :"} field (note the casing/spacing: genuinely different text from
 * the appendix's own {@code "Sample ID :"} marker, issue #50 -- confirmed in the real
 * report, not normalized away as if it were the same string with a typo). Searched with
 * {@link Matcher#find()}, not {@code matches()}, since the sentence can be preceded by
 * other text (a date, that Sample Id field) on the same line.
 */
public class HistogeneticsNarrativeAmbiguityDetector {
	private static final String SAMPLE_ID_MARKER = "Sample Id :";
	private static final String POSSIBLE_ALLELE_MARKER = "Possible Allele in";

	private static final Pattern POSSIBLE_ALLELE_PATTERN = Pattern
			.compile(POSSIBLE_ALLELE_MARKER + " (\\S+)\\s*:\\s*(.*?)\\.\\s*$");

	private final HistogeneticsNoiseFilter noiseFilter = new HistogeneticsNoiseFilter();

	public List<HistogeneticsNarrativeAmbiguityEntry> detect(String extractedText) {
		List<NumberedLine> lines = noiseFilter.filterNoise(extractedText);
		List<HistogeneticsNarrativeAmbiguityEntry> entries = new ArrayList<>();

		String currentSampleId = null;
		for (NumberedLine line : lines) {
			String trimmedLine = line.getText().trim();

			int sampleIdIndex = trimmedLine.indexOf(SAMPLE_ID_MARKER);
			if (sampleIdIndex >= 0) {
				String afterMarker = trimmedLine.substring(sampleIdIndex + SAMPLE_ID_MARKER.length());
				int possibleAlleleIndex = afterMarker.indexOf(POSSIBLE_ALLELE_MARKER);
				currentSampleId = (possibleAlleleIndex >= 0 ? afterMarker.substring(0, possibleAlleleIndex) : afterMarker)
						.trim();
			}

			Matcher matcher = POSSIBLE_ALLELE_PATTERN.matcher(trimmedLine);
			if (!matcher.find()) {
				continue;
			}

			Locus locus = LocusLookup.byShortName(matcher.group(1));
			if (locus == null) {
				// Not a recognized locus name -- surfaced as no entry for this line,
				// not a guess. Not expected against the one real sample this was built
				// against, but not assumed impossible either.
				continue;
			}

			entries.add(new HistogeneticsNarrativeAmbiguityEntry(currentSampleId, locus, matcher.group(2).trim(),
					trimmedLine, line.getLineNumber()));
		}

		return entries;
	}
}
