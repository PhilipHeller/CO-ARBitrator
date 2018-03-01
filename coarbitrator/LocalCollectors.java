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
 *    LocalCollectors.java
 *    Copyright (C) 2018 Philip Heller
 *    
 */

package coarbitrator;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import java.util.stream.Collector.Characteristics;


public class LocalCollectors 
{
	private LocalCollectors()		{ }
	
	
	private class TreeBinCounterCollector<T> implements Collector<T, TreeBinCounter<T>, TreeBinCounter<T>>
	{
		@Override
		public Supplier<TreeBinCounter<T>> supplier() 
		{	
			return () -> new TreeBinCounter<T>();
		}
		

		@Override
		public BiConsumer<TreeBinCounter<T>, T> accumulator() 
		{
			return (ctr, s) -> ctr.bumpCountForBin(s);
		}

		@Override
		public BinaryOperator<TreeBinCounter<T>> combiner() {
			return (ctr1, ctr2) -> ctr1.combineWith(ctr2);
		}

		@Override
		public Function<TreeBinCounter<T>, TreeBinCounter<T>> finisher() 
		{
			return ctr -> ctr;
		}

		@Override
		public Set<Characteristics> characteristics() 
		{
			Set<Characteristics> set = new HashSet<>();
			set.add(Characteristics.IDENTITY_FINISH);
			set.add(Characteristics.UNORDERED);
			return set;
		}
	}  // End of inner class TreeBinCounterCollector<T>
	
	
	public static <T> TreeBinCounterCollector<T> toTreeBinCounter()
	{
		return new LocalCollectors().new TreeBinCounterCollector<T>();
	}
	
	
	private static<T> TreeSet<T> mergeSets(TreeSet<T> set1, TreeSet<T> set2)
	{
		set1.addAll(set2);
		return set1;
	}
	
	
	private class TreeSetCollector<T> implements Collector<T, TreeSet<T>, TreeSet<T>>
	{
		@Override
		public Supplier<TreeSet<T>> supplier() 
		{	
			return () -> new TreeSet<T>();
		}

		@Override
		public BiConsumer<TreeSet<T>, T> accumulator() 
		{
			return (set, s) -> set.add(s);
		}

		@Override
		public BinaryOperator<TreeSet<T>> combiner() {
			return (set1, set2) -> mergeSets(set1, set2);
		}

		@Override
		public Function<TreeSet<T>, TreeSet<T>> finisher() 
		{
			return ctr -> ctr;
		}

		@Override
		public Set<Characteristics> characteristics() 
		{
			Set<Characteristics> set = new HashSet<>();
			set.add(Characteristics.IDENTITY_FINISH);
			set.add(Characteristics.UNORDERED);
			return set;
		}
	}  // End of inner class TreeSetCollector<T>

	
	public static <T> TreeSetCollector<T> toTreeSet()
	{
		return new LocalCollectors().new TreeSetCollector<T>();
	}
	
	
	private static<T> Stack<T> mergeStacks(Stack<T> s1, Stack<T> s2)
	{
		s1.addAll(s2);
		return s1;
	}
	
	
	

	
	
	private class StackCollector<T> implements Collector<T, Stack<T>, Stack<T>>
	{
		@Override
		public Supplier<Stack<T>> supplier() 
		{	
			return () -> new Stack<T>();
		}
		

		@Override
		public BiConsumer<Stack<T>, T> accumulator() 
		{
			return (stack, s) -> stack.push(s);
		}

		@Override
		public BinaryOperator<Stack<T>> combiner() {
			return (stack1, stack2) -> mergeStacks(stack1, stack2);
		}

		@Override
		public Function<Stack<T>, Stack<T>> finisher() 
		{
			return st -> st;
		}

		@Override
		public Set<Characteristics> characteristics() 
		{
			Set<Characteristics> set = new HashSet<>();
			set.add(Characteristics.IDENTITY_FINISH);
			return set;
		}
	}  // End of inner class TreeSetCollector<T>

	
	public static <T> StackCollector<T> toStack()
	{
		return new LocalCollectors().new StackCollector<T>();
	}
	
	
	
	
	
	
	static void sop(Object x)	{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		System.out.println("START");
		List<Integer> ss = new ArrayList<>();
		ss.add(11111);
		ss.add(22222);
		ss.add(33333);
		ss.add(44444);
		Stack<Integer> st = ss.stream().collect(toStack());
		st.stream().forEach(i -> sop(i));
	}
}
