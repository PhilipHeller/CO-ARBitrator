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
 *    Pair.java
 *    Copyright (C) 2018 Philip Heller
 *    
 */

package coarbitrator;

public class Pair<T1, T2>
{
	private T1		t1;
	private T2		t2;
	
	
	public Pair(T1 t1, T2 t2)
	{
		this.t1 = t1;
		this.t2 = t2;
	}
	
	
	public T1 getFirst()
	{
		return t1;
	}
	
	
	public T2 getSecond()
	{
		return t2;
	}
	
	
	public String toString()
	{
		return "Pair<" + getType(t1) + "," + getType(t2) + "> = " + t1 + "  ,  " + t2;
	}
	
	
	private String getType(Object x)
	{
		if (x == null)
			return "#";
		String cname = x.getClass().getName();
		if (cname.contains("."))
			cname = cname.substring(cname.lastIndexOf('.') + 1);
		return cname;
	}
}
