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
 *    HashBinCounter.java
 *    Copyright (C) 2018 Philip Heller
 *    
 */

package coarbitrator;

import java.util.*;


public class HashBinCounter<K> extends HashMap<K, int[]> implements java.io.Serializable
{
	private static final long serialVersionUID = 3742898961273739961L;
	
	
	public HashBinCounter()			{ }


	public String toString()
	{
		String s = "";
		for (K key: keySet())
			s += "\n  " + key + ": " + get(key)[0];
		s += "\n" + size() + " bins";
		return s;
	}
	
	
	public void bumpCountForBin(K bin)
	{
		bumpCountForBin(bin, 1);
	}
	
	
	public void bumpCountForBin(K bin, int delta)
	{
		int[] count = get(bin);
		if (count == null)
			put(bin, new int[]{delta});
		else
			count[0] += delta;
	}
	
	
	// -1 if bin doesn't exist.
	public int getCountForBin(K bin)
	{
		return containsKey(bin)  ?  get(bin)[0]:  -1;
	}
	
	
	public int getCountForBinZeroDefault(K bin)
	{
		return containsKey(bin)  ?  getCountForBin(bin)  :  0;
	}
	
	
	public Vector<K> keysByPopulationAscending()
	{
		Map<Integer, Vector<K>> popToKeySet = new TreeMap<Integer, Vector<K>>();
		for (K key: keySet())
		{
			int pop = get(key)[0];
			Vector<K> keysForPop = popToKeySet.get(pop);
			if (keysForPop == null)
			{
				keysForPop = new Vector<K>();
				popToKeySet.put(pop, keysForPop);
			}
			keysForPop.add(key);
		}
		Vector<K> ret = new Vector<K>();
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
	
	
	public int getSumOfAllCounts()
	{
		int sum = 0;
		for (int[] intarr: values())
			sum += intarr[0];
		return sum;
	}
	
	
	public HashBinCounter<Integer> getCountsHistogram()
	{
		HashBinCounter<Integer> histo = new HashBinCounter<Integer>();
		for (int[] countArr: values())
			histo.bumpCountForBin(countArr[0]);
		return histo;
	}
	
	
	public void add(HashBinCounter<K> that)
	{
		for (K bin: that.keySet())
			this.bumpCountForBin(bin, that.getCountForBin(bin));
	}
	
	
	// Arbitrary if there's a tie.
	public K getKeyWithMaxPopulation()
	{
		int maxPop = -Integer.MAX_VALUE;
		K winnerKey = null;
		for (K key: keySet())
		{
			int pop = getCountForBinZeroDefault(key);
			if (pop > maxPop)
			{
				maxPop = pop;
				winnerKey = key;
			}
		}
		return winnerKey;
	}
}
