package cc.ruixing.framework.tarn.plugin.ehcache;


import cc.ruixing.framework.tarn.aop.ActionInvocation;

/**
 * EvictInterceptor.
 */
public class EvictInterceptor {
	
	final public void intercept(ActionInvocation ai) {
		CacheKit.removeAll(buildCacheName(ai));
	}
	
	private String buildCacheName(ActionInvocation ai) {
		CacheName cacheName = ai.getClass().getAnnotation(CacheName.class);
		if (cacheName != null)
			return cacheName.value();
		
		cacheName = ai.getClass().getAnnotation(CacheName.class);
		if (cacheName == null)
			throw new RuntimeException("EvictInterceptor need CacheName annotation in controller.");
		return cacheName.value();
	}
}

