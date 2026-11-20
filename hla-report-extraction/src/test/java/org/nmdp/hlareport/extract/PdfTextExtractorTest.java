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
package org.nmdp.hlareport.extract;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

/**
 * Exercised against real (de-identified, lab-published or lab-templated) sample
 * reports from three different labs -- src/test/resources/sample-reports/PROVENANCE.md
 * has each source. See the module README's "Real reports drive the grammar, not
 * assumption" principle: these assert against actual report text, not a guess at what
 * a report might contain, and the three labs' genuinely different conventions (no
 * ambiguity at all; footnote-resolved G-group; G-code as the primary reported value)
 * are exactly why more than one sample mattered here.
 */
public class PdfTextExtractorTest {
	private final PdfTextExtractor extractor = new PdfTextExtractor();

	@Test
	public void testExtractTextFindsKnownContentVersiti() throws IOException, URISyntaxException {
		String text = extractText("versiti-hla-c-high-resolution-sample.pdf");

		// Lab identity and report section headers
		assertTrue(text.contains("Versiti Wisconsin"), "Expected lab name in extracted text");
		assertTrue(text.contains("Histocompatibility"), "Expected section header in extracted text");
		assertTrue(text.contains("HLA Typing Report"), "Expected section header in extracted text");

		// The actual locus result: HLA-C High resolution typing, two allele calls,
		// the second reported at the G-group level with a footnote reference (R1)
		// resolving it to the underlying ambiguous alleles. Confirms the text layer
		// survives extraction with its structure (allele calls + footnote markers +
		// footnote text) intact -- see the module README on why that footnote
		// pattern matters: a naive "read the row, done" parser would silently drop
		// the ambiguity that R1 is there to preserve.
		assertTrue(text.contains("C*01:01"), "Expected first allele call in extracted text");
		assertTrue(text.contains("C*07:04:01G"), "Expected second (G-group) allele call in extracted text");
		assertTrue(text.contains("C*07:04:01G = HLA-C*07:04:01G=C*07:04/11"),
				"Expected footnote resolving the G-group call to its underlying alleles");
	}

	@Test
	public void testExtractTextFindsKnownContentCegat() throws IOException, URISyntaxException {
		String text = extractText("cegat-hla-typing-sample.pdf");

		// Lab identity and report section headers
		assertTrue(text.contains("CeGaT GmbH"), "Expected lab name in extracted text");
		assertTrue(text.contains("HLA-Typing Report"), "Expected report title in extracted text");

		// Unlike Versiti, this report has zero ambiguity: every locus is reported as
		// two fully-resolved alleles (or one, for a homozygous or single-gene-present
		// locus like DRB345), no G-codes, no footnotes. Confirms extraction handles
		// the "easy case" cleanly, not just the harder ones.
		assertTrue(text.contains("A*24:02"), "Expected a resolved allele call in extracted text");
		assertTrue(text.contains("DRB4*01:03"), "Expected the single-gene DRB345 call in extracted text");
		assertTrue(text.contains("Zygosity"), "Expected the zygosity explanatory note in extracted text");
	}

	@Test
	public void testExtractTextFindsKnownContentHistogenetics() throws IOException, URISyntaxException {
		String text = extractText("histogenetics-hla-typing-g-code-sample.pdf");

		// Lab identity and report structure -- patient + donor matching, not just a
		// standalone typing result
		assertTrue(text.contains("histogenetics.com"), "Expected lab domain in extracted text");
		assertTrue(text.contains("HLA TYPING REPORT"), "Expected report title in extracted text");
		assertTrue(text.contains("Matching Ratio with Patient"), "Expected donor matching field in extracted text");
		assertTrue(text.contains("10/10"), "Expected the matching ratio value in extracted text");

		// This report's primary reported value is the G-code itself (not a resolved
		// allele pair, and not footnote-resolved the way Versiti's is) -- e.g.
		// "A* 02:01:01G". Confirms that value survives extraction.
		assertTrue(text.contains("02:01:01G"), "Expected a G-code result in extracted text");

		// Null Allele Resolution Status: specific null alleles explicitly excluded
		// from a G-code call, e.g. "A*02:43N ... excluded". A different ambiguity
		// disclosure mechanism again from either other report.
		assertTrue(text.contains("A*02:43N"), "Expected an explicitly-excluded null allele in extracted text");
		assertTrue(text.contains("excluded"), "Expected the null-allele-exclusion note in extracted text");

		// The appendix (later pages) expands each G-code to its NMDP allele code and
		// full enumerated list of included alleles -- confirms multi-page extraction
		// actually reaches this content, not just the page 1 summary table.
		assertTrue(text.contains("NMDP Allele Code"), "Expected appendix column header in extracted text");
		assertTrue(text.contains("Included Alleles"), "Expected appendix column header in extracted text");
		assertTrue(text.contains("02:DMFHE"), "Expected the NMDP allele code for A*02:01:01G in extracted text");
	}

	@Test
	public void testHasExtractableTextLayerIsTrueForRealTextLayerPdfs() throws IOException, URISyntaxException {
		assertTrue(extractor.hasExtractableTextLayer(extractText("versiti-hla-c-high-resolution-sample.pdf")));
		assertTrue(extractor.hasExtractableTextLayer(extractText("cegat-hla-typing-sample.pdf")));
		assertTrue(extractor.hasExtractableTextLayer(extractText("histogenetics-hla-typing-g-code-sample.pdf")));
	}

	private String extractText(String sampleReportResourceName) throws IOException, URISyntaxException {
		String resource = "sample-reports/" + sampleReportResourceName;
		File file = new File(getClass().getClassLoader().getResource(resource).toURI());
		return extractor.extractText(file);
	}
}
