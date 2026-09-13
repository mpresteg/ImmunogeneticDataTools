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

import java.util.List;
import java.util.stream.Collectors;

import org.dash.valid.gl.GLStringConstants;
import org.nmdp.hlareport.candidate.LocusResultCandidate;

/**
 * Builds a GL String from CeGaT candidates ({@link LocusResultCandidate}). See issue
 * #45 -- the first, and so far only, lab this has been implemented for.
 *
 * CeGaT is the deliberate starting point, same reasoning as issue #43 picking it first
 * for candidate-line detection: every allele call is already a complete, standalone
 * designation (no G-codes, no NMDP codes, no footnote-resolved ambiguity to expand).
 * Building it requires no shorthand interpretation at all -- just prefixing "HLA-" and
 * joining with the existing grammar's own delimiters
 * ({@link GLStringConstants#GENE_COPY_DELIMITER} within a locus,
 * {@link GLStringConstants#GENE_DELIMITER} between loci). Versiti and Histogenetics are
 * deliberately NOT attempted here yet -- see the module README for why (Versiti's
 * footnote shorthand has a genuine open semantic question; Histogenetics' NMDP-code
 * decoding depends on a live network call whose behavior needs verifying first).
 *
 * Assembles ALL given candidates into ONE GL String, on the assumption they all belong
 * to the same subject -- true for every CeGaT report seen so far (a single-patient
 * report, unlike Histogenetics' patient+donor(s) bundling). Not revisited until a CeGaT
 * report with multiple subjects is actually seen.
 */
public class CegatGlStringBuilder {
	public ConstructedGlString build(List<LocusResultCandidate> candidates) {
		String glString = candidates.stream().map(this::buildLocusFragment)
				.collect(Collectors.joining(GLStringConstants.GENE_DELIMITER));

		return new ConstructedGlString(glString, candidates);
	}

	// One locus's fragment: its allele call(s) joined by "+" (the diploid-copy
	// delimiter). A single-call candidate (e.g. a single-gene-present DRB345 result --
	// see LocusResultCandidate's own comment) produces a single-copy fragment with no
	// "+" at all, rather than guessing at zygosity by duplicating the one known call --
	// same "don't guess beyond what's known" principle as candidate detection itself.
	private String buildLocusFragment(LocusResultCandidate candidate) {
		return candidate.getAlleleCalls().stream().map(alleleCall -> GLStringConstants.HLA_DASH + alleleCall)
				.collect(Collectors.joining(GLStringConstants.GENE_COPY_DELIMITER));
	}
}
