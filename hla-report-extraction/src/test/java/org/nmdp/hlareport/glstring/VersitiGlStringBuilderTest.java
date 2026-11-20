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
package org.nmdp.hlareport.glstring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.dash.valid.Locus;
import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.junit.jupiter.api.Test;
import org.nmdp.hlareport.candidate.FootnoteReferencedAlleleCall;
import org.nmdp.hlareport.candidate.VersitiLocusResultCandidate;
import org.nmdp.hlareport.candidate.VersitiLocusResultLineDetector;
import org.nmdp.hlareport.extract.PdfTextExtractor;

/**
 * Exercised against the real (de-identified) Versiti sample report end-to-end: extract
 * -> detect -> build -> hand the result to ld-validation's own real code. Per this
 * repo's CONTRIBUTING.md ("Prefer verifying behavior for real") and the module's "Real
 * reports drive the grammar, not assumption" principle.
 */
public class VersitiGlStringBuilderTest {
	private final PdfTextExtractor extractor = new PdfTextExtractor();
	private final VersitiLocusResultLineDetector detector = new VersitiLocusResultLineDetector();
	private final VersitiGlStringBuilder builder = new VersitiGlStringBuilder();

	@Test
	public void testBuildsTheExpectedGlStringFromTheRealVersitiReport() throws IOException, URISyntaxException {
		ConstructedGlString result = builder.build(detectCandidates());

		// C*01:01 (no footnote) + the R1-footnote-resolved ambiguity, fully expanded --
		// the whole point of chasing the footnote instead of reporting the bare
		// G-group code (see VersitiLocusResultLineDetector, issue #42).
		assertEquals("HLA-C*01:01+HLA-C*07:04/HLA-C*07:11", result.getGlString());
	}

	@Test
	public void testConstructedGlStringIsValidPerLdValidationsOwnRules() throws IOException, URISyntaxException {
		String glString = builder.build(detectCandidates()).getGlString();

		assertTrue(GLStringUtilities.validateGLStringFormat(glString),
				"Expected ld-validation's own validator to accept this GL string: " + glString);
	}

	@Test
	public void testConstructedGlStringIsActuallyConsumableByLdValidation() throws IOException, URISyntaxException {
		String glString = builder.build(detectCandidates()).getGlString();

		LinkageDisequilibriumGenotypeList genotypeList = new LinkageDisequilibriumGenotypeList("versiti-test", glString);

		assertNotNull(genotypeList);
		assertEquals(glString, genotypeList.getGLString());
	}

	@Test
	public void testUnresolvedFootnoteReferenceFailsLoudlyRatherThanProducingAnIncompleteGlString() {
		// Synthetic (no real sample has hit this yet -- same situation
		// VersitiLocusResultLineDetectorTest's own unresolved-footnote test is in).
		// The actual point: a marker with no matching definition must never be
		// silently treated as if it weren't there, which would produce a GL string
		// missing real ambiguity information without any sign anything was wrong.
		FootnoteReferencedAlleleCall unresolved = new FootnoteReferencedAlleleCall("C*07:04:01G", "R2", null, -1);
		VersitiLocusResultCandidate candidate = new VersitiLocusResultCandidate(Locus.HLA_C, "High",
				List.of(unresolved), "HLA-C High C*07:04:01G R2", 1);

		GlStringConstructionException exception = assertThrows(GlStringConstructionException.class,
				() -> builder.build(List.of(candidate)));
		assertTrue(exception.getMessage().contains("HLA-C"), "Expected the failure message to name the locus: "
				+ exception.getMessage());
	}

	private List<VersitiLocusResultCandidate> detectCandidates() throws IOException, URISyntaxException {
		String resource = "sample-reports/versiti-hla-c-high-resolution-sample.pdf";
		File file = new File(getClass().getClassLoader().getResource(resource).toURI());
		return detector.detect(extractor.extractText(file));
	}
}
