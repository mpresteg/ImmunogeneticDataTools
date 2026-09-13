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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.junit.jupiter.api.Test;
import org.nmdp.hlareport.candidate.CegatLocusResultLineDetector;
import org.nmdp.hlareport.candidate.LocusResultCandidate;
import org.nmdp.hlareport.extract.PdfTextExtractor;

/**
 * Exercised against the real (de-identified) CeGaT sample report end-to-end: extract
 * -> detect -> build -> hand the result to ld-validation's own real code, not a mock or
 * a hand-typed GL string -- per this repo's CONTRIBUTING.md ("Prefer verifying behavior
 * for real over trusting `mvn test` alone where it matters") and the module's "Real
 * reports drive the grammar, not assumption" principle.
 */
public class CegatGlStringBuilderTest {
	private final PdfTextExtractor extractor = new PdfTextExtractor();
	private final CegatLocusResultLineDetector detector = new CegatLocusResultLineDetector();
	private final CegatGlStringBuilder builder = new CegatGlStringBuilder();

	@Test
	public void testBuildsTheExpectedGlStringFromTheRealCegatReport() throws IOException, URISyntaxException {
		ConstructedGlString result = builder.build(detectCandidates());

		assertEquals("HLA-A*24:02+HLA-A*24:02^HLA-B*27:05+HLA-B*39:06^HLA-C*02:02+HLA-C*07:02"
				+ "^HLA-DPA1*01:03+HLA-DPA1*01:03^HLA-DPB1*02:01+HLA-DPB1*04:01"
				+ "^HLA-DQA1*03:01+HLA-DQA1*04:01^HLA-DQB1*03:02+HLA-DQB1*04:02"
				+ "^HLA-DRB1*04:04+HLA-DRB1*08:01^HLA-DRB4*01:03", result.getGlString());
	}

	@Test
	public void testSingleGenePresentDrb345ProducesASingleCopyFragmentNotAGuessedPair()
			throws IOException, URISyntaxException {
		// DRB345 is the one locus in this report with only one allele call (a
		// single-gene-present result -- see CegatLocusResultLineDetector). The point of
		// this test: its GL fragment is "HLA-DRB4*01:03" alone, with no "+" at all --
		// not "HLA-DRB4*01:03+HLA-DRB4*01:03", which would be guessing homozygosity
		// nothing in the report actually states.
		String glString = builder.build(detectCandidates()).getGlString();

		assertTrue(glString.endsWith("^HLA-DRB4*01:03"), "Expected a trailing single-copy DRB345 fragment: " + glString);
		assertTrue(!glString.contains("HLA-DRB4*01:03+"), "Expected no invented second copy: " + glString);
	}

	@Test
	public void testSourceCandidatesAreCarriedForTraceability() throws IOException, URISyntaxException {
		List<LocusResultCandidate> candidates = detectCandidates();

		ConstructedGlString result = builder.build(candidates);

		assertEquals(candidates, result.getSourceCandidates());
	}

	@Test
	public void testConstructedGlStringIsValidPerLdValidationsOwnRules() throws IOException, URISyntaxException {
		String glString = builder.build(detectCandidates()).getGlString();

		// Not this module's own opinion of what "valid" means -- ld-validation's own
		// existing validator, exactly per the module README's "no new GL String
		// grammar" principle.
		assertTrue(GLStringUtilities.validateGLStringFormat(glString),
				"Expected ld-validation's own validator to accept this GL string: " + glString);
	}

	@Test
	public void testConstructedGlStringIsActuallyConsumableByLdValidation() throws IOException, URISyntaxException {
		String glString = builder.build(detectCandidates()).getGlString();

		// The actual point of this module's existence: hand the constructed string to
		// real ld-validation code, not just assert on the string's own shape. Confirms
		// it doesn't throw and round-trips back out unchanged.
		LinkageDisequilibriumGenotypeList genotypeList = new LinkageDisequilibriumGenotypeList("cegat-test", glString);

		assertNotNull(genotypeList);
		assertEquals(glString, genotypeList.getGLString());
	}

	private List<LocusResultCandidate> detectCandidates() throws IOException, URISyntaxException {
		String resource = "sample-reports/cegat-hla-typing-sample.pdf";
		File file = new File(getClass().getClassLoader().getResource(resource).toURI());
		return detector.detect(extractor.extractText(file));
	}
}
