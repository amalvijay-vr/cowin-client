package com.cowin.client;

import com.cowin.client.models.Center;
import com.cowin.client.models.Centers;
import com.cowin.client.models.Session;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class CoWinClient {
    //Basic code to check if there are any slots available for a given date; modify/encapsulate as per your needs

    public static final int MAX_DAYS = 1;
    public static final String CO_VIN_IN_API_V_2_PUBLIC_CALENDAR = "https://cdn-api.co-vin.in/api/v2/appointment/sessions/public/calendarByDistrict";
    public static final String TRIVANDRUM_DISTRICT_CODE = "296"; //Externalise the district code as required

    public static void main(String[] args) {
        getNextDays(MAX_DAYS)
                .forEach(CoWinClient::checkAvailabilityProxy);
        //To check a single date, use => checkAvailabilityFor("06-05-2021");
    }

    private static void checkAvailabilityProxy(String date){
        try {
            checkAvailability(date);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void checkAvailability(String date) throws IOException {
        System.out.println("Availability on " + date);
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Gson gson = new Gson();
        Request request = new Request.Builder()
                .url(CO_VIN_IN_API_V_2_PUBLIC_CALENDAR + "?district_id=" + TRIVANDRUM_DISTRICT_CODE + "&date=" + date)
                .method("GET", null)
                .addHeader("accept", "application/json")
                .addHeader("Accept-Language", "hi_IN")
                .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36") //User-Agent header was added as workaround when all the non-browser requests were getting rejected with a 403
                .build();
        Response response = null;

        response = client.newCall(request).execute();

        assert response != null;
        if (response.code() == 200) {
            if (response.body() != null) {
                Centers centers = null;
                centers = gson.fromJson(Objects.requireNonNull(response.body()).string(), Centers.class);
                assert centers != null;
                centers.getCenters()
                        .forEach(CoWinClient::processCenter);
            }
        } else {
            System.out.println("Service not Available");
            if (response.body() != null) {
                System.out.println(Objects.requireNonNull(response.body()).string());

            }

        }
    }

    private static List<String> getNextDays(int maxDays) {
        List<String> days = new ArrayList<>();
        String pattern = "dd-MM-yyyy";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        Date dt = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(dt);
        for (int i = 0; i < maxDays; i++) {
            c.add(Calendar.DATE, 1);
            days.add(simpleDateFormat.format(c.getTime()));
        }
        return days;
    }

    private static void processCenter(Center center) {
        AvailabilityHandler handler = new PrinterHandler();
        List<Session> availableSessions = center.getSessions()
                .stream()
                .filter(CoWinClient::availableSlots)
                .collect(Collectors.toList());
        boolean availableSlots = !availableSessions.isEmpty();
        if (availableSlots) {
            handler.handleAvailability(center, availableSessions);
        } else {
            handler.handleUnAvailability(center);
        }
    }

    private static boolean availableSlots(Session session) {
        return session.getAvailable_capacity() > 0;
    }
}

interface AvailabilityHandler {
    void handleAvailability(Center center, List<Session> availableSessions);
    void handleUnAvailability(Center center);
}

class PrinterHandler implements AvailabilityHandler {
    @Override
    public void handleAvailability(Center center, List<Session> availableSessions) {
        int slotsCount = availableSessions.stream()
                .mapToInt(Session::getAvailable_capacity)
                .sum();
        System.out.println(Instant.now().toString() + " : "
                + slotsCount + " slot(s) available in " + center.getBlock_name() + ", " + center.getName());
    }

    @Override
    public void handleUnAvailability(Center center) {
        System.out.println("    ** No Slots **");
    }
}
