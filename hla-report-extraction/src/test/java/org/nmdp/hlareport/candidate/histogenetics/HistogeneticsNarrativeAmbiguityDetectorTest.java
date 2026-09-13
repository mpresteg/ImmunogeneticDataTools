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
import java.util.Optional;

import org.dash.valid.Locus;
import org.junit.jupiter.api.Test;
import org.nmdp.hlareport.extract.PdfTextExtractor;

/**
 * Exercised against the real (de-identified) Histogenetics sample report -- see
 * src/test/resources/sample-reports/PROVENANCE.md. Per the module README's "Real
 * reports drive the grammar, not assumption" principle.
 */
public class HistogeneticsNarrativeAmbiguityDetectorTest {
	private final PdfTextExtractor extractor = new PdfTextExtractor();
	private final HistogeneticsNarrativeAmbiguityDetector detector = new HistogeneticsNarrativeAmbiguityDetector();

	@Test
	public void testDetectsAllElevenLociForTheXxxxReport() throws IOException, URISyntaxException {
		List<HistogeneticsNarrativeAmbiguityEntry> entries = detector.detect(extractText());

		// 11, not 9: this narrative section spells out DRB3/DRB4/DRB5 individually,
		// unlike the page-1 table's combined DRB345 column.
		assertEquals(11, entries.size(), "Expected one entry per locus mentioned in the narrative: " + entries);
		assertTrue(entries.stream().allMatch(e -> "Patient ID 4".equals(e.getSampleId())),
				"Expected the Sample Id parsed from the first sentence's own line to apply to every entry: " + entries);
	}

	@Test
	public void testParsesTheFirstSentenceIncludingItsEmbeddedSampleIdAndDatePrefix() throws IOException, URISyntaxException {
		// The actual point of this test: the first "Possible Allele" sentence shares a
		// physical line with a date and a "Sample Id :" field before it -- confirms
		// both get separated out correctly, not swallowed into either field.
		HistogeneticsNarrativeAmbiguityEntry entry = findEntry(extractText(), Locus.HLA_A);

		assertEquals("Patient ID 4", entry.getSampleId());
		assertEquals("01:01:01/02:01:01/30:01:01", entry.getPossibleAlleles());
		assertEquals(597, entry.getLineNumber());
	}

	@Test
	public void testEmptyPossibleAllelesListIsSurfacedAsEmptyNotSkipped() throws IOException, URISyntaxException {
		// "Possible Allele in DRB3 :  ." -- no alleles listed at all. This is real
		// information (this locus's ambiguity resolution came up empty), not a parse
		// failure to silently drop.
		HistogeneticsNarrativeAmbiguityEntry entry = findEntry(extractText(), Locus.HLA_DRB3);

		assertEquals("", entry.getPossibleAlleles());
	}

	@Test
	public void testBareAlleleDesignationsHaveNoLocusPrefixUnlikeEverythingElseInThisModule() throws IOException, URISyntaxException {
		// Worth asserting explicitly: this notation genuinely differs from every other
		// ambiguity representation seen so far (no "DRB1*" prefix, no "*" at all) --
		// captured verbatim, not normalized to look like the others.
		HistogeneticsNarrativeAmbiguityEntry entry = findEntry(extractText(), Locus.HLA_DRB1);

		assertEquals("01:01:01/07:01:01/15:01:01", entry.getPossibleAlleles());
	}

	@Test
	public void testDoesNotMisfireAgainstCegatsDifferentlyShapedReport() throws IOException, URISyntaxException {
		List<HistogeneticsNarrativeAmbiguityEntry> entries = detector.detect(extractText("cegat-hla-typing-sample.pdf"));

		assertTrue(entries.isEmpty(), "Expected no entries in a CeGaT-format report, found: " + entries);
	}

	@Test
	public void testDoesNotMisfireAgainstVersitisDifferentlyShapedReport() throws IOException, URISyntaxException {
		List<HistogeneticsNarrativeAmbiguityEntry> entries = detector
				.detect(extractText("versiti-hla-c-high-resolution-sample.pdf"));

		assertTrue(entries.isEmpty(), "Expected no entries in a Versiti-format report, found: " + entries);
	}

	private HistogeneticsNarrativeAmbiguityEntry findEntry(String extractedText, Locus locus) {
		List<HistogeneticsNarrativeAmbiguityEntry> entries = detector.detect(extractedText);
		Optional<HistogeneticsNarrativeAmbiguityEntry> entry = entries.stream().filter(e -> e.getLocus() == locus).findFirst();
		assertTrue(entry.isPresent(), "Expected an entry for " + locus + " in: " + entries);
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
