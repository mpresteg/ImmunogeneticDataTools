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
import java.util.regex.Pattern;

import org.dash.valid.Locus;
import org.nmdp.hlareport.candidate.LocusLookup;

/**
 * Accumulates Histogenetics' appendix rows -- see issue #50, the "stateful accumulator"
 * #44 originally called for. Named for what it does (not "...Detector", like the other
 * per-lab classes) because the interesting part isn't spotting a row, it's correctly
 * reassembling one row's "Included Alleles" list across however many physical lines it
 * spans, which a single-line-at-a-time detector can't do.
 *
 * A row header looks like (after {@link HistogeneticsNoiseFilter} has already run --
 * see that class's comment for why this depends on it):
 * <pre>
 * A 02:01:01G 02:DMFHE Exon 1,2,3,4 02:01:01:01/02:01:01:02L/02:01:01:03/...
 * </pre>
 * exactly 6 whitespace-tokens: locus, reported value, NMDP allele code, the literal
 * word "Exon", the sequenced-segment list, and the first chunk of the Included Alleles
 * list. Every following line that is itself exactly one more such chunk (no per-line
 * terminator -- just keeps ending mid-list with a trailing "/") gets appended, stopping
 * as soon as a line no longer looks like a continuation -- which, after filtering, means
 * either a new row header or a new {@code "Sample ID :"} marker, never boilerplate.
 *
 * The appendix repeats its whole table once per sample (patient, donor 1, donor 2, ...)
 * -- see {@link HistogeneticsAppendixEntry#getSampleId()}. Each sample's rows are kept
 * independently; nothing here assumes or checks that repeated samples' rows agree
 * with each other, per issue #50's own note not to deduplicate on that assumption.
 */
public class HistogeneticsAppendixAccumulator {
	private static final String SAMPLE_ID_PREFIX = "Sample ID :";

	// Covers both a real G-code ("02:01:01G") and an NMDP allele code ("02:DMFHE",
	// "13:01:01") -- both columns 2 and 3 of a row header use this same general shape,
	// digit-led colon-separated fields where any field may be digits, letters, or both.
	private static final Pattern CODE_TOKEN_PATTERN = Pattern.compile("^[0-9]+(:[0-9A-Za-z]+)*$");

	private static final String EXON_LITERAL = "Exon";
	private static final Pattern SEGMENT_LIST_PATTERN = Pattern.compile("^[0-9]+(,[0-9]+)*$");

	// One chunk of the Included Alleles list -- either the header row's trailing token,
	// or a whole continuation line on its own. Digits, letters (allele suffixes like N/
	// Q/L), colons, and slashes only.
	private static final Pattern ALLELE_CHUNK_PATTERN = Pattern.compile("^[0-9A-Za-z:/]+$");

	private final HistogeneticsNoiseFilter noiseFilter = new HistogeneticsNoiseFilter();

	public List<HistogeneticsAppendixEntry> detect(String extractedText) {
		List<NumberedLine> lines = noiseFilter.filterNoise(extractedText);
		List<HistogeneticsAppendixEntry> entries = new ArrayList<>();

		String currentSampleId = null;
		int i = 0;
		while (i < lines.size()) {
			String trimmedLine = lines.get(i).getText().trim();

			if (trimmedLine.startsWith(SAMPLE_ID_PREFIX)) {
				currentSampleId = trimmedLine.substring(SAMPLE_ID_PREFIX.length()).trim();
				i++;
				continue;
			}

			RowHeader header = parseRowHeader(trimmedLine);
			if (header == null) {
				i++;
				continue;
			}

			StringBuilder includedAlleles = new StringBuilder(header.firstAlleleChunk);
			int consumed = 1;
			while (i + consumed < lines.size() && isAlleleContinuationChunk(lines.get(i + consumed).getText().trim())) {
				includedAlleles.append(lines.get(i + consumed).getText().trim());
				consumed++;
			}

			entries.add(new HistogeneticsAppendixEntry(currentSampleId, header.locus, header.reportedValue,
					header.nmdpAlleleCode, header.segmentsSequenced, includedAlleles.toString(), lines.get(i).getText().trim(),
					lines.get(i).getLineNumber()));

			i += consumed;
		}

		return entries;
	}

	private RowHeader parseRowHeader(String trimmedLine) {
		String[] tokens = trimmedLine.split("\\s+");
		if (tokens.length != 6) {
			return null;
		}

		Locus locus = LocusLookup.byShortName(tokens[0]);
		if (locus == null || !CODE_TOKEN_PATTERN.matcher(tokens[1]).matches()
				|| !CODE_TOKEN_PATTERN.matcher(tokens[2]).matches() || !EXON_LITERAL.equals(tokens[3])
				|| !SEGMENT_LIST_PATTERN.matcher(tokens[4]).matches() || !ALLELE_CHUNK_PATTERN.matcher(tokens[5]).matches()) {
			return null;
		}

		return new RowHeader(locus, tokens[1], tokens[2], tokens[4], tokens[5]);
	}

	// A continuation line is always exactly one chunk (no internal whitespace) -- a new
	// row header always has 6 whitespace-separated tokens, and a "Sample ID :" marker
	// always has several, so neither is ever mistaken for a continuation.
	private boolean isAlleleContinuationChunk(String trimmedLine) {
		if (trimmedLine.isEmpty()) {
			return false;
		}
		String[] tokens = trimmedLine.split("\\s+");
		return tokens.length == 1 && ALLELE_CHUNK_PATTERN.matcher(tokens[0]).matches();
	}

	private static final class RowHeader {
		private final Locus locus;
		private final String reportedValue;
		private final String nmdpAlleleCode;
		private final String segmentsSequenced;
		private final String firstAlleleChunk;

		private RowHeader(Locus locus, String reportedValue, String nmdpAlleleCode, String segmentsSequenced,
				String firstAlleleChunk) {
			this.locus = locus;
			this.reportedValue = reportedValue;
			this.nmdpAlleleCode = nmdpAlleleCode;
			this.segmentsSequenced = segmentsSequenced;
			this.firstAlleleChunk = firstAlleleChunk;
		}
	}
}
