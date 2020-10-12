package lexer;

import static lexer.Consts.SYMBOL_TABLE_MAX;

public class Token {
    public TokenType type;
    public int row_pos;
    public int column_pos;
    public int symbol_table_index;

    public Token()
    {
        set_invalid();
    }

    public Token(Token anotherToken)
    {
        this.symbol_table_index = anotherToken.symbol_table_index;
        this.type = anotherToken.type;
        this.row_pos = anotherToken.row_pos;
        this.column_pos = anotherToken.column_pos;
    }

    public Token(TokenType type, int row, int col)
    {
        this.type = type;
        this.row_pos = row;
        this.column_pos = col;
        this.symbol_table_index = SYMBOL_TABLE_MAX;
    }

    public Token(TokenType type, int row, int col, int symbol_table_index)
    {
        this.type = type;
        this.row_pos = row;
        this.column_pos = col;
        this.symbol_table_index = symbol_table_index;
    }

    public void set_invalid()
    {
        type = TokenType.INVALID;
        row_pos = column_pos = symbol_table_index = SYMBOL_TABLE_MAX;
    }
}
