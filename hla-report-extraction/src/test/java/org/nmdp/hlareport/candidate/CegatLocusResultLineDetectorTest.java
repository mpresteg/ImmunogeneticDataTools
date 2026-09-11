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
package org.nmdp.hlareport.candidate;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Exercised against real (de-identified) sample reports -- see
 * src/test/resources/sample-reports/PROVENANCE.md. Per the module README's "Real
 * reports drive the grammar, not assumption" principle: the positive case asserts
 * against CeGaT's actual report content, and the negative cases confirm this
 * CeGaT-specific detector doesn't misfire against the other two labs' differently-shaped
 * reports -- important given this detector is shape-driven rather than section-scoped
 * (see CegatLocusResultLineDetector's class comment).
 */
public class CegatLocusResultLineDetectorTest {
	private final PdfTextExtractor extractor = new PdfTextExtractor();
	private final CegatLocusResultLineDetector detector = new CegatLocusResultLineDetector();

	@Test
	public void testDetectsAllNineLociFromTheRealCegatReport() throws IOException, URISyntaxException {
		List<LocusResultCandidate> candidates = detector.detect(extractText("cegat-hla-typing-sample.pdf"));

		assertEquals(9, candidates.size(), "Expected exactly the 9 loci CeGaT's Results table reports: " + candidates);

		assertAlleleCalls(candidates, Locus.HLA_A, "A*24:02", "A*24:02");
		assertAlleleCalls(candidates, Locus.HLA_B, "B*27:05", "B*39:06");
		assertAlleleCalls(candidates, Locus.HLA_C, "C*02:02", "C*07:02");
		assertAlleleCalls(candidates, Locus.HLA_DPA1, "DPA1*01:03", "DPA1*01:03");
		assertAlleleCalls(candidates, Locus.HLA_DPB1, "DPB1*02:01", "DPB1*04:01");
		assertAlleleCalls(candidates, Locus.HLA_DQA1, "DQA1*03:01", "DQA1*04:01");
		assertAlleleCalls(candidates, Locus.HLA_DQB1, "DQB1*03:02", "DQB1*04:02");
		assertAlleleCalls(candidates, Locus.HLA_DRB1, "DRB1*04:04", "DRB1*08:01");
		// Single-gene-present DRB345 result -- one allele call, not two, and not
		// treated as missing/malformed (the actual point of issue #43).
		assertAlleleCalls(candidates, Locus.HLA_DRB345, "DRB4*01:03");
	}

	@Test
	public void testDoesNotMisfireAgainstVersitisDifferentlyShapedReport() throws IOException, URISyntaxException {
		List<LocusResultCandidate> candidates = detector.detect(extractText("versiti-hla-c-high-resolution-sample.pdf"));

		assertTrue(candidates.isEmpty(),
				"CeGaT-specific detector should find no candidates in a Versiti-format report, found: " + candidates);
	}

	@Test
	public void testDoesNotMisfireAgainstHistogeneticsDifferentlyShapedReport() throws IOException, URISyntaxException {
		List<LocusResultCandidate> candidates = detector.detect(extractText("histogenetics-hla-typing-g-code-sample.pdf"));

		assertTrue(candidates.isEmpty(),
				"CeGaT-specific detector should find no candidates in a Histogenetics-format report, found: " + candidates);
	}

	private void assertAlleleCalls(List<LocusResultCandidate> candidates, Locus locus, String... expectedAlleleCalls) {
		Optional<LocusResultCandidate> candidate = candidates.stream().filter(c -> c.getLocus() == locus).findFirst();
		assertTrue(candidate.isPresent(), "Expected a candidate for " + locus + " in: " + candidates);
		assertEquals(List.of(expectedAlleleCalls), candidate.get().getAlleleCalls());
	}

	private String extractText(String sampleReportResourceName) throws IOException, URISyntaxException {
		String resource = "sample-reports/" + sampleReportResourceName;
		File file = new File(getClass().getClassLoader().getResource(resource).toURI());
		return extractor.extractText(file);
	}
}
