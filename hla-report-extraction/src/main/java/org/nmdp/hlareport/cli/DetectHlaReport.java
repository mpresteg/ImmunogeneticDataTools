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
import org.nmdp.hlareport.candidate.LocusResultCandidate;
import org.nmdp.hlareport.candidate.VersitiLocusResultCandidate;
import org.nmdp.hlareport.candidate.VersitiLocusResultLineDetector;
import org.nmdp.hlareport.candidate.histogenetics.HistogeneticsAppendixAccumulator;
import org.nmdp.hlareport.candidate.histogenetics.HistogeneticsAppendixEntry;
import org.nmdp.hlareport.candidate.histogenetics.HistogeneticsNarrativeAmbiguityDetector;
import org.nmdp.hlareport.candidate.histogenetics.HistogeneticsPageOneTableDetector;
import org.nmdp.hlareport.candidate.histogenetics.HistogeneticsPlaceholderTableDetector;
import org.nmdp.hlareport.candidate.histogenetics.HistogeneticsSampleResultCandidate;
import org.nmdp.hlareport.extract.PdfTextExtractor;
import org.nmdp.hlareport.glstring.CegatGlStringBuilder;
import org.nmdp.hlareport.glstring.ConstructedGlString;
import org.nmdp.hlareport.glstring.GlStringConstructionException;
import org.nmdp.hlareport.glstring.HistogeneticsGlStringBuilder;
import org.nmdp.hlareport.glstring.VersitiGlStringBuilder;

/**
 * A minimal, interim CLI for manually trying this module's candidate-line detection
 * against a real PDF -- there's no packaged distribution or formal argument parsing
 * (unlike ld-tools' appassembler-based CLIs) because this module doesn't have a stable
 * enough surface yet to justify that ceremony: #44's sub-issues (page-1 table, appendix,
 * and non-standard result states) are all done, and so is all of #45 (CeGaT, Versiti,
 * and Histogenetics GL String construction), but there's no validation gate (#46) at
 * all. This exists so a real PDF can be tried against what's here today, without
 * writing a one-off script by hand each time -- see the module README's "Trying it
 * yourself" section.
 *
 * Deliberately prints candidates (and the constructed GL String) as
 * a review worklist, not as trusted output: every candidate line is prefixed with its
 * source line number and shows the exact report text it came from, and the GL string
 * section carries the same "not validated" framing, per the module's "structural
 * signal, not a content guess" principle. Nothing here should be treated as validated
 * typing data without a human actually checking it against the report.
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

		printCegatGlString(extractedText);
		printVersitiGlString(extractedText);
		printHistogeneticsGlStrings(extractedText);
	}

	// GL String construction (#45) is now implemented for all three labs. Not run
	// through the generic DETECTORS registry above since each needs its own typed
	// candidate list, not the List<?> that registry's shared interface deliberately
	// uses.
	private static void printCegatGlString(String extractedText) {
		List<LocusResultCandidate> cegatCandidates = new CegatLocusResultLineDetector().detect(extractedText);
		if (cegatCandidates.isEmpty()) {
			return;
		}

		ConstructedGlString glString = new CegatGlStringBuilder().build(cegatCandidates);
		System.out.println("== cegat GL String (NOT validated -- confirm against the candidates above first) ==");
		System.out.println("  " + glString.getGlString());
		System.out.println();
	}

	private static void printVersitiGlString(String extractedText) {
		List<VersitiLocusResultCandidate> versitiCandidates = new VersitiLocusResultLineDetector().detect(extractedText);
		if (versitiCandidates.isEmpty()) {
			return;
		}

		System.out.println("== versiti GL String (NOT validated -- confirm against the candidates above first) ==");
		try {
			ConstructedGlString glString = new VersitiGlStringBuilder().build(versitiCandidates);
			System.out.println("  " + glString.getGlString());
		} catch (GlStringConstructionException e) {
			// A candidate exists but couldn't be safely converted (e.g. an unresolved
			// footnote reference) -- surfaced clearly, never silently dropped or
			// papered over with a partial/wrong GL string.
			System.out.println("  Could not build a GL String: " + e.getMessage());
		}
		System.out.println();
	}

	// One GL String per sample (patient, donor 1, donor 2, ...) -- Histogenetics can
	// bundle more than one subject in a single report, unlike CeGaT/Versiti.
	private static void printHistogeneticsGlStrings(String extractedText) {
		List<HistogeneticsSampleResultCandidate> samples = new HistogeneticsPageOneTableDetector().detect(extractedText);
		if (samples.isEmpty()) {
			return;
		}

		List<HistogeneticsAppendixEntry> appendixEntries = new HistogeneticsAppendixAccumulator().detect(extractedText);
		HistogeneticsGlStringBuilder builder = new HistogeneticsGlStringBuilder();

		for (HistogeneticsSampleResultCandidate sample : samples) {
			System.out.println("== histogenetics GL String for " + sample.getSampleId()
					+ " (NOT validated -- confirm against the candidates above first) ==");
			try {
				ConstructedGlString glString = builder.build(sample, appendixEntries);
				System.out.println("  " + glString.getGlString());
			} catch (GlStringConstructionException e) {
				System.out.println("  Could not build a GL String: " + e.getMessage());
			}
			System.out.println();
		}
	}

	private static Map<String, ReportDetector> buildDetectorRegistry() {
		Map<String, ReportDetector> detectors = new LinkedHashMap<>();
		detectors.put("cegat", new CegatLocusResultLineDetector()::detect);
		detectors.put("versiti", new VersitiLocusResultLineDetector()::detect);
		detectors.put("histogenetics-appendix", new HistogeneticsAppendixAccumulator()::detect);
		detectors.put("histogenetics-page1", new HistogeneticsPageOneTableDetector()::detect);
		detectors.put("histogenetics-failed", new HistogeneticsPlaceholderTableDetector("FAILED")::detect);
		detectors.put("histogenetics-pending", new HistogeneticsPlaceholderTableDetector("PENDING")::detect);
		detectors.put("histogenetics-xxxx", new HistogeneticsPlaceholderTableDetector("XXXX")::detect);
		detectors.put("histogenetics-narrative-ambiguity", new HistogeneticsNarrativeAmbiguityDetector()::detect);
		return detectors;
	}

	@FunctionalInterface
	private interface ReportDetector {
		List<?> detect(String extractedText);
	}
}
