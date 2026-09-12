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

/**
 * Strips Histogenetics' repeated per-page boilerplate (watermark fragments, letterhead,
 * footer, disclaimers, repeated report title, repeated appendix column headers) from
 * extracted report text, preserving every real content line in original relative
 * order. See issue #48 (a sub-issue of #44).
 *
 * This exists because the boilerplate isn't just cosmetic clutter sitting between
 * unrelated content -- it can interrupt a single logical field mid-stream. Confirmed
 * against the real fixture: an appendix G-code's "Included Alleles" list can be split
 * across a page break, with ~17 lines of this boilerplate landing between two halves of
 * the *same* list. A downstream multi-line accumulator (issue #50) only sees those two
 * halves as adjacent, and therefore only works correctly, once this filter has already
 * run -- otherwise the accumulator itself would need to understand every boilerplate
 * category just to know it isn't real content, conflating "is this still the same
 * field" with "is this noise" in one piece of logic.
 *
 * Scoped to Histogenetics specifically (its own letterhead/title text) -- not a general
 * PDF-boilerplate remover, same as {@code CegatLocusResultLineDetector} and
 * {@code VersitiLocusResultLineDetector} are each scoped to their own lab.
 *
 * Deliberately does NOT filter {@code "Sample ID : ..."} lines: those mark which sample
 * (patient/donor N) the following appendix rows belong to, and are the only signal
 * telling later detection logic where one sample's block ends and the next begins.
 */
public final class HistogeneticsNoiseFilter {
	// Per issue #48's own finding: the watermark's letter-grouping isn't perfectly
	// consistent across every occurrence in the real fixture (one instance splits
	// differently than the rest), so this matches by shape -- a short, all-caps,
	// letters-only line -- rather than memorizing the literal fragment list. Confirmed
	// against the real fixture that nothing else (locus values, NA, FAILED, PENDING,
	// XXXX) ever appears alone on its own line in this shape; those only ever appear as
	// part of a longer whitespace-separated row.
	private static final Pattern WATERMARK_FRAGMENT_PATTERN = Pattern.compile("^[A-Z]{1,4}$");

	// The irregular double space before the total ("Page 4 of  14") is real, not a typo
	// in this class -- confirmed present in the actual extracted text.
	private static final Pattern PAGE_FOOTER_PATTERN = Pattern.compile("^Page [0-9]+ of\\s+[0-9]+$");

	private static final String CONFIDENTIAL_NOTICE_PREFIX = "CONFIDENTIAL:";
	private static final String DISCLAIMER_PREFIX = "DISCLAIMER:";

	// The letterhead block that repeats at the top of every page. Matched by prefix,
	// not full-line equality, since none of these actually vary in the one fixture seen
	// so far, but a prefix match is the less brittle choice if a future report has (for
	// example) a different phone/fax number on a later page.
	private static final String[] LETTERHEAD_LINE_PREFIXES = { "300 Executive Blvd", "Phone : 914-762-0300",
			"ASHI # 03-1-NY-26-2", "Soo Young Yang Ph.D", "Email : customerservice@histogenetics.com" };

	private static final String REPORT_TITLE_LINE = "HLA TYPING REPORT";
	private static final String APPENDIX_REPORT_TITLE_LINE = "HLA TYPING REPORT - APPENDIX";

	// The appendix's column headers wrap across 3 lines and repeat at the top of every
	// appendix page.
	private static final String APPENDIX_COLUMN_HEADER_LINE_1 = "Locus G Code NMDP Allele Code Segment";
	private static final String APPENDIX_COLUMN_HEADER_LINE_2 = "Sequenced";
	private static final String APPENDIX_COLUMN_HEADER_LINE_3 = "Included Alleles";

	/**
	 * @param extractedText the report's full extracted text (e.g. from
	 *                       {@link org.nmdp.hlareport.extract.PdfTextExtractor})
	 * @return every line NOT identified as boilerplate, in original relative order
	 */
	public List<String> filterNoise(String extractedText) {
		String[] lines = extractedText.split("\\r?\\n");
		List<String> cleaned = new ArrayList<>();

		int i = 0;
		while (i < lines.length) {
			String trimmedLine = lines[i].trim();

			if (trimmedLine.startsWith(CONFIDENTIAL_NOTICE_PREFIX)) {
				// Always exactly 2 lines in every occurrence seen (itself + its wrapped
				// continuation, which carries no distinguishing prefix of its own) --
				// see the class comment. Bounded so a CONFIDENTIAL: line at the very
				// end of the document can't skip past the array.
				i += (i + 1 < lines.length) ? 2 : 1;
				continue;
			}

			if (isNoiseLine(trimmedLine)) {
				i++;
				continue;
			}

			cleaned.add(lines[i]);
			i++;
		}

		return cleaned;
	}

	private boolean isNoiseLine(String trimmedLine) {
		return WATERMARK_FRAGMENT_PATTERN.matcher(trimmedLine).matches()
				|| PAGE_FOOTER_PATTERN.matcher(trimmedLine).matches()
				|| trimmedLine.startsWith(DISCLAIMER_PREFIX)
				|| isLetterheadLine(trimmedLine)
				|| trimmedLine.equals(REPORT_TITLE_LINE)
				|| trimmedLine.equals(APPENDIX_REPORT_TITLE_LINE)
				|| trimmedLine.equals(APPENDIX_COLUMN_HEADER_LINE_1)
				|| trimmedLine.equals(APPENDIX_COLUMN_HEADER_LINE_2)
				|| trimmedLine.equals(APPENDIX_COLUMN_HEADER_LINE_3);
	}

	private boolean isLetterheadLine(String trimmedLine) {
		for (String prefix : LETTERHEAD_LINE_PREFIXES) {
			if (trimmedLine.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}
}
