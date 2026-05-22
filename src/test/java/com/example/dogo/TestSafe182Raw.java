import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestSafe182Raw {
    public static void main(String[] args) throws Exception {
        String url = "https://www.safe182.go.kr/api/lcm/findChildList.do" +
                     "?esntlId=10000945&authKey=4b704b46c661430b&rowSize=100&page=1&xmlUseYN=Y" +
                     "&detailDate1=2026-05-01&detailDate2=2026-05-21";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        byte[] bodyBytes = response.body();
        
        String contentType = response.headers().firstValue("content-type").orElse("");
        System.out.println("Content-Type: " + contentType);
        
        String body = new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8);
        System.out.println("Total response length (UTF-8): " + body.length());
        
        // Let's check if there's any Korean innm, etc. If it looks broken, try EUC-KR
        if (body.contains("nm") && (body.contains("") || body.contains("?"))) {
            String bodyEuc = new String(bodyBytes, java.nio.charset.Charset.forName("EUC-KR"));
            if (!bodyEuc.contains("")) {
                body = bodyEuc;
                System.out.println("Using EUC-KR decoding!");
            }
        }
        
        int itemCount = 0;
        int index = 0;
        while ((index = body.indexOf("<item>", index)) != -1) {
            itemCount++;
            index += 6;
        }
        System.out.println("Number of <item> tags: " + itemCount);
        
        if (itemCount > 0) {
            int firstItemStart = body.indexOf("<item>");
            int firstItemEnd = body.indexOf("</item>");
            String firstItem = body.substring(firstItemStart, firstItemEnd + 7);
            
            // Remove or truncate the huge base64 string inside <tknphotoFile> for clean display
            if (firstItem.contains("<tknphotoFile>")) {
                int photoStart = firstItem.indexOf("<tknphotoFile>");
                int photoEnd = firstItem.indexOf("</tknphotoFile>");
                firstItem = firstItem.substring(0, photoStart + 14) + "...[BASE64_IMAGE_DATA_TRUNCATED]..." + firstItem.substring(photoEnd);
            }
            System.out.println("First item (cleaned):");
            System.out.println(firstItem);
        } else {
            System.out.println("Response: " + body);
        }
    }
}
