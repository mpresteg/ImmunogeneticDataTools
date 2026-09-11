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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.dash.valid.Locus;
import org.junit.jupiter.api.Test;
import org.nmdp.hlareport.extract.PdfTextExtractor;

/**
 * Exercised against real (de-identified) sample reports -- see
 * src/test/resources/sample-reports/PROVENANCE.md. Per the module README's "Real
 * reports drive the grammar, not assumption" principle.
 */
public class VersitiLocusResultLineDetectorTest {
	private final PdfTextExtractor extractor = new PdfTextExtractor();
	private final VersitiLocusResultLineDetector detector = new VersitiLocusResultLineDetector();

	@Test
	public void testDetectsTheLocusResultAndFollowsItsFootnoteToResolution() throws IOException, URISyntaxException {
		List<VersitiLocusResultCandidate> candidates = detector.detect(extractText("versiti-hla-c-high-resolution-sample.pdf"));

		assertEquals(1, candidates.size(), "Expected exactly the one HLA-C result this report contains: " + candidates);

		VersitiLocusResultCandidate candidate = candidates.get(0);
		assertEquals(Locus.HLA_C, candidate.getLocus());
		assertEquals("High", candidate.getResolutionDescriptor());
		assertEquals(24, candidate.getLineNumber());
		assertEquals("HLA-C High C*01:01", candidate.getSourceLine());

		List<FootnoteReferencedAlleleCall> alleleCalls = candidate.getAlleleCalls();
		assertEquals(2, alleleCalls.size());

		// First allele call: no ambiguity, no footnote reference at all.
		FootnoteReferencedAlleleCall firstAlleleCall = alleleCalls.get(0);
		assertEquals("C*01:01", firstAlleleCall.getAlleleCall());
		assertNull(firstAlleleCall.getFootnoteMarker());
		assertNull(firstAlleleCall.getResolvedAlleles());
		assertFalse(firstAlleleCall.hasUnresolvedFootnoteReference());

		// Second allele call: this is the whole point of issue #42 -- the G-group code
		// alone ("C*07:04:01G") is NOT the end of the story. The R1 marker must be
		// followed to its actual resolution text, not silently dropped.
		FootnoteReferencedAlleleCall secondAlleleCall = alleleCalls.get(1);
		assertEquals("C*07:04:01G", secondAlleleCall.getAlleleCall());
		assertEquals("R1", secondAlleleCall.getFootnoteMarker());
		assertEquals("C*07:04:01G = HLA-C*07:04:01G=C*07:04/11", secondAlleleCall.getResolvedAlleles());
		assertEquals(27, secondAlleleCall.getFootnoteLineNumber());
		assertFalse(secondAlleleCall.hasUnresolvedFootnoteReference());

		assertFalse(candidate.hasUnresolvedFootnoteReference());
	}

	@Test
	public void testUnresolvedFootnoteReferenceIsSurfacedNotSilentlyDropped() {
		// A marker referenced on the allele-call line with no matching footnote
		// definition anywhere in the document -- the module's "surface for review,
		// never silently drop" principle applies here just as much as to a resolved
		// one. Deliberately synthetic (not from a real report) since this specific
		// failure mode -- a marker with no definition at all -- hasn't been seen in a
		// real sample yet; it's a defensive case this detector should still handle
		// sanely rather than crash or misreport on.
		String syntheticExtractedText = "HLA-C High C*01:01\nC*07:04:01G R2\n";

		List<VersitiLocusResultCandidate> candidates = detector.detect(syntheticExtractedText);

		assertEquals(1, candidates.size());
		FootnoteReferencedAlleleCall secondAlleleCall = candidates.get(0).getAlleleCalls().get(1);
		assertEquals("R2", secondAlleleCall.getFootnoteMarker());
		assertNull(secondAlleleCall.getResolvedAlleles());
		assertTrue(secondAlleleCall.hasUnresolvedFootnoteReference());
		assertTrue(candidates.get(0).hasUnresolvedFootnoteReference());
	}

	@Test
	public void testDoesNotMisfireAgainstCegatsDifferentlyShapedReport() throws IOException, URISyntaxException {
		List<VersitiLocusResultCandidate> candidates = detector.detect(extractText("cegat-hla-typing-sample.pdf"));

		assertTrue(candidates.isEmpty(),
				"Versiti-specific detector should find no candidates in a CeGaT-format report, found: " + candidates);
	}

	@Test
	public void testDoesNotMisfireAgainstHistogeneticsDifferentlyShapedReport() throws IOException, URISyntaxException {
		List<VersitiLocusResultCandidate> candidates = detector.detect(extractText("histogenetics-hla-typing-g-code-sample.pdf"));

		assertTrue(candidates.isEmpty(),
				"Versiti-specific detector should find no candidates in a Histogenetics-format report, found: " + candidates);
	}

	private String extractText(String sampleReportResourceName) throws IOException, URISyntaxException {
		String resource = "sample-reports/" + sampleReportResourceName;
		File file = new File(getClass().getClassLoader().getResource(resource).toURI());
		return extractor.extractText(file);
	}
}
