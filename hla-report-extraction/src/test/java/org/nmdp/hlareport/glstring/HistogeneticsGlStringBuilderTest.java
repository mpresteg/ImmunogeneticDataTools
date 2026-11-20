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

import org.dash.valid.gl.AmbiguousGenotypeException;
import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.junit.jupiter.api.Test;
import org.nmdp.hlareport.candidate.histogenetics.HistogeneticsAppendixAccumulator;
import org.nmdp.hlareport.candidate.histogenetics.HistogeneticsAppendixEntry;
import org.nmdp.hlareport.candidate.histogenetics.HistogeneticsLocusValues;
import org.nmdp.hlareport.candidate.histogenetics.HistogeneticsPageOneTableDetector;
import org.nmdp.hlareport.candidate.histogenetics.HistogeneticsSampleResultCandidate;
import org.nmdp.hlareport.extract.PdfTextExtractor;

/**
 * Exercised against the real (de-identified) Histogenetics sample report end-to-end:
 * extract -> detect (both the page-1 table and the appendix) -> build -> hand the
 * result to ld-validation's own real code. Per this repo's CONTRIBUTING.md ("Prefer
 * verifying behavior for real") and the module's "Real reports drive the grammar, not
 * assumption" principle.
 */
public class HistogeneticsGlStringBuilderTest {
	private final PdfTextExtractor extractor = new PdfTextExtractor();
	private final HistogeneticsPageOneTableDetector pageOneDetector = new HistogeneticsPageOneTableDetector();
	private final HistogeneticsAppendixAccumulator appendixAccumulator = new HistogeneticsAppendixAccumulator();
	private final HistogeneticsGlStringBuilder builder = new HistogeneticsGlStringBuilder();

	@Test
	public void testBuildsACorrectlyStructuredGlStringForEachOfTheThreeSamples() throws IOException, URISyntaxException {
		List<HistogeneticsSampleResultCandidate> samples = detectSamples();
		List<HistogeneticsAppendixEntry> appendixEntries = detectAppendixEntries();

		for (HistogeneticsSampleResultCandidate sample : samples) {
			String glString = builder.build(sample, appendixEntries).getGlString();

			// 5 loci (A, B, C, DRB1, DQB1) joined by "^", each with 2 allele copies
			// joined by "+" -- confirms the overall shape without hardcoding the
			// (very long) full expansion.
			assertEquals(5, glString.split("\\^").length, "Expected 5 loci: " + glString);
			assertTrue(glString.startsWith("HLA-A*"), "Expected to start with the first locus: " + glString);
			for (String locusFragment : glString.split("\\^")) {
				assertEquals(2, locusFragment.split("\\+").length,
						"Expected 2 allele copies (heterozygous) in every locus fragment: " + locusFragment);
			}
		}
	}

	@Test
	public void testConstructedGlStringIsValidPerLdValidationsOwnRules() throws IOException, URISyntaxException {
		String glString = buildFirstSample();

		assertTrue(GLStringUtilities.validateGLStringFormat(glString),
				"Expected ld-validation's own validator to accept this GL string");
	}

	@Test
	public void testFullRealAmbiguityExceedsLdValidationsDefaultThresholdsByDesign() throws IOException, URISyntaxException {
		// NOT a defect in this builder's output -- confirmed by first checking that
		// validateGLStringFormat() (above) accepts the same string as syntactically
		// well-formed. ld-validation deliberately refuses to enumerate possible
		// haplotypes for a genotype this ambiguous (HLA-C alone has 133 alleles in its
		// real "Included Alleles" list) -- its own existing safety mechanism against
		// combinatorial explosion, exactly the kind of thing the module README's
		// "Downstream is already solved" principle means: this module doesn't need to
		// reinvent that judgment call. Manually confirmed (outside this automated
		// suite, since LinkageDisequilibriumGenotypeList reads its threshold from a
		// system property in a static initializer that only runs once per JVM, so it
		// can't be reliably toggled mid-test-run) that raising both
		// -Dorg.dash.ambThreshold and -Dorg.dash.proteinThreshold lets the exact same
		// string construct successfully and round-trip unchanged.
		String glString = buildFirstSample();

		AmbiguousGenotypeException exception = assertThrows(AmbiguousGenotypeException.class,
				() -> new LinkageDisequilibriumGenotypeList("histo-full-ambiguity-test", glString));
		assertTrue(exception.getMessage().contains("HLA-C"), "Expected the exception to name the highly-ambiguous locus: "
				+ exception.getMessage());
	}

	@Test
	public void testALowAmbiguityLocusIsFullyConsumableByLdValidationWithoutRaisingAnyThreshold()
			throws IOException, URISyntaxException {
		// DRB1 alone (7-9 alleles per copy, well under ld-validation's default
		// threshold of 20) -- proves genuine end-to-end construction succeeds against
		// real output from this builder, without needing to touch JVM-wide static
		// state the way confirming the full-ambiguity case would.
		HistogeneticsSampleResultCandidate patient = findSample(detectSamples(), "Patient ID");
		HistogeneticsLocusValues drb1Values = patient.getLocusValues().stream()
				.filter(v -> v.getLocus() == org.dash.valid.Locus.HLA_DRB1).findFirst().orElseThrow();
		HistogeneticsSampleResultCandidate drb1Only = new HistogeneticsSampleResultCandidate(patient.getSampleId(),
				List.of(drb1Values), patient.getTypingStatusText(), patient.getMatchingRatio(),
				patient.getNullAlleleResolutionStatus(), patient.getSourceLine(), patient.getLineNumber());

		String glString = builder.build(drb1Only, detectAppendixEntries()).getGlString();

		LinkageDisequilibriumGenotypeList genotypeList = new LinkageDisequilibriumGenotypeList("histo-drb1-test", glString);
		assertNotNull(genotypeList);
		assertEquals(glString, genotypeList.getGLString());
	}

	@Test
	public void testSourceCandidatesIncludeBothTheSampleAndEveryAppendixEntryUsed() throws IOException, URISyntaxException {
		HistogeneticsSampleResultCandidate patient = findSample(detectSamples(), "Patient ID");

		ConstructedGlString result = builder.build(patient, detectAppendixEntries());

		// 1 sample candidate + 10 appendix entries (2 values x 5 loci) -- full
		// traceability back to every piece of report text this GL string came from.
		assertEquals(11, result.getSourceCandidates().size());
	}

	@Test
	public void testMissingAppendixEntryFailsLoudlyRatherThanFallingBackToTheBareGCode() throws IOException, URISyntaxException {
		// The actual point: falling back to the bare G-code (e.g. "HLA-A*02:01:01G")
		// would silently collapse to just that G-group's first/representative allele
		// -- exactly the ambiguity loss this builder exists to avoid. An empty
		// appendix entry list means no expansion is possible, so this must fail, not
		// degrade to something that looks like a real (but wrong) answer.
		HistogeneticsSampleResultCandidate patient = findSample(detectSamples(), "Patient ID");

		GlStringConstructionException exception = assertThrows(GlStringConstructionException.class,
				() -> builder.build(patient, List.of()));
		assertTrue(exception.getMessage().contains("HLA-A"), "Expected the failure message to name the locus: "
				+ exception.getMessage());
	}

	private String buildFirstSample() throws IOException, URISyntaxException {
		return builder.build(detectSamples().get(0), detectAppendixEntries()).getGlString();
	}

	private HistogeneticsSampleResultCandidate findSample(List<HistogeneticsSampleResultCandidate> samples, String sampleId) {
		return samples.stream().filter(s -> sampleId.equals(s.getSampleId())).findFirst()
				.orElseThrow(() -> new AssertionError("Expected a sample for " + sampleId));
	}

	private List<HistogeneticsSampleResultCandidate> detectSamples() throws IOException, URISyntaxException {
		return pageOneDetector.detect(extractText());
	}

	private List<HistogeneticsAppendixEntry> detectAppendixEntries() throws IOException, URISyntaxException {
		return appendixAccumulator.detect(extractText());
	}

	private String extractText() throws IOException, URISyntaxException {
		String resource = "sample-reports/histogenetics-hla-typing-g-code-sample.pdf";
		File file = new File(getClass().getClassLoader().getResource(resource).toURI());
		return extractor.extractText(file);
	}
}
