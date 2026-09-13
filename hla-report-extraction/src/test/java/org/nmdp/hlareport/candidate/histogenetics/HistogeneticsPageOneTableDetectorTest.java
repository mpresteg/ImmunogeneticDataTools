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
import static org.junit.jupiter.api.Assertions.assertNull;
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
public class HistogeneticsPageOneTableDetectorTest {
	private final PdfTextExtractor extractor = new PdfTextExtractor();
	private final HistogeneticsPageOneTableDetector detector = new HistogeneticsPageOneTableDetector();

	@Test
	public void testDetectsAllThreeCompleteSamplesAndOnlyThose() throws IOException, URISyntaxException {
		List<HistogeneticsSampleResultCandidate> candidates = detector.detect(extractText());

		// The fixture bundles 4 reports: the "Complete" one (patient + 2 donors = 3
		// blocks) plus FAILED, PENDING, and XXXX/narrative-ambiguity ones. Expecting
		// exactly 3 -- not 6 -- is the actual point of this test: it confirms those
		// other 3 reports produced zero candidates each, not that they were never
		// examined.
		assertEquals(3, candidates.size(), "Expected exactly the 3 blocks from the Complete report: " + candidates);
		assertEquals("Patient ID", candidates.get(0).getSampleId());
		assertEquals("Donor ID 1", candidates.get(1).getSampleId());
		assertEquals("Donor ID 2", candidates.get(2).getSampleId());
	}

	@Test
	public void testPatientBlockHasNoMatchingRatioButDonorBlocksDo() throws IOException, URISyntaxException {
		List<HistogeneticsSampleResultCandidate> candidates = detector.detect(extractText());

		HistogeneticsSampleResultCandidate patient = findBySampleId(candidates, "Patient ID");
		HistogeneticsSampleResultCandidate donor1 = findBySampleId(candidates, "Donor ID 1");
		HistogeneticsSampleResultCandidate donor2 = findBySampleId(candidates, "Donor ID 2");

		assertNull(patient.getMatchingRatio(), "The patient isn't matched against anyone -- no ratio field at all");
		assertEquals("10/10 Matched", donor1.getMatchingRatio());
		assertEquals("10/10 Matched", donor2.getMatchingRatio());
	}

	@Test
	public void testNullAlleleResolutionStatusIsReassembledAcrossItsLineWrap() throws IOException, URISyntaxException {
		HistogeneticsSampleResultCandidate patient = findBySampleId(detector.detect(extractText()), "Patient ID");

		// Wraps across 2 physical lines in the real report, with no distinguishing
		// prefix on the continuation -- reassembled using "ends with 'excluded'" as
		// the stop condition, not a fixed line count.
		assertEquals("A*02:43N, A*02:83N, A*02:305N, A*02:356N, A*02:608N, A*02:675N, A*02:691N, A*30:130N, "
				+ "A*30:132N, B*13:139N, B*15:01:01:02N, B*15:483N, C*03:380N, C*03:432N, C*06:46N, "
				+ "C*06:211:01:01N, C*06:211:01:02N, DQB1*02:96N, DQB1*02:163N, DQB1*02:176N excluded",
				patient.getNullAlleleResolutionStatus());
	}

	@Test
	public void testTypingStatusIsCapturedButNotUsedToDecideValidity() throws IOException, URISyntaxException {
		// This test documents the actual finding behind this detector's design: the
		// report's own "Typing Status" field literally reads "Complete" here too --
		// it's captured for transparency, but detection relies on the locus values
		// themselves being code-shaped, not on this field. See
		// testDetectsAllThreeCompleteSamplesAndOnlyThose and this class's own comment.
		HistogeneticsSampleResultCandidate patient = findBySampleId(detector.detect(extractText()), "Patient ID");

		assertEquals("Complete", patient.getTypingStatusText());
	}

	@Test
	public void testLocusValuesReadDownColumnsSkippingNaAndCapturingBothAllelesWhereHeterozygous()
			throws IOException, URISyntaxException {
		HistogeneticsSampleResultCandidate patient = findBySampleId(detector.detect(extractText()), "Patient ID");

		// Exactly 5 loci have real values (A, B, C, DRB1, DQB1) -- DRB345, DQA1, DPB1,
		// DPA1 were "NA" in both data rows and are correctly absent altogether, not
		// present with an empty value list.
		assertEquals(5, patient.getLocusValues().size(), "Expected only loci with real values: " + patient.getLocusValues());

		assertLocusValues(patient, Locus.HLA_A, "02:01:01G", "30:01:01G");
		assertLocusValues(patient, Locus.HLA_B, "13:02:01G", "15:01:01G");
		assertLocusValues(patient, Locus.HLA_C, "03:03:01G", "06:02:01G");
		assertLocusValues(patient, Locus.HLA_DRB1, "07:01:01G", "13:01:01G");
		// The actual point of this assertion: DQB1's first value ("02:DKCVG") is the
		// NMDP-code fallback case (issue #49's own note), not a real G-code, and it
		// still gets captured correctly as a value.
		assertLocusValues(patient, Locus.HLA_DQB1, "02:DKCVG", "06:03:01G");

		Locus[] absentLoci = { Locus.HLA_DRB345, Locus.HLA_DQA1, Locus.HLA_DPB1, Locus.HLA_DPA1 };
		for (Locus locus : absentLoci) {
			assertTrue(patient.getLocusValues().stream().noneMatch(v -> v.getLocus() == locus),
					"Expected " + locus + " (reported as NA) to be absent, not present with an empty value list");
		}
	}

	@Test
	public void testDoesNotMisfireAgainstCegatsDifferentlyShapedReport() throws IOException, URISyntaxException {
		List<HistogeneticsSampleResultCandidate> candidates = detector.detect(extractText("cegat-hla-typing-sample.pdf"));

		assertTrue(candidates.isEmpty(), "Expected no page-1 table candidates in a CeGaT-format report, found: " + candidates);
	}

	@Test
	public void testDoesNotMisfireAgainstVersitisDifferentlyShapedReport() throws IOException, URISyntaxException {
		List<HistogeneticsSampleResultCandidate> candidates = detector
				.detect(extractText("versiti-hla-c-high-resolution-sample.pdf"));

		assertTrue(candidates.isEmpty(),
				"Expected no page-1 table candidates in a Versiti-format report, found: " + candidates);
	}

	private void assertLocusValues(HistogeneticsSampleResultCandidate candidate, Locus locus, String... expectedValues) {
		Optional<HistogeneticsLocusValues> locusValues = candidate.getLocusValues().stream()
				.filter(v -> v.getLocus() == locus).findFirst();
		assertTrue(locusValues.isPresent(), "Expected values for " + locus + " in: " + candidate.getLocusValues());
		assertEquals(List.of(expectedValues), locusValues.get().getValues());
	}

	private HistogeneticsSampleResultCandidate findBySampleId(List<HistogeneticsSampleResultCandidate> candidates,
			String sampleId) {
		return candidates.stream().filter(c -> sampleId.equals(c.getSampleId())).findFirst()
				.orElseThrow(() -> new AssertionError("Expected a candidate for sampleId " + sampleId + " in: " + candidates));
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
