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

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Pulls raw text out of a lab report PDF's embedded text layer.
 *
 * Deliberately does no interpretation of that text -- no locus/allele recognition, no
 * line classification, nothing GL-String-shaped. See the module README's guiding
 * principles: this is step 1 of the "Then (tentative)" list only, and the next step
 * (candidate-line detection) is intentionally a separate concern so that "did we read
 * the PDF correctly" and "did we understand what it says" stay independently testable
 * and reviewable.
 *
 * OCR fallback for scanned pages (an empty or near-empty text layer) is out of scope
 * for this class -- see {@link #hasExtractableTextLayer(String)} for how a caller can
 * detect that case and decide what to do about it. No OCR fallback is wired up yet
 * because no scanned sample report has been seen yet to test one against.
 */
public class PdfTextExtractor {
	// An essentially-empty extraction (e.g. a handful of stray characters from a
	// stamp or watermark on an otherwise-scanned page) shouldn't be mistaken for a
	// real text layer. This is a starting guess, not a tuned threshold -- revisit
	// once a real scanned report is available to test against.
	private static final int MIN_TEXT_LAYER_LENGTH = 40;

	/**
	 * @param pdfFile a lab report PDF
	 * @return the PDF's full embedded text layer, page breaks preserved as newlines
	 * @throws IOException if the file can't be read or isn't a valid PDF
	 */
	public String extractText(File pdfFile) throws IOException {
		try (PDDocument document = Loader.loadPDF(pdfFile)) {
			PDFTextStripper stripper = new PDFTextStripper();
			return stripper.getText(document);
		}
	}

	/**
	 * @param extractedText the result of {@link #extractText(File)}
	 * @return false if the text layer is effectively absent (e.g. a scanned page),
	 *         meaning the caller has no text-layer content to work with and should
	 *         fall back to OCR -- or, until an OCR path exists, surface the page for
	 *         human review rather than silently treating it as "no results found"
	 */
	public boolean hasExtractableTextLayer(String extractedText) {
		return extractedText != null && extractedText.trim().length() >= MIN_TEXT_LAYER_LENGTH;
	}
}
