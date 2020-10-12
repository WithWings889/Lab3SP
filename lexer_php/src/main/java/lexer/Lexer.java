package lexer;

import javafx.util.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Lexer {
    private class CurrentLineState {
        int column = 0;
        int row = 0;
        String line = new String("");
        Token token = new Token();
    }

    private List<String> symbol_table = new ArrayList<String>();
    private List<Token> tokens = new ArrayList<Token>();
    private List<InvalidToken> invalid_tokens = new ArrayList<InvalidToken>();
    private CurrentLineState state = new CurrentLineState();

    private void handle_tag_error(TokenType type, int end_pos)
    {
        StringBuilder wordBuilder = new StringBuilder();
        while (state.column < end_pos)
        {
            wordBuilder.append(state.line.charAt(state.column));
            state.column += 1;
        }
        invalid_tokens.add(new InvalidToken("Invalid token ", wordBuilder.toString(), state.row, state.column));
    }

    private void get_dfa_token()
    {
        Pair<TokenType, Integer> token_from_dfa = DetFiniteAutomaton.check_value(state.line, state.column);
        if (token_from_dfa.getKey() == TokenType.INVALID)
            handle_tag_error(token_from_dfa.getKey(), token_from_dfa.getValue());
        else
        {
            tokens.add(new Token(token_from_dfa.getKey(), state.row, state.column));
            state.column = token_from_dfa.getValue();
        }
    }

    private void get_single_line_comment(TokenType type)
    {
        String comment = state.line.substring(state.column);
        int index = symbol_table.size();
        symbol_table.add(comment);
        tokens.add(new Token(type, state.row, state.column, index));
        state.column = state.line.length();
    }

    private void get_multiline_comment()
    {
        char c = 0;
        boolean prev_is_star = false;

        do
        {
            if (state.line.length() <= state.column)
                break;
            prev_is_star = c == '*';
            c = state.line.charAt(state.column);
            symbol_table.set(state.token.symbol_table_index, symbol_table.get(state.token.symbol_table_index) + c);
            state.column += 1;
        }
        while (state.column < state.line.length() && (!prev_is_star || c != '/'));

        if (prev_is_star && c == '/')
        {
            tokens.add(new Token(state.token));
            state.token.set_invalid();
        }
    }

    private void get_one_quote_string()
    {
        char c = 0;
        boolean prev_is_slash = false;
        do
        {
            if (state.line.length() == state.column)
                break;
            prev_is_slash = c == '\\';
            c = state.line.charAt(state.column);
            symbol_table.set(state.token.symbol_table_index, symbol_table.get(state.token.symbol_table_index) + c);
            state.column += 1;
        }
        while (state.column < state.line.length() && (prev_is_slash || c != '\''));

        if (!prev_is_slash && c == '\'')
        {
            tokens.add(new Token(state.token));
            state.token.set_invalid();
        }
        else
        {
            symbol_table.set(state.token.symbol_table_index, symbol_table.get(state.token.symbol_table_index) + "\\n");
        }
    }

    private void handle_tokens_in_two_quote()
    {
        boolean is_in_string = true;
        boolean normal_end = false;

        while (state.column < state.line.length() && is_in_string)
        {
            get_next_token(is_in_string, normal_end);
        }

        if (!normal_end)
        {
            if ( state.column < state.line.length())
            {
                String symbol = "" + state.line.charAt(state.column);
                invalid_tokens.add(new InvalidToken("Expected ", symbol, state.row, state.column));
            }
            else if (state.column >= state.line.length())
            {
                String symbol = "" + state.line.charAt(state.line.length()-1);
                invalid_tokens.add(new InvalidToken("Expected ", "}", state.row, state.column - 1));
            }
        }
    }

    private void init_two_quote_string()
    {
        int index = symbol_table.size();
        symbol_table.add(new String(""));
        state.token = new Token(TokenType.StringValueInTwoQuotes, state.row, state.column, index);
    }

    private void get_two_quote_string()
    {
        char c = 0;
        boolean prev_is_slash = false;
        do
        {
            prev_is_slash = c == '\\';
            if (state.line.length() <= state.column)
                break;
            c = state.line.charAt(state.column);
            if ((c == '{') && !prev_is_slash && state.column+1 < state.line.length())
            {
                c = state.line.charAt(state.column + 1);
                if (CharChecks.is_dollar(c))
                {
                    tokens.add(new Token(state.token));
                    tokens.add(new Token(TokenType.Concat, state.row, state.column));
                    state.token.set_invalid();
                    handle_tokens_in_two_quote();
                    tokens.add(new Token(TokenType.Concat, state.row, state.column));
                    init_two_quote_string();
                    if (state.column >= state.line.length())
                        break;
                }
                c = state.line.charAt(state.column);
            }
            symbol_table.set(state.token.symbol_table_index, symbol_table.get(state.token.symbol_table_index) + c);
            state.column += 1;
        }
        while (state.column < state.line.length() && (prev_is_slash || c != '"'));

        if (!prev_is_slash && c == '"')
        {
            String str = symbol_table.get(state.token.symbol_table_index);
            symbol_table.set(state.token.symbol_table_index, str.substring(0,str.length()-1));
            tokens.add(new Token(state.token));
            state.token.set_invalid();
        }
        else
        {
            symbol_table.set(state.token.symbol_table_index, symbol_table.get(state.token.symbol_table_index) + "\\n");
        }
    }

    private int in_key_words(String word)
    {
        int start = TokenType.Include.ordinal();
        int end = TokenType.XorWord.ordinal();
        for (int i = start;i <= end; ++i)
        {
            if (word.equals(Consts.TokenValue[i]))
                return i;
        }
        return Consts.SYMBOL_TABLE_MAX;
    }

    private int in_key_variables(String word)
    {
        int start = TokenType.This.ordinal();
        int end = TokenType.SESSION.ordinal();
        for (int i = start;i <= end; ++i)
        {
            if (word.equals(Consts.TokenValue[i]))
                return i;
        }
        return Consts.SYMBOL_TABLE_MAX;
    }

    private void handle_multiline_mode()
    {
        if (state.token.type == TokenType.MultiLineComment)
            get_multiline_comment();
        else if (state.token.type == TokenType.StringValueOneQuote)
            get_one_quote_string();
        else if (state.token.type == TokenType.StringValueInTwoQuotes)
            get_two_quote_string();
    }

    private void handle_number()
    {
        char c = state.line.charAt(state.column);
        if (CharChecks.is_dot(c) && (state.column+1 >= state.line.length() || 
                !CharChecks.is_number(state.line.charAt(state.column + 1))))
        {
            get_dfa_token();
            return;
        }
        int start_pos_number = state.column;
        boolean was_dot = false;
        StringBuilder num_value = new StringBuilder();
        boolean is_correct = true;
        do
        {
            c = state.line.charAt(state.column);
            if (!CharChecks.is_number(c) && !CharChecks.is_dot(c))
            {
                is_correct = false;
                break;
            }
            if (CharChecks.is_dot(c))
            {
                if (was_dot)
                    is_correct = false;
                else
                    was_dot = true;
            }
            if (is_correct)
            {
                num_value.append(c);
                state.column += 1;
            }
        }
        while (is_correct && state.column < state.line.length());

        int index = symbol_table.size();
        if (is_correct || CharChecks.is_correct_after_number(c))
        {
            symbol_table.add(num_value.toString());
            if (was_dot)
                tokens.add(new Token(TokenType.FloatValue, state.row, start_pos_number, index));
            else
                tokens.add(new Token(TokenType.IntValue, state.row, start_pos_number, index));
        }
        else
        {
            num_value.append(state.line.charAt(state.column));
            invalid_tokens.add(new InvalidToken("Unresolved symbol", num_value.toString(), state.row, state.column));
            state.column += 1;
        }
    }

    private void handle_comment()
    {
        char c = state.line.charAt(state.column);
        if (CharChecks.is_hash_tag_comment(c))
            get_single_line_comment(TokenType.SingleLineHashTagComment);
        else
        {
            c = state.line.charAt(state.column+1);
            if (c == '/')
                get_single_line_comment(TokenType.SingleLineSlashComment);
            else
            {
                int index = symbol_table.size();
                symbol_table.add("/*");
                state.token = new Token(TokenType.MultiLineComment, state.row, state.column, index);
                state.column += 2;

                get_multiline_comment();
            }
        }
    }

    private void handle_word()
    {
        int first_ind = state.column;
        char c;
        StringBuilder word = new StringBuilder();
        do
        {
            if (state.line.length() <= state.column)
                break;
            c = state.line.charAt(state.column);
            if (!CharChecks.is_word(c))
                break;
            word.append(c);
            state.column += 1;
        }
        while (state.column < state.line.length());
        String lower_case_word = word.toString();
        lower_case_word = lower_case_word.toLowerCase();

        int pos = in_key_words(lower_case_word);
        if (pos != Consts.SYMBOL_TABLE_MAX)
            tokens.add(new Token(TokenType.values()[pos], state.row, first_ind));
        else
        {
            int index = symbol_table.size();
            symbol_table.add(word.toString());
            tokens.add(new Token(TokenType.Identifier, state.row, first_ind, index));
        }
    }

    private void handle_one_quote_string()
    {
        state.token = new Token(TokenType.StringValueOneQuote, state.row, state.column, symbol_table.size());
        symbol_table.add("'");
        state.column += 1;
        get_one_quote_string();
    }

    private void handle_two_quote_string()
    {
        state.column += 1;
        if (state.column < state.line.length())
        {
            init_two_quote_string();
            get_two_quote_string();
        }
        else
        {
            init_two_quote_string();
            symbol_table.set(state.token.symbol_table_index, symbol_table.get(state.token.symbol_table_index) + "\\n");
        }
    }

    private void handle_variable()
    {
        char c;
        int first_in = state.column;
        StringBuilder word = new StringBuilder();
        do
        {
            c = state.line.charAt(state.column);
            if (!CharChecks.is_dollar(c))
                break;
            word.append(c);
            state.column += 1;
        }
        while (state.column < state.line.length());

        if (state.column == state.line.length() || !CharChecks.is_symbol(state.line.charAt(state.column)))
        {
            invalid_tokens.add(new InvalidToken("Invalid symbol for var name ", word.toString(), state.row, state.column));
            state.column += 1;
            return;
        }

        do
        {
            c = state.line.charAt(state.column);
            if (!CharChecks.is_word(c))
                break;
            word.append(c);
            state.column += 1;
        }
        while (state.column < state.line.length());

        int pos = in_key_variables(word.toString());
        if (pos != Consts.SYMBOL_TABLE_MAX)
            tokens.add(new Token(TokenType.values()[pos], state.row, first_in));
        else
        {
            int  index = symbol_table.size();
            symbol_table.add(word.toString());
            tokens.add(new Token(TokenType.DollarIdentifier, state.row, first_in, index));
        }
    }

    private void handle_punctuation()
    {
        StringBuilder word = new StringBuilder();
        word.append(state.line.charAt(state.column));
        int start = TokenType.Comma.ordinal();
        int end = TokenType.RBrace.ordinal();
        int pos = Consts.SYMBOL_TABLE_MAX;
        for (int i = start;i <= end; ++i)
        {
            if (word.toString().equals(Consts.TokenValue[i]))
            {
                pos = i;
                break;
            }
        }
        tokens.add(new Token(TokenType.values()[pos], state.row, state.column));
        state.column += 1;
    }

    private void get_next_token(Boolean is_in_string, Boolean normal_end)
    {
        if (state.token.type != TokenType.INVALID)
        {
            handle_multiline_mode();
            return;
        }


        while (state.column < state.line.length() && CharChecks.is_whitespace(state.line.charAt(state.column)))
            state.column += 1;

        if (state.line.length() <= state.column)
            return;
        if (is_in_string)
        {
            if (state.line.charAt(state.column) == '"')
            {
                is_in_string = false;
                normal_end = false;
                return;
            }
            if (state.line.charAt(state.column) == '}')
            {
                tokens.add(new Token(TokenType.RBrace, state.row, state.column));
                state.column += 1;
                is_in_string = false;
                normal_end = true;
                return;
            }
        }
        char current_symbol = state.line.charAt(state.column);
        if (CharChecks.is_number(current_symbol) || CharChecks.is_dot(current_symbol))
        {
            handle_number();
            return;
        }
        if (state.column+1 < state.line.length() &&
                CharChecks.is_comment(current_symbol, state.line.charAt(state.column+1)) ||
                CharChecks.is_hash_tag_comment(current_symbol))
        {
            handle_comment();
            return;
        }
        if (CharChecks.is_symbol(current_symbol))
        {
            handle_word();
            return;
        }
        if (CharChecks.is_single_quote_string(current_symbol))
        {
            handle_one_quote_string();
            return;
        }
        if (CharChecks.is_double_quote_string(current_symbol))
        {
            handle_two_quote_string();
            return;
        }
        if (CharChecks.is_dollar(current_symbol))
        {
            handle_variable();
            return;
        }
        if (CharChecks.is_operation(current_symbol))
        {
            get_dfa_token();
            return;
        }
        if (CharChecks.is_punctuation(current_symbol))
        {
            handle_punctuation();
            return;
        }
        StringBuilder symbol = new StringBuilder();
        symbol.append(state.line.charAt(state.column));
        invalid_tokens.add(new InvalidToken("Invalid symbol", symbol.toString(), state.row, state.column));
        state.column += 1;
    }

    public Lexer()
    {
        DetFiniteAutomaton.init_dfa_states();
    }

    public boolean get_all_tokens(String path_to_file)
    {
        try {
            File file = new File(path_to_file);
            Scanner reader = new Scanner(file);
            while (reader.hasNextLine()) {
                state.line = reader.nextLine();
                state.column = 0;
                Boolean is_in_string = false, normal_end = true;
                while (state.column < state.line.length()) {
                    get_next_token(is_in_string, normal_end);
                }
                state.row += 1;
            }
            if (state.token.type != TokenType.INVALID) {
                if (state.token.type == TokenType.StringValueInTwoQuotes)
                    invalid_tokens.add(new InvalidToken("End of two quote string not exists",
                            symbol_table.get(symbol_table.size()-1), state.row, state.column));
                else if (state.token.type == TokenType.StringValueOneQuote)
                    invalid_tokens.add(new InvalidToken("End of one quote string not exists",
                            symbol_table.get(symbol_table.size() - 1), state.row, state.column));
                else if (state.token.type == TokenType.MultiLineComment)
                    invalid_tokens.add(new InvalidToken("End of multi line comment not exists",
                            symbol_table.get(symbol_table.size() - 1), state.row, state.column));
                else
                    invalid_tokens.add(new InvalidToken("Unknown error occurred", "", state.row, state.column));
                symbol_table.remove(symbol_table.size() - 1);
            }
        }
        catch (FileNotFoundException e) {
            System.out.println("File doesn't find.");
            return false;
        }
        return true;
    }

    public void output(String path_to_file)
    {
        try {
            FileWriter writer = new FileWriter(path_to_file);
            PrintWriter out = new PrintWriter(writer);
            out.println("---------------------------Tokens list---------------------------");
            String colFormat22 = "%-24s";
            String colFormat4 = "%-2s";
            String colFormat6 = "%-6s";
            out.format(colFormat22, "Token type");
            out.format(colFormat4, "|row");
            out.print("|");
            out.format(colFormat6, "col|");
            out.println("symbol table index and value");
            out.println("-----------------------------------------------------------------");
            for (int i = 0; i < tokens.size(); ++i) {
                out.format(colFormat22, Consts.TokenValue[tokens.get(i).type.ordinal()]);
                out.format(colFormat4, tokens.get(i).row_pos);
                out.print("|");
                out.format(colFormat6, tokens.get(i).column_pos);

                if (tokens.get(i).symbol_table_index != Consts.SYMBOL_TABLE_MAX) {
                    out.print(tokens.get(i).symbol_table_index);
                    out.print(") |");
                    out.print(symbol_table.get(tokens.get(i).symbol_table_index));
                    out.print("|");
                }
                out.println();
            }
            out.println();
            out.println("---------------------------Invalid tokens---------------------------");
            String colFormat2 = "%-2s";
            String colFormat8 = "%-8s";
            out.format(colFormat2,"|row");
            out.print("|");
            out.format(colFormat8, "col|");
            out.println("Error explanation");
            out.println("--------------------------------------------------------------------");

            for (int i = 0; i < invalid_tokens.size(); ++i) {
                out.format(colFormat2, invalid_tokens.get(i).row_pos);
                out.print("|");
                out.format(colFormat8, invalid_tokens.get(i).column_pos);
                out.println(invalid_tokens.get(i).error_message + " |" + invalid_tokens.get(i).error_symbol + "|");
            }
            out.println();
            out.println("---------------------------Symbol table---------------------------");
            for (int i = 0; i < symbol_table.size(); ++i) {
                out.print(i);
                out.print(") |");
                out.print( symbol_table.get(i));
                out.println("|");
            }
            out.flush();
            out.close();
            writer.close();
        }
        catch (IOException e) {
            System.out.println("IO exception occured");
            e.printStackTrace();
        }
    }
}
