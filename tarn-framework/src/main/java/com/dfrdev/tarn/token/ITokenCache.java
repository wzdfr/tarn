package com.dfrdev.tarn.token;

import java.util.List;

/**
 * ITokenCache.
 */
public interface ITokenCache {
	
	void put(Token token);
	
	void remove(Token token);
	
	boolean contains(Token token);
	
	List<Token> getAll();
}
