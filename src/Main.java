import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class Main {
    public static void main(String[] args) {
        try {
            // 오늘 날짜를 yyyy/MM/dd 형식으로 생성
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String formattedDate = today.format(formatter);

            // API URL에 날짜 추가
            String apiUrl = "https://open.neis.go.kr/hub/mealServiceDietInfo/?ATPT_OFCDC_SC_CODE=E10&SD_SCHUL_CODE=7341089&KEY=37cdc168c38a44779bdd17a51ea4a797&MLSV_YMD=" + formattedDate;

            // API 요청 및 데이터 파싱
            String parsedData = fetchAndParseApiData(apiUrl);

            // 텍스트 데이터를 이미지로 렌더링
            Path imagePath = createImageFromText(parsedData);

            // Windows 바탕화면으로 설정
            setDesktopWallpaper(imagePath);

            System.out.println("바탕화면이 설정되었습니다!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String fetchAndParseApiData(String apiUrl) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // XML 응답 처리 및 <DDISH_NM> 값 추출
            return extractDdishNm(response.toString());
        } else {
            throw new IOException("API 호출 실패. 응답 코드: " + responseCode);
        }
    }

    private static String extractDdishNm(String xmlResponse) {
        try {
            // XML 문자열을 Document 객체로 변환
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes("UTF-8")));

            // <DDISH_NM> 태그 찾기
            NodeList nodeList = document.getElementsByTagName("DDISH_NM");

            // 결과를 하나의 문자열로 병합
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < nodeList.getLength(); i++) {
                String rawContent = nodeList.item(i).getTextContent().trim();

                // 소괄호와 내용 제거: 정규식 사용
                String cleanedContent = rawContent.replaceAll("\\(.*?\\)", "");

                // <br/> 및 줄바꿈(\n, \r\n)을 ','로 대체
                cleanedContent = cleanedContent.replaceAll("<br\\s*/?>|[\\r\\n]+", ",");

                result.append(cleanedContent).append("\n");
            }
            return result.toString().trim(); // 최종 문자열 반환
        } catch (Exception e) {
            e.printStackTrace();
            return "데이터 파싱 오류!";
        }
    }

    private static Path createImageFromText(String text) throws IOException {
        // 이미지 크기 설정
        int width = 1980;
        int height = 1080;

        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        String formattedDate = today.format(formatter);

        // 흰색 배경 BufferedImage 생성
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 배경 색상 설정
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        g.setColor(Color.BLACK);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 60));

        String header = "오늘점심메뉴";
        g.drawString(header, 700, 300);

        g.setColor(Color.BLACK);
        g.setFont(new Font("맑은 고딕", Font.PLAIN, 40));

        String header2 = "(오늘일자 : " + formattedDate +")";
        g.drawString(header2, 1100, 300);

        // 텍스트 색상 및 폰트 설정
        g.setColor(Color.BLACK);
        g.setFont(new Font("맑은 고딕", Font.PLAIN, 60));


        // 텍스트 렌더링
        int y = 400; // 시작 y 좌표
        for (String line : text.split(",")) {
            g.drawString(line.trim(), 700, y);
            y += 60; // 줄 간격
        }

        g.setColor(Color.gray);
        g.setFont(new Font("맑은 고딕", Font.PLAIN, 30));

        String header3 = "Made by 조윤채";
        g.drawString(header3, 700, 900);

        g.dispose();

        // 이미지 저장
        Path imagePath = Path.of(System.getProperty("user.home"), "Desktop", "api_wallpaper.bmp");
        ImageIO.write(image, "bmp", imagePath.toFile());
        return imagePath;
    }

    private static void setDesktopWallpaper(Path imagePath) throws IOException, InterruptedException {
        // Windows 명령어를 통해 바탕화면 설정
        String command = "reg add \"HKEY_CURRENT_USER\\Control Panel\\Desktop\" /v Wallpaper /t REG_SZ /d \"" +
                imagePath.toString() + "\" /f && RUNDLL32.EXE user32.dll,UpdatePerUserSystemParameters";
        Process process = Runtime.getRuntime().exec("cmd /c " + command);
        process.waitFor();
    }
}
