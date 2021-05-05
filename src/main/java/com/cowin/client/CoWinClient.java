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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CoWinClient {

    //Basic code to check if there are any slots available for a given date; modify/encapsulate as per your needs

    public static void main(String[] args) throws IOException {
        getNextDays(4)
                .forEach(CoWinClient::checkAvailabilityFor);

        //To check a single date, use => checkAvailabilityFor("06-05-2021");
    }

    private static List<String> getNextDays(int maxDays) {
        List<String> days = new ArrayList<>();
        String pattern = "dd-MM-yyyy";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        Date dt = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(dt);
        days.add(simpleDateFormat.format(c.getTime()));
        for (int i = 1; i < maxDays; i++) {
            c.add(Calendar.DATE, 1);
            days.add(simpleDateFormat.format(c.getTime()));
        }
        return days;
    }

    private static void checkAvailabilityFor(String date) {
        System.out.println("Availability on " + date);
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Gson gson = new Gson();
        String trivandrumDistrictCode = "296"; //Externalise the district code as required
        Request request = new Request.Builder()
                .url("https://cdn-api.co-vin.in/api/v2/appointment/sessions/public/calendarByDistrict?district_id=" + trivandrumDistrictCode + "&date=" + date)
                .method("GET", null)
                .addHeader("accept", "application/json")
                .addHeader("Accept-Language", "hi_IN")
                .build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (response.code() == 200) {
            if (response.body() != null) {
                Centers centers = null;
                try {
                    centers = gson.fromJson(response.body().string(), Centers.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                centers.getCenters()
                        .forEach(CoWinClient::processCenter);
            }
        }else {
            System.out.println("Service not Available");
            if (response.body() != null) {
                try {
                    System.out.println(response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
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
