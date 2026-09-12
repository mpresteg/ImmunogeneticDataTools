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
 * One row of Histogenetics' appendix table, fully accumulated: a locus, its reported
 * value (a G-code or, when no G-code is available, an NMDP code standing in for one --
 * see {@link #isGCode()}), the NMDP allele code column, the sequenced segment list, and
 * the complete "Included Alleles" list reassembled from however many physical lines it
 * spanned in the original report. See issue #50.
 *
 * Carries {@code sampleId} (from the nearest preceding {@code "Sample ID : ..."}
 * marker) because the appendix repeats its whole table once per sample (patient,
 * donor 1, donor 2, ...) -- without it, entries from different samples for the same
 * locus/value would be indistinguishable.
 */
public class HistogeneticsAppendixEntry {
	private final String sampleId;
	private final Locus locus;
	private final String reportedValue;
	private final String nmdpAlleleCode;
	private final String segmentsSequenced;
	private final String includedAlleles;
	private final String sourceLine;
	private final int lineNumber;

	/**
	 * @param sampleId          from the nearest preceding "Sample ID :" marker, or null
	 *                          if none was seen before this row (shouldn't happen in a
	 *                          well-formed report, but not assumed impossible)
	 * @param locus             the locus this row is for
	 * @param reportedValue     the row's own reported value (column 2) exactly as
	 *                          printed -- a G-code (e.g. "02:01:01G") or, when no G-code
	 *                          is available, an NMDP code (e.g. "02:DKCVG") per the
	 *                          report's own note #4
	 * @param nmdpAlleleCode    the NMDP Allele Code column (column 3) exactly as
	 *                          printed -- may equal reportedValue when reportedValue is
	 *                          already an NMDP code, or may be a genuinely distinct NMDP
	 *                          mapping when reportedValue is a real G-code
	 * @param segmentsSequenced the "Segment Sequenced" column's raw value, e.g. "1,2,3,4"
	 * @param includedAlleles   the full "Included Alleles" list, reassembled from
	 *                          however many physical lines it spanned, exactly as printed
	 *                          (slash-delimited, no separators added or removed)
	 * @param sourceLine        this row's header line, for traceability
	 * @param lineNumber        the 1-based line number of sourceLine in the ORIGINAL
	 *                          (pre-noise-filtering) extracted text -- see
	 *                          {@link NumberedLine}
	 */
	public HistogeneticsAppendixEntry(String sampleId, Locus locus, String reportedValue, String nmdpAlleleCode,
			String segmentsSequenced, String includedAlleles, String sourceLine, int lineNumber) {
		this.sampleId = sampleId;
		this.locus = locus;
		this.reportedValue = reportedValue;
		this.nmdpAlleleCode = nmdpAlleleCode;
		this.segmentsSequenced = segmentsSequenced;
		this.includedAlleles = includedAlleles;
		this.sourceLine = sourceLine;
		this.lineNumber = lineNumber;
	}

	public String getSampleId() {
		return sampleId;
	}

	public Locus getLocus() {
		return locus;
	}

	public String getReportedValue() {
		return reportedValue;
	}

	public String getNmdpAlleleCode() {
		return nmdpAlleleCode;
	}

	public String getSegmentsSequenced() {
		return segmentsSequenced;
	}

	public String getIncludedAlleles() {
		return includedAlleles;
	}

	public String getSourceLine() {
		return sourceLine;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	/**
	 * @return true if reportedValue is a real G-code (ends in a digit followed by "G",
	 *         per the report's own note #3: "Allele bearing suffix G"), false if it's
	 *         an NMDP-code fallback (e.g. "02:DKCVG", which ends in a letter followed by
	 *         "G" -- not the same shape). A convenience interpretation, not an
	 *         authoritative decode -- a reviewer can always check reportedValue's exact
	 *         text themselves.
	 */
	public boolean isGCode() {
		return reportedValue.length() >= 2
				&& reportedValue.charAt(reportedValue.length() - 1) == 'G'
				&& Character.isDigit(reportedValue.charAt(reportedValue.length() - 2));
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof HistogeneticsAppendixEntry)) {
			return false;
		}
		HistogeneticsAppendixEntry that = (HistogeneticsAppendixEntry) other;
		return lineNumber == that.lineNumber && Objects.equals(sampleId, that.sampleId) && locus == that.locus
				&& Objects.equals(reportedValue, that.reportedValue) && Objects.equals(nmdpAlleleCode, that.nmdpAlleleCode)
				&& Objects.equals(segmentsSequenced, that.segmentsSequenced)
				&& Objects.equals(includedAlleles, that.includedAlleles) && Objects.equals(sourceLine, that.sourceLine);
	}

	@Override
	public int hashCode() {
		return Objects.hash(sampleId, locus, reportedValue, nmdpAlleleCode, segmentsSequenced, includedAlleles,
				sourceLine, lineNumber);
	}

	@Override
	public String toString() {
		return "HistogeneticsAppendixEntry[sampleId=" + sampleId + ", locus=" + locus + ", reportedValue=" + reportedValue
				+ ", nmdpAlleleCode=" + nmdpAlleleCode + ", segmentsSequenced=" + segmentsSequenced
				+ ", includedAlleles=" + includedAlleles + ", lineNumber=" + lineNumber + "]";
	}
}
