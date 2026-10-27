 package com.dfrdev.tarn.collection;

 public class ConcurrentMapx<K, V> extends Mapx<K, V>
 {
   private static final long serialVersionUID = 201404182133L;

   public ConcurrentMapx()
   {
     super(true);
   }

   public ConcurrentMapx(int initCapacity) {
     super(initCapacity, true);
   }
 }

