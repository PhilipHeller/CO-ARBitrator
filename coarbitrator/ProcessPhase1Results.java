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
 *    ProcessPhase1Results.java
 *    Copyright (C) 2018 Philip Heller
 *    
 */

package coarbitrator;

import java.io.*;
import java.util.*;


public class ProcessPhase1Results 
{
	private final static double		QUALITY_THRESHOLD	= 5;
	private final static File		PHASE_1_BLOUTF		= new File("blastp_out.csv");
	private final static File  		PHASE_2_QUERY_FASTA = new File("phase_2_query.faa");
	
	
	private static Collection<String> collectProtAccs()
	{
		Set<String> protAccs = new HashSet<>();
		
		try
		(
				FileReader fr = new FileReader(PHASE_1_BLOUTF);
				BufferedReader br = new BufferedReader(fr);
				TabularBlastReader tbr = new TabularBlastReader(br);
		)
		{
			TabularBlastHit hit;
			while ((hit = tbr.readBlastHit()) != null)
			{
				if (hit.e <= QUALITY_THRESHOLD)
					protAccs.add(hit.subject);
			}
		}
		catch (IOException x)
		{
			sop("Trouble reading blastn output file " + PHASE_1_BLOUTF.getAbsolutePath() + ":");
			sop(x.getMessage());
			System.exit(10);
		}
		return protAccs;
	}
	
	
	private static void writePhase2QueryFasta(Collection<String> protAccs)
	{
		int nRecs = 0;
		String fileName = "";
		try (FileWriter fw = new FileWriter(PHASE_2_QUERY_FASTA))
		{
			for (String protAcc: protAccs)
			{
				nRecs++;
				ProteinProperties protProps = new ProteinProperties(protAcc);
				try
				{
					sop("Phase I  looking up rec " + nRecs + " of " + protAccs.size() + ": " + protAcc);
					protProps.initFromNCBI();
				}
				catch (IOException | ConversionException x)
				{
					sop("   ... Trouble fetching for protein accession " + protAcc + ": " + x.getMessage());
					continue;
				}
				if (!protProps.animal)
				{
					sop("   ... Reject: not Metazoan");
					continue;
				}
				String protSeq = protProps.seq;
				if (protSeq == null)			
				{
					sop("   ... Reject: can't retrieve aa sequence");
					continue;
				}
				if (protSeq.length() < 95)			
				{
					sop("   ... Reject: sequence too short (" + protSeq.length() + " aa)");
					continue;
				}
				sop("   ... ok");
				String nucAcc = protProps.nucacc;
				if (nucAcc == null)
					nucAcc = "na";
				String defline = ">" + protAcc + "_" + nucAcc;
				try
				{
					fw.write(defline + "\n" + protProps.seq + "\n");
				}
				catch (IOException x)
				{
					sop("Couldn't write to output fasta " + PHASE_2_QUERY_FASTA + ":\n" + x.getMessage());
					System.exit(1);
				}
			}
		}
		catch (IOException x)
		{
			sop("Couldn't create output fasta " + PHASE_2_QUERY_FASTA + ":\n" + x.getMessage());
			System.exit(1);
		}
	}
	
	
	private static void sop(Object x)
	{
		System.out.println(x);
	}
	
	
	public static void main(String[] args)
	{		
		Collection<String> protAccs = collectProtAccs();
		writePhase2QueryFasta(protAccs);
	}
}
