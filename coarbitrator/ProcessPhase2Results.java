/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *    
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *    
 */

/*
 *    ProcessPhase2Results.java
 *    Copyright (C) 2018 Philip Heller
 *    
 */

package coarbitrator;

import java.util.*;
import java.util.stream.Collectors;
import java.io.*;


//
// Processes rps blast result file. This file contains hits for multiple queries, and each query can
// have up to 10 hits. Assumes queries are unique across all the blast result files; ProcessPhase1Results
// ensures that this is so.
//


public class ProcessPhase2Results 
{
	private final static double			SUPERIORITY_THRESHOLD		= 0.9;
	private final static File			DFLT_RPS_BLAST_OUT_CSV		= new File("data/rpsblast_out.csv");
	private final static File			BACKUP_RPS_BLAST_OUT_CSV	= new File("rpsblast_out.csv");
	private final static File			DFLT_FINAL_OUTPUT_CSV		= new File("data/coarbitrator_out.csv");
	private final static File			BACKUP_FINAL_OUTPUT_CSV		= new File("coarbitrator_out.csv");
	
	
	private static void collectAcceptedQueries(File rpsoutf, Collection<String> acceptedQueries) throws IOException
	{
		try
		(
			FileReader fr = new FileReader(rpsoutf);
			BufferedReader br = new BufferedReader(fr);
			ReversibleBufferedReader rbr = new ReversibleBufferedReader(br);
			ConserverDomainReportReader cdrr = new ConserverDomainReportReader(rbr);
		)
		{
			ConservedDomainReport report;
			while ((report = cdrr.readReport()) != null)
			{
				report.classifyForSuperiorityThreshold(SUPERIORITY_THRESHOLD);
				if (report.isCOI())
					acceptedQueries.add(report.getQuery());
			}
		}
	}
	
	
	private static void sop(Object x)
	{
		System.out.println(x);
	}
	
	

	public static void main(String[] args)
	{
		List<String> acceptedQueries = new ArrayList<>();
		File bloutf = DFLT_RPS_BLAST_OUT_CSV.exists()  ?  DFLT_RPS_BLAST_OUT_CSV  :  BACKUP_RPS_BLAST_OUT_CSV;
		if (bloutf == DFLT_RPS_BLAST_OUT_CSV)
			sop("START");
		try
		{
			// Query format is e.g. ARO47330.1_KY263006, where 1st part is protein acc and 2nd part is nuc acc.
			collectAcceptedQueries(bloutf, acceptedQueries);
		}
		catch (IOException x)
		{
			sop("Trouble reading rpsblast output file " + bloutf.getAbsolutePath());
		}
		File finalOutf = DFLT_RPS_BLAST_OUT_CSV.exists()  ?  DFLT_FINAL_OUTPUT_CSV  :  BACKUP_FINAL_OUTPUT_CSV;
		if (finalOutf == DFLT_FINAL_OUTPUT_CSV)
			sop("Writing final output csv");
		try
		(
				FileWriter fw = new FileWriter(finalOutf);
		)
		{
			fw.write("Protein accession #, Coded by nucleotide accession #\n");
			for (String query: acceptedQueries)
				fw.write(query.replace("_", ",") + "\n");
		}
		catch (IOException x)
		{
			sop("Trouble writing output file: " + x.getMessage());
			System.exit(1);
		}
		sop("DONE");
	}
}
