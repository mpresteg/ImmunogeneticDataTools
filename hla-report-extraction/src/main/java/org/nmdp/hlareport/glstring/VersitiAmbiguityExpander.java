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

import org.dash.valid.gl.GLStringConstants;

/**
 * Expands Versiti's footnote ambiguity shorthand (e.g. {@code "C*07:04/11"}, meaning
 * "C*07:04 or C*07:11") into a fully-qualified GL-String ambiguity fragment (e.g.
 * {@code "HLA-C*07:04/HLA-C*07:11"}). See issue #45.
 *
 * This is a genuinely different shorthand convention from the one
 * {@code ld-validation}'s own {@code GLStringUtilities.fullyQualifyGLString()} already
 * handles -- confirmed by reading {@code ld-validation}'s own
 * {@code shorthandExamples.txt} test fixture, where a shared leading field is always
 * restated on every ambiguity member (e.g. {@code "B*38:01:01/38:27"}: {@code "38"}
 * appears on both sides, never dropped). Versiti's convention drops the shared leading
 * field(s) too, keeping only the diverging trailing field(s) -- confirmed against the
 * real sample with the user's own domain knowledge: {@code "C*07:04/11"} means
 * {@code "C*07:04 or C*07:11"}, not a standalone {@code "C*11"} (which is what
 * {@code fullyQualifyGLString()} produces if handed this text, since it was never built
 * for this convention). That's why this is new interpretation logic scoped to this
 * module rather than a change to the shared {@code ld-validation} utility, which
 * remains correct for the convention it already serves.
 *
 * The general rule (confirmed for one real case; not yet tested against more): the
 * first ambiguity member is a complete, standalone allele designation. Each following
 * member supplies only its own trailing field(s); the expansion keeps the reference
 * member's leading fields for however many are NOT supplied, then appends the
 * shorthand's own fields. For {@code "07:04"} (2 fields) with shorthand {@code "11"}
 * (1 field): keep the first {@code 2 - 1 = 1} leading field ({@code "07"}), append
 * {@code "11"} -> {@code "07:11"}.
 */
public final class VersitiAmbiguityExpander {
	private VersitiAmbiguityExpander() {
	}

	/**
	 * @param rawFootnoteText the full footnote resolution text exactly as extracted,
	 *                        e.g. {@link org.nmdp.hlareport.candidate.FootnoteReferencedAlleleCall#getResolvedAlleles()}
	 *                        (e.g. {@code "C*07:04:01G = HLA-C*07:04:01G=C*07:04/11"})
	 * @return the fully-qualified, "/"-joined ambiguity fragment (e.g.
	 *         {@code "HLA-C*07:04/HLA-C*07:11"}), or null if the text doesn't match the
	 *         shape this expander knows how to parse, or a shorthand member supplies
	 *         more fields than the reference member has (nothing to safely keep) --
	 *         never a guess, per the module's "structural signal, not a content guess"
	 *         principle
	 */
	public static String expand(String rawFootnoteText) {
		int lastEquals = rawFootnoteText.lastIndexOf('=');
		if (lastEquals < 0 || lastEquals == rawFootnoteText.length() - 1) {
			return null;
		}
		String shorthandList = rawFootnoteText.substring(lastEquals + 1).trim();

		String[] members = shorthandList.split(GLStringConstants.ALLELE_AMBIGUITY_DELIMITER);
		if (members.length == 0) {
			return null;
		}

		String locus = null;
		String[] referenceFields = null;
		StringBuilder expanded = new StringBuilder();

		for (int i = 0; i < members.length; i++) {
			String member = members[i];
			int asteriskIndex = member.indexOf(GLStringConstants.ASTERISK);

			String[] fields;
			if (asteriskIndex >= 0) {
				// A complete, standalone member -- establishes (or re-establishes) the
				// reference locus/fields subsequent bare-field members inherit from.
				locus = member.substring(0, asteriskIndex);
				fields = member.substring(asteriskIndex + 1).split(":");
				referenceFields = fields;
			} else {
				if (locus == null || referenceFields == null) {
					// A bare-field member with no earlier complete member to inherit
					// from -- can't safely resolve what locus/leading fields it means.
					return null;
				}
				String[] shorthandFields = member.split(":");
				int keepCount = referenceFields.length - shorthandFields.length;
				if (keepCount < 0) {
					// The shorthand supplies more fields than the reference has --
					// nothing to safely keep from. Not expected from real data seen so
					// far; refuse to guess rather than truncate the reference.
					return null;
				}
				fields = new String[referenceFields.length];
				System.arraycopy(referenceFields, 0, fields, 0, keepCount);
				System.arraycopy(shorthandFields, 0, fields, keepCount, shorthandFields.length);
			}

			if (i > 0) {
				expanded.append(GLStringConstants.ALLELE_AMBIGUITY_DELIMITER);
			}
			expanded.append(GLStringConstants.HLA_DASH).append(locus).append(GLStringConstants.ASTERISK)
					.append(String.join(":", fields));
		}

		return expanded.toString();
	}
}
