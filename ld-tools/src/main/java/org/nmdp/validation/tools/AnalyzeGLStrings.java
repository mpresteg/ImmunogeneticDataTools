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
package org.nmdp.validation.tools;

import static org.dishevelled.compress.Readers.reader;
import static org.dishevelled.compress.Writers.writer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.dash.valid.LinkageDisequilibriumAnalyzer;
import org.dash.valid.Sample;
import org.dash.valid.freq.Frequencies;
import org.dash.valid.freq.HLAFrequenciesLoader;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.handler.CommonWellDocumentedFileHandler;
import org.dash.valid.handler.HaplotypePairFileHandler;
import org.dash.valid.handler.HaplotypePairWarningFileHandler;
import org.dash.valid.handler.LinkageDisequilibriumFileHandler;
import org.dash.valid.handler.LinkageWarningFileHandler;
import org.dash.valid.report.CommonWellDocumentedWriter;
import org.dash.valid.report.DetectedFindingsWriter;
import org.dash.valid.report.DetectedLinkageFindings;
import org.dash.valid.report.HaplotypePairWriter;
import org.dash.valid.report.LinkageDisequilibriumWriter;
import org.dash.valid.report.SamplesList;
import org.dash.valid.report.SummaryWriter;
import org.dishevelled.commandline.ArgumentList;
import org.dishevelled.commandline.CommandLine;
import org.dishevelled.commandline.CommandLineParseException;
import org.dishevelled.commandline.CommandLineParser;
import org.dishevelled.commandline.Switch;
import org.dishevelled.commandline.Usage;
import org.dishevelled.commandline.argument.BooleanArgument;
import org.dishevelled.commandline.argument.FileArgument;
import org.dishevelled.commandline.argument.FileSetArgument;
import org.dishevelled.commandline.argument.StringArgument;

/**
 * AnalyzeGLStrings
 *
 */
public class AnalyzeGLStrings implements Callable<Integer> {
	
    private final File inputFile;
    private final File outputFile;
    private String hladb;
    private final String freq;
    private final Boolean warnings;
    private final Set<File> frequencyFiles;
    private final File allelesFile;
    private static final String USAGE = "analyze-gl-strings [args]";


    /**
     * Analyze gl string using linkage disequilibrium frequencies
     *
     * @param inputFile input file, if any
     * @param outputFile output interpretation file, if any
     */
    public AnalyzeGLStrings(File inputFile, File outputFile, String hladb, String freq, Boolean warnings, Set<File> frequencyFiles, File allelesFile) {
        this.inputFile = inputFile;
        this.outputFile   = outputFile;
        this.hladb = hladb;
        this.freq = freq;
        this.warnings = warnings;
        this.frequencyFiles = frequencyFiles;
        this.allelesFile = allelesFile;
    }
    
    @Override
    public Integer call() throws Exception {
    	BufferedReader reader = reader(inputFile);
    	
    	runAnalysis(reader);
    	return 0;
    }

	public void runAnalysis(BufferedReader reader) throws IOException {
		List<Sample> samplesList;
		
	    	samplesList = performAnalysis(reader);
	    	
	    	writeOutput(samplesList);
	}

	public List<Sample> performAnalysis(BufferedReader reader) throws IOException {
		List<Sample> samplesList;
    	    	
    	if (frequencyFiles !=  null) {
    		HLAFrequenciesLoader.getInstance(frequencyFiles, allelesFile);
    	}
    	
    	System.setProperty(Frequencies.FREQUENCIES_PROPERTY, (freq != null) ? freq : "Inputted");
    	
    	if (hladb == null) hladb = GLStringConstants.LATEST_HLADB;
    	System.setProperty(GLStringConstants.HLADB_PROPERTY, hladb);
    	 
    	samplesList = LinkageDisequilibriumAnalyzer.analyzeGLStringFile(inputFile == null ? "STDIN" : inputFile.getName(), reader);
		return samplesList;
	}

	private void writeOutput(List<Sample> samplesList) throws IOException {
		PrintWriter writer = null;
	    	PrintWriter summaryWriter = null;
	    	PrintWriter pairWriter = null;
	    	PrintWriter pairWarningsWriter = null;
	    	PrintWriter linkageWriter = null;
	    	PrintWriter linkageWarningsWriter = null;
	    	PrintWriter nonCwdWriter = null;
	    	PrintWriter detectedFindingsWriter = null;
	    	
	    	boolean writeToDir = false;
	    	
	    	if (outputFile != null && outputFile.isDirectory()) {
	    		writeToDir = true;
	    		
	    		summaryWriter = writer(new File(outputFile + "/" + SummaryWriter.SUMMARY_XML_FILE), true);
	    		pairWriter = writer(new File(outputFile + "/" + HaplotypePairFileHandler.HAPLOTYPE_PAIRS_LOG), true);
	    		pairWarningsWriter = writer(new File(outputFile + "/" + HaplotypePairWarningFileHandler.HAPLOTYPE_PAIRS_WARNING_LOG), true);
	    		linkageWriter = writer(new File(outputFile + "/" + LinkageDisequilibriumFileHandler.LINKAGES_LOG), true);
	    		linkageWarningsWriter = writer(new File(outputFile + "/" + LinkageWarningFileHandler.LINKAGE_WARNINGS_LOG), true);
	    		nonCwdWriter = writer(new File(outputFile + "/" + CommonWellDocumentedFileHandler.NON_CWD_WARNINGS_LOG), true);
	    		detectedFindingsWriter = writer(new File(outputFile + "/" + DetectedFindingsWriter.DETECTED_FINDINGS_CSV), true);
	    	}
	    	else {
	    		writer = writer(outputFile, true);
	    	}
	    	
	    	SamplesList allSamples = new SamplesList();
	    	allSamples.setSamples(samplesList);
	    	String summaryFindings = SummaryWriter.formatDetectedLinkages(allSamples);
    	
		for (Sample sample : samplesList) {
			DetectedLinkageFindings findings = sample.getFindings();
    		if (warnings != null && warnings == Boolean.TRUE && !findings.hasAnomalies()) {
    			continue;
    		}
    		
        	if (writeToDir) {
        		//summaryWriter.write(SummaryWriter.formatDetectedLinkages(findings));
        		
        		if (sample.getFindings().hasAnomalies()) {
        			pairWarningsWriter.write(HaplotypePairWriter.formatDetectedLinkages(findings));
        			linkageWarningsWriter.write(LinkageDisequilibriumWriter.formatDetectedLinkages(findings));
        		}
        		else {
        			pairWriter.write(HaplotypePairWriter.formatDetectedLinkages(findings));
        			linkageWriter.write(LinkageDisequilibriumWriter.formatDetectedLinkages(findings));
        			nonCwdWriter.write(CommonWellDocumentedWriter.formatCommonWellDocumented(findings));
        			detectedFindingsWriter.write(DetectedFindingsWriter.formatDetectedFindings(findings));
        		}
        	}
        	else {
        		//writer.write(SummaryWriter.formatDetectedLinkages(findings));
        	}
        		
		}
    	
		if (writeToDir) {
			summaryWriter.write(summaryFindings);
			summaryWriter.close();
			pairWriter.close();
			pairWarningsWriter.close();
			linkageWriter.close();
			linkageWarningsWriter.close();
			nonCwdWriter.close();
			detectedFindingsWriter.close();
		}
		else {
			writer.write(summaryFindings);
			writer.close();
		}
	}

    /**
     * Main.
     *
     * @param args command line args
     */
    public static void main(final String[] args) {
        Switch about = new Switch("a", "about", "display about message");
        Switch help  = new Switch("h", "help", "display help message");
        FileArgument inputFile = new FileArgument("i", "input-file", "input file, default stdin", false);
        FileArgument outputFile   = new FileArgument("o", "output-location", "specify an output file or an existing directory, default stdout", false);
        StringArgument hladb = new StringArgument("v", "hladb-version", "HLA DB version (e.g. 3.19.0), default latest", false);
        StringArgument freq = new StringArgument("f", "frequencies", "Frequency Set (e.g. nmdp, nmdp-2007, wiki), default nmdp-2007", false);
        BooleanArgument warnings = new BooleanArgument("w", "warnings-only", "Only log warnings, default all GL String output", false);
        FileSetArgument frequencyFiles = new FileSetArgument("q", "frequency-file(s)", "frequency input files (comma separated), default nmdp-2007 five locus", false);
        FileArgument allelesFile = new FileArgument("l", "allele-file", "alleles known to have frequencies, default none", false);

        ArgumentList arguments  = new ArgumentList(about, help, inputFile, outputFile, hladb, freq, warnings, frequencyFiles, allelesFile);
        CommandLine commandLine = new CommandLine(args);

        AnalyzeGLStrings analyzeGLStrings = null;
        try
        {
            CommandLineParser.parse(commandLine, arguments);
            if (about.wasFound()) {
                About.about(System.out);
                System.exit(0);
            }
            if (help.wasFound()) {
                Usage.usage(USAGE, null, commandLine, arguments, System.out);
                System.exit(0);
            }
            
            analyzeGLStrings = new AnalyzeGLStrings(inputFile.getValue(), outputFile.getValue(), hladb.getValue(), freq.getValue(), warnings.getValue(), frequencyFiles.getValue(), allelesFile.getValue());
        }
        catch (CommandLineParseException | IllegalArgumentException e) {
            Usage.usage(USAGE, e, commandLine, arguments, System.err);
            System.exit(-1);
        }
        try {
            System.exit(analyzeGLStrings.call());
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
}
