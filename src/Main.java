import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
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
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String formattedDate = today.format(formatter);

            // === 급식 API 호출 ===
            String mealApi = "https://open.neis.go.kr/hub/mealServiceDietInfo/?ATPT_OFCDC_SC_CODE=E10&SD_SCHUL_CODE=7341089&KEY=2a84b7bf39f64c35b6e73a19fe2202bb&MLSV_YMD=" + formattedDate;
            String mealData = fetchAndParseApiData(mealApi, "DDISH_NM");

            // === 시간표 API 호출 ===
            String timetableApi = "https://open.neis.go.kr/hub/misTimetable?ATPT_OFCDC_SC_CODE=E10&SD_SCHUL_CODE=7341089&KEY=2a84b7bf39f64c35b6e73a19fe2202bb&CLASS_NM=6&GRADE=3&TI_FROM_YMD=" + formattedDate + "&TI_TO_YMD=" + formattedDate;
            String timetableData = fetchAndParseApiData(timetableApi, "ITRT_CNTNT");

            // === 이미지 생성 ===
            Path imagePath = createImageFromText(mealData, timetableData);

            // === 바탕화면 설정 ===
            setDesktopWallpaper(imagePath);

            System.out.println("✅ 바탕화면이 급식 + 시간표로 설정되었습니다!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // XML 파싱 (공용)
    private static String fetchAndParseApiData(String apiUrl, String tagName) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return extractXmlTag(response.toString(), tagName);
        } else {
            throw new IOException("API 호출 실패. 응답 코드: " + responseCode);
        }
    }

    // XML에서 특정 태그 값 추출
    private static String extractXmlTag(String xmlResponse, String tagName) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes("UTF-8")));

            NodeList nodeList = document.getElementsByTagName(tagName);

            StringBuilder result = new StringBuilder();
            for (int i = 0; i < nodeList.getLength(); i++) {
                String text = nodeList.item(i).getTextContent().trim();

                // 급식인 경우에는 (원산지)나 () 괄호 제거
                if (tagName.equals("DDISH_NM")) {
                    text = text.replaceAll("\\(.*?\\)", "").replaceAll("<br\\s*/?>|[\\r\\n]+", ",");
                }
                result.append(text).append(",");
            }
            return result.toString().replaceAll(",$", "");
        } catch (Exception e) {
            e.printStackTrace();
            return "데이터 파싱 오류!";
        }
    }

    // 이미지 생성
    private static Path createImageFromText(String mealText, String timetableText) throws IOException {
        int width = 1980;
        int height = 1080;

        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        String formattedDate = today.format(formatter);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 배경 흰색
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // 제목
        g.setColor(Color.BLACK);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 80));
        g.drawString("점심메뉴 & 시간표", 650, 200);
        g.setFont(new Font("맑은 고딕", Font.PLAIN, 40));
        g.drawString("(오늘일자 : " + formattedDate +")", 770, 250);

        // === 급식 출력 ===
        g.setFont(new Font("맑은 고딕", Font.BOLD, 70));
        g.drawString("오늘점심메뉴", 400, 350);
        g.setFont(new Font("맑은 고딕", Font.PLAIN, 75));
        int y = 450;
        for (String line : mealText.split(",")) {
            g.drawString(line.trim(), 390, y);
            y += 85;
        }

        // === 시간표 출력 ===
        g.setFont(new Font("맑은 고딕", Font.BOLD, 70));
        g.drawString("시간표", 1100, 350);
        g.setFont(new Font("맑은 고딕", Font.PLAIN, 75));
        int y2 = 450;
        int idx = 1;
        for (String line : timetableText.split(",")) {
            g.drawString(idx + "교시: " + line.trim(), 1100, y2);
            y2 += 85;
            idx++;
        }

        g.setColor(Color.GRAY);
        g.setFont(new Font("맑은 고딕", Font.PLAIN, 30));
        g.drawString("Made by 조윤채", 1700, 1000);

        g.dispose();

        Path imagePath = Path.of(System.getProperty("user.home"), "Desktop", "api_wallpaper.bmp");
        ImageIO.write(image, "bmp", imagePath.toFile());
        return imagePath;
    }

    private static void setDesktopWallpaper(Path imagePath) throws IOException, InterruptedException {
        String command = "reg add \"HKEY_CURRENT_USER\\Control Panel\\Desktop\" /v Wallpaper /t REG_SZ /d \"" +
                imagePath.toString() + "\" /f && RUNDLL32.EXE user32.dll,UpdatePerUserSystemParameters";
        Process process = Runtime.getRuntime().exec("cmd /c " + command);
        process.waitFor();
    }
}

//ⓒ 2024. 조윤채 All rights reserved.
//메일:kpnkpn1324@gmail.com
//본 저작권은 조윤채에게 있습니다
//2차 수정 및 배포 금지
//허락없이 수정은 죽빵을 맞을 수 있습니다
