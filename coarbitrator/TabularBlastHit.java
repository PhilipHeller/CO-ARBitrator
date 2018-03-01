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
 *    TabularBlastHit.java
 *    Copyright (C) 2018 Philip Heller
 *    
 */

package coarbitrator;


import java.util.*;
import java.io.*;


// 
// Tabular (output format = 10) blast output is e.g.
// SCRPIER:1:1101:14543:1467#NGTAGC/1	646312021	85.47	117	17	0	35	151	1291015	1290899	8e-22	97.6
// Fields are query, subject, %ident, length, mismatches, gap opens, q start, q end, s start, s end, e, score
//
// 7/21/16: NCBI returns tsv with 13 fields, local blast returns csv with 12 fields.
//

public class TabularBlastHit implements Comparable<TabularBlastHit>, java.io.Serializable
{
	private static final long serialVersionUID = 2335909001698215384L;
	
	public String				query;
	public String 				subject;
	public float				pctIdent;
	public int					length;
	public int					mismatches;
	public int					gapOpens;
	public double				e;
	public int					queryStart;
	public int					queryEnd;
	public int					subjectStart;
	public int					subjectEnd;
	public float				score;
	
	
	private enum Field
	{
		QUERY, SUBJECT, IDENT, IDENT2, LENGTH, MISMATCHES, GAP_OPENS, QSTART, QEND, SSTART, SEND, E, SCORE;	
		//                        ^----- This showed up July 11 2016 in results from NCBI.
	}
	
	
	private TabularBlastHit()	{ }
	
	
	public TabularBlastHit(String s) throws IllegalArgumentException
	{
		this(s, ',');
	}
	
	
	public TabularBlastHit(String s, char delim) throws IllegalArgumentException
	{		
		if (delim != '\t'  &&  delim != ',')
			throw new IllegalArgumentException("Illegal delimiter " + delim + " (must be tab or comma)");
		
		if (s.startsWith("#") || s.startsWith(">"))
			throw new IllegalArgumentException("Unexpected 1st char in " + s);
		
		String splitter = (delim == '\t')  ?  "\\t"  :  ",";
		int expectedNFields = (delim == '\t')  ?  13  :  12;
		String[] pieces = s.split(splitter);
		Vector<String> vec = new Vector<String>();
		for (String piece: pieces)
			if (!piece.trim().isEmpty())
				vec.add(piece);		
		if (vec.size() != expectedNFields)
		{
			String err = "Wrong number of fields: saw " + vec.size() + ", expected " + Field.values().length + ":\n" + s;
			throw new IllegalArgumentException(err);
		}
		int n = 0;
		try
		{
			query = vec.get(n);
			subject = vec.get(++n);
			pctIdent = Float.parseFloat(vec.get(++n));
			if (expectedNFields == 13)
				n++;
			length = Integer.parseInt(vec.get(++n));
			mismatches = Integer.parseInt(vec.get(++n));
			gapOpens = Integer.parseInt(vec.get(++n));
			int q1 = Integer.parseInt(vec.get(++n));
			int q2 = Integer.parseInt(vec.get(++n));
			queryStart = Math.min(q1, q2);
			queryEnd = Math.max(q1, q2);
			int s1 = Integer.parseInt(vec.get(++n));
			int s2 = Integer.parseInt(vec.get(++n));
			subjectStart = Math.min(s1, s2);
			subjectEnd = Math.max(s1, s2);
			e = Double.parseDouble(vec.get(++n));
			score = Float.parseFloat(vec.get(++n));
		}
		catch (NumberFormatException nfx)
		{
			String err = "Can't parse field: " + Field.values()[n] + "\n" +
				"Fields: query, subject, %ident, length, mismatches, gap opens, q start, q end, s start, s end, e, score\n" +
				s + "\n";
			for (int i=0; i<pieces.length; i++)
				err += "\n  " + i + ": " + pieces[i];
			err += "\nNFE message: " + nfx.getMessage();
			assert false : err;	
			throw new IllegalArgumentException(err);
		}
	}
	
	
	public int compareTo(TabularBlastHit that)
	{
		if (!this.query.equals(that.query))
			return this.query.compareTo(that.query);
		if (this.length != that.length)
			return (int)Math.signum(this.length - that.length);
		if (this.pctIdent != that.pctIdent)
			return (int)Math.signum(this.pctIdent - that.pctIdent);
		if (!this.subject.equals(that.subject))
			return this.subject.compareTo(that.subject);
		if (this.e != that.e)
			return (int)Math.signum(that.e - this.e);
		return this.hashCode() - that.hashCode();
	}
	
	
	public String toString()
	{
		return "Query=" + query + ", Sbjct=" + subject + ", %ident=" + pctIdent + " over " + length +
			" at " + queryStart + "-" + queryEnd + ", e-value=" + e;
	}
	
	
	public int queryLength()
	{
		return Math.abs(queryEnd - queryStart) + 1;
	}
	
	
	public int subjectLength()
	{
		return Math.abs(subjectEnd - subjectStart) + 1;
	}
	
	
	public String getGIFromSubject()
	{
		String[] pieces = subject.toUpperCase().split("\\|");
		for (int i=0; i<pieces.length-1; i++)
			if (pieces[i].equals("GI"))
				return pieces[i+1];
		return null;
	}
	
	
	// E.g. gi|548786528|gb|AGX13878.1|
	public String getAccNoFromSubject()
	{
		String[] pieces = subject.split("\\|");
		if (pieces.length < 2)
			return null;
		for (int i=0; i<pieces.length-1; i++)
			if (pieces[i].equals("gb"))
				return pieces[i+1];
		return null;
	}
	
	
	static void sop(Object x)		{ System.out.println(x); }


	public static void main(String[] args) throws IOException
	{
		TabularBlastHit hit = new TabularBlastHit();
		hit.subject = "gi|12345|gb|AGX13878.1";
		sop(hit.getAccNoFromSubject());
	}
}
