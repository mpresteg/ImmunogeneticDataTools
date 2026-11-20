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

import java.util.Objects;

/**
 * One allele call from a Versiti-style report, optionally annotated with a footnote
 * marker (e.g. {@code "R1"}) that resolves it to its underlying ambiguous alleles
 * elsewhere on the page. See issue #42.
 *
 * The three footnote-related fields are deliberately independent rather than a single
 * "resolved" flag: {@code footnoteMarker} present with {@code resolvedAlleles} null
 * means a marker was seen on the allele-call line but no matching footnote definition
 * could be found for it elsewhere in the report -- a real, distinct situation a human
 * reviewer needs to see (something referenced but not resolved), not the same as "no
 * footnote was ever referenced." Never silently drop that distinction -- see the module
 * README's "Structural signal, not a content guess" principle.
 */
public class FootnoteReferencedAlleleCall {
	private final String alleleCall;
	private final String footnoteMarker;
	private final String resolvedAlleles;
	private final int footnoteLineNumber;

	/**
	 * @param alleleCall         the allele call exactly as reported, e.g. "C*07:04:01G"
	 * @param footnoteMarker     the footnote marker referenced on the allele-call line
	 *                           (e.g. "R1"), or null if this allele call carried no
	 *                           footnote reference at all
	 * @param resolvedAlleles    the footnote's resolution content exactly as reported
	 *                           (e.g. "C*07:04:01G = HLA-C*07:04:01G=C*07:04/11"), or
	 *                           null if footnoteMarker is null, or if footnoteMarker is
	 *                           non-null but no matching footnote definition was found
	 * @param footnoteLineNumber the 1-based line number the resolution text came from,
	 *                           or -1 if resolvedAlleles is null
	 */
	public FootnoteReferencedAlleleCall(String alleleCall, String footnoteMarker, String resolvedAlleles,
			int footnoteLineNumber) {
		this.alleleCall = alleleCall;
		this.footnoteMarker = footnoteMarker;
		this.resolvedAlleles = resolvedAlleles;
		this.footnoteLineNumber = footnoteLineNumber;
	}

	public String getAlleleCall() {
		return alleleCall;
	}

	public String getFootnoteMarker() {
		return footnoteMarker;
	}

	public String getResolvedAlleles() {
		return resolvedAlleles;
	}

	public int getFootnoteLineNumber() {
		return footnoteLineNumber;
	}

	/**
	 * @return true if a footnote marker was seen on the allele-call line but no
	 *         matching footnote definition could be located -- a candidate in this
	 *         state still gets surfaced (never silently dropped), but a reviewer needs
	 *         to know the ambiguity this marker was supposed to resolve is missing.
	 */
	public boolean hasUnresolvedFootnoteReference() {
		return footnoteMarker != null && resolvedAlleles == null;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof FootnoteReferencedAlleleCall)) {
			return false;
		}
		FootnoteReferencedAlleleCall that = (FootnoteReferencedAlleleCall) other;
		return footnoteLineNumber == that.footnoteLineNumber && Objects.equals(alleleCall, that.alleleCall)
				&& Objects.equals(footnoteMarker, that.footnoteMarker)
				&& Objects.equals(resolvedAlleles, that.resolvedAlleles);
	}

	@Override
	public int hashCode() {
		return Objects.hash(alleleCall, footnoteMarker, resolvedAlleles, footnoteLineNumber);
	}

	@Override
	public String toString() {
		if (footnoteMarker == null) {
			return alleleCall;
		}
		if (resolvedAlleles == null) {
			return alleleCall + " [" + footnoteMarker + ": UNRESOLVED -- no matching footnote found]";
		}
		return alleleCall + " [" + footnoteMarker + ": " + resolvedAlleles + "]";
	}
}
