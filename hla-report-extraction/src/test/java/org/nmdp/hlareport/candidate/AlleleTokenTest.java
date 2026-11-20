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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class AlleleTokenTest {
	@Test
	public void testMatchesRealAllelesSeenAcrossTheThreeSampleReports() {
		// CeGaT: two-field
		assertTrue(AlleleToken.matches("A*24:02"));
		assertTrue(AlleleToken.matches("DRB4*01:03"));
		// Versiti: G-group, three-field-plus-suffix
		assertTrue(AlleleToken.matches("C*01:01"));
		assertTrue(AlleleToken.matches("C*07:04:01G"));
		// Histogenetics appendix: four-field
		assertTrue(AlleleToken.matches("A*02:01:01:01"));
	}

	@Test
	public void testDoesNotMatchNonAlleleTokens() {
		assertFalse(AlleleToken.matches("HLA-Locus"));
		assertFalse(AlleleToken.matches("Allele"));
		// Histogenetics G-code/NMDP-code tokens have no locus prefix before the "*" at
		// all (no "*" present) -- correctly not allele-shaped by this module's
		// definition, which is specific to the "LOCUS*field:field..." shape used by
		// Versiti and CeGaT.
		assertFalse(AlleleToken.matches("02:01:01G"));
		assertFalse(AlleleToken.matches("02:DMFHE"));
		// One field only -- below GLStringUtilities' own two-field floor
		assertFalse(AlleleToken.matches("A*24"));
		assertFalse(AlleleToken.matches(""));
		assertFalse(AlleleToken.matches(null));
	}

	@Test
	public void testLocusPrefix() {
		assertEquals("A", AlleleToken.locusPrefix("A*24:02"));
		assertEquals("DRB4", AlleleToken.locusPrefix("DRB4*01:03"));
		assertNull(AlleleToken.locusPrefix("no-asterisk-here"));
	}
}
