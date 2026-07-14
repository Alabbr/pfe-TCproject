import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;

public class JacksonTest {
    public static void main(String[] args) throws Exception {
        String json = "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"hello world\"}]}}]}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        String text = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        System.out.println("Extracted: " + text);
    }
}
