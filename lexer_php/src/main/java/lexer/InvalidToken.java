package lexer;

public class InvalidToken {
    public String error_message;
    public String error_symbol;
    public int row_pos;
    public int column_pos;

    public InvalidToken() {}

    public InvalidToken(String message, String symbol, int row, int col)
    {
        this.error_message = message;
        this.error_symbol = symbol;
        this.row_pos = row;
        this.column_pos = col;
    }
}
