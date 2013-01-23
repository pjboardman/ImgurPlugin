import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.cordova.api.CallbackContext;
import org.apache.cordova.api.CordovaPlugin;
import org.apache.cordova.api.LOG;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;


public class ImgurPlugin extends CordovaPlugin {
	
	private static final String IMGUR_POSTURI = "http://api.imgur.com/2/upload";

	@Override
	public boolean execute(String action, JSONArray args,
			CallbackContext callbackContext) throws JSONException {
		
		if ("upload".equalsIgnoreCase(action)){
			JSONObject params = args.getJSONObject(0);
			uploadFile(params, callbackContext);
			return true;
		}
		
		return false;
	}
	
	void uploadFile(JSONObject params, CallbackContext callbackContext){
		
		try{
			HttpResponse response = streamFileToRequest(params);
			String imgUrl = getImageUrl(response);
		
			JSONObject result = new JSONObject();
			result.put("url", imgUrl);
			callbackContext.success(result);
			
		} catch(Exception e){
			LOG.e("ImgurPlugin", "Error uploading image", e);
			callbackContext.error(e.getMessage());
		}
	}
	
	private String getImageUrl(HttpResponse response) 
			throws IllegalStateException, IOException, ParserConfigurationException, SAXException {
		
		String xmlResponse = getResponseContent(response);
		return getImageUrlFromXml(xmlResponse);
	}

	private String getImageUrlFromXml(String xmlResponse)
			throws ParserConfigurationException, SAXException, IOException {
		
		Document doc = getXmlDoc(xmlResponse);
		Element locationElement = (Element) doc.getElementsByTagName("original").item(0);
		Text text = (Text)locationElement.getChildNodes().item(0);
		return text.getData();
	}
	
	private Document getXmlDoc(String source) throws ParserConfigurationException, SAXException, IOException{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		InputSource input = new InputSource();
		
		input.setCharacterStream(new StringReader(source));
		return builder.parse(input);
		
	}
	
	private String getResponseContent(HttpResponse response) throws IllegalStateException, IOException{
		
        InputStream content = response.getEntity().getContent();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final int BUF_SIZE = 1 << 8; // 1KiB buffer
        byte[] buffer = new byte[BUF_SIZE];
        int bytesRead = -1;
        while ((bytesRead = content.read(buffer)) > -1) {
        	outputStream.write(buffer, 0, bytesRead);
        }
        content.close();
        System.out.println(outputStream.toString());
        return outputStream.toString();
	}

	private HttpResponse streamFileToRequest(JSONObject params)
			throws JSONException, UnsupportedEncodingException, IOException, ClientProtocolException {
		String fileLocation = GetFileUri(params.getString("file"));
		String key = params.getString("key");
		
		MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        entity.addPart("key", new StringBody(key));
        entity.addPart("image", new FileBody(new File(fileLocation)));
        
        HttpPost post = new HttpPost(IMGUR_POSTURI);
        post.setEntity(entity);
        
        HttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(post);
		return response;
	}
	
	private String GetFileUri(String fileLocation){
		String[] projection = { MediaStore.Images.Media.DATA };
		
		CursorLoader cursorLoader = new CursorLoader(cordova.getActivity(), Uri.parse(fileLocation), projection, null, null, null);
		Cursor cursor = cursorLoader.loadInBackground();
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}
}
