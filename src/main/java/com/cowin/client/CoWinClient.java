package com.cowin.client;

import com.cowin.client.models.Center;
import com.cowin.client.models.Centers;
import com.cowin.client.models.Session;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

public class CoWinClient {

    public static void main(String[] args) throws IOException {
        String date = "06-05-2021";
        checkAvailabilityFor(date);
    }

    private static void checkAvailabilityFor(String date) throws IOException {
        System.out.println("Availability on " + date);
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Gson gson = new Gson();
        String trivandrumDistrictCode = "296";
        Request request = new Request.Builder()
                .url("https://cdn-api.co-vin.in/api/v2/appointment/sessions/public/calendarByDistrict?district_id=" + trivandrumDistrictCode + "&date=" + date)
                .method("GET", null)
                .addHeader("accept", "application/json")
                .addHeader("Accept-Language", "hi_IN")
                .build();
        Response response = client.newCall(request).execute();
        if (response.code() == 200) {
            if (response.body() != null) {
                Centers centers = gson.fromJson(response.body().string(), Centers.class);
                centers.getCenters()
                        .forEach(CoWinClient::processCenter);
            }
        }
    }

    private static void processCenter(Center center) {
        AvailabilityHandler handler = new PrinterHandler();
        Map<String, Integer> availableSessions = center.getSessions()
                .stream()
                .filter(CoWinClient::availableSlots)
                .collect(Collectors.groupingBy(Session::getSession_id, Collectors.summingInt(Session::getAvailable_capacity)));
        availableSessions.forEach((s, availability) -> handler.handleAvailability(center, availability));
    }

    private static boolean availableSlots(Session session) {
        return session.getAvailable_capacity() > 0;
    }
}

interface AvailabilityHandler {
    void handleAvailability(Center center, int availableCapacity);
}

class PrinterHandler implements AvailabilityHandler {
    @Override
    public void handleAvailability(Center center, int availableCapacity) {
        System.out.println(Instant.now().toString() + " : "
                + availableCapacity + " slot(s) available in " + center.getBlock_name() + ", " + center.getName());
    }
}
