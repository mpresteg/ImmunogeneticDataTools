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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dash.valid.Locus;

/**
 * Detects Versiti-format locus-result blocks: a header line
 * ({@code "HLA-<locus> <resolution descriptor> <allele call>"}, e.g.
 * {@code "HLA-C High C*01:01"}), optionally followed by a continuation line carrying a
 * second allele call, either of which may carry a footnote marker (e.g. {@code "R1"})
 * that resolves elsewhere on the page. See issue #42.
 *
 * This is the pattern a naive "read the row, done" parser would get wrong: reporting
 * just the G-group code on a marked allele call (e.g. {@code "C*07:04:01G"}) without
 * following the marker to its resolution (e.g.
 * {@code "C*07:04:01G = HLA-C*07:04:01G=C*07:04/11"}) silently drops the very ambiguity
 * the footnote exists to preserve. This detector always follows the marker: if a
 * resolution is found, it's attached to the candidate; if a marker is seen but no
 * matching footnote definition exists, that's surfaced too (see
 * {@link FootnoteReferencedAlleleCall#hasUnresolvedFootnoteReference()}) rather than
 * silently treated as if no marker had been there at all.
 *
 * Deliberately Versiti-specific, not a generalization of
 * {@link CegatLocusResultLineDetector}: see that class's own comment, and the module
 * README's "How tethered is this to the 3 known reports?" section, for why a shared
 * detector isn't attempted yet with one real sample per pattern.
 */
public class VersitiLocusResultLineDetector {
	// The one footnote-marker shape actually seen in the one real sample this was built
	// against. Not generalized to other marker shapes (asterisks, "Note 1", etc.) since
	// nothing observed yet uses them -- see the module README's "Real reports drive the
	// grammar, not assumption" principle.
	private static final Pattern FOOTNOTE_MARKER_PATTERN = Pattern.compile("^R[0-9]+$");
	private static final Pattern FOOTNOTE_HEADER_PATTERN = Pattern.compile("^(R[0-9]+):\\s+.*$");

	public List<VersitiLocusResultCandidate> detect(String extractedText) {
		String[] lines = extractedText.split("\\r?\\n");
		Map<String, FootnoteDefinition> footnotesByMarker = indexFootnoteDefinitions(lines);

		List<VersitiLocusResultCandidate> candidates = new ArrayList<>();
		int i = 0;
		while (i < lines.length) {
			BlockDetectionResult result = detectBlock(lines, i, footnotesByMarker);
			if (result != null) {
				candidates.add(result.candidate);
				i += result.linesConsumed;
			} else {
				i++;
			}
		}

		return candidates;
	}

	// A footnote definition is two lines: "<marker>: <descriptor repeated>" (e.g.
	// "R1: HLA-C High") followed immediately by the actual resolution content (e.g.
	// "C*07:04:01G = HLA-C*07:04:01G=C*07:04/11"). Indexed up front, over the whole
	// document, since a footnote definition can appear anywhere relative to the
	// locus-result block(s) that reference it -- in the one sample seen so far it's
	// immediately below, but nothing guarantees that in general.
	private Map<String, FootnoteDefinition> indexFootnoteDefinitions(String[] lines) {
		Map<String, FootnoteDefinition> footnotesByMarker = new HashMap<>();

		for (int i = 0; i < lines.length; i++) {
			Matcher matcher = FOOTNOTE_HEADER_PATTERN.matcher(lines[i].trim());
			if (!matcher.matches() || i + 1 >= lines.length) {
				continue;
			}
			String marker = matcher.group(1);
			String resolutionText = lines[i + 1].trim();
			footnotesByMarker.put(marker, new FootnoteDefinition(resolutionText, i + 2));
		}

		return footnotesByMarker;
	}

	private BlockDetectionResult detectBlock(String[] lines, int startIndex,
			Map<String, FootnoteDefinition> footnotesByMarker) {
		String headerLine = lines[startIndex].trim();
		String[] headerTokens = headerLine.split("\\s+");
		if (headerTokens.length < 3 || !headerTokens[0].startsWith("HLA-")) {
			return null;
		}

		Locus locus = LocusLookup.byShortName(headerTokens[0].substring("HLA-".length()));
		if (locus == null) {
			return null;
		}

		String resolutionDescriptor = headerTokens[1];

		ParsedAlleleCall firstAlleleCall = parseAlleleCallToken(headerTokens, 2, locus);
		if (firstAlleleCall == null || 2 + firstAlleleCall.tokensConsumed != headerTokens.length) {
			return null;
		}

		List<FootnoteReferencedAlleleCall> alleleCalls = new ArrayList<>();
		alleleCalls.add(resolve(firstAlleleCall, footnotesByMarker));
		int linesConsumed = 1;

		// A continuation line carrying a second allele call is common (see the module
		// README) but not required -- a single allele call is a legitimate homozygous
		// result, not a malformed one, same principle as CegatLocusResultLineDetector.
		if (startIndex + 1 < lines.length) {
			String continuationLine = lines[startIndex + 1].trim();
			String[] continuationTokens = continuationLine.isEmpty() ? new String[0] : continuationLine.split("\\s+");
			ParsedAlleleCall secondAlleleCall = parseAlleleCallToken(continuationTokens, 0, locus);
			if (secondAlleleCall != null && secondAlleleCall.tokensConsumed == continuationTokens.length) {
				alleleCalls.add(resolve(secondAlleleCall, footnotesByMarker));
				linesConsumed = 2;
			}
		}

		VersitiLocusResultCandidate candidate = new VersitiLocusResultCandidate(locus, resolutionDescriptor,
				alleleCalls, headerLine, startIndex + 1);
		return new BlockDetectionResult(candidate, linesConsumed);
	}

	// Parses one allele call, with its optional trailing footnote marker, starting at
	// tokens[startIndex]. Requires the allele token's own locus prefix to agree with
	// expectedLocus -- same "looks allele-shaped" -> "is internally consistent with the
	// row it's on" upgrade as CegatLocusResultLineDetector.
	private ParsedAlleleCall parseAlleleCallToken(String[] tokens, int startIndex, Locus expectedLocus) {
		if (startIndex >= tokens.length || !AlleleToken.matches(tokens[startIndex])) {
			return null;
		}

		String alleleToken = tokens[startIndex];
		Locus alleleLocus = LocusLookup.byShortName(AlleleToken.locusPrefix(alleleToken));
		if (alleleLocus != expectedLocus) {
			return null;
		}

		if (startIndex + 1 < tokens.length && FOOTNOTE_MARKER_PATTERN.matcher(tokens[startIndex + 1]).matches()) {
			return new ParsedAlleleCall(alleleToken, tokens[startIndex + 1], 2);
		}

		return new ParsedAlleleCall(alleleToken, null, 1);
	}

	private FootnoteReferencedAlleleCall resolve(ParsedAlleleCall parsed, Map<String, FootnoteDefinition> footnotesByMarker) {
		if (parsed.footnoteMarker == null) {
			return new FootnoteReferencedAlleleCall(parsed.alleleToken, null, null, -1);
		}

		FootnoteDefinition definition = footnotesByMarker.get(parsed.footnoteMarker);
		if (definition == null) {
			// Marker seen, but no matching definition found -- surfaced as unresolved
			// rather than silently treated as if there had been no marker at all.
			return new FootnoteReferencedAlleleCall(parsed.alleleToken, parsed.footnoteMarker, null, -1);
		}

		return new FootnoteReferencedAlleleCall(parsed.alleleToken, parsed.footnoteMarker, definition.resolutionText,
				definition.resolutionLineNumber);
	}

	private static final class ParsedAlleleCall {
		private final String alleleToken;
		private final String footnoteMarker;
		private final int tokensConsumed;

		private ParsedAlleleCall(String alleleToken, String footnoteMarker, int tokensConsumed) {
			this.alleleToken = alleleToken;
			this.footnoteMarker = footnoteMarker;
			this.tokensConsumed = tokensConsumed;
		}
	}

	private static final class FootnoteDefinition {
		private final String resolutionText;
		private final int resolutionLineNumber;

		private FootnoteDefinition(String resolutionText, int resolutionLineNumber) {
			this.resolutionText = resolutionText;
			this.resolutionLineNumber = resolutionLineNumber;
		}
	}

	private static final class BlockDetectionResult {
		private final VersitiLocusResultCandidate candidate;
		private final int linesConsumed;

		private BlockDetectionResult(VersitiLocusResultCandidate candidate, int linesConsumed) {
			this.candidate = candidate;
			this.linesConsumed = linesConsumed;
		}
	}
}
