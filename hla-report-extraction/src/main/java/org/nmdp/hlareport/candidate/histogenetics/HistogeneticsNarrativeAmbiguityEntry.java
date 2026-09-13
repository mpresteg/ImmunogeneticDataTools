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

import java.util.Objects;

import org.dash.valid.Locus;

/**
 * One locus's ambiguity, expressed as narrative prose in a Histogenetics report's
 * "Report History" section rather than as a table cell -- e.g.
 * {@code "Possible Allele in A : 01:01:01/02:01:01/30:01:01."}. See issue #51.
 *
 * A genuinely different notation from every other ambiguity representation this module
 * has seen: Versiti's footnote-resolved G-group, Histogenetics' own G-code-as-primary-
 * result (issue #49), and this -- bare slash-separated allele designations (no locus
 * prefix, no "*") inside a free-text sentence. The possibleAlleles list can also be
 * empty (e.g. {@code "Possible Allele in DRB3 :  ."} in the real fixture, meaning no
 * candidate alleles at all for that locus) -- surfaced as an empty string, not treated
 * as a parse failure.
 */
public class HistogeneticsNarrativeAmbiguityEntry {
	private final String sampleId;
	private final Locus locus;
	private final String possibleAlleles;
	private final String sourceLine;
	private final int lineNumber;

	/**
	 * @param sampleId        from the nearest preceding "Sample Id :" field in this
	 *                        Report History entry, or null if none was seen
	 * @param locus           the locus this narrative sentence is about
	 * @param possibleAlleles the raw slash-separated allele list exactly as printed
	 *                        (e.g. "01:01:01/02:01:01/30:01:01"), or an empty string if
	 *                        none were listed
	 * @param sourceLine      the full report line this entry was read from
	 * @param lineNumber      the 1-based line number of sourceLine in the ORIGINAL
	 *                        (pre-noise-filtering) extracted text
	 */
	public HistogeneticsNarrativeAmbiguityEntry(String sampleId, Locus locus, String possibleAlleles, String sourceLine,
			int lineNumber) {
		this.sampleId = sampleId;
		this.locus = locus;
		this.possibleAlleles = possibleAlleles;
		this.sourceLine = sourceLine;
		this.lineNumber = lineNumber;
	}

	public String getSampleId() {
		return sampleId;
	}

	public Locus getLocus() {
		return locus;
	}

	public String getPossibleAlleles() {
		return possibleAlleles;
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
		if (!(other instanceof HistogeneticsNarrativeAmbiguityEntry)) {
			return false;
		}
		HistogeneticsNarrativeAmbiguityEntry that = (HistogeneticsNarrativeAmbiguityEntry) other;
		return lineNumber == that.lineNumber && Objects.equals(sampleId, that.sampleId) && locus == that.locus
				&& Objects.equals(possibleAlleles, that.possibleAlleles) && Objects.equals(sourceLine, that.sourceLine);
	}

	@Override
	public int hashCode() {
		return Objects.hash(sampleId, locus, possibleAlleles, sourceLine, lineNumber);
	}

	@Override
	public String toString() {
		return "HistogeneticsNarrativeAmbiguityEntry[sampleId=" + sampleId + ", locus=" + locus + ", possibleAlleles="
				+ possibleAlleles + ", lineNumber=" + lineNumber + "]";
	}
}
