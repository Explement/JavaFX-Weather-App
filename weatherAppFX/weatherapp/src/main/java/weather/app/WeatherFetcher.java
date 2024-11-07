package weather.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class WeatherFetcher {
    private static final String API_KEY = "PUT YOUR OPENWEATHERMAP API KEY HERE";
    private static final String UV_API_KEY = "PUT YOUR OPENUV API KEY HERE";

    private static final String NORMAL_BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final String ONE_CALL_BASE_URL = "https://api.openuv.io/api/v1/uv";

    @FXML
    private TextField cityTextField;
    @FXML
    private Label cityText, weatherDescription, temperatureText, windSpeedText, uvIndexText;
    @FXML
    private ImageView weatherDisplay;

    private DoubleProperty previousTemperature = new SimpleDoubleProperty();
    private DoubleProperty previousUVIndex = new SimpleDoubleProperty();
    private DoubleProperty previousWindSpeed = new SimpleDoubleProperty();

    public void enterWeather() {
        if (cityTextField != null) {
            try {
                String replacedText = cityTextField.getText().replace(" ", "+");
                String fetchedWeather = fetchWeather(replacedText);

                if (fetchedWeather != "") {
                    String[] returnedWeatherData = parseWeatherData(fetchedWeather);
                    
                    String cityName = returnedWeatherData[0];
                    double temperature = Double.parseDouble(returnedWeatherData[1]);
                    String description = returnedWeatherData[2];
                    String icon = returnedWeatherData[3];
                    String windSpeed = returnedWeatherData[4];
                    String lat = returnedWeatherData[5];
                    String lon = returnedWeatherData[6];
                    String uvIndex = fetchOneCall(Double.parseDouble(lat), Double.parseDouble(lon));

                    temperatureText.textProperty().bind(previousTemperature.asString("%.1f" + "°"));
                    uvIndexText.textProperty().bind(previousUVIndex.asString("%.1f"));
                    windSpeedText.textProperty().bind(previousWindSpeed.asString("%.1f" + " km/h"));

                    setText(cityName, cityText);
                    setText(description, weatherDescription);
                    //setText(windSpeed + " km/h", windSpeedText);
                    //setText(uvIndex, uvIndexText);
                    setImage(weatherDisplay, icon);

                    final Timeline timeline = new Timeline();
                    timeline.getKeyFrames().add(new KeyFrame(
                        Duration.millis(750),
                        new KeyValue(previousTemperature, temperature),
                        new KeyValue(previousUVIndex, Double.parseDouble(uvIndex)),
                        new KeyValue(previousWindSpeed, Double.parseDouble(windSpeed))
                    ));
                    timeline.play();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }     
        }
    }

    private void setText(String text, Label label) {
        label.setText(text);
    }

    private void setImage(ImageView image, String status) {
        image.setImage(new Image("https://openweathermap.org/img/wn/" + status + "@2x.png"));
    }

    public static String fetchWeather(String city) {
        try {
            URI uriWeather = new URI(NORMAL_BASE_URL + "?q=" + city + "&appid=" + API_KEY);

            URL urlWeather = uriWeather.toURL();
            HttpURLConnection conn = (HttpURLConnection) urlWeather.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // Success
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }

                in.close();
                conn.disconnect();

                System.out.println(content.toString());
                return content.toString();
            } else {
                System.out.println("GET request failed. Response Code: " + responseCode);
                return "";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String fetchOneCall (double lat, double lon) {
        try {
            System.out.println(lat);
            System.out.println(lon);

            URI uriWeather = new URI(ONE_CALL_BASE_URL + "?lat=" + lat + "&lng=" + lon);

            URL urlWeather = uriWeather.toURL();
            HttpURLConnection conn = (HttpURLConnection) urlWeather.openConnection(); // 
            conn.setRequestMethod("GET");
            conn.setRequestProperty("x-access-token", UV_API_KEY);
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // Success
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }

                in.close();
                conn.disconnect();

                return parseUVIndex(content.toString());
            } else {
                System.out.println("GET request failed. Response Code: " + responseCode);
                return "";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String[] parseWeatherData(String jsonData) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(jsonData);

            String cityName = (String) jsonObject.get("name");

            JSONObject main = (JSONObject) jsonObject.get("main");
            double temperature = ((Number) main.get("temp")).doubleValue() - 273.15; 

            JSONArray weatherArray = (JSONArray) jsonObject.get("weather");
            JSONObject weatherObject = (JSONObject) weatherArray.get(0);
            String description = (String) weatherObject.get("description");
            String iconCode = (String) weatherObject.get("icon");

            JSONObject wind = (JSONObject) jsonObject.get("wind");
            double windSpeed = (double) wind.get("speed");

            JSONObject coordsArray = (JSONObject) jsonObject.get("coord");
            double lat = ((Number) coordsArray.get("lat")).doubleValue();
            double lon = ((Number) coordsArray.get("lon")).doubleValue();
            
            return new String[]{cityName, String.format("%.2f", temperature), description, iconCode, Double.toString(Operations.round(windSpeed, 1)), Double.toString(lat), Double.toString(lon)};
        } catch (ParseException e) {
            e.printStackTrace();
            return new String[]{""};
        }
    }

    public static String parseUVIndex(String jsonData) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(jsonData);
    
            JSONObject result = (JSONObject) jsonObject.get("result");
            double uv = ((Number) result.get("uv")).doubleValue();
    
            return Double.toString(Operations.round(uv, 1));
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }
}