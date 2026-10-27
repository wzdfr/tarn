package cc.ruixing.framework.tarn.util.commons;

import cc.ruixing.framework.tarn.core.castor.EntityArrays;

public class StringEscapeUtils
{
  public static final CharSequenceTranslator ESCAPE_HTML4 = new AggregateTranslator(new CharSequenceTranslator[] { new LookupTranslator(EntityArrays.BASIC_ESCAPE()),
    new LookupTranslator(EntityArrays.ISO8859_1_ESCAPE()), new LookupTranslator(EntityArrays.HTML40_EXTENDED_ESCAPE()) });

  public static final CharSequenceTranslator UNESCAPE_HTML4 = new AggregateTranslator(new CharSequenceTranslator[] {
    new LookupTranslator(EntityArrays.BASIC_UNESCAPE()), new LookupTranslator(EntityArrays.ISO8859_1_UNESCAPE()),
    new LookupTranslator(EntityArrays.HTML40_EXTENDED_UNESCAPE()), new NumericEntityUnescaper(new NumericEntityUnescaper.OPTION[0]) });

  public static final String escapeHtml4(String input)
  {
    return ESCAPE_HTML4.translate(input);
  }

  public static final String unescapeHtml4(String input)
  {
    return UNESCAPE_HTML4.translate(input);
  }
}