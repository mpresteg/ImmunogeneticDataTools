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
package org.nmdp.hlareport.cli;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nmdp.hlareport.candidate.CegatLocusResultLineDetector;
import org.nmdp.hlareport.candidate.VersitiLocusResultLineDetector;
import org.nmdp.hlareport.candidate.histogenetics.HistogeneticsAppendixAccumulator;
import org.nmdp.hlareport.candidate.histogenetics.HistogeneticsPageOneTableDetector;
import org.nmdp.hlareport.extract.PdfTextExtractor;

/**
 * A minimal, interim CLI for manually trying this module's candidate-line detection
 * against a real PDF -- there's no packaged distribution or formal argument parsing
 * (unlike ld-tools' appassembler-based CLIs) because this module doesn't have a stable
 * enough surface yet to justify that ceremony: partial support for three lab formats
 * so far (#43, #42, and #44's still-in-progress sub-issues), and no GL String
 * construction (#45) or validation gate (#46) at all. This exists so a real PDF can be
 * tried against what's here today, without writing a one-off script by hand each time
 * -- see the module README's "Trying it yourself" section.
 *
 * Deliberately prints candidates as a review worklist, not as trusted output: every
 * line is prefixed with its source line number and shows the exact report text it came
 * from, per the module's "structural signal, not a content guess" principle. This is
 * not a GL String, and nothing here should be treated as validated typing data.
 */
public class DetectHlaReport {
	// A small registry rather than hardcoding one detector as the only option -- adding
	// #44 (Histogenetics) later should mean adding an entry here, not restructuring this
	// class. ReportDetector returns List<?> rather than a shared candidate type because
	// there isn't one: CegatLocusResultLineDetector and VersitiLocusResultLineDetector
	// each return their own candidate type (see VersitiLocusResultCandidate's class
	// comment for why that's deliberate, not an oversight). Printed via each candidate's
	// own toString() below, which is enough for a human review worklist.
	private static final Map<String, ReportDetector> DETECTORS = buildDetectorRegistry();

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: detect-hla-report <path-to-report.pdf>");
			System.err.println();
			System.err.println("Runs every candidate-line detector this module currently has (so far: "
					+ String.join(", ", DETECTORS.keySet())
					+ ") against the given PDF and prints what each one finds, as candidates for human"
					+ " review -- not a GL String, not validated typing data.");
			System.exit(1);
		}

		File pdfFile = new File(args[0]);
		if (!pdfFile.isFile()) {
			System.err.println("Not a file: " + pdfFile);
			System.exit(1);
		}

		try {
			run(pdfFile);
		} catch (IOException e) {
			System.err.println("Failed to read " + pdfFile + ": " + e.getMessage());
			System.exit(1);
		}
	}

	private static void run(File pdfFile) throws IOException {
		PdfTextExtractor extractor = new PdfTextExtractor();
		String extractedText = extractor.extractText(pdfFile);

		if (!extractor.hasExtractableTextLayer(extractedText)) {
			System.out.println("No extractable text layer found in " + pdfFile
					+ " -- likely a scanned page. This module has no OCR fallback yet (see the"
					+ " module README); nothing more can be done with this file right now.");
			return;
		}

		System.out.println("Report: " + pdfFile);
		System.out.println("(candidates below are for human review -- not a GL String, not validated)");
		System.out.println();

		for (Map.Entry<String, ReportDetector> entry : DETECTORS.entrySet()) {
			String detectorName = entry.getKey();
			List<?> candidates = entry.getValue().detect(extractedText);

			System.out.println("== " + detectorName + " detector: " + candidates.size() + " candidate(s) ==");
			for (Object candidate : candidates) {
				System.out.println("  " + candidate);
			}
			System.out.println();
		}
	}

	private static Map<String, ReportDetector> buildDetectorRegistry() {
		Map<String, ReportDetector> detectors = new LinkedHashMap<>();
		detectors.put("cegat", new CegatLocusResultLineDetector()::detect);
		detectors.put("versiti", new VersitiLocusResultLineDetector()::detect);
		// #51 (FAILED/PENDING/narrative-ambiguity) is still open for Histogenetics.
		detectors.put("histogenetics-appendix", new HistogeneticsAppendixAccumulator()::detect);
		detectors.put("histogenetics-page1", new HistogeneticsPageOneTableDetector()::detect);
		return detectors;
	}

	@FunctionalInterface
	private interface ReportDetector {
		List<?> detect(String extractedText);
	}
}
