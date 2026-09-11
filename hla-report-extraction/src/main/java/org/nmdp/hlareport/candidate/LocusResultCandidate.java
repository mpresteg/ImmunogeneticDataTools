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
 * A single locus's typing result, as detected in a report's raw extracted text --
 * deliberately NOT a GL String, and deliberately not yet trusted. Per the module
 * README's guiding principles, this is a candidate for human review, carrying its
 * source line and line number along so a reviewer (or a later automated check) can
 * trace it straight back to the report text it came from rather than taking this
 * class's interpretation on faith.
 *
 * One allele call (not two) is a legitimate result, not a malformed one -- either a
 * homozygous locus, or (for a combined locus like DRB345) only one of the constituent
 * genes being present in this subject. See issue #43.
 */
public class LocusResultCandidate {
	private final Locus locus;
	private final List<String> alleleCalls;
	private final String sourceLine;
	private final int lineNumber;

	public LocusResultCandidate(Locus locus, List<String> alleleCalls, String sourceLine, int lineNumber) {
		this.locus = locus;
		this.alleleCalls = Collections.unmodifiableList(alleleCalls);
		this.sourceLine = sourceLine;
		this.lineNumber = lineNumber;
	}

	public Locus getLocus() {
		return locus;
	}

	/**
	 * @return the allele call(s) exactly as they appeared in the report text, in
	 *         report order. One element for a homozygous or single-gene-present
	 *         result, otherwise (so far) two.
	 */
	public List<String> getAlleleCalls() {
		return alleleCalls;
	}

	/**
	 * @return the full report line this candidate was detected on, for a human
	 *         reviewer to check this candidate's interpretation against
	 */
	public String getSourceLine() {
		return sourceLine;
	}

	/**
	 * @return the 1-based line number within the report's extracted text
	 */
	public int getLineNumber() {
		return lineNumber;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof LocusResultCandidate)) {
			return false;
		}
		LocusResultCandidate that = (LocusResultCandidate) other;
		return lineNumber == that.lineNumber && locus == that.locus && alleleCalls.equals(that.alleleCalls)
				&& sourceLine.equals(that.sourceLine);
	}

	@Override
	public int hashCode() {
		return Objects.hash(locus, alleleCalls, sourceLine, lineNumber);
	}

	@Override
	public String toString() {
		return "LocusResultCandidate[locus=" + locus + ", alleleCalls=" + alleleCalls + ", lineNumber=" + lineNumber
				+ ", sourceLine=" + sourceLine + "]";
	}
}
