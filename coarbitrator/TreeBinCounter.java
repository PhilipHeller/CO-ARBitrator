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
 *    TreeBinCounter.java
 *    Copyright (C) 2018 Philip Heller
 *    
 */

package coarbitrator;

import java.util.*;


public class TreeBinCounter<K> extends TreeMap<K, long[]> implements java.io.Serializable
{
	private static final long serialVersionUID = 3742898961273739961L;
	
	
	public TreeBinCounter()			{ }


	public String toString()
	{
		String s = "";
		for (K key: keySet())
			s += "\n  " + key + ": " + get(key)[0];
		s += "\n" + size() + " bins.";
		return s;
	}
	

	// This repetition, rather than calling bumpCountForBin(bin, 1) allows subclasses to
	// override as an illegal operation.
	public void bumpCountForBin(K bin)
	{
		long[] count = get(bin);
		if (count == null)
			put(bin, new long[]{1});
		else
			count[0]++;
	}
	
	
	public void bumpCountForBin(K bin, int delta)
	{
		long[] count = get(bin);
		if (count == null)
			put(bin, new long[]{delta});
		else
			count[0] += delta;
	}
	
	
	// -1 if bin doesn't exist.
	public long getCountForBin(K bin)
	{
		return containsKey(bin)  ?  get(bin)[0]:  -1;
	}
	
	
	public long getCountForBinZeroDefault(K bin)
	{
		return containsKey(bin)  ?  getCountForBin(bin)  :  0;
	}
	
	
	public Vector<K> keysByPopulationAscending()
	{
		Map<Long, Vector<K>> popToKeySet = new TreeMap<Long, Vector<K>>();
		for (K key: keySet())
		{
			long pop = get(key)[0];
			Vector<K> keysForPop = popToKeySet.get(pop);
			if (keysForPop == null)
			{
				keysForPop = new Vector<K>();
				popToKeySet.put(pop, keysForPop);
			}
			keysForPop.add(key);
		}
		Vector<K> ret = new Vector<K>(size());
		for (Vector<K> addMe: popToKeySet.values())
			ret.addAll(addMe);
		return ret;
	}
	
	
	public Vector<K> keysByPopulationDescending()
	{
		Vector<K> ret = new Vector<K>(size());
		Vector<K> ascending = keysByPopulationAscending();
		for (int i=ascending.size()-1; i>=0; i--)
			ret.add(ascending.get(i));
		return ret;
	}
	
	
	public long getSumOfAllCounts()
	{
		long sum = 0;
		for (long[] longarr: values())
			sum += longarr[0];
		return sum;
	}
	
	
	public TreeBinCounter<K> combineWith(TreeBinCounter<K> that)
	{
		TreeBinCounter<K> ret = new TreeBinCounter<K>();
		for (K k: keySet())
			ret.bumpCountForBin(k);
		for (K k: that.keySet())
			ret.bumpCountForBin(k);
		return ret;
	}
	
	
	public K getMinKey()
	{
		return keySet().iterator().next();
	}
	
	
	public K getMaxKey()
	{
		if (isEmpty())
			return null;
		List<K> list = new ArrayList<>(keySet()); 
		return list.get(list.size()-1);	
	}
}
