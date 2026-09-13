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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A GL String built from one or more reviewed candidates -- step 3 of the module
 * README's "Then" plan (issue #45). This is the module's final output shape, shared
 * across whichever lab's candidates it was built from: the README's "Output contract"
 * fixes this shape (a GL String, plus an explicit human review/confirmation step)
 * independent of which lab produced the underlying candidates, unlike the candidate
 * types themselves, which are deliberately kept lab-specific until real convergence is
 * shown (see the module README's "How tethered is this to the 3 known reports?"
 * section). sourceCandidates is therefore {@code List<?>} rather than any one
 * candidate type -- traceability across whichever builder produced this.
 *
 * Deliberately carries no "reviewed" flag: nothing in this module ever flips one, so a
 * mutable field nothing sets would just be a false promise. The human review step this
 * class's own existence assumes happens outside this code entirely -- a person reading
 * this GL string (e.g. via the CLI, which prints it with the same "not validated"
 * framing every candidate already gets) and deciding, as a separate action, whether to
 * trust it as input to ld-validation/ld-tools/ld-service.
 */
public class ConstructedGlString {
	private final String glString;
	private final List<?> sourceCandidates;

	public ConstructedGlString(String glString, List<?> sourceCandidates) {
		this.glString = glString;
		this.sourceCandidates = Collections.unmodifiableList(sourceCandidates);
	}

	public String getGlString() {
		return glString;
	}

	/**
	 * @return the candidate(s) this GL String was built from, for traceability back to
	 *         the report text a reviewer would need to check it against
	 */
	public List<?> getSourceCandidates() {
		return sourceCandidates;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ConstructedGlString)) {
			return false;
		}
		ConstructedGlString that = (ConstructedGlString) other;
		return Objects.equals(glString, that.glString) && sourceCandidates.equals(that.sourceCandidates);
	}

	@Override
	public int hashCode() {
		return Objects.hash(glString, sourceCandidates);
	}

	@Override
	public String toString() {
		return "ConstructedGlString[glString=" + glString + ", sourceCandidates=" + sourceCandidates + "]";
	}
}
