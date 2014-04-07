/* Generated By:JavaCC: Do not edit this line. MFileParserConstants.java */
package gov.redhawk.mfile.parser;


/**
 * Token literal values and constants.
 * Generated by org.javacc.parser.OtherFilesGen#start()
 */
public interface MFileParserConstants {

  /** End of File. */
  int EOF = 0;
  /** RegularExpression Id. */
  int SINGLE_LINE_COMMENT = 3;
  /** RegularExpression Id. */
  int SINGLE_LINE_COMMENT_2 = 4;
  /** RegularExpression Id. */
  int MULTI_LINE_COMMENT = 6;
  /** RegularExpression Id. */
  int MULTI_LINE_COMMENT_2 = 9;
  /** RegularExpression Id. */
  int FUNCTION = 11;
  /** RegularExpression Id. */
  int NL = 19;
  /** RegularExpression Id. */
  int ID = 20;
  /** RegularExpression Id. */
  int OCTALINT = 21;
  /** RegularExpression Id. */
  int DECIMALINT = 22;
  /** RegularExpression Id. */
  int FIXED_PT = 23;
  /** RegularExpression Id. */
  int HEXADECIMALINT = 24;
  /** RegularExpression Id. */
  int FLOATONE = 25;
  /** RegularExpression Id. */
  int FLOATTWO = 26;
  /** RegularExpression Id. */
  int CHARACTER = 27;
  /** RegularExpression Id. */
  int STRING = 28;

  /** Lexical state. */
  int DEFAULT = 0;
  /** Lexical state. */
  int IN_MULTI_LINE_COMMENT = 1;
  /** Lexical state. */
  int IN_MULTI_LINE_COMMENT_2 = 2;

  /** Literal token values. */
  String[] tokenImage = {
    "<EOF>",
    "\" \"",
    "\"\\t\"",
    "<SINGLE_LINE_COMMENT>",
    "<SINGLE_LINE_COMMENT_2>",
    "\"%{\"",
    "\"%}\"",
    "<token of kind 7>",
    "\"#{\"",
    "\"#}\"",
    "<token of kind 10>",
    "\"function\"",
    "\"[\"",
    "\"]\"",
    "\"=\"",
    "\"(\"",
    "\",\"",
    "\")\"",
    "\";\"",
    "<NL>",
    "<ID>",
    "<OCTALINT>",
    "<DECIMALINT>",
    "<FIXED_PT>",
    "<HEXADECIMALINT>",
    "<FLOATONE>",
    "<FLOATTWO>",
    "<CHARACTER>",
    "<STRING>",
  };

}
