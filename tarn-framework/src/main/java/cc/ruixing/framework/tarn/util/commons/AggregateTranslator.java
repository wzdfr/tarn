package cc.ruixing.framework.tarn.util.commons;

import java.io.IOException;
import java.io.Writer;

public class AggregateTranslator extends CharSequenceTranslator
{
  private final CharSequenceTranslator[] translators;

  public AggregateTranslator(CharSequenceTranslator[] translators)
  {
    this.translators = ((CharSequenceTranslator[]) ArrayUtils.clone(translators));
  }

  public int translate(CharSequence input, int index, Writer out)
    throws IOException
  {
    for (CharSequenceTranslator translator : this.translators) {
      int consumed = translator.translate(input, index, out);
      if (consumed != 0) {
        return consumed;
      }
    }
    return 0;
  }
}