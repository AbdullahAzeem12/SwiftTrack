package com.swifttrack.app.data.remote;

import com.swifttrack.app.data.remote.dto.*;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;
import java.util.UUID;

public interface SwiftTrackApi {

    @POST("api/v1/auth/register")
    Call<AuthResponses.JwtResponse> register(@Body AuthRequests.RegisterRequest request);

    @POST("api/v1/auth/login")
    Call<AuthResponses.JwtResponse> login(@Body AuthRequests.LoginRequest request);

    @GET("api/v1/stations")
    Call<List<StationDto>> getAllStations();

    @GET("api/v1/stations/search")
    Call<List<StationDto>> searchStations(@Query("q") String query);

    @GET("api/v1/journeys/search")
    Call<List<JourneyDto>> searchJourneys(
            @Query("originId") UUID originId,
            @Query("destinationId") UUID destinationId,
            @Query("date") String dateIso
    );

    @GET("api/v1/journeys/{id}")
    Call<JourneyDto> getJourneyDetails(@Path("id") UUID id);

    @POST("api/v1/quotes")
    Call<QuoteDto.QuoteResponse> createQuote(@Body QuoteDto.CreateQuoteRequest request);

    @POST("api/v1/bookings")
    Call<BookingDto.BookingResponse> createBooking(@Body BookingDto.CreateBookingRequest request);

    @POST("api/v1/payments/session")
    Call<PaymentDto.PaymentSessionResponse> createPaymentSession(@Body PaymentDto.CreatePaymentSessionRequest request);

    @POST("api/v1/payments/paypal/capture")
    Call<PaymentDto.PaymentStatusResponse> capturePayPalPayment(@Body PaymentDto.PayPalCaptureRequest request);

    @GET("api/v1/payments/{id}/status")
    Call<PaymentDto.PaymentStatusResponse> getPaymentStatus(@Path("id") UUID id);

    @GET("api/v1/tickets")
    Call<List<TicketDto>> getUserTickets();

    @GET("api/v1/tickets/{code}")
    Call<TicketDto> getTicketByCode(@Path("code") String code);

    @GET("api/v1/service-alerts")
    Call<List<ServiceAlertDto>> getActiveAlerts();
}
