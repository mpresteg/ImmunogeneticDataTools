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

import org.dash.valid.Locus;

/**
 * Detects Histogenetics' page-1 table when it's filled with a single placeholder word
 * instead of real values -- e.g. every cell reads {@code "FAILED"} or every cell reads
 * {@code "PENDING"}. See issue #51.
 *
 * Parameterized by which placeholder word to look for, rather than three
 * near-identical classes: confirmed against the real fixture that FAILED, PENDING, and
 * the XXXX/narrative-ambiguity report (see {@link HistogeneticsNarrativeAmbiguityDetector}
 * for its separate narrative-text piece) all use the *exact same* table shape as
 * {@link HistogeneticsPageOneTableDetector}'s real-result case -- a header row of locus
 * labels, then 2 data rows, column-aligned. Only the cell values differ. That's three
 * independent real confirmations of one shape, not a guess extended from one sample --
 * the same bar {@link HistogeneticsCode} and {@link LocusLookup} were held to before
 * being factored out.
 *
 * One quirk observed in two of the three real placeholder blocks (FAILED and XXXX, not
 * PENDING): the DRB345 column's two cells aren't bare placeholders, they're
 * {@code "DRB3*FAILED"} / {@code "DRB4*FAILED"} (or the XXXX equivalent) -- a
 * locus-sub-gene prefix attached to the placeholder. Matched via
 * {@link #cellMatchesPlaceholder(String)} handling both the bare and prefixed forms
 * with one check, rather than the DRB345-specific special-casing
 * {@code CegatLocusResultLineDetector} needed for its very different report shape.
 */
public class HistogeneticsPlaceholderTableDetector {
	private static final String TYPING_STATUS_PREFIX = "Typing Status :";

	private final HistogeneticsNoiseFilter noiseFilter = new HistogeneticsNoiseFilter();
	private final String placeholder;

	/**
	 * @param placeholder the exact placeholder word to look for, e.g. "FAILED",
	 *                     "PENDING", or "XXXX"
	 */
	public HistogeneticsPlaceholderTableDetector(String placeholder) {
		this.placeholder = placeholder;
	}

	public List<HistogeneticsPlaceholderResultCandidate> detect(String extractedText) {
		List<NumberedLine> lines = noiseFilter.filterNoise(extractedText);
		List<HistogeneticsPlaceholderResultCandidate> candidates = new ArrayList<>();

		String currentSampleId = null;
		int i = 0;
		while (i < lines.size()) {
			String trimmedLine = lines.get(i).getText().trim();

			String sampleId = HistogeneticsSampleBlockHeader.extractSampleId(trimmedLine);
			if (sampleId != null) {
				currentSampleId = sampleId;
				i++;
				continue;
			}

			List<Locus> headerLoci = HistogeneticsSampleBlockHeader.parseLocusHeaderRow(trimmedLine);
			if (headerLoci == null || i + 2 >= lines.size()) {
				i++;
				continue;
			}

			String[] row1 = lines.get(i + 1).getText().trim().split("\\s+");
			String[] row2 = lines.get(i + 2).getText().trim().split("\\s+");
			if (row1.length != headerLoci.size() || row2.length != headerLoci.size()) {
				i++;
				continue;
			}

			List<Locus> affectedLoci = findAffectedLoci(headerLoci, row1, row2);
			int nextIndex = i + 3;

			if (affectedLoci.isEmpty()) {
				// This placeholder word wasn't found anywhere in this block -- e.g.
				// running the "FAILED" detector against the real Complete result, or
				// against the PENDING block. Correctly not a candidate for this
				// detector instance.
				i = nextIndex;
				continue;
			}

			String typingStatusText = null;
			if (nextIndex < lines.size() && lines.get(nextIndex).getText().trim().startsWith(TYPING_STATUS_PREFIX)) {
				typingStatusText = lines.get(nextIndex).getText().trim().substring(TYPING_STATUS_PREFIX.length()).trim();
				nextIndex++;
			}

			candidates.add(new HistogeneticsPlaceholderResultCandidate(currentSampleId, placeholder, affectedLoci,
					typingStatusText, lines.get(i).getText().trim(), lines.get(i).getLineNumber()));

			i = nextIndex;
		}

		return candidates;
	}

	private List<Locus> findAffectedLoci(List<Locus> headerLoci, String[] row1, String[] row2) {
		List<Locus> affected = new ArrayList<>();
		for (int column = 0; column < headerLoci.size(); column++) {
			if (cellMatchesPlaceholder(row1[column]) || cellMatchesPlaceholder(row2[column])) {
				affected.add(headerLoci.get(column));
			}
		}
		return affected;
	}

	// Handles both the bare form ("FAILED") and the DRB345-column prefixed form
	// ("DRB3*FAILED") with one check -- see this class's own comment.
	private boolean cellMatchesPlaceholder(String cell) {
		return cell.equals(placeholder) || cell.endsWith("*" + placeholder);
	}
}
