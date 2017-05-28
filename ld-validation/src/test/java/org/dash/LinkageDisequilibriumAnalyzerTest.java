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
package org.dash;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;

import org.dash.valid.HLALinkageDisequilibrium;
import org.dash.valid.LinkageDisequilibriumAnalyzer;
import org.dash.valid.Linkages;
import org.dash.valid.LinkagesLoader;
import org.dash.valid.Locus;
import org.dash.valid.freq.Frequencies;
import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.dash.valid.gl.haplo.Haplotype;
import org.dash.valid.report.DetectedLinkageFindings;
import org.junit.Ignore;
import org.junit.Test;
import org.nmdp.gl.MultilocusUnphasedGenotype;

import junit.framework.TestCase;

public class LinkageDisequilibriumAnalyzerTest extends TestCase {	
	@Test
	public void testLinkageReportingExamples() {
		LinkagesLoader.getInstance(Linkages.lookup(Locus.C_B_LOCI));
		LinkageDisequilibriumAnalyzer.main(new String[] {"contrivedExamples.txt", "fullyQualifiedExample.txt", "strictExample.txt", "hml_1_0_2-example7-ngsFull.xml"});//, "shorthandExamples.txt"});
	}
	
	@Test
	public void testLinkageReportingMugs() throws IOException {
		LinkedHashMap<String, String> glStrings = GLStringUtilities.readGLStringFile("fullyQualifiedExample.txt");
		
		for (String key : glStrings.keySet()) {
			MultilocusUnphasedGenotype mug = GLStringUtilities.convertToMug(glStrings.get(key));
			
			assertNotNull(mug);
			
			DetectedLinkageFindings findings = LinkageDisequilibriumAnalyzer.detectLinkages(mug);
			
			assertNotNull(findings);
		}
	}
	
	@Test
	public void testPhasedGenotypeList() throws IOException {
		System.setProperty(Frequencies.FREQUENCIES_PROPERTY, Frequencies.NMDP.getShortName());
		String fullyQualified = GLStringUtilities.fullyQualifyGLString("HLA-A*24:02:01:01~HLA-C*04:01:01:06~HLA-B*35:02:01~HLA-DRB3*02:02:01:02~HLA-DRB1*11:01:01:01~HLA-DQA1*05:05:01:01/HLA-DQA1*05:05:01:02~HLA-DQB1*03:01:01:03~HLA-DPA1*01:03:01:01~HLA-DPB1*05:01:01+HLA-A*11:01:01:01~HLA-C*12:03:01:01~HLA-B*35:03:01~HLA-DRB3*02:02:01:01~HLA-DRB1*13:01:01:01/HLA-DRB1*13:01:01:02~HLA-DQA1*01:03:01:02~HLA-DQB1*06:03:01~HLA-DPA1*02:01:01:01~HLA-DPB1*13:01:01/HLA-DPB1*107:01");
		
		List<Haplotype> knownHaplotypes = GLStringUtilities.buildHaplotypes(fullyQualified);
		LinkageDisequilibriumGenotypeList glString = new LinkageDisequilibriumGenotypeList("SBCFMW0003", fullyQualified);
		
		DetectedLinkageFindings findings = HLALinkageDisequilibrium.hasLinkageDisequilibrium(glString, knownHaplotypes);
		
		assertNotNull(findings);
		
		//HaplotypePairWriter.getInstance().reportDetectedLinkages(findings);
	}
	
//	@Test
//	@Ignore
//	public void testGetFrequencies() throws MalformedURLException, ProtocolException, IOException {
//		String uri = "http://hfcus-dash6.b12x.org:8080/hfc";
//			URL url = new URL(uri);
//			HttpURLConnection connection =
//			    (HttpURLConnection) url.openConnection();
//			connection.setRequestMethod("GET");
//			connection.setRequestProperty("Accept", "application/json");
//
//			//JAXBContext jc = JAXBContext.newInstance(Customer.class);
//			InputStream xml = connection.getInputStream();
//			BufferedReader reader = new BufferedReader(new InputStreamReader(xml));
//			String line;
//			while ((line = reader.readLine()) != null) {
//				System.out.println(line);
//			}
//			//Customer customer =
//			//    (Customer) jc.createUnmarshaller().unmarshal(xml);
//
//			connection.disconnect();
//	}
	
	@Test
	public void testLinkageReportingInlineGLString() throws IOException {				
		//String fullyQualified = GLStringUtilities.fullyQualifyGLString("HLA-A*11:01:01+HLA-A*24:02:01:01/HLA-A*24:02:01:02L/HLA-A*24:02:01:03^HLA-B*18:01:01:01/HLA-B*18:01:01:02/HLA-B*18:51+HLA-B*53:01:01^HLA-C*04:01:01:01/HLA-C*04:01:01:02/HLA-C*04:01:01:03/HLA-C*04:01:01:04/HLA-C*04:01:01:05/HLA-C*04:20/HLA-C*04:117+HLA-C*12:03:01:01/HLA-C*12:03:01:02/HLA-C*12:34^HLA-DPA1*01:03:01:01/HLA-DPA1*01:03:01:02/HLA-DPA1*01:03:01:03/HLA-DPA1*01:03:01:04/HLA-DPA1*01:03:01:05+HLA-DPA1*02:01:01^HLA-DPB1*02:01:02+HLA-DPB1*09:01^HLA-DQA1*01:02:01:01/HLA-DQA1*01:02:01:02/HLA-DQA1*01:02:01:03/HLA-DQA1*01:02:01:04/HLA-DQA1*01:11+HLA-DQA1*03:01:01^HLA-DQB1*03:05:01+HLA-DQB1*06:09^HLA-DRB1*11:04:01+HLA-DRB1*13:02:01^HLA-DRB3*02:02:01:01/HLA-DRB3*02:02:01:02+HLA-DRB3*03:01:01");
		
		String fullyQualified = GLStringUtilities.fullyQualifyGLString("HLA-A*24:02:01:01~HLA-C*04:01:01:06~HLA-B*35:02:01~HLA-DRB3*02:02:01:02~HLA-DRB1*11:01:01:01~HLA-DQA1*05:05:01:01/HLA-DQA1*05:05:01:02~HLA-DQB1*03:01:01:03~HLA-DPA1*01:03:01:01~HLA-DPB1*05:01:01+HLA-A*11:01:01:01~HLA-C*12:03:01:01~HLA-B*35:03:01~HLA-DRB3*02:02:01:01~HLA-DRB1*13:01:01:01/HLA-DRB1*13:01:01:02~HLA-DQA1*01:03:01:02~HLA-DQB1*06:03:01~HLA-DPA1*02:01:01:01~HLA-DPB1*13:01:01/HLA-DPB1*107:01");
		LinkageDisequilibriumGenotypeList genotypeList = new LinkageDisequilibriumGenotypeList("SBCFMW0003", fullyQualified);
		//MultilocusUnphasedGenotype mug = GLStringUtilities.convertToMug(fullyQualified);
		//DetectedLinkageFindings findings = LinkageDisequilibriumAnalyzer.detectLinkages(mug);
		
		DetectedLinkageFindings findings = LinkageDisequilibriumAnalyzer.detectLinkages(genotypeList);
		
		assertNotNull(findings);	
	}
}
