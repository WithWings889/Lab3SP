package lexer;

public class CharChecks {
    private CharChecks() {}

    public static boolean is_number(char c)
    {
        return ('0' <= c && c <= '9');
    }

    public static boolean is_symbol(char c)
    {
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || c == '_';
    }

    public static boolean is_dot(char c)
    {
        return c == '.';
    }

    public static boolean is_whitespace(char c)
    {
        return c == ' ';
    }

    public static boolean is_punctuation(char c)
    {
        String punctuation = ",;[]{}()";
        return punctuation.indexOf(c) != -1;
    }

    public static boolean is_operation(char c)
    {
        String operators = "+-*/%^|&=<>!?:";
        return operators.indexOf(c) != -1;
    }

    public static boolean is_hash_tag_comment(char c)
    {
        return c == '#';
    }

    public static boolean is_correct_after_number(char c)
    {
        return is_punctuation(c) || is_hash_tag_comment(c) || is_whitespace(c) || is_operation(c);
    }

    public static boolean is_dollar(char c)
    {
        return c == '$';
    }

    public static boolean is_comment(char c1, char c2)
    {
        return c1 == '/' && (c2 == '/' || c2 == '*');
    }

    public static boolean is_word(char c)
    {
        return is_symbol(c) || is_number(c);
    }

    public static boolean is_single_quote_string(char c)
    {
        return c == '\'';
    }

    public static boolean is_double_quote_string(char c)
    {
        return c == '"';
    }

    public static boolean is_bracket(char c)
    {
        return c == '[' || c == ']';
    }

    public static boolean is_arrow(char c1, char c2)
    {
        return c1 == '-' && c2 == '>';
    }

    public static boolean is_close_brace(char c)
    {
        return c == '}';
    }

    public static boolean is_tag_after_punctuation(char c)
    {
        return c == ' ' || c == ';';
    }
}
