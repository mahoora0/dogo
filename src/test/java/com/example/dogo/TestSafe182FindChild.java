import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestSafe182FindChild {
    public static void main(String[] args) throws Exception {
        String url = "https://www.safe182.go.kr/api/lcm/findChildList.do" +
                     "?esntlId=10000945&authKey=4b704b46c661430b&rowSize=10&page=1&xmlUseYN=Y";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body().substring(0, Math.min(response.body().length(), 500)));
    }
}
