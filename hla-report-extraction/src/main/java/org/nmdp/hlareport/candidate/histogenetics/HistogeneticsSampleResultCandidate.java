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
package org.nmdp.hlareport.candidate.histogenetics;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * One sample's (patient, donor 1, donor 2, ...) page-1 result block from a Histogenetics
 * report: per-locus values plus the block-level fields that go with them. See issue #49.
 *
 * Kept as one candidate per sample block, not one per locus (unlike CeGaT/Versiti):
 * the source document itself binds all of a sample's loci together in one 3-line table
 * (a header row plus 2 data rows, columns aligned by position -- see
 * {@code HistogeneticsPageOneTableDetector}), and {@code Matching Ratio}/
 * {@code Null Allele Resolution Status} are properties of the whole sample, not any one
 * locus. Splitting into per-locus candidates would mean either duplicating those
 * block-level fields onto every locus or discarding them -- this mirrors the source
 * structure instead.
 */
public class HistogeneticsSampleResultCandidate {
	private final String sampleId;
	private final List<HistogeneticsLocusValues> locusValues;
	private final String typingStatusText;
	private final String matchingRatio;
	private final String nullAlleleResolutionStatus;
	private final String sourceLine;
	private final int lineNumber;

	/**
	 * @param sampleId                   from the nearest preceding "Histo ID :" field,
	 *                                    or null if none was seen before this block
	 * @param locusValues                one entry per locus that had at least one
	 *                                    code-shaped value -- a locus reported as "NA"
	 *                                    (or, on a non-"Complete" report, a placeholder
	 *                                    like FAILED/PENDING/XXXX) is simply absent, not
	 *                                    included with an empty value list
	 * @param typingStatusText            the raw "Typing Status :" field text. Captured
	 *                                    for transparency, not trusted as a success
	 *                                    signal -- confirmed against the real fixture
	 *                                    that this literally reads "Complete" on reports
	 *                                    where every locus is FAILED or XXXX too
	 * @param matchingRatio               the raw "Matching Ratio with Patient :" field
	 *                                    (donor blocks only), or null if absent
	 * @param nullAlleleResolutionStatus  the raw "Null Allele Resolution Status :" field,
	 *                                    reassembled across however many lines it
	 *                                    wrapped, or null if absent
	 * @param sourceLine                  this block's locus-header row (e.g.
	 *                                    "A* B* C* DRB1* DRB345* DQB1* DQA1* DPB1* DPA1*"),
	 *                                    for traceability
	 * @param lineNumber                  the 1-based line number of sourceLine in the
	 *                                    ORIGINAL (pre-noise-filtering) extracted text
	 */
	public HistogeneticsSampleResultCandidate(String sampleId, List<HistogeneticsLocusValues> locusValues,
			String typingStatusText, String matchingRatio, String nullAlleleResolutionStatus, String sourceLine,
			int lineNumber) {
		this.sampleId = sampleId;
		this.locusValues = Collections.unmodifiableList(locusValues);
		this.typingStatusText = typingStatusText;
		this.matchingRatio = matchingRatio;
		this.nullAlleleResolutionStatus = nullAlleleResolutionStatus;
		this.sourceLine = sourceLine;
		this.lineNumber = lineNumber;
	}

	public String getSampleId() {
		return sampleId;
	}

	public List<HistogeneticsLocusValues> getLocusValues() {
		return locusValues;
	}

	public String getTypingStatusText() {
		return typingStatusText;
	}

	public String getMatchingRatio() {
		return matchingRatio;
	}

	public String getNullAlleleResolutionStatus() {
		return nullAlleleResolutionStatus;
	}

	public String getSourceLine() {
		return sourceLine;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof HistogeneticsSampleResultCandidate)) {
			return false;
		}
		HistogeneticsSampleResultCandidate that = (HistogeneticsSampleResultCandidate) other;
		return lineNumber == that.lineNumber && Objects.equals(sampleId, that.sampleId)
				&& locusValues.equals(that.locusValues) && Objects.equals(typingStatusText, that.typingStatusText)
				&& Objects.equals(matchingRatio, that.matchingRatio)
				&& Objects.equals(nullAlleleResolutionStatus, that.nullAlleleResolutionStatus)
				&& Objects.equals(sourceLine, that.sourceLine);
	}

	@Override
	public int hashCode() {
		return Objects.hash(sampleId, locusValues, typingStatusText, matchingRatio, nullAlleleResolutionStatus,
				sourceLine, lineNumber);
	}

	@Override
	public String toString() {
		return "HistogeneticsSampleResultCandidate[sampleId=" + sampleId + ", locusValues=" + locusValues
				+ ", typingStatusText=" + typingStatusText + ", matchingRatio=" + matchingRatio
				+ ", nullAlleleResolutionStatus=" + nullAlleleResolutionStatus + ", lineNumber=" + lineNumber + "]";
	}
}
