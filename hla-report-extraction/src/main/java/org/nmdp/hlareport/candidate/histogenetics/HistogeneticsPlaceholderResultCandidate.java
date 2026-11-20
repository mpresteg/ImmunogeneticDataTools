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

import org.dash.valid.Locus;

/**
 * One sample's page-1 table where every (or some) locus reads a placeholder word
 * instead of a real G-code/NMDP-code -- e.g. every locus is literally {@code "FAILED"}
 * (insufficient DNA) or {@code "PENDING"} (not yet resulted). See issue #51.
 *
 * This is deliberately surfaced as its own candidate type, not silently treated as "0
 * candidates" the way {@link HistogeneticsPageOneTableDetector} correctly does for a
 * report shape it doesn't recognize at all -- a FAILED or PENDING result is real,
 * important information a reviewer needs to see, not indistinguishable from "nothing
 * here understood this report."
 */
public class HistogeneticsPlaceholderResultCandidate {
	private final String sampleId;
	private final String placeholder;
	private final List<Locus> affectedLoci;
	private final String typingStatusText;
	private final String sourceLine;
	private final int lineNumber;

	/**
	 * @param sampleId          from the nearest preceding "Histo ID :" field, or null
	 *                          if none was seen before this block
	 * @param placeholder       the literal placeholder word found (e.g. "FAILED",
	 *                          "PENDING", "XXXX")
	 * @param affectedLoci      which loci actually showed this placeholder -- in every
	 *                          real sample seen so far, all 9, but not assumed
	 * @param typingStatusText  the raw "Typing Status :" field text, captured for
	 *                          context (e.g. PENDING's spells out exactly which loci are
	 *                          pending) -- see {@link HistogeneticsPageOneTableDetector}
	 *                          on why this field alone is never trusted as a success/
	 *                          failure signal
	 * @param sourceLine        this block's locus-header row, for traceability
	 * @param lineNumber        the 1-based line number of sourceLine in the ORIGINAL
	 *                          (pre-noise-filtering) extracted text
	 */
	public HistogeneticsPlaceholderResultCandidate(String sampleId, String placeholder, List<Locus> affectedLoci,
			String typingStatusText, String sourceLine, int lineNumber) {
		this.sampleId = sampleId;
		this.placeholder = placeholder;
		this.affectedLoci = Collections.unmodifiableList(affectedLoci);
		this.typingStatusText = typingStatusText;
		this.sourceLine = sourceLine;
		this.lineNumber = lineNumber;
	}

	public String getSampleId() {
		return sampleId;
	}

	public String getPlaceholder() {
		return placeholder;
	}

	public List<Locus> getAffectedLoci() {
		return affectedLoci;
	}

	public String getTypingStatusText() {
		return typingStatusText;
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
		if (!(other instanceof HistogeneticsPlaceholderResultCandidate)) {
			return false;
		}
		HistogeneticsPlaceholderResultCandidate that = (HistogeneticsPlaceholderResultCandidate) other;
		return lineNumber == that.lineNumber && Objects.equals(sampleId, that.sampleId)
				&& Objects.equals(placeholder, that.placeholder) && affectedLoci.equals(that.affectedLoci)
				&& Objects.equals(typingStatusText, that.typingStatusText) && Objects.equals(sourceLine, that.sourceLine);
	}

	@Override
	public int hashCode() {
		return Objects.hash(sampleId, placeholder, affectedLoci, typingStatusText, sourceLine, lineNumber);
	}

	@Override
	public String toString() {
		return "HistogeneticsPlaceholderResultCandidate[sampleId=" + sampleId + ", placeholder=" + placeholder
				+ ", affectedLoci=" + affectedLoci + ", typingStatusText=" + typingStatusText + ", lineNumber=" + lineNumber
				+ "]";
	}
}
