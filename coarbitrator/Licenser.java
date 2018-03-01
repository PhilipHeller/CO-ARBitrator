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
 *    Licenser.java
 *    Copyright (C) 2018 Philip Heller
 *    
 */

package coarbitrator;

import java.io.*;


public class Licenser 
{
	static void sop(Object x) 		{ System.out.println(x); }
	
	
	private static void prependLicense(File f) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		sb.append("/*\n");
		appendComment(sb, "This program is free software; you can redistribute it and/or modify");
		appendComment(sb, "it under the terms of the GNU General Public License as published by");
		appendComment(sb, "the Free Software Foundation; either version 2 of the License, or");
		appendComment(sb, "(at your option) any later version.");
		appendComment(sb, "");
		appendComment(sb, "This program is distributed in the hope that it will be useful,");
		appendComment(sb, "but WITHOUT ANY WARRANTY; without even the implied warranty of");
		appendComment(sb, "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the");
		appendComment(sb, "GNU General Public License for more details.");
		appendComment(sb, "");
		sb.append(" */\n\n/*\n");
		appendComment(sb, f.getName());
		appendComment(sb, "Copyright (C) 2018 Philip Heller");
		appendComment(sb, "");
		sb.append(" */\n\n");
		
		try
		(
				FileReader fr = new FileReader(f);
				BufferedReader br = new BufferedReader(fr);
		)
		{
			String line;
			while ((line = br.readLine()) != null)
				sb.append(line + "\n");
		}
		catch (IOException x) { }
		
		try
		(
				FileWriter fw = new FileWriter(f);
		)
		{
			fw.write(sb.toString());
		}
	}
	
	
	private static void appendComment(StringBuilder sb, String comment)
	{
		sb.append(" *    " + comment + "\n");
	}
	
	
	public static void main(String[] args) throws IOException
	{
		sop("START");
		File dirf = new File("src/coarbitrator");
		for (String child: dirf.list())
		{
			if (!child.endsWith(".java"))
				continue;
			sop(child);
			prependLicense(new File(dirf, child));
		}
	}
}
