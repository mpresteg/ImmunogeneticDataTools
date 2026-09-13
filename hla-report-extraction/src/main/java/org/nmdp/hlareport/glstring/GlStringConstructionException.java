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

/**
 * Thrown when a GL String builder has real candidates to work with but cannot safely
 * convert one of them -- e.g. a footnote reference that was never resolved (see
 * {@link org.nmdp.hlareport.candidate.FootnoteReferencedAlleleCall#hasUnresolvedFootnoteReference()}),
 * or ambiguity shorthand {@link VersitiAmbiguityExpander} doesn't recognize.
 *
 * Deliberately an exception, not a null/empty return: a candidate list that's missing
 * one locus's worth of real information is a fundamentally different situation from "no
 * candidates were detected at all" (the latter returns an empty list elsewhere in this
 * module, correctly). Silently producing a GL String short one locus -- or an empty one
 * -- would be far more dangerous than refusing outright, since a reviewer has no way to
 * tell "correctly complete" apart from "silently incomplete" just by looking at the
 * result. See the module's "structural signal, not a content guess" principle.
 */
public class GlStringConstructionException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public GlStringConstructionException(String message) {
		super(message);
	}
}
