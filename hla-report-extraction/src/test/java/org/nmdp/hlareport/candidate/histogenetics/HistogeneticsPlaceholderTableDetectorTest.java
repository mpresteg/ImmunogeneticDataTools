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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.dash.valid.Locus;
import org.junit.jupiter.api.Test;
import org.nmdp.hlareport.extract.PdfTextExtractor;

/**
 * Exercised against the real (de-identified) Histogenetics sample report -- see
 * src/test/resources/sample-reports/PROVENANCE.md. Per the module README's "Real
 * reports drive the grammar, not assumption" principle.
 */
public class HistogeneticsPlaceholderTableDetectorTest {
	private static final List<Locus> ALL_NINE_LOCI = List.of(Locus.HLA_A, Locus.HLA_B, Locus.HLA_C, Locus.HLA_DRB1,
			Locus.HLA_DRB345, Locus.HLA_DQB1, Locus.HLA_DQA1, Locus.HLA_DPB1, Locus.HLA_DPA1);

	private final PdfTextExtractor extractor = new PdfTextExtractor();

	@Test
	public void testDetectsTheFailedReportAndOnlyIt() throws IOException, URISyntaxException {
		List<HistogeneticsPlaceholderResultCandidate> candidates = new HistogeneticsPlaceholderTableDetector("FAILED")
				.detect(extractText());

		assertEquals(1, candidates.size(), "Expected only the one FAILED report: " + candidates);
		HistogeneticsPlaceholderResultCandidate candidate = candidates.get(0);
		assertEquals("Patient ID 2", candidate.getSampleId());
		assertEquals("FAILED", candidate.getPlaceholder());
		assertEquals("Complete", candidate.getTypingStatusText(), "Captured for transparency even though it's misleading");
		// All 9 loci, including DRB345 -- whose two cells are the prefixed
		// "DRB3*FAILED"/"DRB4*FAILED" form, not the bare word, and still recognized.
		assertEquals(ALL_NINE_LOCI, candidate.getAffectedLoci());
	}

	@Test
	public void testDetectsThePendingReportAndOnlyIt() throws IOException, URISyntaxException {
		List<HistogeneticsPlaceholderResultCandidate> candidates = new HistogeneticsPlaceholderTableDetector("PENDING")
				.detect(extractText());

		assertEquals(1, candidates.size(), "Expected only the one PENDING report: " + candidates);
		HistogeneticsPlaceholderResultCandidate candidate = candidates.get(0);
		assertEquals("Patient ID 3", candidate.getSampleId());
		assertEquals("PENDING", candidate.getPlaceholder());
		assertEquals(
				"HLA-A, HLA-B, HLA-C, HLA-DRB1, HLA-DRB3, HLA-DRB4, HLA-DRB5, HLA-DQB1, HLA-DQA1, HLA-DPB1, HLA-DPA1 is pending",
				candidate.getTypingStatusText());
		// Unlike FAILED/XXXX, PENDING's DRB345 cells are the bare word, no
		// "DRB3*"/"DRB4*" prefix -- a real, observed inconsistency between the three
		// placeholder reports, not a bug in this detector.
		assertEquals(ALL_NINE_LOCI, candidate.getAffectedLoci());
	}

	@Test
	public void testDetectsTheXxxxReportAndOnlyIt() throws IOException, URISyntaxException {
		List<HistogeneticsPlaceholderResultCandidate> candidates = new HistogeneticsPlaceholderTableDetector("XXXX")
				.detect(extractText());

		assertEquals(1, candidates.size(), "Expected only the one XXXX report: " + candidates);
		HistogeneticsPlaceholderResultCandidate candidate = candidates.get(0);
		assertEquals("Patient ID 4", candidate.getSampleId());
		assertEquals("XXXX", candidate.getPlaceholder());
		assertEquals(ALL_NINE_LOCI, candidate.getAffectedLoci());
	}

	@Test
	public void testEachPlaceholderDetectorIgnoresTheOtherReports() throws IOException, URISyntaxException {
		String text = extractText();

		// The actual point of this test: a "FAILED" detector run against the whole
		// document (which also contains the real Complete result, PENDING, and XXXX)
		// finds only the FAILED block -- not 0 (it does work), not >1 (it doesn't
		// over-match the other placeholder words or the real G-codes).
		assertEquals(1, new HistogeneticsPlaceholderTableDetector("FAILED").detect(text).size());
		assertEquals(1, new HistogeneticsPlaceholderTableDetector("PENDING").detect(text).size());
		assertEquals(1, new HistogeneticsPlaceholderTableDetector("XXXX").detect(text).size());

		// A placeholder word that doesn't appear anywhere -- the general "0 candidates,
		// not an error" convention every other detector in this module already uses.
		assertTrue(new HistogeneticsPlaceholderTableDetector("CANCELLED").detect(text).isEmpty());
	}

	@Test
	public void testDoesNotMisfireAgainstCegatsDifferentlyShapedReport() throws IOException, URISyntaxException {
		List<HistogeneticsPlaceholderResultCandidate> candidates = new HistogeneticsPlaceholderTableDetector("FAILED")
				.detect(extractText("cegat-hla-typing-sample.pdf"));

		assertTrue(candidates.isEmpty(), "Expected no candidates in a CeGaT-format report, found: " + candidates);
	}

	@Test
	public void testDoesNotMisfireAgainstVersitisDifferentlyShapedReport() throws IOException, URISyntaxException {
		List<HistogeneticsPlaceholderResultCandidate> candidates = new HistogeneticsPlaceholderTableDetector("FAILED")
				.detect(extractText("versiti-hla-c-high-resolution-sample.pdf"));

		assertTrue(candidates.isEmpty(), "Expected no candidates in a Versiti-format report, found: " + candidates);
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
