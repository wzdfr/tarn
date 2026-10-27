package com.dfrdev.tarn.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Filters.
 */
final public class Filters {
	
	private final List<FilterInterface> filterList = new ArrayList<FilterInterface>();
	
	public Filters add(FilterInterface plugin) {
		if (plugin != null)
			this.filterList.add(plugin);
		return this;
	}
	
	public List<FilterInterface> getFilterList() {
		return filterList;
	}
}
