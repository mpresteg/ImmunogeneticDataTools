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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.nmdp.hlareport.extract.PdfTextExtractor;

/**
 * Exercised against the real (de-identified) Histogenetics sample report -- see
 * src/test/resources/sample-reports/PROVENANCE.md. Per the module README's "Real
 * reports drive the grammar, not assumption" principle.
 */
public class HistogeneticsNoiseFilterTest {
	private static final Pattern ANY_KNOWN_NOISE_LINE = Pattern.compile(
			"^([A-Z]{1,4}|Page [0-9]+ of\\s+[0-9]+|CONFIDENTIAL:.*|DISCLAIMER:.*|300 Executive Blvd.*"
					+ "|Phone : 914-762-0300.*|ASHI # 03-1-NY-26-2.*|Soo Young Yang Ph\\.D.*"
					+ "|Email : customerservice@histogenetics\\.com.*|HLA TYPING REPORT( - APPENDIX)?"
					+ "|Locus G Code NMDP Allele Code Segment|Sequenced|Included Alleles)$");

	private final PdfTextExtractor extractor = new PdfTextExtractor();
	private final HistogeneticsNoiseFilter filter = new HistogeneticsNoiseFilter();

	@Test
	public void testNoKnownNoiseLineSurvivesFiltering() throws IOException, URISyntaxException {
		List<NumberedLine> cleaned = filter.filterNoise(extractText());

		for (NumberedLine line : cleaned) {
			assertFalse(ANY_KNOWN_NOISE_LINE.matcher(line.getText().trim()).matches(),
					"Expected this line to have been filtered as noise: \"" + line + "\"");
		}
	}

	@Test
	public void testRealContentSurvivesAcrossAllFourBundledReports() throws IOException, URISyntaxException {
		List<NumberedLine> cleaned = filter.filterNoise(extractText());
		String joined = cleaned.stream().map(NumberedLine::getText).collect(Collectors.joining("\n"));

		// Sample ID lines are NOT noise -- they're the only signal marking which
		// sample's appendix block follows (see the class comment).
		assertTrue(joined.contains("Sample ID : Patient ID"), "Expected Sample ID markers to survive filtering");

		// Report 1 ("Complete"): page-1 table content and its footnotes
		assertTrue(joined.contains("Matching Ratio with Patient"));
		assertTrue(joined.contains("Null Allele Resolution Status"));
		assertTrue(joined.contains("A*02:43N"));

		// Report 1's appendix content, including the NMDP allele code column
		assertTrue(joined.contains("02:DMFHE"));

		// Report 2 (FAILED), report 3 (PENDING), report 4 (XXXX + narrative ambiguity)
		// -- the filter shouldn't touch these just because they're not the "Complete"
		// case; that's a detection-scope decision for #51, not something this filter
		// should be deciding by silently dropping lines.
		assertTrue(joined.contains("FAILED"), "Expected the FAILED report's content to survive filtering");
		assertTrue(joined.contains("PENDING"), "Expected the PENDING report's content to survive filtering");
		assertTrue(joined.contains("Possible Allele in A : 01:01:01/02:01:01/30:01:01."),
				"Expected the narrative-ambiguity report's content to survive filtering");
	}

	@Test
	public void testFilteringRejoinsAnIncludedAllelesListSplitAcrossAPageBreak() throws IOException, URISyntaxException {
		// This is the actual point of issue #48: confirm the filter turns the two
		// halves of one interrupted field back into adjacent lines. Before filtering,
		// ~17 lines of boilerplate (footer, disclaimer, page number, watermark,
		// letterhead, title, repeated column headers) sit between these two halves in
		// the real extracted text (original lines 139 and 160).
		List<NumberedLine> cleaned = filter.filterNoise(extractText());

		int firstHalfIndex = -1;
		for (int i = 0; i < cleaned.size(); i++) {
			if ("13:02:01:11/13:02:01:12/13:02:01:13/13:02:01:14/13:02:01:15/".equals(cleaned.get(i).getText())) {
				firstHalfIndex = i;
				break;
			}
		}
		assertTrue(firstHalfIndex >= 0, "Expected to find the first half of the split Included Alleles list");

		NumberedLine firstHalf = cleaned.get(firstHalfIndex);
		NumberedLine secondHalf = cleaned.get(firstHalfIndex + 1);
		assertEquals(139, firstHalf.getLineNumber(), "Expected original line numbers to be preserved through filtering");
		assertEquals("13:02:01:16/13:02:20/13:02:28/13:114/13:116N/13:117/13:123Q/13:125/", secondHalf.getText(),
				"Expected the second half to immediately follow the first after filtering -- the whole point of this filter");
		assertEquals(160, secondHalf.getLineNumber(),
				"Expected the second half's ORIGINAL line number to reflect the real gap (not e.g. 140), even though"
						+ " it's now positionally adjacent to the first half after filtering");
	}

	private String extractText() throws IOException, URISyntaxException {
		String resource = "sample-reports/histogenetics-hla-typing-g-code-sample.pdf";
		File file = new File(getClass().getClassLoader().getResource(resource).toURI());
		return extractor.extractText(file);
	}
}
