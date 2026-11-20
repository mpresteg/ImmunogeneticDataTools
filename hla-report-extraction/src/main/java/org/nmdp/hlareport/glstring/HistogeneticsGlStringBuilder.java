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
import java.util.Objects;
import java.util.Optional;

import org.dash.valid.Locus;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.gl.GLStringUtilities;
import org.nmdp.hlareport.candidate.histogenetics.HistogeneticsAppendixEntry;
import org.nmdp.hlareport.candidate.histogenetics.HistogeneticsLocusValues;
import org.nmdp.hlareport.candidate.histogenetics.HistogeneticsSampleResultCandidate;

/**
 * Builds a GL String for one Histogenetics sample (patient, donor 1, donor 2, ...) by
 * combining {@link HistogeneticsPageOneTableDetector}'s output (which loci/values a
 * sample has) with {@link HistogeneticsAppendixAccumulator}'s output (what each value
 * actually means, as a complete enumerated allele list). See issue #45.
 *
 * Deliberately does NOT use {@code GLStringUtilities.decodeMAC()} -- confirmed against
 * the real fixture that the appendix's own "Included Alleles" text, once prefixed with
 * the locus, is already shaped exactly the way {@code GLStringUtilities.
 * fullyQualifyGLString()} expects (each member restates its full field set, the same
 * convention {@code ld-validation}'s own {@code shorthandExamples.txt} uses -- unlike
 * Versiti's footnote shorthand, which needed {@link VersitiAmbiguityExpander}). That
 * makes the report's own already-extracted text sufficient on its own: no live network
 * call to NMDP's MAC decode API, and no risk of a version mismatch between whatever
 * IMGT/HLA release the lab used when the report was issued and whatever release a live
 * query would return today. Confirmed by timing every appendix entry in the real
 * fixture through {@code fullyQualifyGLString()} before relying on this (0-21ms each,
 * no network calls triggered) -- that method calls {@code decodeMAC()} internally under
 * some conditions, so this wasn't assumed safe, it was checked.
 *
 * Needs both detectors' output together because a sample's page-1 table only says
 * *which* value was reported for a locus (e.g. a G-code); the appendix is the only place
 * that says what that value actually *means* (the full allele list). Building from the
 * page-1 table alone (e.g. using the bare G-code "HLA-A*02:01:01G" directly) would
 * silently collapse to just the G-group's first/representative allele -- exactly the
 * kind of silent ambiguity loss this module exists to avoid (the same reasoning as
 * chasing Versiti's footnote instead of reporting its bare G-group code, issue #42).
 */
public class HistogeneticsGlStringBuilder {
	/**
	 * @param sampleCandidate one sample's page-1 result (from
	 *                        {@code HistogeneticsPageOneTableDetector})
	 * @param appendixEntries the FULL appendix result set (from
	 *                        {@code HistogeneticsAppendixAccumulator}) -- not
	 *                        pre-filtered to this sample; matched internally by sample
	 *                        id, locus, and reported value
	 * @throws GlStringConstructionException if any of sampleCandidate's locus values has
	 *                                        no matching appendix entry -- refuses to
	 *                                        fall back to the bare, ambiguity-collapsing
	 *                                        G-code
	 */
	public ConstructedGlString build(HistogeneticsSampleResultCandidate sampleCandidate,
			List<HistogeneticsAppendixEntry> appendixEntries) {
		List<Object> sourceCandidates = new ArrayList<>();
		sourceCandidates.add(sampleCandidate);

		List<String> locusFragments = new ArrayList<>();
		for (HistogeneticsLocusValues locusValues : sampleCandidate.getLocusValues()) {
			List<String> copyFragments = new ArrayList<>();

			for (String value : locusValues.getValues()) {
				Optional<HistogeneticsAppendixEntry> appendixEntry = findAppendixEntry(sampleCandidate.getSampleId(),
						locusValues.getLocus(), value, appendixEntries);
				if (appendixEntry.isEmpty()) {
					throw new GlStringConstructionException("Cannot build a GL String: " + locusValues.getLocus() + "*"
							+ value + " (sample " + sampleCandidate.getSampleId()
							+ ") has no matching appendix entry to expand into its full allele list. Candidate: "
							+ sampleCandidate);
				}

				sourceCandidates.add(appendixEntry.get());
				copyFragments.add(qualifyIncludedAlleles(locusValues.getLocus(), appendixEntry.get()));
			}

			locusFragments.add(String.join(GLStringConstants.GENE_COPY_DELIMITER, copyFragments));
		}

		String glString = String.join(GLStringConstants.GENE_DELIMITER, locusFragments);
		GlStringValidation.requireValid(glString);
		return new ConstructedGlString(glString, sourceCandidates);
	}

	// The appendix's "Included Alleles" text has no locus prefix at all (e.g.
	// "13:01:01:01/13:01:01:02/..."), unlike Versiti's footnote resolutions (which
	// already carry a locus-prefixed first member) -- so this always needs the locus
	// prepended before GLStringUtilities.fullyQualifyGLString() can qualify it.
	private String qualifyIncludedAlleles(Locus locus, HistogeneticsAppendixEntry appendixEntry) {
		String raw = locus.getFullName() + GLStringConstants.ASTERISK + appendixEntry.getIncludedAlleles();
		return GLStringUtilities.fullyQualifyGLString(raw);
	}

	private Optional<HistogeneticsAppendixEntry> findAppendixEntry(String sampleId, Locus locus, String reportedValue,
			List<HistogeneticsAppendixEntry> appendixEntries) {
		return appendixEntries.stream().filter(entry -> Objects.equals(sampleId, entry.getSampleId())
				&& entry.getLocus() == locus && reportedValue.equals(entry.getReportedValue())).findFirst();
	}
}
