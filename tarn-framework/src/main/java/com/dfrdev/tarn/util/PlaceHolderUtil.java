package com.dfrdev.tarn.util;


import java.util.ArrayList;
import java.util.List;

public class PlaceHolderUtil {
    public static List<PlaceHolder> parse(String html) {
        List list = new ArrayList();
        int lastIndex = 0;
        int pos = 0;
        while ((pos = html.indexOf("${", lastIndex)) >= 0) {
            if ((pos != 0) && (lastIndex != pos)) {
                PlaceHolder p = new PlaceHolder();
                p.isString = true;
                p.Value = html.substring(lastIndex, pos);
                list.add(p);
            }
            char lastStringChar = '\000';
            boolean escapeFlag = false;
            boolean continueFlag = false;
            for (int i = pos + 2; i < html.length(); i++) {
                char c = html.charAt(i);
                if (c == '\\') {
                    escapeFlag = true;
                } else if (!escapeFlag) {
                    if ((c == '"') || (c == '\'')) {
                        if (lastStringChar == c) {
                            lastStringChar = '\000';
                        } else
                            lastStringChar = c;
                    } else {
                        if ((c == '}') && (lastStringChar == 0)) {
                            String v = html.substring(pos, i + 1);
                            PlaceHolder p = new PlaceHolder();
                            p.isExpression = true;
                            p.Value = v;
                            list.add(p);
                            lastIndex = i + 1;
                            continueFlag = true;
                            break;
                        }
                        if (c == '\n') {
                            lastIndex = i + 1;
                            continueFlag = true;
                            break;
                        }
                    }
                } else escapeFlag = false;

            }

            if (!continueFlag) {
                break;
            }
        }
        if (lastIndex != html.length()) {
            PlaceHolder p = new PlaceHolder();
            p.isString = true;
            p.Value = html.substring(lastIndex);
            list.add(p);
        }
        return list;
    }
//
//  public static String replacePlaceHolder(String html, Map<?, ?> map, boolean blankFlag, boolean spaceFlag) throws TarnException {
//    if ((html == null) || (map == null)) {
//      return "";
//    }
//    String blank = "";
//    if (spaceFlag) {
//      blank = "&nbsp;";
//    }
//    List list = parse(html);
//    FastStringBuilder sb = new FastStringBuilder();
//    LoopVariableResolver vr = new LoopVariableResolver();
//    vr.setMap(map);
//    for (PlaceHolder p : list) {
//      if (p.isString) {
//        sb.append(p.Value);
//      }
//      if (p.isExpression) {
//        Object v = ZhtmlManagerContext.getInstance().getEvaluator()
//          .evaluate(p.Value, Object.class, vr, DefaultFunctionMapper.getInstance());
//        if (v != null) {
//          if ((v == null) || (v.equals(""))) {
//            sb.append(blank);
//          } else if ((v instanceof String)) {
//            String str = (String)v;
//            str = LangUtil.get(str);
//            sb.append(str);
//          } else if ((v instanceof Date)) {
//            String str = DateUtil.toDateTimeString((Date)v);
//            sb.append(str);
//          } else {
//            sb.append(v);
//          }
//        } else if (blankFlag)
//          sb.append("");
//        else {
//          sb.append(p.Value);
//        }
//      }
//    }
//    return sb.toStringAndClose();
//  }

//  public static String replaceWithDataTable(String html, DataTable dt, boolean blankFlag, boolean spaceFlag) throws ExpressionException {
//    if ((html == null) || (dt == null)) {
//      return "";
//    }
//    String blank = "";
//    if (spaceFlag) {
//      blank = "&nbsp;";
//    }
//    List list = parse(html);
//    FastStringBuilder sb = new FastStringBuilder();
//    LoopVariableResolver vr = new LoopVariableResolver();
//    Iterator localIterator2;
//    for (Iterator localIterator1 = dt.iterator(); localIterator1.hasNext();
//      localIterator2.hasNext())
//    {
//      DataRow dr = (DataRow)localIterator1.next();
//      vr.setDataRow(dr);
//      localIterator2 = list.iterator(); continue; PlaceHolder p = (PlaceHolder)localIterator2.next();
//      if (p.isString) {
//        sb.append(p.Value);
//      }
//      if (p.isExpression) {
//        Object v = ZhtmlManagerContext.getInstance().getEvaluator()
//          .evaluate(p.Value, Object.class, vr, DefaultFunctionMapper.getInstance());
//        if (v != null) {
//          if ((v == null) || (v.equals(""))) {
//            sb.append(blank);
//          } else if ((v instanceof String)) {
//            String str = (String)v;
//            str = LangUtil.get(str);
//            sb.append(str);
//          } else if ((v instanceof Date)) {
//            String str = DateUtil.toDateTimeString((Date)v);
//            sb.append(str);
//          } else {
//            sb.append(v);
//          }
//        } else if (blankFlag)
//          sb.append("");
//        else {
//          sb.append(p.Value);
//        }
//      }
//    }
//
//    return sb.toStringAndClose();
//  }
}