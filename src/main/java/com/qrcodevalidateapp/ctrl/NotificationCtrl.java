package com.qrcodevalidateapp.ctrl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

@RestController
@RequestMapping(value = "/qrcode")
public class NotificationCtrl {

	@Autowired
	RestTemplate restTemplate;

	public static File getFileFromClassPath() throws FileNotFoundException {
//		File file = ResourceUtils.getFile("classpath:sysFile.txt");
		File file = new File("sysFile.txt");
		return file;
	}

	public String fileRead(File file) {
		String fileData = "";
		try {
			byte[] data = Files.readAllBytes(file.toPath());
			fileData = new String(data);
		} catch (IOException e) {
			System.out.println(e);
		}
		return fileData;
	}

	public void fileWrite(File file, String data) {
		try {
			Files.write(file.toPath(), data.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	public String base64Decoder(String data) throws FileNotFoundException {
		byte[] valueDecoded = Base64.getDecoder().decode(data.getBytes());
		return new String(valueDecoded);
	}

	public String base64Encoder(String data) throws FileNotFoundException {
		byte[] valueDecoded = Base64.getEncoder().encode(data.getBytes());
		return new String(valueDecoded);
	}

	@CrossOrigin(origins = "*")
	@GetMapping(value = "/emailNotificationOff")
	public ResponseEntity emailNotificationOff() throws FileNotFoundException {
		try {

			String strEncodeFile = fileRead(getFileFromClassPath());
			String strDecodedFile = base64Decoder(strEncodeFile);

			JsonElement element = JsonParser.parseString(strDecodedFile);
			JsonObject obj = element.getAsJsonObject();
			JsonObject appProp = obj.get("appProp").getAsJsonObject();
			String strSchedulerKey = appProp.get("schedulerKey").getAsString();

			if (!strSchedulerKey.equals("false")) {
				appProp.addProperty("schedulerKey", "false");
				Gson gson = new Gson();
				String encodedStr = base64Encoder(gson.toJson(obj));
				fileWrite(getFileFromClassPath(), encodedStr);
			}
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed");
		}
		return ResponseEntity.ok("Success");
	}

	@CrossOrigin(origins = "*")
	@GetMapping(value = "/emailNotificationOn")
	public ResponseEntity emailNotificationOn() throws FileNotFoundException {
		try {
			String strEncodeFile = fileRead(getFileFromClassPath());
			String strDecodedFile = base64Decoder(strEncodeFile);

			JsonElement element = JsonParser.parseString(strDecodedFile);
			JsonObject obj = element.getAsJsonObject();
			JsonObject appProp = obj.get("appProp").getAsJsonObject();
			String strSchedulerKey = appProp.get("schedulerKey").getAsString();

			if (!strSchedulerKey.equals("true")) {
				appProp.addProperty("schedulerKey", "true");
				Gson gson = new Gson();
				String encodedStr = base64Encoder(gson.toJson(obj));
				fileWrite(getFileFromClassPath(), encodedStr);
			}
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed");
		}
		return ResponseEntity.ok("Success");
	}

	@Scheduled(fixedDelay = 43200000) // 15min 900000  
	public void scheduleFixedDelayTask() throws NotFoundException, IOException {
		File file = getFileFromClassPath();
		String strFileContent = fileRead(file);
		String decodedFile = base64Decoder(strFileContent);

		JsonElement element = JsonParser.parseString(decodedFile);
		JsonObject obj = element.getAsJsonObject();
		JsonObject appProp = obj.get("appProp").getAsJsonObject();
		String scheduleKey = appProp.get("schedulerKey").getAsString();

		if (scheduleKey.equals("true")) {
//			File file1 = ResourceUtils.getFile("classpath:mmv-qr--httpstinyurl.com5ctwd9dd.png");
			File file1 = new File("mmv-qr--httpstinyurl.com5ctwd9dd.png");
			
			String decodedText = decodeQRCode(file1);
			if (decodedText == null) {
				System.out.println("No QR Code found in the image");
			} else {
				String validateUrl = validateUrl(decodedText);
				if (validateUrl.equals("Success")) {
					sendNotificationMailWithSubAndCont("*** QR-Code Alert | Working Fine ***",
							"Hi Buddy, your qr code is valid and working fine.");
				} else {
					sendNotificationMailWithSubAndCont("*** QR-Code Alert | something went wrong ***",
							"Hi Buddy, your qr code was not  working please contact the support team.");
				}
			}
		}
	}

	private static String decodeQRCode(File qrCodeimage) throws IOException, NotFoundException {
		BufferedImage bufferedImage = ImageIO.read(qrCodeimage);
		LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
		try {
			Result result = new MultiFormatReader().decode(bitmap);
			return result.getText();
		} catch (NotFoundException e) {
			System.out.println("There is no QR code in the image");
			return null;
		}
	}

	public String sendNotificationMailWithSubAndCont(String Sub, String Cont) throws FileNotFoundException {

		String strEncodeFile = fileRead(getFileFromClassPath());
		String strDecodedFile = base64Decoder(strEncodeFile);
		JsonElement element = JsonParser.parseString(strDecodedFile);
		JsonObject obj = element.getAsJsonObject();
		JsonObject mailProp = obj.get("mailProp").getAsJsonObject();
		String resp = "Success";
		try {
			String from = mailProp.get("senderMailId").getAsString();
			String pass = mailProp.get("senderMailPassKey").getAsString();

			String host = "smtp.gmail.com";
			Properties properties = System.getProperties();
			properties.put("mail.smtp.host", host);
			properties.put("mail.smtp.port", "587");
			properties.put("mail.smtp.starttls.enable", "true");
			properties.put("mail.smtp.auth", "true");

			Session session = Session.getInstance(properties, new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(from, pass);
				}
			});
			MimeMessage message = new MimeMessage(session);

			message.setRecipients(Message.RecipientType.TO, "ajayjw@gmail.com");

			message.setSubject(Sub);
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			helper.setText(Cont, true);
			System.out.println("sending...");
			Transport.send(message);
			System.out.println("Sent message successfully....");

		} catch (Exception e) {
			System.out.println("" + e.toString());
			resp = e.toString();
		}
		return resp;
	}

	public String validateUrl(String url) throws IOException {
		String returnResp = "";
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
			HttpEntity<String> entity = new HttpEntity<String>(headers);

			HttpHeaders respondsHeader = restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getHeaders();
			String respondsStr = respondsHeader.toString();
			if (respondsStr.contains("c404bd6f9c4042e08d8dd17a1ea74236")) {
				returnResp = "Success";
			} else {
				returnResp = "Failed";
			}
		} catch (Exception ex) {
			returnResp = "Error";
		}
		return returnResp;
	}

	public static void main(String arge[]) throws NotFoundException, IOException {
		NotificationCtrl nc = new NotificationCtrl();
		nc.scheduleFixedDelayTask();

	}

}
