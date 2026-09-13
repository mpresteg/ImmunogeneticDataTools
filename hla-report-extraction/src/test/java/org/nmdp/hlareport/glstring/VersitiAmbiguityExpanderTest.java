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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class VersitiAmbiguityExpanderTest {
	@Test
	public void testExpandsTheRealFootnoteFromTheVersitiSampleReport() {
		// The actual real case, confirmed correct by the user's own domain knowledge:
		// "07:04/11" means "07:04 or 07:11" (the shared leading field "07" is dropped
		// from the shorthand member, not restated the way ld-validation's own
		// shorthandExamples.txt convention always does).
		assertEquals("HLA-C*07:04/HLA-C*07:11",
				VersitiAmbiguityExpander.expand("C*07:04:01G = HLA-C*07:04:01G=C*07:04/11"));
	}

	@Test
	public void testExpandsMultipleShorthandMembers() {
		// Not from a real report yet -- exercises the general rule (keep leading
		// fields not supplied by the shorthand, append the shorthand's own fields)
		// against more than one shorthand member in the same list.
		assertEquals("HLA-A*02:01/HLA-A*02:05/HLA-A*02:10", VersitiAmbiguityExpander.expand("=A*02:01/05/10"));
	}

	@Test
	public void testShorthandMemberCanSupplyMoreThanOneField() {
		assertEquals("HLA-A*02:01:01/HLA-A*02:01:05", VersitiAmbiguityExpander.expand("=A*02:01:01/01:05"));
	}

	@Test
	public void testReturnsNullWhenShorthandSuppliesMoreFieldsThanTheReferenceHas() {
		// "02:01" has 2 fields; a 3-field shorthand member has nothing safe to keep
		// from -- refuse to guess rather than silently truncate the reference.
		assertNull(VersitiAmbiguityExpander.expand("=A*02:01/01:02:03"));
	}

	@Test
	public void testReturnsNullWhenThereIsNoCompleteReferenceMemberAtAll() {
		assertNull(VersitiAmbiguityExpander.expand("=11"));
	}

	@Test
	public void testReturnsNullForTextWithNoEqualsSignAtAll() {
		assertNull(VersitiAmbiguityExpander.expand("C*07:04/11"));
	}
}
