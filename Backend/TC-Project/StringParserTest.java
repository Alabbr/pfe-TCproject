public class StringParserTest {
    public static void main(String[] args) {
        String json = "{\n  \"text\": \"Hello \\\"world\\\"!\\nThis is a test.\"\n}";
        System.out.println("JSON: " + json);
        System.out.println("Extracted: " + extractTextFromJson(json));
    }
    
    private static String extractTextFromJson(String json) {
        String key = "\"text\":";
        int index = json.indexOf(key);
        if (index == -1) {
            key = "\"text\": ";
            index = json.indexOf(key);
        }
        if (index != -1) {
            int startQuote = json.indexOf("\"", index + key.length());
            if (startQuote != -1) {
                StringBuilder extracted = new StringBuilder();
                boolean escaped = false;
                for (int i = startQuote + 1; i < json.length(); i++) {
                    char c = json.charAt(i);
                    if (escaped) {
                        extracted.append(c);
                        escaped = false;
                    } else {
                        if (c == '\\') {
                            escaped = true;
                            extracted.append(c);
                        } else if (c == '"') {
                            break;
                        } else {
                            extracted.append(c);
                        }
                    }
                }
                return extracted.toString()
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");
            }
        }
        return "";
    }
}
