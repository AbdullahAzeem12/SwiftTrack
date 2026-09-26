package com.swifttrack.app.data.remote;

import com.google.gson.annotations.SerializedName;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;

public interface NationalRailApiService {

    /**
     * TransportAPI / UK Open Rail Data Live Station Departures Endpoint
     * GET /v3/uk/train/station/{station_code}/live.json
     */
    @GET("v3/uk/train/station/{station_code}/live.json")
    Call<LiveStationResponse> getLiveStationDepartures(
            @Path("station_code") String stationCode,
            @Query("app_id") String appId,
            @Query("app_key") String appKey,
            @Query("calling_at") String callingAt
    );

    class LiveStationResponse {
        @SerializedName("date")
        public String date;

        @SerializedName("time_of_day")
        public String timeOfDay;

        @SerializedName("station_name")
        public String stationName;

        @SerializedName("station_code")
        public String stationCode;

        @SerializedName("departures")
        public DeparturesContainer departures;
    }

    class DeparturesContainer {
        @SerializedName("all")
        public List<TrainDepartureDto> allDepartures;
    }

    class TrainDepartureDto {
        @SerializedName("mode")
        public String mode;

        @SerializedName("service")
        public String service;

        @SerializedName("train_uid")
        public String trainUid;

        @SerializedName("platform")
        public String platform;

        @SerializedName("operator")
        public String operator;

        @SerializedName("operator_name")
        public String operatorName;

        @SerializedName("aimed_departure_time")
        public String aimedDepartureTime;

        @SerializedName("expected_departure_time")
        public String expectedDepartureTime;

        @SerializedName("aimed_arrival_time")
        public String aimedArrivalTime;

        @SerializedName("origin_name")
        public String originName;

        @SerializedName("destination_name")
        public String destinationName;

        @SerializedName("status")
        public String status;
    }
}
