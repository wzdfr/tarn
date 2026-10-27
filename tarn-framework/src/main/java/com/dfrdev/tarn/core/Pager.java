package com.dfrdev.tarn.core;


import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.Url;

import java.io.Serializable;
import java.util.List;

/**
 * 数据分页对象
 */
public class Pager<E> implements Serializable{
    /**
     * 第几页
     */
    private int pageIndex;
    /**
     * 每页显示多少条
     */
    private int pageSize;
    /**
     * 总共多少条记录
     */
    private int totalRecord;
    /**
     * 总共多少页
     */
    private int totalPage;
    /**
     * 放置具体数据的列表
     */
    private List<E> datas;

    private String thisUrl;

    private String currPageStr;

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalRecord() {
        return totalRecord;
    }

    public void setTotalRecord(int totalRecord) {
        this.totalRecord = totalRecord;
        this.totalPage = (totalRecord + pageSize - 1) / pageSize;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }

    public List<E> getDatas() {
        return datas;
    }

    public String getThisUrl() {
        return thisUrl;
    }

    public void setThisUrl(String thisUrl) {
        this.thisUrl = thisUrl;
    }

    public void setDatas(List<E> datas) {
        this.datas = datas;
    }

    public String getCurrPageStr() {
        return currPageStr;
    }

    public void setCurrPageStr(String currPageStr) {
        this.currPageStr = currPageStr;

    }


    /**
     * 返回默认页码字符串<br/>
     *
     * @return String  %首页 %上一页 %numbers　%下一页　%尾页
     */
    public String getPageStr() throws TarnException {
        return getPageStr("%首页%上一页%numbers%下一页%尾页");
    }

    /**
     * 返回指定页码字符串<br/>
     *
     * @param styleNum 页面风格，1-6种。
     *                 1：%首页 %上一页　%下一页　%尾页
     *                 2：%首页 %上一页　%下一页　%尾页  共%pages页(%count条记录)　当前第%page页
     *                 3：%首页 %上一页 %numbers　%下一页　%尾页  共%pages页(%count条记录)　当前第%page页
     *                 4：%首页  %numbers　%尾页
     *                 5：共%pages页(%count条记录)　当前第%page页　%首页 %上一页 &numbers　%下一页　%尾页　转到第%select页
     *                 6：<span class='ttips'>共%pages页(%count条记录)</span>%首页%上一页%numbers%下一页%尾页
     *                 其它默认都是：
     *                 %首页 %上一页 &numbers　%下一页　%尾页
     * @return String 返回页码字符串
     */
    public String getPageStr(int styleNum) {
        String pageStr = null;
        switch (styleNum) {
            case 1:
                pageStr = getPageStr("%首页 %上一页　%下一页　%尾页");
                break;
            case 2:
                pageStr = getPageStr("%首页 %上一页　%下一页　%尾页  共%pages页(%count条记录)　当前第%page页");
                break;
            case 3:
                pageStr = getPageStr("%首页 %上一页 %numbers %下一页 %尾页  共%pages页(%count条记录)　当前第%page页");
                break;
            case 4:
                pageStr = getPageStr("%首页 %numbers %尾页");
                break;
            case 5:
                pageStr = getPageStr("共%pages页(%count条记录)　当前第%page页　%首页 %上一页 %numbers　%下一页　%尾页　转到第%select页");
                break;
            case 6:
                pageStr = getPageStr("<span class='ttips'>共%pages页(%count条记录)</span>%首页%上一页%numbers%下一页%尾页");
                break;
            default:
                pageStr = getPageStr();
        }
        return pageStr;
    }

    /**
     * 返回页码字符串. <br />
     *
     * @param pattern 指定页码字符串的样式。
     *                <blockquote>
     *                pattern参数支持的样式如下：<br />
     *                %首页　%上一页　%下一页　%尾页　%pages　%count　%page %select　%Home　%Previous　%Next　%End  %numbers<br />
     *                </blockquote>
     * @return String 页码字符串
     * @throws com.dfrdev.tarn.core.TarnException
     * @see #getPageStr()
     */
    public String getPageStr(String pattern) throws TarnException {
        if (getPageSize() <= 0 || thisUrl == null) {
            LogUtil.debug("Pager对象未设置分页信息，无法获取分页字符串。");
            return "";
//            throw new TarnException("Pager对象未设置分页信息，无法获取分页字符串。");
        }
        if (pattern == null) return getPageStr();
        Url url = new Url(thisUrl);
        if (pattern.indexOf("%pages") != -1) {
            pattern = pattern.replaceAll("%pages", String.valueOf(getTotalPage()));
        }
        if (pattern.indexOf("%count") != -1) {
            pattern = pattern.replaceAll("%count", String.valueOf(getTotalRecord()));
        }
        if (pattern.indexOf("%page") != -1) {
            pattern = pattern.replaceAll("%page", "<span class=\"txtA\">" + getPageIndex() + "</span>");
        }
        if (getPageIndex() > 1) {
            if (pattern.indexOf("%首页") != -1) {
                pattern = pattern.replaceAll("%首页", "<span class=\"pg_home\"><a href=\"" + url.del("page") + "\">首页</a></span>");
            }
            if (pattern.indexOf("%Home") != -1) {
                pattern = pattern.replaceAll("%Home", "<span class=\"pg_home\"><a href=\"" + url.set("page", 1) + "\">Home</a></span>");
            }
            if (pattern.indexOf("%上一页") != -1) {
                pattern = pattern.replaceAll("%上一页", "<span class=\"pg_prev\"><a href=\"" + url.set("page", getPageIndex() - 1) + "\">上一页</a></span>");
            }
            if (pattern.indexOf("%Previous") != -1) {
                pattern = pattern.replaceAll("%Previous", "<span class=\"pg_prev\"><a href=\"" + url.set("page", getPageIndex() - 1) + "\">Previous</a></span>");
            }
        } else {
            if (pattern.indexOf("%首页") != -1) {
                pattern = pattern.replaceAll("%首页", "<span class=\"pg_home_dis\">首页</span>");
            }
            if (pattern.indexOf("%Home") != -1) {
                pattern = pattern.replaceAll("%Home", "<span class=\"pg_home_dis\">Home</span>");
            }
            if (pattern.indexOf("%上一页") != -1) {
                pattern = pattern.replaceAll("%上一页", "<span class=\"pg_prev_dis\">上一页</span>");
            }
            if (pattern.indexOf("%Previous") != -1) {
                pattern = pattern.replaceAll("%Previous", "<span class=\"pg_prev_dis\">Previous</span>");
            }
        }

        if (pattern.indexOf("%numbers") != -1) {
            if (totalPage <= 0) totalPage = 1;
            StringBuffer pgStr = new StringBuffer();
            boolean has1 = pageIndex - 5 >= 1;
            boolean has2 = pageIndex + 5 <= totalPage;
            int begin = 1;
            int end = 1;
            if (has1 && has2) {
                begin = pageIndex - 4;
                end = pageIndex + 5;
            } else if (!has1 && has2) {
                begin = 1;
                end = totalPage >= 10 ? 10 : totalPage;
            } else if (has1 && !has2) {
                begin = totalPage - 9;
                begin = begin < 1 ? 1 : begin;
                end = totalPage;
            } else {
                begin = 1;
                end = totalPage;
            }
            for (int i = begin; i <= end; i++) {
                if (i == pageIndex) {
                    pgStr.append(" <span class=\"pg_num_current\" ><a href='" + url.set("page", i) + "'>[" + i + "]</a></span>");
                } else {
                    pgStr.append(" <span class=\"pg_num\" ><a href='" + url.set("page", i) + "'>" + i + "</a></span>");
                }
            }
            pattern = pattern.replaceAll("%numbers", pgStr.toString());
        }
        if (getPageIndex() < getTotalPage()) {
            if (pattern.indexOf("%下一页") != -1) {
                pattern = pattern.replaceAll("%下一页", "<span class=\"pg_next\"><a href=\"" + url.set("page", getPageIndex() + 1) + "\">下一页</a></span>");
            }
            if (pattern.indexOf("%Next") != -1) {
                pattern = pattern.replaceAll("%Next", "<span class=\"pg_next\"><a href=\"" + url.set("page", getPageIndex() + 1) + "\">Next</a></span>");
            }
            if (pattern.indexOf("%尾页") != -1) {
                pattern = pattern.replaceAll("%尾页", "<span class=\"pg_end\"><a href=\"" + url.set("page", getTotalPage()) + "\">尾页</a></span>");
            }
            if (pattern.indexOf("%End") != -1) {
                pattern = pattern.replaceAll("%End", "<span class=\"pg_end\"><a href=\"" + url.set("page", getPageIndex()) + "\">End</a></span>");
            }
        } else {
            if (pattern.indexOf("%下一页") != -1) {
                pattern = pattern.replaceAll("%下一页", "<span class=\"pg_next_dis\">下一页</span>");
            }
            if (pattern.indexOf("%Next") != -1) {
                pattern = pattern.replaceAll("%Next", "<span class=\"pg_next_dis\">Next</span>");
            }
            if (pattern.indexOf("%尾页") != -1) {
                pattern = pattern.replaceAll("%尾页", "<span class=\"pg_end_dis\">尾页</span>");
            }
            if (pattern.indexOf("%End") != -1) {
                pattern = pattern.replaceAll("%End", "<span class=\"pg_end_dis\">End</span>");
            }
        }
        if (pattern.indexOf("%select") != -1) {
            String s = "<select onchange=\"new comcon.Url().set('page', this.value).go()\" id='rxcms_page_select'><script type=\"text/javascript\">var $RXCMSPAGES=[];for(var i=1;i<="
                    + getTotalPage() + ";i++){$RXCMSPAGES.push('<option value=\"'+i+'\"');if(i==" + getPageIndex() + ")$RXCMSPAGES.push(' selected=\"selected\"');"
                    + "$RXCMSPAGES.push('>'+i+'</option>');} document.write($RXCMSPAGES.join(''));</script></select>\r\n";
            pattern = pattern.replaceAll("%select", s);
        }
        return pattern;
    }


    public Pager() {

    }


    public Pager(List<E> datas, int pageIndex, int pageSize, int totalPage, int totalRecord, String thisUrl) {
        this.datas = datas;
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
        this.totalPage = totalPage;
        this.totalRecord = totalRecord;
        this.thisUrl = thisUrl;
    }

}
