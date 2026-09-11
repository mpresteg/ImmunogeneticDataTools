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
 * Exercised against a real (de-identified, lab-published) sample report --
 * src/test/resources/sample-reports/PROVENANCE.md has the source. See the module
 * README's "Real reports drive the grammar, not assumption" principle: this asserts
 * against actual report text, not a guess at what a report might contain.
 */
public class PdfTextExtractorTest {
	private static final String SAMPLE_REPORT_RESOURCE = "sample-reports/versiti-hla-c-high-resolution-sample.pdf";

	private final PdfTextExtractor extractor = new PdfTextExtractor();

	@Test
	public void testExtractTextFindsKnownReportContent() throws IOException, URISyntaxException {
		String text = extractor.extractText(sampleReportFile());

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
	public void testHasExtractableTextLayerIsTrueForRealTextLayerPdf() throws IOException, URISyntaxException {
		String text = extractor.extractText(sampleReportFile());

		assertTrue(extractor.hasExtractableTextLayer(text));
	}

	private File sampleReportFile() throws URISyntaxException {
		return new File(getClass().getClassLoader().getResource(SAMPLE_REPORT_RESOURCE).toURI());
	}
}
