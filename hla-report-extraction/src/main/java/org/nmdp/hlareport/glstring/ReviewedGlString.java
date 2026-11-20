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

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;

/**
 * Step 4 of the module README's "Then" plan (issue #46): the gate a
 * {@link ConstructedGlString} must pass through before it's "reviewed and ready" for
 * downstream consumption by {@code ld-validation}/{@code ld-tools}/{@code ld-service}.
 *
 * A real, type-enforced gate, not a boolean flag on {@code ConstructedGlString} someone
 * could set without actually reviewing anything: the only way to get an instance of
 * this class is {@link #confirm(ConstructedGlString, String)}, which requires an
 * explicit, non-blank reviewer identity -- nothing anywhere in this module ever calls
 * it automatically. That mirrors the module README's "No demographic/PHI
 * auto-population without explicit human review" principle, applied here to the GL
 * String itself, not just patient context: no code path in this module can mark its own
 * output reviewed, only a named human action can. The CLI deliberately does NOT call
 * this -- it only ever prints a bare {@code ConstructedGlString} with a "NOT validated"
 * label, because a non-interactive tool can't actually review anything; the review
 * itself has to happen outside this code, by a person reading that output and checking
 * it against the source report.
 *
 * {@link #confirm(ConstructedGlString, String)} also re-checks
 * {@link GLStringUtilities#validateGLStringFormat(String)} at confirmation time, on top
 * of the same check each builder already runs before returning a
 * {@code ConstructedGlString} at all (defense in depth: a human confirming a GL string
 * by eye is reviewing whether it matches the report, not re-deriving GL String syntax
 * rules, so this class doesn't rely on that alone to catch a syntactically malformed
 * string). A structurally invalid string is refused here even if a reviewer claims it's
 * correct -- correctness of content is a human judgment call this class trusts once
 * given; well-formedness of syntax is not a judgment call at all, so it's still checked.
 *
 * {@link #toLinkageDisequilibriumGenotypeList(String)} is the actual point of this
 * class existing: it's the only place in this module that constructs one, and it's only
 * reachable through a confirmed review. A bare {@code ConstructedGlString} has no
 * equivalent method -- there's no path from "just constructed" straight into
 * {@code ld-validation}'s own analysis code without going through this gate first.
 */
public final class ReviewedGlString {
	private final ConstructedGlString constructedGlString;
	private final String reviewedBy;
	private final Instant reviewedAt;

	private ReviewedGlString(ConstructedGlString constructedGlString, String reviewedBy, Instant reviewedAt) {
		this.constructedGlString = constructedGlString;
		this.reviewedBy = reviewedBy;
		this.reviewedAt = reviewedAt;
	}

	/**
	 * @param constructedGlString the GL String to confirm as reviewed
	 * @param reviewedBy          who is confirming it -- required, and not defaulted or
	 *                             inferred from anything, since the whole point is that
	 *                             a real person is vouching for this specific GL string
	 * @return a {@link ReviewedGlString} wrapping constructedGlString
	 * @throws IllegalArgumentException    if constructedGlString is null, or reviewedBy
	 *                                      is null or blank
	 * @throws GlStringConstructionException if constructedGlString's GL string text
	 *                                      isn't valid per
	 *                                      {@link GLStringUtilities#validateGLStringFormat(String)}
	 */
	public static ReviewedGlString confirm(ConstructedGlString constructedGlString, String reviewedBy) {
		if (constructedGlString == null) {
			throw new IllegalArgumentException("Cannot confirm a null ConstructedGlString as reviewed.");
		}
		if (reviewedBy == null || reviewedBy.isBlank()) {
			throw new IllegalArgumentException(
					"A reviewer must be identified to confirm a GL String as reviewed -- this is never inferred or"
							+ " defaulted, since the whole point of this gate is an explicit human action.");
		}
		if (!GLStringUtilities.validateGLStringFormat(constructedGlString.getGlString())) {
			throw new GlStringConstructionException(
					"Refusing to mark as reviewed: not valid per GLStringUtilities.validateGLStringFormat(): "
							+ constructedGlString.getGlString());
		}

		return new ReviewedGlString(constructedGlString, reviewedBy, Instant.now());
	}

	public String getGlString() {
		return constructedGlString.getGlString();
	}

	/**
	 * @return the candidate(s) this GL String was built from, for traceability back to
	 *         the report text a reviewer would need to check it against
	 */
	public List<?> getSourceCandidates() {
		return constructedGlString.getSourceCandidates();
	}

	public String getReviewedBy() {
		return reviewedBy;
	}

	public Instant getReviewedAt() {
		return reviewedAt;
	}

	/**
	 * @param id an identifier for this genotype list -- not auto-populated from any
	 *           extracted report text (same PHI principle as reviewedBy above), the
	 *           caller supplies it explicitly
	 * @return a real {@code ld-validation} object, ready for LD/haplotype analysis
	 */
	public LinkageDisequilibriumGenotypeList toLinkageDisequilibriumGenotypeList(String id) {
		return new LinkageDisequilibriumGenotypeList(id, getGlString());
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ReviewedGlString)) {
			return false;
		}
		ReviewedGlString that = (ReviewedGlString) other;
		return constructedGlString.equals(that.constructedGlString) && reviewedBy.equals(that.reviewedBy)
				&& reviewedAt.equals(that.reviewedAt);
	}

	@Override
	public int hashCode() {
		return Objects.hash(constructedGlString, reviewedBy, reviewedAt);
	}

	@Override
	public String toString() {
		return "ReviewedGlString[glString=" + getGlString() + ", reviewedBy=" + reviewedBy + ", reviewedAt=" + reviewedAt
				+ "]";
	}
}
