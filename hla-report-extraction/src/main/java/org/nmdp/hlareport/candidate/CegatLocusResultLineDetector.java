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
package org.nmdp.hlareport.candidate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dash.valid.Locus;

/**
 * Detects CeGaT-format locus-result rows: a bare locus label followed by one or two
 * allele-shaped tokens on the same line, e.g. {@code "A A*24:02 A*24:02"} or (a
 * single-gene-present DRB345 result) {@code "DRB345 DRB4*01:03"}. See issue #43 -- the
 * "easy case" among the three known report conventions, since CeGaT reports every
 * locus fully resolved with no ambiguity, no G-codes, and no footnotes to chase.
 *
 * Deliberately named for CeGaT specifically, not a generic "locus/allele-pair table"
 * detector: Versiti's and Histogenetics' page-1 tables are shaped differently enough
 * (see #42 and #44) that a shared detector would either miss real structure or need to
 * already anticipate patterns from reports this module hasn't generalized from yet.
 * See the module README's "Real reports drive the grammar, not assumption" principle --
 * a name this specific is the honest one until a second lab is confirmed to share this
 * exact row shape.
 *
 * This is shape-driven, not section-scoped: it doesn't look for CeGaT's "Results"
 * header first and confine itself to the table below it, it just looks for lines that
 * are shaped like a locus-result row anywhere in the extracted text. That's safe for
 * the one report this has been tested against (see CegatLocusResultLineDetectorTest --
 * nothing outside the actual Results table matches this shape), but a report from
 * elsewhere in CeGaT's own output (or a future revision of this report format) could in
 * principle produce a same-shaped false positive elsewhere on the page. Exactly why
 * this produces *candidates* for human review rather than being trusted outright --
 * see the module README's "Structural signal, not a content guess" principle.
 */
public class CegatLocusResultLineDetector {
	// Built from Locus's own shortName rather than a hardcoded list of strings, so this
	// stays in sync with whatever loci ld-validation's Locus enum recognizes rather than
	// duplicating that knowledge here.
	private static final Map<String, Locus> LOCUS_BY_SHORT_NAME = Arrays.stream(Locus.values())
			.collect(Collectors.toMap(Locus::getShortName, locus -> locus));

	public List<LocusResultCandidate> detect(String extractedText) {
		List<LocusResultCandidate> candidates = new ArrayList<>();

		String[] lines = extractedText.split("\\r?\\n");
		for (int i = 0; i < lines.length; i++) {
			LocusResultCandidate candidate = detectLine(lines[i], i + 1);
			if (candidate != null) {
				candidates.add(candidate);
			}
		}

		return candidates;
	}

	private LocusResultCandidate detectLine(String line, int lineNumber) {
		String trimmedLine = line.trim();
		// Avoid calling Locus.lookup() (and ld-validation's LOCUS_BY_SHORT_NAME) here as
		// the first check -- Locus.lookup() logs a WARNING on every miss, and almost
		// every line in a real report isn't a locus-result row, so that would spam the
		// log once per non-matching line in the whole document.
		String[] tokens = trimmedLine.split("\\s+");
		if (tokens.length < 2 || tokens.length > 3) {
			return null;
		}

		Locus locus = LOCUS_BY_SHORT_NAME.get(tokens[0]);
		if (locus == null) {
			return null;
		}

		List<String> alleleCalls = new ArrayList<>();
		for (int i = 1; i < tokens.length; i++) {
			if (!AlleleToken.matches(tokens[i]) || !alleleTokenAgreesWithRowLocus(locus, tokens[i])) {
				return null;
			}
			alleleCalls.add(tokens[i]);
		}

		return new LocusResultCandidate(locus, alleleCalls, trimmedLine, lineNumber);
	}

	// DRB345 is CeGaT's own combined column label for the DRB3/4/5 genes; an allele
	// token on that row names whichever of DRB3/DRB4/DRB5 is actually present (e.g.
	// "DRB4*01:03"), never literally "DRB345*...". Every other locus's allele tokens
	// are expected to repeat the row's own label as their prefix. Checking this (rather
	// than accepting any allele-shaped token on a recognized locus row) turns "looks
	// allele-shaped" into "is internally consistent with the row it's on" -- a stronger
	// structural signal, per the module README's guiding principle.
	private boolean alleleTokenAgreesWithRowLocus(Locus rowLocus, String alleleToken) {
		String prefix = AlleleToken.locusPrefix(alleleToken);
		Locus alleleLocus = LOCUS_BY_SHORT_NAME.get(prefix);
		if (alleleLocus == null) {
			return false;
		}

		if (rowLocus == Locus.HLA_DRB345) {
			return Locus.isDRB345(alleleLocus);
		}

		return alleleLocus == rowLocus;
	}
}
