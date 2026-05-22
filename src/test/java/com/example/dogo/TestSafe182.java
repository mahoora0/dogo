import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestSafe182 {
    public static void main(String[] args) throws Exception {
        String url = "https://www.safe182.go.kr/api/lcm/amberList.do";
        String esntlId = "10000945"; 
        String authKey = "4b704b46c661430b";

        String formData = "esntlId=" + esntlId +
                          "&authKey=" + authKey +
                          "&rowSize=10" +
                          "&page=1" +
                          "&occrde=20260521" +
                          "&xmlUseYN=Y";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();
        
        int totalIndex = body.indexOf("totalCount");
        if(totalIndex != -1) {
            System.out.println("Total Count info: " + body.substring(totalIndex - 10, totalIndex + 40));
        }
        
        int firstOccrde = body.indexOf("<occrde>");
        int endOccrde = body.indexOf("</occrde>", firstOccrde);
        if(firstOccrde != -1) {
            System.out.println("First occrde: " + body.substring(firstOccrde + 8, endOccrde));
        }
    }
}
