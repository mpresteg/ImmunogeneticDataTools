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
import java.util.Optional;

import org.dash.valid.Locus;
import org.junit.jupiter.api.Test;
import org.nmdp.hlareport.extract.PdfTextExtractor;

/**
 * Exercised against the real (de-identified) Histogenetics sample report -- see
 * src/test/resources/sample-reports/PROVENANCE.md. Per the module README's "Real
 * reports drive the grammar, not assumption" principle.
 */
public class HistogeneticsAppendixAccumulatorTest {
	private final PdfTextExtractor extractor = new PdfTextExtractor();
	private final HistogeneticsAppendixAccumulator accumulator = new HistogeneticsAppendixAccumulator();

	@Test
	public void testAccumulatesAllThreeSamplesIndependently() throws IOException, URISyntaxException {
		List<HistogeneticsAppendixEntry> entries = accumulator.detect(extractText());

		// 3 samples (patient, donor 1, donor 2) x 10 rows each (A, A, B, B, C, C,
		// DQB1, DQB1, DRB1, DRB1 -- DRB345/DQA1/DPB1/DPA1 were NA on page 1, so no
		// appendix rows for them exist). Not deduplicated even though all 3 samples'
		// content is identical (10/10 matched donors) -- see issue #50's explicit note
		// not to assume repeats always match.
		assertEquals(30, entries.size(), "Expected 10 rows per sample across 3 samples: " + entries);

		assertEquals(10, entries.stream().filter(e -> "Patient ID".equals(e.getSampleId())).count());
		assertEquals(10, entries.stream().filter(e -> "Donor ID 1".equals(e.getSampleId())).count());
		assertEquals(10, entries.stream().filter(e -> "Donor ID 2".equals(e.getSampleId())).count());
	}

	@Test
	public void testFullyReassemblesAShortMultiLineIncludedAllelesList() throws IOException, URISyntaxException {
		// DRB1*13:01:01G's list spans 2 physical lines (header + 1 continuation) --
		// short enough to assert on in full, confirming exact reassembly (not just
		// "some content survived").
		HistogeneticsAppendixEntry entry = findEntry(extractText(), "Patient ID", Locus.HLA_DRB1, "13:01:01G");

		assertEquals("13:01:01", entry.getNmdpAlleleCode(),
				"This row's NMDP allele code is literally the G-code's digits with no letter suffix at all");
		assertEquals("2,3", entry.getSegmentsSequenced());
		assertEquals(
				"13:01:01:01/13:01:01:02/13:01:01:03/13:01:01:04/13:01:01:05/13:01:01:06/13:01:01:07/13:01:01:08/13:01:01:09",
				entry.getIncludedAlleles());
		assertTrue(entry.isGCode());
	}

	@Test
	public void testDistinguishesNmdpCodeFallbackFromARealGCode() throws IOException, URISyntaxException {
		// DQB1*02:DKCVG: no G-code was available, so the reported value (column 2) is
		// itself the NMDP code, duplicated in column 3 -- per the report's own note #4.
		// This is the actual point of this test: isGCode() must say false here, not
		// just default true because the value looks G-code-shaped at a glance.
		HistogeneticsAppendixEntry entry = findEntry(extractText(), "Patient ID", Locus.HLA_DQB1, "02:DKCVG");

		assertFalse(entry.isGCode());
		assertEquals("02:DKCVG", entry.getNmdpAlleleCode());
		assertEquals("2,3", entry.getSegmentsSequenced());
		assertEquals("02:02:01:01/02:02:01:02/02:02:01:03/02:02:01:04/02:02:01:05/02:02:01:06/02:02:06:02/02:02:09/"
				+ "02:97/02:110/02:131/02:156/02:165/02:175", entry.getIncludedAlleles());
	}

	@Test
	public void testLineNumberReflectsOriginalDocumentNotFilteredPosition() throws IOException, URISyntaxException {
		HistogeneticsAppendixEntry entry = findEntry(extractText(), "Patient ID", Locus.HLA_A, "02:01:01G");

		// Confirmed directly against the real extracted text (see PR discussion):
		// this row's header is original line 95.
		assertEquals(95, entry.getLineNumber());
		assertEquals("A 02:01:01G 02:DMFHE Exon 1,2,3,4 02:01:01:01/02:01:01:02L/02:01:01:03/02:01:01:04/02:01:01:05/",
				entry.getSourceLine());
	}

	@Test
	public void testLongListSpanningManyLinesAndAPageBreakReassemblesToExpectedLength() throws IOException, URISyntaxException {
		// A*02:01:01G's list is the longest in the report: ~39 physical lines,
		// including a page-break interruption (see issue #48). Asserting on the exact
		// full string here would be impractical, so this asserts on length plus the
		// exact tail -- enough to confirm the whole list, including the content right
		// after the page-break-rejoin, actually made it into the result.
		HistogeneticsAppendixEntry entry = findEntry(extractText(), "Patient ID", Locus.HLA_A, "02:01:01G");

		assertEquals(2392, entry.getIncludedAlleles().length());
		assertTrue(entry.getIncludedAlleles().endsWith("02:896N/02:916/02:917/02:928/02:930/02:942"),
				"Expected the list to end with its actual last alleles, not truncate at the page break");
	}

	@Test
	public void testDoesNotMisfireAgainstCegatsDifferentlyShapedReport() throws IOException, URISyntaxException {
		List<HistogeneticsAppendixEntry> entries = accumulator.detect(extractText("cegat-hla-typing-sample.pdf"));

		assertTrue(entries.isEmpty(), "Expected no appendix entries in a CeGaT-format report, found: " + entries);
	}

	@Test
	public void testDoesNotMisfireAgainstVersitisDifferentlyShapedReport() throws IOException, URISyntaxException {
		List<HistogeneticsAppendixEntry> entries = accumulator.detect(extractText("versiti-hla-c-high-resolution-sample.pdf"));

		assertTrue(entries.isEmpty(), "Expected no appendix entries in a Versiti-format report, found: " + entries);
	}

	private HistogeneticsAppendixEntry findEntry(String extractedText, String sampleId, Locus locus, String reportedValue) {
		List<HistogeneticsAppendixEntry> entries = accumulator.detect(extractedText);
		Optional<HistogeneticsAppendixEntry> entry = entries.stream()
				.filter(e -> sampleId.equals(e.getSampleId()) && e.getLocus() == locus && reportedValue.equals(e.getReportedValue()))
				.findFirst();
		assertTrue(entry.isPresent(), "Expected an entry for " + sampleId + "/" + locus + "/" + reportedValue + " in: " + entries);
		return entry.get();
	}

	private String extractText() throws IOException, URISyntaxException {
		return extractText("histogenetics-hla-typing-g-code-sample.pdf");
	}

	private String extractText(String sampleReportResourceName) throws IOException, URISyntaxException {
		String resource = "sample-reports/" + sampleReportResourceName;
		File file = new File(getClass().getClassLoader().getResource(resource).toURI());
		return extractor.extractText(file);
	}
}
