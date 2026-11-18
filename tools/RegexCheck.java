public class RegexCheck {
    public static void main(String[] args) {
        String p1 = "rc\\.push\\(\\s*\\{([^}]*)}";
        String p2 = "rc\\.push\\((\\s*\\{([^}]*)}";
        for (String[] pair : new String[][]{{"p1", p1}, {"p2", p2}}) {
            System.out.println(pair[0] + " = " + pair[1]);
            try {
                java.util.regex.Pattern.compile(pair[1]);
                System.out.println(pair[0] + " OK");
            } catch (Exception e) {
                System.out.println(pair[0] + " ERR: " + e.getMessage());
            }
        }
    }
}
