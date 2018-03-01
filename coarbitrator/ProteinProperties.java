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
 *    ProteinProperties.java
 *    Copyright (C) 2018 Philip Heller
 *    
 */

package coarbitrator;

import java.io.*;
import java.util.*;


public class ProteinProperties 
{
	public String 			acc;			// var names match col names in accprops table
	public String 			nucacc;
	public int				len;
	public String			anno;
	public String			binomial;
	public String			organism;
	public boolean 			bold;
	public boolean			proke;
	public String 			seq;
	public boolean 			animal;
	private boolean			verbosePage;
	
	
	public ProteinProperties(String acc)
	{
		this.acc = acc;
	}
	
	
	public void initFromNCBI() throws IOException, ConversionException
	{
		String page = ProteinGIPageFetch.getProteinGPPage(acc);
		
		if (verbosePage)
			sop(page);
				
		len = extractSequenceLength(page);
		nucacc = extractNucleotideAccession(page);
		anno = extractDefinitionField(page);
		anno = anno.replace("'", "");
		anno = anno.replace("\"", "");
		String[] binomialAndOrganism = extractBinomialAndOrganismField(page);
		binomial = binomialAndOrganism[0];
		organism = binomialAndOrganism[1];
		bold = extractBoldness(page);
		seq = extractSequence(page);
		if (organism.startsWith("Eukaryot")  ||  organism.startsWith("Eucaryot"))
			proke = false;
		else if (organism.startsWith("Bacteria")  ||  organism.startsWith("Prokaryot")  ||  organism.startsWith("Procaryot"))
			proke = true;
		String orgUC = organism.toUpperCase();
		animal = orgUC.contains("ANIMALIA;")  ||  orgUC.contains("METAZOA;");
	}
	
	
	private static String extractNucleotideAccession(String page) throws IOException
	{
		if (!page.contains("/coded_by"))
			return null;
		
		String s = null;
		try
		(
			StringReader sr = new StringReader(page);
			BufferedReader br = new BufferedReader(sr);
		)
		{
			String line;
			while (!(line = br.readLine().trim()).startsWith("/coded_by="));
			int nStart = line.indexOf('"') + 1;
			int nPastEnd = line.indexOf('.');
			s = line.substring(nStart, nPastEnd);
			
			String[] rarities = { "complement(join(", "complement(", "join(" };
			for (String rarity: rarities)
			{
				if (s.startsWith(rarity))
				{
					s = s.substring(rarity.length());
					break;
				}
			}
			
			return s;
		}
	}
	
	
	private static String[] MONTHS_ARR = { "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC" };
	private static Map<String, Integer> MONTH_TO_INT = new LinkedHashMap<>();
	static
	{
		int n = 1;
		for (String m: MONTHS_ARR)
			MONTH_TO_INT.put(m, n++);
	}
	
	
	private static Integer[] extractYearMonthDay(String page)
	{
		try
		(
			StringReader sr = new StringReader(page);
			BufferedReader br = new BufferedReader(sr);
		)
		{
			String line;
			while ((line = br.readLine()) != null  &&  !line.trim().startsWith("LOCUS"))
				;
			if (line == null)  
				return null; 
			// LOCUS       NC_011814              19527 bp    DNA     circular VRT 07-JAN-2009
			line = line.trim();
			String ymds = line.substring(line.length()-11);
			Integer[] ymd = new Integer[3];
			String[] pieces = ymds.split("-");
			try
			{
				ymd[2] = Integer.valueOf(pieces[0]);
				ymd[1] = MONTH_TO_INT.get(pieces[1]);
				ymd[0] = Integer.valueOf(pieces[2]);
			}
			catch (NumberFormatException x)
			{
				br.close();
				sr.close();
				return null;
			}
			br.close();
			sr.close();
			return ymd;
		}
		catch (IOException x)
		{
			return null;
		}
	}
	
	
	//                      /db_xref="BOLD:CNWBH029-13.COI-5P"
	public static boolean extractBoldness(String gpPage)
	{
		try
		(
			StringReader sr = new StringReader(gpPage);
			BufferedReader br = new BufferedReader(sr);
		)
		{
			String line;
			while ((line = br.readLine()) != null  &&  !line.trim().startsWith("/db_xref="))
				;
			if (line == null)  
				return false; 
			return line.contains(".COI");
		}
		catch (IOException x)
		{
			return false;
		}
	}
	
	
	static int extractSequenceLength(String gpPage)
	{
		try
		(
			StringReader sr = new StringReader(gpPage);
			BufferedReader br = new BufferedReader(sr);
		)
		{
			String line;
			while (!(line = br.readLine()).startsWith("LOCUS"));
			int nEnd = line.indexOf(" aa");
			int n = nEnd - 1;
			while (Character.isDigit(line.charAt(n)))
				n--;
			n++;
			return Integer.parseInt(line.substring(n, nEnd).trim());
		}
		catch (IOException x)
		{
			return -12345;
		}
	}
	
	
	static String extractDefinitionField(String gpPage)
	{		
		String definition = "";
		
		try
		(
			StringReader sr = new StringReader(gpPage);
			BufferedReader br = new BufferedReader(sr);
		)
		{
			String line;
			while (!(line = br.readLine()).startsWith("DEFINITION"))
				;
			definition += line.substring("DEFINITION".length()).trim();
			line = br.readLine();
			while (Character.isWhitespace(line.charAt(0)))
			{
				definition += " " + line.trim();
				line = br.readLine();
			}
			// cytochrome c oxidase subunit I, partial (mitochondrion) [Haemadipsa zeylanica agilis].
		}
		catch (IOException x) { }
		
		if (definition.endsWith("."))
			definition = definition.substring(0,  definition.length()-1);
		definition = definition.trim();
		if (definition.endsWith(","))
			definition = definition.substring(0,  definition.length()-1).trim();
		if (definition.startsWith("RecName:"))
		{
			definition = definition.substring(8).trim();
			if (definition.contains(";"))
				definition = definition.substring(0,  definition.indexOf(';'));
		}
		
		return definition;
	}
	
	
	static String[] extractBinomialAndOrganismField(String gpPage)
	{		
		String organism = "";
		String binomial = "";
		
		try
		(
			StringReader sr = new StringReader(gpPage);
			BufferedReader br = new BufferedReader(sr);
		)
		{
			String line;
			while (!(line = br.readLine().trim()).startsWith("ORGANISM"))
				;
			binomial = line.trim().substring("ORGANISM".length()).trim();	//   ORGANISM  Poliopastea sp. Janzen02
			binomial = binomial.replace("'", "");
			binomial = binomial.replace("\"", "");
			binomial = binomial.trim();
			line = br.readLine();
			while (Character.isWhitespace(line.charAt(0)))
			{
				organism += line.trim();
				line = br.readLine();
			}
			organism = organism.replace("'", "");
			organism = organism.replace("\"", "");
			organism = organism.trim();
		}
		catch (IOException x) { }
		
		return new String[] { binomial, organism };
	}
	
	
	static String extractSequence(String gpPage)
	{
		if (!gpPage.contains("ORIGIN"))
			return null;
		if (!gpPage.contains("//"))
			return null;
		
		try
		(
			StringReader sr = new StringReader(gpPage);
			BufferedReader br = new BufferedReader(sr);
		)
		{
			String line;
			StringBuilder sb = new StringBuilder();
			while (!(line = br.readLine()).startsWith("ORIGIN"));
			while (!(line = br.readLine()).startsWith("//"))
			{
				line.toUpperCase().chars()
					.mapToObj(i -> (char)i)
					.filter(ch -> ch >= 'A' && ch <= 'Z')
					.forEach(ch -> sb.append(ch));
			}
			
			return sb.toString();
		}
		catch (IOException x)
		{
			return null;
		}
	}
	
	
	public String toString()
	{
		String s = "ProteinProperties for " + acc + " (nuc acc " + nucacc + "): " + len + " aas\n" + organism + "\n" + anno + "\n";
		s += "BOLD=" + bold + ", prokaryote=" + proke + ", animal=" + animal + "\n" + seq;
		return s;
	}
	
	
	public void setVerbosePage(boolean b)				{ this.verbosePage = b; }
	
	
	public static void sop(Object x)					{ System.out.println(x); }
	
	
	public static void main(String[] args) throws Exception
	{
		String acc = "YP_002456258";
		sop(acc);
		ProteinProperties pp = new ProteinProperties(acc);
		pp.setVerbosePage(true);
		pp.initFromNCBI();
		sop(pp);
	}
}
