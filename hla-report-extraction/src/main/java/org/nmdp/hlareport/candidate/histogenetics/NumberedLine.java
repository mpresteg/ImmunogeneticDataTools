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

/**
 * One line of text paired with its 1-based line number in the *original* extracted
 * text it came from -- e.g. {@link HistogeneticsNoiseFilter#filterNoise(String)}'s
 * output. Needed because filtering removes lines, so position within the filtered
 * result no longer corresponds to position in the original document; without carrying
 * the original number along, a downstream candidate (see issue #50) would lose the
 * traceability {@code LocusResultCandidate}/{@code VersitiLocusResultCandidate} both
 * already provide.
 */
public class NumberedLine {
	private final int lineNumber;
	private final String text;

	public NumberedLine(int lineNumber, String text) {
		this.lineNumber = lineNumber;
		this.text = text;
	}

	/**
	 * @return the 1-based line number in the original (pre-filtering) extracted text
	 */
	public int getLineNumber() {
		return lineNumber;
	}

	public String getText() {
		return text;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof NumberedLine)) {
			return false;
		}
		NumberedLine that = (NumberedLine) other;
		return lineNumber == that.lineNumber && Objects.equals(text, that.text);
	}

	@Override
	public int hashCode() {
		return Objects.hash(lineNumber, text);
	}

	@Override
	public String toString() {
		return lineNumber + ": " + text;
	}
}
