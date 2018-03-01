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
 *    TabularBlastReader.java
 *    Copyright (C) 2018 Philip Heller
 *    
 */

package coarbitrator;


import java.io.*;
import java.util.*;


public class TabularBlastReader implements Closeable
{	
	private BufferedReader				srcReader;
	
	
	public TabularBlastReader(BufferedReader srcReader) throws IOException
	{
		this.srcReader = srcReader;
	}
	
	
	public void close() throws IOException
	{
	}
	
	
	public TabularBlastHit readBlastHit() throws IOException
	{
		return readBlastHit(',');
	}
	
	

	public TabularBlastHit readBlastHit(char delim) throws IOException
	{
		String line = "#";
		int nLinesRead = 0;
		while (line != null  &&  (line.trim().isEmpty()  ||  line.startsWith("#")))
		{
			line = srcReader.readLine();
			nLinesRead++;
		}
		if (line == null)
			return null;
		else
		{
			try
			{
				return new TabularBlastHit(line, delim);
			}
			catch (IllegalArgumentException x)
			{
				throw new IllegalArgumentException("Line " + nLinesRead, x);
			}
		}
	}
	
	
	public static List<TabularBlastHit> readAll(File f) throws IOException
	{
		FileReader fr = new FileReader(f);
		BufferedReader br = new BufferedReader(fr);
		TabularBlastReader tbr = new TabularBlastReader(br);
		List<TabularBlastHit> ret = new ArrayList<TabularBlastHit>();
		TabularBlastHit hit;
		while ((hit = tbr.readBlastHit()) != null)
			ret.add(hit);
		br.close();
		fr.close();
		tbr.close();
		return ret;
	}
}
