package cc.ruixing.framework.tarn.plugin.ehcache;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface CacheName {
	String value();
}
