package lexer;

import javafx.util.Pair;

public class Main {
    public static void main(String[] args) {
        Lexer lexer = new Lexer();
        if (lexer.get_all_tokens("input.txt"))
        {
            lexer.output("log_output.txt");
            System.out.println("Done!");
        }
    }
}
