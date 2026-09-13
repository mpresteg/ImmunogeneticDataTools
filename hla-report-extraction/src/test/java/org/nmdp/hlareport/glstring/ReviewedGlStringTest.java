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
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.junit.jupiter.api.Test;
import org.nmdp.hlareport.candidate.CegatLocusResultLineDetector;
import org.nmdp.hlareport.candidate.LocusResultCandidate;
import org.nmdp.hlareport.extract.PdfTextExtractor;

/**
 * Exercised against the real (de-identified) CeGaT sample report end-to-end where
 * possible: extract -> detect -> build -> confirm -> hand to real ld-validation code.
 * Per this repo's CONTRIBUTING.md ("Prefer verifying behavior for real") and the
 * module's "Real reports drive the grammar, not assumption" principle. See issue #46.
 */
public class ReviewedGlStringTest {
	private final PdfTextExtractor extractor = new PdfTextExtractor();
	private final CegatLocusResultLineDetector detector = new CegatLocusResultLineDetector();
	private final CegatGlStringBuilder builder = new CegatGlStringBuilder();

	@Test
	public void testConfirmSucceedsAndCapturesReviewerAndTimestamp() throws IOException, URISyntaxException {
		ConstructedGlString constructed = buildRealCegatGlString();
		Instant before = Instant.now();

		ReviewedGlString reviewed = ReviewedGlString.confirm(constructed, "mpresteg");

		assertEquals(constructed.getGlString(), reviewed.getGlString());
		assertEquals(constructed.getSourceCandidates(), reviewed.getSourceCandidates());
		assertEquals("mpresteg", reviewed.getReviewedBy());
		assertNotNull(reviewed.getReviewedAt());
		assertTrue(!reviewed.getReviewedAt().isBefore(before) && Duration.between(before, reviewed.getReviewedAt())
				.compareTo(Duration.ofSeconds(5)) < 0, "Expected reviewedAt to be essentially \"now\": " + reviewed.getReviewedAt());
	}

	@Test
	public void testConfirmRequiresANonBlankReviewer() throws IOException, URISyntaxException {
		ConstructedGlString constructed = buildRealCegatGlString();

		assertThrows(IllegalArgumentException.class, () -> ReviewedGlString.confirm(constructed, null));
		assertThrows(IllegalArgumentException.class, () -> ReviewedGlString.confirm(constructed, ""));
		assertThrows(IllegalArgumentException.class, () -> ReviewedGlString.confirm(constructed, "   "));
	}

	@Test
	public void testConfirmRequiresANonNullConstructedGlString() {
		assertThrows(IllegalArgumentException.class, () -> ReviewedGlString.confirm(null, "mpresteg"));
	}

	@Test
	public void testConfirmRejectsAStructurallyInvalidGlStringEvenWithAReviewerSupplied() {
		// Bypasses a builder entirely (which would never produce this -- see
		// GlStringValidation) to prove the point: a reviewer claiming a string is
		// correct cannot override a syntactic well-formedness failure. Correctness of
		// content is the human judgment call this class trusts; well-formedness of
		// syntax is not a judgment call at all.
		ConstructedGlString malformed = new ConstructedGlString("not a valid GL string", List.of());

		GlStringConstructionException exception = assertThrows(GlStringConstructionException.class,
				() -> ReviewedGlString.confirm(malformed, "mpresteg"));
		assertTrue(exception.getMessage().contains("not a valid GL string"), exception.getMessage());
	}

	@Test
	public void testReviewedGlStringIsActuallyConsumableByLdValidation() throws IOException, URISyntaxException {
		// The actual point of this class existing: hand a REVIEWED string to real
		// ld-validation code via the method only a ReviewedGlString has.
		ReviewedGlString reviewed = ReviewedGlString.confirm(buildRealCegatGlString(), "mpresteg");

		LinkageDisequilibriumGenotypeList genotypeList = reviewed.toLinkageDisequilibriumGenotypeList("cegat-reviewed-test");

		assertNotNull(genotypeList);
		assertEquals(reviewed.getGlString(), genotypeList.getGLString());
	}

	private ConstructedGlString buildRealCegatGlString() throws IOException, URISyntaxException {
		String resource = "sample-reports/cegat-hla-typing-sample.pdf";
		File file = new File(getClass().getClassLoader().getResource(resource).toURI());
		List<LocusResultCandidate> candidates = detector.detect(extractor.extractText(file));
		return builder.build(candidates);
	}
}
