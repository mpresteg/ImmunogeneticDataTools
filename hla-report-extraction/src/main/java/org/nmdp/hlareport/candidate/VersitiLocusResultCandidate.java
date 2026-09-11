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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.dash.valid.Locus;

/**
 * A single locus's typing result from a Versiti-style report, e.g. two lines reading
 * {@code "HLA-C High    C*01:01"} then {@code "C*07:04:01G  R1"}, where R1 resolves
 * elsewhere on the page to the underlying ambiguous alleles. See issue #42.
 *
 * Deliberately a distinct type from {@link LocusResultCandidate} rather than reusing it
 * with extra optional fields: Versiti's report carries real structure
 * (per-allele-call footnote resolution, a resolution-level descriptor like "High")
 * that CeGaT's simpler row shape has no equivalent for. Forcing one shared shape onto
 * both now, with only one real sample of each, would be guessing at what should
 * generalize before there's enough evidence to know -- see the module README's "Real
 * reports drive the grammar, not assumption" principle, applied to the candidate data
 * model itself and not just to detection logic.
 */
public class VersitiLocusResultCandidate {
	private final Locus locus;
	private final String resolutionDescriptor;
	private final List<FootnoteReferencedAlleleCall> alleleCalls;
	private final String sourceLine;
	private final int lineNumber;

	/**
	 * @param locus                the locus this result is for
	 * @param resolutionDescriptor the report's own resolution-level word for this
	 *                              result, e.g. "High" (from "HLA-C High") -- captured
	 *                              for reviewer context, not interpreted
	 * @param alleleCalls           one or two allele calls, in report order
	 * @param sourceLine            the first line of this locus block (for traceability)
	 * @param lineNumber            the 1-based line number of sourceLine
	 */
	public VersitiLocusResultCandidate(Locus locus, String resolutionDescriptor,
			List<FootnoteReferencedAlleleCall> alleleCalls, String sourceLine, int lineNumber) {
		this.locus = locus;
		this.resolutionDescriptor = resolutionDescriptor;
		this.alleleCalls = Collections.unmodifiableList(alleleCalls);
		this.sourceLine = sourceLine;
		this.lineNumber = lineNumber;
	}

	public Locus getLocus() {
		return locus;
	}

	public String getResolutionDescriptor() {
		return resolutionDescriptor;
	}

	public List<FootnoteReferencedAlleleCall> getAlleleCalls() {
		return alleleCalls;
	}

	public String getSourceLine() {
		return sourceLine;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	/**
	 * @return true if any allele call referenced a footnote marker that couldn't be
	 *         resolved -- see {@link FootnoteReferencedAlleleCall#hasUnresolvedFootnoteReference()}
	 */
	public boolean hasUnresolvedFootnoteReference() {
		return alleleCalls.stream().anyMatch(FootnoteReferencedAlleleCall::hasUnresolvedFootnoteReference);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof VersitiLocusResultCandidate)) {
			return false;
		}
		VersitiLocusResultCandidate that = (VersitiLocusResultCandidate) other;
		return lineNumber == that.lineNumber && locus == that.locus
				&& Objects.equals(resolutionDescriptor, that.resolutionDescriptor) && alleleCalls.equals(that.alleleCalls)
				&& sourceLine.equals(that.sourceLine);
	}

	@Override
	public int hashCode() {
		return Objects.hash(locus, resolutionDescriptor, alleleCalls, sourceLine, lineNumber);
	}

	@Override
	public String toString() {
		return "VersitiLocusResultCandidate[locus=" + locus + ", resolutionDescriptor=" + resolutionDescriptor
				+ ", alleleCalls=" + alleleCalls + ", lineNumber=" + lineNumber + ", sourceLine=" + sourceLine + "]";
	}
}
