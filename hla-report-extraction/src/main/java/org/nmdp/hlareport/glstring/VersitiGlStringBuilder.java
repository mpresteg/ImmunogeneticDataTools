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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.dash.valid.gl.GLStringConstants;
import org.nmdp.hlareport.candidate.FootnoteReferencedAlleleCall;
import org.nmdp.hlareport.candidate.VersitiLocusResultCandidate;

/**
 * Builds a GL String from Versiti candidates ({@link VersitiLocusResultCandidate}). See
 * issue #45.
 *
 * Unlike {@link CegatGlStringBuilder}, an allele call here can carry a footnote-resolved
 * ambiguity (see {@link VersitiAmbiguityExpander}) instead of being a complete,
 * standalone designation -- that's the whole reason Versiti wasn't attempted alongside
 * CeGaT: the expansion needed real verification first (see that class's own comment,
 * and the module README).
 *
 * A candidate with an UNRESOLVED footnote reference (a marker seen but never matched to
 * a definition -- see
 * {@link FootnoteReferencedAlleleCall#hasUnresolvedFootnoteReference()}) makes the whole
 * build fail loudly ({@link GlStringConstructionException}), not silently produce a GL
 * String missing that locus's real ambiguity.
 */
public class VersitiGlStringBuilder {
	public ConstructedGlString build(List<VersitiLocusResultCandidate> candidates) {
		List<String> locusFragments = new ArrayList<>();

		for (VersitiLocusResultCandidate candidate : candidates) {
			String fragment = buildLocusFragment(candidate);
			if (fragment == null) {
				throw new GlStringConstructionException("Cannot build a GL String: " + candidate.getLocus()
						+ " has an allele call this builder cannot safely resolve (an unresolved footnote"
						+ " reference, or ambiguity shorthand VersitiAmbiguityExpander doesn't recognize)."
						+ " Candidate: " + candidate);
			}
			locusFragments.add(fragment);
		}

		String glString = String.join(GLStringConstants.GENE_DELIMITER, locusFragments);
		return new ConstructedGlString(glString, candidates);
	}

	// One locus's fragment: each allele call's own qualified/expanded representation,
	// joined by "+" (the diploid-copy delimiter). Returns null (never a guess) if any
	// allele call can't be safely resolved -- see this class's own comment.
	private String buildLocusFragment(VersitiLocusResultCandidate candidate) {
		List<String> copyFragments = new ArrayList<>();

		for (FootnoteReferencedAlleleCall alleleCall : candidate.getAlleleCalls()) {
			String copyFragment = buildAlleleCallFragment(alleleCall);
			if (copyFragment == null) {
				return null;
			}
			copyFragments.add(copyFragment);
		}

		return copyFragments.stream().collect(Collectors.joining(GLStringConstants.GENE_COPY_DELIMITER));
	}

	private String buildAlleleCallFragment(FootnoteReferencedAlleleCall alleleCall) {
		if (alleleCall.getFootnoteMarker() == null) {
			// No footnote at all -- already a complete, standalone designation, same
			// as every CeGaT allele call.
			return GLStringConstants.HLA_DASH + alleleCall.getAlleleCall();
		}

		if (alleleCall.hasUnresolvedFootnoteReference()) {
			return null;
		}

		return VersitiAmbiguityExpander.expand(alleleCall.getResolvedAlleles());
	}
}
