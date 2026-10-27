package com.dfrdev.tarn.plugin;

import java.util.Properties;

/**
 * IPlugin
 */
public interface IPlugin {
	boolean start();
	boolean start(Properties properties);
	boolean stop();
}