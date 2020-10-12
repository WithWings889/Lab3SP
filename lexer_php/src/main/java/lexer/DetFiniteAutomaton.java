package lexer;

import java.util.*;

import javafx.util.Pair;

public class DetFiniteAutomaton {
    static class DFAState
    {
        TokenType type = TokenType.INVALID;
        char c = '\0';
        List<DFAState> next_state = new ArrayList<DFAState>();
    };

    private static DFAState root = new DFAState();
    
    private DetFiniteAutomaton() {};

    private static void init_dfs(DFAState state, List<Pair<String, TokenType>> token_list, int depth)
    {
        int i = 0;
        do
        {
            if (token_list.get(i).getKey().length() > depth)
            {
                List<Pair<String, TokenType> > next_list = new ArrayList<Pair<String, TokenType>>();
                DFAState next_state = new DFAState();
                if (depth+1 == token_list.get(i).getKey().length())
                    next_state.type = token_list.get(i).getValue();
                else
                {
                    next_state.type = TokenType.INVALID;
                    next_list.add(token_list.get(i));
                }
                next_state.c = token_list.get(i).getKey().charAt(depth);

                int j = i+1;
                while (j < token_list.size() && token_list.get(j).getKey().length() > depth + 1)
                {
                    if (next_state.c == token_list.get(j).getKey().charAt(depth))
                        next_list.add(token_list.get(j));
                    else
                        break;
                    ++j;
                }
                if (!next_list.isEmpty())
                    init_dfs(next_state, next_list, depth + 1);

                state.next_state.add(next_state);
                i = j;
            }
        }
        while (i < token_list.size());
    }

    private static void rec_output(DFAState root, int depth)
    {
        for (int i = 0;i < depth; ++i)
            System.out.print("  ");
        System.out.print(root.c);
        System.out.print(" ");
        System.out.print(Consts.TokenValue[root.type.ordinal()]);
        System.out.print("\n");
        for (int i = 0;i < root.next_state.size(); ++i)
            rec_output(root.next_state.get(i), depth+1);
    }
    
    private static Pair check_dfs(DFAState state, String code, int pos)
    {
        if (code.length() <= pos)
            return new Pair(state.type, pos);
        for (int i = 0;i < state.next_state.size(); ++i)
            if (state.next_state.get(i).c == code.charAt(pos))
                return check_dfs(state.next_state.get(i), code, pos+1);
        return new Pair(state.type, pos);
    }

    public static void init_dfa_states()
    {
        List<Pair<String, TokenType> > dfa_tokens_list = new ArrayList<Pair<String, TokenType>>();
        int start = TokenType.Add.ordinal();
        int end = TokenType.SingleLineSlashComment.ordinal();
        for (int i = start;i < end; ++i)
            dfa_tokens_list.add(new Pair(Consts.TokenValue[i], TokenType.values()[i]));

        Collections.sort(dfa_tokens_list, new MyComparator());

        init_dfs(root, dfa_tokens_list, 0);
    }

    public static Pair<TokenType, Integer> check_value(String code, int start_pos)
    {
        return check_dfs(root, code, start_pos);
    }

    public static void output_dfa_states()
    {
        rec_output(root, 0);
    }

}


class MyComparator implements Comparator<Pair<String, TokenType> > {
    @Override
    public int compare(Pair<String, TokenType> a, Pair<String, TokenType> b) {
        return a.getKey().compareToIgnoreCase(b.getKey());
    }
}