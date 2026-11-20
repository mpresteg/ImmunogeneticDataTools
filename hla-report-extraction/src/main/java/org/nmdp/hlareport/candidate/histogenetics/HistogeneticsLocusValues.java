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
 * One locus's reported value(s) from Histogenetics' page-1 table -- one or two
 * G-code/NMDP-code tokens (see {@link HistogeneticsCode}), in report order. A locus
 * with no code-shaped value in either of the table's two rows (e.g. reported as "NA")
 * simply doesn't get one of these -- see issue #49.
 */
public class HistogeneticsLocusValues {
	private final Locus locus;
	private final List<String> values;

	public HistogeneticsLocusValues(Locus locus, List<String> values) {
		this.locus = locus;
		this.values = Collections.unmodifiableList(values);
	}

	public Locus getLocus() {
		return locus;
	}

	/**
	 * @return one or two G-code/NMDP-code tokens exactly as reported, in report order
	 */
	public List<String> getValues() {
		return values;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof HistogeneticsLocusValues)) {
			return false;
		}
		HistogeneticsLocusValues that = (HistogeneticsLocusValues) other;
		return locus == that.locus && values.equals(that.values);
	}

	@Override
	public int hashCode() {
		return Objects.hash(locus, values);
	}

	@Override
	public String toString() {
		return locus + "=" + values;
	}
}
