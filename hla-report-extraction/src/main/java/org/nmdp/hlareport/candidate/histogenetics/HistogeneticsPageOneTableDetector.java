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
 * Detects Histogenetics' page-1 summary table: a header row of locus labels (e.g.
 * {@code "A* B* C* DRB1* DRB345* DQB1* DQA1* DPB1* DPA1*"}), followed by exactly 2 data
 * rows whose values line up with the header BY COLUMN POSITION -- a genuinely different
 * shape from every other detector in this module, all of which read one locus per
 * line. Up to 2 values per locus (heterozygous), read down each locus's own column
 * across both data rows, not across a row. See issue #49.
 *
 * A locus's value is "NA" when not typed at all, or (on a report that isn't the
 * "Complete" case this issue is scoped to) a placeholder like FAILED/PENDING/XXXX. This
 * detector doesn't special-case any of those -- it just checks whether a value is
 * {@link HistogeneticsCode#isCodeShaped(String)}, which is false for all of them
 * (none start with a digit). Confirmed against the real fixture that this is the
 * *correct* way to tell a real result apart from a placeholder here: the report's own
 * {@code "Typing Status :"} field is NOT reliable for that -- it reads "Complete" on
 * the FAILED and XXXX/narrative-ambiguity reports too (see the module README). A block
 * where every locus is a placeholder simply produces zero code-shaped values across the
 * whole table, so no candidate is produced for it at all -- the same "0 candidates,
 * not an error" convention every other detector already uses for a report shape it
 * doesn't recognize.
 *
 * Those placeholder blocks aren't just ignored, though -- see
 * {@link HistogeneticsPlaceholderTableDetector} (issue #51), which reads the identical
 * table shape looking for exactly the placeholders this class skips. The two share
 * {@link HistogeneticsSampleBlockHeader} for recognizing a sample marker and parsing
 * the locus-label header row, factored out once #51 needed the identical logic this
 * class already had.
 */
public class HistogeneticsPageOneTableDetector {
	private static final String TYPING_STATUS_PREFIX = "Typing Status :";
	private static final String MATCHING_RATIO_PREFIX = "Matching Ratio with Patient :";
	private static final String NULL_ALLELE_STATUS_PREFIX = "Null Allele Resolution Status :";
	private static final String NULL_ALLELE_STATUS_TERMINATOR = "excluded";

	private final HistogeneticsNoiseFilter noiseFilter = new HistogeneticsNoiseFilter();

	public List<HistogeneticsSampleResultCandidate> detect(String extractedText) {
		List<NumberedLine> lines = noiseFilter.filterNoise(extractedText);
		List<HistogeneticsSampleResultCandidate> candidates = new ArrayList<>();

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
				// Not the shape expected -- surfaced as no candidate here, not an error.
				i++;
				continue;
			}

			List<HistogeneticsLocusValues> locusValues = zipLocusValues(headerLoci, row1, row2);
			int nextIndex = i + 3;

			if (locusValues.isEmpty()) {
				// Nothing code-shaped anywhere in this block -- e.g. FAILED/PENDING/
				// XXXX. Correctly not a candidate; see this class's own comment on why
				// "Typing Status" can't be used to decide this instead.
				i = nextIndex;
				continue;
			}

			String typingStatusText = null;
			if (nextIndex < lines.size() && lines.get(nextIndex).getText().trim().startsWith(TYPING_STATUS_PREFIX)) {
				typingStatusText = lines.get(nextIndex).getText().trim().substring(TYPING_STATUS_PREFIX.length()).trim();
				nextIndex++;
			}

			String matchingRatio = null;
			if (nextIndex < lines.size() && lines.get(nextIndex).getText().trim().startsWith(MATCHING_RATIO_PREFIX)) {
				matchingRatio = lines.get(nextIndex).getText().trim().substring(MATCHING_RATIO_PREFIX.length()).trim();
				nextIndex++;
			}

			String nullAlleleResolutionStatus = null;
			if (nextIndex < lines.size()
					&& lines.get(nextIndex).getText().trim().startsWith(NULL_ALLELE_STATUS_PREFIX)) {
				StringBuilder statusText = new StringBuilder(
						lines.get(nextIndex).getText().trim().substring(NULL_ALLELE_STATUS_PREFIX.length()).trim());
				nextIndex++;
				// Wraps across however many lines it needs to -- confirmed against the
				// real fixture that every occurrence ends with the word "excluded",
				// which is used as the stop condition rather than a fixed line count.
				while (!statusText.toString().endsWith(NULL_ALLELE_STATUS_TERMINATOR) && nextIndex < lines.size()) {
					statusText.append(' ').append(lines.get(nextIndex).getText().trim());
					nextIndex++;
				}
				nullAlleleResolutionStatus = statusText.toString();
			}

			candidates.add(new HistogeneticsSampleResultCandidate(currentSampleId, locusValues, typingStatusText,
					matchingRatio, nullAlleleResolutionStatus, lines.get(i).getText().trim(), lines.get(i).getLineNumber()));

			i = nextIndex;
		}

		return candidates;
	}

	// Reads DOWN each locus's own column across both data rows (not across a row) --
	// the whole point of this table's shape. A value that isn't code-shaped (NA, or a
	// FAILED/PENDING/XXXX placeholder) is simply skipped, not recorded as an empty or
	// placeholder value.
	private List<HistogeneticsLocusValues> zipLocusValues(List<Locus> headerLoci, String[] row1, String[] row2) {
		List<HistogeneticsLocusValues> result = new ArrayList<>();

		for (int column = 0; column < headerLoci.size(); column++) {
			List<String> values = new ArrayList<>();
			if (HistogeneticsCode.isCodeShaped(row1[column])) {
				values.add(row1[column]);
			}
			if (HistogeneticsCode.isCodeShaped(row2[column])) {
				values.add(row2[column]);
			}
			if (!values.isEmpty()) {
				result.add(new HistogeneticsLocusValues(headerLoci.get(column), values));
			}
		}

		return result;
	}
}
