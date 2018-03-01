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
 *    CdPssmidToAccAndShortName.java
 *    Copyright (C) 2018 Philip Heller
 *    
 */

package coarbitrator;

import java.util.*;
import java.util.stream.Collectors;
import java.io.*;


/*
 * The file cdd.versions has been downloaded from NCBI. It maps conserved domain accession number to short name
 * and pssm id. It isn't clear why the pssm id exists, but it's what appears as hit subjects when you do a local
 * rpsblast against Cdd. So this map is a necessary evil.
 */

public class CdPssmidToAccAndShortName extends HashMap<String, Pair<String, String>> implements Serializable
{
	private final static File		DFLT_IFILE = new File("data/cdd.versions");
	private final static File		BACKUP_IFILE = new File("cdd.versions");
	
	static int nctors = 0;
	
	public CdPssmidToAccAndShortName() throws IOException
	{
		File ifile = DFLT_IFILE.exists()  ?  DFLT_IFILE  :  BACKUP_IFILE;
		try
		(
				FileReader fr = new FileReader(ifile);
				BufferedReader br = new BufferedReader(fr);
		)
		{
			for (int i=0; i<4; i++)
				br.readLine();
			
			String line;
			while ((line = br.readLine()) != null)
			{
				if (line.trim().isEmpty())
					continue;
				String[] pieces = line.split("\\s");
				List<String> nonWhitePieces =
					Arrays.stream(pieces)
					.filter(p -> !p.trim().isEmpty())
					.collect(Collectors.toList());
				Pair<String, String> pair = new Pair<>(nonWhitePieces.get(0), nonWhitePieces.get(1));
				put(nonWhitePieces.get(2), pair);
			}
		}
	}
	
	
	public String getAccession(String pssmId)
	{
		Pair<String, String> pair = get(pssmId);
		return (pair != null)  ?  pair.getFirst()  :  null;
	}
	
	
	public String getShortName(String pssmId)
	{
		Pair<String, String> pair = get(pssmId);
		return (pair != null)  ?  pair.getSecond()  :  null;
	}
	
	
	static void sop(Object x)	{ System.out.println(x); }
	
	
	public static void main(String[] args) throws IOException
	{
		sop("START");
		CdPssmidToAccAndShortName map = new CdPssmidToAccAndShortName();
		map.keySet().stream().forEach(k -> sop(k + " " + map.getAccession(k) + "  = " + map.getShortName(k)));
		sop("DONE");
	}
}
