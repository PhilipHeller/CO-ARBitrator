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
 *    ConservedDomainReport.java
 *    Copyright (C) 2018 Philip Heller
 *    
 */

package coarbitrator;

import java.util.*;
import java.util.stream.Collectors;
import java.sql.*;
import java.io.*;


//
// Keys are Conserved Domain pssm ids (e.g. "cd01663"). Values are RPSBLAST e-values. Computes superiority 
// and classifies.
//


public class ConservedDomainReport extends LinkedHashMap<String, Double>
{
	private final static double					MAX_SUPERIORITY		= 180;
	private final static File					DFLT_CD_FILE = new File("data/cds.csv");
	private final static File					BACKUP_CD_FILE = new File("cds.csv");
	
	private String								query;
	private Map<Double, EchelonType> 			expectToEchelonType;
	private Map<Double, Echelon> 				expectToEchelon;
	private Double								superiorityBound;
	private Boolean								call;

	
	// Keys are cd names e.g. cd00387 or codes e.g. 238833. Codes are irritating values returned
	// by terse-format local rpsblasts. They correspond 1-1 to cds, and hopefully I've manually
	// identified the ones that matter.
	private static Map<String, ConservedDomainCOIness>
												ID_TO_COINESS;
	
	private static CdPssmidToAccAndShortName 	PSSM_MAP;

	
	static 
	{
		try
		{
			// Accession -> ConservedDomainCOIness
			ID_TO_COINESS = new TreeMap<>();
			File ifile = DFLT_CD_FILE.exists()  ?  DFLT_CD_FILE  :  BACKUP_CD_FILE;
			try
			(
				FileReader fr = new FileReader(ifile);
				BufferedReader br = new BufferedReader(fr);
			)
			{
				String line;
				while ((line = br.readLine()) != null)
				{
					if (line.trim().isEmpty())
						continue;
					String[] pieces = line.split(",");
					assert pieces.length == 2  :  line;
					ID_TO_COINESS.put(pieces[0].trim(), ConservedDomainCOIness.valueOf(pieces[1].trim()));
				}
			}
			
			// Pssm id -> ConservedDomainCOIness
			PSSM_MAP = new CdPssmidToAccAndShortName();
			for (String pssm: PSSM_MAP.keySet())
			{
				String acc = PSSM_MAP.getAccession(pssm);
				if (ID_TO_COINESS.containsKey(acc))
					ID_TO_COINESS.put(pssm, ID_TO_COINESS.get(acc));
			}
		}
		catch (IOException x)
		{
			sop("Couldn't initialize conserved domain tables: " + x.getMessage());
			System.exit(1);
		}
	}
	
	
	private static ConservedDomainCOIness getCOIness(String cd)
	{
		if (cd.toLowerCase().startsWith("cl"))
			return ConservedDomainCOIness.UNINFORMATIVE;
		
		else if (ID_TO_COINESS.containsKey(cd))
			return ID_TO_COINESS.get(cd);
		
		// 12/4/16 - 209 conserved domains have been manually classified, including all top hits. Of the
		// remainder, 698 appear as cd2, 515 as cd3, 1439 as cd4, and 2206 as cd5. All of the last 40 cds
		// that I checked were NEGATIVE and I'm convinced I've found all the POSITIVEs (i.e. COIs). So
		// any unknown cd will be henceforth classified as NEGATIVE, and every cd will get a classification.
		// 7/21/17 - More confirmation of above.
		else 
			return ConservedDomainCOIness.NEGATIVE;
	}
	
	
	ConservedDomainReport() { }
	
	
	ConservedDomainReport(String query)
	{
		if (query.startsWith("MIDORI"))				// MIDORI_n_U36794_p_U36794  oops, nuc field copied into prot field
			query = query.split("_")[2];
		this.query = query;
	}
	
	
	// For local rpsblasting, all subjects are prefixed by "CDD:";
	public Double put(String subj, Double evalue)
	{
		if (subj.startsWith("CDD:"))
			subj = subj.substring(4);
		return super.put(subj, evalue);
	}
	
	
	public Boolean getCall()
	{
		return call;
	}
	
	
	public boolean isCOI()
	{
		return call != null  &&  call == true;
	}
	
	
	public Double getSuperiorityBound()
	{
		return superiorityBound;
	}
	
	
	public String toString()
	{
		String s = "\nAccession = " + query + "\n";
		for (String pssm: keySet())
		{
			s += pssm;
			if (PSSM_MAP.containsKey(pssm))
				s += "=" + PSSM_MAP.getAccession(pssm);
			s += " (" + getCOIness(pssm) + ") ";
			s += " evalue = " + get(pssm) + "\n";
		}
		if (superiorityBound != null)
			s += "SUPERIORITY = " + superiorityBound;
		return s;
	}
	
	
	private class Echelon extends ArrayList<String>
	{
		public String toString()
		{
			String s = "Echelon:";
			for (String pssm: this)
				s += " " + pssm + "=" + PSSM_MAP.getAccession(pssm);
			return s;
		}
	}
	
	
	private enum EchelonType
	{
		POSITIVE, NEGATIVE, UNKNOWN, UNINFORMATIVE;
		
		
		// Echelon is a collection of cds.
		static EchelonType forEchelon(Collection<String> echelon)
		{
			// Count by COIness.
			HashBinCounter<ConservedDomainCOIness> ctr = new HashBinCounter<>();
			for (String cd: echelon)
			{
				ConservedDomainCOIness ness = getCOIness(cd);
				assert ness != null;
				ctr.bumpCountForBin(ness);	
			}
			
			// No known cds.
			if (ctr.isEmpty())
				return UNKNOWN;
			
			// Homogeneous.
			else if (ctr.size() == 1)
			{
				ConservedDomainCOIness ness = ctr.keySet().iterator().next();
				return EchelonType.valueOf(ness.toString());
			}
		
			// Mixed.
			boolean havePositive = ctr.getCountForBin(ConservedDomainCOIness.POSITIVE) > 0;
			boolean haveNegative = ctr.getCountForBin(ConservedDomainCOIness.NEGATIVE) > 0;
			if (havePositive  &&  !haveNegative)
				return POSITIVE;
			else if (haveNegative  &&  !havePositive)
				return NEGATIVE;
			else
				return UNINFORMATIVE;		
		} 
		
		
		static EchelonType positiveByNonMinority(Collection<String> echelon)
		{
			int nPosCDs = 0;
			int nNegCDs = 0;
			for (String cd: echelon)
			{
				ConservedDomainCOIness ness = getCOIness(cd);
				if (ness == ConservedDomainCOIness.POSITIVE)
					nPosCDs++;
				else if (ness == ConservedDomainCOIness.NEGATIVE)
					nNegCDs++;
			}
			
			return (nPosCDs >= 1  &&  nNegCDs <= 1)  ?  POSITIVE  :  UNINFORMATIVE;
		}
	} // EchelonType
	
	
	//
	// With a known superiority threshold, certain shortcuts are possible. In particular, it isn't
	// necessary to classify every conserved domain found by RPS blast.
	//
	// Throws if needs classification for a CD.
	//
	// Returns this report to support classifying in midstream.
	//
	public ConservedDomainReport classifyForSuperiorityThreshold(double superiorityThresh)
	{
		if (isEmpty())
		{
			call = false;
			return this;
		}
				
		// Collect cds into echelons by e-value. An echelon is all hits with the same e-value. Echelon size will 
		// almost always be 1.
		expectToEchelon = new TreeMap<>();
		for (String cd: keySet())
		{
			Double e = get(cd);
			Echelon members = expectToEchelon.get(e);
			if (members == null)
			{
				members = new Echelon();
				expectToEchelon.put(e, members);
			}
			members.add(cd);
		}
		
		// Classify echelons as POSITIVE, NEGATIVE, UNKNOWN, or UNINFORMATIVE.
		expectToEchelonType = new TreeMap<>();
		for (Double e: expectToEchelon.keySet())
		{
			Echelon echelon = expectToEchelon.get(e);
			EchelonType type = EchelonType.forEchelon(echelon);
			expectToEchelonType.put(e, type);
			// Special case: if single echelon has strong e-value, classify by majority rule.
			if (expectToEchelonType.size() == 1  &&  type == EchelonType.UNINFORMATIVE  &&  e < 1.0E-50)
			{
				type = EchelonType.positiveByNonMinority(echelon);	// might change to POSITIVE
				expectToEchelonType.put(e, type);
			}
		}
		
		// Convert UNKNOWN to NEGATIVE. With high probability, all relevant CDs have been identified so
		// remainder are NEGATIVE.
		Set<Double> esOfUnknown =
			expectToEchelonType.keySet()
			.stream()
			.filter(e -> expectToEchelonType.get(e) == EchelonType.UNKNOWN)
			.collect(Collectors.toSet());
		esOfUnknown
			.stream()
			.forEach(e -> expectToEchelonType.put(e, EchelonType.NEGATIVE));
		
		// Remove UNINFORMATIVE and UNKNOWN echelons from top of list.
		while (expectToEchelonType.size() > 1)
		{
			Double bestSurvivingE = expectToEchelonType.keySet().iterator().next();
			EchelonType typeOfBestSurvivor = expectToEchelonType.get(bestSurvivingE);
			if (typeOfBestSurvivor == EchelonType.UNINFORMATIVE  ||  typeOfBestSurvivor == EchelonType.UNKNOWN)
			{
				expectToEchelonType.remove(bestSurvivingE);
				expectToEchelon.remove(bestSurvivingE);
			}
			else
				break;
		}
		assert expectToEchelonType.size() == expectToEchelon.size();	
		
		// Special case: No echelons.
		if (expectToEchelonType.isEmpty())
		{
			call = false;
			return this;
		}

		// Special case: 1 echelon.
		Double eOfBestEchelon = expectToEchelon.keySet().iterator().next();
		Echelon bestEchelon = expectToEchelon.get(eOfBestEchelon);
		if (expectToEchelonType.size() == 1)
		{
			superiorityBound = (eOfBestEchelon == 0)  ?  MAX_SUPERIORITY  :  -Math.log10(eOfBestEchelon);
			if (superiorityBound < superiorityThresh)
			{
				// Insufficient superiority.
				call = false;
				return this;
			}
			EchelonType type = expectToEchelonType.get(eOfBestEchelon);
			switch (type)
			{
				case POSITIVE:
					call = true;
					return this;
				case NEGATIVE:
					superiorityBound = -superiorityBound;
					call = false;
					return this;
				case UNINFORMATIVE:
					// Accept if mostly positive and very strong e-value.
					call = superiorityBound > 25  &&  
						   EchelonType.positiveByNonMinority(bestEchelon) == EchelonType.POSITIVE;
					if (call == false)
						superiorityBound = -MAX_SUPERIORITY;
					return this;
				case UNKNOWN:
					call = null;
					superiorityBound = null;
					assert false : "Unknown echelon type ... all echelons chould have known CDs.";
					return this;
				default:
					assert false;
					return this;
			}			
		}
		
		// Special case: multiple echelons, 1 echelon type.
		TreeBinCounter<EchelonType> echelonTypeCtr = 
				expectToEchelonType.values()
				.stream()
				.collect(LocalCollectors.toTreeBinCounter());
		if (echelonTypeCtr.size() == 1)
		{
			List<Double> expects = new ArrayList<>(expectToEchelonType.keySet());
			Double worstExpect = expects.get(expects.size()-1);
			double supeBound = toSuperiority(eOfBestEchelon, worstExpect);
			EchelonType bestEchelonType = expectToEchelonType.get(eOfBestEchelon);
			switch (bestEchelonType)
			{
				case POSITIVE:
					call = supeBound >= superiorityThresh;
					superiorityBound = supeBound;
					return this;
				case NEGATIVE:
				case UNINFORMATIVE:
					call = false;
					superiorityBound = -supeBound;
					return this;
				case UNKNOWN:
					assert false : "Unknown echelon type ... all echelons chould have known CDs.";
					return this;
				default:
					assert false;
					return this;
			}
		}
		
		// General case: multiple echelons, multiple types. Top echelon is POSITIVE or NEGATIVE; reject if 
		// it's NEGATIVE or not sufficiently POSITIVE.
		assert expectToEchelon.size() > 1;
		EchelonType bestEchelonType = expectToEchelonType.get(eOfBestEchelon);
		assert bestEchelonType == EchelonType.POSITIVE  ||  bestEchelonType == EchelonType.NEGATIVE   :  
			"Unexpected echelon type " + bestEchelonType + " for gi = " + query;
		List<Double> expectsBelowBest = new ArrayList<>(expectToEchelon.keySet());
		expectsBelowBest.remove(0);
		if (bestEchelonType == EchelonType.NEGATIVE)
		{
			// Top echelon is negative.
			superiorityBound = -toSuperiority(eOfBestEchelon, expectsBelowBest.get(0));
			call = false;
		}
		else
		{
			// Top echelon is positive. Remove all echelons except POSITIVEs and NEGATIVEs, compute superiority, and classify.
			assert bestEchelonType == EchelonType.POSITIVE;
			Set<Double> expectsToRemove =
				expectToEchelonType.keySet()
				.stream()
				.filter(e -> expectToEchelonType.get(e) != EchelonType.POSITIVE  &&  
				             expectToEchelonType.get(e) != EchelonType.NEGATIVE)
				.collect(Collectors.toSet());
			expectsToRemove
				.stream()
				.forEach(e -> { expectToEchelon.remove(e); expectToEchelonType.remove(e); } );
			long nNegatives = 
				expectToEchelonType.values()
				.stream()
				.filter(et -> et == EchelonType.NEGATIVE)
				.count();
			if (nNegatives == 0)
			{
				// No negatives. Derive superiority from e-value of top echelon (which is positive).
				superiorityBound = (eOfBestEchelon != 0)  ?  -Math.log10(eOfBestEchelon)  :  MAX_SUPERIORITY;
			}
			else
			{
				// At least 1 negative. Derive superiority from e-values of best positive and best negative.
				Double eOfBestNegativeEchelon = null;
				for (Double e: expectToEchelonType.keySet())
				{
					if (expectToEchelonType.get(e) == EchelonType.NEGATIVE)
					{
						eOfBestNegativeEchelon = e;
						break;
					}
				}
				assert eOfBestNegativeEchelon != null;
				superiorityBound = toSuperiority(eOfBestEchelon, eOfBestNegativeEchelon);
			}
			assert superiorityBound > 0;
			call = superiorityBound >= superiorityThresh;
		}

		return this;
	}
	
	
	private static double toSuperiority(double betterE, double worseE)
	{
		assert betterE < worseE  :  "Expected " + betterE + " to be < " + worseE;
		assert worseE >= 0;

		if (betterE == 0)
			betterE = 1.0e-200;
		return Math.log10(worseE) - Math.log10(betterE);
	}
	
	
	public String getQuery()
	{
		return query;
	}
	
	
	static void sop(Object x)		{ System.out.println(x); }
	
	
	public static void main(String[] args) throws Exception
	{
		sop("START");
		File f = new File("data/rpsblast_out.csv");
		try
		(
				FileReader fr = new FileReader(f);
				BufferedReader br = new BufferedReader(fr);
				ReversibleBufferedReader rbr = new ReversibleBufferedReader(br);
				ConserverDomainReportReader cdr = new ConserverDomainReportReader(rbr);
		)
		{
			ConservedDomainReport report;
			while ((report = cdr.readReport()) != null)
			{
				sop("Classifying " + report.getQuery());
				report.classifyForSuperiorityThreshold(0.9);
				sop("... " + report.isCOI());
			}
		}
		
		sop("DONE");
	}
}
