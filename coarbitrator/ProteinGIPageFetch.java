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
 *    ProteinGIPageFetch.java
 *    Copyright (C) 2018 Philip Heller
 *    
 */

package coarbitrator;

import java.net.*;
import java.util.*;
import java.io.*;
import java.sql.*;


// 
// Nov 9, 2016: NCBI announced switching to https. Probably all http urls s/b updated.
//


public class ProteinGIPageFetch
{	
	private String					surl;
	private URLConnection			urlConn;
	private InputStreamReader		isr;
	
	
	public ProteinGIPageFetch(String surl)		{ this.surl = surl; }

	
	public static ProteinGIPageFetch forInitiateGPLookup(String accession)
	{
		String surl = buildInitiateGPLookupSurl(accession);
		return new ProteinGIPageFetch(surl);
	}
		
		
	private static String buildInitiateGPLookupSurl(String accession)	
	{
		return "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?" +
			"&rettype=gp&usehistory=n&db=protein&term=" + accession;
	}
	
	
	public static ProteinGIPageFetch forRetrieveGPFromEntrez(String accession)
	{
		String surl = buildRetrieveGPFromEntrez(accession);
		return new ProteinGIPageFetch(surl);
	}

		
	private static String buildRetrieveGPFromEntrez(String euID)	
	{
		return "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?" +
			"&rettype=gp&db=protein&id=" + euID;
	}
	
	
	public static String getProteinGPPage(String accessionOrGI) throws IOException, ConversionException
	{		
		// Use eUtils to retrieve .gp page. 1st response page is XML. Retrieve 1st ID in <ID> tag.
		ProteinGIPageFetch client = forInitiateGPLookup(accessionOrGI);
		String eutilsInitialResponse = client.getResponsePageAsString();
		if (eutilsInitialResponse == null)
			throw new ConversionException(ConversionFailure.PROTEIN_GP_PAGE_NO_INITIAL_RESPONSE);
		int index = eutilsInitialResponse.indexOf("<Id>");
		if (index < 0)
			throw new ConversionException(ConversionFailure.PROTEIN_GP_PAGE_NO_ID_TAG_IN_INITIAL_RESPONSE);
		index += "<Id>".length();
		eutilsInitialResponse = eutilsInitialResponse.substring(index);
		String euID = "";
		int n = 0;
		while (Character.isJavaIdentifierPart(eutilsInitialResponse.charAt(n)))
			euID += eutilsInitialResponse.charAt(n++);
		
		// Retrieve the .gp protein page from Entrez. It contains a "coded_by" tag that contains 
		// the nucleotide accession #, range, and strand that we need.
		client = forRetrieveGPFromEntrez(euID);
		String gpPage = client.getResponsePageAsString();
		if (gpPage == null)
			throw new ConversionException(ConversionFailure.PROTEIN_GP_PAGE_NO_GP_PAGE);
		return gpPage;
	}
		
	
	public LineNumberReader getLineNumberReaderForResponse() throws MalformedURLException, IOException
	{
		URL url = new URL(surl);		// throws MalformedURLException
		urlConn = url.openConnection();
		isr = new InputStreamReader(urlConn.getInputStream());
		return new LineNumberReader(isr);
	}
	
	
	// Converts IOException to ConversionException.
	public String getResponsePageAsString() throws ConversionException
	{		
		return getResponsePageAsString(-1);
	}

	
	// Converts IOException to ConversionException. Honors maxLines if >0. 
	public String getResponsePageAsString(int maxLines) throws ConversionException
	{
		try
		(
			LineNumberReader lnr = getLineNumberReaderForResponse();
		)
		{
			StringBuilder sb = new StringBuilder();
			String line = null;
			int nLines = 0;
			while ((line = lnr.readLine()) != null)
			{
				sb.append(line);
				if (!line.endsWith("\n"))
					sb.append("\n");
				if (maxLines > 0  &&  ++nLines >= maxLines)
					break;
			}
			return sb.toString();
		}
		catch (IOException x)
		{
			sop("Stress: " + x.getMessage());
			throw new ConversionException(ConversionFailure.NUCLEOTIDE_PAGE_NOT_RECEIVED);
		}
	}
	
	
	public static Pair<Boolean, String> getBacterialAndSequence(String gi) throws IOException, ConversionException
	{
		// Get page.
		String page = getProteinGPPage(gi);
		
		// Parse bacterial call.
		Boolean bacterial = null;	// null means can't determine
		String organism = extractBinomialAndOrganismField(page)[1];
		if (organism != null)
		{
			if (organism.startsWith("Eukaryot")  ||  organism.startsWith("Eucaryot"))
				bacterial = false;
			else if (organism.startsWith("Bacteria")  ||  organism.startsWith("Prokaryot")  ||  organism.startsWith("Procaryot"))
				bacterial = true;
		}
		
		// Extract sequence.
		String seq = extractSequence(page);
		
		return new Pair<Boolean, String>(bacterial, seq);
	}
	
	
	// [0] is the binomial, [1] is the organism field (which dioesn't include species).
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
	
	
	public String getURLString()				{ return surl; }
	static void sop(Object x)					{ System.out.println(x); }
	
	
	public static void main(String[] args) throws Exception
	{
		String gi = "BAB62384";
		
		try
		{
			String page = getProteinGPPage(gi);
			sop(page);
		}
		catch (Exception x)
		{
			sop("STRESS: " + x.getMessage());
			x.printStackTrace();
		}
	}
}
