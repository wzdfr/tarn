package com.dfrdev.tarn.util.text;


import java.util.Iterator;
import java.util.Map;
import com.dfrdev.tarn.util.LogUtil;

public class HtmlUtil {
//    public static Mapx<String, Object> codeToMapx(String CodeType) {
//        Map map = CacheKit.get("framework-program", "html-code-" + CodeType);
//        Mapx map2 = new Mapx();
//        if (map == null) {
//            return map2;
//        }
//        for (String key : map.keySet()) {
//            if (map.get(key).toString().startsWith("{")) {
//                DAO dao = (DAO) map.get(key);
//                map2.put(key, LangUtil.get(dao.get("CodeName")));
//            } else {
//                map2.put(key, LangUtil.get(map.get(key).toString()));
//            }
//        }
//        return map2;
//    }

//    public static String codeToCheckboxes(String name, String CodeType, String[] checkedArray, boolean direction) {
//        return mapxToCheckboxes(name, codeToMapx(CodeType), checkedArray, null, direction);
//    }

    public static String mapxToOptions(Map<?, ?> map, String type, Object checkedValue, boolean addBlankOptionFlag) {
        FastStringBuilder sb = new FastStringBuilder();
        if (addBlankOptionFlag) {
            sb.append("<").append(type).append(" value=''></").append(type).append(">");
        }
        if (map == null) {
            return sb.toStringAndClose();
        }
        Object[] keys = map.keySet().toArray();
        for (Object v : keys) {
            sb.append("<").append(type).append(" value=\"");
            sb.append(v);
            if ((v != null) && (v.equals(checkedValue)))
                sb.append("\" selected='true' >");
            else {
                sb.append("\">");
            }
            sb.append(map.get(v));
            sb.append("</").append(type).append(">");
        }
        return sb.toStringAndClose();
    }

    public static String mapxToRadios(String name, Map<?, ?> map, Object checkedValue) {
        return mapxToRadios(name, map, checkedValue, false);
    }

    public static String mapxToRadios(String name, Map<?, ?> map, Object checkedValue, boolean direction) {
        FastStringBuilder sb = new FastStringBuilder();
        Object[] keys = map.keySet().toArray();
        for (int i = 0; i < keys.length; i++) {
            Object value = keys[i];
            sb.append("<input type='radio' name='").append(name);
            sb.append("' id='").append(name).append("_").append(i).append("' value='");
            sb.append(value);
            if (value.equals(checkedValue))
                sb.append("' checked='true' >");
            else {
                sb.append("' />");
            }
            sb.append("<label for='").append(name).append("_").append(i).append("'>");
            sb.append(map.get(value));
            sb.append("</label>&nbsp;");
            if (direction) {
                sb.append("<br>");
            }
        }
        return sb.toStringAndClose();
    }

    public static String mapxToCheckboxes(String name, Map<?, ?> map, Object[] checkedArray) {
        return mapxToCheckboxes(name, map, checkedArray, null);
    }

    public static String mapxToCheckboxes(String name, Map<?, ?> map, Object[] checkedArray, Object[] disabledValue) {
        return mapxToCheckboxes(name, map, checkedArray, disabledValue, false);
    }

    public static String mapxToCheckboxes(String name, Map<?, ?> map, Object[] checkedArray, Object[] disabledValue, boolean direction) {
        FastStringBuilder sb = new FastStringBuilder();
        int k = 0;
        for (Iterator localIterator = map.keySet().iterator(); localIterator.hasNext(); ) {
            Object value = localIterator.next();
            sb.append("<input type='checkbox'");
            sb.append(" name='").append(name);
            sb.append("' id='").append(name).append("_").append(k).append("' value='");
            sb.append(value);
            sb.append("'");
            if (disabledValue != null) {
                for (Object element : disabledValue) {
                    if (value.equals(element)) {
                        sb.append(" disabled='true'");
                        break;
                    }
                }
            }

            if (checkedArray != null) {
                for (Object element : checkedArray) {
                    if (value.equals(element)) {
                        sb.append(" checked='true'");
                        break;
                    }
                }
            }

            sb.append(" >");
            sb.append("<label for='").append(name).append("_").append(k).append("'>");
            sb.append(map.get(value));
            sb.append("</label>&nbsp;");
            if (direction) {
                sb.append("<br>");
            }
            k++;
        }
        return sb.toStringAndClose();
    }

//    public static String replacePlaceHolder(String html, Map<?, ?> map, boolean blankFlag) {
//        return replacePlaceHolder(html, map, blankFlag, false);
//    }

//    public static String replacePlaceHolder(String html, Map<?, ?> map, boolean blankFlag, boolean spaceFlag) {
//        try {
//            return PlaceHolderUtil.replacePlaceHolder(html, map, blankFlag, spaceFlag);
//        } catch (ExpressionException e) {
//            LogUtil.error("操作失败: " + e.getMessage(), e);
//        }
//        return null;
//    }
//
//    public static String replaceWithDataTable(DataTable dt, String html) {
//        return replaceWithDataTable(dt, html, false, true);
//    }
//
//    public static String replaceWithDataTable(DataTable dt, String html, boolean spaceFlag) {
//        return replaceWithDataTable(dt, html, false, spaceFlag);
//    }
//
//    public static String replaceWithDataTable(DataTable dt, String html, boolean blankFlag, boolean spaceFlag) {
//        try {
//            return PlaceHolderUtil.replaceWithDataTable(html, dt, blankFlag, spaceFlag);
//        } catch (ExpressionException e) {
//            LogUtil.error("操作失败: " + e.getMessage(), e);
//        }
//        return null;
//    }
//
//    public static String replaceWithDataRow(DataRow dr, String html) {
//        return replaceWithDataRow(dr, html, true);
//    }
//
//    public static String replaceWithDataRow(DataRow dr, String html, boolean spaceFlag) {
//        return replaceWithDataRow(dr, html, false, spaceFlag);
//    }
//
//    public static String replaceWithDataRow(DataRow dr, String html, boolean blankFlag, boolean spaceFlag) {
//        return replacePlaceHolder(html, dr.toMapx(), blankFlag, spaceFlag);
//    }
//
//    public static HtmlTable dataTableToHtmlTable(DataTable dt, String cellHtml, int columnCount) {
//        HtmlTable table = new HtmlTable();
//        HtmlTR tr = null;
//        for (int i = 0; i < dt.getRowCount(); i++) {
//            if (i % columnCount == 0) {
//                tr = new HtmlTR();
//                table.addTR(tr);
//            }
//            HtmlTD td = new HtmlTD();
//            td.setInnerHTML(replaceWithDataRow(dt.getDataRow(i), cellHtml));
//            tr.addChild(td);
//        }
//        for (int i = 0; i < dt.getRowCount() % columnCount; i++) {
//            HtmlTD td = new HtmlTD();
//            td.setInnerHTML("&nbsp;");
//            tr.addChild(td);
//        }
//        return table;
//    }
}