package examples.test;

import com.dfrdev.tarn.collection.EnumerationIter;
import com.dfrdev.tarn.plugin.orm.QueryBuffer;
import com.dfrdev.tarn.util.classloader.ClassUtil;
import com.dfrdev.tarn.util.date.DateUtil;
import com.dfrdev.tarn.util.classloader.ReflectUtil;
import com.dfrdev.tarn.util.ResourceUtil;
import com.alibaba.fastjson.JSON;

import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dfr on 16-1-15.
 */
public class Test {

    public static void main(String[] args) {
//        String datestr="2021-03-27 22:20 0:0:0";
//        String datestr2="2021/03/27 22:20 0:0:0";
//        String datestr3="2021/3/27";
//        String datestr5="2021年3月27日";
//        String datestr6="08:30:23";
//        String datestr7="2021-03-27T22:20";
////        String datestr7=" 2022-07-23'T'23:12:2Z";
//        Date date=DateUtil.parseFull(datestr);
//        Date date2=DateUtil.parseFull(datestr2);
//        Date date3=DateUtil.parseFull(datestr3);
//        Date date4=DateUtil.parseFull(datestr5);
//        Date date5=DateUtil.parseFull(datestr6);
//        Date date6=DateUtil.parseFull(datestr7);
//
//        System.out.println("date:"+date);
//        System.out.println("date2:"+date2);
//        System.out.println("date3:"+date3);
//        System.out.println("date4:"+date4);
//        System.out.println("date5:"+date5);
//        System.out.println("date6:"+date6);
//
//        String sql = "  SELECt asdf,asdf,sdf,sdfrom bus_sss where zzdf=234 ORDER by sdf,asdf DESC 234.dfsd ASC";
//        Pattern pattern = Pattern.compile("SELECT(.*)FROM", Pattern.CASE_INSENSITIVE);
//        Matcher matcher = pattern.matcher(sql);
//        String countField="count(a) as ccc ";
//        System.out.println("a:"+sql);
//        while (matcher.find()) {
//            String temp = sql.substring(matcher.start(),matcher.end());
//            System.out.println("Text:"+temp);
//            sql = sql.replaceAll(temp, "SELECT "+countField+" FROM ");
//        }
//        System.out.println("replace sql:"+sql);
//        sql=sql.replaceAll("(?i)order by(.*)","");
//        System.out.println("replace sql2:"+sql);

        List<URL> list=ResourceUtil.getResources("examples/test/controller");
        EnumerationIter<URL> list2=ResourceUtil.getResourceIter("examples/test/controller/");
        Set<Class<?>> clazzList= ClassUtil.scanPackage("examples.test.controller");
        for (Class<?> clazz : clazzList) {
            System.out.println(clazz);
            System.out.println("name:"+clazz.getName());
            System.out.println("package:"+clazz.getPackage());
            Object[] enumConstants = clazz.getEnumConstants();
        }
            list2.forEach(item->{
                System.out.println("path2:"+item.getPath());
                System.out.println("item2:"+ JSON.toJSONString(item));
            });
        list.stream().forEach(item->{
            System.out.println("path:"+item.getPath());
            System.out.println("item:"+ JSON.toJSONString(item));
        });
    }
}
