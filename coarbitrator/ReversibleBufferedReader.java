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
 *    ReversibleBufferedReader.java
 *    Copyright (C) 2018 Philip Heller
 *    
 */

package coarbitrator;

import java.io.*;
import java.util.Stack;


public class ReversibleBufferedReader extends BufferedReader
{
	private Stack<String>		stack;
	
	
	public ReversibleBufferedReader(Reader src)
	{
		super(src);
		stack = new Stack<>();
	}
	
	
	public String readLine() throws IOException
	{
		return stack.isEmpty()  ?  super.readLine()  :  stack.pop();
	}
	
	
	public void push(String s)
	{
		stack.push(s);
	}
}
